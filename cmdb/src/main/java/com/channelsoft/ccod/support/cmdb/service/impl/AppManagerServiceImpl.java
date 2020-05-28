package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.config.AppDefine;
import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.config.CCODBiz;
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

    @Value("${cmdb.url}")
    private String cmdbUrl;

    @Value("${nexus.platform-app-cfg-repository}")
    private String platformAppCfgRepository;

    @Value("${nexus.app-module-repository}")
    private String appRepository;

    @Value("${nexus.unconfirmed-platform-app-repository}")
    private String unconfirmedPlatformAppRepository;

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

    @Value("${k8s.gls_oracle_svc_name}")
    private String glsOracleSvcName;

    @Value("${k8s.gls_oracle_sid}")
    private String glsOracleSid;

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
    ILJPaasService paasService;

    @Autowired
    private CCODBiz ccodBiz;

    private Map<String, List<BizSetDefine>> appSetRelationMap;

    private Map<String, BizSetDefine> setDefineMap;

    private Set<String> notCheckCfgAppSet;

    private boolean isPlatformCheckOngoing = false;

    private final static Logger logger = LoggerFactory.getLogger(AppManagerServiceImpl.class);

    private Map<String, PlatformUpdateSchemaInfo> platformUpdateSchemaMap = new ConcurrentHashMap<>();

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
        this.appReadLock.writeLock().lock();
        try {
            flushRegisteredApp();
        }
        finally {
            this.appReadLock.writeLock().unlock();
        }
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

            System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");

        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    /**
     * 更新平台当前注册应用信息
     */
    protected void flushRegisteredApp()
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
    public List<AppModuleVo> queryAllRegisterAppModule() {
        this.appReadLock.readLock().lock();
        try
        {
            return this.registerAppMap.values().stream().flatMap(listContainer -> listContainer.stream()).collect(Collectors.toList());
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

    private void deleteNotUserData()
    {
        Map<Integer, AppInstallPackagePo> pkgMap = this.appInstallPackageMapper.select().stream().collect(Collectors.toMap(AppInstallPackagePo::getAppId, Function.identity()));
        Map<Integer, List<AppCfgFilePo>> cfgMap = this.appCfgFileMapper.select(null).stream().collect(Collectors.groupingBy(AppCfgFilePo::getAppId));
        Map<Integer, AppModuleVo> moduleMap = this.appModuleMapper.select(null, null, null, null).stream().collect(Collectors.toMap(AppModuleVo::getAppId, Function.identity()));
        for(int appId : pkgMap.keySet())
        {
            if(!moduleMap.containsKey(appId))
            {
                logger.debug(String.format("install package of appId=%d is not effect, so delete", appId));
                this.appInstallPackageMapper.delete(null, appId);
            }
        }
        for(int appId : cfgMap.keySet())
        {
            if(!moduleMap.containsKey(appId))
            {
                logger.debug(String.format("%d cfgs of appId=%d is not effective, so delete", cfgMap.get(appId).size(), appId));
                for(AppCfgFilePo cfg : cfgMap.get(appId))
                {
                    logger.debug(String.format("delete fileId=%d and appId=%d", cfg.getCfgFileId(), appId));
                    this.appCfgFileMapper.delete(cfg.getCfgFileId(), appId);
                }


            }
        }
    }

    private void rectifyAppModule()
    {
        List<AppModuleVo> moduleList = this.appModuleMapper.select(null, null, null, null);
        for(AppModuleVo moduleVo : moduleList)
        {
            String tag = moduleVo.toString();
            boolean isPkgOk = true;
            if(!this.appRepository.equals(moduleVo.getInstallPackage().getNexusRepository()))
                isPkgOk = false;
            for(AppCfgFilePo cfg : moduleVo.getCfgs())
            {
                if(!this.appRepository.equals(cfg.getNexusRepository()) && isPkgOk
                || this.appRepository.equals(cfg.getNexusRepository()) && !isPkgOk)
                {
                    logger.error(String.format("%s can not been rectified", tag));
                    continue;
                }
            }
            if(isPkgOk)
            {
                logger.info(String.format("%s is OK", tag));
                continue;
            }
            else {
                try {
                    logger.debug(String.format("update %s", tag));
                    this.updateAppModule(moduleVo);
                    AppModuleVo newModule = this.appModuleMapper.selectByNameAndVersion(moduleVo.getAppName(), moduleVo.getVersion());
                    if (!this.appRepository.equals(newModule.getInstallPackage().getNexusRepository())) {
                        logger.error(String.format("rectify %s fail", tag));
                        continue;
                    }
                    for (AppCfgFilePo cfg : newModule.getCfgs()) {
                        if (!this.appRepository.equals(cfg.getNexusRepository())) {
                            logger.error(String.format("rectify %s fail", tag));
                            continue;
                        }
                    }
                } catch (Exception ex) {
                    logger.error(String.format("rectify %s exception", tag), ex);
                }
            }
        }
    }

    private void updateAssetId4AppModule() throws Exception
    {
        List<AppModuleVo> moduleList = this.appModuleMapper.select(null, null, null, null);
        for(AppModuleVo moduleVo : moduleList)
        {
            System.out.println(String.format("appName=%s and version=%s", moduleVo.getAppName(), moduleVo.getVersion()));
            String group = String.format("/%s", moduleVo.getInstallPackage().getNexusDirectory());
            List<NexusAssetInfo> assetList = this.nexusService.queryGroupAssetMap(this.nexusHostUrl, this.nexusUserName,
                    this.nexusPassword, moduleVo.getInstallPackage().getNexusRepository(), group);
            String assetId = assetList.stream().collect(Collectors.toMap(NexusAssetInfo::getNexusAssetFileName, Function.identity()))
                    .get(moduleVo.getInstallPackage().getFileName()).getId();
            moduleVo.getInstallPackage().setNexusAssetId(assetId);
            this.appInstallPackageMapper.update(moduleVo.getInstallPackage());
            for(AppCfgFilePo cfg : moduleVo.getCfgs())
            {
                assetList.stream().collect(Collectors.toMap(NexusAssetInfo::getNexusAssetFileName, Function.identity()))
                        .get(cfg.getFileName()).getId();
                cfg.setNexusAssetId(assetId);
                this.appCfgFileMapper.update(cfg);
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
    public AppModuleVo[] queryApps(String appName) throws DataAccessException {
        this.appReadLock.readLock().lock();
        try
        {
            logger.debug(String.format("begin to query app modules : appName=%s", appName));
            List<AppModuleVo> list;
            if(StringUtils.isBlank(appName))
               list  = this.registerAppMap.values().stream().flatMap(listContainer -> listContainer.stream()).collect(Collectors.toList());
            else
                list = this.registerAppMap.containsKey(appName) ? this.registerAppMap.get(appName) : new ArrayList<>();
            logger.info(String.format("query %d app module record with appName=%s", list.size(), appName));
            return list.toArray(new AppModuleVo[0]);
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
    public PlatformAppModuleVo[] startCollectPlatformAppData(String platformId, String platformName, int bkBizId, int bkCloudId) throws ParamException, Exception {
        PlatformPo platformPo = platformMapper.selectByPrimaryKey(platformId);
        if(platformPo != null)
        {
            logger.error(String.format("platformId=%s platform has existed", platformId));
            throw new ParamException(String.format("platformId=%s platform has existed", platformId));
        }
        if(this.isPlatformCheckOngoing)
        {
            logger.error(String.format("start platform=%s app data collect FAIL : some collect task is ongoing", platformId));
            throw new ClientCollectDataException(String.format("start platform=%s app data collect FAIL : some collect task is ongoing", platformId));
        }
        this.isPlatformCheckOngoing = true;
        try
        {
            List<PlatformAppModuleVo> modules = this.platformAppCollectService.collectPlatformAppData(platformId, platformName, null, null, null, null);
            List<PlatformAppModuleVo> failList = new ArrayList<>();
            List<PlatformAppModuleVo> successList = new ArrayList<>();
            for(PlatformAppModuleVo collectedModule : modules)
            {
                if(collectedModule.isOk(platformId, platformName, this.appSetRelationMap))
                    successList.add(collectedModule);
                else
                {
                    logger.debug(collectedModule.getComment());
                    failList.add(collectedModule);
                }
            }
            platformPo = modules.get(0).getPlatform();
            platformPo.setBkBizId(bkBizId);
            platformPo.setBkCloudId(bkCloudId);
            platformMapper.insert(platformPo);
            List<DomainPo> domainList = new ArrayList<>();
            Map<String, List<PlatformAppModuleVo>> domainAppMap = successList.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getDomainName));
            List<String> domainNameList = new ArrayList<>(domainAppMap.keySet());
            for(String domainName : domainNameList)
            {
                try
                {
                    DomainPo domainPo = parseDomainApps(domainAppMap.get(domainName));
                    domainList.add(domainPo);
                }
                catch (ParamException ex)
                {
                    for(PlatformAppModuleVo moduleVo : domainAppMap.get(domainName))
                    {
                        moduleVo.setComment(ex.getMessage());
                        failList.add(moduleVo);
                    }
                    domainAppMap.remove(domainName);
                }
            }
            logger.debug(String.format("%s has %d domain : %s", platformName, domainAppMap.size(), String.join(",", domainAppMap.keySet())));
            Map<String, List<DomainPo>> setDomainMap = domainList.stream().collect(Collectors.groupingBy(DomainPo::getType));
            for(String bkSetName : setDomainMap.keySet())
            {
                List<DomainPo> setDomainList = setDomainMap.get(bkSetName);
                List<String> usedIds = new ArrayList<>();
                String standardDomainId = this.setDefineMap.get(bkSetName).getFixedDomainId();
                for(DomainPo domainPo : setDomainList)
                {
                    String newDomainId = autoGenerateDomainId(standardDomainId, usedIds);
                    domainPo.setDomainId(newDomainId);
                    usedIds.add(newDomainId);
                    logger.debug(String.format("domainId of %s is %s", domainPo.getDomainName(), domainPo.getDomainId()));
                    for(PlatformAppModuleVo moduleVo : domainAppMap.get(domainPo.getDomainName()))
                    {
                        moduleVo.setDomainId(newDomainId);
                    }
                }
            }
            for(DomainPo po : domainList)
            {
                logger.debug(String.format("insert new domain [%s]", JSONObject.toJSONString(po)));
                this.domainMapper.insert(po);
            }
            logger.debug(String.format("begin to handle collected %d app", successList.size()));
            this.appWriteLock.writeLock().lock();
            try
            {
                for(PlatformAppModuleVo moduleVo : successList)
                {
                    try
                    {
                        handlePlatformAppModule(moduleVo);
                    }
                    catch (ParamException ex)
                    {
                        moduleVo.setComment(ex.getMessage());
                        failList.add(moduleVo);
                    }
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
            for(PlatformAppModuleVo moduleVo : failList)
            {
                try
                {
                    UnconfirmedAppModulePo unconfirmedAppModulePo = handleUnconfirmedPlatformAppModule(moduleVo);
                    this.unconfirmedAppModuleMapper.insert(unconfirmedAppModulePo);
                }
                catch (Exception ex)
                {
                    logger.error(String.format("handle unconfirmed app exception"), ex);
                }

            }
            this.paasService.syncClientCollectResultToPaas(bkBizId, platformId, bkCloudId);
            platformPo.setStatus(CCODPlatformStatus.RUNNING.id);
            this.platformMapper.update(platformPo);
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
     * 从指定的域模块获取域信息以及域归属的集群信息
     * @param domainAppList 域模块列表
     * @return 模块所属的域信息
     * @throws ParamException 从域模块中无法获取正确的域信息
     */
    private DomainPo parseDomainApps(List<PlatformAppModuleVo> domainAppList) throws ParamException
    {
        String domainName = domainAppList.get(0).getDomainName();
        logger.debug(String.format("begin to parse %d apps of domainName=%s", domainAppList.size(), domainName));
        Map<String, List<PlatformAppModuleVo>> map = domainAppList.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getDomainName));
        if(map.size() > 1)
        {
            logger.error(String.format("%s has not unique id %s", domainName, String.join(",", map.keySet())));
            throw new ParamException(String.format("%s has not unique id %s", domainName, String.join(",", map.keySet())));
        }
        String domainId = domainAppList.get(0).getDomainId();
        map = domainAppList.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getModuleAliasName));
        for(String alias : map.keySet())
        {
            if(map.get(alias).size() > 1)
            {
                logger.error(String.format("alias %s of %s(%s) is not unique", alias, domainName, domainId));
                throw new ParamException(String.format("alias %s of %s(%s) is not unique", alias, domainName, domainId));
            }
        }
        String bkSetName = null;
        Map<String, Set<String>> appSetNameMap = new HashMap<>();
        map = domainAppList.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getModuleName));
        String comparedAppName = null;
        for(String appName : map.keySet())
        {
            if(this.appSetRelationMap.containsKey(appName))
            {
                appSetNameMap.put(appName, this.appSetRelationMap.get(appName).stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity())).keySet());
                comparedAppName = appName;
            }
        }
        if(appSetNameMap.size() == 0)
        {
            logger.error(String.format("can not decline bkSetName of %s(%s) : all apps not support", domainName, domainId));
            throw new ParamException(String.format("can not decline bkSetName of %s(%s) : all apps not support", domainName, domainId));
        }
        else if(appSetNameMap.size()  == 1 && appSetNameMap.get(comparedAppName).size() > 1)
        {
            logger.error(String.format("can not decline bkSetName of %s(%s) : set of %s is ambiguous", domainName, domainId, comparedAppName));
            throw new ParamException(String.format("can not decline bkSetName of %s(%s) : set of %s is ambiguous", domainName, domainId, comparedAppName));
        }
        for(String setName : appSetNameMap.get(comparedAppName))
        {
            if(bkSetName != null)
                break;
            boolean isMatch = true;
            for(String appName : appSetNameMap.keySet())
            {
                if(!appSetNameMap.get(appName).contains(setName)) {
                    isMatch =false;
                    break;
                }
            }
            if(isMatch)
            {
                bkSetName = setName;
                break;
            }
        }
        if(bkSetName == null)
        {
            logger.error(String.format("can not decline bkSetName of %s(%s) : set of all apps is ambiguous", domainName, domainId));
            throw new ParamException(String.format("can not decline bkSetName of %s(%s) : set of all apps is ambiguous", domainName, domainId));
        }
        map = domainAppList.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getModuleName));
        for(String appName : map.keySet())
        {
            List<PlatformAppModuleVo> appModuleList = map.get(appName);
            boolean onlyOne = appModuleList.size() == 1 ? true : false;
            String standardAlias =  this.appSetRelationMap.get(appName).stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity())).get(bkSetName).getApps()
                    .stream().collect(Collectors.toMap(AppDefine::getName, Function.identity())).get(appName).getAlias();
            List<String> usedAlias = new ArrayList<>();
            for(PlatformAppModuleVo appModuleVo : appModuleList)
            {
                String alias = autoGenerateAlias(standardAlias, usedAlias, onlyOne);
                appModuleVo.setAlias(alias);
                logger.debug(String.format("alias of %s at %s is %s[%s]",
                        appName, domainName, appModuleVo.getAlias(), appModuleVo.getModuleAliasName()));
                usedAlias.add(alias);
            }
        }
        DomainPo  po  = map.get(comparedAppName).get(0).getDomain();
        po.setType(bkSetName);
        logger.debug(String.format("%s belong to %s", domainName, bkSetName));
        return po;
    }


    private UnconfirmedAppModulePo handleUnconfirmedPlatformAppModule(PlatformAppModuleVo moduleVo) throws Exception
    {
        AppPo appPo = moduleVo.getAppInfo();
        String appName = appPo.getAppName();
        String appVersion = appPo.getVersion();
        UnconfirmedAppModulePo po = moduleVo.getUnconfirmedModule();
        logger.debug(String.format("begin to handle unconfirmed %s", po.toString()));
        List<DeployFileInfo> fileList = new ArrayList<>();
        if(StringUtils.isNotBlank(moduleVo.getInstallPackage().getLocalSavePath()))
            fileList.add(moduleVo.getInstallPackage());
        for(DeployFileInfo cfg : moduleVo.getCfgs())
        {
            if(StringUtils.isNotBlank(cfg.getLocalSavePath()))
                fileList.add(cfg);
        }
        if(fileList.size() > 0)
        {
            PlatformAppPo platformApp = moduleVo.getPlatformApp();
            String platformCfgDirectory = platformApp.getPlatformAppDirectory(appName, appVersion, platformApp);
            List<NexusAssetInfo> assetList = this.nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, this.unconfirmedPlatformAppRepository, platformCfgDirectory, fileList.toArray(new DeployFileInfo[0]));
            Map<String, String> fileUrlMap = new HashMap<>();
            for(NexusAssetInfo assetInfo : assetList)
            {
                String[] arr = assetInfo.getPath().split("/");
                fileUrlMap.put(arr[arr.length - 1], String.format("%s/%s", this.unconfirmedPlatformAppRepository, assetInfo.getPath()));
            }
            if(StringUtils.isNotBlank(moduleVo.getInstallPackage().getLocalSavePath()))
            {
                po.setPackageDownloadUrl(fileUrlMap.get(moduleVo.getInstallPackage().getFileName()));
                fileUrlMap.remove(moduleVo.getInstallPackage().getFileName());
            }
            else
                po.setPackageDownloadUrl("");
            po.setCfgDownloadUrl(String.join(",", fileUrlMap.values()));
        }
        return po;
    }

    /**
     * 处理客户端收集的平台应用信息
     * 如果该模块在db中没有记录则需要上传二进制安装包以及配置文件并在数据库创建一条记录
     * 归档平台应用的配置文件，并在数据库创建一条平台应用详情记录
     * @param module 客户端收集的平台应用信息
     * @return 添加后的模块
     * @throws DataAccessException 查询数据库异常
     * @throws InterfaceCallException 调用nexus接口异常
     * @throws NexusException nexus返回调用失败信息或是解析nexus调用结果失败
     * @throws DBNexusNotConsistentException cmdb记录的信息和nexus不一致
     */
    private boolean handlePlatformAppModule(PlatformAppModuleVo module)
            throws DataAccessException, InterfaceCallException, NexusException, ParamException
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
            if(!this.registerAppMap.containsKey(appName))
                this.registerAppMap.put(appName, new ArrayList<>());
            logger.debug(String.format("update register app module info"));
            this.registerAppMap.get(appName).add(newModule);
        }
        AppModuleVo moduleVo = this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(appVersion);
        logger.debug(String.format("check package and cfgs of %s(%s)", appName, appVersion));
        checkInstPkgAndCfg(moduleVo, module.getInstallPackage(), module.getCfgs());
        PlatformAppPo platformApp = module.getPlatformApp();
        String platformCfgDirectory = platformApp.getPlatformAppDirectory(appName, appVersion, platformApp);
        logger.debug(String.format("update cfgs to %s/%s", this.platformAppCfgRepository, platformCfgDirectory));
        nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword,
                this.platformAppCfgRepository, platformCfgDirectory, module.getCfgs());
        platformApp.setAppId(moduleVo.getAppId());
        logger.debug(String.format("insert platform app info to db"));
        this.platformAppMapper.insert(platformApp);
        for(DeployFileInfo cfgFilePo : module.getCfgs())
        {
            PlatformAppCfgFilePo po = new PlatformAppCfgFilePo(platformApp.getPlatformAppId(), cfgFilePo);
            logger.debug(String.format("insert cfg %s into db", po.getFileName()));
            this.platformAppCfgFileMapper.insert(po);
        }
        logger.info(String.format("[%s] platform app module handle SUCCESS", module.toString()));
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
                    startCollectPlatformAppData(platformId, platformName, bkBizId, bkCloudId);
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
     * 部署应用到域主机
     * @param domain 部署应用的域信息
     * @param bkSet 部署该应用的set信息
     * @param hostList 平台下面的所有主机列表
     * @param deployOperationList 部署的操作
     * @throws InterfaceCallException 调用蓝鲸api失败
     * @throws LJPaasException 蓝鲸api返回调用失败或是解析蓝鲸api返回结果
     */
    private void deployAppsToDomainHost(DomainPo domain, LJSetInfo bkSet, List<LJHostInfo> hostList,
                                        List<AppUpdateOperationInfo> deployOperationList)
            throws InterfaceCallException, LJPaasException, NexusException, IOException
    {
        logger.debug(String.format("begin to add new %d apps to %s(%s)/%s(%s)", deployOperationList.size(),
                domain.getPlatformId(), bkSet.getBkSetName(), domain.getDomainId(), domain.getDomainName()));
        Date now = new Date();
        List<PlatformAppPo> deployAppList = new ArrayList<>();
        Map<String, LJHostInfo> hostMap = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity()));
        for(AppUpdateOperationInfo deployOperationInfo : deployOperationList)
        {
            Map<String, AppModuleVo> versionMap = this.registerAppMap.get(deployOperationInfo.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
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
        paasService.bindDeployAppsToBizSet(bkSet.getBkBizId(), bkSet.getBkSetId(), bkSet.getBkSetName(), deployAppList);
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

    /**
     * 检查平台升级计划参数是否合法
     * @param schema 需要检查的平台升级计划
     * @param domainList 该平台下的所有域
     * @param deployApps 该平台已经部署的应用
     * @param bkSetList 该平台的对应的蓝鲸biz下的所有set列表
     * @param bkHostList 该平台下所有主机
     * @param appBkModuleList 该平台升级签部署的应用和蓝鲸模块的关系表
     * @return 如果检查通过返回空，否则返回检查失败原因
     */
    private String checkPlatformUpdateTask(PlatformUpdateSchemaInfo schema, List<DomainPo> domainList,
                                           List<PlatformAppPo> deployApps,
                                           List<LJSetInfo> bkSetList, List<LJHostInfo> bkHostList,
                                           List<PlatformAppBkModulePo> appBkModuleList)
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
                            if(!setDefineMap.containsKey(planInfo.getBkSetName()))
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
            String operationCheckResult = updateOperationCheck(updateType, planInfo.getAppUpdateOperationList(), domainAppList, appBkModuleList, bkHostList);
            sb.append(operationCheckResult);
        }
        return sb.toString();
    }


    /**
     * 验证应用升级操作的相关蚕食是否正确
     * @param updateType 域升级方案类型
     * @param operationList 域升级的应用升级操作明细
     * @param deployApps 域已经部署的应用列表
     * @param appBkModuleList 已经部署的应用和蓝鲸模块的关系表
     * @param hostList 平台下所有服务器列表
     * @return 检查结果,如果为空则表示检查通过，否则返回检查失败原因
     */
    private String updateOperationCheck(DomainUpdateType updateType, List<AppUpdateOperationInfo> operationList, List<PlatformAppPo> deployApps, List<PlatformAppBkModulePo> appBkModuleList, List<LJHostInfo> hostList)
    {
        Map<String, LJHostInfo> hostMap = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity()));
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
            else if(!this.registerAppMap.containsKey(operationInfo.getAppName()))
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
                Map<String, AppModuleVo> versionMap = this.registerAppMap.get(operationInfo.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
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
        if(StringUtils.isBlank(updateSchema.getPlatformId()))
        {
            logger.error("platformId of schema is blank");
            throw new ParamException("platformId of schema is blank");
        }
        if(StringUtils.isBlank(updateSchema.getPlatformName()))
        {
            logger.error("platformName of schema is blank");
            throw new ParamException("platformName of schema is blank");
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
        List<DomainPo> domainList = this.domainMapper.select(updateSchema.getPlatformId(), null);
        logger.debug("begin check param of schema");
        checkPlatformUpdateSchema(updateSchema, domainList);
        logger.debug("schema param check success");
        List<PlatformAppDeployDetailVo> platformDeployApps = this.platformAppDeployDetailMapper.selectPlatformApps(updateSchema.getPlatformId(), null, null);
        List<LJHostInfo> bkHostList = this.paasService.queryBKHost(updateSchema.getBkBizId(), null, null, null, null);
        Map<String, DomainPo> domainMap = domainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = platformDeployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        this.appReadLock.readLock().lock();
        try
        {
            List<DomainUpdatePlanInfo> planList = new ArrayList<>();
            List<DomainUpdatePlanInfo> successList = new ArrayList<>();
            for(DomainUpdatePlanInfo plan : updateSchema.getDomainUpdatePlanList())
            {
                String domainId = plan.getDomainId();
                boolean isCreate = domainMap.containsKey(plan.getDomainId()) ? false : true;
                DomainPo domainPo = isCreate ? plan.getDomain(updateSchema.getPlatformId()) : domainMap.get(domainId);
                List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
                logger.debug(String.format("check %s %d apps with isCreate=%b and %d deployed apps",
                        JSONObject.toJSONString(domainPo), plan.getAppUpdateOperationList().size(), isCreate, domainAppList.size()));
                checkDomainApps(plan.getAppUpdateOperationList(), domainAppList, domainPo, isCreate, bkHostList);
                if(plan.getStatus().equals(UpdateStatus.SUCCESS))
                {
                    successList.add(plan);
                }
                else
                    planList.add(plan);
            }
            logger.debug(String.format("generate platform app deploy param and script"));
            generatePlatformDeployParamAndScript(updateSchema);
            if(updateSchema.getStatus().equals(UpdateStatus.SUCCESS) && planList.size() > 0)
            {
                logger.error(String.format("status of %s(%s) update schema is SUCCESS, but there are %d domain update plan not execute",
                        updateSchema.getPlatformName(), updateSchema.getPlatformId(), planList.size()));
                throw new ParamException(String.format("status of %s(%s) update schema is SUCCESS, but there are %d domain update plan not execute",
                        updateSchema.getPlatformName(), updateSchema.getPlatformId(), planList.size()));
            }
            logger.debug(String.format("generate domainId for new add with status SUCCESS and id is blank domain"));
            generateId4AddDomain(updateSchema.getPlatformId(), updateSchema.getDomainUpdatePlanList(), domainList);
            Map<String, Map<String, List<NexusAssetInfo>>> domainCfgMap = new HashMap<>();
            for(DomainUpdatePlanInfo plan : successList)
            {
                String domainId = plan.getDomainId();
                boolean isCreate = domainMap.containsKey(plan.getDomainId()) ? false : true;
                DomainPo domainPo = isCreate ? plan.getDomain(updateSchema.getPlatformId()) : domainMap.get(domainId);
                List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
                logger.debug(String.format("preprocess %s %d apps with isCreate=%b and %d deployed apps",
                        JSONObject.toJSONString(domainPo), plan.getAppUpdateOperationList().size(), isCreate, domainAppList.size()));
                Map<String, List<NexusAssetInfo>> cfgMap = preprocessDomainApps(updateSchema.getPlatformId(), plan.getAppUpdateOperationList(), domainAppList, domainPo, isCreate);
                domainCfgMap.put(domainId, cfgMap);
            }
            for(DomainUpdatePlanInfo plan : successList)
            {
                String domainId = plan.getDomainId();
                boolean isCreate = domainMap.containsKey(plan.getDomainId()) ? false : true;
                DomainPo domainPo = isCreate ? plan.getDomain(updateSchema.getPlatformId()) : domainMap.get(domainId);if(isCreate)
                    this.domainMapper.insert(domainPo);
                List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
                logger.debug(String.format("handle %s %d apps with isCreate=%b and %d deployed apps",
                        JSONObject.toJSONString(domainPo), plan.getAppUpdateOperationList().size(), isCreate, domainAppList.size()));
                handleDomainApps(updateSchema.getPlatformId(), domainPo, plan.getAppUpdateOperationList(), domainAppList, domainCfgMap.get(domainId), isCreate);
            }
            if(updateSchema.getStatus().equals(UpdateStatus.SUCCESS))
            {
                logger.debug(String.format("%s(%s) platform complete deployment, so remove schema and set status to RUNNING", updateSchema.getPlatformName(), updateSchema.getPlatformId()));
                if(this.platformUpdateSchemaMap.containsKey(updateSchema.getPlatformId()))
                    this.platformUpdateSchemaMap.remove(updateSchema.getPlatformId());
                this.platformUpdateSchemaMapper.delete(updateSchema.getPlatformId());
                platformPo.setStatus(CCODPlatformStatus.RUNNING.id);
                this.platformMapper.update(platformPo);
            }
            else
            {
                logger.debug(String.format("%s(%s) platform not complete deployment, so update schema and set status to PLANING", updateSchema.getPlatformName(), updateSchema.getPlatformId()));
                updateSchema.setDomainUpdatePlanList(planList);
                PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
                schemaPo.setContext(JSONObject.toJSONString(updateSchema).getBytes());
                schemaPo.setPlatformId(updateSchema.getPlatformId());
                this.platformUpdateSchemaMapper.delete(updateSchema.getPlatformId());
                this.platformUpdateSchemaMapper.insert(schemaPo);
                this.platformUpdateSchemaMap.put(updateSchema.getPlatformId(), updateSchema);
                platformPo.setStatus(CCODPlatformStatus.SCHEMA_CREATE_PLATFORM.id);
                this.platformMapper.update(platformPo);
            }
            if(successList.size() > 0)
            {
                logger.debug(String.format("%d domain complete deployment, so sync new platform topo to lj paas", successList.size()));
                this.paasService.syncClientCollectResultToPaas(platformPo.getBkBizId(), platformPo.getPlatformId(), platformPo.getBkCloudId());
            }
        }
        finally {
            this.appReadLock.readLock().unlock();
        }
    }

    /**
     * 为新加的且id为空的域生成域id
     * @param platformId 平台id
     * @param planList 所有的域升级任务
     * @param existDomainList 已经存在的域
     */
    private void generateId4AddDomain(String platformId, List<DomainUpdatePlanInfo> planList, List<DomainPo> existDomainList) throws ParamException
    {
        List<DomainPo> hasIdList = new ArrayList<>();
        hasIdList.addAll(existDomainList);
        List<DomainUpdatePlanInfo> notIdList = new ArrayList<>();
        for(DomainUpdatePlanInfo plan : planList)
        {
            if(plan.getUpdateType().equals(DomainUpdateType.ADD) && plan.getStatus().equals(UpdateStatus.SUCCESS))
            {
                if(StringUtils.isBlank(plan.getDomainId()))
                    notIdList.add(plan);
                else
                    hasIdList.add(plan.getDomain(platformId));
            }
        }
        Map<String, List<DomainUpdatePlanInfo>> notIdMap = notIdList.stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getBkSetName));
        Map<String, List<DomainPo>> hasIdMap = hasIdList.stream().collect(Collectors.groupingBy(DomainPo::getType));
        for(String bkSetName : notIdMap.keySet())
        {
            String standardDomainId = this.setDefineMap.get(bkSetName).getFixedDomainId();
            List<String> usedId = hasIdMap.containsKey(bkSetName) ? new ArrayList<>(hasIdMap.get(bkSetName).stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity())).keySet()) : new ArrayList<>();
            for(DomainUpdatePlanInfo plan : notIdMap.get(bkSetName))
            {
                String domainId = autoGenerateDomainId(standardDomainId, usedId);
                logger.debug(String.format("domain id of %s with bkSetName=%s is %s", plan.getDomainName(), bkSetName, domainId));
                plan.setDomainId(domainId);
                usedId.add(domainId);
            }
        }
    }
    /**
     * 检查平台升级相关参数
     * @param updateSchema 平台升级计划
     * @param existDomainList 该平台已经有的域
     * @throws ParamException 平台升级计划参数异常
     */
    private void checkPlatformUpdateSchema(PlatformUpdateSchemaInfo updateSchema, List<DomainPo> existDomainList) throws ParamException
    {
        StringBuffer sb = new StringBuffer();
        if(updateSchema.getTaskType() == null)
            sb.append("platform task type is null;");
        if(updateSchema.getStatus() == null)
            sb.append("platform task status is null;");
        if(updateSchema.getGlsDBType() == null)
            sb.append("gls database type is null;");
        else if(!updateSchema.getGlsDBType().equals(DatabaseType.ORACLE))
            sb.append(String.format("this version cmdb not support glsserver with database %s;", updateSchema.getGlsDBType().name));
        if(StringUtils.isBlank(updateSchema.getPlatformId()))
            sb.append("platformId of update schema is blank;");
        if(StringUtils.isBlank(updateSchema.getPlatformName()))
            sb.append("platformName of update schema is blank;");
        if(updateSchema.getBkBizId() <= 0)
            sb.append("bkBizId of update schema not define;");
        if(StringUtils.isBlank(updateSchema.getK8sHostIp()))
            sb.append("k8sHostIp of update schema is blank;");
        if(updateSchema.getGlsDBType() == null)
            sb.append("glsDBType of update schema is blank;");
        if(StringUtils.isBlank(updateSchema.getGlsDBUser()))
            sb.append("glsDbUser of update schema is blank;");
        if(StringUtils.isBlank(updateSchema.getGlsDBPwd()))
            sb.append("glsDBPwd of update schema is blank;");
        if(StringUtils.isBlank(updateSchema.getBaseDataNexusRepository()))
            sb.append("baseDataNexusRepository of update schema is blank;");
        if(StringUtils.isBlank(updateSchema.getBaseDataNexusPath()))
            sb.append("baseDataNexusPath of update schema is blank;");
        if(StringUtils.isNotBlank(sb.toString()))
        {
            logger.error(String.format("platform update schema check fail : %s", sb.toString()));
            throw new ParamException(String.format("platform update schema check fail : %s", sb.toString()));
        }
        for(DomainUpdatePlanInfo plan : updateSchema.getDomainUpdatePlanList())
        {
            if(plan.getUpdateType() == null)
                sb.append("domain update type is null;");
            if(plan.getStatus() == null)
                sb.append("domain update status is null;");
        }
        if(StringUtils.isNotBlank(sb.toString()))
        {
            logger.error(String.format("platform update schema check fail : %s", sb.toString()));
            throw new ParamException(String.format("platform update schema check fail : %s", sb.toString()));
        }
        List<DomainUpdatePlanInfo> hasIdList = new ArrayList<>();
        List<DomainUpdatePlanInfo> hasNameList = new ArrayList<>();
        Map<String, DomainPo> domainIdMap = existDomainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        Map<String, DomainPo> domainNameMap = existDomainList.stream().collect(Collectors.toMap(DomainPo::getDomainName, Function.identity()));
        for(DomainUpdatePlanInfo plan : updateSchema.getDomainUpdatePlanList())
        {
            switch (plan.getUpdateType())
            {
                case ADD:
                    if(StringUtils.isBlank(plan.getDomainName()))
                        sb.append("name of new add domain is blank;");
                    else if(domainNameMap.containsKey(plan.getDomainName()))
                        sb.append(String.format("name %s of new add domain has been used;", plan.getDomainName()));
                    else
                    {
                        if(StringUtils.isBlank(plan.getBkSetName()))
                            sb.append(String.format("bkSetName of new add domain %s is blank;", plan.getBkSetName()));
                        else if(!this.setDefineMap.containsKey(plan.getBkSetName()))
                            sb.append(String.format("set %s is of %s not support;", plan.getBkSetName(), plan.getDomainName()));
                        else if(StringUtils.isNotBlank(plan.getDomainId()))
                        {
                            String regex = String.format("^%s(0[1-9]|[1-9]\\d+)", this.setDefineMap.get(plan.getBkSetName()).getFixedDomainId());
                            if(!plan.getDomainId().matches(regex))
                                sb.append(String.format("predefine domainId %s of new add domain %s is illegal;", plan.getDomainId(), plan.getBkSetName()));
                            else if(domainIdMap.containsKey(plan.getDomainId()))
                                sb.append(String.format("domainId %s has been used;", plan.getDomainId()));
                        }
                        if(StringUtils.isBlank(plan.getTags()))
                            sb.append(String.format("tag of new add domain %s is blank;", plan.getBkSetName()));
                        if(plan.getAppUpdateOperationList() == null || plan.getAppUpdateOperationList().size() == 0)
                            sb.append(String.format("app of new add domain %s is 0;", plan.getBkSetName()));
                        if(plan.getOccurs() == 0)
                            sb.append(String.format("occurs of new add domain %s is 0;", plan.getBkSetName()));
                        if(plan.getMaxOccurs() == 0)
                            sb.append(String.format("maxOccurs of new add domain %s is 0;", plan.getBkSetName()));
                        hasNameList.add(plan);
                        if(StringUtils.isNotBlank(plan.getDomainId()))
                            hasIdList.add(plan);
                    }
                    break;
                case UPDATE:
                    if(StringUtils.isBlank(plan.getDomainId()))
                        sb.append(String.format("domainId of update domain is blank;"));
                    else if(!domainIdMap.containsKey(plan.getDomainId()))
                        sb.append(String.format("domain %s of update domain not exist;", plan.getDomainId()));
                    else if(plan.getAppUpdateOperationList() == null || plan.getAppUpdateOperationList().size() == 0)
                        sb.append(String.format("update app count of %s is 0;", plan.getDomainId()));
                    hasIdList.add(plan);
                    break;
                case DELETE:
                    if(StringUtils.isBlank(plan.getDomainId()))
                        sb.append(String.format("domainId of delete domain is blank;"));
                    else if(!domainIdMap.containsKey(plan.getDomainId()))
                        sb.append(String.format("domain %s of delete domain not exist;", plan.getDomainId()));
                    hasIdList.add(plan);
                    break;
                default:
                    sb.append(String.format("domain update type %s is not support now;", plan.getUpdateType().name));
                    break;
            }
        }
        if(StringUtils.isNotBlank(sb.toString()))
        {
            logger.error(String.format("domain update plan of platform schema is checked fail : %s", sb.toString().replaceAll(";$", "")));
            throw new ParamException(sb.toString().replaceAll(";$", ""));
        }
        Map<String, List<DomainUpdatePlanInfo>> idMap = hasIdList.stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getDomainId));
        for(String domainId : idMap.keySet())
        {
            if(idMap.get(domainId).size() > 1)
                sb.append(String.format("domainId %s update plan  is not unique;", domainId));
        }
        Map<String, List<DomainUpdatePlanInfo>> nameMap = hasNameList.stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getDomainName));
        for(String domainName : nameMap.keySet())
        {
            if(nameMap.get(domainName).size() > 1)
                sb.append(String.format("domainName %s update plan  is not unique;", domainName));
        }
        if(StringUtils.isNotBlank(sb.toString()))
        {
            logger.error(String.format("id or name of domain plan of platform schema is checked fail : %s", sb.toString().replaceAll(";$", "")));
            throw new ParamException(sb.toString().replaceAll(";$", ""));
        }
    }

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
        for(PlatformAppDeployDetailVo deployApp : deployApps)
        {
            if(!this.appSetRelationMap.containsKey(deployApp.getAppName()))
            {
                logger.error(String.format("%s没有在配置文件的lj-paas.set-apps节点中定义", deployApp.getAppName()));
                throw new NotSupportAppException(String.format("%s未定义所属的set信息", deployApp.getAppName()));
            }
            deployApp.setBkBizId(bizId);
            BizSetDefine sd = this.appSetRelationMap.get(deployApp.getAppName()).get(0);
            deployApp.setBkSetName(sd.getName());
            if(StringUtils.isNotBlank(sd.getFixedDomainName()))
            {
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
        Map<String, List<PlatformAppDeployDetailVo>> setAppMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getBkSetName));
        List<CCODSetInfo> setList = new ArrayList<>();
        for(PlatformAppDeployDetailVo deployApp : deployAppList)
        {
            if(!this.appSetRelationMap.containsKey(deployApp.getAppName()))
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
        Map<String, BizSetDefine> bizSetMap = this.ccodBiz.getSet().stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        List<PlatformAppDeployDetailVo> deployAppList;
        List<UnconfirmedAppModulePo> unconfirmedAppModuleList = null;
        switch (topology.getStatus())
        {
//            case SCHEMA_CREATE_PLATFORM:
//                setList = new ArrayList<>();
//                idleHostList = this.paasService.queryBizIdleHost(platform.getBkBizId());
//                schema = this.platformUpdateSchemaMap.containsKey(platformId) ? this.platformUpdateSchemaMap.get(platformId) : null;
//                break;
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
                unconfirmedAppModuleList = unconfirmedAppModuleMapper.select(platformId);
                break;
            case SCHEMA_CREATE_PLATFORM:
            case SCHEMA_UPDATE_PLATFORM:
            case PLANING:
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
        topology.setUnconfirmedAppModuleList(unconfirmedAppModuleList);
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
            AppPo appPo = new AppPo(appModule);
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
            String directory = appModule.getAppNexusDirectory();
            if (!this.registerAppMap.containsKey(appName) || !this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version))
            {
                logger.error(String.format("%s version %s has not registered", appName, version));
                throw new ParamException(String.format("%s version %s has not registered", appName, version));
            }
            List<AppFilePo> fileList = new ArrayList<>();
            fileList.add(appModule.getInstallPackage());
            for(AppCfgFilePo cfg : appModule.getCfgs())
                fileList.add(cfg);
            logger.debug(String.format("clear all file at %s", directory));
            this.nexusService.clearComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, this.appRepository, directory);
            logger.debug(String.format("download package and cfgs and upload to %s", directory));
            Map<String, NexusAssetInfo> fileMap = downloadAndUploadAppFiles(appModule.getAppId(), this.nexusHostUrl, this.nexusUserName, this.nexusPassword, fileList, this.appRepository, directory)
                    .stream().collect(Collectors.toMap(NexusAssetInfo::getNexusAssetFileName, Function.identity()));
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
     * @param appId 应用id
     * @param srcNexusHostUrl 源nexus的url地址
     * @param srcNexusUser 源nexus的登录用户
     * @param srcPwd 源nexus
     * @param srcFileList 需要下载并上传文件列表
     * @param dstRepository 上传的目的仓库
     * @param dstDirectory 上传路径
     * @return
     */
    protected List<NexusAssetInfo> downloadAndUploadAppFiles(int appId, String srcNexusHostUrl, String srcNexusUser, String srcPwd, List<AppFilePo> srcFileList, String dstRepository, String dstDirectory) throws ParamException, InterfaceCallException, NexusException, IOException
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

    @Override
    public PlatformUpdateSchemaInfo createNewPlatform(PlatformCreateParamVo paramVo) throws ParamException, NotSupportSetException, NotSupportAppException, InterfaceCallException, LJPaasException {
        logger.debug(String.format("prepare to create new platform : %s", JSONObject.toJSONString(paramVo)));
        String paramCheckResult = checkPlatformCreateParam(paramVo);
        if(StringUtils.isNotBlank(paramCheckResult))
        {
            logger.error(String.format("check param of create platform fail : %s", paramCheckResult));
            throw new ParamException(String.format("create platform fail : %s", paramCheckResult));
        }
        if(!paramVo.getGlsDBType().equals(DatabaseType.ORACLE))
        {
            logger.error(String.format("this version cmdb not support glsserver with database %s", paramVo.getGlsDBType().name));
            throw new ParamException(String.format("this version cmdb not support glsserver with database %s", paramVo.getGlsDBType().name));
        }
        PlatformUpdateSchemaInfo schemaInfo;
        this.appReadLock.readLock().lock();
        try
        {
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
            List<BizSetDefine> setDefineList = this.ccodBiz.getSet();
            makeupPlatform4CreateSchema(schemaInfo, setDefineList);
            schemaInfo.setK8sHostIp(paramVo.getK8sHostIp());
            schemaInfo.setGlsDBType(paramVo.getGlsDBType());
            schemaInfo.setGlsDBUser(paramVo.getGlsDBUser());
            schemaInfo.setGlsDBPwd(paramVo.getGlsDBPwd());
            schemaInfo.setBaseDataNexusPath(paramVo.getBaseDataNexusPath());
            schemaInfo.setBaseDataNexusRepository(paramVo.getBaseDataNexusRepository());
            schemaInfo.setSchemaId(paramVo.getSchemaId());
            String pbCfgData = "[{\"deployPath\":\"/root/resin-4.0.13/conf\",\"fileName\":\"local_datasource.xml\",\"fileSize\":0,\"md5\":\"e9c26f00f17a7660bfa3f785c4fe34be\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFhN2U5NGFhNDVlZTIxN2Nm\",\"nexusPath\":\"/configText/123456-wuph/publicConfig/local_datasource.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"/root/resin-4.0.13/conf\",\"fileName\":\"local_jvm.xml\",\"fileSize\":0,\"md5\":\"d41d8cd98f00b204e9800998ecf8427e\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNkODFhZTYxM2NmNDM3NmQ3\",\"nexusPath\":\"/configText/123456-wuph/publicConfig/local_jvm.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"/usr/local/lib\",\"fileName\":\"tnsnames.ora\",\"fileSize\":0,\"md5\":\"d41d8cd98f00b204e9800998ecf8427e\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQzMDcyY2Q2ZTEyNzg2NTQ3\",\"nexusPath\":\"/configText/123456-wuph/publicConfig/tnsnames.ora\",\"nexusRepository\":\"tmp\"}]";
            schemaInfo.setPublicConfig(JSONArray.parseArray(pbCfgData, AppFileNexusInfo.class));
            return schemaInfo;
        }
        finally {
            this.appReadLock.readLock().unlock();
        }
    }

    private String checkPlatformCreateParam(PlatformCreateParamVo param)
    {
        StringBuffer sb = new StringBuffer();
        if(StringUtils.isBlank(param.getSchemaId()))
        {
            sb.append("schemaId is blank;");
        }
        if(StringUtils.isBlank(param.getPlatformId()))
        {
            sb.append("platformId is blank;");
        }
        if(StringUtils.isBlank(param.getPlatformName()))
        {
            sb.append("platformName is blank;");
        }
        if(param.getBkBizId() == 0)
        {
            sb.append("bizId of platform not define;");
        }
        if(StringUtils.isBlank(param.getK8sHostIp()))
        {
            sb.append("k8sHostIp is blank;");
        }
        if(param.getGlsDBType() == null)
        {
            sb.append("glsDBType is blank;");
        }
        if(StringUtils.isBlank(param.getGlsDBUser()))
        {
            sb.append("glsDBPwd is blank;");
        }
        if(StringUtils.isBlank(param.getGlsDBPwd()))
        {
            sb.append("glsDBPwd is blank");
        }
        if(StringUtils.isBlank(param.getBaseDataNexusRepository()))
        {
            sb.append("baseDataNexusRepository is blank;");
        }
        if(StringUtils.isBlank(param.getBaseDataNexusPath()))
        {
            sb.append("baseDataNexusPath is blank;");
        }
        return sb.toString().replaceAll(";$", "");
    }

    @Override
    public PlatformUpdateSchemaInfo createDemoNewPlatform(String platformId, String platformName, int bkBizId, int bkCloudId, List<String> planAppList) throws ParamException, InterfaceCallException, LJPaasException
    {
        logger.debug(String.format("begin to create demo platform:id=%s,name=%s,bizId=%d,cloudId=%s and planApps=%s",
                platformId, platformName, bkBizId, bkCloudId, String.join("|", planAppList)));
        this.appReadLock.readLock().lock();
        try
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
            PlatformUpdateSchemaInfo schema = new PlatformUpdateSchemaInfo(platformId, platformName, bkBizId, bkCloudId, "CCOD4.1",
                    PlatformUpdateTaskType.CREATE, String.format("%s(%s)平台新建计划", platformName, platformId),
                    String.format("通过程序自动创建的%s(%s)平台新建计划", platformName, platformId));
            Set<String> ipSet = new HashSet<>();
            for(BizSetDefine setDefine : this.setDefineMap.values())
            {
                if(setDefine.getApps().size() == 0)
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
                for(AppDefine appDefine : setDefine.getApps())
                {
                    String appName = appDefine.getName();
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
                            Map<String, AppModuleVo> versionMap = this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
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
            String checkResult = checkPlatformUpdateTask(schema, new ArrayList<>(), new ArrayList<>(), setList, idleHostList, new ArrayList<>());
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
        finally {
            this.appReadLock.readLock().unlock();
        }
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
     * 为平台创建计划的新建域自动生成域id，为域部署的应用自动生成应用别名
     * @param schema 原始平台升级计划
     * @param existDomainList 平台已经存在的域
     * @param existAppList 平台已经部署的应用
     * @throws ParamException 输入参数有误
     * @throws NotSupportAppException
     * @throws NotSupportSetException
     */
    void makeupPlatformUpdateSchema(PlatformUpdateSchemaInfo schema, List<DomainPo> existDomainList, List<PlatformAppDeployDetailVo> existAppList) throws ParamException, NotSupportAppException, NotSupportSetException
    {
        List<DomainUpdatePlanInfo> noDomainIdPlans = new ArrayList<>();
        Map<String, List<String>> setDomainIdMap = new HashMap<>();
        for(DomainUpdatePlanInfo planInfo : schema.getDomainUpdatePlanList())
        {
            if(!this.setDefineMap.containsKey(planInfo.getBkSetName()))
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
            Map<String, AppDefine> appDefineMap = setDefine.getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity()));
            for(AppUpdateOperationInfo opt : planInfo.getAppUpdateOperationList())
            {
                if(!appDefineMap.containsKey(opt.getAppName()))
                {
                    logger.error(String.format("set %s does not support %s", planInfo.getBkSetName(), opt.getAppName()));
                    throw new ParamException(String.format("set %s does not support %s", planInfo.getBkSetName(), opt.getAppName()));
                }
                if(StringUtils.isNotBlank(opt.getAppAlias()))
                {
                    String standAlias = appDefineMap.get(opt.getAppName()).getAlias();
                    String regex = String.format("^%s($|[0-9]\\d*$)", standAlias);
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
                if(!appDefineMap.containsKey(appName))
                {
                    logger.error(String.format("set %s not support app %s", planInfo.getBkSetName(), appName));
                    throw new NotSupportAppException(String.format("set %s not support app %s", planInfo.getBkSetName(), appName));
                }
                String standardAlias = appDefineMap.get(appName).getAlias();
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
        for(BizSetDefine setDefine : this.ccodBiz.getSet())
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

    private String generateId4Domain(DomainPo domainPo, List<DomainPo> existDomain)
    {
        return null;
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
            else if(index == 0 && !standardAlias.equals("ucgateway"))
            {
                index = 1;
            }

        }
        index++;
        String appAlias = String.format("%s%s", standardAlias, index);
        return appAlias;
    }

    /**
     * 从已有的平台创建一个新的平台
     * @param clonedPlatformId 被克隆的平台id
     * @param platformId 平台id
     * @param platformName 平台名
     * @param bkBizId 平台在蓝鲸paas的biz id
     * @param bkCloudId 平台服务器所在的机房id
     * @return 创建的平台
     * @throws ParamException 计划的参数异常
     * @throws InterfaceCallException 处理计划时调用蓝鲸api或是nexus api失败
     * @throws LJPaasException 调用蓝鲸api返回调用失败或是解析蓝鲸api结果失败
     */
    private PlatformUpdateSchemaInfo cloneExistPlatform(String clonedPlatformId, String platformId, String platformName, int bkBizId, int bkCloudId) throws ParamException, NotSupportSetException, NotSupportAppException, InterfaceCallException, LJPaasException {
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
                            deployApp.getBkSetName());
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
        makeupPlatformUpdateSchema(schema, new ArrayList<>(), new ArrayList<>());
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

    private DomainUpdatePlanInfo generateCloneExistDomain(DomainPo clonedDomain, String setName)
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
        this.unconfirmedAppModuleMapper.delete(platformId);
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
        params.put("k8s_host_ip", schemaInfo.getK8sHostIp());
        params.put("gls_db_type", schemaInfo.getGlsDBType().name);
        params.put("gls_db_user", schemaInfo.getGlsDBUser());
        params.put("gls_db_pwd", schemaInfo.getGlsDBPwd());
        params.put("gls_db_sid", this.glsOracleSid);
        if(schemaInfo.getGlsDBType().equals(DatabaseType.ORACLE))
        {
            params.put("gls_db_svc_name", this.glsOracleSvcName);
        }
        params.put("platform_id", schemaInfo.getPlatformId());
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
        Map<String, AppDefine> map = setDefine.getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity()));
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
            for(int i= setDefine.getApps().size() - 1; i >=0; i--)
            {
                String appName = setDefine.getApps().get(i).getName();
                if(appDelMap.containsKey(appName))
                {
                    final String standardAlias = map.get(appName).getAlias();
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
            for(AppDefine appDefine : setDefine.getApps())
            {
                String appName = appDefine.getName();
                if(appAddMap.containsKey(appName))
                {
                    final String standardAlias = map.get(appName).getAlias();
                    List<AppUpdateOperationInfo> appOptList = appAddMap.get(appName);
                    for(AppUpdateOperationInfo opt : appOptList)
                    {
                        opt.setAddDelay(map.get(appName).getDelay());
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
                define.setDomainType(setDefine.getDomainType());
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
    public List<PlatformAppPo> updatePlatformApps(String platformId, String platformName, List<AppUpdateOperationInfo> appList) throws NotSupportAppException, ParamException, InterfaceCallException, NexusException, LJPaasException, IOException {
        logger.debug(String.format("begin to update %d apps of %s(%s)", appList.size(), platformName, platformId));
        this.appReadLock.readLock().lock();
        try {
            PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
            if(platformPo == null)
            {
                logger.error(String.format("%s platform not exist", platformId));
                throw new ParamException(String.format("%s platform not exist", platformId));
            }
            if(!platformName.equals(platformPo.getPlatformName()))
            {
                logger.error(String.format("name of %s is %s not %s", platformId, platformPo.getPlatformName(), platformName));
                throw new ParamException(String.format("name of %s is %s not %s", platformId, platformPo.getPlatformName(), platformName));
            }
            List<DomainPo> domainList = this.domainMapper.select(platformId, null);
            Map<String, DomainPo> domainMap = domainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
            Map<String, List<AppUpdateOperationInfo>> domainOptMap = appList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getDomainId));
            List<PlatformAppDeployDetailVo> deployApps = this.platformAppDeployDetailMapper.selectPlatformApps(platformId, null, null);
            Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = deployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
            List<LJHostInfo> hostList = this.paasService.queryBKHost(platformPo.getBkBizId(), null, null, null, null);
            for(String domainId : domainOptMap.keySet())
            {
                if(!domainMap.containsKey(domainId))
                {
                    logger.error(String.format("domain %s not exist", domainId));
                    throw new ParamException(String.format("domain %s not exist", domainId));
                }
                List<PlatformAppDeployDetailVo> deployAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
                checkDomainApps(domainOptMap.get(domainId), deployAppList, domainMap.get(domainId), false, hostList);
            }
            Map<String, Map<String, List<NexusAssetInfo>>> domainCfgMap = new HashMap<>();
            for(String domainId : domainOptMap.keySet())
            {
                List<PlatformAppDeployDetailVo> deployAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
                Map<String, List<NexusAssetInfo>> appCfgMap = preprocessDomainApps(platformId, domainOptMap.get(domainId), deployAppList, domainMap.get(domainId), false);
                domainCfgMap.put(domainId, appCfgMap);
            }
            for(String domainId : domainOptMap.keySet())
            {
                List<PlatformAppDeployDetailVo> deployAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
                handleDomainApps(platformId, domainMap.get(domainId), domainOptMap.get(domainId), deployAppList, domainCfgMap.get(domainId), false);
            }
            this.paasService.syncClientCollectResultToPaas(platformPo.getBkBizId(), platformId, platformPo.getBkCloudId());
        }
        finally {
            this.appReadLock.readLock().unlock();
        }
        return new ArrayList<>();
    }

    private void generateAlias4AddApps(List<AppUpdateOperationInfo> addOptList, List<PlatformAppDeployDetailVo> deployApps, List<DomainPo> domainList) throws ParamException
    {
        Map<String, DomainPo> domainMap = domainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        Map<String, List<AppUpdateOperationInfo>> domainOptMap = addOptList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getDomainId));
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = deployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        for(String domainId : domainOptMap.keySet())
        {
            Map<String, List<AppUpdateOperationInfo>> addAppMap = domainOptMap.get(domainId).stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
            for(String appName : addAppMap.keySet())
            {
                List<String> usedAlias = new ArrayList<>();
                String standAlias = this.setDefineMap.get(domainMap.get(domainId).getType()).getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity())).get(appName).getAlias();
                if(domainAppMap.containsKey(domainId) && domainAppMap.get(domainId).stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getAppName)).containsKey(appName))
                {
                    usedAlias = new ArrayList<>(domainAppMap.get(domainId).stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getAppName)).get(appName).stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAppAlias, Function.identity())).keySet());
                }
                boolean onlyOne = false;
                if(usedAlias.size() == 0 && addAppMap.get(appName).size() == 1)
                    onlyOne = true;
                for(AppUpdateOperationInfo optInfo : addAppMap.get(appName))
                {
                    String alias = autoGenerateAlias(standAlias, usedAlias, onlyOne);
                    optInfo.setAppAlias(alias);
                    if(StringUtils.isBlank(optInfo.getOriginalAlias()))
                        optInfo.setOriginalAlias(alias);
                    usedAlias.add(alias);
                }
            }
        }
    }

    /**
     * 为域新加的应用自动生成应用别名
     * @param domainPo 添加应用的域
     * @param addOptList 该域所有被添加的应用
     * @param deployAppList 该域已经部署的应用列表
     * @param isCreate 该域是否为新建域
     */
    private void generateAlias4DomainApps(DomainPo domainPo, List<AppUpdateOperationInfo> addOptList, List<PlatformAppDeployDetailVo> deployAppList, boolean isCreate) throws ParamException
    {
        Map<String, List<AppUpdateOperationInfo>> addAppMap = addOptList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getAppName));
        String domainId = domainPo.getDomainId();
        for(String appName : addAppMap.keySet())
        {
            List<String> usedAlias = new ArrayList<>();
            String standAlias = this.setDefineMap.get(domainPo.getType()).getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity())).get(appName).getAlias();
            if(domainAppMap.containsKey(appName)
                    && domainAppMap.get(appName).stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getAppName)).containsKey(appName))
            {
                usedAlias = new ArrayList<>(domainAppMap.get(appName).stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAppAlias, Function.identity())).keySet());
            }
            List<AppUpdateOperationInfo> needAliasList = new ArrayList<>();
            for(AppUpdateOperationInfo optInfo : addAppMap.get(appName))
            {
                if(isCreate && StringUtils.isNotBlank(optInfo.getAppAlias()))
                    usedAlias.add(optInfo.getAppAlias());
                else
                {
                    needAliasList.add(optInfo);
                    if(!isCreate && StringUtils.isBlank(optInfo.getOriginalAlias()))
                        optInfo.setOriginalAlias(optInfo.getAppAlias());
                }

            }
            boolean onlyOne = false;
            if(usedAlias.size() == 0 && needAliasList.size() == 1)
                onlyOne = true;
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

    /**
     * 检查一个域的操作是否合法
     * @param domainOptList 对指定域的所有操作
     * @param domainAppList 指定域已经部署的所有应用
     * @param domainPo 指定的域信息
     * @param isCreate 指定域是否是新建域
     * @param hostList 该域所属平台的服务器信息
     * @throws ParamException
     * @throws NotSupportAppException
     */
    private void checkDomainApps(List<AppUpdateOperationInfo> domainOptList, List<PlatformAppDeployDetailVo> domainAppList, DomainPo domainPo, boolean isCreate, List<LJHostInfo> hostList) throws ParamException, NotSupportAppException
    {
        Map<String, LJHostInfo> hostMap = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity()));
        String domainId = domainPo.getDomainId();
        for(AppUpdateOperationInfo optInfo : domainOptList)
        {
            String appName = optInfo.getAppName();
            String alias = optInfo.getAppAlias();
            String version = optInfo.getTargetVersion();
            String hostIp = optInfo.getHostIp();
            String originalAlias = optInfo.getOriginalAlias();
            String tag = String.format("UPDATE %s(%s) to %s in %s at %s", alias, appName, version, domainId, hostIp);
            switch (optInfo.getOperation())
            {
                case UPDATE:
                    if(StringUtils.isBlank(appName))
                    {
                        logger.error(String.format("%s FAIL : appName is blank", tag));
                        throw new ParamException(String.format("%s FAIL : appName is blank", tag));
                    }
                    else if(StringUtils.isBlank(version))
                    {
                        logger.error(String.format("%s FAIL : version is blank", tag));
                        throw new ParamException(String.format("%s FAIL : version is blank", tag));
                    }
                    else if(!this.registerAppMap.containsKey(appName) || !this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version))
                    {
                        logger.error(String.format("%s FAIL : version %s not register", tag, version));
                        throw new ParamException(String.format("%s FAIL : version %s not register", tag, version));
                    }
                    if(StringUtils.isBlank(alias))
                    {
                        logger.error(String.format("%s FAIL : appAlias is blank", tag));
                        throw new ParamException(String.format("%s FAIL : appAlias is blank", tag));
                    }
                    else if(!domainAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).containsKey(alias))
                    {
                        logger.error(String.format("%s FAIL : %s not exist", tag, alias));
                        throw new ParamException(String.format("%s FAIL : %s not exist", tag, alias));
                    }
                    if(StringUtils.isBlank(hostIp))
                    {
                        logger.error(String.format("%s FAIL : hostIp is blank", tag));
                        throw new ParamException(String.format("%s FAIL : hostIp is blank", tag));
                    }
                    else if(!hostMap.containsKey(hostIp))
                    {
                        logger.error(String.format("%s FAIL : host %s not exist", tag, hostIp));
                        throw new ParamException(String.format("%s FAIL : host %s not exist", tag, hostIp));
                    }
                    if(!this.setDefineMap.get(domainPo.getType()).getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity())).containsKey(appName))
                    {
                        logger.error(String.format("%s not support %s", domainPo.getType(), appName));
                        throw new NotSupportAppException(String.format("%s not support %s", domainPo.getType(), appName));
                    }
                    if(optInfo.getCfgs() == null || optInfo.getCfgs().size() == 0)
                    {
                        logger.error(String.format("%s FAIL : cfg is blank", tag));
                        throw new ParamException(String.format("%s FAIL : cfg is blank", tag));
                    }
                    break;
                case ADD:
                    tag = String.format("ADD %s[%s(%s)] in %s at %s", originalAlias, appName, version, domainId, hostIp);
                    if(StringUtils.isBlank(appName))
                    {
                        logger.error(String.format("%s FAIL : appName is blank", tag));
                        throw new ParamException(String.format("%s FAIL : appName is blank", tag));
                    }
                    else if(StringUtils.isBlank(version))
                    {
                        logger.error(String.format("%s FAIL : version is blank", tag));
                        throw new ParamException(String.format("%s FAIL : version is blank", tag));
                    }
                    else if(!this.registerAppMap.containsKey(appName) || !this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version))
                    {
                        logger.error(String.format("%s FAIL : version %s not register", tag, version));
                        throw new ParamException(String.format("%s FAIL : version %s not register", tag, version));
                    }
                    if(!isCreate && StringUtils.isBlank(alias))
                    {
                        logger.error(String.format("%s FAIL : appAlias is blank", tag));
                        throw new ParamException(String.format("%s FAIL : appAlias is blank", tag));
                    }
                    else if(StringUtils.isNotBlank(originalAlias)
                            && domainAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).containsKey(originalAlias))
                    {
                        logger.error(String.format("%s FAIL : %s has exist", tag, originalAlias));
                        throw new ParamException(String.format("%s FAIL : %s has exist", tag, originalAlias));
                    }
                    if(StringUtils.isBlank(hostIp))
                    {
                        logger.error(String.format("%s FAIL : hostIp is blank", tag));
                        throw new ParamException(String.format("%s FAIL : hostIp is blank", tag));
                    }
                    else if(!hostMap.containsKey(hostIp))
                    {
                        logger.error(String.format("%s FAIL : host %s not exist", tag, hostIp));
                        throw new ParamException(String.format(String.format("%s FAIL : host %s not exist", tag, hostIp)));
                    }
                    if(!this.setDefineMap.get(domainPo.getType()).getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity())).containsKey(appName))
                    {
                        logger.error(String.format("%s not support %s", domainPo.getType(), appName));
                        throw new NotSupportAppException(String.format("%s not support %s", domainPo.getType(), appName));
                    }
                    if(optInfo.getCfgs() == null || optInfo.getCfgs().size() == 0)
                    {
                        logger.error(String.format("%s FAIL : cfg is blank", tag));
                        throw new ParamException(String.format("%s FAIL : cfg is blank", tag));
                    }
                    break;
                case DELETE:
                    tag = String.format("DELETE %s in %s", alias, domainId);
                    if(StringUtils.isBlank(alias))
                    {
                        logger.error(String.format("%s FAIL : appAlias is blank", tag));
                        throw new ParamException(String.format("%s FAIL : appAlias is blank", tag));
                    }
                    else if(!domainAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAppAlias, Function.identity())).containsKey(alias))
                    {
                        logger.error(String.format("%s FAIL : %s not exist", tag, alias));
                        throw new ParamException(String.format("%s FAIL : %s not exist", tag, alias));
                    }
                    break;
                default:
                    logger.error(String.format("%s operation not support", optInfo.getOperation().name));
                    throw new ParamException(String.format("%s operation not support", optInfo.getOperation().name));
            }
            logger.debug(String.format("%s is ok", tag));
