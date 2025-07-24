#!/usr/bin/env python3
"""
Distributed Systems Simulation
Simulates various distributed system concepts and consensus algorithms.
"""

import time
import threading
import random
from typing import Dict, List, Set, Optional
from dataclasses import dataclass
from enum import Enum


class NodeState(Enum):
    FOLLOWER = "follower"
    CANDIDATE = "candidate"
    LEADER = "leader"
    FAILED = "failed"


@dataclass
class LogEntry:
    term: int
    index: int
    command: str
    timestamp: float = None
    
    def __post_init__(self):
        if self.timestamp is None:
            self.timestamp = time.time()


@dataclass
class Message:
    type: str
    from_node: str
    to_node: str
    term: int
    data: Dict = None
    timestamp: float = None
    
    def __post_init__(self):
        if self.timestamp is None:
            self.timestamp = time.time()


class RaftNode:
    """Raft Consensus Algorithm Implementation"""
    
    def __init__(self, node_id: str, cluster_nodes: List[str]):
        self.node_id = node_id
        self.cluster_nodes = cluster_nodes
        self.state = NodeState.FOLLOWER
        self.current_term = 0
        self.voted_for = None
        self.log: List[LogEntry] = []
        self.commit_index = 0
        self.last_applied = 0
        
        # Leader state
        self.next_index: Dict[str, int] = {}
        self.match_index: Dict[str, int] = {}
        
        # Election state
        self.election_timeout = random.uniform(1.5, 3.0)
        self.last_heartbeat = time.time()
        self.votes_received: Set[str] = set()
        
        # Network simulation
        self.message_queue: List[Message] = []
        self.is_failed = False
        self.network_partition = False
        
        self.lock = threading.Lock()
    
    def start_election(self):
        """Start leader election"""
        with self.lock:
            if self.is_failed:
                return
            
            self.state = NodeState.CANDIDATE
            self.current_term += 1
            self.voted_for = self.node_id
            self.votes_received = {self.node_id}
            
            # Send vote requests to all other nodes
            for node in self.cluster_nodes:
                if node != self.node_id:
                    vote_request = Message(
                        type="vote_request",
                        from_node=self.node_id,
                        to_node=node,
                        term=self.current_term,
                        data={
                            "last_log_index": len(self.log) - 1 if self.log else -1,
                            "last_log_term": self.log[-1].term if self.log else 0
                        }
                    )
                    self.send_message(vote_request)
    
    def handle_vote_request(self, message: Message) -> bool:
        """Handle vote request from candidate"""
        with self.lock:
            if self.is_failed:
                return False
            
            # Update term if necessary
            if message.term > self.current_term:
                self.current_term = message.term
                self.voted_for = None
                self.state = NodeState.FOLLOWER
            
            # Vote if haven't voted and candidate's log is up-to-date
            can_vote = (
                message.term >= self.current_term and
                (self.voted_for is None or self.voted_for == message.from_node)
            )
            
            if can_vote:
                self.voted_for = message.from_node
                response = Message(
                    type="vote_response",
                    from_node=self.node_id,
                    to_node=message.from_node,
                    term=self.current_term,
                    data={"vote_granted": True}
                )
                self.send_message(response)
                return True
            
            return False
    
    def handle_vote_response(self, message: Message):
        """Handle vote response"""
        with self.lock:
            if (self.state != NodeState.CANDIDATE or 
                message.term != self.current_term or
                self.is_failed):
                return
            
            if message.data.get("vote_granted"):
                self.votes_received.add(message.from_node)
                
                # Check if won election
                if len(self.votes_received) > len(self.cluster_nodes) // 2:
                    self.become_leader()
    
    def become_leader(self):
        """Become leader and initialize leader state"""
        self.state = NodeState.LEADER
        
        # Initialize leader state
        for node in self.cluster_nodes:
            if node != self.node_id:
                self.next_index[node] = len(self.log)
                self.match_index[node] = -1
        
        # Send initial heartbeat
        self.send_heartbeat()
    
    def send_heartbeat(self):
        """Send heartbeat to all followers"""
        if self.state != NodeState.LEADER or self.is_failed:
            return
        
        for node in self.cluster_nodes:
            if node != self.node_id:
                heartbeat = Message(
                    type="append_entries",
                    from_node=self.node_id,
                    to_node=node,
                    term=self.current_term,
                    data={
                        "prev_log_index": len(self.log) - 1 if self.log else -1,
                        "prev_log_term": self.log[-1].term if self.log else 0,
                        "entries": [],
                        "leader_commit": self.commit_index
                    }
                )
                self.send_message(heartbeat)
    
    def append_entry(self, command: str) -> bool:
        """Append new entry to log (only for leaders)"""
        if self.state != NodeState.LEADER or self.is_failed:
            return False
        
        entry = LogEntry(
            term=self.current_term,
            index=len(self.log),
            command=command
        )
        self.log.append(entry)
        
        # Replicate to followers
        self.replicate_log()
        return True
    
    def replicate_log(self):
        """Replicate log entries to followers"""
        if self.state != NodeState.LEADER:
            return
        
        for node in self.cluster_nodes:
            if node != self.node_id:
                next_idx = self.next_index.get(node, 0)
                entries = self.log[next_idx:] if next_idx < len(self.log) else []
                
                append_msg = Message(
                    type="append_entries",
                    from_node=self.node_id,
                    to_node=node,
                    term=self.current_term,
                    data={
                        "prev_log_index": next_idx - 1,
                        "prev_log_term": self.log[next_idx - 1].term if next_idx > 0 else 0,
                        "entries": entries,
                        "leader_commit": self.commit_index
                    }
                )
                self.send_message(append_msg)
    
    def handle_append_entries(self, message: Message) -> bool:
        """Handle append entries (heartbeat or log replication)"""
        with self.lock:
            if self.is_failed:
                return False
            
            # Update term and become follower if necessary
            if message.term >= self.current_term:
                self.current_term = message.term
                self.state = NodeState.FOLLOWER
                self.voted_for = None
                self.last_heartbeat = time.time()
            
            # Reply false if term is outdated
            if message.term < self.current_term:
                return False
            
            # Log consistency check
            prev_log_index = message.data["prev_log_index"]
            prev_log_term = message.data["prev_log_term"]
            
            if (prev_log_index >= 0 and
                (len(self.log) <= prev_log_index or
                 self.log[prev_log_index].term != prev_log_term)):
                return False
            
            # Append new entries
            entries = message.data["entries"]
            if entries:
                start_index = prev_log_index + 1
                self.log = self.log[:start_index] + entries
            
            # Update commit index
            leader_commit = message.data["leader_commit"]
            if leader_commit > self.commit_index:
                self.commit_index = min(leader_commit, len(self.log) - 1)
            
            return True
    
    def send_message(self, message: Message):
        """Simulate sending message over network"""
        if not self.network_partition:
            # Simulate network delay
            delay = random.uniform(0.01, 0.1)
            threading.Timer(delay, lambda: self._deliver_message(message)).start()
    
    def _deliver_message(self, message: Message):
        """Deliver message to target node"""
        # This would be implemented by the cluster coordinator
        pass
    
    def fail_node(self):
        """Simulate node failure"""
        self.is_failed = True
        self.state = NodeState.FAILED
    
    def recover_node(self):
        """Simulate node recovery"""
        self.is_failed = False
        self.state = NodeState.FOLLOWER
        self.last_heartbeat = time.time()
    
    def partition_node(self, partitioned: bool):
        """Simulate network partition"""
        self.network_partition = partitioned
    
    def get_status(self) -> Dict:
        """Get node status"""
        return {
            "node_id": self.node_id,
            "state": self.state.value,
            "term": self.current_term,
            "log_length": len(self.log),
            "commit_index": self.commit_index,
            "is_failed": self.is_failed,
            "network_partition": self.network_partition
        }


