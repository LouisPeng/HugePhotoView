
package cn.louispeng.hugephotoview;

import java.io.IOException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import cn.louispeng.hugephotoview.demo.R;

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

    private static final String KEY_FILEPATH = "FILE_PATH";

    private static final String DEFAULT_PHOTO = Environment.getExternalStorageDirectory() + "/TombNotesMaps.jpg";

    // private static final String DEFAULT_PHOTO = Environment.getExternalStorageDirectory() + "/world.jpg";

    private String mFilePath = null;

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
            Log.d(TAG, "Restoring state");
            int x = (Integer)savedInstanceState.get(KEY_X);
            int y = (Integer)savedInstanceState.get(KEY_Y);

            if (savedInstanceState.containsKey(KEY_FILEPATH)) {
                mFilePath = (String)savedInstanceState.get(KEY_FILEPATH);
            }

            if (mFilePath == null || mFilePath.length() == 0) {
                Log.w(TAG, "Invalid file path");
                finish();
                return;
            }

            try {
                mSurfaceView.setFilepath(mFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }

            mSurfaceView.setViewportOrigin(new Point(x, y));
        } else {
            // Centering the map to start
            Intent intent = getIntent();
            Uri uri = null;
            if (intent != null && (uri = getIntent().getData()) != null) {
                mFilePath = uri.getPath();
                if (mFilePath == null || mFilePath.length() == 0) {
                    Log.w(TAG, "Invalid file path");
                    finish();
                    return;
                }
            } else {
                mFilePath = DEFAULT_PHOTO;
            }

            try {
                mSurfaceView.setFilepath(mFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSurfaceView.setViewportCenter();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Point viewportOrigin = mSurfaceView.getViewportOrigin(new Point());

        outState.putInt(KEY_X, viewportOrigin.x);
        outState.putInt(KEY_Y, viewportOrigin.y);
        if (mFilePath != null) {
            outState.putString(KEY_FILEPATH, mFilePath);
        }

        super.onSaveInstanceState(outState);
    }
}
