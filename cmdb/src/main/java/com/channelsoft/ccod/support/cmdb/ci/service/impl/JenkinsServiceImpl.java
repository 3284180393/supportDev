package com.channelsoft.ccod.support.cmdb.ci.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.ci.po.BuildDetailPo;
import com.channelsoft.ccod.support.cmdb.ci.service.IJenkinsService;
import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.*;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @ClassName: JenkinsServiceImpl
 * @Author: lanhb
 * @Description: IJenkinsService接口实现类
 * @Date: 2020/9/7 17:37
 * @Version: 1.0
 */
@Service
public class JenkinsServiceImpl implements IJenkinsService {

    @Value("${ci.jenkins.host-url}")
    private String hostUrl;

    @Value("${ci.jenkins.user-name}")
    private String user;

    @Value("${ci.jenkins.password}")
    private String password;

    private final static Logger logger = LoggerFactory.getLogger(JenkinsServiceImpl.class);

    @Override
    public BuildDetailPo getLastBuildDetails(String jobName) throws Exception{
        JenkinsServer jenkins = getJenKinsServer();
        JobWithDetails job = jenkins.getJob(jobName);
        Assert.notNull(job, String.format("job %s not exist", jobName));
        int number = job.getBuilds().get(0).getNumber();
        BuildWithDetails details = job.getBuilds().get(0).details();
        while (details.isBuilding()){
            Thread.sleep(3000);
            details = job.getBuilds().stream().collect(Collectors.toMap(Build::getNumber, Function.identity())).get(number).details();
        }
        BuildDetailPo po = getBuildResultFromDetail(job, details);
        return po;
    }

    @Override
    public BuildDetailPo getBuildResultFromDetail(JobWithDetails job, BuildWithDetails details) throws IOException
    {
        String leader = job.getDescription().split("\\n")[1].replace("责任人：", "");
        BuildDetailPo po = new BuildDetailPo();
        po.setProjectLeader(leader);
        Map<String, String> params = new HashMap<>();
        List<LinkedHashMap<String, String>> paramList = (List<LinkedHashMap<String, String>>)((LinkedHashMap<String, Object>)details.getActions().get(0)).get("parameters");
        paramList.forEach(p->{
            params.put(p.get("name"), p.get("value"));
        });
        po.setParameters(params);
        po.setAppName(params.get("ModName"));
        po.setVersion(params.get("VERSION"));
        po.setStartTime(new Date(details.getTimestamp()));
        po.setEndTime(new Date(details.getTimestamp() + details.getDuration()));
        po.setJenkinsLog(details.getConsoleOutputText());
        po.setJenkinsResult(details.getResult().name());
        po.setJobName(job.getName());
        po.setNumber(details.getNumber());
        po.setQueueId(details.getQueueId());
        logger.warn(JSONObject.toJSONString(po));
        return po;
    }

    @Override
    public JobWithDetails getJobDetail(String jobName) throws Exception {
        JenkinsServer jenkins = getJenKinsServer();
        JobWithDetails job = jenkins.getJob(jobName);
        return job;
    }

    @Override
    public BuildWithDetails getBuildDetails(String jobName, int number) throws Exception {
        JobWithDetails job = getJobDetail(jobName);
        Assert.notNull(job, String.format("job %s not exist", jobName));
        Map<Integer, Build> buildMap = job.getBuilds().stream().collect(Collectors.toMap(Build::getNumber, Function.identity()));
        Assert.isTrue(buildMap.containsKey(number), String.format("job %s with number %d build not exist", job, number));
        return buildMap.get(number).details();
    }

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

    private JenkinsServer getJenKinsServer() throws Exception
    {
        JenkinsServer jenkins = new JenkinsServer(new URI(hostUrl), user, password);
        return jenkins;
    }

