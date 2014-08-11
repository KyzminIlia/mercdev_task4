package com.example.photo;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

public class PhotoActivity extends FragmentActivity {
	private SavePhotoDialog saveDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		if (getPhotoFragment() == null) {
			PhotoFragment photoFragment = new PhotoFragment();
			getSupportFragmentManager()
					.beginTransaction()
					.replace(android.R.id.content, photoFragment,
							PhotoFragment.FRAGMENT_TAG).commit();
		}
	}

	public PhotoFragment getPhotoFragment() {
		return (PhotoFragment) getSupportFragmentManager().findFragmentByTag(
				PhotoFragment.FRAGMENT_TAG);
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
