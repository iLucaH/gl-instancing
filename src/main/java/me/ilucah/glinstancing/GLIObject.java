package me.ilucah.glinstancing;

import static org.lwjgl.opengl.GL40.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import comp3170.GLBuffers;
import comp3170.Shader;
import comp3170.ShaderLibrary;

public class GLIObject<T extends GLIMesh> {

    // Local class variables
    private final List<T> sceneObjects;
    private final List<Vector2f> positions;
    private final List<Float> rotations;
    private final List<Float> scales;
    private final List<GLIAttribute<?>> customAttributes;

    // Builder variables
    private String vertexShader, fragmentShader;
    private Consumer<Shader> onDraw;

    // Built variables
    private boolean built = false;
    private Shader shader;
    private int vertexBuffer, indexBuffer, indexBufferLength;
    private int positionBuffer;
    private int rotationBuffer;
    private int scaleBuffer;

    public GLIObject() {
        sceneObjects = new ArrayList<>();
        positions = new ArrayList<>();
        rotations = new ArrayList<>();
        scales = new ArrayList<>();
        customAttributes = new ArrayList<>();
    }

    // Builder Functions

    public GLIObject<T> withFragShader(String fragmentShader) {
        this.fragmentShader = fragmentShader;
        return this;
    }

    public GLIObject<T> withVertexShader(String vertexShader) {
        this.vertexShader = vertexShader;
        return this;
    }

    /**
     * Add a scene object to be instanced.
     *
     * @param t the scene object to add
     * @throws NullPointerException if t is null
     * @return the current instance being built
     */
    public GLIObject<T> withSceneObject(T t) {
        Objects.requireNonNull(t);
        sceneObjects.add(t);
        return this;
    }

    /**
     * Add a list of scene object to be instanced.
     *
     * @param list the list of scene objects to add
     * @throws NullPointerException if the list is null
     * @return the current instance being built
     */
    public GLIObject<T> withSceneObjects(List<T> list) {
        Objects.requireNonNull(list);
        list.stream().filter(Objects::nonNull).forEach(sceneObjects::add);
        return this;
    }

    /**
     * Add a list of scene object to be instanced using a lambda function.
     *
     * @param supplier the functional expression to provide a list of scene objects to be added
     * @throws NullPointerException if the supplier function is null
     * @return the current instance being built
     */
    public GLIObject<T> withSceneObjects(Supplier<List<T>> supplier) {
        Objects.requireNonNull(supplier);
        return withSceneObjects(supplier.get());
    }

    /**
     * Set a custom method to be called just before the instanced objects are drawn.
     *
     * Used for setting custom uniforms such as colour, time, etc.
     *
     * @param onDraw a consumer function that takes in the already enabled shader as the input value
     * @throws NullPointerException if the consumer function is null
     * @return the current instance being built
     */
    public GLIObject<T> withCustomDrawMethod(Consumer<Shader> onDraw) {
        Objects.requireNonNull(onDraw);
        this.onDraw = onDraw;
        return this;
    }

    /**
     * Add a custom instanced attribute for each instance.
     *
     * Used for adding additional attributes such as per instance colours.
     * Currently only supports Float types and Vector3f types because that's all that is needed for this project.
     *
     * @param customAttribute the custom attribute object to be added
     * @throws NullPointerException if the CustomAttribute object is null
     * @return the current instance being built
     */
    public GLIObject<T> withCustomInstancedAttribute(GLIAttribute<?> customAttribute) {
        Objects.requireNonNull(customAttribute);
        customAttributes.add(customAttribute);
        return this;
    }

