package com.channelsoft.ccod.support.cmdb.vo;

import com.channelsoft.ccod.support.cmdb.constant.ServicePortType;
import com.channelsoft.ccod.support.cmdb.exception.ParamException;

/**
 * @ClassName: PortVo
 * @Author: lanhb
 * @Description: 用来定义同端口相关的类
 * @Date: 2020/8/7 10:59
 * @Version: 1.0
 */
public class PortVo {

    private ServicePortType type;

    private int port;

    private int targetPort;

    private int nodePort;

    private String protocol;

    public ServicePortType getType() {
        return type;
    }

    public void setType(ServicePortType type) {
        this.type = type;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public int getNodePort() {
        return nodePort;
    }

    public void setNodePort(int nodePort) {
        this.nodePort = nodePort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public static PortVo parse(String portStr, ServicePortType portType)
    {
        PortVo vo = new PortVo();
        vo.setType(portType);
        String[] arr = portStr.split("/");
        vo.setProtocol(arr[1]);
        String[] arr1 = arr[0].split("\\:");
        vo.setPort(Integer.parseInt(arr1[0]));
        if(arr1.length > 1)
        {
            if(portType.equals(ServicePortType.ClusterIP))
                vo.setTargetPort(Integer.parseInt(arr1[1]));
            else
                vo.setNodePort(Integer.parseInt(arr1[1]));
        }
        return vo;
    }
}
