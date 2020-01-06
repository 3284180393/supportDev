package com.channelsoft.ccod.support.cmdb.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.constant.AppOperationMethod;
import com.channelsoft.ccod.support.cmdb.constant.PlatformAppOperationMethod;
import com.channelsoft.ccod.support.cmdb.constant.PlatformUpdateTaskType;
import com.channelsoft.ccod.support.cmdb.po.AjaxResultPo;
import com.channelsoft.ccod.support.cmdb.po.AppPo;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import com.channelsoft.ccod.support.cmdb.service.ILJPaasService;
import com.channelsoft.ccod.support.cmdb.service.IPlatformResourceService;
import com.channelsoft.ccod.support.cmdb.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


/**
 * @ClassName: CMDBController
 * @Author: lanhb
 * @Description: 用来提供cmddb的http查询接口
 * @Date: 2019/11/25 20:03
 * @Version: 1.0
 */
@RestController
@RequestMapping("/cmdb/api")
public class CMDBController {

    @Autowired
    IAppManagerService appManagerService;

    @Autowired
    IPlatformResourceService platformResourceService;

    @Autowired
    ILJPaasService ljPaasService;

    private final static Logger logger = LoggerFactory.getLogger(CMDBController.class);

    private String apiBasePath = "/cmdb/api";


//    @RequestMapping(value = "/apps", method = RequestMethod.POST)
//    public AjaxResultPo addNewApp(@RequestBody AppParamVo param)
//    {
//        String uri = String.format("POST %s/apps", this.apiBasePath);
//        logger.debug(String.format("enter %s controller and app param=%s", uri, JSONObject.toJSONString(param)));
//        if(param.getMethod() != AppOperationMethod.ADD_BY_SCAN_NEXUS_REPOSITORY.id)
//        {
//            logger.error(String.format("not not support method=%d app operation", param.getMethod()));
//            return new AjaxResultPo(false, String.format("not not support method=%d app operation", param.getMethod()));
//        }
//        AjaxResultPo resultPo;
//        try
//        {
//            String data = param.getData().toString();
//            JSONObject jsonObject = JSONObject.parseObject(data);
//            AppModuleFileNexusInfo instPkg = JSONObject.parseObject(jsonObject.get("installPackage").toString(), AppModuleFileNexusInfo.class);
//            List<AppModuleFileNexusInfo> cfgs = JSONArray.parseArray(jsonObject.get("cfgs").toString(), AppModuleFileNexusInfo.class);
//            AppPo appPo = this.appManagerService.addNewAppFromPublishNexus(param.getAppType(), param.getAppName(),
//                    param.getAppAlias(), param.getVersion(), param.getCcodVersion(), instPkg,
//                    cfgs.toArray(new AppModuleFileNexusInfo[0]), param.getBasePath());
//            logger.info(String.format("query SUCCESS add app=%s, quit %s", JSONObject.toJSONString(appPo), uri));
//            resultPo = new AjaxResultPo(true, "add new app SUCCESS");
//        }
//        catch (Exception e)
//        {
//            logger.error(String.format("query app modules exception, quit %s controller", uri), e);
//            resultPo = AjaxResultPo.failed(e);
//        }
//        return resultPo;
//    }


