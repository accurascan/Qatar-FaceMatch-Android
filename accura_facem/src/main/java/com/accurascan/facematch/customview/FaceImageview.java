package com.accurascan.facematch.customview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import com.inet.facelock.callback.FaceDetectionResult;

public class FaceImageview extends View {
    Bitmap image = null;
    FaceDetectionResult detectionResult = null;

    public FaceImageview(Context context) {
        super(context);

        LayoutParams param = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        this.setLayoutParams(param);
    }

    public void setImage(Bitmap image) {
        this.image = Bitmap.createBitmap(image);
    }

    public Bitmap getImage() {
        return image;
    }

    public void setFaceDetectionResult(FaceDetectionResult result) {
        this.detectionResult = result;
    }

    public FaceDetectionResult getFaceDetectionResult() {
        return this.detectionResult;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        if (image != null) {
            Rect clipRect = canvas.getClipBounds();
            int w = clipRect.width();
            int h = clipRect.height();
            int imgW = image.getWidth();
            int imgH = image.getHeight();

            float scaleX = ((float) w) / imgW;
            float scaleY = ((float) h) / imgH;
            float scale = scaleX;
            if (scaleX > scaleY)
                scale = scaleY;
            imgW = (int) (scale * imgW);
            imgH = (int) (scale * imgH);
            Rect dst = new Rect();
            dst.left = (w - imgW) / 2;
            dst.top = (h - imgH) / 2;
            dst.right = dst.left + imgW;
            dst.bottom = dst.top + imgH;


            canvas.drawBitmap(image, null, dst, null);

            if (detectionResult != null) {
                Paint myPaint = new Paint();
                myPaint.setColor(Color.GREEN);
                myPaint.setStyle(Paint.Style.STROKE);
                myPaint.setStrokeWidth(2);
                int x1 = (int) (detectionResult.getFaceRect().left * scale + dst.left);
                int y1 = (int) (detectionResult.getFaceRect().top * scale + dst.top);
                int x2 = (int) (detectionResult.getFaceRect().right * scale + dst.left);
                int y2 = (int) (detectionResult.getFaceRect().bottom * scale + dst.top);

                canvas.drawRect(x1, y1, x2, y2, myPaint);
            }
        }
    }
}