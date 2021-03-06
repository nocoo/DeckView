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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import me.lizheng.deckview.R;
import me.lizheng.deckview.helpers.DeckChildViewTransform;
import me.lizheng.deckview.helpers.DeckViewConfig;
import me.lizheng.deckview.helpers.FakeShadowDrawable;
import me.lizheng.deckview.utilities.DVUtils;

/* A task view */
public class DeckChildView<T> extends FrameLayout implements
        View.OnClickListener, View.OnLongClickListener {

    /**
     * The TaskView callbacks
     */
    interface DeckChildViewCallbacks<T> {
        //void onDeckChildViewAppIconClicked(DeckChildView dcv);
        void onDeckChildViewAppInfoClicked(DeckChildView dcv);
        void onDeckChildViewClicked(DeckChildView<T> dcv, T key);
        void onDeckChildViewDismissed(DeckChildView<T> dcv);
        void onDeckChildViewClipStateChanged(DeckChildView dcv);
        void onDeckChildViewFocusChanged(DeckChildView<T> dcv, boolean focused);
    }

    DeckViewConfig mConfig;

    float mTaskProgress;
    ObjectAnimator mTaskProgressAnimator;
    float mMaxDimScale;
    int mDimAlpha;
    AccelerateInterpolator mDimInterpolator = new AccelerateInterpolator(1f);
    PorterDuffColorFilter mDimColorFilter = new PorterDuffColorFilter(0, PorterDuff.Mode.SRC_ATOP);
    Paint mDimLayerPaint = new Paint();

    T mKey;
    boolean mIsFocused;
    boolean mFocusAnimationsEnabled;
    boolean mClipViewInStack;

    View mContent;
    DeckChildViewThumbnail mThumbnailView;
    DeckChildViewHeader mHeaderView;
    DeckChildViewCallbacks<T> mCb;

    // Optimizations
    ValueAnimator.AnimatorUpdateListener mUpdateDimListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    setTaskProgress((Float) animation.getAnimatedValue());
                }
            };


    public DeckChildView(Context context) {
        this(context, null);
    }

    public DeckChildView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeckChildView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mConfig = DeckViewConfig.getInstance();
        mMaxDimScale = mConfig.taskStackMaxDim / 255f;
        mClipViewInStack = true;
        setTaskProgress(getTaskProgress());
        setDim(getDim());

        if (mConfig.fakeShadows) {
            setBackgroundDrawable(new FakeShadowDrawable(context, mConfig));
        }
    }

    /**
     * Set callback
     */
    void setCallbacks(DeckChildViewCallbacks cb) {
        //noinspection unchecked
        mCb = cb;
    }

    /**
     * Resets this TaskView for reuse.
     */
    void reset() {
        resetViewProperties();
        resetNoUserInteractionState();
        setClipViewInStack(false);
        setCallbacks(null);
    }

    /**
     * Gets the task
     */
    T getAttachedKey() {
        return mKey;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // Bind the views
        mContent = findViewById(R.id.task_view_content);
        mHeaderView = (DeckChildViewHeader) findViewById(R.id.task_view_bar);
        mThumbnailView = (DeckChildViewThumbnail) findViewById(R.id.task_view_thumbnail);
        mThumbnailView.updateClipToTaskBar(mHeaderView);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int widthWithoutPadding = width - getPaddingLeft() - getPaddingRight();
//        int heightWithoutPadding = height - getPaddingTop() - getPaddingBottom();

        // Measure the content
        mContent.measure(MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY));

        // Measure the bar view, and action button
        mHeaderView.measure(MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(mConfig.taskBarHeight, MeasureSpec.EXACTLY));

        // Measure the thumbnail to be square
        mThumbnailView.measure(
                MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(widthWithoutPadding, MeasureSpec.EXACTLY));
        setMeasuredDimension(width, height);
    }

    /**
     * Synchronizes this view's properties with the task's transform
     */
    void updateViewPropertiesToTaskTransform(DeckChildViewTransform toTransform, int duration) {
        updateViewPropertiesToTaskTransform(toTransform, duration, null);
    }

    void updateViewPropertiesToTaskTransform(DeckChildViewTransform toTransform, int duration,
                                             ValueAnimator.AnimatorUpdateListener updateCallback) {
        // Apply the transform
        toTransform.applyToTaskView(this, duration, mConfig.fastOutSlowInInterpolator, !mConfig.fakeShadows);

        // Update the task progress
        DVUtils.cancelAnimationWithoutCallbacks(mTaskProgressAnimator);
        if (duration <= 0) {
            setTaskProgress(toTransform.p);
        } else {
            mTaskProgressAnimator = ObjectAnimator.ofFloat(this, "taskProgress", toTransform.p);
            mTaskProgressAnimator.setDuration(duration);
            mTaskProgressAnimator.addUpdateListener(mUpdateDimListener);
            mTaskProgressAnimator.start();
        }
    }

    /**
     * Resets this view's properties
     */
    void resetViewProperties() {
        setDim(0);
        setLayerType(View.LAYER_TYPE_NONE, null);
        DeckChildViewTransform.reset(this);
    }

    /**
     * Prepares this task view for the enter-recents animations.  This is called earlier in the
     * first layout because the actual animation into recents may take a long time.
     */
    void prepareEnterRecentsAnimation(boolean isTaskViewLaunchTargetTask,
                                      boolean occludesLaunchTarget, int offscreenY) {
        int initialDim = getDim();

        //noinspection StatementWithEmptyBody
        if (mConfig.launchedHasConfigurationChanged) {
            // Just load the views as-is
        } else if (mConfig.launchedFromAppWithThumbnail) {
            if (isTaskViewLaunchTargetTask) {
                // Set the dim to 0 so we can animate it in
                initialDim = 0;
            } else if (occludesLaunchTarget) {
                // Move the task view off screen (below) so we can animate it in
                setTranslationY(offscreenY);
            }

        } else if (mConfig.launchedFromHome) {
            // Move the task view off screen (below) so we can animate it in
            setTranslationY(offscreenY);
            ViewCompat.setTranslationY(this, 0);
            setScaleX(1f);
            setScaleY(1f);
        }
        // Apply the current dim
        setDim(initialDim);
        // Prepare the thumbnail view alpha
        mThumbnailView.prepareEnterRecentsAnimation(isTaskViewLaunchTargetTask);
    }

    /**
     * Animates this task view as it enters recents
     */
    void startEnterRecentsAnimation(final ViewAnimation.TaskViewEnterContext ctx) {
        Log.i(getClass().getSimpleName(), "startEnterRecentsAnimation");
        final DeckChildViewTransform transform = ctx.currentTaskTransform;
        int startDelay = 0;

        if (mConfig.launchedFromHome) {
            Log.i(getClass().getSimpleName(), "mConfig.launchedFromHome false");

            // Animate the tasks up
            int frontIndex = (ctx.currentStackViewCount - ctx.currentStackViewIndex - 1);
            int delay = mConfig.transitionEnterFromHomeDelay +
                    frontIndex * mConfig.taskViewEnterFromHomeStaggerDelay;

            setScaleX(transform.scale);
            setScaleY(transform.scale);

            ObjectAnimator animator = ObjectAnimator.ofFloat(this, "TranslationY", getTranslationY(), transform.translationY);
            animator.addUpdateListener(ctx.updateListener);
            animator.setDuration(mConfig.taskViewEnterFromHomeDuration + frontIndex * mConfig.taskViewEnterFromHomeStaggerDelay);
            animator.setStartDelay(delay);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    // Decrement the post animation trigger
                    ctx.postAnimationTrigger.decrement();
                }
            });
            animator.start();

            ctx.postAnimationTrigger.increment();
            startDelay = delay;
        }

        // Enable the focus animations from this point onwards so that they aren't affected by the
        // window transitions
        postDelayed(new Runnable() {
            @Override
            public void run() {
                enableFocusAnimations();
            }
        }, startDelay);
    }

    private float getSize() {
        final DisplayMetrics dm = getContext().getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    /**
     * Animates the deletion of this task view
     */
    void startDeleteTaskAnimation(final Runnable r) {
        // Disabling clipping with the stack while the view is animating away
        setClipViewInStack(false);
        ValueAnimator anim = ObjectAnimator.ofFloat(this,
                View.TRANSLATION_X , getSize());
        anim.setInterpolator(new LinearInterpolator());
        anim.setDuration(100);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                setClipViewInStack(true);

                setTouchEnabled(true);
                r.run();
            }
        });
        anim.start();
    }

    /**
     * Animates this task view if the user does not interact with the stack after a certain time.
     */
    void startNoUserInteractionAnimation() {
        mHeaderView.startNoUserInteractionAnimation();
    }

    /**
     * Mark this task view that the user does has not interacted with the stack after a certain time.
     */
    void setNoUserInteractionState() {
        mHeaderView.setNoUserInteractionState();
    }

    /**
     * Resets the state tracking that the user has not interacted with the stack after a certain time.
     */
    void resetNoUserInteractionState() {
        mHeaderView.resetNoUserInteractionState();
    }

    /**
     * Dismisses this task.
     */
    void dismissTask() {
        // Animate out the view and call the callback
        final DeckChildView<T> tv = this;
        startDeleteTaskAnimation(new Runnable() {
            @Override
            public void run() {
                if (mCb != null) {
                    mCb.onDeckChildViewDismissed(tv);
                }
            }
        });
    }

    /**
     * Returns whether this view should be clipped, or any views below should clip against this
     * view.
     */
    boolean shouldClipViewInStack() {
        return mClipViewInStack && (getVisibility() == View.VISIBLE);
    }

    /**
     * Sets whether this view should be clipped, or clipped against.
     */
    public void setClipViewInStack(boolean clip) {
        if (clip != mClipViewInStack) {
            mClipViewInStack = clip;
            if (mCb != null) {
                mCb.onDeckChildViewClipStateChanged(this);
            }
        }
    }

    /**
     * Sets the current task progress.
     */
    public void setTaskProgress(float p) {
        mTaskProgress = p;
        updateDimFromTaskProgress();
    }

    /**
     * Returns the current task progress.
     */
    public float getTaskProgress() {
        return mTaskProgress;
    }

    /**
     * Returns the current dim.
     */
    public void setDim(int dim) {
        mDimAlpha = dim;
        if (mConfig.useHardwareLayers) {
            // Defer setting hardware layers if we have not yet measured, or there is no dim to draw
            if (getMeasuredWidth() > 0 && getMeasuredHeight() > 0) {
                mDimColorFilter =
                        new PorterDuffColorFilter(Color.argb(mDimAlpha, 0, 0, 0),
                                PorterDuff.Mode.SRC_ATOP);
                mDimLayerPaint.setColorFilter(mDimColorFilter);
                mContent.setLayerType(LAYER_TYPE_HARDWARE, mDimLayerPaint);
            }
        } else {
            float dimAlpha = mDimAlpha / 255.0f;
            if (mThumbnailView != null) {
                mThumbnailView.setDimAlpha(dimAlpha);
            }
        }
    }

    /**
     * Returns the current dim.
     */
    public int getDim() {
        return mDimAlpha;
    }

    /**
     * Compute the dim as a function of the scale of this view.
     */
    int getDimFromTaskProgress() {
        float dim = mMaxDimScale * mDimInterpolator.getInterpolation(1f - mTaskProgress);
        return (int) (dim * 255);
    }

    /**
     * Update the dim as a function of the scale of this view.
     */
    void updateDimFromTaskProgress() {
        setDim(getDimFromTaskProgress());
    }

    /**** View focus state ****/

    /**
     * Sets the focused task explicitly. We need a separate flag because requestFocus() won't happen
     * if the view is not currently visible, or we are in touch state (where we still want to keep
     * track of focus).
     */
    public void setFocusedTask(boolean animateFocusedState) {
        mIsFocused = true;
        if (mFocusAnimationsEnabled) {
            // Focus the header bar
            mHeaderView.onTaskViewFocusChanged(true, animateFocusedState);
        }
        // Update the thumbnail alpha with the focus
        mThumbnailView.onFocusChanged(true);
        // Call the callback
        if (mCb != null) {
            mCb.onDeckChildViewFocusChanged(this, true);
        }
        // Workaround, we don't always want it focusable in touch mode, but we want the first task
        // to be focused after the enter-recents animation, which can be triggered from either touch
        // or keyboard
        setFocusableInTouchMode(true);
        requestFocus();
        setFocusableInTouchMode(false);
        invalidate();
    }

    /**
     * Unsets the focused task explicitly.
     */
    void unsetFocusedTask() {
        mIsFocused = false;
        if (mFocusAnimationsEnabled) {
            // Un-focus the header bar
            mHeaderView.onTaskViewFocusChanged(false, true);
        }

        // Update the thumbnail alpha with the focus
        mThumbnailView.onFocusChanged(false);
        // Call the callback
        if (mCb != null) {
            mCb.onDeckChildViewFocusChanged(this, false);
        }
        invalidate();
    }

    /**
     * Updates the explicitly focused state when the view focus changes.
     */
    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus) {
            unsetFocusedTask();
        }
    }

    /**
     * Returns whether we have explicitly been focused.
     */
    public boolean isFocusedTask() {
        return mIsFocused || isFocused();
    }

    /**
     * Enables all focus animations.
     */
    void enableFocusAnimations() {
        boolean wasFocusAnimationsEnabled = mFocusAnimationsEnabled;
        mFocusAnimationsEnabled = true;
        if (mIsFocused && !wasFocusAnimationsEnabled) {
            // Re-notify the header if we were focused and animations were not previously enabled
            mHeaderView.onTaskViewFocusChanged(true, true);
        }
    }

    /**** TaskCallbacks Implementation ****/

    /**
     * Binds this task view to the task
     */
    public void onTaskBound(T key) {
        mKey = key;
    }

    private boolean isBound() {
        return mKey != null;
    }

    /**
     * Binds this task view to the task
     */
    public void onTaskUnbound() {
        mKey = null;
    }

    public void onDataLoaded(T key, Bitmap thumbnail, Drawable headerIcon,
                             String headerTitle, int headerBgColor) {
        if (!isBound() || !mKey.equals(key))
            return;

        if (mThumbnailView != null && mHeaderView != null) {
            // Bind each of the views to the new task data
            mThumbnailView.rebindToTask(thumbnail);
            mHeaderView.rebindToTask(headerIcon, headerTitle, headerBgColor);
            // Rebind any listeners
            mHeaderView.mApplicationIcon.setOnClickListener(this);
            mHeaderView.mDismissButton.setOnClickListener(this);

            // TODO: Check if this functionality is needed
            mHeaderView.mApplicationIcon.setOnLongClickListener(this);
        }
    }

    public void onDataUnloaded() {
        if (mThumbnailView != null && mHeaderView != null) {
            // Unbind each of the views from the task data and remove the task callback
            mThumbnailView.unbindFromTask();
            mHeaderView.unbindFromTask();

            // Unbind any listeners
            mHeaderView.mApplicationIcon.setOnClickListener(null);
            mHeaderView.mDismissButton.setOnClickListener(null);
            mHeaderView.mApplicationIcon.setOnLongClickListener(null);
        }
    }

    /**
     * Enables/disables handling touch on this task view.
     */
    public void setTouchEnabled(boolean enabled) {
        setOnClickListener(enabled ? this : null);
    }

    /**
     * * View.OnClickListener Implementation ***
     */

    @Override
    public void onClick(final View v) {
        final DeckChildView<T> tv = this;
        final boolean delayViewClick = (v != this);
        if (delayViewClick) {
            // We purposely post the handler delayed to allow for the touch feedback to draw
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (v == mHeaderView.mDismissButton) {
                        dismissTask();
                    }
                }
            }, 125);
        } else {
            if (mCb != null) {
                mCb.onDeckChildViewClicked(tv, tv.getAttachedKey());
            }
        }
    }

    /**
     * * View.OnLongClickListener Implementation ***
     */

    @Override
    public boolean onLongClick(View v) {
        if (v == mHeaderView.mApplicationIcon) {
            if (mCb != null) {
                mCb.onDeckChildViewAppInfoClicked(this);
                return true;
            }
        }
        return false;
    }
}
