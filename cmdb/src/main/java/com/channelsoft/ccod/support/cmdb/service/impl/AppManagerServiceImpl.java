package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.config.AppDefine;
import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.config.CCODBiz;
import com.channelsoft.ccod.support.cmdb.config.ImageCfg;
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

import javax.annotation.PostConstruct;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
//        updateAssetId4AppModule();
//        rectifyAppModule();
//        deleteNotUserData();
        try
        {
            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
//            List<LJBizInfo> bizList = paasService.queryAllBiz();
//            for(LJBizInfo biz : bizList)
//            {
//                List<LJHostInfo> hostList = paasService.queryBKHost(biz.getBkBizId(), null, null, null, null);
//                System.out.println(String.format("%s[%d] has %d hosts : %s", biz.getBkBizName(), biz.getBkBizId(), hostList.size(),
//                        String.join(",", hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity())).keySet())));
//                if(biz.getBkBizId() == 34)
//                    System.out.println("haha");
//            }

//            updateRegisterAppModuleImageExist();
            List<AppModuleVo> list = queryAllHasImageAppModule();
            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
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
    public void appDataTransfer(String targetRepository) {
        List<AppModuleVo> moduleList = this.appModuleMapper.select(null, null, null, null);
        Map<Integer, AppPo> appMap = this.appMapper.select(null, null, null, null).stream().collect(Collectors.toMap(AppPo::getAppId, Function.identity()));
        for(AppModuleVo vo : moduleList)
        {
            logger.debug(String.format("begin to transfer %s", vo.getAppNexusDirectory()));
            try
            {
                transferModule(appMap.get(vo.getAppId()), vo.getInstallPackage(), vo.getCfgs(), targetRepository);
            }
            catch (Exception ex)
            {
                logger.error(String.format("transfer %s exception", vo.getAppNexusDirectory()), ex);
                ex.printStackTrace();
            }

        }
    }

    /**
     * 迁移数据模块到指定的nexus仓库并修改数据库
     * 处理流程:首先下载程序包和配置文件,其次将下载的配置文件按一定格式上传到nexus指定仓库去，最后修改数据库记录
     * @param appPo 该平台应用对应的app详情
     * @param installPackagePo 程序包
     * @param cfgs 配置文件信息
     * @param targetRepository 目标仓库
     * @throws InterfaceCallException 接口调用失败
     * @throws NexusException nexus返回失败或是处理nexus返回信息失败
     * @throws IOException 保存文件失败
     */
    private void transferModule(AppPo appPo, AppInstallPackagePo installPackagePo, List<AppCfgFilePo> cfgs, String targetRepository) throws InterfaceCallException, NexusException, IOException
    {
        List<DeployFileInfo> fileList = new ArrayList<>();
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String directory = appPo.getAppNexusDirectory();
        logger.debug(String.format("begin to handle %s cfgs", directory));
        String tmpSaveDir = getTempSaveDir(DigestUtils.md5DigestAsHex(String.format("%s/%s", directory, sf.format(now)).getBytes()));
        {
            String downloadUrl = installPackagePo.getFileNexusDownloadUrl(this.nexusHostUrl);
            logger.debug(String.format("download install package from %s", downloadUrl));
            String savePth = nexusService.downloadFile(this.nexusUserName, this.nexusPassword, downloadUrl, tmpSaveDir, installPackagePo.getFileName());
            DeployFileInfo fileInfo = new DeployFileInfo();
            fileInfo.setExt(installPackagePo.getExt());
            fileInfo.setFileMd5(installPackagePo.getMd5());
            fileInfo.setLocalSavePath(savePth);
            fileInfo.setNexusRepository(this.appRepository);
            fileInfo.setNexusDirectory(directory);
            fileInfo.setFileSize(0);
            fileInfo.setFileName(installPackagePo.getFileName());
            fileList.add(fileInfo);
        }
        for(AppCfgFilePo cfg : cfgs)
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
            fileInfo.setFileSize(0);
            fileInfo.setFileName(cfg.getFileName());
            fileList.add(fileInfo);
        }
        directory = String.format("%s/%s", appPo.getAppName(),  appPo.getVersion());
        logger.debug(String.format("upload app package and cfgs to %s/%s/%s", nexusHostUrl, targetRepository, directory));
        nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, targetRepository, directory, fileList.toArray(new DeployFileInfo[0]));
        installPackagePo.setNexusDirectory(directory);
        installPackagePo.setNexusRepository(targetRepository);
        logger.debug(String.format("update package"));
        this.appInstallPackageMapper.update(installPackagePo);
        for(AppCfgFilePo cfg : cfgs)
        {
            cfg.setNexusDirectory(directory);
            cfg.setNexusRepository(targetRepository);
            logger.debug(String.format("update cfg"));
            this.appCfgFileMapper.update(cfg);
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
    public AppModuleVo queryAppByVersion(String appName, String version) throws ParamException, DataAccessException {
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
            return versionMap.get(version);
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
        logger.debug(String.format("begin to handle module[%s]", JSONObject.toJSONString(module)));
        AppPo appPo = module.getAppInfo();
        String appName = appPo.getAppName();
        String appVersion = appPo.getVersion();
        String appDirectory = appPo.getAppNexusDirectory();
        if(!this.registerAppMap.containsKey(appName) || !this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(appVersion))
        {
            String group = appPo.getAppNexusGroup();
            logger.warn(String.format("%s[%s] not exit register it first", appName, appVersion));
            List<NexusAssetInfo> appFileAssetList = this.nexusService.queryGroupAssetMap(this.nexusHostUrl, this.nexusUserName, nexusPassword, this.appRepository, group);
            if(appFileAssetList.size() > 0)
            {
                logger.info(String.format("%s[%s] is has not registered at cmdb, but %s not empty, clear fisrt",
                        appName, appVersion, group));
                for(NexusAssetInfo assetInfo : appFileAssetList)
                {
                    nexusService.deleteAsset(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, assetInfo.getId());
                }
                appFileAssetList = this.nexusService.queryGroupAssetMap(this.nexusHostUrl, this.nexusUserName, nexusPassword, this.appRepository, group);
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
        logger.debug(String.format("begin to handle module[%s]", JSONObject.toJSONString(module)));
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
            List<NexusAssetInfo> appFileAssetList = this.nexusService.queryGroupAssetMap(this.nexusHostUrl, this.nexusUserName, nexusPassword, this.appRepository, group);
            if(appFileAssetList.size() > 0)
            {
                logger.info(String.format("%s[%s] is has not registered at cmdb, but %s not empty, clear fisrt",
                        appName, appVersion, group));
                for(NexusAssetInfo assetInfo : appFileAssetList)
                {
                    nexusService.deleteAsset(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, assetInfo.getId());
                }
                appFileAssetList = this.nexusService.queryGroupAssetMap(this.nexusHostUrl, this.nexusUserName, nexusPassword, this.appRepository, group);
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
        else if(!installPackage.getFileMd5().equals(moduleVo.getInstallPackage().getMd5()) && !AppType.CCOD_WEBAPPS_MODULE.equals(appType))
        {
            logger.error(String.format("%s[%s] install package file md5 is %s not %s",
                    appName, appVersion, moduleVo.getInstallPackage().getMd5(), installPackage.getFileMd5()));
            throw new ParamException(String.format("%s[%s] install package file md5 is %s not %s",
                    appName, appVersion, moduleVo.getInstallPackage().getMd5(), installPackage.getFileMd5()));
        }
        if(!this.appSetRelationMap.containsKey(appName) && !AppType.CCOD_WEBAPPS_MODULE.equals(appType)) {
            Map<String, DeployFileInfo> reportCfgMap = Arrays.stream(cfgs).collect(Collectors.toMap(DeployFileInfo::getFileName, Function.identity()));
            Map<String, AppCfgFilePo> wantCfgMap = moduleVo.getCfgs().stream().collect(Collectors.toMap(AppCfgFilePo::getFileName, Function.identity()));
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
        logger.debug(String.format("add new app module %s(%s)[%s] to cmdb", appName, version, JSONObject.toJSONString(appPo)));
        String directory = appPo.getAppNexusDirectory();
        addAppToNexus(appName, version, installPackage, cfgs, this.appRepository, directory);
        AppModuleVo moduleVo = addNewAppToDB(appPo, installPackage, cfgs);
        logger.info(String.format("%s[%s] add success", appName, version));
        return moduleVo;
    }

    /**
     * 将安装包和配置文件和nexus存储的信息对比
     * @param moduleVo 应用模块
     * @param NexusFileList 该应用存储在nexus的文件信息
     * @return 对比结果,如果完全符合返回""
     */
    private String compareAppFileWithNexusRecord(AppPo appPo, AppModuleVo moduleVo, List<NexusAssetInfo> NexusFileList)
    {
        String appName = moduleVo.getAppName();
        String version = moduleVo.getVersion();
        AppInstallPackagePo installPackage= moduleVo.getInstallPackage();
        List<AppCfgFilePo> cfgs = moduleVo.getCfgs();
        logger.debug(String.format("begin to compare appName=%s and version=%s installPackage and cfgs with nexus record",
                appName, version));
        Map<String, NexusAssetInfo> nexusFileMap = NexusFileList.stream().collect(Collectors.toMap(NexusAssetInfo::getPath, Function.identity()));
        logger.debug(String.format("nexusFileMap=%s", String.join(",", nexusFileMap.keySet())));
        StringBuffer sb = new StringBuffer();
        String directory = appPo.getAppNexusDirectory();
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
        boolean imageExist = false;
        String url = String.format("http://%s/v2/%s/%s/tags/list", this.nexusDockerUrl, this.imageRepository, appName.toLowerCase());
        try
        {
            String queryResult = HttpRequestTools.httpGetRequest(url, this.nexusUserName, this.nexusPassword);
            String tags = JSONObject.parseObject(queryResult).getString("tags");
            Set<String> set = new HashSet<>(JSONArray.parseArray(tags, String.class));
            if(set.contains(version.replaceAll("\\:", "-")))
                imageExist = true;
        }
        catch (Exception ex)
        {
            logger.error(String.format("query %s fail", ex));
        }
        logger.info(String.format("%s[%s] image exist : %b", appName, version, imageExist));
        return imageExist;
    }

    void updateRegisterAppModuleImageExist() throws Exception
    {
        this.appWriteLock.writeLock().lock();
        try
        {
            List<AppPo> appList = this.appMapper.select(null, null, null, null);
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
    public void registerNewAppModule(AppModuleVo appModule) throws NotSupportAppException, ParamException, InterfaceCallException, NexusException, IOException {
        logger.debug(String.format("begin to register app=[%s] into cmdb", JSONObject.toJSONString(appModule)));
        this.appWriteLock.writeLock().lock();
        try
        {
            String appName = appModule.getAppName();
            String version = appModule.getVersion();
            if (!this.appSetRelationMap.containsKey(appName)) {
                logger.error(String.format("app %s is not supported by cmdb", appName));
                throw new NotSupportAppException(String.format("app %s is not supported by cmdb", appName));
            }
            String moduleCheckResult = checkModuleParam(appModule);
            if (StringUtils.isNotBlank(moduleCheckResult)) {
                logger.error(String.format("app module params check FAIL %s", moduleCheckResult));
                throw new ParamException(String.format("app module params check FAIL %s", moduleCheckResult));
            }
            if(this.registerAppMap.containsKey(appName) && this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getAppName, Function.identity())).containsKey(version))
            {
                logger.error(String.format("%s(%s) has registered", appName, version));
                throw new ParamException(String.format("%s(%s) has registered", appName, version));
            }
            String directory = appModule.getAppNexusDirectory();
            String tmpSaveDir = getTempSaveDir(DigestUtils.md5DigestAsHex(directory.getBytes()));
            List<DeployFileInfo> fileList = new ArrayList<>();
            String downloadUrl = appModule.getInstallPackage().getFileNexusDownloadUrl(this.publishNexusHostUrl);
            logger.debug(String.format("download package from %s", downloadUrl));
            String savePth = nexusService.downloadFile(this.nexusUserName, this.nexusPassword, downloadUrl, tmpSaveDir, appModule.getInstallPackage().getFileName());
            String md5 = DigestUtils.md5DigestAsHex(new FileInputStream(savePth));
            if(!md5.equals(appModule.getInstallPackage().getMd5()))
            {
                logger.error(String.format("install package %s verify md5 FAIL : report=%s and download=%s",
                        appModule.getInstallPackage().getFileName(), appModule.getInstallPackage().getMd5(), md5));
                throw new ParamException(String.format("install package %s verify md5 FAIL : report=%s and download=%s",
                        appModule.getInstallPackage().getFileName(), appModule.getInstallPackage().getMd5(), md5));
            }
            fileList.add(new DeployFileInfo(appModule.getInstallPackage().getFileName(), savePth));
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
                fileList.add(new DeployFileInfo(cfg.getFileName(), savePth));
            }
            this.nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, this.appRepository, directory, fileList.toArray(new DeployFileInfo[0])).stream().collect(Collectors.toMap(NexusAssetInfo::getPath, Function.identity()));
            Map<String, DeployFileInfo> fileMap = fileList.stream().collect(Collectors.toMap(DeployFileInfo::getFileName, Function.identity()));
            AppPo appPo = new AppPo(appModule, false);
            Date now = new Date();
            appPo.setCreateTime(now);
            appPo.setUpdateTime(now);
            logger.debug(String.format("insert app info [%s]", JSONObject.toJSONString(appPo)));
            this.appMapper.insert(appPo);
            int appId = appPo.getAppId();
            AppInstallPackagePo packagePo = new AppInstallPackagePo(appId, fileMap.get(appModule.getInstallPackage().getFileName()));
            logger.debug(String.format("insert package info [%s]", JSONObject.toJSONString(packagePo)));
            this.appInstallPackageMapper.insert(packagePo);
            for(AppCfgFilePo cfg : appModule.getCfgs())
            {
                AppCfgFilePo cfgFilePo = new AppCfgFilePo(appId, fileMap.get(cfg.getFileName()));
                logger.debug(String.format("insert cfg info [%s]", JSONObject.toJSONString(cfgFilePo)));
                this.appCfgFileMapper.insert(cfgFilePo);
            }
            AppModuleVo newModule = this.appModuleMapper.selectByNameAndVersion(appName, version);
            this.appReadLock.writeLock().lock();
            try
            {
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
    public void updateAppModule(AppModuleVo appModule) throws NotSupportAppException, ParamException, InterfaceCallException, NexusException, IOException {
        logger.debug(String.format("begin to modify cfg of app=[%s] in cmdb", JSONObject.toJSONString(appModule)));
        String appName = appModule.getAppName();
        String version = appModule.getVersion();
        if (!this.appSetRelationMap.containsKey(appName)) {
            logger.error(String.format("app %s is not supported by cmdb", appName));
            throw new NotSupportAppException(String.format("app %s is not supported by cmdb", appName));
        }
        String moduleCheckResult = checkModuleParam(appModule);
        if (StringUtils.isNotBlank(moduleCheckResult)) {
            logger.error(String.format("app module params check FAIL %s", moduleCheckResult));
            throw new ParamException(String.format("app module params check FAIL %s", moduleCheckResult));
        }
        this.appWriteLock.writeLock().lock();
        try
        {
            String directory = appModule.getAppNexusDirectory();
            if (!this.registerAppMap.containsKey(appName) || !this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version))
            {
                logger.error(String.format("%s version %s has not registered", appName, version));
                throw new ParamException(String.format("%s version %s has not registered", appName, version));
            }
            List<NexusAssetInfo> fileList = new ArrayList<>();
            fileList.add(appModule.getInstallPackage().getNexusAsset(this.nexusHostUrl));
            for(AppCfgFilePo cfg : appModule.getCfgs())
                fileList.add(cfg.getNexusAsset(this.nexusHostUrl));
            logger.debug(String.format("download package and cfgs and upload to %s", directory));
            Map<String, NexusAssetInfo> fileMap = this.nexusService.downloadAndUploadFiles(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, fileList, this.nexusHostUrl, this.nexusUserName, this.nexusPassword, this.appRepository, directory, false).stream().collect(Collectors.toMap(NexusAssetInfo::getNexusAssetFileName, Function.identity()));;
            AppModuleVo oldModuleVo = this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version);
            logger.debug(String.format("delete old version install package"));
            this.appInstallPackageMapper.delete(oldModuleVo.getInstallPackage().getPackageId(), oldModuleVo.getAppId());
            logger.debug(String.format("delete old version cfgs"));
            this.appCfgFileMapper.delete(null, oldModuleVo.getAppId());
            logger.debug(String.format("add package info"));
            AppInstallPackagePo packagePo = new AppInstallPackagePo(oldModuleVo.getAppId(), appModule.getInstallPackage().getDeployPath(), fileMap.get(appModule.getInstallPackage().getFileName()));
            this.appInstallPackageMapper.insert(packagePo);
            logger.debug(String.format("add %d cfgs info", appModule.getCfgs().size()));
            for(AppCfgFilePo cfg : appModule.getCfgs())
            {
                AppCfgFilePo cfgFilePo = new AppCfgFilePo(oldModuleVo.getAppId(), cfg.getDeployPath(), fileMap.get(cfg.getFileName()));
                this.appCfgFileMapper.insert(cfgFilePo);
            }
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
    public List<NexusAssetInfo> downloadAndUploadAppFiles(String srcNexusHostUrl, String srcNexusUser, String srcPwd, List<AppFilePo> srcFileList, String dstRepository, String dstDirectory) throws ParamException, InterfaceCallException, NexusException, IOException
    {
        List<DeployFileInfo> fileList = new ArrayList<>();
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String tmpSaveDir = getTempSaveDir(DigestUtils.md5DigestAsHex(String.format("%s;%s", dstDirectory, sf.format(now)).getBytes()));
        for(AppFilePo filePo : srcFileList)
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
            fileList.add(new DeployFileInfo(filePo.getFileName(), savePth));
        }
        return this.nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, dstRepository, dstDirectory, fileList.toArray(new DeployFileInfo[0]));
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
            Map<String, AppDefine> map = setDefine.getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity()));
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
                    if(!this.registerAppMap.containsKey(appName))
                    {
                        logger.error(String.format("set %s not support %s", setName, appName));
                        throw new NotSupportAppException(String.format("set %s not support %s", setName, appName));
                    }
                    String standardAlias = map.get(appName).getAlias();
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
        AppModuleVo moduleVo = queryAppByVersion(appName, version);
        if(moduleVo == null)
        {
            logger.error(String.format("%s[%] not exist", appName, version));
            throw new ParamException(String.format("%s[%] not exist", appName, version));
        }
        Map<String, AppCfgFilePo> cfgMap = moduleVo.getCfgs().stream()
                .collect(Collectors.toMap(AppCfgFilePo::getFileName, Function.identity()));
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
        AppCfgFilePo cfgFilePo = cfgMap.get(cfgFileName);
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
                usedAlias = new ArrayList<>(domainAppMap.get(appName).stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAppAlias, Function.identity())).keySet());
            }
            List<AppUpdateOperationInfo> needAliasList = new ArrayList<>();
            for(AppUpdateOperationInfo optInfo : addAppMap.get(appName))
            {
                if(StringUtils.isBlank(optInfo.getAppAlias()) || !clone)
                    needAliasList.add(optInfo);
                else
                    usedAlias.add(optInfo.getAppAlias());
            }
            boolean onlyOne = usedAlias.size() == 0 && needAliasList.size() == 1 ? true : false;
            for(AppUpdateOperationInfo optInfo : needAliasList)
            {
                String alias = autoGenerateAlias(standAlias, usedAlias, onlyOne);
                optInfo.setAppAlias(alias);
                usedAlias.add(alias);
            }
        }
        for(AppUpdateOperationInfo optInfo : addOptList)
        {
            if(StringUtils.isBlank(optInfo.getOriginalAlias()))
                optInfo.setOriginalAlias(optInfo.getAppAlias());
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
    public void indexTest()
    {
        try
        {
            String standId = "ucgateway";
            List<String> usedIds = new ArrayList<>();
//            usedIds.add("tr207");
//            usedIds.add("tr203");
            for(int i = 0; i < 5; i++)
            {
                String domainId = autoGenerateAlias(standId, usedIds, true);
                System.out.println(domainId);
                usedIds.add(domainId);
            }
            usedIds.add(standId + "225");
            usedIds.add(standId + "112");
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

    @Test
    public void removeTest()
    {
        String pbCfgData = "[{\"deployPath\":\"/root/resin-4.0.13/conf\",\"fileName\":\"local_datasource.xml\",\"fileSize\":0,\"md5\":\"e9c26f00f17a7660bfa3f785c4fe34be\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFhN2U5NGFhNDVlZTIxN2Nm\",\"nexusPath\":\"/configText/123456-wuph/publicConfig/local_datasource.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"/root/resin-4.0.13/conf\",\"fileName\":\"local_jvm.xml\",\"fileSize\":0,\"md5\":\"d41d8cd98f00b204e9800998ecf8427e\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNkODFhZTYxM2NmNDM3NmQ3\",\"nexusPath\":\"/configText/123456-wuph/publicConfig/local_jvm.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"/usr/local/lib\",\"fileName\":\"tnsnames.ora\",\"fileSize\":0,\"md5\":\"d41d8cd98f00b204e9800998ecf8427e\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQzMDcyY2Q2ZTEyNzg2NTQ3\",\"nexusPath\":\"/configText/123456-wuph/publicConfig/tnsnames.ora\",\"nexusRepository\":\"tmp\"}]";
        List<AppFileNexusInfo> list = JSONArray.parseArray(pbCfgData, AppFileNexusInfo.class);
        System.out.println(list.size());
        try {
            Map<String, String> map = new HashMap<>();
            map.put("1", "Wuhan");
            map.put("2", "Shanghai");
            map.put("3", "Beijing");
            map.put("4", "Nanjing");
            map.put("5", "Guangzhou");
            map.put("6", "Chengdu");
            map.put("7", "Helei");
            List<String> set = new ArrayList<>(map.keySet());
            for(String key : set)
            {
                if("1".equals(key) || "3".equals(key))
                    map.remove(key);
            }
            System.out.println(String.join(",", map.keySet()));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
