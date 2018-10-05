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
    private static final float MODEL_SCALE_FACTOR = 0.2f;
    private static final float MODEL_ALTITUDE_OFFSET = 0.9f;
    private static final float MODEL_Z_OFFSET = 0.125f;
    private Point3D position;

    /**
     * Create a new Tree
     * @param x - The position of tree in the x-direction
     * @param y - The position of tree in the y-direction
     * @param z - The position of tree in the z-direction
     */
    public Tree(float x, float y, float z) {
        y = y + MODEL_ALTITUDE_OFFSET;
        z = z + MODEL_Z_OFFSET;
        position = new Point3D(x, y, z);
    }

    /**
     * Generate tree
     * @param gl
     */
    public void init(GL3 gl) {
        try {
            // Initialise tree model
            tree = new TriangleMesh("res/models/tree.ply", true, true);
            tree.init(gl);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param gl
     * @param frame
     */
    public void draw(GL3 gl, CoordFrame3D frame) {
        // Create new tree's frame, extending from Terrain's frame
        CoordFrame3D treeFrame = frame
                .translate(position)
                .scale(MODEL_SCALE_FACTOR, MODEL_SCALE_FACTOR, MODEL_SCALE_FACTOR);

        // Draw the tree's meshes
        tree.draw(gl, treeFrame);
    }

    /**
     * Get position of the tree
     * @return Point3D - Position of tree
     */
    public Point3D getPosition() {
        return position;
    }
    

}
