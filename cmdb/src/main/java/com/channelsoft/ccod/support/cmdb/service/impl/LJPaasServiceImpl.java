package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.constant.CCODPlatformStatus;
import com.channelsoft.ccod.support.cmdb.dao.PlatformAppDeployDetailMapper;
import com.channelsoft.ccod.support.cmdb.dao.PlatformMapper;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.service.ILJPaasService;
import com.channelsoft.ccod.support.cmdb.vo.*;
import org.apache.commons.lang3.StringUtils;
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

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Autowired
    PlatformAppDeployDetailMapper platformAppDeployDetailMapper;

    @Value("${lj_paas.host_url}")
    private String paasHostUrl;

    @Value("${lj_paas.bk_app_code}")
    private String appCode;

    @Value("${lj_paas.bk_app_secret}")
    private String appSecret;

    @Value("${lj_paas.user_name}")
    private String userName;

    @Value("${nexus.host_url}")
    private String nexusHostUrl;

    @Value("${develop}")
    private boolean isDevelop;

    @Value("${lj_paas.set_module}")
    private String paasSetModule;

    @Value("${lj_paas.exclude_biz}")
    private String excludeBiz;

    private String paasIdlePoolSetName = "idle pool";

    private String queryBizUrlFmt = "%s/api/c/compapi/v2/cc/search_business/";

    private String queryBizSetUrlFmt = "%s/api/c/compapi/v2/cc/search_set/";

    private String queryHostUrlFmt = "%s/api/c/compapi/v2/cc/search_host/";

    private String downloadUrlFmt = "%s/%s/%s";

    private final static Logger logger = LoggerFactory.getLogger(LJPaasServiceImpl.class);

    private Map<String, SetDomain> supportSetDomainMap;

    private Set<Integer> exludeBizSet;


    @PostConstruct
    void init() throws Exception
    {
        supportSetDomainMap  = new ConcurrentHashMap<>();
        String[] setArr = this.paasSetModule.split("\\|");
        for(String str : setArr)
        {
            if(StringUtils.isBlank(str))
                continue;
            String[] arr = str.split("\\:");
            String[] setDomArr = arr[0].split("@");
            SetDomain setDomain = new SetDomain();
            setDomain.setName = setArr[0];
            if(setArr.length > 1)
            {
                setDomain.domainId = setDomArr[1];
                setDomain.domainName = setDomArr[1];
            }
            if(arr.length > 0)
            {
                setDomain.moduleNameSet = new HashSet<>(Arrays.asList(arr[1].split(";")));
            }
            else
            {
                setDomain.moduleNameSet = new HashSet<>();
            }
            this.supportSetDomainMap.put(setDomain.setName, setDomain);
        }
        logger.info(String.format("ccod set, domain and app relation is : %s", JSONObject.toJSONString(this.supportSetDomainMap)));
        this.exludeBizSet = new HashSet<>();
        if(StringUtils.isNotBlank(this.excludeBiz))
        {
            String[] bizIds = this.excludeBiz.split(";");
            for(String bizId : bizIds)
            {
                if(StringUtils.isNotBlank(bizId))
                {
                    this.exludeBizSet.add(Integer.parseInt(bizId));
                }
            }
        }
        logger.info(String.format("exclude biz=%s", JSONArray.toJSONString(this.exludeBizSet)));
    }

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

    private List<LJBizInfo> queryLJPaasAllBiz(String paasHostUrl, String bkAppCode, String bkAppSecret, String bkUserName) throws Exception
    {
        String queryUrl = String.format(this.queryBizUrlFmt, paasHostUrl);
        logger.debug(String.format("begin to query all biz info from paasUrl=%s : appCode=%s, appSecret=%s and userName=%s",
                queryUrl, bkAppCode, bkAppSecret, bkUserName));
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("bk_app_code", bkAppCode);
        jsonParam.put("bk_app_secret", bkAppSecret);
        jsonParam.put("bk_username", bkUserName);
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
        List<LJBizInfo> bizList = JSONArray.parseArray(data, LJBizInfo.class);
        System.out.println(bizList.size());
//        Map<Integer, LJBizInfo> bkMap = new HashMap<>();
//        Map<Integer, LJSetInfo> setMap = new HashMap<>();
//        Map<Integer, LJHostInfo> hostInfoMap = new HashMap<>();
        return bizList;
    }

    private List<LJSetInfo> queryLJPaasBizSet(int bizId, String paasHostUrl, String bkAppCode, String bkAppSecret, String bkUserName) throws Exception
    {
        String queryUrl = String.format(this.queryBizSetUrlFmt, paasHostUrl);
        logger.debug(String.format("begin to query all set of bizId=%d from paasUrl=%s : appCode=%s, appSecret=%s and userName=%s",
                bizId, queryUrl, bkAppCode, bkAppSecret, bkUserName));
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("bk_app_code", bkAppCode);
        jsonParam.put("bk_app_secret", bkAppSecret);
        jsonParam.put("bk_username", bkUserName);
        jsonParam.put("bk_biz_id", bizId + "");
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
        List<LJSetInfo> setList = JSONArray.parseArray(data, LJSetInfo.class);
        System.out.println(setList.size());
//        Map<Integer, LJBizInfo> bkMap = new HashMap<>();
//        Map<Integer, LJSetInfo> setMap = new HashMap<>();
//        Map<Integer, LJHostInfo> hostInfoMap = new HashMap<>();
        return setList;
    }

    private List<LJHostResourceInfo> queryLJPaasBKHostResource(String paasHostUrl, String bkAppCode, String bkAppSecret, String bkUserName) throws Exception
    {
        String queryUrl = String.format(this.queryHostUrlFmt, paasHostUrl);
        logger.debug(String.format("begin to query all bk_biz info from paasUrl=%s : appCode=%s, appSecret=%s and userName=%s",
                queryUrl, bkAppCode, bkAppSecret, bkUserName));
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("bk_app_code", bkAppCode);
        jsonParam.put("bk_app_secret", bkAppSecret);
        jsonParam.put("bk_username", bkUserName);
        List<Map<String, Object>> conditionsList = new ArrayList<>();
        conditionsList.add(generateLJObjectParam("set", new String[0], new HashMap[0]));
        conditionsList.add(generateLJObjectParam("biz", new String[0], new HashMap[0]));
        conditionsList.add(generateLJObjectParam("host", new String[0], new HashMap[0]));
        conditionsList.add(generateLJObjectParam("module", new String[0], new HashMap[0]));
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


    private List<LJHostInfo> queryIdleHost(int bizId, int idlePoolSetId, String paasHostUrl, String bkAppCode, String bkAppSecret, String bkUserName) throws Exception
    {
        String queryUrl = String.format(this.queryHostUrlFmt, paasHostUrl);
        logger.debug(String.format("begin to query idle host for bizId=%s and idlePoolSetId=%d from paasUrl=%s : appCode=%s, appSecret=%s and userName=%s",
                bizId, idlePoolSetId, queryUrl, bkAppCode, bkAppSecret, bkUserName));
        JSONObject jsonParam = new JSONObject();
        jsonParam.put("bk_app_code", bkAppCode);
        jsonParam.put("bk_app_secret", bkAppSecret);
        jsonParam.put("bk_username", bkUserName);
        jsonParam.put("bk_biz_id", bizId + "");
        List<Map<String, Object>> conditionsList = new ArrayList<>();
        List<Map<String, Object>> setParams = new ArrayList<>();
        Map<String, Object> setParam = new HashMap<>();
        setParam.put("field", "bk_set_id");
        setParam.put("operator", "$eq");
        setParam.put("value", idlePoolSetId);
        setParams.add(setParam);
        conditionsList.add(generateLJObjectParam("set", new String[0], setParams.toArray(new Map[0])));
        conditionsList.add(generateLJObjectParam("biz", new String[0], new HashMap[0]));
        conditionsList.add(generateLJObjectParam("host", new String[0], new HashMap[0]));
        conditionsList.add(generateLJObjectParam("module", new String[0], new HashMap[0]));
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
        List<LJHostInfo> idleHosts = new ArrayList<>();
        for(LJHostResourceInfo hostResourceInfo : hostResources)
        {
            idleHosts.add(hostResourceInfo.getHost());
        }
        return idleHosts;
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

    private CCODPlatformInfo getCCODPlatformInfo(PlatformPo platform, LJSetInfo idlePoolSet, List<PlatformAppDeployDetailVo> deployApps, List<LJHostInfo> idleHosts)
    {
        CCODIdlePoolInfo idlePool = new CCODIdlePoolInfo(idlePoolSet);
        for(LJHostInfo bkHost : idleHosts)
        {
            CCODHostInfo host = new CCODHostInfo(bkHost);
            idlePool.getIdleHosts().add(host);
        }
        Map<Integer, CCODSetInfo> setMap = new HashMap<>();
        Map<Integer, List<PlatformAppDeployDetailVo>> setAppMap = new HashMap<>();
        Map<Integer, List<DomainPo>> setDomainMap = new HashMap<>();
        Map<Integer, CCODDomainInfo> domainMap = new HashMap<>();
        for(PlatformAppDeployDetailVo deployApp : deployApps)
        {
            int setId = deployApp.getSetId();
            if(!setMap.containsKey(deployApp.getSetId()))
            {
                LJSetInfo bkSet = new LJSetInfo();
                bkSet.setBizId(platform.getBkBizId());
                bkSet.setSetId(setId);
                bkSet.setSetName(deployApp.getSetName());
                CCODSetInfo ccodSet = new CCODSetInfo(bkSet);
                setMap.put(setId, ccodSet);
                setAppMap.put(bkSet.getSetId(), new ArrayList<>());
            }
            CCODModuleInfo module = new CCODModuleInfo(deployApp, this.nexusHostUrl, this.downloadUrlFmt);
            if(!domainMap.containsKey(deployApp.getDomId()))
            {
                CCODDomainInfo domain = new CCODDomainInfo(setId, deployApp.getDomId(), deployApp.getDomainId(), deployApp.getDomainName());
                domain.setDomId(deployApp.getDomId());
                domain.setDomainName(deployApp.getDomainName());
                domainMap.put(domain.getDomId(), domain);
                setDomainMap.put(domain.getDomId(), new ArrayList<>());
            }
            domainMap.get(deployApp.getDomId()).getModules().add(module);
        }
        LJBizInfo bizInfo = new LJBizInfo();
        bizInfo.setBizId(platform.getBkBizId());
        bizInfo.setBizName(platform.getPlatformName());
        CCODPlatformInfo ccodPlatformInfo = new CCODPlatformInfo(bizInfo, CCODPlatformStatus.RUNNING.id,
                idlePool, new ArrayList<>(setMap.values()));
        return ccodPlatformInfo;
    }

    private CCODPlatformInfo generatePlatformInfo( PlatformPo platform,
                                                   LJBizInfo bizInfo,
                                                   List<LJSetInfo> setList,
                                                   List<LJHostInfo> idleHostList,
                                                   List<PlatformAppDeployDetailVo> deloyAppList)
    {
        Map<Integer, List<PlatformAppDeployDetailVo>> setAppMap = deloyAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getBkSetId));
        if(!bizInfo.getBizName().equals(platform.getPlatformName()))
        {
            logger.error(String.format("bizName=%s of bizId=%d not equal with platformName=%s of bizId=%d and platId=%s",
                    bizInfo.getBizName(), bizInfo.getBizId(), platform.getBkBizId(), platform.getPlatformName()));
            return null;
        }
        if(intSetCompare(moduleHostMap.keySet(), platAppMap.keySet()))
        {
            logger.error(String.format("modules at paas are not consistent with apps saved in cmdb database"));
            return null;
        }
        CCODPlatformInfo platformInfo = new CCODPlatformInfo(bizInfo, CCODPlatformStatus.RUNNING.id);
        for(LJSetInfo setInfo : setList)
        {
            if(this.paasIdlePoolSetName.equals(setInfo.getSetName()))
            {
                for(LJHostInfo bkHost : bkSetHostMap.get(setInfo.getSetId()))
                {
                    CCODIdlePoolInfo idlePoolInfo = new CCODIdlePoolInfo(bizInfo.getBizId(), setInfo, idleHostList);
                    platformInfo.setIdlePool(idlePoolInfo);
                }
            }
            else
            {
                CCODSetInfo ccodSet = new CCODSetInfo(setInfo);
                List<CCODModuleInfo> moduleList = new ArrayList<>();
                for(LJModuleInfo bkModule : bkSetModuleMap.get(setInfo.getSetId()))
                {
                    LJHostInfo bkHost = moduleHostMap.get(bkModule.getModuleId());
                    PlatformAppDeployDetailVo detailVo = platAppMap.get(bkModule.getModuleId());
                    CCODModuleInfo moduleInfo = new CCODModuleInfo(bkModule);
                    moduleInfo.setPlatformAppId(detailVo.getPlatformAppId());
                    if(!bkModule.getModuleName().equals(detailVo.getAppAlias()))
                    {
                        logger.error(String.format("bkModuleId=%d, bkModuleName=%s and platformAppId=%s app's bkModuleName=%s and appAlias=%s, not equal",
                                bkModule.getModuleId(), bkModule.getModuleName(), detailVo.getPlatformAppId(), detailVo.getAppAlias()));
                        return null;
                    }
                    if(bkHost.getHostInnerIp().equals(detailVo.getHostIp()))
                    {
                        logger.error(String.format("bkModuleId=%d, bkModuleName=%s, and platformAppId=%s app's bkHostIp=%s and appHostIp=%s, not equal",
                                bkModule.getModuleId(), bkModule.getModuleName(), bkHost.getHostInnerIp(), detailVo.getHostIp()));
                        return null;
                    }
                    moduleInfo.setHostIp(moduleHostMap.get(bkModule.getModuleId()).getHostInnerIp());
                    moduleInfo.setVersion(detailVo.getVersion());
                    moduleInfo.setVersionControl(detailVo.getVersionControl());
                    String pkgUrl = String.format(this.downloadUrlFmt, this.nexusHostUrl,
                            detailVo.getInstallPackage().getNexusRepository(),
                            detailVo.getInstallPackage().getNexusDirectory(),
                            detailVo.getInstallPackage().getFileName());
                    DownloadFileInfo pkg = new DownloadFileInfo(pkgUrl, detailVo.getInstallPackage().getMd5());
                    moduleInfo.setInstallPackage(pkg);
                    List<DownloadFileInfo> cfgList = new ArrayList<>();
                    for(PlatformAppCfgFilePo cfg : detailVo.getCfgs())
                    {
                        String downloadUrl = String.format(this.downloadUrlFmt, this.nexusHostUrl, cfg.getNexusRepository(),
                                cfg.getNexusDirectory(), cfg.getFileName());
                        DownloadFileInfo cfgFile = new DownloadFileInfo(downloadUrl, cfg.getMd5());
                        cfgList.add(cfgFile);
                    }
                    moduleInfo.setCfgs(cfgList);
                    moduleList.add(moduleInfo);
                    SetDomain sd = getDomainNameForApp(detailVo.getAppName(), detailVo.getDomainId(), detailVo.getDomainName());
                    addModuleToSetDomain(ccodSet, moduleInfo, sd);
                }
                platformInfo.getSets().add(ccodSet);
            }
        }
        return platformInfo;
    }

    private boolean intSetCompare(Set<Integer> srcSet, Set<Integer> dstSet)
    {
        if(srcSet.size() != dstSet.size())
        {
            logger.info(String.format("srcSet size=%d and dstSet size=%d, not equal",
                    srcSet.size(), dstSet.size()));
            return false;
        }
        for(Integer key : srcSet)
        {
            if(!dstSet.contains(key))
            {
                logger.info(String.format("the value of set not equal, srcSet=%s and dstSet=%s",
                        JSONArray.toJSONString(srcSet), JSONArray.toJSONString(dstSet)));
                return false;

            }
        }
        logger.info(String.format("srcSet=dstSet : %s", JSONArray.toJSONString(srcSet)));
        return true;
    }

    private boolean addModuleToSetDomain(CCODSetInfo ccodSet, CCODModuleInfo ccodModule, SetDomain sd)
    {
        for(CCODDomainInfo domain : ccodSet.getDomains())
        {
            if(domain.getDomainId().equals(sd.domainId))
            {
                domain.getModules().add(ccodModule);
                return true;
            }
        }
        CCODDomainInfo domainInfo = new CCODDomainInfo(ccodSet.getBkSetId(), 0, sd.domainId, sd.domainName);
        domainInfo.getModules().add(ccodModule);
        ccodSet.getDomains().add(domainInfo);
        return true;
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

    private Map<String, Object> generateLJObjectParam(String objId, String[] fields, Map<String, Object>[] condition)
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

    private SetDomain getDomainNameForApp(String appName, String domainId, String domainName)
    {
        for(SetDomain sd : this.supportSetDomainMap.values())
        {
            if(sd.moduleNameSet.contains(appName))
            {
                SetDomain setDomain = new SetDomain();
                setDomain.setName = sd.setName;
                if(StringUtils.isBlank(sd.domainId))
                {
                    setDomain.domainId = domainId;
                    setDomain.domainName = domainName;
                }
                else
                {
                    setDomain.domainId = sd.domainId;
                    setDomain.domainName = sd.domainName;
                }
                return setDomain;
            }
        }
        return null;
    }

    @Override
    public CCODPlatformInfo[] queryAllCCODBiz() throws Exception {
        logger.info(String.format("begin to query all biz platform info for ccod"));
        List<PlatformPo> platformList = platformMapper.select(1);
        List<PlatformAppDeployDetailVo> deployAppList = platformAppDeployDetailMapper.selectPlatformApps(null,
                null, null,null);
        List<LJBizInfo> bizList = queryLJPaasAllBiz(this.paasHostUrl, this.appCode, this.appSecret, this.userName);
        Map<Integer, List<PlatformAppDeployDetailVo>> bizAppMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo :: getBkBizId));
        Map<Integer, PlatformPo> platformMap = platformList.stream().collect(Collectors.toMap(PlatformPo::getBkBizId, Function.identity()));
        for(LJBizInfo biz : bizList)
        {
            if(this.exludeBizSet.contains(biz.getBizId()))
            {
                logger.info(String.format("bizId=%d is in exclude set", biz.getBizId()));
                continue;
            }
            List<LJSetInfo> setList = this.queryLJPaasBizSet(biz.getBizId(), this.paasHostUrl, this.appCode,
                    this.appSecret, this.userName);
            int bizType = checkBizType(biz, setList, platformMap);
            if(bizType == 2)
            {
                List<LJHostInfo> idleHostList = queryIdleHost(biz.getBizId(), setList.get(0).getSetId(),
                        this.paasHostUrl, this.appCode, this.appSecret, this.userName);
                CCODPlatformInfo platformInfo = createNewPlatformInfo(biz, setList.get(0), idleHostList);
            }
            else if(bizType == 1)
            {

            }
        }
        return new CCODPlatformInfo[0];
    }

    class SetDomain
    {
        public String setName;

        public String domainName;

        public String domainId;

        public Set<String> moduleNameSet;
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
//            queryLJPaasBKHostResource("http://paas.ccod.com:80", "wyffirstsaas", "8a4c0887-ca15-462b-8804-8bedefe1f352", "admin");
            queryIdleHost(10, 30, "http://paas.ccod.com:80", "wyffirstsaas", "8a4c0887-ca15-462b-8804-8bedefe1f352", "admin");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
