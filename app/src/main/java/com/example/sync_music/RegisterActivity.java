package com.example.sync_music;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sync_music.Model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {
    private TextView txtMale,txtFemale,txtTitle;
    private Button customSignupButton;
    private EditText nameEditText,emailEditText, passwordEditText1,passwordEditText2,ageEditText;
    private ProgressBar progressbar;
    private CheckBox checkbox_male,checkBox_female;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private String gender=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        bindViews();
        listeners();
    }
    private void listeners() {
        customSignupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Register_User();
            }
        });
        checkbox_male.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (checkbox_male.isChecked()) gender="male";
                else gender="";
                if(checkBox_female.isEnabled()){
                    checkBox_female.setEnabled(false);
                    txtFemale.setEnabled(false);
                }
                else {
                    checkBox_female.setEnabled(true);
                    txtFemale.setEnabled(true);
                }
            }
        });
        checkBox_female.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (checkBox_female.isChecked()) gender="female";
                else gender="";
                if(checkbox_male.isEnabled()){
                    checkbox_male.setEnabled(false);
                    txtMale.setEnabled(false);
                }
                else{
                    checkbox_male.setEnabled(true);
                    txtMale.setEnabled(true);
                }

            }
        });
    }
    private void bindViews() {
        customSignupButton = findViewById(R.id.custom_signup_button);
        emailEditText = findViewById(R.id.email_edittext);
        passwordEditText1 =  findViewById(R.id.password_edittext1);
        passwordEditText2 =  findViewById(R.id.password_edittext2);
        ageEditText=findViewById(R.id.age_edittext);
        firebaseAuth=FirebaseAuth.getInstance();
        databaseReference=FirebaseDatabase.getInstance().getReference("users");
        progressbar=findViewById(R.id.progressbar);
        checkBox_female=findViewById(R.id.checkbox_female);
        checkbox_male=findViewById(R.id.checkbox_male);
        txtFemale=findViewById(R.id.txtFemale);
        txtMale=findViewById(R.id.txtMale);
        nameEditText=findViewById(R.id.name_edittext);
    }
    private void Register_User() {
        if (nameEditText.getText().toString().isEmpty()){
            nameEditText.setError("İsim alanı boş bırakılamaz.");
            nameEditText.requestFocus();
        }
        if (emailEditText.getText().toString().isEmpty()){
            emailEditText.setError("Email alanı boş bırakılamaz.");
            emailEditText.requestFocus();
            return;
        }
        if (passwordEditText1.getText().toString().isEmpty()){
            passwordEditText1.setError("Şifre alanı boş bırakılamaz.");
            passwordEditText1.requestFocus();
            return;
        }
        if (passwordEditText2.getText().toString().isEmpty()){
            passwordEditText2.setError("Şifre alanı boş bırakılamaz.");
            passwordEditText2.requestFocus();
            return;
        }
        if (passwordEditText1.getText().toString().compareTo(passwordEditText2.getText().toString())<0){
            passwordEditText2.setError("Şifreler eşleşmiyor.");
            passwordEditText2.requestFocus();
            return;
        }
        if (ageEditText.getText().toString().isEmpty()){
            ageEditText.setError("Yaşınızı giriniz.");
            ageEditText.requestFocus();
            return;
        }
        if(!checkbox_male.isChecked() && !checkBox_female.isChecked()){
            Toast.makeText(RegisterActivity.this,"Cinsiyetinizi seçiniz.",Toast.LENGTH_LONG).show();
            return;
        }

        progressbar.setVisibility(View.VISIBLE);
        firebaseAuth.createUserWithEmailAndPassword(emailEditText.getText().toString(),passwordEditText1.getText().toString())
                .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            progressbar.setVisibility(View.GONE);
                            FirebaseUser firebaseUser=firebaseAuth.getCurrentUser();
                            String id=firebaseUser.getUid();
                            User user = new User(id,nameEditText.getText().toString(),emailEditText.getText().toString(),passwordEditText1.getText().toString(),Integer.parseInt(ageEditText.getText().toString()),gender);
                            databaseReference.child(id).setValue(user);
                            Toast.makeText(RegisterActivity.this,"Authentication success",Toast.LENGTH_LONG).show();
                            Intent i = new Intent(RegisterActivity.this,MainActivity.class);
                            startActivity(i);
                        }
                        else{
                            if (task.getException() instanceof FirebaseAuthUserCollisionException)
                                Toast.makeText(RegisterActivity.this,"Zaten kayıtlısınız.",Toast.LENGTH_LONG).show();
                            else
                                Toast.makeText(RegisterActivity.this,task.getException().getMessage(),Toast.LENGTH_LONG).show();

                        }
                    }
                });

    }
}
