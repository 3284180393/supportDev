package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.vo.PlatformResourceVo;

/**
 * @ClassName: IPlatformResourceService
 * @Author: lanhb
 * @Description: 用来管理平台资源的服务接口类
 * @Date: 2019/11/27 15:49
 * @Version: 1.0
 */
public interface IPlatformResourceService {

    /**
     * 查询所有的平台资源
     * @return 查询结果
     * @throws Exception
     */
    PlatformResourceVo[] queryPlatformResources() throws Exception;

    /**
     * 查询指定条件的平台资源
     * @param platformId 平台id
     * @param domainId 域id
     * @param hostIp 主机ip
     * @return 查询结果
     * @throws Exception
     */
    PlatformResourceVo queryPlatformResource(String platformId, String domainId, String hostIp) throws Exception;
}
