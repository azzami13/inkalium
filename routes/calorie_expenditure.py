from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from database.database import get_db
from models.calorie_expenditure import CalorieExpenditure
from schemas.calorie_expenditure import CalorieExpenditureCreate, CalorieExpenditureResponse
from routes.auth import get_current_user
from models.user import User

router = APIRouter(prefix="/calorie-expenditure", tags=["Calorie Expenditure"])

@router.post("", response_model=CalorieExpenditureResponse)
@router.post("/", response_model=CalorieExpenditureResponse)
async def save_calorie_expenditure(
    expenditure: CalorieExpenditureCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    if expenditure.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized to save data for this user")
    db_expenditure = CalorieExpenditure(**expenditure.dict())
    db.add(db_expenditure)
    db.commit()
    db.refresh(db_expenditure)
    return db_expenditure

@router.get("/history", response_model=list[CalorieExpenditureResponse])
async def get_calorie_expenditure_history(
    user_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    if user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized to access this user's data")
    return (
        db.query(CalorieExpenditure)
        .filter(CalorieExpenditure.user_id == user_id)
        .order_by(CalorieExpenditure.id.desc())
        .all()
    )
