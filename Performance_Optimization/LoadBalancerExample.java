import java.util.*;
import java.util.concurrent.*;

/**
 * Load Balancer Implementation
 * 
 * Real-world Use Case: Uber Driver Assignment Load Balancer
 * Distributes ride requests across multiple driver matching services
 */

public class LoadBalancerExample {
    
    // Server representation
    static class Server {
        private final String id;
        private final String host;
        private final int port;
        private boolean isHealthy;
        private int activeConnections;
        private long lastResponseTime;
        private final Map<String, Object> metadata;
        
        public Server(String id, String host, int port) {
            this.id = id;
            this.host = host;
            this.port = port;
            this.isHealthy = true;
            this.activeConnections = 0;
            this.lastResponseTime = 0;
            this.metadata = new HashMap<>();
        }
        
        public String getId() { return id; }
        public String getHost() { return host; }
        public int getPort() { return port; }
        public boolean isHealthy() { return isHealthy; }
        public void setHealthy(boolean healthy) { this.isHealthy = healthy; }
        public int getActiveConnections() { return activeConnections; }
        public void incrementConnections() { this.activeConnections++; }
        public void decrementConnections() { this.activeConnections--; }
        public long getLastResponseTime() { return lastResponseTime; }
        public void setLastResponseTime(long responseTime) { this.lastResponseTime = responseTime; }
        public Map<String, Object> getMetadata() { return metadata; }
        
        @Override
        public String toString() {
            return String.format("Server{id='%s', %s:%d, healthy=%s, connections=%d}", 
                    id, host, port, isHealthy, activeConnections);
        }
    }
    
    // Load balancing strategy interface
    interface LoadBalancingStrategy {
        Server selectServer(List<Server> healthyServers);
        String getStrategyName();
    }
    
    // Round Robin strategy
    static class RoundRobinStrategy implements LoadBalancingStrategy {
        private int currentIndex = 0;
        
        @Override
        public Server selectServer(List<Server> healthyServers) {
            if (healthyServers.isEmpty()) {
                return null;
            }
            
            Server selected = healthyServers.get(currentIndex % healthyServers.size());
            currentIndex = (currentIndex + 1) % healthyServers.size();
            return selected;
        }
        
        @Override
        public String getStrategyName() {
            return "Round Robin";
        }
    }
    
    // Least Connections strategy
    static class LeastConnectionsStrategy implements LoadBalancingStrategy {
        @Override
        public Server selectServer(List<Server> healthyServers) {
            if (healthyServers.isEmpty()) {
                return null;
            }
            
            return healthyServers.stream()
                    .min(Comparator.comparingInt(Server::getActiveConnections))
                    .orElse(null);
        }
        
        @Override
        public String getStrategyName() {
            return "Least Connections";
        }
    }
    
    // Weighted Round Robin strategy
    static class WeightedRoundRobinStrategy implements LoadBalancingStrategy {
        private final Map<String, Integer> serverWeights;
        private final Map<String, Integer> currentWeights;
        
        public WeightedRoundRobinStrategy(Map<String, Integer> weights) {
            this.serverWeights = new HashMap<>(weights);
            this.currentWeights = new HashMap<>();
        }
        
        @Override
        public Server selectServer(List<Server> healthyServers) {
            if (healthyServers.isEmpty()) {
                return null;
            }
            
            Server selected = null;
            int totalWeight = 0;
            
            for (Server server : healthyServers) {
                int weight = serverWeights.getOrDefault(server.getId(), 1);
                int currentWeight = currentWeights.getOrDefault(server.getId(), 0) + weight;
                currentWeights.put(server.getId(), currentWeight);
                totalWeight += weight;
                
                if (selected == null || currentWeight > currentWeights.get(selected.getId())) {
                    selected = server;
                }
            }
            
            if (selected != null) {
                currentWeights.put(selected.getId(), 
                        currentWeights.get(selected.getId()) - totalWeight);
            }
            
            return selected;
        }
        
        @Override
        public String getStrategyName() {
            return "Weighted Round Robin";
        }
    }
    
