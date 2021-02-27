package com.channelsoft.ccod.support.cmdb.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import com.channelsoft.ccod.support.cmdb.exception.CommandExecuteException;
import com.channelsoft.ccod.support.cmdb.vo.ThreadTopInfo;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import org.springframework.util.Assert;

/**
 * @ClassName: JschTools
 * @Author: lanhb
 * @Description: 用来封装同jsch相关的功能
 * @Date: 2020/2/26 10:19
 * @Version: 1.0
 */
public class JschTools
{
    private static Logger logger = LoggerFactory.getLogger(JschTools.class);

    /**
     * 创建一个jsch的ssh会话
     *
     * @param serverIp
     *            服务器IP
     * @param sshPort
     *            ssh端口
     * @param loginName
     *            登陆名
     * @param password
     *            登陆密码
     * @return 创建的ssh会话
     */
    public static Session createSshSession(String serverIp, int sshPort, String loginName, String password) throws JSchException, IOException, CommandExecuteException
    {
        logger.info("准备开始创建" + serverIp + " ssh会话,用户名" + loginName + ",密码"
                + password + ",ssh端口" + sshPort);
        Assert.isTrue(StringUtils.isNotBlank(serverIp), "serverIp can not be blank");
        Assert.isTrue(sshPort>0 && sshPort<65535, String.format("%d is not legal ssh port", sshPort));
        Assert.isTrue(StringUtils.isNotBlank(loginName), "loginName can not be blank");
        Assert.isTrue(StringUtils.isNotBlank(password), "password can not be blank");
        JSch jsch = new JSch();
        Session session = jsch.getSession(loginName, serverIp, sshPort);
        session.setPassword(password);
        Properties properties = new Properties();
        properties.put("StrictHostKeyChecking", "no");
        session.setConfig(properties);
        session.connect();
        runCommand(session, "source ~/.bash_profile");
        return session;
    }

    /**
     * 在指定时间创建jsch的ssh会话
     *
     * @param serverIp
     *            服务器IP,不可为空
     * @param sshPort
     *            ssh端口,不可为空
     * @param loginName
     *            用户登录名,不可为空
     * @param password
     *            用户登陆密码,不可为空
     * @param timeout
     *            超时时长,不可为空
     * @return 创建的ssh会话,不可为空
     * @throws TimeoutException
     * @throws Exception
     */
    public static Session createSshSession(final String serverIp,
                                           final int sshPort, final String loginName, final String password,
                                           final int timeout) throws TimeoutException,
            Exception
    {
        logger.info("准备开始创建" + serverIp + " ssh会话,用户名" + loginName + ",密码"
                + password + ",ssh端口" + sshPort + ",超时时长" + timeout);
        Assert.isTrue(StringUtils.isNotBlank(serverIp), "serverIp can not be blank");
        Assert.isTrue(sshPort>0 && sshPort<65535, String.format("%d is not legal ssh port", sshPort));
        Assert.isTrue(StringUtils.isNotBlank(loginName), "loginName can not be blank");
        Assert.isTrue(StringUtils.isNotBlank(password), "password can not be blank");
        Assert.isTrue(timeout > 0, String.format("%d is not legel timeout", timeout));
        final ExecutorService executor = Executors.newFixedThreadPool(1);
        FutureTask<Session> futureTask = new FutureTask<Session>(
                new Callable<Session>()
                {
                    @Override
                    public Session call() throws Exception
                    {

                        return createSshSession(serverIp, sshPort, loginName,
                                password);
                    }
                });
        executor.execute(futureTask);
        executor.shutdown();
        try{
            Session session = futureTask.get(timeout, TimeUnit.SECONDS);
            //下面命令用来加载本地环境变量
            runCommand(session, "source ~/.bash_profile");
            return session;
        }
        catch (TimeoutException te){
            throw te;
        }
        catch (Exception ex){
            throw new CommandExecuteException(ex.getMessage());
        }
    }

