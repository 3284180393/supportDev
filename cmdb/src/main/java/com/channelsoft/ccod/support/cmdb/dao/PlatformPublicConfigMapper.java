package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.PlatformPublicConfigPo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: PlatformPublicConfigMapper
 * @Author: lanhb
 * @Description: 用来定义PlatformPublicConfigPo的dao接口
 * @Date: 2020/6/22 20:37
 * @Version: 1.0
 */
@Component
public interface PlatformPublicConfigMapper {

    /**
     * 查询指定平台的公共配置
     * @param platformId 指定平台
     * @return 查询结果
     */
    List<PlatformPublicConfigPo> select(String platformId);

    /**
     * 向平台添加一条新的平台公共配置
     * @param platformPublicConfigPo 需要添加的平台公共配置
     */
    void insert(PlatformPublicConfigPo platformPublicConfigPo);

    /**
     * 删除平台所有的平台公共配置
     * @param platformId 平台id
     */
    void delete(String platformId);
    
}
