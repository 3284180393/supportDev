package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.config.AppDefine;
import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.config.CCODBiz;
import com.channelsoft.ccod.support.cmdb.constant.CCODPlatformStatus;
import com.channelsoft.ccod.support.cmdb.constant.DomainStatus;
import com.channelsoft.ccod.support.cmdb.constant.K8sServiceType;
import com.channelsoft.ccod.support.cmdb.constant.PlatformType;
import com.channelsoft.ccod.support.cmdb.dao.*;
import com.channelsoft.ccod.support.cmdb.exception.*;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.service.*;
import com.channelsoft.ccod.support.cmdb.vo.*;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
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
    CCODBiz ccodBiz;

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

    private final Map<String, PlatformUpdateSchemaInfo> platformUpdateSchemaMap = new ConcurrentHashMap<>();

    private Map<String, List<BizSetDefine>> appSetRelationMap;

    private Map<String, BizSetDefine> setDefineMap;

    private boolean isPlatformCheckOngoing = false;

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
//            V1Namespace ns = ik8sApiService.queryNamespace(namespace, k8sApiUrl, authToken);
//            System.out.println(JSONObject.toJSONString(ns));
//            List<V1Namespace> nsList = ik8sApiService.queryAllNamespace(k8sApiUrl, authToken);
//            System.out.println(String.format("find %d namespace from %s", nsList.size(), k8sApiUrl));
//            List<V1Pod> podList = ik8sApiService.queryAllPodAtNamespace(namespace, k8sApiUrl, authToken);
//            System.out.println(String.format("find %d pod at namespace %s from %s", podList.size(), namespace, k8sApiUrl));
//            V1Pod pod = ik8sApiService.queryPod(namespace, podList.get(0).getMetadata().getName(), k8sApiUrl, authToken);
//            System.out.println(JSONObject.toJSONString(pod.getMetadata().getName()));
//            getPlatformTopologyFromK8s("工具组平台", "202005-test", 34, 0, "CCOD4.1", k8sApiUrl, authToken);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public PlatformTopologyInfo getPlatformTopologyFromK8s(String platformName, String platformId, int bkBizId, int bkCloudId, String ccodVersion, String k8sApiUrl, String k8sAuthToken) throws ApiException, ParamException, NotSupportAppException, NexusException, LJPaasException, InterfaceCallException, IOException {
        logger.debug(String.format("begin to get %s(%s) topology from %s with authToke=%s", platformId, platformName, k8sApiUrl, k8sAuthToken));
        Date now = new Date();
        V1Namespace ns = ik8sApiService.queryNamespace(platformId, k8sApiUrl, k8sAuthToken);
        if(!"Active".equals(ns.getStatus().getPhase()))
        {
            logger.error(String.format("status of %s is %s", platformId, ns.getStatus().getPhase()));
            throw new ParamException(String.format("status of %s is %s", platformId, ns.getStatus().getPhase()));
        }
        List<V1Pod> podList = ik8sApiService.queryAllPodAtNamespace(platformId, k8sApiUrl, k8sAuthToken);
        List<V1Service> serviceList = ik8sApiService.queryAllServiceAtNamespace(platformId, k8sApiUrl, k8sAuthToken);
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
        List<AppModuleVo> registerAppList = appManagerService.queryAllRegisterAppModule();
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
     * 从服务信息中获取端口信息
     * @param service k8s服务信息
     * @return 使用的端口信息
     */
    private String getPortFromK8sService(V1Service service)
    {
        String port = "";
        for(V1ServicePort svcPort : service.getSpec().getPorts())
        {
            if(svcPort.getNodePort() != null && svcPort.getNodePort() > 0)
                port = String.format("%s%s:%s/%s;", port, svcPort.getPort(), svcPort.getNodePort(), svcPort.getProtocol());
            else
                port = String.format("%s%s/%s;", port, svcPort.getPort(), svcPort.getProtocol());
        }
        port = port.replaceAll(";$", "");
        return port;
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
        String port = getPortFromK8sService(service);
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
            domainPo.setType(setDefine.getName());
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
        String port = getPortFromK8sService(service);
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
        String selectName = service.getSpec().getSelector().get("name");
        String regex = String.format("^%s-.+", selectName);
        for(V1Pod pod : podList)
        {
            if(pod.getMetadata().getName().matches(regex))
            {
                logger.debug(String.format("%s matches %s", pod.getMetadata().getName(), regex));
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
            logger.debug(String.format("begin to preprocess collected %d app", successList.size()));
            successList = this.appManagerService.preprocessCollectedPlatformAppModule(successList, failList);
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
        po.setType(bkSetName);
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
}
