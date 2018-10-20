package unsw.graphics.world;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import unsw.graphics.*;
import unsw.graphics.geometry.TriangleMesh;

import java.awt.*;
import java.io.IOException;
import java.util.Random;

public class ParticleSystem {

    private static final int MAX_PARTICLES = 200; // max number of particles
    private Particle[] particles = new Particle[MAX_PARTICLES];

    // Pull forces in each direction
    private static float gravityY = -0.0008f; // gravity

    // Initial speed for all the particles
    private static float speedYGlobal = 0.1f;

    private TriangleMesh model;
    private Texture texture;

    private int terrainWidth;
    private int terrainDepth;

    public ParticleSystem(int width, int depth) {
        this.terrainWidth = width;
        this.terrainDepth = depth;
    }

    public void init(GL3 gl) {
        try {
            // Initialise rain  model
            model = new TriangleMesh("res/models/sphere.ply");
            model.init(gl);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // load texture once
        texture = new Texture(gl, "res/textures/rain.jpg", "jpg", false);

        // Initialize the particles
        for (int i = 0; i < MAX_PARTICLES; i++) {
            particles[i] = new Particle();
        }
    }

    public void draw(GL3 gl, CoordFrame3D frame) {
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendColor(1.0f,1.0f,1.0f,0.5f);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);
        gl.glDisable(GL.GL_DEPTH_TEST);

        // Update the particles
        for (int i = 0; i < MAX_PARTICLES; i++) {
            // Move the particle
            particles[i].y -= Math.abs(particles[i].speedY);

            // Apply the gravity force on y-axis
            particles[i].speedY += gravityY;

            // Slowly kill it
            particles[i].life -= 0.002;

            if (particles[i].y <= 0) {
                particles[i].life = -1.0f;
            }

            particles[i].draw(gl, frame);

            // Revive particle -- loop
            if (particles[i].life < 0.0) {
                particles[i] = new Particle();
            }
        }
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDisable(GL.GL_BLEND);
    }

    public void destroy(GL3 gl) {
        model.destroy(gl);
        texture.destroy(gl);
    }

    // Particle (inner class)
    class Particle {
        private static final float MODEL_SCALE = 0.0002f;
        private static final float SPEED = 0.025f;

        float life; // how alive it is
        float x, y, z; // position
        float speedY; // speed in the y direction

        private Random rand = new Random();

        // Constructor
        public Particle() {
            // position the rain
            x = (rand.nextFloat() * 10 % terrainWidth);
            y = (rand.nextFloat() * 10 % 10);
            z = (rand.nextFloat() * 10 % terrainDepth);

            float angle = (float) Math.toRadians(45);

            speedY = SPEED * (float) Math.sin(angle) + speedYGlobal;

            // Initially it's fully alive
            life = 1.0f;
        }

        public void draw(GL3 gl, CoordFrame3D frame) {
            Shader.setPenColor(gl, Color.WHITE);

            Shader.setInt(gl, "tex", 0);
            gl.glActiveTexture(GL.GL_TEXTURE0);
            gl.glBindTexture(GL.GL_TEXTURE_2D, texture.getId());

            CoordFrame3D particleFrame = frame.translate(x, y, z).scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

            model.draw(gl, particleFrame);
        }
    }
}
