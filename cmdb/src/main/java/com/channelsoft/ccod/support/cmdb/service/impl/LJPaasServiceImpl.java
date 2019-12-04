package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.constant.CCODPlatformStatus;
import com.channelsoft.ccod.support.cmdb.dao.PlatformMapper;
import com.channelsoft.ccod.support.cmdb.po.PlatformAppPo;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;
import com.channelsoft.ccod.support.cmdb.service.ILJPaasService;
import com.channelsoft.ccod.support.cmdb.vo.*;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.*;
import java.util.function.Function;

/**
 * @ClassName: LJPaasServiceImpl
 * @Author: lanhb
 * @Description: ILJPaasService接口实现类
 * @Date: 2019/12/2 14:33
 * @Version: 1.0
 */
public class LJPaasServiceImpl implements ILJPaasService {

    @Autowired
    PlatformMapper platformMapper;

    @Value("${lj_paas.host_url}")
    private String paasHostUrl;

    @Value("${lj_paas.bk_app_code}")
    private String appCode;

    @Value("${lj_paas.bk_app_secret}")
    private String appSecret;

    @Value("${lj_paas.user_name}")
    private String userName;

    @Value("${develop}")
    private boolean isDevelop;

    private String paasSetModule = "域服务:cmsserver;daengine;DataKeeper;dcproxy;dcs;ddaengine;ddcs;DDSServer;tsrecadsrv;tsr2;UCDServer;ucxserver;EAService;fpsvr;psr;slee;StatSchedule|公共组件:LicenseServer|网关接入:umg|管理门户:agentProxy;cci2;customWebservice;dcms;dcmsDialer;dcmsDialerWebservice;dcmsMonitor;dcmsNEWSG;dcmsRecord;dcmsRecordCXM;dcmssg;dcmsSG;dcmsSR;dcmsStatics;dcmsStaticsIB;dcmsStaticsReport;dcmsStaticsReportfj;dcmsStaticsReportNew;dcmsWebservice;dcmsWebservicespeed;dcmsWebservicespeednew;dcmsWebserviceucds;dcmsx;gls;httpd;IBcustomWebservice;IBsafetymonitor;interfaceAdapter;ivrprocessinterface;omsp;PADnStatistics;PADnStatisticsbilibili;PAreload;portal;safetyMonitor;safetyStatics;safetyStaticsQT;tomcat|对外接口|运营门户:gls|httpd|IBcustomWebs|IBsafetymoni|licen";

    private String paasIdlePoolSetName = "idle pool";

    private String queryBizUrlFmt = "%s/api/c/compapi/v2/cc/search_business/";

    private String queryBizSetUrlFmt = "%s/api/c/compapi/v2/cc/search_set/";

    private String queryHostUrlFmt = "%s/api/c/compapi/v2/cc/search_host/";

    private final static Logger logger = LoggerFactory.getLogger(LJPaasServiceImpl.class);

    @Override
    public LJBizInfo queryBizInfoById(int bizId) throws Exception {
        return null;
    }

