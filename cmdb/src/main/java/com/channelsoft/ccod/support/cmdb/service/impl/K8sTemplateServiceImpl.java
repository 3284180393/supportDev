package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.config.GsonDateUtil;
import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.dao.CCODThreePartAppMapper;
import com.channelsoft.ccod.support.cmdb.dao.K8sTemplateMapper;
import com.channelsoft.ccod.support.cmdb.exception.InterfaceCallException;
import com.channelsoft.ccod.support.cmdb.exception.NexusException;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sCCODDomainAppVo;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sThreePartAppVo;
import com.channelsoft.ccod.support.cmdb.k8s.vo.K8sThreePartServiceVo;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import com.channelsoft.ccod.support.cmdb.service.IK8sTemplateService;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.channelsoft.ccod.support.cmdb.utils.FileUtils;
import com.channelsoft.ccod.support.cmdb.vo.*;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.custom.IntOrString;
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

import javax.annotation.PostConstruct;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @ClassName: K8sTemplateServiceImpl
 * @Author: lanhb
 * @Description: IK8sTemplateService接口实现类
 * @Date: 2020/8/6 9:55
 * @Version: 1.0
 */
@Service
public class K8sTemplateServiceImpl implements IK8sTemplateService {

    private final static Logger logger = LoggerFactory.getLogger(K8sTemplateServiceImpl.class);

    @Autowired
    IK8sApiService ik8sApiService;

    @Autowired
    IAppManagerService appManagerService;

    @Autowired
    INexusService nexusService;

    @Autowired
    K8sTemplateMapper k8sTemplateMapper;

    @Autowired
    CCODThreePartAppMapper ccodThreePartAppMapper;

    @Value("${nexus.platform-app-cfg-repository}")
    private String platformAppCfgRepository;

    @Value("${k8s.labels.app-name}")
    private String appNameLabel;

    @Value("${k8s.labels.app-type}")
    private String appTypeLabel;

    @Value("${k8s.labels.ccod-version}")
    private String ccodVersionLabel;

    @Value("${k8s.labels.platform-id}")
    private String platformIdLabel;

    @Value("${k8s.labels.domain-id}")
    private String domainIdLabel;

    @Value("${k8s.labels.node-ip}")
    private String nodeIpLabel;

    @Value("${k8s.labels.app-version}")
    private String appVersionLabel;

    @Value("${k8s.labels.service-type}")
    private String serviceTypeLabel;

    @Value("${k8s.labels.platform-tag}")
    private String platformTagLabel;

    @Value("${k8s.labels.domain-tag}")
    private String domainTagLabel;

    @Value("${k8s.labels.app-tag}")
    private String appTagLabel;

    @Value("${k8s.deployment.defaultCfgMountPath}")
    private String defaultCfgMountPath;

    @Value("${nexus.nexus-docker-url}")
    private String nexusDockerUrl;

    @Value("${ccod.service-port-regex}")
    private String portRegex;

    @Value("${ccod.health-check-at-regex}")
    private String healthCheckRegex;

    @Value("${ccod.start-cmd-regex}")
    private String startCmdRegex;

    @Value("${k8s.template-file-path}")
    private String templateSavePath;

    @Value("${k8s.test-three-part-service-save-path}")
    private String testThreePartServiceSavePath;

    @Value("${nexus.user}")
    private String nexusUserName;

    @Value("${nexus.password}")
    private String nexusPassword;

    @Value("${nexus.host-url}")
    private String nexusHostUrl;

    private final static Gson gson = new GsonBuilder().registerTypeAdapter(DateTime.class, new GsonDateUtil()).disableHtmlEscaping().create();

