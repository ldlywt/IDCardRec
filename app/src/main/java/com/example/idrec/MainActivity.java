package com.example.idrec;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;

import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;


public class MainActivity extends Activity {
    private final static String TAG = MainActivity.class.getSimpleName();

    private TessBaseAPI mTessBaseAPI;
    private String language = "cn";
    private TextView mTvIdNum;
    private ImageView mIvIdCard;
    private ProgressDialog progressDialog;
    private Bitmap mResultImage;
    private Bitmap mFullImage;
    private PermissionHelper mHelper;


    private void showProgress() {
        if (null != progressDialog) {
            progressDialog.show();
        } else {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("请稍候...");
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }
    }

    private void dismissProgress() {
        if (null != progressDialog) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mIvIdCard = (ImageView) findViewById(R.id.idcard);
        mTvIdNum = (TextView) findViewById(R.id.tesstext);
        requestPermission();
    }

    private void init() {
        mTessBaseAPI = new TessBaseAPI();
        showProgress();
        AssetsUtils.getInstance(MainActivity.this).copyAssetsToSD("trained", "tess/tessdata");
        AssetsUtils.getInstance(MainActivity.this).setFileOperateCallback(new AssetsUtils.FileOperateCallback() {
            @Override
            public void onSuccess() {
                boolean init = mTessBaseAPI.init(Environment.getExternalStorageDirectory() + "/tess", language);
                if (init) {
                    dismissProgress();
                } else {
                    Toast.makeText(MainActivity.this, "load trainedData failed", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailed(String error) {
                Toast.makeText(MainActivity.this, "load trainedData failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void requestPermission() {
        mHelper = new PermissionHelper(this);
        mHelper.requestPermissions("请授予[读写]权限！",
                new PermissionHelper.PermissionListener() {
                    @Override
                    public void doAfterGrand(String... permission) {
                        init();
                    }

                    @Override
                    public void doAfterDenied(String... permission) {
                        Toast.makeText(MainActivity.this, "权限获取失败", Toast.LENGTH_SHORT).show();
                    }
                }, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mHelper.handleRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void search(View view) {
        Intent intent;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            intent = new Intent();
            intent.setAction(Intent.ACTION_GET_CONTENT);
        } else {
            intent = new Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        intent.setType("image/*");
        //使用选取器并自定义标题
        startActivityForResult(Intent.createChooser(intent, "选择待识别图片"), 100);
    }

    public void searchId(View view) {
        mTvIdNum.setText(null);
        mResultImage = null;
        Bitmap bitmapResult = NativeUtils.getIdNumber(mFullImage, Bitmap.Config.ARGB_8888);
        mFullImage.recycle();
        mResultImage = bitmapResult;
        //tesseract-ocr
        mIvIdCard.setImageBitmap(bitmapResult);
    }

    public void recognition(View view) {
        // 识别Bitmap中的图片
        mTessBaseAPI.setImage(mResultImage);
        mTvIdNum.setText(mTessBaseAPI.getUTF8Text());
        mTessBaseAPI.clear();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && null != data) {
            getResult(data.getData());
        }
    }

    private void getResult(Uri uri) {
        String imagePath = FileUtil.getFilePathByUri(this, uri);
        if (!TextUtils.isEmpty(imagePath)) {
            if (mFullImage != null) {
                mFullImage.recycle();
            }
            mFullImage = toBitmap(imagePath);
            mTvIdNum.setText(null);
            mIvIdCard.setImageBitmap(mFullImage);
        }
    }

    public static Bitmap toBitmap(String pathName) {
        if (TextUtils.isEmpty(pathName))
            return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pathName, o);
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (width_tmp > 640 || height_tmp > 480) {
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = scale;
        opts.outHeight = height_tmp;
        opts.outWidth = width_tmp;
        return BitmapFactory.decodeFile(pathName, opts);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissProgress();
        mTessBaseAPI.end();
    }


}
