package unsw.graphics.world;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.GL3;

import unsw.graphics.Application3D;
import unsw.graphics.CoordFrame3D;
import unsw.graphics.Matrix4;
import unsw.graphics.Shader;
import unsw.graphics.geometry.Point2D;
import unsw.graphics.geometry.Point3D;
import unsw.graphics.geometry.TriangleMesh;


/**
 * COMMENT: Comment Game 
 *
 * @author malcolmr
 */
public class World extends Application3D implements MouseListener {

    private Terrain terrain;
	private List<TriangleMesh> meshes =  new ArrayList<>();
    private float rotateX = 0;
    private float rotateY = 0;
    private Point2D myMousePoint = null;
    private static final int ROTATION_SCALE = 1;


    public World(Terrain terrain) {
    	super("Assignment 2", 800, 600);
        this.terrain = terrain;
   
    }
   
    /**
     * Load a level file and display it.
     * 
     * @param args - The first argument is a level file in JSON format
     * @throws FileNotFoundException
     */
    public static void main(String[] args) throws FileNotFoundException {
        Terrain terrain = LevelIO.load(new File(args[0]));
        World world = new World(terrain);
        world.start();
    }

	@Override
	public void display(GL3 gl) {
		super.display(gl);

		//camera
        CoordFrame3D view = CoordFrame3D.identity().translate(0, -0.5f, 0f);
        Shader.setViewMatrix(gl, view.getMatrix());

        CoordFrame3D frame = CoordFrame3D.identity().translate(-2, 0, -8)//.scale(0.5f, 0.5f, 0.5f)
                .rotateX(rotateX).rotateY(rotateY);
        Shader.setPenColor(gl, Color.GRAY);
        for (TriangleMesh mesh : meshes)
            mesh.draw(gl, frame);

	}

	@Override
	public void destroy(GL3 gl) {
		super.destroy(gl);
		
	}

	@Override
	public void init(GL3 gl) {
		super.init(gl);
        getWindow().addMouseListener(this);

        Shader shader = new Shader(gl, "shaders/vertex_flat.glsl",
                "shaders/fragment_flat.glsl");
        shader.use(gl);

        // Set the lighting properties
        Shader.setPoint3D(gl, "lightPos", new Point3D(0, 0, 5));
        Shader.setColor(gl, "lightIntensity", Color.WHITE);
        Shader.setColor(gl, "ambientIntensity", new Color(0.2f, 0.2f, 0.2f));

        // Set the material properties
        Shader.setColor(gl, "ambientCoeff", Color.WHITE);
        Shader.setColor(gl, "diffuseCoeff", new Color(0.5f, 0.5f, 0.5f));
        Shader.setColor(gl, "specularCoeff", new Color(0.8f, 0.8f, 0.8f));
        Shader.setFloat(gl, "phongExp", 16f);

        makeTerrain(gl);
    }

	@Override
	public void reshape(GL3 gl, int width, int height) {
        super.reshape(gl, width, height);
        Shader.setProjMatrix(gl, Matrix4.perspective(60, width/(float)height, 1, 100));
	}

    private void makeTerrain(GL3 gl) {
        for (int z = 0; z < terrain.getDepth() - 1; z++) {
            List<Point3D> points = new ArrayList<>();
            List<Integer> indices = new ArrayList<>();
            for (int x = 0; x < terrain.getWidth() - 1; x++) {

                //      p1      p0
                //        ------
                //        |   /|
                //        |  / |
                //        | /  |
                //        |/   |
                //        ------
                //      p2      p3

                points.add(new Point3D(x + 1, (float) terrain.getGridAltitude(x + 1, z), z));
                points.add(new Point3D(x, (float) terrain.getGridAltitude(x, z), z));
                points.add(new Point3D(x, (float) terrain.getGridAltitude(x , z + 1), z + 1));
                points.add(new Point3D(x + 1, (float) terrain.getGridAltitude(x + 1, z + 1), z + 1));

//                indices.add(4*x);
//                indices.add(4*x + 1);
//                indices.add(4*x + 2);
//
//                indices.add(4*x);
//                indices.add(4*x + 2);
//                indices.add(4*x + 3);
                
                // NOTE*: Have to comment above and uncomment below to make it display like example
                // determine which diagonal to take
                // abs(alt(p2) - alt(p0)) > abs(alt(p1) - alt(p3))
                if (Math.abs(terrain.getGridAltitude(x, z + 1) - terrain.getGridAltitude(x + 1, z)) >
                        Math.abs(terrain.getGridAltitude(x + 1, z + 1) - terrain.getGridAltitude(x, z))) {
                    indices.add(4*x);
                    indices.add(4*x + 1);
                    indices.add(4*x + 2);

                    indices.add(4*x);
                    indices.add(4*x + 2);
                    indices.add(4*x + 3);
                } else {
                    indices.add(4*x + 3);
                    indices.add(4*x);
                    indices.add(4*x + 1);

                    indices.add(4*x + 3);
                    indices.add(4*x + 1);
                    indices.add(4*x + 2);
                }

            }
            TriangleMesh segment = new TriangleMesh(points, indices, true);
            segment.init(gl);
            meshes.add(segment);
        }
    }

    @Override
    public void mouseClicked(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {

    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {

    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        myMousePoint = new Point2D(mouseEvent.getX(), mouseEvent.getY());
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        Point2D p = new Point2D(mouseEvent.getX(), mouseEvent.getY());

        if (myMousePoint != null) {
            float dx = p.getX() - myMousePoint.getX();
            float dy = p.getY() - myMousePoint.getY();

            rotateY += dx * ROTATION_SCALE;
            rotateX += dy * ROTATION_SCALE;

        }
        myMousePoint = p;
    }

    @Override
    public void mouseWheelMoved(MouseEvent mouseEvent) {

    }
}
