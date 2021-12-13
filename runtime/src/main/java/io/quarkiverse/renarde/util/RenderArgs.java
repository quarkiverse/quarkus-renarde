package io.quarkiverse.renarde.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class RenderArgs {

    Map<String, Object> args = new HashMap<>();

    public void put(String key, Object value) {
        args.put(key, value);
    }

    public Set<Entry<String, Object>> entrySet() {
        return args.entrySet();
    }

    public <T> T get(String key) {
        return (T) args.get(key);
    }
}
