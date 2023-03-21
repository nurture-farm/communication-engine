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
import farm.nurture.communication.engine.dao.TemplateReadBase;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.MetricTracker;
import farm.nurture.communication.engine.models.Template;
import farm.nurture.core.contracts.common.Attribs;
import farm.nurture.core.contracts.communication.engine.GetAllTemplateRequest;
import farm.nurture.core.contracts.communication.engine.TemplateUpdateRequest;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.laminar.core.io.sql.dao.WriteBase;
import farm.nurture.util.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import farm.nurture.core.contracts.common.enums.NameSpace;
import farm.nurture.core.contracts.common.enums.Status;

@Slf4j
@Singleton
public class TemplateRepository {

    private static final String GET_ALL_ACTIVE_SQL = "SELECT * FROM templates where active = true";

    private static final String GET_TEMPLATE_BY_NAME_AND_LANGUAGE_ID_SQL = "SELECT * FROM templates where name = ? and language_id = ? and active = true";

    private static final String GET_ALL_TEMPLATE_BY_NAME_AND_LANGUAGE_ID_SQL = "SELECT * FROM templates where name = ? and language_id = ?";

    private static final String INSERT_TEMPLATE_SQL = "INSERT INTO templates(name, language_id, content_type, content, attributes, active, owner_email, vertical, title, meta_data) " +
            "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_TEMPLATE_SQL =  "UPDATE templates SET updated_at = now()";
    private static final String UPDATE_TEMPLATE_WHERE_CLAUSE = " WHERE name = ? AND language_id = ?";
    private static final String GET_ALL_TEMPLATE_SQL = "SELECT * FROM templates";

