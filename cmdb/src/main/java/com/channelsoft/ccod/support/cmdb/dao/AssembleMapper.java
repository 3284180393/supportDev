package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.AssemblePo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @ClassName: AssembleMapper
 * @Author: lanhb
 * @Description: 用来定义AssemblePo的mapper接口
 * @Date: 2020/6/1 19:57
 * @Version: 1.0
 */
public interface AssembleMapper {

    /**
     * 查询指定条件的assemble
     * @param platformId 平台id，为空的时候忽略该条件
     * @param domainId 域id，为空的时候忽略该条件
     * @return 查询结果
     */
    List<AssemblePo> select(@Param("platformId") String  platformId, @Param("domainId") String  domainId);

    /**
     * 查询指定id的assemble
     * @param assembleId 需要查询的assemble的id
     * @return 查询结果
     */
    AssemblePo selectByPrimaryKey(int assembleId);

    /**
     * 向数据库插入一条assemble记录
     * @param assemblePo 需要插入的assemble记录
     */
    void insert(AssemblePo assemblePo);

    /**
     * 修改已有的assemble
     * @param assemblePo
     */
    void update(AssemblePo assemblePo);

    /**
     * 删除指定条件的assemble
     * @param platformId 平台id
     * @param domainId 域id
     * @param assembleId assemble id
     */
    void delete(@Param("platformId") String  platformId, @Param("domainId") String  domainId, @Param("assembleId") Integer assembleId);

}
