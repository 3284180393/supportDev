package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.config.*;
import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.dao.*;
import com.channelsoft.ccod.support.cmdb.exception.*;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.service.*;
import com.channelsoft.ccod.support.cmdb.utils.FileUtils;
import com.channelsoft.ccod.support.cmdb.utils.ZipUtils;
import com.channelsoft.ccod.support.cmdb.vo.*;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import io.kubernetes.client.util.Yaml;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.joda.time.DateTime;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
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

    private final static Gson gson = new GsonBuilder().registerTypeAdapter(DateTime.class, new GsonDateUtil()).disableHtmlEscaping().create();

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
    }).registerTypeAdapter(DateTime.class, new GsonDateUtil()).disableHtmlEscaping().create();

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
    AppMapper appMapper;

    @Autowired
    K8sOperationMapper k8sOperationMapper;

    @Autowired
    PlatformUpdateRecordMapper platformUpdateRecordMapper;

    @Autowired
    AppDebugDetailMapper appDebugDetailMapper;

    @Autowired
    CCODThreePartAppMapper ccodThreePartAppMapper;

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

    @Value("${ccod.host-deploy-template}")
    private String platformDeployScriptFileName;

    @Value("${ccod.platform-deploy-template}")
    private String hostDeployScriptFileName;

    @Value("${ccod.platform-deploy-cfg}")
    private String platformDeployCfgFileName;

    @Value("${ccod.platform-deploy-mysql-cfg}")
    private String platformDeployMysqlCfgFileName;

    @Value("${ccod.host-script-deploy-cfg}")
    private String hostScriptDeployCfgFileName;

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

    @Value("${debug-timeout}")
    private int debugTimeout;

    private final Map<String, PlatformUpdateSchemaInfo> platformUpdateSchemaMap = new ConcurrentHashMap<>();

    private final List<AppDebugTaskVo> debugQueue = new ArrayList<>();

    private final Map<String, List<K8sOperationPo>> debugLogsMap = new ConcurrentHashMap<>();

    private final List<K8sOperationPo> platformDeployLogs = new ArrayList<>();

    private Map<String, List<BizSetDefine>> appSetRelationMap;

    private Map<String, BizSetDefine> setDefineMap;

    protected final ReentrantReadWriteLock appWriteLock = new ReentrantReadWriteLock();

    protected final ReentrantReadWriteLock debugLock = new ReentrantReadWriteLock();

    private boolean isPlatformCheckOngoing = false;

    private String ongoingPlatformId = null;

    private int debugTaskId = 1;

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
//            String json = "{\"apiVersion\":\"batch/v1\",\"kind\":\"Job\",\"metadata\":{\"labels\":{},\"name\":\"platform-base-init\",\"namespace\":\"test08\"},\"spec\":{\"selector\":{\"matchLabels\":{}},\"template\":{\"metadata\":{\"labels\":{}},\"spec\":{\"containers\":[{\"args\":[\"cd /root/data;rm -rf /root/data/volume/${PLATFORMID};mkdir /root/data/volume/${PLATFORMID} -p;cd /root/data/volume/${PLATFORMID};wget -q ${FILEURL};tar -xzf ${FILENAME}\"],\"command\":[\"/bin/bash\",\"-c\"],\"image\":\"nexus.io:5000/ccod-base/centos-tool:7.2.1511\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"base-init\",\"volumeMounts\":[{\"mountPath\":\"/root/data\",\"name\":\"base-volume\"}]}],\"restartPolicy\":\"Never\",\"terminationGracePeriodSeconds\":30,\"volumes\":[{\"name\":\"base-volume\",\"hostPath\":{\"path\":\"\"}}]}}}}";
//            System.out.println(json);
//            String baseDataPath = "initSql/3.9/initPlatformData-2020-09-22.gz";
//            V1Job job = this.k8sTemplateService.generatePlatformInitJob("3.9", "test", baseDataPath);
//            System.out.println(gson.toJson(job));
//            baseDataPath = "initSql/4.1/initPlatformData-2020-09-22.gz";
//            job = this.k8sTemplateService.generatePlatformInitJob("4.1", "test", baseDataPath);
//            System.out.println(gson.toJson(job));
//            Runtime runtime = Runtime.getRuntime();
//            String command = job.getSpec().getTemplate().getSpec().getContainers().get(0).getArgs().get(0);
//            logger.warn(String.format("begin to exec %s", command));
//            runtime.exec(command);
//            logger.warn("write msg to sysLog success");
//            updateK8sTemplate();
//            PlatformUpdateSchemaInfo schema = restoreExistK8sPlatform("pahjgs");
//            logger.error(gson.toJson(schema));
//            updatePlatformUpdateSchema(schema);
//            PlatformCreateParamVo paramVo = new PlatformCreateParamVo();
//            paramVo.setParams("pahjgs");
//            paramVo.setNfsServerIp("10.130.41.218");
//            paramVo.setK8sHostIp("10.130.41.218");
//            paramVo.setPlatformId("script-test");
//            paramVo.setHostUrl("script-test.ccod.com");
//            System.out.println(gson.toJson(paramVo));
//            String zipFilePath = generatePlatformCreateScript(paramVo);
//            logger.debug(String.format("generate script saved to %s", zipFilePath));
//            initThreePartAppDepend();
//            updateThreePartApp();

        } catch (Exception ex) {
            logger.error("write msg error", ex);
        }
    }

    private void debugDetailTest()
    {
        AppUpdateOperationInfo optInfo = new AppUpdateOperationInfo();
        optInfo.setAppName("ucxserver");
        optInfo.setAlias("ucx01");
        optInfo.setCfgs(gson.fromJson("[{\"fileName\":\"Config.ini\",\"ext\":\"\",\"md5\":\"10d27726780b713ffbff1fc90efeee56\",\"deployPath\":\"./bin/\",\"nexusRepository\":\"ccod_module_releases\",\"nexusPath\":\"LicenseServer/a508e3bf0ddd9e11a7edd758f814d2c98939e5a5/Config.ini\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVfcmVsZWFzZXM6MTNiMjllNDQ5ZjBlM2I4ZGU5OWQ3MTlkMTAyMDEwNTc\"}]", new TypeToken<List<AppFileNexusInfo>>() {}.getType()));
        optInfo.setAppType(AppType.BINARY_FILE);
        optInfo.setOperation(AppUpdateOperation.REGISTER);
        AppDebugDetailPo po = new AppDebugDetailPo();
        po.setAlias("ucxserver");
        po.setAppName("ucx01");
        po.setCreateTime(new Date());
        po.setUpdateTime(new Date());
        po.setDebugging(true);
        po.setDetail(optInfo);
        po.setDomainId("domain01");
        po.setPlatformId("test");
        po.setTryCount(1);
        appDebugDetailMapper.insert(po);
        List<AppDebugDetailPo> list = appDebugDetailMapper.select("test", "domain01", "ucxserver", "ucx01");
        System.out.println(gson.toJson(list));
        optInfo.setCfgs(gson.fromJson("[{\"fileName\":\"fps.cfg\",\"ext\":\"cfg\",\"md5\":\"33d01caf16d256ec16175b1fa24328ee\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"ccod_module_releases\",\"nexusPath\":\"fpsvr/27768:27779/fps.cfg\",\"nexusAssetId\":\"Y2NvZF9tb2R1bGVfcmVsZWFzZXM6ZDQ4MTE3NTQxZGNiODllYzI5MmFjNjYzNGFlZTgzMWU\"}]", new TypeToken<List<AppFileNexusInfo>>() {}.getType()));
        po.setDebugging(false);
        po.setUpdateTime(new Date());
        po.setTryCount(2);
        appDebugDetailMapper.update(po);
        list = appDebugDetailMapper.select("test", "domain01", "ucxserver", "ucx01");
        System.out.println(gson.toJson(list));
//        appDebugDetailMapper.delete("test", "domain01", "cmsserver", "cms01");
    }

