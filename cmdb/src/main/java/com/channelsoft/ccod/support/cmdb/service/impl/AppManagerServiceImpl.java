package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.config.*;
import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.dao.*;
import com.channelsoft.ccod.support.cmdb.exception.*;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import com.channelsoft.ccod.support.cmdb.service.ILJPaasService;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.channelsoft.ccod.support.cmdb.service.IPlatformAppCollectService;
import com.channelsoft.ccod.support.cmdb.utils.HttpRequestTools;
import com.channelsoft.ccod.support.cmdb.vo.*;
import com.google.gson.*;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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

    private final static Gson gson = new GsonBuilder().registerTypeAdapter(DateTime.class, new GsonDateUtil()).create();

    @Value("${nexus.platform-app-cfg-repository}")
    private String platformAppCfgRepository;

    @Value("${nexus.app-module-repository}")
    private String appRepository;

    @Value("${nexus.unconfirmed-platform-app-repository}")
    private String unconfirmedPlatformAppRepository;

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

    @Value("${nexus.nexus-docker-url}")
    private String nexusDockerUrl;

    @Value("${nexus.image-repository}")
    private String imageRepository;

    @Autowired
    IPlatformAppCollectService platformAppCollectService;

    @Autowired
    INexusService nexusService;

    @Autowired
    AppMapper appMapper;

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
    UnconfirmedAppModuleMapper unconfirmedAppModuleMapper;

    @Autowired
    PlatformThreePartAppMapper platformThreePartAppMapper;

    @Autowired
    PlatformThreePartServiceMapper platformThreePartServiceMapper;

    @Autowired
    AssembleMapper assembleMapper;

    @Autowired
    ILJPaasService paasService;

    @Autowired
    private CCODBiz ccodBiz;

    @Autowired
    private ImageCfg imageCfg;

    @Value("${ccod.health-check-at-regex}")
    private String healthCheckRegex;

    @Value("${ccod.service-port-regex}")
    private String portRegex;

    private Map<String, List<BizSetDefine>> appSetRelationMap;

    private Map<String, BizSetDefine> setDefineMap;

    private Set<String> notCheckCfgAppSet;

    private boolean isPlatformCheckOngoing = false;

    private final static Logger logger = LoggerFactory.getLogger(AppManagerServiceImpl.class);

    protected final ReentrantReadWriteLock appReadLock = new ReentrantReadWriteLock();

    protected final ReentrantReadWriteLock appWriteLock = new ReentrantReadWriteLock();

    protected final Map<String, List<AppModuleVo>> registerAppMap = new HashMap<>();

    @PostConstruct
    void init() throws  Exception
    {
        this.appSetRelationMap  = new HashMap<>();
        for(BizSetDefine setDefine : this.ccodBiz.getSet())
        {
            for(AppDefine appDefine : setDefine.getApps())
            {
                if(!this.appSetRelationMap.containsKey(appDefine.getName()))
                    this.appSetRelationMap.put(appDefine.getName(), new ArrayList<>());
                this.appSetRelationMap.get(appDefine.getName()).add(setDefine);
            }
        }
        this.notCheckCfgAppSet = new HashSet<>(ccodBiz.getNotCheckCfgApps());
        this.setDefineMap = this.ccodBiz.getSet().stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        flushRegisteredApp();
        try
        {
//            appUpdate();
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }


    private void appUpdate(){
//        this.registerAppMap.values().stream().flatMap(s->s.stream()).filter(a->a.getStartCmd().equals("./")).forEach(a->{
//            a.setStartCmd(String.format("./%s", a.getInstallPackage().getFileName()));
//            appMapper.update(a.getApp());
//        });
        String regex = ".*\\.war\\.war\\s*$";
        this.registerAppMap.values().stream().flatMap(s->s.stream())
                .filter(a->a.getInstallPackage().getFileName().matches(regex) || a.getInstallPackage().getNexusPath().matches(regex)).forEach(a->{
                    a.getInstallPackage().setFileName(a.getInstallPackage().getFileName().replaceAll("\\.war\\.war\\s*$", "\\.war"));
                    a.getInstallPackage().setNexusPath(a.getInstallPackage().getNexusPath().replaceAll("\\.war\\.war\\s*$", "\\.war"));
                    a.setUpdateTime(new Date());
                    appMapper.update(a.getApp());
                    try{
                        nexusService.deleteAsset(nexusHostUrl, nexusUserName, nexusPassword, a.getInstallPackage().getNexusAssetId());
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                    }
        });
    }

    @Autowired
    public void flushRegisteredApp()
    {
        Map<String, List<AppModuleVo>> map = this.appModuleMapper.select(null, null, null, null)
                .stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        this.registerAppMap.clear();
        for(String appName : map.keySet())
            this.registerAppMap.put(appName, map.get(appName));

    }

    private String getTempSaveDir(String directory) {
        String saveDir = String.format("%s/downloads/%s", System.getProperty("user.dir"), directory);
        return saveDir;
    }

    @Override
    public List<AppModuleVo> queryAllRegisterAppModule(Boolean hasImage) {
        this.appReadLock.readLock().lock();
        try
        {
            List<AppModuleVo> registerList = this.registerAppMap.values().stream().flatMap(listContainer -> listContainer.stream()).collect(Collectors.toList());
            List<AppModuleVo> retList = registerList;
            if(hasImage != null)
                retList = registerList.stream().collect(Collectors.groupingBy(AppModuleVo::isHasImage)).containsKey(hasImage) ? registerList.stream().collect(Collectors.groupingBy(AppModuleVo::isHasImage)).get(hasImage) : new ArrayList<>();
            logger.debug(String.format("find %d register app module with hasImage=%s", retList.size(), hasImage));
            return retList;
        }
        finally {
            this.appReadLock.readLock().unlock();
        }
    }

    @Override
    public Map<String, List<BizSetDefine>> getAppSetRelation() {
        return this.appSetRelationMap;
    }

    @Override
    public List<AppModuleVo> queryApps(String appName, Boolean hasImage) throws DataAccessException {
        this.appReadLock.readLock().lock();
        try
        {
            logger.debug(String.format("begin to query app modules : appName=%s", appName));
            List<AppModuleVo> list = this.registerAppMap.containsKey(appName) ? this.registerAppMap.get(appName) : new ArrayList<>();;
            if(hasImage != null)
                list = list.stream().collect(Collectors.groupingBy(AppModuleVo::isHasImage)).containsKey(hasImage) ? list.stream().collect(Collectors.groupingBy(AppModuleVo::isHasImage)).get(hasImage) : new ArrayList<>();
            logger.info(String.format("query %d app module record with appName=%s and hasImage=%s", list.size(), appName, hasImage));
            return list;
        }
        finally {
            this.appReadLock.readLock().unlock();
        }
    }

    @Override
    public AppModuleVo queryAppByVersion(String appName, String version, Boolean hasImage) throws ParamException, DataAccessException {
        logger.debug(String.format("begin to query appName=%s and version=%s app module record", appName, version));
        this.appReadLock.readLock().lock();
        try
        {
            if(!this.registerAppMap.containsKey(appName))
            {
                logger.error(String.format("not find registered app with name=%s", appName));
                throw new ParamException(String.format("not find registered app with name=%s", appName));
            }
            Map<String, AppModuleVo> versionMap = this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
            if(!versionMap.containsKey(version))
            {
                logger.error(String.format("%s has not version %s", appName, version));
                throw new ParamException(String.format("%s has not version %s", appName, version));
            }
            AppModuleVo moduleVo = versionMap.get(version);
            if(hasImage != null && moduleVo.isHasImage() != hasImage)
                throw new ParamException(String.format("%s(%s) not match hasImage=%s", appName, version, hasImage));
            return moduleVo;
        }
        finally {
            this.appReadLock.readLock().unlock();
        }
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
    public void preprocessPlatformAppModule(PlatformAppModuleVo module) throws DataAccessException, InterfaceCallException, NexusException, ParamException
    {
        logger.debug(String.format("begin to handle module[%s]", gson.toJson(module)));
        AppPo appPo = module.getAppInfo();
        String appName = appPo.getAppName();
        String appVersion = appPo.getVersion();
        String appDirectory = appPo.getAppNexusDirectory();
        if(!this.registerAppMap.containsKey(appName) || !this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(appVersion))
        {
            String group = appPo.getAppNexusGroup();
            logger.warn(String.format("%s[%s] not exit register it first", appName, appVersion));
            List<NexusAssetInfo> appFileAssetList = this.nexusService.queryGroupAssets(this.nexusHostUrl, this.nexusUserName, nexusPassword, this.appRepository, group);
            if(appFileAssetList.size() > 0)
            {
                logger.info(String.format("%s[%s] is has not registered at cmdb, but %s not empty, clear fisrt",
                        appName, appVersion, group));
                for(NexusAssetInfo assetInfo : appFileAssetList)
                {
                    nexusService.deleteAsset(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, assetInfo.getId());
                }
                appFileAssetList = this.nexusService.queryGroupAssets(this.nexusHostUrl, this.nexusUserName, nexusPassword, this.appRepository, group);
                if(appFileAssetList.size() != 0)
                {
                    logger.error(String.format("delete assets at %s/%s/%s fail", this.nexusHostUrl, this.appRepository, appPo.getAppNexusDirectory()));
                    throw new NexusException(String.format("delete assets at %s/%s/%s fail", this.nexusHostUrl, this.appRepository, appPo.getAppNexusDirectory()));
                }
                logger.debug(String.format("delete assets at %s/%s/%s success", this.nexusHostUrl, this.appRepository, appPo.getAppNexusDirectory()));
            }
            logger.debug(String.format("upload %s(%s) package and cfgs to nexus", appName, appVersion));
            addAppToNexus(appName, appVersion, module.getInstallPackage(), module.getCfgs(), this.appRepository, appDirectory);
            logger.debug(String.format("add %s(%s) relative info to db", appName, appVersion));
            AppModuleVo newModule = addNewAppToDB(appPo, module.getInstallPackage(), module.getCfgs());
            this.appReadLock.writeLock();
            try
            {
                if(!this.registerAppMap.containsKey(appName))
                    this.registerAppMap.put(appName, new ArrayList<>());
                logger.debug(String.format("update register app module info"));
                this.registerAppMap.get(appName).add(newModule);
            }
            finally {
                this.appReadLock.writeLock().unlock();
            }
        }
        else
            logger.debug("%s[%s] has registered", appName, appVersion);
        AppModuleVo moduleVo = this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(appVersion);
        logger.debug(String.format("check package and cfgs of %s(%s)", appName, appVersion));
        checkInstPkgAndCfg(moduleVo, module.getInstallPackage(), module.getCfgs());
        PlatformAppPo platformApp = module.getPlatformApp();
        String platformCfgDirectory = platformApp.getPlatformAppDirectory(appName, appVersion, platformApp);
        logger.debug(String.format("update cfgs to %s/%s", this.platformAppCfgRepository, platformCfgDirectory));
        nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword,
                this.platformAppCfgRepository, platformCfgDirectory, module.getCfgs());
    }

    @Override
    public List<PlatformAppModuleVo> preprocessCollectedPlatformAppModule(String platformName, String platformId, List<PlatformAppModuleVo> moduleList, List<PlatformAppModuleVo> failList) {
        List<PlatformAppModuleVo> successList = new ArrayList<>();
        logger.debug(String.format("begin to preprocess %d collected platform app modules", moduleList.size()));
        this.appWriteLock.writeLock().lock();
        try
        {
            for(PlatformAppModuleVo moduleVo : moduleList)
            {
                if(!moduleVo.isOk(platformId, platformName, this.appSetRelationMap))
                {
                    failList.add(moduleVo);
                    continue;
                }
                List<AppModuleVo> registerAppList = this.queryAllRegisterAppModule(null);
                try
                {
                    handlePlatformAppModule(moduleVo, registerAppList);
                    successList.add(moduleVo);
                }
                catch (ParamException ex)
                {
                    moduleVo.setComment(ex.getMessage());
                    failList.add(moduleVo);
                }
                catch (Exception ex)
                {
                    moduleVo.setComment(ex.getMessage());
                    failList.add(moduleVo);
                }
            }
            this.appReadLock.writeLock().lock();
            try
            {
                this.flushRegisteredApp();
            }
            finally {
                this.appReadLock.writeLock().unlock();
            }
        }
        finally {
            this.appWriteLock.writeLock().unlock();
        }
        logger.debug(String.format("preprocess %d collected platform apps and %d is Ok", moduleList.size(), successList.size()));
        return successList;
    }

    /**
     * 处理客户端收集的平台应用信息
     * 如果该模块在db中没有记录则需要上传二进制安装包以及配置文件并在数据库创建一条记录
     * 归档平台应用的配置文件，并在数据库创建一条平台应用详情记录
     * @param module 客户端收集的平台应用信息
     * @param registerAppList 已经注册的应用模块列表
     * @return 添加后的模块
     * @throws DataAccessException 查询数据库异常
     * @throws InterfaceCallException 调用nexus接口异常
     * @throws NexusException nexus返回调用失败信息或是解析nexus调用结果失败
     * @throws DBNexusNotConsistentException cmdb记录的信息和nexus不一致
     */
    private boolean handlePlatformAppModule(PlatformAppModuleVo module, List<AppModuleVo> registerAppList)
            throws DataAccessException, InterfaceCallException, NexusException, ParamException
    {
        logger.debug(String.format("begin to handle module[%s]", gson.toJson(module)));
        Map<String, List<AppModuleVo>> appMap = registerAppList.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        AppPo appPo = module.getAppInfo();
        String appName = appPo.getAppName();
        String appVersion = appPo.getVersion();
        String appDirectory = appPo.getAppNexusDirectory();
        AppModuleVo moduleVo;
        if(!appMap.containsKey(appName) || !appMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(appVersion))
        {
            String group = appPo.getAppNexusGroup();
            logger.warn(String.format("%s[%s] not exit register it first", appName, appVersion));
            List<NexusAssetInfo> appFileAssetList = this.nexusService.queryGroupAssets(this.nexusHostUrl, this.nexusUserName, nexusPassword, this.appRepository, group);
            if(appFileAssetList.size() > 0)
            {
                logger.info(String.format("%s[%s] is has not registered at cmdb, but %s not empty, clear fisrt",
                        appName, appVersion, group));
                for(NexusAssetInfo assetInfo : appFileAssetList)
                {
                    nexusService.deleteAsset(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, assetInfo.getId());
                }
                appFileAssetList = this.nexusService.queryGroupAssets(this.nexusHostUrl, this.nexusUserName, nexusPassword, this.appRepository, group);
                if(appFileAssetList.size() != 0)
                {
                    logger.error(String.format("delete assets at %s/%s/%s fail", this.nexusHostUrl, this.appRepository, appPo.getAppNexusDirectory()));
                    throw new NexusException(String.format("delete assets at %s/%s/%s fail", this.nexusHostUrl, this.appRepository, appPo.getAppNexusDirectory()));
                }
                logger.debug(String.format("delete assets at %s/%s/%s success", this.nexusHostUrl, this.appRepository, appPo.getAppNexusDirectory()));
            }
            logger.debug(String.format("upload %s(%s) package and cfgs to nexus", appName, appVersion));
            addAppToNexus(appName, appVersion, module.getInstallPackage(), module.getCfgs(), this.appRepository, appDirectory);
            logger.debug(String.format("add %s(%s) relative info to db", appName, appVersion));
            moduleVo = addNewAppToDB(appPo, module.getInstallPackage(), module.getCfgs());
            registerAppList.add(moduleVo);
        }
        else
            moduleVo = this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(appVersion);
        logger.debug(String.format("check package and cfgs of %s(%s)", appName, appVersion));
        checkInstPkgAndCfg(moduleVo, module.getInstallPackage(), module.getCfgs());
        return true;
    }

    private void checkInstPkgAndCfg(AppModuleVo moduleVo, DeployFileInfo installPackage, DeployFileInfo[] cfgs) throws ParamException
    {
        String appName = moduleVo.getAppName();
        String appVersion = moduleVo.getVersion();
        AppType appType = moduleVo.getAppType();
        if(!installPackage.getFileName().equals(moduleVo.getInstallPackage().getFileName()))
        {
            logger.error(String.format("%s[%s] install package file name is %s not %s",
                    appName, appVersion, moduleVo.getInstallPackage().getFileName(), installPackage.getFileName()));
            throw new ParamException(String.format("%s[%s] install package file name is %s not %s",
                    appName, appVersion, moduleVo.getInstallPackage().getFileName(), installPackage.getFileName()));
        }
        else if(!installPackage.getFileMd5().equals(moduleVo.getInstallPackage().getMd5()) && !AppType.RESIN_WEB_APP.equals(appType))
        {
            logger.error(String.format("%s[%s] install package file md5 is %s not %s",
                    appName, appVersion, moduleVo.getInstallPackage().getMd5(), installPackage.getFileMd5()));
            throw new ParamException(String.format("%s[%s] install package file md5 is %s not %s",
                    appName, appVersion, moduleVo.getInstallPackage().getMd5(), installPackage.getFileMd5()));
        }
        if(!this.appSetRelationMap.containsKey(appName) && !AppType.RESIN_WEB_APP.equals(appType)) {
            Map<String, DeployFileInfo> reportCfgMap = Arrays.stream(cfgs).collect(Collectors.toMap(DeployFileInfo::getFileName, Function.identity()));
            Map<String, AppFileNexusInfo> wantCfgMap = moduleVo.getCfgs().stream().collect(Collectors.toMap(AppFileNexusInfo::getFileName, Function.identity()));
            if (reportCfgMap.size() != wantCfgMap.size()) {
                logger.error(String.format("%s[%s] want %d cfgs but report %d cfgs",
                        appName, appVersion, wantCfgMap.size(), reportCfgMap.size()));
                throw new ParamException(String.format("%s[%s] want %d cfgs but report %d cfgs",
                        appName, appVersion, wantCfgMap.size(), reportCfgMap.size()));
            }
            for (String fileName : wantCfgMap.keySet()) {
                if (!reportCfgMap.containsKey(fileName)) {
                    logger.error(String.format("%s[%s] want %s but not reported",
                            appName, appVersion, fileName));
                    throw new ParamException(String.format("%s[%s] want %s but not reported",
                            appName, appVersion, fileName));
                }
            }
        }
    }

    /**
     * 将应用信息，程序包以及配置文件相关信息入库
     * @param app 应用信息
     * @param installPackage 程序包
     * @param cfgs 配置文件
     * @return 添加后的应用模块信息
     * @throws DataAccessException
     */
    private AppModuleVo addNewAppToDB(AppPo app, DeployFileInfo installPackage, DeployFileInfo[] cfgs) throws DataAccessException
    {
        if(debug)
        {
            if(app.getAppType() == null)
                app.setAppType(AppType.BINARY_FILE);
            if(StringUtils.isBlank(app.getCcodVersion()))
                app.setCcodVersion("4.5");
        }
        this.appMapper.insert(app);
        return this.appModuleMapper.selectByNameAndVersion(app.getAppName(), app.getVersion());
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


    @Override
    public AppModuleVo addNewAppModule(AppPo appPo, DeployFileInfo installPackage, DeployFileInfo[] cfgs) throws InterfaceCallException, NexusException {
        String appName = appPo.getAppName();
        String version = appPo.getVersion();
        logger.debug(String.format("add new app module %s(%s)[%s] to cmdb", appName, version, gson.toJson(appPo)));
        String directory = appPo.getAppNexusDirectory();
        addAppToNexus(appName, version, installPackage, cfgs, this.appRepository, directory);
        AppModuleVo moduleVo = addNewAppToDB(appPo, installPackage, cfgs);
        logger.info(String.format("%s[%s] add success", appName, version));
        return moduleVo;
    }

    @Override
    public void createNewPlatformAppDataCollectTask(String platformId, String platformName, int bkBizId, int bkCloudId) throws Exception {
        logger.info(String.format("begin to create %s(%s) with bkBizId=%d and bkCloudId=%d platform app collect task",
                platformName, platformId, bkBizId, bkCloudId));
        PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
        if(platformPo != null)
        {
            logger.error(String.format("create platform data collect task fail  : %s has exist", platformId));
            throw new ParamException(String.format("create platform data collect task fail  : %s has exist", platformId));
        }
        LJBizInfo bizInfo = this.paasService.queryBizInfoById(bkBizId);
        if(bizInfo == null)
        {
            logger.error(String.format("id=%d biz not exist", bkBizId));
            throw new ParamException(String.format("id=%d biz not exist", bkBizId));
        }
        if(!platformName.equals(bizInfo.getBkBizName()))
        {
            logger.error(String.format("name of bkBizId=%d is %s not %s", bkBizId, bizInfo.getBkBizName(), platformName));
            throw new ParamException(String.format("name of bkBizId=%d is %s not %s", bkBizId, bizInfo.getBkBizName(), platformName));
        }
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
//                    startCollectPlatformAppData(platformId, platformName, bkBizId, bkCloudId);
                }
                catch (Exception ex)
                {
                    logger.error(String.format("collect %s(%s) task exception",
                            platformName, platformId), ex);
                }
            }
        });
        executor.execute(taskThread);
        executor.shutdown();
    }

    @Override
    public boolean hasImage(String appName, String version) throws ParamException {
        logger.debug(String.format("check image of %s[%s] exist", appName, version));
        this.appReadLock.readLock().lock();
        try
        {
            if(!this.registerAppMap.containsKey(appName)
                    || !this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version))
            {
                logger.error(String.format("%s[%s] not register", appName, version));
                throw new ParamException(String.format("%s[%s] not register", appName, version));
            }
        }
        finally {
            this.appReadLock.readLock().unlock();
        }
        return imageExist(appName, version);
    }

    @Override
    public Comparator<AppBase> getAppSort(BizSetDefine setDefine) {
        Map<String, Integer> appSortMap = new HashMap<>();
        for(int i = 0; i < setDefine.getApps().size(); i++)
            appSortMap.put(setDefine.getApps().get(i).getName(), i);
        Map<String, String> aliasMap = setDefine.getApps().stream().collect(Collectors.toMap(o->o.getName(), v->v.getAlias()));
        Comparator<AppBase> sort = new Comparator<AppBase>() {
            @Override
            public int compare(AppBase o1, AppBase o2) {
                if(!o1.getAppName().equals(o2.getAppName()))
                    return appSortMap.get(o1.getAppName()) - appSortMap.get(o2.getAppName());
                return getIndexFromAlias(o1.getAlias(), aliasMap.get(o1.getAppName())) - getIndexFromAlias(o2.getAlias(), aliasMap.get(o2.getAppName()));
            }
        };
        return sort;
    }

    private int getIndexFromAlias(String alias, String standAlias)
    {
        String indexStr = alias.replace(standAlias, "");
        int index = StringUtils.isBlank(indexStr) ? 0 : Integer.parseInt(indexStr);
        return index;
    }

    private boolean imageExist(String appName, String version)
    {
        boolean imageExist = false;
        String url = String.format("http://%s/v2/%s/%s/tags/list", this.nexusDockerUrl, this.imageRepository, appName.toLowerCase());
        try
        {
            String queryResult = HttpRequestTools.httpGetRequest(url, this.nexusUserName, this.nexusPassword);
            JsonParser jp = new JsonParser();
            JsonObject jo = jp.parse(queryResult).getAsJsonObject();
//            String tags = JSONObject.parseObject(queryResult).getString("tags");
            JsonArray je = jo.get("tags").getAsJsonArray();
            for(JsonElement e : je){
                if(version.replaceAll("\\:", "-").equals(e.getAsString())){
                    imageExist = true;
                    break;
                }
            }
        }
        catch (Exception ex)
        {
            logger.error(String.format("query %s fail", ex));
        }
        logger.info(String.format("%s[%s] image exist : %b", appName, version, imageExist));
        return imageExist;
    }

    @Override
    public String checkAppBaseProperties(AppBase appBase, AppUpdateOperation operation)
    {
        String appName = appBase.getAppName();
        if(StringUtils.isBlank(appName))
            return String.format("appName of %s is blank;", operation.name);
        boolean needVersion = false;
        boolean needAlias = false;
        boolean notRegister = false;
        boolean needProp = false;
        boolean checkProp = false;
        switch (operation)
        {
            case ADD:
                needVersion = true;
                checkProp = true;
                needProp = true;
                break;
            case MODIFY_REGISTER:
                needVersion = true;
                checkProp = true;
                break;
            case UPDATE:
            case DEBUG:
                needAlias = true;
                checkProp = true;
                break;
            case REGISTER:
                needVersion = true;
                notRegister = true;
                needProp = true;
                checkProp = true;
                break;
            case DELETE:
                needAlias = true;
        }
        this.appReadLock.readLock().lock();
        String tag = String.format("%s %s", operation.name, appName);
        try {
            String alias = appBase.getAlias();
            if(needAlias && StringUtils.isBlank(alias))
                return String.format("alias of %s is blank", tag);
            String version = appBase.getVersion();
            if(needVersion && StringUtils.isBlank(version))
                return String.format("version of %s is blank", tag);
            List<AppModuleVo> registers = this.registerAppMap.containsKey(appName) ? this.registerAppMap.get(appName) : new ArrayList<>();
            Map<String, AppModuleVo> versionMap = registers.stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
            if(notRegister && versionMap.containsKey(version))
                return String.format("version %s of %s has registered", version, tag);
            else if(notRegister && appBase.getInstallPackage() == null)
                return String.format("installPackage of %s is empty;", tag);
            else if(!notRegister && !this.registerAppMap.containsKey(appName))
                return String.format("app %s of %s has not registered", appName, tag);
            else if(!notRegister && StringUtils.isNotBlank(version) && !versionMap.containsKey(version))
                return String.format("version %s of %s has not registered", version, tag);
            if(checkProp){
                if(StringUtils.isNotBlank(appBase.getPorts()) && !appBase.getPorts().matches(this.portRegex))
                    return String.format("%s is illegal port for %s", appBase.getPorts(), tag);
                if(StringUtils.isNotBlank(appBase.getNodePorts()) && !appBase.getNodePorts().matches(this.portRegex))
                    return String.format("%s is illegal nodePort for %s", appBase.getPorts(), tag);
                if(StringUtils.isNotBlank(appBase.getCheckAt()) && !appBase.getCheckAt().matches(this.healthCheckRegex))
                    return String.format("%s is illegal checkAt for %s", appBase.getCheckAt(), tag);
                if(appBase.getInitialDelaySeconds() != null && appBase.getInitialDelaySeconds() <= 0)
                    return String.format("%d is illegal initialDelaySeconds for %s", appBase.getInitialDelaySeconds(), tag);
                if(appBase.getPeriodSeconds() != null && appBase.getPeriodSeconds() <= 0)
                    return String.format("%d is illegal periodSeconds for %s", appBase.getPeriodSeconds(), tag);
                if(appBase.getAppType() != null && appBase.getAppType().equals(AppType.JAR) && appBase.getBasePath() != null && appBase.getBasePath().equals("/root"))
                    return String.format("basePath can not be /root for %s", tag);
            }
            if(needProp) {
                StringBuffer sb = new StringBuffer();
                if(!notRegister && StringUtils.isNotBlank(version)){
                    AppModuleVo module = versionMap.get(version);
                    if(appBase.getAppType() != null && !appBase.getAppType().equals(module.getAppType()))
                        return String.format("type of %s is %s not %s", module.getAppType().name, appBase.getAppType().name);
                    else if(StringUtils.isNotBlank(appBase.getCcodVersion()) && !appBase.getCcodVersion().equals(module.getCcodVersion()))
                        return String.format("ccodVersion of %s[%s] is %s not %s", appName, version, module.getCcodVersion(), appBase.getCcodVersion());
                    appBase.setAppType(module.getAppType());
                    appBase.setCcodVersion(module.getCcodVersion());
                }
                if(StringUtils.isBlank(appBase.getBasePath()))
                    sb.append(String.format("basePath of %s is blank;", tag));
                if(StringUtils.isBlank(appBase.getDeployPath()))
                    sb.append(String.format("deployPath of %s is blank;", tag));
                if(StringUtils.isBlank(appBase.getStartCmd()))
                    sb.append(String.format("startCmd is blank for %s;", tag));
                if(StringUtils.isBlank(appBase.getLogOutputCmd()))
                    sb.append(String.format("logOutputCmd for %s is blank;", tag));
                if(StringUtils.isBlank(appBase.getPorts()))
                    sb.append(String.format("ports is blank for %s;", tag));
                if(appBase.getCfgs() == null || appBase.getCfgs().size() == 0)
                    sb.append(String.format("cfgs of %s is empty;", tag));
                if(appBase.getInitialDelaySeconds() == null)
                    sb.append(String.format("initialDelaySeconds can not be null for %s", tag));
                if(appBase.getPeriodSeconds() == null)
                    sb.append(String.format("periodSeconds can not be null for %s", tag));
                return sb.toString();
            }
            return "";
        }
        finally {
            this.appReadLock.readLock().unlock();
        }
    }

    void updateRegisterAppModuleImageExist() throws Exception
    {
        this.appWriteLock.writeLock().lock();
        try
        {
            List<AppPo> appList = this.appMapper.select(null, null);
            for(AppPo appPo : appList)
            {
                boolean hasImage = this.hasImage(appPo.getAppName(), appPo.getVersion());
                appPo.setHasImage(hasImage);
                this.appMapper.update(appPo);
            }
            this.appReadLock.writeLock().lock();
            try
            {
                flushRegisteredApp();
            }
            finally {
                this.appReadLock.writeLock().unlock();
            }
        }
        finally {
            this.appWriteLock.writeLock().unlock();
        }
    }

    @Override
    public List<AppModuleVo> queryAllHasImageAppModule() {
        logger.debug(String.format("query all register app module which has image"));
        this.appReadLock.readLock().lock();
        try
        {
            List<AppModuleVo> registerList = this.registerAppMap.values().stream().flatMap(listContainer -> listContainer.stream()).collect(Collectors.toList());
            List<AppModuleVo> list = registerList.stream().collect(Collectors.groupingBy(AppModuleVo::isHasImage)).containsKey(true) ? registerList.stream().collect(Collectors.groupingBy(AppModuleVo::isHasImage)).get(true) : new ArrayList<>();
            logger.debug(String.format("%d register app has image", list.size()));
            return list;
        }
        finally {
            this.appReadLock.readLock().unlock();
        }
    }

    @Override
    public void registerNewAppModule(AppModuleVo appModule) throws ParamException, InterfaceCallException, NexusException, IOException {
        logger.debug(String.format("begin to register app=[%s] into cmdb", gson.toJson(appModule)));
        String checkResult = this.checkAppBaseProperties(appModule, AppUpdateOperation.REGISTER);
        Assert.isTrue(StringUtils.isBlank(checkResult), checkResult);
        String appName = appModule.getAppName();
        String version = appModule.getVersion();
        if(appModule.isHasImage() != null && appModule.isHasImage()) {
            Assert.isTrue(imageExist(appName, version), String.format("not find image for %s version %s at nexus", appName, version));
        }
        this.appWriteLock.writeLock().lock();
        try
        {
            AppPo appPo = getAppFromModule(appModule, true);
            logger.debug(String.format("insert app info [%s]", gson.toJson(appPo)));
            this.appMapper.insert(appPo);
            AppModuleVo newModule = this.appModuleMapper.selectByNameAndVersion(appName, version);
            this.appReadLock.writeLock().lock();
            try {
                if(!this.registerAppMap.containsKey(appName))
                    this.registerAppMap.put(appName, new ArrayList<>());
                this.registerAppMap.get(appName).add(newModule);
            }
            finally {
                this.appReadLock.writeLock().unlock();
            }
        }
        finally {
            this.appWriteLock.writeLock().unlock();
        }
    }

    @Override
    public void registerCIAppModule(AppModuleVo appModule) throws ParamException, InterfaceCallException, NexusException, IOException {
        logger.debug(String.format("register appMode[%s] from ci", gson.toJson(appModule)));
        Assert.isTrue(StringUtils.isNotBlank(appModule.getAppName()), String.format("appName can not be empty"));
        String appName = appModule.getAppName();
        Assert.notNull(appModule.getAppType(), String.format(String.format("appType of %s is null", appName)));
        AppType appType = appModule.getAppType();
        Assert.isTrue(StringUtils.isNotBlank(appModule.getVersion()), String.format("version of %s can not be empty", appName));
        String version = appModule.getVersion();
        Assert.isTrue(StringUtils.isNotBlank(appModule.getCcodVersion()), String.format("ccodVersion of %s(%s) can not be null", appName, version));
        String ccodVersion = appModule.getCcodVersion();
        Assert.notNull(appModule.getInstallPackage(), String.format("installPackage of %s(%s) can not be null", appName, version));
        Assert.notNull(appModule.getVersionControl(), String.format("versionControl of %s(%s) can not be null", appName,  version));
        Assert.isTrue(StringUtils.isNotBlank(appModule.getVersionControlUrl()), String.format("versionControlUrl of %s(%s) can not be empty", appName, version));
        this.appReadLock.writeLock().lock();
        AppModuleVo chosen = null;
        boolean isMatch = true;
        try{
            Map<AppType, List<AppModuleVo>> typeMap = registerAppMap.containsKey(appName) ? registerAppMap.get(appName).stream().filter(a->StringUtils.isBlank(checkAppBaseProperties(a, AppUpdateOperation.ADD))).collect(Collectors.groupingBy(AppModuleVo::getAppType)) : new HashMap<>();
            List<AppModuleVo> modules = typeMap.get(appType);
            if(modules != null){
                AppModuleVo registered = modules.stream().collect(Collectors.toMap(a->a.getVersion(), v->v)).get(version);
                if(registered != null){
                    if(!ccodVersion.equals(registered.getCcodVersion())){
                        throw new ParamException(String.format("%s(%s) for ccod %s has been register", appName, version, registered.getCcodVersion()));
                    }
                    logger.warn(String.format("%s(%s) has registered", appName, version));
                    registered.setInstallPackage(appModule.getInstallPackage());
                    registered.setVersionControl(appModule.getVersionControl());
                    registered.setVersionControlUrl(appModule.getVersionControlUrl());
                    registered.setHasImage(true);
                    registered.setUpdateTime(new Date());
                    appModule = registered;
                }
                else{
                    List<AppModuleVo> ms = modules.stream().filter(a->a.getCcodVersion().equals(ccodVersion))
                            .sorted(Comparator.comparing(AppModuleVo::getCreateTime).reversed()).collect(Collectors.toList());
                    if(ms.size() > 0){
                        chosen = ms.get(0);
                    }
                    else{
                        chosen = modules.get(modules.size() - 1);
                    }
                }
            }
            else{
                modules = registerAppMap.values().stream().flatMap(v->v.stream()).filter(a->a.getAppType().equals(appType) && StringUtils.isBlank(checkAppBaseProperties(a, AppUpdateOperation.ADD))).sorted(Comparator.comparing(AppModuleVo::getCreateTime).reversed()).collect(Collectors.toList());
                chosen = modules.get(modules.size() - 1);
                isMatch = false;
            }
        }
        finally {
            this.appReadLock.writeLock().unlock();
        }
        appModule.setHasImage(true);
        if(chosen == null){
            this.updateAppModule(appModule);
        }
        else{
            appModule.fill(chosen);
            if(!isMatch && appType.equals(AppType.BINARY_FILE)) {
                appModule.setStartCmd(String.format("./%s", appModule.getInstallPackage().getFileName()));
            }
            registerNewAppModule(appModule);
        }
        logger.info(String.format("register app module %s(%s) from ci success", appName, version));
    }

    @Override
    public void registerCIAppModule(String appName, AppType appType, String version, String ccodVersion, String gitUrl, String repository, String path) throws ParamException, InterfaceCallException, NexusException, IOException {
        logger.debug(String.format("register app module(appName=%s, version=%s, ccodVersion=%s, gitUrl=%s, repository=%s, path=%s) from ci",
                appName, version, ccodVersion, gitUrl, repository, path));
        Assert.isTrue(StringUtils.isNotBlank(appName), "appName is blank");
        Assert.notNull(appType, "appType is null");
        Assert.isTrue(StringUtils.isNotBlank(version), "version is blank");
        Assert.isTrue(StringUtils.isNotBlank(ccodVersion), "ccodVersion is blank");
        Assert.isTrue(StringUtils.isNotBlank(gitUrl), "gitUrl is blank");
        Assert.isTrue(StringUtils.isNotBlank(repository), "repository is blank");
        Assert.isTrue(StringUtils.isNotBlank(path), "path is blank");
        String group = String.format("/%s", path.replaceAll("/[^/]+$", ""));
        Map<String, NexusAssetInfo> assetMap = nexusService.queryGroupAssets(nexusHostUrl, nexusUserName, nexusPassword, repository, group)
                .stream().collect(Collectors.toMap(a->a.getPath(), v->v));
        if(!assetMap.containsKey(path)){
            throw new ParamException(String.format("%s at repository %s not exist", path, repository));
        }
        AppModuleVo module = new AppModuleVo();
        module.setAppName(appName);
        module.setVersion(version);
        module.setCcodVersion(ccodVersion);
        module.setVersionControl(VersionControl.GIT);
        module.setVersionControlUrl(gitUrl);
        module.setAppType(appType);
        String deployPath;
        String basePath;
        switch (appType){
            case BINARY_FILE:
                basePath = "/root/Platform";
                deployPath = "./bin";
                break;
            case TOMCAT_WEB_APP:
                basePath = "/usr/local/tomcat";
                deployPath = "./webapps";
                break;
            case RESIN_WEB_APP:
                basePath = "/root/resin";
                deployPath = "./webapps";
                break;
            case NODEJS:
                basePath = String.format("/usr/share/nginx/html/%s", assetMap.get(path).getNexusAssetFileName().split("\\.")[0]);
                deployPath = "./";
                break;
            case JAR:
                basePath = "/root/Platform";
                deployPath = "./";
                break;
            default:
                throw new ParamException(String.format("not support appType %s", appType.name));
        }
        module.setBasePath(basePath);
        module.setInstallPackage(new AppFileNexusInfo(assetMap.get(path), deployPath));
        registerCIAppModule(module);
        logger.info(String.format("app from ci register success"));
    }

    @Override
    public void updateAppModule(AppModuleVo appModule) throws ParamException, InterfaceCallException, NexusException, IOException {
        logger.debug(String.format("begin to modify cfg of app=[%s] in cmdb", gson.toJson(appModule)));
        String appName = appModule.getAppName();
        String version = appModule.getVersion();
        String checkResult = checkAppBaseProperties(appModule, AppUpdateOperation.MODIFY_REGISTER);
        Assert.isTrue(StringUtils.isBlank(checkResult), checkResult);
        if(appModule.isHasImage()) {
            Assert.isTrue(imageExist(appName, version), String.format("not find image for %s version %s at nexus", appName, version));
        }
        this.appWriteLock.writeLock().lock();
        try
        {
            AppPo appPo = getAppFromModule(appModule, false);
            logger.debug(String.format("update app to %s", gson.toJson(appPo)));
            this.appMapper.update(appPo);
            logger.debug(String.format("flush register app map"));
            AppModuleVo newModule = this.appModuleMapper.selectByNameAndVersion(appName, version);
            this.appReadLock.writeLock().lock();
            try
            {
                Map<String, AppModuleVo> versionMap = this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
                versionMap.put(version, newModule);
                this.registerAppMap.put(appName, new ArrayList<>(versionMap.values()));
            }
            finally {
                this.appReadLock.writeLock().unlock();
            }
        }
        finally {
            this.appWriteLock.writeLock().unlock();
        }

    }


    private AppPo getAppFromModule(AppModuleVo appModule, boolean isNew) throws ParamException, InterfaceCallException, NexusException, IOException
    {
        String directory = appModule.getAppNexusDirectory();
        List<AppFileNexusInfo> srcFiles = new ArrayList<>();
        if(appModule.getCfgs() != null && appModule.getCfgs().size() > 0)
            srcFiles.addAll(appModule.getCfgs());
        if(appModule.getInstallPackage() != null)
            srcFiles.add(appModule.getInstallPackage());
        if(srcFiles.size() > 0) {
            Map<String, AppFileNexusInfo> fileMap = this.nexusService.downloadAndUploadAppFiles(this.nexusHostUrl, this.nexusUserName,
                    this.nexusPassword, srcFiles, this.nexusHostUrl, this.nexusUserName, this.nexusPassword, this.appRepository,
                    directory, true).stream().collect(Collectors.toMap(AppFileNexusInfo::getFileName, Function.identity()));
            if(appModule.getInstallPackage() != null){
                appModule.setInstallPackage(fileMap.get(appModule.getInstallPackage().getFileName()));
                fileMap.remove(appModule.getInstallPackage().getFileName());
            }
            if(appModule.getCfgs() != null && appModule.getCfgs().size() > 0){
                appModule.setCfgs(new ArrayList<>(fileMap.values()));
            }
        }
        AppPo appPo;
        Date now = new Date();
        if(!isNew) {
            AppModuleVo registered = this.registerAppMap.get(appModule.getAppName()).stream()
                    .collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(appModule.getVersion());
            registered.update(appModule);
            appPo = registered.getApp();
        }
        else {
            appPo = appModule.getApp();
            appPo.setCreateTime(now);
        }
        appPo.setUpdateTime(now);
        return appPo;
    }

    /**
     * 从源nexus下载应用相关文件并上传到指定的nexus仓库里
     * @param srcNexusHostUrl 源nexus的url地址
     * @param srcNexusUser 源nexus的登录用户
     * @param srcPwd 源nexus
     * @param srcFileList 需要下载并上传文件列表
     * @param dstRepository 上传的目的仓库
     * @param dstDirectory 上传路径
     * @return
     */
    @Override
    public List<AppFileNexusInfo> downloadAndUploadAppFiles(String srcNexusHostUrl, String srcNexusUser, String srcPwd, List<AppFileNexusInfo> srcFileList, String dstRepository, String dstDirectory) throws ParamException, InterfaceCallException, NexusException, IOException
    {
        List<DeployFileInfo> fileList = new ArrayList<>();
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String tmpSaveDir = getTempSaveDir(DigestUtils.md5DigestAsHex(String.format("%s;%s", dstDirectory, sf.format(now)).getBytes()));
        for(AppFileNexusInfo filePo : srcFileList)
        {
            String downloadUrl = filePo.getFileNexusDownloadUrl(srcNexusHostUrl);
            logger.debug(String.format("download cfg from %s", downloadUrl));
            String savePth = nexusService.downloadFile(srcNexusUser, srcPwd, downloadUrl, tmpSaveDir, filePo.getFileName());
            FileInputStream is = new FileInputStream(savePth);
            String md5 = DigestUtils.md5DigestAsHex(is);
            is.close();
            if(!md5.equals(filePo.getMd5()))
            {
                logger.error(String.format("file %s verify md5 FAIL : report=%s and download=%s",
                        filePo.getFileName(), filePo.getMd5(), md5));
                throw new ParamException(String.format("file %s verify md5 FAIL : report=%s and download=%s",
                        filePo.getFileName(), filePo.getMd5(), md5));
            }
            fileList.add(new DeployFileInfo(filePo, savePth));
        }
        Map<String, AppFileNexusInfo> srcMap = srcFileList.stream().collect(Collectors.toMap(AppFileNexusInfo::getFileName, Function.identity()));
        List<NexusAssetInfo> assets = this.nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName,
                this.nexusPassword, dstRepository, dstDirectory, fileList.toArray(new DeployFileInfo[0]));
        return assets.stream().map(a->new AppFileNexusInfo(a, srcMap.get(a.getNexusAssetFileName()).getDeployPath()))
                .collect(Collectors.toList());
    }

    /**
     * 自动生成域id
     * @param standardDomainId 标准域id
     * @param usedId 已经使用过的id
     * @return 自动生成域id
     * @throws ParamException
     */
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
        if(usedAlias.size() == 0)
        {
            if(standardAlias.equals("ucgateway"))
            {
                return "ucgateway0";
            }
            else
            {
                return onlyOne ? standardAlias : standardAlias + "1";
            }
        }

        String regex = String.format("^%s\\d*$", standardAlias);
        Pattern pattern = Pattern.compile(regex);
        int index = 0;
        for(String alias : usedAlias)
        {
            Matcher matcher = pattern.matcher(alias);
            if(!matcher.find())
            {
                logger.error(String.format("%s is an illegal alias for %s", alias, standardAlias));
                throw new ParamException(String.format("%s is an illegal alias for %s", alias, standardAlias));
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
            else if(index == 0 && !standardAlias.equals("ucgateway"))
            {
                index = 1;
            }

        }
        index++;
        String appAlias = String.format("%s%s", standardAlias, index);
        return appAlias;
    }

    @Override
    public String getAppCfgText(String appName, String version, String cfgFileName) throws ParamException, NexusException, InterfaceCallException , IOException{
        logger.debug("begin to get %s text of %s[%s]", cfgFileName, appName, version);
        AppModuleVo moduleVo = queryAppByVersion(appName, version, null);
        if(moduleVo == null)
        {
            logger.error(String.format("%s[%] not exist", appName, version));
            throw new ParamException(String.format("%s[%] not exist", appName, version));
        }
        Map<String, AppFileNexusInfo> cfgMap = moduleVo.getCfgs().stream()
                .collect(Collectors.toMap(AppFileNexusInfo::getFileName, Function.identity()));
        if(!cfgMap.containsKey(cfgFileName))
        {
            logger.error(String.format("%s[%s] has not %s cfg", appName, version, cfgFileName));
            throw new ParamException(String.format("%s[%s] has not %s cfg", appName, version, cfgFileName));
        }
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String dateStr = sf.format(now);
        String tmpSaveDir = String.format("%s/temp/cfgs/%s/%s", System.getProperty("user.dir"), appName, dateStr);
        File saveDir = new File(tmpSaveDir);
        if(!saveDir.exists())
        {
            saveDir.mkdirs();
        }
        AppFileNexusInfo cfgFilePo = cfgMap.get(cfgFileName);
        String downloadUrl = cfgFilePo.getFileNexusDownloadUrl(this.nexusHostUrl);
        String savePath = this.nexusService.downloadFile(this.nexusUserName, this.nexusPassword, downloadUrl, tmpSaveDir, cfgFileName);
        savePath = savePath.replaceAll("\\\\", "/");
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(savePath)),
                "UTF-8"));
        String lineTxt = null;
        String context = "";
        while ((lineTxt = br.readLine()) != null)
        {
            context += lineTxt + "\n";
        }
        br.close();
        return context;
    }

    @Override
    public List<BizSetDefine> queryCCODBizSet(boolean isCheckApp) {
        if(!isCheckApp)
            return this.ccodBiz.getSet();
        this.appReadLock.readLock().lock();
        try
        {
            List<BizSetDefine> defineList = new ArrayList<>();
            for(BizSetDefine setDefine : this.ccodBiz.getSet())
            {
                BizSetDefine define = new BizSetDefine();
                define.setApps(new ArrayList<>());
                define.setFixedDomainId(setDefine.getFixedDomainId());
                define.setFixedDomainName(setDefine.getFixedDomainName());
                define.setId(setDefine.getId());
                define.setIsBasic(setDefine.getIsBasic());
                define.setName(setDefine.getName());
                for(AppDefine appDefine : setDefine.getApps())
                {
                    if(this.registerAppMap.containsKey(appDefine.getName()))
                        define.getApps().add(appDefine);
                }
                defineList.add(define);
            }
            return defineList;
        }
        finally {
            this.appReadLock.readLock().unlock();
        }
    }

    @Override
    public List<BizSetDefine> queryCCODBizSetWithImage(String ccodVersion, Boolean hasImage) {
        this.appReadLock.readLock().lock();
        try
        {
            List<AppModuleVo> registerApps = this.registerAppMap.values().stream().flatMap(apps -> apps.stream()).collect(Collectors.toList());
            List<AppModuleVo> imageList = registerApps;
            if(hasImage != null) {
                Map<Boolean, List<AppModuleVo>> map = registerApps.stream().collect(Collectors.groupingBy(AppModuleVo::isHasImage));
                imageList = map.containsKey(hasImage) ? map.get(hasImage) : new ArrayList<>();
            }
            Map<String, List<AppModuleVo>> imageMap = imageList.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
            List<AppModuleVo> versionList = registerApps;
            if(StringUtils.isNotBlank(ccodVersion)) {
                Map<String, List<AppModuleVo>> map = registerApps.stream().collect(Collectors.groupingBy(AppModuleVo::getCcodVersion));
                versionList = map.containsKey(ccodVersion) ? map.get(ccodVersion) : new ArrayList<>();
            }
            Map<String, List<AppModuleVo>> ccodVersionMap = versionList.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
            List<BizSetDefine> defineList = new ArrayList<>();
            for(BizSetDefine setDefine : this.ccodBiz.getSet())
            {
                BizSetDefine define = new BizSetDefine();
                define.setApps(new ArrayList<>());
                define.setFixedDomainId(setDefine.getFixedDomainId());
                define.setFixedDomainName(setDefine.getFixedDomainName());
                define.setId(setDefine.getId());
                define.setIsBasic(setDefine.getIsBasic());
                define.setName(setDefine.getName());
                for(AppDefine appDefine : setDefine.getApps())
                {
                    if(imageMap.containsKey(appDefine.getName()) && ccodVersionMap.containsKey(appDefine.getName()))
                        define.getApps().add(appDefine);
                }
                defineList.add(define);
            }
            return defineList;
        }
        finally {
            this.appReadLock.readLock().unlock();
        }
    }

    /**
     * 为域新加的应用自动生成应用别名
     * @param domainPo 添加应用的域
     * @param addOptList 该域所有被添加的应用
     * @param deployAppList 该域已经部署的应用列表
     * @param clone 该域是否是否是通过clone方式获得
     */
    private void generateAlias4DomainApps(DomainPo domainPo, List<AppUpdateOperationInfo> addOptList, List<PlatformAppDeployDetailVo> deployAppList, boolean clone) throws ParamException
    {
        Map<String, List<AppUpdateOperationInfo>> addAppMap = addOptList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getAppName));
        for(String appName : addAppMap.keySet())
        {
            List<String> usedAlias = new ArrayList<>();
            String standAlias = this.setDefineMap.get(domainPo.getBizSetName()).getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity())).get(appName).getAlias();
            if(domainAppMap.containsKey(appName))
            {
                usedAlias = new ArrayList<>(domainAppMap.get(appName).stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAlias, Function.identity())).keySet());
            }
            List<AppUpdateOperationInfo> needAliasList = new ArrayList<>();
            for(AppUpdateOperationInfo optInfo : addAppMap.get(appName))
            {
                if(StringUtils.isBlank(optInfo.getAlias()) || !clone)
                    needAliasList.add(optInfo);
                else
                    usedAlias.add(optInfo.getAlias());
            }
            boolean onlyOne = usedAlias.size() == 0 && needAliasList.size() == 1 ? true : false;
            for(AppUpdateOperationInfo optInfo : needAliasList)
            {
                String alias = autoGenerateAlias(standAlias, usedAlias, onlyOne);
                optInfo.setAlias(alias);
                usedAlias.add(alias);
            }
        }
        for(AppUpdateOperationInfo optInfo : addOptList)
        {
            if(StringUtils.isBlank(optInfo.getOriginalAlias()))
                optInfo.setOriginalAlias(optInfo.getAlias());
        }
    }

    @Override
    public AppType getAppTypeFromImageUrl(String imageUrl) throws ParamException, NotSupportAppException {
        String[] arr = imageUrl.split("\\-");
        if(arr.length != 3)
            throw new ParamException(String.format("%s is illegal imageUrl", imageUrl));
        String repository = arr[1];
        arr = arr[2].split("\\:");
        if(arr.length != 2)
            throw new ParamException(String.format("%s is illegal image tag", arr[2]));
        String appName = arr[0];
        String version = arr[1];
        Set<String> ccodRepSet = new HashSet<>(imageCfg.getCcodModuleRepository());
        Set<String> threeAppRepSet = new HashSet<>(imageCfg.getThreeAppRepository());
        AppType appType = null;
        if(ccodRepSet.contains(repository))
        {
            for(String name : this.registerAppMap.keySet())
            {
                if(name.equals(appName))
                {
                    if(this.registerAppMap.get(name).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version))
                    {
                        appType = registerAppMap.get(name).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version).getAppType();
                        break;
                    }
                    else
                        throw new ParamException(String.format("%s[%s] not register", name, version));
                }
            }
            if(appType == null)
                throw new NotSupportAppException(String.format("ccod module %s not supported", appName));
        }
        else if(threeAppRepSet.contains(repository))
            appType = AppType.THREE_PART_APP;
        else
            appType = AppType.OTHER;
        logger.debug(String.format("type of image %s is %s", imageUrl, appType.name));
        return appType;
    }

    @Override
    public boolean isRegisteredCCODAppImage(String imageUrl) {
        logger.debug(String.format("check image %s is ccod app", imageUrl));
        String[] arr = imageUrl.replaceAll(".*/", "").split(":");
        if(arr.length != 2)
            return false;
        this.appReadLock.readLock().lock();
        try{
            for(String appName : this.registerAppMap.keySet())
            {
                if(appName.toLowerCase().equals(arr[0])){
                    AppModuleVo module = this.registerAppMap.get(appName).stream()
                            .collect(Collectors.toMap(a->a.getVersion(), v->v)).get(arr[1].replaceAll("\\-", ":"));
                    if(module != null)
                        return true;
                }
            }
        }
        finally {
            this.appReadLock.readLock().unlock();
        }
        return false;
    }

    @Override
    public AppModuleVo getRegisteredCCODAppFromImageUrl(String imageUrl) throws ParamException{
        logger.debug(String.format("get ccod module register info from %s", imageUrl));
        String[] arr = imageUrl.replaceAll(".*/", "").split(":");
        if(arr.length != 2)
            throw new ParamException(String.format("%s is illegal ccod module image url", imageUrl));
        this.appReadLock.readLock().lock();
        try{
            AppModuleVo module = null;
            for(String appName : this.registerAppMap.keySet())
            {
                if(appName.toLowerCase().equals(arr[0])){
                    module = this.registerAppMap.get(appName).stream()
                            .collect(Collectors.toMap(a->a.getVersion(), v->v)).get(arr[1].replaceAll("\\-", ":"));
                    if(module != null)
                        return module;
                }
            }
        }
        finally {
            this.appReadLock.readLock().unlock();
        }
        throw new ParamException(String.format("can not get module register info for %s", imageUrl));
    }

    @Override
    public AppModuleVo getAppModuleForBizSet(String bizSetName, String appAlias, String version) throws ParamException, NotSupportAppException {
        logger.debug(String.format("find app module info for alias %s[%s] at bisSet %s", appAlias, version, bizSetName));
        if(!this.ccodBiz.getSet().stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity())).containsKey(bizSetName))
            throw new ParamException(String.format("%s is illegal bizSetName for ccod", bizSetName));
        BizSetDefine setDefine = this.ccodBiz.getSet().stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity())).get(bizSetName);
        AppDefine appDefine = null;
        for(AppDefine define : setDefine.getApps())
        {
            String aliasRegex = String.format("^%s($|[0-9]\\d*$)", define.getAlias());
            if(appAlias.matches(aliasRegex)) {
                appDefine = define;
                break;
            }
        }
        if(appDefine == null)
            throw new NotSupportAppException(String.format("bizSet %s not support alias %s", bizSetName, appAlias));
        this.appReadLock.readLock().lock();
        try
        {
            if(!registerAppMap.containsKey(appDefine.getName()))
                throw new ParamException(String.format("%s[%s] not registered any version", appAlias, appDefine.getName()));
            else if(!registerAppMap.get(appDefine.getName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version))
                throw new ParamException(String.format("%s[%s] for %s not register", appDefine.getName(), version, appAlias));
            logger.debug(String.format("%s[%s] for %s found", appDefine.getName(), version, appAlias));
            return this.registerAppMap.get(appDefine.getName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version);
        }
        finally {
            this.appReadLock.readLock().unlock();
        }
    }

    @Override
    public boolean isSupport(String appName) {
        Set<String> appSet = this.ccodBiz.getSet().stream().flatMap(s->s.getApps().stream()).collect(Collectors.toList())
                .stream().collect(Collectors.groupingBy(AppDefine::getName)).keySet();
        return appSet.contains(appName);
    }
}
