//=============================================================================
//===	Copyright (C) 2001-2007 Food and Agriculture Organization of the
//===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
//===	and United Nations Environment Programme (UNEP)
//===
//===	Copyright (C) 2008-2011 CSIRO Marine and Atmospheric Research,
//=== Australia
//===
//===	This program is free software; you can redistribute it and/or modify
//===	it under the terms of the GNU General Public License as published by
//===	the Free Software Foundation; either version 2 of the License, or (at
//===	your option) any later version.
//===
//===	This program is distributed in the hope that it will be useful, but
//===	WITHOUT ANY WARRANTY; without even the implied warranty of
//===	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
//===	General Public License for more details.
//===
//===	You should have received a copy of the GNU General Public License
//===	along with this program; if not, write to the Free Software
//===	Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
//===
//===	Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
//===	Rome - Italy. email: geonetwork@osgeo.org
//==============================================================================

package org.fao.geonet.kernel.harvest.harvester.thredds;

import jeeves.server.context.ServiceContext;
import jeeves.xlink.Processor;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import org.fao.geonet.Constants;
import org.fao.geonet.GeonetContext;
import org.fao.geonet.Logger;
import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.ISODate;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.MetadataCategory;
import org.fao.geonet.domain.MetadataType;
import org.fao.geonet.exceptions.BadServerCertificateEx;
import org.fao.geonet.exceptions.BadXmlResponseEx;
import org.fao.geonet.kernel.DataManager;
import org.fao.geonet.kernel.SchemaManager;
import org.fao.geonet.kernel.UpdateDatestamp;
import org.fao.geonet.kernel.harvest.BaseAligner;
import org.fao.geonet.kernel.harvest.harvester.CategoryMapper;
import org.fao.geonet.kernel.harvest.harvester.GroupMapper;
import org.fao.geonet.kernel.harvest.harvester.HarvestError;
import org.fao.geonet.kernel.harvest.harvester.HarvestResult;
import org.fao.geonet.kernel.harvest.harvester.IHarvester;
import org.fao.geonet.kernel.harvest.harvester.RecordInfo;
import org.fao.geonet.kernel.harvest.harvester.UriMapper;
import org.fao.geonet.kernel.harvest.harvester.fragment.FragmentHarvester;
import org.fao.geonet.kernel.harvest.harvester.fragment.FragmentHarvester.FragmentParams;
import org.fao.geonet.kernel.harvest.harvester.fragment.FragmentHarvester.HarvestSummary;
import org.fao.geonet.kernel.setting.SettingInfo;
import org.fao.geonet.lib.Lib;
import org.fao.geonet.repository.MetadataCategoryRepository;
import org.fao.geonet.util.Sha1Encoder;
import org.fao.geonet.utils.GeonetHttpRequestFactory;
import org.fao.geonet.utils.Xml;
import org.fao.geonet.utils.XmlRequest;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Namespace;

import thredds.catalog.InvAccess;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvCatalogImpl;
import thredds.catalog.InvCatalogRef;
import thredds.catalog.InvDataset;
import thredds.catalog.InvService;
import thredds.catalog.ServiceType;
import ucar.nc2.units.DateRange;
import ucar.nc2.units.DateType;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonRect;
import ucar.unidata.util.StringUtil2;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLHandshakeException;

//=============================================================================

/**
 * A ThreddsHarvester is able to generate metadata for datasets and services from a Thredds
 * catalogue. The idea here is that datasets with the same variables but possibly differing spatial and
 * temporal extents are stored in a thredds catalog. This harvester can be pointed at the directory
 * and it will query each dataset using WMS to retrieve the spatial and temporal extents plus variables
 * (each one is a wms layer). A single metadata record with these variables and the union of the extents will 
 * be then created by running an XSLT on the information collected from the datasets in the directory.
 *
 * Metadata produced are : <ul> <li>ISO19119 for service metadata (all services in the catalog)</li>
 * <li>ISO19139 (or profile) metadata for the directory</li> </ul>
 *
 * <pre>
 * <nodes>
 *  <node type="thredds" id="114">
 *    <site>
 *      <name>TEST</name>
 *      <uuid>c1da2928-c866-49fd-adde-466fe36d3508</uuid>
 *      <url>http://opendap.bom.gov.au:8080/thredds/catalog/bmrc/access-r-fc/ops/surface/catalog.xml</url>
 *      <icon>thredds.gif</icon>
 *    </site>
 *    <options>
 *      <every>90</every>
 *      <oneRunOnly>false</oneRunOnly>
 *      <status>active</status>
 *      <lang>eng</lang>
 *      <createServiceMd>true</createServiceMd>
 *      <outputSchema>iso19139.mcp</outputSchema>
 *      <datasetCategory>datasets</datasetCategory>
 *      <serviceCategory>services</serviceCategory>
 *    </options>
 *    <privileges>
 *      <group id="1">
 *        <operation name="view" />
 *      </group>
 *    </privileges>
 *    <categories>
 *      <category id="3" />
 *    </categories>
 *    <info>
 *      <lastRun>2007-12-05T16:17:20</lastRun>
 *      <running>false</running>
 *    </info>
 *  </node>
 * </nodes>
 * </pre>
 *
 * @author Simon Pigot
 */
