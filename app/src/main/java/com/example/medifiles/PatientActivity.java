package com.example.medifiles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.cardview.widget.CardView;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class PatientActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private FirebaseUser firebaseUser;
    private DatabaseReference reference;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient);

        recyclerView = findViewById(R.id.recyclerViewChat);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        reference = FirebaseDatabase.getInstance().getReference("chats").child(firebaseUser.getUid());

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                messageList.clear();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Message message = snapshot.getValue(Message.class);
                    messageList.add(message);
                }

                messageAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // 데이터베이스 읽기에 실패한 경우 처리할 내용
            }
        });

        // CardView 초기화
        CardView cardViewQnA = findViewById(R.id.qna);
        CardView cardViewVideo = findViewById(R.id.video);

        // CardView에 터치 이벤트 리스너 추가
        cardViewQnA.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 손가락이 눌린 상태
                        cardViewQnA.setCardBackgroundColor(getResources().getColor(R.color.purple_200));
                        break;
                    case MotionEvent.ACTION_UP:
                        // 손가락이 떼진 상태
                        cardViewQnA.setCardBackgroundColor(getResources().getColor(R.color.white));
                        // 클릭 이벤트 처리
                        onCardViewClick(cardViewQnA);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        // 터치 이벤트가 취소된 경우
                        cardViewQnA.setCardBackgroundColor(getResources().getColor(R.color.white));
                        break;
                }
                return true;
            }
        });

        cardViewVideo.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 손가락이 눌린 상태
                        cardViewVideo.setCardBackgroundColor(getResources().getColor(R.color.purple_200));
                        break;
                    case MotionEvent.ACTION_UP:
                        // 손가락이 떼진 상태
                        cardViewVideo.setCardBackgroundColor(getResources().getColor(R.color.white));
                        // 클릭 이벤트 처리
                        onCardViewClick(cardViewVideo);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        // 터치 이벤트가 취소된 경우
                        cardViewVideo.setCardBackgroundColor(getResources().getColor(R.color.white));
                        break;
                }
                return true;
            }
        });
    }

    public void onCardViewClick(View view) {
        int cardId = view.getId();
        if (cardId == R.id.qna) {
            Intent intent = new Intent(this, Chat.class);
            startActivity(intent);
        } else if (cardId == R.id.video) {
            Intent intent = new Intent(this, VideoListActivity.class);
            startActivity(intent);
        }
    }
}
