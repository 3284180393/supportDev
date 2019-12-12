package com.channelsoft.ccod.support.cmdb.exception;

/**
 * @ClassName: ParamException
 * @Author: lanhb
 * @Description: 用来定义参数异常的类
 * @Date: 2019/12/12 16:05
 * @Version: 1.0
 */
public class ParamException extends Exception{

    private static final long serialVersionUID = -1L;

    public ParamException(String msg)
    {
        super(msg);
    }
}
