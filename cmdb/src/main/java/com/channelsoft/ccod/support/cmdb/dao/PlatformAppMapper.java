package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.PlatformAppPo;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;

/**
 * @ClassName: PlatformAppMapper
 * @Author: lanhb
 * @Description: 用来操作PlatformAppPo的dao接口
 * @Date: 2019/11/22 15:30
 * @Version: 1.0
 */
public interface PlatformAppMapper {
    void insert(PlatformAppPo platformApp) throws DataAccessException;

    PlatformAppPo selectByPrimaryKey(int platformAppId) throws DataAccessException;

    List<PlatformAppPo> select(@Param("platformId")String platformId, @Param("domainId")String domainId,
                               @Param("hostIp")String hostIp, @Param("appName")String appName,
                               @Param("appAlias")String appAlias, @Param("version")String version)
            throws DataAccessException;

    void update(PlatformAppPo app) throws DataAccessException;
}
