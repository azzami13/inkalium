from pydantic import BaseModel
from typing import List, Dict

class FoodSearchItem(BaseModel):
    fdcId: int
    description: str
    nutrients: Dict[str, float]

class FoodSearchResponse(BaseModel):
    foodItems: List[FoodSearchItem]
    totalResults: int

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