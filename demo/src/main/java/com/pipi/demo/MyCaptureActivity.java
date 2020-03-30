package com.pipi.demo;

import android.view.SurfaceHolder;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.pipi.scancode.activity.CaptureActivity;
import com.pipi.scancode.view.ViewfinderView;

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
                //本地图片，建议扫码是让用户裁剪二维码
//                scanAlbunUrl("/storage/emulated/0/Pictures/Screenshots/Screenshot_20200330_171027.jpg");
            }
        });
    }
}

