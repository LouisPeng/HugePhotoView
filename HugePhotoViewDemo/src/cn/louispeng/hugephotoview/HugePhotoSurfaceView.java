/**
 * @author pengluyu
 *
 * HugeHugeImageSurfaceView.java
 * 9:33:44 PM 2014
 */

package cn.louispeng.hugephotoview;

import java.io.IOException;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Scroller;

/**
 * TODO Add gesture and scale features
 * 
 * @author pengluyu
 */
public class HugePhotoSurfaceView extends SurfaceView implements SurfaceHolder.Callback, OnGestureListener {

    public static final String TAG = "HugePhotoSurfaceView";

    private final Touch mTouch;

    private GestureDetector mGestureDectector;

    HugePhotoScene mScene;

    private HugePhotoSurfaceViewDrawingThread mDrawThread;

    // region extends SurfaceView
    public HugePhotoSurfaceView(Context context) {
        super(context);
        mTouch = new Touch(context);
        init(context);
    }

    public HugePhotoSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mTouch = new Touch(context);
        init(context);
    }

    public HugePhotoSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mTouch = new Touch(context);
        init(context);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mGestureDectector.onTouchEvent(event)) {
            return true;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                return mTouch.down(event);
            case MotionEvent.ACTION_MOVE:
                return mTouch.move(event);
            case MotionEvent.ACTION_UP:
                return mTouch.up(event);
            case MotionEvent.ACTION_CANCEL:
                return mTouch.cancel(event);
        }
        return super.onTouchEvent(event);
    }

    // endregion extends SurfaceView

    // region of HugePhotoSurfaceView
    private void init(Context context) {
        mGestureDectector = new GestureDetector(context, this);
        getHolder().addCallback(this);
    }

    public Point getViewportOrigin(Point p) {
        if (null != mScene) {
            mScene.getViewportOrigin(p);
        } else {
            p.x = 0;
            p.y = 0;
        }
        return p;
    }

    public HugePhotoSurfaceView setViewportOrigin(Point p) {
        if (null != mScene) {
            mScene.setViewportOrigin(p);
        }
        return this;
    }

    public HugePhotoSurfaceView setViewportCenter() {
        if (null != mScene) {
            mScene.setViewportCenter();
        }
        return this;
    }

    public HugePhotoSurfaceView setFilepath(String filepath) throws IOException {
        if (null != filepath) {
            if (null != mScene) {
                mScene.stopCaching();
            }
            mScene = new HugePhotoScene(filepath);
        }
        return this;
    }

    // endregion of HugePhotoSurfaceView

    // region implements SurfaceHolder.Callback
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (null != mScene) {
            mScene.startCaching();
        }
        mDrawThread = new HugePhotoSurfaceViewDrawingThread(this);
        mDrawThread.setName("HugePhotoSurfaceViewDrawingThread");
        mDrawThread.start();
        mTouch.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (null != mScene) {
            mScene.setViewportSize(new Point(width, height));
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mTouch.stop();
        mDrawThread.cancel();
        boolean retry = true;
        while (retry) {
            try {
                mDrawThread.join();
                retry = false;
            } catch (InterruptedException e) {
                // we will try it again and again...
            }
        }

        if (null != mScene) {
            mScene.stopCaching();
        }
    }

    // endregion implements SurfaceHolder.Callback

    // region implements OnGestureListener
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        return mTouch.fling(e1, e2, velocityX, velocityY);
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    // endregion implements SurfaceHolder.Callback

    // region class Touch

    class TouchState {
        final static int UNTOUCHED = 0;

        final static int IN_TOUCH = 1;

        final static int START_FLING = 2;

        final static int IN_FLING = 3;
    };

    class Touch {
        int state = TouchState.UNTOUCHED;

        /** Where on the view did we initially touch */
        final Point viewDown = new Point(0, 0);

        /** What was the coordinates of the viewport origin? */
        final Point viewportOriginAtDown = new Point(0, 0);

        final Scroller scroller;

        TouchThread touchThread;

        Touch(Context context) {
            scroller = new Scroller(context);
        }

        void start() {
            touchThread = new TouchThread(this);
            touchThread.setName("touchThread");
            touchThread.start();
        }

        void stop() {
            touchThread.running = false;
            touchThread.interrupt();

            boolean retry = true;
            while (retry) {
                try {
                    touchThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    // we will try it again and again...
                }
            }
            touchThread = null;
        }

        Point fling_viewOrigin = new Point();

        Point fling_viewSize = new Point();

        Point fling_sceneSize = new Point();

        boolean fling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            mScene.getViewportOrigin(fling_viewOrigin);
            mScene.getViewportSize(fling_viewSize);
            mScene.getSceneSize(fling_sceneSize);

            synchronized (this) {
                state = TouchState.START_FLING;
                mScene.suspendCache();
                scroller.fling(fling_viewOrigin.x, fling_viewOrigin.y, (int)-velocityX, (int)-velocityY, 0,
                        fling_sceneSize.x - fling_viewSize.x, 0, fling_sceneSize.y - fling_viewSize.y);
                touchThread.interrupt();
            }
            return true;
        }

        boolean down(MotionEvent event) {
            mScene.resumeCache(); // If we were suspended because of a fling
            synchronized (this) {
                state = TouchState.IN_TOUCH;
                viewDown.x = (int)event.getX();
                viewDown.y = (int)event.getY();
                Point p = mScene.getViewportOrigin(new Point());
                viewportOriginAtDown.set(p.x, p.y);
            }
            return true;
        }

        boolean move(MotionEvent event) {
            if (state == TouchState.IN_TOUCH) {
                float zoom = 1.0f;
                float deltaX = zoom * (event.getX() - viewDown.x);
                float deltaY = zoom * (event.getY() - viewDown.y);
                float newX = (viewportOriginAtDown.x - deltaX);
                float newY = (viewportOriginAtDown.y - deltaY);

                mScene.setViewportOrigin(new Point((int)newX, (int)newY));
                invalidate();
            }
            return true;
        }

        boolean up(MotionEvent event) {
            if (state == TouchState.IN_TOUCH) {
                state = TouchState.UNTOUCHED;
            }
            return true;
        }

        boolean cancel(MotionEvent event) {
            if (state == TouchState.IN_TOUCH) {
                state = TouchState.UNTOUCHED;
            }
            return true;
        }

        class TouchThread extends Thread {
            final Touch touch;

            boolean running = false;

            void setRunning(boolean value) {
                running = value;
            }

            TouchThread(Touch touch) {
                this.touch = touch;
            }

            @Override
            public void run() {
                running = true;
                while (running) {
                    while (touch.state != TouchState.START_FLING && touch.state != TouchState.IN_FLING) {
                        try {
                            Thread.sleep(Integer.MAX_VALUE);
                        } catch (InterruptedException e) {
                        }
                        if (!running)
                            return;
                    }
                    synchronized (touch) {
                        if (touch.state == TouchState.START_FLING) {
                            touch.state = TouchState.IN_FLING;
                        }
                    }
                    if (touch.state == TouchState.IN_FLING) {
                        scroller.computeScrollOffset();
                        mScene.setViewportOrigin(new Point(scroller.getCurrX(), scroller.getCurrY()));
                        if (scroller.isFinished()) {
                            mScene.resumeCache();
                            synchronized (touch) {
                                touch.state = TouchState.UNTOUCHED;
                                try {
                                    Thread.sleep(5);
                                } catch (InterruptedException e) {
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
