package com.channelsoft.ccod.support.cmdb.utils;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * @ClassName: CharsetDetector
 * @Author: lanhb
 * @Description: 用来处理编码相关的工具类
 * @Date: 2020/10/13 10:25
 * @Version: 1.0
 */
public class CharsetDetector {

    public static String detectCharset(String fileName) throws IOException
    {
        java.io.FileInputStream fis = new java.io.FileInputStream(fileName);

        // (1)
        UniversalDetector detector = new UniversalDetector(null);

        // (2)
        int nread;
        byte[] buf = new byte[4096];
        while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
            detector.handleData(buf, 0, nread);
        }
        // (3)
        detector.dataEnd();
        fis.close();

        // (4)
        String encoding = detector.getDetectedCharset();
        return encoding;
    }

    public Charset detectCharset(File f, String[] charsets) {

        Charset charset = null;

        for (String charsetName : charsets) {
            charset = detectCharset(f, Charset.forName(charsetName));
            if (charset != null) {
                break;
            }
        }

        return charset;
    }

    private Charset detectCharset(File f, Charset charset) {
        try {
            BufferedInputStream input = new BufferedInputStream(new FileInputStream(f));

            CharsetDecoder decoder = charset.newDecoder();
            decoder.reset();

            byte[] buffer = new byte[512];
            boolean identified = false;
            while ((input.read(buffer) != -1) && (!identified)) {
                identified = identify(buffer, decoder);
            }

            input.close();

            if (identified) {
                return charset;
            } else {
                return null;
            }

        } catch (Exception e) {
            return null;
        }
    }

    private boolean identify(byte[] bytes, CharsetDecoder decoder) {
        try {
            decoder.decode(ByteBuffer.wrap(bytes));
        } catch (CharacterCodingException e) {
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws Exception{
        File f = new File("d:\\tmp\\AppGateway.ini");

        String fileName = "d:\\tmp\\ucds_config.cfg.txt";
        java.io.FileInputStream fis = new java.io.FileInputStream(fileName);

        // (1)
        UniversalDetector detector = new UniversalDetector(null);

        // (2)
        int nread;
        byte[] buf = new byte[4096];
        while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
            detector.handleData(buf, 0, nread);
        }
        // (3)
        detector.dataEnd();

        // (4)
        String encoding = detector.getDetectedCharset();
        if (encoding != null) {
            System.out.println("Detected encoding = " + encoding);
        } else {
            System.out.println("No encoding detected.");
        }

        // (5)
        detector.reset();

        String[] charsetsToBeTested = {"windows-1253", "UTF-8", "ISO-8859-7"};

        CharsetDetector cd = new CharsetDetector();
        Charset charset = cd.detectCharset(f, charsetsToBeTested);

        if (charset != null) {
            try {
                InputStreamReader reader = new InputStreamReader(new FileInputStream(f), charset);
                int c = 0;
                while ((c = reader.read()) != -1) {
                    System.out.print((char)c);
                }
                reader.close();
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
            }catch(IOException ioe){
                ioe.printStackTrace();
            }

        }else{
            System.out.println("Unrecognized charset.");
        }
    }
}
