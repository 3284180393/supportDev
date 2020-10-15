package com.channelsoft.ccod.support.cmdb.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @ClassName: FileUtils
 * @Author: lanhb
 * @Description: 封装文件管理相关函数的工具类
 * @Date: 2020/10/14 18:03
 * @Version: 1.0
 */
public class FileUtils {

    /**
     * 将文本添加到指定文件里
     * @param saveDir 文件存储路径
     * @param fileName 存储文件名
     * @param content 保存内容
     * @param createNew 如果文件已经存在是覆盖还是添加
     * @return 文件完整路径
     * @throws IOException
     */
    public static String saveContextToFile(String saveDir, String fileName, String content, boolean createNew) throws IOException
    {
        saveDir = saveDir.replaceAll("/$", "");
        File dir = new File(saveDir);
        if(!dir.exists()){
            dir.mkdirs();
        }
        String filePath = String.format("%s/%s", saveDir, fileName);
        File file = new File(filePath);
        if(!file.exists() || createNew){
            file.createNewFile();
        }
        FileWriter writer = new FileWriter(file);
        BufferedWriter out = new BufferedWriter(writer);
        out.write(content);
        out.close();
        writer.close();
        return filePath;
    }
}
