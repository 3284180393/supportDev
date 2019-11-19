package com.channelsoft.ccod.support.cmdb.service;

import com.channelsoft.ccod.support.cmdb.vo.CcodPlatformAppVo;
import com.channelsoft.ccod.support.cmdb.vo.PlatformAppModuleVo;

import java.util.List;

/**
 * @ClassName: IPlatformAppCollectService
 * @Author: lanhb
 * @Description: 用来定义平台模块收集服务的接口
 * @Date: 2019/11/15 11:20
 * @Version: 1.0
 */
public interface IPlatformAppCollectService {

    List<PlatformAppModuleVo> collectPlatformApp(String platformId) throws Exception;
}
