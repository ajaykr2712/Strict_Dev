# Distributed Consensus Algorithms

## Table of Contents
1. [Raft Consensus Algorithm](#raft-consensus-algorithm)
2. [Byzantine Fault Tolerance](#byzantine-fault-tolerance)
3. [Practical Implementation](#practical-implementation)
4. [Real-World Applications](#real-world-applications)

---

## Raft Consensus Algorithm

### Overview
Raft is a consensus algorithm designed to be easy to understand. It's used to manage a replicated log across a cluster of servers, ensuring consistency even in the presence of failures.

### Key Concepts
- **Leader Election**: One server acts as leader, others as followers
- **Log Replication**: Leader replicates log entries to followers
- **Safety**: Ensures consistency across all nodes

### Implementation

```python
import random
import time
import threading
from enum import Enum
from typing import Dict, List, Optional, Tuple
from dataclasses import dataclass

class NodeState(Enum):
    FOLLOWER = "follower"
    CANDIDATE = "candidate"
    LEADER = "leader"

@dataclass
class LogEntry:
    term: int
    index: int
    command: str
    committed: bool = False

@dataclass
class VoteRequest:
    term: int
    candidate_id: str
    last_log_index: int
    last_log_term: int

@dataclass
class VoteResponse:
    term: int
    vote_granted: bool

@dataclass
class AppendEntriesRequest:
    term: int
    leader_id: str
    prev_log_index: int
    prev_log_term: int
    entries: List[LogEntry]
    leader_commit: int

@dataclass
class AppendEntriesResponse:
    term: int
    success: bool

class RaftNode:
    def __init__(self, node_id: str, cluster_nodes: List[str]):
        self.node_id = node_id
        self.cluster_nodes = cluster_nodes
        self.other_nodes = [n for n in cluster_nodes if n != node_id]
        
        # Persistent state
        self.current_term = 0
        self.voted_for: Optional[str] = None
        self.log: List[LogEntry] = []
        
        # Volatile state
        self.commit_index = 0
        self.last_applied = 0
        self.state = NodeState.FOLLOWER
        
        # Leader state
        self.next_index: Dict[str, int] = {}
        self.match_index: Dict[str, int] = {}
        
        # Election timeout (randomized)
        self.election_timeout = random.uniform(150, 300)  # milliseconds
        self.last_heartbeat = time.time()
        self.heartbeat_interval = 50  # milliseconds
        
        # Threading
        self.running = True
        self.lock = threading.Lock()
        
        print(f"🚀 Node {self.node_id} initialized as {self.state.value}")

    def start(self):
        """Start the Raft node"""
        election_thread = threading.Thread(target=self._election_timer)
        heartbeat_thread = threading.Thread(target=self._heartbeat_timer)
        
        election_thread.daemon = True
        heartbeat_thread.daemon = True
        
        election_thread.start()
        heartbeat_thread.start()

    def _election_timer(self):
        """Handle election timeout"""
        while self.running:
            time.sleep(0.01)  # 10ms check interval
            
            with self.lock:
                if self.state != NodeState.LEADER:
                    time_since_heartbeat = (time.time() - self.last_heartbeat) * 1000
                    if time_since_heartbeat > self.election_timeout:
                        self._start_election()

    def _heartbeat_timer(self):
        """Send heartbeats if leader"""
        while self.running:
            time.sleep(self.heartbeat_interval / 1000)
            
            with self.lock:
                if self.state == NodeState.LEADER:
                    self._send_heartbeats()

    def _start_election(self):
        """Start a new election"""
        print(f"🗳️ Node {self.node_id} starting election for term {self.current_term + 1}")
        
        self.state = NodeState.CANDIDATE
        self.current_term += 1
        self.voted_for = self.node_id
        self.last_heartbeat = time.time()
        self.election_timeout = random.uniform(150, 300)
        
        # Vote for self
        votes_received = 1
        
        # Request votes from other nodes (simulated)
        for node in self.other_nodes:
            if self._request_vote(node):
                votes_received += 1
        
        # Check if won election
        if votes_received > len(self.cluster_nodes) // 2:
            self._become_leader()
        else:
            self._become_follower()

    def _request_vote(self, node_id: str) -> bool:
        """Request vote from another node (simulated)"""
        # Simulate network delay
        time.sleep(random.uniform(0.01, 0.05))
        
        # Simulate vote response (simplified)
        # In real implementation, this would be an RPC call
        vote_granted = random.choice([True, False])
        
        if vote_granted:
            print(f"✅ Node {node_id} voted for {self.node_id}")
        else:
            print(f"❌ Node {node_id} denied vote for {self.node_id}")
        
        return vote_granted

    def _become_leader(self):
        """Become the leader"""
        print(f"👑 Node {self.node_id} became leader for term {self.current_term}")
        
        self.state = NodeState.LEADER
        
        # Initialize leader state
        for node in self.other_nodes:
            self.next_index[node] = len(self.log) + 1
            self.match_index[node] = 0
        
        # Send immediate heartbeat
        self._send_heartbeats()

    def _become_follower(self):
        """Become a follower"""
        print(f"👥 Node {self.node_id} became follower")
        self.state = NodeState.FOLLOWER
        self.voted_for = None

    def _send_heartbeats(self):
        """Send heartbeat to all followers"""
        for node in self.other_nodes:
            self._send_append_entries(node, [])

    def _send_append_entries(self, node_id: str, entries: List[LogEntry]):
        """Send append entries RPC (simulated)"""
        prev_log_index = self.next_index[node_id] - 1
        prev_log_term = self.log[prev_log_index].term if prev_log_index > 0 else 0
        
        request = AppendEntriesRequest(
            term=self.current_term,
            leader_id=self.node_id,
            prev_log_index=prev_log_index,
            prev_log_term=prev_log_term,
            entries=entries,
            leader_commit=self.commit_index
        )
        
        # Simulate RPC call
        response = self._simulate_append_entries_response(node_id, request)
        
        if response.success:
            self.next_index[node_id] += len(entries)
            self.match_index[node_id] = self.next_index[node_id] - 1
        else:
            # Decrement next_index and retry
            self.next_index[node_id] = max(1, self.next_index[node_id] - 1)

    def _simulate_append_entries_response(self, node_id: str, request: AppendEntriesRequest) -> AppendEntriesResponse:
        """Simulate append entries response"""
        # Simulate network delay
        time.sleep(random.uniform(0.01, 0.03))
        
        # Simplified response logic
        success = random.choice([True, False])
        
        return AppendEntriesResponse(
            term=self.current_term,
            success=success
        )

    def append_log_entry(self, command: str) -> bool:
        """Append a new log entry (only leader can do this)"""
        if self.state != NodeState.LEADER:
            print(f"❌ Node {self.node_id} is not leader, cannot append entry")
            return False
        
        with self.lock:
            entry = LogEntry(
                term=self.current_term,
                index=len(self.log) + 1,
                command=command
            )
            
            self.log.append(entry)
            print(f"📝 Leader {self.node_id} appended entry: {command}")
            
            # Replicate to followers
            for node in self.other_nodes:
                self._send_append_entries(node, [entry])
            
            return True

    def get_status(self) -> Dict:
        """Get current node status"""
        with self.lock:
            return {
                'node_id': self.node_id,
                'state': self.state.value,
                'term': self.current_term,
                'log_size': len(self.log),
                'commit_index': self.commit_index
            }

class RaftCluster:
    def __init__(self, node_count: int = 5):
        self.node_ids = [f"node-{i}" for i in range(node_count)]
        self.nodes: Dict[str, RaftNode] = {}
        
        # Create nodes
        for node_id in self.node_ids:
            self.nodes[node_id] = RaftNode(node_id, self.node_ids)

    def start_cluster(self):
        """Start all nodes in the cluster"""
        print(f"🚀 Starting Raft cluster with {len(self.nodes)} nodes")
        
        for node in self.nodes.values():
            node.start()

    def get_leader(self) -> Optional[RaftNode]:
        """Get the current leader"""
        for node in self.nodes.values():
            if node.state == NodeState.LEADER:
                return node
        return None

    def append_entry(self, command: str) -> bool:
        """Append entry through the leader"""
        leader = self.get_leader()
        if leader:
            return leader.append_log_entry(command)
        else:
            print("❌ No leader available")
            return False

    def get_cluster_status(self) -> Dict:
        """Get status of all nodes"""
        return {node_id: node.get_status() for node_id, node in self.nodes.items()}

    def stop_cluster(self):
        """Stop all nodes"""
        for node in self.nodes.values():
            node.running = False

def demonstrate_raft():
    print("🏛️ Raft Consensus Algorithm Demo")
    print("=" * 50)
    
    # Create and start cluster
    cluster = RaftCluster(5)
    cluster.start_cluster()
    
    # Wait for leader election
    print("⏳ Waiting for leader election...")
    time.sleep(2)
    
    # Show cluster status
    print("\n📊 Cluster Status:")
    status = cluster.get_cluster_status()
    for node_id, node_status in status.items():
        print(f"{node_id}: {node_status['state']} (term {node_status['term']})")
    
    # Append some entries
    print("\n📝 Appending log entries...")
    commands = ["SET x=1", "SET y=2", "SET z=3"]
    
    for command in commands:
        success = cluster.append_entry(command)
        if success:
            print(f"✅ Successfully appended: {command}")
        else:
            print(f"❌ Failed to append: {command}")
        time.sleep(0.5)
    
    # Final status
    print("\n📊 Final Cluster Status:")
    final_status = cluster.get_cluster_status()
    for node_id, node_status in final_status.items():
        print(f"{node_id}: {node_status['state']} - {node_status['log_size']} entries")
    
    # Stop cluster
    cluster.stop_cluster()
    print("\n🛑 Cluster stopped")

if __name__ == "__main__":
    demonstrate_raft()
```

---

## Byzantine Fault Tolerance

Byzantine Fault Tolerance (BFT) algorithms can handle arbitrary failures, including malicious behavior.

### PBFT (Practical Byzantine Fault Tolerance)

```python
from typing import Set, Dict
from dataclasses import dataclass
from enum import Enum

class MessageType(Enum):
    PRE_PREPARE = "pre-prepare"
    PREPARE = "prepare"
    COMMIT = "commit"

@dataclass
class Message:
    msg_type: MessageType
    view: int
    sequence: int
    digest: str
    node_id: str

class PBFTNode:
    def __init__(self, node_id: str, total_nodes: int):
        self.node_id = node_id
        self.total_nodes = total_nodes
        self.f = (total_nodes - 1) // 3  # Maximum faulty nodes
        
        self.view = 0
        self.sequence = 0
        
        # Message logs
        self.pre_prepare_log: Dict[int, Message] = {}
        self.prepare_log: Dict[int, Set[str]] = {}
        self.commit_log: Dict[int, Set[str]] = {}
        
        print(f"🛡️ PBFT Node {node_id} initialized (f={self.f})")

    def is_primary(self) -> bool:
        """Check if this node is the primary for current view"""
        return int(self.node_id.split('-')[1]) == (self.view % self.total_nodes)

    def broadcast_pre_prepare(self, request: str) -> Message:
        """Primary broadcasts pre-prepare message"""
        if not self.is_primary():
            raise ValueError("Only primary can send pre-prepare")
        
        self.sequence += 1
        digest = f"hash({request})"
        
        message = Message(
            msg_type=MessageType.PRE_PREPARE,
            view=self.view,
            sequence=self.sequence,
            digest=digest,
            node_id=self.node_id
        )
        
        self.pre_prepare_log[self.sequence] = message
        print(f"📢 Primary {self.node_id} sent pre-prepare for seq {self.sequence}")
        
        return message

    def handle_pre_prepare(self, message: Message) -> Optional[Message]:
        """Handle pre-prepare message and send prepare"""
        # Validate message
        if message.view != self.view:
            print(f"❌ Wrong view: expected {self.view}, got {message.view}")
            return None
        
        # Store pre-prepare
        self.pre_prepare_log[message.sequence] = message
        
        # Send prepare message
        prepare_msg = Message(
            msg_type=MessageType.PREPARE,
            view=self.view,
            sequence=message.sequence,
            digest=message.digest,
            node_id=self.node_id
        )
        
        print(f"✅ Node {self.node_id} sent prepare for seq {message.sequence}")
        return prepare_msg

    def handle_prepare(self, message: Message):
        """Handle prepare message"""
        seq = message.sequence
        
        if seq not in self.prepare_log:
            self.prepare_log[seq] = set()
        
        self.prepare_log[seq].add(message.node_id)
        
        # Check if we have enough prepare messages (2f)
        if len(self.prepare_log[seq]) >= 2 * self.f:
            print(f"🎯 Node {self.node_id} has enough prepares for seq {seq}")
            return self._send_commit(seq)
        
        return None

    def _send_commit(self, sequence: int) -> Message:
        """Send commit message"""
        pre_prepare = self.pre_prepare_log[sequence]
        
        commit_msg = Message(
            msg_type=MessageType.COMMIT,
            view=self.view,
            sequence=sequence,
            digest=pre_prepare.digest,
            node_id=self.node_id
        )
        
        print(f"💫 Node {self.node_id} sent commit for seq {sequence}")
        return commit_msg

    def handle_commit(self, message: Message) -> bool:
        """Handle commit message"""
        seq = message.sequence
        
        if seq not in self.commit_log:
            self.commit_log[seq] = set()
        
        self.commit_log[seq].add(message.node_id)
        
        # Check if we have enough commit messages (2f+1)
        if len(self.commit_log[seq]) >= 2 * self.f + 1:
            print(f"🏆 Node {self.node_id} committed sequence {seq}")
            return True
        
        return False

def demonstrate_pbft():
    print("🛡️ PBFT Consensus Demo")
    print("=" * 30)
    
    # Create 4 nodes (can tolerate 1 Byzantine failure)
    nodes = [PBFTNode(f"node-{i}", 4) for i in range(4)]
    primary = nodes[0]  # node-0 is primary for view 0
    
    # Simulate client request
    request = "TRANSFER 100 FROM A TO B"
    print(f"📝 Client request: {request}")
    
    # Phase 1: Pre-prepare
    pre_prepare_msg = primary.broadcast_pre_prepare(request)
    
    # Phase 2: Prepare
    prepare_messages = []
    for node in nodes[1:]:  # All except primary
        prepare_msg = node.handle_pre_prepare(pre_prepare_msg)
        if prepare_msg:
            prepare_messages.append(prepare_msg)
    
    # Distribute prepare messages
    for node in nodes:
        for prepare_msg in prepare_messages:
            if prepare_msg.node_id != node.node_id:
                commit_msg = node.handle_prepare(prepare_msg)
    
    # Phase 3: Commit (simplified - in reality, commit messages would be distributed)
    print(f"\n✅ PBFT consensus reached for: {request}")

if __name__ == "__main__":
    demonstrate_pbft()
```

---

## Real-World Applications

### Blockchain Networks
- **Bitcoin**: Proof of Work consensus
- **Ethereum 2.0**: Proof of Stake with finality
- **Hyperledger Fabric**: PBFT-based consensus

### Distributed Databases
- **Apache Cassandra**: Eventual consistency with tunable consistency
- **CockroachDB**: Raft-based consensus for strong consistency
- **TiDB**: Raft for metadata management

### Service Coordination
- **etcd**: Raft-based key-value store for Kubernetes
- **Consul**: Raft for service discovery and configuration
- **Apache Zookeeper**: ZAB (Zookeeper Atomic Broadcast) protocol

### Trade-offs

| Algorithm | Fault Tolerance | Performance | Complexity |
|-----------|----------------|-------------|------------|
| Raft | Crash failures only | High | Low |
| PBFT | Byzantine failures | Medium | High |
| PoW | Byzantine + Sybil | Low | Medium |
| PoS | Byzantine + Economic | Medium | Medium |

---

## Key Considerations

### Network Partitions
- **Split-brain**: Ensure only one partition can make progress
- **CAP Theorem**: Choose between consistency and availability
- **Quorum**: Require majority for decisions

### Performance Optimization
- **Batching**: Process multiple requests together
- **Pipelining**: Overlap request processing
- **Local Reads**: Read from local replica when possible

### Security
- **Message Authentication**: Prevent message tampering
- **Identity Verification**: Ensure message sender authenticity
- **Replay Protection**: Prevent old message replay attacks

---

*Consensus algorithms are the foundation of distributed systems reliability. Choose the right algorithm based on your failure model and performance requirements.*
