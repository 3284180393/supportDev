package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.config.NotCheckCfgApp;
import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.VersionControl;
import com.channelsoft.ccod.support.cmdb.dao.*;
import com.channelsoft.ccod.support.cmdb.exception.*;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import com.channelsoft.ccod.support.cmdb.service.ILJPaasService;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.channelsoft.ccod.support.cmdb.service.IPlatformAppCollectService;
import com.channelsoft.ccod.support.cmdb.vo.*;
import com.sun.javafx.binding.StringFormatter;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Value("${nexus.platform-app-cfg-repository}")
    private String platformAppCfgRepository;

    @Value("${nexus.app-module-repository}")
    private String appRepository;

    @Value("${debug}")
    private boolean debug;

    @Value("${nexus.user}")
    private String nexusUserName;

    @Value("${nexus.password}")
    private String nexusPassword;

    @Value("${nexus.host-url}")
    private String nexusHostUrl;

    @Value("${app-publish-nexus.user}")
    private String publishNexusUserName;

    @Value("${app-publish-nexus.password}")
    private String publishNexusPassword;

    @Value("${app-publish-nexus.host-url}")
    private String publishNexusHostUrl;

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

    @Autowired
    ServerMapper serverMapper;

    @Autowired
    ServerUserMapper serverUserMapper;

    @Autowired
    PlatformMapper platformMapper;

    @Autowired
    DomainMapper domainMapper;

    @Autowired
    PlatformAppMapper platformAppMapper;

    @Autowired
    PlatformAppCfgFileMapper platformAppCfgFileMapper;

    @Autowired
    AppModuleMapper appModuleMapper;

    @Autowired
    PlatformAppDeployDetailMapper platformAppDeployDetailMapper;

    @Autowired
    PlatformResourceMapper platformResourceMapper;

    @Autowired
    ILJPaasService paasService;

    @Autowired
    private NotCheckCfgApp notCheckCfgApp;

    private boolean isPlatformCheckOngoing = false;

    private Set<String> notCheckCfgAppSet;

    private final static Logger logger = LoggerFactory.getLogger(AppManagerServiceImpl.class);

    private String appDirectoryFmt = "/%s/%s";

    private String appCfgDirectoryFmt = "/%s/%s/%s/%s/%s%s";

    private String domainKeyFmt = "%s/%s";

    private String serverKeyFmt = "%s/%s/%s";

    private String serverUserkeyFmt = "/%d/%d/%s";

    private String tmpSaveDirFmt = "%s/downloads/%s";

    @PostConstruct
    void init() throws  Exception
    {
        this.notCheckCfgAppSet = new HashSet<>(this.notCheckCfgApp.getNotCheckCfgApps());
        logger.info(String.format("%s will not check cfg count of app",
                JSONArray.toJSONString(this.notCheckCfgAppSet)));
        try
        {
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
//            this.appMapper.selectByPrimaryKey(1);
//            this.appMapper.select(null, null, null, null);
//            platformAppCollectService.collectPlatformAppData("shltPA", null, null, null, null);
//            this.startCollectPlatformAppData("tool", null, null, null, null);
//            this.appModuleMapper.select("jj","aa", "bb", "kk");
//            this.platformAppDeployDetailMapper.select("11", "22", "33", "44",
//                    "55", "66", "77", "88");
//            List<AppModuleVo> appList = this.appModuleMapper.select(null, null, null, null);
//            System.out.println(JSONArray.toJSONString(appList));
//            List<PlatformAppDeployDetailVo> deployList = this.platformAppDeployDetailMapper.select(null, null, null,
//                    null, null, null, null, null);
//            System.out.println(JSONArray.toJSONString(deployList));
//            List<PlatformResourceVo> platformResourceList = this.platformResourceMapper.select();
//            System.out.println(JSONArray.toJSONString(platformResourceList));
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public AppModuleVo createNewAppModule(String appName, String appAlias, String version, VersionControl versionControl, String versionControlUrl, AppInstallPackagePo installPackage, AppCfgFilePo[] cfgs, String basePath) throws Exception {
        return null;
    }

    @Override
    public AppModuleVo[] queryApps(String appName) throws DataAccessException {
        logger.debug(String.format("begin to query app modules : appName=%s", appName));
        List<AppModuleVo> list = this.appModuleMapper.select(null, appName, null, null);
        logger.info(String.format("query %d app module record with appName=%s", list.size(), appName));
        return list.toArray(new AppModuleVo[0]);
    }

    @Override
    public AppModuleVo queryAppByVersion(String appName, String version) throws ParamException, DataAccessException {
        logger.debug(String.format("begin to query appName=%s and version=%s app module record", appName, version));
        if(StringUtils.isBlank(appName))
        {
            logger.error("query FAIL : appName is blank");
            throw new ParamException("appName is blank");
        }
        if(StringUtils.isBlank(version))
        {
            logger.error("query FAIL : version is blank");
            throw new ParamException("version is blank");
        }
        AppModuleVo moduleVo = this.appModuleMapper.selectByNameAndVersion(appName, version);
        if(moduleVo == null)
        {
            logger.warn(String.format("not find app module with appName=%s and version=%s", appName, version));
        }
        else
        {
            logger.info(String.format("query app module[%s] with appName=%s and version=%s",
                    JSONObject.toJSONString(moduleVo), appName, version));
        }
        return moduleVo;
    }

    @Override
    public PlatformAppDeployDetailVo[] queryPlatformApps(String platformId, String domainId, String hostIp) throws DataAccessException {
        logger.debug(String.format("begin to query platform apps : platformId=%s, domainId=%s, hostIp=%s",
                platformId, domainId, hostIp));
        List<PlatformAppDeployDetailVo> list = this.platformAppDeployDetailMapper.selectPlatformApps(platformId, domainId, hostIp, null);
        logger.info(String.format("query %d record with platformId=%s and domainId=%s and hostIp=%s",
                list.size(), platformId, domainId, hostIp));
        return list.toArray(new PlatformAppDeployDetailVo[0]);
    }


    @Override
    public PlatformAppDeployDetailVo[] queryAppDeployDetails(String appName, String platformId, String domainId, String hostIp) throws DataAccessException {
        logger.debug(String.format("begin to query platform apps : appName=%s, platformId=%s, domainId=%s, hostIp=%s",
                appName, platformId, domainId, hostIp));
        List<PlatformAppDeployDetailVo> list = this.platformAppDeployDetailMapper.selectAppDeployDetails(appName, platformId, domainId, hostIp);
        logger.info(String.format("query %d record with appName=%s and platformId=%s and domainId=%s and hostIp=%s",
                list.size(), appName, platformId, domainId, hostIp));
        return list.toArray(new PlatformAppDeployDetailVo[0]);
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
//        if(debug)
//        {
//            for(PlatformAppModuleVo module : modules)
//            {
//                module.setPlatformId("yg");
//                module.setPlatformName("阳光保险");
//            }
//        }
        handleCollectedPlatformAppModules(modules.toArray(new PlatformAppModuleVo[0]));
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
    void handleCollectedPlatformAppModules(PlatformAppModuleVo[] modules) throws Exception
    {
        List<AppPo> appList = appMapper.select(null, null, null, null);
        Map<String, AppPo> appMap = new HashMap<>();
        List<PlatformPo> platformList = this.platformMapper.select(null);
        Map<String, PlatformPo> platformMap = new HashMap<>();
        for(PlatformPo po : platformList)
        {
            platformMap.put(po.getPlatformId(), po);
        }
        List<DomainPo> domainList = this.domainMapper.select(null, null);
        Map<String, DomainPo> domainMap = new HashMap<>();
        for(DomainPo po : domainList)
        {
            domainMap.put(String.format(this.domainKeyFmt, po.getPlatformId(), po.getDomainId()), po);
        }
        List<ServerPo> severList = this.serverMapper.select(null, null, null);
        Map<String, ServerPo> serverMap = new HashMap<>();
        for(ServerPo po : severList)
        {
            serverMap.put(String.format(this.serverKeyFmt, po.getPlatformId(), po.getDomainId(), po.getHostIp()), po);
        }
        List<ServerUserPo> serverUserList = this.serverUserMapper.select(null);
        Map<String, ServerUserPo> serverUserMap = new HashMap<>();
        for(ServerUserPo po : serverUserList)
        {
            serverUserMap.put(String.format(this.serverUserkeyFmt, po.getServerId(), 1, po.getUserName()), po);
        }
        Map<String, Map<String, NexusAssetInfo>> appFileAssetMap = new HashMap<>();
        for(PlatformAppModuleVo module : modules)
        {
            try
            {
                boolean handleSucc = handlePlatformAppModule(module, appList, appFileAssetMap, platformMap, domainMap, serverMap,
                        serverUserMap);
                if(!handleSucc)
                {
                    logger.error(String.format("handle [%s] FAIL", module.toString()));
                }
            }
            catch (Exception ex)
            {
                logger.error(String.format("handle [%s] exception", module.toString()), ex);
            }

        }
    }

    /**
     * 处理客户端收集的平台应用信息
     * 如果该模块在db中没有记录则需要上传二进制安装包以及配置文件并在数据库创建一条记录
     * 归档平台应用的配置文件，并在数据库创建一条平台应用详情记录
     * @param module 客户端收集的平台应用信息
     * @param appList 已有应用列表
     * @param appFileAssetMap
     * @param platformMap 已有平台列表
     * @param domainMap 已有域列表
     * @param serverMap
     * @param userMap
     * @return
     * @throws Exception
     */
    private boolean handlePlatformAppModule(PlatformAppModuleVo module, List<AppPo> appList,
                                            Map<String, List<NexusAssetInfo>> appFileAssetMap,
                                            Map<String, PlatformPo> platformMap, Map<String, DomainPo> domainMap,
                                            Map<String, ServerPo> serverMap, Map<String, ServerUserPo> userMap)
            throws DataAccessException, InterfaceCallException, NexusException, DBNexusNotConsistentException
    {
        AppPo appPo = module.getAppInfo();
        String appName = appPo.getAppName();
        String appVersion = appPo.getVersion();
        if(StringUtils.isBlank(module.getInstallPackage().getLocalSavePath()))
        {
            logger.error(String.format("handle [%s] FAIL : not receive install package[%s]",
                    module.toString(), module.getInstallPackage().getFileName()));
            return false;
        }
        StringBuffer sb = new StringBuffer();
        for(DeployFileInfo cfg : module.getCfgs())
        {
            if(StringUtils.isBlank(cfg.getLocalSavePath()))
            {
                sb.append(String.format("%s,", cfg.getFileName()));
            }
        }
        if(StringUtils.isNotBlank(sb.toString()))
        {
            logger.error(String.format("handle [%s] FAIL : not receive cfg[%s]",
                    module.toString(), sb.toString()));
            return false;
        }
        String appDirectory = String.format(this.appDirectoryFmt, module.getModuleName(), module.getVersion());
        String cfgDirectory = String.format(this.appCfgDirectoryFmt, module.getPlatformId(), module.getDomainId(), module.getHostIp(),
                module.getModuleName(), module.getModuleAliasName(), module.getBasePath());
        logger.info(String.format("handle [%s] platform app module : appDirectory=%s and cfgDirectory=%s",
                module.toString(), appDirectory, cfgDirectory));
        if(!appFileAssetMap.containsKey(appDirectory))
        {
            List<NexusAssetInfo> appSetList = this.nexusService.queryGroupAssetMap(this.nexusHostUrl,
                    this.nexusUserName, this.nexusPassword, appRepository, appDirectory);
            if(appSetList.size() > 0)
            {
                appFileAssetMap.put(appDirectory, appSetList);
            }
        }
        Map<String, List<AppPo>> appMap = appList.stream().collect(Collectors.groupingBy(AppPo::getAppName));
        if(appMap.containsKey(appName))
        {
            Map<String, AppPo> versionMap = appMap.get(appName).stream().collect(Collectors.toMap(AppPo::getVersion, Function.identity()));
            //检查报告的应用的的安装包和配置文件数是否和保存在nexus上的相同
            if(versionMap.containsKey(appVersion) && appFileAssetMap.containsKey(appDirectory))
            {
                //检查应用的安装包文件名是否和保存的同版本相同
                Map<String, NexusAssetInfo> fileAssetMap = appFileAssetMap.get(appDirectory).stream()
                        .collect(Collectors.toMap(NexusAssetInfo::getPath, Function.identity()));
                //检查安装包名是否一致
                String installPackagePath = String.format("%s/%s", module.getInstallPackage().getFileName()).replaceAll("^/", "");
                if (!fileAssetMap.containsKey(installPackagePath)) {
                    logger.error(String.format("handle [%s] module FAIL : reported install package=%s not equal the saved same version",
                            module.toString(), installPackagePath));
                    throw new DBNexusNotConsistentException(String.format("handle [%s] module FAIL : reported install package=%s not equal the saved same version",
                            module.toString(), installPackagePath));
                }
                //检查应用的安装包的md5是否和保存的同版本相同
                if (!fileAssetMap.get(installPackagePath).getMd5().equals(module.getInstallPackage().getFileMd5())) {
                    logger.error(String.format("handle [%s] module FAIL : reported install package md5=%s not equal the saved same version md5=%s",
                            module.toString(), module.getInstallPackage().getFileMd5(), fileAssetMap.get(module.getInstallPackage().getFileName()).getMd5()));
                    throw new DBNexusNotConsistentException(String.format("handle [%s] module FAIL : reported install package md5=%s not equal the saved same version md5=%s",
                            module.toString(), module.getInstallPackage().getFileMd5(), fileAssetMap.get(module.getInstallPackage().getFileName()).getMd5()));
                }
                if(this.notCheckCfgAppSet.contains(appName))
                {
                    if(fileAssetMap.size() != module.getCfgs().length + 1)
                    {
                        logger.error(String.format("cfg count of module[%s] is error : save=%d and report=%d",
                                module.toString(), fileAssetMap.size()-1, module.getCfgs().length));
                        throw new DBNexusNotConsistentException(String.format("cfg count of module[%s] is error : save=%d and report=%d",
                                module.toString(), fileAssetMap.size()-1, module.getCfgs().length));
                    }
                    //检查应用的配置文件名是否和保存的相同
                    for (DeployFileInfo cfg : module.getCfgs()) {
                        String cfgPath = String.format("%s/%s", appDirectory, cfg.getFileName()).replaceAll("^/", "");
                        if (!fileAssetMap.containsKey(cfgPath)) {
                            logger.error(String.format("handle [%s] module FAIL : reported cfg=%s not in the saved same version list",
                                    module.toString(), cfg.getFileName()));
                            throw new DBNexusNotConsistentException(String.format("handle [%s] module FAIL : reported cfg=%s not in the saved same version list",
                                    module.toString(), cfg.getFileName()));
                        }
                    }
                }
                else
                {
                    logger.info(String.format("appName=%s with version=%s is in not check cfg count apps, so save=%d and report=%d is legal",
                            appName, appVersion, fileAssetMap.size()-1, module.getCfgs().length));
                }
            }
        }
        if(appMap.containsKey(appDirectory) && appFileAssetMap.containsKey(appDirectory)) {
            Map<String, NexusAssetInfo> fileAssetMap = appFileAssetMap.get(appDirectory);
            //检查应用的配置文件数是否和保存的同版本相同
            if (fileAssetMap.size() != module.getCfgs().length + 1) {
                logger.error(String.format("handle [%s] module FAIL : reported cfg count=%d not equal the same version app=%d",
                        module.toString(), module.getCfgs().length, fileAssetMap.size() - 1));
                return false;
            }
            //检查应用的安装包文件名是否和保存的同版本相同
            DeployFileInfo installPackage = module.getInstallPackage();
            if (!fileAssetMap.containsKey(installPackage.getFileName())) {
                logger.error(String.format("handle [%s] module FAIL : reported install package=%s not equal the saved same version",
                        module.toString(), installPackage.getFileName()));
                return false;
            }
            //检查应用的安装包的md5是否和保存的同版本相同
            if (!fileAssetMap.get(installPackage.getFileName()).getMd5().equals(installPackage.getFileMd5())) {
                logger.error(String.format("handle [%s] module FAIL : reported install package md5=%s not equal the saved same version md5=%s",
                        module.toString(), module.getInstallPackage().getFileMd5(), fileAssetMap.get(module.getInstallPackage().getFileName()).getMd5()));
                return false;
            }
            //检查应用的配置文件名是否和保存的相同
            for (DeployFileInfo cfg : module.getCfgs()) {
                if (!fileAssetMap.containsKey(cfg.getFileName())) {
                    logger.error(String.format("handle [%s] module FAIL : reported cfg=%s not in the saved same version list",
                            module.toString(), cfg.getFileName()));
                    return false;
                }
            }
            logger.info(String.format("collect %s app's install package and cfg files is same with nexus repository=%s",
                    appDirectory, appRepository));
        }
        else if(!appMap.containsKey(appDirectory) && !appFileAssetMap.containsKey(appDirectory))
        {
            //上传应用安装包以及配置文件到nexus
            Map<String, NexusAssetInfo> fileAssetMap = addAppToNexusAndDB(appPo, module.getInstallPackage(), module.getCfgs(), appRepository, appDirectory);
            logger.info(String.format("%s been added to appFileAssetMap", appDirectory));
            appFileAssetMap.put(appDirectory, fileAssetMap);
            appMap.put(appDirectory, appPo);
        }
        else if(!appMap.containsKey(appDirectory) && appFileAssetMap.containsKey(appDirectory))
        {
            logger.error(String.format("appDirectory=%s in appFileAssetMap not in appMap, appSet=%s and appFileAssetSet=%s",
                    appDirectory, JSONObject.toJSONString(appMap.keySet()), JSONObject.toJSONString(appFileAssetMap.keySet())));
            return false;
        }
        else
        {
            logger.error(String.format("appDirectory=%s in appMap not in appFileAssetMap, appSet=%s and appFileAssetSet=%s",
                    appDirectory, JSONObject.toJSONString(appMap.keySet()), JSONObject.toJSONString(appFileAssetMap.keySet())));
            return false;
        }
        appPo = appMap.get(appDirectory);
        //上传平台应用配置文件到nexus
        this.nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, this.platformAppCfgRepository, cfgDirectory, module.getCfgs());
        PlatformPo platformPo = module.getPlatform();
        String platformId = platformPo.getPlatformId();
        if(!platformMap.containsKey(platformId))
        {
            platformPo.setCcodVersion("4.5");
            platformPo.setCreateTime(new Date());
            this.platformMapper.insert(platformPo);
            platformMap.put(platformId, platformPo);
        }
        DomainPo domainPo = module.getDomain();
        String domainKey = String.format(this.domainKeyFmt, domainPo.getPlatformId(), domainPo.getDomainId());
        if(!domainMap.containsKey(domainKey))
        {
            domainPo.setCreateTime(new Date());
            this.domainMapper.insert(domainPo);
            domainMap.put(domainKey, domainPo);
        }
        ServerPo serverPo = module.getServerInfo();
        String serverKey = String.format(this.serverKeyFmt, serverPo.getPlatformId(), serverPo.getDomainId(), serverPo.getHostIp());
        if(!serverMap.containsKey(serverKey))
        {
            this.serverMapper.insert(serverPo);
            serverMap.put(serverKey, serverPo);
        }
        serverPo = serverMap.get(serverKey);
        ServerUserPo userPo =  module.getServerUser();
        String serverUserKey = String.format(this.serverUserkeyFmt, serverPo.getServerId(),
                1, module.getLoginUser());
        if(userMap.containsKey(serverUserKey))
        {
            if(userMap.get(serverUserKey).getLoginMethod() != userPo.getLoginMethod() || userMap.get(serverUserKey).getSshPort() != userPo.getSshPort()
                    || !userMap.get(serverUserKey).getPassword().equals(userPo.getPassword()))
            {
                userPo.setUserId(userMap.get(serverUserKey).getUserId());
                this.serverUserMapper.update(userPo);
                userMap.put(serverUserKey, userPo);
            }
        }
        else
        {
            userPo = module.getServerUser();
            userPo.setServerId(serverPo.getServerId());
            this.serverUserMapper.insert(userPo);
            userMap.put(serverUserKey, userPo);
        }
        userPo = userMap.get(serverUserKey);
        PlatformAppPo platformApp = module.getPlatformApp();
        platformApp.setAppId(appPo.getAppId());
        platformApp.setServerId(serverPo.getServerId());
        platformApp.setRunnerId(userPo.getUserId());
        this.platformAppMapper.insert(platformApp);
        for(DeployFileInfo cfgFilePo : module.getCfgs())
        {
            PlatformAppCfgFilePo po = new PlatformAppCfgFilePo(platformApp.getPlatformAppId(), cfgFilePo);
            System.out.println("asset_id=" + po.getNexusAssetId());
            this.platformAppCfgFileMapper.insert(po);
        }
        logger.info(String.format("[%s] platform app module handle SUCCESS", module.toString()));
        return true;
    }


    private Map<String, NexusAssetInfo> addAppToNexusAndDB(AppPo app, DeployFileInfo installPackage, DeployFileInfo[] cfgs, String repository, String directory) throws Exception
    {
        logger.info(String.format("prepare to upload appName=%s and appAlias=%s and version=%s app upload to directory=%s at repository=%s",
                app.getAppName(), app.getAppAlias(), app.getVersion(), directory, repository));
        List<DeployFileInfo> uploadFiles = new ArrayList<>();
        uploadFiles.add(installPackage);
        uploadFiles.addAll(Arrays.asList(cfgs));
        Map<String, NexusAssetInfo> fileAssetMap = this.nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, repository, directory, uploadFiles.toArray(new DeployFileInfo[0]));
        logger.info(String.format("prepare to add appName=%s and appAlias=%s and version=%s app info to database",
                app.getAppName(), app.getAppAlias(), app.getVersion()));
        app.setAppType(AppType.CCOD_KERNEL_MODULE.name);
        app.setCcodVersion("4.5");
        this.appMapper.insert(app);
        AppInstallPackagePo instPkgPo = new AppInstallPackagePo(app.getAppId(), installPackage);
        this.appInstallPackageMapper.insert(instPkgPo);
        for(DeployFileInfo cfg : cfgs)
        {
            AppCfgFilePo cfgFilePo = new AppCfgFilePo(app.getAppId(), cfg);
            this.appCfgFileMapper.insert(cfgFilePo);
        }
        return fileAssetMap;
    }


//    private Map<String, Map<String, NexusAssetInfo>> uploadAppComponent(String appDirectory, PlatformAppModuleVo module, Map<String, AppPo> appMap) throws Exception
//    {
//        List<DeployFileInfo> appFiles = new ArrayList<>();
//        appFiles.add(module.getInstallPackage());
//        appFiles.addAll(Arrays.asList(module.getCfgs()));
//        Map<String, Map<String, NexusAssetInfo>> appFileAssetMap = this.nexusService.uploadRawComponent(this.appRepository, appDirectory, appFiles.toArray(new DeployFileInfo[0]));
//        Date now = new Date();
//        AppPo appPo = appMap.get(appDirectory);
//        int appId = appPo.getAppId();
//        AppInstallPackagePo packagePo = new AppInstallPackagePo();
//        packagePo.setAppId(appId);
//        packagePo.setCreateTime(now);
//        packagePo.setDeployPath(module.getInstallPackage().getDeployPath());
//        packagePo.setFileName(module.getInstallPackage().getExt());
//        packagePo.setMd5(module.getInstallPackage().getFileMd5());
//        NexusAssetInfo ipAsset = appFileAssetMap.get(appDirectory).get(module.getInstallPackage().getFileName());
//        packagePo.setNexusAssetId(ipAsset.getId());
//        packagePo.setNexusRepository(this.appRepository);
//        this.appInstallPackageMapper.insert(packagePo);
//        for(DeployFileInfo cfg : module.getCfgs())
//        {
//            AppCfgFilePo cfgFilePo = new AppCfgFilePo();
//            cfgFilePo.setAppId(appId);
//            cfgFilePo.setCreateTime(now);
//            cfgFilePo.setDeployPath(module.getInstallPackage().getDeployPath());
//            cfgFilePo.setFileName(module.getInstallPackage().getExt());
//            cfgFilePo.setMd5(module.getInstallPackage().getFileMd5());
//            ipAsset = appFileAssetMap.get(appDirectory).get(module.getInstallPackage().getFileName());
//            cfgFilePo.setNexusAssetId(ipAsset.getId());
//            cfgFilePo.setNexusRepository(this.appRepository);
//            this.appCfgFileMapper.insert(cfgFilePo);
//        }
//        return appFileAssetMap;
//    }
//
//    private Map<String, Map<String, NexusAssetInfo>> addNewApp(String appDirectory, PlatformAppModuleVo module, Map<String, AppPo> appMap) throws Exception
//    {
//        Date now = new Date();
////        String appDirectory = String.format(this.appDirectoryFmt, module.getModuleName(), module.getModuleAliasName(), module.getVersion());
//        List<DeployFileInfo> appFiles = new ArrayList<>();
//        appFiles.add(module.getInstallPackage());
//        appFiles.addAll(Arrays.asList(module.getCfgs()));
//        Map<String, Map<String, NexusAssetInfo>> appFileAssetMap = this.nexusService.uploadRawComponent(this.appRepository, appDirectory, appFiles.toArray(new DeployFileInfo[0]));
//        AppPo appPo = new AppPo();
//        appPo.setAppAlias(module.getModuleAliasName());
//        appPo.setAppName(module.getModuleName());
//        appPo.setAppType(module.getModuleType());
//        appPo.setBasePath(module.getBasePath());
//        appPo.setCcodVersion(module.getCcodVersion());
//        appPo.setComment("");
//        appPo.setCreateReason("client collect");
//        appPo.setCreateTime(now);
//        appPo.setUpdateTime(now);
//        appPo.setVersion(module.getVersion());
//        this.appMapper.insert(appPo);
//        int appId = appPo.getAppId();
//        AppInstallPackagePo packagePo = new AppInstallPackagePo();
//        packagePo.setAppId(appId);
//        packagePo.setCreateTime(now);
//        packagePo.setDeployPath(module.getInstallPackage().getDeployPath());
//        packagePo.setFileName(module.getInstallPackage().getExt());
//        packagePo.setMd5(module.getInstallPackage().getFileMd5());
//        NexusAssetInfo ipAsset = appFileAssetMap.get(appDirectory).get(module.getInstallPackage().getFileName());
//        packagePo.setNexusAssetId(ipAsset.getId());
//        packagePo.setNexusRepository(this.appRepository);
//        this.appInstallPackageMapper.insert(packagePo);
//        for(DeployFileInfo cfg : module.getCfgs())
//        {
//            AppCfgFilePo cfgFilePo = new AppCfgFilePo();
//            cfgFilePo.setAppId(appId);
//            cfgFilePo.setCreateTime(now);
//            cfgFilePo.setDeployPath(module.getInstallPackage().getDeployPath());
//            cfgFilePo.setFileName(module.getInstallPackage().getExt());
//            cfgFilePo.setMd5(module.getInstallPackage().getFileMd5());
//            ipAsset = appFileAssetMap.get(appDirectory).get(module.getInstallPackage().getFileName());
//            cfgFilePo.setNexusAssetId(ipAsset.getId());
//            cfgFilePo.setNexusRepository(this.appRepository);
//            this.appCfgFileMapper.insert(cfgFilePo);
//        }
//        appMap.put(appDirectory, appPo);
//        return appFileAssetMap;
//    }

    @Override
    public void createNewPlatformAppDataCollectTask(String platformId, String domainId, String hostIp, String appName, String version) throws Exception {
        logger.info(String.format("begin to create platformId=%s, domainId=%s, hostIp=%s, appName=%s, version=%s app collect task",
                platformId, domainId, hostIp, appName, version));
        if(this.isPlatformCheckOngoing)
        {
            logger.error(String.format("create platform collect task FaIL : some collect task is ongoing"));
            throw new Exception(String.format("create platform collect task FaIL : some collect task is ongoing"));
        }
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Thread taskThread = new Thread(new Runnable(){
            @Override
            public void run() {
                try
                {
                    startCollectPlatformAppData(platformId, domainId, hostIp, appName, version);
                }
                catch (Exception ex)
                {
                    logger.error(String.format("start platformId=%s, domainId=%s, hostIp=%s, appName=%s, version=%s app collect task exception",
                            platformId, domainId, hostIp, appName, version), ex);
                }
            }
        });
        executor.execute(taskThread);
        executor.shutdown();
    }

    @Override
    public AppPo addNewAppFromPublishNexus(String appType, String appName, String appAlias, String version, String ccodVersion, AppModuleFileNexusInfo installPackage, AppModuleFileNexusInfo[] cfgs, String basePath) throws Exception {
        logger.info(String.format("prepare to add new app version from app publish nexus %s : appName=%s, appAlias=%s, version=%s, installPackage=%s, cfgs=%s",
                this.publishNexusHostUrl, appName, appAlias, version, JSONObject.toJSONString(installPackage),
                JSONArray.toJSONString(cfgs)));
        String appDirectory = String.format(this.appDirectoryFmt, appName, appAlias, version);
        Date now = new Date();
        List<DeployFileInfo> appFileInfoList = new ArrayList<>();
        DeployFileInfo pkgInfo = getFileFromTargetNexus(this.publishNexusHostUrl, this.publishNexusUserName,
                this.nexusPassword, installPackage);
        appFileInfoList.add(pkgInfo);
        for(AppModuleFileNexusInfo cfg : cfgs)
        {
            DeployFileInfo cfgInfo = getFileFromTargetNexus(this.publishNexusHostUrl, this.publishNexusUserName,
                    this.nexusPassword, cfg);
            appFileInfoList.add(cfgInfo);
        }
        Map<String, NexusAssetInfo> fileAssetMap = this.nexusService.uploadRawComponent(this.nexusHostUrl,
                this.nexusUserName, this.nexusPassword, this.appRepository, appDirectory, appFileInfoList.toArray(new DeployFileInfo[0]));
        AppPo appPo = new AppPo();
        appPo.setVersionControlUrl(installPackage.getNexusName());
        appPo.setVersionControl("nexus");
        appPo.setAppAlias(appAlias);
        appPo.setCcodVersion(ccodVersion);
        appPo.setAppType(appType);
        appPo.setVersion(version);
        appPo.setBasePath(basePath);
        appPo.setComment(String.format("version %s for %s", version, appName));
        appPo.setCreateReason("RELEASE");
        appPo.setCreateTime(now);
        appPo.setUpdateTime(now);
        this.appMapper.insert(appPo);
        AppInstallPackagePo installPackagePo = new AppInstallPackagePo();
        installPackagePo.setAppId(appPo.getAppId());
        installPackagePo.setExt(appFileInfoList.get(0).getExt());
        installPackagePo.setNexusDirectory(appDirectory);
        installPackagePo.setCreateTime(now);
        installPackagePo.setDeployPath(appFileInfoList.get(0).getDeployPath());
        installPackagePo.setFileName(appFileInfoList.get(0).getFileName());
        installPackagePo.setMd5(appFileInfoList.get(0).getFileMd5());
        installPackagePo.setNexusAssetId(appFileInfoList.get(0).getNexusAssetId());
        installPackagePo.setNexusRepository(this.appRepository);
        this.appInstallPackageMapper.insert(installPackagePo);
        for(int i = 1; i < appFileInfoList.size() - 1; i++)
        {
            AppCfgFilePo cfgFilePo = new AppCfgFilePo();
            cfgFilePo.setAppId(appPo.getAppId());
            cfgFilePo.setExt(appFileInfoList.get(i).getExt());
            cfgFilePo.setNexusDirectory(appDirectory);
            cfgFilePo.setCreateTime(now);
            cfgFilePo.setDeployPath(appFileInfoList.get(i).getDeployPath());
            cfgFilePo.setFileName(appFileInfoList.get(i).getFileName());
            cfgFilePo.setMd5(appFileInfoList.get(i).getFileMd5());
            cfgFilePo.setNexusAssetId(appFileInfoList.get(i).getNexusAssetId());
            cfgFilePo.setNexusRepository(this.appRepository);
            this.appCfgFileMapper.insert(cfgFilePo);
        }
        return appPo;
    }

    private DeployFileInfo getFileFromTargetNexus(String nexusUrl, String userName, String password, AppModuleFileNexusInfo nexusInfo) throws Exception
    {
        NexusAssetInfo assetInfo = this.nexusService.queryAssetByNexusName(nexusUrl, userName, password,
                nexusInfo.getRepository(), nexusInfo.getNexusName());
        String fileName = getFileNameFromDownloadUrl(assetInfo.getDownloadUrl());
        String repository = nexusInfo.getRepository();
        String directory = "/" + nexusInfo.getNexusName().replaceAll("/" + fileName + "$", "");
        String key = String.format("%s/%s%s", nexusUrl, repository, directory);
        String saveDir = String.format(this.tmpSaveDirFmt,
                System.getProperty("user.dir"), DigestUtils.md5DigestAsHex(key.getBytes()));
        saveDir = saveDir.replaceAll("\\\\", "/");
        String savePath = this.nexusService.downloadFile(userName, password,
                assetInfo.getDownloadUrl(), saveDir, fileName);
        DeployFileInfo info = new DeployFileInfo();
        info.setFileName(fileName);
        info.setLocalSavePath(savePath);
        String cfgMd5 = DigestUtils.md5DigestAsHex(new FileInputStream(savePath));
        String[] arr = fileName.split("\\.");
        String ext = arr.length>1 ? arr[arr.length-1] : "binary";
        info.setExt(ext);
        info.setFileMd5(cfgMd5);
        return info;
    }

    private String getFileNameFromDownloadUrl(String downloadUrl)
    {
        String url = downloadUrl;
        if(url.lastIndexOf("/") == url.length() - 1)
        {
            url = url.substring(0, downloadUrl.length()-1);
        }
        String[] arr = url.split("/");
        return arr[arr.length - 1];
    }

    /**
     * 将一条新的平台应用部署信息同时添加到蓝鲸paas以及本地数据库
     * @param platformId 部署应用的平台id
     * @param setId 部署该应用的域归属的set id
     * @param domainId 部署该应用的域id
     * @param bkBizId 平台对应的biz id
     * @param bkSet 部署该应用的set信息
     * @param moduleList set下的module定义列表
     * @param hostList 平台下面的所有主机列表
     * @param deployApp 部署的应用
     * @throws DataAccessException cmdb数据库访问异常
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    private void addNewDeployAppModule(String platformId, String setId, String domainId, int bkBizId, LJSetInfo bkSet,
                                       List<LJModuleInfo> moduleList, List<LJHostInfo> hostList,
                                       AppUpdateOperationInfo deployApp)
            throws DataAccessException, InterfaceCallException, LJPaasException
    {
        Date now = new Date();
        Map<String, LJModuleInfo> moduleMap = moduleList.stream().collect(Collectors.toMap(LJModuleInfo::getModuleName, Function.identity()));
        Map<Integer, LJHostInfo> hostMap = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostId, Function.identity()));
        /**
         * 如果新部署的应用的alias在蓝鲸paas的指定set没有定义，则需要将alias添加到set的module定义
         */
        if(!moduleMap.containsKey(deployApp.getAppAlias()))
        {
            LJModuleInfo module = paasService.addNewBkModule(bkBizId, bkSet.getSetId(), deployApp.getAppAlias());
            moduleList.add(module);
            moduleMap.put(deployApp.getAppAlias(), module);
        }
        //将该应用绑定到指定的host
        paasService.transferModulesToHost(bkBizId, new Integer[]{deployApp.getBzHostId()},
                new Integer[]{moduleMap.get(deployApp.getAppAlias()).getModuleId()}, true);
        //在数据库添加该应用记录
        PlatformAppPo po = new PlatformAppPo();
        po.setDeployTime(deployApp.getUpdateTime());
        po.setAppId(deployApp.getTargetAppId());
        po.setRunnerId(0);
        po.setServerId(0);
        po.setDomainId(domainId);
        po.setBasePath(deployApp.getBasePath());
        po.setPlatformId(platformId);
        po.setAppAlias(deployApp.getAppAlias());
        po.setBkBizId(bkBizId);
        po.setBkHostId(hostMap.get(deployApp.getBzHostId()).getHostId());
        po.setBkModuleId(moduleMap.get(deployApp.getAppAlias()).getModuleId());
        po.setBkSetId(bkSet.getSetId());
        po.setBkSetName(bkSet.getSetName());
        po.setAppRunner(deployApp.getAppRunner());
        po.setSetId(setId);
        platformAppMapper.insert(po);
        //将应用的配置文件添加到数据库
//        for(AppCfgFilePo cfg : deployApp.getCfgs())
//        {
//            PlatformAppCfgFilePo cfgFilePo = new PlatformAppCfgFilePo();
//            cfgFilePo.setDeployPath(cfg.getDeployPath());
//            cfgFilePo.setCreateTime(now);
//            cfgFilePo.setPlatformAppId(po.getPlatformAppId());
//            cfgFilePo.setNexusRepository(cfg.getNexusRepository());
//            cfgFilePo.setExt(cfg.getExt());
//            cfgFilePo.setFileName(cfg.getFileName());
//            cfgFilePo.setMd5(cfg.getMd5());
//            cfgFilePo.setNexusAssetId(cfg.getNexusAssetId());
//            cfgFilePo.setNexusDirectory(cfg.getNexusDirectory());
//            platformAppCfgFileMapper.insert(cfgFilePo);
//        }
    }

    private List<PlatformAppCfgFilePo> downloadAndUpdateCfg(List<NexusAssetInfo> cfgs)
    {
        List<PlatformAppCfgFilePo> cfgFileList = new ArrayList<>();
        return cfgFileList;
    }

    /**
     * 升级已有的应用模块
     * @param updateApp 模块升级操作信息
     * @throws DataAccessException cmdb数据库访问异常
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    private void updateDeployAppModule(AppUpdateOperationInfo updateApp)
            throws DataAccessException, InterfaceCallException, LJPaasException
    {
        Date now = new Date();
        PlatformAppPo deployApp = platformAppMapper.selectByPrimaryKey(updateApp.getPlatformAppId());
        deployApp.setAppId(updateApp.getTargetAppId());
        deployApp.setDeployTime(now);
        platformAppMapper.update(deployApp);
        //将应用的配置文件添加到数据库
//        for(AppCfgFilePo cfg : updateApp.getCfgs())
//        {
//            PlatformAppCfgFilePo cfgFilePo = new PlatformAppCfgFilePo();
//            cfgFilePo.setDeployPath(cfg.getDeployPath());
//            cfgFilePo.setCreateTime(now);
//            cfgFilePo.setPlatformAppId(deployApp.getPlatformAppId());
//            cfgFilePo.setNexusRepository(cfg.getNexusRepository());
//            cfgFilePo.setExt(cfg.getExt());
//            cfgFilePo.setFileName(cfg.getFileName());
//            cfgFilePo.setMd5(cfg.getMd5());
//            cfgFilePo.setNexusAssetId(cfg.getNexusAssetId());
//            cfgFilePo.setNexusDirectory(cfg.getNexusDirectory());
//            platformAppCfgFileMapper.insert(cfgFilePo);
//        }
    }

    @Test
    public void fileNameTest()
    {
        String url = "http://10.130.41.216:8081/service/rest/v1/search?repository=CCOD1&name=CCOD/MONITOR_MODULE/ivr/3.0.0.0/test1.ini";
        String fileName = getFileNameFromDownloadUrl(url);
        System.out.println(fileName);
    }

    @Test
    public void appParamTest()
    {
        AppParamVo paramVo = new AppParamVo();
        AppModuleFileNexusInfo installPackage = new AppModuleFileNexusInfo();
        installPackage.setDeployPath("./bin");
        installPackage.setNexusName("CCOD_DCMS/2.0.1.0/cas/be23fd608a/cas.war");
        installPackage.setMd5("43ce23175e26e87c8460717da6984678");
        installPackage.setRepository("CCOD");
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("installPackage", JSONObject.toJSONString(installPackage));
        List<AppModuleFileNexusInfo> cfgs = new ArrayList<>();
        AppModuleFileNexusInfo cfg = new AppModuleFileNexusInfo();
        cfg.setRepository("CCOD");
        cfg.setNexusName("DDSServer/dds/14551:27035/dds_config.cfg");
        cfg.setDeployPath("./cfg");
        cfg.setMd5("6198271820863529d2b025e985c03527");
        cfgs.add(cfg);
        cfg = new AppModuleFileNexusInfo();
        cfg.setRepository("CCOD");
        cfg.setNexusName("DDSServer/dds/14551:27035/dds_logger.cfg");
        cfg.setDeployPath("./cfg");
        cfg.setMd5("afdc1796d7a8e3e26b87fc3235f0c6bf");
        cfgs.add(cfg);
        dataMap.put("cfgs", JSONArray.toJSONString(cfgs.toArray(new AppModuleFileNexusInfo[0])));
        paramVo.setData(JSONObject.toJSONString(dataMap));
        paramVo.setAppAlias("cas");
        paramVo.setAppName("cas");
        paramVo.setAppType("CCOD_KERNEL_MODULE");
        paramVo.setBasePath("/home/platform");
        paramVo.setCcodVersion("4.5");
        paramVo.setMethod(1);
        paramVo.setVersion("3434:5656");
        System.out.println(JSONObject.toJSONString(paramVo));
    }
}
