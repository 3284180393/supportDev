package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.AppPo;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;

/**
 * @ClassName: AppMapper
 * @Author: lanhb
 * @Description: 用来定义AppPo类的dao操作接口
 * @Date: 2019/11/21 9:24
 * @Version: 1.0
 */
public interface AppMapper {
    /**
     * 添加一条新的app记录
     * @param app 需要添加的app记录
     * @throws DataAccessException
     */
    void insert(AppPo app) throws DataAccessException;

    /**
     * 根据主键查询app信息
     * @param appId
     * @return 满足条件的app记录
     * @throws DataAccessException
     */
    AppPo selectByPrimaryKey(int appId) throws DataAccessException;

    /**
     * 根据指定条件查询app记录，如果某个参数为空，则查询时忽略该参数，如果所有参数为空则查询所有记录
     * @param appType 应用类型
     * @param appName 应用名
     * @param appAlias 应用别名
     * @param version 应用版本号
     * @return 满足条件的记录
     * @throws DataAccessException
     */
    List<AppPo> select(@Param("appType")String appType, @Param("appName")String appName, @Param("appAlias")String appAlias, @Param("version")String version) throws DataAccessException;

    /**
     * 修改已有的app记录
     * @param app 需要修改的app记录
     * @throws DataAccessException
     */
    void update(AppPo app) throws DataAccessException;

    /**
     * 删除指定的app记录
     * @param appId 需要删除的记录主键
     * @throws DataAccessException
     */
    void delete(int appId) throws DataAccessException;
}
