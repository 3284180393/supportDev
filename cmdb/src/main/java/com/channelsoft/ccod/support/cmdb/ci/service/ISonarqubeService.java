package com.channelsoft.ccod.support.cmdb.ci.service;

import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;

/**
 * @ClassName: ISonarqubeService
 * @Author: lanhb
 * @Description: 用来定义sonarqube相关接口
 * @Date: 2020/9/27 20:05
 * @Version: 1.0
 */
public interface ISonarqubeService {

    /**
     * 获取sonarqube检查结果
     * @param jobName job名
     * @return sonarqube的检查结果
     */
    String getCheckResult(String jobName) throws InterfaceCallException;

}
