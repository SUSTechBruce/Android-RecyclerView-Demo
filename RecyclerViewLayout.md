## 2019/7/24
## RecyclerView的技术优点
- RecyclerView封装了viewHolder的复用功能，并且RecyclerView标准化了ViewHolder，编写Adapter面向的也是ViewHolder而不再是View。
- RecyclerView使用能很方便的实现和转换各种布局，可设置布局管理器控制item的布局方式，通常有横向horizontal布局，纵向vertical布局以及gridLayout和staggeringLayout瀑布流布局，可以通过`LinearLayoutManager`,`GridLayoutManager`,`StaggeredGridLayoutManager`进行控制，
```java
LinearLayoutManager layoutManager = new LinearLayoutManager(this ); 
recyclerView.setLayoutManager(layoutManager); 
```
不局限与`ListView`的线性展示方式。

## RecyclerView的技术细节

- RecyclerView 是ViewGroup的子类，每一个列表项都是作为一个View子对象显示的。这些View子对象。显示屏幕充满的是View子对象，recyclerview做的就是切换屏幕时，回收再利用这些子对象。
- RecyclerView离不开`Adapter`子类和`ViewHolder`子类，viewHolder通过itemView管理View。ViewHolder本身不会创建ViewHolder，这个任务需要由adapter来完成的，adapter是一个控制器，从模型层获取数据，然后提供给RecyclerView显示。因此，adapter负责创建必要的ViewHolder，绑定ViewHolder至模型数据层.
- RecyclerView显示视图对象时，就会找到他的Adapter子类：1.RecyclerView调用adapter的`getItemCount（）`的方法询问列表中有多少对象。2.RecyclerView调用adapter的`createViewHolder（ViewGroup，int）`方法创建ViewHolder以及以及ViewHolder要显示的视图。3.RecyclerView会传入ViewHolder及其位置，调用`onBindViewHolder（ViewHolder，int）`的方法，adapter会找到目标位置的数据并绑定在ViewHolder的视图上，使用模型数据填充视图。
- createViewHolder调用的并不频繁，一旦创建了足够多的ViewHolder，RecyclerView就会停止调用createViewHolder，通过回收利用旧的ViewHolder节约时间和内存。