    // Response Time based strategy
    static class ResponseTimeStrategy implements LoadBalancingStrategy {
        @Override
        public Server selectServer(List<Server> healthyServers) {
            if (healthyServers.isEmpty()) {
                return null;
            }
            
            return healthyServers.stream()
                    .min(Comparator.comparingLong(Server::getLastResponseTime))
                    .orElse(null);
        }
        
        @Override
        public String getStrategyName() {
            return "Best Response Time";
        }
    }
    
    // Health checker
    static class HealthChecker {
        private final ScheduledExecutorService executor;
        private final List<Server> servers;
        
        public HealthChecker(List<Server> servers) {
            this.servers = servers;
            this.executor = Executors.newScheduledThreadPool(2);
        }
        
        public void startHealthChecks() {
            executor.scheduleAtFixedRate(this::performHealthCheck, 0, 5, TimeUnit.SECONDS);
        }
        
        private void performHealthCheck() {
            for (Server server : servers) {
                // Simulate health check
                boolean isHealthy = simulateHealthCheck(server);
                if (server.isHealthy() != isHealthy) {
                    server.setHealthy(isHealthy);
                    System.out.println("[HEALTH-CHECK] Server " + server.getId() + 
                                     " is now " + (isHealthy ? "HEALTHY" : "UNHEALTHY"));
                }
            }
        }
        
        private boolean simulateHealthCheck(Server server) {
            // Simulate health check with 90% success rate
            return Math.random() > 0.1;
        }
        
        public void shutdown() {
            executor.shutdown();
        }
    }
    
    // Main Load Balancer
    static class UberLoadBalancer {
        private final List<Server> servers;
        private LoadBalancingStrategy strategy;
        private final HealthChecker healthChecker;
        
        public UberLoadBalancer() {
            this.servers = new CopyOnWriteArrayList<>();
            this.strategy = new RoundRobinStrategy();
            this.healthChecker = new HealthChecker(servers);
        }
        
        public void addServer(Server server) {
            servers.add(server);
            System.out.println("[LOAD-BALANCER] Added server: " + server);
        }
        
        public void removeServer(String serverId) {
            servers.removeIf(server -> server.getId().equals(serverId));
            System.out.println("[LOAD-BALANCER] Removed server: " + serverId);
        }
        
        public void setStrategy(LoadBalancingStrategy strategy) {
            this.strategy = strategy;
            System.out.println("[LOAD-BALANCER] Strategy changed to: " + strategy.getStrategyName());
        }
        
        public Server routeRequest(String requestId) {
            List<Server> healthyServers = servers.stream()
                    .filter(Server::isHealthy)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            
            if (healthyServers.isEmpty()) {
                System.out.println("[LOAD-BALANCER] No healthy servers available for request: " + requestId);
                return null;
            }
            
            Server selectedServer = strategy.selectServer(healthyServers);
            if (selectedServer != null) {
                selectedServer.incrementConnections();
                System.out.println("[LOAD-BALANCER] Routed request " + requestId + 
                                 " to server " + selectedServer.getId());
            }
            
            return selectedServer;
        }
        
        public void completeRequest(String serverId, long responseTime) {
            servers.stream()
                    .filter(server -> server.getId().equals(serverId))
                    .findFirst()
                    .ifPresent(server -> {
                        server.decrementConnections();
                        server.setLastResponseTime(responseTime);
                    });
        }
        
        public void startHealthChecks() {
            healthChecker.startHealthChecks();
        }
        
        public void shutdown() {
            healthChecker.shutdown();
        }
        
