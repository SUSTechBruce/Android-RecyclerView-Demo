## Importance关键的运用与源码的解析
- task是android系统的一个activity的栈，包含多个app的activity，通过`ActivityManager`可以获取栈中的activity信息，从而判断activity对应`应用的状态`.
 
- 判断前后台的方法1： 通过runningProcess获取到一个当前正在运行的进程的List，我们遍历这个List中的每一个进程，判断这个进程的一个importance 属性 是否是前台进程，并且包名是否与我们判断的APP的包名一样，如果这两个条件都符合，那么这个App就处于前台。 `IMPORTANCE_FOREGROUND == 100`
```java
private static boolean isAppForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Service.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningAppProcessInfoList 
        = activityManager.getRunningAppProcesses();

        if (runningAppProcessInfoList == null) {
            Log.d("runningAppProcess:", "runningAppProcessInfoList is null!");
            return false;
        }
        for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcessInfoList) {
            if (processInfo.processName.equals(context.getPackageName()) && 
            (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND)) {
                return true;
            }
        }
        return false;
    }
```
- important关键字含义
```
IMPORTANCE_BACKGROUND
IMPORTANCE_EMPTY
IMPORTANCE_FOREGROUND
IMPORTANCE_PERCEPTIBLE
IMPORTANCE_SERVICE
IMPORTANCE_VISIBLE
```
```java
/**
         * The relative importance level that the system places on this process.
         * These constants are numbered so that "more important" values are
         * always smaller than "less important" values.
         */
```
- ActivityManager.RunningAppProcessInfo下的importance有6个值，分别是
```java
/**
         * Constant for {@link #importance}: This process is running the
         * foreground UI; that is, it is the thing currently at the top of the screen
         * that the user is interacting with.
         */
        public static final int IMPORTANCE_FOREGROUND = 100;

```
```java
 /**
         * Constant for {@link #importance}: This process is running a foreground
         * service, for example to perform music playback even while the user is
         * not immediately in the app.  This generally indicates that the process
         * is doing something the user actively cares about.
         */
        public static final int IMPORTANCE_FOREGROUND_SERVICE = 125;
```
- important相关源码 onUidStateChanged -> procStateToImportanceForClient -> procStateToImportanceForTargetSdk-> procStateToImportance
- `onUidStateChanged`:需要进程状态改变，调用`RunningAppProcessInfo.procStateToImportanceForClient（）`
- `procStateToImportanceForClient`：将进程状态转为为对应的`client IMPORTANCE_* constant`，调用`procStateToImportanceForTargetSdk`，调用`procStateToImportance`，并根据`targetSDK`状态返回`importance`值
- `procStateToImportance`，根据进程的状态转换为相对应的`important`值.

```java

public void onUidStateChanged(int uid, int procState, long procStateSeq) {
            mListener.onUidImportance(uid, RunningAppProcessInfo.procStateToImportanceForClient(
                    procState, mContext));
        }
        
public static @Importance int procStateToImportanceForClient(int procState,
                Context clientContext) {
            return procStateToImportanceForTargetSdk(procState,
                    clientContext.getApplicationInfo().targetSdkVersion);
        }

public static @Importance int procStateToImportanceForTargetSdk(int procState,
                int targetSdkVersion) {
            final int importance = procStateToImportance(procState);

            // For pre O apps, convert to the old, wrong values.
            if (targetSdkVersion < VERSION_CODES.O) {
                switch (importance) {
                    case IMPORTANCE_PERCEPTIBLE:
                        return IMPORTANCE_PERCEPTIBLE_PRE_26;
                    case IMPORTANCE_TOP_SLEEPING:
                        return IMPORTANCE_TOP_SLEEPING_PRE_28;
                    case IMPORTANCE_CANT_SAVE_STATE:
                        return IMPORTANCE_CANT_SAVE_STATE_PRE_26;
                }
            }
            return importance;
        }
**
         * Convert a proc state to the correspondent IMPORTANCE_* constant.  If the return value
         * will be passed to a client, use {@link #procStateToImportanceForClient}.
         * @hide
         */
  
public static @Importance int procStateToImportance(int procState) {
            if (procState == PROCESS_STATE_NONEXISTENT) {
                return IMPORTANCE_GONE;
            } else if (procState >= PROCESS_STATE_HOME) {
                return IMPORTANCE_CACHED;
            } else if (procState == PROCESS_STATE_HEAVY_WEIGHT) {
                return IMPORTANCE_CANT_SAVE_STATE;
            } else if (procState >= PROCESS_STATE_TOP_SLEEPING) {
                return IMPORTANCE_TOP_SLEEPING;
            } else if (procState >= PROCESS_STATE_SERVICE) {
                return IMPORTANCE_SERVICE;
            } else if (procState >= PROCESS_STATE_TRANSIENT_BACKGROUND) {
                return IMPORTANCE_PERCEPTIBLE;
            } else if (procState >= PROCESS_STATE_IMPORTANT_FOREGROUND) {
                return IMPORTANCE_VISIBLE;
            } else if (procState >= PROCESS_STATE_FOREGROUND_SERVICE) {
                return IMPORTANCE_FOREGROUND_SERVICE;
            } else {
                return IMPORTANCE_FOREGROUND;
            }
        }
        
        public RunningAppProcessInfo() {
            importance = IMPORTANCE_FOREGROUND;
            importanceReasonCode = REASON_UNKNOWN;
            processState = PROCESS_STATE_IMPORTANT_FOREGROUND;
        }
        
```
## Android进程状态的改变

