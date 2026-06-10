from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from datetime import datetime
from models.user import User
from models.water_intake import WaterIntake
from models.daily_water_summary import DailyWaterSummary
from database.database import get_db
from pydantic import BaseModel
from typing import List

router = APIRouter(prefix="/water-intake", tags=["Water Intake"])

# Skema untuk input asupan air
class WaterIntakeCreate(BaseModel):
    intake_ml: int
    intake_time: int
    is_synced: int = 0

# Skema untuk respons asupan air
class WaterIntakeResponse(BaseModel):
    intake_id: int
    user_id: int
    summary_id: int
    intake_ml: int
    intake_time: int
    is_synced: int

    class Config:
        from_attributes = True

# Endpoint untuk menambahkan asupan air
@router.post("/", response_model=WaterIntakeResponse)
async def add_water_intake(
    water_intake: WaterIntakeCreate,
    db: Session = Depends(get_db)
):
    # Asumsikan user_id diketahui (misalnya, dari token JWT)
    # Untuk contoh, kita gunakan user_id=1
    user_id = 1
    db_user = db.query(User).filter(User.id == user_id).first()
    if not db_user:
        raise HTTPException(status_code=404, detail="User tidak ditemukan")

    # Cek atau buat DailyWaterSummary untuk hari ini
    today = datetime.utcnow().strftime("%Y-%m-%d")
    db_summary = db.query(DailyWaterSummary).filter(
        DailyWaterSummary.user_id == user_id,
        DailyWaterSummary.date == today
    ).first()

    if not db_summary:
        db_summary = DailyWaterSummary(
            user_id=user_id,
            date=today,
            total_ml=0
        )
        db.add(db_summary)
        db.commit()
        db.refresh(db_summary)

    # Tambahkan asupan air
    db_water_intake = WaterIntake(
        user_id=user_id,
        summary_id=db_summary.summary_id,
        intake_ml=water_intake.intake_ml,
        intake_time=water_intake.intake_time,
        is_synced=water_intake.is_synced
    )
    db_summary.total_ml += water_intake.intake_ml
    db.add(db_water_intake)
    db.commit()
    db.refresh(db_water_intake)

    return db_water_intake

# Endpoint untuk mendapatkan semua asupan air pengguna
@router.get("/", response_model=List[WaterIntakeResponse])
async def get_water_intakes(
    db: Session = Depends(get_db)
):
    # Asumsikan user_id diketahui
    user_id = 1
    return db.query(WaterIntake).filter(WaterIntake.user_id == user_id).all()
