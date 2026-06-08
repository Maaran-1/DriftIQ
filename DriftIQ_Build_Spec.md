# DriftIQ — Complete MVP Build Specification
> Version: 1.0 | Target: Antigravity Autonomous Build System
> Platform: Android First | Stack: Kotlin + FastAPI + PostgreSQL + Redis + ML

---

## 1. PRODUCT OVERVIEW

DriftIQ is a behavioral intelligence Android app that passively monitors device usage patterns, constructs a personalized behavioral digital twin, detects meaningful drift from the user's established baseline, and surfaces actionable wellness insights through an explainable risk engine.

**Core Loop:**
```
Collect → Model Baseline → Detect Drift → Score Risk → Surface Insight
```

**MVP Scope:**
- Android data collection service (foreground + background)
- Behavioral Digital Twin (BDT) per user
- Drift Detection Engine (DDE)
- Risk Engine (Level 0–5)
- User Dashboard (Daily / Weekly / Monthly)
- Backend API + AI inference pipeline
- Cloud sync with end-to-end encryption

---

## 2. COMPLETE FEATURE LIST

### 2.1 Data Collection
- F-DC-01: App usage duration per app per session
- F-DC-02: Session start/end timestamps
- F-DC-03: Unlock frequency and timestamps
- F-DC-04: Screen-on duration per hour/day
- F-DC-05: App category mapping (social, productivity, entertainment, learning, health)
- F-DC-06: Notification received count per app
- F-DC-07: Time-of-day usage distribution
- F-DC-08: Sleep estimation (first unlock to last lock before inactivity threshold)
- F-DC-09: Behavioral rhythm extraction (hourly usage vectors)
- F-DC-10: Routine stability index (day-over-day consistency score)

### 2.2 Behavioral Digital Twin
- F-BDT-01: Baseline construction (7-day minimum, 14-day optimal)
- F-BDT-02: Rolling baseline adaptation (exponential weighted moving average)
- F-BDT-03: Separate sub-models: sleep, activity, productivity, learning, social, consumption, routine
- F-BDT-04: Seasonality-aware modeling (weekday vs. weekend)
- F-BDT-05: Confidence score per baseline dimension
- F-BDT-06: Twin versioning (snapshots on major drift events)

### 2.3 Drift Detection
- F-DD-01: Per-dimension drift score (z-score normalized)
- F-DD-02: Composite behavioral drift score (0–100)
- F-DD-03: Drift velocity (rate of change over time)
- F-DD-04: Drift direction tagging (increase/decrease/pattern_shift)
- F-DD-05: Significant drift event logging
- F-DD-06: Drift attribution (which dimensions contribute most)

### 2.4 Risk Engine
- F-RE-01: Multi-dimensional risk scoring
- F-RE-02: Risk level assignment (0–5)
- F-RE-03: Explainability narrative per risk level
- F-RE-04: Risk trend tracking (improving / stable / worsening)
- F-RE-05: Risk history timeline

### 2.5 AI Outputs
- F-AI-01: Daily behavioral summary (natural language)
- F-AI-02: Weekly insight report
- F-AI-03: Monthly trend report
- F-AI-04: Drift event notifications
- F-AI-05: Behavioral timeline visualization data
- F-AI-06: Wellness score (composite, 0–100)

