package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.UnconfirmedAppModulePo;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: UnconfirmedAppModuleMapper
 * @Author: lanhb
 * @Description: UnconfirmedAppModulePo类的dao接口
 * @Date: 2020/4/28 15:52
 * @Version: 1.0
 */
@Component
@Mapper
public interface UnconfirmedAppModuleMapper {

    /**
     * 向数据库添加一条未确认模块信息
     * @param modulePo 需要被添加的未确认模块
     */
    void insert(UnconfirmedAppModulePo modulePo);

    /**
     * 删除平台下的未被确认模块信息
     * @param platformId 需要删除的平台id
     */
    void delete(String platformId);

    /**
     * 查询某个平台下的所有未被确认模块信息
     * @param platformId 需要查询的平台id
     * @return 查询结果
     */
    List<UnconfirmedAppModulePo> select(String platformId);
}
