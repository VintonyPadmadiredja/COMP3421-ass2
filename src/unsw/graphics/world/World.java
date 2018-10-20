package unsw.graphics.world;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import unsw.graphics.*;
import unsw.graphics.geometry.Point2D;
import unsw.graphics.geometry.Point3D;
import unsw.graphics.scene.MathUtil;


/**
 * COMMENT: Comment Game 
 *
 * @author malcolmr
 */
public class World extends Application3D implements KeyListener {

    private Terrain terrain;
    private Avatar avatar;

    private final float MINIMUM_ALTITUDE = 1.5f;
    private final float TRANSITION_ALTITUDE_THRESHOLD = 0.5f;
    private final float TRANSITION_ALTITUDE_SCALE = 0.25f;

    private final float ROTATION_SCALE = 2f;
    private final float TRANSLATION_SCALE = 0.2f;

    private final float CAMERA_UP = 1;
    private final float CAMERA_BACK = 5;

    private final float DAY_CYCLE_IN_MILLISECONDS = 20000f;
    private final float SUN_CENTRE_X = 0;
    private final float SUN_CENTRE_Y = 0;

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
    private Texture avatarTexture;
    private Texture roadTexture;

    private boolean avatarView; //False == Third person view
    private boolean nightTime;
    private boolean isRaining;
    private boolean dayNightMode = false;

    private Color ambientIntesity = new Color(0.5f, 0.5f, 0.5f);
    private Color diffuseCoeff = new Color(0.8f, 0.8f, 0.8f);
    private Color specularCoeff = new Color(0.2f, 0.2f, 0.2f);

    private ParticleSystem rain;
    private Point3D initialSunPosition;
    private Point3D sunPosition;
    private float sunRadius;
    private float sunAngle;
    private Color sunColor = Color.WHITE;
    private long startTime = System.currentTimeMillis();

    public World(Terrain terrain) {
    	super("Assignment 2", 800, 600);
        this.terrain = terrain;
        this.avatar = new Avatar();
        this.avatarView = false;
        this.nightTime = false;
        this.isRaining = false;
        this.rain = new ParticleSystem(terrain.getWidth(), terrain.getDepth());

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
        avatar.init(gl);
        rain.init(gl);

        sunPosition = terrain.getSunlight().asPoint3D();
        initialSunPosition = terrain.getSunlight().asPoint3D();

        double x = sunPosition.getX();
        double y = sunPosition.getY();
        double z = sunPosition.getZ();
        sunRadius = (float) Math.sqrt(x*x + y*y + z*z);

        // Initialise textures
        terrainTexture = new Texture(gl, "res/textures/grass.jpg", "jpg", true);
        treeTexture = new Texture(gl, "res/textures/tree.bmp", "bmp", true);
        avatarTexture = new Texture(gl, "res/textures/bunny.jpg", "jpg", true);
        roadTexture = new Texture(gl, "res/textures/road.jpg", "jpg", true);

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
        if (dayNightMode) {
            updateSunPosition();
            updateSunColor();
        }

        Shader.setPoint3D(gl, "lightPos", sunPosition);
        Shader.setColor(gl, "lightIntensity", sunColor);
        Shader.setColor(gl, "ambientIntensity", ambientIntesity);

        // Set the material properties
        Shader.setColor(gl, "ambientCoeff", Color.WHITE);
        Shader.setColor(gl, "diffuseCoeff", diffuseCoeff);
        Shader.setColor(gl, "specularCoeff", specularCoeff);
        Shader.setFloat(gl, "phongExp", 16f);

        if (nightTime) {
            Shader.setFloat(gl, "torchEnabled", 1f);
            Shader.setPoint3D(gl, "torchLightDirection", new Point3D(0, 0, -1));
            Shader.setColor(gl, "torchDiffuseCoeff", new Color(0.8f, 0.8f, 0.8f));
            Shader.setColor(gl, "torchSpecularCoeff", new Color(0.5f, 0.5f, 0.5f));
            Shader.setPoint3D(gl, "cameraPos", new Point3D(cameraX, cameraY, cameraZ));
            Shader.setFloat(gl, "cutoff", 12.5f);
            Shader.setFloat(gl, "attenuationExp", 128f);
            Shader.setFloat(gl, "constant", 1f);
            Shader.setFloat(gl, "linear", 0.008f);
            Shader.setFloat(gl, "quadratic", 0.005f);
        } else {
            Shader.setFloat(gl, "torchEnabled", 0f);
        }

        // Camera
        CoordFrame3D view;
        if (avatarView) {
            view = CoordFrame3D.identity().rotateY(cameraRotationY).translate(-cameraX, -cameraY, -cameraZ);
        } else {
            view = CoordFrame3D.identity().translate(0, -CAMERA_UP, -CAMERA_BACK)
                    .rotateY(cameraRotationY).translate(-cameraX, -cameraY, -cameraZ);
        }

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

        // Use Avatar texture and draw Avatar
        useTexture(gl, avatarTexture);
        avatar.draw(gl,frame);

        // Use Road texture and draw Roads
        useTexture(gl, roadTexture);
        terrain.drawRoads(gl, frame);

        if (isRaining) {
            rain.draw(gl, frame);
        }
	}

