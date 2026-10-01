# ZeroMesh-UPI: Production Deployment Blueprint 🚀

This document covers two battle-tested deployment paths:
1. **100% Free Cloud Deployment** (Render.com + Neon.tech + Upstash) — Zero credit card, zero maintenance, permanent live demo URL.
2. **AWS EC2 Cloud Deployment** — Single-server containerized deployment using Docker Compose.

---

## 🌟 PATH 1: 100% Free Cloud Architecture (Recommended for Portfolio)

```
                     ┌──────────────────────────────────────────────┐
                     │               RENDER.COM (FREE)              │
                     │                                              │
                     │  ┌────────────────────┐ ┌─────────────────┐  │
                     │  │ Ingestion Gateway  │ │Settlement Engine│  │
                     │  │  (Web Service :80) │ │ (Background)    │  │
                     │  └─────────┬──────────┘ └────────┬────────┘  │
                     └────────────┼─────────────────────┼───────────┘
                                  │                     │
         ┌────────────────────────┼─────────────────────┴─────────────┐
         │                        │                                   │
         ▼                        ▼                                   ▼
┌─────────────────┐      ┌─────────────────┐                ┌──────────────────┐
│   UPSTASH REDIS │      │  UPSTASH KAFKA  │                │ NEON.TECH POSTGRES│
│  (Serverless)   │      │  (Serverless)   │                │   (Serverless)   │
│ Port 6379 (TLS) │      │ Port 9092 (TLS) │                │ Port 5432 (Pool) │
└─────────────────┘      └─────────────────┘                └──────────────────┘
```

### Step 1: Create Free Cloud Managed Infrastructure

#### A. Free PostgreSQL on Neon.tech:
1. Sign up on [Neon.tech](https://neon.tech) (Free tier, no credit card required).
2. Create project `zeromesh-db`.
3. Copy your connection string:
   ```env
   SPRING_DATASOURCE_URL=jdbc:postgresql://ep-xyz.us-east-2.aws.neon.tech/zeromeshdb?sslmode=require
   SPRING_DATASOURCE_USERNAME=zeromesh_owner
   SPRING_DATASOURCE_PASSWORD=your_neon_password
   ```

#### B. Free Redis on Upstash:
1. Sign up on [Upstash.com](https://upstash.com) (Free tier: 10,000 commands/day).
2. Create Redis Database: `zeromesh-redis`.
3. Copy connection details:
   ```env
   SPRING_DATA_REDIS_HOST=your-db.upstash.io
   SPRING_DATA_REDIS_PORT=6379
   SPRING_DATA_REDIS_PASSWORD=your_upstash_redis_token
   ```

#### C. Free Apache Kafka on Upstash:
1. On Upstash dashboard, create Kafka Cluster: `zeromesh-kafka`.
2. Create topic: `mesh-payment-settlements` (partitions: 3).
3. Create topic: `mesh-payment-settlements-dlt` (partitions: 1).
4. Copy SASL/SCRAM connection string:
   ```env
   SPRING_KAFKA_BOOTSTRAP_SERVERS=your-kafka.upstash.io:9092
   ```

---

### Step 2: Deploy to Render.com

1. Push your repository to GitHub.
2. Sign up on [Render.com](https://render.com) using GitHub.
3. Click **New +** $\rightarrow$ **Web Service**.
4. Connect your `ZeroMesh-UPI` repository.
5. Settings:
   - **Environment**: `Docker`
   - **Dockerfile Path**: `ingestion-service/Dockerfile`
   - **Docker Context**: `.` (Root repository)
6. Add Environment Variables:
   - `SPRING_DATASOURCE_URL`: (from Neon)
   - `SPRING_DATASOURCE_USERNAME`: (from Neon)
   - `SPRING_DATASOURCE_PASSWORD`: (from Neon)
   - `SPRING_DATA_REDIS_HOST`: (from Upstash)
   - `SPRING_KAFKA_BOOTSTRAP_SERVERS`: (from Upstash)
7. Click **Deploy Web Service**!
   Render will build the multi-stage Docker image and assign a live HTTPS URL:
   👉 `https://zeromesh-upi.onrender.com`

---

## ☁️ PATH 2: AWS EC2 Hands-on Deployment (Single Ubuntu Node)

### Step 1: Launch EC2 Instance
1. Login to AWS Console $\rightarrow$ EC2 $\rightarrow$ Launch Instance.
2. Configuration:
   - **Name**: `zeromesh-production`
   - **OS**: Ubuntu 22.04 LTS (Free Tier eligible)
   - **Instance Type**: `t2.micro` (or `t3.small` if available)
   - **Key Pair**: Create new `zeromesh-key.pem` and download.
   - **Security Group (Inbound Rules)**:
     - Port 22 (SSH) $\rightarrow$ My IP
     - Port 8080 (Ingestion UI) $\rightarrow$ 0.0.0.0/0 (Anywhere)
     - Port 8081 (Settlement) $\rightarrow$ My IP (Restricted)

### Step 2: Connect via SSH
Open PowerShell on your laptop:
```bash
ssh -i "zeromesh-key.pem" ubuntu@ec2-your-instance-ip.compute-1.amazonaws.com
```

### Step 3: Install Docker & Docker Compose on Ubuntu
```bash
# Update and install Docker
sudo apt update && sudo apt upgrade -y
sudo apt install -y docker.io docker-compose-v2 git

# Allow ubuntu user to run docker without sudo
sudo usermod -aG docker ubuntu
newgrp docker
```

### Step 4: Clone & Launch ZeroMesh Stack
```bash
# Clone repository
git clone https://github.com/your-username/ZeroMesh-UPI.git
cd ZeroMesh-UPI

# Launch complete 5-container ecosystem
docker compose up -d --build

# Verify all 5 containers are healthy
docker compose ps
```

### Step 5: Access Live UI
Open browser and navigate to:
👉 `http://your-ec2-public-ip:8080`

### ⚠️ IMPORTANT AWS COST SAFETY:
When you are done testing/demonstrating on AWS:
```bash
# On your AWS Console:
EC2 -> Instances -> Select 'zeromesh-production' -> Instance State -> Terminate
```
*(Terminating deletes the instance so you are never billed \$1!)*
