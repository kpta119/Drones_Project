CREATE TYPE user_role AS ENUM (
    'CLIENT',
    'OPERATOR',
    'ADMIN',
    'BLOCKED'
    );

CREATE TYPE order_status AS ENUM (
    'OPEN',
    'AWAITING_OPERATOR',
    'IN_PROGRESS',
    'COMPLETED',
    'CANCELLED'
    );

CREATE TYPE matched_order_status AS ENUM (
    'PENDING',
    'ACCEPTED',
    'REJECTED'
    );



CREATE TABLE users
(
    id                  UUID PRIMARY KEY             DEFAULT gen_random_uuid(),
    role                user_role           NOT NULL DEFAULT 'CLIENT',
    username            VARCHAR(255)        NOT NULL,
    name                VARCHAR(255)        NOT NULL,
    surname             VARCHAR(255)        NOT NULL,
    password            VARCHAR(255)        NOT NULL,
    email               VARCHAR(255) UNIQUE NOT NULL,
    phone_number        VARCHAR(20),
    created_at          TIMESTAMP                    DEFAULT NOW(),
    google_user_id      VARCHAR(255),
    google_access_token VARCHAR(255),
    coordinates         VARCHAR(255), -- Np. "52.2297,21.0122"
    radius              INTEGER,
    certificates        JSONB         -- np. ['cert1', 'cert2']
);

CREATE TABLE services
(
    name varchar(100) PRIMARY KEY
);

CREATE TABLE portfolio
(
    id          UUID PRIMARY KEY,
    operator_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    description TEXT,
    title       VARCHAR(255)
);

CREATE TABLE photos
(
    id           SERIAL PRIMARY KEY,
    portfolio_id UUID         NOT NULL REFERENCES portfolio (id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    photo_url    VARCHAR(255) NOT NULL
);

CREATE TABLE operator_service
(
    id           SERIAL PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL REFERENCES services (name) ON DELETE CASCADE,
    operator_id  UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE (service_name, operator_id)
);

CREATE TABLE orders
(
    id           UUID PRIMARY KEY,
    title        VARCHAR(255),
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    description  TEXT,
    service_name VARCHAR(100) REFERENCES services (name),
    parameters   JSONB, -- key -value pairs specific to the service
    coordinates  VARCHAR(255),
    from_date    TIMESTAMP,
    to_date      TIMESTAMP,
    created_at   TIMESTAMP    DEFAULT NOW(),
    status       order_status DEFAULT 'OPEN'
);

CREATE TABLE new_matched_orders
(
    id              SERIAL PRIMARY KEY,
    operator_id     UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    order_id        UUID NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    operator_status matched_order_status DEFAULT 'PENDING',
    client_status   matched_order_status DEFAULT 'PENDING',
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