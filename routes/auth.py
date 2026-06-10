from fastapi import APIRouter, Depends, HTTPException, Response, Request
from sqlalchemy.orm import Session
from sqlalchemy.exc import IntegrityError, SQLAlchemyError
from passlib.context import CryptContext
from jose import jwt, JWTError
from datetime import datetime, timedelta
from models.user import User
from database.database import get_db
from pydantic import BaseModel
from schemas.user import UserCreate, UserResponse
from schemas.login_request import GoogleSignInRequest
from google.oauth2 import id_token
from google.auth.transport import requests
import os
import re
from dotenv import load_dotenv
from config import SECRET_KEY, ALGORITHM, ACCESS_TOKEN_EXPIRE_MINUTES

load_dotenv()

router = APIRouter(prefix="/auth", tags=["Authentication"])
pwd_context = CryptContext(
    schemes=["pbkdf2_sha256", "bcrypt"],
    deprecated=["bcrypt"]
)

def create_access_token(data: dict):
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)

# Skema request login/register
class AuthRequest(BaseModel):
    email: str
    password: str

# Skema respons login/register
class AuthResponse(BaseModel):
    success: bool
    message: str
    token: str | None = None

def build_unique_username(db: Session, email: str, requested_username: str | None = None) -> str:
    base_username = requested_username or email.split("@")[0]
    base_username = re.sub(r"[^a-zA-Z0-9_]", "_", base_username).strip("_") or "user"
    username = base_username
    suffix = 1

    while db.query(User).filter(User.username == username).first():
        suffix += 1
        username = f"{base_username}_{suffix}"

    return username

# Endpoint untuk registrasi
@router.post("/register", response_model=AuthResponse)
async def register(
    user_data: UserCreate,
    db: Session = Depends(get_db)
):
    if db.query(User).filter(User.email == user_data.email).first():
        raise HTTPException(status_code=400, detail="Email sudah terdaftar!")

    try:
        hashed_password = pwd_context.hash(user_data.password)
        username = build_unique_username(db, user_data.email, user_data.username)

        new_user = User(
            email=user_data.email,
            hashed_password=hashed_password,
            last_login=None,
            login_count=0,
            role="user",
            username=username,
            umur=user_data.umur,
            jenis_kelamin=user_data.jenis_kelamin,
            berat_badan=user_data.berat_badan,
            tinggi_badan=user_data.tinggi_badan
        )
        db.add(new_user)
        db.commit()
        db.refresh(new_user)
    except IntegrityError:
        db.rollback()
        raise HTTPException(status_code=400, detail="Email atau username sudah terdaftar!")
    except SQLAlchemyError:
        db.rollback()
        raise HTTPException(status_code=500, detail="Gagal menyimpan user baru")

    token = create_access_token({"sub": new_user.email, "role": new_user.role})
    return AuthResponse(success=True, message="Registrasi berhasil! Silakan login.", token=token)

# Endpoint untuk login
@router.post("/login", response_model=AuthResponse)
async def login(
    auth: AuthRequest,
    response: Response,
    db: Session = Depends(get_db)
):
    db_user = db.query(User).filter(User.email == auth.email).first()
    if not db_user or not pwd_context.verify(auth.password, db_user.hashed_password):
        raise HTTPException(status_code=401, detail="Email atau password salah!")

    db_user.last_login = datetime.utcnow()
    db_user.login_count += 1
    db.commit()

    token = create_access_token({"sub": db_user.email, "role": db_user.role})
    response.set_cookie(
        key="access_token",
        value=token,
        httponly=True,
        max_age=ACCESS_TOKEN_EXPIRE_MINUTES * 60,
        samesite="lax"
    )

    return AuthResponse(success=True, message="Login berhasil!", token=token)

