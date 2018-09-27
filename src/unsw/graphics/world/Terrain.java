package unsw.graphics.world;



import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL3;
import unsw.graphics.CoordFrame3D;
import unsw.graphics.Vector3;
import unsw.graphics.geometry.Point2D;
import unsw.graphics.geometry.Point3D;
import unsw.graphics.geometry.TriangleMesh;


/**
 * COMMENT: Comment HeightMap 
 *
 * @author malcolmr
 */
public class Terrain {

    private int width;
    private int depth;
    private float[][] altitudes;
    private List<Tree> trees;
    private List<Road> roads;
    private Vector3 sunlight;
    private List<TriangleMesh> terrainMeshes =  new ArrayList<>();
    /**
     * Create a new terrain
     *
     * @param width The number of vertices in the x-direction
     * @param depth The number of vertices in the z-direction
     */
    public Terrain(int width, int depth, Vector3 sunlight) {
        this.width = width;
        this.depth = depth;
        altitudes = new float[width][depth];
        trees = new ArrayList<Tree>();
        roads = new ArrayList<Road>();
        this.sunlight = sunlight;
    }

    public List<Tree> trees() {
        return trees;
    }

    public List<Road> roads() {
        return roads;
    }

    public Vector3 getSunlight() {
        return sunlight;
    }

    /**
     * Set the sunlight direction. 
     * 
     * Note: the sun should be treated as a directional light, without a position
     * 
     * @param dx
     * @param dy
     * @param dz
     */
    public void setSunlightDir(float dx, float dy, float dz) {
        sunlight = new Vector3(dx, dy, dz);      
    }

    /**
     * Get the altitude at a grid point
     * 
     * @param x
     * @param z
     * @return
     */
    public double getGridAltitude(int x, int z) {
        return altitudes[x][z];
    }

    /**
     * Set the altitude at a grid point
     * 
     * @param x
     * @param z
     * @return
     */
    public void setGridAltitude(int x, int z, float h) {
        altitudes[x][z] = h;
    }

    /**
     * Get the altitude at an arbitrary point. 
     * Non-integer points should be interpolated from neighbouring grid points
     * 
     * @param x
     * @param z
     * @return
     */
    public float altitude(float x, float z) {
        float altitude = 0;

        // TODO: Implement this
        int lowerBoundX = (int) Math.floor(x);
        int upperBoundX = (int) Math.ceil(x);
        int lowerBoundZ = (int) Math.floor(z);
        int upperBoundZ = (int) Math.ceil(z);

        Point3D p0 = new Point3D(upperBoundX, (float) getGridAltitude(upperBoundX, lowerBoundZ), lowerBoundZ);
//        System.out.format("p0 = %d, %d, %d", upperBoundX, (int)getGridAltitude(upperBoundX, lowerBoundZ), lowerBoundZ);
        Point3D p1 = new Point3D(lowerBoundX, (float) getGridAltitude(lowerBoundX, lowerBoundZ), lowerBoundZ);
//        System.out.format("p1 = %d, %d, %d", lowerBoundX, (int)getGridAltitude(lowerBoundX, lowerBoundZ), lowerBoundZ);
        Point3D p2 = new Point3D(lowerBoundX, (float) getGridAltitude(lowerBoundX, upperBoundZ), upperBoundZ);
//        System.out.format("p2 = %d, %d, %d", lowerBoundX, (int)getGridAltitude(lowerBoundX, upperBoundZ), upperBoundZ);
        Point3D p3 = new Point3D(upperBoundX, (float) getGridAltitude(upperBoundX, upperBoundZ), upperBoundZ);
//        System.out.format("p3 = %d, %d, %d", upperBoundX, (int)getGridAltitude(upperBoundX, upperBoundZ), upperBoundZ);

        // test point is above or below a line
        // p1      p0
        // ------
        // |   /|
        // |  / |
        // | /  |
        // |/   |
        // ------
        // p2      p3
        // Z increases down, so this slope is decreasing
        // m = 1/-1 = -1
        // f = 2h(x − x0) + 2w(z − z0) //h == w == 1
        // < 0 below the line
        // > 0 above the line
        // == 0 on the line

        float f_value = 2*(x - p0.getX()) + 2 * (z - p0.getZ());
//        System.out.println(f_value);
        if (f_value < 0) {
            altitude = (float) bilinearInterpolate(x, z, p2, p0, p3);
        } else if (f_value > 0) {
            altitude = (float) bilinearInterpolate(x, z, p1, p2, p0);
        } else { // == 0
            altitude = (float) linearInterpolateZ(z, p0, p2);
        }

        return altitude;
    }

