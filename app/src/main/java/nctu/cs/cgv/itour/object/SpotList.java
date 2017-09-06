package nctu.cs.cgv.itour.object;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static nctu.cs.cgv.itour.Utility.gpsToImgPx;

/**
 * Created by lobZter on 2017/8/15.
 */

public class SpotList {

    public Map<String, SpotNode> primarySpot;
    public Map<String, SpotNode> secondarySpot;

    public SpotList(File spotListFile, Mesh realMesh, Mesh warpMesh) {
        primarySpot = new HashMap<>();
        secondarySpot = new HashMap<>();
        readSpotsFile(spotListFile, realMesh, warpMesh);
    }

    private void readSpotsFile(File spotListFile, Mesh realMesh, Mesh warpMesh) {

        try {
            FileInputStream inputStream = new FileInputStream(spotListFile);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;

            for(int i=0; i<14; i++) {
                line = bufferedReader.readLine();
                String arr[] = line.split(",");
                float[] gpsDistorted = gpsToImgPx(realMesh, warpMesh, Float.valueOf(arr[1]), Float.valueOf(arr[2]));
                primarySpot.put(arr[0],
                        new SpotNode(gpsDistorted[0], gpsDistorted[1], arr[0]));
            }

            while ((line = bufferedReader.readLine()) != null) {
                String arr[] = line.split(",");
                float[] gpsDistorted = gpsToImgPx(realMesh, warpMesh, Float.valueOf(arr[1]), Float.valueOf(arr[2]));
                secondarySpot.put(arr[0],
                        new SpotNode(gpsDistorted[0], gpsDistorted[1], arr[0]));
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getSpots() {
        return primarySpot.keySet();
    }
}
