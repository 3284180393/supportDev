package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.AppUpdateOperation;
import com.channelsoft.ccod.support.cmdb.constant.VersionControl;
import com.channelsoft.ccod.support.cmdb.exception.*;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.vo.*;
import org.springframework.dao.DataAccessException;

import java.io.IOException;
import java.util.Comparator;
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
     * 根据set定义生成应用模块sort
     * @param setDefine set定义
     * @return set下模块的排序sort
     */
    Comparator<AppBase> getAppSort(BizSetDefine setDefine);

    /**
     * 查询指定版本的应用模块
     * @param appName 模块名
     * @param version 版本号
     * @param hasImage 是否有镜像，如果为空忽略对镜像检查
     * @return 查询结果
     * @throws Exception
     */
    AppModuleVo queryAppByVersion(String appName, String version, Boolean hasImage) throws ParamException, DataAccessException;

    /**
     * 查询指定应用信息,如果appName为空则查询所有的应用信息
     * @param appName 应用名
     * @param hasImage 是否已经生成镜像
     * @param ccodVersion 支持指定ccod大版本，如果为空则忽略该参数
     * @return 查询结果
     * @throws Exception
     */
    List<AppModuleVo> queryApps(String appName, Boolean hasImage, String ccodVersion) throws DataAccessException;


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
     * 创建一个新的平台应用收集任务
     * @param platformId 平台id，不能为空
     * @param platformName 平台名,不能为空
     * @throws Exception
     */
    void createNewPlatformAppDataCollectTask(String platformId, String platformName, int bkBizId, int bkCloudId) throws Exception;

    /**
     * 向系统注册新的版本的应用
     * @param appModule 被注册的版本信息
     * @throws NotSupportAppException 不支持注册的应用
     * @throws ParamException 应用参数错误，例如版本重复
     * @throws InterfaceCallException 调用nexus的api失败
     * @throws NexusException nexus的api返回调用失败或是解析nexus的返回结果失败
     * @throws IOException
     */
    void registerNewAppModule(AppModuleVo appModule) throws ParamException, InterfaceCallException, NexusException, IOException;

    /**
     * CI注册ccod模块
     * @param appModule 模块信息
     * @throws NotSupportAppException
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws NexusException
     * @throws IOException
     */
    void registerCIAppModule(AppModuleVo appModule) throws ParamException, InterfaceCallException, NexusException, IOException;

    /**
     * CI注册ccod模块
     * @param appName 模块名
     * @param version 版本
     * @param ccodVersion 适用ccod大版本
     * @param gitUrl 模块的git url地址
     * @param repository 构建后的应用模块存放在nexus的仓库
     * @param path 构建后的应用模块存放在nexus的path
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws NexusException
     * @throws IOException
     */
    void registerCIAppModule(String appName, AppType appType, String version, String ccodVersion, String gitUrl, String repository, String path) throws ParamException, InterfaceCallException, NexusException, IOException;

    /**
     * 检查应用的基础属性
     * @param appBase 应用基础数据集合
     * @param operation 对应用的相关操作
     * @return 检查结果，如果检查通过返回"",否则返回检查出来的问题描述
     */
    String checkAppBaseProperties(AppBase appBase, AppUpdateOperation operation);

    /**
     * 更新已有的应用模块
     * @param appModule 需要修改配置的应用模块信息
     * @throws ParamException 应用参数错误，例如版本重复
     * @throws InterfaceCallException 调用nexus的api失败
     * @throws NexusException nexus的api返回调用失败或是解析nexus的返回结果失败
     * @throws IOException
     */
    void updateAppModule(AppModuleVo appModule) throws ParamException, InterfaceCallException, NexusException, IOException;

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
     * 查询ccod biz下面set信息
     * @param ccodVersion ccod大版本
     * @param hasImage 是否有image
     * @return set下的所有模块都至少有一个已经注册并且已经生成镜像版本
     */
    List<BizSetDefine> queryCCODBizSetWithImage(String ccodVersion, Boolean hasImage);

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
     * @throws NotSupportAppException 指定应用不支持
     */
    AppType getAppTypeFromImageUrl(String imageUrl) throws ParamException, NotSupportAppException;

    /**
     * 指定镜像是不是ccod模块镜像
     * @param imageUrl 指定镜像
     * @return 是或是不是
     */
    boolean isRegisteredCCODAppImage(String imageUrl);

    /**
     * 从imageUrl获取该镜像对应的ccod模块注册信息
     * @param imageUrl 镜像url
     * @return 模块注册信息
     * @throws ParamException
     */
    AppModuleVo getRegisteredCCODAppFromImageUrl(String imageUrl) throws ParamException;

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

    /**
     * 从源nexus下载应用相关文件并上传到指定的nexus仓库里
     * @param srcNexusHostUrl 源nexus的url地址
     * @param srcNexusUser 源nexus的登录用户
     * @param srcPwd 源nexus
     * @param srcFileList 需要下载并上传文件列表
     * @param dstRepository 上传的目的仓库
     * @param dstDirectory 上传路径
     * @return
     */
    List<AppFileNexusInfo> downloadAndUploadAppFiles(String srcNexusHostUrl, String srcNexusUser, String srcPwd, List<AppFileNexusInfo> srcFileList, String dstRepository, String dstDirectory) throws ParamException, InterfaceCallException, NexusException, IOException;

    /**
     * 更新平台已经注册模块信息
     */
    void flushRegisteredApp();

    /**
     * 添加新的应用模块
     * @param appPo 应用相关信息
     * @param installPackage 安装包
     * @param cfgs 配置文件
     * @return 添加后的模块信息
     * @throws InterfaceCallException
     * @throws NexusException
     */
    AppModuleVo addNewAppModule(AppPo appPo, DeployFileInfo installPackage, DeployFileInfo[] cfgs) throws InterfaceCallException, NexusException;

    /**
     * 预处理平台收集模块
     * @param module 平台收集模块
     * @throws DataAccessException
     * @throws InterfaceCallException
     * @throws NexusException
     * @throws ParamException
     */
    void preprocessPlatformAppModule(PlatformAppModuleVo module) throws DataAccessException, InterfaceCallException, NexusException, ParamException;

    /**
     * cmdb是否支持指定的应用名
     * @param appName 应用名
     * @return true支持，false不支持
     */
    boolean isSupport(String appName);

    /**
     * 指定的应用是否支持某个ccod大版本
     * @param appName 应用名
     * @param version 应用版本
     * @param ccodVersion ccod大版本号
     * @return 是否支持
     */
    boolean isCcodVersionSupport(String appName, String version, String ccodVersion);
}
