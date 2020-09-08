package com.channelsoft.ccod.support.cmdb.ci.service.impl;

import com.channelsoft.ccod.support.cmdb.ci.service.IJenkinsService;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Job;
import org.junit.Test;

import java.net.URI;
import java.util.Map;

/**
 * @ClassName: JenkinsServiceImpl
 * @Author: lanhb
 * @Description: IJenkinsService接口实现类
 * @Date: 2020/9/7 17:37
 * @Version: 1.0
 */
public class JenkinsServiceImpl implements IJenkinsService {

    private String testJenkinsUrl = "http://jenkins.ci.com";

    private String testJenkinsUser = "admin";

    private String testJenkinsPwd = "123456";


    @Test
    public void someTest()
    {
        try
        {
            firstTest();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    private JenkinsServer getTestServer() throws Exception
    {
        JenkinsServer jenkins = new JenkinsServer(new URI(testJenkinsUrl), testJenkinsUser, testJenkinsPwd);
        return jenkins;
    }

    private void firstTest() throws Exception
    {
        JenkinsServer jenkins = getTestServer();
        Map<String, Job> jobs = jenkins.getJobs();
        jobs.forEach((k,v)->{System.out.println(k); System.out.println(v);});
    }

}
