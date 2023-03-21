package farm.nurture.communication.engine.vendor;

import com.google.gson.Gson;
import com.google.inject.Singleton;
import farm.nurture.communication.engine.dto.HttpClientRequest;
import farm.nurture.communication.engine.dto.InteractiveAttributes;
import farm.nurture.communication.engine.event.DerivedCommunicationEvent;
import farm.nurture.communication.engine.models.Template;
import farm.nurture.communication.engine.models.WhatsappUsers;
import farm.nurture.core.contracts.common.enums.LanguageCode;
import farm.nurture.core.contracts.common.enums.MediaType;
import farm.nurture.infra.util.ApplicationConfiguration;
import farm.nurture.infra.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Consts;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static farm.nurture.communication.engine.Constants.*;


@Slf4j
@Singleton
public abstract class Vendor {

    public static ApplicationConfiguration config;
    public static Map<LanguageCode, String> languageToVendorLanguageMap;

    public abstract HttpClientRequest requestForSendSms(DerivedCommunicationEvent event);

    public abstract VendorType getVendorName();

    public abstract HttpClientRequest getWhatsAppData(DerivedCommunicationEvent event);

    public abstract HttpClientRequest getWhatsAppOptInData(String mobileNumber, WhatsappUsers.WhatsAppStatus optType);

    public abstract HttpClientRequest createWhatsAppTemplate(Template template, String file);


    public String getHeaderForCreateTemplateRequest(String header, String headerExamples){
        if(StringUtils.isEmpty(headerExamples)) return header;
        return header.replaceAll("\\{\\{.*?\\}\\}", "{{1}}");
    }

    public String getHeaderExample(InteractiveAttributes interactiveAttributes, Map<String, Object> placeHolderMap) {
        if(interactiveAttributes == null || StringUtils.isEmpty(interactiveAttributes.getHeader_examples())) return null;
        String headerExample;
        if (placeHolderMap != null && placeHolderMap.containsKey(interactiveAttributes.getHeader_examples())) {
            headerExample = (String) placeHolderMap.get(interactiveAttributes.getHeader_examples());
        } else {
            headerExample = interactiveAttributes.getHeader_examples();
        }
        return headerExample;
    }

    public String getHeader(String headerExample, String header){
        if(StringUtils.isEmpty(headerExample)) return header;
        return header.replaceAll("\\{\\{.*?\\}\\}", headerExample);
    }

    public String parseContent(Template template) {
        if (template.getAttributes() == null || template.getAttributes().size() == 0) return template.getContent();
        String content = template.getContent();
        for (Map.Entry<String, String> entry : template.getAttributes().entrySet()) {
            content = content.replace("{{" + entry.getValue() + "}}", "{{" + (Integer.parseInt(entry.getKey()) + 1) + "}}");
        }
       return content;
    }


    public InteractiveAttributes parseIntegrativeAttributes(Map<String, Object> vendorMetaData) {
        InteractiveAttributes interactiveAttributes = null;
        if (vendorMetaData != null && vendorMetaData.containsKey(INTERACTIVE_ATTRIBUTES_KEY)) {
            try {
                 interactiveAttributes = new Gson().fromJson((String) vendorMetaData.get(INTERACTIVE_ATTRIBUTES_KEY), InteractiveAttributes.class);
            } catch (Exception e) {
                log.error("Interactive Attribute Template Exception", e);
            }
        }
        return interactiveAttributes;
    }

    public void init() {
        languageToVendorLanguageMap = new HashMap<>();
        config = ApplicationConfiguration.getInstance();
        languageToVendorLanguageMap.put(LanguageCode.EN_US, "en_US");
        languageToVendorLanguageMap.put(LanguageCode.HI_IN, "hi");
        languageToVendorLanguageMap.put(LanguageCode.GU, "gu");
        languageToVendorLanguageMap.put(LanguageCode.PA, "pa");
        languageToVendorLanguageMap.put(LanguageCode.KA, "kn");
        languageToVendorLanguageMap.put(LanguageCode.TA, "ta");
        languageToVendorLanguageMap.put(LanguageCode.TE, "te");
        languageToVendorLanguageMap.put(LanguageCode.BN, "bn");
        languageToVendorLanguageMap.put(LanguageCode.MR, "mr");
        languageToVendorLanguageMap.put(LanguageCode.KN, "kn");
    }


    public StringBody getVendorLanguage(Short languageId) {
        String vendorLanguage = languageToVendorLanguageMap.get(LanguageCode.valueOf(languageId));
        return createFormData(vendorLanguage);
    }

    public StringBody createFormData(String value) {
        return new StringBody(value,  ContentType.create("multipart/form-data", Consts.UTF_8));
    }


    public String getFormat(String mediaType) {
        String media = TEXT;
        switch (mediaType) {
            case IMAGE:
                media = MediaType.IMAGE.name();
                break;
            case DOCUMENT:
                media = MediaType.DOCUMENT.name();
                break;
            case VIDEO:
                media = MediaType.VIDEO.name();
                break;
        }
        return media;
    }

    public String findFileType(String extension) {
        String fileType = "image/png";
        switch (extension) {
            case "png":
                fileType = "image/png";
                break;
            case "mp4":
                fileType = "video/mp4";
                break;
            case "pdf":
                fileType = "application/pdf";
                break;
        }
        return fileType;
    }

    public Optional<String> findFileExtension(String fileName) {
        return Optional.ofNullable(fileName)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(fileName.lastIndexOf(".") + 1));
    }


    public FileBody createFile(String fileName){
        File file = new File("/communication-engine/"+fileName);
        return new FileBody(file);
    }


    public String findFile(Map<String, Object> metaData) {
        log.info("Finding media file for meta data {} ", metaData);
        if (metaData == null || !metaData.containsKey(MEDIA_TYPE)) return null;

        String mediaType = (String) metaData.get(MEDIA_TYPE);
        String fileName = null;
        switch (mediaType) {
            case TEXT:
                fileName = null;
                break;
            case IMAGE:
                fileName = "sample_img.png";
                break;
            case VIDEO:
                fileName = "sample_video.mp4";
                break;
            case DOCUMENT:
                fileName = "sample_doc.pdf";
                break;
        }
        return fileName;
    }

    public String[] mapAttributeValue(Map<String, String> attributeMap) {
        return attributeMap.values().toArray(new String[0]);
    }
}
