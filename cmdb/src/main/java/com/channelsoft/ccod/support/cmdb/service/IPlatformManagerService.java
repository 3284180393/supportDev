package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.exception.NotSupportAppException;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformTopologyInfo;
import io.kubernetes.client.openapi.ApiException;

/**
 * @ClassName: IPlatformManagerService
 * @Author: lanhb
 * @Description: 用来定义平台管理的接口类
 * @Date: 2020/5/28 9:53
 * @Version: 1.0
 */
public interface IPlatformManagerService {

    /**
     * 从k8s生成指定平台的拓扑结构
     * @param platformName 平台名
     * @param platformId 平台id
     * @param k8sApiUrl k8s api的url
     * @param k8sAuthToken k8s api的访问token
     * @return 平台拓扑
     * @throws ApiException 访问k8s api异常
     */
    PlatformTopologyInfo getPlatformTopologyFromK8s(String platformName, String platformId, String k8sApiUrl, String k8sAuthToken) throws ApiException, ParamException, NotSupportAppException;
}
