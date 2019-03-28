package org.fao.geonet.kernel.datamanager.draft;

import static org.fao.geonet.repository.specification.MetadataDraftSpecs.hasMetadataUuid;
import static org.springframework.data.jpa.domain.Specifications.where;


import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.fao.geonet.constants.Geonet;
import org.fao.geonet.constants.Params;
import org.fao.geonet.domain.Group;
import org.fao.geonet.domain.ISODate;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.MetadataCategory;
import org.fao.geonet.domain.MetadataDataInfo;
import org.fao.geonet.domain.MetadataDraft;
import org.fao.geonet.domain.MetadataHarvestInfo;
import org.fao.geonet.domain.MetadataType;
import org.fao.geonet.domain.OperationAllowed;
import org.fao.geonet.domain.ReservedGroup;
import org.fao.geonet.domain.ReservedOperation;
import org.fao.geonet.exceptions.MetadataLockedException;
import org.fao.geonet.kernel.AccessManager;
import org.fao.geonet.kernel.datamanager.base.BaseMetadataUtils;
import org.fao.geonet.kernel.datamanager.IMetadataOperations;
import org.fao.geonet.kernel.datamanager.IMetadataStatus;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.kernel.UpdateDatestamp;
import org.fao.geonet.kernel.schema.AssociatedResourcesSchemaPlugin;
import org.fao.geonet.kernel.schema.SchemaPlugin;
import org.fao.geonet.repository.GroupRepository;
import org.fao.geonet.repository.MetadataDraftRepository;
import org.fao.geonet.repository.MetadataLockRepository;
import org.fao.geonet.repository.specification.MetadataDraftSpecs;
import org.fao.geonet.repository.specification.OperationAllowedSpecs;
import org.fao.geonet.repository.Updater;
import org.fao.geonet.utils.Log;
import org.fao.geonet.utils.Xml;

import org.jdom.Element;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import jeeves.server.UserSession;
import jeeves.server.context.ServiceContext;

/**
 * It also uses the
 * 
 * @author delawen
 * 
 * 
 */
public class DraftMetadataUtils extends BaseMetadataUtils {

  @Autowired
  private MetadataDraftRepository mdDraftRepository;

  @Autowired
  private AccessManager accessManager;

  @Autowired
  private GroupRepository groupRepository;

  @Autowired
  private IMetadataOperations metadataOperations;

  @Autowired
  private IMetadataStatus metadataStatus;

  @Autowired
  protected MetadataLockRepository mdLockRepository;

