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

package farm.nurture.communication.engine.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
public class KarixWhatsAppCallbackRequest {

    @JsonProperty("channel")
    String channel;


    @JsonProperty("appDetails")
    AppDetails appDetails;

    @JsonProperty("recipient")
    Recipient recipient;

    @JsonProperty("sender")
    Sender sender;

    @JsonProperty("events")
    Events events;

    @JsonProperty("notificationAttributes")
    NotificationAttributes notificationAttributes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class AppDetails{
        @JsonProperty("type")
        String type;
        @JsonProperty("id")
        String id;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Recipient{
        @JsonProperty("to")
        String to;
        @JsonProperty("recipient_type")
        String recipient_type;
        @JsonProperty("reference")
        Reference reference;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Reference{
        @JsonProperty("cust_ref")
        String cust_ref;
        @JsonProperty("messageTag1")
        String messageTag1;
        @JsonProperty("conversationId")
        String conversationId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Sender{
        @JsonProperty("from")
        String from;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Events {
        @JsonProperty("eventType")
        String eventType;
        @JsonProperty("timestamp")
        Long timestamp;
        @JsonProperty("date")
        String date;
        @JsonProperty("mid")
        String mid;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class NotificationAttributes {
        @JsonProperty("status")
        String status;
        @JsonProperty("reason")
        String reason;
        @JsonProperty("code")
        String code;
    }
}
