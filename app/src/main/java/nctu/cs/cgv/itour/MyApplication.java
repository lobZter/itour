package nctu.cs.cgv.itour;

import android.app.Application;
import android.os.Environment;

import java.io.File;

import nctu.cs.cgv.itour.object.EdgeNode;
import nctu.cs.cgv.itour.object.Mesh;
import nctu.cs.cgv.itour.object.SpotList;

/**
 * Created by lobZter on 2017/6/21.
 */

public class MyApplication extends Application {

//    public static final String mapTag = "tamsui_1111";
    public static final String mapTag = "tamsui";
    public static final String fileServerURL = "http://140.113.210.14/map/json_maps";
    public static final String APPServerURL = "http://140.113.210.17:55555";
//    public static final String APPServerURL = "http://140.113.210.17";
//    public static final String APPServerURL = "https://itour-lobst3rd.c9users.io";
    public static final String fileUploadURL = APPServerURL + "/upload";
    public static final String fileDownloadURL = APPServerURL + "/download";
    public static final String dirPath = Environment.getExternalStorageDirectory().toString() + "/iTour";
    public static final String audioLogPath = dirPath + "/audioLog";
    public static final String imageLogPath = dirPath + "/imageLog";
    private static final String TAG = "MyApplication";
    public static SpotList spotList;
    public static Mesh realMesh;
    public static Mesh warpMesh;
    public static EdgeNode edgeNode;
    public static boolean logFlag = true;

    @Override
    public void onCreate() {
        super.onCreate();

        // Create folder if it doesn't exist to prevent path not found error.
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File audioDir = new File(audioLogPath);
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }
        File imageDir = new File(imageLogPath);
        if (!imageDir.exists()) {
            imageDir.mkdirs();
        }
    }
}