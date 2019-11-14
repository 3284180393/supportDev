package com.channelsoft.ccod.support.cmdb.service.impl;

import com.channelsoft.ccod.support.cmdb.po.NexusAssetInfo;
import com.channelsoft.ccod.support.cmdb.po.NexusComponentPo;
import com.channelsoft.ccod.support.cmdb.service.INexusService;
import org.springframework.stereotype.Service;

/**
 * @ClassName: NexusServiceImpl
 * @Author: lanhb
 * @Description: INexusService接口实现类
 * @Date: 2019/11/14 13:50
 * @Version: 1.0
 */
@Service
public class NexusServiceImpl implements INexusService {
    @Override
    public boolean uploadRawFile(String sourceFilePath, String repository, String group, String fileName) throws Exception {
        return false;
    }

    @Override
    public NexusComponentPo queryComponentById(String repository, String componentId) throws Exception {
        return null;
    }

    @Override
    public NexusAssetInfo queryAssetById(String repository, String assetId) throws Exception {
        return null;
    }

    @Override
    public NexusComponentPo[] queryComponentFromRepository(String repository) throws Exception {
        return new NexusComponentPo[0];
    }
}
