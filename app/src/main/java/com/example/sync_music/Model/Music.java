package com.example.sync_music.Model;

public class Music {
    private String musicTitle;
    private long currentTime;
    private boolean isMusicPlaying;
    public Music(String musicTitle, long currentTime) {
        this.musicTitle = musicTitle;
        this.currentTime = currentTime;
    }
    public Music(){

    }
    public Music(String musicTitle) {
        this.musicTitle = musicTitle;
    }
    public String getMusicTitle() {
        return musicTitle;
    }

    public boolean isMusicPlaying() {
        return isMusicPlaying;
    }

    public void setMusicPlaying(boolean musicPlaying) {
        isMusicPlaying = musicPlaying;
    }

    public long getCurrentTime() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }
}
