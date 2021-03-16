package com.pipi.demo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pipi.scancode.activity.CaptureActivity;
import com.pipi.scancode.tools.EaseFileUtils;
import com.pipi.scancode.tools.VersionUtils;
import com.pipi.scancode.view.ViewfinderView;

import java.io.File;

import androidx.annotation.Nullable;

public class MyCaptureActivity extends CaptureActivity implements SurfaceHolder.Callback {
    private ImageView back;
    private TextView scanner_toolbar_album;

    @Override
    public int getContentView() {
        return R.layout.activity_scancode;
    }

    @Override
    public void inittView() {
        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_content);
        this.back = (ImageView) findViewById(R.id.scanner_toolbar_back);
        scanner_toolbar_album = (TextView) findViewById(R.id.scanner_toolbar_album);
        this.back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                MyCaptureActivity.this.finish();
            }
        });
        scanner_toolbar_album.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //本地图片，打开本地相册，建议扫码是让用户裁剪二维码
                Intent intent = null;
                if (VersionUtils.isTargetQ(view.getContext())) {
                    intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                } else {
                    intent = new Intent(Intent.ACTION_GET_CONTENT);
                }
                intent.setType("image/*");
                startActivityForResult(intent, 100);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && requestCode == 100) {
            onActivityResultForLocalPhotos(data);
        }
    }

    /**
     * 选择本地图片处理结果
     *
     * @param data
     */
    protected void onActivityResultForLocalPhotos(@Nullable Intent data) {
        if (data != null) {
            Uri selectedImage = data.getData();
            if (selectedImage != null) {
                String filePath = EaseFileUtils.getFilePath(this, selectedImage);
                Log.e("图片路径", filePath);
                if (!TextUtils.isEmpty(filePath) && new File(filePath).exists()) {
                    scanAlbunUrl(filePath);
                } else {
                    Toast.makeText(this, "扫描失败", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}

