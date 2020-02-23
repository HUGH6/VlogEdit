package com.hu.vlogedit.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack;
import com.hu.vlogedit.model.LocalVideoModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    private static final String TRIM_PATH = "small_video"; // 剪辑路径
    private static final String THUMB_PATH = "thumb";       //

    public static final String POSTFIX = ".jpeg";   //  从视频中提取出的图片保存时的后缀名

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

    // 获取视频剪辑缓存数据的保存路径
    public static String getSaveEditThumbnailDir(Context context) {
        // 获取sd卡状态（当需要向sd卡中读取或写入数据时，需要判断其状态是否可用）
        String state = Environment.getExternalStorageState();
        // 判断状态是否正常
        // getExternalCacheDir可以获取到 SDCard/Android/data/你的应用包名/cache/目录，一般存放临时缓存数据
        // getCacheDir()方法用于获取/data/data/<application package>/cache目录，一般在sd卡不可用时使用
        File rootDir =
                state.equals(Environment.MEDIA_MOUNTED) ? context.getExternalCacheDir()
                        : context.getCacheDir();
        File folderDir = new File(rootDir.getAbsolutePath() + File.separator + TRIM_PATH + File.separator + THUMB_PATH);
        if (folderDir == null) {
            folderDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + File.separator + "videoeditor" + File.separator + "picture");
        }
        if (!folderDir.exists() && folderDir.mkdirs()) {

        }
        return folderDir.getAbsolutePath();
    }

    // 保存从视频序列中提取的帧图片到sd卡，并返回其存储路径
    // bmp：要保存图片，dirPath：缓存帧图片的路径，fileName：保存时的命名
    public static String saveImageToSDForEdit(Bitmap bmp, String dirPath, String fileName) {
        if (bmp == null) {
            return "";
        }
        // 缓存图片的目录
        File appDir = new File(dirPath);
        // 若该目录不存在则创建
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        // 写入图片数据，将其保存本地
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 返回文件了路径
        return file.getAbsolutePath();
    }

    /**
     * 裁剪视频(异步操作)
     * @param src 源文件
     * @param dest 输出地址
     * @param startSec 开始时间
     * @param endSec 结束时间
     */
    public static Observable<String> cutVideo(final String src, final String dest, final double startSec, final double endSec) {

        return Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) {
                try {
                    double startSecond = startSec;
                    double endSecond = endSec;
                    //构造一个movie对象
                    Movie movie = MovieCreator.build(src);
                    List<Track> tracks = movie.getTracks();
                    movie.setTracks(new ArrayList<Track>());

                    boolean timeCorrected = false;
                    // Here we try to find a track that has sync samples. Since we can only start decoding
                    // at such a sample we SHOULD make sure that the start of the new fragment is exactly
                    // such a frame
                    for (Track track : tracks) {
                        if (track.getSyncSamples() != null && track.getSyncSamples().length > 0) {
                            if (timeCorrected) {
                                // This exception here could be a false positive in case we have multiple tracks
                                // with sync samples at exactly the same positions. E.g. a single movie containing
                                // multiple qualities of the same video (Microsoft Smooth Streaming file)

                                throw new RuntimeException(
                                        "The startTime has already been corrected by another track with SyncSample. Not Supported.");
                            }
                            //矫正开始时间
                            startSecond = correctTimeToSyncSample(track, startSecond, false);
                            //矫正结束时间
                            endSecond = correctTimeToSyncSample(track, endSecond, true);

                            timeCorrected = true;
                        }
                    }

                    // 裁剪后的位置   startSecond:299400, endSecond:309390
                    // 矫正后的位置   startSecond:291.3327083333511, endSecond:313.18787500003214
                    // Log.e(TAG, "startSecond:" + startSecond + ", endSecond:" + endSecond);

                    //fix bug: 部分视频矫正过后会超出10s,这里进行强制限制在10s内
                    if (endSecond - startSecond > 10) {
                        int duration = (int) (endSec - startSec);
                        endSecond = startSecond + duration;
                    }
                    //fix bug: 部分视频裁剪后endSecond=0.0,导致播放失败
                    if (endSecond == 0.0) {
                        int duration = (int) (endSec - startSec);
                        endSecond = startSecond + duration;
                    }

                    for (Track track : tracks) {
                        long currentSample = 0;
                        double currentTime = 0;
                        double lastTime = -1;
                        long startSample = -1;
                        long endSample = -1;

                        for (int i = 0; i < track.getSampleDurations().length; i++) {
                            long delta = track.getSampleDurations()[i];

                            if (currentTime > lastTime && currentTime <= startSecond) {
                                // current sample is still before the new starttime
                                startSample = currentSample;
                            }
                            if (currentTime > lastTime && currentTime <= endSecond) {
                                // current sample is after the new start time and still before the new endtime
                                endSample = currentSample;
                            }

                            lastTime = currentTime;
                            //计算出某一帧的时长 = 采样时长 / 时间长度
                            currentTime +=
                                    (double) delta / (double) track.getTrackMetaData().getTimescale();
                            //这里就是帧数（采样）加一
                            currentSample++;
                        }
                        //在这里，裁剪是根据关键帧进行裁剪的，而不是指定的开始时间和结束时间
                        //startSample:2453, endSample:2846   393
                        //startSample:4795, endSample:5564   769
                        //Log.e(TAG, "startSample:" + startSample + ", endSample:" + endSample);
                        movie.addTrack(new CroppedTrack(track, startSample, endSample));

                        Container out = new Defaul tMp4Builder().build(movie);
                        FileOutputStream fos = new FileOutputStream(String.format(dest));
                        FileChannel fc = fos.getChannel();
                        out.writeContainer(fc);

                        fc.close();
                        fos.close();
                    }

                    emitter.onNext(dest);

                } catch (Exception e) {
                    emitter.onError(e);
                }
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * 矫正裁剪的sample位置
     * @param track 视频轨道
     * @param cutHere 裁剪位置
     * @param next 是否还继续裁剪
     */
    private static double correctTimeToSyncSample(Track track, double cutHere, boolean next) {
        double[] timeOfSyncSamples = new double[track.getSyncSamples().length];
        long currentSample = 0;
        double currentTime = 0;
        for (int i = 0; i < track.getSampleDurations().length; i++) {
            long delta = track.getSampleDurations()[i];

            if (Arrays.binarySearch(track.getSyncSamples(), currentSample + 1) >= 0) {
                // samples always start with 1 but we start with zero therefore +1（采样的下标从1开始而不是0开始，所以要+1 ）
                timeOfSyncSamples[Arrays
                        .binarySearch(track.getSyncSamples(), currentSample + 1)] = currentTime;
            }
            currentTime += (double) delta / (double) track.getTrackMetaData().getTimescale();
            currentSample++;

        }
        double previous = 0;
        for (double timeOfSyncSample : timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                if (next) {
                    return timeOfSyncSample;
                } else {
                    return previous;
                }
            }
            previous = timeOfSyncSample;
        }
        return timeOfSyncSamples[timeOfSyncSamples.length - 1];
    }

}
