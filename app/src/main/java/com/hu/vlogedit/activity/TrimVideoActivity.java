package com.hu.vlogedit.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.hu.vlogedit.R;
import com.hu.vlogedit.adapter.TrimVideoAdapter;
import com.hu.vlogedit.base.BaseActivity;
import com.hu.vlogedit.model.VideoEditInfo;
import com.hu.vlogedit.util.ExtractFrameWorkThread;
import com.hu.vlogedit.util.ExtractVideoInfoUtil;
import com.hu.vlogedit.util.UIUtil;
import com.hu.vlogedit.util.VideoUtil;
import com.hu.vlogedit.view.NormalProgressDialog;
import com.hu.vlogedit.view.RangeSeekBar;
import com.hu.vlogedit.view.VideoThumbSpacingItemDecoration;

import java.io.File;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 裁剪视频界面
 */
public class TrimVideoActivity extends BaseActivity {

    @BindView(R.id.glsurfaceview)
    GlVideoView mSurfaceView;   // gl_surface_view
    @BindView(R.id.video_shoot_tip)
    TextView mTvShootTip;
    @BindView(R.id.video_thumb_listview)
    RecyclerView mRecyclerView; // 视频剪辑各帧列表
    @BindView(R.id.positionIcon)
    ImageView mIvPosition;  // 剪切分割线
    @BindView(R.id.id_seekBarLayout)
    LinearLayout seekBarLayout; // 剪辑进度条
    @BindView(R.id.layout_surface_view)
    RelativeLayout mRlVideo;    // 视频节目
    @BindView(R.id.view_trim_indicator)
    View mViewTrimIndicator;
    @BindView(R.id.view_effect_indicator)
    View mViewEffectIndicator;
    @BindView(R.id.ll_trim_container)
    LinearLayout mLlTrimContainer;  // 剪辑容器
    @BindView(R.id.hsv_effect)
    HorizontalScrollView mHsvEffect;
    @BindView(R.id.ll_effect_container)
    LinearLayout mLlEffectContainer; // 滤镜容器

    private RangeSeekBar seekBar; // 剪辑进度条

    private static final String TAG = TrimVideoActivity.class.getSimpleName();
    private static final long MIN_CUT_DURATION = 3 * 1000L;// 最小剪辑时间3s
    private static final long MAX_CUT_DURATION = 10 * 1000L;//视频最多剪切多长时间
    private static final int MAX_COUNT_RANGE = 10;//seekBar的区域内一共有多少张图片
    private static final int MARGIN = UIUtil.dp2Px(56); //左右两边间距
    private ExtractVideoInfoUtil mExtractVideoInfoUtil; // 提取视频元信息的工具类
    private int mMaxWidth; //可裁剪区域的最大宽度
    private long duration; //视频总时长
    private TrimVideoAdapter videoEditAdapter;  // 视频剪辑进度条中RecyclerView的Adapter
    private float averageMsPx;//每毫秒所占的px
    private float averagePxMs;//每px所占用的ms毫秒
    private String OutPutFileDirPath;   // 视频剪辑后的保存路径
    private ExtractFrameWorkThread mExtractFrameWorkThread;
    private long leftProgress, rightProgress; //裁剪视频左边区域的时间位置, 右边时间位置
    private long scrollPos = 0;
    private int mScaledTouchSlop;
    private int lastScrollX;
    private boolean isSeeking;
    private String mVideoPath;
    private int mOriginalWidth; //视频原始宽度
    private int mOriginalHeight; //视频原始高度
    private List<FilterModel> mVideoEffects = new ArrayList<>(); //视频滤镜效果
    private MagicFilterType[] mMagicFilterTypes;
    private ValueAnimator mEffectAnimator;
    private SurfaceTexture mSurfaceTexture;
    private MediaPlayer mMediaPlayer;
    private Mp4Composer mMp4Composer;

    // 启动剪辑的Activity
    public static void startActivity(Context context, String videoPath) {
        Intent intent = new Intent(context, TrimVideoActivity.class);
        intent.putExtra("videoPath", videoPath);
        context.startActivity(intent);
    }

    // 获取对应的layout
    @Override
    protected int getLayoutId() {
        return R.layout.activity_trim_video;
    }

