import java.util.*;
import java.util.concurrent.*;

public class CacheAsidePatternExample {
    static class Database { private final Map<String, String> store = new ConcurrentHashMap<>(); String get(String k){ return store.get(k); } void put(String k, String v){ store.put(k,v); } }
    static class Cache { private final Map<String, String> store = new ConcurrentHashMap<>(); String get(String k){ return store.get(k); } void put(String k, String v){ store.put(k,v); } void evict(String k){ store.remove(k); } }
    static class Service {
        private final Database db = new Database();
        private final Cache cache = new Cache();
        String get(String key){
            String v = cache.get(key);
            if (v != null) return v; // cache hit
            v = db.get(key); // cache miss
            if (v != null) cache.put(key, v);
            return v;
        }
        void update(String key, String value){ db.put(key, value); cache.evict(key); }
    }
    public static void main(String[] args){
        Service svc = new Service();
        svc.update("k1", "v1");
        System.out.println("get1: " + svc.get("k1")); // miss then hit
        System.out.println("get2: " + svc.get("k1")); // hit
        svc.update("k1", "v2");
        System.out.println("get3: " + svc.get("k1")); // miss then hit v2
    }
}
