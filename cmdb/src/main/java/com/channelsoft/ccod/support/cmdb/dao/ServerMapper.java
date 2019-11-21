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

    List<ServerPo> select(@Param("platformId")String platformId, @Param("domainId")String domainId, @Param("status")Integer status) throws DataAccessException;

    void update(ServerPo server) throws DataAccessException;

    void delete(@Param("serverId")Integer serverId, @Param("platformId")String platformId, @Param("domainId")String domainId) throws DataAccessException;
}
