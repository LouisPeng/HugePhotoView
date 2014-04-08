/**
 * @author pengluyu
 *
 * HugePhotoScene.java
 * 10:42:20 PM 2014
 */

package cn.louispeng.hugephotoview;

import java.io.IOException;

import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Point;
import android.view.SurfaceHolder;
import cn.louispeng.hugephotoview.ViewportWithCache.CacheState;

/*
 * (0,0)---------------------------------------------------------------+
 * |                                                                   |
 * |  +------------------------+                                       |
 * |  |                        |                                       |
 * |  |                        |                                       |
 * |  |                        |                                       |
 * |  |               viewport |                                       |
 * |  +------------------------+                                       |
 * |                                                                   |
 * |                                                                   |
 * |                                                                   |
 * |                                                                   |
 * |                                                                   |
 * |                                                                   |
 * |                                                                   |
 * |                                                                   |
 * |                                                                   |
 * |                                                                   |
 * |                                              Scene, Entire bitmap |
 * +-----------------------------------------------------------(w-1,h-1)
 */
/**
 * @author pengluyu
 */
class HugePhotoScene {
    // region of HugePhotoScene

    Point mSceneSize = new Point();

    ViewportWithCache mViewport;

    HugePhotoScene(String filepath) throws IOException {
        Options opts = new Options();

        // Grab the bounds for the scene dimensions
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filepath, opts);
        mSceneSize.set(opts.outWidth, opts.outHeight);

        initializViewport(filepath);
    }

    /**
     * Returns a Point representing the size of the scene. Don't modify the returned Point!
     */
    Point getSceneSize(Point size) {
        synchronized (this) {
            size.set(mSceneSize.x, mSceneSize.y);
        }
        return size;
    }

    // endregion

    // region of the CacheViewport
    private void initializViewport(String filepath) throws IOException {
        mViewport = new ViewportWithCache(mSceneSize, filepath);
        if (mViewport.getCacheState() == CacheState.UNINITIALIZED) {
            synchronized (mViewport) {
                mViewport.setCacheState(CacheState.INITIALIZED);
            }
        }
    }

    /** Starts the cache thread */
    HugePhotoScene startCaching() {
        mViewport.startCaching();
        return this;
    }

    /** Stops the cache thread */
    HugePhotoScene stopCaching() {
        mViewport.stopCaching();
        return this;
    }

    /** Suspends the cache thread. This can be used to temporarily stop the cache from updating during a fling event. */
    HugePhotoScene suspendCache() {
        if (mViewport.getCacheState() != CacheState.UNINITIALIZED) {
            synchronized (mViewport) {
                mViewport.setCacheState(CacheState.SUSPEND);
            }
        }
        return this;
    }

    /** Resume the cache thread. */
    HugePhotoScene resumeCache() {
        if (mViewport.getCacheState() == CacheState.SUSPEND) {
            synchronized (mViewport) {
                mViewport.setCacheState(CacheState.INITIALIZED);
            }
        }
        return this;
    }

    /** Invalidate the scene. This causes it to refill */
    HugePhotoScene invalidate() {
        mViewport.invalidate();
        return this;
    }

    Point getViewportSize(Point size) {
        return mViewport.getSize(size);
    }

    HugePhotoScene setViewportSize(Point size) {
        Point currentSize = mViewport.getSize(new Point());
        if (!currentSize.equals(size.x, size.y)) {
            mViewport.setSize(size);
        }
        return this;
    }

    Point getViewportOrigin(Point p) {
        return mViewport.getOrigin(p);
    }

    HugePhotoScene setViewportOrigin(Point p) {
        int x = p.x;
        int y = p.y;
        Point size = mViewport.getSize(new Point());

        // Clamp the origin
        if (x < 0) {
            x = 0;
        }
        if (y < 0) {
            y = 0;
        }
        if (x + size.x > mSceneSize.x) {
            x = mSceneSize.x - size.x;
        }
        if (y + size.y > mSceneSize.y) {
            y = mSceneSize.y - size.y;
        }

        Point currentOrigin = mViewport.getOrigin(new Point());
        if (!currentOrigin.equals(x, y)) {
            mViewport.setOrigin(new Point(x, y));
        }
        return this;
    }

    HugePhotoScene setViewportCenter() {
        Point centerOfScene = new Point(mSceneSize.x >> 1, mSceneSize.y >> 1);

        // Calculate the origin
        Point sizeOfViewport = mViewport.getSize(new Point());
        int x = centerOfScene.x - (sizeOfViewport.x >> 1);
        int y = centerOfScene.y - (sizeOfViewport.y >> 1);

        return setViewportOrigin(new Point(x, y));
    }

    HugePhotoScene draw(SurfaceHolder surfaceHolder) {
        mViewport.updateVisibleViewportBitmap();
        synchronized (surfaceHolder) {
            if (mViewport.isViewportBitmapChanged()) {
                Canvas canvas = surfaceHolder.lockCanvas();
                if (canvas != null) {
                    mViewport.draw(canvas);
                }

                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
        return this;
    }
    // endregion

}
