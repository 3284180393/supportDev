package com.channelsoft.ccod.support.cmdb.utils;

import com.channelsoft.ccod.support.cmdb.po.AppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.AppInstallPackagePo;
import com.channelsoft.ccod.support.cmdb.po.AppPo;
import com.channelsoft.ccod.support.cmdb.po.PlatformAppPo;
import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleFileNexusInfo;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @ClassName: CMDBTools
 * @Author: lanhb
 * @Description: cmdb工具类，为应用一些常用的操作提供统一的接口，以保证唯一
 * @Date: 2019/12/27 17:26
 * @Version: 1.0
 */
public class CMDBTools {

    public static String getAppDirectory(AppPo appPo) {
        String directory = String.format("%s/%s/%s", appPo.getAppName(), appPo.getAppAlias(), appPo.getVersion());
        return directory;
    }

    public static String getAppModuleDirectory(AppModuleVo appModuleVo) {
        String directory = String.format("%s/%s/%s", appModuleVo.getAppName(), appModuleVo.getAppAlias(), appModuleVo.getVersion());
        return directory;
    }

    public static String getTempSaveDir(String directory) {
        String saveDir = String.format("%s/downloads/%s", System.getProperty("user.dir"), directory);
        return saveDir;
    }

    public static String getDirectoryFromAppModuleFileNexusInfo(AppModuleFileNexusInfo nexusInfo) {
        String[] arr = nexusInfo.getNexusName().split("/");
        String fileName = arr[arr.length - 1];
        String directory = "/" + nexusInfo.getNexusName().replaceAll("/" + fileName + "$", "");
        return directory;
    }

    public static String getFileNameFromDownloadUrl(String downloadUrl) {
        String url = downloadUrl;
        if (url.lastIndexOf("/") == url.length() - 1) {
            url = url.substring(0, downloadUrl.length() - 1);
        }
        String[] arr = url.split("/");
        return arr[arr.length - 1];
    }

    public static String getPlatformAppDirectory(AppModuleVo appPo, PlatformAppPo platformAppPo) {
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String directory = String.format("%s/%s/%s/%s/%s/%s/%s", platformAppPo.getPlatformId(),
                platformAppPo.getDomainId(), platformAppPo.getHostIp(), appPo.getAppName(), appPo.getVersion(),
                platformAppPo.getAppAlias(), sf.format(now));
        return directory;
    }

    public static String getNexusRepositoryQueryUrl(String nexusHostUrl, String repository) {
        String url = String.format("%s/service/rest/v1/components?repository=%s", nexusHostUrl, repository);
        return url;
    }

    public static String getNexusUploadRawUrl(String nexusHostUrl, String repository) {
        String url = String.format("%s/service/rest/v1/components?repository=%s", nexusHostUrl, repository);
        return url;
    }

    public static String getNexusQueryGroupItemsUrl(String nexusHostUrl, String repository, String group)
    {
        String url = String.format("%s/service/rest/v1/search?repository=%s&group=%s", nexusHostUrl, repository, group);
        return url;
    }

    public static String getNexusQueryAssetByNameUrl(String nexusHostUrl, String repository, String name)
    {
        String url = String.format("%s/service/rest/v1/search?repository=%s&name=%s", nexusHostUrl, repository, name);
        return url;
    }

    public static String getAppFileDownloadUrl(String nexusHostUrl, AppFileNexusInfo nexusInfo)
    {
        String downloadUrl = String.format("%s/repository/%s%s", nexusHostUrl, nexusInfo.getNexusRepository(), nexusInfo.getNexusPath());
        return downloadUrl;
    }

    public static String getInstallPackageDownloadUrl(String nexusHostUrl, AppInstallPackagePo installPackage)
    {
        String downloadUrl = String.format("%s/repository/%s/%s/%s", nexusHostUrl, installPackage.getNexusRepository(), installPackage.getNexusDirectory(), installPackage.getFileName());
        return downloadUrl;
    }

    public static String getAppCfgDownloadUrl(String nexusHostUrl, AppCfgFilePo cfgFilePo)
    {
        String downloadUrl = String.format("%s/repository/%s/%s/%s", nexusHostUrl, cfgFilePo.getNexusRepository(), cfgFilePo.getNexusDirectory(), cfgFilePo.getFileName());
        return downloadUrl;
    }
}
