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

import com.fasterxml.jackson.databind.ObjectMapper;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.Template;
import farm.nurture.communication.engine.models.Template.TemplateBuilder;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.laminar.core.io.sql.dao.ReadBase;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class TemplateReadBase extends ReadBase<Template> {

    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String CONTENT_TYPE = "content_type";
    public static final String CONTENT = "content";
    public static final String LANGUAGE_ID = "language_id";
    public static final String TITLE = "title";
    public static final String ACTIVE = "active";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";
    public static final String DELETED_AT = "deleted_at";
    public static final String ATTRIBUTES ="attributes";
    public static final String METADATA ="meta_data";
    public static final String OWNER_EMAIL = "owner_email";
    public static final String VERTICAL = "vertical";
    public static final String INTERACTIVE_ATTRIBUTES = "interactive_attributes";
    private List<Template> records = null;

    private final Metrics metrics = Metrics.getInstance();

    @Override
    protected List<Template> populate() throws SQLException {
        if ( null == this.rs) {
            log.warn("Template ResultSet is not initialized.");
            throw new SQLException("Template ResultSet is not initialized.");
        }

        if ( null == records) records = new ArrayList<Template>();
        this.rs.setFetchSize(1000);
        while (this.rs.next()) {
            recordsCount++;
            records.add(populateTemplate());
        }
        return records;
    }

    private Template populateTemplate() {
        Template template = null;
        try {
            TemplateBuilder builder = Template.builder();
            builder.id(rs.getShort(ID));
            builder.name(rs.getString(NAME));
            builder.contentType(Template.ContentType.valueOf(rs.getString(CONTENT_TYPE)));
            builder.content(rs.getString(CONTENT));
            builder.languageId(rs.getShort(LANGUAGE_ID));
            builder.title(rs.getString(TITLE));
            builder.active(rs.getBoolean(ACTIVE));
            builder.createdAt(rs.getTimestamp(CREATED_AT));
            builder.updatedAt(rs.getTimestamp(UPDATED_AT));
            builder.deletedAt(rs.getTimestamp(DELETED_AT));
            builder.ownerEmail(rs.getString(OWNER_EMAIL));
            builder.vertical(rs.getString(VERTICAL));
            ObjectMapper mapper = new ObjectMapper();
            String attributes = rs.getString(ATTRIBUTES);
            String metaData = rs.getString(METADATA);
            try{
                if(StringUtils.isNonEmpty(attributes))
                    builder.attributes(mapper.readValue(attributes, Map.class));}
             catch (Exception exception){
                log.error("Attributes is not in Json Format", exception);
            }
            try{
                if(StringUtils.isNonEmpty(metaData))
                    builder.metaData(mapper.readValue(metaData, Map.class));
            }
            catch (Exception exception){
                log.error("Vendor Metadata is not in Json Format", exception);
            }
            template = builder.build();

        } catch (Exception e) {
            metrics.onIncrement(MetricGroupNames.NF_CE_TEMPLATE_READ_BASE, "populate_record_failed");
            log.error("Unable to populate Template from resultSet : {}", rs, e);
        }
        return template;
    }

    @Override
    protected Template getFirstRow() throws SQLException {
        if ( null == this.rs) {
            log.warn("Template ResultSet is not initialized.");
            throw new SQLException("Template ResultSet is not initialized.");
        }

        Template template = null;
        this.rs.setFetchSize(1);

        while (this.rs.next()) {
            recordsCount++;
            template = populateTemplate();
        }
        return template;
    }

    @Override
    protected int getRecordsCount() {
        return recordsCount;
    }
}
