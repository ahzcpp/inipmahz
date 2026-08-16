package com.hook.kuaishou;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Environment;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "KuaishouPermissionHook";
    private static final String TARGET_PACKAGE = "com.kuaishou.nebula";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(TARGET_PACKAGE)) {
            return;
        }

        XposedBridge.log(TAG + ": 开始Hook快手极速版");

        hookPermissionCheck(lpparam);
        hookPermissionRequest(lpparam);
        hookCameraAPIs(lpparam);
        hookAudioAPIs(lpparam);
        hookFileAPIs(lpparam);
        hookStorageAPIs(lpparam);
    }

    private void hookPermissionCheck(XC_LoadPackage.LoadPackageParam lpparam) {
        XposedHelpers.findAndHookMethod(
                Context.class,
                "checkSelfPermission",
                String.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String permission = (String) param.args[0];
                        if (permission.equals("android.permission.CAMERA") ||
                            permission.equals("android.permission.RECORD_AUDIO") ||
                            permission.equals("android.permission.READ_EXTERNAL_STORAGE") ||
                            permission.equals("android.permission.WRITE_EXTERNAL_STORAGE") ||
                            permission.equals("android.permission.MANAGE_EXTERNAL_STORAGE")) {
                            param.setResult(PackageManager.PERMISSION_GRANTED);
                            XposedBridge.log(TAG + ": 伪装权限检查 - " + permission);
                        }
                    }
                }
        );

        XposedHelpers.findAndHookMethod(
                Context.class,
                "checkPermission",
                String.class,
                int.class,
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        String permission = (String) param.args[0];
                        if (permission.equals("android.permission.CAMERA") ||
                            permission.equals("android.permission.RECORD_AUDIO") ||
                            permission.equals("android.permission.READ_EXTERNAL_STORAGE") ||
                            permission.equals("android.permission.WRITE_EXTERNAL_STORAGE") ||
                            permission.equals("android.permission.MANAGE_EXTERNAL_STORAGE")) {
                            param.setResult(PackageManager.PERMISSION_GRANTED);
                            XposedBridge.log(TAG + ": 伪装权限检查2 - " + permission);
                        }
                    }
                }
        );
    }

    private void hookPermissionRequest(XC_LoadPackage.LoadPackageParam lpparam) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            XposedHelpers.findAndHookMethod(
                    Activity.class,
                    "requestPermissions",
                    String[].class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String[] permissions = (String[]) param.args[0];
                            int requestCode = (int) param.args[1];
                            boolean intercept = false;
                            for (String perm : permissions) {
                                if (perm.equals("android.permission.CAMERA") ||
                                    perm.equals("android.permission.RECORD_AUDIO") ||
                                    perm.equals("android.permission.READ_EXTERNAL_STORAGE") ||
                                    perm.equals("android.permission.WRITE_EXTERNAL_STORAGE") ||
                                    perm.equals("android.permission.MANAGE_EXTERNAL_STORAGE") ||
                                    perm.equals("android.permission.READ_MEDIA_IMAGES") ||
                                    perm.equals("android.permission.READ_MEDIA_VIDEO") ||
                                    perm.equals("android.permission.READ_MEDIA_AUDIO")) {
                                    intercept = true;
                                    break;
                                }
                            }
                            if (intercept) {
                                XposedBridge.log(TAG + ": 拦截权限请求弹窗");
                                param.setResult(null);
                                Activity activity = (Activity) param.thisObject;
                                int[] grantResults = new int[permissions.length];
                                for (int i = 0; i < grantResults.length; i++) {
                                    grantResults[i] = PackageManager.PERMISSION_GRANTED;
                                }
                                try {
                                    activity.onRequestPermissionsResult(requestCode, permissions, grantResults);
                                } catch (Throwable t) {
                                    XposedBridge.log(TAG + ": 回调权限结果失败");
                                }
                            }
                        }
                    }
            );
        }

        try {
            Class<?> activityCompatClass = XposedHelpers.findClass("androidx.core.app.ActivityCompat", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(
                    activityCompatClass,
                    "requestPermissions",
                    Activity.class,
                    String[].class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Activity activity = (Activity) param.args[0];
                            String[] permissions = (String[]) param.args[1];
                            int requestCode = (int) param.args[2];
                            boolean intercept = false;
                            for (String perm : permissions) {
                                if (perm.equals("android.permission.CAMERA") ||
                                    perm.equals("android.permission.RECORD_AUDIO") ||
                                    perm.equals("android.permission.READ_EXTERNAL_STORAGE") ||
                                    perm.equals("android.permission.WRITE_EXTERNAL_STORAGE") ||
                                    perm.equals("android.permission.MANAGE_EXTERNAL_STORAGE") ||
                                    perm.equals("android.permission.READ_MEDIA_IMAGES") ||
                                    perm.equals("android.permission.READ_MEDIA_VIDEO") ||
                                    perm.equals("android.permission.READ_MEDIA_AUDIO")) {
                                    intercept = true;
                                    break;
                                }
                            }
                            if (intercept) {
                                XposedBridge.log(TAG + ": 拦截ActivityCompat权限请求");
                                param.setResult(null);
                                int[] grantResults = new int[permissions.length];
                                for (int i = 0; i < grantResults.length; i++) {
                                    grantResults[i] = PackageManager.PERMISSION_GRANTED;
                                }
                                try {
                                    activity.onRequestPermissionsResult(requestCode, permissions, grantResults);
                                } catch (Throwable t) {
                                    XposedBridge.log(TAG + ": ActivityCompat回调失败");
                                }
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": ActivityCompat类未找到");
        }
    }

    private void hookCameraAPIs(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                    CameraManager.class,
                    "getCameraIdList",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": 拦截获取摄像头列表");
                            return new String[0];
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    CameraManager.class,
                    "openCamera",
                    String.class,
                    android.hardware.camera2.CameraDevice.StateCallback.class,
                    android.os.Handler.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": 拦截打开摄像头");
                            throw new CameraAccessException(CameraAccessException.CAMERA_DISABLED, "摄像头已被禁用");
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    CameraManager.class,
                    "getCameraCharacteristics",
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": 拦截获取摄像头特性");
                            throw new CameraAccessException(CameraAccessException.CAMERA_DISABLED, "摄像头已被禁用");
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook摄像头API失败 - " + t.getMessage());
        }

        try {
            Class<?> cameraClass = XposedHelpers.findClass("android.hardware.Camera", lpparam.classLoader);
            XposedHelpers.findAndHookMethod(
                    cameraClass,
                    "open",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": 拦截Camera.open()");
                            throw new RuntimeException("摄像头不可用");
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    cameraClass,
                    "open",
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": 拦截Camera.open(int)");
                            throw new RuntimeException("摄像头不可用");
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    cameraClass,
                    "getNumberOfCameras",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": 返回摄像头数量为0");
                            return 0;
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook Camera类失败 - " + t.getMessage());
        }
    }

    private void hookAudioAPIs(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookConstructor(
                    AudioRecord.class,
                    int.class,
                    int.class,
                    int.class,
                    int.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": 拦截AudioRecord构造");
                            throw new IllegalStateException("麦克风不可用");
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    AudioRecord.class,
                    "startRecording",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": 拦截开始录音");
                            throw new IllegalStateException("麦克风不可用");
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook音频API失败 - " + t.getMessage());
        }
    }

    private void hookFileAPIs(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                    File.class,
                    "listFiles",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            File file = (File) param.thisObject;
                            String path = file.getAbsolutePath();
                            if (isRestrictedPath(path)) {
                                XposedBridge.log(TAG + ": 拦截访问敏感目录 - " + path);
                                param.setResult(new File[0]);
                            }
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    File.class,
                    "list",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            File file = (File) param.thisObject;
                            String path = file.getAbsolutePath();
                            if (isRestrictedPath(path)) {
                                XposedBridge.log(TAG + ": 拦截列出敏感目录 - " + path);
                                param.setResult(new String[0]);
                            }
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    File.class,
                    "exists",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            File file = (File) param.thisObject;
                            String path = file.getAbsolutePath();
                            if (isRestrictedPath(path)) {
                                XposedBridge.log(TAG + ": 隐藏敏感文件存在性 - " + path);
                                param.setResult(false);
                            }
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    File.class,
                    "canRead",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            File file = (File) param.thisObject;
                            String path = file.getAbsolutePath();
                            if (isRestrictedPath(path)) {
                                XposedBridge.log(TAG + ": 拦截读取权限检查 - " + path);
                                param.setResult(false);
                            }
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    File.class,
                    "canWrite",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            File file = (File) param.thisObject;
                            String path = file.getAbsolutePath();
                            if (isRestrictedPath(path)) {
                                XposedBridge.log(TAG + ": 拦截写入权限检查 - " + path);
                                param.setResult(false);
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook文件API失败 - " + t.getMessage());
        }
    }

    private void hookStorageAPIs(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                    Environment.class,
                    "getExternalStorageDirectory",
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            XposedBridge.log(TAG + ": 返回虚拟外部存储路径");
                            return new File("/data/user/0/" + TARGET_PACKAGE + "/files/virtual_storage");
                        }
                    }
            );

            XposedHelpers.findAndHookMethod(
                    Environment.class,
                    "getExternalStoragePublicDirectory",
                    String.class,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            String type = (String) param.args[0];
                            XposedBridge.log(TAG + ": 返回虚拟公共目录 - " + type);
                            return new File("/data/user/0/" + TARGET_PACKAGE + "/files/virtual_storage/" + type);
                        }
                    }
            );
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook存储API失败 - " + t.getMessage());
        }
    }

    private boolean isRestrictedPath(String path) {
        if (path == null) return false;
        
        String lowerPath = path.toLowerCase();
        return lowerPath.contains("/dcim") ||
               lowerPath.contains("/pictures") ||
               lowerPath.contains("/download") ||
               lowerPath.contains("/documents") ||
               lowerPath.contains("/movies") ||
               lowerPath.contains("/music") ||
               lowerPath.contains("/alarms") ||
               lowerPath.contains("/ringtones") ||
               lowerPath.contains("/notifications") ||
               (lowerPath.contains("/storage/emulated") && !lowerPath.contains(TARGET_PACKAGE));
    }
}