    private Gson templateParseGson = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            //过滤掉字段名包含"age"
            return f.getName().contains("creationTimestamp") || f.getName().contains("status") || f.getName().contains("resourceVersion") || f.getName().contains("selfLink") || f.getName().contains("uid")
                    || f.getName().contains("generation") || f.getName().contains("strategy")
                    || f.getName().contains("terminationMessagePath") || f.getName().contains("terminationMessagePolicy")
                    || f.getName().contains("dnsPolicy") || f.getName().contains("securityContext") || f.getName().contains("schedulerName")
                    || f.getName().contains("restartPolicy") || f.getName().contains("clusterIP")
                    || f.getName().contains("sessionAffinity") || f.getName().contains("nodePort") || f.getName().contains("managedFields")
                    || f.getName().contains("annotations");
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            //过滤掉 类名包含 Bean的类
            return clazz.getName().contains("Bean");
        }
    }).registerTypeAdapter(DateTime.class, new GsonDateUtil()).create();

    private final List<K8sObjectTemplatePo> objectTemplateList = new ArrayList<>();

    private final List<K8sThreePartServiceVo> testThreePartServices = new ArrayList<>();

    protected String testK8sApiUrl = "https://10.130.41.218:6443";

    protected String testAuthToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA";

    private final static String forAllVersion = "ANY_VERSION";

    private final List<String> templateSupportTags = new ArrayList<>();

    @PostConstruct
    void init() throws Exception
    {
//        List<K8sObjectTemplatePo> testList = generatePlatformObjectTemplate("test-by-wyf", "4.1", "ucds-cloud01", "cas-manage01", "dcms-manage01");
//        this.objectTemplateList.addAll(testList);
//        testList = generatePlatformObjectTemplate("jhkzx-1", "3.9", "ucds-cloud01", "cas-manage01", "dcmswebservice-manage01");
////        this.objectTemplateList.addAll(testList);
//        List<K8sObjectTemplatePo> list = parseTemplateFromFile(this.templateSavePath);
////        this.objectTemplateList.addAll(list);
//        list.forEach(t->{
//            K8sTemplatePo po = new K8sTemplatePo(t);
//            k8sTemplateMapper.insert(po);
//        });
//        List<K8sThreePartServiceVo> threeSvcs = getThreePartServices("test-by-wyf", testK8sApiUrl, testAuthToken);
//        logger.warn(this.templateParseGson.toJson(threeSvcs));
//        this.testThreePartServices.addAll(threeSvcs);
//        k8sTemplateMapper.select().forEach(t->{
//            String appType = t.getLabels().get(appTypeLabel);
//            if(appType != null && appType.equals(AppType.THREE_PART_APP.name)){
//                t.getObjectTemplate().setConfigMaps(new ArrayList<>());
//                k8sTemplateMapper.update(t);
//            }
//        });
        List<K8sTemplatePo> templateList = k8sTemplateMapper.select();
        templateList.forEach(t->objectTemplateList.add(t.getObjectTemplate()));
//        List<K8sThreePartServiceVo> threeSvcs = parseTestThreePartServiceFromFile(this.testThreePartServiceSavePath);
        templateSupportTags.addAll(Arrays.asList(new String[]{ccodVersionLabel, appTypeLabel, platformTagLabel, appTagLabel}));
//        this.testThreePartServices.addAll(threeSvcs);
        try{
//            updateTemplate();
//            getTemplateForBic();
            String json = "{\"apiVersion\":\"apps/v1\",\"kind\":\"Deployment\",\"metadata\":{\"annotations\":{\"deployment.kubernetes.io/revision\":\"2\"},\"creationTimestamp\":\"2021-01-13T09:40:17Z\",\"generation\":2,\"labels\":{\"app-name\":\"filepreview\",\"cattle.io/creator\":\"norman\",\"filepreview\":\"preview\",\"workload.user.cattle.io/workloadselector\":\"deployment-bicys-filepreview\"},\"name\":\"filepreview\",\"namespace\":\"base-bicys\",\"resourceVersion\":\"78357130\",\"selfLink\":\"/apis/apps/v1/namespaces/base-bicys/deployments/filepreview\",\"uid\":\"c042395c-9716-4f22-9577-96059da616db\"},\"spec\":{\"progressDeadlineSeconds\":600,\"replicas\":1,\"revisionHistoryLimit\":10,\"selector\":{\"matchLabels\":{\"workload.user.cattle.io/workloadselector\":\"deployment-bicys-filepreview\"}},\"strategy\":{\"rollingUpdate\":{\"maxSurge\":\"25%\",\"maxUnavailable\":\"25%\"},\"type\":\"RollingUpdate\"},\"template\":{\"metadata\":{\"annotations\":{\"cattle.io/timestamp\":\"2021-01-15T10:11:28Z\",\"field.cattle.io/ports\":\"[[{\\\"containerPort\\\":80,\\\"dnsName\\\":\\\"filepreview\\\",\\\"hostPort\\\":0,\\\"kind\\\":\\\"ClusterIP\\\",\\\"name\\\":\\\"ui\\\",\\\"protocol\\\":\\\"TCP\\\",\\\"sourcePort\\\":0}]]\"},\"creationTimestamp\":null,\"labels\":{\"app\":\"filepreview\",\"workload.user.cattle.io/workloadselector\":\"deployment-bicys-filepreview\"}},\"spec\":{\"containers\":[{\"env\":[{\"name\":\"DOMAIN_NAME\",\"value\":\"bicys.ccod.com\"}],\"image\":\"ccod/filepreview:2.2.1\",\"imagePullPolicy\":\"Always\",\"livenessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":20,\"periodSeconds\":10,\"successThreshold\":1,\"tcpSocket\":{\"port\":80},\"timeoutSeconds\":1},\"name\":\"filepreview\",\"ports\":[{\"containerPort\":80,\"name\":\"ui\",\"protocol\":\"TCP\"}],\"readinessProbe\":{\"failureThreshold\":3,\"initialDelaySeconds\":20,\"periodSeconds\":10,\"successThreshold\":1,\"tcpSocket\":{\"port\":80},\"timeoutSeconds\":1},\"resources\":{},\"securityContext\":{\"capabilities\":{}},\"stdin\":true,\"terminationMessagePath\":\"/dev/termination-log\",\"terminationMessagePolicy\":\"File\",\"tty\":true}],\"dnsPolicy\":\"ClusterFirst\",\"restartPolicy\":\"Always\",\"schedulerName\":\"default-scheduler\",\"securityContext\":{},\"terminationGracePeriodSeconds\":30}}},\"status\":{\"availableReplicas\":1,\"conditions\":[{\"lastTransitionTime\":\"2021-01-13T09:40:53Z\",\"lastUpdateTime\":\"2021-01-13T09:40:53Z\",\"message\":\"Deployment has minimum availability.\",\"reason\":\"MinimumReplicasAvailable\",\"status\":\"True\",\"type\":\"Available\"},{\"lastTransitionTime\":\"2021-01-13T09:40:17Z\",\"lastUpdateTime\":\"2021-01-15T10:12:35Z\",\"message\":\"ReplicaSet \\\"filepreview-84f74f67f6\\\" has successfully progressed.\",\"reason\":\"NewReplicaSetAvailable\",\"status\":\"True\",\"type\":\"Progressing\"}],\"observedGeneration\":2,\"readyReplicas\":1,\"replicas\":1,\"updatedReplicas\":1}}";
            V1Deployment deployment = templateParseGson.fromJson(json, V1Deployment.class);
            System.out.println(templateParseGson.toJson(deployment));
        }
        catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void updateTemplate() throws Exception
    {
    }

    private List<K8sTemplatePo> getPlatformTemplateForBic() throws ApiException{
        List<K8sTemplatePo> list = new ArrayList<>();
        String platformId = "bic";
        String ccodVersion = "bic";
        String domainId = null;
        String nfsServerIp = null;
        String testK8sApiUrl = "https://10.130.36.102:6443";
        String testAuthToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjBMWWJQVEczMTFKRzllVnhDazI1SC0tOU0xNjdHTGVrS0ltaVQ2VUpQN2sifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJhZG1pbi10b2tlbi1ndGR6eCIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJhZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImVhZmE2OGYwLWFmNTYtNDg1MC05NzZiLTBiMjRmODdhZDM4OCIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTphZG1pbiJ9.gHuo1vZXLXKrpJFXqefjNZ2RdAPKf095msES1AilAFxzWD-3Sei4Meigy_DkzJnZcG1F7Dg7b9T9WNzuE9dDOWIbvRF_fCSj53Of3DuMw873yMAAZXn6sZkXP4_61nPhBp1GGTqdk8CVPYXiaOgF69In_U32e9A4tbRrj3NhWV0MN34mGtqAvRKCQrBDVklIpzl17E-vniNs4MDv0lCotCWn2BTw2s8yuRWWyJGyJBgIp9ZfoGdlTD5FcXJNwXSdaeH6lCsb8l0ummFb_8_QVpEoTyv4IwXM1jQDu8ElPAJRsEbttBrPtaa5gyK7NnlneqDYqCfPf2BWVQJ8h-iDPQ";
        K8sObjectTemplatePo obj;
        String platformTag;
        List<String> pvNames;
        List<String> pvcNames;
        List<String> secretNames;
        List<String> configMapNames;

        platformTag = "base";
        pvNames = new ArrayList<>();
        pvcNames = Arrays.asList(new String[]{"fastdfs", "mysql", "redis-data-redis-master-0", "redis-data-redis-slave-0", "redis-data-redis-slave-1"});
        secretNames = Arrays.asList(new String[]{"default-token-ndcmm", "mysql", "redis"});
        configMapNames = new ArrayList<>();
        obj = generatePlatformObjectTemplateFromExist(pvNames, pvcNames, secretNames, configMapNames, nfsServerIp, platformId, platformTag, ccodVersion, testK8sApiUrl, testAuthToken);
        String json = templateParseGson.toJson(obj);
        json = json.replace("\"name\":\"${PLATFORMID}\"", "\"name\":\"base-${PLATFORMID}\"");
        json = json.replace("\"namespace\":\"${PLATFORMID}\"", "\"namespace\":\"base-${PLATFORMID}\"");
        obj = templateParseGson.fromJson(json, K8sObjectTemplatePo.class);
        logger.warn(String.format("%s=%s", gson.toJson(obj.getLabels()), gson.toJson(obj)));
        list.add(new K8sTemplatePo(obj));

        platformTag = null;
        pvNames = new ArrayList<>();
        pvcNames = Arrays.asList(new String[]{"cbs"});
        secretNames = new ArrayList<>();
        configMapNames = new ArrayList<>();
        obj = generatePlatformObjectTemplateFromExist(pvNames, pvcNames, secretNames, configMapNames, nfsServerIp, platformId, platformTag, ccodVersion, testK8sApiUrl, testAuthToken);
        logger.warn(String.format("%s=%s", gson.toJson(obj.getLabels()), gson.toJson(obj)));
        list.add(new K8sTemplatePo(obj));

        return list;
    }

    private K8sObjectTemplatePo generatePlatformObjectTemplateFromExist(
            List<String> pvNames, List<String> pvcNames,
            List<String> secretNames, List<String> configMapNames, String nfsServerIp, String platformId,
            String platformTag, String ccodVersion, String k8sApiUrl, String k8sApiAuthToken) throws ApiException{
        logger.debug(String.format("generate platform k8s object template from %s which pvNames=%s, pvcName=%s, secretName=%s, configNames=%s, nfsServerIp=%s",
                platformId, gson.toJson(pvNames), gson.toJson(pvcNames), gson.toJson(secretNames), gson.toJson(configMapNames), nfsServerIp));
        V1Namespace namespace = ik8sApiService.readNamespace(platformId, k8sApiUrl, k8sApiAuthToken);
        List<V1PersistentVolume> pvList = new ArrayList<>();
        for(String pvName : pvNames){
            V1PersistentVolume pv = ik8sApiService.readPersistentVolume(pvName, k8sApiUrl, k8sApiAuthToken);
            pvList.add(pv);
        }
        if(StringUtils.isNotBlank(nfsServerIp)){
            pvList = templateParseGson.fromJson(templateParseGson.toJson(pvList).replace(nfsServerIp, K8sObjectTemplatePo.NFS_SERVER_IP), new TypeToken<List<V1PersistentVolume>>() {}.getType());
        }
        List<V1PersistentVolumeClaim> pvcList = new ArrayList<>();
        for(String pvcName : pvcNames){
            V1PersistentVolumeClaim pvc = ik8sApiService.readNamespacedPersistentVolumeClaim(pvcName, platformId, k8sApiUrl, k8sApiAuthToken);
            pvcList.add(pvc);
        }
        List<V1Secret> secrets = new ArrayList<>();
        for(String secretName : secretNames){
            V1Secret secret = ik8sApiService.readNamespacedSecret(secretName, platformId, k8sApiUrl, k8sApiAuthToken);
            secrets.add(secret);
        }
        List<V1ConfigMap> configMaps = new ArrayList<>();
        for(String configName : configMapNames){
            V1ConfigMap configMap = ik8sApiService.readNamespacedConfigMap(configName, platformId, k8sApiUrl, k8sApiAuthToken);
            configMaps.add(configMap);
        }
        K8sObjectTemplatePo obj = new K8sObjectTemplatePo();
        Map<String, String> labels = new HashMap<>();
        labels.put(ccodVersionLabel, ccodVersion);
        if(StringUtils.isNotBlank(platformTag)){
            labels.put(platformTagLabel, platformTag);
        }
        obj.setLabels(labels);
        obj.setNamespaces(namespace);
        obj.setConfigMaps(configMaps);
        obj.setPvList(pvList);
        obj.setPvcList(pvcList);
        obj.setSecrets(secrets);
        String json = templateParseGson.toJson(obj);
        json = json.replace(platformId, K8sObjectTemplatePo.PLATFORM_ID);
        obj = templateParseGson.fromJson(json, K8sObjectTemplatePo.class);
        obj.getLabels().put(ccodVersionLabel, ccodVersion);
        return obj;
    }

    @Override
    public K8sObjectTemplatePo getAppObjectTemplateFromExist(
            AppType appType, String appName, String alias, List<String> deploymentNames, List<String> statefulSetNames,
            List<String> serviceNames, List<String> endpointNames, List<String> ingressNames, List<String> configMapNames,  List<String> secretNames,
            String platformId, String domainId, String hostUrl, String k8sApiUrl, String k8sApiAuthToken) throws ApiException, ParamException
    {
        Assert.isTrue(StringUtils.isNotBlank(appName), "appName can not be blank");
        Assert.notNull(deploymentNames, "deploymentNames can not be null");
        Assert.notNull(statefulSetNames, "statefulSetNames can not be null");
        Assert.notNull(serviceNames, "serviceNames can not be null");
        Assert.isTrue(serviceNames.size()>0, "service can not be empty");
        Assert.notNull(endpointNames, "endpointNames can not be null");
        Assert.notNull(ingressNames, "ingressNames can not be null");
        Assert.notNull(configMapNames, "configMapNames can not be null");
        Assert.notNull(secretNames, "secretNames can not be null");
        Assert.isTrue(StringUtils.isNotBlank(alias), "alias can not be blank");
        Assert.isTrue(StringUtils.isNotBlank(platformId), "platformId can not be blank");
        Assert.isTrue(StringUtils.isNotBlank(hostUrl), "hostUrl can not be blank");
        Assert.isTrue(StringUtils.isNotBlank(k8sApiUrl), "k8sApiUrl can not be blank");
        Assert.isTrue(StringUtils.isNotBlank(k8sApiAuthToken), "k8sApiAuthToken can not be blank");
        logger.debug(String.format("generate k8s object template for %s(appType=%s) from platformId=%s and domainId=%s which deployments=%s, statefulSets=%s, services=%s, endpoints=%s, ingress=%s and configMaps=%s",
                appName, appType.name, platformId, domainId, gson.toJson(deploymentNames), gson.toJson(statefulSetNames),
                gson.toJson(serviceNames), gson.toJson(endpointNames), gson.toJson(ingressNames), gson.toJson(configMapNames)));
        List<V1Deployment> deployments = new ArrayList<>();
        for(String deploymentName : deploymentNames){
            V1Deployment deployment = ik8sApiService.readNamespacedDeployment(deploymentName, platformId, k8sApiUrl, k8sApiAuthToken);
            deployments.add(deployment);
        }
        List<V1StatefulSet> statefulSets = new ArrayList<>();
        for(String statefulSetName : statefulSetNames){
            V1StatefulSet statefulSet = ik8sApiService.readNamespacedStatefulSet(statefulSetName, platformId, k8sApiUrl, k8sApiAuthToken);
            statefulSets.add(statefulSet);
        }
        List<V1Service> services = new ArrayList<>();
        for(String serviceName : serviceNames){
            V1Service service = ik8sApiService.readNamespacedService(serviceName, platformId, k8sApiUrl, k8sApiAuthToken);
            services.add(service);
        }
        List<ExtensionsV1beta1Ingress> ingresses = new ArrayList<>();
        for(String ingressName : ingressNames){
            ExtensionsV1beta1Ingress ingress = ik8sApiService.readNamespacedIngress(ingressName, platformId, k8sApiUrl, k8sApiAuthToken);
            ingresses.add(ingress);
        }
        List<V1Endpoints> endpoints = new ArrayList<>();
        for(String endpointName : endpointNames){
            V1Endpoints endpoint = ik8sApiService.readNamespacedEndpoints(endpointName, platformId, k8sApiUrl, k8sApiAuthToken);
            endpoints.add(endpoint);
        }
        List<V1ConfigMap> configMaps = new ArrayList<>();
        for(String configName : configMapNames){
            V1ConfigMap configMap = ik8sApiService.readNamespacedConfigMap(configName, platformId, k8sApiUrl, k8sApiAuthToken);
            configMaps.add(configMap);
        }
        List<V1Secret> secrets = new ArrayList<>();
        for(String secretName : secretNames){
            V1Secret secret = ik8sApiService.readNamespacedSecret(secretName, platformId, k8sApiUrl, k8sApiAuthToken);
            secrets.add(secret);
        }
        K8sObjectTemplatePo obj = new K8sObjectTemplatePo();
        Map<String, String> labels = new HashMap<>();
        labels.put(appTypeLabel, appType.name);
        obj.setLabels(labels);
        obj.setDeployments(deployments);
        obj.setStatefulSets(statefulSets);
        obj.setServices(services);
        obj.setEndpoints(endpoints);
        obj.setIngresses(ingresses);
        obj.setConfigMaps(configMaps);
        obj.setSecrets(secrets);
        String json = templateParseGson.toJson(obj);
        json = json.replace(String.format("\"%s\":\"%s\"", appName, alias), String.format("\"%s\":\"%s\"", K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS));
        platformId = platformId.replaceAll("^base\\-", "");
        json = json.replace(platformId, K8sObjectTemplatePo.PLATFORM_ID);
        if(StringUtils.isNotBlank(domainId)){
            json = json.replace(domainId, K8sObjectTemplatePo.DOMAIN_ID);
        }
//        json = json.replace(appType.name, K8sObjectTemplatePo.APP_TYPE);
        json = json.replace(hostUrl, K8sObjectTemplatePo.HOST_URL);
        json = json.replace(alias, K8sObjectTemplatePo.ALIAS);
        obj = templateParseGson.fromJson(json, K8sObjectTemplatePo.class);
        return obj;
    }

    @Override
    public List<K8sObjectTemplatePo> getK8sTemplates() {
        return this.objectTemplateList;
    }


    public K8sTemplatePo addNewAppK8sTemplate(Map<String, String> labels, List<V1Deployment> deployments, List<V1StatefulSet> statefulSets, List<V1Service> services, ExtensionsV1beta1Ingress ingress, List<V1Endpoints> endpoints, List<V1ConfigMap> configMaps) throws ParamException
    {
        logger.debug(String.format("create template for %s : deployments=%s, statefulSets=%s, services=%s, ingress=%s, endpoints=%s, configMaps=%s",
                gson.toJson(deployments), gson.toJson(statefulSets), gson.toJson(services), gson.toJson(ingress), gson.toJson(endpoints), gson.toJson(configMaps) ));
        if(!labels.containsKey(ccodVersionLabel)){
            throw new ParamException(String.format("labels for new add app template mush has %s key", ccodVersionLabel));
        }
        else if(!labels.containsKey(appTypeLabel)){
            throw new ParamException(String.format("labels for new add app template mush has %s key", appTypeLabel));
        }
        for(K8sObjectTemplatePo template : objectTemplateList){
            if(isEqual(template.getLabels(), labels)){
                throw new ParamException(String.format("template for %s has been defined", gson.toJson(labels)));
            }
        }
        K8sObjectTemplatePo po = new K8sObjectTemplatePo();
        po.setLabels(labels);
        po.setConfigMaps(configMaps);
        po.setDeployments(deployments);
        po.setStatefulSets(statefulSets);
        po.setServices(services);
        po.setEndpoints(endpoints);
        K8sTemplatePo templatePo = new K8sTemplatePo(po);
        k8sTemplateMapper.insert(templatePo);
        logger.info("template for %s has been added : %s", gson.toJson(labels), gson.toJson(templatePo));
        return templatePo;
    }

    private void resetAppTemplate(K8sObjectTemplatePo template)
    {
        String appType = template.getLabels().get(appTypeLabel);
//        if(!template.getLabels().containsKey(appNameLabel) || !template.getLabels().get(appNameLabel).equals("umg"))
//            return;
        boolean isDomainApp = appType.equals(AppType.THREE_PART_APP.name) || appType.equals(AppType.OTHER.name) ? false : true;
        template.setStatefulSets(template.getStatefulSets().stream().map(s->{
            s.getMetadata().setLabels(new HashMap<>());
            s.getMetadata().getLabels().put(appTypeLabel, appType);
            s.getMetadata().getLabels().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
            s.getSpec().getSelector().setMatchLabels(new HashMap<>());
            s.getSpec().getSelector().getMatchLabels().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
            s.getSpec().getTemplate().getMetadata().setLabels(new HashMap<>());
            s.getSpec().getTemplate().getMetadata().getLabels().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
            if(isDomainApp){
                s.getMetadata().getLabels().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
                s.getSpec().getSelector().getMatchLabels().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
                s.getSpec().getTemplate().getMetadata().getLabels().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
            }
            s.getSpec().getTemplate().getSpec().setNodeSelector(new HashMap<>());
            return s;
        }).collect(Collectors.toList()));
        if(template.getConfigMaps() != null){
            template.setConfigMaps(template.getConfigMaps().stream().map(c->{
                c.getMetadata().setLabels(new HashMap<>());
                return c;
            }).collect(Collectors.toList()));
        }
        else{
            template.setConfigMaps(new ArrayList<>());
        }
        if(template.getEndpoints() != null){
            template.setEndpoints(template.getEndpoints().stream().map(e->{
                e.getMetadata().setName(K8sObjectTemplatePo.ALIAS);
                e.getMetadata().setLabels(new HashMap<>());
                e.getSubsets().forEach(s->s.getAddresses().forEach(a->a.setIp("${UMGIP}")));
                return e;
            }).collect(Collectors.toList()));
            ;
        }
        else{
            template.setEndpoints(new ArrayList<>());
        }
        if(template.getServices() == null){
            template.setServices(new ArrayList<>());
        }
        template.setServices(template.getServices().stream().map(s->{
            s.getMetadata().setLabels(new HashMap<>());
            s.getMetadata().getLabels().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
            if(isDomainApp){
                s.getMetadata().getLabels().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
                s.getMetadata().setName(String.format("%s-%s", K8sObjectTemplatePo.ALIAS, K8sObjectTemplatePo.DOMAIN_ID));
            }
            else{
                s.getMetadata().setName(K8sObjectTemplatePo.ALIAS);
            }
            if(template.getDeployments() != null && template.getDeployments().size() > 0){
                s.getSpec().setSelector(new HashMap<>());
                s.getSpec().getSelector().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
                if(isDomainApp){
                    s.getSpec().getSelector().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
                }
            }
            return s;
        }).collect(Collectors.toList()));
        if(template.getDeployments() != null){
            template.setDeployments(template.getDeployments().stream().map(d->{
                d.getMetadata().setLabels(new HashMap<>());
                d.getMetadata().getLabels().put(appTypeLabel, appType);
                d.getMetadata().getLabels().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
                if(isDomainApp){
                    d.getMetadata().getLabels().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
                }
                if(!isDomainApp)
                    d.getMetadata().setName(K8sObjectTemplatePo.ALIAS);
                else
                    d.getMetadata().setName(String.format("%s-%s", K8sObjectTemplatePo.ALIAS, K8sObjectTemplatePo.DOMAIN_ID));
                d.getSpec().getSelector().setMatchLabels(new HashMap<>());
                d.getSpec().getSelector().getMatchLabels().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
                d.getSpec().getTemplate().getMetadata().setLabels(new HashMap<>());
                d.getSpec().getTemplate().getMetadata().getLabels().put(K8sObjectTemplatePo.APP_NAME, K8sObjectTemplatePo.ALIAS);
                if(isDomainApp){
                    d.getSpec().getSelector().getMatchLabels().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
                    d.getSpec().getTemplate().getMetadata().getLabels().put(domainIdLabel, K8sObjectTemplatePo.DOMAIN_ID);
                }
                if(isDomainApp && d.getSpec().getTemplate().getSpec().getInitContainers() != null){
                    List<V1Container> inits = d.getSpec().getTemplate().getSpec().getInitContainers().stream()
                            .map(c->{
                                c.setName(K8sObjectTemplatePo.ALIAS);
                                c.setCommand(new ArrayList<>());
                                c.setArgs(new ArrayList<>());
                                c.setImage(String.format("nexus.io:5000/ccod/%s:%s", K8sObjectTemplatePo.APP_LOW_NAME, K8sObjectTemplatePo.APP_VERSION));
                                return c;
                            }).collect(Collectors.toList());
                    d.getSpec().getTemplate().getSpec().setInitContainers(inits);
                }
                d.getSpec().getTemplate().getSpec().getContainers().get(0).setName(String.format("%s-runtime", K8sObjectTemplatePo.ALIAS));
                if(isDomainApp){
                    List<V1Container> runs = d.getSpec().getTemplate().getSpec().getContainers().stream()
                            .map(c->{
                                c.setCommand(new ArrayList<>());
                                c.setArgs(new ArrayList<>());
                                return c;
                            }).collect(Collectors.toList());
                    d.getSpec().getTemplate().getSpec().setContainers(runs);
                }
                return d;
            }).collect(Collectors.toList()));
        }
        else{
            template.setDeployments(new ArrayList<>());
        }
        if(template.getIngresses() != null){
            template.getIngresses().forEach(i->i.getMetadata().setLabels(new HashMap<>()));
        }
    }

    public K8sTemplatePo updateAppK8sTemplate(Map<String, String> labels, List<V1Deployment> deployments, List<V1StatefulSet> statefulSets, List<V1Service> services, ExtensionsV1beta1Ingress ingress, List<V1Endpoints> endpoints, List<V1ConfigMap> configMaps) throws ParamException
    {
        logger.debug(String.format("updated template for %s : deployments=%s, statefulSets=%s, services=%s, ingress=%s, endpoints=%s, configMaps=%s",
                gson.toJson(labels), gson.toJson(deployments), gson.toJson(statefulSets), gson.toJson(services), gson.toJson(ingress), gson.toJson(endpoints), gson.toJson(configMaps) ));
        if(!labels.containsKey(ccodVersionLabel)){
            throw new ParamException(String.format("labels for new add app template mush has %s key", ccodVersionLabel));
        }
        else if(!labels.containsKey(appTypeLabel)){
            throw new ParamException(String.format("labels for new add app template mush has %s key", appTypeLabel));
        }
        List<K8sTemplatePo> templateList = k8sTemplateMapper.select();
        K8sTemplatePo po = null;
        for(K8sTemplatePo template : templateList){
            if(isEqual(template.getLabels(), labels)){
                po = template;
                break;
            }
        }
        if(po == null){
            throw new ParamException(String.format("can not find template to be updated for %s", gson.toJson(labels)));
        }
        po.setLabels(labels);
        po.getObjectTemplate().setConfigMaps(configMaps);
        po.getObjectTemplate().setDeployments(deployments);
        po.getObjectTemplate().setStatefulSets(statefulSets);
        po.getObjectTemplate().setServices(services);
        po.getObjectTemplate().setEndpoints(endpoints);
        k8sTemplateMapper.update(po);
        logger.info("template for %s has been updated : %s", gson.toJson(labels), gson.toJson(po));
        templateList = k8sTemplateMapper.select();
        objectTemplateList.clear();
        templateList.forEach(t->objectTemplateList.add(t.getObjectTemplate()));
        return po;
    }

    public K8sTemplatePo addNewAppK8sTemplateFromExistNamespace(Map<String, String> labels, Map<String, String> selector, String namespace, String k8sApiUrl, String k8sAuthToken)throws ApiException, ParamException{
        for(K8sObjectTemplatePo po : objectTemplateList){
            if(isEqual(po.getLabels(), labels)){
                throw new ParamException(String.format("k8s template for %s exist", gson.toJson(labels)));
            }
            List<V1Deployment> deployments = ik8sApiService.selectNamespacedDeployment(namespace, selector, k8sApiUrl, k8sAuthToken);
            List<V1Service> services = ik8sApiService.selectNamespacedService(namespace, selector, k8sApiUrl, k8sAuthToken);

        }
        return null;
    }


    @Override
    public List<K8sObjectTemplatePo> queryK8sObjectTemplate(String ccodVersion, AppType appType, String appName, String version)
    {
        logger.debug(String.format("begin to query template with ccodVersion=%s, appType=%s, appName=%s, version=%s", ccodVersion, appType==null ? "":appType.name, appName, version));
        List<K8sObjectTemplatePo> list = new ArrayList<>();
        if(StringUtils.isNotBlank(version)){
            for(K8sObjectTemplatePo template : objectTemplateList){
                if (isAppVersionMatch(template, ccodVersion, appType, appName, version)) {
                    list.add(template);
                }
            }
        }
        else if(StringUtils.isNotBlank(appName)){
            for(K8sObjectTemplatePo template : objectTemplateList){
                if (isAppNameMatch(template, ccodVersion, appType, appName)) {
                    list.add(template);
                }
            }
        }
        else if(appType != null){
            for(K8sObjectTemplatePo template : objectTemplateList){
                if (isAppTypeMatch(template, ccodVersion, appType)) {
                    list.add(template);
                }
            }
        }
        else if(StringUtils.isNotBlank(ccodVersion)){
            for(K8sObjectTemplatePo template : objectTemplateList){
                if (isCcodVersionMatch(template, ccodVersion)) {
                    list.add(template);
                }
            }
        }
        else{
            list.addAll(objectTemplateList);
        }
        logger.debug(String.format("find template %s for ccodVersion=%s, appType=%s, appName=%s, version=%s",
                gson.toJson(list), ccodVersion, appType==null ? "":appType.name, appName, version));
        return list;
    }

    @Override
    public List<K8sObjectTemplatePo> queryK8sObjectTemplate(Map<String, String> labels) {
        logger.debug(String.format("begin to query template with labels=%s", gson.toJson(labels)));
        if(labels == null || labels.size() == 0){
            return objectTemplateList;
        }
        List<K8sObjectTemplatePo> retList = new ArrayList<>();
        for(K8sObjectTemplatePo template : objectTemplateList){
            boolean isMatch = true;
            for(String k : labels.keySet()){
                String v = labels.get(k);
                String c = template.getLabels().get(k);
                if(k.equals(ccodVersionLabel) || k.equals(appTypeLabel)){
                    if(StringUtils.isBlank(c) || !c.equals(v)){
                        isMatch = false;
                        break;
                    }
                }
                else if(k.equals(appTagLabel)){
                    if(!isTagMatch(v, c)){
                        isMatch = false;
                        break;
                    }
                }
                else if(k.equals(platformTagLabel)){
                    if(!isTagMatch(v, c) && !isTagMatch(String.format("%s,base", v), c)){
                        isMatch = false;
                        break;
                    }
                }
                else {
                    if(StringUtils.isBlank(c) || (!isVersionMatch(c, v) && !c.equals("*") && !c.equals(forAllVersion))){
                        isMatch = false;
                        break;
                    }

                }
            }
            if(isMatch){
                retList.add(template);
            }
        }
        logger.debug(String.format("find %d template for labels=%s", retList.size(), gson.toJson(labels)));
        return retList;
    }

    @Override
    public void addNewK8sObjectTemplate(K8sObjectTemplatePo template) throws ParamException {
        logger.debug(String.format("begin to add new template %s", gson.toJson(template)));
        checkTemplate(template);
        for(K8sObjectTemplatePo exist : objectTemplateList){
            if(isEqual(template.getLabels(), exist.getLabels())){
                throw new ParamException(String.format("template for %s has exist", gson.toJson(template.getLabels())));
            }
        }
        K8sTemplatePo po = new K8sTemplatePo(template);
        k8sTemplateMapper.insert(po);
        List<K8sTemplatePo> list = k8sTemplateMapper.select();
        objectTemplateList.clear();
        list.forEach(t->objectTemplateList.add(t.getObjectTemplate()));
        logger.info(String.format("add template success"));
    }

    @Override
    public void updateK8sObjectTemplate(K8sObjectTemplatePo template) throws ParamException {
        logger.debug(String.format("begin to update template %s", gson.toJson(template)));
        checkTemplate(template);
        K8sTemplatePo updated = null;
        List<K8sTemplatePo> list = k8sTemplateMapper.select();
        for(K8sTemplatePo exist : list){
            if(isEqual(template.getLabels(), exist.getLabels())){
                updated = exist;
                break;
            }
        }
        if(updated == null){
            throw new ParamException(String.format("updated template with labels %s not exist", gson.toJson(template.getLabels())));
        }
        updated.setObjectTemplate(template);
        k8sTemplateMapper.update(updated);
        list = k8sTemplateMapper.select();
        objectTemplateList.clear();
        list.forEach(t->objectTemplateList.add(t.getObjectTemplate()));
        logger.info(String.format("template update success"));
    }

    @Override
    public void deleteObjectTemplate(Map<String, String> labels) throws ParamException {
        logger.debug(String.format("begin to delete template with labels %s", gson.toJson(labels)));
        K8sTemplatePo updated = null;
        List<K8sTemplatePo> list = k8sTemplateMapper.select();
        for(K8sTemplatePo exist : list){
            if(isEqual(labels, exist.getLabels())){
                updated = exist;
                break;
            }
        }
        if(updated == null){
            throw new ParamException(String.format("delete template with labels %s not exist", gson.toJson(labels)));
        }
        k8sTemplateMapper.delete(updated.getId());
        list = k8sTemplateMapper.select();
        objectTemplateList.clear();
        list.forEach(t->objectTemplateList.add(t.getObjectTemplate()));
        logger.info(String.format("template delete success"));
    }

    @Override
    public List<K8sObjectTemplatePo> cloneExistPlatformTemplate(String srcCcodVersion, String srcPlatformTag, String dstCcodVersion, String dstPlatformTag) throws ParamException {
        logger.debug(String.format("clone ccod template from ccodVersion=%s and platformTag=%s to %s(%s)",
                srcCcodVersion, srcPlatformTag, dstCcodVersion, dstPlatformTag));
        if(StringUtils.isBlank(srcCcodVersion)){
            throw new ParamException(String.format("srcCcodVersion can not be blank"));
        }
        if(StringUtils.isBlank(srcPlatformTag)){
            throw new ParamException(String.format("srcPlatformTag can not be blank"));
        }
        if(StringUtils.isBlank(dstCcodVersion)){
            throw new ParamException(String.format("dstCcodVersion can not be blank"));
        }
        if(StringUtils.isBlank(dstPlatformTag)){
            throw new ParamException(String.format("dstPlatformTag can not be blank"));
        }
        if(srcCcodVersion.equals(dstCcodVersion) && srcPlatformTag.equals(dstCcodVersion)){
            throw new ParamException(String.format("can not clone to self"));
        }
        List<K8sObjectTemplatePo> templateList = new ArrayList<>();
        for(K8sObjectTemplatePo obj : objectTemplateList){
            String ccodVersion = obj.getLabels().get(ccodVersionLabel);
            String tag = obj.getLabels().get(platformTagLabel);
            if(ccodVersion == null || !ccodVersion.equals(srcCcodVersion) || tag == null ){
                continue;
            }
            if(!isTagMatch(tag, srcPlatformTag) && !isTagMatch(tag, String.format("%s,base", srcPlatformTag))){
                continue;
            }
            K8sObjectTemplatePo template = gson.fromJson(gson.toJson(obj), K8sObjectTemplatePo.class);
            template.getLabels().put(ccodVersionLabel, dstCcodVersion);
            if(isTagMatch(tag, srcPlatformTag)){
                template.getLabels().put(platformTagLabel, dstPlatformTag);
            }
            else{
                template.getLabels().put(platformTagLabel, String.format("%s,base", dstPlatformTag));
            }
            checkTemplate(template);
            templateList.add(template);
        }
        for(K8sObjectTemplatePo obj : templateList){
            K8sTemplatePo template = new K8sTemplatePo(obj);
            k8sTemplateMapper.insert(template);
        }
        ccodThreePartAppMapper.delete(dstCcodVersion, dstPlatformTag, null, null);
        ccodThreePartAppMapper.select(srcCcodVersion, "standard", null).forEach(a->{
            a.setCcodVersion(dstCcodVersion);
            a.setTag(dstPlatformTag);
            ccodThreePartAppMapper.insert(a);
        });
        objectTemplateList.clear();
        k8sTemplateMapper.select().forEach(t->objectTemplateList.add(t.getObjectTemplate()));
        return templateList;
    }

    @Override
    public List<K8sObjectTemplatePo> cloneExistAppTemplate(String srcCcodVersion, String dstCcodVersion) throws ParamException {
        logger.debug(String.format("clone ccod app template from ccodVersion=%s  to %s", srcCcodVersion, dstCcodVersion));
        if(StringUtils.isBlank(srcCcodVersion)){
            throw new ParamException(String.format("srcCcodVersion can not be blank"));
        }
        if(StringUtils.isBlank(dstCcodVersion)){
            throw new ParamException(String.format("dstCcodVersion can not be blank"));
        }
        if(srcCcodVersion.equals(dstCcodVersion)){
            throw new ParamException(String.format("srcCcodVersion and dstCcodVersion can not be equal"));
        }
        List<K8sObjectTemplatePo> templateList = new ArrayList<>();
        for(K8sObjectTemplatePo obj : objectTemplateList){
            String ccodVersion = obj.getLabels().get(ccodVersionLabel);
            String appType = obj.getLabels().get(appTypeLabel);
            if(ccodVersion == null || !ccodVersion.equals(srcCcodVersion) || appType == null || obj.getLabels().containsKey(appTagLabel)){
                continue;
            }
            if(!appType.equals(AppType.THREE_PART_APP.name) && obj.getLabels().size() > 2){
                continue;
            }
            K8sObjectTemplatePo template = gson.fromJson(gson.toJson(obj), K8sObjectTemplatePo.class);
            template.getLabels().put(ccodVersionLabel, dstCcodVersion);
            checkTemplate(template);
            templateList.add(template);
        }
        for(K8sObjectTemplatePo obj : templateList){
            K8sTemplatePo template = new K8sTemplatePo(obj);
            k8sTemplateMapper.insert(template);
        }
        objectTemplateList.clear();
        k8sTemplateMapper.select().forEach(t->objectTemplateList.add(t.getObjectTemplate()));
        return templateList;
    }

    private void checkTemplate(K8sObjectTemplatePo template) throws ParamException
    {
        if(template.getLabels() == null || template.getLabels().size() == 0){
            throw new ParamException("labels of template can not be empty");
        }
        for(String key : template.getLabels().keySet()){
            if(StringUtils.isBlank(template.getLabels().get(key))){
                throw new ParamException(String.format("value of %s can not be blank", key));
            }
        }
        Map<String, String> labels = new HashMap<>();
        for(String key : template.getLabels().keySet()){
            labels.put(key, template.getLabels().get(key));
        }
        if(!labels.containsKey(ccodVersionLabel)){
            throw new ParamException(String.format("labels of template mush has %s label", ccodVersionLabel));
        }
        if(!labels.containsKey(appTypeLabel)){
            for(String key : labels.keySet()){
                if(!key.equals(ccodVersionLabel) && !key.equals(platformTagLabel)){
                    throw new ParamException(String.format("without %s, labels only support %s and %s tag", appTypeLabel, ccodVersionLabel, platformTagLabel));
                }
            }
            if(template.getPvList() == null){
                throw new ParamException("pvList can not been null");
            }
            if(template.getPvcList() == null){
                throw new ParamException("pvc can not been null");
            }
            if(template.getNamespaces() == null){
                throw new ParamException("namespace can not been null");
            }
            if(template.getSecrets() == null){
                throw new ParamException("secret can not be null");
            }
            if(template.getEndpoints() != null){
                throw new ParamException("endpoints should be null");
            }
            if(template.getIngresses() != null){
                throw new ParamException("ingresses should be null");
            }
            if(template.getServices() != null){
                throw new ParamException("services should be null");
            }
            if(template.getDeployments() != null){
                throw new ParamException("deployments should be null");
            }
            if(template.getStatefulSets() != null){
                throw new ParamException("statefulSets should be null");
            }
            if(StringUtils.isBlank(template.getComment())){
                throw new ParamException("comment of template can not be blank");
            }
            return;
        }
        if(template.getPvList() != null){
            throw new ParamException("pvList should been null");
        }
        if(template.getPvcList() != null){
            throw new ParamException("pvc should be null");
        }
        if(template.getNamespaces() != null){
            throw new ParamException("namespace should been null");
        }
        if(template.getEndpoints() == null && template.getIngresses() == null && template.getStatefulSets() == null
                && template.getDeployments() == null && template.getServices() == null){
            throw new ParamException("deployments, services, ingresses, endpoints and statefulSets can not be null at same time");
        }
        AppType appType = AppType.getEnum(labels.get(appTypeLabel));
        if(appType == null){
            throw new ParamException(String.format("%s is unsupported app type", labels.get(appTypeLabel)));
        }
        for(String tag : templateSupportTags){
            if(labels.containsKey(tag)){
                labels.remove(tag);
            }
        }
        switch (appType){
            case THREE_PART_APP:
                if(labels.size() == 0){
                    throw new ParamException(String.format("%s template must appName and version info, example: mysql=ANY_VERSION or mysql=5.7.1", appType.name));
                }
                else if(labels.size() > 1){
                    throw new ParamException(String.format("%s template support only one app and can not support %s at same time", appType.name, String.join(",", labels.keySet())));
                }
                break;
            default:
                for(String key : labels.keySet()){
                    if(!appManagerService.isSupport(key)){
                        throw new ParamException(String.format("app %s is not been supported", key));
                    }
//                    List<AppModuleVo> modules = appManagerService.queryApps(key, true);
//                    if(!appType.equals(modules.get(0))){
//                        throw new ParamException(String.format("appType of %s is %s not %s", key, modules.get(0).getAppType().name, appType.name));
//                    }
                }

        }
    }

    @Override
    public List<ExtensionsV1beta1Ingress> generateIngress(AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException {
        Map<String, String> k8sMacroData = appBase.getK8sMacroData(domain, platform);
        Object selectObject = selectK8sObjectForApp(K8sKind.INGRESS, appBase.getAppName(), appBase.getVersion(), appBase.getAppType(), appBase.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find ingress template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    appBase.getAppName(), appBase.getVersion(), appBase.getAppType().name, platform.getCcodVersion(), appBase.getTag()));
        }
        List<ExtensionsV1beta1Ingress> ingress = (List<ExtensionsV1beta1Ingress>)selectObject;
        return ingress;
    }

    @Override
    public V1Service generateCCODDomainAppService(AppUpdateOperationInfo appBase, ServicePortType portType, String portStr, DomainPo domain, PlatformPo platform) throws ParamException {
        AppType appType = appBase.getAppType();
        String alias = appBase.getAlias();
        String domainId = domain.getDomainId();
        String appName = appBase.getAppName();
        String platformId = platform.getPlatformId();
        logger.debug(String.format("generate service for %s(%s) : portType=%s and port=%s", alias, appName, portType.name, portStr));
        if(!portType.equals(ServicePortType.ClusterIP) && !portType.equals(ServicePortType.NodePort))
            throw new ParamException(String.format("can not handle service port type : %s", portType.name));
        Map<String, String> k8sMacroData = appBase.getK8sMacroData(domain, platform);
        Object selectObject = selectK8sObjectForApp(K8sKind.SERVICE, appBase.getAppName(), appBase.getVersion(), appBase.getAppType(), appBase.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find ingress template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    appBase.getAppName(), appBase.getVersion(), appBase.getAppType().name, platform.getCcodVersion(), appBase.getTag()));
        }
        V1Service service = new V1Service();
        service.setApiVersion("v1");
        service.setKind("Service");
        service.setMetadata(new V1ObjectMeta());
        service.setSpec(new V1ServiceSpec());
        List<PortVo> portList = parsePort(portStr, portType, appType);;
        String name = portType.equals(ServicePortType.NodePort) ? String.format("%s-%s-out", alias, domainId) : String.format("%s-%s", alias, domainId);
        service.getMetadata().setLabels(new HashMap<>());
        service.getMetadata().getLabels().put(this.domainIdLabel, domainId);
        service.getMetadata().getLabels().put(appName, alias);
        service.getMetadata().setName(name);
        service.getMetadata().setNamespace(platformId);
        service.getSpec().setSelector(new HashMap<>());
        service.getSpec().getSelector().put(this.domainIdLabel, domainId);
        service.getSpec().getSelector().put(appName, alias);
        service.getSpec().setPorts(new ArrayList<>());
        service.getSpec().setType(portType.name);
        for(PortVo portVo : portList)
        {
            V1ServicePort svcPort = new V1ServicePort();
            svcPort.setPort(portVo.getPort());
            svcPort.setName(portVo.getPort() + "");
            svcPort.setProtocol(portVo.getProtocol());
            if(portType.equals(ServicePortType.NodePort))
                svcPort.setNodePort(portVo.getNodePort());
            else
                svcPort.setTargetPort(new IntOrString(portVo.getTargetPort()));
            service.getSpec().getPorts().add(svcPort);
        }
        logger.info(String.format("service for %s(%s), portType=%s and port=%s)", alias, appName, portType.name, portStr));
        return service;
    }

    @Override
    public V1Deployment generateCCODDomainAppDeployment(AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException {
        String domainId = domain.getDomainId();
        List<AppFileNexusInfo> domainCfg = domain.getCfgs() == null ? new ArrayList<>() : domain.getCfgs();
        logger.debug(String.format("generate deployment for %s : domainId=%s", gson.toJson(appBase), domainId));
        String appName = appBase.getAppName();
        String alias = appBase.getAlias();
        String version = appBase.getVersion();
        String hostIp = appBase.getHostIp();
        boolean fixedIp = appBase.isFixedIp();
        AppModuleVo module = this.appManagerService.queryAllRegisterAppModule(true).stream()
                .collect(Collectors.groupingBy(AppModuleVo::getAppName)).get(appName).stream()
                .collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version);
        appBase.setInstallPackage(module.getInstallPackage());
        AppType appType = appBase.getAppType() == null ? module.getAppType() : appBase.getAppType();
        Map<String, String> k8sMacroData = appBase.getK8sMacroData(domain, platform);
        Object selectObject = selectK8sObjectForApp(K8sKind.DEPLOYMENT, appBase.getAppName(), appBase.getVersion(), appBase.getAppType(), appBase.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find ingress template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    appBase.getAppName(), appBase.getVersion(), appBase.getAppType().name, platform.getCcodVersion(), appBase.getTag()));
        }
        V1Deployment deploy = ((List<V1Deployment>)selectObject).get(0);
        if(fixedIp){
            deploy.getSpec().getTemplate().getSpec().setNodeSelector(new HashMap<>());
            deploy.getSpec().getTemplate().getSpec().getNodeSelector().put(nodeIpLabel, hostIp);
        }
        String basePath = appBase.getBasePath();
        String deployPath = getAbsolutePath(appBase.getBasePath(), appBase.getDeployPath());
        String platformId = platform.getPlatformId();