- 进程有两个比较重要的状态值，即ProcessList.java->adj和ActivityManager.java->procState
- 安卓存在的几种进程状态：前台进程，可见进程，后台进程，空进程
- Android进程间优先级的变换，是根据应用组件的生命周期变化相关，以下事件会触发进程状态发生改变，主要包括：
- ACTIVITY
```java
ActivityStackSupervisor.realStartActivityLocked:// 启动Activity
ActivityStack.resumeTopActivityInnerLocked:// 恢复栈顶Activity
ActivityStack.finishCurrentActivityLocked:// 结束当前Activity
ActivityStack.destroyActivityLocked: //摧毁当前Activity
```
- SERVICE
```java
realStartServiceLocked: //启动服务
bindServiceLocked:// 绑定服务(只更新当前app)
unbindServiceLocked:// 解绑服务 (只更新当前app)
bringDownServiceLocked:// 结束服务 (只更新当前app)
sendServiceArgsLocked:// 在bringup或则cleanup服务过程调用 (只更新当前app)
```
- BROADCAST
```java
BQ.processNextBroadcast:// 处理下一个广播
BQ.processCurBroadcastLocked:// 处理当前广播
BQ.deliverToRegisteredReceiverLocked:// 分发已注册的广播 (只更新当前app)
```
- CONTENTPROVIDER
```java
AMS.removeContentProvider:// 移除provider
AMS.publishContentProviders:// 发布provider (只更新当前app)
AMS.getContentProviderImpl:// 获取provider (只更新当前app)
```
- PROCESS
```java
setSystemProcess:// 创建并设置系统进程
addAppLocked:// 创建persistent进程
attachApplicationLocked:// 进程创建后attach到system_server的过程;
trimApplications:// 清除没有使用app
appDiedLocked:// 进程死亡
killAllBackgroundProcesses:// 杀死所有后台进程.即(ADJ>9或removed=true的普通进程)
killPackageProcessesLocked:// 以包名的形式 杀掉相关进程;
```
- ADJ的核心算法
```java
updateOomAdjLocked：更新adj，当目标进程为空，或者被杀则返回false；否则返回true;
computeOomAdjLocked：计算adj，返回计算后RawAdj值;
applyOomAdjLocked：应用adj，当需要杀掉目标进程则返回false；否则返回true。
```
- 使用computeOomAdjLocked计算进程adj值
```java
1. 空进程情况
2. maxAdj<=0情况
3. 前台的情况
4. 非前台activity的情况
5. adj > 2的情况
6. HeavyWeightProces情况
7. HomeProcess情况
8. PreviousProcess情况
9. 备份进程情况
10. Service情况
11. ContentProvider情况
```
- updateOomAdjLocked的算法流程图

- computeOomAdjLocked的算法流程图
`ACTIVITY,SERVICE,BROADCASR,CONTENTPROVIDER,PROCESS`，这些事件都会直接/间接调用`ActivityManagerService.java`中的`updateOomAdjLocked`的方法来更新进程的优先级，`updateOomAdjLocked `先通过 `computeOomAdjLocked` 方法负责计算进程的优先级，再通过调用`applyOomAdjLocked`应用进程的优先级。
## startForegroundService和startForeground
- ContextImpl.startServiceCommon()-->ActivityManagerService.startService()-->ActiveServices.startServiceLocked()
```java
@Override
    public ComponentName startForegroundService(Intent service) {
        warnIfCallingFromSystemProcess();
        return startServiceCommon(service, true, mUser);
    }
```
启动Service过程中，调用ActiveServices.bringUpServiceLocked()方法,然后会调用ActiveServices.sendServiceArgsLocked()，从而在
5s之内调用Service.startForeground()
- startForegroundService和startForeground的importance关键字转变的流程


