import httpx
from typing import Dict, Any, List
import os
from models.food_item import FoodItem, NutrientInfo, FoodSearchResponse

class EdamamService:
    BASE_URL = "https://api.edamam.com/api/food-database/v2/parser"
    
    def __init__(self):
        self.app_id = os.getenv("EDAMAM_APP_ID")
        self.app_key = os.getenv("EDAMAM_APP_KEY")
        
    async def search_food(self, query: str) -> FoodSearchResponse:
        """
        Search for food items using Edamam API
        """
        params = {
            "app_id": self.app_id,
            "app_key": self.app_key,
            "ingr": query,
            "nutrition-type": "cooking"
        }
        
        async with httpx.AsyncClient() as client:
            response = await client.get(self.BASE_URL, params=params)
            response.raise_for_status()
            data = response.json()
            
            return self._parse_response(data)
    
    def _parse_response(self, data: Dict[str, Any]) -> FoodSearchResponse:
        """
        Parse Edamam API response into our model
        """
        food_items = []
        
        for hint in data.get("hints", []):
            food = hint.get("food", {})
            measures = hint.get("measures", [])
            
            # Get default measure (usually in grams)
            serving_size = 100.0
            serving_unit = "g"
            
            if measures:
                serving_size = measures[0].get("weight", 100.0)
                serving_unit = measures[0].get("label", "g")
            
            # Parse nutrients
            nutrients = {}
            nutrients_data = food.get("nutrients", {})
            
            # Map Edamam nutrient codes to our format
            nutrient_mapping = {
                "ENERC_KCAL": "CALORIES",
                "PROCNT": "PROCNT",
                "FAT": "FAT",
                "CHOCDF": "CHOCDF",
                "FIBTG": "FIBTG",
                "SUGAR": "SUGAR",
                "NA": "NA"
            }
            
            for code, our_code in nutrient_mapping.items():
                if code in nutrients_data:
                    unit = "g"
                    if code == "ENERC_KCAL":
                        unit = "kcal"
                    elif code == "NA":
                        unit = "mg"
                        
                    nutrients[our_code] = NutrientInfo(
                        label=our_code,
                        quantity=nutrients_data[code],
                        unit=unit
                    )
            
            food_item = FoodItem(
                id=food.get("foodId", ""),
                name=food.get("label", ""),
                brand=food.get("brand", None),
                calories=nutrients_data.get("ENERC_KCAL", 0),
                serving_size=serving_size,
                serving_unit=serving_unit,
                nutrients=nutrients
            )
            
            food_items.append(food_item)
        
        return FoodSearchResponse(
            food_items=food_items,
            total_results=len(food_items)
        )

class EdamamService:
    BASE_URL = "https://api.edamam.com/api/food-database/v2/parser"
    async def search_food(self, query: str) -> FoodSearchResponse:
        ...