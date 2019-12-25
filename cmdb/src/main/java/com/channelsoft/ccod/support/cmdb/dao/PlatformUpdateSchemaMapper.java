package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.PlatformUpdateSchemaPo;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: PlatformUpdateSchemaMapper
 * @Author: lanhb
 * @Description: 台升级计划PlatformUpdateSchemaPo类的dao接口
 * @Date: 2019/12/25 15:13
 * @Version: 1.0
 */
@Component
public interface PlatformUpdateSchemaMapper {
    /**
     * 向数据库添加一条新的平台升级计划记录
     * @param schemaPo 需要添加的平台升级计划
     * @throws DataAccessException
     */
    void insert(PlatformUpdateSchemaPo schemaPo) throws DataAccessException;

    /**
     * 查询当前所有的平台升级计划
     * @return 所有平台升级计划
     * @throws DataAccessException
     */
    List<PlatformUpdateSchemaPo> select() throws DataAccessException;

    /**
     * 删除指定平台的升级计划
     * @param platformId 被删除升级计划的平台
     * @throws DataAccessException
     */
    void delete(String platformId) throws DataAccessException;
}
