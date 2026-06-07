#!/bin/sh
if [ ! -f models/booking_completion_model.joblib ] || [ ! -f models/sentiment_model.joblib ]; then
  echo "Models not found, training..."
  python train.py
fi
exec uvicorn main:app --host 0.0.0.0 --port 8000
