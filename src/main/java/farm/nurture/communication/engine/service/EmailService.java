/*
 *  Copyright 2023 NURTURE AGTECH PVT LTD
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package farm.nurture.communication.engine.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.cache.TemplateCache;
import farm.nurture.communication.engine.cache.TemplateCacheKey;
import farm.nurture.communication.engine.cache.TemplateCacheValue;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.models.MessageAcknowledgement;
import farm.nurture.communication.engine.models.Template;
import farm.nurture.communication.engine.repository.MessageAcknowledgementRepository;
import farm.nurture.core.contracts.communication.engine.CommunicationEvent;
import farm.nurture.core.contracts.communication.engine.Placeholder;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.infra.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.activation.DataHandler;
import javax.mail.*;
import javax.mail.internet.*;
import javax.mail.util.ByteArrayDataSource;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Singleton
public class EmailService {

    public static final String vendor = "Google";

    @Inject
    private TemplateCache templateCache;

    @Inject
    private LanguageCache languageCache;

    @Inject
    private MessageAcknowledgementRepository messageAcknowledgementRepository;

    private static CloseableHttpClient client = HttpClientBuilder.create().build();
    private static final String PORT_587 = "587";
    private static final String PORT_465 = "465";
    private static String EMAIL_PORT = System.getenv("email_port");
    private static final String UNDERSCORE = "_";
    private static final String CARET = "^";
    private static final String EMPTY = "";
    static {
        if(StringUtils.isEmpty(EMAIL_PORT)) {
            EMAIL_PORT = PORT_587;
        }
    }

    private static final Properties mailServerProperties = System.getProperties();
    static {
        mailServerProperties.put("mail.smtp.port", EMAIL_PORT);
        if(EMAIL_PORT.equals(PORT_587)) {
            mailServerProperties.put("mail.smtp.starttls.enable", "true");
        } else if(EMAIL_PORT.equals(PORT_465)) {
            mailServerProperties.put("mail.smtp.ssl.enable", "true");
        }
        mailServerProperties.put("mail.smtp.auth", "true");
//        mailServerProperties.put("mail.smtp.starttls.enable", "true"); //works for 587
//        mailServerProperties.put("mail.smtp.ssl.enable", "true"); //works for 465
        mailServerProperties.put("mail.smtp.starttls.enable", "true");
        mailServerProperties.put("mail.smtp.starttls.required", "true");
        mailServerProperties.put("mail.smtp.ssl.protocols", "TLSv1.2");
        mailServerProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
    }
    private static Session mailSession = Session.getDefaultInstance(mailServerProperties, null);

    private static final String PROTOCOL = "smtp";
    private static final String TEXT_HTML = "text/html";
    private static final String ATTACHMENT_FILEURL = "attachment_fileurl";
    private static final String ATTACHMENT_FILENAME = "attachment_filename";
    private static final String ATTACHMENT_TYPE = "application/x-any";

    public void sendEmail(DerivedCommunicationEvent event) {
        log.info("Sending Email from event : {}", event);

        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "email_service", Constants.LABEL_TEMPLATE, Constants.LABEL_LANGUAGE);


        Transport transport = null;
        List<EmailAttachmentAttributes> emailAttachmentAttributes = null;
        try {
            emailAttachmentAttributes = getAttachmentAttributes(event);
            ApplicationConfiguration config = ApplicationConfiguration.getInstance();
            MimeMessage generateMailMessage = new MimeMessage(mailSession);
            generateMailMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(event.getEmailAttributes().getEmailId()));
            generateMailMessage.addRecipients(Message.RecipientType.TO,getEmailRecipients(event.getEmailAttributes().getToRecipients()));
            generateMailMessage.addRecipients(Message.RecipientType.CC,getEmailRecipients(event.getEmailAttributes().getCcRecipients()));
            generateMailMessage.addRecipients(Message.RecipientType.BCC,getEmailRecipients(event.getEmailAttributes().getBccRecipients()));
            generateMailMessage.setSubject(event.getEmailAttributes().getSubject());

            Multipart multipart = new MimeMultipart();

            MimeBodyPart textBodyPart = new MimeBodyPart();
            String emailBody = event.getContent();
            Template template = getTemplate(event);
            if(template.getContentType().equals(Template.ContentType.HTML)) {
                textBodyPart.setContent(emailBody, TEXT_HTML+"; charset=UTF-8");
            } else {
                textBodyPart.setText(emailBody,"UTF-8");
            }

            for(EmailAttachmentAttributes emailAttachmentAttribute: emailAttachmentAttributes) {
                MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                String attachmentName = emailAttachmentAttribute.getAttachmentName();
                String attachmentURL = emailAttachmentAttribute.getAttachmentURL();
                InputStream inputStream = emailAttachmentAttribute.getInputStream();
                if (StringUtils.isNonEmpty(attachmentName) && StringUtils.isNonEmpty(attachmentURL) && inputStream != null) {
                    attachmentBodyPart.setFileName(attachmentName);
                    ByteArrayDataSource bds = new ByteArrayDataSource(inputStream, ATTACHMENT_TYPE);
                    attachmentBodyPart.setDataHandler(new DataHandler(bds));
                    multipart.addBodyPart(attachmentBodyPart);
                }
            }

            multipart.addBodyPart(textBodyPart);
            generateMailMessage.setContent(multipart);

            transport = mailSession.getTransport(PROTOCOL);

            String host = config.get("emailService.email.host");
            String user = config.get("emailService.email.user");
            String password = config.get("emailService.email.password");
            transport.connect(host, user, password);
            transport.sendMessage(generateMailMessage, generateMailMessage.getAllRecipients());
            populateMessageAcknowledgements(event);
            log.info("Email sent successfully for event:{}",event);
            success = true;

        } catch (Exception e) {
            log.error("Error in sending Email for event : {}", event, e);

        } finally {
            closeQuietly(event, emailAttachmentAttributes, transport);
            tracker.stop(success,event.getTemplateName(), languageCache.getLanguageById(event.getLanguageId()).getName());
        }
    }

    private void closeQuietly(DerivedCommunicationEvent event, List<EmailAttachmentAttributes> emailAttachmentAttributes, Transport transport) {
        try {
            if (transport != null) transport.close();
            for(EmailAttachmentAttributes emailAttachmentAttribute: emailAttachmentAttributes)
            {
                closeInputStreamQuietly(emailAttachmentAttribute.getInputStream());
            }
        } catch (Exception e) {
            log.error("Unable to close transport or is for event : {}", event, e);
        }
    }

    private void closeInputStreamQuietly(InputStream is) {

        try {
            if (is != null) is.close();
        } catch (Exception e) {
            log.error("Unable to close inputstream", e);
        }
    }

    private Template getTemplate(DerivedCommunicationEvent event) {
        TemplateCacheKey cacheKey = new TemplateCacheKey(event.getTemplateName(),
                event.getLanguageId());
        TemplateCacheValue value = templateCache.getCompiledTemplateByCacheKey(cacheKey);
        return value.getTemplate();
    }

    private InputStream getAttachmentInputStream(String attachmentFileUrl) {

        InputStream is = null;
        try {
            HttpGet request = new HttpGet(attachmentFileUrl);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            is = entity.getContent();
        } catch (Exception e) {
            log.error("Error in downloading attachment file ", e);
        }
        return is;
    }

    private List<EmailAttachmentAttributes> getAttachmentAttributes(DerivedCommunicationEvent event) {

        List<EmailAttachmentAttributes> emailAttachmentAttributes = new ArrayList<>();
        Map<Integer,EmailAttachmentAttributes> attributesMap = new HashMap<>();
        List<Placeholder> metaData = event.getMetaData();
        if(metaData!= null) {
            metaData.forEach(placeholder -> processAttachmentAttributes(attributesMap, placeholder.getKey(), placeholder.getValue()));
        }
        attributesMap.forEach((key,emailAttachmentAttribute)-> emailAttachmentAttributes.add(emailAttachmentAttribute));
        return emailAttachmentAttributes;
    }

    private void processAttachmentAttributes(Map<Integer,EmailAttachmentAttributes> attributesMap, String key, String value) {

        String keyType = "";
        String attachmentURL = "";
        String attachmentName = "";
        InputStream is = null;

        if(key.startsWith(ATTACHMENT_FILEURL)) {
            keyType = ATTACHMENT_FILEURL;
            attachmentURL = value;
            is = getAttachmentInputStream(value);
        } else if(key.startsWith(ATTACHMENT_FILENAME)) {
            keyType = ATTACHMENT_FILENAME;
            attachmentName = value;
        }

        Integer index = getAttachmentIndex(key, keyType);
        if(attributesMap.containsKey(index))
        {
            EmailAttachmentAttributes emailAttachmentAttribute = attributesMap.get(index);
            if(StringUtils.isNonEmpty(attachmentURL)) emailAttachmentAttribute.setAttachmentURL(attachmentURL);
            if(is!= null) {
                if(emailAttachmentAttribute.getInputStream()!=null) closeInputStreamQuietly(emailAttachmentAttribute.getInputStream());
                emailAttachmentAttribute.setInputStream(is);
            }
            if(StringUtils.isNonEmpty(attachmentName)) emailAttachmentAttribute.setAttachmentName(attachmentName);
            attributesMap.put(index,emailAttachmentAttribute);
        }
        else
        {
            attributesMap.put(index,EmailAttachmentAttributes.builder().
                    attachmentURL(attachmentURL).inputStream(is).attachmentName(attachmentName).build());
        }
        return;
    }

    private Integer getAttachmentIndex(String key, String keyType) {

        Integer res;
        String keyReplaced = key.replaceFirst(CARET+keyType, EMPTY);
        String[] parts = keyReplaced.split(UNDERSCORE);

        if(parts.length==1)
            res = 0;
        else
            res = Integer.parseInt(parts[1]);
        return res;
    }

    private String populateMessageAcknowledgements(DerivedCommunicationEvent derivedCommunicationEvent) {

        CommunicationEvent commEvent = null;
        String referenceId = null;
        try {
            commEvent = CommunicationEvent.parseFrom((byte[]) derivedCommunicationEvent.getOriginalEvent().getMessage());
            referenceId = commEvent.getReferenceId();
        } catch (InvalidProtocolBufferException e) {
            log.error("Unable to deserialize protobuf in SMS service", e);
            return referenceId;
        }

        HashMap<String, Object> attributeMap = new HashMap<>();
        List<Placeholder> metaData = derivedCommunicationEvent.getMetaData();
        if(metaData!= null && metaData.size() > 0) {
            metaData.forEach(placeholder -> attributeMap.put("content_metadata_"+placeholder.getKey(), placeholder.getValue()));
        }

        MessageAcknowledgement messageAcknowledgement = MessageAcknowledgement.builder()
                .actorId(commEvent.getReceiverActor().getActorId())
                .actorType(commEvent.getReceiverActor().getActorType())
                .communicationChannel(Constants.EMAIL)
                .referenceId(referenceId)
                .tempateName(derivedCommunicationEvent.getTemplateName())
                .languageId(derivedCommunicationEvent.getLanguageId())
                .messageContent(derivedCommunicationEvent.getContent())
                .isUnicode(derivedCommunicationEvent.isUnicode())
                .vendorName(vendor)
                .vendorMessageId("")
                .state(MessageAcknowledgement.State.VENDOR_DELIVERED)
                .retryCount(derivedCommunicationEvent.getRetryCount())
                .placeHolders(derivedCommunicationEvent.getPlaceholders())
                .attributes(attributeMap)
                .parentReferenceId(derivedCommunicationEvent.getParentReferenceId())
                .campaignName(derivedCommunicationEvent.getCampaignName())
                .actorContactId(derivedCommunicationEvent.getEmailAttributes().getEmailId())
                .build();
        messageAcknowledgementRepository.insertMessageAcknowledgement(messageAcknowledgement);
        return referenceId;
    }

    private Address[] getEmailRecipients(List<String> Recipients) throws AddressException {

        if(Recipients!=null)
        {
            Address[] emailRecipients = new Address[Recipients.size()];
            for(int i=0;i<Recipients.size();i++){
                emailRecipients[i]=new InternetAddress(Recipients.get(i));
            }
            return emailRecipients;
        }
        return null;
    }
}
