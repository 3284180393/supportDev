package com.channelsoft.ccod.support.cmdb.utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @program: supportDev
 * @ClassName: NetTools
 * @author: lanhb
 * @description: 用来封装同net相关的工具函数
 * @create: 2021-02-28 14:43
 **/
public class NetTools {

    /**
     * 测试telnet 机器端口的连通性
     * @param hostname 主机
     * @param port 测试端口
     * @param timeout 连接超时
     * @return 端口是否可以连接
     */
    public static boolean telnet(String hostname, int port, int timeout){
        Socket socket = new Socket();
        boolean isConnected = false;
        try {
            socket.connect(new InetSocketAddress(hostname, port), timeout); // 建立连接
            isConnected = socket.isConnected(); // 通过现有方法查看连通状态
//            System.out.println(isConnected);    // true为连通
        } catch (IOException e) {
            System.out.println("false");        // 当连不通时，直接抛异常，异常捕获即可
        }finally{
            try {
                socket.close();   // 关闭连接
            } catch (IOException e) {
                System.out.println("false");
            }
        }
        return isConnected;
    }

}
