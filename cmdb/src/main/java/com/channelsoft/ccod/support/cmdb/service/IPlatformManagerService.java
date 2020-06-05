package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.exception.*;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppModuleVo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformTopologyInfo;
import io.kubernetes.client.openapi.ApiException;

import java.io.IOException;

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
     * @param bkBizId 平台在蓝鲸paas的biz id
     * @param bkCloudId 平台服务器在蓝鲸paas的cloud id
     * @param ccodVersion 平台的ccod大版本号
     * @param k8sApiUrl k8s api的url
     * @param k8sAuthToken k8s api的访问token
     * @return 平台拓扑
     * @throws ApiException
     * @throws ParamException
     * @throws NotSupportAppException
     * @throws NexusException
     * @throws LJPaasException
     * @throws InterfaceCallException
     * @throws IOException
     */
    PlatformTopologyInfo getPlatformTopologyFromK8s(String platformName, String platformId, int bkBizId, int bkCloudId, String ccodVersion, String k8sApiUrl, String k8sAuthToken) throws ApiException, ParamException, NotSupportAppException, NexusException, LJPaasException, InterfaceCallException, IOException;

    /**
     * 删除指定平台的升级计划
     * @param platformId 指定的平台id
     * @throws ParamException 指定的平台不存在
     */
    void deletePlatformUpdateSchema(String platformId) throws ParamException;

    /**
     * 创建平台应用更新任务
     * @param platformId 平台id
     * @param platformName 平台名
     * @throws Exception 创建任务失败
     */
    void createNewPlatformAppDataUpdateTask(String platformId, String platformName) throws Exception;

    /**
     * 删除已有的平台
     * @param platformId 被删除的平台的id
     * @throws ParamException 被删除的平台不存在
     */
    void deletePlatform(String platformId) throws ParamException;

    /**
     * 检查指定条件的平台的应用部署情况，并上传对应的安装包和配置文件
     * @param platformId 平台id，不能为空
     * @param platformName 平台名,不能为空
     * @param bkBizId 该平台在蓝鲸paas上的的biz id
     * @param bkCloudId 该平台服务器在蓝鲸paas上的cloud id
     * @throws Exception
     */
    void startCollectPlatformAppData(String platformId, String platformName, int bkBizId, int bkCloudId) throws Exception;
}
