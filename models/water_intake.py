from sqlalchemy import Column, Integer, BigInteger, ForeignKey
from sqlalchemy.orm import relationship
from database.database import Base

class WaterIntake(Base):
    __tablename__ = "water_intake"

    intake_id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    summary_id = Column(Integer, ForeignKey("daily_water_summary.summary_id"), nullable=False)
    intake_ml = Column(Integer, nullable=False)
    intake_time = Column(BigInteger, nullable=False)
    is_synced = Column(Integer, default=0, nullable=False)

    user = relationship("User", back_populates="water_intakes")
    summary = relationship("DailyWaterSummary", back_populates="water_intakes")