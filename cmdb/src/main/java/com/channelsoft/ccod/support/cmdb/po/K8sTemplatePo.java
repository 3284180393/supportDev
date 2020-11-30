package com.channelsoft.ccod.support.cmdb.po;

import java.util.Map;
/**
 * @ClassName: K8sTemplatePo
 * @Author: lanhb
 * @Description: 用来定义创建ccod的k8s平台的对象模板类
 * @Date: 2020/11/30 18:10
 * @Version: 1.0
 */
public class K8sTemplatePo {

    private int id; //id数据库唯一主键

    private Map<String, String> labels; //该模板的标签

    private K8sObjectTemplatePo objectTemplate;  //指定标签对应的模板

    public K8sTemplatePo(){
    }

    public K8sTemplatePo(K8sObjectTemplatePo template){
        labels = template.getLabels();
        objectTemplate = template;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public K8sObjectTemplatePo getObjectTemplate() {
        return objectTemplate;
    }

    public void setObjectTemplate(K8sObjectTemplatePo objectTemplate) {
        this.objectTemplate = objectTemplate;
    }
}
