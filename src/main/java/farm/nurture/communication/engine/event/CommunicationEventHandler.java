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

package farm.nurture.communication.engine.event;

import co.elastic.apm.api.CaptureTransaction;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.InvalidProtocolBufferException;
import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.cache.*;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent.EmailAttributes;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent.PNAttributes;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent.SMSAttributes;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent.WhatsappAttributes;
import farm.nurture.communication.engine.helper.TemplateHelper;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.*;
import farm.nurture.communication.engine.models.Language;
import farm.nurture.communication.engine.repository.ActorAppTokenRepository;
import farm.nurture.communication.engine.repository.ActorCommunicationDetailsRepository;
import farm.nurture.communication.engine.repository.MessageAcknowledgementRepository;
import farm.nurture.communication.engine.service.*;
import farm.nurture.communication.engine.utils.AESUtils;
import farm.nurture.communication.engine.utils.VendorLoadBalancer;
import farm.nurture.communication.engine.vendor.VendorType;
import farm.nurture.core.contracts.common.ActorID;
import farm.nurture.core.contracts.common.enums.*;
import farm.nurture.core.contracts.communication.engine.ActorDetails;
import farm.nurture.core.contracts.communication.engine.CommunicationEvent;
import farm.nurture.core.contracts.communication.engine.Media;
import farm.nurture.core.contracts.communication.engine.Placeholder;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.kafka.Consumer;
import farm.nurture.kafka.Event;
import farm.nurture.kafka.EventHandler;
import lombok.extern.slf4j.Slf4j;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Singleton
public class CommunicationEventHandler implements EventHandler<byte[], byte[]> {

    public static final AtomicInteger IN_FLIGHT_COUNTER = new AtomicInteger();
    private static final String DEFAULT_LANGUAGE_CODE = "hi-in";
    private static final Map<ActorType, List<Short>> actorTypeToAppIdMapping = new HashMap<>(5);

    @Inject
    private ActorCommunicationDetailsRepository actorCommDetailRepository;
    @Inject
    private ActorAppTokenRepository actorAppTokenRepository;
    @Inject
    private LanguageCache languageCache;
    @Inject
    private MobileAppDetailsCache appDetailsCache;
    @Inject
    private TemplateCache templateCache;
    @Inject
    private SMSService smsService;
    @Inject
    private PushNotificationService pushNotificationService;
    @Inject
    private EmailService emailService;
    @Inject
    private WhatsappService whatsappService;
    @Inject
    private MessageAcknowledgementRepository messageAcknowledgementRepository;
    @Inject
    private AESUtils aesUtils;
    @Inject
    private VendorLoadBalancer vendorLoadBalancer;
    private Metrics metrics = Metrics.getInstance();

    @Inject
    private TemplateHelper templateHelper;


