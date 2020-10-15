package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.config.GsonDateUtil;
import com.channelsoft.ccod.support.cmdb.constant.*;
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
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

import javax.annotation.PostConstruct;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.Map.*;

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

    @Value("${k8s.labels.platform-name}")
    private String platformNameLabel;

    @Value("${k8s.labels.domain-id}")
    private String domainIdLabel;

    @Value("${k8s.labels.app-version}")
    private String appVersionLabel;

    @Value("${k8s.labels.service-type}")
    private String serviceTypeLabel;

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

    @Value("${debug-timeout}")
    private int debugTimeout;

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

    private final List<K8sObjectTemplatePo> objectTemplateList = new ArrayList<>();

    private final List<K8sThreePartServiceVo> testThreePartServices = new ArrayList<>();

    protected String testK8sApiUrl = "https://10.130.41.218:6443";

    protected String testAuthToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA";

    @PostConstruct
    void init() throws Exception
    {
//        List<K8sObjectTemplatePo> testList = generatePlatformObjectTemplate("test-by-wyf", "4.1", "ucds-cloud01", "cas-manage01", "dcms-manage01");
//        this.objectTemplateList.addAll(testList);
//        testList = generatePlatformObjectTemplate("jhkzx-1", "3.9", "ucds-cloud01", "cas-manage01", "dcmswebservice-manage01");
////        this.objectTemplateList.addAll(testList);
        List<K8sObjectTemplatePo> list = parseTemplateFromFile(this.templateSavePath);
        this.objectTemplateList.addAll(list);
//        List<K8sThreePartServiceVo> threeSvcs = getThreePartServices("test-by-wyf", testK8sApiUrl, testAuthToken);
//        logger.warn(this.templateParseGson.toJson(threeSvcs));
//        this.testThreePartServices.addAll(threeSvcs);
        List<K8sThreePartServiceVo> threeSvcs = parseTestThreePartServiceFromFile(this.testThreePartServiceSavePath);
        this.testThreePartServices.addAll(threeSvcs);
    }

    private List<K8sObjectTemplatePo> generatePlatformObjectTemplate(String srcPlatformId, String ccodVersion, String binaryApp, String tomcatApp, String resinApp) throws ApiException
    {
        List<K8sObjectTemplatePo> templateList = new ArrayList<>();
        Map<String, String> labels = new HashMap<>();
        labels.put(this.ccodVersionLabel, ccodVersion);
        K8sObjectTemplatePo template = new K8sObjectTemplatePo(labels);
        V1Namespace ns = this.ik8sApiService.readNamespace(srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setNamespaceJson(templateParseGson.toJson(ns));
        V1Secret ssl = this.ik8sApiService.readNamespacedSecret("ssl", srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setSecretJson(templateParseGson.toJson(ssl));
        V1PersistentVolume pv = this.ik8sApiService.readPersistentVolume(String.format("base-volume-%s", srcPlatformId), testK8sApiUrl, testAuthToken);
        template.setPersistentVolumeJson(templateParseGson.toJson(pv));
        V1PersistentVolumeClaim pvc = this.ik8sApiService.readNamespacedPersistentVolumeClaim(String.format("base-volume-%s", srcPlatformId), srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setPersistentVolumeClaimJson(templateParseGson.toJson(pvc));
        templateList.add(template);
        labels = new HashMap<>();
        labels.put(this.ccodVersionLabel, ccodVersion);
        labels.put(this.appNameLabel, "oracle");
        labels.put(this.appTypeLabel, AppType.THREE_PART_APP.name);
        template = new K8sObjectTemplatePo(labels);
        V1Deployment oraDep = this.ik8sApiService.readNamespacedDeployment("oracle", srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setDeployJson(templateParseGson.toJson(oraDep));
        V1Service oraSvc = this.ik8sApiService.readNamespacedService("oracle", srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setServiceJson(templateParseGson.toJson(oraSvc));
        templateList.add(template);

        labels = new HashMap<>();
        labels.put(this.ccodVersionLabel, ccodVersion);
        labels.put(this.appNameLabel, "mysql");
        labels.put(this.appTypeLabel, AppType.THREE_PART_APP.name);
        template = new K8sObjectTemplatePo(labels);
        V1Deployment mysqlDep = this.ik8sApiService.readNamespacedDeployment("mysql", srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setDeployJson(templateParseGson.toJson(mysqlDep));
        V1Service mysqlSvc = this.ik8sApiService.readNamespacedService("mysql", srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setServiceJson(templateParseGson.toJson(mysqlSvc));
        templateList.add(template);

        labels = new HashMap<>();
        labels.put(this.ccodVersionLabel, ccodVersion);
        labels.put(this.appTypeLabel, AppType.BINARY_FILE.name);
        template = new K8sObjectTemplatePo(labels);
        V1Deployment binDep = this.ik8sApiService.readNamespacedDeployment(binaryApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        Map<String, V1Volume> volumeMap = binDep.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity()));
        volumeMap.remove(String.format("%s-volume", srcPlatformId));
        volumeMap.remove(String.format("%s-volume", binaryApp));
        binDep.getSpec().getTemplate().getSpec().setVolumes(new ArrayList<>(volumeMap.values()));
        Map<String, V1VolumeMount> volumeMountMap = binDep.getSpec().getTemplate().getSpec().getInitContainers().get(0)
                .getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()));
        volumeMountMap.remove(String.format("%s-volume", binaryApp));
        binDep.getSpec().getTemplate().getSpec().getInitContainers().get(0).setVolumeMounts(new ArrayList<>(volumeMountMap.values()));
        volumeMountMap = binDep.getSpec().getTemplate().getSpec().getContainers().get(0)
                .getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()));
        volumeMountMap.remove(String.format("%s-volume", srcPlatformId));
        binDep.getSpec().getTemplate().getSpec().getContainers().get(0).setVolumeMounts(new ArrayList<>(volumeMountMap.values()));
        template.setDeployJson(templateParseGson.toJson(binDep));
        V1Service binSvc = this.ik8sApiService.readNamespacedService(tomcatApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setServiceJson(templateParseGson.toJson(binSvc));
        templateList.add(template);

        labels = new HashMap<>();
        labels.put(this.ccodVersionLabel, ccodVersion);
        labels.put(this.appTypeLabel, AppType.TOMCAT_WEB_APP.name);
        template = new K8sObjectTemplatePo(labels);
        V1Deployment tomcatDep = this.ik8sApiService.readNamespacedDeployment(tomcatApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        volumeMap = tomcatDep.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity()));
        volumeMap.remove(String.format("%s-volume", srcPlatformId));
        volumeMap.remove(String.format("%s-volume", tomcatApp));
        tomcatDep.getSpec().getTemplate().getSpec().setVolumes(new ArrayList<>(volumeMap.values()));
        volumeMountMap = tomcatDep.getSpec().getTemplate().getSpec().getInitContainers().get(0)
                .getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()));
        volumeMountMap.remove(String.format("%s-volume", tomcatApp));
        tomcatDep.getSpec().getTemplate().getSpec().getInitContainers().get(0).setVolumeMounts(new ArrayList<>(volumeMountMap.values()));
        volumeMountMap = tomcatDep.getSpec().getTemplate().getSpec().getContainers().get(0)
                .getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()));
        volumeMountMap.remove(String.format("%s-volume", srcPlatformId));
        tomcatDep.getSpec().getTemplate().getSpec().getContainers().get(0).setVolumeMounts(new ArrayList<>(volumeMountMap.values()));
        template.setDeployJson(templateParseGson.toJson(tomcatDep));
        V1Service tomcatSvc = this.ik8sApiService.readNamespacedService(tomcatApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setServiceJson(templateParseGson.toJson(tomcatSvc));
        ExtensionsV1beta1Ingress tomIngress = this.ik8sApiService.readNamespacedIngress(tomcatApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setIngressJson(templateParseGson.toJson(tomIngress));
        templateList.add(template);

        labels = new HashMap<>();
        labels.put(this.ccodVersionLabel, ccodVersion);
        labels.put(this.appTypeLabel, AppType.RESIN_WEB_APP.name);
        template = new K8sObjectTemplatePo(labels);
        V1Deployment resinDep = this.ik8sApiService.readNamespacedDeployment(resinApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        volumeMap = resinDep.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity()));
        volumeMap.remove(String.format("%s-volume", srcPlatformId));
        volumeMap.remove(String.format("%s-volume", resinApp));
        resinDep.getSpec().getTemplate().getSpec().setVolumes(new ArrayList<>(volumeMap.values()));
        volumeMountMap = resinDep.getSpec().getTemplate().getSpec().getInitContainers().get(0)
                .getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()));
        volumeMountMap.remove(String.format("%s-volume", resinApp));
        resinDep.getSpec().getTemplate().getSpec().getInitContainers().get(0).setVolumeMounts(new ArrayList<>(volumeMountMap.values()));
        volumeMountMap = resinDep.getSpec().getTemplate().getSpec().getContainers().get(0)
                .getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()));
        volumeMountMap.remove(String.format("%s-volume", srcPlatformId));
        resinDep.getSpec().getTemplate().getSpec().getContainers().get(0).setVolumeMounts(new ArrayList<>(volumeMountMap.values()));
        template.setDeployJson(templateParseGson.toJson(resinDep));
        V1Service resinSvc = this.ik8sApiService.readNamespacedService(tomcatApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setServiceJson(templateParseGson.toJson(resinSvc));
        ExtensionsV1beta1Ingress resinIngress = this.ik8sApiService.readNamespacedIngress(tomcatApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setIngressJson(templateParseGson.toJson(resinIngress));
        templateList.add(template);
        return templateList;
    }

    @Override
    public ExtensionsV1beta1Ingress generateIngress(String ccodVersion, AppType appType, String appName, String alias, String platformId, String domainId, String hostUrl) throws ParamException {
        ExtensionsV1beta1Ingress ingress = (ExtensionsV1beta1Ingress)selectK8sObjectTemplate(ccodVersion, appType, appName, null, K8sKind.INGRESS);
        ingress.getMetadata().setNamespace(platformId);
        ingress.getMetadata().setName(String.format("%s-%s", alias, domainId));
        ingress.getSpec().getRules().get(0).setHost(hostUrl);
        ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).setPath(String.format("/%s-%s", alias, domainId));
        ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getBackend().setServiceName(String.format("%s-%s", alias, domainId));
        logger.info(String.format("generate ingress is %s",  gson.toJson(ingress)));
        return ingress;
    }

    @Override
    public V1Service generateCCODDomainAppService(String ccodVersion, AppType appType, String appName, String alias, ServicePortType portType, String portStr, String platformId, String domainId) throws ParamException {
        logger.debug(String.format("generate service for %s(%s) : portType=%s and port=%s", alias, appName, portType.name, portStr));
        if(!portType.equals(ServicePortType.ClusterIP) && !portType.equals(ServicePortType.NodePort))
            throw new ParamException(String.format("can not handle service port type : %s", portType.name));
        V1Service service = (V1Service)selectK8sObjectTemplate(ccodVersion, appType, appName, null, K8sKind.SERVICE);
        List<PortVo> portList = parsePort(portStr, portType, appType);
        String[] ports = portStr.split(",");
        String name = portType.equals(ServicePortType.NodePort) ? String.format("%s-%s-out", alias, domainId) : String.format("%s-%s", alias, domainId);
        service.getMetadata().setLabels(new HashMap<>());
        service.getMetadata().getLabels().put(this.domainIdLabel, domainId);
        service.getMetadata().getLabels().put(this.appNameLabel, appName);
        service.getMetadata().getLabels().put(this.serviceTypeLabel, portType.equals(ServicePortType.NodePort) ? K8sServiceType.DOMAIN_OUT_SERVICE.name : K8sServiceType.DOMAIN_SERVICE.name);
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
        logger.info(String.format("service for %s(%s, portType=%s and port=%s) : %s", alias, appName, portType.name, portStr));
        return service;
    }

    @Override
    public V1Service generateThreeAppService(String ccodVersion, String appName, String alias, String version, String platformId) throws ParamException {
        Map<String, String> selector = getK8sTemplateSelector(ccodVersion, appName, version, AppType.THREE_PART_APP, K8sKind.SERVICE);
        V1Service service = (V1Service)selectK8sObjectTemplate(ccodVersion, AppType.THREE_PART_APP, appName, version, K8sKind.SERVICE);
        service.getMetadata().setLabels(new HashMap<>());
        service.getMetadata().getLabels().put(appName, alias);
        service.getMetadata().getLabels().put(this.appNameLabel, appName);
        service.getMetadata().getLabels().put(this.serviceTypeLabel, K8sServiceType.THREE_PART_APP.name);
        service.getMetadata().setName(alias);
        service.getMetadata().setNamespace(platformId);
        service.getSpec().setSelector(new HashMap<>());
        service.getSpec().getSelector().put(appName, alias);
        return service;
    }

    @Override
    public V1Deployment generateCCODDomainAppDeployment(AppBase appBase, String domainId, List<AppFileNexusInfo> domainCfg, PlatformPo platform) throws ParamException {
        logger.debug(String.format("generate deployment for %s : domainId=%s", gson.toJson(appBase), domainId));
        String appName = appBase.getAppName();
        String alias = appBase.getAlias();
        String version = appBase.getVersion();
        AppModuleVo module = this.appManagerService.queryAllRegisterAppModule(true).stream()
                .collect(Collectors.groupingBy(AppModuleVo::getAppName)).get(appName).stream()
                .collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version);
        AppType appType = module.getAppType();
        String ccodVersion = module.getCcodVersion();
        Map<String, String> selector = getK8sTemplateSelector(module.getCcodVersion(), appName, version, appType, K8sKind.DEPLOYMENT);
        V1Deployment deploy = (V1Deployment)selectK8sObjectTemplate(ccodVersion, appType, appName, version, K8sKind.DEPLOYMENT);
        String basePath = appBase.getBasePath();
        String deployPath = getAbsolutePath(appBase.getBasePath(), appBase.getDeployPath());
        String platformId = platform.getPlatformId();
        deploy.getMetadata().setNamespace(platformId);
        deploy.getMetadata().setName(String.format("%s-%s", alias, domainId));
        deploy.getMetadata().setLabels(new HashMap<>());
        deploy.getMetadata().getLabels().put(this.appTypeLabel, appType.name);
        deploy.getMetadata().getLabels().put(this.domainIdLabel, domainId);
        deploy.getMetadata().getLabels().put(appName, alias);
        deploy.getSpec().getSelector().setMatchLabels(new HashMap<>());
        deploy.getSpec().getSelector().getMatchLabels().put(this.domainIdLabel, domainId);
        deploy.getSpec().getSelector().getMatchLabels().put(appName, alias);
        deploy.getSpec().getTemplate().getMetadata().setLabels(new HashMap<>());
        deploy.getSpec().getTemplate().getMetadata().getLabels().put(this.domainIdLabel, domainId);
        deploy.getSpec().getTemplate().getMetadata().getLabels().put(appName, alias);
        List<V1Volume> volumes = generateVolumeForDeployment(deploy, appType, alias, platformId, domainId, appBase.getCfgs(), platform.getCfgs(), domainCfg);
        deploy.getSpec().getTemplate().getSpec().setVolumes(volumes);
        V1Container initContainer = deploy.getSpec().getTemplate().getSpec().getInitContainers().get(0);
        logger.debug(String.format("set initContainer name : %s", alias));
        initContainer.setName(alias);
        List<V1VolumeMount> mounts = generateInitContainerMount(initContainer, appType, alias, domainId, basePath, deployPath);
        initContainer.setVolumeMounts(mounts);
        String image = String.format("%s/ccod/%s:%s", this.nexusDockerUrl, appName.toLowerCase(), version.replaceAll("\\:", "-"));
        logger.debug(String.format("modify image of init container to %s", image));
        initContainer.setImage(image);
        V1Container runtimeContainer = deploy.getSpec().getTemplate().getSpec().getContainers().get(0);
        logger.debug(String.format("set container name to %s-runtime", alias));
        runtimeContainer.setName(String.format("%s-runtime", alias));
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
        }
        logger.info(String.format("generated deployment for %s : %s", gson.toJson(appBase), gson.toJson(deploy)));
        return deploy;
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
                        portVo.setTargetPort(portVo.getPort());
                        break;
                    case TOMCAT_WEB_APP:
                    case RESIN_WEB_APP:
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
        return new ArrayList<>(volumeMountMap.values());
    }

    private List<V1VolumeMount> generateInitContainerMount(V1Container initContainer, AppType appType, String alias, String domainId, String basePath, String deployPath) throws ParamException
    {
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
        volumeMap.get("ccod-runtime").getHostPath().setPath(String.format("/var/ccod-runtime/%s/%s", platformId, domainId));
        volumeMap.get("ccod-runtime").getHostPath().setType("DirectoryOrCreate");
        logger.debug(String.format("modify ccod-runtime volume to %s", gson.toJson(volumeMap.get("ccod-runtime"))));
        if(volumeMap.containsKey("core"))
        {
            volumeMap.get("core").getHostPath().setPath(String.format("/home/kubernetes/%s/core", platformId));
            volumeMap.get("core").getHostPath().setType("");
            logger.debug(String.format("modify core volume to %s", gson.toJson(volumeMap.get("core"))));
        }
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
        List<String> commands = new ArrayList<>();
        commands.add(0, "/bin/sh");
        commands.add(1, "-c");
        String basePath = appBase.getBasePath();
        String deployPath = getAbsolutePath(basePath, appBase.getDeployPath());
        String execParam = "";
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
        AppType appType = appBase.getAppType();
        String cwd = appType.equals(AppType.BINARY_FILE) ? deployPath:basePath;
        if(StringUtils.isNotBlank(appBase.getInitCmd()))
            execParam = String.format("%s;cd %s;%s", execParam, cwd, appBase.getInitCmd());
        execParam = String.format("%s;cd %s;%s", execParam, cwd, appBase.getStartCmd());
        if(StringUtils.isNotBlank(appBase.getLogOutputCmd()))
            execParam = String.format("%s;cd %s;%s", execParam, cwd, appBase.getLogOutputCmd());
        commands.add(execParam.replaceAll("^;", "").replaceAll(";;", ";"));
        logger.debug(String.format("command for %s at %s is : %s", alias, domainId, String.join(";", commands)));
        return commands;
    }

    private List<String> generateCmdForInitContainer(AppBase appBase, String packageFileName, List<AppFileNexusInfo> appCfgs, String domainId) throws ParamException
    {
        String alias = appBase.getAlias();
        List<String> commands = new ArrayList<>();
        commands.add(0, "/bin/sh");
        commands.add(1, "-c");
        AppType appType = appBase.getAppType();
        String appName = appBase.getAppName();
        String theName = packageFileName.replaceAll("\\.war$", "");
        String basePath = appType.equals(AppType.BINARY_FILE) ? "/binary-file" : "/opt";
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
            default:
                throw new ParamException(String.format("error appType %s", appType.name));
        }
        Map<String, List<AppFileNexusInfo>> deployPathCfgMap = appCfgs.stream().collect(Collectors.groupingBy(AppFileNexusInfo::getDeployPath));
        for (String cfgDeployPath : deployPathCfgMap.keySet()) {
            String absolutePath = getAbsolutePath(basePath, cfgDeployPath);
            switch (appType)
            {
                case BINARY_FILE:
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
            String[] arr = checkAt.split("/");
            String[] typeArr = arr[arr.length - 1].split("\\:");
            String checkType = typeArr[0];
            if(checkType.equals("HTTP") || checkType.equals("HTTPS"))
            {
                get = new V1HTTPGetAction();
                get.setPort(new IntOrString(Integer.parseInt(arr[0])));
                String subPath = typeArr.length==1 ? "" : arr[arr.length-1].replaceAll("^%s\\:", "");
                String path = String.format("/%s-%s/%s", alias, domainId, subPath).replaceAll("//", "/").replaceAll("/$", "");
                get.setPath(path);
                get.setScheme(checkType);
            }
            else if(checkType.equals("TCP"))
            {
                tcp = new V1TCPSocketAction();
                tcp.setPort(new IntOrString(Integer.parseInt(arr[0])));
            }
            else
            {
                exec = new V1ExecAction();
                exec.setCommand(Arrays.asList(checkAt.replaceAll(String.format("/%s$", arr[1]), "")));
            }
        }
        else
        {
            int targetPort = portList.get(0).getTargetPort();
            switch (appType) {
                case BINARY_FILE:
                    logger.debug(String.format("monitor port is %d/TCP", targetPort));
                    tcp = new V1TCPSocketAction();
                    tcp.setPort(new IntOrString(targetPort));
                    break;
                case TOMCAT_WEB_APP:
                case RESIN_WEB_APP:
                    logger.debug(String.format("monitor port is %d/HTTPGet", targetPort));
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
        if(initialDelaySeconds > 0)
            runtimeContainer.getLivenessProbe().setInitialDelaySeconds(initialDelaySeconds);
        if(periodSeconds > 0)
            runtimeContainer.getLivenessProbe().setPeriodSeconds(periodSeconds);
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
    public V1Deployment generateThreeAppDeployment(String ccodVersion, String appName, String alias, String version, String platformId, String hostUrl) throws ParamException {
        Map<String, String> selector = getK8sTemplateSelector(ccodVersion, appName, version, AppType.THREE_PART_APP, K8sKind.DEPLOYMENT);
        V1Deployment deploy = null;
        for(K8sObjectTemplatePo template : this.objectTemplateList)
        {
            if(isMatch(selector, template.getLabels()) && StringUtils.isNotBlank(template.getDeployJson()))
            {
                deploy = templateParseGson.fromJson(template.getDeployJson(), V1Deployment.class);
                break;
            }
        }
        if(deploy == null)
            throw new ParamException(String.format("can not match deployment for %s", gson.toJson(selector)));
        deploy.getMetadata().setNamespace(platformId);
        deploy.getMetadata().setLabels(new HashMap<>());
        deploy.getMetadata().getLabels().put(appName, alias);
        deploy.getMetadata().getLabels().put(this.appVersionLabel, version);
        deploy.getSpec().getSelector().setMatchLabels(new HashMap<>());
        deploy.getSpec().getSelector().getMatchLabels().put(appName, alias);
        deploy.getSpec().getTemplate().getMetadata().setLabels(new HashMap<>());
        deploy.getSpec().getTemplate().getMetadata().getLabels().put(appName, alias);
        deploy.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity()))
                .get("sql").getPersistentVolumeClaim().setClaimName(String.format("base-volume-%s", platformId));
        deploy.getSpec().getTemplate().getSpec().getContainers().get(0).getVolumeMounts().stream().filter(m->m.getName().equals("sql"))
                .forEach(m->{
                    if(appName.equals("oracle") || m.getMountPath().equals("/docker-entrypoint-initdb.d/")){
                        m.setSubPath(String.format("%s/base-volume/db/%s/sql", platformId, appName));
                    }
                    else{
                        m.setSubPath(String.format("%s/base-volume/db/%s/data", platformId, appName));
                    }
                });
        if(appName.equals("oracle"))
            deploy.getSpec().getTemplate().getSpec().getContainers().get(0).getArgs().set(0, String.format("/tmp/init.sh %s", hostUrl));
        logger.info(String.format("selected deployment for %s is %s", gson.toJson(selector), gson.toJson(deploy)));
        return deploy;
    }

    @Override
    public V1Namespace generateNamespace(String ccodVersion, String platformId, String platformName) throws ParamException {
        Map<String, String> selector = getK8sTemplateSelector(ccodVersion, null, null, null, K8sKind.NAMESPACE);
        V1Namespace ns = null;
        for(K8sObjectTemplatePo template : this.objectTemplateList)
        {
            if(isMatch(selector, template.getLabels()) && StringUtils.isNotBlank(template.getNamespaceJson()))
            {
                ns = templateParseGson.fromJson(template.getNamespaceJson(), V1Namespace.class);
                break;
            }
        }
        if(ns == null)
            throw new ParamException(String.format("can not match namespace template for select %s", gson.toJson(selector)));
        ns.getMetadata().setNamespace(platformId);
        ns.getMetadata().setName(platformId);
        Map<String, String> labels = new HashMap<>();
        labels.put(this.platformIdLabel, platformId);
        labels.put(this.platformNameLabel, DatatypeConverter.printHexBinary(platformName.getBytes()));
        labels.put(this.ccodVersionLabel, ccodVersion);
        ns.getMetadata().setLabels(labels);
        logger.info(String.format("selected namespace for selector %s is %s", gson.toJson(selector), gson.toJson(ns)));
        return ns;
    }

    @Override
    public V1Secret generateSecret(String ccodVersion, String platformId, String name) throws ParamException {
        V1Secret secret = (V1Secret) selectK8sObjectTemplate(ccodVersion, null, null, null, K8sKind.SECRET);
        secret.getMetadata().setName(name);
        secret.getMetadata().setNamespace(platformId);
        logger.info(String.format("generate secret is %s", gson.toJson(secret)));
        return secret;
    }

    @Override
    public V1PersistentVolume generatePersistentVolume(PlatformPo platform, String nfsServerIp) throws ParamException {
        String platformId = platform.getPlatformId();
        String ccodVersion = platform.getCcodVersion();
        V1PersistentVolume pv = (V1PersistentVolume)selectK8sObjectTemplate(ccodVersion, null, null, null, K8sKind.PV);
        String name = String.format("base-volume-%s", platformId);
        pv.getMetadata().setName(name);
        pv.getSpec().getClaimRef().setNamespace(platformId);
        pv.getSpec().getClaimRef().setName(name);
        pv.getSpec().getNfs().setPath("/home/kubernetes/volume");
        pv.getSpec().getNfs().setServer(nfsServerIp);
        pv.getSpec().setStorageClassName(name);
        logger.info(String.format("generate persistentVolume is %s", gson.toJson(pv)));
        return pv;
    }

    @Override
    public V1PersistentVolumeClaim generatePersistentVolumeClaim(String ccodVersion, String platformId) throws ParamException {
        V1PersistentVolumeClaim pvc = (V1PersistentVolumeClaim)selectK8sObjectTemplate(ccodVersion, null, null, null, K8sKind.PVC);
        pvc.getMetadata().setName(String.format("base-volume-%s", platformId));
        pvc.getMetadata().setNamespace(platformId);
        pvc.getSpec().setStorageClassName(String.format("base-volume-%s", platformId));
        pvc.getSpec().setVolumeName(String.format("base-volume-%s", platformId));
        logger.info(String.format("generate pvc is %s", gson.toJson(pvc)));
        return pvc;
    }

    private boolean isMatch(Map<String, String> selector, Map<String, String> labels)
    {
        if(selector == null || selector.size() == 0 || labels == null || labels.size() == 0)
            return  false;
        if(selector.size() > labels.size())
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
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(savePath)),
                "UTF-8"));
        String lineTxt = br.readLine();
        List<K8sObjectTemplatePo> list = this.gson.fromJson(lineTxt, new TypeToken<List<K8sObjectTemplatePo>>() {
        }.getType());
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
    public String preCheckCCODDomainApps(String domainId, List<AppUpdateOperationInfo> optList, String platformId, String k8sApiUrl, String k8sAuthToken) throws ApiException {
        return null;
    }

    @Override
    public V1Job generatePlatformInitJob(PlatformPo platform) throws ParamException {
        String platformId = platform.getPlatformId();
        String ccodVersion = platform.getCcodVersion();
        String baseDataNexusPath = (String)platform.getParams().get(PlatformBase.baseDataNexusPathKey);
        String platformBaseDataRepository = (String)platform.getParams().get(PlatformBase.baseDataNexusRepositoryKey);
        V1Job job = (V1Job) selectK8sObjectTemplate(ccodVersion, null, null, null, K8sKind.JOB);
        String fileName = baseDataNexusPath.replaceAll("^.*/", "");
        job.getMetadata().setNamespace(platformId);
        String workDir = String.format("/root/data/%s/base-volume", platformId);
        String arg = String.format("mkdir %s -p;cd %s;wget %s/repository/%s/%s;tar -xvzf %s",
                workDir, workDir, nexusHostUrl, platformBaseDataRepository, baseDataNexusPath, fileName);
        job.getSpec().getTemplate().getSpec().getContainers().get(0).setArgs(Arrays.asList(arg));
        job.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity()))
                .get("data").getPersistentVolumeClaim().setClaimName(String.format("base-volume-%s", platformId));
        logger.warn(String.format("arg=%s", arg));
        logger.warn(String.format("workDir=%s", workDir));
        logger.warn(String.format("fileName=%s", fileName));
        logger.warn(String.format("path=%s", baseDataNexusPath));
        logger.warn(String.format("job=%s", gson.toJson(job)));
        return job;
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
        ExtensionsV1beta1Ingress ingress = null;
        if(appType.equals(AppType.RESIN_WEB_APP) || appType.equals(AppType.TOMCAT_WEB_APP))
            ingress = this.ik8sApiService.readNamespacedIngress(String.format("%s-%s", alias, domainId), platformId, k8sApiUrl, k8sAuthToken);
        K8sCCODDomainAppVo appVo = new K8sCCODDomainAppVo(alias, module, domainId, configMap, deploys.get(0), services, ingress);
        return appVo;
    }

    @Override
    public K8sCCODDomainAppVo generateNewCCODDomainApp(AppBase appBase, String domainId, List<AppFileNexusInfo> domainCfg, PlatformPo platform) throws ParamException, InterfaceCallException, IOException{
        String appName = appBase.getAppName();
        String version = appBase.getVersion();
        String alias = appBase.getAlias();
        String tag = String.format("%s[%s(%s)]", alias, appName, version);
        logger.debug(String.format("generate k8s object for %s, domainId=%s", gson.toJson(appBase), domainId));
        AppModuleVo module = this.appManagerService.queryAppByVersion(appName, version, true);
        String ccodVersion = module.getCcodVersion();
        AppType appType = module.getAppType();
        String platformId = platform.getPlatformId();
        logger.debug(String.format("generate configMap for %s : cfg=%s", tag, gson.toJson(module.getCfgs())));
        V1ConfigMap configMap = this.ik8sApiService.getConfigMapFromNexus(platformId, String.format("%s-%s", alias, domainId),
                appName, appBase.getCfgs().stream().map(cfg->cfg.getNexusAssetInfo(nexusHostUrl)).collect(Collectors.toList()),
                this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
        V1Deployment deploy = this.generateCCODDomainAppDeployment(appBase, domainId, domainCfg, platform);
        List<V1Service> services = new ArrayList<>();
        V1Service service = this.generateCCODDomainAppService(ccodVersion, appType, appName, alias, ServicePortType.ClusterIP, appBase.getPorts(), platformId, domainId);
        services.add(service);
        if(StringUtils.isNotBlank(appBase.getNodePorts())) {
            service = this.generateCCODDomainAppService(ccodVersion, appType, appName, alias, ServicePortType.NodePort, appBase.getNodePorts(), platformId, domainId);
            services.add(service);
        }
        ExtensionsV1beta1Ingress ingress = null;
        if(appType.equals(AppType.TOMCAT_WEB_APP) || appType.equals(AppType.RESIN_WEB_APP))
            ingress = this.generateIngress(ccodVersion, appType, appName, alias, platformId, domainId, platform.getHostUrl());
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
        if(app.getIngress() != null)
        {
            step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.INGRESS, name, K8sOperation.DELETE, app.getIngress());
            steps.add(step);
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

    @Override
    public List<K8sOperationInfo> generateAddPlatformAppSteps(String jobId, AppBase appBase, String domainId, List<AppFileNexusInfo> domainCfg, PlatformPo platform, boolean isNewPlatform) throws ParamException, ApiException, InterfaceCallException, IOException {
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
        AppType appType = module.getAppType();
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
        K8sCCODDomainAppVo app = generateNewCCODDomainApp(appBase, domainId, domainCfg, platform);
        List<K8sOperationInfo> steps = new ArrayList<>();
        K8sOperationInfo step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP,
                app.getConfigMap().getMetadata().getName(), K8sOperation.CREATE, app.getConfigMap());
        steps.add(step);
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.DEPLOYMENT,
                app.getDeploy().getMetadata().getName(), K8sOperation.CREATE, app.getDeploy());
        if(module.isKernal()) {
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
        if(app.getIngress() != null)
        {
            step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.INGRESS,
                    app.getIngress().getMetadata().getName(), K8sOperation.CREATE, app.getIngress());
            steps.add(step);
        }
        return steps;
    }

    @Override
    public List<K8sOperationInfo> generateUpdatePlatformAppSteps(String jobId, AppBase appBase, String domainId, List<AppFileNexusInfo> domainCfg, PlatformPo platform) throws ParamException, ApiException, InterfaceCallException, IOException {
        String alias = appBase.getAlias();
        String appName = appBase.getAppName();
        String version = appBase.getVersion();
        logger.debug(String.format("generate update step for %s at %s", alias, domainId));
        String platformId = platform.getPlatformId();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        K8sCCODDomainAppVo oriApp= getCCODDomainApp(appName, alias, version, domainId, platformId, k8sApiUrl, k8sAuthToken);
        K8sCCODDomainAppVo updateApp = generateNewCCODDomainApp(appBase, domainId, domainCfg, platform);
        List<K8sOperationInfo> steps = new ArrayList<>();
        K8sOperationInfo step;
        for(V1Service service : updateApp.getServices())
        {
            String portKind = service.getSpec().getType();
            List<V1Service> oriServices = oriApp.getServices().stream().filter(svc->svc.getSpec().getType().equals(portKind)).collect(Collectors.toList());
            boolean isChanged = this.ik8sApiService.isServicePortChanged(portKind, service, oriServices);
            if(isChanged)
            {
                for(V1Service svc : oriServices)
                {
                    step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE,
                            svc.getMetadata().getName(), K8sOperation.DELETE, svc);
                    steps.add(step);
                }
            }
        }
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.DEPLOYMENT,
                updateApp.getConfigMap().getMetadata().getName(), K8sOperation.DELETE, oriApp.getDeploy());
        steps.add(step);
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP,
                updateApp.getConfigMap().getMetadata().getName(), K8sOperation.DELETE, oriApp.getConfigMap());
        steps.add(step);
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.CONFIGMAP,
                updateApp.getConfigMap().getMetadata().getName(), K8sOperation.CREATE, updateApp.getConfigMap());
        steps.add(step);
        step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.DEPLOYMENT,
                updateApp.getConfigMap().getMetadata().getName(), K8sOperation.CREATE, updateApp.getDeploy());
        steps.add(step);
        for(V1Service service : updateApp.getServices())
        {
            String portKind = service.getSpec().getType();
            List<V1Service> oriServices = oriApp.getServices().stream().filter(svc->svc.getSpec().getType().equals(portKind)).collect(Collectors.toList());
            boolean isChanged = this.ik8sApiService.isServicePortChanged(portKind, service, oriServices);
            if(isChanged)
            {
                step = new K8sOperationInfo(jobId, platformId, domainId, K8sKind.SERVICE,
                        service.getMetadata().getName(), K8sOperation.CREATE, service);
                steps.add(step);
            }
        }
        logger.debug(String.format("update %s at domain %s steps are %s", alias, domainId, gson.toJson(steps)));
        return steps;
    }

    @Override
    public List<K8sOperationInfo> generateDebugPlatformAppSteps(String jobId, AppBase appBase, String domainId, List<AppFileNexusInfo> domainCfg, PlatformPo platform) throws ParamException, ApiException, InterfaceCallException, IOException {
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
        List<K8sOperationInfo> addSteps = generateAddPlatformAppSteps(jobId, appBase, domainId, domainCfg, platform, true);
        addSteps.forEach(v->{
            if (v.getKind().equals(K8sKind.DEPLOYMENT) && v.getOperation().equals(K8sOperation.CREATE)) {
                v.setKernal(true);v.setTimeout(debugTimeout);
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

    @Override
    public Map<String, String> getK8sTemplateSelector(String ccodVersion, String appName, String vesion, AppType appType, K8sKind kind)
    {
        Map<String, String> selector = new HashMap<>();
        selector.put(this.ccodVersionLabel, ccodVersion);
        switch (kind)
        {
            case POD:
            case CONFIGMAP:
            case DEPLOYMENT:
            case SERVICE:
            case INGRESS:
            case ENDPOINTS:
                selector.put(this.appTypeLabel, appType.name);
                switch (appType)
                {
                    case THREE_PART_APP:
                      selector.put(this.appNameLabel, appName);
                      break;
                    default:
                        break;
                }
                break;
            case JOB:
            case SECRET:
            case NAMESPACE:
            case PV:
            case PVC:
                break;
            default:
                break;
        }
        return selector;
    }

    @Override
    public List<K8sOperationInfo> generatePlatformCreateSteps(
            String jobId, V1Job job, V1Namespace namespace, List<V1Secret> secrets,
            V1PersistentVolume pv, V1PersistentVolumeClaim pvc, List<K8sThreePartAppVo> threePartApps,
            List<K8sThreePartServiceVo> threePartServices, String nfsServerIp, PlatformPo platform) throws ApiException, ParamException, IOException, InterfaceCallException {
        String platformId = platform.getPlatformId();
        String platformName = platform.getPlatformName();
        String ccodVersion = platform.getCcodVersion();
        String hostUrl = platform.getHostUrl();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        if(this.ik8sApiService.isNamespaceExist(platformId, k8sApiUrl, k8sAuthToken))
            throw new ParamException(String.format("namespace %s has exist at %s", platformId, k8sApiUrl));
        List<K8sOperationInfo> steps = new ArrayList<>();
        if(namespace == null)
            namespace = generateNamespace(ccodVersion, platformId, platformName);
        if(!namespace.getMetadata().getName().equals(platformId))
            throw new ParamException(String.format("name of namespace should be %s not %s", platformId, namespace.getMetadata().getName()));
        K8sOperationInfo step = new K8sOperationInfo(jobId, platformId, null, K8sKind.NAMESPACE, platformId, K8sOperation.CREATE, namespace);
        steps.add(step);
        if(secrets == null)
            secrets = new ArrayList<>();
        Map<String, List<V1ObjectMeta>> metaMap = secrets.stream().map(se->se.getMetadata()).collect(Collectors.toList())
                .stream().collect(Collectors.groupingBy(V1ObjectMeta::getName));
        for(String name : metaMap.keySet())
        {
            if(metaMap.get(name).size() > 1)
                throw new ParamException(String.format("secret %s multi define", name));
        }
        if(!metaMap.containsKey("ssl"))
        {
            V1Secret sslCert = this.ik8sApiService.generateNamespacedSSLCert(platformId, k8sApiUrl, k8sAuthToken);
            secrets.add(sslCert);
        }
        for(V1Secret secret : secrets)
        {
            step = new K8sOperationInfo(jobId, platformId, null, K8sKind.SECRET, secret.getMetadata().getName(), K8sOperation.CREATE, secret);
            steps.add(step);
        }
        if(pv == null)
            pv = generatePersistentVolume(platform, nfsServerIp);
        step = new K8sOperationInfo(jobId, platformId, null, K8sKind.PV, pv.getMetadata().getName(), K8sOperation.CREATE, pv);
        steps.add(step);
        if(pvc == null)
            pvc = generatePersistentVolumeClaim(ccodVersion, platformId);
        step = new K8sOperationInfo(jobId, platformId, null, K8sKind.PVC, pvc.getMetadata().getName(), K8sOperation.CREATE, pvc);
        steps.add(step);
        job = job == null ? generatePlatformInitJob(platform) : job;
        if(job != null)
        {
            if(!job.getMetadata().getNamespace().equals(platformId))
                throw new ParamException(String.format("namespace of job should be %s not %s", platformId, namespace.getMetadata().getNamespace()));
            step = new K8sOperationInfo(jobId, platformId, null, K8sKind.JOB, job.getMetadata().getName(), K8sOperation.CREATE, job);
            steps.add(step);
        }
        V1ConfigMap configMap = this.ik8sApiService.getConfigMapFromNexus(platformId, platformId, platformId,
                platform.getCfgs().stream().map(cfg->cfg.getNexusAssetInfo(this.nexusHostUrl)).collect(Collectors.toList()),
                this.nexusHostUrl, this.nexusUserName, this.nexusPassword);
        K8sOperationInfo k8sOpt = new K8sOperationInfo(jobId, platformId, null, K8sKind.CONFIGMAP,
                platformId, K8sOperation.CREATE, configMap);
        steps.add(k8sOpt);
        if(threePartApps == null)
            threePartApps = new ArrayList<>();
        if(threePartApps.size() == 0)
        {
            V1Deployment oraDep = generateThreeAppDeployment(ccodVersion,"oracle", "oracle", "32-xe-10g-1.0", platformId, hostUrl);
            V1Service oraSvc = generateThreeAppService(ccodVersion,"oracle", "oracle", "32-xe-10g-1.0", platformId);
            K8sThreePartAppVo oracle = new K8sThreePartAppVo("oracle", "oracle", "32-xe-10g-1.0", oraDep, Arrays.asList(oraSvc), new ArrayList<>());
            threePartApps.add(oracle);
            V1Deployment mysqlDep = generateThreeAppDeployment(ccodVersion,"mysql", "mysql", "5.7", platformId, hostUrl);
            V1Service mysqlSvc = generateThreeAppService(ccodVersion, "mysql", "mysql", "5.7", platformId);
            K8sThreePartAppVo mysql = new K8sThreePartAppVo("mysql", "mysql", "5.7", mysqlDep, Arrays.asList(mysqlSvc), new ArrayList<>());
            threePartApps.add(mysql);
        }
        metaMap = threePartApps.stream().map(app->app.getDeploy().getMetadata()).collect(Collectors.toList())
                .stream().collect(Collectors.groupingBy(V1ObjectMeta::getName));
        for(String name : metaMap.keySet())
        {
            if(metaMap.get(name).size() > 1)
                throw new ParamException(String.format("deployment %s for three part app multi define", name));
        }
        metaMap = threePartApps.stream().flatMap(app->app.getServices().stream()).collect(Collectors.toList())
                .stream().map(svc->svc.getMetadata()).collect(Collectors.toList())
                .stream().collect(Collectors.groupingBy(V1ObjectMeta::getName));
        for(String name : metaMap.keySet())
        {
            if(metaMap.get(name).size() > 1)
                throw new ParamException(String.format("service %s for three part app multi define", name));
        }
        Map<String, List<V1ObjectMeta>> ingMetaMap = threePartApps.stream().filter(app->app.getIngresses() != null&&app.getIngresses().size() != 0).collect(Collectors.toList())
                .stream().flatMap(app->app.getIngresses().stream()).collect(Collectors.toList())
                .stream().map(ing->ing.getMetadata()).collect(Collectors.toList())
                .stream().collect(Collectors.groupingBy(V1ObjectMeta::getName));
        for(String name : ingMetaMap.keySet())
        {
            if(ingMetaMap.get(name).size() > 1)
                throw new ParamException(String.format("ingress %s for three part app multi define", name));
            else if(!metaMap.containsKey(name))
                throw new ParamException(String.format("can not find service for three part app ingress %s", name));
        }
        for(K8sThreePartAppVo threeApp : threePartApps)
        {
            if(!threeApp.getDeploy().getMetadata().getNamespace().equals(platformId))
                throw new ParamException(String.format("namespace of three part app deployment %s should be %s not %s",
                        threeApp.getDeploy().getMetadata().getName(), platformId, threeApp.getDeploy().getMetadata().getNamespace()));
            step = new K8sOperationInfo(jobId, platformId, null, K8sKind.DEPLOYMENT, threeApp.getDeploy().getMetadata().getName(), K8sOperation.CREATE, threeApp.getDeploy());
            step.setKernal(true);
            step.setTimeout(150);
            steps.add(step);
            for(V1Service svc : threeApp.getServices())
            {
                if(!svc.getMetadata().getNamespace().equals(platformId))
                    throw new ParamException(String.format("namespace of three part app service %s should be %s not %s",
                            svc.getMetadata().getName(), platformId, svc.getMetadata().getNamespace()));
                step = new K8sOperationInfo(jobId, platformId, null, K8sKind.SERVICE, svc.getMetadata().getName(), K8sOperation.CREATE, svc);
                steps.add(step);
            }
            if(threeApp.getIngresses() != null && threeApp.getIngresses().size() > 0)
            {
                for(ExtensionsV1beta1Ingress ingress : threeApp.getIngresses())
                {
                    if(!ingress.getMetadata().getNamespace().equals(platformId))
                        throw new ParamException(String.format("namespace of three part app ingress %s should be %s not %s",
                                ingress.getMetadata().getName(), platformId, ingress.getMetadata().getNamespace()));
                    step = new K8sOperationInfo(jobId, platformId, null, K8sKind.INGRESS, ingress.getMetadata().getName(), K8sOperation.CREATE, ingress);
                    steps.add(step);
                }
            }
        }
        if(threePartServices == null || threePartServices.size() == 0)
            threePartServices = this.generateTestThreePartServices(ccodVersion, platformId);
        metaMap = threePartServices.stream().map(threeSvc->threeSvc.getEndpoints().getMetadata()).collect(Collectors.toList())
                .stream().collect(Collectors.groupingBy(V1ObjectMeta::getName));
        for(String name : metaMap.keySet())
        {
            if(metaMap.get(name).size() > 1)
                throw new ParamException(String.format("endpoints %s of three part service multi define", name));
        }
        metaMap = threePartServices.stream().map(threeSvc->threeSvc.getService().getMetadata()).collect(Collectors.toList())
                .stream().collect(Collectors.groupingBy(V1ObjectMeta::getName));
        for(String name : metaMap.keySet())
        {
            if(metaMap.get(name).size() > 1)
                throw new ParamException(String.format("service %s of three part service multi define", name));
        }
        for(K8sThreePartServiceVo threePartSvc : threePartServices)
        {
            V1Endpoints endpoints = threePartSvc.getEndpoints();
            if(!endpoints.getMetadata().getNamespace().equals(platformId))
                throw new ParamException(String.format("namespace of endpoints %s is %s not %s",
                        endpoints.getMetadata().getName(), endpoints.getMetadata().getNamespace(), platformId));
            step = new K8sOperationInfo(jobId, platformId, null, K8sKind.ENDPOINTS, endpoints.getMetadata().getName(), K8sOperation.CREATE, endpoints);
            steps.add(step);
            V1Service service = threePartSvc.getService();
            if(!service.getMetadata().getNamespace().equals(platformId))
                throw new ParamException(String.format("namespace of service %s is %s not %s",
                        service.getMetadata().getName(), service.getMetadata().getNamespace(), platformId));
            step = new K8sOperationInfo(jobId, platformId, null, K8sKind.SERVICE, service.getMetadata().getName(), K8sOperation.CREATE, service);
            steps.add(step);
        }
        return steps;
    }

    @Override
    public List<K8sThreePartServiceVo> generateTestThreePartServices(String ccodVersion, String platformId) throws ApiException, ParamException {
        for(K8sThreePartServiceVo threePartService : this.testThreePartServices)
        {
            threePartService.getService().getMetadata().setLabels(new HashMap<>());
            threePartService.getService().getMetadata().setNamespace(platformId);
            threePartService.getService().getMetadata().getLabels().put(this.serviceTypeLabel, K8sServiceType.THREE_PART_SERVICE.name);
            threePartService.getService().getMetadata().getLabels().put(this.appNameLabel, "umg");
            threePartService.getEndpoints().getMetadata().setLabels(new HashMap<>());
            threePartService.getEndpoints().getMetadata().setNamespace(platformId);
            threePartService.getEndpoints().getMetadata().getLabels().put(this.serviceTypeLabel, K8sServiceType.THREE_PART_SERVICE.name);
            threePartService.getEndpoints().getMetadata().getLabels().put(this.appNameLabel, "umg");
        }
        return testThreePartServices;
    }

    private List<K8sThreePartServiceVo> getThreePartServices(String platformId, String k8sApiUrl, String k8sAuthToken) throws ApiException
    {
        List<K8sThreePartServiceVo> list = new ArrayList<>();
        String name = "umg41";
        V1Endpoints endpoints = this.ik8sApiService.readNamespacedEndpoints(name, platformId, k8sApiUrl, k8sAuthToken);
        V1Service service = this.ik8sApiService.readNamespacedService(name, platformId, k8sApiUrl, k8sAuthToken);
        K8sThreePartServiceVo threeSvc = new K8sThreePartServiceVo(name, service, endpoints);
        threeSvc = this.templateParseGson.fromJson(this.templateParseGson.toJson(threeSvc), K8sThreePartServiceVo.class);
        list.add(threeSvc);
        name = "umg141";
        endpoints = this.ik8sApiService.readNamespacedEndpoints(name, platformId, k8sApiUrl, k8sAuthToken);
        service = this.ik8sApiService.readNamespacedService(name, platformId, k8sApiUrl, k8sAuthToken);
        threeSvc = new K8sThreePartServiceVo(name, service, endpoints);
        threeSvc = this.templateParseGson.fromJson(this.templateParseGson.toJson(threeSvc), K8sThreePartServiceVo.class);
        list.add(threeSvc);
        name = "umg147";
        endpoints = this.ik8sApiService.readNamespacedEndpoints(name, platformId, k8sApiUrl, k8sAuthToken);
        service = this.ik8sApiService.readNamespacedService(name, platformId, k8sApiUrl, k8sAuthToken);
        threeSvc = new K8sThreePartServiceVo(name, service, endpoints);
        threeSvc = this.templateParseGson.fromJson(this.templateParseGson.toJson(threeSvc), K8sThreePartServiceVo.class);
        list.add(threeSvc);
        return list;
    }

    private Object selectK8sObjectTemplate(String ccodVersion, AppType appType, String appName, String version, K8sKind kind) throws ParamException
    {
        if(StringUtils.isBlank(ccodVersion))
            throw new ParamException("ccodVersion can not be empty for select k8s object template");
        else if(appType == null && StringUtils.isNotBlank(appName))
            throw new ParamException(String.format("appType of %s can not be empty for select k8s object template", appName));
        else if(StringUtils.isBlank(appName) && StringUtils.isNotBlank(version))
            throw new ParamException(String.format("appName which has version %s can not be empty for select k8s object template", version));
        if(kind == null)
            throw new ParamException("kind of select k8s object can not be null");
        String errMsg = String.format("can not select %s template for ccodVersion=%s,appType=%s,appName=%s and version=%s",
                kind.name, ccodVersion, appType==null ? "":appType.name, appName, version);
        Map<String, String> selector = new HashMap<>();
        selector.put(this.ccodVersionLabel, ccodVersion);
        if(appType != null)
            selector.put(this.appTypeLabel, appType.name);
        if(StringUtils.isNotBlank(appName))
            selector.put(this.appNameLabel, appName);
        if(StringUtils.isNotBlank(version))
            selector.put(this.appVersionLabel, version);
        Object template = selectK8sObjectTemplate(selector, kind);
        if(template != null){
            logger.info(String.format("%s template for %s is : %s", kind.name, gson.toJson(selector), gson.toJson(template)));
            return template;
        }
        logger.debug(String.format("not select %s template for %s", kind.name, gson.toJson(selector)));
        if(selector.size() == 1)
            throw new ParamException(errMsg);
        if(StringUtils.isNotBlank(version))
            selector.remove(this.appVersionLabel);
        else if(StringUtils.isNotBlank(appName))
            selector.remove(this.appNameLabel);
        else
            selector.remove(this.appTypeLabel);
        template = selectK8sObjectTemplate(selector, kind);
        if(template != null){
            logger.info(String.format("%s template for %s is : %s", kind.name, gson.toJson(selector), gson.toJson(template)));
            return template;
        }
        logger.debug(String.format("not select %s template for %s", kind.name, gson.toJson(selector)));
        if(selector.size() == 1)
            throw new ParamException(errMsg);
        if(StringUtils.isNotBlank(appName))
            selector.remove(this.appNameLabel);
        else
            selector.remove(this.appTypeLabel);
        template = selectK8sObjectTemplate(selector, kind);
        if(template != null){
            logger.info(String.format("%s template for %s is : %s", kind.name, gson.toJson(selector), gson.toJson(template)));
            return template;
        }
        logger.debug(String.format("not select %s template for %s", kind.name, gson.toJson(selector)));
        if(selector.size() == 1)
            throw new ParamException(errMsg);
        selector.remove(this.appTypeLabel);
        template = selectK8sObjectTemplate(selector, kind);
        if(template != null){
            logger.info(String.format("%s template for %s is : %s", kind.name, gson.toJson(selector), gson.toJson(template)));
            return template;
        }
        throw new ParamException(errMsg);
    }

    private Object selectK8sObjectTemplate(Map<String, String> selector, K8sKind kind)
    {
        for(K8sObjectTemplatePo templatePo : this.objectTemplateList)
        {
            if(isMatch(selector, templatePo.getLabels()))
            {
                switch (kind)
                {
                    case ENDPOINTS:
                        if(StringUtils.isNotBlank(templatePo.getEndpointsJson()))
                            return gson.fromJson(templatePo.getEndpointsJson(), V1Endpoints.class);
                        break;
                    case INGRESS:
                        if(StringUtils.isNotBlank(templatePo.getIngressJson()))
                            return gson.fromJson(templatePo.getIngressJson(), ExtensionsV1beta1Ingress.class);
                        break;
                    case SERVICE:
                        if(StringUtils.isNotBlank(templatePo.getServiceJson()))
                            return gson.fromJson(templatePo.getServiceJson(), V1Service.class);
                        break;
                    case DEPLOYMENT:
                        if(StringUtils.isNotBlank(templatePo.getDeployJson()))
                            return gson.fromJson(templatePo.getDeployJson(), V1Deployment.class);
                        break;
                    case PVC:
                        if(StringUtils.isNotBlank(templatePo.getPersistentVolumeClaimJson()))
                            return gson.fromJson(templatePo.getPersistentVolumeClaimJson(), V1PersistentVolumeClaim.class);
                        break;
                    case PV:
                        if(StringUtils.isNotBlank(templatePo.getPersistentVolumeJson()))
                            return gson.fromJson(templatePo.getPersistentVolumeJson(), V1PersistentVolume.class);
                        break;
                    case NAMESPACE:
                        if(StringUtils.isNotBlank(templatePo.getNamespaceJson()))
                            return gson.fromJson(templatePo.getNamespaceJson(), V1Namespace.class);
                        break;
                    case JOB:
                        if(StringUtils.isNotBlank(templatePo.getJobJson()))
                            return gson.fromJson(templatePo.getJobJson(), V1Job.class);
                        break;
                    case SECRET:
                        if(StringUtils.isNotBlank(templatePo.getSecretJson()))
                            return gson.fromJson(templatePo.getSecretJson(), V1Secret.class);
                        break;
                }
            }
        }
        return null;
    }

    private String getPortString(List<V1ServicePort> ports, String serviceType)
    {
        return ports.stream().map(p->serviceType.equals("ClusterIP")?String.format("%s/%s", p.getPort(), p.getProtocol()) : String.format("%s:%s/%s", p.getPort(), p.getNodePort(), p.getProtocol()))
                .collect(Collectors.joining(","));
    }

    private PlatformAppDeployDetailVo getAppDetailFromK8sObj(AppModuleVo module, String alias, V1Deployment deploy, List<V1Service> services, V1ConfigMap configMap) throws IOException, InterfaceCallException, NexusException
    {
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
        V1Container init = deploy.getSpec().getTemplate().getSpec().getInitContainers().stream()
                .collect(Collectors.toMap(V1Container::getName, Function.identity())).get(alias);
        V1Container run = deploy.getSpec().getTemplate().getSpec().getContainers().stream()
                .collect(Collectors.toMap(V1Container::getName, Function.identity())).get(String.format("%s-runtime", alias));
        detail.setAssembleTag(deploy.getMetadata().getName());
        detail.setPlatformId(deploy.getMetadata().getNamespace());
        detail.setAlias(alias);
        String domainId = labels.get(this.domainIdLabel);
        detail.setDomainId(domainId);
        List<String> cmd = run.getCommand();
        String command = cmd.get(2).replaceAll(";$", "").replaceAll("\\s+;", "");
        String cmdTag = "resin.sh";
        if(module.getAppType().equals(AppType.TOMCAT_WEB_APP))
            cmdTag = "startup.sh";
        else if(module.getAppType().equals(AppType.BINARY_FILE))
            cmdTag = module.getInstallPackage().getFileName();
        Pattern pattern = Pattern.compile(String.format(startCmdRegex, cmdTag));
        Matcher matcher = pattern.matcher(command);
        if(!matcher.find()){
            detail.setStartCmd(command);
            detail.setInitCmd(null);
            detail.setLogOutputCmd(null);
        }
        else{
            String startCmd = matcher.group().replace(";", "");
            int index = command.indexOf(startCmd);
            String initCmd = index == 0 ? null : command.substring(0, index - 1).replaceAll(";$", "");
            String logOutputCmd = index == command.length() - 1 ? null : command.substring(index + startCmd.length());
            detail.setStartCmd(startCmd);
            detail.setInitCmd(initCmd);
            detail.setLogOutputCmd(logOutputCmd);
            String tag = cmdTag;
            List<String> cds = Arrays.stream(command.split(";")).filter(s->s.matches("^cd .*")).collect(Collectors.toList());
            if(cds.size() > 0){
                detail.setBasePath(cds.get(cds.size()-1).replaceAll("^cd ", "").replace(";", ""));
                Arrays.stream(startCmd.split("\\s+")).filter(s->s.indexOf(tag)>=0)
                        .forEach(s->detail.setDeployPath(s.replaceAll(String.format("", tag), "")));
            }
        }
        if(run.getLivenessProbe() != null){
            detail.setInitialDelaySeconds(run.getLivenessProbe().getInitialDelaySeconds());
            detail.setPeriodSeconds(run.getLivenessProbe().getPeriodSeconds());
            if(run.getLivenessProbe().getHttpGet() != null){
                detail.setCheckAt(String.format("%d/%s", run.getLivenessProbe().getHttpGet().getPort().getIntValue(), run.getLivenessProbe().getHttpGet().getScheme()));
            }
            else if(run.getLivenessProbe().getTcpSocket() != null){
                detail.setCheckAt(String.format("%d/TCP", run.getLivenessProbe().getTcpSocket().getPort().getIntValue()));
            }
            else if(run.getLivenessProbe().getExec() != null){
                detail.setCheckAt(String.format("%s/CMD", run.getLivenessProbe().getExec().getCommand()));
            }
        }
        cmd = init.getCommand();
        command = cmd.get(2);
        detail.setEnvLoadCmd(command);
        String volume = module.getAppType().equals(AppType.BINARY_FILE) ? "binary-file" : "war";
        String mountPath = run.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, v->v.getMountPath())).get(volume);
        List<AppFileNexusInfo> cfgs = restoreConfigFileFromConfigMap(configMap, command, detail.getBasePath(), volume, mountPath);
        detail.setCfgs(cfgs);
        detail.fill(module);
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
    public List<PlatformAppDeployDetailVo> getPlatformAppDetailFromK8s(PlatformPo platform) throws ApiException, ParamException, InterfaceCallException, NexusException, IOException
    {
        String platformId = platform.getPlatformId();
        String k8sApiUrl = platform.getK8sApiUrl();
        String k8sAuthToken = platform.getK8sAuthToken();
        List<V1Deployment> deployments = this.ik8sApiService.listNamespacedDeployment(platformId, k8sApiUrl, k8sAuthToken);
        List<V1Service> services = this.ik8sApiService.listNamespacedService(platformId, k8sApiUrl, k8sAuthToken);
        Map<String, V1ConfigMap> configMapMap = this.ik8sApiService.listNamespacedConfigMap(platformId, k8sApiUrl, k8sAuthToken)
                .stream().collect(Collectors.toMap(s->s.getMetadata().getName(), v->v));
        List<PlatformAppDeployDetailVo> details = new ArrayList<>();
        for(V1Deployment deployment : deployments){
            boolean isDomainApp = isCCODDomainAppDeployment(deployment);
            logger.debug(String.format("deployment %s is ccod domain app deployment : %b", deployment.getMetadata().getName(), isDomainApp));
            if(!isDomainApp){
                continue;
            }
            String domainId = deployment.getMetadata().getLabels().get(this.domainIdLabel);
            for(V1Container init : deployment.getSpec().getTemplate().getSpec().getInitContainers()){
                V1ConfigMap configMap = configMapMap.get(String.format("%s-%s", init.getName(), domainId));
                if(configMap == null){
                    logger.error(String.format("can not find configMap %s-%s", init.getName(), domainId));
                    continue;
                }
                AppModuleVo module = appManagerService.getRegisteredCCODAppFromImageUrl(init.getImage());
                List<V1Service> relativeSvcs = services.stream().filter(s->isMatch(s.getSpec().getSelector(), deployment.getSpec().getTemplate().getMetadata().getLabels()))
                        .collect(Collectors.toList());
                PlatformAppDeployDetailVo detail = this.getAppDetailFromK8sObj(module, init.getName(), deployment, relativeSvcs, configMap);
                details.add(detail);
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
        Map<String, String> labels = deployment.getMetadata().getLabels();
        if(labels == null || labels.size() == 0)
            return false;
        if(!labels.containsKey(this.domainIdLabel))
            return  false;
        else if(!labels.containsKey(this.appTypeLabel)
                || (!labels.get(this.appTypeLabel).equals(AppType.BINARY_FILE.name) && !labels.get(this.appTypeLabel).equals(AppType.RESIN_WEB_APP.name) && !labels.get(this.appTypeLabel).equals(AppType.TOMCAT_WEB_APP.name)))
            return false;
        String deployName = deployment.getMetadata().getName();
        String domainId = labels.get(this.domainIdLabel);
        List<V1Container> initList = deployment.getSpec().getTemplate().getSpec().getInitContainers() == null ? new ArrayList<>() : deployment.getSpec().getTemplate().getSpec().getInitContainers();
        List<V1Container> runtimeList = deployment.getSpec().getTemplate().getSpec().getInitContainers() == null ? new ArrayList<>() : deployment.getSpec().getTemplate().getSpec().getContainers();
        if(initList.size() != runtimeList.size()){
            logger.error(String.format("%s init container count not equal runtime count", deployName));
            return false;
        }
        if(deployment.getSpec().getTemplate().getSpec().getVolumes() == null || deployment.getSpec().getTemplate().getSpec().getVolumes().size() == 0){
            logger.error(String.format("deployment %s has not any volume", deployName));
            return false;
        }
        Map<String, V1Volume> volumeMap = deployment.getSpec().getTemplate().getSpec().getVolumes().stream()
                .collect(Collectors.toMap(k->k.getName(), v->v));
        try{
            Map<String, V1Container> runtimeMap = runtimeList.stream().collect(Collectors.toMap(c->c.getName(), v->v));
            for(V1Container init : initList){
                AppModuleVo module = appManagerService.getRegisteredCCODAppFromImageUrl(init.getImage());
                V1Container runtime = runtimeMap.get(String.format("%s-runtime", init.getName()));
                if(runtime == null){
                    logger.error(String.format("can not find container %s-runtime at deployment %s", runtime.getName(), deployment.getMetadata().getName()));
                    return false;
                }
                if(init.getCommand() == null || init.getCommand().size() != 3 || !init.getCommand().get(0).equals("/bin/sh") || !init.getCommand().get(1).equals("-c")){
                    logger.error(String.format("deployment %s %s container command is not wanted", deployName, init.getName()));
                    return false;
                }
                if(runtime.getCommand() == null || runtime.getCommand().size() != 3 || !runtime.getCommand().get(0).equals("/bin/sh") || !runtime.getCommand().get(1).equals("-c")){
                    logger.error(String.format("deployment %s %s container command is not wanted", deployName, runtime.getName()));
                    return false;
                }
                if(init.getVolumeMounts() == null || init.getVolumeMounts().size() == 0){
                    logger.error(String.format("deployment %s %s container has not any volumeMount", deployName, init.getName()));
                    return false;
                }
                Map<String, V1VolumeMount> initMountMap = init.getVolumeMounts().stream().collect(Collectors.toMap(k->k.getName(), v->v));
                if(runtime.getVolumeMounts() == null || runtime.getVolumeMounts().size() == 0){
                    logger.error(String.format("deployment %s %s container has not any volumeMount", deployName, runtime.getName()));
                    return false;
                }
                Map<String, V1VolumeMount> runtimeMountMap = runtime.getVolumeMounts().stream().collect(Collectors.toMap(k->k.getName(), v->v));
                String volume = module.getAppType().equals(AppType.BINARY_FILE) ? "binary-file" : "war";
                if(!volumeMap.containsKey(volume)){
                    logger.error(String.format("deployment %s not find %s volume", deployName, volume));
                    return false;
                }
                else if(!initMountMap.containsKey(volume)){
                    logger.error(String.format("%s deployment %s container not has %s volume", deployName, init.getName(), volume));
                    return false;
                }
                else if(!runtimeMountMap.containsKey(volume)){
                    logger.error(String.format("%s deployment %s container not has %s volume", deployName, runtime.getName(), volume));
                    return false;
                }
                volume = String.format("%s-%s-volume", init.getName(), domainId);
                if(!volumeMap.containsKey(volume)){
                    logger.error(String.format("deployment %s not find %s configMap volume", deployName, volume));
                    return false;
                }
                else if(volumeMap.get(volume).getConfigMap() == null){
                    logger.error(String.format("deployment %s %s volume is not configMap", deployName, volume));
                    return false;
                }
                else if(!initMountMap.containsKey(volume)){
                    logger.error(String.format("%s deployment %s container not has %s configMap volume", deployName, init.getName(), volume));
                    return false;
                }
            }
        }
        catch (Exception ex) {
            logger.error(String.format("parse deployment exception", ex));
            return false;
        }
        return true;
    }

    @Override
    public PlatformAppDeployDetailVo getPlatformAppDetailFromK8s(PlatformPo platform, String domainId, String appName, String alias) throws ApiException, ParamException, IOException, InterfaceCallException, NexusException {
        Map<String, String> selector = new HashMap<>();
        selector.put(this.domainIdLabel, domainId);
        selector.put(appName, alias);
        List<V1Deployment> deployments = this.ik8sApiService.selectNamespacedDeployment(platform.getPlatformId(), selector, platform.getK8sApiUrl(), platform.getK8sAuthToken());
        if(deployments.size() == 0 || deployments.size() > 1){
            throw new ParamException(String.format("%s has find %d deployment for %s", platform.getPlatformId(), deployments.size(), gson.toJson(selector)));
        }
        V1Deployment deployment = deployments.get(0);
        if(!isCCODDomainAppDeployment(deployment)){
            throw new ParamException(String.format("deployment %s for %s is illegal ccod domain app deployment",
                    deployment.getMetadata().getName(), gson.toJson(selector)));
        }
        V1Container initContainer = deployment.getSpec().getTemplate().getSpec().getInitContainers().stream()
                .collect(Collectors.toMap(k->k.getName(), v->v)).get(alias);
        if(initContainer == null){
            throw new ParamException(String.format("can not find container for %s", gson.toJson(selector)));
        }
        AppModuleVo module = appManagerService.getRegisteredCCODAppFromImageUrl(initContainer.getImage());
        V1ConfigMap configMap = ik8sApiService.readNamespacedConfigMap(String.format("%s-%s", alias, domainId), platform.getPlatformId(), platform.getK8sApiUrl(), platform.getK8sAuthToken());
        List<V1Service> services = this.ik8sApiService.selectNamespacedService(platform.getPlatformId(), selector, platform.getK8sApiUrl(), platform.getK8sAuthToken());
        if(services.size() == 0){
            throw new ParamException(String.format("can not find service for %s", gson.toJson(selector)));
        }
        return getAppDetailFromK8sObj(module, alias,deployment, services, configMap);
    }
}
