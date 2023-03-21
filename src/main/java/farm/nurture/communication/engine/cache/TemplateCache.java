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
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import farm.nurture.communication.engine.models.Template;
import farm.nurture.communication.engine.repository.TemplateRepository;
import farm.nurture.infra.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.StringReader;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class TemplateCache {

    @Inject
    private TemplateRepository templateRepository;

    private LoadingCache<TemplateCacheKey, TemplateCacheValue> languageByCodeCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .refreshAfterWrite(30, TimeUnit.MINUTES)
            .build(key -> getTemplateDetails(key.getName(), key.getLanguageId()));

    public void init() {
        templateRepository.getAll().forEach(template -> {
            if(template!=null){
             String name = template.getName();
             Short languageId = template.getLanguageId();
             TemplateCacheKey templateCacheKey = new TemplateCacheKey(name, languageId);
             languageByCodeCache.put(templateCacheKey, getTemplateDetails(name, languageId));
            }
        });
    }

    public TemplateCacheValue getTemplateDetails(String name, Short languageId) {
        Template template = templateRepository.getTemplateByNameAndLanguage(name, languageId);
        if(template == null) return null;
        MustacheFactory mf = new DefaultMustacheFactory();
        try {
            Mustache mustacheTemplate = mf.compile(new StringReader(template.getContent()), template.getName() + "_" + template.getLanguageId());
            Mustache mustacheTitleTemplate = null;
            if (StringUtils.isNonEmpty(template.getTitle())) {
                mustacheTitleTemplate = mf.compile(new StringReader(template.getTitle()), template.getName() + "_" + template.getLanguageId());
            }
            return new TemplateCacheValue(template, mustacheTemplate, mustacheTitleTemplate);
        }catch (Exception exp){
            log.error("Error in parsing templates {} ", exp.getMessage());
            return null;
        }
    }

    public TemplateCacheValue getAllTemplateDetails(String name, Short languageId) {
        Template template = templateRepository.getAllTemplateByNameAndLanguage(name, languageId);
        if(template == null) return null;
        MustacheFactory mf = new DefaultMustacheFactory();
        try {
            Mustache mustacheTemplate = mf.compile(new StringReader(template.getContent()), template.getName() + "_" + template.getLanguageId());
            Mustache mustacheTitleTemplate = null;
            if (StringUtils.isNonEmpty(template.getTitle())) {
                mustacheTitleTemplate = mf.compile(new StringReader(template.getTitle()), template.getName() + "_" + template.getLanguageId());
            }
            return new TemplateCacheValue(template, mustacheTemplate, mustacheTitleTemplate);
        }catch (Exception exp){
            log.error("Error in parsing templates {} ", exp.getMessage());
            return null;
        }
    }
    public TemplateCacheValue getCompiledTemplateByCacheKey(TemplateCacheKey key) {
        return languageByCodeCache.get(key);
    }

    public void put(TemplateCacheKey key, TemplateCacheValue value){
        languageByCodeCache.put(key, value);
    }
}
