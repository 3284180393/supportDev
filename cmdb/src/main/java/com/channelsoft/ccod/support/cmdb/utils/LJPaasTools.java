package com.channelsoft.ccod.support.cmdb.utils;

/**
 * @ClassName: LJPaasTools
 * @Author: lanhb
 * @Description: 用来定义同蓝鲸相关参数的工具类
 * @Date: 2019/12/27 18:25
 * @Version: 1.0
 */
public class LJPaasTools {

    public static String getQueryBizUrl(String paasHostUrl)
    {
        String url = String.format("%s/api/c/compapi/v2/cc/search_business/", paasHostUrl);
        return url;
    }

    public static String getQueryBizSetUrl(String paasHostUrl)
    {
        String url = String.format("%s/api/c/compapi/v2/cc/search_set/", paasHostUrl);
        return url;
    }

    public static String getAddHostUrl(String paasHostUrl)
    {
        String url = String.format("%s/api/c/compapi/v2/cc/add_host_to_resource/", paasHostUrl);
        return url;
    }

    public static String getQueryHostUrl(String paasHostUrl)
    {
        String url = String.format("%s/api/c/compapi/v2/cc/search_host/", paasHostUrl);
        return url;
    }

    public static String getCreateNewSetUrl(String paasHostUrl)
    {
        String url = String.format("%s/api/c/compapi/v2/cc/create_set/", paasHostUrl);
        return url;
    }

    public static String getDeleteSetUrl(String paasHostUrl)
    {
        String url = String.format("%s/api/c/compapi/v2/cc/delete_set/", paasHostUrl);
        return url;
    }

    public static String getAddModuleUrl(String paasHostUrl)
    {
        String url = String.format("%s/api/c/compapi/v2/cc/create_module/", paasHostUrl);
        return url;
    }

    public static String getDeleteModuleUr(String paasHostUrl)
    {
        String url = String.format("%s/api/c/compapi/v2/cc/delete_module/", paasHostUrl);
        return url;
    }

    public static String getQueryModuleUrl(String paasHostUrl)
    {
        String url = String.format("%s/api/c/compapi/v2/cc/search_module/", paasHostUrl);
        return url;
    }

    public static String getTransferModuleUrl(String paasHostUrl)
    {
        String url = String.format("%s/api/c/compapi/v2/cc/transfer_host_module/", paasHostUrl);
        return url;
    }

    public static String getTransferHostToIdlePoolUrl(String paasHostUrl)
    {
        String url = String.format("%s/api/c/compapi/v2/cc/transfer_host_to_idlemodule/", paasHostUrl);
        return url;
    }

    public static String getTransferHostToResourceUrll(String paasHostUrl)
    {
        String url = String.format("%s/api/c/compapi/v2/cc/transfer_host_to_resourcemodule/", paasHostUrl);
        return url;
    }

    public static String getCreateNewBizUrl(String paasHostUrl)
    {
        String url = String.format("%s/api/c/compapi/v2/cc/create_business/", paasHostUrl);
        return url;
    }
}


