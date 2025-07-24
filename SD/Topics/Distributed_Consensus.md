# Distributed Consensus

## Overview
Distributed Consensus is the problem of achieving agreement on data values among distributed processes or systems. It's fundamental to building reliable distributed systems where multiple nodes need to agree on shared state, even in the presence of failures and network partitions.

## Key Concepts
- **Consensus Problem**: Getting distributed nodes to agree on a single value
- **Byzantine Fault Tolerance**: Handling malicious or arbitrary failures
- **Split-Brain**: Network partition causing multiple leaders
- **Consistency Models**: Different guarantees about data consistency

## Advanced Topics

### 1. The CAP Theorem and Consensus
- **Consistency**: All nodes see the same data simultaneously
- **Availability**: System remains operational
- **Partition Tolerance**: System continues despite network failures
- **Trade-offs**: Can only guarantee two of the three

### 2. Raft Consensus Algorithm

#### Raft Leader Election
```python
import random
import time
import threading
from enum import Enum
from typing import Dict, List, Optional

class NodeState(Enum):
    FOLLOWER = "follower"
    CANDIDATE = "candidate"
    LEADER = "leader"

class RaftNode:
    def __init__(self, node_id: str, peers: List[str]):
        self.node_id = node_id
        self.peers = peers
        self.state = NodeState.FOLLOWER
        
        # Persistent state
        self.current_term = 0
        self.voted_for = None
        self.log = []  # List of log entries
        
        # Volatile state
        self.commit_index = 0
        self.last_applied = 0
        
        # Leader state
        self.next_index = {}  # Next log index for each peer
        self.match_index = {}  # Highest log index replicated on each peer
        
        # Election timing
        self.election_timeout = random.uniform(5, 10)  # 5-10 seconds
        self.last_heartbeat = time.time()
        
        self.running = True
        self.lock = threading.Lock()
    
    def start(self):
        """Start the Raft node"""
        threading.Thread(target=self._election_timer, daemon=True).start()
        threading.Thread(target=self._heartbeat_timer, daemon=True).start()
    
    def _election_timer(self):
        """Handle election timeouts"""
        while self.running:
            time.sleep(0.1)
            
            with self.lock:
                if (self.state == NodeState.FOLLOWER and 
                    time.time() - self.last_heartbeat > self.election_timeout):
                    self._start_election()
    
    def _start_election(self):
        """Start leader election"""
        print(f"Node {self.node_id} starting election for term {self.current_term + 1}")
        
        self.state = NodeState.CANDIDATE
        self.current_term += 1
        self.voted_for = self.node_id
        self.last_heartbeat = time.time()
        
        # Request votes from peers
        votes_received = 1  # Vote for self
        
        for peer in self.peers:
            if self._request_vote(peer):
                votes_received += 1
        
        # Check if won election
        if votes_received > len(self.peers) // 2:
            self._become_leader()
        else:
            self.state = NodeState.FOLLOWER
    
    def _request_vote(self, peer: str) -> bool:
        """Request vote from peer (simplified)"""
        # In real implementation, this would be an RPC call
        # For demo, simulate random responses
        return random.choice([True, False])
    
    def _become_leader(self):
        """Become the leader"""
        print(f"Node {self.node_id} became leader for term {self.current_term}")
        
        self.state = NodeState.LEADER
        
        # Initialize leader state
        for peer in self.peers:
            self.next_index[peer] = len(self.log) + 1
            self.match_index[peer] = 0
    
    def append_entry(self, data: str) -> bool:
        """Append entry to log (leader only)"""
        if self.state != NodeState.LEADER:
            return False
        
        with self.lock:
            entry = {
                'term': self.current_term,
                'index': len(self.log) + 1,
                'data': data,
                'committed': False
            }
            
            self.log.append(entry)
            
            # Replicate to followers
            return self._replicate_entry(entry)
    
    def _replicate_entry(self, entry: dict) -> bool:
        """Replicate entry to followers"""
        successful_replicas = 1  # Leader counts as one
        
        for peer in self.peers:
            if self._append_entries_rpc(peer, [entry]):
                successful_replicas += 1
        
        # Commit if majority agrees
        if successful_replicas > len(self.peers) // 2:
            entry['committed'] = True
            self.commit_index = entry['index']
            return True
        
        return False
```

### 3. Byzantine Fault Tolerance (PBFT)

