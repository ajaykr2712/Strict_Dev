package com.meta.graphql.resolver;

import com.meta.graphql.dataloader.DataLoaderRegistry;
import java.util.*;

/**
 * User Resolver - Resolves user-related GraphQL fields
 */
public class UserResolver {
    private final DataLoaderRegistry dataLoaderRegistry;
    
    public UserResolver(DataLoaderRegistry dataLoaderRegistry) {
        this.dataLoaderRegistry = dataLoaderRegistry;
    }
    
    public Object resolveUser(String id) {
        // Use DataLoader to batch and cache user fetches
        return Map.of(
            "id", id,
            "name", "User " + id,
            "email", "user" + id + "@example.com"
        );
    }
}

/**
 * Post Resolver - Resolves post-related GraphQL fields
 */
class PostResolver {
    private final DataLoaderRegistry dataLoaderRegistry;
    
    public PostResolver(DataLoaderRegistry dataLoaderRegistry) {
        this.dataLoaderRegistry = dataLoaderRegistry;
    }
    
    public Object resolvePost(String id) {
        return Map.of(
            "id", id,
            "content", "Post content " + id
        );
    }
}

/**
 * Comment Resolver - Resolves comment-related GraphQL fields
 */
class CommentResolver {
    private final DataLoaderRegistry dataLoaderRegistry;
    
    public CommentResolver(DataLoaderRegistry dataLoaderRegistry) {
        this.dataLoaderRegistry = dataLoaderRegistry;
    }
    
    public Object resolveComment(String id) {
        return Map.of(
            "id", id,
            "text", "Comment " + id
        );
    }
}
