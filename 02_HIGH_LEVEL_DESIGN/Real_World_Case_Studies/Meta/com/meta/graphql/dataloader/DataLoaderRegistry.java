package com.meta.graphql.dataloader;

import java.util.*;
import java.util.concurrent.*;

/**
 * DataLoader Registry - Manages all DataLoaders for efficient batching
 * 
 * This is Meta's solution to the N+1 query problem.
 * DataLoaders batch and cache requests within a single request context.
 */
public class DataLoaderRegistry {
    private final Map<String, DataLoader<?>> loaders = new ConcurrentHashMap<>();
    private final Object cacheManager;
    
    public DataLoaderRegistry(Object cacheManager) {
        this.cacheManager = cacheManager;
        initializeLoaders();
    }
    
    private void initializeLoaders() {
        // Initialize loaders for different entity types
        loaders.put("user", new DataLoader<>(this::batchLoadUsers));
        loaders.put("post", new DataLoader<>(this::batchLoadPosts));
        loaders.put("comment", new DataLoader<>(this::batchLoadComments));
    }
    
    public <T> DataLoader<T> getLoader(String name) {
        return (DataLoader<T>) loaders.get(name);
    }
    
    // Batch loading functions
    private Map<String, Object> batchLoadUsers(List<String> ids) {
        // Simulate batch database query
        Map<String, Object> users = new HashMap<>();
        for (String id : ids) {
            users.put(id, Map.of(
                "id", id,
                "name", "User " + id,
                "email", "user" + id + "@example.com"
            ));
        }
        return users;
    }
    
    private Map<String, Object> batchLoadPosts(List<String> ids) {
        Map<String, Object> posts = new HashMap<>();
        for (String id : ids) {
            posts.put(id, Map.of(
                "id", id,
                "content", "Post content " + id,
                "authorId", "author" + id
            ));
        }
        return posts;
    }
    
    private Map<String, Object> batchLoadComments(List<String> ids) {
        Map<String, Object> comments = new HashMap<>();
        for (String id : ids) {
            comments.put(id, Map.of(
                "id", id,
                "text", "Comment " + id
            ));
        }
        return comments;
    }
}

/**
 * DataLoader - Batches and caches data loading
 */
class DataLoader<T> {
    private final BatchLoadFunction<T> batchLoadFn;
    private final Map<String, CompletableFuture<T>> cache = new ConcurrentHashMap<>();
    private final List<String> queue = new ArrayList<>();
    private boolean dispatched = false;
    
    public DataLoader(BatchLoadFunction<T> batchLoadFn) {
        this.batchLoadFn = batchLoadFn;
    }
    
    public CompletableFuture<T> load(String key) {
        // Check cache first
        if (cache.containsKey(key)) {
            return cache.get(key);
        }
        
        // Add to batch queue
        CompletableFuture<T> future = new CompletableFuture<>();
        cache.put(key, future);
        queue.add(key);
        
        // Schedule batch dispatch if not already scheduled
        if (!dispatched) {
            dispatched = true;
            scheduleBatchDispatch();
        }
        
        return future;
    }
    
    private void scheduleBatchDispatch() {
        // In production, this would use the event loop
        // Here we simulate immediate batching
        CompletableFuture.runAsync(this::dispatch);
    }
    
    private void dispatch() {
        if (queue.isEmpty()) return;
        
        List<String> batchKeys = new ArrayList<>(queue);
        queue.clear();
        dispatched = false;
        
        try {
            Map<String, T> results = batchLoadFn.load(batchKeys);
            
            for (String key : batchKeys) {
                CompletableFuture<T> future = cache.get(key);
                T result = results.get(key);
                if (result != null) {
                    future.complete(result);
                } else {
                    future.completeExceptionally(new RuntimeException("Not found: " + key));
                }
            }
        } catch (Exception e) {
            for (String key : batchKeys) {
                cache.get(key).completeExceptionally(e);
            }
        }
    }
    
    public void clearCache() {
        cache.clear();
    }
}

/**
 * Batch Load Function - Interface for batch loading
 */
@FunctionalInterface
interface BatchLoadFunction<T> {
    Map<String, T> load(List<String> keys);
}
