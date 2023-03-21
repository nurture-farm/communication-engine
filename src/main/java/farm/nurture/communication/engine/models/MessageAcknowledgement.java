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

import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.core.contracts.common.enums.ContactType;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class MessageAcknowledgement extends BaseModel<Long>  {

    public enum State {
        VENDOR_DELIVERED, VENDOR_UNDELIVERED, CUSTOMER_DELIVERED, CUSTOMER_UNDELIVERED, PROCESSING_FAILED, CUSTOMER_READ, CUSTOMER_SENT, INCOMING_REPLY
    }

    private String vendorName;

    private Integer retryCount;

    private Timestamp vendorDeliveryTime;

    private Timestamp actorDeliveryTime;

    private Long actorId;

    private ActorType actorType;

    private String mobileNumber;

    private String communicationChannel;

    private String tempateName;

    private Short languageId;

    private String messageContent;

    private Boolean isUnicode;

    private String referenceId;

    private String vendorMessageId;

    private State state;

    private Map<String,Object> attributes;

    private Map<String,Object> placeHolders;

    private ContactType contactType;

    private String actorContactId;

    private String parentReferenceId;

    private String campaignName;

    private Integer version;

    private Date createdDate;
}