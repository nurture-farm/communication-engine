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
    public class WhatsAppCallbackRequest {

            public enum EventType {
                SENT, DELIVERED, READ, FAILED
            }

            public enum Cause {
                SUCCESS, SENT, READ, OTHER, UNKNOWN_SUBSCRIBER, DEFERRED, BLOCKED_FOR_USER
            }

            @JsonProperty("externalId")
            private String externalId;

            @JsonProperty("eventType")
            private EventType eventType;

            @JsonProperty("eventTs")
            private Long eventTs;

            @JsonProperty("destAddr")
            private String destAddr;

            @JsonProperty("srcAddr")
            private String srcAddr;

            @JsonProperty("cause")
            private Cause cause;

            @JsonProperty("errorCode")
            private String errorCode;

            @JsonProperty("channel")
            private String channel;
    }