    @Override
    public LJBizInfo[] queryBizInfo() throws Exception {
        return new LJBizInfo[0];
    }

//    private boolean generateLJBizInfo(LJBizInfo bkInfo, Map<Integer, List<LJSetInfo>> bizSetMap, Map<Integer, Map<Integer, Map<String, LJHostInfo>>> bizSetHostMap,
//                                  Map<Integer, Map<String, LJModuleInfo>> hostModuleMap, Map<String, PlatformPo> namePlatformMap,
//                                  Map<String, Map<String, Map<String, PlatformAppDeployDetailVo>>> platformHostAppMap)
//    {
//        if(bizSetMap.containsKey(bkInfo.getBizId()))
//        {
//            logger.error(String.format("paas data error : there is not any set for bizId=%s and bizName=%s",
//                    bkInfo.getBizId(), bkInfo.getBizName()));
//            return false;
//        }
//        boolean isNew = false;
//        if(!namePlatformMap.containsKey(bkInfo.getBizName()))
//        {
//            logger.info(String.format("database not contain platformName=%s platform info, so %s is new created platform",
//                    bkInfo.getBizName(), bkInfo.getBizName()));
//            bkInfo.setStatus(CCODPlatformStatus.NEW_CREATE.id);
//            isNew = true;
//        }
//        for(LJSetInfo setInfo : bizSetMap.get(bkInfo.getBizId()))
//        {
//            switch (setInfo.getSetName())
//            {
//                case "idle pool":
//                    bkInfo.setIdlePoolSet(setInfo);
//                    break;
//                case "故障自愈":
//                    bkInfo.setHealingSet(setInfo);
//                    break;
//                case "数据服务模块":
//                    bkInfo.setDataSets(setInfo);
//                    break;
//                case "公共组件":
//                    bkInfo.setPublicModuleSet(setInfo);
//                    break;
//                case "集成平台":
//                    bkInfo.setIntegrationSet(setInfo);
//                    break;
//                case "作业平台":
//                    bkInfo.setJobSet(setInfo);
//                    break;
//                case "配置平台":
//                    bkInfo.setCfgSet(setInfo);
//                    break;
//                case "管控平台":
//                    bkInfo.setControlSet(setInfo);
//                    break;
//                default:
//                    logger.error(String.format("unknown biz set name=%s", setInfo.getSetName()));
//            }
//        }
//        if(bkInfo.getIdlePoolSet() == null)
//        {
//            logger.error(String.format("%s platform has not idle pools set", bkInfo.getBizName()));
//            return false;
//        }
//        if(!bizSetHostMap.containsKey(bkInfo.getBizId()))
//        {
//            logger.warn(String.format("%s platform has not any host resource", bkInfo.getBizName()));
//            bkInfo.setUsedHosts(new LJHostInfo[0]);
//            bkInfo.setIdleHosts(new LJHostInfo[0]);
//        }
//        else
//        {
//            List<LJHostInfo> idleList = new ArrayList<>();
//            List<LJHostInfo> usedList = new ArrayList<>();
//            Map<Integer, Map<String, LJHostInfo>> setHostMap = bizSetHostMap.get(bkInfo.getBizId());
//            for(Map.Entry<Integer, Map<String, LJHostInfo>> entry : setHostMap.entrySet())
//            {
//                if(entry.getKey() != bkInfo.getIdlePoolSet().getSetId() && isNew)
//                {
//                    logger.error(String.format("biz=%d and bizName=%s platform is new create but found apps had been deployed",
//                            bkInfo.getBizId(), bkInfo.getBizName()));
//                    return false;
//                }
//                else if(entry.getKey() == bkInfo.getIdlePoolSet().getSetId())
//                {
//                    idleList.addAll(entry.getValue().values());
//                }
//                else
//                {
//                    if(!platformHostAppMap.containsKey(bkInfo.getBizName()))
//                    {
//                        logger.error(String.format("lj paas report bizId=%d and bizName=%s is used platform but cmdb database not such record",
//                                bkInfo.getBizId(), bkInfo.getBizName()));
//                        return false;
//                    }
//                    Map<String, Map<String, PlatformAppDeployDetailVo>> hostAppMap = platformHostAppMap.get(bkInfo.getBizName());
//                    for(LJHostInfo hostInfo : entry.getValue().values())
//                    {
//                        if(!hostAppMap.containsKey(hostInfo.getHostInnerIp()))
//                        {
//                            logger.error(String.format("lj paas report bizId=%d and bizName=%s and hostIp=%s deploy app but cmdb database has not such record",
//                                    bkInfo.getBizId(), bkInfo.getBizName(), hostInfo.getHostInnerIp()));
//                            return false;
//                        }
//                        if(!hostModuleMap.containsKey(hostInfo.getHostInnerIp()))
//                        {
//                            logger.error(String.format("database report hostIp=%s deploy apps but lj paas report bizId=%d and bizName=%s and hostIp=%s has not app",
//                                    hostInfo.getHostInnerIp(), bkInfo.getBizId(), bkInfo.getBizName(), hostInfo.getHostInnerIp()));
//                            return false;
//                        }
//                        Map<String, PlatformAppDeployDetailVo> appMap = hostAppMap.get(hostInfo.getHostInnerIp());
//                        Map<String, LJModuleInfo> ljModuleMap = hostModuleMap.get(hostInfo.getId());
//                        if(appMap.size() != ljModuleMap.size())
//                        {
//                            logger.error(String.format("data error, lj paas report bizId=%s and bizName=%s and hostIp=%s deploy %d apps but database record deploy %d apps",
//                                    bkInfo.getBizId(), bkInfo.getBizName(), hostInfo.getHostInnerIp(), ljModuleMap.size(), appMap.size()));
//                            return false;
//                        }
//                        for(String appName : appMap.keySet())
//                        {
//                            if(!ljModuleMap.containsKey(appName))
//                            {
//                                logger.error(String.format("data error, lj paas report bizId=%s and bizName=%s and hostIp=%s deploy %s app but database has not such record",
//                                        bkInfo.getBizId(), bkInfo.getBizName(), hostInfo.getHostInnerIp(), appName));
//                                return false;
//                            }
//                            ljModuleMap.get(appName).setVersion(appMap.get(appName).getVersion());
//                            ljModuleMap.get(appName).setCcodVersion(appMap.get(appName).getVersion());
//                        }
//                    }
//                    usedList.addAll(entry.getValue().values());
//                }
//            }
//            logger.info(String.format("bizId=%d and bizName=%s platform status is %d and have %d used host and %d idle hosts",
//                    bkInfo.getBizId(), bkInfo.getBizName(), bkInfo.getStatus(), bkInfo.getUsedHosts().length,
//                    bkInfo.getIdleHosts().length));
//            bkInfo.setUsedHosts(usedList.toArray(new LJHostInfo[0]));
//            bkInfo.setIdleHosts(idleList.toArray(new LJHostInfo[0]));
//        }
//        return true;
//    }

