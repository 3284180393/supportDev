package com.channelsoft.ccod.support.cmdb.exception;

/**
 * @ClassName: ClientCollectDataException
 * @Author: lanhb
 * @Description: 用来定义客户端收集的数据异常
 * @Date: 2019/12/19 13:48
 * @Version: 1.0
 */
public class ClientCollectDataException extends Exception{

    private static final long serialVersionUID = -1L;

    public ClientCollectDataException(String msg)
    {
        super(msg);
    }
}
