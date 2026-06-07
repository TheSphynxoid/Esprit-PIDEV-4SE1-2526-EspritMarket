import os
import json
import joblib
import numpy as np
import pandas as pd
from datetime import datetime
from lightgbm import LGBMClassifier
from sklearn.model_selection import train_test_split
from sklearn.metrics import classification_report, accuracy_score
from sklearn.preprocessing import LabelEncoder
from sklearn.feature_extraction.text import TfidfVectorizer

import sqlalchemy
from sqlalchemy import text

DATA_DIR = os.getenv("ML_DATA_DIR", "data")
DB_PASSWORD = os.getenv("ML_DB_PASSWORD", "postgres")
DB_URL = os.getenv("ML_DB_URL", f"postgresql://postgres:{DB_PASSWORD}@postgres:5432/esprit_market")
os.makedirs(DATA_DIR, exist_ok=True)


def get_engine():
    return sqlalchemy.create_engine(DB_URL)


def load_db_records(model_type: str) -> list[dict]:
    try:
        engine = get_engine()
        with engine.connect() as conn:
            result = conn.execute(
                text("SELECT features, label FROM ml_training_data WHERE model_type = :mt ORDER BY created_at"),
                {"mt": model_type}
            )
            rows = []
            for row in result:
                feat = json.loads(row[0]) if isinstance(row[0], str) else dict(row[0])
                feat["_label"] = row[1]
                rows.append(feat)
        engine.dispose()
        return rows
    except Exception as e:
        print(f"  DB load failed ({e}), using local data only")
        return []


def save_to_csv(model_type: str, df: pd.DataFrame):
    path = os.path.join(DATA_DIR, f"{model_type}_dataset.csv")
    df.to_csv(path, index=False)
    print(f"  Saved {len(df)} records to {path}")


def load_from_csv(model_type: str) -> pd.DataFrame | None:
    path = os.path.join(DATA_DIR, f"{model_type}_dataset.csv")
    if os.path.exists(path):
        df = pd.read_csv(path)
        print(f"  Loaded {len(df)} records from {path}")
        return df
    return None


def load_booking_dataset():
    print("\n=== Loading booking dataset ===")
    csv_df = load_from_csv("booking")

    db_rows = load_db_records("BOOKING")
    if db_rows:
        db_df = pd.DataFrame(db_rows)
        if "_label" in db_df.columns:
            db_df = db_df.rename(columns={"_label": "label"})
        if csv_df is not None:
            combined = pd.concat([csv_df, db_df], ignore_index=True).drop_duplicates()
            print(f"  Combined: {len(csv_df)} CSV + {len(db_df)} DB = {len(combined)} total")
        else:
            combined = db_df
            print(f"  Using {len(combined)} DB records")
    elif csv_df is not None:
        combined = csv_df
    else:
        print("  No existing data found, generating synthetic baseline")
        combined = generate_synthetic_booking_data(800)

    if len(combined) < 50:
        print(f"  Dataset too small ({len(combined)} rows), generating synthetic baseline to augment")
        synthetic = generate_synthetic_booking_data(800)
        combined = pd.concat([combined, synthetic], ignore_index=True).drop_duplicates()

    save_to_csv("booking", combined)
    return combined


def load_project_dataset():
    print("\n=== Loading project dataset ===")
    csv_df = load_from_csv("project")

    db_rows = load_db_records("PROJECT")
    if db_rows:
        db_df = pd.DataFrame(db_rows)
        if "_label" in db_df.columns:
            db_df = db_df.rename(columns={"_label": "label"})
        if csv_df is not None:
            combined = pd.concat([csv_df, db_df], ignore_index=True).drop_duplicates()
            print(f"  Combined: {len(csv_df)} CSV + {len(db_rows)} DB = {len(combined)} total")
        else:
            combined = db_df
            print(f"  Using {len(combined)} DB records")
    elif csv_df is not None:
        combined = csv_df
    else:
        print("  No existing data found, generating synthetic baseline")
        combined = generate_synthetic_project_data(500)

    save_to_csv("project", combined)
    return combined


