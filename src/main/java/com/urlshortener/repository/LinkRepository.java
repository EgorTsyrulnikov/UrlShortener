package com.urlshortener.repository;

import com.urlshortener.model.ShortLink;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class LinkRepository {
    // Thread-safe map for concurrent access
    private final Map<String, ShortLink> storage = new ConcurrentHashMap<>();

    public void save(ShortLink link) {
        storage.put(link.getShortCode(), link);
    }

    public ShortLink findByCode(String code) {
        return storage.get(code);
    }

    public void delete(String code) {
        storage.remove(code);
    }

    public List<ShortLink> findAll() {
        return new ArrayList<>(storage.values());
    }

    public boolean exists(String code) {
        return storage.containsKey(code);
    }
}
