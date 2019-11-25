package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.PlatformAppCfgFilePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: PlatformAppCfgFileMapper
 * @Author: lanhb
 * @Description: 用来定义平台应用配置文件的dao接口
 * @Date: 2019/11/25 17:33
 * @Version: 1.0
 */
@Component
@Mapper
public interface PlatformAppCfgFileMapper {
    /**
     * 向数据库添加一条新的cfg记录
     * @param cfgFile 需要添加的配置文件信息
     * @throws DataAccessException
     */
    void insert(PlatformAppCfgFilePo cfgFile) throws DataAccessException;

    /**
     * 根据主键查询配置文件信息
     * @param cfgFileId 应用配置主键
     * @return 查询结果
     * @throws DataAccessException
     */
    PlatformAppCfgFilePo selectByPrimaryKey(int cfgFileId) throws DataAccessException;

    /**
     * 查询指定条件的应用配置
     * @param platformAppId 平台应用id
     * @return 满足条件记录
     * @throws DataAccessException
     */
    List<PlatformAppCfgFilePo> select(@Param("platformAppId")int platformAppId) throws DataAccessException;

    /**
     * 更新已有的应用配置文件信息
     * @param cfgFile 需要更新的应用配置文件信息
     * @throws DataAccessException
     */
    void update(PlatformAppCfgFilePo cfgFile) throws DataAccessException;

    /**
     * 删除指定条件的应用配置文件记录,如果某个参数为空则忽略该参数
     * @param cfgFileId 应用配置文件id
     * @param platformAppId 平台应用id
     */
    void delete(@Param("cfgFileId")Integer cfgFileId, @Param("platformAppId")Integer platformAppId);
}