class Harvester extends BaseAligner implements IHarvester<HarvestResult> {


    //---------------------------------------------------------------------------
    //---
    //--- API methods
    //---
    //---------------------------------------------------------------------------
    static private final Namespace invCatalogNS = Namespace.getNamespace("http://www.unidata.ucar.edu/namespaces/thredds/InvCatalog/v1.0");

    //---------------------------------------------------------------------------
    //---
    //--- Private methods
    //---
    //---------------------------------------------------------------------------

    //---------------------------------------------------------------------------
    static private final Namespace gmd = Namespace.getNamespace("gmd", "http://www.isotc211.org/2005/gmd");

    //---------------------------------------------------------------------------
    static private final Namespace srv = Namespace.getNamespace("srv", "http://www.isotc211.org/2005/srv");

    //---------------------------------------------------------------------------
    static private final Namespace xlink = Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink");

    //---------------------------------------------------------------------------
    private Logger log;

    //---------------------------------------------------------------------------
    private ServiceContext context;

    //---------------------------------------------------------------------------
    private ThreddsParams params;

    //---------------------------------------------------------------------------
    private DataManager dataMan;

    //---------------------------------------------------------------------------
    private SchemaManager schemaMan;

    //---------------------------------------------------------------------------
    private CategoryMapper localCateg;

    //---------------------------------------------------------------------------
    private GroupMapper localGroups;

    //---------------------------------------------------------------------------
    private UriMapper localUris;

    //---------------------------------------------------------------------------
    private HarvestResult result;

    //---------------------------------------------------------------------------
    private String hostUrl;

    //---------------------------------------------------------------------------
    private HashSet<String> harvestUris = new HashSet<String>();

    //---------------------------------------------------------------------------
    private Path cdmCoordsToIsoKeywordsStyleSheet;

    //---------------------------------------------------------------------------
    private Path cdmCoordsToIsoMcpDataParametersStyleSheet;

    //---------------------------------------------------------------------------
    private Path fragmentStylesheetDirectory;

    //---------------------------------------------------------------------------
    private String metadataGetService;

    //---------------------------------------------------------------------------
    private Map<String, ThreddsService> services = new HashMap<String, Harvester.ThreddsService>();

    //---------------------------------------------------------------------------
    private InvCatalogImpl catalog;

    //---------------------------------------------------------------------------
    private List<HarvestError> errors = new LinkedList<HarvestError>();

    //---------------------------------------------------------------------------
    private LatLonRect globalLatLonBox = null;
    private DateRange globalDateRange = null;
    private Element wmsResponse = null;
    private Element gridVariables = null;
    

    /**
     * Constructor
     *
     * @param context Jeeves context
     * @param params  Information about harvesting configuration for the node
     * @return null
     **/

    public Harvester(AtomicBoolean cancelMonitor, Logger log, ServiceContext context, ThreddsParams params) {
        super(cancelMonitor);
        this.log = log;
        this.context = context;
        this.params = params;

        result = new HarvestResult();

        GeonetContext gc = (GeonetContext) context.getHandlerContext(Geonet.CONTEXT_NAME);
        dataMan = gc.getBean(DataManager.class);
        schemaMan = gc.getBean(SchemaManager.class);

        metadataGetService = "local://"+context.getNodeId()+"/api/records/";

    }

    //---------------------------------------------------------------------------

    /**
     * Start the harvesting of a thredds catalog
     **/

