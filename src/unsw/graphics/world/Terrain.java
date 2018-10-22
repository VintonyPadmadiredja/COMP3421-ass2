package unsw.graphics.world;


import java.util.ArrayList;
import java.util.Arrays;
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
    private TriangleMesh terrainMesh;

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
        int upperBoundX = lowerBoundX + 1; // can't do ceil in the case when x,z is a whole number
        if (upperBoundX == width) {
            upperBoundX -= 1;
        }
        int lowerBoundZ = (int) Math.floor(z);
        int upperBoundZ = lowerBoundZ + 1;
        if (upperBoundZ == depth) {
            upperBoundZ -= 1;
        }
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
        // Z increases down, so this slope is decreasing
        // m = 1/-1 = -1
        // f = 2h(x - x0) + 2w(z - z0) //h == w == 1
        // < 0 below the line
        // > 0 above the line
        // == 0 on the line

        float f_value = 2 * (x - p0.getX()) + 2 * (z - p0.getZ());
        if (f_value < 0) {
            altitude = (float) bilinearInterpolate(x, z, p1, p2, p0);
        } else if (f_value > 0) {
            altitude = (float) bilinearInterpolate(x, z, p3, p0, p2);
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
        Road road = new Road(width, spine, this);
        roads.add(road);        
    }

    /**
     * Perform a billinear interpolation on point (x,z) within triangle
     * p1 = left end, p2 = common point, p3 = right end of triangle
     * @param x
     * @param z
     * @param p1
     * @param p2
     * @param p3
     * @return double
     */
    private double bilinearInterpolate(float x, float z, Point3D p1, Point3D p2, Point3D p3) {
        // linear interpolation of z for both lines, p1-p2 and p3-p2
        double alt1 = linearInterpolateZ(z, p1, p2);
        double alt2 = linearInterpolateZ(z, p3, p2);

        // find points which corresponds to altitude calculated
        Point3D point1 = new Point3D(getXFromLine(z, p1.getX(), p1.getZ(), p2.getX(), p2.getZ()),
                (float) alt1, z);
        Point3D point2 = new Point3D(getXFromLine(z, p3.getX(), p3.getZ(), p2.getX(), p2.getZ()),
                (float) alt2, z);

        // return linear interpolation of x
        return linearInterpolateX(x, point1, point2);
    }

    /**
     * Perform a linear interpolation on z
     * @param z
     * @param p1
     * @param p2
     * @return
     */
    private double linearInterpolateZ(float z, Point3D p1, Point3D p2) {
        return ((z - p1.getZ()) / (p2.getZ() - p1.getZ())) * p2.getY() +
                ((p2.getZ() - z) / (p2.getZ() - p1.getZ())) * p1.getY();
    }

    /**
     * Perform a linear interpolation on x
     * @param x
     * @param p1
     * @param p2
     * @return double
     */
    private double linearInterpolateX(float x, Point3D p1, Point3D p2) {
        return ((x - p1.getX()) / (p2.getX() - p1.getX())) * p2.getY() +
                ((p2.getX() - x) / (p2.getX() - p1.getX())) * p1.getY();
    }

    /**
     * Get x value from a line given a z value and 2 points
     * z-z1/x-x1 = z2-z1/x2-x1
     * x = z-z1/(z2-z1/x2-x1) + x1
     * @param z
     * @param x1
     * @param z1
     * @param x2
     * @param z2
     * @return float
     */
    private float getXFromLine(float z, float x1, float z1, float x2, float z2) {
        return ( ( ((z - z1) * (x2 - x1)) / (z2 - z1) ) + x1 );
    }

    /**
     * Generate terrain using a single triangle mesh with vertex normals
     * @param gl
     */
    public void makeTerrain(GL3 gl) {
        List<Point3D> points = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        List<Point2D> texCoords = new ArrayList<Point2D>();
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                points.add(new Point3D(x, (float) getGridAltitude(x, z), z));

                // Add current vertex in terrain as a texture coordinate
                texCoords.add(new Point2D(x, z));

                //      p3      p2
                //        ------
                //        |   /|
                //        |  / |
                //        | /  |
                //        |/   |
                //        ------
                //      p0      p1

                if (z > 0 && x < (width - 1)) {
                    Integer i0 = width * z + x;
                    Integer i1 = (width * z) + 1 + x;
                    Integer i2 = (width * (z - 1)) + 1 + x;
                    Integer i3 = width * (z - 1) + x;

                    // determine which diagonal to take to cut up a tile
                    // abs(alt(p0) - alt(p2)) > abs(alt(p1) - alt(p3))
                    if (Math.abs(getGridAltitude(x, z) - getGridAltitude(x + 1, z - 1)) >
                        Math.abs(getGridAltitude(x + 1, z) - getGridAltitude(x, z - 1))) {

                        // Triangle 1 (p0, p1, p2)
                        indices.addAll(Arrays.asList(i0, i1, i2));

                        // Triangle 2 (p0, p2, p3)
                        indices.addAll(Arrays.asList(i0, i2, i3));

                    } else {
                        //Triangle 1 (p1, p3, p0)
                        indices.addAll(Arrays.asList(i1, i3, i0));

                        // Triangle 2 (p1, p2, p3)
                        indices.addAll(Arrays.asList(i1, i2, i3));
                    }
                }
            }
        }
        
        terrainMesh = new TriangleMesh(points, indices, true, texCoords);

        // Initialise terrain
        terrainMesh.init(gl);

        // Initialise trees
        for (Tree tree : trees)
            tree.init(gl);

        // Initialise roads
        for (Road road : roads)
            road.init(gl);
    }

    /**
     * Draw terrain
     * @param gl
     * @param frame
     */
    public void drawTerrain(GL3 gl, CoordFrame3D frame) {
        terrainMesh.draw(gl, frame);
    }

    /**
     * Draw trees
     * @param gl
     * @param frame
     */
    public void drawTrees(GL3 gl, CoordFrame3D frame) {
        for (Tree tree: trees)
            tree.draw(gl, frame);
    }

    /**
     * Draw roads
     * @param gl
     * @param frame
     */
    public void drawRoads(GL3 gl, CoordFrame3D frame) {
        for (Road road: roads)
            road.draw(gl, frame);
    }


    /**
     * Destroy Terrain object
     * @param gl
     */
    public void destroyTerrain(GL3 gl) {
        terrainMesh.destroy(gl);
    }

    /**
     * Destroy all Tree objects
     * @param gl
     */
    public void destroyTrees(GL3 gl) {
        for (Tree tree: trees)
            tree.destroy(gl);
    }

    /**
     * Destroy all Tree objects
     * @param gl
     */
    public void destroyRoads(GL3 gl) {
        for (Road road: roads)
            road.destroy(gl);
    }

    /**
     * Get width of terrain
     * @return int
     */
    public int getWidth() {
        return width;
    }

    /**
     * Get depth of terrain
     * @return
     */
    public int getDepth() {
        return depth;
    }
}
