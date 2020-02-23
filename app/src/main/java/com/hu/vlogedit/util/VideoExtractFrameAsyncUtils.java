package com.hu.vlogedit.util;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.hu.vlogedit.model.VideoEditInfo;

/**
 * 视频提取帧图片的工具类
 * @description 用于app在打开视频后，在子线程中将视频分段后每段提取出一帧图片，缓存到本地，并将图片路径和截取的视频时间返回给主线程
 */
public class VideoExtractFrameAsyncUtils {

    private Handler mHandler; // Handler：用于线程之间发送消息

    private  int extractW;  // 提取的图片用于缩放的宽高
    private  int extractH;  // 提取的图片用于缩放的宽高

    // 构造函数
    public VideoExtractFrameAsyncUtils(int extractW, int extractH, Handler mHandler) {
        this.mHandler = mHandler;   // 该Handler来自界面主线程
        this.extractW=extractW;
        this.extractH=extractH;
    }

    // 从视频各个分段提取图片并缓存，并逐个发个主线程
    public void getVideoThumbnailsInfoForEdit(String videoPath, String OutPutFileDirPath, long startPosition, long endPosition, int thumbnailsCount) {
        // MediaMetadataRetriever用于提取多媒体元信息
        MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
        // 设置数据源
        metadataRetriever.setDataSource(videoPath);
        // 设置间隔
        long interval = (endPosition - startPosition) / (thumbnailsCount - 1);
        // 逐个间隔进行
        for (int i = 0; i < thumbnailsCount; i++) {
            // 若停止提取，则释放MediaMetadataRetriever
            if (stop) {
                metadataRetriever.release();
                break;
            }
            // 计算需要提取的时间位置
            long time = startPosition + interval * i;
            // 如果是最后一个间隔
            if (i == thumbnailsCount - 1) {
                if (interval > 1000) {
                    String path = extractFrame(metadataRetriever, endPosition - 800, OutPutFileDirPath);
                    sendAPic(path, endPosition - 800);
                } else {
                    String path = extractFrame(metadataRetriever, endPosition, OutPutFileDirPath);
                    sendAPic(path, endPosition);
                }
            } else {
                // 提取帧图片，缓存到本地
                String path = extractFrame(metadataRetriever, time, OutPutFileDirPath);
                sendAPic(path, time);
            }
        }
        // 迭代结束，则释放MediaMetadataRetriever
        metadataRetriever.release();
    }

    /**
     * @description 成功提取并缓存一张帧图片就向主线程发送一张的信息
     * @param path 提取的图片缓存路径
     * @param time 提取的视频时间位置
     */
    private void sendAPic(String path, long time) {
        // 保存该帧的缓存路径和提取时间节点到VideoEditInfo
        VideoEditInfo info = new VideoEditInfo();
        info.path = path;
        info.time = time;

        // 获取一个Message实例，用于保存需要传递给主线程的信息
        Message msg = mHandler.obtainMessage(ExtractFrameWorkThread.MSG_SAVE_SUCCESS);
        // 保存信息
        msg.obj = info;
        // 将信息发送给主线程中的Handler
        mHandler.sendMessage(msg);
    }

    // 从视频的指定时间处提取帧图片
    // 保存到sd卡缓存中
    // 返回其存储路径
    private String extractFrame(MediaMetadataRetriever metadataRetriever, long time, String OutPutFileDirPath) {
        // 从视频最接近time的地方提取一帧图片
        Bitmap bitmap = metadataRetriever.getFrameAtTime(time * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        // 提取成功
        if (bitmap != null) {
            // 如果对bitmap进行缩放处理,得到的图片大小很小(1kb左右)但会非常模糊,不缩放图片大小60kb左右图片很清晰
            // Bitmap bitmapNew= scaleImage(bitmap);
            Bitmap bitmapNew = bitmap;
            // 保存该帧到本地缓存
            String path= VideoUtil.saveImageToSDForEdit(bitmapNew, OutPutFileDirPath, System.currentTimeMillis() + "_" + time + VideoUtil.POSTFIX);
            // 清理位图对象
            if (bitmapNew!=null &&!bitmapNew.isRecycled()) {
                bitmapNew.recycle();
                bitmapNew = null;
            }
            // 返回帧保存路径
            return path;
        }
        // 提取失败，返回空
        return null;
    }

    /**
     * 缩放图片为固定大小
     * 设置固定的宽度，高度随之变化，使图片不会变形
     * @param bm Bitmap
     * @return Bitmap
     */
    private Bitmap scaleImage(Bitmap bm) {
        if (bm == null) {
            return null;
        }

        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = extractW * 1.0f / width;
        // float scaleHeight =extractH*1.0f / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleWidth);
        Bitmap newBm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix,
                true);
        if (!bm.isRecycled()) {
            bm.recycle();
            bm = null;
        }
        return newBm;
    }

    // 控制提取的标志
    private volatile boolean stop;

    // 停止提取
    public void stopExtract() {
        stop = true;
    }
}
