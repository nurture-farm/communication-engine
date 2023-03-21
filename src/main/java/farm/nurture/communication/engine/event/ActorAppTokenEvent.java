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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import farm.nurture.core.contracts.common.enums.ActorType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class ActorAppTokenEvent {

    public enum Action {
        CREATE, UPDATE
    }

    @NotNull
    private Long actorId;

    @NotNull
    private ActorType actorType;

    @NotNull
    private Short appId;

    @NotBlank
    private String fcmToken;

    private Boolean active;

    @NotNull
    private Action action;
}
