## 2019/7/24
## RecyclerView的技术细节
- `RecyclerView` 是`ViewGroup`的子类，每一个列表项都是作为一个`View`子对象显示的。这些`View`子对象。显示屏幕充满的是`View`子对象，`recyclerview`做的就是切换屏幕时，回收再利用这些子对象。
- `RecyclerView`离不开`Adapter`子类和`ViewHolder`子类，`viewHolder`通过`itemView`管理`View`。`ViewHolder`本身不会创建`ViewHolde`r，这个任务需要由`adapter`来完成的，`adapter`是一个控制器，从模型层获取数据，然后提供给`RecyclerView`显示。因此，`adapter`负责创建必要的`ViewHolder`，绑定`ViewHolder`至模型数据层.
- `RecyclerView`显示视图对象时，就会找到他的`Adapter`子类：
-     1.`RecyclerView`调用`adapter`的`getItemCount（）`的方法询问列表中有多少对象。
-     2.`RecyclerView`调用`adapter`的`createViewHolder（ViewGroup，int`）方法创建ViewHolder以及以及ViewHolder要显示的视图。  
-     3.`RecyclerView`会传入`ViewHolder`及其位置，调用`onBindViewHolder（ViewHolder，int）`的方法，`adapter`会找到目标位置的数据并绑定在`ViewHolder`的视图上，使用模型数据填充视图。
-     4.`createViewHolder`调用的并不频繁，一旦创建了足够多的`ViewHolder`，`RecyclerView`就会停止调用`createViewHolder`，通过回收利用旧的`ViewHolder`节约时间和内存。
