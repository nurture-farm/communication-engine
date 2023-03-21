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

import farm.nurture.communication.engine.models.Template;
import farm.nurture.communication.engine.repository.TemplateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TemplateCacheTest {
    private final static String name = "farmer_booking_creation";
    private final static Short languageId = 1;
    private final static String content = "Thank you";
    private final static boolean isActive = true;

    @Mock
    private TemplateRepository templateRepository;

    @InjectMocks
    private TemplateCache templateCache;

    @Test
    public void testGetCompiledTemplateByCacheKey() {
        Template template = Template.builder().name(name).languageId(languageId).contentType(Template.ContentType.STRING).content(content).active(isActive).build();
        TemplateCacheKey templateCacheKey = new TemplateCacheKey(name, languageId);
        when(templateRepository.getAll()).thenReturn(Arrays.asList(template));
        when(templateRepository.getTemplateByNameAndLanguage(name, languageId)).thenReturn(template);
        templateCache.init();
        TemplateCacheValue response = templateCache.getCompiledTemplateByCacheKey(templateCacheKey);

        assertEquals(response.getTemplate().getName(), name);
        assertEquals(response.getTemplate().getLanguageId(), languageId);
        assertEquals(response.getTemplate().getContent(), content);
        assertEquals(response.getTemplate().getActive(), isActive);
    }
}
