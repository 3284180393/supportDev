package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.constant.VersionControl;
import com.channelsoft.ccod.support.cmdb.dao.AppCfgFileMapper;
import com.channelsoft.ccod.support.cmdb.dao.AppInstallPackageMapper;
import com.channelsoft.ccod.support.cmdb.dao.AppMapper;
import com.channelsoft.ccod.support.cmdb.po.*;
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
import java.util.*;
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

    @Autowired
    AppMapper appMapper;

    @Autowired
    AppCfgFileMapper appCfgFileMapper;

    @Autowired
    AppInstallPackageMapper appInstallPackageMapper;

    private boolean isPlatformCheckOngoing = false;

    private Map<String, NexusComponentPo> appNexusComponentMap = new ConcurrentHashMap<>();

    private final static Logger logger = LoggerFactory.getLogger(AppManagerServiceImpl.class);

    private String appDirectoryFmt = "/%s/%s/%s/";

    private String appCfgDirectoryFmt = "/%s/%s/%s/%s/%s/%s/";

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

    /**
     * 将一组平台应用模块上传到nexus
     * 如果某个模块对应的component已经在nexus存在则对比安装包的md5,如果md5不一致则报错，该平台应用模块上传失败,否则上传该平台应用模块的配置文件
     * 如果component不存在则创建该应用对应的component(appName/appAlias/version),并上传该平台应用的配置文件
     * @param modules 需要上传的模块
     * @throws Exception
     */
    void uploadPlatformAppModules(PlatformAppModuleVo[] modules) throws Exception
    {
        List<AppPo> appList = appMapper.select(null, null, null, null);
        Map<String, AppPo> appMap = new HashMap<>();
        for(AppPo appPo : appList)
        {
            appMap.put(String.format(this.appDirectoryFmt, appPo.getAppName(), appPo.getAppAlias(), appPo.getVersion()), appPo);
        }
        Map<String, Map<String, NexusAssetInfo>> appFileAssetMap = this.nexusService.queryRepositoryAssetRelationMap(this.appRepository);
        for(PlatformAppModuleVo module : modules)
        {
            String appDirectory = String.format(this.appDirectoryFmt, module.getModuleName(), module.getModuleAliasName(), module.getVersion());
            String cfgDirectory = String.format(this.appCfgDirectoryFmt, module.getPlatformId(), module.getDomainId(), module.getHostIp(),
                    module.getModuleName(), module.getModuleAliasName(), module.getBasePath());
            if(appMap.containsKey(appDirectory) && appFileAssetMap.containsKey(appDirectory))
            {
                AppPo appPo = appMap.get(appDirectory);
                Map<String, NexusAssetInfo> fileAssetMap = appFileAssetMap.get(appDirectory);
                //检查应用的配置文件数是否和保存的同版本相同
                if(fileAssetMap.size() != module.getCfgs().length+1)
                {
                    logger.error(String.format("handle [%s] module FAIL : reported cfg count=%d not equal the same version app=%d",
                            module.toString(), module.getCfgs().length, fileAssetMap.size()-1));
                    continue;
                }
                //检查应用的安装包文件名是否和保存的同版本相同
                DeployFileInfo installPackage = module.getInstallPackage();
                if(!fileAssetMap.containsKey(installPackage.getFileName()))
                {
                    logger.error(String.format("handle [%s] module FAIL : reported install package=%s not equal the saved same version",
                            module.toString(), installPackage.getFileName()));
                    continue;
                }
                //检查应用的安装包的md5是否和保存的同版本相同
                if(!fileAssetMap.get(installPackage.getFileName()).getMd5().equals(installPackage.getFileMd5()))
                {
                    logger.error(String.format("handle [%s] module FAIL : reported install package md5=%s not equal the saved same version md5=%s",
                            module.toString(), module.getInstallPackage().getFileMd5(), fileAssetMap.get(module.getInstallPackage().getFileName()).getMd5()));
                    continue;
                }
                //检查应用的配置文件名是否和保存的相同
                boolean isSame = true;
                for(DeployFileInfo cfg : module.getCfgs())
                {
                    if(!fileAssetMap.containsKey(cfg.getFileName()))
                    {
                        logger.error(String.format("handle [%s] module FAIL : reported cfg=%s not in the saved same version list",
                                module.toString(), cfg.getFileName()));
                        isSame = false;
                        break;
                    }
                }
                if(!isSame)
                    continue;
                //上传平台应用配置文件到nexus
                this.nexusService.uploadRawComponent(this.platformAppCfgRepository, cfgDirectory, module.getCfgs());


            }
            else if(appMap.containsKey(appDirectory) && !appFileAssetMap.containsKey(appDirectory))
            {

            }
            else if(!appMap.containsKey(appDirectory) && appFileAssetMap.containsKey(appDirectory))
            {

            }
            else
            {

            }
        }

    }



    private Map<String, Map<String, NexusAssetInfo>> uploadAppComponent(String appDirectory, PlatformAppModuleVo module, Map<String, AppPo> appMap) throws Exception
    {
        List<DeployFileInfo> appFiles = new ArrayList<>();
        appFiles.add(module.getInstallPackage());
        appFiles.addAll(Arrays.asList(module.getCfgs()));
        Map<String, Map<String, NexusAssetInfo>> appFileAssetMap = this.nexusService.uploadRawComponent(this.appRepository, appDirectory, appFiles.toArray(new DeployFileInfo[0]));
        Date now = new Date();
        AppPo appPo = appMap.get(appDirectory);
        int appId = appPo.getAppId();
        AppInstallPackagePo packagePo = new AppInstallPackagePo();
        packagePo.setAppId(appId);
        packagePo.setCreateTime(now);
        packagePo.setDeployPath(module.getInstallPackage().getDeployPath());
        packagePo.setFileName(module.getInstallPackage().getExt());
        packagePo.setMd5(module.getInstallPackage().getFileMd5());
        NexusAssetInfo ipAsset = appFileAssetMap.get(appDirectory).get(module.getInstallPackage().getFileName());
        packagePo.setNexusAssetId(ipAsset.getId());
        packagePo.setNexusRepository(this.appRepository);
        this.appInstallPackageMapper.insert(packagePo);
        for(DeployFileInfo cfg : module.getCfgs())
        {
            AppCfgFilePo cfgFilePo = new AppCfgFilePo();
            cfgFilePo.setAppId(appId);
            cfgFilePo.setCreateTime(now);
            cfgFilePo.setDeployPath(module.getInstallPackage().getDeployPath());
            cfgFilePo.setFileName(module.getInstallPackage().getExt());
            cfgFilePo.setMd5(module.getInstallPackage().getFileMd5());
            ipAsset = appFileAssetMap.get(appDirectory).get(module.getInstallPackage().getFileName());
            cfgFilePo.setNexusAssetId(ipAsset.getId());
            cfgFilePo.setNexusRepository(this.appRepository);
            this.appCfgFileMapper.insert(cfgFilePo);
        }
        return appFileAssetMap;
    }

    private Map<String, Map<String, NexusAssetInfo>> addNewApp(String appDirectory, PlatformAppModuleVo module, Map<String, AppPo> appMap) throws Exception
    {
        Date now = new Date();
//        String appDirectory = String.format(this.appDirectoryFmt, module.getModuleName(), module.getModuleAliasName(), module.getVersion());
        List<DeployFileInfo> appFiles = new ArrayList<>();
        appFiles.add(module.getInstallPackage());
        appFiles.addAll(Arrays.asList(module.getCfgs()));
        Map<String, Map<String, NexusAssetInfo>> appFileAssetMap = this.nexusService.uploadRawComponent(this.appRepository, appDirectory, appFiles.toArray(new DeployFileInfo[0]));
        AppPo appPo = new AppPo();
        appPo.setAppAlias(module.getModuleAliasName());
        appPo.setAppName(module.getModuleName());
        appPo.setAppType(module.getModuleType());
        appPo.setBasePath(module.getBasePath());
        appPo.setCcodVersion(module.getCcodVersion());
        appPo.setComment("");
        appPo.setCreateReason("client collect");
        appPo.setCreateTime(now);
        appPo.setUpdateTime(now);
        appPo.setVersion(module.getVersion());
        this.appMapper.insert(appPo);
        int appId = appPo.getAppId();
        AppInstallPackagePo packagePo = new AppInstallPackagePo();
        packagePo.setAppId(appId);
        packagePo.setCreateTime(now);
        packagePo.setDeployPath(module.getInstallPackage().getDeployPath());
        packagePo.setFileName(module.getInstallPackage().getExt());
        packagePo.setMd5(module.getInstallPackage().getFileMd5());
        NexusAssetInfo ipAsset = appFileAssetMap.get(appDirectory).get(module.getInstallPackage().getFileName());
        packagePo.setNexusAssetId(ipAsset.getId());
        packagePo.setNexusRepository(this.appRepository);
        this.appInstallPackageMapper.insert(packagePo);
        for(DeployFileInfo cfg : module.getCfgs())
        {
            AppCfgFilePo cfgFilePo = new AppCfgFilePo();
            cfgFilePo.setAppId(appId);
            cfgFilePo.setCreateTime(now);
            cfgFilePo.setDeployPath(module.getInstallPackage().getDeployPath());
            cfgFilePo.setFileName(module.getInstallPackage().getExt());
            cfgFilePo.setMd5(module.getInstallPackage().getFileMd5());
            ipAsset = appFileAssetMap.get(appDirectory).get(module.getInstallPackage().getFileName());
            cfgFilePo.setNexusAssetId(ipAsset.getId());
            cfgFilePo.setNexusRepository(this.appRepository);
            this.appCfgFileMapper.insert(cfgFilePo);
        }
        appMap.put(appDirectory, appPo);
        return appFileAssetMap;
    }

}