    /**
     * 通过jsch运行一条命令,并返回命令执行结果
     *
     * @param session
     *            jsch会话
     * @param command
     *            运行命令
     * @return 命令在服务器上的运行结果
     * @throws JSchException
     * @throws Exception
     * @throws CommandExecuteException
     */
    public static String runCommand(Session session, String command)
            throws JSchException, IOException, CommandExecuteException
    {
        logger.debug(String.format("begin to command %s by jsch", command));
        Assert.notNull(session, "session can not be null");
        Assert.isTrue(StringUtils.isNotBlank(command), "command can not be blank");
        StringBuilder sBuilder = new StringBuilder();
        Channel channel = session.openChannel("exec");
        ((ChannelExec) channel).setCommand(command);
        channel.setInputStream(null);
        ((ChannelExec) channel).setErrStream(System.err);
        InputStream in = channel.getInputStream();
        InputStream err = ((ChannelExec) channel).getErrStream();
        channel.connect();
        byte[] tmp = new byte[1024];
        boolean success = true;
        while (true)
        {
            while (in.available() > 0 || err.available() > 0)
            {
                System.out.println("in.length=" + in.available());
                System.out.println("err.length=" + err.available());
                int i = 0;
                if(in.available() > 0)
                {
                    i = in.read(tmp, 0, 1024);
                }
                else
                {
                    i = err.read(tmp, 0, 1024);
                    success = false;
                }
                if (i < 0)
                    break;
                sBuilder.append(new String(tmp, 0, i));
            }
            if (channel.isClosed())
            {
                if (in.available() > 0)
                    continue;
                logger.info("exit-status: " + channel.getExitStatus());
                break;
            }
        }
        logger.info(String.format("execute %s output=%s", command, sBuilder.toString()));
        if (in != null) {
            in.close();
        }
        if(err != null) {
            err.close();
        }
        if (channel != null) {
            channel.disconnect();
        }
        if(!success){
            throw new CommandExecuteException(sBuilder.toString());
        }
        return sBuilder.toString();
    }

    /**
     * 通过jsch运行一条命令
     *
     * @param session
     *            jsch会话
     * @param command
     *            运行命令
     * @param timeout
     *            超时时长
     * @return 命令在服务器上的运行结果
     * @throws TimeoutException
     * @throws CommandExecuteException
     */
    public static String runCommand(final Session session,
                                    final String command, final int timeout) throws TimeoutException, CommandExecuteException
    {
        logger.debug(String.format("begin to exec command %s by jsch, timeout=%d", command, timeout));
        Assert.notNull(session, "session can not be null");
        Assert.isTrue(StringUtils.isNotBlank(command), "command can not be blank");
        final ExecutorService executor = Executors.newFixedThreadPool(1);
        FutureTask<String> futureTask = new FutureTask<>(
                () -> runCommand(session, command));
        executor.execute(futureTask);
        executor.shutdown();
        try{
            String output = futureTask.get(timeout, TimeUnit.SECONDS);
            return output;
        }
        catch (TimeoutException te){
            throw te;
        }
        catch (Exception ex){
            throw new CommandExecuteException(ex.getMessage());
        }

    }

    /**
     * 杀死某个进程
     *
     * @param session
     *            jsch的ssh会话
     * @param pid
     *            需要杀死的进程pid
     * @return 成功杀死返回true
     * @throws JSchException
     * @throws Exception
     */
    public static boolean killThead(Session session, String pid) throws JSchException, CommandExecuteException, IOException
    {
        logger.debug(String.format("begin to kill thread pid=%s", pid));
        Assert.notNull(session, "session can not be null");
        Assert.isTrue(StringUtils.isNotBlank(pid), "pid can not be blank");
        String command = "kill -9 " + pid;
        runCommand(session, command);
        return true;
    }