    private List<LJHostResourceInfo> queryLJPaasBKBizInfo(String paasHostUrl, String bkAppCode, String bkAppSecret, String bkUserName) throws Exception
    {
        String queryUrl = String.format(this.queryHostUrlFmt, paasHostUrl);
        logger.debug(String.format("begin to query all bk_biz info from paasUrl=%s : appCode=%s, appSecret=%s and userName=%s",
                queryUrl, bkAppCode, bkAppSecret, bkUserName));
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("bk_app_code", bkAppCode);
        jsonParam.put("bk_app_secret", bkAppSecret);
        jsonParam.put("bk_username", bkUserName);
        List<Map<String, Object>> conditionsList = new ArrayList<>();
        conditionsList.add(generateLJObjectParam("set", new String[0], new String[0]));
        conditionsList.add(generateLJObjectParam("biz", new String[0], new String[0]));
        conditionsList.add(generateLJObjectParam("host", new String[0], new String[0]));
        conditionsList.add(generateLJObjectParam("module", new String[0], new String[0]));
        jsonParam.put("condition", conditionsList);
        String jsonStr = jsonParam.toJSONString();
        StringEntity entity = new StringEntity(jsonParam.toString(),"utf-8");//解决中文乱码问题
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        HttpPost httpPost = new HttpPost(queryUrl);
        httpPost.setEntity(entity);
        HttpClient httpClient = getBasicHttpClient();
        HttpResponse response = httpClient.execute(httpPost);
        // 解析response封装返回对象httpResult
        if (response.getStatusLine().getStatusCode() != 200)
        {
            logger.error(String.format("query paasUrl=%s : appCode=%s, appSecret=%s and userName=%s return errorCode",
                    queryUrl, bkAppCode, bkAppSecret, bkUserName, response.getStatusLine().getStatusCode()));
            throw new Exception(String.format("query paasUrl=%s : appCode=%s, appSecret=%s and userName=%s return errorCode",
                    queryUrl, bkAppCode, bkAppSecret, bkUserName, response.getStatusLine().getStatusCode()));
        }
        String conResult = EntityUtils.toString(response.getEntity(), "utf8");
        JSONObject jsonObject = JSONObject.parseObject(conResult);
        if("success".equals(jsonObject.containsKey("message")))
        {
            logger.error(String.format("paas return error message %s", jsonObject.containsKey("message")));
            throw new Exception(String.format("paas return error message %s", jsonObject.containsKey("message")));
        }
        String data = jsonObject.getJSONObject("data").getString("info");
        List<LJHostResourceInfo> hostResources = JSONArray.parseArray(data, LJHostResourceInfo.class);
        System.out.println(hostResources.size());
//        Map<Integer, LJBizInfo> bkMap = new HashMap<>();
//        Map<Integer, LJSetInfo> setMap = new HashMap<>();
//        Map<Integer, LJHostInfo> hostInfoMap = new HashMap<>();
        return hostResources;
    }


