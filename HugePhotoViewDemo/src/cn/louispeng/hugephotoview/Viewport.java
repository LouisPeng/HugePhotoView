/**
 * @author pengluyu
 *
 * Viewport.java
 * 10:45:30 PM 2014
 */

package cn.louispeng.hugephotoview;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;

/**
 * @author pengluyu
 */
class Viewport implements IViewport {

    // region of Viewport
    public static final String TAG = "Viewport";

    Config CONFIG = Config.ARGB_8888;

    /** The mBitmap of the current viewport */
    Bitmap mBitmap = null;

    /** A Rectangle that defines where the Viewport is within the scene */
    Rect mWindow = new Rect(-1, -1, -1, -1);

    Viewport() {

    }

    // endregion of Viewport

    // region implements IViewport
    @Override
    public IViewport setOrigin(Point viewportOrigin) {
        synchronized (this) {
            int x = viewportOrigin.x;
            int y = viewportOrigin.y;
            int w = mWindow.width();
            int h = mWindow.height();

            // check bounds
            if (x < 0) {
                x = 0;
            }

            if (y < 0) {
                y = 0;
            }

            mWindow.set(x, y, x + w, y + h);

            return this;
        }
    }

    @Override
    public Point getOrigin(Point p) {
        synchronized (this) {
            p.set(mWindow.left, mWindow.top);
            return p;
        }
    }

    @Override
    public IViewport setSize(Point size) {
        synchronized (this) {
            int width = size.x;
            int height = size.y;

            // Adjust bitmap
            if (mBitmap != null) {
                mBitmap.recycle();
            }
            mBitmap = Bitmap.createBitmap(width, height, CONFIG);

            mWindow.set(mWindow.left, mWindow.top, mWindow.left + width, mWindow.top + height);

            return this;
        }
    }

    @Override
    public Point getSize(Point size) {
        synchronized (this) {
            size.x = mWindow.width();
            size.y = mWindow.height();
            return size;
        }
    }

    @Override
    public Point getPhysicalSize(Point size) {
        synchronized (this) {
            if (null != mBitmap) {
                size.x = mBitmap.getWidth();
                size.y = mBitmap.getHeight();
            } else {
                size.x = 0;
                size.y = 0;
            }
            return size;
        }
    }

    @Override
    public IViewport draw(Canvas canvas) {
        synchronized (this) {
            if (null != mBitmap) {
                canvas.drawBitmap(mBitmap, 0, 0, null);
            }
        }
        return this;
    }

    // endregion implements IViewport
}
