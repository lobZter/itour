package nctu.cs.cgv.itour.object;

import android.util.Log;
import android.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.fill;

/**
 * Created by lobst3rd on 2017/7/7.
 */

public class EdgeNode {

    private int vertexNumber = 0;
    private float edgeRatioMin = 0;
    private float headStdX = 0;
    private float headStdY = 0;
    private float tailStdX = 0;
    private float tailStdY = 0;
    private float edgePixelLengthStd = 0;
    private float edgeRealLengthStd = 0;

    private List<Float> edgeList;
    private List<ImageNode> nodeList;
    private Map<Vertex, Integer> vertexIdx;
    private List<Float> pathEdgeList;
    private List<ImageNode> pathNodeList;
    private Vertex[] vertices;
    private float[][] edgeRealWeights;
    private float[][] edgePixelWeights;
    private int vertexNum = 68;

    public EdgeNode(File edgeFile) {
        edgeList = new ArrayList<>();
        nodeList = new ArrayList<>();
        pathEdgeList = new ArrayList<>();
        pathNodeList = new ArrayList<>();
        initEdge(edgeFile);
    }

    private void initEdge(File edgeFile) {

        edgeList.clear();
        try {
            FileInputStream inputStream = new FileInputStream(edgeFile);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            // first line
            String nextLine = bufferedReader.readLine();
            vertexNumber = Integer.valueOf(nextLine);
            nextLine = bufferedReader.readLine();
            edgeRatioMin = Float.valueOf(nextLine);
            nextLine = bufferedReader.readLine();
            headStdX = Float.valueOf(nextLine);
            nextLine = bufferedReader.readLine();
            headStdY = Float.valueOf(nextLine);
            nextLine = bufferedReader.readLine();
            tailStdX = Float.valueOf(nextLine);
            nextLine = bufferedReader.readLine();
            tailStdY = Float.valueOf(nextLine);
            nextLine = bufferedReader.readLine();
            edgePixelLengthStd = Float.valueOf(nextLine);
            nextLine = bufferedReader.readLine();
            edgeRealLengthStd = Float.valueOf(nextLine);

            vertices = new Vertex[vertexNumber];
            vertexIdx = new HashMap<>();
            edgeRealWeights = new float[vertexNumber][vertexNumber];
            edgePixelWeights = new float[vertexNumber][vertexNumber];
            for (int row = 0; row < vertexNumber; row++) {
                for (int col = 0; col < vertexNumber; col++) {
                    edgeRealWeights[row][col] = -1;
                    edgePixelWeights[row][col] = -1;
                }
            }

            int vIdx = 0;

            // read vertex positions
            for (int i = 0; i < vertexNumber / 2; i++) {
                nextLine = bufferedReader.readLine();
                float x1 = Float.valueOf(nextLine);
                nextLine = bufferedReader.readLine();
                float y1 = Float.valueOf(nextLine);
                nextLine = bufferedReader.readLine();
                float x2 = Float.valueOf(nextLine);
                nextLine = bufferedReader.readLine();
                float y2 = Float.valueOf(nextLine);
                nextLine = bufferedReader.readLine();
                float edgePixelLength = Float.valueOf(nextLine);
                nextLine = bufferedReader.readLine();
                float edgeRealLength = Float.valueOf(nextLine);

                edgeList.add(x1);
                edgeList.add(y1);
                edgeList.add(x2);
                edgeList.add(y2);
                edgeList.add(edgePixelLength);
                edgeList.add(edgeRealLength);
                Vertex v1 = new Vertex(x1, y1);
                Vertex v2 = new Vertex(x2, y2);
                if (!vertexIdx.containsKey(v1)) {
                    vertices[vIdx] = v1;
                    vertexIdx.put(v1, vIdx);
                    vIdx++;
                }
                if (!vertexIdx.containsKey(v2)) {
                    vertices[vIdx] = v2;
                    vertexIdx.put(v2, vIdx);
                    vIdx++;
                }
                edgeRealWeights[vertexIdx.get(v1)][vertexIdx.get(v2)] = edgeRealLength;
                edgePixelWeights[vertexIdx.get(v1)][vertexIdx.get(v2)] = edgePixelLength;
                edgeRealWeights[vertexIdx.get(v2)][vertexIdx.get(v1)] = edgeRealLength;
                edgePixelWeights[vertexIdx.get(v2)][vertexIdx.get(v1)] = edgePixelLength;
            }

            vertexNum = vIdx;

            bufferedReader.close();

        } catch (Exception e) {
            Log.d("debug", "Exception read file edge ...");
            e.printStackTrace();
        }

        initNode();
    }