    private List<CCODPlatformInfo> handlePaasResource(List<LJBizInfo> bizList,
                                                      Map<Integer, Map<Integer, LJSetInfo>> bizSetMap,
                                                      List<LJHostResourceInfo> resourceList,
                                                      Map<Integer, PlatformPo> platformMap,
                                                      Map<Integer, PlatformAppPo> appMap)
    {
        Map<Integer, List<LJHostInfo>> setHostMap = new HashMap<>();
        Map<Integer, List<LJModuleInfo>> setModuleMap = new HashMap<>();
        Map<Integer, LJHostInfo> hostMap = new HashMap<>();
        for(LJHostResourceInfo resourceInfo : resourceList)
        {
            int hostId = resourceInfo.getHost().getHostId();
            hostMap.put(hostId, resourceInfo.getHost());
            if(resourceInfo.getSet() != null && resourceInfo.getSet().length > 0)
            {
                for(LJSetInfo setInfo : resourceInfo.getSet())
                {
                    if(!setHostMap.containsKey(setInfo.getSetId()))
                    {
                        setHostMap.put(setInfo.getSetId(), new ArrayList<>());
                    }
                    setHostMap.get(setInfo.getSetId()).add(resourceInfo.getHost());
                }
            }
            if(resourceInfo.getModule() != null && resourceInfo.getModule().length > 0)
            {
                for(LJModuleInfo moduleInfo : resourceInfo.getModule())
                {
                    if(!setModuleMap.containsKey(moduleInfo.getSetId()))
                    {
                        setModuleMap.put(moduleInfo.getSetId(), new ArrayList<>());
                    }
                    setModuleMap.get(moduleInfo.getSetId()).add(moduleInfo);
                }
            }
        }
        List<CCODPlatformInfo> platformInfoList = new ArrayList<>();
        for(LJBizInfo bizInfo : bizList)
        {
            CCODPlatformInfo platformInfo = new CCODPlatformInfo();
            platformInfo.setBizId(bizInfo.getBizId());
            platformInfo.setPlatformName(bizInfo.getBizName());
            List<CCODSetInfo> setList = new ArrayList<>();
            for(LJSetInfo info : bizSetMap.get(bizInfo.getBizId()).values())
            {
                CCODSetInfo setInfo = new CCODSetInfo(info.getSetName());
                setInfo.setBkSetId(info.getSetId());
                setInfo.setBkSetName(info.getSetName());
            }
        }
        if(!isDevelop)
        {

        }
        else
        {

        }
//        Map<Integer, LJBizInfo> platformMap = new HashMap<>();
//        Map<Integer, Map<Integer, LJSetInfo>> setMap = new HashMap<>();
//        Map<Integer, LJHostInfo> hostMap = new HashMap<>();
//        Map<Integer, Map<Integer, LJModuleInfo>> moduleMap = new HashMap<>();
//        for(LJHostResourceInfo resourceInfo : resourceList)
//        {
//            Integer hostId = resourceInfo.getHost().getHostId();
//            hostMap.put(hostId, resourceInfo.getHost());
//            if(resourceInfo.getBiz() != null && resourceInfo.getBiz().length > 0)
//            {
//                for(LJBizInfo info : resourceInfo.getBiz())
//                {
//                    if(!platformMap.containsKey(info.getBizId()))
//                    {
//                        platformMap.put(info.getBizId(), info);
//                    }
//                }
//            }
//            if(resourceInfo.getSet() != null && resourceInfo.getSet().length > 0)
//            {
//                for(LJSetInfo info : resourceInfo.getSet())
//                {
//                    if(!setMap.containsKey(info.getSetId()))
//                    {
//                        setMap.put(info.getSetId(), info);
//                    }
//                }
//            }
//            if(resourceInfo.getModule() != null && resourceInfo.getModule().length > 0)
//            {
//                for(LJModuleInfo info : resourceInfo.getModule())
//                {
//                    if(!moduleMap.containsKey(info.getModuleId()))
//                    {
//                        moduleMap.put(info.getModuleId(), info);
//                    }
//                }
//            }
//        }

        return null;
    }

