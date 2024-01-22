package com.example.medifiles;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class VideoListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private VideoListAdapter adapter;
    private List<String> videoList;
    private ImageButton button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_list);

        button = findViewById(R.id.exit);
        button.setOnClickListener(view -> {
            Intent intent = new Intent(VideoListActivity.this, PatientActivity.class);
            startActivity(intent);
            finish();
        });


        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mLayoutManager.setReverseLayout(true);
        mLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(mLayoutManager);

        videoList = new ArrayList<>();
        adapter = new VideoListAdapter(videoList, this::onVideoClick);
        recyclerView.setAdapter(adapter);

        getVideoListFromFirebase();
    }

    private void onVideoClick(String videoUrl, String videoTitle) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("videoUrl", videoUrl);
        startActivity(intent);
    }

    private void getVideoListFromFirebase() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();
            StorageReference videosRef = storage.getReference().child("patients").child(uid).child("videos");

            videosRef.listAll().addOnSuccessListener(listResult -> {
                videoList.clear();
                for (StorageReference item : listResult.getItems()) {
                    item.getDownloadUrl().addOnSuccessListener(uri -> {
                        String videoUrl = uri.toString();
                        String videoTitle = item.getName(); // 동영상의 제목을 가져옴
                        videoList.add(videoUrl);
                        adapter.addVideoTitle(videoTitle);
                        adapter.notifyDataSetChanged();
                    });
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "동영상 목록을 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