class RaftCluster:
    """Raft Cluster Coordinator"""
    
    def __init__(self, node_ids: List[str]):
        self.nodes = {
            node_id: RaftNode(node_id, node_ids)
            for node_id in node_ids
        }
        self.running = False
        self.message_queue: List[Message] = []
        self.lock = threading.Lock()
    
    def start(self):
        """Start the cluster"""
        self.running = True
        
        # Start election timer for all nodes
        for node in self.nodes.values():
            threading.Thread(target=self._run_node, args=(node,), daemon=True).start()
        
        # Start message processor
        threading.Thread(target=self._process_messages, daemon=True).start()
    
    def _run_node(self, node: RaftNode):
        """Run node event loop"""
        while self.running:
            if node.is_failed:
                time.sleep(0.1)
                continue
            
            # Check for election timeout
            if (node.state in [NodeState.FOLLOWER, NodeState.CANDIDATE] and
                time.time() - node.last_heartbeat > node.election_timeout):
                node.start_election()
            
            # Send periodic heartbeats if leader
            if node.state == NodeState.LEADER:
                node.send_heartbeat()
                time.sleep(0.5)
            else:
                time.sleep(0.1)
    
    def _process_messages(self):
        """Process messages between nodes"""
        while self.running:
            with self.lock:
                messages_to_process = self.message_queue.copy()
                self.message_queue.clear()
            
            for message in messages_to_process:
                target_node = self.nodes.get(message.to_node)
                if target_node and not target_node.is_failed:
                    if message.type == "vote_request":
                        target_node.handle_vote_request(message)
                    elif message.type == "vote_response":
                        target_node.handle_vote_response(message)
                    elif message.type == "append_entries":
                        target_node.handle_append_entries(message)
            
            time.sleep(0.01)
    
    def send_message(self, message: Message):
        """Add message to processing queue"""
        with self.lock:
            self.message_queue.append(message)
    
    def get_leader(self) -> Optional[str]:
        """Get current leader node ID"""
        for node in self.nodes.values():
            if node.state == NodeState.LEADER and not node.is_failed:
                return node.node_id
        return None
    
    def append_command(self, command: str) -> bool:
        """Append command to cluster"""
        leader_id = self.get_leader()
        if leader_id:
            return self.nodes[leader_id].append_entry(command)
        return False
    
    def get_cluster_status(self) -> Dict:
        """Get status of all nodes"""
        return {
            node_id: node.get_status()
            for node_id, node in self.nodes.items()
        }
    
    def fail_node(self, node_id: str):
        """Simulate node failure"""
        if node_id in self.nodes:
            self.nodes[node_id].fail_node()
    
    def recover_node(self, node_id: str):
        """Simulate node recovery"""
        if node_id in self.nodes:
            self.nodes[node_id].recover_node()
    
    def partition_nodes(self, node_ids: List[str], partitioned: bool):
        """Simulate network partition"""
        for node_id in node_ids:
            if node_id in self.nodes:
                self.nodes[node_id].partition_node(partitioned)
    
    def stop(self):
        """Stop the cluster"""
        self.running = False


