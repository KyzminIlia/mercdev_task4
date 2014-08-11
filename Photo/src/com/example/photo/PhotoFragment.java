package com.example.photo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.SensorManager;
import android.media.CamcorderProfile;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

public class PhotoFragment extends Fragment {
	public static final String FRAGMENT_TAG = PhotoFragment.class
			.getSimpleName();
	public static final String ACTION_SAVE_PHOTO = "com.example.photo.SAVE_PHOTO";
	public static final String EXTRA_PHOTO_NAME = "com.example.photo.PHOTO_NAME";
	FrameLayout photoView;
	ImageButton takePhotoButton;
	ImageButton takeVideoButton;
	MediaRecorder mediaRecorder;
	File videoFile;
	BroadcastReceiver photoSaveReceiver;
	Uri photoUri;
	File tempPhotoFile;
	WeakReference<Bitmap> resizedPhoto;
	Camera camera;
	CameraPreview preview;
	PictureCallback picture;
	ImageView photoPreview;
	boolean isPhotoTaken = false;
	boolean isStopped = false;
	boolean isRecordingVideo = false;
	String takePhotoButtonText;
	ImageButton changeCameraButton;
	Camera.CameraInfo currentCamInfo;
	Animation rotateAnimation;
	SensorManager sensorManager;
	int prevPitch = 0;
	int mFrameWidth;
	int mFrameHeight;
	List<Camera.Size> sizes;

	@Override
	public void onDestroy() {
		if (mediaRecorder != null) {
			mediaRecorder.stop();
			mediaRecorder.reset();
			mediaRecorder.release();
			mediaRecorder = null;
		}
		if (camera != null) {
			photoView.removeAllViews();
			preview.setCamera(null);
			camera.release();
			camera = null;
		}
		Log.d(FRAGMENT_TAG, "onDestroy");
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		Log.d(FRAGMENT_TAG, "onDestroyView");
		photoView.removeAllViews();
		super.onDestroyView();
	}

	@Override
	public void onStop() {
		isStopped = true;
		if (mediaRecorder != null) {
			mediaRecorder.stop();
			mediaRecorder.reset();
			mediaRecorder.release();
			mediaRecorder = null;
			takeVideoButton.setImageResource(R.drawable.video_camera_icon);
			takeVideoButton.setOnClickListener(new RecordVideo());
			takePhotoButton.setEnabled(true);
			changeCameraButton.setEnabled(true);
		}
		if (camera != null) {
			camera.release();
			camera = null;
			preview.setCamera(null);
		}

		Log.d(FRAGMENT_TAG, "onStop");
		super.onStop();
	}

	@Override
	public void onPause() {
		if (mediaRecorder != null) {
			mediaRecorder.stop();
			mediaRecorder.reset();
			mediaRecorder.release();
			mediaRecorder = null;
			takeVideoButton.setImageResource(R.drawable.video_camera_icon);
			takeVideoButton.setOnClickListener(new RecordVideo());
			takePhotoButton.setEnabled(true);
			changeCameraButton.setEnabled(true);
		}
		if (camera != null) {
			camera.release();
			camera = null;
			preview.setCamera(null);
		}
		super.onPause();
	}

