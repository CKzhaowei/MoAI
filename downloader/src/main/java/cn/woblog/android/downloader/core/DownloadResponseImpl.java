package cn.woblog.android.downloader.core;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.woblog.android.downloader.R;
import cn.woblog.android.downloader.db.DownloadDBController;
import cn.woblog.android.downloader.domain.DownloadInfo;
import cn.woblog.android.downloader.domain.DownloadThreadInfo;
import cn.woblog.android.downloader.exception.DownloadException;
import cn.woblog.android.downloader.notifyutil.NotifyUtil;
import cn.woblog.android.downloader.notifyutil.builder.ProgressBuilder;

public class DownloadResponseImpl implements DownloadResponse {
    private static final String TAG = "DownloadResponseImpl";
    private final Handler handler;
    private final DownloadDBController downloadDBController;

    public DownloadResponseImpl(DownloadDBController downloadDBController) {
        this.downloadDBController = downloadDBController;

        handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                DownloadInfo downloadInfo = (DownloadInfo) msg.obj;
                switch (downloadInfo.getStatus()) {
                    case DownloadInfo.STATUS_DOWNLOADING:
                        if (downloadInfo.getDownloadListener() != null) {
                            downloadInfo.getDownloadListener()
                                    .onDownloading(downloadInfo.getProgress(), downloadInfo.getSize());
                        }
                        showNotification(downloadInfo,false);
                        break;
                    case DownloadInfo.STATUS_PREPARE_DOWNLOAD:
                        if (downloadInfo.getDownloadListener() != null) {
                            downloadInfo.getDownloadListener().onStart();
                        }
                        break;
                    case DownloadInfo.STATUS_WAIT:
                        if (downloadInfo.getDownloadListener() != null) {
                            downloadInfo.getDownloadListener().onWaited();
                        }
                        break;
                    case DownloadInfo.STATUS_PAUSED:
                        if (downloadInfo.getDownloadListener() != null) {
                            downloadInfo.getDownloadListener().onPaused();
                        }
                        cancelNotify(downloadInfo);
                        break;
                    case DownloadInfo.STATUS_COMPLETED:
                        if (downloadInfo.getDownloadListener() != null) {
                            downloadInfo.getDownloadListener().onDownloadSuccess();
                        }
                        showNotification(downloadInfo,true);
                        break;
                    case DownloadInfo.STATUS_ERROR:
                        if (downloadInfo.getDownloadListener() != null) {
                            downloadInfo.getDownloadListener().onDownloadFailed(downloadInfo.getException());
                        }
                        showErrorNotification(downloadInfo);
                        break;
                    case DownloadInfo.STATUS_REMOVED:
                        if (downloadInfo.getDownloadListener() != null) {
                            downloadInfo.getDownloadListener().onRemoved();
                        }
                        cancelNotify(downloadInfo);
                        break;
                }
            }
        };
    }

    private void showErrorNotification(DownloadInfo downloadInfo) {
        cancelNotify(downloadInfo);
        NotifyUtil.buildSimple(NotifyUtil.notifyDownloadErrorID,R.mipmap.icon_notification_bar_download_failed,downloadInfo.getDownloadTrackName()
                ,R.string.download_error,null)
                .setHeadup()
                .show();
    }

    private void showNotifyResult(int sum,int success) {
        NotifyUtil.cancelAll();
        NotifyUtil.buildResultSimple(NotifyUtil.notifyDownloadResultID,
                R.mipmap.download_complete,
                sum,
                success, null)
                .setHeadup()
                .show();
    }
    CopyOnWriteArrayList<ProgressBuilder> notifyList = new CopyOnWriteArrayList<>();
    private void cancelNotify(DownloadInfo downloadInfo) {
        NotifyUtil.cancel(downloadInfo.getId());
        for(ProgressBuilder build:notifyList) {
            if(build.getId() == downloadInfo.getId()) {
                notifyList.remove(build);
            }
        }
    }
    private ProgressBuilder getProgressBuilder(DownloadInfo downloadInfo) {
        for(ProgressBuilder build:notifyList) {
            if(build.getId() == downloadInfo.getId()) {
                return build;
            }
        }
        ProgressBuilder nbuild = NotifyUtil.buildProgress(downloadInfo.getId(), R.mipmap.icon_notification_bar_downloading, downloadInfo.getDownloadTrackName(),
                (int)downloadInfo.getProgress(), (int) downloadInfo.getSize(),R.string.notify_already_download, "%s%d%%");
        nbuild.build();
        nbuild.setHeadup();
        notifyList.add(nbuild);
        return nbuild;
    }

    private void showNotification(DownloadInfo downloadInfo,boolean isComplete) {
        if (downloadInfo.getSize() != 0) {
            if ( isComplete) {
                cancelNotify(downloadInfo);
                NotifyUtil.buildSimple(NotifyUtil.notifyDownloadSuccessID,R.mipmap.download_complete,downloadInfo.getDownloadTrackName(),
                        R.string.download_complete,null)
                        .setHeadup()
                        .show();
            } else {
                getProgressBuilder(downloadInfo).setProgressAndFormat((int)downloadInfo.getProgress(), (int) downloadInfo.getSize(),
                        false,NotifyUtil.context.getString(R.string.notify_already_download), "%s%d%%").refresh();
            }
        } else {
            getProgressBuilder(downloadInfo).setProgressAndFormat((int)downloadInfo.getProgress(), (int) downloadInfo.getSize(),
                    false,NotifyUtil.context.getString(R.string.notify_already_download), "").refresh();
        }
    }

    @Override
    public void onStatusChanged(DownloadInfo downloadInfo) {
        if (downloadInfo.getStatus() != DownloadInfo.STATUS_REMOVED) {
            downloadDBController.createOrUpdate(downloadInfo);
            if (downloadInfo.getDownloadThreadInfos() != null) {
                for (DownloadThreadInfo threadInfo : downloadInfo.getDownloadThreadInfos()) {
                    downloadDBController.createOrUpdate(threadInfo);
                }
            }
        }

        Message message = handler.obtainMessage(downloadInfo.getId());
        message.obj = downloadInfo;
        message.sendToTarget();
    }

    @Override
    public void handleException(DownloadException exception) {

    }

    @Override
    public void onDownloadFinish(int sum,int success) {
        notifyList.clear();
        showNotifyResult(sum,success);
    }
}
