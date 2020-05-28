package com.channelsoft.ccod.support.cmdb.utils;

import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: HttpRequestTools
 * @Author: lanhb
 * @Description: 封装http请求相关操作的工具类
 * @Date: 2019/12/18 9:58
 * @Version: 1.0
 */
public class HttpRequestTools {

    private final static Logger logger = LoggerFactory.getLogger(HttpRequestTools.class);

    /**
     * 创建一个CloseableHttpClient对象
     * @return 创建的CloseableHttpClient对象
     */
    public static CloseableHttpClient getCloseableHttpClient() {
        // 创建HttpClientBuilder
        CloseableHttpClient client = HttpClientBuilder.create().build();
        return client;
    }

    /**
     * 对指定的url执行http get请求
     * @param url 执行get请求的url
     * @return http接口返回的结果
     * @throws InterfaceCallException 接口调用失败
     */
    public static String httpGetRequest(String url) throws InterfaceCallException
    {
        logger.info(String.format("http get %s", url));
        HttpGet httpGet = new HttpGet(url);
        return executeGetRequest(url, httpGet);
    }


    /**
     * 执行http get请求
     * @param url get的url
     * @param headers 执行get的header
     * @return 执行get返回文本
     * @throws InterfaceCallException
     */
    public static String httpGetRequest(String url, Map<String, String> headers) throws InterfaceCallException
    {
        logger.info(String.format("http get %s", url));
        HttpGet httpGet = new HttpGet(url);
        for(String key : headers.keySet())
            httpGet.addHeader(key, headers.get(key));
        return executeGetRequest(url, httpGet);
    }
    /**
     * 对指定的url执行http get请求，该请求需要执行basic auth
     * @param url 执行get请求的url
     * @param userName basic auth所需要的用户名
     * @param password basic auth用户的密码
     * @return http接口返回的结果
     * @throws InterfaceCallException 接口调用失败
     */
    public static String httpGetRequest(String url, String userName, String password) throws InterfaceCallException
    {
        logger.info(String.format("http get %s, userName=%s and password=%s",
                url, userName, password));
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Authorization", getBasicAuthPropValue(userName, password));
        return executeGetRequest(url, httpGet);
    }

    /**
     * 封装了http get请求的具体过程
     * @param url get请求的url
     * @param httpGet 执行get请求的HttpGet实体类
     * @return http接口返回的结果
     * @throws InterfaceCallException 接口调用失败
     */
    private static String executeGetRequest(String url, HttpGet httpGet) throws InterfaceCallException
    {
        try
        {
            CloseableHttpClient httpClient = getCloseableHttpClient();
            HttpResponse response = httpClient.execute(httpGet);
            if (response.getStatusLine().getStatusCode() == 404)
            {
                logger.error(String.format("errorCode=404, url=%s not exist", url));
                throw new InterfaceCallException(String.format("errorCode=404, %s not exist", url));
            }
            String conResult = EntityUtils.toString(response.getEntity(), "utf8");
            if (response.getStatusLine().getStatusCode() != 200)
            {
                logger.error(String.format("errorCode=%d, errorMsg=%s",
                        response.getStatusLine().getStatusCode(), conResult));
                throw new InterfaceCallException(String.format("%d, %s",
                        response.getStatusLine().getStatusCode(), conResult));
            }
            return conResult;
        }
        catch (Exception ex)
        {
            logger.error(String.format("call %s exception", url), ex);
            throw new InterfaceCallException(String.format("call %s exception : ", url, ex.getMessage()));
        }
    }

    /**
     * 对指定的url执行http post请求
     * @param url post请求的url
     * @param headersMap post请求的headers信息
     * @param paramsMap post请求的params信息
     * @return 接口返回结果
     * @throws InterfaceCallException 接口调用失败
     */
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
            CloseableHttpClient httpClient = getCloseableHttpClient();
            HttpResponse response = httpClient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() == 404)
            {
                logger.error(String.format("errorCode=404, url=%s not exist", url));
                throw new InterfaceCallException(String.format("errorCode=404, %s not exist", url));
            }
            String conResult = EntityUtils.toString(response.getEntity(), "utf8");
            if (response.getStatusLine().getStatusCode() != 200)
            {
                logger.error(String.format("errorCode=%d, errorMsg=%s",
                        response.getStatusLine().getStatusCode(), conResult));
                throw new Exception(String.format("%d, %s",
                        response.getStatusLine().getStatusCode(), conResult));
            }
            return conResult;
        }
        catch (Exception ex)
        {
            logger.error(String.format("call %s exception", url), ex);
            throw new InterfaceCallException(String.format("call %s exception : ", url, ex.getMessage()));
        }
    }

    /**
     * 对指定的url执行http post请求
     * @param url 执行post请求的url
     * @param paramsMap 执行post请求所需的params
     * @return http接口返回的结果
     * @throws InterfaceCallException 接口调用失败
     */
    public static String httpPostRequest(String url, Map<String, Object> paramsMap) throws InterfaceCallException
    {
        Map<String, String> headersMap = new HashMap<>();
        return httpPostRequest(url, headersMap, paramsMap);
    }


    /**
     * 对指定的url执行http post请求,该post请求需要进行basic auth
     * @param url post请求的url
     * @param userName baisc auth的用户名
     * @param password basic auth的用户密码
     * @param headersMap post请求的headers信息
     * @param paramsMap post请求的params信息
     * @return 接口返回结果
     * @throws InterfaceCallException 接口调用失败
     */
    public static String httpPostRequest(String url, String userName, String password, Map<String, String> headersMap, Map<String, Object> paramsMap) throws InterfaceCallException
    {
        headersMap.put("Authorization", getBasicAuthPropValue(userName, password));
        return httpPostRequest(url, headersMap, paramsMap);
    }

    /**
     * 对指定的url执行http post请求,该post请求需要进行basic auth
     * @param url post请求的url
     * @param userName baisc auth的用户名
     * @param password basic auth的用户密码
     * @param paramsMap post请求的params信息
     * @return 接口返回结果
     * @throws InterfaceCallException 接口调用失败
     */
    public static String httpPostRequest(String url, String userName, String password, Map<String, Object> paramsMap) throws InterfaceCallException
    {
        Map<String, String> headersMap = new HashMap<>();
        return httpPostRequest(url, userName, password, headersMap, paramsMap);
    }

    /**
     * 生成basic auth所需的值
     * @param userName basic auth的用户名
     * @param password basic auth的用户密码
     * @return basic auth所需的值
     */
    public static String getBasicAuthPropValue(String userName, String password)
    {
        String input = userName + ":" + password;
        return "Basic " + (new sun.misc.BASE64Encoder().encode(input.getBytes()));
    }
}
