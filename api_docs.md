# Wstępna dokumentacja API - PZSP2 Drony

## Informacje Ogólne

* **Base URL:** `/api`
* **Uwierzytelnianie:** Wymagany token JWT w nagłówku HTTP (`Authorization: Bearer <token>`) dla prawie wszystkich endpointów poza sekcją Auth.
* **Format danych:** JSON.

---

## 1. Autoryzacja (Auth)

Endpointy publiczne `/api/auth`.

### Logowanie

**POST** `/auth/login`

* **Request:**

    ```json
    {
      "email": "jan.kowalski@gmail.com",
      "password": "1234"
    }
    ```

* **Response (200 OK):**

    ```json
    {
      "token": "45674353gfgdfhgfhgfhfghbf",
      "role": "user", // lub "admin", "operator"
    }
    ```

* **Błędy:** `401` (złe hasło), `403` (zablokowany)

### Rejestracja

**POST** `/auth/register`

* **Request:**

    ```json
    {
      "username": "jan_kowalski",
      "password": "superhaslo123",
      "name": "Jan",
      "surname": "Kowalski",
      "email": "jan@example.com",
      "phone_number": "+48 123 456 789"
    }
    ```

* **Response (201 Created) - Nowy Użytkownik:**

### Wylogowanie

 Wymagany token JWT w nagłówku HTTP (`Authorization: Bearer <token>`)
**POST** `/auth/logout`

* **Response:** `200 OK`.

---

## 2. Użytkownik (User)

Base URL: `/api/user`.

### Pobierz dane użytkownika

**GET** `/user/getUserData`

* **Parametry:** `?user_id=1` (opcjonalne).
* **Response:** Obiekt użytkownika (pola jak przy rejestracji).

```json
{
  "username": "jan_kowalski",,
  "name": "Jan",
  "surname": "Kowalski",
  "email": "jan@example.com",
  "phone_number": "+48 123 456 789"

}
```

### Edytuj wybrane dane użytkownika

**PATCH** `/user/editUserData`

* **Request:**

    ```json
    np.
    {
      "name": "Janusz",
      "phone_number": "+48 000 000 000"
    }
    ```

* **Response (202 Accepted) - Zmodyfikowany Użytkownik:**

    ```json
    {
      "username": "jan_kowalski",
      "role": "client",
      "name": "Janusz", // Zmienione
      "surname": "Kowalski",
      "email": "jan@example.com",
      "phone_number": "+48 000 000 000", // Zmienione
      "created_at": "2025-05-20T12:00:00"
    }
    ```

---

## 3. Zlecenia (Orders)

Base URL: `/api/orders`.

### Utwórz zamówienie

**POST** `/orders/createOrder`

* **Request:**

    ```json
    {
      "title": "Inspekcja dachu",
      "description": "Opis zlecenia...",
      "serviceId": 1,
      "parameters": { "typ_dachu": "spadzisty" },
      "coordinates": "52.2300,21.0100",
      "from_date": "2025-06-10T10:00:00",
      "to_date": "2025-06-10T12:00:00"
    }
    ```

* **Response (201 Created) - Nowe Zamówienie:**

    ```json
    {
      "id": 55,
      "title": "Inspekcja dachu",
      "description": "Opis zlecenia...",
      "service_id": 1,
      "parameters": { "typ_dachu": "spadzisty" },
      "coordinates": "52.2300,21.0100",
      "from_date": "2025-06-10T10:00:00",
      "to_date": "2025-06-10T12:00:00",
      "status": "open",
      "created_at": "2025-05-21T09:00:00"
    }
    ```

### Edytuj zamówienie

**PATCH** `/orders/editOrder/:orderId`

* **Request:** `{ "description": "Nowy opis..." }`
* **Response (202 Accepted):** Pełny obiekt zamówienia z nowym opisem.

### Akceptuj ofertę

**PATCH** `/orders/acceptOrder/:orderId`

* **Parametry:** `?operatorId=...` (należy podać jeśli akceptuje klient, gdy akceptuje operator nie podawać).
* **Response (201 Created):**

    ```json
    {
      "id": 55,
      "title": "Inspekcja dachu",
      "status": "in_progress", // Status zmieniony
      // ...reszta pól zamówienia
    }
    ```

### Odrzuć / Anuluj ofertę

**PATCH** `/orders/rejectOrder/:orderId`
**PATCH** `/orders/cancelOrder/:orderId`

* **Response:** Pełny obiekt zamówienia ze statusem `cancelled`

---

## 4. Operatorzy (Operators)

Base URL: `/api/operators`.

### Wyszukaj pasujące zlecenia

