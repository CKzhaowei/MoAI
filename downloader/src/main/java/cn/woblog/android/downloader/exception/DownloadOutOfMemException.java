package cn.woblog.android.downloader.exception;

/**
 * Created by renpingqing on 17/1/22.
 */

public class DownloadOutOfMemException extends DownloadException {


  public DownloadOutOfMemException(@ExceptionType int code) {
    super(code);
  }

  public DownloadOutOfMemException(@ExceptionType int code, String message) {
    super(code, message);
  }

  public DownloadOutOfMemException(@ExceptionType int code, String message, Throwable cause) {
    super(code, message, cause);
  }

  public DownloadOutOfMemException(@ExceptionType int code, Throwable cause) {
    super(code, cause);
  }
}
