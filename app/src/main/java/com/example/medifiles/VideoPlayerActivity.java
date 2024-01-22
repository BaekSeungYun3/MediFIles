package com.example.medifiles;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("videoUrl")) {
            String videoUrl = intent.getStringExtra("videoUrl");
            openVideoWithDefaultPlayer(videoUrl);
        } else {
            Toast.makeText(this, "비디오 URL이 없습니다.", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void openVideoWithDefaultPlayer(String videoUrl) {
        try {
            Uri videoUri = Uri.parse(videoUrl);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(videoUri, "video/*");

            // 새로운 태스크를 시작하여 새로운 액티비티에서 동영상을 열도록 함
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            startActivity(intent);
            finish(); // 현재 액티비티를 종료하여 뒤로가기 버튼이 새로 열린 액티비티로 돌아가지 않도록 함
        } catch (Exception e) {
            Toast.makeText(this, "기본 비디오 플레이어를 열 수 없습니다.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            finish();
        }
    }
}