    private void updateSunColor() {
        float angle = (float) Math.toDegrees(sunAngle);
        if (angle < 10)
            sunColor = Color.WHITE;
        else if (angle < 20)
            sunColor = Color.WHITE;
        else if (angle < 30)
            sunColor = Color.WHITE;
        else if (angle < 40)
            sunColor = Color.WHITE;
        else if (angle < 50)
            sunColor = Color.WHITE;
        else if (angle < 60)
            sunColor = Color.WHITE;
        else if (angle < 70)
            sunColor = Color.WHITE;
        else if (angle < 80)
            sunColor = Color.WHITE;
        else if (angle < 90)
            sunColor = Color.WHITE;
        else if (angle < 100)
            sunColor = Color.WHITE;
        else if (angle < 110)
            sunColor = Color.WHITE;
        else if (angle < 120)
            sunColor = Color.WHITE;
        else if (angle < 130)
            sunColor = Color.WHITE;
        else if (angle < 140)
            sunColor = Color.WHITE;
        else if (angle < 150)
            sunColor = Color.WHITE;
        else if (angle < 160)
            sunColor = Color.WHITE;
        else if (angle < 170)
            sunColor = Color.WHITE;
        else if (angle < 180)
            sunColor = Color.WHITE;
        else if (angle < 190)
            sunColor = Color.WHITE;
        else if (angle < 200)
            sunColor = Color.WHITE;
        else if (angle < 210)
            sunColor = Color.WHITE;
        else if (angle < 220)
            sunColor = Color.WHITE;
        else if (angle < 230)
            sunColor = Color.WHITE;
        else if (angle < 240)
            sunColor = Color.WHITE;
        else if (angle < 250)
            sunColor = Color.WHITE;
        else if (angle < 260)
            sunColor = Color.WHITE;
        else if (angle < 270)
            sunColor = Color.WHITE;
        else if (angle < 280)
            sunColor = Color.WHITE;
        else if (angle < 290)
            sunColor = Color.WHITE;
        else if (angle < 300)
            sunColor = Color.WHITE;
        else if (angle < 310)
            sunColor = Color.WHITE;
        else if (angle < 320)
            sunColor = Color.WHITE;
        else if (angle < 330)
            sunColor = Color.WHITE;
        else if (angle < 340)
            sunColor = Color.WHITE;
        else if (angle < 350)
            sunColor = Color.WHITE;
        else if (angle < 360)
            sunColor = Color.WHITE;
    }

    private void updateSunPosition() {
        // Rotate sun around the centre (0,0)
        // X := originX + cos(angle)*radius;
        // Y := originY + sin(angle)*radius;

        // Calculate angle
        long elapsedTime = System.currentTimeMillis() - startTime;
        float percentageDay = ((float) elapsedTime % DAY_CYCLE_IN_MILLISECONDS) / DAY_CYCLE_IN_MILLISECONDS;
        sunAngle = (float) Math.toRadians(360 * percentageDay);

        float newX = SUN_CENTRE_X + ((float) Math.cos((double) sunAngle)*sunRadius);
        float newY = SUN_CENTRE_Y + ((float) Math.sin((double) sunAngle)*sunRadius);

        sunPosition = new Point3D(newX, newY, sunPosition.getZ());
    }

    @Override
	public void destroy(GL3 gl) {
		super.destroy(gl);
		rain.destroy(gl);
		avatar.destroy(gl);
		terrain.destroyRoads(gl);
        terrain.destroyTrees(gl);
        terrain.destroyTerrain(gl);
		terrainTexture.destroy(gl);
		treeTexture.destroy(gl);
		roadTexture.destroy(gl);
	}

    /**
     * Change which texture to use for rendering from now onwards
     * @param gl
     * @param texture - New texture to use
     */
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
                // move camera's position up
                cameraX += lineOfSightX * TRANSLATION_SCALE;
                cameraZ += lineOfSightZ * TRANSLATION_SCALE;
                updateCameraAltitude();
                // set avatar's position to camera's position
                avatar.updatePosition(cameraX, cameraY - MINIMUM_ALTITUDE, cameraZ);
                break;
            case KeyEvent.VK_DOWN:
                // move camera's position down
                cameraX -= lineOfSightX * TRANSLATION_SCALE;
                cameraZ -= lineOfSightZ * TRANSLATION_SCALE;
                updateCameraAltitude();
                // set avatar's position to camera's position
                avatar.updatePosition(cameraX, cameraY - MINIMUM_ALTITUDE, cameraZ);
                break;
            case KeyEvent.VK_LEFT:
                cameraRotationY -= ROTATION_SCALE;
                avatar.rotate(ROTATION_SCALE);
                lineOfSightX = (float) Math.sin(Math.toRadians(MathUtil.normaliseAngle(cameraRotationY)));
                lineOfSightZ = (float) -Math.cos(Math.toRadians(MathUtil.normaliseAngle(cameraRotationY)));
                break;
            case KeyEvent.VK_RIGHT:
                cameraRotationY += ROTATION_SCALE;
                avatar.rotate(-ROTATION_SCALE);
                lineOfSightX = (float) Math.sin(Math.toRadians(MathUtil.normaliseAngle(cameraRotationY)));
                lineOfSightZ = (float) -Math.cos(Math.toRadians(MathUtil.normaliseAngle(cameraRotationY)));
                break;
            case KeyEvent.VK_A:
                avatarView = !avatarView;
                break;
            case KeyEvent.VK_N:
                nightTime = !nightTime;
                if (nightTime) {
                    ambientIntesity = new Color(0.4f, 0.4f, 0.4f);
                    diffuseCoeff = new Color(0.1f, 0.1f, 0.1f);
                    specularCoeff = new Color(0.1f, 0.1f, 0.1f);
                } else {
                    ambientIntesity = new Color(0.5f, 0.5f, 0.5f);
                    diffuseCoeff = new Color(0.8f, 0.8f, 0.8f);
                    specularCoeff = new Color(0.2f, 0.2f, 0.2f);
                }
                break;
            case KeyEvent.VK_R:
                isRaining = !isRaining;
                break;
            case KeyEvent.VK_SPACE:
                dayNightMode = !dayNightMode;

                // Reset Sun's position
                sunPosition = initialSunPosition;
                startTime = System.currentTimeMillis();
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
