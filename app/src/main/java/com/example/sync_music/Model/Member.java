package com.example.sync_music.Model;

public class Member {
    private String IP;
    private int PORT;
    public Member(){

    }

    public Member(String IP, int PORT) {
        this.IP = IP;
        this.PORT = PORT;
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

    @Override
    public String toString() {
        return "Member{" +
                ", IP='" + IP + '\'' +
                ", PORT='" + PORT + '\'' +
                '}';
    }
}