package nctu.cs.cgv.itour.object;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

public class Mesh {
    // world coordinates bounding box
    public float minLat = 0;
    public float minLon = 0;
    public float maxLat = 0;
    public float maxLon = 0;
    //real-osm-map size
    public float mapWidth = 0;
    public float mapHeight = 0;
    // element count
    int vertexNumber;
    int faceNumber;
    int lineNumber;
    // x- and y- coordinates of nodeMap
    float[][] vertices;

    // face connectivity
    int[][] faces;

    // constructor
    public Mesh(File meshFile) {
        readMeshFile(meshFile);
    }

    private void readMeshFile(File meshFile) {

        try {
            FileInputStream inputStream = new FileInputStream(meshFile);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            // skip first line
            String nextLine = bufferedReader.readLine();

            // the second line contains size information
            nextLine = bufferedReader.readLine();
            StringTokenizer stringTokenizer = new StringTokenizer(nextLine);
            vertexNumber = Integer.valueOf(stringTokenizer.nextToken());
            faceNumber = Integer.valueOf(stringTokenizer.nextToken());
            lineNumber = Integer.valueOf(stringTokenizer.nextToken());

            // get arrayLists
            vertices = new float[vertexNumber][2];
            faces = new int[faceNumber][3];

            // read vertex positions
            for (int vIter = 0; vIter < vertexNumber; vIter++) {
                nextLine = bufferedReader.readLine();

                stringTokenizer = new StringTokenizer(nextLine);
                float x = Float.valueOf(stringTokenizer.nextToken());
                float y = Float.valueOf(stringTokenizer.nextToken());

                vertices[vIter][0] = x;
                vertices[vIter][1] = y;
            }

            // read face indices
            for (int fIter = 0; fIter < faceNumber; fIter++) {
                nextLine = bufferedReader.readLine();

                stringTokenizer = new StringTokenizer(nextLine);
                int vNum = Integer.valueOf(stringTokenizer.nextToken()); // throw away :P
                int v1 = Integer.valueOf(stringTokenizer.nextToken());
                int v2 = Integer.valueOf(stringTokenizer.nextToken());
                int v3 = Integer.valueOf(stringTokenizer.nextToken());

                faces[fIter][0] = v1;
                faces[fIter][1] = v2;
                faces[fIter][2] = v3;
            }

            bufferedReader.close();

        } catch (Exception e) {
            Log.d("debug", "Exception...");
        }

    }

    public void readBoundingBox(File boundBoxFile) {
        try {
            FileInputStream inputStream = new FileInputStream(boundBoxFile);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            // skip first line
            String nextLine = bufferedReader.readLine();

            // in the order of minlat -> minlon -> maxlat -> maxlon ->mapWidth ->mapHeight
            nextLine = bufferedReader.readLine();
            minLat = Float.valueOf(nextLine);

            nextLine = bufferedReader.readLine();
            minLon = Float.valueOf(nextLine);

            nextLine = bufferedReader.readLine();
            maxLat = Float.valueOf(nextLine);

            nextLine = bufferedReader.readLine();
            maxLon = Float.valueOf(nextLine);

            nextLine = bufferedReader.readLine();
            mapWidth = Float.valueOf(nextLine);

            nextLine = bufferedReader.readLine();
            mapHeight = Float.valueOf(nextLine);

            bufferedReader.close();

        } catch (Exception e) {
            Log.d("debug", "Exception...");
        }
    }

    public IdxWeights getPointInTriangleIdx(float px, float py) {

        IdxWeights result = new IdxWeights();

        for (int fIter = 0; fIter < faceNumber; fIter++) {
            int id1 = faces[fIter][0];
            int id2 = faces[fIter][1];
            int id3 = faces[fIter][2];

            float x1 = vertices[id1][0];
            float x2 = vertices[id2][0];
            float x3 = vertices[id3][0];
            float y1 = vertices[id1][1];
            float y2 = vertices[id2][1];
            float y3 = vertices[id3][1];

            float l1 = ((y2 - y3) * (px - x3) + (x3 - x2) * (py - y3)) / ((y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3));
            float l2 = ((y3 - y1) * (px - x3) + (x1 - x3) * (py - y3)) / ((y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3));
            float l3 = 1.0f - l1 - l2;

            if (l1 < 0 && l1 > -0.0001) l1 = 0f;
            if (l2 < 0 && l2 > -0.0001) l2 = 0f;
            if (l3 < 0 && l3 > -0.0001) l3 = 0f;
            if (l1 > 1 && l1 < 1.0001) l1 = 1f;
            if (l2 > 1 && l2 < 1.0001) l2 = 1f;
            if (l3 > 1 && l3 < 1.0001) l3 = 1f;

            if (l1 >= 0 && l1 <= 1 && l2 >= 0 && l2 <= 1 && l3 >= 0 && l3 <= 1) {
                result = new IdxWeights(fIter, l1, l2, l3);
                break;
            }
        }

        return result;
    }

    public float[] interpolatePosition(IdxWeights idxWeights) {
        int id = idxWeights.idx;
        int triId1 = faces[id][0];
        int triId2 = faces[id][1];
        int triId3 = faces[id][2];
        float[] weights = idxWeights.weights;

        float x = weights[0] * vertices[triId1][0] + weights[1] * vertices[triId2][0] + weights[2] * vertices[triId3][0];
        float y = weights[0] * vertices[triId1][1] + weights[1] * vertices[triId2][1] + weights[2] * vertices[triId3][1];

        return new float[]{x, y};
    }
}