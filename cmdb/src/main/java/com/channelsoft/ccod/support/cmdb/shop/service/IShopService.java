package com.channelsoft.ccod.support.cmdb.shop.service;

import com.channelsoft.ccod.support.cmdb.vo.PlatformAppDeployDetailVo;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;
import java.util.List;

/**
 * @ClassName: IShopService
 * @Author: lanhb
 * @Description: 定义应用商店相关服务
 * @Date: 2020/9/25 10:07
 * @Version: 1.0
 */
public interface IShopService {

    void onlineCCODDomainApp(List<PlatformAppDeployDetailVo> appList) throws JsonProcessingException, IOException;

}
