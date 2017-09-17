package nctu.cs.cgv.itour.object;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static nctu.cs.cgv.itour.Utility.gpsToImgPx;
import static nctu.cs.cgv.itour.Utility.lerp;

/**
 * Created by lobZter on 2017/8/15.
 */

public class SpotList {

    public Map<String, SpotNode> spotNodeMap;
    public int primarySpotMaxIdx = 13;
    public int length;

    public SpotList(File spotListFile, Mesh realMesh, Mesh warpMesh) {
        length = 0;
        spotNodeMap = new LinkedHashMap<>();
        readSpotsFile(spotListFile, realMesh, warpMesh);
    }

    private void readSpotsFile(File spotListFile, Mesh realMesh, Mesh warpMesh) {

        try {
            FileInputStream inputStream = new FileInputStream(spotListFile);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                String[] arr = line.split(","); // name,lat,lng
                float[] imgPx = gpsToImgPx(realMesh, warpMesh, Float.valueOf(arr[1]), Float.valueOf(arr[2]));
                spotNodeMap.put(arr[0], new SpotNode(imgPx[0], imgPx[1], arr[0]));
                length++;
            }

            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getSpotsName() {
        return spotNodeMap.keySet();
    }

    public List<SpotNode> getSpotsList() {
        return new ArrayList<>(spotNodeMap.values());
    }
}
