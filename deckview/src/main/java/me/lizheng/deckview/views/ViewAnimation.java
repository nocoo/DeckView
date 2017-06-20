/*
 * Copyright (C) 2016 Zheng Li <https://lizheng.me>
 * Copyright (C) 2014 The Android Open Source Project
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

package me.lizheng.deckview.views;

import android.animation.ValueAnimator;
import android.graphics.Rect;

import me.lizheng.deckview.helpers.DeckChildViewTransform;
import me.lizheng.deckview.utilities.ReferenceCountedTrigger;

/* Common code related to view animations */
class ViewAnimation {

    /* The animation context for a task view animation into Recents */
    static class TaskViewEnterContext {
        // A trigger to run some logic when all the animations complete.  This works around the fact
        // that it is difficult to coordinate ViewPropertyAnimators
        ReferenceCountedTrigger postAnimationTrigger;
        // An update listener to notify as the enter animation progresses (used for the home transition)
        ValueAnimator.AnimatorUpdateListener updateListener;

        // These following properties are updated for each task view we start the enter animation on

        // Whether or not the current task occludes the launch target
        boolean currentTaskOccludesLaunchTarget;
        // The task rect for the current stack
        Rect currentTaskRect;
        // The transform of the current task view
        DeckChildViewTransform currentTaskTransform;
        // The view index of the current task view
        int currentStackViewIndex;
        // The total number of task views
        int currentStackViewCount;

        TaskViewEnterContext(ReferenceCountedTrigger t) {
            postAnimationTrigger = t;
        }
    }

    /* The animation context for a task view animation out of Recents */
    static class TaskViewExitContext {
        // A trigger to run some logic when all the animations complete.  This works around the fact
        // that it is difficult to coordinate ViewPropertyAnimators
        ReferenceCountedTrigger postAnimationTrigger;

        // The translationY to apply to a TaskView to move it off the bottom of the task stack
        int offscreenTranslationY;

        TaskViewExitContext(ReferenceCountedTrigger t) {
            postAnimationTrigger = t;
        }
    }
}
