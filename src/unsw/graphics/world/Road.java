package unsw.graphics.world;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.jogamp.opengl.GL3;
import unsw.graphics.*;
import unsw.graphics.geometry.Point2D;
import unsw.graphics.geometry.Point3D;
import unsw.graphics.geometry.TriangleMesh;

/**
 * COMMENT: Comment Road 
 *
 * @author malcolmr
 */
public class Road {

    private static final float SEGMENTS = 100f; // Sampling rate of the curve
    private static final Vector3 ROAD_NORMAL = new Vector3(0,1,0);
    private List<Point2D> points;
    private float width;
    private TriangleMesh road;
    private Terrain terrain;
    
    /**
     * Create a new road with the specified spine 
     *
     * @param width
     * @param spine
     */
    public Road(float width, List<Point2D> spine, Terrain terrain) {
        this.width = width;
        this.points = spine;
        this.terrain = terrain;
    }

    /**
     * The width of the road.
     * 
     * @return
     */
    public double width() {
        return width;
    }
    
    /**
     * Get the number of segments in the curve
     * 
     * @return
     */
    public int size() {
        return points.size() / 3;
    }

    /**
     * Get the specified control point.
     * 
     * @param i
     * @return
     */
    public Point2D controlPoint(int i) {
        return points.get(i);
    }
    
    /**
     * Get a point on the spine. The parameter t may vary from 0 to size().
     * Points on the kth segment take have parameters in the range (k, k+1).
     * 
     * @param t
     * @return
     */
    public Point2D point(float t) {
        int i = (int)Math.floor(t);
        t = t - i;
        
        i *= 3;
        
        Point2D p0 = points.get(i++);
        Point2D p1 = points.get(i++);
        Point2D p2 = points.get(i++);
        Point2D p3 = points.get(i++);
        

        float x = b(0, t) * p0.getX() + b(1, t) * p1.getX() + b(2, t) * p2.getX() + b(3, t) * p3.getX();
        float y = b(0, t) * p0.getY() + b(1, t) * p1.getY() + b(2, t) * p2.getY() + b(3, t) * p3.getY();        
        
        return new Point2D(x, y);
    }

    /**
     * Calculate the Bezier coefficients
     *
     * @param i
     * @param t
     * @return
     */
    private float b(int i, float t) {

        switch(i) {

            case 0:
                return (1-t) * (1-t) * (1-t);

            case 1:
                return 3 * (1-t) * (1-t) * t;

            case 2:
                return 3 * (1-t) * t * t;

            case 3:
                return t * t * t;
        }

        // this should never happen
        throw new IllegalArgumentException("" + i);
    }

    /**
     * Get a tangent point on the spine. The parameter t may vary from 0 to size().
     * Points on the kth segment take have parameters in the range (k, k+1).
     *
     * @param t
     * @return
     */
    public Point2D tangent(float t) {
        int i = (int)Math.floor(t);
        t = t - i;

        i *= 3;

        Point2D p0 = points.get(i++);
        Point2D p1 = points.get(i++);
        Point2D p2 = points.get(i++);
        Point2D p3 = points.get(i++);


        float x = b_derivative(0, t) * (p1.getX() - p0.getX()) + b_derivative(1, t) * (p2.getX() - p1.getX()) + b_derivative(2, t) * (p3.getX() - p2.getX());
        float y = b_derivative(0, t) * (p1.getY() - p0.getY()) + b_derivative(1, t) * (p2.getY() - p1.getY()) + b_derivative(2, t) * (p3.getY() - p2.getY());

        return new Point2D(x, y);
    }

    /**
     * Calculate the Bezier derivative's coefficients
     *
     * @param i
     * @param t
     * @return
     */
    private float b_derivative(int i, float t) {

        switch(i) {

            case 0:
                return 3 * (1-t) * (1-t);

            case 1:
                return 3 * 2 * t * (1-t);

            case 2:
                return 3 * t * t;
        }

        // this should never happen
        throw new IllegalArgumentException("" + i);
    }

