package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.config.*;
import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.dao.*;
import com.channelsoft.ccod.support.cmdb.exception.*;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.service.*;
import com.channelsoft.ccod.support.cmdb.vo.*;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.DigestUtils;

import javax.annotation.PostConstruct;
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
 * @ClassName: PlatformManagerServiceImpl
 * @Author: lanhb
 * @Description: 实现IPlatformManagerService接口的服务类
 * @Date: 2020/5/28 9:54
 * @Version: 1.0
 */
@Service
public class PlatformManagerServiceImpl implements IPlatformManagerService {

    private final static Logger logger = LoggerFactory.getLogger(PlatformManagerServiceImpl.class);

    private final static Gson gson = new GsonBuilder().registerTypeAdapter(DateTime.class, new GsonDateUtil()).create();

    protected String testPlatformId = "test-by-wyf";

    protected String testK8sApiUrl = "https://10.130.41.218:6443";

    protected String testAuthToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA";

    private Gson templateParseGson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            //过滤掉字段名包含"age"
            return f.getName().contains("creationTimestamp") || f.getName().contains("status") || f.getName().contains("resourceVersion") || f.getName().contains("selfLink") || f.getName().contains("uid")
                    || f.getName().contains("generation") || f.getName().contains("annotations") || f.getName().contains("strategy")
                    || f.getName().contains("terminationMessagePath") || f.getName().contains("terminationMessagePolicy")
                    || f.getName().contains("dnsPolicy") || f.getName().contains("securityContext") || f.getName().contains("schedulerName")
                    || f.getName().contains("restartPolicy") || f.getName().contains("clusterIP")
                    || f.getName().contains("sessionAffinity") || f.getName().contains("nodePort");
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            //过滤掉 类名包含 Bean的类
            return clazz.getName().contains("Bean");
        }
    }).registerTypeAdapter(DateTime.class, new GsonDateUtil()).create();

    @Autowired
    IK8sApiService ik8sApiService;

    @Autowired
    IAppManagerService appManagerService;

    @Autowired
    INexusService nexusService;

    @Autowired
    ILJPaasService ljPaasService;

    @Autowired
    IPlatformAppCollectService platformAppCollectService;

    @Autowired
    IK8sApiService k8sApiService;

    @Autowired
    ILJPaasService paasService;

    @Autowired
    IK8sTemplateService k8sTemplateService;

    @Autowired
    PlatformThreePartAppMapper platformThreePartAppMapper;

    @Autowired
    PlatformThreePartServiceMapper platformThreePartServiceMapper;

    @Autowired
    PlatformMapper platformMapper;

    @Autowired
    DomainMapper domainMapper;

    @Autowired
    AssembleMapper assembleMapper;

    @Autowired
    PlatformAppMapper platformAppMapper;

    @Autowired
    PlatformAppBkModuleMapper platformAppBkModuleMapper;

    @Autowired
    PlatformUpdateSchemaMapper platformUpdateSchemaMapper;

    @Autowired
    UnconfirmedAppModuleMapper unconfirmedAppModuleMapper;

    @Autowired
    PlatformAppDeployDetailMapper platformAppDeployDetailMapper;

    @Autowired
    PlatformPublicConfigMapper platformPublicConfigMapper;

    @Autowired
    AppMapper appMapper;

    @Autowired
    K8sOperationMapper k8sOperationMapper;

    @Autowired
    PlatformUpdateRecordMapper platformUpdateRecordMapper;

    @Autowired
    CCODBiz ccodBiz;

    @Autowired
    private ImageCfg imageCfg;

    @Value("${nexus.platform-app-cfg-repository}")
    private String platformAppCfgRepository;

    @Value("${nexus.app-module-repository}")
    private String appRepository;

    @Value("${nexus.unconfirmed-platform-app-repository}")
    private String unconfirmedPlatformAppRepository;

    @Value("${nexus.user}")
    private String nexusUserName;

    @Value("${nexus.password}")
    private String nexusPassword;

    @Value("${nexus.host-url}")
    private String nexusHostUrl;

    @Value("${k8s.deployment.defaultCfgMountPath}")
    private String defaultCfgMountPath;

    @Value("${k8s.labels.domain-id}")
    private String domainIdLabel;

    @Value("${k8s.labels.app-name}")
    private String appNameLabel;

    @Value("${k8s.labels.app-alias}")
    private String appAliasLabel;

    @Value("${k8s.labels.app-version}")
    private String appVersionLabel;

    @Value("${k8s.labels.app-type}")
    private String appTypeLabel;

    @Value("${k8s.labels.job-id}")
    private String jobIdLabel;

    @Value("${k8s.labels.service-type}")
    private String serviceTypeLabel;

    @Value("${k8s.labels.deployment-tag}")
    private String deploymentTagLabel;

    @Value("${k8s.labels.deployment-type}")
    private String deploymentTypeLabel;

    @Value("${k8s.labels.ccod-version}")
    private String ccodVersionLabel;

    @Value("${k8s.volume-names.web-app}")
    private String webappVolumeName;

    @Value("${k8s.volume-names.binary-file}")
    private String binaryFileVolumeName;

    @Value("${ccod.platform-deploy-template}")
    private String platformDeployScriptFileName;

    @Value("${nexus.image-repository}")
    private String imageRepository;

    @Value("${nexus.platform-deploy-script-repository}")
    private String platformDeployScriptRepository;

    @Value("${k8s.gls_oracle_svc_name}")
    private String glsOracleSvcName;

    @Value("${k8s.gls_oracle_sid}")
    private String glsOracleSid;

    @Value("${nexus.tmp-platform-app-cfg-repository}")
    private String platformTmpCfgRepository;

    @Value("${nexus.nexus-docker-url}")
    private String nexusDockerUrl;

    @Value("${cmdb.url}")
    private String cmdbUrl;

    @Value("${git.k8s_deploy_git_url}")
    private String k8sDeployGitUrl;

    @Value("${ccod.domain-id-regex}")
    private String platformIdRegex;

    @Value("${ccod.health-check-at-regex}")
    private String healthCheckRegex;

    @Value("${ccod.service-port-regex}")
    private String portRegex;

    private final Map<String, PlatformUpdateSchemaInfo> platformUpdateSchemaMap = new ConcurrentHashMap<>();

    private Map<String, List<BizSetDefine>> appSetRelationMap;

    private Map<String, BizSetDefine> setDefineMap;

    protected final ReentrantReadWriteLock appWriteLock = new ReentrantReadWriteLock();

    private boolean isPlatformCheckOngoing = false;

    private String domainIdRegexFmt = "^%s(0[1-9]|[1-9]\\d+)";

    private String aliasRegexFmt = "^%s\\d*$";

    private String domainServiceNameFmt = "^%s-%s($|(-[0-9a-z]+)+$)";

    @PostConstruct
    void init() throws Exception {
        this.appSetRelationMap = new HashMap<>();
        for (BizSetDefine setDefine : this.ccodBiz.getSet()) {
            for (AppDefine appDefine : setDefine.getApps()) {
                if (!this.appSetRelationMap.containsKey(appDefine.getName()))
                    this.appSetRelationMap.put(appDefine.getName(), new ArrayList<>());
                this.appSetRelationMap.get(appDefine.getName()).add(setDefine);
            }
        }
        this.setDefineMap = this.ccodBiz.getSet().stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        List<PlatformUpdateSchemaPo> schemaPoList = this.platformUpdateSchemaMapper.select();
        for (PlatformUpdateSchemaPo po : schemaPoList) {
            try {
                PlatformUpdateSchemaInfo schemaInfo = gson.fromJson(new String(po.getContext()), PlatformUpdateSchemaInfo.class);
                this.platformUpdateSchemaMap.put(po.getPlatformId(), schemaInfo);
            } catch (Exception ex) {
                logger.error(String.format("parse %s platform update schema exception", po.getPlatformId()), ex);
            }
        }
        String authToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA";
        String k8sApiUrl = "https://10.130.41.218:6443";
        String namespace = "clone-test";
        try {
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public List<PlatformUpdateSchemaInfo> queryPlatformUpdateSchema(String platformId) {
        logger.debug(String.format("begin to query platformId=%s platform update schema", platformId));
        List<PlatformUpdateSchemaInfo> schemaList = new ArrayList<>();
        if (StringUtils.isBlank(platformId)) {
            schemaList = new ArrayList<>(this.platformUpdateSchemaMap.values());
        } else {
            if (this.platformUpdateSchemaMap.containsKey(platformId)) {
                schemaList.add(platformUpdateSchemaMap.get(platformId));
            }
        }
        return schemaList;
    }

    @Override
    public PlatformTopologyInfo getPlatformTopology(String platformId) throws ParamException, InterfaceCallException, LJPaasException, NotSupportAppException {
        logger.debug(String.format("begin to query %s platform topology", platformId));
        PlatformPo platform = platformMapper.selectByPrimaryKey(platformId);
        if (platform == null) {
            logger.error(String.format("%s platform not exist", platformId));
            throw new ParamException(String.format("%s platform not exist", platformId));
        }
        PlatformTopologyInfo topology = new PlatformTopologyInfo(platform);
        if (topology.getStatus() == null) {
            logger.error(String.format("unknown ccod platform status %d", platform.getStatus()));
            throw new ParamException(String.format("unknown ccod platform status %d", platform.getStatus()));
        }
        List<LJHostInfo> idleHostList;
        PlatformUpdateSchemaInfo schema;
        List<CCODSetInfo> setList;
        Map<String, BizSetDefine> bizSetMap = this.ccodBiz.getSet().stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        List<PlatformAppDeployDetailVo> deployAppList;
        List<UnconfirmedAppModulePo> unconfirmedAppModuleList = null;
        List<PlatformThreePartAppPo> threeAppList = this.platformThreePartAppMapper.select(platformId);
        List<PlatformThreePartServicePo> threeSvcList = this.platformThreePartServiceMapper.select(platformId);
        switch (topology.getStatus()) {
            case RUNNING:
                if (this.platformUpdateSchemaMap.containsKey(platformId)) {
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
            case SCHEMA_CREATE:
            case SCHEMA_UPDATE:
                if (!this.platformUpdateSchemaMap.containsKey(platformId)) {
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
            case WAIT_SYNC_TO_PAAS:
                if (this.platformUpdateSchemaMap.containsKey(platformId)) {
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
        topology.setThreePartAppList(threeAppList);
        topology.setThreePartServiceList(threeSvcList);
        return topology;
    }

    /**
     * 把通过onlinemanager主动收集上来的ccod应用部署情况同步到paas之前需要给这些应用添加对应的bizId
     * 确定应用归属的set信息,并根据定义的set-app关系对某些应用归属域重新赋值
     *
     * @param bizId      蓝鲸paas的biz id
     * @param deployApps 需要处理的应用详情
     * @return 处理后的结果
     * @throws NotSupportAppException 如果应用中存在没有在lj-paas.set-apps节点定义的应用将抛出此异常
     */
    private List<PlatformAppDeployDetailVo> makeUpBizInfoForDeployApps(int bizId, List<PlatformAppDeployDetailVo> deployApps) throws NotSupportAppException {
        for (PlatformAppDeployDetailVo deployApp : deployApps) {
            if (!this.appSetRelationMap.containsKey(deployApp.getAppName())) {
                logger.error(String.format("%s没有在配置文件的lj-paas.set-apps节点中定义", deployApp.getAppName()));
                throw new NotSupportAppException(String.format("%s未定义所属的set信息", deployApp.getAppName()));
            }
            deployApp.setBkBizId(bizId);
            BizSetDefine sd = this.appSetRelationMap.get(deployApp.getAppName()).get(0);
            deployApp.setBkSetName(sd.getName());
            if (StringUtils.isNotBlank(sd.getFixedDomainName())) {
                deployApp.setBkSetName(sd.getName());
                deployApp.setDomainId(sd.getFixedDomainId());
                deployApp.setDomainName(sd.getFixedDomainName());
            }
        }
        return deployApps;
    }

    void getDeployStartChains(V1Deployment deploy, List<V1Deployment> allDeploys, List<AppModuleVo> registerApps, List<V1Deployment> deploys) throws ParamException, NotSupportAppException
    {
        Map<String, StartChain> chainMap = this.ccodBiz.getStartChains().getChains().stream().collect(Collectors.toMap(StartChain::getName, Function.identity()));
        for(V1Container initContainer : deploy.getSpec().getTemplate().getSpec().getInitContainers())
        {
            AppModuleVo module = getAppModuleFromImageTag(initContainer.getImage(), registerApps);
            if(chainMap.containsKey(module.getAppName()))
            {

            }
        }
    }

    /**
     * 根据应用部署详情生成平台的set拓扑结构
     *
     * @param deployAppList 平台的应用部署明细
     * @return 台的set拓扑结构
     * @throws DBPAASDataNotConsistentException
     * @throws NotSupportAppException
     */
    private List<CCODSetInfo> generateCCODSetInfo(List<PlatformAppDeployDetailVo> deployAppList, List<String> setNames) throws ParamException, NotSupportAppException {
        Map<String, List<PlatformAppDeployDetailVo>> setAppMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getBkSetName));
        List<CCODSetInfo> setList = new ArrayList<>();
        for (PlatformAppDeployDetailVo deployApp : deployAppList) {
            if (!this.appSetRelationMap.containsKey(deployApp.getAppName())) {
                logger.error(String.format("current version not support %s", deployApp.getAppName()));
                throw new NotSupportAppException(String.format("current version not support %s", deployApp.getAppName()));
            } else if (!setDefineMap.containsKey(deployApp.getBkSetName())) {
                logger.error(String.format("%s is not a legal ccod set name", deployApp.getBkSetName()));
                throw new ParamException(String.format("%s is not a legal ccod set name", deployApp.getBkSetName()));
            }
        }
        for (String setName : setAppMap.keySet()) {
            Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = setAppMap.get(setName)
                    .stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainName));
            List<CCODDomainInfo> domainList = new ArrayList<>();
            for (String domainName : domainAppMap.keySet()) {
                List<PlatformAppDeployDetailVo> domAppList = domainAppMap.get(domainName);
                CCODDomainInfo domain = new CCODDomainInfo(domAppList.get(0).getDomainId(), domAppList.get(0).getDomainName());
                Map<String, List<PlatformAppDeployDetailVo>> assembleAppMap = domAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getAssembleTag));
                for (String assembleTag : assembleAppMap.keySet()) {
                    CCODAssembleInfo assemble = new CCODAssembleInfo(assembleTag);
                    for (PlatformAppDeployDetailVo deployApp : assembleAppMap.get(assembleTag)) {
                        CCODModuleInfo bkModule = new CCODModuleInfo(deployApp);
                        assemble.getModules().add(bkModule);
                    }
                    domain.getAssembles().add(assemble);
                }
                domainList.add(domain);
            }
            CCODSetInfo set = new CCODSetInfo(setName);
            set.setDomains(domainList);
            setList.add(set);
        }
        for (String setName : setNames) {
            if (!setAppMap.containsKey(setName)) {
                CCODSetInfo setInfo = new CCODSetInfo(setName);
                setList.add(setInfo);
            }
        }
        return setList;
    }

    @Override
    public List<PlatformTopologyInfo> queryAllPlatformTopology() throws ParamException, InterfaceCallException, LJPaasException, NotSupportAppException {
        List<PlatformTopologyInfo> topoList = new ArrayList<>();
        List<PlatformPo> platforms = platformMapper.select(null);
        for (PlatformPo platformPo : platforms) {
            PlatformTopologyInfo topo = new PlatformTopologyInfo(platformPo);
            if (topo.getStatus() == null) {
                logger.error(String.format("%s status %d is unknown", platformPo.getPlatformId(), platformPo.getStatus()));
                continue;
            }
            switch (topo.getStatus()) {
                case RUNNING:
                case WAIT_SYNC_TO_PAAS:
                case SCHEMA_CREATE:
                case SCHEMA_UPDATE:
                    topoList.add(topo);
                    break;
                default:
            }
        }
        return topoList;
    }

    @Override
    public List<PlatformAppPo> updatePlatformApps(String platformId, String platformName, List<AppUpdateOperationInfo> appList) throws NotSupportAppException, ParamException, InterfaceCallException, NexusException, LJPaasException, IOException {
        logger.debug(String.format("begin to update %d apps of %s(%s)", appList.size(), platformName, platformId));
        PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
        if (platformPo == null) {
            logger.error(String.format("%s platform not exist", platformId));
            throw new ParamException(String.format("%s platform not exist", platformId));
        }
        if (!platformName.equals(platformPo.getPlatformName())) {
            logger.error(String.format("name of %s is %s not %s", platformId, platformPo.getPlatformName(), platformName));
            throw new ParamException(String.format("name of %s is %s not %s", platformId, platformPo.getPlatformName(), platformName));
        }
        modifyPlatformApps(platformPo, appList);
        this.paasService.syncClientCollectResultToPaas(platformPo.getBkBizId(), platformId, platformPo.getBkCloudId());
        return new ArrayList<>();
    }

    private void modifyPlatformApps(PlatformPo platformPo, List<AppUpdateOperationInfo> appList) throws NotSupportAppException, ParamException, InterfaceCallException, NexusException, LJPaasException, IOException
    {
        Date date = new Date();
        String platformId = platformPo.getPlatformId();
        List<DomainPo> domainList = this.domainMapper.select(platformId, null);
        Map<String, DomainPo> domainMap = domainList.stream().collect(Collectors.toMap(DomainPo::getDomainName, Function.identity()));
        Map<String, List<AppUpdateOperationInfo>> domainOptMap = appList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getDomainName));
        List<PlatformAppDeployDetailVo> deployApps = this.platformAppDeployDetailMapper.selectPlatformApps(platformId, null, null);
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = deployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainName));
        List<LJHostInfo> hostList = this.paasService.queryBKHost(platformPo.getBkBizId(), null, null, null, null);
        Map<String, List<AssemblePo>> domainAssembleMap = this.assembleMapper.select(platformId, null).stream().collect(Collectors.groupingBy(AssemblePo::getDomainId));
        List<AppModuleVo> registerApps = this.appManagerService.queryAllRegisterAppModule(null);
        for (String domainName : domainOptMap.keySet()) {
            if (!domainMap.containsKey(domainName)) {
                logger.error(String.format("domain %s not exist", domainName));
                throw new ParamException(String.format("domain %s not exist", domainName));
            }
            List<PlatformAppDeployDetailVo> deployAppList = domainAppMap.containsKey(domainName) ? domainAppMap.get(domainName) : new ArrayList<>();
            checkDomainApps(domainOptMap.get(domainName), deployAppList, domainMap.get(domainName), registerApps, hostList);
        }
        Map<String, Map<String, List<AppFileNexusInfo>>> domainCfgMap = new HashMap<>();
        for (String domainName : domainOptMap.keySet()) {
            List<PlatformAppDeployDetailVo> deployAppList = domainAppMap.containsKey(domainName) ? domainAppMap.get(domainName) : new ArrayList<>();
            Map<String, List<AppFileNexusInfo>> appCfgMap = preprocessDomainApps(platformId, domainOptMap.get(domainName), deployAppList, domainMap.get(domainName), false, date);
            domainCfgMap.put(domainName, appCfgMap);
        }
        for (String domainName : domainOptMap.keySet()) {
            String domainId = domainMap.get(domainName).getDomainId();
            List<PlatformAppDeployDetailVo> deployAppList = domainAppMap.containsKey(domainName) ? domainAppMap.get(domainName) : new ArrayList<>();
            List<AssemblePo> assembleList = domainAssembleMap.containsKey(domainId) ? domainAssembleMap.get(domainId) : new ArrayList<>();
            handleDomainApps(platformId, domainMap.get(domainName), domainOptMap.get(domainName), assembleList, deployAppList, registerApps, domainCfgMap.get(domainName));
        }
    }

    /**
     * 处理域的应用
     *
     * @param platformId         平台id
     * @param domainPo           域相关信息
     * @param domainOptList      指定需要增、删、修改的应用
     * @param domainAppList      域已经部署的所有应用
     * @param domainAssembleList 域已有assemble列表
     * @param registerApps       已经注册应用列表
     * @param cfgMap 域模块配置文件nexus存储相关信息
     */
    private void handleDomainApps(String platformId, DomainPo domainPo, List<AppUpdateOperationInfo> domainOptList, List<AssemblePo> domainAssembleList, List<PlatformAppDeployDetailVo> domainAppList, List<AppModuleVo> registerApps, Map<String, List<AppFileNexusInfo>> cfgMap) {
        Map<String, List<AppModuleVo>> registerAppMap = registerApps.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        String domainId = domainPo.getDomainId();
        Map<AppUpdateOperation, List<AppUpdateOperationInfo>> optMap = domainOptList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getOperation));
        List<AppUpdateOperationInfo> updateList = optMap.containsKey(AppUpdateOperation.UPDATE) ? optMap.get(AppUpdateOperation.UPDATE) : new ArrayList<>();
        List<AppUpdateOperationInfo> addList = optMap.containsKey(AppUpdateOperation.ADD) ? optMap.get(AppUpdateOperation.ADD) : new ArrayList<>();
        List<AppUpdateOperationInfo> deleteList = optMap.containsKey(AppUpdateOperation.DELETE) ? optMap.get(AppUpdateOperation.DELETE) : new ArrayList<>();
        for (AppUpdateOperationInfo optInfo : deleteList) {
            String alias = optInfo.getAlias();
            String tag = String.format("DELETE %s in %s", alias, domainId);
            logger.debug(String.format("begin to %s", tag));
            int platformAppId = domainAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).get(alias).getPlatformAppId();
            logger.debug(String.format("delete platform_app_bk_module with platformAppId=%d", platformAppId));
            this.platformAppBkModuleMapper.delete(platformAppId, null, null, null);
            logger.debug(String.format("delete platform_app_cfg_file with platformAppId=%d", platformAppId));
            logger.debug(String.format("delete platform_app with platformAppId=%d", platformAppId));
            this.platformAppMapper.delete(platformAppId, null, null);
            logger.info(String.format("%s success", tag));
        }
        Map<String, AssemblePo> assembleMap = domainAssembleList.stream().collect(Collectors.toMap(AssemblePo::getTag, Function.identity()));
        for (AppUpdateOperationInfo optInfo : updateList) {
            PlatformAppPo src = domainAppList.stream()
                    .collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).get(optInfo.getAlias()).getPlatformApp();
            int appId = registerAppMap.get(optInfo.getAppName()).stream()
                    .collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(optInfo.getVersion()).getAppId();
            String hostIp = StringUtils.isNotBlank(optInfo.getHostIp()) ? optInfo.getHostIp() : src.getHostIp();
            src.update(optInfo, appId, cfgMap.get(optInfo.getAlias()), hostIp);
            logger.debug(String.format("update platform_app to %s", gson.toJson(src)));
            this.platformAppMapper.update(src);
        }
        for (AppUpdateOperationInfo optInfo : addList) {
            String appName = optInfo.getAppName();
            String alias = optInfo.getAlias();
            String version = optInfo.getVersion();
            String hostIp = optInfo.getHostIp();
            String assembleTag = optInfo.getAssembleTag();
            String tag = String.format("ADD %s(%s) to %s on %s in %s at %s", alias, appName, version, assembleTag, domainId, hostIp);
            logger.debug(String.format("begin to %s", tag));
            if (!assembleMap.containsKey(assembleTag)) {
                logger.debug(String.format("assemble %s of %s is not exist, add it first", assembleTag, domainId));
                AssemblePo assemblePo = new AssemblePo();
                assemblePo.setDomainId(domainId);
                assemblePo.setPlatformId(platformId);
                assemblePo.setStatus("Running");
                assemblePo.setTag(assembleTag);
                assembleMapper.insert(assemblePo);
                assembleMap.put(assembleTag, assemblePo);
            }
            AssemblePo assemblePo = assembleMap.get(assembleTag);
            AppModuleVo module = registerAppMap.get(appName).stream()
                    .collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version);
            PlatformAppPo platformAppPo = optInfo.getPlatformApp(module.getAppId(), cfgMap.get(optInfo.getAlias()),
                    platformId, domainId, assemblePo.getAssembleId());
            platformAppPo.setAssembleId(assemblePo.getAssembleId());
            this.platformAppMapper.insert(platformAppPo);
            logger.info(String.format("%s success", tag));
        }
    }

    /**
     * 为域新加的应用自动生成应用别名
     *
     * @param domainPo      添加应用的域
     * @param addOptList    该域所有被添加的应用
     * @param deployAppList 该域已经部署的应用列表
     * @param clone         该域是否是否是通过clone方式获得
     */
    private void generateAlias4DomainApps(DomainPo domainPo, List<AppUpdateOperationInfo> addOptList, List<PlatformAppDeployDetailVo> deployAppList, boolean clone) throws ParamException {
        addOptList.forEach(o->{
            if(StringUtils.isNotBlank(o.getAlias()) && StringUtils.isBlank(o.getOriginalAlias()))
                o.setOriginalAlias(o.getAlias());});
        Map<String, String> appNameAliasMap = this.setDefineMap.get(domainPo.getBizSetName()).getApps().stream()
                .collect(Collectors.toMap(a->a.getName(), v->v.getAlias()));
        Map<String, List<String>> appUsedAliasMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getAppName,
                Collectors.collectingAndThen(Collectors.toList(), s->s.stream().map(d->d.getAlias()).collect(Collectors.toList()))));
        Map<String, List<AppUpdateOperationInfo>> addAppMap = addOptList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
        for (String appName : addAppMap.keySet()) {
            List<String> usedAlias = appUsedAliasMap.containsKey(appName) ? appUsedAliasMap.get(appName) : new ArrayList<>();
            String standAlias = appNameAliasMap.get(appName);
            List<AppUpdateOperationInfo> needAliasList = new ArrayList<>();
            addAppMap.get(appName).forEach(o->{
                if (StringUtils.isBlank(o.getAlias()) || !clone)
                    needAliasList.add(o);
                else
                    usedAlias.add(o.getAlias());
            });
            boolean onlyOne = usedAlias.size() == 0 && needAliasList.size() == 1 ? true : false;
            for (AppUpdateOperationInfo optInfo : needAliasList) {
                String alias = autoGenerateAlias(standAlias, usedAlias, onlyOne);
                optInfo.setAlias(alias);
                usedAlias.add(alias);
            }
        }
        addOptList.forEach(o->{
            o.setAppRunner(o.getAlias());
            if(StringUtils.isBlank(o.getOriginalAlias()))
                o.setOriginalAlias(o.getAlias());
        });
    }

    /**
     * 为指定域新加的应用生成别名，并且下载添加/更新的应用的配置文件到指定nexus路径
     *
     * @param platformId    平台id
     * @param domainOptList 指定域的应用的相关操作
     * @param deployAppList 指定域已经部署的所有应用
     * @param domainPo      指定域
     * @param clone         是否通过clone方式获得
     * @param date 执行日期
     * @return 添加/更新的应用的配置文件的nexus存放信息
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws NexusException
     * @throws IOException
     */
    private Map<String, List<AppFileNexusInfo>> preprocessDomainApps(String platformId, List<AppUpdateOperationInfo> domainOptList, List<PlatformAppDeployDetailVo> deployAppList, DomainPo domainPo, boolean clone, Date date) throws ParamException, InterfaceCallException, NexusException, IOException {
        String domainId = domainPo.getDomainId();
        List<AppUpdateOperationInfo> addList = domainOptList.stream().filter(o -> o.getOperation().equals(AppUpdateOperation.ADD)).collect(Collectors.toList());
        if (addList.size() > 0)
            generateAlias4DomainApps(domainPo, addList, deployAppList, clone);
        Map<String, List<AppFileNexusInfo>> assetMap = new HashMap<>();
        List<AppUpdateOperationInfo> handleCfgList = domainOptList.stream().filter(o->o.getOperation().equals(AppUpdateOperation.ADD)||o.getOperation().equals(AppUpdateOperation.UPDATE))
                .collect(Collectors.toList());;
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        for (AppUpdateOperationInfo optInfo : handleCfgList) {
            if (StringUtils.isBlank(optInfo.getAssembleTag()))
                optInfo.setAssembleTag(String.format("%s-%s", optInfo.getAlias(), domainId));
            String directory = String.format("%s/%s/%s/%s/%s/%s", platformId, sf.format(date), domainId,
                    optInfo.getAppName(), optInfo.getAlias(), optInfo.getVersion());
            logger.debug(String.format("download cfg of %s at %s and upload to %s/%s", optInfo.getAlias(), domainId, this.platformAppCfgRepository, directory));
            List<AppFileNexusInfo> assetList = appManagerService.downloadAndUploadAppFiles(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, optInfo.getCfgs(), this.platformAppCfgRepository, directory);
            assetMap.put(optInfo.getAlias(), assetList);
        }
        return assetMap;
    }

    /**
     * 检查一个域的操作是否合法
     *
     * @param domainOptList 对指定域的所有操作
     * @param domainAppList 指定域已经部署的所有应用
     * @param domainPo      指定的域信息
     * @param registerApps  已经注册应用列表
     * @param hostList      该域所属平台的服务器信息
     * @throws ParamException
     * @throws NotSupportAppException
     */
    private void checkDomainApps(List<AppUpdateOperationInfo> domainOptList, List<PlatformAppDeployDetailVo> domainAppList, DomainPo domainPo, List<AppModuleVo> registerApps, List<LJHostInfo> hostList) throws ParamException, NotSupportAppException {
        Map<String, LJHostInfo> hostMap = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity()));
        String domainId = domainPo.getDomainId();
        Map<String, List<AppModuleVo>> registerAppMap = registerApps.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        for (AppUpdateOperationInfo optInfo : domainOptList) {
            String appName = optInfo.getAppName();
            String alias = optInfo.getAlias();
            String version = optInfo.getVersion();
            String hostIp = optInfo.getHostIp();
            String originalAlias = optInfo.getOriginalAlias();
            String tag = String.format("UPDATE %s(%s) to %s in %s at %s", alias, appName, version, domainId, hostIp);
            switch (optInfo.getOperation()) {
                case UPDATE:
                    if (StringUtils.isBlank(appName)) {
                        logger.error(String.format("%s FAIL : appName is blank", tag));
                        throw new ParamException(String.format("%s FAIL : appName is blank", tag));
                    } else if (StringUtils.isBlank(version)) {
                        logger.error(String.format("%s FAIL : version is blank", tag));
                        throw new ParamException(String.format("%s FAIL : version is blank", tag));
                    } else if (!registerAppMap.containsKey(appName) || !registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version)) {
                        logger.error(String.format("%s FAIL : version %s not register", tag, version));
                        throw new ParamException(String.format("%s FAIL : version %s not register", tag, version));
                    }
                    if (StringUtils.isBlank(alias)) {
                        logger.error(String.format("%s FAIL : appAlias is blank", tag));
                        throw new ParamException(String.format("%s FAIL : appAlias is blank", tag));
                    } else if (!domainAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).containsKey(alias)) {
                        logger.error(String.format("%s FAIL : %s not exist", tag, alias));
                        throw new ParamException(String.format("%s FAIL : %s not exist", tag, alias));
                    }
                    if (StringUtils.isBlank(hostIp)) {
                        logger.error(String.format("%s FAIL : hostIp is blank", tag));
                        throw new ParamException(String.format("%s FAIL : hostIp is blank", tag));
                    } else if (!hostMap.containsKey(hostIp)) {
                        logger.error(String.format("%s FAIL : host %s not exist", tag, hostIp));
                        throw new ParamException(String.format("%s FAIL : host %s not exist", tag, hostIp));
                    }
                    if (!this.setDefineMap.get(domainPo.getBizSetName()).getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity())).containsKey(appName)) {
                        logger.error(String.format("%s not support %s", domainPo.getBizSetName(), appName));
                        throw new NotSupportAppException(String.format("%s not support %s", domainPo.getBizSetName(), appName));
                    }
                    if (optInfo.getCfgs() == null || optInfo.getCfgs().size() == 0) {
                        logger.error(String.format("%s FAIL : cfg is blank", tag));
                        throw new ParamException(String.format("%s FAIL : cfg is blank", tag));
                    }
                    if (!domainAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).containsKey(alias)) {
                        logger.error(String.format("%s FAIL : %s of %s not exist", tag, alias, domainId));
                        throw new ParamException(String.format("%s FAIL : %s of %s not exist", tag, alias, domainId));
                    }
                    break;
                case ADD:
                    tag = String.format("ADD %s[%s(%s)] in %s at %s", alias, appName, version, domainId, hostIp);
                    if (StringUtils.isBlank(appName)) {
                        logger.error(String.format("%s FAIL : appName is blank", tag));
                        throw new ParamException(String.format("%s FAIL : appName is blank", tag));
                    } else if (StringUtils.isBlank(version)) {
                        logger.error(String.format("%s FAIL : version is blank", tag));
                        throw new ParamException(String.format("%s FAIL : version is blank", tag));
                    } else if (!registerAppMap.containsKey(appName) || !registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version)) {
                        logger.error(String.format("%s FAIL : version %s not register", tag, version));
                        throw new ParamException(String.format("%s FAIL : version %s not register", tag, version));
                    }
                    if (!AppUpdateOperation.ADD.equals(optInfo.getOperation()) && StringUtils.isBlank(alias)) {
                        logger.error(String.format("%s FAIL : appAlias is blank", tag));
                        throw new ParamException(String.format("%s FAIL : appAlias is blank", tag));
                    } else if (StringUtils.isNotBlank(originalAlias)
                            && domainAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).containsKey(originalAlias)) {
                        logger.error(String.format("%s FAIL : %s has exist", tag, originalAlias));
                        throw new ParamException(String.format("%s FAIL : %s has exist", tag, originalAlias));
                    }
                    if (StringUtils.isBlank(hostIp)) {
                        logger.error(String.format("%s FAIL : hostIp is blank", tag));
                        throw new ParamException(String.format("%s FAIL : hostIp is blank", tag));
                    } else if (!hostMap.containsKey(hostIp)) {
                        logger.error(String.format("%s FAIL : host %s not exist", tag, hostIp));
                        throw new ParamException(String.format(String.format("%s FAIL : host %s not exist", tag, hostIp)));
                    }
                    if (!this.setDefineMap.get(domainPo.getBizSetName()).getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity())).containsKey(appName)) {
                        logger.error(String.format("%s not support %s", domainPo.getBizSetName(), appName));
                        throw new NotSupportAppException(String.format("%s not support %s", domainPo.getBizSetName(), appName));
                    }
                    if (optInfo.getCfgs() == null || optInfo.getCfgs().size() == 0) {
                        logger.error(String.format("%s FAIL : cfg is blank", tag));
                        throw new ParamException(String.format("%s FAIL : cfg is blank", tag));
                    }
                    if (domainAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).containsKey(alias)) {
                        logger.error(String.format("%s FAIL : %s of %s exist", tag, alias, domainId));
                        throw new ParamException(String.format("%s FAIL : a%s of %s exist", tag, alias, domainId));
                    }
                    break;
                case DELETE:
                    tag = String.format("DELETE %s in %s", alias, domainId);
                    if (StringUtils.isBlank(alias)) {
                        logger.error(String.format("%s FAIL : appAlias is blank", tag));
                        throw new ParamException(String.format("%s FAIL : appAlias is blank", tag));
                    } else if (!domainAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).containsKey(alias)) {
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
//            for(AppCfgFilePo cfg : this.registerAppMap.get(optInfo.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(optInfo.getVersion()).getCfgs())
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

    private void checkPlatformBase(PlatformBase platformBase, PlatformUpdateTaskType taskType, List<DomainUpdatePlanInfo> plans, UpdateStatus status)
    {
        String platformId = platformBase.getPlatformId();
        Assert.notNull(platformId, "platformId can not be null");
        Assert.isTrue(platformId.matches(this.platformIdRegex), String.format("%s is illegal platformId", platformBase.getPlatformId()));
        switch (taskType)
        {
            case CREATE:
                Assert.notNull(platformBase.getPlatformName(), "platformName can not be null");
                Assert.notNull(platformBase.getCcodVersion(), "ccodVersion can not be null");
                Assert.notNull(platformBase.getType(), "type can not be null");
                Assert.isTrue(platformBase.getType().equals(PlatformType.K8S_CONTAINER), "current version only support k8s deploy");
                Assert.isTrue(platformBase.getBkBizId() > 0, "bkBizId should be assigned");
                Assert.notNull(platformBase.getCreateMethod(), "create method can not be null");
                Assert.notNull(platformBase.getFunc(), "func can not be null");
                Assert.notNull(platformBase.getHostUrl(), "hostUrl can not be null");
                Assert.notNull(platformBase.getK8sHostIp(), "k8sHostIp can not be null");
                Assert.notNull(platformBase.getK8sApiUrl(), "k8sApiUrl can not be null");
                Assert.notNull(platformBase.getK8sAuthToken(), "k8sAuthToken can not be null");
                boolean clone = platformBase.getCreateMethod().equals(PlatformCreateMethod.CLONE) || platformBase.getCreateMethod().equals(PlatformCreateMethod.K8S_API_CLONE) ? true : false;
                if(!clone) {
                    Assert.notNull(platformBase.getBaseDataNexusRepository(), "baseDataNexusRepository can not be null");
                    Assert.notNull(platformBase.getBaseDataNexusPath(), "baseDataNexusPath can not be null");
                    Assert.notNull(platformBase.getGlsDBType(), "glsDBType cannot be null");
                    Assert.notNull(platformBase.getGlsDBUser(), "glsDBUser can not be null");
                    Assert.notNull(platformBase.getGlsDBPwd(), "glsDBPwd can not be null");
                    Assert.notNull(platformBase.getCfgs(), "cfgs of platform can not be null");
                    Assert.notEmpty(platformBase.getCfgs(), "cfgs of platform can not be empty");
                }
                List<DomainUpdatePlanInfo> notAddList = plans==null ? new ArrayList<>() : plans.stream().filter(p->!p.getUpdateType().equals(DomainUpdateType.ADD)).collect(Collectors.toList());
                Assert.isTrue(notAddList.size()==0, String.format("%d domain is not ADD for new CREATE platform", notAddList.size()));
                break;
            case DELETE:
                Assert.isTrue(plans==null || plans.size()==0, "not need assign plan for DELETE platform");
                break;
            case UPDATE:
                break;
        }
        switch (status)
        {
            case EXEC:
            case WAIT_EXEC:
                Assert.notNull(plans, String.format("status is %s, but plans of domain is null", status.name));
                Assert.notEmpty(plans, String.format("status is %s, but plans of domain is empty", status.name));
                List<DomainUpdatePlanInfo> targetPlans = plans.stream().filter(p->p.getStatus().equals(status)).collect(Collectors.toList());
                Assert.notEmpty(targetPlans, String.format("status of platform is %s but not status of domain is %s", status.name, status.name));
                if(status.equals(UpdateStatus.EXEC) && taskType.equals(PlatformUpdateTaskType.CREATE))
                    Assert.isTrue(targetPlans.size() == plans.size(), String.format("status of new CREATE platform is EXEC, but %d domain plans status is not EXEC", plans.size()-targetPlans.size()));
        }
    }

    private String checkAppBase(AppBase appBase, AppUpdateOperation operation, List<AppModuleVo> registerApps)
    {
        String appName = appBase.getAppName();
        if(StringUtils.isBlank(appName))
            return String.format("appName of %s is blank;", operation.name);
        String alias = appBase.getAlias();
        String version = appBase.getVersion();
        Map<String, List<AppModuleVo>> appMap = registerApps.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        if(!appMap.containsKey(appName))
            return String.format("%s %s not register;", operation.name, appName);
        switch (operation)
        {
            case DELETE:
                if(StringUtils.isBlank(alias))
                    return String.format("alias of %s %s is blank;", operation.name, appName);
                return "";
            case UPDATE:
                if(StringUtils.isBlank(alias))
                    return String.format("alias of %s for %s is blank;", appName, operation.name);
            case ADD:
            case DEBUG:
                if(StringUtils.isBlank(version))
                    return String.format("version of %s for %s is blank;", appName, operation.name);
                Map<String, AppModuleVo> versionMap = appMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
                if(!versionMap.containsKey(version))
                    return String.format("%s(%s) not register or not image;", appName, version);
                appBase.setAppType(versionMap.get(version).getAppType());
                break;
            default:
                return String.format("not support %s operation;", operation.name);
        }
        StringBuffer sb = new StringBuffer();
        String tag = String.format("%s %s(%s)", operation.name, alias, appName);
        if(operation.equals(AppUpdateOperation.ADD))
            tag = String.format("%s %s", operation.name, appName);
        if(StringUtils.isBlank(appBase.getBasePath()))
            sb.append(String.format("basePath of %s is blank;", tag));
        if(StringUtils.isBlank(appBase.getDeployPath()))
            sb.append(String.format("deployPath of %s is blank;", tag));
        if(StringUtils.isNotBlank(appBase.getCheckAt()) && !appBase.getCheckAt().matches(this.healthCheckRegex))
            sb.append(String.format("%s is illegal health check word for %s;", appBase.getCheckAt(), tag));
        if(StringUtils.isBlank(appBase.getStartCmd()))
            sb.append(String.format("startCmd is blank for %s;", tag));
        if(StringUtils.isBlank(appBase.getLogOutputCmd()))
            sb.append(String.format("logOutputCmd for %s is blank;", tag));
        if(StringUtils.isBlank(appBase.getPorts()))
            sb.append(String.format("ports is blank for %s;", tag));
        else if(!appBase.getPorts().matches(this.portRegex))
            sb.append(String.format("%s is illegal ports for %s;", appBase.getPorts(), tag));
        if(StringUtils.isNotBlank(appBase.getNodePorts()) && !appBase.getNodePorts().matches(this.portRegex))
            sb.append(String.format("%s is illegal nodePorts for %s;", appBase.getNodePorts(), tag));
        if(appBase.getCfgs() == null || appBase.getCfgs().size() == 0)
            sb.append(String.format("cfgs of %s is empty;", tag));
        return sb.toString();
    }

    private String checkAppBase(String ccodVersion, AppBase appBase, AppUpdateOperation operation, BizSetDefine setDefine, List<PlatformAppDeployDetailVo> deployApps, List<AppModuleVo> registerApps)
    {
        String appName = appBase.getAppName();
        if(StringUtils.isBlank(appName))
            return String.format("appName of %s is blank;", operation.name);
        String alias = appBase.getAlias();
        String version = appBase.getVersion();
        Map<String, List<AppModuleVo>> appMap = registerApps.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        if(!appMap.containsKey(appName))
            return String.format("%s %s not register;", operation.name, appName);
        if(setDefine != null) {
            Map<String, AppDefine> appDefineMap = setDefine.getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity()));
            if(!appDefineMap.containsKey(appName))
                return String.format("set %s not support %s;", setDefine.getName(), appName);
        }
        Map<String, PlatformAppDeployDetailVo> aliasMap = deployApps.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAlias, Function.identity()));
        switch (operation)
        {
            case DELETE:
                if(StringUtils.isBlank(alias))
                    return String.format("alias of %s %s is blank;", operation.name, appName);
                else if(!aliasMap.containsKey(alias) || !aliasMap.get(alias).getAppName().equals(appName))
                    return String.format("%s(%s) for %s not exist;", alias, appName, operation.name);
                return "";
            case UPDATE:
                if(StringUtils.isBlank(alias))
                    return String.format("alias of %s for %s is blank;", appName, operation.name);
                else if(!aliasMap.containsKey(alias) || !aliasMap.get(alias).getAppName().equals(appName))
                    return String.format("%s(%s) for %s not exist;", alias, appName, operation.name);
                appBase.setAppType(aliasMap.get(alias).getAppType());
            case ADD:
            case DEBUG:
                if(StringUtils.isBlank(version))
                    return String.format("version of %s for %s is blank;", appName, operation.name);
                Map<String, AppModuleVo> versionMap = appMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
                if(!versionMap.containsKey(version))
                    return String.format("%s(%s) not register or not image;", appName, version);
                else if(!versionMap.get(version).getCcodVersion().equals(ccodVersion))
                    return String.format("%s(%s) not support ccod %s", appName, version, ccodVersion);
                appBase.setAppType(versionMap.get(version).getAppType());
                break;
            default:
                return String.format("not support %s operation;", operation.name);
        }
        StringBuffer sb = new StringBuffer();
        String tag = String.format("%s %s(%s)", operation.name, alias, appName);
        if(operation.equals(AppUpdateOperation.ADD))
            tag = String.format("%s %s", operation.name, appName);
        if(StringUtils.isBlank(appBase.getBasePath()))
            sb.append(String.format("basePath of %s is blank;", tag));
        if(StringUtils.isBlank(appBase.getDeployPath()))
            sb.append(String.format("deployPath of %s is blank;", tag));
        if(StringUtils.isNotBlank(appBase.getCheckAt()) && !appBase.getCheckAt().matches(this.healthCheckRegex))
            sb.append(String.format("%s is illegal health check word for %s;", appBase.getCheckAt(), tag));
        if(StringUtils.isBlank(appBase.getStartCmd()))
            sb.append(String.format("startCmd is blank for %s;", tag));
        if(StringUtils.isBlank(appBase.getLogOutputCmd()))
            sb.append(String.format("logOutputCmd for %s is blank;", tag));
        if(StringUtils.isBlank(appBase.getPorts()))
            sb.append(String.format("ports is blank for %s;", tag));
        else if(!appBase.getPorts().matches(this.portRegex))
            sb.append(String.format("%s is illegal ports for %s;", appBase.getPorts(), tag));
        if(StringUtils.isNotBlank(appBase.getNodePorts()) && !appBase.getNodePorts().matches(this.portRegex))
            sb.append(String.format("%s is illegal nodePorts for %s;", appBase.getNodePorts(), tag));
        if(appBase.getCfgs() == null || appBase.getCfgs().size() == 0)
            sb.append(String.format("cfgs of %s is empty;", tag));
        return sb.toString();
    }

    @Override
    public void deletePlatformUpdateSchema(String platformId) throws ParamException {
        logger.debug(String.format("begin to delete platform update schema of %s", platformId));
        PlatformPo platformPo = platformMapper.selectByPrimaryKey(platformId);
        if (platformPo == null) {
            logger.error(String.format("%s platform not exist", platformId));
            throw new ParamException(String.format("%s platform not exist", platformId));
        }
        CCODPlatformStatus status = platformPo.getStatus();
        if (status == null) {
            logger.error(String.format("%s status value %d is unknown", platformId, platformPo.getStatus()));
            throw new ParamException(String.format("%s status value %d is unknown", platformId, platformPo.getStatus()));
        }
        if (this.platformUpdateSchemaMap.containsKey(platformId)) {
            logger.debug(String.format("remove schema of %s from memory", platformId));
            this.platformUpdateSchemaMap.remove(platformId);
        }
        logger.debug(String.format("delete schema of %s from database", platformId));
        this.platformUpdateSchemaMapper.delete(platformId);
        switch (status) {
            case SCHEMA_CREATE:
                logger.debug(String.format("%s status is %s, so it should be deleted", platformId, status.name));
                platformMapper.delete(platformId);
                break;
            default:
                logger.debug(String.format("%s status is %s, so it should be updated to %s",
                        platformId, status.name, CCODPlatformStatus.RUNNING.name));
                platformPo.setStatus(CCODPlatformStatus.RUNNING);
                platformMapper.update(platformPo);
                break;
        }
    }

    @Override
    public PlatformAppModuleVo[] startCollectPlatformAppUpdateData(String platformId, String platformName) throws Exception {
        logger.debug(String.format("begin to collect %s(%s) app update data", platformName, platformId));
        PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
        if (platformPo == null) {
            logger.error(String.format("%s platform not exist", platformId));
            throw new ParamException(String.format("%s platform not exist", platformId));
        }
        if (!platformPo.getPlatformName().equals(platformName)) {
            logger.error(String.format("name of %s is %s not %s", platformId, platformPo.getPlatformName(), platformName));
            throw new ParamException(String.format("name of %s is %s not %s", platformId, platformPo.getPlatformName(), platformName));
        }
        List<UnconfirmedAppModulePo> wantList = this.unconfirmedAppModuleMapper.select(platformId);
        logger.debug("want update item of %s(%s) is %d", platformName, platformId, wantList.size());
        if (this.isPlatformCheckOngoing) {
            logger.error(String.format("start platform=%s app data collect FAIL : some collect task is ongoing", platformId));
            throw new ClientCollectDataException(String.format("start platform=%s app data collect FAIL : some collect task is ongoing", platformId));
        }
        this.isPlatformCheckOngoing = true;
        try {
            List<PlatformAppModuleVo> modules = this.platformAppCollectService.updatePlatformAppData(platformId, platformName, wantList);
            List<PlatformAppModuleVo> failList = new ArrayList<>();
            List<PlatformAppModuleVo> successList = new ArrayList<>();
            for (PlatformAppModuleVo collectedModule : modules) {
                if (!collectedModule.isOk(platformId, platformName, this.appSetRelationMap)) {
                    logger.debug(collectedModule.getComment());
                    failList.add(collectedModule);
                } else
                    successList.add(collectedModule);
            }
            List<DomainPo> domainList = this.domainMapper.select(platformId, null);
            Map<String, DomainPo> existDomainMap = domainList.stream().collect(Collectors.toMap(DomainPo::getDomainName, Function.identity()));
            Map<String, List<PlatformAppModuleVo>> domainAppMap = successList.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getDomainName));
            List<String> domainNameList = new ArrayList<>(domainAppMap.keySet());
            List<PlatformAppModuleVo> okList = new ArrayList<>();
            for (String domainName : domainNameList) {
                if (!existDomainMap.containsKey(domainName)) {
                    logger.error(String.format("domain %s not exist", domainName));
                    for (PlatformAppModuleVo moduleVo : domainAppMap.get(domainName)) {
                        moduleVo.setComment(String.format("domain %s not exist", domainName));
                        failList.add(moduleVo);
                    }
                    domainAppMap.remove(domainName);
                } else
                    okList.addAll(domainAppMap.get(domainName));
            }
            logger.debug(String.format("begin to handle collected %d app", okList.size()));
            successList = new ArrayList<>();
            this.appWriteLock.writeLock().lock();
            try {
                for (PlatformAppModuleVo moduleVo : okList) {
                    try {
                        this.appManagerService.preprocessPlatformAppModule(moduleVo);
                        successList.add(moduleVo);
                    } catch (ParamException ex) {
                        moduleVo.setComment(ex.getMessage());
                        failList.add(moduleVo);
                    }
                }
            } finally {
                this.appWriteLock.writeLock().unlock();
            }
            domainAppMap = successList.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getDomainName));
            Map<String, List<PlatformAppDeployDetailVo>> domainDeployMap = this.platformAppDeployDetailMapper.selectPlatformApps(platformId, null, null).stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
            List<AppUpdateOperationInfo> updateList = new ArrayList<>();
            for (String domainName : domainAppMap.keySet()) {
                String domainId = existDomainMap.get(domainName).getDomainId();
                Map<String, PlatformAppDeployDetailVo> origAliasMap = domainDeployMap.get(domainId).stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity()));
                for (PlatformAppModuleVo moduleVo : domainAppMap.get(domainName)) {
                    boolean isAdd = origAliasMap.containsKey(moduleVo.getModuleAliasName()) ? false : true;
                    AppUpdateOperationInfo optInfo = new AppUpdateOperationInfo();
                    List<AppFileNexusInfo> cfgs = new ArrayList<>();
                    if (moduleVo.getCfgs() != null && moduleVo.getCfgs().length > 0) {
                        for (DeployFileInfo fileInfo : moduleVo.getCfgs()) {
                            AppFileNexusInfo cfg = new AppFileNexusInfo();
                            cfg.setDeployPath(fileInfo.getDeployPath());
                            cfg.setExt(fileInfo.getExt());
                            cfg.setFileName(fileInfo.getFileName());
                            cfg.setMd5(fileInfo.getFileMd5());
                            cfg.setNexusAssetId(fileInfo.getNexusAssetId());
                            cfg.setNexusPath(String.format("%s/%s", fileInfo.getNexusDirectory(), fileInfo.getFileName()));
                            cfg.setNexusRepository(fileInfo.getNexusRepository());
                            cfgs.add(cfg);
                        }
                    }
                    optInfo.setCfgs(cfgs);
                    optInfo.setAppRunner(moduleVo.getLoginUser());
                    optInfo.setVersion(moduleVo.getVersion());
                    optInfo.setAppName(moduleVo.getModuleName());
                    optInfo.setBasePath(moduleVo.getBasePath());
                    optInfo.setHostIp(moduleVo.getHostIp());
                    optInfo.setDomainId(domainId);
                    if (isAdd) {
                        optInfo.setAlias(moduleVo.getModuleAliasName());
                        optInfo.setOperation(AppUpdateOperation.ADD);
                    } else {
                        optInfo.setOriginalAlias(moduleVo.getModuleAliasName());
                        optInfo.setOperation(AppUpdateOperation.UPDATE);
                    }
                    updateList.add(optInfo);
                }
            }
            this.updatePlatformApps(platformId, platformName, updateList);
            for (PlatformAppModuleVo moduleVo : failList) {
                try {
                    UnconfirmedAppModulePo unconfirmedAppModulePo = handleUnconfirmedPlatformAppModule(moduleVo);
                    this.unconfirmedAppModuleMapper.insert(unconfirmedAppModulePo);
                } catch (Exception ex) {
                    logger.error(String.format("handle unconfirmed app exception"), ex);
                }

            }
            this.paasService.syncClientCollectResultToPaas(platformPo.getBkBizId(), platformId, platformPo.getBkCloudId());
            platformPo.setStatus(CCODPlatformStatus.RUNNING);
            this.platformMapper.update(platformPo);
            return modules.toArray(new PlatformAppModuleVo[0]);
        } finally {
            this.isPlatformCheckOngoing = false;
        }
    }

    @Override
    public PlatformTopologyInfo getPlatformTopologyFromK8s(String platformName, String platformId, int bkBizId, int bkCloudId, String ccodVersion, String hostIp, String k8sApiUrl, String k8sAuthToken, PlatformFunction func) throws ApiException, ParamException, NotSupportAppException, NexusException, LJPaasException, InterfaceCallException, IOException, K8sDataException {
        logger.debug(String.format("begin to get %s(%s) topology from %s with authToke=%s", platformId, platformName, k8sApiUrl, k8sAuthToken));
        Date now = new Date();
        V1Namespace ns = ik8sApiService.readNamespace(platformId, k8sApiUrl, k8sAuthToken);
        if (!"Active".equals(ns.getStatus().getPhase())) {
            logger.error(String.format("status of %s is %s", platformId, ns.getStatus().getPhase()));
            throw new ParamException(String.format("status of %s is %s", platformId, ns.getStatus().getPhase()));
        }
        List<V1Deployment> deploymentList = ik8sApiService.listNamespacedDeployment(platformId, k8sApiUrl, k8sAuthToken);

        List<V1Service> serviceList = ik8sApiService.listNamespacedService(platformId, k8sApiUrl, k8sAuthToken);
        List<V1ConfigMap> configs = ik8sApiService.listNamespacedConfigMap(platformId, k8sApiUrl, k8sAuthToken);
        List<AppModuleVo> registerApps = this.appManagerService.queryAllRegisterAppModule(true);
        Map<String, V1ConfigMap> configMapMap = new HashMap<>();
        for (V1ConfigMap configMap : configs)
            configMapMap.put(configMap.getMetadata().getName(), configMap);

        List<PlatformAppDeployDetailVo> deployAppList = new ArrayList<>();
        Map<String, List<V1Service>> serviceMap = new HashMap<>();
        Map<String, V1Deployment> serviceDeploymentMap = new HashMap<>();
        List<PlatformThreePartAppPo> threeAppList = new ArrayList<>();
        List<PlatformThreePartServicePo> threePartSvcList = new ArrayList<>();
        for (V1Service service : serviceList) {
            String serviceName = service.getMetadata().getName();
            if (!service.getMetadata().getLabels().containsKey(this.serviceTypeLabel))
                throw new K8sDataException(String.format("service %s with labels %s is unknown", service.getMetadata().getName(), gson.toJson(service.getMetadata().getLabels())));
            List<V1Deployment> relativeDeployments = getServiceDeployment(service, deploymentList);
            if (relativeDeployments.size() > 1)
                throw new K8sDataException(String.format("service %s relative to multi deployment %s",
                        serviceName, String.join(",", relativeDeployments.stream().collect(Collectors.toMap(V1Deployment::getMetadata, Function.identity())).keySet().stream().collect(Collectors.toMap(V1ObjectMeta::getName, Function.identity())).keySet())));
            String serviceType = service.getMetadata().getLabels().get(this.serviceTypeLabel);
            if (serviceType.equals(K8sServiceType.THREE_PART_APP.name)) {
                if (relativeDeployments.size() == 0)
                    throw new K8sDataException(String.format("%s %s not relative to any deployment", serviceType, serviceName));
                PlatformThreePartAppPo po = getPlatformThreePartApp4FromK8s(platformId, service, relativeDeployments.get(0), hostIp);
                threeAppList.add(po);
            } else if (serviceType.equals(K8sServiceType.THREE_PART_SERVICE.name)) {
                if (relativeDeployments.size() != 0)
                    throw new K8sDataException(String.format("%s %s is relative to deployment %s", serviceType, serviceName, relativeDeployments.get(0).getMetadata().getName()));
                PlatformThreePartServicePo po = getPlatformThreePartService4FromK8s(platformId, service);
                threePartSvcList.add(po);
            } else if (serviceType.equals(K8sServiceType.DOMAIN_SERVICE.name)) {
                if (relativeDeployments.size() == 0)
                    throw new K8sDataException(String.format("%s %s not relative to any deployment", serviceType, serviceName));
                if (!serviceMap.containsKey(serviceName))
                    serviceMap.put(serviceName, new ArrayList<>());
                serviceMap.get(serviceName).add(service);
                serviceDeploymentMap.put(serviceName, relativeDeployments.get(0));
            } else if (serviceType.equals(K8sServiceType.DOMAIN_OUT_SERVICE.name)) {
                if (relativeDeployments.size() == 0)
                    throw new K8sDataException(String.format("%s %s not relative to any deployment", serviceType, serviceName));
                String[] arr = serviceName.split("-");
                if (arr.length <= 2)
                    throw new K8sDataException(String.format("%s %s is illegal service name", serviceType, serviceName));
                String domainServiceName = String.format("%s-%s", arr[0], arr[1]);
                if (!serviceMap.containsKey(domainServiceName))
                    serviceMap.put(domainServiceName, new ArrayList<>());
                serviceMap.get(domainServiceName).add(service);
                if (!serviceDeploymentMap.containsKey(domainServiceName))
                    serviceDeploymentMap.put(domainServiceName, relativeDeployments.get(0));
            }
        }
        for (String serviceName : serviceMap.keySet()) {
            PlatformAppDeployDetailVo deployApp = getAppDeployDetail(serviceMap.get(serviceName), serviceDeploymentMap.get(serviceName), configMapMap, registerApps, now);
            deployApp.setHostIp(hostIp);
            deployAppList.add(deployApp);
        }
        logger.debug(String.format("%s(%s) has deployed %d apps", platformName, platformId, deployAppList.size()));
        logger.debug(String.format("%s has %d three part apps", platformId, threeAppList.size()));
        logger.debug(String.format("%s has %d three part service", platformId, threePartSvcList.size()));
        List<DomainPo> allDomains = new ArrayList<>();
        Map<String, List<AssemblePo>> domainAssembleMap = new HashMap<>();
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        for (String domainId : domainAppMap.keySet()) {
            List<PlatformAppDeployDetailVo> domainApps = domainAppMap.get(domainId);
            DomainPo domainPo = new DomainPo();
            domainPo.setType(DomainType.K8S_CONTAINER);
            domainPo.setBizSetName(domainApps.get(0).getBkSetName());
            domainPo.setUpdateTime(now);
            domainPo.setTags("create by k8s api");
            domainPo.setDomainId(domainId);
            domainPo.setOccurs(400);
            domainPo.setMaxOccurs(800);
            domainPo.setComment("create by k8s api");
            domainPo.setPlatformId(platformId);
            domainPo.setStatus(DomainStatus.RUNNING.id);
            domainPo.setCreateTime(now);
            domainPo.setDomainName(domainApps.get(0).getDomainName());
            allDomains.add(domainPo);
            domainAssembleMap.put(domainId, new ArrayList<>());
            Map<String, List<PlatformAppDeployDetailVo>> assembleAppMap = domainApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getAssembleTag));
            for (String assembleTag : assembleAppMap.keySet()) {
                AssemblePo assemblePo = new AssemblePo();
                assemblePo.setDomainId(domainId);
                assemblePo.setPlatformId(platformId);
                assemblePo.setTag(assembleAppMap.get(assembleTag).get(0).getAssembleTag());
                assemblePo.setStatus(assembleAppMap.get(assembleTag).get(0).getStatus());
                domainAssembleMap.get(domainId).add(assemblePo);
            }
        }
        PlatformPo platform = new PlatformPo();
        platform.setStatus(CCODPlatformStatus.RUNNING);
        platform.setPlatformId(platformId);
        platform.setPlatformName(platformName);
        platform.setBkCloudId(bkCloudId);
        platform.setBkBizId(bkBizId);
        platform.setUpdateTime(now);
        platform.setCreateTime(now);
        platform.setComment("create by k8s api");
        platform.setCcodVersion(ccodVersion);
        platform.setK8sApiUrl(k8sApiUrl);
        platform.setK8sAuthToken(k8sAuthToken);
        platform.setType(PlatformType.K8S_CONTAINER);
        platform.setFunc(func);
        platformMapper.insert(platform);
        for (DomainPo domainPo : allDomains) {
            logger.debug(String.format("insert new domain : %s", gson.toJson(domainPo)));
            this.domainMapper.insert(domainPo);
        }
        for (AssemblePo assemblePo : domainAssembleMap.values().stream().flatMap(listContainer -> listContainer.stream()).collect(Collectors.toList())) {
            logger.debug(String.format("insert new assemble : %s", gson.toJson(assemblePo)));
            this.assembleMapper.insert(assemblePo);
        }
        for (PlatformAppDeployDetailVo deployApp : deployAppList) {
            PlatformAppPo platformAppPo = deployApp.getPlatformApp();
            platformAppPo.setAssembleId(domainAssembleMap.get(deployApp.getDomainId()).stream().collect(Collectors.toMap(AssemblePo::getTag, Function.identity())).get(deployApp.getAssembleTag()).getAssembleId());
            logger.debug(String.format("insert new platform app : %s", gson.toJson(platformAppPo)));
            this.platformAppMapper.insert(platformAppPo);
        }
        for (PlatformThreePartAppPo threePartAppPo : threeAppList) {
            logger.debug(String.format("insert new platform three part app : %s", gson.toJson(threePartAppPo)));
            this.platformThreePartAppMapper.insert(threePartAppPo);
        }
        for (PlatformThreePartServicePo threePartServicePo : threePartSvcList) {
            logger.debug(String.format("insert new platform three part service : %s", gson.toJson(threePartServicePo)));
            this.platformThreePartServiceMapper.insert(threePartServicePo);
        }
        ljPaasService.syncClientCollectResultToPaas(bkBizId, platformId, bkCloudId);
        return getPlatformTopology(platformId);
    }

    /**
     * 比较两个选择器是否相同
     *
     * @param selector1 选择器1
     * @param selector2 选择器2
     * @return 比较结果
     */
    private boolean isSelectorEqual(Map<String, String> selector1, Map<String, String> selector2) {
        if (selector1.size() != selector2.size())
            return false;
        for (String key : selector1.keySet()) {
            if (!selector2.containsKey(key) || !selector2.get(key).equals(selector1.get(key)))
                return false;
        }
        return true;
    }

    /**
     * 找出服务关联的deployment列表
     *
     * @param service        指定服务
     * @param deploymentList 命名空间内所有的deployment
     * @return 指定服务关联的服务列表
     */
    private List<V1Deployment> getServiceDeployment(V1Service service, List<V1Deployment> deploymentList) {
        List<V1Deployment> retList = new ArrayList<>();
        if (service.getSpec().getSelector() != null && service.getSpec().getSelector().size() > 0) {
            for (V1Deployment deployment : deploymentList) {
                boolean isSelected = isSelected(deployment.getSpec().getTemplate().getMetadata().getLabels(), service.getSpec().getSelector());
                if (isSelected)
                    retList.add(deployment);
            }
        }
        logger.debug(String.format("service %s select %d deployment", service.getMetadata().getName(), retList.size()));
        return retList;
    }

    private PlatformAppDeployDetailVo getAppDeployDetail(List<V1Service> services, V1Deployment deployment, Map<String, V1ConfigMap> configMapMap, List<AppModuleVo> registerApps, Date date) throws ParamException, InterfaceCallException, NexusException, IOException {
        String domainId = services.get(0).getMetadata().getLabels().get(this.domainIdLabel);
        String appName = services.get(0).getMetadata().getLabels().get(this.appNameLabel);
        String alias = services.get(0).getMetadata().getLabels().get(this.appAliasLabel);
        String version = services.get(0).getMetadata().getLabels().get(this.appVersionLabel);
        String jobId = deployment.getMetadata().getLabels().get(this.jobIdLabel);
        BizSetDefine setDefine = getBizSetForDomainId(domainId);
        Map<String, AppModuleVo> versionMap = registerApps.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName))
                .get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
        AppModuleVo appModule = versionMap.containsKey(version) ? versionMap.get(version) : versionMap.get(version.replaceAll("-", ":"));
        PlatformAppDeployDetailVo deployApp = new PlatformAppDeployDetailVo();
        deployApp.setBkSetName(setDefine.getName());
        deployApp.setDomainName(String.format("%s%s", setDefine.getFixedDomainName(), domainId.replaceAll(setDefine.getFixedDomainId(), "")));
        deployApp.setDomainId(domainId);
        deployApp.setAlias(alias);
        deployApp.setAppId(appModule.getAppId());
        deployApp.setAppName(appModule.getAppName());
        deployApp.setAppRunner(deployApp.getAlias());
        deployApp.setAppType(appModule.getAppType());
        deployApp.setAssembleTag(deployment.getMetadata().getLabels().get("tag"));
        deployApp.setAssembleId(0);
        if (AppType.BINARY_FILE.equals(deployApp.getAppType()))
            deployApp.setBasePath("/binary-file");
        else
            deployApp.setBasePath("/war");
        deployApp.setBkModuleId(0);
        deployApp.setBkSetId(0);
        deployApp.setCfgs(new ArrayList<>());
        Date now = new Date();
        deployApp.setCreateTime(now);
        deployApp.setDeployTime(now);
        deployApp.setInstallPackage(appModule.getInstallPackage());
        deployApp.setOriginalAlias(deployApp.getAlias());
        deployApp.setPlatformAppId(0);
        String port = getPortFromK8sService(services);
        deployApp.setPorts(port);
        int replicas = deployment.getStatus().getReplicas() != null ? deployment.getStatus().getReplicas() : 0;
        int availableReplicas = deployment.getStatus().getAvailableReplicas() != null ? deployment.getStatus().getAvailableReplicas() : 0;
        int unavailableReplicas = deployment.getStatus().getUnavailableReplicas() != null ? deployment.getStatus().getUnavailableReplicas() : 0;
        deployApp.setReplicas(replicas);
        deployApp.setAvailableReplicas(availableReplicas);
        String status = "Running";
        if (unavailableReplicas == replicas)
            status = "Error";
        else if (availableReplicas < replicas)
            status = "Updating";
        deployApp.setStatus(status);
        deployApp.setVersion(appModule.getVersion());
        if (appModule.getVersionControl() != null)
            deployApp.setVersionControl(appModule.getVersionControl().name);
        deployApp.setVersionControlUrl(appModule.getVersionControlUrl());
        deployApp.setPlatformId(services.get(0).getMetadata().getNamespace());
        V1ConfigMap configMap = configMapMap.get(String.format("%s-%s", alias, domainId));
        V1Container runtimeContainer = null;
        V1Container initContainer = null;
        for (V1Container container : deployment.getSpec().getTemplate().getSpec().getContainers()) {
            if (container.getName().equals(String.format("%s-runtime", alias)))
                runtimeContainer = container;
        }
        for (V1Container container : deployment.getSpec().getTemplate().getSpec().getInitContainers()) {
            if (container.getName().equals(alias))
                initContainer = container;
        }
        AppType appType = appModule.getAppType();
        String volumeName = AppType.BINARY_FILE.equals(appType) ? this.binaryFileVolumeName : this.webappVolumeName;
        String basePath = initContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get(volumeName).getMountPath();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String directory = String.format("%s/%s/%s/%s/%s/%s", services.get(0).getMetadata().getNamespace(), sf.format(date),
                domainId, appName, alias, version.replaceAll("\\:", "-"));
        System.out.println(directory);
        List<NexusAssetInfo> assets = uploadK8sConfigMapToNexus(configMap, directory);
        String[] commands = initContainer.getCommand().get(2).split(";");
        Map<String, String> cfgComandMap = new HashMap<>();
        String cfgCmdRegex = AppType.BINARY_FILE.equals(appModule.getAppType()) ? "^cp .*" : "^jar uf .*";
        for (String command : commands) {
            if (command.matches(cfgCmdRegex)) {
                if (appType.equals(AppType.BINARY_FILE)) {
                    String targetPath = command.split("\\s+")[2];
                    String[] arr = targetPath.split("/");
                    String fileName = arr[arr.length - 1];
                    String deployPath = targetPath.replaceAll(String.format("^%s", basePath), "").replaceAll(String.format("/%s$", fileName), "");
                    deployPath = String.format("./%s", deployPath).replaceAll("//", "/");
                    cfgComandMap.put(fileName, deployPath);
                } else {
                    String targetPath = command.split("\\s+")[3];
                    String[] arr = targetPath.split("/");
                    String fileName = arr[arr.length - 1];
                    String deployPath = targetPath.replaceAll(String.format("/%s$", fileName), "");
                    cfgComandMap.put(fileName, deployPath);
                }
            }
        }
        List<AppFileNexusInfo> cfgs = assets.stream().map(a->new AppFileNexusInfo(a, cfgComandMap.get(a.getNexusAssetFileName()))).collect(Collectors.toList());
        deployApp.setCfgs(cfgs);
        basePath = runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get(volumeName).getMountPath();
        deployApp.setBasePath(basePath);
        String pkgDeployPath = null;
        String runCmdRegex = appType.equals(AppType.BINARY_FILE) ? ".+/startup.sh$" : String.format("[^\\s]*%s($|\\s.+)", appModule.getInstallPackage().getFileName());
        for (String command : commands) {
            if (command.matches(runCmdRegex)) {
                String[] arr = command.split("\\s+");
                pkgDeployPath = appType.equals(AppType.BINARY_FILE) ? arr[0].replaceAll(String.format("%s$", appModule.getInstallPackage().getFileName()), "") : arr[0].replaceAll("startup.sh$", "");
                pkgDeployPath = pkgDeployPath.replaceAll("/$", "");
                if (pkgDeployPath.matches(String.format("^%s.*", basePath))) {
                    pkgDeployPath = pkgDeployPath.replaceAll(String.format("^%s.*", basePath), "");
                    pkgDeployPath = StringUtils.isBlank(pkgDeployPath) ? "./" : String.format(".%s", pkgDeployPath);
                }
                deployApp.getInstallPackage().setDeployPath(pkgDeployPath);
            }
        }
        return deployApp;
    }

    /**
     * 从服务信息中获取端口信息
     *
     * @param services k8s一组服务
     * @return 使用的端口信息
     */
    private String getPortFromK8sService(List<V1Service> services) {
        String port = "";
        for (V1Service service : services) {
            for (V1ServicePort svcPort : service.getSpec().getPorts()) {
                if (svcPort.getNodePort() != null && svcPort.getNodePort() > 0)
                    port = String.format("%s%s:%s/%s;", port, svcPort.getPort(), svcPort.getNodePort(), svcPort.getProtocol());
                else
                    port = String.format("%s%s/%s;", port, svcPort.getPort(), svcPort.getProtocol());
            }
        }
        port = port.replaceAll(";$", "");
        return port;
    }

    /**
     * 获得服务的nodePort
     *
     * @param service 指定服务
     * @return 该服务的nodePort
     */
    private int getNodePortFromK8sService(V1Service service) {
        String strPort = getPortFromK8sService(Arrays.asList(service));
        return Integer.parseInt(strPort.split("\\:")[1].split("/")[0]);
    }

    /**
     * 从k8s服务以及服务选择的pod中获取第三方应用的状态
     *
     * @param namespace 命名空间及平台id
     * @param service   服务信息
     * @return 第三方应用状态
     * @throws ParamException
     */
    private PlatformThreePartServicePo getPlatformThreePartService4FromK8s(String namespace, V1Service service) throws ParamException {
        PlatformThreePartServicePo po = new PlatformThreePartServicePo();
        po.setHostIp(null);
        po.setPlatformId(namespace);
        po.setServiceName(service.getMetadata().getName());
        return po;
    }

    /**
     * 从k8s服务以及服务选择的pod中获取第三方应用的状态
     *
     * @param namespace  命名空间及平台id
     * @param service    服务信息
     * @param deployment 服务选择的deployment
     * @return 第三方应用状态
     * @throws ParamException
     */
    private PlatformThreePartAppPo getPlatformThreePartApp4FromK8s(String namespace, V1Service service, V1Deployment deployment, String hostIp) throws ParamException {
        String appName = service.getMetadata().getLabels().get(this.appNameLabel);
        logger.debug(String.format("begin to get three part app %s info", appName));
        String port = getPortFromK8sService(Arrays.asList(service));
        PlatformThreePartAppPo po = new PlatformThreePartAppPo();
        po.setAppName(appName);
        po.setHostIp(hostIp);
        po.setPlatformId(namespace);
        po.setPort(port);
        int replicas = deployment.getStatus().getReplicas() != null ? deployment.getStatus().getReplicas() : 0;
        int availableReplicas = deployment.getStatus().getAvailableReplicas() != null ? deployment.getStatus().getAvailableReplicas() : 0;
        int unavailableReplicas = deployment.getStatus().getUnavailableReplicas() != null ? deployment.getStatus().getUnavailableReplicas() : 0;
        po.setReplicas(replicas);
        po.setAvailableReplicas(availableReplicas);
        String status = "Running";
        if (unavailableReplicas == replicas)
            status = "Error";
        else if (availableReplicas < replicas)
            status = "Updating";
        po.setStatus(status);
        logger.debug(String.format("three part app %s : %s", appName, gson.toJson(po)));
        return po;
    }

    @Override
    public void deletePlatform(String platformId) throws ParamException {
        logger.debug(String.format("begin to delete %s from cmdb", platformId));
        PlatformPo platformPo = platformMapper.selectByPrimaryKey(platformId);
        if (platformPo == null) {
            logger.error(String.format("%s not exist", platformId));
            throw new ParamException(String.format("%s not exist", platformId));
        }
        if (this.platformUpdateSchemaMap.containsKey(platformId)) {
            this.platformUpdateSchemaMap.remove(platformId);
        }
        this.platformUpdateSchemaMapper.delete(platformId);
        this.unconfirmedAppModuleMapper.delete(platformId);
        this.platformThreePartServiceMapper.delete(platformId, null);
        this.platformThreePartAppMapper.delete(platformId, null);
        this.platformAppBkModuleMapper.delete(null, null, platformId, null);
        List<PlatformAppPo> deployApps = platformAppMapper.select(platformId, null, null, null, null, null);
        this.platformAppMapper.delete(null, platformId, null);
        this.assembleMapper.delete(platformId, null, null);
        this.domainMapper.delete(null, platformId);
        this.platformMapper.delete(platformId);
        logger.info(String.format("%s delete success", platformId));
    }

    @Override
    public void createNewPlatformAppDataUpdateTask(String platformId, String platformName) throws Exception {
        logger.info(String.format("begin to create %s(%s) platform app update task",
                platformName, platformId));
        PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
        if (platformPo == null) {
            logger.error(String.format("create platform app update task fail  : %s not exist", platformId));
            throw new ParamException(String.format("create platform app update task fail  : %s not exist", platformId));
        }
        if (!platformPo.getPlatformName().equals(platformName)) {
            logger.error(String.format("name of %s is %s not %s", platformId, platformPo.getPlatformName(), platformName));
            throw new ParamException(String.format("name of %s is %s not %s", platformId, platformPo.getPlatformName(), platformName));
        }
        if (this.isPlatformCheckOngoing) {
            logger.error(String.format("create platform collect task FaIL : some collect task is ongoing"));
            throw new Exception(String.format("create platform collect task FaIL : some collect task is ongoing"));
        }
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Thread taskThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startCollectPlatformAppUpdateData(platformId, platformName);
                } catch (Exception ex) {
                    logger.error(String.format("collect %s(%s) update task exception",
                            platformName, platformId), ex);
                }
            }
        });
        executor.execute(taskThread);
        executor.shutdown();
    }

    @Override
    public List<AppFileNexusInfo> queryPlatformAppCfgs(String platformId, String domainId, String alias) throws ParamException {
        logger.debug(String.format("query cfg of %s at %s in %s", alias, domainId, platformId));
        PlatformPo platform = this.platformMapper.selectByPrimaryKey(platformId);
        if(platform == null)
            throw new ParamException(String.format("platform %s not exist", platformId));
        DomainPo domain = this.domainMapper.selectByPrimaryKey(platformId, domainId);
        if(domain == null)
            throw new ParamException(String.format("domain %s of %s not exist", domainId, platformId));
        List<PlatformAppPo> deploys = this.platformAppMapper.select(platformId, domainId, null, null, alias, null);
        if(deploys.size() == 0)
            throw new ParamException(String.format("%s at %s in %s not exist", alias, domainId, platformId));
        else if(deploys.size() > 1)
            throw new ParamException(String.format("data error : %s duplicate at %s in %s", alias, domainId, platformId));
        logger.info(String.format("%s at %s in %s find %d cfgs : ", alias, domainId, platformId, deploys.get(0).getCfgs().size()));
        return deploys.get(0).getCfgs();
    }

    @Override
    public void modifyK8sPlatformAppCfg(String platformId, String domainId, String alias, List<AppFileNexusInfo> newCfgs) throws ParamException, InterfaceCallException, NexusException, ApiException, IOException {
        String configMapName = String.format("%s-%s", alias, domainId);
        logger.debug(String.format("modify cfgs %s in %s to %s", configMapName, platformId, gson.toJson(newCfgs)));
        List<AppFileNexusInfo> oldCfgs = queryPlatformAppCfgs(platformId, domainId, alias);
        if(oldCfgs.size() != newCfgs.size())
            throw new ParamException(String.format("cfg count of %s at %s error, want %d, offer %d", alias, domainId, oldCfgs.size(), newCfgs.size()));
        Map<String, List<AppFileNexusInfo>> newCfgMap = newCfgs.stream().collect(Collectors.groupingBy(AppFileNexusInfo::getFileName));
        Map<String, AppFileNexusInfo> oldCfgMap = oldCfgs.stream().collect(Collectors.toMap(AppFileNexusInfo::getFileName, Function.identity()));
        for(String fileName : oldCfgMap.keySet())
        {
            if(!newCfgMap.containsKey(fileName))
                throw new ParamException(String.format("want cfg %s of %s at %s not offer", fileName, alias, domainId));
            else if(newCfgMap.get(fileName).size() > 1)
                throw new ParamException(String.format("cfg %s of %s at %s multi defile", fileName, alias, domainId));
        }
        PlatformPo platform = getK8sPlatform(platformId);
        V1ConfigMap oldConfigMap = this.k8sApiService.readNamespacedConfigMap(configMapName, platformId, platform.getK8sApiUrl(), platform.getK8sAuthToken());

        V1ConfigMap newConfigMap = this.k8sApiService.getConfigMapFromNexus(platformId, configMapName,
                newCfgs.stream().map(cfg -> cfg.getNexusAssetInfo(this.nexusHostUrl)).collect(Collectors.toList()),
                this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
        List<V1Deployment> deployments = this.k8sApiService.listNamespacedDeployment(platformId, platform.getK8sApiUrl(), platform.getK8sAuthToken());
        for(V1Deployment deployment : deployments)
        {
            if(!deployment.getSpec().getSelector().getMatchLabels().containsKey(this.domainIdLabel)
                || !deployment.getSpec().getSelector().getMatchLabels().get(this.domainIdLabel).equals(domainId))
                continue;

        }

        logger.debug(String.format("modify config %s from %s to %s", configMapName, gson.toJson(oldConfigMap), gson.toJson(newConfigMap)));
        this.k8sApiService.replaceNamespacedConfigMap(configMapName, platformId, newConfigMap, platform.getK8sApiUrl(), platform.getK8sAuthToken());



    }

    @Override
    public void updatePlatformUpdateSchema(PlatformUpdateSchemaInfo updateSchema) throws NotSupportSetException, NotSupportAppException, ParamException, InterfaceCallException, LJPaasException, NexusException, IOException, ApiException, K8sDataException, ClassNotFoundException, SQLException {
        logger.debug(String.format("begin to update platform update schema : %s", gson.toJson(updateSchema)));
//        resetSchema(updateSchema);
//        logger.warn(gson.toJson(updateSchema));
        PlatformUpdateTaskType taskType = updateSchema.getTaskType();
        UpdateStatus status = updateSchema.getStatus();
        checkPlatformBase(updateSchema, taskType, updateSchema.getDomainUpdatePlanList(), status);
        PlatformUpdateRecordPo rcd = this.platformUpdateRecordMapper.selectByJobId(updateSchema.getSchemaId());
        if(rcd != null)
            throw new ParamException(String.format("schema id %s has been used", updateSchema.getSchemaId()));
        PlatformPo platformPo = platformMapper.selectByPrimaryKey(updateSchema.getPlatformId());
        if(!taskType.equals(PlatformUpdateTaskType.CREATE) && platformPo == null)
            throw new ParamException(String.format("%s platform %s not exist", taskType.name, platformPo.getPlatformId()));
        else if(taskType.equals(PlatformUpdateTaskType.CREATE))
        {
            LJBizInfo bkBiz = paasService.queryBizInfoById(updateSchema.getBkBizId());
            if (bkBiz == null)
                throw new ParamException(String.format("bkBizId=%d biz not exist", updateSchema.getBkBizId()));
            if (!updateSchema.getPlatformName().equals(bkBiz.getBkBizName()))
                throw new ParamException(String.format("bkBizName of bizBkId is %s, not %s", updateSchema.getBkBizId(), bkBiz.getBkBizName(), updateSchema.getPlatformName()));
            if (platformPo == null) {
                if(this.platformMapper.selectByNameBizId(updateSchema.getPlatformName(), null) != null)
                    throw new ParamException(String.format("platform %s has exist", updateSchema.getPlatformName()));
            }
            else {
                if(!platformPo.getPlatformName().equals(updateSchema.getPlatformName()))
                    throw new ParamException(String.format("platform %s exist and name is %s not %s",
                            updateSchema.getPlatformId(), platformPo.getPlatformName(), updateSchema.getPlatformName()));
                if(!platformPo.getStatus().equals(CCODPlatformStatus.SCHEMA_CREATE))
                    throw new ParamException(String.format("%s(%s) exist and status is %s not SCHEMA_CREATE",
                            updateSchema.getPlatformName(), updateSchema.getPlatformId(), platformPo.getStatus().name));
            }
        }
        List<DomainPo> domainList = this.domainMapper.select(updateSchema.getPlatformId(), null);
        Boolean hasImage = PlatformType.K8S_CONTAINER.equals(updateSchema.getType()) ? true : null;
        List<AppModuleVo> registerApps = this.appManagerService.queryAllRegisterAppModule(hasImage);
        boolean clone = PlatformCreateMethod.CLONE.equals(updateSchema.getCreateMethod()) || PlatformCreateMethod.K8S_API_CLONE.equals(updateSchema.getCreateMethod())  ? true : false;
        List<PlatformAppDeployDetailVo> platformDeployApps = this.platformAppDeployDetailMapper.selectPlatformApps(updateSchema.getPlatformId(), null, null);
        logger.debug("begin check param of schema");
        int bkBizId = platformPo == null ? updateSchema.getBkBizId() : platformPo.getBkBizId();
        List<LJHostInfo> bkHostList = this.paasService.queryBKHost(bkBizId, null, null, null, null);
        Set<Integer> cloudSet = bkHostList.stream().flatMap(h-> Arrays.stream(h.getClouds())).map(c->c.getId()).collect(Collectors.toSet());
        Assert.isTrue(cloudSet.size()==1, String.format("cloudId of %s not unique", updateSchema.getPlatformId()));
        int bkCloudId = platformPo == null ? updateSchema.getBkCloudId() : platformPo.getBkCloudId();
        Assert.isTrue(cloudSet.contains(bkCloudId), String.format("cloudId is %d not %d", (new ArrayList<Integer>(cloudSet)).get(0), updateSchema.getBkCloudId()));
        String ccodVersion = platformPo == null ? updateSchema.getCcodVersion() : platformPo.getCcodVersion();
        String platformCheckResult = checkPlatformUpdateSchema(ccodVersion, updateSchema, domainList, platformDeployApps, bkHostList, registerApps);
        Assert.isTrue(StringUtils.isBlank(platformCheckResult), platformCheckResult);
        logger.debug("schema param check success");
        if(taskType.equals(PlatformUpdateTaskType.CREATE))
        {
            boolean needInsert = platformPo == null ? true : false;
            platformPo = updateSchema.getCreatePlatform(updateSchema.getComment());
            if(needInsert)
                this.platformMapper.insert(platformPo);
            else
                this.platformMapper.update(platformPo);
        }
        List<AssemblePo> assembleList = this.assembleMapper.select(updateSchema.getPlatformId(), null);
        makeupDomainIdAndAliasForSchema(updateSchema, domainList, platformDeployApps, clone);
        if (status.equals(UpdateStatus.EXEC) || status.equals(UpdateStatus.WAIT_EXEC))
            execPlatformSchema(platformPo, updateSchema, domainList, assembleList, platformDeployApps, registerApps);
        boolean closeSchema = status.equals(UpdateStatus.EXEC) && updateSchema.getDomainUpdatePlanList().size() == 0 ? true : false;
        if (this.platformUpdateSchemaMap.containsKey(updateSchema.getPlatformId()))
            this.platformUpdateSchemaMap.remove(updateSchema.getPlatformId());
        this.platformUpdateSchemaMapper.delete(updateSchema.getPlatformId());
        if(closeSchema) {
            logger.info(String.format("schema of %s has completed and status change to %s",
                    platformPo.getPlatformId(), CCODPlatformStatus.RUNNING.name));
            platformPo.setStatus(CCODPlatformStatus.RUNNING);
        }
        else
        {
            PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
            schemaPo.setContext(gson.toJson(updateSchema).getBytes());
            schemaPo.setPlatformId(updateSchema.getPlatformId());
            this.platformUpdateSchemaMapper.insert(schemaPo);
            this.platformUpdateSchemaMap.put(updateSchema.getPlatformId(), updateSchema);
            if(updateSchema.getTaskType().equals(PlatformUpdateTaskType.CREATE))
                platformPo.setStatus(CCODPlatformStatus.SCHEMA_CREATE);
            else
                platformPo.setStatus(CCODPlatformStatus.SCHEMA_UPDATE);
        }
        this.platformMapper.update(platformPo);
        logger.info(String.format("update %s schema success", updateSchema.getSchemaId()));
    }

    private Comparator<DomainUpdatePlanInfo> getDomainPlanSort()
    {
        Map<String, Integer> setMap = new HashMap<>();
        for(int i = 0; i < this.ccodBiz.getSet().size(); i++)
            setMap.put(this.ccodBiz.getSet().get(i).getFixedDomainId(), i);
        Comparator<DomainUpdatePlanInfo> sort = new Comparator<DomainUpdatePlanInfo>() {
            @Override
            public int compare(DomainUpdatePlanInfo o1, DomainUpdatePlanInfo o2) {
                String id1 = o1.getDomainId().replaceAll("\\d*", "");
                String id2 = o2.getDomainId().replaceAll("\\d*", "");
                if(!id1.equals(id2))
                    return setMap.get(id1) - setMap.get(id2);
                return Integer.parseInt(o1.getDomainId().replaceAll(id2, "")) - Integer.parseInt(o2.getDomainId().replaceAll(id1, "")) ;
            }
        };
        return sort;
    }

    private Comparator<AppBase> getAppSort(BizSetDefine setDefine)
    {
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

    private void execPlatformSchema(
            PlatformPo platformPo, PlatformUpdateSchemaInfo schema, List<DomainPo> domainList,
            List<AssemblePo> assembleList, List<PlatformAppDeployDetailVo> platformDeployApps,
            List<AppModuleVo> registerApps)
            throws ParamException, ApiException, InterfaceCallException, IOException, LJPaasException, NotSupportAppException, NexusException
    {
        Map<String, Integer> setMap = new HashMap<>();
        for(int i = 0; i < this.ccodBiz.getSet().size(); i++)
            setMap.put(this.ccodBiz.getSet().get(i).getFixedDomainId(), i);
        Comparator<DomainUpdatePlanInfo> sort = new Comparator<DomainUpdatePlanInfo>() {
            @Override
            public int compare(DomainUpdatePlanInfo o1, DomainUpdatePlanInfo o2) {
                String id1 = o1.getDomainId().replaceAll("\\d*", "");
                String id2 = o2.getDomainId().replaceAll("\\d*", "");
                if(!id1.equals(id2))
                    return setMap.get(id1) - setMap.get(id2);
                return Integer.parseInt(o1.getDomainId().replaceAll(id2, "")) - Integer.parseInt(o2.getDomainId().replaceAll(id1, "")) ;
            }
        };
        UpdateStatus status = schema.getStatus();
        List<DomainUpdatePlanInfo> plans = schema.getDomainUpdatePlanList().stream().
                filter(plan->plan.getStatus().equals(status)).sorted(sort).collect(Collectors.toList());
        boolean isNewPlatform = schema.getTaskType().equals(PlatformUpdateTaskType.CREATE) ? true : false;
        List<K8sOperationInfo> steps = new ArrayList<>();
        List<AppFileNexusInfo> platformCfg = isNewPlatform ? schema.getCfgs() : platformPo.getCfgs();
        String platformId = platformPo.getPlatformId();
        String ccodVersion = platformPo.getCcodVersion();
        String jobId = schema.getSchemaId();
        String k8sApiUrl = platformPo.getK8sApiUrl();
        String k8sAuthToken = platformPo.getK8sAuthToken();
        String hostUrl = platformPo.getHostUrl();
        Map<String, DomainPo> domainMap = domainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        boolean clone = PlatformCreateMethod.CLONE.equals(schema.getCreateMethod()) ? true : false;
        schema.getDomainUpdatePlanList().stream()
                .forEach(plan -> plan.getAppUpdateOperationList().stream().forEach(opt->opt.setDomainId(plan.getDomainId())));
        PlatformAppDeployDetailVo deployGls;
        if(isNewPlatform) {
            List<K8sOperationInfo> platformCreateSteps = this.k8sTemplateService.generatePlatformCreateSteps(ccodVersion,
                    platformId, hostUrl, jobId, schema.getK8sJob(), schema.getNamespace(), schema.getK8sSecrets(),
                    null, null, schema.getThreePartApps(), schema.getThreePartServices(), platformCfg,
                    k8sApiUrl, k8sAuthToken);
            steps.addAll(platformCreateSteps);
            Map<String, List<AppUpdateOperationInfo>> optMap = schema.getDomainUpdatePlanList().stream()
                    .flatMap(plan->plan.getAppUpdateOperationList().stream()).collect(Collectors.toList())
                    .stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
            if(!optMap.containsKey("glsServer"))
                throw new ParamException(String.format("new platform must has glsServer"));
            else if(optMap.get("glsServer").size() > 1)
                throw new ParamException(String.format("glsServer multi deploy"));
            deployGls = optMap.get("glsServer").get(0).getPlatformAppDetail(platformId, nexusHostUrl);
        }
        else {
            deployGls = platformDeployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getAppName))
                    .get("glsServer").get(0);
        }
        for(DomainUpdatePlanInfo plan : plans)
        {
            if(domainMap.containsKey(plan.getDomainId()) && (plan.getPublicConfig() == null || plan.getPublicConfig().size() == 0))
                plan.setPublicConfig(domainMap.get(plan.getDomainId()).getCfgs());
            List<K8sOperationInfo> deploySteps = generateDomainDeploySteps(jobId, platformPo, platformCfg, plan, isNewPlatform);
            steps.addAll(deploySteps);
        }
        if(!status.equals(UpdateStatus.EXEC))
            return;
        Map<String, List<AssemblePo>> domainAssembleMap = assembleList.stream().collect(Collectors.groupingBy(AssemblePo::getDomainId));
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = platformDeployApps.stream()
                .collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        Map<String, Map<String, List<AppFileNexusInfo>>> domainCfgMap = new HashMap<>();
        Date date = new Date();
        for (DomainUpdatePlanInfo plan : plans) {
            String domainId = plan.getDomainId();
            boolean isCreate = domainMap.containsKey(plan.getDomainId()) ? false : true;
            DomainPo domainPo = StringUtils.isNotBlank(plan.getDomainId()) && domainMap.containsKey(plan.getDomainId()) ? domainMap.get(domainId) : plan.getDomain(platformId);
            List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
            logger.debug(String.format("preprocess %s %d apps with isCreate=%b and %d deployed apps",
                    gson.toJson(domainPo), plan.getAppUpdateOperationList().size(), isCreate, domainAppList.size()));
            Map<String, List<AppFileNexusInfo>> cfgMap = preprocessDomainApps(schema.getPlatformId(), plan.getAppUpdateOperationList(), domainAppList, domainPo, clone, date);
            domainCfgMap.put(domainId, cfgMap);
        }
        PlatformSchemaExecResultVo execResultVo = execPlatformUpdateSteps(platformPo, steps, schema, platformDeployApps, deployGls);
        logger.info(String.format("platform schema execute result : %s", gson.toJson(execResultVo)));
        if(!execResultVo.isSuccess())
            throw new ParamException(String.format("schema execute fail : %s", execResultVo.getErrorMsg()));
        schema.setDomainUpdatePlanList(schema.getDomainUpdatePlanList().stream()
                .filter(plan->!plan.getStatus().equals(UpdateStatus.EXEC)).collect(Collectors.toList()));
        for (DomainUpdatePlanInfo plan : plans) {
            String domainId = plan.getDomainId();
            boolean isCreate = domainMap.containsKey(plan.getDomainId()) ? false : true;
            DomainPo domainPo = isCreate ? plan.getDomain(schema.getPlatformId()) : domainMap.get(domainId);
            if (isCreate)
                this.domainMapper.insert(domainPo);
            List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
            List<AssemblePo> assembles = domainAssembleMap.containsKey(domainId) ? domainAssembleMap.get(domainId) : new ArrayList<>();
            logger.debug(String.format("handle %s %d apps with isCreate=%b and %d deployed apps",
                    gson.toJson(domainPo), plan.getAppUpdateOperationList().size(), isCreate, domainAppList.size()));
            handleDomainApps(platformId, domainPo, plan.getAppUpdateOperationList(), assembles, domainAppList, registerApps, domainCfgMap.get(domainId));
        }
        this.paasService.syncClientCollectResultToPaas(platformPo.getBkBizId(), platformPo.getPlatformId(), platformPo.getBkCloudId());
    }

    private void resetSchema(PlatformUpdateSchemaInfo schema)
    {
        schema.setNamespace(null);
        schema.setK8sSecrets(null);
        schema.setThreePartApps(null);
        schema.setThreePartServices(null);
        Map<String, List<AppModuleVo>> registerAppMap = this.appManagerService.queryAllRegisterAppModule(true).stream()
            .collect(Collectors.groupingBy(AppModuleVo::getAppName));
        for(DomainUpdatePlanInfo plan : schema.getDomainUpdatePlanList())
        {
            plan.setIngresses(null);
            plan.setDeployments(null);
            plan.setServices(null);
            for(AppUpdateOperationInfo optInfo : plan.getAppUpdateOperationList())
            {
                AppModuleVo module = registerAppMap.get(optInfo.getAppName()).stream()
                        .collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(optInfo.getVersion());
                AppType appType = module.getAppType();
                optInfo.setPeriodSeconds(module.getPeriodSeconds());
                optInfo.setInitialDelaySeconds(module.getInitialDelaySeconds());
                optInfo.setResources(module.getResources());
                optInfo.setLogOutputCmd(module.getLogOutputCmd());
                optInfo.setInitCmd(module.getInitCmd());
                optInfo.setStartCmd(module.getStartCmd());
                optInfo.setNodePorts(module.getNodePorts());
                optInfo.setPorts(module.getPorts());
                optInfo.setAssembleTag(String.format("%s-%s", optInfo.getAlias(), plan.getDomainId()));
                String envLoadCmd = StringUtils.isNotBlank(module.getEnvLoadCmd()) ? module.getEnvLoadCmd() : String.format("echo \"hello, %s\"", optInfo.getAlias());
                optInfo.setEnvLoadCmd(envLoadCmd);
                optInfo.setCheckAt(module.getCheckAt());
            }
        }
    }

    @Override
    public PlatformAppDeployDetailVo debugPlatformApp(String platformId, String domainId, AppUpdateOperationInfo optInfo)
            throws ParamException, ApiException, InterfaceCallException, IOException, NotSupportAppException, LJPaasException, NexusException {
        Assert.notNull(optInfo, "debug detail can not be null");
        String appName = optInfo.getAppName();
        String alias = optInfo.getAlias();
        AppUpdateOperation operation = optInfo.getOperation();
        Assert.notNull(appName, "appName of debug can not be null");
        Assert.notNull(alias, "alias of debug app can not be null");
        Assert.isTrue(operation==null || operation.equals(AppUpdateOperation.DEBUG), "operation of debug should be debug");
        PlatformPo platform = getK8sPlatform(platformId);
        Map<String, PlatformAppDeployDetailVo> deployAppMap = this.platformAppDeployDetailMapper.selectPlatformApps(platformId, domainId, null)
                .stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAlias, Function.identity()));
        if(deployAppMap.containsKey(alias))
            optInfo.fill(deployAppMap.get(alias));
        List<AppModuleVo> registerApps = this.appManagerService.queryAllRegisterAppModule(true);
        BizSetDefine setDefine = getBizSetForDomainId(domainId);
        String checkResult = checkAppBase(platform.getCcodVersion(), optInfo, AppUpdateOperation.ADD, setDefine, new ArrayList<>(), registerApps);
        Assert.isTrue(StringUtils.isBlank(checkResult), checkResult);
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String jobId = DigestUtils.md5DigestAsHex(sf.format(now).getBytes());
        boolean isNsExist = this.k8sApiService.isNamespaceExist(platformId, k8sApiUrl, k8sAuthToken);
        Assert.isTrue(isNsExist, String.format("namespace %s not exist at %s", platformId, k8sApiUrl));
        AppModuleVo module = this.appManagerService.queryAppByVersion(appName, optInfo.getVersion(), true);
        optInfo.setAppType(module.getAppType());
        List<AppFileNexusInfo> domainCfg = optInfo.getDomainCfg();
        if(domainCfg == null || domainCfg.size() == 0){
            DomainPo domainPo = this.domainMapper.selectByPrimaryKey(platformId, domainId);
            domainCfg = domainPo != null ? domainPo.getCfgs() : new ArrayList<>();
        }
        List<K8sOperationInfo> steps = this.k8sTemplateService.generateDebugPlatformAppSteps(jobId, optInfo, domainId, domainCfg, platform);;
        for(K8sOperationInfo step : steps) {
            K8sOperationPo execResult = execK8sOpt(step, platformId, platform.getK8sApiUrl(), platform.getK8sAuthToken());
            if(!execResult.isSuccess())
                throw new ParamException(String.format("debug fail : %s", execResult.getComment()));
        }
        logger.info(String.format("debug success"));
        PlatformAppDeployDetailVo deployApp = optInfo.getPlatformAppDetail(platformId, this.nexusHostUrl);
        logger.info(String.format("deploy detail of debug app is %s", gson.toJson(deployApp)));
        return deployApp;
    }


    private int getIndexFromAlias(String alias, String standAlias)
    {
        String indexStr = alias.replace(standAlias, "");
        int index = StringUtils.isBlank(indexStr) ? 0 : Integer.parseInt(indexStr);
        return index;
    }

    private List<K8sOperationInfo> generateDomainDeploySteps(
            String jobId, PlatformPo platformPo, List<AppFileNexusInfo> platformCfg, DomainUpdatePlanInfo plan, boolean isNewPlatform)
            throws ParamException, ApiException, InterfaceCallException, IOException
    {
        String domainId = plan.getDomainId();
        BizSetDefine setDefine = getBizSetForDomainId(domainId);
        Map<String, AppDefine> appDefineMap = setDefine.getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity()));
        Map<String, Integer> appSortMap = new HashMap<>();
        for(int i = 0; i < setDefine.getApps().size(); i++)
            appSortMap.put(setDefine.getApps().get(i).getName(), i);
        Comparator<AppUpdateOperationInfo> sort = new Comparator<AppUpdateOperationInfo>() {
            @Override
            public int compare(AppUpdateOperationInfo o1, AppUpdateOperationInfo o2) {
                if(!o1.getAppName().equals(o2.getAppName()))
                    return appSortMap.get(o1.getAppName()) - appSortMap.get(o2.getAppName());
                return getIndexFromAlias(o1.getAlias(), appDefineMap.get(o1.getAppName()).getAlias()) - getIndexFromAlias(o2.getAlias(), appDefineMap.get(o2.getAppName()).getAlias());
            }
        };
        List<K8sOperationInfo> steps = new ArrayList<>();
        String platformId = platformPo.getPlatformId();
        String k8sApiUrl = platformPo.getK8sApiUrl();
        String k8sAuthToken = platformPo.getK8sAuthToken();
        List<AppFileNexusInfo> domainCfg = plan.getPublicConfig();
        if(!isNewPlatform && domainCfg != null && domainCfg.size() > 0) {
            if(this.ik8sApiService.isNamespacedConfigMapExist(domainId, platformId, k8sApiUrl, k8sAuthToken)) {
                V1ConfigMap configMap = this.k8sApiService.readNamespacedConfigMap(domainId, platformId, k8sApiUrl, k8sAuthToken);
                K8sOperationInfo optInfo = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP, domainId, K8sOperation.DELETE, configMap);
                steps.add(optInfo);
            }
        }
        if(domainCfg != null && domainCfg.size() > 0) {
            V1ConfigMap configMap = this.k8sApiService.getConfigMapFromNexus(platformId, domainId,
                    domainCfg.stream().map(cfg->cfg.getNexusAssetInfo(nexusHostUrl)).collect(Collectors.toList()),
                    nexusHostUrl, nexusUserName, nexusPassword);
            K8sOperationInfo optInfo = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP, domainId, K8sOperation.CREATE, configMap);
            steps.add(optInfo);
        }
        List<AppUpdateOperationInfo> deleteList = plan.getAppUpdateOperationList().stream()
                .filter(opt->opt.getOperation().equals(AppUpdateOperation.DELETE)).sorted(sort.reversed())
                .collect(Collectors.toList());
        for(AppUpdateOperationInfo optInfo : deleteList) {
            List<K8sOperationInfo> deleteSteps = this.k8sTemplateService.getDeletePlatformAppSteps(jobId, optInfo.getAppName(),
                    optInfo.getAlias(), optInfo.getVersion(), domainId, platformId, k8sApiUrl, k8sAuthToken);
            steps.addAll(deleteSteps);
        }
        List<AppUpdateOperationInfo> addAndUpdateList = plan.getAppUpdateOperationList().stream()
                .filter(opt->opt.getOperation().equals(AppUpdateOperation.ADD) || opt.getOperation().equals(AppUpdateOperation.UPDATE))
                .sorted(sort).collect(Collectors.toList());
        for(AppUpdateOperationInfo optInfo : addAndUpdateList) {
            List<K8sOperationInfo> optSteps = optInfo.getOperation().equals(AppUpdateOperation.ADD) ?
                    this.k8sTemplateService.generateAddPlatformAppSteps(jobId, optInfo, domainId, domainCfg, platformPo, isNewPlatform)
                    : this.k8sTemplateService.generateUpdatePlatformAppSteps(jobId, optInfo, domainId, domainCfg, platformPo);
            steps.addAll(optSteps);
        }
        if(!isNewPlatform)
            steps.forEach(s->s.setKernal(false));
        logger.info(String.format("deploy %s %d apps need %d steps", domainId, plan.getAppUpdateOperationList().size(), steps.size()));
        return steps;
    }

    /**
     * 重用已有的k8s服务的nodePort
     * @param svcOptList 需要执行的服务列表
     * @param existServiceList 系统正在运行的k8s服务列表
     */
    void reuseServiceNodePort(List<K8sOperationInfo> svcOptList, List<V1Service> existServiceList)
    {
        logger.debug(String.format("begin to reuse exist nodePort of k8s service"));
        Map<String, List<K8sOperationInfo>> nameMap = svcOptList.stream().collect(Collectors.groupingBy(K8sOperationInfo::getName));
        for(V1Service svc : existServiceList)
        {
            for(V1ServicePort srcPort : svc.getSpec().getPorts())
            {
                if(srcPort.getNodePort() == null)
                    continue;
                if(!nameMap.containsKey(svc.getMetadata().getName()))
                    continue;
                for(K8sOperationInfo optInfo : nameMap.get(svc.getMetadata().getName()))
                {
                    V1Service execSvc = (V1Service)optInfo.getObj();
                    if(!execSvc.getSpec().getType().equals("NodePort"))
                        continue;
                    switch (optInfo.getOperation())
                    {
                        case CREATE:
                        case REPLACE:

                            Map<String, V1ServicePort> portMap = execSvc.getSpec().getPorts().stream().collect(Collectors.toMap(V1ServicePort::getName, Function.identity()));
                            if(portMap.containsKey(srcPort.getName()))
                            {
                                logger.debug(String.format("service %s has nodePort %s, so reuse it", svc.getMetadata().getName(), srcPort.getNodePort()));
                                portMap.get(srcPort.getName()).setNodePort(srcPort.getNodePort());
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        logger.debug(String.format("reuse exist nodePort of exist k8s services finish"));
    }

    List<V1Deployment> getDomainServiceDeploy(String domainId, List<V1Deployment> allDeployments)
    {
        List<V1Deployment> deployments = new ArrayList<>();
        for(V1Deployment deployment : allDeployments)
        {
            Map<String, String> labels = deployment.getSpec().getTemplate().getMetadata().getLabels();
            if(labels.containsKey(this.domainIdLabel) && labels.get(this.domainIdLabel).equals(domainId))
                deployments.add(deployment);
        }
        return deployments;
    }

    List<V1Service> getDomainServiceService(String domainId, List<V1Service> allService)
    {
        List<V1Service> services = new ArrayList<>();
        for(V1Service service : allService)
        {
            Map<String, String> selector = service.getSpec().getSelector();
            if(selector.containsKey(this.domainIdLabel) && selector.get(this.domainIdLabel).equals(domainId))
                services.add(service);
        }
        return services;
    }

    List<ExtensionsV1beta1Ingress> getDomainServiceIngress(String domainId, List<ExtensionsV1beta1Ingress> allIngresses)
    {
        List<ExtensionsV1beta1Ingress> ingresses = new ArrayList<>();
        for(ExtensionsV1beta1Ingress ingress : allIngresses)
        {
            String regex = String.format(".+-%s", domainId);
            if(ingress.getMetadata().getName().matches(regex))
                ingresses.add(ingress);
        }
        return ingresses;
    }

    List<V1ConfigMap> getDomainServiceConfigMap(String domainId, List<V1ConfigMap> allConfigMaps)
    {
        List<V1ConfigMap> configMaps = new ArrayList<>();
        for(V1ConfigMap configMap : allConfigMaps)
        {
            String regex = String.format("[a-z0-9]+-%s", domainId);
            if(configMap.getMetadata().getName().matches(regex))
                configMaps.add(configMap);
        }
        return configMaps;
    }

    private void makeupDomainIdAndAliasForSchema(PlatformUpdateSchemaInfo schemaInfo, List<DomainPo> existDomainList, List<PlatformAppDeployDetailVo> deployAppList, boolean clone) throws ParamException {
        if(schemaInfo.getDomainUpdatePlanList() == null || schemaInfo.getDomainUpdatePlanList().size() == 0)
            return;
        List<DomainUpdatePlanInfo> addDomainList = schemaInfo.getDomainUpdatePlanList().stream().filter(plan->plan.getUpdateType().equals(DomainUpdateType.ADD))
                .collect(Collectors.toList());
        if(addDomainList.size() > 0)
            generateId4AddDomain(schemaInfo.getPlatformId(), addDomainList, existDomainList, clone);
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        Map<String, DomainPo> domainMap = existDomainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        for (DomainUpdatePlanInfo planInfo : schemaInfo.getDomainUpdatePlanList()) {
            String domainId = planInfo.getDomainId();
            List<AppUpdateOperationInfo> addOptList = planInfo.getAppUpdateOperationList().stream().filter(p->p.getOperation().equals(AppUpdateOperation.ADD))
                    .collect(Collectors.toList());
            logger.debug(String.format("%s has not new add module", domainId, addDomainList.size()));
            if (addOptList.size() > 0) {
                List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
                DomainPo domainPo = domainMap.containsKey(domainId) ? domainMap.get(domainId) : planInfo.getDomain(schemaInfo.getPlatformId());
                generateAlias4DomainApps(domainPo, addOptList, domainAppList, clone);
            }

        }
    }

    /**
     * 为新加的且id为空的域生成域id
     *
     * @param platformId      平台id
     * @param planList        所有的域升级任务
     * @param existDomainList 已经存在的域
     * @param clone           是否为新加的域是否通过clone方式生成
     */
    private void generateId4AddDomain(String platformId, List<DomainUpdatePlanInfo> planList, List<DomainPo> existDomainList, boolean clone) throws ParamException {
        List<DomainPo> hasIdList = new ArrayList<>();
        hasIdList.addAll(existDomainList);
        List<DomainUpdatePlanInfo> notIdList = new ArrayList<>();
        for (DomainUpdatePlanInfo plan : planList) {
            if (plan.getUpdateType().equals(DomainUpdateType.ADD)) {
                if (StringUtils.isBlank(plan.getDomainId()) || !clone)
                    notIdList.add(plan);
                else
                    hasIdList.add(plan.getDomain(platformId));
            }
        }
        Map<String, List<DomainUpdatePlanInfo>> notIdMap = notIdList.stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getBkSetName));
        Map<String, List<DomainPo>> hasIdMap = hasIdList.stream().collect(Collectors.groupingBy(DomainPo::getBizSetName));
        for (String bkSetName : notIdMap.keySet()) {
            String standardDomainId = this.setDefineMap.get(bkSetName).getFixedDomainId();
            List<String> usedId = hasIdMap.containsKey(bkSetName) ? new ArrayList<>(hasIdMap.get(bkSetName).stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity())).keySet()) : new ArrayList<>();
            for (DomainUpdatePlanInfo plan : notIdMap.get(bkSetName)) {
                String domainId = autoGenerateDomainId(standardDomainId, usedId);
                logger.debug(String.format("domain id of %s with bkSetName=%s is %s", plan.getDomainName(), bkSetName, domainId));
                plan.setDomainId(domainId);
                usedId.add(domainId);
            }
        }
    }

    /**
     * 检查平台升级相关参数
     * @param ccodVersion 该平台的ccod大版本
     * @param updateSchema 平台升级计划
     * @param existDomainList 该平台已经有的域
     * @param deployApps 该平台已经部署应用
     * @param hostList 该平台所属主机列表
     * @param registerApps cmdb已经注册的应用列表
     * @return 检查结果
     */
    private String checkPlatformUpdateSchema(String ccodVersion, PlatformUpdateSchemaInfo updateSchema, List<DomainPo> existDomainList, List<PlatformAppDeployDetailVo> deployApps, List<LJHostInfo> hostList, List<AppModuleVo> registerApps) {
        if(updateSchema.getDomainUpdatePlanList() == null || updateSchema.getDomainUpdatePlanList().size() == 0)
            return "";
        StringBuffer sb = new StringBuffer();
        hostList.stream().collect(Collectors.groupingBy(LJHostInfo::getHostInnerIp))
                .forEach((k,v) -> {if(v.size()>1)sb.append(String.format("%s is not unique at paas;", k));});
        Map<String, BizSetDefine> setDefineMap = this.ccodBiz.getSet().stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        Map<String, DomainPo> domainIdMap = existDomainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        Map<String, DomainPo> domainNameMap = existDomainList.stream().collect(Collectors.toMap(DomainPo::getDomainName, Function.identity()));
        Map<DomainUpdateType, List<DomainUpdatePlanInfo>> typePlanMap = updateSchema.getDomainUpdatePlanList().stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getUpdateType));
        List<DomainUpdatePlanInfo> deleteList = typePlanMap.containsKey(DomainUpdateType.DELETE) ? typePlanMap.get(DomainUpdateType.DELETE) : new ArrayList<>();
        Map<String, LJHostInfo> hostMap = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity()));
        for(DomainUpdatePlanInfo planInfo : deleteList)
        {
            String domainId = planInfo.getDomainId();
            if(StringUtils.isBlank(domainId))
                sb.append("domainId of DELETE domain is blank;");
            else if(!domainIdMap.containsKey(domainId))
                sb.append(String.format("DELETE domain %s not exist;", domainId));
            else if(planInfo.getAppUpdateOperationList() != null && planInfo.getAppUpdateOperationList().size() > 0)
                sb.append(String.format("appUpdateOperationList of DELETED domain %s should be empty;", domainId));
        }
        List<DomainUpdatePlanInfo> addList = typePlanMap.containsKey(DomainUpdateType.ADD) ? typePlanMap.get(DomainUpdateType.ADD) : new ArrayList<>();
        for(DomainUpdatePlanInfo planInfo : addList)
        {
            String domainName = planInfo.getDomainName();
            String bkSetName = planInfo.getBkSetName();
            if(StringUtils.isBlank(domainName))
                sb.append("domainName of ADD domain is blank;");
            else if(domainNameMap.containsKey(domainName))
                sb.append(String.format("ADD domain %s has exist;", domainName));
            else if(StringUtils.isBlank(bkSetName))
                sb.append(String.format("bkSetName of ADD domain %s is blank;", domainName));
            else if(!setDefineMap.containsKey(bkSetName))
                sb.append(String.format("bkSetName %s of %s is illegal;", bkSetName, domainName));
            else if(planInfo.getAppUpdateOperationList().stream().filter(opt->!opt.getOperation().equals(AppUpdateOperation.ADD))
                    .collect(Collectors.toList()).size() > 0)
                sb.append(String.format("app operation of CREATED domain %s should be ADD;", domainName));
            else {
                BizSetDefine setDefine = setDefineMap.get(bkSetName);
                for(AppUpdateOperationInfo opt : planInfo.getAppUpdateOperationList()) {
                    String optCheckResult = checkAppBase(ccodVersion, opt, opt.getOperation(), setDefine, new ArrayList<>(), registerApps);
                    if(StringUtils.isBlank(optCheckResult)) {
                        if(StringUtils.isBlank(opt.getHostIp()))
                            sb.append(String.format("hostIp of %s is blank;", opt.toString()));
                        else if(!hostMap.containsKey(opt.getHostIp()))
                            sb.append(String.format("hostIp of %s not exist;", opt.toString()));
                    }
                    else
                        sb.append(optCheckResult);
                }
            }
        }
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = deployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        List<DomainUpdatePlanInfo> updateList = typePlanMap.containsKey(DomainUpdateType.UPDATE) ? typePlanMap.get(DomainUpdateType.UPDATE) : new ArrayList<>();
        for(DomainUpdatePlanInfo planInfo : updateList)
        {
            String domainId = planInfo.getDomainId();
            if(StringUtils.isBlank(domainId))
                sb.append("domainId of UPDATE domain is blank;");
            else if(!domainIdMap.containsKey(domainId))
                sb.append(String.format("UPDATE domain %s not exist;", domainId));
            else {
                BizSetDefine setDefine = setDefineMap.get(domainIdMap.get(domainId).getBizSetName());
                List<PlatformAppDeployDetailVo> domainApps = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
                planInfo.getAppUpdateOperationList()
                        .forEach(opt->sb.append(checkAppBase(ccodVersion, opt, opt.getOperation(), setDefine, domainApps, registerApps)));
            }
        }
        if(StringUtils.isNotBlank(sb.toString()))
            return sb.toString();
        updateSchema.getDomainUpdatePlanList().stream().filter(plan -> StringUtils.isNotBlank(plan.getDomainId()))
                .collect(Collectors.groupingBy(DomainUpdatePlanInfo::getDomainId))
                .forEach((k,v)->{if(v.size()>1)sb.append(String.format("update plan of %s multi define;", k));});
        updateSchema.getDomainUpdatePlanList().stream().filter(plan -> StringUtils.isNotBlank(plan.getDomainName()))
                .collect(Collectors.groupingBy(DomainUpdatePlanInfo::getDomainName))
                .forEach((k,v)->{if(v.size()>1)sb.append(String.format("update plan of %s multi define;", k));});
        return sb.toString();
    }

    private String checkDomainPlan(DomainUpdatePlanInfo planInfo, List<PlatformAppDeployDetailVo> domainApps, List<LJHostInfo> hostList, List<AppModuleVo> registerApps)
    {
        Map<String, List<AppModuleVo>> registerAppMap = registerApps.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        StringBuffer sb = new StringBuffer();
        DomainUpdateType planType = planInfo.getUpdateType();
        String domainId = planInfo.getDomainId();
        String domainName = planInfo.getDomainName();
        Map<AppUpdateOperation, List<AppUpdateOperationInfo>> optMap = planInfo.getAppUpdateOperationList().stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getOperation));
        Map<String, PlatformAppDeployDetailVo> deployAppMap = domainApps.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAlias, Function.identity()));
        List<AppUpdateOperationInfo> deleteList = optMap.containsKey(AppUpdateOperation.DELETE) ? optMap.get(AppUpdateOperation.DELETE) : new ArrayList<>();
        Map<String, LJHostInfo> hostMap = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity()));
        for(AppUpdateOperationInfo optInfo : deleteList)
        {
            if(planType.equals(DomainUpdateType.ADD))
                sb.append(String.format("%s domain can not has %s operation;", planType.name, optInfo.getOperation().name));
            else if(StringUtils.isBlank(optInfo.getAppName()))
                sb.append(String.format("DELETE app name at %s is blank;", domainId));
            else if(StringUtils.isBlank(optInfo.getAlias()))
                sb.append("DELETE app alias is blank;");
            else if(!deployAppMap.containsKey(optInfo.getAlias()))
                sb.append(String.format("DELETE app %s not exist at %s;", optInfo.getAlias(), planInfo.getDomainId()));
        }
        List<AppUpdateOperationInfo> addList = optMap.containsKey(AppUpdateOperation.ADD) ? optMap.get(AppUpdateOperation.ADD) : new ArrayList<>();
        for(AppUpdateOperationInfo optInfo : addList)
        {
            if(planType.equals(DomainUpdateType.DELETE))
                sb.append(String.format("%s domain can not has %s operation;", planType.name, optInfo.getOperation().name));
            else if(StringUtils.isBlank(optInfo.getAppName()))
                sb.append(String.format("new ADD app at %s name is blank;", domainName));
            else
            {
                String appName = optInfo.getAppName();
                String optTag = String.format("ADD %s at %s", appName, domainName);
                if(StringUtils.isBlank(optInfo.getVersion()))
                    sb.append(String.format("version of %s is blank;", optTag));
                else if (!registerAppMap.containsKey(appName)
                        || !registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(optInfo.getVersion()))
                    sb.append(String.format("version %s of %s not register;", optInfo.getVersion(), optTag));
                if(StringUtils.isBlank(optInfo.getBasePath()))
                    sb.append(String.format("basePath of %s is blank;", optTag));
                if(StringUtils.isBlank(optInfo.getDeployPath()))
                    sb.append(String.format("deployPath of %s is blank;", optTag));
                if(StringUtils.isBlank(optInfo.getStartCmd()))
                    sb.append(String.format("startCmd of %s is blank;", optTag));
                if(StringUtils.isBlank(optInfo.getHostIp()))
                    sb.append(String.format("hostIp of %s is blank;", optTag));
                else if(!hostMap.containsKey(optInfo.getHostIp()))
                    sb.append(String.format("hostIp of %s not exist;", optTag));
                if(optInfo.getCfgs() == null || optInfo.getCfgs().size() == 0)
                    sb.append(String.format("cfg of %s is 0;", optTag));

            }
        }
        List<AppUpdateOperationInfo> updateList = optMap.containsKey(AppUpdateOperation.UPDATE) ? optMap.get(AppUpdateOperation.UPDATE) : new ArrayList<>();
        for(AppUpdateOperationInfo optInfo : updateList)
        {
            String alias = optInfo.getAlias();
            String appName = optInfo.getAppName();
            if(StringUtils.isBlank(appName))
                sb.append(String.format("UPDATE app name at %s is blank;", domainId));
            else if(StringUtils.isBlank(optInfo.getAlias()))
                sb.append(String.format("UPDATE alias name of %s at %s is blank;", appName, domainId));
            else if(!deployAppMap.containsKey(alias))
                sb.append(String.format("UPDATE %s at %s not exist;", alias, domainId));
            else if(!deployAppMap.get(alias).getAppName().equals(appName))
                sb.append(String.format("appName of %s at %s is %s not %s;", alias, domainId, deployAppMap.get(alias).getAppName(), appName));
            else
            {
                String optTag = String.format("UPDATE %s(%s) at %s", alias, appName, domainId);
                if(StringUtils.isBlank(optInfo.getVersion()))
                    sb.append(String.format("version of %s is blank;", optTag));
                else if (!registerAppMap.containsKey(appName)
                        || !registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(optInfo.getVersion()))
                    sb.append(String.format("version %s of %s not register;", optInfo.getVersion(), optTag));
                if(StringUtils.isBlank(optInfo.getBasePath()))
                    sb.append(String.format("basePath of %s is blank;", optTag));
                if(StringUtils.isBlank(optInfo.getDeployPath()))
                    sb.append(String.format("deployPath of %s is blank;", optTag));
                if(StringUtils.isBlank(optInfo.getStartCmd()))
                    sb.append(String.format("startCmd of %s is blank;", optTag));
                if(StringUtils.isBlank(optInfo.getHostIp()))
                    sb.append(String.format("hostIp of %s is blank;", optTag));
                else if(!hostMap.containsKey(optInfo.getHostIp()))
                    sb.append(String.format("hostIp of %s not exist;", optTag));
                if(optInfo.getCfgs() == null || optInfo.getCfgs().size() == 0)
                    sb.append(String.format("cfg of %s is 0;", optTag));
            }
        }
        if(StringUtils.isNotBlank(sb.toString()))
            return sb.toString();
        Map<String, List<AppUpdateOperationInfo>> deleteMap = deleteList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAlias));
        for(String alias : deleteMap.keySet())
        {
            if(deleteMap.get(alias).size() > 1)
                sb.append(String.format("%s at %s is DELETE duplicate;", domainId));
        }
        Map<String, List<AppUpdateOperationInfo>> updateMap = deleteList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAlias));
        for(String alias : updateMap.keySet())
        {
            if(updateMap.get(alias).size() > 1)
                sb.append(String.format("%s at %s is UPDATE duplicate;", domainId));
        }
        for(String alias : deleteMap.keySet()) {
            if (updateMap.containsKey(alias))
                sb.append(String.format("%s at %s is UPDATE and DELETE at the same time;"));
        }
        Map<String, List<AppUpdateOperationInfo>> addMap = addList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
        deleteMap = deleteList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
        for(String appName : addMap.keySet())
        {
            if(deleteMap.containsKey(appName))
                sb.append(String.format("%s at %s has been DELETE and ADD at the same time;", appName, domainId));
        }
        return sb.toString();
    }

    @Override
    public PlatformUpdateSchemaInfo createNewPlatform(PlatformCreateParamVo paramVo) throws ParamException, NotSupportSetException, NotSupportAppException, InterfaceCallException, LJPaasException {
        logger.debug(String.format("prepare to create new platform : %s", gson.toJson(paramVo)));
        checkPlatformBase(paramVo, PlatformUpdateTaskType.CREATE, new ArrayList<>(), UpdateStatus.CREATE);
        if(paramVo.getCreateMethod().equals(PlatformCreateMethod.CLONE))
            Assert.notNull(paramVo.getParams(), "params should be assigned to cloned platformId");
        PlatformPo platform = this.platformMapper.selectByPrimaryKey(paramVo.getPlatformId());
        Assert.isNull(platform, String.format("platform with id=%s exist", paramVo.getPlatformId()));
        platform = this.platformMapper.selectByNameBizId(paramVo.getPlatformName(), null);
        Assert.isNull(platform, String.format("platform with name=%s exist", paramVo.getPlatformName()));
        LJBizInfo biz = this.paasService.queryBizInfoById(paramVo.getBkBizId());
        Assert.notNull(biz, String.format("biz with id=%d not exist", paramVo.getBkBizId()));
        Assert.isTrue(biz.getBkBizName().equals(paramVo.getPlatformName()), String.format("name of bizId=%d is %s not %s",
                paramVo.getBkBizId(), biz.getBkBizName(), paramVo.getPlatformName()));
        List<LJHostInfo> hostList = this.paasService.queryBKHost(paramVo.getBkBizId(), null, null, null, null);
        Assert.isTrue(hostList.size() > 0, String.format("%s has not any host", paramVo.getPlatformName()));
        PlatformUpdateSchemaInfo schemaInfo;
        String platformId = paramVo.getPlatformId();
        switch (paramVo.getCreateMethod()) {
            case MANUAL:
                if(paramVo.getType() == null)
                    throw new ParamException(String.format("platform type of new platform is null"));
                schemaInfo = paramVo.getPlatformCreateSchema(new ArrayList<>());
                break;
            case CLONE:
                schemaInfo = cloneExistPlatform(paramVo, hostList);
                break;
            case PREDEFINE:
                if (StringUtils.isBlank(paramVo.getParams())) {
                    logger.error(String.format("apps of pre define is blank"));
                    throw new ParamException(String.format("apps of pre define is blank"));
                }
                List<String> planAppList = new ArrayList<>();
                String[] planApps = paramVo.getParams().split("\n");
                for (String planApp : planApps) {
                    if (StringUtils.isNotBlank(planApp))
                        planAppList.add(planApp);
                }
                schemaInfo = createDemoNewPlatform(paramVo, planAppList, hostList);
                break;
            default:
                throw new ParamException(String.format("current version not support %s create", paramVo.getCreateMethod().name));
        }
        platform = schemaInfo.getCreatePlatform(schemaInfo.getComment());
        platformMapper.insert(platform);
        PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
        schemaPo.setPlatformId(platformId);
        schemaPo.setContext(gson.toJson(schemaInfo).getBytes());
        this.platformUpdateSchemaMapper.delete(platformId);
        this.platformUpdateSchemaMapper.insert(schemaPo);
        this.platformUpdateSchemaMap.put(platformId, schemaInfo);
        return schemaInfo;
    }

    private void checkPlatformCreateParam(PlatformCreateParamVo param) throws ParamException, InterfaceCallException, LJPaasException {
        StringBuffer sb = new StringBuffer();
        if (PlatformType.K8S_CONTAINER.equals(param.getType())) {
            if (StringUtils.isBlank(param.getK8sApiUrl()))
                sb.append("k8sApiUrl is blank;");
            if (StringUtils.isBlank(param.getK8sAuthToken()))
                sb.append("ks8AuthToken is blank;");
            if (StringUtils.isBlank(param.getK8sHostIp()))
                sb.append("k8sHostIp is blank;");
        }
        if (param.getBkBizId() == 0)
            sb.append("bizId of platform not define;");
        if (param.getCreateMethod().equals(PlatformCreateMethod.CLONE) || param.getCreateMethod().equals(PlatformCreateMethod.PREDEFINE))
            if (StringUtils.isBlank(param.getParams()))
                sb.append("params is blank;");
        if (StringUtils.isNotBlank(sb.toString()))
            throw new ParamException(sb.toString().replaceAll(";$", ""));
        List<PlatformPo> platformList = this.platformMapper.select(null);
        if (platformList.stream().collect(Collectors.toMap(PlatformPo::getPlatformId, Function.identity())).containsKey(param.getPlatformId()))
            throw new ParamException(String.format("id=%s platform has exist", param.getPlatformId()));
        if (platformList.stream().collect(Collectors.toMap(PlatformPo::getPlatformName, Function.identity())).containsKey(param.getPlatformName()))
            throw new ParamException(String.format("name=%s platform has exist", param.getPlatformName()));
        LJBizInfo bizInfo = this.paasService.queryBizInfoById(param.getBkBizId());
        if (bizInfo == null)
            throw new ParamException(String.format("bizId=%d biz not exist", param.getBkBizId()));
        if (!bizInfo.getBkBizName().equals(param.getPlatformName()))
            throw new ParamException(String.format("bizId=%d biz name is %s not %s", param.getBkBizId(), bizInfo.getBkBizName(), param.getPlatformName()));
    }

    /**
     * 创建demo新平台
     *
     * @param planAppList 新建平台计划部署的应用
     * @return 创建的平台
     * @throws ParamException         计划的参数异常
     * @throws InterfaceCallException 处理计划时调用蓝鲸api或是nexus api失败
     * @throws LJPaasException        调用蓝鲸api返回调用失败或是解析蓝鲸api结果失败
     */
    public PlatformUpdateSchemaInfo createDemoNewPlatform(PlatformCreateParamVo paramVo, List<String> planAppList, List<LJHostInfo> hostList) throws ParamException, InterfaceCallException, LJPaasException {
        logger.debug(String.format("begin to create new empty platform : %s", gson.toJson(paramVo)));
        Date now = new Date();
        Map<String, List<String>> planAppMap = new HashMap<>();
        for (String planApp : planAppList) {
            String[] arr = planApp.split("##");
            if (!planAppMap.containsKey(arr[0])) {
                planAppMap.put(arr[0], new ArrayList<>());
            }
            planAppMap.get(arr[0]).add(planApp);
        }
        List<DomainUpdatePlanInfo> planList = new ArrayList<>();
        Set<String> ipSet = new HashSet<>();
        Map<String, List<AppModuleVo>> registerAppMap = this.appManagerService.queryAllRegisterAppModule(null).stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        for (BizSetDefine setDefine : this.setDefineMap.values()) {
            if (setDefine.getApps().size() == 0)
                continue;
            DomainUpdatePlanInfo planInfo = new DomainUpdatePlanInfo();
            planInfo.setUpdateType(DomainUpdateType.ADD);
            planInfo.setStatus(UpdateStatus.CREATE);
            planInfo.setComment("由程序自动生成的新建域");
            String domainId = setDefine.getFixedDomainId();
            String domainName = setDefine.getFixedDomainName();
            if (StringUtils.isBlank(domainId)) {
                domainId = "new-create-test-domain";
                domainName = "新建测试域";
            }
            planInfo.setDomainId(domainId);
            planInfo.setDomainName(domainName);
            planInfo.setAppUpdateOperationList(new ArrayList<>());
            planInfo.setBkSetName(setDefine.getName());
            for (AppDefine appDefine : setDefine.getApps()) {
                String appName = appDefine.getName();
                if (planAppMap.containsKey(appName)) {
                    for (String planApp : planAppMap.get(appName)) {
                        String[] arr = planApp.split("##");
                        String appAlias = arr[1];
                        String version = arr[2];
                        String hostIp = arr[3].split("@")[1];
                        ipSet.add(hostIp);
                        String[] pathArr = arr[3].split("@")[0].replaceAll("^/", "").split("/");
                        pathArr[1] = appAlias;
                        String basePath = String.format("/%s", String.join("/", pathArr));
                        Map<String, AppModuleVo> versionMap = registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity()));
                        if (!versionMap.containsKey(version)) {
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
                        for (AppFileNexusInfo appCfgFilePo : appModuleVo.getCfgs()) {
                            AppFileNexusInfo info = new AppFileNexusInfo();
                            info.setDeployPath(appCfgFilePo.getDeployPath());
                            info.setExt(appCfgFilePo.getExt());
                            info.setFileName(appCfgFilePo.getFileName());
                            info.setMd5(appCfgFilePo.getMd5());
                            info.setNexusAssetId(appCfgFilePo.getNexusAssetId());
                            info.setNexusPath(appCfgFilePo.getNexusPath());
                            info.setNexusRepository(appCfgFilePo.getNexusRepository());
                            addOperationInfo.getCfgs().add(info);
                        }
                        addOperationInfo.setBasePath(basePath);
                        addOperationInfo.setAppRunner(appAlias);
                        addOperationInfo.setAlias(appAlias);
                        addOperationInfo.setAppName(appName);
                        addOperationInfo.setVersion(version);
                        planInfo.getAppUpdateOperationList().add(addOperationInfo);
                    }
                }

            }
            planList.add(planInfo);
        }
        Map<String, LJHostInfo> hostMap = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity()));
        for (String hostIp : ipSet) {
            if (!hostMap.containsKey(hostIp))
                throw new LJPaasException(String.format("%s has not %s host", paramVo.getPlatformName(), hostIp));
        }
        PlatformUpdateSchemaInfo schema = paramVo.getPlatformCreateSchema(planList);
        return schema;
    }

    /**
     * 从已有的平台创建一个新的平台
     *
     * @param paramVo       平台创建参数
     * @param hostList      给新平台分配的服务器列表
     * @return 平台创建计划
     * @throws ParamException
     * @throws NotSupportSetException
     * @throws NotSupportAppException
     * @throws InterfaceCallException
     * @throws LJPaasException
     */
    private PlatformUpdateSchemaInfo cloneExistPlatform(
            PlatformCreateParamVo paramVo, List<LJHostInfo> hostList) throws ParamException{
        logger.debug(String.format("begin to clone platform : %s", gson.toJson(paramVo)));
        String clonedPlatformId = paramVo.getParams();
        PlatformPo clonedPlatform = getK8sPlatform(clonedPlatformId);
        Assert.isTrue(clonedPlatform.getCcodVersion().equals(paramVo.getCcodVersion()),
                String.format("created platform version is %s, but cloned platform version is %s", paramVo.getCcodVersion(), clonedPlatform.getCcodVersion()));
        List<DomainPo> clonedDomains = this.domainMapper.select(clonedPlatformId, null);
        Assert.notEmpty(clonedDomains, String.format("domain of cloned platform %s is empty", clonedPlatformId));
        List<PlatformAppDeployDetailVo> clonedApps = this.platformAppDeployDetailMapper.selectPlatformApps(clonedPlatformId, null, null);
        Assert.notEmpty(clonedApps, String.format("apps of cloned platform %s is empty", clonedPlatformId));
        Map<String, List<PlatformAppDeployDetailVo>> hostAppMap = clonedApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getHostIp));
        Assert.isTrue(hostList.size()>=hostAppMap.size(), String.format("%s has not enough hosts, want %s but has %d",
                paramVo.getPlatformName(), hostAppMap.size(), hostList.size()));
        Map<String, DomainUpdatePlanInfo> planMap = new HashMap<>();
        Map<String, DomainPo> clonedDomainMap = clonedDomains.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
