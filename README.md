UnixWatch
=========

A Unix-time watch face for Android Wear.

Setting up
----------
1. Open Android Studio.
2. `File > Import Project` and select the `UnixWatch` project.

Usage
-----
I tried to decouple the actual watch face-painting logic from the rest of the app by creating a `WatchFacePainter` interface. So the idea is that `UnixWatchFaceService` handles all the boilerplate setup, so that a class that implements `WatchFacePainter` can focus on just painting the watch face.

Therefore, if you want to write your own basic watch face, create a class that implements `WatchFacePainter`, and then instantiate it in place of the current `UnixWatchFacePainter` instance within `UnixWatchFaceService`.
