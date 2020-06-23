package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.DomainPublicConfigPo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: DomainPublicConfigMapper
 * @Author: lanhb
 * @Description: 用来定义域公共配置dao接口
 * @Date: 2020/6/23 10:02
 * @Version: 1.0
 */
@Component
public interface DomainPublicConfigMapper {

    /**
     * 查询指定条件的域公共配置
     * @param platformId 平台id
     * @param domainId 域id
     * @return 查询结果
     */
    List<DomainPublicConfigPo> select(@Param("platformId")String platformId, @Param("domainId")String domainId);

    /**
     * 向数据库添加新的域公共配置
     * @param domainPublicConfigPo
     */
    void insert(DomainPublicConfigPo domainPublicConfigPo);

    /**
     * 删除已有的域公共配置
     * @param platformId
     * @param domainId
     */
    void delete(@Param("platformId")String platformId, @Param("domainId")String domainId);

}
