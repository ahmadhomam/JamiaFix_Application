import os
import shutil
import uuid
from typing import List, Optional

from fastapi import APIRouter, Depends, File, HTTPException, Query, UploadFile, status
from sqlalchemy import or_
from sqlalchemy.orm import Session, joinedload

from app.auth import get_current_user, get_db, require_admin
from app.models.category import Category
from app.models.issue import Issue, IssueComment, IssueImage, IssuePriority, IssueStatus
from app.models.location import Location
from app.models.user import User, UserRole
from app.schemas.issue import (
    IssueAssignStaff,
    IssueCommentCreate,
    IssueCommentResponse,
    IssueCreate,
    IssueDetailResponse,
    IssueImageResponse,
    IssueResponse,
    IssueStatusUpdate,
)

router = APIRouter(
    prefix="/issues",
    tags=["Issues"],
)

UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "uploads")
os.makedirs(UPLOAD_DIR, exist_ok=True)

VALID_TRANSITIONS = {
    IssueStatus.SUBMITTED: [IssueStatus.ACKNOWLEDGED, IssueStatus.CLOSED],
    IssueStatus.ACKNOWLEDGED: [IssueStatus.ASSIGNED, IssueStatus.CLOSED],
    IssueStatus.ASSIGNED: [IssueStatus.IN_PROGRESS, IssueStatus.ACKNOWLEDGED, IssueStatus.CLOSED],
    IssueStatus.IN_PROGRESS: [IssueStatus.RESOLVED, IssueStatus.ASSIGNED, IssueStatus.CLOSED],
    IssueStatus.RESOLVED: [IssueStatus.CLOSED, IssueStatus.SUBMITTED],
    IssueStatus.CLOSED: [IssueStatus.SUBMITTED],
}


def compute_allowed_statuses(issue: Issue, user: User) -> List[IssueStatus]:
    current_status = IssueStatus(issue.status)
    possible = VALID_TRANSITIONS.get(current_status, [])
    allowed: List[IssueStatus] = []

    for next_status in possible:
        if can_perform_transition(issue, user, current_status, next_status):
            allowed.append(next_status)

    return allowed


def can_perform_transition(issue: Issue, user: User, current_status: IssueStatus, next_status: IssueStatus) -> bool:
    if next_status not in VALID_TRANSITIONS.get(current_status, []):
        return False

    is_admin = user.role == UserRole.ADMIN.value
    is_reporter = issue.reported_by == user.id
    is_assignee = issue.assigned_to == user.id

    if is_admin:
        return True

    if current_status == IssueStatus.SUBMITTED:
        if next_status == IssueStatus.CLOSED and is_reporter:
            return True
        return False

    if current_status == IssueStatus.ACKNOWLEDGED:
        return False  # Only admin can assign / close acknowledged issues

    if current_status == IssueStatus.ASSIGNED:
        if next_status == IssueStatus.IN_PROGRESS and (is_assignee or user.role == UserRole.STAFF.value):
            return True
        return False

    if current_status == IssueStatus.IN_PROGRESS:
        if next_status == IssueStatus.RESOLVED and (is_assignee or user.role == UserRole.STAFF.value):
            return True
        return False

    if current_status == IssueStatus.RESOLVED:
        if next_status in [IssueStatus.CLOSED, IssueStatus.SUBMITTED] and is_reporter:
            return True
        return False

    if current_status == IssueStatus.CLOSED:
        if next_status == IssueStatus.SUBMITTED and is_reporter:
            return True
        return False

    return False


def build_issue_response(issue: Issue) -> IssueResponse:
    return IssueResponse(
        id=issue.id,
        title=issue.title,
        description=issue.description,
        category_id=issue.category_id,
        category_name=issue.category.name if issue.category else "General",
        location_id=issue.location_id,
        location_name=f"{issue.location.building} ({issue.location.room})" if issue.location and issue.location.room else (issue.location.building if issue.location else "Campus"),
        status=IssueStatus(issue.status),
        priority=IssuePriority(issue.priority),
        reported_by=issue.reported_by,
        reporter_name=issue.reporter.name if issue.reporter else "Unknown",
        assigned_to=issue.assigned_to,
        assignee_name=issue.assignee.name if issue.assignee else None,
        resolution_notes=issue.resolution_notes,
        comments_count=len(issue.comments) if issue.comments is not None else 0,
        images_count=len(issue.images) if issue.images is not None else 0,
        created_at=issue.created_at,
        updated_at=issue.updated_at,
    )


