package com.channelsoft.ccod.support.cmdb.exception;

/**
 * @ClassName: K8sDataException
 * @Author: lanhb
 * @Description: 用来返回解析k8s数据异常
 * @Date: 2020/6/20 17:39
 * @Version: 1.0
 */
public class K8sDataException extends Exception {

    private static final long serialVersionUID = -1L;

    public K8sDataException(String msg)
    {
        super(msg);
    }
}
