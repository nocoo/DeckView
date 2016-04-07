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
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import me.lizheng.deckview.R;
import me.lizheng.deckview.helpers.DeckViewConfig;
import me.lizheng.deckview.utilities.DVConstants;
import me.lizheng.deckview.utilities.DVUtils;

/* The task bar view */
public class DeckChildViewHeader extends FrameLayout {

    DeckViewConfig mConfig;

    // Header views
    ImageView mDismissButton;
    ImageView mApplicationIcon;
    TextView mActivityDescription;

    // Header drawables
    boolean mCurrentPrimaryColorIsDark;
    int mCurrentPrimaryColor;
    int mBackgroundColor;
    Drawable mLightDismissDrawable;
    Drawable mDarkDismissDrawable;
    GradientDrawable mBackgroundColorDrawable;
    AnimatorSet mFocusAnimator;
    String mDismissContentDescription;

    // Static highlight that we draw at the top of each view
    static Paint sHighlightPaint;

    // Header dim, which is only used when task view hardware layers are not used
    Paint mDimLayerPaint = new Paint();
    PorterDuffColorFilter mDimColorFilter = new PorterDuffColorFilter(0, PorterDuff.Mode.SRC_ATOP);

    public DeckChildViewHeader(Context context) {
        this(context, null);
    }

