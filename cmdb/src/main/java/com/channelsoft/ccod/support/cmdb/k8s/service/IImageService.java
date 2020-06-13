package com.channelsoft.ccod.support.cmdb.k8s.service;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.exception.NexusException;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;

import java.util.List;

/**
 * @ClassName: IImageService
 * @Author: lanhb
 * @Description: 用来管理镜像的服务接口
 * @Date: 2020/6/13 10:46
 * @Version: 1.0
 */
public interface IImageService {

    /**
     * 检查指定标签的镜像是否存在
     * @param imageTag 镜像标签
     * @param repository 镜像所在仓库
     * @return 如果应用镜像存在返回true,否则返回false
     * @throws InterfaceCallException 调用nexus接口异常
     * @throws NexusException nexus返回调用失败或是解析nexus返回结果异常
     */
    boolean isImageExist(String imageTag, String repository) throws InterfaceCallException, NexusException, ParamException;

}
