# Data Directory

The CSV files in this directory are generated automatically by `train.py` using synthetic data and the Sentiment140 dataset.

## Datasets

| File | Source | Description |
|------|--------|-------------|
| `booking_dataset.csv` | Generated (synthetic) | Booking completion training data |
| `project_dataset.csv` | Generated (synthetic) | Project delay prediction training data |
| `sentiment_dataset.csv` | Sentiment140 (Kaggle) | Sentiment analysis training data |

## How to regenerate

The datasets are regenerated when the ML service starts for the first time (Docker) or by running:

```bash
cd ml-service
python train.py --download-sentiment140
```

This command:
1. Generates synthetic booking and project data
2. Downloads the Sentiment140 dataset from Kaggle
3. Trains the models and saves `.joblib` files in `models/`
4. Saves the processed datasets as CSV files in `data/`