    /**
     * 杀死某个进程
     *
     * @param session
     *            jsch的ssh会话
     * @param pids
     *            需要杀死的进程pid列表
     * @return 成功杀死返回true
     * @throws JSchException
     * @throws Exception
     */
    public static boolean killThead(Session session, String[] pids)
            throws JSchException, IOException, CommandExecuteException
    {
        Assert.notNull(session, "session can not be null");
        Assert.isTrue(pids!=null && pids.length>0, "pids can not be empty");
        logger.debug(String.format("begin to kill pids=%s", String.join(",", pids)));
        String command = String.format("kill -9 %s", String.join(" ", pids));
        runCommand(session, command);
        return true;
    }

    /**
     * 从服务器获得某个进程的top信息
     *
     * @param session
     *            登陆服务器的ssh会话
     * @param pid
     *            进程pid信息
     * @return 进程的top信息,没有查到返回null
     * @throws JSchException
     * @throws IOException
     */
    public ThreadTopInfo getThreadTopInfo(Session session, String pid)
            throws JSchException, IOException, CommandExecuteException
    {
        logger.debug(String.format("get top info for pid=%s", pid));
        Date nowDate = new Date();
        Assert.notNull(session, "session can not be null");
        Assert.isTrue(StringUtils.isNotBlank(pid), "pid can not be blank");
        String command = "top -b -n 1 | grep " + pid;
        String output = runCommand(session, command);
        if (StringUtils.isBlank(output))
        {
            logger.warn(String.format("can not get top info for pid=%s", pid));
            return null;
        }
        String[] lines = output.split(" ");
        ThreadTopInfo topInfo = null;
        SimpleDateFormat sFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (String line : lines)
        {
            if (StringUtils.isNotBlank(line))
            {
                String[] arr = line.split(" ");
                if (arr.length == 12 && pid.equals(arr[1]))
                {
                    logger.info(pid + "进程当前top信息" + line);
                    topInfo = new ThreadTopInfo(sFormat.format(nowDate),
                            arr[0], arr[1], arr[2], arr[3], arr[4], arr[5],
                            arr[6], arr[7], arr[8], arr[9], arr[10], arr[11]);
                    break;
                }
            }
        }
        return topInfo;
    }

    /**
     * 查询指定用户下的所有进程的pid
     * @param session 服务器的ssh会话
     * @param user 指定用户
     * @return 指定用户的所有进程pid
     * @throws JSchException
     * @throws IOException
     * @throws CommandExecuteException
     */
    public static String[] getUserPid(Session session, String user) throws JSchException, IOException, CommandExecuteException{
        Assert.notNull(session, "session can not be null");
        Assert.isTrue(StringUtils.isNotBlank(user), "user can not be blank");
        logger.debug(String.format("get all pid for user=%s", user));
        String command = String.format("ps -u %s| grep -v PID|awk '{print $1}'", user);
        String[] pids = runCommand(session, command).split("\\\\n");
        logger.info(String.format("%s has pids %s", user, String.join(",", pids)));
        return pids;
    }

    /**
     * 根据过滤关键字过滤指定用户的进程pids
     *
     * @param session ssh会话
     * @param runUser linux的用户
     * @param threadFilterRegEx 进程过滤正则表达式,对ps输出中的CMD进程正则匹配
     * @return 符合要求的进程pids
     * @throws JSchException
     * @throws IOException
     * @throws CommandExecuteException
     */
    public static String[] getThreadPid(Session session, String runUser,
                                        String threadFilterRegEx) throws JSchException, IOException, CommandExecuteException
    {
        logger.debug(String.format("begin to ps thread for user=%s and filterRegex=%s", runUser, threadFilterRegEx));
        Assert.notNull(session, "session can not be null");
        Assert.isTrue(StringUtils.isNotBlank(runUser), "runUser can not be blank");
        Assert.isTrue(StringUtils.isNotBlank(threadFilterRegEx), "threadFilterRegEx can not be blank");
        String command = String.format("ps -ef | grep -E \"^%s \" |awk '{print $2}'", runUser);
        String[] pids = runCommand(session, command).split("\\\\n");
        logger.info(String.format("find %d pids %s for user %s with regexFilter=%s",
                pids.length, String.join(",", pids), runUser, threadFilterRegEx));
        return pids;
    }

