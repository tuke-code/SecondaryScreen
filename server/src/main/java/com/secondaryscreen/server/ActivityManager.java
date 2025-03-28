package com.secondaryscreen.server;

import android.annotation.SuppressLint;
import android.app.IActivityController;
import android.app.ITaskStackListener;
import android.app.TaskInfo;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressLint("PrivateApi,DiscouragedPrivateApi")
public final class ActivityManager {
    public static final String TAG = "ActivityManager";
    private final IInterface mManager;

    public static class StackInfo {
        public int displayId;
        public List<String> taskNames;
    }

    static ActivityManager create() {
        try {
            // On old Android versions, the ActivityManager is not exposed via AIDL,
            // so use ActivityManagerNative.getDefault()
            Class<?> cls = Class.forName("android.app.ActivityManagerNative");
            Method getDefaultMethod = cls.getDeclaredMethod("getDefault");
            IInterface am = (IInterface) getDefaultMethod.invoke(null);
            return new ActivityManager(am);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private ActivityManager(IInterface manager) {
        this.mManager = manager;
    }

    @SuppressWarnings("ConstantConditions")
    public int startActivity(Intent intent, Bundle bOptions) {
        try {
            Class<?> iApplicationThreadClass = Class.forName("android.app.IApplicationThread");
            Class<?> profilerInfo = Class.forName("android.app.ProfilerInfo");
            Method method = mManager.getClass()
                    .getMethod("startActivityAsUser", iApplicationThreadClass, String.class, Intent.class, String.class, IBinder.class, String.class,
                            int.class, int.class, profilerInfo, Bundle.class, int.class);

            return (int) method.invoke(
                    /* this */ mManager,
                    /* caller */ null,
                    /* callingPackage */ Utils.PACKAGE_NAME,
                    /* intent */ intent,
                    /* resolvedType */ null,
                    /* resultTo */ null,
                    /* resultWho */ null,
                    /* requestCode */ 0,
                    /* startFlags */ 0,
                    /* profilerInfo */ null,
                    /* bOptions */ bOptions,
                    /* userId */ /* UserHandle.USER_CURRENT */ -2);
        } catch (Throwable e) {
            Ln.e(TAG, "Could not invoke method", e);
            return -1;
        }
    }

    public List<TaskInfo> getTasks(int maxNum) throws ReflectiveOperationException {
        // List<ActivityManager.RunningTaskInfo> getTasks(int maxNum);
        // android.app.TaskInfo是android.app.ActivityManager.RunningTaskInfo的父类
        Method method = mManager.getClass().getMethod("getTasks", int.class);
        return (List<TaskInfo>)method.invoke(mManager, maxNum);
    }

    @SuppressLint("BlockedPrivateApi")
    public List<StackInfo> getAllTaskInfos() throws ReflectiveOperationException {
        List<StackInfo> ret = null;
        try {
            // Android 12+
            // List<ActivityTaskManager.RootTaskInfo> getAllRootTaskInfos();
            Method method = mManager.getClass().getMethod("getAllRootTaskInfos");
            List<Object> list = (List<Object>)method.invoke(mManager);
            Class<?> cls = Class.forName("android.app.ActivityTaskManager$RootTaskInfo");
            ret = new ArrayList<>(list.size());
            for (Object info : list) {
                // displayId在RootTaskInfo的父类TaskInfo中定义
                // android.app.TaskInfo.displayId
                StackInfo stackInfo = new StackInfo();
                stackInfo.displayId = cls.getField("displayId").getInt(info);
                String[] taskNames = (String[]) cls.getDeclaredField("childTaskNames").get(info);
                stackInfo.taskNames = Arrays.asList(taskNames);
                ret.add(stackInfo);
            }
        } catch (NoSuchMethodException e) {
            Ln.w(TAG, "NoSuchMethodException getAllRootTaskInfos");
            try {
                // Android 10 ~ 11
                // List<ActivityManager.StackInfo> getAllStackInfos();
                Method method = mManager.getClass().getMethod("getAllStackInfos");
                List<Object> list =  (List<Object>)method.invoke(mManager);
                Class<?> cls = Class.forName("android.app.ActivityManager$StackInfo");
                ret = new ArrayList<>(list.size());
                for (Object info : list) {
                    StackInfo stackInfo = new StackInfo();
                    stackInfo.displayId = cls.getDeclaredField("displayId").getInt(info);
                    String[] taskNames = (String[])cls.getDeclaredField("taskNames").get(info);
                    stackInfo.taskNames = Arrays.asList(taskNames);
                    ret.add(stackInfo);
                }
            } catch (NoSuchMethodException e1) {
                Ln.w(TAG, "NoSuchMethodException getAllStackInfos");
            }
        }
        return ret;
    }

    public void setActivityController(IActivityController.Stub stub) {
        try {
            Method method = mManager.getClass().getMethod("setActivityController", IActivityController.class, boolean.class);
            method.invoke(mManager, stub, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerTaskStackListener(ITaskStackListener listener) {
        try {
            mManager.getClass().getMethod("registerTaskStackListener", ITaskStackListener.class).invoke(mManager, listener);
        } catch (Exception e) {
            Ln.e(TAG, "Could not register task stack listener", e);
        }
    }
    public void unregisterTaskStackListener(ITaskStackListener listener) {
        try {
            mManager.getClass().getMethod("unregisterTaskStackListener", ITaskStackListener.class).invoke(mManager, listener);
        } catch (Exception e) {
            Ln.e(TAG, "Could not register task stack listener", e);
        }
    }
}
