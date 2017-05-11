package org.ovirt.engine.core.utils.serialization.json;

import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.jsontype.impl.ClassNameIdResolver;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

public class JsonCollectionIdResolver extends ClassNameIdResolver {

    private static Map<String, String> relacementIds = new HashMap<>();

    static {
        relacementIds.put("java.util.Collections$SingletonSet", "java.util.HashSet");
        relacementIds.put("java.util.Collections$SingletonList", "java.util.ArrayList");
        relacementIds.put("java.util.Collections$UnmodifiableSet", "java.util.HashSet");
        relacementIds.put("java.util.Collections$UnmodifiableList", "java.util.ArrayList");
    }

    public JsonCollectionIdResolver() {
        this(null, null);
    }

    protected JsonCollectionIdResolver(JavaType baseType, TypeFactory typeFactory) {
        super(baseType, typeFactory);
    }

    @Override
    public String idFromValue(Object o) {
        String id = super.idFromValue(o);
        // return a replacement id if it exists
        return relacementIds.getOrDefault(id, id);
    }

    @Override
    public String idFromValueAndType(Object o, Class<?> aClass) {
        String id = super.idFromValueAndType(o, aClass);
        // return a replacement id if it exists
        return relacementIds.getOrDefault(id, id);
    }

    @Override
    public JsonTypeInfo.Id getMechanism() {
        return JsonTypeInfo.Id.CLASS;
    }
}
