package farm.nurture.communication.engine.vendor;

import com.google.gson.Gson;
import com.google.inject.Singleton;
import farm.nurture.communication.engine.Constants;
import farm.nurture.communication.engine.dto.HttpClientRequest;
import farm.nurture.communication.engine.dto.InteractiveAttributes;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent;
import farm.nurture.communication.engine.models.Template;
import farm.nurture.communication.engine.models.WhatsappUsers;
import farm.nurture.core.contracts.common.enums.MediaAccessType;
import farm.nurture.core.contracts.common.enums.MediaType;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.infra.util.StringUtils;
import farm.nurture.util.http.HttpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static farm.nurture.communication.engine.Constants.*;

@Slf4j
@Singleton
public class GupShupVendor extends Vendor {


    @Override
    public HttpClientRequest requestForSendSms(DerivedCommunicationEvent event) {
        HttpClientRequest sendRequest = new HttpClientRequest();
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_JSON);
        headers.put(ACCEPT, ACCEPT_VALUE);
        sendRequest.setHeaders(headers);
        queryParams.put("msg", event.getContent());
        if (event.isUnicode()) {
            queryParams.put("unicode", "1");
            queryParams.put("msg_type", "Unicode_Text");
        }
        queryParams.put("priority", "8");
        queryParams.put("v", "1.1");
        queryParams.put("userid", config.get(GUPSHUP_SMS_USERNAME));
        queryParams.put("password", config.get(GUPSHUP_SMS_PASSWORD));
        queryParams.put("send_to", event.getSmsAttributes().getMobileNumber().trim());
        queryParams.put("method", "sendMessage");
        sendRequest.setMethod(HttpUtils.HttpMethod.GET);
        sendRequest.setRequestParams(queryParams);
        sendRequest.setUrl(config.get(GUPSHUP_SMS_URL));
        return sendRequest;
    }

    public VendorType getVendorName() {
        return VendorType.GUPSHUP;
    }


    public HttpClientRequest<List<NameValuePair>> getWhatsAppData(DerivedCommunicationEvent event) {
        HttpClientRequest<List<NameValuePair>> sendRequest = new HttpClientRequest<>();
        List<NameValuePair> kvPairs = new ArrayList<>();
        DerivedCommunicationEvent.WhatsappAttributes.Media whatsappMedia = event.getWhatsappAttributes().getMedia();
        if (whatsappMedia != null) {
            kvPairs.add(new BasicNameValuePair("method", "SendMediaMessage"));

            if (whatsappMedia.getMediaAccessType() == MediaAccessType.PUBLIC_URL) {
                kvPairs.add(new BasicNameValuePair("media_url", whatsappMedia.getMediaInfo()));
            }
            kvPairs.add(new BasicNameValuePair("caption", event.getContent()));
            kvPairs.add(new BasicNameValuePair("msg_type", whatsappMedia.getMediaType().name()));
            if (MediaType.DOCUMENT.name().equals(whatsappMedia.getMediaType().name())) {
                kvPairs.add(new BasicNameValuePair("filename", whatsappMedia.getDocumentName()));
            }
        } else {
            kvPairs.add(new BasicNameValuePair("method", "SendMessage"));
            kvPairs.add(new BasicNameValuePair("format", TEXT));
            kvPairs.add(new BasicNameValuePair("msg", event.getContent()));
        }

        Map<String, Object> vendorMetaData = event.getVendorMetaData();

        if (vendorMetaData != null && vendorMetaData.containsKey(INTERACTIVE_ATTRIBUTES_KEY)) {
            InteractiveAttributes interactiveAttributes = parseIntegrativeAttributes(vendorMetaData);
            boolean isTemplate = false;
            if(interactiveAttributes!=null && StringUtils.isNonEmpty(interactiveAttributes.getButton_category())){
                isTemplate = true;
            }
            if (whatsappMedia == null && interactiveAttributes != null
                    && StringUtils.isNonEmpty(interactiveAttributes.getHeaders())) {
               isTemplate = true;
                String headerExample = getHeaderExample(interactiveAttributes, event.getPlaceholders());
                kvPairs.add(new BasicNameValuePair("header", getHeader(headerExample, interactiveAttributes.getHeaders())));
            }
            if (interactiveAttributes == null && StringUtils.isNonEmpty(interactiveAttributes.getFooter())) {
                isTemplate = true;
                kvPairs.add(new BasicNameValuePair("footer", interactiveAttributes.getFooter()));
            }
            if(isTemplate){
                kvPairs.add(new BasicNameValuePair("isTemplate", "true"));
            }
        }
        kvPairs.add(new BasicNameValuePair("userid", config.get(GUPSHUP_WHATSAPP_USERNAME)));
        kvPairs.add(new BasicNameValuePair("password", config.get(GUPSHUP_WHATSAPP_PASSWORD)));
        kvPairs.add(new BasicNameValuePair("send_to", event.getWhatsappAttributes().getMobileNumber()));
        kvPairs.add(new BasicNameValuePair("v", config.get(GUPSHUP_WHATSAPP_VERSION)));
        kvPairs.add(new BasicNameValuePair("auth_scheme", WHATSAPP_AUTH_SCHEME));
        kvPairs.add(new BasicNameValuePair("isHSM", "true"));
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_X_WWW_FORM_URLENCODED);
        headers.put(ACCEPT, ACCEPT_VALUE);
        sendRequest.setUrl(config.get(GUPSHUP_WHATSAPP_URL));
        sendRequest.setHeaders(headers);
        sendRequest.setMethod(HttpUtils.HttpMethod.POST);
        sendRequest.setRequestBody(kvPairs);
        return sendRequest;
    }


    public HttpClientRequest<List<NameValuePair>> getWhatsAppOptInData(String mobileNumber, WhatsappUsers.WhatsAppStatus optType) {
        HttpClientRequest<List<NameValuePair>> sendRequest = new HttpClientRequest<>();
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.CONTENT_TYPE, Constants.APPLICATION_X_WWW_FORM_URLENCODED);
        List<NameValuePair> kvPairs = new ArrayList<>();
        ApplicationConfiguration config = ApplicationConfiguration.getInstance();
        kvPairs.add(new BasicNameValuePair("userid", config.get(GUPSHUP_WHATSAPP_USERNAME)));
        kvPairs.add(new BasicNameValuePair("password", config.get(GUPSHUP_WHATSAPP_PASSWORD)));
        if (optType == WhatsappUsers.WhatsAppStatus.OPT_IN) {
            kvPairs.add(new BasicNameValuePair("method", WhatsappUsers.WhatsAppStatus.OPT_IN.name()));
        } else if (optType == WhatsappUsers.WhatsAppStatus.OPT_OUT) {
            kvPairs.add(new BasicNameValuePair("method", WhatsappUsers.WhatsAppStatus.OPT_OUT.name()));
        }
        kvPairs.add(new BasicNameValuePair("auth_scheme", WHATSAPP_AUTH_SCHEME));
        kvPairs.add(new BasicNameValuePair("v", config.get(GUPSHUP_WHATSAPP_VERSION)));
        kvPairs.add(new BasicNameValuePair("phone_number", mobileNumber));
        kvPairs.add(new BasicNameValuePair("channel", OPT_CHANNEL));
        kvPairs.add(new BasicNameValuePair("format", JSON));
        sendRequest.setRequestBody(kvPairs);
        sendRequest.setMethod(HttpUtils.HttpMethod.POST);
        sendRequest.setUrl(config.get(GUPSHUP_WHATSAPP_URL));
        sendRequest.setHeaders(headers);
        return sendRequest;
    }

    public HttpClientRequest<HttpEntity> createWhatsAppTemplate(Template template, String fileName) {
        HttpClientRequest<HttpEntity> httpClientRequest = new HttpClientRequest<>();
        Map<String, Object> vendorMetaDataMap = template.getMetaData();
        Map<String, String> headers = new HashMap<>();
        headers.put(ACCEPT, ACCEPT_VALUE);
        headers.put(Constants.CONTENT_TYPE, Constants.MULTIPART_FORM_DATA);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("userid", createFormData(config.get(GUPSHUP_WHATSAPP_USERNAME)));
        builder.addPart("password", createFormData(config.get(GUPSHUP_WHATSAPP_PASSWORD)));

        if (!vendorMetaDataMap.containsKey(MEDIA_TYPE) || vendorMetaDataMap.get(MEDIA_TYPE).equals(TEXT)) {
            builder.addPart("type", createFormData(TEXT));
        } else {
            builder.addPart("type", createFormData((String) template.getMetaData().get(MEDIA_TYPE)));
            builder.addPart("header_examples", createFile(fileName));
        }
        getInteractiveAttributes(builder, template);
        builder.addPart("method", createFormData(GUPSHUP_CREATE_TEMPLATE_METHOD_NAME));
        builder.addPart("category", createFormData(GUPSHUP_CATEGORY));
        builder.addPart("language", getVendorLanguage(template.getLanguageId()));
        builder.addPart("template_name", createFormData(template.getName()));
        String content=parseContent(template);
        try {
            content= URLEncoder.encode(content, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {
            log.error("Content {} error ",content,e);
        }
        builder.addPart("template", createFormData(content));

        if (template.getAttributes() != null && !template.getAttributes().isEmpty()) {
            builder.addPart("template_variable_examples", createFormData(Arrays.toString(mapAttributeValue(template.getAttributes()))));
        }
        HttpEntity httpEntity = builder.build();
        httpClientRequest.setRequestBody(httpEntity);
        httpClientRequest.setMethod(HttpUtils.HttpMethod.POST);
        httpClientRequest.setUrl(config.get(GUPSHUP_WHATSAPP_CREATE_URL));
        httpClientRequest.setHeaders(headers);
        return httpClientRequest;
    }


    private void getInteractiveAttributes(MultipartEntityBuilder builder, Template template){

        if (template.getMetaData() != null && template.getMetaData().containsKey(INTERACTIVE_ATTRIBUTES_KEY)) {
            try {
                InteractiveAttributes interactiveAttributes = parseIntegrativeAttributes(template.getMetaData());
                if (interactiveAttributes != null) {
                    if (StringUtils.isNonEmpty(interactiveAttributes.getFooter())) {
                        builder.addPart(FOOTER.toLowerCase(), createFormData(interactiveAttributes.getFooter()));
                    }
                    if (StringUtils.isNonEmpty(interactiveAttributes.getHeaders())) {
                        builder.addPart(HEADER.toLowerCase(), createFormData(getHeaderForCreateTemplateRequest(interactiveAttributes.getHeaders(), interactiveAttributes.getHeader_examples())));
                    }
                    if (StringUtils.isNonEmpty(interactiveAttributes.getButton_category()) && CollectionUtils.isNotEmpty(interactiveAttributes.getButtons())) {
                        switch (interactiveAttributes.getButton_category()) {
                            case CALL_TO_ACTION:
                                String buttonJson = new Gson().toJson(interactiveAttributes.getButtons());
                                builder.addPart("call_to_action_buttons", createFormData(buttonJson));
                                break;
                            case QUICK_REPLY:
                                List<String> buttonTextList = interactiveAttributes.getButtons().stream().map(p -> p.text).collect(Collectors.toList());
                                String buttonListJson = new Gson().toJson(buttonTextList);
                                builder.addPart("quick_reply_buttons", createFormData(buttonListJson));
                                break;
                        }
                    }
                    if (StringUtils.isNonEmpty(interactiveAttributes.getHeader_examples())) {
                        builder.addPart(HEADERS_EXAMPLE, createFormData("[" + interactiveAttributes.getHeader_examples() + "]"));
                    }
                }
            } catch (Exception e) {
                log.error("Interactive Attribute Template Exception  ", e);
            }

        }
    }


}