    @CaptureTransaction
    @Override
    public Consumer.Status handle(String topic, Event<byte[], byte[]> event) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE, "send_comm_event");
        CommunicationEvent commEvent = null;

        Long actorId = null;
        ActorType actorType = null;
        List<Placeholder> metaData;
        try {
            commEvent = CommunicationEvent.parseFrom(event.getMessage());
            log.info("Communication Event {}", commEvent );
            ActorID actorID = commEvent.getReceiverActor();
            ActorDetails actorDetails = commEvent.getReceiverActorDetails();
            String actorMobileNumber = actorDetails.getMobileNumber();
            String actorEmail = actorDetails.getEmailId();
            List<String> toRecipients = actorDetails.getToRecipientsList();
            List<String> ccRecipients = actorDetails.getCcRecipientsList();
            List<String> bccRecipients = actorDetails.getBccRecipientsList();
            String actorFcmToken = actorDetails.getFcmToken();
            AppID appId = actorDetails.getAppId();
            AppType appType = actorDetails.getAppType();
            if (StringUtils.isEmpty(commEvent.getTemplateName())) {
                log.error("Invalid event. Template name must be passed.");
                metrics.onIncrement(MetricGroupNames.NF_CE_SEND_COMM_EVENT, "template_name_missing");
                return Consumer.Status.failure;
            }
            if ((actorID.getActorId() == 0 || actorID.getActorType() == ActorType.NO_ACTOR) && StringUtils.isEmpty(actorMobileNumber) && StringUtils.isEmpty(actorEmail) && StringUtils.isEmpty(actorFcmToken)) {
                log.error("Invalid actor details passed. ActorId or ActorDetails should be passed ");
                metrics.onIncrement(MetricGroupNames.NF_CE_SEND_COMM_EVENT, "actor_communication_details_missing", Constants.LABEL_TEMPLATE_LIST, commEvent.getTemplateName());
                return Consumer.Status.failure;
            }

            boolean hasMediaData = false;
            if (commEvent.hasMedia()) {
                Media commMedia = commEvent.getMedia();
                if (StringUtils.isEmpty(commMedia.getMediaInfo()) || commMedia.getMediaAccessType() == MediaAccessType.NO_MEDIA_ACCESS_TYPE
                        || commMedia.getMediaType() == MediaType.NO_MEDIA_TYPE || (MediaType.DOCUMENT == commMedia.getMediaType() && StringUtils.isEmpty(commMedia.getDocumentName()))) {
                    log.error("Not enough data present to send a media template notification on WhatsApp using Media : {}", commEvent.getMedia());
                    metrics.onIncrement(MetricGroupNames.NF_CE_SEND_COMM_EVENT, "media_data_missing", Constants.LABEL_TEMPLATE_LIST, commEvent.getTemplateName());
                    return Consumer.Status.failure;
                } else {
                    hasMediaData = true;
                }
            }

            boolean hasPnEvent = commEvent.getChannelList().contains(CommunicationChannel.APP_NOTIFICATION);
            if (hasPnEvent) {
                if (commEvent.hasReceiverActorDetails() && (StringUtils.isEmpty(actorDetails.getFcmToken()) ||
                                actorDetails.getAppId() == AppID.NO_APP_ID || actorDetails.getAppType() == AppType.NO_APP_TYPE)) {
                    log.error("Invalid Push notification event. Commevent: {}", commEvent);
                    metrics.onIncrement(MetricGroupNames.NF_CE_SEND_COMM_EVENT, "invalid_pn_event");
                    return Consumer.Status.failure;
                }
            }

            actorId = actorID.getActorId();
            actorType = actorID.getActorType();
            metaData = commEvent.getContentMetadataList();
            log.info("Got SendMessage event. ActorId : {}, ActorType: {}, Template : {}, Timestamp : {}, Headers : {}",
                    actorId, actorType, commEvent.getTemplateName(), event.getTimestamp(), event.getHeaders());

            if ((commEvent.getExpiry().getNanos() != 0 || commEvent.getExpiry().getSeconds() != 0) && (System.currentTimeMillis() / 1000) > commEvent.getExpiry().getSeconds()) {
                log.error("Could not send communication within expiry time : {} for ActorId : {}, ActorType: {}, Template : {}, Timestamp : {} commEventExpiry {}  systemTime {}",
                        commEvent.getExpiry(), actorId, actorType, commEvent.getTemplateName(), event.getTimestamp(), commEvent.getExpiry(), System.currentTimeMillis());
                metrics.onIncrement(MetricGroupNames.NF_CE_SEND_COMM_EVENT, "expired", Constants.LABEL_TEMPLATE_LIST, commEvent.getTemplateName());
                return Consumer.Status.failure;
            }

            ActorCommunicationDetails commDetails = null;
            if (StringUtils.isEmpty(actorMobileNumber) && StringUtils.isEmpty(actorEmail) && StringUtils.isEmpty(actorFcmToken)) {
                commDetails = actorCommDetailRepository.getByActorIdAndActorType(actorId, actorType);
                    if (commDetails == null) {
                        log.error("Not able to fetch actor communication details for ActorId : {}, ActorType: {}, Template : {}",
                                actorId, actorType, commEvent.getTemplateName());
                        metrics.onIncrement(MetricGroupNames.NF_CE_SEND_COMM_EVENT, "actor_communication_details_missing", Constants.LABEL_TEMPLATE_LIST, commEvent.getTemplateName());
                        return Consumer.Status.failure;
                    }
            }
            ActorAppToken token = null;
            MobileAppDetails appDetails = null;

            if (!StringUtils.isEmpty(actorMobileNumber) || !StringUtils.isEmpty(actorEmail) || !StringUtils.isEmpty(actorFcmToken))
                commDetails = new ActorCommunicationDetails(actorId, actorType, actorMobileNumber,
                        languageCache.getLanguageByCode(actorDetails.getLanguageCode().name().toLowerCase().replace('_', '-')).getId(), true);

            if (hasPnEvent) {
                List<Short> mobileAppDetailsIds;
                if (appId != AppID.NO_APP_ID && appType != AppType.NO_APP_TYPE) {
                    MobileAppDetailsCacheKey key = new MobileAppDetailsCacheKey(appId.name(), appType.name());
                    appDetails = appDetailsCache.getMobileAppDetailsByAppIdandAppType(key);
                } else {
                    mobileAppDetailsIds = actorTypeToAppIdMapping.get(actorType);
                    if (mobileAppDetailsIds != null && !mobileAppDetailsIds.isEmpty()) {
                        List<ActorAppToken> tokens = actorAppTokenRepository.getByActorAndMobileApp(actorId, actorType, mobileAppDetailsIds);
                        token = tokens == null || tokens.isEmpty() ? null : tokens.get(0);
                        appDetails = appDetailsCache.getMobileAppDetailsById(token == null ? null : token.getMobileAppDetailsId());

                        if (token == null || appDetails == null) {
                            log.error("Not able to fetch actor app token or mobile app details for ActorId : {}, ActorType: {}, mobileAppDetailsId : {}",
                                    actorId, actorType, mobileAppDetailsIds);
                            metrics.onIncrement(MetricGroupNames.NF_CE_SEND_COMM_EVENT, "app_token_or_app_details_missing", Constants.LABEL_TEMPLATE_LIST, commEvent.getTemplateName());

                            if (commEvent.getChannelList().size() == 1) {
                                return Consumer.Status.failure;
                            }
                        }

                    } else {
                        log.error("Actor type to app mapping missing for communicationEvent: {}, actorType: {} ", commEvent, actorType.toString());
                        metrics.onIncrement(MetricGroupNames.NF_CE_SEND_COMM_EVENT, "actor_type_to_app_mapping_missing", Constants.LABEL_TEMPLATE_ACTORTYPE_LIST, commEvent.getTemplateName(), actorType.toString());
                        return Consumer.Status.failure;
                    }
                }
            }

            Language primaryLanguage = commDetails.getLanguageId() != null && commDetails.getLanguageId() != 0 ?
                    languageCache.getLanguageById(commDetails.getLanguageId()) :
                    null;
            Language secondaryLanguage = null;

            if (actorDetails.getSecondaryLanguageCode()!=null && actorDetails.getSecondaryLanguageCode()!=LanguageCode.NO_LANGUAGE_CODE) {
                secondaryLanguage = languageCache.getLanguageByCode(actorDetails.getSecondaryLanguageCode().name().toLowerCase().replace('_', '-'));
            }

            Map<String, String> placeholders = templateHelper.getPlaceHolders(commEvent.getPlaceholderList());
            TemplateCacheValue templateCacheValue = templateHelper.getTemplateFromNameAndLangId(commEvent.getTemplateName(), primaryLanguage,secondaryLanguage);
            primaryLanguage =  languageCache.getLanguageById(templateCacheValue.getTemplate().getLanguageId());

            String content = templateHelper.getContent(templateCacheValue, placeholders);
            String title = templateHelper.getTitle(templateCacheValue, placeholders);
            Map<String, String> attributesMap = templateHelper.getAttributes(templateCacheValue, placeholders);
            Map<String, Object> vendorMetaDataMap = templateHelper.getMetaData(templateCacheValue);
            if (StringUtils.isEmpty(title)) {
                title = commEvent.getContentTitle();
            }
            Timestamp sendAfter = commEvent.getSendAfter() == null ? null : new Timestamp(commEvent.getSendAfter().getNanos() / 1000000);
            Timestamp expiry = commEvent.getExpiry() == null ? null : new Timestamp(commEvent.getExpiry().getNanos() / 1000000);

            List<CommunicationChannel> channels = commEvent.getChannelList();
            for (CommunicationChannel chanel : channels) {
                DerivedCommunicationEvent derivedEvent = new DerivedCommunicationEvent(event, commEvent.getReferenceId(), commEvent.getClientId(), content, primaryLanguage != null ? primaryLanguage.getUnicode() : false,
                        sendAfter, expiry, chanel, primaryLanguage.getId(), commEvent.getTemplateName(), placeholders, metaData, attributesMap, vendorMetaDataMap,
                        commEvent.getCampaignName(), commEvent.getReceiverActor() !=null? commEvent.getReceiverActor().getActorId() : 0,
                        commEvent.getReceiverActor()!=null?commEvent.getReceiverActor().getActorType() : ActorType.NO_ACTOR
                );
                if (!StringUtils.isEmpty(commEvent.getParentReferenceId())) {
                    derivedEvent.setParentReferenceId(commEvent.getParentReferenceId());
                }
                if (!StringUtils.isEmpty(commEvent.getCampaignName())) {
                    derivedEvent.setCampaignName(commEvent.getCampaignName());
                }

                switch (chanel) {
                    case SMS:
                        derivedEvent.setSmsAttributes(new SMSAttributes(commDetails.getMobileNumber()));
                        derivedEvent.setVendor(getVendor(commEvent.getVendor(), CommunicationChannel.SMS.name()));
                        smsService.sendSms(derivedEvent);
                        success = true;
                        break;

                    case APP_NOTIFICATION:
                        if ((token != null || !actorFcmToken.isEmpty()) && appDetails != null) {
                            String fcmToken;
                            if (token == null) fcmToken = actorFcmToken;
                            else fcmToken = token.getFcmToken();
                            PushNotificationType pushNotificationType = PushNotificationType.NO_PUSH_NOTIFICATION_TYPE;
                            if (commEvent.getChannelAttributes() != null)
                                pushNotificationType = commEvent.getChannelAttributes().getPushNotificationType();
                            derivedEvent.setPNAttributes(new PNAttributes(title, fcmToken, appDetails.getFcmApiKey(), pushNotificationType));
                            derivedEvent.setVendor(VendorType.FIREBASE);
                            pushNotificationService.sendPushNotification(derivedEvent);
                            success = true;
                        }
                        break;

                    case EMAIL:
                        derivedEvent.setEmailAttributes(new EmailAttributes(actorEmail, title,toRecipients,ccRecipients,bccRecipients));
                        emailService.sendEmail(derivedEvent);
                        success = true;
                        break;

                    case WHATSAPP:
                        WhatsappAttributes.WhatsappAttributesBuilder whatsappAttributesBuilder = WhatsappAttributes.builder().mobileNumber(commDetails.getMobileNumber());
                        if (hasMediaData) {
                            log.info("Sending a media template notification on WhatsApp using Media : {}", commEvent.getMedia());
                            Media commEventMedia = commEvent.getMedia();
                            whatsappAttributesBuilder.media(WhatsappAttributes.Media.builder().mediaType(commEventMedia.getMediaType())
                                    .mediaAccessType(commEventMedia.getMediaAccessType())
                                    .mediaInfo(commEventMedia.getMediaInfo())
                                    .documentName(commEventMedia.getDocumentName()).build());
                        }
                        derivedEvent.setWhatsappAttributes(whatsappAttributesBuilder.build());
                        derivedEvent.setVendor(getVendor(commEvent.getVendor(), CommunicationChannel.WHATSAPP.name()));
                        whatsappService.sendMessage(derivedEvent);
                        success = true;
                        break;

                    default:
                        metrics.onIncrement(MetricGroupNames.NF_CE_SEND_COMM_EVENT, "communication_chanel_not_supported");
                        log.error("Communication chanel : {} is not supported yet", chanel);
                }
            }

        } catch (InvalidProtocolBufferException e) {
            metrics.onIncrement(MetricGroupNames.NF_CE_SEND_COMM_EVENT, "protobuf_deserialization_failed", Constants.LABEL_TEMPLATE_LIST, commEvent.getTemplateName());
            log.error("Unable to deserialize protobuf send commEvent for topic : {}", topic, e);

        } catch (Exception e) {
            if (commEvent != null) {
                log.error("Unable to process send commEvent event for topic : {}, ActorId : {}, ActorType: {}, Template : {}",
                        topic, actorId, actorType, commEvent.getTemplateName(), e);
            } else {
                log.error("Unable to process send commEvent event for topic : {}", topic, e);
            }

        } finally {
            tracker.stop(success);

            ApplicationConfiguration appConfig = ApplicationConfiguration.getInstance();
            int inflightEventThreshold = appConfig.getInt("inflight.event.threshold", 10);
            while (IN_FLIGHT_COUNTER.get() >= inflightEventThreshold) {
                try {
                    Thread.sleep(100);

                } catch (InterruptedException e) {
                    log.error("Error in Thread.sleep in communication event handler", e);
                }
            }
        }

        return success ? Consumer.Status.success : Consumer.Status.failure;
    }


    private VendorType getVendor(CommunicationVendor vendor, String channel) {
        VendorType vendorType;
        if (vendor == CommunicationVendor.GUPSHUP) {
            vendorType = VendorType.GUPSHUP;
        } else if (vendor == CommunicationVendor.KARIX) {
            vendorType = VendorType.KARIX;
        } else {
            vendorType = vendorLoadBalancer.getVendor(channel);
        }
        return vendorType;

    }
}