	public void rotatePhoto() throws IOException {
		WeakReference<Bitmap> rotatedPhoto = null;
		BitmapDrawable photo = new BitmapDrawable(tempPhotoFile.getPath());
		DisplayMetrics metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay()
				.getMetrics(metrics);
		photo.setDither(false);
		photo.setTargetDensity(metrics);
		Display display = ((WindowManager) getActivity().getSystemService(
				Context.WINDOW_SERVICE)).getDefaultDisplay();
		int rotation = display.getRotation();
		Matrix matrix = new Matrix();
		switch (rotation) {
		case Surface.ROTATION_0:
			if (currentCamInfo.facing == CameraInfo.CAMERA_FACING_FRONT)
				matrix.postRotate((float) 270);
			else
				matrix.postRotate((float) 90);
			rotatedPhoto = new WeakReference<Bitmap>(Bitmap.createBitmap(
					photo.getBitmap(), 0, 0, photo.getBitmap().getWidth(),
					photo.getBitmap().getHeight(), matrix, true));
			Log.d(FRAGMENT_TAG, "0 degrees");
			break;
		case Surface.ROTATION_180:
			Log.d(FRAGMENT_TAG, "180 degrees");
			matrix.postRotate((float) 180);
			rotatedPhoto = new WeakReference<Bitmap>(Bitmap.createBitmap(
					photo.getBitmap(), 0, 0, photo.getBitmap().getWidth(),
					photo.getBitmap().getHeight(), matrix, true));
			break;
		case Surface.ROTATION_270:
			matrix.postRotate((float) 180);
			rotatedPhoto = new WeakReference<Bitmap>(Bitmap.createBitmap(
					photo.getBitmap(), 0, 0, photo.getBitmap().getWidth(),
					photo.getBitmap().getHeight(), matrix, true));
			Log.d(FRAGMENT_TAG, "270 degrees");
			break;
		case ExifInterface.ORIENTATION_NORMAL:
			Log.d(FRAGMENT_TAG, "Normal");
			if (currentCamInfo.facing == CameraInfo.CAMERA_FACING_FRONT)
				matrix.postRotate((float) 270);
			else
				matrix.postRotate((float) 0);
			rotatedPhoto = new WeakReference<Bitmap>(Bitmap.createBitmap(
					photo.getBitmap(), 0, 0, photo.getBitmap().getWidth(),
					photo.getBitmap().getHeight(), matrix, true));
			break;
		}
		FileOutputStream photoOutput = new FileOutputStream(
				tempPhotoFile.getPath());
		rotatedPhoto.get().compress(Bitmap.CompressFormat.JPEG, 100,
				photoOutput);
		photo = null;
		photoOutput.close();
	}

	public Uri getPhotoUri() {
		return photoUri;
	}

