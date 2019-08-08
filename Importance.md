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
            if (processInfo.processName.equals(context.getPackageName()) && (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND)) {
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

