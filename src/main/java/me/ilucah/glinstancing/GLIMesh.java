package me.ilucah.glinstancing;

import org.joml.Vector2f;

public abstract class GLIMesh {

    protected Vector2f position;
    private final float rotation, scale;

    /**
     * Abstraction over the SceneObject class to include basic functionality
     * for handling a mesh in a way that is useful for instancing.
     *
     * This class provides position, rotation, and scale so that it's data can be
     * constructed per instance in the vertex shader.
     * @param position world space position.
     * @param rotation local space rotation.
     * @param scale local space scale.
     */
    public GLIMesh(Vector2f position, float rotation, float scale) {
        this.position = position;
        this.rotation = rotation;
        this.scale = scale;
    }

    /**
     * A method useful for updating the position, rotation, and or scale of the object/mesh with each frame.
     */
    public abstract void update();

    /**
     * Provides the necessary buffers/information needed to render the mesh.
     *
     * @return a mesh object containing the vertex buffer, index buffer, and index buffer length
     */
    public abstract GLIMeshBuffer createMesh();

    public Vector2f getPosition() {
        return position;
    }

    public float getRotation() {
        return rotation;
    }

    public float getScale() {
        return scale;
    }
}