  /**
   * @param context
   */
  @Override
  public void init(ServiceContext context, Boolean force) throws Exception {
    super.init(context, force);
    this.mdDraftRepository = context.getBean(MetadataDraftRepository.class);
    this.accessManager = context.getBean(AccessManager.class);
    this.groupRepository = context.getBean(GroupRepository.class);
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.base.BaseMetadataUtils#getMetadataId(java.lang.String)
   * @param uuid
   * @return
   * @throws Exception
   */
  @Override
  public @Nullable String getMetadataId(@Nonnull String uuid) throws Exception {
    String id = super.getMetadataId(uuid);

    if (id != null && !id.isEmpty()) {
      return id;
    }

    // Theoretically, this should never work. If there is a draft it
    // is because there is a published metadata. But, let's be safe. Who
    // knows.
    List<Integer> idList = mdDraftRepository.findAllIdsBy(hasMetadataUuid(uuid));

    if (idList.isEmpty()) {
      return null;
    }
    return String.valueOf(idList.get(0));
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.BaseMetadataUtils#getMetadataId(java.lang.String,
   *      java.lang.Integer)
   * @param uuid
   * @return
   * @throws Exception
   */
/* Doesn't get used anywhere?
  @Override
  public String getMetadataId(String uuid, int userIdAsInt) throws Exception {

    List<Integer> idList = mdDraftRepository.findAllIdsBy(hasMetadataUuid(uuid));

    if (!idList.isEmpty()) {
      Integer mdId = idList.get(0);
      if (accessManager.canEdit(String.valueOf(mdId))) {
        return mdId.toString();
      }
    }

    return getMetadataId(uuid);

  }
*/

  /**
   * @see org.fao.geonet.kernel.datamanager.base.BaseMetadataUtils#getMetadataUuid(java.lang.String)
   * @param id
   * @return
   * @throws Exception
   */
  @Override
  public String getMetadataUuid(String id) throws Exception {
    String uuid = super.getMetadataUuid(id);

    if (uuid != null && !uuid.isEmpty()) {
      return uuid;
    }

    MetadataDraft metadata = mdDraftRepository.findOne(id);

    if (metadata == null)
      return null;

    return metadata.getUuid();
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.base.BaseMetadataUtils#setHarvestedExt(int,
   *      java.lang.String, com.google.common.base.Optional)
   * @param id
   * @param harvestUuid
   * @param harvestUri
   * @throws Exception
   */
  @Override
  public void setHarvestedExt(final int id, final String harvestUuid, final Optional<String> harvestUri)
      throws Exception {
    if (mdDraftRepository.exists(id)) {
      mdDraftRepository.update(id, new Updater<MetadataDraft>() {
        @Override
        public void apply(MetadataDraft metadata) {
          MetadataHarvestInfo harvestInfo = metadata.getHarvestInfo();
          harvestInfo.setUuid(harvestUuid);
          harvestInfo.setHarvested(harvestUuid != null);
          harvestInfo.setUri(harvestUri.orNull());
        }
      });
    } else {
      super.setHarvestedExt(id, harvestUuid, harvestUri);
    }
  }

  @Override
  public void setTemplateExt(final int id, final MetadataType metadataType) throws Exception {
    if (mdDraftRepository.exists(id)) {
      mdDraftRepository.update(id, new Updater<MetadataDraft>() {
        @Override
        public void apply(@Nonnull MetadataDraft metadata) {
          final MetadataDataInfo dataInfo = metadata.getDataInfo();
          dataInfo.setType(metadataType);
        }
      });
    } else {
      super.setTemplateExt(id, metadataType);
    }

  }

  /**
   * @see org.fao.geonet.kernel.datamanager.base.BaseMetadataUtils#updateDisplayOrder(java.lang.String,
   *      java.lang.String)
   * @param id
   * @param displayOrder
   * @throws Exception
   */
  @Override
  public void updateDisplayOrder(String id, final String displayOrder) throws Exception {
    if (mdDraftRepository.exists(Integer.valueOf(id))) {
      mdDraftRepository.update(Integer.valueOf(id), new Updater<MetadataDraft>() {
        @Override
        public void apply(MetadataDraft entity) {
          entity.getDataInfo().setDisplayOrder(Integer.parseInt(displayOrder));
        }
      });
    } else {
      super.updateDisplayOrder(id, displayOrder);
    }
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.base.BaseMetadataUtils#existsMetadata(int)
   * @param id
   * @return
   * @throws Exception
   */
  @Override
  public boolean existsMetadata(int id) throws Exception {
    return super.existsMetadata(id) || mdDraftRepository.exists(id);
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.base.BaseMetadataUtils#existsMetadataUuid(java.lang.String)
   * @param uuid
   * @return
   * @throws Exception
   */
  @Override
  public boolean existsMetadataUuid(String uuid) throws Exception {
    return super.existsMetadataUuid(uuid)
        || !mdDraftRepository.findAllIdsBy(MetadataDraftSpecs.hasMetadataUuid(uuid)).isEmpty();
  }

  /**
   * @see org.fao.geonet.kernel.datamanager.base.BaseMetadataUtils#createDraft(jeeves.server.context.ServiceContext,
   *      java.lang.String)
   * @param context
   * @param id
   * @throws Exception
   */
  @Override
  public String createDraft(ServiceContext context, String id) throws Exception {
    Metadata md = getMetadataRepository().findOne(Integer.valueOf(id));
    MetadataDraft draftMd = null;
    boolean isApproved = false;
    UserSession userSession = context.getUserSession();


    isApproved = (metadataStatus.getCurrentStatus(md.getId()).equals(Params.Status.APPROVED));

    // if the record is approved and a draft doesn't exist then create one
    if (isApproved) { 
        Log.debug(Geonet.DATA_MANAGER, "Attempting to edit approved metadata with id "+id);


        // lock the original record until the draft is destroyed
        synchronized (this) {
            if (mdLockRepository.isLocked(id, userSession.getPrincipal())) {
                 throw new MetadataLockedException(id);
            }
            mdLockRepository.lock(id, userSession.getPrincipal());
        }
        
        // Get parent record from this record
        String parentUuid = "";
        String schemaIdentifier = metadataSchemaUtils.getMetadataSchema(id);
        SchemaPlugin instance = SchemaManager.getSchemaPlugin(schemaIdentifier);
        AssociatedResourcesSchemaPlugin schemaPlugin = null;
        if (instance instanceof AssociatedResourcesSchemaPlugin) {
          schemaPlugin = (AssociatedResourcesSchemaPlugin) instance;
        }
        if (schemaPlugin != null) {
          Set<String> listOfUUIDs = schemaPlugin.getAssociatedParentUUIDs(md.getXmlData(false));
          if (listOfUUIDs.size() > 0) {
            // FIXME more than one parent? Is it even possible?
            parentUuid = listOfUUIDs.iterator().next();
          }
        }

        String groupOwner = null;
        String source = null;
        Integer owner = 1;

        if (md.getSourceInfo() != null) {
          if (md.getSourceInfo().getSourceId() != null) {
            source = md.getSourceInfo().getSourceId().toString();
          }
        }

        // Now get user id of current user and set as owner, groupOwner is same
        owner = Integer.parseInt(userSession.getUserId());
        if (md.getSourceInfo().getGroupOwner() != null) {
          groupOwner = md.getSourceInfo().getGroupOwner().toString();
        }

        id = createDraftRecord(context, id, groupOwner, source, owner, parentUuid, md.getDataInfo().getType().codeString, false, md.getUuid());
        return id;
    }

    throw new Exception("Creating a draft record on a record without APPROVED status is not allowed");

  }

  private String createDraftRecord(ServiceContext context, String templateId, String groupOwner, String source, int owner,
      String parentUuid, String isTemplate, boolean fullRightsForGroup, String uuid) throws Exception {
    Metadata templateMetadata = getMetadataRepository().findOne(templateId);
    if (templateMetadata == null) {
      throw new IllegalArgumentException("Template id not found : " + templateId);
    }

    String schema = templateMetadata.getDataInfo().getSchemaId();
    String data = templateMetadata.getData();
    Element xml = Xml.loadString(data, false);
    //if (templateMetadata.getDataInfo().getType() == MetadataType.METADATA) {
      xml = metadataManager.updateFixedInfo(schema, Optional.<Integer>absent(), uuid, xml, parentUuid, UpdateDatestamp.NO, context, true);
    //}
    final MetadataDraft newMetadata = new MetadataDraft();
    newMetadata.setUuid(uuid);
    
    Map<String,String> extra = new HashMap<String,String>();
    extra.put(Params.APPROVEDMID, templateId);
    newMetadata.getDataInfo().setChangeDate(new ISODate()).setCreateDate(new ISODate()).setSchemaId(schema).setType(MetadataType.lookup(isTemplate)).setExtra(extra);
    if (groupOwner != null) {
      newMetadata.getSourceInfo().setGroupOwner(Integer.valueOf(groupOwner));
    }
    newMetadata.getSourceInfo().setOwner(owner);

    if (source != null) {
      newMetadata.getSourceInfo().setSourceId(source);
    }
    // If there is a default category for the group, use it:
    if (groupOwner != null) {
      Group group = groupRepository.findOne(Integer.valueOf(groupOwner));
      if (group.getDefaultCategory() != null) {
        newMetadata.getMetadataCategories().add(group.getDefaultCategory());
      }
    }
    Collection<MetadataCategory> filteredCategories = Collections2.filter(templateMetadata.getMetadataCategories(),
        new Predicate<MetadataCategory>() {
          @Override
          public boolean apply(@Nullable MetadataCategory input) {
            return input != null;
          }
        });

    newMetadata.getMetadataCategories().addAll(filteredCategories);

    Integer finalId = metadataManager.insertMetadata(context, newMetadata, xml, false, true, true, UpdateDatestamp.YES, fullRightsForGroup, true).getId();

    // Copy privileges from original metadata
    /* Don't do this - the metadata should be created with no permissions by 
       insertMetadata above. If the user wants to allow others to look at the
       metadata and/or edit it, they can do that using the privileges function.
    for (OperationAllowed op : metadataOperations.getAllOperations(templateMetadata.getId())) {
      if(ReservedGroup.all.getId() != op.getId().getGroupId()) { //except for group All
        try {
          metadataOperations.setOperation(context, finalId, op.getId().getGroupId(), op.getId().getOperationId());
        } catch(Throwable t) {
          //On this particular case, we want to set up the operations
          //even if the person creating the draft does not own the groups

          metadataOperations.forceSetOperation(context, finalId, op.getId().getGroupId(), op.getId().getOperationId());
        }
      }
    }
    */

    // Set status to draft
    String changeMessage = "Draft created after editing approved metadata record";
    metadataStatus.setStatusExt(context, finalId, Integer.valueOf(Params.Status.DRAFT), new ISODate(), changeMessage);
    
    metadataIndexer.indexMetadata(String.valueOf(finalId), true, null);
    
    metadataIndexer.indexMetadata(String.valueOf(templateId), true, null);

    return String.valueOf(finalId);
  }

}
