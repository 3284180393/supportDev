package com.channelsoft.ccod.support.cmdb.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.support.spring.PropertyPreFilters;
import io.kubernetes.client.custom.IntOrString;
import io.kubernetes.client.openapi.models.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: K8sUtils
 * @Author: lanhb
 * @Description: 封装一些和k8s相关的工具函数
 * @Date: 2020/6/11 12:20
 * @Version: 1.0
 */
public class K8sUtils {

    /**
     * 将一个jsonObject转换成V1Service对象
     * @param jsonObject
     * @return
     */
    public static V1Service transferJsonObjectToV1Service(JSONObject jsonObject)
    {
        String[] excludeProperties = {"targetPort"};
        PropertyPreFilters filters = new PropertyPreFilters();
        PropertyPreFilters.MySimplePropertyPreFilter excludefilter = filters.addFilter();
        excludefilter.addExcludes(excludeProperties);
        V1Service service = JSONObject.parseObject(JSONObject.toJSONString(jsonObject, excludefilter), V1Service.class);
        JSONArray jsonPorts = jsonObject.getJSONObject("spec").getJSONArray("ports");
        List<V1ServicePort> ports = new ArrayList<>();
        for(int i = 0; i < jsonPorts.size(); i++)
        {
            JSONObject jsonPort = jsonPorts.getJSONObject(i);
            Object tgPort = jsonPort.get("targetPort");
            IntOrString targetPort;
            if(tgPort instanceof Integer)
                targetPort = new IntOrString((Integer)tgPort);
            else
                targetPort = new IntOrString((String)tgPort);
            jsonPort.remove("targetPort");
            V1ServicePort port = JSONObject.parseObject(JSONObject.toJSONString(jsonPort), V1ServicePort.class);
            port.setTargetPort(targetPort);
            ports.add(port);
        }
        service.getSpec().setPorts(ports);
        return service;
    }

    /**
     * 将V1Service转成可以在网络传输的JSONObject对象
     * @param service 需要转换的V1Service
     * @return 转换后的JSONObject
     */
    public static JSONObject transferV1ServiceToJSONObject(V1Service service)
    {
        String[] excludeProperties = {"targetPort"};
        PropertyPreFilters filters = new PropertyPreFilters();
        PropertyPreFilters.MySimplePropertyPreFilter excludefilter = filters.addFilter();
        excludefilter.addExcludes(excludeProperties);
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(service, excludefilter));
        if(service.getSpec() != null && service.getSpec().getPorts() != null)
        {
            JSONArray jsonPorts = new JSONArray();
            for(V1ServicePort port : service.getSpec().getPorts())
            {
                JSONObject jsonPort = JSONObject.parseObject(JSONObject.toJSONString(port, excludefilter));
                if(port.getTargetPort().isInteger())
                    jsonPort.put("targetPort", port.getTargetPort().getIntValue());
                else
                    jsonPort.put("targetPort", port.getTargetPort().getStrValue());
                jsonPorts.add(jsonPort);
            }
            jsonObject.getJSONObject("spec").put("ports", jsonPorts);
        }
        return jsonObject;
    }

    public static JSONObject transferV1DeploymentToJSONObject(V1Deployment deployment)
    {
        List<V1Container> initContainers = deployment.getSpec().getTemplate().getSpec().getInitContainers();
        List<V1Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
        JSONArray initJsonArr = new JSONArray();
        if(initContainers != null && initContainers.size() > 0)
        {
            for(V1Container container : initContainers)
            {
                JSONObject containerJsonObj = getContainerJsonObj(container);
                initJsonArr.add(containerJsonObj);
            }
            deployment.getSpec().getTemplate().getSpec().setInitContainers(new ArrayList<>());
        }
        JSONArray containerArr = new JSONArray();
        if(containers != null && containers.size() > 0)
        {
            for(V1Container container : containers)
            {
                JSONObject containerJsonObj = getContainerJsonObj(container);
                containerArr.add(containerJsonObj);
            }
            deployment.getSpec().getTemplate().getSpec().setContainers(new ArrayList<>());
        }
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(deployment));
        jsonObject.getJSONObject("spec").put("initContainers", initJsonArr);
        jsonObject.getJSONObject("spec").put("containers", containerArr);
        return jsonObject;
    }

    private static JSONObject getContainerJsonObj(V1Container container)
    {
       V1Probe liveProbe = container.getLivenessProbe();
       V1Probe readProbe = container.getReadinessProbe();
       container.setLivenessProbe(null);
       container.setReadinessProbe(null);
       JSONObject containerJson = JSONObject.parseObject(JSONObject.toJSONString(container));
       if(liveProbe != null)
           containerJson.put("livenessProbe", getProbeJsonObj(liveProbe));
       if(readProbe != null)
           containerJson.put("readinessProbe", getProbeJsonObj(readProbe));
       return containerJson;
    }

    private static JSONObject getProbeJsonObj(V1Probe probe)
    {
        IntOrString getPort = probe.getHttpGet() != null ? probe.getHttpGet().getPort() : null;
        IntOrString tcpPort = probe.getTcpSocket() != null ? probe.getTcpSocket().getPort() : null;
        if(getPort != null)
            probe.getHttpGet().setPort(null);
        if(tcpPort != null)
            probe.getTcpSocket().setPort(null);
        JSONObject jsonProbe = JSONObject.parseObject(JSONObject.toJSONString(probe));
        if(getPort != null)
        {
            if(!jsonProbe.containsKey("httpGet") || jsonProbe.getJSONObject("httpGet").size() == 0)
                jsonProbe.put("httpGet", new JSONObject());
            Object port = getPort.isInteger() ? getPort.getIntValue() : getPort.getStrValue();
            jsonProbe.getJSONObject("httpGet").put("port", port);
        }
        if(tcpPort != null)
        {
            if(!jsonProbe.containsKey("tcpSocket") || jsonProbe.getJSONObject("tcpSocket").size() == 0)
                jsonProbe.put("tcpSocket", new JSONObject());
            Object port = tcpPort.isInteger() ? tcpPort.getIntValue() : tcpPort.getStrValue();
            jsonProbe.getJSONObject("tcpSocket").put("port", port);
        }
        return jsonProbe;
    }
}
