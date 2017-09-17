package nctu.cs.cgv.itour.object;

/**
 * Created by lobst3rd on 2017/8/4.
 */

public class SpotNode extends ImageNode {

    public String name;
    public MergedCheckinNode mergedCheckinNode;

    SpotNode(float x, float y, String name) {
        super(x, y);
        this.name = name;
        this.mergedCheckinNode = null;
    }
}
