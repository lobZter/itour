package nctu.cs.cgv.itour.object;

public class IdxWeights {
    public int idx;
    float[] weights;

    public IdxWeights() {
        idx = -1;

        weights = new float[3];
        weights[0] = -1;
        weights[1] = -1;
        weights[2] = -1;
    }

    public IdxWeights(int pid, float pw1, float pw2, float pw3) {
        idx = pid;

        weights = new float[3];
        weights[0] = pw1;
        weights[1] = pw2;
        weights[2] = pw3;
    }
}
