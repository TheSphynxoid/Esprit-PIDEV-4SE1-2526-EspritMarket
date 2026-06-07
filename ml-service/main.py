from fastapi import FastAPI
from pydantic import BaseModel
from typing import Optional
import joblib
import numpy as np
import os
import json
import threading
from datetime import datetime

import sqlalchemy
from sqlalchemy import text

app = FastAPI(title="EspritMarket ML Service", version="2.1.0")

BOOKING_MODEL_PATH = os.getenv("BOOKING_MODEL_PATH", "models/booking_completion_model.joblib")
PROJECT_MODEL_PATH = os.getenv("PROJECT_MODEL_PATH", "models/project_delay_model.joblib")
SENTIMENT_MODEL_PATH = os.getenv("SENTIMENT_MODEL_PATH", "models/sentiment_model.joblib")
DB_PASSWORD = os.getenv("ML_DB_PASSWORD", "postgres")
DB_URL = os.getenv("ML_DB_URL", f"postgresql://postgres:{DB_PASSWORD}@postgres:5432/esprit_market")
RETRAIN_THRESHOLD = int(os.getenv("RETRAIN_THRESHOLD", "5"))

booking_artifacts = None
project_artifacts = None
sentiment_artifacts = None

_new_records_since_retrain = 0
_retrain_lock = threading.Lock()
_retraining = False


class BookingFeatures(BaseModel):
    serviceCategory: str
    providerRating: float
    duration: float
    totalPrice: float
    dayOfWeek: int
    hour: int
    providerCompletedCount: int = 0
    providerCancelledCount: int = 0


class ProjectFeatures(BaseModel):
    teamSize: int
    milestoneCount: int
    budget: float
    avgMilestoneDurationDays: float
    blockedMilestoneCount: int = 0
    completedMilestonePct: float
    totalPlannedDays: int
    dependencyDensity: float = 0.0
    currentSlippageDays: float = 0.0


class BookingPredictionResponse(BaseModel):
    completionProbability: float
    riskLevel: str
    confidence: str
    keyFactors: list[str]
    recommendation: str


class ProjectPredictionResponse(BaseModel):
    onTimeProbability: float
    delayRiskLevel: str
    estimatedDelayDays: Optional[float] = None
    keyFactors: list[str]
    recommendation: str


class SentimentItem(BaseModel):
    text: str
    sentiment: str
    confidence: float
    positiveProbability: float
    negativeProbability: float
    neutralProbability: float


class SentimentRequest(BaseModel):
    texts: list[str]


class SentimentResponse(BaseModel):
    results: list[SentimentItem]


class HealthResponse(BaseModel):
    status: str
    bookingModelLoaded: bool
    projectModelLoaded: bool


class RecordOutcomeRequest(BaseModel):
    modelType: str
    features: dict
    label: int


class RecordOutcomeResponse(BaseModel):
    success: bool
    totalRecords: int


class RetrainResponse(BaseModel):
    success: bool
    bookingRecords: int = 0
    projectRecords: int = 0
    bookingAccuracy: Optional[float] = None
    projectAccuracy: Optional[float] = None


class DatasetStatsResponse(BaseModel):
    bookingRecords: int = 0
    projectRecords: int = 0
    bookingSources: dict = {}
    projectSources: dict = {}


def get_engine():
    return sqlalchemy.create_engine(DB_URL)


def load_booking_model():
    global booking_artifacts
    if os.path.exists(BOOKING_MODEL_PATH):
        booking_artifacts = joblib.load(BOOKING_MODEL_PATH)
        print(f"  Loaded booking model from {BOOKING_MODEL_PATH}")


def load_project_model():
    global project_artifacts
    if os.path.exists(PROJECT_MODEL_PATH):
        project_artifacts = joblib.load(PROJECT_MODEL_PATH)
        print(f"  Loaded project model from {PROJECT_MODEL_PATH}")


def load_sentiment_model():
    global sentiment_artifacts
    if os.path.exists(SENTIMENT_MODEL_PATH):
        sentiment_artifacts = joblib.load(SENTIMENT_MODEL_PATH)
        print(f"  Loaded sentiment model from {SENTIMENT_MODEL_PATH}")