    public HarvestResult harvest(Logger log) throws Exception {
        this.log = log;

        Element xml = null;
        log.info("Retrieving remote metadata information for : " + params.getName());

        //--- Get uuid's and change dates of metadata records previously
        //--- harvested by this harvester grouping by harvest uri
        localUris = new UriMapper(context, params.getUuid());

        //--- Try to load thredds catalog document
        String url = params.url;
        try {
            XmlRequest req = context.getBean(GeonetHttpRequestFactory.class).createXmlRequest();
            req.setUrl(new URL(url));
            req.setMethod(XmlRequest.Method.GET);
            Lib.net.setupProxy(context, req);

            xml = req.execute();
        } catch (SSLHandshakeException e) {
            throw new BadServerCertificateEx(
                "Most likely cause: The thredds catalog " + url + " does not have a " +
                    "valid certificate. If you feel this is because the server may be " +
                    "using a test certificate rather than a certificate from a well " +
                    "known certification authority, then you can add this certificate " +
                    "to the GeoNetwork keystore using bin/installCert");
        }

        //--- Traverse catalog to create services and dataset metadata as required
        harvestCatalog(xml);

        //--- Remove previously harvested metadata for uris that no longer exist on the remote site
        for (String localUri : localUris.getUris()) {
            if (cancelMonitor.get()) {
                return this.result;
            }

            if (!harvestUris.contains(localUri)) {
                for (RecordInfo record : localUris.getRecords(localUri)) {
                    if (cancelMonitor.get()) {
                        return this.result;
                    }

                    if (log.isDebugEnabled())
                        log.debug("  - Removing deleted metadata with id: " + record.id);
                    dataMan.deleteMetadata(context, record.id);

                    if (record.isTemplate.equals("s")) {
                        //--- Uncache xlinks if a subtemplate
                        Processor.uncacheXLinkUri(metadataGetService + record.uuid);
                        result.subtemplatesRemoved++;
                    } else {
                        result.locallyRemoved++;
                    }
                }
            }
        }

        dataMan.flush();

        result.totalMetadata = result.serviceRecords + result.collectionDatasetRecords;
        return result;
    }

    //---------------------------------------------------------------------------

    /**
     * Add metadata to GN for the services and datasets in a thredds catalog.
     *
     * 1. Open Catalog Document 
     * 2. Crawl the catalog processing datasets as ISO19139 records and
     * request WMS GetCapabilities for each dataset
     * 3. Accumulate union of extents and other info
     * 4. Create a metadata record from accumulated info and service record for the thredds catalog service
     * 5. Save all
     *
     * @param cata Catalog document
     **/

    private void harvestCatalog(Element cata) throws Exception {

        if (cata == null)
            return;

        //--- loading categories and groups
        localCateg = new CategoryMapper(context);
        localGroups = new GroupMapper(context);

        //--- Setup proxy authentication
        Lib.net.setupProxy(context);

        //--- load catalog
        InvCatalogFactory factory = new InvCatalogFactory("default", true);
        catalog = (InvCatalogImpl) factory.readXML(params.url);
        StringBuilder buff = new StringBuilder();
        if (!catalog.check(buff, true)) {
            throw new BadXmlResponseEx("Invalid catalog " + params.url + "\n" + buff.toString());
        }

        //--- display catalog read in log file
        log.info("Catalog read from " + params.url + " is \n" + factory.writeXML(catalog));

        Path schemaDir = schemaMan.getSchemaDir(params.outputSchema);

        //--- get service and dataset style sheets, try schema first
       	Path serviceStyleSheet = schemaDir.
            resolve(Geonet.Path.TDS_19119_19139_STYLESHEETS). 
            resolve("ThreddsCatalog-to-19119.xsl");
       	Path datasetStyleSheet = schemaDir.
            resolve(Geonet.Path.TDS_19119_19139_STYLESHEETS). 
            resolve("ThreddsCatalog-to-19139.xsl");
        if (!Files.exists(serviceStyleSheet)) {
        	serviceStyleSheet = context.getAppPath().
            resolve(Geonet.Path.IMPORT_STYLESHEETS).
            resolve("ThreddsCatalog-to-ISO19119.xsl");
        }
        if (!Files.exists(datasetStyleSheet)) {
        	datasetStyleSheet = context.getAppPath().
            resolve(Geonet.Path.IMPORT_STYLESHEETS).
            resolve("ThreddsCatalog-to-ISO19139.xsl");
				}

				Path dataParamsStylesheet = null;
        // -- This is schema dependent - no real equivalent in 19115:2005
        if (schemaDir.toString().contains("iso19139.mcp")) {
            dataParamsStylesheet = schemaDir.
							resolve(Geonet.Path.TDS_19119_19139_STYLESHEETS).
              resolve("NetcdfSubsetDataset-to-ISO19139MCPDataParameters.xsl");
        } 


        //--- Get base host url
        URL url = new URL(params.url);
        hostUrl = url.getProtocol() + "://" + url.getHost();
        if (url.getPort() != -1) hostUrl += ":" + url.getPort();

        //--- Crawl all datasets in the thredds catalogue
        log.info("Crawling the datasets in the catalog....");
        List<InvDataset> dsets = catalog.getDatasets();
        for (InvDataset ds : dsets) {
            if (cancelMonitor.get()) {
                return;
            }

            crawlDatasets(ds);
        }

        if (params.createServiceMd) {
            //--- process services found by crawling the catalog
            processServices(cata, serviceStyleSheet);
        }

        log.info("Adding dataset metadata...");
        createDatasetMetadata(cata, datasetStyleSheet, dataParamsStylesheet);

        //--- show how many datasets have been processed
        int totalDs = result.collectionDatasetRecords;
        log.info("Processed " + totalDs + " datasets.");

    }

