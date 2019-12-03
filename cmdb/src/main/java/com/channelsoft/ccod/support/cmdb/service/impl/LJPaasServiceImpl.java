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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private String queryBizUrlFmt = "%s/api/c/compapi/v2/cc/search_business/";

    private String queryBizSetUrlFmt = "%s/api/c/compapi/v2/cc/search_set/";

    private String queryHostUrlFmt = "%s/api/c/compapi/v2/cc/search_host/";

    private final static Logger logger = LoggerFactory.getLogger(LJPaasServiceImpl.class);

    @Override
    public LJBKInfo queryBizInfoById(int bizId) throws Exception {
        return null;
    }

    @Override
    public LJBKInfo[] queryBizInfo() throws Exception {
        return new LJBKInfo[0];
    }

//    private boolean generateLJBKInfo(LJBKInfo bkInfo, Map<Integer, List<LJSetInfo>> bizSetMap, Map<Integer, Map<Integer, Map<String, LJBKHostInfo>>> bizSetHostMap,
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
//            bkInfo.setUsedHosts(new LJBKHostInfo[0]);
//            bkInfo.setIdleHosts(new LJBKHostInfo[0]);
//        }
//        else
//        {
//            List<LJBKHostInfo> idleList = new ArrayList<>();
//            List<LJBKHostInfo> usedList = new ArrayList<>();
//            Map<Integer, Map<String, LJBKHostInfo>> setHostMap = bizSetHostMap.get(bkInfo.getBizId());
//            for(Map.Entry<Integer, Map<String, LJBKHostInfo>> entry : setHostMap.entrySet())
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
//                    for(LJBKHostInfo hostInfo : entry.getValue().values())
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
//            bkInfo.setUsedHosts(usedList.toArray(new LJBKHostInfo[0]));
//            bkInfo.setIdleHosts(idleList.toArray(new LJBKHostInfo[0]));
//        }
//        return true;
//    }

    private List<LJBKInfo> queryLJPaasBKBizInfo(String paasHostUrl, String bkAppCode, String bkAppSecret, String bkUserName) throws Exception
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
        List<LJHostResourceInfo> hosts = JSONArray.parseArray(data, LJHostResourceInfo.class);
        System.out.println(hosts.size());
//        Map<Integer, LJBKInfo> bkMap = new HashMap<>();
//        Map<Integer, LJSetInfo> setMap = new HashMap<>();
//        Map<Integer, LJBKHostInfo> hostInfoMap = new HashMap<>();
        return null;
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

//    @Test
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
