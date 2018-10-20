package unsw.graphics.world;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import unsw.graphics.CoordFrame3D;
import unsw.graphics.Shader;
import unsw.graphics.Texture;
import unsw.graphics.geometry.TriangleMesh;

import java.awt.*;
import java.io.IOException;
import java.util.Random;

public class Particle {
    private static final float MODEL_SCALE = 0.02f;

    // Initial speed for all the particles
    private static float speedYGlobal = 0.1f;

    float life; // how alive it is
    float r, g, b; // color
    float x, y, z; // position
    float speedX, speedY, speedZ; // speed in the direction

    private TriangleMesh model;
    private Texture texture;

    private Random rand = new Random();

    // Constructor
    public Particle(int terrainWidth, int terrainDepth) {
        // position the rain
        x = (rand.nextFloat()*10 % terrainWidth);
        y = (rand.nextFloat()*10 % 10); // TODO: Find max altitude
        z = (rand.nextFloat()*10 % terrainDepth);
        // Generate a random speed and direction in polar coordinate, then
        // resolve
        // them into x and y.
        float speed = 0.025f;
        float angle = (float) Math.toRadians(45);

        speedY = speed * (float) Math.sin(angle) + speedYGlobal;

        // Initially it's fully alive
        life = 1.0f;
    }

    public void init(GL3 gl) {
        try {
            // Initialise tree model
            model = new TriangleMesh("res/models/cube.ply");
            model.init(gl);

        } catch (IOException e) {
            e.printStackTrace();
        }

        texture = new Texture(gl, "res/textures/rain.jpg", "jpg", false);
    }

    public void draw(GL3 gl, CoordFrame3D frame) {
        Shader.setPenColor(gl, Color.WHITE);

        Shader.setInt(gl, "tex", 0);
        gl.glActiveTexture(GL.GL_TEXTURE0);
        gl.glBindTexture(GL.GL_TEXTURE_2D, texture.getId());

        CoordFrame3D particleFrame = frame.translate(x,y,z).scale(MODEL_SCALE,MODEL_SCALE,MODEL_SCALE);

        model.draw(gl, particleFrame);
    }

    public void destroy(GL3 gl) {
        model.destroy(gl);
        texture.destroy(gl);
    }

}
