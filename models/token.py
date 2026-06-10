from sqlalchemy.ext.declarative import declarative_base
from pydantic import BaseModel

Base = declarative_base()
class Token(BaseModel):
    access_token: str
    token_type: str
