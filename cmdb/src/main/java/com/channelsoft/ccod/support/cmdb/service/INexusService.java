package com.channelsoft.ccod.support.cmdb.service;

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
     * @param repository 上传到的nexus指定raw类型仓库
     * @param group 在raw类型仓库的存放路径
     * @param fileName 保存在nexus的文件名
     * @return 上传结果
     * @throws Exception
     */
    boolean uploadRawFile(String sourceFilePath, String repository, String group, String fileName) throws Exception;


}
