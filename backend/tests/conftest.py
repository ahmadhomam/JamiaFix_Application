import os
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker
from sqlalchemy.pool import StaticPool

from app.auth import create_access_token, get_db, get_password_hash
from app.database import Base
from app.main import app
from app.models.category import Category
from app.models.location import Location
from app.models.user import User, UserRole

# In-memory SQLite for fast, isolated tests
SQLALCHEMY_DATABASE_URL = "sqlite:///:memory:"

engine = create_engine(
    SQLALCHEMY_DATABASE_URL,
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestingSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)


@pytest.fixture(scope="function")
def db_session():
    Base.metadata.create_all(bind=engine)
    db = TestingSessionLocal()
    try:
        yield db
    finally:
        db.close()
        Base.metadata.drop_all(bind=engine)


@pytest.fixture(scope="function")
def client(db_session):
    def override_get_db():
        try:
            yield db_session
        finally:
            pass

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()


@pytest.fixture
def test_seed_data(db_session):
    # Create test users
    admin = User(
        name="Test Admin",
        email="admin@test.com",
        password_hash=get_password_hash("password123"),
        role=UserRole.ADMIN.value,
    )
    staff = User(
        name="Test Staff",
        email="staff@test.com",
        password_hash=get_password_hash("password123"),
        role=UserRole.STAFF.value,
    )
    student = User(
        name="Test Student",
        email="student@test.com",
        password_hash=get_password_hash("password123"),
        role=UserRole.STUDENT.value,
    )
    other_student = User(
        name="Other Student",
        email="other@test.com",
        password_hash=get_password_hash("password123"),
        role=UserRole.STUDENT.value,
    )

    cat = Category(name="General", description="General issues")
    loc = Location(building="Main Block", room="Room 101", description="Main Building")

    db_session.add_all([admin, staff, student, other_student, cat, loc])
    db_session.commit()

    return {
        "admin": admin,
        "staff": staff,
        "student": student,
        "other_student": other_student,
        "category": cat,
        "location": loc,
        "tokens": {
            "admin": create_access_token(admin.id, admin.role),
            "staff": create_access_token(staff.id, staff.role),
            "student": create_access_token(student.id, student.role),
            "other_student": create_access_token(other_student.id, other_student.role),
        },
    }
