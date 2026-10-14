# ⚡ ZeroMesh-UPI: Day 4 Interview Master Study Notes
> **Focus Area:** Chaos Engineering, Security Attacks, Threat Modeling & Mesh Propagation Dynamics  
> **Candidate:** Sundram Singhal | Role: Java Backend Developer / SDE-1  

---

## 📌 INDEX OF DAY 4 TOPICS
1. **Threat Modeling & Chaos Engine Endpoints**
   - Zero-Trust Bluetooth Environment overview
   - Live Chaos testing endpoints: `/attack/tamper`, `/attack/replay`, `/attack/expired`
2. **The 5 Major Attack Vectors & Production Defenses**
   - 2.1. Attack #1: Data Tamper Attack (Ciphertext alteration, Stage-2 SHA-256 vs Settlement AES-GCM GMAC tag)
   - 2.2. Attack #2: Replay Attack (Duplicate packet spamming, Stage-3 Redis SETNX 25h lock vs PostgreSQL UNIQUE constraint)
   - 2.3. Attack #3: Zombie / Delay Attack (Stale packets > 24h, Stage-1 Temporal Expiry Check)
   - 2.4. Attack #4: Sybil & Broadcast Storm Denial-of-Service (DoS) on Mesh (`TTL=5` hop decay + 2ms gateway fast-fail)
   - 2.5. Attack #5: Multi-Bridge Concurrent Flush Race Condition (Dave vs Bob, Redis atomic distributed mutex)
3. **Deep Architectural Clarifications (Sundram's Killer Doubts)**
   - 3.1. Two-Tier Deduplication Truth: Ingestion uses Redis SETNX, Settlement uses PostgreSQL `packet_id UNIQUE` (Settlement does NOT check Redis, only calls `markSettled` / `release`)
   - 3.2. Redis Lock Key format: `zeromesh:packet:<packetId>`
   - 3.3. 1 Million Fake Packets Attack Trace: 4-Layer Defense-in-Depth model
   - 3.4. `TTL = 5` Hop Concept Deep-Dive: Hop countdown counter, infinite loop prevention, 50m subway physics
   - 3.5. Single Hop Multi-Device Broadcast: Omnidirectional radio waves, 50 devices in 10m range, Jitter delay, Duplicate relay suppression, Low battery awareness
4. **Day 4 Mock Interview Verbal Repertoire (Top 4 Ready-to-Speak English Scripts)**
5. **Day 4 Quick Revision & Threat-Matrix (Last Page Cheat Sheet)**

---

*(All detailed narratives, code blocks, diagrams, and verbatim English interview scripts are fully compiled inside the companion PDF).*
