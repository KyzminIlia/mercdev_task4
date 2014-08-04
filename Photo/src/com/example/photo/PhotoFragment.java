package com.example.photo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

public class PhotoFragment extends Fragment {
	private final int CAMERA_RESULT = 17;
	public static final String FRAGMENT_TAG = PhotoFragment.class
			.getSimpleName();
	public static final String ACTION_SAVE_PHOTO = "com.example.photo.SAVE_PHOTO";
	public static final String EXTRA_PHOTO_NAME = "com.example.photo.PHOTO_NAME";
	FrameLayout photoView;
	Button takePhotoButton;
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

	@Override
	public void onStop() {
		photoView.removeAllViews();
		preview.setCamera(null);
		takePhotoButtonText = takePhotoButton.getText().toString();
		camera.release();
		super.onStop();
	}

	public void setPhoto() throws IOException {
		DisplayMetrics metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay()
				.getMetrics(metrics);
		Point displaySize = new Point();
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		display.getSize(displaySize);
		int displayHeight = (int) (displaySize.y * metrics.density);
		int displayWidth = (int) (displaySize.x * metrics.density);
		Options options = new BitmapFactory.Options();
		options.inScaled = false;
		options.inDither = false;
		options.inPreferredConfig = Bitmap.Config.ARGB_8888;
		ContentResolver contentResolver = getActivity().getContentResolver();
		WeakReference<Bitmap> photo = new WeakReference<Bitmap>(
				MediaStore.Images.Media.getBitmap(contentResolver, photoUri));
		WeakReference<Bitmap> rotatedPhoto = null;
		ExifInterface exif = new ExifInterface(photoUri.getPath().toString());
		int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
				ExifInterface.ORIENTATION_NORMAL);
		Matrix matrix = new Matrix();
		switch (orientation) {
		case ExifInterface.ORIENTATION_ROTATE_90:
			matrix.postRotate((float) 90);
			rotatedPhoto = new WeakReference<Bitmap>(Bitmap.createBitmap(photo
					.get(), 0, 0, photo.get().getWidth(), photo.get()
					.getHeight(), matrix, true));
			Log.d(FRAGMENT_TAG, "90 degrees");
			break;
		case ExifInterface.ORIENTATION_ROTATE_180:
			Log.d(FRAGMENT_TAG, "180 degrees");
			matrix.postRotate((float) 180);
			rotatedPhoto = new WeakReference<Bitmap>(Bitmap.createBitmap(photo
					.get(), 0, 0, photo.get().getWidth(), photo.get()
					.getHeight(), matrix, true));
			break;
		case ExifInterface.ORIENTATION_ROTATE_270:
			matrix.postRotate((float) 270);
			rotatedPhoto = new WeakReference<Bitmap>(Bitmap.createBitmap(photo
					.get(), 0, 0, photo.get().getWidth(), photo.get()
					.getHeight(), matrix, true));
			Log.d(FRAGMENT_TAG, "270 degrees");
			break;
		case ExifInterface.ORIENTATION_NORMAL:
			Log.d(FRAGMENT_TAG, "Normal");
			rotatedPhoto = photo;
			break;
		}
		FileOutputStream photoOutput = new FileOutputStream(photoUri.getPath());
		rotatedPhoto.get().compress(Bitmap.CompressFormat.JPEG, 100,
				photoOutput);
		WeakReference<Bitmap> preparedPhoto = new WeakReference<Bitmap>(
				BitmapFactory.decodeFile(photoUri.getPath(), options));
		resizedPhoto = new WeakReference<Bitmap>(Bitmap.createScaledBitmap(
				preparedPhoto.get(), displayWidth, displayHeight, false));
		// photoView.setImageBitmap(resizedPhoto.get());
	}

	public Uri getPhotoUri() {
		return photoUri;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (getActivity().getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_CAMERA)) {
			camera = Camera.open();
			preview = new CameraPreview(getActivity());
			preview.setCamera(camera);
			Log.d(FRAGMENT_TAG, "camera supported");
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
					BitmapDrawable background = new BitmapDrawable(
							tempPhotoFile.getPath());
					DisplayMetrics metrics = new DisplayMetrics();
					getActivity().getWindowManager().getDefaultDisplay()
							.getMetrics(metrics);
					background.setDither(false);
					background.setTargetDensity(metrics);
					// background.setGravity(Gravity.CENTER);
					camera.stopPreview();
					photoPreview = new ImageView(getActivity());
					photoPreview.setImageBitmap(background.getBitmap());
					photoView.removeAllViews();
					photoView.addView(photoPreview);
					isPhotoTaken = true;
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
				camera = Camera.open();
				if (!isPhotoTaken) {
					takePhotoButton.setOnClickListener(new TakePhoto());
					preview.setCamera(camera);
					takePhotoButton.setText(takePhotoButtonText);
				} else {
					takePhotoButton.setOnClickListener(new RetakePhoto());
					takePhotoButton.setText(takePhotoButtonText);
				}
			}
		super.onResume();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		photoView = (FrameLayout) view.findViewById(R.id.photo_view);
		if (!isPhotoTaken)
			photoView.addView(preview);
		else
			photoView.addView(photoPreview);
		takePhotoButton = (Button) view.findViewById(R.id.take_photo_button);
		takePhotoButton.setOnClickListener(new TakePhoto());
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
			takePhotoButton.setText(getString(R.string.retake_photo));
		}
	}

	class RetakePhoto implements OnClickListener {

		@Override
		public void onClick(View v) {
			photoView.removeAllViews();
			takePhotoButton.setOnClickListener(new TakePhoto());
			takePhotoButton.setText(getString(R.string.take_photo));
			isPhotoTaken = false;
			photoView.addView(preview);
			preview.setCamera(camera);

		}

	}
}
