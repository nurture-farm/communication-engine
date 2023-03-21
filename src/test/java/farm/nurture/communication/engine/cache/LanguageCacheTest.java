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

import farm.nurture.communication.engine.models.Language;
import farm.nurture.communication.engine.repository.LanguageRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LanguageCacheTest {
    private final static Short id = 1;
    private final static String code = "hi-in";
    private final static String name = "Hindi";

    @Mock
    private LanguageRepository languageRepository;

    @InjectMocks
    private static LanguageCache languageCache;

    private static Language language = null;

    @BeforeAll
    public static void setUp() {
        language = Language.builder().code(code).name(name).id(id).build();
    }

    @Test
    public void testGetLanguageByCode() {
        when(languageRepository.getAll()).thenReturn(Arrays.asList(language));
        languageCache.init();
        Language response = languageCache.getLanguageByCode(code);

        assertEquals(response.getCode(), code);
        assertEquals(response.getId(), id);
        assertEquals(response.getName(), name);
    }

    @Test
    public void testGetLanguageById() {
        when(languageRepository.getAll()).thenReturn(Arrays.asList(language));
        languageCache.init();
        Language response = languageCache.getLanguageById(id);

        assertEquals(response.getCode(), code);
        assertEquals(response.getId(), id);
        assertEquals(response.getName(), name);
    }
}
