# Wstępna dokumentacja API - PZSP2 Drony

## Informacje Ogólne

* **Zabezpieczenie dostępu** Wymagany token autoryzacji googla w nagłówku HTTP (`Authorization: Bearer <token>`). Tworzony przez bibliteoke Google Auth. Zabezpiecza to dostęp do backendu przed zewnętrznym ruchem sieciowym.
* **Base URL:** `/api`
* **Uwierzytelnianie:** Wymagany token JWT w nagłówku HTTP (`X-USER-TOKEN: Bearer <token>`) dla prawie wszystkich endpointów poza sekcją Auth.
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
      "userId": 124,
      "email" : "1243@gmail.com"
      "username": "user12345"
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

**Base URL:** `/api/orders`
**Autoryzacja:** Wymagany token JWT w nagłówku HTTP (`Authorization: Bearer <token>`).

### Utwórz zamówienie

**POST** `/createOrder`

* **Request Body:**

    ```json
    {
      "title": "Inspekcja dachu kamienicy",
      "description": "Potrzebuję nagrania inspekcyjnego dachu.",
      "service": "Kopanie rowów",
      "parameters": {
        "cecha": "wartość_cechy"
      },
      "coordinates": "52.2300,21.0100",
      "from_date": "2025-06-10T10:00:00",
      "to_date": "2025-06-10T12:00:00"
    }
    ```

* **Response (201 Created):**
  Zwraca nowo utworzony obiekt zlecenia.

    ```json
    {
      "id": 123,
      "title": "Inspekcja dachu kamienicy",
      "clientId": "fd1f3569-f530-45c5-a81f-d30a9df136e0",
      "description": "Potrzebuję nagrania inspekcyjnego dachu.",
      "service": "Kopanie rowów",
      "parameters": { "cecha": "wartość_cechy" },
      "coordinates": "52.2300,21.0100",
      "from_date": "2025-06-10T10:00:00",
      "to_date": "2025-06-10T12:00:00",
      "status": "open",
      "created_at": "2025-05-21T10:00:00"
    }
    ```

---

### Edytuj zamówienie

**PATCH** `/editOrder/:orderId`

* **Request Body:**

    ```json
    {
      "description": "Zaktualizowany opis zlecenia..."
    }
    ```

* **Response (200 OK):**
  Zwraca zaktualizowany obiekt zlecenia.

    ```json
    {
      "id": 123,
      "title": "Inspekcja dachu kamienicy",
      "clientId": "fd1f3569-f530-45c5-a81f-d30a9df136e0",
      "description": "Zaktualizowany opis zlecenia...", // zmiana
      "service": "Kopanie rowów",
      "status": "open",
      // ...pozostałe pola bez zmian
    }
    ```

---

### Akceptuj ofertę

**PATCH** `/acceptOrder/:orderId`

* **Query Parameters:**
  * `?operatorId=...` – **Wymagane** tylko, gdy ofertę akceptuje **Klient**.
  * *(Brak parametru)* – Gdy zlecenie akceptuje **Operator**, podajemy tylko `orderId` w ścieżce.

* **Response (201 Created):**
  Zwraca obiekt zlecenia ze zmienionym statusem (np. na `in_progress` lub `awaiting_operator`).

    ```json
    {
      "id": 123,
      "title": "Inspekcja dachu kamienicy",
      "clientId": "fd1f3569-f530-45c5-a81f-d30a9df136e0",
      "status": "in_progress", // "in_progress" jeśli zaakceptował klient, "awaiting_operator" jeśli zaakceptował operator
      "operator_id": 55,
      // ...pozostałe pola
    }
    ```

---

### Odrzuć ofertę

**PATCH** `/rejectOrder/:orderId`

* **Query Parameters:**
  * `?operatorId=...` – (Opcjonalnie).

* **Response (200 ok):**
Odrzucenie operatora przez zleceniodawce i na odwrót nie wpływa na staus zlecenia więc nic nie zwracamy

---

### Anuluj zamówienie

**PATCH** `/cancelOrder/:orderId`

* **Response (200 OK):**
  Zwraca anulowany obiekt zlecenia.

    ```json
    {
      "id": 123,
      "title": "Inspekcja dachu kamienicy",
      "clientId": "fd1f3569-f530-45c5-a81f-d30a9df136e0",
      "status": "cancelled",
      // ...pozostałe pola
    }
    ```

---

### Pobierz listę zamówień

**GET** `/getOrders/:status`

* **Dostępne statusy:** `open`, `awaiting_operator`, `in_progress`, `completed`, `cancelled`.

* **Response (200 OK):**

    ```json
    [
      {
        "id": 123,
        "title": "Inspekcja dachu kamienicy",
        "clientId": "fd1f3569-f530-45c5-a81f-d30a9df136e0",
        "description": "Potrzebuję nagrania inspekcyjnego dachu.",
        "service": "Kopanie rowów",
        "parameters": {
          "cecha": "wartość_cechy"
        },
        "coordinates": "52.2300,21.0100",
        "from_date": "2025-06-10T10:00:00",
        "to_date": "2025-06-10T12:00:00",
        "status": "open"
      }
    ]
    ```

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
        "clientId": "fd1f3569-f530-45c5-a81f-d30a9df136e0",
        "description": "Potrzebuję nagrania...",
        "service": "Kopanie rowów",
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

