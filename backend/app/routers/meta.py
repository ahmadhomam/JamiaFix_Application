from typing import List

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.auth import get_current_user, get_db, require_admin
from app.models.category import Category
from app.models.location import Location
from app.models.user import User
from app.schemas.category import CategoryCreate, CategoryResponse
from app.schemas.location import LocationCreate, LocationResponse

router = APIRouter(
    tags=["Metadata"],
)


@router.get("/categories", response_model=List[CategoryResponse])
def get_categories(
    _: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return db.query(Category).order_by(Category.name.asc()).all()


@router.post("/categories", response_model=CategoryResponse, status_code=status.HTTP_201_CREATED)
def create_category(
    category_data: CategoryCreate,
    _: User = Depends(require_admin),
    db: Session = Depends(get_db),
):
    existing = db.query(Category).filter(Category.name == category_data.name).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Category already exists",
        )

    new_cat = Category(
        name=category_data.name,
        description=category_data.description,
    )
    db.add(new_cat)
    db.commit()
    db.refresh(new_cat)
    return new_cat


@router.get("/locations", response_model=List[LocationResponse])
def get_locations(
    _: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    return db.query(Location).order_by(Location.building.asc()).all()


@router.post("/locations", response_model=LocationResponse, status_code=status.HTTP_201_CREATED)
def create_location(
    location_data: LocationCreate,
    _: User = Depends(require_admin),
    db: Session = Depends(get_db),
):
    new_loc = Location(
        building=location_data.building,
        room=location_data.room,
        description=location_data.description,
    )
    db.add(new_loc)
    db.commit()
    db.refresh(new_loc)
    return new_loc
