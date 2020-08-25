package com.example.sync_music;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sync_music.Model.Room;
import com.example.sync_music.Model.User;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class PassActivity extends AppCompatActivity {
    private RelativeLayout passActivity;
    private String TAG="ersiny";
    private EditText edtRoomName;
    private Button btnCreateRoom;
    private ListView roomList;
    private DatabaseReference dbRoom;
    private List<String> listRoom;
    private Context mContext;
    private Room room;
    private Intent intent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pass);
        mContext = this;
        bindViews();
        listeners();
        setListData();
    }
    private void setListData() {
        FirebaseListAdapter<Room> roomAdapter=new FirebaseListAdapter<Room>(this,Room.class,R.layout.list_item_room,dbRoom) {
            @Override
            protected void populateView(View v, Room model, int position) {
                TextView textRoomName;
                textRoomName=v.findViewById(R.id.room_name);
                textRoomName.setText(model.getRoomName());
            }
        };
        roomList.setAdapter(roomAdapter);
        roomList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                room = (Room)parent.getItemAtPosition(position);
                intent=new Intent(PassActivity.this,ClientActivity.class);
                intent.putExtra("ROOM",room);
                Snackbar.make(passActivity,room.getRoomName()+" odasına giriş yapılıyor..", Snackbar.LENGTH_SHORT).show();
                startActivity(intent);
            }
        });
    }
    private void listeners() {
        btnCreateRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edtRoomName.getText().toString().isEmpty()){
                    edtRoomName.setError("Oda adı giriniz!");
                    edtRoomName.requestFocus();
                }
                else{
                    Intent i=new Intent(PassActivity.this,ServerActivity.class);
                    i.putExtra("room_name",edtRoomName.getText().toString());
                    startActivity(i);
                }
            }
        });
    }
    private void bindViews() {
        passActivity=findViewById(R.id.passActivity);
        edtRoomName = findViewById(R.id.roomNameEditText);
        btnCreateRoom = findViewById(R.id.btnCreateRoom);
        roomList=findViewById(R.id.roomListView);
        dbRoom=FirebaseDatabase.getInstance().getReference("rooms").child("");
        listRoom=new ArrayList<String>();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==R.id.menu_Sign_out){
            FirebaseAuth.getInstance().signOut();
            Intent i = new Intent(PassActivity.this,MainActivity.class);
            startActivity(i);
        }
        return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }
}
