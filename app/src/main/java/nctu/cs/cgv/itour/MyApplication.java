package nctu.cs.cgv.itour;

import android.app.Application;
import android.content.Intent;
import android.os.Environment;

import java.io.File;
import java.io.StreamCorruptedException;
import java.io.StringReader;

import nctu.cs.cgv.itour.object.EdgeNode;
import nctu.cs.cgv.itour.object.Mesh;
import nctu.cs.cgv.itour.object.SpotList;
import nctu.cs.cgv.itour.object.SpotNode;
import nctu.cs.cgv.itour.service.GpsLocationService;

/**
 * Created by lobZter on 2017/6/21.
 */

public class MyApplication extends Application {

    private static final String TAG = "MyApplication";
    public static final String mapTag = "TamsuiNewFull";
    public static final String fileServerURL = "http://140.113.210.14/map/json_maps";
//    public static final String APPServerURL = "http://140.113.210.17";
    public static final String APPServerURL = "https://itour-lobst3rd.c9users.io";
    public static final String fileUploadURL = APPServerURL + "/upload";
    public static final String fileDownloadURL = APPServerURL + "/download";
    public static final String dirPath = Environment.getExternalStorageDirectory().toString() + "/iTour";
    public static final String audioLogPath = dirPath + "/audioLog";
    public static SpotList spotList;
    public static Mesh realMesh;
    public static Mesh warpMesh;
    public static EdgeNode edgeNode;
    public static boolean logFlag = true;

    @Override
    public void onCreate() {
        super.onCreate();

        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File audioDir = new File(audioLogPath);
        if (!audioDir.exists()) {
            audioDir.mkdirs();
        }

    }
}