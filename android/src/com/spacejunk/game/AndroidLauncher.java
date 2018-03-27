package com.spacejunk.game;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.facebook.CallbackManager;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.model.ShareVideo;
import com.facebook.share.model.ShareVideoContent;
import com.facebook.share.widget.ShareDialog;
import com.spacejunk.game.interfaces.SystemServices;

import java.io.File;
import java.io.IOException;

public class AndroidLauncher extends AndroidApplication implements SystemServices {


	private static String[] perms = {"android.permission.WRITE_EXTERNAL_STORAGE",
			"android.permission.READ_EXTERNAL_STORAGE",
			"android.permission.RECORD_AUDIO"};

	private static final String SCREEN_SHARE_FILE_PATH = "/temp_facebook_video.mp4";

	public static final String TAG = "AndroidLauncher";

	public static final int WRITE_REQUEST_CODE = 7;
	public static final int AUDIO_REQUEST_CODE = 13;

	private static boolean writeAccepted = false;
	private static boolean recordAudioAccepted = false;


	private static final int PERMISSION_CODE = 1;
	private static final int FACEBOOK_CODE = 3;

	private int mScreenDensity;
	private MediaProjectionManager mProjectionManager;


	private static int DISPLAY_WIDTH = 640;
	private static int DISPLAY_HEIGHT = 480;


	CallbackManager callbackManager;

	private MediaProjection mMediaProjection;
	private VirtualDisplay mVirtualDisplay;
	private MediaRecorder mMediaRecorder;


	private void requestAllPermissions() {

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

			if (this.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
				this.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_REQUEST_CODE);
			}

			if (this.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
				this.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_REQUEST_CODE);
			}


		}
	}

	private void initializeGame() {

		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
		config.useAccelerometer = false;
		config.useCompass = false;

		initialize(new SpaceJunk(SpaceJunk.DIFFICULTY_LEVEL.EASY, this), config);
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestAllPermissions();
		initializeFacebookSDK();
		initializeGame();

	}


	@Override
	public void onDestroy() {
		super.onDestroy();


		if (mMediaProjection != null) {
			mMediaProjection.stop();
			mMediaProjection = null;
		}

		deleteTempFile();

	}


	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (requestCode == FACEBOOK_CODE) {

			if(resultCode != RESULT_OK) {
				return;
			}

			callbackManager.onActivityResult(requestCode, resultCode, data);
			return;
		}


		if(requestCode == PERMISSION_CODE) {

			if (resultCode != RESULT_OK) {
				Toast.makeText(this, getString(R.string.screen_record_denied), Toast.LENGTH_SHORT).show();
				return;
			}


			mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
			mVirtualDisplay = createVirtualDisplay();

			mMediaRecorder.start();
			Toast.makeText(this, getString(R.string.screen_record_accepted), Toast.LENGTH_SHORT).show();

		}

	}


	@Override
	public void onRequestPermissionsResult(int permsRequestCode, @NonNull String[] permissions, @NonNull int[] grantResults){

		switch(permsRequestCode){

			case WRITE_REQUEST_CODE:
				writeAccepted = (grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED);
				break;
			case AUDIO_REQUEST_CODE:
				recordAudioAccepted = (grantResults.length > 0 && grantResults[0]==PackageManager.PERMISSION_GRANTED);
				break;
			default:
				break;

		}

	}


	@Override
	public void startRecording(int xMax, int yMax, boolean recordAudioSetting) {
//		DISPLAY_HEIGHT = yMax;
//		DISPLAY_WIDTH = xMax;
		initializeScreenRecordingTools(recordAudioSetting);
		shareScreen();
	}

	@Override
	public void stopRecording() {
		stopScreenSharing();
		beginVideoSharing();
	}


	private void deleteTempFile() {

		File fdelete = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + SCREEN_SHARE_FILE_PATH);

		if (fdelete.exists()) {
			fdelete.delete();
		}

	}

	private void beginVideoSharing() {

		File dcim = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + SCREEN_SHARE_FILE_PATH);
		Uri videoUri = Uri.fromFile(dcim);

		if (ShareDialog.canShow(ShareLinkContent.class)) {

			ShareVideo shareVideo = new ShareVideo.Builder()
					.setLocalUrl(videoUri)
					.build();

			ShareVideoContent videoContent = new ShareVideoContent.Builder()
					.setVideo(shareVideo)
					.build();


			ShareDialog.show(this, videoContent);
		}

	}

	private void initializeFacebookSDK() {
		callbackManager = CallbackManager.Factory.create();
	}


	private VirtualDisplay createVirtualDisplay() {
		return mMediaProjection.createVirtualDisplay("AndroidLauncher",
				DISPLAY_WIDTH, DISPLAY_HEIGHT, mScreenDensity,
				DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
				mMediaRecorder.getSurface(), null /*Callbacks*/, null /*Handler*/);
	}

	private void initializeScreenRecordingTools(boolean recordAudioSetting) {

		DisplayMetrics metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		mScreenDensity = metrics.densityDpi;

		mMediaRecorder = new MediaRecorder();
		initRecorder(recordAudioSetting);
		prepareRecorder();

		mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

	}


	private void stopScreenSharing() {

		if (mVirtualDisplay == null) {
			return;
		}


		mVirtualDisplay.release();
		mMediaRecorder.release();

		// Preparing for any future recordings
		mMediaRecorder = new MediaRecorder();
	}


	private void shareScreen() {

		if (mMediaProjection == null) {
			startActivityForResult(mProjectionManager.createScreenCaptureIntent(), PERMISSION_CODE);
			return;
		}
		mVirtualDisplay = createVirtualDisplay();
		mMediaRecorder.start();
	}


	private void prepareRecorder() {
		try {
			mMediaRecorder.prepare();
		} catch (IllegalStateException e) {
			e.printStackTrace();
			finish();
		} catch (IOException e) {
			e.printStackTrace();
			finish();
		}
	}

	private void initRecorder(boolean recordAudioSetting) {

		if (recordAudioAccepted && recordAudioSetting) {
			mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		}

		mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
		mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

		if (recordAudioAccepted && recordAudioSetting) {
			mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
		}

		mMediaRecorder.setVideoEncodingBitRate(512 * 1000);
		mMediaRecorder.setVideoFrameRate(30);
		mMediaRecorder.setVideoSize(DISPLAY_WIDTH, DISPLAY_HEIGHT);
		mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory().getAbsolutePath() + SCREEN_SHARE_FILE_PATH);
	}


}
