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
import farm.nurture.communication.engine.models.MobileAppDetails;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith({H2Extension.class})
public class MobileAppDetailsRepositoryTest {
    private static final Short id = 1;
    private static final String appId = "NF Farmer";
    private static final String appName = "NF Farmer";
    private static final String fcmApiKey = "Fcm_Api_Key";
    private static final Short afsAppId = 1;

    private static final MobileAppDetailsRepository mobileAppDetailsRepository = Mockito.spy(MobileAppDetailsRepository.class);

    private static MobileAppDetails mobileAppDetails;

    @BeforeAll
    public static void setUp() {
        mobileAppDetails = MobileAppDetails.builder().appId(appId).appType(MobileAppDetails.AppType.ANDROID).
                appName(appName).fcmApiKey(fcmApiKey).afsAppId(afsAppId).build();
        mobileAppDetailsRepository.insertMobileAppDetails(mobileAppDetails);
    }

    @ParameterizedTest
    @ValueSource(strings = {"getMobileAppDetailsById", "getMobileAppDetailsByAFSAppId"})
    public void testGetMobileAppDetails(String operation) {
        MobileAppDetails response;
        if (operation.equals("getMobileAppDetailsById")) {
            response = mobileAppDetailsRepository.getMobileAppDetailsById(id);
        } else {
            response = mobileAppDetailsRepository.getMobileAppDetailsByAFSAppId(afsAppId);
        }

        assertEquals(response.getAppId(), mobileAppDetails.getAppId());
        assertEquals(response.getAppName(), mobileAppDetails.getAppName());
        assertEquals(response.getFcmApiKey(), mobileAppDetails.getFcmApiKey());
        assertEquals(response.getAfsAppId(), mobileAppDetails.getAfsAppId());
    }

    @Test
    public void testGetAllMobileAppDetails() {
        mobileAppDetailsRepository.insertMobileAppDetails(MobileAppDetails.builder().appId("SMS").appType(MobileAppDetails.AppType.IOS).
                appName("SMS").fcmApiKey(fcmApiKey).afsAppId((short) 2).build());
        List<MobileAppDetails> mobileAppDetails = mobileAppDetailsRepository.getAll();

        assertEquals(mobileAppDetails.size(), 2);
        assertEquals(appId, mobileAppDetails.get(0).getAppId());
        assertEquals(MobileAppDetails.AppType.ANDROID, mobileAppDetails.get(0).getAppType());
        assertEquals(appName, mobileAppDetails.get(0).getAppName());
        assertEquals(fcmApiKey, mobileAppDetails.get(0).getFcmApiKey());
        assertEquals(afsAppId, mobileAppDetails.get(0).getAfsAppId());
        assertEquals("SMS", mobileAppDetails.get(1).getAppId());
        assertEquals(MobileAppDetails.AppType.IOS, mobileAppDetails.get(1).getAppType());
        assertEquals("SMS", mobileAppDetails.get(1).getAppName());
        assertEquals(fcmApiKey, mobileAppDetails.get(1).getFcmApiKey());
        assertEquals((short) 2, mobileAppDetails.get(1).getAfsAppId());
    }

    @Test
    public void testExceptionForInsert() {
        MobileAppDetails mobileAppDetails = Mockito.mock(MobileAppDetails.class);
        when(mobileAppDetails.getAppId()).thenThrow(new RuntimeException("Exception"));
        mobileAppDetailsRepository.insertMobileAppDetails(mobileAppDetails);
        List<MobileAppDetails> mobileAppDetailsList = mobileAppDetailsRepository.getAll();

        assertEquals(mobileAppDetailsList.size(), 1);
    }
}
