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
import farm.nurture.communication.engine.models.WhatsappUsers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

@ExtendWith({H2Extension.class})
public class WhatsappUsersRepositoryTest {
    private static final String mobileNumber = "9029059263";

    private final WhatsappUsersRepository whatsappUsersRepository = new WhatsappUsersRepository();

    private WhatsappUsers whatsappUsers = null;

    @BeforeEach
    public void setUp() {
        whatsappUsers = WhatsappUsers.builder().mobileNumber(mobileNumber).status(WhatsappUsers.WhatsAppStatus.OPT_IN)
                .optOutConsentSent(false).build();
    }

    @Test
    public void testInsertWhatsappUsers() {
        boolean isInserted = whatsappUsersRepository.insertWhatsappUsers(whatsappUsers);

        WhatsappUsers response = whatsappUsersRepository.getByMobileNumberKey(mobileNumber);
        assertTrue(isInserted);
        assertEquals(mobileNumber, response.getMobileNumber());
        assertEquals(WhatsappUsers.WhatsAppStatus.OPT_IN, response.getStatus());
        assertEquals(false, response.getOptOutConsentSent());
    }

    @Test
    public void testUpdateExistingWhatsappUsersStatus() {
        whatsappUsersRepository.insertWhatsappUsers(whatsappUsers);
        final WhatsappUsers.WhatsAppStatus updatedStatusValue = WhatsappUsers.WhatsAppStatus.OPT_OUT;
        whatsappUsers.setStatus(updatedStatusValue);
        boolean isUpdated = whatsappUsersRepository.findAndUpdateWhatsappUsers(whatsappUsers);

        WhatsappUsers response = whatsappUsersRepository.getByMobileNumberKey(mobileNumber);
        assertTrue(isUpdated);
        assertEquals(mobileNumber, response.getMobileNumber());
        assertEquals(WhatsappUsers.WhatsAppStatus.OPT_OUT, response.getStatus());
        assertEquals(false, response.getOptOutConsentSent());
    }

    @Test
    public void testUpdateExistingWhatsappUsersOptOut() {
        whatsappUsersRepository.insertWhatsappUsers(whatsappUsers);
        final boolean updatedOptOutConsentSentValue = true;
        whatsappUsers.setOptOutConsentSent(updatedOptOutConsentSentValue);
        boolean isUpdated = whatsappUsersRepository.updateWhatsappUsersOptOutConsentSent(whatsappUsers);

        WhatsappUsers response = whatsappUsersRepository.getByMobileNumberKey(mobileNumber);
        assertTrue(isUpdated);
        assertEquals(mobileNumber, response.getMobileNumber());
        assertEquals(WhatsappUsers.WhatsAppStatus.OPT_IN, response.getStatus());
        assertEquals(true, response.getOptOutConsentSent());
    }

    @Test
    public void testUpdateNonExistingWhatsappUsers() {
        final String mobileNumber = "9029065989";
        final WhatsappUsers.WhatsAppStatus updatedStatusValue = WhatsappUsers.WhatsAppStatus.OPT_IN;
        whatsappUsers.setMobileNumber(mobileNumber);
        whatsappUsers.setStatus(updatedStatusValue);
        boolean isUpdated = whatsappUsersRepository.findAndUpdateWhatsappUsers(whatsappUsers);

        WhatsappUsers response = whatsappUsersRepository.getByMobileNumberKey(mobileNumber);
        assertTrue(isUpdated);
        assertEquals(mobileNumber, response.getMobileNumber());
        assertEquals(WhatsappUsers.WhatsAppStatus.OPT_IN, response.getStatus());
        assertEquals(false, response.getOptOutConsentSent());
    }

    @Test
    public void testExceptionInInsertWhatsappUsers() {
        whatsappUsers = Mockito.mock(WhatsappUsers.class);
        when(whatsappUsers.getMobileNumber()).thenThrow(new RuntimeException("Exception"));

        boolean isInserted = whatsappUsersRepository.insertWhatsappUsers(whatsappUsers);
        assertFalse(isInserted);
    }

    @Test
    public void testDeleteWhatsappUsers() {
        whatsappUsersRepository.insertWhatsappUsers(whatsappUsers);
        boolean isDeleted = whatsappUsersRepository.deleteWhatsappUsers(whatsappUsers);

        WhatsappUsers response = whatsappUsersRepository.getByMobileNumberKey(mobileNumber);
        assertTrue(isDeleted);
        assertEquals(mobileNumber, response.getMobileNumber());
        assertEquals(WhatsappUsers.WhatsAppStatus.OPT_IN, response.getStatus());
        assertNotNull(response.getDeletedAt());
    }

    @Test
    public void testRowNotExistForDeleting() {
        whatsappUsers.setMobileNumber("9999999990");
        boolean isDeleted = whatsappUsersRepository.deleteWhatsappUsers(whatsappUsers);

        assertFalse(isDeleted);
    }
}

