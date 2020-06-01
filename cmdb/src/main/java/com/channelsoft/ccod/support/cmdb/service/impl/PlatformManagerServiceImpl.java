package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.config.AppDefine;
import com.channelsoft.ccod.support.cmdb.config.BizSetDefine;
import com.channelsoft.ccod.support.cmdb.config.CCODBiz;
import com.channelsoft.ccod.support.cmdb.constant.CCODPlatformStatus;
import com.channelsoft.ccod.support.cmdb.exception.NotSupportAppException;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;
import com.channelsoft.ccod.support.cmdb.k8s.service.IK8sApiService;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;
import com.channelsoft.ccod.support.cmdb.po.UnconfirmedAppModulePo;
import com.channelsoft.ccod.support.cmdb.service.IAppManagerService;
import com.channelsoft.ccod.support.cmdb.service.IPlatformManagerService;
import com.channelsoft.ccod.support.cmdb.vo.AppModuleVo;
import com.channelsoft.ccod.support.cmdb.vo.CCODDomainInfo;
import com.channelsoft.ccod.support.cmdb.vo.CCODModuleInfo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformTopologyInfo;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.models.V1Namespace;
import io.kubernetes.client.openapi.models.V1Pod;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public PlatformTopologyInfo getPlatformTopologyFromK8s(String platformName, String platformId, String k8sApiUrl, String k8sAuthToken) throws ApiException {
        logger.debug(String.format("begin to get %s(%s) topology from %s with authToke=%s", platformId, platformName, k8sApiUrl, k8sAuthToken));
        PlatformPo platform = new PlatformPo();
        platform.setStatus(CCODPlatformStatus.RUNNING.id);
        platform.setPlatformId(platformId);
        platform.setPlatformName(platformName);
        V1Namespace ns = ik8sApiService.queryNamespace(platformId, k8sApiUrl, k8sAuthToken);
        if(!"Active".equals(ns.getStatus().getPhase()))
        {
            logger.error(String.format("status of %s is %s", platformId, ns.getStatus().getPhase()));
            return null;
        }
        Map<String, List<AppModuleVo>> registerAppMap = this.appManagerService.queryAllRegisterAppModule().stream().collect(Collectors.groupingBy(AppModuleVo::getAppName));
        Map<String, List<BizSetDefine>> appSetRelation = this.appManagerService.getAppSetRelation();
        List<UnconfirmedAppModulePo> unconfirmList = new ArrayList<>();
        List<BizSetDefine> setDefineList = this.appManagerService.queryCCODBizSet(false);
        List<V1Pod> podList = ik8sApiService.queryAllPodAtNamespace(platformId, k8sApiUrl, k8sAuthToken);
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
