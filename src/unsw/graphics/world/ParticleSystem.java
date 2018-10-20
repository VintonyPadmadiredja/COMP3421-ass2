package unsw.graphics.world;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import unsw.graphics.*;
import unsw.graphics.geometry.TriangleMesh;

import java.awt.*;
import java.io.IOException;
import java.util.Random;

/**
 * Displays fireworks using a particle system. Taken from NeHe Lesson #19a:
 * Fireworks
 * 
 * @author Robert Clifton-Everest
 */
public class ParticleSystem {

    private static final int MAX_PARTICLES = 200; // max number of particles
    private Particle[] particles = new Particle[MAX_PARTICLES];

    // Pull forces in each direction
    private static float gravityY = -0.0008f; // gravity

    private int terrainWidth;
    private int terrainDepth;

    public ParticleSystem(int width, int depth) {
        this.terrainWidth = width;
        this.terrainDepth = depth;
    }

    public void init(GL3 gl) {
        // Initialize the particles
        for (int i = 0; i < MAX_PARTICLES; i++) {
            particles[i] = new Particle(terrainWidth, terrainDepth);
            particles[i].init(gl);
        }
        
//        Shader.setFloat(gl, "gravity", gravityY);
    }

    public void draw(GL3 gl, CoordFrame3D frame) {
        // Update the particles
        for (int i = 0; i < MAX_PARTICLES; i++) {
            // Move the particle
            particles[i].y += -Math.abs(particles[i].speedY);

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
                particles[i].destroy(gl);
                particles[i] = new Particle(terrainWidth, terrainDepth);
                particles[i].init(gl);
            }
        }

//        time++;
    }

    public void destroy(GL3 gl) {
        for (Particle p : particles) {
            p.destroy(gl);
        }
    }
}
