public class HealthCheckPatternExample {
    interface HealthIndicator { String name(); boolean healthy(); String details(); }
    static class DatabaseHealth implements HealthIndicator { public String name(){ return "database"; } public boolean healthy(){ return true; } public String details(){ return "connection ok"; } }
    static class CacheHealth implements HealthIndicator { public String name(){ return "cache"; } public boolean healthy(){ return false; } public String details(){ return "timeout"; } }

    static class HealthAggregator {
        java.util.List<HealthIndicator> indicators = java.util.List.of(new DatabaseHealth(), new CacheHealth());
        void report(){
            boolean all = true;
            for (var i : indicators){
                boolean ok = i.healthy();
                all &= ok;
                System.out.println(i.name() + ": " + (ok ? "UP" : "DOWN") + " (" + i.details() + ")");
            }
            System.out.println("Overall: " + (all ? "UP" : "DEGRADED"));
        }
    }

    public static void main(String[] args){
        new HealthAggregator().report();
    }
}