#### PBFT Three-Phase Protocol
```python
class PBFTNode:
    def __init__(self, node_id: int, total_nodes: int):
        self.node_id = node_id
        self.total_nodes = total_nodes
        self.f = (total_nodes - 1) // 3  # Max Byzantine nodes
        
        self.view = 0
        self.sequence_number = 0
        self.state = {}
        
        # Message logs
        self.prepare_log = {}  # sequence -> set of node_ids
        self.commit_log = {}   # sequence -> set of node_ids
        
    def pre_prepare(self, request: str) -> dict:
        """Primary sends pre-prepare message"""
        if not self._is_primary():
            raise Exception("Only primary can send pre-prepare")
        
        self.sequence_number += 1
        message = {
            'type': 'pre-prepare',
            'view': self.view,
            'sequence': self.sequence_number,
            'request': request,
            'node_id': self.node_id
        }
        
        return message
    
    def prepare(self, pre_prepare_msg: dict) -> dict:
        """Backup nodes send prepare messages"""
        if self._is_primary():
            return None  # Primary doesn't send prepare
        
        # Validate pre-prepare message
        if not self._validate_pre_prepare(pre_prepare_msg):
            return None
        
        message = {
            'type': 'prepare',
            'view': pre_prepare_msg['view'],
            'sequence': pre_prepare_msg['sequence'],
            'digest': self._hash(pre_prepare_msg['request']),
            'node_id': self.node_id
        }
        
        return message
    
    def commit(self, sequence: int, digest: str) -> dict:
        """Send commit message after receiving 2f prepare messages"""
        message = {
            'type': 'commit',
            'view': self.view,
            'sequence': sequence,
            'digest': digest,
            'node_id': self.node_id
        }
        
        return message
    
    def handle_prepare(self, prepare_msg: dict):
        """Handle incoming prepare message"""
        seq = prepare_msg['sequence']
        
        if seq not in self.prepare_log:
            self.prepare_log[seq] = set()
        
        self.prepare_log[seq].add(prepare_msg['node_id'])
        
        # Check if we have 2f prepare messages
        if len(self.prepare_log[seq]) >= 2 * self.f:
            # Send commit message
            commit_msg = self.commit(seq, prepare_msg['digest'])
            self._broadcast(commit_msg)
    
    def handle_commit(self, commit_msg: dict):
        """Handle incoming commit message"""
        seq = commit_msg['sequence']
        
        if seq not in self.commit_log:
            self.commit_log[seq] = set()
        
        self.commit_log[seq].add(commit_msg['node_id'])
        
        # Check if we have 2f+1 commit messages
        if len(self.commit_log[seq]) >= 2 * self.f + 1:
            # Execute the request
            self._execute_request(seq)
    
    def _is_primary(self) -> bool:
        """Check if this node is the primary for current view"""
        return self.node_id == (self.view % self.total_nodes)
```

### 4. Paxos Algorithm

#### Basic Paxos Implementation
```python
class PaxosNode:
    def __init__(self, node_id: str, acceptors: List[str]):
        self.node_id = node_id
        self.acceptors = acceptors
        
        # Proposer state
        self.proposal_number = 0
        
        # Acceptor state
        self.promised_proposal = None
        self.accepted_proposal = None
        self.accepted_value = None
    
    def propose(self, value: str) -> bool:
        """Propose a value using Paxos"""
        # Phase 1: Prepare
        self.proposal_number += 1
        proposal_id = f"{self.node_id}-{self.proposal_number}"
        
        promises = self._send_prepare(proposal_id)
        
        if len(promises) <= len(self.acceptors) // 2:
            return False  # Not enough promises
        
        # Choose value based on promises
        chosen_value = self._choose_value(promises, value)
        
        # Phase 2: Accept
        accepts = self._send_accept(proposal_id, chosen_value)
        
        return len(accepts) > len(self.acceptors) // 2
    
    def _send_prepare(self, proposal_id: str) -> List[dict]:
        """Send prepare requests to acceptors"""
        promises = []
        
        for acceptor in self.acceptors:
            response = self._prepare_request(acceptor, proposal_id)
            if response and response.get('promised'):
                promises.append(response)
        
        return promises
    
    def _prepare_request(self, acceptor: str, proposal_id: str) -> dict:
        """Handle prepare request (acceptor role)"""
        # Compare with highest promised proposal
        if (self.promised_proposal is None or 
            proposal_id > self.promised_proposal):
            
            self.promised_proposal = proposal_id
            
            return {
                'promised': True,
                'accepted_proposal': self.accepted_proposal,
                'accepted_value': self.accepted_value
            }
        
        return {'promised': False}
    
    def _choose_value(self, promises: List[dict], proposed_value: str) -> str:
        """Choose value based on promises received"""
        # Find highest accepted proposal
        highest_proposal = None
        highest_value = None
        
        for promise in promises:
            if (promise.get('accepted_proposal') and
                (highest_proposal is None or 
                 promise['accepted_proposal'] > highest_proposal)):
                
                highest_proposal = promise['accepted_proposal']
                highest_value = promise['accepted_value']
        
        # Use highest accepted value or proposed value
        return highest_value if highest_value is not None else proposed_value
    
    def _send_accept(self, proposal_id: str, value: str) -> List[dict]:
        """Send accept requests to acceptors"""
        accepts = []
        
        for acceptor in self.acceptors:
            response = self._accept_request(acceptor, proposal_id, value)
            if response and response.get('accepted'):
                accepts.append(response)
        
        return accepts
    
    def _accept_request(self, acceptor: str, proposal_id: str, value: str) -> dict:
        """Handle accept request (acceptor role)"""
        # Accept if proposal >= promised proposal
        if (self.promised_proposal is None or 
            proposal_id >= self.promised_proposal):
            
            self.accepted_proposal = proposal_id
            self.accepted_value = value
            
            return {'accepted': True}
        
        return {'accepted': False}
```

