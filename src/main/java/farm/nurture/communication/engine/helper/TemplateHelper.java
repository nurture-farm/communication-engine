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

package farm.nurture.communication.engine.helper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.cache.LanguageCache;
import farm.nurture.communication.engine.cache.TemplateCache;
import farm.nurture.communication.engine.cache.TemplateCacheKey;
import farm.nurture.communication.engine.cache.TemplateCacheValue;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.Language;
import farm.nurture.core.contracts.communication.engine.Placeholder;
import farm.nurture.util.http.NFException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class TemplateHelper {

    private static final String DEFAULT_LANGUAGE_CODE = "hi-in";
    private static Metrics metrics = Metrics.getInstance();
    @Inject
    private TemplateCache templateCache;
    @Inject
    private LanguageCache languageCache;

    public String getContent(TemplateCacheValue templateCacheValue, Map<String, String> placeholders) throws IOException {
        StringWriter writer = new StringWriter();
        templateCacheValue.getCompiledTemplate().execute(writer, placeholders).flush();
        return writer.toString();
    }

    public List<Placeholder> getPlaceholderMapFromList(Map<String, String> placeholderMap) {
        return placeholderMap.entrySet().stream()
                .map((entry) -> Placeholder.newBuilder().setKey(entry.getKey()).setValue(entry.getValue()).build()).collect(Collectors.toList());
    }

    public Map<String, String> getPlaceHolders(List<Placeholder> placeholderList) {
        Map<String, String> placeholders = new HashMap<>();
        if (placeholderList != null && placeholderList.size() > 0) {
            placeholderList.forEach(placeholder -> placeholders.put(placeholder.getKey(), placeholder.getValue()));
        }
        return placeholders;
    }

    public String getTitle(TemplateCacheValue templateCacheValue, Map<String, String> placeholders) throws IOException {
        StringWriter writer = new StringWriter();
        if (templateCacheValue.getCompiledTitleTemplate() != null) {
            templateCacheValue.getCompiledTitleTemplate().execute(writer, placeholders).flush();
            return writer.toString();
        }
        return StringUtils.EMPTY;
    }

    public TemplateCacheValue getTemplateFromNameAndLangId(String templateName, Language primaryLanguage,Language secondaryLanguage) {
        TemplateCacheKey cacheKey;
        TemplateCacheValue value;
        if (primaryLanguage!=null) {
            cacheKey = new TemplateCacheKey(templateName, primaryLanguage.getId());
            value = templateCache.getCompiledTemplateByCacheKey(cacheKey);
            if (value != null) return value;

            /* Search in database */
            log.info("Unable to find primary language template in cache for language : {}, Template : {}", primaryLanguage, templateName);
            value = templateCache.getTemplateDetails(templateName, primaryLanguage.getId());
            if (value != null) {
                templateCache.put(new TemplateCacheKey(templateName, primaryLanguage.getId()), value);
                return value;
            }
        }
        if (secondaryLanguage!=null) {
            /* Search in cache for secondary language */
            cacheKey = new TemplateCacheKey(templateName, secondaryLanguage.getId());
            value = templateCache.getCompiledTemplateByCacheKey(cacheKey);
            if (value != null) return value;

            /* Search in database */
            log.info("Unable to find secondary language template in cache for language : {}, Template : {}", secondaryLanguage, templateName);
            value = templateCache.getTemplateDetails(templateName, secondaryLanguage.getId());
            if (value != null) {
                templateCache.put(new TemplateCacheKey(templateName, secondaryLanguage.getId()), value);
                return value;
            }
        }

        /* Search in cache for default language */

        log.info("Unable to find template in cache for primaryLanguage : {},secondaryLanguage : {},Template : {}", primaryLanguage,secondaryLanguage, templateName);
        cacheKey = new TemplateCacheKey(templateName, languageCache.getLanguageByCode(DEFAULT_LANGUAGE_CODE).getId());
        value = templateCache.getCompiledTemplateByCacheKey(cacheKey);

         if (value == null) {
             metrics.onIncrement(MetricGroupNames.NF_CE_SEND_COMM_EVENT, "template_not_found", Constants.LABEL_TEMPLATE_LANGUAGE_LIST, templateName, languageCache.getLanguageById(primaryLanguage.getId()).getName());
             log.error("Unable to find template in database for primaryLanguage : {}, secondaryLanguage : {}, defaultLanguage : {},Template : {}", primaryLanguage,secondaryLanguage, languageCache.getLanguageByCode(DEFAULT_LANGUAGE_CODE),templateName);
             throw new NFException("Unable to find template for Template : " + templateName + ", language : " + primaryLanguage.getId());
        }
        return value;
    }

    public TemplateCacheValue getAllTemplateFromNameAndLangId(String templateName, Language language){
        TemplateCacheKey cacheKey = new TemplateCacheKey(templateName, language.getId());
        TemplateCacheValue value = templateCache.getCompiledTemplateByCacheKey(cacheKey);
        if(value!= null) return value;
        /* Search in database */
        value = templateCache.getAllTemplateDetails(templateName, language.getId());
        if(value!= null) {
            return value;
        }
        if (value == null) {
            metrics.onIncrement(MetricGroupNames.NF_CE_SEND_COMM_EVENT, "template_not_found", Constants.LABEL_TEMPLATE_LANGUAGE_LIST, templateName, languageCache.getLanguageById(language.getId()).getName());
            log.error("Unable to find template in database for language : {}, Template : {}", language, templateName);
            throw new NFException("Unable to find template for Template : " + templateName + ", language : " + language.getId());
        }
        return value;
    }

    public Map<String, String> getAttributes(TemplateCacheValue value, Map<String, String> placeholders) {
        Map<String, String> indexToAttributeMap = value.getTemplate().getAttributes() == null ? new HashMap<>() : value.getTemplate().getAttributes(); //{0 : "farmer_first_name"}
        Map<String, String> indexToValueMap = new HashMap<>();
        indexToAttributeMap.forEach((index, attribute) -> indexToValueMap.put(index, placeholders.get(attribute)));
        return indexToValueMap;
    }

    public Map<String, Object> getMetaData(TemplateCacheValue value) {
        return value.getTemplate().getMetaData();
    }

}
