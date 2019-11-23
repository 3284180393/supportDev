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
     * @param repository 指定的仓库
     * @param sourceFilePath 需要上传raw文件路径
     * @param group 在raw类型仓库的存放路径
     * @param fileName 保存在nexus的文件名
     * @return 上传结果
     * @throws Exception
     */
    boolean uploadRawFile(String repository, String sourceFilePath, String group, String fileName) throws Exception;

    /**
     * 根据componentId从指定仓库查询component
     * @param componentId 指定的componentId
     * @return 查询结果
     * @throws Exception
     */
    NexusComponentPo queryComponentById(String componentId) throws Exception;

    /**
     * 根据assetId从指定仓库查询asset
     * @param assetId 指定的assetId
     * @return 查询结果
     */
    NexusAssetInfo queryAssetById(String assetId) throws Exception;

    /**
     * 从指定仓库查询所有的component
     * @param repository 指定的仓库
     * @return 查询结果
     * @throws Exception
     */
    NexusComponentPo[] queryComponentFromRepository(String repository) throws Exception;

    /**
     * 将component的相关文件上传到指定的raw类型的repository下
     * @param repository 指定的raw类型仓库
     * @param directory 在仓库的存放路径
     * @param componentFiles 需要上传的文件
     * @return 是否上传成功
     * @throws Exception
     */
    Map<String, Map<String, NexusAssetInfo>> uploadRawComponent(String repository, String directory, DeployFileInfo[] componentFiles) throws Exception;

    /**
     * reload某个repository的component信息
     * @param repository 需要装载的仓库
     */
    void reloadRepositoryComponent(String repository) throws Exception;

    /**
     * release某个repository的component信息
     * @param repository 需要释放信息的仓库
     */
    void releaseRepositoryComponent(String repository);

    /**
     * 如果module对应app以及版本在nexus仓库没有，则创建
     * @param module
     * @throws Exception
     */
    void addPlatformAppModule(PlatformAppModuleVo module) throws Exception;

    /**
     * 将一组平台应用模块上传到nexus
     * 如果某个模块对应的component已经在nexus存在则对比安装包的md5,如果md5不一致则报错，该平台应用模块上传失败,否则上传该平台应用模块的配置文件
     * 如果component不存在则创建该应用对应的component(appName/appAlias/version),并上传该平台应用的配置文件
     * @param appRepository 保存应用component的仓库
     * @param cfgRepository 保存平台配置文件的仓库
     * @param modules 需要上传的模块
     * @throws Exception
     */
    void uploadPlatformAppModules(String appRepository, String cfgRepository, PlatformAppModuleVo[] modules) throws Exception;

    /**
     * 查询raw仓库，并生成repository : <directory, <fileName, asset>>的关系map
     * @param repository 需要查询的仓库名
     * @return 关系map
     * @throws Exception
     */
    Map<String, Map<String, NexusAssetInfo>> queryRepositoryAssetRelationMap(String repository) throws Exception;

}
