CREATE TABLE people (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    bio TEXT,
    birth_year INT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- movie_id has no foreign key because movies are in another database (movie-service)
CREATE TABLE credits (
    id BIGSERIAL PRIMARY KEY,
    person_id BIGINT NOT NULL REFERENCES people(id) ON DELETE CASCADE,
    movie_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    character_name VARCHAR(255),
    UNIQUE (person_id, movie_id, role)
);

CREATE INDEX idx_credits_movie_id ON credits(movie_id);