# Endpoint untuk Google Sign-In
@router.post("/google", response_model=AuthResponse)
async def google_sign_in(
    request: GoogleSignInRequest,
    response: Response,
    db: Session = Depends(get_db)
):
    try:
        # Verifikasi id_token
        id_info = id_token.verify_oauth2_token(
            request.id_token,
            requests.Request(),
            "YOUR_WEB_CLIENT_ID"  # Ganti dengan Web Client ID dari Google Console
        )

        # Ambil data pengguna dari token
        email = id_info["email"]
        username = id_info.get("name", email.split("@")[0])

        # Cek apakah pengguna sudah ada berdasarkan email
        db_user = db.query(User).filter(User.email == email).first()
        if not db_user:
            # Buat pengguna baru
            db_user = User(
                email=email,
                username=username,
                role="user",
                last_login=datetime.utcnow(),
                login_count=1
            )
            db.add(db_user)
        else:
            # Perbarui last_login dan login_count
            db_user.last_login = datetime.utcnow()
            db_user.login_count += 1
        db.commit()
        db.refresh(db_user)

        # Buat token JWT
        token = create_access_token({"sub": db_user.email, "role": db_user.role})
        response.set_cookie(
            key="access_token",
            value=token,
            httponly=True,
            max_age=ACCESS_TOKEN_EXPIRE_MINUTES * 60,
            samesite="lax"
        )

        return AuthResponse(success=True, message="Google Sign-In berhasil!", token=token)

    except ValueError:
        raise HTTPException(status_code=401, detail="Token Google tidak valid")

# Endpoint untuk mendapatkan data user saat ini
@router.get("/me", response_model=UserResponse)
async def get_current_user(
    request: Request,
    db: Session = Depends(get_db)
):
    token = request.cookies.get("access_token")
    if not token:
        auth_header = request.headers.get("Authorization")
        if auth_header and auth_header.startswith("Bearer "):
            token = auth_header[len("Bearer "):]

    if not token:
        raise HTTPException(status_code=401, detail="Token tidak ditemukan")

    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        email = payload.get("sub")
        if email is None:
            raise HTTPException(status_code=401, detail="Token tidak valid")
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token kedaluwarsa")
    except JWTError:
        raise HTTPException(status_code=403, detail="Token tidak valid")

    db_user = db.query(User).filter(User.email == email).first()
    if not db_user:
        raise HTTPException(status_code=404, detail="User tidak ditemukan")

    return UserResponse.from_orm(db_user)

# Endpoint untuk memperbarui profil user
@router.put("/me", response_model=UserResponse)
async def update_user(
    user_data: UserResponse,
    request: Request,
    db: Session = Depends(get_db)
):
    token = request.cookies.get("access_token")
    if not token:
        auth_header = request.headers.get("Authorization")
        if auth_header and auth_header.startswith("Bearer "):
            token = auth_header[len("Bearer "):]

    if not token:
        raise HTTPException(status_code=401, detail="Token tidak ditemukan")

    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        email = payload.get("sub")
        if email is None:
            raise HTTPException(status_code=401, detail="Token tidak valid")
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token kedaluwarsa")
    except JWTError:
        raise HTTPException(status_code=403, detail="Token tidak valid")

    db_user = db.query(User).filter(User.email == email).first()
    if not db_user:
        raise HTTPException(status_code=404, detail="User tidak ditemukan")

    # Perbarui field yang diberikan
    db_user.email = user_data.email or db_user.email
    db_user.username = user_data.username or db_user.username
    db_user.umur = user_data.umur if user_data.umur is not None else db_user.umur
    db_user.jenis_kelamin = user_data.jenis_kelamin or db_user.jenis_kelamin
    db_user.berat_badan = user_data.berat_badan if user_data.berat_badan is not None else db_user.berat_badan
    db_user.tinggi_badan = user_data.tinggi_badan if user_data.tinggi_badan is not None else db_user.tinggi_badan
    db_user.profile_picture = user_data.profile_picture or db_user.profile_picture

    db.commit()
    db.refresh(db_user)

    return UserResponse.from_orm(db_user)
