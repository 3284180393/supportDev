package com.channelsoft.ccod.support.cmdb.po;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * @ClassName: AjaxResultPo
 * @Author: lanhb
 * @Description: 用来定义http接口的ajax返回结果
 * @Date: 2019/11/26 11:02
 * @Version: 1.0
 */
public class AjaxResultPo implements Serializable {
    private static final long serialVersionUID = -4148768233386711389L;
    private boolean success;
    private String msg;
    private long total = -1;
    private Object rows;
    /**
     * 消息串的最大长度
     */
    public static Integer maxMassgeLength = 1000;

    /**
     * 返回一个失败的消息对象
     */
    public static AjaxResultPo failed(Exception e) {
        return new AjaxResultPo(false, e.getMessage());
    }

    public AjaxResultPo(boolean flag, String msg) {
        this.success = flag;
        this.msg = msg;
    }

    public long getTotal() {
        return total;
    }
    public void setTotal(long total) {
        this.total = total;
    }
    public Object getRows() {
        return rows;
    }
    public void setRows(Object rows) {
        this.rows = rows;
    }

    public AjaxResultPo(boolean b){
        setSuccess(b);
        setMsg("");
    }


    public AjaxResultPo(boolean success, Object rows) {
        super();
        this.success = success;
        this.rows = rows;
    }
    public AjaxResultPo(boolean b, String msg, long total, Object rows){
        setSuccess(b);
        setMsg(msg);
        setTotal(total);
        setRows(rows);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMsg() {
        return StringUtils.substring(msg, 0, maxMassgeLength);
    }

    public String getMessage() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
