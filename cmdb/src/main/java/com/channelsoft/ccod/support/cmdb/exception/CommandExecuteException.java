package com.channelsoft.ccod.support.cmdb.exception;

/**
 * @ClassName: CommandExecuteException
 * @Author: lanhb
 * @Description:  执行命令返回错误
 * @Date: 2021/2/26 11:15
 * @Version: 1.0
 */
public class CommandExecuteException extends Exception{

    private static final long serialVersionUID = -1L;

    public CommandExecuteException(String msg)
    {
        super(msg);
    }
}
