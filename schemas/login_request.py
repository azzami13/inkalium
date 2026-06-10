from pydantic import BaseModel

class GoogleSignInRequest(BaseModel):
    id_token: str