package com.example.photo;

import java.io.FileNotFoundException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class PhotoActivity extends FragmentActivity {
	PhotoFragment photoFragment = new PhotoFragment();
	SavePhotoDialog saveDialog;

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