    // 初始化
    @Override
    protected void init() {
        // 读取传入的视频路径
        mVideoPath = getIntent().getStringExtra("videoPath");
        // 实例化视频元信息提取工具类
        mExtractVideoInfoUtil = new ExtractVideoInfoUtil(mVideoPath);
        // 可裁剪区域的最大宽度
        mMaxWidth = UIUtil.getScreenWidth() - MARGIN * 2;
        // 触发移动事件的最小距离
        // 自定义View处理touch事件的时候，有的时候需要判断用户是否真的存在movie。
        // 表示滑动的时候，手的移动要大于这个返回的距离值才开始移动控件。
        mScaledTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> e) {
                // 获取视频时长
                e.onNext(mExtractVideoInfoUtil.getVideoLength());
                e.onComplete();
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        subscribe(d);
                    }

                    @Override
                    public void onNext(String s) {
                        // 视频时长
                        duration = Long.valueOf(mExtractVideoInfoUtil.getVideoLength());
                        // 矫正获取到的视频时长不是整数问题
                        float tempDuration = duration / 1000f;
                        duration = new BigDecimal(tempDuration).setScale(0, BigDecimal.ROUND_HALF_UP).intValue() * 1000;
                        // Log.e(TAG, "视频总时长：" + duration);

                        // 为视频剪辑做初始化
                        initEditVideo();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    // 设置标题栏
    @Override
    protected void initToolbar(ToolbarHelper toolbarHelper) {
        toolbarHelper.setTitle("裁剪");
        toolbarHelper.setMenuTitle("发布", v -> {
            trimmerVideo();
        });
    }

    // 初始化view
    @Override
    protected void initView() {
        // 设置视频帧序列的view的布局
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        // 初始化Adapter
        videoEditAdapter = new TrimVideoAdapter(this, mMaxWidth / 10);
        // 邦迪Adapter
        mRecyclerView.setAdapter(videoEditAdapter);
        // 绑定滚动监听器
        mRecyclerView.addOnScrollListener(mOnScrollListener);

        mSurfaceView.init(surfaceTexture -> {
            mSurfaceTexture = surfaceTexture;
            initMediaPlayer(surfaceTexture);
        });

        // 滤镜效果集合
        mMagicFilterTypes = new MagicFilterType[]{
                MagicFilterType.NONE, MagicFilterType.INVERT,
                MagicFilterType.SEPIA, MagicFilterType.BLACKANDWHITE,
                MagicFilterType.TEMPERATURE, MagicFilterType.OVERLAY,
                MagicFilterType.BARRELBLUR, MagicFilterType.POSTERIZE,
                MagicFilterType.CONTRAST, MagicFilterType.GAMMA,
                MagicFilterType.HUE, MagicFilterType.CROSSPROCESS,
                MagicFilterType.GRAYSCALE, MagicFilterType.CGACOLORSPACE,
        };

        for (int i = 0; i < mMagicFilterTypes.length; i++) {
            FilterModel model = new FilterModel();
            model.setName(UIUtils.getString(MagicFilterFactory.filterType2Name(mMagicFilterTypes[i])));
            mVideoEffects.add(model);
        }
        // 动态添加滤镜效果View
        addEffectView();
    }

    // 绑定Tab栏点击事件
    @OnClick({R.id.ll_trim_tab, R.id.ll_effect_tab})
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ll_trim_tab: //裁切tab
                mViewTrimIndicator.setVisibility(View.VISIBLE);
                mViewEffectIndicator.setVisibility(View.GONE);
                mLlTrimContainer.setVisibility(View.VISIBLE);
                mHsvEffect.setVisibility(View.GONE);
                break;
            case R.id.ll_effect_tab: //滤镜tab
                mViewTrimIndicator.setVisibility(View.GONE);
                mViewEffectIndicator.setVisibility(View.VISIBLE);
                mLlTrimContainer.setVisibility(View.GONE);
                mHsvEffect.setVisibility(View.VISIBLE);
                break;
        }
    }

    /**
     * 动态添加滤镜效果View
     */
    private void addEffectView() {
        mLlEffectContainer.removeAllViews();
        for (int i = 0; i < mVideoEffects.size(); i++) {
            View itemView = LayoutInflater.from(this)
                    .inflate(R.layout.item_video_effect, mLlEffectContainer, false);
            TextView tv = itemView.findViewById(R.id.tv);
            ImageView iv = itemView.findViewById(R.id.iv);
            FilterModel model = mVideoEffects.get(i);
            int thumbId = MagicFilterFactory.filterType2Thumb(mMagicFilterTypes[i]);
            Glide.with(App.sApplication)
                    .load(thumbId)
                    .into(iv);
            tv.setText(model.getName());
            int index = i;
            itemView.setOnClickListener(v -> {
                for (int j = 0; j < mLlEffectContainer.getChildCount(); j++) {
                    View tempItemView = mLlEffectContainer.getChildAt(j);
                    TextView tempTv = tempItemView.findViewById(R.id.tv);
                    FilterModel tempModel = mVideoEffects.get(j);
                    if (j == index) {
                        //选中的滤镜效果
                        if (!tempModel.isChecked()) {
                            openEffectAnimation(tempTv, tempModel, true);
                        }
                        ConfigUtils.getInstance().setMagicFilterType(mMagicFilterTypes[j]);
                        mSurfaceView.setFilter(MagicFilterFactory.getFilter());
                    } else {
                        //未选中的滤镜效果
                        if (tempModel.isChecked()) {
                            openEffectAnimation(tempTv, tempModel, false);
                        }
                    }
                }
            });
            mLlEffectContainer.addView(itemView);
        }
    }

    private void openEffectAnimation(TextView tv, FilterModel model, boolean isExpand) {
        model.setChecked(isExpand);
        int startValue = UIUtils.dp2Px(30);
        int endValue = UIUtils.dp2Px(100);
        if (!isExpand) {
            startValue = UIUtils.dp2Px(100);
            endValue = UIUtils.dp2Px(30);
        }
        mEffectAnimator = ValueAnimator.ofInt(startValue, endValue);
        mEffectAnimator.setDuration(300);
        mEffectAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (Integer) animation.getAnimatedValue();
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT, value, Gravity.BOTTOM);
                tv.setLayoutParams(params);
                tv.requestLayout();
            }
        });
        mEffectAnimator.start();
    }

    // 为视频剪辑做准备和初始化
    private void initEditVideo() {
        long startPosition = 0;
        long endPosition = duration;
        int thumbnailsCount;
        int rangeWidth;
        boolean isOver_10_s;

        // 结束位置是否超出了最长剪辑范围
        if (endPosition <= MAX_CUT_DURATION) {
            isOver_10_s = false;
            thumbnailsCount = MAX_COUNT_RANGE; // 视频分区数
            rangeWidth = mMaxWidth; // 最大可剪辑区域宽度
        } else {    // 超出了
            isOver_10_s = true;
            // 计算剪辑区域
            thumbnailsCount = (int) (endPosition * 1.0f / (MAX_CUT_DURATION * 1.0f) * MAX_COUNT_RANGE);
            rangeWidth = mMaxWidth / MAX_COUNT_RANGE * thumbnailsCount;
        }
        // 视频剪辑进度条中各个图片帧之间添加分割线
        mRecyclerView.addItemDecoration(new VideoThumbSpacingItemDecoration(MARGIN, thumbnailsCount));

        // 初始化剪辑进度条
        if (isOver_10_s) {
            seekBar = new RangeSeekBar(this, 0L, MAX_CUT_DURATION);
            seekBar.setSelectedMinValue(0L);
            seekBar.setSelectedMaxValue(MAX_CUT_DURATION);
        } else {
            seekBar = new RangeSeekBar(this, 0L, endPosition);
            seekBar.setSelectedMinValue(0L);
            seekBar.setSelectedMaxValue(endPosition);
        }

        seekBar.setMin_cut_time(MIN_CUT_DURATION);//设置最小裁剪时间
        seekBar.setNotifyWhileDragging(true);   // 设置拖拽时通知
        seekBar.setOnRangeSeekBarChangeListener(mOnRangeSeekBarChangeListener); // 设置监听器
        seekBarLayout.addView(seekBar); // 添加组件

//        Log.d(TAG, "-------thumbnailsCount--->>>>" + thumbnailsCount);
        averageMsPx = duration * 1.0f / rangeWidth * 1.0f;
//        Log.d(TAG, "-------rangeWidth--->>>>" + rangeWidth);
//        Log.d(TAG, "-------localMedia.getDuration()--->>>>" + duration);
//        Log.d(TAG, "-------averageMsPx--->>>>" + averageMsPx);

        // 获取视频提取帧图片的后报错的缓存路径
        OutPutFileDirPath = VideoUtil.getSaveEditThumbnailDir(this);
        // 提取的图片预设的宽高
        int extractW = mMaxWidth / MAX_COUNT_RANGE;
        int extractH = UIUtil.dp2Px(62);

        // 该线程作用为：从视频各个分段提取图片并缓存，并逐个发个主线程
        mExtractFrameWorkThread = new ExtractFrameWorkThread(
                extractW,
                extractH,
                mUIHandler,
                mVideoPath,
                OutPutFileDirPath,
                startPosition,
                endPosition,
                thumbnailsCount);
        mExtractFrameWorkThread.start();

        //init pos icon start
        leftProgress = 0;
        if (isOver_10_s) {
            rightProgress = MAX_CUT_DURATION;
        } else {
            rightProgress = endPosition;
        }
        mTvShootTip.setText(String.format("裁剪 %d s", rightProgress / 1000));
        averagePxMs = (mMaxWidth * 1.0f / (rightProgress - leftProgress));
        // Log.d(TAG, "------averagePxMs----:>>>>>" + averagePxMs);
    }

    /**
     * 初始化MediaPlayer，用于播放视频
     */
    private void initMediaPlayer(SurfaceTexture surfaceTexture) {
        mMediaPlayer = new MediaPlayer();
        try {
            // 设置视频源
            mMediaPlayer.setDataSource(mVideoPath);
            // 创建Surface，用于创建
            Surface surface = new Surface(surfaceTexture);
            mMediaPlayer.setSurface(surface);
            surface.release();
            // 设置循环播放
            mMediaPlayer.setLooping(true);
            // 视频准备监听器，准备完成后触发
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    // 给View设置参数
                    ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
                    int videoWidth = mp.getVideoWidth();
                    int videoHeight = mp.getVideoHeight();
                    float videoProportion = (float) videoWidth / (float) videoHeight;
                    int screenWidth = mRlVideo.getWidth();
                    int screenHeight = mRlVideo.getHeight();
                    float screenProportion = (float) screenWidth / (float) screenHeight;
                    if (videoProportion > screenProportion) {
                        lp.width = screenWidth;
                        lp.height = (int) ((float) screenWidth / videoProportion);
                    } else {
                        lp.width = (int) (videoProportion * (float) screenHeight);
                        lp.height = screenHeight;
                    }
                    mSurfaceView.setLayoutParams(lp);

                    mOriginalWidth = videoWidth;
                    mOriginalHeight = videoHeight;
                    // Log.e("videoView", "videoWidth:" + videoWidth + ", videoHeight:" + videoHeight);

                    // 设置MediaPlayer的OnSeekComplete监听
                    // 可以让播放器从指定的位置开始播放的定位完成后触发
                    mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                        @Override
                        public void onSeekComplete(MediaPlayer mp) {
                            // Log.d(TAG, "------ok----real---start-----");
                            // Log.d(TAG, "------isSeeking-----" + isSeeking);
                            if (!isSeeking) {
                                // 播放视频
                                videoStart();
                            }
                        }
                    });
                }
            });
            // 准备播放器
            mMediaPlayer.prepare();
            videoStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 视频裁剪
     */
    private void trimmerVideo() {
        // 显示加载框
        NormalProgressDialog.showLoading(this, getResources().getString(R.string.in_process), false);
        // 暂停视频
        videoPause();
        // Log.e(TAG, "trimVideo...startSecond:" + leftProgress + ", endSecond:"+ rightProgress); //start:44228, end:48217
        //裁剪后的小视频第一帧图片
        // /storage/emulated/0/haodiaoyu/small_video/picture_1524055390067.jpg
//        Bitmap bitmap = mExtractVideoInfoUtil.extractFrame(leftProgress);
//        String firstFrame = FileUtil.saveBitmap("small_video", bitmap);
//        if (bitmap != null && !bitmap.isRecycled()) {
//            bitmap.recycle();
//            bitmap = null;
//        }
        VideoUtil.cutVideo(mVideoPath, VideoUtil.getTrimmedVideoPath(this, "small_video/trimmedVideo",
                        "trimmedVideo_"), leftProgress / 1000,
                        rightProgress / 1000)
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        subscribe(d);
                    }

                    @Override
                    public void onNext(String outputPath) {
                        // /storage/emulated/0/Android/data/com.kangoo.diaoyur/files/small_video/trimmedVideo_20180416_153217.mp4
                        Log.e(TAG, "cutVideo---onSuccess");
                        try {
                            startMediaCodec(outputPath);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Log.e(TAG, "cutVideo---onError:" + e.toString());
                        NormalProgressDialog.stopLoading();
                        Toast.makeText(TrimVideoActivity.this, "视频裁剪失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    /**
     * 视频添加滤镜效果
     */
    private void startMediaCodec(String srcPath) {
        final String outputPath = VideoUtil.getTrimmedVideoPath(this, "small_video/trimmedVideo",
                "filterVideo_");

        mMp4Composer = new Mp4Composer(srcPath, outputPath)
                // .rotation(Rotation.ROTATION_270)
                //.size(720, 1280)
                .fillMode(FillMode.PRESERVE_ASPECT_FIT)
                .filter(MagicFilterFactory.getFilter())
                .mute(false)
                .flipHorizontal(false)
                .flipVertical(false)
                .listener(new Listener() {
                    @Override
                    public void onProgress(double progress) {
                        Log.d(TAG, "filterVideo---onProgress: " + (int) (progress * 100));
                        runOnUiThread(() -> {
                            //show progress
                        });
                    }

                    @Override
                    public void onCompleted() {
                        Log.d(TAG, "filterVideo---onCompleted");
                        runOnUiThread(() -> {
                            compressVideo(outputPath);
                        });
                    }

                    @Override
                    public void onCanceled() {
                        NormalProgressDialog.stopLoading();
                    }

                    @Override
                    public void onFailed(Exception exception) {
                        Log.e(TAG, "filterVideo---onFailed()");
                        NormalProgressDialog.stopLoading();
                        Toast.makeText(TrimVideoActivity.this, "视频处理失败", Toast.LENGTH_SHORT).show();
                    }
                })
                .start();
    }

    /**
     * 视频压缩
     */
    private void compressVideo(String srcPath) {
        String destDirPath = VideoUtil.getTrimmedVideoDir(this, "small_video");
        Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(ObservableEmitter<String> emitter) {
                try {
                    int outWidth = 0;
                    int outHeight = 0;
                    if (mOriginalWidth > mOriginalHeight) {
                        //横屏
                        outWidth = 720;
                        outHeight = 480;
                    } else {
                        //竖屏
                        outWidth = 480;
                        outHeight = 720;
                    }
                    String compressedFilePath = SiliCompressor.with(TrimVideoActivity.this)
                            .compressVideo(srcPath, destDirPath, outWidth, outHeight, 900000);
                    emitter.onNext(compressedFilePath);
                } catch (Exception e) {
                    emitter.onError(e);
                }
                emitter.onComplete();
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        subscribe(d);
                    }

                    @Override
                    public void onNext(String outputPath) {
                        //源路径: /storage/emulated/0/Android/data/com.kangoo.diaoyur/cache/small_video/trimmedVideo_20180514_163858.mp4
                        //压缩路径: /storage/emulated/0/Android/data/com.kangoo.diaoyur/cache/small_video/VIDEO_20180514_163859.mp4
                        Log.e(TAG, "compressVideo---onSuccess");
                        //获取视频第一帧图片
                        mExtractVideoInfoUtil = new ExtractVideoInfoUtil(outputPath);
                        Bitmap bitmap = mExtractVideoInfoUtil.extractFrame();
                        String firstFrame = FileUtil.saveBitmap("small_video", bitmap);
                        if (bitmap != null && !bitmap.isRecycled()) {
                            bitmap.recycle();
                            bitmap = null;
                        }
                        NormalProgressDialog.stopLoading();

                        VideoPreviewActivity.startActivity(TrimVideoActivity.this, outputPath, firstFrame);
                        finish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        Log.e(TAG, "compressVideo---onError:" + e.toString());
                        NormalProgressDialog.stopLoading();
                        Toast.makeText(TrimVideoActivity.this, "视频压缩失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private boolean isOverScaledTouchSlop;

    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            Log.d(TAG, "-------newState:>>>>>" + newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                isSeeking = false;
//                videoStart();
            } else {
                isSeeking = true;
                if (isOverScaledTouchSlop) {
                    videoPause();
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            isSeeking = false;
            int scrollX = getScrollXDistance();
            //达不到滑动的距离
            if (Math.abs(lastScrollX - scrollX) < mScaledTouchSlop) {
                isOverScaledTouchSlop = false;
                return;
            }
            isOverScaledTouchSlop = true;
            Log.d(TAG, "-------scrollX:>>>>>" + scrollX);
            //初始状态,why ? 因为默认的时候有56dp的空白！
            if (scrollX == -MARGIN) {
                scrollPos = 0;
            } else {
                // why 在这里处理一下,因为onScrollStateChanged早于onScrolled回调
                videoPause();
                isSeeking = true;
                scrollPos = (long) (averageMsPx * (MARGIN + scrollX));
                Log.d(TAG, "-------scrollPos:>>>>>" + scrollPos);
                leftProgress = seekBar.getSelectedMinValue() + scrollPos;
                rightProgress = seekBar.getSelectedMaxValue() + scrollPos;
                Log.d(TAG, "-------leftProgress:>>>>>" + leftProgress);
                mMediaPlayer.seekTo((int) leftProgress);
            }
            lastScrollX = scrollX;
        }
    };

    /**
     * 水平滑动了多少px
     * @return int px
     */
    private int getScrollXDistance() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        int position = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleChildView = layoutManager.findViewByPosition(position);
        int itemWidth = firstVisibleChildView.getWidth();
        return (position) * itemWidth - firstVisibleChildView.getLeft();
    }

    private ValueAnimator animator;

    private void anim() {
        Log.d(TAG, "--anim--onProgressUpdate---->>>>>>>" + mMediaPlayer.getCurrentPosition());
        if (mIvPosition.getVisibility() == View.GONE) {
            mIvPosition.setVisibility(View.VISIBLE);
        }
        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mIvPosition
                .getLayoutParams();
        int start = (int) (MARGIN
                + (leftProgress/*mVideoView.getCurrentPosition()*/ - scrollPos) * averagePxMs);
        int end = (int) (MARGIN + (rightProgress - scrollPos) * averagePxMs);
        animator = ValueAnimator
                .ofInt(start, end)
                .setDuration(
                        (rightProgress - scrollPos) - (leftProgress/*mVideoView.getCurrentPosition()*/
                                - scrollPos));
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.leftMargin = (int) animation.getAnimatedValue();
                mIvPosition.setLayoutParams(params);
            }
        });
        animator.start();
    }

    private final MainHandler mUIHandler = new MainHandler(this);

    // Handler，用于接收提取视频帧的线程传来的消息
    private static class MainHandler extends Handler {
        // 弱引用
        private final WeakReference<TrimVideoActivity> mActivity;

        // 构造函数
        MainHandler(TrimVideoActivity activity) {
            // 弱引用到TrimVideoActivity的实例
            mActivity = new WeakReference<>(activity);
        }

        // 接收信息
        @Override
        public void handleMessage(Message msg) {
            TrimVideoActivity activity = mActivity.get();
            if (activity != null) {
                // 判断是否是提取视频帧图片成功的信息
                if (msg.what == ExtractFrameWorkThread.MSG_SAVE_SUCCESS) {
                    // 将接收到的图片信息添加到Adapter
                    if (activity.videoEditAdapter != null) {
                        VideoEditInfo info = (VideoEditInfo) msg.obj;
                        activity.videoEditAdapter.addItemVideoInfo(info);
                    }
                }
            }
        }
    }
    // 视频进度框设置拖动监听器
    private final RangeSeekBar.OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener = new RangeSeekBar.OnRangeSeekBarChangeListener() {
        @Override
        public void onRangeSeekBarValuesChanged(RangeSeekBar bar, long minValue, long maxValue,
                                                int action, boolean isMin, RangeSeekBar.Thumb pressedThumb) {
            // Log.d(TAG, "-----minValue----->>>>>>" + minValue);
            // Log.d(TAG, "-----maxValue----->>>>>>" + maxValue);
            // 调整进度框左右侧被拖动后的位置
            leftProgress = minValue + scrollPos;
            rightProgress = maxValue + scrollPos;
            // Log.d(TAG, "-----leftProgress----->>>>>>" + leftProgress);
            // Log.d(TAG, "-----rightProgress----->>>>>>" + rightProgress);
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    // Log.d(TAG, "-----ACTION_DOWN---->>>>>>");
                    isSeeking = false;
                    videoPause();
                    break;
                case MotionEvent.ACTION_MOVE:
                    // Log.d(TAG, "-----ACTION_MOVE---->>>>>>");
                    isSeeking = true;
                    // 根据拖动位置调整视频播放位置
                    mMediaPlayer.seekTo((int) (pressedThumb == RangeSeekBar.Thumb.MIN ?
                            leftProgress : rightProgress));
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "-----ACTION_UP--leftProgress--->>>>>>" + leftProgress);
                    isSeeking = false;
                    //从minValue开始播
                    mMediaPlayer.seekTo((int) leftProgress);
//                    videoStart();
                    mTvShootTip
                            .setText(String.format("裁剪 %d s", (rightProgress - leftProgress) / 1000));
                    break;
                default:
                    break;
            }
        }
    };

    // 播放视频
    private void videoStart() {
        Log.d(TAG, "----videoStart----->>>>>>>");
        mMediaPlayer.start();
        mIvPosition.clearAnimation();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        anim();
        handler.removeCallbacks(run);
        handler.post(run);
    }

    private void videoProgressUpdate() {
        long currentPosition = mMediaPlayer.getCurrentPosition();
        Log.d(TAG, "----onProgressUpdate-cp---->>>>>>>" + currentPosition);
        if (currentPosition >= (rightProgress)) {
            mMediaPlayer.seekTo((int) leftProgress);
            mIvPosition.clearAnimation();
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
            anim();
        }
    }

    // 暂停视频
    private void videoPause() {
        isSeeking = false;
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            handler.removeCallbacks(run);
        }
        // Log.d(TAG, "----videoPause----->>>>>>>");
        if (mIvPosition.getVisibility() == View.VISIBLE) {
            mIvPosition.setVisibility(View.GONE);
        }
        mIvPosition.clearAnimation();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 将视频播放位置设置为左侧剪辑进度条时间点
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo((int) leftProgress);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停视频
        videoPause();
    }

    // 设置Handler用于线程间传递消息
    private Handler handler = new Handler();
    // 设置更新视频进度的线程
    private Runnable run = new Runnable() {

        @Override
        public void run() {
            videoProgressUpdate();
            // 延迟操作
            handler.postDelayed(run, 1000);
        }
    };

    @Override
    protected void onDestroy() {
        // 停止加载
        NormalProgressDialog.stopLoading();
        ConfigUtils.getInstance().setMagicFilterType(MagicFilterType.NONE);
        if (animator != null) {
            animator.cancel();
        }
        if (mEffectAnimator != null) {
            mEffectAnimator.cancel();
        }
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        if (mMp4Composer != null) {
            mMp4Composer.cancel();
        }
        if (mExtractVideoInfoUtil != null) {
            mExtractVideoInfoUtil.release();
        }
        if (mExtractFrameWorkThread != null) {
            mExtractFrameWorkThread.stopExtract();
        }
        mRecyclerView.removeOnScrollListener(mOnScrollListener);
        mUIHandler.removeCallbacksAndMessages(null);
        handler.removeCallbacksAndMessages(null);
        //删除视频每一帧的预览图
        if (!TextUtils.isEmpty(OutPutFileDirPath)) {
            VideoUtil.deleteFile(new File(OutPutFileDirPath));
        }
        //删除裁剪后的视频，滤镜视频
        String trimmedDirPath = VideoUtil.getTrimmedVideoDir(this, "small_video/trimmedVideo");
        if (!TextUtils.isEmpty(trimmedDirPath)) {
            VideoUtil.deleteFile(new File(trimmedDirPath));
        }
        super.onDestroy();
    }
}