    @RequestMapping(value = "/apps", method = RequestMethod.POST)
    public AjaxResultPo addNewApp(@RequestBody AppModuleVo moduleVo)
    {
        String uri = String.format("POST %s/apps", this.apiBasePath);
        logger.debug(String.format("enter %s controller and app param=%s", uri, JSONObject.toJSONString(moduleVo)));
        AjaxResultPo resultPo;
        try
        {
            this.appManagerService.registerNewAppModule(moduleVo);
            logger.info(String.format("register %s SUCCESS, quit %s", JSONObject.toJSONString(moduleVo), uri));
            resultPo = new AjaxResultPo(true, "register new app SUCCESS");
        }
        catch (Exception e)
        {
            logger.error(String.format("register app module exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/apps", method = RequestMethod.GET)
    public AjaxResultPo queryAllApps()
    {
        String uri = String.format("GET %s/apps", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            AppModuleVo[] apps = this.appManagerService.queryApps(null);
            resultPo = new AjaxResultPo(true, "query SUCCESs", apps.length, apps);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query app modules exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }


    @RequestMapping(value = "/apps/{appName}", method = RequestMethod.GET)
    public AjaxResultPo queryAppsByName(@PathVariable String appName)
    {
        String uri = String.format("GET %s/apps/%s", this.apiBasePath, appName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            AppModuleVo[] apps = this.appManagerService.queryApps(appName);
            resultPo = new AjaxResultPo(true, "query SUCCESs", apps.length, apps);
            logger.info(String.format("query SUCCESS, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query app modules exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/apps/{appName}/{version}")
    public AjaxResultPo queryAppByNameAndVersion(@PathVariable String appName, @PathVariable String version)
    {
        String uri = String.format("GET %s/apps/%s/%s", this.apiBasePath, appName, version);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            AppModuleVo moduleVo = this.appManagerService.queryAppByVersion(appName, version);
            if(moduleVo != null)
            {
                resultPo = new AjaxResultPo(true, "query SUCCESs", 1, moduleVo);
                logger.info(String.format("query SUCCESS, quit %s controller", uri));
            }
            else
            {
                logger.error(String.format("not find app module with appName=%s and version=%s, quit %s controller",
                        appName, version, uri));
                resultPo = new AjaxResultPo(false, String.format("not find app module with appName=%s and version=%s",
                        appName, version));
            }
        }
        catch (Exception e)
        {
            logger.error(String.format("query app module exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/createPlatformAppDataCollectTask")
    public AjaxResultPo createPlatformAppDataCollectTask(String platformId, String platformName)
    {
        AjaxResultPo resultPo;
        try
        {
            this.appManagerService.createNewPlatformAppDataCollectTask(platformId, platformName,null,null, null, null);
            resultPo = new AjaxResultPo(true, "app data task create success");
        }
        catch (Exception ex)
        {
            logger.error(String.format("create %s app data collect task exception", platformId), ex);
            resultPo = AjaxResultPo.failed(ex);
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformApps", method = RequestMethod.GET)
    public AjaxResultPo queryAllPlatformApps()
    {
        String uri = String.format("GET %s/platformApps", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformAppDeployDetailVo[] details = this.appManagerService.queryPlatformApps(null, null, null);
            resultPo = new AjaxResultPo(true, "query SUCCESS", details.length, details);
            logger.info(String.format("query SUCCESS, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platform apps exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformApps", method = RequestMethod.POST)
    public AjaxResultPo addPlatformApps(@RequestBody PlatformAppParamVo param)
    {
        String uri = String.format("POST %s/platformApps", this.apiBasePath);
        logger.debug(String.format("enter %s controller with param=%s", uri, JSONObject.toJSONString(param)));
        if(param.getMethod() != PlatformAppOperationMethod.ADD_BY_PLATFORM_CLIENT_COLLECT.id)
        {
            logger.error(String.format("not support platform app operation method=%d, quit %s controller",
                    param.getMethod(), uri));
            return new AjaxResultPo(false, String.format("not support platform app operation method=%d", param.getMethod()));
        }
        if(StringUtils.isBlank(param.getPlatformId()))
        {
            logger.error(String.format("platformId of app operation method=%d cannot be blank, quit %s controller",
                    param.getMethod(), uri));
            return new AjaxResultPo(false, String.format("platformId can not be blank"));
        }
        AjaxResultPo resultPo;
        try
        {
            this.appManagerService.createNewPlatformAppDataCollectTask(param.getPlatformId(), param.getPlatformName(), param.getDomainId(),
                    param.getHostIp(), param.getAppName(), param.getVersion());
            logger.info(String.format("platform app collect task with param=%s create SUCCESS, quit %s controller",
                    JSONObject.toJSONString(param), uri));
            resultPo = new AjaxResultPo(true, "collect task create SUCCESS");
        }
        catch (Exception e)
        {
            logger.error(String.format("query platform apps exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/platformApps/{platformId}")
    public AjaxResultPo queryPlatformAppsByPlatformId(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/platformApps/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformAppDeployDetailVo[] details = this.appManagerService.queryPlatformApps(platformId, null, null);
            resultPo = new AjaxResultPo(true, "query SUCCESS", details.length, details);
            logger.info(String.format("query SUCCESS, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platform apps exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/platformApps/{platformId}/{domainId}")
    public AjaxResultPo queryPlatformAppsByDomainId(@PathVariable String platformId, @PathVariable String domainId)
    {
        String uri = String.format("GET %s/platformApps/%s/%s", this.apiBasePath, platformId, domainId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformAppDeployDetailVo[] details = this.appManagerService.queryPlatformApps(platformId, domainId, null);
            resultPo = new AjaxResultPo(true, "query SUCCESS", details.length, details);
            logger.info(String.format("query SUCCESS, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platform apps exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/platformApps/{platformId}/{domainId}/{hostIp}")
    public AjaxResultPo queryPlatformAppsByHostIp(@PathVariable String platformId, @PathVariable String domainId, @PathVariable String hostIp)
    {
        String uri = String.format("GET %s/platformApps/%s/%s/%s", this.apiBasePath, platformId, domainId, hostIp);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformAppDeployDetailVo[] details = this.appManagerService.queryPlatformApps(platformId, domainId, hostIp);
            resultPo = new AjaxResultPo(true, "query SUCCESS", details.length, details);
            logger.info(String.format("query SUCCESS, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platform apps exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/platformResources")
    public AjaxResultPo queryAllPlatformResources()
    {
        String uri = String.format("GET %s/platformResources", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformResourceVo[] resources = this.platformResourceService.queryPlatformResources();
            resultPo = new AjaxResultPo(true, "query SUCCESS", resources.length, resources);
            logger.info(String.format("query SUCCESS, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platform resource exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/platformResources/{platformId}")
    public AjaxResultPo queryPlatformResourceByPlatformId(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/platformResources/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformResourceVo resource = this.platformResourceService.queryPlatformResource(platformId, null, null);
            if(resource != null)
            {
                resultPo = new AjaxResultPo(true, "query SUCCESS", 1, resource);
                logger.info(String.format("query SUCCESS, quit %s controller", uri));
            }
            else
            {
                logger.error(String.format("not find platform resource, quit %s controller", platformId, uri));
                resultPo = new AjaxResultPo(false, String.format("not find resource with platformId=%s", platformId));
            }

        }
        catch (Exception e)
        {
            logger.error(String.format("query platform resource exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/platformResources/{platformId}/{domainId}")
    public AjaxResultPo queryPlatformResourceByDomainId(@PathVariable String platformId, @PathVariable String domainId)
    {
        String uri = String.format("GET %s/platformResources/%s/%s", this.apiBasePath, platformId, domainId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformResourceVo resource = this.platformResourceService.queryPlatformResource(platformId, domainId, null);
            if(resource != null)
            {
                logger.info(String.format("query SUCCESS, quit %s controller", uri));
                resultPo = new AjaxResultPo(true, "query SUCCESS", 1, resource);
            }
            else
            {
                logger.error(String.format("not find platform resource, quit %s controller", uri));
                resultPo = new AjaxResultPo(false, String.format("not find resource with platformId=%s and domainId=%s",
                        platformId, domainId));
            }

        }
        catch (Exception e)
        {
            logger.error(String.format("query platform resource exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/platformResources/{platformId}/{domainId}/{hostIp}")
    public AjaxResultPo queryPlatformResourceByHostIp(@PathVariable String platformId, @PathVariable String domainId, @PathVariable String hostIp)
    {
        String uri = String.format("GET %s/platformResources/%s/%s/%s", this.apiBasePath, platformId, domainId, hostIp);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformResourceVo resource = this.platformResourceService.queryPlatformResource(platformId, domainId, hostIp);
            if(resource != null)
            {
                logger.info(String.format("query SUCCESS, quit %s controller", uri));
                resultPo = new AjaxResultPo(true, "query SUCCESS", 1, resource);
            }
            else
            {
                logger.error(String.format("not find platform resource, quit %s controller", uri));
                resultPo = new AjaxResultPo(false, String.format("not find resource with platformId=%s and domainId=%s and hostIp=%s",
                        platformId, domainId, hostIp));
            }

        }
        catch (Exception e)
        {
            logger.error(String.format("query platform resource exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/appDeployDetails")
    public AjaxResultPo queryAllAppsDeployDetails()
    {
        String uri = String.format("GET %s/appDeployDetails", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformAppDeployDetailVo[] details = this.appManagerService.queryAppDeployDetails(null, null, null, null);
            resultPo = new AjaxResultPo(true, "query SUCCESS", details.length, details);
            logger.info(String.format("query SUCCESS, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query apps deploy details exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/appDeployDetails/{appName}")
    public AjaxResultPo queryAppDeployDetailByAppName(@PathVariable String appName)
    {
        String uri = String.format("GET %s/appDeployDetails/%s", this.apiBasePath, appName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformAppDeployDetailVo[] details = this.appManagerService.queryAppDeployDetails(appName, null, null, null);
            resultPo = new AjaxResultPo(true, "query SUCCESS", details.length, details);
            logger.info(String.format("query SUCCESS, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query apps deploy details exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/appDeployDetails/{appName}/{platformId}")
    public AjaxResultPo queryAppDeployDetailByPlatformId(@PathVariable String appName, @PathVariable String platformId)
    {
        String uri = String.format("GET %s/appDeployDetails/%s/%s", this.apiBasePath, appName, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformAppDeployDetailVo[] details = this.appManagerService.queryAppDeployDetails(appName, platformId, null, null);
            resultPo = new AjaxResultPo(true, "query SUCCESS", details.length, details);
            logger.info(String.format("query SUCCESS, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query apps deploy details exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/appDeployDetails/{appName}/{platformId}/{domainId}")
    public AjaxResultPo queryAppDeployDetailsByDomainId(@PathVariable String appName, @PathVariable String platformId, @PathVariable String domainId)
    {
        String uri = String.format("GET %s/appDeployDetails/%s/%s/%s", this.apiBasePath, appName, platformId, domainId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformAppDeployDetailVo[] details = this.appManagerService.queryAppDeployDetails(appName, platformId, domainId, null);
            resultPo = new AjaxResultPo(true, "query SUCCESS", details.length, details);
            logger.info(String.format("query SUCCESS, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query apps deploy details exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/appDeployDetails/{appName}/{platformId}/{domainId}/{hostIp}")
    public AjaxResultPo queryAppDeployDetailsByHostIp(@PathVariable String appName, @PathVariable String platformId, @PathVariable String domainId, @PathVariable String hostIp)
    {
        String uri = String.format("GET %s/appDeployDetails/%s/%s/%s/%s", this.apiBasePath, appName, platformId, domainId, hostIp);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformAppDeployDetailVo[] details = this.appManagerService.queryAppDeployDetails(appName, platformId, domainId, hostIp);
            resultPo = new AjaxResultPo(true, "query SUCCESS", details.length, details);
            logger.info(String.format("query SUCCESS, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query apps deploy details exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/businesses", method = RequestMethod.GET)
    public AjaxResultPo queryAllCCODBiz()
    {
        String uri = String.format("GET %s/businesses", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<CCODPlatformInfo> bizPlatforms = this.ljPaasService.queryCCODBiz(null, null, null);
//            String data = JSONObject.toJSONString(bizPlatforms);
//            List<CCODPlatformInfo> resultList = new ArrayList<>();
//            for(CCODPlatformInfo platformInfo : bizPlatforms)
//            {
//                resultList.add(platformInfo);
//            }
//            resultPo = new AjaxResultPo(true, "query SUCCESS", resultList.size(), resultList);
//            resultPo = new AjaxResultPo(true, "query SUCESS", 1, data);
//            resultPo = new AjaxResultPo(true, "test");
            resultPo = new AjaxResultPo(true, "query SUCCESS", bizPlatforms.size(), bizPlatforms);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query ccod biz platforms exception, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/businesses/{bizId}", method = RequestMethod.GET)
    public AjaxResultPo queryCCODBizByBizId(@PathVariable int bizId)
    {
        String uri = String.format("GET %s/businesses/%d", this.apiBasePath, bizId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<CCODPlatformInfo> bizPlatforms = this.ljPaasService.queryCCODBiz(bizId, null, null);
            resultPo = new AjaxResultPo(true, "query SUCCESS", bizPlatforms.size(), bizPlatforms);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query ccod biz platforms exception, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/businesses/{bizId}/{setId}", method = RequestMethod.GET)
    public AjaxResultPo queryCCODBizBySetId(@PathVariable int bizId, @PathVariable String setId)
    {
        String uri = String.format("GET %s/businesses/%d/%s", this.apiBasePath, bizId, setId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<CCODPlatformInfo> bizPlatforms = this.ljPaasService.queryCCODBiz(bizId, setId, null);
            resultPo = new AjaxResultPo(true, "query SUCCESS", bizPlatforms.size(), bizPlatforms);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query ccod biz platforms exception, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/businesses/{bizId}/{setId}/{domainId}", method = RequestMethod.GET)
    public AjaxResultPo queryCCODBizByDomainId(@PathVariable int bizId, @PathVariable String setId, @PathVariable String domainId)
    {
        String uri = String.format("GET %s/businesses/%d/%s/%s", this.apiBasePath, bizId, setId, domainId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<CCODPlatformInfo> bizPlatforms = this.ljPaasService.queryCCODBiz(bizId, setId, domainId);
            resultPo = new AjaxResultPo(true, "query SUCCESS", bizPlatforms.size(), bizPlatforms);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query ccod biz platforms exception, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }


    @RequestMapping(value = "/sets", method = RequestMethod.GET)
    public AjaxResultPo queryAllCCODBizSet()
    {
        String uri = String.format("GET %s/sets", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<BizSetDefine> setDefines = this.ljPaasService.queryCCODBizSet(true);
            resultPo = new AjaxResultPo(true, "query SUCCESS", setDefines.size(), setDefines);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query sets of ccod biz, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformUpdateSchemas", method = RequestMethod.GET)
    public AjaxResultPo getAllPlatformUpdateSchemas()
    {
        String uri = String.format("GET %s/platformUpdateSchemas", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<PlatformUpdateSchemaInfo> schemaInfos = this.appManagerService.queryPlatformUpdateSchema(null);
            resultPo = new AjaxResultPo(true, "query SUCCESS", schemaInfos.size(), schemaInfos);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platformUpdateSchema exception, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformUpdateSchemas/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo getAllPlatformUpdateSchemaByPlatformId(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/platformUpdateSchemas/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<PlatformUpdateSchemaInfo> schemaInfos = this.appManagerService.queryPlatformUpdateSchema(platformId);
            resultPo = new AjaxResultPo(true, "query SUCCESS", schemaInfos.size(), schemaInfos);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platformUpdateSchema exception, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformUpdateSchema", method = RequestMethod.POST)
    public AjaxResultPo updatePlatformUpdateSchema(@RequestBody PlatformUpdateSchemaInfo schema)
    {
        String uri = String.format("POST %s/platformUpdateSchema", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            appManagerService.updatePlatformUpdateSchema(schema);
            resultPo = new AjaxResultPo(true, "update schema success", 1, null);
        }
        catch (Exception e)
        {
            logger.error(String.format("create demo schema exception"), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformUpdateSchemaDemo", method = RequestMethod.POST)
    public AjaxResultPo createDemoPlatformUpdateSchema(@RequestBody PlatformUpdateSchemaParamVo param)
    {
        String uri = String.format("POST %s/platformUpdateSchemaParamDemo", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        PlatformUpdateSchemaInfo schemaDemo;
        try
        {
            switch (param.getTaskType())
            {
                case CREATE:
                    schemaDemo = appManagerService.createPlatformUpdateSchemaDemo(param);
                    resultPo = new AjaxResultPo(true, "create demo schema success", 1, schemaDemo);
                    break;
                case UPDATE:
                    schemaDemo = appManagerService.createDemoUpdatePlatform(param.getPlatformId(), param.getPlatformName(), param.getBkBizId());
                    resultPo = new AjaxResultPo(true, "create demo schema success", 1, schemaDemo);
                    break;
                default:
                    resultPo = new AjaxResultPo(false, String.format("create demo schema fail : %s not support", param.getTaskType().name));
                    break;
            }
        }
        catch (Exception e)
        {
            logger.error(String.format("create demo schema exception"), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformTopologies", method = RequestMethod.GET)
    public AjaxResultPo getAllPlatformTopology()
    {
        String uri = String.format("GET %s/platformTopologies", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<PlatformTopologyInfo> platformList = this.appManagerService.queryAllPlatformTopology();
            resultPo = new AjaxResultPo(true, "query SUCCESS", platformList.size(), platformList);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platformTopologies exception, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformTopologies/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo getPlatformTopologyByPlatformId(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/platformTopologies/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformTopologyInfo topology = this.appManagerService.getPlatformTopology(platformId);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, topology);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platformTopologies exception, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/demoPlatforms", method = RequestMethod.POST)
    public AjaxResultPo createDemoPlatform(@RequestBody PlatformUpdateSchemaParamVo param)
    {
        String uri = String.format("POST %s/demoPlatform", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            switch (param.getTaskType())
            {
                case CREATE:
                    appManagerService.createDemoNewPlatform(param.getPlatformId(), param.getPlatformName(), param.getBkBizId(), param.getBkCloudId(), param.getPlanAppList());
                    logger.info(String.format("demo new create platform %s create SUCCESS", param.getPlatformId()));
                    resultPo = new AjaxResultPo(true, String.format("demo new create platform %s create SUCCESS", param.getPlatformId()));
                    break;
                case UPDATE:
                    appManagerService.createDemoUpdatePlatform(param.getPlatformId(), param.getPlatformName(), param.getBkBizId());
                    logger.info(String.format("demo new update platform %s create SUCCESS", param.getPlatformId()));
                    resultPo = new AjaxResultPo(true, "demo update platform create success");
                    break;
                default:
                    logger.error(String.format("demo %s platform not been implement", param.getTaskType().name));
                    resultPo = new AjaxResultPo(false, String.format("demo %s platform not been implement", param.getTaskType().name));
                    break;
            }
        }
        catch (Exception e)
        {
            logger.error(String.format("create demo platform exception"), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }
}
