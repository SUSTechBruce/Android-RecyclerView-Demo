# RecyclerView的源码分析

## RecyclerView使用到的设计模式
- 桥接模式，将RecyclerView的布局独立成LayoutManager
- 组合模式，将RecylcerView通过Item View进行布局绘制
- 适配器模式，ViewHolder将recyclerView与itemView联系
- 观察者模式，给ViewHolder注册观察者，调用notifyDataSetChanged时进行更新

## RecyclerView的几大模块
- LayoutManager，控制RecyclerView中item的布局方向
- Adapter，负责承载数据层模型，通知数据更新
- ItemDecoration，为RecyclerView添加分割线
- ItemAnimator，控制item的动画
### 模块间的逻辑关系
- RecyclerView通过LayoutManger协助完成布局
- LayoutManager通过Recycler获取view，并将view通过Recyckler进行回收
- ViewHoler需要通过适配器Adapter与数据层dataset进行模型的联接
- ItemAnimator观察RecyclerView的变化


