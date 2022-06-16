package com.timevale.drc.pd.service.kms;

import com.timevale.drc.base.serialize.JackSonUtil;
import com.timevale.drc.base.util.DrcHttpClientUtil;
import com.timevale.drc.pd.service.kms.eureka.EurekaAppDTOResponse;
import com.timevale.drc.pd.service.kms.eureka.EurekaInstanceDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * @author shanreng
 *
 * esign kms
 */
@Component
@Slf4j
public class KmsComponent {

    @Value("${kms.eureka.domain:}")
    private String eurekaDomain;

    private EurekaAppDTOResponse eurekaAppDTOResponse;

    public String kmsDecrypt(String unDecrypt) throws IOException {
        if(StringUtils.isEmpty(unDecrypt) ||
                !unDecrypt.contains("kmsKeyId") ||
                !unDecrypt.contains("cipherText")){
            // 普通数据，无需解密
            return unDecrypt;
        }

        if(!kmsServiceValid()) {
            // 如果服务有问题，上游每次重试都会尝试初始化
            initEurekaAppDTOResponse();
        }

        KmsValueDTO kmsValueDTO = JackSonUtil.string2Obj(unDecrypt,KmsValueDTO.class);

        // 简单起见，直接调用第一个服务实例
        String kmsServiceHostUrl = getEurekaAppDTOResponse().getApplication()
                .getInstance().get(0).getHomePageUrl();
        String url = kmsServiceHostUrl + "/decryptStrByMainKey/keyId/encryptedContent";

        KmsDecryptParamDTO kmsDecryptParamDTO = new KmsDecryptParamDTO(kmsValueDTO.getKmsKeyId(),kmsValueDTO.getCipherText());
        String response = DrcHttpClientUtil.postAndReturnString(url,JackSonUtil.obj2String(kmsDecryptParamDTO));
        KmsDecryptResult kmsDecryptResult = JackSonUtil.string2Obj(response,KmsDecryptResult.class);
        String decryptContent = kmsDecryptResult.getDecryptContent();
        log.info("kmsDecrypt success, unDecrypt={}, decryptContent={}",unDecrypt,decryptContent);

        return decryptContent;
    }

    private synchronized EurekaAppDTOResponse getEurekaAppDTOResponse() throws IOException {
        if(this.eurekaAppDTOResponse == null){
            initEurekaAppDTOResponse();
            return this.eurekaAppDTOResponse;
        }

        return this.eurekaAppDTOResponse;
    }

    private synchronized void initEurekaAppDTOResponse() throws IOException {
        String url = eurekaDomain + "/eureka/apps/km-service";
        CloseableHttpResponse response = DrcHttpClientUtil.getAcceptJson(url);
        String result = EntityUtils.toString(response.getEntity(), "utf-8");

        this.eurekaAppDTOResponse = JackSonUtil.string2Obj(result,EurekaAppDTOResponse.class);
    }

    /**
     * this.eurekaAppDTOResponse不为空，且instance列表不为空
     * 则return true
     * */
    private synchronized boolean kmsServiceValid(){
        if(this.eurekaAppDTOResponse == null){
            return false;
        }

        List<EurekaInstanceDTO> instanceDTOList = this.eurekaAppDTOResponse.getApplication().getInstance();
        return !instanceDTOList.isEmpty();
    }
}
