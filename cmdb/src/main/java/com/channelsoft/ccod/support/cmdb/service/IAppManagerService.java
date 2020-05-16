package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.constant.VersionControl;
import com.channelsoft.ccod.support.cmdb.exception.*;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.vo.*;
import org.springframework.dao.DataAccessException;

import java.io.IOException;
import java.util.List;
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
     * @param bkBizId 该平台在蓝鲸paas上的的biz id
     * @param bkCloudId 该平台服务器在蓝鲸paas上的cloud id
     * @return 所有满足条件的应用配置信息
     * @throws Exception
     */
    PlatformAppModuleVo[] startCollectPlatformAppData(String platformId, String platformName, int bkBizId, int bkCloudId) throws Exception;

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
     * @throws Exception
     */
    void createNewPlatformAppDataCollectTask(String platformId, String platformName, int bkBizId, int bkCloudId) throws Exception;

    /**
     * 更新平台升级计划
     * @param updateSchema 需要更新的平台计划
     * @throws ParamException 计划的参数异常
     * @throws InterfaceCallException 处理计划时调用蓝鲸api或是nexus api失败
     * @throws LJPaasException 调用蓝鲸api返回调用失败或是解析蓝鲸api结果失败
     * @throws NexusException 调用nexus api返回调用失败或是解析nexus api返回结果失败
     * @throws IOException 处理文件失败
     */
    void updatePlatformUpdateSchema(PlatformUpdateSchemaInfo updateSchema) throws NotSupportSetException, NotSupportAppException, ParamException, InterfaceCallException, LJPaasException, NexusException, IOException;

    /**
     * 创建一个平台升级计划demo
     * @param paramVo 希望生成的demo计划的相关参数
     * @return 生成的计划demo
     * @throws ParamException 计划的参数异常
     * @throws InterfaceCallException 处理计划时调用蓝鲸api或是nexus api失败
     * @throws LJPaasException 调用蓝鲸api返回调用失败或是解析蓝鲸api结果失败
     */
    PlatformUpdateSchemaInfo createPlatformUpdateSchemaDemo(PlatformUpdateSchemaParamVo paramVo) throws ParamException, InterfaceCallException, LJPaasException;

    /**
     * 查询指定条件的平台升级计划
     * @param platformId 平台id可以为空
     * @return 满足条记按的升级计划
     */
    List<PlatformUpdateSchemaInfo> queryPlatformUpdateSchema(String platformId);

    /**
     * 查询指定平台的拓扑接口
     * @param platformId 平台id
     * @return 平台的拓扑
     * @throws ParamException 计划的参数异常
     * @throws InterfaceCallException 处理计划时调用蓝鲸api或是nexus api失败
     * @throws LJPaasException 调用蓝鲸api返回调用失败或是解析蓝鲸api结果失败
     * @throws NexusException 调用nexus api返回调用失败或是解析nexus api返回结果失败
     */
    PlatformTopologyInfo getPlatformTopology(String platformId) throws ParamException, InterfaceCallException, LJPaasException, NotSupportAppException;

    /**
     * 查询所有平台简单拓扑结构
     * @return 当前所有平台的状态
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws LJPaasException
     * @throws NotSupportAppException
     */
    List<PlatformTopologyInfo> queryAllPlatformTopology() throws ParamException, InterfaceCallException, LJPaasException, NotSupportAppException;

    /**
     * 向系统注册新的版本的应用
     * @param appModule 被注册的版本信息
     * @throws NotSupportAppException 不支持注册的应用
     * @throws ParamException 应用参数错误，例如版本重复
     * @throws InterfaceCallException 调用nexus的api失败
     * @throws NexusException nexus的api返回调用失败或是解析nexus的返回结果失败
     * @throws IOException
     */
    void registerNewAppModule(AppModuleVo appModule) throws NotSupportAppException, ParamException, InterfaceCallException, NexusException, IOException;

    /**
     * 更新已有的应用模块
     * @param appModule 需要修改配置的应用模块信息
     * @throws NotSupportAppException 不支持注册的应用
     * @throws ParamException 应用参数错误，例如版本重复
     * @throws InterfaceCallException 调用nexus的api失败
     * @throws NexusException nexus的api返回调用失败或是解析nexus的返回结果失败
     * @throws IOException
     */
    void updateAppModule(AppModuleVo appModule) throws NotSupportAppException, ParamException, InterfaceCallException, NexusException, IOException;

    /**
     * 创建demo新平台
     * @param platformId 平台id
     * @param platformName 平台名
     * @param bkCloudId 平台服务器所在的机房id
     * @param planAppList 新建平台计划部署的应用
     * @return 创建的平台
     * @throws ParamException 计划的参数异常
     * @throws InterfaceCallException 处理计划时调用蓝鲸api或是nexus api失败
     * @throws LJPaasException 调用蓝鲸api返回调用失败或是解析蓝鲸api结果失败
     */
    PlatformUpdateSchemaInfo createDemoNewPlatform(String platformId, String platformName, int bkBizId, int bkCloudId, List<String> planAppList) throws ParamException, InterfaceCallException, LJPaasException;

    /**
     * 创建新的升级计划
     * @param paramVo 被创建的平台相关参数
     * @return 新建的平台创建计划
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws LJPaasException
     */
    PlatformUpdateSchemaInfo createNewPlatform(PlatformCreateParamVo paramVo) throws ParamException, NotSupportSetException, NotSupportAppException, InterfaceCallException, LJPaasException;

    /**
     * 创建demo的升级平台
     * @param platformId 平台id
     * @param platformName 平台名
     * @param bkBizId 平台对应的biz id
     * @return 创建的demo升级平台
     * @throws ParamException 计划的参数异常
     * @throws InterfaceCallException 处理计划时调用蓝鲸api或是nexus api失败
     * @throws LJPaasException 调用蓝鲸api返回调用失败或是解析蓝鲸api结果失败
     */
    PlatformUpdateSchemaInfo createDemoUpdatePlatform(String platformId, String platformName, int bkBizId) throws ParamException, InterfaceCallException, LJPaasException;

    /**
     * 删除指定平台的升级计划
     * @param platformId 指定的平台id
     * @throws ParamException 指定的平台不存在
     */
    void deletePlatformUpdateSchema(String platformId) throws ParamException;

    /**
     * 删除某个平台
     * @param platformId 需要删除的平台id
     * @throws ParamException
     */
    void deletePlatform(String platformId) throws ParamException;

    /**
     * 克隆指定的域
     * @param platformId 被克隆的域所属的平台
     * @param clonedDomainId 被克隆的域id
     * @param domainId 新建域id
     * @param domainName 新建域名
     * @return 该平台的升级计划
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws LJPaasException
     */
    PlatformUpdateSchemaInfo cloneExistDomain(String platformId, String clonedDomainId, String domainId, String domainName) throws ParamException, InterfaceCallException, LJPaasException;

    /**
     * 查询指定应用的配置文件并以字符串的形式返回
     * @param appName 应用名
     * @param version 应用别名
     * @param cfgFileName 配置文件名
     * @return 指定配置文件的文本信息
     * @throws ParamException 指定的应用或是配置文件不存在
     * @throws NexusException 下载配置文件时nexus返回异常
     * @throws InterfaceCallException  调用nexus接口异常
     */
    String getAppCfgText(String appName, String version, String cfgFileName) throws ParamException, NexusException, InterfaceCallException, IOException;

    /**
     * 查询ccod biz下面set信息
     * @param isCheckApp 如果为true在返回的set信息中的应用都可以查到具体版本，否则包含所有的应用，这些应用可能没有记录任何版本
     * @return set信息
     */
    List<BizSetDefine> queryCCODBizSet(boolean isCheckApp);

    /**
     * 将应用从指定仓库迁移到目标仓库
     * @param targetRepository 目标仓库
     */
    void appDataTransfer(String targetRepository);

    /**
     * 更新已有的平台应用模块
     * @param platformId 平台id
     * @param platformName 平台名
     * @param appList 需要更新的平台应用列表
     * @return 更新后的平台应用信息
     * @throws ParamException 需要更新的平台应用信息错误
     * @throws InterfaceCallException 调用接口发生异常
     * @throws NexusException 调用nexus的api返回调用错误或是解析nexus返回结果异常
     */
    List<PlatformAppPo> updatePlatformApps(String platformId, String platformName, List<PlatformAppDeployDetailVo> appList) throws ParamException, InterfaceCallException, NexusException;

}
