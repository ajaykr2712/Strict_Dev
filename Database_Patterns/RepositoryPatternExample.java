public class RepositoryPatternExample {
    static class User {
        final String id; String name;
        User(String id, String name){ this.id = id; this.name = name; }
        public String toString(){ return "User{id='" + id + "', name='" + name + "'}"; }
    }

    interface UserRepository {
        void save(User u);
        User findById(String id);
        java.util.List<User> findAll();
        void delete(String id);
    }

    static class InMemoryUserRepository implements UserRepository {
        private final java.util.Map<String, User> store = new java.util.concurrent.ConcurrentHashMap<>();
        public void save(User u){ store.put(u.id, u); }
        public User findById(String id){ return store.get(id); }
        public java.util.List<User> findAll(){ return new java.util.ArrayList<>(store.values()); }
        public void delete(String id){ store.remove(id); }
    }

    public static void main(String[] args){
        UserRepository repo = new InMemoryUserRepository();
        repo.save(new User("1", "Alice"));
        repo.save(new User("2", "Bob"));
        System.out.println(repo.findAll());
        User u = repo.findById("1"); u.name = "Alicia"; repo.save(u);
        System.out.println(repo.findById("1"));
        repo.delete("2");
        System.out.println(repo.findAll());
    }
}
