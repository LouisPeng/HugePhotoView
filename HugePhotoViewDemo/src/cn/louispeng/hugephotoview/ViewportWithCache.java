/**
 * @author pengluyu
 *
 * Viewport.java
 * 10:45:30 PM 2014
 */

package cn.louispeng.hugephotoview;

import java.io.IOException;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;

/*
 * (0,0)------------------------------+
 * |                                  |
 * |                                  |
 * |    +------------------------+    |
 * |    |                        |    |
 * |    |                        |    |
 * |    |                        |    |
 * |    |       Visible viewport |    |
 * |    +------------------------+    |
 * |                                  |
 * |              Viewport with cache |
 * |----------------------------------+ 
 */
/**
 * While user moves the viewport, we should decode bitmap with new region, the user experience will be jumpy.
 * ViewportWithCache decodes and stores a bigger region than the visible viewport, decoding is not necessary while
 * moving the viewport within the cached region.
 * 
 * @author pengluyu
 */
class ViewportWithCache implements IViewport {
    // region of ViewportWithCache
    public static final String TAG = "ViewportWithCache";

    static class CacheState {
        public static final int UNINITIALIZED = 1;

        public static final int INITIALIZED = 2;

        public static final int START_UPDATE = 3;

        public static final int IN_UPDATE = 4;

        public static final int READY = 5;

        public static final int SUSPEND = 6;
    }

    final static Config DEFAULT_CONFIG = Config.RGB_565;

    final static int BYTES_PER_PIXEL = 2; // Related to DEFAULT_CONFIG

    /**
     * What percent of total memory should we use for the cache? The bigger the cache, the longer it takes to read --
     * 1.2 secs for 25%, 600ms for 10%, 500ms for 5%. User experience seems to be best for smaller values.
     */
    private int mCacheInlargePercent = 5;

    // The size of scene that holds this viewport
    private final Point mSceneSize;

    // The viewport which is visible
    private final Viewport mVisibleViewport;

    // This flag is used to avoid unnecessary bitmap copy
    private boolean mIsSampledBitmapChanged;

    private boolean mIsViewportBitmapChanged;

    // Store the cached bitmap
    private Bitmap mCachedBitmap = null;

    private Rect mCachedWindow;

    // Store the down sampled bitmap for quick look before the region decoding is finished
    private Bitmap mSampledBitmap = null;

    // The down sample size for the sample image. 1=1/2, 2=1/4 3=1/8, etc
    private final int DOWN_SAMPLE_SHIFT = 2;

    private final BitmapRegionDecoder mDecoder;

    private int mCacheState = CacheState.UNINITIALIZED;

    private ViewportWithCacheCachingThread mCacheThread;

    ViewportWithCache(Point sceneSize, String filepath) throws IOException {
        mSceneSize = sceneSize;

        mVisibleViewport = new Viewport();
        mVisibleViewport.CONFIG = DEFAULT_CONFIG;

        mCachedBitmap = null;
        mCachedWindow = null;

        mDecoder = BitmapRegionDecoder.newInstance(filepath, false);

        // TODO Create the sample image, should calculate DOWN_SAMPLE_SHIFT here
        Options opts = new Options();
        opts.inPreferredConfig = DEFAULT_CONFIG;
        opts.inSampleSize = (1 << DOWN_SAMPLE_SHIFT);
        mSampledBitmap = BitmapFactory.decodeFile(filepath, opts);
    }

    ViewportWithCache startCaching() {
        if (mCacheThread != null) {
            mCacheThread.cancel();
            mCacheThread = null;
        }
        mCacheThread = new ViewportWithCacheCachingThread(this);
        mCacheThread.setName("mCacheThread");
        mCacheThread.start();

        return this;
    }

    ViewportWithCache stopCaching() {
        if (mCacheThread != null) {
            mCacheThread.cancel();
            boolean retry = true;
            while (retry) {
                try {
                    mCacheThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    // we will try it again and again...
                }
            }
            mCacheThread = null;
        }

        return this;
    }

    /**
     * Refresh the cache bitmap
     */
    ViewportWithCache invalidate() {
        synchronized (this) {
            setCacheState(CacheState.INITIALIZED);
            mCacheThread.interrupt();
        }
        return this;
    }

    /**
     * 将Visible Viewport中的数据绘制到
     */
    @Override
    public IViewport draw(Canvas canvas) {
        synchronized (this) {
            mVisibleViewport.draw(canvas);
            setIsViewportBitmapChanged(false);
        }

        return this;
    }

