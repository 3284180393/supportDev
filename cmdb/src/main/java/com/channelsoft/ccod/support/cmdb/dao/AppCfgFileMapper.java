package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.AppCfgFilePo;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;

import java.util.List;

/**
 * @ClassName: AppCfgFileMapper
 * @Author: lanhb
 * @Description: AppCfgFilePo的dao接口
 * @Date: 2019/11/21 10:48
 * @Version: 1.0
 */
public interface AppCfgFileMapper {
    /**
     * 向数据库添加一条新的cfg_file记录
     * @param cfgFile 需要添加的配置文件信息
     * @throws DataAccessException
     */
    void insert(AppCfgFilePo cfgFile) throws DataAccessException;

    /**
     * 根据主键查询配置文件信息
     * @param cfgFileId 应用配置主键
     * @return 查询结果
     * @throws DataAccessException
     */
    AppCfgFilePo selectByPrimaryKey(int cfgFileId) throws DataAccessException;

    /**
     * 查询指定条件的应用配置
     * @param appId 应用id
     * @return 满足条件记录
     * @throws DataAccessException
     */
    List<AppCfgFilePo> select(@Param("appId")int appId) throws DataAccessException;

    /**
     * 更新已有的应用配置文件信息
     * @param cfgFile 需要更新的应用配置文件信息
     * @throws DataAccessException
     */
    void update(AppCfgFilePo cfgFile) throws DataAccessException;

    /**
     * 删除指定条件的应用配置文件记录,如果某个参数为空则忽略该参数
     * @param cfgFileId 应用配置文件id
     * @param appId 应用id
     */
    void delete(@Param("cfgFileId")Integer cfgFileId, @Param("appId")Integer appId);
}
