package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.constant.CCODPlatformStatus;
import com.channelsoft.ccod.support.cmdb.dao.PlatformMapper;
import com.channelsoft.ccod.support.cmdb.po.PlatformAppPo;
import com.channelsoft.ccod.support.cmdb.po.PlatformPo;
import com.channelsoft.ccod.support.cmdb.service.ILJPaasService;
import com.channelsoft.ccod.support.cmdb.vo.*;
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

    private String queryIdlePoolUrlFmt = "%s/api/c/compapi/v2/cc/search_host/";

    private final static Logger logger = LoggerFactory.getLogger(LJPaasServiceImpl.class);

    @Override
    public LJBKInfo queryBizInfoById(int bizId) throws Exception {
        return null;
    }

    @Override
    public LJBKInfo[] queryBizInfo() throws Exception {
        return new LJBKInfo[0];
    }

    private boolean generateLJBKInfo(LJBKInfo bkInfo, Map<Integer, List<LJSetInfo>> bizSetMap, Map<Integer, Map<Integer, Map<String, LJBKHostInfo>>> bizSetHostMap,
                                  Map<Integer, Map<String, LJModuleInfo>> hostModuleMap, Map<String, PlatformPo> namePlatformMap,
                                  Map<String, Map<String, Map<String, PlatformAppDeployDetailVo>>> platformHostAppMap)
    {
        if(bizSetMap.containsKey(bkInfo.getBizId()))
        {
            logger.error(String.format("paas data error : there is not any set for bizId=%s and bizName=%s",
                    bkInfo.getBizId(), bkInfo.getBizName()));
            return false;
        }
        boolean isNew = false;
        if(!namePlatformMap.containsKey(bkInfo.getBizName()))
        {
            logger.info(String.format("database not contain platformName=%s platform info, so %s is new created platform",
                    bkInfo.getBizName(), bkInfo.getBizName()));
            bkInfo.setStatus(CCODPlatformStatus.NEW_CREATE.id);
            isNew = true;
        }
        for(LJSetInfo setInfo : bizSetMap.get(bkInfo.getBizId()))
        {
            switch (setInfo.getSetName())
            {
                case "idle pool":
                    bkInfo.setIdlePools(setInfo);
                    break;
                case "故障自愈":
                    bkInfo.setHealingSet(setInfo);
                    break;
                case "数据服务模块":
                    bkInfo.setDataSets(setInfo);
                    break;
                case "公共组件":
                    bkInfo.setPublicModuleSet(setInfo);
                    break;
                case "集成平台":
                    bkInfo.setIntegrationSet(setInfo);
                    break;
                case "作业平台":
                    bkInfo.setJobSet(setInfo);
                    break;
                case "配置平台":
                    bkInfo.setCfgSet(setInfo);
                    break;
                case "管控平台":
                    bkInfo.setControlSet(setInfo);
                    break;
                default:
                    logger.error(String.format("unknown biz set name=%s", setInfo.getSetName()));
            }
        }
        if(bkInfo.getIdlePools() == null)
        {
            logger.error(String.format("%s platform has not idle pools set", bkInfo.getBizName()));
            return false;
        }
        if(!bizSetHostMap.containsKey(bkInfo.getBizId()))
        {
            logger.warn(String.format("%s platform has not any host resource", bkInfo.getBizName()));
            bkInfo.setUsedHosts(new LJBKHostInfo[0]);
            bkInfo.setIdleHost(new LJBKHostInfo[0]);
        }
        else
        {
            List<LJBKHostInfo> idleList = new ArrayList<>();
            List<LJBKHostInfo> usedList = new ArrayList<>();
            Map<Integer, Map<String, LJBKHostInfo>> setHostMap = bizSetHostMap.get(bkInfo.getBizId());
            for(Map.Entry<Integer, Map<String, LJBKHostInfo>> entry : setHostMap.entrySet())
            {
                if(entry.getKey() != bkInfo.getIdlePools().getSetId() && isNew)
                {
                    logger.error(String.format("biz=%d and bizName=%s platform is new create but found apps had been deployed",
                            bkInfo.getBizId(), bkInfo.getBizName()));
                    return false;
                }
                else if(entry.getKey() == bkInfo.getIdlePools().getSetId())
                {
                    idleList.addAll(entry.getValue().values());
                }
                else
                {
                    if(!platformHostAppMap.containsKey(bkInfo.getBizName()))
                    {
                        logger.error(String.format("lj paas report bizId=%d and bizName=%s is used platform but cmdb database not such record",
                                bkInfo.getBizId(), bkInfo.getBizName()));
                        return false;
                    }
                    Map<String, Map<String, PlatformAppDeployDetailVo>> hostPlatformAppMap = platformHostAppMap.get(bkInfo.getBizName());
                    for(LJBKHostInfo hostInfo : entry.getValue().values())
                    {
                        if(!hostPlatformAppMap.containsKey(hostInfo.getHostInnerIp()))
                        {
                            logger.error(String.format("lj paas report bizId=%d and bizName=%s and hostIp=%s deploy app but cmdb database has not such record",
                                    bkInfo.getBizId(), bkInfo.getBizName(), hostInfo.getHostInnerIp()));
                            return false;
                        }
                        if(!hostModuleMap.containsKey(hostInfo.getHostInnerIp()))
                        {
                            logger.error(String.format("database report hostIp=%s deploy apps but lj paas report bizId=%d and bizName=%s and hostIp=%s has not app",
                                    hostInfo.getHostInnerIp(), bkInfo.getBizId(), bkInfo.getBizName(), hostInfo.getHostInnerIp()));
                            return false;
                        }
                        Map<String, PlatformAppDeployDetailVo> detailMap = hostPlatformAppMap.get(hostInfo.getHostInnerIp());
                        Map<String, LJModuleInfo> ljModuleMap = hostModuleMap.get(hostInfo.getId());
                        if(detailMap.size() != ljModuleMap.size())
                        {
                            logger.error(String.format("data error, lj paas report bizId=%s and bizName=%s and hostIp=%s deploy %d apps but database report deploy %d apps",
                                    bkInfo.getBizId(), bkInfo.getBizName(), hostInfo.getHostInnerIp(), ljModuleMap.size(), detailMap.size()));
                            return false;
                        }
                        for(String appName : detailMap.keySet())
                        {
                            if(!ljModuleMap.containsKey(appName))
                            {
                                logger.error(String.format("data error, lj paas report bizId=%s and bizName=%s and hostIp=%s deploy %s app but database has not such record",
                                        bkInfo.getBizId(), bkInfo.getBizName(), hostInfo.getHostInnerIp(), appName));
                                return false;
                            }
                            ljModuleMap.get(appName).setVersion(detailMap.get(appName).getVersion());
                            ljModuleMap.get(appName).setCcodVersion(detailMap.get(appName).getVersion());
                        }
                    }
                    usedList.addAll(entry.getValue().values());
                }
            }
        }
        return true;
    }
}
