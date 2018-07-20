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
import android.graphics.Path;
import android.graphics.PathMeasure;
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
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Set;
import java.util.TimeZone;

public class WatchFaceService extends CanvasWatchFaceService {

    // Left and right dial supported types.
    public static final int[][] COMPLICATION_SUPPORTED_TYPES = {
            {ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_RANGED_VALUE, ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_LONG_TEXT, ComplicationData.TYPE_SHORT_TEXT, ComplicationData.TYPE_SMALL_IMAGE, ComplicationData.TYPE_ICON},
            {ComplicationData.TYPE_LARGE_IMAGE}
    };
    private static final int TOP_DIAL_COMPLICATION = 0;
    private static final int LEFT_DIAL_COMPLICATION = 1;
    private static final int RIGHT_DIAL_COMPLICATION = 2;
    private static final int BOTTOM_DIAL_COMPLICATION = 3;
    private static final int BACKGROUND_COMPLICATION = 4;
    public static final int[] COMPLICATION_IDS = {
            TOP_DIAL_COMPLICATION,
            LEFT_DIAL_COMPLICATION,
            RIGHT_DIAL_COMPLICATION,
            BOTTOM_DIAL_COMPLICATION,
            BACKGROUND_COMPLICATION
    };
    /*
     * Update rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = 32;

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
        private final WeakReference<WatchFaceService.Engine> mWeakReference;

        public EngineHandler(WatchFaceService.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            WatchFaceService.Engine engine = mWeakReference.get();
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
        private boolean mComplicationBorder;
        private boolean mStyleDigitalog;
        private boolean mStyleDigital;
        private boolean mStyleAnalog;
        private boolean mBackgroundEffectDarken;
        private boolean mBackgroundEffectBlur;
        private boolean mBackgroundEffectGrayscale;
        private boolean mAmbientColor;
        private boolean mNotificationIndicatorUnread;
        private boolean mNotificationIndicatorAll;
        private boolean mTimeFormat24;
        private boolean mTimeFormat12;
        private int mPrimaryColor;
        private int mBackgroundColor;
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
        private int mSecondaryColor;
        private int mTertiaryColor;
        private int mQuaternaryColor;
        private Paint mBackgroundOverlayPaint;
        private Paint mHourTextPaint;
        private Paint mMinuteTextPaint;
        private Paint mMinutePaint;
        private Paint mSecondTextPaint;
        private Paint mSecondPaint;
        private Paint mHourTickPaint;
        private Paint mTickPaint;
        private Paint mComplicationArcValuePaint;
        private Paint mComplicationArcPaint;
        private Paint mComplicationCirclePaint;
        private TextPaint mComplicationPrimaryLongTextPaint;
        private Paint mComplicationPrimaryTextPaint;
        private TextPaint mComplicationLongTextPaint;
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
        private int mUnreadNotificationCount;
        private int mNotificationCount;
        private RectF[] mComplicationTapBoxes = new RectF[COMPLICATION_IDS.length];

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                    .setStatusBarGravity(Gravity.TOP | Gravity.CENTER_HORIZONTAL)
                    .setAcceptsTapEvents(true)
                    .build());

            mCalendar = Calendar.getInstance();
            mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

            /* Set defaults for fonts */
            mFontLight = Typeface.create("sans-serif-light", Typeface.NORMAL);
            mFontBold = Typeface.create("sans-serif", Typeface.BOLD);
            mFont = Typeface.create("sans-serif", Typeface.NORMAL);

            initializeBackground();
            initializeComplication();
            initializeWatchFace();
            initializeNotificationCount();

