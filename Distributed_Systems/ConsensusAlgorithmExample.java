package com.systemdesign.distributed;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Consensus Algorithm Implementation - Simplified Raft
 * 
 * Demonstrates distributed consensus for maintaining consistency
 * across WhatsApp's message servers and ensuring message ordering.
 * 
 * Real-world scenario: Ensuring all WhatsApp servers agree on
 * message sequence numbers to maintain global message ordering.
 */

// Node states in Raft consensus
enum NodeState {
    FOLLOWER, CANDIDATE, LEADER
}

// Log entry structure
class LogEntry {
    private final int term;
    private final String command;
    private final long timestamp;
    
    public LogEntry(int term, String command) {
        this.term = term;
        this.command = command;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters
    public int getTerm() { return term; }
    public String getCommand() { return command; }
    public long getTimestamp() { return timestamp; }
    
    @Override
    public String toString() {
        return String.format("LogEntry{term=%d, command='%s', time=%d}", 
                           term, command, timestamp);
    }
}

// Vote request for leader election
class VoteRequest {
    private final int term;
    private final String candidateId;
    private final int lastLogIndex;
    private final int lastLogTerm;
    
    public VoteRequest(int term, String candidateId, int lastLogIndex, int lastLogTerm) {
        this.term = term;
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }
    
    // Getters
    public int getTerm() { return term; }
    public String getCandidateId() { return candidateId; }
    public int getLastLogIndex() { return lastLogIndex; }
    public int getLastLogTerm() { return lastLogTerm; }
}

// Append entries request for log replication
class AppendEntriesRequest {
    private final int term;
    private final String leaderId;
    private final int prevLogIndex;
    private final int prevLogTerm;
    private final List<LogEntry> entries;
    private final int leaderCommit;
    
    public AppendEntriesRequest(int term, String leaderId, int prevLogIndex, 
                               int prevLogTerm, List<LogEntry> entries, int leaderCommit) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }
    
    // Getters
    public int getTerm() { return term; }
    public String getLeaderId() { return leaderId; }
    public int getPrevLogIndex() { return prevLogIndex; }
    public int getPrevLogTerm() { return prevLogTerm; }
    public List<LogEntry> getEntries() { return entries; }
    public int getLeaderCommit() { return leaderCommit; }
}

// Raft node implementation
class RaftNode {
    private final String nodeId;
    private volatile NodeState state;
    private volatile int currentTerm;
    private volatile String votedFor;
    private final List<LogEntry> log;
    private volatile int commitIndex;
    private volatile int lastApplied;
    
    // Leader state
    private final Map<String, Integer> nextIndex;
    private final Map<String, Integer> matchIndex;
    
    // Cluster information
    private final Set<String> clusterNodes;
    private final Map<String, RaftNode> nodeReferences;
    
    // Timing
    private final Random random;
    private volatile long lastHeartbeat;
    private final AtomicLong messagesSent;
    private final AtomicInteger electionsWon;
    
    // Election timeout (150-300ms in real Raft)
    private static final int ELECTION_TIMEOUT_MIN = 1000;
    private static final int ELECTION_TIMEOUT_MAX = 2000;
    private static final int HEARTBEAT_INTERVAL = 500;
    
    public RaftNode(String nodeId, Set<String> clusterNodes) {
        this.nodeId = nodeId;
        this.state = NodeState.FOLLOWER;
        this.currentTerm = 0;
        this.votedFor = null;
        this.log = new ArrayList<>();
        this.commitIndex = -1;
        this.lastApplied = -1;
        
        this.nextIndex = new ConcurrentHashMap<>();
        this.matchIndex = new ConcurrentHashMap<>();
        this.clusterNodes = new HashSet<>(clusterNodes);
        this.nodeReferences = new ConcurrentHashMap<>();
        
        this.random = new Random();
        this.lastHeartbeat = System.currentTimeMillis();
        this.messagesSent = new AtomicLong(0);
        this.electionsWon = new AtomicInteger(0);
        
        // Initialize leader state for all nodes
        for (String node : clusterNodes) {
            if (!node.equals(nodeId)) {
                nextIndex.put(node, 0);
                matchIndex.put(node, -1);
            }
        }
    }
    
