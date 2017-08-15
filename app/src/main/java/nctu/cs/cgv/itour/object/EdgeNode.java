package nctu.cs.cgv.itour.object;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

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

    public EdgeNode(File edgeFile) {
        edgeList = new LinkedList<>();
        nodeList = new LinkedList<>();
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

            //Log.i("TAG", "vertex number: " + vertexNumber + "," + edgeRatioMin + "," + headStandardX + "," + headStandardY + "," + tailStandardX + "," + tailStandardY);

            // read vertex positions
            for (int i = 0, vIter = 0; i < vertexNumber / 2; i++, vIter += 2) {
                nextLine = bufferedReader.readLine();

                float x = Float.valueOf(nextLine);
                nextLine = bufferedReader.readLine();
                float y = Float.valueOf(nextLine);

                nextLine = bufferedReader.readLine();
                float x2 = Float.valueOf(nextLine);
                nextLine = bufferedReader.readLine();
                float y2 = Float.valueOf(nextLine);
                nextLine = bufferedReader.readLine();
                float edgePixelLength = Float.valueOf(nextLine);
                nextLine = bufferedReader.readLine();
                float edgeRealLength = Float.valueOf(nextLine);

                edgeList.add(x);
                edgeList.add(y);
                edgeList.add(x2);
                edgeList.add(y2);
                edgeList.add(edgePixelLength);
                edgeList.add(edgeRealLength);
            }

            bufferedReader.close();

        } catch (Exception e) {
            Log.d("debug", "Exception read file edge ...");
            e.printStackTrace();
        }

        initNode();
    }