//        deploy.getMetadata().setNamespace(platformId);
//        deploy.getMetadata().setName(String.format("%s-%s", alias, domainId));
//        deploy.getMetadata().setLabels(new HashMap<>());
//        deploy.getMetadata().getLabels().put(this.appTypeLabel, appType.name);
//        deploy.getMetadata().getLabels().put(this.domainIdLabel, domainId);
//        deploy.getMetadata().getLabels().put(appName, alias);
//        deploy.getSpec().getSelector().setMatchLabels(new HashMap<>());
//        deploy.getSpec().getSelector().getMatchLabels().put(this.domainIdLabel, domainId);
//        deploy.getSpec().getSelector().getMatchLabels().put(appName, alias);
//        deploy.getSpec().getTemplate().getMetadata().setLabels(new HashMap<>());
//        deploy.getSpec().getTemplate().getMetadata().getLabels().put(this.domainIdLabel, domainId);
//        deploy.getSpec().getTemplate().getMetadata().getLabels().put(appName, alias);
        List<V1Volume> volumes = generateVolumeForDeployment(deploy, appType, alias, platformId, domainId, appBase.getCfgs(), platform.getCfgs(), domainCfg);
        deploy.getSpec().getTemplate().getSpec().setVolumes(volumes);
        V1Container initContainer = deploy.getSpec().getTemplate().getSpec().getInitContainers().get(0);
//        logger.debug(String.format("set initContainer name : %s", alias));
//        initContainer.setName(alias);
        List<V1VolumeMount> mounts = generateInitContainerMount(initContainer, appType, alias, domainId, basePath, deployPath);
        initContainer.setVolumeMounts(mounts);
        String image = String.format("%s/ccod/%s:%s", this.nexusDockerUrl, appName.toLowerCase(), version.replaceAll("\\:", "-"));
//        logger.debug(String.format("modify image of init container to %s", image));
//        initContainer.setImage(image);
        V1Container runtimeContainer = deploy.getSpec().getTemplate().getSpec().getContainers().get(0);
//        logger.debug(String.format("set container name to %s-runtime", alias));
//        runtimeContainer.setName(String.format("%s-runtime", alias));
//        if(appType.equals(AppType.JAR) || appType.equals(AppType.NODEJS)){
//            runtimeContainer.setImage(image);
//        }
        mounts = generateRuntimeContainerMount(runtimeContainer, appBase, platformId, domainId, platform.getCfgs(), domainCfg);
        runtimeContainer.setVolumeMounts(mounts);
        logger.debug(String.format("generate init container commands"));
        String packageFileName = module.getInstallPackage().getFileName();
        List<String> commands = generateCmdForInitContainer(appBase, packageFileName, appBase.getCfgs(), domainId);
        initContainer.setCommand(commands);
        initContainer.setArgs(new ArrayList<>());
        logger.debug(String.format("generate runtime container command"));
        commands = generateCmdForRuntimeContainer(appBase, platformId, domainId, platform.getCfgs(), domainCfg);
        runtimeContainer.setCommand(commands);
        runtimeContainer.setArgs(new ArrayList<>());
        List<V1ContainerPort> containerPorts = generateContainerPortsForRuntimeContainer(appBase.getPorts(), appType);
        logger.debug(String.format("containerPorts of %s runtime container at %s is : %s", alias, domainId, gson.toJson(containerPorts)));
        runtimeContainer.setPorts(containerPorts);
        generateProbeForRuntimeContainer(runtimeContainer, alias, domainId, appType, appBase.getPorts(),
                appBase.getCheckAt(), appBase.getInitialDelaySeconds(), appBase.getPeriodSeconds());
        if(appType.equals(AppType.RESIN_WEB_APP) || appType.equals(AppType.TOMCAT_WEB_APP))
        {
            logger.debug(String.format("modify deployment hostnames of hostAliases to %s", platform.getHostUrl()));
            deploy.getSpec().getTemplate().getSpec().getHostAliases().get(0).getHostnames().set(0, platform.getHostUrl());
            deploy.getSpec().getTemplate().getSpec().getHostAliases().get(0).setIp((String)platform.getParams().get(PlatformBase.k8sHostIpKey));
        }
        logger.info(String.format("generated deployment for %s : %s", gson.toJson(appBase), gson.toJson(deploy)));
        return deploy;
    }

    private void generateMountAndCmdForSingleDeployment(V1Deployment deploy, AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException{
        AppType appType = appBase.getAppType();
        String alias = appBase.getAlias();
        String domainId = domain.getDomainId();
        List<AppFileNexusInfo> domainCfg = domain.getCfgs();
        if(appBase.isFixedIp()){
            deploy.getSpec().getTemplate().getSpec().setNodeSelector(new HashMap<>());
            deploy.getSpec().getTemplate().getSpec().getNodeSelector().put(nodeIpLabel, appBase.getHostIp());
        }
        String basePath = appBase.getBasePath();
        String deployPath = getAbsolutePath(appBase.getBasePath(), appBase.getDeployPath());
        String platformId = platform.getPlatformId();
        List<V1Volume> volumes = generateVolumeForDeployment(deploy, appType, alias, platformId, domainId, appBase.getCfgs(), platform.getCfgs(), domainCfg);
        deploy.getSpec().getTemplate().getSpec().setVolumes(volumes);
        V1Container initContainer = deploy.getSpec().getTemplate().getSpec().getInitContainers().get(0);
        List<V1VolumeMount> mounts = generateInitContainerMount(initContainer, appType, alias, domainId, basePath, deployPath);
        initContainer.setVolumeMounts(mounts);
        V1Container runtimeContainer = deploy.getSpec().getTemplate().getSpec().getContainers().get(0);
        mounts = generateRuntimeContainerMount(runtimeContainer, appBase, platformId, domainId, platform.getCfgs(), domainCfg);
        runtimeContainer.setVolumeMounts(mounts);
        logger.debug(String.format("generate init container commands"));
        String packageFileName = appBase.getInstallPackage().getFileName();
        List<String> commands = generateCmdForInitContainer(appBase, packageFileName, appBase.getCfgs(), domainId);
        initContainer.setCommand(commands);
        initContainer.setArgs(new ArrayList<>());
        logger.debug(String.format("generate runtime container command"));
        commands = generateCmdForRuntimeContainer(appBase, platformId, domainId, platform.getCfgs(), domainCfg);
        runtimeContainer.setCommand(commands);
        runtimeContainer.setArgs(new ArrayList<>());
        List<V1ContainerPort> containerPorts = generateContainerPortsForRuntimeContainer(appBase.getPorts(), appType);
        logger.debug(String.format("containerPorts of %s runtime container at %s is : %s", alias, domainId, gson.toJson(containerPorts)));
        runtimeContainer.setPorts(containerPorts);
        generateProbeForRuntimeContainer(runtimeContainer, alias, domainId, appType, appBase.getPorts(),
                appBase.getCheckAt(), appBase.getInitialDelaySeconds(), appBase.getPeriodSeconds());
        if(appType.equals(AppType.RESIN_WEB_APP) || appType.equals(AppType.TOMCAT_WEB_APP))
        {
            logger.debug(String.format("modify deployment hostnames of hostAliases to %s", platform.getHostUrl()));
            deploy.getSpec().getTemplate().getSpec().getHostAliases().get(0).getHostnames().set(0, platform.getHostUrl());
            deploy.getSpec().getTemplate().getSpec().getHostAliases().get(0).setIp((String)platform.getParams().get(PlatformBase.k8sHostIpKey));
        }
        logger.info(String.format("generated deployment for %s : %s", gson.toJson(appBase), gson.toJson(deploy)));
    }

    private List<PortVo> parsePort(String portStr, ServicePortType portType, AppType appType) throws ParamException
    {
        List<PortVo> portList = new ArrayList<>();
        String[] ports = portStr.split(",");
        for(String thePort : ports)
        {
            if(!thePort.matches(this.portRegex))
                throw new ParamException(String.format("%s is illegal port string", thePort));
            PortVo portVo = PortVo.parse(thePort, portType);
            if(portType.equals(ServicePortType.ClusterIP) && portVo.getTargetPort() == 0)
            {
                switch (appType)
                {
                    case BINARY_FILE:
                    case NODEJS:
                        portVo.setTargetPort(portVo.getPort());
                        break;
                    case TOMCAT_WEB_APP:
                    case RESIN_WEB_APP:
                    case JAR:
                        portVo.setTargetPort(8080);
                        break;
                    default:
                        throw new ParamException(String.format("can not handle port for appType=%s", appType.name));
                }
            }
            portList.add(portVo);
        }
        return portList;
    }

    private List<V1VolumeMount> generateRuntimeContainerMount(V1Container runtimeContainer, AppBase appBase, String platformId, String domainId, List<AppFileNexusInfo> platformCfg, List<AppFileNexusInfo> domainCfg) throws ParamException
    {
        logger.debug(String.format("generate runtime container volume mount"));
        String basePath = appBase.getBasePath();
        String deployPath = getAbsolutePath(basePath, appBase.getDeployPath());
        String alias = appBase.getAlias();
        AppType appType = appBase.getAppType();
        Map<String, V1VolumeMount> volumeMountMap = runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()));
        String pkgPath = getAbsolutePath(basePath, deployPath).replaceAll("/$", "");
        String logPath = appType.equals(AppType.TOMCAT_WEB_APP) ? pkgPath.replaceAll("/[^/]+$", "/logs") : pkgPath.replaceAll("/[^/]+$", "/log");
        volumeMountMap.get("ccod-runtime").setMountPath(logPath);
        volumeMountMap.get("ccod-runtime").setSubPath(alias);
        logger.debug(String.format("modify volume mount ccod-runtime to %s", gson.toJson(volumeMountMap.get("ccod-runtime"))));
        if(domainCfg != null && domainCfg.size() > 0)
        {
            String mountName = String.format("%s-volume", domainId);
            logger.debug(String.format("add domain public config mount %s to container", mountName));
            V1VolumeMount mount = new V1VolumeMount();
            mount.setName(mountName);
            volumeMountMap.put(mountName, mount);
            volumeMountMap.get(mountName).setMountPath(String.format("/cfg/%s", domainId));
        }
        if(platformCfg != null && platformCfg.size() > 0)
        {
            String mountName = String.format("%s-volume", platformId);
            logger.debug(String.format("add platform public config mount %s to container", mountName));
            V1VolumeMount mount = new V1VolumeMount();
            mount.setName(mountName);
            volumeMountMap.put(mountName, mount);
            volumeMountMap.get(mountName).setMountPath(String.format("/cfg/%s", platformId));
        }
        if(volumeMountMap.containsKey("war")){
            volumeMountMap.get("war").setMountPath(deployPath);
        }
        if(volumeMountMap.containsKey("binary-file")){
            volumeMountMap.get("binary-file").setMountPath(basePath);
        }
        if(appType.equals(AppType.NODEJS)){
            String mountName = String.format("%s-%s-volume", alias, domainId);
            logger.debug(String.format("add app config mount %s to container", mountName));
            V1VolumeMount mount = new V1VolumeMount();
            mount.setName(mountName);
            volumeMountMap.put(mountName, mount);
            volumeMountMap.get(mountName).setMountPath(String.format("/cfg/%s-%s", alias, domainId));
        }
        return new ArrayList<>(volumeMountMap.values());
    }

    private List<V1VolumeMount> generateInitContainerMount(V1Container initContainer, AppType appType, String alias, String domainId, String basePath, String deployPath) throws ParamException
    {
        if(appType.equals(AppType.NODEJS)){
            return new ArrayList<>();
        }
        logger.debug(String.format("generate init container volume mount"));
        Map<String, V1VolumeMount> volumeMountMap = initContainer.getVolumeMounts().stream()
                .collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()));
        String mountName = String.format("%s-%s-volume", alias, domainId);
        if(!volumeMountMap.containsKey(mountName))
        {
            logger.debug(String.format("add mount %s to init container", mountName));
            V1VolumeMount mount = new V1VolumeMount();
            mount.setName(String.format("%s-%s-volume", alias, domainId));
            volumeMountMap.put(mountName, mount);
        }
        volumeMountMap.get(mountName).setMountPath(String.format("/cfg/%s-cfg", alias));
        return new ArrayList<>(volumeMountMap.values());
    }

    private List<V1Volume> generateVolumeForDeployment(V1Deployment deploy, AppType appType, String alias, String platformId, String domainId, List<AppFileNexusInfo> appCfgs, List<AppFileNexusInfo> platformCfg, List<AppFileNexusInfo> domainCfg)
    {
        logger.debug(String.format("generate volumes for %s at %s", alias, domainId));
        Map<String, V1Volume> volumeMap = deploy.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity()));
        logger.debug(String.format("generate app configMap volume"));
        String configMapName = String.format("%s-%s", alias, domainId);
        V1Volume volume = generateConfigMapVolume(configMapName, appCfgs);
        volumeMap.put(configMapName, volume);
        if(domainCfg != null && domainCfg.size() > 0)
        {
            logger.debug(String.format("generate domain public configMap volume"));
            configMapName = domainId;
            volume = generateConfigMapVolume(configMapName, domainCfg);
            volumeMap.put(configMapName, volume);
        }
        if(platformCfg != null && platformCfg.size() > 0)
        {
            logger.debug(String.format("generate platform public configMap volume"));
            configMapName = platformId;
            volume = generateConfigMapVolume(configMapName, platformCfg);
            volumeMap.put(configMapName, volume);
        }