### 2.6 User Dashboard
- F-UX-01: Daily dashboard (today's scores, highlights)
- F-UX-02: Weekly dashboard (7-day view, drift chart)
- F-UX-03: Monthly dashboard (30-day trends)
- F-UX-04: Behavioral timeline (scrollable historical view)
- F-UX-05: Drift visualization (radar/line charts)
- F-UX-06: Sleep trend charts
- F-UX-07: Activity trend charts
- F-UX-08: Productivity trend charts
- F-UX-09: Learning trend charts
- F-UX-10: Risk trend chart

### 2.7 Onboarding & Consent
- F-OB-01: Explicit consent flow with data itemization
- F-OB-02: Permission grant flow (UsageStats, Notifications)
- F-OB-03: Baseline calibration progress indicator
- F-OB-04: Privacy settings screen
- F-OB-05: Data export (JSON)
- F-OB-06: Data deletion request

### 2.8 Notifications
- F-NT-01: Weekly insight push notification
- F-NT-02: Drift alert (configurable threshold)
- F-NT-03: Baseline ready notification
- F-NT-04: Risk level change notification (Level 3+)

---

## 3. USER PERSONAS

### P1 — Self-Aware Professional (Primary)
- Age: 28–42
- Goal: Track burnout risk, optimize routine
- Device: Android, mid-to-high end
- Pain: No objective data on behavior change

### P2 — Caregiver / Family Monitor (Secondary, MVP-limited)
- Age: 35–55
- Goal: Passive awareness of family member's wellness
- MVP: Self-monitoring only; caregiver mode post-MVP

### P3 — Mental Health Advocate
- Age: 22–35
- Goal: Supplement therapy with behavioral data
- Need: Export to share with therapist

---

## 4. USER FLOWS

### UF-01: First Launch
```
App Install → Splash → Welcome Screen → Consent Screen (itemized list) 
→ Grant UsageStats Permission → Grant Notification Permission 
→ Account Creation (email + password) → Calibration Explanation Screen 
→ Home Dashboard (empty state with progress bar)
```

### UF-02: Daily Use
```
Unlock → Background service collects data → App opened 
→ Home Dashboard loads → View Today's Scores 
→ Tap Insight Card → Full Insight Screen → Back
```

### UF-03: Drift Alert
```
Background service detects drift threshold breach 
→ Push Notification: "Your behavior pattern has shifted" 
→ Tap → Open App → Drift Detail Screen 
→ Explainability narrative → Historical comparison chart
```

### UF-04: Weekly Report
```
Sunday 8 PM → Push Notification: "Your weekly insights are ready" 
→ Tap → Weekly Dashboard → Dimension-by-dimension breakdown 
→ AI narrative → Risk trend
```

### UF-05: Data Management
```
Settings → Privacy → View collected data categories 
→ Export Data (JSON) | Delete All Data 
→ Confirm deletion → Account wiped → Logout
```

---

## 5. FUNCTIONAL REQUIREMENTS

### Collection Service
- FR-CS-01: Foreground service with persistent notification
- FR-CS-02: UsageStatsManager polling every 15 minutes
- FR-CS-03: Local SQLite buffering before sync
- FR-CS-04: Sync to backend on WiFi or configurable
- FR-CS-05: Battery-aware scheduling (WorkManager)
- FR-CS-06: Survive device reboot (BOOT_COMPLETED receiver)

### Digital Twin
- FR-BDT-01: Minimum 7 days data before twin activation
- FR-BDT-02: Re-baseline trigger: >30 days inactivity, user request
- FR-BDT-03: Weekday / weekend separate sub-baselines
- FR-BDT-04: Baseline stored per user on backend, cached on device

### Drift Detection
- FR-DD-01: Daily drift computation (scheduled nightly, 2 AM)
- FR-DD-02: Per-dimension z-score: drift = (today - baseline_mean) / baseline_std
- FR-DD-03: Composite drift = weighted average of dimension scores
- FR-DD-04: Minimum 7 baseline days required before drift scoring begins

### Risk Engine
- FR-RE-01: Risk computed from composite drift + velocity + duration
- FR-RE-02: Level thresholds configurable via backend config
- FR-RE-03: No diagnosis language in any output string
- FR-RE-04: Risk explanation must cite specific behavioral dimensions

### AI Outputs
- FR-AI-01: Natural language generation via LLM API (OpenAI GPT-4o or Claude)
- FR-AI-02: Prompt templates per output type (daily, weekly, monthly)
- FR-AI-03: Output cached on backend, delivered via API
- FR-AI-04: Fallback: rule-based text if LLM unavailable

---

## 6. NON-FUNCTIONAL REQUIREMENTS

| Attribute | Requirement |
|---|---|
| Battery Impact | < 2% additional drain per day |
| Collection Latency | UsageStats polling ≤ 15 min delay |
| Sync Latency | < 30s on WiFi |
| API Response Time | < 500ms p95 |
| Dashboard Load | < 1.5s cold start |
| Uptime | 99.5% backend SLA |
| Data Retention | User-configurable: 90/180/365 days |
| Encryption at Rest | AES-256 on device, AES-256 on backend |
| Encryption in Transit | TLS 1.3 |
| GDPR Compliance | Full data export + deletion |
| Min Android Version | Android 10 (API 29) |
| Target Android Version | Android 14 (API 34) |
| Offline Operation | Full local operation; sync when online |
| Crash Rate | < 0.5% sessions |

---

## 7. SCREEN-BY-SCREEN UI SPECIFICATION

### S-01: Splash Screen
- Full-screen logo animation (500ms)
- Auto-navigate to Welcome or Home (if authenticated)

### S-02: Welcome Screen
- Headline: "Understand Your Behavioral Patterns"
- Subtext: brief non-medical framing
- CTA: "Get Started"
- Secondary: "Sign In"

### S-03: Consent Screen
- Title: "What DriftIQ Collects"
- Itemized list with icons: App usage, Screen time, Unlock frequency, etc.
- "What We Never Collect" section
- "I Agree & Continue" button
- Link: Full Privacy Policy (WebView)

### S-04: Permission Grant — UsageStats
- Explanation screen
- "Grant Access" button → opens Android UsageStats settings
- Verify grant and continue

### S-05: Permission Grant — Notifications
- Explanation screen
- "Allow Notifications" → system dialog
- Skip option

### S-06: Account Creation
- Email field
- Password field (min 8 chars, show/hide)
- "Create Account" CTA
- "Sign In Instead" link
- No social login in MVP

### S-07: Sign In
- Email + Password
- "Forgot Password" → email reset
- "Create Account" link

### S-08: Calibration Screen
- Title: "Building Your Baseline"
- Progress bar: X of 14 days collected
- Explanation of what's happening
- "View Partial Dashboard" after 3 days
- Estimated ready date

### S-09: Home Dashboard (Daily)
- Top: Date + greeting
- Wellness Score card (large, circular gauge 0–100)
- Risk Level badge (color-coded: green/yellow/orange/red/dark red)
- Drift Score card
- Today's Highlights: 3 key behavioral observations
- Quick-access row: Sleep / Activity / Productivity / Social
- Bottom nav: Home | Timeline | Reports | Settings

### S-10: Drift Detail Screen
- Title: "Today's Behavioral Drift"
- Composite drift score
- Dimension breakdown (horizontal bar chart)
- Drift direction icons (up/down/wave)
- Explainability text: "Your social app usage increased 47% above your baseline"
- Historical drift line chart (30 days)

### S-11: Weekly Dashboard
- 7-day drift line chart
- Dimension cards: sleep / activity / productivity / learning / social
- AI-generated weekly narrative (3–5 sentences)
- Risk trend indicator
- Top changed behavior highlight

### S-12: Monthly Dashboard
- 30-day trend line charts per dimension
- Wellness score trend
- Behavioral shift summary
- Month-over-month comparison
- AI narrative

### S-13: Behavioral Timeline
- Vertical scrollable timeline
- Date-anchored entries
- Event types: Drift Alert, Insight, Risk Level Change, Baseline Update
- Tap entry → detail modal

### S-14: Sleep Trends
- Bar chart: estimated sleep duration per night (30 days)
- Average line overlay
- Deviation highlights
- Stats: avg, min, max, trend direction

### S-15: Activity Trends
- Hourly device activity heatmap (7 days)
- Unlock count chart
- Screen-on duration chart

### S-16: Productivity Trends
- Daily productivity app usage bar chart
- Top productivity apps
- Focus time estimation

### S-17: Learning Trends
- Learning app usage over time
- Learning consistency score
- Streak indicator

### S-18: Risk Trend Screen
- Risk level history chart
- Current level badge + description
- Explainability narrative
- "What This Means" section (non-medical, behavioral framing only)

### S-19: Settings
- Account section: email, password change
- Data section: sync status, export, delete
- Notifications: toggle types, thresholds
- Privacy: consent review, permissions
- Calibration: reset baseline
- About: version, privacy policy, terms

### S-20: Insight Detail Screen
- Full AI-generated insight
- Supporting data charts
- Related dimension links
- Timestamp

---

## 8. ANDROID APP ARCHITECTURE

### Pattern: Clean Architecture + MVI

```
app/
├── presentation/       # Screens, ViewModels, UI State
│   ├── home/
│   ├── drift/
│   ├── timeline/
│   ├── reports/
│   └── settings/
├── domain/             # Use Cases, Domain Models, Interfaces
│   ├── usecase/
│   ├── model/
│   └── repository/
├── data/               # Repositories, Data Sources, Mappers
│   ├── local/          # Room DB, DataStore
│   ├── remote/         # Retrofit API clients
│   └── repository/
├── service/            # Collection Service, WorkManager workers
│   ├── CollectionService.kt
│   ├── UsageStatsCollector.kt
│   └── SyncWorker.kt
└── di/                 # Hilt modules
```

### Key Components

**CollectionService.kt** — Foreground service
- Starts on boot via BroadcastReceiver
- Polls UsageStatsManager every 15 min via Handler
- Writes to local Room DB
- Schedules SyncWorker

**UsageStatsCollector.kt**
- Queries `UsageStatsManager.queryUsageStats()`
- Maps to `AppUsageEvent` domain model
- Derives: duration, category, session boundaries

**SyncWorker.kt** — WorkManager periodic task
- Constraint: NetworkType.CONNECTED
- Reads unsynced records from Room
- POST to `/api/v1/events/batch`
- Marks records as synced

**DigitalTwinViewModel.kt**
- Subscribes to backend twin state
- Exposes UI state via StateFlow
- Triggers refresh on foreground

### State Management: MVI
```kotlin
sealed class HomeUiState {
  object Loading : HomeUiState()
  data class Ready(
    val wellnessScore: Int,
    val driftScore: Float,
    val riskLevel: Int,
    val highlights: List<Highlight>,
    val baselineReady: Boolean
  ) : HomeUiState()
  data class Error(val message: String) : HomeUiState()
}
```

### Local Database: Room
Tables: `app_usage_events`, `sync_queue`, `cached_twin_state`, `cached_insights`

### Remote: Retrofit + OkHttp
- Base URL from BuildConfig
- Auth: Bearer JWT in header
- Interceptor for token refresh
- Response caching for insights

### Dependency Injection: Hilt
- `@Singleton`: Repository, ApiService, Database
- `@ViewModelScoped`: Use cases

---

## 9. BACKEND ARCHITECTURE

### Stack
- **Runtime**: Python 3.11
- **Framework**: FastAPI
- **Task Queue**: Celery + Redis
- **Database**: PostgreSQL 15
- **Cache**: Redis 7
- **ML Inference**: Scikit-learn + custom Python modules
- **LLM**: OpenAI GPT-4o API (or Anthropic Claude API)
- **Object Storage**: S3-compatible (AWS S3 or MinIO)
- **Container**: Docker + Docker Compose (dev), Kubernetes (prod)

### Service Decomposition (Monolith with internal modules, split at scale)

```
backend/
├── api/
│   ├── auth.py            # JWT issue/refresh/revoke
│   ├── events.py          # Ingest usage events
│   ├── twin.py            # Digital twin read/manage
│   ├── insights.py        # AI outputs
│   └── settings.py        # User config, data management
├── services/
│   ├── baseline_builder.py
│   ├── drift_detector.py
│   ├── risk_engine.py
│   ├── insight_generator.py
│   └── twin_manager.py
├── models/                # SQLAlchemy ORM models
├── schemas/               # Pydantic request/response schemas
├── tasks/                 # Celery async tasks
│   ├── nightly_compute.py
│   ├── sync_processor.py
│   └── insight_generator.py
├── ml/
│   ├── baseline/
│   ├── drift/
│   └── risk/
└── core/
    ├── config.py
    ├── security.py
    └── database.py
```

### API Gateway Pattern
- Nginx reverse proxy → FastAPI
- Rate limiting: 100 req/min per user
- JWT auth middleware on all `/api/v1/*` routes
- `/health` and `/api/v1/auth/*` are public

### Async Processing
- Ingest: Sync HTTP → write to DB → enqueue Celery task
- Nightly compute: Celery Beat scheduler at 02:00 UTC per user
- Insight generation: Celery task → LLM API → store result

---

## 10. AI ARCHITECTURE

### Pipeline Overview
```
Raw Events → Feature Extraction → Baseline Modeling 
→ Drift Computation → Risk Scoring → LLM Narrative Generation
```

### 10.1 Feature Extraction
Input: Raw `app_usage_events` for a given day
Output: `DailyFeatureVector`

```python
DailyFeatureVector:
  total_screen_time_minutes: float
  unlock_count: int
  unique_apps_used: int
  social_app_minutes: float
  productivity_app_minutes: float
  entertainment_app_minutes: float
  learning_app_minutes: float
  sleep_estimate_hours: float        # gap from last activity to first morning unlock
  peak_usage_hour: int               # hour with max activity
  usage_spread_entropy: float        # Shannon entropy of hourly usage
  late_night_usage_minutes: float    # 11 PM – 4 AM
  morning_usage_minutes: float       # 6 AM – 9 AM
  session_count: int
  avg_session_duration: float
  notification_count: int
```

App Category Mapping:
- Maintained as a JSON lookup: `{package_name: category}`
- Fallback: Play Store category via API or ML classifier
- Categories: SOCIAL, PRODUCTIVITY, ENTERTAINMENT, LEARNING, HEALTH, UTILITY, OTHER

### 10.2 Baseline Modeling

Per user, per dimension, maintain:
```python
BaselineDimension:
  dimension_name: str
  weekday_mean: float
  weekday_std: float
  weekend_mean: float
  weekend_std: float
  ema_alpha: float = 0.1   # slow adaptation
  n_samples: int
  confidence: float        # min(n_samples / 30, 1.0)
  last_updated: datetime
```

**EWMA update rule:**
```
new_mean = alpha * today_value + (1 - alpha) * old_mean
new_variance = alpha * (today_value - new_mean)^2 + (1 - alpha) * old_variance
new_std = sqrt(new_variance)
```

**Dimensions tracked:**
1. `sleep_hours`
2. `total_screen_time`
3. `unlock_count`
4. `social_minutes`
5. `productivity_minutes`
6. `entertainment_minutes`
7. `learning_minutes`
8. `late_night_usage`
9. `session_count`
10. `usage_entropy`

---

## 11. BEHAVIORAL DIGITAL TWIN DESIGN

### Twin State Object
```python
DigitalTwin:
  user_id: UUID
  version: int
  created_at: datetime
  updated_at: datetime
  baseline_days: int
  is_active: bool              # True after 7 days
  confidence_score: float      # Avg confidence across dimensions
  dimensions: Dict[str, BaselineDimension]
  snapshots: List[TwinSnapshot]  # Historical versions
```

### Twin Lifecycle
1. **Calibration** (days 0–7): Data collected, no drift computed, progress shown to user
2. **Soft Active** (days 7–14): Drift computed but flagged as low-confidence
3. **Active** (day 14+): Full operation
4. **Re-calibration**: Triggered by user request or 30+ days inactivity

### Snapshot Policy
- Snapshot created: every 30 days automatically
- Snapshot created: on risk level change ≥ 2 levels
- Snapshot created: on user-triggered re-baseline
- Retained: last 12 snapshots

---

## 12. DRIFT DETECTION ENGINE DESIGN

### Per-Dimension Z-Score
```python
def compute_dimension_drift(value: float, dimension: BaselineDimension, is_weekend: bool) -> float:
    mean = dimension.weekend_mean if is_weekend else dimension.weekday_mean
    std = dimension.weekend_std if is_weekend else dimension.weekday_std
    if std < 0.01:
        std = 0.01  # prevent division by zero
    z = (value - mean) / std
    return z
```

### Dimension Weights (MVP defaults)
```python
DIMENSION_WEIGHTS = {
    "sleep_hours": 0.20,
    "total_screen_time": 0.15,
    "unlock_count": 0.10,
    "social_minutes": 0.12,
    "productivity_minutes": 0.10,
    "entertainment_minutes": 0.08,
    "learning_minutes": 0.08,
    "late_night_usage": 0.10,
    "session_count": 0.05,
    "usage_entropy": 0.02,
}
```

### Composite Drift Score
```python
def compute_composite_drift(z_scores: Dict[str, float], weights: Dict[str, float]) -> float:
    weighted_sum = sum(abs(z_scores[k]) * weights[k] for k in z_scores)
    # Normalize to 0–100 scale: z-score of 3 = drift score of ~100
    normalized = min((weighted_sum / 3.0) * 100, 100)
    return round(normalized, 2)
```

### Drift Velocity
```python
# 3-day rolling slope of composite drift score
drift_velocity = linregress([-2, -1, 0], [drift_d_minus_2, drift_d_minus_1, drift_today]).slope
```

### Drift Event Logging
- Event logged when composite drift > threshold (default: 40)
- Event logged when single dimension z-score > 2.5
- Event stored in `drift_events` table

---

## 13. RISK ENGINE DESIGN

### Risk Formula
```python
def compute_risk_level(
    composite_drift: float,
    drift_velocity: float,
    sustained_days: int,          # days drift > 30
    confidence: float
) -> Tuple[int, str]:

    base_score = composite_drift * 0.6 + abs(drift_velocity) * 10 * 0.2 + sustained_days * 2 * 0.2
    adjusted = base_score * confidence  # low confidence → lower risk inflation

    if adjusted < 10:   return 0, "Healthy"
    if adjusted < 25:   return 1, "Observation"
    if adjusted < 45:   return 2, "Mild Concern"
    if adjusted < 65:   return 3, "Moderate Concern"
    if adjusted < 80:   return 4, "High Concern"
    return 5, "Critical"
```

### Risk Level Definitions
| Level | Label | Color | Action |
|---|---|---|---|
| 0 | Healthy | #2ECC71 | None |
| 1 | Observation | #F1C40F | Log only |
| 2 | Mild Concern | #E67E22 | Insight nudge |
| 3 | Moderate Concern | #E74C3C | Weekly report flag |
| 4 | High Concern | #C0392B | Push notification |
| 5 | Critical | #7B241C | Push notification + strong nudge |

### Explainability
```python
def generate_risk_explanation(z_scores: Dict[str, float], level: int) -> str:
    top_contributors = sorted(z_scores.items(), key=lambda x: abs(x[1]), reverse=True)[:3]
    # Build template: "Your {dimension} has shifted {direction} by {magnitude} compared to your usual pattern."
    # Templates stored in explanation_templates.json
```

**Critical rule**: No medical language. No diagnosis. No clinical terms. All language is behavioral framing only.

---

## 14. DATABASE SCHEMA

### PostgreSQL Tables

```sql
-- Users
CREATE TABLE users (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  email VARCHAR(255) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW(),
  is_active BOOLEAN DEFAULT TRUE,
  consent_given_at TIMESTAMPTZ,
  data_retention_days INT DEFAULT 365
);

-- App Usage Events (raw ingested data)
CREATE TABLE app_usage_events (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  package_name VARCHAR(255) NOT NULL,
  app_category VARCHAR(50),
  session_start TIMESTAMPTZ NOT NULL,
  session_end TIMESTAMPTZ NOT NULL,
  duration_seconds INT NOT NULL,
  event_date DATE GENERATED ALWAYS AS (session_start::DATE) STORED,
  synced_at TIMESTAMPTZ DEFAULT NOW(),
  device_id VARCHAR(255)
);
CREATE INDEX idx_usage_events_user_date ON app_usage_events(user_id, event_date);

-- Daily Feature Vectors
CREATE TABLE daily_features (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  feature_date DATE NOT NULL,
  total_screen_time_minutes FLOAT,
  unlock_count INT,
  unique_apps_used INT,
  social_minutes FLOAT,
  productivity_minutes FLOAT,
  entertainment_minutes FLOAT,
  learning_minutes FLOAT,
  sleep_estimate_hours FLOAT,
  peak_usage_hour INT,
  usage_entropy FLOAT,
  late_night_minutes FLOAT,
  morning_minutes FLOAT,
  session_count INT,
  avg_session_duration FLOAT,
  notification_count INT,
  is_weekend BOOLEAN,
  computed_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_id, feature_date)
);

-- Digital Twin
CREATE TABLE digital_twins (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  version INT DEFAULT 1,
  is_active BOOLEAN DEFAULT FALSE,
  baseline_days INT DEFAULT 0,
  confidence_score FLOAT DEFAULT 0.0,
  dimensions JSONB NOT NULL DEFAULT '{}',
  created_at TIMESTAMPTZ DEFAULT NOW(),
  updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Twin Snapshots
CREATE TABLE twin_snapshots (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  twin_id UUID REFERENCES digital_twins(id) ON DELETE CASCADE,
  snapshot_date DATE NOT NULL,
  reason VARCHAR(100),
  dimensions_snapshot JSONB NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Daily Drift Scores
CREATE TABLE drift_scores (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  score_date DATE NOT NULL,
  composite_drift FLOAT,
  drift_velocity FLOAT,
  dimension_z_scores JSONB,
  top_contributors JSONB,
  sustained_days INT DEFAULT 0,
  computed_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_id, score_date)
);

-- Risk Records
CREATE TABLE risk_records (
  id BIGSERIAL PRIMARY KEY,
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  record_date DATE NOT NULL,
  risk_level INT NOT NULL,
  risk_label VARCHAR(50),
  explanation TEXT,
  composite_drift FLOAT,
  drift_velocity FLOAT,
  is_notification_sent BOOLEAN DEFAULT FALSE,
  computed_at TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_id, record_date)
);

-- Drift Events
CREATE TABLE drift_events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  event_date DATE NOT NULL,
  event_type VARCHAR(50),    -- 'composite_threshold', 'dimension_spike', 'risk_change'
  dimension VARCHAR(50),
  z_score FLOAT,
  composite_drift FLOAT,
  risk_level INT,
  is_read BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMPTZ DEFAULT NOW()
);

-- AI Insights
CREATE TABLE ai_insights (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  insight_type VARCHAR(20),   -- 'daily', 'weekly', 'monthly'
  period_start DATE NOT NULL,
  period_end DATE NOT NULL,
  content TEXT NOT NULL,
  wellness_score INT,
  risk_level INT,
  metadata JSONB,
  generated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Refresh Tokens
CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID REFERENCES users(id) ON DELETE CASCADE,
  token_hash VARCHAR(255) UNIQUE NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ DEFAULT NOW(),
  revoked BOOLEAN DEFAULT FALSE
);
```

---

## 15. API SPECIFICATIONS

### Base URL: `/api/v1`
### Auth: `Authorization: Bearer <access_token>`

---

### Auth Endpoints

#### POST `/auth/register`
```json
Request: { "email": "str", "password": "str" }
Response 201: { "user_id": "uuid", "access_token": "str", "refresh_token": "str" }
Response 400: { "detail": "Email already registered" }
```

#### POST `/auth/login`
```json
Request: { "email": "str", "password": "str" }
Response 200: { "access_token": "str", "refresh_token": "str", "expires_in": 3600 }
```

#### POST `/auth/refresh`
```json
Request: { "refresh_token": "str" }
Response 200: { "access_token": "str", "expires_in": 3600 }
```

#### POST `/auth/logout`
```json
Request: { "refresh_token": "str" }
Response 200: { "message": "Logged out" }
```

---

### Events Endpoints

#### POST `/events/batch`
```json
Request:
{
  "device_id": "str",
  "events": [
    {
      "package_name": "com.instagram.android",
      "session_start": "2024-01-15T14:30:00Z",
      "session_end": "2024-01-15T14:45:00Z",
      "duration_seconds": 900
    }
  ]
}
Response 202: { "accepted": 47, "duplicate_skipped": 3 }
```

---

### Twin Endpoints

#### GET `/twin/status`
```json
Response 200:
{
  "is_active": true,
  "baseline_days": 21,
  "confidence_score": 0.78,
  "dimensions": {
    "sleep_hours": { "mean": 7.2, "std": 0.8, "confidence": 0.85 }
  },
  "calibration_progress": 1.0
}
```

#### POST `/twin/reset`
```json
Response 200: { "message": "Baseline reset initiated" }
```

---

### Drift Endpoints

#### GET `/drift/today`
```json
Response 200:
{
  "date": "2024-01-15",
  "composite_drift": 42.3,
  "drift_velocity": 2.1,
  "dimension_scores": {
    "sleep_hours": { "z_score": -1.8, "direction": "decrease", "value": 5.5, "baseline": 7.2 },
    "social_minutes": { "z_score": 2.3, "direction": "increase", "value": 180, "baseline": 65 }
  },
  "top_contributors": ["social_minutes", "sleep_hours", "late_night_usage"],
  "explanation": "Your social app usage has significantly increased while sleep duration has decreased."
}
```

#### GET `/drift/history?days=30`
```json
Response 200:
{
  "history": [
    { "date": "2024-01-15", "composite_drift": 42.3, "risk_level": 2 }
  ]
}
```

---

### Risk Endpoints

#### GET `/risk/current`
```json
Response 200:
{
  "level": 2,
  "label": "Mild Concern",
  "explanation": "Your sleep pattern and social behavior have drifted from your usual habits over the past 5 days.",
  "trend": "worsening",
  "sustained_days": 5,
  "color": "#E67E22"
}
```

#### GET `/risk/history?days=30`
```json
Response 200: { "history": [{ "date": "...", "level": 2, "label": "..." }] }
```

---

### Insights Endpoints

#### GET `/insights/daily`
```json
Response 200:
{
  "insight_id": "uuid",
  "type": "daily",
  "date": "2024-01-15",
  "content": "Today your screen time was 3.5 hours above your usual pattern...",
  "wellness_score": 62,
  "risk_level": 2
}
```

#### GET `/insights/weekly`
```json
Response 200:
{
  "insight_id": "uuid",
  "period_start": "2024-01-08",
  "period_end": "2024-01-15",
  "content": "This week showed a significant shift in your evening routines...",
  "wellness_score": 58,
  "dimension_summaries": { "sleep": "...", "social": "..." }
}
```

#### GET `/insights/monthly`
```json
Response 200: { ... similar structure ... }
```

---

### Dashboard Endpoints

#### GET `/dashboard/summary`
```json
Response 200:
{
  "wellness_score": 62,
  "drift_score": 42.3,
  "risk_level": 2,
  "baseline_active": true,
  "highlights": [
    { "type": "sleep", "message": "Sleep duration 2h below your average" },
    { "type": "social", "message": "Social app usage 47% above baseline" }
  ],
  "last_updated": "2024-01-15T03:00:00Z"
}
```

---

### Settings Endpoints

#### GET `/settings`
#### PATCH `/settings`
```json
Request: { "data_retention_days": 180, "drift_alert_threshold": 50 }
```

#### POST `/settings/export`
```json
Response 200: { "download_url": "https://storage/export_uuid.json", "expires_at": "..." }
```

#### DELETE `/settings/account`
```json
Request: { "confirm": true, "password": "str" }
Response 200: { "message": "Account and all data scheduled for deletion" }
```

---

## 16. DATA FLOW DESIGN

### Ingestion Flow
```
Android App
  │
  ├── UsageStatsCollector (every 15 min)
  │     └── Writes to Room DB (app_usage_events_local)
  │
  └── SyncWorker (WiFi, every 30 min)
        └── POST /api/v1/events/batch
              └── Backend: validate → deduplicate → insert app_usage_events
                    └── Enqueue Celery task: process_new_events(user_id, date)
```

### Nightly Compute Flow (Celery Beat, 02:00 UTC)
```
For each active user:
  1. feature_extraction(user_id, yesterday)
     → INSERT daily_features
  
  2. baseline_update(user_id)
     → EWMA update on digital_twins.dimensions
  
  3. drift_computation(user_id, yesterday)
     → INSERT drift_scores
  
  4. risk_computation(user_id, yesterday)
     → INSERT risk_records
     → IF risk_level ≥ 4 AND changed: enqueue send_push_notification
  
  5. insight_generation(user_id, yesterday)
     → Call LLM API with structured prompt
     → INSERT ai_insights
```

### Dashboard Read Flow
```
App opens → GET /dashboard/summary
  → Backend reads from:
      drift_scores (today)
      risk_records (today)
      ai_insights (today, daily)
      digital_twins (confidence, active)
  → Assemble and return
  → App caches response in Room for offline
```

---

## 17. SECURITY DESIGN

### Authentication
- JWT access tokens: 1-hour expiry, RS256 signing
- Refresh tokens: 30-day expiry, stored as bcrypt hash in DB
- Token rotation on refresh
- Revocation via DB lookup on refresh

### Data Encryption
**At Rest (Device):**
- Android Keystore-backed AES-256 key
- Room DB encrypted via SQLCipher
- DataStore preferences encrypted via Jetpack Security

**At Rest (Backend):**
- PostgreSQL column-level encryption for PII (email, device_id)
- Backups encrypted with customer-managed key (AWS KMS)

**In Transit:**
- TLS 1.3 enforced, TLS 1.2 minimum
- Certificate pinning on Android client (dev-pinned, prod-rotatable)

### Input Validation
- All API inputs validated via Pydantic schemas
- package_name: alphanum + dots only
- Timestamps: ISO 8601, reject future timestamps > 5 min
- Batch size limit: 1000 events per request

### Rate Limiting
- Auth endpoints: 10 req/min per IP
- General API: 100 req/min per user
- Batch ingest: 10 req/min per device

### Secrets Management
- All secrets in environment variables
- Production: AWS Secrets Manager
- No secrets in source code or Docker images

---

## 18. PRIVACY DESIGN

### Consent Model
- Explicit opt-in before any collection begins
- Itemized consent: each data type listed individually
- Consent timestamp stored in `users.consent_given_at`
- Consent version tracked; re-consent triggered on policy change

### Data Minimization
- Only collect what's in scope (see Data Sources section)
- package_name stored (not app content)
- session timestamps stored (not in-app activity)
- Never collect: messages, passwords, banking, keystrokes, location

### User Rights
- Export: JSON export of all user data, available within 24 hours
- Deletion: Soft delete → hard delete within 30 days
- Correction: User can reset baseline (proxy for correction)
- Portability: JSON export is machine-readable

### Data Retention
- Default: 365 days
- User-configurable: 90 / 180 / 365 days
- Automated purge job: runs daily, deletes records beyond retention window

### Anonymization
- Analytics (internal): all user-level data aggregated and anonymized before use
- No user data passed to third parties except LLM API (insights generation)
- LLM prompts contain behavioral metrics only, never PII

### App Store Requirements
- Google Play Data Safety form: complete all fields
- Prominent disclosure in app for background data collection

---

## 19. FOLDER STRUCTURE

### Android
```
driftiq-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/driftiq/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── DriftIQApplication.kt
│   │   │   ├── di/
│   │   │   │   ├── AppModule.kt
│   │   │   │   ├── DatabaseModule.kt
│   │   │   │   └── NetworkModule.kt
│   │   │   ├── presentation/
│   │   │   │   ├── home/
│   │   │   │   │   ├── HomeScreen.kt
│   │   │   │   │   └── HomeViewModel.kt
│   │   │   │   ├── drift/
│   │   │   │   ├── timeline/
│   │   │   │   ├── reports/
│   │   │   │   │   ├── weekly/
│   │   │   │   │   └── monthly/
│   │   │   │   ├── settings/
│   │   │   │   ├── onboarding/
│   │   │   │   └── common/
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   │   ├── AppUsageEvent.kt
│   │   │   │   │   ├── DailyFeatureVector.kt
│   │   │   │   │   ├── DriftScore.kt
│   │   │   │   │   ├── RiskRecord.kt
│   │   │   │   │   └── Insight.kt
│   │   │   │   ├── repository/
│   │   │   │   └── usecase/
│   │   │   ├── data/
│   │   │   │   ├── local/
│   │   │   │   │   ├── db/
│   │   │   │   │   │   ├── DriftIQDatabase.kt
│   │   │   │   │   │   └── dao/
│   │   │   │   │   └── datastore/
│   │   │   │   ├── remote/
│   │   │   │   │   ├── DriftIQApiService.kt
│   │   │   │   │   └── dto/
│   │   │   │   └── repository/
│   │   │   └── service/
│   │   │       ├── CollectionService.kt
│   │   │       ├── UsageStatsCollector.kt
│   │   │       ├── SyncWorker.kt
│   │   │       └── BootReceiver.kt
│   │   └── res/
│   │       ├── layout/
│   │       ├── drawable/
│   │       ├── values/
│   │       │   ├── colors.xml
│   │       │   ├── strings.xml
│   │       │   └── themes.xml
│   │       └── raw/
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
└── settings.gradle.kts
```

### Backend
```
driftiq-backend/
├── app/
│   ├── main.py
│   ├── api/
│   │   ├── __init__.py
│   │   ├── auth.py
│   │   ├── events.py
│   │   ├── twin.py
│   │   ├── drift.py
│   │   ├── risk.py
│   │   ├── insights.py
│   │   ├── dashboard.py
│   │   └── settings.py
│   ├── services/
│   │   ├── auth_service.py
│   │   ├── baseline_builder.py
│   │   ├── drift_detector.py
│   │   ├── risk_engine.py
│   │   ├── insight_generator.py
│   │   ├── twin_manager.py
│   │   └── feature_extractor.py
│   ├── models/
│   │   ├── user.py
│   │   ├── usage_event.py
│   │   ├── daily_feature.py
│   │   ├── digital_twin.py
│   │   ├── drift_score.py
│   │   ├── risk_record.py
│   │   └── ai_insight.py
│   ├── schemas/
│   │   ├── auth.py
│   │   ├── events.py
│   │   ├── twin.py
│   │   ├── drift.py
│   │   ├── risk.py
│   │   └── insights.py
│   ├── tasks/
│   │   ├── celery_app.py
│   │   ├── nightly_compute.py
│   │   ├── insight_task.py
│   │   └── notification_task.py
│   ├── ml/
│   │   ├── feature_extractor.py
│   │   ├── baseline_model.py
│   │   ├── drift_engine.py
│   │   └── risk_scorer.py
│   ├── core/
│   │   ├── config.py
│   │   ├── security.py
│   │   ├── database.py
│   │   └── exceptions.py
│   └── prompts/
│       ├── daily_insight.txt
│       ├── weekly_insight.txt
│       └── monthly_insight.txt
├── alembic/
│   └── versions/
├── tests/
│   ├── unit/
│   ├── integration/
│   └── fixtures/
├── docker/
│   ├── Dockerfile
│   ├── Dockerfile.worker
│   └── nginx.conf
├── docker-compose.yml
├── docker-compose.prod.yml
├── requirements.txt
├── alembic.ini
└── .env.example
```

---

## 20. TECHNOLOGY STACK

### Android
| Layer | Technology |
|---|---|
| Language | Kotlin 1.9 |
| UI Framework | Jetpack Compose |
| Architecture | Clean Architecture + MVI |
| DI | Hilt |
| Local DB | Room + SQLCipher |
| Preferences | DataStore Encrypted |
| Networking | Retrofit 2 + OkHttp 4 |
| Async | Kotlin Coroutines + Flow |
| Background | WorkManager |
| Charts | Vico (Compose-native) |
| Push | Firebase Cloud Messaging (FCM) |
| Logging | Timber |
| Crash | Firebase Crashlytics |

### Backend
| Layer | Technology |
|---|---|
| Language | Python 3.11 |
| API Framework | FastAPI 0.110 |
| ORM | SQLAlchemy 2.0 (async) |
| Migrations | Alembic |
| Task Queue | Celery 5.3 |
| Message Broker | Redis 7 |
| Database | PostgreSQL 15 |
| Cache | Redis 7 |
| Auth | python-jose (JWT) + passlib (bcrypt) |
| Validation | Pydantic v2 |
| HTTP Client | httpx (async) |
| LLM | OpenAI Python SDK |
| Push Notifications | Firebase Admin SDK |
| Testing | pytest + pytest-asyncio |
| Containerization | Docker + Docker Compose |
| Reverse Proxy | Nginx |

### Infrastructure (Production)
| Component | Service |
|---|---|
| Cloud | AWS |
| Compute | ECS Fargate |
| Database | RDS PostgreSQL (Multi-AZ) |
| Cache | ElastiCache Redis |
| Object Storage | S3 |
| CDN | CloudFront |
| Secrets | AWS Secrets Manager |
| Monitoring | CloudWatch + Grafana |
| Alerting | PagerDuty |
| CI/CD | GitHub Actions |
| Container Registry | ECR |

---

## 21. MVP DEVELOPMENT PLAN

### Phase 0: Foundation (Week 1)
- [ ] Repo setup: monorepo with `/android`, `/backend`, `/infra`
- [ ] Backend: FastAPI scaffold, PostgreSQL connection, Alembic migrations
- [ ] Android: Compose scaffold, Hilt setup, navigation graph
- [ ] CI: GitHub Actions (lint, test, build) for both
- [ ] Local Docker Compose: postgres + redis + backend + nginx

### Phase 1: Data Collection (Week 2)
- [ ] Android: UsageStatsCollector implementation
- [ ] Android: CollectionService (foreground, persistent notification)
- [ ] Android: BootReceiver
- [ ] Android: Room schema (local event buffer)
- [ ] Android: SyncWorker
- [ ] Backend: POST /events/batch with deduplication
- [ ] Test: end-to-end event flow on real device

### Phase 2: Auth + Onboarding (Week 3)
- [ ] Backend: register, login, refresh, logout endpoints
- [ ] Android: S-02 Welcome, S-03 Consent, S-04/S-05 Permissions
- [ ] Android: S-06 Account Creation, S-07 Sign In
- [ ] Android: JWT storage in encrypted DataStore
- [ ] Android: Auth interceptor in OkHttp

### Phase 3: Feature Extraction + Digital Twin (Week 4)
- [ ] Backend: feature_extractor.py (all 15 features)
- [ ] Backend: daily_features table + computation task
- [ ] Backend: baseline_model.py (EWMA per dimension)
- [ ] Backend: digital_twins schema + update logic
- [ ] Backend: GET /twin/status endpoint
- [ ] Android: S-08 Calibration screen
- [ ] Celery Beat: nightly job wire-up

### Phase 4: Drift + Risk Engine (Week 5)
- [ ] Backend: drift_engine.py (z-score, composite, velocity)
- [ ] Backend: drift_scores computation + storage
- [ ] Backend: risk_engine.py (formula + explainability)
- [ ] Backend: risk_records storage
- [ ] Backend: GET /drift/today, /drift/history, /risk/current
- [ ] Android: S-10 Drift Detail Screen
- [ ] Android: S-18 Risk Trend Screen

### Phase 5: Dashboard + Visualization (Week 6)
- [ ] Backend: GET /dashboard/summary
- [ ] Android: S-09 Home Dashboard (full implementation)
- [ ] Android: S-11 Weekly Dashboard
- [ ] Android: S-12 Monthly Dashboard
- [ ] Android: S-13 Behavioral Timeline
- [ ] Android: S-14/S-15/S-16/S-17 Dimension Screens
- [ ] Charts: Vico integration for all chart types

### Phase 6: AI Insights (Week 7)
- [ ] Backend: LLM prompt templates (daily/weekly/monthly)
- [ ] Backend: insight_generator.py + Celery task
- [ ] Backend: GET /insights/daily, /insights/weekly, /insights/monthly
- [ ] Android: S-20 Insight Detail Screen
- [ ] Fallback: rule-based insight generation

### Phase 7: Notifications + Settings (Week 8)
- [ ] FCM setup: Android + backend
- [ ] Backend: notification_task.py (risk alerts, weekly reports)
- [ ] Android: S-19 Settings screen (all sections)
- [ ] Backend: data export (JSON generation + S3 signed URL)
- [ ] Backend: account deletion flow

### Phase 8: Polish + Testing (Week 9)
- [ ] Full E2E test pass
- [ ] Battery impact measurement on real device
- [ ] Performance profiling (API response times)
- [ ] Crash-free session target: >99.5%
- [ ] Security review (auth, input validation)
- [ ] Privacy review (consent flow, data minimization)

### Phase 9: Deployment (Week 10)
- [ ] Production infrastructure provisioning (Terraform)
- [ ] CI/CD deployment pipeline
- [ ] Monitoring + alerting setup
- [ ] Beta test (internal, 10 users)
- [ ] Google Play internal test track

---

## 22. TESTING STRATEGY

### Unit Tests
**Backend:**
- `test_feature_extractor.py`: known event sequences → expected feature values
- `test_baseline_model.py`: EWMA convergence, confidence scoring
- `test_drift_engine.py`: z-score computation, composite formula
- `test_risk_engine.py`: all level thresholds, boundary conditions
- `test_auth.py`: token generation, refresh, revocation

**Android:**
- ViewModel unit tests (MVI state transitions)
- UsageStatsCollector parsing tests (mocked UsageStatsManager)
- Repository unit tests (mocked DAO / API)

### Integration Tests
**Backend:**
- `test_event_ingestion.py`: batch POST → DB records
- `test_nightly_pipeline.py`: seed events → run full pipeline → assert drift + risk records
- `test_insight_generation.py`: mock LLM → assert insight stored

### End-to-End Tests
- Instrumented Android tests (Espresso): onboarding flow, dashboard render
- API contract tests: Postman/Newman collection for all endpoints

### Performance Tests
- Locust: simulate 100 concurrent users posting events
- Target: p95 < 500ms for all GET endpoints
- Target: batch ingest of 1000 events < 2s

### Device Tests
- Target: 5+ real Android devices across API 29–34
- Battery impact test: 24-hour collection with/without DriftIQ
- Background kill resistance test

---

## 23. DEPLOYMENT ARCHITECTURE

### Production AWS Architecture
```
Internet → Route 53 → CloudFront → ALB
                                    ├── ECS Fargate (FastAPI API, 2–4 tasks, auto-scale)
                                    └── ECS Fargate (Celery Worker, 2 tasks, auto-scale)
                                    
ALB → API → RDS PostgreSQL (Multi-AZ, db.t4g.medium)
         → ElastiCache Redis (cache.t4g.medium, 2 nodes)
         → S3 (exports, static assets)
         
Celery Beat → separate ECS Fargate task (1 task, no auto-scale)
         
Monitoring:
  → CloudWatch Logs (all containers)
  → CloudWatch Metrics → Grafana dashboards
  → CloudWatch Alarms → PagerDuty (p95 latency, error rate, worker lag)
```

### Docker Compose (Local Dev)
```yaml
services:
  api:
    build: .
    ports: ["8000:8000"]
    depends_on: [postgres, redis]
  
  worker:
    build: ./docker/Dockerfile.worker
    depends_on: [postgres, redis]
  
  beat:
    build: ./docker/Dockerfile.worker
    command: celery -A app.tasks.celery_app beat
    depends_on: [redis]
  
  postgres:
    image: postgres:15
    volumes: [postgres_data:/var/lib/postgresql/data]
  
  redis:
    image: redis:7-alpine
```

### CI/CD Pipeline (GitHub Actions)
```
Push to main:
  1. lint (flake8, ktlint)
  2. test (pytest, Android unit tests)
  3. build Docker image → push to ECR
  4. deploy to ECS (rolling update, min 50% healthy)
  5. run smoke tests
  6. notify Slack
```

---

## 24. PRODUCTION ROLLOUT PLAN

### Stage 1: Internal Alpha (Week 10, ~10 users)
- Developers + close team
- Goal: pipeline validation, crash discovery
- Monitor: error rates, worker lag, battery drain reports

### Stage 2: Closed Beta (Week 11–12, ~50 users)
- Google Play internal test track
- Explicit beta consent + feedback channel
- Goal: 7-day baseline completion rate > 80%, dashboard satisfaction
- Monitor: API p95, daily active events per user

### Stage 3: Open Beta (Week 13, ~500 users)
- Google Play open testing
- Scale infrastructure to handle load
- Goal: identify scaling issues, LLM cost per user
- Monitor: Celery queue depth, RDS CPU, LLM API costs

### Stage 4: Production Launch (Week 14+)
- Google Play production track
- App Store submission (post-MVP)
- Monitor: all metrics + weekly cohort retention

### Rollback Criteria
- Error rate > 5% → rollback ECS to previous task definition
- p95 API latency > 2s for > 5 min → scale up + alert
- Celery queue depth > 10,000 jobs → scale workers + alert

---

## APPENDIX A: LLM PROMPT TEMPLATES

### Daily Insight Prompt
```
You are DriftIQ's behavioral insight engine. You generate non-medical behavioral observations.

User's behavioral data for {date}:
- Composite drift score: {composite_drift}/100
- Sleep estimate: {sleep_hours}h (baseline: {baseline_sleep}h)
- Screen time: {screen_time}min (baseline: {baseline_screen}min)
- Social app usage: {social_min}min (baseline: {baseline_social}min)
- Productivity usage: {prod_min}min (baseline: {baseline_prod}min)
- Risk level: {risk_level} ({risk_label})
- Top drift contributors: {contributors}

Write a 3-sentence behavioral observation. 
Rules:
- Use "your usual pattern" not clinical language
- No diagnosis, no medical advice
- Be specific about which behaviors changed
- Neutral, objective tone
- Focus on what changed, not what it means medically
```

### Weekly Insight Prompt
```
You are DriftIQ's behavioral insight engine.

7-day behavioral summary for week ending {end_date}:
{weekly_stats_json}

Write a 5–7 sentence weekly behavioral insight covering:
1. Overall pattern stability
2. Most significant behavioral shifts
3. Sleep and activity trends
4. Productive vs. consumption behavior balance

Rules: Non-medical. Behavioral observations only. Objective tone.
```

---

## APPENDIX B: APP CATEGORY MAPPINGS (Sample)

```json
{
  "com.instagram.android": "SOCIAL",
  "com.twitter.android": "SOCIAL",
  "com.facebook.katana": "SOCIAL",
  "com.linkedin.android": "PRODUCTIVITY",
  "com.slack": "PRODUCTIVITY",
  "com.google.android.apps.docs": "PRODUCTIVITY",
  "com.netflix.mediaclient": "ENTERTAINMENT",
  "com.spotify.music": "ENTERTAINMENT",
  "org.duolingo": "LEARNING",
  "com.google.android.youtube": "ENTERTAINMENT",
  "com.whatsapp": "SOCIAL",
  "com.google.android.gm": "PRODUCTIVITY"
}
```
Unmapped packages → query Google Play API or default to "UTILITY".

---

## APPENDIX C: RISK LEVEL COLOR TOKENS

```kotlin
// Android
val RiskColors = mapOf(
  0 to Color(0xFF2ECC71),  // Healthy - green
  1 to Color(0xFFF1C40F),  // Observation - yellow
  2 to Color(0xFFE67E22),  // Mild Concern - orange
  3 to Color(0xFFE74C3C),  // Moderate Concern - red
  4 to Color(0xFFC0392B),  // High Concern - dark red
  5 to Color(0xFF7B241C),  // Critical - deep red
)
```

---

*End of DriftIQ MVP Build Specification v1.0*
*Total sections: 24 + 3 Appendices*
*Target system: Antigravity Autonomous Coding System*
