# ⚡ ZeroMesh-UPI

<div align="center">

**Offline Bluetooth Mesh Payment System — No Internet. No Problem.**

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)

</div>

---

## 🎯 The Story Behind This Project

> Picture this: You're at a music festival. 50,000 people. The cellular towers are completely jammed — not a single bar of signal. You want to buy food from a stall. The vendor uses UPI. **You both have zero internet.** Transaction fails. You go hungry.
>
> Or: You're in a basement office in Mumbai. Thick concrete walls. No signal. Your client is sitting across the table. You finish the project. He wants to pay you right now. UPI says "Network Error."
>
> **ZeroMesh-UPI solves this.** Your payment travels through other peoples' phones — like a message passed hand-to-hand across a crowd — until it finds internet and settles. You never need internet. Your payment data is never exposed to the strangers relaying it. It just works.

---

## 📋 Table of Contents

1. [The Big Idea](#-the-big-idea)
2. [But Wait — Why Not Just Use Hotspot?](#-but-wait--why-not-just-use-hotspot)
3. [How It Actually Works](#-how-it-actually-works)
4. [The Security Pipeline — 4 Layers of Protection](#-the-security-pipeline--4-layers-of-protection)
5. [Live Attack Simulations — Chaos Engine](#-live-attack-simulations--chaos-engine)
6. [The Cryptography](#-the-cryptography-rsa-2048--aes-256-gcm)
7. [System Architecture](#-system-architecture)
8. [Tech Stack & Why Each Choice](#-tech-stack--why-each-choice)
9. [Project Structure](#-project-structure)
10. [Getting Started](#-getting-started-5-minutes)
11. [API Reference](#-api-reference)
12. [Full Demo Walkthrough](#-full-demo-walkthrough)
13. [Key Design Decisions](#-key-design-decisions-the-why-behind-everything)
14. [Demo Accounts](#-demo-accounts)

---

## 💡 The Big Idea

Traditional UPI works like a phone call — **both sides need a live connection at the same time.**

ZeroMesh-UPI works like a **postal system with encryption** — your payment is sealed in a cryptographic envelope, handed off to whoever is nearby, and they pass it along until it reaches the server. Nobody in the middle can open the envelope. Nobody knows what's inside. But the payment always arrives.

```
Traditional UPI:
  Sundram ──[needs internet]──► UPI Server ──[needs internet]──► Rahul
  
  No internet at Sundram's end? ❌ FAILED.

ZeroMesh-UPI:
  Sundram ──[Bluetooth]──► Stranger's Phone ──[Bluetooth]──► Another Stranger
           (no internet needed)                                       │
                                                               [Bluetooth]
                                                                      │
                                                               Bridge Phone
                                                               (has internet)
                                                                      │
                                                               ZeroMesh Server
                                                                      │
                                                               ✅ SETTLED
```

The payment travels like **gossip through a crowd** — hop by hop, phone to phone — using Bluetooth. The moment ANY phone in the chain gets internet, it uploads the packet and the money moves. Sundram doesn't need to be online. Rahul doesn't need to be online. They just need to be near other humans.

---

## 🤔 But Wait — Why Not Just Use Hotspot?

This is the most important question. Everyone asks it. Let's kill the confusion once and for all.

### What a Hotspot Does

A hotspot turns one phone's cellular data into a WiFi network that other phones can connect to. It's **internet sharing.**

```
[Phone A has 4G] ──creates──► [WiFi Hotspot] ──connects──► [Phone B]
                                                                 │
                                                       Phone B now has internet
                                                       via Phone A's cellular data
```

### Why Hotspot Doesn't Solve This Problem

| Scenario | Hotspot | ZeroMesh-UPI |
|----------|---------|--------------|
| Sundram has no internet | ❌ Can't create hotspot | ✅ Creates encrypted packet offline |
| Rahul has no internet | ❌ Hotspot won't help him receive payment | ✅ Doesn't need internet to receive |
| Nobody nearby has internet | ❌ Completely broken | ✅ Packet waits in mesh, settles later |
| Festival with jammed towers | ❌ All hotspots fail (no cellular) | ✅ Bluetooth still works |
| Basement with zero signal | ❌ No cellular = no hotspot | ✅ Bluetooth passes through walls |
| 10,000 people, 3 have internet | ❌ Only those 3 can help, and only directly | ✅ Packet hops through all 10,000 to reach those 3 |

### The Fundamental Difference

**Hotspot = Internet sharing.** You need at least one person with internet to *directly* connect to and use their connection.

**ZeroMesh-UPI = Payment routing.** Your payment is an **encrypted self-contained packet** that can travel through people who have zero internet, across any number of hops, and settle when it eventually reaches someone who does.

It's the difference between:
- 📡 Hotspot: *"Can I borrow your internet?"*
- 📦 ZeroMesh: *"Can you hold this sealed envelope and pass it along?"*

The stranger passing the envelope doesn't need internet. They don't need to know what's inside. They just need Bluetooth. The envelope finds its own way to the server.

### Security Difference: Hotspot vs ZeroMesh

There's another critical difference: **privacy and security.**

When you use someone's hotspot to make a UPI payment:
- All your UPI traffic passes through their phone
- A malicious hotspot owner can run a man-in-the-middle attack
- Your payment metadata (amounts, UPI IDs) can be logged
- You're trusting a stranger with your entire network connection

When your payment hops through ZeroMesh:
- The packet is encrypted with **RSA-2048 + AES-256-GCM** before it leaves your phone
- Intermediate phones only see an opaque encrypted blob
- They cannot read it, modify it without detection, or replay it
- Even if they try — the 4-stage security pipeline at the server catches every attack

> **In short: Hotspot needs internet and requires trust. ZeroMesh needs neither.**

---

## 🔭 How It Actually Works

### The Journey of a Single Payment

Let's trace ₹500 from Sundram's phone to the server, step by step.

#### Phase 1: Offline Encryption (Sundram's Phone)

```
Sundram wants to pay Rahul ₹500

1. PaymentInstruction created:
   {
     senderId:   "sundram@upi"
     receiverId: "rahul@upi"
     amount:     500.00
     nonce:      "nonce-1728123456789"   ← random, prevents identical packets
     timestamp:  1728123456789           ← epoch ms, used for 24h expiry check
     pinHash:    "hashed-pin-demo"
   }

2. Hybrid Encryption:
   ┌─────────────────────────────────────────────────────┐
   │  Random AES-256 key generated (throwaway, per-payment)
   │  Random 12-byte IV generated                        │
   │  AES-256-GCM encrypts PaymentInstruction JSON       │
   │  RSA-2048 encrypts the AES key (server's public key)│
   └─────────────────────────────────────────────────────┘

3. EncryptedPayloadDto created:
   {
     encryptedAesKey: "BASE64_OF_RSA_ENCRYPTED_AES_KEY..."
     iv:              "BASE64_OF_12_BYTE_IV..."
     ciphertext:      "BASE64_OF_AES_GCM_CIPHERTEXT..."
   }

4. packetId = SHA-256(ciphertext) → Base64
   This fingerprint uniquely identifies this payment.
   If ciphertext is tampered later → fingerprint won't match → rejected.

5. MeshPacket created:
   {
     packetId:  "abc123xyz..."  ← SHA-256 fingerprint
     ttl:       5               ← max 5 Bluetooth hops
     createdAt: 1728123456789   ← server rejects if > 24h old
     payload:   { encryptedAesKey, iv, ciphertext }
   }

6. Packet injected into phone-alice (Sundram's device)
```

#### Phase 2: Bluetooth Gossip (Through Strangers)

```
Each "Gossip Round" = one hop through the mesh

phone-alice (TTL=5)
    │ Bluetooth
    ├──► phone-bob     (receives packet, TTL now 4)
    ├──► phone-charlie (receives packet, TTL now 4)
    ├──► phone-dave    (receives packet, TTL now 4)
    └──► phone-bridge  (receives packet, TTL now 4) ← has WiFi

phone-bob (TTL=4)
    │ Bluetooth
    ├──► phone-alice   (already has it, ignores duplicate)
    ├──► phone-charlie (already has it, ignores duplicate)
    ├──► phone-dave    (already has it, ignores duplicate)
    └──► phone-bridge  (already has it, ignores duplicate)

TTL rules:
  - TTL > 0: phone forwards the packet to all Bluetooth neighbors
  - TTL = 0: phone holds the packet (doesn't forward, but doesn't delete)
  - Packet is NEVER deleted from device memory until bridge flushes it

Each device deduplicates by packetId before storing.
The same packet is never stored twice on the same device.
```

#### Phase 3: Bridge Upload (Internet Reconnect)

```
phone-bridge walks outside, gets WiFi signal

flushBridges() called:
  ├── phone-bridge has internet → YES
  ├── phone-bridge has packets  → YES (count: 1)
  └── Upload all packets to: POST /api/v1/bridge/ingest

packet arrives at Ingestion Service...
```

#### Phase 4: The 4-Stage Security Pipeline

```
MeshPacket received at server
          │
          ▼
   ┌─────────────────────────────────────┐
   │  STAGE 1: Expiry Check              │
   │  now - packet.createdAt > 24h?      │
   │  YES → save EXPIRED to DB → return  │
   │  NO  → continue ↓                   │
   └─────────────────────────────────────┘
          │
          ▼
   ┌─────────────────────────────────────┐
   │  STAGE 2: Integrity Check           │
   │  SHA-256(ciphertext) == packetId?   │
   │  NO  → save TAMPERED to DB → return │
   │  YES → continue ↓                   │
   └─────────────────────────────────────┘
          │
          ▼
   ┌─────────────────────────────────────┐
   │  STAGE 3: Redis SETNX               │
   │  zeromesh:packet:<id> exists?       │
   │  YES → save DUPLICATE to DB → return│
   │  NO  → claim key (25h TTL) → cont ↓ │
   └─────────────────────────────────────┘
          │
          ▼
   ┌─────────────────────────────────────┐
   │  STAGE 4: Kafka Publish             │
   │  Serialize packet → Kafka topic     │
   │  Return HTTP 200 immediately (~3ms) │
   └─────────────────────────────────────┘
          │
          ▼ (async, Kafka consumer)
   ┌─────────────────────────────────────┐
   │  SETTLEMENT SERVICE                 │
   │  1. RSA decrypt → AES key           │
   │  2. AES-GCM decrypt → JSON          │
   │  3. Validate amount > 0             │
   │  4. Load sender + receiver accounts │
   │  5. Check balance (optimistic lock) │
   │  6. Debit sender, Credit receiver   │
   │  7. Write SETTLED to ledger         │
   │  8. Mark Redis key as SETTLED       │
   └─────────────────────────────────────┘
```

---

## 🔒 The Security Pipeline — 4 Layers of Protection

### Stage 1 — Expiry Check

**Threat it stops:** Stale packet replay

If someone captures a payment packet today and tries to submit it 2 days later, Stage 1 catches it immediately. No DB write, no Kafka, no wasted resources — just a fast timestamp comparison.

```java
// Reject if packet is older than 24 hours
if ((now - packet.getCreatedAt()) > 24 * 60 * 60 * 1000L) → EXPIRED
```

**Why 24 hours?**
- Long enough: A packet could realistically spend hours hopping through a festival crowd before finding internet. 24h gives it enough time to reach the server in almost any real-world scenario.
- Short enough: An attacker can't store packets for delayed replay. After 24h, the window permanently closes.

---

### Stage 2 — SHA-256 Integrity Check

**Threat it stops:** Man-in-the-Middle tampering

The `packetId` is computed as `SHA-256(ciphertext)` on the sender's phone. At the server, we recompute `SHA-256(ciphertext)` from the received packet and compare. If ANY bit of the encrypted payload changed in transit — even one byte — SHA-256 produces a completely different hash, and the mismatch is detected.

```
Sender computes:   packetId = Base64(SHA-256("V2FzZWVtamFhZ..."))
                              = "abc123xyz..."

Attacker changes:  ciphertext = "MODIFIED_V2FzZWVtamFhZ..."

Server computes:   SHA-256("MODIFIED_V2FzZWVtamFhZ...") = "totally_different_hash"
                   "totally_different_hash" ≠ "abc123xyz..." → TAMPERED ✅
```

**Important:** This check runs on the *encrypted* ciphertext — before any decryption attempt. The server doesn't need to decrypt anything to verify integrity. This is computationally cheap (~0.1ms) and provides a strong guarantee.

---

### Stage 3 — Redis SETNX Idempotency Lock

**Threat it stops:** Duplicate payment / Replay attack

`SETNX` = Set if Not Exists. It's an atomic Redis operation — if the key already exists, the operation fails. This guarantees that even if 10 bridge phones simultaneously upload the same packet (all got WiFi at the same moment), only one will succeed in claiming the Redis lock. All others are dropped as DUPLICATE.

```
Redis state before: empty

Bridge 1 submits packetId "abc123" → SETNX "zeromesh:packet:abc123" → SUCCESS → claims
Bridge 2 submits packetId "abc123" → SETNX "zeromesh:packet:abc123" → FAIL → DUPLICATE
Bridge 3 submits packetId "abc123" → SETNX "zeromesh:packet:abc123" → FAIL → DUPLICATE

Redis state after: { "zeromesh:packet:abc123": "PROCESSING", TTL: 25h }
```

**The 25-hour TTL rationale:**
- Packet validity window: 24 hours
- Redis TTL: 25 hours (24h + 1h buffer)
- Why the buffer? A packet created at 23:59:59 on Day 1 is still valid until 23:59:59 on Day 2. Without the buffer, the Redis key could expire at exactly 24h, leaving a 1-second window where the same packet could slip through again. The 1h buffer closes this gap with margin to spare.

---

### Stage 4 — Kafka Async Settlement

**What it enables:** Non-blocking, fault-tolerant processing

Once a packet passes all 3 security checks, it's published to Kafka and the HTTP response is returned immediately (~2-3ms total for all 4 stages). The heavy work — RSA decryption, database writes, balance updates — happens asynchronously in the Settlement Service.

**Fault tolerance chain:**
```
Kafka publish fails?
  └── RuntimeException thrown → HTTP 500 → bridge retries upload

Settlement Service crashes after receiving from Kafka?
  └── Kafka message not ACK'd → redelivered on restart

Settlement processing fails 3 times?
  └── @RetryableTopic: 3 attempts, exponential backoff (1s → 2s → 4s)
  └── After 3 failures: routed to Dead Letter Topic
  └── Ops team alerted → manual investigation
  └── Payment NEVER silently lost
```

---

## 💥 Live Attack Simulations — Chaos Engine

The dashboard has a built-in **Security Chaos Engine** — three buttons that trigger real attack scenarios so you can watch the security pipeline catch them in real time.

### 🔴 Tamper Attack — Man-in-the-Middle

**Real-world scenario:** An attacker intercepts the packet as it hops through phone-bob. They try to modify the ciphertext — maybe hoping to corrupt the payment or redirect it.

**What the Chaos Engine does:**
```
Sends a MeshPacket where:
  payload.ciphertext      = "altered-tampered-content"
  payload.encryptedAesKey = "attacker-key"
  packetId                = "TAMPER-ATTACK-{timestamp}"

Note: packetId was computed from the ORIGINAL ciphertext,
      but the ciphertext has been replaced with tampered data.
```

**What happens:**
```
Stage 2: SHA-256("altered-tampered-content")
       = "some_hash_X"
       
       ≠ "TAMPER-ATTACK-{timestamp}"
       
       → TAMPERED. Saved to DB. Visible in ledger immediately.
```

---

### ⚫ Replay Attack — Double-Spend

**Real-world scenario:** A criminal captures a valid ₹10,000 payment packet (maybe intercepting Bluetooth traffic). They resubmit it to the server hoping the payment settles twice.

**What the Chaos Engine does:**
```
1. Computes a valid-looking packetId from sample ciphertext
2. Pre-claims that packetId in Redis using idempotencyService.claim()
   (simulating a packet that already settled once)
3. Sends the same packet again to the ingestion pipeline
```

**What happens:**
```
Stage 3: Redis SETNX "zeromesh:packet:{id}"
       → Key already exists (pre-claimed in step 2)
       → FAIL
       → DUPLICATE. Saved to DB. Double-spend blocked.
```

---

### 🟠 Expired Packet — Stale Replay

**Real-world scenario:** An attacker stores a valid payment packet from 2001 (or any old packet) and tries submitting it years later — hoping the server's memory is short.

**What the Chaos Engine does:**
```
Sends a MeshPacket where:
  createdAt = 1000000000000L  ← Unix epoch ms for ~September 9, 2001
  (This packet is ~23 years old)
```

**What happens:**
```
Stage 1: now - 1000000000000L
       = ~730,000,000,000 ms
       = ~23 years
       >> 86,400,000 ms (24h limit)
       → EXPIRED. Rejected at the very first check. DB updated.
```

All three attacks produce real `TransactionLedger` entries in PostgreSQL — visible in the dashboard's ledger table within seconds.

---

## 🔐 The Cryptography: RSA-2048 + AES-256-GCM

### Why Not Just RSA?

RSA-2048 has a maximum plaintext size of ~245 bytes. A `PaymentInstruction` JSON is typically 150–300 bytes — right at the edge, and that's before any future additions. More importantly, RSA is ~1000× slower than AES for bulk data. Using RSA alone to encrypt payloads doesn't scale.

### Why Not Just AES?

AES is symmetric — both sides need the same secret key. How do you securely get the AES key to the server? You'd need to encrypt the key somehow... which brings you back to asymmetric cryptography.

### The Hybrid Solution

Use RSA for what it's good at (key exchange), and AES for what it's good at (bulk encryption):

```
ENCRYPT (sender's phone):
─────────────────────────
Step 1: Generate a random AES-256 key       [one-time-use, per payment]
Step 2: Generate a random 12-byte IV
Step 3: AES-256-GCM.encrypt(PaymentJSON, aesKey, iv) → ciphertext + auth tag
Step 4: RSA-2048.encrypt(aesKey, serverPublicKey)   → encryptedAesKey

Packet carries: { encryptedAesKey, iv, ciphertext }


DECRYPT (settlement service — server side only):
─────────────────────────────────────────────────
Step 1: RSA-2048.decrypt(encryptedAesKey, serverPrivateKey) → aesKey
Step 2: AES-256-GCM.decrypt(ciphertext, aesKey, iv)        → PaymentJSON
Step 3: JSON deserialize → PaymentInstruction object
```

### Why AES-GCM Specifically?

AES-GCM is **authenticated encryption**. Unlike AES-CBC (which only encrypts), AES-GCM also computes a 128-bit **authentication tag** over the ciphertext. This tag is mathematically tied to both the key and the ciphertext.

If anyone modifies even **one bit** of the encrypted data, the authentication tag verification fails and decryption throws a `GeneralSecurityException`. This gives us *two independent tamper-detection layers*:

1. **SHA-256 integrity check (Stage 2)** — catches tampering at the packet level (fast, no decryption needed)
2. **AES-GCM authentication tag** — catches tampering at the cryptographic level (during decryption in settlement)

An attacker would need to break AES-256 AND produce a valid authentication tag to bypass both. That's computationally infeasible.

### How the Key Pair Is Shared Between Services

```
Docker Volume: zeromesh-keys
               (mounted by BOTH ingestion and settlement containers)

                    /app/zeromesh-keys/
                    ├── private.key   ← Base64-encoded PKCS8
                    └── public.key    ← Base64-encoded X509

On FIRST startup:
  SharedRsaKeyPairManager generates RSA-2048 key pair
  Writes both keys to /app/zeromesh-keys/

On SUBSEQUENT startups (second service, or restarts):
  SharedRsaKeyPairManager reads existing keys from volume
  Both services always use the same matching key pair
  
Ingestion Service:  loads public.key  → used to ENCRYPT payments
Settlement Service: loads private.key → used to DECRYPT payments
```

No key distribution protocol, no secrets manager, no manual copy-paste. The shared Docker volume handles it automatically.

---

## 🏗 System Architecture

```
                    ┌─────────────────────────────────────────┐
                    │         INGESTION SERVICE               │
                    │              Port 8080                  │
                    │                                         │
        HTTP ──────►│  ┌───────────────────────────────────┐ │
                    │  │         REST Controllers           │ │
        Browser ───►│  │  MeshController    /api/v1/mesh/* │ │
        (Dashboard) │  │  BridgeController  /api/v1/bridge/*│ │
                    │  │  AccountController /api/v1/accounts│ │
                    │  └──────────────┬────────────────────┘ │
                    │                 │                       │
                    │  ┌──────────────▼────────────────────┐ │
                    │  │     BridgeIngestionService        │ │
                    │  │     (4-Stage Security Pipeline)   │ │
                    │  └──────────────┬────────────────────┘ │
                    │                 │                       │
                    │  ┌──────────────▼────────────────────┐ │
                    │  │    SettlementEventProducer         │ │
                    │  └──────────────┬────────────────────┘ │
                    └─────────────────┼───────────────────────┘
                                      │
                              Kafka Topic:
                        mesh-payment-settlements
                                      │
                    ┌─────────────────▼───────────────────────┐
                    │         SETTLEMENT SERVICE              │
                    │              Port 8081                  │
                    │                                         │
                    │  ┌───────────────────────────────────┐  │
                    │  │   SettlementEventConsumer          │  │
                    │  │   @KafkaListener + @Transactional  │  │
                    │  │   @RetryableTopic (3 attempts)     │  │
                    │  └───────────────────────────────────┘  │
                    └─────────────────────────────────────────┘

    ┌──────────────┐    ┌──────────────┐    ┌──────────────────────┐
    │  PostgreSQL  │    │    Redis     │    │   zeromesh-keys      │
    │  zeromeshdb  │    │  Port 6379   │    │   Docker Volume      │
    │              │    │              │    │                      │
    │  accounts    │    │  Idempotency │    │  private.key         │
    │  ├ id        │    │  Keys:       │    │  public.key          │
    │  ├ upi_id    │    │  zeromesh:   │    │                      │
    │  ├ name      │    │  packet:*    │    │  (shared between     │
    │  ├ balance   │    │  TTL: 25h    │    │  both containers)    │
    │  └ version   │    │              │    │                      │
    │              │    │              │    └──────────────────────┘
    │  tx_ledger   │    │              │
    │  ├ packet_id │    │              │
    │  ├ sender_id │    │              │
    │  ├ receiver  │    │              │
    │  ├ amount    │    │              │
    │  ├ status    │    │              │
    │  └ settled_at│    │              │
    └──────────────┘    └──────────────┘
```

---

## 🛠 Tech Stack & Why Each Choice

| Technology | Role | Why This, Not Something Else |
|-----------|------|------------------------------|
| **Java 17** | Language | LTS release, virtual threads preview, strong cryptographic library (JCA) built-in |
| **Spring Boot 3.x** | Application Framework | Auto-configuration, production-ready health checks, native Kafka/Redis/JPA integration |
| **Apache Kafka** | Async Message Queue | Durable (messages survive crashes), ordered per partition, built-in retry and DLT support. Unlike RabbitMQ, Kafka retains messages even after consumption — critical for payment audit |
| **Redis 7** | Idempotency Store | Sub-millisecond SETNX atomic operation. A DB unique constraint would require an expensive write+catch-exception cycle. Redis catches duplicates at the edge, before Kafka or DB are even touched |
| **PostgreSQL 15** | Primary Database | Full ACID transactions, row-level locking, `@Version` optimistic locking support via JPA. H2 was not used — we're running real PostgreSQL exactly as production would |
| **RSA-2048 + AES-256-GCM** | Hybrid Encryption | Industry standard. RSA for key exchange, AES-GCM for speed + authenticated encryption (built-in tamper detection). Zero external library dependency — uses Java's built-in JCA |
| **Docker Compose** | Orchestration | 5-service setup (ingestion, settlement, kafka, redis, postgres) reproducible with one command. Shared volume for RSA keys. |
| **Maven Multi-Module** | Build | `common-mesh-dto` shared library compiled once and packaged into both service JARs |
| **Vanilla HTML/CSS/JS** | Dashboard | No framework overhead. Single-file deployment. Runs from Spring Boot's static resources. |

---

## 📁 Project Structure

```
ZeroMesh-UPI/
│
├── 📦 common-mesh-dto/                    ← Shared library (compiled into both services)
│   └── src/main/java/com/zeromesh/
│       ├── config/
│       │   └── SharedRsaKeyPairManager.java   ← Generates/loads RSA-2048 key pair from disk
│       ├── dto/
│       │   ├── EncryptedPayloadDto.java        ← { encryptedAesKey, iv, ciphertext }
│       │   ├── PaymentInstruction.java         ← { senderId, receiverId, amount, nonce, timestamp }
│       │   └── ErrorResponse.java             ← RFC-7807 error format
│       ├── exception/
│       │   ├── AccountNotFoundException.java
│       │   ├── InsufficientFundsException.java
│       │   ├── DuplicateTransactionException.java
│       │   └── PacketTamperedException.java
│       └── model/
│           ├── Account.java            ← JPA entity with @Version optimistic lock
│           ├── MeshPacket.java         ← Bluetooth mesh envelope { packetId, ttl, createdAt, payload }
│           ├── TransactionLedger.java  ← Audit log: SETTLED / DUPLICATE / EXPIRED / TAMPERED
│           └── VirtualDevice.java     ← In-memory virtual phone for mesh simulation
│
├── 🌐 ingestion-service/                  ← Port 8080 | Validation + Dashboard + Kafka Producer
│   └── src/main/java/com/zeromesh/
│       ├── IngestionServiceApplication.java
│       ├── config/
│       │   ├── DataSeederConfig.java          ← Seeds 3 demo accounts on first boot
│       │   └── KafkaTopicConfig.java          ← Topic names + partition config
│       ├── controller/
│       │   ├── BridgeController.java          ← /ingest, /ingest/batch, /attack/*
│       │   ├── MeshController.java            ← /inject, /gossip, /flush, /reset, /status
│       │   └── AccountController.java         ← /accounts, /ledger, /reset, /ledger/clear
│       ├── exception/
│       │   └── GlobalExceptionHandler.java   ← Centralized @RestControllerAdvice
│       ├── kafka/
│       │   └── SettlementEventProducer.java   ← Publishes to Kafka (packetId as message key)
│       ├── repository/
│       │   ├── AccountRepository.java
│       │   └── TransactionLedgerRepository.java
│       └── service/
│           ├── ingestion/
│           │   └── BridgeIngestionService.java  ← ⭐ THE 4-STAGE PIPELINE
│           ├── validation/
│           │   └── PacketValidationService.java ← Expiry + SHA-256 integrity
│           ├── idempotency/
│           │   └── IdempotencyService.java      ← Redis SETNX claim/release/markSettled
│           ├── crypto/
│           │   └── CryptoService.java           ← RSA + AES-GCM encrypt/decrypt
│           └── mesh/
│               └── MeshSimulatorService.java    ← Virtual Bluetooth mesh (5 phones)
│   └── src/main/resources/
│       ├── application.yml              ← Local config
│       ├── application-docker.yml       ← Docker config (overrides local)
│       └── static/
│           └── index.html              ← Complete real-time dashboard (single file)
│
├── ⚙️  settlement-service/                ← Port 8081 | Kafka Consumer + ACID Settlement
│   └── src/main/java/com/zeromesh/
│       ├── SettlementServiceApplication.java
│       ├── config/KafkaTopicConfig.java
│       ├── kafka/
│       │   └── SettlementEventConsumer.java  ← ⭐ DECRYPT + DEBIT + CREDIT + LEDGER
│       ├── repository/
│       │   ├── AccountRepository.java
│       │   └── TransactionLedgerRepository.java
│       └── service/
│           ├── crypto/CryptoService.java       ← Same hybrid crypto (decrypt path used here)
│           └── idempotency/IdempotencyService.java ← markSettled / release on failure
│
├── docker-compose.yml                  ← 5-service orchestration (all containers)
├── DEPLOYMENT.md                       ← Render.com deployment guide
├── .gitignore                          ← Ignores target/, zeromesh-keys/, .idea/
└── README.md                           ← You are here
```

---

## 🚀 Getting Started (5 Minutes)

### You Need

- **Docker Desktop** — [Download](https://www.docker.com/products/docker-desktop/) (v4.x or later)
- **Git**

That's it. Java 17, Maven, PostgreSQL, Kafka, Redis — everything runs inside Docker. Nothing to install locally.

### Step 1: Clone the Repository

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/ZeroMesh-UPI.git

# Navigate into the project root directory
cd ZeroMesh-UPI
```

### Step 2: Start All Services via Docker Compose

```bash
docker compose up -d --build
```

> ⏱️ **First run:** ~3-4 minutes (Maven downloads dependencies and builds both microservices inside the containers — fully cached for subsequent runs).  
> ⚡ **Subsequent runs:** ~15-30 seconds.

### Step 3: Verify Container Health & Startup Logs

Watch the ingestion service startup logs:
```bash
docker compose logs -f ingestion-service
```

You should see:
```text
[KeyManager] Generated new RSA-2048 key pair → saved to: zeromesh-keys
[ZeroMesh] Network initialized with 5 devices.
[ZeroMesh] Seeded 3 accounts into DB.
Started IngestionServiceApplication in 8.3 seconds
```

### Step 4: Network & Port Allocation

All services are containerized and mapped to localhost:

| Service | Container Name | Host Port | Internal Port | Protocol / Purpose |
|---------|----------------|-----------|---------------|-------------------|
| **Ingestion Service** | `zeromesh-ingestion` | `8080` | `8080` | HTTP / REST API & Web Dashboard |
| **Settlement Service** | `zeromesh-settlement` | `8081` | `8081` | Internal Consumer / Health |
| **PostgreSQL 15** | `zeromesh-postgres` | `5432` | `5432` | JDBC / Ledger & Account Store |
| **Redis 7** | `zeromesh-redis` | `6379` | `6379` | RESP / Distributed Idempotency |
| **Apache Kafka** | `zeromesh-kafka` | `9092` | `9092` | PLAINTEXT / Event Stream |

### Step 5: Access the Live Dashboard

Open your browser and navigate to:
👉 **[http://localhost:8080](http://localhost:8080)**

Verify all 5 containers are running and healthy:
```bash
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

### Stopping or Resetting the Environment

```bash
# Graceful stop
docker compose down

# Full wipe (removes Docker volumes, resets PostgreSQL & Redis state)
docker compose down -v
```

### 🛠️ Common Troubleshooting

- **Port 5432 or 8080 already in use?** If you have a local PostgreSQL or Tomcat/Spring server running on your machine, stop it or update the host port in `docker-compose.yml` (e.g., `5433:5432` or `8082:8080`).
- **Docker Desktop Engine not responding?** Ensure Docker Desktop is running and WSL2 / Linux container mode is enabled with at least 4GB of RAM allocated.

---

## 📡 API Reference

### Mesh Simulation

```
POST /api/v1/mesh/inject    → Create offline payment (inject into phone-alice)
POST /api/v1/mesh/gossip    → One Bluetooth gossip round (packet hops to all neighbors)
POST /api/v1/mesh/flush     → Bridge walks outside — upload all held packets to server
POST /api/v1/mesh/reset     → Reset all virtual devices (clear packets, fresh demo)
GET  /api/v1/mesh/status    → Live packet count on each of the 5 virtual phones
```

**Inject a Payment:**
```bash
curl -X POST http://localhost:8080/api/v1/mesh/inject \
  -H "Content-Type: application/json" \
  -d '{
    "senderId":   "sundram@upi",
    "receiverId": "rahul@upi",
    "amount":     "500"
  }'

# Response:
{
  "message":  "Packet injected into phone-alice",
  "packetId": "abc123xyz...",
  "ttl":      5
}
```

### Bridge Upload

```
POST /api/v1/bridge/ingest        → Upload single MeshPacket (runs 4-stage pipeline)
POST /api/v1/bridge/ingest/batch  → Upload multiple packets at once
```

### Accounts & Ledger

```
GET  /api/v1/accounts              → All accounts with current balances
GET  /api/v1/accounts/{upiId}      → Single account
GET  /api/v1/accounts/ledger       → Full transaction audit log
POST /api/v1/accounts/reset        → Reset balances (Sundram ₹2000, Rahul ₹1000, Priya ₹1500)
POST /api/v1/accounts/ledger/clear → Clear all ledger entries
```

### Security Chaos Engine

```
POST /api/v1/bridge/attack/tamper   → Simulate MitM tamper attack → response: TAMPERED
POST /api/v1/bridge/attack/replay   → Simulate replay attack      → response: DUPLICATE
POST /api/v1/bridge/attack/expired  → Simulate stale packet        → response: EXPIRED
```

### Transaction Status Values

| Status | Meaning | Which Stage |
|--------|---------|-------------|
| `SETTLED` | Payment completed, balances updated | Stage 4 → Settlement Service |
| `TAMPERED` | SHA-256 hash mismatch, ciphertext modified | Stage 2 |
| `DUPLICATE` | packetId already seen in Redis | Stage 3 |
| `EXPIRED` | Packet older than 24 hours | Stage 1 |
| `INSUFFICIENT_FUNDS` | Sender balance too low | Settlement Service |

---

## 🎬 Full Demo Walkthrough

### The Complete Offline Payment Story

#### 1. Start with a Clean State
```
Click: "Reset Balances"   → Sundram: ₹2000, Rahul: ₹1000, Priya: ₹1500
Click: "Clear Ledger"     → Empty ledger
Click: "Reset Mesh"       → All 5 phones clear, no packets
```

#### 2. Sundram Creates a Payment (Offline, No Internet)
```
Fill: Sender = sundram@upi, Receiver = rahul@upi, Amount = 500
Click: "Send Payment"

Behind the scenes:
→ PaymentInstruction JSON built
→ AES-256-GCM encrypts it
→ RSA-2048 wraps the AES key
→ SHA-256(ciphertext) = packetId
→ MeshPacket { packetId, ttl=5, createdAt=now, payload }
→ Injected into phone-alice

Network status shows: phone-alice: 1 packet, others: 0
```

#### 3. Packet Travels Through the Mesh
```
Click "Gossip" once:
  phone-alice   (TTL=5) → forwards to all → everyone gets it at TTL=4
  Network: all 5 phones have the packet

Click "Gossip" again:
  All phones try to forward → but everyone already has it → ignored as duplicates
  TTL now 3 on all copies
  
(You can gossip up to 5 times before TTL hits 0)
```

#### 4. Bridge Gets WiFi — Settles the Payment
```
Click: "Flush Bridge"

phone-bridge has internet → uploads its packet to /api/v1/bridge/ingest

4-stage pipeline:
  Stage 1: createdAt is recent → ✅ not expired
  Stage 2: SHA-256(ciphertext) == packetId → ✅ not tampered
  Stage 3: Redis SETNX → ✅ first time seeing this packetId
  Stage 4: Published to Kafka mesh-payment-settlements

Settlement Service (async, ~100-500ms later):
  → RSA decrypt → AES-GCM decrypt → PaymentInstruction recovered
  → Sundram balance: 2000 - 500 = 1500
  → Rahul balance:   1000 + 500 = 1500
  → Ledger: SETTLED entry written
  → Redis: packetId marked SETTLED
```

#### 5. Observe Results (Auto-refresh every 6s)
```
Sundram: ₹1500  (was ₹2000)
Rahul:   ₹1500  (was ₹1000)
Ledger:  1 entry — SETTLED | sundram@upi → rahul@upi | ₹500
```

#### 6. Watch the Security Attacks in Action
```
Click "🔴 Tamper Attack"
  → Ledger: TAMPERED | REJECTED → REJECTED | ₹0

Click "⚫ Replay Attack"  
  → Ledger: DUPLICATE | REPLAY_ATTACK → BLOCKED | ₹0

Click "🟠 Expired Packet"
  → Ledger: EXPIRED | REJECTED → REJECTED | ₹0
```

All three attacks — real rejection, real DB entries, real-time visibility. No mocking.

---

## 🧠 Key Design Decisions (The "Why" Behind Everything)

### Why Kafka Instead of Synchronous Settlement?

**Availability + Durability.**

If settlement happened synchronously (ingestion → DB directly), then:
- A DB slowdown makes the bridge phone wait → timeout → it retries → duplicate
- A settlement crash mid-write → payment in unknown state

With Kafka:
- Ingestion returns in ~3ms regardless of DB speed
- If settlement service crashes, the message sits safely in Kafka
- When settlement restarts, it picks up exactly where it left off
- `@RetryableTopic` handles transient failures automatically
- `DLT` catches anything that fails 3+ times — never a silent loss

### Why Redis for Dedup Instead of Just the DB Unique Constraint?

Both are used, but Redis is the **fast first-line check** and the DB constraint is the **final safety net.**

Redis approach (used):
```
Stage 3: Redis SETNX → sub-millisecond, before Kafka touch, before DB touch
Result: duplicate blocked in ~1ms, zero wasted resources
```

DB-only approach (what we avoid):
```
Stage 3: INSERT into DB → wait for DB round-trip → unique constraint violation → exception
Result: duplicate blocked in ~5-50ms, but DB was written and rolled back wastefully
         AND Kafka message might have already been published
```

Redis catches it early, cheap, and atomically. The DB unique constraint is still there — if Redis is somehow bypassed (Redis down, direct DB insert, etc.), the DB won't allow a duplicate `packetId`.

### Why `@Version` Optimistic Locking on Account?

**Preventing the double-spend race condition.**

Without it:
```
Time  Thread1 (Settlement 1)    Thread2 (Settlement 2)
0ms   READ  Sundram: ₹2000      READ  Sundram: ₹2000
10ms  DEDUCT ₹1500              DEDUCT ₹1500
20ms  WRITE ₹500    ✅          WRITE ₹500    ✅  ← WRONG! Should be ₹-1000 (insufficient)
```

Both threads read the same balance and both succeed. ₹3000 deducted, only ₹1500 reflected. Money created from thin air.

With `@Version`:
```
Time  Thread1 (Settlement 1)    Thread2 (Settlement 2)
0ms   READ  Sundram: ₹2000 (version=5)
0ms                              READ  Sundram: ₹2000 (version=5)
10ms  WRITE ₹500, version=5→6  ✅
20ms                              WRITE ₹500 WHERE version=5
                                  → version is now 6, not 5
                                  → OptimisticLockException thrown
                                  → Kafka retries → re-reads ₹500
                                  → insufficient funds → blocked ✅
```

### Why Does `CryptoService` Exist in Both Services?

Both services compile into separate, independently deployable Docker images. The `common-mesh-dto` module is a shared library for models and DTOs, but runtime service logic stays within each service for clean boundaries.

Ingestion needs `CryptoService.encrypt()` (for the `/mesh/inject` endpoint that simulates a sender's phone).  
Settlement needs `CryptoService.decrypt()` (for decrypting incoming Kafka messages).

Same logic, same file — but packaged separately. This keeps each service self-contained and independently deployable without coupling build artifacts.

---

## 👤 Demo Accounts

| UPI ID | Name | Starting Balance |
|--------|------|-----------------|
| `sundram@upi` | Sundram | ₹2,000 |
| `rahul@upi` | Rahul | ₹1,000 |
| `priya@upi` | Priya | ₹1,500 |

You can send between any two accounts. Use **Reset Balances** to restore starting values after testing.

---

## 🗺 What This Demonstrates

This project is a working proof-of-concept for:

1. **Offline-first payment systems** — payments that don't require the sender or receiver to have internet
2. **Bluetooth mesh networking** — epidemic-protocol gossip for packet propagation
3. **Hybrid cryptography in practice** — RSA + AES-GCM end-to-end encryption with no external crypto libraries
4. **Distributed idempotency** — Redis SETNX for exactly-once processing in a distributed system
5. **Event-driven microservices** — Kafka-based async settlement with retry and dead-letter handling
6. **Security pipeline design** — layered defenses catching replay, tampering, and expiry attacks independently
7. **Optimistic concurrency control** — JPA `@Version` preventing double-spend race conditions

---

<div align="center">

---

*Built to answer one question:*

**What does a truly offline, cryptographically secure payment system actually look like when you implement it?**

---

</div>
