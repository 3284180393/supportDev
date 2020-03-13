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
import com.channelsoft.ccod.support.cmdb.utils.HttpRequestTools;
import com.channelsoft.ccod.support.cmdb.utils.ServiceUnitUtils;
import com.channelsoft.ccod.support.cmdb.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.ResourceUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    @Value("${cmdb.url}")
    private String cmdbUrl;

    @Value("${nexus.platform-app-cfg-repository}")
    private String platformAppCfgRepository;

    @Value("${nexus.app-module-repository}")
    private String appRepository;

    @Value("${nexus.tmp-platform-app-cfg-repository}")
    private String platformTmpCfgRepository;

    @Value("${nexus.nexus-docker-url}")
    private String nexusDockerUrl;

    @Value("${nexus.image-repository}")
    private String imageRepository;

    @Value("${nexus.platform-deploy-script-repository}")
    private String platformDeployScriptRepository;

    @Value("${ccod.platform-deploy-template}")
    private String platformDeployScriptFileName;

    @Value("${git.k8s_deploy_git_url}")
    private String k8sDeployGitUrl;

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

    @Value("${ccod.domain-id-regex}")
    private String domainIdRegex;

    private String domainIdFmt = "%s%s";

    private String appAliasFmt = "%s%d";

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
//            platformAppCollectService.collectPlatformAppData("gscsShlt", "公司测试上海联通", null, null, null, null);
//            this.startCollectPlatformAppData("gscsShlt", "公司测试上海联通", null, null, null, null);
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

    private String getTempSaveDir(String directory) {
        String saveDir = String.format("%s/downloads/%s", System.getProperty("user.dir"), directory);
        return saveDir;
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
                Map<String, List<PlatformAppModuleVo>> domainNameAppModuleMap = domainAppMap.get(reportDomainId)
                        .stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getDomainName));
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
        String appDirectory = appPo.getAppNexusDirectory();
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
            String group = appPo.getAppNexusGroup();
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
            logger.error(String.format("cmdb data is not matched with nexus, so delete them first : nexus has appName=%s and version=%s record but cmdb has not",
                    appName, appVersion));
            for(NexusAssetInfo assetInfo : appFileAssetList)
            {
                nexusService.deleteAsset(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, assetInfo.getId());
            }
            String group = appPo.getAppNexusGroup();
            appFileAssetList = this.nexusService.queryGroupAssetMap(this.nexusHostUrl, this.nexusUserName, nexusPassword, this.appRepository, group);
            if(appFileAssetList.size() != 0)
            {
                logger.error(String.format("delete assets at %s/%s/%s fail", this.nexusHostUrl, this.appRepository, appPo.getAppNexusDirectory()));
                throw new NexusException(String.format("delete assets at %s/%s/%s fail", this.nexusHostUrl, this.appRepository, appPo.getAppNexusDirectory()));
            }
            logger.debug(String.format("delete assets at %s/%s/%s success", this.nexusHostUrl, this.appRepository, appPo.getAppNexusDirectory()));
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
        String compareResult = compareAppFileWithNexusRecord(moduleVo, appFileAssetList);
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
     * @param moduleVo 应用模块
     * @param NexusFileList 该应用存储在nexus的文件信息
     * @return 对比结果,如果完全符合返回""
     */
    private String compareAppFileWithNexusRecord(AppModuleVo moduleVo, List<NexusAssetInfo> NexusFileList)
    {
        String appName = moduleVo.getAppName();
        String version = moduleVo.getVersion();
        AppInstallPackagePo installPackage= moduleVo.getInstallPackage();
        List<AppCfgFilePo> cfgs = moduleVo.getCfgs();
        logger.debug(String.format("begin to compare appName=%s and version=%s installPackage and cfgs with nexus record",
                appName, version));
        Map<String, NexusAssetInfo> nexusFileMap = NexusFileList.stream().collect(Collectors.toMap(NexusAssetInfo::getPath, Function.identity()));
        StringBuffer sb = new StringBuffer();
        String directory = moduleVo.getAppNexusDirectory();
        String path = String.format("%s/%s", directory, installPackage.getFileName());
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
                    path = String.format("%s/%s", directory, cfg.getFileName());
                    if(!nexusFileMap.containsKey(path))
                    {
                        sb.append(String.format("cfg=%s not in nexus,", cfg.getFileName()));
                    }
                }
            }
        }
        logger.info(String.format("the result of files of appName=%s with version=%s compare with nexus is : %s",
                appName, version, sb.toString()));
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
        String directory = platformAppPo.getPlatformAppDirectory(appPo.getAppName(), appPo.getVersion(), platformAppPo);
        logger.debug(String.format("begin to handle %s cfgs", directory));
        String tmpSaveDir = getTempSaveDir(DigestUtils.md5DigestAsHex(directory.getBytes()));
        for(AppFileNexusInfo cfg : cfgs)
        {
            String downloadUrl = cfg.getFileNexusDownloadUrl(this.nexusHostUrl);
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


    private void addNewDomainToPlatform(DomainUpdatePlanInfo planInfo, List<AppUpdateOperationInfo> domainApps,
                              List<AppModuleVo> appList, PlatformPo platform, String setId, LJSetInfo bkSet, List<LJHostInfo> bkHostList)
            throws InterfaceCallException, LJPaasException, NexusException, IOException
    {
        Date now = new Date();
        DomainPo newDomain = new DomainPo(planInfo.getDomainId(), planInfo.getDomainName(), platform.getPlatformId(),
                DomainStatus.RUNNING, "create by domain add plan", planInfo.getBkSetName(),
                planInfo.getOccurs(), planInfo.getMaxOccurs(), planInfo.getTags());
//        newDomain.setComment("");
//        newDomain.setDomainId(planInfo.getDomainId());
//        newDomain.setPlatformId(platform.getPlatformId());
//        newDomain.setStatus(1);
//        newDomain.setUpdateTime(now);
//        newDomain.setCreateTime(now);
//        newDomain.setDomainName(planInfo.getDomainName());
//        newDomain.setType(planInfo.getBkSetName());
//        newDomain.setMaxOccurs(planInfo.getMaxOccurs());
//        newDomain.setOccurs(planInfo.getOccurs());
//        newDomain.setTags(planInfo.getTags());
        this.domainMapper.insert(newDomain);
        deployAppsToDomainHost(newDomain, setId, bkSet, bkHostList, domainApps, appList);
    }

    /**
     * 将指定域的一组应用删除
     * @param domain 指定删除应用的域
     * @param bkSet 该域归属的蓝鲸paas的set
     * @param deleteOperationList 需要移除的应用操作列表
     * @param appBkModuleList 被移除的应用对应的蓝鲸模块关系列表
     * @throws ParamException
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果失败
     */
    private void removeAppsFromDomainHost(DomainPo domain, LJSetInfo bkSet, List<AppUpdateOperationInfo> deleteOperationList, List<PlatformAppBkModulePo> appBkModuleList)
            throws ParamException, InterfaceCallException, LJPaasException
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
                                           List<PlatformAppBkModulePo> appBkModuleList, Map<String, BizSetDefine> bizSetDefineMap)
    {
        if(schema.getDomainUpdatePlanList() == null)
        {
            schema.setDomainUpdatePlanList(new ArrayList<>());
        }
        Map<String, DomainPo> domainMap = domainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        Map<String, DomainPo> domainNameMap = domainList.stream().collect(Collectors.toMap(DomainPo::getDomainName, Function.identity()));
        Map<String, LJSetInfo> setMap = bkSetList.stream().collect(Collectors.toMap(LJSetInfo::getBkSetName, Function.identity()));
        PlatformUpdateTaskType taskType = schema.getTaskType();
        StringBuffer sb = new StringBuffer();
        for(DomainUpdatePlanInfo planInfo : schema.getDomainUpdatePlanList())
        {
            if(StringUtils.isBlank(planInfo.getDomainId()) || StringUtils.isBlank(planInfo.getDomainName()))
            {
                return "domainId and domainName of domain update plan can not be blank";
            }
            else if(!Pattern.matches(this.domainIdRegex, planInfo.getDomainId()))
            {
                sb.append(String.format("%s,", planInfo.getDomainId()));
            }
        }
        if(StringUtils.isNotBlank(sb.toString()))
        {
            return String.format("domainId %s is not regal : only wanted 0-9a-z or - and - can not be beginning or end", sb.toString().replaceAll(",$", ""));
        }
        Map<String, List<DomainUpdatePlanInfo>> updateDomainMap = schema.getDomainUpdatePlanList().stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getDomainId));
        Map<String, List<PlatformAppPo>> domainAppMap = deployApps.stream().collect(Collectors.groupingBy(PlatformAppPo::getDomainId));
        for(String domainId : updateDomainMap.keySet())
        {
            if(updateDomainMap.get(domainId).size() > 1)
            {
                logger.error(String.format("domainId %s duplicate", domainId));
                sb.append(String.format("%s;", domainId));
            }
        }
        if(StringUtils.isNotBlank(sb.toString()))
        {
            return String.format("domainId %s duplicate", sb.toString().replaceAll(";$", ""));
        }
        updateDomainMap = schema.getDomainUpdatePlanList().stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getDomainName));
        for(String domainName : updateDomainMap.keySet())
        {
            if(updateDomainMap.get(domainName).size() > 1)
            {
                logger.error(String.format("domainName %s duplicate", domainName));
                sb.append(String.format("%s;", domainName));
            }
        }
        if(StringUtils.isNotBlank(sb.toString()))
        {
            return String.format("domainName %s duplicate", sb.toString().replaceAll(";$", ""));
        }
        for(DomainUpdatePlanInfo planInfo : schema.getDomainUpdatePlanList())
        {
            DomainUpdateType updateType = planInfo.getUpdateType();
            if(updateType == null)
            {
                sb.append(String.format("DomainUpdateType of %s is blank;", JSONObject.toJSONString(planInfo)));
                continue;
            }
            switch (updateType)
            {
                case ADD:
                    if(domainMap.containsKey(planInfo.getDomainId()))
                    {
                        sb.append(String.format("%s domain %s, but %s exist;",
                                updateType.name, planInfo.getDomainId(), planInfo.getDomainId()));
                        continue;
                    }
                    else if(domainNameMap.containsKey(planInfo.getDomainName()))
                    {
                        sb.append(String.format("%s domain %s, but %s exist;",
                                updateType.name, planInfo.getDomainName(), planInfo.getDomainName()));
                        continue;
                    }
                    break;
                default:
                    if(!domainMap.containsKey(planInfo.getDomainId()))
                    {
                        sb.append(String.format("%s domain %s, but %s not exist;",
                                updateType.name, planInfo.getDomainId(), planInfo.getDomainId()));
                        continue;
                    }
                    else if(!domainNameMap.containsKey(planInfo.getDomainName()))
                    {
                        sb.append(String.format("%s domain %s, but %s not exist;",
                                updateType.name, planInfo.getDomainName(), planInfo.getDomainName()));
                        continue;
                    }
                    else if(!setMap.containsKey(planInfo.getBkSetName()))
                    {
                        sb.append(String.format("%s %s of %s of domain %s not exist;",
                                updateType.name, planInfo.getDomainId(), planInfo.getBkSetName()));
                        continue;
                    }
                    break;
            }
            switch (taskType)
            {
                case CREATE:
                    switch (updateType)
                    {
                        case ADD:
                            if(!bizSetDefineMap.containsKey(planInfo.getBkSetName()))
                            {
                                sb.append(String.format("setName %s of new create domain %s not exist",
                                        planInfo.getBkSetName(), planInfo.getDomainId()));
                                continue;
                            }
                            break;
                        default:
                            sb.append(String.format("%s of %s only support %s %s, not %s;",
                                    taskType.name, schema.getPlatformId(), DomainUpdateType.ADD.name, planInfo.getDomainId(), updateType.name));
                            continue;
                    }
                    break;
                case DELETE:
                    switch (updateType)
                    {
                        case DELETE:
                            break;
                        default:
                            sb.append(String.format("%s of %s only support %s %s, not %s;",
                                    taskType.name, schema.getPlatformId(), DomainUpdateType.ADD.name, planInfo.getDomainId(), updateType.name));
                            continue;
                    }
                    break;
                case UPDATE:
                    switch (updateType)
                    {
                        case ADD:
                            if(!setMap.containsKey(planInfo.getBkSetName()))
                            {
                                sb.append(String.format("bkSetName=%s of domain %s not exist;",
                                        planInfo.getBkSetName(), planInfo.getDomainId()));
                                continue;
                            }
                            break;
                        default:
                            break;
                    }
                    break;
            }
            if(planInfo.getAppUpdateOperationList() == null || planInfo.getAppUpdateOperationList().size() == 0)
            {
//                sb.append(String.format("operation of %s %s is blank;", updateType.name, planInfo.getDomainId()));
                continue;
            }
            List<PlatformAppPo> domainAppList = new ArrayList<>();
            if(domainAppMap.containsKey(planInfo.getDomainId()))
            {
                domainAppList = domainAppMap.get(planInfo.getDomainId());
            }
            String operationCheckResult = updateOperationCheck(updateType, planInfo.getAppUpdateOperationList(), appList, domainAppList, appBkModuleList, bkHostList);
            sb.append(operationCheckResult);
        }
