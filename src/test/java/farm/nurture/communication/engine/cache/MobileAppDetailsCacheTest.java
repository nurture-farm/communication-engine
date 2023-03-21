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

package farm.nurture.communication.engine.cache;

import farm.nurture.communication.engine.models.MobileAppDetails;
import farm.nurture.communication.engine.repository.MobileAppDetailsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MobileAppDetailsCacheTest {
    private static final Short id = 1;
    private static final String appId = "NF Farmer";
    private static final String appName = "NF Farmer";
    private static final String fcmApiKey = "Fcm_Api_Key";
    private static final Short afsAppId = 1;

    @Mock
    private MobileAppDetailsRepository appDetailsRepository;

    @InjectMocks
    private MobileAppDetailsCache mobileAppDetailsCache;

    private MobileAppDetails mobileAppDetails;

    @BeforeEach
    public void setUp() {
        mobileAppDetails = MobileAppDetails.builder().appId(appId).appType(MobileAppDetails.AppType.ANDROID).
                appName(appName).fcmApiKey(fcmApiKey).afsAppId(afsAppId).id(id).build();
    }

    @Test
    public void testGetMobileAppDetailsById() {
        when(appDetailsRepository.getAll()).thenReturn(Arrays.asList(mobileAppDetails));
        mobileAppDetailsCache.init();
        MobileAppDetails response = mobileAppDetailsCache.getMobileAppDetailsById(id);

        assertEquals(response.getAfsAppId(), afsAppId);
        assertEquals(response.getFcmApiKey(), fcmApiKey);
        assertEquals(response.getAppId(), appId);
        assertEquals(response.getAppName(), appName);
    }

    @Test
    public void testGetMobileAppDetailsByAFSAppId() {
        when(appDetailsRepository.getAll()).thenReturn(Arrays.asList(mobileAppDetails));
        mobileAppDetailsCache.init();
        MobileAppDetails response = mobileAppDetailsCache.getMobileAppDetailsByAFSAppId(afsAppId);

        assertEquals(response.getAfsAppId(), afsAppId);
        assertEquals(response.getFcmApiKey(), fcmApiKey);
        assertEquals(response.getAppId(), appId);
        assertEquals(response.getAppName(), appName);
    }

    @Test
    public void testNullId() {
        MobileAppDetails response = mobileAppDetailsCache.getMobileAppDetailsById(null);
        assertNull(response);
    }
}
