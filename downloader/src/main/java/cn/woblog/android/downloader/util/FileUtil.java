package cn.woblog.android.downloader.util;

import java.io.File;

/**
 * Created by YOTA on 2018/2/3.
 */

public class FileUtil {
    public static void deleteFile(String path) {
        File file = new File(path);
        if(file.exists()) file.delete();
    }
    public static boolean isExists(String path) {
        File file = new File(path);
        return file.exists();
    }
}