@app.on_event("startup")
def startup():
    load_booking_model()
    load_project_model()
    load_sentiment_model()
    print("ML Service started")


@app.get("/health", response_model=HealthResponse)
def health():
    return HealthResponse(
        status="ok",
        bookingModelLoaded=booking_artifacts is not None,
        projectModelLoaded=project_artifacts is not None,
    )


def _apply_provider_smoothing(features: BookingFeatures) -> BookingFeatures:
    PRIOR_RATING = 3.0
    PRIOR_COMPLETED = 3
    PRIOR_CANCELLED = 1
    MIN_REVIEWS_FOR_DIRECT = 3

    total_bookings = features.providerCompletedCount + features.providerCancelledCount
    if total_bookings < MIN_REVIEWS_FOR_DIRECT or features.providerRating == 0.0:
        alpha = max(total_bookings, 1)
        beta = MIN_REVIEWS_FOR_DIRECT
        weight = alpha / (alpha + beta)

        smoothed = BookingFeatures(
            serviceCategory=features.serviceCategory,
            providerRating=round(weight * features.providerRating + (1 - weight) * PRIOR_RATING, 2),
            duration=features.duration,
            totalPrice=features.totalPrice,
            dayOfWeek=features.dayOfWeek,
            hour=features.hour,
            providerCompletedCount=features.providerCompletedCount + PRIOR_COMPLETED,
            providerCancelledCount=features.providerCancelledCount + PRIOR_CANCELLED,
        )
        return smoothed
    return features


@app.post("/predict/booking", response_model=BookingPredictionResponse)
def predict_booking(features: BookingFeatures):
    if booking_artifacts is None:
        load_booking_model()
    if booking_artifacts is None:
        raise ValueError("Booking model not available")

    smoothed = _apply_provider_smoothing(features)

    model = booking_artifacts["model"]
    le = booking_artifacts["label_encoder"]
    cat_cols = booking_artifacts["categorical_cols"]
    num_cols = booking_artifacts["numerical_cols"]

    row = {
        "service_category": smoothed.serviceCategory,
        "provider_rating": smoothed.providerRating,
        "duration": smoothed.duration,
        "total_price": smoothed.totalPrice,
        "day_of_week": smoothed.dayOfWeek,
        "hour": smoothed.hour,
        "provider_completed_count": smoothed.providerCompletedCount,
        "provider_cancelled_count": smoothed.providerCancelledCount,
    }
    total = max(smoothed.providerCompletedCount + smoothed.providerCancelledCount, 1)
    row["provider_completion_ratio"] = smoothed.providerCompletedCount / total
    row["is_weekend"] = 1 if smoothed.dayOfWeek >= 5 else 0
    row["is_long_duration"] = 1 if smoothed.duration > 4 else 0

    df_cat = le.transform([row[c] for c in cat_cols])
    df_num = np.array([[row[c] for c in num_cols]], dtype=float)
    x = np.hstack([df_num, df_cat.reshape(-1, 1)])

    proba = model.predict_proba(x)[0]
    prob_completed = float(proba[1])

    importances = model.feature_importances_
    top_features = sorted(zip(num_cols + cat_cols, importances), key=lambda t: -t[1])[:3]

    key_factors = []
    has_limited_data = (features.providerCompletedCount + features.providerCancelledCount) < 3 or features.providerRating == 0.0
    if has_limited_data:
        key_factors.append("Limited provider history — using estimated baseline")
    if smoothed.providerRating < 3.5:
        key_factors.append("Provider rating below average increases risk")
    if features.totalPrice > 200:
        key_factors.append("High booking value detected")
    if features.dayOfWeek >= 5:
        key_factors.append("Weekend booking has higher cancellation rate")
    if features.duration > 4:
        key_factors.append("Long-duration bookings face more disruption")
    if not key_factors:
        key_factors.append("Booking parameters look favorable")

    for fname, _ in top_features:
        if "rating" in fname:
            if has_limited_data:
                key_factors.append(f"Provider rating is a key factor (no reviews yet, est. {smoothed.providerRating}/5)")
            else:
                key_factors.append(f"Provider rating is the top predictive factor ({features.providerRating}/5)")
            break
        if "ratio" in fname:
            key_factors.append(f"Provider completion history is the top predictive factor ({prob_completed:.0%})")
            break

    if prob_completed >= 0.8:
        risk_level = "LOW"
        confidence = "HIGH"
        recommendation = "Booking is likely to complete successfully. Proceed with confidence."
    elif prob_completed >= 0.6:
        risk_level = "MEDIUM"
        confidence = "MODERATE"
        recommendation = "Some risk factors detected. Consider confirming with the provider."
    elif prob_completed >= 0.4:
        risk_level = "HIGH"
        confidence = "LOW"
        recommendation = "Significant cancellation risk. Consider requiring pre-payment or shorter initial booking."
    else:
        risk_level = "CRITICAL"
        confidence = "VERY LOW"
        recommendation = "Very high cancellation risk. Recommend rejecting or requesting alternative provider/timing."

    return BookingPredictionResponse(
        completionProbability=round(prob_completed, 4),
        riskLevel=risk_level,
        confidence=confidence,
        keyFactors=key_factors[:4],
        recommendation=recommendation,
    )


