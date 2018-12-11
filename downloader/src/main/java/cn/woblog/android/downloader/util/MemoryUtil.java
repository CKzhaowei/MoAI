package cn.woblog.android.downloader.util;

import android.os.Environment;
import android.os.StatFs;

/**
 * Created by YOTA on 2018/1/24.
 */

public class MemoryUtil {

    public static final long SpareSpace = 10*1024*1024;
    private static String getPath(boolean isSdcard) {
        if (isSdcard) {
            //1.获取内存可用大小,内存路径
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        } else {
            //2.获取sd卡可用大小，sd卡路径
            return Environment.getDataDirectory().getAbsolutePath();
        }
    }
    public static long getAvailSpace(boolean isSdcard) {
        //获取可用内存大小
        StatFs statfs=new StatFs(getPath(isSdcard));
        //获取可用区块的个数
        long count=statfs.getAvailableBlocks();
        //获取区块大小
        long size=statfs.getBlockSize();
        //可用空间总大小
        return count*size;
    }
}
