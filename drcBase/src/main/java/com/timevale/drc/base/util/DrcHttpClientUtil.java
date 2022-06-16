package com.timevale.drc.base.util;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * @author gwk_2
 * @date 2021/12/21 11:48
 */
@Slf4j
public class DrcHttpClientUtil {

    private static CloseableHttpClient httpClient;

    /**
     * get 请求,并返回 String 数据.
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static String getAndReturnString(String url) throws IOException {
        return EntityUtils.toString(get(url).getEntity());
    }

    /**
     * post 请求, 没有 body 参数.
     *
     * @param url
     * @return CloseableHttpResponse
     * @throws IOException
     */
    public static CloseableHttpResponse post(String url) throws IOException {
        return post(url, "");
    }

    /**
     * post 请求, 有 body 参数.
     *
     * @param url
     * @param bodyText
     * @return String
     * @throws IOException
     */
    public static String postAndReturnString(String url, String bodyText) throws IOException {
        return EntityUtils.toString(post(url, bodyText).getEntity());
    }

    @SneakyThrows
    public static String getBodyText(CloseableHttpResponse response) {
        return EntityUtils.toString(response.getEntity());
    }

    public static CloseableHttpResponse get(String url) throws IOException {
        CloseableHttpClient client = getClient();

        final HttpGet httpGet = new HttpGet(url);

        httpGet.setConfig(getDefaultConfig());
        httpGet.setHeader("Connection", "keep-alive");

        CloseableHttpResponse response = client.execute(httpGet);
        return response;
    }

    public static CloseableHttpResponse getAcceptJson(String url) throws IOException {
        CloseableHttpClient client = getClient();

        final HttpGet httpGet = new HttpGet(url);
        // json
        httpGet.setHeader("Accept", "application/json");
        httpGet.setHeader("Content-Type", "application/json");
        httpGet.setHeader("Connection", "keep-alive");

        httpGet.setConfig(getDefaultConfig());

        CloseableHttpResponse response = client.execute(httpGet);
        return response;
    }

    private static CloseableHttpResponse post(String url, String bodyText) throws IOException {
        CloseableHttpClient client = getClient();

        final HttpPost httpPost = new HttpPost(url);
        // json
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("Content-Type", "application/json");
        httpPost.setHeader("Connection", "keep-alive");

        httpPost.setConfig(getDefaultConfig());

        StringEntity s = new StringEntity(bodyText, "UTF-8");
        httpPost.setEntity(s);

        CloseableHttpResponse response = client.execute(httpPost);
        return response;
    }


    private static RequestConfig getDefaultConfig() {
        RequestConfig.Builder builder = RequestConfig.custom();
        builder.setSocketTimeout(5000)
                .setConnectTimeout(3000)
                .setConnectionRequestTimeout(300);
        return builder.build();
    }

    private static CloseableHttpClient getClient() {
        if (httpClient != null) {
            return httpClient;
        }
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(5000);
        connectionManager.setDefaultMaxPerRoute(500);

        httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setKeepAliveStrategy(DefaultConnectionKeepAliveStrategy.INSTANCE)
                .disableAutomaticRetries()
                .build();

        return httpClient;
    }
}
