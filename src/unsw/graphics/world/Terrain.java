package unsw.graphics.world;



import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import javafx.beans.binding.IntegerBinding;
import unsw.graphics.CoordFrame3D;
import unsw.graphics.Shader;
import unsw.graphics.Texture;
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

    private static final int NUM_SLICES = 32;
    private int width;
    private int depth;
    private float[][] altitudes;
    private List<Tree> trees;
    private List<Road> roads;
    private Vector3 sunlight;
    private List<TriangleMesh> terrainMeshes =  new ArrayList<>();

    private Shader shader;
    private Texture terrainTexture;
    private Texture treeTexture;

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
//        System.out.println(x + " " + z);
        // TODO: Implement this
        int lowerBoundX = (int) Math.floor(x);
        int upperBoundX = lowerBoundX + 1; // can't do ceil in the case when x,z is a whole number
        int lowerBoundZ = (int) Math.floor(z);
        int upperBoundZ = lowerBoundZ + 1;

        Point3D p0 = new Point3D(upperBoundX, (float) getGridAltitude(upperBoundX, lowerBoundZ), lowerBoundZ);
//        System.out.format("p0 = %d, %d, %d\n", upperBoundX, (int) getGridAltitude(upperBoundX, lowerBoundZ), lowerBoundZ);
        Point3D p1 = new Point3D(lowerBoundX, (float) getGridAltitude(lowerBoundX, lowerBoundZ), lowerBoundZ);
//        System.out.format("p1 = %d, %d, %d\n", lowerBoundX, (int)getGridAltitude(lowerBoundX, lowerBoundZ), lowerBoundZ);
        Point3D p2 = new Point3D(lowerBoundX, (float) getGridAltitude(lowerBoundX, upperBoundZ), upperBoundZ);
//        System.out.format("p2 = %d, %d, %d\n", lowerBoundX, (int)getGridAltitude(lowerBoundX, upperBoundZ), upperBoundZ);
        Point3D p3 = new Point3D(upperBoundX, (float) getGridAltitude(upperBoundX, upperBoundZ), upperBoundZ);
//        System.out.format("p3 = %d, %d, %d\n", upperBoundX, (int)getGridAltitude(upperBoundX, upperBoundZ), upperBoundZ);

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

        float f_value = 2 * (x - p0.getX()) + 2 * (z - p0.getZ());
//        System.out.println(f_value);
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
//        System.out.println("x = " + x + " z = " + z);
        float y = altitude(x, z) + 1f;
        z = z + 0.125f;
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

    // (x,z) = point within triangle, p1 = left end, p2 = common point, p3 = right end
    private double bilinearInterpolate(float x, float z, Point3D p1, Point3D p2, Point3D p3) {
        // linear interpolation of z for both lines, p1-p2 and p3-p2
        double alt1 = linearInterpolateZ(z, p1, p2);
//        System.out.println("alt1 = " + alt1);
        double alt2 = linearInterpolateZ(z, p3, p2);
//        System.out.println("alt2 = " + alt2);

//        System.out.format("p1 = %f, %f, %f\n", p1.getX(), p1.getY(), p1.getZ());
//        System.out.format("p2 = %f, %f, %f\n", p2.getX(), p2.getY(), p2.getZ());
//        System.out.format("p3 = %f, %f, %f\n", p3.getX(), p3.getY(), p3.getZ());
        // find points which corresponds to altitude calculated
        Point3D point1 = new Point3D(getXFromLine(z, p1.getX(), p1.getZ(), p2.getX(), p2.getZ()),
                (float) alt1, z);
//        System.out.println(point1.getX() + " " + point1.getY() + " " + point1.getZ());
        Point3D point2 = new Point3D(getXFromLine(z, p3.getX(), p3.getZ(), p2.getX(), p2.getZ()),
                (float) alt2, z);
//        System.out.println(point2.getX() + " " + point2.getY() + " " + point2.getZ());
        // return linear interpolation of x
        return linearInterpolateX(x, point1, point2);
    }

    private double linearInterpolateZ(float z, Point3D p1, Point3D p2) {
        return ((z - p1.getZ()) / (p2.getZ() - p1.getZ())) * p2.getY() +
                ((p2.getZ() - z) / (p2.getZ() - p1.getZ())) * p1.getY();
    }

    private double linearInterpolateX(float x, Point3D p1, Point3D p2) {
        return ((x - p1.getX()) / (p2.getX() - p1.getX())) * p2.getY() +
                ((p2.getX() - x) / (p2.getX() - p1.getX())) * p1.getY();
    }

