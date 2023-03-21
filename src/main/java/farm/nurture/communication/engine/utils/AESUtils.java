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

package farm.nurture.communication.engine.utils;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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
import farm.nurture.core.contracts.communication.engine.CommunicationEvent;
import farm.nurture.util.http.NFException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

@Slf4j
@Singleton
public class AESUtils {
    private static final int GCM_IV_LENGTH = 12;

    private static final int GCM_TAG_LENGTH = 16;

    private static final String DEFAULT_LANGUAGE_CODE = "hi-in";

    @Inject
    private TemplateCache templateCache;

    @Inject
    private LanguageCache languageCache;

    private Metrics metrics = Metrics.getInstance();

    //TODO have to give key
    private static final String GIVEN_KEY = "QOahfcdo98NLjYJuhP4-VKigx51NkUETsKlIu9uXZFY";

    public static String encrypt(String text) throws Exception {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        Key secretKey = new SecretKeySpec(Base64.decodeBase64(GIVEN_KEY), "AES");
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getEncoded(), "AES");

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);

        byte[] cipherText = cipher.doFinal(bytes);

        byte[] finalArray = new byte[cipherText.length + GCM_IV_LENGTH];

        System.arraycopy(iv, 0, finalArray, 0, GCM_IV_LENGTH);
        System.arraycopy(cipherText, 0, finalArray, GCM_IV_LENGTH, cipherText.length);

        return new String(Base64.encodeBase64URLSafe(finalArray), StandardCharsets.UTF_8);
    }


    public String getContent(CommunicationEvent event, Language language) throws IOException {

        String templateName = event.getTemplateName();
        TemplateCacheKey cacheKey = new TemplateCacheKey(templateName, language.getId());
        TemplateCacheValue value = templateCache.getCompiledTemplateByCacheKey(cacheKey);

        if(value == null) {
            metrics.onIncrement(MetricGroupNames.NF_CE_SEND_COMM_EVENT, "template_not_found", Constants.LABEL_TEMPLATE_LANGUAGE_LIST, event.getTemplateName(), languageCache.getLanguageById(language.getId()).getName());
            log.error("Unable to find template for language : {}, ActorId : {}, ActorType: {}, Template : {}", language, event.getReceiverActor().getActorId(),
                    event.getReceiverActor().getActorType(), event.getTemplateName());
            cacheKey = new TemplateCacheKey(templateName, languageCache.getLanguageByCode(DEFAULT_LANGUAGE_CODE).getId());
            value = templateCache.getCompiledTemplateByCacheKey(cacheKey);
            if(value == null) throw new NFException("Unable to find template for Template : " + event.getTemplateName() + ", language : " + language.getId());
        }

        StringWriter writer = new StringWriter();

        Map<String, Object> placeholders = new HashMap<>();
        if(event.getPlaceholderList() != null && event.getPlaceholderList().size() > 0) {
            event.getPlaceholderList().forEach(placeholder -> placeholders.put(placeholder.getKey(), placeholder.getValue()));
        }

        value.getCompiledTemplate().execute(writer, placeholders).flush();
        return writer.toString();
    }
}
