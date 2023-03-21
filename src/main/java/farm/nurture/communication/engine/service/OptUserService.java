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
import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.TimeOutConfigs;
import farm.nurture.communication.engine.dto.HttpClientRequest;
import farm.nurture.communication.engine.dto.KarixWhatsAppOptinRequest;
import farm.nurture.communication.engine.dto.WhatsAppOptUserResponse;
import farm.nurture.communication.engine.metric.MetricGroupNames;
import farm.nurture.communication.engine.metric.Metrics;
import farm.nurture.communication.engine.models.WhatsappUsers;
import farm.nurture.communication.engine.vendor.GupShupVendor;
import farm.nurture.communication.engine.vendor.KarixVendor;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.util.http.client.NFHttpClient;
import farm.nurture.util.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.NameValuePair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static farm.nurture.communication.engine.helper.Utils.validateMobileNumber;

@Slf4j
public class OptUserService {


    private static final ApplicationConfiguration config = ApplicationConfiguration.getInstance();
    private static final Metrics metrics = Metrics.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Map<String, Serializer> serializerMap = new HashMap<>();

    static {
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        serializerMap.put(Constants.APPLICATION_JSON, new PlainJsonSerializer());
    }

    @Inject
    private NFHttpClient nfHttpClient;
    @Inject
    private KarixVendor karixVendor;
    @Inject
    private GupShupVendor gupShupVendor;

    public WhatsAppOptUserResponse whatsappOptUser(String mobileNumber, WhatsappUsers.WhatsAppStatus optType) {
        log.info("Mobile number for whatsApp optUser request. Mobile number: {}, optType: {}", mobileNumber, optType);

        if (validateMobileNumber(mobileNumber)) {
            WhatsAppOptUserResponse whatsAppOptUserResponse = null;
            boolean isKarixOptIn = false;
            try {
                for(int retryCount = 0; retryCount < 3; retryCount++){
                    whatsAppOptUserResponse = gupshupWhatsAppOptIn(mobileNumber, optType);
                    if (whatsAppOptUserResponse != null && whatsAppOptUserResponse.getResponse() !=null &&
                            whatsAppOptUserResponse.getResponse().getStatus().equalsIgnoreCase("success"))
                     break;
                }
                for(int retryCount=0;  retryCount< 3; retryCount++){
                    isKarixOptIn = karixWhatsAppOptIn(mobileNumber, optType);
                    if(isKarixOptIn) break;
                }
                return whatsAppOptUserResponse;
            } catch (Exception exception) {
                log.error("WhatsApp OptIn API failed for exception ", exception);
                return null;
            }

        } else {
            log.error("Given mobile number: {} is wrong", mobileNumber);
            metrics.onIncrement(MetricGroupNames.NF_CE, "whatsapp_opt_user_wrong_mobile_number");
            return null;
        }
    }


    public boolean karixWhatsAppOptIn(String mobileNumber, WhatsappUsers.WhatsAppStatus optType) {
        log.info("Mobile number for karix whatsApp optUser request. Mobile number: {}, optType: {}", mobileNumber, optType);
        HttpClientRequest<KarixWhatsAppOptinRequest> httpClientRequest = karixVendor.getWhatsAppOptInData(mobileNumber, optType);
        try {

            String response = nfHttpClient.sendMessage(httpClientRequest.getMethod(), httpClientRequest.getUrl(),
                    httpClientRequest.getRequestParams(), httpClientRequest.getHeaders(), httpClientRequest.getRequestBody(),
                    String.class, serializerMap,TimeOutConfigs.OptUserServiceTimeOutConfig());

            log.info("karix response for optIn user is: {}", response);
            return patternMatching(response);
        } catch (Exception exc) {
            log.error("Error in Karix Optin {}, mobile number {}", exc, mobileNumber);
            return false;
        }
    }

    public WhatsAppOptUserResponse gupshupWhatsAppOptIn(String mobileNumber, WhatsappUsers.WhatsAppStatus optType) {
        log.info("Mobile number for gupshup whatsApp optUser request. Mobile number: {}, optType: {}", mobileNumber, optType);
        WhatsAppOptUserResponse whatsAppOptUserResponse = new WhatsAppOptUserResponse();
        HttpClientRequest<List<NameValuePair>> httpClientRequest = gupShupVendor.getWhatsAppOptInData(mobileNumber, optType);
        try {

            whatsAppOptUserResponse = nfHttpClient.sendMessage(httpClientRequest.getMethod(), httpClientRequest.getUrl(),
                    httpClientRequest.getRequestParams(), httpClientRequest.getHeaders(), httpClientRequest.getRequestBody(),
                    WhatsAppOptUserResponse.class, serializerMap, TimeOutConfigs.OptUserServiceTimeOutConfig());
            log.info("WhatsAppOptInUserResponse for optUser Gupshup is: {}", whatsAppOptUserResponse);
        } catch (Exception exp) {
            log.error("Error in Gupshup Optin {}, mobile number {}", exp,  mobileNumber);
        }
        return whatsAppOptUserResponse;
    }

    private boolean patternMatching(String actualResponse) {
        Pattern pattern = Pattern.compile("success");
        Matcher matcher = pattern.matcher(actualResponse);
        return matcher.find();
    }

}
