import sys
from app.auth import get_password_hash
from app.database import Base, SessionLocal, engine
from app.models.category import Category
from app.models.issue import Issue, IssueComment, IssueImage, IssuePriority, IssueStatus
from app.models.location import Location
from app.models.user import User, UserRole


def seed_database(reset: bool = False):
    print("[+] Initializing Database Schema...")
    if reset or "--reset" in sys.argv:
        print("[!] Dropping old tables to recreate clean schema...")
        Base.metadata.drop_all(bind=engine)

    Base.metadata.create_all(bind=engine)

    db = SessionLocal()
    try:
        # 1. Seed Categories
        categories_data = [
            {"name": "Electrical", "description": "Fans, tube lights, switchboards, power sockets, AC units"},
            {"name": "IT & Projectors", "description": "Projectors, smartboards, LAN ports, lab workstations, Wi-Fi"},
            {"name": "Plumbing & Water", "description": "Water coolers, tap leakage, washroom fittings, drainage"},
            {"name": "Furniture & Carpentry", "description": "Desks, chairs, lecture podiums, doors, locks, window panes"},
            {"name": "Civil & Infrastructure", "description": "Broken tiles, wall seepage, pathway repair, railings"},
        ]

        categories_map = {}
        for cat_info in categories_data:
            cat = db.query(Category).filter(Category.name == cat_info["name"]).first()
            if not cat:
                cat = Category(name=cat_info["name"], description=cat_info["description"])
                db.add(cat)
                db.flush()
                print(f"  [+] Category: {cat.name}")
            categories_map[cat_info["name"]] = cat

        # 2. Seed Locations
        locations_data = [
            {"building": "Faculty of Engineering & Technology", "room": "ECE Lab 3", "description": "2nd Floor, Electronics Block"},
            {"building": "Faculty of Engineering & Technology", "room": "FET Auditorium", "description": "Ground Floor"},
            {"building": "Central Library", "room": "2nd Floor Reading Hall", "description": "Main Central Library"},
            {"building": "Hall of Residence (Hostel)", "room": "Block A - Room 104", "description": "Boys Hostel"},
            {"building": "Natural Science Block", "room": "Physics Lab 1", "description": "1st Floor"},
        ]

        locations_map = {}
        for loc_info in locations_data:
            loc = db.query(Location).filter(
                Location.building == loc_info["building"],
                Location.room == loc_info["room"],
            ).first()
            if not loc:
                loc = Location(
                    building=loc_info["building"],
                    room=loc_info["room"],
                    description=loc_info["description"],
                )
                db.add(loc)
                db.flush()
                print(f"  [+] Location: {loc.building} - {loc.room}")
            locations_map[f"{loc_info['building']}_{loc_info['room']}"] = loc

        # 3. Seed Users
        users_data = [
            {
                "name": "Campus Administrator",
                "email": "admin@jamia.edu",
                "password": "adminpassword123",
                "role": UserRole.ADMIN.value,
            },
            {
                "name": "Mohd Imran (Electrician)",
                "email": "electrician@jamia.edu",
                "password": "staffpassword123",
                "role": UserRole.STAFF.value,
            },
            {
                "name": "Suresh Kumar (Plumber)",
                "email": "plumber@jamia.edu",
                "password": "staffpassword123",
                "role": UserRole.STAFF.value,
            },
            {
                "name": "Homam (Student)",
                "email": "student@jamia.edu",
                "password": "studentpassword123",
                "role": UserRole.STUDENT.value,
            },
            {
                "name": "Ayesha Khan (Student)",
                "email": "ayesha@jamia.edu",
                "password": "studentpassword123",
                "role": UserRole.STUDENT.value,
            },
        ]

        users_map = {}
        for user_info in users_data:
            user = db.query(User).filter(User.email == user_info["email"]).first()
            if not user:
                user = User(
                    name=user_info["name"],
                    email=user_info["email"],
                    password_hash=get_password_hash(user_info["password"]),
                    role=user_info["role"],
                )
                db.add(user)
                db.flush()
                print(f"  [+] User: {user.name} ({user.role}) - {user.email}")
            users_map[user_info["email"]] = user

        db.commit()

        # 4. Seed Sample Issues
        loc_ece = db.query(Location).filter(Location.building == "Faculty of Engineering & Technology", Location.room == "ECE Lab 3").first()
        loc_lib = db.query(Location).filter(Location.building == "Central Library", Location.room == "2nd Floor Reading Hall").first()
        loc_aud = db.query(Location).filter(Location.building == "Faculty of Engineering & Technology", Location.room == "FET Auditorium").first()
        loc_phy = db.query(Location).filter(Location.building == "Natural Science Block", Location.room == "Physics Lab 1").first()

        sample_issues_data = [
            {
                "title": "Projector not working in ECE Lab 3",
                "description": "HDMI signal is flickering intermittently during lectures. Needs cable/connector check.",
                "category": categories_map["IT & Projectors"].id,
                "location": loc_ece.id if loc_ece else 1,
                "reporter": users_map["student@jamia.edu"].id,
                "assignee": users_map["electrician@jamia.edu"].id,
                "status": IssueStatus.IN_PROGRESS.value,
                "priority": IssuePriority.HIGH.value,
                "comments": [
                    (users_map["student@jamia.edu"].id, "Faculty had to cancel today's slides presentation due to this."),
                    (users_map["electrician@jamia.edu"].id, "Inspecting the ceiling mount and spare HDMI cables today at 2 PM."),
                ],
            },
            {
                "title": "Water cooler leaking continuously",
                "description": "Water is pooling near the 2nd floor study tables and creating a slipping hazard.",
                "category": categories_map["Plumbing & Water"].id,
                "location": loc_lib.id if loc_lib else 1,
                "reporter": users_map["ayesha@jamia.edu"].id,
                "assignee": users_map["plumber@jamia.edu"].id,
                "status": IssueStatus.ASSIGNED.value,
                "priority": IssuePriority.URGENT.value,
                "comments": [
                    (users_map["admin@jamia.edu"].id, "Assigned to Suresh Kumar for immediate repair."),
                ],
            },
            {
                "title": "Ceiling fan making loud screeching noise",
                "description": "Fan #4 on the left side of the auditorium is vibrating and making high-pitch noise.",
                "category": categories_map["Electrical"].id,
                "location": loc_aud.id if loc_aud else 1,
                "reporter": users_map["student@jamia.edu"].id,
                "assignee": None,
                "status": IssueStatus.SUBMITTED.value,
                "priority": IssuePriority.MEDIUM.value,
                "comments": [],
            },
            {
                "title": "Broken bench support leg",
                "description": "Corner wooden desk has an unstable leg, can tip over easily.",
                "category": categories_map["Furniture & Carpentry"].id,
                "location": loc_phy.id if loc_phy else 1,
                "reporter": users_map["ayesha@jamia.edu"].id,
                "assignee": users_map["electrician@jamia.edu"].id,
                "status": IssueStatus.RESOLVED.value,
                "priority": IssuePriority.LOW.value,
                "resolution_notes": "Welded reinforced metal bracket to the desk frame and verified stability.",
                "comments": [
                    (users_map["electrician@jamia.edu"].id, "Fixed and verified with lab assistant."),
                ],
            },
        ]

        for issue_info in sample_issues_data:
            existing_issue = db.query(Issue).filter(Issue.title == issue_info["title"]).first()
            if not existing_issue:
                new_issue = Issue(
                    title=issue_info["title"],
                    description=issue_info["description"],
                    category_id=issue_info["category"],
                    location_id=issue_info["location"],
                    reported_by=issue_info["reporter"],
                    assigned_to=issue_info["assignee"],
                    status=issue_info["status"],
                    priority=issue_info["priority"],
                    resolution_notes=issue_info.get("resolution_notes"),
                )
                db.add(new_issue)
                db.flush()

                for user_id, comment_text in issue_info.get("comments", []):
                    c = IssueComment(
                        issue_id=new_issue.id,
                        user_id=user_id,
                        comment=comment_text,
                    )
                    db.add(c)

                print(f"  [+] Issue: '{new_issue.title}' [{new_issue.status}]")

        db.commit()
        print("\n[SUCCESS] Database Seed Completed Successfully!")
        print("\n--- Demo Credentials ---")
        print("Admin:   admin@jamia.edu       / adminpassword123")
        print("Staff:   electrician@jamia.edu / staffpassword123")
        print("Student: student@jamia.edu     / studentpassword123")
        print("------------------------\n")

    except Exception as e:
        db.rollback()
        print(f"[ERROR] Error seeding database: {e}")
        raise e
    finally:
        db.close()


if __name__ == "__main__":
    seed_database()
