from passlib.context import CryptContext

# Buat objek hashing dengan bcrypt
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# Password yang ingin di-hash
password = "Admin123"

# Generate hash
hashed_password = pwd_context.hash(password)

print("Hash Password:", hashed_password)
