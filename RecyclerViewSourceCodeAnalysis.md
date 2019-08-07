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
## 模块间的逻辑关系
- RecyclerView通过LayoutManger协助完成布局
- LayoutManager通过Recycler获取view，并将view通过Recyckler进行回收
- ViewHoler需要通过适配器Adapter与数据层dataset进行模型的联接
- ItemAnimator观察RecyclerView的变化

## RecyclerView的主线代码源码分析
```java
recyclerView = (RecyclerView) findViewById(R.id.recyclerView);  
LinearLayoutManager layoutManager = new LinearLayoutManager(this);  // //设置布局管理器  
recyclerView.setLayoutManager(layoutManager);  //设置为垂直布局
layoutManager.setOrientation(OrientationHelper. VERTICAL);  //设置Adapter  
recyclerView.setAdapter( recycleAdapter);   //设置分隔线  
recyclerView.addItemDecoration( new DividerGridItemDecoration(this ));  //设置增加或删除条目的动画  
recyclerView.setItemAnimator( new DefaultItemAnimator());
```
- 1. 根据常规应用的主线代码，首先调用`recyclerView = (RecyclerView) findViewById(R.id.recyclerView);`,会触发RecyclerView源码执行构造方法`public RecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle)`，代码首先进行一些列的初始化方法的调用，其中主要包括获取布局的属性obtainStyledAttributes，然后是关键的createLayoutManager
```java
public RecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
if (attrs != null) {
            ......            
            int defStyleRes = 0;
            //获取布局的属性
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RecyclerView,
                    defStyle, defStyleRes);
            String layoutManagerName = a.getString(R.styleable.RecyclerView_layoutManager);
            ......
            a.recycle();
            //创建布局管理器
            createLayoutManager(context, layoutManagerName, attrs, defStyle, defStyleRes);            
            ......     
        }
      }
```
- 2. 在createLayoutManager方法中，首选判断布局的属性是否存在，如果布局文件中已经设置了布局管理器的类型，那么这里会通过反射的方式实例化出对应的布局管理器，最后将实例化的布局管理器设置到当前的RecyclerView中

```java
private void createLayoutManager(Context context, String className, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        if (className != null) {  // 判断布局属性是否存在
            className = className.trim();
            if (!className.isEmpty()) {
                className = getFullClassName(context, className);
                try {
                    ClassLoader classLoader;
                    if (isInEditMode()) {
                        // Stupid layoutlib cannot handle simple class loaders.
                        classLoader = this.getClass().getClassLoader();
                    } else {
                        classLoader = context.getClassLoader();
                    }
                    //根据布局属性设置的layoutManager通过反射实例化layoutManager
                    //------------------------------------------------
                    Class<? extends LayoutManager> layoutManagerClass =
                            classLoader.loadClass(className).asSubclass(LayoutManager.class);
                    Constructor<? extends LayoutManager> constructor;
                    Object[] constructorArgs = null;
                    try {
                        constructor = layoutManagerClass
                                .getConstructor(LAYOUT_MANAGER_CONSTRUCTOR_SIGNATURE);
                        constructorArgs = new Object[]{context, attrs, defStyleAttr, defStyleRes};
                    //---------------------------------------------------
                    } catch (NoSuchMethodException e) {
                    try {
                        constructor = layoutManagerClass.getConstructor();
                    } catch (NoSuchMethodException e1) {
                        e1.initCause(e);
                        throw new IllegalStateException(attrs.getPositionDescription() +
                                ": Error creating LayoutManager " + className, e1);
                    }
                }
                constructor.setAccessible(true);
                //如果已经在布局文件中设置了布局管理的类型，直接通过反射的方式实例化出对应的布局管理器
                //调用setLayoutManager
                setLayoutManager(constructor.newInstance(constructorArgs)); 
            }
        }
    }
```

