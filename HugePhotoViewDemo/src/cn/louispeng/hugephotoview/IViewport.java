/**
 * @author pengluyu
 *
 * IViewport.java
 * 12:02:10 PM 2014
 */

package cn.louispeng.hugephotoview;

import android.graphics.Canvas;
import android.graphics.Point;

/**
 * @author pengluyu
 */
interface IViewport {
    IViewport setOrigin(Point viewportOrigin);

    Point getOrigin(Point p);

    IViewport setSize(Point size);

    Point getSize(Point size);

    Point getPhysicalSize(Point size);

    IViewport draw(Canvas canvas);
}
