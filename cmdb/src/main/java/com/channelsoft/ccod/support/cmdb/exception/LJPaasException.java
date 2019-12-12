package com.channelsoft.ccod.support.cmdb.exception;

/**
 * @ClassName: LJPaasException
 * @Author: lanhb
 * @Description: 用来定义蓝鲸paas返回的失败信息
 * @Date: 2019/12/12 17:24
 * @Version: 1.0
 */
public class LJPaasException extends Exception {

    private static final long serialVersionUID = -1L;

    public LJPaasException(String msg)
    {
        super(msg);
    }
}
