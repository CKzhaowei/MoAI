package cn.woblog.android.downloader.callback;

import java.lang.ref.WeakReference;

/**
 * Created by renpingqing on 17/1/22.
 */

public abstract class AbsDownloadListener implements DownloadListener {

  private WeakReference<Object> userTag;

  public AbsDownloadListener() {
  }

  public AbsDownloadListener(WeakReference<Object> userTag) {
    this.userTag = userTag;
  }

  public WeakReference<Object> getUserTag() {
    return userTag;
  }

  public void setUserTag(WeakReference<Object> userTag) {
    this.userTag = userTag;
  }


}
