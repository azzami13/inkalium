from sqlalchemy import Boolean, Column, DateTime, Float, ForeignKey, Integer, String
from sqlalchemy.orm import relationship
from database.database import Base


class ActivitySession(Base):
    __tablename__ = "activity_sessions"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id", ondelete="CASCADE"), index=True, nullable=False)
    activity_type = Column(String, nullable=False)
    uses_location = Column(Boolean, default=False, nullable=False)
    start_time = Column(DateTime, nullable=False)
    end_time = Column(DateTime, nullable=False)
    duration_seconds = Column(Integer, nullable=False, default=0)
    total_distance = Column(Float, nullable=False, default=0.0)
    avg_speed = Column(Float, nullable=False, default=0.0)
    calories_burned = Column(Float, nullable=False, default=0.0)

    user = relationship("User", back_populates="activity_sessions")
    location_points = relationship(
        "ActivityLocationPoint",
        back_populates="session",
        cascade="all, delete-orphan",
        order_by="ActivityLocationPoint.timestamp"
    )


class ActivityLocationPoint(Base):
    __tablename__ = "activity_location_points"

    id = Column(Integer, primary_key=True, index=True)
    session_id = Column(Integer, ForeignKey("activity_sessions.id", ondelete="CASCADE"), index=True, nullable=False)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    altitude = Column(Float, nullable=True)
    accuracy = Column(Float, nullable=True)
    timestamp = Column(DateTime, nullable=False)

    session = relationship("ActivitySession", back_populates="location_points")
