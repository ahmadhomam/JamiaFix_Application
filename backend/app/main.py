import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from sqlalchemy import text

from app.database import Base, engine
import app.models  # Ensures all models are registered with Base metadata
from app.routers import auth_router, issues_router, meta_router

app = FastAPI(
    title="JamiaFix API",
    description="Campus Issue & Maintenance Tracking System Backend",
    version="1.0.0",
)

# CORS middleware for local development and Android Emulator
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Ensure uploads directory exists and mount static files
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(__file__)), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=UPLOAD_DIR), name="uploads")

# Auto create tables if they don't exist
Base.metadata.create_all(bind=engine)

# Include API Routers
app.include_router(auth_router)
app.include_router(issues_router)
app.include_router(meta_router)


@app.get("/")
def root():
    return {
        "app": "JamiaFix API",
        "version": "1.0.0",
        "docs": "/docs",
        "status": "online",
    }


@app.get("/health")
def health_check():
    return {"status": "ok"}


@app.get("/health/db")
def database_health_check():
    try:
        with engine.connect() as connection:
            result = connection.execute(text("SELECT 1"))
            return {"database": "connected", "result": result.scalar()}
    except Exception as e:
        return {"database": "error", "detail": str(e)}