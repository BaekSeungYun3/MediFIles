package com.example.medifiles;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class SignUp_Activity extends AppCompatActivity {

    private FirebaseAuth auth;

    EditText username, email, password;
    Button signupBtn;
    TextView loginRedirectText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        username = findViewById(R.id.signup_username);
        email = findViewById(R.id.signup_email);
        password = findViewById(R.id.signup_password);
        signupBtn = findViewById(R.id.signup_button);
        loginRedirectText = findViewById(R.id.loginRedirectText);

        loginRedirectText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignUp_Activity.this, Login_Activity.class));
            }
        });

        signupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String str_username = username.getText().toString();
                String str_email = email.getText().toString();
                String str_password = password.getText().toString();

                // 비밀번호 유효성 검사를 위한 정규표현식
                String passwordPattern = "^(?=.*[A-Z])(?=.*[!@#$%^&*+=?-]).+$";

                if (str_username.isEmpty()) {
                    username.setError("이름을 입력해주세요!");
                } else if (str_email.isEmpty()) {
                    email.setError("이메일을 입력해주세요!");
                } else if (str_password.isEmpty()) {
                    password.setError("비밀번호를 입력해주세요!");
                } else if (!str_password.matches(passwordPattern)) {
                    password.setError("비밀번호는 최소 한 개의 대문자와 특수문자를 포함해야 합니다!");
                } else {
                    auth.createUserWithEmailAndPassword(str_email, str_password)
                            .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                                @Override
                                public void onComplete(@NonNull Task<AuthResult> task) {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(SignUp_Activity.this, "회원가입 성공!", Toast.LENGTH_SHORT).show();

                                        FirebaseUser user = auth.getCurrentUser();
                                        if (user != null) {
                                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                    .setDisplayName(str_username)
                                                    .build();
                                            user.updateProfile(profileUpdates);

                                            saveUserToFirebase(user.getUid(), str_email, str_username);
                                        }

                                        startActivity(new Intent(SignUp_Activity.this, Login_Activity.class));
                                    } else {
                                        if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                                            Toast.makeText(SignUp_Activity.this, "이미 등록된 이메일입니다.", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(SignUp_Activity.this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                            });
                }
            }
        });
    }

    private void saveUserToFirebase(String userId, String email, String username) {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference userRef = database.getReference("users").child(userId);

            Map<String, Object> userData = new HashMap<>();
            userData.put("email", email);
            userData.put("username", username);

            RadioGroup radioGroupRole = findViewById(R.id.useGroup);
            int selectedRoleId = radioGroupRole.getCheckedRadioButtonId();

            if (selectedRoleId == R.id.Doctor) {
                userData.put("role", "의사");
            } else if (selectedRoleId == R.id.Patient) {
                userData.put("role", "환자");
            }

            userRef.setValue(userData);
        }
    }
}
