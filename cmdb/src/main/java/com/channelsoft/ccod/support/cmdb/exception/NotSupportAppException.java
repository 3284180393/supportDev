package com.channelsoft.ccod.support.cmdb.exception;

/**
 * @ClassName: NotSupportAppException
 * @Author: lanhb
 * @Description: 用来定义当前不能支持的应用异常
 * @Date: 2019/12/6 11:17
 * @Version: 1.0
 */
public class NotSupportAppException extends Exception {

    private static final long serialVersionUID = -1L;

    public NotSupportAppException(String msg)
    {
        super(msg);
    }
}
