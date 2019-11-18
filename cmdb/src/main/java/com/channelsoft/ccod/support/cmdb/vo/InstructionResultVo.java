package com.channelsoft.ccod.support.cmdb.vo;

import org.springframework.util.DigestUtils;

import java.io.Serializable;

/**
 * @ClassName: InstructionResultVo
 * @Author: lanhb
 * @Description: 执行指令返回结果
 * @Date: 2019/11/18 16:39
 * @Version: 1.0
 */
public class InstructionResultVo implements Serializable {
    private static final long serialVersionUID = -4148768233386711389L;

    private String instruction;  //对客户端发出的指令

    private boolean success; //客户端指令是否执行成功

    private String data; //如果执行成功返回执行结果,否则返回失败原因

    private int timestamp; //发出指令的时间戳

    private int nonce; //服务器生成的随机数

    private int clientNonce; //客户端生成的随机数

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }

    public int getNonce() {
        return nonce;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    public int getClientNonce() {
        return clientNonce;
    }

    public void setClientNonce(int clientNonce) {
        this.clientNonce = clientNonce;
    }

    public String generateSignature(String shareSecret)
    {
        String plainText = String.format("%s%b%s%s%d%d%d", instruction, success, data, shareSecret, timestamp,
                nonce, clientNonce);
        return DigestUtils.md5DigestAsHex(plainText.getBytes());
    }

    public boolean verifySignature(String signature, String shareSecret)
    {
        String plainText = String.format("%s%b%s%s%d%d%d", instruction, success, data, shareSecret, timestamp,
                nonce, clientNonce);
        String sig = DigestUtils.md5DigestAsHex(plainText.getBytes());
        return sig.equals(signature) ? true : false;
    }

}
