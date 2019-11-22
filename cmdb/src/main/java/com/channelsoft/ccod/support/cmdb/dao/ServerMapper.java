package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.ServerPo;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;

/**
 * @ClassName: ServerMapper
 * @Author: lanhb
 * @Description: 用来操作ServerPo的dao接口
 * @Date: 2019/11/21 16:12
 * @Version: 1.0
 */
public interface ServerMapper {
    /**
     * 向数据库添加一条新的服务器记录
     * @param server 需要添加的服务器记录
     * @throws DataAccessException
     */
    void insert(ServerPo server) throws DataAccessException;

    /**
     * 根据主键查询服务器
     * @param serverId 服务器id
     * @return 查询结果
     * @throws DataAccessException
     */
    ServerPo selectByPrimaryKey(int serverId) throws DataAccessException;

    /**
     * 查询指定条件的服务器信息,如果某项参数为空则忽略该参数
     * @param platformId 平台id
     * @param domainId 域id
     * @param status 服务器状态
     * @return 满足条件的记录
     * @throws DataAccessException
     */
    List<ServerPo> select(@Param("platformId")String platformId, @Param("domainId")String domainId, @Param("status")Integer status) throws DataAccessException;

    /**
     * 更新已有的服务器信息
     * @param server 服务器信息
     * @throws DataAccessException
     */
    void update(ServerPo server) throws DataAccessException;

    /**
     * 删除已有的服务信息
     * @param serverId 服务器id
     * @param platformId 平台id
     * @param domainId 域id
     * @throws DataAccessException
     */
    void delete(@Param("serverId")Integer serverId, @Param("platformId")String platformId, @Param("domainId")String domainId) throws DataAccessException;
}