//    private void initNode() {
//
//        float mScaleFactor = 1f;
////        if (mScaleFactor <= 1) mScaleFactor = 2f;
////        else if (mScaleFactor > 1 && mScaleFactor <= 1.3)
////            mScaleFactor = 2.3f;
////        else if (mScaleFactor > 1.3 && mScaleFactor <= 1.8)
////            mScaleFactor = 2.6f;
////        else if (mScaleFactor > 1.8 && mScaleFactor <= 2.7)
////            mScaleFactor = 3.2f;
////        else if (mScaleFactor > 2.7 && mScaleFactor <= 3)
////            mScaleFactor = 3.8f;
////        else if (mScaleFactor > 3 && mScaleFactor <= 3.5)
////            mScaleFactor = 4.3f;
////        else if (mScaleFactor > 3.5 && mScaleFactor <= 3.9)
////            mScaleFactor = 4.8f;
////        else if (mScaleFactor > 3.9 && mScaleFactor <= 4.3)
////            mScaleFactor = 5.3f;
////        else if (mScaleFactor > 4.3 && mScaleFactor <= 5)
////            mScaleFactor = 5.9f;
////        else if (mScaleFactor > 5 && mScaleFactor <= 5.5)
////            mScaleFactor = 6.1f;
////        else if (mScaleFactor > 5.5 && mScaleFactor <= 6)
////            mScaleFactor = 6.6f;
////        else if (mScaleFactor > 6 && mScaleFactor <= 6.5)
////            mScaleFactor = 7.1f;
////        else if (mScaleFactor > 6.5 && mScaleFactor <= 7)
////            mScaleFactor = 7.6f;
////        else if (mScaleFactor > 7 && mScaleFactor <= 7.5)
////            mScaleFactor = 8.3f;
////        else if (mScaleFactor > 7.5) mScaleFactor = 8.3f;
//
////        float width = 36f / mScaleFactor;
////        float height = 36f / mScaleFactor;
//        float width = 16;
//        float height = 16;
//
//        float x1, y1;
//        float x2, y2;
//        float edgePixelLength;
//        float edgeRealLength;
//        float distanceToAddOrSubtractX;
//        float distanceToAddOrSubtractY;
//        float distanceRatioStandard = edgePixelLengthStd / (1.2f * width);
//        float distanceVectorXStandard = Math.abs((headStdX - tailStdX));
//        float distanceVectorYStandard = Math.abs((headStdY - tailStdY));
//        float distanceToAddOrSubtractXStandard = distanceVectorXStandard / distanceRatioStandard;
//        float distanceToAddOrSubtractYStandard = distanceVectorYStandard / distanceRatioStandard;
//        float distanceRatioRealLength = distanceRatioStandard * edgeRealLengthStd / edgePixelLengthStd;
//        //Log.i("TAG", "PointF standard: " + distanceRatioRealLength + "," + distanceRatioStandard + "," + distanceVectorXStandard + "," + distanceVectorYStandard + "," + distanceToAddorSubtractXStandard + "," + distanceToAddorSubtractYStandard);
//
//        for (int i = 0; i < edgeList.size(); i += 6) {
//            x1 = edgeList.get(i);
//            y1 = edgeList.get(i + 1);
//            x2 = edgeList.get(i + 2);
//            y2 = edgeList.get(i + 3);
//            edgePixelLength = edgeList.get(i + 4);
//            edgeRealLength = edgeList.get(i + 5);
//
//            float distanceRatio = (distanceRatioRealLength * edgePixelLength / edgeRealLength);
//            float distanceVectorX = Math.abs(x1 - x2);
//            float distanceVectorY = Math.abs(y1 - y2);
//            if (distanceVectorX > 80 || distanceVectorY > 80)
//                distanceRatio = distanceRatio * 10;
//
//            distanceToAddOrSubtractX = (distanceVectorX / distanceRatio);
//            distanceToAddOrSubtractY = (distanceVectorY / distanceRatio);
//            //Log.i("TAG", "PointF normal: " + distanceRatio + "," + distanceVectorX + "," + distanceVectorY + "," + distanceToAddorSubtractX + "," + distanceToAddorSubtractY);
//            nodeList.add(new PointF(x1, y1));
//
//            if ((x1 == headStdX && y1 == headStdY && x2 == tailStdX && y2 == tailStdY) || (x2 == headStdX && y2 == headStdY && x1 == tailStdX && y1 == tailStdY)) {
//                if (headStdX >= tailStdX && headStdY >= tailStdY) {
//                    while (headStdY - distanceToAddOrSubtractYStandard >= tailStdY && headStdX - distanceToAddOrSubtractXStandard >= tailStdX) {
//                        nodeList.add(new PointF(headStdX - distanceToAddOrSubtractXStandard, headStdY - distanceToAddOrSubtractYStandard));
//                        headStdX -= distanceToAddOrSubtractXStandard;
//                        headStdY -= distanceToAddOrSubtractYStandard;
//                    }
//                } else if (headStdX >= tailStdX && headStdY <= tailStdY) {
//                    while (headStdY + distanceToAddOrSubtractYStandard <= tailStdY && headStdX - distanceToAddOrSubtractXStandard >= tailStdX) {
//                        nodeList.add(new PointF(headStdX - distanceToAddOrSubtractXStandard, headStdY + distanceToAddOrSubtractYStandard));
//                        headStdX -= distanceToAddOrSubtractXStandard;
//                        headStdY += distanceToAddOrSubtractYStandard;
//                    }
//                } else if (headStdX <= tailStdX && headStdY <= tailStdY) {
//                    while (headStdY + distanceToAddOrSubtractYStandard <= tailStdY && headStdX + distanceToAddOrSubtractXStandard <= tailStdX) {
//                        nodeList.add(new PointF(headStdX + distanceToAddOrSubtractXStandard, headStdY + distanceToAddOrSubtractYStandard));
//                        headStdX += distanceToAddOrSubtractXStandard;
//                        headStdY += distanceToAddOrSubtractYStandard;
//                    }
//                } else if (headStdX <= tailStdX && headStdY >= tailStdY) {
//                    while (headStdY - distanceToAddOrSubtractYStandard >= tailStdY && headStdX + distanceToAddOrSubtractXStandard <= tailStdX) {
//                        nodeList.add(new PointF(headStdX + distanceToAddOrSubtractXStandard, headStdY - distanceToAddOrSubtractYStandard));
//                        headStdX += distanceToAddOrSubtractXStandard;
//                        headStdY -= distanceToAddOrSubtractYStandard;
//                    }
//                }
//            } else {
//                if (x1 >= x2 && y1 >= y2) {
//                    while ((distanceToAddOrSubtractY > distanceToAddOrSubtractX && distanceToAddOrSubtractY < (width)) || (distanceToAddOrSubtractX > distanceToAddOrSubtractY && distanceToAddOrSubtractX < (width))) {
//                        distanceToAddOrSubtractY *= 2;
//                        distanceToAddOrSubtractX *= 2;
//                    }
//                    while (y1 - distanceToAddOrSubtractY >= y2 && x1 - distanceToAddOrSubtractX >= x2) {
//                        if (((y1 - distanceToAddOrSubtractY) - y2 < (width)) && ((x1 - distanceToAddOrSubtractX) - x2 < (width))) {
//                            x1 -= distanceToAddOrSubtractX;
//                            y1 -= distanceToAddOrSubtractY;
//                        } else {
//                            nodeList.add(new PointF(x1 - distanceToAddOrSubtractX, y1 - distanceToAddOrSubtractY));
//                            x1 -= distanceToAddOrSubtractX;
//                            y1 -= distanceToAddOrSubtractY;
//                        }
//                    }
//                } else if (x1 >= x2 && y1 <= y2) {
//                    while ((distanceToAddOrSubtractY > distanceToAddOrSubtractX && distanceToAddOrSubtractY < (width)) || (distanceToAddOrSubtractX > distanceToAddOrSubtractY && distanceToAddOrSubtractX < (width))) {
//                        distanceToAddOrSubtractY *= 2;
//                        distanceToAddOrSubtractX *= 2;
//                    }
//                    while (y1 + distanceToAddOrSubtractY <= y2 && x1 - distanceToAddOrSubtractX >= x2) {
//                        if ((y2 - (y1 + distanceToAddOrSubtractY) < (width)) && ((x1 - distanceToAddOrSubtractX) - x2 < (width))) {
//                            x1 -= distanceToAddOrSubtractX;
//                            y1 += distanceToAddOrSubtractY;
//                        } else {
//                            nodeList.add(new PointF(x1 - distanceToAddOrSubtractX, y1 + distanceToAddOrSubtractY));
//                            x1 -= distanceToAddOrSubtractX;
//                            y1 += distanceToAddOrSubtractY;
//                        }
//                    }
//                } else if (x1 <= x2 && y1 <= y2) {
//                    while ((distanceToAddOrSubtractY > distanceToAddOrSubtractX && distanceToAddOrSubtractY < (width)) || (distanceToAddOrSubtractX > distanceToAddOrSubtractY && distanceToAddOrSubtractX < (width))) {
//                        distanceToAddOrSubtractY *= 2;
//                        distanceToAddOrSubtractX *= 2;
//                    }
//                    while (y1 + distanceToAddOrSubtractY <= y2 && x1 + distanceToAddOrSubtractX <= x2) {
//                        if ((y2 - (y1 + distanceToAddOrSubtractY) < (width)) && (x2 - (x1 + distanceToAddOrSubtractX) < (width))) {
//                            x1 += distanceToAddOrSubtractX;
//                            y1 += distanceToAddOrSubtractY;
//                        } else {
//                            nodeList.add(new PointF(x1 + distanceToAddOrSubtractX, y1 + distanceToAddOrSubtractY));
//                            x1 += distanceToAddOrSubtractX;
//                            y1 += distanceToAddOrSubtractY;
//                        }
//                    }
//                } else if (x1 <= x2 && y1 >= y2) {
//                    while ((distanceToAddOrSubtractY > distanceToAddOrSubtractX && distanceToAddOrSubtractY < (width)) || (distanceToAddOrSubtractX > distanceToAddOrSubtractY && distanceToAddOrSubtractX < (width))) {
//                        distanceToAddOrSubtractY *= 2;
//                        distanceToAddOrSubtractX *= 2;
//                    }
//                    while (y1 - distanceToAddOrSubtractY >= y2 && x1 + distanceToAddOrSubtractX <= x2) {
//                        if (((y1 - distanceToAddOrSubtractY) - y2 < (width)) && (x2 - (x1 + distanceToAddOrSubtractX) < (width))) {
//                            x1 += distanceToAddOrSubtractX;
//                            y1 -= distanceToAddOrSubtractY;
//                        } else {
//                            nodeList.add(new PointF(x1 + distanceToAddOrSubtractX, y1 - distanceToAddOrSubtractY));
//                            x1 += distanceToAddOrSubtractX;
//                            y1 -= distanceToAddOrSubtractY;
//                        }
//                    }
//                }
//            }
//        }
//    }

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

}


