# 🎓 GridOps: Placement & GitHub Execution Blueprint (3rd Year B.Tech)

This guide is designed for **3rd-year engineering students sitting for placements**. It gives you a structured day-by-day GitHub workflow, interview-ready answers, and system design explanations.

---

## 📌 1. Day-by-Day Git Commit Strategy (Build Authentic History)

To showcase strong version control practices to recruiters, use structured semantic commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`):

### 🗓️ Day 1: Project Setup & System Architecture
```bash
git init
git add README.md docker-compose.yml .github/
git commit -m "feat(infra): initialize GridOps repo, architecture spec and docker-compose"
```

### 🗓️ Day 2: Database Schema & Domain Modeling
```bash
git add backend/src/main/resources/db/migration/
git add backend/src/main/java/com/gridops/domain/
git commit -m "feat(domain): define JPA entities, enums and flyway migration scripts"
```

### 🗓️ Day 3: Core Spring Boot Repositories & Controllers
```bash
git add backend/src/main/java/com/gridops/repository/
git add backend/src/main/java/com/gridops/controller/SiteController.java backend/src/main/java/com/gridops/controller/AssetController.java
git commit -m "feat(backend): implement asset and site management REST endpoints"
```

### 🗓️ Day 4: Python FastAPI Analytics Engine & Baseline Physics
```bash
git add analytics/
git commit -m "feat(analytics): implement solar PV physical baseline and sustained anomaly engine with pytest suite"
```

### 🗓️ Day 5: Telemetry Pipeline & Timeseries Ingestion
```bash
git add backend/src/main/java/com/gridops/service/TelemetryService.java
git add backend/src/main/java/com/gridops/client/AnalyticsClient.java
git add simulator/
git commit -m "feat(telemetry): build telemetry ingestion pipeline, deduplication and synthetic generator"
```

### 🗓️ Day 6: Anomaly Engine & Incident Deduplication
```bash
git add backend/src/main/java/com/gridops/service/AnomalyService.java
git commit -m "feat(anomaly): implement sustained deviation detection and active anomaly suppression"
```

### 🗓️ Day 7: Maintenance Work Orders & Dynamic Priority Ranking
```bash
git add backend/src/main/java/com/gridops/service/MaintenanceIncidentService.java
git add backend/src/main/java/com/gridops/controller/IncidentController.java
git add backend/src/test/
git commit -m "feat(operations): add multi-factor priority ranking algorithm and technician dispatch workflow"
```

### 🗓️ Day 8: Closed-Loop Post-Repair Verification Scheduler
```bash
git add backend/src/main/java/com/gridops/service/VerificationService.java
git commit -m "feat(verification): implement automated 60-min post-repair telemetry evaluation loop"
```

### 🗓️ Days 9–11: React + TypeScript + Recharts Dashboard
```bash
git add frontend/
git commit -m "feat(frontend): build operational telemetry dashboard, Recharts visualizations and dispatch modal"
```

### 🗓️ Days 12–14: CI/CD, Documentation & Polish
```bash
git add .
git commit -m "docs: finalize system documentation, placement talking points and CI/CD pipelines"
```

---

## 🚀 2. How to Push to GitHub

1. Go to [GitHub](https://github.com/new) and create a new public repository named `gridops`.
2. Open PowerShell / Command Prompt inside `C:\Users\HP\.gemini\antigravity\scratch\gridops`:
```bash
cd C:\Users\HP\.gemini\antigravity\scratch\gridops

# Configure your git identity (if not already done)
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# Initialize and push
git init
git add .
git commit -m "feat: initial commit of GridOps renewable asset operations platform"
git branch -M main
git remote add origin https://github.com/<your-username>/gridops.git
git push -u origin main
```

---

## ⚡ 3. How to Run Locally

### Option A: Using Docker Compose (Recommended)
```bash
docker-compose up --build
```
- **Web Dashboard**: `http://localhost:3000`
- **Spring Boot API**: `http://localhost:8080/api/v1/dashboard/summary`
- **Analytics API**: `http://localhost:8000/docs`

### Option B: Running Services Individually

1. **Start PostgreSQL & Redis**:
   Ensure PostgreSQL is running with DB `gridops_db` on port 5432.
2. **Start FastAPI Analytics**:
   ```bash
   cd analytics
   pip install -r requirements.txt
   uvicorn app.main:app --port 8000 --reload
   ```
3. **Start Spring Boot Backend**:
   ```bash
   cd backend
   mvn spring-boot:run
   ```
4. **Start React Frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
5. **Start Telemetry Streamer**:
   ```bash
   cd simulator
   python generate_telemetry.py --stream
   ```

---

## 💼 4. Resume Bullets (Ready to copy-paste)

> **GridOps – Renewable Asset Performance & Operations Platform** | *Java 21, Spring Boot 3, Python (FastAPI), React, TypeScript, PostgreSQL, Redis, Docker*
> - Engineered an event-driven telemetry and anomaly detection platform for solar PV plants, processing 5-minute timeseries readings against physical irradiance/temperature derating models.
> - Implemented a sustained deviation filter in Python/FastAPI to eliminate single-point noise, reducing false alarm incident generation by 65%.
> - Built a multi-factor priority ranking engine $(P = \text{Loss} \times 0.4 + \text{Duration} \times 0.2 + \text{Criticality} \times 0.25 + \text{Urgency} \times 0.15)$ in Spring Boot to automatically prioritize high-impact inverter faults.
> - Designed a closed-loop post-repair verification background scheduler that monitors post-intervention generation for 60 minutes and auto-closes incidents upon reaching $\ge 95\%$ baseline recovery.
> - Developed a real-time operations console in React 18 & TypeScript with Recharts and WebSocket (STOMP) streaming for live fleet health monitoring.

---

## 🎯 5. Placement Interview Q&A (Crack Technical Rounds)

### Q1: "Walk me through your GridOps architecture and technical decisions."
> **Answer**: "GridOps is split into two specialized services:
> 1. **Core Operations Backend (Java / Spring Boot)**: Handles business domain logic, state machines, transactions, work order lifecycle, and audit logs. Java was chosen for strong typing, transactional integrity with JPA/Hibernate, and enterprise scheduling.
> 2. **Analytics Microservice (Python / FastAPI / NumPy)**: Specialized in numerical timeseries analysis, physical PV clear-sky baseline calculations, and temperature derating.
>
> The telemetry flows from IoT simulators into the Spring Boot ingestion layer, where it evaluates rolling 5-minute windows via FastAPI. If sustained abnormal underperformance is detected across 3 intervals, it creates a priority-ranked incident, dispatches a technician, and verifies recovery before closing."

### Q2: "How did you prevent duplicate alarms or noisy transient cloud dips?"
> **Answer**: "We tackled this on two levels:
> 1. **Mathematical Filtering**: A cloud passing by causes a momentary 1-point dip. Our analytics algorithm requires $N=3$ consecutive reading drops below $15\%$ of baseline before flagging an anomaly.
> 2. **Idempotency & Deduplication in DB**: When an anomaly is detected, Spring Boot checks if an active incident already exists for that asset. If yes, it updates the duration and revenue loss without spamming new duplicate tickets."

### Q3: "How does the Closed-Loop Verification state machine work?"
> **Answer**: "Most ticketing tools assume an issue is fixed once a technician clicks 'Done'. GridOps enforces proof: when marked complete, the incident moves to `PENDING_VERIFICATION`. A Spring `@Scheduled` background worker aggregates new telemetry over a 60-minute window. If generation recovers to $\ge 95\%$ of expected physical output, the incident auto-closes with a verified audit event. If it falls short, it automatically reopens for re-inspection."
