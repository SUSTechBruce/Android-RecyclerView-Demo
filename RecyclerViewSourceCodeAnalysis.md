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
recyclerView.setLayoutManager(layoutManager); 
layoutManager.setOrientation(OrientationHelper. VERTICAL);  //设置为垂直布局 
recyclerView.setAdapter( recycleAdapter);  //设置Adapter  
recyclerView.addItemDecoration( new DividerGridItemDecoration(this ));  //设置分隔线    
recyclerView.setItemAnimator( new DefaultItemAnimator());//设置增加或删除条目的动画  
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
- 2. 在createLayoutManager方法中，首选判断布局的属性是否存在，如果为null，直接return。如果布局文件中已经设置了布局管理器的类型，那么这里会通过反射的方式实例化出对应的布局管理器，最后将实例化的布局管理器设置到当前的RecyclerView中

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
- 3. 调用setLayoutManager时，首先判断LayoutManager是不是和旧的一样，一样就直接返回。接着调用stopScroll，停止当前滚动。判断新的Layout，如果不为空，则调用mItemAnimator.endAnimations()停止所有item当前的动画，并调用removeAndRecycleAllViews(mRecycler)和removeAndRecycleScrapInt(mRecycler)分别对所有的view进和scrapInt进行移除和回收。判断RecyclerView是否已经attach到window上，为true则调用dispatchDetachedFromWindow(this, mRecycler)方法。之后，将Layoutmanager的RecyclerView设置为null，并调用removeAllViewsUnfiltered方法，移除RecyclerView所有child，并将新的layout赋值给mLayout。最后，调用mLayout.setRecyclerView(this)将新的RecyclerView调用给新的Layoutmanager的dispatchAttachedToWindow的方法，接着调用mRecycler.updateViewCacheSize()和
 requestLayout()进行recyclerView内容上的绘制。

