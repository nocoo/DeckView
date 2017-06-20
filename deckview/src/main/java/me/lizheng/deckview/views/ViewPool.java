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

import android.content.Context;

import java.util.Iterator;
import java.util.LinkedList;

/* A view pool to manage more views than we can visibly handle */
class ViewPool<V, T> {

    private Context mContext;
    private ViewPoolConsumer<V, T> mViewCreator;
    private LinkedList<V> mPool = new LinkedList<>();

    /**
     * Initializes the pool with a fixed predetermined pool size
     */
    ViewPool(Context context, ViewPoolConsumer<V, T> viewCreator) {
        mContext = context;
        mViewCreator = viewCreator;
    }

    /**
     * Returns a view into the pool
     */
    void returnViewToPool(V v) {
        mViewCreator.prepareViewToEnterPool(v);
        mPool.push(v);
    }

    /**
     * Gets a view from the pool and prepares it
     */
    V pickUpViewFromPool(T preferredData, T prepareData) {
        V v = null;
        boolean isNewView = false;
        if (mPool.isEmpty()) {
            v = mViewCreator.createView(mContext);
            isNewView = true;
        } else {
            // Try and find a preferred view
            Iterator<V> iter = mPool.iterator();
            while (iter.hasNext()) {
                V vpv = iter.next();
                if (mViewCreator.hasPreferredData(vpv, preferredData)) {
                    v = vpv;
                    iter.remove();
                    break;
                }
            }
            // Otherwise, just grab the first view
            if (v == null) {
                v = mPool.pop();
            }
        }
        mViewCreator.prepareViewToLeavePool(v, prepareData, isNewView);
        return v;
    }

    /**
     * Returns an iterator to the list of the views in the pool.
     */
    Iterator<V> poolViewIterator() {
        if (mPool != null) {
            return mPool.iterator();
        }
        return null;
    }

    /* An interface to the consumer of a view pool */
    public interface ViewPoolConsumer<V, T> {
        V createView(Context context);

        void prepareViewToEnterPool(V v);

        void prepareViewToLeavePool(V v, T prepareData, boolean isNewView);

        boolean hasPreferredData(V v, T preferredData);
    }
}
