package com.channelsoft.ccod.support.cmdb.utils;

import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
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

import java.io.IOException;
import java.util.HashMap;
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

    public static String httpPostRequest(String url, Map<String, String> headersMap, Map<String, Object> paramsMap) throws InterfaceCallException
    {
        logger.info(String.format("http post %s, headers=%s and params=%s",
                url, JSONObject.toJSONString(headersMap), JSONObject.toJSONString(paramsMap)));
        String paramStr = JSONObject.toJSONString(paramsMap);
        System.out.println(paramStr);
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
        try
        {
            StringEntity entity = new StringEntity(jsonParam.toString(),"utf-8");//解决中文乱码问题
            entity.setContentEncoding("UTF-8");
            entity.setContentType("application/json; charset=utf-8");
            httpPost.setEntity(entity);
            CloseableHttpClient httpClient = getBasicHttpClient();
            HttpResponse response = httpClient.execute(httpPost);
            String conResult = EntityUtils.toString(response.getEntity(), "utf8");
            if (response.getStatusLine().getStatusCode() != 200)
            {
                logger.error(String.format("query url=%s FAIL: errorCode=%d and errMsg=%s",
                        response.getStatusLine().getStatusCode(), conResult));
                throw new InterfaceCallException(String.format("query url=%s return errorCode=%d",
                        response.getStatusLine().getStatusCode()));
            }
            logger.info(String.format("query url=%s return %s", url, conResult));
            return conResult;
        }
        catch (ClientProtocolException e)
        {
            logger.error(String.format("error http protocol", e));
            throw new InterfaceCallException(e.getMessage());
        }
        catch (IOException e)
        {
            logger.error(String.format("read http post return result exception", e));
            throw new InterfaceCallException(e.getMessage());
        }
    }

    public static String httpPostRequest(String url, Map<String, Object> paramsMap) throws InterfaceCallException
    {
        Map<String, String> headersMap = new HashMap<>();
        return httpPostRequest(url, headersMap, paramsMap);
    }
}
