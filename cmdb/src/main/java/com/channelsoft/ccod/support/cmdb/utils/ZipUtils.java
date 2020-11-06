package com.channelsoft.ccod.support.cmdb.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * @ClassName: ZipUtils
 * @Author: lanhb
 * @Description: 用来封装zip相关功能
 * @Date: 2020/11/6 15:04
 * @Version: 1.0
 */
public class ZipUtils {

    public static void zipFolder(String _path, String target) throws IOException
    {
        Path path = Paths.get(_path);
        //String target = path.getParent() +"/" + path.getFileName() +".zip";
        /*
         System.out.println(path.getFileName());
         System.out.println(path.getRoot());
         System.out.println(path.getParent());System.out.println(target);
        */
        ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(target));
        zipFile(zo,path,"");
        zo.close();
    }

    public static void zipFile(ZipOutputStream zo,Path _path,String parentpath) throws IOException
    {
        File _file = _path.toFile();
        if(_file.isFile())
        {
            byte[] buff = new byte[1024];
            FileInputStream fi = new FileInputStream(_file);
            int len;
            zo.putNextEntry(new ZipEntry(parentpath +"/" + _file.getName()));
            while((len=fi.read(buff))>0)
                zo.write(buff, 0, len);
            zo.closeEntry();
            fi.close();
        }
        if(_file.isDirectory())
        {
            if(_file.listFiles().length==0)
            {
                zo.putNextEntry(new ZipEntry(parentpath.equals("")?_file.getName():parentpath + "/" + _file.getName() + "/"));
            }
            for(File __file : _file.listFiles())
                zipFile(zo,__file.toPath(),parentpath.equals("")?_file.getName():parentpath+ "/" + _file.getName());
        }
    }

    public static void unzip(String path,String target) throws IOException
    {
        File targetfolder = new File(target);
        ZipInputStream zi = new ZipInputStream(new FileInputStream(path));
        ZipEntry ze = null;
        FileOutputStream fo = null;
        byte[] buff = new byte[1024];
        int len;
        while((ze =  zi.getNextEntry())!=null)
        {
            File _file = new File(targetfolder,ze.getName());
            if(!_file.getParentFile().exists()) _file.getParentFile().mkdirs();
            if(ze.isDirectory())
            {
                _file.mkdir();
            }
            else //file
            {
                fo = new FileOutputStream(_file);
                while((len=zi.read(buff))>0) fo.write(buff, 0, len);
                fo.close();
            }
            zi.closeEntry();
        }
        zi.close();
    }

}
