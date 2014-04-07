/**
 * @author pengluyu
 *
 * ViewportWithCacheCachingThread.java
 * 11:08:01 AM 2014
 */

package cn.louispeng.hugephotoview;

import android.graphics.Bitmap;
import android.os.Debug;
import android.util.Log;

import cn.louispeng.hugephotoview.ViewportWithCache.CacheState;

/**
 * The CachingThread's job is to wait until the CacheState is START_UPDATE and then update the Cache bitmap.
 * 
 * @author pengluyu
 */
public class ViewportWithCacheCachingThread extends Thread {
    // region of ViewportWithCacheCachingThread
    public static final String TAG = "ViewportWithCacheCachingThread";

    private ViewportWithCache mCachedViewport;

    private boolean mRunning = false;

    ViewportWithCacheCachingThread(ViewportWithCache cachedViewport) {
        mCachedViewport = cachedViewport;
    }

    // endregion of ViewportWithCacheCachingThread

    // region extends Thread
    @Override
    public void run() {
        mRunning = true;
        while (mRunning) {
            while (mRunning && mCachedViewport.getCacheState() != CacheState.START_UPDATE) {
                try {
                    // Sleep until we have something to do
                    Thread.sleep(Integer.MAX_VALUE);
                } catch (InterruptedException ignored) {
                }
            }
            if (!mRunning) {
                return;
            }

            synchronized (mCachedViewport) {
                if (mCachedViewport.getCacheState() == CacheState.START_UPDATE) {
                    try {
                        mCachedViewport.fillCache();
                    } catch (OutOfMemoryError e) {
                        Log.d(TAG, "CacheThread out of memory");
                        /*
                         * Attempt to recover. Experience shows that if we do get an OutOfMemoryError, we're pretty
                         * hosed and are going down.
                         */
                        synchronized (mCachedViewport) {
                            mCachedViewport.fillCacheOutOfMemoryError(e);
                            if (mCachedViewport.getCacheState() == CacheState.IN_UPDATE) {
                                mCachedViewport.setCacheState(CacheState.START_UPDATE);
                            }
                        }
                    }
                }
            }
        }
    }

    void cancel() {
        mRunning = false;
        mCachedViewport = null;
        super.interrupt();
    }
    // endregion
}
