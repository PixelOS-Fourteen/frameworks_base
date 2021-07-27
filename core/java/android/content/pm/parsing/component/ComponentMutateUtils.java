/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.content.pm.parsing.component;

import android.annotation.NonNull;

/**
 * Contains mutation methods so that code doesn't have to cast to the Impl. Meant to eventually
 * be removed once all post-parsing mutation is moved to parsing.
 *
 * @hide
 */
public class ComponentMutateUtils {

    public static void setMaxAspectRatio(@NonNull ParsedActivity activity, int resizeMode,
            float maxAspectRatio) {
        ((ParsedActivityImpl) activity).setMaxAspectRatio(resizeMode, maxAspectRatio);
    }

    public static void setMinAspectRatio(@NonNull ParsedActivity activity, int resizeMode,
            float minAspectRatio) {
        ((ParsedActivityImpl) activity).setMinAspectRatio(resizeMode, minAspectRatio);
    }

    public static void setSupportsSizeChanges(@NonNull ParsedActivity activity,
            boolean supportsSizeChanges) {
        ((ParsedActivityImpl) activity).setSupportsSizeChanges(supportsSizeChanges);
    }

    public static void setResizeMode(@NonNull ParsedActivity activity, int resizeMode) {
        ((ParsedActivityImpl) activity).setResizeMode(resizeMode);
    }

    public static void setExactFlags(ParsedActivity activity, int exactFlags) {
        ((ParsedActivityImpl) activity).setFlags(exactFlags);
    }

    public static void setEnabled(@NonNull ParsedMainComponent component, boolean enabled) {
        ((ParsedMainComponentImpl) component).setEnabled(enabled);
    }

    public static void setPackageName(@NonNull ParsedComponent component,
            @NonNull String packageName) {
        ((ParsedComponentImpl) component).setPackageName(packageName);
    }

    public static void setDirectBootAware(@NonNull ParsedMainComponent component,
            boolean directBootAware) {
        ((ParsedMainComponentImpl) component).setDirectBootAware(directBootAware);
    }

    public static void setExported(@NonNull ParsedMainComponent component, boolean exported) {
        ((ParsedMainComponentImpl) component).setExported(exported);
    }
}
