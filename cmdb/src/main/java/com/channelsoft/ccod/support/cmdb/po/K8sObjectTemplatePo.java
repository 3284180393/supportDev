package com.channelsoft.ccod.support.cmdb.po;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.K8sKind;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.kubernetes.client.openapi.models.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: K8sObjectTemplatePo
 * @Author: lanhb
 * @Description: 用来定义k8s资源对象模板的类
 * @Date: 2020/8/6 10:17
 * @Version: 1.0
 */
public class K8sObjectTemplatePo {

    private final static Gson gson = new Gson();

    private Map<String, String> labels;

    private List<V1Deployment> deployments;

    private List<V1StatefulSet> statefulSets;

    private List<V1Service> services;

    private ExtensionsV1beta1Ingress ingress;

    private List<V1Endpoints> endpoints;

    private List<V1Pod> pods;

    private List<V1ConfigMap> configMaps;

    private V1Namespace namespaces;

    private List<V1Job> jobs;

    private List<V1Secret> secrets;

    private List<V1PersistentVolume> pvList;

    private List<V1PersistentVolumeClaim> pvcList;

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public List<V1Deployment> getDeployments() {
        return deployments == null ? null : gson.fromJson(gson.toJson(deployments), new TypeToken<List<V1Deployment>>() {}.getType());
    }

    public void setDeployments(List<V1Deployment> deployments) {
        this.deployments = deployments;
    }

    public List<V1StatefulSet> getStatefulSets() {
        return statefulSets == null ? null : gson.fromJson(gson.toJson(statefulSets), new TypeToken<List<V1StatefulSet>>() {}.getType());
    }

    public void setStatefulSets(List<V1StatefulSet> statefulSets) {
        this.statefulSets = statefulSets;
    }

    public List<V1Service> getServices() {
        return services == null ? null : gson.fromJson(gson.toJson(services), new TypeToken<List<V1Service>>() {}.getType());
    }

    public void setServices(List<V1Service> services) {
        this.services = services;
    }

    public ExtensionsV1beta1Ingress getIngress() {
        return ingress == null ? null : gson.fromJson(gson.toJson(ingress), ExtensionsV1beta1Ingress.class);
    }

    public void setIngress(ExtensionsV1beta1Ingress ingress) {
        this.ingress = ingress;
    }

    public List<V1Endpoints> getEndpoints() {
        return endpoints == null ? null : gson.fromJson(gson.toJson(endpoints), new TypeToken<List<V1Endpoints>>() {}.getType());
    }

    public void setEndpoints(List<V1Endpoints> endpoints) {
        this.endpoints = endpoints;
    }

    public List<V1Pod> getPods() {
        return pods == null ? null : gson.fromJson(gson.toJson(pods), new TypeToken<List<V1Pod>>() {}.getType());
    }

    public void setPods(List<V1Pod> pods) {
        this.pods = pods;
    }

    public V1Namespace getNamespaces() {
        return namespaces == null ? null : gson.fromJson(gson.toJson(namespaces), V1Namespace.class);
    }

    public void setNamespaces(V1Namespace namespaces) {
        this.namespaces = namespaces;
    }

    public List<V1Job> getJobs() {
        return jobs == null ? null : gson.fromJson(gson.toJson(jobs), new TypeToken<List<V1Job>>() {}.getType());
    }

    public void setJobs(List<V1Job> jobs) {
        this.jobs = jobs;
    }

    public List<V1Secret> getSecrets() {
        return secrets == null ? null : gson.fromJson(gson.toJson(secrets), new TypeToken<List<V1Secret>>() {}.getType());
    }

    public void setSecrets(List<V1Secret> secrets) {
        this.secrets = secrets;
    }

    public List<V1PersistentVolume> getPvList() {
        return pvList == null ? null : gson.fromJson(gson.toJson(pvList), new TypeToken<List<V1PersistentVolume>>() {}.getType());
    }

    public void setPvList(List<V1PersistentVolume> pvList) {
        this.pvList = pvList;
    }

    public List<V1PersistentVolumeClaim> getPvcList() {
        return pvcList == null ? null : gson.fromJson(gson.toJson(pvcList), new TypeToken<List<V1PersistentVolumeClaim>>() {}.getType());
    }

    public void setPvcList(List<V1PersistentVolumeClaim> pvcList) {
        this.pvcList = pvcList;
    }

    public List<V1ConfigMap> getConfigMaps() {
        return configMaps;
    }

    public void setConfigMaps(List<V1ConfigMap> configMaps) {
        this.configMaps = configMaps;
    }

    public Object getK8sObject(K8sKind kind){
        switch (kind){
            case CONFIGMAP:
                return configMaps;
            case PVC:
                return pvcList;
            case PV:
                return pvList;
            case NAMESPACE:
                return namespaces;
            case SECRET:
                return secrets;
            case JOB:
                return jobs;
            case POD:
                return pods;
            case DEPLOYMENT:
                return deployments;
            case SERVICE:
                return services;
            case INGRESS:
                return ingress;
            case ENDPOINTS:
                return endpoints;
            case STATEFULSET:
                return statefulSets;
            default:
                return null;
        }
    }

    @Autowired
    public K8sObjectTemplatePo clone()
    {
        K8sObjectTemplatePo po = new K8sObjectTemplatePo();
        po.setLabels(new HashMap<>());
        labels.forEach((k,v)->po.getLabels().put(k, v));
        return po;
    }

    @Override
    public String toString()
    {
        if(labels == null || labels.size() == 0)
            return "";
        StringBuffer sb = new StringBuffer();
        labels.forEach((k,v)->sb.append(String.format("%s=%s;", k, v)));
        return sb.toString().replaceAll(";$", "");
    }
}