    private CCODPlatformInfo createNewPlatformInfo(LJBizInfo bizInfo, LJSetInfo idlePoolSet, List<LJHostInfo> idleHosts)
    {
        CCODIdlePoolInfo idlePoolInfo = new CCODIdlePoolInfo(bizInfo.getBizId(), idlePoolSet.getSetId(), idlePoolSet.getSetName());
        for(LJHostInfo bkHost : idleHosts)
        {
            CCODHostInfo host = new CCODHostInfo(bkHost);
            idlePoolInfo.getIdleHosts().add(host);
        }
        List<CCODSetInfo> ccodSetList = new ArrayList<>();
        Map<String, Set<String>> paasSetMap = getAllPaasBizSetModuleMap();
        for(String setName : paasSetMap.keySet())
        {
            CCODSetInfo setInfo = new CCODSetInfo(setName);
            ccodSetList.add(setInfo);
        }
        CCODPlatformInfo platformInfo = new CCODPlatformInfo(bizInfo, CCODPlatformStatus.NEW_CREATE.id, idlePoolInfo, ccodSetList);
        return platformInfo;
    }

    private CCODPlatformInfo generatePlatformInfo( LJBizInfo bizInfo, List<LJSetInfo> bizSetList,
                                         Map<Integer, List<LJHostInfo>> bkSetHostMap, Map<Integer, List<LJModuleInfo>> bkSetModuleMap,
                                         Map<Integer, LJHostInfo> moduleHostMap, PlatformPo platform, List<PlatformAppPo> platformAppList)
    {
        CCODPlatformInfo platformInfo = new CCODPlatformInfo(bizInfo, CCODPlatformStatus.RUNNING.id);
        for(LJSetInfo setInfo : bizSetList)
        {
            if(this.paasIdlePoolSetName.equals(setInfo.getSetName()))
            {
                for(LJHostInfo bkHost : bkSetHostMap.get(setInfo.getSetId()))
                {
                    CCODIdlePoolInfo idlePoolInfo = new CCODIdlePoolInfo(bizInfo.getBizId(), setInfo.getSetId(), setInfo.getSetName());
                    CCODHostInfo host = new CCODHostInfo(bkHost);
                    idlePoolInfo.getIdleHosts().add(host);
                    platformInfo.setIdlePool(idlePoolInfo);
                }
            }
            else
            {
                List<CCODModuleInfo> moduleList = new ArrayList<>();
                for(LJModuleInfo moduleInfo : bkSetModuleMap.get(setInfo.getSetId()))
                {

                }
            }
        }
        return null;
    }