//        volumeMap.get("ccod-runtime").getHostPath().setPath(String.format("/var/ccod-runtime/%s/%s", platformId, domainId));
//        volumeMap.get("ccod-runtime").getHostPath().setType("DirectoryOrCreate");
//        logger.debug(String.format("modify ccod-runtime volume to %s", gson.toJson(volumeMap.get("ccod-runtime"))));
//        if(volumeMap.containsKey("core"))
//        {
//            volumeMap.get("core").getHostPath().setPath(String.format("/home/kubernetes/%s/core", platformId));
//            volumeMap.get("core").getHostPath().setType("");
//            logger.debug(String.format("modify core volume to %s", gson.toJson(volumeMap.get("core"))));
//        }
        return new ArrayList<>(volumeMap.values());
    }

    private V1Volume generateConfigMapVolume(String configMapName, List<AppFileNexusInfo> cfgs)
    {
        V1ConfigMapVolumeSource source = new V1ConfigMapVolumeSource();
        source.setItems(new ArrayList<>());
        source.setName(configMapName);
        for (AppFileNexusInfo cfg : cfgs) {
            V1KeyToPath item = new V1KeyToPath();
            item.setKey(cfg.getFileName());
            item.setPath(cfg.getFileName());
            source.getItems().add(item);
        }
        V1Volume volume = new V1Volume();
        volume.setName(String.format("%s-volume", configMapName));
        volume.setConfigMap(source);
        return volume;
    }

    private List<String> generateCmdForRuntimeContainer(AppBase appBase,String platformId, String domainId, List<AppFileNexusInfo> platformCfg, List<AppFileNexusInfo> domainCfg) throws ParamException
    {
        String alias = appBase.getAlias();
        AppType appType = appBase.getAppType();
        List<String> commands = new ArrayList<>();
        commands.add(0, "/bin/sh");
        commands.add(1, "-c");
        String basePath = appBase.getBasePath().trim().replaceAll("/$", "");
        if(appType.equals(AppType.JAR) && basePath.equals("/root")){
            throw new ParamException("base path of JAR can not be /root");
        }
        String deployPath = getAbsolutePath(basePath, appBase.getDeployPath()).replaceAll("/$", "");
        String execParam;
        switch (appType){
            case JAR:
                execParam = String.format("mkdir %s -p;mv /root/%s %s", deployPath, appBase.getInstallPackage().getFileName(), deployPath);
                break;
            default:
                execParam = "";
                break;
        }
        execParam = String.format("%s;cd %s", execParam, basePath);
        if(platformCfg != null && platformCfg.size() > 0)
        {
            String mountPath = String.format("/cfg/%s", platformId);
            Map<String, List<AppFileNexusInfo>> deployPathCfgMap = platformCfg.stream().collect(Collectors.groupingBy(AppFileNexusInfo::getDeployPath));
            for (String cfgDeployPath : deployPathCfgMap.keySet()) {
                String absolutePath = getAbsolutePath(basePath, cfgDeployPath);
                execParam = String.format("%s;mkdir %s -p", execParam, absolutePath);
                for (AppFileNexusInfo cfg : deployPathCfgMap.get(cfgDeployPath)) {
                    execParam = String.format("%s;cp %s/%s %s/%s", execParam, mountPath, cfg.getFileName(), absolutePath, cfg.getFileName());
                }
            }
        }
        if(domainCfg != null && domainCfg.size() > 0)
        {
            String mountPath = String.format("/cfg/%s", domainId);
            Map<String, List<AppFileNexusInfo>> deployPathCfgMap = domainCfg.stream().collect(Collectors.groupingBy(AppFileNexusInfo::getDeployPath));
            for (String cfgDeployPath : deployPathCfgMap.keySet()) {
                String absolutePath = getAbsolutePath(basePath, cfgDeployPath);
                execParam = String.format("%s;mkdir %s -p", execParam, absolutePath);
                for (AppFileNexusInfo cfg : deployPathCfgMap.get(cfgDeployPath)) {
                    execParam = String.format("%s;cp %s/%s %s/%s", execParam, mountPath, cfg.getFileName(), absolutePath, cfg.getFileName());
                }
            }
        }
        switch (appType){
            case NODEJS:
            {
                String mountPath = String.format("/cfg/%s-%s", alias, domainId);
                for(AppFileNexusInfo cfg : appBase.getCfgs()){
                    String cfgSavePath = String.format("%s/%s", cfg.getDeployPath(), cfg.getFileName()).replaceAll("//", "/");
                    execParam = String.format("%s;cd %s;cp %s/%s %s", execParam, basePath, mountPath, cfg.getFileName(), cfgSavePath);
                }
                break;
            }
            default:
                break;
        }
        String cwd;
        switch (appType){
            case BINARY_FILE:
            case JAR:
            case NODEJS:
                cwd = deployPath;
                break;
            default:
                cwd = basePath;
                break;
        }
        if(StringUtils.isNotBlank(appBase.getInitCmd()))
            execParam = String.format("%s;cd %s;%s", execParam, cwd, appBase.getInitCmd());
        switch (appType){
            case NODEJS:
                if(!deployPath.matches(".*/nginx/html(/)?$")){
                    String deployDir = deployPath.replaceAll(".*/", "");
                    String newDir = String.format("%s-%s", alias, domainId);
                    execParam = String.format("%s;cd %s;mv %s %s", execParam, deployPath.replaceAll(String.format("/%s$", deployDir), ""), deployDir, newDir);
                    basePath = basePath.replaceAll(String.format("/%s$", deployDir), "/" + newDir);
                    cwd = deployPath.replaceAll(String.format("/%s$", deployDir), "/" + newDir);
                    deployPath = cwd;
                }
                break;
            default:
                break;
        }
        execParam = String.format("%s;cd %s;%s", execParam, cwd, appBase.getStartCmd());
        if(StringUtils.isNotBlank(appBase.getLogOutputCmd()))
            execParam = String.format("%s;cd %s;%s", execParam, cwd, appBase.getLogOutputCmd());
        commands.add(execParam.replaceAll("^;", "").replaceAll(";;", ";"));
        logger.debug(String.format("command for %s at %s is : %s", alias, domainId, String.join(";", commands)));
        return commands;
    }

    private List<String> generateCmdForInitContainer(AppBase appBase, String packageFileName, List<AppFileNexusInfo> appCfgs, String domainId) throws ParamException
    {
        if(appBase.getAppType().equals(AppType.NODEJS)){
            return Arrays.asList(new String[]{"/bin/sh", "-c", ""});
        }
        String alias = appBase.getAlias();
        List<String> commands = new ArrayList<>();
        commands.add(0, "/bin/sh");
        commands.add(1, "-c");
        AppType appType = appBase.getAppType();
        String appName = appBase.getAppName();
        String theName = packageFileName.replaceAll("\\.war$", "");
        String basePath = appType.equals(AppType.BINARY_FILE) || appType.equals(AppType.JAR) ? "/binary-file" : "/opt";
        String deployPath = getAbsolutePath(basePath, appBase.getDeployPath());
        String execParam = "";
        String mountPath = String.format("/cfg/%s-cfg", alias);
        switch (appType)
        {
            case BINARY_FILE:
                execParam = String.format("mkdir %s -p;mkdir %s/log -p;mv /opt/%s %s/%s", deployPath, basePath, packageFileName, deployPath, packageFileName);
                break;
            case TOMCAT_WEB_APP:
            case RESIN_WEB_APP:
                execParam = String.format("mkdir %s -p;cd %s;mv /opt/%s %s/%s", deployPath, deployPath, packageFileName, deployPath, packageFileName);
                break;
            case JAR:
            case NODEJS:
                break;
            default:
                throw new ParamException(String.format("error appType %s", appType.name));
        }
        Map<String, List<AppFileNexusInfo>> deployPathCfgMap = appCfgs.stream().collect(Collectors.groupingBy(AppFileNexusInfo::getDeployPath));
        for (String cfgDeployPath : deployPathCfgMap.keySet()) {
            String absolutePath = getAbsolutePath(basePath, cfgDeployPath);
            switch (appType)
            {
                case BINARY_FILE:
                case JAR:
                case NODEJS:
                    execParam = String.format("%s;mkdir %s -p", execParam, absolutePath);
                    break;
                case RESIN_WEB_APP:
                case TOMCAT_WEB_APP:
                    execParam = String.format("%s;mkdir %s -p", execParam, absolutePath.replaceAll(String.format("/%s/", theName), "/"));
                    break;
            }
            execParam = String.format("%s;mkdir %s -p", execParam, absolutePath);
            for (AppFileNexusInfo cfg : deployPathCfgMap.get(cfgDeployPath)) {
                switch (appType)
                {
                    case BINARY_FILE:
                    case JAR:
                        execParam = String.format("%s;cp %s/%s %s/%s", execParam, mountPath, cfg.getFileName(), absolutePath, cfg.getFileName());
                        break;
                    case RESIN_WEB_APP:
                    case TOMCAT_WEB_APP:
                        absolutePath = absolutePath.replaceAll(String.format("/%s/", theName), "/");
                        execParam = String.format("%s;cp %s/%s %s/%s", execParam, mountPath, cfg.getFileName(), absolutePath, cfg.getFileName());
                        absolutePath = absolutePath.replaceAll(String.format("^%s/", deployPath), "");
                        execParam = String.format("%s;jar uf %s %s/%s", execParam, packageFileName, absolutePath, cfg.getFileName());
                        break;
                    default:
                        break;
                }
//                execParam = String.format("%s;cp %s/%s %s/%s", execParam, mountPath, cfg.getFileName(), absolutePath, cfg.getFileName());
//                if (appType.equals(AppType.RESIN_WEB_APP) || appType.equals(AppType.TOMCAT_WEB_APP))
//                    execParam = String.format("%s;jar uf %s %s/%s", execParam, module.getInstallPackage().getFileName(), absolutePath.replaceAll(String.format("^%s", deployPath), "").replaceAll("^/", "").replaceAll(String.format("^%s/", theName), ""), cfg.getFileName());
            }
        }
        switch (appType)
        {
            case TOMCAT_WEB_APP:
            case RESIN_WEB_APP:
                execParam = String.format("%s;mv /%s/%s /war/%s-%s.war", execParam, deployPath, packageFileName, alias, domainId);
        }
        if(StringUtils.isNotBlank(appBase.getEnvLoadCmd()))
            execParam = String.format("%s;cd %s;%s", execParam, basePath, appBase.getEnvLoadCmd());
        commands.add(execParam.replaceAll("^;", "").replaceAll(";;", ";").replaceAll("//", "/"));
        logger.debug(String.format("command of init container is %s", String.join(";", commands)));
        return commands;
    }

    private List<V1ContainerPort> generateContainerPortsForRuntimeContainer(String portStr, AppType appType) throws ParamException
    {
        List<V1ContainerPort> containerPorts = new ArrayList<>();
        List<PortVo> portList = parsePort(portStr, ServicePortType.ClusterIP, appType);
        for(PortVo portVo : portList)
        {
            V1ContainerPort containerPort = new V1ContainerPort();
            containerPort.setProtocol(portVo.getProtocol());
            containerPort.setContainerPort(portVo.getTargetPort());
            containerPorts.add(containerPort);
        }
        return containerPorts;
    }

    private void generateProbeForRuntimeContainer(
            V1Container runtimeContainer, String alias, String domainId, AppType appType, String portStr,
            String checkAt, int initialDelaySeconds, int periodSeconds) throws ParamException
    {
        V1HTTPGetAction get = null;
        V1TCPSocketAction tcp = null;
        V1ExecAction exec = null;
        List<PortVo> portList = parsePort(portStr, ServicePortType.ClusterIP, appType);
        if(StringUtils.isNotBlank(checkAt))
        {
            if(!checkAt.matches(this.healthCheckRegex))
                throw new ParamException(String.format("%s is not legal health check word", checkAt));
            String checkType = checkAt.replaceAll("^.+/", "");
            String check = checkAt.replaceAll(String.format("/%s$", checkType), "");
            String port = check.replaceAll("\\:.+$", "");
            if(checkType.equals("HTTP") || checkType.equals("HTTPS"))
            {
                get = new V1HTTPGetAction();
                get.setPort(new IntOrString(Integer.parseInt(port)));
                String subPath = check.replaceAll(String.format("^%s\\:?", port), "");
                String path = String.format("/%s-%s%s", alias, domainId, subPath).replaceAll("/$", "");
                get.setPath(path);
                get.setScheme(checkType);
            }
            else if(checkType.equals("TCP"))
            {
                tcp = new V1TCPSocketAction();
                tcp.setPort(new IntOrString(Integer.parseInt(port)));
            }
            else
            {
                exec = new V1ExecAction();
                exec.setCommand(Arrays.asList(new String[]{"/bin/sh", "-c", check}));
            }
        }
        else
        {
            int targetPort = portList.get(0).getTargetPort();
            switch (appType) {
                case BINARY_FILE:
                case NODEJS:
                    logger.debug(String.format("monitor port is %d/TCP", targetPort));
                    tcp = new V1TCPSocketAction();
                    tcp.setPort(new IntOrString(targetPort));
                    break;
                case TOMCAT_WEB_APP:
                case RESIN_WEB_APP:
                case JAR:
                    logger.debug(String.format("checked port is %d/HTTPGet", targetPort));
                    get = new V1HTTPGetAction();
                    get.setPort(new IntOrString(targetPort));
                    get.setPath(String.format("/%s-%s", alias, domainId));
                    get.setScheme("HTTP");
                    break;
                default:
                    throw new ParamException(String.format("can not handle probe for appType=%s", appType.name));
            }
        }
        runtimeContainer.getLivenessProbe().setTcpSocket(tcp);
        runtimeContainer.getLivenessProbe().setHttpGet(get);
        runtimeContainer.getLivenessProbe().setExec(exec);
        runtimeContainer.getReadinessProbe().setTcpSocket(tcp);
        runtimeContainer.getReadinessProbe().setHttpGet(get);
        runtimeContainer.getReadinessProbe().setExec(exec);
        if(initialDelaySeconds > 0){
            runtimeContainer.getLivenessProbe().setInitialDelaySeconds(initialDelaySeconds);
            runtimeContainer.getReadinessProbe().setInitialDelaySeconds(initialDelaySeconds);
        }
        if(periodSeconds > 0){
            runtimeContainer.getLivenessProbe().setPeriodSeconds(periodSeconds);
            runtimeContainer.getReadinessProbe().setPeriodSeconds(periodSeconds);
        }
        runtimeContainer.getReadinessProbe().setTcpSocket(tcp);
        runtimeContainer.getReadinessProbe().setHttpGet(get);
        runtimeContainer.getReadinessProbe().setExec(exec);
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

    @Override
    public List<V1Deployment> generateThreeAppDeployment(CCODThreePartAppPo threePartAppPo, PlatformPo platform, boolean isBase) throws ParamException {
        String appName = threePartAppPo.getAppName();
        String version = threePartAppPo.getVersion();
        String alias = threePartAppPo.getAlias();
        Map<String, String> k8sMacroData = threePartAppPo.getK8sMacroData(platform);
        Object selectObject = selectK8sObjectForApp(K8sKind.DEPLOYMENT, threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP, threePartAppPo.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find deployment template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP.name, platform.getCcodVersion(), threePartAppPo.getTag()));
        }
        List<V1Deployment> deploys = (List<V1Deployment>)selectObject;
        for(V1Deployment deploy : deploys){
//            deploy.getMetadata().setLabels(new HashMap<>());
            deploy.getMetadata().getLabels().put(appName, alias);
            if(StringUtils.isNotBlank(version)){
                deploy.getMetadata().getLabels().put(this.appVersionLabel, version);
            }
//            deploy.getSpec().getSelector().setMatchLabels(new HashMap<>());
//            deploy.getSpec().getSelector().getMatchLabels().put(appName, alias);
//            deploy.getSpec().getTemplate().getMetadata().setLabels(new HashMap<>());
//            deploy.getSpec().getTemplate().getMetadata().getLabels().put(appName, alias);
//            List<V1Volume> volumes = deploy.getSpec().getTemplate().getSpec().getVolumes().stream()
//                    .collect(Collectors.groupingBy(V1Volume::getName)).get(threePartAppPo.getVolume());
////            if(volumes == null){
////                throw new ParamException(String.format("not find %s volume from %s yaml", threePartAppPo.getVolume(), threePartAppPo.getAppName()));
////            }
////            else if(volumes.size() > 1){
////                throw new ParamException(String.format("%s volume from %s yaml multi defined", threePartAppPo.getVolume(), threePartAppPo.getAppName()));
////            }
//            if(volumes != null && volumes.size() > 0){
//                V1Volume volume = volumes.get(0);
//                if(volume.getHostPath() != null){
//                    volume.getHostPath().setPath(String.format("/home/kubernetes/volume/%s/%s", platform.getPlatformId(), threePartAppPo.getMountSubPath()).replaceAll("//", "/"));
//                }
//                else if(volume.getPersistentVolumeClaim() != null){
//                    volume.getPersistentVolumeClaim().setClaimName(String.format("base-volume-%s", platform.getPlatformId()));
//                }
//            }
        }