### 5. Consensus in Blockchain

#### Proof of Work Consensus
```python
import hashlib

class Block:
    def __init__(self, index: int, previous_hash: str, data: str, nonce: int = 0):
        self.index = index
        self.previous_hash = previous_hash
        self.data = data
        self.nonce = nonce
        self.timestamp = time.time()
    
    def calculate_hash(self) -> str:
        """Calculate block hash"""
        content = f"{self.index}{self.previous_hash}{self.data}{self.nonce}{self.timestamp}"
        return hashlib.sha256(content.encode()).hexdigest()
    
    def mine_block(self, difficulty: int):
        """Mine block using Proof of Work"""
        target = "0" * difficulty
        
        while True:
            hash_value = self.calculate_hash()
            if hash_value.startswith(target):
                print(f"Block mined: {hash_value}")
                break
            
            self.nonce += 1

class Blockchain:
    def __init__(self, difficulty: int = 4):
        self.chain = [self._create_genesis_block()]
        self.difficulty = difficulty
        self.pending_transactions = []
        self.mining_reward = 100
    
    def _create_genesis_block(self) -> Block:
        """Create the first block in the chain"""
        return Block(0, "0", "Genesis Block")
    
    def get_latest_block(self) -> Block:
        """Get the latest block in the chain"""
        return self.chain[-1]
    
    def add_block(self, new_block: Block):
        """Add a new block to the chain"""
        new_block.previous_hash = self.get_latest_block().calculate_hash()
        new_block.mine_block(self.difficulty)
        self.chain.append(new_block)
    
    def is_chain_valid(self) -> bool:
        """Validate the entire blockchain"""
        for i in range(1, len(self.chain)):
            current_block = self.chain[i]
            previous_block = self.chain[i - 1]
            
            # Check if current block's hash is valid
            if current_block.calculate_hash() != current_block.calculate_hash():
                return False
            
            # Check if current block points to previous block
            if current_block.previous_hash != previous_block.calculate_hash():
                return False
        
        return True
```

### 6. Practical Consensus Applications

#### Database Replication with Consensus
```python
class ConsensusDatabase:
    def __init__(self, node_id: str, peers: List[str]):
        self.node_id = node_id
        self.peers = peers
        self.data = {}
        self.consensus_engine = RaftNode(node_id, peers)
    
    def write(self, key: str, value: str) -> bool:
        """Write data with consensus"""
        if not self.consensus_engine.state == NodeState.LEADER:
            return False  # Only leader can accept writes
        
        # Propose the write operation
        operation = {
            'type': 'write',
            'key': key,
            'value': value,
            'timestamp': time.time()
        }
        
        if self.consensus_engine.append_entry(operation):
            self.data[key] = value
            return True
        
        return False
    
    def read(self, key: str) -> Optional[str]:
        """Read data (can be from any node)"""
        return self.data.get(key)
    
    def apply_operation(self, operation: dict):
        """Apply committed operation to state machine"""
        if operation['type'] == 'write':
            self.data[operation['key']] = operation['value']
        elif operation['type'] == 'delete':
            self.data.pop(operation['key'], None)
```

### 7. Consensus Performance Considerations

#### Optimization Strategies
```python
class OptimizedRaftNode(RaftNode):
    def __init__(self, node_id: str, peers: List[str]):
        super().__init__(node_id, peers)
        self.batch_size = 10
        self.pending_entries = []
    
    def append_entries_batch(self, entries: List[dict]) -> bool:
        """Batch multiple entries for better throughput"""
        if self.state != NodeState.LEADER:
            return False
        
        with self.lock:
            # Add entries to log
            for entry in entries:
                entry['term'] = self.current_term
                entry['index'] = len(self.log) + 1
                self.log.append(entry)
            
            # Replicate batch
            return self._replicate_batch(entries)
    
    def _replicate_batch(self, entries: List[dict]) -> bool:
        """Replicate batch of entries"""
        successful_replicas = 1  # Leader counts as one
        
        for peer in self.peers:
            if self._append_entries_rpc(peer, entries):
                successful_replicas += 1
        
        # Commit if majority agrees
        if successful_replicas > len(self.peers) // 2:
            for entry in entries:
                entry['committed'] = True
                self.commit_index = entry['index']
            return True
        
        return False
```

### 8. Benefits of Distributed Consensus
- **Fault Tolerance**: System continues operating despite node failures
- **Consistency**: All nodes maintain consistent state
- **Reliability**: Guaranteed progress under certain conditions
- **Scalability**: Can add nodes to increase fault tolerance

### 9. Challenges
- **Performance**: Consensus protocols add latency
- **Complexity**: Difficult to implement correctly
- **Network Partitions**: Can cause availability issues
- **Byzantine Failures**: Handling malicious nodes is complex

### 10. Interview Questions
- Explain the difference between Raft and Paxos
- How does Byzantine fault tolerance differ from crash fault tolerance?
- What are the trade-offs in consensus algorithms?
- How would you design a consensus system for a payment network?

---
Continue to the next topic for deeper mastery!
