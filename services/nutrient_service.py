from typing import Dict

def calculate_nutrient_recommendations(height_cm: float, weight_kg: float, age: int, gender: str, activity_level: str) -> Dict:
    # Mifflin-St Jeor Equation for BMR
    if gender.lower() == "male":
        bmr = 10 * weight_kg + 6.25 * height_cm - 5 * age + 5
    else:
        bmr = 10 * weight_kg + 6.25 * height_cm - 5 * age - 161

    # Adjust BMR based on activity level
    activity_multipliers = {
        "sedentary": 1.2,
        "light": 1.375,
        "moderate": 1.55,
        "active": 1.725,
        "very_active": 1.9
    }
    total_calories = bmr * activity_multipliers.get(activity_level, 1.2)

    # Calculate macronutrient ranges
    nutrients = {
        "calories": total_calories,
        "protein": (total_calories * 0.15 / 4, total_calories * 0.35 / 4),  # 15-35%, 4 kcal/g
        "fat": (total_calories * 0.20 / 9, total_calories * 0.35 / 9),      # 20-35%, 9 kcal/g
        "carbs": (total_calories * 0.45 / 4, total_calories * 0.65 / 4)      # 45-65%, 4 kcal/g
    }
    return nutrients