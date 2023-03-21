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
import farm.nurture.communication.engine.models.Template;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith({H2Extension.class})
public class TemplateRepositoryTest {
    private final static String name = "farmer_booking_creation99";
    private final static Short languageId = 1;
    private final static String content = "Thank you";
    private final static boolean isActive = true;

    private static final TemplateRepository repository = new TemplateRepository();

    private static Template template;

    @BeforeAll
    public static void setUp() {
        template = Template.builder().name(name).languageId(languageId).contentType(Template.ContentType.STRING).content(content).active(isActive).build();
        repository.insertTemplates(template);
    }

    @Test
    public void testGetTemplateByNameAndLanguage() {
        Template response = repository.getTemplateByNameAndLanguage(name, languageId);

        assertEquals(response.getLanguageId(), template.getLanguageId());
        assertEquals(response.getName(), template.getName());
        assertEquals(response.getActive(), template.getActive());
        assertEquals(response.getContent(), template.getContent());
        assertEquals(response.getContentType(), template.getContentType());
    }

    @Test
    public void testGetAllTemplates() {
        repository.insertTemplates(Template.builder().name("farmer_service_completed").languageId((short) 6).contentType(Template.ContentType.STRING)
                .content(content).active(isActive).build());
        List<Template> templates = repository.getAll();

        assertEquals(templates.get(0).getLanguageId(), languageId);
        assertEquals(templates.get(0).getName(), name);
        assertEquals(templates.get(0).getActive(), isActive);
        assertEquals(templates.get(0).getContent(), content);
        assertEquals(templates.get(0).getContentType(), Template.ContentType.STRING);
        assertEquals(templates.get(1).getLanguageId(), (short) 6);
        assertEquals(templates.get(1).getName(), "farmer_service_completed");
        assertEquals(templates.get(1).getActive(), isActive);
        assertEquals(templates.get(1).getContent(), content);
        assertEquals(templates.get(1).getContentType(), Template.ContentType.STRING);
    }

    @Test
    public void testExceptionForInsert() {
        Template template = Mockito.mock(Template.class);
        when(template.getLanguageId()).thenThrow(new RuntimeException("Exception"));
        repository.insertTemplates(template);

        List<Template> templates = repository.getAll();
        assertEquals(templates.size(), 1);
    }
}