//        if(appName.equals("oracle")){
//            deploys.get(0).getSpec().getTemplate().getSpec().getContainers().get(0).getArgs().set(0, String.format("/tmp/init.sh %s", platform.getHostUrl()));
//        }
        logger.info(String.format("selected deployments %s", gson.toJson(deploys)));
        return deploys;
    }

    @Override
    public V1Namespace generateNamespace(String ccodVersion, String platformId, String platformTag) throws ParamException {
        logger.debug(String.format("begin to generate namespace for %s with ccodVersion=%s and platformTag=%s", platformId, ccodVersion, platformTag));
        Map<String, String> k8sMacroData = new HashMap<>();
        k8sMacroData.put(K8sObjectTemplatePo.PLATFORM_ID, platformId);
        Object selectObject = selectK8sObjectForPlatform(K8sKind.NAMESPACE, platformTag, ccodVersion, k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not select namespace template for ccodVersion=%s and platformTag=%s", ccodVersion, platformTag));
        }
        V1Namespace ns = (V1Namespace)selectObject;
//        ns.getMetadata().setNamespace(platformId);
//        ns.getMetadata().setName(platformId);
//        Map<String, String> labels = new HashMap<>();
//        labels.put(this.platformIdLabel, platformId);
//        labels.put(this.ccodVersionLabel, ccodVersion);
//        ns.getMetadata().setLabels(labels);
        logger.info(String.format("selected namespace is %s", gson.toJson(ns)));
        return ns;
    }

    /**
     * 为指定平台生成缺省secret
     * @param platform 平台信息
     * @param platformTag 平台标签
     * @return 生成的secret
     * @throws ParamException
     */
    private List<V1Secret> generateSecret(PlatformPo platform, String platformTag) throws ParamException, ApiException{
        String ccodVersion = platform.getCcodVersion();
        logger.debug(String.format("begin to generate secret for ccodVersion=%s with tag=%s", ccodVersion, platformTag));
        Object selectObject = selectK8sObjectForPlatform(K8sKind.SECRET, platformTag, ccodVersion, platform.getK8sMacroData());
        if(selectObject == null){
            throw new ParamException(String.format("can not select pv template for ccodVersion=%s and platformTag=%s", platform.getCcodVersion(), platform.getTag()));
        }
        List<V1Secret> secrets = (List<V1Secret>)selectObject;
        logger.debug(String.format("generate secret is %s", gson.toJson(secrets)));
        return secrets;
    }

    /**
     * 为指定平台生成缺省pv
     * @param platform 平台信息
     * @param platformTag 平台标签
     * @return 生成的pv
     * @throws ParamException
     */
    private List<V1ConfigMap> generateConfigMap(PlatformPo platform, String platformTag) throws ParamException {
        String ccodVersion = platform.getCcodVersion();
        logger.debug(String.format("begin to generate configMap for ccodVersion=%s with tag=%s", ccodVersion, platformTag));
        Object selectObject = selectK8sObjectForPlatform(K8sKind.CONFIGMAP, platformTag, ccodVersion, platform.getK8sMacroData());
        if(selectObject == null){
            selectObject = new ArrayList<>();
        }
        List<V1ConfigMap> configs = (List<V1ConfigMap>)selectObject;
        logger.debug(String.format("generate pv is %s", gson.toJson(configs)));
        return configs;
    }

    /**
     * 为指定平台生成缺省pv
     * @param platform 平台信息
     * @param platformTag 平台标签
     * @return 生成的pv
     * @throws ParamException
     */
    private List<V1PersistentVolume> generatePersistentVolume(PlatformPo platform, String platformTag) throws ParamException {
        String ccodVersion = platform.getCcodVersion();
        logger.debug(String.format("begin to generate pv for ccodVersion=%s with tag=%s", ccodVersion, platformTag));
        Object selectObject = selectK8sObjectForPlatform(K8sKind.PV, platformTag, ccodVersion, platform.getK8sMacroData());
        if(selectObject == null){
            throw new ParamException(String.format("can not select pv template for ccodVersion=%s and platformTag=%s", platform.getCcodVersion(), platform.getTag()));
        }
        List<V1PersistentVolume> pvList = (List<V1PersistentVolume>)selectObject;
        logger.debug(String.format("generate pv is %s", gson.toJson(pvList)));
        return pvList;
    }

    /**
     * 生成指定的平台pvc
     * @param platform 平台信息
     * @param platformTag 平台标签
     * @return 生成的pvc
     * @throws ParamException
     */
    private List<V1PersistentVolumeClaim> generatePersistentVolumeClaim(PlatformPo platform, String platformTag) throws ParamException {
        String ccodVersion = platform.getCcodVersion();
        logger.debug(String.format("begin to generate pvc for ccodVersion=%s with tag=%s", ccodVersion, platformTag));
        Object selectObject = selectK8sObjectForPlatform(K8sKind.PVC, platformTag, ccodVersion, platform.getK8sMacroData());
        if(selectObject == null){
            throw new ParamException(String.format("can not select pv template for ccodVersion=%s and platformTag=%s", platform.getCcodVersion(), platform.getTag()));
        }
        List<V1PersistentVolumeClaim> pvcList = (List<V1PersistentVolumeClaim>)selectObject;
        logger.debug(String.format("generate pv is %s", gson.toJson(pvcList)));
        return pvcList;
    }

    private Object selectK8sObjectForPlatform(K8sKind kind, String platformTag, String ccodVersion, Map<String, String> k8sMacroData){
        logger.debug(String.format("begin to select platform k8s object %s with platformTag=%s and ccodVersion=%s and k8sMacroData=%s",
                kind.name, platformTag, ccodVersion, gson.toJson(k8sMacroData)));
        List<K8sObjectTemplatePo> templateList = objectTemplateList.stream().filter(t->isMatchForPlatform(t, platformTag, ccodVersion))
                .collect(Collectors.toList());
        for(K8sObjectTemplatePo template : templateList){
            if(template.getK8sObject(kind) != null){
                logger.debug(String.format("template %s with platformTag=%s and ccodVersion=%s been selected : %s", kind.name, platformTag, ccodVersion, gson.toJson(template.getK8sObject(kind))));;
                return template.toMacroReplace(kind, k8sMacroData);
            }
        }
        logger.warn(String.format("can not select platform k8s object %s with platformTag=%s and ccodVersion=%s", kind.name, platformTag, ccodVersion));
        return null;
    }

    private Object selectK8sObjectForApp(K8sKind kind, String appName, String version, AppType appType, String appTag, String ccodVersion, Map<String, String> k8sMacroData){
        logger.info(String.format("begin to select %s template for ccodVersion=%s,appType=%s,appName=%s and version=%s and appTag=%s, k8sMacroData=%s",
                kind.name, ccodVersion, appType.name, appName, version, appTag, gson.toJson(k8sMacroData)));
        boolean isDomainApp = appType.equals(AppType.THREE_PART_APP) || appType.equals(AppType.OTHER) ? false : true;
        List<K8sObjectTemplatePo> templateList = objectTemplateList.stream().filter(t->isMatchForApp(t, appType, appTag, ccodVersion))
                .collect(Collectors.toList());
        for(K8sObjectTemplatePo template : templateList){
            if (template.getK8sObject(kind) != null && isAppVersionSelected(template, appName, version, appType, appTag, ccodVersion)) {
                logger.debug(String.format("%s template for %s with version matched : %s", kind.name, appName, gson.toJson(template.getK8sObject(kind))));
                return template.toMacroReplace(kind, k8sMacroData);
            }
        }
        for(K8sObjectTemplatePo template : templateList){
            if(template.getK8sObject(kind) != null && isAppNameSelected(template, appName, appType, appTag, ccodVersion)){
                logger.debug(String.format("%s template for %s with appName matched : %s", kind.name, appName, gson.toJson(template.getK8sObject(kind))));
                return template.toMacroReplace(kind, k8sMacroData);
            }
        }
        if(isDomainApp){
            for(K8sObjectTemplatePo template : templateList){
                if(template.getK8sObject(kind) != null && isAppTypeSelected(template, appType, appTag, ccodVersion)){
                    logger.debug(String.format("%s template for %s with appType matched : %s", kind.name, appName, gson.toJson(template.getK8sObject(kind))));
                    return template.toMacroReplace(kind, k8sMacroData);
                }
            }
        }
        for(K8sObjectTemplatePo template : templateList){
            if(template.getK8sObject(kind) != null && isAppTagSelected(template, appTag, ccodVersion)){
                logger.debug(String.format("%s template for %s with appTag matched : %s", kind.name, appName, gson.toJson(template.getK8sObject(kind))));
                return template.toMacroReplace(kind, k8sMacroData);
            }
        }
        logger.warn(String.format("can not select %s template for ccodVersion=%s,appType=%s,appName=%s and version=%s and appTag=%s",
                kind.name, ccodVersion, appType.name, appName, version, appTag));
        return null;
    }


    private boolean isMatchForPlatform(K8sObjectTemplatePo template, String platformTag, String ccodVersion){
        if(!isCcodVersionMatch(template, ccodVersion)){
            return false;
        }
        if(!isTagMatch(template.getLabels().get(platformTagLabel), platformTag)){
            return false;
        }
        for(String key : template.getLabels().keySet()){
            if(!key.equals(ccodVersionLabel) && !key.equals(platformTagLabel)){
                return false;
            }
        }
        return true;
    }

    private boolean isMatchForApp(K8sObjectTemplatePo template, AppType appType, String appTag, String ccodVersion){
        if(!isCcodVersionMatch(template, ccodVersion)){
            return false;
        }
        if(!template.getLabels().containsKey(appTypeLabel) || !template.getLabels().get(appTypeLabel).equals(appType.name))
            return false;
        if(!isTagMatch(template.getLabels().get(appTagLabel), appTag))
            return false;
        return true;
    }

    private boolean isCcodVersionMatch(K8sObjectTemplatePo template, String ccodVersion){
        if(!template.getLabels().containsKey(ccodVersionLabel) || !template.getLabels().get(ccodVersionLabel).equals(ccodVersion)){
            return false;
        }
        return true;
    }

    private boolean isAppTypeMatch(K8sObjectTemplatePo template, String ccodVersion, AppType appType){
        if(!isCcodVersionMatch(template, ccodVersion)){
            return false;
        }
        if(!template.getLabels().containsKey(appTypeLabel) || !template.getLabels().get(appTypeLabel).equals(appType.name)){
            return false;
        }
        return true;
    }

    private boolean isAppNameMatch(K8sObjectTemplatePo template, String ccodVersion, AppType appType, String appName){
        if(!isAppTypeMatch(template, ccodVersion, appType)){
            return false;
        }
        if(!template.getLabels().containsKey(appName)){
            return false;
        }
        return true;
    }

    private boolean isAppVersionMatch(K8sObjectTemplatePo template, String ccodVersion, AppType appType, String appName, String version){
        if(!isAppNameMatch(template, ccodVersion, appType, appName)){
            return false;
        }
        return isVersionMatch(version, template.getLabels().get(appName));
    }

    private boolean isVersionMatch(String version, String supportedVersion){
        List<String> versions = Arrays.asList(supportedVersion.split(","));
        if(versions.contains(version)){
            return true;
        }

        return false;
    }

    private boolean isAppTypeSelected(K8sObjectTemplatePo template, AppType appType, String appTag, String ccodVersion){
        if(!isTagMatch(template.getLabels().get(appTagLabel), appTag)){
            return false;
        }
        for(String key : template.getLabels().keySet()){
            if(!key.equals(ccodVersionLabel) && !key.equals(appTypeLabel) && !key.equals(appTagLabel)){
                return false;
            }
        }
        return isAppTypeMatch(template, ccodVersion, appType);
    }

    private boolean isAppTagSelected(K8sObjectTemplatePo template, String appTag, String ccodVersion){
        if(!isCcodVersionMatch(template, ccodVersion)){
            return false;
        }
        if(StringUtils.isBlank(appTag) || !isTagMatch(template.getLabels().get(appTagLabel), appTag)){
            return false;
        }
        for(String key : template.getLabels().keySet()){
            if(!key.equals(ccodVersionLabel) && !key.equals(appTagLabel)){
                return false;
            }
        }
        return true;
    }

    private boolean isAppNameSelected(K8sObjectTemplatePo template, String appName, AppType appType, String appTag, String ccodVersion){
        if(!isTagMatch(template.getLabels().get(appTagLabel), appTag)){
            return false;
        }
        return isAppNameMatch(template, ccodVersion, appType, appName) && (template.getLabels().get(appName).equals(forAllVersion) || template.getLabels().get(appName).equals("*"));
    }

    private boolean isAppVersionSelected(K8sObjectTemplatePo template, String appName, String version, AppType appType, String appTag, String ccodVersion){
        if(!isTagMatch(template.getLabels().get(appTagLabel), appTag)){
            return false;
        }
        return isAppVersionMatch(template, ccodVersion, appType, appName, version);
    }

    private boolean isMatch(Map<String, String> selector, Map<String, String> labels)
    {
        if(selector == null || selector.size() == 0 || labels == null || labels.size() == 0)
            return  false;
        if(selector.size() > labels.size())
            return false;
        if (!selector.containsKey(ccodVersionLabel)){
            return false;
        }
        if(!isTagMatch(selector.get(platformTagLabel), labels.get(platformTagLabel))){
            return false;
        }
        if(!isTagMatch(selector.get(domainIdLabel), labels.get(domainIdLabel))){
            return false;
        }
        if(!isTagMatch(selector.get(appTagLabel), labels.get(appTagLabel))){
            return false;
        }
        for(String key : selector.keySet())
        {
            if(key.equals(platformTagLabel) || key.equals(domainTagLabel) || key.equals(appTagLabel)){
                continue;
            }
            if(!labels.containsKey(key))
                return false;
        }
        return true;
    }

    private boolean isTagMatch(String srcTag, String dstTag){
        if(StringUtils.isBlank(srcTag) && StringUtils.isBlank(dstTag)){
            return true;
        }
        else if(StringUtils.isNotBlank(srcTag) && StringUtils.isNotBlank(dstTag)) {
            Set<String> src = Arrays.stream(srcTag.split(",")).map(t->t.trim()).collect(Collectors.toSet());
            Set<String> dst = Arrays.stream(dstTag.split(",")).map(t->t.trim()).collect(Collectors.toSet());
            if(src.size() != dst.size()){
                return false;
            }
            for(String tag : src){
                if(!dst.contains(tag)){
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean isEqual(Map<String, String> selector, Map<String, String> labels)
    {
        if(selector == null || selector.size() == 0 || labels == null || labels.size() == 0)
            return  false;
        if(selector.size() != labels.size())
            return false;
        for(String key : selector.keySet())
        {
            if(!labels.containsKey(key) || !labels.get(key).equals(selector.get(key)))
                return false;
        }
        return true;
    }

    private List<K8sObjectTemplatePo> parseTemplateFromFile(String savePath) throws IOException
    {
        List<K8sObjectTemplatePo> list = new ArrayList<>();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(savePath)),
                "UTF-8"));
        String lineTxt = br.readLine();
        JsonParser jp = new JsonParser();
        JsonArray je = jp.parse(lineTxt).getAsJsonArray();
        for(JsonElement e : je){
            K8sObjectTemplatePo po = new K8sObjectTemplatePo();
            JsonObject jo = e.getAsJsonObject();
            JsonObject labels = jo.get("labels").getAsJsonObject();
            po.setLabels(new HashMap<>());
            for(String key : labels.keySet()){
                po.getLabels().put(key, labels.get(key).getAsString());
            }
//            po.setLabels(gson.fromJson(jo.get("labels").getAsString(), HashMap.class));
            if(jo.has("deployJson")){
                po.setDeployments(gson.fromJson(jo.get("deployJson").getAsString(), new TypeToken<List<V1Deployment>>() {
                }.getType()));
            }
            if(jo.has("serviceJson")){
                po.setServices(gson.fromJson(jo.get("serviceJson").getAsString(), new TypeToken<List<V1Service>>() {
                }.getType()));
            }
//            if(jo.has("ingressJson")){
//                po.setIngress(gson.fromJson(jo.get("ingressJson").getAsString(), ExtensionsV1beta1Ingress.class));
//            }
            if(jo.has("endpointsJson")){
                po.setEndpoints(gson.fromJson(jo.get("endpointsJson").getAsString(), new TypeToken<List<V1Endpoints>>() {
                }.getType()));
            }
            if(jo.has("podJson")){
                po.setPods(gson.fromJson(jo.get("podJson").getAsString(), new TypeToken<List<V1Pod>>() {
                }.getType()));
            }
            if(jo.has("namespaceJson")){
                po.setNamespaces(gson.fromJson(jo.get("namespaceJson").getAsString(), V1Namespace.class));
            }
            if(jo.has("jobJson")){
                po.setJobs(Arrays.asList(gson.fromJson(jo.get("jobJson").getAsString(), V1Job.class)));
            }
            if(jo.has("secretJson")){
                po.setSecrets(Arrays.asList(gson.fromJson(jo.get("secretJson").getAsString(), V1Secret.class)));
            }
            if(jo.has("persistentVolumeJson")){
                po.setPvList(Arrays.asList(gson.fromJson(jo.get("persistentVolumeJson").getAsString(), V1PersistentVolume.class)));
            }
            if(jo.has("persistentVolumeClaimJson")){
                po.setPvcList(Arrays.asList(gson.fromJson(jo.get("persistentVolumeClaimJson").getAsString(), V1PersistentVolumeClaim.class)));
            }
            list.add(po);
        }
        return list;
    }

    private List<K8sThreePartServiceVo> parseTestThreePartServiceFromFile(String savePath) throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(savePath)),
                "UTF-8"));
        String lineTxt = br.readLine();
        List<K8sThreePartServiceVo> list = this.gson.fromJson(lineTxt, new TypeToken<List<K8sThreePartServiceVo>>() {
        }.getType());
        return list;
    }

    @Override
    public List<V1Job> generatePlatformInitJob(PlatformPo platform, boolean isBase) throws ParamException {
        String platformId = isBase ? String.format("base-%s", platform.getPlatformId()) : platform.getPlatformId();
        String ccodVersion = platform.getCcodVersion();
        String baseDataNexusPath = (String)platform.getParams().get(PlatformBase.baseDataNexusPathKey);
        String platformBaseDataRepository = (String)platform.getParams().get(PlatformBase.baseDataNexusRepositoryKey);
        Object selectObject = selectK8sObjectForPlatform(K8sKind.JOB, platform.getTag(), ccodVersion, platform.getK8sMacroData());
        if(selectObject == null){
            throw new ParamException(String.format("can not select pv template for ccodVersion=%s and platformTag=%s", platform.getCcodVersion(), platform.getTag()));
        }
        List<V1Job> jobs = (List<V1Job>) selectObject;
        for(V1Job job : jobs){
            String fileName = baseDataNexusPath.replaceAll("^.*/", "");
            job.getMetadata().setNamespace(platformId);
            String workDir = String.format("/root/data/base-volume");
            String arg = String.format("mkdir %s -p;cd %s;wget %s/repository/%s/%s;tar -xvzf %s",
                    workDir, workDir, nexusHostUrl, platformBaseDataRepository, baseDataNexusPath, fileName);
            job.getSpec().getTemplate().getSpec().getContainers().get(0).setArgs(Arrays.asList(arg));
            job.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity()))
                    .get("data").getPersistentVolumeClaim().setClaimName(String.format("base-volume-%s", platformId));
        }
        return jobs;
    }

    @Override
    public K8sCCODDomainAppVo getCCODDomainApp(String appName, String alias, String version, String domainId, String platformId, String k8sApiUrl, String k8sAuthToken) throws ParamException, ApiException {
        logger.debug(String.format("get %s[%s(%s)] at %s deploy detail from k8s", alias, appName, version, domainId));
        List<AppModuleVo> modules = this.appManagerService.queryAllRegisterAppModule(true).stream()
                .filter(app->app.getAppName().equals(appName) && app.getVersion().equals(version)).collect(Collectors.toList());
        if(modules.size() == 0)
            throw new ParamException(String.format("%s(%s) has not register or not image", appName, version));
        AppModuleVo module = modules.get(0);
        AppType appType = module.getAppType();
        String name = String.format("%s-%s", alias, domainId);
        V1ConfigMap configMap = this.ik8sApiService.readNamespacedConfigMap(name, platformId, k8sApiUrl, k8sAuthToken);
        Map<String, String> selector = getCCODDomainAppSelector(appName, alias, version, appType, domainId, K8sKind.DEPLOYMENT);
        List<V1Deployment> deploys = this.ik8sApiService.selectNamespacedDeployment(platformId, selector, k8sApiUrl, k8sAuthToken);
        if(deploys.size() == 0)
            throw new ParamException(String.format("not select deployment for %s at %s from %s", gson.toJson(selector), platformId, k8sApiUrl));
        else if(deploys.size() > 1)
            throw new ParamException(String.format("select %d deployment for %s at %s from %s", deploys.size(), gson.toJson(selector), platformId, k8sApiUrl));
        selector = getCCODDomainAppSelector(appName, alias, version, appType, domainId, K8sKind.SERVICE);
        List<V1Service> services = this.ik8sApiService.selectNamespacedService(platformId, selector, k8sApiUrl, k8sAuthToken);
        if(services.size() == 0)
            throw new ParamException(String.format("not select service for %s at %s", gson.toJson(selector), platformId));
        List<ExtensionsV1beta1Ingress> ingress = new ArrayList<>();
        selector = getCCODDomainAppSelector(appName, alias, version, appType, domainId, K8sKind.INGRESS);
        if(appType.equals(AppType.RESIN_WEB_APP) || appType.equals(AppType.TOMCAT_WEB_APP))
            ingress = this.ik8sApiService.selectNamespacedIngress(platformId, selector, k8sApiUrl, k8sAuthToken);
        K8sCCODDomainAppVo appVo = new K8sCCODDomainAppVo(alias, module, domainId, configMap, deploys.get(0), services, ingress);
        return appVo;
    }

    @Override
    public K8sCCODDomainAppVo generateNewCCODDomainApp(AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException, InterfaceCallException, IOException{
        String appName = appBase.getAppName();
        String version = appBase.getVersion();
        String alias = appBase.getAlias();
        String tag = String.format("%s[%s(%s)]", alias, appName, version);
        String domainId = domain.getDomainId();
        logger.debug(String.format("generate k8s object for %s, domainId=%s", gson.toJson(appBase), domainId));
        AppModuleVo module = this.appManagerService.queryAppByVersion(appName, version, true);
        appBase.setInstallPackage(module.getInstallPackage());
        String ccodVersion = module.getCcodVersion();
        AppType appType = module.getAppType();
        String platformId = platform.getPlatformId();
        logger.debug(String.format("generate configMap for %s : cfg=%s", tag, gson.toJson(module.getCfgs())));
        V1ConfigMap configMap = this.ik8sApiService.getConfigMapFromNexus(platformId, String.format("%s-%s", alias, domainId),
                appName, appBase.getCfgs().stream().map(cfg->cfg.getNexusAssetInfo(nexusHostUrl)).collect(Collectors.toList()),
                this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
        V1Deployment deploy = this.generateCCODDomainAppDeployment(appBase, domain, platform);
        List<V1Service> services = new ArrayList<>();
        V1Service service = this.generateCCODDomainAppService(appBase, ServicePortType.ClusterIP, appBase.getPorts(), domain, platform);
        services.add(service);
        if(StringUtils.isNotBlank(appBase.getNodePorts())) {
            service = this.generateCCODDomainAppService(appBase, ServicePortType.NodePort, appBase.getNodePorts(), domain, platform);
            Map<Integer, V1ServicePort> portMap = services.stream().flatMap(s->s.getSpec().getPorts().stream())
                    .collect(Collectors.toMap(V1ServicePort::getPort, Function.identity()));
            for(V1ServicePort nodePort : service.getSpec().getPorts()){
                V1ServicePort clusterIpPort = portMap.get(nodePort.getPort());
                if(clusterIpPort == null){
                    throw new ParamException(String.format("nodePort %d not define in ports %s", nodePort.getPort(), appBase.getPorts()));
                }
                nodePort.setTargetPort(clusterIpPort.getTargetPort());
            }
            services.add(service);
        }
        List<ExtensionsV1beta1Ingress> ingress = null;
        if(appType.equals(AppType.TOMCAT_WEB_APP) || appType.equals(AppType.RESIN_WEB_APP) || appType.equals(AppType.JAR) || appType.equals(AppType.NODEJS))
            ingress = this.generateIngress(appBase, domain, platform);
        K8sCCODDomainAppVo app = new K8sCCODDomainAppVo(alias, module, domainId, configMap, deploy, services, ingress);
        return app;
    }

    @Override
    public List<K8sOperationInfo> getDeletePlatformAppSteps(String jobId, String appName, String alias, String version, String domainId, String platformId, String k8sApiUrl, String k8sAuthToken) throws ApiException, ParamException {
        logger.debug(String.format("generate delete %s[%s(%s)] at %s from k8s steps", alias, appName, version, domainId));
        K8sCCODDomainAppVo app = getCCODDomainApp(appName, alias, version, domainId, platformId, k8sApiUrl, k8sAuthToken);
        String name = String.format("%s-%s", alias, domainId);
        List<K8sOperationInfo> steps = new ArrayList<>();
        K8sOperationInfo step;
        if(app.getIngresses() != null && app.getIngresses().size() > 0)
        {
            for(ExtensionsV1beta1Ingress ingress : app.getIngresses()){
                step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.INGRESS, name, K8sOperation.DELETE, ingress);
                steps.add(step);
            }
        }
        for(V1Service service : app.getServices())
        {
            step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE, name, K8sOperation.DELETE, service);
            steps.add(step);
        }
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.DEPLOYMENT, name, K8sOperation.DELETE, app.getDeploy());
        steps.add(step);
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP, name, K8sOperation.DELETE, app.getConfigMap());
        steps.add(step);
        logger.debug(String.format("delete %s at domain %s steps are %s", alias, domainId, gson.toJson(steps)));
        return steps;
    }

    private List<K8sAppCollection> generateK8sAppCollections(List<AppUpdateOperationInfo> domainOpts, DomainPo domain, PlatformPo platform) throws ParamException, IOException, InterfaceCallException{
        String domainId = domain.getDomainId();
        String platformId = platform.getPlatformId();
        List<K8sAppCollection> list = new ArrayList<>();
        Map<String, AppUpdateOperationInfo> aliasMap = domainOpts.stream().collect(Collectors.toMap(AppUpdateOperationInfo::getAlias, Function.identity()));
        Map<String, V1Deployment> handleMap = new HashMap<>();
        for(AppUpdateOperationInfo opt : domainOpts){
            if(handleMap.containsKey(opt.getAlias())){
                continue;
            }
            AppModuleVo module = appManagerService.queryAppByVersion(opt.getAppName(), opt.getVersion(), true);
            opt.fill(module);
            Object selectObject = selectK8sObjectForApp(K8sKind.DEPLOYMENT, opt.getAppName(), opt.getVersion(), opt.getAppType(), opt.getTag(), platform.getCcodVersion(), opt.getK8sMacroData(domain, platform));
            if(selectObject == null){
                throw new ParamException(String.format("can not select deployment template for %s at %s", opt.getAlias(), domain.getDomainId()));
            }
            List<V1Deployment> deployments = (List<V1Deployment>) selectObject;
            if(deployments.size() == 0){
                throw new ParamException(String.format("selected deployment template for %s at %s is empty", opt.getAlias(), domain.getDomainId()));
            }
            else if(deployments.size() > 1){
                throw new ParamException(String.format("selected deployment template for %s at %s has %d deployment definition",
                        opt.getAlias(), domain.getDomainId(), deployments.size()));
            }
            V1Deployment deployment = deployments.get(0);
            selectObject = selectK8sObjectForApp(K8sKind.SERVICE, opt.getAppName(), opt.getVersion(), opt.getAppType(), opt.getTag(), platform.getCcodVersion(), opt.getK8sMacroData(domain, platform));
            if(selectObject == null){
                throw new ParamException(String.format("can not select service template for %s at %s", opt.getAlias(), domain.getDomainId()));
            }
            List<V1Service> services = (List<V1Service>) selectObject;
            selectObject = selectK8sObjectForApp(K8sKind.INGRESS, opt.getAppName(), opt.getVersion(), opt.getAppType(), opt.getTag(), platform.getCcodVersion(), opt.getK8sMacroData(domain, platform));
            if(selectObject == null){
                throw new ParamException(String.format("can not select ingress template for %s at %s", opt.getAlias(), domain.getDomainId()));
            }
            List<ExtensionsV1beta1Ingress> ingresses = (List<ExtensionsV1beta1Ingress>)selectObject;
            selectObject = selectK8sObjectForApp(K8sKind.CONFIGMAP, opt.getAppName(), opt.getVersion(), opt.getAppType(), opt.getTag(), platform.getCcodVersion(), opt.getK8sMacroData(domain, platform));
            if(selectObject == null){
                throw new ParamException(String.format("can not select configMap template for %s at %s", opt.getAlias(), domain.getDomainId()));
            }
            List<V1ConfigMap> configMaps = (List<V1ConfigMap>)selectObject;
            List<AppUpdateOperationInfo> optList = new ArrayList<>();
            List<V1ConfigMap> appConfigs = new ArrayList<>();
            List<V1Service> appServices = new ArrayList<>();
            List<String> aliases = deployment.getSpec().getTemplate().getSpec().getContainers().stream().map(c->c.getName().replaceAll("\\-runtime$", "")).collect(Collectors.toList());
            for(String alias : aliases){
                if(!aliasMap.containsKey(alias)){
                    throw new ParamException(String.format("%s and %s have been deployed at the same pod, so %s is wanted at opt list",
                            opt.getAlias(), alias, alias));
                }
                AppUpdateOperationInfo dst = aliasMap.get(alias);
                if(!dst.getOperation().equals(opt.getOperation())){
                    throw new ParamException(String.format("%s and %s have been deployed at the same pod, so the operation of them must be same",
                            opt.getAlias(), alias));
                }
                if(!isTagMatch(opt.getTag(), dst.getTag())){
                    throw new ParamException(String.format("%s and %s have been deployed at the same pod, so the appTag of them must be same",
                            opt.getAlias(), alias));
                }
                if(configMaps.size() == 0){
                    V1ConfigMap configMap = this.ik8sApiService.getConfigMapFromNexus(platformId, String.format("%s-%s", dst.getAlias(), domainId), dst.getAppName(),
                            dst.getCfgs().stream().map(cfg->cfg.getNexusAssetInfo(nexusHostUrl)).collect(Collectors.toList()),
                            nexusHostUrl, nexusUserName, nexusPassword);
                    if(configMap.getMetadata().getLabels() == null){
                        configMap.getMetadata().setLabels(new HashMap<>());
                    }
                    configMap.getMetadata().getLabels().put(domainIdLabel, domainId);
                    configMap.getMetadata().getLabels().put(aliasMap.get(alias).getAppName(), alias);
                    appConfigs.add(configMap);
                }
                else{
                    configMaps = configMaps.stream().map(c->{
                        if(c.getMetadata().getLabels() == null){
                            c.getMetadata().setLabels(new HashMap<>());
                        }
                        c.getMetadata().getLabels().put(domainIdLabel, domainId);
                        c.getMetadata().getLabels().put(dst.getAppName(), dst.getAlias());
                        return c;
                    }).collect(Collectors.toList());
                }
                if(services.size() == 0){
                    V1Service service = this.generateCCODDomainAppService(dst, ServicePortType.ClusterIP, dst.getPorts(), domain, platform);
                    appServices.add(service);
                    if(StringUtils.isNotBlank(dst.getNodePorts())) {
                        service = this.generateCCODDomainAppService(dst, ServicePortType.NodePort, dst.getNodePorts(), domain, platform);
                        appServices.add(service);
                    }
                }
                else{
                    services = services.stream().map(s->{
                        if(s.getMetadata().getLabels() == null){
                            s.getMetadata().setLabels(new HashMap<>());
                        }
                        s.getMetadata().getLabels().put(domainIdLabel, domainId);
                        s.getMetadata().getLabels().put(dst.getAppName(), dst.getAlias());
                        return s;
                    }).collect(Collectors.toList());
                }
                handleMap.put(alias, deployments.get(0));
                optList.add(dst);
            }
            if(aliases.size() == 1 && !opt.getAppType().equals(AppType.DOCKER_RUN)){
                generateMountAndCmdForSingleDeployment(deployment, opt, domain, platform);
            }
            Integer timeout = opt.getTimeout() == null ? module.getTimeout() : opt.getTimeout();
            if(timeout == null)
                timeout = 0;
            K8sAppCollection collection = new K8sAppCollection(optList, domain, platform, deployment, services.size()==0 ? appServices:services, ingresses,
                    configMaps.size()==0 ? appConfigs:configMaps, timeout);
            list.add(collection);
        }
        return list;
    }

    private List<K8sOperationInfo> generateDeployStepFromCollection(String jobId, K8sAppCollection collection, DomainPo doamin, PlatformPo platform) throws ParamException, ApiException{
        List<K8sOperationInfo> steps = new ArrayList<>();
        K8sOperation operation;
        switch (collection.getOperation()){
            case ADD:
                operation = K8sOperation.CREATE;
                break;
            case DELETE:
                operation = K8sOperation.DELETE;
                break;
            case UPDATE:
                List<K8sOperationInfo> deletes = generateDeleteAppSteps(jobId, collection.getOptList().get(0), doamin, platform);
                steps.addAll(deletes);
                operation = K8sOperation.CREATE;
                break;
            default:
                throw new ParamException(String.format("unsupported operation %s for k8sAppCollection", collection.getOperation().name));
        }
        collection.getConfigMaps().forEach(c->{
            K8sOperationInfo step = new K8sOperationInfo(jobId, collection.getPlatform().getPlatformId(), collection.getDomain().getDomainId(),
                    K8sKind.CONFIGMAP, c.getMetadata().getName(), operation, c, collection.getTimeout());
            steps.add(step);
        });
        collection.getServices().forEach(s->{
            K8sOperationInfo step = new K8sOperationInfo(jobId, collection.getPlatform().getPlatformId(), collection.getDomain().getDomainId(),
                    K8sKind.SERVICE, s.getMetadata().getName(), operation, s, collection.getTimeout());
            steps.add(step);
        });
        collection.getIngresses().forEach(s->{
            K8sOperationInfo step = new K8sOperationInfo(jobId, collection.getPlatform().getPlatformId(), collection.getDomain().getDomainId(),
                    K8sKind.INGRESS, s.getMetadata().getName(), operation, s, collection.getTimeout());
            steps.add(step);
        });
        steps.add( new K8sOperationInfo(jobId, collection.getPlatform().getPlatformId(), collection.getDomain().getDomainId(),
                K8sKind.DEPLOYMENT, collection.getDeployment().getMetadata().getName(), operation, collection.getDeployment(), collection.getTimeout()));
        return steps;
    }

    @Override
    public List<K8sOperationInfo> generateAddPlatformAppSteps(String jobId, AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform, boolean isNewPlatform) throws ParamException, ApiException, InterfaceCallException, IOException {
        String domainId = domain.getDomainId();
        List<AppFileNexusInfo> domainCfg = domain.getCfgs() == null ? new ArrayList<>() : domain.getCfgs();
        logger.debug(String.format("generate step of add %s to %s", gson.toJson(appBase), domainId));
        String appName = appBase.getAppName();
        String alias = appBase.getAlias();
        String version = appBase.getVersion();
        String name = String.format("%s-%s", alias, domainId);
        String platformId = platform.getPlatformId();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        AppModuleVo module = this.appManagerService.queryAllRegisterAppModule(true).stream()
                .filter(app->app.getAppName().equals(appName)&&app.getVersion().equals(version))
                .collect(Collectors.toList()).get(0);
        appBase.setInstallPackage(module.getInstallPackage());
        AppType appType = appBase.getAppType() == null ? module.getAppType() : appBase.getAppType();
        if(!isNewPlatform)
        {
            if(this.ik8sApiService.isNamespacedConfigMapExist(name, platformId, k8sApiUrl, k8sAuthToken))
                throw new ParamException(String.format("configMap %s exist at %s", name, platformId));
            Map<String, String> selector = getCCODDomainAppSelector(appName, alias, version, appType, domainId, K8sKind.DEPLOYMENT);
            if(this.ik8sApiService.isNamespacedDeploymentExist(name, platformId, k8sApiUrl, k8sAuthToken))
                throw new ParamException(String.format("deployment %s exist at %s", name, platformId));
            List<V1Deployment> deploys = this.ik8sApiService.selectNamespacedDeployment(platformId, selector, k8sApiUrl, k8sAuthToken);
            if(deploys.size() > 0)
                throw new ParamException(String.format("deployment for selector %s exist at %s", gson.toJson(selector), platformId));
            if(this.ik8sApiService.isNamespacedServiceExist(name, platformId, k8sApiUrl, k8sAuthToken))
                throw new ParamException(String.format("service %s exist at %s", name, platformId));
            if(this.ik8sApiService.isNamespacedServiceExist(String.format("%s-out", name), platformId, k8sApiUrl, k8sAuthToken))
                throw new ParamException(String.format("service %s-out exist at %s", name, platformId));
            selector = getCCODDomainAppSelector(appName, alias, version, appType, domainId, K8sKind.SERVICE);
            List<V1Service> services = this.ik8sApiService.selectNamespacedService(platformId, selector, k8sApiUrl, k8sAuthToken);
            if(services.size() > 0)
                throw new ParamException(String.format("service for selector %s exist at %s", gson.toJson(selector), platformId));
            if(this.ik8sApiService.isNamespacedIngressExist(name, platformId, k8sApiUrl, k8sAuthToken))
                throw new ParamException(String.format("ingress %s exist at %s", name, platformId));
        }
        K8sCCODDomainAppVo app = generateNewCCODDomainApp(appBase, domain, platform);
        List<K8sOperationInfo> steps = new ArrayList<>();
        K8sOperationInfo step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP,
                app.getConfigMap().getMetadata().getName(), K8sOperation.CREATE, app.getConfigMap());
        steps.add(step);
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.DEPLOYMENT,
                app.getDeploy().getMetadata().getName(), K8sOperation.CREATE, app.getDeploy());
        if(module.isKernal() != null && module.isKernal()) {
            step.setKernal(true);
            step.setTimeout(module.getTimeout());
        }
        steps.add(step);
        for(V1Service service : app.getServices())
        {
            step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE,
                    service.getMetadata().getName(), K8sOperation.CREATE, service);
            steps.add(step);
        }
        if(app.getIngresses() != null && app.getIngresses().size() > 0){
            for(ExtensionsV1beta1Ingress ingress : app.getIngresses())
            {
                step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.INGRESS,
                        ingress.getMetadata().getName(), K8sOperation.CREATE, ingress);
                steps.add(step);
            }
        }

        return steps;
    }

