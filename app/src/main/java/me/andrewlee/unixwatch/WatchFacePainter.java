package me.andrewlee.unixwatch;

import android.graphics.Canvas;
import android.graphics.Rect;

/**
 * Created by andrewl on 12/31/14.
 */
public interface WatchFacePainter {

    void paint(Canvas canvas,
               Rect bounds,
               long currentMilliseconds,
               String timezone,
               boolean isInAmbientMode);

}
