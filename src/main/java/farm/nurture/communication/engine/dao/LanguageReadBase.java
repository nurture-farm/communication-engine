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

package farm.nurture.communication.engine.dao;

import farm.nurture.laminar.core.io.sql.dao.ReadBase;
import lombok.extern.slf4j.Slf4j;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.Language;
import farm.nurture.communication.engine.models.Language.LanguageBuilder;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class LanguageReadBase extends ReadBase<Language> {

    public static final String ID = "id";
    public static final String CODE = "code";
    public static final String NAME = "name";
    public static final String UNICODE = "unicode";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String DELETED_AT = "deleted_at";
    private List<Language> records = null;

    private final Metrics metrics = Metrics.getInstance();

    @Override
    protected List<Language> populate() throws SQLException {
        if ( null == this.rs) {
            log.warn("Language ResultSet is not initialized.");
            throw new SQLException("Language ResultSet is not initialized.");
        }

        if ( null == records) records = new ArrayList<Language>();
        this.rs.setFetchSize(100);
        while (this.rs.next()) {
            recordsCount++;
            records.add(populateLanguage());
        }
        return records;
    }

    private Language populateLanguage() {
        Language language = null;
        try {
            LanguageBuilder builder = Language.builder();
            builder.id(rs.getShort(ID));
            builder.code(rs.getString(CODE));
            builder.name(rs.getString(NAME));
            builder.unicode(rs.getBoolean(UNICODE));
            builder.createdAt(rs.getTimestamp(CREATED_AT));
            builder.updatedAt(rs.getTimestamp(UPDATED_AT));
            builder.deletedAt(rs.getTimestamp(DELETED_AT));
            language = builder.build();

        } catch (Exception e) {
            metrics.onIncrement(MetricGroupNames.NF_CE_LANGUAGE_READ_BASE, "populate_record_failed");
            log.error("Unable to populate Language from resultSet : {}", rs, e);
        }
        return language;
    }

    @Override
    protected Language getFirstRow() throws SQLException {
        if ( null == this.rs) {
            log.warn("Language ResultSet is not initialized.");
            throw new SQLException("Language ResultSet is not initialized.");
        }

        Language language = null;
        this.rs.setFetchSize(1);

        while (this.rs.next()) {
            recordsCount++;
            language = populateLanguage();
        }
        return language;
    }

    @Override
    protected int getRecordsCount() {
        return recordsCount;
    }
}