    public DeckChildViewHeader(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DeckChildViewHeader(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mConfig = DeckViewConfig.getInstance();
        setWillNotDraw(false);

        // Load the dismiss resources
        Resources res = context.getResources();
        mLightDismissDrawable = ContextCompat.getDrawable(context, R.drawable.deck_child_view_dismiss_light);
        mDarkDismissDrawable = ContextCompat.getDrawable(context, R.drawable.deck_child_view_dismiss_dark);
        mDismissContentDescription =
                res.getString(R.string.accessibility_item_will_be_dismissed);

        // Configure the highlight paint
        if (sHighlightPaint == null) {
            sHighlightPaint = new Paint();
            sHighlightPaint.setStyle(Paint.Style.STROKE);
            sHighlightPaint.setStrokeWidth(mConfig.taskViewHighlightPx);
            sHighlightPaint.setColor(mConfig.taskBarViewHighlightColor);
            sHighlightPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.ADD));
            sHighlightPaint.setAntiAlias(true);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        // Initialize the icon and description views
        mApplicationIcon = (ImageView) findViewById(R.id.application_icon);
        mActivityDescription = (TextView) findViewById(R.id.activity_description);
        mDismissButton = (ImageView) findViewById(R.id.dismiss_task);

        // Hide the backgrounds if they are ripple drawables
//        if (!DVConstants.DebugFlags.App.EnableTaskFiltering) {
//            if (mApplicationIcon.getBackground() instanceof RippleDrawable) {
//                mApplicationIcon.setBackgroundDrawable(null);
//            }
//        }

        mBackgroundColorDrawable = (GradientDrawable) ContextCompat.getDrawable(getContext(), R.drawable
                .deck_child_view_header_bg_color);
        setBackgroundDrawable(mBackgroundColorDrawable);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Draw the highlight at the top edge (but put the bottom edge just out of view)
        float offset = (float) Math.ceil(mConfig.taskViewHighlightPx / 2f);
        float radius = mConfig.taskViewRoundedCornerRadiusPx;
        int count = canvas.save(Canvas.CLIP_SAVE_FLAG);
        canvas.clipRect(0, 0, getMeasuredWidth(), getMeasuredHeight());
        canvas.drawRoundRect(new RectF(-offset, 0f, (float) getMeasuredWidth() + offset,
                getMeasuredHeight() + radius), radius, radius, sHighlightPaint);
        canvas.restoreToCount(count);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    /**
     * Sets the dim alpha, only used when we are not using hardware layers.
     * (see RecentsConfiguration.useHardwareLayers)
     */
    void setDimAlpha(int alpha) {
        mDimColorFilter = new PorterDuffColorFilter(Color.argb(alpha, 0, 0, 0),
                PorterDuff.Mode.SRC_ATOP);
        mDimLayerPaint.setColorFilter(mDimColorFilter);
        setLayerType(LAYER_TYPE_HARDWARE, mDimLayerPaint);
    }

    /**
     * Returns the secondary color for a primary color.
     */
    int getSecondaryColor(int primaryColor, boolean useLightOverlayColor) {
        int overlayColor = useLightOverlayColor ? Color.WHITE : Color.BLACK;
        return DVUtils.getColorWithOverlay(primaryColor, overlayColor, 0.8f);
    }

    /**
     * Binds the bar view to the task
     */
    //public void rebindToTask(Task t) {
    public void rebindToTask(Drawable headerIcon, String headerTitle, int headerBgColor) {
        // If an activity icon is defined, then we use that as the primary icon to show in the bar,
        // otherwise, we fall back to the application icon
        mApplicationIcon.setImageDrawable(headerIcon);
        mApplicationIcon.setContentDescription(headerTitle);

        mActivityDescription.setText(headerTitle);

        // Try and apply the system ui tint
        int existingBgColor = (getBackground() instanceof ColorDrawable) ?
                ((ColorDrawable) getBackground()).getColor() : 0;
        if (existingBgColor != headerBgColor) {
            mBackgroundColorDrawable.setColor(headerBgColor);
            mBackgroundColor = headerBgColor;
        }
        mCurrentPrimaryColor = headerBgColor;
        //mCurrentPrimaryColorIsDark = t.useLightOnPrimaryColor;
        mActivityDescription.setTextColor(mConfig.taskBarViewLightTextColor);
        mDismissButton.setImageDrawable(mLightDismissDrawable);
        mDismissButton.setContentDescription(String.format(mDismissContentDescription,
                headerTitle));
    }

    /**
     * Unbinds the bar view from the task
     */
    void unbindFromTask() {
        mApplicationIcon.setImageDrawable(null);
    }

    /**
     * Animates this task bar dismiss button when launching a task.
     */
    void startLaunchTaskDismissAnimation() {
        if (mDismissButton.getVisibility() == View.VISIBLE) {
            mDismissButton.animate().cancel();
            mDismissButton.animate()
                    .alpha(0f)
                    .setStartDelay(0)
                    .setInterpolator(mConfig.fastOutSlowInInterpolator)
                    .setDuration(mConfig.taskViewExitToAppDuration)
//                    .withLayer()
                    .start();
        }
    }

    /**
     * Animates this task bar if the user does not interact with the stack after a certain time.
     */
    void startNoUserInteractionAnimation() {
        if (mDismissButton.getVisibility() != View.VISIBLE) {
            mDismissButton.setVisibility(View.VISIBLE);
            mDismissButton.setAlpha(0f);
            mDismissButton.animate()
                    .alpha(1f)
                    .setStartDelay(0)
                    .setInterpolator(mConfig.fastOutLinearInInterpolator)
                    .setDuration(mConfig.taskViewEnterFromAppDuration)
//                    .withLayer()
                    .start();
        }
    }

    /**
     * Mark this task view that the user does has not interacted with the stack after a certain time.
     */
    void setNoUserInteractionState() {
        if (mDismissButton.getVisibility() != View.VISIBLE) {
            mDismissButton.animate().cancel();
            mDismissButton.setVisibility(View.VISIBLE);
            mDismissButton.setAlpha(1f);
        }
    }

    /**
     * Resets the state tracking that the user has not interacted with the stack after a certain time.
     */
    void resetNoUserInteractionState() {
        mDismissButton.setVisibility(View.INVISIBLE);
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {

        // Don't forward our state to the drawable - we do it manually in onTaskViewFocusChanged.
        // This is to prevent layer trashing when the view is pressed.
        return new int[]{};
    }

    /**
     * Notifies the associated TaskView has been focused.
     */
    void onTaskViewFocusChanged(boolean focused, boolean animateFocusedState) {
        // If we are not animating the visible state, just return
        if (!animateFocusedState) return;

        boolean isRunning = false;
        if (mFocusAnimator != null) {
            isRunning = mFocusAnimator.isRunning();
            DVUtils.cancelAnimationWithoutCallbacks(mFocusAnimator);
        }

        if (focused) {
//            int secondaryColor = getSecondaryColor(mCurrentPrimaryColor, mCurrentPrimaryColorIsDark);
//            int[][] states = new int[][]{
//                    new int[]{android.R.attr.state_enabled},
//                    new int[]{android.R.attr.state_pressed}
//            };
//            int[] newStates = new int[]{
//                    android.R.attr.state_enabled,
//                    android.R.attr.state_pressed
//            };
//            int[] colors = new int[]{
//                    secondaryColor,
//                    secondaryColor
//            };

            // Pulse the background color
            int currentColor = mBackgroundColor;
            int lightPrimaryColor = getSecondaryColor(mCurrentPrimaryColor, mCurrentPrimaryColorIsDark);
            ValueAnimator backgroundColor = ValueAnimator.ofObject(new ArgbEvaluator(),
                    currentColor, lightPrimaryColor);
//            backgroundColor.addListener(new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationStart(Animator animation) {
//                    mBackground.setState(new int[]{});
//                }
//            });
            backgroundColor.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int color = (int) animation.getAnimatedValue();
                    mBackgroundColorDrawable.setColor(color);
                    mBackgroundColor = color;
                }
            });

            backgroundColor.setRepeatCount(ValueAnimator.INFINITE);
            backgroundColor.setRepeatMode(ValueAnimator.REVERSE);

            // Pulse the translation
            ObjectAnimator translation = ObjectAnimator.ofFloat(this, "translationZ", 15f);
            translation.setRepeatCount(ValueAnimator.INFINITE);
            translation.setRepeatMode(ValueAnimator.REVERSE);

            mFocusAnimator = new AnimatorSet();
            mFocusAnimator.playTogether(backgroundColor, translation);
            mFocusAnimator.setStartDelay(750);
            mFocusAnimator.setDuration(750);
            mFocusAnimator.start();
        } else if (isRunning) {
            // Restore the background color
            int currentColor = mBackgroundColor;
            ValueAnimator backgroundColor = ValueAnimator.ofObject(new ArgbEvaluator(),
                    currentColor, mCurrentPrimaryColor);
            backgroundColor.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int color = (int) animation.getAnimatedValue();
                    mBackgroundColorDrawable.setColor(color);
                    mBackgroundColor = color;
                }
            });

            // Restore the translation
            ObjectAnimator translation = ObjectAnimator.ofFloat(this, "translationZ", 0f);

            mFocusAnimator = new AnimatorSet();
            mFocusAnimator.playTogether(backgroundColor, translation);
            mFocusAnimator.setDuration(150);
            mFocusAnimator.start();
        }
    }
}
