DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS new_matched_orders CASCADE;
DROP TABLE IF EXISTS operator_service CASCADE;
DROP TABLE IF EXISTS portfolio CASCADE;
DROP TABLE IF EXISTS drone_operator CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS services CASCADE;
DROP TABLE IF EXISTS users CASCADE;

DROP TYPE IF EXISTS matched_order_status CASCADE;
DROP TYPE IF EXISTS order_status CASCADE;
DROP TYPE IF EXISTS user_role CASCADE;

CREATE TYPE user_role AS ENUM (
    'client',
    'operator',
    'admin',
    'blocked'
    );

CREATE TYPE order_status AS ENUM (
    'open',
    'awaiting_operator',
    'in_progress',
    'completed',
    'cancelled'
    );

CREATE TYPE matched_order_status AS ENUM (
    'pending',
    'accepted',
    'rejected'
    );



CREATE TABLE users
(
    id                  UUID PRIMARY KEY             DEFAULT gen_random_uuid(),
    role                user_role           NOT NULL DEFAULT 'client',
    username            VARCHAR(255) UNIQUE NOT NULL,
    name                VARCHAR(255)        NOT NULL,
    surname             VARCHAR(255)        NOT NULL,
    password            VARCHAR(255)        NOT NULL,
    contact             JSONB               NOT NULL,
    created_at          TIMESTAMP                    DEFAULT NOW(),
    google_user_id      VARCHAR(255),
    google_access_token VARCHAR(255)
);

CREATE TABLE services
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE drone_operator
(
    user_id      UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    coordinates  VARCHAR(255), -- Np. "52.2297,21.0122"
    radius       INTEGER,
    certificates JSONB         -- np. ['cert1', 'cert2']
);

CREATE TABLE portfolio
(
    id          UUID PRIMARY KEY,
    operator_id UUID NOT NULL REFERENCES drone_operator (user_id) ON DELETE CASCADE,
    description TEXT,
    title       VARCHAR(255),
    photos      JSONB
    /** np. "photos": [{
                "id": 1,
                "name": "photo scan",
                "photo_url": "https://example.com/photo1.jpg"
                    },
                {
                    "id": 2,
                    "name": "photo scan v2",
                    "photo_url": "https://example.com/photo2.jpg"
                }]**/
);

CREATE TABLE operator_service
(
    id          SERIAL PRIMARY KEY,
    service_id  INTEGER NOT NULL REFERENCES services (id) ON DELETE CASCADE,
    operator_id UUID NOT NULL REFERENCES drone_operator (user_id) ON DELETE CASCADE,
    UNIQUE (service_id, operator_id)
);

CREATE TABLE orders
(
    id          UUID PRIMARY KEY,
    title       VARCHAR(255),
    user_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    description TEXT,
    service_id  INTEGER REFERENCES services (id),
    parameters  JSONB,  -- key -value pairs specific to the service
    coordinates VARCHAR(255),
    from_date   TIMESTAMP,
    to_date     TIMESTAMP,
    created_at  TIMESTAMP    DEFAULT NOW(),
    status      order_status DEFAULT 'open'
);

CREATE TABLE new_matched_orders
(
    id              SERIAL PRIMARY KEY,
    operator_id     UUID NOT NULL REFERENCES drone_operator (user_id) ON DELETE CASCADE,
    order_id        UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    operator_status matched_order_status DEFAULT 'pending',
    client_status   matched_order_status DEFAULT 'pending',
    UNIQUE (operator_id, order_id) -- One operator can match to one order only once
);

CREATE TABLE reviews
(
    id        SERIAL PRIMARY KEY,
    order_id  UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    body      TEXT,
    stars     INTEGER CHECK (stars >= 1 AND stars <= 5)
);