from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from database.database import get_db
from models.food_intake import FoodIntake
from schemas.food_intake import FoodIntakeCreate, FoodIntakeResponse
from routes.auth import get_current_user
from models.user import User
from pydantic import BaseModel  # Added this import
import requests
import os
from typing import List
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/food", tags=["Food"])

USDA_API_KEY = os.getenv("USDA_API_KEY")
USDA_SEARCH_URL = "https://api.nal.usda.gov/fdc/v1/foods/search"

class FoodSearchItem(BaseModel):
    fdcId: int
    description: str
    nutrients: dict
    serving_size: float = 100.0
    serving_unit: str = "g"

class FoodSearchResponse(BaseModel):
    foodItems: List[FoodSearchItem]
    totalResults: int

@router.get("/search", response_model=FoodSearchResponse)
def search_food(query: str):
    logger.info(f"Searching for food with query: {query}")
    if not USDA_API_KEY:
        logger.error("USDA_API_KEY is not set")
        raise HTTPException(status_code=500, detail="USDA API key is not configured")

    search_query = "chicken" if query.lower() == "ayam" else query
    params = {
        "api_key": USDA_API_KEY,
        "query": search_query,
        "pageSize": 20,
        "dataType": ["Foundation", "Survey (FNDDS)", "SR Legacy"],
        "requireAllWords": False
    }
    
    try:
        response = requests.get(USDA_SEARCH_URL, params=params)
        response.raise_for_status()
        data = response.json()
        logger.info(f"USDA API response: {data}")
        
        foods = data.get("foods", [])
        if not foods:
            logger.warning(f"No foods found for query: {query}")
        
        result = []
        for food in foods:
            nutrients = {}
            for nutrient in food.get("foodNutrients", []):
                nutrient_id = nutrient.get("nutrientId")
                if nutrient_id in [1003, 1004, 1005, 1008]:  # Protein, Fat, Carbs, Energy
                    nutrients[nutrient.get("nutrientName")] = nutrient.get("value", 0.0)
            result.append(FoodSearchItem(
                fdcId=food.get("fdcId"),
                description=food.get("description"),
                nutrients=nutrients,
                serving_size=food.get("servingSize", 100.0),
                serving_unit=food.get("servingSizeUnit", "g")
            ))
        
        return FoodSearchResponse(
            foodItems=result,
            totalResults=len(result)
        )
    
    except requests.RequestException as e:
        logger.error(f"Failed to fetch food data from USDA API: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Failed to fetch food data: {str(e)}")

@router.post("/intake", response_model=FoodIntakeResponse)
async def save_food_intake(
    intake: FoodIntakeCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    logger.info(f"Saving food intake for user_id: {intake.user_id}")
    if intake.user_id != current_user.id:
        logger.error("Unauthorized attempt to save food intake")
        raise HTTPException(status_code=403, detail="Not authorized to save data for this user")
    
    db_intake = FoodIntake(**intake.dict())
    db.add(db_intake)
    db.commit()
    db.refresh(db_intake)
    logger.info(f"Food intake saved successfully: {db_intake.id}")
    return db_intake

@router.get("/intake/history", response_model=list[FoodIntakeResponse])
async def get_food_intake_history(
    user_id: int,
    date: str = None,  # Format: YYYY-MM-DD
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    logger.info(f"Fetching food intake history for user_id: {user_id}, date: {date}")
    if user_id != current_user.id:
        logger.error("Unauthorized attempt to access food intake history")
        raise HTTPException(status_code=403, detail="Not authorized to access this user's data")
    
    query = db.query(FoodIntake).filter(FoodIntake.user_id == user_id)
    if date:
        query = query.filter(FoodIntake.timestamp.startswith(date))
    
    history = query.all()
    if not history:
        logger.warning(f"No food intake history found for user_id: {user_id}")
        raise HTTPException(status_code=404, detail="No food intake history found for this user")
    
    return history