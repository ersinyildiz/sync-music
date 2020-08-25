package com.example.sync_music.Model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class Room implements Parcelable {
    private String hostID;
    private String roomName;
    private String IP;
    private int PORT;
    private ArrayList<Member> members;
    public Room(){

    }
    public Room(String hostID, String roomName, String IP, int PORT, ArrayList<Member> members) {
        this.hostID = hostID;
        this.roomName = roomName;
        this.IP = IP;
        this.PORT = PORT;
        this.members = members;
    }

    //Parcelable Implementation
    protected Room(Parcel in) {
        hostID = in.readString();
        roomName = in.readString();
        IP = in.readString();
        PORT = in.readInt();
    }
    public static final Creator<Room> CREATOR = new Creator<Room>() {
        @Override
        public Room createFromParcel(Parcel in) {
            return new Room(in);
        }

        @Override
        public Room[] newArray(int size) {
            return new Room[size];
        }
    };
    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(hostID);
        dest.writeString(roomName);
        dest.writeString(IP);
        dest.writeInt(PORT);
    }

    public String getHostID() {
        return hostID;
    }
    public void setHostID(String hostID) {
        this.hostID = hostID;
    }
    public String getRoomName() {
        return roomName;
    }
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }
    public String getIP() {
        return IP;
    }
    public void setIP(String IP) {
        this.IP = IP;
    }
    public int getPORT() {
        return PORT;
    }
    public void setPORT(int PORT) {
        this.PORT = PORT;
    }
    public ArrayList<Member> getMembers() {
        return members;
    }
    public void setMembers(ArrayList<Member> members) {
        this.members = members;
    }
    @Override
    public String toString() {
        return "Room{" +
                "roomName='" + roomName + '\'' +
                ", IP='" + IP + '\'' +
                ", PORT='" + PORT + '\'' +
                '}';
    }


}