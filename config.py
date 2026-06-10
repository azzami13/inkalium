import os
from dotenv import load_dotenv

# Memuat variabel dari .env
load_dotenv()

# Menggunakan DATABASE_URL dari .env
DATABASE_URL = os.getenv("DATABASE_URL")  # Mengambil URL database yang ada di .env

SECRET_KEY = os.getenv("SECRET_KEY", "default_secret_key")  # Default jika tidak ada di .env
ALGORITHM = os.getenv("ALGORITHM", "HS256")
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("ACCESS_TOKEN_EXPIRE_MINUTES", 60 * 24 * 30))
