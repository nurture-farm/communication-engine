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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunicationServiceCallbackRequest {

    public enum Status {
        /**
         * SENT: CUSTOMER_SENT / SENT (WHATSAPP)
         * SUBMITTED: SUBMITTED (SMS)
         * SUCCESS: CUSTOMER_DELIVERED / DELIVERED (WHATSAPP)
         * VIEW: CUSTOMER_READ / READ (WHATSAPP)
         * FAIL : FAIL
         * */
         SUCCESS, FAIL, UNKNOWN, SUBMITTED, VIEW, SENT
    }

    private String externalId;

    private Timestamp deliveredTS;

    private Status status;

    private String cause;

    private String phoneNo;

    private String errCode;

    private Long noOfFrags;

    private String vendorName;
}
