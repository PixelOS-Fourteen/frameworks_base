/*
 * Copyright (C) 2022 Paranoid Android
 * Copyright (C) 2023 StatiXOS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.util;

import android.app.Application;
import android.content.res.Resources;
import android.os.Build;
import android.os.Build.VERSION;
import android.util.Log;

import com.android.internal.R;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PropImitationHooks {

    private static final String TAG = "PropImitationHooks";
    private static final boolean DEBUG = false;

    private static final String sCertifiedFp =
            Resources.getSystem().getString(R.string.config_certifiedFingerprint);

    private static final String sStockFp =
            Resources.getSystem().getString(R.string.config_stockFingerprint);

    private static final String PACKAGE_ARCORE = "com.google.ar.core";
    private static final String PACKAGE_FINSKY = "com.android.vending";
    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PROCESS_GMS_UNSTABLE = PACKAGE_GMS + ".unstable";

    private static final String PACKAGE_SUBSCRIPTION_RED = "com.google.android.apps.subscriptions.red";
    private static final String PACKAGE_TURBO = "com.google.android.apps.turbo";
    private static final String PACKAGE_VELVET = "com.google.android.googlequicksearchbox";
    private static final String PACKAGE_GBOARD = "com.google.android.inputmethod.latin";
    private static final String PACKAGE_GPHOTOS = "com.google.android.apps.photos";
    private static final String FEATURE_NEXUS_PRELOAD = "com.google.android.apps.photos.NEXUS_PRELOAD";
    private static final String PACKAGE_SETUPWIZARD = "com.google.android.setupwizard";

    private static final Map<String, Object> sP8Props = new HashMap<>();
    static {
        sP8Props.put("BRAND", "google");
        sP8Props.put("MANUFACTURER", "Google");
        sP8Props.put("DEVICE", "husky");
        sP8Props.put("PRODUCT", "husky");
        sP8Props.put("MODEL", "Pixel 8 Pro");
        sP8Props.put("FINGERPRINT", "google/husky/husky:14/UD1A.231105.004/11010374:user/release-keys");
    }

    private static final Map<String, Object> sPixelXLProps = new HashMap<>();
    static {
        sPixelXLProps.put("BRAND", "google");
        sPixelXLProps.put("MANUFACTURER", "Google");
        sPixelXLProps.put("DEVICE", "marlin");
        sPixelXLProps.put("PRODUCT", "marlin");
        sPixelXLProps.put("MODEL", "Pixel XL");
        sPixelXLProps.put("FINGERPRINT", "google/marlin/marlin:10/QP1A.191005.007.A3/5972272:user/release-keys");
    }

    private static final Set<String> sPixelFeatures = Set.of(
        "PIXEL_2017_PRELOAD",
        "PIXEL_2018_PRELOAD",
        "PIXEL_2019_MIDYEAR_PRELOAD",
        "PIXEL_2019_PRELOAD",
        "PIXEL_2020_EXPERIENCE",
        "PIXEL_2020_MIDYEAR_EXPERIENCE"
    );

    private static volatile boolean sIsGms = false;
    private static volatile boolean sIsFinsky = false;
    private static volatile boolean sIsPhotos = false;

    public static void setProps(Application app) {
        final String packageName = app.getPackageName();
        final String processName = app.getProcessName();

        if (packageName == null || processName == null) {
            return;
        }

        sIsGms = packageName.equals(PACKAGE_GMS) && processName.equals(PROCESS_GMS_UNSTABLE);
        sIsFinsky = packageName.equals(PACKAGE_FINSKY);
        sIsPhotos = packageName.equals(PACKAGE_GPHOTOS);

        if (sIsGms) {
            dlog("Setting ASUS_X00HD fingerprint for: " + packageName);
            spoofBuildGms();
        } else if (!sCertifiedFp.isEmpty() && sIsFinsky) {
            dlog("Setting certified fingerprint for: " + packageName);
            setPropValue("FINGERPRINT", sCertifiedFp);
        } else if (!sStockFp.isEmpty() && packageName.equals(PACKAGE_ARCORE)) {
            dlog("Setting stock fingerprint for: " + packageName);
            setPropValue("FINGERPRINT", sStockFp);
        } else if (packageName.equals(PACKAGE_SUBSCRIPTION_RED) || packageName.equals(PACKAGE_TURBO)
                   || packageName.equals(PACKAGE_VELVET) || packageName.equals(PACKAGE_GBOARD) || packageName.equals(PACKAGE_SETUPWIZARD) || packageName.equals(PACKAGE_GMS)) {
            dlog("Spoofing Pixel 8 Pro for: " + packageName);
            sP8Props.forEach((k, v) -> setPropValue(k, v));
        } else if (sIsPhotos) {
            dlog("Spoofing Pixel XL for Google Photos");
            sPixelXLProps.forEach((k, v) -> setPropValue(k, v));
        }
    }

    private static void setPropValue(String key, Object value){
        try {
            dlog("Setting prop " + key + " to " + value.toString());
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    private static void setVersionField(String key, Integer value) {
        try {
            // Unlock
            Field field = Build.VERSION.class.getDeclaredField(key);
            field.setAccessible(true);
            // Edit
            field.set(null, value);
            // Lock
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static void spoofBuildGms() {
        // Alter build parameters to Nexus 5X for avoiding hardware attestation enforcement
        setPropValue("DEVICE", "bullhead");
        setPropValue("FINGERPRINT", "google/bullhead/bullhead:8.0.0/OPR6.170623.013/4283548:user/release-keys");
        setPropValue("MODEL", "Nexus 5X");
        setPropValue("PRODUCT", "bullhead");
        setVersionField("DEVICE_INITIAL_SDK_INT", Build.VERSION_CODES.N);
    }

    private static boolean isCallerSafetyNet() {
        return sIsGms && Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet or Play Integrity
        if (isCallerSafetyNet() || sIsFinsky) {
            dlog("Blocked key attestation sIsGms=" + sIsGms + " sIsFinsky=" + sIsFinsky);
            throw new UnsupportedOperationException();
        }
    }

    public static boolean hasSystemFeature(String name, boolean has) {
        if (sIsPhotos) {
            if (has && sPixelFeatures.stream().anyMatch(name::contains)) {
                dlog("Blocked system feature " + name + " for Google Photos");
                has = false;
            } else if (!has && name.equalsIgnoreCase(FEATURE_NEXUS_PRELOAD)) {
                dlog("Enabled system feature " + name + " for Google Photos");
                has = true;
            }
        }
        return has;
    }

    public static void dlog(String msg) {
      if (DEBUG) Log.d(TAG, msg);
    }
}
