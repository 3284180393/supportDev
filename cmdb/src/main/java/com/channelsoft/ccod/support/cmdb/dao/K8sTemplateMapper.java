package com.channelsoft.ccod.support.cmdb.dao;

import com.channelsoft.ccod.support.cmdb.po.K8sTemplatePo;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @ClassName: K8sTemplateMapper
 * @Author: lanhb
 * @Description: K8sTemplatePo类的dao接口
 * @Date: 2020/11/30 18:14
 * @Version: 1.0
 */
@Component
public interface K8sTemplateMapper {
    /**
     * 向数据库添加一条新的模板记录
     * @param templatePo 需要添加的新模板
     */
    void insert(K8sTemplatePo templatePo);

    /**
     * 修改数据库已有的模板记录
     * @param templatePo 需要修改的模板记录
     */
    void update(K8sTemplatePo templatePo);

    /**
     * 删除已有的模板记录
     * @param id 需要删除的模板id
     */
    void delete(int id);

    /**
     * 查询数据库所有的模板记录
     * @return 查询到的所有模板记录
     */
    List<K8sTemplatePo> select();
}
