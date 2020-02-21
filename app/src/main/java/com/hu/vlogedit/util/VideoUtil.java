package com.hu.vlogedit.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.hu.vlogedit.model.LocalVideoModel;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * 视频工具类
 * 具有一些和视频相关的辅助函数
 */
public class VideoUtil {
    /**
     * 获取本地相册中所有视频文件
     * @param context
     * @return
     */
    public static Observable<ArrayList<LocalVideoModel>> getLocalVideoFiles(final Context context) {

        return Observable.create(new ObservableOnSubscribe<ArrayList<LocalVideoModel>>() {
            @Override
            public void subscribe(ObservableEmitter<ArrayList<LocalVideoModel>> emitter) {
                ArrayList<LocalVideoModel> videoModels = new ArrayList<>();
                ContentResolver resolver = context.getContentResolver();

                try {
                    Cursor cursor = resolver
                            .query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null,
                                    null, null, MediaStore.Video.Media.DATE_MODIFIED + " desc");

                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            LocalVideoModel video = new LocalVideoModel();

                            if (cursor
                                    .getLong(cursor.getColumnIndex(MediaStore.Video.Media.DURATION))
                                    != 0) {
                                video.setDuration(
                                        cursor.getLong(
                                                cursor.getColumnIndex(MediaStore.Video.Media.DURATION)));
                                video.setVideoPath(
                                        cursor.getString(
                                                cursor.getColumnIndex(MediaStore.Video.Media.DATA)));
                                video.setCreateTime(cursor
                                        .getString(
                                                cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)));
                                video.setVideoName(cursor
                                        .getString(cursor
                                                .getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)));
                                videoModels.add(video);
                            }
                        }
                        emitter.onNext(videoModels);
                        cursor.close();
                    }
                } catch (Exception e) {
                    emitter.onError(e);
                }
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    // 获取视频的文件路径
    public static String getVideoFilePath(String url) {
        // 错误处理
        if (TextUtils.isEmpty(url) || url.length() < 5) {
            return "";
        }

        // 处理传入的视频路径url
        // 如果是网络中的视频（开头为http），则不做处理
        // 如果是本地文件，则标准化为“file://”开头，方便可以根据该地址直接读取文件
        if (url.substring(0, 4).equalsIgnoreCase("http")) {
            // 不处理
        } else {
            url = "file://" + url;
        }

        return url;
    }

    // 时间格式转换
    // 将秒转换为“小时：分钟：秒”的格式
    public static String converSecondToTime(long seconds) {
        String timeStr = null;
        int hour = 0;
        int minute = 0;
        int second = 0;

        // 异常处理
        if (seconds <= 0) {
            return "00:00";
        } else {
            minute = (int)seconds / 60;
            if (minute < 60) {
                second = (int)seconds % 60;
                timeStr = unitFormat(minute) + ":" + unitFormat(second);
            } else {
                hour = minute / 60;
                // 如果超出格式可以表示的最大时长，直接设置为最大时长
                if (hour > 99) {
                    return "99:59:59";
                }

                minute = minute % 60;
                second = (int) (seconds - hour * 3600 - minute * 60);
                timeStr = unitFormat(hour) + ":" + unitFormat(minute) + ":" + unitFormat(second);
            }
            timeStr = unitFormat(minute) + ":" + unitFormat(second);
        }

        return timeStr;
    }

    // 标准化数字为两位的格式
    // 如1格式化为“01”
    public static String unitFormat(int i) {
        String retStr = null;
        if (i >= 0 && i < 10)
            retStr = "0" + Integer.toString(i);
        else
            retStr = "" + i;
        return retStr;
    }
}