    public Vertex findVertex(float x, float y) {

        double min = Double.MAX_VALUE;
        Vertex result = null;

        for (int i=0; i < vertexNum; i++) {
            Vertex v = vertices[i];
            double distance = Math.pow(v.first - x, 2) + Math.pow(v.second - y, 2);
            if (distance < min) {
                min = distance;
                result = v;
            }
        }
        return result;
    }

    public void shortestPath(Vertex v1, Vertex v2) {

        int v1Idx = vertexIdx.get(v1);
        int v2Idx = vertexIdx.get(v2);

        boolean[] visit = new boolean[vertexNum];
        float[] distance = new float[vertexNum];
        int[] prev = new int[vertexNum];

        fill(visit, Boolean.FALSE);
        fill(distance, Float.MAX_VALUE);
        fill(prev, -1);

        visit[v1Idx] = true;
        distance[v1Idx] = 0;
        prev[v1Idx] = v1Idx;

        for (int i = 0; i < vertexNum; i++) {
            if (edgeRealWeights[v1Idx][i] != -1f && distance[v1Idx] + edgeRealWeights[v1Idx][i] < distance[i]) {
                distance[i] = distance[v1Idx] + edgeRealWeights[v1Idx][i];
                prev[i] = v1Idx;
            }
        }

        for (int i = 0; i < vertexNum; i++) {
            float min = Float.MAX_VALUE;
            int idx = -1;
            for (int j = 0; j < vertexNum; j++) {
                if (!visit[j] && distance[j] < min) {
                    idx = j;
                    min = distance[j];
                }
            }
            if (idx == -1) break;

            for (int j = 0; j < vertexNum; j++) {
                if (edgeRealWeights[idx][j] != -1f && distance[idx] + edgeRealWeights[idx][j] < distance[j]) {
                    distance[j] = distance[idx] + edgeRealWeights[idx][j];
                    prev[j] = idx;
                }
            }
            visit[idx] = true;
        }

        String str = "";

        pathEdgeList.clear();
        int idxIter = v2Idx;
        while (idxIter != v1Idx) {
            str += idxIter + " ";
            pathEdgeList.add(vertices[idxIter].first);
            pathEdgeList.add(vertices[idxIter].second);
            pathEdgeList.add(vertices[prev[idxIter]].first);
            pathEdgeList.add(vertices[prev[idxIter]].second);
            pathEdgeList.add(edgeRealWeights[idxIter][prev[idxIter]]);
            pathEdgeList.add(edgePixelWeights[idxIter][prev[idxIter]]);
            idxIter = prev[idxIter];
        }

        initPathNode();
    }

