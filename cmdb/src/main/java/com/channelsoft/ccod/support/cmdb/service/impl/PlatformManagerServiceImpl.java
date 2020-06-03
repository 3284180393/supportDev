package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.config.AppDefine;
import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.config.CCODBiz;
import com.channelsoft.ccod.support.cmdb.constant.CCODPlatformStatus;
import com.channelsoft.ccod.support.cmdb.constant.DomainStatus;
import com.channelsoft.ccod.support.cmdb.constant.K8sServiceType;
import com.channelsoft.ccod.support.cmdb.exception.NotSupportAppException;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import com.channelsoft.ccod.support.cmdb.service.IPlatformManagerService;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;
import com.channelsoft.ccod.support.cmdb.vo.CCODDomainInfo;
import com.channelsoft.ccod.support.cmdb.vo.CCODModuleInfo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformTopologyInfo;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Container;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    CCODBiz ccodBiz;

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
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Override
    public PlatformTopologyInfo getPlatformTopologyFromK8s(String platformName, String platformId, String k8sApiUrl, String k8sAuthToken) throws ApiException, ParamException, NotSupportAppException {
        logger.debug(String.format("begin to get %s(%s) topology from %s with authToke=%s", platformId, platformName, k8sApiUrl, k8sAuthToken));
        PlatformPo platform = new PlatformPo();
        platform.setStatus(CCODPlatformStatus.RUNNING.id);
        platform.setPlatformId(platformId);
        platform.setPlatformName(platformName);
        V1Namespace ns = ik8sApiService.queryNamespace(platformId, k8sApiUrl, k8sAuthToken);
        if(!"Active".equals(ns.getStatus().getPhase()))
        {
            logger.error(String.format("status of %s is %s", platformId, ns.getStatus().getPhase()));
            throw new ParamException(String.format("status of %s is %s", platformId, ns.getStatus().getPhase()));
        }
        Map<String, List<AppModuleVo>> registerAppMap = this.appManagerService.queryAllRegisterAppModule().stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        Map<String, List<BizSetDefine>> appSetRelation = this.appManagerService.getAppSetRelation();
        List<UnconfirmedAppModulePo> unconfirmList = new ArrayList<>();
        List<BizSetDefine> setDefineList = this.appManagerService.queryCCODBizSet(false);
        List<V1Pod> podList = ik8sApiService.queryAllPodAtNamespace(platformId, k8sApiUrl, k8sAuthToken);
        List<V1Service> serviceList = ik8sApiService.queryAllServiceAtNamespace(platformId, k8sApiUrl, k8sAuthToken);
        Map<String, DomainPo> domainMap = new HashMap<>();
        Map<String, AssemblePo> srvAsbMap = new HashMap<>();
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
        for(V1Pod pod : podList)
        {
            String[] arr = pod.getMetadata().getName().split("\\-");
            if(arr.length < 2)
            {
                logger.error(String.format("%s is illegal pod name for ccod module", pod.getMetadata().getName()));
                UnconfirmedAppModulePo failPo = new UnconfirmedAppModulePo();
                failPo.setAppName(pod.getMetadata().getName());
                failPo.setHostIp(pod.getStatus().getHostIP());
                failPo.setReason(String.format("%s is illegal pod name for ccod module", pod.getMetadata().getName()));
                unconfirmList.add(failPo);
                continue;
            }
            String alias = arr[0];
            String domainId = arr[1];
            String appName = null;
            Map<String, CCODDomainInfo> domainMap = new HashMap<>();
            for(BizSetDefine setDefine : setDefineList)
            {
                for(AppDefine appDefine : setDefine.getApps())
                {
                    String appRegex = String.format("^%s\\d*", appDefine.getAlias());
                    if(!alias.matches(appRegex))
                        continue;
                    String domainRegex = String.format("^%s(0[1-9]|[1-9]\\d+)", setDefine.getFixedDomainId());
                    if(!domainId.matches(domainRegex))
                        continue;
                    if(!domainMap.containsKey(domainId))
                    {
                        String domainName = String.format("%s%s", setDefine.getFixedDomainName(), domainId.replaceAll(setDefine.getFixedDomainId(), ""));
                        CCODDomainInfo domainInfo = new CCODDomainInfo(domainId, domainName);
                        domainMap.put(domainId, domainInfo);
                    }
                    CCODModuleInfo moduleInfo = new CCODModuleInfo();

                }
            }
        }
        return null;
    }

    private PlatformAppPo getPlatformApp4FromK8s(String namespace, V1Service service, List<V1Pod> svrPodList, Map<String, DomainPo> domainMap, Map<String, List<AssemblePo>> domainAsbMap, List<AppModuleVo> registerAppList) throws ParamException
    {
        logger.debug(String.format("get platform app from service %s with %d pods at %s", service.getMetadata().getName(), svrPodList.size(), namespace));
        if(svrPodList.size() != 1)
            throw new ParamException(String.format("domain service %s select %d pods, current version not support", service.getMetadata().getName(), svrPodList.size()));
        V1Pod pod = svrPodList.get(0);
        Date now = new Date();
        String alias = service.getMetadata().getName().split("\\-")[0];
        String domainId = service.getMetadata().getName().split("\\-")[0];
        BizSetDefine setDefine = null;
        AppDefine appDefine = null;
        for(BizSetDefine set : this.ccodBiz.getSet())
        {
            String domainRegex = String.format("^%s(0[1-9]|[1-9]\\d+)", setDefine.getFixedDomainId());
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
        if(!domainMap.containsKey(domainId))
        {
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
            domainMap.put(domainId, domainPo);
        }
        if(!domainAsbMap.containsKey(domainId))
            domainAsbMap.put(domainId, new ArrayList<>());
        String assembleTag = String.format("%s-%s", pod.getMetadata().getName().split("\\-")[0], pod.getMetadata().getName().split("\\-")[1]);
        if(!domainAsbMap.get(domainId).stream().collect(Collectors.toMap(AssemblePo::getTag, Function.identity())).containsKey(assembleTag))
        {
            AssemblePo assemblePo = new AssemblePo();
            assemblePo.setDomainId(domainId);
            assemblePo.setPlatformId(namespace);
            assemblePo.setStatus(pod.getStatus().getPhase());
            assemblePo.setTag(assembleTag);
            domainAsbMap.get(domainId).add(assemblePo);
        }
        String version = null;
        String versionRegex = String.format("^%s\\:[^\\:]+$", appDefine.getName().toLowerCase());
        for(V1Container container : pod.getSpec().getInitContainers())
        {
            if(container.getImage().matches(versionRegex))
            {
                version = container.getImage().replace("^%s\\:", "").replaceAll("\\-", ":");
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
        if(appMap.containsKey(appDefine.getName()) || !appMap.get(appDefine.getName()).stream().collect(Collectors.toMap(AppModuleVo::getVersion, Function.identity())).containsKey(version))
        {
            logger.error(String.format("%s[%s] not register", appDefine.getName(), version));
            throw new ParamException(String.format("%s[%s] not register", appDefine.getName(), version));
        }
        PlatformAppPo po = new PlatformAppPo();
        po.setAppAlias(alias);
        po.setHostIp(pod.getStatus().getHostIP());
        po.setAppRunner(alias);
        po.setBasePath("/");
        po.setDeployTime(now);
        po.setDomainId(domainId);
        po.setOriginalAlias(alias);
        po.setPlatformId(namespace);
        return po;
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
            logger.debug(String.format("service %s has not selector, so pod is 0"));
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
        if(arr.length != 2)
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
