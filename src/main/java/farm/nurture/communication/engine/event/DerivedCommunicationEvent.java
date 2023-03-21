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

import farm.nurture.communication.engine.vendor.VendorType;
import farm.nurture.core.contracts.common.enums.*;
import farm.nurture.core.contracts.communication.engine.Placeholder;
import farm.nurture.kafka.Event;
import lombok.*;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DerivedCommunicationEvent<K, V> {

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Event<K, V> originalEvent;

    @Data
    @AllArgsConstructor
    public static class SMSAttributes {
        private String mobileNumber;
    }

    @Data
    @AllArgsConstructor
    public static class PNAttributes {
        private String title;

        private String appToken;

        private String apiKey;

        private PushNotificationType pushNotificationType;
    }

    @Data
    @AllArgsConstructor
    public static class EmailAttributes {
        private String emailId;

        private String subject;

        private List<String> toRecipients;

        private List<String> ccRecipients;

        private List<String> bccRecipients;
    }

    @Data
    @AllArgsConstructor
    public static class NotificationInboxAttributes {

        private String eventType;

        private List<Map<String,String> > placeholder;

        private String notificationInboxUUID;

        private String source;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WhatsappAttributes {
        private String mobileNumber;

        private Media media;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Media {
            private MediaType mediaType;

            private MediaAccessType mediaAccessType;

            private String mediaInfo;

            private String documentName;

        }
    }

    private String clientId;

    private String referenceId;

    private String content;

    private boolean isUnicode;

    private Timestamp sendAfter;

    private Timestamp expiry;

    private CommunicationChannel communicationChannel;

    private SMSAttributes smsAttributes;

    private PNAttributes PNAttributes;

    private EmailAttributes emailAttributes;

    private NotificationInboxAttributes notificationInboxAttributes;

    private WhatsappAttributes whatsappAttributes;

    private int retryCount = 0;

    private Short languageId;

    private String templateName;

    private Map<String, Object> placeholders;

    private String parentReferenceId;

    private String campaignName;

    private List<Placeholder> metaData;

    private VendorType vendor;

    private Map<String, String> attributesMap;

    public Map<String, String> vendorMetaData;

    private ActorType actorType;

    private Long actorId;

    public DerivedCommunicationEvent(Event originalEvent, String referenceId, String clientId, String content, boolean isUnicode, Timestamp sendAfter,
                                     Timestamp expiry, CommunicationChannel communicationChannel, Short languageId, String templateName,
                                     Map<String, Object> placeholders, List<Placeholder> metaData,
                                     Map<String, String> attributesMap,
                                     Map<String, String> vendorMetaData,
                                     String campaignName, Long actorId, ActorType actorType
    ) {
        this.originalEvent = originalEvent;
        this.referenceId = referenceId;
        this.clientId = clientId;
        this.content = content;
        this.isUnicode = isUnicode;
        this.sendAfter = sendAfter;
        this.expiry = expiry;
        this.communicationChannel = communicationChannel;
        this.languageId = languageId;
        this.templateName = templateName;
        this.placeholders = placeholders;
        this.metaData = metaData;
        this.attributesMap = attributesMap;
        this.vendorMetaData = vendorMetaData;
        this.campaignName = campaignName;
        this.actorId  = actorId;
        this.actorType = actorType;

    }
}