//        Map<String, List<AppUpdateOperationInfo>> ipAppMap = allOperationList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getHostIp));
//        for(String hostIp : ipAppMap.keySet())
//        {
//            Map<String, List<AppUpdateOperationInfo>> appNameMap = ipAppMap.get(hostIp).stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
//            for(String appName : appNameMap.keySet())
//            {
//                if(appNameMap.get(appName).size() > 1)
//                {
//                    Set<String> aliasSet = new HashSet<>();
//                    for(AppUpdateOperationInfo opt : appNameMap.get(appName))
//                    {
//                        switch (opt.getOperation())
//                        {
//                            case ADD:
//                            case CFG_UPDATE:
//                            case VERSION_UPDATE:
//                                if(aliasSet.contains(opt.getAppAlias()))
//                                {
//                                    sb.append(String.format("%s at %s alias duplicate;", appName, hostIp));
//                                }
//                                else
//                                {
//                                    aliasSet.add(opt.getAppAlias());
//                                }
//                            default:
//                                break;
//                        }
//                    }
//                }
//            }
//        }
        return sb.toString();
    }


    /**
     * 验证应用升级操作的相关蚕食是否正确
     * @param updateType 域升级方案类型
     * @param operationList 域升级的应用升级操作明细
     * @param appList 当前应用列表
     * @param deployApps 域已经部署的应用列表
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
        Set<String> deployAppSet = new HashSet<>();
        for(PlatformAppPo po : deployApps)
        {
            if(deployAppSet.contains(po.getAppAlias()))
            {
                sb.append(String.format("%s,", po.getAppAlias()));
            }
            else
            {
                deployAppSet.add(po.getAppAlias());
            }
        }
        if(StringUtils.isNotBlank(sb.toString()))
        {
            return String.format("alias %s duplicate;", sb.toString().replaceAll(",$", ""));
        }
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
                            sb.append(String.format("%s : version of %s is blank;", operation.name, operationInfo.getAppName()));
                        }
                        else if(!versionMap.containsKey(operationInfo.getTargetVersion()))
                        {
                            sb.append(String.format("%s : version %s of %s not exist;", operation.name, operationInfo.getTargetVersion(), operationInfo.getAppName()));
                        }
                        if(StringUtils.isBlank(operationInfo.getAppAlias()))
                        {
                            sb.append(String.format("%s : alias of %s is blank;", operation.name, operationInfo.getAppName()));
                        }
                        else if(deployAppSet.contains(operationInfo.getAppAlias()))
                        {
                            sb.append(String.format("%s : alias %s duplicate;", operation.name, operationInfo.getAppAlias()));
                        }
                        else
                        {
                            deployAppSet.add(operationInfo.getAppAlias());
                        }
                        if(StringUtils.isBlank(operationInfo.getBasePath()))
                        {
                            sb.append(String.format("%s : basePath of %s is blank;", operation.name, operationInfo.getAppAlias()));
                        }
                        if(StringUtils.isBlank(operationInfo.getAppRunner()))
                        {
                            sb.append(String.format("%s : appRunner of %s is blank;", operation.name, operationInfo.getAppAlias()));
                        }
                        if(operationInfo.getCfgs() == null || operationInfo.getCfgs().size() == 0)
                        {
                            sb.append(String.format("%s : cfg of %s is 0;", operation.name, operationInfo.getAppAlias()));
                        }
                        else
                        {
                            String compareResult = cfgFileCompare(versionMap.get(operationInfo.getTargetVersion()), operationInfo.getCfgs());
                            if(StringUtils.isNotBlank(compareResult))
                                sb.append(String.format("%s : cfg of %s is not match [%s];",  operation.name, operationInfo.getAppAlias(), compareResult));
                        }
                        break;
                    case VERSION_UPDATE:
                        if(!platformAppMap.containsKey(operationInfo.getPlatformAppId()))
                        {
                            sb.append(String.format("%s : platformAppId=%d of %s not exist", operation.name, operationInfo.getPlatformAppId(), operationInfo.getAppName()));
                        }
                        if(StringUtils.isBlank(operationInfo.getTargetVersion()))
                        {
                            sb.append(String.format("%s : target version of %s is blank;", operation.name, operationInfo.getAppName()));
                        }
                        else if(!versionMap.containsKey(operationInfo.getTargetVersion()))
                        {
                            sb.append(String.format("%s  : target version %s of %s not exist;", operation.name, operationInfo.getTargetVersion(), operationInfo.getAppName()));
                        }
                        if(StringUtils.isBlank(operationInfo.getOriginalVersion()))
                        {
                            sb.append(String.format("%s : original version of %s is blank;", operation.name, operationInfo.getAppName()));
                        }
                        else if(!versionMap.containsKey(operationInfo.getOriginalVersion()))
                        {
                            sb.append(String.format("%s : original version %s of %s not exist;", operation.name, operationInfo.getOriginalVersion(), operationInfo.getAppName()));
                        }
                        if(StringUtils.isBlank(operationInfo.getAppAlias()))
                        {
                            sb.append(String.format("%s : alias of %s is blank;", operation.name, operationInfo.getAppName()));
                        }
                        else if(!deployAppSet.contains(operationInfo.getAppAlias()))
                        {
                            sb.append(String.format("%s : alias of %s not exist;", operation.name, operationInfo.getAppName()));
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
                            sb.append(String.format("%s : original version of %s is blank;", operation.name, operationInfo.getAppName()));
                        }
                        else if(!versionMap.containsKey(operationInfo.getOriginalVersion()))
                        {
                            sb.append(String.format("%s : original version %s of %s not exist;", operation.name, operationInfo.getOriginalVersion(), operationInfo.getAppName()));
                        }
                        if(StringUtils.isBlank(operationInfo.getAppAlias()))
                        {
                            sb.append(String.format("%s : alias of %s is blank;", operation.name, JSONObject.toJSONString(operationInfo)));
                        }
                        else if(!deployAppSet.contains(operationInfo.getAppAlias()))
                        {
                            sb.append(String.format("%s : %s of %s not exist;", operation.name, operationInfo.getAppAlias(), operationInfo.getAppName()));
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
                            String compareResult = cfgFileCompare(versionMap.get(operationInfo.getOriginalVersion()), operationInfo.getCfgs());
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
                        else if(!deployAppSet.contains(operationInfo.getAppAlias()))
                        {
                            sb.append(String.format("%s : %s of %s not exist;", operation.name, operationInfo.getAppAlias(), operationInfo.getAppName()));
                        }
                        else
                        {
                            deployAppSet.remove(operationInfo.getAppAlias());
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
     * @throws ParamException
     * @throws InterfaceCallException 调用蓝鲸api或是nexus的api失败
     * @throws LJPaasException 蓝鲸api返回接口调用失败或是解析蓝鲸api返回结果失败
     * @throws NexusException nexus api返回调用失败或是解析nexus api的返回结果失败
     * @throws IOException 处理文件失败
     */
    private void recordPlatformUpdateResult(PlatformUpdateSchemaInfo schemaInfo, PlatformPo platformPo,
                                                              List<DomainPo> domainList, List<AppModuleVo> appList, List<PlatformAppPo> deployApps,
                                                              List<PlatformAppBkModulePo> appBkModuleList, List<LJSetInfo> bkSetList,
                                                              List<LJHostInfo> bkHostList) throws ParamException, InterfaceCallException, LJPaasException, NexusException, IOException
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
                    addNewDomainToPlatform(planInfo, planInfo.getAppUpdateOperationList(),
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
                        for(AppUpdateOperationInfo cfgOperationInfo : operationMap.get(AppUpdateOperation.CFG_UPDATE))
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

    public void updatePlatformUpdateSchema(PlatformUpdateSchemaInfo updateSchema) throws NotSupportSetException, NotSupportAppException, ParamException, InterfaceCallException, LJPaasException, NexusException, IOException {
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
                platformPo = new PlatformPo(updateSchema.getPlatformId(), updateSchema.getPlatformName(),
                        updateSchema.getBkBizId(), updateSchema.getBkCloudId(), CCODPlatformStatus.SCHEMA_CREATE_PLATFORM,
                        updateSchema.getCcodVersion(), "create by platform create schema");
                if(StringUtils.isNotBlank(updateSchema.getCcodVersion()))
                {
                    platformPo.setCcodVersion("CCOD4.1");
                }
                this.platformMapper.insert(platformPo);
            }
        }
        if(!platformPo.getPlatformName().equals(updateSchema.getPlatformName()))
        {
            logger.error(String.format("platformName of %s is %s, not %s",
                    platformPo.getPlatformId(), platformPo.getPlatformName(), updateSchema.getPlatformName()));
            throw new ParamException(String.format("bkBizName of bizBkId is %s, not %s", updateSchema.getBkBizId(), bkBiz.getBkBizName(), updateSchema.getPlatformName()));
        }
        CCODPlatformStatus platformStatus = CCODPlatformStatus.getEnumById(platformPo.getStatus());
        if(platformStatus == null)
        {
            logger.error(String.format("%s status %d is unknown", platformPo.getPlatformId(), platformPo.getStatus()));
            throw new ParamException(String.format("%s status %d is unknown", platformPo.getPlatformId(), platformPo.getStatus()));
        }
        List<BizSetDefine> setDefineList = paasService.queryCCODBizSet(false);
        Map<String, BizSetDefine> bizSetDefineMap = setDefineList.stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        List<DomainPo> domainList = new ArrayList<>();
        List<PlatformAppDeployDetailVo> platformDeployApps = new ArrayList<>();
        if(PlatformUpdateTaskType.UPDATE.equals(updateSchema.getTaskType()))
        {
            domainList = this.domainMapper.select(updateSchema.getPlatformId(), null);
            platformDeployApps = platformAppDeployDetailMapper.selectPlatformApps(updateSchema.getPlatformId(), null, null);
        }
        switch (updateSchema.getTaskType()) {
            case CREATE:
                makeupPlatformUpdateSchema(updateSchema, domainList, platformDeployApps, setDefineList);
                switch (platformStatus) {
                    case SCHEMA_CREATE_PLATFORM:
                        if (StringUtils.isBlank(updateSchema.getCcodVersion())) {
                            logger.error(String.format("ccod version of %s is blank", updateSchema.getPlatformId()));
                            throw new ParamException(String.format("ccod version of %s is blank", updateSchema.getPlatformId()));
                        }
                        break;
                    default:
                        logger.error(String.format("not support %s platform %s which status is %s",
                                updateSchema.getTaskType().name, updateSchema.getPlatformId(), platformStatus.name));
                        throw new ParamException(String.format("not support %s platform %s which status is %s",
                                updateSchema.getTaskType().name, updateSchema.getPlatformId(), platformStatus.name));
                }
                break;
            case UPDATE:
//                Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = platformAppDeployDetailMapper.selectPlatformApps(updateSchema.getPlatformId(), null, null)
//                        .stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
//                List<DomainUpdatePlanInfo> addDomainPlanList = new ArrayList<>();
//                for(DomainUpdatePlanInfo planInfo : updateSchema.getDomainUpdatePlanList())
//                {
//                    if(planInfo.getUpdateType().equals(DomainUpdateType.ADD))
//                    {
//                        addDomainPlanList.add(planInfo);
//                    }
//                    else if(planInfo.getUpdateType().equals(DomainUpdateType.UPDATE))
//                    {
//                        if(StringUtils.isBlank(planInfo.getDomainId()))
//                        {
//                            logger.error(String.format("domainId of domain update plan is blank"));
//                            throw new ParamException(String.format("domainId of domain update plan is blank"));
//                        }
//                        else if(!domainAppMap.containsKey(planInfo.getDomainId()))
//                        {
//                            logger.error(String.format("updated domain %s not deploy any app", planInfo.getDomainId()));
//                            throw new ParamException(String.format("updated domain %s not deploy any app", planInfo.getDomainId()));
//                        }
//                        else if(!bizSetDefineMap.containsKey(planInfo.getBkSetName()))
//                        {
//                            logger.error(String.format("set %s not support", planInfo.getBkSetName()));
//                            throw new NotSupportSetException(String.format("set %s not support", planInfo.getBkSetName()));
//                        }
//                        else
//                        {
//                            List<AppUpdateOperationInfo> addAppOptList = new ArrayList<>();
//                            for(AppUpdateOperationInfo opt : planInfo.getAppUpdateOperationList())
//                            {
//                                if(opt.getOperation().equals(AppUpdateOperation.ADD))
//                                {
//                                    addAppOptList.add(opt);
//                                }
//                            }
//                            if(addAppOptList.size() > 0)
//                            {
//                                BizSetDefine setDefine = bizSetDefineMap.get(planInfo.getBkSetName());
//                                List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.get(planInfo.getDomainId());
//                                autoDefineAlias4DomainNewApp(addAppOptList, domainAppList, setDefine);
//                            }
//
//                        }
//                    }
//                }
//                if(addDomainPlanList.size() > 0)
//                {
//                    Map<String, List<DomainUpdatePlanInfo>> setNewDomainMap = addDomainPlanList.stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getBkSetName));
//                    for(String bkSetName : setNewDomainMap.keySet())
//                    {
//                        if(!bizSetDefineMap.containsKey(bkSetName))
//                        {
//                            logger.error(String.format("set %s not support", bkSetName));
//                            throw new NotSupportSetException(String.format("set %s not support", bkSetName));
//                        }
//                        BizSetDefine setDefine = bizSetDefineMap.get(bkSetName);
//                        autoDefineDomainId4SetDomain(setNewDomainMap.get(bkSetName), domainList, setDefine);
//                    }
//        }
//
                makeupPlatformUpdateSchema(updateSchema, domainList, platformDeployApps, setDefineList);
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
                        schemaCheckResult = checkPlatformUpdateTask(updateSchema, domainList, appList, deployApps, bkSetList, bkHostList, appBkModuleList, bizSetDefineMap);
                        if(StringUtils.isNotBlank(schemaCheckResult))
                        {
                            logger.error(String.format("schema is not legal : %s", schemaCheckResult));
                            throw new ParamException(String.format("schema is not legal : %s", schemaCheckResult));
                        }
                        Map<String, BizSetDefine> setDefineMap = setDefineList.stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
                        bkSetList = this.paasService.resetExistBiz(updateSchema.getBkBizId(), new ArrayList<>(setDefineMap.keySet()));
                        recordPlatformUpdateResult(updateSchema, platformPo, domainList, appList, deployApps, appBkModuleList, bkSetList, bkHostList);
                        if(this.platformUpdateSchemaMap.containsKey(updateSchema.getPlatformId()))
                            this.platformUpdateSchemaMap.remove(updateSchema.getPlatformId());
                        this.platformUpdateSchemaMapper.delete(updateSchema.getPlatformId());
                        platformPo.setStatus(CCODPlatformStatus.RUNNING.id);
                        platformPo.setUpdateTime(now);
                        this.platformMapper.update(platformPo);
                        break;
                    default:
                        schemaCheckResult = checkPlatformUpdateTask(updateSchema, domainList, appList, deployApps, bkSetList, bkHostList, appBkModuleList, bizSetDefineMap);
                        if(StringUtils.isNotBlank(schemaCheckResult))
                        {
                            logger.error(String.format("schema is not legal : %s", schemaCheckResult));
                            throw new ParamException(String.format("schema is not legal : %s", schemaCheckResult));
                        }
                        generatePlatformDeployParamAndScript(updateSchema);
                        logger.info(String.format("updateSchema=%s", JSONObject.toJSONString(updateSchema)));
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
                        schemaCheckResult = checkPlatformUpdateTask(updateSchema, domainList, appList, deployApps, bkSetList, bkHostList, appBkModuleList, bizSetDefineMap);
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
                        schemaCheckResult = checkPlatformUpdateTask(updateSchema, domainList, appList, deployApps, bkSetList, bkHostList, appBkModuleList, bizSetDefineMap);
                        if(StringUtils.isNotBlank(schemaCheckResult))
                        {
                            logger.error(String.format("schema is not legal : %s", schemaCheckResult));
                            throw new ParamException(String.format("schema is not legal : %s", schemaCheckResult));
                        }
                        generatePlatformDeployParamAndScript(updateSchema);
                        logger.info(String.format("updateSchema=%s", JSONObject.toJSONString(updateSchema)));
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
            throws ParamException, InterfaceCallException, LJPaasException
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
        logger.info(String.format("update %s version from %s to %s at %s SUCCESS",
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
        platform = new PlatformPo(platformId, platformName, bkBizId, bkCloudId, CCODPlatformStatus.RUNNING,
                "CCOD4.1", "通过程序自动创建的demo平台");
        platformMapper.insert(platform);
        PlatformUpdateSchemaInfo schema = new PlatformUpdateSchemaInfo(platformId, platformName, bkBizId, bkCloudId,
                "CCOD4.1", PlatformUpdateTaskType.CREATE,
                String.format("%s(%s)平台新建计划", platformName, platformId),
                String.format("通过程序自动创建的%s(%s)平台新建计划"));
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
        Map<String, BizSetDefine> bizSetDefineMap = paasService.queryCCODBizSet(false).stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        String planCheckResult = checkPlatformUpdateTask(schema, new ArrayList<>(), appList, new ArrayList<>(), setList, bkHostList, new ArrayList<>(), bizSetDefineMap);
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
    private List<CCODSetInfo> generateCCODSetInfo(List<PlatformAppDeployDetailVo> deployAppList, List<String> setNames) throws ParamException, NotSupportAppException
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
        for(String setName : setNames)
        {
            if(!setAppMap.containsKey(setName))
            {
                CCODSetInfo setInfo = new CCODSetInfo(setName);
                setList.add(setInfo);
            }
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
        Map<String, BizSetDefine> bizSetMap = paasService.queryCCODBizSet(false).stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
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
                setList = generateCCODSetInfo(deployAppList, new ArrayList<>(bizSetMap.keySet()));
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
                setList = generateCCODSetInfo(deployAppList, new ArrayList<>(bizSetMap.keySet()));
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
                setList = generateCCODSetInfo(deployAppList, new ArrayList<>(bizSetMap.keySet()));
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
        String directory = appModule.getAppNexusDirectory();
        String tmpSaveDir = getTempSaveDir(DigestUtils.md5DigestAsHex(directory.getBytes()));
        List<DeployFileInfo> fileList = new ArrayList<>();
        String downloadUrl = appModule.getInstallPackage().getFileNexusDownloadUrl(this.publishNexusHostUrl);
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
            downloadUrl = cfg.getFileNexusDownloadUrl(this.publishNexusHostUrl);
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
        appModule.getInstallPackage().setNexusRepository(this.appRepository);
        appModule.getInstallPackage().setNexusDirectory(directory);
        appModule.getInstallPackage().setAppId(appPo.getAppId());
        appModule.getInstallPackage().setNexusAssetId(assetMap.get(String.format("%s/%s", directory, appModule.getInstallPackage().getFileName())).getId());
        this.appInstallPackageMapper.insert(appModule.getInstallPackage());
        for(AppCfgFilePo cfgFilePo : appModule.getCfgs())
        {
            cfgFilePo.setAppId(appPo.getAppId());
            cfgFilePo.setNexusDirectory(directory);
            cfgFilePo.setNexusRepository(this.appRepository);
            cfgFilePo.setNexusAssetId(assetMap.get(String.format("%s/%s", directory, cfgFilePo.getFileName())).getId());
            this.appCfgFileMapper.insert(cfgFilePo);
        }
    }

    @Override
    public void updateAppModule(AppModuleVo appModule) throws NotSupportAppException, ParamException, InterfaceCallException, NexusException, IOException {
        logger.debug(String.format("begin to modify cfg of app=[%s] in cmdb", JSONObject.toJSONString(appModule)));
        if(StringUtils.isBlank(appModule.getAppName()))
        {
            logger.error(String.format("app name is blank"));
            throw new ParamException(String.format("app name is blank"));
        }
        if(StringUtils.isBlank(appModule.getVersion()))
        {
            logger.error(String.format("version of %s is blank", appModule.getAppName()));
            throw new ParamException(String.format("version of %s is blank", appModule.getAppName()));
        }
        if(appModule.getInstallPackage() == null)
        {
            logger.error(String.format("install package of %s version %s is blank", appModule.getAppName(), appModule.getVersion()));
            throw new ParamException(String.format("install package of %s version %s is blank", appModule.getAppName(), appModule.getVersion()));
        }
        if(appModule.getCfgs() == null || appModule.getCfgs().size() == 0)
        {
            logger.error(String.format("cfg of %s version %s is blank", appModule.getAppName(), appModule.getVersion()));
            throw new ParamException(String.format("cfg of %s version %s is blank", appModule.getAppName(), appModule.getVersion()));
        }
        Map<String, List<BizSetDefine>> appSetRelationMap = this.paasService.getAppBizSetRelation();
        if (!appSetRelationMap.containsKey(appModule.getAppName())) {
            logger.error(String.format("appName=%s is not supported by cmdb", appModule.getAppName()));
            throw new NotSupportAppException(String.format("appName=%s is not supported by cmdb", appModule.getAppName()));
        }
        AppModuleVo oldModuleVo = this.appModuleMapper.selectByNameAndVersion(appModule.getAppName(), appModule.getVersion());
        if (oldModuleVo == null)
        {
            logger.error(String.format("%s version %s has not registered", appModule.getAppName(), appModule.getVersion()));
            throw new ParamException(String.format("%s version %s has not registered", appModule.getAppName(), appModule.getVersion()));
        }
        String directory = appModule.getAppNexusDirectory();
        String tmpSaveDir = getTempSaveDir(DigestUtils.md5DigestAsHex(directory.getBytes()));
        List<DeployFileInfo> fileList = new ArrayList<>();
        String downloadUrl = appModule.getInstallPackage().getFileNexusDownloadUrl(this.publishNexusHostUrl);
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
            downloadUrl = cfg.getFileNexusDownloadUrl(this.publishNexusHostUrl);
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
        logger.debug(String.format("delete old version install package"));
        this.appInstallPackageMapper.delete(null, oldModuleVo.getAppId());
        logger.debug(String.format("delete old version cfgs"));
        this.appCfgFileMapper.delete(null, oldModuleVo.getAppId());
        oldModuleVo.getInstallPackage().setNexusAssetId(assetMap.get(String.format("%s/%s", directory, appModule.getInstallPackage().getFileName())).getId());
        this.appInstallPackageMapper.insert(oldModuleVo.getInstallPackage());
        for(AppCfgFilePo cfgFilePo : appModule.getCfgs())
        {
            cfgFilePo.setAppId(oldModuleVo.getAppId());
            cfgFilePo.setNexusDirectory(directory);
            cfgFilePo.setNexusRepository(this.appRepository);
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
    public PlatformUpdateSchemaInfo createNewPlatform(PlatformCreateParamVo paramVo) throws ParamException, NotSupportSetException, NotSupportAppException, InterfaceCallException, LJPaasException {
        PlatformUpdateSchemaInfo schemaInfo;
        if(paramVo.getCreateMethod() == PlatformCreateParamVo.MANUAL)
        {
            schemaInfo = createNewEmptyPlatformSchema(paramVo.getPlatformId(), paramVo.getPlatformName(), paramVo.getBkBizId(), paramVo.getBkCloudId());
        }
        else if(paramVo.getCreateMethod() == PlatformCreateParamVo.CLONE)
        {
            if(StringUtils.isBlank(paramVo.getParams()))
            {
                logger.error(String.format("cloned platform id is blank"));
                throw new ParamException(String.format("cloned platform id is blank"));
            }
            schemaInfo = cloneExistPlatform(paramVo.getParams(), paramVo.getPlatformId(), paramVo.getPlatformName(), paramVo.getBkBizId(), paramVo.getBkCloudId());
        }
        else if(paramVo.getCreateMethod() == PlatformCreateParamVo.PREDEFINE)
        {
            if(StringUtils.isBlank(paramVo.getParams()))
            {
                logger.error(String.format("apps of pre define is blank"));
                throw new ParamException(String.format("apps of pre define is blank"));
            }
            List<String> planAppList = new ArrayList<>();
            String[] planApps = paramVo.getParams().split("\n");
            for(String planApp : planApps)
            {
                if(StringUtils.isNotBlank(planApp))
                    planAppList.add(planApp);
            }
            schemaInfo = createDemoNewPlatform(paramVo.getPlatformId(), paramVo.getPlatformName(), paramVo.getBkBizId(), paramVo.getBkCloudId(), planAppList);
        }
        else
        {
            logger.error(String.format("unknown platform create method %d", paramVo.getCreateMethod()));
            throw new ParamException(String.format("unknown platform create method %d", paramVo.getCreateMethod()));
        }
        List<BizSetDefine> setDefineList = paasService.queryCCODBizSet(false);
        makeupPlatform4CreateSchema(schemaInfo, setDefineList);
        return schemaInfo;
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
        PlatformUpdateSchemaInfo schema = new PlatformUpdateSchemaInfo(platformId, platformName, bkBizId, bkCloudId, "CCOD4.1",
                PlatformUpdateTaskType.CREATE, String.format("%s(%s)平台新建计划", platformName, platformId),
                String.format("通过程序自动创建的%s(%s)平台新建计划", platformName, platformId));
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
                domainId = "new-create-test-domain";
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
                        if(!versionMap.containsKey(version))
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
        platformPo = new PlatformPo(platformId, platformName, bkBizId, bkCloudId, CCODPlatformStatus.SCHEMA_CREATE_PLATFORM,
                "CCOD4.1", "create by tools for test");
        this.platformMapper.insert(platformPo);
        List<LJSetInfo> setList = this.paasService.queryBkBizSet(bkBizId);
        List<LJHostInfo> idleHostList = paasService.queryBizIdleHost(bkBizId);
        Map<String, BizSetDefine> bizSetDefineMap = paasService.queryCCODBizSet(false).stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        String checkResult = checkPlatformUpdateTask(schema, new ArrayList<>(), appList, new ArrayList<>(), setList, idleHostList, new ArrayList<>(), bizSetDefineMap);
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


    private PlatformUpdateSchemaInfo createNewEmptyPlatformSchema(String platformId, String platformName, int bkBizId, int bkCloudId) throws ParamException, InterfaceCallException, LJPaasException
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
        List<LJHostInfo> hostList = paasService.queryBKHost(bkBizId, null, null, null, null);
        if(hostList.size() == 0)
        {
            logger.error(String.format("%s has not any host", platformName));
            throw new ParamException(String.format("%s has not any host", platformName));
        }
        PlatformUpdateSchemaInfo schemaInfo = new PlatformUpdateSchemaInfo();
        schemaInfo.setDomainUpdatePlanList(new ArrayList<>());
        schemaInfo.setBkCloudId(bkCloudId);
        schemaInfo.setBkBizId(bkBizId);
        schemaInfo.setExecuteTime(now);
        schemaInfo.setDeadline(now);
        schemaInfo.setUpdateTime(now);
        schemaInfo.setPlatformId(platformId);
        schemaInfo.setTaskType(PlatformUpdateTaskType.CREATE);
        schemaInfo.setStatus(UpdateStatus.CREATE);
        schemaInfo.setCreateTime(now);
        schemaInfo.setComment("auto created");
        schemaInfo.setPlatformName(platformName);
        platformPo = new PlatformPo(platformId, platformName, bkBizId, bkCloudId, CCODPlatformStatus.SCHEMA_CREATE_PLATFORM,
                "CCOD4.1", "create by tools for test");
        this.platformMapper.insert(platformPo);
        PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
        schemaPo.setContext(JSONObject.toJSONString(schemaInfo).getBytes());
        schemaPo.setPlatformId(platformId);
        return schemaInfo;
    }

    @Override
    public PlatformUpdateSchemaInfo createDemoUpdatePlatform(String platformId, String platformName, int bkBizId) throws ParamException, InterfaceCallException, LJPaasException {
        logger.debug(String.format("begin to create demo update platform[platformId=%s,platformName=%s,bkBizId=%d]",
                platformId, platformName, bkBizId));
        Date now = new Date();
        PlatformPo platform = platformMapper.selectByPrimaryKey(platformId);
        if(platform == null)
        {
            logger.error(String.format("platformId=%s not exist", platformId));
            throw new ParamException(String.format("platformId=%s not exist", platformId));
        }
        if(!platform.getPlatformName().equals(platformName))
        {
            logger.error(String.format("name of platformId=%s is %s not %s",
                    platformId, platform.getPlatformName(), platformName));
            throw new ParamException(String.format("name of platformId=%s is %s not %s",
                    platformId, platform.getPlatformName(), platformName));
        }
       LJBizInfo bkBiz = this.paasService.queryBizInfoById(bkBizId);
        if(bkBiz == null)
        {
            logger.error(String.format("bkBizId=%d biz not exist", bkBizId));
            throw new ParamException(String.format("bkBizId=%d biz not exist", bkBizId));
        }
        if(!bkBiz.getBkBizName().equals(platformName))
        {
            logger.error(String.format("bkBizId=%d biz name is %s not %s",
                    bkBizId, bkBiz.getBkBizName(), platformName));
            throw new ParamException(String.format("bkBizId=%d biz name is %s not %s",
                    bkBizId, bkBiz.getBkBizName(), platformName));
        }
        List<DomainPo> domainList = domainMapper.select(platformId, null);
        if(domainList.size() == 0)
        {
            logger.error(String.format("%s has 0 domain", platformId));
            throw new ParamException(String.format("%s has 0 domain", platformId));
        }
        List<PlatformAppDeployDetailVo> deployApps = platformAppDeployDetailMapper.selectPlatformApps(platformId, null, null);
        if(deployApps.size() == 0)
        {
            logger.error(String.format("%s has deployed 0 app", platformId));
            throw new ParamException(String.format("%s has deployed 0 app", platformId));
        }
        List<AppModuleVo> appModuleList = appModuleMapper.select(null, null, null, null);
        Map<String, List<AppModuleVo>> appModuleMap = appModuleList.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        List<DomainUpdatePlanInfo> planList = new ArrayList<>();
        Map<String, List<PlatformAppDeployDetailVo>> setAppMap = deployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getBkSetName));
        for(String bkSetName : setAppMap.keySet())
        {
            List<PlatformAppDeployDetailVo> setAppList = setAppMap.get(bkSetName);
            Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = setAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
            int index = 1;
            for(String domainId : domainAppMap.keySet())
            {
                List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.get(domainId);
                if("域服务".equals(bkSetName))
                {
                    DomainUpdatePlanInfo planInfo = new DomainUpdatePlanInfo();
                    planInfo.setAppUpdateOperationList(new ArrayList<>());
                    planInfo.setStatus(UpdateStatus.CREATE);
                    planInfo.setSetId(domainAppList.get(0).getSetId());
                    planInfo.setBkSetName(bkSetName);
                    planInfo.setExecuteTime(now);
                    planInfo.setUpdateTime(now);
                    planInfo.setCreateTime(now);
                    planInfo.setDomainName(domainAppList.get(0).getDomainName());
                    planInfo.setDomainId(domainId);
                    planInfo.setComment(String.format("域%s移除方案", domainId));
                    planInfo.setUpdateType(DomainUpdateType.DELETE);
                    for(PlatformAppDeployDetailVo deployApp : domainAppList)
                    {
                        AppUpdateOperationInfo opt = new AppUpdateOperationInfo();
                        opt.setAppName(deployApp.getAppName());
                        opt.setAppAlias(deployApp.getAppAlias());
                        opt.setOperation(AppUpdateOperation.DELETE);
                        opt.setHostIp(deployApp.getHostIp());
                        opt.setPlatformAppId(deployApp.getPlatformAppId());
                        opt.setOriginalVersion(deployApp.getVersion());
                        planInfo.getAppUpdateOperationList().add(opt);
                    }
                    planList.add(planInfo);
                    planInfo = new DomainUpdatePlanInfo();
                    planInfo.setAppUpdateOperationList(new ArrayList<>());
                    planInfo.setStatus(UpdateStatus.CREATE);
                    planInfo.setSetId(domainAppList.get(0).getSetId());
                    planInfo.setBkSetName(bkSetName);
                    planInfo.setExecuteTime(now);
                    planInfo.setUpdateTime(now);
                    planInfo.setCreateTime(now);
                    planInfo.setDomainName(String.format("新建%s%d", domainAppList.get(0).getDomainName(), index));
                    planInfo.setDomainId(String.format("new-create-%s-%d", domainAppList.get(0).getDomainId(), index));
                    index++;
                    planInfo.setComment(String.format("域%s新建方案", planInfo.getDomainId()));
                    planInfo.setUpdateType(DomainUpdateType.ADD);
                    for(PlatformAppDeployDetailVo deployApp : domainAppList)
                    {
                        AppUpdateOperationInfo opt = new AppUpdateOperationInfo();
                        opt.setCfgs(new ArrayList<>());
                        for(PlatformAppCfgFilePo cfgFilePo : deployApp.getCfgs())
                        {
                            AppFileNexusInfo nexusInfo = new AppFileNexusInfo();
                            nexusInfo.setNexusRepository(cfgFilePo.getNexusRepository());
                            nexusInfo.setNexusPath(cfgFilePo.getFileNexusSavePath());
                            nexusInfo.setNexusAssetId(nexusInfo.getNexusAssetId());
                            nexusInfo.setMd5(cfgFilePo.getMd5());
                            nexusInfo.setFileSize(0);
                            nexusInfo.setFileName(cfgFilePo.getFileName());
                            nexusInfo.setExt(cfgFilePo.getExt());
                            nexusInfo.setDeployPath(cfgFilePo.getDeployPath());
                            opt.getCfgs().add(nexusInfo);
                        }
                        opt.setHostIp(deployApp.getHostIp());
                        opt.setOperation(AppUpdateOperation.ADD);
                        opt.setBasePath(deployApp.getBasePath());
                        opt.setAppRunner(deployApp.getAppRunner());
                        opt.setAppAlias(deployApp.getAppAlias());
                        opt.setAppName(deployApp.getAppName());
                        opt.setTargetVersion(deployApp.getVersion());
                        planInfo.getAppUpdateOperationList().add(opt);
                    }
                    planList.add(planInfo);
                }
                else
                {
                    DomainUpdatePlanInfo planInfo = new DomainUpdatePlanInfo();
                    planInfo.setAppUpdateOperationList(new ArrayList<>());
                    planInfo.setStatus(UpdateStatus.CREATE);
                    planInfo.setSetId(domainAppList.get(0).getSetId());
                    planInfo.setBkSetName(bkSetName);
                    planInfo.setExecuteTime(now);
                    planInfo.setUpdateTime(now);
                    planInfo.setCreateTime(now);
                    planInfo.setDomainName(domainAppList.get(0).getDomainName());
                    planInfo.setDomainId(domainId);
                    planInfo.setComment(String.format("域%s升级方案", domainId));
                    planInfo.setUpdateType(DomainUpdateType.UPDATE);
                    planList.add(planInfo);
                    for(PlatformAppDeployDetailVo deployApp : domainAppList)
                    {
                        if(index % 2 == 1)
                        {
                            if(appModuleMap.get(deployApp.getAppName()).size() > 1)
                            {
                                for(AppModuleVo moduleVo : appModuleMap.get(deployApp.getAppName()))
                                {
                                    if(!moduleVo.getVersion().equals(deployApp.getVersion()))
                                    {
                                        AppUpdateOperationInfo opt = new AppUpdateOperationInfo();
                                        opt.setTargetVersion(moduleVo.getVersion());
                                        opt.setAppName(deployApp.getAppName());
                                        opt.setAppAlias(deployApp.getAppAlias());
                                        opt.setAppRunner(deployApp.getAppRunner());
                                        opt.setBasePath(deployApp.getBasePath());
                                        opt.setOperation(AppUpdateOperation.VERSION_UPDATE);
                                        opt.setHostIp(deployApp.getHostIp());
                                        opt.setOriginalVersion(deployApp.getVersion());
                                        opt.setPlatformAppId(deployApp.getPlatformAppId());
                                        opt.setCfgs(new ArrayList<>());
                                        for(AppCfgFilePo cfgFilePo : moduleVo.getCfgs())
                                        {
                                            AppFileNexusInfo nexusInfo = new AppFileNexusInfo();
                                            nexusInfo.setNexusRepository(cfgFilePo.getNexusRepository());
                                            nexusInfo.setNexusPath(cfgFilePo.getNexusFileSavePath());
                                            nexusInfo.setNexusAssetId(nexusInfo.getNexusAssetId());
                                            nexusInfo.setMd5(cfgFilePo.getMd5());
                                            nexusInfo.setFileSize(0);
                                            nexusInfo.setFileName(cfgFilePo.getFileName());
                                            nexusInfo.setExt(cfgFilePo.getExt());
                                            nexusInfo.setDeployPath(cfgFilePo.getDeployPath());
                                            opt.getCfgs().add(nexusInfo);
                                        }
                                        planInfo.getAppUpdateOperationList().add(opt);
                                        break;
                                    }
                                }
                            }
                            else
                            {
                                AppUpdateOperationInfo opt = new AppUpdateOperationInfo();
                                opt.setPlatformAppId(deployApp.getPlatformAppId());
                                opt.setOriginalVersion(deployApp.getVersion());
                                opt.setHostIp(deployApp.getHostIp());
                                opt.setOperation(AppUpdateOperation.CFG_UPDATE);
                                opt.setBasePath(deployApp.getBasePath());
                                opt.setAppRunner(deployApp.getAppRunner());
                                opt.setAppAlias(deployApp.getAppAlias());
                                opt.setAppName(deployApp.getAppName());
                                opt.setCfgs(new ArrayList<>());
                                for(PlatformAppCfgFilePo cfgFilePo : deployApp.getCfgs())
                                {
                                    AppFileNexusInfo nexusInfo = new AppFileNexusInfo();
                                    nexusInfo.setNexusRepository(cfgFilePo.getNexusRepository());
                                    nexusInfo.setNexusPath(cfgFilePo.getFileNexusSavePath());
                                    nexusInfo.setNexusAssetId(nexusInfo.getNexusAssetId());
                                    nexusInfo.setMd5(cfgFilePo.getMd5());
                                    nexusInfo.setFileSize(0);
                                    nexusInfo.setFileName(cfgFilePo.getFileName());
                                    nexusInfo.setExt(cfgFilePo.getExt());
                                    nexusInfo.setDeployPath(cfgFilePo.getDeployPath());
                                    opt.getCfgs().add(nexusInfo);
                                }
                                planInfo.getAppUpdateOperationList().add(opt);
                            }
                        }
                        else
                        {
                            AppUpdateOperationInfo opt = new AppUpdateOperationInfo();
                            opt.setAppName(deployApp.getAppName());
                            opt.setAppAlias(deployApp.getAppAlias());
                            opt.setAppRunner(deployApp.getAppRunner());
                            opt.setBasePath(deployApp.getBasePath());
                            opt.setOperation(AppUpdateOperation.DELETE);
                            opt.setHostIp(deployApp.getHostIp());
                            opt.setOriginalVersion(deployApp.getVersion());
                            opt.setPlatformAppId(deployApp.getPlatformAppId());
                            planInfo.getAppUpdateOperationList().add(opt);
                        }
                    }
                }
            }
        }
        PlatformUpdateSchemaInfo schema = new PlatformUpdateSchemaInfo(platformId, platformName, bkBizId, platform.getBkCloudId(),
                "CCOD4.1", PlatformUpdateTaskType.UPDATE, "create demo update platform",
                "demo update platform create by program");
        schema.setDomainUpdatePlanList(planList);
        List<PlatformAppPo> platformAppList = platformAppMapper.select(platformId, null, null, null, null, null);
        List<LJSetInfo> bkSetList = paasService.queryBkBizSet(bkBizId);
        List<LJHostInfo> bkHostList = paasService.queryBKHost(bkBizId, null, null, null, null);
        List<PlatformAppBkModulePo> appBkModuleList = platformAppBkModuleMapper.select(platformId, null, null, null, null, null);
        Map<String, BizSetDefine> bizSetDefineMap = paasService.queryCCODBizSet(false).stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        String schemaCheckResult = checkPlatformUpdateTask(schema, domainList, appModuleList, platformAppList, bkSetList, bkHostList, appBkModuleList, bizSetDefineMap);
        if(StringUtils.isNotBlank(schemaCheckResult))
        {
            logger.error(String.format("generate schema fail : %s", schemaCheckResult));
            throw new ParamException(String.format("generate schema fail : %s", schemaCheckResult));
        }
        PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
        schemaPo.setPlatformId(platformId);
        schemaPo.setContext(JSONObject.toJSONString(schema).getBytes());
        this.platformUpdateSchemaMapper.insert(schemaPo);
        platform.setStatus(CCODPlatformStatus.SCHEMA_UPDATE_PLATFORM.id);
        platformMapper.update(platform);
        this.platformUpdateSchemaMap.put(platformId, schema);
        return schema;
    }

    /**
     * 为平台创建计划的新建域自动生成域id，为域部署的应用自动生成应用别名
     * @param schema 原始平台创建计划
     * @param setDefineList 预定义的ccod集群
     * @throws ParamException 输入参数有误
     * @throws NotSupportAppException
     * @throws NotSupportSetException
     */
    void makeupPlatform4CreateSchema(PlatformUpdateSchemaInfo schema, List<BizSetDefine> setDefineList) throws ParamException, NotSupportAppException, NotSupportSetException
    {
        Map<String, BizSetDefine> setDefineMap = setDefineList.stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        Map<String, List<DomainUpdatePlanInfo>> setDomainMap = schema.getDomainUpdatePlanList().stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getBkSetName));
        for(String setName : setDefineMap.keySet())
        {
            if(!setDomainMap.containsKey(setName))
                continue;
            List<DomainUpdatePlanInfo> planList = setDomainMap.get(setName);
            if(!setDefineMap.containsKey(setName))
            {
                logger.error(String.format("set name %s is not support", setName));
                throw new NotSupportSetException(String.format("set name %s is not support", setName));
            }
            BizSetDefine setDefine = setDefineMap.get(setName);
            String standardDomainId = setDefine.getFixedDomainId();
            List<String> usedDomainIds = new ArrayList<>();
            List<DomainUpdatePlanInfo> notDomainIdPlans = new ArrayList<>();
            for(DomainUpdatePlanInfo planInfo : planList)
            {
                if(StringUtils.isBlank(planInfo.getDomainId()))
                {
                    notDomainIdPlans.add(planInfo);
                }
                else
                {
                    usedDomainIds.add(planInfo.getDomainId());
                }
                Map<String, List<AppUpdateOperationInfo>> appOptMap = planInfo.getAppUpdateOperationList().stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
                for(String appName : appOptMap.keySet())
                {
                    if(!setDefine.getAppAliasMap().containsKey(appName))
                    {
                        logger.error(String.format("set %s not support %s", setName, appName));
                        throw new NotSupportAppException(String.format("set %s not support %s", setName, appName));
                    }
                    String standardAlias = setDefine.getAppAliasMap().get(appName);
                    List<AppUpdateOperationInfo> optList = appOptMap.get(appName);
                    List<AppUpdateOperationInfo> notAliasOpts = new ArrayList<>();
                    List<String> usedAlias = new ArrayList<>();
                    for(AppUpdateOperationInfo opt : optList)
                    {
                        if(StringUtils.isBlank(opt.getAppAlias()))
                        {
                            notAliasOpts.add(opt);
                        }
                        else
                        {
                            usedAlias.add(opt.getAppAlias());
                        }
                    }
                    if(optList.size() > 0) {
                        boolean onlyOne = optList.size() > 1 ? false : true;
                        for (AppUpdateOperationInfo opt : notAliasOpts) {
                            String alias = autoGenerateAlias(standardAlias, usedAlias, onlyOne);
                            opt.setAppAlias(alias);
                            usedAlias.add(alias);
                        }
                    }
                }
            }
            if(notDomainIdPlans.size() > 0)
            {
                for(DomainUpdatePlanInfo planInfo : notDomainIdPlans)
                {
                    String domainId = autoGenerateDomainId(standardDomainId, usedDomainIds);
                    planInfo.setDomainId(domainId);
                    usedDomainIds.add(domainId);
                }
            }
        }
    }


    /**
     * 为平台创建计划的新建域自动生成域id，为域部署的应用自动生成应用别名
     * @param schema 原始平台升级计划
     * @param existDomainList 平台已经存在的域
     * @param existAppList 平台已经部署的应用
     * @param setDefineList 预定义的ccod集群
     * @throws ParamException 输入参数有误
     * @throws NotSupportAppException
     * @throws NotSupportSetException
     */
    void makeupPlatformUpdateSchema(PlatformUpdateSchemaInfo schema, List<DomainPo> existDomainList, List<PlatformAppDeployDetailVo> existAppList, List<BizSetDefine> setDefineList) throws ParamException, NotSupportAppException, NotSupportSetException
    {
        Map<String, BizSetDefine> setDefineMap = setDefineList.stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        List<DomainUpdatePlanInfo> noDomainIdPlans = new ArrayList<>();
        Map<String, List<String>> setDomainIdMap = new HashMap<>();
        for(DomainUpdatePlanInfo planInfo : schema.getDomainUpdatePlanList())
        {
            if(!setDefineMap.containsKey(planInfo.getBkSetName()))
            {
                logger.error(String.format("error schema : set %s is not supported", planInfo.getBkSetName()));
                throw new NotSupportSetException(String.format("set %s is not supported", planInfo.getBkSetName()));
            }
            if(StringUtils.isBlank(planInfo.getDomainId()))
            {
                noDomainIdPlans.add(planInfo);
            }
            else
            {
                if(!setDomainIdMap.containsKey(planInfo.getBkSetName()))
                {
                    setDomainIdMap.put(planInfo.getBkSetName(), new ArrayList<>());
                }
                setDomainIdMap.get(planInfo.getBkSetName()).add(planInfo.getDomainId());
            }
        }
        Map<String, List<DomainPo>> setExistDomains = existDomainList.stream().collect(Collectors.groupingBy(DomainPo::getType));
        if(noDomainIdPlans.size() > 0)
        {
            Map<String, List<DomainUpdatePlanInfo>> setDomainMap = noDomainIdPlans.stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getBkSetName));
            for(String setName : setDomainMap.keySet())
            {
                BizSetDefine setDefine = setDefineMap.get(setName);
                List<String> usedDomainIds = new ArrayList<>();
                if(setExistDomains.containsKey(setName))
                {
                    for(DomainPo po : setExistDomains.get(setName))
                    {
                        usedDomainIds.add(po.getDomainId());
                    }
                }
                if(setDomainIdMap.containsKey(setName))
                {
                    usedDomainIds.addAll(setDomainIdMap.get(setName));
                }
                for(DomainUpdatePlanInfo planInfo : setDomainMap.get(setName))
                {
                    String domainId = autoGenerateDomainId(setDefine.getFixedDomainId(), usedDomainIds);
                    logger.debug(String.format("for domain %s of %s auto generate id %s",
                            planInfo.getDomainName(), planInfo.getBkSetName(), domainId));
                    planInfo.setDomainId(domainId);
                    usedDomainIds.add(domainId);
                }
            }
        }
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = existAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        for(DomainUpdatePlanInfo planInfo : schema.getDomainUpdatePlanList())
        {
            Map<String, List<String>> appAliasMap = new HashMap<>();
            List<AppUpdateOperationInfo> addOptNotAliasList = new ArrayList<>();
            BizSetDefine setDefine = setDefineMap.get(planInfo.getBkSetName());
            for(AppUpdateOperationInfo opt : planInfo.getAppUpdateOperationList())
            {
                if(!setDefine.getAppAliasMap().containsKey(opt.getAppName()))
                {
                    logger.error(String.format("set %s does not support %s", planInfo.getBkSetName(), opt.getAppName()));
                    throw new ParamException(String.format("set %s does not support %s", planInfo.getBkSetName(), opt.getAppName()));
                }
                if(StringUtils.isNotBlank(opt.getAppAlias()))
                {
                    String standAlias = setDefine.getAppAliasMap().get(opt.getAppName());
                    String regex = String.format("^%s($|[1-9]\\d*$)", standAlias);
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(opt.getAppAlias());
                    if(!matcher.find())
                    {
                        logger.error(String.format("%s is not a legal alias for appName=%s and standardAlias=%s",
                                opt.getAppAlias(), opt.getAppName(), standAlias));
                        throw new ParamException(String.format("%s is not a legal alias for appName=%s and standardAlias=%s",
                                opt.getAppAlias(), opt.getAppName(), standAlias));
                    }
                }
                if(AppUpdateOperation.ADD.equals(opt.getOperation()))
                {
                    if(StringUtils.isBlank(opt.getAppAlias()))
                        addOptNotAliasList.add(opt);
                    else
                    {
                        if(!appAliasMap.containsKey(opt.getAppName()))
                        {
                            appAliasMap.put(opt.getAppName(), new ArrayList<>());
                        }
                        appAliasMap.get(opt.getAppName()).add(opt.getAppAlias());
                    }
                }
                else
                {
                    if(StringUtils.isBlank(opt.getAppAlias()))
                    {
                        logger.error(String.format("alias of %s %s is blank", opt.getOperation().name, opt.getAppName()));
                        throw new ParamException(String.format("alias of %s %s is blank", opt.getOperation().name, opt.getAppName()));
                    }
                    if(!appAliasMap.containsKey(opt.getAppName()))
                    {
                        appAliasMap.put(opt.getAppName(), new ArrayList<>());
                    }
                    appAliasMap.get(opt.getAppName()).add(opt.getAppAlias());
                }
            }
            if(addOptNotAliasList.size() == 0)
                continue;
            String domainId = planInfo.getDomainId();
            List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
            Map<String, List<PlatformAppDeployDetailVo>> existAppMap = domainAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getAppName));
            Map<String, List<AppUpdateOperationInfo>> appAddOptMapp = addOptNotAliasList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));

            for(String appName : appAddOptMapp.keySet())
            {
                if(!setDefine.getAppAliasMap().containsKey(appName))
                {
                    logger.error(String.format("set %s not support app %s", planInfo.getBkSetName(), appName));
                    throw new NotSupportAppException(String.format("set %s not support app %s", planInfo.getBkSetName(), appName));
                }
                String standardAlias = setDefine.getAppAliasMap().get(appName);
                List<String> usedAliasList = new ArrayList<>();
                if(existAppMap.containsKey(appName))
                {
                    for(PlatformAppDeployDetailVo deployApp : existAppMap.get(appName))
                    {
                        usedAliasList.add(deployApp.getAppAlias());
                    }
                }
                if(appAliasMap.containsKey(appName))
                {
                    usedAliasList.addAll(appAliasMap.get(appName));
                }
                List<AppUpdateOperationInfo> opts = appAddOptMapp.get(appName);
                boolean onlyOne = opts.size() > 1 ? false : true;
                for(AppUpdateOperationInfo opt : opts)
                {
                    String alias = autoGenerateAlias(standardAlias, usedAliasList, onlyOne);
                    logger.debug(String.format("auto generate alias %s for %s/%s/%s",
                            alias, planInfo.getBkSetName(), domainId, appName));
                    opt.setAppAlias(alias);
                    opt.setAppRunner(alias);
                    usedAliasList.add(alias);
                }
            }
        }
        //对域下的应用按照别名排序
        for(DomainUpdatePlanInfo planInfo : schema.getDomainUpdatePlanList())
        {
            BizSetDefine setDefine = setDefineMap.get(planInfo.getBkSetName());
            List<AppUpdateOperationInfo> sortedOptList = sortAppUpdateOperations(planInfo.getAppUpdateOperationList(), setDefine);
            planInfo.setAppUpdateOperationList(sortedOptList);
        }
        //对set以及set下的domain排序
        List<DomainUpdatePlanInfo> sortedPlanList = new ArrayList<>();
        Map<String, List<DomainUpdatePlanInfo>> setDomainMap = schema.getDomainUpdatePlanList().stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getBkSetName));
        for(BizSetDefine setDefine : setDefineList)
        {
            if(!setDomainMap.containsKey(setDefine.getName()))
                continue;
            final String standardDomainId = setDefine.getFixedDomainId();
            List<DomainUpdatePlanInfo> setPlanList = setDomainMap.get(setDefine.getName());
            Collections.sort(setPlanList, new Comparator<DomainUpdatePlanInfo>(){
                @Override
                public int compare(DomainUpdatePlanInfo o1, DomainUpdatePlanInfo o2) {
                    int index1 = Integer.parseInt(o1.getDomainId().replaceAll(standardDomainId, ""));
                    int index2 = Integer.parseInt(o2.getDomainId().replaceAll(standardDomainId, ""));
                    return index1 - index2;
                }
            });
            sortedPlanList.addAll(setPlanList);
        }
        schema.setDomainUpdatePlanList(sortedPlanList);
    }

    private String autoGenerateDomainId(String standardDomainId, List<String> usedId) throws ParamException
    {
        String regex = String.format("^%s(0[1-9]|[1-9]\\d+)", standardDomainId);
        Pattern pattern = Pattern.compile(regex);
        int index = 0;
        for(String id : usedId)
        {
            Matcher matcher = pattern.matcher(id);
            if(!matcher.find())
            {
                logger.error(String.format("%s is an illegal tag for %s", id, standardDomainId));
                throw new ParamException(String.format("%s is an illegal tag for %s", id, standardDomainId));
            }
            String str = id.replaceAll(standardDomainId, "");
            if(StringUtils.isNotBlank(str))
            {
                int oldIndex = Integer.parseInt(str);
                if(oldIndex > index)
                {
                    index = oldIndex;
                }
            }
        }
        index++;
        String domainId = String.format("%s%s", standardDomainId, (index > 9 ? index + "" : "0" + index));
        return domainId;
    }

    private String autoGenerateAlias(String standardAlias, List<String> usedAlias, boolean onlyOne) throws ParamException
    {
        String regex = String.format("^%s\\d*", standardAlias);
        Pattern pattern = Pattern.compile(regex);
        int index = 0;
        for(String alias : usedAlias)
        {
            Matcher matcher = pattern.matcher(alias);
            if(!matcher.find())
            {
                logger.error(String.format("%s is an illegal tag for %s", alias, standardAlias));
                throw new ParamException(String.format("%s is an illegal tag for %s", alias, standardAlias));
            }
            String str = alias.replaceAll(standardAlias, "");
            if(StringUtils.isNotBlank(str))
            {
                int oldIndex = Integer.parseInt(str);
                if(oldIndex > index)
                {
                    index = oldIndex;
                }
            }
            else if(index == 0)
            {
                index = 1;
            }

        }
        index++;
        String appAlias = String.format("%s%s", standardAlias, index);
        if(index == 1 && onlyOne)
            appAlias = standardAlias;
        return appAlias;
    }

    /**
     * 为新加域自动生成域id
     * @param setDefine 新加域归属的集群定义
     * @param newDomainPlanList 新加的域列表
     * @param oldDomainList 平台已有域列表
     */
    void autoDefineDomainId4SetDomain(List<DomainUpdatePlanInfo> newDomainPlanList, List<DomainPo> oldDomainList, BizSetDefine setDefine)
    {
        String standId = setDefine.getFixedDomainId();
        String standIdRegex = String.format("^%s-?\\d+$", standId);
        int index = 0;
        for(DomainPo domainPo : oldDomainList)
        {
            int oldIndex = getIndexFromId(domainPo.getDomainId(), standIdRegex);
            if(oldIndex > index)
            {
                index = oldIndex;
            }
        }

        for(int i = 1; i <= newDomainPlanList.size(); i++)
        {
            DomainUpdatePlanInfo planInfo = newDomainPlanList.get(i - 1);
            planInfo.setDomainId(String.format(domainIdFmt, standId, i + index));
        }
    }

    /**
     * 为域新加app自动生成别名以及运行用户
     * @param newAddAppList 域所有的新加应用操作列表
     * @param domainExistAppList 该域已经部署的应用列表
     * @param setDefine 该域归属的集群定义
     * @throws NotSupportAppException 如果新加的域不被集群支持则抛出此异常
     */
    void autoDefineAlias4DomainNewApp(List<AppUpdateOperationInfo> newAddAppList, List<PlatformAppDeployDetailVo> domainExistAppList, BizSetDefine setDefine) throws NotSupportAppException
    {
        Map<String, List<AppUpdateOperationInfo>> addAppMap = newAddAppList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
        Map<String, List<PlatformAppDeployDetailVo>> deployedAppMap = domainExistAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getAppName));
        for(String appName : addAppMap.keySet())
        {
            if(!setDefine.getAppAliasMap().containsKey(appName))
            {
                logger.error(String.format("%s set not support %s app", setDefine.getName(), appName));
                throw new NotSupportAppException(String.format("%s set not support %s app", setDefine.getName(), appName));
            }
            String standAlias = setDefine.getAppAliasMap().get(appName);
            String aliasRegex = String.format("^%s-?\\d+$", standAlias);
            int index = 0;
            List<AppUpdateOperationInfo> newAddList = addAppMap.get(appName);
            if(deployedAppMap.containsKey(appName))
            {
                List<PlatformAppDeployDetailVo> deployedList = deployedAppMap.get(appName);
                for(PlatformAppDeployDetailVo deployApp : deployedList)
                {
                    int deployIndex = getIndexFromId(deployApp.getAppAlias(), aliasRegex);
                    if(deployIndex > index)
                    {
                        index = deployIndex;
                    }
                }
            }
            if(index == 0 && newAddList.size() == 1)
            {
                newAddList.get(0).setAppRunner(standAlias);
                newAddList.get(0).setAppAlias(standAlias);
            }
            else
            {
                for(int j = 1; j < newAddList.size(); j++)
                {
                    newAddList.get(j - 1).setAppAlias(String.format(this.appAliasFmt, standAlias, index + j));
                    newAddList.get(j - 1).setAppRunner(String.format(this.appAliasFmt, standAlias, index + j));
                }
            }
        }
    }

    private int getIndexFromId(String id, String idRegex)
    {
        int index = 0;
        Pattern idPattern = Pattern.compile(idRegex);
        Matcher idMatter = idPattern.matcher(id);
        if(idMatter.find())
        {
            String indexRegex = "0*\\d+$";
            Pattern indexPatter = Pattern.compile(indexRegex);
            Matcher indexMatcher = indexPatter.matcher(id);
            if(indexMatcher.find())
            {
                index = Integer.parseInt(indexMatcher.group());
            }
        }
        return index;
    }

    @Override
    public PlatformUpdateSchemaInfo cloneExistPlatform(String clonedPlatformId, String platformId, String platformName, int bkBizId, int bkCloudId) throws ParamException, NotSupportSetException, NotSupportAppException, InterfaceCallException, LJPaasException {
        logger.debug(String.format("begin to clone platform from %s with platformId=%s, platformName=%s, bkBizId=%s and bkCloudId=%s",
                clonedPlatformId, platformId, platformName, bkBizId, bkCloudId));
        PlatformPo clonedPlatform = platformMapper.selectByPrimaryKey(clonedPlatformId);
        if(clonedPlatform == null)
        {
            logger.error(String.format("id=%s platform not exist", clonedPlatformId));
            throw new ParamException(String.format("%s platform not exist", clonedPlatformId));
        }
        Map<String, DomainPo> clonedDomainMap = domainMapper.select(clonedPlatformId, null).stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        if(clonedDomainMap.size() == 0)
        {
            logger.error(String.format("not find any domain for %s", clonedPlatformId));
            throw new ParamException(String.format("%s has not any domain", clonedPlatform));
        }
        PlatformPo platform = platformMapper.selectByPrimaryKey(platformId);
        if(platform != null)
        {
            logger.error(String.format("id=%s platform has exist", platformId));
            throw new ParamException(String.format("id=%s platform has exist", platformId));
        }
        platform = platformMapper.selectByNameBizId(platformName, null);
        if(platform != null)
        {
            logger.error(String.format("name=%s platform has exist", platformName));
            throw new ParamException(String.format("name=%s platform has exist", platformName));
        }
        LJBizInfo bkBiz = this.paasService.queryBizInfoById(bkBizId);
        if(bkBiz == null)
        {
            logger.error(String.format("bkBizId=%d biz not exist", bkBizId));
            throw new ParamException(String.format("bkBizId=%d biz not exist", bkBizId));
        }
        if(!bkBiz.getBkBizName().equals(platformName))
        {
            logger.error(String.format("bkBizId=%d biz name is %s not %s",
                    bkBizId, bkBiz.getBkBizName(), platformName));
            throw new ParamException(String.format("bkBizId=%d biz name is %s not %s",
                    bkBizId, bkBiz.getBkBizName(), platformName));
        }
        List<LJHostInfo> hostList = paasService.queryBKHost(bkBizId, null, null, null, null);
        if(hostList == null || hostList.size() == 0)
        {
            logger.error(String.format("%s has not any host", platformName));
            throw new ParamException(String.format("%s has not any host", platformName));
        }
        List<PlatformAppDeployDetailVo> deployApps = platformAppDeployDetailMapper.selectPlatformApps(clonedPlatformId, null, null);
        Map<String, List<PlatformAppDeployDetailVo>> hostAppMap = deployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getHostIp));
        if(hostAppMap.size() > hostList.size())
        {
            logger.error(String.format("%s need at least %d hosts to clone %s, but only has %d",
                    platformId, hostAppMap.size(), clonedPlatformId, hostList.size()));
            throw new ParamException(String.format("%s need at least %d hosts to clone %s, but only has %d",
                    platformId, hostAppMap.size(), clonedPlatformId, hostList.size()));
        }
        Map<String, DomainUpdatePlanInfo> planMap = new HashMap<>();
        int i = 0;
        for(List<PlatformAppDeployDetailVo> hostAppList : hostAppMap.values())
        {
            String hostIp = hostList.get(i).getHostInnerIp();
            for(PlatformAppDeployDetailVo deployApp : hostAppList)
            {
                if(!planMap.containsKey(deployApp.getDomainId()))
                {
                    DomainUpdatePlanInfo planInfo = generateCloneExistDomain(clonedDomainMap.get(deployApp.getDomainId()),
                            deployApp.getSetId(), deployApp.getBkSetName());
                    planMap.put(deployApp.getDomainId(), planInfo);
                }
                AppUpdateOperationInfo opt = new AppUpdateOperationInfo();
                opt.setHostIp(hostIp);
                opt.setOperation(AppUpdateOperation.ADD);
                opt.setBasePath(deployApp.getBasePath());
                opt.setAppRunner(deployApp.getAppRunner());
                opt.setAppAlias(deployApp.getAppAlias());
                opt.setAppName(deployApp.getAppName());
                opt.setTargetVersion(deployApp.getVersion());
                List<AppFileNexusInfo> cfgs = new ArrayList<>();
                for(PlatformAppCfgFilePo cfg : deployApp.getCfgs())
                {
                    AppFileNexusInfo nexusInfo = new AppFileNexusInfo();
                    nexusInfo.setDeployPath(cfg.getDeployPath());
                    nexusInfo.setExt(cfg.getExt());
                    nexusInfo.setFileName(cfg.getFileName());
                    nexusInfo.setFileSize(0);
                    nexusInfo.setMd5(cfg.getMd5());
                    nexusInfo.setNexusAssetId(cfg.getNexusAssetId());
                    nexusInfo.setNexusPath(cfg.getFileNexusSavePath());
                    nexusInfo.setNexusRepository(cfg.getNexusRepository());
                    cfgs.add(nexusInfo);
                }
                opt.setCfgs(cfgs);
                planMap.get(deployApp.getDomainId()).getAppUpdateOperationList().add(opt);
            }
            i++;
        }
        PlatformUpdateSchemaInfo schema = new PlatformUpdateSchemaInfo(platformId, platformName, bkBizId, bkCloudId,
                clonedPlatform.getCcodVersion(), PlatformUpdateTaskType.CREATE, String.format("新建%s(%s)计划", platformName, platformId),
                String.format("create %s(%s) by clone %s(%s)", platformName, platformId, clonedPlatformId, clonedPlatform.getPlatformName()));
        schema.setDomainUpdatePlanList(new ArrayList<>(planMap.values()));
        List<BizSetDefine> setDefineList = paasService.queryCCODBizSet(false);
        makeupPlatformUpdateSchema(schema, new ArrayList<>(), new ArrayList<>(), setDefineList);
        platform = new PlatformPo(platformId, platformName, bkBizId, bkCloudId, CCODPlatformStatus.SCHEMA_CREATE_PLATFORM,
                clonedPlatform.getCcodVersion(), String.format("create %s(%s) by clone %s(%s)", platformName, platformId, clonedPlatformId, clonedPlatform.getPlatformName()));
