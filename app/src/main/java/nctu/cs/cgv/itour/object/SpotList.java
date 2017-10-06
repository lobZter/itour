package nctu.cs.cgv.itour.object;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
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

    public Map<String, Node> nodeMap;
    public int primarySpotMaxIdx = 13;

    public SpotList(File spotListFile, Mesh realMesh, Mesh warpMesh) {
        nodeMap = new LinkedHashMap<>();
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
                nodeMap.put(arr[0], new Node(imgPx[0], imgPx[1]));
            }

            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Set<String> getSpotsName() {
        return nodeMap.keySet();
    }

    public List<Node> getSpotsList() {
        return new ArrayList<>(nodeMap.values());
    }
}
