package me.ilucah.glinstancing;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class GLIAttribute<T> {

    private final String name;
    private final List<T> data; // The list of data that this attribute will store
    private final float defaultDataValue;
    private final Function<GLIMesh, T> populate;
    private final Class<T> type;

    private Integer buffer;

    /**
     * A class used for creating custom instanced attributes for InstancedObjects
     *
     * After specifying what data type this class will hold, you can set a 'populate'
     * function which is a way of producing the data value for each InstancedMesh object this
     * attribute is relevant for.
     *
     * @param name the name of the attribute (a_colour, etc)
     * @param defaultDataValue used as a default float type when type == Float.class
     * @param type the class of the type. currently only supports Float and Vector3f
     * @param populate a function to produce the data type based on the InstancedMesh object
     */
    public GLIAttribute(String name, float defaultDataValue, Class<T> type, Function<GLIMesh, T> populate) {
        this.name = name;
        this.data = new ArrayList<>();
        this.defaultDataValue = defaultDataValue;
        this.type = type;
        this.populate = populate;
    }

    public GLIAttribute(String name, Class<T> type, Function<GLIMesh, T> populate) {
        this(name, 0.0f, type, populate);
    }

    /**
     * Adds the custom attribute data from the InstancedMesh instance.
     * @param t the InstancedMesh object.
     */
    public void populate(GLIMesh t) {
        data.add(populate.apply(t));
    }

    public String getName() {
        return name;
    }

    public List<T> getData() {
        return data;
    }

    public float getDefaultDataValue() {
        return defaultDataValue;
    }

    public Class<T> getType() {
        return type;
    }

    // Nullable if the buffer hasn't been created yet
    public Integer getBuffer() {
        return buffer;
    }

    public void setBuffer(Integer buffer) {
        this.buffer = buffer;
    }

}