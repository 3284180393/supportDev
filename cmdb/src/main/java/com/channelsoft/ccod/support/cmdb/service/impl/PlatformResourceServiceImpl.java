package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.dao.PlatformResourceMapper;
import com.channelsoft.ccod.support.cmdb.service.IPlatformResourceService;
import com.channelsoft.ccod.support.cmdb.vo.PlatformResourceVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @ClassName: PlatformResourceServiceImpl
 * @Author: lanhb
 * @Description: 平台资源服务接口的实现类
 * @Date: 2019/11/27 15:50
 * @Version: 1.0
 */
@Service
public class PlatformResourceServiceImpl implements IPlatformResourceService {

    private final static Logger logger = LoggerFactory.getLogger(PlatformResourceServiceImpl.class);

    @Autowired
    PlatformResourceMapper platformResourceMapper;

    @Override
    public PlatformResourceVo[] queryPlatformResources() throws Exception {
        logger.debug("begin to query resource of all platforms");
        List<PlatformResourceVo> list = this.platformResourceMapper.select();
        logger.info(String.format("query %d platforms resource", list.size()));
        return list.toArray(new PlatformResourceVo[0]);
    }

    @Override
    public PlatformResourceVo queryPlatformResource(String platformId, String domainId, String hostIp) throws Exception {
        logger.debug(String.format("begin to query platform resource : platformId=%s, domainId=%s, hostIp=%s",
                platformId, domainId, hostIp));
        PlatformResourceVo resourceVo = this.platformResourceMapper.selectByPlatform(platformId, domainId, hostIp);
        if(resourceVo == null)
        {
            logger.warn(String.format("not find platform resource with platformId=%s and domainId=%s and hostIp=%s",
                    platformId, domainId, hostIp));
        }
        else
        {
            logger.info(String.format("platformId=%s and domainId=%s and hostIp=%s platform resource been found",
                    platformId, domainId, hostIp));
        }
        return resourceVo;
    }
}
