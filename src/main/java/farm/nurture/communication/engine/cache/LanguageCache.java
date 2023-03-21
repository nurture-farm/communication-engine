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
import farm.nurture.communication.engine.models.Language;
import farm.nurture.communication.engine.repository.LanguageRepository;

import java.util.concurrent.TimeUnit;

@Singleton
public class LanguageCache {

    @Inject
    private LanguageRepository languageRepository;

    private LoadingCache<String, Language> languageByCodeCache = Caffeine.newBuilder()
            .maximumSize(50)
            .refreshAfterWrite(1, TimeUnit.HOURS)
            .build(code -> languageRepository.getLanguageByCode(code));

    private LoadingCache<Short, Language> languageByIdCache = Caffeine.newBuilder()
            .maximumSize(50)
            .refreshAfterWrite(1, TimeUnit.HOURS)
            .build(id -> languageRepository.getLanguageById(id));

    public void init() {
        languageRepository.getAll().forEach(language -> languageByCodeCache.put(language.getCode(), language));
        languageRepository.getAll().forEach(language -> languageByIdCache.put(language.getId(), language));
    }

    public Language getLanguageByCode(String code) {
        return languageByCodeCache.get(code);
    }

    public Language getLanguageById(Short id) {
        return languageByIdCache.get(id);
    }
}