            getSettingValues();
            updateStyle();
        }

        private void initializeBackground() {
            mBackgroundOverlayPaint = new Paint();
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

            mComplicationPrimaryLongTextPaint = new TextPaint();
            mComplicationPrimaryLongTextPaint.setColor(mSecondaryColor);
            mComplicationPrimaryLongTextPaint.setAntiAlias(true);
            mComplicationPrimaryLongTextPaint.setTypeface(mFontBold);

            mComplicationPrimaryTextPaint = new Paint();
            mComplicationPrimaryTextPaint.setColor(mSecondaryColor);
            mComplicationPrimaryTextPaint.setAntiAlias(true);
            mComplicationPrimaryTextPaint.setTypeface(mFontBold);

            mComplicationTextPaint = new Paint();
            mComplicationTextPaint.setColor(mTertiaryColor);
            mComplicationTextPaint.setAntiAlias(true);
            mComplicationTextPaint.setTypeface(mFontBold);

            mComplicationLongTextPaint = new TextPaint();
            mComplicationLongTextPaint.setColor(mTertiaryColor);
            mComplicationLongTextPaint.setAntiAlias(true);
            mComplicationLongTextPaint.setTypeface(mFontBold);
        }

        private void initializeWatchFace() {
            mHourTextPaint = new Paint();
            mHourTextPaint.setColor(mPrimaryColor);
            mHourTextPaint.setAntiAlias(true);

            mMinuteTextPaint = new Paint();
            mMinuteTextPaint.setColor(mSecondaryColor);
            mMinuteTextPaint.setAntiAlias(true);
            mMinuteTextPaint.setTypeface(mFontBold);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mPrimaryColor);
            mMinutePaint.setStrokeWidth(4f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.SQUARE);

            mSecondTextPaint = new Paint();
            mSecondTextPaint.setColor(mTertiaryColor);
            mSecondTextPaint.setAntiAlias(true);
            mSecondTextPaint.setTextAlign(Paint.Align.LEFT);
            mSecondTextPaint.setTypeface(mFontBold);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mSecondaryColor);
            mSecondPaint.setStrokeWidth(6f);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.BUTT);
            mSecondPaint.setStyle(Paint.Style.STROKE);

            mHourTickPaint = new Paint();
            mHourTickPaint.setColor(mSecondaryColor);
            mHourTickPaint.setStrokeWidth(4f);
            mHourTickPaint.setAntiAlias(true);
            mHourTickPaint.setStrokeCap(Paint.Cap.SQUARE);

            mTickPaint = new Paint();
            mTickPaint.setColor(mTertiaryColor);            mMinuteTextPaint.setColor(mSecondaryColor);
            mMinuteTextPaint.setAntiAlias(true);
            mMinuteTextPaint.setTypeface(mFontBold);

            mMinutePaint = new Paint();
            mMinutePaint.setColor(mPrimaryColor);
            mMinutePaint.setStrokeWidth(4f);
            mMinutePaint.setAntiAlias(true);
            mMinutePaint.setStrokeCap(Paint.Cap.SQUARE);

            mSecondTextPaint = new Paint();
            mSecondTextPaint.setColor(mTertiaryColor);
            mSecondTextPaint.setAntiAlias(true);
            mSecondTextPaint.setTextAlign(Paint.Align.LEFT);
            mSecondTextPaint.setTypeface(mFontBold);

            mSecondPaint = new Paint();
            mSecondPaint.setColor(mSecondaryColor);
            mSecondPaint.setStrokeWidth(6f);
            mSecondPaint.setAntiAlias(true);
            mSecondPaint.setStrokeCap(Paint.Cap.BUTT);
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

            updateStyle();

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);
            mIsRound = insets.isRound();
        }

        private void updateStyle() {
            int overlayColor = Color.argb(128, Color.red(mBackgroundColor), Color.green(mBackgroundColor), Color.blue(mBackgroundColor));
            mBackgroundOverlayPaint.setColor(overlayColor);
            mMinuteTextPaint.setColor(mSecondaryColor);
            mMinutePaint.setColor(mPrimaryColor);
            mSecondTextPaint.setColor(mTertiaryColor);
            mSecondPaint.setColor(mSecondaryColor);
            mHourTickPaint.setColor(mSecondaryColor);
            mTickPaint.setColor(mTertiaryColor);
            mComplicationArcValuePaint.setColor(mSecondaryColor);
            mComplicationArcPaint.setColor(mTertiaryColor);
            mComplicationCirclePaint.setColor(mQuaternaryColor);
            mComplicationPrimaryLongTextPaint.setColor(mSecondaryColor);
            mComplicationPrimaryTextPaint.setColor(mSecondaryColor);
            mComplicationTextPaint.setColor(mTertiaryColor);
            mComplicationLongTextPaint.setColor(mTertiaryColor);

            if (mAmbient) {
                mHourTextPaint.setColor(Color.WHITE);
                mMinutePaint.setColor(Color.WHITE);
                if (mLowBitAmbient) {
                    mHourTickPaint.setColor(Color.WHITE);
                    mComplicationArcValuePaint.setColor(Color.WHITE);
                    mComplicationPrimaryLongTextPaint.setColor(Color.WHITE);
                    mComplicationPrimaryTextPaint.setColor(Color.WHITE);
                }
                if (mBurnInProtection) {
                    mHourTextPaint.setTypeface(mFontLight);
                    mNotificationCirclePaint.setStyle(Paint.Style.STROKE);
                    mNotificationTextPaint.setColor(Color.WHITE);
                }

            } else {
                mHourTextPaint.setColor(mPrimaryColor);
                mHourTextPaint.setTypeface(mFont);
                mMinutePaint.setColor(mPrimaryColor);
                mHourTickPaint.setColor(mSecondaryColor);
                mComplicationArcValuePaint.setColor(mSecondaryColor);
                mComplicationPrimaryLongTextPaint.setColor(mSecondaryColor);
                mComplicationPrimaryTextPaint.setColor(mSecondaryColor);
                mNotificationCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mNotificationTextPaint.setColor(mBackgroundColor);
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

            mHourTextPaint.setTextSize(width / 6);
            mMinuteTextPaint.setTextSize(width / 15);
            mSecondTextPaint.setTextSize(width / 15);
            mComplicationPrimaryLongTextPaint.setTextSize(width / 23);
            mComplicationPrimaryTextPaint.setTextSize(width / 18);
            mComplicationLongTextPaint.setTextSize(width / 25);
            mComplicationTextPaint.setTextSize(width / 20);
            mNotificationTextPaint.setTextSize(width / 25);

            int gradientColor = Color.argb(128, Color.red(mBackgroundColor), Color.green(mBackgroundColor), Color.blue(mBackgroundColor));
            Shader shader = new LinearGradient(0, height - height / 4, 0, height, Color.TRANSPARENT, gradientColor, Shader.TileMode.CLAMP);
            mNotificationBackgroundPaint.setShader(shader);
        }

        /**
         * Captures tap event (and tap type). The {@link android.support.wearable.watchface.WatchFaceService#TAP_TYPE_TAP} case can be
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
                            WatchFaceService.class);

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

        private void getSettingValues() {
            mPrimaryColor = mPrefs.getInt("settings_color_value", Color.parseColor("#18FFFF"));
            int secondaryColor = mPrefs.getInt("settings_accent_color_value", Color.parseColor("#FFFFFF"));
            float secondaryOpacity = Integer.parseInt(mPrefs.getString("settings_accent_color_opacity", "60")) / 100f;
            mSecondaryColor = Color.argb(Math.round(secondaryOpacity * 255), Color.red(secondaryColor), Color.green(secondaryColor), Color.blue(secondaryColor));
            mTertiaryColor = Color.argb(Math.round(secondaryOpacity * 152), Color.red(secondaryColor), Color.green(secondaryColor), Color.blue(secondaryColor));
            mQuaternaryColor = Color.argb(Math.round(secondaryOpacity * 48), Color.red(secondaryColor), Color.green(secondaryColor), Color.blue(secondaryColor));
            mComplicationBorder = mPrefs.getBoolean("settings_complication_border", true);
            String mStyle = mPrefs.getString("settings_style", "0");
            mStyleDigitalog = mStyle.equals("0");
            mStyleDigital = mStyle.equals("1");
            mStyleAnalog = mStyle.equals("2");
            mBackgroundColor = mPrefs.getInt("settings_background_color_value", Color.BLACK);
            Set<String> backgroundEffects = mPrefs.getStringSet("settings_background_effects", null);
            mBackgroundEffectDarken = backgroundEffects != null && backgroundEffects.contains("0");
            mBackgroundEffectBlur = backgroundEffects != null && backgroundEffects.contains("1");
            mBackgroundEffectGrayscale = backgroundEffects != null && backgroundEffects.contains("2");
            mAmbientColor = mPrefs.getBoolean("settings_ambient", false);
            String mNotificationIndicator = mPrefs.getString("settings_notification_indicator", null);
            mNotificationIndicatorUnread = mNotificationIndicator != null && mNotificationIndicator.equals("1");
            mNotificationIndicatorAll = mNotificationIndicator != null && mNotificationIndicator.equals("2");
            String mTimeFormat = mPrefs.getString("settings_time_format", null);
            mTimeFormat24 = mTimeFormat != null && mTimeFormat.equals("1");
            mTimeFormat12 = mTimeFormat != null && mTimeFormat.equals("2");
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            drawBackground(canvas, now, BACKGROUND_COMPLICATION);

            float offset = mStyleDigital ? mCenterX * 0.13f : 0;

            drawComplication(canvas, now, TOP_DIAL_COMPLICATION, mCenterX, mCenterY / 2 - offset);
            drawComplication(canvas, now, LEFT_DIAL_COMPLICATION, mCenterX / 2 - offset, mCenterY);
            drawComplication(canvas, now, BOTTOM_DIAL_COMPLICATION, mCenterX, mCenterY * 1.5f + offset);
            drawComplication(canvas, now, RIGHT_DIAL_COMPLICATION, mCenterX * 1.5f + offset, mCenterY);

            if (mStyleDigitalog || mStyleAnalog) {
                drawTicks(canvas);
            }
            if (!mAmbient && !mStyleDigital) {
                drawSeconds(canvas);
            }
            if (mStyleDigitalog) {
                drawDigitalogTime(canvas);
            } else if (mStyleDigital) {
                drawDigitalTime(canvas);
            }

            drawNotificationCount(canvas);
        }

        private void drawBackground(Canvas canvas, long currentTimeMillis, int id) {
            ComplicationData complicationData = mActiveComplicationDataSparseArray.get(id);
            canvas.drawColor(mAmbient && (mBurnInProtection || !mAmbientColor) ? Color.BLACK : mBackgroundColor);
            if ((complicationData != null) && (complicationData.isActive(currentTimeMillis))) {
                if (complicationData.getType() == ComplicationData.TYPE_LARGE_IMAGE) {
                    Icon largeImage = complicationData.getLargeImage();
                    if (largeImage != null && !(mAmbient && (mBurnInProtection || mLowBitAmbient))) {
                        Drawable drawable = largeImage.loadDrawable(getApplicationContext());
                        if (drawable != null) {
                            if (mBackgroundEffectBlur) {
                                drawable = convertToBlur(drawable, 10);
                            }
                            if (mBackgroundEffectGrayscale || (mAmbient && !mAmbientColor)) {
                                drawable = convertToGrayscale(drawable);
                            }
                            drawable.setBounds(0, 0, (int) mCenterX * 2, (int) mCenterY * 2);
                            drawable.draw(canvas);
                            if (mBackgroundEffectDarken) {
                                canvas.drawRect(0, 0, mCenterX * 2, mCenterY * 2, mBackgroundOverlayPaint);
                            }
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
                                currentTimeMillis,
                                id);
                        break;
                    case ComplicationData.TYPE_SMALL_IMAGE:
                        drawSmallImageComplication(canvas,
                                complicationData,
                                centerX,
                                centerY,
                                id);
                        break;
                    case ComplicationData.TYPE_LONG_TEXT:
                        drawLongTextComplication(canvas,
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
                                centerX,
                                centerY,
                                id);
                        break;
                }
            }
        }

        private String complicationNumberString(float val) {
            if (val > 100000) {
                return String.valueOf(Math.round(val / 100000) + "m");
            } else if (val > 1000) {
                return String.valueOf(Math.round(val / 1000) + "k");
            } else {
                return String.valueOf(Math.round(val));
            }
        }

        private void drawRangeComplication(Canvas canvas, ComplicationData data, long currentTimeMillis, int id) {
            float min = data.getMinValue();
            float max = data.getMaxValue();
            float dataVal = data.getValue();
            float arcVal = (dataVal > max) ? max : dataVal;

            ComplicationData bottomComplicationData = mActiveComplicationDataSparseArray.get(BOTTOM_DIAL_COMPLICATION);


            float centerX;
            float centerY;
            float radius;
            float startAngle = -90;

            /*
            If bottom complication data exists then only the right space is available
            instead of the bottom right space.
            */
            float offset = mStyleDigital ? mCenterX * 0.1f : 0;

            if (bottomComplicationData != null &&
                    bottomComplicationData.getType() != ComplicationData.TYPE_EMPTY &&
                    bottomComplicationData.getType() != ComplicationData.TYPE_NO_DATA &&
                    bottomComplicationData.isActive(currentTimeMillis)) {
                centerX = mCenterX * 1.5f + offset;
                centerY = mCenterY;
                radius = mCenterX / 4;
            } else {
                centerX = mCenterX + mCenterX / 4 + 10 + offset * 1.3f;
                centerY = mCenterY + mCenterY / 4 + 10 + offset * 0.3f;
                radius = mCenterX / 2;
                if (!mIsRound) {
                    radius *= 1.2f;
                }
                radius -= 20;
            }

            mComplicationTapBoxes[id] = new RectF(centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius);

            Bitmap arcBitmap = Bitmap.createBitmap((int) radius * 2 + 4, (int) radius * 2 + 4, Bitmap.Config.ARGB_8888);
            Canvas arcCanvas = new Canvas(arcBitmap);
            Path path = new Path();
            path.addArc(2, 2, radius * 2 + 2, radius * 2 + 2,
                    -90 + (arcVal - min) / (max - min) * 270,
                    270 - (arcVal - min) / (max - min) * 270);

            int complicationSteps = 10;
            for (int tickIndex = 1; tickIndex < complicationSteps; tickIndex++) {
                float tickRot = (float) (tickIndex * Math.PI * 3 / 2 / complicationSteps - startAngle / 180 * Math.PI - Math.PI / 2);
                float innerX = (float) Math.sin(tickRot) * (radius - 4 - (0.05f * mCenterX));
                float innerY = (float) -Math.cos(tickRot) * (radius - 4 - (0.05f * mCenterX));
                float outerX = (float) Math.sin(tickRot) * (radius - 4);
                float outerY = (float) -Math.cos(tickRot) * (radius - 4);
                path.moveTo(radius + innerX + 2, radius + innerY + 2);
                path.lineTo(radius + outerX + 2, radius + outerY + 2);
            }
            arcCanvas.drawPath(path, mComplicationArcPaint);

            float valRot = (float) ((arcVal - min) * Math.PI * 3 / 2 / (max - min) - startAngle / 180 * Math.PI - Math.PI / 2);
            Path valuePath = new Path();
            valuePath.addArc(2, 2, radius * 2 + 2, radius * 2 + 2,
                    -90, (arcVal - min) / (max - min) * 270 + 0.0001f);
            valuePath.lineTo((float) Math.sin(valRot) * (radius - (0.15f * mCenterX)) + radius + 2, (float) -Math.cos(valRot) * (radius - (0.15f * mCenterX)) + radius + 2);
            mComplicationArcValuePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
            arcCanvas.drawPath(valuePath, mComplicationArcValuePaint);
            mComplicationArcValuePaint.setXfermode(null);
            arcCanvas.drawPath(valuePath, mComplicationArcValuePaint);

            canvas.drawBitmap(arcBitmap, centerX - radius - 2, centerY - radius - 2, null);

            mComplicationTextPaint.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText(complicationNumberString(min),
                    centerX + -6,
                    centerY - radius - mComplicationTextPaint.descent() - mComplicationTextPaint.ascent(),
                    mComplicationTextPaint);

            mComplicationTextPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(complicationNumberString(max),
                    centerX - radius - 4,
                    centerY - 6,
                    mComplicationTextPaint);

            Icon icon = mAmbient && mBurnInProtection ? data.getBurnInProtectionIcon() : data.getIcon();
            if (icon != null) {
                Drawable drawable = icon.loadDrawable(getApplicationContext());
                if (drawable != null) {
                    int size = (int) Math.round(0.15 * mCenterX);
                    drawable.setTint(mComplicationArcValuePaint.getColor());
                    drawable.setBounds(Math.round(centerX - size / 2), Math.round(centerY - size / 2), Math.round(centerX + size / 2), Math.round(centerY + size / 2));
                    drawable.draw(canvas);
                }
            } else {
                mComplicationPrimaryTextPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(complicationNumberString(dataVal),
                        centerX,
                        centerY - (mComplicationPrimaryTextPaint.descent() + mComplicationPrimaryTextPaint.ascent()) / 2,
                        mComplicationPrimaryTextPaint);
            }
        }

        private void drawLongTextComplication(Canvas canvas, ComplicationData data,
                                              long currentTimeMillis, float centerX,
                                              float centerY, int id) {
            ComplicationText text = data.getLongText();
            String textText = text != null ? text.getText(getApplicationContext(), currentTimeMillis).toString() : null;
            ComplicationText title = data.getLongTitle();
            String titleText = title != null ? title.getText(getApplicationContext(), currentTimeMillis).toString() : null;
            Icon icon = mBurnInProtection && mAmbient && data.getBurnInProtectionIcon() != null ? data.getBurnInProtectionIcon() : data.getIcon();
            Icon image = data.getSmallImage();

            float height = mCenterY / 4;
            float width = mCenterX * 1.2f;
            if (!mIsRound) {
                width = mCenterX * 1.5f;
                centerY += mCenterY / 16;
            }
            float maxWidth = width;

            Rect bounds = new Rect();
            Rect bounds2 = new Rect();
            float textWidth = 0;
            float titleWidth = 0;
            if (textText != null) {
                mComplicationPrimaryLongTextPaint.getTextBounds(textText, 0, textText.length(), bounds);
                textWidth = bounds.width() + height / 2;
            }
            if (titleText != null) {
                mComplicationLongTextPaint.getTextBounds(titleText, 0, titleText.length(), bounds2);
                titleWidth = bounds2.width() + height / 2;
            }
            if (textWidth > titleWidth && textWidth > 0) {
                width = textWidth;
            }
            if (textWidth < titleWidth && titleWidth > 0) {
                width = titleWidth;
            }
            if (image != null && !(mAmbient && mBurnInProtection)) {
                width += height + 8;
            } else if (icon != null) {
                width += height;
            }
            boolean ellipsize = false;
            if (width > maxWidth) {
                width = maxWidth;
                ellipsize = true;
            }

            RectF tapbox = new RectF(centerX - width / 2,
                    centerY - height / 2,
                    centerX + width / 2,
                    centerY + height / 2);

            mComplicationTapBoxes[id] = tapbox;

            if (mComplicationBorder) {
                Path path = new Path();
                path.moveTo(tapbox.left + height / 2, tapbox.top);
                path.lineTo(tapbox.right - height / 2, tapbox.top);
                path.arcTo(tapbox.right - height, tapbox.top, tapbox.right, tapbox.bottom, -90, 180, false);
                path.lineTo(tapbox.left + height / 2, tapbox.bottom);
                path.arcTo(tapbox.left, tapbox.top, tapbox.left + height, tapbox.bottom, 90, 180, false);
                canvas.drawPath(path, mComplicationCirclePaint);
            }

            float textY = centerY - (mComplicationPrimaryLongTextPaint.descent() + mComplicationPrimaryLongTextPaint.ascent() / 2);
            float textX = tapbox.left + height / 4;
            float textW = width - height / 4;

            if (image != null && !(mAmbient && mBurnInProtection)) {
                Drawable drawable = image.loadDrawable(getApplicationContext());
                if (drawable != null) {
                    if (mAmbient && !mAmbientColor) {
                        drawable = convertToGrayscale(drawable);
                    }
                    drawable = convertToCircle(drawable);
                    drawable.setBounds(Math.round(tapbox.left + 2),
                            Math.round(tapbox.top + 2),
                            Math.round(tapbox.left + height - 2),
                            Math.round(tapbox.bottom - 2));
                    drawable.draw(canvas);

                    textX = tapbox.left + height + 8;
                    textW = width - (textX - tapbox.left) - height / 4;
                }
            } else if (icon != null) {
                Drawable drawable = icon.loadDrawable(getApplicationContext());
                if (drawable != null) {
                    drawable.setTint(mComplicationPrimaryLongTextPaint.getColor());
                    int size = (int) Math.round(0.15 * mCenterX);
                    drawable.setBounds(Math.round(tapbox.left + height / 2 - size / 2),
                            Math.round(tapbox.top + height / 2 - size / 2),
                            Math.round(tapbox.left + height / 2 + size / 2),
                            Math.round(tapbox.top + height / 2 + size / 2));
                    drawable.draw(canvas);

                    textX = tapbox.left + height;
                    textW = width - (textX - tapbox.left) - height / 4;
                }
            }

            if (title != null) {
                canvas.drawText(
                        ellipsize ? TextUtils.ellipsize(
                                titleText,
                                mComplicationLongTextPaint,
                                textW,
                                TextUtils.TruncateAt.END
                        ).toString() : titleText,
                        textX,
                        centerY - mComplicationLongTextPaint.descent() - mComplicationLongTextPaint.ascent() + 4,
                        mComplicationLongTextPaint);
                textY = centerY - 4;
            }

            canvas.drawText(
                    ellipsize ? TextUtils.ellipsize(
                            textText,
                            mComplicationPrimaryLongTextPaint,
                            textW,
                            TextUtils.TruncateAt.END
                    ).toString() : textText,
                    textX,
                    textY,
                    mComplicationPrimaryLongTextPaint);
        }

        private void drawShortTextComplication(Canvas canvas, ComplicationData data,
                                               long currentTimeMillis, float centerX,
                                               float centerY, int id) {
            ComplicationText title = data.getShortTitle();
            ComplicationText text = data.getShortText();
            Icon icon = mBurnInProtection && mAmbient && data.getBurnInProtectionIcon() != null ? data.getBurnInProtectionIcon() : data.getIcon();

            float radius = mCenterX / 4;

            mComplicationTapBoxes[id] = new RectF(centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius);

            if (mComplicationBorder) {
                canvas.drawCircle(centerX, centerY, radius, mComplicationCirclePaint);
            }

            mComplicationPrimaryTextPaint.setTextAlign(Paint.Align.CENTER);
            mComplicationTextPaint.setTextAlign(Paint.Align.CENTER);

            float textY = centerY - (mComplicationPrimaryTextPaint.descent() + mComplicationPrimaryTextPaint.ascent() / 2);

            if (icon != null) {
                Drawable drawable = icon.loadDrawable(getApplicationContext());
                if (drawable != null) {
                    drawable.setTint(mComplicationPrimaryTextPaint.getColor());
                    int size = (int) Math.round(0.15 * mCenterX);
                    drawable.setBounds(Math.round(centerX - size / 2), Math.round(centerY - size - 2), Math.round(centerX + size / 2), Math.round(centerY - 2));
                    drawable.draw(canvas);

                    textY = centerY - mComplicationPrimaryTextPaint.descent() - mComplicationPrimaryTextPaint.ascent() + 4;
                }
            } else if (title != null) {
                canvas.drawText(title.getText(getApplicationContext(), currentTimeMillis).toString().toUpperCase(),
                        centerX,
                        centerY - mComplicationTextPaint.descent() - mComplicationTextPaint.ascent() + 4,
                        mComplicationTextPaint);
                textY = centerY - 4;
            }

            canvas.drawText(text.getText(getApplicationContext(), currentTimeMillis).toString(),
                    centerX,
                    textY,
                    mComplicationPrimaryTextPaint);
        }

        private void drawIconComplication(Canvas canvas, ComplicationData data,
                                          float centerX, float centerY, int id) {
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
                    drawable.setTint(mComplicationPrimaryTextPaint.getColor());
                    drawable.setBounds(Math.round(centerX - size), Math.round(centerY - size), Math.round(centerX + size), Math.round(centerY + size));
                    drawable.draw(canvas);
                    if (mComplicationBorder) {
                        canvas.drawCircle(centerX, centerY, radius, mComplicationCirclePaint);
                    }
                }
            }
        }

        private void drawSmallImageComplication(Canvas canvas, ComplicationData data,
                                                float centerX, float centerY, int id) {
            float radius = mCenterX / 4;

            mComplicationTapBoxes[id] = new RectF(centerX - radius,
                    centerY - radius,
                    centerX + radius,
                    centerY + radius);

            Icon smallImage = data.getSmallImage();
            if (smallImage != null && !(mAmbient && mBurnInProtection)) {
                Drawable drawable = smallImage.loadDrawable(getApplicationContext());
                if (drawable != null) {
                    if (mAmbient && !mAmbientColor) {
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
                    if (mComplicationBorder) {
                        canvas.drawCircle(centerX, centerY, radius, mComplicationCirclePaint);
                    }
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

        private Drawable convertToBlur(Drawable drawable, float radius) {
            Bitmap bitmap = drawableToBitmap(drawable);
            int width = Math.round(bitmap.getWidth() * 0.5f);
            int height = Math.round(bitmap.getHeight() * 0.5f);

            Bitmap input = Bitmap.createScaledBitmap(bitmap, width, height, false);
            Bitmap output = Bitmap.createBitmap(input);

            RenderScript rs = RenderScript.create(getApplicationContext());
            ScriptIntrinsicBlur theIntrinsic = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            Allocation tmpIn = Allocation.createFromBitmap(rs, input);
            Allocation tmpOut = Allocation.createFromBitmap(rs, output);
            theIntrinsic.setRadius(radius);
            theIntrinsic.setInput(tmpIn);
            theIntrinsic.forEach(tmpOut);
            tmpOut.copyTo(output);

            return new BitmapDrawable(output);
        }

        private void drawTicks(Canvas canvas) {
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
        }

        private void drawSeconds(Canvas canvas) {
            int milliseconds = mCalendar.get(Calendar.SECOND) * 1000 + mCalendar.get(Calendar.MILLISECOND);
            float percentage = milliseconds / 60000f;
            Path path = new Path();
            if (mIsRound) {
                path.moveTo(mCenterX - 2, 1);
                path.lineTo(mCenterX + 2, 1);
                path.arcTo(1, 1, mCenterX * 2 - 1, mCenterY * 2 - 1, -90, 359.99f, false);
            } else {
                path.moveTo(mCenterX - 2, 1);
                path.lineTo(mCenterX * 2 - 1, 1);
                path.lineTo(mCenterX * 2 - 1, mCenterY * 2 - 1);
                path.lineTo(1, mCenterY * 2 - 1);
                path.lineTo(1, 1);
                path.lineTo(mCenterX, 1);
            }
            PathMeasure measure = new PathMeasure(path, false);
            float length = measure.getLength();
            Path partialPath = new Path();
            measure.getSegment(0, length * percentage, partialPath, true);
            canvas.drawPath(partialPath, mSecondPaint);
        }

        private void drawMinutes(Canvas canvas) {
            if (mIsRound) {
                float outerRadius = mCenterX - 7;
                float innerRadius = mCenterX / 2;
                float minuteRot = (float) Math.PI / 30 * mCalendar.get(Calendar.MINUTE);
                float innerX = (float) Math.sin(minuteRot) * innerRadius;
                float innerY = (float) -Math.cos(minuteRot) * innerRadius;
                float outerX = (float) Math.sin(minuteRot) * outerRadius;
                float outerY = (float) -Math.cos(minuteRot) * outerRadius;
                canvas.drawLine(mCenterX + innerX, mCenterY + innerY,
                        mCenterX + outerX, mCenterY + outerY, mMinutePaint);
            } else {
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
        }

        private void drawDigitalogTime(Canvas canvas) {
            String hourString;
            if ((DateFormat.is24HourFormat(WatchFaceService.this) && !mTimeFormat24 && !mTimeFormat12) || mTimeFormat24) {
                hourString = String.valueOf(mCalendar.get(Calendar.HOUR_OF_DAY));
            } else {
                int hour = mCalendar.get(Calendar.HOUR);
                if (hour == 0) {
                    hour = 12;
                }
                hourString = String.valueOf(hour);
            }
            mHourTextPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(hourString, mCenterX, mCenterY - (mHourTextPaint.descent() + mHourTextPaint.ascent()) / 2, mHourTextPaint);

            drawMinutes(canvas);
        }

        private void drawDigitalTime(Canvas canvas) {
            String hourString;
            if ((DateFormat.is24HourFormat(WatchFaceService.this) && !mTimeFormat24 && !mTimeFormat12) || mTimeFormat24) {
                hourString = String.valueOf(mCalendar.get(Calendar.HOUR_OF_DAY));
            } else {
                int hour = mCalendar.get(Calendar.HOUR);
                if (hour == 0) {
                    hour = 12;
                }
                hourString = String.valueOf(hour);
            }
            String minuteString = String.valueOf(mCalendar.get(Calendar.MINUTE));
            if (minuteString.length() == 1) {
                minuteString = "0" + minuteString;
            }
            String secondString = String.valueOf(mCalendar.get(Calendar.SECOND));
            if (secondString.length() == 1) {
                secondString = "0" + secondString;
            }

            mHourTextPaint.setTextAlign(Paint.Align.RIGHT);
            mMinuteTextPaint.setTextAlign(Paint.Align.LEFT);
            mSecondTextPaint.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(hourString,
                    mCenterX + mCenterX / 12,
                    mCenterY - (mHourTextPaint.descent() + mHourTextPaint.ascent()) / 2,
                    mHourTextPaint);
            canvas.drawText(minuteString,
                    mCenterX + mCenterX / 9,
                    mCenterY - (mMinuteTextPaint.descent() + mMinuteTextPaint.ascent()) +
                            (mHourTextPaint.descent() + mHourTextPaint.ascent()) / 2,
                    mMinuteTextPaint);
            if (!mAmbient) {
                canvas.drawText(secondString,
                        mCenterX + mCenterX / 9,
                        mCenterY - (mHourTextPaint.descent() + mHourTextPaint.ascent()) / 2,
                        mSecondTextPaint);
            }
        }

        private void drawNotificationCount(Canvas canvas) {
            int count = 0;
            if (mNotificationIndicatorUnread) {
                count = mUnreadNotificationCount;
            } else if (mNotificationIndicatorAll) {
                count = mNotificationCount;
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
                getSettingValues();
                updateStyle();
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
            WatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            WatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
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
