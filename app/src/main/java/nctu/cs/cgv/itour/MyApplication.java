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

    public static final String mapTag = "tamsui";
    public static final String fileServerURL = "http://140.113.210.14/map/json_maps";
    public static final String APPServerURL = "http://140.113.210.17:55555";
    public static final String fileUploadURL = APPServerURL + "/upload";
    public static final String fileDownloadURL = APPServerURL + "/download";
    public static final String dirPath = Environment.getExternalStorageDirectory().toString() + "/iTour";
    public static final String audioLogPath = dirPath + "/audioLog";
    public static final String imageLogPath = dirPath + "/imageLog";
    public static final String gpsLogPath = dirPath + "/gpsLog";
    public static final String actionLogPath = dirPath + "/actionLog";
    public static final String appLogPath = dirPath + "/appLog";
    private static final String TAG = "MyApplication";
    public static SpotList spotList;
    public static Mesh realMesh;
    public static Mesh warpMesh;
    public static EdgeNode edgeNode;
    public static boolean logFlag = false;

    // map constant
    public static final float MIN_ZOOM = 0.5f;
    public static final float MAX_ZOOM = 6.0f;
    public static final float ZOOM_THRESHOLD = 1.4f;
    public static final int CLUSTER_THRESHOLD = 50000;
    public static final int OVERLAP_THRESHOLD = 500;

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
        File gpsLogDir = new File(gpsLogPath);
        if (!gpsLogDir.exists()) {
            gpsLogDir.mkdirs();
        }
        File actionLogDir = new File(actionLogPath);
        if (!actionLogDir.exists()) {
            actionLogDir.mkdirs();
        }
        File appLogDir = new File(appLogPath);
        if (!appLogDir.exists()) {
            appLogDir.mkdirs();
        }
    }
}