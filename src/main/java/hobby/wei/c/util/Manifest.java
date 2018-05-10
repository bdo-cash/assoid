package hobby.wei.c.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

import java.lang.ref.WeakReference;

public class Manifest {
    private static volatile WeakReference<PackageInfo> ref;

    private static PackageInfo getPackageInfo(Context context) {
        PackageInfo info = null;
        if (ref != null) info = ref.get();
        if (info == null) {
            try {
                info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);    //0代表是获取版本信息
                ref = new WeakReference<PackageInfo>(info);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }
        return info;
    }

    public static String getVersionName(Context context) {
        return getPackageInfo(context).versionName;
    }

    public static int getVersionCode(Context context) {
        return getPackageInfo(context).versionCode;
    }

    /**
     * 有以下三个属性：<br>
     * android:name<br>
     * android:resource	可通过Bundle.getInt()获取到<br>
     * android:value	可通过getString()、getFloat()、getBoolean()获取到<br>
     *
     * @author WeiChou
     */
    public static class MetaData {
        private static WeakReference<Bundle> ref;

        private static Bundle getBundle(Context context) {
            Bundle b = null;
            if (ref != null) b = ref.get();
            if (b == null) {
                try {
                    ApplicationInfo appi = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
                    b = appi.metaData;
                    ref = new WeakReference<Bundle>(b);
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }
            }
            return b;
        }

        public static int getResourceId(Context context, String keyName) {
            return getBundle(context).getInt(keyName);
        }

        public static String getString(Context context, String keyName) {
            return getBundle(context).getString(keyName);
        }

        public static boolean getBoolean(Context context, String keyName, boolean defValue) {
            return getBundle(context).getBoolean(keyName, defValue);
        }

        public static float getFloat(Context context, String keyName, float defValue) {
            return getBundle(context).getFloat(keyName, defValue);
        }
    }
}
