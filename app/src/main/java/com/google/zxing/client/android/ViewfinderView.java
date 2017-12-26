package com.google.zxing.client.android;

import com.google.zxing.ResultPoint;
import com.google.zxing.client.android.camera.CameraManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

  private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
  private static final long ANIMATION_DELAY = 80L;
  private static final int CURRENT_POINT_OPACITY = 0xA0;
  private static final int MAX_RESULT_POINTS = 20;
  private static final int POINT_SIZE = 6;

  private CameraManager cameraManager;
  private final Paint paint;
  private Bitmap resultBitmap;
  private final int maskColor;
  private final int resultColor;
  private final int resultPointColor;
  private int scannerAlpha;
  private List<ResultPoint> possibleResultPoints;
  private List<ResultPoint> lastPossibleResultPoints;
  private final int laserColor;
  private int mTop = -1;
  private final Bitmap scanLight;

  // This constructor is used when the class is built from an XML resource.
  public ViewfinderView(Context context, AttributeSet attrs) {
    super(context, attrs);

    // Initialize these once for performance rather than calling them every time in onDraw().
    paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    Resources resources = getResources();
    maskColor = resources.getColor(R.color.viewfinder_mask);
    resultColor = resources.getColor(R.color.result_view);
    laserColor = resources.getColor(android.R.color.holo_green_dark);
    resultPointColor = resources.getColor(R.color.possible_result_points);
    scannerAlpha = 0;
    possibleResultPoints = new ArrayList<>(5);
    lastPossibleResultPoints = null;

    scanLight = BitmapFactory.decodeResource(this.getResources(), R.drawable.launcher_icon);

  }

  public void setCameraManager(CameraManager cameraManager) {
    this.cameraManager = cameraManager;
  }

  @SuppressLint("DrawAllocation")
  @Override
  public void onDraw(Canvas canvas) {
    if (cameraManager == null) {
      return; // not ready yet, early draw before done configuring
    }
    Rect frame = cameraManager.getFramingRect();
    Rect previewFrame = cameraManager.getFramingRectInPreview();    
    if (frame == null || previewFrame == null) {
      return;
    }
    int width = canvas.getWidth();
    int height = canvas.getHeight();

    // Draw the exterior (i.e. outside the framing rect) darkened
    paint.setColor(resultBitmap != null ? resultColor : maskColor);
    canvas.drawRect(0, 0, width, frame.top, paint);
    canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
    canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
    canvas.drawRect(0, frame.bottom + 1, width, height, paint);

    if (resultBitmap != null) {
      // Draw the opaque result bitmap over the scanning rectangle
      paint.setAlpha(CURRENT_POINT_OPACITY);
      canvas.drawBitmap(resultBitmap, null, frame, paint);
    } else {

      // Draw a red "laser scanner" line through the middle to show decoding is active
      paint.setColor(laserColor);
      paint.setAlpha(SCANNER_ALPHA[scannerAlpha]);
      scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.length;
//      canvas.drawRect(frame.left + 2, mTop, frame.right - 1, middle + 2, paint);
      drawScanLight(canvas,frame);

      float scaleX = frame.width() / (float) previewFrame.width();
      float scaleY = frame.height() / (float) previewFrame.height();

      List<ResultPoint> currentPossible = possibleResultPoints;
      List<ResultPoint> currentLast = lastPossibleResultPoints;
      int frameLeft = frame.left;
      int frameTop = frame.top;

      if (currentPossible.isEmpty()) {
        lastPossibleResultPoints = null;
      } else {
        possibleResultPoints = new ArrayList<>(5);
        lastPossibleResultPoints = currentPossible;
        paint.setAlpha(CURRENT_POINT_OPACITY);
        paint.setColor(resultPointColor);
        synchronized (currentPossible) {
          for (ResultPoint point : currentPossible) {
            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                              frameTop + (int) (point.getY() * scaleY),
                              POINT_SIZE, paint);
          }
        }
      }

      drawFrameCorner(canvas,frame);

      if (currentLast != null) {
        paint.setAlpha(CURRENT_POINT_OPACITY / 2);
        paint.setColor(resultPointColor);
        synchronized (currentLast) {
          float radius = POINT_SIZE / 2.0f;
          for (ResultPoint point : currentLast) {
            canvas.drawCircle(frameLeft + (int) (point.getX() * scaleX),
                              frameTop + (int) (point.getY() * scaleY),
                              radius, paint);
          }
        }
      }

      // Request another update at the animation interval, but only repaint the laser line,
      // not the entire viewfinder mask.
      postInvalidateDelayed(ANIMATION_DELAY,
                            frame.left - POINT_SIZE,
                            frame.top - POINT_SIZE,
                            frame.right + POINT_SIZE,
                            frame.bottom + POINT_SIZE);
    }
  }


  /**
   * 绘制扫描线方法
   * @param canvas
   * @param frame
   */
  private void drawScanLight(Canvas canvas, Rect frame) {

    if (scanLineTop == 0) {
      scanLineTop = frame.top;
    }

    if (scanLineTop >= frame.bottom - 30) {
      scanLineTop = frame.top;
    } else {
      scanLineTop += SCAN_VELOCITY;// SCAN_VELOCITY可以在属性中设置，默认为5
    }
    Rect scanRect = new Rect(frame.left, scanLineTop, frame.right, scanLineTop + 30);
    canvas.drawBitmap(scanLight, null, scanRect, paint);
  }


  public void drawViewfinder() {
    Bitmap resultBitmap = this.resultBitmap;
    this.resultBitmap = null;
    if (resultBitmap != null) {
      resultBitmap.recycle();
    }
    invalidate();
  }

  /**
   * Draw a bitmap with the result points highlighted instead of the live scanning display.
   *
   * @param barcode An image of the decoded barcode.
   */
  public void drawResultBitmap(Bitmap barcode) {
    resultBitmap = barcode;
    invalidate();
  }

  public void addPossibleResultPoint(ResultPoint point) {
    List<ResultPoint> points = possibleResultPoints;
    synchronized (points) {
      points.add(point);
      int size = points.size();
      if (size > MAX_RESULT_POINTS) {
        // trim it
        points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
      }
    }
  }


  private int laserFrameCornerWidth = 50;//扫描框4角宽
  private int laserFrameCornerLength = 50;//扫描框4角高
