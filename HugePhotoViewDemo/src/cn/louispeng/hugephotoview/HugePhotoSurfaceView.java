/**
 * @author pengluyu
 *
 * HugeHugeImageSurfaceView.java
 * 9:33:44 PM 2014
 */

package cn.louispeng.hugephotoview;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author pengluyu
 */
public class HugePhotoSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    HugePhotoScene mScene;

    // region extends SurfaceView
    public HugePhotoSurfaceView(Context context) {
        super(context);
        init(context);
    }

    public HugePhotoSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HugePhotoSurfaceView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    // endregion

    // region HugePhotoSurfaceView
    private void init(Context context) {
        // TODO
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
        // TODO
        return this;
    }

    public HugePhotoSurfaceView setInputStream(InputStream inputStream) throws IOException {
        if (null != inputStream) {
            if (null != mScene) {
                mScene.stopCaching();
            }
            mScene = new HugePhotoScene(inputStream);
        }
        return this;
    }

    // endregion

    // region implements SurfaceHolder.Callback
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub

    }

    // endregion

}
