/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.systemui.statusbar.notification.collection.coordinator;

import android.annotation.Nullable;

import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.notification.collection.ListEntry;
import com.android.systemui.statusbar.notification.collection.NotifPipeline;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.collection.coordinator.dagger.CoordinatorScope;
import com.android.systemui.statusbar.notification.collection.listbuilder.pluggable.NotifFilter;
import com.android.systemui.statusbar.notification.collection.listbuilder.pluggable.NotifSectioner;
import com.android.systemui.statusbar.notification.collection.provider.HighPriorityProvider;
import com.android.systemui.statusbar.notification.collection.provider.SectionStyleProvider;
import com.android.systemui.statusbar.notification.collection.render.NodeController;
import com.android.systemui.statusbar.notification.dagger.AlertingHeader;
import com.android.systemui.statusbar.notification.dagger.SilentHeader;
import com.android.systemui.statusbar.notification.stack.NotificationPriorityBucketKt;

import java.util.Collections;

import javax.inject.Inject;

/**
 * Filters out NotificationEntries based on its Ranking and dozing state.
 * Assigns alerting / silent section based on the importance of the notification entry.
 * We check the NotificationEntry's Ranking for:
 *  - whether the notification's app is suspended or hiding its notifications
 *  - whether DND settings are hiding notifications from ambient display or the notification list
 */
@CoordinatorScope
public class RankingCoordinator implements Coordinator {
    public static final boolean SHOW_ALL_SECTIONS = false;
    private final StatusBarStateController mStatusBarStateController;
    private final HighPriorityProvider mHighPriorityProvider;
    private final SectionStyleProvider mSectionStyleProvider;
    private final NodeController mSilentNodeController;
    private final NodeController mAlertingHeaderController;
    private final AlertingNotifSectioner mAlertingNotifSectioner = new AlertingNotifSectioner();
    private final SilentNotifSectioner mSilentNotifSectioner = new SilentNotifSectioner();
    private final MinimizedNotifSectioner mMinimizedNotifSectioner = new MinimizedNotifSectioner();

    @Inject
    public RankingCoordinator(
            StatusBarStateController statusBarStateController,
            HighPriorityProvider highPriorityProvider,
            SectionStyleProvider sectionStyleProvider,
            @AlertingHeader NodeController alertingHeaderController,
            @SilentHeader NodeController silentNodeController) {
        mStatusBarStateController = statusBarStateController;
        mHighPriorityProvider = highPriorityProvider;
        mSectionStyleProvider = sectionStyleProvider;
        mAlertingHeaderController = alertingHeaderController;
        mSilentNodeController = silentNodeController;
    }

    @Override
    public void attach(NotifPipeline pipeline) {
        mStatusBarStateController.addCallback(mStatusBarStateCallback);
        mSectionStyleProvider.setMinimizedSections(Collections.singleton(mMinimizedNotifSectioner));

        pipeline.addPreGroupFilter(mSuspendedFilter);
        pipeline.addPreGroupFilter(mDndVisualEffectsFilter);
    }

    public NotifSectioner getAlertingSectioner() {
        return mAlertingNotifSectioner;
    }

    public NotifSectioner getSilentSectioner() {
        return mSilentNotifSectioner;
    }

    public NotifSectioner getMinimizedSectioner() {
        return mMinimizedNotifSectioner;
    }

    /**
     * Checks whether to filter out the given notification based the notification's Ranking object.
     * NotifListBuilder invalidates the notification list each time the ranking is updated,
     * so we don't need to explicitly invalidate this filter on ranking update.
     */
    private final NotifFilter mSuspendedFilter = new NotifFilter("IsSuspendedFilter") {
        @Override
        public boolean shouldFilterOut(NotificationEntry entry, long now) {
            return entry.getRanking().isSuspended();
        }
    };

    private final NotifFilter mDndVisualEffectsFilter = new NotifFilter(
            "DndSuppressingVisualEffects") {
        @Override
        public boolean shouldFilterOut(NotificationEntry entry, long now) {
            if (mStatusBarStateController.isDozing() && entry.shouldSuppressAmbient()) {
                return true;
            }

            return !mStatusBarStateController.isDozing() && entry.shouldSuppressNotificationList();
        }
    };

    private final StatusBarStateController.StateListener mStatusBarStateCallback =
            new StatusBarStateController.StateListener() {
                @Override
                public void onDozingChanged(boolean isDozing) {
                    mDndVisualEffectsFilter.invalidateList();
                }
            };

    private class AlertingNotifSectioner extends NotifSectioner {

        AlertingNotifSectioner() {
            super("Alerting", NotificationPriorityBucketKt.BUCKET_ALERTING);
        }

        @Override
        public boolean isInSection(ListEntry entry) {
            return mHighPriorityProvider.isHighPriority(entry);
        }

        @Nullable
        @Override
        public NodeController getHeaderNodeController() {
            // TODO: remove SHOW_ALL_SECTIONS, this redundant method, and mAlertingHeaderController
            if (SHOW_ALL_SECTIONS) {
                return mAlertingHeaderController;
            }
            return null;
        }
    }

    private class SilentNotifSectioner extends NotifSectioner {

        SilentNotifSectioner() {
            super("Silent", NotificationPriorityBucketKt.BUCKET_SILENT);
        }

        @Override
        public boolean isInSection(ListEntry entry) {
            return !mHighPriorityProvider.isHighPriority(entry)
                    && !entry.getRepresentativeEntry().isAmbient();
        }

        @Nullable
        @Override
        public NodeController getHeaderNodeController() {
            return mSilentNodeController;
        }
    }

    private class MinimizedNotifSectioner extends NotifSectioner {

        MinimizedNotifSectioner() {
            super("Minimized", NotificationPriorityBucketKt.BUCKET_SILENT);
        }

        @Override
        public boolean isInSection(ListEntry entry) {
            return !mHighPriorityProvider.isHighPriority(entry)
                    && entry.getRepresentativeEntry().isAmbient();
        }

        @Nullable
        @Override
        public NodeController getHeaderNodeController() {
            return mSilentNodeController;
        }
    }
}