    private void loadSampleIntoViewport() {
        if (getCacheState() != CacheState.UNINITIALIZED) {
            synchronized (mVisibleViewport) {
                if (null != mVisibleViewport.mBitmap) {
                    Canvas canvas = new Canvas(mVisibleViewport.mBitmap);
                    int left = (mVisibleViewport.mWindow.left >> DOWN_SAMPLE_SHIFT);
                    int top = (mVisibleViewport.mWindow.top >> DOWN_SAMPLE_SHIFT);
                    int right = left + (mVisibleViewport.mWindow.width() >> DOWN_SAMPLE_SHIFT);
                    int bottom = top + (mVisibleViewport.mWindow.height() >> DOWN_SAMPLE_SHIFT);
                    Rect srcRect = new Rect(left, top, right, bottom);
                    Rect identity = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
                    canvas.drawBitmap(mSampledBitmap, srcRect, identity, null);
                }
            }
        }
    }

    void loadBitmapIntoViewport(Bitmap bitmap) {
        synchronized (mVisibleViewport) {
            if (null != mVisibleViewport.mBitmap) {
                Rect srcRect = new Rect(0, 0, 0, 0);
                Rect dstRect = new Rect(0, 0, 0, 0);
                Point dstSize = new Point();
                int left = mVisibleViewport.mWindow.left - mCachedWindow.left;
                int top = mVisibleViewport.mWindow.top - mCachedWindow.top;
                int right = left + mVisibleViewport.mWindow.width();
                int bottom = top + mVisibleViewport.mWindow.height();
                mVisibleViewport.getPhysicalSize(dstSize);
                srcRect.set(left, top, right, bottom);
                dstRect.set(0, 0, dstSize.x, dstSize.y);
                Canvas canvas = new Canvas(mVisibleViewport.mBitmap);
                canvas.drawBitmap(bitmap, srcRect, dstRect, null);
            }
        }
    }

    /**
     * 根据CacheState的更新Visible Viewport中的Bitmap
     * 
     * @param cachedViewport
     */
    void updateVisibleViewportBitmap() {
        // This function needs improvement. We shouldn't update viewport until it's changed.
        Bitmap bitmap = null; // If this is null at the bottom, then load from the sample
        synchronized (this) {
            switch (getCacheState()) {
                case CacheState.UNINITIALIZED: {
                    // nothing can be done -- should never get here
                    return;
                }
                case CacheState.INITIALIZED: {
                    // time to cache some data
                    setCacheState(CacheState.START_UPDATE);
                    mCacheThread.interrupt();
                    break;
                }
                case CacheState.START_UPDATE: {
                    // I already told the thread to start
                    break;
                }
                case CacheState.IN_UPDATE: {
                    // Already reading some data, just use the sample
                    break;
                }
                case CacheState.SUSPEND: {
                    // Loading from cache suspended.
                    break;
                }
                case CacheState.READY: {
                    // I have some data to show
                    if (mCachedBitmap == null) {
                        // Start the cache
                        setCacheState(CacheState.START_UPDATE);
                        mCacheThread.interrupt();
                    } else if (!mCachedWindow.contains(mVisibleViewport.mWindow)) {
                        setCacheState(CacheState.START_UPDATE);
                        mCacheThread.interrupt();
                    } else {
                        // Happy case -- the cache.window already contains the viewport.window
                        bitmap = mCachedBitmap;
                    }
                    break;
                }
            }
        }

        if (isSampledBitmapChanged() || isViewportBitmapChanged()) {
            if (bitmap != null) {
                loadBitmapIntoViewport(bitmap);
            } else {
                loadSampleIntoViewport();
            }

            setIsViewportBitmapChanged(true);
            setIsSampledBitmapChanged(false);
        }
    }

    int getCacheState() {
        return mCacheState;
    }

    ViewportWithCache setCacheState(int cacheState) {
        synchronized (this) {
            mCacheState = cacheState;
        }
        return this;
    }

    boolean isViewportBitmapChanged() {
        synchronized (this) {
            return mIsViewportBitmapChanged;
        }
    }

    private void setIsViewportBitmapChanged(boolean flag) {
        synchronized (this) {
            mIsViewportBitmapChanged = flag;
        }
    }

    private void setIsSampledBitmapChanged(boolean flag) {
        synchronized (this) {
            mIsSampledBitmapChanged = flag;
        }
    }

    private boolean isSampledBitmapChanged() {
        synchronized (this) {
            return mIsSampledBitmapChanged;
        }
    }

    /**
     * 
     */
    public void clearCache() {
        synchronized (this) {
            if (null != mCachedBitmap) {
                mCachedBitmap.recycle();
                mCachedBitmap = null;
                mCachedWindow = null;
            }
        }
    }

    /**
     * Not enough memory for caching
     */
    public void fillCacheOutOfMemoryError(OutOfMemoryError e) {
        synchronized (this) {
            if (mCacheInlargePercent > 0) {
                mCacheInlargePercent--;
            }
        }
    }

