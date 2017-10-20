package org.fao.geonet.kernel.datamanager.draft;

import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.MetadataDraft;
import org.fao.geonet.domain.IMetadata;
import org.fao.geonet.events.md.MetadataDraftAdd;
import org.fao.geonet.kernel.datamanager.IMetadataUtils;
import org.fao.geonet.utils.Log;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import jeeves.server.context.ServiceContext;

/**
 * Create draft from a metadata record. 
 */
@Component
public class DraftCreate implements ApplicationListener<MetadataDraftAdd> {

    @Autowired
    private IMetadataUtils utils;

    /**
     * @see org.springframework.context.ApplicationListener#onApplicationEvent(org.springframework.context.ApplicationEvent)
     * @param event
     */
    @Override
    public void onApplicationEvent(MetadataDraftAdd event) {
        try {
            IMetadata md = event.getMd();

            if (!(md instanceof MetadataDraft)) {
                ServiceContext serviceContext = ServiceContext.get();

                String groupOwner = null;
                String source = null;
                Integer owner = 1;

                if (md.getSourceInfo() != null) {
                  if (md.getSourceInfo().getSourceId() != null) {
                    source = md.getSourceInfo().getSourceId().toString();
                  }
                  if (md.getSourceInfo().getGroupOwner() != null) {
                    groupOwner = md.getSourceInfo().getGroupOwner().toString();
                  }
                  owner = md.getSourceInfo().getOwner();
                }
                /*utils.createDraft(serviceContext, md.getId(), source, owner, "", 
                              md.getDataInfo().getType().codeString, 
                              false, md.getUuid());*/
            }

        } catch (Exception e) {
            Log.error(Geonet.DATA_MANAGER, e);
        }
    }
}
