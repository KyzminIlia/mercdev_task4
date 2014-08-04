package com.example.photo;

import java.io.IOException;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.Camera.Parameters;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback, SensorEventListener {
	private SurfaceHolder holder;
	private Camera camera;
	Sensor orientationSensor;
	SensorManager sensManager;
	float curOrientation;

	public Camera getCamera() {
		return camera;
	}

	public CameraPreview(Context context) {
		super(context);
		holder = getHolder();
		holder.addCallback(this);
		holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	}

	public void setCamera(Camera camera) {
		if (this.camera == camera)
			return;
		if (this.camera != null) {
			this.camera.stopPreview();
			this.camera.release();
		}
		this.camera = camera;
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		sensManager = (SensorManager) getContext().getSystemService(
				Context.SENSOR_SERVICE);
		sensManager.registerListener(this, orientationSensor,
				SensorManager.SENSOR_DELAY_NORMAL);
		if (camera != null)
			try {
				camera.setPreviewDisplay(holder);
			} catch (IOException e) {
				e.printStackTrace();
			}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {

	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		if (holder.getSurface() == null) {
			return;
		}

		try {
			camera.stopPreview();
		} catch (Exception e) {
		}

		try {

			orientationSensor = sensManager
					.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			int orientation = getResources().getConfiguration().orientation;
			Parameters params = camera.getParameters();
			switch (orientation) {
			case Configuration.ORIENTATION_PORTRAIT:
				camera.setDisplayOrientation((int) curOrientation);
				break;
			case Configuration.ORIENTATION_LANDSCAPE:
				camera.setDisplayOrientation((int) curOrientation);
				break;

			}
			params.setPreviewSize(camera.getParameters()
					.getSupportedPreviewSizes().get(0).width, camera
					.getParameters().getSupportedPreviewSizes().get(0).height);
			camera.setParameters(params);
			camera.startPreview();
		} catch (Exception e) {
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		curOrientation = event.values[1];

	}
}
