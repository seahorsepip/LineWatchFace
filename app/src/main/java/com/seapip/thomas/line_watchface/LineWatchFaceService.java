/*
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

package com.seapip.thomas.line_watchface;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;

public class LineWatchFaceService extends CanvasWatchFaceService {

    // Left and right dial supported types.
    public static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_RANGED_VALUE, ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_LARGE_IMAGE}
    };
    // Unique IDs for each complication.
    private static final int TOP_DIAL_COMPLICATION = 0;
    private static final int LEFT_DIAL_COMPLICATION = 1;
    private static final int RIGHT_DIAL_COMPLICATION = 2;
    private static final int BACKGROUND_COMPLICATION = 3;
    // Left and right complication IDs as array for Complication API.
    public static final int[] COMPLICATION_IDS = {
            TOP_DIAL_COMPLICATION,
            LEFT_DIAL_COMPLICATION,
            RIGHT_DIAL_COMPLICATION,
            BACKGROUND_COMPLICATION
    };
    /*
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = 20;

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    private SharedPreferences mPrefs;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<LineWatchFaceService.Engine> mWeakReference;

        public EngineHandler(LineWatchFaceService.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            LineWatchFaceService.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {
        /* Handler to update the time once a second in interactive mode. */
        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean mRegisteredTimeZoneReceiver = false;
        private float mCenterX;
        private float mCenterY;

        /* Colors */
        private int mPrimaryColor;
        private int mSecondaryColor;
        private int mTertiaryColor;
        private int mQuaternaryColor;
        private int mBackgroundColor;

        private Paint mBackgroundOverlayPaint;
        private Paint mHourPaint;
        private Paint mMinutePaint;
        private Paint mSecondPaint;
        private Paint mHourTickPaint;
        private Paint mTickPaint;
        private Paint mComplicationArcValuePaint;
        private Paint mComplicationArcPaint;
        private Paint mComplicationCirclePaint;
        private Paint mComplicationPrimaryTextPaint;
        private Paint mComplicationTextPaint;
        private Paint mNotificationBackgroundPaint;
        private Paint mNotificationCirclePaint;
        private Paint mNotificationTextPaint;


        private Typeface mFontLight;
        private Typeface mFontBold;
        private Typeface mFont;

        private SparseArray<ComplicationData> mActiveComplicationDataSparseArray;

        private boolean mAmbient;
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;
        private boolean mIsRound;

        private RectF mLeftDialTapBox;
        private RectF mRightDialTapBox;

        private int mUnreadNotificationCount;
        private int mNotificationCount;

        private RectF[] mComplicationTapBoxes = new RectF[COMPLICATION_IDS.length];

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(LineWatchFaceService.this)
                    .setStatusBarGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL)
                    .setAcceptsTapEvents(true)
                    .build());

            mCalendar = Calendar.getInstance();


            mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            /* Set defaults for fonts */
            mFontLight = Typeface.create("sans-serif-light", Typeface.NORMAL);
            mFontBold = Typeface.create("sans-serif", Typeface.BOLD);
            mFont = Typeface.create("sans-serif", Typeface.NORMAL);

            /* Set defaults for colors */
            mSecondaryColor = Color.argb(128, 255, 255, 255);
            mTertiaryColor = Color.argb(76, 255, 255, 255);
            mQuaternaryColor = Color.argb(24, 255, 255, 255);

            initializeBackground();
            initializeComplication();
            initializeWatchFace();
            initializeNotificationCount();
        }

        private void initializeBackground() {
            mBackgroundColor = Color.BLACK;

            mBackgroundOverlayPaint = new Paint();
            int overlayColor = Color.argb(200, Color.red(mBackgroundColor), Color.green(mBackgroundColor), Color.blue(mBackgroundColor));
            mBackgroundOverlayPaint.setColor(overlayColor);
        }

        private void initializeComplication() {
            mActiveComplicationDataSparseArray = new SparseArray<>(COMPLICATION_IDS.length);
            setActiveComplications(COMPLICATION_IDS);

            mComplicationArcValuePaint = new Paint();
            mComplicationArcValuePaint.setColor(mSecondaryColor);
            mComplicationArcValuePaint.setStrokeWidth(4f);
            mComplicationArcValuePaint.setAntiAlias(true);
            mComplicationArcValuePaint.setStrokeCap(Paint.Cap.SQUARE);
            mComplicationArcValuePaint.setStyle(Paint.Style.STROKE);

            mComplicationArcPaint = new Paint();
            mComplicationArcPaint.setColor(mTertiaryColor);
            mComplicationArcPaint.setStrokeWidth(4f);
            mComplicationArcPaint.setAntiAlias(true);
            mComplicationArcPaint.setStrokeCap(Paint.Cap.SQUARE);
            mComplicationArcPaint.setStyle(Paint.Style.STROKE);


            mComplicationCirclePaint = new Paint();
            mComplicationCirclePaint.setColor(mQuaternaryColor);
            mComplicationCirclePaint.setStrokeWidth(3f);
            mComplicationCirclePaint.setAntiAlias(true);
            mComplicationCirclePaint.setStrokeCap(Paint.Cap.SQUARE);
            mComplicationCirclePaint.setStyle(Paint.Style.STROKE);

            mComplicationPrimaryTextPaint = new Paint();
            mComplicationPrimaryTextPaint.setColor(mSecondaryColor);
            mComplicationPrimaryTextPaint.setAntiAlias(true);
            mComplicationPrimaryTextPaint.setTypeface(mFontBold);

            mComplicationTextPaint = new Paint();
            mComplicationTextPaint.setColor(mTertiaryColor);
            mComplicationTextPaint.setAntiAlias(true);
            mComplicationTextPaint.setTypeface(mFontBold);
        }

        private void initializeWatchFace() {
            mHourPaint = new Paint();
            mHourPaint.setColor(mPrimaryColor);
            mHourPaint.setAntiAlias(true);
            mHourPaint.setTextAlign(Paint.Align.CENTER);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mPrimaryColor);
            mMinutePaint.setStrokeWidth(4f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.SQUARE);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mSecondaryColor);
            mSecondPaint.setStrokeWidth(6f);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.SQUARE);
            mSecondPaint.setStyle(Paint.Style.STROKE);

            mHourTickPaint = new Paint();
            mHourTickPaint.setColor(mSecondaryColor);
            mHourTickPaint.setStrokeWidth(4f);
            mHourTickPaint.setAntiAlias(true);
            mHourTickPaint.setStrokeCap(Paint.Cap.SQUARE);

            mTickPaint = new Paint();
            mTickPaint.setColor(mTertiaryColor);
            mTickPaint.setStrokeWidth(4f);
            mTickPaint.setAntiAlias(true);
            mTickPaint.setStrokeCap(Paint.Cap.SQUARE);
        }

        private void initializeNotificationCount() {
            mNotificationBackgroundPaint = new Paint();

            mNotificationCirclePaint = new Paint();
            mNotificationCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mNotificationCirclePaint.setColor(Color.WHITE);
            mNotificationCirclePaint.setAntiAlias(true);
            mNotificationCirclePaint.setStrokeWidth(2);

            mNotificationTextPaint = new Paint();
            mNotificationTextPaint.setColor(mBackgroundColor);
            mNotificationTextPaint.setTextAlign(Paint.Align.CENTER);
            mNotificationTextPaint.setAntiAlias(true);
            mNotificationTextPaint.setTypeface(mFontBold);
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        /*
         * Called when there is updated data for a complication id.
         */
        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
            // Adds/updates active complication data in the array.
            mActiveComplicationDataSparseArray.put(complicationId, complicationData);
            invalidate();
        }


        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;

            updateWatchHandStyle();

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            mIsRound = insets.isRound();
        }

        private void updateWatchHandStyle() {
            if (mAmbient) {
                mHourPaint.setColor(Color.WHITE);
                mMinutePaint.setColor(Color.WHITE);
                if (mLowBitAmbient || mBurnInProtection) {
                    mHourPaint.setTypeface(mFontLight);
                    mHourPaint.setAntiAlias(false);
                    mMinutePaint.setAntiAlias(false);
                    mMinutePaint.setStrokeWidth(2f);
                    mHourTickPaint.setAntiAlias(false);
                    mHourTickPaint.setStrokeWidth(2f);
                    mTickPaint.setAntiAlias(false);
                    mTickPaint.setStrokeWidth(2f);
                    mComplicationArcValuePaint.setAntiAlias(false);
                    mComplicationArcValuePaint.setStrokeWidth(2f);
                    mComplicationArcPaint.setAntiAlias(false);
                    mComplicationArcPaint.setStrokeWidth(2f);
                    mComplicationCirclePaint.setAntiAlias(false);
                    mComplicationCirclePaint.setStrokeWidth(2f);
                    mComplicationPrimaryTextPaint.setTypeface(mFont);
                    mComplicationPrimaryTextPaint.setAntiAlias(false);
                    mComplicationTextPaint.setAntiAlias(false);
                    mComplicationTextPaint.setTypeface(mFont);
                    mNotificationCirclePaint.setStyle(Paint.Style.STROKE);
                    mNotificationCirclePaint.setAntiAlias(false);
                    mNotificationTextPaint.setColor(Color.WHITE);
                    mNotificationTextPaint.setTypeface(mFont);
                    mNotificationTextPaint.setAntiAlias(false);
                }

            } else {
                mHourPaint.setColor(mPrimaryColor);
                mHourPaint.setAntiAlias(true);
                mHourPaint.setTypeface(mFont);
                mMinutePaint.setColor(mPrimaryColor);
                mMinutePaint.setAntiAlias(true);
                mMinutePaint.setStrokeWidth(4f);
                mHourTickPaint.setAntiAlias(true);
                mHourTickPaint.setStrokeWidth(4f);
                mTickPaint.setAntiAlias(true);
                mTickPaint.setStrokeWidth(4f);
                mComplicationArcValuePaint.setAntiAlias(true);
                mComplicationArcValuePaint.setStrokeWidth(4f);
                mComplicationArcPaint.setAntiAlias(true);
                mComplicationArcPaint.setStrokeWidth(4f);
                mComplicationCirclePaint.setAntiAlias(true);
                mComplicationCirclePaint.setStrokeWidth(3f);
                mComplicationPrimaryTextPaint.setTypeface(mFontBold);
                mComplicationPrimaryTextPaint.setAntiAlias(true);
                mComplicationTextPaint.setAntiAlias(true);
                mComplicationTextPaint.setTypeface(mFontBold);
                mNotificationCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mNotificationCirclePaint.setAntiAlias(true);
                mNotificationTextPaint.setColor(mBackgroundColor);
                mNotificationTextPaint.setTypeface(mFontBold);
                mNotificationTextPaint.setAntiAlias(true);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            /*
             * Find the coordinates of the center point on the screen, and ignore the window
             * insets, so that, on round watches with a "chin", the watch face is centered on the
             * entire screen, not just the usable portion.
             */

            mCenterX = width / 2;
            mCenterY = height / 2;

            mHourPaint.setTextSize(width / 6);
            mComplicationPrimaryTextPaint.setTextSize(width / 18);
            mComplicationTextPaint.setTextSize(width / 20);
            mNotificationTextPaint.setTextSize(width / 25);

            int gradientColor = Color.argb(200, Color.red(mBackgroundColor), Color.green(mBackgroundColor), Color.blue(mBackgroundColor));
            Shader shader = new LinearGradient(0, height - 100, 0, height, Color.TRANSPARENT, gradientColor, Shader.TileMode.CLAMP);
            mNotificationBackgroundPaint.setShader(shader);
        }

        /**
         * Captures tap event (and tap type). The {@link WatchFaceService#TAP_TYPE_TAP} case can be
         * used for implementing specific logic to handle the gesture.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    for (int i = 0; i < mComplicationTapBoxes.length; i++) {
                        if (mComplicationTapBoxes[i] != null && mComplicationTapBoxes[i].contains(x, y)) {
                            onComplicationTapped(i);
                        }
                    }
                    break;
            }
            invalidate();
        }

        private void onComplicationTapped(int id) {
            ComplicationData complicationData =
                    mActiveComplicationDataSparseArray.get(id);

            if (complicationData != null) {

                if (complicationData.getTapAction() != null) {
                    try {
                        complicationData.getTapAction().send();
                    } catch (PendingIntent.CanceledException e) {
                    }

                } else if (complicationData.getType() == ComplicationData.TYPE_NO_PERMISSION) {
                    ComponentName componentName = new ComponentName(
                            getApplicationContext(),
                            LineWatchFaceService.class);

                    Intent permissionRequestIntent =
                            ComplicationHelperActivity.createPermissionRequestHelperIntent(
                                    getApplicationContext(), componentName);

                    startActivity(permissionRequestIntent);
                }
            }
        }

        @Override
        public void onUnreadCountChanged(int count) {
            super.onUnreadCountChanged(count);
            mUnreadNotificationCount = count;
        }

        @Override
        public void onNotificationCountChanged(int count) {
            super.onNotificationCountChanged(count);
            mNotificationCount = count;
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            for (RectF tapBox : mComplicationTapBoxes) {
                tapBox = null;
            }

            drawBackground(canvas, now, BACKGROUND_COMPLICATION);
            drawComplication(canvas, now, TOP_DIAL_COMPLICATION, mCenterX, mCenterY / 2);
            drawComplication(canvas, now, LEFT_DIAL_COMPLICATION, mCenterX / 2, mCenterY);
            drawComplication(canvas, now, RIGHT_DIAL_COMPLICATION, mCenterX * 1.5f, mCenterY);
            drawWatchFace(canvas);
            drawNotificationCount(canvas);
        }

        private void drawBackground(Canvas canvas, long currentTimeMillis, int id) {
            ComplicationData complicationData = mActiveComplicationDataSparseArray.get(id);
            canvas.drawColor(mBackgroundColor);
            if ((complicationData != null) && (complicationData.isActive(currentTimeMillis))) {
                if (complicationData.getType() == ComplicationData.TYPE_LARGE_IMAGE) {
                    Icon largeImage = complicationData.getLargeImage();
                    if (largeImage != null && !(mAmbient && (mBurnInProtection || mLowBitAmbient))) {
                        Drawable drawable = largeImage.loadDrawable(getApplicationContext());
                        if (drawable != null) {
                            if (mAmbient) {
                                drawable = convertToGrayscale(drawable);
                            }
                            drawable.setBounds(0, 0, (int) mCenterX * 2, (int) mCenterY * 2);
                            drawable.draw(canvas);
                            canvas.drawRect(0, 0, mCenterX * 2, mCenterY * 2, mBackgroundOverlayPaint);
                        }
                    }
                }
            }
        }

        private void drawComplication(Canvas canvas, long currentTimeMillis, int id, float centerX, float centerY) {
            ComplicationData complicationData = mActiveComplicationDataSparseArray.get(id);

            if ((complicationData != null) && (complicationData.isActive(currentTimeMillis))) {
                switch (complicationData.getType()) {
                    case ComplicationData.TYPE_RANGED_VALUE:
                        drawRangeComplication(canvas,
                                complicationData,
                                id);
                        break;
                    case ComplicationData.TYPE_SMALL_IMAGE:
                        drawSmallImageComplication(canvas,
                                complicationData,
                                currentTimeMillis,
                                centerX,
                                centerY,
                                id);
                        break;
                    case ComplicationData.TYPE_SHORT_TEXT:
                        drawShortTextComplication(canvas,
                                complicationData,
                                currentTimeMillis,
                                centerX,
                                centerY,
                                id);
                        break;
                    case ComplicationData.TYPE_ICON:
                        drawIconComplication(canvas,
                                complicationData,
                                currentTimeMillis,
                                centerX,
                                centerY,
                                id);
                        break;
                }
            }
        }

        private void drawRangeComplication(Canvas canvas, ComplicationData data, int id) {
            float min = data.getMinValue();
            float max = data.getMaxValue();
            float val = data.getValue();

            float centerX = mCenterX + mCenterX / 4 + 10;
            float centerY = mCenterY + mCenterY / 4 + 10;
            float radius = mCenterX / 2;

            if (!mIsRound) {
                radius *= 1.2f;
            }
            radius -= 20;

            float startAngle = -90;

            RectF tapBox = new RectF(centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius);
            mComplicationTapBoxes[id] = tapBox;

            if (val < max) {
                canvas.drawArc(tapBox,
                        -90 + (val - min) / (max - min) * 270 + 3.8f,
                        270 - (val - min) / (max - min) * 270 - 3.8f, false, mComplicationArcPaint);
            }
            if (val > min) {
                canvas.drawArc(tapBox,
                        startAngle,
                        (val - min) / (max - min) * 270 - 3.8f, false, mComplicationArcValuePaint);
            }
            int complicationSteps = 10;
            for (int tickIndex = 1; tickIndex < complicationSteps; tickIndex++) {
                if (tickIndex != (val - min) / (max - min) * complicationSteps) {
                    float tickRot = (float) (tickIndex * Math.PI * 3 / 2 / complicationSteps - startAngle / 180 * Math.PI - Math.PI / 2);
                    float innerX = (float) Math.sin(tickRot) * (radius - 4 - (0.05f * mCenterX));
                    float innerY = (float) -Math.cos(tickRot) * (radius - 4 - (0.05f * mCenterX));
                    float outerX = (float) Math.sin(tickRot) * (radius - 4);
                    float outerY = (float) -Math.cos(tickRot) * (radius - 4);
                    canvas.drawLine(centerX + innerX, centerY + innerY,
                            centerX + outerX, centerY + outerY, mTickPaint);
                }
            }

            float valRot = (float) ((val - min) * Math.PI * 3 / 2 / (max - min) - startAngle / 180 * Math.PI - Math.PI / 2);
            float innerX = (float) Math.sin(valRot) * (radius - (0.15f * mCenterX));
            float innerY = (float) -Math.cos(valRot) * (radius - (0.15f * mCenterX));
            float outerX = (float) Math.sin(valRot) * radius;
            float outerY = (float) -Math.cos(valRot) * radius;
            canvas.drawLine(centerX + innerX, centerY + innerY,
                    centerX + outerX, centerY + outerY, mHourTickPaint);

            mComplicationTextPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(String.valueOf(Math.round(min)),
                    centerX + -6,
                    centerY - radius - mComplicationTextPaint.descent() - mComplicationTextPaint.ascent(),
                    mComplicationTextPaint);

            mComplicationTextPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(String.valueOf(Math.round(max)),
                    centerX - radius - 4,
                    centerY - 6,
                    mComplicationTextPaint);

            Icon icon = mAmbient && mBurnInProtection ? data.getBurnInProtectionIcon() : data.getIcon();
            if (icon != null) {
                icon.setTint(mSecondaryColor);
                Drawable drawable = icon.loadDrawable(getApplicationContext());
                if (drawable != null) {
                    int size = (int) Math.round(0.15 * mCenterX);
                    drawable.setBounds(Math.round(centerX - size / 2), Math.round(centerY - size / 2), Math.round(centerX + size / 2), Math.round(centerY + size / 2));
                    drawable.draw(canvas);
                }
            } else {
                mComplicationPrimaryTextPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(String.valueOf(Math.round(val)),
                        centerX,
                        centerY - (mComplicationPrimaryTextPaint.descent() + mComplicationPrimaryTextPaint.ascent()) / 2,
                        mComplicationPrimaryTextPaint);
            }
        }

        private void drawShortTextComplication(Canvas canvas, ComplicationData data,
                                               long currentTimeMillis, float centerX,
                                               float centerY, int id) {
            ComplicationText title = data.getShortTitle();
            ComplicationText shortText = data.getShortText();
            Icon icon = mBurnInProtection && mAmbient ? data.getBurnInProtectionIcon() : data.getIcon();

            float radius = mCenterX / 4;

            mComplicationTapBoxes[id] = new RectF(centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius);

            canvas.drawCircle(centerX, centerY, radius, mComplicationCirclePaint);

            mComplicationPrimaryTextPaint.setTextAlign(Paint.Align.CENTER);
            mComplicationTextPaint.setTextAlign(Paint.Align.CENTER);

            float shortTextY = centerY - (mComplicationTextPaint.descent() + mComplicationTextPaint.ascent() / 2);

            if (icon != null) {
                icon.setTint(mSecondaryColor);
                Drawable drawable = icon.loadDrawable(getApplicationContext());
                if (drawable != null) {
                    int size = (int) Math.round(0.15 * mCenterX);
                    drawable.setBounds(Math.round(centerX - size / 2), Math.round(centerY - size - 2), Math.round(centerX + size / 2), Math.round(centerY - 2));
                    drawable.draw(canvas);
                }
                shortTextY = centerY - mComplicationPrimaryTextPaint.descent() - mComplicationPrimaryTextPaint.ascent() + 4;
            } else if (title != null) {
                canvas.drawText(title.getText(getApplicationContext(), currentTimeMillis).toString().toUpperCase(),
                        centerX,
                        centerY - mComplicationTextPaint.descent() - mComplicationTextPaint.ascent() + 4,
                        mComplicationTextPaint);
                shortTextY = centerY - 4;
            }

            canvas.drawText(shortText.getText(getApplicationContext(), currentTimeMillis).toString(),
                    centerX,
                    shortTextY,
                    mComplicationPrimaryTextPaint);
        }

        private void drawIconComplication(Canvas canvas, ComplicationData data,
                                          long currentTimeMillis, float centerX,
                                          float centerY, int id) {
            float radius = mCenterX / 4;

            mComplicationTapBoxes[id] = new RectF(centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius);

            Icon icon = mAmbient && mBurnInProtection ? data.getBurnInProtectionIcon() : data.getSmallImage();
            if (icon != null) {
                Drawable drawable = icon.loadDrawable(getApplicationContext());
                if (drawable != null) {
                    int size = (int) Math.round(0.15 * mCenterX);
                    drawable.setBounds(Math.round(centerX - size), Math.round(centerY - size), Math.round(centerX + size), Math.round(centerY + size));
                    drawable.draw(canvas);
                    canvas.drawCircle(centerX, centerY, radius, mComplicationCirclePaint);
                }
            }
        }

        private void drawSmallImageComplication(Canvas canvas, ComplicationData data,
                                                long currentTimeMillis, float centerX,
                                                float centerY, int id) {
            float radius = mCenterX / 4;

            mComplicationTapBoxes[id] = new RectF(centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius);

            Icon smallImage = data.getSmallImage();
            if (smallImage != null && !(mAmbient && mBurnInProtection)) {
                Drawable drawable = smallImage.loadDrawable(getApplicationContext());
                if (drawable != null) {
                    if (mAmbient) {
                        drawable = convertToGrayscale(drawable);
                    }
                    int size = Math.round(radius - mComplicationCirclePaint.getStrokeWidth() / 2);
                    if (data.getImageStyle() == ComplicationData.IMAGE_STYLE_ICON) {
                        size = (int) Math.round(0.15 * mCenterX);
                    } else {
                        drawable = convertToCircle(drawable);
                    }
                    drawable.setBounds(Math.round(centerX - size), Math.round(centerY - size), Math.round(centerX + size), Math.round(centerY + size));
                    drawable.draw(canvas);
                    canvas.drawCircle(centerX, centerY, radius, mComplicationCirclePaint);
                }
            }
        }

        private Drawable convertToGrayscale(Drawable drawable) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);

            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);

            drawable.setColorFilter(filter);

            return drawable;
        }

        private Bitmap drawableToBitmap(Drawable drawable) {
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            }

            int width = drawable.getIntrinsicWidth();
            width = width > 0 ? width : 1;
            int height = drawable.getIntrinsicHeight();
            height = height > 0 ? height : 1;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            return bitmap;
        }

        private Drawable convertToCircle(Drawable drawable) {
            Bitmap bitmap = drawableToBitmap(drawable);
            Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                    bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            final int color = 0xff424242;
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(),
                    bitmap.getHeight());

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            canvas.drawCircle(bitmap.getWidth() / 2,
                    bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
            return new BitmapDrawable(output);
        }

        private void drawWatchFace(Canvas canvas) {
            mPrimaryColor = mPrefs.getInt("setting_color_value", Color.parseColor("#18FFFF"));
            if (!mAmbient) {
                mHourPaint.setColor(mPrimaryColor);
                mMinutePaint.setColor(mPrimaryColor);
            }

            if (mIsRound) {
                float outerRadius = mCenterX - 6;
                for (int tickIndex = 0; tickIndex < 60; tickIndex++) {
                    Paint tickPaint = mTickPaint;
                    float innerRadius = mCenterX - (0.10f * mCenterX);
                    if (tickIndex % 5 == 0) {
                        tickPaint = mHourTickPaint;
                        innerRadius -= (0.05f * mCenterX);
                    }
                    float tickRot = (float) (tickIndex * Math.PI * 2 / 60);
                    float innerX = (float) Math.sin(tickRot) * innerRadius;
                    float innerY = (float) -Math.cos(tickRot) * innerRadius;
                    float outerX = (float) Math.sin(tickRot) * outerRadius;
                    float outerY = (float) -Math.cos(tickRot) * outerRadius;
                    canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                            mCenterX + outerX, mCenterY + outerY, tickPaint);
                }

                outerRadius--;
                float innerRadius = mCenterX / 2;
                float minuteRot = (float) Math.PI / 30 * mCalendar.get(Calendar.MINUTE);
                float innerX = (float) Math.sin(minuteRot) * innerRadius;
                float innerY = (float) -Math.cos(minuteRot) * innerRadius;
                float outerX = (float) Math.sin(minuteRot) * outerRadius;
                float outerY = (float) -Math.cos(minuteRot) * outerRadius;
                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mMinutePaint);
            } else {
                for (int x = 0; x < 4; x++) {
                    canvas.save();
                    canvas.rotate(x * 90, mCenterX, mCenterY);
                    for (int tickIndex = 0; tickIndex < 15; tickIndex++) {
                        Paint tickPaint = mTickPaint;
                        float magic = (float) (-1 / Math.tan(-0.25 * Math.PI + tickIndex * Math.PI / 30 + Math.PI / 60));
                        float outerY = mCenterY - 6;
                        float outerX = outerY / magic;
                        float innerY = mCenterY - (0.10f * mCenterX);
                        if ((tickIndex + 3) % 5 == 0) {
                            tickPaint = mHourTickPaint;
                            innerY -= 0.05f * mCenterX;
                        }
                        float innerX = innerY / magic;
                        canvas.drawLine(innerX + mCenterX, mCenterY + innerY, outerX + mCenterX, mCenterY + outerY, tickPaint);
                    }
                    canvas.restore();
                }

                int min = mCalendar.get(Calendar.MINUTE) + 7;
                canvas.save();
                canvas.rotate((float) Math.floor(min / 15) * 90 - 180, mCenterX, mCenterY);
                float magic = (float) (-1 / Math.tan(-0.25 * Math.PI + (min % 15) * Math.PI / 30 + Math.PI / 60));
                float outerY = mCenterY - 7;
                float outerX = outerY / magic;
                float innerY = mCenterY / 2;
                float innerX = innerY / magic;
                canvas.drawLine(innerX + mCenterX, mCenterY + innerY, outerX + mCenterX, mCenterY + outerY, mMinutePaint);
                canvas.restore();
            }

            int milliseconds = mCalendar.get(Calendar.SECOND) * 1000 + mCalendar.get(Calendar.MILLISECOND);
            if (!mAmbient) {
                float percentage = milliseconds / 60000f;
                if (mIsRound) {
                    canvas.drawArc(1, 1, mCenterX * 2 - 1, mCenterY * 2 - 1, -89.7f, 360 * percentage, false, mSecondPaint);
                } else {
                    if (percentage > 0) {
                        canvas.drawLine(mCenterX + 1, 1, mCenterX + mCenterX * (percentage / 0.125f), 1, mSecondPaint);
                    }
                    if (percentage > 0.125) {
                        canvas.drawLine(mCenterX * 2 - 1, 1, mCenterX * 2 - 1, mCenterY * 2 * ((percentage - 0.125f) / 0.25f) - 1, mSecondPaint);
                    }
                    if (percentage > 0.375) {
                        canvas.drawLine(mCenterX * 2 - 1, mCenterY * 2 - 1, mCenterX * 2 - mCenterX * 2 * ((percentage - 0.375f) / 0.25f) + 1, mCenterY * 2 - 1, mSecondPaint);
                    }
                    if (percentage > 0.625) {
                        canvas.drawLine(1, mCenterY * 2 - 1, 1, mCenterY * 2 - mCenterY * 2 * ((percentage - 0.625f) / 0.25f) + 1, mSecondPaint);
                    }
                    if (percentage > 0.875) {
                        canvas.drawLine(1, 1, mCenterX * ((percentage - 0.875f) / 0.125f) - 1, 1, mSecondPaint);
                    }
                }
            }

            String hourString;
            if (DateFormat.is24HourFormat(LineWatchFaceService.this)) {
                hourString = String.valueOf(mCalendar.get(Calendar.HOUR_OF_DAY));
            } else {
                int hour = mCalendar.get(Calendar.HOUR);
                if (hour == 0) {
                    hour = 12;
                }
                hourString = String.valueOf(hour);
            }
            canvas.drawText(hourString, mCenterX, mCenterY - (mHourPaint.descent() + mHourPaint.ascent()) / 2, mHourPaint);
        }

        private void drawNotificationCount(Canvas canvas) {
            int count = 0;
            NotificationIndicator notificationCount = NotificationIndicator.fromValue(mPrefs.getInt("setting_notification_indicator", NotificationIndicator.NONE.getValue()));
            switch (notificationCount) {
                case UNREAD:
                    count = mUnreadNotificationCount;
                    break;
                case ALL:
                    count = mNotificationCount;
                    break;
            }
            if (count > 0) {
                canvas.drawRect(0, mCenterY * 2 - 100, mCenterX * 2, mCenterY * 2, mNotificationBackgroundPaint);
                canvas.drawCircle(mCenterX, mCenterY * 2 - 6 - mCenterX * 0.1f, mCenterX * 0.08f, mNotificationCirclePaint);
                canvas.drawText(String.valueOf(mNotificationCount), mCenterX, mCenterY * 2 - 6 - mCenterX * 0.1f - (mNotificationTextPaint.descent() + mNotificationTextPaint.ascent()) / 2, mNotificationTextPaint);
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            LineWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            LineWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        /**
         * Starts/stops the {@link #mUpdateTimeHandler} timer based on the state of the watch face.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer
         * should only run in active mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
