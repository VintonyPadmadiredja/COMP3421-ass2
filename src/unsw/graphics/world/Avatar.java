package unsw.graphics.world;

import com.jogamp.opengl.GL3;
import unsw.graphics.CoordFrame3D;
import unsw.graphics.geometry.Point3D;
import unsw.graphics.geometry.TriangleMesh;

import java.io.IOException;

public class Avatar {

    private static final float MODEL_SCALE_FACTOR = 4.5f;
    private static final float MODEL_ROTATION = 180f;

    private TriangleMesh avatar;
    private Point3D position;
    private float rotateY = 0;

    public Avatar() {
        position = new Point3D(0,0,0);
    }

    /**
     * Generate avatar
     * @param gl
     */
    public void init(GL3 gl) {
        try {
            // Initialise tree model
            avatar = new TriangleMesh("res/models/bunny.ply", true, true);
            avatar.init(gl);

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
        // Create new avatar's frame, extending from Terrain's frame
        CoordFrame3D avatarFrame = frame
                .translate(position)
                .rotateY(MODEL_ROTATION + rotateY)
                .scale(MODEL_SCALE_FACTOR, MODEL_SCALE_FACTOR, MODEL_SCALE_FACTOR);


        // Enable polygon offset to avoid Z-Fighting of Avatar and Terrain
        gl.glEnable(GL3.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(-1,-1);

        // Draw the avatar's meshes
        avatar.draw(gl, avatarFrame);

        // Disable polygon offset
        gl.glDisable(GL3.GL_POLYGON_OFFSET_FILL);

    }

    /**
     * Get position of the avatar
     * @return Point3D - Position of avatar
     */
    public Point3D getPosition() {
        return position;
    }

    /**
     * Destroy avatar object
     * @param gl
     */
    public void destroy(GL3 gl) {
        avatar.destroy(gl);
    }

    public void rotate(float r) {
        rotateY += r;
    }

    public void updatePosition(float x, float y, float z) {
        position = new Point3D(x, y, z);
    }
}
