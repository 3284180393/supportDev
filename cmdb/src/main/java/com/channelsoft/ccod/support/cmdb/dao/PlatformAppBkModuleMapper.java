package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.PlatformAppBkModulePo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

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
}
