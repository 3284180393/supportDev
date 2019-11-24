package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.DomainPo;
import org.apache.ibatis.annotations.Param;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: DomainMapper
 * @Author: lanhb
 * @Description: 用来定义Domain的dao接口
 * @Date: 2019/11/21 15:18
 * @Version: 1.0
 */
@Component
public interface DomainMapper {
    /**
     * 添加一条新的域记录
     * @param domain 需要添加的域信息
     * @throws DataAccessException
     */
    void insert(DomainPo domain) throws DataAccessException;

    /**
     * 根据域id查询指定域信息
     * @param domainId 指定的域id
     * @return 查询到的记录
     * @throws DataAccessException
     */
    DomainPo selectByPrimaryKey(String domainId) throws DataAccessException;

    /**
     * 根据指定条件查询域记录,如果某个参数为空则忽略该参数
     * @param platformId 平台id
     * @param status 域状态
     * @return 满足条件的记录
     * @throws DataAccessException
     */
    List<DomainPo> select(@Param("platformId")String platformId, @Param("status")Integer status) throws DataAccessException;

    /**
     * 更新已有的域信息
     * @param domain 需要更新的域信息
     * @throws DataAccessException
     */
    void update(DomainPo domain) throws DataAccessException;

    /**
     * 删除指定条件的域记录,如果某个参数为空则忽略该参数
     * @param domainId 域id
     * @param platformId 平台id
     * @throws DataAccessException
     */
    void delete(@Param("domainId")String domainId, @Param("platformId")String platformId) throws DataAccessException;
}
