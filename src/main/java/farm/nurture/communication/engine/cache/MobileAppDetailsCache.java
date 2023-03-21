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

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import farm.nurture.communication.engine.models.MobileAppDetails;
import farm.nurture.communication.engine.repository.MobileAppDetailsRepository;

import java.util.concurrent.TimeUnit;

@Singleton
public class MobileAppDetailsCache {

    @Inject
    private MobileAppDetailsRepository appDetailsRepository;

    private LoadingCache<Short, MobileAppDetails> mobileAppDetailsCache = Caffeine.newBuilder()
            .maximumSize(20)
            .refreshAfterWrite(1, TimeUnit.HOURS)
            .build(id -> appDetailsRepository.getMobileAppDetailsById(id));

    private LoadingCache<Short, MobileAppDetails> mobileAppDetailsByAFSAppIdCache = Caffeine.newBuilder()
            .maximumSize(20)
            .refreshAfterWrite(1, TimeUnit.HOURS)
            .build(afsAppId -> appDetailsRepository.getMobileAppDetailsByAFSAppId(afsAppId));

    private LoadingCache<MobileAppDetailsCacheKey, MobileAppDetails> mobileAppDetailsByAppIdandAppTypeCache = Caffeine.newBuilder()
            .maximumSize(20)
            .refreshAfterWrite(1, TimeUnit.HOURS)
            .build(key -> appDetailsRepository.getMobileAppDetailsByAppIdandAppName(key.getAppId(), key.getAppType()));

    public void init() {
        appDetailsRepository.getAll().forEach(appDetails -> mobileAppDetailsCache.put(appDetails.getId(), appDetails));
        appDetailsRepository.getAll().forEach(appDetails -> {
            String appId = appDetails.getAppId();
            String appType = appDetails.getAppType().name();
            MobileAppDetailsCacheKey mobileAppDetailsCacheKey = new MobileAppDetailsCacheKey(appId, appType);
            mobileAppDetailsByAppIdandAppTypeCache.put(mobileAppDetailsCacheKey, appDetails);
            mobileAppDetailsByAFSAppIdCache.put(appDetails.getAfsAppId(), appDetails);
        });
    }

    public MobileAppDetails getMobileAppDetailsById(Short id) {
        if(id == null) return null;
        return mobileAppDetailsCache.get(id);
    }

    public MobileAppDetails getMobileAppDetailsByAFSAppId(Short afsAppId) {
        return mobileAppDetailsByAFSAppIdCache.get(afsAppId);
    }

    public MobileAppDetails getMobileAppDetailsByAppIdandAppType(MobileAppDetailsCacheKey key) {
        return mobileAppDetailsByAppIdandAppTypeCache.get(key);
    }

}
