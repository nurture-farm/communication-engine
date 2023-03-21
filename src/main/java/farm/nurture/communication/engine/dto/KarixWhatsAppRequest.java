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
import java.util.Map;


@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@SuperBuilder
public class KarixWhatsAppRequest {


    @JsonProperty("message")
    Message message;


    @JsonProperty("metaData")
    MetaData metaData;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Message {
        String channel;

        @JsonProperty("content")
        Content content;

        @JsonProperty("recipient")
        Recipient recipient;

        @JsonProperty("sender")
        Sender sender;

        @JsonProperty("preferences")
        Preferences preferences;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class MetaData {
        @JsonProperty("version")
        String version;
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
    public static class Preferences{
        @JsonProperty("webHookDNId")
        String webHookDNId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Content{
        @JsonProperty("preview_url")
        boolean preview_url;

        @JsonProperty("type")
        String type;

        @JsonProperty("template")
        Template template;

        @JsonProperty("mediaTemplate")
        MediaTemplate mediaTemplate;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Template{
        @JsonProperty("templateId")
        String templateId;

        @JsonProperty("parameterValues")
       Map<String, String> parameterValues;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class MediaTemplate{
        @JsonProperty("templateId")
        String templateId;

        @JsonProperty("bodyParameterValues")
        Map<String, String> bodyParameterValues;

        @JsonProperty("media")
        Media media;

        @JsonProperty("buttons")
        Buttons buttons;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Media{
        @JsonProperty("type")
        String type;
        @JsonProperty("url")
        String url;
        @JsonProperty("fileName")
        String fileName;
        @JsonProperty("title")
        String title;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Buttons{

        @JsonProperty("quickReplies")
        List<QuickReplies> quickReplies;
        @JsonProperty("actions")
        List<Actions> actions;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class QuickReplies{
        @JsonProperty("index")
         String index;
        @JsonProperty("payload")
         String payload;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Actions {
        @JsonProperty("index")
        String index;
        @JsonProperty("payload")
        String payload;
        @JsonProperty("type")
        String type;
    }

}