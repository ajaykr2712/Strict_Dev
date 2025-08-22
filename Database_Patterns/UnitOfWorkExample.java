import java.util.*;

public class UnitOfWorkExample {
    static class Entity { String id; String name; Entity(String id, String name){ this.id=id; this.name=name; } public String toString(){ return id+":"+name; } }
    interface Repository { void insert(Entity e); void update(Entity e); void delete(String id); Entity find(String id); Collection<Entity> all(); }
    static class InMemoryRepository implements Repository {
        private final Map<String, Entity> db = new HashMap<>();
        public void insert(Entity e){ db.put(e.id, e); }
        public void update(Entity e){ db.put(e.id, e); }
        public void delete(String id){ db.remove(id); }
        public Entity find(String id){ return db.get(id); }
        public Collection<Entity> all(){ return db.values(); }
    }
    static class UnitOfWork {
        private final List<Entity> newEntities = new ArrayList<>();
        private final List<Entity> dirtyEntities = new ArrayList<>();
        private final List<String> removedIds = new ArrayList<>();
        private final Repository repo;
        UnitOfWork(Repository repo){ this.repo = repo; }
        void registerNew(Entity e){ newEntities.add(e); }
        void registerDirty(Entity e){ dirtyEntities.add(e); }
        void registerRemoved(String id){ removedIds.add(id); }
        void commit(){
            newEntities.forEach(repo::insert);
            dirtyEntities.forEach(repo::update);
            removedIds.forEach(repo::delete);
            newEntities.clear(); dirtyEntities.clear(); removedIds.clear();
        }
    }
    public static void main(String[] args){
        Repository repo = new InMemoryRepository();
        UnitOfWork uow = new UnitOfWork(repo);
        uow.registerNew(new Entity("1", "Alice"));
        uow.registerNew(new Entity("2", "Bob"));
        uow.commit();
        Entity e = repo.find("1"); e.name = "Alicia"; uow.registerDirty(e); uow.commit();
        uow.registerRemoved("2"); uow.commit();
        System.out.println(repo.all());
    }
}
