package com.channelsoft.ccod.support.cmdb.exception;

/**
 * @ClassName: NexusException
 * @Author: lanhb
 * @Description: 用来定义访问nexus返回的异常
 * @Date: 2019/12/17 17:31
 * @Version: 1.0
 */
public class NexusException extends Exception {

    private static final long serialVersionUID = -1L;

    public NexusException(String msg)
    {
        super(msg);
    }
}
