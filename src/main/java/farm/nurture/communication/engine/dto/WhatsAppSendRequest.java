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

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
public class WhatsAppSendRequest {
    @JsonProperty("mobileNumber")
    String mobileNumber;

    @JsonProperty("languageCode")
    String languageCode;

    @JsonProperty("templateName")
    String templateName;

    @JsonProperty("userId")
    Long userId;

    @JsonProperty("userType")
    String userType;

    @JsonProperty("campaignName")
    String campaignName;

    @JsonProperty("media")
    Media media;

    @JsonProperty("placeholders")
    Map<String, String> placeholders;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Media{

        @JsonProperty("media_info")
        String mediaInfo;

        @JsonProperty("media_access_type")
        String mediaAccessType;

        @JsonProperty("media_type")
        String mediaType;

        @JsonProperty("document_name")
        String documentName;

    }
}
