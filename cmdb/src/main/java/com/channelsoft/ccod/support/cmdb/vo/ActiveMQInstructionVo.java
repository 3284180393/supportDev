package com.channelsoft.ccod.support.cmdb.vo;

import com.alibaba.fastjson.JSONObject;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: ActiveMQInstructionVo
 * @Author: lanhb
 * @Description: 用来定义activemq的指令
 * @Date: 2019/11/18 18:37
 * @Version: 1.0
 */
public class ActiveMQInstructionVo {
    private String instruction;

    private Map<String, String> params;

    private int timestamp;

    private int nonce;

    private int clientNonce;

    public ActiveMQInstructionVo(String instruction, Map<String, String>params, int timestamp, int nonce)
    {
        if(params == null)
        {
            this.params = new HashMap<>();
        }
        else
        {
            this.params = params;
        }
        this.instruction = instruction;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.clientNonce = 0;
    }

    public ActiveMQInstructionVo(String instruction, Map<String, String>params, int timestamp, int nonce, int clientNonce)
    {
        if(params == null)
        {
            this.params = new HashMap<>();
        }
        else
        {
            this.params = params;
        }
        this.instruction = instruction;
        this.timestamp = timestamp;
        this.nonce = nonce;
        this.clientNonce = clientNonce;
    }


    public String generateSignature(String shareSecret)
    {
        String plainText = String.format("%s%s%s%d%d%d", instruction, JSONObject.toJSONString(params), shareSecret,
                timestamp, nonce, clientNonce);
        return DigestUtils.md5DigestAsHex(plainText.getBytes());
    }

    public boolean verifySignature(String signature, String shareSecret)
    {
        String plainText = String.format("%s%s%s%d%d%d", instruction, JSONObject.toJSONString(params), shareSecret,
                timestamp, nonce, clientNonce);
        String sig = DigestUtils.md5DigestAsHex(plainText.getBytes());
        return sig.equals(signature) ? true : false;
    }


    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
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

}
