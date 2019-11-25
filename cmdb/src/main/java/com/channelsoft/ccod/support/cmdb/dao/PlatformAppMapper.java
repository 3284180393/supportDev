package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.PlatformAppPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: PlatformAppMapper
 * @Author: lanhb
 * @Description: 用来操作PlatformAppPo的dao接口
 * @Date: 2019/11/22 15:30
 * @Version: 1.0
 */
@Component
@Mapper
public interface PlatformAppMapper {
    /**
     * 添加一条平台模块记录
     * @param platformApp 需要添加的平台模块记录
     * @throws DataAccessException
     */
    void insert(PlatformAppPo platformApp) throws DataAccessException;

    /**
     * 根据主键查询指定平台模块记录
     * @param platformAppId 平台模块id
     * @return 查询结果
     * @throws DataAccessException
     */
    PlatformAppPo selectByPrimaryKey(int platformAppId) throws DataAccessException;

    /**
     * 根据指定条件查询平台应用部署记录,如果某个参数为空,查询时忽略该参数
     * @param platformId 平台id
     * @param domainId 域id
     * @param hostIp 主机ip
     * @param hostname 主机名
     * @param appName 应用名
     * @param appAlias 应用别名
     * @param version 应用版本号
     * @return
     * @throws DataAccessException
     */
    List<PlatformAppPo> select(@Param("platformId")String platformId, @Param("domainId")String domainId,
                               @Param("hostIp")String hostIp, @Param("hostname")String hostname, @Param("appName")String appName,
                               @Param("appAlias")String appAlias, @Param("version")String version)
            throws DataAccessException;

    void update(PlatformAppPo app) throws DataAccessException;

    /**
     * 删除指定条件的平台应用配置,如果某个参数为空则忽略该参数
     * @param platformAppId 平台应用id
     * @param platformId 平台id
     * @param domainId 域id
     * @param serverId 服务器id
     * @throws DataAccessException
     */
    void delete(@Param("platformAppId")Integer platformAppId, @Param("platformId")String platformId, @Param("domainId")String domainId, @Param("serverId")Integer serverId) throws DataAccessException;
}