//        platform.setCcodVersion(clonedPlatform.getCcodVersion());
//        platform.setStatus(CCODPlatformStatus.SCHEMA_CREATE_PLATFORM.id);
//        platform.setBkBizId(bkBizId);
//        platform.setPlatformName(platformName);
//        platform.setPlatformId(platformId);
//        platform.setComment(String.format("create %s(%s) by clone %s(%s)", platformName, platformId, clonedPlatformId, clonedPlatform.getPlatformName()));
//        Date now = new Date();
//        platform.setCreateTime(now);
//        platform.setBkCloudId(bkCloudId);
//        platform.setUpdateTime(now);
        platformMapper.insert(platform);
        PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
        schemaPo.setPlatformId(platformId);
        schemaPo.setContext(JSONObject.toJSONString(schema).getBytes());
        this.platformUpdateSchemaMapper.delete(platformId);
        this.platformUpdateSchemaMapper.insert(schemaPo);
        this.platformUpdateSchemaMap.put(platformId, schema);
        return schema;
    }

    @Override
    public PlatformUpdateSchemaInfo cloneExistDomain(String platformId, String clonedDomainId, String domainId, String domainName) throws ParamException, InterfaceCallException, LJPaasException {
        PlatformPo platform = platformMapper.selectByPrimaryKey(platformId);
        if(platform == null)
        {
            logger.error(String.format("id=%s platform has exist", platformId));
            throw new ParamException(String.format("id=%s platform has exist", platformId));
        }
        DomainPo clonedDomainPo = domainMapper.selectByPrimaryKey(platformId, clonedDomainId);
        if(clonedDomainPo == null)
        {
            logger.error(String.format("%s has not domain %s", platformId, clonedDomainId));
            throw new ParamException(String.format("%s has not domain %s", platformId, clonedDomainId));
        }
        if(this.platformUpdateSchemaMap.containsKey(platformId))
        {
            PlatformUpdateSchemaInfo existSchema = this.platformUpdateSchemaMap.get(platformId);
            for(DomainUpdatePlanInfo existPlan : existSchema.getDomainUpdatePlanList())
            {
                if(existPlan.getDomainId().equals(clonedDomainId))
                {
                    logger.error(String.format("domain %s of %s has update plan not execute", clonedDomainId, platformId));
                    throw new ParamException(String.format("domain %s of %s has update plan not execute", clonedDomainId, platformId));
                }
            }
        }
        DomainPo domainPo = domainMapper.selectByPrimaryKey(platformId, domainId);
        if(domainPo != null)
        {
            logger.error(String.format("domainId %s of %s not unique", domainId, platformId));
            throw new ParamException(String.format("domainId %s of %s not unique", domainId, platformId));
        }
        domainPo = domainMapper.selectByName(platformId, domainName);
        if(domainPo != null)
        {
            logger.error(String.format("domainName %s of %s not unique", domainName, platformId));
            throw new ParamException(String.format("domainName %s of %s not unique", domainName, platformId));
        }
        List<PlatformAppDeployDetailVo> deployApps = platformAppDeployDetailMapper.selectPlatformApps(platformId, clonedDomainId, null);
        if(deployApps == null || deployApps.size() == 0)
        {
            logger.error(String.format("%s has not deployed any app", clonedDomainId));
            throw new ParamException(String.format("%s has not deployed any app", clonedDomainId));
        }
        DomainUpdatePlanInfo planInfo = generateCloneExistDomain(clonedDomainPo,
                deployApps.get(0).getSetId(), deployApps.get(0).getBkSetName());
        for(PlatformAppDeployDetailVo deployApp : deployApps) {
            AppUpdateOperationInfo opt = new AppUpdateOperationInfo();
            opt.setHostIp(deployApp.getHostIp());
            opt.setOperation(AppUpdateOperation.ADD);
            opt.setBasePath(deployApp.getBasePath());
            opt.setAppRunner(deployApp.getAppRunner());
            opt.setAppAlias(deployApp.getAppAlias());
            opt.setAppName(deployApp.getAppName());
            opt.setTargetVersion(deployApp.getVersion());
            List<AppFileNexusInfo> cfgs = new ArrayList<>();
            for (PlatformAppCfgFilePo cfg : deployApp.getCfgs()) {
                AppFileNexusInfo nexusInfo = new AppFileNexusInfo();
                nexusInfo.setDeployPath(cfg.getDeployPath());
                nexusInfo.setExt(cfg.getExt());
                nexusInfo.setFileName(cfg.getFileName());
                nexusInfo.setFileSize(0);
                nexusInfo.setMd5(cfg.getMd5());
                nexusInfo.setNexusAssetId(cfg.getNexusAssetId());
                nexusInfo.setNexusPath(cfg.getFileNexusSavePath());
                nexusInfo.setNexusRepository(cfg.getNexusRepository());
                cfgs.add(nexusInfo);
            }
            opt.setCfgs(cfgs);
            planInfo.getAppUpdateOperationList().add(opt);
        }
        PlatformUpdateSchemaInfo schema;
        if(this.platformUpdateSchemaMap.containsKey(platformId))
        {
            schema = this.platformUpdateSchemaMap.get(platformId);
            schema.getDomainUpdatePlanList().add(planInfo);
        }
        else
        {
            schema = new PlatformUpdateSchemaInfo(platformId, platform.getPlatformName(), platform.getBkBizId(), platform.getBkCloudId(),
                    platform.getCcodVersion(), PlatformUpdateTaskType.CREATE, String.format("新建%s(%s)计划", platform.getPlatformName(), platformId),
                    String.format("create %s(%s) by clone %s(%s)", platform.getPlatformName(), domainName, domainId,
                            clonedDomainPo.getDomainName(), clonedDomainPo.getDomainId()));
            schema.getDomainUpdatePlanList().add(planInfo);
        }
        PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
        schemaPo.setPlatformId(platformId);
        schemaPo.setContext(JSONObject.toJSONString(schema).getBytes());
        this.platformUpdateSchemaMapper.delete(platformId);
        this.platformUpdateSchemaMapper.insert(schemaPo);
        this.platformUpdateSchemaMap.put(platformId, schema);
        return schema;
    }

    private DomainUpdatePlanInfo generateCloneExistDomain(DomainPo clonedDomain, String setId, String setName)
    {
        DomainUpdatePlanInfo planInfo = new DomainUpdatePlanInfo();
        planInfo.setUpdateType(DomainUpdateType.ADD);
        planInfo.setComment(String.format("clone from %s of %s", clonedDomain.getDomainName(), clonedDomain.getPlatformId()));
        planInfo.setDomainId(clonedDomain.getDomainId());
        planInfo.setDomainName(clonedDomain.getDomainName());
        Date now = new Date();
        planInfo.setCreateTime(now);
        planInfo.setUpdateTime(now);
        planInfo.setExecuteTime(now);
        planInfo.setBkSetName(setName);
        planInfo.setSetId(setId);
        planInfo.setStatus(UpdateStatus.CREATE);
        planInfo.setAppUpdateOperationList(new ArrayList<>());
        planInfo.setMaxOccurs(clonedDomain.getMaxOccurs());
        planInfo.setOccurs(clonedDomain.getOccurs());
        planInfo.setTags(clonedDomain.getTags());
        return planInfo;
    }

    @Override
    public void deletePlatformUpdateSchema(String platformId) throws ParamException {
        logger.debug(String.format("begin to delete platform update schema of %s", platformId));
        PlatformPo platformPo = platformMapper.selectByPrimaryKey(platformId);
        if(platformPo == null)
        {
            logger.error(String.format("%s platform not exist", platformId));
            throw new ParamException(String.format("%s platform not exist", platformId));
        }
        CCODPlatformStatus status = CCODPlatformStatus.getEnumById(platformPo.getStatus());
        if(status == null)
        {
            logger.error(String.format("%s status value %d is unknown", platformId, platformPo.getStatus()));
            throw new ParamException(String.format("%s status value %d is unknown", platformId, platformPo.getStatus()));
        }
        if(this.platformUpdateSchemaMap.containsKey(platformId))
        {
            logger.debug(String.format("remove schema of %s from memory", platformId));
            this.platformUpdateSchemaMap.remove(platformId);
        }
        logger.debug(String.format("delete schema of %s from database", platformId));
        this.platformUpdateSchemaMapper.delete(platformId);
        switch (status)
        {
            case SCHEMA_CREATE_PLATFORM:
                logger.debug(String.format("%s status is %s, so it should be deleted", platformId, status.name));
                platformMapper.delete(platformId);
                break;
            default:
                logger.debug(String.format("%s status is %s, so it should be updated to %s",
                        platformId, status.name, CCODPlatformStatus.RUNNING.name));
                platformPo.setStatus(CCODPlatformStatus.RUNNING.id);
                platformMapper.update(platformPo);
                break;
        }
    }

    @Override
    public void deletePlatform(String platformId) throws ParamException {
        logger.debug(String.format("begin to delete %s from cmdb", platformId));
        PlatformPo platformPo = platformMapper.selectByPrimaryKey(platformId);
        if(platformPo == null)
        {
            logger.error(String.format("%s not exist", platformId));
            throw new ParamException(String.format("%s not exist", platformId));
        }
        if(this.platformUpdateSchemaMap.containsKey(platformId))
        {
            this.platformUpdateSchemaMap.remove(platformId);
        }
        this.platformUpdateSchemaMapper.delete(platformId);
        List<PlatformAppPo> deployApps = platformAppMapper.select(platformId, null, null, null, null, null);
        this.platformAppBkModuleMapper.delete(null, null, platformId, null);
        for(PlatformAppPo deployApp : deployApps)
        {
            platformAppCfgFileMapper.delete(null, deployApp.getPlatformAppId());
        }
        this.platformAppMapper.delete(null, platformId, null);
        this.domainMapper.delete(null, platformId);
        this.platformMapper.delete(platformId);
        logger.info(String.format("%s delete success", platformId));
    }

    private void generatePlatformDeployParamAndScript(PlatformUpdateSchemaInfo schemaInfo) throws IOException, InterfaceCallException, NexusException
    {
        String platformId = schemaInfo.getPlatformId();
        Map<String, Object> params = new HashMap<>();
        params.put("Authorization", HttpRequestTools.getBasicAuthPropValue(this.nexusUserName, this.nexusPassword));
        params.put("app_repository", this.appRepository);
        params.put("image_repository", this.imageRepository);
        params.put("nexus_host_url", this.nexusHostUrl);
        params.put("nexus_user", this.nexusUserName);
        params.put("nexus_user_pwd", this.nexusPassword);
        params.put("cfg_repository", this.platformTmpCfgRepository);
        params.put("nexus_image_repository_url", this.nexusDockerUrl);
        params.put("cmdb_host_url", this.cmdbUrl);
        params.put("k8s_deploy_git_url", this.k8sDeployGitUrl);
        params.put("update_schema", schemaInfo);
        Resource resource = new ClassPathResource(this.platformDeployScriptFileName);
        InputStreamReader isr = new InputStreamReader(resource.getInputStream(), "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateStr = sf.format(now);
        String tmpSaveDir = String.format("%s/temp/deployScript/%s/%s", System.getProperty("user.dir"), platformId, dateStr);
        File saveDir = new File(tmpSaveDir);
        if(!saveDir.exists())
        {
            saveDir.mkdirs();
        }
        String savePath = String.format("%s/%s", tmpSaveDir, platformDeployScriptFileName);
        savePath = savePath.replaceAll("\\\\", "/");
        File scriptFile = new File(savePath);
        scriptFile.createNewFile();
        BufferedWriter out = new BufferedWriter(new FileWriter(scriptFile));
        String lineTxt = null;
        while ((lineTxt = br.readLine()) != null)
        {
            if("platform_deploy_params = \"\"\"\"\"\"".equals(lineTxt))
            {
                lineTxt = String.format("platform_deploy_params = %s", JSONObject.toJSONString(params));
            }
            out.write(lineTxt + "\n");
        }
        br.close();
        out.close();
        String md5 = DigestUtils.md5DigestAsHex(new FileInputStream(savePath));
        DeployFileInfo fileInfo = new DeployFileInfo();
        fileInfo.setExt(".py");
        fileInfo.setFileMd5(md5);
        fileInfo.setLocalSavePath(savePath);
        fileInfo.setFileName(this.platformDeployScriptFileName);
        String scriptNexusDirectory = String.format("%s/%s", platformId, dateStr);
        String scriptPath = String.format("%s/%s/%s", platformId, dateStr, this.platformDeployScriptFileName);
        nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, this.platformDeployScriptRepository, scriptNexusDirectory, new DeployFileInfo[]{fileInfo});
        schemaInfo.setDeployScriptRepository(this.platformDeployScriptRepository);
        schemaInfo.setDeployScriptPath(scriptPath);
        schemaInfo.setDeployScriptMd5(md5);
    }

    private List<AppUpdateOperationInfo> sortAppUpdateOperations(List<AppUpdateOperationInfo> optList, BizSetDefine setDefine) throws NotSupportAppException
    {
        List<AppUpdateOperationInfo> addOptList = new ArrayList<>();
        List<AppUpdateOperationInfo> deleteOptList = new ArrayList<>();
        for(AppUpdateOperationInfo opt : optList)
        {
            switch (opt.getOperation())
            {
                case ADD:
                case VERSION_UPDATE:
                case CFG_UPDATE:
                case START:
                    addOptList.add(opt);
                    break;
                default:
                    deleteOptList.add(opt);
            }
        }
        List<AppUpdateOperationInfo> sortedOptList = new ArrayList<>();
        if(deleteOptList.size() > 0)
        {
            Map<String, List<AppUpdateOperationInfo>> appDelMap = deleteOptList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
            for(int i= setDefine.getApps().length - 1; i >=0; i--)
            {
                String appName = setDefine.getApps()[i];
                if(appDelMap.containsKey(appName))
                {
                    final String standardAlias = setDefine.getAppAliasMap().get(appName);
                    List<AppUpdateOperationInfo> appOptList = appDelMap.get(appName);
                    Collections.sort(appOptList,new Comparator<AppUpdateOperationInfo>() {
                        @Override
                        public int compare(AppUpdateOperationInfo o1, AppUpdateOperationInfo o2) {
                            int index1 = 1;
                            if(!o1.getAppAlias().equals(standardAlias))
                            {
                                index1 = Integer.parseInt(o1.getAppAlias().replaceAll(standardAlias, ""));
                            }
                            int index2 = 1;
                            if(!o2.getAppAlias().equals(standardAlias))
                            {
                                index2 = Integer.parseInt(o2.getAppAlias().replaceAll(standardAlias, ""));
                            }
                            return index1 - index2;
                        }
                    });
                    sortedOptList.addAll(appOptList);
                    appDelMap.remove(appName);
                }
            }
            if(appDelMap.size() > 0)
            {
                logger.error(String.format("app %s is not supported by %s", JSONArray.toJSONString(appDelMap.keySet()), setDefine.getName()));
                throw new NotSupportAppException(String.format("app %s is not supported by %s", JSONArray.toJSONString(appDelMap.keySet()), setDefine.getName()));
            }
        }
        if(addOptList.size() > 0)
        {
            Map<String, List<AppUpdateOperationInfo>> appAddMap = addOptList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
            for(String appName : setDefine.getApps())
            {
                if(appAddMap.containsKey(appName))
                {
                    final String standardAlias = setDefine.getAppAliasMap().get(appName);
                    List<AppUpdateOperationInfo> appOptList = appAddMap.get(appName);
                    for(AppUpdateOperationInfo opt : appOptList)
                    {
                        opt.setAddDelay(setDefine.getAppAddDelayMap().get(appName));
                    }
                    Collections.sort(appOptList,new Comparator<AppUpdateOperationInfo>() {
                        @Override
                        public int compare(AppUpdateOperationInfo o1, AppUpdateOperationInfo o2) {
                            int index1 = 1;
                            if(!o1.getAppAlias().equals(standardAlias))
                            {
                                index1 = Integer.parseInt(o1.getAppAlias().replaceAll(standardAlias, ""));
                            }
                            int index2 = 1;
                            if(!o2.getAppAlias().equals(standardAlias))
                            {
                                index2 = Integer.parseInt(o2.getAppAlias().replaceAll(standardAlias, ""));
                            }
                            return index1 - index2;
                        }
                    });
                    sortedOptList.addAll(appOptList);
                    appAddMap.remove(appName);
                }
            }
            if(appAddMap.size() > 0)
            {
                logger.error(String.format("app %s is not supported by %s", JSONArray.toJSONString(appAddMap.keySet()), setDefine.getName()));
                throw new NotSupportAppException(String.format("app %s is not supported by %s", JSONArray.toJSONString(appAddMap.keySet()), setDefine.getName()));
            }
        }
        return sortedOptList;
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

    @Test
    public void jsonTest()
    {
        String json = "{\"status\":\"SUCCESS\",\"executeTime\":null,\"updateTime\":null,\"domainUpdatePlanList\":[{\"status\":\"CREATE\",\"executeTime\":null,\"updateTime\":null,\"domainId\":\"ops01\",\"domainName\":\"运营门户01\",\"updateType\":\"ADD\",\"comment\":\"new domain\",\"bkSetName\":\"运营门户\",\"setId\":\"supportPortal\",\"appUpdateOperationList\":[{\"originalVersion\":\"\",\"appName\":\"gls\",\"targetVersion\":\"10309\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"gls/gls/10309\",\"fileName\":\"Param-Config.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQ5ZjQ3NzA2NWIwOTA3ODdm\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./gls/WEB-INF/classes/\",\"md5\":\"0d4c565c8a683c7f33204f92de20e489\"}],\"appRunner\":\"gls\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"gls\"}],\"createTime\":null},{\"status\":\"CREATE\",\"executeTime\":null,\"updateTime\":null,\"domainId\":\"public01\",\"domainName\":\"公共组件01\",\"updateType\":\"ADD\",\"comment\":\"new domain\",\"bkSetName\":\"公共组件\",\"setId\":\"publicModules\",\"appUpdateOperationList\":[{\"originalVersion\":\"\",\"appName\":\"LicenseServer\",\"targetVersion\":\"5214\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"/LicenseServer/license/5214\",\"fileName\":\"Config.ini\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWM4NWZiNzUwN2U2Y2I5MTNl\",\"ext\":\"ini\",\"fileSize\":0,\"deployPath\":\"./bin/license/\",\"md5\":\"1797e46c56de0b00e11255d61d5630e8\"}],\"appRunner\":\"LicenseServer\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"LicenseServer\"},{\"originalVersion\":\"\",\"appName\":\"configserver\",\"targetVersion\":\"aca2af60caa0fb9f4af57f37f869dafc90472525\",\"basePath\":\"/home/cfs/Platform/\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"configserver/configserver/aca2af60caa0fb9f4af57f37f869dafc90472525\",\"fileName\":\"ccs_config.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWM5YmQ1ZDdmMGY4Y2U4M2M3\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"844cbcf66f9d16f7d376067831d67cfd\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"configserver/configserver/aca2af60caa0fb9f4af57f37f869dafc90472525\",\"fileName\":\"ccs_logger.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQyMTFmNGY1Y2E4OGY0ZGYx\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"197075eb110327da19bfc2a31f24b302\"}],\"appRunner\":\"configserver\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"configserver\"},{\"originalVersion\":\"\",\"appName\":\"glsServer\",\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"glsServer/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"fileName\":\"gls_config.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGRmYTZkNGEwOWE1YWUwNjQ0\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"fff65661bc6b88f7c21910146432044b\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"glsServer/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"fileName\":\"gls_logger.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjYwY2U1OGU2MGUyOWUxYjlh\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\"}],\"appRunner\":\"glsServer\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"glsServer\"}],\"createTime\":null},{\"status\":\"CREATE\",\"executeTime\":null,\"updateTime\":null,\"domainId\":\"manage01\",\"domainName\":\"管理门户01\",\"updateType\":\"ADD\",\"comment\":\"new domain\",\"bkSetName\":\"管理门户\",\"setId\":\"managerPortal\",\"appUpdateOperationList\":[{\"originalVersion\":\"\",\"appName\":\"customWebservice\",\"targetVersion\":\"19553\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"customWebservice/customWebservice/19553\",\"fileName\":\"web.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjZkZjVmZjhmOTMwODUzMGQ1\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./customWebservice/WEB-INF/classes/\",\"md5\":\"4708f827e04c5f785930696d7c81e23e\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"customWebservice/customWebservice/19553\",\"fileName\":\"config.properties\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWE4MzdlOWUyOWE2MmM2YjZl\",\"ext\":\"properties\",\"fileSize\":0,\"deployPath\":\"./customWebservice/WEB-INF/classes/\",\"md5\":\"74e822b75eb8a90e5c0b0f0eec00df38\"}],\"appRunner\":\"customWebservice\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"customWebservice\"},{\"originalVersion\":\"\",\"appName\":\"dcms\",\"targetVersion\":\"11110\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcms/dcms/11110\",\"fileName\":\"web.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWNmNDliMTA1NWZjM2E3NmRi\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./dcms/WEB-INF/\",\"md5\":\"748cbedd71488664433cb2bb59f7b3c7\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcms/dcms/11110\",\"fileName\":\"config.properties\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGRjZmE0YWQ2NDJlNTA3MTVj\",\"ext\":\"properties\",\"fileSize\":0,\"deployPath\":\"./dcms/WEB-INF/classes/\",\"md5\":\"039961b0aff865b1fb563a2823d28ae1\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcms/dcms/11110\",\"fileName\":\"Param-Config.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjZkNGUwNTUzZWI3YmExYmQ0\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./dcms/WEB-INF/classes/\",\"md5\":\"1d54648884d965951101abade31564fd\"}],\"appRunner\":\"dcms\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"dcms\"},{\"originalVersion\":\"\",\"appName\":\"dcmsRecord\",\"targetVersion\":\"21763\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcmsRecord/dcmsRecord/21763\",\"fileName\":\"web.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWExZjFmNmJmMjI1ZDdkMDZk\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./dcmsRecord/WEB-INF/\",\"md5\":\"5e292ede1aa89f7255848fc3eb0b98e9\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcmsRecord/dcmsRecord/21763\",\"fileName\":\"config.properties\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWNhMmI2MDgwODljMDM3Yzk3\",\"ext\":\"properties\",\"fileSize\":0,\"deployPath\":\"./dcmsRecord/WEB-INF/classes/\",\"md5\":\"4e2f8f01783d5a59ba2d665f3342630d\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcmsRecord/dcmsRecord/21763\",\"fileName\":\"applicationContext.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQ0YjUwZDNmNjIzMDk3NzMz\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./dcmsRecord/WEB-INF/classes/\",\"md5\":\"2167da546f02041f985e59bc7abb5b88\"}],\"appRunner\":\"dcmsRecord\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"dcmsRecord\"},{\"originalVersion\":\"\",\"appName\":\"dcmssg\",\"targetVersion\":\"20070\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcmssg/dcmsSG/20070\",\"fileName\":\"web.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGRiOTVjMTcyMjRjYmNjZTk0\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./dcmsSG/WEB-INF/\",\"md5\":\"c9c2d995e436f9e3ce20bea9f58675f3\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcmssg/dcmsSG/20070\",\"fileName\":\"config.properties\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjZmODEyOGZmYzlhYWFlYTIz\",\"ext\":\"properties\",\"fileSize\":0,\"deployPath\":\"./dcmsSG/WEB-INF/classes/\",\"md5\":\"06c1c1a72c35280b61e8c0005101aced\"}],\"appRunner\":\"dcmssg\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"dcmssg\"},{\"originalVersion\":\"\",\"appName\":\"dcmsStaticsReport\",\"targetVersion\":\"20537\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcmsStaticsReport/dcmsStatics/20537\",\"fileName\":\"web.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWE2MTVlNTE3NDdkMmU5YjAz\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./dcmsStatics/WEB-INF/\",\"md5\":\"1bd3d0faf77ef7e72ae3dc853eb2a9f5\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcmsStaticsReport/dcmsStatics/20537\",\"fileName\":\"config.properties\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWM1NzYyZWU3OGJiNjZlODhk\",\"ext\":\"properties\",\"fileSize\":0,\"deployPath\":\"./dcmsStatics/WEB-INF/classes/\",\"md5\":\"7adfc663082fa8a5a45792d9beda3f90\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcmsStaticsReport/dcmsStatics/20537\",\"fileName\":\"applicationContext.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGRhMTVkMGY1ZjVlYWM4NGJh\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./dcmsStatics/WEB-INF/classes/\",\"md5\":\"9e6f0f413ce17c98aa20c960cf3eae0c\"}],\"appRunner\":\"dcmsStatics\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"dcmsStatics\"},{\"originalVersion\":\"\",\"appName\":\"dcmsWebservice\",\"targetVersion\":\"20503\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcmsWebservice/dcmsWebservice/20503\",\"fileName\":\"web.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQyZTEyNzI2ZjFjM2U0OTgz\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./dcmsWebservice/WEB-INF/\",\"md5\":\"dae594913326ed68249ae37d8dae94d4\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcmsWebservice/dcmsWebservice/20503\",\"fileName\":\"config.properties\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjZiZmNlZTExZDI1ZWRmMjA0\",\"ext\":\"properties\",\"fileSize\":0,\"deployPath\":\"./dcmsWebservice/WEB-INF/classes/\",\"md5\":\"7beb9ba371f97d22dbd1fed55c10bc78\"}],\"appRunner\":\"dcmsWebservice\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"dcmsWebservice\"},{\"originalVersion\":\"\",\"appName\":\"dcmsx\",\"targetVersion\":\"master_8efabf4\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcmsx/dcmsx/master_8efabf4\",\"fileName\":\"web.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWNhZjVjYzdmMGI4ZjJkNTky\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./dcmsx/WEB-INF/\",\"md5\":\"6540a11bd5c91033c3adf062275154ca\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"dcmsx/dcmsx/master_8efabf4\",\"fileName\":\"application.properties\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGRkNDdhZjY5ZTg5NDAwYWI3\",\"ext\":\"properties\",\"fileSize\":0,\"deployPath\":\"./dcmsx/WEB-INF/classes/\",\"md5\":\"a16001e657e776c6d0a5d3076cfad13d\"}],\"appRunner\":\"dcmsx\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"dcmsx\"},{\"originalVersion\":\"\",\"appName\":\"safetyMonitor\",\"targetVersion\":\"20383\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"safetyMonitor/safetyMonitor/20383\",\"fileName\":\"web.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWE4ZGFkNjBlNDc2Mzc2N2Yy\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./safetyMonitor/WEB-INF/\",\"md5\":\"0129c9dab847d5fc0f50f437d66f06c2\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"safetyMonitor/safetyMonitor/20383\",\"fileName\":\"config.properties\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWM5ODc1MzAyZjBhODc5MzM4\",\"ext\":\"properties\",\"fileSize\":0,\"deployPath\":\"./safetyMonitor/WEB-INF/classes/\",\"md5\":\"b9f401a56d80cd92c2840c7965b9c3f6\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"safetyMonitor/safetyMonitor/20383\",\"fileName\":\"applicationContext.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGRjZmE0ZTIxNzkzNTllODky\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./safetyMonitor/WEB-INF/classes/\",\"md5\":\"493795bd1d8b35dde443e9dd732da30e\"}],\"appRunner\":\"safetyMonitor\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"safetyMonitor\"},{\"originalVersion\":\"\",\"appName\":\"dcmsStaticsReport\",\"targetVersion\":\"20528\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"nexusRepository\":\"some_test\",\"nexusPath\":\"dcmsStaticsReport/20528\",\"fileName\":\"web.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQ2MDM0ZTU3MTA1N2RlM2Qx\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./dcmsStaticsReport/WEB-INF/\",\"md5\":\"4673b1b939e9567a8e6a6a4ef6da4993\"},{\"nexusRepository\":\"some_test\",\"nexusPath\":\"dcmsStaticsReport/20528\",\"fileName\":\"config.properties\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjZlMTE1MTliMjc0MDE4NGE5\",\"ext\":\"properties\",\"fileSize\":0,\"deployPath\":\"./dcmsStaticsReport/WEB-INF/classes/\",\"md5\":\"34fb9d13c742306b2141f3a1bc79aaa2\"},{\"nexusRepository\":\"some_test\",\"nexusPath\":\"dcmsStaticsReport/20528\",\"fileName\":\"applicationContext.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWE0MjlkMWRiMGUzMDlkMTJh\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./dcmsStaticsReport/WEB-INF/classes/\",\"md5\":\"3d58aeb1b72748800e78c136b0232c4c\"}],\"appRunner\":\"dcmsStaticsReport\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"dcmsStaticsReport\"},{\"originalVersion\":\"\",\"appName\":\"cas\",\"targetVersion\":\"10973\",\"basePath\":\"/home/portal/tomcat/webapps/\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"cas/cas/10973\",\"fileName\":\"web.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWFjOTg5M2E5MzI0YjFlODRj\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./cas/WEB-INF/\",\"md5\":\"06c29dce651ed51e092276533559853a\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"cas/cas/10973\",\"fileName\":\"cas.properties\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWMwMTJlOWVmZGUwMTNjMjlk\",\"ext\":\"properties\",\"fileSize\":0,\"deployPath\":\"./cas/WEB-INF/\",\"md5\":\"c74190420467285db96a1e7a46a26573\"}],\"appRunner\":\"cas\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"cas\"}],\"createTime\":null},{\"status\":\"CREATE\",\"executeTime\":null,\"updateTime\":null,\"domainId\":\"cloud01\",\"domainName\":\"域服务01\",\"updateType\":\"ADD\",\"comment\":\"new domain\",\"bkSetName\":\"域服务\",\"setId\":\"domainService\",\"appUpdateOperationList\":[{\"originalVersion\":\"\",\"appName\":\"UCDServer\",\"targetVersion\":\"deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"UCDServer/ucds/deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\",\"fileName\":\"DRWRClient.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWM3NWY1YmY5Yjg1OTgwYTY0\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"1954c2c1f488406f383cdf5a235ab868\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"UCDServer/ucds/deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\",\"fileName\":\"ucds_logger.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWEzMjM2NmQ2OTRkZDRjNjAy\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"ec57329ddcec302e0cc90bdbb8232a3c\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"UCDServer/ucds/deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\",\"fileName\":\"ucds_config.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjY0MWFhYmE4NGQ0NDZkNjFk\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"c78bfdf874c8c8a1ae6c55ac2e952306\"}],\"appRunner\":\"ucds\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"ucds\"},{\"originalVersion\":\"\",\"appName\":\"dcs\",\"targetVersion\":\"155:21974\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"/dcs/dcs/155:21974\",\"fileName\":\"DCServer.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWNlZGU3MjU2ZTc2N2QxYjY3\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"63d5267c83a84a236f7e9e6f10ab8720\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"/dcs/dcs/155:21974\",\"fileName\":\"dc_log4cpp.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQwY2M1Mzc0MTM4ZGVkZmYz\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"138877a50a0f85a397ddbcf6be62095b\"}],\"appRunner\":\"dcs\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"dcs\"},{\"originalVersion\":\"\",\"appName\":\"cmsserver\",\"targetVersion\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"basePath\":\"/home/ccodrunner/Platform/\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"cmsserver/cms/4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"fileName\":\"config.cms2\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWEzMGRmM2FkMDUyZmI0MmNl\",\"ext\":\"cms2\",\"fileSize\":0,\"deployPath\":\"./etc/\",\"md5\":\"ebc8435fcea1515c2d73eaa8b46ccf39\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"cmsserver/cms/4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"fileName\":\"beijing.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWFiNzEyOTMxNjdlMmQwMTIw\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./etc/\",\"md5\":\"4168695ceba63dd24d53d46fa65cffb1\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"cmsserver/cms/4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"fileName\":\"cms_log4cpp.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWMyMmU0NzE3OGU4MDU1NWZi\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./etc/\",\"md5\":\"b16210d40a7ef123eef0296393df37b8\"}],\"appRunner\":\"cms1\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"cms1\"},{\"originalVersion\":\"\",\"appName\":\"cmsserver\",\"targetVersion\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"basePath\":\"/home/ccodrunner/Platform/\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"cmsserver/cms/4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"fileName\":\"config.cms2\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWEzMGRmM2FkMDUyZmI0MmNl\",\"ext\":\"cms2\",\"fileSize\":0,\"deployPath\":\"./etc/\",\"md5\":\"ebc8435fcea1515c2d73eaa8b46ccf39\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"cmsserver/cms/4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"fileName\":\"beijing.xml\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWFiNzEyOTMxNjdlMmQwMTIw\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./etc/\",\"md5\":\"4168695ceba63dd24d53d46fa65cffb1\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"cmsserver/cms/4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"fileName\":\"cms_log4cpp.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWMyMmU0NzE3OGU4MDU1NWZi\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./etc/\",\"md5\":\"b16210d40a7ef123eef0296393df37b8\"}],\"appRunner\":\"cms2\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"cms2\"},{\"originalVersion\":\"\",\"appName\":\"ucxserver\",\"targetVersion\":\"1fef2157ea07c483979b424c758192bd709e6c2a\",\"basePath\":\"/home/ccodrunner/Platform/\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"ucxserver/ucx/1fef2157ea07c483979b424c758192bd709e6c2a\",\"fileName\":\"config.ucx\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWNhYTUxY2IxOGM3ZGI5ZTRh\",\"ext\":\"ucx\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"6c2aca996f3e1e6fad277cafffd1ebf7\"}],\"appRunner\":\"ucx\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"ucx\"},{\"originalVersion\":\"\",\"appName\":\"DDSServer\",\"targetVersion\":\"150:18722\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"/DDSServer/dds/150:18722\",\"fileName\":\"dds_config.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWEzZGUyZjhjYmUzZWE1N2Uw\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"38e4194d03e10f5ce7fbf364fc5678b9\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"/DDSServer/dds/150:18722\",\"fileName\":\"dds_logger.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWNmMDQwZTBiMWM4NTBlMmVh\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"fe3c70d26b3827d44473b06f46af0970\"}],\"appRunner\":\"dds\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"dds\"},{\"originalVersion\":\"\",\"appName\":\"StatSchedule\",\"targetVersion\":\"154:21104\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"/StatSchedule/ss/154:21104\",\"fileName\":\"ss_config.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQxZGY1ZjgzYzJlZGI5ZGU5\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"9c3476beac9ee275fa06a91497f58cd7\"}],\"appRunner\":\"ss\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"ss\"},{\"originalVersion\":\"\",\"appName\":\"dcproxy\",\"targetVersion\":\"195:21857\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"/dcproxy/dcproxy/195:21857\",\"fileName\":\"dcp_config.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQ2ZDM1YmRiYjljOTI0OWY5\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"3fd2a067221bbc974cd05997fe46fe6b\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"/dcproxy/dcproxy/195:21857\",\"fileName\":\"dcp_logger.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjY0YTg1MDkwMThkMmJhYmQ0\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"08dbf42e8c02425e3a11b9cef38a9a7c\"}],\"appRunner\":\"dcproxy\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"dcproxy\"},{\"originalVersion\":\"\",\"appName\":\"daengine\",\"targetVersion\":\"179:20744\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"/daengine/daengine/179:20744\",\"fileName\":\"dae.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjBhYjgwYTc0MzkyMWU0MjZmNTc5Yjg3ZDQwNmIyOGRi\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"b52196dbae7fa53481ec937dacfa7e2a\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"/daengine/daengine/179:20744\",\"fileName\":\"dae_config.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjg1MTM1NjUyYTk3OGJlOWE2NTc0YWZhMGZiMzNhN2Y2\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"04544c8572c42b176d501461168dacf4\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"/daengine/daengine/179:20744\",\"fileName\":\"dae_log4cpp.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOmQ0ODExNzU0MWRjYjg5ZWM3YjVjMjVjYzgxODhhOTcw\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"0d5b6405de9af28401f7d494888eed8f\"},{\"nexusRepository\":\"ccod_modules\",\"nexusPath\":\"/daengine/daengine/179:20744\",\"fileName\":\"dae_logger.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVzOjEzYjI5ZTQ0OWYwZTNiOGQzMjg1NjAwMzEzNTI4ODE4\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"ac2fde58b18a5ab1ee66d911982a326c\"}],\"appRunner\":\"daengine\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"appAlias\":\"daengine\"}],\"createTime\":null}],\"title\":\"pahjgsrqhcs平台使用模板完成规划\",\"platformId\":\"pahjgsrqhcs\",\"bkCloudId\":0,\"comment\":\"pahjgsrqhcs平台使用模板完成规划\",\"bkBizId\":29,\"deadline\":null,\"ccodVersion\":\"ccod4.1\",\"taskType\":\"CREATE\",\"platformName\":\"平安环境公司容器化测试\",\"createTime\":null}";
        PlatformUpdateSchemaInfo schemaInfo = JSONObject.parseObject(json, PlatformUpdateSchemaInfo.class);
        if(schemaInfo != null)
        {
            System.out.println("ok");
        }
    }

    @Test
    public void indexTest()
    {
        try
        {
            String standId = "tr2";
            List<String> usedIds = new ArrayList<>();
//            usedIds.add("tr207");
//            usedIds.add("tr203");
            for(int i = 0; i < 1; i++)
            {
                String domainId = autoGenerateAlias(standId, usedIds, true);
                System.out.println(domainId);
                usedIds.add(domainId);
            }
            usedIds.add(standId + "225");
            String domainId = autoGenerateAlias(standId, usedIds, true);
            System.out.println(domainId);
            usedIds.add(domainId);

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Test
    public void sqlTest()
    {
        DatabaseType dbType = DatabaseType.ORACLE;
        String areaId = "010";
        String domainId = "cloud01";
        String alias = "cms1";
        try
        {
            String insertSql = ServiceUnitUtils.getDefaultCMSServerInsertSql(dbType, areaId, domainId, alias);
            System.out.println(String.format("insertSql=%s", insertSql));
            alias = "ucds";
            String dbName = "ucds";
            String dbUser = "ucds";
            String dbPwd = "ucds";
            String ucdsIp = "10.130.41.218";
            int ucdsPort = 32194;
            String ucdsDataKeeperIp = "10.130.29.72";
            String ucdsInnerIp = "10.130.29.75";
            insertSql = ServiceUnitUtils.getDefaultUCDSServerInsertSql(dbType, areaId, domainId, alias, dbName, dbUser,
                    dbPwd, ucdsIp, ucdsPort, ucdsDataKeeperIp, ucdsInnerIp);
            System.out.println(String.format("insertSql=%s", insertSql));
            alias = "dcs";
            insertSql = ServiceUnitUtils.getDefaultDCSServerInsertSql(dbType, areaId, domainId, alias);
            System.out.println(String.format("insertSql=%s", insertSql));
            alias = "dds";
            insertSql = ServiceUnitUtils.getDefaultDDSServerInsertSql(dbType, areaId, domainId, alias);
            System.out.println(String.format("insertSql=%s", insertSql));

            alias = "daengine";
            insertSql = ServiceUnitUtils.getDefaultDAEInsertSql(dbType, areaId, domainId, alias);
            System.out.println(String.format("insertSql=%s", insertSql));

            alias = "ss";
            insertSql = ServiceUnitUtils.getDefaultSSInsertSql(dbType, areaId, domainId, alias);
            System.out.println(String.format("insertSql=%s", insertSql));

            alias = "ucds";
            ucdsPort = 33333;
            String updateSql = ServiceUnitUtils.getDefaultUCDSServerUpdateSql(dbType, domainId, alias, ucdsPort);
            System.out.println(String.format("updateSql=%s", updateSql));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
