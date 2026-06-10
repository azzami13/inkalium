from datetime import datetime
from pydantic import BaseModel, Field


class ActivityLocationPointCreate(BaseModel):
    latitude: float
    longitude: float
    altitude: float | None = None
    accuracy: float | None = None
    timestamp: datetime


class ActivityLocationPointResponse(ActivityLocationPointCreate):
    id: int
    session_id: int

    class Config:
        from_attributes = True


class ActivitySessionCreate(BaseModel):
    user_id: int
    activity_type: str = Field(..., min_length=1)
    uses_location: bool = False
    start_time: datetime
    end_time: datetime
    duration_seconds: int = Field(default=0, ge=0)
    total_distance: float = Field(default=0.0, ge=0.0)
    avg_speed: float = Field(default=0.0, ge=0.0)
    calories_burned: float = Field(default=0.0, ge=0.0)
    location_points: list[ActivityLocationPointCreate] = Field(default_factory=list)


class ActivitySessionResponse(BaseModel):
    id: int
    user_id: int
    activity_type: str
    uses_location: bool
    start_time: datetime
    end_time: datetime
    duration_seconds: int
    total_distance: float
    avg_speed: float
    calories_burned: float
    location_points: list[ActivityLocationPointResponse] = Field(default_factory=list)

    class Config:
        from_attributes = True
