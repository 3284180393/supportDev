package com.channelsoft.ccod.support.cmdb.ci.service.impl;

import com.channelsoft.ccod.support.cmdb.ci.po.BuildDetailPo;
import com.channelsoft.ccod.support.cmdb.ci.service.ICIService;
import com.channelsoft.ccod.support.cmdb.ci.service.IJenkinsService;
import com.channelsoft.ccod.support.cmdb.ci.service.ISonarqubeService;
import com.channelsoft.ccod.support.cmdb.dao.BuildDetailMapper;
import com.channelsoft.ccod.support.cmdb.utils.HttpRequestTools;
import com.google.gson.*;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @ClassName: CIServiceImpl
 * @Author: lanhb
 * @Description: ICIService接口实现类
 * @Date: 2020/9/27 21:03
 * @Version: 1.0
 */
@Service
public class CIServiceImpl implements ICIService {

    @Value("${debug}")
    private boolean debug;

    private final static Logger logger = LoggerFactory.getLogger(CIServiceImpl.class);

    private final static Gson gson = new Gson();

    @Autowired
    IJenkinsService jenkinsService;

    @Autowired
    ISonarqubeService sonarqubeService;

    @Autowired
    BuildDetailMapper buildDetailMapper;

    @Value("${ci.notify.default-receiver}")
    private String msgReceiver;

    @Value("${ci.notify.msg-send-type}")
    private String msgSendType;

    @Value("${ci.notify.msg-type}")
    private String msgType;

    @Value("${ci.notify.msg-send-url}")
    private String msgSendUrl;

    @Value("${ci.notify.default-msg-image-url}")
    private String msgImageUrl;

    @Value("${ci.jenkins.host-url}")
    private String jenkinsHostUrl;

    @Override
    public BuildDetailPo getJobBuildResult(String jobName) throws Exception {
        BuildDetailPo po = jenkinsService.getLastBuildDetails(jobName);
        po = getSonarCheckResult(po);
        return po;
    }

    private BuildDetailPo getSonarCheckResult(BuildDetailPo jenkinsResult) throws Exception
    {
        String sonarQubeCheckResult = sonarqubeService.getCheckResult(jenkinsResult.getJobName());
        jenkinsResult.setSonarOutput(sonarQubeCheckResult);
        JsonParser jp = new JsonParser();
        JsonObject jsonObj = jp.parse(sonarQubeCheckResult).getAsJsonObject();
        String sonarResult = jsonObj.get("projectStatus").getAsJsonObject().get("status").getAsString();
        jenkinsResult.setSonarResult(sonarResult);
        return jenkinsResult;
    }

    @Override
    public void createBuild(String jobName) throws Exception {
        JobWithDetails job = jenkinsService.getJobDetail(jobName);
        Assert.notNull(job, String.format("job %s not exist", jobName));
        List<Build> builds = new ArrayList<>();
        for(Build build : job.getBuilds()){
            if(build.details().isBuilding()){
                builds.add(build);
            }
        }
        Assert.isTrue(builds.size()==1, String.format("job %s has %d build ongoing", jobName, builds.size()));
        int number = builds.get(0).getNumber();
        long current = System.currentTimeMillis();
        new Thread(()->{
            try{
                BuildWithDetails details = jenkinsService.getBuildDetails(jobName, number);
                while(details.isBuilding()){
                    Thread.sleep(3000);
                    details = jenkinsService.getBuildDetails(jobName, number);
                    logger.info(String.format("job %s #%d build has used %d(s)", jobName, number, (System.currentTimeMillis()-current)/1000));
                }
                BuildDetailPo po = jenkinsService.getBuildResultFromDetail(job, details);
                po = getSonarCheckResult(po);
                logger.warn(String.format("job %s #%d build result is : %s", jobName, number, gson.toJson(po)));
                sendBuildResult(po);
                buildDetailMapper.insert(po);
            }
            catch (Exception ex) {
                logger.error(String.format("job %s #%d build error", jobName, number), ex);
            }
        }).start();
    }

