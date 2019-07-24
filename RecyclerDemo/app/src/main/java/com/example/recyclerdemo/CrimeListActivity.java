package com.example.recyclerdemo;


import android.support.v4.app.Fragment;

public class CrimeListActivity extends SingleListFragmentActivity{
    @Override
    protected Fragment createFragment(){
        return new CrimeListFragment();
    }
}
