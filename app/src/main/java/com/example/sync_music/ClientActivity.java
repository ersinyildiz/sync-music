package com.example.sync_music;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;

import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.sync_music.Model.ChatMessage;
import com.example.sync_music.Model.Room;
import com.example.sync_music.Util.initTrueTimeAsyncTask;
import com.example.sync_music.Util.Utils;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.instacart.library.truetime.TrueTime;
import com.medavox.library.mutime.MissingTimeDataException;
import com.medavox.library.mutime.MuTime;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

public class ClientActivity extends AppCompatActivity {
    private RelativeLayout activity_main;
    private LinearLayout playingLayout;
    private TextView txtRoomName,txtMusicName,totalTime,currentTime;
    private EditText input;
    private SeekBar seekbar;
    private Button btnSync,btnStartStop;

    private Room room;
    private Utils utils;

    MediaPlayer mediaPlayer;
    Socket socket;

    HashSet<String> songs= new HashSet<>();

    FirebaseListAdapter<ChatMessage> adapter;

    Handler handler;
    Runnable runnable;

    long time=0;
    long gecikme=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        initTrueTime(this);
        bindViews();
        setRoomName();
        listeners();
        loginControl();
        getSongsOnThePhone();
        new connectionTask().execute();
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==R.id.menu_Sign_out){
            FirebaseAuth.getInstance().signOut();
            if (mediaPlayer.isPlaying()){ mediaPlayer.stop(); mediaPlayer.release();}
            Intent i = new Intent(ClientActivity.this,MainActivity.class);
            startActivity(i);
            finish();
        }
        return true;
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mediaPlayer!=null) mediaPlayer.release();
    }

    public class connectionTask extends AsyncTask<Void,Void,Void> {
        BufferedOutputStream bos = null;
        InputStream is = null;
        OutputStream os = null;
        DataInputStream dis = null;
        File music;
        @Override
        protected Void doInBackground(Void... voids) {
            MuTime.enableDiskCaching(ClientActivity.this);//this hardens MuTime against clock changes and reboots
            try {
                MuTime.requestTimeFromServer("time.apple.com");//use any ntp server address here, eg "time.apple.com"
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                connect(); //connect to server
                byte[] data;
                while (true) {
                    timeSync();
                    data = receiveMusicData(); //Wait for data from server
                    music = extractData(data); //Extract data(Name,Size..)-(return music file)
                    startMusic(music);
                }
            }catch(IOException e){
                e.printStackTrace();
            }
            return null;
        }
        private void timeSync() throws IOException {
            byte[] timeData=new byte[Long.BYTES];
            is.read(timeData,0,timeData.length);
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(timeData);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            time = dataInputStream.readLong();
            Log.v("ersiny","start-time"+time);
            byteArrayInputStream.close();
            dataInputStream.close();
        }
        private void connect() throws IOException {
            socket = new Socket(room.getIP(), room.getPORT());
            is = socket.getInputStream();
            os = socket.getOutputStream();
            dis = new DataInputStream(is);
        }
        private byte[] receiveMusicData() throws IOException {
            byte[] buffer = new byte[32 * 1024];
            byte[] dataSize=new byte[4];
            byte[] file;
            int bytesRead;
            int size,n=0;
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            is.read(dataSize,0,dataSize.length);
            size=selectFileSize(dataSize);
            while ((bytesRead = is.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, bytesRead);
                baos.flush();
                size-=bytesRead;
                if (size<=0) break;
            }
            file = baos.toByteArray();
            return file;
        }
        private int selectFileSize(byte[] file) throws IOException {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(file);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            int fileSize = dataInputStream.readInt();
            byteArrayInputStream.close();
            dataInputStream.close();
            return fileSize;
        }
        private File extractData(byte[] file) throws IOException {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(file);
            DataInputStream dataInputStream = new DataInputStream(byteArrayInputStream);
            int fileSize = dataInputStream.readInt();
            String fileName = dataInputStream.readUTF();
            dataInputStream.read(file, 0, fileSize);
            byteArrayInputStream.close();
            dataInputStream.close();
            File music = saveMusic(file, fileName);
            return music;
        }
        private File saveMusic(byte[] bytes, String fileName) throws IOException {
            File publicMusicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            String publicMusicPath = publicMusicDir.getAbsolutePath();
            File musicFile = new File(publicMusicPath, fileName);
            int listSize = songs.size();
            songs.add(fileName);
            int afterSavedListSize = songs.size();
            if (afterSavedListSize > listSize) {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(musicFile, true));
                bos.write(bytes, 0, bytes.length);
                bos.close();
            }
            return musicFile;
        }
    }

    private void bindViews() {
        activity_main=findViewById(R.id.client_activity);
        playingLayout=findViewById(R.id.playingLayout);
        txtRoomName=findViewById(R.id.txtRoomName);
        txtMusicName=findViewById(R.id.txtMusicName);
        btnSync = findViewById(R.id.btnSync);
        btnStartStop=findViewById(R.id.btnStartStop);
        seekbar=findViewById(R.id.seekBar);
        totalTime=findViewById(R.id.totalTime);
        currentTime=findViewById(R.id.currentTime);
        input = findViewById(R.id.edtMessage);
        handler = new Handler();
        utils=new Utils();
        mediaPlayer=new MediaPlayer();
        room=(Room)getIntent().getParcelableExtra("ROOM");
    }
    private void listeners() {
        input.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode==KeyEvent.KEYCODE_ENTER){
                FABonClick(v);
                return true;
            }
            return false;
        });
        DatabaseReference dbRoom=FirebaseDatabase.getInstance().getReference("rooms").child(room.getRoomName());
        dbRoom.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }
            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                if(mediaPlayer!=null) mediaPlayer.release();
                Snackbar.make(activity_main,"Oda kurucusu tarafından kapatıldığı için anasayfaya yönlendiriliyorsunuz!",Snackbar.LENGTH_LONG).show();
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    // Actions to do after 3 seconds
                    finish();
                }, 3000);

            }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
        btnSync.setOnClickListener(v -> {
            try {
                mediaPlayer.seekTo((int)(MuTime.now()-time));
                seekbar.setProgress(mediaPlayer.getCurrentPosition());
            } catch (MissingTimeDataException e) {
                e.printStackTrace();
            }
        });
        btnStartStop.setOnClickListener(v -> {
            ClientActivity.this.runOnUiThread(() ->{
                if (mediaPlayer.isPlaying()){
                    mediaPlayer.pause();
                    btnStartStop.setText(">");
                }
                else{
                    mediaPlayer.start();
                    btnStartStop.setText("||");
                }
            });
        });
    }

    private void getSongsOnThePhone() {
        ArrayList<File> musics=findSongs(Environment.getExternalStorageDirectory());
        if (musics.size()>0){
            for (int i=0;i<musics.size();i++){
                songs.add(musics.get(i).getName());
            }
        }

    }
    public ArrayList<File> findSongs(File root){
        ArrayList<File> resultList=new ArrayList<File>();
        File[] files = root.listFiles();
        for (File singleFile:files){
            if (singleFile.isDirectory() && !singleFile.isHidden()){
                resultList.addAll(findSongs(singleFile));
            }
            else{
                if (singleFile.getName().endsWith(".mp3")){
                    resultList.add(singleFile);
                }
            }
        }
        return resultList;
    }
    private void startMusic(final File file) throws IOException {
        ClientActivity.this.runOnUiThread(() -> {
            playingLayout.setVisibility(View.VISIBLE);
            btnStartStop.setText("||");
            txtMusicName.setText(file.getName());
        });
        Uri uri = Uri.parse(file.getPath());
        if (mediaPlayer!=null) mediaPlayer.reset();
        mediaPlayer.setDataSource(getApplicationContext(),uri);
        mediaPlayer.prepareAsync();
        mediaPlayer.setOnPreparedListener(mediaPlayer -> {
            seekbar.setMax(mediaPlayer.getDuration());
            mediaPlayer.start();
            try {
                mediaPlayer.seekTo((int) (MuTime.now()-time+100));
            } catch (MissingTimeDataException e) {
                e.printStackTrace();
            }
            changeSeekBar();

        });
        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser){
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }
    private void changeSeekBar(){
        seekbar.setProgress(mediaPlayer.getCurrentPosition());
        long totalDuration=mediaPlayer.getDuration();
        long currentDuration=mediaPlayer.getCurrentPosition();
        currentTime.setText(""+utils.milliSecondsToTimer(currentDuration));
        totalTime.setText(""+utils.milliSecondsToTimer(totalDuration));
        if (mediaPlayer.isPlaying()){
            runnable = () -> {
                changeSeekBar();
            };
            handler.postDelayed(runnable,1000);
        }
    }

    private void loginControl() {
        if (FirebaseAuth.getInstance().getCurrentUser()==null){
            Intent i = new Intent(ClientActivity.this,MainActivity.class);
            startActivity(i);
        }
        else{
            Snackbar.make(activity_main,"Hoşgeldin "+FirebaseAuth.getInstance().getCurrentUser().getEmail(),Snackbar.LENGTH_SHORT).show();
            displayChatMessage();
        }
    }
    private void setRoomName() {
        txtRoomName.setText(room.getRoomName());
    }

    private void displayChatMessage() {
        ListView list = findViewById(R.id.list);
        adapter=new FirebaseListAdapter<ChatMessage>(this,ChatMessage.class,R.layout.list_item,FirebaseDatabase.getInstance().getReference("chat-room").child(room.getRoomName())) {
            @Override
            protected void populateView(View v, ChatMessage model, int position) {
                TextView messageUser,messageTime,messageText;
                messageText=v.findViewById(R.id.message_text);
                messageTime=v.findViewById(R.id.message_time);
                messageUser=v.findViewById(R.id.message_user);
                messageText.setText(model.getMessageText());
                messageUser.setText(model.getMessageUser());
                messageTime.setText(DateFormat.format("HH:mm:ss",model.getMessageTime()));
            }
        };
        list.setAdapter(adapter);
    }
    public void FABonClick(View v) {
        if (input.getText().length()>0){
            DatabaseReference dbChat = FirebaseDatabase.getInstance().getReference("chat-room");
            dbChat.child(room.getRoomName()).push().setValue(new ChatMessage(input.getText().toString(), FirebaseAuth.getInstance().getCurrentUser().getEmail()));
            input.setText("");
        }

    }

    private void initTrueTime(Context ctx) {
        if (isNetworkConnected(ctx)) {
            if (!TrueTime.isInitialized()) {
                initTrueTimeAsyncTask trueTime = new initTrueTimeAsyncTask(ctx);
                trueTime.execute();
            }
        }
    }
    public static boolean isNetworkConnected(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx
                .getSystemService (Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();

        return ni != null && ni.isConnectedOrConnecting();
    }
    public static Date getTrueTime() {
        Date date = TrueTime.isInitialized() ? TrueTime.now() : new Date();
        return date;
    }

}
