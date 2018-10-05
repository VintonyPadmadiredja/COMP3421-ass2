package unsw.graphics.world;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.opengl.GL3;

import unsw.graphics.*;
import unsw.graphics.geometry.Point3D;
import unsw.graphics.scene.MathUtil;


/**
 * COMMENT: Comment Game 
 *
 * @author malcolmr
 */
public class World extends Application3D implements KeyListener {

    private Terrain terrain;

    private final float MINIMUM_ALTITUDE = 1.8f;

    private float cameraX = 0;
    private float cameraY = MINIMUM_ALTITUDE;
    private float cameraZ = 0;
    private float cameraRotationY = 90;

    private final float ROTATION_SCALE = 2f;
    private final float TRANSLATION_SCALE = 0.2f;

    private float terrainRotationY = 0;
    private float terrainScale = 1;
    private Point3D terrainTranslation = new Point3D(0, 0, 0);

    private float lineOfSightX = 1;
    private float lineOfSightZ = 0;

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

        CoordFrame3D frame = CoordFrame3D.identity();
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
                cameraX += lineOfSightX * TRANSLATION_SCALE;
                cameraZ += lineOfSightZ * TRANSLATION_SCALE;
                moveCameraAltitude();
                break;
            case KeyEvent.VK_DOWN:
                cameraX -= lineOfSightX * TRANSLATION_SCALE;
                cameraZ -= lineOfSightZ * TRANSLATION_SCALE;
                moveCameraAltitude();
                break;
            case KeyEvent.VK_LEFT:
                cameraRotationY -= ROTATION_SCALE;
                lineOfSightX = (float) Math.sin(Math.toRadians(MathUtil.normaliseAngle(cameraRotationY)));
                lineOfSightZ = (float) -Math.cos(Math.toRadians(MathUtil.normaliseAngle(cameraRotationY)));
                break;
            case KeyEvent.VK_RIGHT:
                cameraRotationY += ROTATION_SCALE;
                lineOfSightX = (float) Math.sin(Math.toRadians(MathUtil.normaliseAngle(cameraRotationY)));
                lineOfSightZ = (float) -Math.cos(Math.toRadians(MathUtil.normaliseAngle(cameraRotationY)));
                break;
            default:
                break;
        }

    }

    private void moveCameraAltitude() {
        Point3D cameraPosition = getCameraPositionInTerrain();
        if (insideTerrain(cameraPosition)) {
            float tempY = terrain.altitude(cameraPosition.getX(), cameraPosition.getZ()) + MINIMUM_ALTITUDE;
            if (tempY - cameraY > 0.5f)
                cameraY += (tempY - cameraY) * 0.25f;
            else
                cameraY = tempY;
        } else {
            float tempY = MINIMUM_ALTITUDE;
            if (cameraY - tempY > 0.5f)
                cameraY -= (cameraY - tempY) * 0.25f;
            else
                cameraY = tempY;
        }
    }

    private boolean insideTerrain(Point3D cameraPosition) {
        return cameraPosition.getX() >= 0 && cameraPosition.getZ() >= 0
                && cameraPosition.getX() <= terrain.getWidth()-1
                && cameraPosition.getZ() <= terrain.getDepth()-1;
    }

    private Point3D getCameraPositionInTerrain() {
        Matrix4 inv = getTerrainInverseModelMatrix();
        Point3D cameraTranslation = new Point3D(cameraX, cameraY, cameraZ);
        return inv.multiply(cameraTranslation.asHomogenous()).asPoint3D();
    }

    private Matrix4 getTerrainInverseModelMatrix() {
        return Matrix4.scale(1/terrainScale, 1/terrainScale, 1/terrainScale)
                .multiply(Matrix4.rotationY(-terrainRotationY))
                .multiply(Matrix4.translation(-terrainTranslation.getX(), -terrainTranslation.getY(),
                        -terrainTranslation.getZ()));
    }

    @Override
    public void keyReleased(KeyEvent keyEvent) {

    }
}
