package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.vo.CcodPlatformAppVo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppModuleVo;

import java.util.List;

/**
 * @ClassName: IPlatformAppCollectService
 * @Author: lanhb
 * @Description: 用来定义平台模块收集服务的接口
 * @Date: 2019/11/15 11:20
 * @Version: 1.0
 */
public interface IPlatformAppCollectService {

    /**
     * 从平台收集指定条件的应用部署情况，并上传安装包和配置文件
     * @param platformId 平台id不能为空
     * @param platformName 平台名
     * @param domainName 域名可以为空
     * @param hostIp 主机ip可以为空
     * @param appName 应用名，可以为空
     * @param version 应用版本，可以为空
     * @return 指定条件的应用部署情况
     * @throws Exception
     */
    List<PlatformAppModuleVo> collectPlatformAppData(String platformId, String platformName, String domainName, String hostIp, String appName, String version) throws Exception;

    /**
     * 从平台收集指定条件的应用部署情况，不上传安装包和配置文件
     * @param platformId 平台id不能为空
     * @param platformName 平台名
     * @param domainName 域名可以为空
     * @param hostIp 主机ip可以为空
     * @param appName 应用名，可以为空
     * @param version 应用版本，可以为空
     * @return 指定条件的应用部署情况
     * @throws Exception
     */
    List<PlatformAppModuleVo> checkPlatformAppData(String platformId, String platformName, String domainName, String hostIp, String appName, String version) throws Exception;
}
