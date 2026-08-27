from datetime import datetime, timezone
from enum import Enum

from sqlalchemy import DateTime, ForeignKey, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base


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


class Issue(Base):
    __tablename__ = "issues"

    id: Mapped[int] = mapped_column(
        primary_key=True,
        index=True,
    )

    title: Mapped[str] = mapped_column(
        String(150),
        index=True,
    )

    description: Mapped[str] = mapped_column(
        Text,
    )

    category_id: Mapped[int] = mapped_column(
        ForeignKey("categories.id"),
        index=True,
    )

    location_id: Mapped[int] = mapped_column(
        ForeignKey("locations.id"),
        index=True,
    )

    reported_by: Mapped[int] = mapped_column(
        ForeignKey("users.id"),
        index=True,
    )

    assigned_to: Mapped[int | None] = mapped_column(
        ForeignKey("users.id"),
        nullable=True,
        index=True,
        default=None,
    )

    status: Mapped[str] = mapped_column(
        String(30),
        default=IssueStatus.SUBMITTED.value,
        index=True,
    )

    priority: Mapped[str] = mapped_column(
        String(20),
        default=IssuePriority.MEDIUM.value,
    )

    resolution_notes: Mapped[str | None] = mapped_column(
        Text,
        nullable=True,
        default=None,
    )

    created_at: Mapped[datetime] = mapped_column(
        DateTime,
        default=lambda: datetime.now(timezone.utc),
    )

    updated_at: Mapped[datetime] = mapped_column(
        DateTime,
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
    )

    # Relationships
    reporter = relationship(
        "User",
        foreign_keys=[reported_by],
        back_populates="reported_issues",
    )

    assignee = relationship(
        "User",
        foreign_keys=[assigned_to],
        back_populates="assigned_issues",
    )

    category = relationship(
        "Category",
        back_populates="issues",
    )

    location = relationship(
        "Location",
        back_populates="issues",
    )

    comments = relationship(
        "IssueComment",
        back_populates="issue",
        cascade="all, delete-orphan",
        order_by="IssueComment.created_at.asc()",
    )

    images = relationship(
        "IssueImage",
        back_populates="issue",
        cascade="all, delete-orphan",
    )


class IssueComment(Base):
    __tablename__ = "issue_comments"

    id: Mapped[int] = mapped_column(
        primary_key=True,
        index=True,
    )

    issue_id: Mapped[int] = mapped_column(
        ForeignKey("issues.id"),
        index=True,
    )

    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id"),
        index=True,
    )

    comment: Mapped[str] = mapped_column(
        Text,
    )

    created_at: Mapped[datetime] = mapped_column(
        DateTime,
        default=lambda: datetime.now(timezone.utc),
    )

    issue = relationship(
        "Issue",
        back_populates="comments",
    )

    user = relationship(
        "User",
        back_populates="comments",
    )


class IssueImage(Base):
    __tablename__ = "issue_images"

    id: Mapped[int] = mapped_column(
        primary_key=True,
        index=True,
    )

    issue_id: Mapped[int] = mapped_column(
        ForeignKey("issues.id"),
        index=True,
    )

    image_url: Mapped[str] = mapped_column(
        String(500),
    )

    created_at: Mapped[datetime] = mapped_column(
        DateTime,
        default=lambda: datetime.now(timezone.utc),
    )

    issue = relationship(
        "Issue",
        back_populates="images",
    )