    // Set node references for communication
    public void setNodeReferences(Map<String, RaftNode> nodeReferences) {
        this.nodeReferences.putAll(nodeReferences);
    }
    
    // Check if election timeout occurred
    public boolean isElectionTimeout() {
        long timeout = ELECTION_TIMEOUT_MIN + 
                      random.nextInt(ELECTION_TIMEOUT_MAX - ELECTION_TIMEOUT_MIN);
        return (System.currentTimeMillis() - lastHeartbeat) > timeout;
    }
    
    // Start leader election
    public void startElection() {
        state = NodeState.CANDIDATE;
        currentTerm++;
        votedFor = nodeId;
        lastHeartbeat = System.currentTimeMillis();
        
        System.out.println(nodeId + " starting election for term " + currentTerm);
        
        // Vote for self
        int votes = 1;
        
        // Request votes from other nodes
        int lastLogIndex = log.size() - 1;
        int lastLogTerm = lastLogIndex >= 0 ? log.get(lastLogIndex).getTerm() : 0;
        
        VoteRequest request = new VoteRequest(currentTerm, nodeId, lastLogIndex, lastLogTerm);
        
        for (String nodeId : clusterNodes) {
            if (!nodeId.equals(this.nodeId)) {
                RaftNode node = nodeReferences.get(nodeId);
                if (node != null && node.handleVoteRequest(request)) {
                    votes++;
                }
            }
        }
        
        // Check if won election (majority)
        if (votes > clusterNodes.size() / 2) {
            becomeLeader();
        } else {
            state = NodeState.FOLLOWER;
            votedFor = null;
        }
    }
    
    // Handle vote request from candidate
    public synchronized boolean handleVoteRequest(VoteRequest request) {
        messagesSent.incrementAndGet();
        
        // Reply false if term < currentTerm
        if (request.getTerm() < currentTerm) {
            return false;
        }
        
        // If term > currentTerm, become follower
        if (request.getTerm() > currentTerm) {
            currentTerm = request.getTerm();
            votedFor = null;
            state = NodeState.FOLLOWER;
        }
        
        // Vote for candidate if haven't voted or voted for same candidate
        if (votedFor == null || votedFor.equals(request.getCandidateId())) {
            // Check if candidate's log is at least as up-to-date
            int lastLogIndex = log.size() - 1;
            int lastLogTerm = lastLogIndex >= 0 ? log.get(lastLogIndex).getTerm() : 0;
            
            boolean logUpToDate = request.getLastLogTerm() > lastLogTerm ||
                                (request.getLastLogTerm() == lastLogTerm && 
                                 request.getLastLogIndex() >= lastLogIndex);
            
            if (logUpToDate) {
                votedFor = request.getCandidateId();
                lastHeartbeat = System.currentTimeMillis();
                return true;
            }
        }
        
        return false;
    }
    
    // Become leader after winning election
    private void becomeLeader() {
        state = NodeState.LEADER;
        electionsWon.incrementAndGet();
        System.out.println(nodeId + " became leader for term " + currentTerm);
        
        // Initialize leader state
        for (String node : clusterNodes) {
            if (!node.equals(nodeId)) {
                nextIndex.put(node, log.size());
                matchIndex.put(node, -1);
            }
        }
        
        // Send initial heartbeat
        sendHeartbeat();
    }
    
    // Send heartbeat to maintain leadership
    public void sendHeartbeat() {
        if (state != NodeState.LEADER) return;
        
        AppendEntriesRequest request = new AppendEntriesRequest(
            currentTerm, nodeId, -1, -1, new ArrayList<>(), commitIndex
        );
        
        for (String nodeId : clusterNodes) {
            if (!nodeId.equals(this.nodeId)) {
                RaftNode node = nodeReferences.get(nodeId);
                if (node != null) {
                    node.handleAppendEntries(request);
                }
            }
        }
    }
    
