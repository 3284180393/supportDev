package com.channelsoft.ccod.support.cmdb.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * @ClassName: YamlUtils
 * @Author: lanhb
 * @Description: 提供同yaml相关的工具函数
 * @Date: 2020/9/25 9:51
 * @Version: 1.0
 */
public class YamlUtils {

    /**
     * 将json字符串转换成yaml字符串
     * @param json 需要转换的json字符串
     * @return 转换后的yaml字符串
     */
    public static String toYamlStringFromJson(String json) throws JsonProcessingException
    {
        JsonNode jsonNodeTree = new ObjectMapper().readTree(json);
        String jsonAsYaml = new YAMLMapper().writeValueAsString(jsonNodeTree);
        System.out.println(jsonAsYaml);
        return jsonAsYaml;
    }
}