* **Response:** Lista operatorów, którzy zgłosili się do zlecenia, czyli mają status ACCEPTED

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
      "portfolio": {
          "title": "Wesele w Krakowie",
          "description": "Ujęcia z drona...",
          "photos": [
            {
              "id": 1,
              "name": "Zdjęcie skanu",
              "url": "htps:/34675745"
            },
            {
              "id": 2,
              "name": "Zdjęcie skanu v2",
              "url": "htps:/34675745"
            }
           ]
        }
    }
    ```

### Utwórz profil operatora

**POST** `/operators/createOperatorProfile`

* **Request:**

    ```json
    {
      "coordinates": "52.230000, 21.010000",
      "radius": 300, // Zasięg w km
      "certificates": ["Certyfikat UB321", "XY321"],
      "services": ["Kopanie rowów", "Malowanie scian"]
    }
    ```

* **Response (201 Created) - Nowy Profil:**

    ```json
    {
      "coordinates": "52.230000, 21.010000",
      "radius": 300,
      "certificates": ["Certyfikat UB321", "XY321"],
      "services": ["Kopanie rowów", "Malowanie scian"]
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
      "coordinates": "52.230000, 21.010000",
      "radius": 500, // Zmienione
      "certificates": ["UAVO BVLOS"], // Zmienione
      "services": ["Kopanie rowów", "Malowanie scian"]
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
    [
      "Inspekcja techniczna",
      "Fotografia ślubna"
    ]
    ```

### Dodaj nowe usługi

**POST** `/services`

* **Request:**

    ```json
    [
      "Geodezja",
      "Rolnictwo"
    ]
    ```

* **Response (201 Created) - Utworzone Usługi:**

    ```json
    [
      "Geodezja",
      "Rolnictwo"
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

Base URL: `/api/admin`.

### Pobierz statystyki systemu

**GET** `/admin/getStats`

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

**GET** `/admin/getUsers`

* **Parametry (Query):** `query` (wyszukiwanie po username/mailu), `role` (typ użytkownika), `page` (strona, domyślnie 0), `size` (wielkośc strony, domyślnie 20), 
* **Response:**

    ```json
    {
      "content": [
        {
          "id": "92142266-cdf6-45dd-8749-4ff3de268feb",
          "username": "user_1",
          "role": "CLIENT",
          "name": "Imie_1",
          "surname": "Nazwisko_1",
          "email": "user1@test.com",
          "phone_number": "595883690"
        },
        {},
       ],
      "page": {
        "size": 20,
        "number": 0,
        "totalElements": 4003,
        "totalPages": 201
        }
    }
    ```

### Zablokuj użytkownika

**PATCH** `/admin/banUser/:userId`

* **Response (200 OK) - Zablokowany Użytkownik:**

    ```json
        {
          "id": "1702fbf6-a13e-4e1e-91c4-eec1d6fc871e",
          "username": "user_5",
          "role": "BLOCKED",
          "name": "Imie_5",
          "surname": "Nazwisko_5",
          "email": "user5@test.com",
          "phone_number": "566212183"
        }
    ```

### Pobierz wszystkie zamówienia (Widok Admina)

**GET** `/admin/getOrders`

* **Response:** Pełny zrzut zamówień ze szczegółami stron.

    ```json
    {
      "content": [
        {
          "order_id": "24b71e24-62ac-4b32-99e0-3c691df633d9",
          "title": "Inspekcja dachu/mostu",
          "description": "Numeryczny Model Terenu (NMT) z filtracją roślinności.",
          "service_name": "Skaning Laserowy",
          "coordinates": "52.9096,21.3593",
          "from_date": "2025-12-25T20:41:04.904761",
          "to_date": "2025-12-31T20:41:04.904761",
          "status": "OPEN",
          "created_at": "2025-12-17T20:41:04.904761",
          "client_id": "bf3f520c-0ce9-47ea-8c22-bff041164f37",
          "operator_id": null // null jeśli brak przydzielonego operatora do zlecenia.
        },
        {},
    ]
      "page": {
        "size": 10,
        "number": 299,
        "totalElements": 3000,
        "totalPages": 300
      }
  }
    ```

---

## 8. Zdjęcia (Photos)

Base URL: `/api/photos`.

### Dodaj zdjęcia do portfolio

**POST** `/photos/addPortfolioPhotos`

* **Wymagania:** Request typu `multipart/form-data`.
* **Parametry:**
  * `images` (pliki) - wiele plików w tym samym polu
  * `names` (string) - tablica JSON z nazwami zdjęć, np. `["Nazwa 1", "Nazwa 2"]
* **Response (201 Created) - Wszystkie zdjęcia zaktualizowane o te nowo dodane:**

    ```json
    {
      "photos": [
        {
          "id": 1,
          "name": "stare zdjęcie",
          "url": "https:/3536464"
        },
        {
          "id": 2,
          "name": "nowe zdjęcie",
          "url": "https:/3536464"
        }
      ]
    }
    ```

### Usuń zdjęcia

**DELETE** `/photos/deletePhotos`
* **Wymagania:** Request typu `application/json`.
* 
* **Request:**

    ```json
    [ 1, 2, 3 ]
    ```

* **Response:** `204 No Content`
