package com.example.medifiles;
import static androidx.activity.result.contract.ActivityResultContracts.*;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.ToggleButton;

//firebase
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DoctorActivity extends AppCompatActivity {
    // Firebase Storage 관련 변수 추가
    private FirebaseStorage storage;
    private StorageReference storageRef;
    // 환자 UID를 가져올 변수 추가
    private String patientUid;
    private ActivityResultLauncher<Intent> screenCaptureLauncher;
    private static final int REQUEST_CODE = 999;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;

    int heightPixels;
    int widthPixels;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionCallback mMediaProjectionCallback;
    private Button recordButton; // 녹화 시작/중지 버튼
    private boolean isRecording = false; // 녹화 중인지 상태를 저장하는 변수
    private boolean isRecordingPaused = false; // 일시정지
    private String pausedVideoPath;
    private String currentVideoPath;
    private boolean isPaused = false;
    private MediaRecorder mMediaRecorder;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    Intent record_intent;
    //--필기 관련 선언
    private static final int OVERLAY_PERMISSION_REQUEST_CODE = 1234;
    private boolean isServiceStarted = false;
    private BroadcastReceiver recordingPauseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("ACTION_PAUSE_RECORDING".equals(intent.getAction())) {
                // 녹화 일시정지 처리
                pauseRecording();
            }

        }
    };
    private BroadcastReceiver recordingStopReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if("ACTION_STOP_RECORDING".equals(intent.getAction())) {
                // 녹화 정지
                stopScreenRecording();
            }
        }
    };



    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_doctor);
        requirePermission();

        // Intent에서 patientUid를 가져옴
        Intent intent = getIntent();
        if (intent != null) {
            patientUid = intent.getStringExtra("patientUid");
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        widthPixels = metrics.widthPixels;
        heightPixels = metrics.heightPixels;

        // Firebase 초기화 및 storageRef 초기화
        storage = FirebaseStorage.getInstance();

        // patientUid를 이용해 Firebase Storage 경로 설정
        if (patientUid != null) {
            storageRef = storage.getReference("patients/" + patientUid + "/videos/");
            // 이제 storageRef를 사용하여 영상 업로드 등을 수행할 수 있습니다.
        }

        mMediaRecorder = new MediaRecorder();
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        recordButton = findViewById(R.id.rb);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    startScreenRecording();
                }
            }
        });

        screenCaptureLauncher = registerForActivityResult(
                new StartActivityForResult(), // 화면 녹화 권한 요청을 위한 인텐트를 실행
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == RESULT_OK) {
                            mMediaProjectionCallback = new MediaProjectionCallback();
                            mMediaProjection = mProjectionManager.getMediaProjection(result.getResultCode(), result.getData());
                            mMediaProjection.registerCallback(mMediaProjectionCallback, null);
                            mVirtualDisplay = createVirtualDisplay(); //화면의 내용을 녹화하기 위한 VirtualDisplay를 생성
                            mMediaRecorder.start(); //녹화를 시작
                        } else {
                            Toast.makeText(DoctorActivity.this,
                                    "권한이 거부 되었습니다.", Toast.LENGTH_SHORT).show();
                            isRecording=false;
                        }
                    }
                }
        );
        // 리시버 등록
        IntentFilter Pfilter = new IntentFilter("ACTION_PAUSE_RECORDING");
        registerReceiver(recordingPauseReceiver, Pfilter);
        IntentFilter Sfilter = new IntentFilter("ACTION_STOP_RECORDING");
        registerReceiver(recordingStopReceiver, Sfilter);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