**GET** `/operators/getMatchedOrders`

* **Opis:** Zwraca zlecenia, które nie zostały jeszcze zatwierdzone przez obie strony (status inny niż `in_progress` lub `completed`).
* **Parametry (Query):** `location`, `radius`, `serviceId`, `fromDate`, `toDate`.
* **Response:** Lista obiektów zamówień.

    ```json
    [
      {
        "id": 55,
        "title": "Inspekcja dachu",
        "description": "Potrzebuję nagrania...",
        "service_name": "Inspekcja",
        "parameters": { "wysokosc": "20m" },
        "coordinates": "52.2300,21.0100",
        "from_date": "2025-06-10T10:00:00",
        "to_date": "2025-06-10T12:00:00",
        "created_at": "2025-05-20T12:00:00"
      },
    ]
    ```

### Pobierz informacje o operatorach (dla danego zlecenia)

**GET** `/operators/getOperatorsInfo/:orderId`

* **Response:** Lista operatorów, którzy zgłosili się do zlecenia lub pasują do niego.

    ```json
    [
      {
        "user_id": 10,
        "username": "droniarz_pl",
        "name": "Jan",
        "surname": "Kowalski",
        "certificates": ["UAVO VLOS", "UAVO BVLOS"]
      },
    ]
    ```

### Pobierz portfolio operatora

**GET** `/operators/getOperatorPortfolio/:operator_id`

* **Response:** Zwraca profil operatora połączony z danymi użytkownika i wpisami portfolio.

    ```json
    {
      "name": "Jan",
      "surname": "Kowalski",
      "username": "droniarz_pl",
      "certificates": ["UAVO VLOS"],
      "operator_services": ["Fotografia", "Wideo"],
      "email": "jan@example.com",
      "phone_number": "123456789",
      "portfolio": [
        {
          "id": 1,
          "title": "Wesele w Krakowie",
          "description": "Ujęcia z drona...",
          "photos": [
            {
              "id": 1,
              "name": "Zdjęcie skanu",
            },
            {
              "id": 2,
              "name": "Zdjęcie skanu v2",
            }
           ]
        }
      ]
    }
    ```

### Utwórz profil operatora

**POST** `/operators/createOperatorProfile`

* **Request:**

    ```json
    {
      "coordinates": "52.230000, 21.010000",
      "radius": 300, // Zasięg w km
      "services": [1, 2] // Lista ID usług
    }
    ```

* **Response (201 Created) - Nowy Profil:**

    ```json
    {
      "coordinates": "52.230000, 21.010000",
      "radius": 300,
      "certificates": [], // Domyślnie pusta lista
      "services": [1, 2]
    }
    ```

### Edytuj profil operatora

**PATCH** `/operators/editOperatorProfile`

* **Request:**

    ```json
    {
      "radius": 500,
      "certificates": ["UAVO BVLOS"]
    }
    ```

* **Response (202 Accepted) - Zaktualizowany Profil:**

    ```json
    {
      "user_id": 123,
      "coordinates": "52.230000, 21.010000",
      "radius": 500, // Zmienione
      "certificates": ["UAVO BVLOS"], // Zmienione
      "services": [1, 2]
    }
    ```

### Dodaj wpis do Portfolio

**POST** `/operators/addPortfolio`

* **Request:**

    ```json
    {
      "title": "Inspekcja mostu",
      "description": "Szczegółowe zdjęcia pęknięć..."
    }
    ```

* **Response (201 Created) - Utworzony Wpis:**

    ```json
    {
      "title": "Inspekcja mostu",
      "description": "Szczegółowe zdjęcia pęknięć...",
      "photos": []
    }
    ```

### Edytuj wpis w Portfolio

**PATCH** `/api/user/editPortfolio`

* **Request:**

    ```json
    {
      "title": "Inspekcja mostu (Aktualizacja)"
    }
    ```

* **Response (202 Accepted) - Zmieniony Wpis:**

    ```json
    {
      "id": 2611,
      "operator_id": 123,
      "title": "Inspekcja mostu (Aktualizacja)", // zmieniona wartość
      "description": "Szczegółowe zdjęcia pęknięć...",
      "photos": []
    }
    ```

---

## 5. Usługi (Services)

Base URL: `/api/services`.

### Pobierz listę usług

**GET** `/services/getServices`

* **Response:** Mapa lub lista dostępnych usług.

    ```json
    {
      "1": "Inspekcja techniczna",
      "2": "Fotografia ślubna"
    }
    ```

### Dodaj nowe usługi

**POST** `/services`

* **Request:**

    ```json
    {
      "names": ["Geodezja", "Rolnictwo"]
    }
    ```