//    @Override
//    public List<K8sOperationInfo> generateUpdatePlatformAppSteps(String jobId, AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException, ApiException, InterfaceCallException, IOException {
//        String domainId = domain.getDomainId();
//        String alias = appBase.getAlias();
//        String appName = appBase.getAppName();
//        String version = appBase.getVersion();
//        logger.debug(String.format("generate update step for %s at %s", alias, domainId));
//        String platformId = platform.getPlatformId();
//        String k8sApiUrl = platform.getK8sApiUrl();
//        String k8sAuthToken = platform.getK8sAuthToken();
//        K8sCCODDomainAppVo oriApp= getCCODDomainApp(appName, alias, version, domainId, platformId, k8sApiUrl, k8sAuthToken);
//        K8sCCODDomainAppVo updateApp = generateNewCCODDomainApp(appBase, domain, platform);
//        List<K8sOperationInfo> steps = new ArrayList<>();
//        K8sOperationInfo step;
//        for(V1Service service : updateApp.getServices())
//        {
//            String portKind = service.getSpec().getType();
//            List<V1Service> oriServices = oriApp.getServices().stream().filter(svc->svc.getSpec().getType().equals(portKind)).collect(Collectors.toList());
//            boolean isChanged = this.ik8sApiService.isServicePortChanged(portKind, service, oriServices);
//            if(isChanged)
//            {
//                for(V1Service svc : oriServices)
//                {
//                    step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE,
//                            svc.getMetadata().getName(), K8sOperation.DELETE, svc);
//                    steps.add(step);
//                }
//            }
//        }
//        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.DEPLOYMENT,
//                updateApp.getConfigMap().getMetadata().getName(), K8sOperation.DELETE, oriApp.getDeploy());
//        steps.add(step);
//        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP,
//                updateApp.getConfigMap().getMetadata().getName(), K8sOperation.DELETE, oriApp.getConfigMap());
//        steps.add(step);
//        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP,
//                updateApp.getConfigMap().getMetadata().getName(), K8sOperation.CREATE, updateApp.getConfigMap());
//        steps.add(step);
//        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.DEPLOYMENT,
//                updateApp.getConfigMap().getMetadata().getName(), K8sOperation.CREATE, updateApp.getDeploy());
//        steps.add(step);
////        for(V1Service service : updateApp.getServices())
////        {
////            String portKind = service.getSpec().getType();
////            List<V1Service> oriServices = oriApp.getServices().stream().filter(svc->svc.getSpec().getType().equals(portKind)).collect(Collectors.toList());
////            boolean isChanged = this.ik8sApiService.isServicePortChanged(portKind, service, oriServices);
////            if(isChanged)
////            {
////                step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE,
////                        service.getMetadata().getName(), K8sOperation.CREATE, service);
////                steps.add(step);
////            }
////        }
//        logger.debug(String.format("update %s at domain %s steps are %s", alias, domainId, gson.toJson(steps)));
//        return steps;
//    }

    @Override
    public List<K8sOperationInfo> generateUpdatePlatformAppSteps(String jobId, AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException, ApiException, InterfaceCallException, IOException {
        String domainId = domain.getDomainId();
        String alias = appBase.getAlias();
        String appName = appBase.getAppName();
        String version = appBase.getVersion();
        logger.debug(String.format("generate update step for %s at %s", alias, domainId));
        String platformId = platform.getPlatformId();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        K8sCCODDomainAppVo oriApp = getCCODDomainApp(appName, alias, version, domainId, platformId, k8sApiUrl, k8sAuthToken);
        K8sCCODDomainAppVo updateApp = generateNewCCODDomainApp(appBase, domain, platform);
        List<K8sOperationInfo> steps = new ArrayList<>();
        List<K8sOperationInfo> configMapSteps = fromOriAndDst(oriApp.getConfigMap(), oriApp.getConfigMap().getMetadata().getName(),
                updateApp.getConfigMap(), updateApp.getConfigMap().getMetadata().getName(), K8sKind.CONFIGMAP, jobId, platformId, domainId);
        steps.addAll(configMapSteps);
        Map<String, Object> oriMap = oriApp.getServices().stream().collect(Collectors.toMap(s->s.getMetadata().getName(), Function.identity()));
        Map<String, Object> dstMap = updateApp.getServices().stream().collect(Collectors.toMap(s->s.getMetadata().getName(), Function.identity()));
        List<K8sOperationInfo> svcSteps = fromOriAndDst(oriMap, dstMap, K8sKind.SERVICE, jobId, platformId, domainId);
        steps.addAll(svcSteps);
        oriMap = oriApp.getIngresses().stream().collect(Collectors.toMap(s->s.getMetadata().getName(), Function.identity()));
        dstMap = updateApp.getIngresses().stream().collect(Collectors.toMap(s->s.getMetadata().getName(), Function.identity()));
        List<K8sOperationInfo> ingressSteps = fromOriAndDst(oriMap, dstMap, K8sKind.INGRESS, jobId, platformId, domainId);
        steps.addAll(ingressSteps);
        List<K8sOperationInfo> deploySteps = fromOriAndDst(oriApp.getDeploy(), oriApp.getDeploy().getMetadata().getName(),
                updateApp.getDeploy(), updateApp.getDeploy().getMetadata().getName(), K8sKind.DEPLOYMENT, jobId, platformId, domainId);
        steps.addAll(deploySteps);
        logger.debug(String.format("update %s at domain %s steps are %s", alias, domainId, gson.toJson(steps)));
        return steps;
    }

    private List<K8sOperationInfo> fromOriAndDst(Map<String, Object> ori, Map<String, Object> dst, K8sKind kind, String jobId, String platformId, String domainId){
        List<K8sOperationInfo> steps = new ArrayList<>();
        ori.forEach((k,v)->{
            K8sOperationInfo step;
            if(!dst.containsKey(k)){
                step = new K8sOperationInfo(jobId, platformId, domainId, kind, k, K8sOperation.DELETE, v);
            }
            else{
                step = new K8sOperationInfo(jobId, platformId, domainId, kind, k, K8sOperation.REPLACE, dst.get(k));
            }
            steps.add(step);
        });
        dst.forEach((k,v)->{
            K8sOperationInfo step = new K8sOperationInfo(jobId, platformId, domainId, kind, k, K8sOperation.CREATE, v);
            steps.add(step);
        });
        return steps;
    }

    private List<K8sOperationInfo> fromOriAndDst(Object ori, String oriName, Object dst, String dstName, K8sKind kind, String jobId, String platformId, String domainId){
        List<K8sOperationInfo> steps = new ArrayList<>();
        K8sOperationInfo step;
        if(oriName.equals(dstName)){
            step = new K8sOperationInfo(jobId, platformId, domainId, kind, dstName, K8sOperation.REPLACE, dst);
            steps.add(step);
        }
        else{
            step = new K8sOperationInfo(jobId, platformId, domainId, kind, dstName, K8sOperation.DELETE, ori);
            steps.add(step);
            step = new K8sOperationInfo(jobId, platformId, domainId, kind, dstName, K8sOperation.CREATE, dst);
            steps.add(step);
        }
        return steps;
    }

    private List<K8sOperationInfo> generateDeleteAppSteps(String jobId, AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException, ApiException
    {
        String domainId = domain.getDomainId();
        String alias = appBase.getAlias();
        String appName = appBase.getAppName();
        String version = appBase.getVersion();
        String platformId = platform.getPlatformId();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        if(!this.ik8sApiService.isNamespaceExist(platformId, k8sApiUrl, k8sAuthToken))
            throw new ParamException(String.format("namespace %s not exist at %s", platformId, k8sApiUrl));
        String name = String.format("%s-%s", alias, domainId);
        Map<String, String> selector = this.getCCODDomainAppSelector(appName, alias, version, appBase.getAppType(), domainId, K8sKind.DEPLOYMENT);;
        List<V1Deployment> srcDeploys = this.ik8sApiService.selectNamespacedDeployment(platformId, selector, k8sApiUrl, k8sAuthToken);
        selector = this.getCCODDomainAppSelector(appName, alias, version, appBase.getAppType(), domainId, K8sKind.SERVICE);
        List<V1Service> srcServices = this.ik8sApiService.selectNamespacedService(platformId, selector, k8sApiUrl, k8sAuthToken);
        ExtensionsV1beta1Ingress srcIngress = null;
        if(this.ik8sApiService.isNamespacedIngressExist(name, platformId, k8sApiUrl, k8sAuthToken))
            srcIngress = this.ik8sApiService.readNamespacedIngress(name, platformId, k8sApiUrl, k8sAuthToken);
        V1ConfigMap srcCm = null;
        if(this.ik8sApiService.isNamespacedConfigMapExist(name, platformId, k8sApiUrl, k8sAuthToken))
            srcCm = this.ik8sApiService.readNamespacedConfigMap(name, platformId, k8sApiUrl, k8sAuthToken);
        List<K8sOperationInfo> steps = new ArrayList<>();
        if(srcIngress != null)
            steps.add(new K8sOperationInfo(jobId, platformId, domainId, K8sKind.INGRESS, name, K8sOperation.DELETE, srcIngress));
        for(V1Service svc : srcServices)
            steps.add(new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE, svc.getMetadata().getName(), K8sOperation.DELETE, svc));
        for(V1Deployment deploy : srcDeploys)
            steps.add(new K8sOperationInfo(jobId, platformId, domainId, K8sKind.DEPLOYMENT, deploy.getMetadata().getName(), K8sOperation.DELETE, deploy));
        if(srcCm != null)
            steps.add(new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP, name, K8sOperation.DELETE, srcCm));
        return steps;
    }

    @Override
    public List<K8sOperationInfo> generateDebugPlatformAppSteps(String jobId, AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform, int timeout) throws ParamException, ApiException, InterfaceCallException, IOException {
        String domainId = domain.getDomainId();
        String alias = appBase.getAlias();
        String appName = appBase.getAppName();
        String version = appBase.getVersion();
        String platformId = platform.getPlatformId();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        if(!this.ik8sApiService.isNamespaceExist(platformId, k8sApiUrl, k8sAuthToken))
            throw new ParamException(String.format("namespace %s not exist at %s", platformId, k8sApiUrl));
        String name = String.format("%s-%s", alias, domainId);
        Map<String, String> selector = this.getCCODDomainAppSelector(appName, alias, version, appBase.getAppType(), domainId, K8sKind.DEPLOYMENT);;
        List<V1Deployment> srcDeploys = this.ik8sApiService.selectNamespacedDeployment(platformId, selector, k8sApiUrl, k8sAuthToken);
        selector = this.getCCODDomainAppSelector(appName, alias, version, appBase.getAppType(), domainId, K8sKind.SERVICE);
        List<V1Service> srcServices = this.ik8sApiService.selectNamespacedService(platformId, selector, k8sApiUrl, k8sAuthToken);
        ExtensionsV1beta1Ingress srcIngress = null;
        if(this.ik8sApiService.isNamespacedIngressExist(name, platformId, k8sApiUrl, k8sAuthToken))
            srcIngress = this.ik8sApiService.readNamespacedIngress(name, platformId, k8sApiUrl, k8sAuthToken);
        V1ConfigMap srcCm = null;
        if(this.ik8sApiService.isNamespacedConfigMapExist(name, platformId, k8sApiUrl, k8sAuthToken))
            srcCm = this.ik8sApiService.readNamespacedConfigMap(name, platformId, k8sApiUrl, k8sAuthToken);
        List<K8sOperationInfo> steps = new ArrayList<>();
        if(srcIngress != null)
            steps.add(new K8sOperationInfo(jobId, platformId, domainId, K8sKind.INGRESS, name, K8sOperation.DELETE, srcIngress));
        for(V1Service svc : srcServices)
            steps.add(new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE, svc.getMetadata().getName(), K8sOperation.DELETE, svc));
        for(V1Deployment deploy : srcDeploys)
            steps.add(new K8sOperationInfo(jobId, platformId, domainId, K8sKind.DEPLOYMENT, deploy.getMetadata().getName(), K8sOperation.DELETE, deploy));
        if(srcCm != null)
            steps.add(new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP, name, K8sOperation.DELETE, srcCm));
        List<K8sOperationInfo> addSteps = generateAddPlatformAppSteps(jobId, appBase, domain, platform, true);
        addSteps.forEach(v->{
            if (v.getKind().equals(K8sKind.DEPLOYMENT) && v.getOperation().equals(K8sOperation.CREATE)) {
                v.setKernal(true);
                v.setTimeout(timeout);
            }});
        steps.addAll(addSteps);
        return steps;
    }

    /**
     * 生成选择器用于选择k8s上的ccod域应用相关资源
     * @param appName 应用名
     * @param alias 应用别名
     * @param version 应用版本
     * @param appType 应用类型
     * @param domainId 域id
     * @param kind k8s资源类型
     * @return 生成的选择器
     */
    @Override
    public Map<String, String> getCCODDomainAppSelector(String appName, String alias, String version, AppType appType, String domainId, K8sKind kind)
    {
        Map<String, String> selector = new HashMap<>();
        selector.put(this.domainIdLabel, domainId);
        selector.put(appName, alias);
        return selector;
    }

    private List<K8sOperationInfo> generatePlatformBaseSteps(String jobId, PlatformPo platform, boolean isBase) throws ApiException, ParamException, IOException, InterfaceCallException
    {
        String ns = isBase ? String.format("base-%s", platform.getPlatformId()) : platform.getPlatformId();
        String ccodVersion = platform.getCcodVersion();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        String checkPlatformId = isBase ? String.format("base-%s", platform.getPlatformId()) : platform.getPlatformId();
        if(this.ik8sApiService.isNamespaceExist(checkPlatformId, k8sApiUrl, k8sAuthToken))
            throw new ParamException(String.format("namespace %s has exist at %s", checkPlatformId, k8sApiUrl));
        String platformTag = StringUtils.isBlank(platform.getTag()) ? "standard" : platform.getTag();
        if(isBase){
            platformTag = String.format("%s,base", platformTag);
        }
        List<K8sOperationInfo> steps = new ArrayList<>();
        V1Namespace namespace = generateNamespace(ccodVersion, platform.getPlatformId(), platformTag);
        K8sOperationInfo step = new K8sOperationInfo(jobId, ns, null, K8sKind.NAMESPACE, ns, K8sOperation.CREATE, namespace);
        steps.add(step);
        List<V1Secret> secrets = generateSecret(platform, platformTag);
        if(!isBase && !secrets.stream().map(s->s.getMetadata().getName()).collect(Collectors.toSet()).contains("ssl")){
            V1Secret sslCert = this.ik8sApiService.generateNamespacedSSLCert(platform.getPlatformId(), platform.getK8sApiUrl(), platform.getK8sAuthToken());
            secrets.add(sslCert);
        }
        for(V1Secret secret : secrets) {
            step = new K8sOperationInfo(jobId, ns, null, K8sKind.SECRET, secret.getMetadata().getName(), K8sOperation.CREATE, secret);
            steps.add(step);
        }
        generatePersistentVolume(platform, platformTag).forEach(v->steps.add(new K8sOperationInfo(jobId, ns, null, K8sKind.PV, v.getMetadata().getName(), K8sOperation.CREATE, v)));
        generatePersistentVolumeClaim(platform, platformTag).forEach(v->steps.add(new K8sOperationInfo(jobId, ns, null, K8sKind.PVC, v.getMetadata().getName(), K8sOperation.CREATE, v)));
        List<V1ConfigMap> configMaps = generateConfigMap(platform,  platformTag);
        V1ConfigMap configMap = this.ik8sApiService.getConfigMapFromNexus(ns, ns, ns,
                platform.getCfgs().stream().map(cfg->cfg.getNexusAssetInfo(this.nexusHostUrl)).collect(Collectors.toList()),
                this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
        configMaps.add(configMap);
        configMaps.forEach(c->steps.add(new K8sOperationInfo(jobId, ns, null, K8sKind.CONFIGMAP,
                ns, K8sOperation.CREATE, c)));
        return steps;
    }

    @Override
    public List<K8sOperationInfo> generatePlatformCreateSteps(
            String jobId, PlatformPo platform, List<CCODThreePartAppPo> threePartApps) throws ApiException, ParamException, IOException, InterfaceCallException {
        String platformId = platform.getPlatformId();
        List<K8sOperationInfo> steps = generatePlatformBaseSteps(jobId, platform, false);
        generateThreePartServices(String.format("base-%s", platformId), threePartApps, platform).forEach(s->{
            steps.add(new K8sOperationInfo(jobId, platformId, null, K8sKind.SERVICE, s.getMetadata().getName(), K8sOperation.CREATE, s));
        });
        return steps;
    }

    @Override
    public List<K8sOperationInfo> generateBasePlatformCreateSteps(String jobId, PlatformPo platform, List<CCODThreePartAppPo> threePartApps) throws ApiException, ParamException, IOException, InterfaceCallException {
        String ns = String.format("base-%s", platform.getPlatformId());
        List<K8sOperationInfo> steps = generatePlatformBaseSteps(jobId, platform, true);
        for(CCODThreePartAppPo threePartAppPo : threePartApps){
            K8sThreePartAppVo vo = generateK8sThreePartApp(platform, threePartAppPo, true);
            vo.getConfigMaps().forEach(c->steps.add(new K8sOperationInfo(jobId, ns, null, K8sKind.CONFIGMAP, c.getMetadata().getName(), K8sOperation.CREATE, c)));
            vo.getEndpoints().forEach(e->steps.add(new K8sOperationInfo(jobId, ns, null, K8sKind.ENDPOINTS, e.getMetadata().getName(), K8sOperation.CREATE, e)));
            vo.getServices().forEach(s->steps.add(new K8sOperationInfo(jobId, ns, null, K8sKind.SERVICE, s.getMetadata().getName(), K8sOperation.CREATE, s)));
            vo.getDeploys().forEach(d->steps.add(new K8sOperationInfo(jobId, ns, null, K8sKind.DEPLOYMENT, d.getMetadata().getName(), K8sOperation.CREATE, d, threePartAppPo.getTimeout())));
            vo.getStatefulSets().forEach(s->steps.add(new K8sOperationInfo(jobId, ns, null, K8sKind.STATEFULSET, s.getMetadata().getName(), K8sOperation.CREATE, s)));
            vo.getIngresses().forEach(s->steps.add(new K8sOperationInfo(jobId, ns, null, K8sKind.INGRESS, s.getMetadata().getName(), K8sOperation.CREATE, s)));
        }
        return steps;
    }

    @Override
    public List<K8sOperationInfo> generateDomainDeploySteps(
            String jobId, PlatformPo platformPo, DomainUpdatePlanInfo plan, List<PlatformAppDeployDetailVo> domainApps,
            boolean isNewPlatform) throws ApiException, InterfaceCallException, IOException, ParamException
    {
        String domainId = plan.getDomainId();
        DomainPo domain = plan.getDomain(platformPo.getPlatformId());
        List<K8sOperationInfo> steps = new ArrayList<>();
        String platformId = platformPo.getPlatformId();
        String k8sApiUrl = platformPo.getK8sApiUrl();
        String k8sAuthToken = platformPo.getK8sAuthToken();
        Map<String, PlatformAppDeployDetailVo> aliasAppMap = domainApps.stream().collect(Collectors.toMap(o->o.getAlias(), v->v));
        plan.getApps().stream().filter(o->o.getOperation().equals(AppUpdateOperation.UPDATE))
                .forEach(o->o.fill(aliasAppMap.get(o.getAlias())));
        List<AppFileNexusInfo> domainCfg = plan.getPublicConfig();
        if(!isNewPlatform && domainCfg != null && domainCfg.size() > 0) {
            if(this.ik8sApiService.isNamespacedConfigMapExist(domainId, platformId, k8sApiUrl, k8sAuthToken)) {
                V1ConfigMap configMap = this.ik8sApiService.readNamespacedConfigMap(domainId, platformId, k8sApiUrl, k8sAuthToken);
                K8sOperationInfo optInfo = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP, domainId, K8sOperation.DELETE, configMap);
                steps.add(optInfo);
            }
        }
        if(domainCfg != null && domainCfg.size() > 0) {
            V1ConfigMap configMap = this.ik8sApiService.getConfigMapFromNexus(platformId, domainId, domainId,
                    domainCfg.stream().map(cfg->cfg.getNexusAssetInfo(nexusHostUrl)).collect(Collectors.toList()),
                    nexusHostUrl, nexusUserName, nexusPassword);
            K8sOperationInfo optInfo = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP, domainId, K8sOperation.CREATE, configMap);
            steps.add(optInfo);
        }
        List<K8sAppCollection> collections = generateK8sAppCollections(plan.getApps(), domain, platformPo);
        for(K8sAppCollection collection : collections){
            List<K8sOperationInfo> appSteps = generateDeployStepFromCollection(jobId, collection, domain, platformPo);
            steps.addAll(appSteps);
            if(collection.getAppName().equals("UCDServer")){
                String glsDomId = collection.getDomain().getDomainId();
                List<V1Deployment> glsList;
                if(isNewPlatform){
                    glsList = collections.stream().filter(c->c.getAppName().equals("glsServer")).map(c->c.getDeployment()).collect(Collectors.toList());
                }
                else{
                    Map<String, String> selector = new HashMap<>();
                    selector.put("glsServer", "glsserver");
                    glsList = ik8sApiService.selectNamespacedDeployment(platformId, selector, k8sApiUrl, k8sAuthToken);
                }
                glsList.forEach(d->{
                    K8sOperationInfo info = new K8sOperationInfo(jobId, platformId, glsDomId, K8sKind.DEPLOYMENT, d.getMetadata().getName(), K8sOperation.REPLACE, d);
                    info.setTimeout(60);
                    steps.add(info);
                });
            }

        }
        steps.forEach(s->s.setKernal(false));
        logger.info(String.format("deploy %s %d apps need %d steps", domainId, plan.getApps().size(), steps.size()));
        return steps;
    }

    private List<String> getIntegrationDeploy(AppUpdateOperationInfo appBase, DomainPo domain, PlatformPo platform) throws ParamException{
        Object selectObject = selectK8sObjectForApp(K8sKind.DEPLOYMENT, appBase.getAppName(), appBase.getVersion(), appBase.getAppType(), appBase.getTag(), platform.getCcodVersion(), new HashMap<>());
        if(selectObject == null){
            throw new ParamException(String.format("can not find ingress template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    appBase.getAppName(), appBase.getVersion(), appBase.getAppType().name, platform.getCcodVersion(), appBase.getTag()));
        }
        V1Deployment deploy = ((List<V1Deployment>)selectObject).get(0);
        return deploy.getSpec().getTemplate().getSpec().getContainers().stream().map(c->c.getName()).collect(Collectors.toList());
    }

    private List<V1Service> generateThreePartServices(String baseNamespace, List<CCODThreePartAppPo> threePartApps, PlatformPo platform) throws ParamException
    {
        List<V1Service> allServices = new ArrayList<>();
        for(CCODThreePartAppPo threePartAppPo : threePartApps){
            Map<String, String> k8sMacroData = threePartAppPo.getK8sMacroData(platform);
            Object selectObject = selectK8sObjectForApp(K8sKind.SERVICE, threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP, threePartAppPo.getTag(), platform.getCcodVersion(), k8sMacroData);
            if(selectObject == null){
                throw new ParamException(String.format("can not find service template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                        threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP.name, platform.getCcodVersion(), threePartAppPo.getTag()));
            }
            List<V1Service> services = (List<V1Service>)selectObject;
            services.forEach(s->{
                V1Service svc = new V1Service();
                svc.setApiVersion("v1");
                svc.setKind("Service");
                V1ObjectMeta meta = new V1ObjectMeta();
                meta.setLabels(s.getMetadata().getLabels());
                meta.setNamespace(platform.getPlatformId());
                meta.setName(s.getMetadata().getName());
                svc.setMetadata(meta);
                V1ServiceSpec spec = new V1ServiceSpec();
                spec.setType("ExternalName");
                spec.setExternalName(String.format("%s.%s.svc.cluster.local", s.getMetadata().getName(), baseNamespace));
                svc.setSpec(spec);
                allServices.add(svc);
            });
        }
        return allServices;
    }

    private K8sThreePartAppVo generateK8sThreePartApp(PlatformPo platform, CCODThreePartAppPo threePartAppPo, boolean isBase) throws ParamException
    {
        Map<String, String> k8sMacroData = threePartAppPo.getK8sMacroData(platform);
        if(threePartAppPo.getParams() != null && threePartAppPo.getParams().size() > 0){
            threePartAppPo.getParams().forEach((k,v)->{
                k8sMacroData.put(String.format("${%s.%s}", threePartAppPo.getAppName(), k).toUpperCase(), v);
            });
        }
        Object selectObject = selectK8sObjectForApp(K8sKind.SERVICE, threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP, threePartAppPo.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find service template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP.name, platform.getCcodVersion(), threePartAppPo.getTag()));
        }
        List<V1Service> services = (List<V1Service>)selectObject;
        services.forEach(s->{
            if(s.getMetadata().getLabels() == null){
                s.getMetadata().setLabels(new HashMap<>());
            }
            s.getMetadata().getLabels().put(appNameLabel, threePartAppPo.getAppName());
            s.getMetadata().getLabels().put(threePartAppPo.getAppName(), threePartAppPo.getAlias());
        });
        selectObject = selectK8sObjectForApp(K8sKind.DEPLOYMENT, threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP, threePartAppPo.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find deployment template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP.name, platform.getCcodVersion(), threePartAppPo.getTag()));
        }
        List<V1Deployment> deployments = (List<V1Deployment>)selectObject;
        deployments.forEach(d->{
            if(d.getMetadata().getLabels() == null){
                d.getMetadata().setLabels(new HashMap<>());
            }
            d.getMetadata().getLabels().put(appNameLabel, threePartAppPo.getAppName());
            d.getMetadata().getLabels().put(threePartAppPo.getAppName(), threePartAppPo.getAlias());
        });
        selectObject = selectK8sObjectForApp(K8sKind.ENDPOINTS, threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP, threePartAppPo.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find endpoint template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP.name, platform.getCcodVersion(), threePartAppPo.getTag()));
        }
        List<V1Endpoints> endpoints = (List<V1Endpoints>)selectObject;
        endpoints.forEach(e->{
            if(e.getMetadata().getLabels() == null){
                e.getMetadata().setLabels(new HashMap<>());
            }
            e.getMetadata().getLabels().put(appNameLabel, threePartAppPo.getAppName());
            e.getMetadata().getLabels().put(threePartAppPo.getAppName(), threePartAppPo.getAlias());
        });
        selectObject = selectK8sObjectForApp(K8sKind.CONFIGMAP, threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP, threePartAppPo.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find configMap template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP.name, platform.getCcodVersion(), threePartAppPo.getTag()));
        }
        List<V1ConfigMap> configMaps = (List<V1ConfigMap>)selectObject;
        configMaps.forEach(c->{
            if(c.getMetadata().getLabels() == null){
                c.getMetadata().setLabels(new HashMap<>());
            }
            c.getMetadata().getLabels().put(appNameLabel, threePartAppPo.getAppName());
            c.getMetadata().getLabels().put(threePartAppPo.getAppName(), threePartAppPo.getAlias());
        });
        selectObject = selectK8sObjectForApp(K8sKind.STATEFULSET, threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP, threePartAppPo.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find statefulSet template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP.name, platform.getCcodVersion(), threePartAppPo.getTag()));
        }
        List<V1StatefulSet> statefulSets = (List<V1StatefulSet>)selectObject;
        statefulSets.forEach(s->{
            if(s.getMetadata().getLabels() == null){
                s.getMetadata().setLabels(new HashMap<>());
            }
            s.getMetadata().getLabels().put(appNameLabel, threePartAppPo.getAppName());
            s.getMetadata().getLabels().put(threePartAppPo.getAppName(), threePartAppPo.getAlias());
        });

        selectObject = selectK8sObjectForApp(K8sKind.INGRESS, threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP, threePartAppPo.getTag(), platform.getCcodVersion(), k8sMacroData);
        if(selectObject == null){
            throw new ParamException(String.format("can not find ingress template for appName=%s, version=%s, appType=%s, ccodVersion=%s and appTag=%s",
                    threePartAppPo.getAppName(), threePartAppPo.getVersion(), AppType.THREE_PART_APP.name, platform.getCcodVersion(), threePartAppPo.getTag()));
        }
        List<ExtensionsV1beta1Ingress> ingresses = (List<ExtensionsV1beta1Ingress>)selectObject;
        ingresses.forEach(s->{
            if(s.getMetadata().getLabels() == null){
                s.getMetadata().setLabels(new HashMap<>());
            }
            s.getMetadata().getLabels().put(appNameLabel, threePartAppPo.getAppName());
            s.getMetadata().getLabels().put(threePartAppPo.getAppName(), threePartAppPo.getAlias());
        });
        K8sThreePartAppVo appVo = new K8sThreePartAppVo(threePartAppPo.getAppName(), threePartAppPo.getAlias(), threePartAppPo.getVersion(), deployments, statefulSets, services, endpoints, configMaps, ingresses);
