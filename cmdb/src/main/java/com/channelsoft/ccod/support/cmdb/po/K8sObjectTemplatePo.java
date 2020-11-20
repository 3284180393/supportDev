package com.channelsoft.ccod.support.cmdb.po;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: K8sObjectTemplatePo
 * @Author: lanhb
 * @Description: 用来定义k8s资源对象模板的类
 * @Date: 2020/8/6 10:17
 * @Version: 1.0
 */
public class K8sObjectTemplatePo {

    private int templateId;

    private Map<String, String> labels;

    private String deployJson;

    private String serviceJson;

    private String ingressJson;

    private String endpointsJson;

    private String podJson;

    private String namespaceJson;

    private String jobJson;

    private String secretJson;

    private String persistentVolumeJson;

    private String persistentVolumeClaimJson;

    public int getTemplateId() {
        return templateId;
    }

    public K8sObjectTemplatePo(){}

    public K8sObjectTemplatePo(Map<String, String> labels)
    {
        this.labels = labels;
    }

    public void setTemplateId(int templateId) {
        this.templateId = templateId;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public String getDeployJson() {
        return deployJson;
    }

    public void setDeployJson(String deployJson) {
        this.deployJson = deployJson;
    }

    public String getServiceJson() {
        return serviceJson;
    }

    public void setServiceJson(String serviceJson) {
        this.serviceJson = serviceJson;
    }

    public String getIngressJson() {
        return ingressJson;
    }

    public void setIngressJson(String ingressJson) {
        this.ingressJson = ingressJson;
    }

    public String getEndpointsJson() {
        return endpointsJson;
    }

    public void setEndpointsJson(String endpointsJson) {
        this.endpointsJson = endpointsJson;
    }

    public String getNamespaceJson() {
        return namespaceJson;
    }

    public void setNamespaceJson(String namespaceJson) {
        this.namespaceJson = namespaceJson;
    }

    public String getSecretJson() {
        return secretJson;
    }

    public void setSecretJson(String secretJson) {
        this.secretJson = secretJson;
    }

    public String getPodJson() {
        return podJson;
    }

    public void setPodJson(String podJson) {
        this.podJson = podJson;
    }

    public String getPersistentVolumeJson() {
        return persistentVolumeJson;
    }

    public void setPersistentVolumeJson(String persistentVolumeJson) {
        this.persistentVolumeJson = persistentVolumeJson;
    }

    public String getPersistentVolumeClaimJson() {
        return persistentVolumeClaimJson;
    }

    public void setPersistentVolumeClaimJson(String persistentVolumeClaimJson) {
        this.persistentVolumeClaimJson = persistentVolumeClaimJson;
    }

    public String getJobJson() {
        return jobJson;
    }

    public void setJobJson(String jobJson) {
        this.jobJson = jobJson;
    }

    @Autowired
    public K8sObjectTemplatePo clone()
    {
        K8sObjectTemplatePo po = new K8sObjectTemplatePo();
        po.setLabels(new HashMap<>());
        labels.forEach((k,v)->po.getLabels().put(k, v));
        po.templateId = templateId;
        po.deployJson = deployJson;
        po.serviceJson = serviceJson;
        po.ingressJson = ingressJson;
        po.endpointsJson = endpointsJson;
        po.podJson = podJson;
        po.namespaceJson = namespaceJson;
        po.jobJson = jobJson;
        po.secretJson = secretJson;
        po.persistentVolumeJson = persistentVolumeJson;
        po.persistentVolumeClaimJson = persistentVolumeClaimJson;
        return po;
    }

    @Override
    public String toString()
    {
        if(labels == null || labels.size() == 0)
            return "";
        StringBuffer sb = new StringBuffer();
        for(String key : labels.keySet()){
            sb.append(String.format("%s=%s;", key, labels.get(key)));
        }
        return sb.toString().replaceAll(";$", "");
    }
}
