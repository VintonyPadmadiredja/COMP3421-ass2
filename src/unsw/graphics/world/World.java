package unsw.graphics.world;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import unsw.graphics.*;
import unsw.graphics.geometry.Point2D;
import unsw.graphics.geometry.Point3D;
import unsw.graphics.geometry.TriangleMesh;
import unsw.graphics.scene.MathUtil;


/**
 * COMMENT: Comment Game 
 *
 * @author malcolmr
 */
public class World extends Application3D implements KeyListener, MouseListener {

    private Terrain terrain;

    private float cameraX = 0;
    private float cameraY = 2;
    private float cameraZ = 0;
    private float cameraRotationY = 0;

    private float terrainRotationY = 90;
    private float terrainScale = 1;
    private Point3D terrainTranslation = new Point3D(0, 0, 0);

    private float lineOfSightX = 0;
    private float lineOfSightZ = -1;

    private float rotateX = 0;
    private float rotateY = 0;
    private Point2D myMousePoint = null;
    private static final int ROTATION_SCALE = 1;
    private Texture terrainTexture;


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
        CoordFrame3D view = CoordFrame3D.identity().rotateY(cameraRotationY).translate(-cameraX, -cameraY, -cameraZ);
        Shader.setViewMatrix(gl, view.getMatrix());
//        System.out.println("x = " + cameraX +" y = " + cameraY + " z = " + cameraZ);

        CoordFrame3D frame = CoordFrame3D.identity().rotateY(terrainRotationY)
            .rotateX(rotateX).rotateY(rotateY);
        Shader.setPenColor(gl, Color.GRAY);

        terrain.drawTerrain(gl, frame);

	}

	@Override
	public void destroy(GL3 gl) {
		super.destroy(gl);

	}

	@Override
	public void init(GL3 gl) {
		super.init(gl);
        getWindow().addKeyListener(this);
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

        Shader terrainShader = new Shader(gl, "shaders/vertex_tex_3d.glsl", "shaders/fragment_tex_3d.glsl");
        terrainShader.use(gl);
        terrainTexture = new Texture(gl, "res/textures/grassTile.bmp", "bmp", false);
        Shader.setInt(gl, "tex", 0);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, terrainTexture.getId());
        terrain.makeTerrain(gl);
    }

	@Override
	public void reshape(GL3 gl, int width, int height) {
        super.reshape(gl, width, height);
        Shader.setProjMatrix(gl, Matrix4.perspective(60, width/(float)height, 1, 100));
	}


    @Override
    public void keyPressed(KeyEvent keyEvent) {
        switch (keyEvent.getKeyCode()) {

            case KeyEvent.VK_UP:
//                cameraZ += 0.2f;
                cameraX += lineOfSightX * 0.2f;
                cameraZ += lineOfSightZ * 0.2f;
                moveCamera();
                break;
            case KeyEvent.VK_DOWN:
//                cameraZ -= 0.2f;
                cameraX -= lineOfSightX * 0.2f;
                cameraZ -= lineOfSightZ * 0.2f;
                moveCamera();
                break;
            case KeyEvent.VK_LEFT:
                cameraRotationY -= 2f;
//                cameraX -= 0.2f;
                lineOfSightX = (float) Math.sin(Math.toRadians(MathUtil.normaliseAngle(cameraRotationY)));
                lineOfSightZ = (float) -Math.cos(Math.toRadians(MathUtil.normaliseAngle(cameraRotationY)));
//                System.out.println("left, camera angle = "+ cameraRotationY);
//                System.out.println("X = " + lineOfSightX + " Z = "+lineOfSightZ);
//                moveCamera();
                break;
            case KeyEvent.VK_RIGHT:
                cameraRotationY += 2f;
//                cameraX += 0.2f;
                lineOfSightX = (float) Math.sin(Math.toRadians(MathUtil.normaliseAngle(cameraRotationY)));
                lineOfSightZ = (float) -Math.cos(Math.toRadians(MathUtil.normaliseAngle(cameraRotationY)));
//                System.out.println("right, camera angle = "+ cameraRotationY);
//                System.out.println("X = " + lineOfSightX + " Z = "+lineOfSightZ);
//                moveCamera();
                break;
            default:
                break;
        }

    }

    private void moveCamera() {
        if (insideTerrain()) {
            Point3D cameraPosition = getCameraPositionInTerrain();
            cameraY = terrain.altitude(cameraPosition.getX(), cameraPosition.getZ()) + 2;
        } else {
            cameraY = 2;
        }
    }

    private boolean insideTerrain() {
        Point3D cameraPosition = getCameraPositionInTerrain();
        return cameraPosition.getX() >= 0 && cameraPosition.getZ() >= 0
                && cameraPosition.getX() <= terrain.getWidth()-1
                && cameraPosition.getZ() <= terrain.getDepth()-1;
    }

    private Point3D getCameraPositionInTerrain() {
        Matrix4 inv = Matrix4.scale(1/terrainScale, 1/terrainScale, 1/terrainScale)
                .multiply(Matrix4.rotationY(-terrainRotationY))
                .multiply(Matrix4.translation(-terrainTranslation.getX(), -terrainTranslation.getY(),
                        -terrainTranslation.getZ()));
        Point3D cameraTranslation = new Point3D(cameraX, cameraY, cameraZ);
        return inv.multiply(cameraTranslation.asHomogenous()).asPoint3D();
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {

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
