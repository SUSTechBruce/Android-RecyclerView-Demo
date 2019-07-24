package com.example.recyclerdemo;


import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CrimeLab {
    private static CrimeLab sCrimeLab;
    private List<Crime> mCrimes;
    private CrimeLab(Context context){
        mCrimes = new ArrayList<>();
        for(int i = 0;i<16;i++){

            Crime crime = new Crime();
            if(i == 0){
                crime.setnID(i);
                crime.setmTitle("All");
                crime.setmSovled(false);
                mCrimes.add(crime);
            }else{
                crime.setnID(i);
                crime.setmTitle("Authority #"+i);
                crime.setmSovled(false);
                mCrimes.add(crime);
            }

        }

    }

    public static CrimeLab get(Context context){
        if(sCrimeLab == null){
            sCrimeLab = new CrimeLab(context);

        }
        return sCrimeLab;
    }

    public List<Crime>getmCrimes(){return mCrimes;}
    public Crime getCrime(UUID id){
        for(Crime crime: mCrimes){
            if(crime.getmId().equals(id)){
                return crime;
            }
        }
        return null;
    }
}