    /**
     * 判断蓝鲸paas平台的biz类型
     * @param bizInfo paas平台biz信息
     * @param bizSetList paas平台同biz相关的set信息
     * @param platformMap 数据库存储的已有平台信息
     * @return 0:同cmdb无关的biz,1：已有的同cmdb相关的biz,2：可能同cmdb相关的新建平台
     */
    private int checkBizType(LJBizInfo bizInfo, List<LJSetInfo> bizSetList, Map<Integer, PlatformPo> platformMap)
    {
        Map<String, Set<String>> wantSetAppMap = getAllPaasBizSetModuleMap();
        if(bizSetList.size() != 1 && bizSetList.size() != wantSetAppMap.size())
        {
            logger.info(String.format("bizId=%d and bizName=%s biz has %d bizSet not equal 1 or %d, so it not relative to cmdb and return 0",
                    bizInfo.getBizId(), bizInfo.getBizName(), bizSetList.size(), wantSetAppMap.size()));
            return 0;
        }
        if(bizSetList.size() == 1)
        {
            if(platformMap.containsKey(bizInfo.getBizId()))
            {
                logger.error(String.format("data error : bizId=%d and bizName=%s biz has only one set but it has been saved in database",
                        bizInfo.getBizId(), bizInfo.getBizName()));
                return 0;
            }
            else if(!this.paasIdlePoolSetName.equals(bizSetList.get(0)))
            {
                logger.warn(String.format("bizId=%d and bizName=%s biz has only one set but it's name not %s'",
                        bizInfo.getBizId(), bizInfo.getBizName(), this.paasIdlePoolSetName));
                return 0;
            }
            else
            {
                logger.info(String.format("bizId=%d and bizName=%s biz has one set with name=%s and it not saved in database so it maybe a new create biz relative to cmdb, so return 2",
                        bizInfo.getBizId(), bizInfo.getBizName(), this.paasIdlePoolSetName));
                return 2;
            }
        }
        else
        {
            if(!platformMap.containsKey(bizInfo.getBizId()))
            {
                logger.warn(String.format("bizId=%d and bizName=%s biz has %d set but it has been saved in database, so return 0'",
                        bizInfo.getBizId(), bizInfo.getBizName(), bizSetList.size()));
                return 0;
            }
            for(LJSetInfo setInfo : bizSetList)
            {
                if(!wantSetAppMap.containsKey(setInfo.getSetName()))
                {
                    logger.warn(String.format("setName=%s of bizId=%d and bizName=%s biz is not a set name for cmdb biz, so return 0",
                            setInfo.getSetName(), bizInfo.getBizId(), bizInfo.getBizName()));
                    return 0;
                }
            }
        }
        logger.info(String.format("bizId=%d and bizName=%s is an old biz for cmd, so return 1",
                bizInfo.getBizId(), bizInfo.getBizName()));
        return 1;
    }

    private Map<String, Set<String>> getAllPaasBizSetModuleMap()
    {
        String[] arr = this.paasSetModule.split("\\|");
        Map<String, Set<String>> map = new HashMap<>();
        List<String> setNameList = new ArrayList<>();
        setNameList.add(this.paasIdlePoolSetName);
        for(int i = 0; i < arr.length; i++)
        {
            String[] tmpArr = arr[i].split("\\:");
            Set<String> moduleNameSet = new HashSet<>();
            for(int j = 1; j < tmpArr.length; j++)
            {
                moduleNameSet.add(tmpArr[i]);
            }
            map.put(tmpArr[0], moduleNameSet);
        }
        return map;
    }

    private Map<String, Object> generateLJObjectParam(String objId, String[] fields, String[] condition)
    {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("bk_obj_id", objId);
        paramMap.put("fields", fields);
        paramMap.put("condition", condition);
        return paramMap;
    }

    private CloseableHttpClient getBasicHttpClient() {
        // 创建HttpClientBuilder
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        // 设置BasicAuth
        CredentialsProvider provider = new BasicCredentialsProvider();
        // Set the default credentials provider
        httpClientBuilder.setDefaultCredentialsProvider(provider);
        // HttpClient
        CloseableHttpClient closeableHttpClient = httpClientBuilder.build();
        return closeableHttpClient;
    }

