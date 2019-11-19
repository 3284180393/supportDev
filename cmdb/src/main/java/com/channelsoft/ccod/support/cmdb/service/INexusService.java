package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.po.NexusComponentPo;

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
     * @param sourceFilePath 需要上传raw文件路径
     * @param group 在raw类型仓库的存放路径
     * @param fileName 保存在nexus的文件名
     * @return 上传结果
     * @throws Exception
     */
    boolean uploadRawFile(String sourceFilePath, String group, String fileName) throws Exception;

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
     * @return
     * @throws Exception
     */
    NexusComponentPo[] queryComponentFromRepository() throws Exception;
}
