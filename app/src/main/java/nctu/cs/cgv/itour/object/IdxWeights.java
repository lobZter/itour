package nctu.cs.cgv.itour.object;

public class IdxWeights {
    public int idx;
    double [] weights;

    public IdxWeights() {
        idx = -1;

        weights = new double[3];
        weights[0] = -1;
        weights[1] = -1;
        weights[2] = -1;
    }

    public IdxWeights(int pid, double pw1, double pw2, double pw3) {
        idx = pid;

        weights = new double[3];
        weights[0] = pw1;
        weights[1] = pw2;
        weights[2] = pw3;
    }
}
