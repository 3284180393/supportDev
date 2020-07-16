package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.AppType;
import com.channelsoft.ccod.support.cmdb.constant.K8sOperation;
import com.channelsoft.ccod.support.cmdb.po.K8sOperationPo;
import io.kubernetes.client.openapi.models.ExtensionsV1beta1Ingress;
import io.kubernetes.client.openapi.models.V1Deployment;
import io.kubernetes.client.openapi.models.V1Service;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @ClassName: K8sAppAssembleInfo
 * @Author: lanhb
 * @Description: 用来定义k8s应用集合相关内容
 * @Date: 2020/7/10 17:54
 * @Version: 1.0
 */
public class K8sAppAssembleInfo {

    @NotNull(message = "assembleTag can not be null")
    private String assembleTag;

    @NotNull(message = "assembleName can not be null")
    private String assembleName;

    private boolean isThreePartApp;

    @NotNull(message = "appOptList can not be null")
    private List<AppUpdateOperationInfo> appOptList;

    @NotNull(message = "k8sOperationList can not be null")
    private List<K8sOperationPo> k8sOperationList;

    public String getAssembleTag() {
        return assembleTag;
    }

    public void setAssembleTag(String assembleTag) {
        this.assembleTag = assembleTag;
    }

    public String getAssembleName() {
        return assembleName;
    }

    public void setAssembleName(String assembleName) {
        this.assembleName = assembleName;
    }

    public boolean isThreePartApp() {
        return isThreePartApp;
    }

    public void setThreePartApp(boolean threePartApp) {
        isThreePartApp = threePartApp;
    }

    public List<AppUpdateOperationInfo> getAppOptList() {
        return appOptList;
    }

    public void setAppOptList(List<AppUpdateOperationInfo> appOptList) {
        this.appOptList = appOptList;
    }

    public List<K8sOperationPo> getK8sOperationList() {
        return k8sOperationList;
    }

    public void setK8sOperationList(List<K8sOperationPo> k8sOperationList) {
        this.k8sOperationList = k8sOperationList;
    }
}