def generate_synthetic_booking_data(n=800):
    rng = np.random.default_rng(42)
    categories = ["SRV", "CONSULTING", "DESIGN", "DEVELOPMENT", "MARKETING",
                   "PHOTOGRAPHY", "WRITING", "TUTORING", "MAINTENANCE", "CLEANING", "OTHER"]

    records = []
    for _ in range(n):
        cat = rng.choice(categories)
        provider_rating = round(rng.uniform(1.0, 5.0), 2)
        duration = round(rng.uniform(0.5, 8.0), 1)
        price = round(duration * rng.uniform(20, 200), 2)
        day_of_week = rng.integers(0, 7)
        hour = rng.integers(8, 21)
        provider_completed = int(rng.integers(0, 50))
        provider_cancelled = int(rng.integers(0, 20))

        completed_ratio = provider_completed / max(provider_completed + provider_cancelled, 1)
        price_factor = 0.0 if price < 50 else 0.05 if price < 150 else 0.1
        rating_factor = 0.0 if provider_rating < 3.0 else 0.1 if provider_rating < 4.0 else 0.15
        completion_factor = completed_ratio * 0.3
        weekend_factor = 0.05 if day_of_week >= 5 else 0.0
        long_duration_factor = 0.08 if duration > 4 else 0.0

        success_prob = 0.5 + price_factor + rating_factor + completion_factor - weekend_factor - long_duration_factor
        success_prob = np.clip(success_prob, 0.1, 0.95)

        label = 1 if rng.random() < success_prob else 0
        records.append({
            "service_category": cat,
            "provider_rating": provider_rating,
            "duration": duration,
            "total_price": price,
            "day_of_week": int(day_of_week),
            "hour": int(hour),
            "provider_completed_count": provider_completed,
            "provider_cancelled_count": provider_cancelled,
            "label": int(label),
            "source": "synthetic"
        })

    return pd.DataFrame(records)


def generate_synthetic_project_data(n=500):
    rng = np.random.default_rng(42)

    records = []
    for _ in range(n):
        team_size = int(rng.integers(1, 8))
        milestone_count = int(rng.integers(2, 12))
        budget = round(rng.uniform(500, 50000), 2)
        avg_milestone_duration = round(rng.uniform(1, 30), 1)
        blocked_count = int(rng.integers(0, 3))
        completed_pct = round(rng.uniform(0.0, 1.0), 2)
        total_days = int(round(rng.uniform(7, 180)))
        dependency_density = round(rng.uniform(0.0, 1.0), 2)
        current_slippage = round(rng.uniform(0.0, 15.0), 1)

        success_prob = 0.6
        success_prob += 0.05 * min(team_size, 4)
        success_prob -= 0.1 * blocked_count
        success_prob -= 0.15 * (avg_milestone_duration / 30)
        success_prob -= 0.1 * dependency_density
        success_prob -= 0.2 * (current_slippage / 15)
        success_prob = np.clip(success_prob, 0.1, 0.95)

        label = 1 if rng.random() < success_prob else 0
        records.append({
            "team_size": team_size,
            "milestone_count": milestone_count,
            "budget": budget,
            "avg_milestone_duration_days": avg_milestone_duration,
            "blocked_milestone_count": blocked_count,
            "completed_milestone_pct": completed_pct,
            "total_planned_days": total_days,
            "dependency_density": dependency_density,
            "current_slippage_days": current_slippage,
            "label": int(label),
            "source": "synthetic"
        })

    return pd.DataFrame(records)


def prepare_booking_features(df: pd.DataFrame):
    cat_cols = ["service_category"]
    num_cols = ["provider_rating", "duration", "total_price", "day_of_week", "hour",
                "provider_completed_count", "provider_cancelled_count", "provider_completion_ratio",
                "is_weekend", "is_long_duration"]

    df = df.copy()
    df["provider_completion_ratio"] = (
        df["provider_completed_count"] /
        df[["provider_completed_count", "provider_cancelled_count"]].sum(axis=1).clip(lower=1)
    )
    df["is_weekend"] = (df["day_of_week"] >= 5).astype(int)
    df["is_long_duration"] = (df["duration"] > 4).astype(int)

    le = LabelEncoder()
    for c in cat_cols:
        df[c] = le.fit_transform(df[c].astype(str))

    X = df[num_cols + cat_cols]
    y = df["label"].astype(int)
    return X, y, le, cat_cols, num_cols


