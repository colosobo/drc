package com.timevale.drc.pd.service.kms;

import com.timevale.drc.base.serialize.JackSonUtil;
import com.timevale.drc.base.util.DrcHttpClientUtil;
import com.timevale.drc.pd.service.kms.eureka.EurekaAppDTOResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public class KmsTest {

    public static void main(String[] args) throws IOException {
        EurekaAppDTOResponse eurekaAppDTOResponse = findKmsService();
        String kmsServiceHostUrl = eurekaAppDTOResponse.getApplication().getInstance().get(0).getHomePageUrl();
        String url = kmsServiceHostUrl + "/decryptStrByMainKey/keyId/encryptedContent";


        String jsonParam = "{\n" +
                "    \"keyId\": \"357B9CCFA9914A3C802F7F9FBDFEBAB8\",\n" +
                "    \"encryptedContent\": \"1Z2qVMNcRlyIpicGCRI8JPCh0fBXAhJtFwT3iKr/BFX8=\"\n" +
                "}";

        String result = DrcHttpClientUtil.postAndReturnString(url,jsonParam);
        System.out.println(result);
    }

    private static EurekaAppDTOResponse findKmsService() throws IOException {
        String domainName = "http://172.20.61.183:7001";
        String appId = "drcWorker";
        String url = domainName + "/eureka/apps/km-service?" + "appId=" + appId;
        CloseableHttpResponse response = DrcHttpClientUtil.getAcceptJson(url);
        String result = EntityUtils.toString(response.getEntity(), "utf-8");
        EurekaAppDTOResponse eurekaAppDTOResponse = JackSonUtil.string2Obj(result,EurekaAppDTOResponse.class);
        System.out.println(eurekaAppDTOResponse);

        return eurekaAppDTOResponse;
    }
}

