package com.channelsoft.ccod.support.cmdb.exception;

/**
 * @ClassName: K8SException
 * @Author: lanhb
 * @Description: 调用k8s的api时返回调用错误或是k8s的api返回的结果解析异常
 * @Date: 2020/5/27 17:46
 * @Version: 1.0
 */
public class K8SException extends Exception{

    private static final long serialVersionUID = -1L;

    public K8SException(String msg)
    {
        super(msg);
    }

}
