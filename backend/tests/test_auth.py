def test_register_student_success(client):
    response = client.post(
        "/auth/register",
        json={
            "name": "New Student",
            "email": "newstudent@test.com",
            "password": "mypassword123",
            "role": "STUDENT",
        },
    )
    assert response.status_code == 201
    data = response.json()
    assert data["name"] == "New Student"
    assert data["email"] == "newstudent@test.com"
    assert data["role"] == "STUDENT"


def test_register_duplicate_email_fails(client, test_seed_data):
    response = client.post(
        "/auth/register",
        json={
            "name": "Duplicate Student",
            "email": "student@test.com",
            "password": "mypassword123",
        },
    )
    assert response.status_code == 400
    assert "already registered" in response.json()["detail"]


def test_login_success(client, test_seed_data):
    response = client.post(
        "/auth/login",
        json={
            "email": "student@test.com",
            "password": "password123",
        },
    )
    assert response.status_code == 200
    data = response.json()
    assert "access_token" in data
    assert data["token_type"] == "bearer"
    assert data["user"]["email"] == "student@test.com"
    assert data["user"]["role"] == "STUDENT"


def test_login_invalid_password_fails(client, test_seed_data):
    response = client.post(
        "/auth/login",
        json={
            "email": "student@test.com",
            "password": "wrongpassword",
        },
    )
    assert response.status_code == 401


def test_get_me_endpoint(client, test_seed_data):
    token = test_seed_data["tokens"]["student"]
    response = client.get(
        "/auth/me",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200
    data = response.json()
    assert data["email"] == "student@test.com"
    assert data["role"] == "STUDENT"


def test_get_staff_directory(client, test_seed_data):
    token = test_seed_data["tokens"]["student"]
    response = client.get(
        "/auth/staff",
        headers={"Authorization": f"Bearer {token}"},
    )
    assert response.status_code == 200
    data = response.json()
    assert len(data) >= 1
    assert any(s["email"] == "staff@test.com" for s in data)
