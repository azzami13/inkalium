from fastapi import APIRouter, Request, Depends, UploadFile, File, Form, HTTPException
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy.orm import Session
from sqlalchemy import and_
import jwt
import config
from database.database import get_db
from models.user import User
from models.food_intake import FoodIntake
from schemas.food_intake import FoodIntakeCreate, FoodIntakeResponse
from routes.auth import get_current_user
from uuid import uuid4
from pathlib import Path
from datetime import datetime, timedelta
import logging

router = APIRouter(tags=["Dashboard"])
templates = Jinja2Templates(directory="templates")

# Setup logging
logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)

UPLOAD_DIR = Path("static/uploads")
UPLOAD_DIR.mkdir(parents=True, exist_ok=True)

@router.get("/dashboard", response_class=HTMLResponse)
def dashboard(request: Request, db: Session = Depends(get_db)):
    token = request.cookies.get("access_token")
    if not token:
        logger.warning("Tidak ada token di cookies")
        return RedirectResponse(url="/auth/login")
    try:
        payload = jwt.decode(token, config.SECRET_KEY, algorithms=[config.ALGORITHM])
        email = payload.get("sub")
    except jwt.ExpiredSignatureError:
        logger.warning("Token kedaluwarsa")
        return RedirectResponse(url="/auth/login")
    db_user = db.query(User).filter(User.email == email).first()
    if not db_user:
        logger.warning("Pengguna tidak ditemukan untuk email: %s", email)
        return RedirectResponse(url="/auth/login")
    return templates.TemplateResponse(request, "dashboard.html", {"user": db_user})

@router.post("/upload-photo")
async def upload_photo(request: Request, file: UploadFile = File(...), db: Session = Depends(get_db)):
    token = request.cookies.get("access_token")
    if not token:
        logger.warning("Tidak ada token saat mengunggah foto")
        return RedirectResponse(url="/auth/login")
    try:
        payload = jwt.decode(token, config.SECRET_KEY, algorithms=[config.ALGORITHM])
        email = payload.get("sub")
    except jwt.ExpiredSignatureError:
        logger.warning("Token kedaluwarsa saat mengunggah foto")
        return RedirectResponse(url="/auth/login")
    db_user = db.query(User).filter(User.email == email).first()
    if not db_user:
        logger.warning("Pengguna tidak ditemukan untuk email: %s", email)
        return RedirectResponse(url="/auth/login")
    filename = f"{uuid4().hex}_{file.filename}"
    file_path = UPLOAD_DIR / filename
    with open(file_path, "wb") as f:
        content = await file.read()
        f.write(content)
    db_user.profile_picture = filename
    db.commit()
    logger.info("Foto profil diunggah untuk pengguna: %s", email)
    return RedirectResponse(url="/dashboard", status_code=302)

@router.post("/update-profile")
async def update_profile(request: Request, umur: int = Form(...), jenis_kelamin: str = Form(...), berat_badan: float = Form(...), tinggi_badan: float = Form(...), db: Session = Depends(get_db)):
    token = request.cookies.get("access_token")
    if not token:
        logger.warning("Tidak ada token saat memperbarui profil")
        return RedirectResponse(url="/auth/login")
    try:
        payload = jwt.decode(token, config.SECRET_KEY, algorithms=[config.ALGORITHM])
        email = payload.get("sub")
    except jwt.ExpiredSignatureError:
        logger.warning("Token kedaluwarsa saat memperbarui profil")
        return RedirectResponse(url="/auth/login")
    db_user = db.query(User).filter(User.email == email).first()
    if not db_user:
        logger.warning("Pengguna tidak ditemukan untuk email: %s", email)
        return RedirectResponse(url="/auth/login")
    db_user.umur = umur
    db_user.jenis_kelamin = jenis_kelamin
    db_user.berat_badan = berat_badan
    db_user.tinggi_badan = tinggi_badan
    db.commit()
    logger.info("Profil diperbarui untuk pengguna: %s", email)
    return RedirectResponse(url="/dashboard", status_code=302)

@router.get("/me")
def read_users_me(current_user: User = Depends(get_current_user)):
    logger.info("Mengambil data pengguna: %s", current_user.email)
    return {"email": current_user.email, "role": current_user.role}

@router.put("/me")
async def update_profile(request: Request, umur: int = Form(...), jenis_kelamin: str = Form(...), berat_badan: float = Form(...), tinggi_badan: float = Form(...), db: Session = Depends(get_db)):
    token = request.cookies.get("access_token")
    if not token:
        logger.warning("Tidak ada token saat memperbarui profil (PUT)")
        return RedirectResponse(url="/auth/login")
    try:
        payload = jwt.decode(token, config.SECRET_KEY, algorithms=[config.ALGORITHM])
        email = payload.get("sub")
    except jwt.ExpiredSignatureError:
        logger.warning("Token kedaluwarsa saat memperbarui profil (PUT)")
        return RedirectResponse(url="/auth/login")
    db_user = db.query(User).filter(User.email == email).first()
    if not db_user:
        logger.warning("Pengguna tidak ditemukan untuk email: %s", email)
        return RedirectResponse(url="/auth/login")
    if not umur or not jenis_kelamin or not berat_badan or not tinggi_badan:
        logger.error("Data profil tidak lengkap")
        raise HTTPException(status_code=422, detail="Field 'umur', 'jenis_kelamin', 'berat_badan', and 'tinggi_badan' are required")
    db_user.umur = umur
    db_user.jenis_kelamin = jenis_kelamin
    db_user.berat_badan = berat_badan
    db_user.tinggi_badan = tinggi_badan
    db.commit()
    logger.info("Profil diperbarui (PUT) untuk pengguna: %s", email)
    return {"message": "Profile updated successfully"}

@router.post("/food-intakes", response_model=FoodIntakeResponse)
async def save_food_intake(intake: FoodIntakeCreate, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    logger.debug("Menerima permintaan POST /food-intakes: %s", intake.dict())
    if intake.user_id != current_user.id:
        logger.error("Validasi user_id gagal: intake.user_id=%s, current_user.id=%s", intake.user_id, current_user.id)
        raise HTTPException(status_code=403, detail="Tidak diizinkan menyimpan data untuk pengguna ini")
    db_intake = FoodIntake(**intake.dict())
    db.add(db_intake)
    db.commit()
    db.refresh(db_intake)
    logger.info("Data asupan makanan disimpan untuk user_id=%s: %s", intake.user_id, db_intake.__dict__)
    return db_intake

@router.get("/food-intakes/history", response_model=list[FoodIntakeResponse])
async def get_food_intake_history(user_id: int, current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    logger.debug("Menerima permintaan GET /food-intakes/history untuk user_id=%s", user_id)
    if user_id != current_user.id:
        logger.error("Validasi user_id gagal: user_id=%s, current_user.id=%s", user_id, current_user.id)
        raise HTTPException(status_code=403, detail="Tidak diizinkan mengakses data pengguna ini")
    history = db.query(FoodIntake).filter(FoodIntake.user_id == user_id).all()
    logger.info("Riwayat asupan makanan diambil untuk user_id=%s: %s entri", user_id, len(history))
    return history
