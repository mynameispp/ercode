package com.pipi.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.pipi.scancode.activity.CaptureActivity;

public class MainActivity extends AppCompatActivity {
    TextView result;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        result = findViewById(R.id.scan_code_result);
        findViewById(R.id.scan_code).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.VIBRATE};
                //申请权限，其中RC_PERMISSION是权限申请码，用来标志权限申请的
                ActivityCompat.requestPermissions(MainActivity.this, permissions, 100);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 100 && grantResults.length == 2
                && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            startActivityForResult(new Intent(this, CaptureActivity.class),1);
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
}