    public List<Template> getAll() {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "get_all_active_templates");
        TemplateReadBase readBase = new TemplateReadBase();
        List<Template> templates = null;
        try {
            templates = readBase.execute(GET_ALL_ACTIVE_SQL);
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching all active templates", e);

        } finally {
            tracker.stop(success);
        }
        return templates;
    }

    public List<Template> getAllTemplate(GetAllTemplateRequest getAllTemplateRequest){
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "get_all_templates");
        TemplateReadBase readBase = new TemplateReadBase();
        List<Template> templates = null;
        log.info("Serving Get All Template request {}",getAllTemplateRequest);
        List<Object> params=new ArrayList<>();
        String query=GET_ALL_TEMPLATE_SQL;
        query=getAllTemplateQuery(params,query,getAllTemplateRequest);
        try {
            templates = readBase.execute(query,params);
            log.info("List of templates are {} ", templates);
            success = true;
        } catch (Exception e) {
            log.error("Error in fetching all templates", e);

        } finally {
            tracker.stop(success);
        }
        return templates;
    }
    public Integer insertTemplates(Template template) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "insert_templates");

        String attributes = template.getAttributes() == null || template.getAttributes().isEmpty() ? null :
                Serializer.DEFAULT_JSON_SERIALIZER.serialize(template.getAttributes());
        String metaData = template.getMetaData() == null || template.getMetaData().isEmpty() ? null :
                Serializer.DEFAULT_JSON_SERIALIZER.serialize(template.getMetaData());

        Integer id = null;
        try {
            WriteBase writeBase = new WriteBase();
            id =  writeBase.insert(INSERT_TEMPLATE_SQL, Arrays.asList(template.getName(), template.getLanguageId(), template.getContentType().name(),
                    template.getContent(), attributes, template.getActive(), template.getOwnerEmail(), template.getVertical(), template.getTitle(),
                    metaData));
            success = true;

        } catch (Exception e) {
            log.error("Error in inserting template : {}", template, e);
        } finally {
            tracker.stop(success);
        }
        return id;
    }

    public Template getTemplateByNameAndLanguage(String name, Short languageId) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "get_template_by_name_language");
        TemplateReadBase readBase = new TemplateReadBase();
        Template template = null;
        try {
            template = readBase.selectByUniqueKey(GET_TEMPLATE_BY_NAME_AND_LANGUAGE_ID_SQL, Arrays.asList(name, languageId).toArray());
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching template by name : {} and languageId : {}", name, languageId, e);

        } finally {
            tracker.stop(success);
        }
        return template;
    }

    public Template getAllTemplateByNameAndLanguage(String name, Short languageId) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "get_all_template_by_name_language");
        TemplateReadBase readBase = new TemplateReadBase();
        Template template = null;
        try {
            template = readBase.selectByUniqueKey(GET_ALL_TEMPLATE_BY_NAME_AND_LANGUAGE_ID_SQL, Arrays.asList(name, languageId).toArray());
            success = true;

        } catch (Exception e) {
            log.error("Error in fetching all template by name : {} and languageId : {}", name, languageId, e);

        } finally {
            tracker.stop(success);
        }
        return template;
    }
    private Map<String, String> getAttributesMap(List<Attribs> attribList) {
        Map<String,String> attributesMap = new HashMap<>();
        for(Attribs attribs: attribList) {
            attributesMap.put(attribs.getKey(), attribs.getValue());
        }
        return attributesMap;
    }
    public Integer updateTemplate(TemplateUpdateRequest templateUpdateRequest) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "update_template");
        Map<String, String> attributesMap = getAttributesMap(templateUpdateRequest.getAttribsList());
        Map<String, String> metaDataMap = getAttributesMap(templateUpdateRequest.getMetaDataList());
        String attributes = attributesMap.isEmpty() ? null : Serializer.DEFAULT_JSON_SERIALIZER.serialize(attributesMap);
        String metaData = metaDataMap.isEmpty()? null: Serializer.DEFAULT_JSON_SERIALIZER.serialize(metaDataMap);
        StringBuilder updateSqlString = new StringBuilder();
        updateSqlString.append(UPDATE_TEMPLATE_SQL);

        List<Object> fieldList = new ArrayList<>();

        if(StringUtils.isNonEmpty(templateUpdateRequest.getContent())){
            updateSqlString.append(", content = ?");
            fieldList.add(templateUpdateRequest.getContent());
        }
        if(StringUtils.isNonEmpty(templateUpdateRequest.getVertical())){
            updateSqlString.append(", vertical = ?");
            fieldList.add(templateUpdateRequest.getVertical());
        }
        if(StringUtils.isNonEmpty(templateUpdateRequest.getTitle())){
            updateSqlString.append(", title = ?");
            fieldList.add(templateUpdateRequest.getTitle());
        }
        if(templateUpdateRequest.getAttribsList()!= null && templateUpdateRequest.getAttribsCount()!=0){
            updateSqlString.append(", attributes = ?");
            fieldList.add(attributes);
        }
        if(templateUpdateRequest.getMetaDataList()!= null && templateUpdateRequest.getMetaDataCount()!=0){
            updateSqlString.append(", meta_data = ?");
            fieldList.add(metaData);
        }
        if(StringUtils.isNonEmpty(templateUpdateRequest.getOwner())){
            updateSqlString.append(", owner_email = ?");
            fieldList.add(templateUpdateRequest.getOwner());
        }
        updateSqlString.append(UPDATE_TEMPLATE_WHERE_CLAUSE);
        fieldList.add(templateUpdateRequest.getName());
        fieldList.add(templateUpdateRequest.getLanguageCodeValue());
        Integer id = null;
        try {
            WriteBase writeBase = new WriteBase();
            id =  writeBase.execute(updateSqlString.toString(), fieldList);
            success = true;
        } catch (Exception e) {
            log.error("Error in updating template : {}", templateUpdateRequest, e);
        } finally {
            tracker.stop(success);
        }
        return id;
    }

    public Integer updateTemplateToActive( String templateName, Short languageId) {
        boolean success = false;
        MetricTracker tracker = new MetricTracker(MetricGroupNames.NF_CE_MYSQL, "update_template_active");
         StringBuilder updateSqlString = new StringBuilder();
        updateSqlString.append(UPDATE_TEMPLATE_SQL);
        updateSqlString.append(", active = true");
        updateSqlString.append(UPDATE_TEMPLATE_WHERE_CLAUSE);
        Integer id = null;
        try {
            WriteBase writeBase = new WriteBase();
            id =  writeBase.execute(updateSqlString.toString(), Arrays.asList(templateName, languageId));
            success = true;
        } catch (Exception e) {
            log.error("Error in updating template to active: template Name {}, languageId {}, error {}", templateName, languageId, e);
        } finally {
            tracker.stop(success);
        }
        return id;
    }

    public String getAllTemplateQuery(List<Object> params,String query,GetAllTemplateRequest getAllTemplateRequest){
            boolean isFirst=true;
            StringBuilder queryBuilder = new StringBuilder(query);
            NameSpace namespace=getAllTemplateRequest.getNamespace();
            Status status=getAllTemplateRequest.getStatus();
            String searchQuery=getAllTemplateRequest.getSearchQuery();
            List<String> templateNames=getAllTemplateRequest.getTemplateNamesList();
            long limit=getAllTemplateRequest.getLimit();
            long offset=getAllTemplateRequest.getOffset();
            if (!templateNames.isEmpty() && templateNames!=null){
                if (isFirst){
                    queryBuilder.append(" where ");
                    isFirst=false;
                }
                else{
                    queryBuilder.append(" and ");
                }
                queryBuilder.append(" name in (");
                int templateNamesSize=templateNames.size();
                for (int index=0;index<templateNamesSize;index++){
                    if (index!=templateNamesSize-1) {
                        queryBuilder.append("?,");
                    }
                    else{
                        queryBuilder.append("?) ");
                    }
                    params.add(templateNames.get(index));
                }
            }
            if (namespace!=NameSpace.NO_NAMESPACE){
                 if (isFirst) {
                     queryBuilder.append(" where ");
                     isFirst=false;
                 }
                 else{
                     queryBuilder.append(" and ");
                 }
                queryBuilder.append(" vertical=? ");
                 System.out.println(namespace);

                if ((namespace != NameSpace.NURTURE_PARTNER)) {
                    params.add("NURTURE_" + namespace.name());
                } else {
                    params.add(namespace.name());
                }


            }
            if (status!=Status.NO_KNOWN_STATUS){
                if (isFirst) {
                    queryBuilder.append(" where ");
                    isFirst=false;
                }
                else{
                    queryBuilder.append(" and ");
                }
                queryBuilder.append(" active=? ");
                params.add(status==Status.ACTIVE?1:0);
            }
            if (!StringUtils.isEmpty(searchQuery)){
                if (isFirst){
                    queryBuilder.append(" where ");
                    isFirst=false;
                }
                else{
                    queryBuilder.append(" and ");
                }
                queryBuilder.append(" (name like ? or content like ? or owner_email like ?) ");
                params.add("%"+searchQuery+"%");
                params.add("%"+searchQuery+"%");
                params.add("%"+searchQuery+"%");
            }
            queryBuilder.append(" order by id desc ");
            if (limit>0 && offset>=0){
                queryBuilder.append(" limit ? offset ? ");
                params.add(limit);
                params.add(offset);
            }
            return queryBuilder.toString();
    }

}