    //---------------------------------------------------------------------------

    /**
     * Crawl all datasets in the catalog recursively
     *
     * @param    catalogDs        the dataset being processed
     * @throws Exception
     **/

    private void crawlDatasets(InvDataset catalogDs) throws Exception {
        log.info("Crawling through " + catalogDs.getName());

        // HACK!! Get real dataset hidden by netcdf library when catalog ref name
        // equals top dataset name in referenced catalog
        InvDataset realDs = catalogDs;
        if (catalogDs instanceof InvCatalogRef) {
            InvDataset proxyDataset = ((InvCatalogRef) catalogDs).getProxyDataset();
            realDs = proxyDataset.getName().equals(catalogDs.getName()) ? proxyDataset : catalogDs;
        }

        // if there are nested datasets then process those recursively
        if (realDs.hasNestedDatasets()) {
            List<InvDataset> dsets = realDs.getDatasets();
            for (InvDataset ds : dsets) {
                crawlDatasets(ds);
            }
        } else {
            log.info("Processing dataset: " + realDs.getName() + " with URL: " + getUri(realDs));
            examineThreddsDataset(realDs);
        }

        // Release resources allocated when crawling catalog references
        if (catalogDs instanceof InvCatalogRef) {
            ((InvCatalogRef) catalogDs).release();
        }
    }

    //---------------------------------------------------------------------------

    /**
     * Save the metadata to GeoNetwork's database
     *
     * @param md   the metadata being saved
     * @param uuid the uuid of the metadata being saved
     * @param uri  the uri from which the metadata has been harvested
     * @param isService  is this a service metadata record?
     **/

    private void saveMetadata(Element md, String uuid, String uri, boolean isService) throws Exception {

        //--- strip the catalog namespace as it is not required
        md.removeNamespaceDeclaration(invCatalogNS);

        String schema = dataMan.autodetectSchema(md, null); // should be iso19139
        if (schema == null) {
            log.warning("Skipping metadata with unknown schema.");
            result.unknownSchema++;
        }

        log.info("  - Adding metadata with " + uuid + " schema is set to " + schema + "\n XML is " + Xml.getString(md));

        deleteExistingMetadata(uri);

        //
        // insert metadata
        //
        Metadata metadata = new Metadata().setUuid(uuid);
        metadata.getDataInfo().
            setSchemaId(schema).
            setRoot(md.getQualifiedName()).
            setType(MetadataType.METADATA);
        metadata.getSourceInfo().
            setSourceId(params.getUuid()).
            setOwner(Integer.parseInt(params.getOwnerId()));
        metadata.getHarvestInfo().
            setHarvested(true).
            setUuid(params.getUuid()).
            setUri(uri);

				if (!isService) {
        	if (params.datasetCategory != null && !params.datasetCategory.equals("")) {
           	MetadataCategory metadataCategory = context.getBean(MetadataCategoryRepository.class).findOne(Integer.parseInt(params.datasetCategory));
	
           	if (metadataCategory == null) {
             	throw new IllegalArgumentException("No category found with name: " + params.datasetCategory);
           	}
           	metadata.getMetadataCategories().add(metadataCategory);
        	}
				}
				else {
        	addCategories(metadata, params.getCategories(), localCateg, context, log, null, false);
				}

        metadata = dataMan.insertMetadata(context, metadata, md, true, false, false, UpdateDatestamp.NO, false, false);

        String id = String.valueOf(metadata.getId());

        addPrivileges(id, params.getPrivileges(), localGroups, dataMan, context, log);

        dataMan.indexMetadata(id, true, null);

        dataMan.flush();
    }

    /**
     * Examine one dataset getting metadata including variables, extents and 
     * service urls.
     * Note: collection datasets are not processes here.
     *
     * @param ds the dataset to be examined.
     * @throws Exception
     **/

    private void examineThreddsDataset(InvDataset ds) throws Exception {
        //--- harvest metadata only if the dataset has changed
        if (datasetChanged(ds)) {
            getMetadata(ds);
        }

        //--- Add dataset uri to list of harvested uri's
        harvestUris.add(getUri(ds));

        //--- Record service URL for the dataset so that it can be added to the service
        //--- if required 
        List<InvAccess> accesses = ds.getAccess();
        for (InvAccess access : accesses) {
            processService(access.getService(), getUuid(ds), ds);
        }
    }

