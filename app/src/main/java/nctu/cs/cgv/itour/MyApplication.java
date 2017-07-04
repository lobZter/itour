package nctu.cs.cgv.itour;

import android.app.Application;
import android.os.Environment;
import android.util.Log;

import java.io.File;

/**
 * Created by lobZter on 2017/6/21.
 */

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    public static final String URL = "http://140.113.210.3/map/json_maps/";
    public static final String dirPath = Environment.getExternalStorageDirectory().toString() + "/iTour/";
    public static final String audioPath = dirPath + "audio/";

    @Override
    public void onCreate() {
        super.onCreate();
        mkdirs();
    }

    private void mkdirs() {
        File dir = new File(dirPath);
        dir.mkdirs();

        File audioDir = new File(audioPath);
        Log.d(TAG, String.valueOf(audioDir.mkdirs()));
    }
}