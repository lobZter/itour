package nctu.cs.cgv.itour;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.inputmethod.InputMethodManager;

import com.google.firebase.auth.FirebaseAuth;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.mime.content.InputStreamBody;
import nctu.cs.cgv.itour.object.IdxWeights;

import static nctu.cs.cgv.itour.MyApplication.APPServerURL;
import static nctu.cs.cgv.itour.MyApplication.logFlag;
import static nctu.cs.cgv.itour.MyApplication.realMesh;
import static nctu.cs.cgv.itour.MyApplication.warpMesh;

/**
 * Created by lobZter on 2017/6/21.
 */

public class Utility {

    private static final String TAG = "Utility";

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
        float realMeshX = realMesh.mapWidth * (lng - realMesh.minLon) / (realMesh.maxLon - realMesh.minLon);
        float realMeshY = realMesh.mapHeight * (realMesh.maxLat - lat) / (realMesh.maxLat - realMesh.minLat);
        IdxWeights idxWeights = realMesh.getPointInTriangleIdx(realMeshX, realMeshY);
        if (idxWeights.idx >= 0) {
            return warpMesh.interpolatePosition(idxWeights);
        } else {
            return new float[]{-1, -1};
        }
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
        if (!logFlag && FirebaseAuth.getInstance().getCurrentUser() == null)
            return;

        AsyncHttpClient client = new AsyncHttpClient();
        String url = APPServerURL + "/actionLog";
        RequestParams requestParams = new RequestParams();
        requestParams.put("log", log);
        requestParams.put("username", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        requestParams.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        requestParams.put("timestamp", String.valueOf(System.currentTimeMillis() / 1000));

        client.post(url, requestParams, new AsyncHttpResponseHandler() {

            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
    }

    public static void screenShotLog(Context context, final byte[] png) {
        if (!logFlag && FirebaseAuth.getInstance().getCurrentUser() == null)
            return;


        File output = new File(context.getExternalFilesDir(null), "screenshot.png");

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(output);
            fileOutputStream.write(png);
            fileOutputStream.flush();
            fileOutputStream.getFD().sync();
            fileOutputStream.close();
        } catch (Exception e) {
            Log.e(TAG, "Exception writing out screenshot", e);
        }

        // upload files to app server
        AsyncHttpClient client = new AsyncHttpClient();
        String url = APPServerURL + "/screenShotLog";
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        RequestParams requestParams = new RequestParams();
        requestParams.put("username", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        requestParams.put("uid", uid);
        requestParams.put("timestamp", timeStamp);
        requestParams.setForceMultipartEntityContentType(true);
//        requestParams.put("image", new InputStreamBody(new ByteArrayInputStream(png), uid + "-" + timeStamp + ".png"));
        try {
            requestParams.put("image", output);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        client.post(url, requestParams, new AsyncHttpResponseHandler() {

            @Override
            public void onStart() {
                // called before request is started
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] response) {
                // called when response HTTP status is "200 OK"
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] errorResponse, Throwable e) {
                // called when response HTTP status is "4XX" (eg. 401, 403, 404)
            }

            @Override
            public void onRetry(int retryNo) {
                // called when request is retried
            }
        });
    }
}
