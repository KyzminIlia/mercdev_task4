package com.example.photo;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class CameraPreview extends SurfaceView implements
		SurfaceHolder.Callback {
	private SurfaceHolder holder;
	private Camera camera;

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
		if (camera != null) {

			Camera.Parameters params = this.camera.getParameters();
			List<Size> sizes = params.getSupportedPreviewSizes();
			Size optimalSize = getOptimalPreviewSize(sizes, getResources()
					.getDisplayMetrics().widthPixels, getResources()
					.getDisplayMetrics().heightPixels);
			params.setPreviewSize(optimalSize.width, optimalSize.height);
			requestLayout();
			this.camera.setParameters(params);
			if (this.camera != null) {
				try {
					this.camera.setPreviewDisplay(holder);
				} catch (IOException e) {
					e.printStackTrace();
				}

				this.camera.startPreview();
			}
		}
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		/*
		 * try { if (camera != null) camera.setPreviewDisplay(holder);
		 * 
		 * } catch (IOException e) {
		 * 
		 * e.printStackTrace(); }
		 */

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub

	}

	private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.05;
		double targetRatio = (double) w / h;

		if (sizes == null)
			return null;

		Size optimalSize = null;

		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Find size
		for (Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
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
			camera.setPreviewDisplay(holder);
			camera.startPreview();

		} catch (Exception e) {
		}
	}
}
