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
        String url = kmsServiceHostUrl + "null";


        String jsonParam = "{\n" +
                "    \"keyId\": \"null\",\n" +
                "    \"encryptedContent\": \"null\n" +
                "}";

        String result = DrcHttpClientUtil.postAndReturnString(url,jsonParam);
        System.out.println(result);
    }

    private static EurekaAppDTOResponse findKmsService() throws IOException {
        String domainName = "http://172.20.0.0:7001";
        String appId = "drcWorker";
        String url = domainName + "/eureka/apps/km?" + "appId=" + appId;
        CloseableHttpResponse response = DrcHttpClientUtil.getAcceptJson(url);
        String result = EntityUtils.toString(response.getEntity(), "utf-8");
        EurekaAppDTOResponse eurekaAppDTOResponse = JackSonUtil.string2Obj(result,EurekaAppDTOResponse.class);
        System.out.println(eurekaAppDTOResponse);

        return eurekaAppDTOResponse;
    }
}

