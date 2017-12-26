package com.zj.zxing;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.CaptureActivity;
import com.google.zxing.client.android.R;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class MainActivity extends Activity {

    Button encode = null;
    Button encodelogo = null;
    Button parsecode = null;
    Button scan = null;
    ImageView pic = null;
    private String ENCODE_URL = "\"http://blog.csdn.net/user11223344abc\"";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        encode = findViewById(R.id.encode);
        encodelogo = findViewById(R.id.encodelogo);
        scan = findViewById(R.id.scan);
        pic = findViewById(R.id.pic);
        parsecode = findViewById(R.id.parsecode);

        encodelogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Drawable drawable = getResources().getDrawable(R.mipmap.qianlizhumeng);
                Bitmap bitmapLogo = BitmapUtil.drawable2Bitmap(drawable);
                Bitmap qrLogoImage = createQRLogoImage(ENCODE_URL, 1550, bitmapLogo);
                setImgBitmap(pic, qrLogoImage);
            }
        });

        encode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                startActivity(new Intent(MainActivity.this, CaptureActivity.class));
//                Create2QR2(ENCODE_URL, pic, MainActivity.this);

                Drawable drawable = getResources().getDrawable(R.mipmap.qianlizhumeng);
//                Bitmap bitmapLogo = BitmapUtil.drawable2Bitmap(drawable);
                Bitmap qrImage = createQRLogoImage(ENCODE_URL, 1550, null);
                setImgBitmap(pic, qrImage);
            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, CaptureActivity.class));
            }
        });


        parsecode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Bitmap bm = ((BitmapDrawable) ((ImageView) pic).getDrawable()).getBitmap();

//                pic.setDrawingCacheEnabled(true);
//                Bitmap bm = pic.getDrawingCache();
                Result result = parseQRcodeBitmapSimple(bm);
                String text = "";
                if (null != result) {
                    text = result.getText();
                } else {
                    text = "空";
                }