    private void sendBuildResult(BuildDetailPo detail) throws Exception
    {
        boolean jenkSucc = "SUCCESS".equals(detail.getJenkinsResult()) ? true : false;
        boolean sonarSucc = "ERROR".equals(detail.getSonarResult()) ? false : true;
        boolean succ = jenkSucc & sonarSucc ? true : false;
        String title = String.format("%s#%d构建%s", detail.getJobName(), detail.getNumber(), succ ? "成功" : "失败");
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("版本:%s  \n  ", detail.getParameters().get("VERSION")));
        sb.append(String.format("### jenkins构建%s  \n  ", jenkSucc ? "成功" : "失败"));
        sb.append(String.format("### sonarqube检查%s  \n  ", sonarSucc ? "成功" : "未通过"));
        if(!sonarSucc){
            JsonParser jp = new JsonParser();
            JsonArray jsonArray = jp.parse(detail.getSonarOutput()).getAsJsonObject()
                    .get("projectStatus").getAsJsonObject().get("conditions").getAsJsonArray();
            jsonArray.forEach(e->{
                JsonObject o = e.getAsJsonObject();
                if(o != null && o.get("status") != null && !"OK".equals(o.get("status").getAsString())){
                    sb.append(gson.toJson(o)).append("\n");
                }

            });
        }
        Map<String, Object> params = new HashMap<>();
        params.put("msgType", this.msgType);
        params.put("sendtype", this.msgSendType);
        params.put("listSendto", !debug && StringUtils.isNotBlank(detail.getProjectLeader()) ? detail.getProjectLeader() : this.msgReceiver);
        params.put("msgSentFrom", "jenkins构建结果");
        params.put("msgContent", sb.toString());
        params.put("msgTitle", title);
        params.put("single_url", String.format("%s/job/%s/%d/console", jenkinsHostUrl, detail.getJobName(), detail.getNumber()));
        params.put("imgUrl", detail.getParameters().containsKey("DingDingImage") ? detail.getParameters().get("DingDingImage") : this.msgImageUrl);
        Map<String, Object> sendMsg = new HashMap<>();
        sendMsg.put("sendDDCorpMsg", params);
        Map<String, Object> groupMap = new HashMap<>();
        groupMap.put("deptName", "");
        sendMsg.put("querydept", groupMap);
        HttpRequestTools.httpPostRequest(this.msgSendUrl, sendMsg);
    }

    @Override
    public List<BuildDetailPo> queryBuildHistory(String jobName, String appName, String version, String startTime, String endTime) throws Exception{
        logger.debug(String.format("query build history for jobName=%s,appName=%s,version=%s,beginTime=%s,endTime=%s",
                jobName, appName, version, startTime, endTime));
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        Date start = null;
        Date end = null;
        if(StringUtils.isNotBlank(startTime)) {
            try{
                start = sf.parse(String.format("%s000000", startTime));
            }
            catch (Exception ex) {
                throw new Exception(String.format("error startTime string, wanted format yyyyMMdd", startTime));
            }
        }
        if(StringUtils.isNotBlank(endTime)) {
            try{
                end = sf.parse(String.format("%s235959", endTime));
            }
            catch (Exception ex)
            {
                throw new Exception(String.format("error endTime string, wanted format yyyyMMdd", endTime));
            }
        }

        List<BuildDetailPo> list = buildDetailMapper.select(jobName, appName, version, start, end);
        logger.info(String.format("search %d record", list.size()));
        return list;
    }

    private void someTest() throws Exception
    {
        String json = "{\"appName\":\"cmsserver\",\"endTime\":1601280955287,\"id\":0,\"jenkinsResult\":\"SUCCESS\",\"jobName\":\"cmsserver\",\"number\":7,\"projectLeader\":\"肖少辉\",\"queueId\":3848,\"sonarOutput\":\"{\\\"projectStatus\\\":{\\\"status\\\":\\\"ERROR\\\",\\\"conditions\\\":[{\\\"status\\\":\\\"OK\\\",\\\"metricKey\\\":\\\"new_reliability_rating\\\",\\\"comparator\\\":\\\"GT\\\",\\\"periodIndex\\\":1,\\\"errorThreshold\\\":\\\"1\\\",\\\"actualValue\\\":\\\"1\\\"},{\\\"status\\\":\\\"OK\\\",\\\"metricKey\\\":\\\"new_security_rating\\\",\\\"comparator\\\":\\\"GT\\\",\\\"periodIndex\\\":1,\\\"errorThreshold\\\":\\\"1\\\",\\\"actualValue\\\":\\\"1\\\"},{\\\"status\\\":\\\"OK\\\",\\\"metricKey\\\":\\\"new_maintainability_rating\\\",\\\"comparator\\\":\\\"GT\\\",\\\"periodIndex\\\":1,\\\"errorThreshold\\\":\\\"1\\\",\\\"actualValue\\\":\\\"1\\\"},{\\\"status\\\":\\\"ERROR\\\",\\\"metricKey\\\":\\\"new_coverage\\\",\\\"comparator\\\":\\\"LT\\\",\\\"periodIndex\\\":1,\\\"errorThreshold\\\":\\\"80\\\",\\\"actualValue\\\":\\\"0.0\\\"},{\\\"status\\\":\\\"ERROR\\\",\\\"metricKey\\\":\\\"new_duplicated_lines_density\\\",\\\"comparator\\\":\\\"GT\\\",\\\"periodIndex\\\":1,\\\"errorThreshold\\\":\\\"3\\\",\\\"actualValue\\\":\\\"3.874813710879285\\\"}],\\\"periods\\\":[{\\\"index\\\":1,\\\"mode\\\":\\\"previous_version\\\",\\\"date\\\":\\\"2020-09-28T08:10:52+0000\\\",\\\"parameter\\\":\\\"efb771002ed7c4991216cecc2d9b014cb53a1a1d\\\"}],\\\"ignoredConditions\\\":false}}\",\"sonarResult\":\"ERROR\",\"startTime\":1601280800580,\"version\":\"8f7b2f98951ba85bca4fc1dbd57dd361531d9020\"}";
//        BuildDetailPo detail = gson.fromJson(json, BuildDetailPo.class);
        BuildDetailPo detail = new BuildDetailPo();
        detail.setJenkinsResult("SUCCESS");
        detail.setSonarResult("ERROR");
        json = "{\"projectStatus\":{\"status\":\"ERROR\",\"conditions\":[{\"status\":\"OK\",\"metricKey\":\"new_reliability_rating\",\"comparator\":\"GT\",\"periodIndex\":1,\"errorThreshold\":\"1\",\"actualValue\":\"1\"},{\"status\":\"ERROR\",\"metricKey\":\"new_security_rating\",\"comparator\":\"GT\",\"periodIndex\":1,\"errorThreshold\":\"1\",\"actualValue\":\"2\"},{\"status\":\"OK\",\"metricKey\":\"new_maintainability_rating\",\"comparator\":\"GT\",\"periodIndex\":1,\"errorThreshold\":\"1\",\"actualValue\":\"1\"},{\"status\":\"ERROR\",\"metricKey\":\"new_coverage\",\"comparator\":\"LT\",\"periodIndex\":1,\"errorThreshold\":\"80\",\"actualValue\":\"0.0\"},{\"status\":\"OK\",\"metricKey\":\"new_duplicated_lines_density\",\"comparator\":\"GT\",\"periodIndex\":1,\"errorThreshold\":\"3\",\"actualValue\":\"1.4722536806342015\"}],\"periods\":[{\"index\":1,\"mode\":\"previous_version\",\"date\":\"2020-09-25T10:38:50+0000\",\"parameter\":\"b4a6a0a76279af35ffc0833519afaf3dd4262ca8\"}],\"ignoredConditions\":false}}";
        detail.setSonarOutput(json);
        sendBuildResult(detail);
    }

    @Test
    public void doTest()
    {
        try{
            someTest();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

}