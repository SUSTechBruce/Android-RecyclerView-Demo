## RecyclerView的技术细节
- RecyclerView 是ViewGroup的子类，每一个列表项都是作为一个View子对象显示的。这些View子对象。显示屏幕充满的是View子对象，recyclerview做的就是切换屏幕时，回收再利用这些子对象。
- RecyclerView离不开Adapter子类和ViewHolder子类，viewHolder通过itemView管理View。ViewHolder本身不会创建ViewHolder，这个任务需要由adapter来完成的，adapter是一个控制器，从模型层获取数据，然后提供给RecyclerView显示。因此，adapter负责创建必要的ViewHolder，绑定ViewHolder至模型数据层.
- RecyclerView显示视图对象时，就会找到他的Adapter子类：1.RecyclerView调用adapter的getItemCount（）的方法询问列表中有多少对象。2.RecyclerView调用adapter的createViewHolder（ViewGroup，int）方法创建ViewHolder以及以及ViewHolder要显示的视图。3.RecyclerView会传入ViewHolder及其位置，调用onBindViewHolder（ViewHolder，int）的方法，adapter会找到目标位置的数据并绑定在ViewHolder的视图上，使用模型数据填充视图。
- createViewHolder调用的并不频繁，一旦创建了足够多的ViewHolder，RecyclerView就会停止调用createViewHolder，通过回收利用旧的ViewHolder节约时间和内存。

## RecyclerView中Adapter子类和ViewHolder子类的具体实现
- 初始化onCreateView，绑定具体的RecyclerView
```java
@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_crime_list, container, false);
        mCrimeRecycleView = view.findViewById(R.id.crime_recycle_view); //绑定recyclerView
        mCrimeRecycleView.setLayoutManager(new LinearLayoutManager(getActivity()));
        onResume();
        return view;
    }
    @Override
    public void onResume(){
        super.onResume();
        updateUI();

    }
```
- 实现RecyclerView.Adapter,具体在Adapter中实现onCreateViewHolder和onBindViewHolder功能，一是创建具体的ViewHolder并返回对象，二是绑定模型层数据。
```java
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
```

