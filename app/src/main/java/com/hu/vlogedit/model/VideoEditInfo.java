package com.hu.vlogedit.model;

import java.io.Serializable;

/**
 * 视频剪辑帧信息类
 * @description 该类用于存放被剪辑视频的各个分段中提取的帧的信息，包扩帧的缓存路径和提取时间节点
 */
public class VideoEditInfo implements Serializable {

    public String path; //图片的sd卡路径
    public long time;//图片所在视频的时间（毫秒）

    public VideoEditInfo() {}


    @Override
    public String toString() {
        return "VideoEditInfo{" +
                "path='" + path + '\'' +
                ", time='" + time + '\'' +
                '}';
    }
}

