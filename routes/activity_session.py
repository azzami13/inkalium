from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session, selectinload
from database.database import get_db
from models.activity_session import ActivityLocationPoint, ActivitySession
from models.user import User
from routes.auth import get_current_user
from schemas.activity_session import ActivitySessionCreate, ActivitySessionResponse


router = APIRouter(prefix="/activity-sessions", tags=["Activity Sessions"])


@router.post("", response_model=ActivitySessionResponse)
@router.post("/", response_model=ActivitySessionResponse)
async def save_activity_session(
    session_data: ActivitySessionCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    if session_data.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized to save data for this user")

    duration_seconds = session_data.duration_seconds
    if duration_seconds <= 0:
        duration_seconds = max(0, int((session_data.end_time - session_data.start_time).total_seconds()))

    db_session = ActivitySession(
        user_id=session_data.user_id,
        activity_type=session_data.activity_type,
        uses_location=session_data.uses_location,
        start_time=session_data.start_time,
        end_time=session_data.end_time,
        duration_seconds=duration_seconds,
        total_distance=session_data.total_distance,
        avg_speed=session_data.avg_speed,
        calories_burned=session_data.calories_burned
    )
    db.add(db_session)
    db.flush()

    if session_data.uses_location and session_data.location_points:
        db.add_all([
            ActivityLocationPoint(
                session_id=db_session.id,
                latitude=point.latitude,
                longitude=point.longitude,
                altitude=point.altitude,
                accuracy=point.accuracy,
                timestamp=point.timestamp
            )
            for point in session_data.location_points
        ])

    db.commit()

    saved_session = (
        db.query(ActivitySession)
        .options(selectinload(ActivitySession.location_points))
        .filter(ActivitySession.id == db_session.id)
        .first()
    )
    return saved_session


@router.get("/history", response_model=list[ActivitySessionResponse])
async def get_activity_session_history(
    user_id: int,
    limit: int = 30,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    if user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized to access this user's data")

    return (
        db.query(ActivitySession)
        .options(selectinload(ActivitySession.location_points))
        .filter(ActivitySession.user_id == user_id)
        .order_by(ActivitySession.start_time.desc())
        .limit(limit)
        .all()
    )


@router.get("/{session_id}", response_model=ActivitySessionResponse)
async def get_activity_session_detail(
    session_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    session = (
        db.query(ActivitySession)
        .options(selectinload(ActivitySession.location_points))
        .filter(ActivitySession.id == session_id)
        .first()
    )
    if not session:
        raise HTTPException(status_code=404, detail="Activity session not found")
    if session.user_id != current_user.id:
        raise HTTPException(status_code=403, detail="Not authorized to access this session")
    return session