    /**
     * 判断一个字符串是否是数字
     * @param string
     * @return
     */
    private static boolean isNumeric(String string)
    {
        Pattern pattern = Pattern.compile("[0-9]*");
        return pattern.matcher(string).matches();
    }

    public String exec(String host,String user,String psw,int port,String command){
        String result="";
        Session session =null;
        ChannelExec openChannel =null;
        try {
            JSch jsch=new JSch();
            session = jsch.getSession(user, host, port);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.setPassword(psw);
            session.connect();
            openChannel = (ChannelExec) session.openChannel("exec");
            openChannel.setCommand(command);
            int exitStatus = openChannel.getExitStatus();
            System.out.println(exitStatus);
            openChannel.connect();
            InputStream in = openChannel.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String buf = null;
            while ((buf = reader.readLine()) != null) {
                result+= new String(buf.getBytes("gbk"),"UTF-8")+"    <br>\r\n";
            }
        } catch (JSchException | IOException e) {
            result+=e.getMessage();
        }finally{
            if(openChannel!=null&&!openChannel.isClosed()){
                openChannel.disconnect();
            }
            if(session!=null&&session.isConnected()){
                session.disconnect();
            }
        }
        return result;
    }

    /**
     * 检查文件/目录是否存在并可以访问,如果存在并可以访问返回文件/目录的绝对路径,否则返回空
     * @param session ssh会话
     * @param basePath base路径,如果为空则默认"/"
     * @param filePath 相对于basePath的相对路径也可以是绝对路径
     * @param isDir 是否是目录
     * @return 如果存在并可以访问返回绝对路径,否则返回""
     * @throws JSchException
     * @throws SftpException
     */
    public static String getFileRealPath(Session session, String basePath, String filePath, boolean isDir) throws JSchException, SftpException
    {
        logger.info("basePath=" + basePath + ";filePath=" + filePath + ";isDir=" + isDir);
        ChannelSftp chSftp = (ChannelSftp) session.openChannel("sftp");
        chSftp.connect();
        String retVal = "";
        if(StringUtils.isBlank(basePath)) {
            chSftp.cd("/");
        }
        else {
            chSftp.cd(basePath);
        }
        try
        {
            String realPath = chSftp.realpath(filePath);
            SftpATTRS attr = chSftp.stat(filePath);
            if(isDir)
            {
                if(attr.isDir())
                {
                    retVal = realPath;
                }
                else
                {
                    logger.error(realPath + "不是一个目录");
                }
            }
            else
            {

                if(!attr.isDir())
                {
                    retVal = realPath;
                }
                else
                {
                    logger.error(realPath + "不是文件");
                }
            }
        }
        finally
        {
            chSftp.disconnect();
        }
        logger.info("basePath=" + basePath + ";filePath=" + filePath + ";isDir=" + isDir + ";realPath=" + retVal);
        return retVal;
    }

    /**
     * 检查file是否存在
     * @param session ssh会话
     * @param path file的绝对路径
     * @param isDir file的类型是否是目录
     * @return 存在结果
     * @throws JSchException
     */
    public static boolean isFileExist(Session session, String path, boolean isDir) throws JSchException
    {
        logger.debug(String.format("check %s is exist, isDir=%b", path, isDir));
        ChannelSftp chSftp = (ChannelSftp) session.openChannel("sftp");
        chSftp.connect();
        boolean isExist = false;
        String retVal = "";
        try
        {
            chSftp.ls(path);
            SftpATTRS attr = chSftp.stat(path);
            if(isDir && attr.isDir())
            {
                isExist = true;
            }
            else if(!isDir && !attr.isDir())
            {
                isExist = true;
            }
        }
        catch (SftpException e)
        {
            // TODO: handle exception
            logger.error("access exception", e);
            isExist = false;
        }
        finally
        {
            chSftp.disconnect();
        }
        logger.info("filePath=" + path + ",isDir=" + isDir + ",exist=" + retVal);
        return isExist;
    }

