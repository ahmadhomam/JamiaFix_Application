from datetime import datetime
from enum import Enum
from typing import List, Optional

from pydantic import BaseModel, ConfigDict


class IssueStatus(str, Enum):
    SUBMITTED = "SUBMITTED"
    ACKNOWLEDGED = "ACKNOWLEDGED"
    ASSIGNED = "ASSIGNED"
    IN_PROGRESS = "IN_PROGRESS"
    RESOLVED = "RESOLVED"
    CLOSED = "CLOSED"


class IssuePriority(str, Enum):
    LOW = "LOW"
    MEDIUM = "MEDIUM"
    HIGH = "HIGH"
    URGENT = "URGENT"


class IssueCreate(BaseModel):
    title: str
    description: str
    category_id: int
    location_id: int
    priority: Optional[IssuePriority] = IssuePriority.MEDIUM


class IssueStatusUpdate(BaseModel):
    status: IssueStatus
    resolution_notes: Optional[str] = None


class IssueAssignStaff(BaseModel):
    staff_id: int


class IssueCommentCreate(BaseModel):
    comment: str


class IssueCommentResponse(BaseModel):
    id: int
    issue_id: int
    user_id: int
    user_name: str
    user_role: str
    comment: str
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class IssueImageResponse(BaseModel):
    id: int
    issue_id: int
    image_url: str
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class IssueResponse(BaseModel):
    id: int
    title: str
    description: str
    category_id: int
    category_name: str
    location_id: int
    location_name: str
    status: IssueStatus
    priority: IssuePriority
    reported_by: int
    reporter_name: str
    assigned_to: Optional[int] = None
    assignee_name: Optional[str] = None
    resolution_notes: Optional[str] = None
    comments_count: int = 0
    images_count: int = 0
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class IssueDetailResponse(IssueResponse):
    comments: List[IssueCommentResponse] = []
    images: List[IssueImageResponse] = []
    allowed_next_statuses: List[IssueStatus] = []