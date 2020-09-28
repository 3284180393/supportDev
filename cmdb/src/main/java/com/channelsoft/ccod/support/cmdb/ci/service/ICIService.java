package com.channelsoft.ccod.support.cmdb.ci.service;

import com.channelsoft.ccod.support.cmdb.ci.po.BuildDetailPo;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;

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

}
