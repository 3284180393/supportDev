package com.channelsoft.ccod.support.cmdb.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.channelsoft.ccod.support.cmdb.constant.*;
import com.channelsoft.ccod.support.cmdb.dao.DomainMapper;
import com.channelsoft.ccod.support.cmdb.dao.PlatformAppDeployDetailMapper;
import com.channelsoft.ccod.support.cmdb.dao.PlatformMapper;
import com.channelsoft.ccod.support.cmdb.exception.*;
import com.channelsoft.ccod.support.cmdb.po.*;
import com.channelsoft.ccod.support.cmdb.service.ILJPaasService;
import com.channelsoft.ccod.support.cmdb.utils.HttpTools;
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
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;
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

    @Autowired
    private CCODBizSetInfo bizSetInfo;

    @Autowired
    private ExcludeBiz excludeBiz;

    @Autowired
    private DomainMapper domainMapper;

    @Value("${lj-paas.host-url}")
    private String paasHostUrl;

    @Value("${lj-paas.bk-app-code}")
    private String bkAppCode;

    @Value("${lj-paas.bk-app-secret}")
    private String bkAppSecret;

    @Value("${lj-paas.user-name}")
    private String bkUserName;

    @Value("${develop}")
    private boolean isDevelop;

    @Value("${lj-paas.idle-pool-set-name}")
    private String paasIdlePoolSetName;

    @Value("${lj-paas.idle-pool-set-id}")
    private String paasIdlePoolSetId;

    @Value("${lj-paas.update-schema-set-name}")
    private String updateSchemaSetName;

    @Value("${lj-paas.update-schema-set-id}")
    private String updateSchemaSetId;

    @Value("${lj-paas.host-default-cloud-id}")
    private int defaultCloudId;

    private final String queryBizUrlFmt = "%s/api/c/compapi/v2/cc/search_business/";

    private final String queryBizSetUrlFmt = "%s/api/c/compapi/v2/cc/search_set/";

    private final String addHostUrlFmt = "%s/api/c/compapi/v2/cc/add_host_to_resource/";

    private final String queryHostUrlFmt = "%s/api/c/compapi/v2/cc/search_host/";

    private final String createNewSetUrlFmt = "%s/api/c/compapi/v2/cc/create_set/";

    private final String deleteSetUrlFmt = "%s/api/c/compapi/v2/cc/delete_set/";

    private final String addModuleUrlFmt = "%s/api/c/compapi/v2/cc/create_module/";

    private final String deleteModuleUrlFmt = "%s/api/c/compapi/v2/cc/delete_module/";

    private final String queryModuleUrlFmt = "%s/api/c/compapi/v2/cc/search_module/";

    private final String transferModuleUrlFmt = "%s/api/c/compapi/v2/cc/transfer_host_module/";

    private final String transferHostToIdlePoolUrlFmt = "%s/api/c/compapi/v2/cc/transfer_host_to_idlemodule/";

    private final String transferHostToResourceUrlFmt = "%s/api/c/compapi/v2/cc/transfer_host_to_resourcemodule/";

    private final static Logger logger = LoggerFactory.getLogger(LJPaasServiceImpl.class);

    private Map<String, BizSetDefine> basicBizSetMap;

    private Map<String, BizSetDefine> extendBizSetMap;

    private Map<String, List<BizSetDefine>> appSetRelationMap;

    private Set<String> excludeBizSet;

    private Set<Integer> waitSyncUpdateToPaasBiz;

    private Random random = new Random();

    private List<CCODPlatformInfo> allPlatformBiz;

    @PostConstruct
    void init() throws Exception
    {
        this.basicBizSetMap  = new HashMap<>();
        this.extendBizSetMap = new HashMap<>();
        this.appSetRelationMap = new HashMap<>();
        for(BizSetDefine define : this.bizSetInfo.getSet())
        {
            if(define.getIsBasic() == 1)
            {
                this.basicBizSetMap.put(define.getName(), define);
            }
            else
            {
                this.extendBizSetMap.put(define.getName(), define);
            }
            for(String app : define.getApps())
            {
                if(!this.appSetRelationMap.containsKey(app))
                {
                    this.appSetRelationMap.put(app, new ArrayList<>());
                }
                this.appSetRelationMap.get(app).add(define);
            }
        }
        logger.info(String.format("basic ccod biz set : %s", JSONObject.toJSONString(basicBizSetMap)));
        logger.info(String.format("extend ccod biz set : %s", JSONObject.toJSONString(extendBizSetMap)));
        logger.info(String.format("app and set relation is : %s", JSONObject.toJSONString(this.appSetRelationMap)));
        this.excludeBizSet = new HashSet<>();
        for(String bizName : this.excludeBiz.getExcludes())
        {
            excludeBizSet.add(bizName);
        }
        logger.info(String.format("%s will be excluded from ccod biz", JSONObject.toJSONString(this.excludeBizSet)));
        this.waitSyncUpdateToPaasBiz = initWaitToSyncPaasBiz();
        logger.info(String.format("biz=%s wait to sync update detail from cmdb to paas", JSONObject.toJSONString(this.waitSyncUpdateToPaasBiz)));
        this.allPlatformBiz = this.queryAllCCODBiz();
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
//        this.waitSyncUpdateToPaasBiz.add(11);
//        syncClientCollectResultToPaas(14, "shltPA", this.defaultCloudId);
    }


    private List<LJBizInfo> queryBKBiz(Integer bkBizId, String bkBizName) throws InterfaceCallException, LJPaasException
    {
        logger.info(String.format("begin to query biz info : bkBizId=%d and bkBizName=%s", bkBizId, bkBizName));
        String url = String.format(this.queryBizUrlFmt, paasHostUrl);
        Map<String, Object> paramsMap = getLJPaasCallBaseParams();
        Map<String, Object> condition = new HashMap<>();
        if(bkBizId != null)
        {
            condition.put("bk_biz_id", bkBizId);
        }
        if(bkBizName != null)
        {
            condition.put("bk_biz_name", bkBizName);
        }
        paramsMap.put("condition", condition);
        String retVal = HttpTools.httpPostRequest(url, paramsMap);
        String data = parsePaasInterfaceResult(retVal);
        try
        {
            List<LJBizInfo> bizList = JSONArray.parseArray(JSONObject.parseObject(data).getString("info"), LJBizInfo.class);
            logger.info(String.format("find %d biz with bkBizId=%d and bkBizName=%s", bizList.size(), bkBizId, bkBizName));
            return bizList;
        }
        catch (Exception ex)
        {
            logger.error(String.format("parse lj paas return info for biz info exception"), ex);
            throw new LJPaasException(String.format("parse lj paas return info for biz info exception"));
        }
    }

    private List<LJSetInfo> queryLJPaasBizSet(int bkBizId) throws InterfaceCallException, LJPaasException
    {
        logger.info(String.format("begin to query set info of bkBizId=%d", bkBizId));
        Map<String, Object> paramsMap = getLJPaasCallBaseParams();
        paramsMap.put("bk_biz_id", bkBizId);
        String url = String.format(this.queryBizSetUrlFmt, paasHostUrl);
        String retVal = HttpTools.httpPostRequest(url, paramsMap);
        String data = parsePaasInterfaceResult(retVal);
        try
        {
            List<LJSetInfo> setList = JSONArray.parseArray(JSONObject.parseObject(data).getString("info"), LJSetInfo.class);
            logger.info(String.format("find %d set at bkBizId=%d", setList.size(), bkBizId));
            return setList;
        }
        catch (Exception ex)
        {
            logger.error(String.format("parse lj paas return set info of bkBizId=%d biz exception", bkBizId), ex);
            throw new LJPaasException(String.format("parse lj paas return set info of bkBizId=%d biz exception", bkBizId));
        }
    }

    private List<LJHostInfo> queryIdleHost(int bizId, int idlePoolSetId) throws Exception
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
        for(BizSetDefine define : this.basicBizSetMap.values())
        {
            if(define.getName().equals(this.paasIdlePoolSetName))
                continue;
            CCODSetInfo setInfo = new CCODSetInfo(define.getId(), define.getName());
            ccodSetList.add(setInfo);
        }
        CCODPlatformInfo platformInfo = new CCODPlatformInfo(bizInfo, CCODPlatformStatus.NEW_CREATE.id, idlePoolInfo, ccodSetList);
        return platformInfo;
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
            BizSetDefine setDefine;
            if(this.basicBizSetMap.containsKey(setName))
            {
                setDefine = this.basicBizSetMap.get(setName);
            }
            else
            {
                setDefine = this.extendBizSetMap.get(setName);
            }
            LJSetInfo bkSet = setMap.get(setName);
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
            CCODSetInfo set = new CCODSetInfo(setDefine.getId(), bkSet);
            set.setDomains(domainList);
            setList.add(set);
        }
        CCODIdlePoolInfo idlePool = new CCODIdlePoolInfo(bizInfo.getBizId(), setMap.get(this.paasIdlePoolSetName), idleHostList);
        platformInfo.setIdlePool(idlePool);
        platformInfo.setSets(setList);
        return platformInfo;
    }

    /**
     * 把通过onlinemanager主动收集上来的ccod应用部署情况同步到paas之前需要给这些应用添加对应的bizId
     * 确定应用归属的set信息,并根据定义的set-app关系对某些应用归属域重新赋值
     * @param bizId 蓝鲸paas的biz id
     * @param deployApps 需要处理的应用详情
     * @return 处理后的结果
     * @throws NotSupportAppException 如果应用中存在没有在lj-paas.set-apps节点定义的应用将抛出此异常
     */
    private List<PlatformAppDeployDetailVo> makeUpBizInfoForDeployApps(int bizId, List<PlatformAppDeployDetailVo> deployApps) throws NotSupportAppException
    {
        for(PlatformAppDeployDetailVo deployApp : deployApps)
        {
            if(!this.appSetRelationMap.containsKey(deployApp.getAppName()))
            {
                logger.error(String.format("%s没有在配置文件的lj-paas.set-apps节点中定义", deployApp.getAppName()));
                throw new NotSupportAppException(String.format("%s未定义所属的set信息", deployApp.getAppName()));
            }
            deployApp.setBkBizId(bizId);
            BizSetDefine sd = this.appSetRelationMap.get(deployApp.getAppName()).get(0);
            deployApp.setSetName(sd.getName());
            if(StringUtils.isNotBlank(sd.getFixedDomainName()))
            {
                deployApp.setSetId(sd.getId());
                deployApp.setSetName(sd.getName());
                deployApp.setDomainId(sd.getFixedDomainId());
                deployApp.setDomainName(sd.getFixedDomainName());
            }

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
        for(String setName : this.extendBizSetMap.keySet())
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

    private Map<String, Object> generateLJObjectParam(String objId, String[] fields, Map<String, Object> equalCondition)
    {
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("bk_obj_id", objId);
        paramMap.put("fields", fields);
        List<Map<String, Object>> conditionList = new ArrayList<>();
        for(Map.Entry<String, Object> entry : equalCondition.entrySet())
        {
            Map<String, Object> condition = new HashMap<>();
            condition.put("field", entry.getKey());
            condition.put("operator", "$eq");
            condition.put("value", entry.getValue());
            conditionList.add(condition);
        }
        paramMap.put("condition", conditionList);
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

    @Override
    public List<CCODPlatformInfo> queryAllCCODBiz() throws Exception {
        logger.info(String.format("begin to query all biz platform info for ccod"));
        if(this.allPlatformBiz != null)
            return this.allPlatformBiz;
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
        List<LJBizInfo> bizList = queryBKBiz(null, null);
        Map<Integer, PlatformPo> bizPlatformMap = platformList.stream().collect(Collectors.toMap(PlatformPo::getBkBizId, Function.identity()));
        Map<String, PlatformPo> namePlatformMap = platformList.stream().collect(Collectors.toMap(PlatformPo::getPlatformName, Function.identity()));;
        Map<Integer, List<PlatformAppDeployDetailVo>> bizAppMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getBkBizId));
        Map<String, List<PlatformAppDeployDetailVo>> platformNameAppMap = deployAppList.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getPlatformName));
        List<CCODPlatformInfo> ccodPlatformList = new ArrayList<>();
        for(LJBizInfo biz : bizList)
        {
            //判断biz是否在exclude列表里
            if(this.excludeBizSet.contains(biz.getBizName()))
            {
                logger.info(String.format("bizId=%d is in exclude set", biz.getBizId()));
                continue;
            }
            List<LJSetInfo> setList = this.queryLJPaasBizSet(biz.getBizId());
            boolean isCcodBiz = isCCODBiz(biz, setList);
            if(!isCcodBiz)
                continue;
            LJSetInfo idlePoolSet = setList.stream().collect(Collectors.toMap(LJSetInfo::getSetName, Function.identity())).get(this.paasIdlePoolSetName);
            List<LJHostInfo> idleHostList = queryIdleHost(biz.getBizId(), idlePoolSet.getSetId());
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
        return ccodPlatformList;
    }

    @Override
    public List<CCODPlatformInfo> queryCCODBiz(Integer bizId, String setId, String domainId) throws Exception {
        logger.info(String.format("begin to query biz info : bizId=%d, setId=%s, domainId=%s",
                bizId, setId, domainId));
        List<CCODPlatformInfo> platformList = new ArrayList<>();
        if(bizId == null)
        {
            for(CCODPlatformInfo platformInfo : this.allPlatformBiz)
            {
                CCODPlatformInfo platform = platformInfo.clone();
                platform.setSets(new ArrayList<>());
                platform.setIdlePool(null);
                platformList.add(platform);
            }
        }
        else if(StringUtils.isBlank(setId))
        {
            Map<Integer, CCODPlatformInfo> bizMap = this.allPlatformBiz.stream().collect(Collectors.toMap(CCODPlatformInfo::getBizId, Function.identity()));
            if(bizMap.containsKey(bizId))
            {
                CCODPlatformInfo platform = bizMap.get(bizId).clone();
                for(CCODSetInfo set : platform.getSets())
                {
                    set.setDomains(new ArrayList<>());
                }
                platform.setIdlePool(null);
                platformList.add(platform);
            }
        }
        else if(StringUtils.isBlank(domainId))
        {
            Map<Integer, CCODPlatformInfo> bizMap = this.allPlatformBiz.stream().collect(Collectors.toMap(CCODPlatformInfo::getBizId, Function.identity()));
            if(bizMap.containsKey(bizId))
            {
                CCODPlatformInfo platform = bizMap.get(bizId).clone();
                Map<String, CCODSetInfo> setMap = platform.getSets().stream().collect(Collectors.toMap(CCODSetInfo::getSetId, Function.identity()));
                if(setMap.containsKey(setId))
                {
                    CCODSetInfo set = setMap.get(setId);
                    for(CCODDomainInfo domain : set.getDomains())
                    {
                        domain.setModules(new ArrayList<>());
                    }
                    platform.setSets(new ArrayList<>());
                    platform.getSets().add(set);
                    platform.setIdlePool(null);
                    platformList.add(platform);
                }
                else if(setId.equals(this.paasIdlePoolSetId))
                {
                    List<LJHostInfo> idleHosts = queryIdleHost(bizId, platform.getIdlePool().getBkSetId());
                    CCODIdlePoolInfo idlePoolInfo = new CCODIdlePoolInfo(bizId, platform.getIdlePool().getIdlePoolSet(), idleHosts);
                    platform.setSets(new ArrayList<>());
                    platform.setIdlePool(idlePoolInfo);
                    platformList.add(platform);
                }
            }
        }
        else
        {
            Map<Integer, CCODPlatformInfo> bizMap = this.allPlatformBiz.stream().collect(Collectors.toMap(CCODPlatformInfo::getBizId, Function.identity()));
            if(bizMap.containsKey(bizId))
            {
                CCODPlatformInfo platform = bizMap.get(bizId).clone();
                Map<String, CCODSetInfo> setMap = platform.getSets().stream().collect(Collectors.toMap(CCODSetInfo::getSetId, Function.identity()));
                if(setMap.containsKey(setId))
                {
                    CCODSetInfo set = setMap.get(setId);
                    Map<String, CCODDomainInfo> domainMap = set.getDomains().stream().collect(Collectors.toMap(CCODDomainInfo::getDomainId, Function.identity()));
                    if(domainMap.containsKey(domainId))
                    {
                        CCODDomainInfo domain = domainMap.get(domainId);
                        set.setDomains(new ArrayList<>());
                        set.getDomains().add(domain);
                        platform.setSets(new ArrayList<>());
                        platform.getSets().add(set);
                        platform.setIdlePool(null);
                        platformList.add(platform);
                    }
                }
            }
        }
        return platformList;
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
            if(platform.getStatus() == CCODPlatformStatus.WAIT_SYNC_NEW_CREATE_PLATFORM_TO_PAAS.id
                    && "上海联通平安".equals(platform.getPlatformName()))
            {
//                platform = reduceData(platform);
                src = platform;
            }
            platform.setBizId(random.nextInt(10000));
            list.add(platform);
        }
        if(src != null)
        {
            CCODPlatformInfo newPlat = src.clone();
            newPlat.setStatus(CCODPlatformStatus.WAIT_SYNC_EXIST_PLATFORM_TO_PAAS.id);
            newPlat.setPlatformName("新收集等待同步paas平台");
            newPlat.setPlatformId("newCollectPlatform");
            newPlat.setBizId(random.nextInt(10000));
            list.add(newPlat);

            newPlat = src.clone();
            newPlat.setStatus(CCODPlatformStatus.WAIT_SYNC_PLATFORM_UPDATE_TO_PAAS.id);
            newPlat.setPlatformName("等待同步更新结果平台");
            newPlat.setPlatformId("waitSyncAppUpdatePlatform");
            newPlat.setBizId(random.nextInt(10000));
            boolean isAdd = false;
            for(CCODSetInfo setInfo : newPlat.getSets())
            {
                if(isAdd)
                    break;
                for(CCODDomainInfo domainInfo : setInfo.getDomains())
                {
                    if(isAdd)
                        break;
                    if(domainInfo.getModules().size() > 0)
                    {
                        for(CCODModuleInfo moduleInfo : domainInfo.getModules())
                        {
                            isAdd = true;
                            break;
                        }
                    }
                }
            }
            list.add(newPlat);

            newPlat = src.clone();
            newPlat.setStatus(CCODPlatformStatus.PLAN_CREATE_PLATFORM.id);
            newPlat.setPlatformName("计划创建新平台");
            newPlat.setPlatformId("planCreateNewPlatform");
            newPlat.setBizId(random.nextInt(10000));
            list.add(newPlat);

            newPlat = src.clone();
            newPlat.setStatus(CCODPlatformStatus.PLAN_CREATE_DOMAIN.id);
            newPlat.setPlatformName("计划创建新域");
            newPlat.setPlatformId("planCreateNewDomain");
            newPlat.setBizId(random.nextInt(10000));
            for(CCODSetInfo setInfo : newPlat.getSets())
            {
                if(setInfo.getBkSetName().equals("域服务"))
                {
                    break;
                }
            }
            list.add(newPlat);

            newPlat = src.clone();
            newPlat.setStatus(CCODPlatformStatus.PLAN_APP_UPDATE.id);
            newPlat.setPlatformName("等待应用升级平台");
            newPlat.setPlatformId("waitAppUpdatePlatform");
            newPlat.setBizId(random.nextInt());
            isAdd = false;
            for(CCODSetInfo setInfo : newPlat.getSets())
            {
                if(isAdd)
                    break;
                for(CCODDomainInfo domainInfo : setInfo.getDomains())
                {
                    if(isAdd)
                        break;
                    if(domainInfo.getModules().size() > 0)
                    {
                        for(CCODModuleInfo moduleInfo : domainInfo.getModules())
                        {
                            isAdd = true;
                            break;
                        }
                    }
                }
            }
            list.add(newPlat);
        }
        return list;
    }

    private CCODPlatformInfo reduceData(CCODPlatformInfo platformInfo)
    {
        CCODPlatformInfo platform = platformInfo.clone();
        for(CCODSetInfo setInfo : platform.getSets())
        {
            if(setInfo.getDomains().size() > 1)
            {
                List<CCODDomainInfo> domainList = new ArrayList<>();
                domainList.add(setInfo.getDomains().get(0));
                setInfo.setDomains(domainList);
            }
        }
        return platformInfo;
    }

    private PlatformUpdateSchemaInfo generateDemoSchemaFromPlatform(CCODPlatformInfo src, PlatformUpdateTaskType taskType, DomainUpdateType updateType, AppUpdateOperation operation, UpdateStatus updateStatus, List<AppPo> appList)
    {
        List<PlatformAppDeployDetailVo> deployApps = new ArrayList<>();
        for(CCODSetInfo setInfo : src.getSets())
        {

        }
        return null;
    }

    private DomainUpdatePlanInfo generateDomainUpdatePlan(DomainUpdateType updateType, UpdateStatus updateStatus, AppUpdateOperation operation, int domId, String domainName, String domainId, List<CCODModuleInfo> deployApps, List<AppPo> appList)
    {
        Date now = new Date();
        DomainUpdatePlanInfo planInfo = new DomainUpdatePlanInfo();
        planInfo.setDomId(domId);
        planInfo.setDomainId(domainId);
        planInfo.setDomainName(domainName);
        planInfo.setCreateTime(now);
        planInfo.setUpdateTime(now);
        planInfo.setExecuteTime(now);
        List<AppUpdateOperationInfo> operationList = new ArrayList<>();
        Map<String, List<AppPo>> nameAppMap = appList.stream().collect(Collectors.groupingBy(AppPo::getAppName));
        for(CCODModuleInfo deployApp : deployApps)
        {
            Map<String, AppPo> versionAppMap = nameAppMap.get(deployApp.getModuleName()).stream().collect(Collectors.toMap(AppPo::getVersion, Function.identity()));
            AppPo chosenApp = versionAppMap.get(deployApp.getVersion());
            if(!DomainUpdateType.ADD.equals(updateType))
            {
                for(String version : versionAppMap.keySet())
                {
                    if(!version.equals(deployApp.getVersion()))
                    {
                        chosenApp = versionAppMap.get(version);
                        break;
                    }
                }
            }
            AppUpdateOperationInfo operationInfo = generateAppUpdateOperation(operation, deployApp, chosenApp, updateStatus);
            operationList.add(operationInfo);
        }
        planInfo.setAppUpdateOperationList(operationList);
        return planInfo;
    }

    private PlatformUpdateSchemaInfo generatePlatformUpdateSchema(CCODPlatformInfo srcPlatform, PlatformUpdateTaskType taskType, DomainUpdateType updateType, UpdateStatus updateStatus, AppUpdateOperation operation, List<AppPo> appList)
    {
        PlatformUpdateSchemaInfo schemaInfo = new PlatformUpdateSchemaInfo();
        Date now = new Date();
        schemaInfo.setUpdateTime(now);
        schemaInfo.setDeadline(now);
        schemaInfo.setExecuteTime(now);
        schemaInfo.setTaskType(taskType);
        schemaInfo.setStatus(updateStatus);
        schemaInfo.setBkBizId(srcPlatform.getBizId());
        schemaInfo.setCreateTime(now);
        schemaInfo.setComment(taskType.desc);
        schemaInfo.setPlatformName(srcPlatform.getPlatformName());
        schemaInfo.setTitle(String.format("%s平台%s计划", srcPlatform.getPlatformName(), taskType.name));
        List<DomainUpdatePlanInfo> planList = new ArrayList<>();
        for(CCODSetInfo setInfo : srcPlatform.getSets())
        {
            for(CCODDomainInfo domainInfo : setInfo.getDomains())
            {
                if(domainInfo.getModules().size() > 0)
                {
                    DomainUpdatePlanInfo planInfo = new DomainUpdatePlanInfo();
                    planInfo.setExecuteTime(now);
                    planInfo.setUpdateTime(now);
                    planInfo.setCreateTime(now);
                    planInfo.setDomainName(domainInfo.getDomainName());
                    planInfo.setDomainId(domainInfo.getDomainId());
                    planInfo.setComment(String.format("%s域%s计划", domainInfo.getDomainName(), updateType.name));
                    planInfo.setDomId(domainInfo.getDomId());
                    planInfo.setStatus(updateStatus);
                    planInfo.setUpdateType(updateType);
                    planList.add(planInfo);
                }
            }
        }
        schemaInfo.setDomainUpdatePlanList(planList);
        return schemaInfo;
    }

    private AppUpdateOperationInfo generateAppUpdateOperation(AppUpdateOperation operation, CCODModuleInfo deployApp, AppPo targetApp, UpdateStatus updateStatus)
    {
        Date now = new Date();
        AppUpdateOperationInfo info = new AppUpdateOperationInfo();
        info.setAppRunner(deployApp.getAppRunner());
        info.setBasePath(deployApp.getBasePath());
        info.setBkModuleId(deployApp.getBkModuleId());
        info.setBzHostId(deployApp.getBkHostId());
        info.setCfgs(new ArrayList<>());
        for(PlatformAppCfgFilePo cfg : deployApp.getCfgs())
        {
            NexusAssetInfo assetInfo = new NexusAssetInfo();
            Checksum checksum = new Checksum();
            checksum.md5 = cfg.getMd5();
            assetInfo.setChecksum(checksum);
            assetInfo.setId(cfg.getNexusAssetId());
            assetInfo.setPath(cfg.getNexusDirectory());
            assetInfo.setRepository(cfg.getNexusRepository());
            info.getCfgs().add(assetInfo);
        }
        info.setOperation(operation);
        info.setOriginalAppId(deployApp.getAppId());
        info.setStatus(updateStatus);
        info.setUpdateTime(now);
        info.setTargetAppId(deployApp.getAppId());
        switch (operation)
        {
            case ADD:
                info.setBkModuleId(0);
                info.setOriginalAppId(0);
                break;
            case DELETE:
                info.setTargetAppId(0);
                info.setCfgs(new ArrayList<>());
                break;
            case CFG_UPDATE:
                info.setTargetAppId(0);
                break;
            default:
                break;
        }
        return info;
    }

    @Override
    public List<BizSetDefine> queryCCODBizSet() {
        logger.info(String.format("begin to query all set info of ccod biz"));
        List<BizSetDefine> setList = new ArrayList<>();
        setList.addAll(this.basicBizSetMap.values());
        setList.addAll(this.extendBizSetMap.values());
        logger.info(String.format("ccod biz has %d set", setList.size()));
        return setList;
    }

    @Override
    public List<String> queryAppsInSet(String setId) throws ParamException {
        logger.info(String.format("begin to query all appName in setId=%s", setId));
        if(!this.basicBizSetMap.containsKey(setId) && !this.extendBizSetMap.containsKey(setId))
        {
            logger.error(String.format("%s not in ccod biz basic sets and extend sets, so it is not a legal set id", setId));
            throw new ParamException(String.format("ccod biz has not set with id=%s", setId));
        }
        String[] apps = this.basicBizSetMap.containsKey(setId) ? this.basicBizSetMap.get(setId).getApps() : this.extendBizSetMap.get(setId).getApps();
        logger.info(String.format("find %s relative to setId=%s", String.join(",", apps), setId));
        return Arrays.asList(apps);
    }

    @Override
    public LJSetInfo createNewBizSet(int bkBizId, String bkSetName, String desc, int capacity) throws ParamException, LJPaasException {
        return null;
    }


    /**
     * 向蓝鲸paas指定biz添加一个新的set
     * @param bkBizId set所属的biz id
     * @param bkSetName 需要创建的set名字
     * @param desc 该set的描述
     * @param capacity 描述
     * @throws LJPaasException 蓝鲸paas返回创建失败信息
     */
    private LJSetInfo addNewBizSet(int bkBizId, String bkSetName, String desc, int capacity) throws InterfaceCallException, LJPaasException
    {
        logger.info(String.format("begin to add new set=%s of biz=%d, desc=%s and capacity=%d",
                bkSetName, bkBizId, desc, capacity));
        String url = String.format(this.createNewSetUrlFmt, this.paasHostUrl);
        Map<String, String> headersMap = new HashMap<>();
        Map<String, Object> paramsMap = getLJPaasCallBaseParams();
        paramsMap.put("bk_biz_id", bkBizId + "");
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("bk_parent_id", bkBizId);
        dataMap.put("bk_supplier_id", "0");
        dataMap.put("bk_set_name", bkSetName);
        dataMap.put("bk_set_desc", bkSetName);
        dataMap.put("bk_capacity", capacity);
        dataMap.put("description", String.format("create by tools"));
        paramsMap.put("data", dataMap);
        String retVal = HttpTools.httpPostRequest(url, headersMap, paramsMap);
        String data = parsePaasInterfaceResult(retVal);
        LJSetInfo setInfo = JSONObject.parseObject(data, LJSetInfo.class);
        return setInfo;
    }

    private void deleteExistBizSet(int bkBizId, int bkSetId) throws InterfaceCallException, LJPaasException
    {
        logger.info(String.format("begin to delete bkSetId=%d of bkBizId=%s", bkSetId, bkBizId));
        Map<String, Object> paramsMap = getLJPaasCallBaseParams();
        paramsMap.put("bk_biz_id", bkBizId + "");
        paramsMap.put("bk_set_id", bkSetId + "");
        Map<String, String> headersMap = new HashMap<>();
        String url = String.format(this.deleteSetUrlFmt, this.paasHostUrl);
        String result = HttpTools.httpPostRequest(url, headersMap, paramsMap);
        parsePaasInterfaceResult(result);
        logger.info(String.format("delete bkSetId=%d of bkBizId=%s SUCCESS", bkSetId, bkBizId));
    }

    private LJModuleInfo addNewBkModule(int bkBizId, int bkSetId, String moduleName) throws InterfaceCallException, LJPaasException
    {
        logger.info(String.format("begin to add new module=%s to bizId=%d and setId=%d",
                moduleName, bkBizId, bkSetId));
        Map<String, Object> paramsMap = getLJPaasCallBaseParams();
        paramsMap.put("bk_biz_id", bkBizId);
        paramsMap.put("bk_set_id", bkSetId);
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("bk_parent_id", bkSetId);
        dataMap.put("bk_module_name", moduleName);
        paramsMap.put("data", dataMap);
        Map<String, String> headersMap = new HashMap<>();
        String url = String.format(this.addModuleUrlFmt, this.paasHostUrl);
        String retVal = HttpTools.httpPostRequest(url, headersMap, paramsMap);
        String data = parsePaasInterfaceResult(retVal);
        return JSONObject.parseObject(data, LJModuleInfo.class);
    }

    private void deleteExistModule(int bkBizId, int bkSetId, int bkModuleId) throws InterfaceCallException, LJPaasException
    {
        logger.info(String.format("begin to delete bkModuleId=%d from bkBizId=%d and bkSetId=%d",
                bkModuleId, bkBizId, bkSetId));
        Map<String, Object> paramsMap = getLJPaasCallBaseParams();
        paramsMap.put("bk_biz_id", bkBizId);
        paramsMap.put("bk_set_id", bkSetId);
        paramsMap.put("bk_module_id", bkModuleId);
        Map<String, String> headersMap = new HashMap<>();
        String url = String.format(this.deleteModuleUrlFmt, this.paasHostUrl);
        String retVal = HttpTools.httpPostRequest(url, headersMap, paramsMap);
        parsePaasInterfaceResult(retVal);
    }

    /**
     * 在蓝鲸paas查询指定条件的module信息
     * @param bkBizId 需要查询的biz id,必填
     * @param bkSetId 需要查询的set id,必填
     * @param moduleId 需要查询的module id,可以为空，为空则忽略此条件
     * @param moduleName 需要查询的module name,可以为空,为空则忽略此条件
     * @return 查询结果
     * @throws InterfaceCallException 调用接口发生异常
     * @throws LJPaasException 蓝鲸报错或是返回结果异常
     */
    private List<LJModuleInfo> queryBkModule(int bkBizId, int bkSetId, Integer moduleId, String moduleName) throws InterfaceCallException, LJPaasException
    {
        logger.info(String.format("begin to query module info with bkBizId=%d and bkSetId=%d and bkModuleId=%s and bkModuleName=%s",
                bkBizId, bkSetId, moduleId, moduleName));
        List<LJModuleInfo> moduleList;
        Map<String, Object> paramsMap = getLJPaasCallBaseParams();
        paramsMap.put("bk_biz_id", bkBizId);
        paramsMap.put("bk_set_id", bkSetId);
        Map<String, Object> conditionMap = new HashMap<>();
        if(moduleId != null)
        {
            conditionMap.put("bk_module_id", moduleId);
        }
        if(moduleName != null)
        {
            conditionMap.put("bk_module_name", moduleName);
        }
        paramsMap.put("condition", conditionMap);
        String url = String.format(this.queryModuleUrlFmt, this.paasHostUrl);
        String retVal = HttpTools.httpPostRequest(url, paramsMap);
        String data = parsePaasInterfaceResult(retVal);
        try
        {
            JSONObject jsonObject = JSONObject.parseObject(data);
            moduleList = JSONArray.parseArray(jsonObject.getString("info"), LJModuleInfo.class);
        }
        catch (Exception ex)
        {
            logger.error(String.format("parse lj paas module info exception"), ex);
            throw new LJPaasException(String.format("parse lj paas module info exception"));
        }
        logger.info(String.format("find %d modules with bkBizId=%d and bkSetId=%d and bkModuleId=%s and bkModuleName=%s",
                moduleList.size(), bkBizId, bkSetId, moduleId, moduleName));
        return moduleList;
    }

    /**
     * 查询指定set下的所有主机
     * @param bkBizId 蓝鲸paas的biz id
     * @param bkSetId 蓝鲸paas的set id
     * @param bkModuleName 蓝鲸paas上定义的模块名
     * @param bkHostInnerIp 主机的内部ip
     * @return 指定set下的所有host
     * @throws Exception
     */
    private List<LJHostInfo> queryBKHost(Integer bkBizId, Integer bkSetId, String bkModuleName, String bkHostInnerIp) throws InterfaceCallException, LJPaasException
    {
        String queryUrl = String.format(this.queryHostUrlFmt, paasHostUrl);
        logger.debug(String.format("begin to query hosts for bkBizId=%d and bkSetId=%d and bkModuleName=%s and bkHostInnerIp=%s",
                bkBizId, bkSetId, bkModuleName, bkHostInnerIp));
        List<LJHostResourceInfo> resourceList = queryBKHostResource(bkBizId, bkSetId, bkModuleName, bkHostInnerIp);
        List<LJHostInfo> hostList = new ArrayList<>();
        for(LJHostResourceInfo resourceInfo : resourceList)
        {
            hostList.add(resourceInfo.getHost());
        }
        logger.debug(String.format("find %d host for bkBizId=%d and bkSetId=%d and bkModuleName=%s and bkHostInnerIp=%s",
                hostList.size(), bkBizId, bkSetId, bkModuleName, bkHostInnerIp));
        return hostList;
    }


    /**
     * 查询指定条件下的所有host resource
     * @param bkBizId 蓝鲸paas的biz id
     * @param bkSetId 蓝鲸paas的set id
     * @param bkModuleName 蓝鲸paas上定义的模块名
     * @param bkHostInnerIp 主机的内部ip
     * @return 满足条件的host resource
     * @throws Exception
     */
    private List<LJHostResourceInfo> queryBKHostResource(Integer bkBizId, Integer bkSetId, String bkModuleName, String bkHostInnerIp) throws InterfaceCallException, LJPaasException
    {
        String queryUrl = String.format(this.queryHostUrlFmt, paasHostUrl);
        logger.debug(String.format("begin to query host resource for bkBizId=%d and bkSetId=%d and bkHostInnerIp=%s",
                bkBizId, bkSetId, bkHostInnerIp));
        Map<String, Object> paramsMap = getLJPaasCallBaseParams();
        if(bkBizId != null)
        {
            paramsMap.put("bk_biz_id", bkBizId);
        }
        List<Map<String, Object>> conditionsList = new ArrayList<>();
        Map<String, Object> equalCondition = new HashMap<>();
        conditionsList.add(generateLJObjectParam("biz", new String[0], equalCondition));
        equalCondition = new HashMap<>();
        if(bkSetId != null)
        {
            equalCondition.put("bk_set_id", bkSetId);
        }
        conditionsList.add(generateLJObjectParam("set", new String[0], equalCondition));
        equalCondition = new HashMap<>();
        if(bkModuleName != null)
        {
            equalCondition.put("bk_module_name", bkModuleName);
        }
        conditionsList.add(generateLJObjectParam("module", new String[0], equalCondition));
        equalCondition = new HashMap<>();
        if(bkHostInnerIp != null)
        {
            equalCondition.put("bk_host_innerip", bkHostInnerIp);
        }
        conditionsList.add(generateLJObjectParam("host", new String[0], equalCondition));
        paramsMap.put("condition", conditionsList);
        String url = String.format(this.queryHostUrlFmt, this.paasHostUrl);
        String retVal = HttpTools.httpPostRequest(url, paramsMap);
        String data = parsePaasInterfaceResult(retVal);
        try
        {
            String info = JSONObject.parseObject(data).getString("info");
            List<LJHostResourceInfo> resourceList = JSONArray.parseArray(info, LJHostResourceInfo.class);
            logger.info(String.format("find %d host resource for bkBizId=%d and bkSetId=%d and bkHostInnerIp=%s",
                    resourceList.size(), bkBizId, bkSetId, bkHostInnerIp));;
            return resourceList;
        }
        catch (Exception ex)
        {
            logger.error(String.format("parse lj %s return exception", url), ex);
            throw new LJPaasException(String.format("parse paas return message exception"));
        }
    }

    private Map<String, Object> getLJPaasCallBaseParams()
    {
        Map<String, Object> paramsMap = new HashMap<>();
        paramsMap.put("bk_app_code", bkAppCode);
        paramsMap.put("bk_app_secret", bkAppSecret);
        paramsMap.put("bk_username", bkUserName);
        return paramsMap;
    }

    /**
     * 修改已有的主机上绑定的模块信息
     * @param bkBizId paas上的biz id
     * @param hostIdList paas上的host列表
     * @param moduleIdList 需要绑定的模块id列表
     * @param isIncrement 是追加还是覆盖,true追加,false覆盖
     * @throws InterfaceCallException
     * @throws LJPaasException
     */
    private void transferModulesToHost(int bkBizId, Integer[] hostIdList, Integer[] moduleIdList, boolean isIncrement) throws InterfaceCallException, LJPaasException
    {
        Map<String, Object> paramsMap = getLJPaasCallBaseParams();
        paramsMap.put("bk_biz_id", bkBizId);
        paramsMap.put("bk_host_id", hostIdList);
        paramsMap.put("bk_module_id", moduleIdList);
        paramsMap.put("is_increment", isIncrement);
        String url = String.format(this.transferModuleUrlFmt, this.paasHostUrl);
        String retVal = HttpTools.httpPostRequest(url, paramsMap);
        parsePaasInterfaceResult(retVal);
    }

    /**
     * 将一组新的主机添加到idle pool去
     * @param bkBizId 需要添加新主机的biz的id
     * @param newHostIps 被添加的主机ip
     * @param bkCloudId 该服务器所处的云id
     * @throws InterfaceCallException 接口调用失败
     * @throws LJPaasException 接口返回调用失败或是解析接口调用结果失败
     */
    private void addNewHostToIdlePool(int bkBizId, List<String> newHostIps, int bkCloudId) throws InterfaceCallException, LJPaasException
    {
        logger.info(String.format("begin to add %s to bkBizId=%d", String.join(",", newHostIps), bkBizId));
        Map<String, Object> paramsMap = getLJPaasCallBaseParams();
        paramsMap.put("bk_biz_id", bkBizId);
        Map<String, Object> hostMap = new HashMap<>();
        for(int i = 0; i < newHostIps.size(); i++)
        {
            Map<String, Object> hostInfo = new HashMap<>();
            hostInfo.put("bk_host_innerip", newHostIps.get(i));
            hostInfo.put("bk_cloud_id", bkCloudId);
            hostInfo.put("import_from", "3");
            hostMap.put(i + "", hostInfo);
        }
        paramsMap.put("host_info", hostMap);
        String url = String.format(this.addHostUrlFmt, this.paasHostUrl);
        String retVal = HttpTools.httpPostRequest(url, paramsMap);
        parsePaasInterfaceResult(retVal);
        logger.info(String.format("add %s to bkBizId=%d : SUCCESS", String.join(",", newHostIps), bkBizId));
    }

    private void transferHostToIdlePool(int bkBizId, Integer[] hostList) throws InterfaceCallException, LJPaasException
    {
        logger.info(String.format("begin to transfer hosts=%s of bkBizId=%d to idle pool",
                JSONArray.toJSONString(hostList), bkBizId));
        String url = String.format(this.transferHostToIdlePoolUrlFmt, this.paasHostUrl);
        Map<String, Object> paramsMap = getLJPaasCallBaseParams();
        paramsMap.put("bk_biz_id", bkBizId);
        paramsMap.put("bk_host_id", hostList);
        String retVal = HttpTools.httpPostRequest(url, paramsMap);
        parsePaasInterfaceResult(retVal);
        logger.info(String.format("transfer hosts=%s of bkBizId=%d to idle pool : SUCCESS",
                JSONArray.toJSONString(hostList), bkBizId));
    }

    private void transferHostToResource(int bkBizId, Integer[] hostList) throws InterfaceCallException, LJPaasException
    {
        logger.info(String.format("begin to transfer hosts=%s of bkBizId=%d to resource",
                JSONArray.toJSONString(hostList), bkBizId));
        String url = String.format(this.transferHostToResourceUrlFmt, this.paasHostUrl);
        Map<String, Object> paramsMap = getLJPaasCallBaseParams();
        paramsMap.put("bk_biz_id", bkBizId);
        paramsMap.put("bk_host_id", hostList);
        String retVal = HttpTools.httpPostRequest(url, paramsMap);
        parsePaasInterfaceResult(retVal);
        logger.info(String.format("transfer hosts=%s of bkBizId=%d to resource : SUCCESS",
                JSONArray.toJSONString(hostList), bkBizId));
    }

    private String parsePaasInterfaceResult(String interfaceResult) throws LJPaasException
    {
        JSONObject jsonObject;
        try
        {
            jsonObject = JSONObject.parseObject(interfaceResult);
        }
        catch (Exception ex)
        {
            logger.error(String.format("parse paas interface result exception", ex));
            throw new LJPaasException(String.format("parse paas interface result exception"));
        }
        try
        {
            boolean isSucc = jsonObject.getBoolean("result");
            if(!isSucc)
            {
                String errMsg = jsonObject.getString("message");
                logger.error(String.format("lj paas return errorMsg[%s]", errMsg));
                throw new LJPaasException(errMsg);
            }
            else
            {
                String data = jsonObject.getString("data");
                JSONObject jso = JSONObject.parseObject(data);
                logger.info(String.format("lj paas return data : %s", data));
                return data;
            }
        }
        catch (LJPaasException e)
        {
            throw e;
        }
        catch (Exception ex)
        {
            logger.error(String.format("parse paas interface result exception", ex));
            throw new LJPaasException(String.format("parse paas interface result exception"));
        }
    }

    /**
     * 将一个已经存在的biz重置,并给它创建指定的set
     * @param bkBizId 需要重置的set名
     * @param setNames 需要创建的set名
     * @throws InterfaceCallException
     * @throws LJPaasException
     */
    private List<LJSetInfo> resetExistBiz(int bkBizId, List<String> setNames) throws InterfaceCallException, LJPaasException
    {
        List<LJHostInfo> hostList = queryBKHost(bkBizId, null, null, null);
        Integer[] hostIds = hostList.stream().collect(Collectors.groupingBy(LJHostInfo::getHostId)).keySet().toArray(new Integer[0]);
        if(hostIds.length > 0)
        {
            transferHostToIdlePool(bkBizId, hostIds);
//            transferHostToResource(bkBizId, hostIds);
        }
        List<LJSetInfo> setList = queryLJPaasBizSet(bkBizId);
        for(LJSetInfo set : setList)
        {
            if(this.paasIdlePoolSetName.equals(set.getSetName()))
                continue;
            List<LJModuleInfo> moduleList = this.queryBkModule(bkBizId, set.getSetId(), null, null);
            for(LJModuleInfo module : moduleList)
            {
                deleteExistModule(bkBizId, set.getSetId(), module.getModuleId());
            }
            deleteExistBizSet(bkBizId, set.getSetId());
        }
        List<LJSetInfo> createSets = new ArrayList<>();
        for(String setName : setNames)
        {
            LJSetInfo setInfo = addNewBizSet(bkBizId, setName, String.format("%s is created by cmdb", setName), 2000);
        }
        return queryLJPaasBizSet(bkBizId);
    }

    private void autoCreateCCODBizFromClientCollectResult(int bkBizId, String platformName, int hostCloudId, List<PlatformAppDeployDetailVo> deployAppList)
            throws NotSupportAppException, InterfaceCallException, LJPaasException
    {
        List<PlatformAppDeployDetailVo> deployApps = makeUpBizInfoForDeployApps(bkBizId, deployAppList);
        Map<String, List<PlatformAppDeployDetailVo>> setAppMap = deployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getSetName));
        List<String> setNames = new ArrayList<>(this.basicBizSetMap.keySet());
        setNames.addAll(new ArrayList<>(this.extendBizSetMap.keySet()));
        Collections.sort(setNames);
        Map<String, LJSetInfo> setMap = resetExistBiz(bkBizId, setNames).stream().collect(Collectors.toMap(LJSetInfo::getSetName, Function.identity()));
        Map<String, List<PlatformAppDeployDetailVo>> hostIpAppMap = deployApps.stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getHostIp));
        List<String> ipList = new ArrayList<>(hostIpAppMap.keySet());
        ipSort(ipList);
        addNewHostToIdlePool(bkBizId, ipList, hostCloudId);
        Map<String, LJHostInfo> hostMap = queryBKHost(bkBizId, setMap.get(this.paasIdlePoolSetName).getSetId(), null, null).stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity()));
        Map<String, List<Integer>> hostModuleMap = new HashMap<>();
        for(String hostIp : ipList)
        {
            if(!hostMap.containsKey(hostIp))
            {
                logger.error(String.format("%s is not added to idle pool=%d of bkBizId=%d",
                        hostIp, setMap.get(this.paasIdlePoolSetName).getSetId(), bkBizId));
                throw new LJPaasException(String.format("add %s host to idle pool for %s : FAIL", hostIp, platformName));
            }
            hostModuleMap.put(hostIp, new ArrayList<>());
        }
        for(String setName : setAppMap.keySet())
        {
            Map<String, List<PlatformAppDeployDetailVo>> deployAppMap = setAppMap.get(setName).stream().collect(Collectors.groupingBy(PlatformAppDeployDetailVo::getAppAlias));
            List<String> appAliasList = new ArrayList<>(deployAppMap.keySet());
            Collections.sort(appAliasList);
            for(String appAlias : appAliasList)
            {
                LJModuleInfo module = addNewBkModule(bkBizId, setMap.get(setName).getSetId(), appAlias);
                for(PlatformAppDeployDetailVo deployApp : deployAppMap.get(appAlias))
                {
                    hostModuleMap.get(deployApp.getHostIp()).add(module.getModuleId());
                }
            }
        }
        for(String hostIp : ipList)
        {
            transferModulesToHost(bkBizId, new Integer[]{hostMap.get(hostIp).getHostId()}, hostModuleMap.get(hostIp).toArray(new Integer[0]), false);
        }
    }

    /**
     * 将一组应用添加到指定的set下
     * @param bkBizId
     * @param set
     * @param deployAppList
     * @param hostList 归属该平台的主机列表
     */
    private void addNewAppsToBkSet(int bkBizId, LJSetInfo set, List<PlatformAppDeployDetailVo> deployAppList, List<LJHostInfo> hostList) throws InterfaceCallException, LJPaasException, DBPAASDataNotConsistentException
    {
        Map<String, LJHostInfo> hostMap = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostInnerIp, Function.identity()));
        Map<String, LJModuleInfo> moduleMap = queryBkModule(bkBizId, set.getSetId(), null, null)
                .stream().collect(Collectors.toMap(LJModuleInfo::getModuleName, Function.identity()));

    }

    private void addNewDomain(String platformId, String domainId, String domainName, int bkBizId, LJSetInfo bkSet, List<LJHostInfo> hostList, List<AppUpdateOperationInfo> deployAppList) throws DataAccessException, InterfaceCallException, LJPaasException, ParamException
    {
        Map<Integer, LJHostInfo> hostMap = hostList.stream().collect(Collectors.toMap(LJHostInfo::getHostId, Function.identity()));
        Map<Integer, Set<Integer>> transferAppMap = new HashMap<>();
        for(AppUpdateOperationInfo deployApp : deployAppList)
        {
            if(!AppUpdateOperation.ADD.equals(deployApp.getOperation()))
            {
                logger.error(String.format("create domain can not include %s operation", deployApp.getOperation().name));
                throw new ParamException(String.format("create domain can not include %s operation", deployApp.getOperation().name));
            }
            if(!hostMap.containsKey(deployApp.getBzHostId()))
            {
                logger.error(String.format("bkBizId=%d has not bkHostId=%d host", bkBizId, deployApp.getBzHostId()));
                throw new ParamException(String.format("bkBizId=%d has not bkHostId=%d host", bkBizId, deployApp.getBzHostId()));
            }
            if(!transferAppMap.containsKey(deployApp.getBzHostId()))
            {
                transferAppMap.put(deployApp.getBzHostId(), new HashSet<>());
            }
        }
        Map<String, LJModuleInfo> moduleMap = queryBkModule(bkBizId, bkSet.getSetId(), null, null)
                .stream().collect(Collectors.toMap(LJModuleInfo::getModuleName, Function.identity()));
        for(AppUpdateOperationInfo deployApp : deployAppList)
        {
            if(!moduleMap.containsKey(deployApp.getAppAlias()))
            {
                LJModuleInfo moduleInfo = addNewBkModule(bkBizId, bkSet.getSetId(), deployApp.getAppAlias());
                moduleMap.put(deployApp.getAppAlias(), moduleInfo);
            }
            transferAppMap.get(deployApp.getBzHostId()).add(moduleMap.get(deployApp.getAppAlias()).getModuleId());
        }
        for(Integer bkHostId : transferAppMap.keySet())
        {
            transferModulesToHost(bkBizId, new Integer[]{bkHostId}, transferAppMap.get(bkHostId).toArray(new Integer[0]), true);
        }
        Date now = new Date();
        DomainPo domainPo = new DomainPo();
        domainPo.setDomainName(domainName);
        domainPo.setCreateTime(now);
        domainPo.setUpdateTime(now);
        domainPo.setStatus(1);
        domainPo.setPlatformId(platformId);
        domainPo.setDomainId(domainId);
        domainPo.setComment(String.format("created by tools automatic"));
        domainPo.setBkSetId(bkSet.getSetId());
        this.domainMapper.insert(domainPo);

    }

    @Override
    public void deleteBizSet(int bkBizId, int bkSetId) throws ParamException, LJPaasException {

    }

    @Override
    public void syncClientCollectResultToPaas(int bkBizId, String platformId, int hostCloudId) throws ParamException, NotSupportAppException, InterfaceCallException, LJPaasException {
        logger.info(String.format("begin to sync client collect apps deploy details to lj paas : bkBizId=%d and platformId=%s",
                bkBizId, platformId));
        List<LJBizInfo> bizList = queryBKBiz(bkBizId, null);
        if(bizList.size() == 0)
        {
            logger.error(String.format("biz with bkBizId=%d and bkBizName=%s not exist", bkBizId, platformId));
            throw new ParamException(String.format("biz with bkBizId=%d and bkBizName=%s not exist", bkBizId, platformId));
        }
        String platformName = bizList.get(0).getBizName();
        List<PlatformAppDeployDetailVo> deloyAppList = platformAppDeployDetailMapper.selectPlatformApps(platformId, null, null, null);
        if(deloyAppList.size() == 0)
        {
            logger.error(String.format("platformId=%s has not collected platform app deploy info record", platformId));
            throw new ParamException(String.format("platformId=%s has not collected platform app deploy info record", platformId));
        }
        if(!platformName.equals(deloyAppList.get(0).getPlatformName()))
        {
            logger.error(String.format("bkBizId=%s with bkBizName=%s is not equal platformId=%s with platformName=%s",
                    bkBizId, platformName, platformId, deloyAppList.get(0).getPlatformName()));
            throw new ParamException(String.format("bkBizId=%s with bkBizName=%s is not equal platformId=%s with platformName=%s",
                    bkBizId, platformName, platformId, deloyAppList.get(0).getPlatformName()));
        }
        autoCreateCCODBizFromClientCollectResult(bkBizId, platformName, hostCloudId, deloyAppList);
    }

    private void initParamForTest()
    {
        this.paasHostUrl = "http://paas.ccod.com:80";

        this.bkAppCode = "wyffirstsaas";

        this.bkAppSecret = "8a4c0887-ca15-462b-8804-8bedefe1f352";

        this.bkUserName = "admin";

        this.isDevelop = true;

        this.paasIdlePoolSetName = "idle pool";

        this.paasIdlePoolSetId = "ccodIdlePool";

        this.updateSchemaSetName = "平台升级方案";

        this.updateSchemaSetId = "platformUpdateSchema";

        this.defaultCloudId = 5;
    }

    public void ipSort(List<String> ipList)
    {
        ipList.stream().sorted((e1,e2) -> {

            StringTokenizer token=new StringTokenizer(e1,".");
            StringTokenizer token2=new StringTokenizer(e2,".");
            while (token.hasMoreTokens() && token2.hasMoreTokens()){
                int parseInt = Integer.parseInt(token.nextToken());
                int parseInt2 = Integer.parseInt(token2.nextToken());
                if(parseInt > parseInt2) {
                    return 1;
                }
                if(parseInt < parseInt2) {
                    return -1;
                }

            }
            if(token.hasMoreElements()) { // e1还有值，则e2已遍历完
                return 1;
            }else {
                return -1;
            }
        }).forEach(System.out::println);
    }

    @Test
    public void autoCreateBizTest()
    {
        initParamForTest();
        try
        {
            List<LJSetInfo> setList = resetExistBiz(14, Arrays.asList(new String[]{"测试域1", "测试域2", "测试域3"}));
            System.out.println(JSONArray.toJSONString(setList));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }


    @Test
    public void jsonTest()
    {
        try
        {
            String haha = "thada";
            JSONObject jsonObject = JSONObject.parseObject(haha);
            AppUpdateOperationVo operationVo = new AppUpdateOperationVo();
            operationVo.setOperation(AppUpdateOperation.ADD);
            String str = JSONObject.toJSONString(operationVo);
            System.out.println(str);
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
    public void setOperationTest()
    {
        this.initParamForTest();
        try
        {
            LJSetInfo setInfo = addNewBizSet(11, "域服务222", "域服222务", 1000);
            deleteExistBizSet(setInfo.getBizId(), setInfo.getSetId());
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Test
    public void moduleOperationTest()
    {
        this.initParamForTest();
        try
        {
//            LJModuleInfo moduleInfo = addNewBkModule(11, 70, "dcs");
//            deleteExistModule(moduleInfo.getBizId(), moduleInfo.getSetId(), moduleInfo.getModuleId());
//            List<LJModuleInfo> moduleInfos = queryBkModule(11, 69, null, null);
//            System.out.println(JSONArray.toJSONString(moduleInfos));
            transferModulesToHost(11, new Integer[]{198}, new Integer[]{234}, true);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Test
    public void hostQueryTest()
    {
        this.initParamForTest();
        try
        {
//            queryLJPaasBKHostResource("http://paas.ccod.com:80", "wyffirstsaas", "8a4c0887-ca15-462b-8804-8bedefe1f352", "admin");
//            queryIdleHost(10, 30, "http://paas.ccod.com:80", "wyffirstsaas", "8a4c0887-ca15-462b-8804-8bedefe1f352", "admin");
//            List<LJHostInfo> hostList = queryBKHost(11, null, "cms", null);
//            System.out.println(JSONArray.toJSONString(hostList));
            transferHostToIdlePool(11, new Integer[]{198});
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    @Test
    public void ipSortTest()
    {
        List<String> list = Arrays.asList(
                "003.322.805.822.840.438.220.274",
                "055.786.157.416.245",
                "077.134.673.105.355.003.758.727.066",
                "085.013.435.523.224",
                "152.441.564.586.073",
                "152.177.480",
                "152.465.444.522.626.526.568",
                "152.177.480.748.018.647.570",
                "323.624",
                "356.773.718.782.171.536.871",
                "364.180.121.483.601.678.067",
                "402.107.014",
                "472.602.046",
                "472.602.046.263.170",
                "472.602.046.263.803",
                "527.530.350.778.137.513.335",
                "536.017.404.734.537.134.241",
                "604.255.236.550",
                "640.117.263.314.358.353.678",
                "677.873.326.803.167.528.474",
                "733.212.422",
                "783.850.435.605.204.862.722.563.417",
                "800.461.476.404.442.666.212",
                "810.454.842.314.848.623",
                "823.405.158.606",
                "833.204.283.833.320.664.236",
                "854.367.556.645.628.764.760"
        );
        list.stream().sorted((e1,e2) -> {

            StringTokenizer token=new StringTokenizer(e1,".");
            StringTokenizer token2=new StringTokenizer(e2,".");
            while (token.hasMoreTokens() && token2.hasMoreTokens()){
                int parseInt = Integer.parseInt(token.nextToken());
                int parseInt2 = Integer.parseInt(token2.nextToken());
                if(parseInt > parseInt2) {
                    return 1;
                }
                if(parseInt < parseInt2) {
                    return -1;
                }

            }
            if(token.hasMoreElements()) { // e1还有值，则e2已遍历完
                return 1;
            }else {
                return -1;
            }
        });
        for(String ip : list)
        {
            System.out.println(ip);
        }
    }
}
