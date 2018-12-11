package cn.woblog.android.downloader.notifyutil;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import cn.woblog.android.downloader.R;
import cn.woblog.android.downloader.notifyutil.builder.BigPicBuilder;
import cn.woblog.android.downloader.notifyutil.builder.BigTextBuilder;
import cn.woblog.android.downloader.notifyutil.builder.MailboxBuilder;
import cn.woblog.android.downloader.notifyutil.builder.MediaBuilder;
import cn.woblog.android.downloader.notifyutil.builder.ProgressBuilder;
import cn.woblog.android.downloader.notifyutil.builder.SingleLineBuilder;

/**
 * Created by Administrator on 2017/2/13 0013.
 */

public class NotifyUtil {

    public static final int notifyDownloadSuccessID = 10000000;
    public static final int notifyDownloadErrorID = 10000001;
    public static final int notifyDownloadResultID = 10000002;

    public static Context context;

    public static NotificationManager getNm() {
        return nm;
    }

    private static NotificationManager nm;
    public static void init(Context context1){
        context = context1;
        nm = (NotificationManager) context1
                .getSystemService(Activity.NOTIFICATION_SERVICE);
    }
    public static SingleLineBuilder buildSimple(int id,int smallIcon,String contentTitle ,int contentText,PendingIntent contentIntent){
        SingleLineBuilder builder = new SingleLineBuilder();
        String contex = context.getString(contentText);
        builder.setBase(smallIcon,contentTitle,contex)
                .setId(id)
                .setContentIntent(contentIntent);
        return builder;
    }
    public static SingleLineBuilder buildSimple(int id,int smallIcon,CharSequence contentTitle ,CharSequence contentText,PendingIntent contentIntent){
        SingleLineBuilder builder = new SingleLineBuilder();
        builder.setBase(smallIcon,contentTitle,contentText)
                .setId(id)
                .setContentIntent(contentIntent);
        return builder;
    }
    public static SingleLineBuilder buildResultSimple(int id,int smallIcon,int contentTitle ,int contentText,PendingIntent contentIntent){
        SingleLineBuilder builder = new SingleLineBuilder();
        String title = String.format(context.getString(R.string.notify_download_result_title),contentTitle);
        String content = String.format(context.getString(R.string.notify_download_result_content),contentText,(contentTitle - contentText));
        builder.setBase(smallIcon,title,content)
                .setId(id)
                .setContentIntent(contentIntent);
        return builder;
    }
    @Deprecated
    public static ProgressBuilder buildProgress(int id,int smallIcon,CharSequence contentTitle,int progress,int max,int headStr){
        ProgressBuilder builder = new ProgressBuilder();
        builder.setBase(smallIcon,contentTitle,progress+"/"+max)
                .setId(id);
        builder.setProgress(max,progress,false,context.getString(headStr));
        return builder;
    }

    public static ProgressBuilder buildProgress(int id,int smallIcon,CharSequence contentTitle,int progress,int max,int headStr,String format){
        CharSequence showTitle = contentTitle;
        if(!TextUtils.isEmpty(contentTitle) && contentTitle.length()>17)
            showTitle = contentTitle.subSequence(0,16)+"...";
        ProgressBuilder builder = new ProgressBuilder();
        builder.setBase(smallIcon,showTitle,progress+"/")
                .setId(id);
        builder.setProgressAndFormat(progress,max,false,context.getString(headStr),format);
        return builder;
    }

    public static BigPicBuilder buildBigPic(int id,int smallIcon,CharSequence contentTitle,CharSequence contentText,CharSequence summaryText){
        BigPicBuilder builder = new BigPicBuilder();
        builder.setBase(smallIcon,contentTitle,contentText).setId(id);
        builder.setSummaryText(summaryText);
        return builder;
    }
    public static BigTextBuilder buildBigText(int id,int smallIcon,CharSequence contentTitle,CharSequence contentText){
        BigTextBuilder builder = new BigTextBuilder();
        builder.setBase(smallIcon,contentTitle,contentText).setId(id);
        //builder.setSummaryText(summaryText);
        return builder;
    }
    public static MailboxBuilder buildMailBox(int id,int smallIcon,CharSequence contentTitle){
        MailboxBuilder builder = new MailboxBuilder();
        builder.setBase(smallIcon,contentTitle,"").setId(id);
        return builder;
    }
    public static MediaBuilder buildMedia(int id,int smallIcon,CharSequence contentTitle,CharSequence contentText){
        MediaBuilder builder = new MediaBuilder();
        builder.setBase(smallIcon,contentTitle,contentText).setId(id);
        return builder;
    }
    /*public static CustomViewBuilder buildCustomView(BigPicBuilder builder){

    }*/

    public static void notify(int id,Notification notification){

        nm.notify(id,notification);

    }

    public static PendingIntent buildIntent(Class clazz){
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        Intent intent = new Intent(NotifyUtil.context, clazz);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(NotifyUtil.context, 0, intent, flags);
        return pi;
    }

    public static void cancel(int id){
        if(nm!=null){
            nm.cancel(id);
        }
    }

    public static void cancelAll(){
        if(nm!=null){
            nm.cancelAll();
        }
    }


}