    private String getDomainNameForApp(String appName, String domainName) throws Exception
    {
        String domainSeviceModule = "cmsserver|daengine|DataKeeper|dcproxy|dcs|ddaengine|ddcs|DDSServer|tsrecadsrv|tsr2|UCDServer|ucxserver|EAService|fpsvr|psr|slee|StatSchedule";
        String publicModuleModule = "LicenseServer";
        String gatewayModule = "umg";
        String managerPortalModule = "agentProxy|cci2|customWebservice|dcms|dcmsDialer|dcmsDialerWebservice|dcmsMonitor|dcmsNEWSG|dcmsRecord|dcmsRecordCXM|dcmssg|dcmsSG|dcmsSR|dcmsStatics|dcmsStaticsIB|dcmsStaticsReport|dcmsStaticsReportfj|dcmsStaticsReportNew|dcmsWebservice|dcmsWebservicespeed|dcmsWebservicespeednew|dcmsWebserviceucds|dcmsx|gls|httpd|IBcustomWebservice|IBsafetymonitor|interfaceAdapter|ivrprocessinterface|omsp|PADnStatistics|PADnStatisticsbilibili|PAreload|portal|safetyMonitor|safetyStatics|safetyStaticsQT|tomcat";
        String interfaceModule = "unknown";
        String supportPortalModule = "gls|httpd|IBcustomWebs|IBsafetymoni|licen";
        String compareStr = "|" + appName + "|";
        if(String.format("|%s|", domainSeviceModule).indexOf(compareStr) >= 0)
            return domainName;
        else if(String.format("|%s|", publicModuleModule).indexOf(compareStr) >= 0)
            return "公共组件";
        else if(String.format("|%s|", gatewayModule).indexOf(compareStr) >= 0)
            return "网关接入";
        else if(String.format("|%s|", managerPortalModule).indexOf(compareStr) >= 0)
            return "管理门户";
        else if(String.format("|%s|", interfaceModule).indexOf(compareStr) >= 0)
            return "对外接口";
        else if(String.format("|%s|", supportPortalModule).indexOf(compareStr) >= 0)
            return "运营门户";
        logger.error(String.format("unknown appName=%s", appName));
        throw new Exception(String.format("unknown appName=%s", appName));
    }

