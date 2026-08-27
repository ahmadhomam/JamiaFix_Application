from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict


class LocationCreate(BaseModel):
    building: str
    room: Optional[str] = None
    description: Optional[str] = None


class LocationResponse(BaseModel):
    id: int
    building: str
    room: Optional[str] = None
    description: Optional[str] = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)
