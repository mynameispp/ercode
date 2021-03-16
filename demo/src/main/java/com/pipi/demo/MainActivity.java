package com.pipi.demo;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pipi.scancode.activity.CaptureActivity;
import com.pipi.scancode.encoding.EncodingHandler;
import com.pipi.scancode.tools.VersionUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.SoftReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private TextView result;//扫码结果
    private EditText inputContent;//输入内容
    private ImageView codeImage;//生成内容的二维码

    private String SAVE_PIC_PATH;//保存到SD卡
    private Bitmap erCodeBitmap;//生成的二维码图
    private MyHandler myHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        result = findViewById(R.id.scan_code_result);
        inputContent = findViewById(R.id.create_code_ed);
        codeImage = findViewById(R.id.create_code_img);
//        codeImage.setImageBitmap(BitmapFactory.decodeFile("/storage/emulated/0/DCIM/Camera/1615876167868.jpg"));
        findViewById(R.id.scan_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.VIBRATE, Manifest.permission.READ_EXTERNAL_STORAGE
                        , Manifest.permission.WRITE_EXTERNAL_STORAGE};
                //申请权限，其中RC_PERMISSION是权限申请码，用来标志权限申请的
                ActivityCompat.requestPermissions(MainActivity.this, permissions, 100);
            }
        });
        findViewById(R.id.create_code_img_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //创建二维码
                String content = inputContent.getText().toString().trim();
                if (TextUtils.isEmpty(content)) {
                    Toast.makeText(view.getContext(), "内容不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }
                erCodeBitmap = EncodingHandler.createQRCodeBitmap(content, 150, 150, "utf-8"
                        , "H", "1", Color.BLUE, Color.WHITE);
                codeImage.setImageBitmap(erCodeBitmap);

            }
        });

        findViewById(R.id.save_create_code_img_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                //保存创建二维码
                if (null == myHandler) {
                    myHandler = new MyHandler(new SoftReference<Context>(view.getContext()));
                }
//                SAVE_PIC_PATH = view.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath()
//                        + "/ercode/" + System.currentTimeMillis() + ".jpg";

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            SAVE_PIC_PATH = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath() + "/";
                            if (VersionUtils.isTargetQ(view.getContext())) {
                                saveBitmap(view.getContext(), erCodeBitmap, System.currentTimeMillis() + ".jpg");
                            } else {
                                saveFile(erCodeBitmap, SAVE_PIC_PATH, System.currentTimeMillis() + ".jpg");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            myHandler.sendEmptyMessage(-1);
                        }
                    }
                }).start();

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100 && grantResults.length == 4
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED
                && grantResults[2] == PackageManager.PERMISSION_GRANTED
                && grantResults[3] == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(new Intent(this, MyCaptureActivity.class), 1);
        } else {
            result.setText("获取权限失败");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == CaptureActivity.RESULT_CODE_QR_SCAN && data != null && data.getExtras() != null) {
            result.setText(data.getExtras().getString(CaptureActivity.INTENT_EXTRA_KEY_QR_SCAN));
        }
    }

    public void saveFile(Bitmap bitmap, String path, String bitName) throws Exception {
        File file = new File(path, bitName);
        file.mkdirs();
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            if (bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)) {
                out.flush();
                out.close();
                //保存图片后发送广播通知更新数据库
                // Uri uri = Uri.fromFile(file);
                // sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri uri = Uri.fromFile(file);
                intent.setData(uri);
                sendBroadcast(intent);
                myHandler.sendEmptyMessage(1);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveBitmap(Context context, Bitmap bitmap, String bitName) {

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DESCRIPTION, "二维码");
        values.put(MediaStore.Images.Media.DISPLAY_NAME, bitName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpg");
        values.put(MediaStore.Images.Media.TITLE, bitName);
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/Camera");

        Uri external = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        ContentResolver resolver = context.getContentResolver();

        Uri insertUri = resolver.insert(external, values);
        OutputStream os = null;
        if (insertUri != null) {
            try {
                os = resolver.openOutputStream(insertUri);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, os);
                myHandler.sendEmptyMessage(1);
            } catch (IOException e) {
                e.printStackTrace();
                myHandler.sendEmptyMessage(-1);
            } finally {
                try {
                    if (os != null) {
                        os.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    static class MyHandler extends Handler {
        SoftReference<Context> contextSoftReference;

        public MyHandler(SoftReference<Context> contextSoftReference) {
            super();
            this.contextSoftReference = contextSoftReference;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                Toast.makeText(contextSoftReference.get(), "保存成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(contextSoftReference.get(), "无法保存当前图", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != myHandler) {
            myHandler.removeCallbacksAndMessages(null);
            myHandler = null;
        }
        if (erCodeBitmap != null) {
            erCodeBitmap.recycle();
            erCodeBitmap = null;
        }
    }
}