    @Test
    public void jsonTest()
    {
        try
        {
            String setArrStr = "[{\"bk_biz_id\":10,\"TopModuleName\":\"##idlepool\",\"bk_service_status\":\"1\",\"description\":\"\",\"bk_set_env\":\"3\",\"default\":1,\"bk_supplier_account\":\"0\",\"bk_capacity\":null,\"create_time\":\"2019-11-04T12:19:56.327+08:00\",\"bk_set_name\":\"idlepool\",\"bk_set_id\":30,\"bk_set_desc\":\"\",\"bk_parent_id\":10,\"last_time\":\"2019-11-04T12:19:56.327+08:00\"}]";
            List<LJSetInfo> setList = JSONArray.parseArray(setArrStr, LJSetInfo.class);
            System.out.println(setList.size());
            String data = "[{\"biz\":[{\"life_cycle\":\"2\",\"create_time\":\"2019-06-04T11:16:44.87+08:00\",\"bk_biz_id\":2,\"bk_biz_developer\":\"\",\"language\":\"1\",\"bk_supplier_account\":\"0\",\"time_zone\":\"Asia/Shanghai\",\"bk_biz_tester\":\"\",\"operator\":\"\",\"bk_biz_name\":\"蓝鲸\",\"default\":0,\"bk_supplier_id\":0,\"last_time\":\"2019-06-20T10:04:44.932+08:00\",\"bk_biz_maintainer\":\"admin,luoxin,wangyf,lanhb\",\"bk_biz_productor\":\"\"}],\"set\":[{\"create_time\":\"2019-06-04T11:16:45.003+08:00\",\"bk_parent_id\":2,\"bk_biz_id\":2,\"bk_set_env\":\"3\",\"TopModuleName\":\"##公共组件\",\"description\":\"\",\"bk_supplier_account\":\"0\",\"bk_set_id\":5,\"default\":0,\"last_time\":\"2019-06-04T11:16:45.003+08:00\",\"bk_set_desc\":\"\",\"bk_service_status\":\"1\",\"bk_set_name\":\"公共组件\"},{\"create_time\":\"2019-06-04T11:16:45.048+08:00\",\"bk_parent_id\":2,\"bk_biz_id\":2,\"bk_set_env\":\"3\",\"TopModuleName\":\"##集成平台\",\"description\":\"\",\"bk_supplier_account\":\"0\",\"bk_set_id\":6,\"default\":0,\"last_time\":\"2019-06-04T11:16:45.048+08:00\",\"bk_set_desc\":\"\",\"bk_service_status\":\"1\",\"bk_set_name\":\"集成平台\"}],\"module\":[{\"create_time\":\"2019-06-04T11:16:45.036+08:00\",\"bk_parent_id\":5,\"bk_biz_id\":2,\"TopModuleName\":\"##公共组件##consul\",\"bk_supplier_account\":\"0\",\"bk_set_id\":5,\"bk_module_id\":19,\"operator\":\"\",\"bk_bak_operator\":\"\",\"default\":0,\"bk_module_name\":\"consul\",\"last_time\":\"2019-06-04T11:16:45.036+08:00\",\"bk_module_type\":\"1\"},{\"create_time\":\"2019-06-04T11:16:45.026+08:00\",\"bk_parent_id\":5,\"bk_biz_id\":2,\"TopModuleName\":\"##公共组件##mysql\",\"bk_supplier_account\":\"0\",\"bk_set_id\":5,\"bk_module_id\":16,\"operator\":\"\",\"bk_bak_operator\":\"\",\"default\":0,\"bk_module_name\":\"mysql\",\"last_time\":\"2019-06-04T11:16:45.026+08:00\",\"bk_module_type\":\"1\"},{\"create_time\":\"2019-06-04T11:16:45.019+08:00\",\"bk_parent_id\":5,\"bk_biz_id\":2,\"TopModuleName\":\"##公共组件##nginx\",\"bk_supplier_account\":\"0\",\"bk_set_id\":5,\"bk_module_id\":14,\"operator\":\"\",\"bk_bak_operator\":\"\",\"default\":0,\"bk_module_name\":\"nginx\",\"last_time\":\"2019-06-04T11:16:45.019+08:00\",\"bk_module_type\":\"1\"},{\"create_time\":\"2019-06-04T11:16:45.041+08:00\",\"bk_parent_id\":5,\"bk_biz_id\":2,\"TopModuleName\":\"##公共组件##zookeeper\",\"bk_supplier_account\":\"0\",\"bk_set_id\":5,\"bk_module_id\":20,\"operator\":\"\",\"bk_bak_operator\":\"\",\"default\":0,\"bk_module_name\":\"zookeeper\",\"last_time\":\"2019-06-04T11:16:45.041+08:00\",\"bk_module_type\":\"1\"},{\"create_time\":\"2019-06-04T11:16:45.066+08:00\",\"bk_parent_id\":6,\"bk_biz_id\":2,\"TopModuleName\":\"##集成平台##appo\",\"bk_supplier_account\":\"0\",\"bk_set_id\":6,\"bk_module_id\":26,\"operator\":\"\",\"bk_bak_operator\":\"\",\"default\":0,\"bk_module_name\":\"appo\",\"last_time\":\"2019-06-04T11:16:45.066+08:00\",\"bk_module_type\":\"1\"}],\"host\":{\"bk_os_bit\":\"64-bit\",\"bk_host_outerip\":\"\",\"bk_comment\":\"\",\"docker_client_version\":\"\",\"bk_sn\":\"\",\"bk_host_innerip\":\"10.130.41.39\",\"bk_supplier_account\":\"0\",\"import_from\":\"2\",\"bk_os_version\":\"7.2.1511\",\"bk_mac\":\"52:54:00:05:b4:56\",\"bk_mem\":15887,\"bk_os_name\":\"linux centos\",\"last_time\":\"2019-08-05T18:24:50.228+08:00\",\"bk_host_id\":1,\"bk_host_name\":\"localhost.localdomain\",\"bk_cpu_module\":\"QEMU Virtual CPU version (cpu64-rhel6)\",\"bk_outer_mac\":\"\",\"docker_server_version\":\"\",\"create_time\":\"2019-06-04T12:02:32.522+08:00\",\"bk_asset_id\":\"\",\"bk_disk\":191,\"bk_os_type\":\"1\",\"bk_cpu\":4,\"bk_cloud_id\":[{\"bk_obj_name\":\"\",\"bk_obj_icon\":\"\",\"bk_inst_name\":\"default area\",\"bk_obj_id\":\"plat\",\"id\":\"0\",\"bk_inst_id\":0}],\"bk_cpu_mhz\":2599}}]";
            List<LJHostResourceInfo> resourceInfos = JSONArray.parseArray(data, LJHostResourceInfo.class);
            System.out.println(JSONArray.toJSONString(resourceInfos));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Test
    public void hostQueryTest()
    {
        try
        {
            queryLJPaasBKBizInfo("http://paas.ccod.com:80", "wyffirstsaas", "8a4c0887-ca15-462b-8804-8bedefe1f352", "admin");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
