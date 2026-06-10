from pydantic import BaseModel
from datetime import datetime

class FoodIntakeCreate(BaseModel):
    user_id: int
    food_name: str
    calories: float
    protein: float
    fat: float
    carbs: float
    amount_grams: float

class FoodIntakeResponse(BaseModel):
    id: int
    user_id: int
    food_name: str
    calories: float
    protein: float
    fat: float
    carbs: float
    amount_grams: float
    timestamp: datetime

    class Config:
        from_attributes = True