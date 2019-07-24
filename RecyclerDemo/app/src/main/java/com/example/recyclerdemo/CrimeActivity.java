package com.example.recyclerdemo;

import android.support.v4.app.Fragment;

public class CrimeActivity extends SingleFragmentActivity{
    @Override
    protected Fragment createFragment(){
        System.out.println("FUCK");
        return new CrimeFragment();

    }
}
