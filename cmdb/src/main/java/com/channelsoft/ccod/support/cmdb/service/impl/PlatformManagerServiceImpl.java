package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.config.*;
import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.dao.*;
import com.channelsoft.ccod.support.cmdb.exception.*;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.service.*;
import com.channelsoft.ccod.support.cmdb.utils.HttpRequestTools;
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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
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
    PlatformAppCfgFileMapper platformAppCfgFileMapper;

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
    AppCfgFileMapper appCfgFileMapper;

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

    private final Map<String, PlatformUpdateSchemaInfo> platformUpdateSchemaMap = new ConcurrentHashMap<>();

    private Map<String, List<BizSetDefine>> appSetRelationMap;

    private Map<String, BizSetDefine> setDefineMap;

    protected final ReentrantReadWriteLock appWriteLock = new ReentrantReadWriteLock();

    private boolean isPlatformCheckOngoing = false;

    private String domainIdRegexFmt = "^%s(0[1-9]|[1-9]\\d+)";

    private String aliasRegexFmt = "^%s\\d*$";

    private String domainServiceNameFmt = "^%s-%s($|(-[0-9a-z]+)+$)";

    protected String testK8sApiUrl = "https://10.130.41.218:6443";

    protected String testAuthToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA";

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
            this.k8sOperationMapper.select(null, null, null);
//            portTest();
//            V1Namespace ns = ik8sApiService.queryNamespace(namespace, k8sApiUrl, authToken);
//            System.out.println(JSONObject.toJSONString(ns));
//            List<V1Namespace> nsList = ik8sApiService.queryAllNamespace(k8sApiUrl, authToken);
//            System.out.println(String.format("find %d namespace from %s", nsList.size(), k8sApiUrl));
//            List<V1Pod> podList = ik8sApiService.queryAllPodAtNamespace(namespace, k8sApiUrl, authToken);
//            System.out.println(String.format("find %d pod at namespace %s from %s", podList.size(), namespace, k8sApiUrl));
//            V1Pod pod = ik8sApiService.queryPod(namespace, podList.get(0).getMetadata().getName(), k8sApiUrl, authToken);
//            System.out.println(JSONObject.toJSONString(pod.getMetadata().getName()));
//            getPlatformTopologyFromK8s("工具组平台", "202005-test", 34, 0, "CCOD4.1", k8sApiUrl, authToken);
//            someTest();
//            configMapTest();
//            generateDemoCreateSchema();
            String platformName = "ccod开发测试平台";
            String platformId = "123456-wuph";
            int bkBizId = 25;
            int bkCloudId = 0;
            String ccodVersion = "4.1";
//            getPlatformTopologyFromK8s(platformName, platformId, bkBizId, bkCloudId, ccodVersion, k8sApiUrl, authToken, PlatformFunction.TEST);
//            generateDemoCreateSchema();
//            String json = "{\"schemaId\":\"12345666778991\",\"platformId\":\"123456-wuph\",\"platformName\":\"工具组平台\",\"bkBizId\":34,\"bkCloudId\":10,\"ccodVersion\":\"CCOD4.1\",\"taskType\":\"CREATE\",\"title\":\"新建工具组平台(proto-platform)计划\",\"comment\":\"create 工具组平台(proto-platform) by clone pahj(平安环境公司容器化测试)\",\"deployScriptRepository\":\"some_test\",\"deployScriptPath\":\"proto-platform/20200526123032/platform_deploy.py\",\"deployScriptMd5\":\"5843a22c7476403f234069af0af16e57\",\"k8sHostIp\":\"10.130.41.218\",\"glsDBType\":\"ORACLE\",\"glsDBUser\":\"ccod\",\"glsDBPwd\":\"ccod\",\"baseDataNexusRepository\":\"platform_base_data\",\"baseDataNexusPath\":\"ccod/4.1/baseVolume.zip\",\"publicConfig\":[{\"fileName\":\"local_datasource.xml\",\"fileSize\":0,\"md5\":\"e9c26f00f17a7660bfa3f785c4fe34be\",\"deployPath\":\"/root/resin-             4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/123456-             wuph/publicConfig/local_datasource.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFhN2U5NGFhNDVlZTIxN2Nm\"},{\"fileName\":\"local_jvm.xml\",\"fileSize\":0,\"md5\":\"d41d8cd98f00b204e9800998ecf8427e\",\"deployPath\":\"/root/resin-             4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/123456-             wuph/publicConfig/local_jvm.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNkODFhZTYxM2NmNDM3NmQ3\"},{\"fileName\":\"tnsnames.ora\",\"fileSize\":0,\"md5\":\"d41d8cd98f00b204e9800998ecf8427e\",\"deployPath\":\"/usr/local/lib\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/123456-             wuph/publicConfig/tnsnames.ora\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQzMDcyY2Q2ZTEyNzg2NTQ3\"}],\"k8sApiUrl\":\"https://10.130.41.218:6443\",\"k8sAuthToken\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1     YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50I     iwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsI     mt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc     2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc     2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVyb     mV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0Z     TYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprd     WJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4fr     qzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-     9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-     LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-     mndngivX0G1aucrK-     RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-     LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA\",\"k8sDeploymentList\":[{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"cas\":\"cas1\"},\"name\":\"cas1-manage01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"cas\":\"cas1\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"cas\":\"cas1\"}},\"spec\":{\"containers\":[{\"command\":[\"/bin/sh\",\"-c\",\"keytool -import -v -trustcacerts -noprompt -                         storepass changeit -alias test -file /ssl/tls.crt -keystore                         $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh; tail -F                         /usr/local/tomcat/logs/catalina.out\"],\"image\":\"nexus.io:5000/ccod-                         base/tomcat6-jre7:1\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/cas\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"cas1-manage01\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"}]}],\"hostAliases\":[{\"hostnames\":[\"123456-wuph.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;wget http://10.130.41.218:8081/repository/tmp/configText/123456-             wuph/manage01_cas/cas.properties;mkdir -p WEB-INF;mv cas.properties WEB-             INF/;jar uf cas.war WEB-INF/cas.properties;wget             http://10.130.41.218:8081/repository/tmp/configText/123456-             wuph/manage01_cas/web.xml;mkdir -p WEB-INF;mv web.xml WEB-INF/;jar uf             cas.war WEB-INF/web.xml;mv /opt/cas.war /war;cd /opt;jar uf cas.war             /home/portal/tomcat/webapps/cas/WEB-INF//cas.properties;jar uf cas.war             /home/portal/tomcat/webapps/cas/WEB-INF//web.xml\"],\"image\":\"nexus.io:5000/ccod/cas:10973\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"cas1\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/home/portal/tomcat/webapps/cas/WEB-INF/\",\"name\":\"cas1-manage01-volume\"},{\"mountPath\":\"/opt/WEB-INF\",\"name\":\"123456-wuph-volume\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"hostPath\":{\"path\":\"/var/ccod-             runtime/123456-wuph/cas-manage01/cas-manage01/\",\"type\":\"\"},\"name\":\"ccod-             runtime\"},{\"configMap\":{\"items\":[{\"key\":\"cas.properties\",\"path\":\"cas.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"cas1-manage01\"},\"name\":\"cas1-manage01-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"123456-wuph\"},\"name\":\"123456-wuph-     volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"cas\":\"cas2\"},\"name\":\"cas2-manage01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"cas\":\"cas2\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"cas\":\"cas2\"}},\"spec\":{\"containers\":[{\"command\":[\"/bin/sh\",\"-c\",\"keytool -import -v -trustcacerts -noprompt -                         storepass changeit -alias test -file /ssl/tls.crt -keystore                         $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh; tail -F                         /usr/local/tomcat/logs/catalina.out\"],\"image\":\"nexus.io:5000/ccod-                         base/tomcat6-jre7:1\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/cas\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"cas-manage01\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"}]}],\"hostAliases\":[{\"hostnames\":[\"123456-wuph.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;wget http://10.130.41.218:8081/repository/tmp/configText/123456-             wuph/manage01_cas/cas.properties;mkdir -p WEB-INF;mv cas.properties WEB-             INF/;jar uf cas.war WEB-INF/cas.properties;wget             http://10.130.41.218:8081/repository/tmp/configText/123456-             wuph/manage01_cas/web.xml;mkdir -p WEB-INF;mv web.xml WEB-INF/;jar uf             cas.war WEB-INF/web.xml;mv /opt/cas.war /war;cd /opt;jar uf cas.war             /home/portal/tomcat/webapps/cas/WEB-INF//cas.properties;jar uf cas.war             /home/portal/tomcat/webapps/cas/WEB-INF//web.xml\"],\"image\":\"nexus.io:5000/ccod/cas:10973\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"cas2\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/home/portal/tomcat/webapps/cas/WEB-INF/\",\"name\":\"cas2-manage01-volume\"},{\"mountPath\":\"/opt/WEB-INF\",\"name\":\"123456-wuph-volume\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"hostPath\":{\"path\":\"/var/ccod-             runtime/123456-wuph/cas-manage01/cas-manage01/\",\"type\":\"\"},\"name\":\"ccod-             runtime\"},{\"configMap\":{\"items\":[{\"key\":\"cas.properties\",\"path\":\"cas.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"cas2-manage01\"},\"name\":\"cas2-manage01-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"123456-wuph\"},\"name\":\"123456-wuph-     volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"type\":\"CCODDomainModule\",\"glsServer\":\"glsserver\"},\"name\":\"glsserver- public01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir -p                     /root/Platform/bin;cd /root/Platform; wget -t 3                     http://10.130.41.218:8081/repository/tmp/configText/123456-                     wuph/public01_glsserver/gls_config.cfg -P ./cfg -N; wget -t 3                     http://10.130.41.218:8081/repository/tmp/configText/123456-                     wuph/public01_glsserver/gls_logger.cfg -P ./cfg -N; wget -t 3                     http://10.130.41.218:8081/repository/tmp/configText/123456-                     wuph/publicConfig/tnsnames.ora -P /usr/local/lib -N; cd                     /root/Platform/bin/;/root/Platform/bin/Glsserver;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-                 base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"lifecycle\":{\"preStop\":{\"exec\":{\"command\":[\"/bin/bash\",\"-c\",\"echo \\u0027gls stop                 ...\\u0027\"]}}},\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":600,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":17020},\"timeoutSeconds\":1},\"name\":\"glsserver-     public01\",\"ports\":[{\"containerPort\":17020,\"protocol\":\"TCP\"}],\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/root/Platform/bin\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-         runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp             /opt/Glsserver /binary-file/;\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/ccod/glsserver:7b699a4aece10ef28dce83ab36e4d79213ec4f             69\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"glsserver\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/home/ccodrunner/Platform/cfg/\",\"name\":\"glsserver-public01-         volume\"},{\"mountPath\":\"/root/Platform/bin\",\"name\":\"123456-wuph-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-             runtime/123456-wuph/glsserver-public01/glsserver-public01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"gls_config.cfg\",\"path\":\"gls_config.cfg\"},{\"key\":\"gls_logger.cfg\",\"path\":\"gls_logger.cfg\"}],\"name\":\"glsserver-public01\"},\"name\":\"glsserver-public01-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"123456-wuph\"},\"name\":\"123456-wuph-     volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"public02\",\"type\":\"CCODDomainModule\",\"glsServer\":\"glsserver\"},\"name\":\"glsserver- public02\",\"namespace\":\"123456-wuph\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"public02\",\"glsServer\":\"glsserver\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"public02\",\"glsServer\":\"glsserver\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir -p                     /root/Platform/bin;cd /root/Platform; wget -t 3                     http://10.130.41.218:8081/repository/tmp/configText/123456-                     wuph/public01_glsserver/gls_config.cfg -P ./cfg -N; wget -t 3                     http://10.130.41.218:8081/repository/tmp/configText/123456-                     wuph/public01_glsserver/gls_logger.cfg -P ./cfg -N; wget -t 3                     http://10.130.41.218:8081/repository/tmp/configText/123456-                     wuph/publicConfig/tnsnames.ora -P /usr/local/lib -N; cd                     /root/Platform/bin/;/root/Platform/bin/Glsserver;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-                 base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"lifecycle\":{\"preStop\":{\"exec\":{\"command\":[\"/bin/bash\",\"-c\",\"echo \\u0027gls stop                 ...\\u0027\"]}}},\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":600,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":17020},\"timeoutSeconds\":1},\"name\":\"glsserver-     public02\",\"ports\":[{\"containerPort\":17020,\"protocol\":\"TCP\"}],\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/root/Platform/bin\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-         runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp             /opt/Glsserver /binary-file/;\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/ccod/glsserver:7b699a4aece10ef28dce83ab36e4d79213ec4f             69\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"glsserver\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/home/ccodrunner/Platform/cfg/\",\"name\":\"glsserver-public02-         volume\"},{\"mountPath\":\"/root/Platform/bin\",\"name\":\"123456-wuph-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-             runtime/123456-wuph/glsserver-public01/glsserver-public01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"gls_config.cfg\",\"path\":\"gls_config.cfg\"},{\"key\":\"gls_logger.cfg\",\"path\":\"gls_logger.cfg\"}],\"name\":\"glsserver-public02\"},\"name\":\"glsserver-public02-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"123456-wuph\"},\"name\":\"123456-wuph-     volume\"}]}}}},{\"metadata\":{\"labels\":{\"name\":\"mysql-5-7-29\",\"type\":\"ThreePartApp\"},\"name\":\"mysql-5-7-29\",\"namespace\":\"123456-wuph\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"mysql-5-7-             29\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"mysql-5-7-29\"}},\"spec\":{\"containers\":[{\"args\":[\"--             default_authentication_plugin\\u003dmysql_native_password\",\"--character-set-             server\\u003dutf8mb4\",\"--collation-server\\u003dutf8mb4_unicode_ci\",\"--             lower-case-table-names\\u003d1\"],\"env\":[{\"name\":\"MYSQL_ROOT_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_USER\",\"value\":\"ccod\"},{\"name\":\"MYSQL_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_DATABASE\",\"value\":\"ccod\"}],\"image\":\"nexus.io:5000/db/mysql:5.7.29\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"mysql-5-7-29\",\"ports\":[{\"containerPort\":3306,\"protocol\":\"TCP\"}],\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/docker-entrypoint-initdb.d/\",\"name\":\"sql\",\"subPath\":\"db/mysql/sql\"},{\"mountPath\":\"/var/lib/mysql/\",\"name\":\"sql\",\"subPath\":\"db/mysql/data\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-             volume-123456-wuph\"}}]}}}},{\"metadata\":{\"labels\":{\"name\":\"oracle\",\"type\":\"ThreePartApp\"},\"name\":\"oracle\",\"namespace\":\"123456-wuph\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"oracle\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"oracle\"}},\"spec\":{\"containers\":[{\"args\":[\"-c\",\"/tmp/init.sh 123456-wuph.ccod.io\"],\"command\":[\"/bin/bash\"],\"image\":\"nexus.io:5000/db/oracle-32-xe-10g:1.0\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"oracle\",\"ports\":[{\"containerPort\":1521,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"exec\":{\"command\":[\"cat\",\"/readiness\"]},\"failureThreshold\":1,\"periodSeconds\":1,\"successThreshold\":1,\"timeoutSeconds\":1},\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/tmp\",\"name\":\"sql\",\"subPath\":\"db/oracle/sql\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-                     volume-123456-wuph\"}}]}}}}],\"k8sServiceList\":[{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"name\":\"glsserver-public01\"},\"name\":\"glsserver-public01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"clusterIP\":\"10.111.231.27\",\"ports\":[{\"name\":\"glsserver-public01-17020\",\"port\":17020,\"protocol\":\"TCP\",\"targetPort\":17020}],\"selector\":{\"glsServer\":\"glsserver\"\"domain-id\":\"public01\"},\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"name\":\"glsserver-public02\"},\"name\":\"glsserver-public02\",\"namespace\":\"123456-wuph\"},\"spec\":{\"clusterIP\":\"10.111.231.27\",\"ports\":[{\"name\":\"glsserver-public02-17020\",\"port\":17020,\"protocol\":\"TCP\",\"targetPort\":17020}],\"selector\":{\"glsServer\":\"glsserver\"\"domain-             id\":\"public02\"},\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"name\":\"cas1-             manage01\"},\"name\":\"cas1-manage01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"clusterIP\":\"10.105.228.49\",\"ports\":[{\"name\":\"cas1-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"cas\":\"cas1\",\"domain-         id\":\"manage01\"},\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"name\":\"cas2-         manage01\"},\"name\":\"cas2-manage01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"clusterIP\":\"10.105.228.49\",\"ports\":[{\"name\":\"cas2-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"cas\":\"cas2\",\"domain-     id\":\"manage01\"},\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"name\":\"mysql\",\"namespace\":\"202005-test\"},\"spec\":{\"clusterIP\":\"10.96.74.215\",\"externalTrafficPolicy\":\"Cluster\",\"ports\":[{\"nodePort\":30414,\"port\":3306,\"protocol\":\"TCP\",\"targetPort\":3306}],\"selector\":{\"name\":\"mysql-5-7-29\"},\"sessionAffinity\":\"None\",\"type\":\"NodePort\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"name\":\"oracle\",\"namespace\":\"123456-wuph\"},\"spec\":{\"clusterIP\":\"10.110.48.142\",\"externalTrafficPolicy\":\"Cluster\",\"ports\":[{\"nodePort\":32422,\"port\":1521,\"protocol\":\"TCP\",\"targetPort\":1521}],\"selector\":{\"name\":\"oracle\"},\"sessionAffinity\":\"None\",\"type\":\"NodePort\"}}],\"k8sIngressList\":[{\"apiVersion\":\"extensions/v1beta1\",\"kind\":\"Ingress\",\"metadata\":{\"name\":\"cas1-manage01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"rules\":[{\"host\":\"123456-wuph.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"cas1-manage01\",\"servicePort\":80},\"path\":\"/cas\"}]}}]}},{\"apiVersion\":\"extensions/v1beta1\",\"kind\":\"Ingress\",\"metadata\":{\"name\":\"cas2-manage01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"rules\":[{\"host\":\"123456-wuph.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"cas2-manage01\",\"servicePort\":80},\"path\":\"/cas\"}]}}]}}]}{\"schemaId\":\"12345666778991\",\"domainUpdatePlanList\":[{\"domainName\":\"公共组件01\",\"domainId\":\"public01\",\"bkSetName\":\"公共组件\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"glsserver-public01\",\"appName\":\"glsServer\",\"appAlias\":\"glsserver\",\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/home/ccodrunner/Platform\",\"appRunner\":\"glsserver\",\"cfgs\":[{\"fileName\":\"gls_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/public01/10.130.29.111/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69/glsserver/20200327152037/gls_logger.cfg\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzoxM2IyOWU0NDlmMGUzYjhkOTU5MTg0MDdhYTgzNzY0NQ\"},{\"fileName\":\"gls_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"609e331e9d5052a61de5e6b5addd5ce3\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/public01/10.130.29.111/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69/glsserver/20200327152037/gls_config.cfg\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzowYWI4MGE3NDM5MjFlNDI2NTk3NmY5YWEyNDkzM2M5ZQ\"}],\"addDelay\":30}],\"updateType\":\"ADD\",\"comment\":\"clone from 公共组件01 of pahj\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"公共组件02\",\"domainId\":\"public02\",\"bkSetName\":\"公共组件\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"glsserver-public02\",\"appName\":\"glsServer\",\"appAlias\":\"glsserver\",\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/home/ccodrunner/Platform\",\"appRunner\":\"glsserver\",\"cfgs\":[{\"fileName\":\"gls_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/public01/10.130.29.111/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69/glsserver/20200327152037/gls_logger.cfg\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzoxM2IyOWU0NDlmMGUzYjhkOTU5MTg0MDdhYTgzNzY0NQ\"},{\"fileName\":\"gls_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"609e331e9d5052a61de5e6b5addd5ce3\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/public01/10.130.29.111/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69/glsserver/20200327152037/gls_config.cfg\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzowYWI4MGE3NDM5MjFlNDI2NTk3NmY5YWEyNDkzM2M5ZQ\"}],\"addDelay\":30},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"glsserver-public02\",\"appName\":\"glsServer\",\"appAlias\":\"glsserver\",\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/home/ccodrunner/Platform\",\"appRunner\":\"glsserver\",\"cfgs\":[{\"fileName\":\"gls_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/public01/10.130.29.111/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69/glsserver/20200327152037/gls_logger.cfg\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzoxM2IyOWU0NDlmMGUzYjhkOTU5MTg0MDdhYTgzNzY0NQ\"},{\"fileName\":\"gls_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"609e331e9d5052a61de5e6b5addd5ce3\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/public01/10.130.29.111/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69/glsserver/20200327152037/gls_config.cfg\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzowYWI4MGE3NDM5MjFlNDI2NTk3NmY5YWEyNDkzM2M5ZQ\"}],\"addDelay\":30}],\"updateType\":\"ADD\",\"comment\":\"clone from 公共组件01 of pahj\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"管理门户01\",\"domainId\":\"manage01\",\"bkSetName\":\"管理门户\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"cas1-manage01\",\"appName\":\"cas\",\"appAlias\":\"cas1\",\"targetVersion\":\"10973\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/home/portal/tomcat/webapps/\",\"appRunner\":\"cas\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\",\"deployPath\":\"./cas/WEB-INF/\",\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/manage01/10.130.29.111/cas/10973/cas/20200327152226/web.xml\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzoxM2IyOWU0NDlmMGUzYjhkM2YwNGQ5NDc5MTYwMzFkNg\"},{\"fileName\":\"cas.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"1fe26a2aa7df9ca4d1173ef8bfef2a5c\",\"deployPath\":\"./cas/WEB-INF/\",\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/manage01/10.130.29.111/cas/10973/cas/20200327152226/cas.properties\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzpkNDgxMTc1NDFkY2I4OWVjMzA5NDAyMWQxZDRjYTM1Yw\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"cas2-manage01\",\"appName\":\"cas\",\"appAlias\":\"cas2\",\"targetVersion\":\"10973\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/home/portal/tomcat/webapps/\",\"appRunner\":\"cas\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\",\"deployPath\":\"./cas/WEB-INF/\",\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/manage01/10.130.29.111/cas/10973/cas/20200327152226/web.xml\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzoxM2IyOWU0NDlmMGUzYjhkM2YwNGQ5NDc5MTYwMzFkNg\"},{\"fileName\":\"cas.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"1fe26a2aa7df9ca4d1173ef8bfef2a5c\",\"deployPath\":\"./cas/WEB-INF/\",\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/manage01/10.130.29.111/cas/10973/cas/20200327152226/cas.properties\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzpkNDgxMTc1NDFkY2I4OWVjMzA5NDAyMWQxZDRjYTM1Yw\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"comment\":\"clone from 管理门户01 of pahj\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"}],\"platformId\":\"k8s-platform-test\",\"platformName\":\"工具组平台\",\"bkBizId\":34,\"bkCloudId\":10,\"ccodVersion\":\"CCOD4.1\",\"taskType\":\"CREATE\",\"title\":\"新建工具组平台(proto-platform)计划\",\"comment\":\"create 工具组平台(proto-platform) by clone pahj(平安环境公司容器化测试)\",\"deployScriptRepository\":\"some_test\",\"deployScriptPath\":\"proto-platform/20200526123032/platform_deploy.py\",\"deployScriptMd5\":\"5843a22c7476403f234069af0af16e57\",\"k8sHostIp\":\"10.130.41.218\",\"glsDBType\":\"ORACLE\",\"glsDBUser\":\"ccod\",\"glsDBPwd\":\"ccod\",\"baseDataNexusRepository\":\"platform_base_data\",\"baseDataNexusPath\":\"ccod/4.1/baseVolume.zip\",\"publicConfig\":[{\"fileName\":\"local_datasource.xml\",\"fileSize\":0,\"md5\":\"e9c26f00f17a7660bfa3f785c4fe34be\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/123456-wuph/publicConfig/local_datasource.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFhN2U5NGFhNDVlZTIxN2Nm\"},{\"fileName\":\"local_jvm.xml\",\"fileSize\":0,\"md5\":\"d41d8cd98f00b204e9800998ecf8427e\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/123456-wuph/publicConfig/local_jvm.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNkODFhZTYxM2NmNDM3NmQ3\"},{\"fileName\":\"tnsnames.ora\",\"fileSize\":0,\"md5\":\"d41d8cd98f00b204e9800998ecf8427e\",\"deployPath\":\"/usr/local/lib\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/123456-wuph/publicConfig/tnsnames.ora\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQzMDcyY2Q2ZTEyNzg2NTQ3\"}],\"k8sApiUrl\":\"https://10.130.41.218:6443\",\"k8sAuthToken\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA\",\"k8sDeploymentList\":[{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"cas\":\"cas1\"},\"name\":\"cas1-manage01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"cas\":\"cas1\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"cas\":\"cas1\"}},\"spec\":{\"containers\":[{\"command\":[\"/bin/sh\",\"-c\",\"keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh; tail -F /usr/local/tomcat/logs/catalina.out\"],\"image\":\"nexus.io:5000/ccod-base/tomcat6-jre7:1\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/cas\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"cas1-manage01\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"}]}],\"hostAliases\":[{\"hostnames\":[\"123456-wuph.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;wget http://10.130.41.218:8081/repository/tmp/configText/123456-wuph/manage01_cas/cas.properties;mkdir -p WEB-INF;mv cas.properties WEB-INF/;jar uf cas.war WEB-INF/cas.properties;wget http://10.130.41.218:8081/repository/tmp/configText/123456-wuph/manage01_cas/web.xml;mkdir -p WEB-INF;mv web.xml WEB-INF/;jar uf cas.war WEB-INF/web.xml;mv /opt/cas.war /war;cd /opt;jar uf cas.war /home/portal/tomcat/webapps/cas/WEB-INF//cas.properties;jar uf cas.war /home/portal/tomcat/webapps/cas/WEB-INF//web.xml\"],\"image\":\"nexus.io:5000/ccod/cas:10973\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"cas1\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/home/portal/tomcat/webapps/cas/WEB-INF/\",\"name\":\"cas1-manage01-volume\"},{\"mountPath\":\"/opt/WEB-INF\",\"name\":\"123456-wuph-volume\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/123456-wuph/cas-manage01/cas-manage01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"configMap\":{\"items\":[{\"key\":\"cas.properties\",\"path\":\"cas.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"cas1-manage01\"},\"name\":\"cas1-manage01-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"123456-wuph\"},\"name\":\"123456-wuph-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"cas\":\"cas2\"},\"name\":\"cas2-manage01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"cas\":\"cas2\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"cas\":\"cas2\"}},\"spec\":{\"containers\":[{\"command\":[\"/bin/sh\",\"-c\",\"keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh; tail -F /usr/local/tomcat/logs/catalina.out\"],\"image\":\"nexus.io:5000/ccod-base/tomcat6-jre7:1\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/cas\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"cas-manage01\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"}]}],\"hostAliases\":[{\"hostnames\":[\"123456-wuph.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;wget http://10.130.41.218:8081/repository/tmp/configText/123456-wuph/manage01_cas/cas.properties;mkdir -p WEB-INF;mv cas.properties WEB-INF/;jar uf cas.war WEB-INF/cas.properties;wget http://10.130.41.218:8081/repository/tmp/configText/123456-wuph/manage01_cas/web.xml;mkdir -p WEB-INF;mv web.xml WEB-INF/;jar uf cas.war WEB-INF/web.xml;mv /opt/cas.war /war;cd /opt;jar uf cas.war /home/portal/tomcat/webapps/cas/WEB-INF//cas.properties;jar uf cas.war /home/portal/tomcat/webapps/cas/WEB-INF//web.xml\"],\"image\":\"nexus.io:5000/ccod/cas:10973\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"cas2\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/home/portal/tomcat/webapps/cas/WEB-INF/\",\"name\":\"cas2-manage01-volume\"},{\"mountPath\":\"/opt/WEB-INF\",\"name\":\"123456-wuph-volume\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/123456-wuph/cas-manage01/cas-manage01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"configMap\":{\"items\":[{\"key\":\"cas.properties\",\"path\":\"cas.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"cas2-manage01\"},\"name\":\"cas2-manage01-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"123456-wuph\"},\"name\":\"123456-wuph-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"type\":\"CCODDomainModule\",\"glsServer\":\"glsserver\"},\"name\":\"glsserver-public01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir -p /root/Platform/bin;cd /root/Platform; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/123456-wuph/public01_glsserver/gls_config.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/123456-wuph/public01_glsserver/gls_logger.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/123456-wuph/publicConfig/tnsnames.ora -P /usr/local/lib -N; cd /root/Platform/bin/;/root/Platform/bin/Glsserver;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"lifecycle\":{\"preStop\":{\"exec\":{\"command\":[\"/bin/bash\",\"-c\",\"echo \\u0027gls stop ...\\u0027\"]}}},\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":600,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":17020},\"timeoutSeconds\":1},\"name\":\"glsserver-public01\",\"ports\":[{\"containerPort\":17020,\"protocol\":\"TCP\"}],\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/root/Platform/bin\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/Glsserver /binary-file/;\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/ccod/glsserver:7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"glsserver\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/home/ccodrunner/Platform/cfg/\",\"name\":\"glsserver-public01-volume\"},{\"mountPath\":\"/root/Platform/bin\",\"name\":\"123456-wuph-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/123456-wuph/glsserver-public01/glsserver-public01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"gls_config.cfg\",\"path\":\"gls_config.cfg\"},{\"key\":\"gls_logger.cfg\",\"path\":\"gls_logger.cfg\"}],\"name\":\"glsserver-public01\"},\"name\":\"glsserver-public01-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"123456-wuph\"},\"name\":\"123456-wuph-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"public02\",\"type\":\"CCODDomainModule\",\"glsServer\":\"glsserver\"},\"name\":\"glsserver-public02\",\"namespace\":\"123456-wuph\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"public02\",\"glsServer\":\"glsserver\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"public02\",\"glsServer\":\"glsserver\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir -p /root/Platform/bin;cd /root/Platform; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/123456-wuph/public01_glsserver/gls_config.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/123456-wuph/public01_glsserver/gls_logger.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/123456-wuph/publicConfig/tnsnames.ora -P /usr/local/lib -N; cd /root/Platform/bin/;/root/Platform/bin/Glsserver;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"lifecycle\":{\"preStop\":{\"exec\":{\"command\":[\"/bin/bash\",\"-c\",\"echo \\u0027gls stop ...\\u0027\"]}}},\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":600,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":17020},\"timeoutSeconds\":1},\"name\":\"glsserver-public02\",\"ports\":[{\"containerPort\":17020,\"protocol\":\"TCP\"}],\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/root/Platform/bin\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/Glsserver /binary-file/;\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/ccod/glsserver:7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"glsserver\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/home/ccodrunner/Platform/cfg/\",\"name\":\"glsserver-public02-volume\"},{\"mountPath\":\"/root/Platform/bin\",\"name\":\"123456-wuph-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/123456-wuph/glsserver-public01/glsserver-public01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"gls_config.cfg\",\"path\":\"gls_config.cfg\"},{\"key\":\"gls_logger.cfg\",\"path\":\"gls_logger.cfg\"}],\"name\":\"glsserver-public02\"},\"name\":\"glsserver-public02-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"123456-wuph\"},\"name\":\"123456-wuph-volume\"}]}}}},{\"metadata\":{\"labels\":{\"name\":\"mysql-5-7-29\",\"type\":\"ThreePartApp\"},\"name\":\"mysql-5-7-29\",\"namespace\":\"123456-wuph\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"mysql-5-7-29\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"mysql-5-7-29\"}},\"spec\":{\"containers\":[{\"args\":[\"--default_authentication_plugin\\u003dmysql_native_password\",\"--character-set-server\\u003dutf8mb4\",\"--collation-server\\u003dutf8mb4_unicode_ci\",\"--lower-case-table-names\\u003d1\"],\"env\":[{\"name\":\"MYSQL_ROOT_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_USER\",\"value\":\"ccod\"},{\"name\":\"MYSQL_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_DATABASE\",\"value\":\"ccod\"}],\"image\":\"nexus.io:5000/db/mysql:5.7.29\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"mysql-5-7-29\",\"ports\":[{\"containerPort\":3306,\"protocol\":\"TCP\"}],\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/docker-entrypoint-initdb.d/\",\"name\":\"sql\",\"subPath\":\"db/mysql/sql\"},{\"mountPath\":\"/var/lib/mysql/\",\"name\":\"sql\",\"subPath\":\"db/mysql/data\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-123456-wuph\"}}]}}}},{\"metadata\":{\"labels\":{\"name\":\"oracle\",\"type\":\"ThreePartApp\"},\"name\":\"oracle\",\"namespace\":\"123456-wuph\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"oracle\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"oracle\"}},\"spec\":{\"containers\":[{\"args\":[\"-c\",\"/tmp/init.sh 123456-wuph.ccod.io\"],\"command\":[\"/bin/bash\"],\"image\":\"nexus.io:5000/db/oracle-32-xe-10g:1.0\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"oracle\",\"ports\":[{\"containerPort\":1521,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"exec\":{\"command\":[\"cat\",\"/readiness\"]},\"failureThreshold\":1,\"periodSeconds\":1,\"successThreshold\":1,\"timeoutSeconds\":1},\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/tmp\",\"name\":\"sql\",\"subPath\":\"db/oracle/sql\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-123456-wuph\"}}]}}}}],\"k8sServiceList\":[{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"name\":\"glsserver-public01\"},\"name\":\"glsserver-public01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"clusterIP\":\"10.111.231.27\",\"ports\":[{\"name\":\"glsserver-public01-17020\",\"port\":17020,\"protocol\":\"TCP\",\"targetPort\":17020}],\"selector\":{\"glsServer\":\"glsserver\",\"domain-id\":\"public01\"},\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"name\":\"glsserver-public02\"},\"name\":\"glsserver-public02\",\"namespace\":\"123456-wuph\"},\"spec\":{\"clusterIP\":\"10.111.231.27\",\"ports\":[{\"name\":\"glsserver-public02-17020\",\"port\":17020,\"protocol\":\"TCP\",\"targetPort\":17020}],\"selector\":{\"glsServer\":\"glsserver\",\"domain-id\":\"public02\"},\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"name\":\"cas1-manage01\"},\"name\":\"cas1-manage01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"clusterIP\":\"10.105.228.49\",\"ports\":[{\"name\":\"cas1-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"cas\":\"cas1\",\"domain-id\":\"manage01\"},\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"name\":\"cas2-manage01\"},\"name\":\"cas2-manage01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"clusterIP\":\"10.105.228.49\",\"ports\":[{\"name\":\"cas2-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"cas\":\"cas2\",\"domain-id\":\"manage01\"},\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"name\":\"mysql\",\"namespace\":\"202005-test\"},\"spec\":{\"clusterIP\":\"10.96.74.215\",\"externalTrafficPolicy\":\"Cluster\",\"ports\":[{\"nodePort\":30414,\"port\":3306,\"protocol\":\"TCP\",\"targetPort\":3306}],\"selector\":{\"name\":\"mysql-5-7-29\"},\"sessionAffinity\":\"None\",\"type\":\"NodePort\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"name\":\"oracle\",\"namespace\":\"123456-wuph\"},\"spec\":{\"clusterIP\":\"10.110.48.142\",\"externalTrafficPolicy\":\"Cluster\",\"ports\":[{\"nodePort\":32422,\"port\":1521,\"protocol\":\"TCP\",\"targetPort\":1521}],\"selector\":{\"name\":\"oracle\"},\"sessionAffinity\":\"None\",\"type\":\"NodePort\"}}],\"k8sIngressList\":[{\"apiVersion\":\"extensions/v1beta1\",\"kind\":\"Ingress\",\"metadata\":{\"name\":\"cas1-manage01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"rules\":[{\"host\":\"123456-wuph.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"cas1-manage01\",\"servicePort\":80},\"path\":\"/cas\"}]}}]}},{\"apiVersion\":\"extensions/v1beta1\",\"kind\":\"Ingress\",\"metadata\":{\"name\":\"cas2-manage01\",\"namespace\":\"123456-wuph\"},\"spec\":{\"rules\":[{\"host\":\"123456-wuph.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"cas2-manage01\",\"servicePort\":80},\"path\":\"/cas\"}]}}]}}]}";
//            PlatformUpdateSchemaInfo schema = generateDemoCreateSchema("202005-test", "schema-test");
//            logger.info(String.format("generateSchema=%s", gson.toJson(schema)));
//            schema.setPlatformId("k8s-platform-test");
//            String jsonStr = "{\"schemaId\":\"e2b849bd-0ac4-4591-97f3-eee6e7084a58\",\"domainUpdatePlanList\":[{\"domainName\":\"公共组件01\",\"domainId\":\"public01\",\"bkSetName\":\"公共组件\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"glsserver-public01\",\"appName\":\"glsServer\",\"appAlias\":\"glsserver\",\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/home/ccodrunner/Platform\",\"appRunner\":\"glsserver\",\"cfgs\":[{\"fileName\":\"gls_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"f23a83a2d871d59c89d12b0281e10e90\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_glsserver/gls_config.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmMmRiNjc0YzQ5YmE4Nzdj\"},{\"fileName\":\"gls_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_glsserver/gls_logger.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFhYTlmZWY0MTJkMDY2ZTM3\"}],\"addDelay\":30}],\"updateType\":\"ADD\",\"comment\":\"clone from 公共组件01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"管理门户01\",\"domainId\":\"manage01\",\"bkSetName\":\"管理门户\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"cas-manage01\",\"appName\":\"cas\",\"appAlias\":\"cas\",\"targetVersion\":\"10973\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/home/portal/tomcat/webapps/\",\"appRunner\":\"cas\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\",\"deployPath\":\"./cas/WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_cas/web.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE2MDA0MWExZjQ1ODc4Njhh\"},{\"fileName\":\"cas.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"6622e01a4df917d747e078e89c774a52\",\"deployPath\":\"./cas/WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_cas/cas.properties\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3N2E3NDBhYmM0NzU2NDg5\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"comment\":\"clone from 管理门户01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"}],\"platformId\":\"k8s-test\",\"platformName\":\"工具组平台\",\"bkBizId\":34,\"bkCloudId\":0,\"ccodVersion\":\"CCOD4.1\",\"taskType\":\"CREATE\",\"title\":\"新建工具组平台(202005-test)计划\",\"comment\":\"create 工具组平台(202005-test) by clone 123456-wuph(ccod开发测试平台)\",\"k8sHostIp\":\"10.130.41.218\",\"glsDBType\":\"ORACLE\",\"glsDBUser\":\"ccod\",\"glsDBPwd\":\"ccod\",\"baseDataNexusRepository\":\"platform_base_data\",\"baseDataNexusPath\":\"ccod/4.1/baseVolume.zip\",\"publicConfig\":[{\"fileName\":\"local_datasource.xml\",\"fileSize\":0,\"md5\":\"112940181aeb983baa9d7fd2733f194f\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_datasource.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmNmE4MWY3MWI3MmJlY2Ji\"},{\"fileName\":\"local_jvm.xml\",\"fileSize\":0,\"md5\":\"d172a5321944aba5bc19c35d00950afc\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_jvm.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRjMWQyMTIwNWRmYmY1MDM0\"},{\"fileName\":\"tnsnames.ora\",\"fileSize\":0,\"md5\":\"811f7f9472d5f6e733d732619a17ac77\",\"deployPath\":\"/usr/local/lib\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/tnsnames.ora\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY0NzFkOTU3MGMyODRjMGJm\"}],\"k8sApiUrl\":\"https://10.130.41.218:6443\",\"k8sAuthToken\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA\",\"k8sDeploymentList\":[{\"metadata\":{\"labels\":{\"name\":\"mysql-5-7-29\",\"type\":\"ThreePartApp\"},\"name\":\"mysql-5-7-29\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"mysql-5-7-29\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"mysql-5-7-29\"}},\"spec\":{\"containers\":[{\"args\":[\"--default_authentication_plugin\\u003dmysql_native_password\",\"--character-set-server\\u003dutf8mb4\",\"--collation-server\\u003dutf8mb4_unicode_ci\",\"--lower-case-table-names\\u003d1\"],\"env\":[{\"name\":\"MYSQL_ROOT_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_USER\",\"value\":\"ccod\"},{\"name\":\"MYSQL_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_DATABASE\",\"value\":\"ccod\"}],\"image\":\"nexus.io:5000/db/mysql:5.7.29\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"mysql-5-7-29\",\"ports\":[{\"containerPort\":3306,\"protocol\":\"TCP\"}],\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/docker-entrypoint-initdb.d/\",\"name\":\"sql\",\"subPath\":\"db/mysql/sql\"},{\"mountPath\":\"/var/lib/mysql/\",\"name\":\"sql\",\"subPath\":\"db/mysql/data\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-k8s-test\"}}]}}}},{\"metadata\":{\"labels\":{\"name\":\"oracle\",\"type\":\"ThreePartApp\"},\"name\":\"oracle\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"oracle\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"oracle\"}},\"spec\":{\"containers\":[{\"args\":[\"-c\",\"/tmp/init.sh 202005-test.ccod.io\"],\"command\":[\"/bin/bash\"],\"image\":\"nexus.io:5000/db/oracle-32-xe-10g:1.0\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"oracle\",\"ports\":[{\"containerPort\":1521,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"exec\":{\"command\":[\"cat\",\"/readiness\"]},\"failureThreshold\":1,\"periodSeconds\":1,\"successThreshold\":1,\"timeoutSeconds\":1},\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/tmp\",\"name\":\"sql\",\"subPath\":\"db/oracle/sql\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-k8s-test\"}}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\"},\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"cas\":\"cas\"}},\"strategy\":{\"type\":\"Recreate\"},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"cas\":\"cas\"}},\"spec\":{\"containers\":[{\"command\":[\"/bin/sh\",\"-c\",\"keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh; tail -F /usr/local/tomcat/logs/catalina.out\"],\"image\":\"nexus.io:5000/ccod-base/tomcat6-jre7:1\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/cas\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"cas-manage01\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;jar uf cas.war WEB-INF/cas.properties;jar uf cas.war WEB-INF/web.xml\"],\"image\":\"nexus.io:5000/ccod/cas:10973\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"cas\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/opt/WEB-INF/classes\",\"name\":\"cas-manage01-volume\"},{\"mountPath\":\"/opt/WEB-INF\",\"name\":\"k8s-test-volume\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/k8s-test/cas-manage01/cas-manage01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"configMap\":{\"items\":[{\"key\":\"cas.properties\",\"path\":\"cas.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"cas-manage01\"},\"name\":\"cas-manage01-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"k8s-test\"},\"name\":\"k8s-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"type\":\"CCODDomainModule\"},\"name\":\"glsserver-public01\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"}},\"strategy\":{\"type\":\"Recreate\"},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir -p /root/Platform/bin;cd /root/Platform; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_config.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_logger.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/publicConfig/tnsnames.ora -P /usr/local/lib -N; cd /root/Platform/bin/;/root/Platform/bin/Glsserver;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"lifecycle\":{\"preStop\":{\"exec\":{\"command\":[\"/bin/bash\",\"-c\",\"echo \\u0027gls stop ...\\u0027\"]}}},\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":600,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":17020},\"timeoutSeconds\":1},\"name\":\"glsserver-public01\",\"ports\":[{\"containerPort\":17020,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform/bin\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/Glsserver /binary-file/;\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/ccod/glsserver:7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"glsserver\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/home/ccodrunner/Platform/cfg/\",\"name\":\"glsserver-public01-volume\"},{\"mountPath\":\"/root/Platform/bin\",\"name\":\"k8s-test-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/k8s-test/glsserver-public01/glsserver-public01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"gls_config.cfg\",\"path\":\"gls_config.cfg\"},{\"key\":\"gls_logger.cfg\",\"path\":\"gls_logger.cfg\"}],\"name\":\"glsserver-public01\"},\"name\":\"glsserver-public01-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"k8s-test\"},\"name\":\"k8s-test-volume\"}]}}}}],\"k8sServiceList\":[{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"},\"name\":\"glsserver-public01\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"name\":\"glsserver-public01-17020\",\"port\":17020,\"protocol\":\"TCP\",\"targetPort\":17020}],\"selector\":{\"name\":\"glsserver-public01\"},\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"cas\":\"cas\"},\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"name\":\"cas-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"name\":\"cas-manage01\"},\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"name\":\"mysql\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"port\":3306,\"protocol\":\"TCP\",\"targetPort\":3306}],\"selector\":{\"name\":\"mysql-5-7-29\"},\"type\":\"NodePort\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"name\":\"oracle\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"port\":1521,\"protocol\":\"TCP\",\"targetPort\":1521}],\"selector\":{\"name\":\"oracle\"},\"type\":\"NodePort\"}}],\"k8sEndpointsList\":[{\"apiVersion\":\"v1\",\"kind\":\"Endpoints\",\"metadata\":{\"labels\":{\"name\":\"cas-manage01\"},\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.6\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"cas-manage01-5b8599cf8d-qqkqz\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"cas-manage01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"apiVersion\":\"v1\",\"kind\":\"Endpoints\",\"metadata\":{\"labels\":{\"name\":\"glsserver-public01\"},\"name\":\"glsserver-public01\",\"namespace\":\"202005-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.2\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"glsserver-public01-69d8868fb-sm5mm\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"glsserver-public01-17020\",\"port\":17020,\"protocol\":\"TCP\"}]}]}],\"k8sIngressList\":[{\"apiVersion\":\"extensions/v1beta1\",\"kind\":\"Ingress\",\"metadata\":{\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"spec\":{\"rules\":[{\"host\":\"k8s-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"cas-manage01\",\"servicePort\":80},\"path\":\"/cas\"}]}}]}}],\"k8sPVList\":[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolume\",\"metadata\":{\"name\":\"base-volume-k8s-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"capacity\":{\"storage\":\"1Gi\"},\"claimRef\":{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"name\":\"base-volume-k8s-test\",\"namespace\":\"k8s-test\"},\"nfs\":{\"path\":\"/home/kubernetes/volume/k8s-test/baseVolume\",\"server\":\"10.130.41.218\"},\"persistentVolumeReclaimPolicy\":\"Retain\",\"storageClassName\":\"base-volume-k8s-test\",\"volumeMode\":\"Filesystem\"}}],\"k8sPVCList\":[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"metadata\":{\"name\":\"base-volume-k8s-test\",\"namespace\":\"k8s-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"resources\":{\"requests\":{\"storage\":\"1Gi\"}},\"storageClassName\":\"base-volume-k8s-test\",\"volumeMode\":\"Filesystem\",\"volumeName\":\"base-volume-k8s-test\"}}]}";
//            String jsonStr = "{\"schemaId\":\"e2b849bd-0ac4-4591-97f3-eee6e7084a58\",\"domainUpdatePlanList\":[{\"domainName\":\"公共组件01\",\"domainId\":\"public01\",\"bkSetName\":\"公共组件\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"LicenseServer\",\"appAlias\":\"licenseserver\",\"targetVersion\":\"5214\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"licenseserver\",\"assembleTag\":\"glsserver-public01\",\"cfgs\":[{\"fileName\":\"Config.ini\",\"ext\":\"ini\",\"fileSize\":0,\"md5\":\"6c513269c4e2bc10f4a6cf0eb05e5bfc\",\"deployPath\":\"./license/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_licenseserver/Config.ini\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNlZGYyZTBkYjgyNWQ0OTRi\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"comment\":\"clone from 公共组件01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"管理门户01\",\"domainId\":\"manage01\",\"bkSetName\":\"管理门户\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"cas\",\"appAlias\":\"cas\",\"targetVersion\":\"10973\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"cas\",\"assembleTag\":\"cas-manage01\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_cas/web.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE2MDA0MWExZjQ1ODc4Njhh\"},{\"fileName\":\"cas.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"6622e01a4df917d747e078e89c774a52\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_cas/cas.properties\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3N2E3NDBhYmM0NzU2NDg5\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"comment\":\"clone from 管理门户01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"}],\"platformId\":\"k8s-test\",\"platformName\":\"工具组平台\",\"bkBizId\":34,\"bkCloudId\":0,\"ccodVersion\":\"CCOD4.1\",\"taskType\":\"CREATE\",\"title\":\"新建工具组平台(202005-test)计划\",\"comment\":\"create 工具组平台(202005-test) by clone 123456-wuph(ccod开发测试平台)\",\"k8sHostIp\":\"10.130.41.218\",\"glsDBType\":\"ORACLE\",\"glsDBUser\":\"ccod\",\"glsDBPwd\":\"ccod\",\"baseDataNexusRepository\":\"platform_base_data\",\"baseDataNexusPath\":\"ccod/4.1/baseVolume.zip\",\"publicConfig\":[{\"fileName\":\"local_datasource.xml\",\"fileSize\":0,\"md5\":\"112940181aeb983baa9d7fd2733f194f\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_datasource.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmNmE4MWY3MWI3MmJlY2Ji\"},{\"fileName\":\"local_jvm.xml\",\"fileSize\":0,\"md5\":\"d172a5321944aba5bc19c35d00950afc\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_jvm.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRjMWQyMTIwNWRmYmY1MDM0\"},{\"fileName\":\"tnsnames.ora\",\"fileSize\":0,\"md5\":\"811f7f9472d5f6e733d732619a17ac77\",\"deployPath\":\"/usr/local/lib\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/tnsnames.ora\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY0NzFkOTU3MGMyODRjMGJm\"}],\"k8sApiUrl\":\"https://10.130.41.218:6443\",\"k8sAuthToken\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA\",\"k8sDeploymentList\":[{\"metadata\":{\"labels\":{\"name\":\"mysql-5-7-29\",\"type\":\"ThreePartApp\"},\"name\":\"mysql-5-7-29\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"mysql-5-7-29\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"mysql-5-7-29\"}},\"spec\":{\"containers\":[{\"args\":[\"--default_authentication_plugin\\u003dmysql_native_password\",\"--character-set-server\\u003dutf8mb4\",\"--collation-server\\u003dutf8mb4_unicode_ci\",\"--lower-case-table-names\\u003d1\"],\"env\":[{\"name\":\"MYSQL_ROOT_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_USER\",\"value\":\"ccod\"},{\"name\":\"MYSQL_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_DATABASE\",\"value\":\"ccod\"}],\"image\":\"nexus.io:5000/db/mysql:5.7.29\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"mysql-5-7-29\",\"ports\":[{\"containerPort\":3306,\"protocol\":\"TCP\"}],\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/docker-entrypoint-initdb.d/\",\"name\":\"sql\",\"subPath\":\"db/mysql/sql\"},{\"mountPath\":\"/var/lib/mysql/\",\"name\":\"sql\",\"subPath\":\"db/mysql/data\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-k8s-test\"}}]}}}},{\"metadata\":{\"labels\":{\"name\":\"oracle\",\"type\":\"ThreePartApp\"},\"name\":\"oracle\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"oracle\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"oracle\"}},\"spec\":{\"containers\":[{\"args\":[\"-c\",\"/tmp/init.sh 202005-test.ccod.io\"],\"command\":[\"/bin/bash\"],\"image\":\"nexus.io:5000/db/oracle-32-xe-10g:1.0\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"oracle\",\"ports\":[{\"containerPort\":1521,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"exec\":{\"command\":[\"cat\",\"/readiness\"]},\"failureThreshold\":1,\"periodSeconds\":1,\"successThreshold\":1,\"timeoutSeconds\":1},\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/tmp\",\"name\":\"sql\",\"subPath\":\"db/oracle/sql\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-k8s-test\"}}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\"},\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"cas\":\"cas\"}},\"strategy\":{\"type\":\"Recreate\"},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"cas\":\"cas\"}},\"spec\":{\"containers\":[{\"command\":[\"/bin/sh\",\"-c\",\"keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh; tail -F /usr/local/tomcat/logs/catalina.out\"],\"image\":\"nexus.io:5000/ccod-base/tomcat6-jre7:1\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/cas\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"cas-manage01\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"command\":[\"/bin/sh\",\"-c\",\"\"],\"image\":\"nexus.io:5000/ccod/cas:10973\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"cas\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/k8s-test/cas-manage01/cas-manage01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"type\":\"CCODDomainModule\"},\"name\":\"glsserver-public01\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"}},\"strategy\":{\"type\":\"Recreate\"},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir -p /root/Platform/bin;cd /root/Platform; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_config.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_logger.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/publicConfig/tnsnames.ora -P /usr/local/lib -N; cd /root/Platform/bin/;/root/Platform/bin/Glsserver;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"lifecycle\":{\"preStop\":{\"exec\":{\"command\":[\"/bin/bash\",\"-c\",\"echo \\u0027gls stop ...\\u0027\"]}}},\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":600,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":17020},\"timeoutSeconds\":1},\"name\":\"glsserver-public01\",\"ports\":[{\"containerPort\":17020,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform/bin\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/Glsserver /binary-file/;\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/ccod/glsserver:7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"glsserver\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/k8s-test/glsserver-public01/glsserver-public01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"}]}}}}],\"k8sServiceList\":[{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"},\"name\":\"glsserver-public01\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"name\":\"glsserver-public01-17020\",\"port\":17020,\"protocol\":\"TCP\",\"targetPort\":17020}],\"selector\":{\"name\":\"glsserver-public01\"},\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"cas\":\"cas\"},\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"name\":\"cas-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"name\":\"cas-manage01\"},\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"name\":\"mysql\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"port\":3306,\"protocol\":\"TCP\",\"targetPort\":3306}],\"selector\":{\"name\":\"mysql-5-7-29\"},\"type\":\"NodePort\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"name\":\"oracle\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"port\":1521,\"protocol\":\"TCP\",\"targetPort\":1521}],\"selector\":{\"name\":\"oracle\"},\"type\":\"NodePort\"}}],\"k8sEndpointsList\":[{\"apiVersion\":\"v1\",\"kind\":\"Endpoints\",\"metadata\":{\"labels\":{\"name\":\"cas-manage01\"},\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.6\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"cas-manage01-5b8599cf8d-qqkqz\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"cas-manage01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"apiVersion\":\"v1\",\"kind\":\"Endpoints\",\"metadata\":{\"labels\":{\"name\":\"glsserver-public01\"},\"name\":\"glsserver-public01\",\"namespace\":\"202005-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.2\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"glsserver-public01-69d8868fb-sm5mm\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"glsserver-public01-17020\",\"port\":17020,\"protocol\":\"TCP\"}]}]}],\"k8sIngressList\":[{\"apiVersion\":\"extensions/v1beta1\",\"kind\":\"Ingress\",\"metadata\":{\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"spec\":{\"rules\":[{\"host\":\"k8s-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"cas-manage01\",\"servicePort\":80},\"path\":\"/cas\"}]}}]}}],\"k8sPVList\":[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolume\",\"metadata\":{\"name\":\"base-volume-k8s-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"capacity\":{\"storage\":\"1Gi\"},\"claimRef\":{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"name\":\"base-volume-k8s-test\",\"namespace\":\"k8s-test\"},\"nfs\":{\"path\":\"/home/kubernetes/volume/k8s-test/baseVolume\",\"server\":\"10.130.41.218\"},\"persistentVolumeReclaimPolicy\":\"Retain\",\"storageClassName\":\"base-volume-k8s-test\",\"volumeMode\":\"Filesystem\"}}],\"k8sPVCList\":[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"metadata\":{\"name\":\"base-volume-k8s-test\",\"namespace\":\"k8s-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"resources\":{\"requests\":{\"storage\":\"1Gi\"}},\"storageClassName\":\"base-volume-k8s-test\",\"volumeMode\":\"Filesystem\",\"volumeName\":\"base-volume-k8s-test\"}}]}";
//            String jsonStr = "{\"comment\":\"create by MANUAL\",\"platformFunc\":\"TEST\",\"platformId\":\"test29\",\"schemaId\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"k8sHostIp\":\"10.130.41.218\",\"taskType\":\"CREATE\",\"platformType\":\"K8S_CONTAINER\",\"deployScriptRepository\":\"some_test\",\"baseDataNexusPath\":\"ccod/4.1/baseVolume.zip\",\"title\":\"\\u5f00\\u53d1\\u7ec4\\u6d4b\\u8bd5\\u5e73\\u53f0[test29]\\u65b0\\u5efa\\u8ba1\\u5212\",\"bkCloudId\":8,\"publicConfig\":[],\"domainUpdatePlanList\":[{\"status\":\"CREATE\",\"domainId\":\"public01\",\"domainName\":\"\\u516c\\u5171\\u7ec4\\u4ef601\",\"updateType\":\"ADD\",\"tags\":\"\\u5165\\u547c\\u53eb,\\u5916\\u547c\",\"comment\":\"clone from \\u516c\\u5171\\u7ec4\\u4ef601 of pahj\",\"occurs\":600,\"bkSetName\":\"\\u516c\\u5171\\u7ec4\\u4ef6\",\"maxOccurs\":1000,\"appUpdateOperationList\":[{\"cfgs\":[{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/public01/10.130.29.111/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69/glsserver/20200327152037/gls_config.cfg\",\"fileName\":\"gls_config.cfg\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzowYWI4MGE3NDM5MjFlNDI2NTk3NmY5YWEyNDkzM2M5ZQ\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"609e331e9d5052a61de5e6b5addd5ce3\"},{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/public01/10.130.29.111/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69/glsserver/20200327152037/gls_logger.cfg\",\"fileName\":\"gls_logger.cfg\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzoxM2IyOWU0NDlmMGUzYjhkOTU5MTg0MDdhYTgzNzY0NQ\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\"}],\"appName\":\"glsServer\",\"platformAppId\":0,\"basePath\":\"/home/ccodrunner/Platform\",\"assembleTag\":\"separate01\",\"appRunner\":\"glsserver\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"assembleName\":\"glsserver1-public01\",\"addDelay\":0,\"appAlias\":\"glsserver1\"}]},{\"status\":\"CREATE\",\"domainId\":\"public02\",\"domainName\":\"\\u516c\\u5171\\u7ec4\\u4ef602\",\"updateType\":\"ADD\",\"tags\":\"\\u5165\\u547c\\u53eb,\\u5916\\u547c\",\"comment\":\"clone from \\u516c\\u5171\\u7ec4\\u4ef601 of pahj\",\"occurs\":600,\"bkSetName\":\"\\u516c\\u5171\\u7ec4\\u4ef6\",\"maxOccurs\":1000,\"appUpdateOperationList\":[{\"cfgs\":[{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/public01/10.130.29.111/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69/glsserver/20200327152037/gls_config.cfg\",\"fileName\":\"gls_config.cfg\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzowYWI4MGE3NDM5MjFlNDI2NTk3NmY5YWEyNDkzM2M5ZQ\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"609e331e9d5052a61de5e6b5addd5ce3\"},{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/public01/10.130.29.111/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69/glsserver/20200327152037/gls_logger.cfg\",\"fileName\":\"gls_logger.cfg\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzoxM2IyOWU0NDlmMGUzYjhkOTU5MTg0MDdhYTgzNzY0NQ\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\"}],\"appName\":\"glsServer\",\"platformAppId\":0,\"basePath\":\"/home/ccodrunner/Platform\",\"assembleTag\":\"separate01\",\"appRunner\":\"glsserver\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"assembleName\":\"glsserver2-public02\",\"addDelay\":0,\"appAlias\":\"glsserver2\"}]},{\"status\":\"CREATE\",\"domainId\":\"manage01\",\"domainName\":\"\\u7ba1\\u7406\\u95e8\\u623701\",\"updateType\":\"ADD\",\"tags\":\"\\u5165\\u547c\\u53eb,\\u5916\\u547c\",\"comment\":\"clone from \\u7ba1\\u7406\\u95e8\\u623701 of pahj\",\"occurs\":600,\"bkSetName\":\"\\u7ba1\\u7406\\u95e8\\u6237\",\"maxOccurs\":1000,\"appUpdateOperationList\":[{\"cfgs\":[{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/manage01/10.130.29.111/cas/10973/cas/20200327152226/cas.properties\",\"fileName\":\"cas.properties\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzpkNDgxMTc1NDFkY2I4OWVjMzA5NDAyMWQxZDRjYTM1Yw\",\"ext\":\"properties\",\"fileSize\":0,\"deployPath\":\"./cas/WEB-INF/\",\"md5\":\"1fe26a2aa7df9ca4d1173ef8bfef2a5c\"},{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/manage01/10.130.29.111/cas/10973/cas/20200327152226/web.xml\",\"fileName\":\"web.xml\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzoxM2IyOWU0NDlmMGUzYjhkM2YwNGQ5NDc5MTYwMzFkNg\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./cas/WEB-INF/\",\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\"}],\"appName\":\"cas\",\"platformAppId\":0,\"basePath\":\"/home/portal/tomcat/webapps/\",\"assembleTag\":\"base_01\",\"appRunner\":\"cas\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"targetVersion\":\"10973\",\"assembleName\":\"cas1-manage01\",\"addDelay\":0,\"appAlias\":\"cas1\"},{\"cfgs\":[{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/manage01/10.130.29.111/cas/10973/cas/20200327152226/cas.properties\",\"fileName\":\"cas.properties\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzpkNDgxMTc1NDFkY2I4OWVjMzA5NDAyMWQxZDRjYTM1Yw\",\"ext\":\"properties\",\"fileSize\":0,\"deployPath\":\"./cas/WEB-INF/\",\"md5\":\"1fe26a2aa7df9ca4d1173ef8bfef2a5c\"},{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/manage01/10.130.29.111/cas/10973/cas/20200327152226/web.xml\",\"fileName\":\"web.xml\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzoxM2IyOWU0NDlmMGUzYjhkM2YwNGQ5NDc5MTYwMzFkNg\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./cas/WEB-INF/\",\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\"}],\"appName\":\"cas\",\"platformAppId\":0,\"basePath\":\"/home/portal/tomcat/webapps/\",\"assembleTag\":\"base_01\",\"appRunner\":\"cas\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"targetVersion\":\"10973\",\"assembleName\":\"cas2-manage01\",\"addDelay\":0,\"appAlias\":\"cas2\"}]}],\"k8sApiUrl\":\"https://10.130.41.218:6443\",\"platformName\":\"\\u5f00\\u53d1\\u7ec4\\u6d4b\\u8bd5\\u5e73\\u53f0\",\"k8sAuthToken\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA\",\"status\":\"CREATE\",\"k8sIngressList\":[{\"kind\":\"Ingress\",\"spec\":{\"rules\":[{\"host\":\"test29.ccod.com\",\"http\":{\"paths\":[{\"path\":\"/cas1-manage01\",\"backend\":{\"serviceName\":\"cas1-manage01\",\"servicePort\":80}}]}}]},\"apiVersion\":\"extensions/v1beta1\",\"metadata\":{\"labels\":{\"cas\":\"cas1\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"name\":\"cas1-manage01\"},\"namespace\":\"test29\",\"name\":\"cas1-manage01\"}},{\"kind\":\"Ingress\",\"spec\":{\"rules\":[{\"host\":\"test29.ccod.com\",\"http\":{\"paths\":[{\"path\":\"/cas2-manage01\",\"backend\":{\"serviceName\":\"cas2-manage01\",\"servicePort\":80}}]}}]},\"apiVersion\":\"extensions/v1beta1\",\"metadata\":{\"labels\":{\"cas\":\"cas2\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"name\":\"cas2-manage01\"},\"namespace\":\"test29\",\"name\":\"cas2-manage01\"}}],\"k8sServiceList\":[{\"kind\":\"Service\",\"spec\":{\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\",\"ports\":[{\"targetPort\":17020,\"protocol\":\"TCP\",\"name\":\"register\",\"port\":17020}],\"selector\":{\"name\":\"glsserver1-public01\"}},\"apiVersion\":\"v1\",\"metadata\":{\"labels\":{\"glsServer\":\"glsserver1\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"public01\",\"type\":\"CCODDomainModule\",\"name\":\"glsserver1-public01\"},\"namespace\":\"test29\",\"name\":\"glsserver1-public01\"}},{\"kind\":\"Service\",\"spec\":{\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\",\"ports\":[{\"targetPort\":17020,\"protocol\":\"TCP\",\"name\":\"register\",\"port\":17020}],\"selector\":{\"name\":\"glsserver2-public02\"}},\"apiVersion\":\"v1\",\"metadata\":{\"labels\":{\"glsServer\":\"glsserver2\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"public02\",\"type\":\"CCODDomainModule\",\"name\":\"glsserver2-public02\"},\"namespace\":\"test29\",\"name\":\"glsserver2-public02\"}},{\"kind\":\"Service\",\"spec\":{\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\",\"ports\":[{\"targetPort\":8080,\"protocol\":\"TCP\",\"name\":\"Null\",\"port\":80}],\"selector\":{\"name\":\"cas1-manage01\"}},\"apiVersion\":\"v1\",\"metadata\":{\"labels\":{\"cas\":\"cas1\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"name\":\"cas1-manage01\"},\"namespace\":\"test29\",\"name\":\"cas1-manage01\"}},{\"kind\":\"Service\",\"spec\":{\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\",\"ports\":[{\"targetPort\":8080,\"protocol\":\"TCP\",\"name\":\"Null\",\"port\":80}],\"selector\":{\"name\":\"cas2-manage01\"}},\"apiVersion\":\"v1\",\"metadata\":{\"labels\":{\"cas\":\"cas2\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"name\":\"cas2-manage01\"},\"namespace\":\"test29\",\"name\":\"cas2-manage01\"}}],\"k8sDeploymentList\":[{\"kind\":\"Deployment\",\"spec\":{\"selector\":{\"matchLabels\":{\"glsServer\":\"glsserver1\",\"domain-id\":\"public01\",\"name\":\"glsserver1-public01\"}},\"replicas\":1,\"template\":{\"spec\":{\"terminationGracePeriodSeconds\":600,\"initContainers\":[{\"name\":\"glsserver1\",\"image\":\"nexus.io:5000/ccod/glsserver:7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"args\":[\"cp /opt/Glsserver /binary-file/;\"],\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"}],\"command\":[\"/bin/sh\",\"-c\"],\"imagePullPolicy\":\"IfNotPresent\",\"resources\":{\"requests\":{\"cpu\":\"100m\",\"memory\":\"100Mi\"},\"limits\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}}}],\"containers\":[{\"livenessProbe\":{\"tcpSocket\":{\"port\":\"17020\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"3\",\"periodSeconds\":\"30\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"name\":\"running\",\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"args\":[\"mkdir -p /root/Platform/bin;cd /root/Platform; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_config.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_logger.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/publicConfig/tnsnames.ora -P /usr/local/lib -N; cd /root/Platform/bin/;/root/Platform/bin/Glsserver;\"],\"volumeMounts\":[{\"mountPath\":\"/root/Platform/bin\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"}],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"imagePullPolicy\":\"IfNotPresent\",\"readinessProbe\":{\"tcpSocket\":{\"port\":\"17020\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"60\",\"periodSeconds\":\"10\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"ports\":[{\"protocol\":\"TCP\",\"containerPort\":\"17020\"}],\"resources\":{\"requests\":{\"cpu\":\"100m\",\"memory\":\"100Mi\"},\"limits\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}}}],\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/test29/public01/glsserver1\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"\"},\"name\":\"core\"}]},\"metadata\":{\"labels\":{\"glsServer\":\"glsserver1\",\"domain-id\":\"public01\",\"name\":\"glsserver1-public01\"}}},\"strategy\":{\"type\":\"Recreate\"}},\"apiVersion\":\"apps/v1\",\"metadata\":{\"labels\":{\"glsServer\":\"glsserver1\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"public01\",\"type\":\"CCODDomainModule\",\"name\":\"glsserver1-public01\"},\"namespace\":\"test29\",\"name\":\"glsserver1-public01\"}},{\"kind\":\"Deployment\",\"spec\":{\"selector\":{\"matchLabels\":{\"glsServer\":\"glsserver2\",\"domain-id\":\"public02\",\"name\":\"glsserver2-public02\"}},\"replicas\":1,\"template\":{\"spec\":{\"terminationGracePeriodSeconds\":600,\"initContainers\":[{\"name\":\"glsserver2\",\"image\":\"nexus.io:5000/ccod/glsserver:7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"args\":[\"cp /opt/Glsserver /binary-file/;\"],\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"}],\"command\":[\"/bin/sh\",\"-c\"],\"imagePullPolicy\":\"IfNotPresent\",\"resources\":{\"requests\":{\"cpu\":\"100m\",\"memory\":\"100Mi\"},\"limits\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}}}],\"containers\":[{\"livenessProbe\":{\"tcpSocket\":{\"port\":\"17020\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"3\",\"periodSeconds\":\"30\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"name\":\"running\",\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"args\":[\"mkdir -p /root/Platform/bin;cd /root/Platform; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_config.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_logger.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/publicConfig/tnsnames.ora -P /usr/local/lib -N; cd /root/Platform/bin/;/root/Platform/bin/Glsserver;\"],\"volumeMounts\":[{\"mountPath\":\"/root/Platform/bin\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"}],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"imagePullPolicy\":\"IfNotPresent\",\"readinessProbe\":{\"tcpSocket\":{\"port\":\"17020\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"60\",\"periodSeconds\":\"10\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"ports\":[{\"protocol\":\"TCP\",\"containerPort\":\"17020\"}],\"resources\":{\"requests\":{\"cpu\":\"100m\",\"memory\":\"100Mi\"},\"limits\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}}}],\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/test29/public02/glsserver2\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"\"},\"name\":\"core\"}]},\"metadata\":{\"labels\":{\"glsServer\":\"glsserver2\",\"domain-id\":\"public02\",\"name\":\"glsserver2-public02\"}}},\"strategy\":{\"type\":\"Recreate\"}},\"apiVersion\":\"apps/v1\",\"metadata\":{\"labels\":{\"glsServer\":\"glsserver2\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"public02\",\"type\":\"CCODDomainModule\",\"name\":\"glsserver2-public02\"},\"namespace\":\"test29\",\"name\":\"glsserver2-public02\"}},{\"kind\":\"Deployment\",\"spec\":{\"selector\":{\"matchLabels\":{\"cas\":\"cas1\",\"domain-id\":\"manage01\",\"name\":\"cas1-manage01\"}},\"replicas\":1,\"template\":{\"spec\":{\"terminationGracePeriodSeconds\":600,\"initContainers\":[{\"name\":\"cas1\",\"image\":\"nexus.io:5000/ccod/cas:10973\",\"args\":[],\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"}],\"command\":[\"/bin/sh\",\"-c\"],\"imagePullPolicy\":\"IfNotPresent\",\"resources\":{\"requests\":{\"cpu\":\"100m\",\"memory\":\"512Mi\"},\"limits\":{\"cpu\":\"200m\",\"memory\":\"1024Mi\"}}}],\"containers\":[{\"livenessProbe\":{\"httpGet\":{\"path\":\"/cas\",\"scheme\":\"HTTP\",\"port\":\"8080\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"3\",\"periodSeconds\":\"30\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"name\":\"cas-manage01\",\"image\":\"\",\"args\":[\"keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh;tail -F /usr/local/tomcat/logs/catalina.out;\"],\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"}],\"command\":[\"/bin/sh\",\"-c\"],\"imagePullPolicy\":\"IfNotPresent\",\"readinessProbe\":{\"httpGet\":{\"path\":\"/\",\"scheme\":\"HTTP\",\"port\":\"8080\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"60\",\"periodSeconds\":\"10\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"ports\":[{\"protocol\":\"TCP\",\"containerPort\":\"8080\"}],\"resources\":{\"requests\":{\"cpu\":\"200m\",\"memory\":\"512Mi\"},\"limits\":{\"cpu\":\"500m\",\"memory\":\"1024Mi\"}}}],\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"secret\":{\"secretName\":\"\"},\"name\":\"ssl\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/test29/manage01/cas1\"},\"name\":\"ccod-runtime\"}],\"hostAliases\":[{\"ip\":\"10.130.41.218\",\"hostnames\":[\"test29.ccod.com\"]}]},\"metadata\":{\"labels\":{\"cas\":\"cas1\",\"domain-id\":\"manage01\",\"name\":\"cas1-manage01\"}}},\"strategy\":{\"type\":\"Recreate\"}},\"apiVersion\":\"apps/v1\",\"metadata\":{\"labels\":{\"cas\":\"cas1\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"name\":\"cas1-manage01\"},\"namespace\":\"test29\",\"name\":\"cas1-manage01\"}},{\"kind\":\"Deployment\",\"spec\":{\"selector\":{\"matchLabels\":{\"cas\":\"cas2\",\"domain-id\":\"manage01\",\"name\":\"cas2-manage01\"}},\"replicas\":1,\"template\":{\"spec\":{\"terminationGracePeriodSeconds\":600,\"initContainers\":[{\"name\":\"cas2\",\"image\":\"nexus.io:5000/ccod/cas:10973\",\"args\":[],\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"}],\"command\":[\"/bin/sh\",\"-c\"],\"imagePullPolicy\":\"IfNotPresent\",\"resources\":{\"requests\":{\"cpu\":\"100m\",\"memory\":\"512Mi\"},\"limits\":{\"cpu\":\"200m\",\"memory\":\"1024Mi\"}}}],\"containers\":[{\"livenessProbe\":{\"httpGet\":{\"path\":\"/cas\",\"scheme\":\"HTTP\",\"port\":\"8080\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"3\",\"periodSeconds\":\"30\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"name\":\"cas-manage01\",\"image\":\"\",\"args\":[\"keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh;tail -F /usr/local/tomcat/logs/catalina.out;\"],\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"}],\"command\":[\"/bin/sh\",\"-c\"],\"imagePullPolicy\":\"IfNotPresent\",\"readinessProbe\":{\"httpGet\":{\"path\":\"/\",\"scheme\":\"HTTP\",\"port\":\"8080\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"60\",\"periodSeconds\":\"10\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"ports\":[{\"protocol\":\"TCP\",\"containerPort\":\"8080\"}],\"resources\":{\"requests\":{\"cpu\":\"200m\",\"memory\":\"512Mi\"},\"limits\":{\"cpu\":\"500m\",\"memory\":\"1024Mi\"}}}],\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"secret\":{\"secretName\":\"\"},\"name\":\"ssl\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/test29/manage01/cas2\"},\"name\":\"ccod-runtime\"}],\"hostAliases\":[{\"ip\":\"10.130.41.218\",\"hostnames\":[\"test29.ccod.com\"]}]},\"metadata\":{\"labels\":{\"cas\":\"cas2\",\"domain-id\":\"manage01\",\"name\":\"cas2-manage01\"}}},\"strategy\":{\"type\":\"Recreate\"}},\"apiVersion\":\"apps/v1\",\"metadata\":{\"labels\":{\"cas\":\"cas2\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"name\":\"cas2-manage01\"},\"namespace\":\"test29\",\"name\":\"cas2-manage01\"}}],\"k8sPVList\":[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolume\",\"metadata\":{\"name\":\"base-volume-test29\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"capacity\":{\"storage\":\"1Gi\"},\"claimRef\":{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"name\":\"base-volume-test29\",\"namespace\":\"test29\"},\"nfs\":{\"path\":\"/home/kubernetes/volume/k8s-test/baseVolume\",\"server\":\"10.130.41.218\"},\"persistentVolumeReclaimPolicy\":\"Retain\",\"storageClassName\":\"base-volume-test29\",\"volumeMode\":\"Filesystem\"}}],\"k8sPVCList\":[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"metadata\":{\"name\":\"base-volume-test29\",\"namespace\":\"test29\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"resources\":{\"requests\":{\"storage\":\"1Gi\"}},\"storageClassName\":\"base-volume-test29\",\"volumeMode\":\"Filesystem\",\"volumeName\":\"base-volume-test29\"}}],\"createMethod\":\"MANUAL\",\"baseDataNexusRepository\":\"platform_base_data\",\"deployScriptMd5\":\"351688fcd178c23ea7ee4788bae70d32\",\"bkBizId\":26,\"deployScriptPath\":\"test29/20200630152420/platform_deploy.py\",\"ccodVersion\":\"4.1\",\"glsDBPwd\":\"ccod\",\"glsDBUser\":\"ccod\",\"glsDBType\":\"ORACLE\"}";
//            logger.debug(String.format("demoSchema=%s", jsonStr));
//            PlatformUpdateSchemaInfo schema = gson.fromJson(jsonStr, PlatformUpdateSchemaInfo.class);
//            PlatformTopologyInfo topologyInfo = createK8sPlatform(schema);
//            PlatformTopologyInfo topologyInfo = getPlatformTopologyFromK8s(schema.getPlatformName(), schema.getPlatformId(), schema.getBkBizId(), schema.getBkCloudId(), schema.getCcodVersion(), schema.getK8sHostIp(), schema.getK8sApiUrl(), schema.getK8sAuthToken(), PlatformFunction.TEST);
//            System.out.println(JSONObject.toJSONString(topologyInfo));
//            pathTest();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public PlatformTopologyInfo createDemoK8sPlatform() throws Exception {
        PlatformUpdateSchemaInfo schema = generateDemoCreateSchema("202005-test", "just-test");
        PlatformTopologyInfo topologyInfo = createK8sPlatform(schema);
        return topologyInfo;
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
//            case SCHEMA_CREATE_PLATFORM:
//                setList = new ArrayList<>();
//                idleHostList = this.paasService.queryBizIdleHost(platform.getBkBizId());
//                schema = this.platformUpdateSchemaMap.containsKey(platformId) ? this.platformUpdateSchemaMap.get(platformId) : null;
//                break;
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
            case SCHEMA_CREATE_PLATFORM:
            case SCHEMA_UPDATE_PLATFORM:
            case PLANING:
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
            case WAIT_SYNC_EXIST_PLATFORM_TO_PAAS:
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
        Map<String, Map<String, List<NexusAssetInfo>>> domainCfgMap = new HashMap<>();
        for (String domainName : domainOptMap.keySet()) {
            List<PlatformAppDeployDetailVo> deployAppList = domainAppMap.containsKey(domainName) ? domainAppMap.get(domainName) : new ArrayList<>();
            Map<String, List<NexusAssetInfo>> appCfgMap = preprocessDomainApps(platformId, domainOptMap.get(domainName), deployAppList, domainMap.get(domainName), registerApps, false);
            domainCfgMap.put(domainName, appCfgMap);
        }
        for (String domainName : domainOptMap.keySet()) {
            String domainId = domainMap.get(domainName).getDomainId();
            List<PlatformAppDeployDetailVo> deployAppList = domainAppMap.containsKey(domainName) ? domainAppMap.get(domainName) : new ArrayList<>();
            List<AssemblePo> assembleList = domainAssembleMap.containsKey(domainId) ? domainAssembleMap.get(domainId) : new ArrayList<>();
            handleDomainApps(platformId, domainMap.get(domainName), domainOptMap.get(domainName), assembleList, deployAppList, registerApps, domainCfgMap.get(domainName));
        }
        this.paasService.syncClientCollectResultToPaas(platformPo.getBkBizId(), platformId, platformPo.getBkCloudId());
        return new ArrayList<>();
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
     * @param nexusAssetMap      域模块配置文件nexus存储相关信息
     */
    private void handleDomainApps(String platformId, DomainPo domainPo, List<AppUpdateOperationInfo> domainOptList, List<AssemblePo> domainAssembleList, List<PlatformAppDeployDetailVo> domainAppList, List<AppModuleVo> registerApps, Map<String, List<NexusAssetInfo>> nexusAssetMap) {
        Map<String, List<AppModuleVo>> registerAppMap = registerApps.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        String domainId = domainPo.getDomainId();
        Map<AppUpdateOperation, List<AppUpdateOperationInfo>> optMap = domainOptList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getOperation));
        List<AppUpdateOperationInfo> updateList = optMap.containsKey(AppUpdateOperation.UPDATE) ? optMap.get(AppUpdateOperation.UPDATE) : new ArrayList<>();
        List<AppUpdateOperationInfo> addList = optMap.containsKey(AppUpdateOperation.ADD) ? optMap.get(AppUpdateOperation.ADD) : new ArrayList<>();
        List<AppUpdateOperationInfo> deleteList = optMap.containsKey(AppUpdateOperation.DELETE) ? optMap.get(AppUpdateOperation.DELETE) : new ArrayList<>();
        for (AppUpdateOperationInfo optInfo : deleteList) {
            String alias = optInfo.getAppAlias();
            String tag = String.format("DELETE %s in %s", alias, domainId);
            logger.debug(String.format("begin to %s", tag));
            int platformAppId = domainAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).get(alias).getPlatformAppId();
            logger.debug(String.format("delete platform_app_bk_module with platformAppId=%d", platformAppId));
            this.platformAppBkModuleMapper.delete(platformAppId, null, null, null);
            logger.debug(String.format("delete platform_app_cfg_file with platformAppId=%d", platformAppId));
            this.platformAppCfgFileMapper.delete(null, platformAppId);
            logger.debug(String.format("delete platform_app with platformAppId=%d", platformAppId));
            this.platformAppMapper.delete(platformAppId, null, null);
            logger.info(String.format("%s success", tag));
        }
        Map<String, AssemblePo> assembleMap = domainAssembleList.stream().collect(Collectors.toMap(AssemblePo::getTag, Function.identity()));
        for (AppUpdateOperationInfo optInfo : updateList) {
            String appName = optInfo.getAppName();
            String alias = optInfo.getAppAlias();
            String version = optInfo.getTargetVersion();
            String hostIp = optInfo.getHostIp();
            String assembleTag = optInfo.getAssembleTag();
            String tag = String.format("UPDATE %s(%s) to %s in %s on %s at %s", alias, appName, version, assembleTag, domainId, hostIp);
            logger.debug(String.format("begin to %s", tag));
            AppModuleVo module = registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version);
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
            int platformAppId = domainAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).get(alias).getPlatformApp().getPlatformAppId();
            PlatformAppPo platformAppPo = optInfo.getPlatformApp(platformAppId, module.getAppId(), platformId, domainId);
            platformAppPo.setAssembleId(assemblePo.getAssembleId());
            List<NexusAssetInfo> assetList = nexusAssetMap.get(alias);
            Map<String, AppFileNexusInfo> cfgMap = optInfo.getCfgs().stream().collect(Collectors.toMap(AppFileNexusInfo::getFileName, Function.identity()));
            logger.debug(String.format("update platform_app to %s", JSONObject.toJSONString(platformAppPo)));
            this.platformAppMapper.update(platformAppPo);
            logger.debug(String.format("delete from platform_app_cfg_file where platformAppId=%d", platformAppId));
            this.platformAppCfgFileMapper.delete(null, platformAppId);
            for (NexusAssetInfo cfg : assetList) {
                PlatformAppCfgFilePo cfgFilePo = new PlatformAppCfgFilePo(platformAppId, module.getAppId(), cfgMap.get(cfg.getNexusAssetFileName()).getDeployPath(), cfg);
                logger.debug(String.format("insert cfg[%s]", JSONObject.toJSONString(cfgFilePo)));
                this.platformAppCfgFileMapper.insert(cfgFilePo);
            }
            logger.info(String.format("%s success", tag));
        }
        for (AppUpdateOperationInfo optInfo : addList) {
            String appName = optInfo.getAppName();
            String alias = optInfo.getAppAlias();
            String version = optInfo.getTargetVersion();
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
            AppModuleVo module = registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version);
            PlatformAppPo platformAppPo = optInfo.getPlatformApp(module.getAppId(), platformId, domainId);
            platformAppPo.setAssembleId(assemblePo.getAssembleId());
            this.platformAppMapper.insert(platformAppPo);
            int platformAppId = platformAppPo.getPlatformAppId();
            List<NexusAssetInfo> assetList = nexusAssetMap.get(alias);
            Map<String, AppFileNexusInfo> cfgMap = optInfo.getCfgs().stream().collect(Collectors.toMap(AppFileNexusInfo::getFileName, Function.identity()));
            for (NexusAssetInfo cfg : assetList) {
                PlatformAppCfgFilePo cfgFilePo = new PlatformAppCfgFilePo(platformAppId, module.getAppId(), cfgMap.get(cfg.getNexusAssetFileName()).getDeployPath(), cfg);
                logger.debug(String.format("insert cfg[%s]", JSONObject.toJSONString(cfgFilePo)));
                this.platformAppCfgFileMapper.insert(cfgFilePo);
            }
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
        for (AppUpdateOperationInfo optInfo : addOptList)
            if (StringUtils.isNotBlank(optInfo.getAppAlias()))
                optInfo.setOriginalAlias(optInfo.getAppAlias());
        Map<String, List<AppUpdateOperationInfo>> addAppMap = addOptList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getAppName));
        for (String appName : addAppMap.keySet()) {
            List<String> usedAlias = new ArrayList<>();
            String standAlias = this.setDefineMap.get(domainPo.getBizSetName()).getApps().stream().collect(Collectors.toMap(AppDefine::getName, Function.identity())).get(appName).getAlias();
            if (domainAppMap.containsKey(appName)) {
                usedAlias = new ArrayList<>(domainAppMap.get(appName).stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAppAlias, Function.identity())).keySet());
            }
            List<AppUpdateOperationInfo> needAliasList = new ArrayList<>();
            for (AppUpdateOperationInfo optInfo : addAppMap.get(appName)) {
                if (StringUtils.isBlank(optInfo.getAppAlias()) || !clone)
                    needAliasList.add(optInfo);
                else
                    usedAlias.add(optInfo.getAppAlias());
            }
            boolean onlyOne = usedAlias.size() == 0 && needAliasList.size() == 1 ? true : false;
            for (AppUpdateOperationInfo optInfo : needAliasList) {
                String alias = autoGenerateAlias(standAlias, usedAlias, onlyOne);
                optInfo.setAppAlias(alias);
                usedAlias.add(alias);
            }
        }
        for (AppUpdateOperationInfo optInfo : addOptList) {
            if (StringUtils.isBlank(optInfo.getOriginalAlias()))
                optInfo.setOriginalAlias(optInfo.getAppAlias());
        }
    }

    private void updateDeployApp()
    {
        Map<Integer, AppModuleVo> registerAppMap = this.appManagerService.queryAllRegisterAppModule(null).stream().collect(Collectors.toMap(AppModuleVo::getAppId, Function.identity()));
        List<PlatformAppPo> platformApps = this.platformAppMapper.select(null, null, null, null, null, null);
        for(PlatformAppPo  po : platformApps)
        {
            if(StringUtils.isNotBlank(po.getStartCmd()) && StringUtils.isNotBlank(po.getDeployPath()))
                continue;
            if(!registerAppMap.containsKey(po.getAppId()))
                logger.error(String.format("%d is illegal appId", po.getAppId()));
            po.setStartCmd(registerAppMap.get(po.getAppId()).getStartCmd());
            po.setDeployPath(registerAppMap.get(po.getAppId()).getDeployPath());
            this.platformAppMapper.update(po);
        }
    }

    private void updateRegisterApp()
    {
        List<AppModuleVo> registerApps = this.appManagerService.queryAllRegisterAppModule(null);
        for(AppModuleVo moduleVo : registerApps) {
            AppPo app = moduleVo.getApp();
            if(app.getAppType().equals(AppType.BINARY_FILE.name)
                    && app.getStartCmd().trim().split("\\s").length == 1)
            {
                app.setStartCmd(String.format("%s;sleep 5;tailf ../log/*/*.log", app.getStartCmd()));
                this.appMapper.update(app);
            }
        }
    }

    private void updateRegisterApp(List<AppUpdateOperationInfo> optList)
    {
        List<AppModuleVo> registerApps = this.appManagerService.queryAllRegisterAppModule(null);
        Map<String, List<AppUpdateOperationInfo>> optMap = optList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
        for(AppModuleVo moduleVo : registerApps)
        {
            AppPo app = moduleVo.getApp();
            if(AppType.BINARY_FILE.equals(moduleVo.getAppType()))
            {
                app.setBasePath(String.format("/home/%s", app.getAppName().toLowerCase()));
                app.setDeployPath("./bin");
                app.setStartCmd(String.format("./%s", moduleVo.getInstallPackage().getFileName()));
            }
            else if(AppType.RESIN_WEB_APP.equals(moduleVo.getAppType()))
            {
                app.setBasePath("/root/resin-4.0.13");
                app.setDeployPath("./webapps");
                app.setStartCmd("keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/jre/lib/security/cacerts;./bin/resin.sh start;tail -F ./log/jvm-default.log");
            }
            else
            {
                app.setBasePath("/usr/local/tomcat");
                app.setDeployPath("./webapps");
                app.setStartCmd("keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;./bin/startup.sh;tail -F ./logs/catalina.out");
            }
            if(optMap.containsKey(app.getAppName()))
            {
                app.setBasePath(optMap.get(app.getAppName()).get(0).getBasePath());
                app.setDeployPath(optMap.get(app.getAppName()).get(0).getDeployPath());
                app.setStartCmd(optMap.get(app.getAppName()).get(0).getStartCmd());
            }
        }
    }

    private void updateRegisterAppCfgs(List<AppUpdateOperationInfo> optList)
    {
        List<AppModuleVo> registerApps = this.appManagerService.queryAllRegisterAppModule(null);
        Map<String, List<AppModuleVo>> appMap = registerApps.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        for(AppUpdateOperationInfo optInfo : optList)
        {
            for(AppModuleVo module : appMap.get(optInfo.getAppName()))
            {
                Map<String, AppCfgFilePo> cfgMap = module.getCfgs().stream().collect(Collectors.toMap(AppCfgFilePo::getFileName, Function.identity()));
                for(AppFileNexusInfo cfg : optInfo.getCfgs())
                {
                    if(cfgMap.containsKey(cfg.getFileName()))
                    {
                        AppCfgFilePo cfgFilePo = cfgMap.get(cfg.getFileName());
                        cfgFilePo.setDeployPath(cfg.getDeployPath());
                        this.appCfgFileMapper.update(cfgFilePo);
                    }
                    else
                        logger.error(String.format("%s is not cfg file of %s with version %s", cfg.getFileName(), module.getAppName(), module.getVersion()));
                }
                Map<String, AppFileNexusInfo> existMap = optInfo.getCfgs().stream().collect(Collectors.toMap(AppFileNexusInfo::getFileName, Function.identity()));
                for(AppCfgFilePo cfgFilePo : module.getCfgs())
                {
                    if(!existMap.containsKey(cfgFilePo.getFileName()))
                        logger.error(String.format("deploy path of cfg file %s of %s(%s) is unconfirmed", cfgFilePo.getFileName(), module.getAppName(), module.getVersion()));
                }
            }
        }
        Map<String, List<AppUpdateOperationInfo>> optMap = optList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppName));
        for(AppModuleVo moduleVo : registerApps)
        {
            AppPo app = moduleVo.getApp();
            if(AppType.BINARY_FILE.equals(moduleVo.getAppType()))
            {
                app.setBasePath(String.format("/home/%s", app.getAppName().toLowerCase()));
                app.setDeployPath("./bin");
                app.setStartCmd(String.format("./%s", moduleVo.getInstallPackage().getFileName()));
            }
            else if(AppType.RESIN_WEB_APP.equals(moduleVo.getAppType()))
            {
                app.setBasePath("/root/resin-4.0.13");
                app.setDeployPath("./webapps");
                app.setStartCmd("keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/jre/lib/security/cacerts;./bin/resin.sh start;tail -F ./log/jvm-default.log");
            }
            else
            {
                app.setBasePath("/usr/local/tomcat");
                app.setDeployPath("./webapps");
                app.setStartCmd("keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;./bin/startup.sh;tail -F ./logs/catalina.out");
            }
            if(optMap.containsKey(app.getAppName()))
            {
                app.setBasePath(optMap.get(app.getAppName()).get(0).getBasePath());
                app.setDeployPath(optMap.get(app.getAppName()).get(0).getDeployPath());
                app.setStartCmd(optMap.get(app.getAppName()).get(0).getStartCmd());
            }
            this.appMapper.update(app);
        }
    }

    /**
     * 为指定域新加的应用生成别名，并且下载添加/更新的应用的配置文件到指定nexus路径
     *
     * @param platformId    平台id
     * @param domainOptList 指定域的应用的相关操作
     * @param deployAppList 指定域已经部署的所有应用
     * @param domainPo      指定域
     * @param registerApps  已经注册应用列表
     * @param clone         是否通过clone方式获得
     * @return 添加/更新的应用的配置文件的nexus存放信息
     * @throws ParamException
     * @throws InterfaceCallException
     * @throws NexusException
     * @throws IOException
     */
    private Map<String, List<NexusAssetInfo>> preprocessDomainApps(String platformId, List<AppUpdateOperationInfo> domainOptList, List<PlatformAppDeployDetailVo> deployAppList, DomainPo domainPo, List<AppModuleVo> registerApps, boolean clone) throws ParamException, InterfaceCallException, NexusException, IOException {
        String domainId = domainPo.getDomainId();
        Map<String, List<NexusAssetInfo>> assetMap = new HashMap<>();
        Map<AppUpdateOperation, List<AppUpdateOperationInfo>> optMap = domainOptList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getOperation));
        List<AppUpdateOperationInfo> updateList = optMap.containsKey(AppUpdateOperation.UPDATE) ? optMap.get(AppUpdateOperation.UPDATE) : new ArrayList<>();
        List<AppUpdateOperationInfo> addList = optMap.containsKey(AppUpdateOperation.ADD) ? optMap.get(AppUpdateOperation.ADD) : new ArrayList<>();
        Map<String, List<AppModuleVo>> registerAppMap = registerApps.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        if (addList.size() > 0)
            generateAlias4DomainApps(domainPo, addList, deployAppList, clone);
        for (AppUpdateOperationInfo optInfo : addList) {
            if (StringUtils.isBlank(optInfo.getAssembleTag()))
                optInfo.setAssembleTag(optInfo.getAppAlias());
            AppModuleVo module = registerAppMap.get(optInfo.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(optInfo.getTargetVersion());
            PlatformAppPo platformAppPo = optInfo.getPlatformApp(0, module.getAppId(), platformId, domainId);
            String directory = platformAppPo.getPlatformAppDirectory(module.getAppName(), module.getVersion(), platformAppPo);
            List<AppFilePo> fileList = new ArrayList<>();
            for (AppFileNexusInfo cfg : optInfo.getCfgs()) {
                AppFilePo filePo = new AppFilePo(module.getAppId(), cfg);
                fileList.add(filePo);
            }
            logger.debug(String.format("download cfg of %s at %s and upload to %s/%s", optInfo.getAppAlias(), domainId, this.platformAppCfgRepository, directory));
            List<NexusAssetInfo> assetList = appManagerService.downloadAndUploadAppFiles(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, fileList, this.platformAppCfgRepository, directory);
            assetMap.put(optInfo.getAppAlias(), assetList);
        }
        for (AppUpdateOperationInfo optInfo : updateList) {
            if (StringUtils.isBlank(optInfo.getAssembleTag()))
                optInfo.setAssembleTag(deployAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).get(optInfo.getAppAlias()).getAppAlias());
            AppModuleVo module = registerAppMap.get(optInfo.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(optInfo.getTargetVersion());
            int platformAppId = deployAppList.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getOriginalAlias, Function.identity())).get(optInfo.getAppAlias()).getPlatformApp().getPlatformAppId();
            PlatformAppPo platformAppPo = optInfo.getPlatformApp(platformAppId, module.getAppId(), platformId, domainId);
            String directory = platformAppPo.getPlatformAppDirectory(module.getAppName(), module.getVersion(), platformAppPo);
            List<AppFilePo> fileList = new ArrayList<>();
            for (AppFileNexusInfo cfg : optInfo.getCfgs()) {
                AppFilePo filePo = new AppFilePo(module.getAppId(), cfg);
                fileList.add(filePo);
            }
            logger.debug(String.format("download cfg of %s at %s and upload to %s/%s", optInfo.getAppAlias(), domainId, this.platformAppCfgRepository, directory));
            List<NexusAssetInfo> assetList = appManagerService.downloadAndUploadAppFiles(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, fileList, this.platformAppCfgRepository, directory);
            assetMap.put(optInfo.getAppAlias(), assetList);
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
            String alias = optInfo.getAppAlias();
            String version = optInfo.getTargetVersion();
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

    @Override
    public K8sPlatformSchemaInfo createK8sPlatformSchema(K8sPlatformSchemaInfo schemaInfo) throws Exception {
//        logger.debug(String.format("begin to update platform update schema : %s", JSONObject.toJSONString(schemaInfo)));
//        if (StringUtils.isBlank(schemaInfo.getPlatformId())) {
//            logger.error("platformId of schema is blank");
//            throw new ParamException("platformId of schema is blank");
//        }
//        if (StringUtils.isBlank(schemaInfo.getPlatformName())) {
//            logger.error("platformName of schema is blank");
//            throw new ParamException("platformName of schema is blank");
//        }
//        LJBizInfo bkBiz = paasService.queryBizInfoById(schemaInfo.getBkBizId());
//        if (bkBiz == null) {
//            logger.error(String.format("bkBizId=%d biz not exist", schemaInfo.getBkBizId()));
//            throw new ParamException(String.format("bkBizId=%d biz not exist", schemaInfo.getBkBizId()));
//        }
//        if (!schemaInfo.getPlatformName().equals(bkBiz.getBkBizName())) {
//            logger.error(String.format("bkBizName of bizBkId is %s, not %s", schemaInfo.getBkBizId(), bkBiz.getBkBizName(), schemaInfo.getPlatformName()));
//            throw new ParamException(String.format("bkBizName of bizBkId is %s, not %s", schemaInfo.getBkBizId(), bkBiz.getBkBizName(), schemaInfo.getPlatformName()));
//        }
//        PlatformPo platformPo = platformMapper.selectByPrimaryKey(schemaInfo.getPlatformId());
//        if (platformPo == null) {
//            if (!schemaInfo.getStatus().equals(UpdateStatus.CREATE)) {
//                logger.error(String.format("%s platform %s not exist", schemaInfo.getStatus().name, schemaInfo.getPlatformId()));
//                throw new ParamException(String.format("%s platform %s not exist", schemaInfo.getStatus().name, schemaInfo.getPlatformId()));
//            } else {
//                platformPo = new PlatformPo(schemaInfo.getPlatformId(), schemaInfo.getPlatformName(),
//                        schemaInfo.getBkBizId(), schemaInfo.getBkCloudId(), CCODPlatformStatus.SCHEMA_CREATE_PLATFORM,
//                        schemaInfo.getCcodVersion(), "create by platform create schema", schemaInfo.getPlatformType(), schemaInfo.getPlatformFunc(), schemaInfo.getCreateMethod());
//                platformPo.setApiUrl(schemaInfo.getK8sApiUrl());
//                platformPo.setAuthToken(schemaInfo.getK8sAuthToken());
//                if (StringUtils.isNotBlank(schemaInfo.getCcodVersion())) {
//                    platformPo.setCcodVersion("CCOD4.1");
//                }
//                this.platformMapper.insert(platformPo);
//            }
//        }
//        if (!platformPo.getPlatformName().equals(schemaInfo.getPlatformName())) {
//            logger.error(String.format("platformName of %s is %s, not %s",
//                    platformPo.getPlatformId(), platformPo.getPlatformName(), schemaInfo.getPlatformName()));
//            throw new ParamException(String.format("bkBizName of bizBkId is %s, not %s", schemaInfo.getBkBizId(), bkBiz.getBkBizName(), schemaInfo.getPlatformName()));
//        }
//        List<DomainPo> domainList = this.domainMapper.select(schemaInfo.getPlatformId(), null);
//        logger.debug("begin check param of schema");
//        checkPlatformUpdateSchema(schemaInfo, domainList);
//        logger.debug("schema param check success");
//        boolean clone = PlatformCreateMethod.CLONE.equals(schemaInfo.getCreateMethod()) ? true : false;
//        List<PlatformAppDeployDetailVo> platformDeployApps = this.platformAppDeployDetailMapper.selectPlatformApps(schemaInfo.getPlatformId(), null, null);
//        List<LJHostInfo> bkHostList = this.paasService.queryBKHost(schemaInfo.getBkBizId(), null, null, null, null);
//        Map<String, DomainPo> domainMap = domainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
//        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = platformDeployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
//        Map<String, List<AssemblePo>> domainAssembleMap = this.assembleMapper.select(schemaInfo.getPlatformId(), null).stream().collect(Collectors.groupingBy(AssemblePo::getDomainId));
//        List<AppModuleVo> registerApps = this.appManagerService.queryAllRegisterAppModule(null);
//        List<DomainUpdatePlanInfo> planList = new ArrayList<>();
//        List<DomainUpdatePlanInfo> successList = new ArrayList<>();
//        makeupDomainIdAndAliasForSchema(schemaInfo, domainList, platformDeployApps, clone);
//        for (DomainUpdatePlanInfo plan : schemaInfo.getDomainUpdatePlanList()) {
//            String domainId = plan.getDomainId();
//            DomainPo domainPo = StringUtils.isNotBlank(plan.getDomainId()) && domainMap.containsKey(plan.getDomainId()) ? domainMap.get(domainId) : plan.getDomain(schemaInfo.getPlatformId());
//            List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
//            logger.debug(String.format("check %s %d apps with isCreate=%b and %d deployed apps",
//                    JSONObject.toJSONString(domainPo), plan.getAppUpdateOperationList().size(), StringUtils.isNotBlank(plan.getDomainId()) && domainMap.containsKey(plan.getDomainId()), domainAppList.size()));
//            checkDomainApps(plan.getAppUpdateOperationList(), domainAppList, domainPo, registerApps, bkHostList);
//            if (plan.getStatus().equals(UpdateStatus.SUCCESS)) {
//                successList.add(plan);
//            } else
//                planList.add(plan);
//        }
//        logger.debug(String.format("generate platform app deploy param and script"));
//        generatePlatformDeployParamAndScript(schemaInfo);
//        if (schemaInfo.getStatus().equals(UpdateStatus.SUCCESS) && planList.size() > 0) {
//            logger.error(String.format("status of %s(%s) update schema is SUCCESS, but there are %d domain update plan not execute",
//                    schemaInfo.getPlatformName(), schemaInfo.getPlatformId(), planList.size()));
//            throw new ParamException(String.format("status of %s(%s) update schema is SUCCESS, but there are %d domain update plan not execute",
//                    schemaInfo.getPlatformName(), schemaInfo.getPlatformId(), planList.size()));
//        }
//        logger.debug(String.format("generate domainId for new add with status SUCCESS and id is blank domain"));
//        Map<String, Map<String, List<NexusAssetInfo>>> domainCfgMap = new HashMap<>();
//        for (DomainUpdatePlanInfo plan : successList) {
//            String domainId = plan.getDomainId();
//            boolean isCreate = domainMap.containsKey(plan.getDomainId()) ? false : true;
//            DomainPo domainPo = StringUtils.isNotBlank(plan.getDomainId()) && domainMap.containsKey(plan.getDomainId()) ? domainMap.get(domainId) : plan.getDomain(schemaInfo.getPlatformId());
//            List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
//            logger.debug(String.format("preprocess %s %d apps with isCreate=%b and %d deployed apps",
//                    JSONObject.toJSONString(domainPo), plan.getAppUpdateOperationList().size(), isCreate, domainAppList.size()));
//            Map<String, List<NexusAssetInfo>> cfgMap = preprocessDomainApps(schemaInfo.getPlatformId(), plan.getAppUpdateOperationList(), domainAppList, domainPo, registerApps, clone);
//            domainCfgMap.put(domainId, cfgMap);
//        }
//        for (DomainUpdatePlanInfo plan : successList) {
//            String domainId = plan.getDomainId();
//            boolean isCreate = domainMap.containsKey(plan.getDomainId()) ? false : true;
//            DomainPo domainPo = isCreate ? plan.getDomain(schemaInfo.getPlatformId()) : domainMap.get(domainId);
//            if (isCreate)
//                this.domainMapper.insert(domainPo);
//            List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
//            List<AssemblePo> assembleList = domainAssembleMap.containsKey(domainId) ? domainAssembleMap.get(domainId) : new ArrayList<>();
//            logger.debug(String.format("handle %s %d apps with isCreate=%b and %d deployed apps",
//                    JSONObject.toJSONString(domainPo), plan.getAppUpdateOperationList().size(), isCreate, domainAppList.size()));
//            handleDomainApps(schemaInfo.getPlatformId(), domainPo, plan.getAppUpdateOperationList(), assembleList, domainAppList, registerApps, domainCfgMap.get(domainId));
//        }
//        if (schemaInfo.getStatus().equals(UpdateStatus.SUCCESS)) {
//            logger.debug(String.format("%s(%s) platform complete deployment, so remove schema and set status to RUNNING", schemaInfo.getPlatformName(), schemaInfo.getPlatformId()));
//            if (this.platformschemaInfoMap.containsKey(schemaInfo.getPlatformId()))
//                this.platformschemaInfoMap.remove(schemaInfo.getPlatformId());
//            this.platformschemaInfoMapper.delete(schemaInfo.getPlatformId());
//            platformPo.setStatus(CCODPlatformStatus.RUNNING.id);
//            this.platformMapper.update(platformPo);
//        } else {
//            logger.debug(String.format("%s(%s) platform not complete deployment, so update schema and set status to PLANING", schemaInfo.getPlatformName(), schemaInfo.getPlatformId()));
//            schemaInfo.setDomainUpdatePlanList(planList);
//            PlatformschemaInfoPo schemaPo = new PlatformschemaInfoPo();
//            schemaPo.setContext(JSONObject.toJSONString(schemaInfo).getBytes());
//            schemaPo.setPlatformId(schemaInfo.getPlatformId());
//            this.platformschemaInfoMapper.delete(schemaInfo.getPlatformId());
//            this.platformschemaInfoMapper.insert(schemaPo);
//            this.platformschemaInfoMap.put(schemaInfo.getPlatformId(), schemaInfo);
//            platformPo.setStatus(CCODPlatformStatus.SCHEMA_CREATE_PLATFORM.id);
//            this.platformMapper.update(platformPo);
//        }
//        if (successList.size() > 0) {
//            logger.debug(String.format("%d domain complete deployment, so sync new platform topo to lj paas", successList.size()));
//            this.paasService.syncClientCollectResultToPaas(platformPo.getBkBizId(), platformPo.getPlatformId(), platformPo.getBkCloudId());
//        }
        return null;
    }

    @Override
    public void deletePlatformUpdateSchema(String platformId) throws ParamException {
        logger.debug(String.format("begin to delete platform update schema of %s", platformId));
        PlatformPo platformPo = platformMapper.selectByPrimaryKey(platformId);
        if (platformPo == null) {
            logger.error(String.format("%s platform not exist", platformId));
            throw new ParamException(String.format("%s platform not exist", platformId));
        }
        CCODPlatformStatus status = CCODPlatformStatus.getEnumById(platformPo.getStatus());
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
                            cfg.setFileSize(fileInfo.getFileSize());
                            cfg.setMd5(fileInfo.getFileMd5());
                            cfg.setNexusAssetId(fileInfo.getNexusAssetId());
                            cfg.setNexusPath(String.format("%s/%s", fileInfo.getNexusDirectory(), fileInfo.getFileName()));
                            cfg.setNexusRepository(fileInfo.getNexusRepository());
                            cfgs.add(cfg);
                        }
                    }
                    optInfo.setCfgs(cfgs);
                    optInfo.setAppRunner(moduleVo.getLoginUser());
                    optInfo.setTargetVersion(moduleVo.getVersion());
                    optInfo.setAppName(moduleVo.getModuleName());
                    optInfo.setBasePath(moduleVo.getBasePath());
                    optInfo.setHostIp(moduleVo.getHostIp());
                    optInfo.setDomainId(domainId);
                    if (isAdd) {
                        optInfo.setAppAlias(moduleVo.getModuleAliasName());
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
            platformPo.setStatus(CCODPlatformStatus.RUNNING.id);
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
                throw new K8sDataException(String.format("service %s with labels %s is unknown", service.getMetadata().getName(), JSONObject.toJSONString(service.getMetadata().getLabels())));
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
        platform.setStatus(CCODPlatformStatus.RUNNING.id);
        platform.setPlatformId(platformId);
        platform.setPlatformName(platformName);
        platform.setBkCloudId(bkCloudId);
        platform.setBkBizId(bkBizId);
        platform.setUpdateTime(now);
        platform.setCreateTime(now);
        platform.setComment("create by k8s api");
        platform.setCcodVersion(ccodVersion);
        platform.setApiUrl(k8sApiUrl);
        platform.setAuthToken(k8sAuthToken);
        platform.setType(PlatformType.K8S_CONTAINER);
        platform.setFunc(func);
        platformMapper.insert(platform);
        for (DomainPo domainPo : allDomains) {
            logger.debug(String.format("insert new domain : %s", JSONObject.toJSONString(domainPo)));
            this.domainMapper.insert(domainPo);
        }
        for (AssemblePo assemblePo : domainAssembleMap.values().stream().flatMap(listContainer -> listContainer.stream()).collect(Collectors.toList())) {
            logger.debug(String.format("insert new assemble : %s", JSONObject.toJSONString(assemblePo)));
            this.assembleMapper.insert(assemblePo);
        }
        for (PlatformAppDeployDetailVo deployApp : deployAppList) {
            PlatformAppPo platformAppPo = deployApp.getPlatformApp();
            platformAppPo.setAssembleId(domainAssembleMap.get(deployApp.getDomainId()).stream().collect(Collectors.toMap(AssemblePo::getTag, Function.identity())).get(deployApp.getAssembleTag()).getAssembleId());
            logger.debug(String.format("insert new platform app : %s", JSONObject.toJSONString(platformAppPo)));
            this.platformAppMapper.insert(platformAppPo);
            for (PlatformAppCfgFilePo cfg : deployApp.getCfgs()) {
                cfg.setPlatformAppId(platformAppPo.getPlatformAppId());
                logger.debug(String.format("insert new cfg : %s", JSONObject.toJSONString(cfg)));
                this.platformAppCfgFileMapper.insert(cfg);
            }
        }
        for (PlatformThreePartAppPo threePartAppPo : threeAppList) {
            logger.debug(String.format("insert new platform three part app : %s", JSONObject.toJSONString(threePartAppPo)));
            this.platformThreePartAppMapper.insert(threePartAppPo);
        }
        for (PlatformThreePartServicePo threePartServicePo : threePartSvcList) {
            logger.debug(String.format("insert new platform three part service : %s", JSONObject.toJSONString(threePartServicePo)));
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
        deployApp.setAppAlias(alias);
        deployApp.setAppId(appModule.getAppId());
        deployApp.setAppName(appModule.getAppName());
        deployApp.setAppRunner(deployApp.getAppAlias());
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
        deployApp.setOriginalAlias(deployApp.getAppAlias());
        deployApp.setPlatformAppId(0);
        String port = getPortFromK8sService(services);
        deployApp.setPort(port);
        deployApp.setSrcCfgs(appModule.getCfgs());
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
        List<PlatformAppCfgFilePo> cfgs = new ArrayList<>();
        for (NexusAssetInfo assetInfo : assets) {
            PlatformAppCfgFilePo cfgFilePo = new PlatformAppCfgFilePo(appModule.getAppId(), cfgComandMap.get(assetInfo.getNexusAssetFileName()), assetInfo);
            cfgs.add(cfgFilePo);
        }
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

    private K8sPlatformParamVo generateK8sPlatformParam(String platformId, String platformName, String ccodVersion, String k8sApiUrl, String k8sAuthToken) throws IOException, K8sDataException, ApiException, NotSupportAppException, InterfaceCallException, NexusException, ParamException {
        logger.debug(String.format("generate %s[%s] topology from %s", platformName, platformId, k8sApiUrl));
        List<V1Deployment> deployments = this.k8sApiService.listNamespacedDeployment(platformId, k8sApiUrl, k8sAuthToken);
        List<V1Service> services = this.k8sApiService.listNamespacedService(platformId, k8sApiUrl, k8sAuthToken);
        List<V1ConfigMap> configMaps = this.k8sApiService.listNamespacedConfigMap(platformId, k8sApiUrl, k8sAuthToken);
        List<AppModuleVo> registerApps = this.appManagerService.queryAllRegisterAppModule(true);
        K8sPlatformParamVo paramVo = getK8sPlatformParam(platformId, platformName, ccodVersion, deployments, services, configMaps, registerApps);
        return paramVo;
    }

    private PlatformAppDeployDetailVo parsePlatformDeployApp(V1Deployment deployment, V1Container container, List<V1Pod> pods, List<V1Service> services, String deploymentName, String platformId, String platformName, List<AppModuleVo> registerApps, int bkBizId, int bkCloudId, String ccodVersion, List<LJHostInfo> hostList, String status) throws ParamException, NotSupportAppException {
        String domainId = deploymentName.split("\\-")[1];
        BizSetDefine setDefine = getBizSetForDomainId(domainId);
        AppModuleVo moduleVo = getAppModuleFromImageTag(container.getImage(), registerApps);
        PlatformAppDeployDetailVo deployApp = new PlatformAppDeployDetailVo();
        deployApp.setBkSetName(setDefine.getName());
        deployApp.setBkBizId(bkBizId);
        deployApp.setDomainName(String.format("%s%s", setDefine.getFixedDomainName(), domainId.replaceAll(setDefine.getFixedDomainId(), "")));
        deployApp.setDomainId(domainId);
        deployApp.setAppAlias(container.getName().split("\\-")[0]);
        deployApp.setAppId(moduleVo.getAppId());
        deployApp.setAppName(moduleVo.getAppName());
        deployApp.setAppRunner(deployApp.getAppAlias());
        deployApp.setAppType(moduleVo.getAppType());
        deployApp.setAssembleTag(deployment.getSpec().getSelector().getMatchLabels().get("name"));
        deployApp.setAssembleId(0);
        if (AppType.BINARY_FILE.equals(deployApp.getAppType()))
            deployApp.setBasePath("/binary-file");
        else
            deployApp.setBasePath("/war");
        String hostIp = pods.get(0).getStatus().getHostIP();
        LJHostInfo host = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity())).get(hostIp);
        deployApp.setBkHostId(host.getBkHostId());
        deployApp.setBkModuleId(0);
        deployApp.setBkSetId(0);
        deployApp.setCcodVersion(ccodVersion);
        deployApp.setCfgs(new ArrayList<>());
        Date now = new Date();
        deployApp.setCreateTime(now);
        deployApp.setDeployTime(now);
        deployApp.setHostIp(hostIp);
        deployApp.setHostname(host.getOsName());
        deployApp.setInstallPackage(moduleVo.getInstallPackage());
        deployApp.setOriginalAlias(deployApp.getAppAlias());
        deployApp.setPlatformAppId(0);
        String port = getPortFromK8sService(services);
        deployApp.setPort(port);
        deployApp.setPlatformId(platformId);
        deployApp.setPlatformName(platformName);
        deployApp.setSrcCfgs(moduleVo.getCfgs());
        deployApp.setStatus(status);
        deployApp.setVersion(moduleVo.getVersion());
        deployApp.setVersionControl(moduleVo.getVersionControl().name);
        deployApp.setVersionControlUrl(moduleVo.getVersionControlUrl());
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
        logger.debug(String.format("three part app %s : %s", appName, JSONObject.toJSONString(po)));
        return po;
    }

    /**
     * 获取平台部署的ccod应用信息
     *
     * @param namespace       k8s的namespace也就是平台的id
     * @param service         ccod应用对应的k8s服务
     * @param svrPodList      ccod应用选择的pod列表
     * @param registerAppList 已经向cmdb注册的应用列表
     * @param allDomains      平台下所有域
     * @param allAssembles    平台下所有assembles
     * @return ccod应用模块的部署详情
     * @throws ParamException
     */
    private PlatformAppModuleParam getPlatformAppParam4FromK8s(String namespace, V1Service service, List<V1Pod> svrPodList, List<DomainPo> allDomains, List<AssemblePo> allAssembles, List<AppModuleVo> registerAppList) throws ParamException {
        logger.debug(String.format("get platform app from service %s with %d pods at %s", service.getMetadata().getName(), svrPodList.size(), namespace));
        if (svrPodList.size() != 1)
            throw new ParamException(String.format("domain service %s select %d pods, current version not support", service.getMetadata().getName(), svrPodList.size()));
        V1Pod pod = svrPodList.get(0);
        Date now = new Date();
        String alias = service.getMetadata().getName().split("\\-")[0];
        String domainId = service.getMetadata().getName().split("\\-")[1];
        BizSetDefine setDefine = null;
        AppDefine appDefine = null;
        for (BizSetDefine set : this.ccodBiz.getSet()) {
            String domainRegex = String.format("^%s(0[1-9]|[1-9]\\d+)", set.getFixedDomainId());
            if (domainId.matches(domainRegex)) {
                setDefine = set;
                for (AppDefine app : set.getApps()) {
                    String aliasRegex = String.format("^%s\\d*$", app.getAlias());
                    if (alias.matches(aliasRegex)) {
                        appDefine = app;
                        break;
                    }
                }
                break;
            }
        }
        DomainPo domainPo;
        if (!allDomains.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity())).containsKey(domainId)) {
            domainPo = new DomainPo();
            domainPo.setUpdateTime(now);
            domainPo.setDomainName(String.format("%s%s", setDefine.getFixedDomainName(), domainId.replaceAll(setDefine.getFixedDomainId(), "")));
            domainPo.setCreateTime(now);
            domainPo.setStatus(DomainStatus.RUNNING.id);
            domainPo.setPlatformId(namespace);
            domainPo.setComment("created from k8s api");
            domainPo.setMaxOccurs(800);
            domainPo.setOccurs(600);
            domainPo.setTags(appDefine.getName());
            domainPo.setUpdateTime(now);
            domainPo.setBizSetName(setDefine.getName());
            domainPo.setDomainId(domainId);
            allDomains.add(domainPo);
        } else
            domainPo = allDomains.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity())).get(domainId);
        String assembleTag = pod.getMetadata().getName().split("\\-")[0];
        AssemblePo assemblePo;
        if (allAssembles.stream().collect(Collectors.groupingBy(AssemblePo::getDomainId)).containsKey(domainId)
                && allAssembles.stream().collect(Collectors.groupingBy(AssemblePo::getDomainId)).get(domainId).stream().collect(Collectors.toMap(AssemblePo::getTag, Function.identity())).containsKey(assembleTag))
            assemblePo = allAssembles.stream().collect(Collectors.groupingBy(AssemblePo::getDomainId)).get(domainId)
                    .stream().collect(Collectors.toMap(AssemblePo::getTag, Function.identity())).get(assembleTag);
        else {
            assemblePo = new AssemblePo();
            assemblePo.setDomainId(domainId);
            assemblePo.setPlatformId(namespace);
            assemblePo.setStatus(pod.getStatus().getPhase());
            assemblePo.setTag(assembleTag);
            allAssembles.add(assemblePo);
        }
        String version = null;
        String versionRegex = String.format("^%s\\:[^\\:]+$", appDefine.getName().toLowerCase());
        for (V1Container container : pod.getSpec().getInitContainers()) {
            String[] arr = container.getImage().split("/");
            String imageTag = arr[arr.length - 1];
            if (imageTag.matches(versionRegex)) {
                version = imageTag.replaceAll("^[^\\:]+\\:", "").replaceAll("\\-", ":");
                break;
            }
        }
        if (StringUtils.isBlank(version)) {
            for (V1Container container : pod.getSpec().getContainers()) {
                if (container.getImage().matches(versionRegex)) {
                    version = container.getImage().replace("^%s\\:", "").replaceAll("\\-", ":");
                    break;
                }
            }
        }
        if (StringUtils.isBlank(version)) {
            logger.error(String.format("can not get version of service %s with podName=%s", service.getMetadata().getName(), pod.getMetadata().getName()));
            throw new ParamException(String.format("can not get version of service %s with podName=%s", service.getMetadata().getName(), pod.getMetadata().getName()));
        }
        Map<String, List<AppModuleVo>> appMap = registerAppList.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        if (!appMap.containsKey(appDefine.getName()) || !appMap.get(appDefine.getName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version)) {
            logger.error(String.format("%s[%s] not register", appDefine.getName(), version));
            throw new ParamException(String.format("%s[%s] not register", appDefine.getName(), version));
        }
        String port = getPortFromK8sService(Arrays.asList(service));
        AppModuleVo moduleVo = appMap.get(appDefine.getName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version);
        PlatformAppPo po = new PlatformAppPo();
        po.setAppAlias(alias);
        po.setHostIp(pod.getStatus().getHostIP());
        po.setPort(port);
        po.setAppRunner(alias);
        po.setBasePath("/");
        po.setDeployTime(now);
        po.setDomainId(domainId);
        po.setOriginalAlias(alias);
        po.setPlatformId(namespace);
        po.setAppId(moduleVo.getAppId());
        List<NexusAssetInfo> cfgs = new ArrayList<>();
        for (AppCfgFilePo cfg : moduleVo.getCfgs())
            cfgs.add(cfg.getNexusAsset(this.nexusHostUrl));
        PlatformAppModuleParam param = new PlatformAppModuleParam(moduleVo, alias, namespace, domainPo, assemblePo, po, cfgs);
        return param;
    }

    /**
     * 获取服务选择的pod
     *
     * @param service k8s服务
     * @param podList 服务所属命名空间下的所有pod
     * @return k8s服务选择的pod
     */
    private List<V1Pod> getServicePod(V1Service service, List<V1Pod> podList) {
        logger.debug(String.format("select pods for service %s", service.getMetadata().getName()));
        List<V1Pod> list = new ArrayList<>();
        if (service.getSpec().getSelector() == null || service.getSpec().getSelector().size() == 0) {
            logger.debug(String.format("service %s has not selector, so pod is 0", service.getMetadata().getName()));
            return list;
        }
        for (V1Pod pod : podList) {
            boolean isSelected = isSelected(pod.getMetadata().getLabels(), service.getSpec().getSelector());
            if (isSelected) {
                list.add(pod);
            }
        }
        logger.debug(String.format("service %s select %d pod", service.getMetadata().getName(), list.size()));
        return list;
    }

    /**
     * 通过服务名判断k8s的服务的类型
     *
     * @param serviceName 服务名
     * @return 指定服务的服务类型
     * @throws NotSupportAppException 指定的服务对应的应用不被支持
     * @throws ParamException         解析服务名失败
     */
    private K8sServiceType getServiceType(String serviceName) throws NotSupportAppException, ParamException {
        for (String name : ccodBiz.getThreePartApps()) {
            String regex = String.format("^%s\\d*$", name.toLowerCase());
            if (serviceName.matches(regex))
                return K8sServiceType.THREE_PART_APP;
        }
        for (String name : ccodBiz.getThreePartServices()) {
            String regex = String.format("^%s\\d*$", name.toLowerCase());
            if (serviceName.matches(regex))
                return K8sServiceType.THREE_PART_SERVICE;
        }
        String[] arr = serviceName.split("\\-");
        if (arr.length < 2)
            throw new ParamException(String.format("%s is illegal domain service name", serviceName));
        String alias = arr[0];
        String domainId = arr[1];
        for (BizSetDefine setDefine : this.ccodBiz.getSet()) {
            String domainRegex = String.format("^%s(0[1-9]|[1-9]\\d+)", setDefine.getFixedDomainId());
            if (domainId.matches(domainRegex)) {
                for (AppDefine appDefine : setDefine.getApps()) {
                    String aliasRegex = String.format("^%s\\d*$", appDefine.getAlias());
                    if (alias.matches(aliasRegex))
                        if (arr.length == 2)
                            return K8sServiceType.DOMAIN_SERVICE;
                        else
                            return K8sServiceType.DOMAIN_OUT_SERVICE;
                }
                throw new ParamException(String.format("%s is not support by set %s", alias, setDefine.getName()));
            }
        }
        throw new ParamException(String.format("%s is illegal domain id for ccod set", domainId));
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
        for (PlatformAppPo deployApp : deployApps) {
            platformAppCfgFileMapper.delete(null, deployApp.getPlatformAppId());
        }
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
    public void updatePlatformUpdateSchema(PlatformUpdateSchemaInfo updateSchema) throws NotSupportSetException, NotSupportAppException, ParamException, InterfaceCallException, LJPaasException, NexusException, IOException, ApiException, K8sDataException, ClassNotFoundException, SQLException {
        logger.debug(String.format("begin to update platform update schema : %s", gson.toJson(updateSchema)));
        if (StringUtils.isBlank(updateSchema.getPlatformId())) {
            logger.error("platformId of schema is blank");
            throw new ParamException("platformId of schema is blank");
        }
        if (StringUtils.isBlank(updateSchema.getPlatformName())) {
            logger.error("platformName of schema is blank");
            throw new ParamException("platformName of schema is blank");
        }
        LJBizInfo bkBiz = paasService.queryBizInfoById(updateSchema.getBkBizId());
        if (bkBiz == null) {
            logger.error(String.format("bkBizId=%d biz not exist", updateSchema.getBkBizId()));
            throw new ParamException(String.format("bkBizId=%d biz not exist", updateSchema.getBkBizId()));
        }
        if (!updateSchema.getPlatformName().equals(bkBiz.getBkBizName())) {
            logger.error(String.format("bkBizName of bizBkId is %s, not %s", updateSchema.getBkBizId(), bkBiz.getBkBizName(), updateSchema.getPlatformName()));
            throw new ParamException(String.format("bkBizName of bizBkId is %s, not %s", updateSchema.getBkBizId(), bkBiz.getBkBizName(), updateSchema.getPlatformName()));
        }
//        updateRegisterAppCfgs(updateSchema.getDomainUpdatePlanList().stream().flatMap(plan -> plan.getAppUpdateOperationList().stream()).collect(Collectors.toList()));
//        updateRegisterApp();
        PlatformUpdateRecordPo rcd = this.platformUpdateRecordMapper.selectByJobId(updateSchema.getSchemaId());
        if(rcd != null)
            throw new ParamException(String.format("schema id %s has been used", updateSchema.getSchemaId()));
        PlatformPo platformPo = platformMapper.selectByPrimaryKey(updateSchema.getPlatformId());
        if (platformPo == null) {
            if (!updateSchema.getTaskType().equals(PlatformUpdateTaskType.CREATE)) {
                logger.error(String.format("%s platform %s not exist", updateSchema.getStatus().name, updateSchema.getPlatformId()));
                throw new ParamException(String.format("%s platform %s not exist", updateSchema.getTaskType().name, updateSchema.getPlatformId()));
            } else {
                platformPo = new PlatformPo(updateSchema.getPlatformId(), updateSchema.getPlatformName(),
                        updateSchema.getBkBizId(), updateSchema.getBkCloudId(), CCODPlatformStatus.SCHEMA_CREATE_PLATFORM,
                        updateSchema.getCcodVersion(), "create by platform create schema", updateSchema.getPlatformType(), updateSchema.getPlatformFunc(), updateSchema.getCreateMethod());
                if (PlatformType.K8S_CONTAINER.equals(updateSchema.getPlatformType())) {
                    platformPo.setApiUrl(updateSchema.getK8sApiUrl());
                    platformPo.setAuthToken(updateSchema.getK8sAuthToken());
                }
                if (StringUtils.isNotBlank(updateSchema.getCcodVersion())) {
                    platformPo.setCcodVersion("CCOD4.1");
                }
                this.platformMapper.insert(platformPo);
            }
        }
        if (!platformPo.getPlatformName().equals(updateSchema.getPlatformName())) {
            logger.error(String.format("platformName of %s is %s, not %s",
                    platformPo.getPlatformId(), platformPo.getPlatformName(), updateSchema.getPlatformName()));
            throw new ParamException(String.format("bkBizName of bizBkId is %s, not %s", updateSchema.getBkBizId(), bkBiz.getBkBizName(), updateSchema.getPlatformName()));
        }
        List<DomainPo> domainList = this.domainMapper.select(updateSchema.getPlatformId(), null);
        Boolean hasImage = PlatformType.K8S_CONTAINER.equals(updateSchema.getPlatformType()) ? true : null;
        List<AppModuleVo> registerApps = this.appManagerService.queryAllRegisterAppModule(hasImage);
        boolean clone = PlatformCreateMethod.CLONE.equals(updateSchema.getCreateMethod()) ? true : false;
        List<PlatformAppDeployDetailVo> platformDeployApps = this.platformAppDeployDetailMapper.selectPlatformApps(updateSchema.getPlatformId(), null, null);
        logger.debug("begin check param of schema");
        List<LJHostInfo> bkHostList = this.paasService.queryBKHost(updateSchema.getBkBizId(), null, null, null, null);
        String platformCheckResult = checkPlatformUpdateSchema(updateSchema, domainList, platformDeployApps, bkHostList, registerApps);
        if(StringUtils.isBlank(platformCheckResult))
            logger.debug("schema param check success");
        else
            throw new ParamException(String.format("schema check fail : %s", platformCheckResult));
        Map<String, DomainPo> domainMap = domainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = platformDeployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        Map<String, List<AssemblePo>> domainAssembleMap = this.assembleMapper.select(updateSchema.getPlatformId(), null).stream().collect(Collectors.groupingBy(AssemblePo::getDomainId));
        List<DomainUpdatePlanInfo> planList = new ArrayList<>();
        List<DomainUpdatePlanInfo> successList = new ArrayList<>();
        makeupDomainIdAndAliasForSchema(updateSchema, domainList, platformDeployApps, clone);
        for (DomainUpdatePlanInfo plan : updateSchema.getDomainUpdatePlanList()) {
            if (plan.getStatus().equals(UpdateStatus.SUCCESS)) {
                successList.add(plan);
            } else
                planList.add(plan);
        }
        if (updateSchema.getStatus().equals(UpdateStatus.SUCCESS) && planList.size() > 0) {
            logger.error(String.format("status of %s(%s) update schema is SUCCESS, but there are %d domain update plan not execute",
                    updateSchema.getPlatformName(), updateSchema.getPlatformId(), planList.size()));
            throw new ParamException(String.format("status of %s(%s) update schema is SUCCESS, but there are %d domain update plan not execute",
                    updateSchema.getPlatformName(), updateSchema.getPlatformId(), planList.size()));
        }
        Map<UpdateStatus, List<DomainUpdatePlanInfo>> statusPlanMap = updateSchema.getDomainUpdatePlanList().stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getStatus));
        List<DomainUpdatePlanInfo> preparePlans = statusPlanMap.containsKey(UpdateStatus.WAIT_EXEC) ? statusPlanMap.get(UpdateStatus.WAIT_EXEC) : new ArrayList<>();
        if(preparePlans.size() > 0)
        {
            List<K8sOperationInfo> tryOptList = generateSchemaK8sExecStep(updateSchema, UpdateStatus.WAIT_EXEC, registerApps);
            logger.debug(String.format("schema need exec steps : %s", gson.toJson(tryOptList)));
            updateSchema.setExecSteps(tryOptList);
//            execPlatformUpdateSteps(tryOptList, updateSchema);
        }
        List<DomainUpdatePlanInfo> execPlans = statusPlanMap.containsKey(UpdateStatus.EXEC) ? statusPlanMap.get(UpdateStatus.EXEC) : new ArrayList<>();
        if(execPlans.size() > 0)
        {
            List<K8sOperationInfo> execSteps = generateSchemaK8sExecStep(updateSchema, UpdateStatus.EXEC, registerApps);
            logger.debug(String.format("exec step is %s", gson.toJson(execSteps)));
            PlatformSchemaExecResultVo execResultVo = execPlatformUpdateSteps(execSteps, updateSchema, platformDeployApps);
            logger.info(String.format("platform schema execute result : %s", gson.toJson(execResultVo)));
            if(!execResultVo.isSuccess())
                throw new ParamException(String.format("schema execute fail : %s", execResultVo.getErrorMsg()));
        }
        Map<String, Map<String, List<NexusAssetInfo>>> domainCfgMap = new HashMap<>();
        for (DomainUpdatePlanInfo plan : execPlans) {
            String domainId = plan.getDomainId();
            boolean isCreate = domainMap.containsKey(plan.getDomainId()) ? false : true;
            DomainPo domainPo = StringUtils.isNotBlank(plan.getDomainId()) && domainMap.containsKey(plan.getDomainId()) ? domainMap.get(domainId) : plan.getDomain(updateSchema.getPlatformId());
            List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
            logger.debug(String.format("preprocess %s %d apps with isCreate=%b and %d deployed apps",
                    JSONObject.toJSONString(domainPo), plan.getAppUpdateOperationList().size(), isCreate, domainAppList.size()));
            Map<String, List<NexusAssetInfo>> cfgMap = preprocessDomainApps(updateSchema.getPlatformId(), plan.getAppUpdateOperationList(), domainAppList, domainPo, registerApps, clone);
            domainCfgMap.put(domainId, cfgMap);
        }
        for (DomainUpdatePlanInfo plan : execPlans) {
            String domainId = plan.getDomainId();
            boolean isCreate = domainMap.containsKey(plan.getDomainId()) ? false : true;
            DomainPo domainPo = isCreate ? plan.getDomain(updateSchema.getPlatformId()) : domainMap.get(domainId);
            if (isCreate)
                this.domainMapper.insert(domainPo);
            List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.containsKey(domainId) ? domainAppMap.get(domainId) : new ArrayList<>();
            List<AssemblePo> assembleList = domainAssembleMap.containsKey(domainId) ? domainAssembleMap.get(domainId) : new ArrayList<>();
            logger.debug(String.format("handle %s %d apps with isCreate=%b and %d deployed apps",
                    JSONObject.toJSONString(domainPo), plan.getAppUpdateOperationList().size(), isCreate, domainAppList.size()));
            handleDomainApps(updateSchema.getPlatformId(), domainPo, plan.getAppUpdateOperationList(), assembleList, domainAppList, registerApps, domainCfgMap.get(domainId));
        }
        if(statusPlanMap.containsKey(UpdateStatus.EXEC))
            statusPlanMap.remove(UpdateStatus.EXEC);
        updateSchema.setDomainUpdatePlanList(statusPlanMap.values().stream().flatMap(plans -> plans.stream()).collect(Collectors.toList()));
        if (updateSchema.getDomainUpdatePlanList().size() == 0) {
            logger.debug(String.format("%s(%s) platform complete deployment, so remove schema and set status to RUNNING", updateSchema.getPlatformName(), updateSchema.getPlatformId()));
            if (this.platformUpdateSchemaMap.containsKey(updateSchema.getPlatformId()))
                this.platformUpdateSchemaMap.remove(updateSchema.getPlatformId());
            this.platformUpdateSchemaMapper.delete(updateSchema.getPlatformId());
            platformPo.setStatus(CCODPlatformStatus.RUNNING.id);
            this.platformMapper.update(platformPo);
        } else {
            logger.debug(String.format("%s(%s) platform not complete deployment, so update schema and set status to PLANING", updateSchema.getPlatformName(), updateSchema.getPlatformId()));
            updateSchema.setDomainUpdatePlanList(planList);
            PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
            schemaPo.setContext(gson.toJson(updateSchema).getBytes());
            schemaPo.setPlatformId(updateSchema.getPlatformId());
            this.platformUpdateSchemaMapper.delete(updateSchema.getPlatformId());
            this.platformUpdateSchemaMapper.insert(schemaPo);
            this.platformUpdateSchemaMap.put(updateSchema.getPlatformId(), updateSchema);
            platformPo.setStatus(CCODPlatformStatus.SCHEMA_CREATE_PLATFORM.id);
            this.platformMapper.update(platformPo);
        }
        if (execPlans.size() > 0) {
            logger.debug(String.format("%d domain complete deployment, so sync new platform topo to lj paas", successList.size()));
            this.paasService.syncClientCollectResultToPaas(platformPo.getBkBizId(), platformPo.getPlatformId(), platformPo.getBkCloudId());
        }
    }

    private List<K8sOperationInfo> generateSchemaK8sExecStep(PlatformUpdateSchemaInfo schema, UpdateStatus planStatus, List<AppModuleVo> registerApps) throws K8sDataException, ParamException, ApiException, NotSupportAppException, IOException, InterfaceCallException
    {
        Map<UpdateStatus, List<DomainUpdatePlanInfo>> typePlanMap = schema.getDomainUpdatePlanList().stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getStatus));
        List<DomainUpdatePlanInfo> plans = typePlanMap.containsKey(planStatus) ? typePlanMap.get(planStatus) : new ArrayList<>();
        if(plans.size() == 0)
            return new ArrayList<>();
        List<K8sOperationInfo> execSteps = new ArrayList<>();
        String apiUrl = schema.getK8sApiUrl();
        String authToken = schema.getK8sAuthToken();
        String platformId = schema.getPlatformId();
        String jobId = schema.getSchemaId();
        if(schema.getTaskType().equals(PlatformUpdateTaskType.CREATE))
        {
            if(schema.getNamespace() == null)
                throw new ParamException("namespace of new CREATE platform can not be null");
            else if(!schema.getNamespace().getMetadata().getName().equals(platformId))
                throw new ParamException(String.format("namespace name %s is not equal platformId %s", schema.getNamespace().getMetadata().getName(), platformId));
            else if(this.k8sApiService.isNamespaceExist(platformId, apiUrl, authToken))
                throw new ParamException(String.format("namespace %s exist at %s", platformId, apiUrl));
            if(schema.getK8sJob() == null)
                throw new ParamException(String.format("job of new create platform is blank"));
            if(schema.getK8sSecrets() == null || schema.getK8sSecrets().size() == 0)
                throw new ParamException(String.format("secret of new create platform can not be empty"));
            Map<String, List<V1ObjectMeta>> metaMap = schema.getK8sSecrets().stream().map(svc -> svc.getMetadata())
                    .collect(Collectors.toList()).stream().collect(Collectors.groupingBy(V1ObjectMeta::getName));
            if(!metaMap.containsKey("ssl"))
                throw new ParamException("new create platform must has ssl secret");
            for(String name : metaMap.keySet())
            {
                if(metaMap.get(name).size() > 1)
                    throw new ParamException(String.format("secret %s multi define", name));
                if(!metaMap.get(name).get(0).getNamespace().equals(platformId))
                    throw new ParamException(String.format("namespace of secret %s is %s not %s", name, metaMap.get(name).get(0).getNamespace(), platformId));
            }
            if(schema.getK8sPVList() == null || schema.getK8sPVList().size() == 0)
                throw new ParamException("k8sPVList of new create platform can not be empty");
            metaMap = schema.getK8sPVList().stream().map(pv -> pv.getMetadata()).collect(Collectors.toList()).stream()
                    .collect(Collectors.groupingBy(V1ObjectMeta::getName));
            for(String name : metaMap.keySet())
            {
                if(metaMap.get(name).size() > 1)
                    throw new ParamException(String.format("pv %s multi define", name));
            }
            if(schema.getK8sPVCList() == null || schema.getK8sPVCList().size() == 0)
                throw new ParamException("k8sPVCList of new create platform can not be empty");
            metaMap = schema.getK8sPVCList().stream().map(pvc -> pvc.getMetadata()).collect(Collectors.toList())
                    .stream().collect(Collectors.groupingBy(V1ObjectMeta::getName));
            for(String name : metaMap.keySet())
            {
                if(metaMap.get(name).size() > 1)
                    throw new ParamException(String.format("pvc %s multi define", name));
                if(!metaMap.get(name).get(0).getNamespace().equals(platformId))
                    throw new ParamException(String.format("namespace of pvc %s is %s not %s", name, metaMap.get(name).get(0).getNamespace(), platformId));
            }
            if(schema.getThreePartApps() == null || schema.getThreePartApps().size() == 0)
                throw new ParamException("three part apps of new create platform can not be empty");
            metaMap = schema.getThreePartApps().stream().map(app -> app.getDeployment()).collect(Collectors.toList())
                    .stream().map(dep -> dep.getMetadata()).collect(Collectors.toList())
                    .stream().collect(Collectors.groupingBy(V1ObjectMeta::getName));
            for(String name : metaMap.keySet())
            {
                if(metaMap.get(name).size() > 1)
                    throw new ParamException(String.format("deployment %s of three app multi define", name));
                if(!metaMap.get(name).get(0).getNamespace().equals(platformId))
                    throw new ParamException(String.format("namespace of deployment %s of three app is %s not %s", name, metaMap.get(name).get(0).getNamespace(), platformId));
            }
            metaMap = schema.getThreePartApps().stream().flatMap(app -> app.getServices().stream()).collect(Collectors.toList())
                    .stream().map(svc -> svc.getMetadata()).collect(Collectors.toList())
                    .stream().collect(Collectors.groupingBy(V1ObjectMeta::getName));
            for(String name : metaMap.keySet())
            {
                if(metaMap.get(name).size() > 1)
                    throw new ParamException(String.format("service %s of three app multi define", name));
                if(!metaMap.get(name).get(0).getNamespace().equals(platformId))
                    throw new ParamException(String.format("namespace of service %s of three app is %s not %s", name, metaMap.get(name).get(0).getNamespace(), platformId));
            }
            if(schema.getPublicConfig() == null || schema.getPublicConfig().size() == 0)
                throw new ParamException("public config of new create platform is empty");
            K8sOperationInfo optInfo = new K8sOperationInfo(jobId, platformId, null, K8sKind.NAMESPACE, schema.getNamespace().getMetadata().getName(), K8sOperation.CREATE, schema.getNamespace());
            execSteps.add(optInfo);
            optInfo = new K8sOperationInfo(jobId, platformId, null, K8sKind.JOB, schema.getK8sJob().getMetadata().getName(), K8sOperation.CREATE, schema.getK8sJob());
            execSteps.add(optInfo);
            optInfo = new K8sOperationInfo(jobId, platformId, null, K8sKind.SECRET, schema.getK8sSecrets().get(0).getMetadata().getName(), K8sOperation.CREATE, schema.getK8sSecrets().get(0));
            execSteps.add(optInfo);
            for(V1PersistentVolume pv : schema.getK8sPVList())
            {
                optInfo = new K8sOperationInfo(jobId, platformId, null, K8sKind.PV, pv.getMetadata().getName(), K8sOperation.CREATE, pv);
                execSteps.add(optInfo);
            }
            for(V1PersistentVolumeClaim pvc : schema.getK8sPVCList())
            {
                optInfo = new K8sOperationInfo(jobId, platformId, null, K8sKind.PVC, pvc.getMetadata().getName(), K8sOperation.CREATE, pvc);
                execSteps.add(optInfo);
            }
            List<NexusAssetInfo> cfgs = new ArrayList<>();
            for(AppFileNexusInfo cfgFile : schema.getPublicConfig())
            {
                NexusAssetInfo cfg = cfgFile.getNexusAssetInfo(this.nexusHostUrl);
                cfgs.add(cfg);
            }
            V1ConfigMap configMap = this.k8sApiService.getConfigMapFromNexus(platformId, platformId,
                    cfgs, this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
            K8sOperationInfo k8sOpt = new K8sOperationInfo(jobId, platformId, null, K8sKind.CONFIGMAP,
                    platformId, K8sOperation.CREATE, configMap);
            execSteps.add(k8sOpt);
            for(K8sCollection threeApp : schema.getThreePartApps())
            {
                if(threeApp.getDeployment().getMetadata().getLabels() == null)
                    threeApp.getDeployment().getMetadata().setLabels(new HashMap<>());
                threeApp.getDeployment().getMetadata().getLabels().put(this.deploymentTypeLabel, K8sDeploymentType.THREE_PART_APP.name);
                threeApp.getDeployment().getMetadata().getLabels().put(this.jobIdLabel, jobId);
                threeApp.getDeployment().getMetadata().getLabels().put(this.appNameLabel, threeApp.getAppName());
                threeApp.getDeployment().getMetadata().getLabels().put(this.appAliasLabel, threeApp.getAlias());
                threeApp.getDeployment().getMetadata().getLabels().put(this.appVersionLabel, threeApp.getVersion().replaceAll("\\:", "-"));
                optInfo = new K8sOperationInfo(jobId, platformId, null, K8sKind.DEPLOYMENT, threeApp.getDeployment().getMetadata().getName(), K8sOperation.CREATE, threeApp.getDeployment());;
                execSteps.add(optInfo);
                if(threeApp.getServices() == null || threeApp.getServices().size() == 0)
                    throw new ParamException(String.format("service of new create three part app %s can not be null", threeApp.getAppName()));
                for(V1Service svc : threeApp.getServices()) {
                    if(svc.getMetadata().getLabels() == null)
                        svc.getMetadata().setLabels(new HashMap<>());
                    svc.getMetadata().getLabels().put(serviceTypeLabel, K8sServiceType.THREE_PART_APP.name);
                    svc.getMetadata().getLabels().put(this.jobIdLabel, jobId);
                    svc.getMetadata().getLabels().put(this.appNameLabel, threeApp.getAppName());
                    svc.getMetadata().getLabels().put(this.appAliasLabel, threeApp.getAlias());
                    svc.getMetadata().getLabels().put(this.appVersionLabel, threeApp.getVersion().replaceAll("\\:", "-"));
                    svc.getMetadata().setNamespace(platformId);
                    optInfo = new K8sOperationInfo(jobId, platformId, null, K8sKind.SERVICE, svc.getMetadata().getName(), K8sOperation.CREATE, svc);
                    execSteps.add(optInfo);
                }
                for(ExtensionsV1beta1Ingress ingress : threeApp.getIngresses())
                {
                    if(ingress.getMetadata().getLabels() == null)
                        ingress.getMetadata().setLabels(new HashMap<>());
                    ingress.getMetadata().getLabels().put(serviceTypeLabel, K8sServiceType.THREE_PART_APP.name);
                    ingress.getMetadata().getLabels().put(this.jobIdLabel, jobId);
                    ingress.getMetadata().getLabels().put(this.appNameLabel, threeApp.getAppName());
                    ingress.getMetadata().getLabels().put(this.appAliasLabel, threeApp.getAlias());
                    ingress.getMetadata().getLabels().put(this.appVersionLabel, threeApp.getVersion());
                    ingress.getMetadata().setNamespace(platformId);
                    optInfo = new K8sOperationInfo(jobId, platformId, null, K8sKind.INGRESS, ingress.getMetadata().getName(), K8sOperation.CREATE, ingress);
                    execSteps.add(optInfo);
                }
            }
        }
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
        Collections.sort(plans, sort);
        List<V1Deployment> allDeployments = new ArrayList<>();
        List<V1Service> allServices = new ArrayList<>();
        List<ExtensionsV1beta1Ingress> allIngresses = new ArrayList<>();
        List<V1ConfigMap> allConfigs = new ArrayList<>();
        if(!schema.getTaskType().equals(PlatformUpdateTaskType.CREATE))
        {
            allDeployments = this.k8sApiService.listNamespacedDeployment(platformId, apiUrl, authToken);
            allServices = this.k8sApiService.listNamespacedService(platformId, apiUrl, authToken);
            allIngresses = this.k8sApiService.listNamespacedIngress(platformId, apiUrl, authToken);
            allConfigs = this.k8sApiService.listNamespacedConfigMap(platformId, apiUrl, authToken);
        }
        for(DomainUpdatePlanInfo plan : plans)
        {
            String domainId = plan.getDomainId();
            List<V1Deployment> domainDeploys = getDomainServiceDeploy(domainId, allDeployments);
            List<V1Service> domainServices = getDomainServiceService(domainId, allServices);
            List<ExtensionsV1beta1Ingress> domainIngresses = getDomainServiceIngress(domainId, allIngresses);
            List<V1ConfigMap> domainConfigs = getDomainServiceConfigMap(domainId, allConfigs);
            BizSetDefine setDefine = getBizSetForDomainId(domainId);
            List<K8sCollection> existDomainCollects = parseAppK8sCollection(domainId, domainDeploys, domainServices, domainIngresses, registerApps);
            List<K8sOperationInfo> optList = generateDomainK8sOperation(jobId, platformId, domainId, plan, existDomainCollects, domainConfigs, schema.getPublicConfig(), plan.getPublicConfig(), setDefine, registerApps);
            execSteps.addAll(optList);
        }
        Map<K8sKind, List<K8sOperationInfo>> operationMap = execSteps.stream().collect(Collectors.groupingBy(K8sOperationInfo::getKind));
        List<K8sOperationInfo> serviceList = operationMap.containsKey(K8sKind.SERVICE) ? operationMap.get(K8sKind.SERVICE) : new ArrayList<>();
        reuseServiceNodePort(serviceList, allServices);
        return execSteps;
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
        Map<DomainUpdateType, List<DomainUpdatePlanInfo>> planTypeMap = schemaInfo.getDomainUpdatePlanList().stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getUpdateType));
        List<DomainUpdatePlanInfo> addDomainList = planTypeMap.containsKey(DomainUpdateType.ADD) ? planTypeMap.get(DomainUpdateType.ADD) : new ArrayList<>();
        generateId4AddDomain(schemaInfo.getPlatformId(), addDomainList, existDomainList, clone);
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        Map<String, DomainPo> domainMap = existDomainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        for (DomainUpdatePlanInfo planInfo : schemaInfo.getDomainUpdatePlanList()) {
            Map<AppUpdateOperation, List<AppUpdateOperationInfo>> optTypeMap = planInfo.getAppUpdateOperationList().stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getOperation));
            List<AppUpdateOperationInfo> addOptList = optTypeMap.containsKey(AppUpdateOperation.ADD) ? optTypeMap.get(AppUpdateOperation.ADD) : new ArrayList<>();
            if (addOptList.size() == 0) {
                logger.debug(String.format("%s has not new add module", planInfo.getDomainName()));
                continue;
            }
            List<PlatformAppDeployDetailVo> domainAppList = domainAppMap.containsKey(planInfo.getDomainId()) ? domainAppMap.get(planInfo.getDomainId()) : new ArrayList<>();
            DomainPo domainPo = domainMap.containsKey(planInfo.getDomainId()) ? domainMap.get(planInfo.getDomainId()) : planInfo.getDomain(schemaInfo.getPlatformId());
            generateAlias4DomainApps(domainPo, addOptList, deployAppList, clone);
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

    private void generatePlatformDeployParamAndScript(PlatformUpdateSchemaInfo schemaInfo) throws IOException, InterfaceCallException, NexusException {
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
        if (schemaInfo.getGlsDBType().equals(DatabaseType.ORACLE)) {
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
        if (!saveDir.exists()) {
            saveDir.mkdirs();
        }
        String savePath = String.format("%s/%s", tmpSaveDir, platformDeployScriptFileName);
        savePath = savePath.replaceAll("\\\\", "/");
        File scriptFile = new File(savePath);
        scriptFile.createNewFile();
        BufferedWriter out = new BufferedWriter(new FileWriter(scriptFile));
        String lineTxt = null;
        while ((lineTxt = br.readLine()) != null) {
            if ("platform_deploy_params = \"\"\"\"\"\"".equals(lineTxt)) {
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

    /**
     * 检查平台升级相关参数
     * @param updateSchema    平台升级计划
     * @param existDomainList 该平台已经有的域
     * @throws ParamException 平台升级计划参数异常
     */
    private String checkPlatformUpdateSchema(PlatformUpdateSchemaInfo updateSchema, List<DomainPo> existDomainList, List<PlatformAppDeployDetailVo> deployApps, List<LJHostInfo> hostList, List<AppModuleVo> registerApps) {
        StringBuffer sb = new StringBuffer();
        PlatformUpdateTaskType taskType = updateSchema.getTaskType();
        if (!updateSchema.getGlsDBType().equals(DatabaseType.ORACLE))
            sb.append(String.format("this version cmdb not support glsserver with database %s;", updateSchema.getGlsDBType().name));
        if (StringUtils.isBlank(updateSchema.getPlatformId()))
            sb.append("platformId of update schema is blank;");
        if(PlatformUpdateTaskType.CREATE.equals(taskType))
        {
            if (StringUtils.isBlank(updateSchema.getPlatformName()))
                sb.append("platformName of update schema is blank;");
            if (updateSchema.getBkBizId() <= 0)
                sb.append("bkBizId of update schema not define;");
        }
        if(PlatformType.K8S_CONTAINER.equals(updateSchema.getPlatformType()))
        {
            if(StringUtils.isBlank(updateSchema.getK8sHostIp()))
                sb.append(String.format("k8sHostIp is blank;"));
        }
        if (StringUtils.isNotBlank(sb.toString()))
            return sb.toString();
        Map<String, List<LJHostInfo>> hostMap = hostList.stream().collect(Collectors.groupingBy(LJHostInfo::getHostInnerIp));
        for(String hostIp : hostMap.keySet())
        {
            if(hostMap.get(hostIp).size() > 1)
                sb.append(String.format("ip %s is indistinct;", hostIp));
        }
        if(StringUtils.isNotBlank(sb.toString()))
            return sb.toString();
        Map<String, DomainPo> domainIdMap = existDomainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        Map<String, DomainPo> domainNameMap = existDomainList.stream().collect(Collectors.toMap(DomainPo::getDomainName, Function.identity()));
        Map<DomainUpdateType, List<DomainUpdatePlanInfo>> typePlanMap = updateSchema.getDomainUpdatePlanList().stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getUpdateType));
        List<DomainUpdatePlanInfo> deleteList = typePlanMap.containsKey(DomainUpdateType.DELETE) ? typePlanMap.get(DomainUpdateType.DELETE) : new ArrayList<>();
        for(DomainUpdatePlanInfo planInfo : deleteList)
        {
            String domainId = planInfo.getDomainId();
            if(StringUtils.isBlank(domainId))
                sb.append("domainId of DELETE domain is blank;");
            else if(!domainIdMap.containsKey(domainId))
                sb.append(String.format("DELETE domain %s not exist;", domainId));
        }
        List<DomainUpdatePlanInfo> addList = typePlanMap.containsKey(DomainUpdateType.ADD) ? typePlanMap.get(DomainUpdateType.ADD) : new ArrayList<>();
        for(DomainUpdatePlanInfo planInfo : addList)
        {
            String domainName = planInfo.getDomainName();
            if(StringUtils.isBlank(domainName))
                sb.append("domainName of ADD domain is blank;");
            else if(domainNameMap.containsKey(domainName))
                sb.append(String.format("ADD domain %s has exist;", domainName));
            else if(StringUtils.isBlank(planInfo.getBkSetName()))
                sb.append(String.format("bkSetName of ADD domain %s is blank", domainName));
        }
        List<DomainUpdatePlanInfo> updateList = typePlanMap.containsKey(DomainUpdateType.UPDATE) ? typePlanMap.get(DomainUpdateType.UPDATE) : new ArrayList<>();
        for(DomainUpdatePlanInfo planInfo : updateList)
        {
            String domainId = planInfo.getDomainId();
            if(StringUtils.isBlank(domainId))
                sb.append("domainId of UPDATE domain is blank;");
            else if(!domainIdMap.containsKey(domainId))
                sb.append(String.format("UPDATE domain %s not exist;", domainId));
        }
        if(StringUtils.isNotBlank(sb.toString()))
            return sb.toString();
        Map<String, List<DomainUpdatePlanInfo>> deleteMap = deleteList.stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getDomainId));
        for(String domainId : deleteMap.keySet())
        {
            if(deleteMap.get(domainId).size() > 1)
                sb.append(String.format("DELETE Domain %S duplicate;", domainId));
        }
        Map<String, List<DomainUpdatePlanInfo>> updateMap = updateList.stream().collect(Collectors.groupingBy(DomainUpdatePlanInfo::getDomainId));
        for(String domainId : updateMap.keySet())
        {
            if(updateMap.get(domainId).size() > 1)
                sb.append(String.format("UPDATE Domain %S duplicate;", domainId));
        }
        for(String domainId : deleteMap.keySet())
        {
            if(deleteMap.containsKey(domainId))
                sb.append(String.format(" domain %s has been DELETED and UPDATE at the same time;", domainId));
        }
        if(StringUtils.isNotBlank(sb.toString()))
            return sb.toString();
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = deployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        for(DomainUpdatePlanInfo planInfo : deleteList)
        {
            List<PlatformAppDeployDetailVo> domainApps = domainAppMap.containsKey(planInfo.getDomainId()) ? domainAppMap.get(planInfo.getDomainId()) : new ArrayList<>();
            String planCheckResult = checkDomainPlan(planInfo, domainApps, hostList, registerApps);
            sb.append(planCheckResult);
        }
        for(DomainUpdatePlanInfo planInfo : addList)
        {
            String planCheckResult = checkDomainPlan(planInfo, new ArrayList<>(), hostList, registerApps);
            sb.append(planCheckResult);
        }
        for(DomainUpdatePlanInfo planInfo : updateList)
        {
            List<PlatformAppDeployDetailVo> domainApps = domainAppMap.containsKey(planInfo.getDomainId()) ? domainAppMap.get(planInfo.getDomainId()) : new ArrayList<>();
            String planCheckResult = checkDomainPlan(planInfo, domainApps, hostList, registerApps);
            sb.append(planCheckResult);
        }
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
        Map<String, PlatformAppDeployDetailVo> deployAppMap = domainApps.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAppAlias, Function.identity()));
        List<AppUpdateOperationInfo> deleteList = optMap.containsKey(AppUpdateOperation.DELETE) ? optMap.get(AppUpdateOperation.DELETE) : new ArrayList<>();
        Map<String, LJHostInfo> hostMap = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity()));
        for(AppUpdateOperationInfo optInfo : deleteList)
        {
            if(planType.equals(DomainUpdateType.ADD))
                sb.append(String.format("%s domain can not has %s operation;", planType.name, optInfo.getOperation().name));
            else if(StringUtils.isBlank(optInfo.getAppName()))
                sb.append(String.format("DELETE app name at %s is blank;", domainId));
            else if(StringUtils.isBlank(optInfo.getAppAlias()))
                sb.append("DELETE app alias is blank;");
            else if(!deployAppMap.containsKey(optInfo.getAppAlias()))
                sb.append(String.format("DELETE app %s not exist at %s;", optInfo.getAppAlias(), planInfo.getDomainId()));
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
                if(StringUtils.isBlank(optInfo.getTargetVersion()))
                    sb.append(String.format("version of %s is blank;", optTag));
                else if (!registerAppMap.containsKey(appName)
                        || !registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(optInfo.getTargetVersion()))
                    sb.append(String.format("version %s of %s not register;", optInfo.getTargetVersion(), optTag));
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
            String alias = optInfo.getAppAlias();
            String appName = optInfo.getAppName();
            if(StringUtils.isBlank(appName))
                sb.append(String.format("UPDATE app name at %s is blank;", domainId));
            else if(StringUtils.isBlank(optInfo.getAppAlias()))
                sb.append(String.format("UPDATE alias name of %s at %s is blank;", appName, domainId));
            else if(!deployAppMap.containsKey(alias))
                sb.append(String.format("UPDATE %s at %s not exist;", alias, domainId));
            else if(!deployAppMap.get(alias).getAppName().equals(appName))
                sb.append(String.format("appName of %s at %s is %s not %s;", alias, domainId, deployAppMap.get(alias).getAppName(), appName));
            else
            {
                String optTag = String.format("UPDATE %s(%s) at %s", alias, appName, domainId);
                if(StringUtils.isBlank(optInfo.getTargetVersion()))
                    sb.append(String.format("version of %s is blank;", optTag));
                else if (!registerAppMap.containsKey(appName)
                        || !registerAppMap.get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(optInfo.getTargetVersion()))
                    sb.append(String.format("version %s of %s not register;", optInfo.getTargetVersion(), optTag));
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
        Map<String, List<AppUpdateOperationInfo>> deleteMap = deleteList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppAlias));
        for(String alias : deleteMap.keySet())
        {
            if(deleteMap.get(alias).size() > 1)
                sb.append(String.format("%s at %s is DELETE duplicate;", domainId));
        }
        Map<String, List<AppUpdateOperationInfo>> updateMap = deleteList.stream().collect(Collectors.groupingBy(AppUpdateOperationInfo::getAppAlias));
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
        checkPlatformCreateParam(paramVo);
        logger.debug(String.format("prepare to create new platform : %s", JSONObject.toJSONString(paramVo)));
        List<LJHostInfo> hostList = this.paasService.queryBKHost(paramVo.getBkBizId(), null, null, null, null);
        if (hostList.size() == 0)
            throw new ParamException(String.format("%s has not any host", paramVo.getPlatformName()));
        if(paramVo.getPlatformType().equals(PlatformType.K8S_CONTAINER))
        {
            if(StringUtils.isBlank(paramVo.getK8sApiUrl()))
                throw new ParamException(String.format("k8sApiUrl of new create platform is blank"));
            if(StringUtils.isBlank(paramVo.getK8sAuthToken()))
                throw new ParamException(String.format("k8s auth token of new create platform is blank"));
        }
        PlatformUpdateSchemaInfo schemaInfo;
        String platformId = paramVo.getPlatformId();
        switch (paramVo.getCreateMethod()) {
            case MANUAL:
                schemaInfo = paramVo.getPlatformCreateSchema(new ArrayList<>());
                break;
            case CLONE:
                if (StringUtils.isBlank(paramVo.getParams())) {
                    logger.error(String.format("cloned platform id is blank"));
                    throw new ParamException(String.format("cloned platform id is blank"));
                }
                PlatformPo clonedPlatform = this.platformMapper.selectByPrimaryKey(paramVo.getParams());
                List<DomainPo> clonedDomains = this.domainMapper.select(paramVo.getParams(), null);
                if (clonedPlatform == null)
                    throw new ParamException(String.format("cloned platform %s not exist", paramVo.getParams()));
                List<PlatformAppDeployDetailVo> clonedApps = this.platformAppDeployDetailMapper.selectPlatformApps(paramVo.getParams(), null, null);
                if (hostList.size() < clonedApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getHostIp)).keySet().size())
                    throw new ParamException(String.format("%s has not enough hosts, want %s but has %d",
                            paramVo.getPlatformName(), clonedApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getHostIp)).size(), hostList.size()));
                schemaInfo = cloneExistPlatform(paramVo, clonedPlatform, clonedDomains, clonedApps, hostList);
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
        PlatformPo platform = paramVo.getCreatePlatform();
        platformMapper.insert(platform);
        PlatformUpdateSchemaPo schemaPo = new PlatformUpdateSchemaPo();
        schemaPo.setPlatformId(platformId);
        schemaPo.setContext(JSONObject.toJSONString(schemaInfo).getBytes());
        this.platformUpdateSchemaMapper.delete(platformId);
        this.platformUpdateSchemaMapper.insert(schemaPo);
        this.platformUpdateSchemaMap.put(platformId, schemaInfo);
        return schemaInfo;
    }

    private void checkPlatformCreateParam(PlatformCreateParamVo param) throws ParamException, InterfaceCallException, LJPaasException {
        StringBuffer sb = new StringBuffer();
        if (PlatformType.K8S_CONTAINER.equals(param.getPlatformType())) {
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
        if (!param.getGlsDBType().equals(DatabaseType.ORACLE))
            throw new ParamException(String.format("this version cmdb not support glsserver with database %s", param.getGlsDBType().name));
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
        logger.debug(String.format("begin to create new empty platform : %s", JSONObject.toJSONString(paramVo)));
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
                        for (AppCfgFilePo appCfgFilePo : appModuleVo.getCfgs()) {
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
     * @param clonedPlatform 被克隆的平台
     * @param clonedDomains 需要克隆的域
     * @param clonedApps    需要克隆的应用
     * @param hostList      给新平台分配的服务器列表
     * @return 平台创建计划
     * @throws ParamException
     * @throws NotSupportSetException
     * @throws NotSupportAppException
     * @throws InterfaceCallException
     * @throws LJPaasException
     */
    private PlatformUpdateSchemaInfo cloneExistPlatform(PlatformCreateParamVo paramVo, PlatformPo clonedPlatform, List<DomainPo> clonedDomains, List<PlatformAppDeployDetailVo> clonedApps, List<LJHostInfo> hostList) throws ParamException, NotSupportSetException, NotSupportAppException, InterfaceCallException, LJPaasException {
        logger.debug(String.format("begin to clone platform : %s", JSONObject.toJSONString(paramVo)));
        Map<String, DomainUpdatePlanInfo> planMap = new HashMap<>();
        Map<String, DomainPo> clonedDomainMap = clonedDomains.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity()));
        Map<String, List<PlatformAppDeployDetailVo>> hostAppMap = clonedApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getHostIp));
        int i = 0;
        for (List<PlatformAppDeployDetailVo> hostAppList : hostAppMap.values()) {
            String hostIp = hostList.get(i).getHostInnerIp();
            for (PlatformAppDeployDetailVo deployApp : hostAppList) {
                if (!planMap.containsKey(deployApp.getDomainId())) {
                    DomainUpdatePlanInfo planInfo = generateCloneExistDomain(clonedDomainMap.get(deployApp.getDomainId()));
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
                opt.setAssembleTag(deployApp.getAssembleTag());
                opt.setStartCmd(deployApp.getStartCmd());
                opt.setDeployPath(deployApp.getDeployPath());
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
                planMap.get(deployApp.getDomainId()).getAppUpdateOperationList().add(opt);
            }
            i++;
        }
        PlatformUpdateSchemaInfo schema = paramVo.getPlatformCreateSchema(new ArrayList<>(planMap.values()));
        schema.setPlatformType(clonedPlatform.getType());
        schema.setPlatformFunc(schema.getPlatformFunc());
        schema.setCreateMethod(PlatformCreateMethod.CLONE);
        schema.setPublicConfig(paramVo.getPublicConfig());
//        makeupPlatformUpdateSchema(schema, new ArrayList<>(), new ArrayList<>());
        return schema;
    }

    private DomainUpdatePlanInfo generateCloneExistDomain(DomainPo clonedDomain) {
        DomainUpdatePlanInfo planInfo = new DomainUpdatePlanInfo();
        planInfo.setUpdateType(DomainUpdateType.ADD);
        planInfo.setComment(String.format("clone from %s of %s", clonedDomain.getDomainName(), clonedDomain.getPlatformId()));
        planInfo.setDomainId(clonedDomain.getDomainId());
        planInfo.setDomainName(clonedDomain.getDomainName());
        Date now = new Date();
        planInfo.setBkSetName(clonedDomain.getBizSetName());
        planInfo.setStatus(UpdateStatus.CREATE);
        planInfo.setAppUpdateOperationList(new ArrayList<>());
        planInfo.setMaxOccurs(clonedDomain.getMaxOccurs());
        planInfo.setOccurs(clonedDomain.getOccurs());
        planInfo.setTags(clonedDomain.getTags());
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
                logger.debug(String.format("insert new domain [%s]", JSONObject.toJSONString(po)));
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
                assemblePo.setTag(platformApp.getAppAlias());
                assemblePo.setStatus("Running");
                assemblePo.setPlatformId(platformId);
                assemblePo.setDomainId(moduleVo.getDomainId());
                this.assembleMapper.insert(assemblePo);
                platformApp.setAssembleId(assemblePo.getAssembleId());
                platformApp.setAppId(appMap.get(moduleVo.getModuleName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(moduleVo.getVersion()).getAppId());
                logger.debug(String.format("insert %s into platform_app", JSONObject.toJSONString(platformApp)));
                this.platformAppMapper.insert(platformApp);
                for (DeployFileInfo cfgFilePo : moduleVo.getCfgs()) {
                    PlatformAppCfgFilePo po = new PlatformAppCfgFilePo(platformApp.getPlatformAppId(), cfgFilePo);
                    logger.debug(String.format("insert cfg %s into platform_app_cfg", JSONObject.toJSONString(po)));
                    this.platformAppCfgFileMapper.insert(po);
                }
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
            platformPo.setStatus(CCODPlatformStatus.RUNNING.id);
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

    private void updateAppDefaultCfg(String protoPlatformId) throws Exception {
        List<PlatformAppDeployDetailVo> deployAppList = this.platformAppDeployDetailMapper.selectPlatformApps(protoPlatformId, null, null);
        Map<String, List<AppModuleVo>> registerAppMap = this.appManagerService.queryAllRegisterAppModule(null).stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        for (PlatformAppDeployDetailVo deployApp : deployAppList) {
            logger.info(String.format("begin to update %s[%s]", deployApp.getAppName(), deployApp.getVersion()));
            AppModuleVo moduleVo = registerAppMap.get(deployApp.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(deployApp.getVersion());
            List<AppCfgFilePo> cfgs = new ArrayList<>();
            for (PlatformAppCfgFilePo cfg : deployApp.getCfgs()) {
                AppCfgFilePo cfgFilePo = new AppCfgFilePo();
                cfgFilePo.setNexusAssetId(cfg.getNexusAssetId());
                cfgFilePo.setNexusRepository(cfg.getNexusRepository());
                cfgFilePo.setNexusDirectory(cfg.getNexusDirectory());
                cfgFilePo.setAppId(moduleVo.getAppId());
                cfgFilePo.setCreateTime(new Date());
                cfgFilePo.setDeployPath(cfg.getDeployPath());
                cfgFilePo.setExt(cfg.getExt());
                cfgFilePo.setFileName(cfg.getFileName());
                cfgFilePo.setMd5(cfg.getMd5());
                cfgs.add(cfgFilePo);
            }
            moduleVo.setCfgs(cfgs);
            this.appManagerService.updateAppModule(moduleVo);
            logger.info(String.format("update %s[%s] finish", deployApp.getAppName(), deployApp.getVersion()));
        }
    }

    /**
     * 将已有的平台迁移到带assemble架构的拓扑
     *
     * @param platformId 需要迁移的平台
     */
    private void transferPlatformToAssembleTopology(String platformId) throws Exception {
        logger.debug(String.format("begin to transfer %s to the topology with assemble", platformId));
        PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
        if (platformPo == null) {
            logger.error(String.format("%s not exist", platformId));
            throw new Exception(String.format("%s not exist", platformId));
        }
        Map<String, List<PlatformAppPo>> domainAppMap = this.platformAppMapper.select(platformId, null, null, null, null, null)
                .stream().collect(Collectors.groupingBy(PlatformAppPo::getDomainId));
        for (String domainId : domainAppMap.keySet()) {
            for (PlatformAppPo deployApp : domainAppMap.get(domainId)) {
                AssemblePo assemblePo = new AssemblePo();
                assemblePo.setTag(deployApp.getAppAlias());
                assemblePo.setStatus("Running");
                assemblePo.setPlatformId(platformId);
                assemblePo.setDomainId(domainId);
                this.assembleMapper.insert(assemblePo);
                deployApp.setAssembleId(assemblePo.getAssembleId());
                deployApp.setPort(null);
                this.platformAppMapper.update(deployApp);
            }
        }
    }

    @Override
    public V1Namespace queryPlatformK8sNamespace(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("begin to query k8s namespace of %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        return this.k8sApiService.readNamespace(platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
    }

    @Override
    public List<V1Pod> queryPlatformAllK8sPods(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("begin to query k8s pods of %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        return this.k8sApiService.listNamespacedPod(platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
    }

    @Override
    public V1Pod queryPlatformK8sPodByName(String platformId, String podName) throws ParamException, ApiException {
        logger.debug(String.format("begin to query k8s pod %s of %s", podName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        return this.k8sApiService.readNamespacedPod(podName, platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
    }

    @Override
    public List<V1Service> queryPlatformAllK8sServices(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("begin to query k8s services of %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        return this.k8sApiService.listNamespacedService(platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
    }

    @Override
    public V1Service queryPlatformK8sServiceByName(String platformId, String serviceName) throws ParamException, ApiException {
        logger.debug(String.format("begin to query k8s service %s of %s", serviceName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        return this.k8sApiService.readNamespacedService(serviceName, platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
    }

    @Override
    public V1Service createK8sPlatformService(String platformId, V1Service service) throws ParamException, ApiException {
        logger.debug(String.format("create new Service for platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Service create = this.k8sApiService.createNamespacedService(platformId, service, platformPo.getApiUrl(), platformPo.getAuthToken());
        return create;
    }

    @Override
    public void deleteK8sPlatformService(String platformId, String serviceName) throws ParamException, ApiException {
        logger.debug(String.format("delete Service %s from platform %s"));
        PlatformPo platformPo = getK8sPlatform(platformId);
        this.k8sApiService.deleteNamespacedEndpoints(serviceName, platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
    }

    @Override
    public V1Service replaceK8sPlatformService(String platformId, String serviceName, V1Service service) throws ParamException, ApiException {
        logger.debug(String.format("replace Service %s of platform %s", serviceName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Service replace = this.k8sApiService.replaceNamespacedService(serviceName, platformId, service, platformPo.getApiUrl(), platformPo.getAuthToken());
        return replace;
    }

    @Override
    public List<V1ConfigMap> queryPlatformAllK8sConfigMaps(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("begin to query k8s configMap of %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        return this.k8sApiService.listNamespacedConfigMap(platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
    }

    @Override
    public V1ConfigMap queryPlatformK8sConfigMapByName(String platformId, String configMapName) throws ParamException, ApiException {
        logger.debug(String.format("begin to query k8s configMap %s of %s", configMapName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        return this.k8sApiService.readNamespacedConfigMap(platformId, configMapName, platformPo.getApiUrl(), platformPo.getAuthToken());
    }

    private PlatformPo getK8sPlatform(String platformId) throws ParamException {
        PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
        if (platformPo == null)
            throw new ParamException(String.format("%s platform not exit", platformId));
        if (!PlatformType.K8S_CONTAINER.equals(platformPo.getType())) {
            logger.error(String.format("platform %s type is %s not %s", platformId, platformPo.getType().name, PlatformType.K8S_CONTAINER.name));
            throw new ParamException(String.format("%s is not %s platform", platformId, PlatformType.K8S_CONTAINER.name));
        }
        if (StringUtils.isBlank(platformPo.getApiUrl())) {
            logger.error(String.format("k8s api url of %s is blank", platformId));
            throw new ParamException(String.format("k8s api url of %s is blank", platformId));
        }
        if (StringUtils.isBlank(platformPo.getAuthToken())) {
            logger.error(String.format("k8s auth token of %s is blank", platformId));
            throw new ParamException(String.format("k8s auth token of %s is blank", platformId));
        }
        return platformPo;
    }

    @Override
    public List<V1Deployment> queryPlatformAllK8sDeployment(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("query all deployment of platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        List<V1Deployment> list = this.k8sApiService.listNamespacedDeployment(platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
        return list;
    }

    @Override
    public V1Deployment queryPlatformK8sDeploymentByName(String platformId, String deploymentName) throws ParamException, ApiException {
        logger.debug(String.format("query deployment %s at platform %s", deploymentName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Deployment deployment = this.k8sApiService.readNamespacedDeployment(deploymentName, platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
        return deployment;
    }

    @Override
    public V1Deployment createK8sPlatformDeployment(String platformId, V1Deployment deployment) throws ParamException, ApiException {
        logger.debug(String.format("create new Deployment for platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Deployment create = this.k8sApiService.createNamespacedDeployment(platformId, deployment, platformPo.getApiUrl(), platformPo.getAuthToken());
        return create;
    }

    @Override
    public void deleteK8sPlatformDeployment(String platformId, String deploymentName) throws ParamException, ApiException {
        logger.debug(String.format("delete Deployment %s from platform %s"));
        PlatformPo platformPo = getK8sPlatform(platformId);
        this.k8sApiService.deleteNamespacedEndpoints(deploymentName, platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
    }

    @Override
    public V1Deployment replaceK8sPlatformDeployment(String platformId, String deploymentName, V1Deployment deployment) throws ParamException, ApiException {
        logger.debug(String.format("replace Deployment %s of platform %s", deploymentName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Deployment replace = this.k8sApiService.replaceNamespacedDeployment(deploymentName, platformId, deployment, platformPo.getApiUrl(), platformPo.getAuthToken());
        return replace;
    }

    @Override
    public ExtensionsV1beta1Ingress queryPlatformK8sIngressByName(String platformId, String ingressName) throws ParamException, ApiException {
        logger.debug(String.format("query ingress %s at platform %s", ingressName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        ExtensionsV1beta1Ingress ingress = this.k8sApiService.readNamespacedIngress(ingressName, platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
        return ingress;
    }

    @Override
    public List<ExtensionsV1beta1Ingress> queryPlatformAllK8sIngress(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("query all ingress of platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        List<ExtensionsV1beta1Ingress> list = this.k8sApiService.listNamespacedIngress(platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
        return list;
    }

    @Override
    public ExtensionsV1beta1Ingress createK8sPlatformIngress(String platformId, ExtensionsV1beta1Ingress ingress) throws ParamException, ApiException {
        logger.debug(String.format("create new Ingress for platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        ExtensionsV1beta1Ingress create = this.k8sApiService.createNamespacedIngress(platformId, ingress, platformPo.getApiUrl(), platformPo.getAuthToken());
        return create;
    }

    @Override
    public void deleteK8sPlatformIngress(String platformId, String ingressName) throws ParamException, ApiException {
        logger.debug(String.format("delete Ingress %s from platform %s"));
        PlatformPo platformPo = getK8sPlatform(platformId);
        this.k8sApiService.deleteNamespacedIngress(ingressName, platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
    }

    @Override
    public ExtensionsV1beta1Ingress replaceK8sPlatformIngress(String platformId, String ingressName, ExtensionsV1beta1Ingress ingress) throws ParamException, ApiException {
        logger.debug(String.format("replace Ingress %s of platform %s", ingressName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        ExtensionsV1beta1Ingress replace = this.k8sApiService.replaceNamespacedIngress(ingressName, platformId, ingress, platformPo.getApiUrl(), platformPo.getAuthToken());
        return replace;
    }

    @Override
    public List<V1Endpoints> queryPlatformAllK8sEndpoints(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("query all endpoints of platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        List<V1Endpoints> list = this.k8sApiService.listNamespacedEndpoints(platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
        return list;
    }

    @Override
    public V1Endpoints queryPlatformK8sEndpointsByName(String platformId, String endpointsName) throws ParamException, ApiException {
        logger.debug(String.format("query endpoints %s at platform %s", endpointsName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Endpoints endpoints = this.k8sApiService.readNamespacedEndpoints(endpointsName, platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
        return endpoints;
    }

    @Override
    public V1Endpoints createK8sPlatformEndpoints(String platformId, V1Endpoints endpoints) throws ParamException, ApiException {
        logger.debug(String.format("create new Endpoints for platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Endpoints create = this.k8sApiService.createNamespacedEndpoints(platformId, endpoints, platformPo.getApiUrl(), platformPo.getAuthToken());
        return create;
    }

    @Override
    public void deleteK8sPlatformEndpoints(String platformId, String endpointsName) throws ParamException, ApiException {
        logger.debug(String.format("delete Endpoints %s from platform %s"));
        PlatformPo platformPo = getK8sPlatform(platformId);
        this.k8sApiService.deleteNamespacedEndpoints(endpointsName, platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
    }

    @Override
    public V1Endpoints replaceK8sPlatformEndpoints(String platformId, String endpointName, V1Endpoints endpoints) throws ParamException, ApiException {
        logger.debug(String.format("replace Endpoints %s of platform %s", endpointName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Endpoints replace = this.k8sApiService.replaceNamespacedEndpoints(endpointName, platformId, endpoints, platformPo.getApiUrl(), platformPo.getAuthToken());
        return replace;
    }

    @Override
    public List<V1Secret> queryPlatformAllK8sSecret(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("query all endpoints of platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        List<V1Secret> list = this.k8sApiService.listNamespacedSecret(platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
        return list;
    }

    @Override
    public V1Secret queryPlatformK8sSecretByName(String platformId, String secretName) throws ParamException, ApiException {
        logger.debug(String.format("query secretName %s at platform %s", secretName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Secret secret = this.k8sApiService.readNamespacedSecret(secretName, platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
        return secret;
    }

    @Override
    public V1Secret createK8sPlatformSecret(String platformId, V1Secret secret) throws ParamException, ApiException {
        logger.debug(String.format("create new Secret for platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Secret create = this.k8sApiService.createNamespacedSecret(platformId, secret, platformPo.getApiUrl(), platformPo.getAuthToken());
        return create;
    }

    @Override
    public void deleteK8sPlatformSecret(String platformId, String secretName) throws ParamException, ApiException {
        logger.debug(String.format("delete Secret %s from platform %s"));
        PlatformPo platformPo = getK8sPlatform(platformId);
        this.k8sApiService.deleteNamespacedSecret(secretName, platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
    }

    @Override
    public V1Secret replaceK8sPlatformSecret(String platformId, String secretName, V1Secret secret) throws ParamException, ApiException {
        logger.debug(String.format("replace Secret %s of platform %s", secretName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1Secret replace = this.k8sApiService.replaceNamespacedSecret(secretName, platformId, secret, platformPo.getApiUrl(), platformPo.getAuthToken());
        return replace;
    }

    @Override
    public List<V1PersistentVolumeClaim> queryPlatformAllK8sPersistentVolumeClaim(String platformId) throws ParamException, ApiException {
        logger.debug(String.format("query all PersistentVolumeClaim of platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        List<V1PersistentVolumeClaim> list = this.k8sApiService.listNamespacedPersistentVolumeClaim(platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
        return list;
    }

    @Override
    public V1PersistentVolumeClaim queryPlatformK8sPersistentVolumeClaimByName(String platformId, String persistentVolumeClaimName) throws ParamException, ApiException {
        logger.debug(String.format("query PersistentVolumeClaim %s at platform %s", persistentVolumeClaimName, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1PersistentVolumeClaim claim = this.k8sApiService.readNamespacedPersistentVolumeClaim(persistentVolumeClaimName, platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
        return claim;
    }

    @Override
    public V1PersistentVolumeClaim createK8sPlatformPersistentVolumeClaim(String platformId, V1PersistentVolumeClaim persistentVolumeClaim) throws ParamException, ApiException {
        logger.debug(String.format("create new PersistentVolumeClaim for platform %s", platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1PersistentVolumeClaim create = this.k8sApiService.createNamespacedPersistentVolumeClaim(platformId, persistentVolumeClaim, platformPo.getApiUrl(), platformPo.getAuthToken());
        return create;
    }

    @Override
    public void deleteK8sPlatformPersistentVolumeClaim(String platformId, String persistentVolumeClaimName) throws ParamException, ApiException {
        logger.debug(String.format("delete PersistentVolumeClaim %s from platform %s"));
        PlatformPo platformPo = getK8sPlatform(platformId);
        this.k8sApiService.deleteNamespacedPersistentVolumeClaim(persistentVolumeClaimName, platformId, platformPo.getApiUrl(), platformPo.getAuthToken());
    }

    @Override
    public V1PersistentVolumeClaim replaceK8sPlatformPersistentVolumeClaim(String platformId, String persistentVolumeClaimName, V1PersistentVolumeClaim persistentVolumeClaim) throws ParamException, ApiException {
        logger.debug(String.format("replace PersistentVolumeClaim %s of platform %s", persistentVolumeClaim, platformId));
        PlatformPo platformPo = getK8sPlatform(platformId);
        V1PersistentVolumeClaim replace = this.k8sApiService.replaceNamespacedPersistentVolumeClaim(persistentVolumeClaimName, platformId, persistentVolumeClaim, platformPo.getApiUrl(), platformPo.getAuthToken());
        return replace;
    }

    @Override
    public List<V1ConfigMap> createConfigMapForNewPlatform(PlatformUpdateSchemaInfo createSchema) throws InterfaceCallException, IOException, ApiException {
        String platformId = createSchema.getPlatformId();
        String k8sApiUrl = createSchema.getK8sApiUrl();
        String k8sAuthToken = createSchema.getK8sAuthToken();
        String jobId = createSchema.getSchemaId();
        logger.debug(String.format("create configMap for %s from %s", platformId, createSchema.getK8sApiUrl()));
        if (createSchema.getPublicConfig() != null && createSchema.getPublicConfig().size() > 0) {
            List<NexusAssetInfo> publicConfigList = new ArrayList<>();
            for (AppFileNexusInfo cfg : createSchema.getPublicConfig())
                publicConfigList.add(cfg.getNexusAssetInfo(this.nexusHostUrl));
            this.k8sApiService.createConfigMapFromNexus(platformId, platformId, k8sApiUrl, k8sAuthToken, publicConfigList, this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
        }
        for (DomainUpdatePlanInfo planInfo : createSchema.getDomainUpdatePlanList()) {
            String domainId = planInfo.getDomainId();
            if (planInfo.getPublicConfig() != null && planInfo.getPublicConfig().size() > 0) {
                List<NexusAssetInfo> domainPublicConfigs = new ArrayList<>();
                for (AppFileNexusInfo cfg : planInfo.getPublicConfig())
                    domainPublicConfigs.add(cfg.getNexusAssetInfo(nexusHostUrl));
                this.k8sApiService.createConfigMapFromNexus(platformId, domainId, k8sApiUrl, k8sAuthToken, domainPublicConfigs, this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
            }
            for (AppUpdateOperationInfo optInfo : planInfo.getAppUpdateOperationList()) {
                List<NexusAssetInfo> appCfgs = new ArrayList<>();
                for (AppFileNexusInfo cfg : optInfo.getCfgs())
                    appCfgs.add(cfg.getNexusAssetInfo(nexusHostUrl));
                this.k8sApiService.createConfigMapFromNexus(platformId, String.format("%s-%s", optInfo.getAppAlias(), domainId), k8sApiUrl, k8sAuthToken, appCfgs, this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
            }
        }
        return this.k8sApiService.listNamespacedConfigMap(platformId, k8sApiUrl, k8sAuthToken);
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

    private void someTest() throws Exception {
        String jsonStr = "{\"k8sApiUrl\":\"https://10.130.41.218:6443\",\"k8sAuthToken\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA\",\"baseDataNexusPath\":\"ccod/4.1/baseVolume.zip\",\"baseDataNexusRepository\":\"platform_base_data\",\"bkBizId\":34,\"bkCloudId\":0,\"ccodVersion\":\"CCOD4.1\",\"comment\":\"create 工具组平台(202005-test) by clone 123456-wuph(ccod开发测试平台)\",\"createTime\":1591061579290,\"deadline\":1591061579290,\"domainUpdatePlanList\":[{\"appUpdateOperationList\":[{\"addDelay\":0,\"appAlias\":\"licenseserver\",\"appName\":\"LicenseServer\",\"appRunner\":\"licenseserver\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./bin/license/\",\"ext\":\"ini\",\"fileName\":\"Config.ini\",\"fileSize\":0,\"md5\":\"6c513269c4e2bc10f4a6cf0eb05e5bfc\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNlZGYyZTBkYjgyNWQ0OTRi\",\"nexusPath\":\"/configText/202005-test/public01_licenseserver/Config.ini\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"5214\"},{\"addDelay\":0,\"appAlias\":\"configserver\",\"appName\":\"configserver\",\"appRunner\":\"configserver\",\"basePath\":\"/home/cfs/Platform/\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"ccs_config.cfg\",\"fileSize\":0,\"md5\":\"1095494274dc98445b79ec1d32900a6f\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY5NWYyNTlkYWY3ZWEzZWNl\",\"nexusPath\":\"/configText/202005-test/public01_configserver/ccs_config.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"ccs_logger.cfg\",\"fileSize\":0,\"md5\":\"197075eb110327da19bfc2a31f24b302\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ1NGYyYWEyOGE2ZGNhYjlh\",\"nexusPath\":\"/configText/202005-test/public01_configserver/ccs_logger.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"aca2af60caa0fb9f4af57f37f869dafc90472525\"},{\"addDelay\":30,\"appAlias\":\"glsserver\",\"appName\":\"glsServer\",\"appRunner\":\"glsserver\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"gls_config.cfg\",\"fileSize\":0,\"md5\":\"f23a83a2d871d59c89d12b0281e10e90\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmMmRiNjc0YzQ5YmE4Nzdj\",\"nexusPath\":\"/configText/202005-test/public01_glsserver/gls_config.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"gls_logger.cfg\",\"fileSize\":0,\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFhYTlmZWY0MTJkMDY2ZTM3\",\"nexusPath\":\"/configText/202005-test/public01_glsserver/gls_logger.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\"}],\"bkSetName\":\"公共组件\",\"comment\":\"clone from 公共组件01 of 123456-wuph\",\"createTime\":1591061579289,\"domainId\":\"public01\",\"domainName\":\"公共组件01\",\"executeTime\":1591061579289,\"maxOccurs\":1000,\"occurs\":600,\"status\":\"CREATE\",\"tags\":\"入呼叫,外呼\",\"updateTime\":1591061579289,\"updateType\":\"ADD\"},{\"appUpdateOperationList\":[{\"addDelay\":20,\"appAlias\":\"dds\",\"appName\":\"DDSServer\",\"appRunner\":\"dds\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dds_logger.cfg\",\"fileSize\":0,\"md5\":\"7f783a4ea73510c73ac830f135f4c762\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5MDNhNjZiNDlmZDMxNzYx\",\"nexusPath\":\"/configText/202005-test/cloud01_dds/dds_logger.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dds_config.cfg\",\"fileSize\":0,\"md5\":\"d89e98072e96a06efa41c69855f4a3cc\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZjOTYzMzczYzhmZjFjMTRm\",\"nexusPath\":\"/configText/202005-test/cloud01_dds/dds_config.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"150:18722\"},{\"addDelay\":20,\"appAlias\":\"ucds\",\"appName\":\"UCDServer\",\"appRunner\":\"ucds\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"ucds_config.cfg\",\"fileSize\":0,\"md5\":\"f4445f10c75c9ef2f6d4de739c634498\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxMzRlYzQyOWIwNzZlMDU0\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/ucds_config.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"ucds_logger.cfg\",\"fileSize\":0,\"md5\":\"ec57329ddcec302e0cc90bdbb8232a3c\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFjYWUzYjQxZWU5NTMxOTg4\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/ucds_logger.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"DRWRClient.cfg\",\"fileSize\":0,\"md5\":\"8b901d87855de082318314d868664c03\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ2NWYyYzE5NTAwNDY4YzQ1\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/DRWRClient.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\"},{\"addDelay\":20,\"appAlias\":\"dcs\",\"appName\":\"dcs\",\"appRunner\":\"dcs\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dc_log4cpp.cfg\",\"fileSize\":0,\"md5\":\"5784d6983f5e6722622b727d0987a15e\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE0ODU4YTJmMGM3M2FlNTE5\",\"nexusPath\":\"/configText/202005-test/cloud01_dcs/dc_log4cpp.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"DCServer.cfg\",\"fileSize\":0,\"md5\":\"ce208427723a0ebc0fff405fd7c382dc\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjYwNGZjN2U3YWFlN2YwYWYy\",\"nexusPath\":\"/configText/202005-test/cloud01_dcs/DCServer.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"155:21974\"},{\"addDelay\":20,\"appAlias\":\"cms1\",\"appName\":\"cmsserver\",\"appRunner\":\"cms1\",\"basePath\":\"/home/ccodrunner/Platform/\",\"cfgs\":[{\"deployPath\":\"./etc/\",\"ext\":\"xml\",\"fileName\":\"beijing.xml\",\"fileSize\":0,\"md5\":\"4074321f266b42fe7d7266b6fa9d7ca2\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5NmMwMTdkNWU4ZjE3NGRi\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/beijing.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./etc/\",\"ext\":\"cms2\",\"fileName\":\"config.cms2\",\"fileSize\":0,\"md5\":\"cf032451250db89948f775e4d7799e40\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZlYmViNGRkNzM3YjM5MzE5\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/config.cms2\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./etc/\",\"ext\":\"cfg\",\"fileName\":\"cms_log4cpp.cfg\",\"fileSize\":0,\"md5\":\"b16210d40a7ef123eef0296393df37b8\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmMDEzYTFjMTFkNzBlNDkz\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/cms_log4cpp.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\"},{\"addDelay\":20,\"appAlias\":\"cms2\",\"appName\":\"cmsserver\",\"appRunner\":\"cms2\",\"basePath\":\"/home/ccodrunner/Platform/\",\"cfgs\":[{\"deployPath\":\"./etc/\",\"ext\":\"xml\",\"fileName\":\"beijing.xml\",\"fileSize\":0,\"md5\":\"4074321f266b42fe7d7266b6fa9d7ca2\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM0OTk5N2I3NjRhZWIzNDg5\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/beijing.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./etc/\",\"ext\":\"cfg\",\"fileName\":\"cms_log4cpp.cfg\",\"fileSize\":0,\"md5\":\"b16210d40a7ef123eef0296393df37b8\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQxN2NjMTNmZmJhYjRlMWZh\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/cms_log4cpp.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./etc/\",\"ext\":\"cms2\",\"fileName\":\"config.cms2\",\"fileSize\":0,\"md5\":\"5f5e2e498e5705b84297b2721fdbb603\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFiODMyZmE4OTU2MmM3NmNh\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/config.cms2\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\"},{\"addDelay\":20,\"appAlias\":\"ucx\",\"appName\":\"ucxserver\",\"appRunner\":\"ucx\",\"basePath\":\"/home/ccodrunner/Platform/\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"ucx\",\"fileName\":\"config.ucx\",\"fileSize\":0,\"md5\":\"0c7c8b38115a9d0cabb2d1505f195821\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZlNTVjMGQzNzkxMjA3MzYw\",\"nexusPath\":\"/configText/202005-test/cloud01_ucx/config.ucx\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"1fef2157ea07c483979b424c758192bd709e6c2a\"},{\"addDelay\":20,\"appAlias\":\"daengine\",\"appName\":\"daengine\",\"appRunner\":\"daengine\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dae.cfg\",\"fileSize\":0,\"md5\":\"431128629db6c93804b86cc1f9428a87\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE1ODQyOGRjYjc2YzRjODdj\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dae_logger.cfg\",\"fileSize\":0,\"md5\":\"ac2fde58b18a5ab1ee66d911982a326c\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3Mzc5Y2E3NzU2ZjczMDEy\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_logger.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dae_config.cfg\",\"fileSize\":0,\"md5\":\"04544c8572c42b176d501461168dacf4\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxM2UzYmZmYzhmOGY2NWMw\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_config.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dae_log4cpp.cfg\",\"fileSize\":0,\"md5\":\"ece32d86439201eefa186fbe8ad6db06\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ2OWMwMGQ4YTFlZjRhZDQ4\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_log4cpp.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"179:20744\"},{\"addDelay\":20,\"appAlias\":\"dcproxy\",\"appName\":\"dcproxy\",\"appRunner\":\"dcproxy\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dcp_config.cfg\",\"fileSize\":0,\"md5\":\"087cb6d8e6263dc6f1e8079fac197983\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM5YThlM2QxMjI2NDc3NzZh\",\"nexusPath\":\"/configText/202005-test/cloud01_dcproxy/dcp_config.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dcp_logger.cfg\",\"fileSize\":0,\"md5\":\"8d3d4de160751677d6a568c9d661d7c0\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEyZmRkNzJmMWJjZDMwYWNj\",\"nexusPath\":\"/configText/202005-test/cloud01_dcproxy/dcp_logger.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"195:21857\"},{\"addDelay\":0,\"appAlias\":\"ss\",\"appName\":\"StatSchedule\",\"appRunner\":\"ss\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"ss_config.cfg\",\"fileSize\":0,\"md5\":\"825e14101d79c733b2ea8becb8ea4e3b\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5YTgxMTU1YjhmNDMyZjU4\",\"nexusPath\":\"/configText/202005-test/cloud01_ss/ss_config.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"154:21104\"}],\"bkSetName\":\"域服务\",\"comment\":\"clone from 域服务01 of 123456-wuph\",\"createTime\":1591061579289,\"domainId\":\"cloud01\",\"domainName\":\"域服务01\",\"executeTime\":1591061579289,\"maxOccurs\":1000,\"occurs\":600,\"status\":\"CREATE\",\"tags\":\"入呼叫,外呼\",\"updateTime\":1591061579289,\"updateType\":\"ADD\"},{\"appUpdateOperationList\":[{\"addDelay\":0,\"appAlias\":\"cas\",\"appName\":\"cas\",\"appRunner\":\"cas\",\"basePath\":\"/home/portal/tomcat/webapps/\",\"cfgs\":[{\"deployPath\":\"./cas/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE2MDA0MWExZjQ1ODc4Njhh\",\"nexusPath\":\"/configText/202005-test/manage01_cas/web.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cas/WEB-INF/\",\"ext\":\"properties\",\"fileName\":\"cas.properties\",\"fileSize\":0,\"md5\":\"6622e01a4df917d747e078e89c774a52\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3N2E3NDBhYmM0NzU2NDg5\",\"nexusPath\":\"/configText/202005-test/manage01_cas/cas.properties\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"10973\"},{\"addDelay\":0,\"appAlias\":\"customwebservice\",\"appName\":\"customWebservice\",\"appRunner\":\"customwebservice\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./customWebservice/WEB-INF/classes/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"96e5bc553847dab185d32c260310bb77\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRiNGZjNzM0NjU4MDMyNTZl\",\"nexusPath\":\"/configText/202005-test/manage01_customwebservice/web.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./customWebservice/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"24eebd53ad6d6d2585f8164d189b4592\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM3ZDgwMWZlMjNlNjU1MWNk\",\"nexusPath\":\"/configText/202005-test/manage01_customwebservice/config.properties\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"19553\"},{\"addDelay\":0,\"appAlias\":\"dcms\",\"appName\":\"dcms\",\"appRunner\":\"dcms\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./dcms/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"52ba707ab07e7fcd50d3732268dd9b9d\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZiMDVmY2FhMjFmZmNhN2Iz\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/web.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcms/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"98a8781d1808c69448c9666642d7b8ed\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM5NTA4MmY4ODU2NmJhZmJj\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/config.properties\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcms/WEB-INF/classes/\",\"ext\":\"xml\",\"fileName\":\"Param-Config.xml\",\"fileSize\":0,\"md5\":\"9a977ea04c6e936307bec2683cadd379\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFlODhlMDRiZWM1NTBkZTc0\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/Param-Config.xml\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"11110\"},{\"addDelay\":0,\"appAlias\":\"dcmsrecord\",\"appName\":\"dcmsRecord\",\"appRunner\":\"dcmsrecord\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./dcmsRecord/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"a4500823701a6b430a98b25eeee6fea3\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY4YjgyMjllYTBmNzcwYzI4\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/web.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsRecord/WEB-INF/classes/\",\"ext\":\"xml\",\"fileName\":\"applicationContext.xml\",\"fileSize\":0,\"md5\":\"5a355d87e0574ffa7bc120f61d8bf61e\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEwODE2YjIwM2VjNmEzYjA0\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/applicationContext.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsRecord/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"830bf1a0205f407eba5f3a449b749cba\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRlOGM3ZWI3YmIwMGI5ZDJk\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/config.properties\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"21763\"},{\"addDelay\":0,\"appAlias\":\"dcmssg\",\"appName\":\"dcmssg\",\"appRunner\":\"dcmssg\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./dcmsSG/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"e76da17fe273dc7f563a9c7c86183d20\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ0Y2ZlOWIxNTkzOWVhZTYz\",\"nexusPath\":\"/configText/202005-test/manage01_dcmssg/config.properties\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsSG/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"52a87ceaeebd7b9bb290ee863abe98c9\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMyYWJhYzhlZWNiYjdmNTAw\",\"nexusPath\":\"/configText/202005-test/manage01_dcmssg/web.xml\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"20070\"},{\"addDelay\":0,\"appAlias\":\"dcmsstatics\",\"appName\":\"dcmsStatics\",\"appRunner\":\"dcmsstaticsreport2\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./dcmsStatics/WEB-INF/classes/\",\"ext\":\"xml\",\"fileName\":\"applicationContext.xml\",\"fileSize\":0,\"md5\":\"2a7ef2d3a9fc97e8e59db7f21b7d4d45\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNkZmI4MWI4ODk5NjNkZDZk\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/applicationContext.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsStatics/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"7212df6a667e72ecb604b03fee20f639\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY0ZDc1YmFiZTlkMDUxYTRi\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/web.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsStatics/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"973ba4d65b93a47bb5ead294b9415e68\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4ODkyNWYwNTA1ZTk1NWJi\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/config.properties\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"20537\"},{\"addDelay\":0,\"appAlias\":\"dcmsstaticsreport\",\"appName\":\"dcmsStaticsReport\",\"appRunner\":\"dcmsstaticsreport1\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./dcmsStaticsReport/WEB-INF/classes/\",\"ext\":\"xml\",\"fileName\":\"applicationContext.xml\",\"fileSize\":0,\"md5\":\"1e2f67f773110caf7a91a1113564ce4c\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3MzVkODMwZTVlMTFkZTRk\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/applicationContext.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsStaticsReport/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"f02bb5a99546b80a3a82f55154be143d\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEyYzVkMWFhMDExOTUwODQz\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/config.properties\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsStaticsReport/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"5a32768163ade7c4bce70270d79b6c66\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRlOGM5MTIwOTY2ZWRkMjZk\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/web.xml\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"20528\"},{\"addDelay\":0,\"appAlias\":\"dcmswebservice\",\"appName\":\"dcmsWebservice\",\"appRunner\":\"dcmswebservice\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./dcmsWebservice/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"9a9671156ab2454951b9561fbefeed42\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxYjBmNjYxYzFmNDljYTkx\",\"nexusPath\":\"/configText/202005-test/manage01_dcmswebservice/config.properties\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsWebservice/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"d93dd1fe127f46a13f27d6f8d4a7def3\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ3YzI3NWZhZTNlMjFkMmVm\",\"nexusPath\":\"/configText/202005-test/manage01_dcmswebservice/web.xml\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"20503\"},{\"addDelay\":0,\"appAlias\":\"dcmsx\",\"appName\":\"dcmsx\",\"appRunner\":\"dcmsx\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./dcmsx/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"5d6550ab653769a49006b4957f9a0a65\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3OWVmMzQyNGYzODg0ODBi\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsx/web.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsx/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"application.properties\",\"fileSize\":0,\"md5\":\"675edca3ccfa6443d8dfc9b34b1eee0b\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4MmE0YzY0YjQxNDc1ODU4\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsx/application.properties\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"master_8efabf4\"},{\"addDelay\":0,\"appAlias\":\"safetymonitor\",\"appName\":\"safetyMonitor\",\"appRunner\":\"safetymonitor\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./safetyMonitor/WEB-INF/classes/\",\"ext\":\"xml\",\"fileName\":\"applicationContext.xml\",\"fileSize\":0,\"md5\":\"4543cb1aba46640edc3e815750fd3a94\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY5ZDNkNTQ5NTk5Yjg0Mjc2\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/applicationContext.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./safetyMonitor/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"752b9c6cc870d294fa413d64c090e49e\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmYjk0NDEwZTVlN2NkMjIw\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/config.properties\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./safetyMonitor/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"4d952d3e6d356156dd461144416f4816\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ1MjYyN2JlYzdjMGI0ZmRl\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/web.xml\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"20383\"}],\"bkSetName\":\"管理门户\",\"comment\":\"clone from 管理门户01 of 123456-wuph\",\"createTime\":1591061579289,\"domainId\":\"manage01\",\"domainName\":\"管理门户01\",\"executeTime\":1591061579289,\"maxOccurs\":1000,\"occurs\":600,\"status\":\"CREATE\",\"tags\":\"入呼叫,外呼\",\"updateTime\":1591061579289,\"updateType\":\"ADD\"},{\"appUpdateOperationList\":[{\"addDelay\":0,\"appAlias\":\"gls\",\"appName\":\"gls\",\"appRunner\":\"gls\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./gls/WEB-INF/classes/\",\"ext\":\"xml\",\"fileName\":\"Param-Config.xml\",\"fileSize\":0,\"md5\":\"1da62c81dacf6d7ee21fca3384f134c5\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4M2U3MDJjNjViN2E5MjQw\",\"nexusPath\":\"/configText/202005-test/ops01_gls/Param-Config.xml\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"10309\"}],\"bkSetName\":\"运营门户\",\"comment\":\"clone from 运营门户01 of 123456-wuph\",\"createTime\":1591061579290,\"domainId\":\"ops01\",\"domainName\":\"运营门户01\",\"executeTime\":1591061579290,\"maxOccurs\":1000,\"occurs\":600,\"status\":\"CREATE\",\"tags\":\"入呼叫,外呼\",\"updateTime\":1591061579290,\"updateType\":\"ADD\"}],\"executeTime\":1591061579290,\"glsDBPwd\":\"ccod\",\"glsDBType\":\"ORACLE\",\"glsDBUser\":\"ccod\",\"k8sHostIp\":\"10.130.41.218\",\"platformId\":\"202005-test\",\"platformName\":\"工具组平台\",\"publicConfig\":[{\"deployPath\":\"/root/resin-4.0.13/conf\",\"fileName\":\"local_datasource.xml\",\"fileSize\":0,\"md5\":\"112940181aeb983baa9d7fd2733f194f\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmNmE4MWY3MWI3MmJlY2Ji\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_datasource.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"/root/resin-4.0.13/conf\",\"fileName\":\"local_jvm.xml\",\"fileSize\":0,\"md5\":\"d172a5321944aba5bc19c35d00950afc\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRjMWQyMTIwNWRmYmY1MDM0\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_jvm.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"/usr/local/lib\",\"fileName\":\"tnsnames.ora\",\"fileSize\":0,\"md5\":\"811f7f9472d5f6e733d732619a17ac77\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY0NzFkOTU3MGMyODRjMGJm\",\"nexusPath\":\"/configText/202005-test/publicConfig/tnsnames.ora\",\"nexusRepository\":\"tmp\"}],\"schemaId\":\"e2b849bd-0ac4-4591-97f3-eee6e7084a58\",\"status\":\"CREATE\",\"taskType\":\"CREATE\",\"title\":\"新建工具组平台(202005-test)计划\",\"updateTime\":1591061579290}";
        PlatformUpdateSchemaInfo createSchema = JSONObject.parseObject(jsonStr, PlatformUpdateSchemaInfo.class);
        String platformId = createSchema.getPlatformId();
        String k8sApiUrl = createSchema.getK8sApiUrl();
        String k8sAuthToken = createSchema.getK8sAuthToken();
        List<V1ConfigMap> mapList = this.k8sApiService.listNamespacedConfigMap(platformId, k8sApiUrl, k8sAuthToken);
        for (V1ConfigMap configMap : mapList) {
            this.k8sApiService.deleteNamespacedConfigMap(platformId, configMap.getMetadata().getName(), k8sApiUrl, k8sAuthToken);
        }
        mapList = this.createConfigMapForNewPlatform(createSchema);
        System.out.println(String.format("create %d configMap for %s", mapList.size(), platformId));
    }

    private K8sPlatformParamVo getK8sPlatformParam(String platformId, String platformName, String ccodVersion, List<V1Deployment> deployments, List<V1Service> services, List<V1ConfigMap> configMaps, List<AppModuleVo> registerApps) throws IOException, K8sDataException, ParamException, NotSupportAppException, InterfaceCallException, NexusException {
        K8sPlatformParamVo paramVo = new K8sPlatformParamVo(platformId, platformName);
        Map<V1Service, V1Deployment> svcDeployMap = new HashMap<>();
        List<V1Service> notDeploySvcList = new ArrayList<>();
        for (V1Service service : services) {
            for (V1Deployment deployment : deployments) {
                boolean isMatch = isSelected(deployment.getMetadata().getLabels(), service.getSpec().getSelector());
                if (isMatch && svcDeployMap.containsKey(service))
                    throw new K8sDataException(String.format("service %s has related to deployment %s and %s",
                            service.getMetadata().getName(), deployment.getMetadata().getName(), svcDeployMap.get(service).getMetadata().getName()));
                else if (isMatch)
                    svcDeployMap.put(service, deployment);
            }
            if (!svcDeployMap.containsKey(service))
                notDeploySvcList.add(service);
        }
        Map<V1Deployment, List<V1Service>> deploySvcMap = new HashMap<>();
        for (V1Deployment deployment : deployments) {
            deploySvcMap.put(deployment, new ArrayList<>());
            for (V1Service service : services) {
                boolean isMatch = isSelected(deployment.getMetadata().getLabels(), service.getSpec().getSelector());
                if (isMatch)
                    deploySvcMap.get(deployment).add(service);
            }
        }
        Map<String, V1ConfigMap> configMapMap = new HashMap<>();
        for (V1ConfigMap configMap : configMaps)
            configMapMap.put(configMap.getMetadata().getName(), configMap);
        SimpleDateFormat sf = new SimpleDateFormat();
        Date now = new Date();
        String tag = sf.format(now);
        for (V1Deployment deployment : deployments) {
            String deploymentName = deployment.getMetadata().getName();
            List<V1Container> containers = new ArrayList<>();
            if (deployment.getSpec().getTemplate().getSpec().getInitContainers() != null)
                containers.addAll(deployment.getSpec().getTemplate().getSpec().getInitContainers());
            if (deployment.getSpec().getTemplate().getSpec().getContainers() != null)
                containers.addAll(deployment.getSpec().getTemplate().getSpec().getContainers());
            for (V1Container container : containers) {
                AppType appType = getAppTypeFromImageUrl(container.getImage());
                switch (appType) {
                    case BINARY_FILE:
                    case RESIN_WEB_APP: {
                        AppModuleVo moduleVo = getAppModuleFromImageTag(container.getImage(), registerApps);
                        if (paramVo.getDeployAppList().stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAppName, Function.identity())).containsKey(moduleVo.getAppName()))
                            throw new K8sDataException(String.format("deployment %s has duplicate module %s", deploymentName, moduleVo.getAppName()));
                        PlatformAppDeployDetailVo deployApp = getK8sPlatformAppDeployDetail(platformName, ccodVersion, deployment, container, deploySvcMap.get(deployment), moduleVo, configMapMap.get(container.getName()).getData());
                        String directory = deployApp.getCfgNexusDirectory(tag);
                        List<NexusAssetInfo> assets = uploadK8sConfigMapToNexus(configMapMap.get(container.getName()), directory);
                        List<PlatformAppCfgFilePo> cfgs = new ArrayList<>();
                        String deployPath = container.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get(String.format("%s-volume", container.getName())).getMountPath();
                        for (NexusAssetInfo assetInfo : assets) {
                            PlatformAppCfgFilePo cfgFilePo = new PlatformAppCfgFilePo(0, moduleVo.getAppId(), deployPath, assetInfo);
                            cfgs.add(cfgFilePo);
                        }
                        deployApp.setCfgs(cfgs);
                        paramVo.getDeployAppList().add(deployApp);
                        break;
                    }
                    case THREE_PART_APP:
                        PlatformThreePartAppPo threePartAppPo = getK8sPlatformThreePartApp(deployment, container, deploySvcMap.get(deployment));
                        paramVo.getThreeAppList().add(threePartAppPo);
                        break;
                    default:
                        break;
                }
            }
        }
        for (V1Service threeSvc : notDeploySvcList) {
            PlatformThreePartServicePo threePartServicePo = getK8sPlatformThreePartService(threeSvc);
            paramVo.getThreeSvcList().add(threePartServicePo);
        }
        if (configMapMap.containsKey(platformId)) {
            V1ConfigMap configMap = configMapMap.get(platformId);
            String directory = String.format("%s/%s/publicConfig", platformId, tag);
            List<NexusAssetInfo> assets = uploadK8sConfigMapToNexus(configMap, directory);
            for (NexusAssetInfo asset : assets) {
                PlatformPublicConfigPo cfgPo = new PlatformPublicConfigPo(platformId, "/", asset);
                paramVo.getPlatformPublicConfigList().add(cfgPo);
            }
        }
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = paramVo.getDeployAppList().stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        for (String domainId : domainAppMap.keySet()) {
            if (configMapMap.containsKey(domainId)) {
                V1ConfigMap configMap = configMapMap.get(domainId);
                String directory = String.format("%s/%s/%s/publicConfig", platformId, tag, domainId);
                List<NexusAssetInfo> assets = uploadK8sConfigMapToNexus(configMap, directory);
                for (NexusAssetInfo asset : assets) {
                    DomainPublicConfigPo cfgPo = new DomainPublicConfigPo(domainId, platformId, "/", asset);
                    paramVo.getDomainPublicConfigList().add(cfgPo);
                }
            }
        }
        return paramVo;
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
        vo.setAppAlias(alias);
        vo.setOriginalAlias(alias);
        vo.setAppType(appModuleVo.getAppType());
        vo.setHostIp(services.get(0).getSpec().getClusterIP());
        vo.setPort(getPortFromK8sService(services));
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
        vo.setSrcCfgs(appModuleVo.getCfgs());
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

    /**
     * 根据basePath获得相对路径代表的绝对路径
     *
     * @param basePath     basePath
     * @param relativePath 需要被转换成绝对路径的相对路径
     * @return 相对路径对应的绝对路径
     * @throws ParamException
     */
    private String getAbsolutePath(String basePath, String relativePath) throws ParamException {
        if (relativePath.matches("^/.*"))
            return relativePath;
        else if (relativePath.matches("^\\./.*"))
            return String.format("%s/%s", basePath, relativePath.replaceAll("^\\./", "")).replaceAll("//", "/");
        else if (relativePath.matches("^\\.\\./.*")) {
            int count = 1;
            String str = relativePath.replaceAll("^\\.\\./", "");
            while (str.matches("^\\.\\./.*")) {
                count++;
                str = str.replaceAll("^\\.\\./", "");
            }
            String[] arr = basePath.replaceAll("^/", "").replaceAll("/$", "").split("/");
            String abPath = "/";
            for (int i = 0; i < arr.length - count; i++)
                abPath = String.format("%s%s/", abPath, arr[i]);
            return String.format("%s%s", abPath, str);
        } else
            return String.format("%s/%s", basePath, relativePath).replaceAll("//", "/");
    }

    /**
     * 将为容器挂载指定的配置文件信息
     * @param configName 添加的configMap的名字
     * @param cfgs       需要加载的配置文件
     * @param basePath   部署配置文件的basePath
     * @param deployPath 执行程序部署路径
     * @param moduleVo   被挂载配置文件的ccod域模块定义
     * @param mountPath  挂载路径
     * @param isPublic   被挂载的配置文件是否是公共配置
     * @param container  需要挂载configMap的容器
     * @param deployment 容器所属的deployment
     */
    public void addModuleCfgToContainer(String configName, List<AppFileNexusInfo> cfgs, String basePath, String deployPath, AppModuleVo moduleVo, String mountPath, boolean isPublic, V1Container container, V1Deployment deployment) throws ParamException {
        String volumeName = String.format("%s-volume", configName);
        if (!deployment.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity())).containsKey(volumeName)) {
            V1ConfigMapVolumeSource source = new V1ConfigMapVolumeSource();
            source.setItems(new ArrayList<>());
            source.setName(configName);
            for (AppFileNexusInfo cfg : cfgs) {
                V1KeyToPath item = new V1KeyToPath();
                item.setKey(cfg.getFileName());
                item.setPath(cfg.getFileName());
                source.getItems().add(item);
            }
            V1Volume volume = new V1Volume();
            volume.setName(volumeName);
            volume.setConfigMap(source);
            deployment.getSpec().getTemplate().getSpec().getVolumes().add(volume);
        }
        V1VolumeMount mount = new V1VolumeMount();
        mount.setName(volumeName);
        AppType appType = moduleVo.getAppType();
        mount.setMountPath(mountPath);
        container.getVolumeMounts().add(mount);
        String execParam = "";
        if (appType.equals(AppType.BINARY_FILE) && !isPublic)
            execParam = String.format("mkdir %s -p;mkdir %s/log -p;mv /opt/%s %s/%s", deployPath, basePath, moduleVo.getInstallPackage().getFileName(), deployPath, moduleVo.getInstallPackage().getFileName());
        else if (appType.equals(AppType.RESIN_WEB_APP) && !isPublic)
            execParam = String.format("mkdir %s -p;cd %s;mv /opt/%s %s/%s", deployPath, deployPath, moduleVo.getInstallPackage().getFileName(), deployPath, moduleVo.getInstallPackage().getFileName());
        Map<String, List<AppFileNexusInfo>> deployPathCfgMap = cfgs.stream().collect(Collectors.groupingBy(AppFileNexusInfo::getDeployPath));
        for (String cfgDeployPath : deployPathCfgMap.keySet()) {
            String absolutePath = getAbsolutePath(basePath, cfgDeployPath);
            execParam = String.format("%s;mkdir %s -p", execParam, absolutePath);
            for (AppFileNexusInfo cfg : deployPathCfgMap.get(cfgDeployPath)) {
                execParam = String.format("%s;cp %s/%s %s/%s", execParam, mountPath, cfg.getFileName(), absolutePath, cfg.getFileName());
                if (appType.equals(AppType.RESIN_WEB_APP) && !isPublic)
                    execParam = String.format("%s;jar uf %s %s/%s", execParam, moduleVo.getInstallPackage().getFileName(), absolutePath.replaceAll(String.format("^%s", deployPath), "").replaceAll("^/", ""), cfg.getFileName());
            }
        }
        execParam = execParam.replaceAll("^;", "").replaceAll("//", "/");
        if (isPublic) {
            if (container.getArgs() == null || container.getArgs().size() == 0) {
                container.setArgs(new ArrayList<>());
                execParam = execParam.replaceAll("^;", "").replaceAll("//", "/");
                container.getArgs().add(execParam);
            } else {
                execParam = String.format("%s;%s", execParam, container.getArgs().get(0));
                execParam = execParam.replaceAll("^;", "").replaceAll("//", "/");
                container.getArgs().set(0, execParam);
            }
        } else {
            if (container.getCommand() == null) {
                container.setCommand(new ArrayList<>());
                container.getCommand().add("/bin/sh");
                container.getCommand().add("-c");
                container.getCommand().add("");
            }
            execParam = String.format("%s;%s", container.getCommand().get(2), execParam);
            execParam = execParam.replaceAll("^;", "").replaceAll("//", "/");
            container.getCommand().set(2, execParam);
        }
        if (container.getArgs() != null && container.getArgs().size() > 0) {
            String[] arr = container.getArgs().get(0).split(";");
            List<String> strList = new ArrayList<>();
            for (String str : arr)
                if (StringUtils.isNotBlank(str) && str.indexOf("wget") < 0)
                    strList.add(str);
            container.getArgs().set(0, String.join(";", strList));
        }
    }

    /**
     * 将为容器挂载指定的配置文件信息
     * @param configName 添加的configMap的名字
     * @param cfgs       需要加载的配置文件
     * @param basePath   部署配置文件的basePath
     * @param deployPath 执行程序部署路径
     * @param configMap  需要被挂载的配置文件
     * @param moduleVo   被挂载配置文件的ccod域模块定义
     * @param mountPath  挂载路径
     * @param isPublic   被挂载的配置文件是否是公共配置
     * @param container  需要挂载configMap的容器
     * @param deployment 容器所属的deployment
     */
    public void addModuleCfgToContainer(String configName, List<AppFileNexusInfo> cfgs, String basePath, String deployPath, V1ConfigMap configMap, AppModuleVo moduleVo, String mountPath, boolean isPublic, V1Container container, V1Deployment deployment) throws ParamException {
        String volumeName = String.format("%s-volume", configName);
        if (!deployment.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity())).containsKey(volumeName)) {
            V1ConfigMapVolumeSource source = new V1ConfigMapVolumeSource();
            source.setItems(new ArrayList<>());
            source.setName(configName);
            for (String fileName : configMap.getData().keySet()) {
                V1KeyToPath item = new V1KeyToPath();
                item.setKey(fileName);
                item.setPath(fileName);
                source.getItems().add(item);
            }
            V1Volume volume = new V1Volume();
            volume.setName(volumeName);
            volume.setConfigMap(source);
            deployment.getSpec().getTemplate().getSpec().getVolumes().add(volume);
        }
        V1VolumeMount mount = new V1VolumeMount();
        mount.setName(volumeName);
        AppType appType = moduleVo.getAppType();
        mount.setMountPath(mountPath);
        container.getVolumeMounts().add(mount);
        String execParam = "";
        Map<String, List<AppFileNexusInfo>> deployPathCfgMap = cfgs.stream().collect(Collectors.groupingBy(AppFileNexusInfo::getDeployPath));
        if (appType.equals(AppType.BINARY_FILE) && !isPublic)
            execParam = String.format("%s;mkdir %s -p;mkdir %s/log -p;mv /opt/%s %s/%s", execParam, deployPath, basePath, moduleVo.getInstallPackage().getFileName(), deployPath, moduleVo.getInstallPackage().getFileName());
        else if (appType.equals(AppType.RESIN_WEB_APP) && !isPublic)
            execParam = String.format("%s;cd %s", execParam, deployPath);
        for (String cfgDeployPath : deployPathCfgMap.keySet()) {
            String absolutePath = getAbsolutePath(basePath, cfgDeployPath);
            execParam = String.format("%s;mkdir %s -p", execParam, absolutePath);
            for (AppFileNexusInfo cfg : deployPathCfgMap.get(cfgDeployPath)) {
                execParam = String.format("%s;cp %s/%s %s/%s", execParam, mountPath, cfg.getFileName(), absolutePath, cfg.getFileName());
                if (appType.equals(AppType.RESIN_WEB_APP) && !isPublic)
                    execParam = String.format("%s;jar uf %s %s/%s", execParam, moduleVo.getInstallPackage().getFileName(), absolutePath.replaceAll(String.format("^%s", deployPath), "").replaceAll("^/", ""), cfg.getFileName());
            }
        }
        execParam = execParam.replaceAll("^;", "").replaceAll("//", "/");
        if (isPublic) {
            if (container.getArgs() == null || container.getArgs().size() == 0) {
                container.setArgs(new ArrayList<>());
                execParam = execParam.replaceAll("^;", "").replaceAll("//", "/");
                container.getArgs().add(execParam);
            } else {
                execParam = String.format("%s;%s", execParam, container.getArgs().get(0));
                execParam = execParam.replaceAll("^;", "").replaceAll("//", "/");
                container.getArgs().set(0, execParam);
            }
        } else {
            if (container.getCommand() == null) {
                container.setCommand(new ArrayList<>());
                container.getCommand().add("/bin/sh");
                container.getCommand().add("-c");
                container.getCommand().add("");
            }
            execParam = String.format("%s;%s", container.getCommand().get(2), execParam);
            execParam = execParam.replaceAll("^;", "").replaceAll("//", "/");
            container.getCommand().set(2, execParam);
        }
        if (container.getArgs() != null && container.getArgs().size() > 0) {
            String[] arr = container.getArgs().get(0).split(";");
            List<String> strList = new ArrayList<>();
            for (String str : arr)
                if (StringUtils.isNotBlank(str) && str.indexOf("wget") < 0)
                    strList.add(str);
            container.getArgs().set(0, String.join(";", strList));
        }
    }


    private PlatformUpdateSchemaInfo generateDemoCreateSchema(String srcPlatformId, String dstPlatformId) throws Exception {

        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String jobId = DigestUtils.md5DigestAsHex(sf.format(now).getBytes()).substring(0, 10);
        String jsonStr = "{\"schemaId\":\"e2b849bd-0ac4-4591-97f3-eee6e7084a58\",\"domainUpdatePlanList\":[{\"domainName\":\"公共组件01\",\"domainId\":\"public01\",\"bkSetName\":\"公共组件\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"LicenseServer\",\"appAlias\":\"licenseserver\",\"targetVersion\":\"5214\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"licenseserver\",\"cfgs\":[{\"fileName\":\"Config.ini\",\"ext\":\"ini\",\"fileSize\":0,\"md5\":\"6c513269c4e2bc10f4a6cf0eb05e5bfc\",\"deployPath\":\"./bin/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_licenseserver/Config.ini\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNlZGYyZTBkYjgyNWQ0OTRi\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"configserver\",\"appAlias\":\"configserver\",\"targetVersion\":\"aca2af60caa0fb9f4af57f37f869dafc90472525\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"configserver\",\"cfgs\":[{\"fileName\":\"ccs_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"1095494274dc98445b79ec1d32900a6f\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_configserver/ccs_config.cfg\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY5NWYyNTlkYWY3ZWEzZWNl\"},{\"fileName\":\"ccs_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"197075eb110327da19bfc2a31f24b302\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_configserver/ccs_logger.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ1NGYyYWEyOGE2ZGNhYjlh\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"glsServer\",\"appAlias\":\"glsserver\",\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"glsserver\",\"cfgs\":[{\"fileName\":\"gls_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"f23a83a2d871d59c89d12b0281e10e90\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_glsserver/gls_config.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmMmRiNjc0YzQ5YmE4Nzdj\"},{\"fileName\":\"gls_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_glsserver/gls_logger.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFhYTlmZWY0MTJkMDY2ZTM3\"}],\"addDelay\":30}],\"updateType\":\"ADD\",\"createTime\":\"Jun 2, 2020 9:32:59 AM\",\"updateTime\":\"Jun 2, 2020 9:32:59 AM\",\"executeTime\":\"Jun 2, 2020 9:32:59 AM\",\"comment\":\"clone from 公共组件01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"域服务01\",\"domainId\":\"cloud01\",\"bkSetName\":\"域服务\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"DDSServer\",\"appAlias\":\"dds\",\"targetVersion\":\"150:18722\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"dds\",\"cfgs\":[{\"fileName\":\"dds_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"7f783a4ea73510c73ac830f135f4c762\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dds/dds_logger.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5MDNhNjZiNDlmZDMxNzYx\"},{\"fileName\":\"dds_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"d89e98072e96a06efa41c69855f4a3cc\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dds/dds_config.cfg\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZjOTYzMzczYzhmZjFjMTRm\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"UCDServer\",\"appAlias\":\"ucds\",\"targetVersion\":\"deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"ucds\",\"cfgs\":[{\"fileName\":\"ucds_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"f4445f10c75c9ef2f6d4de739c634498\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/ucds_config.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxMzRlYzQyOWIwNzZlMDU0\"},{\"fileName\":\"ucds_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"ec57329ddcec302e0cc90bdbb8232a3c\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/ucds_logger.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFjYWUzYjQxZWU5NTMxOTg4\"},{\"fileName\":\"DRWRClient.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"8b901d87855de082318314d868664c03\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/DRWRClient.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ2NWYyYzE5NTAwNDY4YzQ1\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcs\",\"appAlias\":\"dcs\",\"targetVersion\":\"155:21974\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"dcs\",\"cfgs\":[{\"fileName\":\"dc_log4cpp.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"5784d6983f5e6722622b727d0987a15e\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dcs/dc_log4cpp.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE0ODU4YTJmMGM3M2FlNTE5\"},{\"fileName\":\"DCServer.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"ce208427723a0ebc0fff405fd7c382dc\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dcs/DCServer.cfg\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjYwNGZjN2U3YWFlN2YwYWYy\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"cmsserver\",\"appAlias\":\"cms1\",\"targetVersion\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"cms1\",\"startCmd\":\"./cmsserver --config.main=../etc/config.cms2\",\"cfgs\":[{\"fileName\":\"beijing.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"4074321f266b42fe7d7266b6fa9d7ca2\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/beijing.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5NmMwMTdkNWU4ZjE3NGRi\"},{\"fileName\":\"config.cms2\",\"ext\":\"cms2\",\"fileSize\":0,\"md5\":\"cf032451250db89948f775e4d7799e40\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/config.cms2\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZlYmViNGRkNzM3YjM5MzE5\"},{\"fileName\":\"cms_log4cpp.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"b16210d40a7ef123eef0296393df37b8\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/cms_log4cpp.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmMDEzYTFjMTFkNzBlNDkz\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"cmsserver\",\"appAlias\":\"cms2\",\"targetVersion\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"cms2\",\"startCmd\":\"./cmsserver --config.main=../etc/config.cms2\",\"cfgs\":[{\"fileName\":\"beijing.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"4074321f266b42fe7d7266b6fa9d7ca2\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/beijing.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM0OTk5N2I3NjRhZWIzNDg5\"},{\"fileName\":\"cms_log4cpp.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"b16210d40a7ef123eef0296393df37b8\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/cms_log4cpp.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQxN2NjMTNmZmJhYjRlMWZh\"},{\"fileName\":\"config.cms2\",\"ext\":\"cms2\",\"fileSize\":0,\"md5\":\"5f5e2e498e5705b84297b2721fdbb603\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/config.cms2\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFiODMyZmE4OTU2MmM3NmNh\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"ucxserver\",\"appAlias\":\"ucx\",\"targetVersion\":\"1fef2157ea07c483979b424c758192bd709e6c2a\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"ucx\",\"startCmd\":\"./ucxserver --config.main=../cfg/config.ucx\",\"cfgs\":[{\"fileName\":\"config.ucx\",\"ext\":\"ucx\",\"fileSize\":0,\"md5\":\"0c7c8b38115a9d0cabb2d1505f195821\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ucx/config.ucx\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZlNTVjMGQzNzkxMjA3MzYw\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"daengine\",\"appAlias\":\"daengine\",\"targetVersion\":\"179:20744\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"daengine\",\"cfgs\":[{\"fileName\":\"dae.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"431128629db6c93804b86cc1f9428a87\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE1ODQyOGRjYjc2YzRjODdj\"},{\"fileName\":\"dae_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"ac2fde58b18a5ab1ee66d911982a326c\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_logger.cfg\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3Mzc5Y2E3NzU2ZjczMDEy\"},{\"fileName\":\"dae_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"04544c8572c42b176d501461168dacf4\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_config.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxM2UzYmZmYzhmOGY2NWMw\"},{\"fileName\":\"dae_log4cpp.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"ece32d86439201eefa186fbe8ad6db06\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_log4cpp.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ2OWMwMGQ4YTFlZjRhZDQ4\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcproxy\",\"appAlias\":\"dcproxy\",\"targetVersion\":\"195:21857\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"dcproxy\",\"cfgs\":[{\"fileName\":\"dcp_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"087cb6d8e6263dc6f1e8079fac197983\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dcproxy/dcp_config.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM5YThlM2QxMjI2NDc3NzZh\"},{\"fileName\":\"dcp_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"8d3d4de160751677d6a568c9d661d7c0\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dcproxy/dcp_logger.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEyZmRkNzJmMWJjZDMwYWNj\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"StatSchedule\",\"appAlias\":\"ss\",\"targetVersion\":\"154:21104\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"ss\",\"cfgs\":[{\"fileName\":\"ss_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"825e14101d79c733b2ea8becb8ea4e3b\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ss/ss_config.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5YTgxMTU1YjhmNDMyZjU4\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"createTime\":\"Jun 2, 2020 9:32:59 AM\",\"updateTime\":\"Jun 2, 2020 9:32:59 AM\",\"executeTime\":\"Jun 2, 2020 9:32:59 AM\",\"comment\":\"clone from 域服务01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"管理门户01\",\"domainId\":\"manage01\",\"bkSetName\":\"管理门户\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"cas\",\"appAlias\":\"cas\",\"targetVersion\":\"10973\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"cas\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_cas/web.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE2MDA0MWExZjQ1ODc4Njhh\"},{\"fileName\":\"cas.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"6622e01a4df917d747e078e89c774a52\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_cas/cas.properties\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3N2E3NDBhYmM0NzU2NDg5\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"customWebservice\",\"appAlias\":\"customwebservice\",\"targetVersion\":\"19553\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"customwebservice\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"96e5bc553847dab185d32c260310bb77\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_customwebservice/web.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRiNGZjNzM0NjU4MDMyNTZl\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"24eebd53ad6d6d2585f8164d189b4592\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_customwebservice/config.properties\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM3ZDgwMWZlMjNlNjU1MWNk\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcms\",\"appAlias\":\"dcms\",\"targetVersion\":\"11110\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcms\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"52ba707ab07e7fcd50d3732268dd9b9d\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/web.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZiMDVmY2FhMjFmZmNhN2Iz\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"98a8781d1808c69448c9666642d7b8ed\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/config.properties\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM5NTA4MmY4ODU2NmJhZmJj\"},{\"fileName\":\"Param-Config.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"9a977ea04c6e936307bec2683cadd379\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/Param-Config.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFlODhlMDRiZWM1NTBkZTc0\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcmsRecord\",\"appAlias\":\"dcmsrecord\",\"targetVersion\":\"21763\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmsrecord\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"a4500823701a6b430a98b25eeee6fea3\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/web.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY4YjgyMjllYTBmNzcwYzI4\"},{\"fileName\":\"applicationContext.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"5a355d87e0574ffa7bc120f61d8bf61e\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/applicationContext.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEwODE2YjIwM2VjNmEzYjA0\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"830bf1a0205f407eba5f3a449b749cba\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/config.properties\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRlOGM3ZWI3YmIwMGI5ZDJk\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcmssg\",\"appAlias\":\"dcmssg\",\"targetVersion\":\"20070\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmssg\",\"cfgs\":[{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"e76da17fe273dc7f563a9c7c86183d20\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmssg/config.properties\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ0Y2ZlOWIxNTkzOWVhZTYz\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"52a87ceaeebd7b9bb290ee863abe98c9\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmssg/web.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMyYWJhYzhlZWNiYjdmNTAw\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcmsStatics\",\"appAlias\":\"dcmsstatics\",\"targetVersion\":\"20537\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmsstaticsreport2\",\"cfgs\":[{\"fileName\":\"applicationContext.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"2a7ef2d3a9fc97e8e59db7f21b7d4d45\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/applicationContext.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNkZmI4MWI4ODk5NjNkZDZk\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"7212df6a667e72ecb604b03fee20f639\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/web.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY0ZDc1YmFiZTlkMDUxYTRi\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"973ba4d65b93a47bb5ead294b9415e68\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/config.properties\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4ODkyNWYwNTA1ZTk1NWJi\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcmsStaticsReport\",\"appAlias\":\"dcmsstaticsreport\",\"targetVersion\":\"20528\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmsstaticsreport1\",\"cfgs\":[{\"fileName\":\"applicationContext.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"1e2f67f773110caf7a91a1113564ce4c\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/applicationContext.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3MzVkODMwZTVlMTFkZTRk\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"f02bb5a99546b80a3a82f55154be143d\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/config.properties\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEyYzVkMWFhMDExOTUwODQz\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"5a32768163ade7c4bce70270d79b6c66\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/web.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRlOGM5MTIwOTY2ZWRkMjZk\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcmsWebservice\",\"appAlias\":\"dcmswebservice\",\"targetVersion\":\"20503\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmswebservice\",\"cfgs\":[{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"9a9671156ab2454951b9561fbefeed42\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmswebservice/config.properties\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxYjBmNjYxYzFmNDljYTkx\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"d93dd1fe127f46a13f27d6f8d4a7def3\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmswebservice/web.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ3YzI3NWZhZTNlMjFkMmVm\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcmsx\",\"appAlias\":\"dcmsx\",\"targetVersion\":\"master_8efabf4\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmsx\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"5d6550ab653769a49006b4957f9a0a65\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsx/web.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3OWVmMzQyNGYzODg0ODBi\"},{\"fileName\":\"application.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"675edca3ccfa6443d8dfc9b34b1eee0b\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsx/application.properties\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4MmE0YzY0YjQxNDc1ODU4\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"safetyMonitor\",\"appAlias\":\"safetymonitor\",\"targetVersion\":\"20383\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"safetymonitor\",\"cfgs\":[{\"fileName\":\"applicationContext.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"4543cb1aba46640edc3e815750fd3a94\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/applicationContext.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY5ZDNkNTQ5NTk5Yjg0Mjc2\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"752b9c6cc870d294fa413d64c090e49e\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/config.properties\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmYjk0NDEwZTVlN2NkMjIw\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"4d952d3e6d356156dd461144416f4816\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/web.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ1MjYyN2JlYzdjMGI0ZmRl\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"createTime\":\"Jun 2, 2020 9:32:59 AM\",\"updateTime\":\"Jun 2, 2020 9:32:59 AM\",\"executeTime\":\"Jun 2, 2020 9:32:59 AM\",\"comment\":\"clone from 管理门户01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"运营门户01\",\"domainId\":\"ops01\",\"bkSetName\":\"运营门户\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"gls\",\"appAlias\":\"gls\",\"targetVersion\":\"10309\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"gls\",\"cfgs\":[{\"fileName\":\"Param-Config.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"1da62c81dacf6d7ee21fca3384f134c5\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/ops01_gls/Param-Config.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4M2U3MDJjNjViN2E5MjQw\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"createTime\":\"Jun 2, 2020 9:32:59 AM\",\"updateTime\":\"Jun 2, 2020 9:32:59 AM\",\"executeTime\":\"Jun 2, 2020 9:32:59 AM\",\"comment\":\"clone from 运营门户01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"}],\"platformId\":\"k8s-test\",\"platformName\":\"ccod开发测试平台\",\"bkBizId\":25,\"bkCloudId\":0,\"ccodVersion\":\"CCOD4.1\",\"taskType\":\"CREATE\",\"createTime\":\"Jun 2, 2020 9:32:59 AM\",\"updateTime\":\"Jun 2, 2020 9:32:59 AM\",\"title\":\"新建工具组平台(202005-test)计划\",\"comment\":\"create 工具组平台(202005-test) by clone 123456-wuph(ccod开发测试平台)\",\"k8sHostIp\":\"10.130.41.218\",\"glsDBType\":\"ORACLE\",\"glsDBUser\":\"ccod\",\"glsDBPwd\":\"ccod\",\"baseDataNexusRepository\":\"platform_base_data\",\"baseDataNexusPath\":\"ccod/4.1/baseVolume.zip\",\"publicConfig\":[{\"fileName\":\"local_datasource.xml\",\"fileSize\":0,\"md5\":\"112940181aeb983baa9d7fd2733f194f\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_datasource.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmNmE4MWY3MWI3MmJlY2Ji\"},{\"fileName\":\"local_jvm.xml\",\"fileSize\":0,\"md5\":\"d172a5321944aba5bc19c35d00950afc\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_jvm.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRjMWQyMTIwNWRmYmY1MDM0\"},{\"fileName\":\"tnsnames.ora\",\"fileSize\":0,\"md5\":\"811f7f9472d5f6e733d732619a17ac77\",\"deployPath\":\"/usr/local/lib\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/tnsnames.ora\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY0NzFkOTU3MGMyODRjMGJm\"}],\"k8sApiUrl\":\"https://10.130.41.218:6443\",\"k8sAuthToken\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA\"}";
        PlatformUpdateSchemaInfo schemaInfo = gson.fromJson(jsonStr, PlatformUpdateSchemaInfo.class);
        schemaInfo.setThreePartServices(new ArrayList<>());
        schemaInfo.setPlatformId(dstPlatformId);
        if (schemaInfo.getPlatformId().equals(srcPlatformId))
            throw new Exception(String.format("created platformId equal src platformId %s", srcPlatformId));
        String createdPlatformId = schemaInfo.getPlatformId();
        for (DomainUpdatePlanInfo planInfo : schemaInfo.getDomainUpdatePlanList()) {
            planInfo.setDeployments(new ArrayList<>());
            planInfo.setServices(new ArrayList<>());
            planInfo.setIngresses(new ArrayList<>());
            planInfo.setStatus(UpdateStatus.WAIT_EXEC);
            for (AppUpdateOperationInfo optInfo : planInfo.getAppUpdateOperationList()) {
                AppModuleVo moduleVo = this.appManagerService.queryAppByVersion(optInfo.getAppName(), optInfo.getTargetVersion());
                if (AppType.RESIN_WEB_APP.equals(moduleVo.getAppType())) {
                    optInfo.setDeployPath("/opt/webapps");
                    optInfo.setBasePath("/opt");
                    if(StringUtils.isBlank(optInfo.getStartCmd()))
                        optInfo.setStartCmd("keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh;tail -F /usr/local/tomcat/logs/catalina.out");
                    for (AppFileNexusInfo cfg : optInfo.getCfgs())
                        cfg.setDeployPath(cfg.getDeployPath().replaceAll("^.*WEB-INF/", "./WEB-INF/"));
                } else {
                    optInfo.setBasePath("/root/Platform");
                    optInfo.setDeployPath("./bin");
                    if(StringUtils.isBlank(optInfo.getStartCmd()))
                        optInfo.setStartCmd(String.format("./%s", moduleVo.getInstallPackage().getFileName()));
                }
            }
        }
        PlatformPo platformPo = getK8sPlatform(srcPlatformId);
        schemaInfo.setK8sApiUrl(platformPo.getApiUrl());
        schemaInfo.setK8sAuthToken(platformPo.getAuthToken());
        logger.info(String.format("lastSchema=%s", gson.toJson(schemaInfo)));
//        createConfigMapForNewPlatform(schemaInfo);
        List<AppModuleVo> registerApps = this.appManagerService.queryAllRegisterAppModule(true);
        String platformId = platformPo.getPlatformId();
        String k8sApiUrl = platformPo.getApiUrl();
        String k8sAuthToken = platformPo.getAuthToken();
        List<V1Deployment> deployments = this.k8sApiService.listNamespacedDeployment(platformId, k8sApiUrl, k8sAuthToken);
        jsonStr = this.templateParseGson.toJson(deployments);
        deployments = this.templateParseGson.fromJson(jsonStr, new TypeToken<List<V1Deployment>>() {
        }.getType());
        List<V1ConfigMap> configMaps = this.k8sApiService.listNamespacedConfigMap(platformId, k8sApiUrl, k8sAuthToken);
        Map<String, V1ConfigMap> configMapMap = new HashMap<>();
        for (V1ConfigMap configMap : configMaps)
            configMapMap.put(configMap.getMetadata().getName(), configMap);
        V1ConfigMap configMap = configMapMap.get(srcPlatformId);
        configMap.getMetadata().setNamespace(createdPlatformId);
        configMap.getMetadata().setName(createdPlatformId);
        configMapMap.put(createdPlatformId, configMap);
        configMapMap.remove(srcPlatformId);
        Map<String, List<AppFileNexusInfo>> cfgMap = new HashMap<>();
        if (schemaInfo.getPublicConfig() != null && schemaInfo.getPublicConfig().size() > 0)
            cfgMap.put(platformId, schemaInfo.getPublicConfig());
        cfgMap.put(createdPlatformId, cfgMap.get(srcPlatformId));
        cfgMap.remove(srcPlatformId);
        for (DomainUpdatePlanInfo planInfo : schemaInfo.getDomainUpdatePlanList()) {
            String domainId = planInfo.getDomainId();
            if (planInfo.getPublicConfig() != null && planInfo.getPublicConfig().size() > 0)
                cfgMap.put(domainId, planInfo.getPublicConfig());
            for (AppUpdateOperationInfo optInfo : planInfo.getAppUpdateOperationList()) {
                String alias = optInfo.getAppAlias();
                cfgMap.put(String.format("%s-%s", alias, domainId), optInfo.getCfgs());
            }
        }
        Map<String, V1Deployment> deploymentMap = new HashMap<>();
        Map<String, Map<String, AppModuleVo>> domainAppMap = new HashMap<>();
        for (V1Deployment deployment : deployments) {
            String deploymentName = deployment.getMetadata().getName();
            deployment.getMetadata().setNamespace(createdPlatformId);
            V1Container runtimeContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
            V1Container initContainer = deployment.getSpec().getTemplate().getSpec().getInitContainers() != null ? deployment.getSpec().getTemplate().getSpec().getInitContainers().get(0) : null;
            if (initContainer != null) {
                String domainId = deploymentName.split("\\-")[1];
                deployment.getMetadata().getLabels().clear();
                deployment.getMetadata().getLabels().put(this.domainIdLabel, domainId);
                deployment.getMetadata().getLabels().put("type", K8sDeploymentType.CCOD_DOMAIN_APP.name);
                String alias = deploymentName.split("\\-")[0];
                initContainer.setName(alias);
                List<AppUpdateOperationInfo> optList = schemaInfo.getDomainUpdatePlanList().stream().collect(Collectors.toMap(DomainUpdatePlanInfo::getDomainId, Function.identity())).get(domainId).getAppUpdateOperationList();
                optList.stream().collect(Collectors.toMap(AppUpdateOperationInfo::getAppAlias, Function.identity())).get(alias).setAssembleTag(deploymentName);
                AppModuleVo moduleVo = getAppModuleFromImageTag(initContainer.getImage(), registerApps);
                Map<String, String> labels = new HashMap<>();
                labels.put(this.domainIdLabel, domainId);
                labels.put(moduleVo.getAppName(), alias);
                labels.put(String.format("%s-version", moduleVo.getAppName()), moduleVo.getVersion().replaceAll("\\:", "-"));
                deployment.getSpec().getSelector().setMatchLabels(labels);
                deployment.getSpec().getTemplate().getMetadata().setLabels(labels);
                if (!domainAppMap.containsKey(domainId))
                    domainAppMap.put(domainId, new HashMap<>());
                domainAppMap.get(domainId).put(alias, moduleVo);
                if (deployment.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity())).containsKey("data"))
                    deployment.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity())).get("data").getPersistentVolumeClaim().setClaimName(String.format("base-volume-%s", dstPlatformId));
            } else {
                deployment.getMetadata().getLabels().put("type", "ThreePartApp");
                deployment.getMetadata().setNamespace(createdPlatformId);
                for (V1Volume volume : deployment.getSpec().getTemplate().getSpec().getVolumes()) {
                    if (volume.getName().equals("sql"))
                        volume.getPersistentVolumeClaim().claimName("base-volume-just-test");
                }
            }
            deploymentMap.put(deployment.getMetadata().getName(), deployment);
        }
        schemaInfo.setK8sDeploymentList(new ArrayList<>());
        List<V1Deployment> threeSvcDpList = new ArrayList<>();
        List<V1Deployment> moduleDpList = new ArrayList<>();
        for (V1Deployment deployment : deployments) {
            String deployName = deployment.getMetadata().getName();
            if(deployment.equals("oracle"))
//            if(!"gls-ops01".equals(deployment.getMetadata().getName()))
//                continue;
            if (deployment.getMetadata().getLabels().get("type").equals(K8sDeploymentType.CCOD_DOMAIN_APP.name)) {
                String domainId = deployment.getMetadata().getLabels().get("domain-id");
//                generateConfigForCCODDomainModuleDeployment(jobId, deployment, configMapMap, cfgMap, schemaInfo.getDomainUpdatePlanList().stream().collect(Collectors.toMap(DomainUpdatePlanInfo::getDomainId, Function.identity())).get(domainId).getAppUpdateOperationList(), registerApps);
                String alias = deployment.getSpec().getTemplate().getSpec().getInitContainers().get(0).getName();
                deployment.getSpec().getTemplate().getSpec().getContainers().get(0).setName(String.format("%s-runtime", alias));
                moduleDpList.add(deployment);
            } else if (deployment.getMetadata().getName().equals("mysql-5-7-29") || deployment.getMetadata().getName().equals("oracle")) {
                threeSvcDpList.add(deployment);
            }
            deployment.getMetadata().getLabels().put(this.jobIdLabel, jobId);
            deployment.getMetadata().getLabels().put("tag", deployment.getMetadata().getName());
        }
        schemaInfo.getK8sDeploymentList().addAll(threeSvcDpList);
        schemaInfo.getK8sDeploymentList().addAll(moduleDpList);
        List<V1Service> services = this.k8sApiService.listNamespacedService(srcPlatformId, k8sApiUrl, k8sAuthToken);
        services = this.templateParseGson.fromJson(this.templateParseGson.toJson(services), new TypeToken<List<V1Service>>() {
        }.getType());
        schemaInfo.setK8sServiceList(new ArrayList<>());
        Map<String, V1Service> serviceMap = new HashMap<>();
        for (V1Service service : services) {
            if (service.getMetadata().getLabels() == null)
                service.getMetadata().setLabels(new HashMap<>());
            serviceMap.put(service.getMetadata().getName(), service);
            service.getMetadata().setNamespace(createdPlatformId);
            String[] arr = service.getMetadata().getName().split("\\-");
            String serviceName = service.getMetadata().getName();
            if (arr.length == 1) {
                if (serviceName.equals("oracle")) {
                    service.getMetadata().getLabels().put(this.serviceTypeLabel, K8sServiceType.THREE_PART_APP.name);
                    service.getMetadata().getLabels().put(this.appNameLabel, serviceName);
                    service.getMetadata().getLabels().put(this.appAliasLabel, "oracle");
                    service.getMetadata().getLabels().put(this.appVersionLabel, "10.1.0");
                } else if (serviceName.equals("mysql")) {
                    service.getMetadata().getLabels().put(this.serviceTypeLabel, K8sServiceType.THREE_PART_APP.name);
                    service.getMetadata().getLabels().put(this.appNameLabel, serviceName);
                    service.getMetadata().getLabels().put(this.appAliasLabel, "mysql");
                    service.getMetadata().getLabels().put(this.appVersionLabel, "5.7.29");
                } else {
                    service.getMetadata().getLabels().put(this.serviceTypeLabel, K8sServiceType.THREE_PART_SERVICE.name);
                    service.getMetadata().getLabels().put("service", "umg");
                }
                continue;
            }
            String alias = arr[0];
            String domainId = arr[1];
            AppModuleVo moduleVo = domainAppMap.get(domainId).get(alias);
            String appName = moduleVo.getAppName();
            service.getMetadata().getLabels().put(this.domainIdLabel, domainId);
            service.getMetadata().getLabels().put(this.appNameLabel, appName);
            service.getMetadata().getLabels().put(this.appAliasLabel, alias);
            service.getMetadata().getLabels().put(this.appVersionLabel, moduleVo.getVersion().replaceAll("\\:", "-"));
            if (arr.length == 2)
                service.getMetadata().getLabels().put(this.serviceTypeLabel, K8sServiceType.DOMAIN_SERVICE.name);
            else
                service.getMetadata().getLabels().put(this.serviceTypeLabel, K8sServiceType.DOMAIN_OUT_SERVICE.name);
            Map<String, String> selector = new HashMap<>();
            selector.put(this.domainIdLabel, domainId);
            selector.put(appName, alias);
            selector.put(String.format("%s-version", appName), moduleVo.getVersion().replaceAll("\\:", "-"));
            service.getSpec().setSelector(selector);
        }
        schemaInfo.getK8sServiceList().addAll(services);
        schemaInfo.setK8sEndpointsList(new ArrayList<>());
        List<V1Endpoints> endList = this.k8sApiService.listNamespacedEndpoints(srcPlatformId, k8sApiUrl, k8sAuthToken);
        endList = this.templateParseGson.fromJson(this.templateParseGson.toJson(endList), new TypeToken<List<V1Endpoints>>() {
        }.getType());
        for (V1Endpoints ends : endList) {
            ends.getMetadata().setNamespace(createdPlatformId);
            schemaInfo.getK8sEndpointsList().add(ends);
        }
        Map<String, ExtensionsV1beta1Ingress> ingressMap = new HashMap<>();
        schemaInfo.setK8sIngressList(new ArrayList<>());
        List<ExtensionsV1beta1Ingress> ingressList = this.k8sApiService.listNamespacedIngress(srcPlatformId, k8sApiUrl, k8sAuthToken);
        ingressList = this.templateParseGson.fromJson(this.templateParseGson.toJson(ingressList), new TypeToken<List<ExtensionsV1beta1Ingress>>() {
        }.getType());
        for (ExtensionsV1beta1Ingress ingress : ingressList) {
            ingress.getMetadata().setNamespace(createdPlatformId);
            schemaInfo.getK8sIngressList().add(ingress);
            ingressMap.put(ingress.getMetadata().getName(), ingress);
        }
        schemaInfo.setThreePartApps(new ArrayList<>());
        K8sCollection oracle = new K8sCollection(deploymentMap.get("oracle"), 150);
        oracle.getServices().add(serviceMap.get("oracle"));
        oracle.setTag("oracle");
        oracle.setAppName("oracle");
        oracle.setAlias("oracle");
        oracle.setVersion("11.0");
        oracle.setAppType(AppType.THREE_PART_APP);
//        schemaInfo.setOracle(oracle);
        schemaInfo.getThreePartApps().add(oracle);
        oracle.getDeployment().getMetadata().setNamespace(dstPlatformId);
        oracle.getDeployment().getSpec().getTemplate().getSpec().getVolumes().stream()
                .collect(Collectors.toMap(V1Volume::getName, Function.identity())).get("sql").getPersistentVolumeClaim()
                .setClaimName(String.format("base-volume-%s", dstPlatformId));
        for(V1Service service : oracle.getServices())
            service.getMetadata().setNamespace(dstPlatformId);
        deploymentMap.remove("oracle");
        serviceMap.remove("oracle");

        K8sCollection mysql = new K8sCollection(deploymentMap.get("mysql-5-7-29"));
        mysql.getServices().add(serviceMap.get("mysql"));
        mysql.setAppName("mysql");
        mysql.setAppName("mysql");
        mysql.setVersion("5-7-29");
        mysql.setAppType(AppType.THREE_PART_APP);
//        schemaInfo.setMysql(mysql);
        mysql.getDeployment().getMetadata().setNamespace(dstPlatformId);
        mysql.getDeployment().getSpec().getTemplate().getSpec().getVolumes().stream()
                .collect(Collectors.toMap(V1Volume::getName, Function.identity())).get("sql").getPersistentVolumeClaim()
                .setClaimName(String.format("base-volume-%s", dstPlatformId));
        for(V1Service service : mysql.getServices())
            service.getMetadata().setNamespace(dstPlatformId);
        schemaInfo.getThreePartApps().add(mysql);
        deploymentMap.remove("mysql-5-7-29");
        serviceMap.remove("mysql");

//        K8sCollection license = new K8sCollection(deploymentMap.get("licenseserver-public01"));
//        license.getServices().add(serviceMap.get("licenseserver-public01"));
//        schemaInfo.setLicenseServer(license);
//        deploymentMap.remove("licenseserver-public01");
//        serviceMap.remove("licenseserver-public01");
//
//        K8sCollection glsserver = new K8sCollection(deploymentMap.get("glsserver-public01"));
//        glsserver.getServices().add(serviceMap.get("glsserver-public01"));
//        deploymentMap.remove("glsserver-public01");
//        serviceMap.remove("glsserver-public01");
//        schemaInfo.setGlsserver(glsserver);
//
//        K8sCollection ucds = new K8sCollection(deploymentMap.get("ucds-cloud01"));
//        ucds.getServices().add(serviceMap.get("ucds-cloud01"));
//        ucds.getServices().add(serviceMap.get("ucds-cloud01-out"));
//        deploymentMap.remove("ucds-cloud01");
//        serviceMap.remove("ucds-cloud01");
//        serviceMap.remove("ucds-cloud01-out");
//        schemaInfo.setUcds(ucds);
//
//        K8sCollection dcs = new K8sCollection(deploymentMap.get("dcs-cloud01"));
//        dcs.getServices().add(serviceMap.get("dcs-cloud01"));
//        deploymentMap.remove("dcs-cloud01");
//        serviceMap.remove("dcs-cloud01");
//        schemaInfo.setDcs(dcs);
//        schemaInfo.setK8sDeploymentList(new ArrayList<>(deploymentMap.values()));
//        schemaInfo.setK8sServiceList(new ArrayList<>(serviceMap.values()));

        String pvJsonStr = "[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolume\",\"metadata\":{\"name\":\"base-volume-just-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"capacity\":{\"storage\":\"1Gi\"},\"claimRef\":{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"name\":\"base-volume-just-test\",\"namespace\":\"just-test\"},\"nfs\":{\"path\":\"/home/kubernetes/volume/just-test/baseVolume\",\"server\":\"10.130.41.218\"},\"persistentVolumeReclaimPolicy\":\"Retain\",\"storageClassName\":\"base-volume-just-test\",\"volumeMode\":\"Filesystem\"}}]";
        List<V1PersistentVolume> pvList = gson.fromJson(pvJsonStr, new TypeToken<List<V1PersistentVolume>>() {
        }.getType());
        for(V1PersistentVolume pv : pvList) {
            String pvName = String.format("base-volume-%s", dstPlatformId);
            pv.getMetadata().setName(pvName);
            pv.getSpec().getClaimRef().setName(pvName);
            pv.getSpec().getClaimRef().setName(pvName);
            pv.getSpec().getClaimRef().setNamespace(dstPlatformId);
            pv.getSpec().setStorageClassName(pvName);
            pv.getSpec().getNfs().setPath(String.format("/home/kubernetes/volume/%s/baseVolume", dstPlatformId));
        }
        schemaInfo.setK8sPVList(pvList);
        String pvcJsonStr = "[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"metadata\":{\"name\":\"base-volume-just-test\",\"namespace\":\"just-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"resources\":{\"requests\":{\"storage\":\"1Gi\"}},\"storageClassName\":\"base-volume-just-test\",\"volumeMode\":\"Filesystem\",\"volumeName\":\"base-volume-just-test\"}}]";
        List<V1PersistentVolumeClaim> pvcList = gson.fromJson(pvcJsonStr, new TypeToken<List<V1PersistentVolumeClaim>>() {
        }.getType());
        for(V1PersistentVolumeClaim pvc : pvcList)
        {
            String pvcName = String.format("base-volume-%s", dstPlatformId);
            pvc.getMetadata().setNamespace(dstPlatformId);
            pvc.getMetadata().setName(pvcName);
            pvc.getSpec().setStorageClassName(pvcName);
            pvc.getSpec().setVolumeName(pvcName);
        }
        schemaInfo.setK8sPVCList(pvcList);
        for(String name : deploymentMap.keySet())
        {
            String domainId = name.split("\\-")[1];
            schemaInfo.getDomainUpdatePlanList().stream().collect(Collectors.toMap(DomainUpdatePlanInfo::getDomainId, Function.identity())).get(domainId).getDeployments().add(deploymentMap.get(name));
        }
        for(String name : serviceMap.keySet())
        {
            if(name.split("\\-").length == 1)
                schemaInfo.getThreePartServices().add(serviceMap.get(name));
            else
            {
                String domainId = name.split("\\-")[1];
                schemaInfo.getDomainUpdatePlanList().stream().collect(Collectors.toMap(DomainUpdatePlanInfo::getDomainId, Function.identity())).get(domainId).getServices().add(serviceMap.get(name));
            }
        }
        for(String name : ingressMap.keySet())
        {
            String domainId = name.split("\\-")[1];
            schemaInfo.getDomainUpdatePlanList().stream().collect(Collectors.toMap(DomainUpdatePlanInfo::getDomainId, Function.identity())).get(domainId).getIngresses().add(ingressMap.get(name));
        }
//        schemaInfo.setK8sServiceList(gson.fromJson(gson.toJson(schemaInfo.getK8sServiceList()), new TypeToken<List<V1Service>>(){}.getType()));
//        schemaInfo.setK8sEndpointsList(gson.fromJson(gson.toJson(schemaInfo.getK8sEndpointsList()), new TypeToken<List<V1Endpoints>>(){}.getType()));
//        schemaInfo.setK8sIngressList(gson.fromJson(gson.toJson(schemaInfo.getK8sIngressList()), new TypeToken<List<ExtensionsV1beta1Ingress>>(){}.getType()));
//        generateConfigForCCODDomainModuleDeployment(deployments, configMapMap, allOptList, registerApps);
        schemaInfo.setSchemaId(jobId);
        schemaInfo.setPlatformFunc(PlatformFunction.TEST);
        V1Namespace ns = this.k8sApiService.readNamespace(srcPlatformId, k8sApiUrl, k8sAuthToken);
        ns.getMetadata().setNamespace(dstPlatformId);
        ns.getMetadata().setName(dstPlatformId);
        ns = this.templateParseGson.fromJson(this.templateParseGson.toJson(ns), V1Namespace.class);
        schemaInfo.setNamespace(ns);
        V1Secret ssl = this.k8sApiService.readNamespacedSecret("ssl", platformId, k8sApiUrl, k8sAuthToken);
        ssl.getMetadata().setNamespace(dstPlatformId);
        ssl = this.templateParseGson.fromJson(this.templateParseGson.toJson(ssl), V1Secret.class);
        schemaInfo.setK8sSecrets(new ArrayList<>());
        schemaInfo.getK8sSecrets().add(ssl);
        schemaInfo.setK8sDeploymentList(null);
        schemaInfo.setK8sServiceList(null);
        schemaInfo.setK8sIngressList(null);
        schemaInfo.setK8sEndpointsList(null);
        schemaInfo.setBaseDataNexusRepository(null);
        schemaInfo.setBaseDataNexusPath(null);
        schemaInfo.setPlatformType(PlatformType.K8S_CONTAINER);
        schemaInfo.setStatus(UpdateStatus.WAIT_EXEC);
        return schemaInfo;
    }

    private void generateParamForCollection(String jobId, String platformId, String domainId,
                                      AppUpdateOperationInfo optInfo,
                                      List<AppFileNexusInfo> domainCfg, List<AppFileNexusInfo> platformCfg,
                                      K8sCollection k8sCollection) throws ParamException
    {
        AppModuleVo appModule = k8sCollection.getAppModule();
        String appName = appModule.getAppName();
        V1Deployment deployment = k8sCollection.getDeployment();
        String alias = optInfo.getAppAlias();
        deployment.getMetadata().getLabels().put(String.format("%s-alias", appName), alias);
        String version = appModule.getVersion().replaceAll("\\:", "-");
        deployment.getMetadata().getLabels().put(appName, alias);
        deployment.getMetadata().getLabels().put(String.format("%s-version", appName), version);
        deployment.getSpec().getSelector().getMatchLabels().put(appName, alias);
        deployment.getSpec().getTemplate().getMetadata().getLabels().put(appName, alias);
        V1Container initContainer = deployment.getSpec().getTemplate().getSpec().getInitContainers().stream()
                .collect(Collectors.toMap(V1Container::getName, Function.identity())).get(alias);
        V1Container runtimeContainer = deployment.getSpec().getTemplate().getSpec().getContainers().stream()
                .collect(Collectors.toMap(V1Container::getName, Function.identity())).get(String.format("%s-runtime", alias));
        AppType appType = appModule.getAppType();
        initContainer.setCommand(new ArrayList<>());
        initContainer.getCommand().add("/bin/sh");
        initContainer.getCommand().add("-c");
        initContainer.getCommand().add("");
        String mountPath = String.format("%s/%s-cfg", this.defaultCfgMountPath, alias);
        if (appType.equals(AppType.BINARY_FILE)) {
            runtimeContainer.setArgs(new ArrayList<>());
            if(!runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).containsKey("binary-file"))
                throw new ParamException(String.format("%s container of %s has not binary-file mount volume",
                        runtimeContainer.getName(), deployment.getMetadata().getName()));
            runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get("binary-file").setMountPath(optInfo.getBasePath());
            if(!runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).containsKey("ccod-runtime"))
                throw new ParamException(String.format("%s container of %s has not ccod-runtime mount volume",
                        runtimeContainer.getName(), deployment.getMetadata().getName()));
            runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get("ccod-runtime")
                    .setMountPath(String.format("%s/log", optInfo.getBasePath()).replaceAll("//", "/"));
        }
        else
        {
            runtimeContainer.setArgs(new ArrayList<>());
            runtimeContainer.setCommand(new ArrayList<>());
            runtimeContainer.getCommand().add("/bin/sh");
            runtimeContainer.getCommand().add("-c");
        }
        String basePath = appType.equals(AppType.BINARY_FILE) ? "/binary-file" : "/opt";
        String deployPath = getAbsolutePath(basePath, optInfo.getDeployPath());
        String startCmd = optInfo.getStartCmd();
        addModuleCfgToContainer(String.format("%s-%s", alias, domainId), optInfo.getCfgs(), basePath, deployPath, appModule, mountPath, false, initContainer, deployment);
        basePath = optInfo.getBasePath();
        deployPath = getAbsolutePath(basePath, optInfo.getDeployPath());
        if (domainCfg != null && domainCfg.size() > 0) {
            mountPath = String.format("%s/%s", this.defaultCfgMountPath, domainId);
            addModuleCfgToContainer(domainId, domainCfg, basePath, deployPath, appModule, mountPath, true, runtimeContainer, deployment);
        }
        if (platformCfg != null && platformCfg.size() > 0) {
            mountPath = String.format("%s/%s", this.defaultCfgMountPath, platformId);
            addModuleCfgToContainer(platformId, platformCfg, basePath, deployPath, appModule, mountPath, true, runtimeContainer, deployment);
        }
        if (appType.equals(AppType.BINARY_FILE)) {
            String args = String.format("cd %s;%s;", deployPath, startCmd);
            if (runtimeContainer.getArgs() == null) {
                runtimeContainer.setArgs(new ArrayList<>());
                runtimeContainer.getArgs().add(args);
            } else {
                args = String.format("%s;%s", runtimeContainer.getArgs().get(0), args);
                runtimeContainer.getArgs().set(0, args);
            }
            if ("ucxserver".equals(appName)) {
                String cmd = initContainer.getCommand().get(2);
                cmd = String.format("%s;mv /opt/FlowMap.full /binary-file/cfg", cmd, basePath);
                initContainer.getCommand().set(2, cmd);
            }
        } else if (appType.equals(AppType.RESIN_WEB_APP)) {
            initContainer.setArgs(new ArrayList<>());
            String cmd = initContainer.getCommand().get(2);
            cmd = String.format("%s;mv /opt/webapps/%s /war/%s-%s.war", cmd, appModule.getInstallPackage().getFileName(), alias, domainId);
            initContainer.getCommand().set(2, cmd);
            runtimeContainer.getArgs().set(0, String.format("%s;cd %s;%s", runtimeContainer.getArgs().get(0), basePath, startCmd));
        }
        V1HostPathVolumeSource host = new V1HostPathVolumeSource();
        host.setPath(String.format("/var/ccod-runtime/%s/%s", platformId, domainId));
        host.setType("DirectoryOrCreate");
        deployment.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity())).get("ccod-runtime").setHostPath(host);
        if (initContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).containsKey("ccod-runtime"))
            initContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get("ccod-runtime").setSubPath(alias);
        if (runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).containsKey("ccod-runtime"))
            runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get("ccod-runtime").setSubPath(alias);
        if(appType.equals(AppType.RESIN_WEB_APP)) {
            runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()))
                    .get("war").setMountPath(String.format("%s/webapps", optInfo.getBasePath()).replaceAll("//", "/"));
            runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()))
                    .get("ccod-runtime").setMountPath(String.format("%s/log", optInfo.getBasePath()).replaceAll("//", "/"));
        }
        else if(appType.equals(AppType.TOMCAT_WEB_APP)){
            runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()))
                    .get("war").setMountPath(String.format("%s/webapps", optInfo.getBasePath()).replaceAll("//", "/"));
            runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()))
                    .get("ccod-runtime").setMountPath(String.format("%s/logs", optInfo.getBasePath()).replaceAll("//", "/"));
        }
        for(V1Service service : k8sCollection.getServices())
        {
            Map<String, String> selector = new HashMap<>();
            selector.put(this.domainIdLabel, domainId);
            selector.put(appModule.getAppName(), alias);
            service.getSpec().setSelector(selector);
            service.getMetadata().getLabels().put(this.domainIdLabel, domainId);
            service.getMetadata().getLabels().put(this.appNameLabel, appModule.getAppName());
            service.getMetadata().getLabels().put(this.appAliasLabel, alias);
            service.getMetadata().getLabels().put(this.jobIdLabel, jobId);
        }
    }

    private Map<String, V1Service> getRelativeService(String domainId, String alias, List<V1Service> services)
    {
        Map<String, V1Service> serviceMap = new HashMap<>();
        String regex = String.format("^%s-%s($|(-[0-9a-z]+)+$)", alias, domainId);
        for(V1Service service : services)
        {
            if(service.getMetadata().getName().matches(regex))
                serviceMap.put(service.getMetadata().getName(), service);
        }
        return serviceMap;
    }

    private Map<String, ExtensionsV1beta1Ingress> getRelativeIngress(String domainId, String alias, List<ExtensionsV1beta1Ingress> ingresses)
    {
        Map<String, ExtensionsV1beta1Ingress> ingressMap = new HashMap<>();
        String regex = String.format("^%s-%s($|(-[0-9a-z]+)+$)", alias, domainId);
        for(ExtensionsV1beta1Ingress ingress : ingresses)
        {
            if(ingress.getMetadata().getName().matches(regex))
                ingressMap.put(ingress.getMetadata().getName(), ingress);
        }
        return ingressMap;
    }

    private void proprocessDeployApp(String jobId, String platformId, String domainId, AppUpdateOperationInfo optInfo,
                                     List<AppFileNexusInfo> platformPublicConfig, List<AppFileNexusInfo> domainPublicConfig,
                                     AppModuleVo appModule, V1Deployment deployment, List<V1Service> services)
            throws ParamException
    {
        String alias = optInfo.getAppAlias();
        String appName = optInfo.getAppName();
        String version = optInfo.getTargetVersion().replaceAll("\\:", "-");
        deployment.getMetadata().setNamespace(platformId);
        deployment.getMetadata().getLabels().put(this.jobIdLabel, jobId);
        deployment.getMetadata().getLabels().put(this.domainIdLabel, domainId);
        deployment.getMetadata().getLabels().put("type", K8sDeploymentType.CCOD_DOMAIN_APP.name);
        deployment.getMetadata().getLabels().put(String.format("%s-alias", appName), alias);
        deployment.getMetadata().getLabels().put(String.format("%s-version", appName), version);
        if(deployment.getSpec().getTemplate().getMetadata().getLabels() == null)
            deployment.getSpec().getTemplate().getMetadata().setLabels(new HashMap<>());
        Map<String, String> labels = deployment.getSpec().getTemplate().getMetadata().getLabels();
        labels.put(this.jobIdLabel, jobId);
        labels.put(this.domainIdLabel, domainId);
        labels.put(appName, alias);
        labels.put(String.format("%s-version", appName), version);
        deployment.getSpec().getSelector().setMatchLabels(labels);
        V1Container initContainer = deployment.getSpec().getTemplate().getSpec().getInitContainers().stream()
                .collect(Collectors.toMap(V1Container::getName, Function.identity())).get(alias);
        V1Container runtimeContainer = deployment.getSpec().getTemplate().getSpec().getContainers().stream()
                .collect(Collectors.toMap(V1Container::getName, Function.identity())).get(String.format("%s-runtime", alias));
        AppType appType = appModule.getAppType();
        initContainer.setCommand(new ArrayList<>());
        initContainer.getCommand().add("/bin/sh");
        initContainer.getCommand().add("-c");
        initContainer.getCommand().add("");
        String mountPath = String.format("%s/%s-cfg", this.defaultCfgMountPath, alias);
        if (appType.equals(AppType.BINARY_FILE)) {
            runtimeContainer.setArgs(new ArrayList<>());
            runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get("binary-file").setMountPath(optInfo.getBasePath());
        }
        String basePath = appType.equals(AppType.BINARY_FILE) ? "/binary-file" : optInfo.getBasePath();
        String deployPath = getAbsolutePath(basePath, optInfo.getDeployPath());
        addModuleCfgToContainer(String.format("%s-%s", alias, domainId), optInfo.getCfgs(), basePath, deployPath, appModule, mountPath, false, initContainer, deployment);
        deployPath = getAbsolutePath(optInfo.getBasePath(), optInfo.getDeployPath());
        if (domainPublicConfig != null && domainPublicConfig.size() > 0) {
            mountPath = String.format("%s/%s-%s", this.defaultCfgMountPath, domainId, jobId);
            addModuleCfgToContainer(String.format("%s-%s", domainId, jobId), domainPublicConfig, optInfo.getBasePath(),
                    deployPath, appModule, mountPath, true, runtimeContainer, deployment);
        }
        if (platformPublicConfig != null && platformPublicConfig.size() > 0) {
            mountPath = String.format("%s/%s-%s", this.defaultCfgMountPath, platformId, jobId);
            addModuleCfgToContainer(String.format("%s-%s", platformId, jobId), domainPublicConfig, optInfo.getBasePath(),
                    deployPath, appModule, mountPath, true, runtimeContainer, deployment);
        }
        if (appType.equals(AppType.BINARY_FILE)) {
            String startCmd = StringUtils.isNotBlank(optInfo.getStartCmd()) ? optInfo.getStartCmd() : String.format("./%s", appModule.getInstallPackage().getFileName());
            String args = String.format("cd %s;%s;sleep 5;tailf /root/Platform/log/*/*.log;", deployPath, startCmd);
            if (runtimeContainer.getArgs() == null) {
                runtimeContainer.setArgs(new ArrayList<>());
                runtimeContainer.getArgs().add(args);
            } else {
                args = String.format("%s;%s", runtimeContainer.getArgs().get(0), args);
                runtimeContainer.getArgs().set(0, args);
            }
            if ("ucxserver".equals(optInfo.getAppName())) {
                String cmd = initContainer.getCommand().get(2);
                cmd = String.format("%s;mv /opt/FlowMap.full /binary-file/cfg", cmd, basePath);
                initContainer.getCommand().set(2, cmd);
            }
        } else if (appType.equals(AppType.RESIN_WEB_APP)) {
            initContainer.setArgs(new ArrayList<>());
            String cmd = initContainer.getCommand().get(2);
            cmd = String.format("%s;mv /opt/%s /war/%s-%s.war", cmd, appModule.getInstallPackage().getFileName(), alias, domainId);
            initContainer.getCommand().set(2, cmd);
            if (runtimeContainer.getCommand() != null && runtimeContainer.getCommand().size() > 2) {
                cmd = runtimeContainer.getCommand().get(2);
                cmd = String.format("%s;%s", runtimeContainer.getArgs().get(0), cmd).replaceAll("^;", "").replaceAll("//", "/");
                runtimeContainer.getArgs().set(0, cmd);
                runtimeContainer.setCommand(new ArrayList<>());
                runtimeContainer.getCommand().add("/bin/sh");
                runtimeContainer.getCommand().add("-c");
            }
        }
        V1HostPathVolumeSource host = new V1HostPathVolumeSource();
        host.setPath(String.format("/var/ccod-runtime/%s/%s", platformId, domainId));
        deployment.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity())).get("ccod-runtime").setHostPath(host);
        if (initContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).containsKey("ccod-runtime"))
            initContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get("ccod-runtime").setSubPath(alias);
        if (runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).containsKey("ccod-runtime"))
            runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get("ccod-runtime").setSubPath(alias);
        for(V1Service service : services)
        {
            Map<String, String> selector = new HashMap<>();
            selector.put(this.domainIdLabel, domainId);
            selector.put(appName, alias);
            selector.put(this.jobIdLabel, jobId);
            service.getSpec().setSelector(selector);
            service.getMetadata().setLabels(selector);
        }
    }

    /**
     * 为定义ccod域模块的deployment挂载所有配置文件
     *
     * @param schemaId     本次平台创建/升级的唯一id
     * @param deployment   用来定义ccod域模块的deployment
     * @param configMapMap 以键值对保存的所有configMap信息
     * @param cfgMap       平台所有配置
     * @param optList      deployment归属域下的所有模块操作信息
     * @param registerApps 已经向cmdb注册过的所有ccod模块信息
     * @throws ParamException
     * @throws NotSupportAppException
     */
    private void generateConfigForCCODDomainModuleDeployment(String schemaId, V1Deployment deployment, Map<String, V1ConfigMap> configMapMap, Map<String, List<AppFileNexusInfo>> cfgMap, List<AppUpdateOperationInfo> optList, List<AppModuleVo> registerApps) throws ParamException, NotSupportAppException {
        String platformId = deployment.getMetadata().getNamespace();
        String domainId = deployment.getMetadata().getLabels().get("domain-id");
        V1Container initContainer = deployment.getSpec().getTemplate().getSpec().getInitContainers().get(0);
        V1Container container = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        AppType appType = getAppTypeFromImageUrl(initContainer.getImage());
        initContainer.setCommand(new ArrayList<>());
        initContainer.getCommand().add("/bin/sh");
        initContainer.getCommand().add("-c");
        initContainer.getCommand().add("");
        String alias = initContainer.getName();
        AppModuleVo moduleVo = getAppModuleFromImageTag(initContainer.getImage(), registerApps);
        V1ConfigMap configMap = configMapMap.get(String.format("%s-%s", alias, domainId));
        AppUpdateOperationInfo optInfo = optList.stream().collect(Collectors.toMap(AppUpdateOperationInfo::getAppAlias, Function.identity())).get(alias);
        String mountPath = String.format("%s/%s-cfg", this.defaultCfgMountPath, alias);
        if (appType.equals(AppType.BINARY_FILE)) {
            container.setArgs(new ArrayList<>());
            container.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get("binary-file").setMountPath(optInfo.getBasePath());
        }
        String basePath = appType.equals(AppType.BINARY_FILE) ? "/binary-file" : "/opt";
        String deployPath = getAbsolutePath(basePath, optInfo.getDeployPath());
        addModuleCfgToContainer(String.format("%s-%s", alias, domainId), cfgMap.get(String.format("%s-%s", alias, domainId)), basePath, deployPath, configMap, moduleVo, mountPath, false, initContainer, deployment);
        basePath = optInfo.getBasePath();
        deployPath = getAbsolutePath(optInfo.getBasePath(), optInfo.getDeployPath());
        if (configMapMap.containsKey(domainId)) {
            mountPath = String.format("%s/%s", this.defaultCfgMountPath, domainId);
            addModuleCfgToContainer(domainId, cfgMap.get(domainId), optInfo.getBasePath(), deployPath, configMapMap.get(domainId), moduleVo, mountPath, true, container, deployment);
        }
        if (configMapMap.containsKey(platformId)) {
            mountPath = String.format("%s/%s", this.defaultCfgMountPath, platformId);
            addModuleCfgToContainer(platformId, cfgMap.get(platformId), optInfo.getBasePath(), deployPath, configMapMap.get(platformId), moduleVo, mountPath, true, container, deployment);
        }
        if (appType.equals(AppType.BINARY_FILE)) {
            String startCmd = StringUtils.isNotBlank(optInfo.getStartCmd()) ? optInfo.getStartCmd() : String.format("./%s", moduleVo.getInstallPackage().getFileName());
            String args = String.format("cd %s;%s;sleep 5;tailf /root/Platform/log/*/*.log;", deployPath, startCmd);
            if (container.getArgs() == null) {
                container.setArgs(new ArrayList<>());
                container.getArgs().add(args);
            } else {
                args = String.format("%s;%s", container.getArgs().get(0), args);
                container.getArgs().set(0, args);
            }
            if ("ucx".equals(alias)) {
                String cmd = initContainer.getCommand().get(2);
                cmd = String.format("%s;mv /opt/FlowMap.full /binary-file/cfg", cmd, basePath);
                initContainer.getCommand().set(2, cmd);
            }
        } else if (appType.equals(AppType.RESIN_WEB_APP)) {
            initContainer.setArgs(new ArrayList<>());
            String cmd = initContainer.getCommand().get(2);
            cmd = String.format("%s;mv /opt/%s /war/%s-%s.war", cmd, moduleVo.getInstallPackage().getFileName(), alias, domainId);
            initContainer.getCommand().set(2, cmd);
            if (container.getCommand() != null && container.getCommand().size() > 2) {
                cmd = container.getCommand().get(2);
                cmd = String.format("%s;%s", container.getArgs().get(0), cmd).replaceAll("^;", "").replaceAll("//", "/");
                container.getArgs().set(0, cmd);
                container.setCommand(new ArrayList<>());
                container.getCommand().add("/bin/sh");
                container.getCommand().add("-c");
            }
        }
        container.setName(String.format("%s-runtime", alias));
        V1HostPathVolumeSource host = new V1HostPathVolumeSource();
        host.setPath(String.format("/var/ccod-runtime/%s/%s-%s/%s-%s", platformId, alias, domainId, alias, domainId));
        deployment.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity())).get("ccod-runtime").setHostPath(host);
        if(appType.equals(AppType.RESIN_WEB_APP)) {
            container.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()))
                    .get("war").setMountPath(String.format("%s/webapps", optInfo.getBasePath()).replaceAll("//", "/"));
            container.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()))
                    .get("ccod-runtime").setMountPath(String.format("%s/log", optInfo.getBasePath()).replaceAll("//", "/"));
        }
        else if(appType.equals(AppType.TOMCAT_WEB_APP)){
            container.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()))
                    .get("war").setMountPath(String.format("%s/webapps", optInfo.getBasePath()).replaceAll("//", "/"));
            container.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()))
                    .get("ccod-runtime").setMountPath(String.format("%s/logs", optInfo.getBasePath()).replaceAll("//", "/"));
        }
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
        for(AppUpdateOperationInfo info : planInfo.getAppUpdateOperationList())
        {
            if(!info.getOperation().equals(AppUpdateOperation.DELETE) && !targetMap.containsKey(info.getAppAlias()))
                notRelativeApps.add(info.getAppAlias());
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
            if(!existMap.containsKey(deleted.getAppAlias()))
                throw new ParamException(String.format("not find exist k8s definition for DELETE %s", deleted.getAppAlias()));
            deleteCollections.add(existMap.get(deleted.getAppAlias()));
        }
        List<AppUpdateOperationInfo> addList = optMap.containsKey(AppUpdateOperation.ADD) ? optMap.get(AppUpdateOperation.ADD) : new ArrayList<>();
        Collections.sort(addList, sort);
        List<K8sCollection> addCollections = new ArrayList<>();
        for(AppUpdateOperationInfo optInfo : addList)
        {
            if(!targetMap.containsKey(optInfo.getAppAlias()))
                throw new ParamException(String.format("not find target k8s definition for ADD %s", optInfo.getAppAlias()));
            addCollections.add(targetMap.get(optInfo.getAppAlias()));
        }
        List<AppUpdateOperationInfo> updateList = optMap.containsKey(AppUpdateOperation.UPDATE) ? optMap.get(AppUpdateOperation.UPDATE) : new ArrayList<>();
        Collections.sort(updateList, sort);
        for(AppUpdateOperationInfo optInfo : updateList) {
            if(!existMap.containsKey(optInfo.getAppAlias()))
                throw new ParamException(String.format("not find exist k8s definition for UPDATE %s", optInfo.getAppAlias()));
            deleteCollections.add(existMap.get(optInfo.getAppAlias()));
            if(!targetMap.containsKey(optInfo.getAppAlias()))
                throw new ParamException(String.format("not find target k8s definition for UPDATE %s", optInfo.getAppAlias()));
            addCollections.add(targetMap.get(optInfo.getAppAlias()));
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
                if(!deleteList.stream().collect(Collectors.toMap(AppUpdateOperationInfo::getAppAlias, Function.identity())).containsKey(collection.getAlias()))
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
            List<AppFileNexusInfo> cfgFiles = planInfo.getAppUpdateOperationList().stream().collect(Collectors.toMap(AppUpdateOperationInfo::getAppAlias, Function.identity())).get(alias).getCfgs();
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
        for(V1Deployment deployment : addMap.keySet())
        {
            if(!deployment.getMetadata().getNamespace().equals(platformId))
                throw new ParamException(String.format("error namespace of add deployment %s:wanted %s,actual %s",
                        deployment.getMetadata().getName(), platformId, deployment.getMetadata().getNamespace()));
            List<K8sCollection> collections = addMap.get(deployment);
            if(deployment.getMetadata().getLabels() == null)
                deployment.getMetadata().setLabels(new HashMap<>());
            deployment.getMetadata().getLabels().put(this.deploymentTypeLabel, K8sDeploymentType.CCOD_DOMAIN_APP.name);
            deployment.getMetadata().getLabels().put(this.domainIdLabel, domainId);
            deployment.getMetadata().getLabels().put(this.jobIdLabel, jobId);
            deployment.getSpec().getSelector().getMatchLabels().put(this.domainIdLabel, domainId);
            deployment.getSpec().getTemplate().getMetadata().getLabels().put(this.domainIdLabel, domainId);
            K8sOperationInfo k8sOpt = new K8sOperationInfo(jobId, platformId, planInfo.getDomainId(), K8sKind.DEPLOYMENT,
                    deployment.getMetadata().getName(), K8sOperation.CREATE, deployment);
            k8sOptList.add(k8sOpt);
            for(K8sCollection collection : collections) {
                if(!addList.stream().collect(Collectors.toMap(AppUpdateOperationInfo::getAppAlias, Function.identity())).containsKey(collection.getAlias()))
                    throw new ParamException(String.format("%s not in %s ADD list", collection.getAlias(), domainId));
                AppUpdateOperationInfo optInfo = addList.stream().collect(Collectors.toMap(AppUpdateOperationInfo::getAppAlias, Function.identity())).get(collection.getAlias());
                generateParamForCollection(jobId, platformId, domainId, optInfo, domainCfg, platformCfg, collection);
                deployment.getMetadata().getLabels().put(String.format("%s-alias", optInfo.getAppName()), optInfo.getAppAlias());
                deployment.getMetadata().getLabels().put(String.format("%s-version", optInfo.getAppName()), optInfo.getTargetVersion().replaceAll("\\:", "-"));
                for(V1Service service : collection.getServices())
                {
                    if(!service.getMetadata().getNamespace().equals(platformId))
                        throw new ParamException(String.format("error namespace of add service %s:wanted %s,actual %s",
                                service.getMetadata().getName(), platformId, service.getMetadata().getNamespace()));
                    service.getMetadata().setNamespace(platformId);
                    Map<String, String> selector = new HashMap<>();
                    selector.put(collection.getAppName(), collection.getAlias());
                    selector.put(this.domainIdLabel, domainId);
                    service.getSpec().setSelector(selector);
                    if(service.getMetadata().getLabels() == null)
                        service.getMetadata().setLabels(new HashMap<>());
                    service.getMetadata().getLabels().put(this.domainIdLabel, domainId);
                    service.getMetadata().getLabels().put(this.appNameLabel, collection.getAppName());
                    service.getMetadata().getLabels().put(this.appAliasLabel, collection.getAlias());
                    service.getMetadata().getLabels().put(this.appVersionLabel, collection.getVersion().replaceAll("\\:", "-"));
                    service.getMetadata().getLabels().put(this.appTypeLabel, collection.getAppType().name);
                    service.getMetadata().getLabels().put(this.jobIdLabel, jobId);
                    if(service.getMetadata().getName().split("\\-").length > 2)
                        service.getMetadata().getLabels().put(this.serviceTypeLabel, K8sServiceType.DOMAIN_OUT_SERVICE.name);
                    else
                        service.getMetadata().getLabels().put(this.serviceTypeLabel, K8sServiceType.DOMAIN_SERVICE.name);
                    k8sOpt = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE,
                            service.getMetadata().getName(), K8sOperation.CREATE, service);
                    k8sOptList.add(k8sOpt);
                }
                for(ExtensionsV1beta1Ingress ingress : collection.getIngresses())
                {
                    if(!ingress.getMetadata().getNamespace().equals(platformId))
                        throw new ParamException(String.format("error namespace of add ingress %s:wanted %s,actual %s",
                                ingress.getMetadata().getName(), platformId, ingress.getMetadata().getNamespace()));
                    k8sOpt = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.INGRESS,
                            ingress.getMetadata().getName(), K8sOperation.CREATE, ingress);
                    k8sOptList.add(k8sOpt);
                }
            }
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
                            break;
                        case DELETE:
                            this.k8sApiService.deleteNamespacedIngress(optInfo.getName(), platformId, k8sApiUrl, k8sAuthToken);
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

    private PlatformSchemaExecResultVo execPlatformUpdateSteps(List<K8sOperationInfo> k8sOptList, PlatformUpdateSchemaInfo schema, List<PlatformAppDeployDetailVo> platformApps) throws ApiException, ParamException, SQLException, ClassNotFoundException, K8sDataException
    {
        Date startTime = new Date();
        String platformId = schema.getPlatformId();
        String k8sApiUrl = schema.getK8sApiUrl();
        String k8sAuthToken = schema.getK8sAuthToken();
        V1Deployment glsserver = null;
        int oraclePort = 0;
        List<K8sOperationPo> execResults = new ArrayList<>();
        List<PlatformUpdateRecordPo> lastRecords = this.platformUpdateRecordMapper.select(platformId, true);
        PlatformSchemaExecResultVo execResultVo = new PlatformSchemaExecResultVo(schema.getSchemaId(), platformId, k8sOptList);
        for(K8sOperationInfo k8sOpt : k8sOptList)
        {
            K8sOperationPo ret = execK8sOpt(k8sOpt, platformId, k8sApiUrl, k8sAuthToken);
            if(!ret.isSuccess())
            {
                logger.error(String.format("platform update schema exec fail : %s", ret.getComment()));
                execResults.add(ret);
                execResultVo.execFail(execResults, ret.getComment());
                return execResultVo;
            }
            try
            {
                if(k8sOpt.getKind().equals(K8sKind.SERVICE) && k8sOpt.getOperation().equals(K8sOperation.CREATE))
                {
                    V1Service service = (V1Service)gson.fromJson(ret.getRetJson(), V1Service.class);
                    Map<String, String> labels = service.getMetadata().getLabels();
                    if(labels == null || labels.size() == 0 || !labels.containsKey(this.serviceTypeLabel) || !labels.containsKey(this.appNameLabel))
                        continue;
                    if(labels.get(this.serviceTypeLabel).equals(K8sServiceType.THREE_PART_APP.name)
                            && labels.get(this.appNameLabel).equals("oracle"))
                    {
                        oraclePort = getNodePortFromK8sService(service);
                        boolean isConn = oracleConnectTest(schema.getGlsDBUser(), schema.getGlsDBPwd(), schema.getK8sHostIp(), oraclePort, "xe", 240);
                        if(!isConn)
                            throw new ApiException("create service for oracle fail");
                    }
                    else if(labels.get(this.serviceTypeLabel).equals(K8sServiceType.DOMAIN_OUT_SERVICE.name) && labels.get(this.appNameLabel).equals("UCDServer"))
                    {
                        if(oraclePort == 0)
                        {
                            V1Service oracleService = k8sApiService.readNamespacedService("oracle", platformId, k8sApiUrl, k8sAuthToken);
                            oraclePort = getNodePortFromK8sService(oracleService);
                        }
                        Connection connect = createOracleConnection(schema.getGlsDBUser(), schema.getGlsDBPwd(), schema.getK8sHostIp(), oraclePort, "xe");
                        int ucdsPort = getNodePortFromK8sService(service);
                        String updateSql = String.format("update \"CCOD\".\"GLS_SERVICE_UNIT\" set PARAM_UCDS_PORT=%d where NAME='ucds-cloud01'", ucdsPort);
                        PreparedStatement ps = connect.prepareStatement(updateSql);
                        logger.debug(String.format("begin to update ucds port : %s", updateSql));
                        ps.executeUpdate();
                        if(glsserver == null) {
                            glsserver = this.k8sApiService.readNamespacedDeployment("glsserver-public01", platformId, k8sApiUrl, k8sAuthToken);
                            glsserver = templateParseGson.fromJson(templateParseGson.toJson(glsserver), V1Deployment.class);
                        }
                        Date now = new Date();
                        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
                        glsserver.getMetadata().getLabels().put("restart-time", sf.format(now));
                        this.k8sApiService.replaceNamespacedDeployment(glsserver.getMetadata().getName(), platformId, glsserver, k8sApiUrl, k8sAuthToken);
                    }
                }
                else if(k8sOpt.getKind().equals(K8sKind.DEPLOYMENT) && k8sOpt.getOperation().equals(K8sOperation.CREATE))
                {
                    V1Deployment deployment = gson.fromJson(ret.getRetJson(), V1Deployment.class);
                    Map<String, String> labels = deployment.getMetadata().getLabels();
                    if(labels.containsKey(this.deploymentTypeLabel) && labels.get(this.deploymentTypeLabel).equals(K8sDeploymentType.CCOD_DOMAIN_APP.name) && labels.containsKey("glsServer"))
                        glsserver = (V1Deployment) k8sOpt.getObj();
                }
            }
            catch (Exception ex)
            {
                logger.error(String.format("exec %s fail", gson.toJson(k8sOpt)), ex);
                ret.fail(ex.getMessage());
                execResults.add(ret);
                execResultVo.execFail(execResults, ex.getMessage());
                return execResultVo;
            }
            execResults.add(ret);
        }
        logger.info(String.format("%s schema with jobId=%s execute success", platformId, schema.getSchemaId()));
        execResultVo.execResult(execResults);
        for(K8sOperationPo stepResult : execResultVo.getExecResults())
        {
            this.k8sOperationMapper.insert(stepResult);
        }
        String lastJobId = lastRecords.size() == 1 ? lastRecords.get(0).getJobId() : null;
        PlatformUpdateRecordPo recordPo = new PlatformUpdateRecordPo();
        recordPo.setResult(execResultVo.isSuccess());
        recordPo.setLast(true);
        recordPo.setJobId(schema.getSchemaId());
        recordPo.setExecSchema(gson.toJson(schema).getBytes());
        if(!execResultVo.isSuccess())
            recordPo.setComment(execResultVo.getErrorMsg());
        recordPo.setPreUpdateJobId(lastJobId);
        recordPo.setPlatformId(platformId);
        recordPo.setJobId(schema.getSchemaId());
        recordPo.setPreDeployApps(gson.toJson(platformApps).getBytes());
        recordPo.setUpdateTime(startTime);
        recordPo.setTimeUsage((int)((new Date()).getTime() - startTime.getTime())/1000);
        logger.error(String.format("record=%s", gson.toJson(recordPo)));
        this.platformUpdateRecordMapper.insert(recordPo);
        for(PlatformUpdateRecordPo po : lastRecords)
        {
            po.setLast(false);
            this.platformUpdateRecordMapper.update(po);
        }
        return execResultVo;
    }

    @Override
    public PlatformUpdateRecordVo rollbackPlatform(String platformId, List<String> domainIds) throws ParamException, ApiException {
        logger.debug(String.format("rollback domain %s of %s to previous status", platformId, String.join(",", domainIds)));
        PlatformPo platform = getK8sPlatform(platformId);
        List<V1Service> existServices = this.k8sApiService.listNamespacedService(platformId, platform.getApiUrl(), platform.getAuthToken());
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
            K8sOperationPo execResult = execK8sOpt(optInfo, platformId, platform.getApiUrl(), platform.getAuthToken());
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

    @Override
    public PlatformTopologyInfo createK8sPlatform(PlatformUpdateSchemaInfo createSchema) throws ParamException, InterfaceCallException, NexusException, IOException, ApiException, LJPaasException, NotSupportAppException, SQLException, ClassNotFoundException, K8sDataException {
        String k8sApiUrl = createSchema.getK8sApiUrl();
        String k8sAuthToken = createSchema.getK8sAuthToken();
        String platformId = createSchema.getPlatformId();
        String hostIp = createSchema.getK8sHostIp();
//        V1Namespace ns = this.k8sApiService.readNamespace(platformId, k8sApiUrl, k8sAuthToken);
//        if(ns != null)
//            this.k8sApiService.deleteNamespace(platformId, k8sApiUrl, k8sAuthToken);
        this.k8sApiService.createDefaultNamespace(platformId, k8sApiUrl, k8sAuthToken);
        this.k8sApiService.createNamespacedSSLCert(platformId, k8sApiUrl, k8sAuthToken);
        List<V1ConfigMap> configMapList = createConfigMapForNewPlatform(createSchema);
        Map<String, V1ConfigMap> configMapMap = new HashMap<>();
        for (V1ConfigMap configMap : configMapList)
            configMapMap.put(configMap.getMetadata().getName(), configMap);
        Map<String, List<AppFileNexusInfo>> cfgMap = new HashMap<>();
        if (createSchema.getPublicConfig() != null && createSchema.getPublicConfig().size() > 0)
            cfgMap.put(platformId, createSchema.getPublicConfig());
        for (DomainUpdatePlanInfo planInfo : createSchema.getDomainUpdatePlanList()) {
            String domainId = planInfo.getDomainId();
            if (planInfo.getPublicConfig() != null && planInfo.getPublicConfig().size() > 0)
                cfgMap.put(domainId, planInfo.getPublicConfig());
            for (AppUpdateOperationInfo optInfo : planInfo.getAppUpdateOperationList()) {
                String alias = optInfo.getAppAlias();
                cfgMap.put(String.format("%s-%s", alias, domainId), optInfo.getCfgs());
            }
        }
        for (V1PersistentVolume pv : createSchema.getK8sPVList())
            this.k8sApiService.createPersistentVolume(pv, k8sApiUrl, k8sAuthToken);
        for (V1PersistentVolumeClaim pvc : createSchema.getK8sPVCList())
            this.k8sApiService.createNamespacedPersistentVolumeClaim(platformId, pvc, k8sApiUrl, k8sAuthToken);
        deploySpecialApp(createSchema, createSchema.getOracle(), k8sApiUrl, k8sAuthToken);
        deploySpecialApp(createSchema, createSchema.getMysql(), k8sApiUrl, k8sAuthToken);
        deploySpecialApp(createSchema, createSchema.getLicenseServer(), k8sApiUrl, k8sAuthToken);
        deploySpecialApp(createSchema, createSchema.getGlsserver(), k8sApiUrl, k8sAuthToken);
        deploySpecialApp(createSchema, createSchema.getUcds(), k8sApiUrl, k8sAuthToken);
        V1Service ucdsOutSvc = this.k8sApiService.readNamespacedService(createSchema.getUcds().getServices().get(1).getMetadata().getName(), platformId, k8sApiUrl, k8sAuthToken);
        int ucdsPort = getNodePortFromK8sService(ucdsOutSvc);
        Connection connect = createOracleConnection(createSchema.getGlsDBUser(), createSchema.getGlsDBPwd(), createSchema.getK8sHostIp(), createSchema.getOracle().getNodePort(), "xe");
        String updateSql = String.format("update \"CCOD\".\"GLS_SERVICE_UNIT\" set PARAM_UCDS_PORT=%d where NAME='ucds-cloud01'", ucdsPort);
        PreparedStatement ps = connect.prepareStatement(updateSql);
        logger.debug(String.format("begin to update ucds port : %s", updateSql));
        ps.executeUpdate();
        this.k8sApiService.replaceNamespacedDeployment(createSchema.getGlsserver().getDeployment().getMetadata().getName(), platformId, createSchema.getGlsserver().getDeployment(), k8sApiUrl, k8sAuthToken);
        deploySpecialApp(createSchema, createSchema.getDcs(), k8sApiUrl, k8sAuthToken);
        for (V1Deployment deployment : createSchema.getK8sDeploymentList())
            this.k8sApiService.createNamespacedDeployment(platformId, deployment, k8sApiUrl, k8sAuthToken);
        for (V1Service service : createSchema.getK8sServiceList())
            this.k8sApiService.createNamespacedService(platformId, service, k8sApiUrl, k8sAuthToken);
        for (ExtensionsV1beta1Ingress ingress : createSchema.getK8sIngressList())
            this.k8sApiService.createNamespacedIngress(platformId, ingress, k8sApiUrl, k8sAuthToken);
        this.k8sApiService.replaceNamespacedDeployment(createSchema.getDcs().getDeployment().getMetadata().getName(), platformId, createSchema.getDcs().getDeployment(), k8sApiUrl, k8sAuthToken);
        return this.getPlatformTopologyFromK8s(createSchema.getPlatformName(), platformId, createSchema.getBkBizId(), createSchema.getBkCloudId(), createSchema.getCcodVersion(), hostIp, k8sApiUrl, k8sAuthToken, createSchema.getPlatformFunc());
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
        Map<String, PlatformAppDeployDetailVo> deployMap = deployApps.stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAppAlias, Function.identity()));
        for(AppUpdateOperationInfo src : updateList)
        {
            AppUpdateOperationInfo roll = new AppUpdateOperationInfo();
            roll.setAppName(src.getAppName());
            roll.setAppAlias(src.getAppAlias());
            roll.setOriginalAlias(src.getOriginalAlias());
            switch (src.getOperation())
            {
                case ADD:
                    roll.setOperation(AppUpdateOperation.DELETE);
                    break;
                case UPDATE:
                case DELETE:
                    if(!deployMap.containsKey(src.getAppAlias()))
                        throw new ParamException(String.format("%s has been %s at domain %s, but can not find src deploy detail", src.getAppAlias(), domainId));
                    PlatformAppDeployDetailVo srcDetail = deployMap.get(src.getAppAlias());
                    if(src.getOperation().equals(AppUpdateOperation.UPDATE)) {
                        roll.setOperation(AppUpdateOperation.UPDATE);
                        roll.setTargetVersion(srcDetail.getVersion());
                        roll.setOriginalVersion(src.getTargetVersion());
                    }
                    else {
                        roll.setOperation(AppUpdateOperation.ADD);
                        roll.setTargetVersion(srcDetail.getVersion());
                        roll.setOriginalVersion(null);
                    }
                    roll.setDeployPath(srcDetail.getDeployPath());
                    roll.setStartCmd(srcDetail.getStartCmd());
                    roll.setBasePath(srcDetail.getBasePath());
                    roll.setAssembleTag(srcDetail.getAssembleTag());
                    roll.setDomainId(domainId);
                    roll.setHostIp(srcDetail.getHostIp());
                    roll.setAppRunner(srcDetail.getAppAlias());
                    roll.setCfgs(getFromPlatformAppCfg(srcDetail.getCfgs()));
                    roll.setDomainName(srcDetail.getDomainName());
                    roll.setPort(srcDetail.getPort());
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

    List<AppFileNexusInfo> getFromPlatformAppCfg(List<PlatformAppCfgFilePo> cfgFiles)
    {
        List<AppFileNexusInfo> cfgs = new ArrayList<>();
        for (PlatformAppCfgFilePo cfg : cfgFiles) {
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
        return cfgs;
    }

    private void deploySpecialApp(PlatformUpdateSchemaInfo schemaInfo, K8sCollection specialApp, String k8sApiUrl, String k8sAuthToken) throws ApiException, SQLException {
        String platformId = schemaInfo.getPlatformId();
        this.k8sApiService.createNamespacedDeployment(platformId, specialApp.getDeployment(), k8sApiUrl, k8sAuthToken);
        for (V1Service service : specialApp.getServices())
            this.k8sApiService.createNamespacedService(platformId, service, k8sApiUrl, k8sAuthToken);
        if (StringUtils.isNotBlank(specialApp.getTag()) && specialApp.getTag().toLowerCase().equals("oracle")) {
            try {
                Thread.sleep(5000);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            V1Service service = this.k8sApiService.readNamespacedService(specialApp.getServices().get(0).getMetadata().getName(), platformId, k8sApiUrl, k8sAuthToken);
            int port = getNodePortFromK8sService(service);
            boolean connected = oracleConnectTest(schemaInfo.getGlsDBUser(), schemaInfo.getGlsDBPwd(), schemaInfo.getK8sHostIp(), port, "xe", specialApp.getTimeout());
            if (!connected)
                throw new ApiException("connect oralce fail");
            specialApp.setNodePort(port);
        }
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

    public K8sPlatformSchemaInfo getSchemaFromOldSchema(PlatformUpdateSchemaInfo oldSchema) {
        K8sPlatformSchemaInfo schema = new K8sPlatformSchemaInfo();
//        schema.setBkBizId(oldSchema.getBkBizId());
//        schema.setBkCloudId(oldSchema.getBkCloudId());
//        schema.setCcodVersion(oldSchema.getCcodVersion());
//        schema.setComment(oldSchema.getComment());
//        schema.setCreateMethod(PlatformCreateMethod.K8S_API);
//        schema.setDomainPlanList(new ArrayList<>());
//        schema.setGlsDBPwd(oldSchema.getGlsDBPwd());
//        schema.setGlsDBType(oldSchema.getGlsDBType());
//        schema.setGlsDBUser(oldSchema.getGlsDBUser());
//        schema.setK8sApiUrl(oldSchema.getK8sApiUrl());
//        schema.setK8sAuthToken(oldSchema.getK8sAuthToken());
//        schema.setK8sHostIp(oldSchema.getK8sHostIp());
//        schema.setPlatformFunc(oldSchema.getPlatformFunc());
//        schema.setPlatformId(oldSchema.getPlatformId());
//        schema.setPlatformName(oldSchema.getPlatformName());
//        schema.setPlatformType(PlatformType.K8S_CONTAINER);
//        schema.setPublicConfig(oldSchema.getPublicConfig());
//        schema.setSchemaId(oldSchema.getSchemaId());
//        schema.setStatus(oldSchema.getStatus());
//        schema.setTaskType(oldSchema.getTaskType());
//        schema.setThreeAppAssembleList(new ArrayList<>());
//        schema.setTitle(oldSchema.getTitle());
//        schema.setPvcList(oldSchema.getK8sPVCList());
//        schema.setPvList(oldSchema.getK8sPVList());
//        K8sAppAssembleInfo assembleInfo = new K8sAppAssembleInfo();
//        assembleInfo.setAppOptList(new ArrayList<>());
//        assembleInfo.setThreePartApp(true);
//        assembleInfo.setAssembleName("oracle");
//        assembleInfo.setAssembleTag("oracle");
//        assembleInfo.setDeployment(oldSchema.getOracle().getDeployment());
//        assembleInfo.setIngresses(oldSchema.getOracle().getIngresses());
//        assembleInfo.setServices(oldSchema.getOracle().getServices());
//        schema.getThreeAppAssembleList().add(assembleInfo);
//        assembleInfo = new K8sAppAssembleInfo();
//        assembleInfo.setAppOptList(new ArrayList<>());
//        assembleInfo.setThreePartApp(true);
//        assembleInfo.setAssembleName("mysql");
//        assembleInfo.setAssembleTag("mysql");
//        assembleInfo.setDeployment(oldSchema.getMysql().getDeployment());
//        assembleInfo.setIngresses(oldSchema.getMysql().getIngresses());
//        assembleInfo.setServices(oldSchema.getMysql().getServices());
//        schema.getThreeAppAssembleList().add(assembleInfo);
//        Map<String, V1Service> serviceMap = new HashMap<>();
//        for (V1Service service : oldSchema.getK8sServiceList())
//            serviceMap.put(service.getMetadata().getName(), service);
//        Map<String, ExtensionsV1beta1Ingress> ingressMap = new HashMap<>();
//        for (ExtensionsV1beta1Ingress ingress : oldSchema.getK8sIngressList())
//            ingressMap.put(ingress.getMetadata().getName(), ingress);
//        Map<String, V1Deployment> deploymentMap = new HashMap<>();
//        for (V1Deployment deployment : oldSchema.getK8sDeploymentList())
//            deploymentMap.put(deployment.getMetadata().getName(), deployment);
//        for (DomainUpdatePlanInfo planInfo : oldSchema.getDomainUpdatePlanList()) {
//            K8sDomainPlanInfo k8sPlan = new K8sDomainPlanInfo();
//            k8sPlan.setAppAssembleList(new ArrayList<>());
//            k8sPlan.setBkSetName(planInfo.getBkSetName());
//            k8sPlan.setComment(planInfo.getComment());
//            k8sPlan.setDomainId(planInfo.getDomainId());
//            k8sPlan.setDomainName(planInfo.getDomainName());
//            k8sPlan.setDomainType(DomainType.K8S_CONTAINER);
//            k8sPlan.setMaxOccurs(planInfo.getMaxOccurs());
//            k8sPlan.setOccurs(planInfo.getOccurs());
//            k8sPlan.setPublicConfig(new ArrayList<>());
//            k8sPlan.setStatus(planInfo.getStatus());
//            k8sPlan.setTags(planInfo.getTags());
//            k8sPlan.setUpdateType(planInfo.getUpdateType());
//            String domainId = planInfo.getDomainId();
//            for (AppUpdateOperationInfo optInfo : planInfo.getAppUpdateOperationList()) {
//                String tag = String.format("%s-%s", optInfo.getAppAlias(), domainId);
//                assembleInfo = new K8sAppAssembleInfo();
//                assembleInfo.setAssembleTag(tag);
//                assembleInfo.setThreePartApp(false);
//                if (optInfo.getAppAlias().equals("licenseserver")) {
//                    assembleInfo.setServices(oldSchema.getLicenseServer().getServices());
//                    assembleInfo.setIngresses(oldSchema.getLicenseServer().getIngresses());
//                    assembleInfo.setDeployment(oldSchema.getLicenseServer().getDeployment());
//                } else if (optInfo.getAppAlias().equals("glsserver")) {
//                    assembleInfo.setServices(oldSchema.getGlsserver().getServices());
//                    assembleInfo.setIngresses(oldSchema.getGlsserver().getIngresses());
//                    assembleInfo.setDeployment(oldSchema.getGlsserver().getDeployment());
//                } else if (optInfo.getAppAlias().equals("ucds")) {
//                    assembleInfo.setServices(oldSchema.getUcds().getServices());
//                    assembleInfo.setIngresses(oldSchema.getUcds().getIngresses());
//                    assembleInfo.setDeployment(oldSchema.getUcds().getDeployment());
//                } else if (optInfo.getAppAlias().equals("dcs")) {
//                    assembleInfo.setServices(oldSchema.getDcs().getServices());
//                    assembleInfo.setIngresses(oldSchema.getDcs().getIngresses());
//                    assembleInfo.setDeployment(oldSchema.getDcs().getDeployment());
//
//                } else {
//                    assembleInfo.setServices(Arrays.asList(serviceMap.get(tag)));
//                    if (ingressMap.containsKey(tag))
//                        assembleInfo.setIngresses(Arrays.asList(ingressMap.get(tag)));
//                    assembleInfo.setDeployment(deploymentMap.get(tag));
//                }
//                assembleInfo.setAppOptList(Arrays.asList(optInfo));
//            }
//        }
        return schema;
    }

    @Test
    public void pathTest() throws Exception {
//        String basePath = "/home/onlinemanager/apache-tomcat-7.0.91/webapps/onlinemanager/WEB-INF";
//        String relativePath = "./lib";
//        String abPath = getAbsolutePath(basePath, relativePath);
//        System.out.println(abPath);
//        relativePath = "lib";
//        abPath = getAbsolutePath(basePath, relativePath);
//        System.out.println(abPath);
//        relativePath = "/home/onlinemanager/apache-tomcat-7.0.91/webapps/onlinemanager/WEB-INF/lib";
//        abPath = getAbsolutePath(basePath, relativePath);
//        System.out.println(abPath);
//        relativePath = "../../manager";
//        abPath = getAbsolutePath(basePath, relativePath);
//        System.out.println(abPath);
//        String json = "{\"schemaId\":\"c95bf0a00a\",\"domainUpdatePlanList\":[{\"domainName\":\"公共组件01\",\"domainId\":\"public01\",\"bkSetName\":\"公共组件\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"licenseserver-public01\",\"appName\":\"LicenseServer\",\"appAlias\":\"licenseserver\",\"targetVersion\":\"5214\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"./bin\",\"appRunner\":\"licenseserver\",\"cfgs\":[{\"fileName\":\"Config.ini\",\"ext\":\"ini\",\"fileSize\":0,\"md5\":\"6c513269c4e2bc10f4a6cf0eb05e5bfc\",\"deployPath\":\"./bin/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_licenseserver/Config.ini\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNlZGYyZTBkYjgyNWQ0OTRi\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"configserver-public01\",\"appName\":\"configserver\",\"appAlias\":\"configserver\",\"targetVersion\":\"aca2af60caa0fb9f4af57f37f869dafc90472525\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"./bin\",\"appRunner\":\"configserver\",\"cfgs\":[{\"fileName\":\"ccs_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"1095494274dc98445b79ec1d32900a6f\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_configserver/ccs_config.cfg\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY5NWYyNTlkYWY3ZWEzZWNl\"},{\"fileName\":\"ccs_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"197075eb110327da19bfc2a31f24b302\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_configserver/ccs_logger.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ1NGYyYWEyOGE2ZGNhYjlh\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"glsserver-public01\",\"appName\":\"glsServer\",\"appAlias\":\"glsserver\",\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"./bin\",\"appRunner\":\"glsserver\",\"cfgs\":[{\"fileName\":\"gls_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"f23a83a2d871d59c89d12b0281e10e90\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_glsserver/gls_config.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmMmRiNjc0YzQ5YmE4Nzdj\"},{\"fileName\":\"gls_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_glsserver/gls_logger.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFhYTlmZWY0MTJkMDY2ZTM3\"}],\"addDelay\":30}],\"updateType\":\"ADD\",\"createTime\":\"Jun 2, 2020 9:32:59 AM\",\"updateTime\":\"Jun 2, 2020 9:32:59 AM\",\"executeTime\":\"Jun 2, 2020 9:32:59 AM\",\"comment\":\"clone from 公共组件01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"域服务01\",\"domainId\":\"cloud01\",\"bkSetName\":\"域服务\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"dds-cloud01\",\"appName\":\"DDSServer\",\"appAlias\":\"dds\",\"targetVersion\":\"150:18722\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"./bin\",\"appRunner\":\"dds\",\"cfgs\":[{\"fileName\":\"dds_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"7f783a4ea73510c73ac830f135f4c762\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dds/dds_logger.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5MDNhNjZiNDlmZDMxNzYx\"},{\"fileName\":\"dds_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"d89e98072e96a06efa41c69855f4a3cc\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dds/dds_config.cfg\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZjOTYzMzczYzhmZjFjMTRm\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"ucds-cloud01\",\"appName\":\"UCDServer\",\"appAlias\":\"ucds\",\"targetVersion\":\"deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"./bin\",\"appRunner\":\"ucds\",\"cfgs\":[{\"fileName\":\"ucds_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"f4445f10c75c9ef2f6d4de739c634498\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/ucds_config.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxMzRlYzQyOWIwNzZlMDU0\"},{\"fileName\":\"ucds_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"ec57329ddcec302e0cc90bdbb8232a3c\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/ucds_logger.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFjYWUzYjQxZWU5NTMxOTg4\"},{\"fileName\":\"DRWRClient.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"8b901d87855de082318314d868664c03\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/DRWRClient.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ2NWYyYzE5NTAwNDY4YzQ1\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"dcs-cloud01\",\"appName\":\"dcs\",\"appAlias\":\"dcs\",\"targetVersion\":\"155:21974\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"./bin\",\"appRunner\":\"dcs\",\"cfgs\":[{\"fileName\":\"dc_log4cpp.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"5784d6983f5e6722622b727d0987a15e\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dcs/dc_log4cpp.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE0ODU4YTJmMGM3M2FlNTE5\"},{\"fileName\":\"DCServer.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"ce208427723a0ebc0fff405fd7c382dc\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dcs/DCServer.cfg\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjYwNGZjN2U3YWFlN2YwYWYy\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"cms1-cloud01\",\"appName\":\"cmsserver\",\"appAlias\":\"cms1\",\"targetVersion\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"./bin\",\"appRunner\":\"cms1\",\"startCmd\":\"./cmsserver --config.main\\u003d../etc/config.cms2\",\"cfgs\":[{\"fileName\":\"beijing.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"4074321f266b42fe7d7266b6fa9d7ca2\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/beijing.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5NmMwMTdkNWU4ZjE3NGRi\"},{\"fileName\":\"config.cms2\",\"ext\":\"cms2\",\"fileSize\":0,\"md5\":\"cf032451250db89948f775e4d7799e40\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/config.cms2\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZlYmViNGRkNzM3YjM5MzE5\"},{\"fileName\":\"cms_log4cpp.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"b16210d40a7ef123eef0296393df37b8\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/cms_log4cpp.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmMDEzYTFjMTFkNzBlNDkz\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"cms2-cloud01\",\"appName\":\"cmsserver\",\"appAlias\":\"cms2\",\"targetVersion\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"./bin\",\"appRunner\":\"cms2\",\"startCmd\":\"./cmsserver --config.main\\u003d../etc/config.cms2\",\"cfgs\":[{\"fileName\":\"beijing.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"4074321f266b42fe7d7266b6fa9d7ca2\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/beijing.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM0OTk5N2I3NjRhZWIzNDg5\"},{\"fileName\":\"cms_log4cpp.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"b16210d40a7ef123eef0296393df37b8\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/cms_log4cpp.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQxN2NjMTNmZmJhYjRlMWZh\"},{\"fileName\":\"config.cms2\",\"ext\":\"cms2\",\"fileSize\":0,\"md5\":\"5f5e2e498e5705b84297b2721fdbb603\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/config.cms2\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFiODMyZmE4OTU2MmM3NmNh\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"ucx-cloud01\",\"appName\":\"ucxserver\",\"appAlias\":\"ucx\",\"targetVersion\":\"1fef2157ea07c483979b424c758192bd709e6c2a\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"./bin\",\"appRunner\":\"ucx\",\"startCmd\":\"./ucxserver --config.main\\u003d../cfg/config.ucx\",\"cfgs\":[{\"fileName\":\"config.ucx\",\"ext\":\"ucx\",\"fileSize\":0,\"md5\":\"0c7c8b38115a9d0cabb2d1505f195821\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ucx/config.ucx\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZlNTVjMGQzNzkxMjA3MzYw\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"daengine-cloud01\",\"appName\":\"daengine\",\"appAlias\":\"daengine\",\"targetVersion\":\"179:20744\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"./bin\",\"appRunner\":\"daengine\",\"cfgs\":[{\"fileName\":\"dae.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"431128629db6c93804b86cc1f9428a87\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE1ODQyOGRjYjc2YzRjODdj\"},{\"fileName\":\"dae_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"ac2fde58b18a5ab1ee66d911982a326c\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_logger.cfg\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3Mzc5Y2E3NzU2ZjczMDEy\"},{\"fileName\":\"dae_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"04544c8572c42b176d501461168dacf4\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_config.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxM2UzYmZmYzhmOGY2NWMw\"},{\"fileName\":\"dae_log4cpp.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"ece32d86439201eefa186fbe8ad6db06\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_log4cpp.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ2OWMwMGQ4YTFlZjRhZDQ4\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"dcproxy-cloud01\",\"appName\":\"dcproxy\",\"appAlias\":\"dcproxy\",\"targetVersion\":\"195:21857\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"./bin\",\"appRunner\":\"dcproxy\",\"cfgs\":[{\"fileName\":\"dcp_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"087cb6d8e6263dc6f1e8079fac197983\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dcproxy/dcp_config.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM5YThlM2QxMjI2NDc3NzZh\"},{\"fileName\":\"dcp_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"8d3d4de160751677d6a568c9d661d7c0\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dcproxy/dcp_logger.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEyZmRkNzJmMWJjZDMwYWNj\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"ss-cloud01\",\"appName\":\"StatSchedule\",\"appAlias\":\"ss\",\"targetVersion\":\"154:21104\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"./bin\",\"appRunner\":\"ss\",\"cfgs\":[{\"fileName\":\"ss_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"825e14101d79c733b2ea8becb8ea4e3b\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ss/ss_config.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5YTgxMTU1YjhmNDMyZjU4\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"createTime\":\"Jun 2, 2020 9:32:59 AM\",\"updateTime\":\"Jun 2, 2020 9:32:59 AM\",\"executeTime\":\"Jun 2, 2020 9:32:59 AM\",\"comment\":\"clone from 域服务01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"管理门户01\",\"domainId\":\"manage01\",\"bkSetName\":\"管理门户\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"cas-manage01\",\"appName\":\"cas\",\"appAlias\":\"cas\",\"targetVersion\":\"10973\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"cas\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_cas/web.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE2MDA0MWExZjQ1ODc4Njhh\"},{\"fileName\":\"cas.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"6622e01a4df917d747e078e89c774a52\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_cas/cas.properties\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3N2E3NDBhYmM0NzU2NDg5\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"customwebservice-manage01\",\"appName\":\"customWebservice\",\"appAlias\":\"customwebservice\",\"targetVersion\":\"19553\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"customwebservice\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"96e5bc553847dab185d32c260310bb77\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_customwebservice/web.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRiNGZjNzM0NjU4MDMyNTZl\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"24eebd53ad6d6d2585f8164d189b4592\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_customwebservice/config.properties\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM3ZDgwMWZlMjNlNjU1MWNk\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"dcms-manage01\",\"appName\":\"dcms\",\"appAlias\":\"dcms\",\"targetVersion\":\"11110\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcms\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"52ba707ab07e7fcd50d3732268dd9b9d\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/web.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZiMDVmY2FhMjFmZmNhN2Iz\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"98a8781d1808c69448c9666642d7b8ed\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/config.properties\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM5NTA4MmY4ODU2NmJhZmJj\"},{\"fileName\":\"Param-Config.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"9a977ea04c6e936307bec2683cadd379\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/Param-Config.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFlODhlMDRiZWM1NTBkZTc0\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"dcmsrecord-manage01\",\"appName\":\"dcmsRecord\",\"appAlias\":\"dcmsrecord\",\"targetVersion\":\"21763\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmsrecord\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"a4500823701a6b430a98b25eeee6fea3\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/web.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY4YjgyMjllYTBmNzcwYzI4\"},{\"fileName\":\"applicationContext.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"5a355d87e0574ffa7bc120f61d8bf61e\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/applicationContext.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEwODE2YjIwM2VjNmEzYjA0\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"830bf1a0205f407eba5f3a449b749cba\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/config.properties\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRlOGM3ZWI3YmIwMGI5ZDJk\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"dcmssg-manage01\",\"appName\":\"dcmssg\",\"appAlias\":\"dcmssg\",\"targetVersion\":\"20070\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmssg\",\"cfgs\":[{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"e76da17fe273dc7f563a9c7c86183d20\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmssg/config.properties\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ0Y2ZlOWIxNTkzOWVhZTYz\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"52a87ceaeebd7b9bb290ee863abe98c9\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmssg/web.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMyYWJhYzhlZWNiYjdmNTAw\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"dcmsstatics-manage01\",\"appName\":\"dcmsStatics\",\"appAlias\":\"dcmsstatics\",\"targetVersion\":\"20537\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmsstaticsreport2\",\"cfgs\":[{\"fileName\":\"applicationContext.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"2a7ef2d3a9fc97e8e59db7f21b7d4d45\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/applicationContext.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNkZmI4MWI4ODk5NjNkZDZk\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"7212df6a667e72ecb604b03fee20f639\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/web.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY0ZDc1YmFiZTlkMDUxYTRi\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"973ba4d65b93a47bb5ead294b9415e68\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/config.properties\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4ODkyNWYwNTA1ZTk1NWJi\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"dcmsstaticsreport-manage01\",\"appName\":\"dcmsStaticsReport\",\"appAlias\":\"dcmsstaticsreport\",\"targetVersion\":\"20528\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmsstaticsreport1\",\"cfgs\":[{\"fileName\":\"applicationContext.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"1e2f67f773110caf7a91a1113564ce4c\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/applicationContext.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3MzVkODMwZTVlMTFkZTRk\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"f02bb5a99546b80a3a82f55154be143d\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/config.properties\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEyYzVkMWFhMDExOTUwODQz\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"5a32768163ade7c4bce70270d79b6c66\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/web.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRlOGM5MTIwOTY2ZWRkMjZk\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"dcmswebservice-manage01\",\"appName\":\"dcmsWebservice\",\"appAlias\":\"dcmswebservice\",\"targetVersion\":\"20503\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmswebservice\",\"cfgs\":[{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"9a9671156ab2454951b9561fbefeed42\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmswebservice/config.properties\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxYjBmNjYxYzFmNDljYTkx\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"d93dd1fe127f46a13f27d6f8d4a7def3\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmswebservice/web.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ3YzI3NWZhZTNlMjFkMmVm\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"dcmsx-manage01\",\"appName\":\"dcmsx\",\"appAlias\":\"dcmsx\",\"targetVersion\":\"master_8efabf4\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmsx\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"5d6550ab653769a49006b4957f9a0a65\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsx/web.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3OWVmMzQyNGYzODg0ODBi\"},{\"fileName\":\"application.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"675edca3ccfa6443d8dfc9b34b1eee0b\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsx/application.properties\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4MmE0YzY0YjQxNDc1ODU4\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"safetymonitor-manage01\",\"appName\":\"safetyMonitor\",\"appAlias\":\"safetymonitor\",\"targetVersion\":\"20383\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"safetymonitor\",\"cfgs\":[{\"fileName\":\"applicationContext.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"4543cb1aba46640edc3e815750fd3a94\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/applicationContext.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY5ZDNkNTQ5NTk5Yjg0Mjc2\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"752b9c6cc870d294fa413d64c090e49e\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/config.properties\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmYjk0NDEwZTVlN2NkMjIw\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"4d952d3e6d356156dd461144416f4816\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/web.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ1MjYyN2JlYzdjMGI0ZmRl\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"createTime\":\"Jun 2, 2020 9:32:59 AM\",\"updateTime\":\"Jun 2, 2020 9:32:59 AM\",\"executeTime\":\"Jun 2, 2020 9:32:59 AM\",\"comment\":\"clone from 管理门户01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"运营门户01\",\"domainId\":\"ops01\",\"bkSetName\":\"运营门户\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"gls-ops01\",\"appName\":\"gls\",\"appAlias\":\"gls\",\"targetVersion\":\"10309\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"gls\",\"cfgs\":[{\"fileName\":\"Param-Config.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"1da62c81dacf6d7ee21fca3384f134c5\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/ops01_gls/Param-Config.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4M2U3MDJjNjViN2E5MjQw\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"createTime\":\"Jun 2, 2020 9:32:59 AM\",\"updateTime\":\"Jun 2, 2020 9:32:59 AM\",\"executeTime\":\"Jun 2, 2020 9:32:59 AM\",\"comment\":\"clone from 运营门户01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"}],\"platformId\":\"just-test\",\"platformName\":\"ccod开发测试平台\",\"platformFunc\":\"TEST\",\"bkBizId\":25,\"bkCloudId\":0,\"ccodVersion\":\"CCOD4.1\",\"taskType\":\"CREATE\",\"title\":\"新建工具组平台(202005-test)计划\",\"comment\":\"create 工具组平台(202005-test) by clone 123456-wuph(ccod开发测试平台)\",\"k8sHostIp\":\"10.130.41.218\",\"glsDBType\":\"ORACLE\",\"glsDBUser\":\"ccod\",\"glsDBPwd\":\"ccod\",\"baseDataNexusRepository\":\"platform_base_data\",\"baseDataNexusPath\":\"ccod/4.1/baseVolume.zip\",\"publicConfig\":[{\"fileName\":\"local_datasource.xml\",\"fileSize\":0,\"md5\":\"112940181aeb983baa9d7fd2733f194f\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_datasource.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmNmE4MWY3MWI3MmJlY2Ji\"},{\"fileName\":\"local_jvm.xml\",\"fileSize\":0,\"md5\":\"d172a5321944aba5bc19c35d00950afc\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_jvm.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRjMWQyMTIwNWRmYmY1MDM0\"},{\"fileName\":\"tnsnames.ora\",\"fileSize\":0,\"md5\":\"811f7f9472d5f6e733d732619a17ac77\",\"deployPath\":\"/usr/local/lib\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/tnsnames.ora\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY0NzFkOTU3MGMyODRjMGJm\"}],\"k8sApiUrl\":\"https://10.130.41.218:6443\",\"k8sAuthToken\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA\",\"k8sDeploymentList\":[{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"dcmssg-manage01\"},\"name\":\"dcmssg-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"dcmssg\":\"dcmssg\",\"dcmssg-version\":\"20070\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"dcmssg\":\"dcmssg\",\"dcmssg-version\":\"20070\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/jre/lib/security/cacerts; /root/resin-4.0.13/bin/resin.sh start; tail -F /root/resin-4.0.13/log/jvm-default.log\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/plastic-pa/resin4:pa2\",\"imagePullPolicy\":\"Always\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcmssg-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":120,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"dcmssg-runtime\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcmssg-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":60,\"periodSeconds\":5,\"successThreshold\":1,\"timeoutSeconds\":2},\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/resin-4.0.13/webapps\",\"name\":\"war\"},{\"mountPath\":\"/root/resin-4.0.13/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"args\":[],\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;mkdir /opt/WEB-INF/classes/ -p;cp /cfg/dcmssg-cfg/config.properties /opt/WEB-INF/classes/config.properties;jar uf dcmsSG.war WEB-INF/classes/config.properties;mkdir /opt/WEB-INF/ -p;cp /cfg/dcmssg-cfg/web.xml /opt/WEB-INF/web.xml;jar uf dcmsSG.war WEB-INF/web.xml;mv /opt/dcmsSG.war /war/dcmssg-manage01.war\"],\"image\":\"nexus.io:5000/ccod/dcmssg:20070\",\"imagePullPolicy\":\"Always\",\"name\":\"dcmssg\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/cfg/dcmssg-cfg\",\"name\":\"dcmssg-manage01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"data\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-just-test\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/dcmssg-manage01/dcmssg-manage01\"},\"name\":\"ccod-runtime\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"configMap\":{\"items\":[{\"key\":\"config.properties\",\"path\":\"config.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"dcmssg-manage01-c95bf0a00a\"},\"name\":\"dcmssg-manage01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"configserver-public01\"},\"name\":\"configserver-public01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"public01\",\"configserver-version\":\"aca2af60caa0fb9f4af57f37f869dafc90472525\",\"configserver\":\"configserver\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"configserver-version\":\"aca2af60caa0fb9f4af57f37f869dafc90472525\",\"configserver\":\"configserver\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;cd /root/Platform/bin;./configserver;sleep 5;tailf /root/Platform/log/*/*.log;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":18869},\"timeoutSeconds\":1},\"name\":\"configserver-runtime\",\"ports\":[{\"containerPort\":18869,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}],\"workingDir\":\"/root\"}],\"initContainers\":[{\"args\":[\"cp /opt/configserver /binary-file/\"],\"command\":[\"/bin/sh\",\"-c\",\"mkdir /binary-file/bin -p;mkdir /binary-file/log -p;mv /opt/configserver /binary-file/bin/configserver;mkdir /binary-file/cfg/ -p;cp /cfg/configserver-cfg/ccs_config.cfg /binary-file/cfg/ccs_config.cfg;cp /cfg/configserver-cfg/ccs_logger.cfg /binary-file/cfg/ccs_logger.cfg\"],\"image\":\"nexus.io:5000/ccod/configserver:aca2af60caa0fb9f4af57f37f869dafc90472525\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"configserver\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/cfg/configserver-cfg\",\"name\":\"configserver-public01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/configserver-public01/configserver-public01\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"ccs_config.cfg\",\"path\":\"ccs_config.cfg\"},{\"key\":\"ccs_logger.cfg\",\"path\":\"ccs_logger.cfg\"}],\"name\":\"configserver-public01-c95bf0a00a\"},\"name\":\"configserver-public01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"dcms-manage01\"},\"name\":\"dcms-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"dcms-version\":\"11110\",\"dcms\":\"dcms\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"dcms-version\":\"11110\",\"dcms\":\"dcms\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/jre/lib/security/cacerts; /root/resin-4.0.13/bin/resin.sh start; tail -F /root/resin-4.0.13/log/jvm-default.log\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/plastic-pa/resin4:pa2\",\"imagePullPolicy\":\"Always\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcms-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":120,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"dcms-runtime\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcms-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":60,\"periodSeconds\":5,\"successThreshold\":1,\"timeoutSeconds\":2},\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/resin-4.0.13/webapps\",\"name\":\"war\"},{\"mountPath\":\"/root/resin-4.0.13/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"args\":[],\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;mkdir /opt/WEB-INF/classes/ -p;cp /cfg/dcms-cfg/config.properties /opt/WEB-INF/classes/config.properties;jar uf dcms.war WEB-INF/classes/config.properties;cp /cfg/dcms-cfg/Param-Config.xml /opt/WEB-INF/classes/Param-Config.xml;jar uf dcms.war WEB-INF/classes/Param-Config.xml;mkdir /opt/WEB-INF/ -p;cp /cfg/dcms-cfg/web.xml /opt/WEB-INF/web.xml;jar uf dcms.war WEB-INF/web.xml;mv /opt/dcms.war /war/dcms-manage01.war\"],\"image\":\"nexus.io:5000/ccod/dcms:11110\",\"imagePullPolicy\":\"Always\",\"name\":\"dcms\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/cfg/dcms-cfg\",\"name\":\"dcms-manage01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"data\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-just-test\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/dcms-manage01/dcms-manage01\"},\"name\":\"ccod-runtime\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"configMap\":{\"items\":[{\"key\":\"Param-Config.xml\",\"path\":\"Param-Config.xml\"},{\"key\":\"config.properties\",\"path\":\"config.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"dcms-manage01-c95bf0a00a\"},\"name\":\"dcms-manage01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"daengine-cloud01\"},\"name\":\"daengine-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"daengine-version\":\"179-20744\",\"domain-id\":\"cloud01\",\"daengine\":\"daengine\"}},\"template\":{\"metadata\":{\"labels\":{\"daengine-version\":\"179-20744\",\"domain-id\":\"cloud01\",\"daengine\":\"daengine\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;cd /root/Platform/bin;./daengine;sleep 5;tailf /root/Platform/log/*/*.log;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":10101},\"timeoutSeconds\":1},\"name\":\"daengine-runtime\",\"ports\":[{\"containerPort\":10101,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/daengine /binary-file/\"],\"command\":[\"/bin/sh\",\"-c\",\"mkdir /binary-file/bin -p;mkdir /binary-file/log -p;mv /opt/daengine /binary-file/bin/daengine;mkdir /binary-file/cfg/ -p;cp /cfg/daengine-cfg/dae.cfg /binary-file/cfg/dae.cfg;cp /cfg/daengine-cfg/dae_logger.cfg /binary-file/cfg/dae_logger.cfg;cp /cfg/daengine-cfg/dae_config.cfg /binary-file/cfg/dae_config.cfg;cp /cfg/daengine-cfg/dae_log4cpp.cfg /binary-file/cfg/dae_log4cpp.cfg\"],\"image\":\"nexus.io:5000/ccod/daengine:179-20744\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"daengine\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/cfg/daengine-cfg\",\"name\":\"daengine-cloud01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/daengine-cloud01/daengine-cloud01\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"dae.cfg\",\"path\":\"dae.cfg\"},{\"key\":\"dae_config.cfg\",\"path\":\"dae_config.cfg\"},{\"key\":\"dae_log4cpp.cfg\",\"path\":\"dae_log4cpp.cfg\"},{\"key\":\"dae_logger.cfg\",\"path\":\"dae_logger.cfg\"}],\"name\":\"daengine-cloud01-c95bf0a00a\"},\"name\":\"daengine-cloud01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"dcmswebservice-manage01\"},\"name\":\"dcmswebservice-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"dcmsWebservice\":\"dcmswebservice\",\"dcmsWebservice-version\":\"20503\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"dcmsWebservice\":\"dcmswebservice\",\"dcmsWebservice-version\":\"20503\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/jre/lib/security/cacerts; /root/resin-4.0.13/bin/resin.sh start; tail -F /root/resin-4.0.13/log/jvm-default.log\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/plastic-pa/resin4:pa2\",\"imagePullPolicy\":\"Always\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcmswebservice-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":120,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"dcmswebservice-runtime\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcmswebservice-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":60,\"periodSeconds\":5,\"successThreshold\":1,\"timeoutSeconds\":2},\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/resin-4.0.13/webapps\",\"name\":\"war\"},{\"mountPath\":\"/root/resin-4.0.13/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"args\":[],\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;mkdir /opt/WEB-INF/classes/ -p;cp /cfg/dcmswebservice-cfg/config.properties /opt/WEB-INF/classes/config.properties;jar uf dcmsWebservice.war WEB-INF/classes/config.properties;mkdir /opt/WEB-INF/ -p;cp /cfg/dcmswebservice-cfg/web.xml /opt/WEB-INF/web.xml;jar uf dcmsWebservice.war WEB-INF/web.xml;mv /opt/dcmsWebservice.war /war/dcmswebservice-manage01.war\"],\"image\":\"nexus.io:5000/ccod/dcmswebservice:20503\",\"imagePullPolicy\":\"Always\",\"name\":\"dcmswebservice\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/cfg/dcmswebservice-cfg\",\"name\":\"dcmswebservice-manage01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"data\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-just-test\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/dcmswebservice-manage01/dcmswebservice-manage01\"},\"name\":\"ccod-runtime\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"configMap\":{\"items\":[{\"key\":\"config.properties\",\"path\":\"config.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"dcmswebservice-manage01-c95bf0a00a\"},\"name\":\"dcmswebservice-manage01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"cms1-cloud01\"},\"name\":\"cms1-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"cloud01\",\"cmsserver-version\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"cmsserver\":\"cms1\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"cmsserver-version\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"cmsserver\":\"cms1\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;cd /root/Platform/bin;./cmsserver --config.main\\u003d../etc/config.cms2;sleep 5;tailf /root/Platform/log/*/*.log;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":17119},\"timeoutSeconds\":1},\"name\":\"cms1-runtime\",\"ports\":[{\"containerPort\":17119,\"protocol\":\"TCP\"},{\"containerPort\":11520,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/cmsserver /binary-file/\"],\"command\":[\"/bin/sh\",\"-c\",\"mkdir /binary-file/bin -p;mkdir /binary-file/log -p;mv /opt/cmsserver /binary-file/bin/cmsserver;mkdir /binary-file/etc/ -p;cp /cfg/cms1-cfg/beijing.xml /binary-file/etc/beijing.xml;cp /cfg/cms1-cfg/config.cms2 /binary-file/etc/config.cms2;cp /cfg/cms1-cfg/cms_log4cpp.cfg /binary-file/etc/cms_log4cpp.cfg\"],\"image\":\"nexus.io:5000/ccod/cmsserver:4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"cms1\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/cfg/cms1-cfg\",\"name\":\"cms1-cloud01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/cms1-cloud01/cms1-cloud01\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"beijing.xml\",\"path\":\"beijing.xml\"},{\"key\":\"cms_log4cpp.cfg\",\"path\":\"cms_log4cpp.cfg\"},{\"key\":\"config.cms2\",\"path\":\"config.cms2\"}],\"name\":\"cms1-cloud01-c95bf0a00a\"},\"name\":\"cms1-cloud01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"dcmsstaticsreport-manage01\"},\"name\":\"dcmsstaticsreport-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"dcmsStaticsReport\":\"dcmsstaticsreport\",\"dcmsStaticsReport-version\":\"20528\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"dcmsStaticsReport\":\"dcmsstaticsreport\",\"dcmsStaticsReport-version\":\"20528\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/jre/lib/security/cacerts; /root/resin-4.0.13/bin/resin.sh start; tail -F /root/resin-4.0.13/log/jvm-default.log\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/plastic-pa/resin4:pa2\",\"imagePullPolicy\":\"Always\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcmsstaticsreport-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":120,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"dcmsstaticsreport-runtime\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcmsstaticsreport-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":60,\"periodSeconds\":5,\"successThreshold\":1,\"timeoutSeconds\":2},\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/resin-4.0.13/webapps\",\"name\":\"war\"},{\"mountPath\":\"/root/resin-4.0.13/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"args\":[],\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;mkdir /opt/WEB-INF/classes/ -p;cp /cfg/dcmsstaticsreport-cfg/applicationContext.xml /opt/WEB-INF/classes/applicationContext.xml;jar uf dcmsStaticsReport.war WEB-INF/classes/applicationContext.xml;cp /cfg/dcmsstaticsreport-cfg/config.properties /opt/WEB-INF/classes/config.properties;jar uf dcmsStaticsReport.war WEB-INF/classes/config.properties;mkdir /opt/WEB-INF/ -p;cp /cfg/dcmsstaticsreport-cfg/web.xml /opt/WEB-INF/web.xml;jar uf dcmsStaticsReport.war WEB-INF/web.xml;mv /opt/dcmsStaticsReport.war /war/dcmsstaticsreport-manage01.war\"],\"image\":\"nexus.io:5000/ccod/dcmsstaticsreport:20528\",\"imagePullPolicy\":\"Always\",\"name\":\"dcmsstaticsreport\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/cfg/dcmsstaticsreport-cfg\",\"name\":\"dcmsstaticsreport-manage01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"data\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-just-test\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/dcmsstaticsreport-manage01/dcmsstaticsreport-manage01\"},\"name\":\"ccod-runtime\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"configMap\":{\"items\":[{\"key\":\"applicationContext.xml\",\"path\":\"applicationContext.xml\"},{\"key\":\"config.properties\",\"path\":\"config.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"dcmsstaticsreport-manage01-c95bf0a00a\"},\"name\":\"dcmsstaticsreport-manage01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"cms2-cloud01\"},\"name\":\"cms2-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"cloud01\",\"cmsserver-version\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"cmsserver\":\"cms2\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"cmsserver-version\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"cmsserver\":\"cms2\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;cd /root/Platform/bin;./cmsserver --config.main\\u003d../etc/config.cms2;sleep 5;tailf /root/Platform/log/*/*.log;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":17119},\"timeoutSeconds\":1},\"name\":\"cms2-runtime\",\"ports\":[{\"containerPort\":17119,\"protocol\":\"TCP\"},{\"containerPort\":11520,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/cmsserver /binary-file/\"],\"command\":[\"/bin/sh\",\"-c\",\"mkdir /binary-file/bin -p;mkdir /binary-file/log -p;mv /opt/cmsserver /binary-file/bin/cmsserver;mkdir /binary-file/etc/ -p;cp /cfg/cms2-cfg/beijing.xml /binary-file/etc/beijing.xml;cp /cfg/cms2-cfg/cms_log4cpp.cfg /binary-file/etc/cms_log4cpp.cfg;cp /cfg/cms2-cfg/config.cms2 /binary-file/etc/config.cms2\"],\"image\":\"nexus.io:5000/ccod/cmsserver:4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"cms2\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/cfg/cms2-cfg\",\"name\":\"cms2-cloud01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/cms2-cloud01/cms2-cloud01\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"beijing.xml\",\"path\":\"beijing.xml\"},{\"key\":\"cms_log4cpp.cfg\",\"path\":\"cms_log4cpp.cfg\"},{\"key\":\"config.cms2\",\"path\":\"config.cms2\"}],\"name\":\"cms2-cloud01-c95bf0a00a\"},\"name\":\"cms2-cloud01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"ops01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"gls-ops01\"},\"name\":\"gls-ops01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"ops01\",\"gls-version\":\"10309\",\"gls\":\"gls\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"ops01\",\"gls-version\":\"10309\",\"gls\":\"gls\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/jre/lib/security/cacerts; /root/resin-4.0.13/bin/resin.sh start; tail -F /root/resin-4.0.13/log/jvm-default.log\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/plastic-pa/resin4:pa2\",\"imagePullPolicy\":\"Always\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/gls-ops01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":120,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"gls-runtime\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/gls-ops01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":60,\"periodSeconds\":5,\"successThreshold\":1,\"timeoutSeconds\":2},\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/resin-4.0.13/webapps\",\"name\":\"war\"},{\"mountPath\":\"/root/resin-4.0.13/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"args\":[],\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;mkdir /opt/WEB-INF/classes/ -p;cp /cfg/gls-cfg/Param-Config.xml /opt/WEB-INF/classes/Param-Config.xml;jar uf gls.war WEB-INF/classes/Param-Config.xml;mv /opt/gls.war /war/gls-ops01.war\"],\"image\":\"nexus.io:5000/ccod/gls:10309\",\"imagePullPolicy\":\"Always\",\"name\":\"gls\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/cfg/gls-cfg\",\"name\":\"gls-ops01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"data\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-just-test\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/gls-ops01/gls-ops01\"},\"name\":\"ccod-runtime\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"configMap\":{\"items\":[{\"key\":\"Param-Config.xml\",\"path\":\"Param-Config.xml\"}],\"name\":\"gls-ops01-c95bf0a00a\"},\"name\":\"gls-ops01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"dcmsstatics-manage01\"},\"name\":\"dcmsstatics-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"dcmsStatics\":\"dcmsstatics\",\"dcmsStatics-version\":\"20537\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"dcmsStatics\":\"dcmsstatics\",\"dcmsStatics-version\":\"20537\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/jre/lib/security/cacerts; /root/resin-4.0.13/bin/resin.sh start; tail -F /root/resin-4.0.13/log/jvm-default.log\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/plastic-pa/resin4:pa2\",\"imagePullPolicy\":\"Always\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcmsstatics-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":120,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"dcmsstatics-runtime\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcmsstatics-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":60,\"periodSeconds\":5,\"successThreshold\":1,\"timeoutSeconds\":2},\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/resin-4.0.13/webapps\",\"name\":\"war\"},{\"mountPath\":\"/root/resin-4.0.13/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"args\":[],\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;mkdir /opt/WEB-INF/classes/ -p;cp /cfg/dcmsstatics-cfg/applicationContext.xml /opt/WEB-INF/classes/applicationContext.xml;jar uf dcmsStatics.war WEB-INF/classes/applicationContext.xml;cp /cfg/dcmsstatics-cfg/config.properties /opt/WEB-INF/classes/config.properties;jar uf dcmsStatics.war WEB-INF/classes/config.properties;mkdir /opt/WEB-INF/ -p;cp /cfg/dcmsstatics-cfg/web.xml /opt/WEB-INF/web.xml;jar uf dcmsStatics.war WEB-INF/web.xml;mv /opt/dcmsStatics.war /war/dcmsstatics-manage01.war\"],\"image\":\"nexus.io:5000/ccod/dcmsstatics:20537\",\"imagePullPolicy\":\"Always\",\"name\":\"dcmsstatics\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/cfg/dcmsstatics-cfg\",\"name\":\"dcmsstatics-manage01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"data\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-just-test\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/dcmsstatics-manage01/dcmsstatics-manage01\"},\"name\":\"ccod-runtime\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"configMap\":{\"items\":[{\"key\":\"applicationContext.xml\",\"path\":\"applicationContext.xml\"},{\"key\":\"config.properties\",\"path\":\"config.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"dcmsstatics-manage01-c95bf0a00a\"},\"name\":\"dcmsstatics-manage01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"dds-cloud01\"},\"name\":\"dds-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"cloud01\",\"DDSServer\":\"dds\",\"DDSServer-version\":\"150-18722\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"DDSServer\":\"dds\",\"DDSServer-version\":\"150-18722\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;cd /root/Platform/bin;./DDSServer;sleep 5;tailf /root/Platform/log/*/*.log;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":17088},\"timeoutSeconds\":1},\"name\":\"dds-runtime\",\"ports\":[{\"containerPort\":17088,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/DDSServer /binary-file/\"],\"command\":[\"/bin/sh\",\"-c\",\"mkdir /binary-file/bin -p;mkdir /binary-file/log -p;mv /opt/DDSServer /binary-file/bin/DDSServer;mkdir /binary-file/cfg/ -p;cp /cfg/dds-cfg/dds_logger.cfg /binary-file/cfg/dds_logger.cfg;cp /cfg/dds-cfg/dds_config.cfg /binary-file/cfg/dds_config.cfg\"],\"image\":\"nexus.io:5000/ccod/ddsserver:150-18722\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"dds\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/cfg/dds-cfg\",\"name\":\"dds-cloud01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/dds-cloud01/dds-cloud01\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"dds_config.cfg\",\"path\":\"dds_config.cfg\"},{\"key\":\"dds_logger.cfg\",\"path\":\"dds_logger.cfg\"}],\"name\":\"dds-cloud01-c95bf0a00a\"},\"name\":\"dds-cloud01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"cas-manage01\"},\"name\":\"cas-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"cas\":\"cas\",\"cas-version\":\"10973\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"cas\":\"cas\",\"cas-version\":\"10973\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh; tail -F /usr/local/tomcat/logs/catalina.out\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/ccod-base/tomcat6-jre7:1\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/cas-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"cas-runtime\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"args\":[],\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;mkdir /opt/WEB-INF/ -p;cp /cfg/cas-cfg/web.xml /opt/WEB-INF/web.xml;jar uf cas.war WEB-INF/web.xml;cp /cfg/cas-cfg/cas.properties /opt/WEB-INF/cas.properties;jar uf cas.war WEB-INF/cas.properties;mv /opt/cas.war /war/cas-manage01.war\"],\"image\":\"nexus.io:5000/ccod/cas:10973\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"cas\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/cfg/cas-cfg\",\"name\":\"cas-manage01-c95bf0a00a-volume\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/cas-manage01/cas-manage01\"},\"name\":\"ccod-runtime\"},{\"configMap\":{\"items\":[{\"key\":\"cas.properties\",\"path\":\"cas.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"cas-manage01-c95bf0a00a\"},\"name\":\"cas-manage01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"dcproxy-cloud01\"},\"name\":\"dcproxy-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"cloud01\",\"dcproxy\":\"dcproxy\",\"dcproxy-version\":\"195-21857\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"dcproxy\":\"dcproxy\",\"dcproxy-version\":\"195-21857\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;cd /root/Platform/bin;./dcproxy;sleep 5;tailf /root/Platform/log/*/*.log;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":12009},\"timeoutSeconds\":1},\"name\":\"dcproxy-runtime\",\"ports\":[{\"containerPort\":12009,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/dcproxy /binary-file/\"],\"command\":[\"/bin/sh\",\"-c\",\"mkdir /binary-file/bin -p;mkdir /binary-file/log -p;mv /opt/dcproxy /binary-file/bin/dcproxy;mkdir /binary-file/cfg/ -p;cp /cfg/dcproxy-cfg/dcp_config.cfg /binary-file/cfg/dcp_config.cfg;cp /cfg/dcproxy-cfg/dcp_logger.cfg /binary-file/cfg/dcp_logger.cfg\"],\"image\":\"nexus.io:5000/ccod/dcproxy:195-21857\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"dcproxy\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/cfg/dcproxy-cfg\",\"name\":\"dcproxy-cloud01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/dcproxy-cloud01/dcproxy-cloud01\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"dcp_config.cfg\",\"path\":\"dcp_config.cfg\"},{\"key\":\"dcp_logger.cfg\",\"path\":\"dcp_logger.cfg\"}],\"name\":\"dcproxy-cloud01-c95bf0a00a\"},\"name\":\"dcproxy-cloud01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"safetymonitor-manage01\"},\"name\":\"safetymonitor-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"safetyMonitor-version\":\"20383\",\"domain-id\":\"manage01\",\"safetyMonitor\":\"safetymonitor\"}},\"template\":{\"metadata\":{\"labels\":{\"safetyMonitor-version\":\"20383\",\"domain-id\":\"manage01\",\"safetyMonitor\":\"safetymonitor\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/jre/lib/security/cacerts; /root/resin-4.0.13/bin/resin.sh start; tail -F /root/resin-4.0.13/log/jvm-default.log\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/plastic-pa/resin4:pa2\",\"imagePullPolicy\":\"Always\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/safetymonitor-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":120,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"safetymonitor-runtime\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/safetymonitor-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":60,\"periodSeconds\":5,\"successThreshold\":1,\"timeoutSeconds\":2},\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/resin-4.0.13/webapps\",\"name\":\"war\"},{\"mountPath\":\"/root/resin-4.0.13/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"args\":[],\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;mkdir /opt/WEB-INF/classes/ -p;cp /cfg/safetymonitor-cfg/applicationContext.xml /opt/WEB-INF/classes/applicationContext.xml;jar uf safetyMonitor.war WEB-INF/classes/applicationContext.xml;cp /cfg/safetymonitor-cfg/config.properties /opt/WEB-INF/classes/config.properties;jar uf safetyMonitor.war WEB-INF/classes/config.properties;mkdir /opt/WEB-INF/ -p;cp /cfg/safetymonitor-cfg/web.xml /opt/WEB-INF/web.xml;jar uf safetyMonitor.war WEB-INF/web.xml;mv /opt/safetyMonitor.war /war/safetymonitor-manage01.war\"],\"image\":\"nexus.io:5000/ccod/safetymonitor:20383\",\"imagePullPolicy\":\"Always\",\"name\":\"safetymonitor\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/cfg/safetymonitor-cfg\",\"name\":\"safetymonitor-manage01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"data\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-just-test\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/safetymonitor-manage01/safetymonitor-manage01\"},\"name\":\"ccod-runtime\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"configMap\":{\"items\":[{\"key\":\"applicationContext.xml\",\"path\":\"applicationContext.xml\"},{\"key\":\"config.properties\",\"path\":\"config.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"safetymonitor-manage01-c95bf0a00a\"},\"name\":\"safetymonitor-manage01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"ucx-cloud01\"},\"name\":\"ucx-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"cloud01\",\"ucxserver-version\":\"1fef2157ea07c483979b424c758192bd709e6c2a\",\"ucxserver\":\"ucx\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"ucxserver-version\":\"1fef2157ea07c483979b424c758192bd709e6c2a\",\"ucxserver\":\"ucx\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;cd /root/Platform/bin;./ucxserver --config.main\\u003d../cfg/config.ucx;sleep 5;tailf /root/Platform/log/*/*.log;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":11000},\"timeoutSeconds\":1},\"name\":\"ucx-runtime\",\"ports\":[{\"containerPort\":11000,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/ucxserver /binary-file/\"],\"command\":[\"/bin/sh\",\"-c\",\"mkdir /binary-file/bin -p;mkdir /binary-file/log -p;mv /opt/ucxserver /binary-file/bin/ucxserver;mkdir /binary-file/cfg/ -p;cp /cfg/ucx-cfg/config.ucx /binary-file/cfg/config.ucx;mv /opt/FlowMap.full /binary-file/cfg\"],\"image\":\"nexus.io:5000/ccod/ucxserver:1fef2157ea07c483979b424c758192bd709e6c2a\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"ucx\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/cfg/ucx-cfg\",\"name\":\"ucx-cloud01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/ucx-cloud01/ucx-cloud01\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"config.ucx\",\"path\":\"config.ucx\"}],\"name\":\"ucx-cloud01-c95bf0a00a\"},\"name\":\"ucx-cloud01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"customwebservice-manage01\"},\"name\":\"customwebservice-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"customWebservice\":\"customwebservice\",\"customWebservice-version\":\"19553\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"customWebservice\":\"customwebservice\",\"customWebservice-version\":\"19553\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/jre/lib/security/cacerts; /root/resin-4.0.13/bin/resin.sh start; tail -F /root/resin-4.0.13/log/jvm-default.log\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/plastic-pa/resin4:pa2\",\"imagePullPolicy\":\"Always\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/customwebservice-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":120,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"customwebservice-runtime\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/customwebservice-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":60,\"periodSeconds\":5,\"successThreshold\":1,\"timeoutSeconds\":2},\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/resin-4.0.13/webapps\",\"name\":\"war\"},{\"mountPath\":\"/root/resin-4.0.13/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"args\":[],\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;mkdir /opt/WEB-INF/classes/ -p;cp /cfg/customwebservice-cfg/web.xml /opt/WEB-INF/classes/web.xml;jar uf customWebservice.war WEB-INF/classes/web.xml;cp /cfg/customwebservice-cfg/config.properties /opt/WEB-INF/classes/config.properties;jar uf customWebservice.war WEB-INF/classes/config.properties;mv /opt/customWebservice.war /war/customwebservice-manage01.war\"],\"image\":\"nexus.io:5000/ccod/customwebservice:19553\",\"imagePullPolicy\":\"Always\",\"name\":\"customwebservice\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/cfg/customwebservice-cfg\",\"name\":\"customwebservice-manage01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"data\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-just-test\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/customwebservice-manage01/customwebservice-manage01\"},\"name\":\"ccod-runtime\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"configMap\":{\"items\":[{\"key\":\"config.properties\",\"path\":\"config.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"customwebservice-manage01-c95bf0a00a\"},\"name\":\"customwebservice-manage01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"dcmsx-manage01\"},\"name\":\"dcmsx-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"dcmsx-version\":\"master_8efabf4\",\"dcmsx\":\"dcmsx\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"dcmsx-version\":\"master_8efabf4\",\"dcmsx\":\"dcmsx\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/jre/lib/security/cacerts; /root/resin-4.0.13/bin/resin.sh start; tail -F /root/resin-4.0.13/log/jvm-default.log\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/plastic-pa/resin4:pa2\",\"imagePullPolicy\":\"Always\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcmsx-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":120,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"dcmsx-runtime\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcmsx-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":60,\"periodSeconds\":5,\"successThreshold\":1,\"timeoutSeconds\":2},\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/resin-4.0.13/webapps\",\"name\":\"war\"},{\"mountPath\":\"/root/resin-4.0.13/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"args\":[],\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;mkdir /opt/WEB-INF/classes/ -p;cp /cfg/dcmsx-cfg/application.properties /opt/WEB-INF/classes/application.properties;jar uf dcmsx.war WEB-INF/classes/application.properties;mkdir /opt/WEB-INF/ -p;cp /cfg/dcmsx-cfg/web.xml /opt/WEB-INF/web.xml;jar uf dcmsx.war WEB-INF/web.xml;mv /opt/dcmsx.war /war/dcmsx-manage01.war\"],\"image\":\"nexus.io:5000/ccod/dcmsx:master_8efabf4\",\"imagePullPolicy\":\"Always\",\"name\":\"dcmsx\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/cfg/dcmsx-cfg\",\"name\":\"dcmsx-manage01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"data\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-just-test\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/dcmsx-manage01/dcmsx-manage01\"},\"name\":\"ccod-runtime\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"configMap\":{\"items\":[{\"key\":\"application.properties\",\"path\":\"application.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"dcmsx-manage01-c95bf0a00a\"},\"name\":\"dcmsx-manage01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"dcmsrecord-manage01\"},\"name\":\"dcmsrecord-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"dcmsRecord\":\"dcmsrecord\",\"dcmsRecord-version\":\"21763\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"dcmsRecord\":\"dcmsrecord\",\"dcmsRecord-version\":\"21763\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/jre/lib/security/cacerts; /root/resin-4.0.13/bin/resin.sh start; tail -F /root/resin-4.0.13/log/jvm-default.log\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/plastic-pa/resin4:pa2\",\"imagePullPolicy\":\"Always\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcmsrecord-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":120,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"dcmsrecord-runtime\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/dcmsrecord-manage01\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":60,\"periodSeconds\":5,\"successThreshold\":1,\"timeoutSeconds\":2},\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/resin-4.0.13/webapps\",\"name\":\"war\"},{\"mountPath\":\"/root/resin-4.0.13/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"args\":[],\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;mkdir /opt/WEB-INF/classes/ -p;cp /cfg/dcmsrecord-cfg/applicationContext.xml /opt/WEB-INF/classes/applicationContext.xml;jar uf dcmsRecord.war WEB-INF/classes/applicationContext.xml;cp /cfg/dcmsrecord-cfg/config.properties /opt/WEB-INF/classes/config.properties;jar uf dcmsRecord.war WEB-INF/classes/config.properties;mkdir /opt/WEB-INF/ -p;cp /cfg/dcmsrecord-cfg/web.xml /opt/WEB-INF/web.xml;jar uf dcmsRecord.war WEB-INF/web.xml;mv /opt/dcmsRecord.war /war/dcmsrecord-manage01.war\"],\"image\":\"nexus.io:5000/ccod/dcmsrecord:21763\",\"imagePullPolicy\":\"Always\",\"name\":\"dcmsrecord\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/cfg/dcmsrecord-cfg\",\"name\":\"dcmsrecord-manage01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"data\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-just-test\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/dcmsrecord-manage01/dcmsrecord-manage01\"},\"name\":\"ccod-runtime\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"configMap\":{\"items\":[{\"key\":\"applicationContext.xml\",\"path\":\"applicationContext.xml\"},{\"key\":\"config.properties\",\"path\":\"config.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"dcmsrecord-manage01-c95bf0a00a\"},\"name\":\"dcmsrecord-manage01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"ss-cloud01\"},\"name\":\"ss-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"cloud01\",\"StatSchedule-version\":\"154-21104\",\"StatSchedule\":\"ss\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"StatSchedule-version\":\"154-21104\",\"StatSchedule\":\"ss\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;cd /root/Platform/bin;./StatSchedule;sleep 5;tailf /root/Platform/log/*/*.log;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":18889},\"timeoutSeconds\":1},\"name\":\"ss-runtime\",\"ports\":[{\"containerPort\":18889,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/StatSchedule /binary-file/\"],\"command\":[\"/bin/sh\",\"-c\",\"mkdir /binary-file/bin -p;mkdir /binary-file/log -p;mv /opt/StatSchedule /binary-file/bin/StatSchedule;mkdir /binary-file/cfg/ -p;cp /cfg/ss-cfg/ss_config.cfg /binary-file/cfg/ss_config.cfg\"],\"image\":\"nexus.io:5000/ccod/statschedule:154-21104\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"ss\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/cfg/ss-cfg\",\"name\":\"ss-cloud01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/ss-cloud01/ss-cloud01\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"ss_config.cfg\",\"path\":\"ss_config.cfg\"}],\"name\":\"ss-cloud01-c95bf0a00a\"},\"name\":\"ss-cloud01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}}],\"k8sServiceList\":[{\"metadata\":{\"labels\":{\"name\":\"dcmssg-manage01\",\"domain-id\":\"manage01\",\"app-name\":\"dcmssg\",\"alias\":\"dcmssg\",\"version\":\"20070\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"dcmssg-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"dcmssg-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"domain-id\":\"manage01\",\"dcmssg\":\"dcmssg\",\"dcmssg-version\":\"20070\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"configserver-public01\",\"domain-id\":\"public01\",\"app-name\":\"configserver\",\"alias\":\"configserver\",\"version\":\"aca2af60caa0fb9f4af57f37f869dafc90472525\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"configserver-public01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"configserver-public01-18869\",\"port\":18869,\"protocol\":\"TCP\",\"targetPort\":18869}],\"selector\":{\"domain-id\":\"public01\",\"configserver-version\":\"aca2af60caa0fb9f4af57f37f869dafc90472525\",\"configserver\":\"configserver\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"dcms-manage01\",\"domain-id\":\"manage01\",\"app-name\":\"dcms\",\"alias\":\"dcms\",\"version\":\"11110\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"dcms-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"dcms-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"domain-id\":\"manage01\",\"dcms-version\":\"11110\",\"dcms\":\"dcms\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"service\":\"umg\",\"service-type\":\"THREE_PART_SERVICE\"},\"name\":\"umg147\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"port\":12000,\"protocol\":\"TCP\",\"targetPort\":12000}],\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"daengine-cloud01\",\"domain-id\":\"cloud01\",\"app-name\":\"daengine\",\"alias\":\"daengine\",\"version\":\"179-20744\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"daengine-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"daengine-cloud01-10101\",\"port\":10101,\"protocol\":\"TCP\",\"targetPort\":10101}],\"selector\":{\"daengine-version\":\"179-20744\",\"domain-id\":\"cloud01\",\"daengine\":\"daengine\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"dcmswebservice-manage01\",\"domain-id\":\"manage01\",\"app-name\":\"dcmsWebservice\",\"alias\":\"dcmswebservice\",\"version\":\"20503\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"dcmswebservice-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"dcmswebservice-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"domain-id\":\"manage01\",\"dcmsWebservice\":\"dcmswebservice\",\"dcmsWebservice-version\":\"20503\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"cms1-cloud01\",\"domain-id\":\"cloud01\",\"app-name\":\"cmsserver\",\"alias\":\"cms1\",\"version\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"cms1-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"cms1-cloud01-17119\",\"port\":17119,\"protocol\":\"TCP\",\"targetPort\":17119},{\"name\":\"cms1-cloud01-11520\",\"port\":11520,\"protocol\":\"TCP\",\"targetPort\":11520}],\"selector\":{\"domain-id\":\"cloud01\",\"cmsserver-version\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"cmsserver\":\"cms1\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"dcmsstaticsreport-manage01\",\"domain-id\":\"manage01\",\"app-name\":\"dcmsStaticsReport\",\"alias\":\"dcmsstaticsreport\",\"version\":\"20528\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"dcmsstaticsreport-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"dcmsstaticsreport-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"domain-id\":\"manage01\",\"dcmsStaticsReport\":\"dcmsstaticsreport\",\"dcmsStaticsReport-version\":\"20528\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"cms2-cloud01\",\"domain-id\":\"cloud01\",\"app-name\":\"cmsserver\",\"alias\":\"cms2\",\"version\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"cms2-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"cms2-cloud01-17119\",\"port\":17119,\"protocol\":\"TCP\",\"targetPort\":17119},{\"name\":\"cms2-cloud01-11520\",\"port\":11520,\"protocol\":\"TCP\",\"targetPort\":11520}],\"selector\":{\"domain-id\":\"cloud01\",\"cmsserver-version\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"cmsserver\":\"cms2\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"gls-ops01\",\"domain-id\":\"ops01\",\"app-name\":\"gls\",\"alias\":\"gls\",\"version\":\"10309\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"gls-ops01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"gls-ops01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"domain-id\":\"ops01\",\"gls-version\":\"10309\",\"gls\":\"gls\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"dcmsstatics-manage01\",\"domain-id\":\"manage01\",\"app-name\":\"dcmsStatics\",\"alias\":\"dcmsstatics\",\"version\":\"20537\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"dcmsstatics-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"dcmsstatics-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"domain-id\":\"manage01\",\"dcmsStatics\":\"dcmsstatics\",\"dcmsStatics-version\":\"20537\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"dds-cloud01\",\"domain-id\":\"cloud01\",\"app-name\":\"DDSServer\",\"alias\":\"dds\",\"version\":\"150-18722\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"dds-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"dds-cloud01-17088\",\"port\":17088,\"protocol\":\"TCP\",\"targetPort\":17088}],\"selector\":{\"domain-id\":\"cloud01\",\"DDSServer\":\"dds\",\"DDSServer-version\":\"150-18722\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"cas-manage01\",\"domain-id\":\"manage01\",\"app-name\":\"cas\",\"alias\":\"cas\",\"version\":\"10973\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"cas-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"cas-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"domain-id\":\"manage01\",\"cas\":\"cas\",\"cas-version\":\"10973\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"service\":\"umg\",\"service-type\":\"THREE_PART_SERVICE\"},\"name\":\"umg141\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"port\":12000,\"protocol\":\"TCP\",\"targetPort\":12000}],\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"dcproxy-cloud01\",\"domain-id\":\"cloud01\",\"app-name\":\"dcproxy\",\"alias\":\"dcproxy\",\"version\":\"195-21857\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"dcproxy-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"dcproxy-cloud01-12009\",\"port\":12009,\"protocol\":\"TCP\",\"targetPort\":12009}],\"selector\":{\"domain-id\":\"cloud01\",\"dcproxy\":\"dcproxy\",\"dcproxy-version\":\"195-21857\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"safetymonitor-manage01\",\"domain-id\":\"manage01\",\"app-name\":\"safetyMonitor\",\"alias\":\"safetymonitor\",\"version\":\"20383\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"safetymonitor-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"safetymonitor-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"safetyMonitor-version\":\"20383\",\"domain-id\":\"manage01\",\"safetyMonitor\":\"safetymonitor\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"ucx-cloud01\",\"domain-id\":\"cloud01\",\"app-name\":\"ucxserver\",\"alias\":\"ucx\",\"version\":\"1fef2157ea07c483979b424c758192bd709e6c2a\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"ucx-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"ucx-cloud01-11000\",\"port\":11000,\"protocol\":\"TCP\",\"targetPort\":11000}],\"selector\":{\"domain-id\":\"cloud01\",\"ucxserver-version\":\"1fef2157ea07c483979b424c758192bd709e6c2a\",\"ucxserver\":\"ucx\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"service\":\"umg\",\"service-type\":\"THREE_PART_SERVICE\"},\"name\":\"umg41\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"port\":12000,\"protocol\":\"TCP\",\"targetPort\":12000}],\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"customwebservice-manage01\",\"domain-id\":\"manage01\",\"app-name\":\"customWebservice\",\"alias\":\"customwebservice\",\"version\":\"19553\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"customwebservice-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"customwebservice-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"domain-id\":\"manage01\",\"customWebservice\":\"customwebservice\",\"customWebservice-version\":\"19553\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"dcmsx-manage01\",\"domain-id\":\"manage01\",\"app-name\":\"dcmsx\",\"alias\":\"dcmsx\",\"version\":\"master_8efabf4\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"dcmsx-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"dcmsx-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"domain-id\":\"manage01\",\"dcmsx-version\":\"master_8efabf4\",\"dcmsx\":\"dcmsx\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"dcmsrecord-manage01\",\"domain-id\":\"manage01\",\"app-name\":\"dcmsRecord\",\"alias\":\"dcmsrecord\",\"version\":\"21763\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"dcmsrecord-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"dcmsrecord-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"domain-id\":\"manage01\",\"dcmsRecord\":\"dcmsrecord\",\"dcmsRecord-version\":\"21763\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"ss-cloud01\",\"domain-id\":\"cloud01\",\"app-name\":\"StatSchedule\",\"alias\":\"ss\",\"version\":\"154-21104\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"ss-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"ss-cloud01-18889\",\"port\":18889,\"protocol\":\"TCP\",\"targetPort\":18889}],\"selector\":{\"domain-id\":\"cloud01\",\"StatSchedule-version\":\"154-21104\",\"StatSchedule\":\"ss\"},\"type\":\"ClusterIP\"}}],\"k8sEndpointsList\":[{\"metadata\":{\"labels\":{\"name\":\"cas-manage01\"},\"name\":\"cas-manage01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.116\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"cas-manage01-7b57b46c84-f9wxl\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"cas-manage01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"cms1-cloud01\"},\"name\":\"cms1-cloud01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.3\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"cms1-cloud01-56d5967cbb-2pphl\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"cms1-cloud01-11520\",\"port\":11520,\"protocol\":\"TCP\"},{\"name\":\"cms1-cloud01-17119\",\"port\":17119,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"cms2-cloud01\"},\"name\":\"cms2-cloud01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.40\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"cms2-cloud01-85b95cd8c5-2vgvj\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"cms2-cloud01-11520\",\"port\":11520,\"protocol\":\"TCP\"},{\"name\":\"cms2-cloud01-17119\",\"port\":17119,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"configserver-public01\"},\"name\":\"configserver-public01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.57\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"configserver-public01-6486748b57-8zxgb\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"configserver-public01-18869\",\"port\":18869,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"customwebservice-manage01\"},\"name\":\"customwebservice-manage01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.7\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"customwebservice-manage01-b85cbcd8f-wjmzk\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"customwebservice-manage01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"daengine-cloud01\"},\"name\":\"daengine-cloud01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.31\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"daengine-cloud01-cc988bbbd-6tvjn\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"daengine-cloud01-10101\",\"port\":10101,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"dcms-manage01\"},\"name\":\"dcms-manage01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.55\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"dcms-manage01-cd76d86cd-qqsdr\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"dcms-manage01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"dcmsrecord-manage01\"},\"name\":\"dcmsrecord-manage01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.16\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"dcmsrecord-manage01-56bdbf5f74-qpn2j\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"dcmsrecord-manage01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"dcmssg-manage01\"},\"name\":\"dcmssg-manage01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.20\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"dcmssg-manage01-79f488b548-9p8sm\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"dcmssg-manage01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"dcmsstatics-manage01\"},\"name\":\"dcmsstatics-manage01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.17\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"dcmsstatics-manage01-5f7d6cf7fd-hndcp\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"dcmsstatics-manage01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"dcmsstaticsreport-manage01\"},\"name\":\"dcmsstaticsreport-manage01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.5\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"dcmsstaticsreport-manage01-857dfc5d9d-qj7qp\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"dcmsstaticsreport-manage01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"dcmswebservice-manage01\"},\"name\":\"dcmswebservice-manage01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.37\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"dcmswebservice-manage01-7f488855cd-26hv6\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"dcmswebservice-manage01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"dcmsx-manage01\"},\"name\":\"dcmsx-manage01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.43\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"dcmsx-manage01-6f98489586-nnt2l\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"dcmsx-manage01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"dcproxy-cloud01\"},\"name\":\"dcproxy-cloud01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.25\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"dcproxy-cloud01-b7fc6464-q5lks\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"dcproxy-cloud01-12009\",\"port\":12009,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"dcs-cloud01\"},\"name\":\"dcs-cloud01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.21\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"dcs-cloud01-86f859845d-ttm5d\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"dcs-cloud01-18070\",\"port\":18070,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"dds-cloud01\"},\"name\":\"dds-cloud01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.33\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"dds-cloud01-56c5d84c4d-r5l8g\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"dds-cloud01-17088\",\"port\":17088,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"gls-ops01\"},\"name\":\"gls-ops01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.18\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"gls-ops01-777b9cf7bc-zj48g\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"gls-ops01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"glsserver-public01\"},\"name\":\"glsserver-public01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.54\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"glsserver-public01-69d8868fb-cwrbg\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"glsserver-public01-17020\",\"port\":17020,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"licenseserver-public01\"},\"name\":\"licenseserver-public01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.29\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"licenseserver-public01-7954667f4-xq7wv\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"licenseserver-public01-17021\",\"port\":17021,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"name\":\"mysql\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.2\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"mysql-5-7-29-6b7678546d-nn4g7\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"port\":3306,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"name\":\"oracle\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.23\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"oracle-55976dbbfc-8v2sj\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"port\":1521,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"safetymonitor-manage01\"},\"name\":\"safetymonitor-manage01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.61\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"safetymonitor-manage01-64f49fbd67-rlwjx\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"safetymonitor-manage01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"ss-cloud01\"},\"name\":\"ss-cloud01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.30\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"ss-cloud01-567bbb8497-jf5mx\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"ss-cloud01-18889\",\"port\":18889,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"ucds-cloud01\"},\"name\":\"ucds-cloud01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.34\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"ucds-cloud01-695d567675-zmcp4\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"ucds-cloud01-60001\",\"port\":60001,\"protocol\":\"TCP\"},{\"name\":\"ucds-cloud01-17009\",\"port\":17009,\"protocol\":\"TCP\"},{\"name\":\"ucds-cloud01-17002\",\"port\":17002,\"protocol\":\"TCP\"},{\"name\":\"ucds-cloud01-17004\",\"port\":17004,\"protocol\":\"TCP\"},{\"name\":\"ucds-cloud01-12003\",\"port\":12003,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"ucds-cloud01-out\"},\"name\":\"ucds-cloud01-out\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.34\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"ucds-cloud01-695d567675-zmcp4\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"ucds-cloud01-12003\",\"port\":12003,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"labels\":{\"name\":\"ucx-cloud01\"},\"name\":\"ucx-cloud01\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.59\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"ucx-cloud01-6b8cbcdb8b-z6sn5\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"ucx-cloud01-11000\",\"port\":11000,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"name\":\"umg141\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"10.130.41.141\"}],\"ports\":[{\"port\":12000,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"name\":\"umg147\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"10.130.41.147\"}],\"ports\":[{\"port\":12000,\"protocol\":\"TCP\"}]}]},{\"metadata\":{\"name\":\"umg41\",\"namespace\":\"just-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"10.130.41.41\"}],\"ports\":[{\"port\":12000,\"protocol\":\"TCP\"}]}]}],\"k8sIngressList\":[{\"metadata\":{\"name\":\"cas-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"rules\":[{\"host\":\"202005-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"cas-manage01\",\"servicePort\":80},\"path\":\"/cas\"}]}}]}},{\"metadata\":{\"name\":\"customwebservice-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"rules\":[{\"host\":\"202005-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"customwebservice-manage01\",\"servicePort\":80},\"path\":\"/customwebservice-manage01\"}]}}]}},{\"metadata\":{\"name\":\"dcms-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"rules\":[{\"host\":\"202005-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"dcms-manage01\",\"servicePort\":80},\"path\":\"/dcms-manage01\"}]}}]}},{\"metadata\":{\"name\":\"dcmsrecord-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"rules\":[{\"host\":\"202005-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"dcmsrecord-manage01\",\"servicePort\":80},\"path\":\"/dcmsrecord-manage01\"}]}}]}},{\"metadata\":{\"name\":\"dcmssg-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"rules\":[{\"host\":\"202005-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"dcmssg-manage01\",\"servicePort\":80},\"path\":\"/dcmssg-manage01\"}]}}]}},{\"metadata\":{\"name\":\"dcmsstatics-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"rules\":[{\"host\":\"202005-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"dcmsstatics-manage01\",\"servicePort\":80},\"path\":\"/dcmsstatics-manage01\"}]}}]}},{\"metadata\":{\"name\":\"dcmsstaticsreport-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"rules\":[{\"host\":\"202005-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"dcmsstaticsreport-manage01\",\"servicePort\":80},\"path\":\"/dcmsstaticsreport-manage01\"}]}}]}},{\"metadata\":{\"name\":\"dcmswebservice-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"rules\":[{\"host\":\"202005-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"dcmswebservice-manage01\",\"servicePort\":80},\"path\":\"/dcmswebservice-manage01\"}]}}]}},{\"metadata\":{\"name\":\"dcmsx-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"rules\":[{\"host\":\"202005-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"dcmsx-manage01\",\"servicePort\":80},\"path\":\"/dcmsx-manage01\"}]}}]}},{\"metadata\":{\"name\":\"gls-ops01\",\"namespace\":\"just-test\"},\"spec\":{\"rules\":[{\"host\":\"202005-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"gls-ops01\",\"servicePort\":80},\"path\":\"/gls-ops01\"}]}}]}},{\"metadata\":{\"name\":\"safetymonitor-manage01\",\"namespace\":\"just-test\"},\"spec\":{\"rules\":[{\"host\":\"202005-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"safetymonitor-manage01\",\"servicePort\":80},\"path\":\"/safetymonitor-manage01\"}]}}]}}],\"k8sPVList\":[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolume\",\"metadata\":{\"name\":\"base-volume-just-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"capacity\":{\"storage\":\"1Gi\"},\"claimRef\":{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"name\":\"base-volume-just-test\",\"namespace\":\"just-test\"},\"nfs\":{\"path\":\"/home/kubernetes/volume/just-test/baseVolume\",\"server\":\"10.130.41.218\"},\"persistentVolumeReclaimPolicy\":\"Retain\",\"storageClassName\":\"base-volume-just-test\",\"volumeMode\":\"Filesystem\"}}],\"k8sPVCList\":[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"metadata\":{\"name\":\"base-volume-just-test\",\"namespace\":\"just-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"resources\":{\"requests\":{\"storage\":\"1Gi\"}},\"storageClassName\":\"base-volume-just-test\",\"volumeMode\":\"Filesystem\",\"volumeName\":\"base-volume-just-test\"}}],\"oracle\":{\"tag\":\"oracle\",\"deployment\":{\"metadata\":{\"labels\":{\"name\":\"oracle\",\"type\":\"ThreePartApp\",\"job-id\":\"c95bf0a00a\",\"tag\":\"oracle\"},\"name\":\"oracle\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"oracle\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"oracle\"}},\"spec\":{\"containers\":[{\"args\":[\"-c\",\"/tmp/init.sh 202005-test.ccod.io\"],\"command\":[\"/bin/bash\"],\"image\":\"nexus.io:5000/db/oracle-32-xe-10g:1.0\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"oracle\",\"ports\":[{\"containerPort\":1521,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"exec\":{\"command\":[\"cat\",\"/readiness\"]},\"failureThreshold\":1,\"periodSeconds\":1,\"successThreshold\":1,\"timeoutSeconds\":1},\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/tmp\",\"name\":\"sql\",\"subPath\":\"db/oracle/sql\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-just-test\"}}]}}}},\"services\":[{\"metadata\":{\"labels\":{\"service-type\":\"THREE_PART_APP\",\"alias\":\"oracle\",\"version\":\"10.1.0\",\"app-name\":\"oracle\"},\"name\":\"oracle\",\"namespace\":\"just-test\"},\"spec\":{\"externalTrafficPolicy\":\"Cluster\",\"ports\":[{\"port\":1521,\"protocol\":\"TCP\",\"targetPort\":1521}],\"selector\":{\"name\":\"oracle\"},\"type\":\"NodePort\"}}],\"ingresses\":[],\"timeout\":150},\"mysql\":{\"deployment\":{\"metadata\":{\"labels\":{\"name\":\"mysql-5-7-29\",\"type\":\"ThreePartApp\",\"job-id\":\"c95bf0a00a\",\"tag\":\"mysql-5-7-29\"},\"name\":\"mysql-5-7-29\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"mysql-5-7-29\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"mysql-5-7-29\"}},\"spec\":{\"containers\":[{\"args\":[\"--default_authentication_plugin\\u003dmysql_native_password\",\"--character-set-server\\u003dutf8mb4\",\"--collation-server\\u003dutf8mb4_unicode_ci\",\"--lower-case-table-names\\u003d1\"],\"env\":[{\"name\":\"MYSQL_ROOT_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_USER\",\"value\":\"ccod\"},{\"name\":\"MYSQL_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_DATABASE\",\"value\":\"ccod\"}],\"image\":\"nexus.io:5000/db/mysql:5.7.29\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"mysql-5-7-29\",\"ports\":[{\"containerPort\":3306,\"protocol\":\"TCP\"}],\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/docker-entrypoint-initdb.d/\",\"name\":\"sql\",\"subPath\":\"db/mysql/sql\"},{\"mountPath\":\"/var/lib/mysql/\",\"name\":\"sql\",\"subPath\":\"db/mysql/data\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-just-test\"}}]}}}},\"services\":[{\"metadata\":{\"labels\":{\"service-type\":\"THREE_PART_APP\",\"alias\":\"mysql\",\"version\":\"5.7.29\",\"app-name\":\"mysql\"},\"name\":\"mysql\",\"namespace\":\"just-test\"},\"spec\":{\"externalTrafficPolicy\":\"Cluster\",\"ports\":[{\"port\":3306,\"protocol\":\"TCP\",\"targetPort\":3306}],\"selector\":{\"name\":\"mysql-5-7-29\"},\"type\":\"NodePort\"}}],\"ingresses\":[],\"timeout\":0},\"licenseServer\":{\"deployment\":{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"licenseserver-public01\"},\"name\":\"licenseserver-public01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"public01\",\"LicenseServer-version\":\"5214\",\"LicenseServer\":\"licenseserver\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"LicenseServer-version\":\"5214\",\"LicenseServer\":\"licenseserver\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;cd /root/Platform/bin;./LicenseServer;sleep 5;tailf /root/Platform/log/*/*.log;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":17021},\"timeoutSeconds\":1},\"name\":\"licenseserver-runtime\",\"ports\":[{\"containerPort\":17021,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/LicenseServer /binary-file/\"],\"command\":[\"/bin/sh\",\"-c\",\"mkdir /binary-file/bin -p;mkdir /binary-file/log -p;mv /opt/LicenseServer /binary-file/bin/LicenseServer;mkdir /binary-file/bin/ -p;cp /cfg/licenseserver-cfg/Config.ini /binary-file/bin/Config.ini\"],\"image\":\"nexus.io:5000/ccod/licenseserver:5214\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"licenseserver\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/cfg/licenseserver-cfg\",\"name\":\"licenseserver-public01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/licenseserver-public01/licenseserver-public01\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"Config.ini\",\"path\":\"Config.ini\"}],\"name\":\"licenseserver-public01-c95bf0a00a\"},\"name\":\"licenseserver-public01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},\"services\":[{\"metadata\":{\"labels\":{\"name\":\"licenseserver-public01\",\"domain-id\":\"public01\",\"app-name\":\"LicenseServer\",\"alias\":\"licenseserver\",\"version\":\"5214\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"licenseserver-public01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"licenseserver-public01-17021\",\"port\":17021,\"protocol\":\"TCP\",\"targetPort\":17021}],\"selector\":{\"domain-id\":\"public01\",\"LicenseServer-version\":\"5214\",\"LicenseServer\":\"licenseserver\"},\"type\":\"ClusterIP\"}}],\"ingresses\":[],\"timeout\":0},\"glsserver\":{\"deployment\":{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"glsserver-public01\"},\"name\":\"glsserver-public01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"public01\",\"glsServer-version\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"glsServer\":\"glsserver\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"glsServer-version\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"glsServer\":\"glsserver\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;cd /root/Platform/bin;./Glsserver;sleep 5;tailf /root/Platform/log/*/*.log;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"lifecycle\":{\"preStop\":{\"exec\":{\"command\":[\"/bin/bash\",\"-c\",\"echo \\u0027gls stop ...\\u0027\"]}}},\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":600,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":17020},\"timeoutSeconds\":1},\"name\":\"glsserver-runtime\",\"ports\":[{\"containerPort\":17020,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/Glsserver /binary-file/\"],\"command\":[\"/bin/sh\",\"-c\",\"mkdir /binary-file/bin -p;mkdir /binary-file/log -p;mv /opt/Glsserver /binary-file/bin/Glsserver;mkdir /binary-file/cfg/ -p;cp /cfg/glsserver-cfg/gls_config.cfg /binary-file/cfg/gls_config.cfg;cp /cfg/glsserver-cfg/gls_logger.cfg /binary-file/cfg/gls_logger.cfg\"],\"image\":\"nexus.io:5000/ccod/glsserver:7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"glsserver\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/cfg/glsserver-cfg\",\"name\":\"glsserver-public01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/glsserver-public01/glsserver-public01\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"gls_config.cfg\",\"path\":\"gls_config.cfg\"},{\"key\":\"gls_logger.cfg\",\"path\":\"gls_logger.cfg\"}],\"name\":\"glsserver-public01-c95bf0a00a\"},\"name\":\"glsserver-public01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},\"services\":[{\"metadata\":{\"labels\":{\"name\":\"glsserver-public01\",\"domain-id\":\"public01\",\"app-name\":\"glsServer\",\"alias\":\"glsserver\",\"version\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"glsserver-public01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"glsserver-public01-17020\",\"port\":17020,\"protocol\":\"TCP\",\"targetPort\":17020}],\"selector\":{\"domain-id\":\"public01\",\"glsServer-version\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"glsServer\":\"glsserver\"},\"type\":\"ClusterIP\"}}],\"ingresses\":[],\"timeout\":0},\"ucds\":{\"deployment\":{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"ucds-cloud01\"},\"name\":\"ucds-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"cloud01\",\"UCDServer-version\":\"deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\",\"UCDServer\":\"ucds\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"UCDServer-version\":\"deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\",\"UCDServer\":\"ucds\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;cd /root/Platform/bin;./UCDServer;sleep 5;tailf /root/Platform/log/*/*.log;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":12003},\"timeoutSeconds\":1},\"name\":\"ucds-runtime\",\"ports\":[{\"containerPort\":12003,\"protocol\":\"TCP\"},{\"containerPort\":60001,\"protocol\":\"TCP\"},{\"containerPort\":17002,\"protocol\":\"TCP\"},{\"containerPort\":17004,\"protocol\":\"TCP\"},{\"containerPort\":17009,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/UCDServer /binary-file/\"],\"command\":[\"/bin/sh\",\"-c\",\"mkdir /binary-file/bin -p;mkdir /binary-file/log -p;mv /opt/UCDServer /binary-file/bin/UCDServer;mkdir /binary-file/cfg/ -p;cp /cfg/ucds-cfg/ucds_config.cfg /binary-file/cfg/ucds_config.cfg;cp /cfg/ucds-cfg/ucds_logger.cfg /binary-file/cfg/ucds_logger.cfg;cp /cfg/ucds-cfg/DRWRClient.cfg /binary-file/cfg/DRWRClient.cfg\"],\"image\":\"nexus.io:5000/ccod/ucdserver:deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"ucds\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/cfg/ucds-cfg\",\"name\":\"ucds-cloud01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/ucds-cloud01/ucds-cloud01\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"DRWRClient.cfg\",\"path\":\"DRWRClient.cfg\"},{\"key\":\"ucds_config.cfg\",\"path\":\"ucds_config.cfg\"},{\"key\":\"ucds_logger.cfg\",\"path\":\"ucds_logger.cfg\"}],\"name\":\"ucds-cloud01-c95bf0a00a\"},\"name\":\"ucds-cloud01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},\"services\":[{\"metadata\":{\"labels\":{\"name\":\"ucds-cloud01\",\"domain-id\":\"cloud01\",\"app-name\":\"UCDServer\",\"alias\":\"ucds\",\"version\":\"deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"ucds-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"ucds-cloud01-12003\",\"port\":12003,\"protocol\":\"TCP\",\"targetPort\":12003},{\"name\":\"ucds-cloud01-17009\",\"port\":17009,\"protocol\":\"TCP\",\"targetPort\":17009},{\"name\":\"ucds-cloud01-17002\",\"port\":17002,\"protocol\":\"TCP\",\"targetPort\":17002},{\"name\":\"ucds-cloud01-60001\",\"port\":60001,\"protocol\":\"TCP\",\"targetPort\":60001},{\"name\":\"ucds-cloud01-17004\",\"port\":17004,\"protocol\":\"TCP\",\"targetPort\":17004}],\"selector\":{\"domain-id\":\"cloud01\",\"UCDServer-version\":\"deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\",\"UCDServer\":\"ucds\"},\"type\":\"ClusterIP\"}},{\"metadata\":{\"labels\":{\"name\":\"ucds-cloud01-out\",\"domain-id\":\"cloud01\",\"app-name\":\"UCDServer\",\"alias\":\"ucds\",\"version\":\"deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"ucds-cloud01-out\",\"namespace\":\"just-test\"},\"spec\":{\"externalTrafficPolicy\":\"Cluster\",\"ports\":[{\"name\":\"ucds-cloud01-12003\",\"port\":12003,\"protocol\":\"TCP\",\"targetPort\":12003}],\"selector\":{\"domain-id\":\"cloud01\",\"UCDServer-version\":\"deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\",\"UCDServer\":\"ucds\"},\"type\":\"NodePort\"}}],\"ingresses\":[],\"timeout\":0},\"dcs\":{\"deployment\":{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"type\":\"CCODDomainModule\",\"job-id\":\"c95bf0a00a\",\"tag\":\"dcs-cloud01\"},\"name\":\"dcs-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"cloud01\",\"dcs\":\"dcs\",\"dcs-version\":\"155-21974\"}},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"cloud01\",\"dcs\":\"dcs\",\"dcs-version\":\"155-21974\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir /root/resin-4.0.13/conf -p;cp /cfg/just-test/local_datasource.xml /root/resin-4.0.13/conf/local_datasource.xml;cp /cfg/just-test/local_jvm.xml /root/resin-4.0.13/conf/local_jvm.xml;mkdir /usr/local/lib -p;cp /cfg/just-test/tnsnames.ora /usr/local/lib/tnsnames.ora;cd /root/Platform/bin;./dcs;sleep 5;tailf /root/Platform/log/*/*.log;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":18070},\"timeoutSeconds\":1},\"name\":\"dcs-runtime\",\"ports\":[{\"containerPort\":18070,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"},{\"mountPath\":\"/cfg/just-test\",\"name\":\"just-test-volume\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/dcs /binary-file/\"],\"command\":[\"/bin/sh\",\"-c\",\"mkdir /binary-file/bin -p;mkdir /binary-file/log -p;mv /opt/dcs /binary-file/bin/dcs;mkdir /binary-file/cfg/ -p;cp /cfg/dcs-cfg/dc_log4cpp.cfg /binary-file/cfg/dc_log4cpp.cfg;cp /cfg/dcs-cfg/DCServer.cfg /binary-file/cfg/DCServer.cfg\"],\"image\":\"nexus.io:5000/ccod/dcs:155-21974\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"dcs\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/cfg/dcs-cfg\",\"name\":\"dcs-cloud01-c95bf0a00a-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/just-test/dcs-cloud01/dcs-cloud01\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"DCServer.cfg\",\"path\":\"DCServer.cfg\"},{\"key\":\"dc_log4cpp.cfg\",\"path\":\"dc_log4cpp.cfg\"}],\"name\":\"dcs-cloud01-c95bf0a00a\"},\"name\":\"dcs-cloud01-c95bf0a00a-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"just-test\"},\"name\":\"just-test-volume\"}]}}}},\"services\":[{\"metadata\":{\"labels\":{\"name\":\"dcs-cloud01\",\"domain-id\":\"cloud01\",\"app-name\":\"dcs\",\"alias\":\"dcs\",\"version\":\"155-21974\",\"service-type\":\"DOMAIN_SERVICE\"},\"name\":\"dcs-cloud01\",\"namespace\":\"just-test\"},\"spec\":{\"ports\":[{\"name\":\"dcs-cloud01-18070\",\"port\":18070,\"protocol\":\"TCP\",\"targetPort\":18070}],\"selector\":{\"domain-id\":\"cloud01\",\"dcs\":\"dcs\",\"dcs-version\":\"155-21974\"},\"type\":\"ClusterIP\"}}],\"ingresses\":[],\"timeout\":0}}";
        try {
            PlatformUpdateSchemaInfo oldSchema = generateDemoCreateSchema("202005-test", "just-test");
            K8sPlatformSchemaInfo schemaInfo = getSchemaFromOldSchema(oldSchema);
            String str = gson.toJson(schemaInfo);
            System.out.println(str);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
//        String ip = "10.130.41.218";
//        int port = 32076;
//        String sid = "xe";
//        String user = "ccod";
//        String pwd = "ccod";
//        oracleConnectTest(user, pwd, ip, port, sid, 150);
    }


    private void portTest() throws Exception {
        V1Service service = this.k8sApiService.readNamespacedService("oracle", "k8s-test", this.testK8sApiUrl, this.testAuthToken);
        int port = getNodePortFromK8sService(service);
        System.out.println(port);
    }
}