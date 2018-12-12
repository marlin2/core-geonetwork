/*
 * Copyright (C) 2001-2016 Food and Agriculture Organization of the
 * United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * and United Nations Environment Programme (UNEP)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 * Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * Rome - Italy. email: geonetwork@osgeo.org
 */

package org.fao.geonet.api.records;

import io.swagger.annotations.*;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.api.API;
import org.fao.geonet.api.ApiParams;
import org.fao.geonet.api.ApiUtils;
import org.fao.geonet.api.tools.i18n.LanguageUtils;
import org.fao.geonet.api.processing.report.MetadataProcessingReport;
import org.fao.geonet.api.processing.report.SimpleMetadataProcessingReport;
import org.fao.geonet.domain.ISODate;
import org.fao.geonet.domain.IMetadata;
import org.fao.geonet.domain.Metadata;
import org.fao.geonet.domain.Pair;
import org.fao.geonet.kernel.AccessManager;
import org.fao.geonet.kernel.datamanager.IMetadataIndexer;
import org.fao.geonet.kernel.metadata.StatusActions;
import org.fao.geonet.kernel.metadata.StatusActionsFactory;
import org.fao.geonet.repository.MetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import jeeves.server.context.ServiceContext;
import jeeves.services.ReadWriteController;

import org.springframework.web.bind.annotation.ResponseStatus;

import springfox.documentation.annotations.ApiIgnore;

import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_OPS;
import static org.fao.geonet.api.ApiParams.API_CLASS_RECORD_TAG;
import static org.fao.geonet.api.ApiParams.API_PARAM_RECORD_UUID;

@RequestMapping(value = {
    "/api/records",
    "/api/" + API.VERSION_0_1 +
        "/records"
})
@Api(value = API_CLASS_RECORD_TAG,
    tags = API_CLASS_RECORD_TAG,
    description = API_CLASS_RECORD_OPS)
@Controller("recordWorkflow")
@ReadWriteController
public class MetadataWorkflowApi {

    @Autowired
    LanguageUtils languageUtils;