    /**
     * Compiles the shader and compiles all attributes into buffers ready for rendering.
     *
     * @return false if not ready for compilation. true if all was complete.
     */
    public boolean build() {
        if (fragmentShader == null || vertexShader == null) // not ready to build yet.
            return false;
        shader = ShaderLibrary.instance.compileShader(vertexShader, fragmentShader);

        T t = populateArrays();

        GLIMeshBuffer mesh = t.createMesh();
        this.vertexBuffer = mesh.vertexBuffer();
        this.indexBuffer = mesh.indexBuffer();
        this.indexBufferLength = mesh.indexBufferLength();
        positionBuffer = GLBuffers.createBuffer(positions.toArray(new Vector2f[positions.size()])); // Turning the arraylist into an array to be parsed as an argument when creating the buffer.
        rotationBuffer = GLBuffers.createBuffer(floatArrayCast(rotations, 0.0f), GL_FLOAT);
        scaleBuffer = GLBuffers.createBuffer(floatArrayCast(scales, 0.0f), GL_FLOAT);
        bufferCustomAttributeData(false);
        built = true;
        return true;
    }

    // Class Methods

    /**
     * Collects all InstancedMesh data into arrays.
     *
     * This method is used to populate the arrays so that they can be later turned into OpenGL Buffers.
     * This method returns an InstancedMesh object so that {@link GLIMesh#createMesh()} can be called on an InstancedMesh object.
     * It does not matter which InstancedMesh object is returned, any is fine.
     *
     * @return the first available InstancedMesh object. null if there are no objects available.
     */
    private T populateArrays() {
        T firstAvailableObject = null; // Store an InstancedMesh object to be returned.
        for (T t : sceneObjects) {
            if (t == null) // Null check just in case, but all scene objects assert NonNull anyway.
                continue;
            firstAvailableObject = t;
            positions.add(t.getPosition());
            rotations.add(t.getRotation());
            scales.add(t.getScale());
            for (GLIAttribute<?> customAttribute : customAttributes) {
                customAttribute.populate(t);
            }
        }
        return firstAvailableObject;
    }

    /**
     * Updates all buffers taking into account any InstancedMesh data that may have changed.
     *
     * This method clears all data in the arrays, populates them again taking into account any
     * data which may have been updated since last render/update pass, and then pushes the update
     * to the OpenGL buffer using GLBuffers#updateBuffer.
     */
    public void updateBuffers() {
        if (!built)
            return;
        positions.clear();
        rotations.clear();
        scales.clear();
        for (GLIAttribute<?> customAttribute : customAttributes) {
            customAttribute.getData().clear();
        }

        populateArrays();

        GLBuffers.updateBuffer(positionBuffer, positions.toArray(new Vector2f[0])); // Turning the arraylist into an array to be parsed as an argument when creating the buffer.
        GLBuffers.updateBuffer(rotationBuffer, floatArrayCast(rotations, 0.0f), GL_FLOAT);
        GLBuffers.updateBuffer(scaleBuffer, floatArrayCast(scales, 0.0f), GL_FLOAT);
        bufferCustomAttributeData(true);
    }

    /**
     * Calls the abstract update method for each InstancedMesh sceneObject that is being rendered.
     *
     * Iterates all sceneObjects, updates them, and then updates all buffers to account for any
     * changes that may have been made to each InstancedMesh sceneObject's data within it's update method.
     */
    public void update() {
        sceneObjects.forEach(GLIMesh::update);
        updateBuffers();
    }

