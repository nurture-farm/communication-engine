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

package farm.nurture.communication.engine;

import com.google.protobuf.Timestamp;
import farm.nurture.communication.engine.dto.WhatsAppOptUserResponse;
import farm.nurture.communication.engine.grpc.CommunicationEngineImpl;
import farm.nurture.communication.engine.helper.RequestMapper;
import farm.nurture.communication.engine.helper.RequestValidator;
import farm.nurture.communication.engine.helper.ResponseMapper;
import farm.nurture.communication.engine.models.MessageAcknowledgement;
import farm.nurture.communication.engine.models.WhatsappUsers;
import farm.nurture.communication.engine.repository.MessageAcknowledgementRepository;
import farm.nurture.communication.engine.repository.WhatsappUsersRepository;
import farm.nurture.communication.engine.service.OptUserService;
import farm.nurture.core.contracts.common.enums.ActorType;
import farm.nurture.core.contracts.common.enums.ResponseStatus;
import farm.nurture.core.contracts.common.enums.ResponseStatusCode;
import farm.nurture.core.contracts.communication.engine.MessageAcknowledgementRequest;
import farm.nurture.core.contracts.communication.engine.MessageAcknowledgementResponse;
import farm.nurture.core.contracts.communication.engine.OptInRequest;
import farm.nurture.core.contracts.communication.engine.OptInRespone;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith({H2Extension.class, MockitoExtension.class})
public class CommunicationEngineImplTest {

    @Spy
    private RequestValidator requestValidator;

    @Spy
    private RequestMapper requestMapper;

    @Spy
    private ResponseMapper responseMapper;

    @Spy
    private WhatsappUsersRepository whatsappUsersRepository;

    @Spy
    private MessageAcknowledgementRepository messageAcknowledgementRepository;

    @Mock
    private OptUserService optUserService;

    @InjectMocks
    private CommunicationEngineImpl communicationEngine;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testOptInUser(boolean userRegistered) throws Exception {
        WhatsAppOptUserResponse.ResponseDetails responseDetails = WhatsAppOptUserResponse.ResponseDetails.builder()
                .id("104").details("OPT_IN").status("success").build();
        OptInRequest optInRequest = OptInRequest.newBuilder().setMobileNumber("919078679034").build();

        if (userRegistered) {
            WhatsappUsers whatsappUsers = WhatsappUsers.builder().mobileNumber("919078679034").status(WhatsappUsers.WhatsAppStatus.OPT_IN)
                    .id(12L).build();

            when(whatsappUsersRepository.getByMobileNumberKey("919078679034")).thenReturn(whatsappUsers);

            OptInRespone optInUser = communicationEngine.optInUser(optInRequest);

            assertNotNull(optInUser);
            assertEquals(optInUser.getId(), whatsappUsers.getId());
            assertEquals(optInUser.getStatus(), ResponseStatus.SUCCESSFUL);
            assertEquals(optInUser.getStatusCode(), ResponseStatusCode.OK);
        } else {
            when(optUserService.whatsappOptUser("919078679034", WhatsappUsers.WhatsAppStatus.OPT_IN))
                    .thenReturn(WhatsAppOptUserResponse.builder().response(responseDetails).build());
            OptInRespone optInUser = communicationEngine.optInUser(optInRequest);
            assertNotNull(optInUser);
            assertEquals(optInUser.getStatus(), ResponseStatus.SUCCESSFUL);
            assertEquals(optInUser.getStatusCode(), ResponseStatusCode.OK);
            assertEquals(optInUser.getAttribsCount(), 2);
            assertNotNull(optInUser.getId());
        }
    }

    @Test
    public void testOptInUserFailed() throws Exception {
        WhatsAppOptUserResponse.ResponseDetails responseDetails = WhatsAppOptUserResponse.ResponseDetails.builder()
                .id("105").details("The phone number \"666\" is not a valid phone number").status("error").build();
        OptInRequest optInRequest = OptInRequest.newBuilder().setMobileNumber("919078679088").build();

        when(optUserService.whatsappOptUser("919078679088", WhatsappUsers.WhatsAppStatus.OPT_IN))
                .thenReturn(WhatsAppOptUserResponse.builder().response(responseDetails).build());

        OptInRespone optInUser = communicationEngine.optInUser(optInRequest);

        assertNotNull(optInUser);
        assertEquals(optInUser.getStatus(), ResponseStatus.ERROR);
        assertEquals(optInUser.getStatusCode(), ResponseStatusCode.NO_RESPONSE_STATUS_CODE);
        assertEquals(optInUser.getAttribsCount(), 3);
    }

    @Test
    public void testSearchMessageAcknowledgements() {
        java.sql.Timestamp timestamp = java.sql.Timestamp.valueOf("2021-04-29 18:14:45");
        MessageAcknowledgement messageAcknowledgement = MessageAcknowledgement.builder().actorId(233968L).actorType(ActorType.FARMER)
                .mobileNumber("7086880268").communicationChannel("SMS").referenceId("d9c634fa-9e6f-4a62-86bb-40d58a1f1df7")
                .tempateName("farmer_app_otp").messageContent("9017 आपका nurture.farm एप्लिकेशन OTP कोड है oCuDdxyOvb+").isUnicode(true)
                .vendorName("Gupshup").vendorMessageId("4283214995455242262-512233208777052101").state(MessageAcknowledgement.State.CUSTOMER_UNDELIVERED)
                .retryCount(0)
                .createdAt(timestamp).build();

        MessageAcknowledgementRequest messageAcknowledgementRequest=MessageAcknowledgementRequest.newBuilder()
                .setStartTime(Timestamp.newBuilder().setSeconds(1619700270).setNanos(805262000).build())
                .setEndTime(Timestamp.newBuilder().setSeconds(1619700330).setNanos(805331000).build())
//                .setChannel(CommunicationChannel.SMS)
                .setTemplateNameLike("%otp%").build();

        boolean isInserted = messageAcknowledgementRepository.insertMessageAcknowledgement(messageAcknowledgement);
        MessageAcknowledgementResponse response = communicationEngine.searchMessageAcknowledgements(messageAcknowledgementRequest);
        assertTrue(isInserted);
        assertNotNull(response);
    }
}
