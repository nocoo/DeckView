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

package me.lizheng.deckview.utilities;

import android.content.Context;

import java.util.ArrayList;

/**
 * A ref counted trigger that does some logic when the count is first incremented, or last
 * decremented.  Not thread safe as it's not currently needed.
 */
public class ReferenceCountedTrigger {

    Context mContext;
    int mCount;
    ArrayList<Runnable> mFirstIncRunnables = new ArrayList<>();
    ArrayList<Runnable> mLastDecRunnables = new ArrayList<>();
    Runnable mErrorRunnable;

    public ReferenceCountedTrigger(Context context, Runnable firstIncRunnable,
                                   Runnable lastDecRunnable, Runnable errorRunanable) {
        mContext = context;
        if (firstIncRunnable != null) mFirstIncRunnables.add(firstIncRunnable);
        if (lastDecRunnable != null) mLastDecRunnables.add(lastDecRunnable);
        mErrorRunnable = errorRunanable;
    }

    /**
     * Increments the ref count
     */
    public void increment() {
        if (mCount == 0 && !mFirstIncRunnables.isEmpty()) {
            int numRunnables = mFirstIncRunnables.size();
            for (int i = 0; i < numRunnables; i++) {
                mFirstIncRunnables.get(i).run();
            }
        }
        mCount++;
    }

    /**
     * Adds a runnable to the last-decrement runnables list.
     */
    public void addLastDecrementRunnable(Runnable r) {
        // To ensure that the last decrement always calls, we increment and decrement after setting
        // the last decrement runnable
        boolean ensureLastDecrement = (mCount == 0);
        if (ensureLastDecrement) increment();
        mLastDecRunnables.add(r);
        if (ensureLastDecrement) decrement();
    }

    /**
     * Decrements the ref count
     */
    public void decrement() {
        mCount--;
        if (mCount == 0 && !mLastDecRunnables.isEmpty()) {
            int numRunnables = mLastDecRunnables.size();
            for (int i = 0; i < numRunnables; i++) {
                mLastDecRunnables.get(i).run();
            }
        } else if (mCount < 0) {
            if (mErrorRunnable != null) {
                mErrorRunnable.run();
            } else {
                new Throwable("Invalid ref count").printStackTrace();
            }
        }
    }
}