    private boolean initPathNode() {
        float x, y, x2, y2, edgePixelLength, edgeRealLength, distanceToAddorSubtractX, distanceToAddorSubtractY;

        float width = 16;
        float height = 16;

        pathNodeList.clear();

        float headStandardX = headStdX;
        float headStandardY = headStdY;
        float tailStandardX = tailStdX;
        float tailStandardY = tailStdY;
        float edgePixelLengthStandard = edgePixelLengthStd;
        float edgeRealLengthStandard = edgeRealLengthStd;

        float distanceRatioStandard = edgePixelLengthStandard / (1.2f * width);
        float distanceVectorXStandard = Math.abs((headStandardX - tailStandardX));
        float distanceVectorYStandard = Math.abs((headStandardY - tailStandardY));
        float distanceToAddorSubtractXStandard = distanceVectorXStandard / distanceRatioStandard;
        float distanceToAddorSubtractYStandard = distanceVectorYStandard / distanceRatioStandard;
        float distanceRatioRealLength = distanceRatioStandard * edgeRealLengthStandard / edgePixelLengthStandard;
        //Log.i("TAG", "PointF standard: " + distanceRatioRealLength + "," + distanceRatioStandard + "," + distanceVectorXStandard + "," + distanceVectorYStandard + "," + distanceToAddorSubtractXStandard + "," + distanceToAddorSubtractYStandard);

        for (int i = 0; i < pathEdgeList.size(); i += 6) {
            x = pathEdgeList.get(i);
            y = pathEdgeList.get(i + 1);
            x2 = pathEdgeList.get(i + 2);
            y2 = pathEdgeList.get(i + 3);
            edgePixelLength = pathEdgeList.get(i + 4);
            edgeRealLength = pathEdgeList.get(i + 5);

            float distanceRatio = (distanceRatioRealLength * edgePixelLength / edgeRealLength);
            float distanceVectorX = Math.abs(x - x2);
            float distanceVectorY = Math.abs(y - y2);
//            distanceToAddorSubtractX = distanceVectorX / (edgePixelLength / distanceRatio);
//            distanceToAddorSubtractY = distanceVectorY / (edgePixelLength / distanceRatio);
            if (distanceVectorX > 80 || distanceVectorY > 80) distanceRatio = distanceRatio * 10;

            distanceToAddorSubtractX = (distanceVectorX / distanceRatio);
            distanceToAddorSubtractY = (distanceVectorY / distanceRatio);
            //Log.i("TAG", "PointF normal: " + distanceRatio + "," + distanceVectorX + "," + distanceVectorY + "," + distanceToAddorSubtractX + "," + distanceToAddorSubtractY);
            pathNodeList.add(new ImageNode(x, y));

            if ((x == headStandardX && y == headStandardY && x2 == tailStandardX && y2 == tailStandardY) || (x2 == headStandardX && y2 == headStandardY && x == tailStandardX && y == tailStandardY)) {
                if (headStandardX >= tailStandardX && headStandardY >= tailStandardY) {
                    while (headStandardY - distanceToAddorSubtractYStandard >= tailStandardY && headStandardX - distanceToAddorSubtractXStandard >= tailStandardX) {
                        pathNodeList.add(new ImageNode(headStandardX - distanceToAddorSubtractXStandard, headStandardY - distanceToAddorSubtractYStandard));
                        headStandardX -= distanceToAddorSubtractXStandard;
                        headStandardY -= distanceToAddorSubtractYStandard;
                    }
                } else if (headStandardX >= tailStandardX && headStandardY <= tailStandardY) {
                    while (headStandardY + distanceToAddorSubtractYStandard <= tailStandardY && headStandardX - distanceToAddorSubtractXStandard >= tailStandardX) {
                        pathNodeList.add(new ImageNode(headStandardX - distanceToAddorSubtractXStandard, headStandardY + distanceToAddorSubtractYStandard));
                        headStandardX -= distanceToAddorSubtractXStandard;
                        headStandardY += distanceToAddorSubtractYStandard;
                    }
                } else if (headStandardX <= tailStandardX && headStandardY <= tailStandardY) {
                    while (headStandardY + distanceToAddorSubtractYStandard <= tailStandardY && headStandardX + distanceToAddorSubtractXStandard <= tailStandardX) {
                        pathNodeList.add(new ImageNode(headStandardX + distanceToAddorSubtractXStandard, headStandardY + distanceToAddorSubtractYStandard));
                        headStandardX += distanceToAddorSubtractXStandard;
                        headStandardY += distanceToAddorSubtractYStandard;
                    }
                } else if (headStandardX <= tailStandardX && headStandardY >= tailStandardY) {
                    while (headStandardY - distanceToAddorSubtractYStandard >= tailStandardY && headStandardX + distanceToAddorSubtractXStandard <= tailStandardX) {
                        pathNodeList.add(new ImageNode(headStandardX + distanceToAddorSubtractXStandard, headStandardY - distanceToAddorSubtractYStandard));
                        headStandardX += distanceToAddorSubtractXStandard;
                        headStandardY -= distanceToAddorSubtractYStandard;
                    }
                }
            } else {
                if (x >= x2 && y >= y2) {
                    while ((distanceToAddorSubtractY > distanceToAddorSubtractX && distanceToAddorSubtractY < (width)) || (distanceToAddorSubtractX > distanceToAddorSubtractY && distanceToAddorSubtractX < (width))) {
                        distanceToAddorSubtractY *= 2;
                        distanceToAddorSubtractX *= 2;
                    }
//                    if(distanceVectorX%distanceToAddorSubtractX!=0)distanceToAddorSubtractX+=distanceVectorX%distanceToAddorSubtractX;
//                    if(distanceVectorY%distanceToAddorSubtractY!=0)distanceToAddorSubtractY+=distanceVectorY%distanceToAddorSubtractY;
                    while (y - distanceToAddorSubtractY >= y2 && x - distanceToAddorSubtractX >= x2) {
                        if (((y - distanceToAddorSubtractY) - y2 < (width)) && ((x - distanceToAddorSubtractX) - x2 < (width))) {
                            x -= distanceToAddorSubtractX;
                            y -= distanceToAddorSubtractY;
                        } else {
                            pathNodeList.add(new ImageNode(x - distanceToAddorSubtractX, y - distanceToAddorSubtractY));
                            x -= distanceToAddorSubtractX;
                            y -= distanceToAddorSubtractY;
                        }
                    }
                } else if (x >= x2 && y <= y2) {
                    while ((distanceToAddorSubtractY > distanceToAddorSubtractX && distanceToAddorSubtractY < (width)) || (distanceToAddorSubtractX > distanceToAddorSubtractY && distanceToAddorSubtractX < (width))) {
                        distanceToAddorSubtractY *= 2;
                        distanceToAddorSubtractX *= 2;
                    }
//                    if(distanceVectorX%distanceToAddorSubtractX!=0)distanceToAddorSubtractX+=distanceVectorX%distanceToAddorSubtractX;
//                    if(distanceVectorY%distanceToAddorSubtractY!=0)distanceToAddorSubtractY+=distanceVectorY%distanceToAddorSubtractY;
                    while (y + distanceToAddorSubtractY <= y2 && x - distanceToAddorSubtractX >= x2) {
                        if ((y2 - (y + distanceToAddorSubtractY) < (width)) && ((x - distanceToAddorSubtractX) - x2 < (width))) {

                            x -= distanceToAddorSubtractX;
                            y += distanceToAddorSubtractY;
                        } else {
                            pathNodeList.add(new ImageNode(x - distanceToAddorSubtractX, y + distanceToAddorSubtractY));
                            x -= distanceToAddorSubtractX;
                            y += distanceToAddorSubtractY;
                        }
                    }
                } else if (x <= x2 && y <= y2) {
                    while ((distanceToAddorSubtractY > distanceToAddorSubtractX && distanceToAddorSubtractY < (width)) || (distanceToAddorSubtractX > distanceToAddorSubtractY && distanceToAddorSubtractX < (width))) {
                        distanceToAddorSubtractY *= 2;
                        distanceToAddorSubtractX *= 2;
                    }
                    //Log.i("TAG", "AddSub: " + distanceVectorX/distanceToAddorSubtractX +"," + distanceVectorX%distanceToAddorSubtractX  + "," + distanceVectorY/distanceToAddorSubtractY + "," + distanceVectorY%distanceToAddorSubtractY);
//                    if(distanceVectorX%distanceToAddorSubtractX!=0)distanceToAddorSubtractX+=distanceVectorX%distanceToAddorSubtractX;
//                    if(distanceVectorY%distanceToAddorSubtractY!=0)distanceToAddorSubtractY+=distanceVectorY%distanceToAddorSubtractY;
                    while (y + distanceToAddorSubtractY <= y2 && x + distanceToAddorSubtractX <= x2) {
                        if ((y2 - (y + distanceToAddorSubtractY) < (width)) && (x2 - (x + distanceToAddorSubtractX) < (width))) {

                            x += distanceToAddorSubtractX;
                            y += distanceToAddorSubtractY;
                        } else {
                            pathNodeList.add(new ImageNode(x + distanceToAddorSubtractX, y + distanceToAddorSubtractY));
                            x += distanceToAddorSubtractX;
                            y += distanceToAddorSubtractY;
                        }
                    }
                } else if (x <= x2 && y >= y2) {
                    while ((distanceToAddorSubtractY > distanceToAddorSubtractX && distanceToAddorSubtractY < (width)) || (distanceToAddorSubtractX > distanceToAddorSubtractY && distanceToAddorSubtractX < (width))) {
                        distanceToAddorSubtractY *= 2;
                        distanceToAddorSubtractX *= 2;
                    }
//                    if(distanceVectorX%distanceToAddorSubtractX!=0)distanceToAddorSubtractX+=distanceVectorX%distanceToAddorSubtractX;
//                    if(distanceVectorY%distanceToAddorSubtractY!=0)distanceToAddorSubtractY+=distanceVectorY%distanceToAddorSubtractY;
                    while (y - distanceToAddorSubtractY >= y2 && x + distanceToAddorSubtractX <= x2) {
//                        if(y- (y - distanceToAddorSubtractY) <width && (x + distanceToAddorSubtractX) - x < width) {
////                        if((y - distanceToAddorSubtractY + (width/2) > y-(width/2))&&(x + distanceToAddorSubtractX - (width/2) < x+(width/2))  ) {

                        if (((y - distanceToAddorSubtractY) - y2 < (width)) && (x2 - (x + distanceToAddorSubtractX) < (width))) {

                            x += distanceToAddorSubtractX;
                            y -= distanceToAddorSubtractY;
                        } else {
                            pathNodeList.add(new ImageNode(x + distanceToAddorSubtractX, y - distanceToAddorSubtractY));
                            x += distanceToAddorSubtractX;
                            y -= distanceToAddorSubtractY;
                        }
                    }
                }
            }

        }
        return true;
    }

