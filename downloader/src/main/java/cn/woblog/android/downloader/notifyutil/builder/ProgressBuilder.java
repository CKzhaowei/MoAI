package cn.woblog.android.downloader.notifyutil.builder;

import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

/**
 * Created by Administrator on 2017/2/13 0013.
 */

public class ProgressBuilder extends BaseBuilder{
    public int max;
    public int progress;
    public boolean interminate = false;

    @Deprecated
    public ProgressBuilder setProgress(int max,int progress,boolean interminate,String headStr){
        setProgressAndFormat(progress,max,interminate,headStr,"%d/%d");
        return this;
    }

    public ProgressBuilder setProgressAndFormat(int progress,int max,boolean interminate,String headStr,String format){
        this.max = max;
        this.progress = progress;
        this.interminate = interminate;

        //contenttext的显示
        if(TextUtils.isEmpty(format) ){
            format = "%d/%d";
            setContentText(String.format(format,progress,max));
        }else {
            if(format.contains("%%")){//百分比类型
               int progressf = progress * 100 / max;
               if (progressf>100) progressf = 100;
                setContentText(String.format(format,headStr,progressf));
            }else {
                setContentText(String.format(format,progress,max));
            }
        }
        return this;
    }

    @Override
    public void build() {
        super.build();
        cBuilder.setProgress(max,progress, interminate);
        cBuilder.setDefaults(0);
    }
    @Override
    public void refresh() {
        cBuilder.setProgress(max,progress, interminate);
        super.refresh();
    }
}