//        List<AppFileNexusInfo> platformCfg = this.platformPublicConfigMapper.select(clonedPlatform.getPlatformId()).stream().collect(Collectors.toList());
        int i = 0;
        for (List<PlatformAppDeployDetailVo> hostAppList : hostAppMap.values()) {
            String hostIp = hostList.get(i).getHostInnerIp();
            for (PlatformAppDeployDetailVo deployApp : hostAppList) {
                String domainId = deployApp.getDomainId();
                if (!planMap.containsKey(domainId)) {
                    DomainUpdatePlanInfo planInfo = generateCloneExistDomain(clonedDomainMap.get(domainId));
                    planMap.put(domainId, planInfo);
                }
                AppUpdateOperationInfo opt = deployApp.getOperationInfo(AppUpdateOperation.ADD);
                opt.setHostIp(hostIp);
                planMap.get(domainId).getAppUpdateOperationList().add(opt);
            }
            i++;
        }
        List<AppFileNexusInfo> cfgs = paramVo.getCfgs() == null || paramVo.getCfgs().size() == 0 ? clonedPlatform.getCfgs() : paramVo.getCfgs();
        PlatformUpdateSchemaInfo schema = paramVo.getPlatformCreateSchema(cfgs, clonedPlatform.getParams(), new ArrayList<>(planMap.values()));
        schema.setComment(String.format("create by %s %s", paramVo.getCreateMethod().name, paramVo.getParams()));
        return schema;
    }

    private DomainUpdatePlanInfo generateCloneExistDomain(DomainPo clonedDomain) {
        DomainUpdatePlanInfo planInfo = new DomainUpdatePlanInfo();
        planInfo.setUpdateType(DomainUpdateType.ADD);
        planInfo.setComment(String.format("clone from %s of %s", clonedDomain.getDomainName(), clonedDomain.getPlatformId()));
        planInfo.setDomainId(clonedDomain.getDomainId());
        planInfo.setDomainName(clonedDomain.getDomainName());
        planInfo.setBkSetName(clonedDomain.getBizSetName());
        planInfo.setStatus(UpdateStatus.CREATE);
        planInfo.setAppUpdateOperationList(new ArrayList<>());
        planInfo.setMaxOccurs(clonedDomain.getMaxOccurs());
        planInfo.setOccurs(clonedDomain.getOccurs());
        planInfo.setTags(clonedDomain.getTags());
        planInfo.setPublicConfig(clonedDomain.getCfgs());
        return planInfo;
    }

    @Override
    public void startCollectPlatformAppData(String platformId, String platformName, int bkBizId, int bkCloudId) throws ParamException, Exception {
        PlatformPo platformPo = platformMapper.selectByPrimaryKey(platformId);
        if (platformPo != null) {
            logger.error(String.format("platformId=%s platform has existed", platformId));
            throw new ParamException(String.format("platformId=%s platform has existed", platformId));
        }
        if (this.isPlatformCheckOngoing) {
            logger.error(String.format("start platform=%s app data collect FAIL : some collect task is ongoing", platformId));
            throw new ClientCollectDataException(String.format("start platform=%s app data collect FAIL : some collect task is ongoing", platformId));
        }
        this.isPlatformCheckOngoing = true;
        try {
            List<PlatformAppModuleVo> modules = this.platformAppCollectService.collectPlatformAppData(platformId, platformName, null, null, null, null);
            List<PlatformAppModuleVo> failList = new ArrayList<>();
            List<DomainPo> domainList = new ArrayList<>();
            Map<String, List<PlatformAppModuleVo>> domainAppMap = modules.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getDomainName));
            List<String> domainNameList = new ArrayList<>(domainAppMap.keySet());
            for (String domainName : domainNameList) {
                try {
                    DomainPo domainPo = parseDomainApps(domainAppMap.get(domainName));
                    domainList.add(domainPo);
                } catch (ParamException ex) {
                    for (PlatformAppModuleVo moduleVo : domainAppMap.get(domainName)) {
                        moduleVo.setComment(ex.getMessage());
                        failList.add(moduleVo);
                    }
                    domainAppMap.remove(domainName);
                }
            }
            logger.debug(String.format("%s has %d domain : %s", platformName, domainAppMap.size(), String.join(",", domainAppMap.keySet())));
            platformPo = modules.get(0).getPlatform();
            platformPo.setBkBizId(bkBizId);
            platformPo.setBkCloudId(bkCloudId);
            platformMapper.insert(platformPo);
            logger.debug(String.format("begin to preprocess collected %d app", modules.size()));
            List<PlatformAppModuleVo> successList = this.appManagerService.preprocessCollectedPlatformAppModule(platformName, platformId, domainAppMap.values().stream().flatMap(listContainer -> listContainer.stream()).collect(Collectors.toList()), failList);
            Map<String, List<DomainPo>> setDomainMap = domainList.stream().collect(Collectors.groupingBy(DomainPo::getBizSetName));
            for (String bkSetName : setDomainMap.keySet()) {
                List<DomainPo> setDomainList = setDomainMap.get(bkSetName);
                List<String> usedIds = new ArrayList<>();
                String standardDomainId = this.setDefineMap.get(bkSetName).getFixedDomainId();
                for (DomainPo domainPo : setDomainList) {
                    String newDomainId = autoGenerateDomainId(standardDomainId, usedIds);
                    domainPo.setDomainId(newDomainId);
                    usedIds.add(newDomainId);
                    logger.debug(String.format("domainId of %s is %s", domainPo.getDomainName(), domainPo.getDomainId()));
                    for (PlatformAppModuleVo moduleVo : domainAppMap.get(domainPo.getDomainName())) {
                        moduleVo.setDomainId(newDomainId);
                    }
                }
            }
            for (DomainPo po : domainList) {
                logger.debug(String.format("insert new domain [%s]", gson.toJson(po)));
                this.domainMapper.insert(po);
            }
            Map<String, List<AppModuleVo>> appMap = this.appManagerService.queryAllRegisterAppModule(null).stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
            for (PlatformAppModuleVo moduleVo : successList) {
                logger.debug(String.format("begin to handle %s", moduleVo.toString()));
                PlatformAppPo platformApp = moduleVo.getPlatformApp();
                String platformCfgDirectory = platformApp.getPlatformAppDirectory(moduleVo.getModuleName(), moduleVo.getVersion(), platformApp);
                logger.debug(String.format("update %d cfgs to %s/%s", moduleVo.getCfgs().length, this.platformAppCfgRepository, platformCfgDirectory));
                nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword,
                        this.platformAppCfgRepository, platformCfgDirectory, moduleVo.getCfgs());
                AssemblePo assemblePo = new AssemblePo();
                assemblePo.setTag(platformApp.getAlias());
                assemblePo.setStatus("Running");
                assemblePo.setPlatformId(platformId);
                assemblePo.setDomainId(moduleVo.getDomainId());
                this.assembleMapper.insert(assemblePo);
                platformApp.setAssembleId(assemblePo.getAssembleId());
                platformApp.setAppId(appMap.get(moduleVo.getModuleName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(moduleVo.getVersion()).getAppId());
                logger.debug(String.format("insert %s into platform_app", gson.toJson(platformApp)));
                this.platformAppMapper.insert(platformApp);
                logger.info(String.format("[%s] platform app module handle SUCCESS", moduleVo.toString()));
            }
            for (PlatformAppModuleVo moduleVo : failList) {
                try {
                    UnconfirmedAppModulePo unconfirmedAppModulePo = handleUnconfirmedPlatformAppModule(moduleVo);
                    this.unconfirmedAppModuleMapper.insert(unconfirmedAppModulePo);
                } catch (Exception ex) {
                    logger.error(String.format("handle unconfirmed app exception"), ex);
                }

            }
            this.ljPaasService.syncClientCollectResultToPaas(bkBizId, platformId, bkCloudId);
            platformPo.setStatus(CCODPlatformStatus.RUNNING);
            this.platformMapper.update(platformPo);
        } finally {
            this.isPlatformCheckOngoing = false;
        }
    }

    /**
     * 自动生成域id
     *
     * @param standardDomainId 标准域id
     * @param usedId           已经使用过的id
     * @return 自动生成域id
     * @throws ParamException
     */
    private String autoGenerateDomainId(String standardDomainId, List<String> usedId) throws ParamException {
        String regex = String.format("^%s(0[1-9]|[1-9]\\d+)", standardDomainId);
        Pattern pattern = Pattern.compile(regex);
        int index = 0;
        for (String id : usedId) {
            Matcher matcher = pattern.matcher(id);
            if (!matcher.find()) {
                logger.error(String.format("%s is an illegal tag for %s", id, standardDomainId));
                throw new ParamException(String.format("%s is an illegal tag for %s", id, standardDomainId));
            }
            String str = id.replaceAll(standardDomainId, "");
            if (StringUtils.isNotBlank(str)) {
                int oldIndex = Integer.parseInt(str);
                if (oldIndex > index) {
                    index = oldIndex;
                }
            }
        }
        index++;
        String domainId = String.format("%s%s", standardDomainId, (index > 9 ? index + "" : "0" + index));
        return domainId;
    }

    /**
     * 从指定的域模块获取域信息以及域归属的集群信息
     *
     * @param domainAppList 域模块列表
     * @return 模块所属的域信息
     * @throws ParamException 从域模块中无法获取正确的域信息
     */
    private DomainPo parseDomainApps(List<PlatformAppModuleVo> domainAppList) throws ParamException {
        String domainName = domainAppList.get(0).getDomainName();
        logger.debug(String.format("begin to parse %d apps of domainName=%s", domainAppList.size(), domainName));
        Map<String, List<PlatformAppModuleVo>> map = domainAppList.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getDomainName));
        if (map.size() > 1) {
            logger.error(String.format("%s has not unique id %s", domainName, String.join(",", map.keySet())));
            throw new ParamException(String.format("%s has not unique id %s", domainName, String.join(",", map.keySet())));
        }
        String domainId = domainAppList.get(0).getDomainId();
        map = domainAppList.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getModuleAliasName));
        for (String alias : map.keySet()) {
            if (map.get(alias).size() > 1) {
                logger.error(String.format("alias %s of %s(%s) is not unique", alias, domainName, domainId));
                throw new ParamException(String.format("alias %s of %s(%s) is not unique", alias, domainName, domainId));
            }
        }
        String bkSetName = null;
        Map<String, Set<String>> appSetNameMap = new HashMap<>();
        map = domainAppList.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getModuleName));
        String comparedAppName = null;
        for (String appName : map.keySet()) {
            if (this.appSetRelationMap.containsKey(appName)) {
                appSetNameMap.put(appName, this.appSetRelationMap.get(appName).stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity())).keySet());
                comparedAppName = appName;
            }
        }
        if (appSetNameMap.size() == 0) {
            logger.error(String.format("can not decline bkSetName of %s(%s) : all apps not support", domainName, domainId));
            throw new ParamException(String.format("can not decline bkSetName of %s(%s) : all apps not support", domainName, domainId));
        } else if (appSetNameMap.size() == 1 && appSetNameMap.get(comparedAppName).size() > 1) {
            logger.error(String.format("can not decline bkSetName of %s(%s) : set of %s is ambiguous", domainName, domainId, comparedAppName));
            throw new ParamException(String.format("can not decline bkSetName of %s(%s) : set of %s is ambiguous", domainName, domainId, comparedAppName));
        }
        for (String setName : appSetNameMap.get(comparedAppName)) {
            if (bkSetName != null)
                break;
            boolean isMatch = true;
            for (String appName : appSetNameMap.keySet()) {
                if (!appSetNameMap.get(appName).contains(setName)) {
                    isMatch = false;
                    break;
                }
            }
            if (isMatch) {
                bkSetName = setName;
                break;
            }
        }
        if (bkSetName == null) {
            logger.error(String.format("can not decline bkSetName of %s(%s) : set of all apps is ambiguous", domainName, domainId));
            throw new ParamException(String.format("can not decline bkSetName of %s(%s) : set of all apps is ambiguous", domainName, domainId));
        }
        map = domainAppList.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getModuleName));
        for (String appName : map.keySet()) {
            List<PlatformAppModuleVo> appModuleList = map.get(appName);
            boolean onlyOne = appModuleList.size() == 1 ? true : false;
            String standardAlias = this.appSetRelationMap.get(appName).stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity())).get(bkSetName).getApps()
                    .stream().collect(Collectors.toMap(AppDefine::getName, Function.identity())).get(appName).getAlias();
            List<String> usedAlias = new ArrayList<>();
            for (PlatformAppModuleVo appModuleVo : appModuleList) {
                String alias = autoGenerateAlias(standardAlias, usedAlias, onlyOne);
                appModuleVo.setAlias(alias);
                logger.debug(String.format("alias of %s at %s is %s[%s]",
                        appName, domainName, appModuleVo.getAlias(), appModuleVo.getModuleAliasName()));
                usedAlias.add(alias);
            }
        }
        DomainPo po = map.get(comparedAppName).get(0).getDomain();
        po.setBizSetName(bkSetName);
        logger.debug(String.format("%s belong to %s", domainName, bkSetName));
        return po;
    }

    /**
     * 生成应用的别名
     *
     * @param standardAlias 应用的标准别名
     * @param usedAlias     该应用已经使用了的别名
     * @param onlyOne       新加的该类应用是否只有一个
     * @return 生成的应用别名
     * @throws ParamException
     */
    private String autoGenerateAlias(String standardAlias, List<String> usedAlias, boolean onlyOne) throws ParamException {
        if (usedAlias.size() == 0) {
            if (standardAlias.equals("ucgateway"))
                return "ucgateway0";
            else
                return onlyOne ? standardAlias : standardAlias + "1";
        }

        String regex = String.format("^%s\\d*$", standardAlias);
        Pattern pattern = Pattern.compile(regex);
        int index = 0;
        for (String alias : usedAlias) {
            Matcher matcher = pattern.matcher(alias);
            if (!matcher.find()) {
                logger.error(String.format("%s is an illegal alias for %s", alias, standardAlias));
                throw new ParamException(String.format("%s is an illegal alias for %s", alias, standardAlias));
            }
            String str = alias.replaceAll(standardAlias, "");
            if (StringUtils.isNotBlank(str)) {
                int oldIndex = Integer.parseInt(str);
                if (oldIndex > index)
                    index = oldIndex;
            } else if (index == 0 && !standardAlias.equals("ucgateway"))
                index = 1;
        }
        index++;
        String appAlias = String.format("%s%s", standardAlias, index);
        return appAlias;
    }

    /**
     * 处理处理失败的平台应用模块
     *
     * @param moduleVo 处理失败的平台应用模块
     * @return 用来入库的pojo类
     * @throws Exception
     */
    private UnconfirmedAppModulePo handleUnconfirmedPlatformAppModule(PlatformAppModuleVo moduleVo) throws Exception {
        AppPo appPo = moduleVo.getAppInfo();
        String appName = appPo.getAppName();
        String appVersion = appPo.getVersion();
        UnconfirmedAppModulePo po = moduleVo.getUnconfirmedModule();
        logger.debug(String.format("begin to handle unconfirmed %s", po.toString()));
        List<DeployFileInfo> fileList = new ArrayList<>();
        if (StringUtils.isNotBlank(moduleVo.getInstallPackage().getLocalSavePath()))
            fileList.add(moduleVo.getInstallPackage());
        for (DeployFileInfo cfg : moduleVo.getCfgs()) {
            if (StringUtils.isNotBlank(cfg.getLocalSavePath()))
                fileList.add(cfg);
        }
        if (fileList.size() > 0) {
            PlatformAppPo platformApp = moduleVo.getPlatformApp();
            String platformCfgDirectory = platformApp.getPlatformAppDirectory(appName, appVersion, platformApp);
            List<NexusAssetInfo> assetList = this.nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, this.unconfirmedPlatformAppRepository, platformCfgDirectory, fileList.toArray(new DeployFileInfo[0]));
            Map<String, String> fileUrlMap = new HashMap<>();
            for (NexusAssetInfo assetInfo : assetList) {
                String[] arr = assetInfo.getPath().split("/");
                fileUrlMap.put(arr[arr.length - 1], String.format("%s/%s", this.unconfirmedPlatformAppRepository, assetInfo.getPath()));
            }
            if (StringUtils.isNotBlank(moduleVo.getInstallPackage().getLocalSavePath())) {
                po.setPackageDownloadUrl(fileUrlMap.get(moduleVo.getInstallPackage().getFileName()));
                fileUrlMap.remove(moduleVo.getInstallPackage().getFileName());
            } else
                po.setPackageDownloadUrl("");
            po.setCfgDownloadUrl(String.join(",", fileUrlMap.values()));
        }
        return po;
    }

    private void updatePlatformPublicConfig(List<AppFileNexusInfo> platformCfg)
    {
        try
        {
            List<PlatformPo> platforms = this.platformMapper.select(null);
            for(PlatformPo platform : platforms)
            {
                if(!platform.getType().equals(PlatformType.K8S_CONTAINER))
                    continue;
                for(AppFileNexusInfo cfg : platformCfg)
                {
                    PlatformPublicConfigPo configPo = new PlatformPublicConfigPo(platform.getPlatformId(), cfg);
                    if(platform.getCcodVersion().equals("3.9"))
                        configPo.setDeployPath(configPo.getDeployPath().replaceAll("^/root/", "/opt/"));
                    this.platformPublicConfigMapper.insert(configPo);
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public V1Namespace queryPlatformK8sNamespace(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("begin to query k8s namespace of %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        return this.k8sApiService.readNamespace(platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
    }

    @Override
    public List<V1Pod> queryPlatformAllK8sPods(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("begin to query k8s pods of %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        return this.k8sApiService.listNamespacedPod(platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
    }

    @Override
    public V1Pod queryPlatformK8sPodByName(String platformId, String podName) throws ParamException, ApiException {
        logger.debug(String.format("begin to query k8s pod %s of %s", podName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        return this.k8sApiService.readNamespacedPod(podName, platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
    }

    @Override
    public List<V1Service> queryPlatformAllK8sServices(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("begin to query k8s services of %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        return this.k8sApiService.listNamespacedService(platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
    }

    @Override
    public V1Service queryPlatformK8sServiceByName(String platformId, String serviceName) throws ParamException, ApiException {
        logger.debug(String.format("begin to query k8s service %s of %s", serviceName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        return this.k8sApiService.readNamespacedService(serviceName, platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
    }

    @Override
    public V1Service createK8sPlatformService(String platformId, V1Service service) throws ParamException, ApiException {
        logger.debug(String.format("create new Service for platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Service create = this.k8sApiService.createNamespacedService(platformId, service, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return create;
    }

    @Override
    public void deleteK8sPlatformService(String platformId, String serviceName) throws ParamException, ApiException {
        logger.debug(String.format("delete Service %s from platform %s"));
        PlatformPo platformPo = getK8sPlatform(platformId);
        this.k8sApiService.deleteNamespacedEndpoints(serviceName, platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
    }

    @Override
    public V1Service replaceK8sPlatformService(String platformId, String serviceName, V1Service service) throws ParamException, ApiException {
        logger.debug(String.format("replace Service %s of platform %s", serviceName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Service replace = this.k8sApiService.replaceNamespacedService(serviceName, platformId, service, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return replace;
    }

    @Override
    public List<V1ConfigMap> queryPlatformAllK8sConfigMaps(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("begin to query k8s configMap of %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        return this.k8sApiService.listNamespacedConfigMap(platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
    }

    @Override
    public V1ConfigMap queryPlatformK8sConfigMapByName(String platformId, String configMapName) throws ParamException, ApiException {
        logger.debug(String.format("begin to query k8s configMap %s of %s", configMapName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        return this.k8sApiService.readNamespacedConfigMap(platformId, configMapName, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
    }

    private PlatformPo getK8sPlatform(String platformId) throws ParamException {
        PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
        Assert.notNull(platformPo, String.format("%s platform not exit", platformId));
        Assert.isTrue(PlatformType.K8S_CONTAINER.equals(platformPo.getType()), String.format("platform %s type is %s not %s", platformId, platformPo.getType().name, PlatformType.K8S_CONTAINER.name));
        if (StringUtils.isBlank(platformPo.getK8sApiUrl())) {
            logger.error(String.format("k8s api url of %s is blank", platformId));
            throw new ParamException(String.format("k8s api url of %s is blank", platformId));
        }
        if (StringUtils.isBlank(platformPo.getK8sAuthToken())) {
            logger.error(String.format("k8s auth token of %s is blank", platformId));
            throw new ParamException(String.format("k8s auth token of %s is blank", platformId));
        }
        return platformPo;
    }

    @Override
    public List<V1Deployment> queryPlatformAllK8sDeployment(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("query all deployment of platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        List<V1Deployment> list = this.k8sApiService.listNamespacedDeployment(platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return list;
    }

    @Override
    public V1Deployment queryPlatformK8sDeploymentByName(String platformId, String deploymentName) throws ParamException, ApiException {
        logger.debug(String.format("query deployment %s at platform %s", deploymentName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Deployment deployment = this.k8sApiService.readNamespacedDeployment(deploymentName, platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return deployment;
    }

    @Override
    public V1Deployment createK8sPlatformDeployment(String platformId, V1Deployment deployment) throws ParamException, ApiException {
        logger.debug(String.format("create new Deployment for platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Deployment create = this.k8sApiService.createNamespacedDeployment(platformId, deployment, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return create;
    }

    @Override
    public void deleteK8sPlatformDeployment(String platformId, String deploymentName) throws ParamException, ApiException {
        logger.debug(String.format("delete Deployment %s from platform %s"));
        PlatformPo platformPo = getK8sPlatform(platformId);
        this.k8sApiService.deleteNamespacedEndpoints(deploymentName, platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
    }

    @Override
    public V1Deployment replaceK8sPlatformDeployment(String platformId, String deploymentName, V1Deployment deployment) throws ParamException, ApiException {
        logger.debug(String.format("replace Deployment %s of platform %s", deploymentName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Deployment replace = this.k8sApiService.replaceNamespacedDeployment(deploymentName, platformId, deployment, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return replace;
    }

    @Override
    public ExtensionsV1beta1Ingress queryPlatformK8sIngressByName(String platformId, String ingressName) throws ParamException, ApiException {
        logger.debug(String.format("query ingress %s at platform %s", ingressName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        ExtensionsV1beta1Ingress ingress = this.k8sApiService.readNamespacedIngress(ingressName, platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return ingress;
    }

    @Override
    public List<ExtensionsV1beta1Ingress> queryPlatformAllK8sIngress(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("query all ingress of platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        List<ExtensionsV1beta1Ingress> list = this.k8sApiService.listNamespacedIngress(platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return list;
    }

    @Override
    public ExtensionsV1beta1Ingress createK8sPlatformIngress(String platformId, ExtensionsV1beta1Ingress ingress) throws ParamException, ApiException {
        logger.debug(String.format("create new Ingress for platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        ExtensionsV1beta1Ingress create = this.k8sApiService.createNamespacedIngress(platformId, ingress, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return create;
    }

    @Override
    public void deleteK8sPlatformIngress(String platformId, String ingressName) throws ParamException, ApiException {
        logger.debug(String.format("delete Ingress %s from platform %s"));
        PlatformPo platformPo = getK8sPlatform(platformId);
        this.k8sApiService.deleteNamespacedIngress(ingressName, platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
    }

    @Override
    public ExtensionsV1beta1Ingress replaceK8sPlatformIngress(String platformId, String ingressName, ExtensionsV1beta1Ingress ingress) throws ParamException, ApiException {
        logger.debug(String.format("replace Ingress %s of platform %s", ingressName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        ExtensionsV1beta1Ingress replace = this.k8sApiService.replaceNamespacedIngress(ingressName, platformId, ingress, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return replace;
    }

    @Override
    public List<V1Endpoints> queryPlatformAllK8sEndpoints(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("query all endpoints of platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        List<V1Endpoints> list = this.k8sApiService.listNamespacedEndpoints(platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return list;
    }

    @Override
    public V1Endpoints queryPlatformK8sEndpointsByName(String platformId, String endpointsName) throws ParamException, ApiException {
        logger.debug(String.format("query endpoints %s at platform %s", endpointsName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Endpoints endpoints = this.k8sApiService.readNamespacedEndpoints(endpointsName, platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return endpoints;
    }

    @Override
    public V1Endpoints createK8sPlatformEndpoints(String platformId, V1Endpoints endpoints) throws ParamException, ApiException {
        logger.debug(String.format("create new Endpoints for platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Endpoints create = this.k8sApiService.createNamespacedEndpoints(platformId, endpoints, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return create;
    }

    @Override
    public void deleteK8sPlatformEndpoints(String platformId, String endpointsName) throws ParamException, ApiException {
        logger.debug(String.format("delete Endpoints %s from platform %s"));
        PlatformPo platformPo = getK8sPlatform(platformId);
        this.k8sApiService.deleteNamespacedEndpoints(endpointsName, platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
    }

    @Override
    public V1Endpoints replaceK8sPlatformEndpoints(String platformId, String endpointName, V1Endpoints endpoints) throws ParamException, ApiException {
        logger.debug(String.format("replace Endpoints %s of platform %s", endpointName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Endpoints replace = this.k8sApiService.replaceNamespacedEndpoints(endpointName, platformId, endpoints, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return replace;
    }

    @Override
    public List<V1Secret> queryPlatformAllK8sSecret(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("query all endpoints of platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        List<V1Secret> list = this.k8sApiService.listNamespacedSecret(platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return list;
    }

    @Override
    public V1Secret queryPlatformK8sSecretByName(String platformId, String secretName) throws ParamException, ApiException {
        logger.debug(String.format("query secretName %s at platform %s", secretName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Secret secret = this.k8sApiService.readNamespacedSecret(secretName, platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return secret;
    }

    @Override
    public V1Secret createK8sPlatformSecret(String platformId, V1Secret secret) throws ParamException, ApiException {
        logger.debug(String.format("create new Secret for platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Secret create = this.k8sApiService.createNamespacedSecret(platformId, secret, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return create;
    }

    @Override
    public void deleteK8sPlatformSecret(String platformId, String secretName) throws ParamException, ApiException {
        logger.debug(String.format("delete Secret %s from platform %s"));
        PlatformPo platformPo = getK8sPlatform(platformId);
        this.k8sApiService.deleteNamespacedSecret(secretName, platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
    }

    @Override
    public V1Secret replaceK8sPlatformSecret(String platformId, String secretName, V1Secret secret) throws ParamException, ApiException {
        logger.debug(String.format("replace Secret %s of platform %s", secretName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Secret replace = this.k8sApiService.replaceNamespacedSecret(secretName, platformId, secret, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return replace;
    }

    @Override
    public List<V1PersistentVolumeClaim> queryPlatformAllK8sPersistentVolumeClaim(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("query all PersistentVolumeClaim of platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        List<V1PersistentVolumeClaim> list = this.k8sApiService.listNamespacedPersistentVolumeClaim(platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return list;
    }

    @Override
    public V1PersistentVolumeClaim queryPlatformK8sPersistentVolumeClaimByName(String platformId, String persistentVolumeClaimName) throws ParamException, ApiException {
        logger.debug(String.format("query PersistentVolumeClaim %s at platform %s", persistentVolumeClaimName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1PersistentVolumeClaim claim = this.k8sApiService.readNamespacedPersistentVolumeClaim(persistentVolumeClaimName, platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return claim;
    }

    @Override
    public V1PersistentVolumeClaim createK8sPlatformPersistentVolumeClaim(String platformId, V1PersistentVolumeClaim persistentVolumeClaim) throws ParamException, ApiException {
        logger.debug(String.format("create new PersistentVolumeClaim for platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1PersistentVolumeClaim create = this.k8sApiService.createNamespacedPersistentVolumeClaim(platformId, persistentVolumeClaim, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return create;
    }

    @Override
    public void deleteK8sPlatformPersistentVolumeClaim(String platformId, String persistentVolumeClaimName) throws ParamException, ApiException {
        logger.debug(String.format("delete PersistentVolumeClaim %s from platform %s"));
        PlatformPo platformPo = getK8sPlatform(platformId);
        this.k8sApiService.deleteNamespacedPersistentVolumeClaim(persistentVolumeClaimName, platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
    }

    @Override
    public V1PersistentVolumeClaim replaceK8sPlatformPersistentVolumeClaim(String platformId, String persistentVolumeClaimName, V1PersistentVolumeClaim persistentVolumeClaim) throws ParamException, ApiException {
        logger.debug(String.format("replace PersistentVolumeClaim %s of platform %s", persistentVolumeClaim, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1PersistentVolumeClaim replace = this.k8sApiService.replaceNamespacedPersistentVolumeClaim(persistentVolumeClaimName, platformId, persistentVolumeClaim, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
        return replace;
    }

    /**
     * 根据域id获得域所在的业务集群
     *
     * @param domainId 域id
     * @return 域所在的业务集群
     */
    private BizSetDefine getBizSetForDomainId(String domainId) throws ParamException {
        logger.debug(String.format("to find bisSet for domainId %s", domainId));
        BizSetDefine setDefine = null;
        for (BizSetDefine set : this.ccodBiz.getSet()) {
            String regex = String.format("^%s(0[1-9]|[1-9]\\d+$)", set.getFixedDomainId());
            if (domainId.matches(regex)) {
                setDefine = set;
                break;
            }
        }
        if (setDefine == null)
            throw new ParamException(String.format("%s is illegal domainId for ccod bizSet", domainId));
        logger.debug(String.format("bizSet for domainId %s found", domainId));
        return setDefine;
    }

    private AppType getAppTypeFromImageUrl(String imageUrl) throws ParamException, NotSupportAppException {
        Map<String, List<AppModuleVo>> registerAppMap = this.appManagerService.queryAllRegisterAppModule(true)
                .stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        String[] arr = imageUrl.split("/");
        if (arr.length != 3)
            throw new ParamException(String.format("%s is illegal imageUrl", imageUrl));
        String repository = arr[1];
        arr = arr[2].split("\\:");
        if (arr.length != 2)
            throw new ParamException(String.format("%s is illegal image tag", arr[2]));
        String appName = arr[0];
        String version = arr[1].replaceAll("\\-", ":");
        Set<String> ccodRepSet = new HashSet<>(imageCfg.getCcodModuleRepository());
        Set<String> threeAppRepSet = new HashSet<>(imageCfg.getThreeAppRepository());
        AppType appType = null;
        if (ccodRepSet.contains(repository)) {
            for (String name : registerAppMap.keySet()) {
                if (name.toLowerCase().equals(appName)) {
                    List<AppModuleVo> modules = registerAppMap.get(name);
                    appType = modules.stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version) ? modules.stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version).getAppType() : null;
                    if (appType == null)
                        throw new ParamException(String.format("%s[%s] not register", name, version));
                    break;
                }
            }
            if (appType == null)
                throw new NotSupportAppException(String.format("ccod module %s not supported", appName));
        } else if (threeAppRepSet.contains(repository))
            appType = AppType.THREE_PART_APP;
        else
            appType = AppType.OTHER;
        logger.debug(String.format("type of image %s is %s", imageUrl, appType.name));
        return appType;
    }

    /**
     * 根据域id和别名获取模块名称
     *
     * @param domainId 域id
     * @param alias    别名
     * @return 对应的模块名称
     * @throws ParamException 别名或域id不正确
     */
    private String getAppNameFromDomainIdAndAlias(String domainId, String alias) throws ParamException {
        BizSetDefine setDefine = null;
        for (BizSetDefine set : this.ccodBiz.getSet()) {
            String domainIdRegex = String.format(this.domainIdRegexFmt, set.getFixedDomainId());
            if (domainId.matches(domainIdRegex)) {
                setDefine = set;
                break;
            }
        }
        if (setDefine == null)
            throw new ParamException(String.format("%s is illegal domainId", domainId));
        AppDefine appDefine = null;
        for (AppDefine define : setDefine.getApps()) {
            String aliasRegex = String.format(this.aliasRegexFmt, define.getAlias());
            if (alias.matches(aliasRegex)) {
                appDefine = define;
                break;
            }
        }
        if (appDefine == null)
            throw new ParamException(String.format("%s is illegal alias for %s", alias, setDefine.getFixedDomainName()));
        logger.debug(String.format("%s is the appName for %s at %s", appDefine.getName(), alias, domainId));
        return appDefine.getName();
    }

    /**
     * 获取镜像标签对应的应用模块信息
     *
     * @param imageTag           镜像标签
     * @param registerAppModules 已经注册的应用模块列表
     * @return 应用模块信息
     * @throws ParamException
     * @throws NotSupportAppException
     */
    private AppModuleVo getAppModuleFromImageTag(String imageTag, List<AppModuleVo> registerAppModules) throws ParamException, NotSupportAppException {
        Map<String, List<AppModuleVo>> registerAppMap = registerAppModules.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        String[] arr = imageTag.split("/");
        String appName = arr[arr.length - 1].split("\\:")[0];
        String version = arr[arr.length - 1].split("\\:")[1];
        for (String name : registerAppMap.keySet()) {
            if (name.toLowerCase().equals(appName)) {
                for (AppModuleVo moduleVo : registerAppMap.get(name)) {
                    if (moduleVo.getVersion().replaceAll("\\:", "-").equals(version)) {
                        logger.debug(String.format("app module for %s found", imageTag));
                        return moduleVo;
                    }
                }
                throw new ParamException(String.format("can not find ccod app module for %s", imageTag));
            }
        }
        throw new ParamException(String.format("can not find ccod app module for %s", imageTag));
    }

    /**
     * 如果labels包含selector的所有key，并且值也相同返回true, 否则返回false
     *
     * @param labels
     * @param selector
     * @return 比较结果
     */
    boolean isSelected(Map<String, String> labels, Map<String, String> selector) {
        for (String key : selector.keySet()) {
            if (!labels.containsKey(key) || !selector.get(key).equals(labels.get(key)))
                return false;
        }
        return true;
    }

    /**
     * 从k8s服务中获取第三方服务信息
     *
     * @param service k8s服务
     * @return 第三方服务信息
     */
    private PlatformThreePartServicePo getK8sPlatformThreePartService(V1Service service) {
        PlatformThreePartServicePo po = new PlatformThreePartServicePo();
        po.setServiceName(service.getMetadata().getName());
        po.setPlatformId(service.getMetadata().getNamespace());
        po.setHostIp(service.getSpec().getClusterIP());
        po.setPort(getPortFromK8sService(Arrays.asList(service)));
        return po;
    }

    /**
     * 获取第三方应用相关信息
     *
     * @param deployment 定义第三方应用的deployment
     * @param container  用来运行第三方应用的container
     * @param services   第三方应用关联的服务信息
     * @return 第三方应用相关信息
     */
    private PlatformThreePartAppPo getK8sPlatformThreePartApp(V1Deployment deployment, V1Container container, List<V1Service> services) {
        PlatformThreePartAppPo po = new PlatformThreePartAppPo();
        int replicas = deployment.getStatus().getReplicas() != null ? deployment.getStatus().getReplicas() : 0;
        int availableReplicas = deployment.getStatus().getAvailableReplicas() != null ? deployment.getStatus().getAvailableReplicas() : 0;
        int unavailableReplicas = deployment.getStatus().getUnavailableReplicas() != null ? deployment.getStatus().getUnavailableReplicas() : 0;
        po.setReplicas(replicas);
        po.setAvailableReplicas(availableReplicas);
        String status = "Running";
        if (unavailableReplicas == replicas)
            status = "Error";
        else if (availableReplicas < replicas)
            status = "Updating";
        po.setStatus(status);
        po.setPort(getPortFromK8sService(services));
        po.setHostIp(services.get(0).getSpec().getClusterIP());
        po.setPlatformId(deployment.getMetadata().getNamespace());
        po.setAppName(container.getName());
        return po;
    }

    /**
     * 获取k8s平台上部署的ccod域模块明细
     *
     * @param platformName 平台名
     * @param ccodVersion  平台的ccod大版本
     * @param deployment   定义ccod域模块的deployment
     * @param container    加载ccod域模块的镜像容器
     * @param services     关联模块的服务
     * @param appModuleVo  ccod域模块定义
     * @param configMap    该域模块对应的configMap
     * @return ccod域模块明细
     * @throws ParamException
     */
    private PlatformAppDeployDetailVo getK8sPlatformAppDeployDetail(String platformName, String ccodVersion, V1Deployment deployment, V1Container container, List<V1Service> services, AppModuleVo appModuleVo, Map<String, String> configMap) throws ParamException {
        String platformId = deployment.getMetadata().getNamespace();
        PlatformAppDeployDetailVo vo = new PlatformAppDeployDetailVo();
        vo.setAppId(appModuleVo.getAppId());
        vo.setPlatformId(platformId);
        vo.setPlatformName(platformName);
        String[] arr = deployment.getMetadata().getName().split("\\-");
        vo.setAssembleTag(arr[0]);
        String domainId = arr[arr.length - 1];
        BizSetDefine setDefine = getBizSetForDomainId(domainId);
        vo.setDomainId(domainId);
        vo.setDomainName(String.format("%s%s", setDefine.getFixedDomainName(), domainId.replaceAll(setDefine.getFixedDomainId(), "")));
        String alias = container.getName().split("\\-")[0];
        vo.setAlias(alias);
        vo.setOriginalAlias(alias);
        vo.setAppType(appModuleVo.getAppType());
        vo.setHostIp(services.get(0).getSpec().getClusterIP());
        vo.setPorts(getPortFromK8sService(services));
        int replicas = deployment.getStatus().getReplicas() != null ? deployment.getStatus().getReplicas() : 0;
        int availableReplicas = deployment.getStatus().getAvailableReplicas() != null ? deployment.getStatus().getAvailableReplicas() : 0;
        int unavailableReplicas = deployment.getStatus().getUnavailableReplicas() != null ? deployment.getStatus().getUnavailableReplicas() : 0;
        vo.setReplicas(replicas);
        vo.setAvailableReplicas(availableReplicas);
        String status = "Running";
        if (unavailableReplicas == replicas)
            status = "Error";
        else if (availableReplicas < replicas)
            status = "Updating";
        vo.setStatus(status);
        vo.setAppRunner(alias);
        vo.setCcodVersion(ccodVersion);
        vo.setBasePath("/");
        Date now = new Date();
        vo.setDeployTime(now);
        vo.setVersion(appModuleVo.getVersion());
        vo.setCreateTime(now);
        vo.setVersionControl(appModuleVo.getVersionControl().name);
        vo.setVersionControlUrl(appModuleVo.getVersionControlUrl());
        vo.setInstallPackage(appModuleVo.getInstallPackage());
        vo.setCfgs(new ArrayList<>());
        vo.setBkSetName(setDefine.getName());
        return vo;
    }

    /**
     * 将configMap中的配置文件信息上传到指定的nexus路径下
     *
     * @param configMap 需要上传配置文件的configMap信息
     * @param directory 保存配置文件的路径
     * @return 上传结果
     * @throws IOException
     * @throws InterfaceCallException
     * @throws NexusException
     */
    private List<NexusAssetInfo> uploadK8sConfigMapToNexus(V1ConfigMap configMap, String directory) throws IOException, InterfaceCallException, NexusException {
        List<DeployFileInfo> fileList = new ArrayList<>();
        String saveDir = getTempSaveDir(this.platformAppCfgRepository);
        for (String fileName : configMap.getData().keySet()) {
            String content = configMap.getData().get(fileName);
            String fileSavePath = String.format("%s/%s", saveDir, fileName).replaceAll("\\\\", "/");
            File file = new File(fileSavePath);
            file.createNewFile();
            FileWriter writer = new FileWriter(file);
            BufferedWriter out = new BufferedWriter(writer);
            out.write(content);
            out.close();
            writer.close();
            DeployFileInfo info = new DeployFileInfo();
            info.setFileName(fileName);
            info.setLocalSavePath(fileSavePath);
            fileList.add(info);
        }
        List<NexusAssetInfo> assets = this.nexusService.uploadRawComponent(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, this.platformAppCfgRepository, directory, fileList.toArray(new DeployFileInfo[0]));
        return assets;
    }

    /**
     * 获取临时存放路径
     *
     * @param directory
     * @return 临时存放路径
     */
    private String getTempSaveDir(String directory) {
        String saveDir = String.format("%s/downloads/%s", System.getProperty("user.dir"), directory);
        return saveDir;
    }

    private List<K8sCollection> parseAppK8sCollection(String domainId, List<V1Deployment> deployments, List<V1Service> services, List<ExtensionsV1beta1Ingress> ingresses, List<AppModuleVo> registerApps) throws ParamException, K8sDataException, NotSupportAppException
    {
        BizSetDefine setDefine = getBizSetForDomainId(domainId);
        Map<String, AppDefine> appDefineMap = setDefine.getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity()));
        List<K8sCollection> collections = new ArrayList<>();
        Map<String, V1Service> serviceMap = new HashMap<>();
        Map<String, V1ConfigMap> configMapMap = new HashMap<>();
        for(V1Service service : services)
            serviceMap.put(service.getMetadata().getName(), service);
        Map<String, ExtensionsV1beta1Ingress> ingressMap = new HashMap<>();
        for(ExtensionsV1beta1Ingress ingress : ingresses)
            ingressMap.put(ingress.getMetadata().getName(), ingress);
        for(V1Deployment deployment : deployments)
        {
            String deployName = deployment.getMetadata().getName();
            List<V1Container> initContainers = deployment.getSpec().getTemplate().getSpec().getInitContainers();
            if(initContainers == null || initContainers.size() == 0)
                throw new K8sDataException(String.format("deployment %s has not any init container", deployName));
            List<V1Container> runtimeContainers = deployment.getSpec().getTemplate().getSpec().getContainers();
            if(runtimeContainers == null || runtimeContainers.size() == 0)
                throw new K8sDataException(String.format("deployment %s has not any init container", deployName));
            if(initContainers.size() != runtimeContainers.size())
                throw new K8sDataException(String.format("deployment %s has %d init container and %d runtime container",
                        deployName, initContainers.size(), runtimeContainers.size()));
            Map<String, V1Container> initMap = initContainers.stream().collect(Collectors.toMap(V1Container::getName, Function.identity()));
            Map<String, V1Container> runtimeMap = runtimeContainers.stream().collect(Collectors.toMap(V1Container::getName, Function.identity()));
            for(String alias : initMap.keySet()) {
                if (!runtimeMap.containsKey(String.format("%s-runtime", alias)))
                    throw new K8sDataException(String.format("deployment %s not find runtime container for %s", deployName, alias));
                V1Container initContainer = initMap.get(alias);
                V1Container runtimeContainer = runtimeMap.get(String.format("%s-runtime", alias));
                AppModuleVo appModule = getAppModuleFromImageTag(initContainer.getImage(), registerApps);
                String appName = appModule.getAppName();
                if (!appDefineMap.containsKey(appName))
                    throw new K8sDataException(String.format("app module %s in deployment %s not support by %s", appName, domainId, setDefine.getName()));
                String regex = String.format(this.aliasRegexFmt, appDefineMap.get(appName).getAlias());
                if (!alias.matches(regex))
                    throw new K8sDataException(String.format("%s in deployment %s is illegal alias for %s", alias, deployName, appName));
                List<V1Service> svcs = new ArrayList<>();
                List<ExtensionsV1beta1Ingress> ings = new ArrayList<>();
                for (String svcName : serviceMap.keySet()) {
                    if (svcName.matches(String.format(this.domainServiceNameFmt, alias, domainId))) {
                        svcs.add(serviceMap.get(svcName));
                        if (ingressMap.containsKey(svcName))
                            ings.add(ingressMap.get(svcName));
                    }
                }
                K8sCollection collection = new K8sCollection(domainId, appModule, alias, deployment, svcs, ings);
                collections.add(collection);
                for (V1Service service : svcs)
                    serviceMap.remove(service.getMetadata().getName());
                for (ExtensionsV1beta1Ingress ingress : ings)
                    ingressMap.remove(ingress.getMetadata().getName());
            }
        }
        if(serviceMap.size() > 0)
            throw new K8sDataException(String.format("service %s at %s has not relative to any app",
                    String.join(",", serviceMap.keySet()), domainId));
        if(ingressMap.size() > 0)
            throw new K8sDataException(String.format("ingress %s at %s has not relative to any app",
                    String.join(",", ingressMap.keySet()), domainId));
        return collections;
    }

    List<K8sOperationInfo> generateDomainK8sOperation(String jobId, String platformId, String domainId, DomainUpdatePlanInfo planInfo, List<K8sCollection> existDomainCollections,  List<V1ConfigMap> existDomainConfigMaps, List<AppFileNexusInfo> platformCfg, List<AppFileNexusInfo> domainCfg, BizSetDefine setDefine, List<AppModuleVo> registerApps) throws ParamException, K8sDataException, NotSupportAppException, IOException, InterfaceCallException
    {
        Map<String, K8sCollection> existMap = existDomainCollections.stream().collect(Collectors.toMap(K8sCollection::getAlias, Function.identity()));
        Map<String, K8sCollection> targetMap = parseAppK8sCollection(domainId, planInfo.getDeployments(), planInfo.getServices(), planInfo.getIngresses(), registerApps)
                .stream().collect(Collectors.toMap(K8sCollection::getAlias, Function.identity()));
        Set<String> notRelativeApps = new HashSet<>();
        Map<String, List<AppModuleVo>> registerAppMap = registerApps.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        for(AppUpdateOperationInfo info : planInfo.getAppUpdateOperationList())
        {
            if(!info.getOperation().equals(AppUpdateOperation.DELETE) && !targetMap.containsKey(info.getAlias()))
                notRelativeApps.add(info.getAlias());
        }
        if(notRelativeApps.size() > 0)
            throw new ParamException(String.format("app %s of %s has not relative to any deployment", String.join(",", notRelativeApps), domainId));
        Map<AppUpdateOperation, List<AppUpdateOperationInfo>> optMap = planInfo.getAppUpdateOperationList().stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getOperation));
        Map<String, V1ConfigMap> configMapMap = new HashMap<>();
        for(V1ConfigMap configMap : existDomainConfigMaps)
            configMapMap.put(configMap.getMetadata().getName(), configMap);
        Map<String, Integer> appSortMap = new HashMap<>();
        for(int i = 0; i < setDefine.getApps().size(); i++)
            appSortMap.put(setDefine.getApps().get(i).getName(), i);
        Comparator<AppUpdateOperationInfo> sort = new Comparator<AppUpdateOperationInfo>() {
            @Override
            public int compare(AppUpdateOperationInfo o1, AppUpdateOperationInfo o2) {
                return appSortMap.get(o1.getAppName()) - appSortMap.get(o2.getAppName());
            }
        };
        List<AppUpdateOperationInfo> deleteList = optMap.containsKey(AppUpdateOperation.DELETE) ? optMap.get(AppUpdateOperation.DELETE) : new ArrayList<>();
        Collections.sort(deleteList, sort);
        List<K8sCollection> deleteCollections = new ArrayList<>();
        for(int i = deleteList.size() - 1; i >=0; i--) {
            AppUpdateOperationInfo deleted = deleteList.get(i);
            if(!existMap.containsKey(deleted.getAlias()))
                throw new ParamException(String.format("not find exist k8s definition for DELETE %s", deleted.getAlias()));
            deleteCollections.add(existMap.get(deleted.getAlias()));
        }
        List<AppUpdateOperationInfo> addList = optMap.containsKey(AppUpdateOperation.ADD) ? optMap.get(AppUpdateOperation.ADD) : new ArrayList<>();
        Collections.sort(addList, sort);
        List<K8sCollection> addCollections = new ArrayList<>();
        for(AppUpdateOperationInfo optInfo : addList)
        {
            if(!targetMap.containsKey(optInfo.getAlias()))
                throw new ParamException(String.format("not find target k8s definition for ADD %s", optInfo.getAlias()));
            addCollections.add(targetMap.get(optInfo.getAlias()));
        }
        List<AppUpdateOperationInfo> updateList = optMap.containsKey(AppUpdateOperation.UPDATE) ? optMap.get(AppUpdateOperation.UPDATE) : new ArrayList<>();
        Collections.sort(updateList, sort);
        for(AppUpdateOperationInfo optInfo : updateList) {
            if(!existMap.containsKey(optInfo.getAlias()))
                throw new ParamException(String.format("not find exist k8s definition for UPDATE %s", optInfo.getAlias()));
            deleteCollections.add(existMap.get(optInfo.getAlias()));
            if(!targetMap.containsKey(optInfo.getAlias()))
                throw new ParamException(String.format("not find target k8s definition for UPDATE %s", optInfo.getAlias()));
            addCollections.add(targetMap.get(optInfo.getAlias()));
        }
        deleteList.addAll(updateList);
        addList.addAll(updateList);
        List<K8sOperationInfo> k8sOptList = new ArrayList<>();
        List<NexusAssetInfo> cfgs = new ArrayList<>();
        if(planInfo.getUpdateType().equals(DomainUpdateType.ADD) && planInfo.getPublicConfig() != null && planInfo.getPublicConfig().size() > 0)
        {
            for(AppFileNexusInfo cfgFile : domainCfg)
            {
                NexusAssetInfo cfg = cfgFile.getNexusAssetInfo(this.nexusHostUrl);
                cfgs.add(cfg);
            }
            V1ConfigMap configMap = this.k8sApiService.getConfigMapFromNexus(platformId, domainId,
                    cfgs, this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
            K8sOperationInfo k8sOpt = new K8sOperationInfo(jobId, platformId, planInfo.getDomainId(), K8sKind.CONFIGMAP,
                    domainId, K8sOperation.CREATE, configMap);
            k8sOptList.add(k8sOpt);
        }
        Map<V1Deployment, List<K8sCollection>> deleteMap = deleteCollections.stream().collect(Collectors.groupingBy(K8sCollection::getDeployment));
        for(V1Deployment deployment : deleteMap.keySet())
        {
            K8sOperationInfo k8sOpt;
            for(K8sCollection collection : deleteMap.get(deployment))
            {
                if(!deleteList.stream().collect(Collectors.toMap(AppUpdateOperationInfo::getAlias, Function.identity())).containsKey(collection.getAlias()))
                    throw new ParamException(String.format("%s not in %s DELETE list", collection.getAlias(), domainId));
                for(ExtensionsV1beta1Ingress ingress : collection.getIngresses())
                {
                    if(!ingress.getMetadata().getNamespace().equals(platformId))
                        throw new ParamException(String.format("error namespace of deleted ingress %s:wanted %s,actual %s",
                                ingress.getMetadata().getName(), platformId, ingress.getMetadata().getNamespace()));
                    k8sOpt = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.INGRESS,
                            ingress.getMetadata().getName(), K8sOperation.DELETE, ingress);
                    k8sOptList.add(k8sOpt);
                }
                for(V1Service service : collection.getServices())
                {
                    if(!service.getMetadata().getNamespace().equals(platformId))
                        throw new ParamException(String.format("error namespace of deleted service %s:wanted %s,actual %s",
                                service.getMetadata().getName(), platformId, service.getMetadata().getNamespace()));
                    k8sOpt = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE,
                            service.getMetadata().getName(), K8sOperation.DELETE, service);
                    k8sOptList.add(k8sOpt);
                }
            }
            k8sOpt = new K8sOperationInfo(jobId, platformId, planInfo.getDomainId(), K8sKind.DEPLOYMENT,
                    deployment.getMetadata().getName(), K8sOperation.DELETE, deployment);
            if(!deployment.getMetadata().getNamespace().equals(platformId))
                throw new ParamException(String.format("error namespace of deleted deployment %s:wanted %s,actual %s",
                        deployment.getMetadata().getName(), platformId, deployment.getMetadata().getNamespace()));
            k8sOptList.add(k8sOpt);
        }
        for(K8sCollection collection : deleteCollections)
        {
            String configName = String.format("%s-%s", collection.getAlias(), domainId);
            if(!configMapMap.containsKey(configName))
                throw new K8sDataException(String.format("configMap %s not exist", configName));
            K8sOperationInfo k8sOpt = new K8sOperationInfo(jobId, platformId, planInfo.getDomainId(), K8sKind.CONFIGMAP,
                    configName, K8sOperation.DELETE, configMapMap.get(configName));
            k8sOptList.add(k8sOpt);
            configMapMap.remove(configName);
        }
        for(K8sCollection collection : addCollections)
        {
            String alias = collection.getAlias();
            String configName = String.format("%s-%s", collection.getAlias(), domainId);
            List<AppFileNexusInfo> cfgFiles = planInfo.getAppUpdateOperationList().stream().collect(Collectors.toMap(AppUpdateOperationInfo::getAlias, Function.identity())).get(alias).getCfgs();
            cfgs = new ArrayList<>();
            for(AppFileNexusInfo cfgFile : cfgFiles)
            {
                NexusAssetInfo cfg = cfgFile.getNexusAssetInfo(this.nexusHostUrl);
                cfgs.add(cfg);
            }
            V1ConfigMap configMap = this.k8sApiService.getConfigMapFromNexus(platformId, String.format("%s-%s", alias, domainId),
                    cfgs, this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
            K8sOperationInfo k8sOpt = new K8sOperationInfo(jobId, platformId, planInfo.getDomainId(), K8sKind.CONFIGMAP,
                    configName, K8sOperation.CREATE, configMap);
            k8sOptList.add(k8sOpt);
        }
        Map<V1Deployment, List<K8sCollection>> addMap = addCollections.stream().collect(Collectors.groupingBy(K8sCollection::getDeployment));
        List<V1Deployment> addDeploys = new ArrayList<>(addMap.keySet());
        Comparator<V1Deployment> deploySort = new Comparator<V1Deployment>() {
            @Override
            public int compare(V1Deployment o1, V1Deployment o2) {
                String appName1 = addMap.get(o1).get(0).getAppName();
                String appName2 = addMap.get(o2).get(0).getAppName();
                if(!appName1.equals(appName2))
                    return appSortMap.get(appName1) - appSortMap.get(appName2);
                Map<String, AppDefine> appDefineMap = setDefine.getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity()));
                String standAlias = appDefineMap.get(appName1).getAlias();
                return Integer.parseInt(addMap.get(o1).get(0).getAlias().replace(standAlias, "")) - Integer.parseInt(addMap.get(o2).get(0).getAlias().replace(standAlias, ""));

            }
        };
        Collections.sort(addDeploys, deploySort);
        for(V1Deployment deployment : addDeploys)
        {
            if(!deployment.getMetadata().getNamespace().equals(platformId))
                throw new ParamException(String.format("error namespace of add deployment %s:wanted %s,actual %s",
                        deployment.getMetadata().getName(), platformId, deployment.getMetadata().getNamespace()));
            List<K8sCollection> collections = addMap.get(deployment);
//            if(deployment.getMetadata().getLabels() == null)
//                deployment.getMetadata().setLabels(new HashMap<>());
//            deployment.getMetadata().getLabels().put(this.deploymentTypeLabel, K8sDeploymentType.CCOD_DOMAIN_APP.name);
//            deployment.getMetadata().getLabels().put(this.domainIdLabel, domainId);
//            deployment.getMetadata().getLabels().put(this.jobIdLabel, jobId);
//            deployment.getSpec().getSelector().getMatchLabels().put(this.domainIdLabel, domainId);
//            deployment.getSpec().getTemplate().getMetadata().getLabels().put(this.domainIdLabel, domainId);
            K8sOperationInfo deployOpt = new K8sOperationInfo(jobId, platformId, planInfo.getDomainId(), K8sKind.DEPLOYMENT,
                    deployment.getMetadata().getName(), K8sOperation.CREATE, deployment);
            k8sOptList.add(deployOpt);
            boolean kernal = false;
            int timeout = 0;
            for(K8sCollection collection : collections) {
                if(!addList.stream().collect(Collectors.toMap(AppUpdateOperationInfo::getAlias, Function.identity())).containsKey(collection.getAlias()))
                    throw new ParamException(String.format("%s not in %s ADD list", collection.getAlias(), domainId));
                AppUpdateOperationInfo optInfo = addList.stream().collect(Collectors.toMap(AppUpdateOperationInfo::getAlias, Function.identity())).get(collection.getAlias());
                AppModuleVo module = registerAppMap.get(optInfo.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(optInfo.getVersion());
                if(module.isKernal())
                {
                    kernal = true;
                    if(module.getTimeout() > timeout)
                        timeout = module.getTimeout();
                }
//                generateParamForCollection(jobId, platformId, domainId, optInfo, domainCfg, platformCfg, collection);
//                deployment.getMetadata().getLabels().put(String.format("%s-alias", optInfo.getAppName()), optInfo.getAlias());
//                deployment.getMetadata().getLabels().put(String.format("%s-version", optInfo.getAppName()), optInfo.getVersion().replaceAll("\\:", "-"));
                for(V1Service service : collection.getServices())
                {
                    if(!service.getMetadata().getNamespace().equals(platformId))
                        throw new ParamException(String.format("error namespace of add service %s:wanted %s,actual %s",
                                service.getMetadata().getName(), platformId, service.getMetadata().getNamespace()));
//                    service.getMetadata().setNamespace(platformId);
//                    Map<String, String> selector = new HashMap<>();
//                    selector.put(collection.getAppName(), collection.getAlias());
//                    selector.put(this.domainIdLabel, domainId);
//                    service.getSpec().setSelector(selector);
//                    if(service.getMetadata().getLabels() == null)
//                        service.getMetadata().setLabels(new HashMap<>());
//                    service.getMetadata().getLabels().put(this.domainIdLabel, domainId);
//                    service.getMetadata().getLabels().put(this.appNameLabel, collection.getAppName());
//                    service.getMetadata().getLabels().put(this.appAliasLabel, collection.getAlias());
//                    service.getMetadata().getLabels().put(this.appVersionLabel, collection.getVersion().replaceAll("\\:", "-"));
//                    service.getMetadata().getLabels().put(this.appTypeLabel, collection.getAppType().name);
//                    service.getMetadata().getLabels().put(this.jobIdLabel, jobId);
//                    if(service.getMetadata().getName().split("\\-").length > 2)
//                        service.getMetadata().getLabels().put(this.serviceTypeLabel, K8sServiceType.DOMAIN_OUT_SERVICE.name);
//                    else
//                        service.getMetadata().getLabels().put(this.serviceTypeLabel, K8sServiceType.DOMAIN_SERVICE.name);
                    K8sOperationInfo k8sOpt = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE,
                            service.getMetadata().getName(), K8sOperation.CREATE, service);
                    k8sOptList.add(k8sOpt);
                }
                for(ExtensionsV1beta1Ingress ingress : collection.getIngresses())
                {
                    if(!ingress.getMetadata().getNamespace().equals(platformId))
                        throw new ParamException(String.format("error namespace of add ingress %s:wanted %s,actual %s",
                                ingress.getMetadata().getName(), platformId, ingress.getMetadata().getNamespace()));
                    K8sOperationInfo k8sOpt = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.INGRESS,
                            ingress.getMetadata().getName(), K8sOperation.CREATE, ingress);
                    k8sOptList.add(k8sOpt);
                }
            }
            deployOpt.setKernal(kernal);
            deployOpt.setTimeout(timeout);
        }
        return k8sOptList;
    }

    private K8sOperationPo execK8sOpt(K8sOperationInfo optInfo, String platformId, String k8sApiUrl, String k8sAuthToken) throws ParamException
    {
        if(optInfo.getOperation().equals(K8sOperation.CREATE) && optInfo.getOperation().equals(K8sOperation.DELETE))
            throw new ParamException(String.format("current version not support %s %s %s",
                    optInfo.getOperation().name, optInfo.getKind().name, optInfo.getName()));
        Object retVal = null;
        K8sOperationPo execResult = new K8sOperationPo(optInfo.getJobId(), platformId, optInfo.getDomainId(), optInfo.getKind(),
                optInfo.getName(), optInfo.getOperation(), gson.toJson(optInfo.getObj()));
        try
        {
            switch (optInfo.getKind())
            {
                case NAMESPACE: {
                    switch (optInfo.getOperation())
                    {
                        case CREATE:
                            retVal = this.k8sApiService.createNamespace((V1Namespace)optInfo.getObj(), k8sApiUrl, k8sAuthToken);
                            break;
                        case DELETE:
                            this.k8sApiService.deleteNamespace(optInfo.getName(), k8sApiUrl, k8sAuthToken);
                            break;
                    }
                    break;
                }
                case SECRET:
                    switch (optInfo.getOperation())
                    {
                        case CREATE:
                            retVal = this.k8sApiService.createNamespacedSecret(platformId, (V1Secret) optInfo.getObj(), k8sApiUrl, k8sAuthToken);
                            break;
                        case DELETE:
                            this.k8sApiService.deleteNamespacedSecret(optInfo.getName(), platformId, k8sApiUrl, k8sAuthToken);
                            break;
                    }
                    break;
                case PV:
                    switch (optInfo.getOperation())
                    {
                        case CREATE:
                            retVal = this.k8sApiService.createPersistentVolume((V1PersistentVolume) optInfo.getObj(), k8sApiUrl, k8sAuthToken);
                            break;
                        case DELETE:
                            this.k8sApiService.deletePersistentVolume(optInfo.getName(), k8sApiUrl, k8sAuthToken);
                            break;
                    }
                    break;
                case PVC:
                    switch (optInfo.getOperation())
                    {
                        case CREATE:
                            retVal = this.k8sApiService.createNamespacedPersistentVolumeClaim(platformId, (V1PersistentVolumeClaim) optInfo.getObj(), k8sApiUrl, k8sAuthToken);
                            break;
                        case DELETE:
                            this.k8sApiService.deleteNamespacedPersistentVolumeClaim(optInfo.getName(), platformId, k8sApiUrl, k8sAuthToken);
                            break;
                    }
                    break;
                case CONFIGMAP:
                    switch (optInfo.getOperation())
                    {
                        case CREATE:
                            retVal = this.k8sApiService.createNamespacedConfigMap(platformId, (V1ConfigMap) optInfo.getObj(), k8sApiUrl, k8sAuthToken);
                            break;
                        case DELETE:
                            this.k8sApiService.deleteNamespacedConfigMap(optInfo.getName(), platformId, k8sApiUrl, k8sAuthToken);
                            break;
                    }
                    break;
                case DEPLOYMENT:
                    switch (optInfo.getOperation())
                    {
                        case CREATE:
                            retVal = this.k8sApiService.createNamespacedDeployment(platformId, (V1Deployment) optInfo.getObj(), k8sApiUrl, k8sAuthToken);
                            if(optInfo.getTimeout() > 0)
                            {
                                int timeUsage = 0;
                                while(timeUsage <= optInfo.getTimeout())
                                {
                                    Thread.sleep(3000);
                                    logger.debug(String.format("wait deployment %s status to ACTIVE, timeUsage=%d", optInfo.getName(), (timeUsage+3)));
                                    K8sStatus status = this.k8sApiService.readNamespacedDeploymentStatus(optInfo.getName(), platformId, k8sApiUrl, k8sAuthToken);
                                    if(status.equals(K8sStatus.ACTIVE))
                                    {
                                        logger.debug(String.format("deployment %s status change to ACTIVE, timeUsage=%d", optInfo.getName(), (timeUsage+3)));
                                        break;
                                    }
                                    timeUsage += 3;
                                }
                                if(timeUsage > optInfo.getTimeout()){
                                    logger.error(String.format("start deployment %s timeout in %d seconds", optInfo.getName(), timeUsage));
                                    if(optInfo.isKernal())
                                        throw new ParamException(String.format("start deployment %s timeout in %d seconds", optInfo.getName(), timeUsage));
                                }
                            }
                            break;
                        case DELETE:
                            this.k8sApiService.deleteNamespacedDeployment(optInfo.getName(), platformId, k8sApiUrl, k8sAuthToken);
                            break;
                    }
                    break;
                case SERVICE:
                    switch (optInfo.getOperation())
                    {
                        case CREATE:
                            retVal = this.k8sApiService.createNamespacedService(platformId, (V1Service) optInfo.getObj(), k8sApiUrl, k8sAuthToken);
                            break;
                        case DELETE:
                            this.k8sApiService.deleteNamespacedService(optInfo.getName(), platformId, k8sApiUrl, k8sAuthToken);
                            break;
                    }
                    break;
                case INGRESS:
                    switch (optInfo.getOperation())
                    {
                        case CREATE:
                            retVal = this.k8sApiService.createNamespacedIngress(platformId, (ExtensionsV1beta1Ingress) optInfo.getObj(), k8sApiUrl, k8sAuthToken);
                            break;
                        case DELETE:
                            this.k8sApiService.deleteNamespacedIngress(optInfo.getName(), platformId, k8sApiUrl, k8sAuthToken);
                            break;
                    }
                    break;
                case JOB:
                    switch (optInfo.getOperation())
                    {
                        case CREATE:
                            retVal = this.k8sApiService.createNamespacedJob(platformId, (V1Job) optInfo.getObj(), k8sApiUrl, k8sAuthToken);
                            Thread.sleep(20);
                            break;
                        case DELETE:
                            this.k8sApiService.deleteNamespacedIngress(optInfo.getName(), platformId, k8sApiUrl, k8sAuthToken);
                            break;
                    }
                    break;
                case ENDPOINTS:
                    switch (optInfo.getOperation())
                    {
                        case CREATE:
                            retVal = this.k8sApiService.createNamespacedEndpoints(platformId, (V1Endpoints) optInfo.getObj(), k8sApiUrl, k8sAuthToken);
                            break;
                        case DELETE:
                            this.k8sApiService.deleteNamespacedEndpoints(optInfo.getName(), platformId, k8sApiUrl, k8sAuthToken);
                            break;
                    }
                    break;
                default:
                    throw new ParamException(String.format("current version not support %s %s %s",
                            optInfo.getOperation().name, optInfo.getKind().name, optInfo.getName()));
            }
            String retJson = retVal != null ? gson.toJson(retVal) : null;
            execResult.success(retJson);
        }
        catch (Exception ex)
        {
            logger.error(String.format("exec %s exception", gson.toJson(optInfo)), ex);
            execResult.fail(ex.getMessage());
        }
        return execResult;
    }

    private PlatformSchemaExecResultVo execPlatformUpdateSteps(PlatformPo platformPo, List<K8sOperationInfo> k8sOptList, PlatformUpdateSchemaInfo schema, List<PlatformAppDeployDetailVo> platformApps, PlatformAppDeployDetailVo deployGls) throws ParamException
    {
        String jobId = schema.getSchemaId();
        Date startTime = new Date();
        String platformId = platformPo.getPlatformId();
        List<PlatformUpdateRecordPo> lastRecords = this.platformUpdateRecordMapper.select(platformId, true);
        PlatformSchemaExecResultVo execResultVo = new PlatformSchemaExecResultVo(jobId, platformId, k8sOptList);
        List<K8sOperationPo> execResults = execK8sDeploySteps(platformPo, k8sOptList, deployGls);
        boolean execSucc = execResults.get(execResults.size() - 1).isSuccess();
        logger.info(String.format("%s schema with jobId=%s execute : %b", platformId, jobId, execSucc));
        if(execSucc)
            execResultVo.execResult(execResults);
        else
            execResultVo.execFail(execResults, execResults.get(execResults.size() - 1).getComment());
        for(K8sOperationPo stepResult : execResultVo.getExecResults())
            this.k8sOperationMapper.insert(stepResult);
        String lastJobId = lastRecords.size() == 1 && execSucc ? lastRecords.get(0).getJobId() : null;
        PlatformUpdateRecordPo recordPo = new PlatformUpdateRecordPo();
        recordPo.setResult(execSucc);
        recordPo.setLast(execSucc);
        recordPo.setJobId(schema.getSchemaId());
        recordPo.setExecSchema(gson.toJson(schema).getBytes());
        if(!execSucc)
            recordPo.setComment(execResultVo.getErrorMsg());
        recordPo.setPreUpdateJobId(lastJobId);
        recordPo.setPlatformId(platformId);
        recordPo.setJobId(schema.getSchemaId());
        recordPo.setPreDeployApps(gson.toJson(platformApps).getBytes());
        recordPo.setUpdateTime(startTime);
        recordPo.setTimeUsage((int)((new Date()).getTime() - startTime.getTime())/1000);
        logger.debug(String.format("platform deploy record=%s", gson.toJson(recordPo)));
        this.platformUpdateRecordMapper.insert(recordPo);
        for(PlatformUpdateRecordPo po : lastRecords) {
            po.setLast(false);
            this.platformUpdateRecordMapper.update(po);
        }
        return execResultVo;
    }

    private List<K8sOperationPo> execK8sDeploySteps(PlatformPo platform, List<K8sOperationInfo> k8sOptList, PlatformAppDeployDetailVo deployGls) throws ParamException
    {
        String platformId = platform.getPlatformId();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        int oraclePort = 0;
        List<K8sOperationPo> execResults = new ArrayList<>();
        Map<String, Object> params = platform.getParams();
        for(K8sOperationInfo k8sOpt : k8sOptList)
        {
            K8sOperationPo ret = execK8sOpt(k8sOpt, platformId, k8sApiUrl, k8sAuthToken);
            if(!ret.isSuccess())
            {
                logger.error(String.format("platform update schema exec fail : %s", ret.getComment()));
                execResults.add(ret);
                return execResults;
            }
            try
            {
                if(k8sOpt.getKind().equals(K8sKind.SERVICE) && k8sOpt.getOperation().equals(K8sOperation.CREATE))
                {
                    V1Service service = gson.fromJson(ret.getRetJson(), V1Service.class);
                    Map<String, String> labels = service.getMetadata().getLabels();
                    if(labels == null || labels.size() == 0 || !labels.containsKey(this.serviceTypeLabel) || !labels.containsKey(this.appNameLabel))
                        continue;
                    if(labels.get(this.serviceTypeLabel).equals(K8sServiceType.THREE_PART_APP.name)
                            && labels.get(this.appNameLabel).equals("oracle"))
                    {
                        V1Service oraSvc = this.k8sApiService.readNamespacedService(service.getMetadata().getName(), platformId, k8sApiUrl, k8sAuthToken);
                        oraclePort = getNodePortFromK8sService(oraSvc);
                        boolean isConn = oracleConnectTest((String)params.get(PlatformBase.glsDBUserKey), (String)params.get(PlatformBase.glsDBPwdKey), (String)params.get(PlatformBase.k8sHostIpKey), oraclePort, (String)params.get(PlatformBase.glsDBSidKey), 240);
                        if(!isConn)
                            throw new ApiException("create service for oracle fail");
                        params.put(PlatformBase.dbPortKey, oraclePort);
                    }
                    else if(labels.get(this.serviceTypeLabel).equals(K8sServiceType.DOMAIN_OUT_SERVICE.name) && labels.get(this.appNameLabel).equals("UCDServer"))
                    {
                        Connection connect = createOracleConnection((String)params.get(PlatformBase.glsDBUserKey),
                                (String)params.get(PlatformBase.glsDBPwdKey), (String)params.get(PlatformBase.k8sHostIpKey),
                                (int)params.get(PlatformBase.dbPortKey), (String)params.get(PlatformBase.glsDBSidKey));
                        V1Service ucdsOutService = this.k8sApiService.readNamespacedService(service.getMetadata().getName(), platformId, k8sApiUrl, k8sAuthToken);
                        int ucdsPort = getNodePortFromK8sService(ucdsOutService);
                        String updateSql = String.format("update \"CCOD\".\"GLS_SERVICE_UNIT\" set PARAM_UCDS_PORT=%d where NAME='ucds-cloud01'", ucdsPort);
                        PreparedStatement ps = connect.prepareStatement(updateSql);
                        logger.debug(String.format("begin to update ucds port : %s", updateSql));
                        ps.executeUpdate();
                        Map<String, String> selector = new HashMap<>();
                        selector.put(this.appTypeLabel, AppType.BINARY_FILE.name);
                        selector.put("glsServer", deployGls.getAlias());
                        List<V1Deployment> deploys = this.k8sApiService.selectNamespacedDeployment(platformId, selector, k8sApiUrl, k8sAuthToken);
                        if(deploys.size() == 0)
                            throw new ParamException(String.format("can not find glsserver from %s", platformId));
                        else if(deploys.size() > 1)
                            throw new ParamException(String.format("glsserver multi deploy at %s ", platformId));
                        V1Deployment glsserver = deploys.get(0);
                        glsserver = templateParseGson.fromJson(templateParseGson.toJson(glsserver), V1Deployment.class);
                        Date now = new Date();
                        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                        glsserver.getMetadata().getLabels().put("restart-time", sf.format(now));
                        this.k8sApiService.replaceNamespacedDeployment(glsserver.getMetadata().getName(), platformId, glsserver, k8sApiUrl, k8sAuthToken);
                        int timeUsage = 0;
                        String glsserverName = glsserver.getMetadata().getName();
                        while(timeUsage <= 30)
                        {
                            Thread.sleep(3000);
                            logger.debug(String.format("wait deployment %s status to ACTIVE, timeUsage=%d", glsserverName, (timeUsage+3)));
                            K8sStatus status = this.k8sApiService.readNamespacedDeploymentStatus(glsserverName, platformId, k8sApiUrl, k8sAuthToken);
                            if(status.equals(K8sStatus.ACTIVE))
                            {
                                logger.debug(String.format("deployment %s status change to ACTIVE, timeUsage=%d", glsserverName, (timeUsage+3)));
                                break;
                            }
                            timeUsage += 3;
                        }
                        if(timeUsage > 30)
                            throw new ParamException(String.format("restart deployment %s timeout in %d seconds", glsserverName, timeUsage));
                    }
                }
            }
            catch (Exception ex)
            {
                logger.error(String.format("exec %s fail", gson.toJson(k8sOpt)), ex);
                ret.fail(ex.getMessage());
                execResults.add(ret);
            }
            execResults.add(ret);
        }
        return execResults;
    }

    @Override
    public PlatformUpdateRecordVo rollbackPlatform(String platformId, List<String> domainIds) throws ParamException, ApiException {
        logger.debug(String.format("rollback domain %s of %s to previous status", platformId, String.join(",", domainIds)));
        PlatformPo platform = getK8sPlatform(platformId);
        List<V1Service> existServices = this.k8sApiService.listNamespacedService(platformId, platform.getK8sApiUrl(), platform.getK8sAuthToken());
        List<PlatformUpdateRecordPo> lastRecords = this.platformUpdateRecordMapper.select(platformId, true);
        if(lastRecords.size() == 0)
            throw new ParamException(String.format("platform %s can not find any record to rollback", platformId));
        else if(lastRecords.size() > 1)
            throw new ParamException(String.format("platform %s has %d record with is_last=true, so dont know to to rollback",
                    platformId, lastRecords.size()));
        PlatformUpdateRecordVo lastRecord = new PlatformUpdateRecordVo(lastRecords.get(0), gson);
        String preJobId = lastRecord.getJobId();
        if(StringUtils.isBlank(preJobId))
            throw new ParamException(String.format("pre_update_job_id of last update record, so %s can not rollback", platformId));
        Map<UpdateStatus, List<DomainUpdatePlanInfo>> statusPlanMap = lastRecord.getExecSchema().getDomainUpdatePlanList().stream()
                .collect(Collectors.groupingBy(DomainUpdatePlanInfo::getStatus));
        if(!statusPlanMap.containsKey(UpdateStatus.EXEC))
            throw new ParamException(String.format("last update of %s do not EXEC any domain plan", platformId));
        Set<String> execDomainIds = statusPlanMap.get(UpdateStatus.EXEC).stream().collect(Collectors.toMap(DomainUpdatePlanInfo::getDomainId, Function.identity())).keySet();
        Map<DomainUpdateType, List<DomainUpdatePlanInfo>> typePlanMap = lastRecord.getExecSchema().getDomainUpdatePlanList().stream()
                .collect(Collectors.groupingBy(DomainUpdatePlanInfo::getUpdateType));
        if(!typePlanMap.containsKey(DomainUpdateType.UPDATE))
            throw new ParamException(String.format("last update of %s has not any domain UPDATE plan", platformId));
        Set<String> updateDomainIds = typePlanMap.get(DomainUpdateType.UPDATE).stream().collect(Collectors.toMap(DomainUpdatePlanInfo::getDomainId, Function.identity())).keySet();
        Map<String, DomainPo> domainMap = this.domainMapper.select(platformId, null).stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        for(String domainId : domainIds)
        {
            if(!domainMap.containsKey(domainId))
                throw new ParamException(String.format("%s is not domain of platform %s", domainId, platformId));
            else if(!execDomainIds.contains(domainId))
                throw new ParamException(String.format("domain %s of %s do not EXEC update plan at last update record", domainId, platformId));
            else if(!updateDomainIds.contains(domainId))
                throw new ParamException(String.format("domain %s of %s has not UPDATE plan at last update record", domainId, platformId));
        }
        List<K8sOperationInfo> optList = new ArrayList<>();
        Map<String, Integer> setMap = new HashMap<>();
        for(int i = 0; i < this.ccodBiz.getSet().size(); i++)
            setMap.put(this.ccodBiz.getSet().get(i).getFixedDomainId(), i);
        Comparator<String> sort = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                String id1 = o1.replaceAll("\\d*$", "");
                String id2 = o2.replaceAll("\\d*$", "");
                if(!id1.equals(id2))
                    return setMap.get(id1) - setMap.get(id2);
                return Integer.parseInt(o1.replaceAll(id2, "")) - Integer.parseInt(o2.replaceAll(id1, "")) ;
            }
        };
        Collections.sort(domainIds, sort);
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        String jobId = DigestUtils.md5DigestAsHex(sf.format(new Date()).getBytes()).substring(0, 10);
        for(String domainId : domainIds)
        {
            List<K8sOperationPo> domainUpdateSteps = this.k8sOperationMapper.select(lastRecord.getJobId(), platformId, domainId);
            List<K8sOperationInfo> domainRollbackOperations = generateDomainRollBackSteps(domainUpdateSteps, jobId);
            optList.addAll(domainRollbackOperations);
        }
        Map<K8sKind, List<K8sOperationInfo>> operationMap = optList.stream().collect(Collectors.groupingBy(K8sOperationInfo::getKind));
        List<K8sOperationInfo> serviceList = operationMap.containsKey(K8sKind.SERVICE) ? operationMap.get(K8sKind.SERVICE) : new ArrayList<>();
        reuseServiceNodePort(serviceList, existServices);
        List<PlatformAppDeployDetailVo> platformApps = this.platformAppDeployDetailMapper.selectPlatformApps(platformId, null, null);
        List<K8sOperationPo> rollbackResults = new ArrayList<>();
        String title = String.format("rollback platform %s to status before %s", platformId, sf.format(lastRecord.getUpdateTime()));
        String comment = String.format("rollback domain %s of platform %s(%s, updateJobId=%s) to status before %s(updateJobId=%s)",
                String.join(",", domainIds), platform.getPlatformName(), platformId, lastRecord.getJobId(),
                sf.format(lastRecord.getUpdateTime()), lastRecord.getPreUpdateJobId());
        PlatformUpdateSchemaInfo schemaInfo = new PlatformUpdateSchemaInfo(platform, PlatformUpdateTaskType.ROLLBACK, UpdateStatus.EXEC, title, comment);
        List<DomainUpdatePlanInfo> planList = getK8sPlatformRollBackInfo(platform, lastRecord.getExecSchema(), lastRecord.getPreDeployApps(), domainIds);
        schemaInfo.setDomainUpdatePlanList(planList);
        PlatformUpdateRecordPo updateRecord = new PlatformUpdateRecordPo();
        updateRecord.setJobId(jobId);
        updateRecord.setPreUpdateJobId(lastRecord.getJobId());
        updateRecord.setLast(false);
        updateRecord.setPreDeployApps(gson.toJson(platformApps).getBytes());
        updateRecord.setUpdateTime(new Date());
        updateRecord.setPlatformId(platformId);
        updateRecord.setExecSchema(gson.toJson(schemaInfo).getBytes());
        logger.info(String.format("rollback step is : %s", gson.toJson(optList)));
        boolean isSuccess = true;
        for(K8sOperationInfo optInfo : optList)
        {
            K8sOperationPo execResult = execK8sOpt(optInfo, platformId, platform.getK8sApiUrl(), platform.getK8sAuthToken());
            rollbackResults.add(execResult);
            if(!execResult.isSuccess()) {
                logger.error(String.format("rollback step exec fail : %s", gson.toJson(execResult)));
                isSuccess = false;
                updateRecord.setResult(false);
                updateRecord.setComment(execResult.getComment());
                break;
            }
        }
        lastRecords.get(0).setLast(false);
        logger.debug(String.format("update is_last of job_id=%s from true to false", lastRecord.getJobId()));
        this.platformUpdateRecordMapper.update(lastRecords.get(0));
        if(isSuccess) {
            logger.info(String.format("rollback domain %s of platform %s SUCCESS", String.join(",", domainIds), platformId));
            updateRecord.setResult(true);
        }
        logger.debug(String.format("insert rollback steps to db"));
        for(K8sOperationPo execResult : rollbackResults)
            this.k8sOperationMapper.insert(execResult);
        logger.debug(String.format("insert new platform update record(updateJobId=%s) to db", jobId));
        this.platformUpdateRecordMapper.insert(updateRecord);
        return new PlatformUpdateRecordVo(updateRecord, gson);
    }

    private List<K8sOperationInfo> generateDomainRollBackSteps(List<K8sOperationPo> updateSteps, String jobId)
    {
        List<K8sOperationInfo> optList = new ArrayList<>();
        for(int i = updateSteps.size() - 1; i >= 0; i--)
        {
            K8sOperationPo step = updateSteps.get(i);
            Object obj = null;
            switch (step.getKind())
            {
                case INGRESS:
                    obj = this.templateParseGson.fromJson(step.getJson(), ExtensionsV1beta1Ingress.class);
                    break;
                case SERVICE:
                    obj = this.templateParseGson.fromJson(step.getJson(), V1Service.class);
                    break;
                case DEPLOYMENT:
                    obj = this.templateParseGson.fromJson(step.getJson(), V1Deployment.class);
                    break;
                case CONFIGMAP:
                    obj = this.templateParseGson.fromJson(step.getJson(), V1ConfigMap.class);
                    break;
                case PV:
                    obj = this.templateParseGson.fromJson(step.getJson(), V1PersistentVolume.class);
                    break;
                case NAMESPACE:
                    obj = this.templateParseGson.fromJson(step.getJson(), V1Namespace.class);
                    break;
                case PVC:
                    obj = this.templateParseGson.fromJson(step.getJson(), V1PersistentVolumeClaim.class);
                    break;
                case SECRET:
                    obj = this.templateParseGson.fromJson(step.getJson(), V1Secret.class);
                    break;
                case JOB:
                    obj = this.templateParseGson.fromJson(step.getJson(), V1Job.class);
                    break;
                case POD:
                    obj = this.templateParseGson.fromJson(step.getJson(), V1Pod.class);
                    break;
            }
            K8sOperation operation = null;
            switch (step.getOperation())
            {
                case CREATE:
                    operation = K8sOperation.DELETE;
                    break;
                case REPLACE:
                    operation = K8sOperation.REPLACE;
                    break;
                case DELETE:
                    operation = K8sOperation.CREATE;
                    break;
            }
            K8sOperationInfo info = new K8sOperationInfo(jobId, step.getPlatformId(), step.getDomainId(), step.getKind(), step.getName(), operation, obj);
            optList.add(info);
        }
        Map<K8sKind, List<K8sOperationInfo>> operationMap = optList.stream().collect(Collectors.groupingBy(K8sOperationInfo::getKind));
        List<K8sOperationInfo> serviceList = operationMap.containsKey(K8sKind.SERVICE) ? operationMap.get(K8sKind.SERVICE) : new ArrayList<>();
        Map<String, List<K8sOperationInfo>> nameMap = serviceList.stream().collect(Collectors.groupingBy(K8sOperationInfo::getName));
        for(Map.Entry<String, List<K8sOperationInfo>> entry : nameMap.entrySet())
        {
            List<K8sOperationInfo> svcs = entry.getValue();
            if(svcs.size() == 2)
            {
                V1Service deleted = (V1Service) svcs.get(0).getObj();
                List<V1ServicePort> srcPorts = deleted.getSpec().getPorts();
                V1Service added = (V1Service)svcs.get(1).getObj();
                Map<String, V1ServicePort> dstPortMap = added.getSpec().getPorts().stream().collect(Collectors.toMap(V1ServicePort::getName, Function.identity()));
                for(V1ServicePort srcPort : srcPorts)
                {
                    if(srcPort.getNodePort() != null && dstPortMap.containsKey(srcPort.getName()))
                        dstPortMap.get(srcPort.getName()).setNodePort(srcPort.getNodePort());
                }
            }
        }
        return optList;
    }

    @Override
    public List<DomainUpdatePlanInfo> queryPlatformRollbackInfo(String platformId) throws ParamException {
        PlatformPo platform = this.getK8sPlatform(platformId);
        List<PlatformUpdateRecordPo> lastRecords = this.platformUpdateRecordMapper.select(platformId, true);
        if(lastRecords.size() == 0)
            throw new ParamException(String.format("can not find last update info of %s", platformId));
        else if(lastRecords.size() > 1)
            throw new ParamException(String.format("find %d update record with is_last=true of %s", lastRecords.size(), platformId));
        PlatformUpdateRecordPo lastRecord = lastRecords.get(0);
        PlatformUpdateSchemaInfo schema = gson.fromJson(new String(lastRecord.getExecSchema()), PlatformUpdateSchemaInfo.class);
        List<PlatformAppDeployDetailVo> preDeployApps = gson.fromJson(new String(lastRecord.getPreDeployApps()), new TypeToken<List<PlatformAppDeployDetailVo>>() {}.getType());
        Map<UpdateStatus, List<DomainUpdatePlanInfo>> planStatusMap = schema.getDomainUpdatePlanList().stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getStatus));
        if(!planStatusMap.containsKey(UpdateStatus.EXEC))
            throw new ParamException(String.format("platform %s with jobId=%s has not any EXEC domain plan", platformId, lastRecord.getJobId()));
        Map<DomainUpdateType, List<DomainUpdatePlanInfo>> planTypeMap = planStatusMap.get(UpdateStatus.EXEC).stream()
                .collect(Collectors.groupingBy(DomainUpdatePlanInfo::getUpdateType));
        if(!planTypeMap.containsKey(DomainUpdateType.UPDATE))
            throw new ParamException(String.format("platform %s with jobId=%s has not any domain UPDATE plan", platformId, lastRecord.getJobId()));
        Set<String> domainIds = planTypeMap.get(DomainUpdateType.UPDATE).stream().collect(Collectors.toMap(DomainUpdatePlanInfo::getDomainId, Function.identity()))
                .keySet();
        List<DomainUpdatePlanInfo> planList = getK8sPlatformRollBackInfo(platform, schema, preDeployApps, new ArrayList<>(domainIds));
        return planList;
    }

    private List<DomainUpdatePlanInfo> getK8sPlatformRollBackInfo(PlatformPo platform, PlatformUpdateSchemaInfo updateSchema, List<PlatformAppDeployDetailVo> preDeployApps, List<String> domainIds) throws ParamException
    {
        String platformId = platform.getPlatformId();
        List<DomainUpdatePlanInfo> planList = new ArrayList<>();
        Map<String, List<PlatformAppDeployDetailVo>> srcDomainAppMap = preDeployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        Map<String, DomainUpdatePlanInfo> planMap = updateSchema.getDomainUpdatePlanList().stream().collect(Collectors.toMap(DomainUpdatePlanInfo::getDomainId, Function.identity()));
        for(String domainId : domainIds)
        {
            if(!planMap.containsKey(domainId))
                throw new ParamException(String.format("%s do not do any change at last update", domainId));
            if(!planMap.get(domainId).getUpdateType().equals(DomainUpdateType.UPDATE))
                throw new ParamException(String.format("rollback only support update domain, not support %s %s",
                        planMap.get(domainId).getUpdateType().name, domainId));
            if(!srcDomainAppMap.containsKey(domainId))
                throw new ParamException(String.format("can not find original app deploy detail of %s", domainId));
            DomainUpdatePlanInfo rollBackPlan = getDomainRollbackPlan(domainId, planMap.get(domainId).getAppUpdateOperationList(), srcDomainAppMap.get(domainId));
            planList.add(rollBackPlan);
        }
        return planList;
    }

    private DomainUpdatePlanInfo getDomainRollbackPlan(String domainId, List<AppUpdateOperationInfo> updateList, List<PlatformAppDeployDetailVo> deployApps) throws ParamException
    {
        List<AppUpdateOperationInfo> rolls = new ArrayList<>();
        Map<String, PlatformAppDeployDetailVo> deployMap = deployApps.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAlias, Function.identity()));
        for(AppUpdateOperationInfo src : updateList)
        {
            AppUpdateOperationInfo roll = new AppUpdateOperationInfo();
            roll.setAppName(src.getAppName());
            roll.setAlias(src.getAlias());
            roll.setOriginalAlias(src.getOriginalAlias());
            switch (src.getOperation())
            {
                case ADD:
                    roll.setOperation(AppUpdateOperation.DELETE);
                    break;
                case UPDATE:
                case DELETE:
                    if(!deployMap.containsKey(src.getAlias()))
                        throw new ParamException(String.format("%s has been %s at domain %s, but can not find src deploy detail", src.getAlias(), domainId));
                    PlatformAppDeployDetailVo srcDetail = deployMap.get(src.getAlias());
                    if(src.getOperation().equals(AppUpdateOperation.UPDATE)) {
                        roll.setOperation(AppUpdateOperation.UPDATE);
                        roll.setVersion(srcDetail.getVersion());
                        roll.setOriginalVersion(src.getVersion());
                    }
                    else {
                        roll.setOperation(AppUpdateOperation.ADD);
                        roll.setVersion(srcDetail.getVersion());
                        roll.setOriginalVersion(null);
                    }
                    roll.setDeployPath(srcDetail.getDeployPath());
                    roll.setStartCmd(srcDetail.getStartCmd());
                    roll.setBasePath(srcDetail.getBasePath());
                    roll.setAssembleTag(srcDetail.getAssembleTag());
                    roll.setDomainId(domainId);
                    roll.setHostIp(srcDetail.getHostIp());
                    roll.setAppRunner(srcDetail.getAlias());
                    roll.setCfgs(src.getCfgs());
                    roll.setDomainName(srcDetail.getDomainName());
                    roll.setPorts(srcDetail.getPorts());
            }
            rolls.add(roll);
        }
        DomainUpdatePlanInfo plan = new DomainUpdatePlanInfo();
        plan.setDomainId(domainId);
        plan.setUpdateType(DomainUpdateType.UPDATE);
        plan.setAppUpdateOperationList(rolls);
        return plan;
    }

    @Override
    public List<PlatformUpdateRecordVo> queryPlatformUpdateRecords() {
        List<PlatformUpdateRecordVo> retList = new ArrayList<>();
        List<PlatformUpdateRecordPo> records = this.platformUpdateRecordMapper.select(null, null);
        for(PlatformUpdateRecordPo record : records)
            retList.add(new PlatformUpdateRecordVo(record, gson));
        return retList;
    }

    @Override
    public List<PlatformUpdateRecordVo> queryPlatformUpdateRecordByPlatformId(String platformId) throws ParamException {
        PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
        if(platformPo == null)
            throw new ParamException(String.format("platform %s not exist", platformId));
        List<PlatformUpdateRecordVo> retList = new ArrayList<>();
        List<PlatformUpdateRecordPo> records =  this.platformUpdateRecordMapper.select(platformId, null);
        for(PlatformUpdateRecordPo record : records)
            retList.add(new PlatformUpdateRecordVo(record, gson));
        return retList;
    }

    @Override
    public PlatformUpdateRecordVo queryPlatformUpdateRecordByJobId(String platformId, String jobId) throws ParamException {
        PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
        if(platformPo == null)
            throw new ParamException(String.format("platform %s not exist", platformId));
        PlatformUpdateRecordPo recordPo = this.platformUpdateRecordMapper.selectByJobId(jobId);
        if(recordPo == null)
            throw new ParamException(String.format("id=%s update record not exist", jobId));
        if(!recordPo.getPlatformId().equals(platformId))
            throw new ParamException(String.format("%s has not id=%s update record", platformId, jobId));
        return new PlatformUpdateRecordVo(recordPo, gson);
    }

    private Connection createOracleConnection(String user, String pwd, String ip, int port, String sid) throws ClassNotFoundException, SQLException {
        String connStr = String.format("jdbc:oracle:thin:@%s:%s:%s", ip, port, sid);
        Class.forName("oracle.jdbc.driver.OracleDriver");
        logger.debug(String.format("create %s oracle conn", connStr));
        Connection conn = DriverManager.getConnection(connStr, user, pwd);
        logger.debug(String.format("conn %s success", connStr));
        return conn;
    }

    private boolean oracleConnectTest(String user, String pwd, String ip, int port, String sid, int timeout) {
        int count = 1;
        while (count < (timeout / 10)) {
            System.out.println("");
            try {
                logger.debug(String.format("try connect oracle"));
                Connection conn = createOracleConnection(user, pwd, ip, port, sid);
                conn.close();
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
                System.out.println(String.format("fail to conn %d", count));
                count++;
                try {
                    Thread.sleep(10000);
                } catch (Exception ex1) {
                    ex1.printStackTrace();
                }
            }
        }
        logger.error("conn timeout");
        return false;
    }
}