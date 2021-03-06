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
		SurfaceHolder.Callback {
	private SurfaceHolder holder;
	private Camera camera;
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
				this.camera.startPreview();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		if (camera != null)
			try {
				camera.setPreviewDisplay(holder);
				camera.startPreview();
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
			Parameters params = camera.getParameters();
			List<Size> supportedSizes = params.getSupportedPreviewSizes();
			Size bestSize = supportedSizes.get(0);
			for (int i = 1; i < supportedSizes.size(); i++) {
				if ((supportedSizes.get(i).width * supportedSizes.get(i).height) > (bestSize.width * bestSize.height)) {
					bestSize = supportedSizes.get(i);
				}
			}
			params.setPictureSize(params.getPreviewSize().width,
					params.getPreviewSize().height);
			camera.setParameters(params);
			camera.startPreview();
		} catch (Exception e) {
		}
	}

}
