package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.vo.PlatformAppDeployDetailVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: PlatformAppDeployDetailMapper
 * @Author: lanhb
 * @Description: PlatformAppDeployDetailVo的dao接口
 * @Date: 2019/11/25 18:12
 * @Version: 1.0
 */
@Component
@Mapper
public interface PlatformAppDeployDetailMapper {
    List<PlatformAppDeployDetailVo> select(@Param("platformId")String platformId, @Param("domainId")String domainId, @Param("hostIp")String hostIp, @Param("hostname")String hostname, @Param("appType")String appType, @Param("appName")String appName, @Param("appAlias")String appAlias, @Param("version")String version) throws DataAccessException;
}
