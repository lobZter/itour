package nctu.cs.cgv.itour;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.util.Log;
import android.util.TypedValue;
import android.view.inputmethod.InputMethodManager;

import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import cz.msebera.android.httpclient.Header;
import nctu.cs.cgv.itour.object.IdxWeights;
import nctu.cs.cgv.itour.object.Mesh;

import static nctu.cs.cgv.itour.MyApplication.realMesh;
import static nctu.cs.cgv.itour.MyApplication.warpMesh;

/**
 * Created by lobZter on 2017/6/21.
 */

public class Utility {

    public static int dpToPx(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }

    public static float lerp(float from, float to, float alpha) {
        return from + alpha * (to - from);
    }

    public static float[] lerp(float[] from, float[] to, float alpha) {
        if (from.length != to.length) return null;

        for (int i = 0; i < from.length; i++) {
            from[i] = from[i] + alpha * (to[i] - from[i]);
        }

        return from;
    }

    public static float[] gpsToImgPx(float lat, float lng) {
        float[] warpMeshPos = new float[]{0, 0};
        float realMeshX = realMesh.mapWidth * (lng - realMesh.minLon) / (realMesh.maxLon - realMesh.minLon);
        float realMeshY = realMesh.mapHeight * (realMesh.maxLat - lat) / (realMesh.maxLat - realMesh.minLat);
        IdxWeights idxWeights = realMesh.getPointInTriangleIdx(realMeshX, realMeshY);
        if (idxWeights.idx >= 0) {
            warpMeshPos = warpMesh.interpolatePosition(idxWeights);
        }
        return warpMeshPos;
    }

    public static float[] imgPxToGps(float imgX, float imgY) {
        float[] realMeshPos = new float[]{0, 0};
        IdxWeights idxWeights = warpMesh.getPointInTriangleIdx(imgX, imgY);
        if (idxWeights.idx >= 0) {
            realMeshPos = realMesh.interpolatePosition(idxWeights);
            realMeshPos[0] = realMeshPos[0] / realMesh.mapWidth * (realMesh.maxLon - realMesh.minLon) + realMesh.minLon;
            realMeshPos[1] = realMesh.maxLat - realMeshPos[1] / realMesh.mapHeight * (realMesh.maxLat - realMesh.minLat);
        }
        return realMeshPos;
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }

    public static void moveFile(String inputPath, String inputFile, String outputPath) {

        InputStream in = null;
        OutputStream out = null;
        try {

            //create output directory if it doesn't exist
            File dir = new File(outputPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }


            in = new FileInputStream(inputPath + "/" + inputFile);
            out = new FileOutputStream(outputPath + "/" + inputFile);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;

            // write the output file
            out.flush();
            out.close();
            out = null;

            // delete the original file
            new File(inputPath + "/" + inputFile).delete();


        } catch (Exception e) {
            Log.e("moveFile", e.getMessage());
        }
    }

    public static void actionLog(String log) {
//        AsyncHttpClient client = new AsyncHttpClient();
//        client.get("https://food-map-lobst3rd.c9users.io/actionLog?user="+ FirebaseAuth.getInstance().getCurrentUser().getDisplayName()+"&log="+log,
//                new AsyncHttpResponseHandler() {
//
//                    @Override
//                    public void onStart() {
//                        // called before request is started
//                    }
//
//                    @Override
//                    public void onSuccess(int statusCode, Header[] headers, byte[] response) {
//                        // called when response HTTP status is "200 OK"
//                    }
//
//                    @Override
//                    public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
//                        // called when response HTTP status is "4XX" (eg. 401, 403, 404)
//                    }
//
//                    @Override
//                    public void onRetry(int retryNo) {
//                        // called when request is retried
//                    }
//                });
    }
}