    private boolean initNode() {
        float x, y, x2, y2, edgePixelLength, edgeRealLength, distanceToAddorSubtractX, distanceToAddorSubtractY;
        float mScaleFactor = 1f;

//        if (scale <= 1) mScaleFactor = 2f;
//        else if (scale > 1 && scale <= 1.3)
//            mScaleFactor = 2.3f;
//        else if (scale > 1.3 && scale <= 1.8)
//            mScaleFactor = 2.6f;
//        else if (scale > 1.8 && scale <= 2.7)
//            mScaleFactor = 3.2f;
//        else if (scale > 2.7 && scale <= 3)
//            mScaleFactor = 3.8f;
//        else if (scale > 3 && scale <= 3.5)
//            mScaleFactor = 4.3f;
//        else if (scale > 3.5 && scale <= 3.9)
//            mScaleFactor = 4.8f;
//        else if (scale > 3.9 && scale <= 4.3)
//            mScaleFactor = 5.3f;
//        else if (scale > 4.3 && scale <= 5)
//            mScaleFactor = 5.9f;
//        else if (scale > 5 && scale <= 5.5)
//            mScaleFactor = 6.1f;
//        else if (scale > 5.5 && scale <= 6)
//            mScaleFactor = 6.6f;
//        else if (scale > 6 && scale <= 6.5)
//            mScaleFactor = 7.1f;
//        else if (scale > 6.5 && scale <= 7)
//            mScaleFactor = 7.6f;
//        else if (scale > 7 && scale <= 7.5)
//            mScaleFactor = 8.3f;
//        else if (scale > 7.5) mScaleFactor = 8.3f;

//        float width = 36f / mScaleFactor;
//        float height = 36f / mScaleFactor;
        float width = 16;
        float height = 16;

        nodeList.clear();

        float headStandardX = headStdX;
        float headStandardY = headStdY;
        float tailStandardX = tailStdX;
        float tailStandardY = tailStdY;
        float edgePixelLengthStandard = edgePixelLengthStd;
        float edgeRealLengthStandard = edgeRealLengthStd;

        float distanceRatioStandard = edgePixelLengthStandard / (1.2f * width);
        float distanceVectorXStandard = Math.abs((headStandardX - tailStandardX));
        float distanceVectorYStandard = Math.abs((headStandardY - tailStandardY));
        float distanceToAddorSubtractXStandard = distanceVectorXStandard / distanceRatioStandard;
        float distanceToAddorSubtractYStandard = distanceVectorYStandard / distanceRatioStandard;
        float distanceRatioRealLength = distanceRatioStandard * edgeRealLengthStandard / edgePixelLengthStandard;
        //Log.i("TAG", "PointF standard: " + distanceRatioRealLength + "," + distanceRatioStandard + "," + distanceVectorXStandard + "," + distanceVectorYStandard + "," + distanceToAddorSubtractXStandard + "," + distanceToAddorSubtractYStandard);

        for (int i = 0; i < edgeList.size(); i += 6) {
            x = edgeList.get(i);
            y = edgeList.get(i + 1);
            x2 = edgeList.get(i + 2);
            y2 = edgeList.get(i + 3);
            edgePixelLength = edgeList.get(i + 4);
            edgeRealLength = edgeList.get(i + 5);

            float distanceRatio = (distanceRatioRealLength * edgePixelLength / edgeRealLength);
            float distanceVectorX = Math.abs(x - x2);
            float distanceVectorY = Math.abs(y - y2);
//            distanceToAddorSubtractX = distanceVectorX / (edgePixelLength / distanceRatio);
//            distanceToAddorSubtractY = distanceVectorY / (edgePixelLength / distanceRatio);
            if (distanceVectorX > 80 || distanceVectorY > 80) distanceRatio = distanceRatio * 10;

            distanceToAddorSubtractX = (distanceVectorX / distanceRatio);
            distanceToAddorSubtractY = (distanceVectorY / distanceRatio);
            //Log.i("TAG", "PointF normal: " + distanceRatio + "," + distanceVectorX + "," + distanceVectorY + "," + distanceToAddorSubtractX + "," + distanceToAddorSubtractY);
            nodeList.add(new ImageNode(x, y));

            if ((x == headStandardX && y == headStandardY && x2 == tailStandardX && y2 == tailStandardY) || (x2 == headStandardX && y2 == headStandardY && x == tailStandardX && y == tailStandardY)) {
                if (headStandardX >= tailStandardX && headStandardY >= tailStandardY) {
                    while (headStandardY - distanceToAddorSubtractYStandard >= tailStandardY && headStandardX - distanceToAddorSubtractXStandard >= tailStandardX) {
                        nodeList.add(new ImageNode(headStandardX - distanceToAddorSubtractXStandard, headStandardY - distanceToAddorSubtractYStandard));
                        headStandardX -= distanceToAddorSubtractXStandard;
                        headStandardY -= distanceToAddorSubtractYStandard;
                    }
                } else if (headStandardX >= tailStandardX && headStandardY <= tailStandardY) {
                    while (headStandardY + distanceToAddorSubtractYStandard <= tailStandardY && headStandardX - distanceToAddorSubtractXStandard >= tailStandardX) {
                        nodeList.add(new ImageNode(headStandardX - distanceToAddorSubtractXStandard, headStandardY + distanceToAddorSubtractYStandard));
                        headStandardX -= distanceToAddorSubtractXStandard;
                        headStandardY += distanceToAddorSubtractYStandard;
                    }
                } else if (headStandardX <= tailStandardX && headStandardY <= tailStandardY) {
                    while (headStandardY + distanceToAddorSubtractYStandard <= tailStandardY && headStandardX + distanceToAddorSubtractXStandard <= tailStandardX) {
                        nodeList.add(new ImageNode(headStandardX + distanceToAddorSubtractXStandard, headStandardY + distanceToAddorSubtractYStandard));
                        headStandardX += distanceToAddorSubtractXStandard;
                        headStandardY += distanceToAddorSubtractYStandard;
                    }
                } else if (headStandardX <= tailStandardX && headStandardY >= tailStandardY) {
                    while (headStandardY - distanceToAddorSubtractYStandard >= tailStandardY && headStandardX + distanceToAddorSubtractXStandard <= tailStandardX) {
                        nodeList.add(new ImageNode(headStandardX + distanceToAddorSubtractXStandard, headStandardY - distanceToAddorSubtractYStandard));
                        headStandardX += distanceToAddorSubtractXStandard;
                        headStandardY -= distanceToAddorSubtractYStandard;
                    }
                }
            } else {
                if (x >= x2 && y >= y2) {
                    while ((distanceToAddorSubtractY > distanceToAddorSubtractX && distanceToAddorSubtractY < (width)) || (distanceToAddorSubtractX > distanceToAddorSubtractY && distanceToAddorSubtractX < (width))) {
                        distanceToAddorSubtractY *= 2;
                        distanceToAddorSubtractX *= 2;
                    }
//                    if(distanceVectorX%distanceToAddorSubtractX!=0)distanceToAddorSubtractX+=distanceVectorX%distanceToAddorSubtractX;
//                    if(distanceVectorY%distanceToAddorSubtractY!=0)distanceToAddorSubtractY+=distanceVectorY%distanceToAddorSubtractY;
                    while (y - distanceToAddorSubtractY >= y2 && x - distanceToAddorSubtractX >= x2) {
                        if (((y - distanceToAddorSubtractY) - y2 < (width)) && ((x - distanceToAddorSubtractX) - x2 < (width))) {
                            x -= distanceToAddorSubtractX;
                            y -= distanceToAddorSubtractY;
                        } else {
                            nodeList.add(new ImageNode(x - distanceToAddorSubtractX, y - distanceToAddorSubtractY));
                            x -= distanceToAddorSubtractX;
                            y -= distanceToAddorSubtractY;
                        }
                    }
                } else if (x >= x2 && y <= y2) {
                    while ((distanceToAddorSubtractY > distanceToAddorSubtractX && distanceToAddorSubtractY < (width)) || (distanceToAddorSubtractX > distanceToAddorSubtractY && distanceToAddorSubtractX < (width))) {
                        distanceToAddorSubtractY *= 2;
                        distanceToAddorSubtractX *= 2;
                    }
//                    if(distanceVectorX%distanceToAddorSubtractX!=0)distanceToAddorSubtractX+=distanceVectorX%distanceToAddorSubtractX;
//                    if(distanceVectorY%distanceToAddorSubtractY!=0)distanceToAddorSubtractY+=distanceVectorY%distanceToAddorSubtractY;
                    while (y + distanceToAddorSubtractY <= y2 && x - distanceToAddorSubtractX >= x2) {
                        if ((y2 - (y + distanceToAddorSubtractY) < (width)) && ((x - distanceToAddorSubtractX) - x2 < (width))) {

                            x -= distanceToAddorSubtractX;
                            y += distanceToAddorSubtractY;
                        } else {
                            nodeList.add(new ImageNode(x - distanceToAddorSubtractX, y + distanceToAddorSubtractY));
                            x -= distanceToAddorSubtractX;
                            y += distanceToAddorSubtractY;
                        }
                    }
                } else if (x <= x2 && y <= y2) {
                    while ((distanceToAddorSubtractY > distanceToAddorSubtractX && distanceToAddorSubtractY < (width)) || (distanceToAddorSubtractX > distanceToAddorSubtractY && distanceToAddorSubtractX < (width))) {
                        distanceToAddorSubtractY *= 2;
                        distanceToAddorSubtractX *= 2;
                    }
                    //Log.i("TAG", "AddSub: " + distanceVectorX/distanceToAddorSubtractX +"," + distanceVectorX%distanceToAddorSubtractX  + "," + distanceVectorY/distanceToAddorSubtractY + "," + distanceVectorY%distanceToAddorSubtractY);
//                    if(distanceVectorX%distanceToAddorSubtractX!=0)distanceToAddorSubtractX+=distanceVectorX%distanceToAddorSubtractX;
//                    if(distanceVectorY%distanceToAddorSubtractY!=0)distanceToAddorSubtractY+=distanceVectorY%distanceToAddorSubtractY;
                    while (y + distanceToAddorSubtractY <= y2 && x + distanceToAddorSubtractX <= x2) {
                        if ((y2 - (y + distanceToAddorSubtractY) < (width)) && (x2 - (x + distanceToAddorSubtractX) < (width))) {

                            x += distanceToAddorSubtractX;
                            y += distanceToAddorSubtractY;
                        } else {
                            nodeList.add(new ImageNode(x + distanceToAddorSubtractX, y + distanceToAddorSubtractY));
                            x += distanceToAddorSubtractX;
                            y += distanceToAddorSubtractY;
                        }
                    }
                } else if (x <= x2 && y >= y2) {
                    while ((distanceToAddorSubtractY > distanceToAddorSubtractX && distanceToAddorSubtractY < (width)) || (distanceToAddorSubtractX > distanceToAddorSubtractY && distanceToAddorSubtractX < (width))) {
                        distanceToAddorSubtractY *= 2;
                        distanceToAddorSubtractX *= 2;
                    }
//                    if(distanceVectorX%distanceToAddorSubtractX!=0)distanceToAddorSubtractX+=distanceVectorX%distanceToAddorSubtractX;
//                    if(distanceVectorY%distanceToAddorSubtractY!=0)distanceToAddorSubtractY+=distanceVectorY%distanceToAddorSubtractY;
                    while (y - distanceToAddorSubtractY >= y2 && x + distanceToAddorSubtractX <= x2) {
//                        if(y- (y - distanceToAddorSubtractY) <width && (x + distanceToAddorSubtractX) - x < width) {
////                        if((y - distanceToAddorSubtractY + (width/2) > y-(width/2))&&(x + distanceToAddorSubtractX - (width/2) < x+(width/2))  ) {

                        if (((y - distanceToAddorSubtractY) - y2 < (width)) && (x2 - (x + distanceToAddorSubtractX) < (width))) {

                            x += distanceToAddorSubtractX;
                            y -= distanceToAddorSubtractY;
                        } else {
                            nodeList.add(new ImageNode(x + distanceToAddorSubtractX, y - distanceToAddorSubtractY));
                            x += distanceToAddorSubtractX;
                            y -= distanceToAddorSubtractY;
                        }
                    }
                }
            }

        }
        return true;
    }

    public List<Float> getEdgeList() {
        return edgeList;
    }

    public List<ImageNode> getNodeList() {
        return nodeList;
    }

    public List<Float> getPathEdgeList() {
        return pathEdgeList;
    }

    public List<ImageNode> getPathNodeList() {
        return pathNodeList;
    }

    public class Vertex extends Pair<Float, Float> {

        public Vertex(Float first, Float second) {
            super(first, second);
        }
    }

    public class Edge extends Pair<Vertex, Vertex> {

        public float weight;

        public Edge(Vertex first, Vertex second, float weight) {
            super(first, second);
            this.weight = weight;
        }
    }

    public class NeighborVertex {

        public Vertex neighbor;
        public float weight;

        public NeighborVertex(Vertex neighbor, float weight) {
            this.neighbor = neighbor;
            this.weight = weight;
        }

    }

}


