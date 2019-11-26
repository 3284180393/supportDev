package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.po.NexusComponentPo;
import com.channelsoft.ccod.support.cmdb.po.ServerPo;
import com.channelsoft.ccod.support.cmdb.po.ServerUserPo;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.channelsoft.ccod.support.cmdb.vo.DeployFileInfo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppModuleVo;
import org.apache.commons.lang3.builder.ToStringExclude;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
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

    @Value("${nexus.host_url}")
    private String nexusHostUrl;

    private String queryRepositoryUrlFmt = "%s/service/rest/v1/components?repository=%s";

    private String queryComponentUrlFmt = "%s/service/rest/v1/components/%s";

    private String queryAssetUrlFmt = "%s/service/rest/v1/assets/%s";

    private String uploadRawUrlFmt = "%s/service/rest/v1/components?repository=%s";

    private String downloadUrlFmt = "%s/%s/%s";

    private String queryGroupItemsUrlFmt = "%s/service/rest/v1/search?repository=%s&group=%s";

    @Value("${nexus.platform_app_cfg_repository}")
    private String platformAppCfgRepository;

    private String appDirectoryFmt = "%s%s%s";

    @Value("${nexus.app_module_repository}")
    private String appRepository;

    @Value("${windows}")
    private boolean isWindows;

    private final Map<String, Map<String, NexusComponentPo>> repositoryComponentMap = new ConcurrentHashMap<>();

    private final static Logger logger = LoggerFactory.getLogger(NexusServiceImpl.class);

    @Override
    public boolean uploadRawFile(String repository, String sourceFilePath, String group, String fileName) throws Exception {
        String url = String.format(this.uploadRawUrlFmt, this.nexusHostUrl, repository);
        logger.debug(String.format("begin to upload %s to %s/%s/%s, uploadUrl=%s",
                sourceFilePath, repository, group, fileName, url));
        HttpClient httpclient = getBasicHttpClient(this.userName, this.password);
        HttpPost httppost = new HttpPost(url);
        httppost.addHeader("Authorization", getBasicAuthPropValue());
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
        String url = String.format(this.queryComponentUrlFmt, this.nexusHostUrl, componentId);
        logger.debug(String.format("begin to query id=%s component info, queryUrl=%s", componentId, url));
        HttpClient httpclient = getBasicHttpClient(this.userName, this.password);
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Authorization", getBasicAuthPropValue());
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
        String url = String.format(this.queryAssetUrlFmt, this.nexusHostUrl, assetId);
        logger.debug(String.format("begin to query id=%s asset info, queryUrl=%s", assetId, url));
        HttpClient httpclient = getBasicHttpClient(this.userName, this.password);
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Authorization", getBasicAuthPropValue());
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
        String url = String.format(this.queryRepositoryUrlFmt, this.nexusHostUrl, repository);
        logger.debug(String.format("begin to query all components from repository=%s, queryUrl=%s",
                repository, url));
        HttpClient httpclient = getBasicHttpClient(this.userName, this.password);
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Authorization", getBasicAuthPropValue());
        HttpResponse response = httpclient.execute(httpGet);
        if (response.getStatusLine().getStatusCode() == 404)
        {
            logger.error(String.format("repository=%s not exist", repository));
            throw new Exception(String.format("repository=%s not exist", repository));
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
    public Map<String, Map<String, NexusAssetInfo>> uploadRawComponent(String repository, String directory, DeployFileInfo[] componentFiles) throws Exception {
        String url = String.format(this.uploadRawUrlFmt, this.nexusHostUrl, repository);
        HttpClient httpclient = getBasicHttpClient(this.userName, this.password);
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Authorization", getBasicAuthPropValue());
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setCharset(java.nio.charset.Charset.forName("UTF-8"));
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addTextBody("raw.directory", directory);
        for(int i = 1; i <= componentFiles.length; i++)
        {
            builder.addTextBody("raw.asset" + i + ".filename", componentFiles[i-1].getFileName());
            builder.addBinaryBody("raw.asset" + i, new File(componentFiles[i-1].getLocalSavePath()));
        }
        HttpEntity entity = builder.build();
        httpPost.setEntity(entity);
        HttpResponse response = httpclient.execute(httpPost);
        if (response.getStatusLine().getStatusCode() != 204)
        {
            String conResult = EntityUtils.toString(response.getEntity(), "utf8");
            logger.error(String.format("upload component to %s/%s FAIL : return code=%d and errMsg=%s ",
                    repository, directory, response.getStatusLine().getStatusCode(), conResult));
            throw new Exception(String.format("upload component to %s/%s FAIL : return code=%d and errMsg=%s ",
                    repository, directory, response.getStatusLine().getStatusCode(), conResult));
        }
        Thread.sleep(10000);
        Map<String, NexusAssetInfo> fileAssetMap = this.queryGroupAssetMap(repository, directory);
        logger.error(String.format("map=%s", JSONObject.toJSONString(fileAssetMap)));
        for(DeployFileInfo fileInfo : componentFiles)
        {
            if(!fileAssetMap.containsKey(fileInfo.getFileName()))
            {
                logger.error(String.format("%s up to repository=%s and directory=%s FAIL : not find %s at nexus",
                        fileInfo.getFileName(), repository, directory, fileInfo.getFileName()));
                throw new Exception(String.format("%s up to repository=%s and directory=%s FAIL : not find %s at nexus",
                        fileInfo.getFileName(), repository, directory, fileInfo.getFileName()));
            }
            else
            {
                NexusAssetInfo assetInfo = fileAssetMap.get(fileInfo.getFileName());
                fileInfo.setNexusAssetId(assetInfo.getId());
                fileInfo.setNexusDirectory(directory);
                fileInfo.setNexusRepository(repository);
            }
        }
//        assetRelationMap.put(directory, fileAssetMap);
        return new HashMap<>();
    }

    /**
     * 从指定仓库查询所有的components
     * @param repository
     * @return 查询结果
     * @throws Exception
     */
    private NexusComponentPo[] queryRepositoryAllComponent(String repository) throws Exception
    {
        String url = String.format(this.queryRepositoryUrlFmt, this.nexusHostUrl, repository);
        logger.debug(String.format("begin to query all components of repository=%s", repository));
        HttpClient httpclient = getBasicHttpClient(this.userName, this.password);
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Authorization", getBasicAuthPropValue());
        HttpResponse response = httpclient.execute(httpGet);
        if (response.getStatusLine().getStatusCode() == 404)
        {
            logger.error(String.format("repository=%s not exist", repository));
            throw new Exception(String.format("repository=%s not exist", repository));
        }
        else if (response.getStatusLine().getStatusCode() != 200)
        {
            logger.error(String.format("query all components from repository=%s FAIL : server return %d code",
                    repository, response.getStatusLine().getStatusCode()));
            throw new Exception(String.format("query all components from repository=%s FAIL : server return %d code",
                    repository, response.getStatusLine().getStatusCode()));
        }
        String conResult = EntityUtils.toString(response.getEntity(), "utf8");
        JSONObject jsonObject = JSONObject.parseObject(conResult);
        List<NexusComponentPo> components = JSONArray.parseArray(jsonObject.get("items").toString(), NexusComponentPo.class);
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

//    @Override
//    public void addPlatformAppModule(PlatformAppModuleVo module) throws Exception {
//        String modulePath = String.format(this.appDirectoryFmt, module.getModuleName(), module.getModuleAliasName(), module.getVersion());
//        if(this.repositoryComponentMap.get(this.appRepository).containsKey(modulePath))
//        {
//            NexusComponentPo savedComp = this.repositoryComponentMap.get(this.appRepository).get(modulePath);
//            if(savedComp.getAssets().length != module.getCfgs().length + 1)
//            {
//                logger.error(String.format("platform=%s and domain=%s and hostname=%s and hostip=%s and basepath=%s %s file is %d but saved same version file is %d",
//                        module.getPlatformId(), module.getDomainName(), module.getHostName(), module.getHostIp(),
//                        module.getBasePath(), savedComp, module.getCfgs().length + 1), savedComp.getAssets().length);
//                throw new Exception(String.format("collect %s file count not equal with same app", modulePath));
//            }
//            Map<String, NexusAssetInfo> assetMap = new HashMap<>();
//            for(NexusAssetInfo info : savedComp.getAssets())
//            {
//                assetMap.put(info.getPath(), info);
//            }
//            if(!assetMap.containsKey("/" + modulePath + "/" + module.getInstallPackage().getFileName()))
//            {
//                logger.error(String.format("platform=%s and domain=%s and hostname=%s and hostip=%s and basepath=%s %s install package name=%s not in saved same version module",
//                        module.getPlatformId(), module.getDomainName(), module.getHostName(), module.getHostIp(),
//                        module.getBasePath(), savedComp, module.getInstallPackage().getFileName()));
//                throw new Exception(String.format("collect %s install package name not equal with the same app", modulePath));
//            }
//            else
//            {
//                NexusAssetInfo savedAsset = assetMap.get("/" + modulePath + "/" + module.getInstallPackage().getFileName());
//                if(!savedAsset.getMd5().equals(module.getInstallPackage().getFileMd5()))
//                {
//                    logger.error(String.format("platform=%s and domain=%s and hostname=%s and hostip=%s and basepath=%s %s install package name=%s's md5 not equal with saved same version module %s",
//                            module.getPlatformId(), module.getDomainName(), module.getHostName(), module.getHostIp(),
//                            module.getBasePath(), savedComp, module.getInstallPackage().getFileName(), module.getInstallPackage().getFileMd5(), savedAsset.getMd5()));
//                    throw new Exception(String.format("collect %s install package md5 not equal with the same app", modulePath));
//                }
//            }
//
//        }
//        else
//        {
//            List<DeployFileInfo> uploadFiles = new ArrayList<>();
//            uploadFiles.add(module.getInstallPackage());
//            for(DeployFileInfo cfg : module.getCfgs())
//            {
//                uploadFiles.add(cfg);
//            }
//            NexusComponentPo[] componentPos = this.uploadRawComponent(this.appRepository, modulePath, uploadFiles.toArray(new DeployFileInfo[0]));
//            Map<String, NexusComponentPo> componentMap = new HashMap<>();
//            for(NexusComponentPo componentPo : componentPos)
//            {
//                componentMap.put(componentPo.getGroup(), componentPo);
//            }
//            this.repositoryComponentMap.put(this.appRepository, componentMap);
//        }
//        String cfgDirectory = String.format(this.platformAppCfgDirectoryFmt, module.getModuleName(), module.getModuleAliasName(),
//                module.getPlatformId(), module.getDomainName(), module.getHostName(), module.getHostIp(), module.getBasePath());
//        this.uploadRawComponent(this.platformAppCfgRepository, cfgDirectory, module.getCfgs());
//    }

    @Override
    public void uploadPlatformAppModules(String appRepository, String cfgRepository, PlatformAppModuleVo[] modules) throws Exception {
        NexusComponentPo[] storeComps = this.queryRepositoryAllComponent(appRepository);
        Map<String, Map<String, NexusAssetInfo>> storeAssetMap = new HashMap<>();
        for(NexusComponentPo component : storeComps)
        {
            for(NexusAssetInfo assetInfo : component.getAssets())
            {
                String[] arr = assetInfo.getPath().split("/");
                String fileName = arr[arr.length - 1];
                String directory = "/" + assetInfo.getPath().replace(fileName, "");
                if(!storeAssetMap.containsKey(directory))
                {
                    storeAssetMap.put(directory, new HashMap<>());
                }
                storeAssetMap.get(directory).put(fileName, assetInfo);
            }
        }
    }


    private Map<String, NexusAssetInfo> queryGroupAssetMap(String repository, String group) throws Exception
    {
        Map<String, NexusAssetInfo> map = new HashMap<>();
        String url = String.format(this.queryGroupItemsUrlFmt, this.nexusHostUrl, repository, group);
        HttpClient httpclient = getBasicHttpClient(this.userName, this.password);
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("Authorization", getBasicAuthPropValue());
        HttpResponse response = httpclient.execute(httpGet);
        if (response.getStatusLine().getStatusCode() == 404)
        {
            logger.error(String.format("repository=%s not exist", repository));
            throw new Exception(String.format("repository=%s not exist", repository));
        }
        else if (response.getStatusLine().getStatusCode() != 200)
        {
            logger.error(String.format("query all components from repository=%s FAIL : server return %d code",
                    repository, response.getStatusLine().getStatusCode()));
            throw new Exception(String.format("query all components from repository=%s FAIL : server return %d code",
                    repository, response.getStatusLine().getStatusCode()));
        }
        String conResult = EntityUtils.toString(response.getEntity(), "utf8");
        JSONObject jsonObject = JSONObject.parseObject(conResult);
        List<NexusComponentPo> components = JSONArray.parseArray(jsonObject.get("items").toString(), NexusComponentPo.class);
        logger.info(String.format("repository=%s has %d components", repository, components.size()));
        for(NexusComponentPo componentPo : components)
        {
            for(NexusAssetInfo assetInfo : componentPo.getAssets())
            {
                String[] arr = assetInfo.getPath().split("/");
                String fileName = arr[arr.length - 1];
                map.put(fileName, assetInfo);
            }
        }
        return map;
    }

    @Override
    public Map<String, Map<String, NexusAssetInfo>> queryRepositoryAssetRelationMap(String repository) throws Exception
    {
        NexusComponentPo[] components = this.queryRepositoryAllComponent(repository);
        Map<String, Map<String, NexusAssetInfo>> storeAssetMap = new HashMap<>();
        for(NexusComponentPo component : components)
        {
            for(NexusAssetInfo assetInfo : component.getAssets())
            {
                String[] arr = assetInfo.getPath().split("/");
                String fileName = arr[arr.length - 1];
                String directory = "/" + assetInfo.getPath().replaceAll("/" + fileName + "$", "");
                if(!storeAssetMap.containsKey(directory))
                {
                    storeAssetMap.put(directory, new HashMap<>());
                }
                storeAssetMap.get(directory).put(fileName, assetInfo);
            }
        }
        return storeAssetMap;
    }


    @Override
    public void downloadComponent(NexusAssetInfo[] componentAssets, String savePath) throws Exception {
        for(NexusAssetInfo assetInfo : componentAssets)
        {
            downloadAsset(assetInfo, savePath);
        }
    }

    private void downloadAsset(NexusAssetInfo assetInfo, String savePath) throws Exception
    {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        HttpURLConnection uc = null;
        try
        {
            String downloadUrl = String.format(this.downloadUrlFmt, this.nexusHostUrl, assetInfo.getRepository(), assetInfo.getPath()).replace("//", "/");
            String[] arr = downloadUrl.split("/");
            String fileName = arr[arr.length - 1];
            String savedFullPath = savePath;
            if(isWindows)
            {
                savedFullPath = "/" + savedFullPath.replace("\\", "/");
            }
            savedFullPath += savedFullPath + "/" + fileName;
            URL url = new URL(downloadUrl);
            uc = (HttpURLConnection) url.openConnection();
            uc.setRequestProperty("Authorization", getBasicAuthPropValue());
//            uc.connect();
            uc.setDoInput(true);// 设置是否要从 URL 连接读取数据,默认为true
            uc.connect();
            String message = uc.getHeaderField(0);
            if (message != null && !"".equals(message.trim())
                    && message.startsWith("HTTP/1.1 404"))
            {
                logger.error("查询到的录音" + downloadUrl + "不存在");

                throw new Exception("录音文件不存在");
            }
            File file = new File(savedFullPath);// 创建新文件
            if (file != null && !file.exists())
            {
                file.createNewFile();
            }
            long fileSize = uc.getContentLength();
            logger.info(downloadUrl + "录音文件长度:" + uc.getContentLength());// 打印文件长度
            // 读取文件
            bis = new BufferedInputStream(uc.getInputStream());
            bos = new BufferedOutputStream(new FileOutputStream(file));
            int len = 2048;
            byte[] b = new byte[len];
            while ((len = bis.read(b)) != -1)
            {
                bos.write(b, 0, len);
            }
            logger.info("下载保存成功");
            bos.flush();
        }
        finally {
            if(uc != null)
            {
                uc.disconnect();
            }
            if(bis != null)
            {
                bis.close();
            }
            if(bos != null)
            {
                bos.close();
            }
        }

    }

    private String getBasicAuthPropValue()
    {
        String input = this.userName + ":" + this.password;
        return "Basic " + (new sun.misc.BASE64Encoder().encode(input.getBytes()));
    }

    @Test
    public void nexusHttpTest()
    {
        try
        {
            String url = "http://10.130.41.216:8081/service/rest/v1/components?repository=ccod_modules";
            CloseableHttpClient httpclient = getBasicHttpClient("admin", "123456");
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Authorization", "Basic YWRtaW46MTIzNDU2");
//            httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS ***");
            HttpResponse response = httpclient.execute(httpGet);
            System.out.println(response.getStatusLine().getStatusCode());
            String conResult = EntityUtils.toString(response.getEntity(), "utf8");
            System.out.println(conResult);
            JSONObject jsonObject = JSONObject.parseObject(conResult);

            List<NexusComponentPo> components = JSONArray.parseArray(jsonObject.get("items").toString(), NexusComponentPo.class);
            System.out.println(JSONObject.toJSONString(components));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Test
    public void uploadTest()
    {
        try {
            String url = "http://10.130.41.216:8081/service/rest/v1/components?repository=CCOD";
            CloseableHttpClient httpclient = getBasicHttpClient("admin", "123456");
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Authorization", "Basic YWRtaW46MTIzNDU2");
//            httpPost.addHeader("X-Content-Type-Options", "nosniff");
//            httpPost.addHeader("Content-Type", "application/json; charset=utf-8");
//            httpPost.addHeader("content-type", "multipart/form-data; boundary=----------------------------e3407fbc6f02");
            String directory = "/CCOD/MONITOR_MODULE/ivr/1.0.0.0/";
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("raw.directory", directory));
            nvps.add(new BasicNameValuePair("raw.asset1", "@D:\\temp\\ivr.jar"));
            nvps.add(new BasicNameValuePair("raw.asset1.filename", "cfg1.ini"));
//            nvps.add(new BasicNameValuePair("raw.asset1", "D:\\My Work\\Java\\idea\\supportDev\\downloads\\9ae8046f6b55f4114d8337bed35660b7\\config.ucx"));
//            nvps.add(new BasicNameValuePair("raw.asset1.filename", "cfg1.ini"));
//            nvps.add(new BasicNameValuePair("raw.asset2", "D:\\My Work\\Java\\idea\\supportDev\\downloads\\9ae8046f6b55f4114d8337bed35660b7\\config.ucx"));
//            nvps.add(new BasicNameValuePair("raw.asset2.filename", "cfg2.ini"));
//            nvps.add(new BasicNameValuePair("raw.asset3", "D:\\My Work\\Java\\idea\\supportDev\\downloads\\9ae8046f6b55f4114d8337bed35660b7\\config.ucx"));
//            nvps.add(new BasicNameValuePair("raw.asset3.filename", "ucx.zip"));
//            UrlEncodedFormEntity refe = new UrlEncodedFormEntity(nvps, Consts.UTF_8);
//            System.out.println(JSONObject.toJSONString(refe));
//            JSONObject postData = new JSONObject();
//            postData.put("raw.directory", directory);
//            postData.put("raw.asset1", "@D:\\temp\\ivr.jar");
//            postData.put("raw.asset1.filename", "cfg1.ini");
//            httpPost.setEntity(refe);
            MultipartEntity httpEntity = new MultipartEntity();
            httpEntity.addPart("raw.directory", new StringBody(directory, Charset.forName("UTF-8")));
            httpEntity.addPart("raw.asset1", new FileBody(new File("D:\\temp\\ivr.jar")));
            httpEntity.addPart("raw.asset1.filename", new StringBody("cfg1.ini", Charset.forName("UTF-8")));
//            System.out.println(JSONObject.toJSONString(new StringEntity(postData.toString())));
//            StringEntity paramEntity = new StringEntity(postData.toString(), "UTF-8");
//            paramEntity.setContentType("application/json; charset=utf-8");;
//            httpPost.setEntity(paramEntity);
            httpPost.setEntity(httpEntity);
//            System.out.println(JSONObject.toJSONString(httpPost.getEntity()));
            HttpResponse response = httpclient.execute(httpPost);
//            String conResult = EntityUtils.toString(response.getEntity(), "utf8");
            System.out.println(response.getStatusLine().getStatusCode());
//            System.out.println(conResult);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Test
    public void uploadTest45()
    {
        try
        {
            String directory = "/CCOD/MONITOR_MODULE/ivr/3.0.0.0/";
            String url = "http://10.130.41.216:8081/service/rest/v1/components?repository=CCOD";
            CloseableHttpClient httpclient = getBasicHttpClient("admin", "123456");
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Authorization", "Basic YWRtaW46MTIzNDU2");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(java.nio.charset.Charset.forName("UTF-8"));
            builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
            builder.addTextBody("raw.directory", directory);
            builder.addTextBody("raw.asset1.filename", "test1.ini");
            builder.addBinaryBody("raw.asset1", new File("D:\\temp\\ivr.jar"));
            builder.addTextBody("raw.asset2.filename", "ivr.zip");
            builder.addBinaryBody("raw.asset2", new File("D:\\temp\\temp.zip"));
            builder.addTextBody("raw.asset3.filename", "test2.xml");
            builder.addBinaryBody("raw.asset3", new File("D:\\temp\\config.xml"));
            HttpEntity entity = builder.build();
            httpPost.setEntity(entity);
            HttpResponse response = httpclient.execute(httpPost);
            System.out.println(response.getStatusLine().getStatusCode());

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Test
    public void downloadTest()
    {
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        HttpURLConnection uc = null;
        String downloadUrl = "http://10.130.41.216:8081/repository/CCOD/CCOD/MONITOR_MODULE/ivr/3.0.0.0/ivr1.zip";
        String savedFullPath = "d:\\temp\\helloTest.zip";
        try
        {
            URL url = new URL(downloadUrl);
            uc = (HttpURLConnection) url.openConnection();
            String username = "admin";
            String password = "123456";
            String input = username + ":" + password;
            String encoding = new sun.misc.BASE64Encoder().encode(input.getBytes());
            uc.setRequestProperty("Authorization", "Basic " + encoding);
//            uc.connect();
            uc.setDoInput(true);// 设置是否要从 URL 连接读取数据,默认为true
            uc.connect();
            String message = uc.getHeaderField(0);
            if (message != null && !"".equals(message.trim())
                    && message.startsWith("HTTP/1.1 404"))
            {
                logger.error("查询到的录音" + downloadUrl + "不存在");

                throw new Exception("录音文件不存在");
            }
            File file = new File(savedFullPath);// 创建新文件
            if (file != null && !file.exists())
            {
                file.createNewFile();
            }
            long fileSize = uc.getContentLength();
            logger.info(downloadUrl + "录音文件长度:" + uc.getContentLength());// 打印文件长度
            // 读取文件
            bis = new BufferedInputStream(uc.getInputStream());
            bos = new BufferedOutputStream(new FileOutputStream(file));
            int len = 2048;
            byte[] b = new byte[len];
            while ((len = bis.read(b)) != -1)
            {
                bos.write(b, 0, len);
            }
            logger.info("下载保存成功");
            bos.flush();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Test
    public void queryMapTest()
    {
        try
        {
            String repository = "ccod_modules";
            this.nexusHostUrl = "http://10.130.41.216:8081";
            this.userName = "admin";
            this.password = "123456";
            this.queryRepositoryAssetRelationMap(repository);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
