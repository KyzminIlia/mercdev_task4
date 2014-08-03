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
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Point;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

public class PhotoFragment extends Fragment {
	private final int CAMERA_RESULT = 17;
	public static final String FRAGMENT_TAG = PhotoFragment.class
			.getSimpleName();
	public static final String ACTION_SAVE_PHOTO = "com.example.photo.SAVE_PHOTO";
	public static final String EXTRA_PHOTO_NAME = "com.example.photo.PHOTO_NAME";
	public static final String EXTRA_URI = "com.example.photo.URI";
	ImageView photoView;
	Button takePhotoButton;
	BroadcastReceiver photoSaveReceiver;
	Uri photoUri;
	File tempPhotoFile;
	WeakReference<Bitmap> resizedPhoto;

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public void setPhoto() throws IOException {
		Display display = getActivity().getWindowManager().getDefaultDisplay();
		Point displaySize = new Point();
		display.getSize(displaySize);
		int displayHeight = displaySize.y;
		int displayWidth = displaySize.x;
		ContentResolver contentResolver = getActivity().getContentResolver();
		WeakReference<Bitmap> photo = new WeakReference<Bitmap>(
				MediaStore.Images.Media.getBitmap(contentResolver, photoUri));
		WeakReference<Bitmap> rotatedPhoto;
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
			resizedPhoto = new WeakReference<Bitmap>(Bitmap.createScaledBitmap(
					rotatedPhoto.get(), displayWidth, displayHeight, false));
			photoView.setImageBitmap(resizedPhoto.get());
			Log.d(FRAGMENT_TAG, "90 degrees");
			break;
		case ExifInterface.ORIENTATION_ROTATE_180:
			Log.d(FRAGMENT_TAG, "180 degrees");
			matrix.postRotate((float) 180);
			rotatedPhoto = new WeakReference<Bitmap>(Bitmap.createBitmap(photo
					.get(), 0, 0, photo.get().getWidth(), photo.get()
					.getHeight(), matrix, true));
			resizedPhoto = new WeakReference<Bitmap>(Bitmap.createScaledBitmap(
					rotatedPhoto.get(), displayWidth, displayHeight, false));
			photoView.setImageBitmap(resizedPhoto.get());
			break;
		case ExifInterface.ORIENTATION_ROTATE_270:
			matrix.postRotate((float) 270);
			rotatedPhoto = new WeakReference<Bitmap>(Bitmap.createBitmap(photo
					.get(), 0, 0, photo.get().getWidth(), photo.get()
					.getHeight(), matrix, true));
			resizedPhoto = new WeakReference<Bitmap>(Bitmap.createScaledBitmap(
					rotatedPhoto.get(), displayWidth, displayHeight, false));
			photoView.setImageBitmap(resizedPhoto.get());
			Log.d(FRAGMENT_TAG, "270 degrees");
			break;
		case ExifInterface.ORIENTATION_NORMAL:
			Log.d(FRAGMENT_TAG, "Normal");
			resizedPhoto = new WeakReference<Bitmap>(Bitmap.createScaledBitmap(
					photo.get(), displayWidth, displayHeight, false));
			photoView.setImageBitmap(resizedPhoto.get());
			break;
		}

	}

	public Uri getPhotoUri() {
		return photoUri;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		photoSaveReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
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
				ContentResolver contentResolver = getActivity()
						.getContentResolver();
				WeakReference<Bitmap> photo = null;
				try {
					photo = new WeakReference<Bitmap>(
							MediaStore.Images.Media.getBitmap(contentResolver,
									photoUri));
				} catch (FileNotFoundException e) {

					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				photo.get().compress(Bitmap.CompressFormat.JPEG, 100,
						photoOutput);
				photo = null;
				tempPhotoFile.delete();

			}
		};
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				photoSaveReceiver, new IntentFilter(ACTION_SAVE_PHOTO));
		setRetainInstance(true);
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		photoView = (ImageView) view.findViewById(R.id.photo_view);
		takePhotoButton = (Button) view.findViewById(R.id.take_photo_button);
		takePhotoButton.setOnClickListener(new TakePhoto());
		if (resizedPhoto != null)
			photoView.setImageBitmap(resizedPhoto.get());
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
			Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			tempPhotoFile = Environment.getExternalStorageDirectory();
			tempPhotoFile = new File(tempPhotoFile.getAbsolutePath()
					+ "/.temp/");
			if (!tempPhotoFile.exists())
				tempPhotoFile.mkdir();
			try {
				tempPhotoFile = File.createTempFile("pht", ".png",
						tempPhotoFile);
				tempPhotoFile.delete();
			} catch (IOException e) {
				e.printStackTrace();
			}
			photoUri = Uri.fromFile(tempPhotoFile);
			cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
			getActivity().startActivityForResult(cameraIntent, CAMERA_RESULT);
		}
	}
}
