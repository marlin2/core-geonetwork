//=============================================================================
//===	Copyright (C) 2001-2011 Food and Agriculture Organization of the
//===	United Nations (FAO-UN), United Nations World Food Programme (WFP)
//===	and United Nations Environment Programme (UNEP)
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

package org.fao.geonet.kernel.metadata;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import jeeves.server.UserSession;
import jeeves.server.context.ServiceContext;
import org.fao.geonet.ApplicationContextHolder;
import org.fao.geonet.constants.Params;
import org.fao.geonet.domain.*;
import org.fao.geonet.events.md.MetadataPublishDraft;
import org.fao.geonet.kernel.datamanager.IMetadataManager;
import org.fao.geonet.kernel.datamanager.IMetadataOperations;
import org.fao.geonet.kernel.datamanager.IMetadataStatus;
import org.fao.geonet.kernel.datamanager.IMetadataUtils;
import org.fao.geonet.kernel.setting.SettingManager;
import org.fao.geonet.kernel.setting.Settings;
import org.fao.geonet.repository.MetadataRepository;
import org.fao.geonet.repository.SortUtils;
import org.fao.geonet.repository.StatusValueRepository;
import org.fao.geonet.repository.UserRepository;
import org.fao.geonet.util.MailSender;
import org.fao.geonet.util.XslUtil;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultStatusActions implements StatusActions {

    public static final Pattern metadataLuceneField = Pattern.compile("\\{\\{index:([^\\}]+)\\}\\}");
    protected ServiceContext context;
    protected String language;
    protected String siteUrl;
    protected String siteName;
    protected UserSession session;
    protected boolean emailNotes = true;
    private String host, port, username, password, from, fromDescr, replyTo, replyToDescr;
    private boolean useSSL;
    private boolean useTLS;
    private boolean ignoreSslCertificateErrors;
    private StatusValueRepository _statusValueRepository;

    private IMetadataManager mdManager;
    private IMetadataOperations mdOperations;
    private IMetadataStatus mdStatus;
    private IMetadataUtils mdUtils;

    private ApplicationEventPublisher eventPublisher;

    /**
     * Constructor.
     */
    public DefaultStatusActions() {
    }

    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.eventPublisher = applicationEventPublisher;
    }

    /**
     * Initializes the StatusActions class with external info from GeoNetwork.
     */
    public void init(ServiceContext context) throws Exception {

        this.context = context;
        ApplicationContext applicationContext = ApplicationContextHolder.get();
        this._statusValueRepository = applicationContext.getBean(StatusValueRepository.class);
        this.language = context.getLanguage();
       
        SettingManager sm = applicationContext.getBean(SettingManager.class);
        mdManager = applicationContext.getBean(IMetadataManager.class);
        mdOperations = applicationContext.getBean(IMetadataOperations.class);
        mdStatus = applicationContext.getBean(IMetadataStatus.class);
        mdUtils = applicationContext.getBean(IMetadataUtils.class);

        siteName = sm.getSiteName();
        host = sm.getValue(Settings.SYSTEM_FEEDBACK_MAILSERVER_HOST);
        port = sm.getValue(Settings.SYSTEM_FEEDBACK_MAILSERVER_PORT);
        from = sm.getValue(Settings.SYSTEM_FEEDBACK_EMAIL);
        username = sm.getValue(Settings.SYSTEM_FEEDBACK_MAILSERVER_USERNAME);
        password = sm.getValue(Settings.SYSTEM_FEEDBACK_MAILSERVER_PASSWORD);
        useSSL = sm.getValueAsBool(Settings.SYSTEM_FEEDBACK_MAILSERVER_SSL);
        useTLS = sm.getValueAsBool(Settings.SYSTEM_FEEDBACK_MAILSERVER_TLS);
        ignoreSslCertificateErrors = sm.getValueAsBool(Settings.SYSTEM_FEEDBACK_MAILSERVER_IGNORE_SSL_CERTIFICATE_ERRORS);

        if (host == null || host.length() == 0) {
            context.error("Mail server host not configure");
            emailNotes = false;
        }

        if (port == null || port.length() == 0) {
            context.error("Mail server port not configured, email notifications won't be sent.");
            emailNotes = false;
        }

        if (from == null || from.length() == 0) {
            context.error("Mail feedback address not configured, email notifications won't be sent.");
            emailNotes = false;
        }

        ResourceBundle messages = ResourceBundle.getBundle("org.fao.geonet.api.Messages", new Locale(this.language));
        fromDescr = siteName + messages.getString("status_email_title");

        session = context.getUserSession();
        replyTo = session.getEmailAddr();
        if (replyTo != null) {
            replyToDescr = session.getName() + " " + session.getSurname();
        } else {
            replyTo = from;
            replyToDescr = fromDescr;
        }

        siteUrl = sm.getSiteURL(context);
    }

    /**
     * Called when a record is edited.
     *
     * @param id        The metadata id that has been edited.
     * @param minorEdit If true then the edit was a minor edit.
     */
    public void onEdit(int id, boolean minorEdit) throws Exception {}

    // -------------------------------------------------------------------------
    // Private methods
    // -------------------------------------------------------------------------

    /**
     * Called when need to set status on a set of metadata records.
     *
     * @param status        The status to set.
     * @param metadataIds   The set of metadata ids to set status on.
     * @param changeDate    The date the status was changed.
     * @param changeMessage The message explaining why the status has changed.
     * @param publishGroups A set of groups associated with the status change.
     */
    public Set<Integer> statusChange(String status, Set<Integer> metadataIds, ISODate changeDate, String changeMessage, String publishGroups) throws Exception {

        Set<Integer> unchanged = new HashSet<Integer>();

        // -- get owners or content reviewers
        List<User> users = null;
        if (status.equals(Params.Status.SUBMITTED)) {
          users = getContentReviewers(metadataIds);
				} else if (status.equals(Params.Status.APPROVED)) {
          users = getOwners(metadataIds);
				}
           

        // -- process the metadata records to set status
        for (Integer mid : metadataIds) {
            String currentStatus = mdStatus.getCurrentStatus(mid);

            // --- if the status is already set to value of status then do nothing
            if (status.equals(currentStatus)) {
                if (context.isDebugEnabled())
                    context.debug("Metadata " + mid + " already has status " + mid);
                unchanged.add(mid);
            }

            if (status.equals(Params.Status.APPROVED)) {
                // set privileges for each group in the list of publishGroups
                setOperations(mid, publishGroups);
                // send an event so that the draft version will replace/become 
                // the approved version
                IMetadata metadata = mdManager.getMetadataObject(mid);
                this.eventPublisher.publishEvent(new MetadataPublishDraft(metadata));
                // make current user the owner of the approved record so
                // original users cannot delete it - get its id by uuid because
                // if draft-copy mid will no longer be present in draft-copy 
                // repo
                String approvedId = mdUtils.getMetadataId(metadata.getUuid());
               	mdManager.updateMetadataOwner(Integer.parseInt(approvedId), session.getUserId(), metadata.getSourceInfo().getGroupOwner()+""); 
            } else if (status.equals(Params.Status.REJECTED) || status.equals(Params.Status.RETIRED)) {
                unsetAllOperations(mid);
            }

            // --- set status, indexing is assumed to take place later
            mdStatus.setStatusExt(context, mid, Integer.valueOf(status), changeDate, changeMessage);
        }

        // --- inform content reviewers if the status is submitted
        if (status.equals(Params.Status.SUBMITTED)) {
            informContentReviewers(metadataIds, users, changeDate.toString(), changeMessage);
            // --- inform owners if status is approved
        } else if (status.equals(Params.Status.APPROVED) || status.equals(Params.Status.REJECTED)) {
            informOwners(metadataIds, users, changeDate.toString(), changeMessage, status);
        }

        return unchanged;
    }

    /**
     * Unset all operations on 'All' Group. Used when status changes from approved to 
     * something else.
     *
     * @param mdId The metadata id to unset privileges on
     */
    private void unsetAllOperations(int mdId) throws Exception {
        int allGroup = 1;
        for (ReservedOperation op : ReservedOperation.values()) {
            mdOperations.forceUnsetOperation(context, mdId, allGroup, op.getId());
        }
    }

    /**
     * Set all operations on the list of groups supplied. Used when status 
     * changes to approved from something else.
     *
     * @param mdId The metadata id to set privileges on
     * @param publishGroups The list of groups to set privileges for
     */
    private void setOperations(int mdId, String publishGroups) throws Exception {
        String[] publishGroupsList = publishGroups.split(",");
        for (int i = 0; i < publishGroupsList.length; i++) {
            int theGroup = Integer.parseInt(publishGroupsList[i]);
            if (context.isDebugEnabled())
                context.debug("Metadata " + mdId + " group " + theGroup + " set");
            System.out.println("XSMetadata " + mdId + " group " + theGroup + " set");
            for (ReservedOperation op : ReservedOperation.values()) {
                mdOperations.forceSetOperation(context, mdId, theGroup, op.getId());
            }
        }
    }

    /**
     * Get list of content reviewers
     *
     * @param metadata      The selected set of metadata records
     */
    private List<User> getContentReviewers(Set<Integer> metadata) throws Exception {

        // --- get content reviewers (sorted on content reviewer userid)
        UserRepository userRepository = context.getBean(UserRepository.class);
        List<Pair<Integer, User>> results = userRepository.findAllByGroupOwnerNameAndProfile(metadata, Profile.Reviewer, SortUtils.createSort(User_.name));

        List<User> users = Lists.transform(results, new Function<Pair<Integer, User>, User>() {
            @Nullable
            @Override
            public User apply(@Nonnull Pair<Integer, User> input) {
                return input.two();
            }
        });

        return users;
    }
     
    /**
     * Inform content reviewers of metadata records in list that they need to review the record.
     *
     * @param metadata      The selected set of metadata records
     * @param users         The list of users to send the emails to
     * @param changeDate    The date that of the change in status
     * @param changeMessage Message supplied by the user that set the status
     */
    protected void informContentReviewers(Set<Integer> metadata, List<User> users, String changeDate, String changeMessage) throws Exception {

        String mdChanged = buildMetadataChangedMessage(metadata);
        String translatedStatusName = getTranslatedStatusName(Params.Status.SUBMITTED);
        ResourceBundle messages = ResourceBundle.getBundle("org.fao.geonet.api.Messages", new Locale(this.language));
        String subject = String.format(messages.getString(
            "status_email_change_title"),
            siteName, translatedStatusName, replyToDescr, replyTo, changeDate
        );
        processList(users, subject, Params.Status.SUBMITTED,
            changeDate, changeMessage, mdChanged);
    }


    private String buildMetadataChangedMessage(Set<Integer> metadata) {
        String statusMetadataDetails = null;
        String message = "";
        ResourceBundle messages = ResourceBundle.getBundle("org.fao.geonet.api.Messages", new Locale(this.language));

        try {
            statusMetadataDetails = messages.getString("status_email_change_details");
        } catch (Exception e) {
        }
        // Fallback on a default value if statusMetadataDetails not resolved
        if (statusMetadataDetails == null) {
            statusMetadataDetails = "* {{index:title}} ({{serverurl}}/catalog.search#/metadata/{{index:_uuid}})";
        }

        ArrayList<String> fields = new ArrayList<String>();

        Matcher m = metadataLuceneField.matcher(statusMetadataDetails);
        Iterable<Metadata> mds = this.context.getBean(MetadataRepository.class).findAll(metadata);

        while (m.find()) {
            fields.add(m.group(1));
        }

        for (Metadata md : mds) {
            String curMdDetails = statusMetadataDetails;
            // First substitution for variables not stored in the index
            curMdDetails = curMdDetails.replace("{{serverurl}}", siteUrl);
            curMdDetails = compileMessageWithIndexFields(curMdDetails, md.getUuid(), this.language);
            message = message.concat(curMdDetails + "\r\n");
        }
        return message;
    }

    /**
     * Substitute lucene index field values in message.
     * Lucene field are identified using {{index:fieldName}} tag.
     *
     * @param message   The message to work on
     * @param uuid      The record UUID
     * @param language  The language (define the index to look into)
     * @return  The message with field substituted by values
     */
    public static String compileMessageWithIndexFields(String message, String uuid, String language) {
        // Search lucene field to replace
        Matcher m = metadataLuceneField.matcher(message);
        ArrayList<String> fields = new ArrayList<String>();
        while (m.find()) {
            fields.add(m.group(1));
        }

        // First substitution for variables not stored in the index
        for (String f : fields) {
            String mdf = XslUtil.getIndexField(null, uuid, f, language);
            message = message.replace("{{index:" + f + "}}", mdf);
        }
        return message;
    }

    private String getTranslatedStatusName(String statusValueId) {
        String translatedStatusName = "";
        StatusValue s = _statusValueRepository.findOneById(Integer.valueOf(statusValueId));
        if (s == null) {
            translatedStatusName = statusValueId;
        } else {
            translatedStatusName = s.getLabel(this.language);
        }
        return translatedStatusName;
    }

    /**
     * Get list of metadata owners from selected set of metadata records.
     *
     * @param metadataIds   The selected set of metadata records
     */
    private List<User> getOwners(Set<Integer> metadataIds) throws Exception {
        Iterable<Metadata> metadata = this.context.getBean(MetadataRepository.class).findAll(metadataIds);
        List<User> owners = new ArrayList<User>();
        UserRepository userRepo = this.context.getBean(UserRepository.class);

        for (Metadata md : metadata) {
            int ownerId = md.getSourceInfo().getOwner();
            owners.add(userRepo.findOne(ownerId));
        }

        return owners;
		}

    /**
     * Inform owners of metadata records that the records have approved or rejected.
     *
     * @param metadataIds   The selected set of metadata records
     * @param oeners        The list of metadata owners
     * @param changeDate    The date that of the change in status
     * @param changeMessage Message supplied by the user that set the status
     */
    protected void informOwners(Set<Integer> metadataIds, List<User> owners, String changeDate, String changeMessage, String status)
        throws Exception {

        String translatedStatusName = getTranslatedStatusName(status);
        // --- get metadata owners (sorted on owner userid)
        ResourceBundle messages = ResourceBundle.getBundle("org.fao.geonet.api.Messages", new Locale(this.language));
        String subject = String.format(messages.getString(
            "status_email_change_title"),
            siteName, translatedStatusName, replyToDescr, replyTo, changeDate
        );
        String mdChanged = buildMetadataChangedMessage(metadataIds);


        processList(owners, subject, status, changeDate, changeMessage, mdChanged);

    }

    /**
     * Process the users and metadata records for emailing notices.
     *
     * @param users         The selected set of users
     * @param subject       Subject to be used for email notices
     * @param status        The status being set
     * @param changeDate    Datestamp of status change
     * @param changeMessage The message indicating why the status has changed
     */
    protected void processList(List<User> users, String subject, String status, String changeDate,
                               String changeMessage, String mdChanged) throws Exception {

        for (User user : users) {
            sendEmail(user.getEmail(), subject, status, changeDate, changeMessage, mdChanged);
        }
    }

    /**
     * Send the email message about change of status on a group of metadata records.
     *
     * @param sendTo        The recipient email address
     * @param subject       Subject to be used for email notices
     * @param status        The status being set on the records
     * @param changeDate    Datestamp of status change
     * @param changeMessage The message indicating why the status has changed
     */
    protected void sendEmail(String sendTo, String subject, String status, String changeDate, String changeMessage, String mdChanged) throws Exception {
        ResourceBundle messages = ResourceBundle.getBundle("org.fao.geonet.api.Messages", new Locale(this.language));
        String message = String.format(messages.getString(
            "status_email_change_text"),
            changeMessage, mdChanged, siteUrl, status, changeDate);

        if (!emailNotes) {
            context.info("Would send email \nTo: " + sendTo + "\nSubject: " + subject + "\n Message:\n" + message);
        } else {
            MailSender sender = new MailSender(context);
            sender.sendWithReplyTo(host, Integer.parseInt(port), username, password, useSSL, useTLS,
                ignoreSslCertificateErrors, from, fromDescr, sendTo, null, replyTo, replyToDescr, subject, message);
        }
    }
}
