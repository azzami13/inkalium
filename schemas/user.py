from pydantic import BaseModel, EmailStr
from datetime import datetime

class UserCreate(BaseModel):
    email: EmailStr
    password: str
    username: str | None = None
    umur: int | None = None
    jenis_kelamin: str | None = None
    berat_badan: float | None = None
    tinggi_badan: float | None = None

class UserUpdate(BaseModel):
    email: EmailStr | None = None
    username: str | None = None
    umur: int | None = None
    jenis_kelamin: str | None = None
    berat_badan: float | None = None
    tinggi_badan: float | None = None
    profile_picture: str | None = None

class UserResponse(BaseModel):
    id: int
    email: str
    role: str
    username: str | None = None
    umur: int | None = None
    jenis_kelamin: str | None = None
    berat_badan: float | None = None
    tinggi_badan: float | None = None
    profile_picture: str | None = None
    dibuat_pada: datetime | None = None

    class Config:
        from_attributes = True