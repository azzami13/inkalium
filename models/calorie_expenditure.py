from sqlalchemy import Column, Integer, Float, String, ForeignKey
from database.database import Base

class CalorieExpenditure(Base):
    __tablename__ = "calorie_expenditure"
    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), index=True)
    calories = Column(Float, nullable=False)
    date = Column(String, nullable=False)