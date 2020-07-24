package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.K8sOperationPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: K8sOperationMapper
 * @Author: lanhb
 * @Description: 用来定义K8sOperationPo类的dao接口
 * @Date: 2020/7/24 17:10
 * @Version: 1.0
 */
@Component
@Mapper
public interface K8sOperationMapper {

    /**
     * 向数据库添加一条新的k8s操作记录
     * @param operationPo
     */
    void insert(K8sOperationPo operationPo);

    /**
     * 查询指定的k8s操作记录
     * @param jobId job id
     * @param platformId 平台id,可以为空
     * @param domainId 域id，可以为空
     * @return 查询结果
     */
    List<K8sOperationPo> select(@Param("jobId")String jobId, @Param("platformId")String platformId, @Param("domainId")String domainId);

    /**
     * 删除指定条件的k8s操作记录
     * @param jobId job id
     * @param platformId 平台id，可以为空
     * @param domainId 域id，可以为空
     */
    void delete(@Param("jobId")String jobId, @Param("platformId")String platformId, @Param("domainId")String domainId);
}
