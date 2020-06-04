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
import com.channelsoft.ccod.support.cmdb.exception.NotSupportAppException;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import com.channelsoft.ccod.support.cmdb.service.IPlatformManagerService;
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
import java.util.*;
import java.util.function.Function;
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
    CCODBiz ccodBiz;

    @Value("${nexus.platform-app-cfg-repository}")
    private String platformAppCfgRepository;

    @Value("${nexus.app-module-repository}")
    private String appRepository;

    @Value("${nexus.user}")
    private String nexusUserName;

    @Value("${nexus.password}")
    private String nexusPassword;

    @Value("${nexus.host-url}")
    private String nexusHostUrl;

    @PostConstruct
    void init() throws Exception
    {
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
            getPlatformTopologyFromK8s("202005-test", "202005-test", k8sApiUrl, authToken);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public PlatformTopologyInfo getPlatformTopologyFromK8s(String platformName, String platformId, int bkBizId, int bkCloudId, String ccodVersion, String k8sApiUrl, String k8sAuthToken) throws ApiException, ParamException, NotSupportAppException {
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
        Map<String, DomainPo> domainMap = new HashMap<>();
        Map<String, List<AssemblePo>> domainAsbMap = new HashMap<>();
        Map<String, List<NexusAssetInfo>> srcCfgMap = new HashMap<>();
        List<AppModuleVo> registerAppList = appManagerService.queryAllRegisterAppModule();
        Map<String, String> outSvcAppMap = new HashMap<>();
        List<V1Service> outSvcList = typeSrvMap.containsKey(K8sServiceType.DOMAIN_OUT_SERVICE) ? typeSrvMap.get(K8sServiceType.DOMAIN_OUT_SERVICE) : new ArrayList<>();
        logger.debug(String.format("%s has %d domain out service", platformId, outSvcList.size()));
        for(V1Service outSvc : outSvcList)
        {
            PlatformAppModuleParam param = getPlatformAppParam4FromK8s(platformId, outSvc, srvPodMap.get(outSvc.getMetadata().getName()), domainMap, domainAsbMap, srcCfgMap, registerAppList);
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
            PlatformAppModuleParam param = getPlatformAppParam4FromK8s(platformId, domainSvc, srvPodMap.get(svcName), registerAppList);
            deployAppParamList.add(param);
        }
        logger.debug(String.format("%s(%s) has deployed %d apps", platformName, platformId, deployAppList.size()));
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
        List<DomainPo> domainList = new ArrayList<>();
        List<AssemblePo> assembleList = new ArrayList<>();
        List<PlatformAppPo> platformAppList = new ArrayList<>();
        for(PlatformAppModuleParam param : deployAppParamList)
        {
            String domainId = param.getDomainPo().getDomainId();
            String assembleTag = param.getAssemblePo().getTag();
            platformAppList.add(param.getPlatformAppPo());
            if(domainList.stream().collect(Collectors.toMap(DomainPo::getDomainId, Function.identity())).containsKey(domainId))
                domainList.add(param.getDomainPo());
            if(!assembleList.stream().collect(Collectors.groupingBy(AssemblePo::getDomainId)).containsKey(domainId)
                    || assembleList.stream().collect(Collectors.groupingBy(AssemblePo::getDomainId)).get(domainId).stream().collect(Collectors.toMap(AssemblePo::getTag, Function.identity())).containsKey(assembleTag))
                assembleList.add(param.getAssemblePo());
            List<NexusAssetInfo> assetList = this.nexusService.downloadAndUploadFiles(this.nexusHostUrl, this.nexusUserName, this.nexusPassword, srcCfgMap.get(key), this.nexusHostUrl, this.nexusUserName, this.nexusPassword, this.platformAppCfgRepository, po.getPlatformAppDirectory(po.))
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
        for(DomainPo domainPo : domainMap.values())
            this.domainMapper.insert(domainPo);
        for(String domainId : domainAsbMap.keySet())
        {
            List<AssemblePo> assembles = domainAsbMap.get(domainId);
            logger.debug(String.format("%s has %d assemble", domainId, assembles.size()));
            for(AssemblePo assemblePo : assembles)
            {
                this.assembleMapper.insert(assemblePo);
            }
        }
        return null;
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
     * @return ccod应用模块的部署详情
     * @throws ParamException
     */
    private PlatformAppModuleParam getPlatformAppParam4FromK8s(String namespace, V1Service service, List<V1Pod> svrPodList, List<AppModuleVo> registerAppList) throws ParamException
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
        DomainPo domainPo = new DomainPo();
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
        String assembleTag = String.format("%s-%s", pod.getMetadata().getName().split("\\-")[0], pod.getMetadata().getName().split("\\-")[1]);
        AssemblePo assemblePo = new AssemblePo();
        assemblePo.setDomainId(domainId);
        assemblePo.setPlatformId(namespace);
        assemblePo.setStatus(pod.getStatus().getPhase());
        assemblePo.setTag(assembleTag);
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

    private void parseModulePod(V1Namespace ns, V1Pod pod, Map<String, CCODDomainInfo> domainMap, List<BizSetDefine> setDefineList, List<AppModuleVo> registerAppList) throws ParamException, NotSupportAppException
    {
        String podName = pod.getMetadata().getName();
        String[] arr = podName.split("\\-");
        if(arr.length < 2)
        {
            logger.error(String.format("%s is illegal pod name for ccod module", podName));
            throw new ParamException(String.format("%s is illegal pod name for ccod module", podName));
        }
        Map<String, List<AppDefine>> aliasAppMap = new HashMap<>();
        Map<AppDefine, BizSetDefine> aliasSetMap = new HashMap<>();
        for(BizSetDefine setDefine : setDefineList)
        {
            for(AppDefine appDefine : setDefine.getApps())
            {
                if(!aliasAppMap.containsKey(appDefine.getAlias()))
                    aliasAppMap.put(appDefine.getAlias(), new ArrayList<>());
                aliasAppMap.get(appDefine.getAlias()).add(appDefine);
                aliasSetMap.put(appDefine, setDefine);
            }
        }
        String alias = arr[0];
        String domainId = arr[1];
        BizSetDefine setDefine = null;
        CCODDomainInfo domainInfo = null;
        for(BizSetDefine set : setDefineList) {
            String domainRegex = String.format("^%s(0[1-9]|[1-9]\\d+)", set.getFixedDomainId());
            if (domainId.matches(domainRegex))
            {
                setDefine = set;
                break;
            }
        }
        if(setDefine == null)
        {
            logger.debug(String.format("%s of %s is illegal domainId for ccod set", domainId, podName));
            throw new ParamException(String.format("%s of %s is illegal domainId for ccod set", domainId, podName));
        }
        AppDefine appDefine = null;
        for (AppDefine app : setDefine.getApps()) {
            String appRegex = String.format("^%s\\d*", app.getAlias());
            if (alias.matches(appRegex))
            {
                appDefine = app;
                break;
            }
        }
        if(appDefine == null)
        {
            logger.error(String.format("%s of %s is illegal alias for set %s", alias, podName, setDefine.getName()));
            throw new ParamException(String.format("%s of %s is illegal alias for set %s", alias, podName, setDefine.getName()));
        }
        String appName = appDefine.getName();
        String image = pod.getSpec().getInitContainers().get(0).getImage();
        arr = image.split("/");
        String tag = arr[arr.length - 1];
        if(tag.split("\\:").length != 2)
        {
            logger.error(String.format("illegal image tag %s", tag));
            throw new ParamException(String.format("illegal image tag %s", tag));
        }
        String version = tag.split("\\:")[1];
        if(!registerAppList.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName)).get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version))
        {
            logger.error(String.format("%s has not version %s", appName, version));
            throw new ParamException(String.format("%s has not version %s", appName, version));
        }
        if(!domainMap.containsKey(domainId))
        {
            String domainName = String.format("%s%s", setDefine.getFixedDomainName(), domainId.replaceAll(setDefine.getFixedDomainId(), ""));
            domainInfo = new CCODDomainInfo(domainId, domainName);
            domainMap.put(domainId, domainInfo);
        }
        CCODModuleInfo moduleInfo = new CCODModuleInfo();
        moduleInfo.setVersion(version);
        moduleInfo.setHostIp(pod.getStatus().getHostIP());
//        moduleInfo.setAppId();
        if(StringUtils.isBlank(appName))
        {
            logger.error(String.format("%s of %s is illegal alias for app module", alias, pod.getMetadata().getName()));
            throw new ParamException(String.format("%s of %s is illegal alias for app module", alias, pod.getMetadata().getName()));
        }
        if(moduleInfo == null)
        {

        }
    }

    private void parseModulePod(V1Pod pod, Map<String, CCODDomainInfo> domainMap, List<BizSetDefine> setDefineList, List<AppModuleVo> registerAppList) throws ParamException, NotSupportAppException
    {
        String podName = pod.getMetadata().getName();
        String[] arr = podName.split("\\-");
        if(arr.length < 2)
        {
            logger.error(String.format("%s is illegal pod name for ccod module", podName));
            throw new ParamException(String.format("%s is illegal pod name for ccod module", podName));
        }
        Map<String, List<AppDefine>> aliasAppMap = new HashMap<>();
        Map<AppDefine, BizSetDefine> aliasSetMap = new HashMap<>();
        for(BizSetDefine setDefine : setDefineList)
        {
            for(AppDefine appDefine : setDefine.getApps())
            {
                if(!aliasAppMap.containsKey(appDefine.getAlias()))
                    aliasAppMap.put(appDefine.getAlias(), new ArrayList<>());
                aliasAppMap.get(appDefine.getAlias()).add(appDefine);
                aliasSetMap.put(appDefine, setDefine);
            }
        }
        String alias = arr[0];
        String domainId = arr[1];
        BizSetDefine setDefine = null;
        CCODDomainInfo domainInfo = null;
        for(BizSetDefine set : setDefineList) {
            String domainRegex = String.format("^%s(0[1-9]|[1-9]\\d+)", set.getFixedDomainId());
            if (domainId.matches(domainRegex))
            {
                setDefine = set;
                break;
            }
        }
        if(setDefine == null)
        {
            logger.debug(String.format("%s of %s is illegal domainId for ccod set", domainId, podName));
            throw new ParamException(String.format("%s of %s is illegal domainId for ccod set", domainId, podName));
        }
        AppDefine appDefine = null;
        for (AppDefine app : setDefine.getApps()) {
            String appRegex = String.format("^%s\\d*", app.getAlias());
            if (alias.matches(appRegex))
            {
                appDefine = app;
                break;
            }
        }
        if(appDefine == null)
        {
            logger.error(String.format("%s of %s is illegal alias for set %s", alias, podName, setDefine.getName()));
            throw new ParamException(String.format("%s of %s is illegal alias for set %s", alias, podName, setDefine.getName()));
        }
        String appName = appDefine.getName();
        String image = pod.getSpec().getInitContainers().get(0).getImage();
        arr = image.split("/");
        String tag = arr[arr.length - 1];
        if(tag.split("\\:").length != 2)
        {
            logger.error(String.format("illegal image tag %s", tag));
            throw new ParamException(String.format("illegal image tag %s", tag));
        }
        String version = tag.split("\\:")[1];
        if(!registerAppList.stream().collect(Collectors.groupingBy(AppModuleVo::getAppName)).get(appName).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version))
        {
            logger.error(String.format("%s has not version %s", appName, version));
            throw new ParamException(String.format("%s has not version %s", appName, version));
        }
        if(!domainMap.containsKey(domainId))
        {
            String domainName = String.format("%s%s", setDefine.getFixedDomainName(), domainId.replaceAll(setDefine.getFixedDomainId(), ""));
            domainInfo = new CCODDomainInfo(domainId, domainName);
            domainMap.put(domainId, domainInfo);
        }
        CCODModuleInfo moduleInfo = new CCODModuleInfo();
        moduleInfo.setVersion(version);
        moduleInfo.setHostIp(pod.getStatus().getHostIP());
//        moduleInfo.setAppId();
        if(StringUtils.isBlank(appName))
        {
            logger.error(String.format("%s of %s is illegal alias for app module", alias, pod.getMetadata().getName()));
            throw new ParamException(String.format("%s of %s is illegal alias for app module", alias, pod.getMetadata().getName()));
        }
        if(moduleInfo == null)
        {

        }
    }

}
