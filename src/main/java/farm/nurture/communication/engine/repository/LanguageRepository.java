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

import com.google.inject.Singleton;
import farm.nurture.communication.engine.dao.LanguageReadBase;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.models.Language;
import farm.nurture.laminar.core.io.sql.dao.WriteBase;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Singleton
public class LanguageRepository {

    private static final String getAllSql = "SELECT * FROM languages";

    private static final String getLanguageByIdSql = "SELECT * FROM languages WHERE id = ?";

    private static final String getLanguageByCodeSql = "SELECT * FROM languages WHERE code = ?";

    private static final String insertLanguageSql = "INSERT INTO languages(code, name, unicode) " +
            "VALUES(?, ?, ?)";

    public List<Language> getAll() {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "get_all_languages");
        LanguageReadBase readBase = new LanguageReadBase();
        List<Language> languages = null;
        try {
            languages = readBase.execute(getAllSql);
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching all languages", e);

        } finally {
            tracker.stop(success);
        }
        return languages;
    }

    public void insertLanguage(Language language) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "insert_languages");

        try {
            WriteBase writeBase = new WriteBase();
            writeBase.execute(insertLanguageSql, Arrays.asList(language.getCode(), language.getName(), language.getUnicode()));
            success = true;

        } catch (Exception e) {
            log.error("Error in inserting Language : {}", language, e);

        } finally {
            tracker.stop(success);
        }
    }

    public Language getLanguageById(Short id) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "get_language_by_id");
        LanguageReadBase readBase = new LanguageReadBase();
        Language language = null;
        try {
            language = readBase.selectByPrimaryKey(getLanguageByIdSql, id);
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching language by id : {}", id, e);

        } finally {
            tracker.stop(success);
        }
        return language;
    }

    public Language getLanguageByCode(String code) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "get_language_by_code");
        LanguageReadBase readBase = new LanguageReadBase();
        Language language = null;
        try {
            language = readBase.selectByUniqueKey(getLanguageByCodeSql, Arrays.asList(code).toArray());
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching language by code : {}", code, e);

        } finally {
            tracker.stop(success);
        }
        return language;
    }
}
