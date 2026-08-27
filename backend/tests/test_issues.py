def test_create_issue_and_list_with_filters(client, test_seed_data):
    student_token = test_seed_data["tokens"]["student"]
    other_token = test_seed_data["tokens"]["other_student"]
    cat_id = test_seed_data["category"].id
    loc_id = test_seed_data["location"].id

    # Create issue by student 1
    res1 = client.post(
        "/issues",
        headers={"Authorization": f"Bearer {student_token}"},
        json={
            "title": "Lab 101 Switchboard broken",
            "description": "Switch sparks when plugged in",
            "category_id": cat_id,
            "location_id": loc_id,
            "priority": "HIGH",
        },
    )
    assert res1.status_code == 201
    issue1_id = res1.json()["id"]

    # Create issue by student 2
    res2 = client.post(
        "/issues",
        headers={"Authorization": f"Bearer {other_token}"},
        json={
            "title": "Door latch stuck",
            "description": "Cannot lock from inside",
            "category_id": cat_id,
            "location_id": loc_id,
            "priority": "LOW",
        },
    )
    assert res2.status_code == 201

    # Filter by reported_by_me
    list_res = client.get(
        "/issues?reported_by_me=true",
        headers={"Authorization": f"Bearer {student_token}"},
    )
    assert list_res.status_code == 200
    data = list_res.json()
    assert len(data) == 1
    assert data[0]["id"] == issue1_id


def test_add_and_view_comments(client, test_seed_data):
    student_token = test_seed_data["tokens"]["student"]
    staff_token = test_seed_data["tokens"]["staff"]
    cat_id = test_seed_data["category"].id
    loc_id = test_seed_data["location"].id

    # Create issue
    res = client.post(
        "/issues",
        headers={"Authorization": f"Bearer {student_token}"},
        json={
            "title": "Projector remote missing",
            "description": "Cannot turn on projector",
            "category_id": cat_id,
            "location_id": loc_id,
        },
    )
    issue_id = res.json()["id"]

    # Student adds comment
    c1 = client.post(
        f"/issues/{issue_id}/comments",
        headers={"Authorization": f"Bearer {student_token}"},
        json={"comment": "Looked in drawer, not found."},
    )
    assert c1.status_code == 201
    assert c1.json()["comment"] == "Looked in drawer, not found."

    # Staff adds comment
    c2 = client.post(
        f"/issues/{issue_id}/comments",
        headers={"Authorization": f"Bearer {staff_token}"},
        json={"comment": "Bringing a replacement remote."},
    )
    assert c2.status_code == 201

    # View issue details
    detail_res = client.get(
        f"/issues/{issue_id}",
        headers={"Authorization": f"Bearer {student_token}"},
    )
    assert detail_res.status_code == 200
    detail = detail_res.json()
    assert len(detail["comments"]) == 2
    assert detail["comments"][0]["user_name"] == "Test Student"
    assert detail["comments"][1]["user_name"] == "Test Staff"
