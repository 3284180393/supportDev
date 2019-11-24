package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.PlatformPo;
import javafx.application.Platform;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: PlatformMapper
 * @Author: lanhb
 * @Description: 用来处理Platform信息的dao接口
 * @Date: 2019/11/21 14:27
 * @Version: 1.0
 */
@Component
public interface PlatformMapper {
    /**
     * 向数据库添加一条新的平台记录
     * @param platform 需要添加的平台记录
     * @throws DataAccessException
     */
    void insert(PlatformPo platform) throws DataAccessException;

    /**
     * 根据平台id查询指定平台信息
     * @param platformId 平台id
     * @return 满足条件的记录
     * @throws DataAccessException
     */
    PlatformPo selectByPrimaryKey(String platformId) throws DataAccessException;

    /**
     * 根据指定条件查询平台信息，如果某个参数为空则忽略该参数
     * @param status 平台状态
     * @return 满足条件的记录
     * @throws DataAccessException
     */
    List<PlatformPo> select(@Param("status")Integer status) throws DataAccessException;

    /**
     * 修改已有的平台记录
     * @param platform 需要修改的平台记录
     * @throws DataAccessException
     */
    void update(PlatformPo platform) throws DataAccessException;

    /**
     * 删除已有的平台记录
     * @param platformId 需要删除的平台id
     * @throws DataAccessException
     */
    void delete(String platformId) throws DataAccessException;
}
