package com.example.sync_music;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.sync_music.Model.ChatMessage;
import com.example.sync_music.Model.Member;
import com.example.sync_music.Model.Room;
import com.example.sync_music.Util.initTrueTimeAsyncTask;
import com.example.sync_music.Util.Utils;
import com.firebase.ui.database.FirebaseListAdapter;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.instacart.library.truetime.TrueTime;
import com.medavox.library.mutime.MissingTimeDataException;
import com.medavox.library.mutime.MuTime;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ServerActivity extends AppCompatActivity {
    private RelativeLayout activity_main;
    private LinearLayout linear3;
    private TextView txtRoomName,totalTime,currentTime,txtMusicTitle;
    private Button btnStartStop;
    private EditText input;
    private SeekBar seekBar;
    private ListView listViewSongs;

    private Utils utils;
    private Room room;

    String roomHostID="",IP,roomName;
    String[] items;
    long syncTime=0;
    boolean control=false,control2=false;
    int PORT=60000;

    ArrayAdapter<String> arrayAdapter;

    File selectedFile;
    ExecutorService executorService;
    MediaPlayer mediaPlayer;
    ServerSocket serverSocket;

    DatabaseReference dbRoom;
    FirebaseListAdapter<ChatMessage> adapter;

    ArrayList<Member> members;
    ArrayList<File> mySongs;

    Handler handler;
    Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server);
        initTrueTime(this); //initialize TrueTime
        getIP(); //get IP address
        bindViews(); //Bind to Views
        setRoomName(); //Set Room Name
        loginControl(); //Control to LogIn
        listeners(); //Set Listeners
        listSongs(); //Load songs to Listview
        createRoom(); //CreateRoom
        new connectionTask().execute(); //create a background thread
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId()==R.id.menu_Sign_out){
            FirebaseAuth.getInstance().signOut();
            FirebaseDatabase.getInstance().getReference("chat-room").child(roomName).setValue(null);
            FirebaseDatabase.getInstance().getReference("rooms").child(roomName).setValue(null);
            Intent i = new Intent(ServerActivity.this,MainActivity.class);
            startActivity(i);
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
        try {
            if (serverSocket!=null)
                serverSocket.close();
            if (executorService!=null)
                executorService.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FirebaseAuth.getInstance().signOut();
        FirebaseDatabase.getInstance().getReference("chat-room").child(roomName).setValue(null);
        FirebaseDatabase.getInstance().getReference("rooms").child(roomName).setValue(null);
        FirebaseDatabase.getInstance().getReference("Music").child(roomName).setValue(null);
        super.onDestroy();
    }

    public class connectionTask extends AsyncTask<Void,Void,Void>{
        @Override
        protected Void doInBackground(Void... voids) {
            try {
                serverSocket=new ServerSocket(PORT);
                while (true){
                    executorService.execute(new SocketHandler(serverSocket.accept()));
                }
            } catch (IOException e){
                e.printStackTrace();
            }
            return null;
        }
    }
    private class SocketHandler extends Thread{
        private Socket socket;
        private OutputStream os;
        private InputStream is;
        private SocketHandler(Socket socket)throws IOException{
            this.socket=socket;
            this.os=socket.getOutputStream();
            this.is=socket.getInputStream();
            socket.setSoTimeout(1000000);
        }
        public void run() {
            MuTime.enableDiskCaching(/*Context*/ ServerActivity.this);//this hardens MuTime against clock changes and reboots
            try {
                MuTime.requestTimeFromServer("time.apple.com");//use any ntp server address here, eg "time.apple.com"
            } catch (IOException e) {
                e.printStackTrace();
            }
            addMember(new Member(socket.getInetAddress().getHostAddress(),socket.getPort()));
            try {
                while (true){
                    if (control){
                        if (selectedFile!=null){
                            timeSync(syncTime,os);
                            writeMP3(selectedFile,os);
                            control=false;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            super.run();
        }
        private void writeMP3(File music,OutputStream os) throws IOException{
            FileInputStream fin = new FileInputStream(music);
            BufferedInputStream bis = new BufferedInputStream(fin);
            int fileSize=(int)music.length();
            String fileName=music.getName();
            byte bytes[] = new byte[fileSize];
            byte array[]=null;
            bis.read(bytes,0,bytes.length);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            try {
                dataOutputStream.writeInt(fileSize);
                dataOutputStream.writeUTF(fileName);
                dataOutputStream.write(bytes,0,bytes.length);
                array=outputStream.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
            byte[] dataSize=intToByteArray(array.length);
            os.write(dataSize,0,dataSize.length);
            os.flush();
            os.write(array,0,array.length);
            os.flush();
        }
        private void timeSync(long time, OutputStream os) throws IOException {
            byte[] timeData=new byte[Long.BYTES];
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            try {
                dataOutputStream.writeLong(time);
                timeData=outputStream.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
            os.write(timeData,0,timeData.length);
            os.flush();
        }
    }

    private void bindViews() {
        activity_main=findViewById(R.id.activity1); //Activity
        linear3=findViewById(R.id.linear3);
        txtRoomName=findViewById(R.id.txtRoomName); //Room-Name
        listViewSongs=findViewById(R.id.songList); //Songs-ListView
        input = findViewById(R.id.edtMessage); //Typing-Message
        room = new Room(); //Create-Room
        btnStartStop=findViewById(R.id.btnStartStop);
        txtMusicTitle=findViewById(R.id.txtMusicName);
        totalTime=findViewById(R.id.totalTime);
        currentTime=findViewById(R.id.currentTime);
        seekBar=findViewById(R.id.seekBar);
        utils=new Utils();
        members=new ArrayList<>();
        handler=new android.os.Handler();
        mediaPlayer=new MediaPlayer();
        executorService=Executors.newFixedThreadPool(8);
    }
    private void listeners() {
        listViewSongs.setOnItemClickListener((parent, view, position, id) -> {
            control=true;
            try {
                syncTime=MuTime.now();
                selectedFile=mySongs.get(position);
                startMusic(selectedFile);
                ServerActivity.this.runOnUiThread(() -> {
                    linear3.setVisibility(View.VISIBLE);
                    txtMusicTitle.setText(selectedFile.getName());
                    totalTime.setText(utils.milliSecondsToTimer(mediaPlayer.getDuration()));
                    currentTime.setText(utils.milliSecondsToTimer(mediaPlayer.getCurrentPosition()));
                });
            } catch (MissingTimeDataException e) {
                e.printStackTrace();
            }catch (IOException e) {
                e.printStackTrace();
            }

        });
        input.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode==KeyEvent.KEYCODE_ENTER){
                FABonClick(v);
                return true;
            }
            return false;
        });
        btnStartStop.setOnClickListener(v -> {
            if (mediaPlayer.isPlaying()){
                mediaPlayer.pause();
                btnStartStop.setText(">");
            }
            else{
                btnStartStop.setText("||");
                mediaPlayer.start();
                cycle();
            }
        });
    }

    private void startMusic(final File file) throws IOException {
        ServerActivity.this.runOnUiThread(() -> btnStartStop.setText("||"));
        Uri uri = Uri.parse(file.getPath());
        if (mediaPlayer!=null) mediaPlayer.reset();
        mediaPlayer.setDataSource(getApplicationContext(),uri);
        mediaPlayer.prepareAsync();
        mediaPlayer.setOnPreparedListener(mp -> {
            seekBar.setMax(mp.getDuration());
            mp.start();
            try {
                mediaPlayer.seekTo((int)(MuTime.now()-syncTime));
            } catch (MissingTimeDataException e) {
                e.printStackTrace();
            }
            cycle();
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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
    private void cycle(){
        seekBar.setProgress(mediaPlayer.getCurrentPosition());
        long totalDuration=mediaPlayer.getDuration();
        final long currentDuration=mediaPlayer.getCurrentPosition();
        currentTime.setText(""+utils.milliSecondsToTimer(currentDuration));
        totalTime.setText(""+utils.milliSecondsToTimer(totalDuration));
        if (mediaPlayer.isPlaying()){
            runnable = () -> cycle();
            handler.postDelayed(runnable,1000);
        }
    }
    private void loginControl(){
        if (FirebaseAuth.getInstance().getCurrentUser()==null){
            Intent i = new Intent(ServerActivity.this,MainActivity.class);
            startActivity(i);
        }
        else{
            Snackbar.make(activity_main,"Hoşgeldin "+FirebaseAuth.getInstance().getCurrentUser().getEmail(), Snackbar.LENGTH_SHORT).show();
            displayChatMessage();
        }
    }
    private void createRoom() {
        try {
            String firebaseUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
            dbRoom= FirebaseDatabase.getInstance().getReference("rooms");
            roomHostID = firebaseUserID;
            room=new Room(roomHostID,roomName,IP,PORT,members);
            dbRoom.child(roomName).setValue(room);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    private void setRoomName() {
        txtRoomName.setText(getIntent().getStringExtra("room_name"));
        roomName=txtRoomName.getText().toString();
    }
    public void addMember(Member m) {
        boolean control=false;
        for (int i=0;i<members.size();i++){
            if ((members.get(i).getIP().compareTo(m.getIP()))==0)
                control=true;
        }
        if (!control) members.add(m);
        room.setMembers(members);
        dbRoom.child(roomName).setValue(room);
    }

    public void FABonClick(View v) {
        if (input.getText().length()>0){
            DatabaseReference dbChat = FirebaseDatabase.getInstance().getReference("chat-room");
            dbChat.child(roomName).push().setValue(new ChatMessage(input.getText().toString(), FirebaseAuth.getInstance().getCurrentUser().getEmail()));
            input.setText("");
        }

    }
    private void displayChatMessage() {
        ListView list = findViewById(R.id.list);
        adapter=new FirebaseListAdapter<ChatMessage>(this,ChatMessage.class,R.layout.list_item,FirebaseDatabase.getInstance().getReference("chat-room").child(roomName)) {
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
    private void listSongs() {
        mySongs=findSongs(Environment.getExternalStorageDirectory());
        items=new String[mySongs.size()];
        for (int i=0;i<mySongs.size();i++){
            items[i]=mySongs.get(i).getName().toString().replace(".mp3","").replace("(Official Video)","");
        }
        arrayAdapter=new ArrayAdapter<String>(getApplicationContext(),R.layout.songlist_item,R.id.textMusic,items);
        listViewSongs.setAdapter(arrayAdapter);
    }

    private void initTrueTime(Context ctx) {
        if (isNetworkConnected(ctx)) {
            if (!TrueTime.isInitialized()) {
                initTrueTimeAsyncTask trueTime = new initTrueTimeAsyncTask(ctx);
                trueTime.execute();
            }
        }
    } //TrueTime initialization
    public static boolean isNetworkConnected(Context ctx) {
        ConnectivityManager cm = (ConnectivityManager) ctx
                .getSystemService (Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();

        return ni != null && ni.isConnectedOrConnecting();
    } // Network Control
    public static Date getTrueTime() {
        Date date = TrueTime.isInitialized() ? TrueTime.now() : new Date();
        return date;
    } //get Date

    private void getIP() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        IP = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
    } //get IP address from Wifi Service

    private byte[] intToByteArray ( final int i ) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.writeInt(i);
        dos.flush();
        return bos.toByteArray();
    }

}
