package com.example.photo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
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
	ImageView photoView;
	Button takePhotoButton;
	Bitmap photo;
	BroadcastReceiver photoSaveReceiver;

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
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				FileOutputStream photoOutput = null;
				try {
					photoOutput = new FileOutputStream(
							photoFile.getAbsoluteFile());
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				photo.compress(Bitmap.CompressFormat.JPEG, 100, photoOutput);

			}
		};
		LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
				photoSaveReceiver, new IntentFilter(ACTION_SAVE_PHOTO));
		setRetainInstance(true);
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onResume() {
		if (photo != null)
			photoView.setImageBitmap(photo);
		super.onResume();
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
		photoView = (ImageView) view.findViewById(R.id.photo_view);
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

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == CAMERA_RESULT) {
			photo = (Bitmap) data.getExtras().get("data");
			photoView.setImageBitmap(photo);
			Log.d(FRAGMENT_TAG, "take a photo with size " + photo.getWidth()
					+ "/" + photo.getHeight() + " ");
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	class TakePhoto implements OnClickListener {

		@Override
		public void onClick(View v) {
			Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			startActivityForResult(cameraIntent, CAMERA_RESULT);

		}
	}
}
