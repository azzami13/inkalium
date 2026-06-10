import os
import uvicorn
from fastapi import FastAPI
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates
from fastapi.responses import HTMLResponse
from fastapi.requests import Request
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.sessions import SessionMiddleware
from dotenv import load_dotenv
from database.database import Base, engine
from database.schema_guard import ensure_runtime_schema
from models.user import User
from models.water_intake import WaterIntake
from models.daily_water_summary import DailyWaterSummary
from models.calorie_expenditure import CalorieExpenditure
from models.food_intake import FoodIntake
from models.activity_session import ActivitySession, ActivityLocationPoint
from routes.auth import router as auth_route
from routes.dashboard import router as dashboard_route
from routes.web import router as web_route
from routes.water_intake import router as water_intake_route
from routes.food import router as food_route
from routes.calorie_expenditure import router as calorie_expenditure_router
from routes.activity_session import router as activity_session_router

load_dotenv()

app = FastAPI(title="Kalium Backend", version="1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

SECRET_KEY = os.getenv("SECRET_KEY", "defaultsecretkey")
app.add_middleware(SessionMiddleware, secret_key=SECRET_KEY)

# Create all tables in the database
Base.metadata.create_all(bind=engine)
ensure_runtime_schema(engine)

app.include_router(auth_route)
app.include_router(dashboard_route)
app.include_router(web_route)
app.include_router(water_intake_route)
app.include_router(food_route)
app.include_router(calorie_expenditure_router)
app.include_router(activity_session_router)

app.mount("/static", StaticFiles(directory="static"), name="static")

templates = Jinja2Templates(directory="templates")

@app.get("/", response_class=HTMLResponse)
async def home(request: Request):
    return templates.TemplateResponse(request, "index.html")

if __name__ == "__main__":
    port = int(os.getenv("PORT", 8000))
    uvicorn.run(app, host="0.0.0.0", port=port)
