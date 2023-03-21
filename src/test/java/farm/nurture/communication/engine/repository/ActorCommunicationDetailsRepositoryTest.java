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
import farm.nurture.communication.engine.models.ActorCommunicationDetails;
import farm.nurture.core.contracts.common.enums.ActorType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith({H2Extension.class})
public class ActorCommunicationDetailsRepositoryTest {
    private static final String mobileNumber="9999999999";
    private static final long actorId = 1l;
    private static final Short languageId = 0;
    private static final boolean activeStatus = true;


    private final ActorCommunicationDetailsRepository actorCommunicationDetailsRepository = new ActorCommunicationDetailsRepository();

    @ParameterizedTest
    @ValueSource(strings =  {"insert", "update"})
    public void testInsertActorCommunicationDetails(String operation) {
        ActorCommunicationDetails communicationDetails = ActorCommunicationDetails.builder().actorId(actorId)
                .actorType(ActorType.FARMER).mobileNumber(mobileNumber).languageId(languageId).active(activeStatus).build();
        if (operation.equals("insert")) {
            actorCommunicationDetailsRepository.insertActorCommunicationDetails(communicationDetails);
        } else {
            actorCommunicationDetailsRepository.updateActorCommunicationDetails(communicationDetails);
        }

        ActorCommunicationDetails details = actorCommunicationDetailsRepository.getByActorIdAndActorType(actorId, ActorType.FARMER);
        assertEquals(actorId, details.getActorId());
        assertEquals(ActorType.FARMER, details.getActorType());
        assertEquals(mobileNumber, details.getMobileNumber());
        assertEquals(languageId, details.getLanguageId());
        assertTrue(details.getActive());
    }

    @Test
    public void testUpdateNonExistingActorCommunicationDetails() {
        ActorCommunicationDetails communicationDetails = ActorCommunicationDetails.builder().actorId(actorId)
                .actorType(ActorType.OPERATOR).mobileNumber(mobileNumber).languageId(languageId).active(activeStatus).build();

        actorCommunicationDetailsRepository.updateActorCommunicationDetails(communicationDetails);
        ActorCommunicationDetails details = actorCommunicationDetailsRepository.getByActorIdAndActorType(actorId, ActorType.OPERATOR);
        assertEquals(actorId, details.getActorId());
        assertEquals(ActorType.OPERATOR, details.getActorType());
        assertEquals(mobileNumber, details.getMobileNumber());
        assertEquals(languageId, details.getLanguageId());
        assertTrue(details.getActive());
    }

    @Test
    public void testExceptionInInsertActorCommunicationDetails() {
        ActorCommunicationDetails communicationDetails = Mockito.mock(ActorCommunicationDetails.class);
        when(communicationDetails.getActorId()).thenThrow(new RuntimeException("Exception"));

        actorCommunicationDetailsRepository.insertActorCommunicationDetails(communicationDetails);
        ActorCommunicationDetails details = actorCommunicationDetailsRepository.getByActorIdAndActorType(actorId, ActorType.EXTENSION_MANAGER);
        assertNull(details);
    }

    @Test
    public void testExceptionInUpdateActorCommunicationDetails() {
        ActorCommunicationDetailsRepository actorCommunicationDetailsRepository1 = Mockito.spy(actorCommunicationDetailsRepository);
        ActorCommunicationDetails communicationDetails = Mockito.mock(ActorCommunicationDetails.class);
        when(communicationDetails.getActorId()).thenReturn(actorId);
        when(communicationDetails.getActorType()).thenReturn(ActorType.SUPPORT_AGENT);
        when(actorCommunicationDetailsRepository1.getByActorIdAndActorType(actorId, ActorType.SUPPORT_AGENT)).thenReturn(communicationDetails);
        when(communicationDetails.getMobileNumber()).thenThrow(new RuntimeException("Exception"));

        actorCommunicationDetailsRepository1.updateActorCommunicationDetails(communicationDetails);
        ActorCommunicationDetails details = actorCommunicationDetailsRepository.getByActorIdAndActorType(actorId, ActorType.SUPPORT_AGENT);
        assertNull(details);
    }

    @Test
    public void testGetByMobileNumberType() {
        ActorCommunicationDetails communicationDetails = ActorCommunicationDetails.builder().actorId(actorId)
                .actorType(ActorType.FARMER).mobileNumber(mobileNumber).languageId(languageId).active(activeStatus).build();
        actorCommunicationDetailsRepository.insertActorCommunicationDetails(communicationDetails);
        ActorCommunicationDetailsRepository.ResponseObject responseObject = actorCommunicationDetailsRepository.getByMobileNumberType(mobileNumber);
        assertTrue(responseObject.success);
        assertEquals(responseObject.actorCommunicationDetails.getActorId(), actorId);
        assertEquals(ActorType.FARMER, responseObject.actorCommunicationDetails.getActorType());
        assertEquals(mobileNumber, responseObject.actorCommunicationDetails.getMobileNumber());
        assertEquals(languageId, responseObject.actorCommunicationDetails.getLanguageId());
        assertTrue(responseObject.actorCommunicationDetails.getActive());
    }
}
