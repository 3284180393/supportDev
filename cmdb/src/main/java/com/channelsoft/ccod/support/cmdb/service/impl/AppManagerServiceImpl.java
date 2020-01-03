package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.config.NotCheckCfgApp;
import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.dao.*;
import com.channelsoft.ccod.support.cmdb.exception.*;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import com.channelsoft.ccod.support.cmdb.service.ILJPaasService;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.channelsoft.ccod.support.cmdb.service.IPlatformAppCollectService;
import com.channelsoft.ccod.support.cmdb.utils.CMDBTools;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.stream.Collector;
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

    @Value("${lj-paas.idle-pool-set-name}")
    private String paasIdlePoolSetName;

    @Value("${test.demo-new-create-platform-id}")
    private String newDemoCreatePlatformId;

    @Value("${test.demo-new-create-platform-name}")
    private String newDemoCreatePlatformName;

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
    PlatformAppBkModuleMapper platformAppBkModuleMapper;

    @Autowired
    PlatformUpdateSchemaMapper platformUpdateSchemaMapper;

    @Autowired
    ILJPaasService paasService;

    @Autowired
    private NotCheckCfgApp notCheckCfgApp;

    private boolean isPlatformCheckOngoing = false;

    private Set<String> notCheckCfgAppSet;

    private final static Logger logger = LoggerFactory.getLogger(AppManagerServiceImpl.class);

    private Map<String, PlatformUpdateSchemaInfo> platformUpdateSchemaMap = new ConcurrentHashMap<>();

    @PostConstruct
    void init() throws  Exception
    {
        this.notCheckCfgAppSet = new HashSet<>(this.notCheckCfgApp.getNotCheckCfgApps());
        logger.info(String.format("%s will not check cfg count of app",
                JSONArray.toJSONString(this.notCheckCfgAppSet)));
        List<PlatformUpdateSchemaPo> schemaPoList = this.platformUpdateSchemaMapper.select();
        for(PlatformUpdateSchemaPo po : schemaPoList)
        {
            try
            {
                PlatformUpdateSchemaInfo schemaInfo = JSONObject.parseObject(po.getContext(), PlatformUpdateSchemaInfo.class);
                this.platformUpdateSchemaMap.put(po.getPlatformId(), schemaInfo);
            }
            catch (Exception ex)
            {
                logger.error(String.format("parse %s platform update schema exception", po.getPlatformId()), ex);
            }
        }
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
        List<PlatformAppDeployDetailVo> list = this.platformAppDeployDetailMapper.selectPlatformApps(platformId, domainId, hostIp);
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
    public PlatformAppModuleVo[] startCollectPlatformAppData(String platformId, String platformName, String domainId, String hostIp, String appName, String version) throws ParamException, Exception {
        PlatformPo platformPo = platformMapper.selectByPrimaryKey(platformId);
        if(platformPo != null && !platformPo.getPlatformName().equals(platformName))
        {
            logger.error(String.format("platformId=%s platform has been record in database, but its name is %s, not %s",
                    platformId, platformPo.getPlatformName(), platformName));
            throw new ParamException(String.format("platformId=%s platform has been record in database, but its name is %s, not %s",
                    platformId, platformPo.getPlatformName(), platformName));
        }
        if(this.isPlatformCheckOngoing)
        {
            logger.error(String.format("start platform=%s app data collect FAIL : some collect task is ongoing", platformId));
            throw new Exception(String.format("start platform=%s app data collect FAIL : some collect task is ongoing", platformId));
        }
        try
        {
            this.isPlatformCheckOngoing = true;
            List<PlatformAppModuleVo> modules = this.platformAppCollectService.collectPlatformAppData(platformId, platformName, domainId, hostIp, appName, version);
            Map<String, List<PlatformAppModuleVo>> platformAppModuleMap = modules.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getPlatformId));
            if(platformAppModuleMap.keySet().size() > 1)
            {
                logger.error(String.format("platformId=%s client collected data platformId not unique : [%s]",
                        platformId, String.join(",", platformAppModuleMap.keySet())));
                throw new ClientCollectDataException(String.format("platformId=%s client collected data platformId not unique : [%s]",
                        platformId, String.join(",", platformAppModuleMap.keySet())));
            }
            else if(!platformAppModuleMap.containsKey(platformId))
            {
                logger.error(String.format("platformId=%s client collected data's platformId is %s",
                        platformId, JSONObject.toJSONString(platformAppModuleMap.keySet())));
                throw new ClientCollectDataException(String.format("platformId=%s client collected data's platformId is %s",
                        platformId, JSONObject.toJSONString(platformAppModuleMap.keySet())));
            }
            platformAppModuleMap = modules.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getPlatformName));
            if(platformAppModuleMap.keySet().size() > 1)
            {
                logger.error(String.format("platformName=%s client collected data platformName not unique : [%s]",
                        platformName, String.join(",", platformAppModuleMap.keySet())));
                throw new ClientCollectDataException(String.format("platformName=%s client collected data platformName not unique : [%s]",
                        platformName, String.join(",", platformAppModuleMap.keySet())));
            }
            else if(!platformAppModuleMap.containsKey(platformName))
            {
                logger.error(String.format("platformName=%s client collected data's platformName is %s",
                        platformName, JSONObject.toJSONString(platformAppModuleMap.keySet())));
                throw new ClientCollectDataException(String.format("platformName=%s client collected data's platformName is %s",
                        platformName, JSONObject.toJSONString(platformAppModuleMap.keySet())));
            }
            Map<String, List<PlatformAppModuleVo>> domainAppMap = modules.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getDomainId));
            Map<String, DomainPo> domainMap = domainMapper.select(platformId, null).stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
            for(String reportDomainId : domainAppMap.keySet())
            {
                Map<String, PlatformAppModuleVo> domainNameAppModuleMap = domainAppMap.get(reportDomainId)
                        .stream().collect(Collectors.toMap(PlatformAppModuleVo::getDomainName, Function.identity()));
                if(domainNameAppModuleMap.size() > 1)
                {
                    logger.error(String.format("domainId=%s has not unique domainName=[%s]",
                            reportDomainId, String.join(",", domainNameAppModuleMap.keySet())));
                    throw new ClientCollectDataException(String.format("domainId=%s has not unique domainName=[%s]",
                            reportDomainId, String.join(",", domainNameAppModuleMap.keySet())));
                }
                String reportDomainName = domainAppMap.get(reportDomainId).get(0).getDomainName();
                if(domainMap.containsKey(reportDomainId) && domainMap.get(reportDomainId).getDomainName().equals(reportDomainName))
                {
                    logger.error(String.format("domainId=%s is record in database, but its name=%s not %s",
                            reportDomainId, domainMap.get(reportDomainId).getDomainName(), reportDomainName));
                    throw new ClientCollectDataException(String.format("domainId=%s is record in database, but its name=%s not %s",
                            reportDomainId, domainMap.get(reportDomainId).getDomainName(), reportDomainName));
                }
            }
            if(platformPo == null)
            {
                platformPo = modules.get(0).getPlatform();
                platformMapper.insert(platformPo);
            }
            for(List<PlatformAppModuleVo> domainModules : domainAppMap.values())
            {
                DomainPo domainPo = domainModules.get(0).getDomain();
                if(!domainMap.containsKey(domainPo.getDomainId()))
                {
                    domainMapper.insert(domainPo);
                }

            }
            Map<String, AppModuleVo> appModuleMap = new HashMap<>();
            handleCollectedPlatformAppModules(appModuleMap, modules.toArray(new PlatformAppModuleVo[0]));
            return modules.toArray(new PlatformAppModuleVo[0]);
        }
        finally {
            this.isPlatformCheckOngoing = false;
        }

    }

    @Override
    public PlatformAppModuleVo[] startCheckPlatformAppData(String platformId, String platformName, String domainName, String hostIp, String appName, String version) throws Exception {
        if(this.isPlatformCheckOngoing)
        {
            logger.error(String.format("start platform=%s app data check FAIL : some collect task is ongoing", platformId));
            throw new Exception(String.format("start platform=%s app data check FAIL : some collect task is ongoing", platformId));
        }
        this.isPlatformCheckOngoing = true;
        List<PlatformAppModuleVo> modules = this.platformAppCollectService.collectPlatformAppData(platformId, platformName, domainName, hostIp, appName, version);
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
    void handleCollectedPlatformAppModules(Map<String, AppModuleVo> appModuleMap, PlatformAppModuleVo[] modules) throws Exception
    {
        Map<String, List<NexusAssetInfo>> appFileAssetMap = new HashMap<>();
        for(PlatformAppModuleVo module : modules)
        {
            try
            {
                boolean handleSucc = handlePlatformAppModule(module, appModuleMap, appFileAssetMap);
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
     * @param appModuleMap 已经处理的应用模块
     * @param appFileAssetMap 已经处理的应用模块在nexus的存储记录
     * @return 添加后的模块
     * @throws DataAccessException 查询数据库异常
     * @throws InterfaceCallException 调用nexus接口异常
     * @throws NexusException nexus返回调用失败信息或是解析nexus调用结果失败
     * @throws DBNexusNotConsistentException cmdb记录的信息和nexus不一致
     */
    private boolean handlePlatformAppModule(PlatformAppModuleVo module, Map<String, AppModuleVo> appModuleMap,
                                            Map<String, List<NexusAssetInfo>> appFileAssetMap)
            throws DataAccessException, InterfaceCallException, NexusException, DBNexusNotConsistentException
    {
        AppPo appPo = module.getAppInfo();
        String appName = appPo.getAppName();
        String appVersion = appPo.getVersion();
        String appDirectory = CMDBTools.getAppDirectory(appPo);
        AppModuleVo moduleVo;
        if(appModuleMap.containsKey(appDirectory))
        {
            moduleVo = appModuleMap.get(appDirectory);
        }
        else
        {
            moduleVo = appModuleMapper.selectByNameAndVersion(appName, appVersion);
        }
        List<NexusAssetInfo> appFileAssetList;
        if(appFileAssetMap.containsKey(appDirectory))
        {
            appFileAssetList = appFileAssetMap.get(appDirectory);
        }
        else
        {
            String group = String.format("/%s/%s", appName, appVersion);
            appFileAssetList = this.nexusService.queryGroupAssetMap(this.nexusHostUrl, this.nexusUserName, nexusPassword, this.appRepository, group);
        }
        if(moduleVo != null && appFileAssetList.size() == 0)
        {
            logger.error(String.format("cmdb data is not matched with nexus : cmdb has appName=%s and version=%s record but nexus has not",
                    appName, appVersion));
            throw new DBNexusNotConsistentException(String.format("cmdb data is not matched with nexus : cmdb has appName=%s and version=%s record but nexus has not",
                    appName, appVersion));
        }
        if(moduleVo == null && appFileAssetList.size() > 0)
        {
            logger.error(String.format("cmdb data is not matched with nexus : nexus has appName=%s and version=%s record but cmdb has not",
                    appName, appVersion));
            throw new DBNexusNotConsistentException(String.format("cmdb data is not matched with nexus : nexus has appName=%s and version=%s record but cmdb has not",
                    appName, appVersion));
        }
        if(appFileAssetList.size() == 0)
        {
            appFileAssetList = addAppToNexus(appName, appVersion, module.getInstallPackage(), module.getCfgs(), this.appRepository, appDirectory);
            appFileAssetMap.put(appDirectory, appFileAssetList);
        }
        if(moduleVo == null)
        {
            moduleVo = addNewAppToDB(appPo, module.getInstallPackage(), module.getCfgs());
            appModuleMap.put(appDirectory, moduleVo);
        }
        if(StringUtils.isBlank(module.getInstallPackage().getLocalSavePath()))
        {
            logger.error(String.format("handle [%s] FAIL : not receive install package[%s]",
                    module.toString(), module.getInstallPackage().getFileName()));
            return false;
        }
        String compareResult = compareAppFileWithNexusRecord(appName, appVersion, moduleVo.getInstallPackage(), moduleVo.getCfgs(), appFileAssetList);
        if(StringUtils.isNotBlank(compareResult))
        {
            logger.error(String.format("collected appName=%s and version=%s files is not matched with nexus : %s",
                    appName, appVersion, compareResult));
            throw new DBNexusNotConsistentException(String.format("collected appName=%s and version=%s files is not matched with nexus : %s",
                    appName, appVersion, compareResult));
        }
        PlatformAppPo platformApp = module.getPlatformApp();
        platformApp.setAppId(appPo.getAppId());
        this.platformAppMapper.insert(platformApp);
        for(DeployFileInfo cfgFilePo : module.getCfgs())
        {
            PlatformAppCfgFilePo po = new PlatformAppCfgFilePo(platformApp.getPlatformAppId(), cfgFilePo);
            this.platformAppCfgFileMapper.insert(po);
        }
        logger.info(String.format("[%s] platform app module handle SUCCESS", module.toString()));
        return true;
    }

    private AppModuleVo addNewAppToDB(AppPo app, DeployFileInfo installPackage, DeployFileInfo[] cfgs) throws DataAccessException
    {
        if(debug)
        {
            if(StringUtils.isBlank(app.getAppType()))
                app.setAppType(AppType.CCOD_KERNEL_MODULE.name);
            if(StringUtils.isBlank(app.getCcodVersion()))
                app.setCcodVersion("4.5");
        }
        this.appMapper.insert(app);
        AppInstallPackagePo instPkgPo = new AppInstallPackagePo(app.getAppId(), installPackage);
        this.appInstallPackageMapper.insert(instPkgPo);
        List<AppCfgFilePo> cfgList = new ArrayList<>();
        for(DeployFileInfo cfg : cfgs)
        {
            AppCfgFilePo cfgFilePo = new AppCfgFilePo(app.getAppId(), cfg);
            this.appCfgFileMapper.insert(cfgFilePo);
            cfgList.add(cfgFilePo);
        }
        AppModuleVo moduleVo = new AppModuleVo(app, instPkgPo, cfgList);
        return moduleVo;
    }

    private List<NexusAssetInfo> addAppToNexus(String appName, String version, DeployFileInfo installPackage, DeployFileInfo[] cfgs, String repository, String directory) throws InterfaceCallException, NexusException
    {
        logger.info(String.format("prepare to upload appName=%s and version=%s app to directory=%s at repository=%s",
                appName, version, directory, repository));
        List<DeployFileInfo> uploadFiles = new ArrayList<>();
        uploadFiles.add(installPackage);
        uploadFiles.addAll(Arrays.asList(cfgs));
        List<NexusAssetInfo> fileAssetList = this.nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, repository, directory, uploadFiles.toArray(new DeployFileInfo[0]));
        return fileAssetList;
    }


    /**
     * 将安装包和配置文件和nexus存储的信息
     * @param appName 应用名
     * @param version 应用版本
     * @param installPackage 安装包
     * @param cfgs 配置文件
     * @param NexusFileList 该应用存储在nexus的文件信息
     * @return 对比结果,如果完全符合返回""
     */
    private String compareAppFileWithNexusRecord(String appName, String version, AppInstallPackagePo installPackage, List<AppCfgFilePo> cfgs, List<NexusAssetInfo> NexusFileList)
    {
        logger.debug(String.format("begin to compare appName=%s and version=%s installPackage and cfgs with nexus record",
                appName, version));
        Map<String, NexusAssetInfo> nexusFileMap = NexusFileList.stream().collect(Collectors.toMap(NexusAssetInfo::getPath, Function.identity()));
        StringBuffer sb = new StringBuffer();
        String path = String.format("%s/%s/%s", appName, version, installPackage.getFileName());
        if(!nexusFileMap.containsKey(path))
        {
            sb.append(String.format("installPackageFile=%s not in nexus record,", installPackage.getFileName()));;
        }
        else if(!nexusFileMap.get(path).getMd5().equals(installPackage.getMd5()))
        {
            sb.append(String.format("installPackage=%s md5=%s is not equal with nexus md5=%s,"
                    ,installPackage.getFileName(), installPackage.getMd5(), nexusFileMap.get(path).getMd5()));;
        }
        if(!this.notCheckCfgAppSet.contains(appName))
        {
            if(cfgs.size() != nexusFileMap.size() -1)
            {
                sb.append(String.format("cfg count is %d but nexus is %d,", cfgs.size(), nexusFileMap.size() -1));
            }
            else
            {
                for(AppCfgFilePo cfg : cfgs)
                {
                    path = String.format("%s/%s/%s", appName, version, cfg.getFileName());
                    if(!nexusFileMap.containsKey(path))
                    {
                        sb.append(String.format("cfg=%s not in nexus,", cfg.getFileName()));
                    }
                }
            }
        }
        logger.info(String.format("the result of files of appName=%s with version=%s compare with nexus is : %s", sb.toString()));
        return sb.toString().replaceAll(",$", "");
    }

    @Override
    public void createNewPlatformAppDataCollectTask(String platformId, String platformName, String domainId, String hostIp, String appName, String version) throws Exception {
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
                    startCollectPlatformAppData(platformId, platformName, domainId, hostIp, appName, version);
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
        AppPo appPo = new AppPo();
        appPo.setAppName(appName);
        appPo.setAppAlias(appAlias);
        appPo.setVersion(version);
        String appDirectory = CMDBTools.getAppDirectory(appPo);
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
        appPo.setVersionControlUrl(installPackage.getNexusName());
        appPo.setVersionControl("nexus");

        appPo.setCcodVersion(ccodVersion);
        appPo.setAppType(appType);

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
        String fileName = CMDBTools.getFileNameFromDownloadUrl(assetInfo.getDownloadUrl());
        String repository = nexusInfo.getRepository();
        String directory = CMDBTools.getDirectoryFromAppModuleFileNexusInfo(nexusInfo);
        String key = String.format("%s/%s%s", nexusUrl, repository, directory);
        String saveDir = CMDBTools.getTempSaveDir(DigestUtils.md5DigestAsHex(key.getBytes()));
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



    /**
     * 部署应用到域主机
     * @param domain 部署应用的域信息
     * @param setId 部署该应用的域归属的set id
     * @param bkSet 部署该应用的set信息
     * @param hostList 平台下面的所有主机列表
     * @param deployOperationList 部署的操作
     * @param appList 应用记录列表
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    private void deployAppsToDomainHost(DomainPo domain, String setId, LJSetInfo bkSet, List<LJHostInfo> hostList,
                                        List<AppUpdateOperationInfo> deployOperationList, List<AppModuleVo> appList)
            throws InterfaceCallException, LJPaasException, NexusException, IOException
    {
        logger.debug(String.format("begin to add new %d apps to %s/%s(%s)/%s(%s)", deployOperationList.size(),
                domain.getPlatformId(), setId, bkSet.getBkSetName(), domain.getDomainId(), domain.getDomainName()));
        Date now = new Date();
        List<PlatformAppPo> deployAppList = new ArrayList<>();
        Map<String, LJHostInfo> hostMap = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity()));
        Map<String, List<AppModuleVo>> appMap = appList.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        for(AppUpdateOperationInfo deployOperationInfo : deployOperationList)
        {
            Map<String, AppModuleVo> versionMap = appMap.get(deployOperationInfo.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
            AppModuleVo appPo = versionMap.get(deployOperationInfo.getTargetVersion());
            LJHostInfo bkHost = hostMap.get(deployOperationInfo.getHostIp());
            logger.debug(String.format("insert appId=%d to hostIp=%s with appAlias=%s and deployDirectory=%s to database",
                    appPo.getAppId(), bkHost.getHostInnerIp(), deployOperationInfo.getAppAlias(), deployOperationInfo.getBasePath()));
            PlatformAppPo deployApp = new PlatformAppPo();
            deployApp.setAppAlias(deployOperationInfo.getAppAlias());
            deployApp.setHostIp(bkHost.getHostInnerIp());
            deployApp.setAppRunner(deployOperationInfo.getAppRunner());
            deployApp.setDeployTime(now);
            deployApp.setAppId(appPo.getAppId());
            deployApp.setPlatformId(domain.getPlatformId());
            deployApp.setBasePath(deployOperationInfo.getBasePath());
            deployApp.setDomainId(domain.getDomainId());
            platformAppMapper.insert(deployApp);
            deployAppList.add(deployApp);
            handlePlatformAppCfgs(deployApp, appPo, deployOperationInfo.getCfgs());
        }
        paasService.bindDeployAppsToBizSet(bkSet.getBkBizId(), setId, bkSet.getBkSetId(), bkSet.getBkSetName(), deployAppList);
    }

    /**
     * 处理平台应用配置文件
     * 处理流程:首先下载配置文件,其次将下载的配置文件按一定格式上传到nexus去，最后入库
     * @param platformAppPo 平台应用
     * @param appPo 该平台应用对应的app详情
     * @param cfgs 配置文件信息
     * @throws InterfaceCallException 接口调用失败
     * @throws NexusException nexus返回失败或是处理nexus返回信息失败
     * @throws IOException 保存文件失败
     */
    private void handlePlatformAppCfgs(PlatformAppPo platformAppPo, AppModuleVo appPo, List<AppFileNexusInfo> cfgs) throws InterfaceCallException, NexusException, IOException
    {
        List<DeployFileInfo> fileList = new ArrayList<>();
        List<PlatformAppCfgFilePo> cfgFileList = new ArrayList<>();
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String directory = CMDBTools.getPlatformAppDirectory(appPo, platformAppPo);
        logger.debug(String.format("begin to handle %s cfgs", directory));
        String tmpSaveDir = CMDBTools.getTempSaveDir(DigestUtils.md5DigestAsHex(directory.getBytes()));
        for(AppFileNexusInfo cfg : cfgs)
        {
            String downloadUrl = CMDBTools.getAppFileDownloadUrl(this.nexusHostUrl, cfg);
            logger.debug(String.format("download cfg from %s", downloadUrl));
            String savePth = nexusService.downloadFile(this.nexusUserName, this.nexusPassword, downloadUrl, tmpSaveDir, cfg.getFileName());
            DeployFileInfo fileInfo = new DeployFileInfo();
            fileInfo.setExt(cfg.getExt());
            fileInfo.setFileMd5(cfg.getMd5());
            fileInfo.setLocalSavePath(savePth);
            fileInfo.setNexusRepository(this.appRepository);
            fileInfo.setNexusDirectory(directory);
            fileInfo.setFileSize(cfg.getFileSize());
            fileInfo.setFileName(cfg.getFileName());
            fileList.add(fileInfo);
            PlatformAppCfgFilePo cfgFilePo = new PlatformAppCfgFilePo();
            cfgFilePo.setMd5(cfg.getMd5());
            cfgFilePo.setFileName(cfg.getFileName());
            cfgFilePo.setExt(cfg.getExt());
            cfgFilePo.setPlatformAppId(platformAppPo.getPlatformAppId());
            cfgFilePo.setCreateTime(now);
            cfgFilePo.setDeployPath(cfg.getDeployPath());
            cfgFilePo.setNexusDirectory(directory);
            cfgFilePo.setNexusRepository(this.platformAppCfgRepository);
            cfgFileList.add(cfgFilePo);
        }
        logger.debug(String.format("upload platform app cfgs to %s/%s/%s", nexusHostUrl, this.platformAppCfgRepository, directory));
        Map<String, NexusAssetInfo> assetMap = nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, this.platformAppCfgRepository, directory, fileList.toArray(new DeployFileInfo[0]))
                .stream().collect(Collectors.toMap(NexusAssetInfo::getPath, Function.identity()));
        logger.debug(String.format("delete platformAppId=%d record from platform_app_cfg_file", platformAppPo.getPlatformAppId()));
        platformAppCfgFileMapper.delete(null, platformAppPo.getPlatformAppId());
        for(PlatformAppCfgFilePo cfgFilePo : cfgFileList)
        {
            String path = String.format("%s/%s", directory, cfgFilePo.getFileName());
            cfgFilePo.setNexusAssetId(assetMap.get(path).getId());
            logger.debug(String.format("insert %s cfg into platform_app_cfg_file", path));
            this.platformAppCfgFileMapper.insert(cfgFilePo);
        }
        logger.info(String.format("handle %s cfgs SUCCESS", directory));
    }


    private void addNewDomainToPlatform(String domainId, String domainName, List<AppUpdateOperationInfo> domainApps,
                              List<AppModuleVo> appList, PlatformPo platform, String setId, LJSetInfo bkSet, List<LJHostInfo> bkHostList)
            throws InterfaceCallException, LJPaasException, NexusException, IOException
    {
        Date now = new Date();
        DomainPo newDomain = new DomainPo();
        newDomain.setComment("");
        newDomain.setDomainId(domainId);
        newDomain.setPlatformId(platform.getPlatformId());
        newDomain.setStatus(1);
        newDomain.setUpdateTime(now);
        newDomain.setCreateTime(now);
        newDomain.setDomainName(domainName);
        this.domainMapper.insert(newDomain);
        deployAppsToDomainHost(newDomain, setId, bkSet, bkHostList, domainApps, appList);
    }

    /**
     * 将指定域的一组应用删除
     * @param domain 指定删除应用的域
     * @param bkSet 该域归属的蓝鲸paas的set
     * @param deleteOperationList 需要移除的应用操作列表
     * @param appBkModuleList 被移除的应用对应的蓝鲸模块关系列表
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果失败
     */
    private void removeAppsFromDomainHost(DomainPo domain, LJSetInfo bkSet, List<AppUpdateOperationInfo> deleteOperationList, List<PlatformAppBkModulePo> appBkModuleList)
            throws InterfaceCallException, LJPaasException
    {
        logger.debug(String.format("begin to remove %d apps from platformId=%s and domainId=%s",
                deleteOperationList.size(), domain.getPlatformId(), domain.getDomainId()));
        List<PlatformAppBkModulePo> removedAppBkModuleList = new ArrayList<>();
        Map<Integer, PlatformAppBkModulePo> appBkModuleMap = appBkModuleList.stream().collect(Collectors.toMap(PlatformAppBkModulePo::getPlatformAppId, Function.identity()));
        for(AppUpdateOperationInfo deleteOperationInfo : deleteOperationList)
        {
            removedAppBkModuleList.add(appBkModuleMap.get(deleteOperationInfo.getPlatformAppId()));
        }
        this.paasService.disBindDeployAppsToBizSet(bkSet.getBkBizId(), bkSet.getBkSetId(), removedAppBkModuleList);
        for(AppUpdateOperationInfo deleteOperationInfo : deleteOperationList)
        {
            logger.debug(String.format("delete id=%d platform app cfg record", deleteOperationInfo.getPlatformAppId()));
            platformAppCfgFileMapper.delete(null, deleteOperationInfo.getPlatformAppId());
            logger.debug(String.format("delete id=%d platform app record", deleteOperationInfo.getPlatformAppId()));
            platformAppMapper.delete(deleteOperationInfo.getPlatformAppId(), null, null);
        }
        logger.debug(String.format("remove %d apps from platformId=%s and domainId=%s SUCCESS",
                removedAppBkModuleList.size(), domain.getPlatformId(), domain.getDomainId()));
    }

    /**
     * 检查平台升级计划参数是否合法
     * @param schema 需要检查的平台升级计划
     * @param domainList 该平台下的所有域
     * @param appList 相关的应用列表
     * @param deployApps 该平台已经部署的应用
     * @param bkSetList 该平台的对应的蓝鲸biz下的所有set列表
     * @param bkHostList 该平台下所有主机
     * @param appBkModuleList 该平台升级签部署的应用和蓝鲸模块的关系表
     * @return 如果检查通过返回空，否则返回检查失败原因
     */
    private String checkPlatformUpdateTask(PlatformUpdateSchemaInfo schema, List<DomainPo> domainList,
                                           List<AppModuleVo> appList, List<PlatformAppPo> deployApps,
                                           List<LJSetInfo> bkSetList, List<LJHostInfo> bkHostList,
                                           List<PlatformAppBkModulePo> appBkModuleList)
    {
        if(schema.getDomainUpdatePlanList() == null)
        {
            schema.setDomainUpdatePlanList(new ArrayList<>());
        }
        Map<String, DomainPo> domainMap = domainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        Map<String, LJSetInfo> setMap = bkSetList.stream().collect(Collectors.toMap(LJSetInfo::getBkSetName, Function.identity()));
        PlatformUpdateTaskType taskType = schema.getTaskType();
        StringBuffer sb = new StringBuffer();
        List<AppUpdateOperationInfo> allOperationList = new ArrayList<>();
        for(DomainUpdatePlanInfo planInfo : schema.getDomainUpdatePlanList())
        {
            DomainUpdateType updateType = planInfo.getUpdateType();
            if(updateType == null)
            {
                sb.append(String.format("DomainUpdateType of %s is blank;", JSONObject.toJSONString(planInfo)));
            }
            else if(PlatformUpdateTaskType.CREATE.equals(taskType))
            {
                if(!DomainUpdateType.ADD.equals(updateType))
                {
                    sb.append(String.format("%s of %s only support DomainUpdateType=%s, not %s",
                            taskType.name, JSONObject.toJSONString(planInfo), DomainUpdateType.ADD.name, updateType.name));
                }
            }
            else if(PlatformUpdateTaskType.DELETE.equals(taskType))
            {
                sb.append(String.format("current version not support PlatformUpdateTaskType=%s of %s", taskType.name, JSONObject.toJSONString(planInfo)));
            }
            if(planInfo.getAppUpdateOperationList() == null)
            {
                planInfo.setAppUpdateOperationList(new ArrayList<>());
            }
            allOperationList.addAll(planInfo.getAppUpdateOperationList());
            switch (taskType)
            {
                case CREATE:
                    switch (updateType)
                    {
                        case ADD:
                            if(domainMap.containsKey(planInfo.getDomainId()))
                            {
                                sb.append(String.format("new add domain[%s] of %s has exist;",
                                        planInfo.getDomainId(), JSONObject.toJSONString(planInfo)));
                            }
                            else if(!setMap.containsKey(planInfo.getBkSetName()))
                            {
                                sb.append(String.format("bkSetName=%s of new add domain %s not exist;",
                                        planInfo.getBkSetName(), JSONObject.toJSONString(planInfo)));
                            }
                            else
                            {
                                String operationCheckResult = updateOperationCheck(updateType, planInfo.getAppUpdateOperationList(), appList, deployApps, appBkModuleList, bkHostList);
                                sb.append(operationCheckResult);
                            }
                            break;
                        default:
                            sb.append(String.format("CREATE platform only support ADD new domain not %s of %s;",
                                    updateType.name, JSONObject.toJSONString(planInfo)));
                    }
                    break;
                case UPDATE:
                    switch (updateType)
                    {
                        case ADD:
                            if(domainMap.containsKey(planInfo.getDomainId()))
                            {
                                sb.append(String.format("new add domain[%s] of %s has exist;",
                                        planInfo.getDomainId(), JSONObject.toJSONString(planInfo)));
                            }
                            else if(!setMap.containsKey(planInfo.getBkSetName()))
                            {
                                sb.append(String.format("bkSetName=%s of new add domain %s not exist;",
                                        planInfo.getBkSetName(), JSONObject.toJSONString(planInfo)));
                            }
                            else
                            {
                                String operationCheckResult = updateOperationCheck(updateType, planInfo.getAppUpdateOperationList(), appList, deployApps, appBkModuleList, bkHostList);
                                sb.append(operationCheckResult);
                            }
                            break;
                        case UPDATE:
                        case DELETE:
                            if(!domainMap.containsKey(planInfo.getDomainId()))
                            {
                                sb.append(String.format("%s domain[%s] of %s not exist;",
                                        updateType.name, planInfo.getDomainId(), JSONObject.toJSONString(planInfo)));
                            }
                            else if(!setMap.containsKey(planInfo.getBkSetName()))
                            {
                                sb.append(String.format("bkSetName=%s of %s domain %s not exist;",
                                        planInfo.getBkSetName(), updateType.name, JSONObject.toJSONString(planInfo)));
                            }
                            else
                            {
                                String operationCheckResult = updateOperationCheck(updateType, planInfo.getAppUpdateOperationList(), appList, deployApps, appBkModuleList, bkHostList);
                                sb.append(operationCheckResult);
                            }
                            break;
                        default:
                    }
                    break;
                default:
            }
        }
        Map<String, List<AppUpdateOperationInfo>> ipAppMap = allOperationList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getHostIp));
        for(String hostIp : ipAppMap.keySet())
        {
            Map<String, List<AppUpdateOperationInfo>> appNameMap = ipAppMap.get(hostIp).stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
            for(String appName : appNameMap.keySet())
            {
                if(appNameMap.get(appName).size() > 1)
                {
                    Set<String> aliasSet = new HashSet<>();
                    for(AppUpdateOperationInfo opt : appNameMap.get(appName))
                    {
                        if(aliasSet.contains(opt.getAppAlias()))
                        {
                            sb.append(String.format("%s at %s alias duplicate;", appName, hostIp));
                            break;
                        }
                        aliasSet.add(opt.getAppAlias());
                    }
                }
            }
        }
        return sb.toString();
    }


    /**
     * 验证应用升级操作的相关蚕食是否正确
     * @param updateType 域升级方案类型
     * @param operationList 域升级的应用升级操作明细
     * @param appList 当前应用列表
     * @param deployApps 平台升级前已经部署的应用列表
     * @param appBkModuleList 已经部署的应用和蓝鲸模块的关系表
     * @param hostList 平台下所有服务器列表
     * @return 检查结果,如果为空则表示检查通过，否则返回检查失败原因
     */
    private String updateOperationCheck(DomainUpdateType updateType, List<AppUpdateOperationInfo> operationList, List<AppModuleVo> appList, List<PlatformAppPo> deployApps, List<PlatformAppBkModulePo> appBkModuleList, List<LJHostInfo> hostList)
    {
        Map<String, LJHostInfo> hostMap = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity()));
        Map<String, List<AppModuleVo>> appMap = appList.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        Map<Integer, PlatformAppBkModulePo> appBkModuleMap = appBkModuleList.stream().collect(Collectors.toMap(PlatformAppBkModulePo::getPlatformAppId, Function.identity()));
        Map<Integer, PlatformAppPo> platformAppMap = deployApps.stream().collect(Collectors.toMap(PlatformAppPo::getPlatformAppId, Function.identity()));
        StringBuffer sb = new StringBuffer();
        for(AppUpdateOperationInfo operationInfo : operationList)
        {
            if(operationInfo.getOperation() == null)
            {
                sb.append(String.format("operationType of %s is blank;", JSONObject.toJSONString(operationInfo)));
            }
            else if(AppUpdateOperation.START.equals(operationInfo.getOperation()) || AppUpdateOperation.STOP.equals(operationInfo.getOperation()))
            {
                sb.append(String.format("%s of %s is not support now", operationInfo.getOperation().name, JSONObject.toJSONString(operationInfo)));
            }
            else if(DomainUpdateType.ADD.equals(updateType) && !AppUpdateOperation.ADD.equals(operationInfo.getOperation()))
            {
                sb.append(String.format("DomainUpdateType=%s only support AppUpdateOperation=%s, not %s of %s;",
                        DomainUpdateType.ADD.name, AppUpdateOperation.ADD.name, operationInfo.getOperation().name, JSONObject.toJSONString(operationInfo)));
            }
            else if(DomainUpdateType.DELETE.equals(updateType) && !AppUpdateOperation.DELETE.equals(operationInfo.getOperation()))
            {
                sb.append(String.format("DomainUpdateType=%s only support AppUpdateOperation=%s, not %s of %s;",
                        DomainUpdateType.DELETE.name, AppUpdateOperation.DELETE.name, operationInfo.getOperation().name, JSONObject.toJSONString(operationInfo)));
            }
            else if(!appMap.containsKey(operationInfo.getAppName()))
            {
                sb.append(String.format("appName %s of %s not exist;", operationInfo.getAppName(), JSONObject.toJSONString(operationInfo)));
            }
            else if(StringUtils.isBlank(operationInfo.getHostIp()))
            {
                sb.append(String.format("host of %s is blank;", JSONObject.toJSONString(operationInfo)));
            }
            else if(!hostMap.containsKey(operationInfo.getHostIp()))
            {
                sb.append(String.format("host %s of %s not exist;", operationInfo.getHostIp(), JSONObject.toJSONString(operationInfo)));
            }
            else
            {
                Map<String, AppModuleVo> versionMap = appMap.get(operationInfo.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
                AppUpdateOperation operation = operationInfo.getOperation();
                switch (operation)
                {
                    case ADD:
                        if(StringUtils.isBlank(operationInfo.getTargetVersion()))
                        {
                            sb.append(String.format("%s : version of %s is blank;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        else if(!versionMap.containsKey(operationInfo.getTargetVersion()))
                        {
                            sb.append(String.format("%s : version %s of %s not exist;", operation.name, operationInfo.getTargetVersion(), JSONObject.toJSONString(operationInfo)));
                        }
                        if(StringUtils.isBlank(operationInfo.getAppAlias()))
                        {
                            sb.append(String.format("%s : alias of %s is blank;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        if(StringUtils.isBlank(operationInfo.getBasePath()))
                        {
                            sb.append(String.format("%s : basePath of %s is blank;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        if(StringUtils.isBlank(operationInfo.getAppRunner()))
                        {
                            sb.append(String.format("%s : appRunner of %s is blank;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        if(operationInfo.getCfgs() == null || operationInfo.getCfgs().size() == 0)
                        {
                            sb.append(String.format("%s : cfg of %s is 0;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        else
                        {
                            String compareResult = cfgFileCompare(versionMap.get(operationInfo.getTargetVersion()), operationInfo.getCfgs());
                            if(StringUtils.isNotBlank(compareResult))
                                sb.append(String.format("%s : cfg of %s is not match [%s];", operation.name, JSONObject.toJSONString(operationInfo), compareResult));
                        }
                        break;
                    case VERSION_UPDATE:
                        if(!platformAppMap.containsKey(operationInfo.getPlatformAppId()))
                        {
                            sb.append(String.format("%s : platformAppId=%d of %s not exist", operation.name, operationInfo.getPlatformAppId(), JSONObject.toJSONString(operationInfo)));
                        }
                        if(StringUtils.isBlank(operationInfo.getTargetVersion()))
                        {
                            sb.append(String.format("%s : target version of %s is blank;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        else if(!versionMap.containsKey(operationInfo.getTargetVersion()))
                        {
                            sb.append(String.format("%s  : target version %s of %s not exist;", operation.name, operationInfo.getTargetVersion(), JSONObject.toJSONString(operationInfo)));
                        }
                        if(StringUtils.isBlank(operationInfo.getOriginalVersion()))
                        {
                            sb.append(String.format("%s : original version of %s is blank;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        else if(!versionMap.containsKey(operationInfo.getOriginalVersion()))
                        {
                            sb.append(String.format("%s : original version %s of %s not exist;", operation.name, operationInfo.getOriginalVersion(), JSONObject.toJSONString(operationInfo)));
                        }
                        if(StringUtils.isBlank(operationInfo.getAppAlias()))
                        {
                            sb.append(String.format("%s : alias of %s is blank;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        if(operationInfo.getCfgs() == null || operationInfo.getCfgs().size() == 0)
                        {
                            sb.append(String.format("%s : cfg of %s is 0;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        if(!appBkModuleMap.containsKey(operationInfo.getPlatformAppId()))
                        {
                            sb.append(String.format("%s : platformAppId=%d of %s lj paas module relation not exist;", operation.name, operationInfo.getPlatformAppId(), JSONObject.toJSONString(operationInfo)));
                        }
                        else
                        {
                            String compareResult = cfgFileCompare(versionMap.get(operationInfo.getTargetVersion()), operationInfo.getCfgs());
                            if(StringUtils.isNotBlank(compareResult))
                                sb.append(String.format("%s : cfg of %s is not match [%s];", operation.name, JSONObject.toJSONString(operationInfo), compareResult));
                        }
                        break;
                    case CFG_UPDATE:
                        if(StringUtils.isBlank(operationInfo.getOriginalVersion()))
                        {
                            sb.append(String.format("%s : original version of %s is blank;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        else if(!versionMap.containsKey(operationInfo.getOriginalVersion()))
                        {
                            sb.append(String.format("%s : original version %s of %s not exist;", operation.name, operationInfo.getTargetVersion(), JSONObject.toJSONString(operationInfo)));
                        }
                        if(StringUtils.isBlank(operationInfo.getAppAlias()))
                        {
                            sb.append(String.format("%s : alias of %s is blank;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        if(operationInfo.getCfgs() == null || operationInfo.getCfgs().size() == 0)
                        {
                            sb.append(String.format("%s : cfg of %s is 0;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        if(!appBkModuleMap.containsKey(operationInfo.getPlatformAppId()))
                        {
                            sb.append(String.format("%s : platformAppId=%d of %s lj paas module relation not exist;", operation.name, operationInfo.getPlatformAppId(), JSONObject.toJSONString(operationInfo)));
                        }
                        else
                        {
                            String compareResult = cfgFileCompare(versionMap.get(operationInfo.getTargetVersion()), operationInfo.getCfgs());
                            if(StringUtils.isNotBlank(compareResult))
                                sb.append(String.format("%s : cfg of %s is not match[%s];", operation.name, JSONObject.toJSONString(operationInfo), compareResult));
                        }
                        break;
                    case DELETE:
                        if(StringUtils.isBlank(operationInfo.getOriginalVersion()))
                        {
                            sb.append(String.format("%s : original version of %s is blank;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        else if(!versionMap.containsKey(operationInfo.getOriginalVersion()))
                        {
                            sb.append(String.format("%s : original version %s of %s not exist;", operation.name, operationInfo.getOriginalVersion(), JSONObject.toJSONString(operationInfo)));
                        }
                        if(StringUtils.isBlank(operationInfo.getAppAlias()))
                        {
                            sb.append(String.format("%s : alias of %s is blank;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        if(!appBkModuleMap.containsKey(operationInfo.getPlatformAppId()))
                        {
                            sb.append(String.format("%s : platformAppId=%d of %s lj paas module relation not exist;", operation.name, operationInfo.getPlatformAppId(), JSONObject.toJSONString(operationInfo)));
                        }
                        break;
                    default:
                }
            }

        }
        return sb.toString();
    }

    /**
     * 将平台升级结果保存到数据库并同步给蓝鲸paas
     * @param schemaInfo 执行完的平台升级计划
     * @param platformPo 执行计划的平台信息
     * @param domainList 平台下的所有域列表
     * @param appList 当前应用列表
     * @param deployApps 执行升级计划前部署应用列表
     * @param appBkModuleList 执行升级计划前部署应用同蓝鲸paas平台的关系
     * @param bkSetList 该平台对应蓝鲸biz下的所有set列表
     * @param bkHostList 该平台所有服务器列表
     * @throws InterfaceCallException 调用蓝鲸api或是nexus的api失败
     * @throws LJPaasException 蓝鲸api返回接口调用失败或是解析蓝鲸api返回结果失败
     * @throws NexusException nexus api返回调用失败或是解析nexus api的返回结果失败
     * @throws IOException 处理文件失败
     */
    private void recordPlatformUpdateResult(PlatformUpdateSchemaInfo schemaInfo, PlatformPo platformPo,
                                                              List<DomainPo> domainList, List<AppModuleVo> appList, List<PlatformAppPo> deployApps,
                                                              List<PlatformAppBkModulePo> appBkModuleList, List<LJSetInfo> bkSetList,
                                                              List<LJHostInfo> bkHostList) throws InterfaceCallException, LJPaasException, NexusException, IOException
    {
        Map<String, LJSetInfo> setMap = bkSetList.stream().collect(Collectors.toMap(LJSetInfo::getBkSetName, Function.identity()));
        Map<String, DomainPo> domainMap = domainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        Map<Integer, PlatformAppPo> platformAppMap = deployApps.stream().collect(Collectors.toMap(PlatformAppPo::getPlatformAppId, Function.identity()));
        Map<String, List<AppModuleVo>> appMap = appList.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        for(DomainUpdatePlanInfo planInfo : schemaInfo.getDomainUpdatePlanList())
        {
            switch (planInfo.getUpdateType())
            {
                case ADD:
                    addNewDomainToPlatform(planInfo.getDomainId(), planInfo.getDomainName(), planInfo.getAppUpdateOperationList(),
                            appList, platformPo, planInfo.getSetId(), setMap.get(planInfo.getBkSetName()), bkHostList);
                    break;
                case DELETE:
                    deleteDomainFromPlatform(domainMap.get(planInfo.getDomainId()), setMap.get(planInfo.getBkSetName()), planInfo.getAppUpdateOperationList(), bkHostList);
                    break;
                default:
                    DomainPo domainPo = domainMap.get(planInfo.getDomainId());
                    LJSetInfo bkSet = setMap.get(planInfo.getBkSetName());
                    Map<AppUpdateOperation, List<AppUpdateOperationInfo>> operationMap = planInfo.getAppUpdateOperationList().stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getOperation));
                    if(operationMap.containsKey(AppUpdateOperation.DELETE))
                    {
                        removeAppsFromDomainHost(domainPo, bkSet, operationMap.get(AppUpdateOperation.DELETE), appBkModuleList);
                    }
                    if(operationMap.containsKey(AppUpdateOperation.VERSION_UPDATE))
                    {
                        for(AppUpdateOperationInfo updateOperationInfo : operationMap.get(AppUpdateOperation.VERSION_UPDATE))
                        {
                            Map<String, AppModuleVo> versionMap = appMap.get(updateOperationInfo.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
                            updateDeployAppModuleVersion(updateOperationInfo, platformAppMap.get(updateOperationInfo.getPlatformAppId()), versionMap.get(updateOperationInfo.getOriginalVersion()), versionMap.get(updateOperationInfo.getTargetVersion()));
                        }
                    }
                    if(operationMap.containsKey(AppUpdateOperation.CFG_UPDATE))
                    {
                        for(AppUpdateOperationInfo cfgOperationInfo : operationMap.get(AppUpdateOperation.VERSION_UPDATE))
                        {
                            Map<String, AppModuleVo> versionMap = appMap.get(cfgOperationInfo.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
                            modifyDeployAppModuleCfg(cfgOperationInfo, platformAppMap.get(cfgOperationInfo.getPlatformAppId()), versionMap.get(cfgOperationInfo.getOriginalVersion()));
                        }
                    }
                    if(operationMap.containsKey(AppUpdateOperation.ADD))
                    {
                        deployAppsToDomainHost(domainPo, planInfo.getSetId(), bkSet, bkHostList, operationMap.get(AppUpdateOperation.ADD), appList);
                    }
            }
        }
    }

    private String cfgFileCompare(AppModuleVo app, List<AppFileNexusInfo> cfgs)
    {
        StringBuffer sb = new StringBuffer();
        Map<String, AppCfgFilePo> appCfgMap = app.getCfgs().stream().collect(Collectors.toMap(AppCfgFilePo::getFileName, Function.identity()));
        Map<String, AppFileNexusInfo> comparedCfgMap = cfgs.stream().collect(Collectors.toMap(AppFileNexusInfo::getFileName, Function.identity()));
        for(String fileName : appCfgMap.keySet())
        {
            if(!comparedCfgMap.containsKey(fileName))
            {
                sb.append(String.format("cfg %s is not exist,", fileName));
            }
        }
        for(String fileName : comparedCfgMap.keySet())
        {
            if(!appCfgMap.containsKey(fileName))
            {
                sb.append(String.format("%s is not wanted cfg,", fileName));
            }
        }
        sb.toString().replaceAll(",$", "");
        return sb.toString();
    }

    @Override
    public void updatePlatformUpdateSchema(PlatformUpdateSchemaInfo updateSchema) throws ParamException, InterfaceCallException, LJPaasException, NexusException, IOException {
        logger.debug(String.format("begin to update platform update schema : %s", JSONObject.toJSONString(updateSchema)));
        Date now = new Date();
        if(StringUtils.isBlank(updateSchema.getPlatformId()))
        {
            logger.error(String.format("platformId of update schema is blank"));
            throw new ParamException(String.format("platformId of update schema is blank"));
        }
        else if(StringUtils.isBlank(updateSchema.getPlatformName()))
        {
            logger.error(String.format("platformName of update schema is blank"));
            throw new ParamException(String.format("platformName of update schema is blank"));
        }
        else if(updateSchema.getBkBizId() <= 0)
        {
            logger.error(String.format("bkBizId of update schema is 0"));
            throw new ParamException(String.format("not bkBizId of update schema"));
        }
        LJBizInfo bkBiz = paasService.queryBizInfoById(updateSchema.getBkBizId());
        if(bkBiz == null)
        {
            logger.error(String.format("bkBizId=%d biz not exist", updateSchema.getBkBizId()));
            throw new ParamException(String.format("bkBizId=%d biz not exist", updateSchema.getBkBizId()));
        }
        if(!updateSchema.getPlatformName().equals(bkBiz.getBkBizName()))
        {
            logger.error(String.format("bkBizName of bizBkId is %s, not %s", updateSchema.getBkBizId(), bkBiz.getBkBizName(), updateSchema.getPlatformName()));
            throw new ParamException(String.format("bkBizName of bizBkId is %s, not %s", updateSchema.getBkBizId(), bkBiz.getBkBizName(), updateSchema.getPlatformName()));
        }
        PlatformPo platformPo = platformMapper.selectByPrimaryKey(updateSchema.getPlatformId());
        if(platformPo == null)
        {
            if(!updateSchema.getStatus().equals(UpdateStatus.CREATE))
            {
                logger.error(String.format("%s platform %s not exist", updateSchema.getStatus().name, updateSchema.getPlatformId()));
                throw new ParamException(String.format("%s platform %s not exist", updateSchema.getStatus().name, updateSchema.getPlatformId()));
            }
            else
            {
                platformPo = new PlatformPo();
                platformPo.setPlatformId(updateSchema.getPlatformId());
                platformPo.setPlatformName(updateSchema.getPlatformName());
                platformPo.setComment("create by platform create schema");
                platformPo.setStatus(CCODPlatformStatus.SCHEMA_CREATE_PLATFORM.id);
                platformPo.setCreateTime(new Date());
                platformPo.setBkBizId(updateSchema.getBkBizId());
                platformPo.setBkCloudId(updateSchema.getBkCloudId());
                platformPo.setUpdateTime(new Date());
                platformPo.setCcodVersion("ccod4.8");
                this.platformMapper.insert(platformPo);
            }
        }
        if(!platformPo.getPlatformName().equals(updateSchema.getPlatformName()))
        {
            logger.error(String.format("platformName of %s is %s, not %s",
                    platformPo.getPlatformId(), platformPo.getPlatformName(), updateSchema.getPlatformName()));
            throw new ParamException(String.format("bkBizName of bizBkId is %s, not %s", updateSchema.getBkBizId(), bkBiz.getBkBizName(), updateSchema.getPlatformName()));
        }
        if(updateSchema.getPlatformId().equals("ccAppTest"))
        {
            updateSchema.setStatus(UpdateStatus.SUCCESS);
            for(DomainUpdatePlanInfo planInfo : updateSchema.getDomainUpdatePlanList())
            {
                planInfo.setStatus(UpdateStatus.SUCCESS);
            }
        }
        CCODPlatformStatus platformStatus = CCODPlatformStatus.getEnumById(platformPo.getStatus());
        if(platformStatus == null)
        {
            logger.error(String.format("%s status %d is unknown", platformPo.getPlatformId(), platformPo.getStatus()));
            throw new ParamException(String.format("%s status %d is unknown", platformPo.getPlatformId(), platformPo.getStatus()));
        }
        switch (updateSchema.getTaskType())
        {
            case CREATE:
                switch (platformStatus)
                {
                    case SCHEMA_CREATE_PLATFORM:
                        break;
                    default:
                        logger.error(String.format("not support %s platform %s which status is %s",
                                updateSchema.getTaskType().name, updateSchema.getPlatformId(), platformStatus.name));
                        throw new ParamException(String.format("not support %s platform %s which status is %s",
                                updateSchema.getTaskType().name, updateSchema.getPlatformId(), platformStatus.name));
                }
                break;
            case UPDATE:
                switch (platformStatus)
                {
                    case WAIT_SYNC_EXIST_PLATFORM_TO_PAAS:
                    case UNKNOWN:
                    case SCHEMA_CREATE_PLATFORM:
                        logger.error(String.format("not support %s platform %s which status is %s",
                                updateSchema.getTaskType().name, updateSchema.getPlatformId(), platformStatus.name));
                        throw new ParamException(String.format("not support %s platform %s which status is %s",
                                updateSchema.getTaskType().name, updateSchema.getPlatformId(), platformStatus.name));
                    default:
                        break;
                }
                break;
            default:
                logger.error(String.format("current version not support %s %s", updateSchema.getTaskType().name, updateSchema.getPlatformId()));
                throw new ParamException(String.format("current version not support %s %s", updateSchema.getTaskType().name, updateSchema.getPlatformId()));
        }
        List<DomainPo> domainList = this.domainMapper.select(updateSchema.getPlatformId(), null);
        List<AppModuleVo> appList = this.appModuleMapper.select(null, null, null, null);
        List<PlatformAppPo> deployApps = this.platformAppMapper.select(updateSchema.getPlatformId(), null, null, null, null, null);
        List<LJSetInfo> bkSetList = this.paasService.queryBkBizSet(updateSchema.getBkBizId());
        List<LJHostInfo> bkHostList = this.paasService.queryBKHost(updateSchema.getBkBizId(), null, null, null, null);
        List<PlatformAppBkModulePo> appBkModuleList = this.platformAppBkModuleMapper.select(updateSchema.getPlatformId(), null, null, null, null, null);
        String schemaCheckResult;
        logger.debug(String.format("%s %s has been %s", updateSchema.getTaskType().name, updateSchema.getPlatformId(), updateSchema.getStatus().name));
        switch (updateSchema.getTaskType())
        {
            case CREATE:
                switch (updateSchema.getStatus())
                {
                    case CANCEL:
                    case FAIL:
                        this.platformUpdateSchemaMap.remove(updateSchema.getPlatformId());
                        this.platformUpdateSchemaMapper.delete(updateSchema.getPlatformId());
                        this.platformMapper.delete(updateSchema.getPlatformId());
                        break;
                    case SUCCESS:
                        schemaCheckResult = checkPlatformUpdateTask(updateSchema, domainList, appList, deployApps, bkSetList, bkHostList, appBkModuleList);
                        if(StringUtils.isNotBlank(schemaCheckResult))
                        {
                            logger.error(String.format("schema is not legal : %s", schemaCheckResult));
                            throw new ParamException(String.format("schema is not legal : %s", schemaCheckResult));
                        }
                        recordPlatformUpdateResult(updateSchema, platformPo, domainList, appList, deployApps, appBkModuleList, bkSetList, bkHostList);
                        if(this.platformUpdateSchemaMap.containsKey(updateSchema.getPlatformId()))
                            this.platformUpdateSchemaMap.remove(updateSchema.getPlatformId());
                        this.platformUpdateSchemaMapper.delete(updateSchema.getPlatformId());
                        platformPo.setStatus(CCODPlatformStatus.RUNNING.id);
                        platformPo.setUpdateTime(now);
                        this.platformMapper.update(platformPo);
                        break;
                    default:
                        schemaCheckResult = checkPlatformUpdateTask(updateSchema, domainList, appList, deployApps, bkSetList, bkHostList, appBkModuleList);
                        if(StringUtils.isNotBlank(schemaCheckResult))
                        {
                            logger.error(String.format("schema is not legal : %s", schemaCheckResult));
                            throw new ParamException(String.format("schema is not legal : %s", schemaCheckResult));
                        }
                        this.platformUpdateSchemaMapper.delete(updateSchema.getPlatformId());
                        this.platformUpdateSchemaMap.put(updateSchema.getPlatformId(), updateSchema);
                        PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
                        schemaPo.setPlatformId(updateSchema.getPlatformId());
                        schemaPo.setContext(JSONObject.toJSONString(updateSchema).getBytes());
                        platformUpdateSchemaMapper.insert(schemaPo);
                        break;
                }
                break;
            case UPDATE:
                switch (updateSchema.getStatus())
                {
                    case CANCEL:
                    case FAIL:
                        this.platformUpdateSchemaMap.remove(updateSchema.getPlatformId());
                        this.platformUpdateSchemaMapper.delete(updateSchema.getPlatformId());
                        platformPo.setStatus(CCODPlatformStatus.RUNNING.id);
                        platformPo.setUpdateTime(now);
                        this.platformMapper.update(platformPo);
                        break;
                    case SUCCESS:
                        schemaCheckResult = checkPlatformUpdateTask(updateSchema, domainList, appList, deployApps, bkSetList, bkHostList, appBkModuleList);
                        if(StringUtils.isNotBlank(schemaCheckResult))
                        {
                            logger.error(String.format("schema is not legal : %s", schemaCheckResult));
                            throw new ParamException(String.format("schema is not legal : %s", schemaCheckResult));
                        }
                        recordPlatformUpdateResult(updateSchema, platformPo, domainList, appList, deployApps, appBkModuleList, bkSetList, bkHostList);
                        if(this.platformUpdateSchemaMap.containsKey(updateSchema.getPlatformId()))
                            this.platformUpdateSchemaMap.remove(updateSchema.getPlatformId());
                        this.platformUpdateSchemaMapper.delete(updateSchema.getPlatformId());
                        platformPo.setStatus(CCODPlatformStatus.RUNNING.id);
                        platformPo.setUpdateTime(now);
                        this.platformMapper.update(platformPo);
                        break;
                    default:
                        schemaCheckResult = checkPlatformUpdateTask(updateSchema, domainList, appList, deployApps, bkSetList, bkHostList, appBkModuleList);
                        if(StringUtils.isNotBlank(schemaCheckResult))
                        {
                            logger.error(String.format("schema is not legal : %s", schemaCheckResult));
                            throw new ParamException(String.format("schema is not legal : %s", schemaCheckResult));
                        }
                        this.platformUpdateSchemaMap.put(updateSchema.getPlatformId(), updateSchema);
                        platformPo.setStatus(CCODPlatformStatus.SCHEMA_UPDATE_PLATFORM.id);
                        platformPo.setUpdateTime(now);
                        platformMapper.update(platformPo);
                        platformUpdateSchemaMapper.delete(updateSchema.getPlatformId());
                        PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
                        schemaPo.setPlatformId(updateSchema.getPlatformId());
                        schemaPo.setContext(JSONObject.toJSONString(updateSchema).getBytes());
                        platformUpdateSchemaMapper.insert(schemaPo);
                        break;
                }
                break;
            default:
                logger.error(String.format("current version not support platform %s", updateSchema.getTaskType().name));
                throw new ParamException(String.format("current version not support platform %s", updateSchema.getTaskType().name));
        }
    }

//    private DomainUpdatePlanInfo generateDomainUpdatePlan(DomainUpdateType updateType, UpdateStatus updateStatus, AppUpdateOperation operation, int domId, String domainName, String domainId, List<CCODModuleInfo> deployApps, List<AppPo> appList)
//    {
//        Date now = new Date();
//        DomainUpdatePlanInfo planInfo = new DomainUpdatePlanInfo();
//        planInfo.setDomainId(domainId);
//        planInfo.setDomainName(domainName);
//        planInfo.setCreateTime(now);
//        planInfo.setUpdateTime(now);
//        planInfo.setExecuteTime(now);
//        List<AppUpdateOperationInfo> operationList = new ArrayList<>();
//        Map<String, List<AppPo>> nameAppMap = appList.stream().collect(Collectors.groupingBy(AppPo::getAppName));
//        for(CCODModuleInfo deployApp : deployApps)
//        {
//            Map<String, AppPo> versionAppMap = nameAppMap.get(deployApp.getModuleName()).stream().collect(Collectors.toMap(AppPo::getVersion, Function.identity()));
//            AppPo chosenApp = versionAppMap.get(deployApp.getVersion());
//            if(!DomainUpdateType.ADD.equals(updateType))
//            {
//                for(String version : versionAppMap.keySet())
//                {
//                    if(!version.equals(deployApp.getVersion()))
//                    {
//                        chosenApp = versionAppMap.get(version);
//                        break;
//                    }
//                }
//            }
//            AppUpdateOperationInfo operationInfo = generateAppUpdateOperation(operation, deployApp, chosenApp, updateStatus);
//            operationList.add(operationInfo);
//        }
//        planInfo.setAppUpdateOperationList(operationList);
//        return planInfo;
//    }

//    private AppUpdateOperationInfo generateAppUpdateOperation(AppUpdateOperation operation, CCODModuleInfo deployApp, AppPo targetApp, UpdateStatus updateStatus)
//    {
//        Date now = new Date();
//        AppUpdateOperationInfo info = new AppUpdateOperationInfo();
////        info.setAppRunner(deployApp.getAppRunner());
////        info.setBasePath(deployApp.getBasePath());
////        info.setHostIp(deployApp.getHostIp());
////        info.setCfgs(new ArrayList<>());
////        for(PlatformAppCfgFilePo cfg : deployApp.getCfgs())
////        {
////            AppFileNexusInfo fileInfo = new AppFileNexusInfo();
////            fileInfo.setDeployPath(deployApp.get);
////            NexusAssetInfo assetInfo = new NexusAssetInfo();
////            Checksum checksum = new Checksum();
////            checksum.md5 = cfg.getMd5();
////            assetInfo.setChecksum(checksum);
////            assetInfo.setId(cfg.getNexusAssetId());
////            assetInfo.setPath(cfg.getNexusDirectory());
////            assetInfo.setRepository(cfg.getNexusRepository());
////            info.getCfgs().add(assetInfo);
////        }
////        info.setOperation(operation);
////        info.setOriginalAppId(deployApp.getAppId());
////        info.setTargetAppId(deployApp.getAppId());
////        switch (operation)
////        {
////            case ADD:
////                info.setOriginalAppId(0);
////                break;
////            case DELETE:
////                info.setTargetAppId(0);
////                info.setCfgs(new ArrayList<>());
////                break;
////            case CFG_UPDATE:
////                info.setTargetAppId(0);
////                break;
////            default:
////                break;
////        }
//        return info;
//    }

    /**
     * 将某个域从平台移除
     * @param deleteDomain 需要删除的域
     * @param bkSet 该域归属的蓝鲸paas的set
     * @param bkHostList 该域关联的服务器信息
     * @throws InterfaceCallException 调用蓝鲸api异常
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸返回消息失败
     */
    private void deleteDomainFromPlatform(DomainPo deleteDomain, LJSetInfo bkSet, List<AppUpdateOperationInfo> deleteOperationList, List<LJHostInfo> bkHostList)
            throws InterfaceCallException, LJPaasException
    {
        logger.debug(String.format("begin to remove domainId=%s and domainName=%s from platformId=%s",
                deleteDomain.getDomainId(), deleteDomain.getDomainName(), deleteDomain.getPlatformId()));
        List<PlatformAppBkModulePo> domainBkModuleList = this.platformAppBkModuleMapper.select(deleteDomain.getPlatformId(),
                deleteDomain.getDomainId(), null, null, null, null);
        this.removeAppsFromDomainHost(deleteDomain, bkSet, deleteOperationList, domainBkModuleList);
        logger.debug(String.format("delete all domainId=%s and platformId=%s app record",
                deleteDomain.getDomainId(), deleteDomain.getPlatformId()));
        platformAppMapper.delete(null, deleteDomain.getPlatformId(), deleteDomain.getDomainId());
        logger.debug(String.format("delete domainId=%s and platformId=%s record from database",
                deleteDomain.getDomainId(), deleteDomain.getPlatformId()));
        domainMapper.delete(deleteDomain.getDomainId(), deleteDomain.getPlatformId());
        logger.info(String.format("delete domainId=%s from platformId=%s SUCCESS",
                deleteDomain.getDomainId(), deleteDomain.getPlatformId()));
    }

    /**
     * 升级已有的应用模块
     * @param updateApp 模块升级操作信息
     * @param deployApp 该模块的部署记录
     * @param originalApp 升级前app信息
     * @param targetApp 升级后app信息
     * @throws DataAccessException cmdb数据库访问异常
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    private void updateDeployAppModuleVersion(AppUpdateOperationInfo updateApp, PlatformAppPo deployApp, AppModuleVo originalApp, AppModuleVo targetApp)
            throws InterfaceCallException, NexusException, IOException
    {
        logger.debug(String.format("begin to modify %s version from %s to %s at %s/%s/%s",
                updateApp.getAppAlias(), originalApp.getVersion(), targetApp.getVersion(), deployApp.getPlatformId(), deployApp.getDomainId(), deployApp.getHostIp()));
        Date now = new Date();
        handlePlatformAppCfgs(deployApp, targetApp, updateApp.getCfgs());
        deployApp.setAppId(targetApp.getAppId());
        deployApp.setDeployTime(now);
        logger.debug(String.format("update platformAppId=%d platform_app record", deployApp.getPlatformAppId()));
        platformAppMapper.update(deployApp);
        logger.info(String.format("update %s version from %s to %s at %s/%s/%s SUCCESS",
                updateApp.getAppAlias(), originalApp.getVersion(), targetApp.getVersion(), deployApp.getHostIp()));
    }

    /**
     * 修改已有的应用的配置文件
     * @param modifiedCfgApp 修改配置文件的操作信息
     * @param deployApp 被修改配置应用部署记录
     * @param appModule 被修改配置app记录
    * @throws DataAccessException cmdb数据库访问异常
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    private void modifyDeployAppModuleCfg(AppUpdateOperationInfo modifiedCfgApp, PlatformAppPo deployApp, AppModuleVo appModule)
            throws InterfaceCallException, NexusException, IOException
    {
        logger.debug(String.format("begin to modify %d %s cfgs at %s/%s/%s",
                modifiedCfgApp.getCfgs().size(), modifiedCfgApp.getAppAlias(), deployApp.getPlatformId(), deployApp.getDomainId(), deployApp.getHostIp()));
        handlePlatformAppCfgs(deployApp, appModule, modifiedCfgApp.getCfgs());
        Date now = new Date();
        deployApp.setDeployTime(now);
        logger.debug(String.format("update platformAppId=%d platform_app record deployTime", deployApp.getPlatformAppId()));
        platformAppMapper.update(deployApp);
        logger.debug(String.format("modify %d %s cfgs at %s/%s/%s SUCCESS",
                modifiedCfgApp.getCfgs().size(), modifiedCfgApp.getAppAlias(), deployApp.getPlatformId(), deployApp.getDomainId(), deployApp.getHostIp()));
    }


    /**
     * 根据现有数据生成一个demo平台创建计划
     * @param bkBizId 需要创建的平台biz id
     * @param platformId 需要创建的平台id
     * @param platformName 需要创建的平台名
     * @param bkCloudId  该平台服务器对应的cloud id
     * @return 新创建的demo平台创建计划
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws LJPaasException
     */
    private PlatformUpdateSchemaInfo generatePlatformCreateDemoSchema(String platformId, String platformName, int bkBizId, int bkCloudId) throws ParamException, InterfaceCallException, LJPaasException {
        PlatformPo platform = platformMapper.selectByPrimaryKey(platformId);
        if (platform != null) {
            logger.error(String.format("create new platform create schema FAIL : %s exist", platformId));
            throw new ParamException(String.format("create new platform create schema FAIL : %s exist", platformId));
        }
        platform = platformMapper.selectByNameBizId(platformName, null);
        if (platform != null) {
            logger.error(String.format("create new platform create schema FAIL : %s exist", platformName));
            throw new ParamException(String.format("create new platform create schema FAIL : %s exist", platformName));
        }
        LJBizInfo bkBiz = paasService.queryBizInfoById(bkBizId);
        if (bkBiz == null) {
            logger.error(String.format("create new platform create schema FAIL : bkBizId=%d biz not exist", bkBizId));
            throw new ParamException(String.format("create new platform create schema FAIL : bkBizId=%d biz not exist", bkBizId));
        }
        if (!platformName.equals(bkBiz.getBkBizName()))
        {
            logger.error(String.format("create new platform create schema FAIL : bkBizId=%d biz name is %s, not %s", bkBizId, bkBiz.getBkBizName(), platformName));
            throw new ParamException(String.format(String.format("create new platform create schema FAIL : bkBizId=%d biz name is %s, not %s", bkBizId, bkBiz.getBkBizName(), platformName)));
        }
        Date now = new Date();
        List<AppModuleVo> appList = this.appModuleMapper.select(null, null, null, null);
        Map<String, List<AppModuleVo>> appMap = appList.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        Map<String, BizSetDefine> setDefineMap = this.paasService.queryCCODBizSet(false).stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        List<LJSetInfo> setList = this.paasService.resetExistBiz(bkBizId, new ArrayList<>(setDefineMap.keySet()));
        platform = new PlatformPo();
        platform.setBkCloudId(bkCloudId);
        platform.setBkBizId(bkBizId);
        platform.setCreateTime(now);
        platform.setCcodVersion("ccod4.5");
        platform.setStatus(1);
        platform.setPlatformName(platformName);
        platform.setPlatformId(platformId);
        platform.setComment("通过程序自动创建的demo平台");
        platformMapper.insert(platform);
        PlatformUpdateSchemaInfo schema = new PlatformUpdateSchemaInfo();
        schema.setTitle(String.format("%s(%s)平台新建计划", platformName, platformId));
        schema.setPlatformName(platformName);
        schema.setComment(String.format("通过程序自动创建的%s(%s)平台新建计划", platformName, platformId));
        schema.setCreateTime(now);
        schema.setBkBizId(bkBizId);
        schema.setStatus(UpdateStatus.CREATE);
        schema.setTaskType(PlatformUpdateTaskType.CREATE);
        schema.setExecuteTime(now);
        schema.setDeadline(now);
        schema.setUpdateTime(now);
        schema.setPlatformId(platformId);
        schema.setDomainUpdatePlanList(new ArrayList<>());
        Random random = new Random();
        List<String> hostList = new ArrayList<>();
        for(BizSetDefine setDefine : setDefineMap.values())
        {
            if(setDefine.getApps().length == 0)
                continue;
            String hostIp = String.format("%d.%d.%d.%d", 173,random.nextInt(10000) % 255,
                    random.nextInt(10000) % 255, random.nextInt(10000) % 255);
            hostList.add(hostIp);
            DomainUpdatePlanInfo planInfo = new DomainUpdatePlanInfo();
            planInfo.setUpdateType(DomainUpdateType.ADD);
            planInfo.setStatus(UpdateStatus.CREATE);
            planInfo.setComment("由程序自动生成的新建域");
            String domainId = setDefine.getFixedDomainId();
            String domainName = setDefine.getFixedDomainName();
            if(StringUtils.isBlank(domainId))
            {
                domainId = "newCreateTestDomain";
                domainName = "新建测试域";
            }
            planInfo.setDomainId(domainId);
            planInfo.setDomainName(domainName);
            planInfo.setCreateTime(now);
            planInfo.setUpdateTime(now);
            planInfo.setExecuteTime(now);
            planInfo.setAppUpdateOperationList(new ArrayList<>());
            planInfo.setBkSetName(setDefine.getName());
            planInfo.setSetId(setDefine.getId());
            for(String appName : setDefine.getApps())
            {
                AppModuleVo appModuleVo = appMap.get(appName).get(0);
                AppUpdateOperationInfo addOperationInfo = new AppUpdateOperationInfo();
                addOperationInfo.setHostIp(hostIp);
                addOperationInfo.setOperation(AppUpdateOperation.ADD);
                addOperationInfo.setCfgs(new ArrayList<>());
                for(AppCfgFilePo appCfgFilePo : appModuleVo.getCfgs())
                {
                    AppFileNexusInfo info = new AppFileNexusInfo();
                    info.setDeployPath(appCfgFilePo.getDeployPath());
                    info.setExt(appCfgFilePo.getExt());
                    info.setFileName(appCfgFilePo.getFileName());
                    info.setFileSize(0);
                    info.setMd5(appCfgFilePo.getMd5());
                    info.setNexusAssetId(appCfgFilePo.getNexusAssetId());
                    info.setNexusPath(String.format("%s/%s", appCfgFilePo.getNexusDirectory(), appCfgFilePo.getFileName()));
                    info.setNexusRepository(appCfgFilePo.getNexusRepository());
                    addOperationInfo.getCfgs().add(info);
                }
                addOperationInfo.setBasePath(appModuleVo.getBasePath());
                addOperationInfo.setAppRunner("qnsoft");
                addOperationInfo.setAppAlias(appModuleVo.getAppAlias());
                addOperationInfo.setAppName(appModuleVo.getAppName());
                addOperationInfo.setTargetVersion(appModuleVo.getVersion());
                planInfo.getAppUpdateOperationList().add(addOperationInfo);
            }
            schema.getDomainUpdatePlanList().add(planInfo);
        }
        LJSetInfo idlePoolSet = setList.stream().collect(Collectors.toMap(LJSetInfo::getBkSetName, Function.identity())).get(this.paasIdlePoolSetName);
        List<LJHostInfo> bkHostList = this.paasService.addNewHostToIdlePool(bkBizId, idlePoolSet.getBkSetId(), hostList, bkCloudId);
        String planCheckResult = checkPlatformUpdateTask(schema, new ArrayList<>(), appList, new ArrayList<>(), setList, bkHostList, new ArrayList<>());
        if(StringUtils.isNotBlank(planCheckResult))
        {
            logger.error(String.format("check platform update schema FAIL : %s", planCheckResult));
            throw new ParamException(String.format("check platform update schema FAIL : %s", planCheckResult));
        }
        return schema;
    }

    @Override
    public PlatformUpdateSchemaInfo createPlatformUpdateSchemaDemo(PlatformUpdateSchemaParamVo paramVo) throws ParamException, InterfaceCallException, LJPaasException {
        logger.debug(String.format("prepare to create platform update schema demo : params=%s", JSONObject.toJSONString(paramVo)));
        if(paramVo.getBkBizId() == 0)
        {
            logger.error(String.format("create demo schema FAIL : bkBizId=0"));
            throw new ParamException(String.format("create demo schema FAIL : bkBizId=0"));
        }
        else if(StringUtils.isBlank(paramVo.getPlatformId()))
        {
            logger.error(String.format("create demo schema FAIL : platformId is blank"));
            throw new ParamException(String.format("create demo schema FAIL : platformId is blank"));
        }
        else if(StringUtils.isBlank(paramVo.getPlatformName()))
        {
            logger.error(String.format("create demo schema FAIL : platformName is blank"));
            throw new ParamException(String.format("create demo schema FAIL : platformName is blank"));
        }
        else if(paramVo.getTaskType() == null)
        {
            logger.error(String.format("create demo schema FAIL : taskType is blank"));
            throw new ParamException(String.format("create demo schema FAIL : taskType is blank"));
        }
        PlatformUpdateSchemaInfo schemaInfo;
        if(PlatformUpdateTaskType.CREATE.equals(paramVo.getTaskType()))
        {
            schemaInfo = generatePlatformCreateDemoSchema(paramVo.getPlatformId(), paramVo.getPlatformName(), paramVo.getBkBizId(), paramVo.getBkCloudId());
        }
        else
        {
            logger.error(String.format("current version not support %s platform update demo schema create", paramVo.getTaskType().name));
            throw new ParamException(String.format("current version not support %s platform update demo schema create", paramVo.getTaskType().name));
        }
        platformUpdateSchemaMapper.delete(paramVo.getPlatformId());
        PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
        schemaPo.setPlatformId(paramVo.getPlatformId());
        schemaPo.setContext(JSONObject.toJSONString(schemaInfo).getBytes());
        platformUpdateSchemaMapper.insert(schemaPo);
        this.platformUpdateSchemaMap.put(paramVo.getPlatformId(), schemaInfo);
        return schemaInfo;
    }

    @Override
    public List<PlatformUpdateSchemaInfo> queryPlatformUpdateSchema(String platformId) {
        logger.debug(String.format("begin to query platformId=%s platform update schema", platformId));
        List<PlatformUpdateSchemaInfo> schemaList = new ArrayList<>();
        if(StringUtils.isBlank(platformId))
        {
            schemaList = new ArrayList<>(this.platformUpdateSchemaMap.values());
        }
        else
        {
            if(this.platformUpdateSchemaMap.containsKey(platformId))
            {
                schemaList.add(platformUpdateSchemaMap.get(platformId));
            }
        }
        return schemaList;
    }

    /**
     * 把通过onlinemanager主动收集上来的ccod应用部署情况同步到paas之前需要给这些应用添加对应的bizId
     * 确定应用归属的set信息,并根据定义的set-app关系对某些应用归属域重新赋值
     * @param bizId 蓝鲸paas的biz id
     * @param deployApps 需要处理的应用详情
     * @return 处理后的结果
     * @throws NotSupportAppException 如果应用中存在没有在lj-paas.set-apps节点定义的应用将抛出此异常
     */
    private List<PlatformAppDeployDetailVo> makeUpBizInfoForDeployApps(int bizId, List<PlatformAppDeployDetailVo> deployApps) throws NotSupportAppException
    {
        Map<String, List<BizSetDefine>> appSetRelationMap = this.paasService.getAppBizSetRelation();
        for(PlatformAppDeployDetailVo deployApp : deployApps)
        {
            if(!appSetRelationMap.containsKey(deployApp.getAppName()))
            {
                logger.error(String.format("%s没有在配置文件的lj-paas.set-apps节点中定义", deployApp.getAppName()));
                throw new NotSupportAppException(String.format("%s未定义所属的set信息", deployApp.getAppName()));
            }
            deployApp.setBkBizId(bizId);
            BizSetDefine sd = appSetRelationMap.get(deployApp.getAppName()).get(0);
            deployApp.setBkSetName(sd.getName());
            if(StringUtils.isNotBlank(sd.getFixedDomainName()))
            {
                deployApp.setSetId(sd.getId());
                deployApp.setBkSetName(sd.getName());
                deployApp.setDomainId(sd.getFixedDomainId());
                deployApp.setDomainName(sd.getFixedDomainName());
            }
        }
        return deployApps;
    }

    /**
     * 根据应用部署详情生成平台的set拓扑结构
     * @param deployAppList 平台的应用部署明细
     * @return 台的set拓扑结构
     * @throws DBPAASDataNotConsistentException
     * @throws NotSupportAppException
     */
    private List<CCODSetInfo> generateCCODSetInfo(List<PlatformAppDeployDetailVo> deployAppList) throws ParamException, NotSupportAppException
    {
        Map<String, List<BizSetDefine>> appSetRelationMap = this.paasService.getAppBizSetRelation();
        Map<String, BizSetDefine> setDefineMap = this.paasService.queryCCODBizSet(false).stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        Map<String, List<PlatformAppDeployDetailVo>> setAppMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getBkSetName));
        List<CCODSetInfo> setList = new ArrayList<>();
        for(PlatformAppDeployDetailVo deployApp : deployAppList)
        {
            if(!appSetRelationMap.containsKey(deployApp.getAppName()))
            {
                logger.error(String.format("current version not support %s", deployApp.getAppName()));
                throw new NotSupportAppException(String.format("current version not support %s", deployApp.getAppName()));
            }
            else if(!setDefineMap.containsKey(deployApp.getBkSetName()))
            {
                logger.error(String.format("%s is not a legal ccod set name", deployApp.getBkSetName()));
                throw new ParamException(String.format("%s is not a legal ccod set name", deployApp.getBkSetName()));
            }
        }
        for(String setName : setAppMap.keySet())
        {
            Map<String, List<PlatformAppDeployDetailVo>> domainAppMap =  setAppMap.get(setName)
                    .stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainName));
            List<CCODDomainInfo> domainList = new ArrayList<>();
            for(String domainName : domainAppMap.keySet())
            {
                List<PlatformAppDeployDetailVo> domAppList = domainAppMap.get(domainName);
                CCODDomainInfo domain = new CCODDomainInfo(domAppList.get(0).getDomainId(), domAppList.get(0).getDomainName());
                for(PlatformAppDeployDetailVo deployApp : domAppList)
                {
                    CCODModuleInfo bkModule = new CCODModuleInfo(deployApp);
                    domain.getModules().add(bkModule);
                }
                domainList.add(domain);
            }
            CCODSetInfo set = new CCODSetInfo(setName);
            set.setDomains(domainList);
            setList.add(set);
        }
        return setList;
    }

    @Override
    public PlatformTopologyInfo getPlatformTopology(String platformId) throws ParamException, InterfaceCallException, LJPaasException, NotSupportAppException
    {
        logger.debug(String.format("begin to query %s platform topology", platformId));
        PlatformPo platform = platformMapper.selectByPrimaryKey(platformId);
        if(platform == null)
        {
            logger.error(String.format("%s platform not exist", platformId));
            throw new ParamException(String.format("%s platform not exist", platformId));
        }
        PlatformTopologyInfo topology = new PlatformTopologyInfo(platform);
        if(topology.getStatus() == null)
        {
            logger.error(String.format("unknown ccod platform status %d", platform.getStatus()));
            throw new ParamException(String.format("unknown ccod platform status %d", platform.getStatus()));
        }
        List<LJHostInfo> idleHostList;
        PlatformUpdateSchemaInfo schema;
        List<CCODSetInfo> setList;
        List<PlatformAppDeployDetailVo> deployAppList;
        switch (topology.getStatus())
        {
            case SCHEMA_CREATE_PLATFORM:
                setList = new ArrayList<>();
                idleHostList = this.paasService.queryBizIdleHost(platform.getBkBizId());
                schema = this.platformUpdateSchemaMap.containsKey(platformId) ? this.platformUpdateSchemaMap.get(platformId) : null;
                break;
            case RUNNING:
                if(this.platformUpdateSchemaMap.containsKey(platformId))
                {
                    logger.error(String.format("%s status is %s, but it has an update schema",
                            platformId, topology.getStatus().name));
                    throw new ParamException(String.format("%s status is %s, but it has an update schema",
                            platformId, topology.getStatus().name));
                }
                deployAppList = this.platformAppDeployDetailMapper.selectPlatformApps(platformId, null, null);
                setList = generateCCODSetInfo(deployAppList);
                idleHostList = this.paasService.queryBizIdleHost(platform.getBkBizId());
                schema = null;
                break;
            case SCHEMA_UPDATE_PLATFORM:
                if(!this.platformUpdateSchemaMap.containsKey(platformId))
                {
                    logger.error(String.format("%s status is %s, but can not find its update schema",
                            platformId, topology.getStatus().name));
                    throw new ParamException(String.format("%s status is %s, but can not find its update schema",
                            platformId, topology.getStatus().name));
                }
                deployAppList = this.platformAppDeployDetailMapper.selectPlatformApps(platformId, null, null);
                setList = generateCCODSetInfo(deployAppList);
                idleHostList = this.paasService.queryBizIdleHost(platform.getBkBizId());
                schema = this.platformUpdateSchemaMap.get(platformId);
                break;
            case WAIT_SYNC_EXIST_PLATFORM_TO_PAAS:
                if(this.platformUpdateSchemaMap.containsKey(platformId))
                {
                    logger.error(String.format("%s status is %s, but it has an update schema",
                            platformId, topology.getStatus().name));
                    throw new ParamException(String.format("%s status is %s, but it has an update schema",
                            platformId, topology.getStatus().name));
                }
                deployAppList = this.platformAppDeployDetailMapper.selectPlatformApps(platformId, null, null);
                deployAppList = makeUpBizInfoForDeployApps(platform.getBkBizId(), deployAppList);
                setList = generateCCODSetInfo(deployAppList);
                idleHostList = this.paasService.queryBizIdleHost(platform.getBkBizId());
                schema = null;
                break;
            default:
                idleHostList = new ArrayList<>();
                setList = new ArrayList<>();
                schema = null;
        }
        topology.setSetList(setList);
        topology.setSchema(schema);
        topology.setIdleHosts(idleHostList);
        return topology;
    }

    @Override
    public List<PlatformTopologyInfo> queryAllPlatformTopology() throws ParamException, InterfaceCallException, LJPaasException, NotSupportAppException {
        List<PlatformTopologyInfo> topoList = new ArrayList<>();
        List<PlatformPo> platforms = platformMapper.select(null);
        for(PlatformPo platformPo : platforms)
        {
            PlatformTopologyInfo topo = new PlatformTopologyInfo(platformPo);
            if(topo.getStatus() == null)
            {
                logger.error(String.format("%s status %d is unknown", platformPo.getPlatformId(), platformPo.getStatus()));
                continue;
            }
            switch (topo.getStatus())
            {
                case RUNNING:
                case WAIT_SYNC_EXIST_PLATFORM_TO_PAAS:
                case SCHEMA_UPDATE_PLATFORM:
                case SCHEMA_CREATE_PLATFORM:
                    topoList.add(topo);
                    break;
                default:
            }
        }
        return topoList;
    }

    @Override
    public void registerNewAppModule(AppModuleVo appModule) throws NotSupportAppException, ParamException, InterfaceCallException, NexusException, IOException {
        logger.debug(String.format("begin to register app=[%s] into cmdb", JSONObject.toJSONString(appModule)));
        Map<String, List<BizSetDefine>> appSetRelationMap = this.paasService.getAppBizSetRelation();
        if (!appSetRelationMap.containsKey(appModule.getAppName())) {
            logger.error(String.format("appName=%s is not supported by cmdb", appModule.getAppName()));
            throw new NotSupportAppException(String.format("appName=%s is not supported by cmdb", appModule.getAppName()));
        }
        String moduleCheckResult = checkModuleParam(appModule);
        if (StringUtils.isNotBlank(moduleCheckResult)) {
            logger.error(String.format("app module params check FAIL %s", moduleCheckResult));
            throw new ParamException(String.format("app module params check FAIL %s", moduleCheckResult));
        }
        AppModuleVo oldModuleVo = this.appModuleMapper.selectByNameAndVersion(appModule.getAppName(), appModule.getVersion());
        if (oldModuleVo != null) {
            if(!oldModuleVo.getInstallPackage().getFileName().equals(appModule.getInstallPackage().getFileName()))
            {
                logger.error(String.format("%s's version %s has been registered, but old install package fileName is %s and new install package fileName is %s",
                        appModule.getAppName(), appModule.getVersion(), appModule.getInstallPackage().getFileName(), oldModuleVo.getInstallPackage().getFileName()));
                throw new ParamException(String.format("%s's version %s has been registered, but old install package fileName is %s and new install package fileName is %s",
                        appModule.getAppName(), appModule.getVersion(), appModule.getInstallPackage().getFileName(), oldModuleVo.getInstallPackage().getFileName()));
            }
            else if(!oldModuleVo.getInstallPackage().getMd5().equals(appModule.getInstallPackage().getMd5()))
            {
                logger.error(String.format("%s's version %s has been registered, but old install package md5 is %s and new install package md5 is %s",
                        appModule.getAppName(), appModule.getVersion(), appModule.getInstallPackage().getFileName(), oldModuleVo.getInstallPackage().getFileName()));
                throw new ParamException(String.format("%s's version %s has been registered, but old install package fileName is %s and new install package fileName is %s",
                        appModule.getAppName(), appModule.getVersion(), appModule.getInstallPackage().getMd5(), oldModuleVo.getInstallPackage().getMd5()));
            }
            else if(oldModuleVo.getCfgs().size() != appModule.getCfgs().size())
            {
                logger.error(String.format("%s's version %s has been registered, but old has %d cfg and new has is %d",
                        appModule.getAppName(), appModule.getVersion(), appModule.getInstallPackage().getFileName(), oldModuleVo.getInstallPackage().getFileName()));
                throw new ParamException(String.format("%s's version %s has been registered, but old install package fileName is %s and new install package fileName is %s",
                        appModule.getAppName(), appModule.getVersion(), appModule.getCfgs().size(), oldModuleVo.getCfgs().size()));
            }
            else
            {
                Map<String, AppCfgFilePo> cfgFileMap = oldModuleVo.getCfgs().stream().collect(Collectors.toMap(AppCfgFilePo::getFileName, Function.identity()));
                for(AppCfgFilePo cfgFilePo : appModule.getCfgs())
                {
                    if(!cfgFileMap.containsKey(cfgFilePo.getFileName()))
                    {
                        logger.error(String.format("%s's version %s has been registered, but cfg fileName not equal",
                                appModule.getAppName(), appModule.getVersion()));
                        throw new ParamException(String.format("%s's version %s has been registered, but cfg fileName not equal",
                                appModule.getAppName(), appModule.getVersion()));
                    }
                    else if(!cfgFileMap.get(cfgFilePo.getFileName()).getDeployPath().equals(cfgFilePo.getDeployPath()))
                    {
                        logger.error(String.format("%s's version %s has been registered, but cfg deploy path not equal",
                                appModule.getAppName(), appModule.getVersion()));
                        throw new ParamException(String.format("%s's version %s has been registered, but cfg deploy path not equal",
                                appModule.getAppName(), appModule.getVersion()));
                    }
                }
            }

            logger.error(String.format("%s's version %s has been registered", appModule.getAppName(), appModule.getVersion()));
            throw new ParamException(String.format("%s's version %s has been registered", appModule.getAppName(), appModule.getVersion()));
        }
        String directory = CMDBTools.getAppModuleDirectory(appModule);
        String tmpSaveDir = CMDBTools.getTempSaveDir(DigestUtils.md5DigestAsHex(directory.getBytes()));
        List<DeployFileInfo> fileList = new ArrayList<>();
        String downloadUrl = CMDBTools.getInstallPackageDownloadUrl(this.publishNexusHostUrl, appModule.getInstallPackage());
        logger.debug(String.format("download cfg from %s", downloadUrl));
        String savePth = nexusService.downloadFile(this.nexusUserName, this.nexusPassword, downloadUrl, tmpSaveDir, appModule.getInstallPackage().getFileName());
        String md5 = DigestUtils.md5DigestAsHex(new FileInputStream(savePth));
        if(!md5.equals(appModule.getInstallPackage().getMd5()))
        {
            logger.error(String.format("install package %s verify md5 FAIL : report=%s and download=%s",
                    appModule.getInstallPackage().getFileName(), appModule.getInstallPackage().getMd5(), md5));
            throw new ParamException(String.format("install package %s verify md5 FAIL : report=%s and download=%s",
                    appModule.getInstallPackage().getFileName(), appModule.getInstallPackage().getMd5(), md5));
        }
        DeployFileInfo fileInfo = new DeployFileInfo();
        fileInfo.setFileName(appModule.getInstallPackage().getFileName());
        fileInfo.setNexusAssetId(appModule.getInstallPackage().getNexusAssetId());
        fileInfo.setNexusDirectory(appModule.getInstallPackage().getNexusDirectory());
        fileInfo.setNexusRepository(appModule.getInstallPackage().getNexusRepository());
        fileInfo.setLocalSavePath(savePth);
        fileInfo.setFileMd5(appModule.getInstallPackage().getMd5());
        fileInfo.setExt(appModule.getInstallPackage().getExt());
        fileInfo.setBasePath(appModule.getBasePath());
        fileInfo.setDeployPath(appModule.getBasePath());
        fileList.add(fileInfo);
        for(AppCfgFilePo cfg : appModule.getCfgs())
        {
            downloadUrl = CMDBTools.getAppCfgDownloadUrl(this.publishNexusHostUrl, cfg);
            logger.debug(String.format("download cfg from %s", downloadUrl));
            savePth = nexusService.downloadFile(this.nexusUserName, this.nexusPassword, downloadUrl, tmpSaveDir, cfg.getFileName());
            md5 = DigestUtils.md5DigestAsHex(new FileInputStream(savePth));
            if(!md5.equals(cfg.getMd5()))
            {
                logger.error(String.format("cfg %s verify md5 FAIL : report=%s and download=%s",
                        cfg.getFileName(), cfg.getMd5(), md5));
                throw new ParamException(String.format("cfg %s verify md5 FAIL : report=%s and download=%s",
                        cfg.getFileName(), cfg.getMd5(), md5));
            }
            fileInfo = new DeployFileInfo();
            fileInfo.setFileName(cfg.getFileName());
            fileInfo.setNexusAssetId(cfg.getNexusAssetId());
            fileInfo.setNexusDirectory(directory);
            fileInfo.setNexusRepository(this.appRepository);
            fileInfo.setLocalSavePath(savePth);
            fileInfo.setFileMd5(cfg.getMd5());
            fileInfo.setExt(cfg.getExt());
            fileInfo.setBasePath(appModule.getBasePath());
            fileInfo.setDeployPath(cfg.getDeployPath());
            fileList.add(fileInfo);
        }
        Map<String, NexusAssetInfo> assetMap = this.nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, this.appRepository, directory, fileList.toArray(new DeployFileInfo[0])).stream().collect(Collectors.toMap(NexusAssetInfo::getPath, Function.identity()));
        AppPo appPo = new AppPo(appModule);
        Date now = new Date();
        appPo.setCreateTime(now);
        appPo.setUpdateTime(now);
        this.appMapper.insert(appPo);
        appModule.getInstallPackage().setAppId(appPo.getAppId());
        appModule.getInstallPackage().setNexusAssetId(assetMap.get(String.format("%s/%s", directory, appModule.getInstallPackage().getFileName())).getId());
        this.appInstallPackageMapper.insert(appModule.getInstallPackage());
        for(AppCfgFilePo cfgFilePo : appModule.getCfgs())
        {
            cfgFilePo.setAppId(appPo.getAppId());
            cfgFilePo.setNexusAssetId(assetMap.get(String.format("%s/%s", directory, cfgFilePo.getFileName())).getId());
            this.appCfgFileMapper.insert(cfgFilePo);
        }
    }

    private String checkModuleParam(AppModuleVo appModuleVo)
    {
        StringBuffer sb = new StringBuffer();
        if(StringUtils.isBlank(appModuleVo.getAppName()))
        {
            sb.append("appName is blank,");
        }
        if(StringUtils.isBlank(appModuleVo.getVersion()))
        {
            sb.append("appVersion is blank,");
        }
        if(StringUtils.isBlank(appModuleVo.getBasePath()))
        {
            sb.append("default base path is blank,");
        }
        if(appModuleVo.getInstallPackage() == null)
        {
            sb.append("install package is null,");
        }
        if(appModuleVo.getCfgs() == null || appModuleVo.getCfgs().size() == 0)
        {
            sb.append("cfg is null,");
        }
        return sb.toString().replaceAll(",$", "");
    }

    @Override
    public PlatformUpdateSchemaInfo createDemoNewPlatform(String platformId, String platformName, int bkBizId, int bkCloudId, List<String> planAppList) throws ParamException, InterfaceCallException, LJPaasException
    {
        Date now = new Date();
        PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
        if(platformPo != null)
        {
            logger.error(String.format("demo platform[id=%s, name=%s] exist", platformId, platformName));
            throw new ParamException(String.format("demo platform[id=%s, name=%s] exist", platformId, platformName));
        }
        List<LJBizInfo> bizList = paasService.queryAllBiz();
        Map<String, LJBizInfo> bizMap = bizList.stream().collect(Collectors.toMap(LJBizInfo::getBkBizName, Function.identity()));
        if(!bizMap.containsKey(platformName))
        {
            logger.error(String.format("biz %s not exist at lj paas", platformName));
            throw new ParamException(String.format("biz %s not exist at lj paas", platformName));
        }
        else if(bizMap.get(platformName).getBkBizId() != bkBizId)
        {
            logger.error(String.format("%s bkBizId is %d not %d", bizMap.get(platformName).getBkBizId(), bkBizId));
            throw new ParamException(String.format("%s bkBizId is %d not %d", bizMap.get(platformName).getBkBizId(), bkBizId));

        }
        Map<String, List<String>> planAppMap = new HashMap<>();
        for(String planApp : planAppList)
        {
            String[] arr = planApp.split("##");
            if(!planAppMap.containsKey(arr[0]))
            {
                planAppMap.put(arr[0], new ArrayList<>());
            }
            planAppMap.get(arr[0]).add(planApp);
        }
        List<AppModuleVo> appList = this.appModuleMapper.select(null, null, null, null);
        Map<String, List<AppModuleVo>> appMap = appList.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        PlatformUpdateSchemaInfo schema = new PlatformUpdateSchemaInfo();
        schema.setTitle(String.format("%s(%s)平台新建计划", platformName, platformId));
        schema.setPlatformName(platformName);
        schema.setComment(String.format("通过程序自动创建的%s(%s)平台新建计划", platformName, platformId));
        schema.setCreateTime(now);
        schema.setStatus(UpdateStatus.CREATE);
        schema.setTaskType(PlatformUpdateTaskType.CREATE);
        schema.setExecuteTime(now);
        schema.setDeadline(now);
        schema.setUpdateTime(now);
        schema.setPlatformId(platformId);
        schema.setDomainUpdatePlanList(new ArrayList<>());
        Set<String> ipSet = new HashSet<>();
        Map<String, BizSetDefine> setDefineMap = this.paasService.queryCCODBizSet(false).stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        for(BizSetDefine setDefine : setDefineMap.values())
        {
            if(setDefine.getApps().length == 0)
                continue;
            DomainUpdatePlanInfo planInfo = new DomainUpdatePlanInfo();
            planInfo.setUpdateType(DomainUpdateType.ADD);
            planInfo.setStatus(UpdateStatus.CREATE);
            planInfo.setComment("由程序自动生成的新建域");
            String domainId = setDefine.getFixedDomainId();
            String domainName = setDefine.getFixedDomainName();
            if(StringUtils.isBlank(domainId))
            {
                domainId = "newCreateTestDomain";
                domainName = "新建测试域";
            }
            planInfo.setDomainId(domainId);
            planInfo.setDomainName(domainName);
            planInfo.setCreateTime(now);
            planInfo.setUpdateTime(now);
            planInfo.setExecuteTime(now);
            planInfo.setAppUpdateOperationList(new ArrayList<>());
            planInfo.setBkSetName(setDefine.getName());
            planInfo.setSetId(setDefine.getId());
            for(String appName : setDefine.getApps())
            {
                if(planAppMap.containsKey(appName))
                {
                    for(String planApp : planAppMap.get(appName))
                    {
                        String[] arr = planApp.split("##");
                        String appAlias = arr[1];
                        String version = arr[2];
                        String hostIp = arr[3].split("@")[1];
                        ipSet.add(hostIp);
                        String[] pathArr = arr[3].split("@")[0].replaceAll("^/", "").split("/");
                        pathArr[1] = appAlias;
                        String basePath = String.format("/%s", String.join("/", pathArr));
                        Map<String, AppModuleVo> versionMap = appMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
                        if(!version.contains(version))
                        {
                            logger.error(String.format("create demo platform create schema FAIL : %s has not version=%s",
                                    appName, version));
                            throw new ParamException(String.format("create demo platform create schema FAIL : %s has not version=%s",
                                    appName, version));
                        }
                        AppModuleVo appModuleVo = versionMap.get(version);
                        AppUpdateOperationInfo addOperationInfo = new AppUpdateOperationInfo();
                        addOperationInfo.setHostIp(hostIp);
                        addOperationInfo.setOperation(AppUpdateOperation.ADD);
                        addOperationInfo.setCfgs(new ArrayList<>());
                        for(AppCfgFilePo appCfgFilePo : appModuleVo.getCfgs())
                        {
                            AppFileNexusInfo info = new AppFileNexusInfo();
                            info.setDeployPath(appCfgFilePo.getDeployPath());
                            info.setExt(appCfgFilePo.getExt());
                            info.setFileName(appCfgFilePo.getFileName());
                            info.setFileSize(0);
                            info.setMd5(appCfgFilePo.getMd5());
                            info.setNexusAssetId(appCfgFilePo.getNexusAssetId());
                            info.setNexusPath(String.format("%s/%s", appCfgFilePo.getNexusDirectory(), appCfgFilePo.getFileName()));
                            info.setNexusRepository(appCfgFilePo.getNexusRepository());
                            addOperationInfo.getCfgs().add(info);
                        }
                        addOperationInfo.setBasePath(basePath);
                        addOperationInfo.setAppRunner(appAlias);
                        addOperationInfo.setAppAlias(appAlias);
                        addOperationInfo.setAppName(appName);
                        addOperationInfo.setTargetVersion(version);
                        planInfo.getAppUpdateOperationList().add(addOperationInfo);
                    }
                }

            }
            schema.getDomainUpdatePlanList().add(planInfo);
        }
        Map<String, LJHostInfo> hostMap = paasService.queryBKHost(bkBizId, null, null, null, null)
                .stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity()));
        for(String hostIp : ipSet)
        {
            if(!hostMap.containsKey(hostIp))
            {
                logger.error(String.format("%s has not %s host", platformName, hostIp));
                throw new LJPaasException(String.format("%s has not %s host", platformName, hostIp));
            }
        }
        paasService.resetExistBiz(bkBizId, new ArrayList<>(setDefineMap.keySet()));
        schema.setBkBizId(bkBizId);
        platformPo = new PlatformPo();
        platformPo.setUpdateTime(now);
        platformPo.setStatus(CCODPlatformStatus.SCHEMA_CREATE_PLATFORM.id);
        platformPo.setCcodVersion("CCOD4.1");
        platformPo.setBkBizId(bkBizId);
        platformPo.setBkCloudId(bkCloudId);
        platformPo.setCreateTime(now);
        platformPo.setComment("create by tools for test");
        platformPo.setPlatformId(platformId);
        platformPo.setPlatformName(platformName);
        this.platformMapper.insert(platformPo);
        List<LJSetInfo> setList = this.paasService.queryBkBizSet(bkBizId);
        List<LJHostInfo> idleHostList = paasService.queryBizIdleHost(bkBizId);
        String checkResult = checkPlatformUpdateTask(schema, new ArrayList<>(), appList, new ArrayList<>(), setList, idleHostList, new ArrayList<>());
        if(StringUtils.isNotBlank(checkResult))
        {
            logger.error(String.format("demo platform generate fail : %s", checkResult));
            throw new ParamException(String.format("demo platform generate fail : %s", checkResult));
        }
        PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
        schemaPo.setContext(JSONObject.toJSONString(schema).getBytes());
        schemaPo.setPlatformId(platformId);
        this.platformUpdateSchemaMapper.insert(schemaPo);
        this.platformUpdateSchemaMap.put(platformId, schema);
        return schema;
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

    @Test
    public void schemaParamTest()
    {
        PlatformUpdateSchemaParamVo paramVo = new PlatformUpdateSchemaParamVo();
        paramVo.setBkBizId(0);
        paramVo.setBkCloudId(8);
        paramVo.setPlatformId("ccodDevelopTestPlatform");
        paramVo.setPlatformName("ccod开发测试平台");
        paramVo.setTaskType(PlatformUpdateTaskType.CREATE);
        String script = "glsServer##glsServer##ece10ef28dce83ab36e4d79213ec4f69##/home/ccodrunner/Platform@10.130.41.218\n" +
                "LicenseServer##license##5214##/home/ccodrunner/Platform@10.130.41.218\n" +
                "configserver##configserver##aca2af60caa0fb9f4af57f37f869dafc90472525##/home/cfs/Platform@10.130.41.218\n" +
                "gls##gls##10309##/home/ccodrunner/resin-4.0.13/webapps@10.130.41.218\n" +
                "dcms##dcms##11110##/home/ccodrunner/resin-4.0.13/webapps@10.130.41.218\n" +
                "dcmsWebservice##dcmsWebservice##20503##/home/ccodrunner/resin-4.0.13/webapps@10.130.41.218\n" +
                "dcmsRecord##dcmsRecord##21763##/home/ccodrunner/resin-4.0.13/webapps@10.130.41.218\n" +
                "dcmsStaticsReport##dcmsStatics##20537##/home/ccodrunner/resin-4.0.13/webapps@10.130.41.218\n" +
                "dcmsStaticsReport##dcmsStaticsReport##20528##/home/ccodrunner/resin-4.0.13/webapps@10.130.41.218\n" +
                "safetyMonitor##safetyMonitor##20383##/home/ccodrunner/resin-4.0.13/webapps@10.130.41.218\n" +
                "dcmssg##dcmsSG##20070##/home/ccodrunner/resin-4.0.13/webapps@10.130.41.218\n" +
                "customWebservice##customWebservice##19553##/home/ccodrunner/resin-4.0.13/webapps@10.130.41.218\n" +
                "dcmsx##dcmsx##master_8efabf4##/home/ccodrunner/resin-4.0.13/webapps@10.130.41.218\n" +
                "slee##slee##3.1.5.0##/home/slee/Platform/slee/ChannelSoft/CsCCP/SoftSwitch/lib@10.130.41.218\n" +
                "UCGateway##UCGateway##b4c50cc9602c11c9cbfae23d07f134dc##/home/ccodrunner/SmartDialer4.1/Service@10.130.41.218\n" +
                "AppGateWay##AppGateWay##c03e1e3fedf73f25a1565c602b8e4040##/home/ccodrunner/SmartDialer4.1/Service@10.130.41.218\n" +
                "DialEngine##DialEngine##24ae5d2c45523ab5c7e0da7b86db4c18##/home/ccodrunner/SmartDialer4.1/Service@10.130.41.218\n" +
                "cmsserver##cms##4c303e2a4b97a047f63eb01b247303c9306fbda5##/home/channelsoft/Platform@10.130.41.218\n" +
                "cmsserver##cms##4c303e2a4b97a047f63eb01b247303c9306fbda5##/home/ccodrunner/Platform@10.130.41.218\n" +
                "UCDServer##ucds##deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e##/home/ccodrunner/Platform@10.130.41.218\n" +
                "ucxserver##ucx##1fef2157ea07c483979b424c758192bd709e6c2a##/home/ccodrunner/Platform@10.130.41.218\n" +
                "DDSServer##dds##150:18722##/home/ccodrunner/Platform@10.130.41.218\n" +
                "dcs##dcs##155:21974##/home/ccodrunner/Platform@10.130.41.218\n" +
                "StatSchedule##ss##154:21104##/home/ccodrunner/Platform@10.130.41.218\n" +
                "EAService##eas##216:11502##/home/ccodrunner/Platform@10.130.41.218\n" +
                "dcproxy##dcproxy##195:21857##/home/ccodrunner/Platform@10.130.41.218\n" +
                "daengine##daengine##179:20744##/home/ccodrunner/Platform@10.130.41.218";
        paramVo.setPlanAppList(Arrays.asList(script.split("\n")));
        System.out.println(JSONObject.toJSONString(paramVo));
    }

    @Test
    public void splitTest()
    {
        String path = "/home/ccodrunner/resin-4.0.13/webapps";
        String appAlias = "dcmsx";
        path = path.replaceAll("^/", "");
        String[] arr = path.split("/");
        arr[1] = appAlias;
        System.out.println(String.join("/", arr));
    }
}
