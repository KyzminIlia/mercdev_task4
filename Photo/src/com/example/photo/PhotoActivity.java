package com.example.photo;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class PhotoActivity extends FragmentActivity {
	PhotoFragment photoFragment = new PhotoFragment();
	SavePhotoDialog saveDialog;
	private final int CAMERA_RESULT = 17;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		photoFragment = (PhotoFragment) getSupportFragmentManager()
				.findFragmentByTag(PhotoFragment.FRAGMENT_TAG);
		if (photoFragment == null) {
			photoFragment = new PhotoFragment();
			getSupportFragmentManager()
					.beginTransaction()
					.replace(android.R.id.content, photoFragment,
							PhotoFragment.FRAGMENT_TAG).commit();
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == CAMERA_RESULT)
			if (resultCode == Activity.RESULT_OK) {
				try {
					photoFragment.setPhoto();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.save_photo_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		saveDialog = new SavePhotoDialog();
		saveDialog
				.show(getSupportFragmentManager(), SavePhotoDialog.DIALOG_TAG);
		return true;
	}
}
