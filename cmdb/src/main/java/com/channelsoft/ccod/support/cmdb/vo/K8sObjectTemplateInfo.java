package com.channelsoft.ccod.support.cmdb.vo;

import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;

import java.util.Map;

/**
 * @ClassName: K8sObjectTemplateInfo
 * @Author: lanhb
 * @Description: 用来定义k8s对象模板信息
 * @Date: 2020/8/6 10:09
 * @Version: 1.0
 */
public class K8sObjectTemplateInfo {

    private Map<String, String> labels;

    private String deployJson;

    private String serviceJson;

    private String ingressJson;

    private String namespaceJson;

    private String secretJson;

    private String persistentVolumeJson;

    private String persistentVolumeClaimJson;

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
}
