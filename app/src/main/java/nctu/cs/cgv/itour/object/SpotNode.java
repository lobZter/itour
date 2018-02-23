package nctu.cs.cgv.itour.object;

/**
 * Created by lobst3rd on 2017/8/4.
 */

public class SpotNode extends ImageNode {

    public int order;
    public String name;
    public CheckinNode checkinNode;

    public SpotNode(float x, float y, String name, int order) {
        super(x, y);
        this.order = order;
        this.name = name;
        this.checkinNode = null;
    }
}
