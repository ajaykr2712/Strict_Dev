public class ServiceLocatorExample {
    interface Service { String getName(); void execute(); }
    static class ServiceA implements Service {
        public String getName(){ return "SERVICE_A"; }
        public void execute(){ System.out.println("Executing Service A"); }
    }
    static class ServiceB implements Service {
        public String getName(){ return "SERVICE_B"; }
        public void execute(){ System.out.println("Executing Service B"); }
    }

    static class Cache {
        private final java.util.Map<String, Service> services = new java.util.HashMap<>();
        Service get(String name){ return services.get(name); }
        void put(Service svc){ services.put(svc.getName(), svc); }
    }

    static class InitialContext {
        Service lookup(String name){
            System.out.println("Looking up " + name);
            if ("SERVICE_A".equalsIgnoreCase(name)) return new ServiceA();
            if ("SERVICE_B".equalsIgnoreCase(name)) return new ServiceB();
            throw new IllegalArgumentException("Unknown service: " + name);
        }
    }

    static class ServiceLocator {
        private static final Cache cache = new Cache();
        private static final InitialContext context = new InitialContext();
        static Service getService(String name){
            String key = name.toUpperCase();
            Service svc = cache.get(key);
            if (svc == null){
                svc = context.lookup(key);
                cache.put(svc);
            }
            return svc;
        }
    }

    public static void main(String[] args){
        Service a1 = ServiceLocator.getService("SERVICE_A"); a1.execute();
        Service a2 = ServiceLocator.getService("SERVICE_A"); a2.execute();
        Service b = ServiceLocator.getService("SERVICE_B"); b.execute();
    }
}
