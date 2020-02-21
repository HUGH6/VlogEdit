package com.hu.vlogedit.model;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import java.io.File;

/**
 * 本地视频模型
 * @description 代表本地中的视频对象，记录了视频的文件路径、时长等信息
 */
public class LocalVideoModel {
    // 视频id
    private long videoId;
    // 视频名称
    private String videoName = "";
    // 作者名称
    private String authorName = "";
    // 视频描述
    private String description = "";
    // 视频全路径,包含视频文件名的路径信息
    private String videoPath;
    // 视频所在文件夹的路径
    private String videoFolderPath;
    // 创建时间
    private String createTime;
    // 时长
    private long duration = 0;
    // （可能是）压缩图片路径
    private String thumbPath;
    // 旋转
    private int rotate;
    // （不明）
    private String lat;
    // （不明）
    private String lon;

    // 根据视频路径，创建视频模型对象
    public static LocalVideoModel buildVideo(Context context, String videoPath) {
        LocalVideoModel info = new LocalVideoModel();
        info.setVideoPath(videoPath);

        try {
            // 创建MediaPlayer，用于播放音视频
            MediaPlayer mp = MediaPlayer.create(context, Uri.fromFile(new File(videoPath)));
            if (mp != null) {
                // 获取视频时长
                info.setDuration(mp.getDuration());
                // 释放MediaPlayer
                mp.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return info;
    }

    // 计算视频的时长
    public LocalVideoModel calcDuration() {
        // MediaMetadataRetriever 类为从多媒体文件中检索帧和元数据提供了统一的接口。
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();

        try {
            // 设置要使用的数据源(文件路径名)。
            mediaMetadataRetriever.setDataSource(getVideoPath());
            // 该方法检索与keyCode关联的元数据值
            // 取得视频的长度(单位为毫秒)
            String time = mediaMetadataRetriever
                    .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            duration = Long.parseLong(time);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }

    public long getVideoId() {
        return videoId;
    }

    public void setVideoId(long videoId) {
        this.videoId = videoId;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }

    public String getThumbPath() {
        return thumbPath;
    }

    public void setThumbPath(String thumbPath) {
        this.thumbPath = thumbPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getVideoFolderPath() {
        return videoFolderPath;
    }

    public void setVideoFolderPath(String videoFolderPath) {
        this.videoFolderPath = videoFolderPath;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
