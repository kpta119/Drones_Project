-- 1. Czyszczenie tabel
TRUNCATE reviews, new_matched_orders, orders, operator_service, photos, portfolio, users, services CASCADE;

-- 2. Definiowanie USŁUG (Główne kategorie z Twojej listy)
INSERT INTO services (name) VALUES
                                ('Fotografia/Wideo'),
                                ('Skaning Laserowy (LiDAR)'),
                                ('Fotogrametria i Geodezja'),
                                ('Inspekcja Techniczna')
ON CONFLICT DO NOTHING;

-- 3. Generowanie UŻYTKOWNIKÓW (5000 sztuk)
INSERT INTO users (id, role, username, name, surname, password, email, phone_number, coordinates, radius, certificates)
SELECT
    gen_random_uuid(),
    CASE WHEN (i % 5) = 0 THEN 'OPERATOR'::user_role ELSE 'CLIENT'::user_role END, -- 20% to operatorzy
    'user_' || i,
    'Imie_' || i,
    'Nazwisko_' || i,
    '$2a$10$NotRealHashForTestingPurposesOnly..............',
    'user' || i || '@test.com',
    CAST(500000000 + floor(random() * 99999999) AS VARCHAR),
    -- Koordynaty w Polsce
    (49.0 + random() * 5.8)::numeric(10,4) || ',' || (14.0 + random() * 10.0)::numeric(10,4),
    CASE WHEN (i % 5) = 0 THEN floor(random() * 150 + 20)::int ELSE NULL END, -- Radius 20-170km
    '["UAVO VLOS", "UAVO BVLOS", "NSTS-01", "NSTS-05", "NSTS-06"]'::jsonb
FROM generate_series(1, 5000) AS i;

-- 4. Przypisanie usług do Operatorów
INSERT INTO operator_service (service_name, operator_id)
SELECT
    s.name,
    u.id
FROM users u
         CROSS JOIN services s
WHERE u.role = 'OPERATOR'
  AND random() < 0.4 -- Każdy operator ma średnio 40% dostępnych usług
ON CONFLICT DO NOTHING;

-- 5. Generowanie ZLECEŃ (Orders) - Tu dzieje się magia PRO vs NOOB
INSERT INTO orders (id, title, user_id, description, service_name, parameters, coordinates, from_date, to_date, status)
SELECT
    gen_random_uuid(),

    -- Tytuł zlecenia
    CASE
        WHEN random() < 0.25 THEN 'Szybkie zdjęcia drona'
        WHEN random() < 0.50 THEN 'Pomiary hałdy/terenu'
        WHEN random() < 0.75 THEN 'Inspekcja dachu/mostu'
        ELSE 'Opracowanie ortofotomapy i NMT'
        END,

    -- ID Klienta
    (SELECT id FROM users WHERE role = 'CLIENT' OFFSET floor(random() * 3000) LIMIT 1),

    -- OPIS (Podział na PRO i NOOB)
    CASE
        -- Klient "NOOB" (nie wie czego chce)
        WHEN random() < 0.5 THEN
            (ARRAY[
                'Potrzebuję ładnych zdjęć działki na sprzedaż.',
                'Chcę sprawdzić czy rynny są zapchane.',
                'Ile kosztuje przelot nad domem?',
                'Potrzebuję mapy terenu pod budowę.'
                ])[floor(random()*4+1)]

        -- Klient "PRO" (Techniczny bełkot zgodny z checklistą)
        ELSE
            (ARRAY[
                'Wymagany nalot fotogrametryczny z GSD < 2cm.',
                'Skaning laserowy konstrukcji stalowej, format .las.',
                'Numeryczny Model Terenu (NMT) z filtracją roślinności.',
                'Ortofotomapa wysokiej rozdzielczości z fotopunktami (GCP).'
                ])[floor(random()*4+1)]
        END,

    -- Usługa
    (ARRAY['Fotografia/Wideo', 'Skaning Laserowy (LiDAR)', 'Fotogrametria i Geodezja', 'Inspekcja Techniczna'])[floor(random()*4+1)],

    -- PARAMETRY (JSONB) - Zgodne z Twoją checklistą
    CASE
        -- 1. FOTOGRAFIA / WIDEO
        WHEN random() < 0.25 THEN jsonb_build_object(
                'resolution', (ARRAY['4K', '1080p', '20MP', '48MP'])[floor(random()*4+1)],
                'format', (ARRAY['RAW', 'JPG', 'MP4', 'MOV'])[floor(random()*4+1)],
                'expectations', 'Materiał surowy do montażu'
                                  )

        -- 2. SKANING LASEROWY
        WHEN random() < 0.5 THEN jsonb_build_object(
                'output_type', (ARRAY['Chmura punktów', 'Model 3D'])[floor(random()*2+1)],
                'format', (ARRAY['.las', '.e57', '.xyz'])[floor(random()*3+1)],
                'color', (random() > 0.5), -- boolean
                'density', (floor(random() * 500 + 50)) || ' pkt/m2'
                                 )

        -- 3. GEODEZJA (Orto, NMT, Mesh) - Najbardziej skomplikowane
        WHEN random() < 0.75 THEN
            CASE
                -- Ortofotomapa
                WHEN random() < 0.33 THEN jsonb_build_object(
                        'product', 'Ortofotomapa',
                        'gsd_cm', floor(random() * 5 + 1), -- GSD 1-5 cm
                        'rtk_receiver', true,
                        'gcp_measured', (random() > 0.3), -- Czy są fotopunkty?
                        'format', 'GeoTIFF'
                                          )
                -- Numeryczny Model Terenu
                WHEN random() < 0.66 THEN jsonb_build_object(
                        'product', 'NMPT (Model Terenu)',
                        'type', (ARRAY['NMT (Teren)', 'NMPT (Pokrycie terenu)'])[floor(random()*2+1)],
                        'format', 'GeoTIFF + .xyz'
                                          )
                -- Mesh 3D
                ELSE jsonb_build_object(
                        'product', 'Model Mesh 3D',
                        'accuracy', 'High',
                        'format', '.obj'
                     )
                END

        -- 4. INSPEKCJA (Default)
        ELSE jsonb_build_object('target', 'Dach', 'report_needed', true)
        END,

    -- Koordynaty
    (52.0 + random())::numeric(10,4) || ',' || (21.0 + random())::numeric(10,4),

    -- Daty
    NOW() + (floor(random() * 10) || ' days')::interval,
    NOW() + (floor(random() * 20 + 10) || ' days')::interval,

    'OPEN'::order_status
FROM generate_series(1, 3000) AS i; -- Generujemy 3000 zleceń

-- 6. Generowanie DOPASOWAŃ (New Matched Orders)
INSERT INTO new_matched_orders (operator_id, order_id, operator_status, client_status)
SELECT
    u.id,
    o.id,
    'PENDING'::matched_order_status,
    'PENDING'::matched_order_status
FROM orders o
         JOIN users u ON u.role = 'OPERATOR'
-- Prosty warunek geograficzny "na oko" (w prawdziwej apce robi to Twój kod Java)
WHERE abs(CAST(SPLIT_PART(o.coordinates, ',', 1) AS float) - CAST(SPLIT_PART(u.coordinates, ',', 1) AS float)) < 0.5
  AND abs(CAST(SPLIT_PART(o.coordinates, ',', 2) AS float) - CAST(SPLIT_PART(u.coordinates, ',', 2) AS float)) < 0.5
  AND random() < 0.1 -- Ograniczamy liczbę dopasowań
LIMIT 5000
ON CONFLICT DO NOTHING;