//    private void initThreePartAppDepend()
//    {
//        List<CCODThreePartAppPo> list = new ArrayList<>();
//        CCODThreePartAppPo appPo = new CCODThreePartAppPo("3.9", null, "mysql", "mysql");
//        list.add(appPo);
//        appPo = new CCODThreePartAppPo("3.9", null, "oracle", "oracle");
//        list.add(appPo);
//        appPo = new CCODThreePartAppPo("4.1", null, "mysql", "mysql");
//        list.add(appPo);
//        appPo = new CCODThreePartAppPo("4.1", null, "oracle", "oracle");
//        list.add(appPo);
//        appPo = new CCODThreePartAppPo("4.8", null, "mysql", "mysql");
//        list.add(appPo);
//        appPo = new CCODThreePartAppPo("4.8", null, "wgw", "wgw");
//        list.add(appPo);
//        appPo = new CCODThreePartAppPo("4.8", null, "sgw", "sgw");
//        list.add(appPo);
////        list.forEach(a->ccodThreePartAppMapper.insert(a));
//        List<CCODThreePartAppPo> results = ccodThreePartAppMapper.select("3.9", "standard", null);
//        System.out.println(gson.toJson(results));
//        results = ccodThreePartAppMapper.select("4.1", "standard", null);
//        System.out.println(gson.toJson(results));
//        results = ccodThreePartAppMapper.select("4.8", "standard", null);
//        System.out.println(gson.toJson(results));
//    }

    private void updateThreePartApp(){
//        List<String> versions = Arrays.asList(new String[]{"3.9", "4.1", "4.8"});
//        List<String> names = Arrays.asList(new String[]{"umg41", "umg141", "umg147"});
//        List<String> ips = Arrays.asList(new String[]{"10.130.41.41", "10.130.41.141", "10.130.41.147"});
//        for(String ccodVersion : versions){
//            for(int i = 0; i <= 2; i++){
//                CCODThreePartAppPo appPo = new CCODThreePartAppPo();
//                appPo.setAlias(names.get(i));
//                appPo.setAppName("umg");
//                appPo.setCcodVersion(ccodVersion);
//                appPo.setParams(new HashMap<>());
//                appPo.setTimeout(20);
//                Map<String, String> cfgs = new HashMap<>();
//                cfgs.put("ip", ips.get(i));
//                cfgs.put("protocol", "TCP");
//                appPo.setCfgs(cfgs);
//                appPo.setKind(K8sKind.ENDPOINTS);
//                appPo.setTag("standard");
//                ccodThreePartAppMapper.insert(appPo);
//            }
//        }
//        List<CCODThreePartAppPo> list = ccodThreePartAppMapper.select(null, null, null);

    }

    private void updateK8sTemplate() throws Exception{
        List<K8sObjectTemplatePo> templateList = k8sTemplateService.getK8sTemplates();
//        PlatformPo platform = getK8sPlatform("jhkgs");
//        List<K8sOperationInfo> steps = k8sTemplateService.generatePlatformCreateSteps("1234455", null, null, null, null, null, null, null, null, platform);
//        {
//            V1Deployment deployment = (V1Deployment) steps.stream().filter(o->o.getKind().equals(K8sKind.DEPLOYMENT) && o.getName().equals("oracle"))
//                    .collect(Collectors.toList()).get(0).getObj();
//            V1PersistentVolumeClaimVolumeSource src = deployment.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(a->a.getName(), v->v)).get("sql").getPersistentVolumeClaim();
//            V1Volume volume = new V1Volume();
//            volume.setPersistentVolumeClaim(src);
//            volume.setName("data");
//            deployment.getSpec().getTemplate().getSpec().getVolumes().clear();
//            deployment.getSpec().getTemplate().getSpec().getVolumes().add(volume);
//            V1VolumeMount mount = new V1VolumeMount();
//            mount.setName("data");
//            mount.setMountPath("/home/oracle/oracle10g/oradata");
//            mount.setSubPath("base-volume/db/oracle/data");
//            deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts().add(mount);
////            mount = new V1VolumeMount();
////            mount.setName("data");
////            mount.setMountPath("/usr/lib/oracle/xe/oradata/XE");
////            mount.setSubPath("base-volume/db/oracle/data");
////            deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts().add(mount);
//            deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts().get(0).setSubPath("base-volume/db/oracle/sql");
//            deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts()
//                    .forEach(m->m.setName("data"));
////                System.out.println(gson.toJson(deployment));
//            templateList.stream().filter(t-> t.getLabels().containsKey("app-name") && t.getLabels().get("app-name").equals("oracle")).forEach(t->{
//                t.setDeployJson(gson.toJson(deployment));
//            });
//        }
//        {
//            V1Deployment deployment = (V1Deployment) steps.stream().filter(o->o.getKind().equals(K8sKind.DEPLOYMENT) && o.getName().equals("mysql"))
//                    .collect(Collectors.toList()).get(0).getObj();
//            deployment.getSpec().getTemplate().getSpec().getVolumes().forEach(v->v.setName("data"));
//            deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts().stream().forEach(v->v.setName("data"));
//            deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts().get(0).setSubPath("base-volume/db/mysql/sql");
//            deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts().get(1).setSubPath("base-volume/db/mysql/data");
//            templateList.stream().filter(t-> t.getLabels().containsKey("app-name") && t.getLabels().get("app-name").equals("mysql")).forEach(t->{
//                t.setDeployJson(gson.toJson(deployment));
//            });
//
//        }
//        List<K8sObjectTemplatePo> newT = new ArrayList<>();
//        templateList.stream().filter(t->t.getLabels().containsKey("ccod-version") && t.getLabels().get("ccod-version").equals("4.1")).forEach(s->{
//            K8sObjectTemplatePo t = s.clone();
//            t.getLabels().put("ccod-version", "4.8");
//            newT.add(t);
//        });
//        templateList.addAll(newT);
//        templateList.stream().filter(t->t.getLabels().containsKey(this.appTypeLabel) && (t.getLabels().get(this.appTypeLabel).equals(AppType.RESIN_WEB_APP.name)) && t.getDeployJson() != null && t.getLabels().get(ccodVersionLabel).equals("4.8")).forEach(t->{
//                    V1Deployment d = gson.fromJson(t.getDeployJson(), V1Deployment.class);
//                    d.getSpec().getTemplate().getSpec().getContainers().get(0).setImage("nexus.io:5000/ccod-base/resin-jdk:resin-4.0.13_jdk-1.8.0_10");
//                    t.setDeployJson(gson.toJson(d));
//        });
//        templateList.stream().filter(t->t.getLabels().containsKey(this.appTypeLabel) && (t.getLabels().get(this.appTypeLabel).equals(AppType.TOMCAT_WEB_APP.name)) && t.getDeployJson() != null && t.getLabels().get(ccodVersionLabel).equals("4.8")).forEach(t->{
//            V1Deployment d = gson.fromJson(t.getDeployJson(), V1Deployment.class);
//            d.getSpec().getTemplate().getSpec().getContainers().get(0).setImage("nexus.io:5000/ccod-base/tomcat:6.0.53-jre8");
//            t.setDeployJson(gson.toJson(d));
//        });
//        templateList.stream().filter(t->t.getLabels().containsKey(appNameLabel) && t.getLabels().get(appNameLabel).equals("mysql") && t.getLabels().get(ccodVersionLabel).equals("4.8")).forEach(t->{
//            V1Deployment d = gson.fromJson(t.getDeployJson(), V1Deployment.class);
//            d.getSpec().getTemplate().getSpec().getContainers().get(0).getArgs().set(3, "--lower-case-table-names=0");
//            t.setDeployJson(gson.toJson(d));
//        });
//        K8sObjectTemplatePo po = gson.fromJson(gson.toJson(templateList.get(15)), K8sObjectTemplatePo.class);
//        po.getLabels().put(appTypeLabel, AppType.JAR.name);
//        po.setServiceJson(templateList.get(17).getServiceJson());
//        po.setIngressJson(templateList.get(17).getIngressJson());
//        V1Deployment deployment = (V1Deployment)gson.fromJson(po.getDeployJson(), V1Deployment.class);
//        deployment.getMetadata().getLabels().put(appTypeLabel, AppType.JAR.name);
//        po.setDeployJson(gson.toJson(deployment));
//        templateList.add(po);
//        K8sObjectTemplatePo po = gson.fromJson(gson.toJson(templateList.get(17)), K8sObjectTemplatePo.class);
//        po.getLabels().put(appNameLabel, "gls");
//        V1Deployment deployment = gson.fromJson(po.getDeployJson(), V1Deployment.class);
//        deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setImage("ccod-base/resin-jdk:resin-4.0.13_jdk-1.7.0_10-0");
//        po.setDeployJson(gson.toJson(deployment));
//        templateList.add(18, po);
//        K8sObjectTemplatePo po = gson.fromJson(gson.toJson(templateList.get(13)), K8sObjectTemplatePo.class);
//        String json= "{\"apiVersion\":\"apps/v1\",\"kind\":\"Deployment\",\"metadata\":{\"name\":\"freeswitch-wgw\",\"namespace\":\"test48\",\"labels\":{\"name\":\"freeswitch-wgw\"}},\"spec\":{\"strategy\":{\"type\":\"Recreate\"},\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"name\":\"freeswitch-wgw\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"freeswitch-wgw\"}},\"spec\":{\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"record\",\"hostPath\":{\"path\":\"/home/kubernetes/volume/test48/record/\"}}],\"containers\":[{\"name\":\"freeswitch-wgw\",\"image\":\"ccod/freeswitch:1.10.2-qn-002\",\"hostNetwork\":true,\"volumeMounts\":[{\"mountPath\":\"/record\",\"name\":\"record\"}],\"workingDir\":\"/root/Platform\",\"command\":[\"/bin/sh\",\"-c\"],\"args\":[\"/usr/local/freeswitch/bin/start.sh WGW;\"]}]}}}}";
//        V1Deployment deployment = gson.fromJson(json, V1Deployment.class);
//        po.getLabels().put(appNameLabel, "wgw");
//        po.setDeployJson(gson.toJson(deployment));
//        po.setServiceJson(null);
//        templateList.add(po);
//        json = "{\"apiVersion\":\"apps/v1\",\"kind\":\"Deployment\",\"metadata\":{\"name\":\"freeswitch-sgw\",\"namespace\":\"test48\",\"labels\":{\"name\":\"freeswitch-sgw\"}},\"spec\":{\"strategy\":{\"type\":\"Recreate\"},\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"name\":\"freeswitch-sgw\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"freeswitch-sgw\"}},\"spec\":{\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"record\",\"hostPath\":{\"path\":\"/home/kubernetes/volume/test48/record/\"}}],\"containers\":[{\"name\":\"freeswitch-sgw\",\"image\":\"ccod/freeswitch:1.10.2-qn-002\",\"hostNetwork\":true,\"volumeMounts\":[{\"mountPath\":\"/record\",\"name\":\"record\"}],\"workingDir\":\"/root/Platform\",\"command\":[\"/bin/sh\",\"-c\"],\"args\":[\"/usr/local/freeswitch/bin/start.sh SGW;\"]}]}}}}";
//        po = gson.fromJson(gson.toJson(templateList.get(13)), K8sObjectTemplatePo.class);
//        deployment = gson.fromJson(json, V1Deployment.class);
//        po.getLabels().put(appNameLabel, "sgw");
//        po.setDeployJson(gson.toJson(deployment));
//        po.setServiceJson(null);
//        templateList.add(po);
//        K8sObjectTemplatePo po = gson.fromJson(gson.toJson(templateList.get(17)), K8sObjectTemplatePo.class);
//        po.getLabels().put(appTypeLabel, AppType.NODEJS.name);
//        V1Deployment deployment = gson.fromJson(po.getDeployJson(), V1Deployment.class);
//        deployment.getMetadata().getLabels().put(appTypeLabel, AppType.NODEJS.name);
//        String json = gson.toJson(deployment);
//        json = "{\"apiVersion\":\"apps/v1\",\"kind\":\"Deployment\",\"metadata\":{\"labels\":{\"dcms\":\"dcms\",\"dcms-alias\":\"dcms\",\"dcms-version\":\"11110\",\"domain-id\":\"manage01\",\"job-id\":\"73a8e02621\",\"type\":\"CCOD_DOMAIN_APP\",\"app-type\":\"NODEJS\"},\"name\":\"dcms-manage01\",\"namespace\":\"test-by-wyf\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"dcms\":\"dcms\",\"domain-id\":\"manage01\"}},\"template\":{\"metadata\":{\"labels\":{\"dcms\":\"dcms\",\"domain-id\":\"manage01\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/test-by-wyf/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/test-by-wyf/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/test-by-wyf/tnsnames.ora /usr/local/lib/tnsnames.ora;cd /root/resin-4.0.13;keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/jre/lib/security/cacerts;./bin/resin.sh start;tail -F ./log/*.log\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/ccod-base/resin-jdk:resin-4.0.13_jdk-1.7.0_10-011\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcms-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":3,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"dcms-runtime\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcms-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":20,\"periodSeconds\":10,\"successThreshold\":1,\"timeoutSeconds\":2},\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"200m\",\"memory\":\"200Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/root/resin-4.0.13/log\",\"name\":\"ccod-runtime\",\"subPath\":\"dcms\"}]}],\"hostAliases\":[{\"hostnames\":[\"test-by-wyf.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"command\":[\"/bin/sh\",\"-c\",\"mkdir /opt/webapps -p;cd /opt/webapps;mv /opt/dcms.war /opt/webapps/dcms.war;mkdir /opt/webapps/WEB-INF/classes/ -p;cp /cfg/dcms-cfg/config.properties /opt/webapps/WEB-INF/classes/config.properties;jar uf dcms.war WEB-INF/classes/config.properties;cp /cfg/dcms-cfg/Param-Config.xml /opt/webapps/WEB-INF/classes/Param-Config.xml;jar uf dcms.war WEB-INF/classes/Param-Config.xml;mkdir /opt/webapps/WEB-INF/ -p;cp /cfg/dcms-cfg/web.xml /opt/webapps/WEB-INF/web.xml;jar uf dcms.war WEB-INF/web.xml;mv /opt/webapps/dcms.war /war/dcms-manage01.war\"],\"image\":\"nexus.io:5000/ccod/dcms:11110\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"dcms\",\"resources\":{\"limits\":{\"cpu\":\"200m\",\"memory\":\"200Mi\"},\"requests\":{\"cpu\":\"100m\",\"memory\":\"100Mi\"}},\"volumeMounts\":[]}],\"terminationGracePeriodSeconds\":30,\"volumes\":[{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/test-by-wyf/manage01\",\"type\":\"DirectoryOrCreate\"},\"name\":\"ccod-runtime\"}]}}}}";
//        po.setDeployJson(json);
//        templateList.add(po);
//        templateList.forEach(t->{
//            if(t.getDeployJson() != null){
//                V1Deployment d = gson.fromJson(t.getDeployJson(), V1Deployment.class);
//                t.setDeployJson(gson.toJson(Arrays.asList(d)));
//            }
//            if(t.getServiceJson() != null){
//                V1Service s = gson.fromJson(t.getServiceJson(), V1Service.class);
//                t.setServiceJson(gson.toJson(Arrays.asList(s)));
//            }
//            if(t.getIngressJson() != null){
//                ExtensionsV1beta1Ingress i = gson.fromJson(t.getIngressJson(), ExtensionsV1beta1Ingress.class);
//                t.setIngressJson(gson.toJson(Arrays.asList(i)));
//            }
//            if(t.getEndpointsJson() != null){
//                V1Endpoints e = gson.fromJson(t.getEndpointsJson(), V1Endpoints.class);
//                t.setEndpointsJson(gson.toJson(Arrays.asList(e)));
//            }
//        });
//        templateList.forEach(t->{
//            if(t.getLabels().containsKey(appTypeLabel) && t.getLabels().get(appTypeLabel).equals(AppType.THREE_PART_APP.name))
//                if(t.getLabels().get(appNameLabel).equals("umg"))
//                    t.setDeployJson(gson.toJson(new ArrayList<>()));
//                else
//                    t.setEndpointsJson(gson.toJson(new ArrayList<>()));
//        });
//        templateList.forEach(t->{
//            if(t.getIngressJson() != null){
//                List<ExtensionsV1beta1Ingress> ingresses = gson.fromJson(t.getIngressJson(), new TypeToken<List<ExtensionsV1beta1Ingress>>() {}.getType());
//                t.setIngressJson(gson.toJson(ingresses.get(0)));
//            }
//        });
//        String json = "{\"metadata\":{\"name\":\"umg141\",\"namespace\":\"k8s-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"10.130.41.141\"}],\"ports\":[{\"port\":12000,\"protocol\":\"TCP\"}]}]}";
//        K8sObjectTemplatePo po = gson.fromJson(gson.toJson(templateList.get(1)), K8sObjectTemplatePo.class);
//        po.getLabels().put(appNameLabel, "umg");
//        po.setDeployJson(gson.toJson(new ArrayList<>()));
//        po.setServiceJson(gson.toJson(new ArrayList<>()));
//        po.setEndpointsJson(json);
//        templateList.add(po);
//        po = gson.fromJson(gson.toJson(templateList.get(7)), K8sObjectTemplatePo.class);
//        po.getLabels().put(appNameLabel, "umg");
//        po.setDeployJson(gson.toJson(new ArrayList<>()));
//        po.setServiceJson(gson.toJson(new ArrayList<>()));
//        po.setEndpointsJson(json);
//        templateList.add(po);
//        po = gson.fromJson(gson.toJson(templateList.get(13)), K8sObjectTemplatePo.class);
//        po.getLabels().put(appNameLabel, "umg");
//        po.setDeployJson(gson.toJson(new ArrayList<>()));
//        po.setServiceJson(gson.toJson(new ArrayList<>()));
//        po.setEndpointsJson(json);
//        templateList.add(po);
//        String json = "{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"job-id\":\"73a8e02621\",\"name\":\"umg41\",\"type\":\"THREE_PART_SERVICE\"},\"name\":\"umg41\",\"namespace\":\"test-by-wyf\"},\"spec\":{\"ports\":[{\"port\":12000,\"protocol\":\"TCP\",\"targetPort\":12000}],\"type\":\"ClusterIP\"}}";
//        templateList.forEach(t->{
//            if(t.getLabels().containsKey(appNameLabel) && t.getLabels().get(appNameLabel).equals("umg")){
////                V1Endpoints endpoints = gson.fromJson(t.getEndpointsJson(), V1Endpoints.class);
////                t.setEndpointsJson(gson.toJson(Arrays.asList(endpoints)));
//                t.setServiceJson(gson.toJson(Arrays.asList(gson.fromJson(json, V1Service.class))));
//            }
//        });
        logger.error(gson.toJson(templateList));
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
    public List<CCODThreePartAppPo> getThreePartAppDepend(String ccodVersion, String tag){
        logger.debug(String.format("begin to query depend of ccodVersion=%s and tag=%s", ccodVersion, tag));
        List<CCODThreePartAppPo> list = ccodThreePartAppMapper.select(ccodVersion, tag, null);
        Assert.isTrue(list.size() > 0, String.format("can not find three part app depend for ccodVersion=%s and tag=%s", ccodVersion, tag));
        logger.info(String.format("ccodVersion=%s and tag=%s three part app depend is %s", ccodVersion, tag, gson.toJson(list)));
        return list;
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
            case CREATING:
            case UPDATING:
            case DEBUG:
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

//    private void copySourceFile(String sourceFileName, String fileSaveDir) throws IOException
//    {
//        Resource resource = new ClassPathResource(sourceFileName);
//        InputStreamReader isr = new InputStreamReader(resource.getInputStream(), "UTF-8");
//        BufferedReader br = new BufferedReader(isr);
//        File saveDir = new File(fileSaveDir);
//        if(!saveDir.exists())
//        {
//            saveDir.mkdirs();
//        }
//        String savePath = String.format("%s/%s", fileSaveDir, sourceFileName);
//        savePath = savePath.replaceAll("\\\\", "/");
//        File scriptFile = new File(savePath);
//        scriptFile.createNewFile();
//        BufferedWriter out = new BufferedWriter(new FileWriter(scriptFile));
//        String lineTxt = null;
//        while ((lineTxt = br.readLine()) != null)
//        {
//            out.write(lineTxt + "\n");
//        }
//        br.close();
//        out.close();
//    }

    /**
     * 将source下指定文件拷贝到指定目目录
     * @param sourceFileName 需要被拷贝的source下指定文件
     * @param fileSaveDir 指定目录
     * @param fileName 拷贝后的文件名
     * @throws IOException
     */
    private void copySourceFile(String sourceFileName, String fileSaveDir, String fileName) throws IOException
    {
        Resource resource = new ClassPathResource(sourceFileName);
        InputStreamReader isr = new InputStreamReader(resource.getInputStream(), "UTF-8");
        BufferedReader br = new BufferedReader(isr);
        File saveDir = new File(fileSaveDir);
        if(!saveDir.exists())
        {
            saveDir.mkdirs();
        }
        String savePath = String.format("%s/%s", fileSaveDir, fileName);
        savePath = savePath.replaceAll("\\\\", "/");
        File scriptFile = new File(savePath);
        scriptFile.createNewFile();
        BufferedWriter out = new BufferedWriter(new FileWriter(scriptFile));
        String lineTxt = null;
        while ((lineTxt = br.readLine()) != null)
        {
            out.write(lineTxt + "\n");
        }
        br.close();
        out.close();
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
            BizSetDefine setDefine = this.ccodBiz.getSet().stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity())).get(setName);
            Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = setAppMap.get(setName)
                    .stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainName));
            List<CCODDomainInfo> domainList = new ArrayList<>();
            for (String domainName : domainAppMap.keySet()) {
                List<PlatformAppDeployDetailVo> domAppList = domainAppMap.get(domainName).stream()
                        .sorted(getAppSort(setDefine)).collect(Collectors.toList());
                CCODDomainInfo domain = new CCODDomainInfo(domAppList.get(0).getDomainId(), domAppList.get(0).getDomainName());
                Map<String, List<PlatformAppDeployDetailVo>> assembleAppMap = domAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getAssembleTag));
                for (String assembleTag : assembleAppMap.keySet()) {
                    CCODAssembleInfo assemble = new CCODAssembleInfo(assembleTag);
                    for (PlatformAppDeployDetailVo deployApp : assembleAppMap.get(assembleTag)) {
                        CCODModuleInfo bkModule = new CCODModuleInfo(deployApp);
                        assemble.getModules().add(bkModule);
                    }
                    assemble.setModules(assemble.getModules().stream().sorted(getCCODModuleSort(setDefine)).collect(Collectors.toList()));
                    domain.getAssembles().add(assemble);
                }
                domain.setAssembles(domain.getAssembles().stream().sorted(getCCODAssembleSort(setDefine)).collect(Collectors.toList()));
                domainList.add(domain);
            }
            CCODSetInfo set = new CCODSetInfo(setName);
            set.setDomains(domainList.stream().sorted(getCCODDomainSort(setDefine)).collect(Collectors.toList()));
            setList.add(set);
        }
        for (String setName : setNames) {
            if (!setAppMap.containsKey(setName)) {
                CCODSetInfo setInfo = new CCODSetInfo(setName);
                setList.add(setInfo);
            }
        }
        setList = setList.stream().sorted(this.getCCODSetSort()).collect(Collectors.toList());
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
                topo.setStatus(CCODPlatformStatus.UNKNOWN);
            }
            topoList.add(topo);
        }
        return topoList;
    }

    @Override
    public List<AppDebugDetailPo> queryPlatformDebugApps(String platformId) {
        return appDebugDetailMapper.select(platformId, null, null, null);
    }

    @Override
    public List<PlatformAppDeployDetailVo> queryPlatformCCODAppDeployStatus(String platformId, boolean isGetCfg) throws ApiException, ParamException, IOException, InterfaceCallException, NexusException {
        PlatformPo platform = getK8sPlatform(platformId);
        List<PlatformAppDeployDetailVo> details = this.k8sTemplateService.getPlatformAppDetailFromK8s(platform, isGetCfg);
        return details;
    }

    @Override
    public PlatformAppDeployDetailVo queryPlatformCCODAppDeployStatus(String platformId, String domainId, String appName, String alias, boolean isGetCfg) throws ApiException, ParamException, IOException, InterfaceCallException, NexusException {
        PlatformPo platform = getK8sPlatform(platformId);
        PlatformAppDeployDetailVo detail = this.k8sTemplateService.getPlatformAppDetailFromK8s(platform, domainId, appName, alias, isGetCfg);
        return detail;
    }

    @Override
    public List<PlatformAppPo> updatePlatformApps(String platformId, String platformName, List<AppUpdateOperationInfo> appList) throws NotSupportAppException, ParamException, InterfaceCallException, NexusException, LJPaasException, IOException {
        logger.debug(String.format("begin to update %d apps of %s(%s)", appList.size(), platformName, platformId));
        PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
        Assert.notNull(platformPo, String.format("%s platform not exist", platformId));
        Assert.isTrue(platformPo.getPlatformName().equals(platformName), String.format("name of %s is %s not %s", platformId, platformPo.getPlatformName(), platformName));
        List<LJHostInfo> bkHostList = this.paasService.queryBKHost(platformPo.getBkBizId(), null, null, null, null);
        Set<Integer> cloudSet = bkHostList.stream().flatMap(h-> Arrays.stream(h.getClouds())).map(c->c.getId()).collect(Collectors.toSet());
        Assert.isTrue(cloudSet.size()==1, String.format("cloudId of %s not unique", platformId));
        List<AppUpdateOperationInfo> opts = appList.stream().filter(o->StringUtils.isBlank(o.getDomainName())).collect(Collectors.toList());
        Assert.isTrue(opts.size() == 0, "domainName can not be empty");
        List<DomainPo> existDomains = this.domainMapper.select(platformId, null);
        Map<String, DomainPo> existDomainMap = existDomains.stream().collect(Collectors.toMap(DomainPo::getDomainName, Function.identity()));
        StringBuffer sb = new StringBuffer();
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = this.platformAppDeployDetailMapper.selectPlatformApps(platformId, null, null)
                .stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        Map<String, BizSetDefine> bizSetMap = this.ccodBiz.getSet().stream().collect(Collectors.toMap(BizSetDefine::getName, Function.identity()));
        appList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getDomainName)).forEach((k,v)->{
            if(!existDomainMap.containsKey(k)) {
                sb.append(String.format("domain %s not exist;", k));
            }
            else {
                String domainId = existDomainMap.get(k).getDomainId();
                BizSetDefine setDefine = bizSetMap.get(existDomainMap.get(k).getBizSetName());
                List<PlatformAppDeployDetailVo> domainApps = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
                v.forEach(a -> {
                    a.setDomainId(domainId);
                    sb.append(checkAppOperationParam(platformPo.getCcodVersion(), a, setDefine, domainApps, bkHostList));
                });
            }
        });
        Assert.isTrue(StringUtils.isBlank(sb.toString()), sb.toString());
        List<PlatformAppDeployDetailVo> deployApps = this.platformAppDeployDetailMapper.selectPlatformApps(platformId, null, null);
        modifyPlatformApps(platformPo, existDomains, appList, deployApps, bkHostList);
        this.paasService.syncClientCollectResultToPaas(platformPo.getBkBizId(), platformId, platformPo.getBkCloudId());
        return new ArrayList<>();
    }

    private void modifyPlatformApps(PlatformPo platformPo, List<DomainPo> existDomainList, List<AppUpdateOperationInfo> appList, List<PlatformAppDeployDetailVo> deployApps, List<LJHostInfo> hostList) throws NotSupportAppException, ParamException, InterfaceCallException, NexusException, LJPaasException, IOException
    {
        Date date = new Date();
        String platformId = platformPo.getPlatformId();
        Map<String, DomainPo> domainMap = existDomainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        Map<String, List<AppUpdateOperationInfo>> domainOptMap = appList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getDomainId));
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = deployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        Map<String, List<AssemblePo>> domainAssembleMap = this.assembleMapper.select(platformId, null).stream().collect(Collectors.groupingBy(AssemblePo::getDomainId));
        List<AppModuleVo> registerApps = this.appManagerService.queryAllRegisterAppModule(null);
        Map<String, Map<String, List<AppFileNexusInfo>>> domainCfgMap = new HashMap<>();
        for (String domainId : domainOptMap.keySet()) {
            List<PlatformAppDeployDetailVo> deployAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
            Map<String, List<AppFileNexusInfo>> appCfgMap = preprocessDomainApps(platformId, domainOptMap.get(domainId), deployAppList, domainMap.get(domainId), false, date);
            domainCfgMap.put(domainId, appCfgMap);
        }
        for (String domainId : domainOptMap.keySet()) {
            List<PlatformAppDeployDetailVo> deployAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
            List<AssemblePo> assembleList = domainAssembleMap.containsKey(domainId) ? domainAssembleMap.get(domainId) : new ArrayList<>();
            handleDomainApps(platformId, domainMap.get(domainId), domainOptMap.get(domainId), assembleList, deployAppList, registerApps, domainCfgMap.get(domainId));
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
        for (AppUpdateOperationInfo optInfo : domainOptList) {
            switch (optInfo.getOperation()) {
                case ADD:
                case UPDATE:
                case DEBUG:
                    if (StringUtils.isBlank(optInfo.getAssembleTag())) {
                        optInfo.setAssembleTag(String.format("%s-%s", optInfo.getAlias(), domainId));
                    }
                    String directory = optInfo.getPlatformAppCfgDirectory(date, platformId, domainId);
                    logger.debug(String.format("download cfg of %s at %s and upload to %s/%s", optInfo.getAlias(), domainId, this.platformAppCfgRepository, directory));
                    List<AppFileNexusInfo> cfgs = appManagerService.downloadAndUploadAppFiles(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, optInfo.getCfgs(), this.platformAppCfgRepository, directory);
                    assetMap.put(optInfo.getAlias(), cfgs);
                    break;
                default:
                    break;
            }
        }
        return assetMap;
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
                    Assert.isTrue(platformBase.getGlsDBType().equals(DatabaseType.ORACLE) || platformBase.getGlsDBType().equals(DatabaseType.MYSQL), String.format("unsupported gls dbType : %s", platformBase.getGlsDBType().name));
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

    private String checkAppOperationParam(String ccodVersion, AppUpdateOperationInfo optInfo, BizSetDefine setDefine, List<PlatformAppDeployDetailVo> deployApps, List<LJHostInfo> hostList)
    {
        String checkResult = this.appManagerService.checkAppBaseProperties(optInfo, optInfo.getOperation());
        if(StringUtils.isNotBlank(checkResult))
            return checkResult;
        String appName = optInfo.getAppName();
        String alias = optInfo.getAlias();
        Map<String, PlatformAppDeployDetailVo> aliasAppMap = deployApps.stream()
                .collect(Collectors.toMap(d->d.getAlias(), v->v));
        Boolean aliasExist = null;
        boolean needHostIp = false;
        boolean notCheckVersion = false;
        boolean notCheckSupport = false;
        AppUpdateOperation operation = optInfo.getOperation();
        String tag = String.format("%s %s", operation.name, appName);
        switch (operation)
        {
            case ADD:
                needHostIp = true;
                break;
            case UPDATE:
                aliasExist = true;
                break;
            case DELETE:
                aliasExist = true;
                notCheckSupport = true;
                notCheckVersion = true;
                break;
            case DEBUG:
                aliasExist = aliasAppMap.containsKey(alias) ? true : false;
                needHostIp = aliasExist ? false : true;
                break;
            default:
                return String.format("not support operation for %s", operation.name, tag);
        }
        Map<String, String> appMap = setDefine.getApps().stream().collect(Collectors.toMap(a->a.getName(), v->v.getAlias()));
        if(!notCheckSupport && !appMap.containsKey(appName))
            return String.format("app %s not support by %s", appName, setDefine.getName());
        if(aliasExist != null && aliasExist && !aliasAppMap.containsKey(alias)) {
            return String.format("%s not exist for %s", alias, tag);
        }
        else if(aliasExist != null && !aliasExist && aliasAppMap.containsKey(alias)){
            return String.format("%s has exist for %s", alias, tag);
        }
        if(!notCheckVersion && StringUtils.isNotBlank(optInfo.getCcodVersion()) && !optInfo.getCcodVersion().equals(ccodVersion)){
            return String.format("%s[%s] support ccod %s not %s", appName, optInfo.getVersion(), optInfo.getCcodVersion(), ccodVersion);
        }
        if(needHostIp && StringUtils.isBlank(optInfo.getHostIp()))
            return String.format("hostIp is blank for %s", tag);
        Map<String, LJHostInfo> hostMap = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity()));
        if(StringUtils.isNotBlank(optInfo.getHostIp()) && !hostMap.containsKey(optInfo.getHostIp()))
            return String.format("host %s not exist for %s", optInfo.getHostIp(), tag);
        return "";
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
        this.ongoingPlatformId = platformId;
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
            this.ongoingPlatformId = null;
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
        Assert.isTrue(bkHostList.size()>0, String.format("%s has not any host", platformPo.getPlatformName()));
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
        this.platformUpdateSchemaMap.put(updateSchema.getPlatformId(), updateSchema);
        this.platformUpdateSchemaMapper.delete(updateSchema.getPlatformId());
        PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
        schemaPo.setContext(gson.toJson(updateSchema).getBytes());
        schemaPo.setPlatformId(updateSchema.getPlatformId());
        this.platformUpdateSchemaMapper.insert(schemaPo);
        platformPo.setStatus(updateSchema.getTaskType().equals(PlatformUpdateTaskType.CREATE) ? CCODPlatformStatus.SCHEMA_CREATE : CCODPlatformStatus.SCHEMA_UPDATE);
        this.platformMapper.update(platformPo);
        if (status.equals(UpdateStatus.EXEC) || status.equals(UpdateStatus.WAIT_EXEC)){
            execPlatformSchema(platformPo, updateSchema, domainList, assembleList, platformDeployApps, registerApps);
        }
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

    private Comparator<CCODSetInfo> getCCODSetSort()
    {
        Map<String, Integer> setSortMap = new HashMap<>();
        for(int i = 0; i < this.ccodBiz.getSet().size(); i++)
            setSortMap.put(this.ccodBiz.getSet().get(i).getName(), i);
        Comparator<CCODSetInfo> sort = new Comparator<CCODSetInfo>() {
            @Override
            public int compare(CCODSetInfo o1, CCODSetInfo o2) {
                return setSortMap.get(o1.getBkSetName()) - setSortMap.get(o2.getBkSetName());
            }
        };
        return sort;
    }

    private Comparator<CCODDomainInfo> getCCODDomainSort(BizSetDefine setDefine)
    {
        Comparator<CCODDomainInfo> sort = new Comparator<CCODDomainInfo>() {
            @Override
            public int compare(CCODDomainInfo o1, CCODDomainInfo o2) {
                return Integer.parseInt(o1.getDomainId().replace(setDefine.getFixedDomainId(), "")) - Integer.parseInt(o2.getDomainId().replace(setDefine.getFixedDomainId(), ""));
            }
        };
        return sort;
    }

    private Comparator<CCODAssembleInfo> getCCODAssembleSort(BizSetDefine setDefine)
    {
        Map<String, Integer> appSortMap = new HashMap<>();
        for(int i = 0; i < setDefine.getApps().size(); i++)
            appSortMap.put(setDefine.getApps().get(i).getName(), i);
        Map<String, String> aliasMap = setDefine.getApps().stream().collect(Collectors.toMap(o->o.getName(), v->v.getAlias()));
        Comparator<CCODAssembleInfo> sort = new Comparator<CCODAssembleInfo>() {
            @Override
            public int compare(CCODAssembleInfo a1, CCODAssembleInfo a2) {
                CCODModuleInfo o1 = a1.getModules().get(0);
                CCODModuleInfo o2 = a2.getModules().get(0);
                if(!o1.getModuleName().equals(o2.getModuleName()))
                    return appSortMap.get(o1.getModuleName()) - appSortMap.get(o2.getModuleName());
                return getIndexFromAlias(o1.getModuleAlias(), aliasMap.get(o1.getModuleName())) - getIndexFromAlias(o2.getModuleAlias(), aliasMap.get(o2.getModuleName()));
            }
        };
        return sort;
    }

    private Comparator<CCODModuleInfo> getCCODModuleSort(BizSetDefine setDefine)
    {
        Map<String, Integer> appSortMap = new HashMap<>();
        for(int i = 0; i < setDefine.getApps().size(); i++)
            appSortMap.put(setDefine.getApps().get(i).getName(), i);
        Map<String, String> aliasMap = setDefine.getApps().stream().collect(Collectors.toMap(o->o.getName(), v->v.getAlias()));
        Comparator<CCODModuleInfo> sort = new Comparator<CCODModuleInfo>() {
            @Override
            public int compare(CCODModuleInfo o1, CCODModuleInfo o2) {
                if(!o1.getModuleName().equals(o2.getModuleName()))
                    return appSortMap.get(o1.getModuleName()) - appSortMap.get(o2.getModuleName());
                return getIndexFromAlias(o1.getModuleAlias(), aliasMap.get(o1.getModuleName())) - getIndexFromAlias(o2.getModuleAlias(), aliasMap.get(o2.getModuleName()));
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
        Comparator<DomainUpdatePlanInfo> sort = getDomainPlanSort();
        UpdateStatus status = schema.getStatus();
        List<DomainUpdatePlanInfo> plans = schema.getDomainUpdatePlanList().stream().
                filter(plan->plan.getStatus().equals(status)).sorted(sort).collect(Collectors.toList());
        boolean isNewPlatform = schema.getTaskType().equals(PlatformUpdateTaskType.CREATE) || schema.getTaskType().equals(PlatformUpdateTaskType.RESTORE) ? true : false;
        String platformId = platformPo.getPlatformId();
        CCODPlatformStatus platformStatus = platformPo.getStatus();
        Map<String, DomainPo> domainMap = domainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        boolean clone = PlatformCreateMethod.CLONE.equals(schema.getCreateMethod()) ? true : false;
        schema.getDomainUpdatePlanList().stream()
                .forEach(plan -> plan.getAppUpdateOperationList().stream().forEach(opt->opt.setDomainId(plan.getDomainId())));
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = platformDeployApps.stream()
                .collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        List<K8sOperationInfo> steps = generateExecStepForSchema(platformPo, schema, domainList, platformDeployApps);
        if(!status.equals(UpdateStatus.EXEC))
            return;
        Assert.isTrue(!this.isPlatformCheckOngoing, "some platform collect or deploy task is ongoing");
        Map<String, List<AssemblePo>> domainAssembleMap = assembleList.stream().collect(Collectors.groupingBy(AssemblePo::getDomainId));
        this.isPlatformCheckOngoing = true;
        this.ongoingPlatformId = platformId;
        this.platformDeployLogs.clear();
        new Thread(()->{
            try {
                platformPo.setStatus(isNewPlatform ? CCODPlatformStatus.CREATING : CCODPlatformStatus.UPDATING);
                logger.debug(String.format("change platform %s status from %s to %s", platformId, platformStatus.name, platformPo.getStatus().name));
                platformMapper.update(platformPo);
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
                PlatformSchemaExecResultVo execResultVo = execPlatformUpdateSteps(platformPo, steps, platformDeployLogs, schema, platformDeployApps);
                logger.info(String.format("platform schema execute result : %s", gson.toJson(execResultVo)));
                if(!execResultVo.isSuccess()){
                    logger.error(String.format("schema execute fail : %s", execResultVo.getErrorMsg()));
                    logger.debug(String.format("deploy platform %s fail change status to %s", platformId, CCODPlatformStatus.DEPLOY_FAIL.name));
                    platformPo.setStatus(CCODPlatformStatus.DEPLOY_FAIL);
                    platformMapper.update(platformPo);
                    return;
                }
                else if(schema.getTaskType().equals(PlatformUpdateTaskType.RESTORE)){
                    platformPo.setStatus(CCODPlatformStatus.RUNNING);
                    platformPo.setUpdateTime(new Date());
                    if(platformUpdateSchemaMap.containsKey(platformId)){
                        platformUpdateSchemaMap.remove(platformId);
                    }
                    platformUpdateSchemaMapper.delete(platformId);
                    platformMapper.update(platformPo);
                    return;
                }
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
                boolean closeSchema = status.equals(UpdateStatus.EXEC) && schema.getDomainUpdatePlanList().size() == 0 ? true : false;
                this.platformUpdateSchemaMap.put(schema.getPlatformId(), schema);
                this.platformUpdateSchemaMapper.delete(schema.getPlatformId());
                if(closeSchema) {
                    this.platformUpdateSchemaMap.remove(schema.getPlatformId());
                    platformPo.setStatus(CCODPlatformStatus.RUNNING);
                }
                else {
                    PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
                    schemaPo.setContext(gson.toJson(schema).getBytes());
                    schemaPo.setPlatformId(schema.getPlatformId());
                    this.platformUpdateSchemaMapper.insert(schemaPo);
                    platformPo.setStatus(CCODPlatformStatus.SCHEMA_UPDATE);
                }
                logger.info(String.format("schema of %s has been execute and status change to %s",
                        platformPo.getPlatformId(), platformPo.getStatus().name));
                this.platformMapper.update(platformPo);
                logger.debug("begin to sync platformTopo to lj paas");
                this.paasService.syncClientCollectResultToPaas(platformPo.getBkBizId(), platformPo.getPlatformId(), platformPo.getBkCloudId());
            }
            catch (Exception ex) {
                logger.error(String.format("schema execute fail"), ex);
                logger.debug(String.format("status of platform %s has been restored to %s", platformId, status.name));
                platformPo.setStatus(platformStatus);
                platformMapper.update(platformPo);
            }
            finally {
                this.isPlatformCheckOngoing = false;
                this.ongoingPlatformId = null;
            }
        }).start();
    }

    private List<K8sOperationInfo> generateExecStepForSchema(
            PlatformPo platformPo, PlatformUpdateSchemaInfo schema, List<DomainPo> domainList,
            List<PlatformAppDeployDetailVo> platformDeployApps)
            throws ParamException, ApiException, InterfaceCallException, IOException, LJPaasException, NotSupportAppException, NexusException {
        Map<String, Integer> setMap = new HashMap<>();
        for(int i = 0; i < this.ccodBiz.getSet().size(); i++)
            setMap.put(this.ccodBiz.getSet().get(i).getFixedDomainId(), i);
        Comparator<DomainUpdatePlanInfo> sort = getDomainPlanSort();
        UpdateStatus status = schema.getStatus();
        List<DomainUpdatePlanInfo> plans = schema.getDomainUpdatePlanList().stream().
                filter(plan->plan.getStatus().equals(status)).sorted(sort).collect(Collectors.toList());
        boolean isNewPlatform = schema.getTaskType().equals(PlatformUpdateTaskType.CREATE) || schema.getTaskType().equals(PlatformUpdateTaskType.RESTORE) ? true : false;
        List<K8sOperationInfo> steps = new ArrayList<>();
        String platformId = platformPo.getPlatformId();
        String jobId = schema.getSchemaId();
        Map<String, DomainPo> domainMap = domainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        schema.getDomainUpdatePlanList().stream()
                .forEach(plan -> plan.getAppUpdateOperationList().stream().forEach(opt->opt.setDomainId(plan.getDomainId())));
        V1Deployment deployGls = null;
        Map<String, List<AppUpdateOperationInfo>> optMap = plans.stream()
                .flatMap(plan->plan.getAppUpdateOperationList().stream()).collect(Collectors.toList())
                .stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
        List<AppUpdateOperationInfo> glsOpts = optMap.get("glsServer");
        boolean hasUCDS = optMap.containsKey("UCDServer");
        if(glsOpts != null && glsOpts.size() > 1)
            throw new ParamException(String.format("operation of glsServer multi define"));
        List<PlatformAppDeployDetailVo> deployedGlsList = platformDeployApps.stream().collect(Collectors.groupingBy(d->d.getAppName())).get("glsServer");
        if(deployedGlsList != null && deployedGlsList.size() > 1){
            throw new ParamException(String.format("glsserver of %s has been multi deployed", platformId));
        }
        if(isNewPlatform) {
            if(hasUCDS && glsOpts == null){
                throw new ParamException(String.format("new platform %s has not glsServer", platformId));
            }
            List<CCODThreePartAppPo> threePartApps = ccodThreePartAppMapper.select(schema.getCcodVersion(), null, null);
            String nfsServerIp = StringUtils.isBlank(schema.getNfsServerIp()) ? schema.getK8sHostIp() : schema.getNfsServerIp();
            List<K8sOperationInfo> baseCreateSteps = this.k8sTemplateService.generateBasePlatformCreateSteps(jobId, schema.getK8sJob(), schema.getNamespace(), new ArrayList<>(),
                    null, null, threePartApps, nfsServerIp, String.format("base-%s", platformPo.getPlatformId()), platformPo);
            steps.addAll(baseCreateSteps);
            List<K8sOperationInfo> platformCreateSteps = this.k8sTemplateService.generatePlatformCreateSteps(jobId, schema.getK8sJob(), schema.getNamespace(), new ArrayList<>(),
                    null, null, threePartApps, schema.getThreePartServices(), nfsServerIp, platformPo);
            steps.addAll(platformCreateSteps);
            generateYamlForDeploy(schema, steps);
        }
        else {
            if(deployedGlsList != null && glsOpts != null && glsOpts.get(0).getOperation().equals(AppUpdateOperation.ADD)){
                throw new ParamException(String.format("exist platform %s has a glsServer, can not be add new one", platformId));
            }
            if(hasUCDS){
                if(deployedGlsList == null){
                    throw new ParamException(String.format("exist platform %s has not glsServer", platformId));
                }
                else{
                    Map<String, String> selector = new HashMap<>();
                    selector.put("glsServer", deployedGlsList.get(0).getAlias());
                    selector.put(domainIdLabel, deployedGlsList.get(0).getDomainId());
                    List<V1Deployment> deploys = k8sApiService.selectNamespacedDeployment(platformId, selector, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
                    if(deploys.size() == 0){
                        throw new ParamException(String.format("not find glsServer deployment at exist platform %s", platformId));
                    }
                    else if(deploys.size() > 1){
                        throw new ParamException(String.format("find %d glsServer deployment at exist platform %s", deploys.size(), platformId));
                    }
                    deployGls = deploys.get(0);
                }
            }
        }
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = platformDeployApps.stream()
                .collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        for(DomainUpdatePlanInfo plan : plans)
        {
            if(domainMap.containsKey(plan.getDomainId()) && (plan.getPublicConfig() == null || plan.getPublicConfig().size() == 0))
                plan.setPublicConfig(domainMap.get(plan.getDomainId()).getCfgs());
            List<PlatformAppDeployDetailVo> domainApps = domainAppMap.containsKey(plan.getDomainId()) ? domainAppMap.get(plan.getDomainId()) : new ArrayList<>();
            List<K8sOperationInfo> deploySteps = generateDomainDeploySteps(jobId, platformPo, plan, domainApps, isNewPlatform, deployGls);
            steps.addAll(deploySteps);
            if(isNewPlatform && hasUCDS){
                List<K8sOperationInfo> opts = steps.stream().filter(s->s.getKind().equals(K8sKind.DEPLOYMENT) && s.getOperation().equals(K8sOperation.CREATE) && s.getName().matches("^glsserver\\-.+"))
                        .collect(Collectors.toList());
                if(opts.size() > 0){
                    deployGls = (V1Deployment)opts.get(0).getObj();
                }
            }
        }
        return steps;
    }

    @Override
    public PlatformTopologyInfo restorePlatformTopologyFromK8s(String platformId, String platformName, String ccodVersion, String k8sApiUrl, String k8sAuthToken) throws ApiException, ParamException {
        logger.debug(String.format("begin to restore platform %s(%s) topology from %s", platformId, platformName, k8sApiUrl));
        boolean isExist = this.k8sApiService.isNamespaceExist(platformName, k8sApiUrl, k8sAuthToken);
        Assert.isTrue(isExist, String.format("namespace %s not exist at %s", platformId, k8sApiUrl));

        return null;
    }

    @Override
    public PlatformAppDeployDetailVo queryPlatformApp(String platformId, String domainId, String alias) {
        PlatformAppDeployDetailVo deployApp = this.platformAppDeployDetailMapper.selectPlatformApp(platformId, domainId, alias);
        Assert.notNull(deployApp, String.format("%s/%s/%s not exist", platformId, domainId, alias));
        return deployApp;
    }

    @Override
    public void debugHandle() {
        this.debugLock.writeLock().lock();
        try{
            logger.debug(String.format("begin to handle app debug task : there are %d tasks in queue", debugQueue.size()));
            Map<String, List<AppDebugTaskVo>> taskMap = debugQueue.stream().collect(Collectors.groupingBy(AppDebugTaskVo::getDebugTag));
            for(String tag : taskMap.keySet()){
                List<AppDebugTaskVo> tasks = taskMap.get(tag).stream().sorted(Comparator.comparing(AppDebugTaskVo::getId).reversed()).collect(Collectors.toList());
                if(tasks.stream().filter(t->t.getStatus()==AppDebugTaskVo.RUNNING).count() == 0){
                    AppDebugTaskVo task = tasks.get(0);
                    for(int i = 1; i < tasks.size(); i++){
                        debugQueue.remove(tasks.get(i));
                    }
                    PlatformPo platform;
                    try{
                        platform = getK8sPlatform(task.getDebugInfo().getPlatformId());
                        if(!platform.getStatus().equals(CCODPlatformStatus.DEBUG)){
                            platform.getParams().put(PlatformBase.statusBeforeDebugKey, platform.getStatus().name);
                            logger.debug(String.format("begin to debug %s platform and change status from to %s", platform.getPlatformId(), platform.getStatus().name, CCODPlatformStatus.DEBUG.name));
                            platform.setStatus(CCODPlatformStatus.DEBUG);
                            platformMapper.update(platform);
                        }
                    }
                    catch (Exception ex){
                        logger.error(String.format("query %s platform exception", task.getDebugInfo().getPlatformId()), ex);
                        continue;
                    }
                    task.setExecTime(new Date());
                    task.setStatus(AppDebugTaskVo.RUNNING);
                    startAppDebug(platform, task);
                }
            }
        }
        finally {
            this.debugLock.writeLock().unlock();
        }
    }

    @Override
    public void debugPlatformApp(String platformId, String domainId, AppUpdateOperationInfo optInfo) throws ParamException, InterfaceCallException, LJPaasException, ApiException {
        logger.debug(String.format("begin to debug %s at %s of %s", gson.toJson(optInfo), domainId, platformId));
        Assert.notNull(optInfo, "debug detail can not be null");
        String appName = optInfo.getAppName();
        String alias = optInfo.getAlias();
        AppUpdateOperation operation = optInfo.getOperation();
        Assert.notNull(appName, "appName of debug can not be null");
        Assert.notNull(alias, "alias of debug app can not be null");
        Assert.isTrue(operation==null || operation.equals(AppUpdateOperation.DEBUG), "operation of debug should be debug");
        PlatformPo platform = getK8sPlatform(platformId);
        List<LJHostInfo> bkHostList = this.paasService.queryBKHost(platform.getBkBizId(), null, null, null, null);
        Assert.isTrue(bkHostList.size()>0, String.format("%s has not any host", platform.getPlatformName()));
        Set<Integer> cloudSet = bkHostList.stream().flatMap(h-> Arrays.stream(h.getClouds())).map(c->c.getId()).collect(Collectors.toSet());
        Assert.isTrue(cloudSet.size()==1, String.format("cloudId of %s not unique", platform.getPlatformName()));
        Map<String, PlatformAppDeployDetailVo> deployAppMap = this.platformAppDeployDetailMapper.selectPlatformApps(platformId, domainId, null)
                .stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAlias, Function.identity()));
        if(deployAppMap.containsKey(alias)){
            optInfo.fill(deployAppMap.get(alias));
            if(StringUtils.isBlank(optInfo.getHostIp()))
                optInfo.setHostIp(deployAppMap.get(alias).getHostIp());
        }
        BizSetDefine setDefine = getBizSetForDomainId(domainId);
        String checkResult = checkAppOperationParam(platform.getCcodVersion(), optInfo, setDefine, new ArrayList<>(), bkHostList);
        Assert.isTrue(StringUtils.isBlank(checkResult), checkResult);
        AppModuleVo module = appManagerService.queryAppByVersion(appName, optInfo.getVersion(), true);
        Integer timeout = optInfo.getTimeout() != null && optInfo.getTimeout() > 0 ? optInfo.getTimeout() : module.getTimeout();
        timeout = timeout == null || timeout == 0 ? 150 : timeout;
        optInfo.setTimeout(timeout);
        optInfo.setPlatformId(platformId);
        optInfo.setDomainId(domainId);
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        boolean isNsExist = this.k8sApiService.isNamespaceExist(platformId, k8sApiUrl, k8sAuthToken);
        Assert.isTrue(isNsExist, String.format("namespace %s not exist at %s", platformId, k8sApiUrl));
        debugLock.writeLock().lock();
        try{
            AppDebugTaskVo task = new AppDebugTaskVo(debugTaskId, optInfo);
            debugTaskId++;
            logger.info(String.format("%s task %d : %s added to queue", task.getDebugTag(), task.getId(), gson.toJson(task.getDebugInfo())));
            debugQueue.add(task);
        }
        finally {
            debugLock.writeLock().unlock();
        }
        logger.info(String.format("debug %s is added to queue", gson.toJson(optInfo)));
    }

    private void startAppDebug(PlatformPo platform, AppDebugTaskVo task)
    {
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String jobId = DigestUtils.md5DigestAsHex(sf.format(now).getBytes());
        logger.debug(String.format("start app debug %s with jobId=%s", gson.toJson(task), jobId));
        new Thread(()->{
            try{
                List<K8sOperationInfo> steps = k8sTemplateService.generateDebugPlatformAppSteps(jobId, task.getDebugInfo(), task.getDebugInfo().getDomainId(), task.getDebugInfo().getDomainCfg(), platform, task.getTimeout());
                task.setSteps(steps);
                execAppDebug(jobId, platform, task);
            }
            catch (Exception ex){
                logger.error(String.format("debug exception"), ex);
                List<K8sOperationPo> logs = debugLogsMap.get(task.getDebugTag());
                if(logs != null && logs.size() > 0){
                    logs.get(logs.size()-1).fail(ex.getMessage());
                }
            }
            debugLock.writeLock().lock();
            try{
                debugQueue.removeIf(t->t.getDebugTag().equals(task.getDebugTag()) && t.getStatus() == AppDebugTaskVo.RUNNING);
                long remains = debugQueue.stream().filter(t->t.getPlatformId().equals(platform.getPlatformId())).count();
                if(remains == 0 || remains > 1){
                    logger.info(String.format("%s all debug tasks has finish, change status from %s to %s",
                            platform.getPlatformId(), platform.getStatus().name, platform.getParams().get(PlatformBase.statusBeforeDebugKey)));
                    platform.setStatus(CCODPlatformStatus.getEnum((String)platform.getParams().get(PlatformBase.statusBeforeDebugKey)));
                    platform.getParams().remove(PlatformBase.statusBeforeDebugKey);
                    platformMapper.update(platform);
                }
            }
            finally {
                debugLock.writeLock().unlock();
            }
        }).start();
    }

    private AppDebugTaskVo execAppDebug(String jobId, PlatformPo platform, AppDebugTaskVo task) throws ApiException, ParamException, IOException, InterfaceCallException
    {
        logger.debug(String.format("begin to exec debug %s, index=%d", task.getDebugTag(), task.getId()));
        String platformId = platform.getPlatformId();
        AppUpdateOperationInfo optInfo = task.getDebugInfo();
        List<K8sOperationPo> execResults = task.getExecResults();
        String appName = optInfo.getAppName();
        String alias = optInfo.getAlias();
        String domainId = optInfo.getDomainId();
        List<AppFileNexusInfo> domainCfg = optInfo.getDomainCfg();
        if(domainCfg == null || domainCfg.size() == 0){
            DomainPo domainPo = this.domainMapper.selectByPrimaryKey(platformId, domainId);
            domainCfg = domainPo != null ? domainPo.getCfgs() : new ArrayList<>();
        }
        int startTimeout = optInfo.getTimeout();
        List<K8sOperationInfo> steps = task.getSteps();
        debugLogsMap.put(task.getDebugTag(), task.getExecResults());
        boolean success = false;
        for(K8sOperationInfo step : steps){
            K8sOperationPo execResult = callK8sApi(step, platform.getK8sApiUrl(), platform.getK8sAuthToken());
            boolean changed = false;
            execResults.add(execResult);
            if(step.getKind().equals(K8sKind.DEPLOYMENT) && step.getOperation().equals(K8sOperation.CREATE)){
                execResult.fail("wait deployment change to ACTIVE");
                int timeUsage = 0;
                while(timeUsage < startTimeout){
                    try{
                        Thread.sleep(3000);
                    }
                    catch (Exception ex){
                        ex.printStackTrace();
                    }
                    timeUsage += 3;
                    debugLock.writeLock().lock();
                    try{
                        List<AppDebugTaskVo> tasks = debugQueue.stream().collect(Collectors.groupingBy(AppDebugTaskVo::getDebugTag))
                                .get(task.getDebugTag()).stream().sorted(Comparator.comparing(AppDebugTaskVo::getId).reversed()).collect(Collectors.toList());
                        if(tasks.size() > 1){
                            logger.debug(String.format("%s has %d tasks in queue, change task from %d to %d", task.getDebugTag(), tasks.size(), task.getId(), tasks.get(0).getId()));
                            task = tasks.get(0);
                            for(int i = 1; i <= tasks.size()-1; i++) {
                                logger.debug(String.format("debug %d of %s is removed", task.getId(), task.getDebugTag()));
                                debugQueue.remove(tasks.get(i));
                            }
                            task.setStatus(AppDebugTaskVo.RUNNING);
                            task.setExecTime(new Date());
                            steps = k8sTemplateService.generateDebugPlatformAppSteps(jobId, task.getDebugInfo(), domainId, domainCfg, platform, startTimeout);
                            task.setSteps(steps);
                            changed = true;;
                        }
                    }
                    finally {
                        debugLock.writeLock().unlock();
                    }
                    if(!changed){
                        K8sStatus status = k8sApiService.readNamespacedDeploymentStatus(step.getName(), platformId, platform.getK8sApiUrl(), platform.getK8sAuthToken());
                        logger.debug(String.format("deployment %s status is %s at %s seconds", step.getName(), status.name, timeUsage));
                        if(status.equals(K8sStatus.ACTIVE)){
                            success = true;
                            execResult.setSuccess(true);
                            execResult.setComment(String.format("deployment change to ACTIVE success"));
                            break;
                        }
                    }
                    else{
                        task.setStatus(AppDebugTaskVo.RUNNING);
                        task.setExecTime(new Date());
                        execAppDebug(jobId, platform, task);
                        return task;
                    }
                }
                if(!success && timeUsage > startTimeout){
                    execResult.fail(String.format("start timeout with %s seconds", timeUsage));
                    throw new ParamException(String.format("start %s(%s) timeout with %d seconds", alias, appName, timeUsage));
                }
            }
        }
        logger.info(String.format("debug %s : success", gson.toJson(optInfo)));
        return task;
    }

    private int getIndexFromAlias(String alias, String standAlias)
    {
        String indexStr = alias.replace(standAlias, "");
        int index = StringUtils.isBlank(indexStr) ? 0 : Integer.parseInt(indexStr);
        return index;
    }

    @Override
    public void deleteNamespaceFromK8s(String platformId) throws ApiException {
        PlatformPo platform = getK8sPlatform(platformId);
        boolean nsExist = this.k8sApiService.isNamespaceExist(platformId, platform.getK8sApiUrl(), platform.getK8sAuthToken());
        if(!nsExist){
            logger.warn(String.format("namespace %s not exist", platformId));
        }
        else{

        }


    }

    private List<K8sOperationInfo> generateDomainDeploySteps(
            String jobId, PlatformPo platformPo, DomainUpdatePlanInfo plan, List<PlatformAppDeployDetailVo> domainApps,
            boolean isNewPlatform, V1Deployment glsserver) throws ParamException, ApiException, InterfaceCallException, IOException
    {
        String domainId = plan.getDomainId();
        BizSetDefine setDefine = getBizSetForDomainId(domainId);
        Comparator<AppBase> sort = getAppSort(setDefine);
        List<K8sOperationInfo> steps = new ArrayList<>();
        String platformId = platformPo.getPlatformId();
        String k8sApiUrl = platformPo.getK8sApiUrl();
        String k8sAuthToken = platformPo.getK8sAuthToken();
        Map<String, PlatformAppDeployDetailVo> aliasAppMap = domainApps.stream().collect(Collectors.toMap(o->o.getAlias(), v->v));
        plan.getAppUpdateOperationList().stream().filter(o->o.getOperation().equals(AppUpdateOperation.UPDATE))
                .forEach(o->o.fill(aliasAppMap.get(o.getAlias())));
        List<AppFileNexusInfo> domainCfg = plan.getPublicConfig();
        if(!isNewPlatform && domainCfg != null && domainCfg.size() > 0) {
            if(this.ik8sApiService.isNamespacedConfigMapExist(domainId, platformId, k8sApiUrl, k8sAuthToken)) {
                V1ConfigMap configMap = this.k8sApiService.readNamespacedConfigMap(domainId, platformId, k8sApiUrl, k8sAuthToken);
                K8sOperationInfo optInfo = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP, domainId, K8sOperation.DELETE, configMap);
                steps.add(optInfo);
            }
        }
        if(domainCfg != null && domainCfg.size() > 0) {
            V1ConfigMap configMap = this.k8sApiService.getConfigMapFromNexus(platformId, domainId, domainId,
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
            if(optInfo.getAppName().equals("UCDServer")){
                String glsDomId = glsserver.getMetadata().getLabels().get(domainIdLabel);
                glsserver = gson.fromJson(gson.toJson(glsserver), V1Deployment.class);
                glsserver.getMetadata().getLabels().put("restart-reason", String.format("%s-deployment-created", optInfo.getAlias()));
                K8sOperationInfo info = new K8sOperationInfo(jobId, platformId, glsDomId, K8sKind.DEPLOYMENT, glsserver.getMetadata().getName(), K8sOperation.REPLACE, glsserver);
                info.setTimeout(60);
                steps.add(info);
            }
        }
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

    private void makeupDomainIdAndAliasForSchema(PlatformUpdateSchemaInfo schemaInfo, List<DomainPo> existDomainList, List<PlatformAppDeployDetailVo> deployAppList, boolean clone) throws ParamException {
        if(schemaInfo.getTaskType().equals(PlatformUpdateTaskType.RESTORE) || schemaInfo.getDomainUpdatePlanList() == null || schemaInfo.getDomainUpdatePlanList().size() == 0)
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
        if(updateSchema.getDomainUpdatePlanList() == null || updateSchema.getDomainUpdatePlanList().size() == 0 || updateSchema.getTaskType().equals(PlatformUpdateTaskType.RESTORE))
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
                    String optCheckResult = checkAppOperationParam(ccodVersion, opt, setDefine, new ArrayList<>(), hostList);
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
                        .forEach(opt->sb.append(checkAppOperationParam(ccodVersion, opt, setDefine, domainApps, hostList)));
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

    @Override
    public PlatformUpdateSchemaInfo generateSchemaForScriptDeploy(PlatformCreateParamVo paramVo) {
        logger.debug(String.format("create platform deploy script for %s", gson.toJson(paramVo)));
        Assert.isTrue(StringUtils.isNotBlank(paramVo.getPlatformId()), "platformId can not be blank");
        Assert.isTrue(paramVo.getPlatformId().matches(platformIdRegex), "must consist of lower case alphanumeric characters, '-', and must start with an alphanumeric character (e.g. 'script-test'");
        Assert.isTrue(StringUtils.isNotBlank(paramVo.getK8sHostIp()), "k8sHostIp can not be blank");
        Assert.isTrue(StringUtils.isNotBlank(paramVo.getNfsServerIp()), "nfsServerIp can not be blank");
        Assert.isTrue(StringUtils.isNotBlank(paramVo.getParams()), "params should be id of cloned platform");
        Assert.isTrue(StringUtils.isNotBlank(paramVo.getHostUrl()), "hostUrl can not be blank");
        Assert.isTrue(paramVo.getHostUrl().matches("[a-z0-9]([-a-z0-9]*[a-z0-9])?(\\.[a-z0-9]([-a-z0-9]*[a-z0-9])?)*"), "must consist of lower case alphanumeric characters, '-' or '.', and must start and end with an alphanumeric character (e.g. 'script-test.ccod.com')");
        PlatformPo srcPlatform = getK8sPlatform(paramVo.getParams());
        paramVo.setCcodVersion(srcPlatform.getCcodVersion());
        paramVo.setSchemaId("hahaha");
        paramVo.setCfgs(srcPlatform.getCfgs());
        paramVo.setCreateMethod(PlatformCreateMethod.CLONE);
//        paramVo.setParams(srcPlatform.getParams());
        LJHostInfo host = new LJHostInfo();
        host.setHostInnerIp(paramVo.getK8sHostIp());
        PlatformUpdateSchemaInfo schema = cloneExistPlatform(paramVo, Arrays.asList(new LJHostInfo[]{host}));
        schema.setTaskType(PlatformUpdateTaskType.CREATE);
        schema.setType(PlatformType.K8S_CONTAINER);
        schema.setK8sApiUrl(srcPlatform.getK8sApiUrl());
        schema.setK8sAuthToken(srcPlatform.getK8sAuthToken());
        schema.setSrcPlatformId(paramVo.getParams());
        return schema;
    }

    @Override
    public String generatePlatformCreateScript(PlatformUpdateSchemaInfo schema) throws ParamException, NexusException, NotSupportAppException, InterfaceCallException, LJPaasException, IOException, ApiException {
        logger.debug(String.format("generate platform deploy script for %s", gson.toJson(schema)));
        List<AppModuleVo> registerApps = this.appManagerService.queryAllRegisterAppModule(true);
        LJHostInfo host = new LJHostInfo();
        host.setHostInnerIp(schema.getK8sHostIp());
        List<LJHostInfo> bkHostList = Arrays.asList(new LJHostInfo[]{host});
        String platformCheckResult = checkPlatformUpdateSchema(schema.getCcodVersion(), schema, new ArrayList<>(), new ArrayList<>(), bkHostList, registerApps);
        Assert.isTrue(StringUtils.isBlank(platformCheckResult), platformCheckResult);
        List<K8sOperationInfo> steps = generateExecStepForSchema(schema.getCreatePlatform("test for generate script"), schema, new ArrayList<>(), new ArrayList<>());
        String zipFilePath = generateYamlForDeploy(schema, steps);
        return zipFilePath;
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

    @Override
    public PlatformUpdateSchemaInfo restoreExistK8sPlatform(String platformId) throws ParamException, InterfaceCallException, IOException, ApiException {
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String jobId = DigestUtils.md5DigestAsHex(sf.format(new Date()).getBytes()).substring(0, 10);
        PlatformPo platform = getK8sPlatform(platformId);
        List<DomainPo> domainList = domainMapper.select(platformId, null);
        Map<String, List<PlatformAppDeployDetailVo>> domainAppsMap = platformAppDeployDetailMapper.selectPlatformApps(platformId, null, null)
                .stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        PlatformUpdateSchemaInfo schemaInfo = new PlatformUpdateSchemaInfo(platform, platform.getParams(), PlatformUpdateTaskType.RESTORE, UpdateStatus.EXEC, String.format("%s还原规划", platform.getPlatformName()), String.format("从cmdb还原出%s平台", platform.getPlatformName()));
        schemaInfo.setSchemaId(jobId);
        schemaInfo.setCreateMethod(PlatformCreateMethod.RESTORE_FROM_CMDB);
        for(DomainPo domain : domainList){
            List<PlatformAppDeployDetailVo> deployApps = domainAppsMap.get(domain.getDomainId());
            if(deployApps == null){
                logger.warn(String.format("domain %s(%s) is empty", domain.getDomainName(), domain.getDomainId()));
                continue;
            }
            DomainUpdatePlanInfo planInfo = generateCloneExistDomain(domain);
            planInfo.setAppUpdateOperationList(deployApps.stream().map(d->d.getOperationInfo(AppUpdateOperation.ADD)).collect(Collectors.toList()));
            planInfo.setStatus(UpdateStatus.EXEC);
            schemaInfo.getDomainUpdatePlanList().add(planInfo);
        }
        return schemaInfo;
    }

    @Override
    public boolean isPlatformDeployOngoing(String platformId) {
        if(this.isPlatformCheckOngoing && StringUtils.isNotBlank(this.ongoingPlatformId) && this.ongoingPlatformId.equals(platformId))
            return true;
        return false;
    }

    @Override
    public List<K8sOperationPo> getPlatformDeployLogs() {
        return this.platformDeployLogs;
    }

    @Override
    public List<K8sOperationPo> getAppDebugLogs(String platformId, String domainId, String appName, String alias) {
        List<K8sOperationPo> logs = debugLogsMap.get(String.format("%s(%s) at %s(%s)", alias, appName, domainId, platformId));
        Assert.notNull(logs, String.format("%s(%s) at %s(%s) has not any debug log", alias, appName, domainId, platformId));
        return logs;
    }

    @Override
    public PlatformDeployStatus getLastPlatformDeployTaskStatus() {
        if(this.isPlatformCheckOngoing)
            return PlatformDeployStatus.DEPLOYING;
        else if(this.platformDeployLogs.size() == 0)
            return PlatformDeployStatus.NOT_EXEC;
        K8sOperationPo last = this.platformDeployLogs.get(this.platformDeployLogs.size() - 1);
        if(last.isSuccess())
            return PlatformDeployStatus.SUCCESS;
        return PlatformDeployStatus.FAIL;
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
            PlatformCreateParamVo paramVo, List<LJHostInfo> hostList){
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
        return this.k8sApiService.readNamespacedConfigMap(configMapName, platformId, platformPo.getK8sApiUrl(), platformPo.getK8sAuthToken());
    }

    private PlatformPo getK8sPlatform(String platformId){
        PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
        Assert.notNull(platformPo, String.format("%s platform not exit", platformId));
        Assert.isTrue(PlatformType.K8S_CONTAINER.equals(platformPo.getType()), String.format("platform %s type is %s not %s", platformId, platformPo.getType().name, PlatformType.K8S_CONTAINER.name));
        Assert.isTrue(StringUtils.isNotBlank(platformPo.getK8sApiUrl()), String.format("k8s api url of %s is blank", platformId));
        Assert.isTrue(StringUtils.isNotBlank(platformPo.getK8sAuthToken()), String.format("k8s auth token of %s is blank", platformId));
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

    private K8sOperationPo callK8sApi(K8sOperationInfo optInfo, String k8sApiUrl, String k8sAuthToken) throws ApiException
    {
        Object retVal = null;
        String platformId = optInfo.getPlatformId();
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
                        case REPLACE:
                            this.k8sApiService.replacePersistentVolume(optInfo.getName(), (V1PersistentVolume)optInfo.getObj(), k8sApiUrl, k8sAuthToken);
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
        catch (ApiException e) {
            throw e;
        }
        catch (Exception ex)
        {
            logger.error(String.format("exec %s exception", gson.toJson(optInfo)), ex);
            execResult.fail(ex.getMessage());
        }
        return execResult;
    }

    private K8sOperationPo execK8sOpt(List<K8sOperationPo> execResults, K8sOperationInfo optInfo, String k8sApiUrl, String k8sAuthToken) throws ApiException, ParamException
    {
        K8sOperationPo execResult = callK8sApi(optInfo, k8sApiUrl, k8sAuthToken);
        execResults.add(execResult);
        if(execResult.isSuccess() && optInfo.getKind().equals(K8sKind.DEPLOYMENT) && optInfo.getTimeout() > 0){
            int timeUsage = 0;
            boolean isActive = false;
            while(timeUsage <= optInfo.getTimeout())
            {
                try{
                    Thread.sleep(3000);
                }
                catch (Exception ex){
                    logger.error("sleep exception", ex);
                }
                logger.debug(String.format("wait deployment %s status to ACTIVE, timeUsage=%d", optInfo.getName(), (timeUsage+3)));
                K8sStatus status = this.k8sApiService.readNamespacedDeploymentStatus(optInfo.getName(), optInfo.getPlatformId(), k8sApiUrl, k8sAuthToken);
                if(status.equals(K8sStatus.ACTIVE))
                {
                    logger.debug(String.format("deployment %s status change to ACTIVE in %d seconds", optInfo.getName(), timeUsage));
                    isActive = true;
                    break;
                }
                timeUsage += 3;
            }
            if(!isActive){
                logger.error(String.format("deployment %s not change to ACTIVE in %s seconds", optInfo.getName(), timeUsage));
                execResult.fail(String.format("deployment %s not change to ACTIVE in %s seconds", optInfo.getName(), timeUsage));
            }
            if(optInfo.isKernal() && !execResult.isSuccess()){
                throw new ParamException(execResult.getComment());
            }
        }
        return execResult;
    }

    private PlatformSchemaExecResultVo execPlatformUpdateSteps(PlatformPo platformPo, List<K8sOperationInfo> k8sOptList, List<K8sOperationPo> execResults, PlatformUpdateSchemaInfo schema, List<PlatformAppDeployDetailVo> platformApps) throws ParamException, ApiException, IOException
    {
        String jobId = schema.getSchemaId();
        Date startTime = new Date();
        String platformId = platformPo.getPlatformId();
        List<PlatformUpdateRecordPo> lastRecords = this.platformUpdateRecordMapper.select(platformId, true);
        boolean isNewPlatform = schema.getTaskType().equals(PlatformUpdateTaskType.CREATE) || schema.getTaskType().equals(PlatformUpdateTaskType.RESTORE) ? true : false;
        if(isNewPlatform){
//            String workDir = String.format("/home/kubernetes/volume/%s/base-volume", platformPo.getPlatformId());
//            String rep = StringUtils.isNotBlank(platformPo.getBaseDataNexusRepository()) ? platformPo.getBaseDataNexusRepository() : (String)platformPo.getParams().get(PlatformBase.baseDataNexusRepositoryKey);
//            String path = StringUtils.isNotBlank(platformPo.getBaseDataNexusPath()) ? platformPo.getBaseDataNexusPath() : (String)platformPo.getParams().get(PlatformBase.baseDataNexusPathKey);
//            StringBuffer command = new StringBuffer();
//            command.append(String.format("rm -rf %s;mkdir %s -p;cd %s;", workDir, workDir, workDir));
//            command.append(String.format("wget %s/repository/%s/%s;", nexusHostUrl, rep, path));
//            command.append(String.format("tar -xvzf %s", path.replaceAll(".*/", "")));
//            logger.debug(String.format("init command=%s", command.toString()));
//            System.out.println(command.toString());
//            Runtime runtime = Runtime.getRuntime();
//            logger.debug(String.format("begin to exec %s", command));
//            runtime.exec(command.toString());
//            logger.debug("exec command success");
        }
        PlatformSchemaExecResultVo execResultVo = new PlatformSchemaExecResultVo(jobId, platformId, k8sOptList);
        execK8sDeploySteps(platformPo, k8sOptList, execResults);
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
//        logger.debug(String.format("platform deploy record=%s", gson.toJson(recordPo)));
        this.platformUpdateRecordMapper.insert(recordPo);
        for(PlatformUpdateRecordPo po : lastRecords) {
            po.setLast(false);
            this.platformUpdateRecordMapper.update(po);
        }
        return execResultVo;
    }

    private void execK8sDeploySteps(PlatformPo platform, List<K8sOperationInfo> k8sOptList, List<K8sOperationPo> execResults) throws ApiException, ParamException
    {
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        Map<String, Object> params = platform.getParams();
        DatabaseType glsDBType = DatabaseType.getEnum((String)platform.getParams().get(PlatformBase.glsDBTypeKey));
        String glsDBService = (String)params.get(PlatformBase.glsDBServiceKey);
        for(K8sOperationInfo k8sOpt : k8sOptList)
        {
            K8sOperationPo ret = execK8sOpt(execResults, k8sOpt, k8sApiUrl, k8sAuthToken);
            try
            {
                String platformId = k8sOpt.getPlatformId();
                if(k8sOpt.getKind().equals(K8sKind.DEPLOYMENT) && k8sOpt.getOperation().equals(K8sOperation.CREATE))
                {
                    V1Deployment deploy = gson.fromJson(ret.getRetJson(), V1Deployment.class);
                    Map<String, String> labels = deploy.getMetadata().getLabels();
                    if(labels == null || labels.size() == 0 || !labels.containsKey(this.serviceTypeLabel) || !labels.containsKey(this.appNameLabel))
                        continue;
                    if(labels.get(this.serviceTypeLabel).equals(K8sServiceType.THREE_PART_APP.name))
                    {
                        if(k8sOpt.getName().equals(glsDBService))
                        {
                            V1Service dbSvc = this.k8sApiService.readNamespacedService(deploy.getMetadata().getName(), platformId, k8sApiUrl, k8sAuthToken);
                            int dbPort = getNodePortFromK8sService(dbSvc);
                            boolean isConn = databaseConnectTest(glsDBType, (String)params.get(PlatformBase.glsDBUserKey), (String)params.get(PlatformBase.glsDBPwdKey), (String)params.get(PlatformBase.k8sHostIpKey), dbPort, (String)params.get(PlatformBase.glsDBSidKey), 240);
                            if(!isConn)
                                throw new ApiException("create service for glsDB fail");
                            params.put(PlatformBase.dbPortKey, dbPort);
                        }
                    }
                    else if(labels.get(this.serviceTypeLabel).equals(K8sServiceType.DOMAIN_OUT_SERVICE.name) && labels.get(this.appNameLabel).equals("UCDServer"))
                    {
                        Connection connect = createDBConnection(DatabaseType.getEnum((String)platform.getParams().get(PlatformBase.glsDBTypeKey)), (String)params.get(PlatformBase.glsDBUserKey),
                                (String)params.get(PlatformBase.glsDBPwdKey), (String)params.get(PlatformBase.k8sHostIpKey),
                                (int)params.get(PlatformBase.dbPortKey), (String)params.get(PlatformBase.glsDBSidKey));
                        V1Service ucdsOutService = this.k8sApiService.readNamespacedService(String.format("%s-out", deploy.getMetadata().getName()), platformId, k8sApiUrl, k8sAuthToken);
                        int ucdsPort = getNodePortFromK8sService(ucdsOutService);
                        String updateSql = String.format("update \"CCOD\".\"GLS_SERVICE_UNIT\" set PARAM_UCDS_PORT=%d where NAME='ucds-cloud01'", ucdsPort);
                        PreparedStatement ps = connect.prepareStatement(updateSql);
                        logger.debug(String.format("begin to update ucds port : %s", updateSql));
                        ps.executeUpdate();
//                        Map<String, String> selector = new HashMap<>();
//                        selector.put(this.appTypeLabel, AppType.BINARY_FILE.name);
//                        selector.put("glsServer", deployGls.getAlias());
//                        List<V1Deployment> deploys = this.k8sApiService.selectNamespacedDeployment(platformId, selector, k8sApiUrl, k8sAuthToken);
//                        if(deploys.size() == 0)
//                            throw new ParamException(String.format("can not find glsserver from %s", platformId));
//                        else if(deploys.size() > 1)
//                            throw new ParamException(String.format("glsserver multi deploy at %s ", platformId));
//                        V1Deployment glsserver = deploys.get(0);
//                        glsserver = templateParseGson.fromJson(templateParseGson.toJson(glsserver), V1Deployment.class);
//                        Date now = new Date();
//                        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
//                        glsserver.getMetadata().getLabels().put("restart-time", sf.format(now));
//                        this.k8sApiService.replaceNamespacedDeployment(glsserver.getMetadata().getName(), platformId, glsserver, k8sApiUrl, k8sAuthToken);
//                        int timeUsage = 0;
//                        String glsserverName = glsserver.getMetadata().getName();
//                        while(timeUsage <= 60)
//                        {
//                            Thread.sleep(3000);
//                            logger.debug(String.format("wait deployment %s status to ACTIVE, timeUsage=%d", glsserverName, (timeUsage+3)));
//                            K8sStatus status = this.k8sApiService.readNamespacedDeploymentStatus(glsserverName, platformId, k8sApiUrl, k8sAuthToken);
//                            if(status.equals(K8sStatus.ACTIVE))
//                            {
//                                logger.debug(String.format("deployment %s status change to ACTIVE, timeUsage=%d", glsserverName, (timeUsage+3)));
//                                break;
//                            }
//                            timeUsage += 3;
//                        }
//                        if(timeUsage > 60)
//                            throw new ParamException(String.format("restart deployment %s timeout in %d seconds", glsserverName, timeUsage));
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
    }

    @Override
    public void deployPlatformByHostScript(PlatformUpdateSchemaInfo schema) throws NexusException, InterfaceCallException, IOException, ParamException {
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String fileSaveDir = String.format("%s/temp/script/%s/%s", System.getProperty("user.dir"), schema.getPlatformId(), sf.format(now));
        List<String> scriptZipList = generatePythonScriptForPlatformDeploy(schema, fileSaveDir);
        logger.info(String.format("generate zip is %s", gson.toJson(scriptZipList)));
    }

    private List<String> generatePythonScriptForPlatformDeploy(PlatformUpdateSchemaInfo schema, String fileSaveDir) throws ParamException, InterfaceCallException, NexusException, IOException
    {
        Map<String, String> hosts = schema.getDomainUpdatePlanList().stream().flatMap(p->p.getAppUpdateOperationList().stream())
                .collect(Collectors.groupingBy(AppUpdateOperationInfo::getHostIp, Collectors.mapping(AppUpdateOperationInfo::getDeployName, Collectors.joining(" "))));
        Map<String, Object> platformParams = new HashMap<>();
        platformParams.put("hosts", hosts);
        platformParams.put("hostname", schema.getHostUrl());
        Comparator<DomainUpdatePlanInfo> sort = getDomainPlanSort();
        List<String> scriptList = new ArrayList<>();
        List<DomainUpdatePlanInfo> plans = schema.getDomainUpdatePlanList().stream().
                sorted(sort).collect(Collectors.toList());
        for(DomainUpdatePlanInfo plan : plans){
            String domainId = plan.getDomainId();
            BizSetDefine setDefine = getBizSetForDomainId(domainId);
            Comparator<AppBase> appSort = getAppSort(setDefine);
            List<AppUpdateOperationInfo> optList = plan.getAppUpdateOperationList().stream().sorted(appSort).collect(Collectors.toList());
            for(AppUpdateOperationInfo o : optList){
                o.setDomainId(plan.getDomainId());
                o.setPlatformId(schema.getPlatformId());
                AppModuleVo module = appManagerService.queryAppByVersion(o.getAppName(), o.getVersion(), true);
                o.fill(module);
            }
            Map<String, List<AppUpdateOperationInfo>> hostAppMap = optList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getHostIp));
            for(String ip : hostAppMap.keySet()){
                String scriptPath = generateScriptForHostDeploy(ip, plan.getDomainId(), hostAppMap.get(ip), fileSaveDir, platformParams);
                scriptList.add(scriptPath);
            }
        }
        return scriptList;
    }

    private String generateScriptForHostDeploy(String hostIp, String domainId, List<AppUpdateOperationInfo> optList, String fileSaveDir, Map<String, Object> platformParams) throws NexusException, InterfaceCallException, IOException
    {
        String saveDir = String.format("%s/%s", fileSaveDir, hostIp);
        Map<String, Object> params = new HashMap<>();
        for(AppUpdateOperationInfo optInfo : optList){
            nexusService.downloadFile(nexusUserName, nexusPassword, optInfo.getInstallPackage().getFileNexusDownloadUrl(nexusHostUrl),
                    String.format("%s/%s/package", saveDir, optInfo.getAlias()), optInfo.getInstallPackage().getFileName());
            for(AppFileNexusInfo cfg : optInfo.getCfgs()){
                nexusService.downloadFile(nexusUserName, nexusPassword, cfg.getFileNexusDownloadUrl(nexusHostUrl),
                        String.format("%s/%s/cfg", saveDir, optInfo.getAlias()), cfg.getFileName());
            }
        }
        params.put("deploySteps", optList);
        platformParams.forEach((k,v)->params.put(k,v));
        FileUtils.saveContextToFile(saveDir, "deploy_param.txt", gson.toJson(params), true);
        copySourceFile(this.hostScriptDeployCfgFileName, saveDir, this.platformDeployCfgFileName);
        copySourceFile(this.hostDeployScriptFileName, saveDir, this.platformDeployScriptFileName);
        String zipFilePath = String.format("%s/%s@%s.zip", fileSaveDir, domainId, hostIp.replaceAll("\\.", "-"));
        File zipFile = new File(zipFilePath);
        if(zipFile.exists()){
            zipFile.delete();
        }
        ZipUtils.zipFolder(saveDir, zipFilePath);
        logger.debug(String.format("generated script for host deploy has saved to %s", zipFilePath));
        return zipFilePath;
    }

    /**
     * 生成平台部署yaml
     * @param schema 平台规划
     * @param steps 平台创建步骤
     * @return 生成的平台部署yaml的zip包地址
     * @throws IOException
     */
    private String generateYamlForDeploy(PlatformUpdateSchemaInfo schema, List<K8sOperationInfo> steps) throws IOException
    {
        String platformId = schema.getPlatformId();
        StringBuffer sb = new StringBuffer();
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String rootPath = String.format("%s/temp/yaml/%s", System.getProperty("user.dir"), platformId);
        String basePath = String.format("%s/%s", rootPath, sf.format(now)).replaceAll("\\\\", "/");
        List<Map<String, Object>> execList = new ArrayList<>();
        List<Map<String, Object>> baseExecList = new ArrayList<>();
        for(K8sOperationInfo step : steps){
            if(step.getKind().equals(K8sKind.JOB) || step.getKind().equals(K8sKind.SECRET))
                continue;
            if(step.getKind().equals(K8sKind.JOB))
                step.setTimeout(20);
            StringBuffer content = new StringBuffer();
            String alias = step.getName().split("-")[0];
            String domainId = step.getDomainId();
            boolean isBase = step.getPlatformId().equals(String.format("base-%s", schema.getPlatformId())) ? true : false;
            String subDir = domainId == null ? String.format("%s", isBase ? "base":"") : String.format("%s/%s", domainId, alias);
            String saveDir = String.format("%s/%s", basePath, subDir).replaceAll("/$", "");
            String fileName = String.format("%s-%s-%s.%s", step.getName(), step.getKind().name.toLowerCase(), step.getOperation().name.toLowerCase(), !step.getKind().equals(K8sKind.CONFIGMAP) ? "yaml" : "json");
            String tag = String.format("# create %s", step.getKind().name.toLowerCase());
            content.append(tag).append("\n---\n").append(Yaml.dump(step.getObj())).append("\n\n");
            String yamlContent = !step.getKind().equals(K8sKind.CONFIGMAP) ? content.toString() : gson.toJson(step.getObj());
            FileUtils.saveContextToFile(saveDir, fileName, yamlContent, true);
            sb.append(content.toString());
            Map<String, Object> param = new HashMap<>();
            param.put("timeout", step.getTimeout());
            param.put("filePath", String.format("%s/%s", subDir, fileName).replaceAll("//", "/").replaceAll("^/", ""));
            param.put("kind", step.getKind().name);
            param.put("operation", step.getOperation());
            if(isBase){
                baseExecList.add(param);
            }
            else{
                execList.add(param);
            }
        }
        Map<String, Object> params = new HashMap<>();
        params.put("execSteps", execList);
        params.put("baseExecSteps", baseExecList);
        Map<String, Object> platParams = schema.getPlatformParam();
        platParams.put("platformId", schema.getPlatformId());
        String glsDBName = ((String)platParams.get(PlatformBase.glsDBTypeKey)).equals("ORACLE") ? "CCOD" : "ucds";
        if(!platParams.containsKey("glsDBName")){
            platParams.put("glsDBName", glsDBName);
        }
        params.put("platformParams", platParams);
        Set<String> images = new HashSet<>();
        steps.stream().filter(s->s.getKind().equals(K8sKind.DEPLOYMENT)).forEach(s->{
           V1Deployment deploy = (V1Deployment)s.getObj();
           if(deploy.getSpec().getTemplate().getSpec().getInitContainers() != null)
               deploy.getSpec().getTemplate().getSpec().getInitContainers().forEach(c->{
                   images.add(c.getImage());
               });
           if(deploy.getSpec().getTemplate().getSpec().getContainers() != null)
               deploy.getSpec().getTemplate().getSpec().getContainers().forEach(c->{
                   images.add(c.getImage());
               });
        });
        steps.stream().filter(s->s.getKind().equals(K8sKind.JOB)).forEach(s->{
            V1Job job = (V1Job)s.getObj();
            if(job.getSpec().getTemplate().getSpec().getContainers() != null)
                job.getSpec().getTemplate().getSpec().getContainers().forEach(c->{
                    images.add(c.getImage());
                });
        });
        params.put("images", new ArrayList<>(images));
        params.put("configCenterData", schema.getConfigCenterData() == null ? new HashMap<>() : schema.getConfigCenterData());
        FileUtils.saveContextToFile(basePath, "start_param.txt", gson.toJson(params), true);
        FileUtils.saveContextToFile(basePath, "platform_create.yaml", sb.toString(), true);
        copySourceFile(this.platformDeployScriptFileName, basePath, this.platformDeployScriptFileName);
        if(((String)platParams.get(PlatformBase.glsDBTypeKey)).equals("ORACLE")){
            copySourceFile(this.platformDeployCfgFileName, basePath, this.platformDeployCfgFileName);
        }
        else{
            copySourceFile(this.platformDeployMysqlCfgFileName, basePath, this.platformDeployCfgFileName);
        }

        String zipFilePath = String.format("%s/%s.zip", rootPath, platformId);
        File zipFile = new File(zipFilePath);
        if(zipFile.exists()){
            zipFile.delete();
        }
        ZipUtils.zipFolder(basePath, zipFilePath);
        logger.debug(String.format("generated script for deploy platform %s has saved to %s", schema.getPlatformId(), zipFilePath));
        return zipFilePath;
    }

    private void generateYamlForDeploy(List<K8sOperationInfo> steps, String name, String basePath, Map<String, Object> deployParams) throws IOException{
        StringBuffer sb = new StringBuffer();
        List<Map<String, Object>> execList = new ArrayList<>();
        for(K8sOperationInfo step : steps){
            if(step.getKind().equals(K8sKind.JOB))
                continue;
            if(step.getKind().equals(K8sKind.JOB))
                step.setTimeout(20);
            StringBuffer content = new StringBuffer();
            String alias = step.getName().split("-")[0];
            String domainId = step.getDomainId();
            String saveDir = domainId == null ? basePath : String.format("%s/%s/%s", basePath, domainId, alias);
            String fileName = String.format("%s-%s-%s.yaml", step.getName(), step.getKind().name.toLowerCase(), step.getOperation().name.toLowerCase());
            String tag = String.format("# create %s", step.getKind().name.toLowerCase());
            content.append(tag).append("\n---\n").append(Yaml.dump(step.getObj())).append("\n\n");
            FileUtils.saveContextToFile(saveDir, fileName, content.toString(), true);
            sb.append(content.toString());
            Map<String, Object> param = new HashMap<>();
            param.put("timeout", step.getTimeout());
            param.put("filePath", domainId == null ? fileName : String.format("%s/%s/%s", domainId, alias, fileName));
            execList.add(param);
        }
        Set<String> images = deployParams.containsKey("images") ? new HashSet<>((List<String>)deployParams.get("images")) : new HashSet<>();
        steps.stream().filter(s->s.getKind().equals(K8sKind.DEPLOYMENT)).forEach(s->{
            V1Deployment deploy = (V1Deployment)s.getObj();
            if(deploy.getSpec().getTemplate().getSpec().getInitContainers() != null)
                deploy.getSpec().getTemplate().getSpec().getInitContainers().forEach(c->{
                    images.add(c.getImage());
                });
            if(deploy.getSpec().getTemplate().getSpec().getContainers() != null)
                deploy.getSpec().getTemplate().getSpec().getContainers().forEach(c->{
                    images.add(c.getImage());
                });
        });
        steps.stream().filter(s->s.getKind().equals(K8sKind.JOB)).forEach(s->{
            V1Job job = (V1Job)s.getObj();
            if(job.getSpec().getTemplate().getSpec().getContainers() != null)
                job.getSpec().getTemplate().getSpec().getContainers().forEach(c->{
                    images.add(c.getImage());
                });
        });
        deployParams.put(String.format("%s-steps", name), steps);
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
        Assert.isTrue(!this.isPlatformCheckOngoing, "some platform deploy, collect or debug is ongoing");
        this.ongoingPlatformId = platformId;
        this.isPlatformCheckOngoing = true;
        this.platformDeployLogs.clear();
        try
        {
            for(K8sOperationInfo optInfo : optList)
            {
                K8sOperationPo execResult = execK8sOpt(this.platformDeployLogs, optInfo, platform.getK8sApiUrl(), platform.getK8sAuthToken());
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
        finally {
            this.ongoingPlatformId = null;
            this.isPlatformCheckOngoing = false;
        }
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

    private Connection createDBConnection(DatabaseType dbType, String user, String pwd, String ip, int port, String dbName) throws ClassNotFoundException, SQLException, ParamException {
        String connStr;
        switch (dbType){
            case ORACLE:
                connStr = String.format("jdbc:oracle:thin:@%s:%s:%s", ip, port, dbName);
                Class.forName("oracle.jdbc.driver.OracleDriver");
                break;
            case MYSQL:
                connStr = String.format("jdbc:mysql://%s:%s/%s", ip, port, dbName);
                Class.forName("com.mysql.jdbc.Driver");
                break;
            default:
                throw new ParamException(String.format("unsupported dbType %s", dbType.name));

        }
        Connection conn = DriverManager.getConnection(connStr, user, pwd);
        logger.debug(String.format("conn %s success", connStr));
        return conn;
    }

    private Connection createDBConnection(DatabaseType dbType, String user, String pwd, String dbService, String dbName) throws ClassNotFoundException, SQLException, ParamException {
        String connStr;
        switch (dbType){
            case ORACLE:
                connStr = String.format("jdbc:oracle:thin:@%s:%s", dbService, dbName);
                Class.forName("oracle.jdbc.driver.OracleDriver");
                break;
            case MYSQL:
                connStr = String.format("jdbc:mysql://%s/%s", dbService, dbName);
                Class.forName("com.mysql.jdbc.Driver");
                break;
            default:
                throw new ParamException(String.format("unsupported dbType %s", dbType.name));

        }
        Connection conn = DriverManager.getConnection(connStr, user, pwd);
        logger.debug(String.format("conn %s success", connStr));
        return conn;
    }

    private boolean databaseConnectTest(DatabaseType dbType, String user, String pwd, String ip, int port, String sid, int timeout) {
        int count = 1;
        while (count < (timeout / 10)) {
            System.out.println("");
            try {
                logger.debug(String.format("try connect oracle"));
                Connection conn = createDBConnection(dbType, user, pwd, ip, port, sid);
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

    @Test
    public void platformTest()
    {
        try{
            dbTest();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void dbTest() throws Exception
    {

        Connection conn = createDBConnection(DatabaseType.MYSQL, "root", "qnsoft", "10.130.41.88", 3306, "cmdb_shop");
        System.out.println("Ok");
    }

}