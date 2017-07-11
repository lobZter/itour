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
    public static final String fileServerURL = "http://140.113.210.14/map/json_maps/";
    public static final String APPServerURL = "https://itour-lobst3rd.c9users.io";
    public static final String fileUploadURL = APPServerURL + "/upload";
    public static final String dirPath = Environment.getExternalStorageDirectory().toString() + "/iTour/";
    public static final String audioPath = dirPath + "audio/";
    public static final String photoPath = dirPath + "photo/";


    @Override
    public void onCreate() {
        super.onCreate();
        mkdirs();
    }

    private void mkdirs() {
        File audioDir = new File(audioPath);
        if(!audioDir.exists())
            audioDir.mkdirs();

        File photoDir = new File(photoPath);
        if(!photoDir.exists())
            photoDir.mkdirs();
    }
}