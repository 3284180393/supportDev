package com.channelsoft.ccod.support.cmdb.controller;

import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.po.AjaxResultPo;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppDeployDetailVo;
import com.channelsoft.ccod.support.cmdb.vo.QueryEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @ClassName: CMDBController
 * @Author: lanhb
 * @Description: 用来提供cmddb的http查询接口
 * @Date: 2019/11/25 20:03
 * @Version: 1.0
 */
@RestController
@RequestMapping("/cmdb")
public class CMDBController {

    @Autowired
    IAppManagerService appManagerService;

    private final static Logger logger = LoggerFactory.getLogger(CMDBController.class);

    @RequestMapping("/appModules")
    public AjaxResultPo queryAppModule()
    {
        QueryEntity queryEntity = new QueryEntity();
        logger.debug(String.format("enter %s : queryEntity=%s", "/appModules", JSONObject.toJSONString(queryEntity)));
        AjaxResultPo resultPo;
        try
        {
            AppModuleVo[] apps = this.appManagerService.queryAppModules(queryEntity);
            resultPo = new AjaxResultPo(true, "query SUCCESs", apps.length, apps);
            logger.debug(String.format("query SUCCESS, quit %s", "/appModules"));
        }
        catch (Exception e)
        {
            logger.error(String.format("query [] app modules exception", JSONObject.toJSONString(queryEntity)), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/platformApp")
    public AjaxResultPo queryPlatformAppDeploy()
    {
        QueryEntity queryEntity = new QueryEntity();
        String uri = "/platformApp";
        logger.debug(String.format("enter %s : queryEntity=%s", uri, JSONObject.toJSONString(queryEntity)));
        AjaxResultPo resultPo;
        try
        {
            PlatformAppDeployDetailVo[] apps = this.appManagerService.queryPlatformAppDeploy(queryEntity);
            resultPo = new AjaxResultPo(true, "query SUCCESs", apps.length, apps);
            logger.debug(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query [] platform app deploy exception", JSONObject.toJSONString(queryEntity)), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }
}