    /**
     * Process a service reference for a dataset. Record details of the service      * and add a url to access the dataset using this service.
     *
     * @param serv the service to be processed
     * @param uuid uuid of the dataset that is delivered by this service
     * @param ds   dataset that is being delivered by this service
     **/

    private void processService(InvService serv, String uuid, InvDataset ds) {

        //--- get service, if compound service then get all nested services
        List<InvService> servs = new ArrayList<InvService>();
        if (serv.getServiceType() == ServiceType.COMPOUND) {
            servs.addAll(serv.getServices());
        } else {
            servs.add(serv);
        }

        //--- add dataset info to the appropriate ThreddsService
        for (InvService s : servs) {
            //Skip resolver services
            if (s.getServiceType().equals(ServiceType.RESOLVER)) continue;

            String sUrl = "";

            if (!s.isRelativeBase()) {
                sUrl = s.getBase();
            } else {
                sUrl = hostUrl + s.getBase();
            }

            log.info("Processing service: "+sUrl+" for "+ds.getName());
            ThreddsService ts = services.get(sUrl);
            if (ts == null) {
                ts = new ThreddsService();
                ts.service = s;
                ts.version = getVersion(serv, ds);
                ts.ops = getServerOperations(serv, ds);

                services.put(sUrl, ts);
            }
            InvAccess access = ds.getAccess(s.getServiceType());
        	  if (access != null) {
              String url = access.getStandardUrlName();
              ts.datasetUrls.add(url);
            }
        }

    }

    /**
     * Find the version of the service that delivers a particular dataset Handles OPeNDAP and HTTP
     * only at present
     *
     * @param    serv    the service that delivers the dataset
     * @param    ds        the dataset being delivered by the service
     **/

    private String getVersion(InvService serv, InvDataset ds) {
        String result = "unknown";
        if (serv.getServiceType() == ServiceType.OPENDAP) {
            InvAccess access = ds.getAccess(ServiceType.OPENDAP);
            if (access != null) {
                String href = access.getStandardUrlName() + ".ver";
                String readResult = getResultFromHttpUrl(href);
                if (readResult != null) result = readResult;
            }
        } else if (serv.getServiceType() == ServiceType.HTTPServer) {
            result = "HTTP/1.1";
        } else if (serv.getServiceType() == ServiceType.WMS) {
            result = "1.3.0"; // hard coded? We could get this elsewhere
        }
        return result;
    }

    /**
     * Get the server operations Applicable to OPeNDAP only at present
     *
     * @param    serv    the service that delivers the dataset
     * @param    ds        the dataset being delivered by the service
     **/

    private String getServerOperations(InvService serv, InvDataset ds) {
        String result = "none";
        if (serv.getServiceType() == ServiceType.OPENDAP) {
            InvAccess access = ds.getAccess(ServiceType.OPENDAP);
            if (access != null) {
                String href = access.getStandardUrlName() + ".help";
                String readResult = getResultFromHttpUrl(href);
                if (readResult != null) result = readResult;
            }
        }
        return result;
    }

    /**
     * Get a String result from an HTTP URL
     *
     * @param href the URL to get the info from
     **/