def train_booking_model(df: pd.DataFrame):
    print("\n=== Training Booking Completion Model (LightGBM) ===")
    X, y, le, cat_cols, num_cols = prepare_booking_features(df)
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    model = LGBMClassifier(
        n_estimators=200,
        max_depth=6,
        learning_rate=0.08,
        num_leaves=31,
        min_child_samples=10,
        subsample=0.8,
        colsample_bytree=0.8,
        reg_alpha=0.1,
        reg_lambda=0.1,
        random_state=42,
        verbose=-1
    )
    model.fit(X_train, y_train, eval_set=[(X_test, y_test)], callbacks=[])

    y_pred = model.predict(X_test)
    acc = accuracy_score(y_test, y_pred)
    print(f"  LightGBM accuracy: {acc:.4f}")
    print(classification_report(y_test, y_pred, target_names=["CANCELLED/DISPUTED", "COMPLETED"]))

    importances = model.feature_importances_
    for name, imp in sorted(zip(X.columns, importances), key=lambda t: -t[1]):
        print(f"    {name}: {imp}")

    artifacts = {"model": model, "label_encoder": le, "categorical_cols": cat_cols, "numerical_cols": num_cols}
    joblib.dump(artifacts, "models/booking_completion_model.joblib")
    return artifacts


def prepare_project_features(df: pd.DataFrame):
    num_cols = ["team_size", "milestone_count", "budget", "avg_milestone_duration_days",
                "blocked_milestone_count", "completed_milestone_pct", "total_planned_days",
                "dependency_density", "current_slippage_days"]
    df = df.copy()
    for c in num_cols:
        df[c] = pd.to_numeric(df[c], errors="coerce").fillna(0)
    return df[num_cols], df["label"].astype(int), num_cols


def train_project_model(df: pd.DataFrame):
    print("\n=== Training Project Delay Model (LightGBM) ===")
    X, y, num_cols = prepare_project_features(df)
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    model = LGBMClassifier(
        n_estimators=200,
        max_depth=6,
        learning_rate=0.08,
        num_leaves=31,
        min_child_samples=5,
        subsample=0.8,
        colsample_bytree=0.8,
        reg_alpha=0.1,
        reg_lambda=0.1,
        random_state=42,
        verbose=-1
    )
    model.fit(X_train, y_train, eval_set=[(X_test, y_test)], callbacks=[])

    y_pred = model.predict(X_test)
    acc = accuracy_score(y_test, y_pred)
    print(f"  LightGBM accuracy: {acc:.4f}")
    print(classification_report(y_test, y_pred, target_names=["DELAYED", "ON_TIME"]))

    importances = model.feature_importances_
    for name, imp in sorted(zip(X.columns, importances), key=lambda t: -t[1]):
        print(f"    {name}: {imp}")

    artifacts = {"model": model, "feature_cols": num_cols}
    joblib.dump(artifacts, "models/project_delay_model.joblib")
    return artifacts


def download_sentiment140_dataset(output_path: str | None = None):
    """
    Download Sentiment140 from Hugging Face and save a balanced sample to CSV.
    Run once at build time or manually: python train.py --download-sentiment140
    Reference: Go, A., Bhayani, R. & Huang, L. (2009). Twitter Sentiment Classification
    using Distant Supervision. CS224N Project Report, Stanford University.
    Dataset: https://huggingface.co/datasets/stanfordnlp/sentiment140
    """
    if output_path is None:
        output_path = os.path.join(DATA_DIR, "sentiment140_dataset.csv")

    print("Downloading Sentiment140 from Hugging Face (stanfordnlp/sentiment140)...")
    from datasets import load_dataset
    ds = load_dataset("stanfordnlp/sentiment140", split="train", trust_remote_code=True)
    df = ds.to_pandas()

    label_map = {0: 0, 2: 1, 4: 2}
    df = df.rename(columns={"text": "text", "sentiment": "label"})
    df["label"] = df["label"].map(label_map)
    df = df.dropna(subset=["text", "label"])
    df["label"] = df["label"].astype(int)
    df["source"] = "sentiment140_hf"

    pos = df[df["label"] == 2].sample(n=min(5000, len(df[df["label"] == 2])), random_state=42)
    neu = df[df["label"] == 1].sample(n=min(2000, len(df[df["label"] == 1])), random_state=42)
    neg = df[df["label"] == 0].sample(n=min(5000, len(df[df["label"] == 0])), random_state=42)
    sampled = pd.concat([pos, neu, neg], ignore_index=True)

    sampled.to_csv(output_path, index=False)
    print(f"Saved {len(sampled)} rows to {output_path} (pos={len(pos)}, neu={len(neu)}, neg={len(neg)})")
    return sampled


