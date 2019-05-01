package org.fao.geonet.kernel.datamanager.draft;

import static org.springframework.data.jpa.domain.Specifications.where;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.fao.geonet.constants.Edit;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.constants.Params;
import org.fao.geonet.domain.Constants;
import org.fao.geonet.domain.Group;
import org.fao.geonet.domain.IMetadata;
import org.fao.geonet.domain.ISODate;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.MetadataCategory;
import org.fao.geonet.domain.MetadataDataInfo;
import org.fao.geonet.domain.MetadataDataInfo_;
import org.fao.geonet.domain.MetadataDraft;
import org.fao.geonet.domain.MetadataSourceInfo;
import org.fao.geonet.domain.MetadataType;
import org.fao.geonet.domain.MetadataValidation;
import org.fao.geonet.domain.Metadata_;
import org.fao.geonet.domain.OperationAllowed;
import org.fao.geonet.domain.Pair;
import org.fao.geonet.domain.ReservedGroup;
import org.fao.geonet.domain.ReservedOperation;
import org.fao.geonet.domain.User;
import org.fao.geonet.kernel.AccessManager;
import org.fao.geonet.kernel.HarvestInfoProvider;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.kernel.ThesaurusManager;
import org.fao.geonet.kernel.UpdateDatestamp;
import org.fao.geonet.kernel.datamanager.base.BaseMetadataManager;
import org.fao.geonet.kernel.datamanager.IMetadataIndexer;
import org.fao.geonet.kernel.schema.AssociatedResourcesSchemaPlugin;
import org.fao.geonet.kernel.schema.SchemaPlugin;
import org.fao.geonet.kernel.search.SearchManager;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.kernel.setting.Settings;
import org.fao.geonet.lib.Lib;
import org.fao.geonet.repository.MetadataDraftRepository;
import org.fao.geonet.repository.MetadataLockRepository;
import org.fao.geonet.repository.SortUtils;
import org.fao.geonet.repository.Updater;
import org.fao.geonet.repository.UserRepository;
import org.fao.geonet.repository.specification.MetadataDraftSpecs;
import org.fao.geonet.repository.specification.MetadataSpecs;
import org.fao.geonet.repository.specification.OperationAllowedSpecs;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;
import org.jdom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;

import jeeves.server.UserSession;
import jeeves.server.context.ServiceContext;

/**
 * trunk-core
 * 
 * @author delawen
 * 
 * 
 */
public class DraftMetadataManager extends BaseMetadataManager {

  @Autowired
  private MetadataDraftRepository mdDraftRepository;

  @Autowired
  private MetadataLockRepository mdLockRepository;

  @Autowired
  private IMetadataIndexer mdIndexer;
  
  @Autowired
  private AccessManager accessManager;