    private String getResultFromHttpUrl(String href) {
        String result = null;
        try {
            //--- get the version from the OPeNDAP server
            URL url = new URL(href);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            Object o = conn.getContent();
            if (log.isDebugEnabled())
                log.debug("Opened " + href + " and got class " + o.getClass().getName());
            StringBuffer version = new StringBuffer();
            String inputLine;
            BufferedReader dis = null;
            InputStreamReader isr = null;
            InputStream is = null;
            try {
                is = conn.getInputStream();
                isr = new InputStreamReader(is, Constants.ENCODING);
                dis = new BufferedReader(isr);
                while ((inputLine = dis.readLine()) != null) {
                    version.append(inputLine + "\n");
                }
                result = version.toString();
                if (log.isDebugEnabled()) log.debug("Read from URL:\n" + result);
            } finally {
                IOUtils.closeQuietly(is);
                IOUtils.closeQuietly(isr);
                IOUtils.closeQuietly(dis);
            }
        } catch (Exception e) {
            if (log.isDebugEnabled())
                log.debug("Caught exception " + e + " whilst attempting to query URL " + href);
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Get dataset uri
     *
     * @param ds the dataset to be processed
     **/

    private String getUri(InvDataset ds) {
        if (ds.getID() == null) {
            return ds.getParentCatalog().getUriString() + "#" + ds.getName();
        } else {
            return getSubsetUrl(ds);
        }
    }

    /**
     * Return url to a catalog having the specified dataset as the top dataset
     *
     * @param ds the dataset to be processed
     **/

    private String getSubsetUrl(InvDataset ds) {
        try {
            return ds.getParentCatalog().getUriString() + "?dataset=" + URLEncoder.encode(ds.getID(), Constants.ENCODING);
        } catch (UnsupportedEncodingException e) {
            log.error("Thrown Exception " + e + " during dataset processing");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Has the dataset has been modified since the last harvest
     *
     * @param ds the dataset to be processed
     **/

    private boolean datasetChanged(InvDataset ds) {
        List<RecordInfo> localRecords = localUris.getRecords(params.url);

        if (localRecords == null) return true;

        Date lastModifiedDate = null;

        List<DateType> dates = ds.getDates();

        for (DateType date : dates) {
            if (date.getType().equalsIgnoreCase("modified")) {
                lastModifiedDate = date.getDate();
            }
        }

        if (lastModifiedDate == null) return true;

        String datasetModifiedDate = new ISODate(lastModifiedDate.getTime(), false).toString();

        for (RecordInfo localRecord : localRecords) {
            if (localRecord.isOlderThan(datasetModifiedDate)) return true;
        }

        return false;
    }

    /**
     * Delete all metadata previously harvested for a particular uri
     *
     * @param uri uri for which previously harvested metadata should be deleted
     **/

    private void deleteExistingMetadata(String uri) throws Exception {
        List<RecordInfo> localRecords = localUris.getRecords(uri);

        if (localRecords == null) return;

        for (RecordInfo record : localRecords) {
            dataMan.deleteMetadata(context, record.id);

            if (record.isTemplate.equals("s")) {
                //--- Uncache xlinks if a subtemplate
                Processor.uncacheXLinkUri(metadataGetService + record.uuid);
            }
        }
    }


    /**
     * Get uuid for dataset
     *
     * @param ds the dataset to be processed
     **/

    private String getUuid(InvDataset ds) {
        String uuid = ds.getUniqueID();

        if (uuid == null) {
            uuid = Sha1Encoder.encodeString(ds.getCatalogUrl()); // md5 full dataset url
        } else {
            uuid = StringUtil2.allow(uuid, "_-.", '-');
        }

        return uuid;
    }

		private Element getXMLResponse(String url) throws MalformedURLException, IOException {
      XmlRequest req = context.getBean(GeonetHttpRequestFactory.class).createXmlRequest();
      req.setUrl(new URL(url));
      req.setMethod(XmlRequest.Method.GET);
      Lib.net.setupProxy(context, req);

      return req.execute();
		}

    /**
     * All datasets are assumed to have the same variables and WMS service metadata so we
     * get the getcapabilities statement for one layer and then just the bounding box and 
     * date range for all other layers.
     *
     * @param ds the dataset to be processed
     */

    private void getMetadata(InvDataset ds) {
        try {

          String url = "";
          InvAccess access = null;
					if (wmsResponse == null) {
            // Get WMS URL and build getcapabilities statement for first layer we find.....
            access = ds.getAccess(ServiceType.WMS);
        	  if (access != null) {
              url = access.getStandardUrlName();
              log.debug("WMS url is "+url);
            } else {
              log.error("Cannot build WMS URL!");
              return;
            }
            url += "?request=GetCapabilities&version=1.3.0&service=WMS";

            wmsResponse = getXMLResponse(url); 
					}

          // now for every other layer go to the subset service and get the 
          // dataset.xml file as it contains bbox and textent
          access = ds.getAccess(ServiceType.NetcdfSubset);
        	if (access != null) {
            url = access.getStandardUrlName();
            log.info("NCSS url is "+url);
          } else {
            log.error("Cannot build NCSS URL!");
            return;
          }
          url += "/dataset.xml";
            
          Element xml = getXMLResponse(url);
					/* Looking for: 
                       ....
                         <LatLonBox>
                              <west>65.0000</west>
                              <east>-175.4299</east>
                              <south>-65.0000</south>
                              <north>16.9500</north>
                         </LatLonBox>
                         <TimeSpan>
                              <begin>2017-07-22T06:00:00Z</begin>
                              <end>2017-07-22T06:00:00Z</end>
                         </TimeSpan>	
                       ....
             If we don't find then we skip this dataset
          */
          Element latLonBox = xml.getChild("LatLonBox");
          if (latLonBox == null) {
						log.error("Cannot find LatLonBox element!, skipping dataset");
						return;
          }

          Element timeSpan = xml.getChild("TimeSpan"); 
          if (timeSpan == null) {
						log.error("Cannot find TimeSpan element!, skipping dataset");
						return;
          }

          if (log.isDebugEnabled()) 
          	log.debug("Bounding box is:\n"+Xml.getString(latLonBox)+"\n Time span is:\n"+Xml.getString(timeSpan));

					// extend global bbox and textent using what we found
          addLatLonBox(latLonBox);
          addTimeSpan(timeSpan);

          // record one copy of the dataset description so that mcp:dataParameters can be 
          // be created
          if (gridVariables == null) {
						gridVariables = xml;
					}

        } catch (Exception e) {
          log.error("Thrown Exception " + e + " during dataset processing");
          e.printStackTrace();
        }
		}

    /**
     * Extend global bounding box (globalLatLonBox) using supplied latLonBox. 
     *
     * @param    latLonBox           bounding box to add to globalLatLonBox 
     **/

    private void addLatLonBox(Element latLonBox) {
			double west = Double.parseDouble(latLonBox.getChildText("west"));
			double east = Double.parseDouble(latLonBox.getChildText("east"));
			double south = Double.parseDouble(latLonBox.getChildText("south"));
			double north = Double.parseDouble(latLonBox.getChildText("north"));
      LatLonRect thisBox = new LatLonRect(new LatLonPointImpl(south,west), new LatLonPointImpl(north,east));
      if (globalLatLonBox == null) {
				globalLatLonBox = thisBox;
			} else {
        globalLatLonBox.extend(thisBox);
			}
    }

    /**
     * Extend global date range (globalDateRange) using supplied timeSpan. 
     *
     * @param    timeSpan           time span to add to globalDateRange 
     **/

    private void addTimeSpan(Element timeSpan) {
      ISODate beginDate = new ISODate(timeSpan.getChildText("begin"));
      ISODate endDate = new ISODate(timeSpan.getChildText("end"));
      DateRange thisDateRange = new DateRange(beginDate.toDate(), endDate.toDate());
      if (globalDateRange == null) {
      	globalDateRange = thisDateRange; 
      } else {
        globalDateRange.extend(thisDateRange);
      }
    }

    /**
     * Process all services that serve datasets in the thredds catalog. 
     *
     * @param    cata                the XML of the catalog
     * @param    styleSheet    name of the stylesheet to produce 19119
     **/

    private void processServices(Element cata, Path styleSheet) throws Exception {

        for (String sUrl : services.keySet()) {

            ThreddsService ts = services.get(sUrl);
            InvService serv = ts.service;
            String type = serv.getServiceType().toString();

            if (log.isDebugEnabled()) log.debug("Processing Thredds service: " + serv.toString());

            String sUuid = Sha1Encoder.encodeString(sUrl);
            String urls = StringUtils.join(ts.datasetUrls,"^^^");

            	//---	pass info to stylesheet which will create a 19119 record
	
            if (log.isDebugEnabled())
                log.debug("  - XSLT transformation using " + styleSheet);

            Map<String, Object> param = new HashMap<String, Object>();
            param.put("lang", params.lang);
            param.put("topic", params.topic);
            param.put("uuid", sUuid);
            param.put("url", urls);
            param.put("name", "Thredds Service "+serv.getName()+ " at "+sUrl);
            param.put("type", serv.getServiceType().toString().toUpperCase());
            param.put("version", ts.version);
            param.put("desc", serv.toString());
            param.put("props", serv.getProperties().toString());
            param.put("serverops", ts.ops);
            param.put("bbox", globalLatLonBox.getLatMin()+"^^^"+globalLatLonBox.getLatMax()+"^^^"+globalLatLonBox.getLonMin()+"^^^"+globalLatLonBox.getLonMax());
            param.put("textent", globalDateRange.getStart().toDateTimeStringISO()+"^^^"+globalDateRange.getEnd().toDateTimeStringISO());
	
            Element md = Xml.transform(cata, styleSheet, param);
	
            String schema = dataMan.autodetectSchema(md, null);
            if (schema == null) {
               	log.warning("Skipping metadata with unknown schema.");
               	result.unknownSchema++;
            } else {
	
               	//--- Now add to geonetwork
                boolean isService = true;
               	saveMetadata(md, sUuid, sUrl, isService);
	
               	harvestUris.add(sUrl);
	
               	result.serviceRecords++;
            }
        }
    }

    /**
     * Create a dataset for the thredds catalog URL, write variables and
     * selected services as online references in distributonInfo.
     *
     * @param    cata                    the XML of the catalog
     * @param    styleSheet              stylesheet to produce 19139
     * @param    dataParamsStyleSheet    stylesheet to produce mcp:dataParameters from subset service xml
     **/

    private void createDatasetMetadata(Element cata, Path styleSheet, Path dataParamsStylesheet) throws Exception {

        String sUuid = Sha1Encoder.encodeString(params.url);

        //---	pass info to stylesheet which will create a 19139 record

        if (log.isDebugEnabled())
               log.debug("  - XSLT transformation using " + styleSheet);

				String title = params.datasetTitle;
        if (title.equals("")) {
					title =  "Thredds Dataset at "+params.url;
				}
        String abst = params.datasetAbstract;
        if (abst.equals("")) {
					abst = "Thredds Dataset";
				}

        Map<String, Object> param = new HashMap<String, Object>();
        param.put("lang", params.lang);
        param.put("topic", params.topic);
        param.put("uuid", sUuid);
        param.put("url", params.url);
        param.put("name", title);
        param.put("desc", abst);
        if (globalLatLonBox != null) {
        	param.put("bbox", globalLatLonBox.getLatMin()+"^^^"+globalLatLonBox.getLatMax()+"^^^"+globalLatLonBox.getLonMin()+"^^^"+globalLatLonBox.getLonMax());
				}
				if (globalDateRange != null) {
        	param.put("textent", globalDateRange.getStart().toDateTimeStringISO()+"^^^"+globalDateRange.getEnd().toDateTimeStringISO());
				}
	
        Element md = Xml.transform(wmsResponse, styleSheet, param);
	
        String schema = dataMan.autodetectSchema(md, null);
        if (schema == null) {
          log.warning("Skipping metadata with unknown schema.");
          result.unknownSchema++;
        } else {
          if (dataParamsStylesheet != null) {
            Element dps = Xml.transform(gridVariables, dataParamsStylesheet);
            addDataParameters(md, dps);
					}
	
          //--- Now add to geonetwork
          boolean isService = false;
          saveMetadata(md, sUuid, params.url, isService);
	
          harvestUris.add(params.url);

          result.collectionDatasetRecords++;
			}
    }

    /**
     * Add an Element to a child list at index after specified element.
     *
     * @param md         iso19139 metadata
     * @param theNewElem the new element to be added
     * @param name       the name of the element to search for
     * @param ns         the namespace of the element to search for
     **/

    boolean addAfter(Element md, Element theNewElem, String name, Namespace ns) throws Exception {
        Element chSet = md.getChild(name, ns);

        if (chSet != null) {
            int pos = md.indexOf(chSet);
            md.addContent(pos + 1, theNewElem);
            return true;
        }

        return false;
    }

    /**
     * Add keywords generated from CDM coordinate systems to identificationInfo
     *
     * <gmd:descriptiveKeywords> <gmd:MD_Keywords> <gmd:keyword> <gco:CharacterString>
     * </gco:CharacterString> </gmd:keyword> ... ... ... <gmd:type> <gmd:MD_KeywordType codelist...>
     * </gmd:type> <gmd:thesaurusName> <gmd:CI_Citation> .... </gmd:CI_Citation>
     * </gmd:thesaurusName> </gmd:MD_Keywords> </gmd:descriptiveKeywords>
     *
     * @param md       iso19139 metadata
     * @param keywords gmd:keywords block to be added to metadata
     **/

    private Element addKeywords(Element md, Element keywords) throws Exception {
        Element root = (Element) md.getChild("identificationInfo", gmd).getChildren().get(0);
        boolean ok = addAfter(root, keywords, "descriptiveKeywords", gmd);
        if (!ok) {
            throw new BadXmlResponseEx("The metadata did not have a descriptiveKeywords Element");
        }
        return md;
    }

    /**
     * Add mcp:dataParameters created from netcdf subset service to identificationInfo (mcp only)
     *
     * <mcp:dataParameters> <mcp:DP_DataParameters> ... ... ... </mcp:DP_DataParameters>
     * </mcp:dataParameters>
     *
     * @param md             iso19139 MCP metadata
     * @param dataParameters mcp:dataParameters block to be added to metadata
     **/

    private Element addDataParameters(Element md, Element dataParameters) throws Exception {
        Element root = (Element) md.getChild("identificationInfo", gmd).getChildren().get(0);
        root.addContent(dataParameters); // this is dependent on the mcp schema - last element
        return md;
    }

    @Override
    public List<HarvestError> getErrors() {
        return errors;
    }

    private static class ThreddsService {
        public List<String> datasetUrls = new ArrayList();
        public InvService service;
        public String version;
        public String ops;
    }

}
