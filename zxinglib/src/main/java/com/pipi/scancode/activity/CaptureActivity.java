package com.pipi.scancode.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Binarizer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.pipi.scancode.R;
import com.pipi.scancode.camera.CameraManager;
import com.pipi.scancode.decoding.CaptureActivityHandler;
import com.pipi.scancode.decoding.InactivityTimer;
import com.pipi.scancode.decoding.RGBLuminanceSource;
import com.pipi.scancode.view.ViewfinderView;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "CaptureActivity";

    private static final int REQUEST_CODE_SCAN_GALLERY = 100;

    private CaptureActivityHandler handler;

    private ViewfinderView viewfinderView;

    private ImageView back;

    private boolean hasSurface;

    private Vector<BarcodeFormat> decodeFormats;

    private String characterSet;

    private InactivityTimer inactivityTimer;

    private MediaPlayer mediaPlayer;

    private boolean playBeep;

    private static final float BEEP_VOLUME = 0.1F;

    private boolean vibrate;

    private ProgressDialog mProgress;

    private String photo_path;

    private Bitmap scanBitmap;

    public static final int RESULT_CODE_QR_SCAN = 161;

    public static final String INTENT_EXTRA_KEY_QR_SCAN = "qr_scan_result";

    private static final long VIBRATE_DURATION = 200L;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        CameraManager.init((Context) getApplication());
        this.viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_content);
        this.back = (ImageView) findViewById(R.id.scanner_toolbar_back);
        this.back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                CaptureActivity.this.finish();
            }
        });
        this.hasSurface = false;
        this.inactivityTimer = new InactivityTimer((Activity) this);
    }

    private void addToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == -1) {
            Cursor cursor;
            switch (requestCode) {
                case 100:
                    cursor = getContentResolver().query(data.getData(), null, null, null, null);
                    if (cursor.moveToFirst())
                        this.photo_path = cursor.getString(cursor.getColumnIndex("_data"));
                    cursor.close();
                    this.mProgress = new ProgressDialog((Context) this);
                    this.mProgress.setMessage("");
                    this.mProgress.setCancelable(false);
                    this.mProgress.show();
                    (new Thread(new Runnable() {
                        public void run() {
                            Result result = CaptureActivity.this.scanningImage(CaptureActivity.this.photo_path);
                            if (result != null) {
                                Intent resultIntent = new Intent();
                                Bundle bundle = new Bundle();
                                bundle.putString(INTENT_EXTRA_KEY_QR_SCAN, result.getText());
                                resultIntent.putExtras(bundle);
                                CaptureActivity.this.setResult(RESULT_CODE_QR_SCAN, resultIntent);
                            } else {
                                Message m = CaptureActivity.this.handler.obtainMessage();
                                m.what = R.id.decode_failed;
                                m.obj = "Scan failed!";
                                CaptureActivity.this.handler.sendMessage(m);
                            }
                        }
                    })).start();
                    break;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public Result scanningImage(String path) {
        if (TextUtils.isEmpty(path))
            return null;
        Hashtable<DecodeHintType, String> hints = new Hashtable<>();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF8");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        this.scanBitmap = BitmapFactory.decodeFile(path, options);
        options.inJustDecodeBounds = false;
        int sampleSize = (int) (options.outHeight / 200.0F);
        if (sampleSize <= 0)
            sampleSize = 1;
        options.inSampleSize = sampleSize;
        this.scanBitmap = BitmapFactory.decodeFile(path, options);
        RGBLuminanceSource source = new RGBLuminanceSource(this.scanBitmap);
        BinaryBitmap bitmap1 = new BinaryBitmap((Binarizer) new HybridBinarizer((LuminanceSource) source));
        QRCodeReader reader = new QRCodeReader();
        try {
            return reader.decode(bitmap1, hints);
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (ChecksumException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected void onResume() {
        super.onResume();
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.scanner_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        if (this.hasSurface) {
            initCamera(surfaceHolder);
        } else {
            surfaceHolder.addCallback(this);
            surfaceHolder.setType(3);
        }
        this.decodeFormats = null;
        this.characterSet = null;
        this.playBeep = true;
        AudioManager audioService = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioService.getRingerMode() != 2)
            this.playBeep = false;
        initBeepSound();
        this.vibrate = true;
    }

    protected void onPause() {
        super.onPause();
        if (this.handler != null) {
            this.handler.quitSynchronously();
            this.handler = null;
        }
        CameraManager.get().closeDriver();
    }

    protected void onDestroy() {
        this.inactivityTimer.shutdown();
        super.onDestroy();
    }

    public void handleDecode(Result result, Bitmap barcode) {
        this.inactivityTimer.onActivity();
        playBeepSoundAndVibrate();
        String resultString = result.getText();
        if (TextUtils.isEmpty(resultString)) {
            Toast.makeText((Context) this, "Scan failed!", Toast.LENGTH_SHORT).show();
        } else {
            Intent resultIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putString(INTENT_EXTRA_KEY_QR_SCAN, resultString);
            Log.i("CaptureActivity", "scan result = " + resultString);
            resultIntent.putExtras(bundle);
            setResult(RESULT_CODE_QR_SCAN, resultIntent);
        }
        finish();
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder);
        } catch (IOException ioe) {
            return;
        } catch (RuntimeException e) {
            return;
        }
        if (this.handler == null)
            this.handler = new CaptureActivityHandler(this, this.decodeFormats, this.characterSet);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if (!this.hasSurface) {
            this.hasSurface = true;
            initCamera(holder);
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        this.hasSurface = false;
    }

    public ViewfinderView getViewfinderView() {
        return this.viewfinderView;
    }

    public Handler getHandler() {
        return (Handler) this.handler;
    }

    public void drawViewfinder() {
        this.viewfinderView.drawViewfinder();
    }

    private void initBeepSound() {
        if (this.playBeep && this.mediaPlayer == null) {
            setVolumeControlStream(3);
            this.mediaPlayer = new MediaPlayer();
            this.mediaPlayer.setAudioStreamType(3);
            this.mediaPlayer.setOnCompletionListener(this.beepListener);
            AssetFileDescriptor file = getResources().openRawResourceFd(R.raw.beep);
            try {
                this.mediaPlayer.setDataSource(file.getFileDescriptor(), file
                        .getStartOffset(), file.getLength());
                file.close();
                this.mediaPlayer.setVolume(0.1F, 0.1F);
                this.mediaPlayer.prepare();
            } catch (IOException e) {
                this.mediaPlayer = null;
            }
        }
    }

    private void playBeepSoundAndVibrate() {
        if (this.playBeep && this.mediaPlayer != null)
            this.mediaPlayer.start();
        if (this.vibrate) {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(200L);
        }
    }

    private final MediaPlayer.OnCompletionListener beepListener = new MediaPlayer.OnCompletionListener() {
        public void onCompletion(MediaPlayer mediaPlayer) {
            mediaPlayer.seekTo(0);
        }
    };
}