  /**
   * @param context
   */
  @Override
  public void init(ServiceContext context, Boolean force) throws Exception {
    super.init(context, force);
    this.mdDraftRepository = context.getBean(MetadataDraftRepository.class);
    this.mdIndexer = context.getBean(IMetadataIndexer.class);
    this.accessManager = context.getBean(AccessManager.class);
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.BaseMetadataManager#index(java.lang.Boolean,
   *      java.util.Map, java.util.ArrayList)
   * @param force
   * @param docs
   * @param toIndex
   */
  @Override
  protected void index(Boolean force, Map<String, String> docs, ArrayList<String> toIndex) {
    super.index(force, docs, toIndex);

    Sort sortByMetadataChangeDate = SortUtils.createSort(Metadata_.dataInfo, MetadataDataInfo_.changeDate);
    int currentPage = 0;
    Page<Pair<Integer, ISODate>> results = mdDraftRepository
        .findAllIdsAndChangeDates(new PageRequest(currentPage, METADATA_BATCH_PAGE_SIZE, sortByMetadataChangeDate));

    // index all metadata in DBMS if needed
    while (results.getNumberOfElements() > 0) {
      currentPage = index(force, docs, toIndex, currentPage, results);
      results = mdDraftRepository
          .findAllIdsAndChangeDates(new PageRequest(currentPage, METADATA_BATCH_PAGE_SIZE, sortByMetadataChangeDate));
    }
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.BaseMetadataManager#deleteMetadata(jeeves.server.context.ServiceContext,
   *      java.lang.String)
   * @param context
   * @param metadataId
   * @throws Exception
   */
  @Override
  public synchronized void deleteMetadata(ServiceContext context, String metadataId) throws Exception {
    Metadata md = getMetadataRepository().findOne(metadataId);
    if (md != null) {
      MetadataDraft mdD = mdDraftRepository.findOneByUuid(md.getUuid());
      if (mdD != null) {
        super.deleteMetadata(context, Integer.toString(mdD.getId()));
      }
      super.deleteMetadata(context, metadataId);
    } else {
      // We are removing a draft
      IMetadata findOne = mdDraftRepository.findOne(metadataId);
      String uuid = findOne.getUuid();
      if (findOne != null) {
        deleteMetadataFromDB(context, metadataId);
      }
      context.getBean(SearchManager.class).delete("_id", metadataId + "");

      // Make sure the original metadata knows it has been removed
      Metadata originalMd = getMetadataRepository().findOneByUuid(uuid);
      if (originalMd != null) {
        mdIndexer.indexMetadata(Integer.toString(originalMd.getId()), true, null);
        // remove any locks on the original metadata
        mdLockRepository.unlock(Integer.toString(originalMd.getId()));
      } else {
        Log.error(Geonet.DATA_MANAGER, "Draft with uuid " + uuid + " was removed. No original metadata was found.");

      }
    }
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.BaseMetadataManager#updateFixedInfo(java.lang.String,
   *      com.google.common.base.Optional, java.lang.String, org.jdom.Element,
   *      java.lang.String, org.fao.geonet.kernel.UpdateDatestamp,
   *      jeeves.server.context.ServiceContext)
   * @param schema
   * @param metadataId
   * @param uuid
   * @param md
   * @param parentUuid
   * @param updateDatestamp
   * @param context
   * @return
   * @throws Exception
   */
  @Override
  public Element updateFixedInfo(String schema, Optional<Integer> metadataId, String uuid, Element md,
      String parentUuid, UpdateDatestamp updateDatestamp, ServiceContext context, boolean created) throws Exception {
    boolean autoFixing = context.getBean(SettingManager.class).getValueAsBool("system/autofixing/enable", true);
    if (autoFixing) {
      if (Log.isDebugEnabled(Geonet.DATA_MANAGER))
        Log.debug(Geonet.DATA_MANAGER,
            "Autofixing is enabled, trying update-fixed-info (updateDatestamp: " + updateDatestamp.name() + ")");

      IMetadata metadata = null;
      if (metadataId.isPresent()) {
        metadata = getMetadataRepository().findOne(metadataId.get());
        if (metadata == null) {
          metadata = mdDraftRepository.findOne(metadataId.get());
        }
        boolean isTemplate = metadata != null && metadata.getDataInfo().getType() != MetadataType.METADATA;

        // don't process templates
        if (isTemplate) {
          if (Log.isDebugEnabled(Geonet.DATA_MANAGER)) {
            Log.debug(Geonet.DATA_MANAGER, "Not applying update-fixed-info for a template");
          }
          return md;
        }
      }

      String currentUuid = metadata != null ? metadata.getUuid() : null;
      String id = metadata != null ? metadata.getId() + "" : null;
      uuid = uuid == null ? currentUuid : uuid;

      // --- setup environment
      Element env = new Element("env");
      env.addContent(new Element("id").setText(id));
      env.addContent(new Element("uuid").setText(uuid));

      final ThesaurusManager thesaurusManager = context.getBean(ThesaurusManager.class);
      env.addContent(thesaurusManager.buildResultfromThTable(context));

      Element schemaLoc = new Element("schemaLocation");
      schemaLoc.setAttribute(schemaManager.getSchemaLocation(schema, context));
      env.addContent(schemaLoc);

      if (created) {
        env.addContent(new Element("created").setText(new ISODate().toString()));
      }

      if (updateDatestamp == UpdateDatestamp.YES) {
        env.addContent(new Element("changeDate").setText(new ISODate().toString()));
      }
      if (parentUuid != null) {
        env.addContent(new Element("parentUuid").setText(parentUuid));
      }
      if (metadataId.isPresent()) {
        String metadataIdString = String.valueOf(metadataId.get());
        final Path resourceDir = Lib.resource.getDir(context, Params.Access.PRIVATE, metadataIdString);
        env.addContent(new Element("datadir").setText(resourceDir.toString()));
      }

      // add user information to env if user is authenticated (should be)
      Element elUser = new Element("user");
      UserSession usrSess = context.getUserSession();
      if (usrSess.isAuthenticated()) {
        String myUserId = usrSess.getUserId();
        User user = context.getBean(UserRepository.class).findOne(myUserId);
        if (user != null) {
          Element elUserDetails = new Element("details");
          elUserDetails.addContent(new Element("surname").setText(user.getSurname()));
          elUserDetails.addContent(new Element("firstname").setText(user.getName()));
          elUserDetails.addContent(new Element("organisation").setText(user.getOrganisation()));
          elUserDetails.addContent(new Element("username").setText(user.getUsername()));
          elUser.addContent(elUserDetails);
          env.addContent(elUser);
        }
      }

      // add original metadata to result
      Element result = new Element("root");
      result.addContent(md);
      // add 'environment' to result
      env.addContent(new Element("siteURL").setText(context.getBean(SettingManager.class).getSiteURL(context)));

      // Settings were defined as an XML starting with root named config
      // Only second level elements are defined (under system).
      List<?> config = context.getBean(SettingManager.class).getAllAsXML(true).cloneContent();
      for (Object c : config) {
        Element settings = (Element) c;
        env.addContent(settings);
      }

      result.addContent(env);
      // apply update-fixed-info.xsl
      Path styleSheet = metadataSchemaUtils.getSchemaDir(schema).resolve(Geonet.File.UPDATE_FIXED_INFO);
      result = Xml.transform(result, styleSheet);
      return result;
    } else {
      if (Log.isDebugEnabled(Geonet.DATA_MANAGER)) {
        Log.debug(Geonet.DATA_MANAGER, "Autofixing is disabled, not applying update-fixed-info");
      }
      return md;
    }
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.BaseMetadataManager#updateMetadata(jeeves.server.context.ServiceContext,
   *      java.lang.String, org.jdom.Element, boolean, boolean, boolean,
   *      java.lang.String, java.lang.String, boolean)
   * @param context
   * @param metadataId
   * @param md
   * @param validate
   * @param ufo
   * @param index
   * @param lang
   * @param changeDate
   * @param updateDateStamp
   * @return
   * @throws Exception
   */
  @Override
  public IMetadata updateMetadata(ServiceContext context, String metadataId, Element md, boolean validate, boolean ufo,
      boolean index, String lang, String changeDate, boolean updateDateStamp) throws Exception {
    IMetadata metaData = super.updateMetadata(context, metadataId, md, validate, ufo, index, lang, changeDate,
        updateDateStamp);

    if (metaData != null) {
      return metaData;
    } else {
      return mdDraftRepository.findOne(metadataId);
    }
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.BaseMetadataManager#updateMetadataOwner(int,
   *      java.lang.String, java.lang.String)
   * @param id
   * @param owner
   * @param groupOwner
   * @throws Exception
   */
  @Override
  public synchronized void updateMetadataOwner(int id, final String owner, final String groupOwner) throws Exception {

    if (getMetadataRepository().exists(id)) {
      super.updateMetadataOwner(id, owner, groupOwner);
    } else {
      mdDraftRepository.update(id, new Updater<MetadataDraft>() {
        @Override
        public void apply(@Nonnull MetadataDraft entity) {
          entity.getSourceInfo().setGroupOwner(Integer.valueOf(groupOwner));
          entity.getSourceInfo().setOwner(Integer.valueOf(owner));
        }
      });
    }
  }

