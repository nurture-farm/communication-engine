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

package farm.nurture.communication.engine.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class Template extends BaseModel<Short> {

    public enum ContentType {
        STRING, HTML
    }

    @JsonProperty("name")
    private String name;

    @JsonProperty("language_id")
    private Short languageId;

    @JsonProperty("content_type")
    private ContentType contentType;

    @JsonProperty("content")
    private String content;

    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("title")
    private String title;

    @JsonProperty("attributes")
    private Map<String, String> attributes = new HashMap<>();

    @JsonProperty("vertical")
    private String vertical;

    @JsonProperty("owner_email")
    private String ownerEmail;

    @JsonProperty("meta_data")
    private Map<String, Object> metaData = new HashMap<>();
}
