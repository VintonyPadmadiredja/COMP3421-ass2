package unsw.graphics.world;

import com.jogamp.opengl.GL3;
import unsw.graphics.CoordFrame3D;
import unsw.graphics.geometry.Point3D;
import unsw.graphics.geometry.TriangleMesh;

import java.io.IOException;

/**
 * COMMENT: Comment Tree 
 *
 * @author malcolmr
 */
public class Tree {

    private TriangleMesh tree;
    private static final float SCALE_FACTOR = 0.2f;
    private Point3D position;


    public Tree(float x, float y, float z) {
        position = new Point3D(x, y, z);
    }

    public void init(GL3 gl) {
        try {
            // Initialise tree model
            tree = new TriangleMesh("res/models/tree.ply", true, true);
            tree.init(gl);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void draw(GL3 gl, CoordFrame3D frame) {
        // Create new tree's frame, extending from Terrain's frame
        CoordFrame3D treeFrame = frame
                .translate(position)
                .scale(SCALE_FACTOR, SCALE_FACTOR, SCALE_FACTOR);

        // Draw the tree's meshes
        tree.draw(gl, treeFrame);
    }

    public Point3D getPosition() {
        return position;
    }
    

}
