package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.AppDebugDetailPo;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: AppDebugDetailMapper
 * @Author: lanhb
 * @Description: AppDebugDetailPo类的dao接口类
 * @Date: 2020/10/29 14:46
 * @Version: 1.0
 */
@Component
public interface AppDebugDetailMapper {

    void insert(AppDebugDetailPo detailPo);

    void update(AppDebugDetailPo detailPo);

    List<AppDebugDetailPo> select(
            @Param("platformId")String platformId,
            @Param("domainId")String domainId,
            @Param("appName") String appName,
            @Param("alias")String alias);

    void delete(
            @Param("platformId")String platformId,
            @Param("domainId")String domainId,
            @Param("appName") String appName,
            @Param("alias")String alias);
}
