package com.channelsoft.ccod.support.cmdb.exception;

/**
 * @ClassName: NotSupportSetException
 * @Author: lanhb
 * @Description: 用来定义不支持的集群异常
 * @Date: 2020/2/15 17:37
 * @Version: 1.0
 */
public class NotSupportSetException extends Exception{

    private static final long serialVersionUID = -1L;

    public NotSupportSetException(String msg)
    {
        super(msg);
    }
}
