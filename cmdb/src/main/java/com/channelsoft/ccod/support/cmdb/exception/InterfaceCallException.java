package com.channelsoft.ccod.support.cmdb.exception;

/**
 * @ClassName: InterfaceCallException
 * @Author: lanhb
 * @Description: 用来定义接口调用异常的类
 * @Date: 2019/12/13 10:17
 * @Version: 1.0
 */
public class InterfaceCallException extends Exception {

    private static final long serialVersionUID = -1L;

    public InterfaceCallException(String msg)
    {
        super(msg);
    }
}