//                final String finalText = text;
//                MainActivity.this.runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        Toast.makeText(MainActivity.this, " >  " + finalText + "  <", Toast.LENGTH_SHORT);
//                    }
//                });
                Log.e("ZXingZjext",text);
           }
        });
    }


    /***
     * 生成带logo的二维码
     * @param content url地址
     * @param heightPix logo 高度
     * @param logoBm logo图
     * @return
     */
    public static Bitmap createQRLogoImage(String content, int heightPix, Bitmap logoBm) {

//        boolean haveLogo = false;
//
//        if(logoBm != null){
//            haveLogo = true;
//        }else {
//            haveLogo = false;
//        }


        try {
            // if (content == null || "".equals(content)) {
            // return false;
            // }

            //配置参数
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            //容错级别
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            //设置空白边距的宽度
            // hints.put(EncodeHintType.MARGIN, 2); //default is 4

            // 图像数据转换，使用了矩阵转换
            BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, heightPix, heightPix, hints);
            int[] pixels = new int[heightPix * heightPix];
            // 下面这里按照二维码的算法，逐个生成二维码的图片，
            // 两个for循环是图片横列扫描的结果
            for (int y = 0; y < heightPix; y++) {
                for (int x = 0; x < heightPix; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * heightPix + x] = 0xff000000;
                    } else {
                        pixels[y * heightPix + x] = 0xffffffff;
                    }
                }
            }

            // 生成二维码图片的格式，使用ARGB_8888
            Bitmap bitmap = Bitmap.createBitmap(heightPix, heightPix, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, heightPix, 0, 0, heightPix, heightPix);

            if (logoBm != null) {
                bitmap = addLogo(bitmap, logoBm);
            }

            //必须使用compress方法将bitmap保存到文件中再进行读取。直接返回的bitmap是没有任何压缩的，内存消耗巨大！
            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 在二维码中间添加Logo图案
     */
    private static Bitmap addLogo(Bitmap src, Bitmap logo) {
        if (src == null) {
            return null;
        }

        if (logo == null) {
            return src;
        }

        //获取图片的宽高
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();

        if (srcWidth == 0 || srcHeight == 0) {
            return null;
        }

        if (logoWidth == 0 || logoHeight == 0) {
            return src;
        }

        //logo大小为二维码整体大小的1/5
        float scaleFactor = srcWidth * 1.0f / 5 / logoWidth;
        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(src, 0, 0, null);
            canvas.scale(scaleFactor, scaleFactor, srcWidth / 2, srcHeight / 2);
            canvas.drawBitmap(logo, (srcWidth - logoWidth) / 2, (srcHeight - logoHeight) / 2, null);

            canvas.save(Canvas.ALL_SAVE_FLAG);
            canvas.restore();
        } catch (Exception e) {
            bitmap = null;
            e.getStackTrace();
        }

        return bitmap;
    }


    /**
     * 生成普通二维码
     */
    private void Create2QR2(String urls, ImageView imageView, Activity aty) {
        String uri = urls;
        int mScreenWidth = 0;
        Bitmap bitmap;
        try {
            /**
             * 获取屏幕信息的区别
             * 只有activity可以使用WindowManager否则应该使用Context.getResources().getDisplayMetrics()来获取。
             * Context.getResources().getDisplayMetrics()依赖于手机系统，获取到的是系统的屏幕信息；
             * WindowManager.getDefaultDisplay().getMetrics(dm)是获取到Activity的实际屏幕信息。
             */
            DisplayMetrics dm = new DisplayMetrics();
            aty.getWindowManager().getDefaultDisplay().getMetrics(dm);
            mScreenWidth = dm.heightPixels;
            bitmap = BitmapUtil.createQRCode(uri, mScreenWidth);//width可选参数

            //自定义操作
            setImgBitmap(imageView, bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 设置bitmap图片显示
     *
     * @param imageView
     * @param bitmap
     */
    private void setImgBitmap(final ImageView imageView, final Bitmap bitmap) {
        if (bitmap != null) {
            imageView.post(new Runnable() {
                @Override
                public void run() {
                    imageView.setImageBitmap(bitmap);
                }
            });
        }
    }


    //解析二维码图片,返回结果封装在Result对象中
    private com.google.zxing.Result parseQRcodeBitmapSimple(Bitmap bitmap) {
        //解析转换类型UTF-8
        Hashtable<DecodeHintType, String> hints = new Hashtable<DecodeHintType, String>();
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
//        //获取到待解析的图片
        BitmapFactory.Options options = new BitmapFactory.Options();
//        //如果我们把inJustDecodeBounds设为true，那么BitmapFactory.decodeFile(String path, Options opt)
//        //并不会真的返回一个Bitmap给你，它仅仅会把它的宽，高取回来7给你
        options.inJustDecodeBounds = true;
//        //此时的bitmap是null，这段代码之后，options.outWidth 和 options.outHeight就是我们想要的宽和高了

//        Bitmap bitmap = BitmapFactory.decodeFile(bitmapPath, options);
        //我们现在想取出来的图片的边长（二维码图片是正方形的）设置为400像素
        /**
         options.outHeight = 400;
         options.outWidth = 400;
         options.inJustDecodeBounds = false;
         bitmap = BitmapFactory.decodeFile(bitmapPath, options);
         */
        //以上这种做法，虽然把bitmap限定到了我们要的大小，但是并没有节约内存，如果要节约内存，我们还需要使用inSimpleSize这个属性
//        options.inSampleSize = options.outHeight / 400;
//        if (options.inSampleSize <= 0) {
//            options.inSampleSize = 1; //防止其值小于或等于0
//        }
        /**
         * 辅助节约内存设置
         *
         * options.inPreferredConfig = Bitmap.Config.ARGB_4444;    // 默认是Bitmap.Config.ARGB_8888
         * options.inPurgeable = true;
         * options.inInputShareable = true;
         */
//        options.inJustDecodeBounds = false;
//        bitmap = BitmapFactory.decodeFile(bitmapPath, options);
        //新建一个RGBLuminanceSource对象，将bitmap图片传给此对象
        RGBLuminanceSource rgbLuminanceSource = new RGBLuminanceSource(bitmap);
        //将图片转换成二进制图片
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(rgbLuminanceSource));
        //初始化解析对象
        QRCodeReader reader = new QRCodeReader();
        //开始解析
        Result result = null;
        try {
            result = reader.decode(binaryBitmap, hints);
        } catch (Exception e) {
            // TODO: handle exception
        }

        return result;
    }


    //解析二维码图片,返回结果封装在Result对象中
    private com.google.zxing.Result parseQRcodeBitmap(String bitmapPath) {
        //解析转换类型UTF-8
        Hashtable<DecodeHintType, String> hints = new Hashtable<DecodeHintType, String>();
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        //获取到待解析的图片
        BitmapFactory.Options options = new BitmapFactory.Options();
        //如果我们把inJustDecodeBounds设为true，那么BitmapFactory.decodeFile(String path, Options opt)
        //并不会真的返回一个Bitmap给你，它仅仅会把它的宽，高取回来给你
        options.inJustDecodeBounds = true;
        //此时的bitmap是null，这段代码之后，options.outWidth 和 options.outHeight就是我们想要的宽和高了
        Bitmap bitmap = BitmapFactory.decodeFile(bitmapPath, options);
        //我们现在想取出来的图片的边长（二维码图片是正方形的）设置为400像素
        /**
         options.outHeight = 400;
         options.outWidth = 400;
         options.inJustDecodeBounds = false;
         bitmap = BitmapFactory.decodeFile(bitmapPath, options);
         */
        //以上这种做法，虽然把bitmap限定到了我们要的大小，但是并没有节约内存，如果要节约内存，我们还需要使用inSimpleSize这个属性
        options.inSampleSize = options.outHeight / 400;
        if (options.inSampleSize <= 0) {
            options.inSampleSize = 1; //防止其值小于或等于0
        }
        /**
         * 辅助节约内存设置
         *
         * options.inPreferredConfig = Bitmap.Config.ARGB_4444;    // 默认是Bitmap.Config.ARGB_8888
         * options.inPurgeable = true;
         * options.inInputShareable = true;
         */
        options.inJustDecodeBounds = false;
        bitmap = BitmapFactory.decodeFile(bitmapPath, options);
        //新建一个RGBLuminanceSource对象，将bitmap图片传给此对象
        RGBLuminanceSource rgbLuminanceSource = new RGBLuminanceSource(bitmap);
        //将图片转换成二进制图片
        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(rgbLuminanceSource));
        //初始化解析对象
        QRCodeReader reader = new QRCodeReader();
        //开始解析
        Result result = null;
        try {
            result = reader.decode(binaryBitmap, hints);
        } catch (Exception e) {
            // TODO: handle exception
        }

        return result;
    }


}
