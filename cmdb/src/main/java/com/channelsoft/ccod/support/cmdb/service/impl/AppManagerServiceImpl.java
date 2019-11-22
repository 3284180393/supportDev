package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.constant.VersionControl;
import com.channelsoft.ccod.support.cmdb.dao.AppMapper;
import com.channelsoft.ccod.support.cmdb.po.AppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.AppInstallPackagePo;
import com.channelsoft.ccod.support.cmdb.po.AppPo;
import com.channelsoft.ccod.support.cmdb.po.NexusComponentPo;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.channelsoft.ccod.support.cmdb.service.IPlatformAppCollectService;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;
import com.channelsoft.ccod.support.cmdb.vo.DeployFileInfo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppModuleVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName: AppManagerServiceImpl
 * @Author: lanhb
 * @Description: AppManagerService的实现类
 * @Date: 2019/11/20 14:21
 * @Version: 1.0
 */
@Service
public class AppManagerServiceImpl implements IAppManagerService {

//    @Autowired
//    AppMapper appMapper;

    @Value("${nexus.platform_app_cfg_repository}")
    private String platformAppCfgRepository;

    @Value("${nexus.app_module_repository}")
    private String appRepository;

    @Autowired
    IPlatformAppCollectService platformAppCollectService;

    @Autowired
    INexusService nexusService;

    private boolean isPlatformCheckOngoing = false;

    private Map<String, NexusComponentPo> appNexusComponentMap = new ConcurrentHashMap<>();

    private final static Logger logger = LoggerFactory.getLogger(AppManagerServiceImpl.class);

    @PostConstruct
    void init() throws  Exception
    {
        try
        {
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
//            platformAppCollectService.collectPlatformAppData("shltPA", null, null, null, null);
            this.startCollectPlatformAppData("shltPA", null, null, null, null);
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public AppPo[] queryAllApp() throws Exception {
        return new AppPo[0];
    }

    @Override
    public AppPo[] queryAllAppByName(String appName, String appAlias) {
        return new AppPo[0];
    }

    @Override
    public AppModuleVo createNewAppModule(String appName, String appAlias, String version, VersionControl versionControl, String versionControlUrl, AppInstallPackagePo installPackage, AppCfgFilePo[] cfgs, String basePath) throws Exception {
        return null;
    }

    @Override
    public AppModuleVo queryAppModuleByVersion(String appName, String appAlias, String version) throws Exception {
        return null;
    }

    @Override
    public AppModuleVo[] queryAppModules(String appName, String appAlias) throws Exception {
        return new AppModuleVo[0];
    }

    @Override
    public PlatformAppModuleVo[] queryPlatformApps(String platformId) throws Exception {
        return new PlatformAppModuleVo[0];
    }

    @Override
    public PlatformAppModuleVo[] queryDomainApps(String platformId, String domainId) throws Exception {
        return new PlatformAppModuleVo[0];
    }

    @Override
    public PlatformAppModuleVo[] queryAppsForHostIp(String platformId, String domainId, String hostIp) throws Exception {
        return new PlatformAppModuleVo[0];
    }

    @Override
    public PlatformAppModuleVo[] startCollectPlatformAppData(String platformId, String domainName, String hostIp, String appName, String version) throws Exception {
        if(this.isPlatformCheckOngoing)
        {
            logger.error(String.format("start platform=%s app data collect FAIL : some collect task is ongoing", platformId));
            throw new Exception(String.format("start platform=%s app data collect FAIL : some collect task is ongoing", platformId));
        }
        this.isPlatformCheckOngoing = true;
        List<PlatformAppModuleVo> modules = this.platformAppCollectService.collectPlatformAppData(platformId, domainName, hostIp, appName, version);
        this.nexusService.reloadRepositoryComponent(this.appRepository);
        for(PlatformAppModuleVo module : modules)
        {
            boolean isGetFile = true;
            if(StringUtils.isBlank(module.getInstallPackage().getLocalSavePath()))
            {
                logger.error(String.format("platformId=%s and domainId=%s and hostIp=%s and appName=%s and appAlias=%s and version=%s and basePath=%s do not get install package=%s",
                        module.getPlatformId(), module.getDomainId(), module.getHostIp(), module.getModuleName(),
                        module.getModuleAliasName(), module.getVersion(), module.getBasePath(), module.getInstallPackage().getFileName()));
                isGetFile = false;
            }
            else
            {
                for(DeployFileInfo cfg : module.getCfgs())
                {
                    if(StringUtils.isBlank(cfg.getLocalSavePath()))
                    {
                        logger.error(String.format("platformId=%s and domainId=%s and hostIp=%s and appName=%s and appAlias=%s and version=%s and basePath=%s do not get cfg=%s",
                                module.getPlatformId(), module.getDomainId(), module.getHostIp(), module.getModuleName(),
                                module.getModuleAliasName(), module.getVersion(), module.getBasePath(), cfg.getFileName()));
                        isGetFile = false;
                    }
                }
            }
            if(isGetFile)
            {
                logger.info(String.format("platformId=%s and domainId=%s and hostIp=%s and appName=%s and appAlias=%s and version=%s and basePath=%s get install package and cfgs SUCCESS, so upload to nexus",
                        module.getPlatformId(), module.getDomainId(), module.getHostIp(), module.getModuleName(),
                        module.getModuleAliasName(), module.getVersion(), module.getBasePath()));
                this.nexusService.addPlatformAppModule(module);
            }
        }
        this.nexusService.releaseRepositoryComponent(this.appRepository);
        this.isPlatformCheckOngoing = false;
        return modules.toArray(new PlatformAppModuleVo[0]);
    }

    @Override
    public PlatformAppModuleVo[] startCheckPlatformAppData(String platformId, String domainName, String hostIp, String appName, String version) throws Exception {
        if(this.isPlatformCheckOngoing)
        {
            logger.error(String.format("start platform=%s app data check FAIL : some collect task is ongoing", platformId));
            throw new Exception(String.format("start platform=%s app data check FAIL : some collect task is ongoing", platformId));
        }
        this.isPlatformCheckOngoing = true;
        List<PlatformAppModuleVo> modules = this.platformAppCollectService.collectPlatformAppData(platformId, domainName, hostIp, appName, version);
        this.isPlatformCheckOngoing = false;
        return modules.toArray(new PlatformAppModuleVo[0]);
    }
}