//        String json = gson.toJson(appVo);
//        if(json.matches(".*\\$\\{.*\\}.*")){
//            throw new ParamException(String.format("%s for %s must be defined", json.replaceAll(".*\\$\\{", "").replaceAll("\\}\\}", ""), threePartAppPo.getAppName()));
//        }
        return appVo;
    }

//    private Object selectK8sObjectTemplate(String ccodVersion, AppType appType, String appName, String version, K8sKind kind) throws ParamException
//    {
//        Map<String, String> params = new HashMap<>();
//        return selectK8sObjectTemplate(ccodVersion, appType, appName, version, params, kind);
//    }

//    private Object selectK8sObjectTemplate(String ccodVersion, AppType appType, String appName, String version, Map<String, String> params, K8sKind kind) throws ParamException
//    {
//        if(StringUtils.isBlank(ccodVersion))
//            throw new ParamException("ccodVersion can not be empty for select k8s object template");
//        else if(appType == null && StringUtils.isNotBlank(appName))
//            throw new ParamException(String.format("appType of %s can not be empty for select k8s object template", appName));
//        else if(StringUtils.isBlank(appName) && StringUtils.isNotBlank(version))
//            throw new ParamException(String.format("appName which has version %s can not be empty for select k8s object template", version));
//        if(kind == null)
//            throw new ParamException("kind of select k8s object can not be null");
//        String errMsg = String.format("can not select %s template for ccodVersion=%s,appType=%s,appName=%s and version=%s",
//                kind.name, ccodVersion, appType==null ? "":appType.name, appName, version);
//        Map<String, String> selector = new HashMap<>();
//        selector.put(this.ccodVersionLabel, ccodVersion);
//        if(appType != null)
//            selector.put(this.appTypeLabel, appType.name);
//        if(StringUtils.isNotBlank(appName))
//            selector.put(this.appNameLabel, appName);
//        if(StringUtils.isNotBlank(version))
//            selector.put(this.appVersionLabel, version);
//        if(params == null)
//            params = new HashMap<>();
//        for(String key : params.keySet()){
//            selector.put(key, params.get(key));
//        }
//        Object template = selectK8sObjectTemplate(selector, kind);
//        if(template != null){
//            logger.info(String.format("%s template for %s is : %s", kind.name, gson.toJson(selector), gson.toJson(template)));
//            return template;
//        }
//        logger.debug(String.format("not select %s template for %s", kind.name, gson.toJson(selector)));
//        if(selector.size() == 1)
//            throw new ParamException(errMsg);
//        if(StringUtils.isNotBlank(version))
//            selector.remove(this.appVersionLabel);
//        else if(StringUtils.isNotBlank(appName))
//            selector.remove(this.appNameLabel);
//        else
//            selector.remove(this.appTypeLabel);
//        template = selectK8sObjectTemplate(selector, kind);
//        if(template != null){
//            logger.info(String.format("%s template for %s is : %s", kind.name, gson.toJson(selector), gson.toJson(template)));
//            return template;
//        }
//        logger.debug(String.format("not select %s template for %s", kind.name, gson.toJson(selector)));
//        if(selector.size() == 1)
//            throw new ParamException(errMsg);
//        if(StringUtils.isNotBlank(appName))
//            selector.remove(this.appNameLabel);
//        else
//            selector.remove(this.appTypeLabel);
//        template = selectK8sObjectTemplate(selector, kind);
//        if(template != null){
//            logger.info(String.format("%s template for %s is : %s", kind.name, gson.toJson(selector), gson.toJson(template)));
//            return template;
//        }
//        logger.debug(String.format("not select %s template for %s", kind.name, gson.toJson(selector)));
//        if(selector.size() == 1)
//            throw new ParamException(errMsg);
//        selector.remove(this.appTypeLabel);
//        template = selectK8sObjectTemplate(selector, kind);
//        if(template != null){
//            logger.info(String.format("%s template for %s is : %s", kind.name, gson.toJson(selector), gson.toJson(template)));
//            return template;
//        }
//        throw new ParamException(errMsg);
//    }

