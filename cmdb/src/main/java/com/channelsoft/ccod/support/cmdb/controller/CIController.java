package com.channelsoft.ccod.support.cmdb.controller;

import com.channelsoft.ccod.support.cmdb.ci.po.BuildDetailPo;
import com.channelsoft.ccod.support.cmdb.ci.service.ICIService;
import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.po.AjaxResultPo;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @ClassName: CIController
 * @Author: lanhb
 * @Description: 用来定义ci相关http接口
 * @Date: 2020/9/27 20:59
 * @Version: 1.0
 */
@RestController
@RequestMapping("/cmdb/api/ci")
public class CIController {

    private final static Logger logger = LoggerFactory.getLogger(CIController.class);

    private final String apiBasePath = "/cmdb/api/ci";

    @Autowired
    ICIService ciService;

    @Autowired
    IAppManagerService appManagerService;

    @RequestMapping(value = "/builds/{jobName}", method = RequestMethod.GET)
    public AjaxResultPo queryBuild(@PathVariable String jobName)
    {
        AjaxResultPo resultPo;
        try {
            BuildDetailPo detail = ciService.getJobBuildResult(jobName);
            resultPo = new AjaxResultPo(true, "query build result success", 1, detail);
        }
        catch (Exception ex)
        {
            logger.error("query build result FAIL", ex);
            resultPo = AjaxResultPo.failed(ex);
        }
        return resultPo;
    }

    @RequestMapping(value = "/builds/{jobName}", method = RequestMethod.POST)
    public AjaxResultPo createBuild(@PathVariable String jobName)
    {
        AjaxResultPo resultPo;
        try {
            ciService.createBuild(jobName);
            resultPo = new AjaxResultPo(true, "build create success");
        }
        catch (Exception ex)
        {
            logger.error("create build history FAIL", ex);
            resultPo = AjaxResultPo.failed(ex);
        }
        return resultPo;
    }

    @RequestMapping(value = "/buildHistory", method = RequestMethod.GET)
    public AjaxResultPo queryBuildHistory(String startTime, String endTime)
    {
        String uri = String.format("GET %s/buildHistory, startTime=%s and endTime=%s", this.apiBasePath, startTime, endTime);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try {
            List<BuildDetailPo> details = ciService.queryBuildHistory(null, null, null, startTime, endTime);
            resultPo = new AjaxResultPo(true, "query build history success", details.size(), details);
        }
        catch (Exception ex)
        {
            logger.error("query build history FAIL", ex);
            resultPo = AjaxResultPo.failed(ex);
        }
        return resultPo;
    }

    @RequestMapping(value = "/buildHistory/{appName}", method = RequestMethod.GET)
    public AjaxResultPo queryAppBuildHistory(@PathVariable String appName, String startTime, String endTime)
    {
        String uri = String.format("GET %s/buildHistory/%s, startTime=%s and endTime=%s", this.apiBasePath, appName, startTime, endTime);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try {
            List<BuildDetailPo> details = ciService.queryBuildHistory(null, appName, null, startTime, endTime);
            resultPo = new AjaxResultPo(true, "query build history success", details.size(), details);
        }
        catch (Exception ex)
        {
            logger.error("query build history FAIL", ex);
            resultPo = AjaxResultPo.failed(ex);
        }
        return resultPo;
    }

    @RequestMapping(value = "/buildHistory/{appName}/{version}", method = RequestMethod.GET)
    public AjaxResultPo queryBuildAppHistoryForVersion(@PathVariable String appName, @PathVariable String version, String startTime, String endTime)
    {
        String uri = String.format("GET %s/buildHistory/%s/%s, startTime=%s and endTime=%s", this.apiBasePath, appName, version, startTime, endTime);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try {
            List<BuildDetailPo> details = ciService.queryBuildHistory(null, appName, version, startTime, endTime);
            resultPo = new AjaxResultPo(true, "query build history success", details.size(), details);
        }
        catch (Exception ex)
        {
            logger.error(String.format("query build history FAIL"), ex);
            resultPo = AjaxResultPo.failed(ex);
        }
        return resultPo;
    }

    @RequestMapping(value = "/app", method = RequestMethod.POST)
    public String registerModule(String appName, AppType appType, String version, String ccodVersion, String gitUrl, String repository, String path)
    {
        try {
            appManagerService.registerCIAppModule(appName, appType, version, ccodVersion, gitUrl, repository, path);
            return "0";
        }
        catch (Exception ex)
        {
            logger.error(String.format("register app module from ci FAIL"), ex);
            return "1";
        }
    }
}
