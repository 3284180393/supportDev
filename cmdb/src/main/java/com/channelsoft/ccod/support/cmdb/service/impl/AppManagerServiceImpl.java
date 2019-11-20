package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.constant.VersionControl;
import com.channelsoft.ccod.support.cmdb.po.AppCfgFilePo;
import com.channelsoft.ccod.support.cmdb.po.AppPackagePo;
import com.channelsoft.ccod.support.cmdb.po.AppPo;
import com.channelsoft.ccod.support.cmdb.po.NexusComponentPo;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.channelsoft.ccod.support.cmdb.service.IPlatformAppCollectService;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppModuleVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    @Override
    public AppPo[] queryAllApp() throws Exception {
        return new AppPo[0];
    }

    @Override
    public AppPo[] queryAllAppByName(String appName, String appAlias) {
        return new AppPo[0];
    }

    @Override
    public AppModuleVo createNewAppModule(String appName, String appAlias, String version, VersionControl versionControl, String versionControlUrl, AppPackagePo installPackage, AppCfgFilePo[] cfgs, String basePath) throws Exception {
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
        for(PlatformAppModuleVo module : modules)
        {
            this.nexusService.addPlatformAppModule(module);
        }
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
