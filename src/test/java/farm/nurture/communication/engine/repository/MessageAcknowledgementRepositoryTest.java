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

package farm.nurture.communication.engine.repository;

import farm.nurture.communication.engine.H2Extension;
import farm.nurture.communication.engine.models.MessageAcknowledgement;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.core.contracts.common.enums.CommunicationChannel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith({H2Extension.class})
public class MessageAcknowledgementRepositoryTest {

    private final MessageAcknowledgementRepository messageAcknowledgementRepository = new MessageAcknowledgementRepository();

    @Test
    public void getMessageAcknowledgementByDurationTest() {
        java.sql.Timestamp timestamp = Timestamp.valueOf("2021-04-29 18:14:45");
        MessageAcknowledgement messageAcknowledgement = MessageAcknowledgement.builder().actorId(233968L).actorType(ActorType.FARMER)
                .mobileNumber("7086880268").communicationChannel("SMS").referenceId("d9c634fa-9e6f-4a62-86bb-40d58a1f1df7")
                .tempateName("farmer_app_otp").messageContent("9017 आपका nurture.farm एप्लिकेशन OTP कोड है oCuDdxyOvb+").isUnicode(true)
                .vendorName("Gupshup").vendorMessageId("4283214995455242262-512233208777052101").state(MessageAcknowledgement.State.CUSTOMER_UNDELIVERED)
                .retryCount(0).campaignName(null).parentReferenceId(null)
                .createdAt(timestamp).build();

        Timestamp startTime = Timestamp.valueOf("2021-04-29 18:14:30");
        Timestamp endTime = Timestamp.valueOf("2021-04-29 18:15:30");
        String communicationChannel = "SMS";
        String templateLike = "%otp%";

        boolean isInserted = messageAcknowledgementRepository.insertMessageAcknowledgement(messageAcknowledgement);
        List<CommunicationChannel> channels = new ArrayList<>();
        channels.add(CommunicationChannel.valueOf(communicationChannel));
        List<MessageAcknowledgement> acknowledgements = messageAcknowledgementRepository.getMessageAcknowledgementByDuration(channels,
                new java.sql.Timestamp(0), new java.sql.Timestamp(0), null, null, null, 0, null ,null);
        assertTrue(isInserted);
        assertNotNull(acknowledgements);
    }
}