	private Camera.Size getBestPreviewSize(Camera.Parameters parameters) {
		Camera.Size bestSize = null;
		List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();

		bestSize = sizeList.get(0);

		for (int i = 1; i < sizeList.size(); i++) {
			if ((sizeList.get(i).width * sizeList.get(i).height) > (bestSize.width * bestSize.height)) {
				bestSize = sizeList.get(i);
			}
		}

		return bestSize;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		getActivity().setRequestedOrientation(
				ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		preview = new CameraPreview(getActivity());
		currentCamInfo = new Camera.CameraInfo();
		currentCamInfo.facing = CameraInfo.CAMERA_FACING_BACK;
		if (getActivity().getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			camera = Camera.open();
			preview.setCamera(camera);
			Log.d(FRAGMENT_TAG, "camera supported");
			Camera.Parameters params = camera.getParameters();
			sizes = params.getSupportedPreviewSizes();
			Display display = ((WindowManager) getActivity().getSystemService(
					Context.WINDOW_SERVICE)).getDefaultDisplay();
			DisplayMetrics metrics = new DisplayMetrics();
			display.getMetrics(metrics);
			mFrameWidth = (int) (getBestPreviewSize(params).width);
			mFrameHeight = (int) (getBestPreviewSize(params).height);
			preview.setLayoutParams(new LayoutParams((int) (params
					.getPreviewSize().width),
					(int) (params.getPreviewSize().height)));
			// params.setPreviewSize((int) (mFrameHeight), (int)
			// (mFrameHeight));
			params.set("cam_mode", 1);
			params.setZoom(0);
			params.setPictureSize(mFrameWidth, mFrameHeight);
			Log.d(FRAGMENT_TAG,
					"size: " + mFrameWidth + "("
							+ params.getPreviewSize().width + "):"
							+ mFrameHeight + "("
							+ params.getPreviewSize().height + ")"
							+ " with density " + metrics.density);
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
			camera.setParameters(params);
		}
		photoSaveReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				File photoDir = null;
				photoDir = Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
				if (!photoDir.exists())
					photoDir.mkdir();
				File photoFile = new File(
						Environment
								.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
						intent.getStringExtra(EXTRA_PHOTO_NAME) + ".jpeg");
				try {
					photoFile.createNewFile();
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				FileOutputStream photoOutput = null;
				try {
					photoOutput = new FileOutputStream(
							photoFile.getAbsoluteFile());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				WeakReference<Bitmap> photo = new WeakReference<Bitmap>(
						BitmapFactory.decodeFile(tempPhotoFile.getPath()));
				photo.get().compress(Bitmap.CompressFormat.JPEG, 100,
						photoOutput);
				photo = null;
				tempPhotoFile.delete();
				try {
					photoOutput.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};
		ShutterCallback shutter = new ShutterCallback() {

			@Override
			public void onShutter() {
				Toast.makeText(getActivity(), "Picture taken",
						Toast.LENGTH_SHORT);

			}
		};
		picture = new PictureCallback() {

			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				Log.d(FRAGMENT_TAG, "take a picture");
				tempPhotoFile = Environment.getExternalStorageDirectory();
				tempPhotoFile = new File(tempPhotoFile.getAbsolutePath()
						+ "/.temp/");
				if (!tempPhotoFile.exists())
					tempPhotoFile.mkdir();
				try {
					tempPhotoFile = File.createTempFile("pht", ".png",
							tempPhotoFile);
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					FileOutputStream photoOutput = new FileOutputStream(
							tempPhotoFile);
					photoOutput.write(data);
					DisplayMetrics metrics = new DisplayMetrics();
					getActivity().getWindowManager().getDefaultDisplay()
							.getMetrics(metrics);
					camera.stopPreview();
					photoPreview = new ImageView(getActivity());
					getActivity().setRequestedOrientation(
							ActivityInfo.SCREEN_ORIENTATION_SENSOR);
					rotatePhoto();
					BitmapDrawable photo = new BitmapDrawable(
							tempPhotoFile.getPath());
					photo.setDither(false);
					photo.setTargetDensity(metrics);
					photoPreview.setImageBitmap(photo.getBitmap());
					photoView.removeAllViews();
					photoView.addView(photoPreview);
					isPhotoTaken = true;
					photo = null;
					photoOutput.close();

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		};
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				photoSaveReceiver, new IntentFilter(ACTION_SAVE_PHOTO));
		setRetainInstance(true);
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onResume() {
		if (preview.getCamera() == null) {
			camera = Camera.open(currentCamInfo.facing);
			preview.setCamera(camera);
			if (!isPhotoTaken) {
				takePhotoButton.setOnClickListener(new TakePhoto());

			} else {
				takeVideoButton.setEnabled(false);
				changeCameraButton.setEnabled(false);
				takePhotoButton.setOnClickListener(new RetakePhoto());
			}

		}

		Log.d(FRAGMENT_TAG, "onResume");
		super.onResume();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		photoView = (FrameLayout) view.findViewById(R.id.photo_view);
		if (isRecordingVideo) {
			photoView.addView(preview);
		} else if (!isPhotoTaken) {
			photoView.addView(preview);
		} else {
			photoView.addView(photoPreview);
		}

		takePhotoButton = (ImageButton) view
				.findViewById(R.id.take_photo_button);
		changeCameraButton = (ImageButton) view
				.findViewById(R.id.change_camera_button);
		takeVideoButton = (ImageButton) view.findViewById(R.id.take_video);
		changeCameraButton.setOnClickListener(new ChangeToFrontCamera());
		takePhotoButton.setOnClickListener(new TakePhoto());
		takeVideoButton.setOnClickListener(new RecordVideo());
		takePhotoButton.getBackground().setColorFilter(
				new LightingColorFilter(0xFF0000, 0xFF0000));
		takeVideoButton.getBackground().setColorFilter(
				new LightingColorFilter(0xFF0000, 0xFF0000));
		changeCameraButton.getBackground().setColorFilter(
				new LightingColorFilter(0xFF0000, 0xFF0000));

		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.f_photo, null);
		return v;
	}

	class TakePhoto implements OnClickListener {

		@Override
		public void onClick(View v) {
			camera.takePicture(null, null, picture);
			takePhotoButton.setOnClickListener(new RetakePhoto());
			takeVideoButton.setEnabled(false);
			changeCameraButton.setEnabled(false);

		}
	}

	class RetakePhoto implements OnClickListener {

		@Override
		public void onClick(View v) {
			photoView.removeAllViews();
			takePhotoButton.setOnClickListener(new TakePhoto());
			isPhotoTaken = false;
			photoView.addView(preview);
			preview.setCamera(camera);
			getActivity().setRequestedOrientation(
					ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			takeVideoButton.setEnabled(true);
			changeCameraButton.setEnabled(true);

		}

	}

	class ChangeToFrontCamera implements OnClickListener {

		@Override
		public void onClick(View v) {
			if (!isPhotoTaken) {
				camera.stopPreview();
			}
			camera.release();
			Camera frontCamera = Camera
					.open(currentCamInfo.CAMERA_FACING_FRONT);
			currentCamInfo.facing = Camera.CameraInfo.CAMERA_FACING_FRONT;
			changeCameraButton.setOnClickListener(new ChangeToBackCamera());
			preview.setCamera(frontCamera);
			camera = frontCamera;

		}

	}

	class ChangeToBackCamera implements OnClickListener {

		@Override
		public void onClick(View v) {
			if (!isPhotoTaken) {
				camera.stopPreview();
			}
			camera.release();
			Camera backCamera = Camera.open(currentCamInfo.CAMERA_FACING_BACK);
			currentCamInfo.facing = Camera.CameraInfo.CAMERA_FACING_BACK;
			changeCameraButton.setOnClickListener(new ChangeToFrontCamera());
			preview.setCamera(backCamera);
			camera = backCamera;
		}

	}

	class RecordVideo implements OnClickListener {

		@Override
		public void onClick(View v) {
			isRecordingVideo = true;
			takeVideoButton
					.setImageResource(R.drawable.stop_recording_video_ico);
			mediaRecorder = new MediaRecorder();
			camera.stopPreview();
			camera.unlock();
			CamcorderProfile camProfile = CamcorderProfile
					.get(CamcorderProfile.QUALITY_HIGH);

			mediaRecorder.setCamera(camera);
			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
			mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			mediaRecorder.setProfile(camProfile);
			mediaRecorder.setPreviewDisplay(preview.getHolder().getSurface());
			videoFile = Environment
					.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
			if (!videoFile.exists()) {
				videoFile.mkdir();
			}
			String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
					.format(new Date());

			videoFile = new File(videoFile.getPath(), "video - " + timeStamp
					+ ".mp4");
			try {
				videoFile.createNewFile();
			} catch (IOException e1) {

				e1.printStackTrace();
			}
			mediaRecorder.setOutputFile(videoFile.getPath().toString());
			try {
				mediaRecorder.prepare();
				mediaRecorder.start();
				Log.d(FRAGMENT_TAG, "video record started");
			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {

				e.printStackTrace();
			}
			takeVideoButton.setOnClickListener(new StopVideo());
			takePhotoButton.setEnabled(false);
			changeCameraButton.setEnabled(false);
		}
	}

	class StopVideo implements OnClickListener {

		@Override
		public void onClick(View v) {
			try {
				mediaRecorder.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
			Log.d(FRAGMENT_TAG, "video record stopped");
			mediaRecorder.reset();
			mediaRecorder.release();
			mediaRecorder = null;
			takeVideoButton.setOnClickListener(new RecordVideo());
			takePhotoButton.setEnabled(true);
			takeVideoButton.setImageResource(R.drawable.video_camera_icon);
			changeCameraButton.setEnabled(true);
			isRecordingVideo = false;
			try {
				camera.reconnect();
				// camera.startPreview();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}
