/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.usbcameratest;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IButtonCallback;
import com.serenegiant.usb.IStatusCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.UVCCameraTextureView;

import org.tensorflow.lite.examples.classification.R;

import java.nio.ByteBuffer;

public final class UVCCameraFragment extends Fragment implements CameraDialog.CameraDialogParent {
    
    private final Object mSync = new Object();
    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private UVCCameraTextureView mUVCCameraView;
    // for open&start / stop&close camera preview
    private ImageButton mCameraButton;
    private Surface mPreviewSurface;
    private Handler handler;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.uvc_camera_preview, null);
        handler = new Handler();
        mCameraButton = (ImageButton) rootView.findViewById(R.id.camera_button);
        mCameraButton.setOnClickListener(mOnClickListener);
        
        mUVCCameraView = (UVCCameraTextureView) rootView.findViewById(R.id.UVCCameraTextureView1);
        mUVCCameraView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);
        
        mUSBMonitor = new USBMonitor(getActivity(), mOnDeviceConnectListener);
        
        return rootView;
    }
    
    
    @Override
    public void onStart() {
        super.onStart();
        mUSBMonitor.register();
        synchronized (mSync) {
            if (mUVCCamera!=null) {
                mUVCCamera.startPreview();
            }
        }
    }
    
    @Override
    public void onStop() {
        synchronized (mSync) {
            if (mUVCCamera!=null) {
                mUVCCamera.stopPreview();
            }
            if (mUSBMonitor!=null) {
                mUSBMonitor.unregister();
            }
        }
        super.onStop();
    }
    
    @Override
    public void onDestroy() {
        synchronized (mSync) {
            releaseCamera();
            if (mToast!=null) {
                mToast.cancel();
                mToast = null;
            }
            if (mUSBMonitor!=null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }
        }
        mUVCCameraView = null;
        mCameraButton = null;
        super.onDestroy();
    }
    
    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            synchronized (mSync) {
                if (mUVCCamera==null) {
                    CameraDialog.showDialog(getActivity(), UVCCameraFragment.this);
                } else {
                    releaseCamera();
                }
            }
        }
    };
    
    private Toast mToast;
    
    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(getActivity(), "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            releaseCamera();
            
            handler.post(new Runnable() {
                @Override
                public void run() {
                    final UVCCamera camera = new UVCCamera();
                    camera.open(ctrlBlock);
                    camera.setStatusCallback(new IStatusCallback() {
                        @Override
                        public void onStatus(final int statusClass, final int event, final int selector,
                                             final int statusAttribute, final ByteBuffer data) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    final Toast toast = Toast.makeText(getActivity(), "onStatus(statusClass=" + statusClass
                                                                                              + "; " +
                                                                                              "event=" + event + "; " +
                                                                                              "selector=" + selector + "; " +
                                                                                              "statusAttribute=" + statusAttribute + "; " +
                                                                                              "data=...)", Toast.LENGTH_SHORT);
                                    synchronized (mSync) {
                                        if (mToast!=null) {
                                            mToast.cancel();
                                        }
                                        toast.show();
                                        mToast = toast;
                                    }
                                }
                            });
                        }
                    });
                    camera.setButtonCallback(new IButtonCallback() {
                        @Override
                        public void onButton(final int button, final int state) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    final Toast toast = Toast.makeText(getActivity(), "onButton(button=" + button + "; " +
                                                                                              "state=" + state + ")", Toast.LENGTH_SHORT);
                                    synchronized (mSync) {
                                        if (mToast!=null) {
                                            mToast.cancel();
                                        }
                                        mToast = toast;
                                        toast.show();
                                    }
                                }
                            });
                        }
                    });
                    camera.setFrameCallback(frame -> {
                        final Bitmap bitmap = mUVCCameraView.captureStillImage();
                        ((UVCCameraActivity) getActivity()).onUVCCameraPreviewAvailable(bitmap, bitmap.getHeight(), bitmap.getWidth());
                        
                    }, UVCCamera.PIXEL_FORMAT_RGB565);
//					camera.setPreviewTexture(camera.getSurfaceTexture());
                    if (mPreviewSurface!=null) {
                        mPreviewSurface.release();
                        mPreviewSurface = null;
                    }
                    try {
                        camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
                    } catch (final IllegalArgumentException e) {
                        // fallback to YUV mode
                        try {
                            camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
                        } catch (final IllegalArgumentException e1) {
                            camera.destroy();
                            return;
                        }
                    }
                    final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
                    if (st!=null) {
                        mPreviewSurface = new Surface(st);
                        camera.setPreviewDisplay(mPreviewSurface);
//						camera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGB565/*UVCCamera.PIXEL_FORMAT_NV21*/);
                        camera.startPreview();
                    }
                    synchronized (mSync) {
                        mUVCCamera = camera;
                    }
                }
            });
        }
        
        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            // XXX you should check whether the coming device equal to camera device that currently using
            releaseCamera();
        }
        
        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(getActivity(), "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }
        
        @Override
        public void onCancel(final UsbDevice device) {
        }
    };
    
    private synchronized void releaseCamera() {
        synchronized (mSync) {
            if (mUVCCamera!=null) {
                try {
                    mUVCCamera.setStatusCallback(null);
                    mUVCCamera.setButtonCallback(null);
                    mUVCCamera.close();
                    mUVCCamera.destroy();
                } catch (final Exception e) {
                    //
                }
                mUVCCamera = null;
            }
            if (mPreviewSurface!=null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }
        }
    }
    
    /**
     * to access from CameraDialog
     *
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }
    
    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    // FIXME
                }
            });
        }
    }
    
    // if you need frame data as byte array on Java side, you can use this callback method with UVCCamera#setFrameCallback
    // if you need to create Bitmap in IFrameCallback, please refer following snippet.
/*	final Bitmap bitmap = Bitmap.createBitmap(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, Bitmap.Config.RGB_565);
	private final IFrameCallback mIFrameCallback = new IFrameCallback() {
		@Override
		public void onFrame(final ByteBuffer frame) {
			frame.clear();
			synchronized (bitmap) {
				bitmap.copyPixelsFromBuffer(frame);
			}
			mImageView.post(mUpdateImageTask);
		}
	};
	
	private final Runnable mUpdateImageTask = new Runnable() {
		@Override
		public void run() {
			synchronized (bitmap) {
				mImageView.setImageBitmap(bitmap);
			}
		}
	}; */
}
