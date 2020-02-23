package com.hu.vlogedit.util;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.text.TextUtils;

import java.io.File;

/**
 * 提取的视频元信息的工具类
 */

public class ExtractVideoInfoUtil {
    // MediaMetadataRetriever 类为从多媒体文件中检索帧和元数据提供了统一的接口。
    private MediaMetadataRetriever mMetadataRetriever;
    private long fileLength = 0;//毫秒

    // 传入多媒体文件路径
    public ExtractVideoInfoUtil(String path) {
        if (TextUtils.isEmpty(path)) {
            throw new RuntimeException("path must be not null !");
        }
        File file = new File(path);
        if (!file.exists()) {
            throw new RuntimeException("path file   not exists !");
        }
        // 实例化MediaMetadataRetriever
        mMetadataRetriever = new MediaMetadataRetriever();
        // 设置使用的数据源
        mMetadataRetriever.setDataSource(file.getAbsolutePath());
    }

    public MediaMetadataRetriever getMetadataRetriever() {
        return mMetadataRetriever;
    }

    // 获取视频的宽度
    public int getVideoWidth() {
        String w = mMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        int width = -1;
        if (!TextUtils.isEmpty(w)) {
            width = Integer.valueOf(w);
        }
        return width;
    }

    // 获取视频的高度
    public int getVideoHeight() {
        String h = mMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        int height = -1;
        if (!TextUtils.isEmpty(h)) {
            height = Integer.valueOf(h);
        }
        return height;
    }

    /**
     * 获取视频的典型的一帧图片，不耗时
     *
     * @return Bitmap
     */
    public Bitmap extractFrame() {
        // 该方法根据给定的选项，找到一个接近给定时间位置的代表帧，并将其作为位图返回。
        return mMetadataRetriever.getFrameAtTime();
    }

    /**
     * 获取视频某一帧,不一定是关键帧
     *
     * @param timeMs 毫秒
     */
    public Bitmap extractFrame(long timeMs) {
        //第一个参数是传入时间，只能是us(微秒)
        //OPTION_CLOSEST ,在给定的时间，检索最近一个帧,这个帧不一定是关键帧。
        //OPTION_CLOSEST_SYNC   在给定的时间，检索最近一个同步与数据源相关联的的帧（关键帧）
        //OPTION_NEXT_SYNC 在给定时间之后检索一个同步与数据源相关联的关键帧。
        //OPTION_PREVIOUS_SYNC  顾名思义，同上
//        Bitmap bitmap = mMetadataRetriever.getFrameAtTime(timeMs * 1000, MediaMetadataRetriever.OPTION_CLOSEST);
        Bitmap bitmap = null;
        for (long i = timeMs; i < fileLength; i += 1000) {
            bitmap = mMetadataRetriever.getFrameAtTime(i * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (bitmap != null) {
                break;
            }
        }
        return bitmap;
    }



    /***
     * 获取视频的长度时间
     *
     * @return String 毫秒
     */
    public String getVideoLength() {
        String len = mMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        fileLength = TextUtils.isEmpty(len) ? 0 : Long.valueOf(len);
        return len;
    }

    /**
     * 获取视频旋转角度
     *
     * @return
     */
    public int getVideoDegree() {
        int degree = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            String degreeStr = mMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            if (!TextUtils.isEmpty(degreeStr)) {
                degree = Integer.valueOf(degreeStr);
            }
        }
        return degree;
    }

    // 释放MediaMetadataRetriver
    public void release() {
        if (mMetadataRetriever != null) {
            mMetadataRetriever.release();
        }
    }


}
