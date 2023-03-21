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
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
public class SmsCallbackRequest {

    @JsonProperty("response")
    private List<ResponseDetails> response;


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class ResponseDetails {
        @JsonProperty("srcAddr")
        String srcAddr;

        @JsonProperty("channel")
        String channel;

        @JsonProperty("externalId")
        String externalId;

        @JsonProperty("cause")
        String cause;

        @JsonProperty("errorCode")
        String errorCode;

        @JsonProperty("destAddr")
        String destAddr;

        @JsonProperty("eventType")
        String eventType;

        @JsonProperty("eventTs")
        Long eventTs;

        @JsonProperty("noOfFrags")
        Long noOfFrags;
    }
}