@app.post("/predict/project", response_model=ProjectPredictionResponse)
def predict_project(features: ProjectFeatures):
    if project_artifacts is None:
        load_project_model()
    if project_artifacts is None:
        raise ValueError("Project model not available")

    model = project_artifacts["model"]
    feat_cols = project_artifacts["feature_cols"]

    row = {
        "team_size": features.teamSize,
        "milestone_count": features.milestoneCount,
        "budget": features.budget,
        "avg_milestone_duration_days": features.avgMilestoneDurationDays,
        "blocked_milestone_count": features.blockedMilestoneCount,
        "completed_milestone_pct": features.completedMilestonePct,
        "total_planned_days": features.totalPlannedDays,
        "dependency_density": features.dependencyDensity,
        "current_slippage_days": features.currentSlippageDays,
    }

    x = np.array([[row[c] for c in feat_cols]], dtype=float)
    proba = model.predict_proba(x)[0]
    prob_on_time = float(proba[1])

    importances = model.feature_importances_
    top_features = sorted(zip(feat_cols, importances), key=lambda t: -t[1])[:3]

    key_factors = []
    if features.blockedMilestoneCount > 0:
        key_factors.append(f"{features.blockedMilestoneCount} blocked milestone(s) detected")
    if features.currentSlippageDays > 3:
        key_factors.append(f"Current slippage of {features.currentSlippageDays} days is concerning")
    if features.milestoneCount > 8:
        key_factors.append(f"High milestone count ({features.milestoneCount}) increases complexity")
    if features.teamSize < 3:
        key_factors.append("Small team size may limit parallel execution")
    if features.dependencyDensity > 0.5:
        key_factors.append("High dependency density creates bottleneck risk")
    if features.avgMilestoneDurationDays > 14:
        key_factors.append("Long average milestone duration")
    if not key_factors:
        key_factors.append("Project parameters look healthy")

    for fname, imp in top_features:
        if "slippage" in fname:
            key_factors.append("Current slippage is the strongest delay predictor")
            break
        if "blocked" in fname:
            key_factors.append("Blocked milestones are the strongest delay predictor")
            break
        if "dependency" in fname:
            key_factors.append("Dependency density is the strongest delay predictor")
            break

    estimated_delay = None
    if prob_on_time < 0.7 and features.totalPlannedDays > 0:
        base_delay = features.currentSlippageDays
        if features.avgMilestoneDurationDays > 14:
            base_delay += 5
        if features.dependencyDensity > 0.5:
            base_delay += 3
        if features.blockedMilestoneCount > 0:
            base_delay += 2 * features.blockedMilestoneCount
        remaining_pct = 1.0 - features.completedMilestonePct
        estimated_delay = round(base_delay + (remaining_pct * features.avgMilestoneDurationDays * 0.3), 1)
        estimated_delay = max(estimated_delay, features.currentSlippageDays + 1)

    if prob_on_time >= 0.8:
        risk_level = "LOW"
        recommendation = "Project is on track. Continue current execution pace."
    elif prob_on_time >= 0.6:
        risk_level = "MEDIUM"
        recommendation = "Moderate delay risk. Monitor blocked milestones and slippage closely."
    elif prob_on_time >= 0.4:
        risk_level = "HIGH"
        recommendation = "Significant delay risk. Consider re-prioritizing milestones or adding resources."
    else:
        risk_level = "CRITICAL"
        recommendation = "Project is likely to miss its deadline. Immediate intervention required: reduce scope, add team members, or negotiate deadline extension."

    return ProjectPredictionResponse(
        onTimeProbability=round(prob_on_time, 4),
        delayRiskLevel=risk_level,
        estimatedDelayDays=estimated_delay,
        keyFactors=key_factors[:5],
        recommendation=recommendation,
    )


