package com.example.medifiles;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.LinearLayout;

public class FloatingViewService extends Service {
    private WindowManager windowManager;
    private Chronometer chronometer;
    private long pauseOffset = 0; // Time elapsed before the pause
    private boolean isChronometerRunning = false;
    private View floatingView;
    private DrawView drawView;
    private View colorPickerView;
    private boolean isFloatingViewVisible = false;
    private WindowManager.LayoutParams colorPickerParams;
    private ImageButton pauseButton;
    private BroadcastReceiver recordingStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isPaused = intent.getBooleanExtra("isPaused", false);
            updatePauseButton(isPaused);
        }
    };
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Binding not used
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //  메뉴바 레이아웃 추가
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_view_layout, null);
        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.END|  Gravity.CENTER;

        params.x = 0; // Offset from the right edge
        params.y = 0; // Offset from the top
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, params);
        LinearLayout menubar = floatingView.findViewById(R.id.menubar);
        ImageButton writeButton = floatingView.findViewById(R.id.write_button);
        pauseButton = floatingView.findViewById(R.id.pause_button);
        ImageButton stopButton = floatingView.findViewById(R.id.stop_button);
        Chronometer chronometer = floatingView.findViewById(R.id.chronometer);
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();
        isChronometerRunning = true;
        menubar.setVisibility(View.VISIBLE);
        //---여기까지 메뉴바 설정 ------

        drawView = new DrawView(this);
        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isFloatingViewVisible) {
                    //컬러피커 초기화 전
                    if (colorPickerView == null) {
                        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
                        //컬러피커
                        colorPickerView = inflater.inflate(R.layout.color_and_size_picker_layout, null);
                        menubar.post(new Runnable() {
                            @Override
                            public void run() {
                                int[] location = new int[2];
                                menubar.getLocationOnScreen(location);
                                //드로우뷰
                                int menubarLeftX = location[0];
                                // 드로우뷰의 너비를 메뉴바의 왼쪽 x좌표까지로 설정
                                WindowManager.LayoutParams drawViewParams = new WindowManager.LayoutParams(
                                        menubarLeftX - 150, // 너비
                                        WindowManager.LayoutParams.MATCH_PARENT,
                                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                                        PixelFormat.TRANSLUCENT);
                                if (drawView.getParent() != null) {
                                    windowManager.updateViewLayout(drawView, drawViewParams);
                                } else {
                                    windowManager.addView(drawView, drawViewParams);
                                }

                                //컬러피커
                                int buttonWidth = menubar.getWidth();
                                // 컬러피커 위치시킴
                                colorPickerParams = new WindowManager.LayoutParams(
                                        WindowManager.LayoutParams.WRAP_CONTENT,
                                        WindowManager.LayoutParams.WRAP_CONTENT,
                                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                                        PixelFormat.TRANSLUCENT);
                                colorPickerParams.gravity = Gravity.TOP | Gravity.START;
                                // 왼쪽에 위치시킴
                                colorPickerParams.x = location[0] - buttonWidth;
                                colorPickerParams.y = location[1];
                                // 컬러피커 추가
                                windowManager.addView(colorPickerView, colorPickerParams);
                            }
                        });

                        //버튼 선택
                        Button colorButtonBlack = colorPickerView.findViewById(R.id.color_button_black);
                        colorButtonBlack.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                // DrawView의 펜 색상을 검정색으로 설정
                                drawView.setPaintColor("#000000");
                                drawView.startDrawingMode();
                            }
                        });
                        Button colorButtonRed = colorPickerView.findViewById(R.id.color_button_red);
                        colorButtonRed.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                drawView.setPaintColor("#FFCDD2");
                                // 그림 그리기 모드로 전환
                                drawView.startDrawingMode();
                            }
                        });
                        Button colorButtonBlue = colorPickerView.findViewById(R.id.color_button_blue);
                        colorButtonBlue.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                drawView.setPaintColor("#3D5AFE");
                                drawView.startDrawingMode();
                            }
                        });
                        Button colorButtonGreen = colorPickerView.findViewById(R.id.color_button_green);
                        colorButtonGreen.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                drawView.setErase(false);
                                drawView.setPaintColor("#E0E0E0");

                                drawView.startDrawingMode();
                            }
                        });
                        Button colorButtonPurple = colorPickerView.findViewById(R.id.color_button_purple);
                        colorButtonPurple.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                drawView.setErase(false);
                                drawView.setPaintColor("#FF6C007E");

                                //  drawView.startDrawingMode();
                            }
                        });
                        ImageButton btn_erase = colorPickerView.findViewById(R.id.btn_erase);
                        btn_erase.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                drawView.setErase(true);
                            }
                        });
                    }
                    else {
                        // 이미 초기화된 colorPickerView를 다시 보여줌
                        colorPickerView.setVisibility(View.VISIBLE);
                        drawView.setVisibility(View.VISIBLE);
                    }                        // Visible 상태를 true로 업데이트합니다.
                    isFloatingViewVisible = true;

                } else {  // 두 번째 클릭: colorPickerView를 숨기고 drawView의 캔버스 클리어

                    // WindowManager에서 colorPickerView를 제거
                    if (colorPickerView != null) {
                        colorPickerView.setVisibility(View.GONE);
                        drawView.clearCanvas();
                        drawView.setVisibility(View.GONE);

                    }
                    isFloatingViewVisible = false;
                }
            }
        });

        pauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 브로드캐스트 보내기
                Intent intent = new Intent("ACTION_PAUSE_RECORDING");
                sendBroadcast(intent);
                if (isChronometerRunning) {
                    pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
                    chronometer.stop();
                    isChronometerRunning = false;
                } else {
                    chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
                    chronometer.start();
                    isChronometerRunning = true;
                }
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chronometer.setBase(SystemClock.elapsedRealtime());
                pauseOffset = 0;
                if (isChronometerRunning) {
                    chronometer.stop();
                    isChronometerRunning = false;
                }
                if (colorPickerView != null) {
                    colorPickerView.setVisibility(View.GONE);
                    drawView.clearCanvas();
                    drawView.setVisibility(View.GONE);
                                 }
                Intent intent = new Intent("ACTION_STOP_RECORDING");
                sendBroadcast(intent);

            }
        });
        IntentFilter filter = new IntentFilter("com.example.ACTION_CHANGE_RECORDING_STATE");
        registerReceiver(recordingStateReceiver, filter);
    }


    private void updatePauseButton(boolean isPaused) {
        // 이미지 뷰의 크기를 조정할 LayoutParams 객체 생성
        ViewGroup.LayoutParams params = pauseButton.getLayoutParams();

        if (isPaused) {
            // 이미지를 rred로 변경하고 크기 조정
            pauseButton.setImageResource(R.drawable.rred);

            // 원하는 크기로 설정 (예: 너비 50dp, 높이 50dp)
            int sizeInDp = 25;
            float scale = getResources().getDisplayMetrics().density;
            int sizeInPx = (int) (sizeInDp * scale + 0.5f); // dp를 픽셀로 변환
            params.width = sizeInPx;
            params.height = sizeInPx;

        } else {
            // 이미지를 ic_pause로 변경하고 원래 크기로 복원
            pauseButton.setImageResource(R.drawable.ic_pause);

            // 원래 크기로 설정
            int sizeInDp = 40;
            float scale = getResources().getDisplayMetrics().density;
            int sizeInPx = (int) (sizeInDp * scale + 0.5f); // dp를 픽셀로 변환
            params.width = sizeInPx;
            params.height = sizeInPx;
        }

        // 변경된 크기 적용
        pauseButton.setLayoutParams(params);
    }

    @Override

    public void onDestroy() {

        if (floatingView != null && floatingView.getParent() != null) {
            windowManager.removeView(floatingView); // FloatingView 제거
        }
        // 다른 정리 작업 수행
        unregisterReceiver(recordingStateReceiver);
        super.onDestroy();
    }
}
