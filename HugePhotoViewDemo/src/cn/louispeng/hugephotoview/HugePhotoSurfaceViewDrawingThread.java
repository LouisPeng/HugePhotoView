/**
 * @author pengluyu
 *
 * HugePhotoSurfaceViewDrawingThread.java
 * 11:18:27 PM 2014
 */

package cn.louispeng.hugephotoview;

import android.view.SurfaceHolder;

/**
 * @author pengluyu
 */
class HugePhotoSurfaceViewDrawingThread extends Thread {
    static int REDRAW_PERIOD = 15; // FPS is about 60

    private HugePhotoSurfaceView mHugePhotoSurfaceView;

    private boolean mRunning = false;

    HugePhotoSurfaceViewDrawingThread(HugePhotoSurfaceView hugePhotoSurfaceView) {
        mHugePhotoSurfaceView = hugePhotoSurfaceView;
    }

    // region extends Thread
    @Override
    public void run() {
        final SurfaceHolder surfaceHolder = mHugePhotoSurfaceView.getHolder();
        final HugePhotoScene scene = mHugePhotoSurfaceView.mScene;
        mRunning = true;
        while (mRunning) {
            if (null != scene) {
                scene.draw(surfaceHolder);// draw it
            }
            try {
                // Don't hog the entire CPU
                Thread.sleep(REDRAW_PERIOD);
            } catch (InterruptedException e) {
            }
        }
    }

    void cancel() {
        mHugePhotoSurfaceView = null;
        mRunning = false;
        super.interrupt();
    }

    // endregion
}
