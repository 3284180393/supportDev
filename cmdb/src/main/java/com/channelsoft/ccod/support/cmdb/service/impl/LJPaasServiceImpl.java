package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.constant.CCODPlatformStatus;
import com.channelsoft.ccod.support.cmdb.dao.PlatformAppDeployDetailMapper;
import com.channelsoft.ccod.support.cmdb.dao.PlatformMapper;
import com.channelsoft.ccod.support.cmdb.exception.DBPAASDataNotConsistentException;
import com.channelsoft.ccod.support.cmdb.exception.NotSupportAppException;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.service.ILJPaasService;
import com.channelsoft.ccod.support.cmdb.vo.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
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
 * @ClassName: LJPaasServiceImpl
 * @Author: lanhb
 * @Description: ILJPaasService接口实现类
 * @Date: 2019/12/2 14:33
 * @Version: 1.0
 */
@Service
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

    @Value("${develop}")
    private boolean isDevelop;

    @Value("${lj_paas.basic_set_module}")
    private String basicPaasSetModule;

    @Value("${lj_paas.extend_set_module}")
    private String extendPaasSetModule;

    @Value("${lj_paas.exclude_biz}")
    private String excludeBiz;

    private String paasIdlePoolSetName = "idle pool";

    private String queryBizUrlFmt = "%s/api/c/compapi/v2/cc/search_business/";

    private String queryBizSetUrlFmt = "%s/api/c/compapi/v2/cc/search_set/";

    private String queryHostUrlFmt = "%s/api/c/compapi/v2/cc/search_host/";

    private final static Logger logger = LoggerFactory.getLogger(LJPaasServiceImpl.class);

    private Map<String, SetDomain> basicBizSetMap;

    private Map<String, SetDomain> extendBizSetMap;

    private Set<Integer> exludeBizSet;

    private Set<Integer> waitSyncUpdateToPaasBiz;

    private Random random = new Random();

    @PostConstruct
    void init() throws Exception
    {
        this.basicBizSetMap  = parseBizSetDomainParam(this.basicPaasSetModule);
        logger.info(String.format("basic ccod biz set : %s", JSONObject.toJSONString(basicBizSetMap)));
        this.extendBizSetMap = parseBizSetDomainParam(this.extendPaasSetModule);
        logger.info(String.format("extend ccod biz set : %s", JSONObject.toJSONString(extendBizSetMap)));
        this.exludeBizSet = new HashSet<>(parseIntArrayStr(";", this.excludeBiz));
        logger.info(String.format("exclude biz=%s", JSONArray.toJSONString(this.exludeBizSet)));
        this.waitSyncUpdateToPaasBiz = initWaitToSyncPaasBiz();
        logger.info(String.format("biz=%s wait to sync update detail from cmdb to paas", JSONObject.toJSONString(this.waitSyncUpdateToPaasBiz)));
        try
        {
            someTest();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private Set<Integer> initWaitToSyncPaasBiz()
    {
        this.waitSyncUpdateToPaasBiz = new HashSet<>();
        return waitSyncUpdateToPaasBiz;
    }

    private List<Integer> parseIntArrayStr(String regex, String intArrayStr)
    {
        String[] arr = intArrayStr.split(regex);
        List<Integer> list = new ArrayList<>();
        for(String str : arr)
        {
            if(StringUtils.isNotBlank(str))
            {
                list.add(Integer.parseInt(str));
            }
        }
        return list;
    }


    private Map<String, SetDomain> parseBizSetDomainParam(String bizSetDomainParamStr)
    {
        logger.info(String.format("begin to parse bizSetDomainParamStr=%s", bizSetDomainParamStr));
        Map<String, SetDomain> map = new HashMap<>();
        if(StringUtils.isNotBlank(bizSetDomainParamStr))
        {
            String[] setArr = bizSetDomainParamStr.split("\\|");
            for(String str : setArr)
            {
                if(StringUtils.isBlank(str))
                    continue;
                String[] arr = str.split("\\:");
                String[] setDomArr = arr[0].split("@");
                SetDomain setDomain = new SetDomain();
                setDomain.setName = setDomArr[0];
                if(setDomArr.length > 1)
                {
                    setDomain.domainId = setDomArr[1];
                    setDomain.domainName = setDomArr[1];
                }
                if(arr.length > 1)
                {
                    setDomain.moduleNameSet = new HashSet<>(Arrays.asList(arr[1].split(";")));
                }
                else
                {
                    setDomain.moduleNameSet = new HashSet<>();
                }
                map.put(setDomain.setName, setDomain);
            }
        }
        return map;
    }

    @Override
    public LJBizInfo queryBizInfoById(int bizId) throws Exception {
        return null;
    }

    @Override
    public LJBizInfo[] queryBizInfo() throws Exception {
        return new LJBizInfo[0];
    }

    private void someTest() throws Exception
    {
//        CCODPlatformInfo[] platformInfos = queryAllCCODBiz();
//        System.out.println(JSONArray.toJSONString(platformInfos));
        this.waitSyncUpdateToPaasBiz.add(11);
    }


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
        List<LJHostInfo> idleHosts = new ArrayList<>();
        for(LJHostResourceInfo hostResourceInfo : hostResources)
        {
            idleHosts.add(hostResourceInfo.getHost());
        }
        return idleHosts;
    }

    private CCODPlatformInfo createNewPlatformInfo(LJBizInfo bizInfo, LJSetInfo idlePoolSet, List<LJHostInfo> idleHosts)
    {
        CCODIdlePoolInfo idlePoolInfo = new CCODIdlePoolInfo(bizInfo.getBizId(), idlePoolSet, idleHosts);
        for(LJHostInfo bkHost : idleHosts)
        {
            CCODHostInfo host = new CCODHostInfo(bkHost);
            idlePoolInfo.getIdleHosts().add(host);
        }
        List<CCODSetInfo> ccodSetList = new ArrayList<>();
        for(String setName : this.basicBizSetMap.keySet())
        {
            if(setName.equals(this.paasIdlePoolSetName))
                continue;
            CCODSetInfo setInfo = new CCODSetInfo(setName);
            ccodSetList.add(setInfo);
        }
        CCODPlatformInfo platformInfo = new CCODPlatformInfo(bizInfo, CCODPlatformStatus.NEW_CREATE.id, idlePoolInfo, ccodSetList);
        return platformInfo;
    }

    private CCODPlatformInfo getCCODPlatformInfo(LJBizInfo bizInfo, LJSetInfo idlePoolSet, List<PlatformAppDeployDetailVo> deployApps, List<LJHostInfo> idleHosts) throws NotSupportAppException
    {
        if(this.isDevelop)
        {
            deployApps = makeUpBizInfoForDeployApps(bizInfo.getBizId(), deployApps);
        }
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
                bkSet.setBizId(bizInfo.getBizId());
                bkSet.setSetId(setId);
                bkSet.setSetName(deployApp.getSetName());
                CCODSetInfo ccodSet = new CCODSetInfo(bkSet);
                setMap.put(setId, ccodSet);
                setAppMap.put(bkSet.getSetId(), new ArrayList<>());
            }
            CCODModuleInfo module = new CCODModuleInfo(deployApp);
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
        CCODPlatformInfo ccodPlatformInfo = new CCODPlatformInfo(bizInfo, CCODPlatformStatus.RUNNING.id,
                idlePool, new ArrayList<>(setMap.values()));
        return ccodPlatformInfo;
    }

    private CCODPlatformInfo generatePlatformInfo( PlatformPo platform,
                                                   LJBizInfo bizInfo,
                                                   List<LJSetInfo> bkSetList,
                                                   List<LJHostInfo> idleHostList,
                                                   List<PlatformAppDeployDetailVo> deloyAppList) throws DBPAASDataNotConsistentException, NotSupportAppException
    {
        if(this.isDevelop)
        {
            deloyAppList = makeUpBizInfoForDeployApps(bizInfo.getBizId(), deloyAppList);
        }
        Map<String, List<PlatformAppDeployDetailVo>> setAppMap = deloyAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getSetName));
        Map<String, LJSetInfo> setMap = bkSetList.stream().collect(Collectors.toMap(LJSetInfo::getSetName, Function.identity()));
        if(!bizInfo.getBizName().equals(platform.getPlatformName()))
        {
            logger.error(String.format("bizName=%s of bizId=%d not equal with platformName=%s of bizId=%d and platId=%s",
                    bizInfo.getBizName(), bizInfo.getBizId(), platform.getBkBizId(), platform.getPlatformName()));
            return null;
        }
        CCODPlatformInfo platformInfo = new CCODPlatformInfo(bizInfo, CCODPlatformStatus.RUNNING.id);
        List<CCODSetInfo> setList = new ArrayList<>();
        for(String setName : setAppMap.keySet())
        {
            if(setName.equals(this.paasIdlePoolSetName))
            {
                continue;
            }
            if(!setMap.containsKey(setName))
            {
                logger.error(String.format("bizId=%d and bizName=%s biz has not setName=%s set which has been record deploy %d apps",
                        bizInfo.getBizId(), bizInfo.getBizName(), setName, setAppMap.get(setName).size()));
                throw new DBPAASDataNotConsistentException(String.format("bizId=%d and bizName=%s biz has not setName=%s set which has been record deploy %d apps",
                        bizInfo.getBizId(), bizInfo.getBizName(), setName, setAppMap.get(setName).size()));
            }
            LJSetInfo bkSet = setMap.get(setName);
            Map<String, CCODDomainInfo> domainMap = new HashMap<>();
            Map<String, List<PlatformAppDeployDetailVo>> domainAppMap =  setAppMap.get(setName)
                    .stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getDomainName));
            List<CCODDomainInfo> domainList = new ArrayList<>();
            for(String domainName : domainAppMap.keySet())
            {
                List<PlatformAppDeployDetailVo> domAppList = domainAppMap.get(domainName);
                CCODDomainInfo domain = new CCODDomainInfo(bkSet.getSetId(), domAppList.get(0).getDomId(),
                        domAppList.get(0).getDomainId(), domAppList.get(0).getDomainName());
                for(PlatformAppDeployDetailVo deployApp : domAppList)
                {
                    CCODModuleInfo bkModule = new CCODModuleInfo(deployApp);
                    domain.getModules().add(bkModule);
                }
                domainList.add(domain);
            }
            CCODSetInfo set = new CCODSetInfo(bkSet);
            set.setDomains(domainList);
            setList.add(set);
        }
        CCODIdlePoolInfo idlePool = new CCODIdlePoolInfo(bizInfo.getBizId(), setMap.get(this.paasIdlePoolSetName), idleHostList);
        platformInfo.setIdlePool(idlePool);
        platformInfo.setSets(setList);
        platformInfo.setPlatId(platform.getId());
        return platformInfo;
    }

    private List<PlatformAppDeployDetailVo> makeUpBizInfoForDeployApps(int bizId, List<PlatformAppDeployDetailVo> deployApps) throws NotSupportAppException
    {
        for(PlatformAppDeployDetailVo deployApp : deployApps)
        {
            deployApp.setBkBizId(bizId);
            SetDomain sd = getDomainNameForApp(deployApp.getAppName(), deployApp.getDomainId(), deployApp.getDomainName());
            deployApp.setSetName(sd.setName);
            deployApp.setDomainId(sd.domainId);
            deployApp.setDomainName(sd.domainName);
        }
        return deployApps;
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
    private CCODPlatformStatus checkBizType(LJBizInfo bizInfo, List<LJSetInfo> bizSetList, List<LJHostInfo> idleHosts, Map<Integer, PlatformPo> platformMap, List<PlatformAppDeployDetailVo> deployApps)
    {
        if(bizSetList.size() != 1 && bizSetList.size() != this.basicBizSetMap.size())
        {
            logger.info(String.format("bizId=%d and bizName=%s biz has %d bizSet not equal 1 or %d, so it not relative to cmdb and return 0",
                    bizInfo.getBizId(), bizInfo.getBizName(), bizSetList.size(), this.basicBizSetMap.size()));
            return CCODPlatformStatus.UNKNOWN;
        }
        if(bizSetList.size() == 1)
        {
            if(!this.paasIdlePoolSetName.equals(bizSetList.get(0).getSetName()))
            {
                logger.warn(String.format("bizId=%d and bizName=%s biz has only one set but it's name not %s'",
                        bizInfo.getBizId(), bizInfo.getBizName(), this.paasIdlePoolSetName));
                return CCODPlatformStatus.UNKNOWN;
            }
            if(platformMap.containsKey(bizInfo.getBizId()))
            {
                if(deployApps.size() > 0 && idleHosts.size() == 0)
                {
                    logger.info(String.format("bizId=%d and bizName=%s biz has only one set=%s and idle host count is 0 and has %d deploy apps record in cmdb db, so it status is %s",
                            bizInfo.getBizId(), bizInfo.getBizName(), this.paasIdlePoolSetName, deployApps.size(),
                            CCODPlatformStatus.WAIT_SYNC_EXIST_PLATFORM_TO_PAAS.name));
                    return CCODPlatformStatus.WAIT_SYNC_EXIST_PLATFORM_TO_PAAS;
                }
                logger.error(String.format("bizId=%d and bizName=%s biz has only one set=%s but idle hosts count is %d and deploy apps is %d in cmdb db, so it status is %s",
                        bizInfo.getBizId(), bizInfo.getBizName(), this.paasIdlePoolSetName, idleHosts.size(), deployApps.size(), CCODPlatformStatus.UNKNOWN.name));
                return CCODPlatformStatus.UNKNOWN;
            }
            else if(!this.paasIdlePoolSetName.equals(bizSetList.get(0).getSetName()))
            {
                logger.warn(String.format("bizId=%d and bizName=%s biz has only one set but it's name not %s'",
                        bizInfo.getBizId(), bizInfo.getBizName(), this.paasIdlePoolSetName));
                return CCODPlatformStatus.UNKNOWN;
            }
            else
            {
                logger.info(String.format("bizId=%d and bizName=%s biz has one set with name=%s and it not saved in database so it maybe a new create biz relative to cmdb, so return 2",
                        bizInfo.getBizId(), bizInfo.getBizName(), this.paasIdlePoolSetName));
                return CCODPlatformStatus.NEW_CREATE;
            }
        }
        else
        {
            if(!platformMap.containsKey(bizInfo.getBizId()))
            {
                logger.warn(String.format("bizId=%d and bizName=%s biz has %d set but it has been saved in database, so return 0'",
                        bizInfo.getBizId(), bizInfo.getBizName(), bizSetList.size()));
                return CCODPlatformStatus.UNKNOWN;
            }
            for(LJSetInfo setInfo : bizSetList)
            {
                if(!this.basicBizSetMap.containsKey(setInfo.getSetName()))
                {
                    logger.warn(String.format("setName=%s of bizId=%d and bizName=%s biz is not a set name for cmdb biz, so return 0",
                            setInfo.getSetName(), bizInfo.getBizId(), bizInfo.getBizName()));
                    return CCODPlatformStatus.UNKNOWN;
                }
            }
        }
        logger.info(String.format("bizId=%d and bizName=%s is an old biz for cmd, so return 1",
                bizInfo.getBizId(), bizInfo.getBizName()));
        return CCODPlatformStatus.RUNNING;
    }


    /**
     * 创建新被创建的等待同步到paas的ccod biz平台信息
     * @param platform ccod biz对应的cmdb平台信息
     * @param bizInfo biz在paas的相关信息
     * @param deployApps cmdb记录的该platform的应用部署详情
     * @return 被创建的ccod biz信息
     * @throws NotSupportAppException
     * @throws DBPAASDataNotConsistentException
     */
    private CCODPlatformInfo createNewCreateWaitSyncPlatformInfo(PlatformPo platform, LJBizInfo bizInfo, List<PlatformAppDeployDetailVo> deployApps) throws NotSupportAppException, DBPAASDataNotConsistentException
    {
        List<PlatformAppDeployDetailVo> apps = makeUpBizInfoForDeployApps(bizInfo.getBizId(), deployApps);
        List<LJSetInfo> setList = createDefaultSetList(bizInfo.getBizId());
        List<LJHostInfo> idleHosts = new ArrayList<>();
        CCODPlatformInfo platformInfo = generatePlatformInfo(platform, bizInfo, setList, idleHosts, apps);
        platformInfo.setStatus(CCODPlatformStatus.WAIT_SYNC_NEW_CREATE_PLATFORM_TO_PAAS.id);
        return platformInfo;
    }

    private List<LJSetInfo> createDefaultSetList(int bizId)
    {
        List<LJSetInfo> setList = new ArrayList<>();
        LJSetInfo idleSet = new LJSetInfo();
        idleSet.setBizId(bizId);
        idleSet.setSetName(this.paasIdlePoolSetName);
        setList.add(idleSet);
        for(String setName : this.basicBizSetMap.keySet())
        {
            LJSetInfo set = new LJSetInfo();
            set.setSetName(setName);
            set.setBizId(bizId);
            setList.add(set);
        }
        return setList;
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

    private SetDomain getDomainNameForApp(String appName, String domainId, String domainName) throws NotSupportAppException
    {
        for(SetDomain sd : this.basicBizSetMap.values())
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
        logger.error(String.format("appName=%s not support", appName));
        throw new NotSupportAppException(String.format("appName=%s not support", appName));
    }

    @Override
    public CCODPlatformInfo[] queryAllCCODBiz() throws Exception {
        logger.info(String.format("begin to query all biz platform info for ccod"));
        List<PlatformPo> platformList = platformMapper.select(1);
        List<PlatformAppDeployDetailVo> deployAppList = platformAppDeployDetailMapper.selectPlatformApps(null,
                null, null,null);
        if(isDevelop)
        {
            for(PlatformPo platformPo : platformList)
            {
                if("shltPA".equals(platformPo.getPlatformId()))
                {
                    platformPo.setBkBizId(11);
                }
                else
                {
                    platformList.remove(platformPo);
                }
            }
            for(PlatformAppDeployDetailVo deployApp : deployAppList)
            {
                if(deployApp.getPlatformId().equals("shltPA"))
                {
                    deployApp.setBkBizId(11);
                }
                else
                {
                    deployAppList.remove(deployApp);
                }
            }
        }
        List<LJBizInfo> bizList = queryLJPaasAllBiz(this.paasHostUrl, this.appCode, this.appSecret, this.userName);
        Map<Integer, PlatformPo> bizPlatformMap = platformList.stream().collect(Collectors.toMap(PlatformPo::getBkBizId, Function.identity()));
        Map<String, PlatformPo> namePlatformMap = platformList.stream().collect(Collectors.toMap(PlatformPo::getPlatformName, Function.identity()));;
        Map<Integer, List<PlatformAppDeployDetailVo>> bizAppMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getBkBizId));
        Map<String, List<PlatformAppDeployDetailVo>> platformNameAppMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getPlatformName));
        List<CCODPlatformInfo> ccodPlatformList = new ArrayList<>();
        for(LJBizInfo biz : bizList)
        {
            //判断biz是否在exclude列表里
            if(this.exludeBizSet.contains(biz.getBizId()))
            {
                logger.info(String.format("bizId=%d is in exclude set", biz.getBizId()));
                continue;
            }
            List<LJSetInfo> setList = this.queryLJPaasBizSet(biz.getBizId(), this.paasHostUrl, this.appCode,
                this.appSecret, this.userName);
            boolean isCcodBiz = isCCODBiz(biz, setList);
            if(!isCcodBiz)
                continue;

            LJSetInfo idlePoolSet = setList.stream().collect(Collectors.toMap(LJSetInfo::getSetName, Function.identity())).get(this.paasIdlePoolSetName);
            List<LJHostInfo> idleHostList = queryIdleHost(biz.getBizId(), idlePoolSet.getSetId(),
                    this.paasHostUrl, this.appCode, this.appSecret, this.userName);
            if(setList.size() > 1 && bizAppMap.containsKey(biz.getBizId()))
            {
                logger.info(String.format("%s biz has all ccod biz sets and cmdb has %d app records for it, so %s is running ccod platform",
                        biz.toString(), bizAppMap.get(biz.getBizId()).size(), biz.getBizName()));
                CCODPlatformInfo platformInfo = generatePlatformInfo(bizPlatformMap.get(biz.getBizId()), biz, setList, idleHostList, bizAppMap.get(biz.getBizId()));
                if(this.waitSyncUpdateToPaasBiz.contains(biz.getBizId()))
                {
                    logger.error(String.format("%s in waitSyncUpdateToPaasBiz, so it status is %s",
                            biz.toString(), CCODPlatformStatus.WAIT_SYNC_PLATFORM_UPDATE_TO_PAAS.name));
                    platformInfo.setStatus(CCODPlatformStatus.WAIT_SYNC_PLATFORM_UPDATE_TO_PAAS.id);
                }
                ccodPlatformList.add(platformInfo);
            }
            else if(setList.size() == 1 && platformNameAppMap.containsKey(biz.getBizName()) && idleHostList.size() > 0)
            {
                logger.info(String.format("%s biz has and only has %s set and %d idle hosts and cmdb has %d app record for it, so %s is %s platform",
                        biz.toString(), this.paasIdlePoolSetName, idleHostList.size(), platformNameAppMap.get(biz.getBizName()).size(), biz.getBizName(), CCODPlatformStatus.WAIT_SYNC_NEW_CREATE_PLATFORM_TO_PAAS.name));
                CCODPlatformInfo platformInfo = createNewCreateWaitSyncPlatformInfo(namePlatformMap.get(biz.getBizName()), biz, platformNameAppMap.get(biz.getBizName()));
                ccodPlatformList.add(platformInfo);
            }
            else if(setList.size() == 1 && !platformNameAppMap.containsKey(biz.getBizName()) && idleHostList.size() > 0)
            {
                logger.info(String.format("%s biz has and only has %s set and cmdb has not app record for it, so %s is %s platform",
                        biz.toString(), this.paasIdlePoolSetName, biz.getBizName(), CCODPlatformStatus.NEW_CREATE.name));
                CCODPlatformInfo platformInfo = createNewPlatformInfo(biz, idlePoolSet, idleHostList);
                ccodPlatformList.add(platformInfo);
            }
        }
        if(this.isDevelop)
        {
            ccodPlatformList = createDemoCCODPlatform(ccodPlatformList);
        }
        return ccodPlatformList.toArray(new CCODPlatformInfo[0]);
    }

    /**
     * 判断paas下的一个biz是否是ccod biz遵循以下规则
     * 1、该biz必须有一个空闲服务器资源池set(idle pool)
     * 2、如果该biz的set数量为1则认为该biz是一个ccod biz
     * 3、如果该biz的数量大于1，则如果该biz的set包含basicBizSetMap中所有的set且不包含除basicBizSetMap和extendBizSetMap之外的set
     */
    private boolean isCCODBiz(LJBizInfo bizInfo, List<LJSetInfo> setList)
    {
        Map<String, LJSetInfo> setMap = setList.stream().collect(Collectors.toMap(LJSetInfo::getSetName, Function.identity()));
        if(!setMap.containsKey(this.paasIdlePoolSetName))
        {
            logger.warn(String.format("%s has not %s set, so it is not ccod biz",
                    bizInfo.toString(), this.paasIdlePoolSetName));
            return false;
        }
        setMap.remove(this.paasIdlePoolSetName);
        if(setMap.size() == 0)
        {
            logger.info(String.format("%s has and only has %s set, so it maybe ccod biz",
                    bizInfo.toString(), this.paasIdlePoolSetName));
            return true;
        }
        for(String setName : setMap.keySet())
        {
            if(!this.basicBizSetMap.containsKey(setName) && !this.extendBizSetMap.containsKey(setName))
            {
                logger.warn(String.format("set=%s of %s not in ccod basic sets and ccod extend sets, so it is not a ccod biz",
                        setName, bizInfo.toString()));
                return false;
            }
        }
        for(String setName : this.basicBizSetMap.keySet())
        {
            if(!setMap.containsKey(setName))
            {
                logger.warn(String.format("%s not has ccod basic set %s, so it is not a ccod biz",
                        bizInfo.toString(), setName));
                return false;
            }
        }
        logger.info(String.format("%s is a ccod biz", bizInfo.toString()));
        return true;
    }

    private List<CCODPlatformInfo> createDemoCCODPlatform(List<CCODPlatformInfo> srcPlatforms)
    {
        List<CCODPlatformInfo> list = new ArrayList<>();
        CCODPlatformInfo src = null;
        for(CCODPlatformInfo platform : srcPlatforms)
        {
            list.add(platform);
            if(platform.getStatus() == CCODPlatformStatus.WAIT_SYNC_NEW_CREATE_PLATFORM_TO_PAAS.id
                    && "上海联通平安".equals(platform.getPlatformName()))
            {
                src = platform;
            }
        }
        if(src != null)
        {
            CCODPlatformInfo newPlat = src.clone();
            newPlat.setStatus(CCODPlatformStatus.WAIT_SYNC_EXIST_PLATFORM_TO_PAAS.id);
            newPlat.setPlatformName("新收集等待同步paas平台");
            newPlat.setPlatformId("newCollectPlatform");
            list.add(src);

            newPlat = src.clone();
            newPlat.setStatus(CCODPlatformStatus.WAIT_SYNC_PLATFORM_UPDATE_TO_PAAS.id);
            newPlat.setPlatformName("等待同步更新结果平台");
            newPlat.setPlatformId("waitSyncAppUpdatePlatform");
            for(CCODSetInfo setInfo : newPlat.getSets())
            {
                for(CCODDomainInfo domainInfo : setInfo.getDomains())
                {
                    if(domainInfo.getModules().size() > 0)
                    {
                        for(CCODModuleInfo moduleInfo : domainInfo.getModules())
                        {
                            newPlat.getPlanUpdateApps().add(moduleInfo.clone());
                        }
                    }
                }
            }
            list.add(src);

            newPlat = src.clone();
            newPlat.setStatus(CCODPlatformStatus.PLAN_CREATE_PLATFORM.id);
            newPlat.setPlatformName("计划创建新平台");
            newPlat.setPlatformId("planCreateNewPlatform");
            list.add(src);

            newPlat = src.clone();
            newPlat.setStatus(CCODPlatformStatus.PLAN_CREATE_DOMAIN.id);
            newPlat.setPlatformName("计划创建新域");
            newPlat.setPlatformId("planCreateNewDomain");
            for(CCODSetInfo setInfo : newPlat.getSets())
            {
                if(setInfo.getBkSetName().equals("域服务"))
                {
                    newPlat.setPlanNewDomain(setInfo.getDomains().get(0).clone());
                    newPlat.getPlanNewDomain().setDomainName("计划新加域");
                    newPlat.getPlanNewDomain().setDomainId("planNewAddDomain");
                }
            }
            list.add(src);

            newPlat = src.clone();
            newPlat.setStatus(CCODPlatformStatus.PLAN_APP_UPDATE.id);
            newPlat.setPlatformName("等待应用升级平台");
            newPlat.setPlatformId("waitAppUpdatePlatform");
            for(CCODSetInfo setInfo : newPlat.getSets())
            {
                for(CCODDomainInfo domainInfo : setInfo.getDomains())
                {
                    if(domainInfo.getModules().size() > 0)
                    {
                        for(CCODModuleInfo moduleInfo : domainInfo.getModules())
                        {
                            newPlat.getPlanUpdateApps().add(moduleInfo.clone());
                        }
                    }
                }
            }
            list.add(src);
        }
        return list;
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
