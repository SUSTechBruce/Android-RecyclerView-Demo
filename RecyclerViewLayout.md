## 2019/8/2
## RecyclerView的技术优点
- RecyclerView封装了viewHolder的复用功能，并且RecyclerView标准化了ViewHolder，编写Adapter面向的也是ViewHolder而不再是View。
- RecyclerView使用能很方便的实现和转换各种布局，可设置布局管理器控制item的布局方式，通常有横向horizontal布局，纵向vertical布局以及gridLayout和staggeringLayout瀑布流布局，可以通过`LinearLayoutManager`,`GridLayoutManager`,`StaggeredGridLayoutManager`进行控制，
```java
LinearLayoutManager layoutManager = new LinearLayoutManager(this ); 
recyclerView.setLayoutManager(layoutManager); 
```
不局限与`ListView`的线性展示方式。
- 默认支持局部刷新,容易实现添加item、删除item的动画效果,容易实现拖拽、侧滑删除等功能。
## RecyclerView的技术细节

- RecyclerView 是ViewGroup的子类，每一个列表项都是作为一个View子对象显示的。这些View子对象。显示屏幕充满的是View子对象，recyclerview做的就是切换屏幕时，回收再利用这些子对象。
- RecyclerView离不开`Adapter`子类和`ViewHolder`子类，viewHolder通过itemView管理View。ViewHolder本身不会创建ViewHolder，这个任务需要由adapter来完成的，adapter是一个控制器，从模型层获取数据，然后提供给RecyclerView显示。因此，adapter负责创建必要的ViewHolder，绑定ViewHolder至模型数据层.
- RecyclerView显示视图对象时，就会找到他的Adapter子类：1.RecyclerView调用adapter的`getItemCount（）`的方法询问列表中有多少对象。2.RecyclerView调用adapter的`createViewHolder（ViewGroup，int）`方法创建ViewHolder以及以及ViewHolder要显示的视图。3.RecyclerView会传入ViewHolder及其位置，调用`onBindViewHolder（ViewHolder，int）`的方法，adapter会找到目标位置的数据并绑定在ViewHolder的视图上，使用模型数据填充视图。
- createViewHolder调用的并不频繁，一旦创建了足够多的ViewHolder，RecyclerView就会停止调用createViewHolder，通过回收利用旧的ViewHolder节约时间和内存。

## RecylerView的具体实现方式
- 1. 在app包下build.gradle文件中引入,例如
```
implementation ' com.android.support:recyclerview-v7:23.4.0''
```
- 2. 创建带有recyclerView控件的`Activity.xml`和具体的布局文件`item.xml`
```java
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#fff">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/rv_one"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:overScrollMode="never"/>
</androidx.constraintlayout.widget.ConstraintLayout>
```
```java
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content">

    <ImageView
        android:id="@+id/iv_item_icon"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:src="@drawable/ic_photos_48dp"
        android:scaleType="centerCrop"
        android:layout_margin="1dp"
        />
</androidx.constraintlayout.widget.ConstraintLayout>
```
- 3. 创建Adapter，继承自`RecyclerView.Adapter<this.class.ViewHolder>`,例如
```java
public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder>{}
```
- 4. 创建ViewHolder：在Adapter中创建一个继承`RecyclerView.ViewHolder`的静态内部类：
```java
 public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public ImageView imageView;
        public TextView nameTextView;
        public TextView ratingTextView;
        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            imageView =  itemView.findViewById(R.id.imageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            ratingTextView =  itemView.findViewById(R.id.ratingTextView);
        }
```
- 5. 在adapter中实现3个方法。a. `onCreateViewHolder()`主要的功能为使得每一个`item inflater`生出一个View, 返回的是viewHolder，并把view直接封装在ViewHolder中，面向ViewHolder这个实例:
```java
@Override
        public ViewHolder onCreateViewHolder( ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(class.this)
            .inflate(R.layout.item, parent, false);
            return new ViewHolder(view);
        }
```
b. onBindViewHolder()，该方法的主要功能为适配模型层数据到View中，具体绑定ViewHolder类里的模型层数据对象
```java
 @Override
        public void onBindViewHolder( ViewHolder holder, int position) {
            holder.element.setImageResource(data.get(position));
        }
```
c. getItemCount()
```c
@Override
        public int getItemCount() {
            return data.size();
        }
```
## RecyclerView的具体布局和设置

