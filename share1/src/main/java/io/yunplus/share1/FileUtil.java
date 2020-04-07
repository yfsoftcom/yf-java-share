package io.yunplus.share1;

import javax.xml.bind.DatatypeConverter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

public class FileUtil {

    public static String checkumMd5(File input) {
        try (InputStream in = new FileInputStream(input)) {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] block = new byte[4096];
            int length;
            while ((length = in.read(block)) > 0) {
                digest.update(block, 0, length);
            }
            return DatatypeConverter.printHexBinary(digest.digest()).toLowerCase();
        } catch (Exception e) {
            return null;
        }
    }
}