  @Override
  protected Element buildInfoElem(ServiceContext context, String id, String version) throws Exception {
    IMetadata metadata = getMetadataRepository().findOne(id);
    if (metadata == null) {
      metadata = mdDraftRepository.findOne(id);
    }
    final MetadataDataInfo dataInfo = metadata.getDataInfo();
    String schema = dataInfo.getSchemaId();
    String createDate = dataInfo.getCreateDate().getDateAndTime();
    String changeDate = dataInfo.getChangeDate().getDateAndTime();
    String source = metadata.getSourceInfo().getSourceId();
    String isTemplate = dataInfo.getType().codeString;
    String title = dataInfo.getTitle();
    String uuid = metadata.getUuid();
    String isHarvested = "" + Constants.toYN_EnabledChar(metadata.getHarvestInfo().isHarvested());
    String harvestUuid = metadata.getHarvestInfo().getUuid();
    String popularity = "" + dataInfo.getPopularity();
    String rating = "" + dataInfo.getRating();
    String owner = "" + metadata.getSourceInfo().getOwner();
    String displayOrder = "" + dataInfo.getDisplayOrder();

    Element info = new Element(Edit.RootChild.INFO, Edit.NAMESPACE);

    addElement(info, Edit.Info.Elem.ID, id);
    addElement(info, Edit.Info.Elem.SCHEMA, schema);
    addElement(info, Edit.Info.Elem.CREATE_DATE, createDate);
    addElement(info, Edit.Info.Elem.CHANGE_DATE, changeDate);
    addElement(info, Edit.Info.Elem.IS_TEMPLATE, isTemplate);
    addElement(info, Edit.Info.Elem.TITLE, title);
    addElement(info, Edit.Info.Elem.SOURCE, source);
    addElement(info, Edit.Info.Elem.UUID, uuid);
    addElement(info, Edit.Info.Elem.IS_HARVESTED, isHarvested);
    addElement(info, Edit.Info.Elem.POPULARITY, popularity);
    addElement(info, Edit.Info.Elem.RATING, rating);
    addElement(info, Edit.Info.Elem.DISPLAY_ORDER, displayOrder);

    if (metadata.getHarvestInfo().isHarvested()) {
      HarvestInfoProvider infoProvider = context.getBean(HarvestInfoProvider.class);
      if (infoProvider != null) {
        info.addContent(infoProvider.getHarvestInfo(harvestUuid, id, uuid));
      }
    }
    if (version != null) {
      addElement(info, Edit.Info.Elem.VERSION, version);
    }

    Map<String, Element> map = Maps.newHashMap();
    map.put(id, info);
    buildPrivilegesMetadataInfo(context, map);

    // add owner name
    User user = userRepository.findOne(owner);
    if (user != null) {
      String ownerName = user.getName();
      addElement(info, Edit.Info.Elem.OWNERNAME, ownerName);
    }

    if (metadata instanceof Metadata) {
      for (MetadataCategory category : ((Metadata) metadata).getMetadataCategories()) {
        addElement(info, Edit.Info.Elem.CATEGORY, category.getName());
      }
    } else {
      for (MetadataCategory category : ((MetadataDraft) metadata).getMetadataCategories()) {
        addElement(info, Edit.Info.Elem.CATEGORY, category.getName());
      }
    }

    // add subtemplates
    /*
     * -- don't add as we need to investigate indexing for the fields -- in the
     * metadata table used here List subList = getSubtemplates(dbms, schema); if
     * (subList != null) { Element subs = new
     * Element(Edit.Info.Elem.SUBTEMPLATES); subs.addContent(subList);
     * info.addContent(subs); }
     */

    // Add validity information
    List<MetadataValidation> validationInfo = metadataValidationRepository.findAllById_MetadataId(Integer.parseInt(id));
    if (validationInfo == null || validationInfo.size() == 0) {
      addElement(info, Edit.Info.Elem.VALID, "-1");
    } else {
      String isValid = "1";
      for (Object elem : validationInfo) {
        MetadataValidation vi = (MetadataValidation) elem;
        String type = vi.getId().getValidationType();
        if (!vi.isValid()) {
          isValid = "0";
        }

        String ratio = "xsd".equals(type) ? "" : vi.getNumFailures() + "/" + vi.getNumTests();

        info.addContent(new Element(Edit.Info.Elem.VALID + "_details").addContent(new Element("type").setText(type))
            .addContent(new Element("status").setText(vi.isValid() ? "1" : "0")
                .addContent(new Element("ratio").setText(ratio))));
      }
      addElement(info, Edit.Info.Elem.VALID, isValid);
    }

    // add baseUrl of this site (from settings)
    String protocol = context.getBean(SettingManager.class).getValue(Settings.SYSTEM_SERVER_PROTOCOL);
    String host = context.getBean(SettingManager.class).getValue(Settings.SYSTEM_SERVER_HOST);
    String port = context.getBean(SettingManager.class).getValue(Settings.SYSTEM_SERVER_PORT);
    if (port.equals("80")) {
      port = "";
    } else {
      port = ":" + port;
    }
    addElement(info, Edit.Info.Elem.BASEURL, protocol + "://" + host + port + context.getBaseUrl());
    addElement(info, Edit.Info.Elem.LOCSERV, "/srv/en");
    return info;
  }

