package unsw.graphics.world;



import java.util.ArrayList;
import java.util.List;

import unsw.graphics.Vector3;
import unsw.graphics.geometry.Point2D;
import unsw.graphics.geometry.Point3D;


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
        Point3D p1 = new Point3D(lowerBoundX, (float) getGridAltitude(lowerBoundX, lowerBoundZ), lowerBoundZ);
        Point3D p2 = new Point3D(lowerBoundX, (float) getGridAltitude(lowerBoundX, upperBoundZ), upperBoundZ);
        Point3D p3 = new Point3D(upperBoundX, (float) getGridAltitude(upperBoundX, upperBoundZ), upperBoundZ);

        // test point is above or below a line
        // p1      p0
        // ------
        // |   /|
        // |  / |
        // | /  |
        // |/   |
        // ------
        // p2      p3
        // m = 1/-1 = -1
        // f = 2h(x − x0)− 2w(y − y0) //h == w == 1
        // < 0 below the line
        // > 0 above the line
        // == 0 on the line

        float f_value = 2*(x - p0.getX()) - 2 * (z - p0.getZ());
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

    public int getWidth() {
        return width;
    }

    public int getDepth() {
        return depth;
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
}
