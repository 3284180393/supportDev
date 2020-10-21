package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.exception.NexusException;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.po.AppFilePo;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.po.NexusComponentPo;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.channelsoft.ccod.support.cmdb.utils.HttpRequestTools;
import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
import com.channelsoft.ccod.support.cmdb.vo.DeployFileInfo;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @ClassName: NexusServiceImpl
 * @Author: lanhb
 * @Description: INexusService接口实现类
 * @Date: 2019/11/14 13:50
 * @Version: 1.0
 */
@Service
public class NexusServiceImpl implements INexusService {

//    private String queryAssetByNameFmt = "%s/service/rest/v1/search?repository=%s&name=%s";

    @Value("${nexus.platform-app-cfg-repository}")
    private String platformAppCfgRepository;

    private String appDirectoryFmt = "%s%s%s";

    @Value("${nexus.app-module-repository}")
    private String appRepository;

    @Value("${app-publish-nexus.host-url}")
    private String appPublishNexusUrl;

    @Value("${app-publish-nexus.user}")
    private String appPublishNexusUserName;

    @Value("${app-publish-nexus.password}")
    private String getAppPublishNexusPassword;

    @Value("${windows}")
    private boolean isWindows;

    private final Map<String, Map<String, NexusComponentPo>> repositoryComponentMap = new ConcurrentHashMap<>();

    private final static Logger logger = LoggerFactory.getLogger(NexusServiceImpl.class);

    private String getNexusUploadUrl(String nexusHostUrl, String repository) {
        String url = String.format("%s/service/rest/v1/components?repository=%s", nexusHostUrl, repository);
        return url;
    }

    private String getNexusQueryGroupItemsUrl(String nexusHostUrl, String repository, String group)
    {
        String url = String.format("%s/service/rest/v1/search?repository=%s&group=%s", nexusHostUrl, repository, group);
        return url;
    }

    private String getNexusQueryAssetByNameUrl(String nexusHostUrl, String repository, String name)
    {
        String url = String.format("%s/service/rest/v1/search?repository=%s&name=%s", nexusHostUrl, repository, name);
        return url;
    }

    private String getNexusRepositoryQueryUrl(String nexusHostUrl, String repository) {
        String url = String.format("%s/service/rest/v1/components?repository=%s", nexusHostUrl, repository);
        return url;
    }

