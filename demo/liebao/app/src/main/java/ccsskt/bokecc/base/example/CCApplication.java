package ccsskt.bokecc.base.example;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.multidex.MultiDex;

import com.bokecc.sskt.base.CCAtlasClient;
import com.tencent.bugly.crashreport.CrashReport;

import java.lang.ref.WeakReference;

/**
 * 作者 ${王德惠}.<br/>
 */
public class CCApplication extends Application {

    private static final String TAG = "CCApp";

    public static int mAppStatus = -1; // 表示 force_kill
    private static WeakReference<Context> context;
    /**
     * 用户角色 老师
     */
    public static final int PRESENTER = 0;
    /**
     * 用户角色 学生
     */
    public static final int TALKER = 1;


    public void onCreate() {
        super.onCreate();
        if (context == null) {
            context = new WeakReference<Context>(this);
        }

        CrashReport.initCrashReport(this, "d5dd98a23c", true);
    }

    public static Context getContext() {
        return context == null ? null : context.get();
    }

    public static String getVersion() {
        try {
            PackageManager manager = context.get().getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.get().getPackageName(), 0);
            return "v" + info.versionName;
        } catch (Exception e) {
            return "v" + Config.APP_VERSION;
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
