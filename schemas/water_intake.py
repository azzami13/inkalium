from pydantic import BaseModel

class WaterIntakeCreate(BaseModel):
    user_id: int
    summary_id: int
    intake_ml: int
    intake_time: int

class WaterIntakeResponse(BaseModel):
    intake_id: int
    user_id: int
    summary_id: int
    intake_ml: int
    intake_time: int
    is_synced: int

    class Config:
        from_attributes = True  # Gunakan from_attributes, bukan orm_mode