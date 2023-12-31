/*
 * Copyright (C) 2022 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.systemui.bouncer.shared.model

import com.android.systemui.plugins.ActivityStarter

/** Encapsulates callbacks to be invoked by the bouncer logic. */
// TODO(b/243683121): Move dismiss logic from view controllers
data class BouncerCallbackActionsModel(
    val onDismissAction: ActivityStarter.OnDismissAction?,
    val cancelAction: Runnable?
)
