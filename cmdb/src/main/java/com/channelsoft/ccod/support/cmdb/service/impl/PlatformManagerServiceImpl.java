package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.config.AppDefine;
import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.config.CCODBiz;
import com.channelsoft.ccod.support.cmdb.config.ImageCfg;
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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
 * @ClassName: PlatformManagerServiceImpl
 * @Author: lanhb
 * @Description: 实现IPlatformManagerService接口的服务类
 * @Date: 2020/5/28 9:54
 * @Version: 1.0
 */
@Service
public class PlatformManagerServiceImpl implements IPlatformManagerService {

    private final static Logger logger = LoggerFactory.getLogger(PlatformManagerServiceImpl.class);

    private Gson gson = new Gson();

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

    private final Map<String, PlatformUpdateSchemaInfo> platformUpdateSchemaMap = new ConcurrentHashMap<>();

    private Map<String, List<BizSetDefine>> appSetRelationMap;

    private Map<String, BizSetDefine> setDefineMap;

    private boolean isPlatformCheckOngoing = false;

    private String domainIdRegexFmt = "^%s(0[1-9]|[1-9]\\d+)";

    private String aliasRegexFmt = "^%s\\d*$";

    protected String testK8sApiUrl = "https://10.130.41.218:6443";

    protected String testAuthToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA";

    @PostConstruct
    void init() throws Exception
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
        String authToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA";
        String k8sApiUrl = "https://10.130.41.218:6443";
        String namespace = "clone-test";
        try {
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
            PlatformUpdateSchemaInfo schema = generateDemoCreateSchema("202005-test");
//            schema.setPlatformId("k8s-platform-test");
//            String jsonStr = "{\"schemaId\":\"e2b849bd-0ac4-4591-97f3-eee6e7084a58\",\"domainUpdatePlanList\":[{\"domainName\":\"公共组件01\",\"domainId\":\"public01\",\"bkSetName\":\"公共组件\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"glsserver-public01\",\"appName\":\"glsServer\",\"appAlias\":\"glsserver\",\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/home/ccodrunner/Platform\",\"appRunner\":\"glsserver\",\"cfgs\":[{\"fileName\":\"gls_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"f23a83a2d871d59c89d12b0281e10e90\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_glsserver/gls_config.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmMmRiNjc0YzQ5YmE4Nzdj\"},{\"fileName\":\"gls_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_glsserver/gls_logger.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFhYTlmZWY0MTJkMDY2ZTM3\"}],\"addDelay\":30}],\"updateType\":\"ADD\",\"comment\":\"clone from 公共组件01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"管理门户01\",\"domainId\":\"manage01\",\"bkSetName\":\"管理门户\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"assembleTag\":\"cas-manage01\",\"appName\":\"cas\",\"appAlias\":\"cas\",\"targetVersion\":\"10973\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/home/portal/tomcat/webapps/\",\"appRunner\":\"cas\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\",\"deployPath\":\"./cas/WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_cas/web.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE2MDA0MWExZjQ1ODc4Njhh\"},{\"fileName\":\"cas.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"6622e01a4df917d747e078e89c774a52\",\"deployPath\":\"./cas/WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_cas/cas.properties\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3N2E3NDBhYmM0NzU2NDg5\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"comment\":\"clone from 管理门户01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"}],\"platformId\":\"k8s-test\",\"platformName\":\"工具组平台\",\"bkBizId\":34,\"bkCloudId\":0,\"ccodVersion\":\"CCOD4.1\",\"taskType\":\"CREATE\",\"title\":\"新建工具组平台(202005-test)计划\",\"comment\":\"create 工具组平台(202005-test) by clone 123456-wuph(ccod开发测试平台)\",\"k8sHostIp\":\"10.130.41.218\",\"glsDBType\":\"ORACLE\",\"glsDBUser\":\"ccod\",\"glsDBPwd\":\"ccod\",\"baseDataNexusRepository\":\"platform_base_data\",\"baseDataNexusPath\":\"ccod/4.1/baseVolume.zip\",\"publicConfig\":[{\"fileName\":\"local_datasource.xml\",\"fileSize\":0,\"md5\":\"112940181aeb983baa9d7fd2733f194f\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_datasource.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmNmE4MWY3MWI3MmJlY2Ji\"},{\"fileName\":\"local_jvm.xml\",\"fileSize\":0,\"md5\":\"d172a5321944aba5bc19c35d00950afc\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_jvm.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRjMWQyMTIwNWRmYmY1MDM0\"},{\"fileName\":\"tnsnames.ora\",\"fileSize\":0,\"md5\":\"811f7f9472d5f6e733d732619a17ac77\",\"deployPath\":\"/usr/local/lib\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/tnsnames.ora\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY0NzFkOTU3MGMyODRjMGJm\"}],\"k8sApiUrl\":\"https://10.130.41.218:6443\",\"k8sAuthToken\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA\",\"k8sDeploymentList\":[{\"metadata\":{\"labels\":{\"name\":\"mysql-5-7-29\",\"type\":\"ThreePartApp\"},\"name\":\"mysql-5-7-29\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"mysql-5-7-29\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"mysql-5-7-29\"}},\"spec\":{\"containers\":[{\"args\":[\"--default_authentication_plugin\\u003dmysql_native_password\",\"--character-set-server\\u003dutf8mb4\",\"--collation-server\\u003dutf8mb4_unicode_ci\",\"--lower-case-table-names\\u003d1\"],\"env\":[{\"name\":\"MYSQL_ROOT_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_USER\",\"value\":\"ccod\"},{\"name\":\"MYSQL_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_DATABASE\",\"value\":\"ccod\"}],\"image\":\"nexus.io:5000/db/mysql:5.7.29\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"mysql-5-7-29\",\"ports\":[{\"containerPort\":3306,\"protocol\":\"TCP\"}],\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/docker-entrypoint-initdb.d/\",\"name\":\"sql\",\"subPath\":\"db/mysql/sql\"},{\"mountPath\":\"/var/lib/mysql/\",\"name\":\"sql\",\"subPath\":\"db/mysql/data\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-k8s-test\"}}]}}}},{\"metadata\":{\"labels\":{\"name\":\"oracle\",\"type\":\"ThreePartApp\"},\"name\":\"oracle\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"oracle\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"oracle\"}},\"spec\":{\"containers\":[{\"args\":[\"-c\",\"/tmp/init.sh 202005-test.ccod.io\"],\"command\":[\"/bin/bash\"],\"image\":\"nexus.io:5000/db/oracle-32-xe-10g:1.0\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"oracle\",\"ports\":[{\"containerPort\":1521,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"exec\":{\"command\":[\"cat\",\"/readiness\"]},\"failureThreshold\":1,\"periodSeconds\":1,\"successThreshold\":1,\"timeoutSeconds\":1},\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/tmp\",\"name\":\"sql\",\"subPath\":\"db/oracle/sql\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-k8s-test\"}}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\"},\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"cas\":\"cas\"}},\"strategy\":{\"type\":\"Recreate\"},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"cas\":\"cas\"}},\"spec\":{\"containers\":[{\"command\":[\"/bin/sh\",\"-c\",\"keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh; tail -F /usr/local/tomcat/logs/catalina.out\"],\"image\":\"nexus.io:5000/ccod-base/tomcat6-jre7:1\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/cas\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"cas-manage01\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"command\":[\"/bin/sh\",\"-c\",\"cd /opt;jar uf cas.war WEB-INF/cas.properties;jar uf cas.war WEB-INF/web.xml\"],\"image\":\"nexus.io:5000/ccod/cas:10973\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"cas\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"},{\"mountPath\":\"/opt/WEB-INF/classes\",\"name\":\"cas-manage01-volume\"},{\"mountPath\":\"/opt/WEB-INF\",\"name\":\"k8s-test-volume\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/k8s-test/cas-manage01/cas-manage01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"configMap\":{\"items\":[{\"key\":\"cas.properties\",\"path\":\"cas.properties\"},{\"key\":\"web.xml\",\"path\":\"web.xml\"}],\"name\":\"cas-manage01\"},\"name\":\"cas-manage01-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"k8s-test\"},\"name\":\"k8s-test-volume\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"type\":\"CCODDomainModule\"},\"name\":\"glsserver-public01\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"}},\"strategy\":{\"type\":\"Recreate\"},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir -p /root/Platform/bin;cd /root/Platform; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_config.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_logger.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/publicConfig/tnsnames.ora -P /usr/local/lib -N; cd /root/Platform/bin/;/root/Platform/bin/Glsserver;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"lifecycle\":{\"preStop\":{\"exec\":{\"command\":[\"/bin/bash\",\"-c\",\"echo \\u0027gls stop ...\\u0027\"]}}},\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":600,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":17020},\"timeoutSeconds\":1},\"name\":\"glsserver-public01\",\"ports\":[{\"containerPort\":17020,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform/bin\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/Glsserver /binary-file/;\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/ccod/glsserver:7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"glsserver\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"},{\"mountPath\":\"/home/ccodrunner/Platform/cfg/\",\"name\":\"glsserver-public01-volume\"},{\"mountPath\":\"/root/Platform/bin\",\"name\":\"k8s-test-volume\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/k8s-test/glsserver-public01/glsserver-public01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"},{\"configMap\":{\"items\":[{\"key\":\"gls_config.cfg\",\"path\":\"gls_config.cfg\"},{\"key\":\"gls_logger.cfg\",\"path\":\"gls_logger.cfg\"}],\"name\":\"glsserver-public01\"},\"name\":\"glsserver-public01-volume\"},{\"configMap\":{\"items\":[{\"key\":\"local_datasource.xml\",\"path\":\"local_datasource.xml\"},{\"key\":\"local_jvm.xml\",\"path\":\"local_jvm.xml\"},{\"key\":\"tnsnames.ora\",\"path\":\"tnsnames.ora\"}],\"name\":\"k8s-test\"},\"name\":\"k8s-test-volume\"}]}}}}],\"k8sServiceList\":[{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"},\"name\":\"glsserver-public01\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"name\":\"glsserver-public01-17020\",\"port\":17020,\"protocol\":\"TCP\",\"targetPort\":17020}],\"selector\":{\"name\":\"glsserver-public01\"},\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"cas\":\"cas\"},\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"name\":\"cas-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"name\":\"cas-manage01\"},\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"name\":\"mysql\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"port\":3306,\"protocol\":\"TCP\",\"targetPort\":3306}],\"selector\":{\"name\":\"mysql-5-7-29\"},\"type\":\"NodePort\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"name\":\"oracle\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"port\":1521,\"protocol\":\"TCP\",\"targetPort\":1521}],\"selector\":{\"name\":\"oracle\"},\"type\":\"NodePort\"}}],\"k8sEndpointsList\":[{\"apiVersion\":\"v1\",\"kind\":\"Endpoints\",\"metadata\":{\"labels\":{\"name\":\"cas-manage01\"},\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.6\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"cas-manage01-5b8599cf8d-qqkqz\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"cas-manage01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"apiVersion\":\"v1\",\"kind\":\"Endpoints\",\"metadata\":{\"labels\":{\"name\":\"glsserver-public01\"},\"name\":\"glsserver-public01\",\"namespace\":\"202005-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.2\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"glsserver-public01-69d8868fb-sm5mm\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"glsserver-public01-17020\",\"port\":17020,\"protocol\":\"TCP\"}]}]}],\"k8sIngressList\":[{\"apiVersion\":\"extensions/v1beta1\",\"kind\":\"Ingress\",\"metadata\":{\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"spec\":{\"rules\":[{\"host\":\"k8s-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"cas-manage01\",\"servicePort\":80},\"path\":\"/cas\"}]}}]}}],\"k8sPVList\":[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolume\",\"metadata\":{\"name\":\"base-volume-k8s-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"capacity\":{\"storage\":\"1Gi\"},\"claimRef\":{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"name\":\"base-volume-k8s-test\",\"namespace\":\"k8s-test\"},\"nfs\":{\"path\":\"/home/kubernetes/volume/k8s-test/baseVolume\",\"server\":\"10.130.41.218\"},\"persistentVolumeReclaimPolicy\":\"Retain\",\"storageClassName\":\"base-volume-k8s-test\",\"volumeMode\":\"Filesystem\"}}],\"k8sPVCList\":[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"metadata\":{\"name\":\"base-volume-k8s-test\",\"namespace\":\"k8s-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"resources\":{\"requests\":{\"storage\":\"1Gi\"}},\"storageClassName\":\"base-volume-k8s-test\",\"volumeMode\":\"Filesystem\",\"volumeName\":\"base-volume-k8s-test\"}}]}";
            String jsonStr = "{\"schemaId\":\"e2b849bd-0ac4-4591-97f3-eee6e7084a58\",\"domainUpdatePlanList\":[{\"domainName\":\"公共组件01\",\"domainId\":\"public01\",\"bkSetName\":\"公共组件\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"LicenseServer\",\"appAlias\":\"licenseserver\",\"targetVersion\":\"5214\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"licenseserver\",\"assembleTag\":\"glsserver-public01\",\"cfgs\":[{\"fileName\":\"Config.ini\",\"ext\":\"ini\",\"fileSize\":0,\"md5\":\"6c513269c4e2bc10f4a6cf0eb05e5bfc\",\"deployPath\":\"./license/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_licenseserver/Config.ini\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNlZGYyZTBkYjgyNWQ0OTRi\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"comment\":\"clone from 公共组件01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"管理门户01\",\"domainId\":\"manage01\",\"bkSetName\":\"管理门户\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"cas\",\"appAlias\":\"cas\",\"targetVersion\":\"10973\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"cas\",\"assembleTag\":\"cas-manage01\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_cas/web.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE2MDA0MWExZjQ1ODc4Njhh\"},{\"fileName\":\"cas.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"6622e01a4df917d747e078e89c774a52\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_cas/cas.properties\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3N2E3NDBhYmM0NzU2NDg5\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"comment\":\"clone from 管理门户01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"}],\"platformId\":\"k8s-test\",\"platformName\":\"工具组平台\",\"bkBizId\":34,\"bkCloudId\":0,\"ccodVersion\":\"CCOD4.1\",\"taskType\":\"CREATE\",\"title\":\"新建工具组平台(202005-test)计划\",\"comment\":\"create 工具组平台(202005-test) by clone 123456-wuph(ccod开发测试平台)\",\"k8sHostIp\":\"10.130.41.218\",\"glsDBType\":\"ORACLE\",\"glsDBUser\":\"ccod\",\"glsDBPwd\":\"ccod\",\"baseDataNexusRepository\":\"platform_base_data\",\"baseDataNexusPath\":\"ccod/4.1/baseVolume.zip\",\"publicConfig\":[{\"fileName\":\"local_datasource.xml\",\"fileSize\":0,\"md5\":\"112940181aeb983baa9d7fd2733f194f\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_datasource.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmNmE4MWY3MWI3MmJlY2Ji\"},{\"fileName\":\"local_jvm.xml\",\"fileSize\":0,\"md5\":\"d172a5321944aba5bc19c35d00950afc\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_jvm.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRjMWQyMTIwNWRmYmY1MDM0\"},{\"fileName\":\"tnsnames.ora\",\"fileSize\":0,\"md5\":\"811f7f9472d5f6e733d732619a17ac77\",\"deployPath\":\"/usr/local/lib\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/tnsnames.ora\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY0NzFkOTU3MGMyODRjMGJm\"}],\"k8sApiUrl\":\"https://10.130.41.218:6443\",\"k8sAuthToken\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA\",\"k8sDeploymentList\":[{\"metadata\":{\"labels\":{\"name\":\"mysql-5-7-29\",\"type\":\"ThreePartApp\"},\"name\":\"mysql-5-7-29\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"mysql-5-7-29\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"mysql-5-7-29\"}},\"spec\":{\"containers\":[{\"args\":[\"--default_authentication_plugin\\u003dmysql_native_password\",\"--character-set-server\\u003dutf8mb4\",\"--collation-server\\u003dutf8mb4_unicode_ci\",\"--lower-case-table-names\\u003d1\"],\"env\":[{\"name\":\"MYSQL_ROOT_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_USER\",\"value\":\"ccod\"},{\"name\":\"MYSQL_PASSWORD\",\"value\":\"ccod\"},{\"name\":\"MYSQL_DATABASE\",\"value\":\"ccod\"}],\"image\":\"nexus.io:5000/db/mysql:5.7.29\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"mysql-5-7-29\",\"ports\":[{\"containerPort\":3306,\"protocol\":\"TCP\"}],\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/docker-entrypoint-initdb.d/\",\"name\":\"sql\",\"subPath\":\"db/mysql/sql\"},{\"mountPath\":\"/var/lib/mysql/\",\"name\":\"sql\",\"subPath\":\"db/mysql/data\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-k8s-test\"}}]}}}},{\"metadata\":{\"labels\":{\"name\":\"oracle\",\"type\":\"ThreePartApp\"},\"name\":\"oracle\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"name\":\"oracle\"}},\"template\":{\"metadata\":{\"labels\":{\"name\":\"oracle\"}},\"spec\":{\"containers\":[{\"args\":[\"-c\",\"/tmp/init.sh 202005-test.ccod.io\"],\"command\":[\"/bin/bash\"],\"image\":\"nexus.io:5000/db/oracle-32-xe-10g:1.0\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"oracle\",\"ports\":[{\"containerPort\":1521,\"protocol\":\"TCP\"}],\"readinessProbe\":{\"exec\":{\"command\":[\"cat\",\"/readiness\"]},\"failureThreshold\":1,\"periodSeconds\":1,\"successThreshold\":1,\"timeoutSeconds\":1},\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/tmp\",\"name\":\"sql\",\"subPath\":\"db/oracle/sql\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"name\":\"sql\",\"persistentVolumeClaim\":{\"claimName\":\"base-volume-k8s-test\"}}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\"},\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"manage01\",\"cas\":\"cas\"}},\"strategy\":{\"type\":\"Recreate\"},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"cas\":\"cas\"}},\"spec\":{\"containers\":[{\"command\":[\"/bin/sh\",\"-c\",\"keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh; tail -F /usr/local/tomcat/logs/catalina.out\"],\"image\":\"nexus.io:5000/ccod-base/tomcat6-jre7:1\",\"imagePullPolicy\":\"IfNotPresent\",\"livenessProbe\":{\"failureThreshold\":3,\"httpGet\":{\"path\":\"/cas\",\"port\":8080,\"scheme\":\"HTTP\"},\"initialDelaySeconds\":30,\"periodSeconds\":30,\"successThreshold\":1,\"timeoutSeconds\":1},\"name\":\"cas-manage01\",\"ports\":[{\"containerPort\":8080,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1500Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"1000Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"}]}],\"hostAliases\":[{\"hostnames\":[\"202005-test.ccod.com\"],\"ip\":\"10.130.41.218\"}],\"initContainers\":[{\"command\":[\"/bin/sh\",\"-c\",\"\"],\"image\":\"nexus.io:5000/ccod/cas:10973\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"cas\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"}]}],\"terminationGracePeriodSeconds\":0,\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"name\":\"ssl\",\"secret\":{\"defaultMode\":420,\"secretName\":\"ssl\"}},{\"hostPath\":{\"path\":\"/var/ccod-runtime/k8s-test/cas-manage01/cas-manage01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"}]}}}},{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"type\":\"CCODDomainModule\"},\"name\":\"glsserver-public01\",\"namespace\":\"k8s-test\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":5,\"selector\":{\"matchLabels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"}},\"strategy\":{\"type\":\"Recreate\"},\"template\":{\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"}},\"spec\":{\"containers\":[{\"args\":[\"mkdir -p /root/Platform/bin;cd /root/Platform; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_config.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_logger.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/publicConfig/tnsnames.ora -P /usr/local/lib -N; cd /root/Platform/bin/;/root/Platform/bin/Glsserver;\"],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"imagePullPolicy\":\"IfNotPresent\",\"lifecycle\":{\"preStop\":{\"exec\":{\"command\":[\"/bin/bash\",\"-c\",\"echo \\u0027gls stop ...\\u0027\"]}}},\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":600,\"periodSeconds\":30,\"successThreshold\":1,\"tcpSocket\":{\"port\":17020},\"timeoutSeconds\":1},\"name\":\"glsserver-public01\",\"ports\":[{\"containerPort\":17020,\"protocol\":\"TCP\"}],\"resources\":{\"limits\":{\"cpu\":\"1\",\"memory\":\"1000Mi\"},\"requests\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}},\"volumeMounts\":[{\"mountPath\":\"/root/Platform/bin\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"}],\"workingDir\":\"/root/Platform\"}],\"initContainers\":[{\"args\":[\"cp /opt/Glsserver /binary-file/;\"],\"command\":[\"/bin/sh\",\"-c\"],\"image\":\"nexus.io:5000/ccod/glsserver:7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"imagePullPolicy\":\"IfNotPresent\",\"name\":\"glsserver\",\"resources\":{},\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"}],\"workingDir\":\"/opt\"}],\"terminationGracePeriodSeconds\":20,\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/k8s-test/glsserver-public01/glsserver-public01/\",\"type\":\"\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/core\",\"type\":\"\"},\"name\":\"core\"}]}}}}],\"k8sServiceList\":[{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"domain-id\":\"public01\",\"glsServer\":\"glsserver\"},\"name\":\"glsserver-public01\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"name\":\"glsserver-public01-17020\",\"port\":17020,\"protocol\":\"TCP\",\"targetPort\":17020}],\"selector\":{\"name\":\"glsserver-public01\"},\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"labels\":{\"domain-id\":\"manage01\",\"cas\":\"cas\"},\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"name\":\"cas-manage01\",\"port\":80,\"protocol\":\"TCP\",\"targetPort\":8080}],\"selector\":{\"name\":\"cas-manage01\"},\"type\":\"ClusterIP\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"name\":\"mysql\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"port\":3306,\"protocol\":\"TCP\",\"targetPort\":3306}],\"selector\":{\"name\":\"mysql-5-7-29\"},\"type\":\"NodePort\"}},{\"apiVersion\":\"v1\",\"kind\":\"Service\",\"metadata\":{\"name\":\"oracle\",\"namespace\":\"k8s-test\"},\"spec\":{\"ports\":[{\"port\":1521,\"protocol\":\"TCP\",\"targetPort\":1521}],\"selector\":{\"name\":\"oracle\"},\"type\":\"NodePort\"}}],\"k8sEndpointsList\":[{\"apiVersion\":\"v1\",\"kind\":\"Endpoints\",\"metadata\":{\"labels\":{\"name\":\"cas-manage01\"},\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.6\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"cas-manage01-5b8599cf8d-qqkqz\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"cas-manage01\",\"port\":8080,\"protocol\":\"TCP\"}]}]},{\"apiVersion\":\"v1\",\"kind\":\"Endpoints\",\"metadata\":{\"labels\":{\"name\":\"glsserver-public01\"},\"name\":\"glsserver-public01\",\"namespace\":\"202005-test\"},\"subsets\":[{\"addresses\":[{\"ip\":\"192.168.104.2\",\"nodeName\":\"node2\",\"targetRef\":{\"kind\":\"Pod\",\"name\":\"glsserver-public01-69d8868fb-sm5mm\",\"namespace\":\"202005-test\"}}],\"ports\":[{\"name\":\"glsserver-public01-17020\",\"port\":17020,\"protocol\":\"TCP\"}]}]}],\"k8sIngressList\":[{\"apiVersion\":\"extensions/v1beta1\",\"kind\":\"Ingress\",\"metadata\":{\"name\":\"cas-manage01\",\"namespace\":\"k8s-test\"},\"spec\":{\"rules\":[{\"host\":\"k8s-test.ccod.com\",\"http\":{\"paths\":[{\"backend\":{\"serviceName\":\"cas-manage01\",\"servicePort\":80},\"path\":\"/cas\"}]}}]}}],\"k8sPVList\":[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolume\",\"metadata\":{\"name\":\"base-volume-k8s-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"capacity\":{\"storage\":\"1Gi\"},\"claimRef\":{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"name\":\"base-volume-k8s-test\",\"namespace\":\"k8s-test\"},\"nfs\":{\"path\":\"/home/kubernetes/volume/k8s-test/baseVolume\",\"server\":\"10.130.41.218\"},\"persistentVolumeReclaimPolicy\":\"Retain\",\"storageClassName\":\"base-volume-k8s-test\",\"volumeMode\":\"Filesystem\"}}],\"k8sPVCList\":[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"metadata\":{\"name\":\"base-volume-k8s-test\",\"namespace\":\"k8s-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"resources\":{\"requests\":{\"storage\":\"1Gi\"}},\"storageClassName\":\"base-volume-k8s-test\",\"volumeMode\":\"Filesystem\",\"volumeName\":\"base-volume-k8s-test\"}}]}";
