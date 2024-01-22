package com.example.medifiles;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class PatientListActivity extends AppCompatActivity {

    private ListView userListView;
    private ArrayList<String> userList;
    private ArrayAdapter<String> adapter;
    private FirebaseDatabase database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_list);

        userListView = findViewById(R.id.userListView);
        userList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, userList);
        userListView.setAdapter(adapter);

        // Firebase 초기화
        database = FirebaseDatabase.getInstance();

        // role이 환자인 사용자들의 username 가져오기
        DatabaseReference usersRef = database.getReference("users");
        usersRef.orderByChild("role").equalTo("환자").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String username = snapshot.child("username").getValue(String.class);
                    userList.add(username);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // 에러 처리를 여기에 추가하세요.
            }
        });

        // ListView 아이템 클릭 이벤트 처리
        userListView.setOnItemClickListener((parent, view, position, id) -> {
            // 선택한 환자의 UID 가져오기
            getUserUid(position);
        });
    }

    private void getUserUid(int position) {
        // 선택한 환자의 UID 가져오기
        DatabaseReference selectedUserRef = database.getReference("users");
        selectedUserRef.orderByChild("username").equalTo(userList.get(position))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            // 선택한 환자의 UID를 가져옴
                            String selectedPatientUid = snapshot.getKey();
                            // DoctorActivity로 전달
                            Intent doctorIntent = new Intent(PatientListActivity.this, DoctorActivity.class);
                            doctorIntent.putExtra("patientUid", selectedPatientUid);
                            startActivity(doctorIntent);
                            return;  // 중복된 UID를 방지하기 위해 추가
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // 에러 처리를 여기에 추가하세요.
                    }
                });
    }

}
