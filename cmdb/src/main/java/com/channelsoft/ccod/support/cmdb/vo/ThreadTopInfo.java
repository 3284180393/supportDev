package com.channelsoft.ccod.support.cmdb.vo;

/**
 * @ClassName: ThreadTopInfo
 * @Author: lanhb
 * @Description: 用来封装同线程top信息相关类
 * @Date: 2021/2/26 10:59
 * @Version: 1.0
 */
public class ThreadTopInfo
{
    private String dateTime;
    private String user;
    private String pid;
    private String pr;
    private String ni;
    private String virt;
    private String res;
    private String shr;
    private String s;
    private String cpu;
    private String mem;
    private String time;
    private String command;

    public ThreadTopInfo()
    {

    }
    public ThreadTopInfo(String dateTime, String user, String pid, String pr,
                         String ni, String virt, String res, String shr, String s,
                         String cpu, String mem, String time, String command)
    {
        this.dateTime = dateTime;
        this.user = user;
        this.pid = pid;
        this.pr = pr;
        this.ni = ni;
        this.virt = virt;
        this.res = res;
        this.shr = shr;
        this.s = s;
        this.cpu = cpu;
        this.mem = mem;
        this.time = time;
        this.command = command;
    }

    public String getDateTime()
    {
        return dateTime;
    }

    public void setDateTime(String dateTime)
    {
        this.dateTime = dateTime;
    }

    public String getUser()
    {
        return user;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    public String getPid()
    {
        return pid;
    }

    public void setPid(String pid)
    {
        this.pid = pid;
    }

    public String getPr()
    {
        return pr;
    }

    public void setPr(String pr)
    {
        this.pr = pr;
    }

    public String getNi()
    {
        return ni;
    }

    public void setNi(String ni)
    {
        this.ni = ni;
    }

    public String getVirt()
    {
        return virt;
    }

    public void setVirt(String virt)
    {
        this.virt = virt;
    }

    public String getRes()
    {
        return res;
    }

    public void setRes(String res)
    {
        this.res = res;
    }

    public String getShr()
    {
        return shr;
    }

    public void setShr(String shr)
    {
        this.shr = shr;
    }

    public String getS()
    {
        return s;
    }

    public void setS(String s)
    {
        this.s = s;
    }

    public String getCpu()
    {
        return cpu;
    }

    public void setCpu(String cpu)
    {
        this.cpu = cpu;
    }

    public String getMem()
    {
        return mem;
    }

    public void setMem(String mem)
    {
        this.mem = mem;
    }

    public String getTime()
    {
        return time;
    }

    public void setTime(String time)
    {
        this.time = time;
    }

    public String getCommand()
    {
        return command;
    }

    public void setCommand(String command)
    {
        this.command = command;
    }
}
