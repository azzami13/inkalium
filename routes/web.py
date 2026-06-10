from fastapi import APIRouter, Request, Form, Depends
from fastapi.responses import HTMLResponse, RedirectResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy.orm import Session
from starlette.requests import Request
import os, requests

from database.database import get_db
from models.food_intake import FoodIntake

router = APIRouter(tags=["Web"])
templates = Jinja2Templates(directory="templates")

USDA_API_KEY = os.getenv("USDA_API_KEY")  # Tambahkan ini ke .env

# 🔹 GET: Halaman Utama (Landing Page)
@router.get("/", response_class=HTMLResponse)
def index_page(request: Request):
    return templates.TemplateResponse(request, "index.html")

# 🔹 GET: Halaman Login
@router.get("/auth/login", response_class=HTMLResponse)
def login_page(request: Request):
    return templates.TemplateResponse(request, "login.html")

# 🔹 GET: Halaman Register
@router.get("/auth/register", response_class=HTMLResponse)
def register_page(request: Request):
    return templates.TemplateResponse(request, "register.html")

# 🔹 GET: Halaman Dashboard
@router.get("/dashboard", response_class=HTMLResponse)
def dashboard_page(request: Request):
    return templates.TemplateResponse(request, "dashboard.html")

# 🔹 GET: Halaman Pencarian Makanan
@router.get("/web/food-search", response_class=HTMLResponse)
async def food_search(request: Request, query: str = None):
    results = []
    if query:
        url = "https://api.nal.usda.gov/fdc/v1/foods/search"
        params = {"api_key": USDA_API_KEY, "query": query, "pageSize": 10}
        response = requests.get(url, params=params)
        if response.status_code == 200:
            results = response.json().get("foods", [])
    return templates.TemplateResponse(request, "food_search.html", {"results": results})

# 🔹 POST: Simpan Makanan ke Database
@router.post("/web/log-food")
async def log_food(
    request: Request,
    user_id: int = Form(...),
    food_name: str = Form(...),
    calories: float = Form(...),
    protein: float = Form(...),
    fat: float = Form(...),
    carbs: float = Form(...),
    amount_grams: float = Form(...),
    db: Session = Depends(get_db)
):
    food = FoodIntake(
        user_id=user_id,
        food_name=food_name,
        calories=calories,
        protein=protein,
        fat=fat,
        carbs=carbs,
        amount_grams=amount_grams
    )
    db.add(food)
    db.commit()
    return RedirectResponse(url="/web/food-search", status_code=303)

@router.get("/web/food-search", response_class=HTMLResponse)
async def food_search(request: Request, query: str = None):
    results = []
    if query:
        url = "https://api.nal.usda.gov/fdc/v1/foods/search"
        params = {"api_key": USDA_API_KEY, "query": query, "pageSize": 10}
        response = requests.get(url, params=params)
        if response.status_code == 200:
            results = response.json().get("foods", [])
    return templates.TemplateResponse(request, "food_search.html", {"results": results})
