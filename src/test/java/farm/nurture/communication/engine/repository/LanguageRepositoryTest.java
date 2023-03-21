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
import farm.nurture.communication.engine.models.Language;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith({H2Extension.class})
public class LanguageRepositoryTest {
    private final static Short id = 1;
    private final static String code = "hi-in";
    private final static String name = "Hindi";

    private static final LanguageRepository languageRepository = new LanguageRepository();

    private static Language language;

    @BeforeAll
    public static void setUp() {
        language = Language.builder().code(code).name(name).unicode(true).build();
        languageRepository.insertLanguage(language);
    }

    @Test
    public void testGetLanguageById() {
        Language response = languageRepository.getLanguageById(id);

        assertEquals(language.getCode(), response.getCode());
        assertEquals(language.getName(), response.getName());
        assertEquals(language.getUnicode(), response.getUnicode());
    }

    @Test
    public void testGetLanguageByCode() {
        Language response = languageRepository.getLanguageByCode(code);

        assertEquals(language.getCode(), response.getCode());
        assertEquals(language.getName(), response.getName());
        assertEquals(language.getUnicode(), response.getUnicode());
    }

    @Test
    public void testGetAllLanguages() {
        languageRepository.insertLanguage(Language.builder().code("gu").name("Gujarati").unicode(true).build());
        List<Language> languages = languageRepository.getAll();

        assertEquals(languages.size(), 2);
        assertEquals(code, languages.get(0).getCode());
        assertEquals(name, languages.get(0).getName());
        assertTrue(languages.get(0).getUnicode());
        assertEquals("gu", languages.get(1).getCode());
        assertEquals("Gujarati", languages.get(1).getName());
        assertTrue(languages.get(1).getUnicode());
    }

    @Test
    public void testExceptionForInsert() {
        Language language = Mockito.mock(Language.class);
        when(language.getCode()).thenThrow(new RuntimeException("Exception"));
        languageRepository.insertLanguage(language);
        List<Language> languages = languageRepository.getAll();

        assertEquals(languages.size(), 1);
    }
}
