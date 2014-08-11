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

public class PhotoFragment extends Fragment implements OnClickListener {
	public static final String FRAGMENT_TAG = PhotoFragment.class
			.getSimpleName();
	public static final String ACTION_SAVE_PHOTO = "com.example.photo.SAVE_PHOTO";
	public static final String EXTRA_PHOTO_NAME = "com.example.photo.PHOTO_NAME";
	private FrameLayout photoView;
	private ImageButton takePhotoButton;
	private ImageButton takeVideoButton;
	private MediaRecorder mediaRecorder;
	private File videoFile;
	private BroadcastReceiver photoSaveReceiver;
	private File tempPhotoFile;
	private Camera camera;
	private CameraPreview preview;
	private PictureCallback picture;
	private ImageView photoPreview;
	private boolean isPhotoTaken = false;
	private boolean isRecordingVideo = false;
	private ImageButton changeCameraButton;
	private Camera.CameraInfo currentCamInfo;
	private int mFrameWidth;
	private int mFrameHeight;

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
	public void onPause() {
		if (mediaRecorder != null) {
			mediaRecorder.stop();
			mediaRecorder.reset();
			mediaRecorder.release();
			mediaRecorder = null;
			takeVideoButton.setImageResource(R.drawable.video_camera_icon);
			takeVideoButton.setOnClickListener(this);
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
		try {
			rotatedPhoto.get().compress(Bitmap.CompressFormat.JPEG, 100,
					photoOutput);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			photoOutput.close();
		}
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
			Display display = ((WindowManager) getActivity().getSystemService(
					Context.WINDOW_SERVICE)).getDefaultDisplay();
			DisplayMetrics metrics = new DisplayMetrics();
			display.getMetrics(metrics);
			preview.setLayoutParams(new LayoutParams((int) (params
					.getPreviewSize().width),
					(int) (params.getPreviewSize().height)));
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
				try {
					photo.get().compress(Bitmap.CompressFormat.JPEG, 100,
							photoOutput);

					tempPhotoFile.delete();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						photoOutput.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
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

				FileOutputStream photoOutput = null;
				try {
					photoOutput = new FileOutputStream(tempPhotoFile);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				}
				try {
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

				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						photoOutput.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
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
				takePhotoButton.setOnClickListener(this);

			} else {
				takeVideoButton.setEnabled(false);
				changeCameraButton.setEnabled(false);
				takePhotoButton.setOnClickListener(this);
			}

		}

		Log.d(FRAGMENT_TAG, "onResume");
		super.onResume();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {

		photoView = (FrameLayout) view.findViewById(R.id.photo_view);
		if (isRecordingVideo || !isPhotoTaken) {
			photoView.addView(preview);
		} else {
			photoView.addView(photoPreview);
		}

		takePhotoButton = (ImageButton) view
				.findViewById(R.id.take_photo_button);
		changeCameraButton = (ImageButton) view
				.findViewById(R.id.change_camera_button);
		takeVideoButton = (ImageButton) view.findViewById(R.id.take_video);
		changeCameraButton.setOnClickListener(this);
		takePhotoButton.setOnClickListener(this);
		takeVideoButton.setOnClickListener(this);
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

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.take_photo_button:
			if (!isPhotoTaken) {
				camera.takePicture(null, null, picture);
				takeVideoButton.setEnabled(false);
				changeCameraButton.setEnabled(false);
			} else {
				photoView.removeAllViews();
				isPhotoTaken = false;
				photoView.addView(preview);
				preview.setCamera(camera);
				getActivity().setRequestedOrientation(
						ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				takeVideoButton.setEnabled(true);
				changeCameraButton.setEnabled(true);
			}

			break;
		case R.id.change_camera_button:
			if (currentCamInfo.facing == CameraInfo.CAMERA_FACING_BACK) {
				if (!isPhotoTaken) {
					camera.stopPreview();
				}
				camera.release();
				Camera frontCamera = Camera.open(currentCamInfo.facing);
				currentCamInfo.facing = Camera.CameraInfo.CAMERA_FACING_FRONT;
				preview.setCamera(frontCamera);
				camera = frontCamera;

			} else {

				if (!isPhotoTaken) {
					camera.stopPreview();
				}
				camera.release();
				Camera backCamera = Camera.open(currentCamInfo.facing);
				currentCamInfo.facing = Camera.CameraInfo.CAMERA_FACING_BACK;
				preview.setCamera(backCamera);
				camera = backCamera;
			}
			break;
		case R.id.take_video:
			if (isRecordingVideo) {
				try {
					mediaRecorder.stop();
				} catch (Exception e) {
					e.printStackTrace();
				}
				Log.d(FRAGMENT_TAG, "video record stopped");
				mediaRecorder.reset();
				mediaRecorder.release();
				mediaRecorder = null;
				takePhotoButton.setEnabled(true);
				takeVideoButton.setImageResource(R.drawable.video_camera_icon);
				changeCameraButton.setEnabled(true);
				isRecordingVideo = false;
				try {
					camera.reconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				isRecordingVideo = true;
				takeVideoButton
						.setImageResource(R.drawable.stop_recording_video_ico);
				mediaRecorder = new MediaRecorder();
				camera.stopPreview();
				camera.unlock();
				CamcorderProfile camProfile = CamcorderProfile
						.get(CamcorderProfile.QUALITY_HIGH);

				mediaRecorder.setCamera(camera);
				mediaRecorder
						.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
				mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
				mediaRecorder.setProfile(camProfile);
				mediaRecorder.setPreviewDisplay(preview.getHolder()
						.getSurface());
				videoFile = Environment
						.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);
				if (!videoFile.exists()) {
					videoFile.mkdir();
				}
				String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
						.format(new Date());

				videoFile = new File(videoFile.getPath(), "video - "
						+ timeStamp + ".mp4");
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
				takePhotoButton.setEnabled(false);
				changeCameraButton.setEnabled(false);
			}

			break;
		}

	}
}
