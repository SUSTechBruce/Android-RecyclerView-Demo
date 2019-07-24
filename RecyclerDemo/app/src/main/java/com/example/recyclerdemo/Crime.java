package com.example.recyclerdemo;

import java.util.UUID;
import java.util.Date;
public class Crime {
    private UUID mId;
    private String mTitle;
    private Date mData;
    private boolean mSovled;
    private int nID;

    public void setmSovled(boolean mSovled){
        this.mSovled = mSovled;
    }

    public void setnID(int nID) {
        this.nID = nID;
    }

    public int getnID() {

        return nID;
    }

    public Crime(){

        this.mId = UUID.randomUUID();
        this.mData = new Date();

    }

    public void setmId(UUID mId) {
        this.mId = mId;
    }

    public boolean ismSovled() {

        return mSovled;
    }

    public void setmTitle(String mTitle) {
        this.mTitle = mTitle;
    }

    public void setmData(Date mData) {
        this.mData = mData;
    }

    public Date getmData() {

        return mData;
    }

    public UUID getmId() {
        return mId;
    }

    public String getmTitle() {
        return mTitle;
    }
}
