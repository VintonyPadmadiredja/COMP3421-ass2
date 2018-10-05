package unsw.graphics.world;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.opengl.GL;
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
    private final float TRANSITION_ALTITUDE_THRESHOLD = 0.5f;
    private final float TRANSITION_ALTITUDE_SCALE = 0.25f;

    private final float ROTATION_SCALE = 2f;
    private final float TRANSLATION_SCALE = 0.2f;

    private float cameraX = 0;
    private float cameraY = MINIMUM_ALTITUDE;
    private float cameraZ = 0;
    private float cameraRotationY = 90;

    private float terrainRotationY = 0;
    private float terrainScale = 1;
    private Point3D terrainTranslation = new Point3D(0, 0, 0);

    private float lineOfSightX = 1;
    private float lineOfSightZ = 0;

    private Texture terrainTexture;
    private Texture treeTexture;

    public World(Terrain terrain) {
    	super("Assignment 2", 800, 600);
        this.terrain = terrain;

        cameraY += (float) terrain.getGridAltitude(0, 0);
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
    public void init(GL3 gl) {
        super.init(gl);
        getWindow().addKeyListener(this);
        terrain.makeTerrain(gl);

        // Initialise textures
        terrainTexture = new Texture(gl, "res/textures/grass.jpg", "jpg", true);
        treeTexture = new Texture(gl, "res/textures/tree.bmp", "bmp", true);

        // Initialise shader
        Shader shader = new Shader(gl, "shaders/vertex_tex_phong_world.glsl",
                "shaders/fragment_tex_phong_world.glsl");
        shader.use(gl);

    }

	@Override
	public void display(GL3 gl) {
		super.display(gl);

        // Set wrap mode for texture in S direction
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);

        // Set wrap mode for texture in T direction
        gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T, GL3.GL_REPEAT);

        // Set the lighting properties
        Shader.setPoint3D(gl, "lightPos", terrain.getSunlight().asPoint3D());
        Shader.setColor(gl, "lightIntensity", Color.WHITE);
        Shader.setColor(gl, "ambientIntensity", new Color(0.5f, 0.5f, 0.5f));

        // Set the material properties
        Shader.setColor(gl, "ambientCoeff", Color.WHITE);
        Shader.setColor(gl, "diffuseCoeff", new Color(0.8f, 0.8f, 0.8f));
        Shader.setColor(gl, "specularCoeff", new Color(0.2f, 0.2f, 0.2f));
        Shader.setFloat(gl, "phongExp", 16f);

        // Camera
        CoordFrame3D view = CoordFrame3D.identity().rotateY(cameraRotationY).translate(-cameraX, -cameraY, -cameraZ);
        Shader.setViewMatrix(gl, view.getMatrix());

        // Terrain coordinate frame
        CoordFrame3D frame = CoordFrame3D.identity().translate(terrainTranslation).rotateY(terrainRotationY)
                .scale(terrainScale, terrainScale, terrainScale);

		// Use Terrain texture and draw Terrain
		useTexture(gl, terrainTexture);
        terrain.drawTerrain(gl, frame);

        // Use Tree texture and draw Trees
        useTexture(gl, treeTexture);
        terrain.drawTrees(gl, frame);
	}

	@Override
	public void destroy(GL3 gl) {
		super.destroy(gl);
        terrain.destroyTrees(gl);
        terrain.destroyTerrain(gl);
		terrainTexture.destroy(gl);
		treeTexture.destroy(gl);
	}

    private void useTexture(GL3 gl, Texture texture) {
        Shader.setPenColor(gl, Color.WHITE);
        Shader.setInt(gl, "tex", 0);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, texture.getId());
    }

	@Override
	public void reshape(GL3 gl, int width, int height) {
        super.reshape(gl, width, height);
        Shader.setProjMatrix(gl, Matrix4.perspective(60, width/(float)height, 0.01f, 100));
	}

    @Override
    public void keyPressed(KeyEvent keyEvent) {
        switch (keyEvent.getKeyCode()) {
            case KeyEvent.VK_UP:
                cameraX += lineOfSightX * TRANSLATION_SCALE;
                cameraZ += lineOfSightZ * TRANSLATION_SCALE;
                updateCameraAltitude();
                break;
            case KeyEvent.VK_DOWN:
                cameraX -= lineOfSightX * TRANSLATION_SCALE;
                cameraZ -= lineOfSightZ * TRANSLATION_SCALE;
                updateCameraAltitude();
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

    /**
     * Update camera's altitude (Y value) to follow terrain when moving up/down hills
     */
    private void updateCameraAltitude() {
        Point3D cameraPosition = getCameraPositionInTerrain();
        if (insideTerrain(cameraPosition)) {
            float tempY = terrain.altitude(cameraPosition.getX(), cameraPosition.getZ()) + MINIMUM_ALTITUDE;
            // To smoothen transition between altitudes
            // Specifically when entering/exiting the terrain
            if (tempY - cameraY > TRANSITION_ALTITUDE_THRESHOLD)
                cameraY += (tempY - cameraY) * TRANSITION_ALTITUDE_SCALE;
            else
                cameraY = tempY;
        } else {
            float tempY = MINIMUM_ALTITUDE;
            if (cameraY - tempY > TRANSITION_ALTITUDE_THRESHOLD)
                cameraY -= (cameraY - tempY) * TRANSITION_ALTITUDE_SCALE;
            else
                cameraY = tempY;
        }
    }

    /**
     * Check if camera's position is inside the terrain
     * @param cameraPosition
     * @return boolean
     */
    private boolean insideTerrain(Point3D cameraPosition) {
        return cameraPosition.getX() >= 0 && cameraPosition.getZ() >= 0
                && cameraPosition.getX() <= terrain.getWidth()-1
                && cameraPosition.getZ() <= terrain.getDepth()-1;
    }

    /**
     * Get the camera's position within the Terrain's coordinate system
     * @return Point3D
     */
    private Point3D getCameraPositionInTerrain() {
        Matrix4 inv = getTerrainInverseModelMatrix();
        Point3D cameraTranslation = new Point3D(cameraX, cameraY, cameraZ);
        return inv.multiply(cameraTranslation.asHomogenous()).asPoint3D();
    }

    /**
     * Get the terrain's inverse model matrix
     * @return Matrix4
     */
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
