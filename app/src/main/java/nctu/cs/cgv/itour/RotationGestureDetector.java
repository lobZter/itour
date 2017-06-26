package nctu.cs.cgv.itour;

import android.view.MotionEvent;

/**
 * Created by lobZter on 2017/5/30.
 */

public class RotationGestureDetector {
    private static final int INVALID_POINTER_ID = -1;
    private float fX, fY, sX, sY;
    private float focusX, focusY;
    private int ptrID1, ptrID2;
    private float mAngle;
    private float mPrevAngle;

    private OnRotationGestureListener mListener;

    public float getAngle() {
        return mAngle;
    }

    public float getDeltaAngle() { return mAngle - mPrevAngle; }

    public float getAnchorX() {
        return sX;
    }

    public float getAnchorY() {
        return sY;
    }

    public float getFocusX() { return focusX; }

    public float getFocusY() { return focusY; }

    public RotationGestureDetector(OnRotationGestureListener listener){
        mListener = listener;
        ptrID1 = INVALID_POINTER_ID;
        ptrID2 = INVALID_POINTER_ID;
    }

    public boolean onTouchEvent(MotionEvent event){
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                ptrID1 = event.getPointerId(event.getActionIndex());
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                ptrID2 = event.getPointerId(event.getActionIndex());
                sX = event.getX(event.findPointerIndex(ptrID1));
                sY = event.getY(event.findPointerIndex(ptrID1));
                fX = event.getX(event.findPointerIndex(ptrID2));
                fY = event.getY(event.findPointerIndex(ptrID2));
                focusX = (sX + fX) / 2;
                focusY = (sY + fY) / 2;
                break;
            case MotionEvent.ACTION_MOVE:
                if(ptrID1 != INVALID_POINTER_ID && ptrID2 != INVALID_POINTER_ID){
                    float nfX, nfY, nsX, nsY;
                    nsX = event.getX(event.findPointerIndex(ptrID1));
                    nsY = event.getY(event.findPointerIndex(ptrID1));
                    nfX = event.getX(event.findPointerIndex(ptrID2));
                    nfY = event.getY(event.findPointerIndex(ptrID2));
                    focusX = (nsX + nfX) / 2;
                    focusY = (nsY + nfY) / 2;
                    mPrevAngle = mAngle;
                    mAngle = angleBetweenLines(fX, fY, sX, sY, nfX, nfY, nsX, nsY);

                    if (mListener != null) {
                        mListener.onRotation(this);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                ptrID1 = INVALID_POINTER_ID;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                ptrID2 = INVALID_POINTER_ID;
                if (mListener != null) {
                    mPrevAngle = 0;
                    mAngle = 0;
                    mListener.onRotationEnd(this);
                }
                break;
            case MotionEvent.ACTION_CANCEL:
                ptrID1 = INVALID_POINTER_ID;
                ptrID2 = INVALID_POINTER_ID;
                if (mListener != null) {
                    mPrevAngle = 0;
                    mAngle = 0;
                    mListener.onRotationEnd(this);
                }
                break;
        }
        return true;
    }

    private float angleBetweenLines (float fX, float fY, float sX, float sY, float nfX, float nfY, float nsX, float nsY)
    {
        float angle1 = (float) Math.atan2( (fY - sY), (fX - sX) );
        float angle2 = (float) Math.atan2( (nfY - nsY), (nfX - nsX) );

        float angle = ((float)Math.toDegrees(angle1 - angle2)) % 360;
        if (angle < -180.f) angle += 360.0f;
        if (angle > 180.f) angle -= 360.0f;
        return angle;
    }

    public static interface OnRotationGestureListener {
        public void onRotation(RotationGestureDetector rotationDetector);
        public void onRotationEnd(RotationGestureDetector rotationDetector);
    }
}
