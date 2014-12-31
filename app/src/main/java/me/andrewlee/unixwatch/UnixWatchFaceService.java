package me.andrewlee.unixwatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.Time;
import android.view.SurfaceHolder;

import java.util.TimeZone;

/**
 * Created by andrewl on 12/31/14.
 */
public class UnixWatchFaceService extends CanvasWatchFaceService {

    @Override
    public Engine onCreateEngine() {
        return new UnixWatchFaceEngine();
    }

    private class UnixWatchFaceEngine extends CanvasWatchFaceService.Engine {

        private final int TICK_MESSAGE_ID = 0;

        private final Handler timerHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                switch (m.what) {
                    case TICK_MESSAGE_ID:
                        invalidate();
                        if (shouldTimerBeRunning()) {
                            long currentMs = System.currentTimeMillis();
                            long delayMs = 1000 - (currentMs % 1000);
                            timerHandler.sendEmptyMessageDelayed(TICK_MESSAGE_ID, delayMs);
                        }
                        break;
                }
            }
        };

        private final BroadcastReceiver timeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                currentTimezone = intent.getStringExtra("time-zone");
            }
        };
        private boolean isTimeZoneReceiverRegistered = false;

        private String currentTimezone = "";

        private WatchFacePainter painter;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(UnixWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            painter = new UnixWatchFacePainter();

            timerHandler.sendEmptyMessage(TICK_MESSAGE_ID);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long currentMilliseconds = System.currentTimeMillis();
            painter.paint(canvas, bounds, currentMilliseconds, currentTimezone, isInAmbientMode());
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            invalidate();

            updateTimerHandler();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            updateTimerHandler();

            if (visible) {
                registerTimeZoneReceiver();
                currentTimezone = TimeZone.getDefault().getID();
            } else {
                unregisterTimeZoneReceiver();
            }
        }

        private void updateTimerHandler() {
            timerHandler.removeMessages(TICK_MESSAGE_ID);
            if (shouldTimerBeRunning()) {
                timerHandler.sendEmptyMessage(TICK_MESSAGE_ID);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        private void registerTimeZoneReceiver() {
            if (isTimeZoneReceiverRegistered) {
                return;
            }
            IntentFilter intentFilter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            UnixWatchFaceService.this.registerReceiver(timeZoneReceiver, intentFilter);
            isTimeZoneReceiverRegistered = true;
        }

        private void unregisterTimeZoneReceiver() {
            if (!isTimeZoneReceiverRegistered) {
                return;
            }
            UnixWatchFaceService.this.unregisterReceiver(timeZoneReceiver);
            isTimeZoneReceiverRegistered = false;
        }
    }

}
