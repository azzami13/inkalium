from database.database import Base, engine
from models.user import User
from models.water_intake import WaterIntake
from models.daily_water_summary import DailyWaterSummary
from models.calorie_expenditure import CalorieExpenditure
from models.food_intake import FoodIntake
from models.activity_session import ActivitySession, ActivityLocationPoint

Base.metadata.drop_all(bind=engine)  # Hapus tabel lama
Base.metadata.create_all(bind=engine)  # Buat tabel baru