    /**
     * Add a tree at the specified (x,z) point. 
     * The tree's y coordinate is calculated from the altitude of the terrain at that point.
     * 
     * @param x
     * @param z
     */
    public void addTree(float x, float z) {
        System.out.println("x = " + x + " z = " + z);
        float y = altitude(x, z);
        Tree tree = new Tree(x, y, z);
        trees.add(tree);
    }


    /**
     * Add a road. 
     * 
     * @param x
     * @param z
     */
    public void addRoad(float width, List<Point2D> spine) {
        Road road = new Road(width, spine);
        roads.add(road);        
    }

    // p = point within triangle, p1 = left end, p2 = common point, p3 = right end
    private double bilinearInterpolate(float x, float z, Point3D p1, Point3D p2, Point3D p3) {
        // linear interpolation of z for both lines, p1-p2 and p3-p2
        double alt1 = linearInterpolateZ(z, p1, p2);
        double alt2 = linearInterpolateZ(z, p3, p2);
        // find points which corresponds to altitude calculated
        Point3D point1 = new Point3D(getXFromLine(z, p1.getX(), p2.getX(), p1.getZ(), p2.getZ()),
                (float) alt1,
                getZFromLine(x, p1.getX(), p2.getX(), p1.getZ(), p2.getZ()));
        Point3D point2 = new Point3D(getXFromLine(z, p1.getX(), p2.getX(), p1.getZ(), p2.getZ()),
                (float) alt2,
                getZFromLine(x, p1.getX(), p2.getX(), p1.getZ(), p2.getZ()));
        // return linear interpolation of x
        return linearInterpolateX(x, point1, point2);
    }

    private double linearInterpolateZ(float z, Point3D p1, Point3D p2) {
        return ((z - p1.getZ()) / (p2.getZ() - p1.getZ())) * getGridAltitude((int) p2.getX(), (int) p2.getZ()) +
                ((p2.getZ() - z) / (p2.getZ() - p1.getZ())) * getGridAltitude((int) p1.getX(), (int) p1.getZ());
    }

    private double linearInterpolateX(float x, Point3D p1, Point3D p2) {
        return ((x - p1.getX()) / (p2.getX() - p1.getX())) * getGridAltitude((int) p2.getX(), (int) p2.getZ()) +
                ((p2.getX() - x) / (p2.getX() - p1.getX())) * getGridAltitude((int) p1.getX(), (int) p1.getZ());
    }

    private float getZFromLine(float x, float x1, float z1, float x2, float z2) {
        return (z1 + ((z2 - z1)/(x2 - x1))*(x - x1));
    }

    private float getXFromLine(float z, float x1, float z1, float x2, float z2) {
        return ((z - z1)/((z2 - z1)/(x2 - x1)) + x1);
    }

    public void makeTerrain(GL3 gl) {
        for (int z = 0; z < depth - 1; z++) {
            List<Point3D> points = new ArrayList<>();
            List<Integer> indices = new ArrayList<>();
            for (int x = 0; x < width - 1; x++) {

                //      p1      p0
                //        ------
                //        |   /|
                //        |  / |
                //        | /  |
                //        |/   |
                //        ------
                //      p2      p3

                points.add(new Point3D(x + 1, (float) getGridAltitude(x + 1, z), z));
                points.add(new Point3D(x, (float) getGridAltitude(x, z), z));
                points.add(new Point3D(x, (float) getGridAltitude(x, z + 1), z + 1));
                points.add(new Point3D(x + 1, (float) getGridAltitude(x + 1, z + 1), z + 1));

                // indices.add(4*x);
                // indices.add(4*x + 1);
                // indices.add(4*x + 2);

                // indices.add(4*x);
                // indices.add(4*x + 2);
                // indices.add(4*x + 3);

                // NOTE*: Have to comment above and uncomment below to make it display like example
                // determine which diagonal to take
                // abs(alt(p2) - alt(p0)) > abs(alt(p1) - alt(p3))
                if (Math.abs(getGridAltitude(x, z + 1) - getGridAltitude(x + 1, z)) >
                        Math.abs(getGridAltitude(x + 1, z + 1) - getGridAltitude(x, z))) {
                    indices.add(4 * x);
                    indices.add(4 * x + 1);
                    indices.add(4 * x + 2);

                    indices.add(4 * x);
                    indices.add(4 * x + 2);
                    indices.add(4 * x + 3);
                } else {
                    indices.add(4 * x + 3);
                    indices.add(4 * x);
                    indices.add(4 * x + 1);

                    indices.add(4 * x + 3);
                    indices.add(4 * x + 1);
                    indices.add(4 * x + 2);
                }

            }
            TriangleMesh segment = new TriangleMesh(points, indices, true);
            segment.init(gl);
            terrainMeshes.add(segment);
        }
    }

    public void drawTerrain(GL3 gl, CoordFrame3D frame) {
        for (TriangleMesh mesh : terrainMeshes)
            mesh.draw(gl, frame);
    }
}
