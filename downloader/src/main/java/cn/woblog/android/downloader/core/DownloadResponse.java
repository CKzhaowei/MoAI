package cn.woblog.android.downloader.core;

import cn.woblog.android.downloader.domain.DownloadInfo;
import cn.woblog.android.downloader.exception.DownloadException;


public interface DownloadResponse {

    void onStatusChanged(DownloadInfo downloadInfo);

    void handleException(DownloadException exception);
    void onDownloadFinish(int sum,int success);
}
