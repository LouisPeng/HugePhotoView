
package cn.louispeng.hugephotoview;

import cn.louispeng.hugephotoview.demo.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * An example full-screen activity that shows and hides the system UI (i.e. status bar and navigation/system bar) with
 * user interaction.
 * 
 * @see SystemUiHider
 */
public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";

    private static final String KEY_X = "X";

    private static final String KEY_Y = "Y";

    private static final String KEY_FN = "FN";

    private String filename = null;

    private HugePhotoSurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        mSurfaceView = (HugePhotoSurfaceView)findViewById(R.id.main_surfaceview);

        // Setup/restore state
        if (savedInstanceState != null && savedInstanceState.containsKey(KEY_X)
                && savedInstanceState.containsKey(KEY_Y)) {
            Log.d(TAG, "restoring state");
            int x = (Integer)savedInstanceState.get(KEY_X);
            int y = (Integer)savedInstanceState.get(KEY_Y);

            String fn = null;
            if (savedInstanceState.containsKey(KEY_FN))
                fn = (String)savedInstanceState.get(KEY_FN);

            try {
                if (fn == null || fn.length() == 0) {
                    Log.w(TAG, "Invalid file path");
                    this.finish();
                    return;
                } else {
                    mSurfaceView.setInputStream(new RandomAccessFileInputStream(fn));
                }
                mSurfaceView.setViewportOrigin(new Point(x, y));
            } catch (java.io.IOException e) {
                Log.e(TAG, e.getMessage());
            }
        } else {
            // Centering the map to start
            Intent intent = getIntent();
            try {
                Uri uri = null;
                if (intent != null)
                    uri = getIntent().getData();

                InputStream is = null;
                if (uri != null) {
                    filename = uri.getPath();
                    is = new RandomAccessFileInputStream(uri.getPath());
                } else {
                    Log.w(TAG, "Invalid file path");
                    this.finish();
                    return;
                }

                mSurfaceView.setInputStream(is);
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
            mSurfaceView.setViewportCenter();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Point p = mSurfaceView.getViewportOrigin(new Point());

        outState.putInt(KEY_X, p.x);
        outState.putInt(KEY_Y, p.y);
        if (filename != null) {
            outState.putString(KEY_FN, filename);
        }
        
        super.onSaveInstanceState(outState);
    }
}
