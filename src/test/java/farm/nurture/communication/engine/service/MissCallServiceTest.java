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

package farm.nurture.communication.engine.service;

import farm.nurture.communication.engine.H2Extension;
import farm.nurture.communication.engine.models.ActorAttributes;
import farm.nurture.communication.engine.models.ActorCommunicationDetails;
import farm.nurture.communication.engine.models.WhatsappUsers;
import farm.nurture.communication.engine.repository.ActorAttributesRepository;
import farm.nurture.communication.engine.repository.ActorCommunicationDetailsRepository;
import farm.nurture.communication.engine.repository.WhatsappUsersRepository;
import farm.nurture.communication.engine.resource.MissCallResource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
@ExtendWith({H2Extension.class})
public class MissCallServiceTest {
    private String senderMobile = "9999999999";

    @Mock
    private ActorCommunicationDetailsRepository actorCommunicationDetailsRepository;

    @Mock
    private WhatsappUsersRepository whatsappUsersRepository;

    @Mock
    private ActorAttributesRepository actorAttributesRepository;

    @Mock
    private ActorCommunicationDetails actorCommunicationDetails;

    @InjectMocks
    private MissCallService missCallService;

    @Test
    public void testUpdateWhatsAppStatusSuccess() {
        ActorCommunicationDetailsRepository.ResponseObject responseObject = new ActorCommunicationDetailsRepository().new ResponseObject(actorCommunicationDetails, true);
        when(actorCommunicationDetailsRepository.getByMobileNumberType(senderMobile)).thenReturn(responseObject);
        when(actorAttributesRepository.updateActorAttributes(any(ActorAttributes.class))).thenReturn(true);
        boolean isUpdated = missCallService.updateWhatsAppStatus(senderMobile, MissCallResource.Status.OPT_IN);
        assertTrue(isUpdated);
    }

    @Test
    public void testUpdateWhatsAppStatusFailure() {
        ActorCommunicationDetailsRepository.ResponseObject responseObject = new ActorCommunicationDetailsRepository().new ResponseObject(actorCommunicationDetails, false);
        when(actorCommunicationDetailsRepository.getByMobileNumberType(senderMobile)).thenReturn(responseObject);
        boolean isUpdated = missCallService.updateWhatsAppStatus(senderMobile, MissCallResource.Status.OPT_OUT);
        assertFalse(isUpdated);
    }

    @Test
    public void testActorCommunicationDetailsNullForOptIn() {
        ActorCommunicationDetailsRepository.ResponseObject responseObject = new ActorCommunicationDetailsRepository().new ResponseObject(null, true);
        when(actorCommunicationDetailsRepository.getByMobileNumberType(senderMobile)).thenReturn(responseObject);
        when(whatsappUsersRepository.findAndUpdateWhatsappUsers(any(WhatsappUsers.class))).thenReturn(true);
        boolean isUpdated = missCallService.updateWhatsAppStatus(senderMobile, MissCallResource.Status.OPT_IN);
        assertTrue(isUpdated);
    }

    @Test
    public void testActorCommunicationDetailsNullForOptOut() {
        ActorCommunicationDetailsRepository.ResponseObject responseObject = new ActorCommunicationDetailsRepository().new ResponseObject(null, true);
        when(actorCommunicationDetailsRepository.getByMobileNumberType(senderMobile)).thenReturn(responseObject);
        when(whatsappUsersRepository.findAndUpdateWhatsappUsers(any(WhatsappUsers.class))).thenReturn(true);
        boolean isUpdated = missCallService.updateWhatsAppStatus(senderMobile, MissCallResource.Status.OPT_OUT);
        assertTrue(isUpdated);
    }
}