//화면 녹화를 시작
    private void startScreenRecording() {
        // 녹화 시작 로직
        record_intent = new Intent(this, MediaProjectionAccessService.class);
        startForegroundService(record_intent);
        currentVideoPath = getNewVideoFilePath(); // 새로운 파일 경로 생성
        initRecorder();
        shareScreen();

        // 시스템 오버레이 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(DoctorActivity.this)) {
            // 권한이 없으면 시스템 오버레이 권한 요청
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE);
        } else {
            // 권한이 있는 경우 FloatingViewService 시작
            startFloatingViewService();
        }

        isRecording = true; // 녹화 상태를 true로 변경 녹화 되고 있는 거면 트루임
    }

    //화면 녹화 일시 정지
    private void pauseRecording() {
        if (isRecording) {
            if (!isRecordingPaused) {
                // 녹화 일시정지
                mMediaRecorder.pause();
                isRecordingPaused = true;
                pausedVideoPath = currentVideoPath; // 일시정지된 파일 경로 저장
                Toast.makeText(this, "녹화가 일시정지되었습니다.", Toast.LENGTH_SHORT).show();
                sendRecordingStateBroadcast(isRecordingPaused);
                isPaused = true;
            } else {
                // 녹화 재개
                mMediaRecorder.resume();
                isRecordingPaused = false;
                Toast.makeText(this, "녹화가 재개되었습니다.", Toast.LENGTH_SHORT).show();
                sendRecordingStateBroadcast(isRecordingPaused);
            }
        }
    }
    //일시정지 이미지 변경을 위한 브로드캐스트
    private void sendRecordingStateBroadcast(boolean isPaused) {
        Intent intent = new Intent("com.example.ACTION_CHANGE_RECORDING_STATE");
        intent.putExtra("isPaused", isPaused);
        sendBroadcast(intent);
    }

    //화면 녹화 중지
    private void stopScreenRecording() {
        if (isRecording) { // 녹화 중지 로직
            mMediaRecorder.stop();
            mMediaRecorder.reset();
            stopScreenSharing();
            stopFloatingViewService();
            stopService(record_intent);
            isRecording = false; // 녹화 상태를 false로 변경

            // Firebase Storage에 녹화한 영상 업로드
            uploadVideoToFirebase(currentVideoPath);
        }
    }

    private void uploadVideoToFirebase(String filePath) {
        // null 체크 추가
        if (storageRef != null) {
            // 파일 URI 생성
            Uri fileUri = Uri.fromFile(new File(filePath));

            // Firebase Storage에 저장할 파일명 설정
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "video_" + timeStamp + ".mp4";

            // 사용자 UID로 저장소 참조 생성
            StorageReference userStorageRef = storageRef.child(fileName);

            // 파일 업로드
            userStorageRef.putFile(fileUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        // 업로드 성공
                        Toast.makeText(DoctorActivity.this, "업로드 성공", Toast.LENGTH_SHORT).show();

                        // 업로드된 동영상의 다운로드 URL을 가져와서 로그에 출력
                        userStorageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            Log.d("DoctorActivity", "Uploaded Video URL: " + downloadUrl);

                            // 업로드 성공 시 PatientListActivity로 이동
                            Intent patientListIntent = new Intent(DoctorActivity.this, PatientListActivity.class);
                            startActivity(patientListIntent);
                            finish(); // 현재 액티비티 종료
                        }).addOnFailureListener(e -> {
                            Log.e("DoctorActivity", "Failed to get download URL", e);
                        });
                    })
                    .addOnFailureListener(e -> {
                        // 업로드 실패
                        Toast.makeText(DoctorActivity.this, "업로드 실패", Toast.LENGTH_SHORT).show();
                    });
        } else {
            // storageRef가 null일 때의 처리
            Toast.makeText(DoctorActivity.this, "Firebase Storage 초기화에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }




    //파일경로
    private String getNewVideoFilePath() {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        String fileName = "video_" + timeStamp + ".mp4";
        return directory + File.separator + fileName;
    }


    // 플로팅 뷰---------------------------------------
    private void startFloatingViewService() {
        // FloatingViewService 시작
        Intent serviceIntent = new Intent(DoctorActivity.this, FloatingViewService.class);
        startService(serviceIntent);
        isServiceStarted = true;
    }
    private void stopFloatingViewService() {
        Intent serviceIntent = new Intent(this, FloatingViewService.class);
        stopService(serviceIntent);
        isServiceStarted = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            // 시스템 오버레이 권한 요청에 대한 결과 처리
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                // 권한이 부여되었으면 FloatingViewService 시작
                startFloatingViewService();
            }
        }
    }
//--------------------------------------------------------------------------

    private void shareScreen() {

        if (mMediaProjection == null) {

            Intent screenCaptureIntent = mProjectionManager.createScreenCaptureIntent();
            screenCaptureLauncher.launch(screenCaptureIntent);
            return;
        }

        mVirtualDisplay = createVirtualDisplay();
        mMediaRecorder.start();
    }
    //녹화할 화면의 가상 디스플레이를 설정
    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay("DoctorActivity",
                //   DISPLAY_WIDTH, DISPLAY_HEIGHT,
                widthPixels,heightPixels,mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mMediaRecorder.getSurface(), null /*Callbacks*/, null
                /*Handler*/);
    }

    private void initRecorder() {
        try {

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); // 출력 형식을 MPEG_4로 변경 화질 개선을 위해.

            // 파일명에 현재 시간의 타임스탬프 추가
            String currentTime = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String outputPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/video_" + currentTime + ".mp4";
            mMediaRecorder.setOutputFile(outputPath);

            mMediaRecorder.setVideoSize(widthPixels,heightPixels);
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setVideoEncodingBitRate(5000000); // 화질 개선을 위해 바꿈
            mMediaRecorder.setVideoFrameRate(60); // 화질 개선을 위해 30으로바꿈
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            int orientation = ORIENTATIONS.get(rotation + 90);
            mMediaRecorder.setOrientationHint(orientation);
            mMediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            if (isRecording = true) {
                isRecording = false;
                mMediaRecorder.stop();
                mMediaRecorder.reset();
            }
            mMediaProjection = null;
            stopScreenSharing();
        }
    }

    private void stopScreenSharing() {
        if (mVirtualDisplay == null) {
            return;
        }
        mVirtualDisplay.release();
        destroyMediaProjection();
    }

    @Override
    public void onDestroy() {

        destroyMediaProjection();
        //리시버 해제
        unregisterReceiver(recordingPauseReceiver);
        unregisterReceiver(recordingStopReceiver);
        super.onDestroy();
    }


    private void destroyMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.unregisterCallback(mMediaProjectionCallback);
            mMediaProjection.stop();
            mMediaProjection = null;
        }

    }

    //권한 요청 메소드
    private void requirePermission(){
        String[] permissions = {Manifest.permission.RECORD_AUDIO,Manifest.permission.WRITE_EXTERNAL_STORAGE};
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int permissionCheck2 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if(permissionCheck == PackageManager.PERMISSION_DENIED || permissionCheck2 == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, permissions, 0);
        }


    }
}