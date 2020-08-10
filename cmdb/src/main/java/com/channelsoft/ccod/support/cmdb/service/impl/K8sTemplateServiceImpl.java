package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.config.GsonDateUtil;
import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.ServicePortType;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.po.K8sObjectTemplatePo;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import com.channelsoft.ccod.support.cmdb.service.IK8sTemplateService;
import com.channelsoft.ccod.support.cmdb.vo.AppFileNexusInfo;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;
import com.channelsoft.ccod.support.cmdb.vo.AppUpdateOperationInfo;
import com.channelsoft.ccod.support.cmdb.vo.PortVo;
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
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Value("${k8s.labels.app-name}")
    private String appNameLabel;

    @Value("${k8s.labels.app-type}")
    private String appTypeLabel;

    @Value("${k8s.labels.ccod-version}")
    private String ccodVersionLabel;

    @Value("${k8s.labels.domain-id}")
    private String domainIdLabel;

    @Value("${k8s.labels.app-version}")
    private String appVersionLabel;

    @Value("${k8s.deployment.defaultCfgMountPath}")
    private String defaultCfgMountPath;

    @Value("${nexus.nexus-docker-url}")
    private String nexusDockerUrl;

    @Value("${ccod.service-port-regex}")
    private String portRegex;

    @Value("${k8s.template-file-path}")
    private String templateSavePath;

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

    protected String testK8sApiUrl = "https://10.130.41.218:6443";

    protected String testAuthToken = "eyJhbGciOiJSUzI1NiIsImtpZCI6IkQwUFZRU3Vzano0cS03eWxwTG8tZGM1YS1aNzdUOE5HNWNFUXh6YThrUG8ifQ.eyJpc3MiOiJrdWJlcm5ldGVzL3NlcnZpY2VhY2NvdW50Iiwia3ViZXJuZXRlcy5pby9zZXJ2aWNlYWNjb3VudC9uYW1lc3BhY2UiOiJrdWJlLXN5c3RlbSIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VjcmV0Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbi10b2tlbi10cnZ4aiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50Lm5hbWUiOiJrdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiIsImt1YmVybmV0ZXMuaW8vc2VydmljZWFjY291bnQvc2VydmljZS1hY2NvdW50LnVpZCI6ImI5ZjQ2YWZlLTQ0ZTYtNDllNC1iYWE2LTY3ODZmY2NhNTkyYiIsInN1YiI6InN5c3RlbTpzZXJ2aWNlYWNjb3VudDprdWJlLXN5c3RlbTprdWJlcm5ldGVzLWRhc2hib2FyZC1hZG1pbiJ9.emXO4luNDCozenbvjxAmk4frqzrzpJzFbn-dBV6lLUjXhuRKWbrRbflko_6Xbwj5Gd1X-0L__a_q1BrE0W-e88uDlu-9dj5FHLihk1hMgrBfJiMiuKLQQmqcJ2-XjXAEZoNdVRY-LTO7C8tkSvYVqzl_Nt2wPxceWVthKc_dpRNEgHsyic4OejqgjI0Txr_awJyjwcF-mndngivX0G1aucrK-RRnM6aj2Xhc9xxDnwB01cS8C2mqKApE_DsBGTgUiCWwee2rr1D2xGMqewGE-LQtQfkb05hzTNUfJRwaKKk6Myby7GqizzPci0O3Y4PwwKFDgY04CI32acp6ltA1cA";

    @PostConstruct
    void init() throws Exception
    {
//        List<K8sObjectTemplatePo> testList = generatePlatformObjectTemplate("test-by-wyf", "4.1", "ucds-cloud01", "cas-manage01", "dcms-manage01");
//        this.objectTemplateList.addAll(testList);
//        testList = generatePlatformObjectTemplate("jhkzx-1", "3.9", "ucds-cloud01", "cas-manage01", "dcmswebservice-manage01");
//        this.objectTemplateList.addAll(testList);
        List<K8sObjectTemplatePo> list = parseTemplateFromFile(this.templateSavePath);
        this.objectTemplateList.addAll(list);
        logger.warn(String.format("test template=%s", gson.toJson(this.objectTemplateList)));
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
    public ExtensionsV1beta1Ingress selectIngress(Map<String, String> selector, String alias, String platformId, String domainId, String hostUrl) throws ParamException {
        ExtensionsV1beta1Ingress ingress = null;
        for(K8sObjectTemplatePo template : this.objectTemplateList)
        {
            if(isMatch(selector, template.getLabels()) && StringUtils.isNotBlank(template.getIngressJson())) {
                ingress = templateParseGson.fromJson(template.getIngressJson(), ExtensionsV1beta1Ingress.class);
                break;
            }
        }
        if(ingress == null)
            throw new ParamException(String.format("can not find matched ingress template for %s", gson.toJson(selector)));
        ingress.getMetadata().setNamespace(platformId);
        ingress.getMetadata().setName(String.format("%s-%s", alias, domainId));
        ingress.getSpec().getRules().get(0).setHost(hostUrl);
        ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).setPath(String.format("/%s-%s", alias, domainId));
        ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getBackend().setServiceName(String.format("%s-%s", alias, domainId));
        logger.info(String.format("selected ingress for selector %s is %s", gson.toJson(selector), gson.toJson(ingress)));
        return ingress;
    }

    @Override
    public V1Service selectService(Map<String, String> selector, String appName, String alias, AppType appType, ServicePortType portType, String portStr, String platformId, String domainId) throws ParamException {
        if(!portType.equals(ServicePortType.ClusterIP) && !portType.equals(ServicePortType.NodePort))
            throw new ParamException(String.format("can not handle service port type : %s", portType.name));
        V1Service service = null;
        for(K8sObjectTemplatePo template : this.objectTemplateList)
        {
            if(isMatch(selector, template.getLabels()) && StringUtils.isNotBlank(template.getServiceJson()))
            {
                service = templateParseGson.fromJson(template.getServiceJson(), V1Service.class);
                break;
            }
        }
        if(service == null)
            throw new ParamException(String.format("can not find service template for %s", gson.toJson(selector)));
        List<PortVo> portList = parsePort(portStr, portType, appType);
        String[] ports = portStr.split(",");
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
            svcPort.setName(portVo.getProtocol() + "");
            svcPort.setProtocol(portVo.getProtocol());
            if(portType.equals(ServicePortType.NodePort))
                svcPort.setNodePort(portVo.getNodePort());
            else
                svcPort.setTargetPort(new IntOrString(portVo.getTargetPort()));


        }
        for(String clusterPort : ports)
        {
            V1ServicePort svcPort = new V1ServicePort();
            String[] arr = clusterPort.split("/");
            svcPort.setProtocol(arr[1]);
            String[] arr1 = arr[0].split("\\:");
            svcPort.setPort(Integer.parseInt(arr1[0]));
            svcPort.setName(arr1[0]);
            if(arr1.length == 2) {
                if(ServicePortType.NodePort.equals(portType))
                    svcPort.setNodePort(Integer.parseInt(arr[1]));
                else
                    svcPort.setTargetPort(new IntOrString(arr1[1]));
            }
            service.getSpec().getPorts().add(svcPort);
        }
        return service;
    }

    @Override
    public V1Service selectService(Map<String, String> selector, String appName, String alias, String platformId) throws ParamException {
        V1Service service = null;
        for(K8sObjectTemplatePo template : this.objectTemplateList)
        {
            if(isMatch(selector, template.getLabels()) && StringUtils.isNotBlank(template.getServiceJson()))
            {
                service = templateParseGson.fromJson(template.getServiceJson(), V1Service.class);
                break;
            }
        }
        if(service == null)
            throw new ParamException(String.format("can not find service template for %s", gson.toJson(selector)));
        service.getMetadata().setLabels(new HashMap<>());
        service.getMetadata().getLabels().put(appName, alias);
        service.getMetadata().setName(alias);
        service.getMetadata().setNamespace(platformId);
        service.getSpec().setSelector(new HashMap<>());
        service.getSpec().getSelector().put(appName, alias);
        return service;
    }

    @Override
    public V1Deployment selectDeployment(AppUpdateOperationInfo optInfo, String hostUrl, String platformId, String domainId, List<AppFileNexusInfo> platformCfg, List<AppFileNexusInfo> domainCfg) throws ParamException {
        String appName = optInfo.getAppName();
        String version = optInfo.getTargetVersion();
        String alias = optInfo.getAppAlias();
        AppModuleVo module = this.appManagerService.queryAllRegisterAppModule(true).stream()
                .collect(Collectors.groupingBy(AppModuleVo::getAppName)).get(appName).stream()
                .collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).get(version);
        AppType appType = module.getAppType();
        Map<String, String> selector = new HashMap<>();
        selector.put(this.ccodVersionLabel, module.getCcodVersion());
        selector.put(this.appTypeLabel, appType.name);
        V1Deployment deploy = null;
        for(K8sObjectTemplatePo template : this.objectTemplateList)
        {
            if(isMatch(selector, template.getLabels()) && StringUtils.isNotBlank(template.getDeployJson()))
            {
                deploy = templateParseGson.fromJson(template.getDeployJson(), V1Deployment.class);
                break;
            }
        }
        String basePath = optInfo.getBasePath();
        String deployPath = getAbsolutePath(optInfo.getBasePath(), optInfo.getDeployPath());
        String logPath = appType.equals(AppType.TOMCAT_WEB_APP) ? deployPath.replaceAll("/[^/]$", "/logs") : deployPath.replaceAll("/[^/]$", "/log");
        //完成应用程序配置文件configMap挂载
        if(deploy == null)
            throw new ParamException(String.format("can not find deployment template for %s", gson.toJson(selector)));
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
        List<V1Volume> volumes = generateVolumeForDeployment(deploy, appType, alias, platformId, domainId, optInfo.getCfgs(), platformCfg, domainCfg);
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
        mounts = generateRuntimeContainerMount(runtimeContainer, optInfo, appType, platformId, domainId, platformCfg, domainCfg);
        runtimeContainer.setVolumeMounts(mounts);
        logger.debug(String.format("generate init container commands"));
        List<String> commands = generateCmdForInitContainer(optInfo, module, domainId);
        initContainer.setCommand(commands);
        initContainer.setArgs(new ArrayList<>());
        logger.debug(String.format("generate runtime container command"));
        commands = generateCmdForRuntimeContainer(optInfo, appType, platformId, domainId, platformCfg, domainCfg);
        runtimeContainer.setCommand(commands);
        runtimeContainer.setArgs(new ArrayList<>());
        List<V1ContainerPort> containerPorts = generateContainerPortsForRuntimeContainer(optInfo.getPorts(), appType);
        logger.debug(String.format("containerPorts of %s runtime container at %s is : %s", alias, domainId, gson.toJson(containerPorts)));
        runtimeContainer.setPorts(containerPorts);
        generateProbeForRuntimeContainer(runtimeContainer, alias, domainId, appType, optInfo.getPorts());
        if(appType.equals(AppType.RESIN_WEB_APP) || appType.equals(AppType.TOMCAT_WEB_APP))
        {
            logger.debug(String.format("modify deployment hostnames of hostAliases to %s", hostUrl));
            deploy.getSpec().getTemplate().getSpec().getHostAliases().get(0).getHostnames().set(0, hostUrl);
        }
        logger.debug("modify labels and selector of deploy");
        logger.info(String.format("selected deployment is %s for selector %s", gson.toJson(deploy), gson.toJson(selector)));
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

    private List<V1VolumeMount> generateRuntimeContainerMount(V1Container runtimeContainer, AppUpdateOperationInfo optInfo, AppType appType, String platformId, String domainId, List<AppFileNexusInfo> platformCfg, List<AppFileNexusInfo> domainCfg) throws ParamException
    {
        logger.debug(String.format("generate runtime container volume mount"));
        String basePath = optInfo.getBasePath();
        String deployPath = getAbsolutePath(basePath, optInfo.getDeployPath());
        String alias = optInfo.getAppAlias();
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

    private List<String> generateCmdForRuntimeContainer(AppUpdateOperationInfo optInfo, AppType appType, String platformId, String domainId, List<AppFileNexusInfo> platformCfg, List<AppFileNexusInfo> domainCfg) throws ParamException
    {
        List<String> commands = new ArrayList<>();
        commands.add(0, "/bin/sh");
        commands.add(1, "-c");
        String basePath = optInfo.getBasePath();
        String deployPath = getAbsolutePath(basePath, optInfo.getDeployPath());
        String alias = optInfo.getAppAlias();
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
        execParam = String.format("%s;cd %s;%s", execParam, appType.equals(AppType.BINARY_FILE) ? deployPath:basePath, optInfo.getStartCmd());
        commands.add(execParam.replaceAll("^;", "").replaceAll(";;", ";"));
        logger.debug(String.format("command for %s at %s is : %s", alias, domainId, String.join(";", commands)));
        return commands;
    }

    private List<String> generateCmdForInitContainer(AppUpdateOperationInfo optInfo, AppModuleVo module, String domainId) throws ParamException
    {
        List<String> commands = new ArrayList<>();
        commands.add(0, "/bin/sh");
        commands.add(1, "-c");
        AppType appType = module.getAppType();
        String appName = optInfo.getAppName();
        String alias = optInfo.getAppAlias();
        String packageFileName = module.getInstallPackage().getFileName();
        String theName = packageFileName.replaceAll("\\.war$", "");
        String basePath = appType.equals(AppType.BINARY_FILE) ? "/binary-file" : "/opt";
        String deployPath = getAbsolutePath(basePath, optInfo.getDeployPath());
        String execParam = "";
        String mountPath = String.format("/cfg/%s-cfg", alias);
        switch (appType)
        {
            case BINARY_FILE:
                execParam = String.format("mkdir %s -p;mkdir %s/log -p;mv /opt/%s %s/%s", deployPath, basePath, packageFileName, deployPath, module.getInstallPackage().getFileName());
                break;
            case TOMCAT_WEB_APP:
            case RESIN_WEB_APP:
                execParam = String.format("mkdir %s -p;cd %s;mv /opt/%s %s/%s", deployPath, deployPath, packageFileName, deployPath, packageFileName);
                break;
            default:
                throw new ParamException(String.format("error appType %s", appType.name));
        }
        Map<String, List<AppFileNexusInfo>> deployPathCfgMap = optInfo.getCfgs().stream().collect(Collectors.groupingBy(AppFileNexusInfo::getDeployPath));
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
        if ("ucxserver".equals(appName))
            execParam = String.format("%s;mv /opt/FlowMap.full /binary-file/cfg", execParam);
        switch (appType)
        {
            case TOMCAT_WEB_APP:
            case RESIN_WEB_APP:
                execParam = String.format("%s;mv /%s/%s /war/%s-%s.war", execParam, deployPath, packageFileName, alias, domainId);
        }
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

    private void generateProbeForRuntimeContainer(V1Container runtimeContainer, String alias, String domainId, AppType appType, String portStr) throws ParamException
    {
        List<PortVo> portList = parsePort(portStr, ServicePortType.ClusterIP, appType);
        int targetPort = portList.get(0).getTargetPort();
        logger.debug(String.format("monitor port is %d/TCP", targetPort));
        switch (appType) {
            case BINARY_FILE:
                runtimeContainer.getLivenessProbe().getTcpSocket().setPort(new IntOrString(targetPort));
                runtimeContainer.getReadinessProbe().getTcpSocket().setPort(new IntOrString(targetPort));
                break;
            case TOMCAT_WEB_APP:
            case RESIN_WEB_APP:
                logger.debug(String.format("monitor port is %d/HTTPGet", targetPort));
                runtimeContainer.getLivenessProbe().getHttpGet().setPort(new IntOrString(targetPort));
                runtimeContainer.getLivenessProbe().getHttpGet().setPath(String.format("/%s-%s", alias, domainId));
                runtimeContainer.getReadinessProbe().getHttpGet().setPort(new IntOrString(targetPort));
                runtimeContainer.getReadinessProbe().getHttpGet().setPath(String.format("/%s-%s", alias, domainId));
                break;
            default:
                throw new ParamException(String.format("can not handle probe for appType=%s", appType.name));
        }
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

    @Override
    public V1Deployment selectDeployment(Map<String, String> selector, String appName, String alias, String version, String platformId) throws ParamException {
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
        logger.info(String.format("selected deployment for %s is %s", gson.toJson(selector), gson.toJson(deploy)));
        return deploy;
    }

    @Override
    public V1Namespace selectNamespace(Map<String, String> selector, String platformId) throws ParamException {
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
        logger.info(String.format("selected namespace for selector %s is %s", gson.toJson(selector), gson.toJson(ns)));
        return ns;
    }

    @Override
    public V1Secret selectSecret(Map<String, String> selector, String platformId, String name) throws ParamException {
        V1Secret secret = null;
        for(K8sObjectTemplatePo template : this.objectTemplateList)
        {
            if(isMatch(selector, template.getLabels()) && StringUtils.isNotBlank(template.getSecretJson()))
            {
                secret = templateParseGson.fromJson(template.getSecretJson(), V1Secret.class);
                break;
            }
        }
        if(secret == null)
            throw new ParamException(String.format("can not match secret for selector %s", gson.toJson(selector)));
        secret.getMetadata().setName(name);
        secret.getMetadata().setNamespace(platformId);
        logger.info(String.format("selected secret for selector %s is %s", gson.toJson(selector), gson.toJson(secret)));
        return secret;
    }

    @Override
    public V1PersistentVolume selectPersistentVolume(Map<String, String> selector, String platformId) throws ParamException {
        V1PersistentVolume pv = null;
        for(K8sObjectTemplatePo template : this.objectTemplateList)
        {
            if(isMatch(selector, template.getLabels()) && StringUtils.isNotBlank(template.getPersistentVolumeJson()))
            {
                pv = templateParseGson.fromJson(template.getPersistentVolumeJson(), V1PersistentVolume.class);
                break;
            }
        }
        if(pv == null)
            throw new ParamException(String.format("can not match persistentVolume for select %s", gson.toJson(selector)));
        String name = String.format("base-volume-%s", platformId);
        pv.getMetadata().setName(name);
        pv.getSpec().getClaimRef().setNamespace(platformId);
        pv.getSpec().getClaimRef().setName(name);
        pv.getSpec().getNfs().setPath(String.format("/home/kubernetes/volume/%s/base-volume", platformId));
        pv.getSpec().setStorageClassName(name);
        logger.info(String.format("selected persistentVolume for select %s is %s", gson.toJson(selector), gson.toJson(pv)));
        return pv;
    }

    @Override
    public V1PersistentVolumeClaim selectPersistentVolumeClaim(Map<String, String> selector, String platformId) throws ParamException {
        V1PersistentVolumeClaim pvc = null;
        for(K8sObjectTemplatePo template : this.objectTemplateList)
        {
            if(isMatch(selector, template.getLabels()) && StringUtils.isNotBlank(template.getPersistentVolumeClaimJson()))
            {
                pvc = templateParseGson.fromJson(template.getPersistentVolumeClaimJson(), V1PersistentVolumeClaim.class);
                break;
            }
        }
        if(pvc == null)
            throw new ParamException(String.format("can not match persistentVolumeClaim for selector %s", gson.toJson(selector)));
        pvc.getMetadata().setName(String.format("base-volume-%s", platformId));
        pvc.getMetadata().setNamespace(platformId);
        pvc.getSpec().setStorageClassName(String.format("base-volume-%s", platformId));
        pvc.getSpec().setVolumeName(String.format("base-volume-%s", platformId));
        logger.info(String.format("selected pvc for selector %s is %s", gson.toJson(selector), gson.toJson(pvc)));
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
}
