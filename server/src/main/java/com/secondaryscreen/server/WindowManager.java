package com.secondaryscreen.server;

import android.os.IInterface;

import java.lang.reflect.Method;

public final class WindowManager {
    private static String TAG = "WindowManager";
    private final IInterface mManager;
    private Method mGetRotationMethod;
    private RotationListener mRotationWatcher;

    private Method mFreezeDisplayRotationMethod;
    private int mFreezeDisplayRotationMethodVersion;

    private Method mIsDisplayRotationFrozenMethod;
    private int mIsDisplayRotationFrozenMethodVersion;

    private Method mThawDisplayRotationMethod;
    private int mThawDisplayRotationMethodVersion;

    private Method mShouldShowImeMethod;
    private int mShouldShowImeMethodVersion;

    private Method mSetShouldShowImeMethod;
    private int mSetShouldShowImeMethodVersion;

    private Method mShouldShowSystemDecorsMethod;

    private Method mSetShouldShowSystemDecorsMethod;

    public interface RotationListener {
        void onRotationChanged(int rotation);
    }

    static WindowManager create() {
        IInterface manager = ServiceManager.getService("window", "android.view.IWindowManager");
        return new WindowManager(manager);
    }

    private WindowManager(IInterface manager) {
        this.mManager = manager;
    }

    private Method getGetRotationMethod() throws NoSuchMethodException {
        if (mGetRotationMethod == null) {
            Class<?> cls = mManager.getClass();
            try {
                // method changed since this commit:
                // https://android.googlesource.com/platform/frameworks/base/+/8ee7285128c3843401d4c4d0412cd66e86ba49e3%5E%21/#F2
                mGetRotationMethod = cls.getMethod("getDefaultDisplayRotation");
            } catch (NoSuchMethodException e) {
                // old version
                mGetRotationMethod = cls.getMethod("getRotation");
            }
        }
        return mGetRotationMethod;
    }

    private Method getFreezeDisplayRotationMethod() throws NoSuchMethodException {
        if (mFreezeDisplayRotationMethod == null) {
            try {
                // Android 15 preview and 14 QPR3 Beta added a String caller parameter for debugging:
                // <https://android.googlesource.com/platform/frameworks/base/+/670fb7f5c0d23cf51ead25538bcb017e03ed73ac%5E%21/>
                mFreezeDisplayRotationMethod = mManager.getClass().getMethod("freezeDisplayRotation", int.class, int.class, String.class);
                mFreezeDisplayRotationMethodVersion = 0;
            } catch (NoSuchMethodException e) {
                try {
                    // New method added by this commit:
                    // <https://android.googlesource.com/platform/frameworks/base/+/90c9005e687aa0f63f1ac391adc1e8878ab31759%5E%21/>
                    mFreezeDisplayRotationMethod = mManager.getClass().getMethod("freezeDisplayRotation", int.class, int.class);
                    mFreezeDisplayRotationMethodVersion = 1;
                } catch (NoSuchMethodException e1) {
                    mFreezeDisplayRotationMethod = mManager.getClass().getMethod("freezeRotation", int.class);
                    mFreezeDisplayRotationMethodVersion = 2;
                }
            }
        }
        return mFreezeDisplayRotationMethod;
    }

    private Method getIsDisplayRotationFrozenMethod() throws NoSuchMethodException {
        if (mIsDisplayRotationFrozenMethod == null) {
            try {
                // New method added by this commit:
                // <https://android.googlesource.com/platform/frameworks/base/+/90c9005e687aa0f63f1ac391adc1e8878ab31759%5E%21/>
                mIsDisplayRotationFrozenMethod = mManager.getClass().getMethod("isDisplayRotationFrozen", int.class);
                mIsDisplayRotationFrozenMethodVersion = 0;
            } catch (NoSuchMethodException e) {
                mIsDisplayRotationFrozenMethod = mManager.getClass().getMethod("isRotationFrozen");
                mIsDisplayRotationFrozenMethodVersion = 1;
            }
        }
        return mIsDisplayRotationFrozenMethod;
    }

    private Method getThawDisplayRotationMethod() throws NoSuchMethodException {
        if (mThawDisplayRotationMethod == null) {
            try {
                // Android 15 preview and 14 QPR3 Beta added a String caller parameter for debugging:
                // <https://android.googlesource.com/platform/frameworks/base/+/670fb7f5c0d23cf51ead25538bcb017e03ed73ac%5E%21/>
                mThawDisplayRotationMethod = mManager.getClass().getMethod("thawDisplayRotation", int.class, String.class);
                mThawDisplayRotationMethodVersion = 0;
            } catch (NoSuchMethodException e) {
                try {
                    // New method added by this commit:
                    // <https://android.googlesource.com/platform/frameworks/base/+/90c9005e687aa0f63f1ac391adc1e8878ab31759%5E%21/>
                    mThawDisplayRotationMethod = mManager.getClass().getMethod("thawDisplayRotation", int.class);
                    mThawDisplayRotationMethodVersion = 1;
                } catch (NoSuchMethodException e1) {
                    mThawDisplayRotationMethod = mManager.getClass().getMethod("thawRotation");
                    mThawDisplayRotationMethodVersion = 2;
                }
            }
        }
        return mThawDisplayRotationMethod;
    }

    public int getRotation() {
        try {
            Method method = getGetRotationMethod();
            return (int) method.invoke(mManager);
        } catch (ReflectiveOperationException e) {
            Ln.w(TAG, "Could not invoke method", e);
            return 0;
        }
    }

