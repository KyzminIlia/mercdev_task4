package com.example.photo;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

public class SavePhotoDialog extends DialogFragment implements OnClickListener {
	public static final String DIALOG_TAG = SavePhotoDialog.class
			.getSimpleName();
	private EditText photoNameEdit;

	private Button buttonSave;
	private Button buttonCancel;

	@Override
	public void onDismiss(DialogInterface dialog) {
		super.onDismiss(dialog);
	}

	public String getPhotoName() {
		return photoNameEdit.getText().toString();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		setRetainInstance(true);
		super.onCreate(savedInstanceState);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		photoNameEdit = (EditText) view.findViewById(R.id.photo_name_edit);
		buttonCancel = (Button) view.findViewById(R.id.button_cancel);
		buttonSave = (Button) view.findViewById(R.id.button_save);
		buttonCancel.setOnClickListener(this);
		buttonSave.setOnClickListener(this);
		getDialog().setTitle(R.string.enter_filename);
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.d_save, null);
		return v;
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.button_cancel:
			dismiss();
			break;
		case R.id.button_save:
			if (!photoNameEdit.getText().equals("")) {
				Intent saveIntent = new Intent(PhotoFragment.ACTION_SAVE_PHOTO);
				saveIntent.putExtra(PhotoFragment.EXTRA_PHOTO_NAME,
						photoNameEdit.getText().toString());
				LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(
						saveIntent);
				dismiss();
			}
			break;
		default:
			dismiss();
			break;
		}

	}
}
