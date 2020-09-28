package com.channelsoft.ccod.support.cmdb.shop.service.impl;

import com.channelsoft.ccod.support.cmdb.service.IK8sTemplateService;
import com.channelsoft.ccod.support.cmdb.service.IPlatformManagerService;
import com.channelsoft.ccod.support.cmdb.shop.service.IShopService;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppDeployDetailVo;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: ShopServiceImpl
 * @Author: lanhb
 * @Description: IShopService接口实现类
 * @Date: 2020/9/25 10:26
 * @Version: 1.0
 */
public class ShopServiceImpl implements IShopService {

    @Autowired
    IK8sTemplateService k8sTemplateService;

    @Autowired
    IPlatformManagerService platformManagerService;

    @Override
    public void onlineCCODDomainApp(List<PlatformAppDeployDetailVo> appList) throws JsonProcessingException, IOException {
        List<String> ccodVersions = appList.stream().map(a->a.getCcodVersion()).distinct().collect(Collectors.toList());
        Assert.isTrue(ccodVersions.size() == 1, String.format("online app belong to different ccod version : %s", String.join(",", ccodVersions)));

        StringBuffer sb = new StringBuffer();
        for(PlatformAppDeployDetailVo detailVo : appList)
        {
//            k8sTemplateService.generateAddPlatformAppSteps()
        }
    }
}
