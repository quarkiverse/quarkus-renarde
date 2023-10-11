package io.quarkiverse.renarde.transporter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import io.quarkus.arc.Arc;

public class InstanceResolver {

    Map<String, Map<Object, Object>> instances = new HashMap<>();

    public <T> T resolve(String type, Object id) {
        //    	Log.infof("resolve type: %s, id: %s", type, id);
        Map<Object, Object> map = instances.get(type);
        if (map == null) {
            map = new HashMap<>();
            instances.put(type, map);
        }
        Object instance = map.get(id);
        if (instance == null) {
            EntityTransporter entityTransporter = Arc.container().instance(EntityTransporter.class).get();
            instance = entityTransporter.instantiate(type);
            map.put(id, instance);
        }
        return (T) instance;
    }

    public <T> T resolveReference(JsonParser p) throws IOException {
        return resolveReference(p, false);
    }

    public <T> T resolveReference(JsonParser p, boolean alreadyReadStartToken) throws IOException {
        if (!alreadyReadStartToken) {
            p.nextToken();
        }
        if (p.currentToken() == JsonToken.VALUE_NULL)
            return null;
        DatabaseTransporter.assertt(p.currentToken() == JsonToken.START_OBJECT);
        Long id = null;
        String type = null;

        String fieldName;
        while ((fieldName = p.nextFieldName()) != null) {
            switch (fieldName) {
                case "id":
                    id = p.nextLongValue(0);
                    break;
                case "_type":
                    type = p.nextTextValue();
                    break;
            }
        }
        T ret = resolve(type, id);
        DatabaseTransporter.assertt(p.currentToken() == JsonToken.END_OBJECT);
        return ret;
    }
}