@router.post("", response_model=IssueResponse, status_code=status.HTTP_201_CREATED)
def create_issue(
    issue_data: IssueCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    category = db.query(Category).filter(Category.id == issue_data.category_id).first()
    if not category:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Category not found")

    location = db.query(Location).filter(Location.id == issue_data.location_id).first()
    if not location:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Location not found")

    priority_val = issue_data.priority.value if issue_data.priority else IssuePriority.MEDIUM.value

    new_issue = Issue(
        title=issue_data.title,
        description=issue_data.description,
        category_id=issue_data.category_id,
        location_id=issue_data.location_id,
        reported_by=current_user.id,
        status=IssueStatus.SUBMITTED.value,
        priority=priority_val,
    )

    db.add(new_issue)
    db.commit()
    db.refresh(new_issue)

    # Reload with relationships
    loaded_issue = (
        db.query(Issue)
        .options(
            joinedload(Issue.category),
            joinedload(Issue.location),
            joinedload(Issue.reporter),
            joinedload(Issue.assignee),
            joinedload(Issue.comments),
            joinedload(Issue.images),
        )
        .filter(Issue.id == new_issue.id)
        .first()
    )

    return build_issue_response(loaded_issue)


@router.get("", response_model=List[IssueResponse])
def list_issues(
    status_filter: Optional[IssueStatus] = Query(None, alias="status"),
    category_id: Optional[int] = Query(None),
    location_id: Optional[int] = Query(None),
    reported_by_me: Optional[bool] = Query(False),
    assigned_to_me: Optional[bool] = Query(False),
    search: Optional[str] = Query(None),
    limit: int = Query(50, ge=1, le=100),
    offset: int = Query(0, ge=0),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    query = (
        db.query(Issue)
        .options(
            joinedload(Issue.category),
            joinedload(Issue.location),
            joinedload(Issue.reporter),
            joinedload(Issue.assignee),
            joinedload(Issue.comments),
            joinedload(Issue.images),
        )
    )

    if status_filter:
        query = query.filter(Issue.status == status_filter.value)

    if category_id:
        query = query.filter(Issue.category_id == category_id)

    if location_id:
        query = query.filter(Issue.location_id == location_id)

    if reported_by_me:
        query = query.filter(Issue.reported_by == current_user.id)

    if assigned_to_me:
        query = query.filter(Issue.assigned_to == current_user.id)

    if search:
        search_pattern = f"%{search}%"
        query = query.filter(
            or_(
                Issue.title.ilike(search_pattern),
                Issue.description.ilike(search_pattern),
            )
        )

    issues = query.order_by(Issue.created_at.desc()).offset(offset).limit(limit).all()
    return [build_issue_response(issue) for issue in issues]


@router.get("/{issue_id}", response_model=IssueDetailResponse)
def get_issue_detail(
    issue_id: int,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    issue = (
        db.query(Issue)
        .options(
            joinedload(Issue.category),
            joinedload(Issue.location),
            joinedload(Issue.reporter),
            joinedload(Issue.assignee),
            joinedload(Issue.comments).joinedload(IssueComment.user),
            joinedload(Issue.images),
        )
        .filter(Issue.id == issue_id)
        .first()
    )

    if not issue:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Issue not found")

    base_response = build_issue_response(issue)
    comments_list = [
        IssueCommentResponse(
            id=c.id,
            issue_id=c.issue_id,
            user_id=c.user_id,
            user_name=c.user.name if c.user else "User",
            user_role=c.user.role if c.user else "STUDENT",
            comment=c.comment,
            created_at=c.created_at,
        )
        for c in issue.comments
    ]
    images_list = [
        IssueImageResponse(
            id=img.id,
            issue_id=img.issue_id,
            image_url=img.image_url,
            created_at=img.created_at,
        )
        for img in issue.images
    ]
    allowed_statuses = compute_allowed_statuses(issue, current_user)

    return IssueDetailResponse(
        **base_response.model_dump(),
        comments=comments_list,
        images=images_list,
        allowed_next_statuses=allowed_statuses,
    )


@router.patch("/{issue_id}/status", response_model=IssueDetailResponse)
def update_issue_status(
    issue_id: int,
    status_update: IssueStatusUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    issue = db.query(Issue).filter(Issue.id == issue_id).first()
    if not issue:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Issue not found")

    current_status = IssueStatus(issue.status)
    next_status = status_update.status

    if current_status == next_status:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Issue is already in status '{current_status.value}'",
        )

    if not can_perform_transition(issue, current_user, current_status, next_status):
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail=f"Transition from {current_status.value} to {next_status.value} is not permitted for role '{current_user.role}'",
        )

    issue.status = next_status.value
    if status_update.resolution_notes:
        issue.resolution_notes = status_update.resolution_notes

    # If transitioning to IN_PROGRESS and assigned_to is null, auto-assign to the acting staff member
    if next_status == IssueStatus.IN_PROGRESS and issue.assigned_to is None and current_user.role == UserRole.STAFF.value:
        issue.assigned_to = current_user.id

    db.commit()
    return get_issue_detail(issue_id, current_user, db)


@router.patch("/{issue_id}/assign", response_model=IssueDetailResponse)
def assign_staff(
    issue_id: int,
    assign_data: IssueAssignStaff,
    current_user: User = Depends(require_admin),
    db: Session = Depends(get_db),
):
    issue = db.query(Issue).filter(Issue.id == issue_id).first()
    if not issue:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Issue not found")

    staff = db.query(User).filter(User.id == assign_data.staff_id, User.role == UserRole.STAFF.value).first()
    if not staff:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Selected user is not a valid maintenance staff member")

    issue.assigned_to = staff.id
    if issue.status in [IssueStatus.SUBMITTED.value, IssueStatus.ACKNOWLEDGED.value]:
        issue.status = IssueStatus.ASSIGNED.value

    db.commit()
    return get_issue_detail(issue_id, current_user, db)


@router.post("/{issue_id}/comments", response_model=IssueCommentResponse, status_code=status.HTTP_201_CREATED)
def add_comment(
    issue_id: int,
    comment_data: IssueCommentCreate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    issue = db.query(Issue).filter(Issue.id == issue_id).first()
    if not issue:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Issue not found")

    new_comment = IssueComment(
        issue_id=issue_id,
        user_id=current_user.id,
        comment=comment_data.comment.strip(),
    )
    db.add(new_comment)
    db.commit()
    db.refresh(new_comment)

    return IssueCommentResponse(
        id=new_comment.id,
        issue_id=new_comment.issue_id,
        user_id=new_comment.user_id,
        user_name=current_user.name,
        user_role=current_user.role,
        comment=new_comment.comment,
        created_at=new_comment.created_at,
    )


@router.post("/{issue_id}/images", response_model=IssueImageResponse, status_code=status.HTTP_201_CREATED)
def upload_image(
    issue_id: int,
    file: UploadFile = File(...),
    _: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    issue = db.query(Issue).filter(Issue.id == issue_id).first()
    if not issue:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Issue not found")

    # Generate unique filename
    ext = os.path.splitext(file.filename or "")[1]
    if not ext:
        ext = ".jpg"
    filename = f"issue_{issue_id}_{uuid.uuid4().hex[:8]}{ext}"
    file_path = os.path.join(UPLOAD_DIR, filename)

    with open(file_path, "wb") as buffer:
        shutil.copyfileobj(file.file, buffer)

    image_url = f"/uploads/{filename}"

    new_img = IssueImage(
        issue_id=issue_id,
        image_url=image_url,
    )
    db.add(new_img)
    db.commit()
    db.refresh(new_img)

    return IssueImageResponse(
        id=new_img.id,
        issue_id=new_img.issue_id,
        image_url=new_img.image_url,
        created_at=new_img.created_at,
    )
