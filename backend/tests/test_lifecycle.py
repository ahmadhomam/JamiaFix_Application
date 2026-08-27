def test_complete_issue_lifecycle(client, test_seed_data):
    student_token = test_seed_data["tokens"]["student"]
    admin_token = test_seed_data["tokens"]["admin"]
    staff_token = test_seed_data["tokens"]["staff"]
    staff_id = test_seed_data["staff"].id

    cat_id = test_seed_data["category"].id
    loc_id = test_seed_data["location"].id

    # 1. Student creates issue (Status: SUBMITTED)
    res = client.post(
        "/issues",
        headers={"Authorization": f"Bearer {student_token}"},
        json={
            "title": "Broken light fixture",
            "description": "Room 101 light is flickering",
            "category_id": cat_id,
            "location_id": loc_id,
            "priority": "HIGH",
        },
    )
    assert res.status_code == 201
    issue_id = res.json()["id"]
    assert res.json()["status"] == "SUBMITTED"

    # 2. Admin acknowledges issue (Status: SUBMITTED -> ACKNOWLEDGED)
    res = client.patch(
        f"/issues/{issue_id}/status",
        headers={"Authorization": f"Bearer {admin_token}"},
        json={"status": "ACKNOWLEDGED"},
    )
    assert res.status_code == 200
    assert res.json()["status"] == "ACKNOWLEDGED"

    # 3. Admin assigns staff (Status: ACKNOWLEDGED -> ASSIGNED)
    res = client.patch(
        f"/issues/{issue_id}/assign",
        headers={"Authorization": f"Bearer {admin_token}"},
        json={"staff_id": staff_id},
    )
    assert res.status_code == 200
    assert res.json()["status"] == "ASSIGNED"
    assert res.json()["assigned_to"] == staff_id

    # 4. Staff starts work (Status: ASSIGNED -> IN_PROGRESS)
    res = client.patch(
        f"/issues/{issue_id}/status",
        headers={"Authorization": f"Bearer {staff_token}"},
        json={"status": "IN_PROGRESS"},
    )
    assert res.status_code == 200
    assert res.json()["status"] == "IN_PROGRESS"

    # 5. Staff marks resolved (Status: IN_PROGRESS -> RESOLVED)
    res = client.patch(
        f"/issues/{issue_id}/status",
        headers={"Authorization": f"Bearer {staff_token}"},
        json={
            "status": "RESOLVED",
            "resolution_notes": "Replaced the LED tube and choke.",
        },
    )
    assert res.status_code == 200
    assert res.json()["status"] == "RESOLVED"
    assert res.json()["resolution_notes"] == "Replaced the LED tube and choke."

    # 6. Student confirms and closes (Status: RESOLVED -> CLOSED)
    res = client.patch(
        f"/issues/{issue_id}/status",
        headers={"Authorization": f"Bearer {student_token}"},
        json={"status": "CLOSED"},
    )
    assert res.status_code == 200
    assert res.json()["status"] == "CLOSED"

    # 7. Student reopens issue (Status: CLOSED -> SUBMITTED)
    res = client.patch(
        f"/issues/{issue_id}/status",
        headers={"Authorization": f"Bearer {student_token}"},
        json={"status": "SUBMITTED"},
    )
    assert res.status_code == 200
    assert res.json()["status"] == "SUBMITTED"


def test_illegal_state_transition_rejected(client, test_seed_data):
    student_token = test_seed_data["tokens"]["student"]
    cat_id = test_seed_data["category"].id
    loc_id = test_seed_data["location"].id

    # Student creates issue
    res = client.post(
        "/issues",
        headers={"Authorization": f"Bearer {student_token}"},
        json={
            "title": "Broken window",
            "description": "Window glass cracked",
            "category_id": cat_id,
            "location_id": loc_id,
        },
    )
    issue_id = res.json()["id"]

    # Student cannot directly jump to RESOLVED
    res = client.patch(
        f"/issues/{issue_id}/status",
        headers={"Authorization": f"Bearer {student_token}"},
        json={"status": "RESOLVED"},
    )
    assert res.status_code == 403


def test_unauthorized_staff_assignment_fails(client, test_seed_data):
    student_token = test_seed_data["tokens"]["student"]
    staff_id = test_seed_data["staff"].id
    cat_id = test_seed_data["category"].id
    loc_id = test_seed_data["location"].id

    # Student creates issue
    res = client.post(
        "/issues",
        headers={"Authorization": f"Bearer {student_token}"},
        json={
            "title": "Air Conditioner issue",
            "description": "AC not cooling",
            "category_id": cat_id,
            "location_id": loc_id,
        },
    )
    issue_id = res.json()["id"]

    # Student attempts to assign staff -> Must be forbidden (Admin only)
    res = client.patch(
        f"/issues/{issue_id}/assign",
        headers={"Authorization": f"Bearer {student_token}"},
        json={"staff_id": staff_id},
    )
    assert res.status_code == 403
