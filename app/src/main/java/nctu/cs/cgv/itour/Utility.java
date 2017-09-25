package nctu.cs.cgv.itour;

import android.app.Activity;
import android.content.Context;
import android.graphics.Matrix;
import android.util.TypedValue;
import android.view.inputmethod.InputMethodManager;

import nctu.cs.cgv.itour.object.IdxWeights;
import nctu.cs.cgv.itour.object.Mesh;

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

    public static float[] gpsToImgPx(Mesh realMesh, Mesh warpMesh, float lat, float lng) {
        float[] warpMeshPos = new float[]{0, 0};
        float realMeshX = realMesh.mapWidth * (lng - realMesh.minLon) / (realMesh.maxLon - realMesh.minLon);
        float realMeshY = realMesh.mapHeight * (realMesh.maxLat - lat) / (realMesh.maxLat - realMesh.minLat);
        IdxWeights idxWeights = realMesh.getPointInTriangleIdx(realMeshX, realMeshY);
        if (idxWeights.idx >= 0) {
            warpMeshPos = warpMesh.interpolatePosition(idxWeights);
        }
        return warpMeshPos;
    }

    public static float[] imgPxToGps(Mesh realMesh, Mesh warpMesh, float imgX, float imgY) {
        float[] realMeshPos = new float[]{0, 0};
        IdxWeights idxWeights = warpMesh.getPointInTriangleIdx(imgX, imgY);
        if (idxWeights.idx >= 0) {
            realMeshPos = realMesh.interpolatePosition(idxWeights);
        }
        return realMeshPos;
    }

    public static void screenPxToGps(Mesh realMesh, Mesh warpMesh, float screenPxX, float screenPxY, Matrix imgTransMat) {
    }


    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }
}