    /**
     * linux文件拷贝 cp -f src dst,如果dst已经存在将直接覆盖,如果目标目录不存在将会被创建
     * @param session ssh会话
     * @param srcFile 文件源地址
     * @param dstFile 文件目标地址
     * @return 拷贝后文件的完整路径
     * @throws JSchException
     * @throws IOException
     * @throws CommandExecuteException
     */
    public static String copyFile(Session session, String srcFile, String dstFile) throws JSchException, IOException, SftpException, CommandExecuteException
    {
        logger.debug(String.format("copy file %s to %s", srcFile, dstFile));
        ChannelSftp chSftp = (ChannelSftp) session.openChannel("sftp");
        chSftp.connect();
        String[] arr = dstFile.split("/");
        String fileName = arr[arr.length - 1];
        String dstDir = dstFile.substring(0, dstFile.length() - fileName.length());
        try
        {
            chSftp.ls(dstDir);
        }
        catch (Exception e)
        {
            // TODO: handle exception
            logger.error(dstDir + "不存在将会被创建");
            chSftp.mkdir(dstDir);
        }
        chSftp.disconnect();
        String command = "cp -f " + srcFile + " " + dstFile;
        runCommand(session, command);
        return dstFile;
    }
    /**
     * 将文件拷贝到目标目录,如果目标目录不存在目录将会被创建,拷贝前和拷贝后的文件名不改变
     * 例如将/root/1/a.txt拷贝到/root/2目录下是/root/2/a.txt
     * @param session ssh会话
     * @param srcFile 源文件
     * @param dstDir 目标目录
     * @return 拷贝后文件的完整路径
     * @throws JSchException
     * @throws IOException
     * @throws CommandExecuteException
     */
    public static String copyFileToDir(Session session, String srcFile, String dstDir) throws JSchException, IOException, SftpException, CommandExecuteException
    {
        logger.info("将文件从" + srcFile + " copy到" + dstDir + "目录");
        ChannelSftp chSftp = (ChannelSftp) session.openChannel("sftp");
        chSftp.connect();
        try
        {
            chSftp.ls(dstDir);
        }
        catch (Exception e)
        {
            // TODO: handle exception
            logger.error(dstDir + "不存在将会被创建");
            chSftp.mkdir(dstDir);
        }
        chSftp.disconnect();
        String[] arr = srcFile.split("/");
        String fileName = arr[arr.length - 1];
        String dstPath = dstDir + "/" + fileName;
        dstPath = dstPath.replace("//", "/");
        String command = "cp -f " + srcFile + " " + dstPath;
        runCommand(session, command);
        return dstPath;
    }

    /**
     * 将目标文件中的srcKey替换成dstKey
     * @param session ssh会话
     * @param filePath 修改文件
     * @param srcKey 被替换的key
     * @param dstKey 替换key
     * @throws JSchException
     * @throws IOException
     * @throws CommandExecuteException
     */
    public static void replaceFileKeyWord(Session session, String filePath, String srcKey, String dstKey) throws JSchException, IOException, CommandExecuteException
    {
        logger.debug(String.format("replace %s at %s to %s", srcKey, filePath, dstKey));
        String command = "cat " + filePath + "|grep " + srcKey + "|wc -l";
        String output = runCommand(session, command);
        logger.debug(String.format("find %s %s at %s", output, srcKey, filePath));
        int count = Integer.parseInt(output);
        if(count > 0)
        {
            logger.info(filePath + "一共有" + count + "处" + srcKey + "需要替换成" + dstKey);
            command = "sed -i \"s/" + srcKey.replace("/", "\\/") + "/" + dstKey.replace("/", "\\/") + "/g\" " + filePath;
            runCommand(session, command);
        }
        logger.info(String.format("%d %s at %s has been replaced to %s", count, srcKey, filePath, dstKey));
    }

