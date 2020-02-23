package com.hu.vlogedit.util;

import android.os.Handler;

/**
 * 完成提取视频关键帧的线程
 */

public class ExtractFrameWorkThread extends Thread {
    public static final int MSG_SAVE_SUCCESS = 0;
    private String videoPath;   // 视频路径
    private String OutPutFileDirPath;   // 输出路径
    private long startPosition; // 开始位置
    private long endPosition;   // 结束位置
    private int thumbnailsCount;    // 分段数
    private VideoExtractFrameAsyncUtils mVideoExtractFrameAsyncUtils;   // 提取视频帧的工具

    // 构造函数
    public ExtractFrameWorkThread(int extractW, int extractH, Handler mHandler, String videoPath, String OutPutFileDirPath,
                                  long startPosition, long endPosition, int thumbnailsCount) {
        this.videoPath = videoPath;
        this.OutPutFileDirPath = OutPutFileDirPath;
        this.startPosition = startPosition;
        this.endPosition = endPosition;
        this.thumbnailsCount = thumbnailsCount;
        this.mVideoExtractFrameAsyncUtils = new VideoExtractFrameAsyncUtils(extractW,extractH,mHandler);
    }

    @Override
    public void run() {
        super.run();
        // 从视频各个分段提取图片并缓存，并逐个发个主线程
        mVideoExtractFrameAsyncUtils.getVideoThumbnailsInfoForEdit(
                videoPath,
                OutPutFileDirPath,
                startPosition,
                endPosition,
                thumbnailsCount);
    }

    // 停止提取
    public void stopExtract() {
        if (mVideoExtractFrameAsyncUtils != null) {
            mVideoExtractFrameAsyncUtils.stopExtract();
        }
    }
}
