package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.ServerUserPo;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: ServerUserMapper
 * @Author: lanhb
 * @Description: 定义ServerUser的dao接口
 * @Date: 2019/11/22 11:36
 * @Version: 1.0
 */
@Component
public interface ServerUserMapper {
    /**
     * 向数据库添加一条新的服务器用户记录
     * @param serverUser
     * @throws DataAccessException
     */
    void insert(ServerUserPo serverUser) throws DataAccessException;

    /**
     * 根据主键查询服务器用户
     * @param userId 用户id
     * @return 查询结果
     * @throws DataAccessException
     */
    ServerUserPo selectByPrimaryKey(int userId) throws DataAccessException;

    /**
     * 查询指定服务器的用户
     * @param serverId 服务器id
     * @return 查询结果
     * @throws DataAccessException
     */
    List<ServerUserPo> select(@Param("serverId")Integer serverId) throws DataAccessException;

    /**
     * 更新已有的服务器用户信息
     * @param serverUser 需要更新的服务器用户
     * @throws DataAccessException
     */
    void update(ServerUserPo serverUser) throws DataAccessException;

    /**
     * 删除指定条件的服务器用户,如果某个条件为空则忽略该参数
     * @param userId 用户id
     * @param serverId 服务器id
     * @throws DataAccessException
     */
    void delete(@Param("userId")Integer userId, @Param("serverId") Integer serverId) throws  DataAccessException;
}