    private void firstTest() throws Exception
    {
        String json = "{\"appName\":\"cmsserver\",\"endTime\":1601280955287,\"id\":0,\"jenkinsResult\":\"SUCCESS\",\"jobName\":\"cmsserver\",\"number\":7,\"parameters\":{\"ModName\":\"cmsserver\",\"buildFileName\":\"\",\"CmdbPath\":\"10.130.40.164:8086\",\"DockerRegisterPass\":\"123456\",\"NexusUser\":\"admin\",\"MakeImageTag\":\"0.8\",\"NexusPass\":\"123456\",\"DockerRegisterUser\":\"admin\",\"MakeCommand\":\"cd /root/unix/; make prehook; cd ../slice; scons; cd ..; scons; mkdir bin; mv cmsserver bin/;\",\"NexusRegisterName\":\"tmp\",\"DingDingImage\":\"https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=69157461,110105380&fm=26&gp=0.jpg\",\"DockerRegisterPath\":\"nexus.io:5000\",\"VERSION\":\"8f7b2f98951ba85bca4fc1dbd57dd361531d9020\",\"NexusPath\":\"10.130.41.218:8081\",\"RunningImage\":\"ccod-base/centos-backend:0.5\",\"MvnModName\":\"cmsserver\",\"VersionVerifyImageTag\":\"1.0.1\"},\"projectLeader\":\"肖少辉\",\"queueId\":3848,\"sonarOutput\":\"{\\\"projectStatus\\\":{\\\"status\\\":\\\"ERROR\\\",\\\"conditions\\\":[{\\\"status\\\":\\\"OK\\\",\\\"metricKey\\\":\\\"new_reliability_rating\\\",\\\"comparator\\\":\\\"GT\\\",\\\"periodIndex\\\":1,\\\"errorThreshold\\\":\\\"1\\\",\\\"actualValue\\\":\\\"1\\\"},{\\\"status\\\":\\\"OK\\\",\\\"metricKey\\\":\\\"new_security_rating\\\",\\\"comparator\\\":\\\"GT\\\",\\\"periodIndex\\\":1,\\\"errorThreshold\\\":\\\"1\\\",\\\"actualValue\\\":\\\"1\\\"},{\\\"status\\\":\\\"OK\\\",\\\"metricKey\\\":\\\"new_maintainability_rating\\\",\\\"comparator\\\":\\\"GT\\\",\\\"periodIndex\\\":1,\\\"errorThreshold\\\":\\\"1\\\",\\\"actualValue\\\":\\\"1\\\"},{\\\"status\\\":\\\"ERROR\\\",\\\"metricKey\\\":\\\"new_coverage\\\",\\\"comparator\\\":\\\"LT\\\",\\\"periodIndex\\\":1,\\\"errorThreshold\\\":\\\"80\\\",\\\"actualValue\\\":\\\"0.0\\\"},{\\\"status\\\":\\\"ERROR\\\",\\\"metricKey\\\":\\\"new_duplicated_lines_density\\\",\\\"comparator\\\":\\\"GT\\\",\\\"periodIndex\\\":1,\\\"errorThreshold\\\":\\\"3\\\",\\\"actualValue\\\":\\\"3.874813710879285\\\"}],\\\"periods\\\":[{\\\"index\\\":1,\\\"mode\\\":\\\"previous_version\\\",\\\"date\\\":\\\"2020-09-28T08:10:52+0000\\\",\\\"parameter\\\":\\\"efb771002ed7c4991216cecc2d9b014cb53a1a1d\\\"}],\\\"ignoredConditions\\\":false}}\",\"sonarResult\":\"ERROR\",\"startTime\":1601280800580,\"version\":\"8f7b2f98951ba85bca4fc1dbd57dd361531d9020\"}";
        String str = "模块：cmsserver\r\n责任人：肖少辉";
        String[] arr = str.split("\\n");
        JenkinsServer jenkins = getJenKinsServer();
        Map<String, Job> jobs = jenkins.getJobs();
        Job j = jobs.get("cmsserver");
        JobWithDetails job = jenkins.getJob("cmsserver");
        BuildWithDetails details = job.getBuilds().get(job.getBuilds().size()-1).details();
        String output = details.getConsoleOutputText();
        System.out.println(output);
        String location = "http://jenkins.ci.com/job/cmsserver/";
        QueueReference ref = new QueueReference(location);
        QueueItem item = jenkins.getQueueItem(ref);
        jenkins.getBuild(item);
        jobs.forEach((k,v)->{System.out.println(k); System.out.println(v);});
    }

}
