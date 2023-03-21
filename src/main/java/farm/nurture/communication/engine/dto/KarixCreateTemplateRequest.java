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
public class KarixCreateTemplateRequest {

    @JsonProperty("template_name")
    private String template_name;

    @JsonProperty("language")
    private String language;

    @JsonProperty("category")
    private String category;

    @JsonProperty("components")
    List<Components> components;


    @JsonProperty("webhook")
    Webhook webhook;

    /**
     *   {
     *        type:BUTTONS,
     *        buttons:[{type:PHONE_NUMBER,text:{phone-button-text},phone_number:+1(650) 555-1111}],
     *      },
     *      {
     *        type:URL,
     *        text:{url-button-text},
     *        url:https://www.website.com/{{1}},
     *        example:[https://www.website.com/dynamic-url-example]
     *      }
     *{"type":"URL","text":"Visit Us",    "url":"www.nurture.farm",    "example":  ["https://www.nurture.farm"]   }]}]}

     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Components {
        @JsonProperty("type")
        private String type;

        @JsonProperty("text")
        private String text;

        @JsonProperty("format")
        private String format;

        @JsonProperty("example")
        private Example example;

        @JsonProperty("buttons")
        private List<Button> buttons;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Button{
        @JsonProperty("type")
        private String type;

        @JsonProperty("text")
        private String text;

        @JsonProperty("phone_number")
        private String phone_number;

        @JsonProperty("url")
        private String url;

        @JsonProperty("example")
        private List<String> example;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Webhook{
        @JsonProperty("url")
        private String url;
    }


    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    @SuperBuilder
    public static class Example{
        @JsonProperty("header_handle")
        private List<String> header_handle;

        @JsonProperty("header_text")
        private List<String> header_text;

        @JsonProperty("body_text")
        private List<List<String>> body_text;
    }

}
