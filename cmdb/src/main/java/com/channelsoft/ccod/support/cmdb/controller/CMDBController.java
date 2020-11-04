package com.channelsoft.ccod.support.cmdb.controller;

import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.config.GsonDateUtil;
import com.channelsoft.ccod.support.cmdb.constant.DomainUpdateType;
import com.channelsoft.ccod.support.cmdb.constant.PlatformDeployStatus;
import com.channelsoft.ccod.support.cmdb.constant.PlatformFunction;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.po.AjaxResultPo;
import com.channelsoft.ccod.support.cmdb.po.AppDebugDetailPo;
import com.channelsoft.ccod.support.cmdb.po.K8sOperationPo;
import com.channelsoft.ccod.support.cmdb.po.PlatformUpdateRecordPo;
import com.channelsoft.ccod.support.cmdb.service.*;
import com.channelsoft.ccod.support.cmdb.vo.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.kubernetes.client.openapi.models.*;
import javafx.application.Platform;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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
    IPlatformManagerService platformManagerService;

    @Autowired
    IPlatformResourceService platformResourceService;

    @Autowired
    ILJPaasService ljPaasService;

    @Autowired
    IPlatformAppCollectService platformAppCollectService;

    @Autowired
    IK8sApiService k8sApiService;

    private final static Logger logger = LoggerFactory.getLogger(CMDBController.class);

    private String apiBasePath = "/cmdb/api";

    private final static Gson gson = new GsonBuilder().registerTypeAdapter(DateTime.class, new GsonDateUtil()).disableHtmlEscaping().create();

    @RequestMapping(value = "/apps", method = RequestMethod.POST)
    public AjaxResultPo addNewApp(@RequestBody AppModuleVo moduleVo)
    {
        String uri = String.format("POST %s/apps", this.apiBasePath);
        logger.debug(String.format("enter %s controller and app param=%s", uri, gson.toJson(moduleVo)));
        AjaxResultPo resultPo;
        try
        {
            this.appManagerService.registerNewAppModule(moduleVo);
            logger.info(String.format("register %s SUCCESS, quit %s", gson.toJson(moduleVo), uri));
            resultPo = new AjaxResultPo(true, "register new app SUCCESS");
        }
        catch (Exception e)
        {
            logger.error(String.format("register app module exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/appModules", method = RequestMethod.POST)
    public AjaxResultPo updateExistAppModule(@RequestBody AppModuleVo moduleVo)
    {
        String uri = String.format("POST %s/appModules", this.apiBasePath);
        logger.debug(String.format("enter %s controller and param=%s", uri, gson.toJson(moduleVo)));
        AjaxResultPo resultPo;
        try
        {
            this.appManagerService.updateAppModule(moduleVo);
            logger.info(String.format("modify %s cfg SUCCESS, quit %s", gson.toJson(moduleVo), uri));
            resultPo = new AjaxResultPo(true, "modify app register info SUCCESS");
        }
        catch (Exception e)
        {
            logger.error(String.format("modify app register info exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/apps", method = RequestMethod.GET)
    public AjaxResultPo queryAllApps(Boolean hasImage)
    {
        String uri = String.format("GET %s/apps", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<AppModuleVo> apps = this.appManagerService.queryAllRegisterAppModule(hasImage);;
            resultPo = new AjaxResultPo(true, "query SUCCESs", apps.size(), apps);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query app modules exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/supportApps/{appName}", method = RequestMethod.GET)
    public AjaxResultPo isSupportApp(@PathVariable String appName)
    {
        String uri = String.format("GET %s/supportApps/%s", this.apiBasePath, appName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            boolean isSupport = this.appManagerService.isSupport(appName);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, isSupport);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query app supported exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/appsFlush", method = RequestMethod.GET)
    public AjaxResultPo flushApps()
    {
        this.appManagerService.flushRegisteredApp();
        AjaxResultPo resultPo = new AjaxResultPo(true, "flush SUCCESS", 1, null);
        return resultPo;
    }

    @RequestMapping(value = "/apps/{appName}", method = RequestMethod.GET)
    public AjaxResultPo queryAppsByName(@PathVariable String appName, Boolean hasImage)
    {
        String uri = String.format("GET %s/apps/%s, hasImage=%s", this.apiBasePath, appName, hasImage);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<AppModuleVo> apps = this.appManagerService.queryApps(appName, hasImage);
            resultPo = new AjaxResultPo(true, "query SUCCESs", apps.size(), apps);
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
            AppModuleVo moduleVo = this.appManagerService.queryAppByVersion(appName, version, null);
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
    public AjaxResultPo createPlatformAppDataCollectTask(String platformId, String platformName, int bkBizId, int bkCloudId)
    {
        AjaxResultPo resultPo;
        try
        {
            this.appManagerService.createNewPlatformAppDataCollectTask(platformId, platformName,bkBizId,bkCloudId);
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

    @PutMapping(value="/platformApps")
//    @RequestMapping(value = "/platformApps", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
//    public AjaxResultPo updatePlatformApps( @RequestParam(value = "platformId") String platformId,  @RequestParam(value = "platformName") String platformName, @RequestParam(value = "updateList") List<PlatformAppDeployDetailVo> updateList)
    public AjaxResultPo updatePlatformApps(@RequestBody PlatformAppUpdateParamVo paramVo)
    {
        String uri = String.format("PUT %s/platformApps", this.apiBasePath);
        logger.debug(String.format("enter %s controller : param=[%s]", uri, gson.toJson(paramVo)));
        AjaxResultPo resultPo;
        try
        {
            this.platformManagerService.updatePlatformApps(paramVo.getPlatformId(), paramVo.getPlatformName(), paramVo.getUpdateAppList());
            resultPo = new AjaxResultPo(true, "update SUCCESS", 1, "good");
            logger.info(String.format("update SUCCESS, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platform apps exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }



//    @RequestMapping(value = "/platformApps", method = RequestMethod.POST)
//    public AjaxResultPo addPlatformApps(@RequestBody PlatformAppParamVo param)
//    {
//        String uri = String.format("POST %s/platformApps", this.apiBasePath);
//        logger.debug(String.format("enter %s controller with param=%s", uri, gson.toJson(param)));
//        if(param.getMethod() != PlatformAppOperationMethod.ADD_BY_PLATFORM_CLIENT_COLLECT.id)
//        {
//            logger.error(String.format("not support platform app operation method=%d, quit %s controller",
//                    param.getMethod(), uri));
//            return new AjaxResultPo(false, String.format("not support platform app operation method=%d", param.getMethod()));
//        }
//        if(StringUtils.isBlank(param.getPlatformId()))
//        {
//            logger.error(String.format("platformId of app operation method=%d cannot be blank, quit %s controller",
//                    param.getMethod(), uri));
//            return new AjaxResultPo(false, String.format("platformId can not be blank"));
//        }
//        AjaxResultPo resultPo;
//        try
//        {
//            this.appManagerService.createNewPlatformAppDataCollectTask(param.getPlatformId(), param.getPlatformName(), param.getDomainId(),
//                    param.getHostIp(), param.getAppName(), param.getVersion());
//            logger.info(String.format("platform app collect task with param=%s create SUCCESS, quit %s controller",
//                    gson.toJson(param), uri));
//            resultPo = new AjaxResultPo(true, "collect task create SUCCESS");
//        }
//        catch (Exception e)
//        {
//            logger.error(String.format("query platform apps exception, quit %s controller", uri), e);
//            resultPo = AjaxResultPo.failed(e);
//        }
//        return resultPo;
//    }

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

    @RequestMapping("/platformApps/{platformId}/{domainId}/{alias}")
    public AjaxResultPo queryPlatformAppsByHostIp(@PathVariable String platformId, @PathVariable String domainId, @PathVariable String alias)
    {
        String uri = String.format("GET %s/platformApps/%s/%s/%s", this.apiBasePath, platformId, domainId, alias);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformAppDeployDetailVo deployApp = this.platformManagerService.queryPlatformApp(platformId, domainId, alias);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, deployApp);
            logger.info(String.format("query SUCCESS, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platform apps exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformApps/{platformId}/{domainId}", method = RequestMethod.PUT)
    public AjaxResultPo debugApp(@RequestBody @Valid AppUpdateOperationInfo optInfo, @PathVariable String platformId, @PathVariable String domainId)
    {
        String uri = String.format("PUT %s/platformApps/%s/%s, params=%s", this.apiBasePath, platformId, domainId, gson.toJson(optInfo));
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            this.platformManagerService.debugPlatformApp(platformId, domainId, optInfo);
            resultPo = new AjaxResultPo(true, "debug task has been added to queue", 1, null);
            logger.info(String.format("debug task has been added to queue, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("add debug task to queue exception, quit %s controller", uri), e);
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

//    @RequestMapping(value = "/businesses", method = RequestMethod.GET)
//    public AjaxResultPo queryAllCCODBiz()
//    {
//        String uri = String.format("GET %s/businesses", this.apiBasePath);
//        logger.debug(String.format("enter %s controller", uri));
//        AjaxResultPo resultPo;
//        try
//        {
////            List<CCODPlatformInfo> bizPlatforms = this.ljPaasService.queryCCODBiz(null, null, null);
////            String data = gson.toJson(bizPlatforms);
////            List<CCODPlatformInfo> resultList = new ArrayList<>();
////            for(CCODPlatformInfo platformInfo : bizPlatforms)
////            {
////                resultList.add(platformInfo);
////            }
////            resultPo = new AjaxResultPo(true, "query SUCCESS", resultList.size(), resultList);
////            resultPo = new AjaxResultPo(true, "query SUCESS", 1, data);
////            resultPo = new AjaxResultPo(true, "test");
//            resultPo = new AjaxResultPo(true, "query SUCCESS", bizPlatforms.size(), bizPlatforms);
//            logger.info(String.format("query SUCCESS, quit %s", uri));
//        }
//        catch (Exception e)
//        {
//            logger.error(String.format("query ccod biz platforms exception, quit %s controller", uri), e);
//            resultPo = new AjaxResultPo(false, e.getMessage());
//        }
//        return resultPo;
//    }
//
//    @RequestMapping(value = "/businesses/{bizId}", method = RequestMethod.GET)
//    public AjaxResultPo queryCCODBizByBizId(@PathVariable int bizId)
//    {
//        String uri = String.format("GET %s/businesses/%d", this.apiBasePath, bizId);
//        logger.debug(String.format("enter %s controller", uri));
//        AjaxResultPo resultPo;
//        try
//        {
//            List<CCODPlatformInfo> bizPlatforms = this.ljPaasService.queryCCODBiz(bizId, null, null);
//            resultPo = new AjaxResultPo(true, "query SUCCESS", bizPlatforms.size(), bizPlatforms);
//            logger.info(String.format("query SUCCESS, quit %s", uri));
//        }
//        catch (Exception e)
//        {
//            logger.error(String.format("query ccod biz platforms exception, quit %s controller", uri), e);
//            resultPo = new AjaxResultPo(false, e.getMessage());
//        }
//        return resultPo;
//    }
//
//    @RequestMapping(value = "/businesses/{bizId}/{setId}", method = RequestMethod.GET)
//    public AjaxResultPo queryCCODBizBySetId(@PathVariable int bizId, @PathVariable String setId)
//    {
//        String uri = String.format("GET %s/businesses/%d/%s", this.apiBasePath, bizId, setId);
//        logger.debug(String.format("enter %s controller", uri));
//        AjaxResultPo resultPo;
//        try
//        {
//            List<CCODPlatformInfo> bizPlatforms = this.ljPaasService.queryCCODBiz(bizId, setId, null);
//            resultPo = new AjaxResultPo(true, "query SUCCESS", bizPlatforms.size(), bizPlatforms);
//            logger.info(String.format("query SUCCESS, quit %s", uri));
//        }
//        catch (Exception e)
//        {
//            logger.error(String.format("query ccod biz platforms exception, quit %s controller", uri), e);
//            resultPo = new AjaxResultPo(false, e.getMessage());
//        }
//        return resultPo;
//    }
//
//    @RequestMapping(value = "/businesses/{bizId}/{setId}/{domainId}", method = RequestMethod.GET)
//    public AjaxResultPo queryCCODBizByDomainId(@PathVariable int bizId, @PathVariable String setId, @PathVariable String domainId)
//    {
//        String uri = String.format("GET %s/businesses/%d/%s/%s", this.apiBasePath, bizId, setId, domainId);
//        logger.debug(String.format("enter %s controller", uri));
//        AjaxResultPo resultPo;
//        try
//        {
//            List<CCODPlatformInfo> bizPlatforms = this.ljPaasService.queryCCODBiz(bizId, setId, domainId);
//            resultPo = new AjaxResultPo(true, "query SUCCESS", bizPlatforms.size(), bizPlatforms);
//            logger.info(String.format("query SUCCESS, quit %s", uri));
//        }
//        catch (Exception e)
//        {
//            logger.error(String.format("query ccod biz platforms exception, quit %s controller", uri), e);
//            resultPo = new AjaxResultPo(false, e.getMessage());
//        }
//        return resultPo;
//    }


    @RequestMapping(value = "/sets", method = RequestMethod.GET)
    public AjaxResultPo queryAllCCODBizSet(String ccodVersion, Boolean hasImage)
    {
        String uri = String.format("GET %s/sets, ccodVersion=%s and hasImage=%s", this.apiBasePath, ccodVersion, hasImage);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<BizSetDefine> setDefines = this.appManagerService.queryCCODBizSetWithImage(ccodVersion, hasImage);
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

    @RequestMapping(value = "/platformDeploy/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo isPlatformDeployOngoing(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/platformDeploy/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            boolean ongoing = this.platformManagerService.isPlatformDeployOngoing(platformId);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, ongoing);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platform deploy ongoing exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformDeployStatus", method = RequestMethod.GET)
    public AjaxResultPo getLastPlatformDeployStatus()
    {
        String uri = String.format("GET %s/platformDeployStatus", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformDeployStatus status = this.platformManagerService.getLastPlatformDeployTaskStatus();
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, status);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platform last deploy status exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformAppDeployStatus/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo getPlatformCCODAppDeployStatus(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/platformAppDeployStatus/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<PlatformAppDeployDetailVo> details = this.platformManagerService.queryPlatformCCODAppDeployStatus(platformId, false);
            resultPo = new AjaxResultPo(true, "query SUCCESS", details.size(), details);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platform ccod app deploy status exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformAppDeployStatus/{platformId}/{domainId}/{appName}/{alias}", method = RequestMethod.GET)
    public AjaxResultPo getPlatformCCODSingleAppDeployStatus(@PathVariable String platformId, @PathVariable String domainId, @PathVariable String appName, @PathVariable String alias)
    {
        String uri = String.format("GET %s/platformAppDeployStatus/%s/%s/%s/%s", this.apiBasePath, platformId, domainId, appName, alias);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformAppDeployDetailVo detail = this.platformManagerService.queryPlatformCCODAppDeployStatus(platformId, domainId, appName, alias, false);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, detail);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platform single ccod app deploy status exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformDeployLogs", method = RequestMethod.GET)
    public AjaxResultPo getPlatformDeployLogs()
    {
        String uri = String.format("GET %s/platformDeployLogs", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<K8sOperationPo> logs = this.platformManagerService.getPlatformDeployLogs();
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, logs);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platform deploy logs exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
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
            List<PlatformUpdateSchemaInfo> schemaInfos = this.platformManagerService.queryPlatformUpdateSchema(null);
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
            List<PlatformUpdateSchemaInfo> schemaInfos = this.platformManagerService.queryPlatformUpdateSchema(platformId);
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
    public AjaxResultPo updatePlatformUpdateSchema(@RequestBody @Valid PlatformUpdateSchemaInfo schema)
    {
        String uri = String.format("POST %s/platformUpdateSchema, para=[%s]", this.apiBasePath, gson.toJson(schema));
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            platformManagerService.updatePlatformUpdateSchema(schema);
            resultPo = new AjaxResultPo(true, "update schema success", 1, null);
        }
        catch (Exception e)
        {
            logger.error(String.format("create demo schema exception"), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformData", method = RequestMethod.POST)
    public AjaxResultPo collectPlatformData(@RequestBody PlatformDataCollectParamVo param)
    {
        String uri = String.format("POST %s/platformData", this.apiBasePath);
        logger.debug(String.format("enter %s controller, param=%s", uri, gson.toJson(param)));
        AjaxResultPo resultPo;
        try
        {
            PlatformFunction func = param.getFunc() != null ? param.getFunc() : PlatformFunction.ONLINE;
            switch (param.getCollectContent())
            {
                case APP_MODULE:
                    if(param.getCollectMethod() == PlatformDataCollectParamVo.K8S_API)
                    {
                        platformManagerService.getPlatformTopologyFromK8s(param.getPlatformName(), param.getPlatformId(), param.getBkBizId(), param.getBkBizId(), param.getCcodVersion(), param.getHostIp(), param.getK8sApiUrl(), param.getK8sAuthToken(), func);
                    }
                    else
                    {
                        platformManagerService.startCollectPlatformAppData(param.getPlatformId(), param.getPlatformName(), param.getBkBizId(), param.getBkCloudId());
                    }
                    logger.info(String.format("start platform data collect task success, content=%s", param.getCollectContent().name));
                    resultPo = new AjaxResultPo(true, String.format("start platform data collect task success, content=%s", param.getCollectContent().name));
                    break;
                case APP_UPDATE:
                    platformManagerService.startCollectPlatformAppUpdateData(param.getPlatformId(), param.getPlatformName());
                    logger.info(String.format("start platform data update task success, content=%s", param.getCollectContent().name));
                    resultPo = new AjaxResultPo(true, String.format("start platform data update task success, content=%s", param.getCollectContent().name));
                    break;
                default:
                    resultPo = new AjaxResultPo(false, String.format("start platform data collect task fail, not support content=%s", param.getCollectContent().name));
                    logger.error(String.format("start platform data collect task fail, not support content=%s", param.getCollectContent().name));
                    break;
            }
        }
        catch (Exception e)
        {
            logger.error(String.format("start platform data collect task fail, content=%s", param.getCollectContent().name), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformTopologies", method = RequestMethod.POST)
    public AjaxResultPo createNewPlatform(@Valid @RequestBody PlatformCreateParamVo param)
    {
        String uri = String.format("POST %s/platformTopologies", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
           PlatformUpdateSchemaInfo schemaInfo = platformManagerService.createNewPlatform(param);
            resultPo = new AjaxResultPo(true, "create platform SUCCESS", 1, schemaInfo);
            logger.info(String.format("create platform SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platformTopologies exception, quit %s controller", uri), e);
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
            List<PlatformTopologyInfo> platformList = this.platformManagerService.queryAllPlatformTopology();
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

    @RequestMapping(value = "/platformRollBack/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo getPlatformRollbackPlans(@PathVariable String platformId, DomainUpdateType updateType)
    {
        String uri = String.format("GET %s/domainPlans/%s?updateType=%s", this.apiBasePath, platformId, updateType);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            if(DomainUpdateType.ROLLBACK.equals(updateType))
            {
                List<DomainUpdatePlanInfo> planList = this.platformManagerService.queryPlatformRollbackInfo(platformId);
                resultPo = new AjaxResultPo(true, "query SUCCESS", planList.size(), planList);
                logger.info(String.format("query SUCCESS, quit %s", uri));
            }
            else
                throw new Exception(String.format("current version not support %s update plan query", updateType));

        }
        catch (Exception e)
        {
            logger.error(String.format("query platformTopologies exception, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformRollBack/{platformId}", method = RequestMethod.PUT)
    public AjaxResultPo rollbackPlatform(@PathVariable String platformId, @RequestBody @Valid PlatformRollbackParamVo paramVo)
    {
        String uri = String.format("POST %s/platformRollBack/%s, param=%s", this.apiBasePath, platformId, gson.toJson(paramVo));
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformUpdateRecordVo recordVo = this.platformManagerService.rollbackPlatform(platformId, paramVo.getDomainIds());
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, recordVo);
            logger.info(String.format("rollback SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platformTopologies exception, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformUpdateRecords", method = RequestMethod.GET)
    public AjaxResultPo getAllUpdateRecords()
    {
        String uri = String.format("GET %s/platformUpdateRecords", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<PlatformUpdateRecordVo> recordList = this.platformManagerService.queryPlatformUpdateRecords();
            resultPo = new AjaxResultPo(true, "query SUCCESS", recordList.size(), recordList);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platformTopologies exception, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformUpdateRecords/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo getPlatformUpdateRecords(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/platformUpdateRecords/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<PlatformUpdateRecordVo> recordList = this.platformManagerService.queryPlatformUpdateRecordByPlatformId(platformId);
            resultPo = new AjaxResultPo(true, "query SUCCESS", recordList.size(), recordList);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platformTopologies exception, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/platformUpdateRecords/{platformId}/{jobId}", method = RequestMethod.GET)
    public AjaxResultPo getPlatformUpdateRecordByJobId(@PathVariable String platformId, @PathVariable String jobId)
    {
        String uri = String.format("GET %s/platformUpdateRecords/%s/%s", this.apiBasePath, platformId, jobId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            PlatformUpdateRecordVo recordPo = this.platformManagerService.queryPlatformUpdateRecordByJobId(platformId, jobId);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, recordPo);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query platformTopologies exception, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/newCCODBiz", method = RequestMethod.GET)
    public AjaxResultPo getPossibleCCODNewBiz()
    {
        String uri = String.format("GET %s/newCCODBizs", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<LJBizInfo> bizList = this.ljPaasService.queryNewCCODBiz();
            resultPo = new AjaxResultPo(true, "query SUCCESS", bizList.size(), bizList);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query new ccod biz exception, quit %s controller", uri), e);
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
            PlatformTopologyInfo topology = this.platformManagerService.getPlatformTopology(platformId);
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

    @DeleteMapping(value="/platformUpdateSchema")
    public AjaxResultPo deletePlatformUpdateSchema(@RequestParam(value="platformId") String platformId)
    {
        String uri = String.format("delete %s/platformUpdateSchema", this.apiBasePath);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            platformManagerService.deletePlatformUpdateSchema(platformId);
            resultPo = new AjaxResultPo(true, "delete platform update schema success");
        }
        catch (Exception ex)
        {
            logger.error(String.format("delete platform update schema FAIL"), ex);
            resultPo = new AjaxResultPo(false, String.format("delete platform update schema FAIL : %s", ex.getMessage()));
        }
        return resultPo;
    }

    @DeleteMapping(value="/platforms")
    public AjaxResultPo deletePlatform(@RequestParam(value="platformId") String platformId)
    {
        String uri = String.format("delete %s/platforms?platformId=%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            platformManagerService.deletePlatform(platformId);
            resultPo = new AjaxResultPo(true, "delete platform success");
        }
        catch (Exception ex)
        {
            logger.error(String.format("delete platform FAIL"), ex);
            resultPo = new AjaxResultPo(false, String.format("delete platform FAIL : %s", ex.getMessage()));
        }
        return resultPo;
    }

    @RequestMapping(value = "/debugApps/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo getPlatformDeployApps(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/debugApps/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<AppDebugDetailPo> details = this.platformManagerService.queryPlatformDebugApps(platformId);
            resultPo = new AjaxResultPo(true, "query SUCCESS", details.size(), details);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query debug detail exception, quit %s controller", uri), e);
            resultPo = new AjaxResultPo(false, e.getMessage());
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sNodes", method = RequestMethod.GET)
    public AjaxResultPo queryAllK8sNode(String k8sApiUrl, String k8sAuthToken)
    {
        String uri = String.format("GET %s/k8sNodes", this.apiBasePath);
        logger.debug(String.format("enter %s controller with k8sApiUrl=%s and authToken=%s", uri, k8sApiUrl, k8sAuthToken));
        AjaxResultPo resultPo;
        try
        {
            if(StringUtils.isBlank(k8sApiUrl))
                throw new Exception("k8s api url is blank");
            if(StringUtils.isBlank(k8sAuthToken))
                throw new Exception("k8s auth token is blank");
            List<V1Node> list = k8sApiService.listNode(k8sApiUrl, k8sAuthToken);
            resultPo = new AjaxResultPo(true, "query SUCCESS", list.size(), list);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query node exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sNodes/{nodeName}", method = RequestMethod.GET)
    public AjaxResultPo queryK8sNodeByName(@PathVariable String nodeName, String k8sApiUrl, String k8sAuthToken)
    {
        String uri = String.format("GET %s/k8sNodes/%s", this.apiBasePath, nodeName);
        logger.debug(String.format("enter %s controller with k8sApiUrl=%s and authToken=%s", uri, k8sApiUrl, k8sAuthToken));
        AjaxResultPo resultPo;
        try
        {
            if(StringUtils.isBlank(k8sApiUrl))
                throw new Exception("k8s api url is blank");
            if(StringUtils.isBlank(k8sAuthToken))
                throw new Exception("k8s auth token is blank");
            V1Node node = k8sApiService.readNode(nodeName, k8sApiUrl, k8sAuthToken);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, node);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query node exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sNamespaces", method = RequestMethod.GET)
    public AjaxResultPo queryAllK8sNamespace(String k8sApiUrl, String k8sAuthToken)
    {
        String uri = String.format("GET %s/k8sNamespaces", this.apiBasePath);
        logger.debug(String.format("enter %s controller with k8sApiUrl=%s and authToken=%s", uri, k8sApiUrl, k8sAuthToken));
        AjaxResultPo resultPo;
        try
        {
            if(StringUtils.isBlank(k8sApiUrl))
                throw new Exception("k8s api url is blank");
            if(StringUtils.isBlank(k8sAuthToken))
                throw new Exception("k8s auth token is blank");
            List<V1Namespace> list = k8sApiService.listNamespace(k8sApiUrl, k8sAuthToken);
            resultPo = new AjaxResultPo(true, "query SUCCESS", list.size(), list);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query namespace exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sNamespaces/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo queryAppsByName(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/k8sNamespaces/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            V1Namespace namespace = this.platformManagerService.queryPlatformK8sNamespace(platformId);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, namespace);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query namespace exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sPods/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo queryAllK8sPod(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/k8sPods/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<V1Pod> list = this.platformManagerService.queryPlatformAllK8sPods(platformId);
            resultPo = new AjaxResultPo(true, "query SUCCESS", list.size(), list);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query pod exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sPods/{platformId}/{podName}", method = RequestMethod.GET)
    public AjaxResultPo queryK8sPodByName(@PathVariable String platformId, @PathVariable String podName)
    {
        String uri = String.format("GET %s/k8sNamespaces/%s/%s", this.apiBasePath, platformId, podName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            V1Pod pod = this.platformManagerService.queryPlatformK8sPodByName(platformId, podName);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, pod);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query pod exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sServices/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo queryAllK8sService(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/k8sServices/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<V1Service> list = this.platformManagerService.queryPlatformAllK8sServices(platformId);
            resultPo = new AjaxResultPo(true, "query SUCCESS", list.size(), list);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query service exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sServices/{platformId}/{serviceName}", method = RequestMethod.GET)
    public AjaxResultPo queryK8sServiceByName(@PathVariable String platformId, @PathVariable String serviceName)
    {
        String uri = String.format("GET %s/k8sServices/%s/%s", this.apiBasePath, platformId, serviceName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            V1Service service = this.platformManagerService.queryPlatformK8sServiceByName(platformId, serviceName);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, service);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query service exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sServices/{platformId}", method = RequestMethod.POST)
    public AjaxResultPo createK8sPlatformK8sService(@PathVariable String platformId, @RequestBody V1Service service)
    {
        String uri = String.format("Post %s/k8sServices/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller, params=%s", uri, gson.toJson(service)));
        AjaxResultPo resultPo;
        try
        {
            V1Service create = this.platformManagerService.createK8sPlatformService(platformId, service);
            resultPo = new AjaxResultPo(true, "create Service SUCCESS", 1, create);
            logger.info(String.format("create Service SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("create Service exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sServices/{platformId}/{serviceName}", method = RequestMethod.PUT)
    public AjaxResultPo replaceK8sPlatformService(@PathVariable String platformId, @PathVariable String serviceName, @RequestBody V1Service service)
    {
        String uri = String.format("PUT %s/k8sServices/%s/%s", this.apiBasePath, platformId, serviceName);
        logger.debug(String.format("enter %s controller, params=%s", uri, gson.toJson(service)));
        AjaxResultPo resultPo;
        try
        {
            V1Service replace = this.platformManagerService.replaceK8sPlatformService(platformId, serviceName, service);
            resultPo = new AjaxResultPo(true, "replace Service SUCCESS", 1, replace);
            logger.info(String.format("replace Service Service, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("replace Service exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sServices/{platformId}/{serviceName}", method = RequestMethod.DELETE)
    public AjaxResultPo deleteK8sPlatformK8sService(@PathVariable String platformId, @PathVariable String serviceName)
    {
        String uri = String.format("delete %s/k8sServices/%s/%s", this.apiBasePath, platformId, serviceName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            this.platformManagerService.deleteK8sPlatformService(platformId, serviceName);
            resultPo = new AjaxResultPo(true, "delete Service SUCCESS", 1, null);
            logger.info(String.format("delete Service SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("delete Service exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sConfigMaps/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo queryAllK8sConfigMap(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/k8sConfigMaps/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<V1ConfigMap> list = this.platformManagerService.queryPlatformAllK8sConfigMaps(platformId);
            resultPo = new AjaxResultPo(true, "query SUCCESS", list.size(), list);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query configMap exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sConfigMaps/{platformId}/{configMapName}", method = RequestMethod.GET)
    public AjaxResultPo queryK8sConfigMapByName(@PathVariable String platformId, @PathVariable String configMapName)
    {
        String uri = String.format("GET %s/k8sConfigMaps/%s/%s", this.apiBasePath, platformId, configMapName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            V1ConfigMap configMap = this.platformManagerService.queryPlatformK8sConfigMapByName(platformId, configMapName);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, configMap);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query configMap exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sDeployments/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo queryAllK8sDeployment(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/k8sDeployments/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<V1Deployment> list = this.platformManagerService.queryPlatformAllK8sDeployment(platformId);
            resultPo = new AjaxResultPo(true, "query SUCCESS", list.size(), list);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query deployment exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sDeployments/{platformId}/{deploymentName}", method = RequestMethod.GET)
    public AjaxResultPo queryK8sDeploymentByName(@PathVariable String platformId, @PathVariable String deploymentName)
    {
        String uri = String.format("GET %s/k8sDeployments/%s/%s", this.apiBasePath, platformId, deploymentName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            V1Deployment deployment = this.platformManagerService.queryPlatformK8sDeploymentByName(platformId, deploymentName);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, deployment);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query deployment exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sDeployments/{platformId}", method = RequestMethod.POST)
    public AjaxResultPo createK8sPlatformK8sDeployments(@PathVariable String platformId, @RequestBody V1Deployment deployment)
    {
        String uri = String.format("Post %s/k8sDeployments/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller, params=%s", uri, gson.toJson(deployment)));
        AjaxResultPo resultPo;
        try
        {
            V1Deployment create = this.platformManagerService.createK8sPlatformDeployment(platformId, deployment);
            resultPo = new AjaxResultPo(true, "create Deployments SUCCESS", 1, create);
            logger.info(String.format("create Deployments SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("create Deployments exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sDeployments/{platformId}/{deploymentName}", method = RequestMethod.PUT)
    public AjaxResultPo replaceK8sPlatformDeployments(@PathVariable String platformId, @PathVariable String deploymentName, @RequestBody V1Deployment deployment)
    {
        String uri = String.format("PUT %s/k8sDeployments/%s/%s", this.apiBasePath, platformId, deploymentName);
        logger.debug(String.format("enter %s controller, params=%s", uri, gson.toJson(deployment)));
        AjaxResultPo resultPo;
        try
        {
            V1Deployment replace = this.platformManagerService.replaceK8sPlatformDeployment(platformId, deploymentName, deployment);
            resultPo = new AjaxResultPo(true, "replace Deployments SUCCESS", 1, replace);
            logger.info(String.format("replace Deployments SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("replace Deployments exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sDeployments/{platformId}/{deploymentName}", method = RequestMethod.DELETE)
    public AjaxResultPo deleteK8sPlatformK8sDeployments(@PathVariable String platformId, @PathVariable String deploymentName)
    {
        String uri = String.format("delete %s/k8sDeployments/%s/%s", this.apiBasePath, platformId, deploymentName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            this.platformManagerService.deleteK8sPlatformDeployment(platformId, deploymentName);
            resultPo = new AjaxResultPo(true, "delete Deployments SUCCESS", 1, null);
            logger.info(String.format("delete Deployments SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("delete Deployments exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sIngress/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo queryPlatformK8sIngress(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/k8sIngress/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<ExtensionsV1beta1Ingress> list = this.platformManagerService.queryPlatformAllK8sIngress(platformId);
            resultPo = new AjaxResultPo(true, "query SUCCESS", list.size(), list);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query ingress exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sIngress/{platformId}/{ingressName}", method = RequestMethod.GET)
    public AjaxResultPo queryK8sPlatformK8sIngressByName(@PathVariable String platformId, @PathVariable String ingressName)
    {
        String uri = String.format("GET %s/k8sIngress/%s/%s", this.apiBasePath, platformId, ingressName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            ExtensionsV1beta1Ingress ingress = this.platformManagerService.queryPlatformK8sIngressByName(platformId, ingressName);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, ingress);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query ingress exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sIngress/{platformId}", method = RequestMethod.POST)
    public AjaxResultPo createK8sPlatformK8sIngress(@PathVariable String platformId, @RequestBody ExtensionsV1beta1Ingress ingress)
    {
        String uri = String.format("Post %s/k8sIngress/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller, params=%s", uri, gson.toJson(ingress)));
        AjaxResultPo resultPo;
        try
        {
            ExtensionsV1beta1Ingress create = this.platformManagerService.createK8sPlatformIngress(platformId, ingress);
            resultPo = new AjaxResultPo(true, "create Ingress SUCCESS", 1, create);
            logger.info(String.format("create Ingress SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("create Ingress exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sIngress/{platformId}/{ingressName}", method = RequestMethod.PUT)
    public AjaxResultPo replaceK8sPlatformIngress(@PathVariable String platformId, @PathVariable String ingressName, @RequestBody ExtensionsV1beta1Ingress ingress)
    {
        String uri = String.format("PUT %s/k8sIngress/%s/%s", this.apiBasePath, platformId, ingressName);
        logger.debug(String.format("enter %s controller, params=%s", uri, gson.toJson(ingress)));
        AjaxResultPo resultPo;
        try
        {
            ExtensionsV1beta1Ingress replace = this.platformManagerService.replaceK8sPlatformIngress(platformId, ingressName, ingress);
            resultPo = new AjaxResultPo(true, "replace Ingress SUCCESS", 1, replace);
            logger.info(String.format("replace Ingress SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("replace Ingress exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sIngress/{platformId}/{ingressName}", method = RequestMethod.DELETE)
    public AjaxResultPo deleteK8sPlatformK8sIngress(@PathVariable String platformId, @PathVariable String ingressName)
    {
        String uri = String.format("delete %s/k8sIngress/%s/%s", this.apiBasePath, platformId, ingressName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            this.platformManagerService.deleteK8sPlatformIngress(platformId, ingressName);
            resultPo = new AjaxResultPo(true, "delete Ingress SUCCESS", 1, null);
            logger.info(String.format("delete Ingress SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("delete Ingress exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sEndpoints/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo queryPlatformK8sEndpoints(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/k8sEndpoints/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<V1Endpoints> list = this.platformManagerService.queryPlatformAllK8sEndpoints(platformId);;
            resultPo = new AjaxResultPo(true, "query SUCCESS", list.size(), list);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query endpoints exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sEndpoints/{platformId}", method = RequestMethod.POST)
    public AjaxResultPo createK8sPlatformK8sEndpoints(@PathVariable String platformId, @RequestBody V1Endpoints endpoints)
    {
        String uri = String.format("Post %s/k8sEndpoints/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller, params=%s", uri, gson.toJson(endpoints)));
        AjaxResultPo resultPo;
        try
        {
            V1Endpoints create = this.platformManagerService.createK8sPlatformEndpoints(platformId, endpoints);
            resultPo = new AjaxResultPo(true, "create Endpoints SUCCESS", 1, create);
            logger.info(String.format("create Endpoints SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("create Endpoints exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sEndpoints/{platformId}/{endpointsName}", method = RequestMethod.PUT)
    public AjaxResultPo replacePlatformK8sEndpointsByName(@PathVariable String platformId, @PathVariable String endpointsName, @RequestBody V1Endpoints endpoints)
    {
        String uri = String.format("PUT %s/k8sEndpoints/%s/%s", this.apiBasePath, platformId, endpointsName);
        logger.debug(String.format("enter %s controller, params=%s", uri, gson.toJson(endpoints)));
        AjaxResultPo resultPo;
        try
        {
            V1Endpoints replace = this.platformManagerService.replaceK8sPlatformEndpoints(platformId, endpointsName, endpoints);
            resultPo = new AjaxResultPo(true, "replace Endpoints SUCCESS", 1, replace);
            logger.info(String.format("replace Endpoints SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("replace Endpoints exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sEndpoints/{platformId}/{endpointsName}", method = RequestMethod.DELETE)
    public AjaxResultPo deleteK8sPlatformK8sEndpoints(@PathVariable String platformId, @PathVariable String endpointsName)
    {
        String uri = String.format("delete %s/k8sEndpoints/%s/%s", this.apiBasePath, platformId, endpointsName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            this.platformManagerService.deleteK8sPlatformEndpoints(platformId, endpointsName);
            resultPo = new AjaxResultPo(true, "delete Endpoints SUCCESS", 1, null);
            logger.info(String.format("delete Endpoints SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("delete Endpoints exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sEndpoints/{platformId}/{endpointsName}", method = RequestMethod.GET)
    public AjaxResultPo queryK8sPlatformK8sEndpointsByName(@PathVariable String platformId, @PathVariable String endpointsName)
    {
        String uri = String.format("GET %s/k8sEndpoints/%s/%s", this.apiBasePath, platformId, endpointsName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            V1Endpoints endpoints = this.platformManagerService.queryPlatformK8sEndpointsByName(platformId, endpointsName);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, endpoints);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query endpoints exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sSecrets/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo queryPlatformK8sSecret(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/k8sSecrets/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<V1Secret> list = this.platformManagerService.queryPlatformAllK8sSecret(platformId);;
            resultPo = new AjaxResultPo(true, "query SUCCESS", list.size(), list);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query Secret exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sSecrets/{platformId}/{secretName}", method = RequestMethod.GET)
    public AjaxResultPo queryK8sPlatformK8sSecretByName(@PathVariable String platformId, @PathVariable String secretName)
    {
        String uri = String.format("GET %s/k8sSecrets/%s/%s", this.apiBasePath, platformId, secretName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            V1Secret secret = this.platformManagerService.queryPlatformK8sSecretByName(platformId, secretName);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, secret);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query Secret exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sSecrets/{platformId}", method = RequestMethod.POST)
    public AjaxResultPo createK8sPlatformK8sSecret(@PathVariable String platformId, @RequestBody V1Secret secret)
    {
        String uri = String.format("Post %s/k8sSecrets/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller, params=%s", uri, gson.toJson(secret)));
        AjaxResultPo resultPo;
        try
        {
            V1Secret create = this.platformManagerService.createK8sPlatformSecret(platformId, secret);
            resultPo = new AjaxResultPo(true, "create Secret SUCCESS", 1, create);
            logger.info(String.format("create Secret SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("create Secret exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sSecrets/{platformId}/{secretName}", method = RequestMethod.PUT)
    public AjaxResultPo replacePlatformK8sSecret(@PathVariable String platformId, @PathVariable String secretName, @RequestBody V1Secret secret)
    {
        String uri = String.format("PUT %s/k8sSecrets/%s/%s", this.apiBasePath, platformId, secretName);
        logger.debug(String.format("enter %s controller, params=%s", uri, gson.toJson(secret)));
        AjaxResultPo resultPo;
        try
        {
            V1Secret replace = this.platformManagerService.replaceK8sPlatformSecret(platformId, secretName, secret);
            resultPo = new AjaxResultPo(true, "replace Secret SUCCESS", 1, replace);
            logger.info(String.format("replace Secret SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("replace Secret exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sSecrets/{platformId}/{secretName}", method = RequestMethod.DELETE)
    public AjaxResultPo deleteK8sPlatformK8sSecret(@PathVariable String platformId, @PathVariable String secretName)
    {
        String uri = String.format("delete %s/k8sSecrets/%s/%s", this.apiBasePath, platformId, secretName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            this.platformManagerService.deleteK8sPlatformSecret(platformId, secretName);
            resultPo = new AjaxResultPo(true, "delete Secret SUCCESS", 1, null);
            logger.info(String.format("delete Secret SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("delete Secret exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sPersistentVolumeClaim/{platformId}", method = RequestMethod.GET)
    public AjaxResultPo queryPlatformK8sPersistentVolumeClaims(@PathVariable String platformId)
    {
        String uri = String.format("GET %s/k8sPersistentVolumeClaim/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            List<V1PersistentVolumeClaim> list = this.platformManagerService.queryPlatformAllK8sPersistentVolumeClaim(platformId);;
            resultPo = new AjaxResultPo(true, "query SUCCESS", list.size(), list);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query PersistentVolumeClaim exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sPersistentVolumeClaim/{platformId}/{persistentVolumeClaimName}", method = RequestMethod.GET)
    public AjaxResultPo queryK8sPlatformK8sPersistentVolumeClaimByName(@PathVariable String platformId, @PathVariable String persistentVolumeClaimName)
    {
        String uri = String.format("GET %s/k8sPersistentVolumeClaim/%s/%s", this.apiBasePath, platformId, persistentVolumeClaimName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            V1PersistentVolumeClaim claim = this.platformManagerService.queryPlatformK8sPersistentVolumeClaimByName(platformId, persistentVolumeClaimName);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, claim);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query PersistentVolumeClaim exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sPersistentVolumeClaim/{platformId}", method = RequestMethod.POST)
    public AjaxResultPo createK8sPlatformK8sPersistentVolumeClaim(@PathVariable String platformId, @RequestBody V1PersistentVolumeClaim persistentVolumeClaim)
    {
        String uri = String.format("Post %s/k8sPersistentVolumeClaim/%s", this.apiBasePath, platformId);
        logger.debug(String.format("enter %s controller, params=%s", uri, gson.toJson(persistentVolumeClaim)));
        AjaxResultPo resultPo;
        try
        {
            V1PersistentVolumeClaim create = this.platformManagerService.createK8sPlatformPersistentVolumeClaim(platformId, persistentVolumeClaim);
            resultPo = new AjaxResultPo(true, "create PersistentVolumeClaim SUCCESS", 1, create);
            logger.info(String.format("create PersistentVolumeClaim SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("create PersistentVolumeClaim exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sPersistentVolumeClaim/{platformId}/{persistentVolumeClaimName}", method = RequestMethod.PUT)
    public AjaxResultPo replaceK8sPlatformPersistentVolumeClaim(@PathVariable String platformId, @PathVariable String persistentVolumeClaimName, @RequestBody V1PersistentVolumeClaim persistentVolumeClaim)
    {
        String uri = String.format("PUT %s/k8sPersistentVolumeClaim/%s/%s", this.apiBasePath, platformId, persistentVolumeClaimName);
        logger.debug(String.format("enter %s controller, params=%s", uri, gson.toJson(persistentVolumeClaim)));
        AjaxResultPo resultPo;
        try
        {
            V1PersistentVolumeClaim replace = this.platformManagerService.replaceK8sPlatformPersistentVolumeClaim(platformId, persistentVolumeClaimName, persistentVolumeClaim);
            resultPo = new AjaxResultPo(true, "replace PersistentVolumeClaim SUCCESS", 1, replace);
            logger.info(String.format("replace PersistentVolumeClaim SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("replace PersistentVolumeClaim exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sPersistentVolumeClaim/{platformId}/{persistentVolumeClaimName}", method = RequestMethod.DELETE)
    public AjaxResultPo deleteK8sPlatformK8sPersistentVolumeClaim(@PathVariable String platformId, @PathVariable String persistentVolumeClaimName)
    {
        String uri = String.format("delete %s/k8sPersistentVolumeClaim/%s/%s", this.apiBasePath, platformId, persistentVolumeClaimName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            this.platformManagerService.deleteK8sPlatformPersistentVolumeClaim(platformId, persistentVolumeClaimName);
            resultPo = new AjaxResultPo(true, "delete PersistentVolumeClaim SUCCESS", 1, null);
            logger.info(String.format("delete PersistentVolumeClaim SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("delete PersistentVolumeClaim exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sPersistentVolumes", method = RequestMethod.GET)
    public AjaxResultPo queryAllK8sk8sPersistentVolumes(String k8sApiUrl, String k8sAuthToken)
    {
        String uri = String.format("GET %s/k8sPersistentVolumes", this.apiBasePath);
        logger.debug(String.format("enter %s controller with k8sApiUrl=%s and authToken=%s", uri, k8sApiUrl, k8sAuthToken));
        AjaxResultPo resultPo;
        try
        {
            if(StringUtils.isBlank(k8sApiUrl))
                throw new Exception("k8s api url is blank");
            if(StringUtils.isBlank(k8sAuthToken))
                throw new Exception("k8s auth token is blank");
            List<V1PersistentVolume> list = k8sApiService.listPersistentVolume(k8sApiUrl, k8sAuthToken);
            resultPo = new AjaxResultPo(true, "query SUCCESS", list.size(), list);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query PersistentVolume exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sPersistentVolumes/{persistentVolumeName}", method = RequestMethod.GET)
    public AjaxResultPo queryK8sPersistentVolumeByName(@PathVariable String persistentVolumeName, String k8sApiUrl, String k8sAuthToken)
    {
        String uri = String.format("GET %s/k8sPersistentVolumes/%s", this.apiBasePath, persistentVolumeName);
        logger.debug(String.format("enter %s controller with k8sApiUrl=%s and authToken=%s", uri, k8sApiUrl, k8sAuthToken));
        AjaxResultPo resultPo;
        try
        {
            if(StringUtils.isBlank(k8sApiUrl))
                throw new Exception("k8s api url is blank");
            if(StringUtils.isBlank(k8sAuthToken))
                throw new Exception("k8s auth token is blank");
            V1PersistentVolume volume = k8sApiService.readPersistentVolume(persistentVolumeName, k8sApiUrl, k8sAuthToken);
            resultPo = new AjaxResultPo(true, "query SUCCESS", 1, volume);
            logger.info(String.format("query SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query PersistentVolume exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sPersistentVolumes", method = RequestMethod.POST)
    public AjaxResultPo createK8sPlatformK8sPersistentVolume(@RequestBody V1PersistentVolume persistentVolume, String k8sApiUrl, String k8sAuthToken)
    {
        String uri = String.format("Post %s/k8sPersistentVolumes", this.apiBasePath);
        logger.debug(String.format("enter %s controller, params=%s", uri, gson.toJson(persistentVolume)));
        AjaxResultPo resultPo;
        try
        {
            V1PersistentVolume create = this.k8sApiService.createPersistentVolume(persistentVolume, k8sApiUrl, k8sAuthToken);
            resultPo = new AjaxResultPo(true, "create PersistentVolume SUCCESS", 1, create);
            logger.info(String.format("create PersistentVolume SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("create PersistentVolume exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sPersistentVolumes/{persistentVolumeName}", method = RequestMethod.PUT)
    public AjaxResultPo replaceK8sPlatformPersistentVolume(@PathVariable String persistentVolumeName, @RequestBody V1PersistentVolume persistentVolume, String k8sApiUrl, String k8sAuthToken)
    {
        String uri = String.format("PUT %s/k8sPersistentVolumes/%s", this.apiBasePath, persistentVolumeName);
        logger.debug(String.format("enter %s controller, params=%s", uri, gson.toJson(persistentVolume)));
        AjaxResultPo resultPo;
        try
        {
            V1PersistentVolume replace = this.k8sApiService.replacePersistentVolume(persistentVolumeName, persistentVolume, k8sApiUrl, k8sAuthToken);
            resultPo = new AjaxResultPo(true, "replace PersistentVolume SUCCESS", 1, replace);
            logger.info(String.format("replace PersistentVolume SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("replace PersistentVolume exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping(value = "/k8sPersistentVolumes/{persistentVolumeName}", method = RequestMethod.DELETE)
    public AjaxResultPo deleteK8sPlatformK8sPersistentVolume(@PathVariable String persistentVolumeName, String k8sApiUrl, String k8sAuthToken)
    {
        String uri = String.format("delete %s/k8sPersistentVolumes/%s", this.apiBasePath, persistentVolumeName);
        logger.debug(String.format("enter %s controller", uri));
        AjaxResultPo resultPo;
        try
        {
            this.k8sApiService.deletePersistentVolume(persistentVolumeName, k8sApiUrl, k8sAuthToken);
            resultPo = new AjaxResultPo(true, "delete PersistentVolume SUCCESS", 1, null);
            logger.info(String.format("delete PersistentVolume SUCCESS, quit %s", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("delete PersistentVolume exception, quit %s controller", uri), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }
}