- 设置RecyclerView，在创建完Adapter后，接着在onCreate中对RecylcerView进行设置，例如Layout的三种布局方式如下：
a. LinearLayout布局，
```java
List<data>Data = initData();
RecyclerView rv = findViewById(R.id.rv);
rv.setLayoutManager(new LinearLayoutManager(holder.recyclerView.getContext(),
                                            RecyclerView.HORIZONTAL, 
                                            false))；
rv.setAdapter(new Adapter(data)); // 通知全部更新
```
LayoutManager负责RecyclerView的布局，包括item view的回收和获取，除此之外，常用的LinearLayout的API有
```java
 scrollToPosition(int position);//滚动到指定位置
 findViewByPosition(int position);//获取指定位置的Item View
  setOrientation(int orientation);//设置滚动的方向
```
b. GridLayoutManager，构造函数中接收两个参数实现格式布局
```java
GridLayoutManager layoutManager = new GridLayoutManager(this, 4, OrientationHelper.VERTICAL, false);
rv.setLayoutManager(layoutManager);
rv.setAdapter(new Adapter(data));
```
c. StaggeredGridLayoutManager
```java
StaggeredGridLayoutManager layoutManager = 
new StaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL);
rv.setLayoutManager(layoutManager);
rv.setAdapter(new Adapter(data));
rv.addItemDecoration(new MDStaggeredRvDividerDecotation(this)); //设置样式
```
## RecyclerView的点击事件的设置

- 1.直接在ViewHolder的类里，设置onClick事件监听，例如：
```java
@Override
        public void onClick(View v) {
            Log.e("App", mApps.get(getAdapterPosition()).getName());
            if(mApps.get(getAdapterPosition()).getName().equals("今日活动指数+")){
                Intent intent = new Intent(v.getContext(), SportDataActivity.class);
                v.getContext().startActivity(intent);
                Log.e("App", "Touch successful");
            }else{
                Toast.makeText(v.getContext(), "你已经点击该功能:"+ mApps.get(getAdapterPosition()).getName(), Toast.LENGTH_SHORT ).show();
            }
        }
```
- 2.在ViewHolder类外，Adapter类中实现监听函数，例如对itemView中的switch组件进行动态监听，函数在`onBindViewHolder()`中进行调用
```java
private void setSwitchListener(final Switch aSwitch, final int position){

        aSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(!buttonView.isPressed()){
                    return;
                }
                if (isChecked) {
                    mCrimes.get(position).setmSovled(true);
                    if(position == 0){
                        openALL(true);
                        notifyDataSetChanged();
                    }else if(position!=0){
                        int count = 0;
                        for (int i = 1; i < mCrimes.size(); i++) {
                            if (mCrimes.get(i).ismSovled()) {
                                count += 1;
                            }
                        }
                        if (count == mCrimes.size() - 1) {
                            mCrimes.get(0).setmSovled(true);
                        }
                    notifyDataSetChanged();
                    }
                    Log.e("Bruce","pos "+position+" open");
                }
                else {
                    mCrimes.get(position).setmSovled(false);
                    if(position == 0){
                        openALL(false);
                        notifyDataSetChanged();
                    }else if(position!=0){
                        mCrimes.get(0).setmSovled(false);
                        notifyDataSetChanged();}
                    Log.e("Bruce","pos "+position+" closed");
                }
            }
        });
    }
```
## RecyclerView设置局部更新
- 在数据层改变数据，可以设置`notifyItemInserted()`,`notifyItemRemoved()`,`notifyItemChanged()`等API更新单个或某个范围的Item视图，例如在Adapter类里三种改变数据层数据的方法updateData，addNewItem和deleteItem中加入notifyDataChanged，view会自动刷新。
```java
public void updateData(ArrayList<String> data) {
        this.mData = data;
        notifyDataSetChanged();
    }
    public void addNewItem() {
        if(mData == null) {
            mData = new ArrayList<>();
        }
        mData.add(0, "new Item");
        notifyItemInserted(0);
    }
    public void deleteItem() {
        if(mData == null || mData.isEmpty()) {
            return;
        }
        mData.remove(0);
        notifyItemRemoved(0);
    }
```
## RecyckerView的回收机制
- RecyclerView是以ViewHolder作为单位进行回收，具体实现四级缓存
- mAttachedScrap缓存屏幕上的ViewHolder
- mCachedViews缓存屏幕外的ViewHolder，默认值为2
- mViewCacheExtensions需要特定定制
- mRecyclerPool为缓存池。多个recyclerView共用。
