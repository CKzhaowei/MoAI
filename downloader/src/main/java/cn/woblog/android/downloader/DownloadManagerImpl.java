package cn.woblog.android.downloader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import cn.woblog.android.downloader.callback.DownloadManager;
import cn.woblog.android.downloader.config.Config;
import cn.woblog.android.downloader.core.DownloadResponse;
import cn.woblog.android.downloader.core.DownloadResponseImpl;
import cn.woblog.android.downloader.core.DownloadTaskImpl;
import cn.woblog.android.downloader.core.DownloadTaskImpl.DownloadTaskListener;
import cn.woblog.android.downloader.core.task.DownloadTask;
import cn.woblog.android.downloader.db.DefaultDownloadDBController;
import cn.woblog.android.downloader.db.DownloadDBController;
import cn.woblog.android.downloader.domain.DownloadInfo;
import cn.woblog.android.downloader.notifyutil.NotifyUtil;
import cn.woblog.android.downloader.util.FileDownloadHelper;
import cn.woblog.android.downloader.util.FileUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DownloadManagerImpl implements DownloadManager, DownloadTaskListener {

    private static final int MIN_EXECUTE_INTERVAL = 500;
    private static DownloadManagerImpl instance;
    private final ExecutorService executorService;
    private final ConcurrentHashMap<Integer, DownloadTask> cacheDownloadTask;
    private final List<DownloadInfo> downloadingCaches;
    private final Context context;

    private final DownloadResponse downloadResponse;
    private final DownloadDBController downloadDBController;
    private final Config config;
    private long lastExecuteTime;
    private int successNum = 0;
    private int downSums = 0;

    private DownloadManagerImpl(Context context, Config config) {
        this.context = context;
        if (config == null) {
            this.config = new Config();
        } else {
            this.config = config;
        }

        if (config.getDownloadDBController() == null) {
            downloadDBController = new DefaultDownloadDBController(context, this.config);
        } else {
            downloadDBController = config.getDownloadDBController();
        }
        successNum = 0;
        if (downloadDBController.findAllDownloading() == null) {
            downloadingCaches = new ArrayList<>();
            downSums = 0;
        } else {
            downloadingCaches = downloadDBController.findAllDownloading();
            downSums = downloadingCaches.size();
            for(DownloadInfo downloadInfo:downloadingCaches) {
                if (downloadInfo.getProgress() > 0 && !FileUtil.isExists(downloadInfo.getPath())) {
                    downloadInfo.setProgress(0);
                    downloadDBController.createOrUpdate(downloadInfo);
                }
            }
        }

        cacheDownloadTask = new ConcurrentHashMap<>();

        downloadDBController.pauseAllDownloading();

        executorService = Executors.newFixedThreadPool(this.config.getDownloadThread());
        FileDownloadHelper.holdContext(context.getApplicationContext());
        NotifyUtil.init(context.getApplicationContext());
        downloadResponse = new DownloadResponseImpl(downloadDBController);
        registerNetworkChangedListener();
    }
    public static DownloadManager getInstance(Context context, Config config) {
        synchronized (DownloadManagerImpl.class) {
            if (instance == null) {
                instance = new DownloadManagerImpl(context, config);
            }
        }
        return instance;
    }

    @Override
    public void setMobileCanDownload(boolean f) {
        config.setMobileCanDownload(f);
    }

    @Override
    public boolean getMobileCanDownload() {
        return config.getMobileCanDownload();
    }

    @Override
    public void download(DownloadInfo downloadInfo) {
        if(!checkDownloadInfoInList(downloadInfo)) {
            downloadingCaches.add(downloadInfo);
            downSums++;
            prepareDownload(downloadInfo);
        }
    }

    private boolean checkDownloadInfoInList(DownloadInfo downloadInfo) {
        boolean rs = false;
        if(downloadingCaches.size()>0) {
            for(DownloadInfo info :downloadingCaches) {
                if(info.getId() == downloadInfo.getId()) {
                    rs = true;
                    break;
                }
            }
        }
        return rs;
    }

    private void prepareDownload(DownloadInfo downloadInfo) {
        if (cacheDownloadTask.size() >= config.getDownloadThread()) {
            downloadInfo.setStatus(DownloadInfo.STATUS_WAIT);
            downloadResponse.onStatusChanged(downloadInfo);
        } else {
            DownloadTaskImpl downloadTask = new DownloadTaskImpl(executorService, downloadResponse,
                    downloadInfo, config, this);
            cacheDownloadTask.put(downloadInfo.getId(), downloadTask);
            downloadInfo.setStatus(DownloadInfo.STATUS_PREPARE_DOWNLOAD);
            downloadResponse.onStatusChanged(downloadInfo);
            downloadTask.start();
        }
    }

    @Override
    public void pause(DownloadInfo downloadInfo) {
        if (isExecute()) {
            downloadInfo.setStatus(DownloadInfo.STATUS_PAUSED);
            cacheDownloadTask.remove(downloadInfo.getId());
            downloadResponse.onStatusChanged(downloadInfo);
            prepareDownloadNextTask();
        }
    }

    @Override
    public void pauseAll() {
        if(downloadDBController!=null) {
            downloadDBController.pauseAllDownloading();
        }
        if(downloadingCaches.size()>0 && cacheDownloadTask.size()>0) {
            Toast.makeText(context.getApplicationContext(),R.string.pause_all_downloading,Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void resumeAll() {
        for(DownloadInfo downloadInfo :downloadingCaches){
            resume(downloadInfo);
        }
    }

    private void prepareDownloadNextTask() {
        for (DownloadInfo downloadInfo : downloadingCaches) {
            if (downloadInfo.getStatus() == DownloadInfo.STATUS_WAIT) {
                prepareDownload(downloadInfo);
                break;
            }
        }
    }

    @Override
    public void resume(DownloadInfo downloadInfo) {
        cacheDownloadTask.remove(downloadInfo.getId());
        prepareDownload(downloadInfo);
    }

    @Override
    public void remove(DownloadInfo downloadInfo) {
        downloadInfo.setStatus(DownloadInfo.STATUS_REMOVED);
        cacheDownloadTask.remove(downloadInfo.getId());
        downloadingCaches.remove(downloadInfo);
        downloadDBController.delete(downloadInfo);
        downloadResponse.onStatusChanged(downloadInfo);
        FileUtil.deleteFile(downloadInfo.getPath());
    }

    @Override
    public void onDestroy() {
        unRegisterListener();
    }

    @Override
    public DownloadInfo getDownloadById(int id) {
        DownloadInfo downloadInfo = null;
        for (DownloadInfo d : downloadingCaches) {
            if (d.getId() == id) {
                downloadInfo = d;
                break;
            }
        }

        if (downloadInfo == null) {
            downloadInfo = downloadDBController.findDownloadedInfoById(id);
        }
        return downloadInfo;
    }

    @Override
    public List<DownloadInfo> findAllDownloading() {
        return downloadingCaches;
    }

    @Override
    public List<DownloadInfo> findAllDownloaded() {
        return downloadDBController.findAllDownloaded();
    }

    @Override
    public DownloadDBController getDownloadDBController() {
        return downloadDBController;
    }

    @Override
    public void onDownloadSuccess(DownloadInfo downloadInfo) {
        cacheDownloadTask.remove(downloadInfo.getId());
        downloadingCaches.remove(downloadInfo);
        successNum++;
        if(downloadingCaches.size() > 0)
            prepareDownloadNextTask();
        else
            downloadResponse.onDownloadFinish(downSums,successNum);
    }

    public boolean isExecute() {
        if (System.currentTimeMillis() - lastExecuteTime > MIN_EXECUTE_INTERVAL) {
            lastExecuteTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }
    private BroadcastReceiver receiver;
    private void registerNetworkChangedListener() {
        receiver = new IntentBroadCastReceiver();
        IntentFilter filter=new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(receiver, filter);
    }
    private void unRegisterListener() {
        if(receiver!=null)
            context.unregisterReceiver(receiver);
        receiver = null;
        Log.i("jay","unRegisterListener");
    }

    /**
     * 没有连接网络
     */
    public static final int NETWORK_NONE = -1;
    /**
     * 移动网络
     */
    public static final int NETWORK_MOBILE = 0;
    /**
     * 无线网络
     */
    public static final int NETWORK_WIFI = 1;
    private int getNetWorkState(Context context) {
        // 得到连接管理器对象
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager
                .getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            if (activeNetworkInfo.getType() == (ConnectivityManager.TYPE_WIFI)) {
                return NETWORK_WIFI;
            } else if (activeNetworkInfo.getType() == (ConnectivityManager.TYPE_MOBILE)) {
                return NETWORK_MOBILE;
            }
        } else {
            return NETWORK_NONE;
        }
        return NETWORK_NONE;
    }
    private class IntentBroadCastReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                int network = getNetWorkState(context);
                //if(network == NETWORK_WIFI) resumeAll();
                if(network == NETWORK_MOBILE){
                    if (getMobileCanDownload()) resumeAll();
                    else pauseAll();
                }
            }
        }
    }
}
