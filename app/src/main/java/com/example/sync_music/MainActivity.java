package com.example.sync_music;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private Button customSigninButton, customSignupButton;
    private EditText emailEditText, passwordEditText;
    private FirebaseAuth firebaseAuth;
    private String email,password;
    private ProgressBar progressBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindViews();
        setListeners();
    }

    private void setListeners() {
        customSigninButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform custom sign in
                email=emailEditText.getText().toString();
                password=passwordEditText.getText().toString();
                if (emailEditText.getText().toString().isEmpty()){
                    emailEditText.setError("Email alanı boş bırakılamaz!");
                    emailEditText.requestFocus();
                    return;
                }
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    emailEditText.setError("Lütfen geçerli bir adres giriniz!");
                    emailEditText.requestFocus();
                    return;
                }
                if (passwordEditText.getText().toString().isEmpty()){
                    passwordEditText.setError("Şifre alanı boş bırakılamaz!");
                    passwordEditText.requestFocus();
                    return;
                }
                if (passwordEditText.getText().toString().length()<6){
                    passwordEditText.setError("Şifre minimum 6 karakter uzunluğunda olmalı!");
                    passwordEditText.requestFocus();
                    return;
                }
                progressBar.setVisibility(View.VISIBLE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    progressBar.setTooltipText("Giriş yapılıyor");
                }
                firebaseAuth.signInWithEmailAndPassword(email,password).addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            progressBar.setVisibility(View.GONE);
                            FirebaseUser firebaseUser=firebaseAuth.getCurrentUser();
                            Toast.makeText(MainActivity.this,"User logged in!",Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(MainActivity.this,PassActivity.class);
                            startActivity(intent);
                        }
                        else{
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this,"Something wrong!",Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });

        customSignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Perform custom sign up
                Intent intent = new Intent(MainActivity.this,RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    private void bindViews() {
        customSigninButton =  findViewById(R.id.custom_signin_button);
        customSignupButton = findViewById(R.id.custom_signup_button);
        emailEditText = findViewById(R.id.email_edittext);
        passwordEditText =  findViewById(R.id.password_edittext);
        progressBar=findViewById(R.id.progressbar);
        firebaseAuth=FirebaseAuth.getInstance();
    }

}