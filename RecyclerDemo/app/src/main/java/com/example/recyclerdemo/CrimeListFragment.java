package com.example.recyclerdemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

public class CrimeListFragment extends Fragment {
    private RecyclerView mCrimeRecycleView;
    private CrimeAdapter mAdapter;
    private int lastOffset = 0;
    private int lastPosition = 0;
    CrimeLab crimeLab = CrimeLab.get(getActivity());
    List<Crime> crimes = crimeLab.getmCrimes();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_crime_list, container, false);
        mCrimeRecycleView = view.findViewById(R.id.crime_recycle_view);
        mCrimeRecycleView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if(recyclerView.getLayoutManager() != null) {
                    getPositionAndOffset();
                }
            }
        });
        scrollToPosition();
        mCrimeRecycleView.setLayoutManager(new LinearLayoutManager(getActivity()));
        onResume();
        return view;
    }
    private void getPositionAndOffset(){
        LinearLayoutManager layoutManager = (LinearLayoutManager) mCrimeRecycleView.getLayoutManager();
        //获取可视的第一个view
        View topView = layoutManager.getChildAt(0);
        if(topView != null) {
            //获取与该view的顶部的偏移量
            lastOffset = topView.getTop();
            //得到该View的数组位置
            lastPosition = layoutManager.getPosition(topView);
        }
    }
    private void scrollToPosition() {
        if(mCrimeRecycleView.getLayoutManager() != null && lastPosition >= 0) {
            ((LinearLayoutManager) mCrimeRecycleView.getLayoutManager()).scrollToPositionWithOffset(lastPosition, lastOffset);
        }
    }
    @Override
    public void onResume() {
        super.onResume();
        updateUI();

    }

    private class CrimeHolder extends RecyclerView.ViewHolder implements View.OnClickListener { // 定义ViewHolder内部类
        //public TextView mTitleTextView;
        private TextView mTitleTextView;
        private TextView mDataTextView;
        private CheckBox mSolvedCheckBox;
        private Crime mCrime;
        private Switch aSwitch;

        @Override
        public void onClick(View v){
            Intent intent = new Intent(getActivity(), CrimeActivity.class);
            startActivity(intent);
        }

        private CrimeHolder(View itemView) {
            super(itemView);
            mTitleTextView = itemView.findViewById(R.id.list_item_crime_title_text_view);
            mDataTextView = itemView.findViewById(R.id.list_item_crime_date_text_view);
            itemView.setOnClickListener(this);
            aSwitch = itemView.findViewById(R.id.switch1);
            aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (aSwitch.isChecked()) {
                        mCrime.setmSovled(true);
                        aSwitch.setChecked(true);
                        rehash(crimes, mCrime);

                    } else if (!aSwitch.isChecked() && mCrime.getnID() == 0) {
                        mCrime.setmSovled(false);
                        aSwitch.setChecked(false);
                        rehash(crimes, mCrime);
                    } else if (!aSwitch.isChecked() && mCrime.getnID() != 0) {
                        mCrime.setmSovled(false);
                        aSwitch.setChecked(false);
                        rehash(crimes, mCrime);
                    }

                    if (aSwitch.isChecked() && mCrime.getnID() != 0) {
                        judge_all(crimes);
                    }
                }

            });
        }
        private void bindCrime(final Crime crime) {
            mCrime = crime;
            mTitleTextView.setText(mCrime.getmTitle());
            mDataTextView.setText(mCrime.getmData().toString());
            aSwitch.setChecked(crime.ismSovled());
        }

        private void rehash(List<Crime> mCrimes, Crime mCrime) {
            if (mCrimes.get(0).ismSovled() && mCrime.getnID() == 0) { // All open
                for (Crime crime : mCrimes) {
                    crime.setmSovled(true);
                }
            }
            if (!mCrimes.get(0).ismSovled() && mCrime.getnID() == 0) {  //全按钮关闭且id为0
                for (Crime crime : mCrimes) {
                    crime.setmSovled(false);
                }
            }
            if (mCrimes.get(0).ismSovled() && mCrime.getnID() != 0) {
                mCrimes.get(0).setmSovled(false);
            }

            //All down
        }
    }

    public void judge_all(List<Crime> mCrimes) {
        int count = 0;
        for (int i = 1; i < mCrimes.size(); i++) {
            if (mCrimes.get(i).ismSovled()) {
                count += 1;
            }
        }
        if (count == mCrimes.size() - 1) {
            mCrimes.get(0).setmSovled(true);
        }
    }

    private class CrimeAdapter extends RecyclerView.Adapter<CrimeHolder> {
        private List<Crime> mCrimes;

        public CrimeAdapter(List<Crime> crimes) {
            this.mCrimes = crimes;
        }

        public CrimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View view = layoutInflater.inflate(R.layout.list_item_crime, parent, false);
            return new CrimeHolder(view);
        }

        @Override
        public void onBindViewHolder(CrimeHolder holder, int position) {
            Crime crime = mCrimes.get(position);
            //holder.mTitleTextView.setText(crime.getmTitle());
            holder.bindCrime(crime);
            //updateUI();
        }

        @Override
        public int getItemCount() {
            return crimes.size();
        }


    }

    private void updateUI() {
        if (mAdapter == null) {
            mAdapter = new CrimeAdapter(crimes);
            mCrimeRecycleView.setAdapter(mAdapter);
        } else {
            mAdapter.notifyDataSetChanged();
        }
    }
}