    @Override
    public  List<NexusAssetInfo> uploadRawComponent(String nexusHostUrl, String userName, String password, String repository, String directory, DeployFileInfo[] componentFiles) throws InterfaceCallException, NexusException {
        logger.debug(String.format("upload %s to repository=%s and directory=%s",
                String.join(",", Arrays.asList(componentFiles).stream().collect(Collectors.groupingBy(DeployFileInfo::getLocalSavePath)).keySet()), repository, directory));
        String url = getNexusUploadUrl(nexusHostUrl, repository);
        HttpClient httpclient = HttpRequestTools.getCloseableHttpClient();
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Authorization", HttpRequestTools.getBasicAuthPropValue(userName, password));
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
        try
        {
            HttpResponse response = httpclient.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 204)
            {
                String conResult = EntityUtils.toString(response.getEntity(), "utf8");
                logger.error(String.format("upload component to %s/%s FAIL : return code=%d and errMsg=%s ",
                        repository, directory, response.getStatusLine().getStatusCode(), conResult));
                throw new InterfaceCallException(String.format("upload component to %s/%s FAIL : return code=%d and errMsg=%s ",
                        repository, directory, response.getStatusLine().getStatusCode(), conResult));
            }
            Thread.sleep(2000);
        }
        catch (InterfaceCallException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            logger.error(String.format("upload component to exception", url), ex);
            throw new InterfaceCallException(String.format("upload component to exception", url));
        }
        String group = String.format("/%s", directory);
        List<NexusAssetInfo> assetList = this.queryGroupAssets(nexusHostUrl, userName, password, repository, group);
        Map<String, NexusAssetInfo> fileAssetMap = assetList.stream().collect(Collectors.toMap(NexusAssetInfo::getPath, Function.identity()));
        for(DeployFileInfo fileInfo : componentFiles)
        {
            String filePath = String.format("%s/%s", directory, fileInfo.getFileName()).replaceAll("^/", "");
            if(!fileAssetMap.containsKey(filePath))
            {
                logger.error(String.format("%s up to repository=%s and directory=%s FAIL : not find %s at nexus",
                        fileInfo.getFileName(), repository, directory, fileInfo.getFileName()));
                throw new NexusException(String.format("%s up to repository=%s and directory=%s FAIL : not find %s at nexus",
                        fileInfo.getFileName(), repository, directory, fileInfo.getFileName()));
            }
            else
            {
                NexusAssetInfo assetInfo = fileAssetMap.get(filePath);
//                if(!assetInfo.getMd5().equals(fileInfo.getFileMd5()))
//                {
//                    logger.error(String.format("%s up to repository=%s and directory=%s FAIL : srcFileMd5=%s and nexusFileMd5=%s",
//                            fileInfo.getLocalSavePath(), repository, directory, fileInfo.getFileMd5(), assetInfo.getMd5()));
//                    throw new NexusException(String.format("%s up to repository=%s and directory=%s FAIL : srcFileMd5=%s and nexusFileMd5=%s",
//                            fileInfo.getLocalSavePath(), repository, directory, fileInfo.getFileMd5(), assetInfo.getMd5()));
//                }
                fileInfo.setNexusAssetId(assetInfo.getId());
                fileInfo.setNexusDirectory(directory);
                fileInfo.setNexusRepository(repository);
            }
        }
//        assetRelationMap.put(directory, fileAssetMap);
        return assetList;
    }

    /**
     * 从指定仓库查询所有的components
     * @param repository
     * @return 查询结果
     * @throws Exception
     */
    private NexusComponentPo[] queryRepositoryAllComponent(String nexusHostUrl, String userName, String password, String repository) throws InterfaceCallException, NexusException
    {
        String url = getNexusRepositoryQueryUrl(nexusHostUrl, repository);
        logger.info(String.format("begin to query all components of repository=%s", repository));
        String conResult = HttpRequestTools.httpGetRequest(url, userName, password);
        try
        {
            JSONObject jsonObject = JSONObject.parseObject(conResult);
            List<NexusComponentPo> components = JSONArray.parseArray(jsonObject.get("items").toString(), NexusComponentPo.class);
            logger.info(String.format("repository=%s has %d components", repository, components.size()));
            return components.toArray(new NexusComponentPo[0]);
        }
        catch (Exception ex)
        {
            logger.error(String.format("parse %s return msg about component exception", url), ex);
            throw new NexusException(String.format("parse %s return msg about component exception", url));
        }

    }

//    private CloseableHttpClient getBasicHttpClient(String username, String password) {
//        // 创建HttpClientBuilder
//        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
//        // 设置BasicAuth
//        CredentialsProvider provider = new BasicCredentialsProvider();
//        // Create the authentication scope
//        AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT, AuthScope.ANY_REALM);
//        // Create credential pair，在此处填写用户名和密码
//        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(username,password);
//        // Inject the credentials
//        provider.setCredentials(scope, credentials);
//        // Set the default credentials provider
//        httpClientBuilder.setDefaultCredentialsProvider(provider);
//        // HttpClient
//        CloseableHttpClient closeableHttpClient = httpClientBuilder.build();
//        return closeableHttpClient;
//    }

    @Override
    public List<NexusAssetInfo> queryGroupAssets(String nexusHostUrl, String userName, String password, String repository, String group) throws InterfaceCallException, NexusException
    {
        String url = getNexusQueryGroupItemsUrl(nexusHostUrl, repository, group);
        String conResult = HttpRequestTools.httpGetRequest(url, userName, password);
        try
        {
            JSONObject jsonObject = JSONObject.parseObject(conResult);
            List<NexusComponentPo> components = JSONArray.parseArray(jsonObject.get("items").toString(), NexusComponentPo.class);
            List<NexusAssetInfo> assetList = new ArrayList<>();
            logger.info(String.format("repository=%s has %d components", repository, components.size()));
            for(NexusComponentPo componentPo : components)
            {
                assetList.addAll(Arrays.asList(componentPo.getAssets()));
            }
            return assetList;
        }
        catch (Exception ex)
        {
            logger.error(String.format("parse %s return msg exception", url), ex);
            throw new NexusException(String.format("parse %s return msg exception:%s", url, ex.getMessage()));
        }
    }