* **Response (201 Created) - Utworzone Usługi:**

    ```json
    [
      { "id": 112, "name": "Geodezja" },
      { "id": 113, "name": "Rolnictwo" }
    ]
    ```

---

## 6. Opinie (Reviews)

Base URL: `/api/reviews`.

### Dodaj opinię

**POST** `/reviews/createReview/:orderId/:userId`

* **Request:**

    ```json
    {
      "star": 5,
      "body": "Zlecenie wykonane wzorowo, polecam."
    }
    ```

* **Response (201 Created) - Nowa Opinia:**

    ```json
    {
      "id": 88,
      "order_id": 55,
      "author_id": 10, // ID z tokena (autora)
      "target_id": 20, // ID ocenianego (z URL)
      "stars": 5,
      "body": "Zlecenie wykonane wzorowo, polecam."
    }
    ```

### Pobierz opinie o użytkowniku

**GET** `/reviews/getUserReviews/:userId`

* **Response:** Lista opinii o danym użytkowniku.

    ```json
    [
      {
        "name": "Marek",
        "surname": "Klient",
        "username": "marek123",
        "stars": 5,
        "body": "Świetny kontakt."
      }
    ]
    ```

---

## 7. Administrator (Admin)

Base URL: `/api/admins`.

### Pobierz statystyki systemu

**GET** `/admins/getStats`

* **Response:**

    ```json
    {
      "users": { "clients": 730, "operators": 270 },
      "orders": { "active": 52, "completed": 248, "avg_per_operator": 3.2 },
      "operators": {
        "busy": 18,
        "top_operator": { "operator_id": 14, "completed_orders": 57 }
      },
      "reviews": { "total": 415 }
    }
    ```

### Pobierz listę użytkowników

**GET** `/admins/getUsers`

* **Parametry (Query):** `query` (wyszukiwanie po nazwie/mailu), `role` (typ użytkownika).
* **Response:**

    ```json
    [
      {
        "id": 101,
        "username": "jan_kowalski",
        "role": "client",
        "name": "Jan",
        "surname": "Kowalski",
        "email": "jan@example.com",
        "phone_number": "+48..."
      }
    ]
    ```

### Zablokuj użytkownika

**PATCH** `/admins/banUser/:userId`

* **Response (202 Accepted) - Zablokowany Użytkownik:**

    ```json
    {
      "id": 101,
      "username": "jan_kowalski",
      "role": "blocked", // Rola zmieniona na zablokowaną
      "name": "Jan",
      "surname": "Kowalski"
    }
    ```

### Pobierz wszystkie zamówienia (Widok Admina)

**GET** `/admins/getOrders`

* **Response:** Pełny zrzut zamówień ze szczegółami stron.

    ```json
    [
     {
      "order_id": 101,
      "title": "Inspekcja dachu kamienicy",
      "description": "Potrzebuję nagrania inspekcyjnego dachu.",
      "service_name": "Inspekcja dachów",
      "coordinates": "52.2300,21.0100",
      "from_date": "2025-06-10T10:00:00",
      "to_date": "2025-06-10T12:00:00",
      "status": "pending",
      "operator_status": "pending",
      "client_status": "pending"
      "created_at": "2025-05-01T09:15:00",
      "client": {
           "user_id": 501,
           "username": "jan_kowalski",
           "name": "Jan",
           "surname": "Kowalski"
    },
    "operator": {
         "user_id": 201,
         "username": "droniarz123",
         "name": "Piotr",
         "surname": "Nowak"
         }
    }
    ]
    ```

---

## 8. Zdjęcia (Photos)

Base URL: `/api/photos`.

### Pobierz zdjęcie

**GET** `/photos/getPhoto/:photoId`

* **Response:** Obraz w formacie binarnym (`Content-Type: image/jpeg`).

### Dodaj zdjęcie do portfolio

**POST** `/photos/addPortfolioPhoto`

* **Wymagania:** Request typu `multipart/form-data`.
* **Parametry:** `photo` (plik).
* **Response (201 Created) - Zaktualizowane Portfolio:**

    ```json
    {
      "photos": [
        {
          "id": 1,
          "name": "Zdjęcie skanu",
        },
        {
          "id": 2,
          "name": "Zdjęcie skanu v2",
        }
      ]
    }
    ```

### Usuń zdjęcie

**DELETE** `/photos/deletePhoto/:photoId`

* **Response:** `204 No Content`

    ```json
    {
      "photos": [
        {
          "id": 1,
          "name": "Zdjęcie skanu",
        },
      ]
    }
    ```
