package com.channelsoft.ccod.support.cmdb.k8s.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.config.ImageCfg;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.exception.NexusException;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.k8s.service.IImageService;
import com.channelsoft.ccod.support.cmdb.utils.HttpRequestTools;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashSet;
import java.util.Set;

/**
 * @ClassName: ImageServiceImpl
 * @Author: lanhb
 * @Description: IImageService接口的实现类
 * @Date: 2020/6/13 12:22
 * @Version: 1.0
 */
public class ImageServiceImpl implements IImageService {

    private final static Logger logger = LoggerFactory.getLogger(ImageServiceImpl.class);

    @Autowired
    ImageCfg imageCfg;

    @Override
    public boolean isImageExist(String imageTag, String repository) throws InterfaceCallException, NexusException, ParamException {
        logger.debug(String.format("begin to check exist of %s at %s", imageTag, repository));
        boolean imageExist = false;
        String[] arr = imageTag.split("\\:");
        if(arr.length != 2)
            throw new ParamException(String.format("%s is illegal image tag", imageTag));
        String appName = arr[0];
        String version = arr[1];
        String url = String.format("http://%s/v2/%s/%s/tags/list", this.imageCfg.getNexusUrl(), repository, appName);
        int statusCode;
        String result = null;
        try
        {
            HttpGet httpGet = new HttpGet(url);
            httpGet.addHeader("Authorization", HttpRequestTools.getBasicAuthPropValue(imageCfg.getNexusUser(), imageCfg.getNexusPwd()));
            CloseableHttpClient httpClient = HttpRequestTools.getCloseableHttpClient();
            HttpResponse response = httpClient.execute(httpGet);
            statusCode = response.getStatusLine().getStatusCode();
            if(statusCode != 404)
                result = EntityUtils.toString(response.getEntity(), "utf8");
        }
        catch (Exception ex)
        {
            throw new InterfaceCallException(String.format("get %s exception", ex));
        }
        if(statusCode != 404 && statusCode != 200)
            throw new InterfaceCallException(String.format("errorCode=%d, %s", statusCode, result));
        else if(statusCode == 200)
        {
            try
            {
                String tags = JSONObject.parseObject(result).getString("tags");
                Set<String> set = new HashSet<>(JSONArray.parseArray(tags, String.class));
                if(set.contains(version.replaceAll("\\:", "-")))
                    imageExist = true;
            }
            catch (Exception ex)
            {
                throw new NexusException(ex.getMessage());
            }
        }
        logger.info(String.format("%s image exist : %b", imageTag, imageExist));
        return imageExist;
    }
}