    public void init(GL3 gl){
        List<Point3D> vertices = new ArrayList<>();
        List<Vector3> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Point2D> texCoords = new ArrayList<Point2D>();

        float roadAltitude = terrain.altitude(points.get(0).getX(), points.get(0).getY());

//        1) Sample points along the spine using different values of t
//        2) For each t:
//              - generate the current point on the spine
//              - generate a frenet transformation matrix
//              - multiply each point on the cross section by the matrix.
//              - join these points to the next set of points using quads/triangles.

        float dt = (points.size()/3f)/SEGMENTS;

        for(float t = 0f; t <= this.size(); t += dt){
            // Spine is the set of control points for our bezier curve (ROAD)
            // Calculate point on Road
            Point2D p1 = point(t);

            // i = (k3, 0, -k1) <- Note: k3, k1 means z, x components respectively of vector k (defined below)
            // k = normalise(tangent to curve at t)
            // j = k x i
            // phi = origin i.e. current point = p1

            Point2D tangent = tangent(t);
            Vector3 k = new Vector3(tangent.getX(),0, tangent.getY()).normalize();
            Vector3 i = new Vector3(k.getZ(),0, -k.getX());
            Vector3 j = k.cross(i);
            Vector3 phi = new Vector3(p1.getX(), roadAltitude, p1.getY());

            float[] frenetValues = new float[] {
                    i.getX(), i.getY(), i.getZ(), 0,       // i
                    j.getX(), j.getY(), j.getZ(), 0,       // j
                    k.getX(), k.getY(), k.getZ(), 0,       // k
                    phi.getX(), phi.getY(), phi.getZ(), 1  // phi
            };
            Matrix4 frenetFrame = new Matrix4(frenetValues);

            // Calculate left and right points of the road
            Point3D left = frenetFrame.multiply(new Point3D(-width/2, 0, 0).asHomogenous()).asPoint3D();
            Point3D right = frenetFrame.multiply(new Point3D(width/2, 0, 0).asHomogenous()).asPoint3D();

            // Add these points to vertices
            vertices.add(left);
            vertices.add(right);

            // Add these points to texture coordinates
            texCoords.add(new Point2D(left.getX(), left.getZ()));
            texCoords.add(new Point2D(right.getX(), right.getZ()));

            // Only create triangles after the second point (i.e. when t != 0)
            if (t != 0f){
                int index0 = vertices.size() - 4;
                int index1 = vertices.size() - 3;
                int index2 = vertices.size() - 2;
                int index3 = vertices.size() - 1;

                // index0 ---------- index2
                //   |                 /|
                //   |               /  |
                //   |             /    |
                //   |           /      |
                //   |         /        |
                //   |       /          |
                //   |     /            |
                //   |   /              |
                //   | /                |
                // index1 ---------- index3

                indices.addAll(Arrays.asList(index2, index3, index1));
                indices.addAll(Arrays.asList(index0, index2, index1));
                normals.add(ROAD_NORMAL);
                normals.add(ROAD_NORMAL);
            }
        }

        road = new TriangleMesh(vertices, normals, indices, texCoords);

        // Initialise road
        road.init(gl);
    }

    /**
     * Draw road
     * @param gl
     * @param frame
     */
    public void draw(GL3 gl, CoordFrame3D frame){
        CoordFrame3D roadFrame = frame;

        // Enable polygon offset to avoid Z-Fighting of Road and Terrain
        gl.glEnable(GL3.GL_POLYGON_OFFSET_FILL);
        gl.glPolygonOffset(-1,-1);

        road.draw(gl, roadFrame);

        // Disable polygon offset
        gl.glDisable(GL3.GL_POLYGON_OFFSET_FILL);

    }

    /**
     * Destroy road
     * @param gl
     */
    public void destroy(GL3 gl) {
        road.destroy(gl);
    }


}
