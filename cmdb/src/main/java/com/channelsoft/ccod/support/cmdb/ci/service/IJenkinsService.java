package com.channelsoft.ccod.support.cmdb.ci.service;

import com.channelsoft.ccod.support.cmdb.ci.po.BuildDetailPo;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;

import java.io.IOException;

/**
 * @ClassName: IJenkinsService
 * @Author: lanhb
 * @Description: 用来定义Jenkins服务接口
 * @Date: 2020/9/7 17:36
 * @Version: 1.0
 */
public interface IJenkinsService {

    BuildDetailPo getLastBuildDetails(String jobName) throws Exception;

    JobWithDetails getJobDetail(String jobName) throws Exception;

    BuildWithDetails getBuildDetails(String jobName, int queueId) throws Exception;

    BuildDetailPo getBuildResultFromDetail(JobWithDetails job, BuildWithDetails details) throws IOException;

}