    // Handle append entries (heartbeat or log replication)
    public synchronized boolean handleAppendEntries(AppendEntriesRequest request) {
        messagesSent.incrementAndGet();
        lastHeartbeat = System.currentTimeMillis();
        
        // Reply false if term < currentTerm
        if (request.getTerm() < currentTerm) {
            return false;
        }
        
        // If term >= currentTerm, become follower
        if (request.getTerm() >= currentTerm) {
            currentTerm = request.getTerm();
            state = NodeState.FOLLOWER;
            votedFor = null;
        }
        
        // For heartbeat, just acknowledge
        if (request.getEntries().isEmpty()) {
            return true;
        }
        
        // Handle log replication (simplified)
        log.addAll(request.getEntries());
        
        // Update commit index
        if (request.getLeaderCommit() > commitIndex) {
            commitIndex = Math.min(request.getLeaderCommit(), log.size() - 1);
        }
        
        return true;
    }
    
    // Client request to add log entry (only leader accepts)
    public boolean addLogEntry(String command) {
        if (state != NodeState.LEADER) {
            return false;
        }
        
        LogEntry entry = new LogEntry(currentTerm, command);
        log.add(entry);
        
        System.out.println(nodeId + " added log entry: " + command);
        
        // Replicate to followers (simplified)
        replicateToFollowers(entry);
        
        return true;
    }
    
    // Replicate log entry to followers
    private void replicateToFollowers(LogEntry entry) {
        List<LogEntry> entries = Arrays.asList(entry);
        AppendEntriesRequest request = new AppendEntriesRequest(
            currentTerm, nodeId, log.size() - 2, 
            log.size() > 1 ? log.get(log.size() - 2).getTerm() : 0,
            entries, commitIndex
        );
        
        int replicationCount = 1; // Leader counts as one
        
        for (String nodeId : clusterNodes) {
            if (!nodeId.equals(this.nodeId)) {
                RaftNode node = nodeReferences.get(nodeId);
                if (node != null && node.handleAppendEntries(request)) {
                    replicationCount++;
                }
            }
        }
        
        // Commit if majority replicated
        if (replicationCount > clusterNodes.size() / 2) {
            commitIndex = log.size() - 1;
            System.out.println(nodeId + " committed entry at index " + commitIndex);
        }
    }
    
    // Getters and status methods
    public String getNodeId() { return nodeId; }
    public NodeState getState() { return state; }
    public int getCurrentTerm() { return currentTerm; }
    public int getLogSize() { return log.size(); }
    public int getCommitIndex() { return commitIndex; }
    public long getMessagesSent() { return messagesSent.get(); }
    public int getElectionsWon() { return electionsWon.get(); }
    
    public void printStatus() {
        System.out.println(String.format(
            "%s: %s (term=%d, log=%d, commit=%d, msgs=%d, elections=%d)",
            nodeId, state, currentTerm, log.size(), commitIndex, 
            messagesSent.get(), electionsWon.get()
        ));
    }
    
    public List<LogEntry> getCommittedEntries() {
        return commitIndex >= 0 ? 
            log.subList(0, Math.min(commitIndex + 1, log.size())) : 
            new ArrayList<>();
    }
}

// WhatsApp-style distributed consensus simulator
class WhatsAppConsensusSimulator {
    private final Map<String, RaftNode> nodes;
    private final ScheduledExecutorService scheduler;
    private final Random random;
    
    public WhatsAppConsensusSimulator() {
        this.nodes = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(10);
        this.random = new Random();
    }
    