LABEL_MAP = {0: "NEGATIVE", 1: "NEUTRAL", 2: "POSITIVE"}


@app.post("/predict/sentiment", response_model=SentimentResponse)
def predict_sentiment(req: SentimentRequest):
    global sentiment_artifacts
    if sentiment_artifacts is None:
        load_sentiment_model()
        if sentiment_artifacts is None:
            from train import load_sentiment_dataset, train_sentiment_model
            df = load_sentiment_dataset()
            sentiment_artifacts = train_sentiment_model(df)

    model = sentiment_artifacts["model"]
    vectorizer = sentiment_artifacts["vectorizer"]

    results = []
    for text in req.texts:
        x = vectorizer.transform([text])
        proba = model.predict_proba(x)[0]
        pred = int(model.predict(x)[0])
        sentiment = LABEL_MAP.get(pred, "NEUTRAL")
        confidence = float(proba[pred])
        results.append(SentimentItem(
            text=text[:200],
            sentiment=sentiment,
            confidence=round(confidence, 4),
            positiveProbability=round(float(proba[2]), 4) if len(proba) > 2 else 0.0,
            negativeProbability=round(float(proba[0]), 4),
            neutralProbability=round(float(proba[1]), 4) if len(proba) > 2 else 0.0,
        ))

    return SentimentResponse(results=results)


@app.post("/data/record", response_model=RecordOutcomeResponse)
def record_outcome(req: RecordOutcomeRequest):
    global _new_records_since_retrain, _retraining

    if req.modelType not in ("BOOKING", "PROJECT", "SENTIMENT"):
        raise ValueError("modelType must be BOOKING, PROJECT, or SENTIMENT")

    engine = get_engine()
    try:
        with engine.connect() as conn:
            conn.execute(
                sqlalchemy.text(
                    "INSERT INTO ml_training_data (model_type, features, label, source, created_at) "
                    "VALUES (:mt, CAST(:feat AS jsonb), :lbl, 'APPLICATION', NOW())"
                ),
                {"mt": req.modelType, "feat": json.dumps(req.features), "lbl": req.label}
            )
            conn.commit()
    except Exception as e:
        engine.dispose()
        raise e

    with engine.connect() as conn:
        count = conn.execute(
            sqlalchemy.text("SELECT COUNT(*) FROM ml_training_data WHERE model_type = :mt"),
            {"mt": req.modelType}
        ).scalar()

    engine.dispose()

    _new_records_since_retrain += 1
    if _new_records_since_retrain >= RETRAIN_THRESHOLD and not _retraining:
        threading.Thread(target=_background_retrain, daemon=True).start()

    return RecordOutcomeResponse(success=True, totalRecords=int(count))


@app.post("/retrain", response_model=RetrainResponse)
def retrain():
    global _new_records_since_retrain
    resp = _do_retrain()
    if resp.success:
        _new_records_since_retrain = 0
    return resp


