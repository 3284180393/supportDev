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
import com.channelsoft.ccod.support.cmdb.vo.K8sCollection;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
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

    @Value("${k8s.version}")
    private String ccodVersionLabel;

    @Value("${k8s.labels.domain-id}")
    private String domainIdLabel;

    @Value("${k8s.deployment.defaultCfgMountPath}")
    private String defaultCfgMountPath;

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
        List<K8sObjectTemplatePo> testList = generatePlatformObjectTemplate("test-by-wyf", "4.1", "glsserver-public01", "cas-manage01", "dcms-manage01");
        this.objectTemplateList.addAll(testList);
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
        V1PersistentVolume pv = this.ik8sApiService.readPersistentVolume("base-volume-test-by-wyf", testK8sApiUrl, testAuthToken);
        template.setPersistentVolumeJson(templateParseGson.toJson(pv));
        V1PersistentVolumeClaim pvc = this.ik8sApiService.readNamespacedPersistentVolumeClaim("base-volume-test-by-wyf", srcPlatformId, testK8sApiUrl, testAuthToken);
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
        template.setDeployJson(templateParseGson.toJson(oraSvc));
        templateList.add(template);

        labels = new HashMap<>();
        labels.put(this.ccodVersionLabel, ccodVersion);
        labels.put(this.appNameLabel, "mysql");
        labels.put(this.appTypeLabel, AppType.THREE_PART_APP.name);
        template = new K8sObjectTemplatePo(labels);
        V1Deployment mysqlDep = this.ik8sApiService.readNamespacedDeployment("mysql", srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setDeployJson(templateParseGson.toJson(mysqlDep));
        V1Service mysqlSvc = this.ik8sApiService.readNamespacedService("mysqlDep", srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setDeployJson(templateParseGson.toJson(mysqlSvc));
        templateList.add(template);

        labels = new HashMap<>();
        labels.put(this.ccodVersionLabel, ccodVersion);
        labels.put(this.appTypeLabel, AppType.BINARY_FILE.name);
        template = new K8sObjectTemplatePo(labels);
        V1Deployment binDep = this.ik8sApiService.readNamespacedDeployment(binaryApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setDeployJson(templateParseGson.toJson(binDep));
        V1Service binSvc = this.ik8sApiService.readNamespacedService(tomcatApp, binaryApp, testK8sApiUrl, testAuthToken);
        template.setDeployJson(templateParseGson.toJson(binSvc));
        templateList.add(template);

        labels = new HashMap<>();
        labels.put(this.ccodVersionLabel, ccodVersion);
        labels.put(this.appTypeLabel, AppType.TOMCAT_WEB_APP.name);
        template = new K8sObjectTemplatePo(labels);
        V1Deployment tomcatDep = this.ik8sApiService.readNamespacedDeployment(tomcatApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setDeployJson(templateParseGson.toJson(tomcatDep));
        V1Service tomcatSvc = this.ik8sApiService.readNamespacedService(tomcatApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setDeployJson(templateParseGson.toJson(tomcatSvc));
        ExtensionsV1beta1Ingress tomIngress = this.ik8sApiService.readNamespacedIngress(tomcatApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setIngressJson(templateParseGson.toJson(tomIngress));
        templateList.add(template);

        labels = new HashMap<>();
        labels.put(this.ccodVersionLabel, ccodVersion);
        labels.put(this.appTypeLabel, AppType.RESIN_WEB_APP.name);
        template = new K8sObjectTemplatePo(labels);
        V1Deployment resinDep = this.ik8sApiService.readNamespacedDeployment(tomcatApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setDeployJson(templateParseGson.toJson(resinDep));
        V1Service resinSvc = this.ik8sApiService.readNamespacedService(tomcatApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setDeployJson(templateParseGson.toJson(resinSvc));
        ExtensionsV1beta1Ingress resinIngress = this.ik8sApiService.readNamespacedIngress(tomcatApp, srcPlatformId, testK8sApiUrl, testAuthToken);
        template.setIngressJson(templateParseGson.toJson(resinIngress));
        templateList.add(template);
        return templateList;
    }

    @Override
    public ExtensionsV1beta1Ingress selectIngress(Map<String, String> selector, String alias, String platformId, String domainId) throws ParamException {
        ExtensionsV1beta1Ingress ingress = null;
        for(K8sObjectTemplatePo template : this.objectTemplateList)
        {
            if(isMatch(selector, template.getLabels())) {
                ingress = templateParseGson.fromJson(template.getIngressJson(), ExtensionsV1beta1Ingress.class);
                break;
            }
        }
        if(ingress == null)
            throw new ParamException(String.format("can not find matched ingress template for %s", gson.toJson(selector)));
        ingress.getMetadata().setNamespace(platformId);
        ingress.getMetadata().setName(String.format("%s-%s", alias, domainId));
        return ingress;
    }

    @Override
    public V1Service selectService(Map<String, String> selector, String appName, String alias, ServicePortType portType, String portStr, String platformId, String domainId) throws ParamException {
        if(!portType.equals(ServicePortType.ClusterIP) && !portType.equals(ServicePortType.NodePort))
            throw new ParamException(String.format("can not handle server port type : %s", portType.name));
        V1Service service = null;
        for(K8sObjectTemplatePo template : this.objectTemplateList)
        {
            if(isMatch(selector, template.getLabels()))
            {
                service = templateParseGson.fromJson(template.getServiceJson(), V1Service.class);
                break;
            }
        }
        if(service == null)
            throw new ParamException(String.format("can not find service template for %s", gson.toJson(selector)));
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
            if(isMatch(selector, template.getLabels()))
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
    public V1Deployment selectDeployment(AppUpdateOperationInfo optInfo, String platformId, String domainId, List<AppFileNexusInfo> platformCfg, List<AppFileNexusInfo> domainCfg) throws ParamException {
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
            if(isMatch(selector, template.getLabels()))
            {
                deploy = templateParseGson.fromJson(template.getDeployJson(), V1Deployment.class);
                break;
            }
        }
        String deployPath = getAbsolutePath(optInfo.getBasePath(), optInfo.getDeployPath());
        String logPath = appType.equals(AppType.TOMCAT_WEB_APP) ? deployPath.replaceAll("/[^/]$", "/logs") : deployPath.replaceAll("/[^/]$", "/log");
        //完成应用程序配置文件configMap挂载
        if(deploy == null)
            throw new ParamException(String.format("can not find deployment template for %s", gson.toJson(selector)));
        String configVolumeName = String.format("%s-%s-volume", alias, domainId);
        V1ConfigMapVolumeSource source = new V1ConfigMapVolumeSource();
        source.setItems(new ArrayList<>());
        source.setName(configVolumeName);
        for (AppFileNexusInfo cfg : optInfo.getCfgs()) {
            V1KeyToPath item = new V1KeyToPath();
            item.setKey(cfg.getFileName());
            item.setPath(cfg.getFileName());
            source.getItems().add(item);
        }
        Map<String, V1Volume> volumeMap = deploy.getSpec().getTemplate().getSpec().getVolumes().stream().collect(Collectors.toMap(V1Volume::getName, Function.identity()));
        logger.debug(String.format("generate app configMap volume"));
        String configMapName = String.format("%s-%s-volume", alias, domainId);
        V1Volume volume = generateConfigMapVolume(configMapName, optInfo.getCfgs());
        volumeMap.put(configMapName, volume);
        if(domainCfg != null && domainCfg.size() > 0)
        {
            logger.debug(String.format("generate domain public configMap volume"));
            configMapName = String.format("%s-volume", domainId);
            volume = generateConfigMapVolume(configMapName, domainCfg);
            volumeMap.put(configMapName, volume);
        }
        if(platformCfg != null && platformCfg.size() > 0)
        {
            logger.debug(String.format("generate platform public configMap volume"));
            configMapName = String.format("%s-volume", platformId);
            volume = generateConfigMapVolume(configMapName, domainCfg);
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
        deploy.getSpec().getTemplate().getSpec().setVolumes(new ArrayList<>(volumeMap.values()));
        V1Container initContainer = deploy.getSpec().getTemplate().getSpec().getInitContainers().get(0);
        logger.debug(String.format("set initContainer name : %s", alias));
        initContainer.setName(alias);
        Map<String, V1VolumeMount> volumeMountMap = initContainer.getVolumeMounts().stream()
                .collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()));
        volumeMountMap.get("ccod-runtime").setMountPath(logPath);
        volumeMountMap.get("ccod-runtime").setSubPath(alias);
        logger.debug(String.format("modify volume mount ccod-runtime to %s", gson.toJson(volumeMountMap.get("ccod-runtime"))));
        String mountName = String.format("%s-%s-volume", alias, domainId);
        if(!volumeMountMap.containsKey(mountName))
        {
            logger.debug(String.format("add mount %s to init container", mountName));
            V1VolumeMount mount = new V1VolumeMount();
            mount.setName(String.format("%s-%s-volume", alias, domainId));
            volumeMountMap.put(mountName, mount);
        }
        volumeMountMap.get(mountName).setMountPath(String.format("/%s/cas-cfg", alias));
        initContainer.setVolumeMounts(new ArrayList<>(volumeMountMap.values()));
        V1Container runtimeContainer = deploy.getSpec().getTemplate().getSpec().getContainers().get(0);
        logger.debug(String.format("set container name to %s-runtime", alias));
        runtimeContainer.setName(String.format("%s-runtime", alias));
        volumeMountMap = runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity()));
        if(domainCfg != null && domainCfg.size() > 0)
        {
            mountName = String.format("%s-volume", domainId);
            logger.debug(String.format("add domain public config mount %s to container", mountName));
            V1VolumeMount mount = new V1VolumeMount();
            mount.setName(mountName);
            volumeMountMap.put(mountName, mount);
        }
        if(platformCfg != null && platformCfg.size() > 0)
        {
            mountName = String.format("%s-volume", platformId);
            logger.debug(String.format("add platform public config mount %s to container", mountName));
            V1VolumeMount mount = new V1VolumeMount();
            mount.setName(mountName);
            volumeMountMap.put(mountName, mount);
        }
        runtimeContainer.setVolumeMounts(new ArrayList<>(volumeMountMap.values()));
        generateParamForCollection(platformId, domainId, optInfo, module, domainCfg, platformCfg, deploy);
        return deploy;
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
        volume.setName(configMapName);
        volume.setConfigMap(source);
        return volume;
    }

    private void generateParamForCollection(String platformId, String domainId,
                                            AppUpdateOperationInfo optInfo, AppModuleVo appModule,
                                            List<AppFileNexusInfo> domainCfg, List<AppFileNexusInfo> platformCfg,
                                            V1Deployment deployment) throws ParamException
    {
        String appName = appModule.getAppName();
        String alias = optInfo.getAppAlias();
        deployment.getMetadata().getLabels().put(String.format("%s-alias", appName), alias);
        String version = appModule.getVersion().replaceAll("\\:", "-");
        deployment.getMetadata().getLabels().put(appName, alias);
        deployment.getMetadata().getLabels().put(String.format("%s-version", appName), version);
        deployment.getSpec().getSelector().getMatchLabels().put(appName, alias);
        deployment.getSpec().getTemplate().getMetadata().getLabels().put(appName, alias);
        V1Container initContainer = deployment.getSpec().getTemplate().getSpec().getInitContainers().get(0);
        initContainer.setName(alias);
        V1Container runtimeContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
        runtimeContainer.setName(String.format("%s-runtime", alias));
        AppType appType = appModule.getAppType();
        initContainer.setCommand(new ArrayList<>());
        initContainer.getCommand().add("/bin/sh");
        initContainer.getCommand().add("-c");
        initContainer.getCommand().add("");
        String mountPath = String.format("%s/%s-cfg", this.defaultCfgMountPath, alias);
        if (appType.equals(AppType.BINARY_FILE)) {
            runtimeContainer.setArgs(new ArrayList<>());
            runtimeContainer.getVolumeMounts().stream().collect(Collectors.toMap(V1VolumeMount::getName, Function.identity())).get("binary-file").setMountPath(optInfo.getBasePath());
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
        } else if (appType.equals(AppType.RESIN_WEB_APP) || appType.equals(AppType.TOMCAT_WEB_APP)) {
            initContainer.setArgs(new ArrayList<>());
            String cmd = initContainer.getCommand().get(2);
            cmd = String.format("%s;mv /opt/webapps/%s /war/%s-%s.war", cmd, appModule.getInstallPackage().getFileName(), alias, domainId);
            initContainer.getCommand().set(2, cmd);
            runtimeContainer.getArgs().set(0, String.format("%s;cd %s;%s", runtimeContainer.getArgs().get(0), basePath, startCmd));
        }
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
        return null;
    }

    @Override
    public V1Namespace selectNamespace(Map<String, String> selector, String platformId) throws ParamException {
        return null;
    }

    @Override
    public V1Secret selectSecret(Map<String, String> selector, String platformId, String name) throws ParamException {
        return null;
    }

    @Override
    public V1PersistentVolume selectPersistentVolume(Map<String, String> selector) throws ParamException {
        return null;
    }

    @Override
    public V1PersistentVolumeClaim selectPersistentVolumeClaim(Map<String, String> selector, String platformId) throws ParamException {
        return null;
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
}
