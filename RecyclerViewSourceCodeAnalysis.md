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
- 5. 我们进入recyclerView.setLayoutManager（layoutManager），这是一种巧妙的“桥接模式”，因为layoutManager的实现是多种的，因为桥接模式可以实例化逻辑ConcreteImplementor
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
回到onMeasure（）函数的分析，可知初始化会设置AutoMeasurEnabled，RecyclerView会将测量与布局交给LayoutManager来做，并且LayoutManager有一个叫做mAutoMeasure的属性，这个属性用来控制LayoutManager是否开启自动测量，开启自动测量的话布局就交由RecyclerView使用一套默认的测量机制，否则，自定义的LayoutManager需要重写onMeasure来处理自身的测量工作。如下为onMeasure（）的重要部分源码以及分析：
```java
protected void onMeasure(int widthSpec, int heightSpec) {
    ...
    if (mLayout.mAutoMeasure) {
        final int widthMode = MeasureSpec.getMode(widthSpec);
        final int heightMode = MeasureSpec.getMode(heightSpec);
        final boolean skipMeasure = widthMode == MeasureSpec.EXACTLY
                && heightMode == MeasureSpec.EXACTLY;
        mLayout.onMeasure(mRecycler, mState, widthSpec, heightSpec);
        if (skipMeasure || mAdapter == null) {
            return;
        }
        if (mState.mLayoutStep == State.STEP_START) {
            dispatchLayoutStep1();
        }
        mLayout.setMeasureSpecs(widthSpec, heightSpec);
        mState.mIsMeasuring = true;
        dispatchLayoutStep2();
        mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec);
        if (mLayout.shouldMeasureTwice()) {
            mLayout.setMeasureSpecs(
                    MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
            mState.mIsMeasuring = true;
            dispatchLayoutStep2();
            mLayout.setMeasuredDimensionFromChildren(widthSpec, heightSpec);
        }
    }
    ...
}
```
首先，调用MeasureSpec.getMode()获取widthSpec和heightSpec等View的属性，之后，recyclerView的布局过程分为三步，其中，STEP_START表示即将开始布局，需要调用dispatchLayoutStep1来执行第一步布局，接下来，布局状态变为STEP_LAYOUT，表示接下来需要调用dispatchLayoutStep2里进行第二步布局，同理，第二步布局后状态变为STEP_ANIMATIONS，需要执行第三步布局dispatchLayoutStep3，因此，step1负责记录状态，step2负责布局，step3与step1进行比较，根据变化来触发动画。
```java
protected void onLayout(boolean changed, int l, int t, int r, int b) {
    TraceCompat.beginSection(TRACE_ON_LAYOUT_TAG);
    dispatchLayout();
    TraceCompat.endSection();
    mFirstLayoutComplete = true;
}
```
- 接下来，我们分别分析dispatchLayout，dispatchLayout2，dispatchLayout3，因为在performLayout中，判断条件中调用了onLayout，onLayout中调用了dispatchLayout：
```java
void dispatchLayout() {
    ...
    mState.mIsMeasuring = false;
    if (mState.mLayoutStep == State.STEP_START) {
        dispatchLayoutStep1();
        mLayout.setExactMeasureSpecsFrom(this);
        dispatchLayoutStep2();
    } else if (mAdapterHelper.hasUpdates() || mLayout.getWidth() != getWidth() ||mLayout.getHeight() != getHeight()) {
        // First 2 steps are done in onMeasure but looks like we have to run again due to
        // changed size.
        mLayout.setExactMeasureSpecsFrom(this);
        dispatchLayoutStep2();
    } else {
        // always make sure we sync them (to ensure mode is exact)
        mLayout.setExactMeasureSpecsFrom(this);
    }
    dispatchLayoutStep3();
}
```
1. 在 dispatchLayoutStep1()，step1的第一步目的就是在记录View的状态，首先遍历当前所有的View依次进行处理，mItemAnimator会根据每个View的信息封装成一个ItemHolderInfo，这个ItemHolderInfo中主要包含的就是当前View的位置状态等。然后ItemHolderInfo 就被存入mViewInfoStore中。接着调用addToPreLayout方法，该方法会根据holder来查询InfoRecord信息，如果没有，则生成，然后将info信息赋值给InfoRecord的preInfo变量。最后标记FLAG_PRE信息。
```java
private void dispatchLayoutStep2() {
    ...
    mLayout.onLayoutChildren(mRecycler, mState);
     ...
    mState.mLayoutStep = State.STEP_ANIMATIONS;
}
```
2. dispatchLayoutStep2,核心的功能是真正地去布局View，其核心代码为onLayoutChildren方法，代码的核心功能为：
```java
- 寻找anchor点
- 根据anchor点一直向前布局，直至填充满anchor点前的所有区域
- 根据anchor一直向后布局，直至填充满anchor点后面的所有区域
```
```java
private void dispatchLayoutStep3() {
    mState.mLayoutStep = State.STEP_START;
    if (mState.mRunSimpleAnimations) {
        for (int i = mChildHelper.getChildCount() - 1; i >= 0; i--) {
            ...
            final ItemHolderInfo animationInfo = mItemAnimator
                    .recordPostLayoutInformation(mState, holder);
                mViewInfoStore.addToPostLayout(holder, animationInfo);
        }
        mViewInfoStore.process(mViewInfoProcessCallback);
    }
    ...
}
```
3. dispatchLayoutStep3方法中，由于子View已经完成了布局，所以子View的信息都发生了变化，mItemAnimator调用的是recordPostLayoutInformation方法，而mViewInfoStore调用的是addToPostLayout方法,来记录布局后的状态。

