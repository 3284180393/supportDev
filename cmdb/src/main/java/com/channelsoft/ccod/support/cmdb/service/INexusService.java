package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.po.NexusComponentPo;
import com.channelsoft.ccod.support.cmdb.vo.DeployFileInfo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppModuleVo;

import java.util.Map;

/**
 * @ClassName: INexusService
 * @Author: lanhb
 * @Description: 用来定义同nexus相关的服务接口
 * @Date: 2019/11/13 19:54
 * @Version: 1.0
 */
public interface INexusService {
    /**
     * 将指定raw文件存放到指定的nexus位置
     * @param nexusHostUrl nexus主机url
     * @param userName nexus登录用户
     * @param password nexus用户登录密码
     * @param repository 指定的仓库
     * @param sourceFilePath 需要上传raw文件路径
     * @param group 在raw类型仓库的存放路径
     * @param fileName 保存在nexus的文件名
     * @return 上传结果
     * @throws Exception
     */
    boolean uploadRawFile(String nexusHostUrl, String userName, String password, String repository, String sourceFilePath, String group, String fileName) throws Exception;

    /**
     * 根据componentId从指定仓库查询component
     * @param nexusHostUrl nexus主机url
     * @param userName nexus登录用户
     * @param password nexus用户登录密码
     * @param componentId 指定的componentId
     * @return 查询结果
     * @throws Exception
     */
    NexusComponentPo queryComponentById(String nexusHostUrl, String userName, String password, String componentId) throws Exception;

    /**
     * 根据assetId从指定仓库查询asset
     * @param nexusHostUrl nexus主机url
     * @param userName nexus登录用户
     * @param password nexus用户登录密码
     * @param assetId 指定的assetId
     * @return 查询结果
     */
    NexusAssetInfo queryAssetById(String nexusHostUrl, String userName, String password, String assetId) throws Exception;

    /**
     * 从指定仓库查询所有的component
     * @param nexusHostUrl nexus主机url
     * @param userName nexus登录用户
     * @param password nexus用户登录密码
     * @param repository 指定的仓库
     * @return 查询结果
     * @throws Exception
     */
    NexusComponentPo[] queryComponentFromRepository(String nexusHostUrl, String userName, String password, String repository) throws Exception;

    /**
     * 将component的相关文件上传到指定的raw类型的repository下
     * @param nexusHostUrl nexus主机url
     * @param userName nexus登录用户
     * @param password nexus用户登录密码
     * @param repository 指定的raw类型仓库
     * @param directory 在仓库的存放路径
     * @param componentFiles 需要上传的文件
     * @return 是否上传成功
     * @throws Exception
     */
    Map<String, NexusAssetInfo> uploadRawComponent(String nexusHostUrl, String userName, String password, String repository, String directory, DeployFileInfo[] componentFiles) throws Exception;

    /**
     * 查询raw仓库，并生成repository : <directory, <fileName, asset>>的关系map
     * @param nexusHostUrl nexus主机url
     * @param userName nexus登录用户
     * @param password nexus用户登录密码
     * @param repository 需要查询的仓库名
     * @return 关系map
     * @throws Exception
     */
    Map<String, Map<String, NexusAssetInfo>> queryRepositoryAssetRelationMap(String nexusHostUrl, String userName, String password, String repository) throws Exception;

    /**
     * 下载一组文件到指定目录
     * @param componentAssets 需要下载的文件
     * @param savePath 保存目录
     * @throws Exception
     */
    void downloadComponent(String nexusHostUrl, String userName, String password, NexusAssetInfo[] componentAssets, String savePath) throws Exception;

    /**
     * 查询指定nexus指定repository下的指定group的存储文件
     * @param nexusHostUrl nexus主机url
     * @param userName nexus登录用户
     * @param password nexus用户登录密码
     * @param repository 仓库名
     * @param group 指定的group
     * @return 查询结果
     * @throws Exception
     */
    Map<String, NexusAssetInfo> queryGroupAssetMap(String nexusHostUrl, String userName, String password, String repository, String group) throws Exception;

    /**
     * 通过扫描应用发布nexus仓库的方式添加新的app
     * @param appName 应用名
     * @param appAlias 应用别名
     * @param version 发布版本
     * @param installPackageNexusAssetId 安装包在nexus的assetId
     * @param packageExt 按转包类型jar, binary, war, zip or war
     * @param cfgNexusAssetIds 配置文件在nexus的assetId
     * @return 添加后新的app在业务nexus的存放信息
     * @throws Exception
     */
    Map<String, NexusAssetInfo> addNewAppByScanPublishNexus(String appName, String appAlias, String version, String installPackageNexusAssetId, String packageExt, String[] cfgNexusAssetIds) throws Exception;

}