def _do_retrain():
    global booking_artifacts, project_artifacts, sentiment_artifacts, _retraining
    from train import (
        load_booking_dataset, load_project_dataset, load_sentiment_dataset,
        train_booking_model, train_project_model, train_sentiment_model,
        prepare_booking_features, prepare_project_features
    )
    from sklearn.metrics import accuracy_score

    _retraining = True
    resp = RetrainResponse(success=True)
    print(f"[{datetime.utcnow()}] Retraining ML models...")

    try:
        booking_df = load_booking_dataset()
        resp.bookingRecords = len(booking_df)
        if len(booking_df) > 10:
            artifacts = train_booking_model(booking_df)
            booking_artifacts = artifacts

            X, y, *_ = prepare_booking_features(booking_df)
            from sklearn.model_selection import train_test_split
            X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
            resp.bookingAccuracy = round(accuracy_score(y_test, artifacts["model"].predict(X_test)), 4)
            print(f"[{datetime.utcnow()}] Booking model retrained: {resp.bookingRecords} records, accuracy={resp.bookingAccuracy}")
        else:
            resp.bookingAccuracy = None
            print(f"[{datetime.utcnow()}] Skipping booking retrain: only {len(booking_df)} records (need >10)")
    except Exception as e:
        resp.success = False
        resp.bookingAccuracy = None
        print(f"[{datetime.utcnow()}] Booking retrain failed: {e}")

    try:
        project_df = load_project_dataset()
        resp.projectRecords = len(project_df)
        if len(project_df) > 10:
            artifacts = train_project_model(project_df)
            project_artifacts = artifacts

            X, y, *_ = prepare_project_features(project_df)
            from sklearn.model_selection import train_test_split
            X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)
            resp.projectAccuracy = round(accuracy_score(y_test, artifacts["model"].predict(X_test)), 4)
            print(f"[{datetime.utcnow()}] Project model retrained: {resp.projectRecords} records, accuracy={resp.projectAccuracy}")
        else:
            resp.projectAccuracy = None
            print(f"[{datetime.utcnow()}] Skipping project retrain: only {len(project_df)} records (need >10)")
    except Exception as e:
        resp.success = False
        resp.projectAccuracy = None
        print(f"[{datetime.utcnow()}] Project retrain failed: {e}")

    try:
        sentiment_df = load_sentiment_dataset()
        if len(sentiment_df) > 30:
            artifacts = train_sentiment_model(sentiment_df)
            sentiment_artifacts = artifacts
            print(f"[{datetime.utcnow()}] Sentiment model retrained: {len(sentiment_df)} records")
        else:
            print(f"[{datetime.utcnow()}] Skipping sentiment retrain: only {len(sentiment_df)} records")
    except Exception as e:
        resp.success = False
        print(f"[{datetime.utcnow()}] Sentiment retrain failed: {e}")

    _retraining = False
    return resp


def _background_retrain():
    try:
        resp = _do_retrain()
        if resp.success:
            global _new_records_since_retrain
            _new_records_since_retrain = 0
    except Exception as e:
        print(f"[{datetime.utcnow()}] Background retrain error: {e}")


@app.get("/data/stats", response_model=DatasetStatsResponse)
def dataset_stats():
    engine = get_engine()
    booking_sources = {}
    project_sources = {}
    booking_count = 0
    project_count = 0

    try:
        with engine.connect() as conn:
            rows = conn.execute(
                text("SELECT model_type, source, COUNT(*) as cnt FROM ml_training_data GROUP BY model_type, source")
            )
            for r in rows:
                mt, src, cnt = r
                if mt == "BOOKING":
                    booking_count += int(cnt)
                    booking_sources[src] = int(cnt)
                elif mt == "PROJECT":
                    project_count += int(cnt)
                    project_sources[src] = int(cnt)
    except Exception:
        pass

    engine.dispose()
    return DatasetStatsResponse(
        bookingRecords=booking_count,
        projectRecords=project_count,
        bookingSources=booking_sources,
        projectSources=project_sources,
    )


class RetrainStatusResponse(BaseModel):
    pendingRecords: int
    threshold: int
    retraining: bool
    bookingModelLoaded: bool
    projectModelLoaded: bool


@app.get("/retrain/status", response_model=RetrainStatusResponse)
def retrain_status():
    global _new_records_since_retrain, _retraining
    return RetrainStatusResponse(
        pendingRecords=_new_records_since_retrain,
        threshold=RETRAIN_THRESHOLD,
        retraining=_retraining,
        bookingModelLoaded=booking_artifacts is not None,
        projectModelLoaded=project_artifacts is not None,
    )
