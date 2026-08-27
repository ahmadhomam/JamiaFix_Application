from app.routers.auth import router as auth_router
from app.routers.issues import router as issues_router
from app.routers.meta import router as meta_router

__all__ = ["auth_router", "issues_router", "meta_router"]
