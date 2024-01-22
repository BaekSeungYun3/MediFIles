package com.example.medifiles;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;


public class DrawView extends View {
    private float brushSize;
    private float lastBrushSize;
    private float eraserSize;
    private boolean isEraserActive;
    private Paint drawPaint, canvasPaint;
    private Path path;
    private int paintColor = Color.BLACK; // 기본 색상
    private int backgroundColor = Color.WHITE; // 배경 색상
    private Canvas drawCanvas;
    private Bitmap canvasBitmap;
    private boolean isDrawingMode = false;
    private int lastPaintColor;
    private Rect menuBarRect = new Rect();

    public DrawView(Context context) {
        super(context);
        setupDrawing();
    }

    private void setupDrawing() {
        // 초기 설정 메소드
        path = new Path();
        drawPaint = new Paint();
        brushSize = 10; // 초기 굵기
        lastBrushSize = brushSize; // 마지막 사용된 펜 굵기를 저장
        eraserSize = 50; // 지우개 굵기 초기값
        isEraserActive = false; // 지우개 활성화 상태
        drawPaint.setColor(paintColor);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(10); // 기본 굵기 설정
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        canvasPaint = new Paint(Paint.DITHER_FLAG);
    }


    // 굵기 설정 메소드
    public void setBrushSize(float newSize) {
        if (isEraserActive) {
            eraserSize = newSize;
        } else {
            lastBrushSize = newSize;
        }
        drawPaint.setStrokeWidth(newSize);
    }


    // 지우개 설정 메소드
    public void setErase(boolean isErase) {
        isEraserActive = isErase;

        if (isEraserActive) {
            lastBrushSize = drawPaint.getStrokeWidth();
            lastPaintColor = drawPaint.getColor(); // 색상을 저장
            drawPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            drawPaint.setStrokeWidth(eraserSize);
        } else {
            drawPaint.setXfermode(null);
            drawPaint.setColor(lastPaintColor); // 색상을 원래대로
            drawPaint.setStrokeWidth(brushSize); // 마지막 펜 굵기를 복원
        }
    }


    // View 사이즈 지정 시 호출되는 메소드
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        drawCanvas = new Canvas(canvasBitmap);
    }
    public void clearCanvas() {
        canvasBitmap.eraseColor(Color.TRANSPARENT);
        invalidate(); // 뷰 재그리기 요청
    }
    // 그리기 메소드
    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawBitmap(canvasBitmap, 0, 0, canvasPaint);
        canvas.drawPath(path, drawPaint);
    }
    private boolean isDrawing = true;

    public void startDrawingMode() {
        isDrawing = true;
    }

    public void setPaintColor(String newColor) {
        invalidate();
        paintColor = Color.parseColor(newColor);
        drawPaint.setColor(paintColor); // 색상만 변경
        if (isEraserActive) {
            isEraserActive = false;
            drawPaint.setXfermode(null);
            drawPaint.setStrokeWidth(lastBrushSize);
        }
        invalidate(); // 뷰 재그리기 요청
    }



//     터치 이벤트 처리 메소드

    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                path.moveTo(x, y);
                break;
            case MotionEvent.ACTION_MOVE:
                path.lineTo(x, y);
                break;
            case MotionEvent.ACTION_UP:
                drawCanvas.drawPath(path, drawPaint);
                path = new Path(); // 다음 그림을 위해 새로운 Path 인스턴스
                break;
            default:
                return false;
        }

        invalidate();
        return true;
    }
}
