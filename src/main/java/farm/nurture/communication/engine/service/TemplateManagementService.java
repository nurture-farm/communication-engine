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

package farm.nurture.communication.engine.service;


import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.dto.*;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.Template;
import farm.nurture.communication.engine.vendor.GupShupVendor;
import farm.nurture.communication.engine.vendor.KarixVendor;
import farm.nurture.core.contracts.common.enums.ResponseStatus;
import farm.nurture.core.contracts.common.enums.ResponseStatusCode;
import farm.nurture.core.contracts.communication.engine.VendorResponse;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.util.http.client.BaseHttpResponseException;
import farm.nurture.util.http.client.NFHttpClient;
import farm.nurture.util.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static farm.nurture.communication.engine.Constants.*;


@Slf4j
@Singleton
public class TemplateManagementService {


    public static final String MEDIA_TYPE = "media_type";
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Map<String, Serializer> serializerMap = new HashMap<>();

    static {
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        serializerMap.put(Constants.APPLICATION_JSON, new PlainJsonSerializer());
    }

    @Inject
    KarixVendor karixVendor;
    @Inject
    GupShupVendor gupshupVendor;

    Metrics metrics = Metrics.getInstance();
    @Inject
    private NFHttpClient nfHttpClient;

    public VendorResponse createTemplateInGupshup(Template template) {
        VendorResponse.Builder vendorResponseBuilder = VendorResponse.newBuilder();
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            String fileName = gupshupVendor.findFile(template.getMetaData());
            HttpClientRequest<HttpEntity> httpClientRequest = gupshupVendor.createWhatsAppTemplate(template, fileName);
            HttpPost httppost = new HttpPost(httpClientRequest.getUrl());
            httppost.setEntity(httpClientRequest.getRequestBody());
            log.info("Serving create template request in gupshup {} ", httpClientRequest.getRequestBody());
            CloseableHttpResponse response = httpClient.execute(httppost);
            HttpEntity responseEntity = response.getEntity();
            int statusCode = response.getStatusLine().getStatusCode();
            vendorResponseBuilder.setVendorName(gupshupVendor.getVendorName().name());

            if (BaseHttpResponseException.Family.familyOf(statusCode) == BaseHttpResponseException.Family.SUCCESSFUL) {
                String result = EntityUtils.toString(responseEntity);
                if (result.contains("error")) {
                    GupshupCreateTemplateErrorResponse gupshupCreateTemplateErrorResponse = Serializer.DEFAULT_JSON_SERIALIZER.deserialize(result, GupshupCreateTemplateErrorResponse.class);
                    log.error("Error in creating gupshup template: {}, result: {}", template, result);
                    vendorResponseBuilder.setErrorMsg(gupshupCreateTemplateErrorResponse.getDetails());
                    vendorResponseBuilder.setStatusCode(ResponseStatusCode.BAD_REQUEST);
                    vendorResponseBuilder.setStatus(ResponseStatus.ERROR);
                } else {
                    GupshupCreateTemplateResponse gupshupCreateTemplateResponse = Serializer.DEFAULT_JSON_SERIALIZER.deserialize(result, GupshupCreateTemplateResponse.class);
                    log.info("GupshupCreateTemplateResponse for creating template in gupshup {} ", gupshupCreateTemplateResponse);
                    Map<String, Object> metaData = template.getMetaData();
                    List<GupshupCreateTemplateResponse.Details> details = gupshupCreateTemplateResponse.getDetails();
                    if (CollectionUtils.isNotEmpty(details)) {
                        metaData.put(GUPSHUP_TEMPLATE_NAME, details.get(0).getTemplateName());
                        metaData.put(GUPSHUP_TEMPLATE_ID, details.get(0).getTemplateId());
                        vendorResponseBuilder.setStatus(ResponseStatus.SUCCESSFUL);
                        vendorResponseBuilder.setStatusCode(ResponseStatusCode.OK);
                        vendorResponseBuilder.setErrorMsg(details.get(0).getTemplateId());
                    }
                    template.setMetaData(metaData);

                }
            }
        } catch (Exception e) {
            log.error("Exception in processing create template gupshup api request  : {}, error : {}", template, e.getMessage(), e);

        }
        return vendorResponseBuilder.build();
    }


    public VendorResponse createTemplateInKarix(Template template) {
        VendorResponse.Builder vendorResponseBuilder = VendorResponse.newBuilder();
        try {
            vendorResponseBuilder.setVendorName(karixVendor.getVendorName().name());
            HttpClientRequest<KarixCreateTemplateRequest> httpClientRequest = karixVendor.createWhatsAppTemplate(template, uploadFile(template.getMetaData()));
            log.info("Serving create template request in Karix {} ", httpClientRequest.getRequestBody());
            KarixCreateTemplateResponse response = nfHttpClient.sendMessage(httpClientRequest.getMethod(), httpClientRequest.getUrl(),
                    httpClientRequest.getRequestParams(), httpClientRequest.getHeaders(),
                    httpClientRequest.getRequestBody(), KarixCreateTemplateResponse.class, serializerMap);
            log.info("KarixCreateTemplateResponse for creating template in karix is: {}", response);

            if (StringUtils.isNonEmpty(response.getErrorCode())) {
                log.error("Error in creating template in Karix for request {} and response {} ", httpClientRequest, response);
                vendorResponseBuilder.setStatusCode(ResponseStatusCode.BAD_REQUEST);
                vendorResponseBuilder.setErrorMsg(response.getErrorMessage());
                vendorResponseBuilder.setStatus(ResponseStatus.ERROR);
            } else {
                Map<String, Object> metaData = template.getMetaData();
                metaData.put(KARIX_TEMPLATE_NAME, template.getName()+"_"+template.getLanguageId());
                metaData.put(KARIX_TEMPLATE_ID, response.getTemplateId());
                template.setMetaData(metaData);
                vendorResponseBuilder.setStatusCode(ResponseStatusCode.OK);
                vendorResponseBuilder.setStatus(ResponseStatus.SUCCESSFUL);
                vendorResponseBuilder.setErrorMsg(response.getTemplateId());
            }
        } catch (Exception e) {
            log.error("Exception in processing create template Karix api request: {}, error : {}", template, e.getMessage(), e);
        }
        return vendorResponseBuilder.build();
    }

    private String uploadFile(Map<String, Object> metaData) {
        String fileHandle = null;
        try {
            String fileName = karixVendor.findFile(metaData);
            if (fileName == null) return null;
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpClientRequest<HttpEntity> httpClientRequest = karixVendor.uploadMedia(fileName);
            log.info("Serving upload file request in Karix for media template {} ", httpClientRequest);

            HttpPost httppost = new HttpPost(httpClientRequest.getUrl());
            httppost.setEntity(httpClientRequest.getRequestBody());

            for (Map.Entry<String,String> entry : httpClientRequest.getHeaders().entrySet()){
                httppost.addHeader(entry.getKey(), entry.getValue());
            }
            CloseableHttpResponse response = httpClient.execute(httppost);

            /*Upload File Response Handling */
            HttpEntity responseEntity = response.getEntity();
            int statusCode = response.getStatusLine().getStatusCode();
            if (BaseHttpResponseException.Family.familyOf(statusCode) == BaseHttpResponseException.Family.SUCCESSFUL) {
                String result = EntityUtils.toString(responseEntity);
                if (result.contains("error")) {
                    log.error("Error in uploading karix file {} ", result);
                } else {
                    KarixUploadMediaResponse karixUploadMediaResponse = Serializer.DEFAULT_JSON_SERIALIZER.deserialize(result, KarixUploadMediaResponse.class);
                    log.info("Karix Response for uploading media {} ", karixUploadMediaResponse);
                    if (karixUploadMediaResponse != null && karixUploadMediaResponse.getResponse() != null) {
                        fileHandle = karixUploadMediaResponse.getResponse().getFileHandle();
                    }
                }
            }
        } catch (Exception e) {
            log.error("Exception in processing upload media Karix api request, error : {}", e.getMessage(), e);
        }
        return fileHandle;
    }

}