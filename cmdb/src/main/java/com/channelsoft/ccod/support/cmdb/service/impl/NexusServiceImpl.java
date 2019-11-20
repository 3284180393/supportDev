package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.po.NexusComponentPo;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.channelsoft.ccod.support.cmdb.vo.DeployFileInfo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppModuleVo;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    @Value("${nexus.platform_app_cfg_directory_fmt}")
    private String platformAppCfgDirectoryFmt;

    @Value("${nexus.platform_app_cfg_repository}")
    private String platformAppCfgRepository;

    @Value("${nexus.app_module_component_directory_fmt}")
    private String appDirectoryFmt;

    @Value("${nexus.app_module_repository}")
    private String appRepository;

    private final Map<String, Map<String, NexusComponentPo>> repositoryComponentMap = new ConcurrentHashMap<>();

    private final static Logger logger = LoggerFactory.getLogger(NexusServiceImpl.class);

    @Override
    public boolean uploadRawFile(String repository, String sourceFilePath, String group, String fileName) throws Exception {
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
    public NexusComponentPo[] queryComponentFromRepository(String repository) throws Exception {
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

    @Override
    public NexusComponentPo[] uploadRawComponent(String repository, String directory, DeployFileInfo[] componentFiles) throws Exception {
        String url = String.format(this.uploadRawUrl, repository);
        HttpClient httpclient = getBasicHttpClient(this.userName, this.password);
        HttpPost httppost = new HttpPost(url);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("raw.directory", directory));
        for(int i = 1; i <= componentFiles.length; i++)
        {
            nvps.add(new BasicNameValuePair("raw.asset" + i, componentFiles[i-1].getLocalSavePath()));
            nvps.add(new BasicNameValuePair("raw.asset" + i + ".filename", componentFiles[i-1].getFileName()));
        }
        httppost.setEntity(new UrlEncodedFormEntity(nvps));
        HttpResponse response = httpclient.execute(httppost);
        String conResult = EntityUtils.toString(response.getEntity(), "utf8");
        if (response.getStatusLine().getStatusCode() != 204)
        {
            logger.debug(String.format("upload %s to %s/%s FAIL : %s and return code=%d",
                    repository, directory, conResult, response.getStatusLine().getStatusCode()));
            throw new Exception(String.format("upload %s to %s/%s FAIL : %s and return code=%d",
                    repository, directory, conResult, response.getStatusLine().getStatusCode()));
        }
        NexusComponentPo[] nexusComps = queryRepositoryAllComponent(repository);
        Map<String, NexusAssetInfo> assetMap = new HashMap<>();
        for(NexusComponentPo componentPo : nexusComps)
        {
            for(NexusAssetInfo assetInfo : componentPo.getAssets())
            {
                assetMap.put(assetInfo.getPath(), assetInfo);
            }
        }
        boolean isUploadSuccess = true;
        for(DeployFileInfo info : componentFiles)
        {
            String path = directory + "/" + info.getFileName();
            if(!assetMap.containsKey(path))
            {
                logger.error(String.format("upload %s to %s/%s FAIL",
                        info.getLocalSavePath(), directory, info.getFileName()));
                isUploadSuccess = false;
            }
            else
            {
                NexusAssetInfo assetInfo = assetMap.get(path);
                if(!info.getFileMd5().equals(assetInfo.getMd5()))
                {
                    logger.error(String.format("upload %s to %s/%s FAIL : wanted md5=%s and receive md5=%s",
                            info.getLocalSavePath(), directory, info.getFileName(), info.getFileMd5(), assetInfo.getMd5()));
                    isUploadSuccess = false;
                }
                else
                {
                    info.setNexusAssetId(assetInfo.getId());
                    info.setNexusDirectory(directory);
                    info.setNexusRepository(repository);
                }
            }
        }
        if(!isUploadSuccess)
        {
            logger.error(String.format("upload component to %s/%s FAIL", repository, directory));
            throw new Exception(String.format("upload component to %s/%s FAIL", repository, directory));
        }
        return nexusComps;
    }

    /**
     * 从指定仓库查询所有的components
     * @param repository
     * @return 查询结果
     * @throws Exception
     */
    private NexusComponentPo[] queryRepositoryAllComponent(String repository) throws Exception
    {
        logger.debug(String.format("begin to query all components of repository=%s", repository));
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

    @Override
    public void reloadRepositoryComponent(String repository) throws Exception {
        NexusComponentPo[] components = this.queryRepositoryAllComponent(repository);
        Map<String, NexusComponentPo> componentMap = new HashMap<>();
        for(NexusComponentPo po : components)
        {
            componentMap.put(po.getGroup(), po);
        }
        this.repositoryComponentMap.put(repository, componentMap);
    }

    @Override
    public void releaseRepositoryComponent(String repository) {
        if(this.repositoryComponentMap.containsKey(repository))
        {
            this.repositoryComponentMap.get(repository).clear();
            this.repositoryComponentMap.remove(repository);
        }
    }

    @Override
    public void addPlatformAppModule(PlatformAppModuleVo module) throws Exception {
        String modulePath = String.format(this.appDirectoryFmt, module.getModuleName(), module.getModuleAliasName(), module.getVersion());
        if(this.repositoryComponentMap.get(this.appRepository).containsKey(modulePath))
        {
            NexusComponentPo savedComp = this.repositoryComponentMap.get(this.appRepository).get(modulePath);
            if(savedComp.getAssets().length != module.getCfgs().length + 1)
            {
                logger.error(String.format("platform=%s and domain=%s and hostname=%s and hostip=%s and basepath=%s %s file is %d but saved same version file is %d",
                        module.getPlatformId(), module.getDomainName(), module.getHostName(), module.getHostIp(),
                        module.getBasePath(), savedComp, module.getCfgs().length + 1), savedComp.getAssets().length);
                throw new Exception(String.format("collect %s file count not equal with same app", modulePath));
            }
            Map<String, NexusAssetInfo> assetMap = new HashMap<>();
            for(NexusAssetInfo info : savedComp.getAssets())
            {
                assetMap.put(info.getPath(), info);
            }
            if(!assetMap.containsKey("/" + modulePath + "/" + module.getInstallPackage().getFileName()))
            {
                logger.error(String.format("platform=%s and domain=%s and hostname=%s and hostip=%s and basepath=%s %s install package name=%s not in saved same version module",
                        module.getPlatformId(), module.getDomainName(), module.getHostName(), module.getHostIp(),
                        module.getBasePath(), savedComp, module.getInstallPackage().getFileName()));
                throw new Exception(String.format("collect %s install package name not equal with the same app", modulePath));
            }
            else
            {
                NexusAssetInfo savedAsset = assetMap.get("/" + modulePath + "/" + module.getInstallPackage().getFileName());
                if(!savedAsset.getMd5().equals(module.getInstallPackage().getFileMd5()))
                {
                    logger.error(String.format("platform=%s and domain=%s and hostname=%s and hostip=%s and basepath=%s %s install package name=%s's md5 not equal with saved same version module %s",
                            module.getPlatformId(), module.getDomainName(), module.getHostName(), module.getHostIp(),
                            module.getBasePath(), savedComp, module.getInstallPackage().getFileName(), module.getInstallPackage().getFileMd5(), savedAsset.getMd5()));
                    throw new Exception(String.format("collect %s install package md5 not equal with the same app", modulePath));
                }
            }

        }
        else
        {
            List<DeployFileInfo> uploadFiles = new ArrayList<>();
            uploadFiles.add(module.getInstallPackage());
            for(DeployFileInfo cfg : module.getCfgs())
            {
                uploadFiles.add(cfg);
            }
            NexusComponentPo[] componentPos = this.uploadRawComponent(this.appRepository, modulePath, uploadFiles.toArray(new DeployFileInfo[0]));
            Map<String, NexusComponentPo> componentMap = new HashMap<>();
            for(NexusComponentPo componentPo : componentPos)
            {
                componentMap.put(componentPo.getGroup(), componentPo);
            }
            this.repositoryComponentMap.put(this.appRepository, componentMap);
        }
        String cfgDirectory = String.format(this.platformAppCfgDirectoryFmt, module.getModuleName(), module.getModuleAliasName(),
                module.getPlatformId(), module.getDomainName(), module.getHostName(), module.getHostIp(), module.getBasePath());
        this.uploadRawComponent(this.platformAppCfgRepository, cfgDirectory, module.getCfgs());
    }
}