def demo_raft_consensus():
    """Demonstrate Raft consensus algorithm"""
    print("Raft Consensus Algorithm Demo")
    print("=" * 40)
    
    # Create cluster with 5 nodes
    node_ids = ["node1", "node2", "node3", "node4", "node5"]
    cluster = RaftCluster(node_ids)
    
    # Override message sending to use cluster coordinator
    for node in cluster.nodes.values():
        node.send_message = cluster.send_message
    
    cluster.start()
    
    print("Starting cluster with 5 nodes...")
    time.sleep(2)
    
    # Check initial state
    status = cluster.get_cluster_status()
    leader = cluster.get_leader()
    print(f"Initial leader: {leader}")
    print(f"Node states: {[(node, info['state']) for node, info in status.items()]}")
    
    # Append some commands
    print("\nAppending commands...")
    commands = ["CREATE user1", "UPDATE user1 name=John", "DELETE temp_data"]
    for cmd in commands:
        success = cluster.append_command(cmd)
        print(f"Command '{cmd}': {'✓' if success else '✗'}")
    
    time.sleep(1)
    
    # Simulate leader failure
    print(f"\nSimulating leader failure: {leader}")
    if leader:
        cluster.fail_node(leader)
    
    time.sleep(3)  # Wait for new election
    
    new_leader = cluster.get_leader()
    print(f"New leader after failure: {new_leader}")
    
    # Test command after leader change
    success = cluster.append_command("RECOVERY command")
    print(f"Command after leader change: {'✓' if success else '✗'}")
    
    # Simulate network partition
    print("\nSimulating network partition...")
    cluster.partition_nodes(["node4", "node5"], True)
    
    time.sleep(2)
    
    status = cluster.get_cluster_status()
    print("Status after partition:")
    for node_id, info in status.items():
        partition_status = "partitioned" if info["network_partition"] else "connected"
        print(f"  {node_id}: {info['state']} ({partition_status})")
    
    # Heal partition
    print("\nHealing network partition...")
    cluster.partition_nodes(["node4", "node5"], False)
    
    time.sleep(2)
    
    final_status = cluster.get_cluster_status()
    final_leader = cluster.get_leader()
    print(f"Final leader: {final_leader}")
    print("Final cluster state:")
    for node_id, info in final_status.items():
        print(f"  {node_id}: {info['state']} (term: {info['term']}, "
              f"log: {info['log_length']}, commit: {info['commit_index']})")
    
    cluster.stop()


if __name__ == "__main__":
    demo_raft_consensus()