//  private int laserColor = android.R.color.holo_green_dark;//扫描线颜色
//  private int laserFrameBoundColor = laserColor;//扫描框4角颜色
  private int laserLineTop;// 扫描线最顶端位置
  private int laserLineHeight;//扫描线默认高度
  private int laserMoveSpeed;// 扫描线默认移动距离px
  private int laserLineResId;//扫描线图片资源
  private String drawText = "将二维码放入框内，即可自动扫描";//提示文字
  private int drawTextSize;//提示文字大小
  private int drawTextColor = Color.WHITE;//提示文字颜色
  private boolean drawTextGravityBottom = true;//提示文字位置
  private int drawTextMargin;//提示文字与扫描框距离
  /**
   * 绘制扫描框4角
   *
   * @param canvas
   * @param frame
   */
  private void drawFrameCorner(Canvas canvas, Rect frame) {
    paint.setColor(getResources().getColor(android.R.color.holo_green_dark));
    paint.setStyle(Paint.Style.FILL);
    // 左上角
    canvas.drawRect(frame.left - laserFrameCornerWidth, frame.top, frame.left, frame.top
            + laserFrameCornerLength, paint);
    canvas.drawRect(frame.left - laserFrameCornerWidth, frame.top - laserFrameCornerWidth, frame.left
            + laserFrameCornerLength, frame.top, paint);
    // 右上角
    canvas.drawRect(frame.right, frame.top, frame.right + laserFrameCornerWidth,
            frame.top + laserFrameCornerLength, paint);
    canvas.drawRect(frame.right - laserFrameCornerLength, frame.top - laserFrameCornerWidth,
            frame.right + laserFrameCornerWidth, frame.top, paint);
    // 左下角
    canvas.drawRect(frame.left - laserFrameCornerWidth, frame.bottom - laserFrameCornerLength,
            frame.left, frame.bottom, paint);
    canvas.drawRect(frame.left - laserFrameCornerWidth, frame.bottom, frame.left
            + laserFrameCornerLength, frame.bottom + laserFrameCornerWidth, paint);
    // 右下角
    canvas.drawRect(frame.right, frame.bottom - laserFrameCornerLength, frame.right
            + laserFrameCornerWidth, frame.bottom, paint);
    canvas.drawRect(frame.right - laserFrameCornerLength, frame.bottom, frame.right
            + laserFrameCornerWidth, frame.bottom + laserFrameCornerWidth, paint);
  }

  private int dp2px(Context context, float dpValue) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dpValue, context.getResources().getDisplayMetrics());
  }


  int scanLineTop;
  int SCAN_VELOCITY = 15;
  private void drawScanLight(Canvas canvas, Rect frame,Bitmap scanLight) {

    if (scanLineTop == 0) {
      scanLineTop = frame.top;
    }

    if (scanLineTop >= frame.bottom - 30) {
      scanLineTop = frame.top;
    } else {
      scanLineTop += SCAN_VELOCITY;// SCAN_VELOCITY 可以在属性中设置，默认为5
    }

    Rect scanRect = new Rect(frame.left, scanLineTop, frame.right, scanLineTop + 30);
    canvas.drawBitmap(scanLight, null, scanRect, paint);
  }



}
