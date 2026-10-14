# ⚡ ZeroMesh-UPI: Day 2 Interview Master Study Notes
> **Focus Area:** Project Architecture, Auto Demo Lifecycle & Apache Kafka KRaft Deep-Dive  
> **Candidate:** Sundram Singhal | Role: Java Backend Developer / SDE-1  

---

## 📌 INDEX OF DAY 2 TOPICS
1. **Microservices Architecture & Complete Folder Structure**
   - 1.1. Why Maven Multi-Module? (`common-mesh-dto`, `ingestion-service`, `settlement-service`)
   - 1.2. Architectural Proofs of Microservices (Independent Deployability, Async Decoupling, Fault Isolation)
   - 1.3. File-by-File Post-Mortem Table (Every critical file's exact responsibility)
2. **"Auto Demo" End-to-End Execution Trace**
   - Step 1: `/api/v1/mesh/inject` (Sealing in Alice's phone)
   - Step 2: `/api/v1/mesh/gossip` (Bluetooth hop relay & TTL decay)
   - Step 3: `/api/v1/mesh/flush` (Dave Bridge upload & 4-Stage Gateway checks)
   - Step 4: Core Settlement (Consumer, RSA decryption, Postgres ACID debit/credit, @Version check)
   - Step 5: Screen Polling & Terminal Logs
3. **Apache Kafka KRaft Deep-Dive**
   - 3.1. The 6:00 PM Subway Rush Problem (HikariCP exhaustion & Tomcat thread starvation)
   - 3.2. KRaft Mode (No ZooKeeper — Raft consensus in modern Kafka 3.x+)
   - 3.3. What is a Kafka Broker? (Port 9092, disk folders, multi-broker cluster)
   - 3.4. Partitions, Hash Key, and `%` Modulo Math (`Math.abs(MurmurHash2(key)) % 3`)
   - 3.5. Out-of-Order Execution Bug (Salary +₹5,000 vs Purchase -₹4,000)
   - 3.6. Consumer Groups & Partition Assignment Rules (3 Partitions = Max 3 parallel consumers)
4. **Offsets, Delivery Semantics & Two-Tier Idempotency**
   - 4.1. Kafka Offsets (The bookmark in `__consumer_offsets`)
   - 4.2. Exactly-Once Processing Formula (`At-Least-Once Delivery + Two-Tier Idempotency`)
   - 4.3. The Two-Tier Deduplication Truth (Tier 1: Redis SETNX at Ingestion, Tier 2: DB Unique Constraint at Consumer)
   - 4.4. Producer ACKs (`acks = all`) & Idempotent Producer (Sequence Numbers & PID)
   - 4.5. Spring Boot Profile Inheritance (`application.yml` parent vs `application-docker.yml` child)
   - 4.6. Consumer Lag & Dynamic Rebalancing
5. **Dead-Letter Topic (DLT) & Poison Pill Mitigation**
   - Head-of-Line Blocking prevention
   - Spring Kafka `@RetryableTopic` (3 attempts, exponential backoff: 1s, 2s, 4s)
   - Routing to `mesh-payment-settlements-dlt`
6. **Day 2 Mock Interview Verbal Repertoire (Top 4 Ready-to-Speak English Scripts)**
7. **Day 2 Quick Revision & Architecture Matrix (Last Page Cheat Sheet)**

---

*(All detailed narratives, code blocks, diagrams, and verbatim English interview scripts are fully compiled inside the companion PDF).*