    @ApiOperation(
        value = "Set record status",
        notes = "",
        nickname = "status")
    @RequestMapping(value = "/{metadataUuid}/status",
        method = RequestMethod.PUT
    )
    @PreAuthorize("hasRole('Editor')")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Status updated."),
        @ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_CAN_EDIT)
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void status(
        @ApiParam(
            value = API_PARAM_RECORD_UUID,
            required = true)
        @PathVariable
            String metadataUuid,
        @ApiParam(
            value = "status",
            required = true
        )
        // TODO: RequestBody could be more appropriate ?
        @RequestParam(
            required = true
        )
            Integer status,
        @ApiParam(
            value = "comment",
            required = true
        )
        @RequestParam(
            required = true
        )
            String comment,
        @ApiParam(
            value = "publishGroups",
            required = false
        )
        @RequestParam(
            required = false
        )
            String publishGroups,
        @ApiParam(
            value = "editingGroups",
            required = false
        )
        @RequestParam(
            required = false
        )
            String editingGroups,
        HttpServletRequest request
    )
        throws Exception {
        IMetadata metadata = ApiUtils.canEditRecord(metadataUuid, request);
        ApplicationContext appContext = ApplicationContextHolder.get();
        Locale locale = languageUtils.parseAcceptLanguage(request.getLocales());
        ServiceContext context = ApiUtils.createServiceContext(request, locale.getISO3Language());


        AccessManager am = appContext.getBean(AccessManager.class);
        //--- only allow the owner of the record to set its status
        if (!am.isOwner(context, String.valueOf(metadata.getId()))) {
            throw new SecurityException(String.format(
                "Only the owner of the metadata can set the status. User is not the owner of the metadata"
            ));
        }
 
        ISODate changeDate = new ISODate();

        //--- use StatusActionsFactory and StatusActions class to
        //--- change status and carry out behaviours for status changes
        StatusActionsFactory saf = appContext.getBean(StatusActionsFactory.class);

        StatusActions sa = saf.createStatusActions(context);

        Set<Integer> metadataIds = new HashSet<Integer>();
        metadataIds.add(metadata.getId());
        Pair<Set<Integer>,Set<Integer>> results = sa.statusChange(String.valueOf(status), metadataIds, changeDate, comment, publishGroups, editingGroups);

        //--- reindex metadata
        appContext.getBean(IMetadataIndexer.class).indexMetadata(results.one());
    }

    @ApiOperation(
        value = "Set status of one or more records",
        notes = "",
        nickname = "setStatusOfRecords")
    @RequestMapping(
        value = "/status",
        produces = {
            MediaType.APPLICATION_JSON_VALUE
        },
        method = RequestMethod.PUT)
    @ResponseStatus(value = HttpStatus.CREATED)
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Report about updated records."),
        @ApiResponse(code = 403, message = ApiParams.API_RESPONSE_NOT_ALLOWED_ONLY_EDITOR)
    })
    @PreAuthorize("hasRole('Editor')")
    @ResponseBody
    public MetadataProcessingReport updateStatus(
        @ApiParam(
            value = ApiParams.API_PARAM_RECORD_UUIDS_OR_SELECTION,
            required = false)
        @RequestParam(required = false) String[] uuids,
        @ApiParam(
            value = ApiParams.API_PARAM_BUCKET_NAME,
            required = false)
        @RequestParam(
            required = false
        )
            String bucket,
        @ApiParam(
            value = "status",
            required = true
        )
        // TODO: RequestBody could be more appropriate ?
        @RequestParam(
            required = true
        )
            Integer status,
        @ApiParam(
            value = "comment",
            required = true
        )
        @RequestParam(
            required = true
        )
            String comment,
        @ApiParam(
            value = "publishGroups",
            required = false
        )
        @RequestParam(
            required = false
        )
            String publishGroups,
        @ApiParam(
            value = "editingGroups",
            required = false
        )
        @RequestParam(
            required = false
        )
            String editingGroups,
        HttpServletRequest request,
        @ApiIgnore
            HttpSession session
    ) throws Exception {
        MetadataProcessingReport report = new SimpleMetadataProcessingReport();

        try {
            Set<String> records = ApiUtils.getUuidsParameterOrSelection(uuids, bucket, ApiUtils.getUserSession(session));
            report.setTotalRecords(records.size());

            final ApplicationContext appContext = ApplicationContextHolder.get();
            final AccessManager am = appContext.getBean(AccessManager.class);
            Locale locale = languageUtils.parseAcceptLanguage(request.getLocales());
            final MetadataRepository metadataRepository = appContext.getBean(MetadataRepository.class);
            ServiceContext context = ApiUtils.createServiceContext(request, locale.getISO3Language());
            ISODate changeDate = new ISODate();

            //--- use StatusActionsFactory and StatusActions class to
            //--- change status and carry out behaviours for status changes
            StatusActionsFactory saf = appContext.getBean(StatusActionsFactory.class);

            StatusActions sa = saf.createStatusActions(context);

            Set<Integer> metadataIds = new HashSet<Integer>();
            for (String uuid : records) {
                Metadata info = metadataRepository.findOneByUuid(uuid);
                if (info == null) {
                    report.incrementNullRecords();
                //--- only allow the owner of the record to set its status
                } else if (!am.isOwner(context, String.valueOf(info.getId()))) {
                    report.addNotOwnerMetadataId(info.getId());
                } else {
                    metadataIds.add(info.getId());
                    report.incrementProcessedRecords();
                }
            }

            // now do the status change
            Pair<Set<Integer>,Set<Integer>> results = sa.statusChange(String.valueOf(status), metadataIds, changeDate, comment, publishGroups, editingGroups);

            appContext.getBean(IMetadataIndexer.class).indexMetadata(results.one());

        } catch (Exception exception) {
            report.addError(exception);
        } finally {
            report.close();
        }

        return report;
    }

}