//    private float getZFromLine(float x, float x1, float z1, float x2, float z2) {
//        System.out.println("GetZ");
//        System.out.println((z2 - z1) / (x2 - x1));
//        System.out.println(x - x1);
//        return ( z1 + ( ((z2 - z1) / (x2 - x1)) * (x - x1) ) );
//    }

    private float getXFromLine(float z, float x1, float z1, float x2, float z2) {
//        System.out.println("GetX");
//        System.out.println((z - z1) * (x2 - x1));
//        System.out.println(z2 - z1);
        return ( ( ((z - z1) * (x2 - x1)) / (z2 - z1) ) + x1 );
    }

    public void makeTerrain(GL3 gl) {
        // Initialise textures
        terrainTexture = new Texture(gl, "res/textures/grass.jpg", "jpg", true);
        treeTexture = new Texture(gl, "res/textures/tree.bmp", "bmp", true);

        ArrayList<Point2D> texCoords = new ArrayList<Point2D>();

        // Initialise shader
        shader = new Shader(gl, "shaders/vertex_tex_phong_world.glsl",
                "shaders/fragment_tex_phong_world.glsl");
        shader.use(gl);

        List<Point3D> points = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                points.add(new Point3D(x, (float) getGridAltitude(x, z), z));
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

                    texCoords.add(new Point2D(0,0));
                    texCoords.add(new Point2D(0,1));
                    texCoords.add(new Point2D(1,1));
                    texCoords.add(new Point2D(1,0));

                    if (Math.abs(getGridAltitude(x, z) - getGridAltitude(x + 1, z - 1)) >
                        Math.abs(getGridAltitude(x + 1, z) - getGridAltitude(x, z - 1))) {
                        indices.add(i0);
                        indices.add(i1);
                        indices.add(i2);

                        indices.add(i0);
                        indices.add(i2);
                        indices.add(i3);
                    } else {
                        indices.add(i1);
                        indices.add(i3);
                        indices.add(i0);

                        indices.add(i1);
                        indices.add(i2);
                        indices.add(i3);
                    }
                }
            }
        }
        TriangleMesh segment = new TriangleMesh(points, indices, true, texCoords);
        segment.init(gl);
        terrainMeshes.add(segment);

//        for (int z = 0; z < depth - 1; z++) {
//            List<Point3D> points = new ArrayList<>();
////            List<Integer> indices = new ArrayList<>();
//            for (int x = 0; x < width - 1; x++) {
//
//                //      p1      p0
//                //        ------
//                //        |   /|
//                //        |  / |
//                //        | /  |
//                //        |/   |
//                //        ------
//                //      p2      p3
//
//                Point3D p0 = new Point3D(x + 1, (float) getGridAltitude(x + 1, z), z);
//                Point3D p1 = new Point3D(x, (float) getGridAltitude(x, z), z);
//                Point3D p2 = new Point3D(x, (float) getGridAltitude(x, z + 1), z + 1);
//                Point3D p3 = new Point3D(x + 1, (float) getGridAltitude(x + 1, z + 1), z + 1);
//
//                texCoords.add(new Point2D(0,0));
//                texCoords.add(new Point2D(0,1));
//                texCoords.add(new Point2D(1,1));
//                texCoords.add(new Point2D(1,0));
//
//                // determine which diagonal to take
//                // abs(alt(p2) - alt(p0)) > abs(alt(p1) - alt(p3))
//                if (Math.abs(getGridAltitude(x, z + 1) - getGridAltitude(x + 1, z)) >
//                        Math.abs(getGridAltitude(x + 1, z + 1) - getGridAltitude(x, z))) {
//                    points.add(p0);
//                    points.add(p1);
//                    points.add(p2);
//
//                    points.add(p0);
//                    points.add(p2);
//                    points.add(p3);
//
////                    indices.add(4 * x);
////                    indices.add(4 * x + 1);
////                    indices.add(4 * x + 2);
////
////                    indices.add(4 * x);
////                    indices.add(4 * x + 2);
////                    indices.add(4 * x + 3);
//                } else {
//                    points.add(p3);
//                    points.add(p0);
//                    points.add(p1);
//
//                    points.add(p3);
//                    points.add(p1);
//                    points.add(p2);
//
////                    indices.add(4 * x + 3);
////                    indices.add(4 * x);
////                    indices.add(4 * x + 1);
////
////                    indices.add(4 * x + 3);
////                    indices.add(4 * x + 1);
////                    indices.add(4 * x + 2);
//                }
//
//            }
//            TriangleMesh segment = new TriangleMesh(points, true, texCoords);
//            segment.init(gl);
//            terrainMeshes.add(segment);
//        }

        for (Tree tree : trees)
            tree.init(gl);
    }

    public void drawTerrain(GL3 gl, CoordFrame3D frame) {

        // ------- DRAW TERRAIN -------
        Shader.setPenColor(gl, Color.WHITE);
        Shader.setInt(gl, "tex", 0);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, terrainTexture.getId());
//        Shader.setViewMatrix(gl, frame.getMatrix());

        // Set wrap mode for texture in S direction
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_MIRRORED_REPEAT);

        // Set wrap mode for texture in T direction
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL3.GL_MIRRORED_REPEAT);

        // Set the lighting properties
        Shader.setPoint3D(gl, "lightPos", sunlight.asPoint3D());
        Shader.setColor(gl, "lightIntensity", Color.WHITE);
        Shader.setColor(gl, "ambientIntensity", new Color(0.5f, 0.5f, 0.5f));

        // Set the material properties
        Shader.setColor(gl, "ambientCoeff", Color.WHITE);
        Shader.setColor(gl, "diffuseCoeff", new Color(0.8f, 0.8f, 0.8f));
        Shader.setColor(gl, "specularCoeff", new Color(0.2f, 0.2f, 0.2f));
        Shader.setFloat(gl, "phongExp", 16f);

        // Draw terrain
        for (TriangleMesh mesh : terrainMeshes)
            mesh.draw(gl, frame);
//
//
//        // ------- DRAW TREES -------
        Shader.setPenColor(gl, Color.WHITE);
        Shader.setInt(gl, "tex", 0);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, treeTexture.getId());
//        Shader.setViewMatrix(gl, frame.getMatrix());

        // Draw trees
        for (Tree tree: trees)
            tree.draw(gl, frame);
    }

    public int getWidth() {
        return width;
    }

    public int getDepth() {
        return depth;
    }
}