//    @Override
//    public void downloadComponent(String nexusHostUrl, String userName, String password, NexusAssetInfo[] componentAssets, String savePath) throws InterfaceCallException, NexusException {
//        for(NexusAssetInfo assetInfo : componentAssets)
//        {
//            downloadAsset(nexusHostUrl, userName, password, assetInfo, savePath);
//        }
//    }

    @Override
    public NexusAssetInfo queryAssetByNexusName(String nexusHostUrl, String userName, String password, String repository, String nexusName) throws InterfaceCallException, NexusException {
        String url = getNexusQueryAssetByNameUrl(nexusHostUrl, repository, nexusName);
        String conResult = HttpRequestTools.httpGetRequest(url, userName, password);
        try
        {
            JSONObject jsonObject = JSONObject.parseObject(conResult);
            NexusAssetInfo assetInfo = null;
            List<NexusComponentPo> components = JSONArray.parseArray(jsonObject.get("items").toString(), NexusComponentPo.class);
            if(components.size() > 0 && components.get(0).getAssets() != null && components.get(0).getAssets().length > 0)
            {
                assetInfo = components.get(0).getAssets()[0];
            }
            if(assetInfo != null)
            {
                logger.info(String.format("success find [%s] at nexusHost=%s and repository=%s with name=%s",
                        JSONObject.toJSONString(assetInfo), nexusHostUrl, repository, nexusName));
            }
            else
            {
                logger.warn(String.format("can not find file at nexusHost=%s and repository=%s with name=%s",
                        nexusHostUrl, repository, nexusName));
            }
            return assetInfo;
        }
        catch (Exception ex)
        {
            logger.error(String.format("parse %s return msg exception", url), ex);
            throw new NexusException(String.format("parse %s return msg exception", url));
        }

    }


    @Override
    public String downloadFile(String userName, String password, String downloadUrl, String saveDir, String saveFileName) throws IOException, InterfaceCallException{
        File dir = new File(saveDir);
        if(!dir.exists())
        {
            dir.mkdirs();
        }
        String savePath = String.format("%s/%s", saveDir, saveFileName).replaceAll("\\\\", "/");
        logger.debug(String.format("begin to download file from %s and save to %s", downloadUrl, savePath));
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        HttpURLConnection uc = null;
        try
        {
            URL url = new URL(downloadUrl);
            uc = (HttpURLConnection) url.openConnection();
            uc.setRequestProperty("Authorization", HttpRequestTools.getBasicAuthPropValue(userName, password));
//            uc.connect();
            uc.setDoInput(true);// 设置是否要从 URL 连接读取数据,默认为true
            uc.connect();
            String message = uc.getHeaderField(0);
            if (message != null && !"".equals(message.trim())
                    && message.startsWith("HTTP/1.1 404"))
            {
                logger.error(String.format("%s not exist", downloadUrl));
                throw new InterfaceCallException(String.format("%s not exist", downloadUrl));
            }
            long fileSize = uc.getContentLength();
            logger.debug(String.format("file length of %s is %d", downloadUrl, fileSize));
            File file = new File(savePath);// 创建新文件
            if (file.exists())
            {
                file.createNewFile();
            }
            // 读取文件
            bis = new BufferedInputStream(uc.getInputStream());
            bos = new BufferedOutputStream(new FileOutputStream(file));
            int len = 2048;
            byte[] b = new byte[len];
            while ((len = bis.read(b)) != -1)
            {
                bos.write(b, 0, len);
            }
            logger.info(String.format("success download %s and save to %s", downloadUrl, savePath));
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
        return savePath;
    }

    @Override
    public void deleteAsset(String nexusHostUrl, String userName, String password, String assetId) throws InterfaceCallException {
        CloseableHttpClient httpClient = HttpRequestTools.getCloseableHttpClient();
        String url = String.format("%s/service/rest/v1/assets/%s", nexusHostUrl, assetId);
        HttpDelete httpdelete = new HttpDelete(url);
        httpdelete.addHeader("Authorization", HttpRequestTools.getBasicAuthPropValue(userName, password));
        int retCode;
        try
        {
            CloseableHttpResponse httpResponse = httpClient.execute(httpdelete);
            retCode = httpResponse.getStatusLine().getStatusCode();

        }
        catch (Exception e)
        {
            logger.error(String.format("delete %s from %s exception", assetId, nexusHostUrl), e);
            throw new InterfaceCallException(e.getMessage());
        }
        if(retCode != 204)
        {
            logger.error(String.format("delete %s from %s FAIL, errorCode=%d", assetId, nexusHostUrl, retCode));
            throw new InterfaceCallException(String.format("delete %s from %s FAIL, errorCode=%d", assetId, nexusHostUrl, retCode));
        }
        logger.info(String.format("delete %s from %s success", assetId, nexusHostUrl));
    }

    @Override
    public void clearComponent(String nexusHostUrl, String userName, String password, String repository, String directory) throws InterfaceCallException, NexusException {
        logger.debug(String.format("clear component %s/%s/%s", nexusHostUrl, repository, directory));
        String group = String.format("/%s", directory);
        List<NexusAssetInfo> assetInfos = this.queryGroupAssets(nexusHostUrl, userName, password, repository, group);
        for(NexusAssetInfo assetInfo : assetInfos)
            deleteAsset(nexusHostUrl, userName, password, assetInfo.getId());
        logger.debug(String.format("component %s/%s/%s cleared", nexusHostUrl, repository, directory));
    }

    private String getTempSaveDir(String directory) {
        String saveDir = String.format("%s/downloads/%s", System.getProperty("user.dir"), directory);
        return saveDir;
    }

    @Override
    public List<NexusAssetInfo> downloadAndUploadFiles(String srcNexusHostUrl, String srcNexusUser, String srcPwd, List<NexusAssetInfo> srcFileList, String dstNexusHostUrl, String dstNexusUser, String dstNexusPwd, String dstRepository, String dstDirectory, boolean isClearTargetDirectory) throws ParamException, InterfaceCallException, NexusException, IOException {
        List<DeployFileInfo> fileList = new ArrayList<>();
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String tmpSaveDir = getTempSaveDir(DigestUtils.md5DigestAsHex(String.format("%s;%s", dstDirectory, sf.format(now)).getBytes()));
        for(NexusAssetInfo filePo : srcFileList)
        {
            String downloadUrl = filePo.getDownloadUrl();
            logger.debug(String.format("download cfg from %s", downloadUrl));
            String savePth = downloadFile(srcNexusUser, srcPwd, downloadUrl, tmpSaveDir, filePo.getNexusAssetFileName());
            FileInputStream is = new FileInputStream(savePth);
            String md5 = DigestUtils.md5DigestAsHex(is);
            is.close();
            if(!md5.equals(filePo.getMd5())) {
                throw new ParamException(String.format("file %s verify md5 FAIL : report=%s and download=%s",
                        filePo.getNexusAssetFileName(), filePo.getMd5(), md5));
            }
            fileList.add(new DeployFileInfo(filePo, savePth));
        }
        if(isClearTargetDirectory)
            this.clearComponent(dstNexusHostUrl, dstNexusUser, dstNexusPwd, dstRepository, dstDirectory);
        return uploadRawComponent(dstNexusHostUrl, dstNexusUser, dstNexusPwd, dstRepository, dstDirectory, fileList.toArray(new DeployFileInfo[0]));
    }

    @Override
    public List<AppFileNexusInfo> downloadAndUploadAppFiles(String srcNexusHostUrl, String srcNexusUser, String srcPwd, List<AppFileNexusInfo> srcFileList, String dstNexusHostUrl, String dstNexusUser, String dstNexusPwd, String dstRepository, String dstDirectory, boolean isClearTargetDirectory) throws ParamException, InterfaceCallException, NexusException, IOException {
        List<NexusAssetInfo> assets = downloadAndUploadFiles(srcNexusHostUrl, srcNexusUser, srcPwd,
                srcFileList.stream().map(f->f.getNexusAssetInfo(srcNexusHostUrl)).collect(Collectors.toList()), dstNexusHostUrl,
                dstNexusUser, dstNexusPwd, dstRepository, dstDirectory, isClearTargetDirectory);
        Map<String, String> fileDeployPathMap = srcFileList.stream()
                .collect(Collectors.toMap(a->a.getFileName(), v->v.getDeployPath()));
        return assets.stream().map(a->new AppFileNexusInfo(a, fileDeployPathMap.get(a.getNexusAssetFileName())))
                .collect(Collectors.toList());
    }
}
