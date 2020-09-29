package com.channelsoft.ccod.support.cmdb.ci.service;

import com.channelsoft.ccod.support.cmdb.ci.po.BuildDetailPo;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;

import java.util.Date;
import java.util.List;

/**
 * @ClassName: ICIService
 * @Author: lanhb
 * @Description: 用来定义ci相关接口
 * @Date: 2020/9/27 20:23
 * @Version: 1.0
 */
public interface ICIService {

    BuildDetailPo getJobBuildResult(String jobName) throws Exception;

    /**
     * 创建一个ci构建
     * @param jobName 该构建在jenkins的job名
     * @throws Exception 异常
     */
    void createBuild(String jobName) throws Exception;

    /**
     * 查询指定条件的构建历史
     * @param jobName job名
     * @param appName 应用名
     * @param version 版本
     * @param startTime 晚于此时间
     * @param endTime 早于此时间
     * @throws Exception
     * @return 查询结果
     */
    List<BuildDetailPo> queryBuildHistory(String jobName, String appName, String version, String startTime, String endTime) throws Exception;

}
