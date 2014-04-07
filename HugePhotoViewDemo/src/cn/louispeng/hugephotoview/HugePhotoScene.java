/**
 * @author pengluyu
 *
 * HugePhotoScene.java
 * 10:42:20 PM 2014
 */

package cn.louispeng.hugephotoview;

import cn.louispeng.hugephotoview.ViewportWithCache.CacheState;

import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;

import java.io.IOException;
import java.io.InputStream;

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

    Point mSceneSize;

    HugePhotoScene(InputStream inputStream) throws IOException {
        BitmapFactory.Options opts = new BitmapFactory.Options();

        // Grab the bounds for the scene dimensions
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(inputStream, null, opts);
        setSceneSize(opts.outWidth, opts.outHeight);

        initializViewport(inputStream);
    }

    HugePhotoScene setSceneSize(int width, int height) {
        mSceneSize.set(width, height);
        return this;
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
    ViewportWithCache mViewport;

    /**
     * Initializes the cache
     * 
     * @throws IOException
     */
    private void initializViewport(InputStream inputStream) throws IOException {
        if (null != mViewport) {
            mViewport.stopCaching().clearCache();
        }
        mViewport = new ViewportWithCache(mSceneSize, inputStream);
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

    /**
     * Suspends or unsuspends the cache thread. This can be used to temporarily stop the cache from updating during a
     * fling event.
     * 
     * @param suspend True to suspend the cache. False to unsuspend.
     */
    HugePhotoScene setSuspend(boolean suspend) {
        if (suspend) {
            synchronized (mViewport) {
                mViewport.setCacheState(CacheState.SUSPEND);
            }
        } else {
            if (mViewport.getCacheState() == CacheState.SUSPEND) {
                synchronized (mViewport) {
                    mViewport.setCacheState(CacheState.INITIALIZED);
                }
            }
        }
        return this;
    }

    /**
     * Invalidate the scene. This causes it to refill
     */
    HugePhotoScene invalidate() {
        mViewport.invalidate();
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

        mViewport.setOrigin(new Point(x, y));

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

    HugePhotoScene draw(Canvas canvas) {
        mViewport.draw(canvas);
        return this;
    }
    // endregion

}
