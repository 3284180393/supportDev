package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.po.NexusComponentPo;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: NexusServiceImpl
 * @Author: lanhb
 * @Description: INexusService接口实现类
 * @Date: 2019/11/14 13:50
 * @Version: 1.0
 */
@Service
public class NexusServiceImpl implements INexusService {

    @Value("${nexus.user}")
    private String userName;

    @Value("${nexus.password}")
    private String password;

    @Value("${nexus.repository_name}")
    private String repository;

    @Value("${nexus.query_repository_url}")
    private String queryRepositoryUrl;

    @Value("${nexus.query_component_url}")
    private String queryComponentUrl;

    @Value("${nexus.query_asset_url}")
    private String queryAssetUrl;

    @Value("${nexus.upload_raw_url}")
    private String uploadRawUrl;

    private final static Logger logger = LoggerFactory.getLogger(NexusServiceImpl.class);

    @Override
    public boolean uploadRawFile(String sourceFilePath, String group, String fileName) throws Exception {
        logger.debug(String.format("begin to upload %s to %s/%s/%s, uploadUrl=%s",
                sourceFilePath, repository, group, fileName, this.uploadRawUrl));
        HttpClient httpclient = getBasicHttpClient(this.userName, this.password);
        HttpPost httppost = new HttpPost(this.uploadRawUrl);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("raw.directory", group));
        nvps.add(new BasicNameValuePair("raw.asset1", sourceFilePath));
        nvps.add(new BasicNameValuePair("raw.asset1.filename", fileName));
        httppost.setEntity(new UrlEncodedFormEntity(nvps));
        HttpResponse response = httpclient.execute(httppost);
        String conResult = EntityUtils.toString(response.getEntity(), "utf8");
        boolean ret;
        if (response.getStatusLine().getStatusCode() == 200)
        {
            logger.debug(String.format("upload %s to %s/%s/%s SUCCESS", sourceFilePath, repository, group, fileName));
            ret = true;
        }
        else
        {
            logger.debug(String.format("upload %s to %s/%s/%s FAIL : %s", sourceFilePath, repository, group, fileName, conResult));
            ret = false;
        }
        return ret;
    }

    @Override
    public NexusComponentPo queryComponentById(String componentId) throws Exception {
        String url = this.queryComponentUrl.replace("\\{component_id\\}", componentId);
        logger.debug(String.format("begin to query id=%s component info, queryUrl=%s", componentId, url));
        HttpClient httpclient = getBasicHttpClient(this.userName, this.password);
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpGet);
        if (response.getStatusLine().getStatusCode() == 404)
        {
            logger.error(String.format("id=%s component not exist", componentId));
            throw new Exception(String.format("id=%s component not exist", componentId));
        }
        else if (response.getStatusLine().getStatusCode() != 200)
        {
            logger.error(String.format("query id=%s component FAIL : server return %d code",
                    componentId, response.getStatusLine().getStatusCode()));
            throw new Exception(String.format("query id=%s component FAIL : server return %d code",
                    componentId, response.getStatusLine().getStatusCode()));
        }
        String conResult = EntityUtils.toString(response.getEntity(), "utf8");
        NexusComponentPo info = JSONObject.parseObject(conResult, NexusComponentPo.class);
        logger.info(String.format("query id=%s component SUCCESS", componentId));
        return info;
    }

    @Override
    public NexusAssetInfo queryAssetById(String assetId) throws Exception {
        String url = this.queryAssetUrl.replace("\\{asset_id\\}", assetId);
        logger.debug(String.format("begin to query id=%s asset info, queryUrl=%s", assetId, url));
        HttpClient httpclient = getBasicHttpClient(this.userName, this.password);
        HttpGet httpGet = new HttpGet(url);
        HttpResponse response = httpclient.execute(httpGet);
        if (response.getStatusLine().getStatusCode() == 422)
        {
            logger.error(String.format("id=%s asset not exist", assetId));
            throw new Exception(String.format("id=%s asset not exist", assetId));
        }
        else if (response.getStatusLine().getStatusCode() != 200)
        {
            logger.error(String.format("query id=%s asset FAIL : server return %d code",
                    assetId, response.getStatusLine().getStatusCode()));
            throw new Exception(String.format("query id=%s asset FAIL : server return %d code",
                    assetId, response.getStatusLine().getStatusCode()));
        }
        String conResult = EntityUtils.toString(response.getEntity(), "utf8");
        NexusAssetInfo info = JSONObject.parseObject(conResult, NexusAssetInfo.class);
        logger.info(String.format("query id=%s asset SUCCESS", assetId));
        return info;
    }

    @Override
    public NexusComponentPo[] queryComponentFromRepository() throws Exception {
        logger.debug(String.format("begin to query all components from repository=%s, queryUrl=%s",
                repository, this.queryRepositoryUrl));
        HttpClient httpclient = getBasicHttpClient(this.userName, this.password);
        HttpGet httpGet = new HttpGet(this.queryRepositoryUrl);
        HttpResponse response = httpclient.execute(httpGet);
        if (response.getStatusLine().getStatusCode() == 404)
        {
            logger.error(String.format("repository=%s not exist", this.repository));
            throw new Exception(String.format("repository=%s not exist", this.repository));
        }
        else if (response.getStatusLine().getStatusCode() != 200)
        {
            logger.error(String.format("query all components from repository=%s FAIL : server return %d code",
                    repository, response.getStatusLine().getStatusCode()));
            throw new Exception(String.format("query all components from repository=%s FAIL : server return %d code",
                    repository, response.getStatusLine().getStatusCode()));
        }

        String conResult = EntityUtils.toString(response.getEntity(), "utf8");
        List<NexusComponentPo> components = JSONArray.parseArray(conResult, NexusComponentPo.class);
        logger.info(String.format("repository=%s has %d components", repository, components.size()));
        return components.toArray(new NexusComponentPo[0]);
    }

    private CloseableHttpClient getBasicHttpClient(String username, String password) {
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
}