    public void freezeRotation(int displayId, int rotation) {
        try {
            Method method = getFreezeDisplayRotationMethod();
            switch (mFreezeDisplayRotationMethodVersion) {
                case 0:
                    method.invoke(mManager, displayId, rotation, "scondaryscreen#freezeRotation");
                    break;
                case 1:
                    method.invoke(mManager, displayId, rotation);
                    break;
                default:
                    if (displayId != 0) {
                        Ln.w(TAG, "Secondary display rotation not supported on this device");
                        return;
                    }
                    method.invoke(mManager, rotation);
                    break;
            }
        } catch (ReflectiveOperationException e) {
            Ln.w(TAG, "Could not invoke method", e);
        }
    }

    public boolean isRotationFrozen(int displayId) {
        try {
            Method method = getIsDisplayRotationFrozenMethod();
            switch (mIsDisplayRotationFrozenMethodVersion) {
                case 0:
                    return (boolean) method.invoke(mManager, displayId);
                default:
                    if (displayId != 0) {
                        Ln.w(TAG, "Secondary display rotation not supported on this device");
                        return false;
                    }
                    return (boolean) method.invoke(mManager);
            }
        } catch (ReflectiveOperationException e) {
            Ln.w(TAG, "Could not invoke method", e);
            return false;
        }
    }

    public void thawRotation(int displayId) {
        try {
            Method method = getThawDisplayRotationMethod();
            switch (mThawDisplayRotationMethodVersion) {
                case 0:
                    method.invoke(mManager, displayId, "scondaryscreen#thawRotation");
                    break;
                case 1:
                    method.invoke(mManager, displayId);
                    break;
                default:
                    if (displayId != 0) {
                        Ln.w(TAG, "Secondary display rotation not supported on this device");
                        return;
                    }
                    method.invoke(mManager);
                    break;
            }
        } catch (ReflectiveOperationException e) {
            Ln.w(TAG, "Could not invoke method", e);
        }
    }

    public void setRotationListener(RotationListener rotationListener) {
        mRotationWatcher = rotationListener;
    }

    public void onRotationChanged(int rotation) {
        if (mRotationWatcher != null) {
            mRotationWatcher.onRotationChanged(rotation);
        }
    }

    private Method getShouldShowImeMethod() throws NoSuchMethodException {
        if (mShouldShowImeMethod == null) {
            try {
                mShouldShowImeMethod = mManager.getClass().getMethod("getDisplayImePolicy", int.class);
                mShouldShowImeMethodVersion = 0;
            } catch (NoSuchMethodException e) {
                mShouldShowImeMethod = mManager.getClass().getMethod("shouldShowIme", int.class);
                mShouldShowImeMethodVersion = 1;
            }
        }
        return mShouldShowImeMethod;
    }

    public boolean shouldShowIme(int displayId) {
        try {
            Method method = getShouldShowImeMethod();
            switch (mShouldShowImeMethodVersion) {
                case 0:
                    int policy = (int) method.invoke(mManager, displayId);
                    Ln.w(TAG, "shouldShowIme:" + policy);
                    return policy == 0;
                default:
                    return (boolean) method.invoke(mManager, displayId);
            }
        } catch (ReflectiveOperationException e) {
            Ln.w(TAG, "Could not invoke method", e);
            return false;
        }
    }

    private Method getSetShouldShowImeMethod() throws NoSuchMethodException {
        if (mSetShouldShowImeMethod == null) {
            try {
                mSetShouldShowImeMethod = mManager.getClass().getMethod("setDisplayImePolicy", int.class, int.class);
                mSetShouldShowImeMethodVersion = 0;
            } catch (NoSuchMethodException e) {
                mSetShouldShowImeMethod = mManager.getClass().getMethod("setShouldShowIme", int.class, boolean.class);
                mSetShouldShowImeMethodVersion = 1;
            }
        }
        return mSetShouldShowImeMethod;
    }

    public void setShouldShowIme(int displayId, boolean shouldShow) {
        try {
            Method method = getSetShouldShowImeMethod();
            switch (mSetShouldShowImeMethodVersion) {
                case 0:
                    int policy = shouldShow ? 0 : 1;
                    method.invoke(mManager, displayId, policy);
                    break;
                default:
                    method.invoke(mManager, displayId, shouldShow);
            }
        } catch (ReflectiveOperationException e) {
            Ln.w(TAG, "Could not invoke method", e);
        }
    }

    private Method getShouldShowSystemDecorsMethod() throws NoSuchMethodException {
        if (mShouldShowSystemDecorsMethod == null) {
            mShouldShowSystemDecorsMethod = mManager.getClass().getMethod("shouldShowSystemDecors", int.class);
        }
        return mShouldShowSystemDecorsMethod;
    }

    public boolean shouldShowSystemDecors(int displayId)  {
        try {
            Method method = getShouldShowSystemDecorsMethod();
            return (boolean)method.invoke(mManager, displayId);
        } catch (ReflectiveOperationException e) {
            Ln.w(TAG, "Could not invoke method", e);
            return false;
        }
    }


    private Method getSetShouldShowSystemDecorsMethod() throws NoSuchMethodException {
        if (mSetShouldShowSystemDecorsMethod == null) {
            mSetShouldShowSystemDecorsMethod = mManager.getClass().getMethod("setShouldShowSystemDecors", int.class, boolean.class);
        }
        return mSetShouldShowSystemDecorsMethod;
    }

    public void setShouldShowSystemDecors(int displayId, boolean shouldShow)  {
        try {
            Method method = getSetShouldShowSystemDecorsMethod();
            method.invoke(mManager, displayId, shouldShow);
        } catch (ReflectiveOperationException e) {
            Ln.w(TAG, "Could not invoke method", e);
        }
    }
}
