package nctu.cs.cgv.itour;

import android.content.Context;
import android.graphics.Matrix;
import android.util.TypedValue;

import nctu.cs.cgv.itour.object.IdxWeights;
import nctu.cs.cgv.itour.object.Mesh;

/**
 * Created by lobZter on 2017/6/21.
 */

public class Utility {
    public static int dpToPx(Context context, int dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics()));
    }

    public static float[] gpsToImgPx(Mesh realMesh, Mesh warpMesh, float lat, float lng) {
        float latDistorted = 0;
        float lngDistorted = 0;
        double imgX = realMesh.mapWidth * (lng - realMesh.minLon) / (realMesh.maxLon - realMesh.minLon);
        double imgY = realMesh.mapHeight * (realMesh.maxLat - lat) / (realMesh.maxLat - realMesh.minLat);
        IdxWeights idxWeights = realMesh.getPointInTriangleIdx(imgX, imgY);
        if (idxWeights.idx >= 0) {
            double[] newPos = warpMesh.interpolatePosition(idxWeights);
            latDistorted = (float) newPos[0];
            lngDistorted = (float) newPos[1];
        }
        return new float[]{latDistorted, lngDistorted};
    }

    public static void screenPxToGps(Mesh realMesh, Mesh warpMesh, float screenPxX, float screenPxY, Matrix imgTransMat) {
    }

    public static void imgPxToGps(Mesh realMesh, Mesh warpMesh, float latDistorted, float lngDistorted) {
    }

}
