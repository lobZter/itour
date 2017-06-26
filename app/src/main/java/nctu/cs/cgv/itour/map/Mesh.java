package nctu.cs.cgv.itour.map;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * Created by lobZter on 2017/5/22.
 */

public class Mesh {
    // element count
    int vertexNumber;
    int faceNumber;
    int lineNumber;

    // world coordinates bounding box
    public double minLat = 0.0;
    public double minLon = 0.0;
    public double maxLat = 0.0;
    public double maxLon = 0.0;

    //real-osm-map size
    public double mapWidth = 0.0;
    public double mapHeight = 0.0;


    // x- and y- coordinates of nodes
    double[][] vertices;

    // face connectivity
    int[][] faces;

    // constructor
    public Mesh(File meshFile) {
        readMeshFile(meshFile);
    }

    public boolean readMeshFile(File meshFile) {

        try {
//            InputStream inputStream = context.getResources().openRawResource(resourceId);
            FileInputStream inputStream  = new FileInputStream(meshFile);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            // skip first line
            String nextLine = bufferedReader.readLine();

            // the second line contains size information
            nextLine = bufferedReader.readLine();
            StringTokenizer st = new StringTokenizer(nextLine);
            vertexNumber = Integer.valueOf( st.nextToken() );
            faceNumber = Integer.valueOf( st.nextToken() );
            lineNumber = Integer.valueOf( st.nextToken() );

            // get arrayLists
            vertices = new double[vertexNumber][2];
            faces = new int[faceNumber][3];

            // read vertex positions
            for(int vIter=0; vIter<vertexNumber; vIter++){
                nextLine = bufferedReader.readLine();

                st = new StringTokenizer(nextLine);
                double x = Double.valueOf( st.nextToken() );
                double y = Double.valueOf( st.nextToken() );

                vertices[vIter][0] = x;
                vertices[vIter][1] = y;
            }

            // read face indices
            for(int fIter=0; fIter<faceNumber; fIter++){
                nextLine = bufferedReader.readLine();

                st = new StringTokenizer(nextLine);
                int vNum = Integer.valueOf( st.nextToken() ); // throw away :P
                int v1 = Integer.valueOf( st.nextToken() );
                int v2 = Integer.valueOf( st.nextToken() );
                int v3 = Integer.valueOf( st.nextToken() );

                faces[fIter][0] = v1;
                faces[fIter][1] = v2;
                faces[fIter][2] = v3;
            }

            bufferedReader.close();

        } catch (Exception e) {
            Log.d("debug", "Exception...");
            return false;
        }

        return true;
    }

    public void scaleByBoundingBox(File boundBoxFile) {
        readBoundingBox(boundBoxFile);

        for(int vIter=0; vIter<vertexNumber; vIter++) {
            double tx = vertices[vIter][0];
            double ty = vertices[vIter][1];

            double new_x = minLon + tx*(maxLon - minLon);
            double new_y = maxLat + ty*(minLat - maxLat);
            //double new_y = minLat + ty*(maxLat - minLat);

            vertices[vIter][0] = new_x;
            vertices[vIter][1] = new_y;
        }
    }

    public boolean readBoundingBox(File boundBoxFile ) {
        try {
//            InputStream inputStream = context.getResources().openRawResource(resourceId);
            FileInputStream inputStream  = new FileInputStream(boundBoxFile);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            // skip first line
            String nextLine = bufferedReader.readLine();

            // in the order of minlat -> minlon -> maxlat -> maxlon ->mapWidth ->mapHeight
            nextLine = bufferedReader.readLine();
            minLat = Double.valueOf(nextLine);

            nextLine = bufferedReader.readLine();
            minLon = Double.valueOf(nextLine);

            nextLine = bufferedReader.readLine();
            maxLat = Double.valueOf(nextLine);

            nextLine = bufferedReader.readLine();
            maxLon = Double.valueOf(nextLine);

            nextLine = bufferedReader.readLine();
            mapWidth = Double.valueOf(nextLine);

            nextLine = bufferedReader.readLine();
            mapHeight = Double.valueOf(nextLine);

            bufferedReader.close();

        } catch (Exception e) {
            Log.d("debug", "Exception...");
            return false;
        }

        return true;
    }

    public IdxWeights getPointInTriangleIdx(double px, double py) {

        IdxWeights result = new IdxWeights();

        for(int fIter=0; fIter<faceNumber; fIter++) {
            int id1 = faces[fIter][0];
            int id2 = faces[fIter][1];
            int id3 = faces[fIter][2];

            double x1 = vertices[id1][0];
            double x2 = vertices[id2][0];
            double x3 = vertices[id3][0];
            double y1 = vertices[id1][1];
            double y2 = vertices[id2][1];
            double y3 = vertices[id3][1];

            double l1 = ((y2-y3)*(px-x3) + (x3-x2)*(py-y3)) / ((y2-y3)*(x1-x3) + (x3-x2)*(y1-y3));
            double l2 = ((y3-y1)*(px-x3) + (x1-x3)*(py-y3)) / ((y2-y3)*(x1-x3) + (x3-x2)*(y1-y3));
            double l3 = 1.0 - l1 - l2;

            if(l1<0 && l1>-0.0001) l1 = 0.0;
            if(l2<0 && l2>-0.0001) l2 = 0.0;
            if(l3<0 && l3>-0.0001) l3 = 0.0;
            if(l1>1 && l1<1.0001) l1 = 1.0;
            if(l2>1 && l2<1.0001) l2 = 1.0;
            if(l3>1 && l3<1.0001) l3 = 1.0;

            if(l1>=0 && l1<=1 && l2>=0 && l2<=1 && l3>=0 && l3<=1)
            {
                result = new IdxWeights(fIter, l1, l2, l3);
                break;
            }
        }

        return result;
    }

    public double[] interpolatePosition(IdxWeights idxWeights) {
        int id = idxWeights.idx;
        int triId1 = faces[id][0];
        int triId2 = faces[id][1];
        int triId3 = faces[id][2];
        double[] weights = idxWeights.weights;

        double x = weights[0]*vertices[triId1][0] + weights[1]*vertices[triId2][0] + weights[2]*vertices[triId3][0];
        double y = weights[0]*vertices[triId1][1] + weights[1]*vertices[triId2][1] + weights[2]*vertices[triId3][1];

        double[] result = {x, y};
        return result;
    }
}