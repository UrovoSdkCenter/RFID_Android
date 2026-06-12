package com.rfid.demo.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


import com.demo.rfid.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ReadFIleBinOrZip {
    private static String TAG = ReadFIleBinOrZip.class.getSimpleName();

    /**
     * 打开文件选择器
     */
    public static void openFileChooser(Activity activity,int requestCode) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // 设置文件类型过滤
        String[] mimeTypes = {"application/zip", "application/octet-stream"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

        try {
            activity.startActivityForResult(Intent.createChooser(intent, activity.getResources().getString(R.string.choice_bin_file)), requestCode);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(activity, activity.getResources().getString(R.string.please_install_filemanager), Toast.LENGTH_SHORT).show();
        }
    }




    public interface UnZipListener{
       void onStatus(int code,String errMsg);
       void onResult(List<String> pathList);
    }


    /**
     * 处理 ZIP 文件（解压并获取所有文件路径）
     */
    public static void handleZipFile(String zipFilePath,UnZipListener listener) {


        // 在后台线程中解压
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // 创建解压目录
                    File zipFile = new File(zipFilePath);
                    String unzipDir = zipFile.getParent() + File.separator +
                            zipFile.getName().replace(".zip", "") + "_unzipped";

                    // 解压文件
                    List<String> extractedFiles = unzipFile(zipFilePath, unzipDir);


                    if (extractedFiles != null && !extractedFiles.isEmpty()) {
                        if (listener!=null){
                            listener.onStatus(0,"success");
                            listener.onResult(extractedFiles);
                        }
                    } else {
                        callbackErr(listener,-2,"ZIP 文件解压失败或为空");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackErr(listener,-3,e.getMessage());
                }
            }
        }).start();
    }

    private static void callbackErr(UnZipListener listener,int code,String msg){
        if (listener!=null){
            listener.onStatus(code,msg);
        }
    }

    /**
     * 解压 ZIP 文件
     * @param zipFilePath ZIP 文件路径
     * @param destDir 解压目标目录
     * @return 解压后的文件绝对路径列表
     */
    private static List<String> unzipFile(String zipFilePath, String destDir) throws IOException {
        List<String> extractedFiles = new ArrayList<>();
        File destDirectory = new File(destDir);

        // 创建目标目录
        if (!destDirectory.exists()) {
            destDirectory.mkdirs();
        }

        ZipInputStream zipInputStream = null;
        try {
            FileInputStream fis = new FileInputStream(zipFilePath);
            zipInputStream = new ZipInputStream(fis);
            ZipEntry entry;

            while ((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                File entryFile = new File(destDir, entryName);

                if (entry.isDirectory()) {
                    // 创建目录
                    entryFile.mkdirs();
                } else {
                    // 确保父目录存在
                    File parent = entryFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    // 解压文件
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(entryFile);
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zipInputStream.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                        fos.flush();

                        // 添加到列表（只添加文件，不添加目录）
                        String absolutePath = entryFile.getAbsolutePath();
                        extractedFiles.add(absolutePath);
                        Log.i(TAG, "解压文件: " + absolutePath);
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                zipInputStream.closeEntry();
            }
        } finally {
            if (zipInputStream != null) {
                try {
                    zipInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return extractedFiles;
    }



    /**
     * 获取文件的绝对路径
     * @param uri 文件 URI
     * @return 文件的绝对路径
     */
    public static String getFileAbsolutePath(Context context,Uri uri) {
        if (uri == null) {
            return null;
        }

        String scheme = uri.getScheme();
        String path = null;

        if ("content".equalsIgnoreCase(scheme)) {
            // 处理 content:// URI
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                path = getPathFromDocumentUri(context,uri);
            }

            if (path == null) {
                path = getPathFromContentUri(context,uri);
            }
        } else if ("file".equalsIgnoreCase(scheme)) {
            // 处理 file:// URI
            path = uri.getPath();
        }

        return path;
    }

    /**
     * 从 Document URI 获取路径 (Android 4.4+)
     */
    private static String getPathFromDocumentUri(Context context,Uri uri) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return null;
        }

        try {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                String docId = DocumentsContract.getDocumentId(uri);
                String authority = uri.getAuthority();

                if ("com.android.externalstorage.documents".equals(authority)) {
                    String[] split = docId.split(":");
                    String type = split[0];
                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                } else if ("com.android.providers.downloads.documents".equals(authority)) {
                    // Downloads 目录
                    if (docId.startsWith("raw:")) {
                        return docId.replaceFirst("raw:", "");
                    }
                } else if ("com.android.providers.media.documents".equals(authority)) {
                    String[] split = docId.split(":");
                    String type = split[0];
                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }
                    if (contentUri != null) {
                        String selection = "_id=?";
                        String[] selectionArgs = new String[]{split[1]};
                        return getDataColumn(context,contentUri, selection, selectionArgs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 从 Content URI 获取路径
     */
    private static String getPathFromContentUri(Context context,Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * 从 Content URI 获取数据列
     */
    private static String getDataColumn(Context context,Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        String column = "_data";
        String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * 从文件路径读取字节数组
     */
    public static byte[] readFileBytesFromPath(String filePath) {
        if (TextUtils.isEmpty(filePath)){
            return null;
        }
        FileInputStream inputStream = null;
        java.io.ByteArrayOutputStream outputStream = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Log.e(TAG, "文件不存在: " + filePath);
                return null;
            }

            inputStream = new FileInputStream(file);
            outputStream = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "读取文件失败: " + filePath, e);
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "关闭流失败", e);
            }
        }
    }

    /**
     * 从 URI 直接读取文件字节数组（不依赖文件路径）
     * 适用于无法获取文件路径的情况，如通过"最近"入口选择的文件
     */
    public static byte[] readFileBytesFromUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        InputStream inputStream = null;
        java.io.ByteArrayOutputStream outputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Log.e(TAG, "无法打开 URI 输入流: " + uri);
                return null;
            }

            outputStream = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        } catch (IOException e) {
            Log.e(TAG, "从 URI 读取文件失败: " + uri, e);
            return null;
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "关闭流失败", e);
            }
        }
    }

    /**
     * 从 URI 获取文件名（用于判断文件类型）
     */
    public static String getFileNameFromUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }
        String fileName = null;
        String scheme = uri.getScheme();
        
        if ("content".equalsIgnoreCase(scheme)) {
            Cursor cursor = null;
            try {
                cursor = context.getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "获取文件名失败", e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        } else if ("file".equalsIgnoreCase(scheme)) {
            fileName = uri.getLastPathSegment();
        }
        
        // 如果仍然无法获取文件名，尝试从路径中提取
        if (TextUtils.isEmpty(fileName)) {
            String path = uri.getPath();
            if (!TextUtils.isEmpty(path)) {
                int lastIndex = path.lastIndexOf('/');
                if (lastIndex >= 0 && lastIndex < path.length() - 1) {
                    fileName = path.substring(lastIndex + 1);
                } else {
                    fileName = path;
                }
            }
        }
        
        return fileName;
    }

    /**
     * 处理 ZIP 文件（从 URI 直接解压）
     * @param context 上下文
     * @param zipUri ZIP 文件的 URI
     * @param listener 解压监听器
     */
    public static void handleZipFileFromUri(Context context, Uri zipUri, UnZipListener listener) {
        // 在后台线程中解压
        new Thread(new Runnable() {
            @Override
            public void run() {
                InputStream zipInputStream = null;
                try {
                    // 从 URI 打开输入流
                    zipInputStream = context.getContentResolver().openInputStream(zipUri);
                    if (zipInputStream == null) {
                        callbackErr(listener, -1, "无法打开 ZIP 文件输入流");
                        return;
                    }

                    // 创建临时解压目录
                    File cacheDir = context.getCacheDir();
                    String zipFileName = getFileNameFromUri(context, zipUri);
                    if (TextUtils.isEmpty(zipFileName)) {
                        zipFileName = "temp.zip";
                    }
                    String unzipDir = cacheDir.getAbsolutePath() + File.separator +
                            zipFileName.replace(".zip", "") + "_unzipped_" + System.currentTimeMillis();

                    // 解压文件
                    List<String> extractedFiles = unzipFileFromInputStream(zipInputStream, unzipDir);

                    if (extractedFiles != null && !extractedFiles.isEmpty()) {
                        if (listener != null) {
                            listener.onStatus(0, "success");
                            listener.onResult(extractedFiles);
                        }
                    } else {
                        callbackErr(listener, -2, "ZIP 文件解压失败或为空");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackErr(listener, -3, e.getMessage());
                } finally {
                    if (zipInputStream != null) {
                        try {
                            zipInputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }).start();
    }

    /**
     * 从输入流解压 ZIP 文件
     * @param zipInputStream ZIP 文件输入流
     * @param destDir 解压目标目录
     * @return 解压后的文件绝对路径列表
     */
    private static List<String> unzipFileFromInputStream(InputStream zipInputStream, String destDir) throws IOException {
        List<String> extractedFiles = new ArrayList<>();
        File destDirectory = new File(destDir);

        // 创建目标目录
        if (!destDirectory.exists()) {
            destDirectory.mkdirs();
        }

        ZipInputStream zis = null;
        try {
            zis = new ZipInputStream(zipInputStream);
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();
                File entryFile = new File(destDir, entryName);

                if (entry.isDirectory()) {
                    // 创建目录
                    entryFile.mkdirs();
                } else {
                    // 确保父目录存在
                    File parent = entryFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }

                    // 解压文件
                    FileOutputStream fos = null;
                    try {
                        fos = new FileOutputStream(entryFile);
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                        fos.flush();

                        // 添加到列表（只添加文件，不添加目录）
                        String absolutePath = entryFile.getAbsolutePath();
                        extractedFiles.add(absolutePath);
                        Log.i(TAG, "解压文件: " + absolutePath);
                    } finally {
                        if (fos != null) {
                            try {
                                fos.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                zis.closeEntry();
            }
        } finally {
            if (zis != null) {
                try {
                    zis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return extractedFiles;
    }
}