    /**
     * 在目标服务器上创建一个新用户
     * @param session 连接目标服务器的ssh session
     * @param user 需要创建的用户名
     * @param password 用户的登陆密码
     * @param home 用户的home路径
     * @throws JSchException
     * @throws IOException
     * @throws CommandExecuteException
     */
    public static void createUser(Session session, String user, String password, String home) throws JSchException, IOException, CommandExecuteException
    {
        logger.debug(String.format("create user %s with password=%s and home=%s", user, password, home));
        String command = String.format("useradd %s -d %s -m", user, home);
        runCommand(session, command);
        command = String.format("echo '%s'| passwd --stdin %s", password, user);
        runCommand(session, command);
        logger.info(String.format("%s has been created"));
    }

    /**
     * 检查服务器上是否已经存在某个用户
     * @param session 连接服务器的ssh 会话
     * @param user 用户名
     * @return 用户是否存在
     * @throws JSchException
     * @throws IOException
     * @throws CommandExecuteException
     */
    public static boolean isUserExist(Session session, String user) throws JSchException, IOException, CommandExecuteException
    {
        logger.debug(String.format("begin to check user %s exist", user));
        String command = String.format("cat /etc/passwd|grep -v nologin|grep -v halt|grep -v shutdown|awk -F\":\" '{ print $1 }'|grep -E '^%s$'", user);
        String execResult = runCommand(session, command);
        boolean exist = StringUtils.isBlank(execResult) ? false : true;
        logger.info(String.format("%s exist : %b", exist));
        return exist;
    }

    /**
     * 获取服务器指定用户的home目录
     * @param session 连接服务器的ssh会话
     * @param user 指定用户
     * @return 指定用户的home目录
     * @throws JSchException
     * @throws IOException
     * @throws CommandExecuteException
     */
    public static String getUserHome(Session session, String user) throws JSchException, IOException, CommandExecuteException
    {
        logger.debug(String.format("begin to get home of %s", user));
        String command = String.format("cat /etc/passwd|grep -v nologin|grep -v halt|grep -v shutdown|grep -E '^%s:'|awk -F\":\" '{ print $6 }'", user);
        String home = runCommand(session, command);
        logger.info(String.format("home of %s is %s", user, home));
        return home;
    }

    /**
     * 将文件传输到服务器的某个位置
     * @param session 连接服务器的ssh会话
     * @param srcFilePath 文件在本地的存放路径
     * @param dstFilePath 文件在目标服务器的存放路径
     * @throws JSchException
     * @throws SftpException
     */
    public static void put(Session session, String srcFilePath, String dstFilePath) throws JSchException, SftpException
    {
        logger.debug(String.format("begin to put file from %s to %s@%s", srcFilePath, dstFilePath, session.getHost()));
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        try{
            sftp.connect();
            sftp.put(srcFilePath, dstFilePath);
            sftp.disconnect();
        }
        finally {
            sftp.disconnect();
        }
        logger.info(String.format("%s has been put to %s@%s", srcFilePath, dstFilePath, session.getHost()));
    }

    /**
     * 将文件从服务器下载到本地
     * @param session 连接服务器的ssh会话
     * @param srcFilePath 文件在服务器的存放路径
     * @param dstFilePath 文件在本地的存放路径
     * @throws JSchException
     * @throws SftpException
     */
    public static void get(Session session, String srcFilePath, String dstFilePath) throws JSchException, SftpException
    {
        logger.debug(String.format("begin to get file from %s to %s@%s", srcFilePath, dstFilePath, session.getHost()));
        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        try{
            sftp.connect();
            sftp.get(srcFilePath, dstFilePath);
            sftp.disconnect();
        }
        finally {
            sftp.disconnect();
        }
        logger.info(String.format("%s has been get to %s@%s", srcFilePath, dstFilePath, session.getHost()));
    }
}
