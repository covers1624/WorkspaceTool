package net.covers1624.wstool.json;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Created by covers1624 on 21/10/24.
 */
public final class TypeFieldDeserializer<T> implements JsonDeserializer<T> {

    private final String desc;
    private final Map<String, Class<? extends T>> frameworkTypes;

    public TypeFieldDeserializer(String desc, Map<String, Class<? extends T>> frameworkTypes) {
        this.desc = desc;
        this.frameworkTypes = frameworkTypes;
    }

    @Override
    public T deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (!(json instanceof JsonObject obj)) throw new JsonParseException("Expected a JsonObject.");
        JsonElement typeElement = obj.get("type");
        if (typeElement == null) throw new JsonParseException("Expected 'type' property for " + desc);
        if (!typeElement.isJsonPrimitive()) throw new JsonParseException("Expected 'type' property to be a primitive.");

        String type = typeElement.getAsString();
        Class<? extends T> frameworkClass = frameworkTypes.get(type);
        if (frameworkClass == null) throw new JsonParseException("No " + desc + " type '" + type + "' registered.");

        return context.deserialize(json, frameworkClass);
    }
}
