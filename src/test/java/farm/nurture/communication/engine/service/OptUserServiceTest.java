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

import farm.nurture.communication.engine.dto.WhatsAppOptUserResponse;
import farm.nurture.communication.engine.models.WhatsappUsers;
import farm.nurture.util.http.client.NFHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class OptUserServiceTest extends BaseServiceTest {

    @Spy
    private NFHttpClient nfHttpClient = buildHttpClient();

    @InjectMocks
    private OptUserService optUserService;

    @Test
    public void whatsappOptInUserTest() throws Exception {
        String mobileNumber = "919078679034";

        WhatsAppOptUserResponse response = optUserService.whatsappOptUser(mobileNumber, WhatsappUsers.WhatsAppStatus.OPT_IN);

        assertNotNull(response);
        assertEquals(response.getResponse().getDetails(), WhatsappUsers.WhatsAppStatus.OPT_IN.name());
        assertEquals(response.getResponse().getStatus(), "success");
    }

    @Test
    public void whatsappOptOutUserTest() throws Exception {
        String mobileNumber = "919078679034";

        WhatsAppOptUserResponse response = optUserService.whatsappOptUser(mobileNumber, WhatsappUsers.WhatsAppStatus.OPT_OUT);

        assertNotNull(response);
        assertEquals(response.getResponse().getDetails(), WhatsappUsers.WhatsAppStatus.OPT_OUT.name());
        assertEquals(response.getResponse().getStatus(), "success");
    }

    @Test
    public void whatsappOptInUserFailedTest() throws Exception {
        String mobileNumber = "78679034";

        Throwable exception = assertThrows(RuntimeException.class, () -> optUserService.whatsappOptUser(mobileNumber, WhatsappUsers.WhatsAppStatus.OPT_IN));
        assertEquals("Given mobile number is wrong", exception.getMessage());
    }
}
