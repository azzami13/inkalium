from sqlalchemy import Column, Integer, String, DateTime, Float
from sqlalchemy.orm import relationship
from datetime import datetime
from database.database import Base
from models.daily_water_summary import DailyWaterSummary

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True)
    hashed_password = Column(String)
    role = Column(String, default="user")
    last_login = Column(DateTime)
    login_count = Column(Integer, default=0)
    username = Column(String, unique=True)
    umur = Column(Integer, nullable=True)
    jenis_kelamin = Column(String, nullable=True)
    berat_badan = Column(Float, nullable=True)
    tinggi_badan = Column(Float, nullable=True)
    dibuat_pada = Column(DateTime, default=datetime.utcnow)
    profile_picture = Column(String, nullable=True)

    # Relasi untuk asupan air
    water_intakes = relationship("WaterIntake", back_populates="user", cascade="all, delete-orphan")
    water_summaries = relationship("DailyWaterSummary", back_populates="user", cascade="all, delete-orphan")

    food_intakes = relationship("FoodIntake", back_populates="user", cascade="all, delete-orphan")
    activity_sessions = relationship("ActivitySession", back_populates="user", cascade="all, delete-orphan")
