DROP TABLE IF EXISTS reviews CASCADE;
DROP TABLE IF EXISTS new_matched_orders CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP TABLE IF EXISTS operator_service CASCADE;
DROP TABLE IF EXISTS photos CASCADE;
DROP TABLE IF EXISTS portfolio CASCADE;
DROP TABLE IF EXISTS services CASCADE;
DROP TABLE IF EXISTS users CASCADE;

DROP TYPE IF EXISTS user_role CASCADE;
DROP TYPE IF EXISTS order_status CASCADE;
DROP TYPE IF EXISTS matched_order_status CASCADE;

CREATE TYPE user_role AS ENUM (
    'CLIENT',
    'OPERATOR',
    'ADMIN',
    'BLOCKED',
    'INCOMPLETE'
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
    username            VARCHAR(255),
    name                VARCHAR(255),
    surname             VARCHAR(255),
    password            VARCHAR(255),
    email               VARCHAR(255) UNIQUE NOT NULL,
    phone_number        VARCHAR(20),
    created_at          TIMESTAMP                    DEFAULT NOW(),
    provider_user_id      VARCHAR(255) UNIQUE,
    provider_refresh_token TEXT,
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
    id          SERIAL PRIMARY KEY,
    operator_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    description TEXT,
    title       VARCHAR(255)
);

CREATE TABLE photos
(
    id           SERIAL PRIMARY KEY,
    portfolio_id INTEGER      NOT NULL REFERENCES portfolio (id) ON DELETE CASCADE,
    name         VARCHAR(255) NOT NULL,
    url    VARCHAR(255) NOT NULL
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
    title        VARCHAR(255) NOT NULL,
    user_id      UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    description  TEXT NOT NULL,
    service_name VARCHAR(100) REFERENCES services (name),
    parameters   JSONB, -- key -value pairs specific to the service
    coordinates  VARCHAR(255),
    from_date    TIMESTAMP NOT NULL,
    to_date      TIMESTAMP NOT NULL,
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