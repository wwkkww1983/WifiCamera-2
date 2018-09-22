package com.hichip.activity.FishEye;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.hichip.base.HiLog;
import com.hichip.base.HiThread;
import com.hichip.callback.ICameraIOSessionCallback;
import com.hichip.callback.ICameraPlayStateCallback;
import com.hichip.content.HiChipDefines;
import com.hichip.control.HiCamera;
import com.hichip.control.HiGLMonitor;
import com.hichip1.R;
import com.thecamhi.activity.VideoOnlineActivity;
import com.thecamhi.base.HiToast;
import com.thecamhi.base.MyPlaybackGLMonitor;
import com.thecamhi.bean.HiDataValue;
import com.thecamhi.bean.MyCamera;
import com.thecamhi.main.HiActivity;
import com.thecamhi.utils.SharePreUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class FishPlaybackOnlineActivity extends HiActivity implements ICameraIOSessionCallback, ICameraPlayStateCallback, OnTouchListener, OnClickListener, OnCheckedChangeListener {
	private final static int HANDLE_MESSAGE_PROGRESSBAR_RUN = 0x90000002;
	private final static int HANDLE_MESSAGE_SEEKBAR_RUN = 0x90000003;
	public final static short HI_P2P_PB_PLAY = 1;
	public final static short HI_P2P_PB_STOP = 2;
	public final static short HI_P2P_PB_PAUSE = 3;
	public final static short HI_P2P_PB_SETPOS = 4;
	public final static short HI_P2P_PB_GETPOS = 5;
	private int video_width;
	private int video_height;
	private ProgressThread pthread = null;
	private ProgressBar prs_loading;
	private ImageView img_shade;
	private byte[] startTime;
	private byte[] oldStartTime;
	private MyPlaybackGLMonitor mMonitor;
	private MyCamera mCamera;
	private SeekBar prs_playing;
	private boolean mVisible = true;
	private long playback_time;
	private long startTimeLong;
	private long endTimeLong;
	private int progressTime;
	private short model;// PLAY=1,STOP=2,PAUSE=3,SETPOS=4,GETPOS=5
	private RelativeLayout playback_view_screen, rl_view_model;
	private boolean visible = true;
	private ImageView play_btn_playback_online, btn_return;
	private ConnectionChangeReceiver myReceiver;
	private boolean isPlaying = false;
	public LinearLayout mllPlay, rl_guide, ll_top;
	private TextView mTvStartTime, mTvEndTime, tv_install;
	private final static SimpleDateFormat sdf = new SimpleDateFormat("mm:ss");
	private boolean mIsDrag = false;
	private LinearLayout mLlCurrPro;
	private TextView mTvCurrPro, mTvPrecent, tv_tit, play_view_model;
	private boolean mIsEnd = false;
	private String str;
	public RadioButton rbtn_wall_overallview, rbtn_circle;
	private RadioGroup rg_view_model;
	private LinearLayout ll_focus_one, ll_four_bg, ll_focus_two, ll_focus_three, ll_focus_four;

	@Override
	protected void onStart() {
		super.onStart();
		IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		myReceiver = new ConnectionChangeReceiver();
		this.registerReceiver(myReceiver, filter);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_fish_playback_online_lanscape);

		Bundle bundle = this.getIntent().getExtras();
		str = bundle.getString("title");
		String uid = bundle.getString(HiDataValue.EXTRAS_KEY_UID);
		byte[] b_startTime = bundle.getByteArray("st");
		oldStartTime = new byte[8];
		System.arraycopy(b_startTime, 0, oldStartTime, 0, 8);
		playback_time = bundle.getLong("pb_time");

		startTimeLong = bundle.getLong(VideoOnlineActivity.VIDEO_PLAYBACK_START_TIME);
		endTimeLong = bundle.getLong(VideoOnlineActivity.VIDEO_PLAYBACK_END_TIME);

		for (MyCamera camera : HiDataValue.CameraList) {
			if (camera.getUid().equals(uid)) {
				mCamera = camera;
				break;
			}
		}
		// 初始化鱼眼顶装和壁装的模式
		int num = SharePreUtils.getInt("mInstallMode", this, mCamera.getUid());
		mCamera.mInstallMode = num == -1 ? 0 : num;

		initView();
		setListerners();
		showLoadingShade();
		mCamera.registerIOSessionListener(this);
		mCamera.registerPlayStateListener(FishPlaybackOnlineActivity.this);

		if (mCamera.getCommandFunction(HiChipDefines.HI_P2P_PB_QUERY_START_NODST)) {
			startTime = oldStartTime;
		} else {
			if (mCamera.getSummerTimer()) {
				HiChipDefines.STimeDay newTime = new HiChipDefines.STimeDay(oldStartTime, 0);
				newTime.resetData(-1);
				startTime = newTime.parseContent();
			} else {
				startTime = oldStartTime;
			}
		}
		startPlayBack();
		model = HI_P2P_PB_PLAY;
	}

	private void setListerners() {
		mMonitor.setOnTouchListener(this);
		// play_btn_exit.setOnClickListener(this);
		rl_guide.setOnClickListener(this);
		ll_top.setOnClickListener(this);
		btn_return.setOnClickListener(this);
		play_view_model.setOnClickListener(this);
		rg_view_model.setOnCheckedChangeListener(this);
		rl_view_model.setOnClickListener(this);

	}

	private void startPlayBack() {
		new Thread() {
			@Override
			public void run() {
				super.run();
				mCamera.startPlayback(new HiChipDefines.STimeDay(startTime, 0), mMonitor);
			}
		}.start();
	}

	private void initView() {

		mMonitor = (MyPlaybackGLMonitor) findViewById(R.id.monitor_playback_view);
		mMonitor.setCamera(mCamera);
		mMonitor.SetViewType(mMonitor.TOP_ALL_VIEW);/* 设置为鱼眼顶装 */
		mMonitor.SetScreenSize(getWindowManager().getDefaultDisplay().getWidth(), getWindowManager().getDefaultDisplay().getHeight());
		rbtn_wall_overallview = (RadioButton) findViewById(R.id.rbtn_wall_overallview);
		rbtn_circle = (RadioButton) findViewById(R.id.rbtn_circle);
		if (mCamera.mInstallMode == 0) {
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CIRCLE, 1);
		} else {// 壁装
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 1);
			rbtn_wall_overallview.setChecked(true);
		}
		mCamera.setLiveShowMonitor(mMonitor);
		// play_btn_exit = (ImageView) findViewById(R.id.play_btn_playback_exit);
		prs_loading = (ProgressBar) findViewById(R.id.prs_loading);
		img_shade = (ImageView) findViewById(R.id.img_shade);
		mllPlay = (LinearLayout) findViewById(R.id.rl_play);
		mTvStartTime = (TextView) findViewById(R.id.tv_start_time);
		mTvEndTime = (TextView) findViewById(R.id.tv_end_time);
		mTvEndTime.setText(sdf.format(new Date(endTimeLong - startTimeLong)));
		progressTime = (int) ((endTimeLong - startTimeLong) / 1000);
		prs_playing = (SeekBar) findViewById(R.id.prs_playing);
		prs_playing.setMax(progressTime);
		prs_playing.setProgress(0);
		mLlCurrPro = (LinearLayout) findViewById(R.id.ll_cureent_progress);
		mTvCurrPro = (TextView) findViewById(R.id.tv_current_pro);
		mTvPrecent = (TextView) findViewById(R.id.tv_precent);
		rl_guide = (LinearLayout) findViewById(R.id.rl_guide);
		ll_top = (LinearLayout) findViewById(R.id.ll_top);
		tv_tit = (TextView) findViewById(R.id.tv_tit);
		tv_tit.setText(str);
		btn_return = (ImageView) findViewById(R.id.btn_return);
		play_view_model = (TextView) findViewById(R.id.play_view_model);
		play_view_model.setVisibility(mCamera.isFishEye() ? View.VISIBLE : View.GONE);
		rl_view_model = (RelativeLayout) findViewById(R.id.rl_view_model);
		tv_install = (TextView) findViewById(R.id.tv_install);
		tv_install.setText(mCamera.mInstallMode == 0 ? getString(R.string.fish_top) : getString(R.string.fish_wall));
		rg_view_model = (RadioGroup) findViewById(R.id.rg_view_model);
		hideAndShowView();
		ll_focus_one = (LinearLayout) findViewById(R.id.ll_focus_one);
		ll_focus_one.requestFocus();
		ll_four_bg = (LinearLayout) findViewById(R.id.ll_four_bg);
		ll_focus_two = (LinearLayout) findViewById(R.id.ll_focus_two);
		ll_focus_three = (LinearLayout) findViewById(R.id.ll_focus_three);
		ll_focus_four = (LinearLayout) findViewById(R.id.ll_focus_four);

		boolean isFirst = SharePreUtils.getBoolean("cache", FishPlaybackOnlineActivity.this, "isFirstPbOnline");
		rl_guide.setVisibility(isFirst ? View.GONE : View.VISIBLE);

		prs_playing.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if (mIsEnd) {
					mCamera.startPlayback(new HiChipDefines.STimeDay(startTime, 0), mMonitor);
				}
				int count = seekBar.getProgress();
				int pre = count * 100 / progressTime;
				if (pre < 1) { // 1.文件的时长的1% （0-6） 秒重头开始播放 下面要的是整数
					if (!mIsEnd) {
						mCamera.stopPlayback();
						mCamera.startPlayback(new HiChipDefines.STimeDay(startTime, 0), mMonitor);
					}
				} else {
					mCamera.sendIOCtrl(HiChipDefines.HI_P2P_PB_POS_SET, HiChipDefines.HI_P2P_PB_SETPOS_REQ.parseContent(0, pre, startTime));
				}
				model = HI_P2P_PB_PAUSE;
				mIsDrag = false;
				mLlCurrPro.setVisibility(View.GONE);
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				mIsDrag = true;
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (mIsDrag) {
					mLlCurrPro.setVisibility(View.VISIBLE);
					double dou = ((double) progress / progressTime) * 100;
					int rate = (int) Math.round(dou);
					mTvPrecent.setText(rate + "%100");
				} else {
					mLlCurrPro.setVisibility(View.GONE);
				}

			}
		});

		playback_view_screen = (RelativeLayout) findViewById(R.id.playback_view_screen);
		playback_view_screen.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				if (model == 0) {
					return;
				}
				visible = !visible;
				mllPlay.setVisibility(visible ? View.VISIBLE : View.GONE);
			}
		});

		play_btn_playback_online = (ImageView) findViewById(R.id.play_btn_playback_online);
		play_btn_playback_online.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				if (mCamera.getConnectState() == HiCamera.CAMERA_CONNECTION_STATE_LOGIN) {
					play_btn_playback_online.setClickable(false);
					if (mIsEnd) {
						mCamera.startPlayback(new HiChipDefines.STimeDay(startTime, 0), mMonitor);
					} else {
						if (isPlaying) {
							play_btn_playback_online.setSelected(true);
						} else {
							play_btn_playback_online.setSelected(false);
						}
						isPlaying = !isPlaying;
						mCamera.sendIOCtrl(HiChipDefines.HI_P2P_PB_PLAY_CONTROL, HiChipDefines.HI_P2P_S_PB_PLAY_REQ.parseContent(0, HI_P2P_PB_PAUSE, startTime));
					}
				}
			}
		});
	}

	private void hideAndShowView() {
		if (mCamera.mInstallMode == 0) {// 顶装
			for (int i = 0; i < rg_view_model.getChildCount(); i++) {
				if (i == 0) {
					rg_view_model.getChildAt(0).setVisibility(View.GONE);
				} else {
					rg_view_model.getChildAt(i).setVisibility(View.VISIBLE);
				}
			}
		} else {
			for (int i = 0; i < rg_view_model.getChildCount(); i++) {
				if (i == 0 || i == 1) {
					rg_view_model.getChildAt(0).setVisibility(View.VISIBLE);
				} else {
					rg_view_model.getChildAt(i).setVisibility(View.GONE);
				}
			}
		}
	}

	private void setImageVisible(boolean b) {
		if (b) {
			prs_playing.setVisibility(View.VISIBLE);
			play_btn_playback_online.setVisibility(View.VISIBLE);

		} else {

			play_btn_playback_online.setVisibility(View.GONE);
			prs_playing.setVisibility(View.GONE);
		}
	}

	private void showLoadingShade() {

		prs_loading.setMax(100);
		prs_loading.setProgress(10);
		pthread = new ProgressThread();
		pthread.startThread();
	}

	private void displayLoadingShade() {
		if (pthread != null)
			pthread.stopThread();
		pthread = null;
		prs_loading.setVisibility(View.GONE);
		img_shade.setVisibility(View.GONE);

		visible = true;
		setImageVisible(visible);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mCamera != null) {
			mCamera.registerIOSessionListener(this);
			mCamera.registerPlayStateListener(this);
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (mCamera != null) {

			if (model != 0) {
				model = 0;
				oldTime = 0;
			}

			mCamera.stopPlayback();
			mCamera.unregisterIOSessionListener(this);
			mCamera.unregisterPlayStateListener(this);
			HiLog.e("unregister");

		} else {
			HiLog.e("camera == null");
		}

		if (pthread != null) {

			pthread.stopThread();
			pthread = null;

		}

		finish();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (myReceiver != null) {
			unregisterReceiver(myReceiver);
		}
	}

	private class ProgressThread extends HiThread {
		public void run() {
			while (isRunning) {
				sleep(100);
				Message msg = handler.obtainMessage();
				msg.what = HANDLE_MESSAGE_PROGRESSBAR_RUN;
				handler.sendMessage(msg);
			}
		}
	}

	@Override
	public void receiveIOCtrlData(HiCamera camera, int arg1, byte[] arg2, int arg3) {

		if (mCamera != camera)
			return;

		Bundle bundle = new Bundle();
		bundle.putByteArray(HiDataValue.EXTRAS_KEY_DATA, arg2);
		Message msg = handler.obtainMessage();
		msg.what = arg1;
		msg.arg2 = arg3;
		msg.setData(bundle);
		handler.sendMessage(msg);

	}

	@Override
	public void receiveSessionState(HiCamera arg0, int arg1) {
		if (arg0 != mCamera || mCamera == null) {
			return;
		}
		Message message = Message.obtain();
		message.what = HiDataValue.HANDLE_MESSAGE_SESSION_STATE;
		message.arg1 = arg1;
		message.obj = arg0;
		handler.sendMessage(message);

	}

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case HiDataValue.HANDLE_MESSAGE_SESSION_STATE:
				MyCamera camera = (MyCamera) msg.obj;
				switch (msg.arg1) {
				case HiCamera.CAMERA_CONNECTION_STATE_DISCONNECTED:
				case HiCamera.CAMERA_CHANNEL_STREAM_ERROR:

					if (camera != null) {
						camera.stopPlayback();
					}
					HiToast.showToast(FishPlaybackOnlineActivity.this, getString(R.string.tips_network_disconnect));
					NetworkError();
					break;
				}
				break;

			case ICameraPlayStateCallback.PLAY_STATE_START:
				isPlaying = true;
				play_btn_playback_online.setClickable(true);
				model = HI_P2P_PB_PLAY;
				mIsEnd = false;
				video_width = msg.arg1;
				video_height = msg.arg2;
				play_btn_playback_online.setSelected(false);
				resetMonitorSize();
				break;
			case ICameraPlayStateCallback.PLAY_STATE_EDN:
				isPlaying = false;
				mIsEnd = true;
				model = HI_P2P_PB_STOP;
				play_btn_playback_online.setSelected(true);
				play_btn_playback_online.setClickable(true);
				prs_playing.setProgress(progressTime);
				mCamera.stopPlayback();
				HiToast.showToast(FishPlaybackOnlineActivity.this, getString(R.string.tips_stop_video));
				break;
			case ICameraPlayStateCallback.PLAY_STATE_POS:
				break;
			case HANDLE_MESSAGE_PROGRESSBAR_RUN:
				int cur = prs_loading.getProgress();
				if (cur >= 100) {
					prs_loading.setProgress(10);
				} else {
					prs_loading.setProgress(cur + 8);
				}
				model = HI_P2P_PB_PLAY;
				break;
			case HANDLE_MESSAGE_SEEKBAR_RUN:
				if (!mIsDrag) {
					prs_playing.setProgress(msg.arg1);
				}
				mTvStartTime.setText(sdf.format(new Date(msg.arg1 * 1000)));
				break;
			case HiChipDefines.HI_P2P_PB_POS_SET:
				model = HI_P2P_PB_PLAY;
				if (!isPlaying && !mIsEnd) {
					// pos_set 有可能先回调,STATE_START也有可能先回调,两种可能都判断了
					mCamera.sendIOCtrl(HiChipDefines.HI_P2P_PB_PLAY_CONTROL, HiChipDefines.HI_P2P_S_PB_PLAY_REQ.parseContent(0, HI_P2P_PB_PAUSE, startTime));
					play_btn_playback_online.setSelected(false);
					isPlaying = !isPlaying;
				}
				break;
			case HiChipDefines.HI_P2P_PB_PLAY_CONTROL:// stopPlayback
														// startPlayback
														// 都会回调这个CONTROL
														// *****注意******
				// isSelected = !isSelected;
				// if (isPlaying) {
				// play_btn_playback_online.setSelected(isSelected);
				// }
				play_btn_playback_online.setClickable(true);

				break;

			}
		}

	};

	private void resetMonitorSize() {

		if (video_width == 0 || video_height == 0) {
			return;
		}
		displayLoadingShade();
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screen_width = dm.widthPixels;
		int screen_height = dm.heightPixels;
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(screen_width, screen_height);
		lp.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
		lp.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.TRUE);
		mMonitor.setLayoutParams(lp);
	}

	long oldTime;

	@Override
	public void callbackPlayUTC(HiCamera camera, int timeInteger) {
		// +++
		if (mCamera != camera || model == HI_P2P_PB_PAUSE || model == 0)
			return;

		if (oldTime == 0) {
			oldTime = (long) timeInteger;
		}
		long sub = (long) timeInteger - oldTime;
		int step = (int) (sub / 1000);
		Message msg = handler.obtainMessage();
		msg.what = HANDLE_MESSAGE_SEEKBAR_RUN;
		msg.arg1 = step;
		handler.sendMessage(msg);
	}

	@Override
	public void callbackState(HiCamera camera, int arg1, int arg2, int arg3) {
		if (mCamera != camera)
			return;

		if (arg1 == ICameraPlayStateCallback.PLAY_STATE_START) {
			HiLog.e("state=PLAY_STATE_START");
		}

		Message msg = handler.obtainMessage();
		msg.what = arg1;
		msg.arg1 = arg2;
		msg.arg2 = arg3;
		handler.sendMessage(msg);

	}

	public void NetworkError() {
		showAlertnew(null, null, getString(R.string.disconnect), getString(R.string.finish), null, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case DialogInterface.BUTTON_POSITIVE:
					FishPlaybackOnlineActivity.this.finish();
					break;
				case DialogInterface.BUTTON_NEGATIVE:
					FishPlaybackOnlineActivity.this.finish();
					break;
				}

			}
		});
	}

	public class ConnectionChangeReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mobNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			NetworkInfo wifiNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (mobNetInfo != null && !mobNetInfo.isConnected() && wifiNetInfo != null && !wifiNetInfo.isConnected()) {
				HiToast.showToast(context, getString(R.string.tips_wifi_connect_failed));
				if (mCamera != null) {
					mCamera.stopPlayback();
				}
				NetworkError();
				return;
			}
		}
	}

	private float action_down_x;
	private float action_down_y;

	float lastX;
	float lastY;

	int xlenOld;
	int ylenOld;

	float move_x;
	float move_y;

	public float left;
	public float width;
	public float height;
	public float bottom;

	double nLenStart = 0;

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (v.getId() == R.id.monitor_playback_view) {
			int nCnt = event.getPointerCount();
			if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN && 2 == nCnt) {
				mMonitor.setTouchMove(2);
				for (int i = 0; i < nCnt; i++) {
					float x = event.getX(i);
					float y = event.getY(i);
					Point pt = new Point((int) x, (int) y);
				}
				xlenOld = Math.abs((int) event.getX(0) - (int) event.getX(1));
				ylenOld = Math.abs((int) event.getY(0) - (int) event.getY(1));
				nLenStart = Math.sqrt((double) xlenOld * xlenOld + (double) ylenOld * ylenOld);

			} else if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_MOVE && 2 == nCnt) {
				mMonitor.setTouchMove(2);
				for (int i = 0; i < nCnt; i++) {
					float x = event.getX(i);
					float y = event.getY(i);
					Point pt = new Point((int) x, (int) y);
				}
				int xlen = Math.abs((int) event.getX(0) - (int) event.getX(1));
				int ylen = Math.abs((int) event.getY(0) - (int) event.getY(1));

				int moveX = Math.abs(xlen - xlenOld);
				int moveY = Math.abs(ylen - ylenOld);

				double nLenEnd = Math.sqrt((double) xlen * xlen + (double) ylen * ylen);
				if (moveX < 20 && moveY < 20) {
					return false;
				}
				if (nLenEnd > nLenStart) {
					mMonitor.SetZoom(true);
					mMonitor.SetZoom(true);
					mMonitor.SetZoom(true);
					mMonitor.SetZoom(true);
				} else {
					mMonitor.SetZoom(false);
					mMonitor.SetZoom(false);
					mMonitor.SetZoom(false);
					mMonitor.SetZoom(false);
				}

				xlenOld = xlen;
				ylenOld = ylen;
				nLenStart = nLenEnd;

				return true;
			} else if (nCnt == 1) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					if (mMonitor.mFrameMode == 4) {
						float x = event.getRawX();
						float y = event.getRawY();
						float screen_width = getWindowManager().getDefaultDisplay().getWidth();
						float screen_height = getWindowManager().getDefaultDisplay().getHeight();

						Animation alphaAnimation = new AlphaAnimation(1, 0.1f);
						alphaAnimation.setDuration(666);
						alphaAnimation.setInterpolator(new LinearInterpolator());
						alphaAnimation.setRepeatCount(4);
						alphaAnimation.setRepeatMode(Animation.REVERSE);

						// 区域 1
						boolean area_one = x < screen_width / 2 && y < screen_height / 2;
						// 区域2
						boolean area_two = x > screen_width / 2 && y < screen_height / 2;
						// 区域3
						boolean area_there = x < screen_width / 2 && y > screen_height / 2;
						// 区域4
						boolean area_four = x > screen_width / 2 && y > screen_height / 2;
						if (area_one) {
							ll_focus_one.requestFocus();
							ll_focus_one.startAnimation(alphaAnimation);
						} else if (area_two) {
							ll_focus_two.requestFocus();
							ll_focus_two.startAnimation(alphaAnimation);

						} else if (area_there) {
							ll_focus_three.requestFocus();
							ll_focus_three.startAnimation(alphaAnimation);
						} else if (area_four) {
							ll_focus_four.requestFocus();
							ll_focus_four.startAnimation(alphaAnimation);
						}
					}
					mMonitor.setTouchMove(0);
					if (model == 0) {
						return false;
					}
					break;
				case MotionEvent.ACTION_MOVE:
					if (mMonitor.getTouchMove() != 0)
						break;
					if (Math.abs(move_x - action_down_x) > 40 || Math.abs(move_y - action_down_y) > 40) {
						mMonitor.setTouchMove(1);
					}
					break;
				case MotionEvent.ACTION_UP:
					break;
				}
			}
		}
		return false;
	}

	int moveX;
	int moveY;

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.monitor_playback_view:
			break;
		case R.id.rl_guide:
			rl_guide.setVisibility(View.GONE);
			SharePreUtils.putBoolean("cache", FishPlaybackOnlineActivity.this, "isFirstPbOnline", true);
			break;
		case R.id.btn_return:
			finish();
			break;
		case R.id.play_view_model:
			rl_view_model.setVisibility(View.VISIBLE);
			if (mCamera.mInstallMode == 1) {
				if (mMonitor.mWallMode == 0) {
					rbtn_circle.setChecked(true);
				} else {
					rbtn_wall_overallview.setChecked(true);
				}
			}
			ScaleAnimation scaleAnimation = new ScaleAnimation(1.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
			scaleAnimation.setDuration(400);
			rl_view_model.startAnimation(scaleAnimation);
			break;
		case R.id.rl_view_model:
			rl_view_model.setVisibility(View.GONE);
			scaleAnimation = new ScaleAnimation(1.0f, 1.0f, 1.0f, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f);
			scaleAnimation.setDuration(200);
			rl_view_model.startAnimation(scaleAnimation);
			break;
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		// 1.圆 2.圆柱 3.二画面 4.四画面 5.碗
		switch (checkedId) {
		case R.id.rbtn_wall_overallview: // 壁装全景
			rl_view_model.setVisibility(View.GONE);
			ll_four_bg.setVisibility(View.GONE);
			mMonitor.mWallMode = 1;
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 1);
			break;
		case R.id.rbtn_circle:// 圆
			rl_view_model.setVisibility(View.GONE);
			ll_four_bg.setVisibility(View.GONE);
			if (mCamera.mInstallMode == 0) {
				mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CIRCLE, 1);
				mMonitor.setmFrameMode(1);
			} else {// 壁装(圆)
				mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_SIDE, 0);
				mMonitor.mWallMode = 0;
			}
			break;
		case R.id.rbtn_bowl: // 碗
			rl_view_model.setVisibility(View.GONE);
			ll_four_bg.setVisibility(View.GONE);
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CARTOON, 0);// 立体碗
			// mMonitor.SetShowScreenMode(0, 0);// 立体碗
			mMonitor.setmFrameMode(5);
			break;
		case R.id.rbtn_two: // 二画面
			rl_view_model.setVisibility(View.GONE);
			ll_four_bg.setVisibility(View.GONE);
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_COLUMN, 2);
			mMonitor.setmFrameMode(3);
			break;
		case R.id.rbtn_four: // 四画面
			rl_view_model.setVisibility(View.GONE);
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CIRCLE, 4);
			ll_four_bg.setVisibility(View.VISIBLE);
			mMonitor.setmFrameMode(4);
			break;
		case R.id.rbtn_cylinder:// 圆柱
			rl_view_model.setVisibility(View.GONE);
			ll_four_bg.setVisibility(View.GONE);
			mMonitor.SetShowScreenMode(HiGLMonitor.VIEW_MODE_CARTOON, 1);
			mMonitor.setmFrameMode(2);
			break;
		}
	}
}
