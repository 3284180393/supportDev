package com.channelsoft.ccod.support.cmdb.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @ClassName: HttpTools
 * @Author: lanhb
 * @Description: 定义的http工具类
 * @Date: 2019/12/12 17:42
 * @Version: 1.0
 */
public class HttpTools {

    private final static Logger logger = LoggerFactory.getLogger(HttpTools.class);

    private static CloseableHttpClient getBasicHttpClient(String username, String password) {
        // 创建HttpClientBuilder
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        // 设置BasicAuth
        CredentialsProvider provider = new BasicCredentialsProvider();
        // Create the authentication scope
        AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
        // Create credential pair，在此处填写用户名和密码
        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username,password);
        // Inject the credentials
        provider.setCredentials(scope, credentials);
        // Set the default credentials provider
        httpClientBuilder.setDefaultCredentialsProvider(provider);
        // HttpClient
        CloseableHttpClient closeableHttpClient = httpClientBuilder.build();
        return closeableHttpClient;
    }

    private static CloseableHttpClient getBasicHttpClient() {
        // 创建HttpClientBuilder
        CloseableHttpClient client = HttpClientBuilder.create().build();
        return client;
    }

    public static String httpGetRequest(String url, Map<String, String> headersMap, Map<String, Object> paramsMap) throws Exception
    {
        logger.info(String.format("http get %s, headers=%s and params=%s",
                url, JSONObject.toJSONString(headersMap), JSONObject.toJSONString(paramsMap)));
        HttpGet httpGet = new HttpGet(url);
        for(Map.Entry<String, String> entry : headersMap.entrySet())
        {
            httpGet.addHeader(entry.getKey(), entry.getValue());
        }
        JSONObject jsonParam = new JSONObject();
        for(Map.Entry<String, Object> entry : paramsMap.entrySet())
        {
            jsonParam.put(entry.getKey(), entry.getValue());
        }
        StringEntity entity = new StringEntity(jsonParam.toString(),"utf-8");//解决中文乱码问题
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        CloseableHttpClient httpClient = getBasicHttpClient();
        HttpResponse response = httpClient.execute(httpGet);
        String conResult = EntityUtils.toString(response.getEntity(), "utf8");
        if (response.getStatusLine().getStatusCode() != 200)
        {
            logger.error(String.format("query url=%s FAIL: errorCode=%d and errMsg=%s",
                    response.getStatusLine().getStatusCode(), conResult));
            throw new Exception(String.format("query url=%s return errorCode=%d",
                    response.getStatusLine().getStatusCode()));
        }
        logger.info(String.format("query url=%s return %s", url, conResult));
        return conResult;
    }

    public static String httpPostRequest(String url, Map<String, String> headersMap, Map<String, Object> paramsMap) throws Exception
    {
        logger.info(String.format("http post %s, headers=%s and params=%s",
                url, JSONObject.toJSONString(headersMap), JSONObject.toJSONString(paramsMap)));
        HttpPost httpPost = new HttpPost(url);
        for(Map.Entry<String, String> entry : headersMap.entrySet())
        {
            httpPost.addHeader(entry.getKey(), entry.getValue());
        }
        JSONObject jsonParam = new JSONObject();
        for(Map.Entry<String, Object> entry : paramsMap.entrySet())
        {
            jsonParam.put(entry.getKey(), entry.getValue());
        }
        String paramStr = JSONObject.toJSONString(jsonParam);
        System.out.println(paramStr);
        StringEntity entity = new StringEntity(jsonParam.toString(),"utf-8");//解决中文乱码问题
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        CloseableHttpClient httpClient = getBasicHttpClient();
        HttpResponse response = httpClient.execute(httpPost);
        String conResult = EntityUtils.toString(response.getEntity(), "utf8");
        if (response.getStatusLine().getStatusCode() != 200)
        {
            logger.error(String.format("query url=%s FAIL: errorCode=%d and errMsg=%s",
                    response.getStatusLine().getStatusCode(), conResult));
            throw new Exception(String.format("query url=%s return errorCode=%d",
                    response.getStatusLine().getStatusCode()));
        }
        logger.info(String.format("query url=%s return %s", url, conResult));
        return conResult;
    }
}