//    private Object selectK8sObjectTemplate(Map<String, String> selector, K8sKind kind)
//    {
//        for(K8sObjectTemplatePo templatePo : this.objectTemplateList)
//        {
//            if(isMatch(selector, templatePo.getLabels()))
//            {
//                switch (kind)
//                {
//                    case ENDPOINTS:
//                        if(templatePo.getEndpoints() != null)
//                            return templatePo.getEndpoints();
//                        break;
//                    case INGRESS:
//                        if(templatePo.getIngress() != null)
//                            return templatePo.getIngress();
//                        break;
//                    case SERVICE:
//                        if(templatePo.getServices() != null)
//                            return templatePo.getServices();
//                        break;
//                    case DEPLOYMENT:
//                        if(templatePo.getDeployments() != null)
//                            return templatePo.getDeployments();
//                        break;
//                    case PVC:
//                        if(templatePo.getPvcList() != null)
//                            return templatePo.getPvcList();
//                        break;
//                    case PV:
//                        if(templatePo.getPvList() != null)
//                            return templatePo.getPvList();
//                        break;
//                    case NAMESPACE:
//                        if(templatePo.getNamespaces() != null)
//                            return templatePo.getNamespaces();
//                        break;
//                    case JOB:
//                        if(templatePo.getJobs() != null)
//                            return templatePo.getJobs();
//                        break;
//                    case SECRET:
//                        if(templatePo.getSecrets() != null)
//                            return templatePo.getSecrets();
//                        break;
//                    case CONFIGMAP:
//                        if(templatePo.getConfigMaps() != null)
//                            return templatePo.getConfigMaps();
//                    case STATEFULSET:
//                        if(templatePo.getStatefulSets() != null)
//                            return templatePo.getStatefulSets();
//                        break;
//                }
//            }
//        }
//        return null;
//    }

    private String getPortString(List<V1ServicePort> ports, String serviceType)
    {
        return ports.stream().map(p->serviceType.equals("ClusterIP")?String.format("%s/%s", p.getPort(), p.getProtocol()) : String.format("%s:%s/%s", p.getPort(), p.getNodePort(), p.getProtocol()))
                .collect(Collectors.joining(","));
    }

    PlatformAppDeployDetailVo getAppDetailFromK8sObj(String alias, V1Deployment deploy, List<V1Service> services, V1ConfigMap configMap) throws IOException, InterfaceCallException, NexusException, ParamException
    {
        V1Container init = deploy.getSpec().getTemplate().getSpec().getInitContainers().stream()
                .collect(Collectors.toMap(V1Container::getName, Function.identity())).get(alias);
        AppModuleVo module = appManagerService.getRegisteredCCODAppFromImageUrl(init.getImage());
        String volume = module.getAppType().equals(AppType.BINARY_FILE) || module.getAppType().equals(AppType.JAR) ? "binary-file" : "war";
        V1Container runtime = deploy.getSpec().getTemplate().getSpec().getContainers().stream()
                .collect(Collectors.toMap(k->k.getName(), v->v)).get(String.format("%s-runtime", alias));
        String basePath = module.getAppType().equals(AppType.NODEJS) ? runtime.getCommand().get(2).replaceAll(";.*", "").replaceAll("^\\s*cd\\s+", "")
                : runtime.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()))
                .get(volume).getMountPath().replaceAll("/$", "");
        if(module.getAppType().equals(AppType.TOMCAT_WEB_APP) || module.getAppType().equals(AppType.RESIN_WEB_APP)){
            basePath = basePath.replaceAll("/webapps$", "");
        }
        PlatformAppDeployDetailVo detail = new PlatformAppDeployDetailVo();
        Map<String, String> labels = deploy.getSpec().getTemplate().getMetadata().getLabels();
        for(V1Service service : services){
            String portStr = getPortString(service.getSpec().getPorts(), service.getSpec().getType());
            if(service.getSpec().getType().equals("ClusterIP"))
                detail.setPorts(portStr);
            else
                detail.setNodePorts(portStr);
        }
        detail.setReplicas(deploy.getStatus().getReplicas());
        detail.setAvailableReplicas(deploy.getStatus().getAvailableReplicas());
        K8sStatus status = this.ik8sApiService.getStatusFromDeployment(deploy);
        detail.setStatus(status.name);
        detail.setInitCmd(module.getInitCmd());
        detail.setAssembleTag(deploy.getMetadata().getName());
        detail.setPlatformId(deploy.getMetadata().getNamespace());
        detail.setAlias(alias);
        detail.setBasePath(basePath);
        String domainId = labels.get(this.domainIdLabel);
        detail.setDomainId(domainId);
        List<String> cmd = runtime.getCommand();
        List<String> commands = Arrays.asList(cmd.get(2).replaceAll(";$", "").replaceAll("\\s*;\\s*", ";").split(";"));
        String cmdTag = "resin.sh";
        if(module.getAppType().equals(AppType.TOMCAT_WEB_APP))
            cmdTag = "startup.sh";
        else if(module.getAppType().equals(AppType.BINARY_FILE) || module.getAppType().equals(AppType.JAR))
            cmdTag = module.getInstallPackage().getFileName();
        else if(module.getAppType().equals(AppType.NODEJS))
            cmdTag = "nginx";
        String cmdRegex = String.format(".*(/|\\s+)?%s(\\s.+|$)", cmdTag);
        String startCmd = null;
        int index = 0;
        for(int i = 0; i < commands.size(); i++){
            String command = commands.get(i);
            if(command.matches(cmdRegex) && !command.matches("^(mv|cp|touch) .+")){
                startCmd = command;
                index = i;
            }
        }
        if(startCmd == null){
            throw new ParamException(String.format("can not parse startCmd from %s at container %s in deployment %s",
                    String.join(";", commands), runtime.getName(), deploy.getMetadata().getName()));
        }
        String initCmd = String.join(";", commands.subList(0, index));
        String logOutputCmd = String.join(";", commands.subList(index+1, commands.size()));
        detail.setStartCmd(startCmd);
        detail.setInitCmd(initCmd);
        detail.setLogOutputCmd(logOutputCmd);
        if(runtime.getLivenessProbe() != null){
            detail.setInitialDelaySeconds(runtime.getLivenessProbe().getInitialDelaySeconds());
            detail.setPeriodSeconds(runtime.getLivenessProbe().getPeriodSeconds());
            if(runtime.getLivenessProbe().getHttpGet() != null){
                detail.setCheckAt(String.format("%d/%s", runtime.getLivenessProbe().getHttpGet().getPort().getIntValue(), runtime.getLivenessProbe().getHttpGet().getScheme()));
            }
            else if(runtime.getLivenessProbe().getTcpSocket() != null){
                detail.setCheckAt(String.format("%d/TCP", runtime.getLivenessProbe().getTcpSocket().getPort().getIntValue()));
            }
            else if(runtime.getLivenessProbe().getExec() != null){
                detail.setCheckAt(String.format("%s/CMD", runtime.getLivenessProbe().getExec().getCommand()));
            }
        }
        detail.setEnvLoadCmd(init.getCommand().get(2));
        if(configMap != null){
            String mountPath = runtime.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, v->v.getMountPath())).get(volume);
            List<AppFileNexusInfo> cfgs = restoreConfigFileFromConfigMap(configMap, init.getCommand().get(2), detail.getBasePath(), volume, mountPath);
            detail.setCfgs(cfgs);
        }
        detail.fill(module);
        return detail;
    }

    PlatformAppDeployDetailVo parseAppDetailFromK8s(String alias, V1Deployment deploy, List<V1Service> services, V1ConfigMap configMap) throws IOException, InterfaceCallException, NexusException, ParamException
    {
        if(alias.equals("umg")){
            System.out.println("haha");
        }
        PlatformAppDeployDetailVo detail = new PlatformAppDeployDetailVo();
        Map<String, String> labels = deploy.getMetadata().getLabels();
        for(V1Service service : services){
            String portStr = getPortString(service.getSpec().getPorts(), service.getSpec().getType());
            if(service.getSpec().getType().equals("ClusterIP"))
                detail.setPorts(portStr);
            else
                detail.setNodePorts(portStr);
        }
        detail.setReplicas(deploy.getStatus().getReplicas());
        detail.setAvailableReplicas(deploy.getStatus().getAvailableReplicas());
        K8sStatus status = this.ik8sApiService.getStatusFromDeployment(deploy);
        detail.setStatus(status.name);
        detail.setPlatformId(deploy.getMetadata().getNamespace());
        detail.setAlias(alias);
        String domainId = labels.get(this.domainIdLabel);
        detail.setDomainId(domainId);
        return detail;
    }

    private List<AppFileNexusInfo> restoreConfigFileFromConfigMap(V1ConfigMap configMap, String cfgCreateCmd, String basePath, String volume, String mountPath) throws IOException, InterfaceCallException, NexusException
    {
        Date now = new Date();
        SimpleDateFormat sf = new SimpleDateFormat("yyyyMMddHHmmss");
        String saveDir = String.format("%s/cfgs/%s/%s", System.getProperty("user.dir"), configMap.getMetadata().getName(), sf.format(now));
        String repository = String.format("restoredFromK8s/%s/%s", configMap.getMetadata().getName(), sf.format(now));
        List<DeployFileInfo> fileList = new ArrayList<>();
        String[] cmds = cfgCreateCmd.replaceAll(String.format("(^|;)/%s", volume), mountPath)
                .replaceAll("//", "").replaceAll("^\\s+", "").replaceAll("\\s$", "")
                .replaceAll("\\s*;\\s*", ";").split(";");
        Map<String, String> cpMap = new HashMap<>();
        Arrays.stream(cmds).filter(s->s.matches("^cp\\s+.+")).forEach(s->{
            String[] arr = s.split("\\s+");
            String fileName = arr[1].replaceAll(".*/", "");
            String deployPath = arr[2].replaceAll(String.format("%s$", fileName), "").replaceAll("/$", "");
            if(deployPath.matches(String.format("^%s(/.*|$)", basePath))) {
                deployPath = "./" + deployPath.replaceAll(String.format("^%s", ""), "").replaceAll("^/", "");
            }
            else if(StringUtils.isBlank(deployPath)){
                deployPath = "./";
            }
            cpMap.put(fileName, deployPath);
        });
        for(String fileName : configMap.getData().keySet()){
            if(cpMap.containsKey(fileName)){
                String filePath = FileUtils.saveContextToFile(saveDir, fileName, configMap.getData().get(fileName), true);
                DeployFileInfo fileInfo = new DeployFileInfo(filePath);
                fileList.add(fileInfo);
            }
            else {
                logger.error(String.format("%s for %s not used", fileName, configMap.getMetadata().getName()));
            }

        }
        List<NexusAssetInfo> assets = nexusService.uploadRawComponent(nexusHostUrl, nexusUserName, nexusPassword, platformAppCfgRepository, repository, fileList.toArray(new DeployFileInfo[0]));
        return assets.stream().map(a->new AppFileNexusInfo(a, cpMap.get(a.getNexusAssetFileName()))).collect(Collectors.toList());
    }

    @Override
    public List<PlatformAppDeployDetailVo> getPlatformAppDetailFromK8s(PlatformPo platform, boolean isGetCfg) throws ApiException, ParamException, InterfaceCallException, NexusException, IOException
    {
        String platformId = platform.getPlatformId();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        List<V1Deployment> deployments = this.ik8sApiService.listNamespacedDeployment(platformId, k8sApiUrl, k8sAuthToken);
        List<V1Service> services = this.ik8sApiService.listNamespacedService(platformId, k8sApiUrl, k8sAuthToken);
        List<PlatformAppDeployDetailVo> details = new ArrayList<>();
        for(V1Deployment deployment : deployments){
            Map<String, String> labels = deployment.getMetadata().getLabels();
            if(!labels.containsKey(appTypeLabel)|| !labels.containsKey(domainIdLabel) ||labels.get(appTypeLabel).equals(AppType.THREE_PART_APP.name)){
                continue;
            }
            List<V1Container> containers = new ArrayList<>();
            containers.addAll(deployment.getSpec().getTemplate().getSpec().getContainers());
            if(deployment.getSpec().getTemplate().getSpec().getInitContainers() != null){
                containers.addAll(deployment.getSpec().getTemplate().getSpec().getInitContainers());
            }
            Map<String, List<V1Container>> imageMap = containers.stream().collect(Collectors.groupingBy(V1Container::getImage));
            for(String imageUrl : imageMap.keySet()){
                try{
                    if(!appManagerService.isRegisteredCCODAppImage(imageUrl)){
                        continue;
                    }
                    AppModuleVo module = appManagerService.getRegisteredCCODAppFromImageUrl(imageUrl);
                    String alias = deployment.getMetadata().getLabels().get(module.getAppName());
                    List<V1Service> relativeSvcs = services.stream().filter(s->isMatch(s.getSpec().getSelector(), deployment.getSpec().getTemplate().getMetadata().getLabels()))
                            .collect(Collectors.toList());
                    PlatformAppDeployDetailVo detail = parseAppDetailFromK8s(alias, deployment, relativeSvcs, null);
                    detail.fill(module);
                    details.add(detail);

                }
                catch (Exception ex){
                    logger.error(String.format("parse %s exception"), ex);
                }
            }
        }
        return details;
    }


    /**
     * 用来判断deployment上运行的是否是ccod域模块
     * @param deployment
     * @return
     */
    private boolean isCCODDomainAppDeployment(V1Deployment deployment)
    {
        String deployName = deployment.getMetadata().getName();
        String errTag = String.format("so %s is not ccod domain app deployment", deployName);
        Map<String, String> labels = deployment.getMetadata().getLabels();
        if(labels == null || labels.size() == 0 || !labels.containsKey(this.domainIdLabel) || !labels.containsKey(this.appTypeLabel)){
            logger.warn(String.format("deployment labels not container %s or %s tag, %s", domainIdLabel, appTypeLabel, errTag));
            return false;
        }
        AppType appType = AppType.getEnum(labels.get(appTypeLabel));
        if(appType == null){
            logger.warn(String.format("appType %s is not been supported, %s", labels.get(appTypeLabel), errTag));
            return false;
        }
        switch (appType){
            case BINARY_FILE:
            case RESIN_WEB_APP:
            case TOMCAT_WEB_APP:
            case JAR:
            case NODEJS:
                break;
            default:
                logger.warn(String.format("appType %s is not supported by ccod domain app, %s", appType.name, errTag));
                return false;
        }
        String domainId = labels.get(this.domainIdLabel);
        List<V1Container> initList = deployment.getSpec().getTemplate().getSpec().getInitContainers() == null ? new ArrayList<>() : deployment.getSpec().getTemplate().getSpec().getInitContainers();
        List<V1Container> runtimeList = deployment.getSpec().getTemplate().getSpec().getInitContainers() == null ? new ArrayList<>() : deployment.getSpec().getTemplate().getSpec().getContainers();
        if(initList.size() != runtimeList.size()){
            logger.warn(String.format("%s init container count not equal runtime count, %s", deployName, errTag));
            return false;
        }
        if(deployment.getSpec().getTemplate().getSpec().getVolumes() == null || deployment.getSpec().getTemplate().getSpec().getVolumes().size() == 0){
            logger.warn(String.format("deployment %s has not any volume, %s", deployName, errTag));
            return false;
        }
        if(appType.equals(AppType.NODEJS)){
            return true;
        }
        Map<String, V1Volume> volumeMap = deployment.getSpec().getTemplate().getSpec().getVolumes().stream()
                .collect(Collectors.toMap(k->k.getName(), v->v));
        try{
            Map<String, V1Container> runtimeMap = runtimeList.stream().collect(Collectors.toMap(c->c.getName(), v->v));
            for(V1Container init : initList){
                V1Container runtime = runtimeMap.get(String.format("%s-runtime", init.getName()));
                if(runtime == null){
                    logger.warn(String.format("can not find container %s-runtime at deployment %s, %s"
                            , runtime.getName(), deployName, errTag));
                    return false;
                }
                if(runtime.getCommand() == null || runtime.getCommand().size() != 3 || !runtime.getCommand().get(0).equals("/bin/sh") || !runtime.getCommand().get(1).equals("-c")){
                    logger.warn(String.format("deployment %s %s container command is not wanted, %s", deployName, runtime.getName(), errTag));
                    return false;
                }

                if(init.getCommand() == null || init.getCommand().size() != 3 || !init.getCommand().get(0).equals("/bin/sh") || !init.getCommand().get(1).equals("-c")){
                    logger.warn(String.format("deployment %s %s container command is not wanted, %s",
                            deployName, init.getName(), errTag));
                    return false;
                }
                if(init.getVolumeMounts() == null || init.getVolumeMounts().size() == 0){
                    logger.warn(String.format("deployment %s %s container has not any volumeMount, %s", deployName, init.getName(), errTag));
                    return false;
                }
                Map<String, V1VolumeMount> initMountMap = init.getVolumeMounts().stream().collect(Collectors.toMap(k->k.getName(), v->v));
                if(runtime.getVolumeMounts() == null || runtime.getVolumeMounts().size() == 0){
                    logger.warn(String.format("deployment %s %s container has not any volumeMount, %s", deployName, runtime.getName(), errTag));
                    return false;
                }
                Map<String, V1VolumeMount> runtimeMountMap = runtime.getVolumeMounts().stream().collect(Collectors.toMap(k->k.getName(), v->v));
                AppModuleVo module = appManagerService.getRegisteredCCODAppFromImageUrl(init.getImage());
                if(!module.getAppType().equals(appType)){
                    logger.warn(String.format("appTye %s is not equal with registered %s, %s", appType.name, module.getAppType().name, errTag));
                    return false;
                }
                String volume = appType.equals(AppType.BINARY_FILE) || appType.equals(AppType.JAR) ? "binary-file" : "war";
                if(!volumeMap.containsKey(volume)){
                    logger.warn(String.format("deployment %s not find %s volume, %s", deployName, volume, errTag));
                    return false;
                }
                else if(!initMountMap.containsKey(volume)){
                    logger.warn(String.format("%s deployment %s container not has %s volume, %s", deployName, init.getName(), volume, errTag));
                    return false;
                }
                else if(!runtimeMountMap.containsKey(volume)){
                    logger.warn(String.format("%s deployment %s container not has %s volume, %s", deployName, runtime.getName(), volume, errTag));
                    return false;
                }
                volume = String.format("%s-%s-volume", init.getName(), domainId);
                if(!volumeMap.containsKey(volume)){
                    logger.warn(String.format("deployment %s not find %s configMap volume, %s", deployName, volume, errTag));
                    return false;
                }
                else if(volumeMap.get(volume).getConfigMap() == null){
                    logger.warn(String.format("deployment %s %s volume is not configMap, %s", deployName, volume, errTag));
                    return false;
                }
                else if(!initMountMap.containsKey(volume)){
                    logger.warn(String.format("%s deployment %s container not has %s configMap volume, %s", deployName, init.getName(), volume, errTag));
                    return false;
                }
            }
        }
        catch (Exception ex) {
            logger.error(String.format("parse deployment exception, %s", errTag), ex);
            return false;
        }
        return true;
    }

    @Override
    public PlatformAppDeployDetailVo getPlatformAppDetailFromK8s(PlatformPo platform, String domainId, String appName, String alias, boolean isGetCfg) {
        try{
            Map<String, String> selector = new HashMap<>();
            selector.put(this.domainIdLabel, domainId);
            selector.put(appName, alias);
            List<V1Deployment> deployments = this.ik8sApiService.selectNamespacedDeployment(platform.getPlatformId(), selector, platform.getK8sApiUrl(), platform.getK8sAuthToken());
            if(deployments.size() == 0 || deployments.size() > 1){
                throw new ParamException(String.format("%s has find %d deployment for %s", platform.getPlatformId(), deployments.size(), gson.toJson(selector)));
            }
            V1Deployment deployment = deployments.get(0);
//            if(deployment.getSpec().getTemplate().getSpec().getContainers().size() == 1){
//                if(!isCCODDomainAppDeployment(deployment)){
//                    throw new ParamException(String.format("deployment %s for %s is illegal ccod domain app deployment",
//                            deployment.getMetadata().getName(), gson.toJson(selector)));
//                }
//                V1Container initContainer = deployment.getSpec().getTemplate().getSpec().getInitContainers().stream()
//                        .collect(Collectors.toMap(k->k.getName(), v->v)).get(alias);
//                if(initContainer == null){
//                    throw new ParamException(String.format("can not find container for %s", gson.toJson(selector)));
//                }
//            }
            V1ConfigMap configMap = isGetCfg ? ik8sApiService.readNamespacedConfigMap(String.format("%s-%s", alias, domainId), platform.getPlatformId(), platform.getK8sApiUrl(), platform.getK8sAuthToken()) : null;
            List<V1Service> services = this.ik8sApiService.selectNamespacedService(platform.getPlatformId(), selector, platform.getK8sApiUrl(), platform.getK8sAuthToken());
            return parseAppDetailFromK8s(alias, deployment, services, configMap);
        }
        catch (Exception ex){
            logger.debug(String.format("get detail for 5s(%s) at %s of %s exception", alias, appName, domainId, platform.getPlatformId()), ex);
            PlatformAppDeployDetailVo detail = new PlatformAppDeployDetailVo();
            detail.setPlatformId(platform.getPlatformId());
            detail.setDomainId(domainId);
            detail.setAppName(appName);
            detail.setAlias(alias);
            detail.setStartCmd("ERROR");
            return detail;
        }
    }
}
