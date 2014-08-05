package com.example.photo;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;

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
		this.camera = camera;
		if (this.camera != null) {
			try {
				this.camera.setPreviewDisplay(holder);
				Display display = ((WindowManager) getContext()
						.getSystemService(Context.WINDOW_SERVICE))
						.getDefaultDisplay();
				int rotation = display.getRotation();
				Parameters params = camera.getParameters();
				switch (rotation) {
				case Surface.ROTATION_0:
					camera.setDisplayOrientation(90);
					break;
				case Surface.ROTATION_270:
					camera.setDisplayOrientation(180);
					break;
				case Surface.ROTATION_180:
					camera.setDisplayOrientation(180);
					break;

				}
				params.setPreviewSize(camera.getParameters()
						.getSupportedPreviewSizes().get(0).width,
						camera.getParameters().getSupportedPreviewSizes()
								.get(0).width);
				camera.setParameters(params);
				this.camera.startPreview();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
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

			Display display = ((WindowManager) getContext().getSystemService(
					Context.WINDOW_SERVICE)).getDefaultDisplay();
			int rotation = display.getRotation();
			Parameters params = camera.getParameters();
			switch (rotation) {
			case Surface.ROTATION_0:
				camera.setDisplayOrientation(90);
				break;
			case Surface.ROTATION_270:
				camera.setDisplayOrientation(180);
				break;
			case Surface.ROTATION_180:
				camera.setDisplayOrientation(180);
				break;

			}
			params.setPreviewSize(camera.getParameters()
					.getSupportedPreviewSizes().get(0).width, camera
					.getParameters().getSupportedPreviewSizes().get(0).width);
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