def load_sentiment_dataset():
    print("\n=== Loading sentiment dataset ===")
    csv_df = load_from_csv("sentiment")

    hf_csv_path = os.path.join(DATA_DIR, "sentiment140_dataset.csv")
    if os.path.exists(hf_csv_path):
        print(f"  Loading pre-downloaded Sentiment140 from {hf_csv_path}")
        hf_df = pd.read_csv(hf_csv_path)
        if csv_df is not None:
            csv_df = pd.concat([csv_df, hf_df], ignore_index=True).drop_duplicates(subset=["text"])
        else:
            csv_df = hf_df

    db_rows = load_db_records("SENTIMENT")
    if db_rows:
        db_df = pd.DataFrame(db_rows)
        if "_label" in db_df.columns:
            db_df = db_df.rename(columns={"_label": "label"})
        if csv_df is not None:
            combined = pd.concat([csv_df, db_df], ignore_index=True).drop_duplicates()
        else:
            combined = db_df
    elif csv_df is not None:
        combined = csv_df
    else:
        combined = generate_synthetic_sentiment_data(600)

    if len(combined) < 50:
        synthetic = generate_synthetic_sentiment_data(600)
        combined = pd.concat([combined, synthetic], ignore_index=True).drop_duplicates()

    save_to_csv("sentiment", combined)
    return combined


def prepare_sentiment_features(df: pd.DataFrame):
    texts = df["text"].fillna("").astype(str).tolist()
    y = df["label"].astype(int)
    vectorizer = TfidfVectorizer(
        max_features=500,
        ngram_range=(1, 2),
        stop_words="english",
        min_df=2,
        max_df=0.95
    )
    X = vectorizer.fit_transform(texts)
    return X, y, vectorizer


def train_sentiment_model(df: pd.DataFrame):
    print("\n=== Training Sentiment Analysis Model (LightGBM + TF-IDF) ===")
    X, y, vectorizer = prepare_sentiment_features(df)
    X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

    model = LGBMClassifier(
        n_estimators=150,
        max_depth=5,
        learning_rate=0.1,
        num_leaves=15,
        min_child_samples=5,
        subsample=0.8,
        colsample_bytree=0.8,
        reg_alpha=0.1,
        reg_lambda=0.1,
        random_state=42,
        verbose=-1
    )
    model.fit(X_train, y_train)

    y_pred = model.predict(X_test)
    acc = accuracy_score(y_test, y_pred)
    print(f"  LightGBM accuracy: {acc:.4f}")
    print(classification_report(y_test, y_pred, target_names=["NEGATIVE", "NEUTRAL", "POSITIVE"]))

    artifacts = {"model": model, "vectorizer": vectorizer}
    joblib.dump(artifacts, "models/sentiment_model.joblib")
    return artifacts


if __name__ == "__main__":
    import sys
    if "--download-sentiment140" in sys.argv:
        download_sentiment140_dataset()
        sys.exit(0)

    booking_df = load_booking_dataset()
    train_booking_model(booking_df)

    project_df = load_project_dataset()
    train_project_model(project_df)

    sentiment_df = load_sentiment_dataset()
    train_sentiment_model(sentiment_df)

    print("\n=== Done. Models saved to models/ ===")
