from sqlalchemy import Column, Integer, String, ForeignKey
from sqlalchemy.orm import relationship
from database.database import Base

class DailyWaterSummary(Base):
    __tablename__ = "daily_water_summary"

    summary_id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    date = Column(String, nullable=False)  # Format: YYYY-MM-DD
    total_ml = Column(Integer, default=0)

    user = relationship("User", back_populates="water_summaries")
    water_intakes = relationship("WaterIntake", back_populates="summary", cascade="all, delete-orphan")