package com.channelsoft.ccod.support.cmdb.controller;

import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.dao.PlatformResourceMapper;
import com.channelsoft.ccod.support.cmdb.po.AjaxResultPo;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import com.channelsoft.ccod.support.cmdb.service.IPlatformResourceService;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppDeployDetailVo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformResourceVo;
import com.channelsoft.ccod.support.cmdb.vo.QueryEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping("/cmdb/api")
public class CMDBController {

    @Autowired
    IAppManagerService appManagerService;

    @Autowired
    IPlatformResourceService platformResourceService;

    private final static Logger logger = LoggerFactory.getLogger(CMDBController.class);

    private String apiBasePath = "/cmdb/api";

    @RequestMapping("/apps")
    public AjaxResultPo queryAllApps()
    {
        String uri = String.format("%s/apps", this.apiBasePath);
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

    @RequestMapping("/apps/{appName}")
    public AjaxResultPo queryAppsByName(@PathVariable String appName)
    {
        String uri = String.format("%s/apps/%s", this.apiBasePath, appName);
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
        String uri = String.format("%s/apps/%s/%s", this.apiBasePath, appName, version);
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
            logger.debug(String.format("query SUCCESS, quit %s controller", uri));
        }
        catch (Exception e)
        {
            logger.error(String.format("query [] platform app deploy exception", JSONObject.toJSONString(queryEntity)), e);
            resultPo = AjaxResultPo.failed(e);
        }
        return resultPo;
    }

    @RequestMapping("/createPlatformAppDataCollectTask")
    public AjaxResultPo queryPlatformAppDeploy(String platformId)
    {
        AjaxResultPo resultPo;
        try
        {
            this.appManagerService.createNewPlatformAppDataCollectTask(platformId, null,null, null, null);
            resultPo = new AjaxResultPo(true, "app data task create success");
        }
        catch (Exception ex)
        {
            logger.error(String.format("create %s app data collect task exception", platformId), ex);
            resultPo = AjaxResultPo.failed(ex);
        }
        return resultPo;
    }

    @RequestMapping("/platformApps")
    public AjaxResultPo queryAllPlatformApps()
    {
        String uri = String.format("%s/platformApps", this.apiBasePath);
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

    @RequestMapping("/platformApps/{platformId}")
    public AjaxResultPo queryPlatformAppsByPlatformId(@PathVariable String platformId)
    {
        String uri = String.format("%s/platformApps/%s", this.apiBasePath, platformId);
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
        String uri = String.format("%s/platformApps/%s/%s", this.apiBasePath, platformId, domainId);
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
        String uri = String.format("%s/platformApps/%s/%s/%s", this.apiBasePath, platformId, domainId, hostIp);
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
        String uri = String.format("%s/platformResources", this.apiBasePath);
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
        String uri = String.format("%s/platformResources/%s", this.apiBasePath, platformId);
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
        String uri = String.format("%s/platformResources/%s/%s", this.apiBasePath, platformId, domainId);
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
        String uri = String.format("%s/platformResources/%s/%s/%s", this.apiBasePath, platformId, domainId, hostIp);
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
        String uri = String.format("%s/appDeployDetails", this.apiBasePath);
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
        String uri = String.format("%s/appDeployDetails/%s", this.apiBasePath, appName);
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
        String uri = String.format("%s/appDeployDetails/%s/%s", this.apiBasePath, appName, platformId);
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
        String uri = String.format("%s/appDeployDetails/%s/%s/%s", this.apiBasePath, appName, platformId, domainId);
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
        String uri = String.format("%s/appDeployDetails/%s/%s/%s/%s", this.apiBasePath, appName, platformId, domainId, hostIp);
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
}