    public void initializeCluster(String... nodeIds) {
        Set<String> clusterNodes = new HashSet<>(Arrays.asList(nodeIds));
        
        // Create nodes
        for (String nodeId : nodeIds) {
            RaftNode node = new RaftNode(nodeId, clusterNodes);
            nodes.put(nodeId, node);
        }
        
        // Set node references for communication
        for (RaftNode node : nodes.values()) {
            node.setNodeReferences(nodes);
        }
        
        System.out.println("Initialized cluster with nodes: " + 
                         String.join(", ", nodeIds));
    }
    
    public void startElectionProcesses() {
        for (RaftNode node : nodes.values()) {
            scheduler.scheduleAtFixedRate(() -> {
                if (node.getState() == NodeState.FOLLOWER && node.isElectionTimeout()) {
                    node.startElection();
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
        }
    }
    
    public void startHeartbeatProcesses() {
        scheduler.scheduleAtFixedRate(() -> {
            for (RaftNode node : nodes.values()) {
                if (node.getState() == NodeState.LEADER) {
                    node.sendHeartbeat();
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }
    
    public void simulateMessageOperations() {
        scheduler.scheduleAtFixedRate(() -> {
            // Find current leader
            RaftNode leader = nodes.values().stream()
                .filter(node -> node.getState() == NodeState.LEADER)
                .findFirst()
                .orElse(null);
            
            if (leader != null && random.nextDouble() < 0.3) {
                String messageId = "MSG_" + System.currentTimeMillis();
                String command = "STORE_MESSAGE:" + messageId;
                leader.addLogEntry(command);
            }
        }, 2000, 1000, TimeUnit.MILLISECONDS);
    }
    
    public void printClusterStatus() {
        System.out.println("\n=== Cluster Status ===");
        for (RaftNode node : nodes.values()) {
            node.printStatus();
        }
        
        // Find leader
        RaftNode leader = nodes.values().stream()
            .filter(node -> node.getState() == NodeState.LEADER)
            .findFirst()
            .orElse(null);
        
        if (leader != null) {
            System.out.println("\nLeader: " + leader.getNodeId());
            System.out.println("Committed entries: " + leader.getCommittedEntries().size());
        } else {
            System.out.println("\nNo leader elected");
        }
    }
    
    public void shutdown() {
        scheduler.shutdown();
    }
}

public class ConsensusAlgorithmExample {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== WhatsApp Distributed Consensus Demo ===\n");
        
        WhatsAppConsensusSimulator simulator = new WhatsAppConsensusSimulator();
        
        // Initialize 5-node cluster (WhatsApp message servers)
        simulator.initializeCluster("SERVER_1", "SERVER_2", "SERVER_3", 
                                   "SERVER_4", "SERVER_5");
        
        // Start consensus processes
        simulator.startElectionProcesses();
        simulator.startHeartbeatProcesses();
        
        // Wait for leader election
        Thread.sleep(2000);
        System.out.println("=== After Initial Leader Election ===");
        simulator.printClusterStatus();
        
        // Start message operations
        simulator.simulateMessageOperations();
        
        // Monitor for 10 seconds
        for (int i = 0; i < 5; i++) {
            Thread.sleep(2000);
            System.out.println("\n=== Status Update " + (i + 1) + " ===");
            simulator.printClusterStatus();
        }
        
        simulator.shutdown();
        
        demonstrateConsensusProperties();
    }
    
    private static void demonstrateConsensusProperties() {
        System.out.println("\n=== Consensus Algorithm Properties ===");
        System.out.println("1. Safety: Only one leader per term");
        System.out.println("2. Liveness: Eventually a leader is elected");
        System.out.println("3. Consistency: All nodes agree on committed entries");
        System.out.println("4. Fault Tolerance: Tolerates minority node failures");
        System.out.println("\n=== WhatsApp Use Cases ===");
        System.out.println("- Message sequence number assignment");
        System.out.println("- Global chat room state synchronization");
        System.out.println("- Distributed configuration management");
        System.out.println("- Leader election for processing coordination");
    }
}
