package com.secondaryscreen.app;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SlaveActivity extends AppCompatActivity {
    private static String TAG = "SlaveActivity";
    private TextureView mTextureView;
    private ControlConnection mControlConnection;
    private VideoConnection mVideoConnection;
    private DisplayConnection mDisplayConnection;
    private int mRotation = -1;
    private float mRealScaleX;
    private float mRealScaleY;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }
        if (getActionBar() != null) {
            getActionBar().hide();
        }
        setContentView(R.layout.activity_slave);

        mVideoConnection = new VideoConnection();
        mControlConnection = new ControlConnection();
        mDisplayConnection = new DisplayConnection();

        DisplayManager dm = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);
        dm.registerDisplayListener(mDisplayListener, null);

        TextView titleTextView = findViewById(R.id.secondaryscreen_title);
        titleTextView.bringToFront();
        titleTextView.append(" " + Resolution.R.toSimpleString());

        mTextureView = findViewById(R.id.secondaryscreen_texture);
        mTextureView.setPivotX(0);
        mTextureView.setPivotY(0);
        mTextureView.setOpaque(false);
        mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        mTextureView.setOnTouchListener((view, event) -> {
            Log.i(TAG, "onTouch:" + event);

            event.setLocation(event.getX()/mRealScaleX, event.getY()/mRealScaleY);

            Utils.offerMotionEvent(event, false);
            return true;
        });

        setRect();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
        mDisplayConnection.shutdown();
        mVideoConnection.shutdown();
        mControlConnection.shutdown();
        DisplayManager dm = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);
        dm.unregisterDisplayListener(mDisplayListener);
    }

    private final TextureView.SurfaceTextureListener mSurfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                    Log.i(TAG, "onSurfaceTextureAvailable width:" + width + " height:" + height);

                    mVideoConnection.start(new Surface(surfaceTexture), mDisplayConnection);
                    mControlConnection.start();
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                    // Log.v(TAG, "onSurfaceTextureDestroyed");
                    return true;
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                    // Log.i(TAG, "onSurfaceTextureSizeChanged width:" + width + " height:" + height);
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                    // Log.v(TAG, "onSurfaceTextureUpdated");
                }
            };

    private final DisplayManager.DisplayListener mDisplayListener =
            new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    if (displayId == 0) {
                        setRect();
                    }
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }
            };

    private void setRect() {
        Display display = getWindowManager().getDefaultDisplay();

        int rotation = display.getRotation();
        if (rotation != mRotation) {
            mDisplayConnection.setScreenInfo(1,
                    Resolution.R.VIRTUALDISPLAY_WIDTH,
                    Resolution.R.VIRTUALDISPLAY_HEIGHT,
                    Resolution.R.VIRTUALDISPLAY_DENSITYDPI,
                    rotation);

            if ((mRotation + rotation) % 2 != 0 || mRotation == -1) {
                if (rotation % 2 == 0) {
                    mTextureView.getLayoutParams().width = Resolution.R.TEXTUREVIEW_WIDTH;
                    mTextureView.getLayoutParams().height = Resolution.R.TEXTUREVIEW_HEIGHT;

                    mRealScaleX = Resolution.R.SCALE_X;
                    mRealScaleY = Resolution.R.SCALE_Y;
                } else {
                    mTextureView.getLayoutParams().width = Resolution.R.TEXTUREVIEW_HEIGHT;
                    mTextureView.getLayoutParams().height = Resolution.R.TEXTUREVIEW_WIDTH;

                    mRealScaleX = Resolution.R.SCALE_Y;
                    mRealScaleY = Resolution.R.SCALE_X;
                }
                mTextureView.requestLayout();
                Log.i(TAG, "rotation:" + mRotation);
                Log.i(TAG, "TextureView width:" + mTextureView.getLayoutParams().width + " height:" + mTextureView.getLayoutParams().height);
                Log.i(TAG, "mRealScaleX:" + mRealScaleX + " mRealScaleY:" + mRealScaleY);
            }

            mRotation = rotation;
        }
    }
}