- 6. 在完成以上的测量，布局，测绘的任务后，setLayoutManager已经完成了，接下来主功能函数调用的是recyclerView.setAdapter( recycleAdapter); 
分析其在源码中的代码：
```java
public static abstract class Adapter {
    private final AdapterDataObservable mObservable = new AdapterDataObservable();
    public void registerAdapterDataObserver(AdapterDataObserver observer) {
        mObservable.registerObserver(observer);
    }
    public void unregisterAdapterDataObserver(AdapterDataObserver observer) {
        mObservable.unregisterObserver(observer);
    }
    public final void notifyItemInserted(int position) {
        mObservable.notifyItemRangeInserted(position, 1);
    }
}
```
RecyclerView的adapter的作用是就是完成了数据源datas 转化成 ItemView的工作，完成datas->Adapter->View的操作，通过`public abstract void onBindViewHolder(VH holder, int position);`将datas绑定到view中然后返回viewHolder，因此可以看到Adapter中包含一个AdapterDataObservable的对象mObservable，这个是一个可观察者，在可观察者中可以注册一系列的观察者AdapterDataObserver。当调用notify函数时候，就是可观察者发出通知，这时已经注册的观察者都可以收到这个通知，然后依次进行处理，因此，观察者注册的地方就是在RecyclerView的这个函数中。这个是setAdapter方法最终调用的地方。它主要做了
```
- 如果之前存在Adapter，先移除原来的，注销观察者，和从RecyclerView Detached。
- 然后根据参数，决定是否清除原来的ViewHolder
- 然后重置AdapterHelper，并更新Adapter，注册观察者。
```
```java
public void setAdapter(Adapter adapter) {
    // bail out if layout is frozen
    setLayoutFrozen(false);
    setAdapterInternal(adapter, false, true);
    requestLayout();
}
```
从setAdapterInternal的代码中得知，mObserver这个成员变量就是注册的观察者，该成员变量是一个RecyclerViewDataObserver的实例，那么RecyclerViewDataObserver实现了AdapterDataObserver中的方法。其中onItemRangeInserted(int positionStart, int itemCount)就是观察者接受到有数据插入通知的方法
```java
private class RecyclerViewDataObserver extends AdapterDataObserver {
    ...
    mPostUpdatesOnAnimation = version >= 16;
    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        assertNotInLayoutOrScroll(null);
        if (mAdapterHelper.onItemRangeInserted(positionStart, itemCount)) {
            triggerUpdateProcessor();
        }
    }
}
```
- RecyclerView的滑动功能
```
- 手指在屏幕上移动，使RecyclerView滑动的过程，可以称为scroll
- 指离开屏幕，RecyclerView继续滑动一段距离的过程，可以称为fling
```
RecyclerView的触屏事件的源码：
```java
public boolean onTouchEvent(MotionEvent e) {
    ...
    if (mVelocityTracker == null) {
        mVelocityTracker = VelocityTracker.obtain();
    }
    ...
    switch (action) {
        ...
        case MotionEvent.ACTION_MOVE: {
            ...
            final int x = (int) (MotionEventCompat.getX(e, index) + 0.5f);
            final int y = (int) (MotionEventCompat.getY(e, index) + 0.5f);
            int dx = mLastTouchX - x;
            int dy = mLastTouchY - y;
            ...
            if (mScrollState != SCROLL_STATE_DRAGGING) {
                ...
                if (canScrollVertically && Math.abs(dy) > mTouchSlop) {
                    if (dy > 0) {
                        dy -= mTouchSlop;
                    } else {
                        dy += mTouchSlop;
                    }
                    startScroll = true;
                }
                if (startScroll) {
                    setScrollState(SCROLL_STATE_DRAGGING);
                }
            }
            if (mScrollState == SCROLL_STATE_DRAGGING) {
                mLastTouchX = x - mScrollOffset[0];
                mLastTouchY = y - mScrollOffset[1];
                if (scrollByInternal(
                        canScrollHorizontally ? dx : 0,
                        canScrollVertically ? dy : 0,
                        vtev)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
            }
        } break;
        ...
        case MotionEvent.ACTION_UP: {
            ...
            final float yvel = canScrollVertically ?
                    -VelocityTrackerCompat.getYVelocity(mVelocityTracker, mScrollPointerId) : 0;
            if (!((xvel != 0 || yvel != 0) && fling((int) xvel, (int) yvel))) {
                setScrollState(SCROLL_STATE_IDLE);
            }
            resetTouch();
        } break;
        ...
    }
    ...
}
public boolean fling(int velocityX, int velocityY) {
    ...
    mViewFlinger.fling(velocityX, velocityY);
    ...
}
```
核心步骤为
```
1. 当RecyclerView接收到ACTION_MOVE事件后，会先计算出手指移动距离（dy），并与滑动阀值（mTouchSlop）比较，当大于此阀值时将滑动状态设置为SCROLL_STATE_DRAGGING，而后调用scrollByInternal()方法，使RecyclerView滑动，这样RecyclerView的滑动的第一阶段scroll就完成了；
2. 接收到ACTION_UP事件时，会根据之前的滑动距离与时间计算出一个初速度yvel，这步计算是由VelocityTracker实现的，然后再以此初速度，调用方法fling()，完成RecyclerView滑动的第二阶段fling
```
- 逻辑缓存是RecyclerView的关键特点，RecyclerView是多级缓存的，Recycler的作用就是重用ItemView。在填充ItemView的时候，ItemView是从它获取的；滑出屏幕的ItemView是由它回收的，关键的获取ItemView调用代码为：`RecyclerView.Recycler.getViewForPosition()`
```java
View getViewForPosition(int position, boolean dryRun) {
    boolean fromScrap = false;
    ViewHolder holder = null;
    if (mState.isPreLayout()) {
        holder = getChangedScrapViewForPosition(position);
        fromScrap = holder != null;
    }
    if (holder == null) {
        holder = getScrapViewForPosition(position, INVALID_TYPE, dryRun);
       ...
    }
    if (holder == null) {
        final int offsetPosition = mAdapterHelper.findPositionOffset(position);
        final int type = mAdapter.getItemViewType(offsetPosition);
        if (mAdapter.hasStableIds()) {
            holder = getScrapViewForId(mAdapter.getItemId(offsetPosition), type, dryRun);
        }
        if (holder == null && mViewCacheExtension != null) {
            final View view = mViewCacheExtension
                    .getViewForPositionAndType(this, position, type);
           ...
        }
        if (holder == null) { // fallback to recycler
            holder = getRecycledViewPool().getRecycledView(type);
            if (holder != null) {
                holder.resetInternal();
                if (FORCE_INVALIDATE_DISPLAY_LIST) {
                    invalidateDisplayListInt(holder);
                }
            }
        }
        if (holder == null) {
            holder = mAdapter.createViewHolder(RecyclerView.this, type);
        }
    }
    return holder.itemView;
}
```
代码的核心逻辑为
```
- 根据列表位置获取ItemView，先后从scrapped、cached、exCached、recycled集合中查找相应的ItemView，如果没有找到，就创建（Adapter.createViewHolder()），最后与数据集绑定。
- scrapped、cached和exCached集合定义在RecyclerView.Recycler中，分别表示将要在RecyclerView中删除的ItemView、一级缓存ItemView和二级缓存ItemView，cached集合的大小默认为２，exCached是需要我们通过RecyclerView.ViewCacheExtension自己实现的.
- recycled集合其实是一个Map,private SparseArray<ArrayList<ViewHolder>> mScrap = new SparseArray<ArrayList<ViewHolder>>();，定义在RecyclerView.RecycledViewPool中，将ItemView以ItemType分类保存了下来，这里算是RecyclerView设计上的亮点，通过RecyclerView.RecycledViewPool可以实现在不同的RecyclerView之间共享ItemView，只要为这些不同RecyclerView设置同一个RecyclerView.RecycledViewPool就可以了
```
```java
public void onItemRangeRemoved(int positionStart, int itemCount) {
    assertNotInLayoutOrScroll(null);
    if (mAdapterHelper.onItemRangeRemoved(positionStart, itemCount)) {
        triggerUpdateProcessor();
    }
}
```
View的回收并不像View的创建那么复杂，这里只涉及了两层缓存mCachedViews与mRecyclerPool，mCachedViews相当于一个先进先出的数据结构，每当有新的View需要缓存时都会将新的View存入mCachedViews，而mCachedViews则会移除头元素，并将头元素放入mRecyclerPool，所以mCachedViews相当于一级缓存，mRecyclerPool则相当于二级缓存，并且mRecyclerPool是可以多个RecyclerView共享的，这在类似于多Tab的新闻类应用会有很大的用处，因为多个Tab下的多个RecyclerView可以共用一个二级缓存。减少内存开销。
