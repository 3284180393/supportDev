package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.constant.VersionControl;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.po.AppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.AppInstallPackagePo;
import com.channelsoft.ccod.support.cmdb.po.AppPo;
import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.vo.*;
import org.springframework.dao.DataAccessException;

import java.util.Map;

/**
 * @ClassName: IAppManagerService
 * @Author: lanhb
 * @Description: 应用管理接口
 * @Date: 2019/11/14 13:54
 * @Version: 1.0
 */
public interface IAppManagerService {

    /**
     * 创建新的应用模块
     * @param appName 应用名
     * @param appAlias 应用别名
     * @param version 应用版本号
     * @param versionControl 版本控制方式
     * @param versionControlUrl 版本控制连接url
     * @param installPackage 应用包存放路径
     * @param cfgs 应用的相关配置存放路径
     * @param basePath 应用在服务器的base path
     * @return 创建后的应用模块信息
     * @throws Exception
     */
    AppModuleVo createNewAppModule(String appName, String appAlias, String version, VersionControl versionControl,
                                   String versionControlUrl, AppInstallPackagePo installPackage, AppCfgFilePo[] cfgs,
                                   String basePath) throws Exception;

    /**
     * 查询指定版本的应用模块
     * @param appName 模块名
     * @param version 版本号
     * @return 查询结果
     * @throws Exception
     */
    AppModuleVo queryAppByVersion(String appName, String version) throws ParamException, DataAccessException;

    /**
     * 查询指定应用信息,如果appName为空则查询所有的应用信息
     * @param appName 应用名
     * @return 查询结果
     * @throws Exception
     */
    AppModuleVo[] queryApps(String appName) throws DataAccessException;

    /**
     * 查询某个平台所有模块部署情况
     * @param platformId 平台id
     * @param domainId 域id
     * @param hostIp 服务器ip
     * @return 查询结果
     * @throws Exception
     */
    PlatformAppDeployDetailVo[] queryPlatformApps(String platformId, String domainId, String hostIp) throws DataAccessException;

    /**
     * 查询应用在平台的部署情况,条件可以为空,如果为空则忽略该参数
     * @param appName 应用名
     * @param platformId 平台id
     * @param domainId 域id
     * @param hostIp 主机ip
     * @return 查询结果
     * @throws Exception
     */
    PlatformAppDeployDetailVo[] queryAppDeployDetails(String appName, String platformId, String domainId, String hostIp) throws DataAccessException;

    /**
     * 检查指定条件的平台的应用部署情况，并上传对应的安装包和配置文件
     * @param platformId 平台id，不能为空
     * @param platformName 平台名,不能为空
     * @param domainName 域名，可以为空
     * @param hostIp 主机名，可以为空
     * @param appName 应用名，可以为空
     * @param version 版本号，可以为空
     * @return 所有满足条件的应用配置信息
     * @throws Exception
     */
    PlatformAppModuleVo[] startCollectPlatformAppData(String platformId, String platformName, String domainName, String hostIp, String appName, String version) throws Exception;

    /**
     * 检查指定条件的平台的应用部署情况，不上传对应的安装包和配置文件
     * @param platformId 平台id，不能为空
     * @param platformName 平台名,不能为空
     * @param domainName 域名，可以为空
     * @param hostIp 主机名，可以为空
     * @param appName 应用名，可以为空
     * @param version 版本号，可以为空
     * @return 所有满足条件的应用配置信息
     * @throws Exception
     */
    PlatformAppModuleVo[] startCheckPlatformAppData(String platformId, String platformName, String domainName, String hostIp, String appName, String version) throws Exception;

    /**
     * 创建一个新的平台应用收集任务
     * @param platformId 平台id，不能为空
     * @param platformName 平台名,不能为空
     * @param domainId 域名，可以为空
     * @param hostIp 主机名，可以为空
     * @param appName 应用名，可以为空
     * @param version 版本号，可以为空
     * @throws Exception
     */
    void createNewPlatformAppDataCollectTask(String platformId, String platformName,String domainId, String hostIp, String appName, String version) throws Exception;

    /**
     * 通过扫描应用发布nexus仓库的方式添加新的app
     * @param appType 应用类型
     * @param appName 应用名
     * @param appAlias 应用别名
     * @param version 发布版本
     * @param ccodVersion 应用适用的ccod版本
     * @param installPackage 安装包信息
     * @param cfgs 配置文件信息
     * @param basePath 应用安装的根路径
     * @return 添加后的应用信息
     * @throws Exception
     */
    AppPo addNewAppFromPublishNexus(String appType, String appName, String appAlias, String version, String ccodVersion, AppModuleFileNexusInfo installPackage, AppModuleFileNexusInfo[] cfgs, String basePath) throws Exception;

}