  @Override
  public Map<Integer, MetadataSourceInfo> getSourceInfos(Collection<Integer> metadataIds) {
    Map<Integer, MetadataSourceInfo> findAllSourceInfo = getMetadataRepository()
        .findAllSourceInfo(MetadataSpecs.hasMetadataIdIn(metadataIds));
    findAllSourceInfo.putAll(mdDraftRepository.findAllSourceInfo(MetadataDraftSpecs.hasMetadataIdIn(metadataIds)));

    return findAllSourceInfo;
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.BaseMetadataManager#getMetadataObject(java.lang.Integer)
   * @param id
   * @return
   * @throws Exception
   */
  @Override
  public IMetadata getMetadataObject(Integer id) throws Exception {
    IMetadata md = mdDraftRepository.findOne(id);

    //If user can't access draft or doesn't exist draft
    if (md == null || !accessManager.canEdit(id.toString())) {
        md = super.getMetadataObject(id);
    }
    
    return md;
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.BaseMetadataManager#getMetadataObject(java.lang.Integer)
   * @param id
   * @return
   * @throws Exception
   */
  @Override
  public IMetadata getMetadataObjectNoPriv(Integer id) throws Exception {
    IMetadata md = mdDraftRepository.findOne(id);

    //If doesn't exist draft
    if (md == null) {
        md = super.getMetadataObject(id);
    }
    
    return md;
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.BaseMetadataManager#getMetadataObject(java.lang.String)
   * @param uuid
   * @return
   * @throws Exception
   */
  @Override
  public IMetadata getMetadataObject(String uuid) throws Exception {
    IMetadata md = mdDraftRepository.findOneByUuid(uuid);
    
    //If user can't access draft or doesn't exist draft
    if (md == null || !accessManager.canEdit(Integer.toString(md.getId()))) {
      md = super.getMetadataObject(uuid);
    }
    return md;
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.BaseMetadataManager#save(org.fao.geonet.domain.IMetadata)
   * @param md
   */
  @Override
  public IMetadata save(IMetadata md) {
    if (md instanceof Metadata) {
      return super.save(md);
    } else {
      return mdDraftRepository.save((MetadataDraft) md);
    }
  }
}
