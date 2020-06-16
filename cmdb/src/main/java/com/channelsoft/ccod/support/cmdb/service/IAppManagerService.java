package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.constant.AppType;
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
     * @param hasImage 是否已经生成镜像
     * @return 查询结果
     * @throws Exception
     */
    List<AppModuleVo> queryApps(String appName, Boolean hasImage) throws DataAccessException;


    /**
     * 查询所有的已经注册的应用模块
     * @param hasImage 是否已经生成镜像
     * @return
     */
    List<AppModuleVo> queryAllRegisterAppModule(Boolean hasImage);

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
     * 开始收集平台更新数据
     * @param platformId
     * @param platformName
     * @return 收集结果
     * @throws Exception
     */
    PlatformAppModuleVo[] startCollectPlatformAppUpdateData(String platformId, String platformName) throws Exception;

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
     * 创建新的升级计划
     * @param paramVo 被创建的平台相关参数
     * @return 新建的平台创建计划
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws LJPaasException
     */
    PlatformUpdateSchemaInfo createNewPlatform(PlatformCreateParamVo paramVo) throws ParamException, NotSupportSetException, NotSupportAppException, InterfaceCallException, LJPaasException;


    /**
     * 删除指定平台的升级计划
     * @param platformId 指定的平台id
     * @throws ParamException 指定的平台不存在
     */
    void deletePlatformUpdateSchema(String platformId) throws ParamException;

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
    List<PlatformAppPo> updatePlatformApps(String platformId, String platformName, List<AppUpdateOperationInfo> appList) throws NotSupportAppException, ParamException, InterfaceCallException, NexusException, LJPaasException, IOException;

    /**
     * 获得app和set之间的关系
     * @return app和set之间的关系
     */
    Map<String, List<BizSetDefine>> getAppSetRelation();

    /**
     * 预处理通过在线管理程序收集上来的平台应用模块
     * @param platformName 平台名
     * @param platformId 平台id
     * @param moduleList 在线管理程序收集的应用模块
     * @param failList 在预处理时处理失败的应用模块
     * @return 预处理成功的应用模块
     */
    List<PlatformAppModuleVo> preprocessCollectedPlatformAppModule(String platformName, String platformId, List<PlatformAppModuleVo> moduleList, List<PlatformAppModuleVo> failList);

    /**
     * 检查应用是否有镜像
     * @param appName 应用名
     * @param version 版本
     * @return 如果有返回true，否则false
     * @throws ParamException 应用或是版本不存在
     */
    boolean hasImage(String appName, String version) throws ParamException;

    /**
     * 查询所有镜像的应用注册模块
     * @return 满足条件的注册应用模块
     */
    List<AppModuleVo> queryAllHasImageAppModule();

    /**
     * 从imageUrl解析该应用的类型
     * @param imageUrl 应用的imageUrl
     * @return 该应用的类型
     * @throws ParamException imageUrl不是一个合法的应用imageUrl
     * @throws NotSupportAppException 该应用对应的应用类型不被支持
     */
    AppType getAppTypeFromImageUrl(String imageUrl) throws ParamException, NotSupportAppException;

    /**
     * 根据应用别名获取某个业务集群下的应用模块信息
     * @param bizSetName 业务集群名
     * @param appAlias 应用别名
     * @param version 应用版本
     * @return 对应的应用模块信息
     * @throws ParamException 业务集群、应用、应用版本不存在
     * @throws NotSupportAppException 业务集群不支持该应用
     */
    AppModuleVo getAppModuleForBizSet(String bizSetName, String appAlias, String version) throws ParamException, NotSupportAppException;

}
