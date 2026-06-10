from pydantic import BaseModel

class LoginRequest(BaseModel):
    email: str
    password: str

class GoogleSignInRequest(BaseModel):
    id_token: str