//            List<AppFileNexusInfo> cfgs = new ArrayList<>();
//            for(AppCfgFilePo cfg : this.registerAppMap.get(optInfo.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(optInfo.getTargetVersion()).getCfgs())
//            {
//                AppFileNexusInfo cfgFilePo = new AppFileNexusInfo();
//                cfgFilePo.setNexusRepository(cfg.getNexusRepository());
//                cfgFilePo.setNexusPath(String.format("%s/%s", cfg.getNexusDirectory(), cfg.getFileName()));
//                cfgFilePo.setNexusAssetId(cfg.getNexusAssetId());
//                cfgFilePo.setMd5(cfg.getMd5());
//                cfgFilePo.setFileSize((long)0);
//                cfgFilePo.setFileName(cfg.getFileName());
//                cfgFilePo.setExt(cfg.getExt());
//                cfgFilePo.setDeployPath(cfg.getDeployPath());
//                cfgs.add(cfgFilePo);
//            }
//            optInfo.setCfgs(cfgs);
        }
    }

    /**
     * 处理域的应用
     * @param platformId 平台id
     * @param domainOptList 指定需要增、删、修改的应用
     * @param domainAppList 域已经部署的所有应用
     * @param nexusAssetMap
     */
    private void handleDomainApps(String platformId, DomainPo domainPo, List<AppUpdateOperationInfo> domainOptList, List<PlatformAppDeployDetailVo> domainAppList, Map<String, List<NexusAssetInfo>> nexusAssetMap, boolean isCreate)
    {
        String domainId = domainPo.getDomainId();
        Map<AppUpdateOperation, List<AppUpdateOperationInfo>> optMap = domainOptList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getOperation));
        List<AppUpdateOperationInfo> updateList = optMap.containsKey(AppUpdateOperation.UPDATE) ? optMap.get(AppUpdateOperation.UPDATE) : new ArrayList<>();
        List<AppUpdateOperationInfo> addList = optMap.containsKey(AppUpdateOperation.ADD) ? optMap.get(AppUpdateOperation.ADD) : new ArrayList<>();
        List<AppUpdateOperationInfo> deleteList = optMap.containsKey(AppUpdateOperation.DELETE) ? optMap.get(AppUpdateOperation.DELETE) : new ArrayList<>();
        for(AppUpdateOperationInfo optInfo : deleteList)
        {
            String alias = optInfo.getAppAlias();
            String tag = String.format("DELETE %s in %s", alias, domainId);
            logger.debug(String.format("begin to %s", tag));
            int platformAppId = domainAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAppAlias, Function.identity())).get(alias).getPlatformAppId();
            logger.debug(String.format("delete platform_app_bk_module with platformAppId=%d", platformAppId));
            this.platformAppBkModuleMapper.delete(platformAppId, null, null, null);
            logger.debug(String.format("delete platform_app_cfg_file with platformAppId=%d", platformAppId));
            this.platformAppCfgFileMapper.delete(null, platformAppId);
            logger.debug(String.format("delete platform_app with platformAppId=%d", platformAppId));
            this.platformAppMapper.delete(platformAppId, null, null);
            logger.info(String.format("%s success", tag));
        }
        for(AppUpdateOperationInfo optInfo : updateList)
        {
            String appName = optInfo.getAppName();
            String alias = optInfo.getAppAlias();
            String version = optInfo.getTargetVersion();
            String hostIp = optInfo.getHostIp();
            String tag = String.format("UPDATE %s(%s) to %s in %s at %s", alias, appName, version, domainId, hostIp);
            logger.debug(String.format("begin to %s", tag));
            AppModuleVo module = this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version);
            int platformAppId = domainAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).get(alias).getPlatformApp().getPlatformAppId();
            PlatformAppPo platformAppPo = optInfo.getPlatformApp(platformAppId, module.getAppId(), platformId, domainId);
            List<NexusAssetInfo> assetList = nexusAssetMap.get(alias);
            Map<String, AppFileNexusInfo> cfgMap = optInfo.getCfgs().stream().collect(Collectors.toMap(AppFileNexusInfo::getFileName, Function.identity()));
            logger.debug(String.format("update id=%d platform_app from %d to %d", platformAppId, platformAppPo.getAppId(), module.getAppId()));
            this.platformAppMapper.update(platformAppPo);
            logger.debug(String.format("delete from platform_app_cfg_file where platformAppId=%d", platformAppId));
            this.platformAppCfgFileMapper.delete(null, platformAppId);
            for(NexusAssetInfo cfg : assetList)
            {
                PlatformAppCfgFilePo cfgFilePo = new PlatformAppCfgFilePo(platformAppId, module.getAppId(), cfgMap.get(cfg.getNexusAssetFileName()).getDeployPath(), cfg);
                logger.debug(String.format("insert cfg[%s]", JSONObject.toJSONString(cfgFilePo)));
                this.platformAppCfgFileMapper.insert(cfgFilePo);
            }
            logger.info(String.format("%s success", tag));
        }
        for(AppUpdateOperationInfo optInfo : addList)
        {
            String appName = optInfo.getAppName();
            String alias = optInfo.getAppAlias();
            String version = optInfo.getTargetVersion();
            String hostIp = optInfo.getHostIp();
            String tag = String.format("ADD %s(%s) to %s in %s at %s", alias, appName, version, domainId, hostIp);
            logger.debug(String.format("begin to %s", tag));
            AppModuleVo module = this.registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version);
            PlatformAppPo platformAppPo = optInfo.getPlatformApp(module.getAppId(), platformId, domainId);
            this.platformAppMapper.insert(platformAppPo);
            int platformAppId = platformAppPo.getPlatformAppId();
            List<NexusAssetInfo> assetList = nexusAssetMap.get(alias);
            Map<String, AppFileNexusInfo> cfgMap = optInfo.getCfgs().stream().collect(Collectors.toMap(AppFileNexusInfo::getFileName, Function.identity()));
            for(NexusAssetInfo cfg : assetList)
            {
                PlatformAppCfgFilePo cfgFilePo = new PlatformAppCfgFilePo(platformAppId, module.getAppId(), cfgMap.get(cfg.getNexusAssetFileName()).getDeployPath(), cfg);
                logger.debug(String.format("insert cfg[%s]", JSONObject.toJSONString(cfgFilePo)));
                this.platformAppCfgFileMapper.insert(cfgFilePo);
            }
            logger.info(String.format("%s success", tag));
        }
    }

    /**
     * 为指定域新加的应用生成别名，并且下载添加/更新的应用的配置文件到指定nexus路径
     * @param platformId 平台id
     * @param domainOptList 指定域的应用的相关操作
     * @param deployAppList 指定域已经部署的所有应用
     * @param domainPo 指定域
     * @param isCreate 该域是不是新建
     * @return 添加/更新的应用的配置文件的nexus存放信息
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws NexusException
     * @throws IOException
     */
    private Map<String, List<NexusAssetInfo>> preprocessDomainApps(String platformId, List<AppUpdateOperationInfo> domainOptList, List<PlatformAppDeployDetailVo> deployAppList, DomainPo domainPo, boolean isCreate) throws ParamException, InterfaceCallException, NexusException, IOException
    {
        String domainId = domainPo.getDomainId();
        Map<String, List<NexusAssetInfo>> assetMap = new HashMap<>();
        Map<AppUpdateOperation, List<AppUpdateOperationInfo>> optMap = domainOptList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getOperation));
        List<AppUpdateOperationInfo> updateList = optMap.containsKey(AppUpdateOperation.UPDATE) ? optMap.get(AppUpdateOperation.UPDATE) : new ArrayList<>();
        List<AppUpdateOperationInfo> addList = optMap.containsKey(AppUpdateOperation.ADD) ? optMap.get(AppUpdateOperation.ADD) : new ArrayList<>();
        if(addList.size() > 0)
            generateAlias4DomainApps(domainPo, addList, deployAppList, isCreate);
        for(AppUpdateOperationInfo optInfo : addList)
        {
            AppModuleVo module = this.registerAppMap.get(optInfo.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(optInfo.getTargetVersion());
            PlatformAppPo platformAppPo = optInfo.getPlatformApp(0, module.getAppId(), platformId, domainId);
            String directory = platformAppPo.getPlatformAppDirectory(module.getAppName(), module.getVersion(), platformAppPo);
            List<AppFilePo> fileList = new ArrayList<>();
            for(AppFileNexusInfo cfg : optInfo.getCfgs())
            {
                AppFilePo filePo = new AppFilePo(module.getAppId(), cfg);
                fileList.add(filePo);
            }
            logger.debug(String.format("download cfg of %s at %s and upload to %s/%s", optInfo.getAppAlias(), domainId, this.platformAppCfgRepository, directory));
            List<NexusAssetInfo> assetList = downloadAndUploadAppFiles(module.getAppId(), this.nexusHostUrl, this.nexusUserName, this.nexusPassword, fileList, this.platformAppCfgRepository, directory);
            assetMap.put(optInfo.getAppAlias(), assetList);
        }
        for(AppUpdateOperationInfo optInfo : updateList)
        {
            AppModuleVo module = this.registerAppMap.get(optInfo.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(optInfo.getTargetVersion());
            int platformAppId = deployAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).get(optInfo.getAppAlias()).getPlatformApp().getPlatformAppId();
            PlatformAppPo platformAppPo = optInfo.getPlatformApp(platformAppId, module.getAppId(), platformId, domainId);
            String directory = platformAppPo.getPlatformAppDirectory(module.getAppName(), module.getVersion(), platformAppPo);
            List<AppFilePo> fileList = new ArrayList<>();
            for(AppFileNexusInfo cfg : optInfo.getCfgs())
            {
                AppFilePo filePo = new AppFilePo(module.getAppId(), cfg);
                fileList.add(filePo);
            }
            logger.debug(String.format("download cfg of %s at %s and upload to %s/%s", optInfo.getAppAlias(), domainId, this.platformAppCfgRepository, directory));
            List<NexusAssetInfo> assetList = downloadAndUploadAppFiles(module.getAppId(), this.nexusHostUrl, this.nexusUserName, this.nexusPassword, fileList, this.platformAppCfgRepository, directory);
            assetMap.put(optInfo.getAppAlias(), assetList);
        }
        return assetMap;
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
