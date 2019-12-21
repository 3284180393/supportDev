package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.PlatformAppBkModulePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: PlatformAppBkModuleMapper
 * @Author: lanhb
 * @Description: pojo类PlatformAppBkModulePo的dao接口
 * @Date: 2019/12/19 16:47
 * @Version: 1.0
 */
@Component
@Mapper
public interface PlatformAppBkModuleMapper {

    /**
     * 添加一条新的平台平台应用关联蓝鲸paas模块信息
     * @param platformAppBkModulePo
     * @throws DataAccessException
     */
    void insert(PlatformAppBkModulePo platformAppBkModulePo) throws DataAccessException;

    /**
     * 删除一条已有的平台应用关联蓝鲸paas模块信息
     * @param platformAppId 需要删除的平台应用id
     * @param bkBizId 需要删除的平台应用模块对应的biz id
     * @throws DataAccessException
     */
    void delete(@Param("platformAppId")Integer platformAppId, @Param("bkBizId")Integer bkBizId) throws DataAccessException;

    /**
     * 根据主键查询应用和蓝鲸paas模块关系
     * @param appBkModuleId 应用模块关系id
     * @return 查询结果
     * @throws DataAccessException
     */
    PlatformAppBkModulePo selectByPrimaryKey(int appBkModuleId) throws DataAccessException;

    /**
     * 根据指定条件查询应用和蓝鲸paas模块关系,如果某个参数为空则忽略该参数
     * @param platformId 平台id
     * @param domainId 域id
     * @param bkBizId 蓝鲸paas的biz id
     * @param bkSetId 蓝鲸paas的set id
     * @param bkModuleId 蓝鲸paas的module id
     * @param bkHostId 蓝鲸paas的host id
     * @return 查询结果
     * @throws DataAccessException
     */
    List<PlatformAppBkModulePo> select(@Param("platformId")String platformId,
                                       @Param("domainId")String domainId,
                                       @Param("bkBizId")Integer bkBizId,
                                       @Param("bkSetId")Integer bkSetId,
                                       @Param("bkModuleId")Integer bkModuleId,
                                       @Param("bkHostId")Integer bkHostId) throws DataAccessException;
}