    /**
     * Calculate the region to be cached
     */
    Rect calculateCacheWindow() {
        final Rect visiableViewportRect = mVisibleViewport.mWindow;
        Rect cachedRegionRect = new Rect(visiableViewportRect);

        long bytesToUse = 0;
        synchronized (this) {
            // TODO This function is incorrect, I suppose
            bytesToUse = Runtime.getRuntime().maxMemory() * mCacheInlargePercent / 100;
        }

        if (bytesToUse > 0) {
            cachedRegionRect = new Rect();
            int vw = visiableViewportRect.width();
            int vh = visiableViewportRect.height();

            // Calculate the max size of the margins to fit in our memory budget
            int tw = 0;
            int th = 0;
            int mw = tw;
            int mh = th;
            while ((vw + tw) * (vh + th) * BYTES_PER_PIXEL < bytesToUse) {
                mw = tw++;
                mh = th++;
            }

            // Trim the margins if they're too big.
            if (vw + mw > mSceneSize.x) {
                // viewport width + margin width > width of the image
                mw = Math.max(0, mSceneSize.x - vw);
            }
            if (vh + mh > mSceneSize.y) {
                // viewport height + margin height > height of the image
                mh = Math.max(0, mSceneSize.y - vh);
            }

            // Figure out the left & right based on the margin. We assume our viewportRect
            // is <= our size. If that's not the case, then this logic breaks.
            int left = visiableViewportRect.left - (mw >> 1);
            int right = visiableViewportRect.right + (mw >> 1);
            if (left < 0) {
                right = right - left; // Add's the overage on the left side back to the right
                left = 0;
            }
            if (right > mSceneSize.x) {
                left = left - (right - mSceneSize.x); // Adds overage on right side back to left
                right = mSceneSize.x;
            }

            // Figure out the top & bottom based on the margin. We assume our viewportRect
            // is <= our size. If that's not the case, then this logic breaks.
            int top = visiableViewportRect.top - (mh >> 1);
            int bottom = visiableViewportRect.bottom + (mh >> 1);
            if (top < 0) {
                bottom = bottom - top; // Add's the overage on the top back to the bottom
                top = 0;
            }
            if (bottom > mSceneSize.y) {
                top = top - (bottom - mSceneSize.y); // Adds overage on bottom back to top
                bottom = mSceneSize.y;
            }

            // Set the origin based on our new calculated values.
            cachedRegionRect.set(left, top, right, bottom);
        }

        return cachedRegionRect;
    }

    /**
     * Decode the bitmap of cache region
     */
    @SuppressLint("NewApi")
    ViewportWithCache fillCache() {
        setCacheState(CacheState.IN_UPDATE);

        Rect newCacheRect = calculateCacheWindow();
        boolean isBitmapReusable = false;
        synchronized (this) {
            if (null != mCachedBitmap) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    // BitmapRegionDecoder support for inBitmap was introduced in JELLY_BEAN
                    int width = newCacheRect.right - newCacheRect.left;
                    int height = newCacheRect.bottom - newCacheRect.top;
                    if (width == mCachedBitmap.getWidth() && height == mCachedBitmap.getHeight()) {
                        // Only when newCacheRect is the same size
                        isBitmapReusable = true;
                    }
                }
            }
        }

        if (!isBitmapReusable) {
            clearCache();
        }

        if (mDecoder != null) {
            Options opts = new Options();
            opts.inPreferredConfig = DEFAULT_CONFIG;
            if (isBitmapReusable) {
                Bitmap inBitmap = mCachedBitmap;
                opts.inBitmap = inBitmap;
                mCachedBitmap = mDecoder.decodeRegion(newCacheRect, opts);
                if (!mCachedBitmap.sameAs(inBitmap)) {
                    // Still we should make sure the bitmap is reused
                    inBitmap.recycle();
                }
            } else {
                mCachedBitmap = mDecoder.decodeRegion(newCacheRect, opts);
            }

            mCachedWindow = newCacheRect;
            setIsSampledBitmapChanged(true);
        }

        setCacheState(CacheState.READY);

        return this;
    }

    // endregion of ViewportWithCache

    // region implements IViewport
    @Override
    public IViewport setOrigin(Point origin) {
        mVisibleViewport.setOrigin(origin);
        setIsViewportBitmapChanged(true);
        return this;
    }

    @Override
    public Point getOrigin(Point origin) {
        return mVisibleViewport.getOrigin(origin);
    }

    @Override
    public IViewport setSize(Point size) {
        mVisibleViewport.setSize(size);
        setIsViewportBitmapChanged(true);
        return this;
    }

    @Override
    public Point getSize(Point size) {
        return mVisibleViewport.getSize(size);
    }

    @Override
    public Point getPhysicalSize(Point size) {
        return mVisibleViewport.getPhysicalSize(size);
    }

    // endregion implements IViewport
}
