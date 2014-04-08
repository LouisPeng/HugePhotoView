/**
 * @author pengluyu
 *
 * ViewportWithCacheCachingThread.java
 * 11:08:01 AM 2014
 */

package cn.louispeng.hugephotoview;

import android.util.Log;
import cn.louispeng.hugephotoview.ViewportWithCache.CacheState;
import cn.louispeng.hugephotoview.demo.BuildConfig;

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

            boolean continueCaching = false;
            synchronized (mCachedViewport) {
                if (mCachedViewport.getCacheState() == CacheState.START_UPDATE) {
                    continueCaching = true;
                }
            }

            if (continueCaching) {
                try {
                    mCachedViewport.fillCache();
                } catch (OutOfMemoryError e) {
                    if (BuildConfig.DEBUG) {
                        Log.i(TAG, "CacheThread out of memory");
                    }
                    /*
                     * Attempt to recover. Experience shows that if we do get an OutOfMemoryError, we're pretty hosed
                     * and are going down.
                     */
                    mCachedViewport.fillCacheOutOfMemoryError(e);

                    synchronized (mCachedViewport) {
                        if (mCachedViewport.getCacheState() == CacheState.IN_UPDATE) {
                            mCachedViewport.setCacheState(CacheState.START_UPDATE);
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
