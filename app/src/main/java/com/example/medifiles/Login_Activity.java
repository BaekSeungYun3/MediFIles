// Login_Activity.java
package com.example.medifiles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Login_Activity extends AppCompatActivity {

    private FirebaseAuth auth;

    EditText email;
    EditText password;
    Button loginButton;
    TextView signupRedirectText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        signupRedirectText = findViewById(R.id.signupText);

        signupRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Login_Activity.this, SignUp_Activity.class));
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str_email = email.getText().toString();
                String str_password = password.getText().toString();

                if (!str_email.isEmpty() && Patterns.EMAIL_ADDRESS.matcher(str_email).matches()) {
                    if (!str_password.isEmpty()) {
                        auth.signInWithEmailAndPassword(str_email, str_password)
                                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                                    @Override
                                    public void onSuccess(AuthResult authResult) {
                                        Toast.makeText(Login_Activity.this, "로그인 되었습니다!", Toast.LENGTH_SHORT).show();
                                        getUserRoleAndRedirect(authResult.getUser());
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(Login_Activity.this, "로그인 실패", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        password.setError("비밀번호를 입력해 주세요!");
                    }
                } else if (str_email.isEmpty()) {
                    email.setError("이메일을 입력해 주세요!");
                } else {
                    email.setError("유효한 이메일을 입력해 주세요!");
                }
            }
        });
    }

    private void getUserRoleAndRedirect(FirebaseUser user) {
        if (user != null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference userRef = database.getReference("users").child(user.getUid()).child("role");

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    String role = snapshot.getValue(String.class);

                    if (role != null) {
                        if (role.equals("의사")) {
                            startActivity(new Intent(Login_Activity.this, PatientListActivity.class)
                                    .putExtra("useremail", user.getEmail()));
                        } else if (role.equals("환자")) {
                            startActivity(new Intent(Login_Activity.this, PatientActivity.class)
                                    .putExtra("useremail", user.getEmail()));
                        } else {
                            // 다른 역할에 대한 처리
                        }
                        finish();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // 에러 처리
                }
            });
        }
    }
}