    /**
     * Draws all InstancedMesh sceneObjects in one draw call using instancing design pattern.
     *
     * This method enables the shader, sets all attributes and uniforms, and then draws all elements.
     * The custom draw method is called here {@link GLIObject#onDraw}.
     *
     * @param vpMatrix the view projection matrix of the scene.
     */
    public void drawAll(Matrix4f vpMatrix) {
        if (!built)
            return;
        shader.enable();
        // call the custom onDraw function if it has been added
        if (onDraw != null)
            onDraw.accept(shader);
        shader.setAttribute("a_position", vertexBuffer);
        shader.setUniform("u_vpMatrix", vpMatrix);

        // Pass through all the instanced attributes.
        shader.setAttribute("a_worldPos", positionBuffer);
        glVertexAttribDivisor(shader.getAttribute("a_worldPos"), 1);

        shader.setAttribute("a_rotation", rotationBuffer);
        glVertexAttribDivisor(shader.getAttribute("a_rotation"), 1);

        shader.setAttribute("a_scale", scaleBuffer);
        glVertexAttribDivisor(shader.getAttribute("a_scale"), 1);

        for (GLIAttribute<?> customAttribute : customAttributes) {
            shader.setAttribute(customAttribute.getName(), customAttribute.getBuffer());
            glVertexAttribDivisor(shader.getAttribute(customAttribute.getName()), 1);
        }

        // draw using an index buffer
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer);
        glDrawElementsInstanced(GL_TRIANGLES, indexBufferLength, GL_UNSIGNED_INT, 0, positions.size());

        // set back to non-instance based drawing
        glVertexAttribDivisor(shader.getAttribute("a_worldPos"), 0);
        glVertexAttribDivisor(shader.getAttribute("a_rotation"), 0);
        glVertexAttribDivisor(shader.getAttribute("a_scale"), 0);
        for (GLIAttribute<?> customAttribute : customAttributes) {
            glVertexAttribDivisor(shader.getAttribute(customAttribute.getName()), 0);
        }
    }

    /**
     * Sets or updates the buffers for all custom attributes that have been added with {@link GLIObject#bufferCustomAttributeData(boolean)}.
     *
     * This method updates and sets buffers based on the class type of the CustomAttribute object.
     * Currently there is only need for Float and Vector3f types for this project, so that is all that is implemented.
     *
     * @param update true if the buffers are being updated, false if the buffers are being set
     * @throws RuntimeException if the attribute type is not Float or Vector3f
     */
    private void bufferCustomAttributeData(boolean update) {
        for (GLIAttribute<?> customAttribute : customAttributes) {
            // If I needed to set more types of custom Attributes other than float and vec3, I would put that here.
            if (customAttribute.getType() == Float.class) { // This if check allows for type safety.
                GLIAttribute<Float> floatAttribute = (GLIAttribute<Float>) customAttribute; // This type safety warning is mitigated with the above type check if statement.
                float[] bufferArray = floatArrayCast(floatAttribute.getData(), floatAttribute.getDefaultDataValue()); // convert the float arraylist to regular array.
                if (update) {
                    GLBuffers.updateBuffer(floatAttribute.getBuffer(), bufferArray, GL_FLOAT);
                } else {
                    floatAttribute.setBuffer(GLBuffers.createBuffer(bufferArray, GL_FLOAT));
                }
            } else if (customAttribute.getType() == Vector3f.class) {
                GLIAttribute<Vector3f> vec3fAttribute = (GLIAttribute<Vector3f>) customAttribute;
                Vector3f[] bufferArray = vec3fAttribute.getData().toArray(new Vector3f[0]);
                if (update) {
                    GLBuffers.updateBuffer(vec3fAttribute.getBuffer(), bufferArray);
                } else {
                    vec3fAttribute.setBuffer(GLBuffers.createBuffer(bufferArray));
                }
            } else {
                throw new RuntimeException("Custom Attribute set to an unsupported type: " + customAttribute.getType().getCanonicalName());
            }
        }
    }

    /**
     * Helper method for casting an ArrayList of floats to a float array.
     *
     * @param arrayList the array list being converted from
     * @param defaultValue a default value to be stored if the value in the array list is null
     * @return a float array converted from a float array list
     */
    private float[] floatArrayCast(List<Float> arrayList, float defaultValue) {
        if (arrayList == null)
            return new float[0];
        float[] arr = new float[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            Float value = arrayList.get(i);
            arr[i] = value == null ? defaultValue : value;
        }
        return arr;
    }

    /**
     * @return All InstancedMesh sceneObjects that have been added to the builder.
     */
    public List<T> getSceneObjects() {
        return sceneObjects;
    }

}