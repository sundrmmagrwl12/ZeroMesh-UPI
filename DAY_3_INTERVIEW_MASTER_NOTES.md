# ⚡ ZeroMesh-UPI: Day 3 Interview Master Study Notes
> **Focus Area:** Core Settlement Engine, Dual Tamper Detection, PostgreSQL ACID, Immutable Ledger & HikariCP Tuning  
> **Candidate:** Sundram Singhal | Role: Java Backend Developer / SDE-1  

---

## 📌 INDEX OF DAY 3 TOPICS
1. **The Core Settlement Engine Lifecycle (Port 8081)**
   - Step 1: JSON Deserialization (`MeshPacket`)
   - Step 2: Dual Cryptographic Decryption (RSA-2048 OAEP unwrapping + AES-256-GCM GMAC tag validation)
   - Step 3: Domain Validations (Amount > 0, Account Existence, Sufficient Balance)
   - Step 4: Atomic Settlement (`@Transactional`, JPA `@Version`, Ledger entry, Redis status update)
2. **Dual-Layer Tamper Detection (The Key Difference)**
   - Layer 1 (Ingestion Gateway): Outside comparison check `SHA-256(ciphertext) == packetId` without opening the vault.
   - Layer 2 (Settlement Engine): Direct mathematical GMAC tag verification (`AEADBadTagException`) upon opening the vault.
3. **PostgreSQL ACID Properties in ZeroMesh-UPI**
   - 3.1. Atomicity: Mid-flight crash scenario, `@Transactional` boundary, automatic rollback.
   - 3.2. Consistency: Double-entry invariant ($\Delta \text{Sender} + \Delta \text{Receiver} = 0$), non-negative balance checks.
   - 3.3. Isolation: PostgreSQL default `READ COMMITTED` (Zero Dirty Reads) + JPA `@Version` write collision handling.
   - 3.4. Durability: PostgreSQL Write-Ahead Logging (WAL) + synchronous disk `fsync`, crash recovery replay.
4. **The Immutable Double-Entry Ledger (Passbook Audit)**
   - 4.1. The Banking Rule: "No UPDATE, No DELETE!" (Append-Only Ledger for RBI/ISO 20022 compliance).
   - 4.2. The 6 Statuses Taxonomy: `SETTLED`, `DUPLICATE`, `EXPIRED`, `TAMPERED`, `INSUFFICIENT_FUNDS`, `INVALID_ACCOUNT`.
   - 4.3. `INVALID_ACCOUNT` vs `TAMPERED` (Human error vs cyber crime).
   - 4.4. `packet_id UNIQUE` constraint on Ledger as Tier-2 Idempotency defense.
5. **HikariCP Connection Pool Deep-Dive**
   - 5.1. TCP connection handshake overhead (50-100ms) vs Pool pre-warmed connections (0.1ms).
   - 5.2. The 500 Connections Myth vs Brett Wooldridge Formula ($2 \times \text{Cores} + \text{Disk}$).
   - 5.3. Spring Boot HikariCP configurations (`maximum-pool-size: 10`, `connection-timeout: 3000ms`).
   - 5.4. Zero Starvation guarantee via Kafka decoupling.
6. **Day 3 Mock Interview Verbal Repertoire (Top 4 Ready-to-Speak English Scripts)**
7. **Day 3 Quick Revision & Architecture Matrix (Last Page Cheat Sheet)**

---

*(All detailed narratives, code blocks, diagrams, and verbatim English interview scripts are fully compiled inside the companion PDF).*