        public void printStatus() {
            System.out.println("\n[LOAD-BALANCER] Current Status:");
            System.out.println("Strategy: " + strategy.getStrategyName());
            System.out.println("Servers:");
            servers.forEach(server -> 
                System.out.println("  " + server + " (response: " + server.getLastResponseTime() + "ms)")
            );
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Load Balancer Demo: Uber Driver Assignment ===\n");
        
        UberLoadBalancer loadBalancer = new UberLoadBalancer();
        
        // Add servers
        loadBalancer.addServer(new Server("driver-service-1", "10.0.1.10", 8080));
        loadBalancer.addServer(new Server("driver-service-2", "10.0.1.11", 8080));
        loadBalancer.addServer(new Server("driver-service-3", "10.0.1.12", 8080));
        loadBalancer.addServer(new Server("driver-service-4", "10.0.1.13", 8080));
        
        // Start health checking
        loadBalancer.startHealthChecks();
        
        // Test Round Robin strategy
        System.out.println("\n1. Testing Round Robin Strategy:");
        for (int i = 1; i <= 6; i++) {
            Server server = loadBalancer.routeRequest("req_" + i);
            if (server != null) {
                // Simulate request completion
                long responseTime = 50 + (long)(Math.random() * 100);
                Thread.sleep(100);
                loadBalancer.completeRequest(server.getId(), responseTime);
            }
        }
        
        loadBalancer.printStatus();
        
        // Switch to Least Connections strategy
        System.out.println("\n2. Testing Least Connections Strategy:");
        loadBalancer.setStrategy(new LeastConnectionsStrategy());
        
        for (int i = 7; i <= 12; i++) {
            Server server = loadBalancer.routeRequest("req_" + i);
            if (server != null) {
                // Simulate some requests taking longer (connections stay active)
                if (i % 3 == 0) {
                    Thread.sleep(50); // Simulate slower request
                }
                long responseTime = 30 + (long)(Math.random() * 80);
                if (i % 3 != 0) { // Complete some requests immediately
                    loadBalancer.completeRequest(server.getId(), responseTime);
                }
            }
        }
        
        loadBalancer.printStatus();
        
        // Test Weighted Round Robin
        System.out.println("\n3. Testing Weighted Round Robin Strategy:");
        Map<String, Integer> weights = new HashMap<>();
        weights.put("driver-service-1", 3); // Higher capacity server
        weights.put("driver-service-2", 2);
        weights.put("driver-service-3", 2);
        weights.put("driver-service-4", 1); // Lower capacity server
        
        loadBalancer.setStrategy(new WeightedRoundRobinStrategy(weights));
        
        for (int i = 13; i <= 20; i++) {
            Server server = loadBalancer.routeRequest("req_" + i);
            if (server != null) {
                long responseTime = 40 + (long)(Math.random() * 60);
                Thread.sleep(50);
                loadBalancer.completeRequest(server.getId(), responseTime);
            }
        }
        
        loadBalancer.printStatus();
        
        // Test Response Time strategy
        System.out.println("\n4. Testing Response Time Strategy:");
        loadBalancer.setStrategy(new ResponseTimeStrategy());
        
        for (int i = 21; i <= 26; i++) {
            Server server = loadBalancer.routeRequest("req_" + i);
            if (server != null) {
                long responseTime = 20 + (long)(Math.random() * 120);
                Thread.sleep(50);
                loadBalancer.completeRequest(server.getId(), responseTime);
            }
        }
        
        loadBalancer.printStatus();
        
        // Simulate server failure and recovery
        System.out.println("\n5. Simulating server failure:");
        loadBalancer.servers.get(0).setHealthy(false);
        
        for (int i = 27; i <= 30; i++) {
            Server server = loadBalancer.routeRequest("req_" + i);
            if (server != null) {
                long responseTime = 35 + (long)(Math.random() * 70);
                Thread.sleep(50);
                loadBalancer.completeRequest(server.getId(), responseTime);
            }
        }
        
        loadBalancer.printStatus();
        
        System.out.println("\n=== Load Balancer Benefits ===");
        System.out.println("✓ Distributes load across multiple servers");
        System.out.println("✓ Provides high availability and fault tolerance");
        System.out.println("✓ Multiple balancing algorithms for different scenarios");
        System.out.println("✓ Health checking prevents routing to failed servers");
        System.out.println("✓ Improves system scalability and performance");
        
        loadBalancer.shutdown();
    }
}
