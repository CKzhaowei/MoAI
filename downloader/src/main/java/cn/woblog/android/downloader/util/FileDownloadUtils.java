package cn.woblog.android.downloader.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class FileDownloadUtils {

    public static boolean isNetworkNotOnWifiType() {
        final ConnectivityManager manager = (ConnectivityManager) FileDownloadHelper.getAppContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return true;
        }
        //noinspection MissingPermission, because we check permission accessable when invoked
        final NetworkInfo info = manager.getActiveNetworkInfo();
        return info == null || info.getType() != ConnectivityManager.TYPE_WIFI;
    }
}