//            String jsonStr = "{\"comment\":\"create by MANUAL\",\"platformFunc\":\"TEST\",\"platformId\":\"test29\",\"schemaId\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"k8sHostIp\":\"10.130.41.218\",\"taskType\":\"CREATE\",\"platformType\":\"K8S_CONTAINER\",\"deployScriptRepository\":\"some_test\",\"baseDataNexusPath\":\"ccod/4.1/baseVolume.zip\",\"title\":\"\\u5f00\\u53d1\\u7ec4\\u6d4b\\u8bd5\\u5e73\\u53f0[test29]\\u65b0\\u5efa\\u8ba1\\u5212\",\"bkCloudId\":8,\"publicConfig\":[],\"domainUpdatePlanList\":[{\"status\":\"CREATE\",\"domainId\":\"public01\",\"domainName\":\"\\u516c\\u5171\\u7ec4\\u4ef601\",\"updateType\":\"ADD\",\"tags\":\"\\u5165\\u547c\\u53eb,\\u5916\\u547c\",\"comment\":\"clone from \\u516c\\u5171\\u7ec4\\u4ef601 of pahj\",\"occurs\":600,\"bkSetName\":\"\\u516c\\u5171\\u7ec4\\u4ef6\",\"maxOccurs\":1000,\"appUpdateOperationList\":[{\"cfgs\":[{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/public01/10.130.29.111/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69/glsserver/20200327152037/gls_config.cfg\",\"fileName\":\"gls_config.cfg\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzowYWI4MGE3NDM5MjFlNDI2NTk3NmY5YWEyNDkzM2M5ZQ\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"609e331e9d5052a61de5e6b5addd5ce3\"},{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/public01/10.130.29.111/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69/glsserver/20200327152037/gls_logger.cfg\",\"fileName\":\"gls_logger.cfg\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzoxM2IyOWU0NDlmMGUzYjhkOTU5MTg0MDdhYTgzNzY0NQ\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\"}],\"appName\":\"glsServer\",\"platformAppId\":0,\"basePath\":\"/home/ccodrunner/Platform\",\"assembleTag\":\"separate01\",\"appRunner\":\"glsserver\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"assembleName\":\"glsserver1-public01\",\"addDelay\":0,\"appAlias\":\"glsserver1\"}]},{\"status\":\"CREATE\",\"domainId\":\"public02\",\"domainName\":\"\\u516c\\u5171\\u7ec4\\u4ef602\",\"updateType\":\"ADD\",\"tags\":\"\\u5165\\u547c\\u53eb,\\u5916\\u547c\",\"comment\":\"clone from \\u516c\\u5171\\u7ec4\\u4ef601 of pahj\",\"occurs\":600,\"bkSetName\":\"\\u516c\\u5171\\u7ec4\\u4ef6\",\"maxOccurs\":1000,\"appUpdateOperationList\":[{\"cfgs\":[{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/public01/10.130.29.111/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69/glsserver/20200327152037/gls_config.cfg\",\"fileName\":\"gls_config.cfg\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzowYWI4MGE3NDM5MjFlNDI2NTk3NmY5YWEyNDkzM2M5ZQ\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"609e331e9d5052a61de5e6b5addd5ce3\"},{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/public01/10.130.29.111/glsServer/7b699a4aece10ef28dce83ab36e4d79213ec4f69/glsserver/20200327152037/gls_logger.cfg\",\"fileName\":\"gls_logger.cfg\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzoxM2IyOWU0NDlmMGUzYjhkOTU5MTg0MDdhYTgzNzY0NQ\",\"ext\":\"cfg\",\"fileSize\":0,\"deployPath\":\"./cfg/\",\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\"}],\"appName\":\"glsServer\",\"platformAppId\":0,\"basePath\":\"/home/ccodrunner/Platform\",\"assembleTag\":\"separate01\",\"appRunner\":\"glsserver\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"assembleName\":\"glsserver2-public02\",\"addDelay\":0,\"appAlias\":\"glsserver2\"}]},{\"status\":\"CREATE\",\"domainId\":\"manage01\",\"domainName\":\"\\u7ba1\\u7406\\u95e8\\u623701\",\"updateType\":\"ADD\",\"tags\":\"\\u5165\\u547c\\u53eb,\\u5916\\u547c\",\"comment\":\"clone from \\u7ba1\\u7406\\u95e8\\u623701 of pahj\",\"occurs\":600,\"bkSetName\":\"\\u7ba1\\u7406\\u95e8\\u6237\",\"maxOccurs\":1000,\"appUpdateOperationList\":[{\"cfgs\":[{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/manage01/10.130.29.111/cas/10973/cas/20200327152226/cas.properties\",\"fileName\":\"cas.properties\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzpkNDgxMTc1NDFkY2I4OWVjMzA5NDAyMWQxZDRjYTM1Yw\",\"ext\":\"properties\",\"fileSize\":0,\"deployPath\":\"./cas/WEB-INF/\",\"md5\":\"1fe26a2aa7df9ca4d1173ef8bfef2a5c\"},{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/manage01/10.130.29.111/cas/10973/cas/20200327152226/web.xml\",\"fileName\":\"web.xml\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzoxM2IyOWU0NDlmMGUzYjhkM2YwNGQ5NDc5MTYwMzFkNg\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./cas/WEB-INF/\",\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\"}],\"appName\":\"cas\",\"platformAppId\":0,\"basePath\":\"/home/portal/tomcat/webapps/\",\"assembleTag\":\"base_01\",\"appRunner\":\"cas\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"targetVersion\":\"10973\",\"assembleName\":\"cas1-manage01\",\"addDelay\":0,\"appAlias\":\"cas1\"},{\"cfgs\":[{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/manage01/10.130.29.111/cas/10973/cas/20200327152226/cas.properties\",\"fileName\":\"cas.properties\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzpkNDgxMTc1NDFkY2I4OWVjMzA5NDAyMWQxZDRjYTM1Yw\",\"ext\":\"properties\",\"fileSize\":0,\"deployPath\":\"./cas/WEB-INF/\",\"md5\":\"1fe26a2aa7df9ca4d1173ef8bfef2a5c\"},{\"nexusRepository\":\"platform_app_cfg\",\"nexusPath\":\"pahj/manage01/10.130.29.111/cas/10973/cas/20200327152226/web.xml\",\"fileName\":\"web.xml\",\"nexusAssetId\":\"cGxhdGZvcm1fYXBwX2NmZzoxM2IyOWU0NDlmMGUzYjhkM2YwNGQ5NDc5MTYwMzFkNg\",\"ext\":\"xml\",\"fileSize\":0,\"deployPath\":\"./cas/WEB-INF/\",\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\"}],\"appName\":\"cas\",\"platformAppId\":0,\"basePath\":\"/home/portal/tomcat/webapps/\",\"assembleTag\":\"base_01\",\"appRunner\":\"cas\",\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"targetVersion\":\"10973\",\"assembleName\":\"cas2-manage01\",\"addDelay\":0,\"appAlias\":\"cas2\"}]}],\"k8sApiUrl\":\"https://10.130.41.218:6443\",\"platformName\":\"\\u5f00\\u53d1\\u7ec4\\u6d4b\\u8bd5\\u5e73\\u53f0\",\"k8sAuthToken\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA\",\"status\":\"CREATE\",\"k8sIngressList\":[{\"kind\":\"Ingress\",\"spec\":{\"rules\":[{\"host\":\"test29.ccod.com\",\"http\":{\"paths\":[{\"path\":\"/cas1-manage01\",\"backend\":{\"serviceName\":\"cas1-manage01\",\"servicePort\":80}}]}}]},\"apiVersion\":\"extensions/v1beta1\",\"metadata\":{\"labels\":{\"cas\":\"cas1\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"name\":\"cas1-manage01\"},\"namespace\":\"test29\",\"name\":\"cas1-manage01\"}},{\"kind\":\"Ingress\",\"spec\":{\"rules\":[{\"host\":\"test29.ccod.com\",\"http\":{\"paths\":[{\"path\":\"/cas2-manage01\",\"backend\":{\"serviceName\":\"cas2-manage01\",\"servicePort\":80}}]}}]},\"apiVersion\":\"extensions/v1beta1\",\"metadata\":{\"labels\":{\"cas\":\"cas2\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"name\":\"cas2-manage01\"},\"namespace\":\"test29\",\"name\":\"cas2-manage01\"}}],\"k8sServiceList\":[{\"kind\":\"Service\",\"spec\":{\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\",\"ports\":[{\"targetPort\":17020,\"protocol\":\"TCP\",\"name\":\"register\",\"port\":17020}],\"selector\":{\"name\":\"glsserver1-public01\"}},\"apiVersion\":\"v1\",\"metadata\":{\"labels\":{\"glsServer\":\"glsserver1\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"public01\",\"type\":\"CCODDomainModule\",\"name\":\"glsserver1-public01\"},\"namespace\":\"test29\",\"name\":\"glsserver1-public01\"}},{\"kind\":\"Service\",\"spec\":{\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\",\"ports\":[{\"targetPort\":17020,\"protocol\":\"TCP\",\"name\":\"register\",\"port\":17020}],\"selector\":{\"name\":\"glsserver2-public02\"}},\"apiVersion\":\"v1\",\"metadata\":{\"labels\":{\"glsServer\":\"glsserver2\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"public02\",\"type\":\"CCODDomainModule\",\"name\":\"glsserver2-public02\"},\"namespace\":\"test29\",\"name\":\"glsserver2-public02\"}},{\"kind\":\"Service\",\"spec\":{\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\",\"ports\":[{\"targetPort\":8080,\"protocol\":\"TCP\",\"name\":\"Null\",\"port\":80}],\"selector\":{\"name\":\"cas1-manage01\"}},\"apiVersion\":\"v1\",\"metadata\":{\"labels\":{\"cas\":\"cas1\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"name\":\"cas1-manage01\"},\"namespace\":\"test29\",\"name\":\"cas1-manage01\"}},{\"kind\":\"Service\",\"spec\":{\"sessionAffinity\":\"None\",\"type\":\"ClusterIP\",\"ports\":[{\"targetPort\":8080,\"protocol\":\"TCP\",\"name\":\"Null\",\"port\":80}],\"selector\":{\"name\":\"cas2-manage01\"}},\"apiVersion\":\"v1\",\"metadata\":{\"labels\":{\"cas\":\"cas2\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"name\":\"cas2-manage01\"},\"namespace\":\"test29\",\"name\":\"cas2-manage01\"}}],\"k8sDeploymentList\":[{\"kind\":\"Deployment\",\"spec\":{\"selector\":{\"matchLabels\":{\"glsServer\":\"glsserver1\",\"domain-id\":\"public01\",\"name\":\"glsserver1-public01\"}},\"replicas\":1,\"template\":{\"spec\":{\"terminationGracePeriodSeconds\":600,\"initContainers\":[{\"name\":\"glsserver1\",\"image\":\"nexus.io:5000/ccod/glsserver:7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"args\":[\"cp /opt/Glsserver /binary-file/;\"],\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"}],\"command\":[\"/bin/sh\",\"-c\"],\"imagePullPolicy\":\"IfNotPresent\",\"resources\":{\"requests\":{\"cpu\":\"100m\",\"memory\":\"100Mi\"},\"limits\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}}}],\"containers\":[{\"livenessProbe\":{\"tcpSocket\":{\"port\":\"17020\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"3\",\"periodSeconds\":\"30\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"name\":\"running\",\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"args\":[\"mkdir -p /root/Platform/bin;cd /root/Platform; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_config.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_logger.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/publicConfig/tnsnames.ora -P /usr/local/lib -N; cd /root/Platform/bin/;/root/Platform/bin/Glsserver;\"],\"volumeMounts\":[{\"mountPath\":\"/root/Platform/bin\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"}],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"imagePullPolicy\":\"IfNotPresent\",\"readinessProbe\":{\"tcpSocket\":{\"port\":\"17020\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"60\",\"periodSeconds\":\"10\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"ports\":[{\"protocol\":\"TCP\",\"containerPort\":\"17020\"}],\"resources\":{\"requests\":{\"cpu\":\"100m\",\"memory\":\"100Mi\"},\"limits\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}}}],\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/test29/public01/glsserver1\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"\"},\"name\":\"core\"}]},\"metadata\":{\"labels\":{\"glsServer\":\"glsserver1\",\"domain-id\":\"public01\",\"name\":\"glsserver1-public01\"}}},\"strategy\":{\"type\":\"Recreate\"}},\"apiVersion\":\"apps/v1\",\"metadata\":{\"labels\":{\"glsServer\":\"glsserver1\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"public01\",\"type\":\"CCODDomainModule\",\"name\":\"glsserver1-public01\"},\"namespace\":\"test29\",\"name\":\"glsserver1-public01\"}},{\"kind\":\"Deployment\",\"spec\":{\"selector\":{\"matchLabels\":{\"glsServer\":\"glsserver2\",\"domain-id\":\"public02\",\"name\":\"glsserver2-public02\"}},\"replicas\":1,\"template\":{\"spec\":{\"terminationGracePeriodSeconds\":600,\"initContainers\":[{\"name\":\"glsserver2\",\"image\":\"nexus.io:5000/ccod/glsserver:7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"args\":[\"cp /opt/Glsserver /binary-file/;\"],\"volumeMounts\":[{\"mountPath\":\"/binary-file\",\"name\":\"binary-file\"}],\"command\":[\"/bin/sh\",\"-c\"],\"imagePullPolicy\":\"IfNotPresent\",\"resources\":{\"requests\":{\"cpu\":\"100m\",\"memory\":\"100Mi\"},\"limits\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}}}],\"containers\":[{\"livenessProbe\":{\"tcpSocket\":{\"port\":\"17020\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"3\",\"periodSeconds\":\"30\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"name\":\"running\",\"image\":\"nexus.io:5000/ccod-base/centos-backend:0.4\",\"args\":[\"mkdir -p /root/Platform/bin;cd /root/Platform; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_config.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/public01_glsserver/gls_logger.cfg -P ./cfg -N; wget -t 3 http://10.130.41.218:8081/repository/tmp/configText/202005-test/publicConfig/tnsnames.ora -P /usr/local/lib -N; cd /root/Platform/bin/;/root/Platform/bin/Glsserver;\"],\"volumeMounts\":[{\"mountPath\":\"/root/Platform/bin\",\"name\":\"binary-file\"},{\"mountPath\":\"/root/Platform/log\",\"name\":\"ccod-runtime\"},{\"mountPath\":\"/ccod-core\",\"name\":\"core\"}],\"command\":[\"/bin/sh\",\"-c\"],\"env\":[{\"name\":\"LD_LIBRARY_PATH\",\"value\":\"/usr/local/lib/:/usr/lib/\"}],\"imagePullPolicy\":\"IfNotPresent\",\"readinessProbe\":{\"tcpSocket\":{\"port\":\"17020\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"60\",\"periodSeconds\":\"10\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"ports\":[{\"protocol\":\"TCP\",\"containerPort\":\"17020\"}],\"resources\":{\"requests\":{\"cpu\":\"100m\",\"memory\":\"100Mi\"},\"limits\":{\"cpu\":\"500m\",\"memory\":\"500Mi\"}}}],\"volumes\":[{\"emptyDir\":{},\"name\":\"binary-file\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/test29/public02/glsserver2\"},\"name\":\"ccod-runtime\"},{\"hostPath\":{\"path\":\"\"},\"name\":\"core\"}]},\"metadata\":{\"labels\":{\"glsServer\":\"glsserver2\",\"domain-id\":\"public02\",\"name\":\"glsserver2-public02\"}}},\"strategy\":{\"type\":\"Recreate\"}},\"apiVersion\":\"apps/v1\",\"metadata\":{\"labels\":{\"glsServer\":\"glsserver2\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"public02\",\"type\":\"CCODDomainModule\",\"name\":\"glsserver2-public02\"},\"namespace\":\"test29\",\"name\":\"glsserver2-public02\"}},{\"kind\":\"Deployment\",\"spec\":{\"selector\":{\"matchLabels\":{\"cas\":\"cas1\",\"domain-id\":\"manage01\",\"name\":\"cas1-manage01\"}},\"replicas\":1,\"template\":{\"spec\":{\"terminationGracePeriodSeconds\":600,\"initContainers\":[{\"name\":\"cas1\",\"image\":\"nexus.io:5000/ccod/cas:10973\",\"args\":[],\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"}],\"command\":[\"/bin/sh\",\"-c\"],\"imagePullPolicy\":\"IfNotPresent\",\"resources\":{\"requests\":{\"cpu\":\"100m\",\"memory\":\"512Mi\"},\"limits\":{\"cpu\":\"200m\",\"memory\":\"1024Mi\"}}}],\"containers\":[{\"livenessProbe\":{\"httpGet\":{\"path\":\"/cas\",\"scheme\":\"HTTP\",\"port\":\"8080\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"3\",\"periodSeconds\":\"30\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"name\":\"cas-manage01\",\"image\":\"\",\"args\":[\"keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh;tail -F /usr/local/tomcat/logs/catalina.out;\"],\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"}],\"command\":[\"/bin/sh\",\"-c\"],\"imagePullPolicy\":\"IfNotPresent\",\"readinessProbe\":{\"httpGet\":{\"path\":\"/\",\"scheme\":\"HTTP\",\"port\":\"8080\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"60\",\"periodSeconds\":\"10\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"ports\":[{\"protocol\":\"TCP\",\"containerPort\":\"8080\"}],\"resources\":{\"requests\":{\"cpu\":\"200m\",\"memory\":\"512Mi\"},\"limits\":{\"cpu\":\"500m\",\"memory\":\"1024Mi\"}}}],\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"secret\":{\"secretName\":\"\"},\"name\":\"ssl\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/test29/manage01/cas1\"},\"name\":\"ccod-runtime\"}],\"hostAliases\":[{\"ip\":\"10.130.41.218\",\"hostnames\":[\"test29.ccod.com\"]}]},\"metadata\":{\"labels\":{\"cas\":\"cas1\",\"domain-id\":\"manage01\",\"name\":\"cas1-manage01\"}}},\"strategy\":{\"type\":\"Recreate\"}},\"apiVersion\":\"apps/v1\",\"metadata\":{\"labels\":{\"cas\":\"cas1\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"name\":\"cas1-manage01\"},\"namespace\":\"test29\",\"name\":\"cas1-manage01\"}},{\"kind\":\"Deployment\",\"spec\":{\"selector\":{\"matchLabels\":{\"cas\":\"cas2\",\"domain-id\":\"manage01\",\"name\":\"cas2-manage01\"}},\"replicas\":1,\"template\":{\"spec\":{\"terminationGracePeriodSeconds\":600,\"initContainers\":[{\"name\":\"cas2\",\"image\":\"nexus.io:5000/ccod/cas:10973\",\"args\":[],\"volumeMounts\":[{\"mountPath\":\"/war\",\"name\":\"war\"}],\"command\":[\"/bin/sh\",\"-c\"],\"imagePullPolicy\":\"IfNotPresent\",\"resources\":{\"requests\":{\"cpu\":\"100m\",\"memory\":\"512Mi\"},\"limits\":{\"cpu\":\"200m\",\"memory\":\"1024Mi\"}}}],\"containers\":[{\"livenessProbe\":{\"httpGet\":{\"path\":\"/cas\",\"scheme\":\"HTTP\",\"port\":\"8080\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"3\",\"periodSeconds\":\"30\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"name\":\"cas-manage01\",\"image\":\"\",\"args\":[\"keytool -import -v -trustcacerts -noprompt -storepass changeit -alias test -file /ssl/tls.crt -keystore $JAVA_HOME/lib/security/cacerts;/usr/local/tomcat/bin/startup.sh;tail -F /usr/local/tomcat/logs/catalina.out;\"],\"volumeMounts\":[{\"mountPath\":\"/ssl\",\"name\":\"ssl\"},{\"mountPath\":\"/usr/local/tomcat/webapps\",\"name\":\"war\"},{\"mountPath\":\"/usr/local/tomcat/logs\",\"name\":\"ccod-runtime\"}],\"command\":[\"/bin/sh\",\"-c\"],\"imagePullPolicy\":\"IfNotPresent\",\"readinessProbe\":{\"httpGet\":{\"path\":\"/\",\"scheme\":\"HTTP\",\"port\":\"8080\"},\"timeoutSeconds\":\"1\",\"initialDelaySeconds\":\"60\",\"periodSeconds\":\"10\",\"successThreshold\":\"1\",\"failureThreshold\":\"3\"},\"ports\":[{\"protocol\":\"TCP\",\"containerPort\":\"8080\"}],\"resources\":{\"requests\":{\"cpu\":\"200m\",\"memory\":\"512Mi\"},\"limits\":{\"cpu\":\"500m\",\"memory\":\"1024Mi\"}}}],\"volumes\":[{\"emptyDir\":{},\"name\":\"war\"},{\"secret\":{\"secretName\":\"\"},\"name\":\"ssl\"},{\"hostPath\":{\"path\":\"/var/ccod-runtime/test29/manage01/cas2\"},\"name\":\"ccod-runtime\"}],\"hostAliases\":[{\"ip\":\"10.130.41.218\",\"hostnames\":[\"test29.ccod.com\"]}]},\"metadata\":{\"labels\":{\"cas\":\"cas2\",\"domain-id\":\"manage01\",\"name\":\"cas2-manage01\"}}},\"strategy\":{\"type\":\"Recreate\"}},\"apiVersion\":\"apps/v1\",\"metadata\":{\"labels\":{\"cas\":\"cas2\",\"job-id\":\"88117b15-11bf-4d08-baab-8a14a874334d\",\"domain-id\":\"manage01\",\"type\":\"CCODDomainModule\",\"name\":\"cas2-manage01\"},\"namespace\":\"test29\",\"name\":\"cas2-manage01\"}}],\"k8sPVList\":[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolume\",\"metadata\":{\"name\":\"base-volume-test29\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"capacity\":{\"storage\":\"1Gi\"},\"claimRef\":{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"name\":\"base-volume-test29\",\"namespace\":\"test29\"},\"nfs\":{\"path\":\"/home/kubernetes/volume/k8s-test/baseVolume\",\"server\":\"10.130.41.218\"},\"persistentVolumeReclaimPolicy\":\"Retain\",\"storageClassName\":\"base-volume-test29\",\"volumeMode\":\"Filesystem\"}}],\"k8sPVCList\":[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"metadata\":{\"name\":\"base-volume-test29\",\"namespace\":\"test29\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"resources\":{\"requests\":{\"storage\":\"1Gi\"}},\"storageClassName\":\"base-volume-test29\",\"volumeMode\":\"Filesystem\",\"volumeName\":\"base-volume-test29\"}}],\"createMethod\":\"MANUAL\",\"baseDataNexusRepository\":\"platform_base_data\",\"deployScriptMd5\":\"351688fcd178c23ea7ee4788bae70d32\",\"bkBizId\":26,\"deployScriptPath\":\"test29/20200630152420/platform_deploy.py\",\"ccodVersion\":\"4.1\",\"glsDBPwd\":\"ccod\",\"glsDBUser\":\"ccod\",\"glsDBType\":\"ORACLE\"}";
//            logger.debug(String.format("demoSchema=%s", jsonStr));
//            PlatformUpdateSchemaInfo schema = gson.fromJson(jsonStr, PlatformUpdateSchemaInfo.class);
//            PlatformTopologyInfo topologyInfo = createK8sPlatform(schema);
            PlatformTopologyInfo topologyInfo = getPlatformTopologyFromK8s(schema.getPlatformName(), schema.getPlatformId(), schema.getBkBizId(), schema.getBkCloudId(), schema.getCcodVersion(), schema.getK8sApiUrl(), schema.getK8sAuthToken(), PlatformFunction.TEST);
            System.out.println(JSONObject.toJSONString(topologyInfo));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public PlatformTopologyInfo getPlatformTopologyFromK8s(String platformName, String platformId, int bkBizId, int bkCloudId, String ccodVersion, String k8sApiUrl, String k8sAuthToken, PlatformFunction func) throws ApiException, ParamException, NotSupportAppException, NexusException, LJPaasException, InterfaceCallException, IOException {
        logger.debug(String.format("begin to get %s(%s) topology from %s with authToke=%s", platformId, platformName, k8sApiUrl, k8sAuthToken));
        Date now = new Date();
        V1Namespace ns = ik8sApiService.readNamespace(platformId, k8sApiUrl, k8sAuthToken);
        if(!"Active".equals(ns.getStatus().getPhase()))
        {
            logger.error(String.format("status of %s is %s", platformId, ns.getStatus().getPhase()));
            throw new ParamException(String.format("status of %s is %s", platformId, ns.getStatus().getPhase()));
        }
        List<V1Pod> podList = ik8sApiService.listNamespacedPod(platformId, k8sApiUrl, k8sAuthToken);
        List<V1Service> serviceList = ik8sApiService.listNamespacedService(platformId, k8sApiUrl, k8sAuthToken);
        Map<String, List<V1Pod>> srvPodMap = new HashMap<>();
        Map<K8sServiceType, List<V1Service>> typeSrvMap = new HashMap<>();
        for(V1Service k8sSvr : serviceList)
        {
            K8sServiceType svrType = getServiceType(k8sSvr.getMetadata().getName());
            List<V1Pod> srvPods = getServicePod(k8sSvr, podList);
            if(!typeSrvMap.containsKey(svrType))
                typeSrvMap.put(svrType, new ArrayList<>());
            typeSrvMap.get(svrType).add(k8sSvr);
            srvPodMap.put(k8sSvr.getMetadata().getName(), srvPods);
        }
        List<PlatformAppModuleParam> deployAppParamList = new ArrayList<>();
        Map<String, List<NexusAssetInfo>> srcCfgMap = new HashMap<>();
        List<AppModuleVo> registerAppList = appManagerService.queryAllRegisterAppModule(true);
        Map<String, String> outSvcAppMap = new HashMap<>();
        List<V1Service> outSvcList = typeSrvMap.containsKey(K8sServiceType.DOMAIN_OUT_SERVICE) ? typeSrvMap.get(K8sServiceType.DOMAIN_OUT_SERVICE) : new ArrayList<>();
        logger.debug(String.format("%s has %d domain out service", platformId, outSvcList.size()));
        List<DomainPo> allDomains = new ArrayList<>();
        List<AssemblePo> allAssembles = new ArrayList<>();
        for(V1Service outSvc : outSvcList)
        {
            PlatformAppModuleParam param = getPlatformAppParam4FromK8s(platformId, outSvc, srvPodMap.get(outSvc.getMetadata().getName()), allDomains, allAssembles, registerAppList);
            String[] arr = outSvc.getMetadata().getName().split("\\-");
            logger.debug(String.format("%s is domain out service, so service %s-%s will been passed", outSvc.getMetadata().getName(), arr[0], arr[1]));
            outSvcAppMap.put(String.format("%s-%s", arr[0], arr[1]), outSvc.getMetadata().getName());
            deployAppParamList.add(param);
        }
        List<V1Service> domainSvcList = typeSrvMap.containsKey(K8sServiceType.DOMAIN_SERVICE) ? typeSrvMap.get(K8sServiceType.DOMAIN_SERVICE) : new ArrayList<>();
        logger.debug(String.format("%s has %d domain service", platformId, domainSvcList.size()));
        for(V1Service domainSvc : domainSvcList)
        {
            String svcName = domainSvc.getMetadata().getName();
            if(outSvcAppMap.containsKey(svcName))
            {
                logger.debug(String.format("%s has been handle as domain out service %s, so pass", svcName, outSvcAppMap.get(svcName)));
                continue;
            }
            PlatformAppModuleParam param = getPlatformAppParam4FromK8s(platformId, domainSvc, srvPodMap.get(svcName), allDomains, allAssembles, registerAppList);
            deployAppParamList.add(param);
        }
        logger.debug(String.format("%s(%s) has deployed %d apps", platformName, platformId, deployAppParamList.size()));
        List<V1Service> threeAppSvcList = typeSrvMap.containsKey(K8sServiceType.THREE_PART_APP) ? typeSrvMap.get(K8sServiceType.THREE_PART_APP) : new ArrayList<>();
        logger.debug(String.format("%s has %d three part apps", platformId, threeAppSvcList.size()));
        List<PlatformThreePartAppPo> threeAppList = new ArrayList<>();
        for(V1Service threeAppSvc : threeAppSvcList)
        {
            PlatformThreePartAppPo po = getPlatformThreePartApp4FromK8s(platformId, threeAppSvc, srvPodMap.get(threeAppSvc.getMetadata().getName()));
            threeAppList.add(po);
        }
        List<V1Service> threeSvcList = typeSrvMap.containsKey(K8sServiceType.THREE_PART_SERVICE) ? typeSrvMap.get(K8sServiceType.THREE_PART_SERVICE) : new ArrayList<>();
        logger.debug(String.format("%s has %d three part service", platformId, threeSvcList.size()));
        List<PlatformThreePartServicePo> threePartSvcList = new ArrayList<>();
        for(V1Service threeSvcSvc : threeSvcList)
        {
            PlatformThreePartServicePo po = getPlatformThreePartService4FromK8s(platformId, threeSvcSvc, srvPodMap.get(threeSvcSvc.getMetadata().getName()));
            threePartSvcList.add(po);
        }
        Map<String, List<NexusAssetInfo>> cfgMap = new HashMap<>();
        for(PlatformAppModuleParam param : deployAppParamList)
        {
            String serviceName = String.format("%s-%s", param.getAlias(), param.getDomainPo().getDomainId());
            List<NexusAssetInfo> assetList = this.nexusService.downloadAndUploadFiles(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, param.getCfgs(), this.nexusHostUrl, this.nexusUserName, this.nexusPassword, this.platformAppCfgRepository, param.getPlatformAppPo().getPlatformAppDirectory(param.getModuleVo().getAppName(), param.getModuleVo().getVersion(), param.getPlatformAppPo()), true);
            cfgMap.put(serviceName, assetList);
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
        for(DomainPo domainPo : allDomains)
        {
            logger.debug(String.format("insert new domain : %s", JSONObject.toJSONString(domainPo)));
            this.domainMapper.insert(domainPo);
        }
        for(AssemblePo assemblePo : allAssembles)
        {
            logger.debug(String.format("insert new assemble : %s", JSONObject.toJSONString(assemblePo)));
            this.assembleMapper.insert(assemblePo);
        }
        for(PlatformAppModuleParam param : deployAppParamList)
        {
            PlatformAppPo platformAppPo = param.getPlatformAppPo();
            platformAppPo.setAssembleId(param.getAssemblePo().getAssembleId());
            logger.debug(String.format("insert new platform app : %s", JSONObject.toJSONString(platformAppPo)));
            this.platformAppMapper.insert(platformAppPo);
            List<NexusAssetInfo> cfgs = cfgMap.get(String.format("%s-%s", platformAppPo.getAppAlias(), platformAppPo.getDomainId()));
            for(NexusAssetInfo cfg : cfgs)
            {
                PlatformAppCfgFilePo cfgFilePo = new PlatformAppCfgFilePo(platformAppPo.getPlatformAppId(), platformAppPo.getAppId(), "/", cfg);
                logger.debug(String.format("insert new cfg : %s", JSONObject.toJSONString(cfgFilePo)));
                this.platformAppCfgFileMapper.insert(cfgFilePo);
            }
        }
        for(PlatformThreePartAppPo threePartAppPo : threeAppList)
        {
            logger.debug(String.format("insert new platform three part app : %s", JSONObject.toJSONString(threePartAppPo)));
            this.platformThreePartAppMapper.insert(threePartAppPo);
        }
        for(PlatformThreePartServicePo threePartServicePo : threePartSvcList)
        {
            logger.debug(String.format("insert new platform three part service : %s", JSONObject.toJSONString(threePartServicePo)));
            this.platformThreePartServiceMapper.insert(threePartServicePo);
        }
        ljPaasService.syncClientCollectResultToPaas(bkBizId, platformId, bkCloudId);
        return this.appManagerService.getPlatformTopology(platformId);
    }

    /**
     * 找出服务关联的deployment列表
     * @param service 指定服务
     * @param deploymentList 命名空间内所有的deployment
     * @return 指定服务关联的服务列表
     */
    private List<V1Deployment> getServiceDeployment(V1Service service, List<V1Deployment> deploymentList)
    {
        List<V1Deployment> retList = new ArrayList<>();
        if(service.getSpec().getSelector() != null && service.getSpec().getSelector().size() > 0)
        {
            for(V1Deployment deployment : deploymentList)
            {
                boolean isSelected = isSelected(deployment.getSpec().getTemplate().getMetadata().getLabels(), service.getSpec().getSelector());
                if(isSelected)
                    retList.add(deployment);
            }
        }
        logger.debug(String.format("service %s select %d deployment", service.getMetadata().getName(), retList.size()));
        return retList;
    }

    private PlatformAppDeployDetailVo getAppDeployDetail(AppModuleVo appModule, V1Service service, V1Deployment deployment, V1ConfigMap configMap) throws ParamException
    {
        String alias = service.getMetadata().getName().split("\\-")[0];
        String domainId = service.getMetadata().getName().split("\\-")[1];
        BizSetDefine setDefine = getBizSetForDomainId(domainId);
        PlatformAppDeployDetailVo deployApp = new PlatformAppDeployDetailVo();
        deployApp.setBkSetName(setDefine.getName());
        deployApp.setDomainName(String.format("%s%s", setDefine.getFixedDomainName(), domainId.replaceAll(setDefine.getFixedDomainId(), "")));
        deployApp.setDomainId(domainId);
        deployApp.setAppAlias(alias);
        deployApp.setAppId(appModule.getAppId());
        deployApp.setAppName(appModule.getAppName());
        deployApp.setAppRunner(deployApp.getAppAlias());
        deployApp.setAppType(appModule.getAppType());
        deployApp.setAssembleTag(deployment.getSpec().getSelector().getMatchLabels().get("name"));
        deployApp.setAssembleId(0);
        if(AppType.CCOD_KERNEL_MODULE.equals(deployApp.getAppType()))
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
        String port = getPortFromK8sService(Arrays.asList(service));
        deployApp.setPort(port);
        deployApp.setSrcCfgs(appModule.getCfgs());
        int replicas = deployment.getStatus().getReplicas() != null ? deployment.getStatus().getReplicas() : 0;
        int availableReplicas = deployment.getStatus().getAvailableReplicas() != null ? deployment.getStatus().getAvailableReplicas() : 0;
        int unavailableReplicas = deployment.getStatus().getUnavailableReplicas() != null ? deployment.getStatus().getUnavailableReplicas() : 0;
        deployApp.setReplicas(replicas);
        deployApp.setAvailableReplicas(availableReplicas);
        String status = "Running";
        if(unavailableReplicas == replicas)
            status = "Error";
        else if(availableReplicas < replicas)
            status = "Updating";
        deployApp.setStatus(status);
        deployApp.setVersion(appModule.getVersion());
        deployApp.setVersionControl(appModule.getVersionControl().name);
        deployApp.setVersionControlUrl(appModule.getVersionControlUrl());
        return deployApp;

    }

    private K8sPlatformParamVo generateK8sPlatformParam(String platformId, String platformName, String ccodVersion, String k8sApiUrl, String k8sAuthToken) throws IOException, K8sDataException, ApiException, NotSupportAppException, InterfaceCallException, NexusException, ParamException
    {
        logger.debug(String.format("generate %s[%s] topology from %s", platformName, platformId, k8sApiUrl));
        List<V1Deployment> deployments = this.k8sApiService.listNamespacedDeployment(platformId, k8sApiUrl, k8sAuthToken);
        List<V1Service> services = this.k8sApiService.listNamespacedService(platformId, k8sApiUrl, k8sAuthToken);
        List<V1ConfigMap> configMaps = this.k8sApiService.listNamespacedConfigMap(platformId, k8sApiUrl, k8sAuthToken);
        List<AppModuleVo> registerApps = this.appManagerService.queryAllRegisterAppModule(true);
        K8sPlatformParamVo paramVo = getK8sPlatformParam(platformId, platformName, ccodVersion, deployments, services, configMaps, registerApps);
        return paramVo;
    }

    private PlatformAppDeployDetailVo parsePlatformDeployApp(V1Deployment deployment, V1Container container, List<V1Pod> pods, List<V1Service> services, String deploymentName, String platformId, String platformName, List<AppModuleVo>  registerApps, int bkBizId, int bkCloudId, String ccodVersion, List<LJHostInfo> hostList, String status) throws ParamException, NotSupportAppException
    {
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
        if(AppType.CCOD_KERNEL_MODULE.equals(deployApp.getAppType()))
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
     * @param services k8s一组服务
     * @return 使用的端口信息
     */
    private String getPortFromK8sService(List<V1Service> services)
    {
        String port = "";
        for(V1Service service : services)
        {
            for(V1ServicePort svcPort : service.getSpec().getPorts())
            {
                if(svcPort.getNodePort() != null && svcPort.getNodePort() > 0)
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
     * @param service 指定服务
     * @return 该服务的nodePort
     */
    private int getNodePortFromK8sService(V1Service service)
    {
        String strPort = getPortFromK8sService(Arrays.asList(service));
        return Integer.parseInt(strPort.split("\\:")[1].split("/")[0]);
    }

    /**
     * 从k8s服务以及服务选择的pod中获取第三方应用的状态
     * @param namespace 命名空间及平台id
     * @param service 服务信息
     * @param svrPodList 服务选择的pod列表
     * @return 第三方应用状态
     * @throws ParamException
     */
    private PlatformThreePartServicePo getPlatformThreePartService4FromK8s(String namespace, V1Service service, List<V1Pod> svrPodList) throws ParamException
    {
        PlatformThreePartServicePo po = new PlatformThreePartServicePo();
        po.setHostIp(null);
        po.setPlatformId(namespace);
        po.setServiceName(service.getMetadata().getName());
        return po;
    }

    /**
     * 从k8s服务以及服务选择的pod中获取第三方应用的状态
     * @param namespace 命名空间及平台id
     * @param service 服务信息
     * @param svrPodList 服务选择的pod列表
     * @return 第三方应用状态
     * @throws ParamException
     */
    private PlatformThreePartAppPo getPlatformThreePartApp4FromK8s(String namespace, V1Service service, List<V1Pod> svrPodList) throws ParamException
    {
        String appName = service.getMetadata().getName().split("\\-")[0];
        logger.debug(String.format("begin to get three part app %s info", appName));
        if(svrPodList.size() != 1)
            throw new ParamException(String.format("%s has select %d pods, which is not support this version", appName, svrPodList.size()));
        V1Pod pod = svrPodList.get(0);
        String port = getPortFromK8sService(Arrays.asList(service));
        PlatformThreePartAppPo po = new PlatformThreePartAppPo();
        po.setAppName(appName);
        po.setHostIp(pod.getStatus().getHostIP());
        po.setPlatformId(namespace);
        po.setPort(port);
        po.setStatus(pod.getStatus().getPhase());
        logger.debug(String.format("three part app %s : %s", appName, JSONObject.toJSONString(po)));
        return po;
    }

    /**
     * 获取平台部署的ccod应用信息
     * @param namespace k8s的namespace也就是平台的id
     * @param service ccod应用对应的k8s服务
     * @param svrPodList ccod应用选择的pod列表
     * @param registerAppList 已经向cmdb注册的应用列表
     * @param allDomains 平台下所有域
     * @param allAssembles 平台下所有assembles
     * @return ccod应用模块的部署详情
     * @throws ParamException
     */
    private PlatformAppModuleParam getPlatformAppParam4FromK8s(String namespace, V1Service service, List<V1Pod> svrPodList, List<DomainPo> allDomains, List<AssemblePo> allAssembles, List<AppModuleVo> registerAppList) throws ParamException
    {
        logger.debug(String.format("get platform app from service %s with %d pods at %s", service.getMetadata().getName(), svrPodList.size(), namespace));
        if(svrPodList.size() != 1)
            throw new ParamException(String.format("domain service %s select %d pods, current version not support", service.getMetadata().getName(), svrPodList.size()));
        V1Pod pod = svrPodList.get(0);
        Date now = new Date();
        String alias = service.getMetadata().getName().split("\\-")[0];
        String domainId = service.getMetadata().getName().split("\\-")[1];
        BizSetDefine setDefine = null;
        AppDefine appDefine = null;
        for(BizSetDefine set : this.ccodBiz.getSet())
        {
            String domainRegex = String.format("^%s(0[1-9]|[1-9]\\d+)", set.getFixedDomainId());
            if(domainId.matches(domainRegex))
            {
                setDefine = set;
                for(AppDefine app : set.getApps())
                {
                    String aliasRegex = String.format("^%s\\d*$", app.getAlias());
                    if(alias.matches(aliasRegex))
                    {
                        appDefine = app;
                        break;
                    }
                }
                break;
            }
        }
        DomainPo domainPo;
        if(!allDomains.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity())).containsKey(domainId))
        {
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
        }
        else
            domainPo = allDomains.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity())).get(domainId);
        String assembleTag = pod.getMetadata().getName().split("\\-")[0];
        AssemblePo assemblePo;
        if(allAssembles.stream().collect(Collectors.groupingBy(AssemblePo::getDomainId)).containsKey(domainId)
                && allAssembles.stream().collect(Collectors.groupingBy(AssemblePo::getDomainId)).get(domainId).stream().collect(Collectors.toMap(AssemblePo::getTag, Function.identity())).containsKey(assembleTag))
            assemblePo = allAssembles.stream().collect(Collectors.groupingBy(AssemblePo::getDomainId)).get(domainId)
                    .stream().collect(Collectors.toMap(AssemblePo::getTag, Function.identity())).get(assembleTag);
        else
        {
            assemblePo = new AssemblePo();
            assemblePo.setDomainId(domainId);
            assemblePo.setPlatformId(namespace);
            assemblePo.setStatus(pod.getStatus().getPhase());
            assemblePo.setTag(assembleTag);
            allAssembles.add(assemblePo);
        }
        String version = null;
        String versionRegex = String.format("^%s\\:[^\\:]+$", appDefine.getName().toLowerCase());
        for(V1Container container : pod.getSpec().getInitContainers())
        {
            String[] arr = container.getImage().split("/");
            String imageTag = arr[arr.length - 1];
            if(imageTag.matches(versionRegex))
            {
                version = imageTag.replaceAll("^[^\\:]+\\:", "").replaceAll("\\-", ":");
                break;
            }
        }
        if(StringUtils.isBlank(version))
        {
            for(V1Container container : pod.getSpec().getContainers())
            {
                if(container.getImage().matches(versionRegex))
                {
                    version = container.getImage().replace("^%s\\:", "").replaceAll("\\-", ":");
                    break;
                }
            }
        }
        if(StringUtils.isBlank(version))
        {
            logger.error(String.format("can not get version of service %s with podName=%s", service.getMetadata().getName(), pod.getMetadata().getName()));
            throw new ParamException(String.format("can not get version of service %s with podName=%s", service.getMetadata().getName(), pod.getMetadata().getName()));
        }
        Map<String, List<AppModuleVo>> appMap = registerAppList.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        if(!appMap.containsKey(appDefine.getName()) || !appMap.get(appDefine.getName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version))
        {
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
        for(AppCfgFilePo cfg : moduleVo.getCfgs())
            cfgs.add(cfg.getNexusAsset(this.nexusHostUrl));
        PlatformAppModuleParam param = new PlatformAppModuleParam(moduleVo, alias, namespace, domainPo, assemblePo, po, cfgs);
        return param;
    }

    /**
     * 获取服务选择的pod
     * @param service k8s服务
     * @param podList 服务所属命名空间下的所有pod
     * @return k8s服务选择的pod
     */
    private List<V1Pod> getServicePod(V1Service service, List<V1Pod> podList)
    {
        logger.debug(String.format("select pods for service %s", service.getMetadata().getName()));
        List<V1Pod> list = new ArrayList<>();
        if(service.getSpec().getSelector() == null || service.getSpec().getSelector().size() == 0)
        {
            logger.debug(String.format("service %s has not selector, so pod is 0", service.getMetadata().getName()));
            return list;
        }
        for(V1Pod pod : podList)
        {
            boolean isSelected = isSelected(pod.getMetadata().getLabels(), service.getSpec().getSelector());
            if(isSelected)
            {
                list.add(pod);
            }
        }
        logger.debug(String.format("service %s select %d pod", service.getMetadata().getName(), list.size()));
        return list;
    }

    /**
     * 通过服务名判断k8s的服务的类型
     * @param serviceName 服务名
     * @return 指定服务的服务类型
     * @throws NotSupportAppException 指定的服务对应的应用不被支持
     * @throws ParamException 解析服务名失败
     */
    private K8sServiceType getServiceType(String serviceName) throws NotSupportAppException, ParamException
    {
        for(String name : ccodBiz.getThreePartApps())
        {
            String regex = String.format("^%s\\d*$", name.toLowerCase());
            if(serviceName.matches(regex))
                return K8sServiceType.THREE_PART_APP;
        }
        for(String name : ccodBiz.getThreePartServices())
        {
            String regex = String.format("^%s\\d*$", name.toLowerCase());
            if(serviceName.matches(regex))
                return K8sServiceType.THREE_PART_SERVICE;
        }
        String[] arr = serviceName.split("\\-");
        if(arr.length < 2)
            throw new ParamException(String.format("%s is illegal domain service name", serviceName));
        String alias = arr[0];
        String domainId = arr[1];
        for(BizSetDefine setDefine : this.ccodBiz.getSet())
        {
            String domainRegex = String.format("^%s(0[1-9]|[1-9]\\d+)", setDefine.getFixedDomainId());
            if(domainId.matches(domainRegex))
            {
                for(AppDefine appDefine : setDefine.getApps())
                {
                    String aliasRegex = String.format("^%s\\d*$", appDefine.getAlias());
                    if(alias.matches(aliasRegex))
                        if(arr.length == 2)
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
        this.platformThreePartServiceMapper.delete(platformId, null);
        this.platformThreePartAppMapper.delete(platformId, null);
        this.platformAppBkModuleMapper.delete(null, null, platformId, null);
        List<PlatformAppPo> deployApps = platformAppMapper.select(platformId, null, null, null, null, null);
        for(PlatformAppPo deployApp : deployApps)
        {
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
        if(platformPo == null)
        {
            logger.error(String.format("create platform app update task fail  : %s not exist", platformId));
            throw new ParamException(String.format("create platform app update task fail  : %s not exist", platformId));
        }
        if(!platformPo.getPlatformName().equals(platformName))
        {
            logger.error(String.format("name of %s is %s not %s", platformId, platformPo.getPlatformName(), platformName));
            throw new ParamException(String.format("name of %s is %s not %s", platformId, platformPo.getPlatformName(), platformName));
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
                    appManagerService.startCollectPlatformAppUpdateData(platformId, platformName);
                }
                catch (Exception ex)
                {
                    logger.error(String.format("collect %s(%s) update task exception",
                            platformName, platformId), ex);
                }
            }
        });
        executor.execute(taskThread);
        executor.shutdown();
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
    public void startCollectPlatformAppData(String platformId, String platformName, int bkBizId, int bkCloudId) throws ParamException, Exception {
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
            List<DomainPo> domainList = new ArrayList<>();
            Map<String, List<PlatformAppModuleVo>> domainAppMap = modules.stream().collect(Collectors.groupingBy(PlatformAppModuleVo::getDomainName));
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
            platformPo = modules.get(0).getPlatform();
            platformPo.setBkBizId(bkBizId);
            platformPo.setBkCloudId(bkCloudId);
            platformMapper.insert(platformPo);
            logger.debug(String.format("begin to preprocess collected %d app", modules.size()));
            List<PlatformAppModuleVo> successList = this.appManagerService.preprocessCollectedPlatformAppModule(platformName, platformId, domainAppMap.values().stream().flatMap(listContainer->listContainer.stream()).collect(Collectors.toList()), failList);
            Map<String, List<DomainPo>> setDomainMap = domainList.stream().collect(Collectors.groupingBy(DomainPo::getBizSetName));
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
            Map<String, List<AppModuleVo>> appMap = this.appManagerService.queryAllRegisterAppModule(null).stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
            for(PlatformAppModuleVo moduleVo : successList)
            {
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
                for(DeployFileInfo cfgFilePo : moduleVo.getCfgs())
                {
                    PlatformAppCfgFilePo po = new PlatformAppCfgFilePo(platformApp.getPlatformAppId(), cfgFilePo);
                    logger.debug(String.format("insert cfg %s into platform_app_cfg", JSONObject.toJSONString(po)));
                    this.platformAppCfgFileMapper.insert(po);
                }
                logger.info(String.format("[%s] platform app module handle SUCCESS", moduleVo.toString()));
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
            this.ljPaasService.syncClientCollectResultToPaas(bkBizId, platformId, bkCloudId);
            platformPo.setStatus(CCODPlatformStatus.RUNNING.id);
            this.platformMapper.update(platformPo);
        }
        finally {
            this.isPlatformCheckOngoing = false;
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
        po.setBizSetName(bkSetName);
        logger.debug(String.format("%s belong to %s", domainName, bkSetName));
        return po;
    }

    /**
     * 生成应用的别名
     * @param standardAlias 应用的标准别名
     * @param usedAlias 该应用已经使用了的别名
     * @param onlyOne 新加的该类应用是否只有一个
     * @return 生成的应用别名
     * @throws ParamException
     */
    private String autoGenerateAlias(String standardAlias, List<String> usedAlias, boolean onlyOne) throws ParamException
    {
        if(usedAlias.size() == 0)
        {
            if(standardAlias.equals("ucgateway"))
                return "ucgateway0";
            else
                return onlyOne ? standardAlias : standardAlias + "1";
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
                    index = oldIndex;
            }
            else if(index == 0 && !standardAlias.equals("ucgateway"))
                index = 1;
        }
        index++;
        String appAlias = String.format("%s%s", standardAlias, index);
        return appAlias;
    }

    /**
     * 处理处理失败的平台应用模块
     * @param moduleVo 处理失败的平台应用模块
     * @return 用来入库的pojo类
     * @throws Exception
     */
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

    private void updateAppDefaultCfg(String protoPlatformId) throws Exception
    {
        List<PlatformAppDeployDetailVo> deployAppList = this.platformAppDeployDetailMapper.selectPlatformApps(protoPlatformId, null, null);
        Map<String, List<AppModuleVo>> registerAppMap = this.appManagerService.queryAllRegisterAppModule(null).stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        for(PlatformAppDeployDetailVo deployApp : deployAppList)
        {
            logger.info(String.format("begin to update %s[%s]", deployApp.getAppName(), deployApp.getVersion()));
            AppModuleVo moduleVo = registerAppMap.get(deployApp.getAppName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(deployApp.getVersion());
            List<AppCfgFilePo> cfgs = new ArrayList<>();
            for(PlatformAppCfgFilePo cfg : deployApp.getCfgs())
            {
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
     * @param platformId 需要迁移的平台
     */
    private void transferPlatformToAssembleTopology(String platformId) throws Exception {
        logger.debug(String.format("begin to transfer %s to the topology with assemble", platformId));
        PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
        if(platformPo == null)
        {
            logger.error(String.format("%s not exist", platformId));
            throw new Exception(String.format("%s not exist", platformId));
        }
        Map<String, List<PlatformAppPo>> domainAppMap = this.platformAppMapper.select(platformId, null, null, null, null, null)
                .stream().collect(Collectors.groupingBy(PlatformAppPo::getDomainId));
        for(String domainId : domainAppMap.keySet())
        {
            for(PlatformAppPo deployApp : domainAppMap.get(domainId))
            {
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

    private PlatformPo getK8sPlatform(String platformId) throws ParamException
    {
        PlatformPo platformPo = this.platformMapper.selectByPrimaryKey(platformId);
        if(platformPo == null)
            throw new ParamException(String.format("%s platform not exit", platformId));
        if(!PlatformType.K8S_CONTAINER.equals(platformPo.getType()))
        {
            logger.error(String.format("platform %s type is %s not %s", platformId, platformPo.getType().name, PlatformType.K8S_CONTAINER.name));
            throw new ParamException(String.format("%s is not %s platform", platformId, PlatformType.K8S_CONTAINER.name));
        }
        if(StringUtils.isBlank(platformPo.getApiUrl()))
        {
            logger.error(String.format("k8s api url of %s is blank", platformId));
            throw new ParamException(String.format("k8s api url of %s is blank", platformId));
        }
        if(StringUtils.isBlank(platformPo.getAuthToken()))
        {
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
        logger.debug(String.format("create configMap for %s from %s", platformId, createSchema.getK8sApiUrl()));
        if(createSchema.getPublicConfig() != null && createSchema.getPublicConfig().size() > 0)
        {
            List<NexusAssetInfo> publicConfigList = new ArrayList<>();
            for(AppFileNexusInfo cfg : createSchema.getPublicConfig())
                publicConfigList.add(cfg.getNexusAssetInfo(this.nexusHostUrl));
            this.k8sApiService.createConfigMapFromNexus(platformId, platformId, k8sApiUrl, k8sAuthToken, publicConfigList, this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
        }
        for(DomainUpdatePlanInfo planInfo : createSchema.getDomainUpdatePlanList())
        {
            String domainId = planInfo.getDomainId();
            if(planInfo.getPublicConfig() != null && planInfo.getPublicConfig().size() > 0)
            {
                List<NexusAssetInfo> domainPublicConfigs = new ArrayList<>();
                for(AppFileNexusInfo cfg : planInfo.getPublicConfig())
                    domainPublicConfigs.add(cfg.getNexusAssetInfo(nexusHostUrl));
                this.k8sApiService.createConfigMapFromNexus(platformId, domainId, k8sApiUrl, k8sAuthToken, domainPublicConfigs, this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
            }
            for(AppUpdateOperationInfo optInfo : planInfo.getAppUpdateOperationList())
            {
                List<NexusAssetInfo> appCfgs = new ArrayList<>();
                for(AppFileNexusInfo cfg : optInfo.getCfgs())
                    appCfgs.add(cfg.getNexusAssetInfo(nexusHostUrl));
                this.k8sApiService.createConfigMapFromNexus(platformId, String.format("%s-%s", optInfo.getAppAlias(), domainId), k8sApiUrl, k8sAuthToken, appCfgs, this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
            }
        }
        return this.k8sApiService.listNamespacedConfigMap(platformId, k8sApiUrl, k8sAuthToken);
    }

    /**
     * 根据域id获得域所在的业务集群
     * @param domainId 域id
     * @return 域所在的业务集群
     */
    private BizSetDefine getBizSetForDomainId(String domainId) throws ParamException
    {
        logger.debug(String.format("to find bisSet for domainId %s", domainId));
        BizSetDefine setDefine = null;
        for(BizSetDefine set : this.ccodBiz.getSet())
        {
            String regex = String.format("^%s(0[1-9]|[1-9]\\d+)", set.getFixedDomainId());
            if(domainId.contains(regex))
            {
                setDefine = set;
                break;
            }
        }
        if(setDefine == null)
            throw new ParamException(String.format("%s is illegal domainId for ccod bizSet", domainId));
        logger.debug(String.format("bizSet for domainId %s found", domainId));
        return setDefine;
    }

    private AppType getAppTypeFromImageUrl(String imageUrl) throws ParamException, NotSupportAppException {
        Map<String, List<AppModuleVo>> registerAppMap = this.appManagerService.queryAllRegisterAppModule(true)
                .stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        String[] arr = imageUrl.split("/");
        if(arr.length != 3)
            throw new ParamException(String.format("%s is illegal imageUrl", imageUrl));
        String repository = arr[1];
        arr = arr[2].split("\\:");
        if(arr.length != 2)
            throw new ParamException(String.format("%s is illegal image tag", arr[2]));
        String appName = arr[0];
        String version = arr[1].replaceAll("\\-", ":");
        Set<String> ccodRepSet = new HashSet<>(imageCfg.getCcodModuleRepository());
        Set<String> threeAppRepSet = new HashSet<>(imageCfg.getThreeAppRepository());
        AppType appType = null;
        if(ccodRepSet.contains(repository))
        {
            for(String name : registerAppMap.keySet())
            {
                if(name.toLowerCase().equals(appName))
                {
                    List<AppModuleVo> modules = registerAppMap.get(name);
                    appType = modules.stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version) ? modules.stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version).getAppType() : null;
                    if(appType == null)
                        throw new ParamException(String.format("%s[%s] not register", name, version));
                    break;
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

    /**
     * 根据域id和别名获取模块名称
     * @param domainId 域id
     * @param alias 别名
     * @return 对应的模块名称
     * @throws ParamException 别名或域id不正确
     */
    private String getAppNameFromDomainIdAndAlias(String domainId, String alias) throws ParamException
    {
        BizSetDefine setDefine = null;
        for(BizSetDefine set : this.ccodBiz.getSet())
        {
            String domainIdRegex = String.format(this.domainIdRegexFmt, set.getFixedDomainId());
            if(domainId.matches(domainIdRegex))
            {
                setDefine = set;
                break;
            }
        }
        if(setDefine == null)
            throw new ParamException(String.format("%s is illegal domainId", domainId));
        AppDefine appDefine = null;
        for(AppDefine define : setDefine.getApps())
        {
            String aliasRegex = String.format(this.aliasRegexFmt, define.getAlias());
            if(alias.matches(aliasRegex))
            {
                appDefine = define;
                break;
            }
        }
        if(appDefine == null)
            throw new ParamException(String.format("%s is illegal alias for %s", alias, setDefine.getFixedDomainName()));
        logger.debug(String.format("%s is the appName for %s at %s", appDefine.getName(), alias, domainId));
        return appDefine.getName();
    }

    /**
     * 获取镜像标签对应的应用模块信息
     * @param imageTag 镜像标签
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
        for(String name : registerAppMap.keySet())
        {
            if(name.toLowerCase().equals(appName))
            {
                for(AppModuleVo moduleVo : registerAppMap.get(name))
                {
                    if(moduleVo.getVersion().replaceAll("\\:", "-").equals(version))
                    {
                        logger.debug(String.format("app module for %s found", imageTag));
                        return moduleVo;
                    }
                }
                throw new ParamException(String.format("can not find ccod app module for %s", imageTag));
            }
        }
        throw new ParamException(String.format("can not find ccod app module for %s", imageTag));
    }

    private void someTest() throws Exception
    {
        String jsonStr = "{\"k8sApiUrl\":\"https://10.130.41.218:6443\",\"k8sAuthToken\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA\",\"baseDataNexusPath\":\"ccod/4.1/baseVolume.zip\",\"baseDataNexusRepository\":\"platform_base_data\",\"bkBizId\":34,\"bkCloudId\":0,\"ccodVersion\":\"CCOD4.1\",\"comment\":\"create 工具组平台(202005-test) by clone 123456-wuph(ccod开发测试平台)\",\"createTime\":1591061579290,\"deadline\":1591061579290,\"domainUpdatePlanList\":[{\"appUpdateOperationList\":[{\"addDelay\":0,\"appAlias\":\"licenseserver\",\"appName\":\"LicenseServer\",\"appRunner\":\"licenseserver\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./bin/license/\",\"ext\":\"ini\",\"fileName\":\"Config.ini\",\"fileSize\":0,\"md5\":\"6c513269c4e2bc10f4a6cf0eb05e5bfc\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNlZGYyZTBkYjgyNWQ0OTRi\",\"nexusPath\":\"/configText/202005-test/public01_licenseserver/Config.ini\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"5214\"},{\"addDelay\":0,\"appAlias\":\"configserver\",\"appName\":\"configserver\",\"appRunner\":\"configserver\",\"basePath\":\"/home/cfs/Platform/\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"ccs_config.cfg\",\"fileSize\":0,\"md5\":\"1095494274dc98445b79ec1d32900a6f\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY5NWYyNTlkYWY3ZWEzZWNl\",\"nexusPath\":\"/configText/202005-test/public01_configserver/ccs_config.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"ccs_logger.cfg\",\"fileSize\":0,\"md5\":\"197075eb110327da19bfc2a31f24b302\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ1NGYyYWEyOGE2ZGNhYjlh\",\"nexusPath\":\"/configText/202005-test/public01_configserver/ccs_logger.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"aca2af60caa0fb9f4af57f37f869dafc90472525\"},{\"addDelay\":30,\"appAlias\":\"glsserver\",\"appName\":\"glsServer\",\"appRunner\":\"glsserver\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"gls_config.cfg\",\"fileSize\":0,\"md5\":\"f23a83a2d871d59c89d12b0281e10e90\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmMmRiNjc0YzQ5YmE4Nzdj\",\"nexusPath\":\"/configText/202005-test/public01_glsserver/gls_config.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"gls_logger.cfg\",\"fileSize\":0,\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFhYTlmZWY0MTJkMDY2ZTM3\",\"nexusPath\":\"/configText/202005-test/public01_glsserver/gls_logger.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\"}],\"bkSetName\":\"公共组件\",\"comment\":\"clone from 公共组件01 of 123456-wuph\",\"createTime\":1591061579289,\"domainId\":\"public01\",\"domainName\":\"公共组件01\",\"executeTime\":1591061579289,\"maxOccurs\":1000,\"occurs\":600,\"status\":\"CREATE\",\"tags\":\"入呼叫,外呼\",\"updateTime\":1591061579289,\"updateType\":\"ADD\"},{\"appUpdateOperationList\":[{\"addDelay\":20,\"appAlias\":\"dds\",\"appName\":\"DDSServer\",\"appRunner\":\"dds\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dds_logger.cfg\",\"fileSize\":0,\"md5\":\"7f783a4ea73510c73ac830f135f4c762\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5MDNhNjZiNDlmZDMxNzYx\",\"nexusPath\":\"/configText/202005-test/cloud01_dds/dds_logger.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dds_config.cfg\",\"fileSize\":0,\"md5\":\"d89e98072e96a06efa41c69855f4a3cc\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZjOTYzMzczYzhmZjFjMTRm\",\"nexusPath\":\"/configText/202005-test/cloud01_dds/dds_config.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"150:18722\"},{\"addDelay\":20,\"appAlias\":\"ucds\",\"appName\":\"UCDServer\",\"appRunner\":\"ucds\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"ucds_config.cfg\",\"fileSize\":0,\"md5\":\"f4445f10c75c9ef2f6d4de739c634498\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxMzRlYzQyOWIwNzZlMDU0\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/ucds_config.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"ucds_logger.cfg\",\"fileSize\":0,\"md5\":\"ec57329ddcec302e0cc90bdbb8232a3c\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFjYWUzYjQxZWU5NTMxOTg4\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/ucds_logger.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"DRWRClient.cfg\",\"fileSize\":0,\"md5\":\"8b901d87855de082318314d868664c03\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ2NWYyYzE5NTAwNDY4YzQ1\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/DRWRClient.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\"},{\"addDelay\":20,\"appAlias\":\"dcs\",\"appName\":\"dcs\",\"appRunner\":\"dcs\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dc_log4cpp.cfg\",\"fileSize\":0,\"md5\":\"5784d6983f5e6722622b727d0987a15e\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE0ODU4YTJmMGM3M2FlNTE5\",\"nexusPath\":\"/configText/202005-test/cloud01_dcs/dc_log4cpp.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"DCServer.cfg\",\"fileSize\":0,\"md5\":\"ce208427723a0ebc0fff405fd7c382dc\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjYwNGZjN2U3YWFlN2YwYWYy\",\"nexusPath\":\"/configText/202005-test/cloud01_dcs/DCServer.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"155:21974\"},{\"addDelay\":20,\"appAlias\":\"cms1\",\"appName\":\"cmsserver\",\"appRunner\":\"cms1\",\"basePath\":\"/home/ccodrunner/Platform/\",\"cfgs\":[{\"deployPath\":\"./etc/\",\"ext\":\"xml\",\"fileName\":\"beijing.xml\",\"fileSize\":0,\"md5\":\"4074321f266b42fe7d7266b6fa9d7ca2\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5NmMwMTdkNWU4ZjE3NGRi\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/beijing.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./etc/\",\"ext\":\"cms2\",\"fileName\":\"config.cms2\",\"fileSize\":0,\"md5\":\"cf032451250db89948f775e4d7799e40\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZlYmViNGRkNzM3YjM5MzE5\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/config.cms2\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./etc/\",\"ext\":\"cfg\",\"fileName\":\"cms_log4cpp.cfg\",\"fileSize\":0,\"md5\":\"b16210d40a7ef123eef0296393df37b8\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmMDEzYTFjMTFkNzBlNDkz\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/cms_log4cpp.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\"},{\"addDelay\":20,\"appAlias\":\"cms2\",\"appName\":\"cmsserver\",\"appRunner\":\"cms2\",\"basePath\":\"/home/ccodrunner/Platform/\",\"cfgs\":[{\"deployPath\":\"./etc/\",\"ext\":\"xml\",\"fileName\":\"beijing.xml\",\"fileSize\":0,\"md5\":\"4074321f266b42fe7d7266b6fa9d7ca2\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM0OTk5N2I3NjRhZWIzNDg5\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/beijing.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./etc/\",\"ext\":\"cfg\",\"fileName\":\"cms_log4cpp.cfg\",\"fileSize\":0,\"md5\":\"b16210d40a7ef123eef0296393df37b8\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQxN2NjMTNmZmJhYjRlMWZh\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/cms_log4cpp.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./etc/\",\"ext\":\"cms2\",\"fileName\":\"config.cms2\",\"fileSize\":0,\"md5\":\"5f5e2e498e5705b84297b2721fdbb603\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFiODMyZmE4OTU2MmM3NmNh\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/config.cms2\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\"},{\"addDelay\":20,\"appAlias\":\"ucx\",\"appName\":\"ucxserver\",\"appRunner\":\"ucx\",\"basePath\":\"/home/ccodrunner/Platform/\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"ucx\",\"fileName\":\"config.ucx\",\"fileSize\":0,\"md5\":\"0c7c8b38115a9d0cabb2d1505f195821\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZlNTVjMGQzNzkxMjA3MzYw\",\"nexusPath\":\"/configText/202005-test/cloud01_ucx/config.ucx\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"1fef2157ea07c483979b424c758192bd709e6c2a\"},{\"addDelay\":20,\"appAlias\":\"daengine\",\"appName\":\"daengine\",\"appRunner\":\"daengine\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dae.cfg\",\"fileSize\":0,\"md5\":\"431128629db6c93804b86cc1f9428a87\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE1ODQyOGRjYjc2YzRjODdj\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dae_logger.cfg\",\"fileSize\":0,\"md5\":\"ac2fde58b18a5ab1ee66d911982a326c\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3Mzc5Y2E3NzU2ZjczMDEy\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_logger.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dae_config.cfg\",\"fileSize\":0,\"md5\":\"04544c8572c42b176d501461168dacf4\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxM2UzYmZmYzhmOGY2NWMw\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_config.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dae_log4cpp.cfg\",\"fileSize\":0,\"md5\":\"ece32d86439201eefa186fbe8ad6db06\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ2OWMwMGQ4YTFlZjRhZDQ4\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_log4cpp.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"179:20744\"},{\"addDelay\":20,\"appAlias\":\"dcproxy\",\"appName\":\"dcproxy\",\"appRunner\":\"dcproxy\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dcp_config.cfg\",\"fileSize\":0,\"md5\":\"087cb6d8e6263dc6f1e8079fac197983\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM5YThlM2QxMjI2NDc3NzZh\",\"nexusPath\":\"/configText/202005-test/cloud01_dcproxy/dcp_config.cfg\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"dcp_logger.cfg\",\"fileSize\":0,\"md5\":\"8d3d4de160751677d6a568c9d661d7c0\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEyZmRkNzJmMWJjZDMwYWNj\",\"nexusPath\":\"/configText/202005-test/cloud01_dcproxy/dcp_logger.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"195:21857\"},{\"addDelay\":0,\"appAlias\":\"ss\",\"appName\":\"StatSchedule\",\"appRunner\":\"ss\",\"basePath\":\"/home/ccodrunner/Platform\",\"cfgs\":[{\"deployPath\":\"./cfg/\",\"ext\":\"cfg\",\"fileName\":\"ss_config.cfg\",\"fileSize\":0,\"md5\":\"825e14101d79c733b2ea8becb8ea4e3b\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5YTgxMTU1YjhmNDMyZjU4\",\"nexusPath\":\"/configText/202005-test/cloud01_ss/ss_config.cfg\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"154:21104\"}],\"bkSetName\":\"域服务\",\"comment\":\"clone from 域服务01 of 123456-wuph\",\"createTime\":1591061579289,\"domainId\":\"cloud01\",\"domainName\":\"域服务01\",\"executeTime\":1591061579289,\"maxOccurs\":1000,\"occurs\":600,\"status\":\"CREATE\",\"tags\":\"入呼叫,外呼\",\"updateTime\":1591061579289,\"updateType\":\"ADD\"},{\"appUpdateOperationList\":[{\"addDelay\":0,\"appAlias\":\"cas\",\"appName\":\"cas\",\"appRunner\":\"cas\",\"basePath\":\"/home/portal/tomcat/webapps/\",\"cfgs\":[{\"deployPath\":\"./cas/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE2MDA0MWExZjQ1ODc4Njhh\",\"nexusPath\":\"/configText/202005-test/manage01_cas/web.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./cas/WEB-INF/\",\"ext\":\"properties\",\"fileName\":\"cas.properties\",\"fileSize\":0,\"md5\":\"6622e01a4df917d747e078e89c774a52\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3N2E3NDBhYmM0NzU2NDg5\",\"nexusPath\":\"/configText/202005-test/manage01_cas/cas.properties\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"10973\"},{\"addDelay\":0,\"appAlias\":\"customwebservice\",\"appName\":\"customWebservice\",\"appRunner\":\"customwebservice\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./customWebservice/WEB-INF/classes/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"96e5bc553847dab185d32c260310bb77\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRiNGZjNzM0NjU4MDMyNTZl\",\"nexusPath\":\"/configText/202005-test/manage01_customwebservice/web.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./customWebservice/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"24eebd53ad6d6d2585f8164d189b4592\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM3ZDgwMWZlMjNlNjU1MWNk\",\"nexusPath\":\"/configText/202005-test/manage01_customwebservice/config.properties\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"19553\"},{\"addDelay\":0,\"appAlias\":\"dcms\",\"appName\":\"dcms\",\"appRunner\":\"dcms\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./dcms/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"52ba707ab07e7fcd50d3732268dd9b9d\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZiMDVmY2FhMjFmZmNhN2Iz\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/web.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcms/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"98a8781d1808c69448c9666642d7b8ed\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM5NTA4MmY4ODU2NmJhZmJj\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/config.properties\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcms/WEB-INF/classes/\",\"ext\":\"xml\",\"fileName\":\"Param-Config.xml\",\"fileSize\":0,\"md5\":\"9a977ea04c6e936307bec2683cadd379\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFlODhlMDRiZWM1NTBkZTc0\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/Param-Config.xml\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"11110\"},{\"addDelay\":0,\"appAlias\":\"dcmsrecord\",\"appName\":\"dcmsRecord\",\"appRunner\":\"dcmsrecord\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./dcmsRecord/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"a4500823701a6b430a98b25eeee6fea3\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY4YjgyMjllYTBmNzcwYzI4\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/web.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsRecord/WEB-INF/classes/\",\"ext\":\"xml\",\"fileName\":\"applicationContext.xml\",\"fileSize\":0,\"md5\":\"5a355d87e0574ffa7bc120f61d8bf61e\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEwODE2YjIwM2VjNmEzYjA0\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/applicationContext.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsRecord/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"830bf1a0205f407eba5f3a449b749cba\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRlOGM3ZWI3YmIwMGI5ZDJk\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/config.properties\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"21763\"},{\"addDelay\":0,\"appAlias\":\"dcmssg\",\"appName\":\"dcmssg\",\"appRunner\":\"dcmssg\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./dcmsSG/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"e76da17fe273dc7f563a9c7c86183d20\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ0Y2ZlOWIxNTkzOWVhZTYz\",\"nexusPath\":\"/configText/202005-test/manage01_dcmssg/config.properties\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsSG/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"52a87ceaeebd7b9bb290ee863abe98c9\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMyYWJhYzhlZWNiYjdmNTAw\",\"nexusPath\":\"/configText/202005-test/manage01_dcmssg/web.xml\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"20070\"},{\"addDelay\":0,\"appAlias\":\"dcmsstatics\",\"appName\":\"dcmsStatics\",\"appRunner\":\"dcmsstaticsreport2\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./dcmsStatics/WEB-INF/classes/\",\"ext\":\"xml\",\"fileName\":\"applicationContext.xml\",\"fileSize\":0,\"md5\":\"2a7ef2d3a9fc97e8e59db7f21b7d4d45\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNkZmI4MWI4ODk5NjNkZDZk\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/applicationContext.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsStatics/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"7212df6a667e72ecb604b03fee20f639\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY0ZDc1YmFiZTlkMDUxYTRi\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/web.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsStatics/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"973ba4d65b93a47bb5ead294b9415e68\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4ODkyNWYwNTA1ZTk1NWJi\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/config.properties\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"20537\"},{\"addDelay\":0,\"appAlias\":\"dcmsstaticsreport\",\"appName\":\"dcmsStaticsReport\",\"appRunner\":\"dcmsstaticsreport1\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./dcmsStaticsReport/WEB-INF/classes/\",\"ext\":\"xml\",\"fileName\":\"applicationContext.xml\",\"fileSize\":0,\"md5\":\"1e2f67f773110caf7a91a1113564ce4c\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3MzVkODMwZTVlMTFkZTRk\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/applicationContext.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsStaticsReport/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"f02bb5a99546b80a3a82f55154be143d\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEyYzVkMWFhMDExOTUwODQz\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/config.properties\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsStaticsReport/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"5a32768163ade7c4bce70270d79b6c66\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRlOGM5MTIwOTY2ZWRkMjZk\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/web.xml\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"20528\"},{\"addDelay\":0,\"appAlias\":\"dcmswebservice\",\"appName\":\"dcmsWebservice\",\"appRunner\":\"dcmswebservice\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./dcmsWebservice/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"9a9671156ab2454951b9561fbefeed42\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxYjBmNjYxYzFmNDljYTkx\",\"nexusPath\":\"/configText/202005-test/manage01_dcmswebservice/config.properties\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsWebservice/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"d93dd1fe127f46a13f27d6f8d4a7def3\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ3YzI3NWZhZTNlMjFkMmVm\",\"nexusPath\":\"/configText/202005-test/manage01_dcmswebservice/web.xml\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"20503\"},{\"addDelay\":0,\"appAlias\":\"dcmsx\",\"appName\":\"dcmsx\",\"appRunner\":\"dcmsx\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./dcmsx/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"5d6550ab653769a49006b4957f9a0a65\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3OWVmMzQyNGYzODg0ODBi\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsx/web.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./dcmsx/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"application.properties\",\"fileSize\":0,\"md5\":\"675edca3ccfa6443d8dfc9b34b1eee0b\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4MmE0YzY0YjQxNDc1ODU4\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsx/application.properties\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"master_8efabf4\"},{\"addDelay\":0,\"appAlias\":\"safetymonitor\",\"appName\":\"safetyMonitor\",\"appRunner\":\"safetymonitor\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./safetyMonitor/WEB-INF/classes/\",\"ext\":\"xml\",\"fileName\":\"applicationContext.xml\",\"fileSize\":0,\"md5\":\"4543cb1aba46640edc3e815750fd3a94\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY5ZDNkNTQ5NTk5Yjg0Mjc2\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/applicationContext.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./safetyMonitor/WEB-INF/classes/\",\"ext\":\"properties\",\"fileName\":\"config.properties\",\"fileSize\":0,\"md5\":\"752b9c6cc870d294fa413d64c090e49e\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmYjk0NDEwZTVlN2NkMjIw\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/config.properties\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"./safetyMonitor/WEB-INF/\",\"ext\":\"xml\",\"fileName\":\"web.xml\",\"fileSize\":0,\"md5\":\"4d952d3e6d356156dd461144416f4816\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ1MjYyN2JlYzdjMGI0ZmRl\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/web.xml\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"20383\"}],\"bkSetName\":\"管理门户\",\"comment\":\"clone from 管理门户01 of 123456-wuph\",\"createTime\":1591061579289,\"domainId\":\"manage01\",\"domainName\":\"管理门户01\",\"executeTime\":1591061579289,\"maxOccurs\":1000,\"occurs\":600,\"status\":\"CREATE\",\"tags\":\"入呼叫,外呼\",\"updateTime\":1591061579289,\"updateType\":\"ADD\"},{\"appUpdateOperationList\":[{\"addDelay\":0,\"appAlias\":\"gls\",\"appName\":\"gls\",\"appRunner\":\"gls\",\"basePath\":\"/home/ccodrunner/resin-4.0.13/webapps/\",\"cfgs\":[{\"deployPath\":\"./gls/WEB-INF/classes/\",\"ext\":\"xml\",\"fileName\":\"Param-Config.xml\",\"fileSize\":0,\"md5\":\"1da62c81dacf6d7ee21fca3384f134c5\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4M2U3MDJjNjViN2E5MjQw\",\"nexusPath\":\"/configText/202005-test/ops01_gls/Param-Config.xml\",\"nexusRepository\":\"tmp\"}],\"hostIp\":\"10.130.41.218\",\"operation\":\"ADD\",\"platformAppId\":0,\"targetVersion\":\"10309\"}],\"bkSetName\":\"运营门户\",\"comment\":\"clone from 运营门户01 of 123456-wuph\",\"createTime\":1591061579290,\"domainId\":\"ops01\",\"domainName\":\"运营门户01\",\"executeTime\":1591061579290,\"maxOccurs\":1000,\"occurs\":600,\"status\":\"CREATE\",\"tags\":\"入呼叫,外呼\",\"updateTime\":1591061579290,\"updateType\":\"ADD\"}],\"executeTime\":1591061579290,\"glsDBPwd\":\"ccod\",\"glsDBType\":\"ORACLE\",\"glsDBUser\":\"ccod\",\"k8sHostIp\":\"10.130.41.218\",\"platformId\":\"202005-test\",\"platformName\":\"工具组平台\",\"publicConfig\":[{\"deployPath\":\"/root/resin-4.0.13/conf\",\"fileName\":\"local_datasource.xml\",\"fileSize\":0,\"md5\":\"112940181aeb983baa9d7fd2733f194f\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmNmE4MWY3MWI3MmJlY2Ji\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_datasource.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"/root/resin-4.0.13/conf\",\"fileName\":\"local_jvm.xml\",\"fileSize\":0,\"md5\":\"d172a5321944aba5bc19c35d00950afc\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRjMWQyMTIwNWRmYmY1MDM0\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_jvm.xml\",\"nexusRepository\":\"tmp\"},{\"deployPath\":\"/usr/local/lib\",\"fileName\":\"tnsnames.ora\",\"fileSize\":0,\"md5\":\"811f7f9472d5f6e733d732619a17ac77\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY0NzFkOTU3MGMyODRjMGJm\",\"nexusPath\":\"/configText/202005-test/publicConfig/tnsnames.ora\",\"nexusRepository\":\"tmp\"}],\"schemaId\":\"e2b849bd-0ac4-4591-97f3-eee6e7084a58\",\"status\":\"CREATE\",\"taskType\":\"CREATE\",\"title\":\"新建工具组平台(202005-test)计划\",\"updateTime\":1591061579290}";
        PlatformUpdateSchemaInfo createSchema = JSONObject.parseObject(jsonStr, PlatformUpdateSchemaInfo.class);
        String platformId = createSchema.getPlatformId();
        String k8sApiUrl = createSchema.getK8sApiUrl();
        String k8sAuthToken = createSchema.getK8sAuthToken();
        List<V1ConfigMap> mapList = this.k8sApiService.listNamespacedConfigMap(platformId, k8sApiUrl, k8sAuthToken);
        for(V1ConfigMap configMap : mapList)
        {
            this.k8sApiService.deleteConfigMapByName(platformId, configMap.getMetadata().getName(), k8sApiUrl, k8sAuthToken);
        }
        mapList = this.createConfigMapForNewPlatform(createSchema);
        System.out.println(String.format("create %d configMap for %s", mapList.size(), platformId));
    }

    private K8sPlatformParamVo getK8sPlatformParam(String platformId, String platformName, String ccodVersion, List<V1Deployment> deployments, List<V1Service> services, List<V1ConfigMap> configMaps, List<AppModuleVo> registerApps) throws IOException, K8sDataException, ParamException, NotSupportAppException, InterfaceCallException, NexusException
    {
        K8sPlatformParamVo paramVo = new K8sPlatformParamVo(platformId, platformName);
        Map<V1Service, V1Deployment> svcDeployMap = new HashMap<>();
        List<V1Service> notDeploySvcList = new ArrayList<>();
        for(V1Service service : services)
        {
            for(V1Deployment deployment : deployments)
            {
                boolean isMatch = isSelected(deployment.getMetadata().getLabels(), service.getSpec().getSelector());
                if(isMatch && svcDeployMap.containsKey(service))
                    throw new K8sDataException(String.format("service %s has related to deployment %s and %s",
                            service.getMetadata().getName(), deployment.getMetadata().getName(), svcDeployMap.get(service).getMetadata().getName()));
                else if(isMatch)
                    svcDeployMap.put(service, deployment);
            }
            if(!svcDeployMap.containsKey(service))
                notDeploySvcList.add(service);
        }
        Map<V1Deployment, List<V1Service>> deploySvcMap = new HashMap<>();
        for(V1Deployment deployment : deployments)
        {
            deploySvcMap.put(deployment, new ArrayList<>());
            for(V1Service service : services)
            {
                boolean isMatch = isSelected(deployment.getMetadata().getLabels(), service.getSpec().getSelector());
                if(isMatch)
                    deploySvcMap.get(deployment).add(service);
            }
        }
        Map<String, V1ConfigMap> configMapMap = new HashMap<>();
        for(V1ConfigMap configMap : configMaps)
            configMapMap.put(configMap.getMetadata().getName(), configMap);
        SimpleDateFormat sf = new SimpleDateFormat();
        Date now = new Date();
        String tag = sf.format(now);
        for(V1Deployment deployment : deployments)
        {
            String deploymentName = deployment.getMetadata().getName();
            List<V1Container> containers = new ArrayList<>();
            if(deployment.getSpec().getTemplate().getSpec().getInitContainers() != null)
                containers.addAll(deployment.getSpec().getTemplate().getSpec().getInitContainers());
            if(deployment.getSpec().getTemplate().getSpec().getContainers() != null)
                containers.addAll(deployment.getSpec().getTemplate().getSpec().getContainers());
            for(V1Container container : containers)
            {
                AppType appType = getAppTypeFromImageUrl(container.getImage());
                switch (appType)
                {
                    case CCOD_KERNEL_MODULE:
                    case CCOD_WEBAPPS_MODULE:
                    {
                        AppModuleVo moduleVo = getAppModuleFromImageTag(container.getImage(), registerApps);
                        if(paramVo.getDeployAppList().stream().collect(Collectors.toMap(PlatformAppDeployDetailVo::getAppName, Function.identity())).containsKey(moduleVo.getAppName()))
                            throw new K8sDataException(String.format("deployment %s has duplicate module %s", deploymentName, moduleVo.getAppName()));
                        PlatformAppDeployDetailVo deployApp = getK8sPlatformAppDeployDetail(platformName, ccodVersion, deployment, container, deploySvcMap.get(deployment), moduleVo, configMapMap.get(container.getName()).getData());
                        String directory = deployApp.getCfgNexusDirectory(tag);
                        List<NexusAssetInfo> assets = uploadK8sConfigMapToNexus(configMapMap.get(container.getName()), directory);
                        List<PlatformAppCfgFilePo> cfgs = new ArrayList<>();
                        String deployPath = container.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get(String.format("%s-volume", container.getName())).getMountPath();
                        for(NexusAssetInfo assetInfo : assets)
                        {
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
        for(V1Service threeSvc : notDeploySvcList)
        {
            PlatformThreePartServicePo threePartServicePo = getK8sPlatformThreePartService(threeSvc);
            paramVo.getThreeSvcList().add(threePartServicePo);
        }
        if(configMapMap.containsKey(platformId))
        {
            V1ConfigMap configMap = configMapMap.get(platformId);
            String directory = String.format("%s/%s/publicConfig", platformId, tag);
            List<NexusAssetInfo> assets = uploadK8sConfigMapToNexus(configMap, directory);
            for(NexusAssetInfo asset : assets)
            {
                PlatformPublicConfigPo cfgPo = new PlatformPublicConfigPo(platformId, "/", asset);
                paramVo.getPlatformPublicConfigList().add(cfgPo);
            }
        }
        Map<String, List<PlatformAppDeployDetailVo>> domainAppMap = paramVo.getDeployAppList().stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainId));
        for(String domainId : domainAppMap.keySet())
        {
            if(configMapMap.containsKey(domainId))
            {
                V1ConfigMap configMap = configMapMap.get(domainId);
                String directory = String.format("%s/%s/%s/publicConfig", platformId, tag, domainId);
                List<NexusAssetInfo> assets = uploadK8sConfigMapToNexus(configMap, directory);
                for(NexusAssetInfo asset : assets)
                {
                    DomainPublicConfigPo cfgPo = new DomainPublicConfigPo(domainId, platformId, "/", asset);
                    paramVo.getDomainPublicConfigList().add(cfgPo);
                }
            }
        }
        return paramVo;
    }

    /**
     * 如果labels包含selector的所有key，并且值也相同返回true, 否则返回false
     * @param labels
     * @param selector
     * @return 比较结果
     */
    boolean isSelected(Map<String, String> labels, Map<String, String> selector)
    {
        for(String key : selector.keySet())
        {
            if(!labels.containsKey(key) || !selector.get(key).equals(labels.get(key)))
                return false;
        }
        return true;
    }

    /**
     * 从k8s服务中获取第三方服务信息
     * @param service k8s服务
     * @return 第三方服务信息
     */
    private PlatformThreePartServicePo getK8sPlatformThreePartService(V1Service service)
    {
        PlatformThreePartServicePo po = new PlatformThreePartServicePo();
        po.setServiceName(service.getMetadata().getName());
        po.setPlatformId(service.getMetadata().getNamespace());
        po.setHostIp(service.getSpec().getClusterIP());
        po.setPort(getPortFromK8sService(Arrays.asList(service)));
        return po;
    }

    /**
     * 获取第三方应用相关信息
     * @param deployment 定义第三方应用的deployment
     * @param container 用来运行第三方应用的container
     * @param services 第三方应用关联的服务信息
     * @return 第三方应用相关信息
     */
    private PlatformThreePartAppPo getK8sPlatformThreePartApp(V1Deployment deployment, V1Container container, List<V1Service> services)
    {
        PlatformThreePartAppPo po = new PlatformThreePartAppPo();
        int replicas = deployment.getStatus().getReplicas() != null ? deployment.getStatus().getReplicas() : 0;
        int availableReplicas = deployment.getStatus().getAvailableReplicas() != null ? deployment.getStatus().getAvailableReplicas() : 0;
        int unavailableReplicas = deployment.getStatus().getUnavailableReplicas() != null ? deployment.getStatus().getUnavailableReplicas() : 0;
        po.setReplicas(replicas);
        po.setAvailableReplicas(availableReplicas);
        String status = "Running";
        if(unavailableReplicas == replicas)
            status = "Error";
        else if(availableReplicas < replicas)
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
     * @param platformName 平台名
     * @param ccodVersion 平台的ccod大版本
     * @param deployment 定义ccod域模块的deployment
     * @param container 加载ccod域模块的镜像容器
     * @param services 关联模块的服务
     * @param appModuleVo ccod域模块定义
     * @param configMap 该域模块对应的configMap
     * @return ccod域模块明细
     * @throws ParamException
     */
    private PlatformAppDeployDetailVo getK8sPlatformAppDeployDetail(String platformName, String ccodVersion, V1Deployment deployment, V1Container container, List<V1Service> services, AppModuleVo appModuleVo, Map<String, String> configMap) throws ParamException
    {
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
        if(unavailableReplicas == replicas)
            status = "Error";
        else if(availableReplicas < replicas)
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
     * @param configMap 需要上传配置文件的configMap信息
     * @param directory 保存配置文件的路径
     * @return 上传结果
     * @throws IOException
     * @throws InterfaceCallException
     * @throws NexusException
     */
    private List<NexusAssetInfo> uploadK8sConfigMapToNexus(V1ConfigMap configMap, String directory) throws IOException, InterfaceCallException, NexusException
    {
        List<DeployFileInfo> fileList = new ArrayList<>();
        String saveDir = getTempSaveDir(this.platformAppCfgRepository);
        for(String fileName : configMap.getData().keySet())
        {
            String content = configMap.getData().get(fileName);
            String fileSavePath = String.format("%s/%s", saveDir, fileName);
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
     * @param directory
     * @return 临时存放路径
     */
    private String getTempSaveDir(String directory) {
        String saveDir = String.format("%s/downloads/%s", System.getProperty("user.dir"), directory);
        return saveDir;
    }

    /**
     * 根据basePath获得相对路径代表的绝对路径
     * @param basePath basePath
     * @param relativePath 需要被转换成绝对路径的相对路径
     * @return 相对路径对应的绝对路径
     * @throws ParamException
     */
    private String getAbsolutePath(String basePath, String relativePath) throws ParamException
    {
        if(relativePath.matches("^/.*"))
            return relativePath;
        else if(relativePath.matches("^\\./.*"))
            return String.format("%s/%s", basePath, relativePath.replaceAll("^\\./", "")).replaceAll("//", "/");
        else if(relativePath.matches("^\\.\\./.*"))
        {
            int count = 1;
            String str = relativePath.replaceAll("^\\.\\./", "");
            while (str.matches("^\\.\\./.*"))
            {
                count++;
                str = str.replaceAll("^\\.\\./", "");
            }
            String[] arr = basePath.replaceAll("^/", "").replaceAll("/$", "").split("/");
            String abPath = "/";
            for(int i = 0; i < arr.length - count; i++)
                abPath = String.format("%s%s/", abPath, arr[i]);
            return String.format("%s%s", abPath, str);
        }
        else
            return String.format("%s/%s", basePath, relativePath).replaceAll("//", "/");
    }

    /**
     * 将为容器挂载指定的配置文件信息
     * @param cfgs 需要加载的配置文件
     * @param basePath 部署配置文件的basePath
     * @param configMap 需要被挂载的配置文件
     * @param moduleVo 被挂载配置文件的ccod域模块定义
     * @param mountPath 挂载路径
     * @param isPublic 被挂载的配置文件是否是公共配置
     * @param container 需要挂载configMap的容器
     * @param deployment 容器所属的deployment
     */
    public void addModuleCfgToContainer(List<AppFileNexusInfo> cfgs, String basePath, String deployPath, V1ConfigMap configMap, AppModuleVo moduleVo, String mountPath, boolean isPublic, V1Container container, V1Deployment deployment) throws ParamException
    {
        String configName = configMap.getMetadata().getName();
        String volumeName = String.format("%s-volume", configName);
        if(!deployment.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity())).containsKey(volumeName))
        {
            V1ConfigMapVolumeSource source = new V1ConfigMapVolumeSource();
            source.setItems(new ArrayList<>());
            source.setName(configName);
            for(String fileName : configMap.getData().keySet())
            {
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
        List<String> command = container.getCommand();
        String execParam = "";
        Map<String, List<AppFileNexusInfo>> deployPathCfgMap = cfgs.stream().collect(Collectors.groupingBy(AppFileNexusInfo::getDeployPath));
        if(appType.equals(AppType.CCOD_KERNEL_MODULE) && !isPublic)
            execParam = String.format("%s;mkdir %s -p;mkdir %s/log -p;mv /opt/%s %s/%s", execParam, deployPath, basePath, moduleVo.getInstallPackage().getFileName(), deployPath, moduleVo.getInstallPackage().getFileName());
        else if(appType.equals(AppType.CCOD_WEBAPPS_MODULE) && !isPublic)
            execParam = String.format("%s;cd %s", execParam, deployPath);
        for(String cfgDeployPath : deployPathCfgMap.keySet())
        {
            String absolutePath = getAbsolutePath(basePath, cfgDeployPath);
            execParam = String.format("%s;mkdir %s -p", execParam, absolutePath);
            for(AppFileNexusInfo cfg : deployPathCfgMap.get(cfgDeployPath))
            {
                execParam = String.format("%s;cp %s/%s %s/%s", execParam, mountPath, cfg.getFileName(), absolutePath, cfg.getFileName());
                if(appType.equals(AppType.CCOD_WEBAPPS_MODULE) && !isPublic)
                    execParam = String.format("%s;jar uf %s %s/%s", execParam, moduleVo.getInstallPackage().getFileName(), absolutePath.replaceAll(String.format("^%s", deployPath), "").replaceAll("^/", ""), cfg.getFileName());
            }
        }
        execParam = execParam.replaceAll("^;", "").replaceAll("//", "/");
        if(isPublic)
        {
            if(container.getArgs() == null || container.getArgs().size() == 0) {
                container.setArgs(new ArrayList<>());
                execParam = execParam.replaceAll("^;", "").replaceAll("//", "/");
                container.getArgs().add(execParam);
            }
            else
            {
                execParam = String.format("%s;%s", execParam, container.getArgs().get(0));
                execParam = execParam.replaceAll("^;", "").replaceAll("//", "/");
                container.getArgs().set(0, execParam);
            }
        }
        else
        {
            if(container.getCommand() == null)
            {
                container.setCommand(new ArrayList<>());
                container.getCommand().add("/bin/sh");
                container.getCommand().add("-c");
                container.getCommand().add("");
            }
            execParam = String.format("%s;%s", container.getCommand().get(2), execParam);
            execParam = execParam.replaceAll("^;", "").replaceAll("//", "/");
            container.getCommand().set(2, execParam);
        }
        if(container.getArgs() != null && container.getArgs().size() > 0)
        {
            String[] arr = container.getArgs().get(0).split(";");
            List<String> strList = new ArrayList<>();
            for(String str : arr)
                if(StringUtils.isNotBlank(str) && str.indexOf("wget") < 0)
                    strList.add(str);
            container.getArgs().set(0, String.join(";", strList));
        }
    }


    private PlatformUpdateSchemaInfo generateDemoCreateSchema(String  srcPlatformId) throws Exception
    {
        Gson gson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
            @Override
            public boolean shouldSkipField(FieldAttributes f) {
                //过滤掉字段名包含"age"
                return f.getName().contains("creationTimestamp") || f.getName().contains("status") || f.getName().contains("resourceVersion") || f.getName().contains("selfLink") || f.getName().contains("uid")
                        || f.getName().contains("generation") || f.getName().contains("annotations") || f.getName().contains("strategy")
                        || f.getName().contains("terminationMessagePath") ||f.getName().contains("terminationMessagePolicy")
                        || f.getName().contains("dnsPolicy") || f.getName().contains("securityContext") || f.getName().contains("schedulerName")
                        || f.getName().contains("restartPolicy") || f.getName().contains("clusterIP")
                        || f.getName().contains("sessionAffinity") || f.getName().contains("nodePort");
            }

            @Override
            public boolean shouldSkipClass(Class<?> clazz) {
                //过滤掉 类名包含 Bean的类
                return clazz.getName().contains("Bean");
            }
        }).create();
        String jsonStr = "{\"schemaId\":\"e2b849bd-0ac4-4591-97f3-eee6e7084a58\",\"domainUpdatePlanList\":[{\"domainName\":\"公共组件01\",\"domainId\":\"public01\",\"bkSetName\":\"公共组件\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"LicenseServer\",\"appAlias\":\"licenseserver\",\"targetVersion\":\"5214\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"licenseserver\",\"cfgs\":[{\"fileName\":\"Config.ini\",\"ext\":\"ini\",\"fileSize\":0,\"md5\":\"6c513269c4e2bc10f4a6cf0eb05e5bfc\",\"deployPath\":\"./bin/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_licenseserver/Config.ini\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNlZGYyZTBkYjgyNWQ0OTRi\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"configserver\",\"appAlias\":\"configserver\",\"targetVersion\":\"aca2af60caa0fb9f4af57f37f869dafc90472525\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"configserver\",\"cfgs\":[{\"fileName\":\"ccs_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"1095494274dc98445b79ec1d32900a6f\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_configserver/ccs_config.cfg\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY5NWYyNTlkYWY3ZWEzZWNl\"},{\"fileName\":\"ccs_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"197075eb110327da19bfc2a31f24b302\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_configserver/ccs_logger.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ1NGYyYWEyOGE2ZGNhYjlh\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"glsServer\",\"appAlias\":\"glsserver\",\"targetVersion\":\"7b699a4aece10ef28dce83ab36e4d79213ec4f69\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"glsserver\",\"cfgs\":[{\"fileName\":\"gls_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"f23a83a2d871d59c89d12b0281e10e90\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_glsserver/gls_config.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmMmRiNjc0YzQ5YmE4Nzdj\"},{\"fileName\":\"gls_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"7b8e1879eab906cba05dabf3f6e0bc37\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/public01_glsserver/gls_logger.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFhYTlmZWY0MTJkMDY2ZTM3\"}],\"addDelay\":30}],\"updateType\":\"ADD\",\"createTime\":\"Jun 2, 2020 9:32:59 AM\",\"updateTime\":\"Jun 2, 2020 9:32:59 AM\",\"executeTime\":\"Jun 2, 2020 9:32:59 AM\",\"comment\":\"clone from 公共组件01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"域服务01\",\"domainId\":\"cloud01\",\"bkSetName\":\"域服务\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"DDSServer\",\"appAlias\":\"dds\",\"targetVersion\":\"150:18722\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"dds\",\"cfgs\":[{\"fileName\":\"dds_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"7f783a4ea73510c73ac830f135f4c762\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dds/dds_logger.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5MDNhNjZiNDlmZDMxNzYx\"},{\"fileName\":\"dds_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"d89e98072e96a06efa41c69855f4a3cc\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dds/dds_config.cfg\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZjOTYzMzczYzhmZjFjMTRm\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"UCDServer\",\"appAlias\":\"ucds\",\"targetVersion\":\"deb3c3c4bf62c5ae5b3f8a467029a03ed95fb39e\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"ucds\",\"cfgs\":[{\"fileName\":\"ucds_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"f4445f10c75c9ef2f6d4de739c634498\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/ucds_config.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxMzRlYzQyOWIwNzZlMDU0\"},{\"fileName\":\"ucds_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"ec57329ddcec302e0cc90bdbb8232a3c\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/ucds_logger.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFjYWUzYjQxZWU5NTMxOTg4\"},{\"fileName\":\"DRWRClient.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"8b901d87855de082318314d868664c03\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ucds/DRWRClient.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ2NWYyYzE5NTAwNDY4YzQ1\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcs\",\"appAlias\":\"dcs\",\"targetVersion\":\"155:21974\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"dcs\",\"cfgs\":[{\"fileName\":\"dc_log4cpp.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"5784d6983f5e6722622b727d0987a15e\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dcs/dc_log4cpp.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE0ODU4YTJmMGM3M2FlNTE5\"},{\"fileName\":\"DCServer.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"ce208427723a0ebc0fff405fd7c382dc\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dcs/DCServer.cfg\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjYwNGZjN2U3YWFlN2YwYWYy\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"cmsserver\",\"appAlias\":\"cms1\",\"targetVersion\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"cms1\",\"startCmd\":\"./cmsserver --config.main=../etc/config.cms2\",\"cfgs\":[{\"fileName\":\"beijing.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"4074321f266b42fe7d7266b6fa9d7ca2\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/beijing.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5NmMwMTdkNWU4ZjE3NGRi\"},{\"fileName\":\"config.cms2\",\"ext\":\"cms2\",\"fileSize\":0,\"md5\":\"cf032451250db89948f775e4d7799e40\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/config.cms2\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZlYmViNGRkNzM3YjM5MzE5\"},{\"fileName\":\"cms_log4cpp.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"b16210d40a7ef123eef0296393df37b8\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms1/cms_log4cpp.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmMDEzYTFjMTFkNzBlNDkz\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"cmsserver\",\"appAlias\":\"cms2\",\"targetVersion\":\"4c303e2a4b97a047f63eb01b247303c9306fbda5\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"cms2\",\"startCmd\":\"./cmsserver --config.main=../etc/config.cms2\",\"cfgs\":[{\"fileName\":\"beijing.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"4074321f266b42fe7d7266b6fa9d7ca2\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/beijing.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM0OTk5N2I3NjRhZWIzNDg5\"},{\"fileName\":\"cms_log4cpp.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"b16210d40a7ef123eef0296393df37b8\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/cms_log4cpp.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQxN2NjMTNmZmJhYjRlMWZh\"},{\"fileName\":\"config.cms2\",\"ext\":\"cms2\",\"fileSize\":0,\"md5\":\"5f5e2e498e5705b84297b2721fdbb603\",\"deployPath\":\"./etc/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_cms2/config.cms2\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFiODMyZmE4OTU2MmM3NmNh\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"ucxserver\",\"appAlias\":\"ucx\",\"targetVersion\":\"1fef2157ea07c483979b424c758192bd709e6c2a\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"ucx\",\"startCmd\":\"./ucxserver --config.main=../cfg/config.ucx\",\"cfgs\":[{\"fileName\":\"config.ucx\",\"ext\":\"ucx\",\"fileSize\":0,\"md5\":\"0c7c8b38115a9d0cabb2d1505f195821\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ucx/config.ucx\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZlNTVjMGQzNzkxMjA3MzYw\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"daengine\",\"appAlias\":\"daengine\",\"targetVersion\":\"179:20744\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"daengine\",\"cfgs\":[{\"fileName\":\"dae.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"431128629db6c93804b86cc1f9428a87\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE1ODQyOGRjYjc2YzRjODdj\"},{\"fileName\":\"dae_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"ac2fde58b18a5ab1ee66d911982a326c\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_logger.cfg\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3Mzc5Y2E3NzU2ZjczMDEy\"},{\"fileName\":\"dae_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"04544c8572c42b176d501461168dacf4\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_config.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxM2UzYmZmYzhmOGY2NWMw\"},{\"fileName\":\"dae_log4cpp.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"ece32d86439201eefa186fbe8ad6db06\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_daengine/dae_log4cpp.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ2OWMwMGQ4YTFlZjRhZDQ4\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcproxy\",\"appAlias\":\"dcproxy\",\"targetVersion\":\"195:21857\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"dcproxy\",\"cfgs\":[{\"fileName\":\"dcp_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"087cb6d8e6263dc6f1e8079fac197983\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dcproxy/dcp_config.cfg\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM5YThlM2QxMjI2NDc3NzZh\"},{\"fileName\":\"dcp_logger.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"8d3d4de160751677d6a568c9d661d7c0\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_dcproxy/dcp_logger.cfg\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEyZmRkNzJmMWJjZDMwYWNj\"}],\"addDelay\":20},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"StatSchedule\",\"appAlias\":\"ss\",\"targetVersion\":\"154:21104\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/root/Platform\",\"deployPath\":\"/root/Platform/bin\",\"appRunner\":\"ss\",\"cfgs\":[{\"fileName\":\"ss_config.cfg\",\"ext\":\"cfg\",\"fileSize\":0,\"md5\":\"825e14101d79c733b2ea8becb8ea4e3b\",\"deployPath\":\"./cfg/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/cloud01_ss/ss_config.cfg\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ5YTgxMTU1YjhmNDMyZjU4\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"createTime\":\"Jun 2, 2020 9:32:59 AM\",\"updateTime\":\"Jun 2, 2020 9:32:59 AM\",\"executeTime\":\"Jun 2, 2020 9:32:59 AM\",\"comment\":\"clone from 域服务01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"管理门户01\",\"domainId\":\"manage01\",\"bkSetName\":\"管理门户\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"cas\",\"appAlias\":\"cas\",\"targetVersion\":\"10973\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"cas\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"8ba7dddf4b7be9132e56841a7206ef74\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_cas/web.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE2MDA0MWExZjQ1ODc4Njhh\"},{\"fileName\":\"cas.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"6622e01a4df917d747e078e89c774a52\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_cas/cas.properties\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3N2E3NDBhYmM0NzU2NDg5\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"customWebservice\",\"appAlias\":\"customwebservice\",\"targetVersion\":\"19553\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"customwebservice\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"96e5bc553847dab185d32c260310bb77\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_customwebservice/web.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRiNGZjNzM0NjU4MDMyNTZl\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"24eebd53ad6d6d2585f8164d189b4592\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_customwebservice/config.properties\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM3ZDgwMWZlMjNlNjU1MWNk\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcms\",\"appAlias\":\"dcms\",\"targetVersion\":\"11110\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcms\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"52ba707ab07e7fcd50d3732268dd9b9d\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/web.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjZiMDVmY2FhMjFmZmNhN2Iz\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"98a8781d1808c69448c9666642d7b8ed\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/config.properties\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWM5NTA4MmY4ODU2NmJhZmJj\"},{\"fileName\":\"Param-Config.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"9a977ea04c6e936307bec2683cadd379\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcms/Param-Config.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWFlODhlMDRiZWM1NTBkZTc0\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcmsRecord\",\"appAlias\":\"dcmsrecord\",\"targetVersion\":\"21763\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmsrecord\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"a4500823701a6b430a98b25eeee6fea3\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/web.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY4YjgyMjllYTBmNzcwYzI4\"},{\"fileName\":\"applicationContext.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"5a355d87e0574ffa7bc120f61d8bf61e\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/applicationContext.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEwODE2YjIwM2VjNmEzYjA0\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"830bf1a0205f407eba5f3a449b749cba\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsrecord/config.properties\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRlOGM3ZWI3YmIwMGI5ZDJk\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcmssg\",\"appAlias\":\"dcmssg\",\"targetVersion\":\"20070\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmssg\",\"cfgs\":[{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"e76da17fe273dc7f563a9c7c86183d20\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmssg/config.properties\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ0Y2ZlOWIxNTkzOWVhZTYz\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"52a87ceaeebd7b9bb290ee863abe98c9\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmssg/web.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMyYWJhYzhlZWNiYjdmNTAw\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcmsStatics\",\"appAlias\":\"dcmsstatics\",\"targetVersion\":\"20537\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmsstaticsreport2\",\"cfgs\":[{\"fileName\":\"applicationContext.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"2a7ef2d3a9fc97e8e59db7f21b7d4d45\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/applicationContext.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNkZmI4MWI4ODk5NjNkZDZk\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"7212df6a667e72ecb604b03fee20f639\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/web.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY0ZDc1YmFiZTlkMDUxYTRi\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"973ba4d65b93a47bb5ead294b9415e68\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstatics/config.properties\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4ODkyNWYwNTA1ZTk1NWJi\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcmsStaticsReport\",\"appAlias\":\"dcmsstaticsreport\",\"targetVersion\":\"20528\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmsstaticsreport1\",\"cfgs\":[{\"fileName\":\"applicationContext.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"1e2f67f773110caf7a91a1113564ce4c\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/applicationContext.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3MzVkODMwZTVlMTFkZTRk\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"f02bb5a99546b80a3a82f55154be143d\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/config.properties\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWEyYzVkMWFhMDExOTUwODQz\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"5a32768163ade7c4bce70270d79b6c66\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsstaticsreport/web.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRlOGM5MTIwOTY2ZWRkMjZk\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcmsWebservice\",\"appAlias\":\"dcmswebservice\",\"targetVersion\":\"20503\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmswebservice\",\"cfgs\":[{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"9a9671156ab2454951b9561fbefeed42\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmswebservice/config.properties\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWMxYjBmNjYxYzFmNDljYTkx\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"d93dd1fe127f46a13f27d6f8d4a7def3\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmswebservice/web.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ3YzI3NWZhZTNlMjFkMmVm\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"dcmsx\",\"appAlias\":\"dcmsx\",\"targetVersion\":\"master_8efabf4\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"dcmsx\",\"cfgs\":[{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"5d6550ab653769a49006b4957f9a0a65\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsx/web.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY3OWVmMzQyNGYzODg0ODBi\"},{\"fileName\":\"application.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"675edca3ccfa6443d8dfc9b34b1eee0b\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_dcmsx/application.properties\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4MmE0YzY0YjQxNDc1ODU4\"}],\"addDelay\":0},{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"safetyMonitor\",\"appAlias\":\"safetymonitor\",\"targetVersion\":\"20383\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"safetymonitor\",\"cfgs\":[{\"fileName\":\"applicationContext.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"4543cb1aba46640edc3e815750fd3a94\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/applicationContext.xml\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY5ZDNkNTQ5NTk5Yjg0Mjc2\"},{\"fileName\":\"config.properties\",\"ext\":\"properties\",\"fileSize\":0,\"md5\":\"752b9c6cc870d294fa413d64c090e49e\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/config.properties\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmYjk0NDEwZTVlN2NkMjIw\"},{\"fileName\":\"web.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"4d952d3e6d356156dd461144416f4816\",\"deployPath\":\"./WEB-INF/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/manage01_safetymonitor/web.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGQ1MjYyN2JlYzdjMGI0ZmRl\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"createTime\":\"Jun 2, 2020 9:32:59 AM\",\"updateTime\":\"Jun 2, 2020 9:32:59 AM\",\"executeTime\":\"Jun 2, 2020 9:32:59 AM\",\"comment\":\"clone from 管理门户01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"},{\"domainName\":\"运营门户01\",\"domainId\":\"ops01\",\"bkSetName\":\"运营门户\",\"appUpdateOperationList\":[{\"platformAppId\":0,\"operation\":\"ADD\",\"appName\":\"gls\",\"appAlias\":\"gls\",\"targetVersion\":\"10309\",\"hostIp\":\"10.130.41.218\",\"basePath\":\"/opt\",\"deployPath\":\"/opt\",\"appRunner\":\"gls\",\"cfgs\":[{\"fileName\":\"Param-Config.xml\",\"ext\":\"xml\",\"fileSize\":0,\"md5\":\"1da62c81dacf6d7ee21fca3384f134c5\",\"deployPath\":\"./WEB-INF/classes/\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/ops01_gls/Param-Config.xml\",\"nexusAssetId\":\"dG1wOjg1MTM1NjUyYTk3OGJlOWE4M2U3MDJjNjViN2E5MjQw\"}],\"addDelay\":0}],\"updateType\":\"ADD\",\"createTime\":\"Jun 2, 2020 9:32:59 AM\",\"updateTime\":\"Jun 2, 2020 9:32:59 AM\",\"executeTime\":\"Jun 2, 2020 9:32:59 AM\",\"comment\":\"clone from 运营门户01 of 123456-wuph\",\"occurs\":600,\"maxOccurs\":1000,\"tags\":\"入呼叫,外呼\"}],\"platformId\":\"k8s-test\",\"platformName\":\"ccod开发测试平台\",\"bkBizId\":25,\"bkCloudId\":0,\"ccodVersion\":\"CCOD4.1\",\"taskType\":\"CREATE\",\"createTime\":\"Jun 2, 2020 9:32:59 AM\",\"updateTime\":\"Jun 2, 2020 9:32:59 AM\",\"title\":\"新建工具组平台(202005-test)计划\",\"comment\":\"create 工具组平台(202005-test) by clone 123456-wuph(ccod开发测试平台)\",\"k8sHostIp\":\"10.130.41.218\",\"glsDBType\":\"ORACLE\",\"glsDBUser\":\"ccod\",\"glsDBPwd\":\"ccod\",\"baseDataNexusRepository\":\"platform_base_data\",\"baseDataNexusPath\":\"ccod/4.1/baseVolume.zip\",\"publicConfig\":[{\"fileName\":\"local_datasource.xml\",\"fileSize\":0,\"md5\":\"112940181aeb983baa9d7fd2733f194f\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_datasource.xml\",\"nexusAssetId\":\"dG1wOmQ0ODExNzU0MWRjYjg5ZWNmNmE4MWY3MWI3MmJlY2Ji\"},{\"fileName\":\"local_jvm.xml\",\"fileSize\":0,\"md5\":\"d172a5321944aba5bc19c35d00950afc\",\"deployPath\":\"/root/resin-4.0.13/conf\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/local_jvm.xml\",\"nexusAssetId\":\"dG1wOjEzYjI5ZTQ0OWYwZTNiOGRjMWQyMTIwNWRmYmY1MDM0\"},{\"fileName\":\"tnsnames.ora\",\"fileSize\":0,\"md5\":\"811f7f9472d5f6e733d732619a17ac77\",\"deployPath\":\"/usr/local/lib\",\"nexusRepository\":\"tmp\",\"nexusPath\":\"/configText/202005-test/publicConfig/tnsnames.ora\",\"nexusAssetId\":\"dG1wOjBhYjgwYTc0MzkyMWU0MjY0NzFkOTU3MGMyODRjMGJm\"}],\"k8sApiUrl\":\"https://10.130.41.218:6443\",\"k8sAuthToken\":\"eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA\"}";
        PlatformUpdateSchemaInfo schemaInfo = gson.fromJson(jsonStr, PlatformUpdateSchemaInfo.class);
        if(schemaInfo.getPlatformId().equals(srcPlatformId))
            throw new Exception(String.format("created platformId equal src platformId %s", srcPlatformId));
        String createdPlatformId = schemaInfo.getPlatformId();
        for(DomainUpdatePlanInfo planInfo : schemaInfo.getDomainUpdatePlanList())
            for(AppUpdateOperationInfo optInfo : planInfo.getAppUpdateOperationList())
            {
                AppModuleVo moduleVo = this.appManagerService.queryAppByVersion(optInfo.getAppName(), optInfo.getTargetVersion());
                if(AppType.CCOD_WEBAPPS_MODULE.equals(moduleVo.getAppType())) {
                    optInfo.setDeployPath("/opt");
                    optInfo.setBasePath("/opt");
                    for(AppFileNexusInfo cfg : optInfo.getCfgs())
                        cfg.setDeployPath(cfg.getDeployPath().replaceAll("^.*WEB-INF/", "./WEB-INF/"));
                }
                else {
                    optInfo.setBasePath("/root/Platform");
                    optInfo.setDeployPath("./bin");
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
        jsonStr = gson.toJson(deployments);
        deployments = gson.fromJson(jsonStr, new TypeToken<List<V1Deployment>>(){}.getType());
        List<V1ConfigMap> configMaps = this.k8sApiService.listNamespacedConfigMap(platformId, k8sApiUrl, k8sAuthToken);
        Map<String, V1ConfigMap> configMapMap = new HashMap<>();
        for(V1ConfigMap configMap : configMaps)
            configMapMap.put(configMap.getMetadata().getName(), configMap);
        V1ConfigMap configMap = configMapMap.get(srcPlatformId);
        configMap.getMetadata().setNamespace(createdPlatformId);
        configMap.getMetadata().setName(createdPlatformId);
        configMapMap.put(createdPlatformId, configMap);
        configMapMap.remove(srcPlatformId);
        Map<String, List<AppFileNexusInfo>> cfgMap = new HashMap<>();
        if(schemaInfo.getPublicConfig() != null && schemaInfo.getPublicConfig().size() > 0)
            cfgMap.put(platformId, schemaInfo.getPublicConfig());
        cfgMap.put(createdPlatformId, cfgMap.get(srcPlatformId));
        cfgMap.remove(srcPlatformId);
        for(DomainUpdatePlanInfo planInfo : schemaInfo.getDomainUpdatePlanList())
        {
            String domainId = planInfo.getDomainId();
            if(planInfo.getPublicConfig() != null && planInfo.getPublicConfig().size() > 0)
                cfgMap.put(domainId, planInfo.getPublicConfig());
            for(AppUpdateOperationInfo optInfo : planInfo.getAppUpdateOperationList())
            {
                String alias = optInfo.getAppAlias();
                cfgMap.put(String.format("%s-%s", alias, domainId), optInfo.getCfgs());
            }
        }
        Map<String, V1Deployment> deploymentMap = new HashMap<>();
        for(V1Deployment deployment : deployments)
        {
            List<V1Container> containers = new ArrayList<>();
            if(deployment.getSpec().getTemplate().getSpec().getInitContainers() != null)
                containers.addAll(deployment.getSpec().getTemplate().getSpec().getInitContainers());
            if(deployment.getSpec().getTemplate().getSpec().getContainers() != null)
                containers.addAll(deployment.getSpec().getTemplate().getSpec().getContainers());
            for(V1Container container : containers)
            {
                AppType appType = getAppTypeFromImageUrl(container.getImage());
                if(appType.equals(AppType.CCOD_WEBAPPS_MODULE) || appType.equals(AppType.CCOD_KERNEL_MODULE))
                {
                    deployment.getMetadata().setNamespace(createdPlatformId);
                    String deploymentName = deployment.getMetadata().getName();
                    String domainId = deploymentName.split("\\-")[1];
                    deployment.getMetadata().getLabels().clear();
                    deployment.getMetadata().getLabels().put("domain-id", domainId);
                    deployment.getMetadata().getLabels().put("type", "CCODDomainModule");
                    String alias = deploymentName.split("\\-")[0];
                    container.setName(alias);
                    List<AppUpdateOperationInfo> optList = schemaInfo.getDomainUpdatePlanList().stream().collect(Collectors.toMap(DomainUpdatePlanInfo::getDomainId, Function.identity())).get(domainId).getAppUpdateOperationList();
                    optList.stream().collect(Collectors.toMap(AppUpdateOperationInfo::getAppAlias, Function.identity())).get(alias).setAssembleTag(deploymentName);
                    AppModuleVo moduleVo = getAppModuleFromImageTag(container.getImage(), registerApps);
                    Map<String, String> labels = new HashMap<>();
                    labels.put("domain-id", domainId);
                    labels.put(moduleVo.getAppName(), alias);
                    deployment.getSpec().getSelector().setMatchLabels(labels);
                    deployment.getSpec().getTemplate().getMetadata().setLabels(labels);
                }
                else if(AppType.THREE_PART_APP.equals(appType))
                {
                    deployment.getMetadata().getLabels().put("type", "ThreePartApp");
                    deployment.getMetadata().setNamespace(createdPlatformId);
                    for(V1Volume volume : deployment.getSpec().getTemplate().getSpec().getVolumes())
                    {
                        if(volume.getName().equals("sql"))
                            volume.getPersistentVolumeClaim().claimName("base-volume-k8s-test");
                    }
                }
                if(appType.equals(AppType.CCOD_WEBAPPS_MODULE) && deployment.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity())).containsKey("data"))
                    deployment.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity())).get("data").getPersistentVolumeClaim().setClaimName("base-volume-k8s-test");
                deploymentMap.put(deployment.getMetadata().getName(), deployment);
            }
        }
        schemaInfo.setK8sDeploymentList(new ArrayList<>());
        List<V1Deployment> threeSvcDpList = new ArrayList<>();
        List<V1Deployment> moduleDpList = new ArrayList<>();
        for(V1Deployment deployment : deployments)
        {
//            if(!"licenseserver-public01".equals(deployment.getMetadata().getName()))
//                continue;
            if(deployment.getMetadata().getLabels().get("type").equals("CCODDomainModule"))
            {
                String domainId = deployment.getMetadata().getLabels().get("domain-id");
                generateConfigForCCODDomainModuleDeployment(deployment, configMapMap, cfgMap, schemaInfo.getDomainUpdatePlanList().stream().collect(Collectors.toMap(DomainUpdatePlanInfo::getDomainId, Function.identity())).get(domainId).getAppUpdateOperationList(), registerApps);
                moduleDpList.add(deployment);
            }
            else if(deployment.getMetadata().getName().equals("mysql-5-7-29") || deployment.getMetadata().getName().equals("oracle"))
            {
                threeSvcDpList.add(deployment);
            }
        }
        schemaInfo.getK8sDeploymentList().addAll(threeSvcDpList);
        schemaInfo.getK8sDeploymentList().addAll(moduleDpList);
        List<V1Service> services = this.k8sApiService.listNamespacedService(srcPlatformId, k8sApiUrl, k8sAuthToken);
        services = gson.fromJson(gson.toJson(services), new TypeToken<List<V1Service>>(){}.getType());
        schemaInfo.setK8sServiceList(new ArrayList<>());
        Map<String, V1Service> serviceMap = new HashMap<>();
        for(V1Service service : services)
        {
            serviceMap.put(service.getMetadata().getName(), service);
            service.getMetadata().setNamespace(createdPlatformId);
            String[] arr = service.getMetadata().getName().split("\\-");
            if(arr.length != 2)
                continue;
            String alias = arr[0];
            String domainId = arr[1];
            String appName = getAppNameFromDomainIdAndAlias(domainId, alias);
            Map<String, String> selector = new HashMap<>();
            selector.put("domain-id", domainId);
            selector.put(appName, alias);
            service.getSpec().setSelector(selector);
        }
        schemaInfo.getK8sServiceList().addAll(services);
        schemaInfo.setK8sEndpointsList(new ArrayList<>());
        List<V1Endpoints> endList = this.k8sApiService.listNamespacedEndpoints(srcPlatformId, k8sApiUrl, k8sAuthToken);
        endList = gson.fromJson(gson.toJson(endList), new TypeToken<List<V1Endpoints>>(){}.getType());
        for(V1Endpoints ends : endList)
        {
            ends.getMetadata().setNamespace(createdPlatformId);
            schemaInfo.getK8sEndpointsList().add(ends);
        }
        Map<String, ExtensionsV1beta1Ingress> ingressMap = new HashMap<>();
        schemaInfo.setK8sIngressList(new ArrayList<>());
        List<ExtensionsV1beta1Ingress> ingressList = this.k8sApiService.listNamespacedIngress(srcPlatformId, k8sApiUrl, k8sAuthToken);
        ingressList = gson.fromJson(gson.toJson(ingressList), new TypeToken<List<ExtensionsV1beta1Ingress>>(){}.getType());
        for(ExtensionsV1beta1Ingress ingress : ingressList)
        {
            ingress.getMetadata().setNamespace(createdPlatformId);
            schemaInfo.getK8sIngressList().add(ingress);
            ingressMap.put(ingress.getMetadata().getName(), ingress);
        }
        K8sCollection oracle = new K8sCollection(deploymentMap.get("oracle"), 150);
        oracle.getServices().add(serviceMap.get("oracle"));
        oracle.setTag("oracle");
        schemaInfo.setOracle(oracle);
        deploymentMap.remove("oracle");
        serviceMap.remove("oracle");

        K8sCollection mysql = new K8sCollection(deploymentMap.get("mysql-5-7-29"));
        mysql.getServices().add(serviceMap.get("mysql"));
        schemaInfo.setMysql(mysql);
        deploymentMap.remove("mysql-5-7-29");
        serviceMap.remove("mysql");

        K8sCollection license = new K8sCollection(deploymentMap.get("licenseserver-public01"));
        license.getServices().add(serviceMap.get("licenseserver-public01"));
        schemaInfo.setLicenseServer(license);
        deploymentMap.remove("licenseserver-public01");
        serviceMap.remove("licenseserver-public01");

        K8sCollection glsserver = new K8sCollection(deploymentMap.get("glsserver-public01"));
        glsserver.getServices().add(serviceMap.get("glsserver-public01"));
        deploymentMap.remove("glsserver-public01");
        serviceMap.remove("glsserver-public01");
        schemaInfo.setGlsserver(glsserver);

        K8sCollection ucds = new K8sCollection(deploymentMap.get("ucds-cloud01"));
        ucds.getServices().add(serviceMap.get("ucds-cloud01"));
        ucds.getServices().add(serviceMap.get("ucds-cloud01-out"));
        deploymentMap.remove("ucds-cloud01");
        serviceMap.remove("ucds-cloud01");
        serviceMap.remove("ucds-cloud01-out");
        schemaInfo.setUcds(ucds);

        K8sCollection dcs = new K8sCollection(deploymentMap.get("dcs-cloud01"));
        dcs.getServices().add(serviceMap.get("dcs-cloud01"));
        deploymentMap.remove("dcs-cloud01");
        serviceMap.remove("dcs-cloud01");
        schemaInfo.setDcs(dcs);
        schemaInfo.setK8sDeploymentList(new ArrayList<>(deploymentMap.values()));
        schemaInfo.setK8sServiceList(new ArrayList<>(serviceMap.values()));

        String pvJsonStr = "[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolume\",\"metadata\":{\"name\":\"base-volume-k8s-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"capacity\":{\"storage\":\"1Gi\"},\"claimRef\":{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"name\":\"base-volume-k8s-test\",\"namespace\":\"k8s-test\"},\"nfs\":{\"path\":\"/home/kubernetes/volume/k8s-test/baseVolume\",\"server\":\"10.130.41.218\"},\"persistentVolumeReclaimPolicy\":\"Retain\",\"storageClassName\":\"base-volume-k8s-test\",\"volumeMode\":\"Filesystem\"}}]";
        List<V1PersistentVolume> pvList = gson.fromJson(pvJsonStr, new TypeToken<List<V1PersistentVolume>>(){}.getType());
        schemaInfo.setK8sPVList(pvList);
        String pvcJsonStr = "[{\"apiVersion\":\"v1\",\"kind\":\"PersistentVolumeClaim\",\"metadata\":{\"name\":\"base-volume-k8s-test\",\"namespace\":\"k8s-test\"},\"spec\":{\"accessModes\":[\"ReadWriteMany\"],\"resources\":{\"requests\":{\"storage\":\"1Gi\"}},\"storageClassName\":\"base-volume-k8s-test\",\"volumeMode\":\"Filesystem\",\"volumeName\":\"base-volume-k8s-test\"}}]";
        List<V1PersistentVolumeClaim> pvcList = gson.fromJson(pvcJsonStr, new TypeToken<List<V1PersistentVolumeClaim>>(){}.getType());
        schemaInfo.setK8sPVCList(pvcList);
//        schemaInfo.setK8sServiceList(gson.fromJson(gson.toJson(schemaInfo.getK8sServiceList()), new TypeToken<List<V1Service>>(){}.getType()));
//        schemaInfo.setK8sEndpointsList(gson.fromJson(gson.toJson(schemaInfo.getK8sEndpointsList()), new TypeToken<List<V1Endpoints>>(){}.getType()));
//        schemaInfo.setK8sIngressList(gson.fromJson(gson.toJson(schemaInfo.getK8sIngressList()), new TypeToken<List<ExtensionsV1beta1Ingress>>(){}.getType()));
//        generateConfigForCCODDomainModuleDeployment(deployments, configMapMap, allOptList, registerApps);
        logger.info(String.format("generateSchema=%s", gson.toJson(schemaInfo)));
        return schemaInfo;
    }

    /**
     * 为定义ccod域模块的deployment挂载所有配置文件
     * @param deployment 用来定义ccod域模块的deployment
     * @param configMapMap 以键值对保存的所有configMap信息
     * @param cfgMap 平台所有配置
     * @param optList deployment归属域下的所有模块操作信息
     * @param registerApps 已经向cmdb注册过的所有ccod模块信息
     * @throws ParamException
     * @throws NotSupportAppException
     */
    private void generateConfigForCCODDomainModuleDeployment(V1Deployment deployment, Map<String, V1ConfigMap> configMapMap, Map<String, List<AppFileNexusInfo>> cfgMap, List<AppUpdateOperationInfo> optList, List<AppModuleVo> registerApps) throws ParamException, NotSupportAppException
    {
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
        if(appType.equals(AppType.CCOD_KERNEL_MODULE))
        {
            container.setArgs(new ArrayList<>());
            container.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get("binary-file").setMountPath(optInfo.getBasePath());
        }
        String basePath = appType.equals(AppType.CCOD_KERNEL_MODULE) ? "/binary-file" : optInfo.getBasePath();
        String deployPath = getAbsolutePath(basePath, optInfo.getDeployPath());
        addModuleCfgToContainer(cfgMap.get(String.format("%s-%s", alias, domainId)), basePath, deployPath, configMap, moduleVo, mountPath, false, initContainer, deployment);
        deployPath = getAbsolutePath(optInfo.getBasePath(), optInfo.getDeployPath());
        if(configMapMap.containsKey(domainId))
        {
            mountPath = String.format("%s/%s", this.defaultCfgMountPath, domainId);
            addModuleCfgToContainer(cfgMap.get(domainId), optInfo.getBasePath(), deployPath, configMapMap.get(domainId), moduleVo, mountPath, true, container, deployment);
        }
        if(configMapMap.containsKey(platformId))
        {
            mountPath = String.format("%s/%s", this.defaultCfgMountPath, platformId);
            addModuleCfgToContainer(cfgMap.get(platformId), optInfo.getBasePath(), deployPath, configMapMap.get(platformId), moduleVo, mountPath, true, container, deployment);
        }
        if(appType.equals(AppType.CCOD_KERNEL_MODULE))
        {
            String startCmd = StringUtils.isNotBlank(optInfo.getStartCmd()) ? optInfo.getStartCmd() : String.format("./%s", moduleVo.getInstallPackage().getFileName());
            String args = String.format("cd %s;%s;sleep 5;tailf /root/Platform/log/*/*.log;", deployPath, startCmd);
            if(container.getArgs() == null) {
                container.setArgs(new ArrayList<>());
                container.getArgs().add(args);
            }
            else {
                args = String.format("%s;%s", container.getArgs().get(0), args);
                container.getArgs().set(0, args);
            }
            if("ucx".equals(alias))
            {
                String cmd = initContainer.getCommand().get(2);
                cmd = String.format("%s;mv /opt/FlowMap.full /binary-file/cfg", cmd, basePath);
                initContainer.getCommand().set(2, cmd);
            }
        }
        else if(appType.equals(AppType.CCOD_WEBAPPS_MODULE))
        {
            initContainer.setArgs(new ArrayList<>());
            String cmd = initContainer.getCommand().get(2);
            cmd = String.format("%s;mv /opt/%s /war/%s-%s.war", cmd, moduleVo.getInstallPackage().getFileName(), alias, domainId);
            initContainer.getCommand().set(2, cmd);
            if(container.getCommand() != null && container.getCommand().size() > 2)
            {
                cmd = container.getCommand().get(2);
                cmd = String.format("%s;%s", container.getArgs().get(0), cmd).replaceAll("^;", "").replaceAll("//", "/");
                container.getArgs().set(0, cmd);
                container.setCommand(new ArrayList<>());
                container.getCommand().add("/bin/sh");
                container.getCommand().add("-c");
            }
        }
        V1HostPathVolumeSource host = new V1HostPathVolumeSource();
        host.setPath(String.format("/var/ccod-runtime/%s/%s-%s/%s-%s", platformId, alias, domainId, alias, domainId));
        deployment.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity())).get("ccod-runtime").setHostPath(host);
    }

    private void generateConfigForCCODDomainModuleDeployment_ex(V1Deployment deployment, Map<String, V1ConfigMap> configMapMap, Map<String, List<AppFileNexusInfo>> cfgMap, List<AppUpdateOperationInfo> optList, List<AppModuleVo> registerApps) throws ParamException, NotSupportAppException
    {
        String platformId = deployment.getMetadata().getNamespace();
        String domainId = deployment.getMetadata().getLabels().get("domain-id");
        List<V1Container> containers = new ArrayList<>();
        AppType theType = null;
        if(deployment.getSpec().getTemplate().getSpec().getInitContainers() != null)
            containers.addAll(deployment.getSpec().getTemplate().getSpec().getInitContainers());
        if(deployment.getSpec().getTemplate().getSpec().getContainers() != null)
            containers.addAll(deployment.getSpec().getTemplate().getSpec().getContainers());
        AppModuleVo moduleVo = null;
        for(V1Container container : containers)
        {
            AppType appType = getAppTypeFromImageUrl(container.getImage());
            if(!AppType.CCOD_KERNEL_MODULE.equals(appType) && !AppType.CCOD_WEBAPPS_MODULE.equals(appType))
                continue;
            container.setCommand(new ArrayList<>());
            container.getCommand().add("/bin/sh");
            container.getCommand().add("-c");
            container.getCommand().add("");
            theType = appType;
            moduleVo = getAppModuleFromImageTag(container.getImage(), registerApps);
            String alias = container.getName();
            V1ConfigMap configMap = configMapMap.get(String.format("%s-%s", alias, domainId));
            AppUpdateOperationInfo optInfo = optList.stream().collect(Collectors.toMap(AppUpdateOperationInfo::getAppAlias, Function.identity())).get(alias);
            String deployPath = getAbsolutePath(optInfo.getBasePath(), optInfo.getDeployPath());
            if(AppType.CCOD_KERNEL_MODULE.equals(appType))
                deployPath = "/binary-file";
            String mountPath = String.format("%s/%s-cfg", this.defaultCfgMountPath, alias);
            addModuleCfgToContainer(cfgMap.get(String.format("%s-%s", alias, domainId)), optInfo.getBasePath(), deployPath, configMap, moduleVo, mountPath, false, container, deployment);
            if(configMapMap.containsKey(domainId))
            {
                deployPath = getAbsolutePath(optInfo.getBasePath(), optInfo.getDeployPath());
                mountPath = String.format("%s/%s", this.defaultCfgMountPath, domainId);
                addModuleCfgToContainer(cfgMap.get(domainId), optInfo.getBasePath(), deployPath, configMapMap.get(domainId), moduleVo, mountPath, true, container, deployment);
            }
            if(configMapMap.containsKey(platformId))
            {
                deployPath = getAbsolutePath(optInfo.getBasePath(), optInfo.getDeployPath());
                mountPath = String.format("%s/%s", this.defaultCfgMountPath, platformId);
                addModuleCfgToContainer(cfgMap.get(platformId), optInfo.getBasePath(), deployPath, configMapMap.get(platformId), moduleVo, mountPath, true, container, deployment);
            }
        }
        if(theType.equals(AppType.CCOD_KERNEL_MODULE))
        {
            for(V1Container container : containers) {
                if(!theType.equals(getAppTypeFromImageUrl(container.getImage())))
                {
                    List<String> command = new ArrayList<>();
                    command.add("/bin/sh");
                    command.add("-c");
                    container.setCommand(command);
                    List<String> args = new ArrayList<>();
                    args.add(String.format("cd /root/Platform/bin;./%s", moduleVo.getInstallPackage().getFileName()));
                    container.setArgs(args);
                    container.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get("binary-file").setMountPath("/root/Platform");
                }
            }
        }
    }

    @Override
    public PlatformTopologyInfo createK8sPlatform(PlatformUpdateSchemaInfo createSchema) throws ParamException, InterfaceCallException, NexusException, IOException, ApiException, LJPaasException, NotSupportAppException, SQLException, ClassNotFoundException {
        String k8sApiUrl = createSchema.getK8sApiUrl();
        String k8sAuthToken = createSchema.getK8sAuthToken();
        String platformId = createSchema.getPlatformId();
//        V1Namespace ns = this.k8sApiService.readNamespace(platformId, k8sApiUrl, k8sAuthToken);
//        if(ns != null)
//            this.k8sApiService.deleteNamespace(platformId, k8sApiUrl, k8sAuthToken);
        this.k8sApiService.createDefaultNamespace(platformId, k8sApiUrl, k8sAuthToken);
        this.k8sApiService.createNamespacedSSLCert(platformId, k8sApiUrl, k8sAuthToken);
        List<V1ConfigMap> configMapList = createConfigMapForNewPlatform(createSchema);
        Map<String, V1ConfigMap> configMapMap = new HashMap<>();
        for(V1ConfigMap configMap : configMapList)
            configMapMap.put(configMap.getMetadata().getName(), configMap);
        Map<String, List<AppFileNexusInfo>> cfgMap = new HashMap<>();
        if(createSchema.getPublicConfig() != null && createSchema.getPublicConfig().size() > 0)
            cfgMap.put(platformId, createSchema.getPublicConfig());
        for(DomainUpdatePlanInfo planInfo : createSchema.getDomainUpdatePlanList())
        {
            String domainId = planInfo.getDomainId();
            if(planInfo.getPublicConfig() != null && planInfo.getPublicConfig().size() > 0)
                cfgMap.put(domainId, planInfo.getPublicConfig());
            for(AppUpdateOperationInfo optInfo : planInfo.getAppUpdateOperationList())
            {
                String alias = optInfo.getAppAlias();
                cfgMap.put(String.format("%s-%s", alias, domainId), optInfo.getCfgs());
            }
        }
        for(V1PersistentVolume pv : createSchema.getK8sPVList())
            this.k8sApiService.createPersistentVolume(pv, k8sApiUrl, k8sAuthToken);
        for(V1PersistentVolumeClaim pvc : createSchema.getK8sPVCList())
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
        for(V1Deployment deployment : createSchema.getK8sDeploymentList())
            this.k8sApiService.createNamespacedDeployment(platformId, deployment, k8sApiUrl, k8sAuthToken);
        for(V1Service service : createSchema.getK8sServiceList())
            this.k8sApiService.createNamespacedService(platformId, service, k8sApiUrl, k8sAuthToken);
        for(ExtensionsV1beta1Ingress ingress : createSchema.getK8sIngressList())
            this.k8sApiService.createNamespacedIngress(platformId, ingress, k8sApiUrl, k8sAuthToken);
        this.k8sApiService.replaceNamespacedDeployment(createSchema.getDcs().getDeployment().getMetadata().getName(), platformId, createSchema.getDcs().getDeployment(), k8sApiUrl, k8sAuthToken);
        return this.getPlatformTopologyFromK8s(createSchema.getPlatformName(), platformId, createSchema.getBkBizId(), createSchema.getBkCloudId(), createSchema.getCcodVersion(), k8sApiUrl, k8sAuthToken, createSchema.getPlatformFunc());
    }

    private void deploySpecialApp(PlatformUpdateSchemaInfo schemaInfo, K8sCollection specialApp, String k8sApiUrl, String k8sAuthToken) throws ApiException, SQLException
    {
        String platformId = schemaInfo.getPlatformId();
        this.k8sApiService.createNamespacedDeployment(platformId, specialApp.getDeployment(), k8sApiUrl, k8sAuthToken);
        for(V1Service service : specialApp.getServices())
            this.k8sApiService.createNamespacedService(platformId, service, k8sApiUrl, k8sAuthToken);
        if(StringUtils.isNotBlank(specialApp.getTag()) && specialApp.getTag().toLowerCase().equals("oracle"))
        {
            try {
                Thread.sleep(5000);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
            V1Service service = this.k8sApiService.readNamespacedService(specialApp.getServices().get(0).getMetadata().getName(), platformId,  k8sApiUrl, k8sAuthToken);
            int port = getNodePortFromK8sService(service);
            boolean connected = oracleConnectTest(schemaInfo.getGlsDBUser(), schemaInfo.getGlsDBPwd(), schemaInfo.getK8sHostIp(), port, "xe", specialApp.getTimeout());
            if(!connected)
                throw new ApiException("connect oralce fail");
            specialApp.setNodePort(port);
        }
    }

    private Connection createOracleConnection(String user, String pwd, String ip, int port, String sid) throws ClassNotFoundException, SQLException
    {
        String connStr = String.format("jdbc:oracle:thin:@%s:%s:%s", ip, port, sid);
        Class.forName("oracle.jdbc.driver.OracleDriver");
        logger.debug(String.format("create %s oracle conn", connStr));
        Connection conn = DriverManager.getConnection(connStr, user, pwd);
        logger.debug(String.format("conn %s success", connStr));
        return conn;
    }

    private boolean oracleConnectTest(String user, String pwd, String ip, int port, String sid, int timeout)
    {
        int count = 1;
        while (count < (timeout / 10))
        {
            System.out.println("");
            try
            {
                logger.debug(String.format("try connect oracle"));
                Connection conn = createOracleConnection(user, pwd, ip, port, sid);
                conn.close();
                return true;
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
                System.out.println(String.format("fail to conn %d", count));
                count++;
                try {
                    Thread.sleep(10000);
                }
                catch (Exception ex1) {
                    ex1.printStackTrace();
                }
            }
        }
        logger.error("conn timeout");
        return false;
    }


    @Test
    public void pathTest() throws Exception
    {
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
        String ip = "10.130.41.218";
        int port = 32076;
        String sid = "xe";
        String user = "ccod";
        String pwd = "ccod";
        oracleConnectTest(user, pwd, ip, port, sid, 150);
    }

    private void portTest() throws Exception
    {
        V1Service service = this.k8sApiService.readNamespacedService("oracle", "k8s-test", this.testK8sApiUrl, this.testAuthToken);
        int port = getNodePortFromK8sService(service);
        System.out.println(port);
    }
}