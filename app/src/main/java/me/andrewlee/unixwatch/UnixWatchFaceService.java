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

        private final Typeface CLOCK_TYPEFACE = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
        private final String[] WEEKDAY_ABBR = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

        private Paint backgroundPaint;
        private Paint interactiveForegroundPaint;
        private Paint interactiveForegroundSubPaint;
        private Paint ambientForegroundPaint;
        private Paint ambientForegroundSubPaint;

        private Time mTime;

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
                mTime.clear(intent.getStringExtra("time-zone"));
            }
        };
        private boolean isTimeZoneReceiverRegistered = false;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(UnixWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            backgroundPaint = new Paint();
            backgroundPaint.setColor(Color.BLACK);
            backgroundPaint.setStyle(Paint.Style.FILL);

            interactiveForegroundPaint = new Paint();
            interactiveForegroundPaint.setColor(Color.GREEN);
            interactiveForegroundPaint.setStyle(Paint.Style.FILL);
            interactiveForegroundPaint.setTextSize(40);
            interactiveForegroundPaint.setTypeface(CLOCK_TYPEFACE);
            interactiveForegroundPaint.setTextAlign(Paint.Align.CENTER);
            interactiveForegroundPaint.setAntiAlias(true);

            interactiveForegroundSubPaint = new Paint(interactiveForegroundPaint);
            interactiveForegroundSubPaint.setTextSize(20);

            ambientForegroundPaint = new Paint(interactiveForegroundPaint);
            ambientForegroundPaint.setColor(Color.GRAY);

            ambientForegroundSubPaint = new Paint(ambientForegroundPaint);
            ambientForegroundSubPaint.setTextSize(20);

            mTime = new Time();

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
            mTime.set(currentMilliseconds);

            boolean[] dots = new boolean[32];
            long tempSeconds = currentMilliseconds / 1000;
            for (int i = 31; i >= 0; i--) {
                dots[i] = tempSeconds % 2 == 1;
                tempSeconds /= 2;
            }

            Paint foregroundPaint = isInAmbientMode() ?
                    ambientForegroundPaint : interactiveForegroundPaint;
            Paint foregroundSubPaint = isInAmbientMode() ?
                    ambientForegroundSubPaint : interactiveForegroundSubPaint;

            canvas.drawRect(bounds, backgroundPaint);
            int dotSize = bounds.width() / 32;
            for (int i = 0; i < 32; i++) {
                if (dots[i]) {
                    canvas.drawCircle(dotSize * i + dotSize / 2,
                            bounds.height() / 2,
                            dotSize / 2,
                            foregroundPaint);
                }
            }

            String clockString = getClockString();
            int clockStringWidth = (int) foregroundPaint.measureText(clockString);
            int clockStringX = bounds.width() / 2;
            int clockStringY = bounds.height() / 2 - 2 * dotSize;
            canvas.drawText(clockString, clockStringX, clockStringY, foregroundPaint);

            String dateString = getDateString();
            int dateStringWidth = (int) foregroundPaint.measureText(clockString);
            int dateStringX = bounds.width() / 2;
            int dateStringY = bounds.height() / 2 + 2 * dotSize + 20;
            canvas.drawText(dateString, dateStringX, dateStringY, foregroundSubPaint);
        }

        private String getClockString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append(String.format("%02d", mTime.hour));
            buffer.append(":");
            buffer.append(String.format("%02d", mTime.minute));
            return buffer.toString();
        }

        private String getDateString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append(WEEKDAY_ABBR[mTime.weekDay]);
            buffer.append(" ");
            buffer.append(String.format("%02d", mTime.month + 1));
            buffer.append("/");
            buffer.append(String.format("%02d", mTime.monthDay));
            buffer.append("/");
            buffer.append(String.format("%d", mTime.year));
            return buffer.toString();
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
                mTime.clear(TimeZone.getDefault().getID());
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
