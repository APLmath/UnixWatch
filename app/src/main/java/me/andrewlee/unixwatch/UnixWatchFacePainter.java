package me.andrewlee.unixwatch;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.format.Time;

/**
 * Created by andrewl on 12/31/14.
 */
public class UnixWatchFacePainter implements WatchFacePainter {

    private final Typeface CLOCK_TYPEFACE = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD);
    private final String[] WEEKDAY_ABBR = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};

    private final Paint backgroundPaint;
    private final Paint interactiveForegroundPaint;
    private final Paint interactiveForegroundSubPaint;
    private final Paint ambientForegroundPaint;
    private final Paint ambientForegroundSubPaint;

    private String currentTimezone;
    private Time time;

    public UnixWatchFacePainter() {
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

        currentTimezone = "";
        time = new Time();
    }

    @Override
    public void paint(Canvas canvas,
                      Rect bounds,
                      long currentMilliseconds,
                      String timezone,
                      boolean isInAmbientMode) {
        if (currentTimezone != timezone) {
            currentTimezone = timezone;
            time.clear(timezone);
        }
        time.set(currentMilliseconds);

        boolean[] dots = new boolean[32];
        long tempSeconds = currentMilliseconds / 1000;
        for (int i = 31; i >= 0; i--) {
            dots[i] = tempSeconds % 2 == 1;
            tempSeconds /= 2;
        }

        Paint foregroundPaint = isInAmbientMode ?
                ambientForegroundPaint : interactiveForegroundPaint;
        Paint foregroundSubPaint = isInAmbientMode ?
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
        int clockStringX = bounds.width() / 2;
        int clockStringY = bounds.height() / 2 - 2 * dotSize;
        canvas.drawText(clockString, clockStringX, clockStringY, foregroundPaint);

        String dateString = getDateString();
        int dateStringX = bounds.width() / 2;
        int dateStringY = bounds.height() / 2 + 2 * dotSize + 20;
        canvas.drawText(dateString, dateStringX, dateStringY, foregroundSubPaint);
    }

    private String getClockString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(String.format("%02d", time.hour));
        buffer.append(":");
        buffer.append(String.format("%02d", time.minute));
        return buffer.toString();
    }

    private String getDateString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(WEEKDAY_ABBR[time.weekDay]);
        buffer.append(" ");
        buffer.append(String.format("%02d", time.month + 1));
        buffer.append("/");
        buffer.append(String.format("%02d", time.monthDay));
        buffer.append("/");
        buffer.append(String.format("%d", time.year));
        return buffer.toString();
    }
}