```java
public void setLayoutManager(@Nullable LayoutManager layout) {
        if (layout == mLayout) {
            return;
        }
        stopScroll();
        // TODO We should do this switch a dispatchLayout pass and animate children. There is a good
        // chance that LayoutManagers will re-use views.
        if (mLayout != null) {
            // end all running animations
            if (mItemAnimator != null) {
                mItemAnimator.endAnimations();
            }
            // 清空所有之前的缓存VIEW
            mLayout.removeAndRecycleAllViews(mRecycler);
            mLayout.removeAndRecycleScrapInt(mRecycler);
            mRecycler.clear();

            if (mIsAttached) {
                mLayout.dispatchDetachedFromWindow(this, mRecycler);
            }
            mLayout.setRecyclerView(null);
            mLayout = null;
        } else {
            mRecycler.clear();
        }
        // this is just a defensive measure for faulty item animators.
        mChildHelper.removeAllViewsUnfiltered();
        mLayout = layout;
        if (layout != null) {
            if (layout.mRecyclerView != null) {
                throw new IllegalArgumentException("LayoutManager " + layout
                        + " is already attached to a RecyclerView:"
                        + layout.mRecyclerView.exceptionLabel());
            }
            mLayout.setRecyclerView(this);
            if (mIsAttached) {
                mLayout.dispatchAttachedToWindow(this);
            }
        }
        mRecycler.updateViewCacheSize();
        requestLayout(); //调用requestLayout进行绘制
    }
```
- 4. 在接下来调用的requestLayout函数中，这个函数的核心功能是更新View（View，ViewGroup的），主要流程为
```java
1.View#requestLayout() 
2.ViewGroup#requestLayout() --->调用父类的requestLayout，一直往上循环
3.ViewRootImpl#requestLayout() --->入口方法，接下来执行scheduleTraversals
4.ViewRootImpl#scheduleTraversals() --->执行mTraversalRunnable，这是一个Runnable多线程方法
5.ViewRootImpl#doTraversal() --->TraversalRunnable 的定义。所以最后执行了doTraversal()
6.ViewRootImpl#performTraversals() --->调用performMeasure() , performLayout() , performDraw()
```
在requestLayout的核心函数中，最重要的是performTraversals()调用的三个方法performMeasure() , performLayout() , performDraw(),这是View工作流程的核心方法，用来进行View的三大工作流程。
```java
private void performTraversals() {
        ...
    if (!mStopped) {
        int childWidthMeasureSpec = getRootMeasureSpec(mWidth, lp.width);  // 1
        int childHeightMeasureSpec = getRootMeasureSpec(mHeight, lp.height);
        performMeasure(childWidthMeasureSpec, childHeightMeasureSpec);   //执行performMeasure方法    
        }
    }
    if (didLayout) {
        performLayout(lp, desiredWindowWidth, desiredWindowHeight); //执行performLayout方法
        ...
    }
    if (!cancelDraw && !newSurface) {
        if (!skipDraw || mReportNextDraw) {
            if (mPendingTransitions != null && mPendingTransitions.size() > 0) {
                for (int i = 0; i < mPendingTransitions.size(); ++i) {
                    mPendingTransitions.get(i).startChangingAnimations();
                }
                mPendingTransitions.clear();
            }
            performDraw();                        //执行performDraw方法
        }
    }
    ...
}
```
a .在performMeasure方法中，直接调用View的measure方法，从ViewRootImpl回到了View。在调用requestLayout时，设置标识位mPrivateFlags = PFLAG_FORCE_LAYOUT，最总这个view会根据mPrivateFlags来判断是否要onMeasure方法
```java
public final void measure(int widthMeasureSpec, int heightMeasureSpec) {
    ......
    //requestLayout方法里有把mPrivateFlags  = PFLAG_FORCE_LAYOUT
    final boolean forceLayout = (mPrivateFlags & PFLAG_FORCE_LAYOUT) == PFLAG_FORCE_LAYOUT;
    final boolean specChanged = widthMeasureSpec != mOldWidthMeasureSpec
            || heightMeasureSpec != mOldHeightMeasureSpec;
    final boolean isSpecExactly = MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY
            && MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY;
    final boolean matchesSpecSize = getMeasuredWidth() == MeasureSpec.getSize(widthMeasureSpec)
            && getMeasuredHeight() == MeasureSpec.getSize(heightMeasureSpec);
    final boolean needsLayout = specChanged
            && (sAlwaysRemeasureExactly || !isSpecExactly || !matchesSpecSize);
    //依据forceLayout 和needsLayout决定是否要执行onMeasure
    if (forceLayout || needsLayout) {
        mPrivateFlags &= ~PFLAG_MEASURED_DIMENSION_SET;
        resolveRtlPropertiesIfNeeded();
        int cacheIndex = forceLayout ? -1 : mMeasureCache.indexOfKey(key);
        if (cacheIndex < 0 || sIgnoreMeasureCache) {
            // measure ourselves, this should set the measured dimension flag back
            //出现了onMeasure
            onMeasure(widthMeasureSpec, heightMeasureSpec);
            mPrivateFlags3 &= ~PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT;
        } 
        //记住这里设置的标识位
        mPrivateFlags |= PFLAG_LAYOUT_REQUIRED;
        ......
}
```
b. 接着，PrivateFlags |= PFLAG_LAYOUT_REQUIRED;，这个接下来onLayout会需要。在layout方法中，host是view，那么performLayout就执行了view的layout(), 这里根据条件执行onLayout，上一步执行measure时，mPrivateFlags |= PFLAG_LAYOUT_REQUIRED:
```java
private void performLayout(WindowManager.LayoutParams lp, int desiredWindowWidth,
        int desiredWindowHeight) {
        host.layout(0, 0, host.getMeasuredWidth(), host.getMeasuredHeight());
        ......
}
public void layout(int l, int t, int r, int b) {
    ......
    boolean changed = isLayoutModeOptical(mParent) ?
            setOpticalFrame(l, t, r, b) : setFrame(l, t, r, b);
    if (changed || (mPrivateFlags & PFLAG_LAYOUT_REQUIRED) == PFLAG_LAYOUT_REQUIRED) {
        onLayout(changed, l, t, r, b);
       ......
    }
    ......
}

```
c .在performDraw（）方法中，核心的逻辑顺序是performDraw()->draw()->drawSoftware()->view.draw()
```java
private void performDraw() {
    ......
        draw(fullRedrawNeeded); //首先调用draw（）
     ......
private void draw(boolean fullRedrawNeeded) {
    ......
    mAttachInfo.mDrawingTime =
            mChoreographer.getFrameTimeNanos() / TimeUtils.NANOS_PER_MS;
    if (!dirty.isEmpty() || mIsAnimating || accessibilityFocusDirty) {
        ......
        } else {
            ......
            if (!drawSoftware(surface, mAttachInfo, xOffset, yOffset, scalingRequired, dirty)) {  //draw中调用drawSoftWare
                return;
            }
        }
    }
    ......
}
private boolean drawSoftware(Surface surface, AttachInfo attachInfo, int xoff, int yoff,
        boolean scalingRequired, Rect dirty) {
    final Canvas canvas;
    try {
        canvas = mSurface.lockCanvas(dirty);
        .....
    } catch (Surface.OutOfResourcesException e) {
        handleOutOfResourcesException(e);
        return false;
    } catch (IllegalArgumentException e) {
        mLayoutRequested = true;    // ask wm for a new surface next time.
        return false;
    }
    try {
        ......
        try {
            ......
            mView.draw(canvas);      //在drawSoftware中调用view.draw（）
            drawAccessibilityFocusedDrawableIfNeeded(canvas);
        } finally {
           ......
        }
    } finally {
       ......
    }
    return true;
}
```
在view.draw（）中，该函数完成的功能有:
```
          1. Draw the background （绘制背景）
          2. If necessary, save the canvas' layers to prepare for fading
          3. Draw view's content （绘制自身）
          4. Draw children （绘制子view）
          5. If necessary, draw the fading edges and restore layers
          6. Draw decorations (scrollbars for instance)
```
```java
public void draw(Canvas canvas) {
    final int privateFlags = mPrivateFlags;
    final boolean dirtyOpaque = (privateFlags & PFLAG_DIRTY_MASK) == PFLAG_DIRTY_OPAQUE &&
            (mAttachInfo == null || !mAttachInfo.mIgnoreDirtyState);
    mPrivateFlags = (privateFlags & ~PFLAG_DIRTY_MASK) | PFLAG_DRAWN;
    int saveCount;
    if (!dirtyOpaque) {
        drawBackground(canvas);
    }
    final int viewFlags = mViewFlags;
    boolean horizontalEdges = (viewFlags & FADING_EDGE_HORIZONTAL) != 0;
    boolean verticalEdges = (viewFlags & FADING_EDGE_VERTICAL) != 0;
    if (!verticalEdges && !horizontalEdges) {
        if (!dirtyOpaque) onDraw(canvas);
        dispatchDraw(canvas);
        drawAutofilledHighlight(canvas);
        if (mOverlay != null && !mOverlay.isEmpty()) {
            mOverlay.getOverlayView().dispatchDraw(canvas);
        }
        onDrawForeground(canvas);
        drawDefaultFocusHighlight(canvas);
        if (debugDraw()) {
            debugDrawFocus(canvas);
        }
        return;
    }
```
- 5. 将
```java
recyclerView = (RecyclerView) findViewById(R.id.recyclerView);  
LinearLayoutManager layoutManager = new LinearLayoutManager(this);  // //设置布局管理器  
```
的主线源码分析完之后，我们进入recyclerView.setLayoutManager（layoutManager），这是一种巧妙的“桥接模式”，因为layoutManager的实现是多种的，因为桥接模式可以实例化逻辑ConcreteImplementor
```java
- ListView功能 recyclerView.setLayoutManager（new LinearLayoutManager（this））；
- 横向ListView的功能 recyclerView.setLayoutManager(new LinearLayoutManager(this)); layoutManager.setOrientation(…)
- GridView功能 recyclerView.setLayoutManager（new GridLayoutManager（this，3））；
- 瀑布流形式功能 recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
```
LinearLayoutManager/ StaggeredGridLayoutManager/GridLayoutManager的源码
```java
public LinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
    setOrientation(orientation);
    setReverseLayout(reverseLayout);
    setAutoMeasureEnabled(true);
}
public StaggeredGridLayoutManager(Context context, AttributeSet attrs, int defStyleAttr,
        int defStyleRes) {
    Properties properties = getProperties(context, attrs, defStyleAttr, defStyleRes);
    setOrientation(properties.orientation);
    setSpanCount(properties.spanCount);
    setReverseLayout(properties.reverseLayout);
    setAutoMeasureEnabled(mGapStrategy != GAP_HANDLING_NONE);
    mLayoutState = new LayoutState();
    createOrientationHelpers();
}
```
