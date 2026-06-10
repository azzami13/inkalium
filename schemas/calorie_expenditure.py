from pydantic import BaseModel
from datetime import datetime

class CalorieExpenditureCreate(BaseModel):
    user_id: int
    calories: float
    date: str

class CalorieExpenditureResponse(BaseModel):
    id: int
    user_id: int
    calories: float
    date: str

    class Config:
        from_attributes = True  # Ganti orm_mode dengan from_attributes