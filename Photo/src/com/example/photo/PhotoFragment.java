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
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Sensor;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Images.Media;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.BoringLayout.Metrics;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;

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
	String takePhotoButtonText;
	ImageButton changeCameraButton;
	Camera.CameraInfo currentCamInfo;
	Animation rotateAnimation;
	SensorManager sensorManager;
	int prevPitch = 0;

	@Override
	public void onStop() {
		if (mediaRecorder != null && !isPhotoTaken) {
			mediaRecorder.stop();
			mediaRecorder.reset();
			mediaRecorder.release();
		}
		photoView.removeAllViews();
		preview.setCamera(null);
		camera.release();
		super.onStop();
	}

	public Bitmap rotatePhoto(Bitmap photo) throws IOException {
		WeakReference<Bitmap> rotatedPhoto = null;
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
			rotatedPhoto = new WeakReference<Bitmap>(Bitmap.createBitmap(photo,
					0, 0, photo.getWidth(), photo.getHeight(), matrix, true));
			Log.d(FRAGMENT_TAG, "0 degrees");
			break;
		case Surface.ROTATION_180:
			Log.d(FRAGMENT_TAG, "180 degrees");
			matrix.postRotate((float) 180);
			rotatedPhoto = new WeakReference<Bitmap>(Bitmap.createBitmap(photo,
					0, 0, photo.getWidth(), photo.getHeight(), matrix, true));
			break;
		case Surface.ROTATION_270:
			matrix.postRotate((float) 180);
			rotatedPhoto = new WeakReference<Bitmap>(Bitmap.createBitmap(photo,
					0, 0, photo.getWidth(), photo.getHeight(), matrix, true));
			Log.d(FRAGMENT_TAG, "270 degrees");
			break;
		case ExifInterface.ORIENTATION_NORMAL:
			Log.d(FRAGMENT_TAG, "Normal");
			rotatedPhoto = new WeakReference<Bitmap>(photo);
			break;
		}
		FileOutputStream photoOutput = new FileOutputStream(
				tempPhotoFile.getPath());
		rotatedPhoto.get().compress(Bitmap.CompressFormat.JPEG, 100,
				photoOutput);
		photoOutput.close();
		return rotatedPhoto.get();
	}

	public Uri getPhotoUri() {
		return photoUri;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		getActivity().setRequestedOrientation(
				ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		currentCamInfo = new Camera.CameraInfo();
		currentCamInfo.facing = CameraInfo.CAMERA_FACING_BACK;
		if (getActivity().getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			camera = Camera.open();
			preview = new CameraPreview(getActivity());
			preview.setCamera(camera);
			Log.d(FRAGMENT_TAG, "camera supported");
			Camera.Parameters params = camera.getParameters();
			List<Camera.Size> sizes = params.getSupportedPreviewSizes();
			Display display = ((WindowManager) getActivity().getSystemService(
					Context.WINDOW_SERVICE)).getDefaultDisplay();
			DisplayMetrics metrics = new DisplayMetrics();
			display.getMetrics(metrics);

			int mFrameWidth = (int) (sizes.get(0).width * metrics.density);
			int mFrameHeight = (int) (sizes.get(0).height * metrics.density);

			preview.setLayoutParams(new LayoutParams(mFrameWidth, mFrameHeight));
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
					BitmapDrawable photo = new BitmapDrawable(
							tempPhotoFile.getPath());
					DisplayMetrics metrics = new DisplayMetrics();
					getActivity().getWindowManager().getDefaultDisplay()
							.getMetrics(metrics);
					photo.setDither(false);
					photo.setTargetDensity(metrics);
					camera.stopPreview();
					photoPreview = new ImageView(getActivity());
					getActivity().setRequestedOrientation(
							ActivityInfo.SCREEN_ORIENTATION_SENSOR);
					photoPreview.setImageBitmap(rotatePhoto(photo.getBitmap()));
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
		if (preview != null)
			if (preview.getCamera() == null) {
				camera = Camera.open(currentCamInfo.facing);
				if (!isPhotoTaken) {
					takePhotoButton.setOnClickListener(new TakePhoto());
					preview.setCamera(camera);
				} else {
					takeVideoButton.setEnabled(false);
					changeCameraButton.setEnabled(false);
					takePhotoButton.setOnClickListener(new RetakePhoto());
				}
			}

		super.onResume();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		photoView = (FrameLayout) view.findViewById(R.id.photo_view);

		if (!isPhotoTaken) {
			photoView.addView(preview);
		} else
			photoView.addView(photoPreview);
		takePhotoButton = (ImageButton) view
				.findViewById(R.id.take_photo_button);
		changeCameraButton = (ImageButton) view
				.findViewById(R.id.change_camera_button);
		takeVideoButton = (ImageButton) view.findViewById(R.id.take_video);
		changeCameraButton.setOnClickListener(new ChangeToFrontCamera());
		takePhotoButton.setOnClickListener(new TakePhoto());
		takeVideoButton.setOnClickListener(new RecordVideo());
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
			mediaRecorder = new MediaRecorder();
			camera.stopPreview();
			camera.unlock();
			mediaRecorder.setCamera(camera);
			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
			mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
			mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
			mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
			mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
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
			mediaRecorder.stop();
			mediaRecorder.reset();
			mediaRecorder.release();
			mediaRecorder = null;
			takeVideoButton.setOnClickListener(new RecordVideo());
			takePhotoButton.setEnabled(true);
			changeCameraButton.setEnabled(true);
			try {
				camera.reconnect();
				camera.startPreview();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
}
