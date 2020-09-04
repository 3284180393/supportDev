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
    /**
     * 查询指定条件的平台应用部署详情,如果某个条件为空则忽略该参数
     * @param platformId 平台id
     * @param domainId 域id
     * @param hostIp 主机ip
     * @return 查询结果
     * @throws DataAccessException
     */
    List<PlatformAppDeployDetailVo> selectPlatformApps(
            @Param("platformId")String platformId,
            @Param("domainId")String domainId,
            @Param("hostIp")String hostIp) throws DataAccessException;

    PlatformAppDeployDetailVo selectPlatformApp(
            @Param("platformId")String platformId,
            @Param("domainId")String domainId,
            @Param("alias")String alias);

    /**
     * 查询应用在平台的部署详情
     * @param appName 应用名
     * @param platformId 平台名
     * @param domainId 域名
     * @param hostIp 服务器ip
     * @return 查询结果
     * @throws DataAccessException
     */
    List<PlatformAppDeployDetailVo> selectAppDeployDetails(
            @Param("appName")String appName,
            @Param("platformId")String platformId,
            @Param("domainId")String domainId,
            @Param("hostIp")String hostIp) throws DataAccessException;
}
