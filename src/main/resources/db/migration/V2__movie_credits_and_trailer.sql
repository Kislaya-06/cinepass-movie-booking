-- Cast, crew and trailer pulled from TMDb on sync
alter table movies add column trailer_key varchar(20);

create table movie_credits (
    movie_id  bigint       not null references movies (id),
    position  integer      not null,
    name      varchar(150) not null,
    role      varchar(150),
    photo_url varchar(500),
    crew      boolean      not null,
    primary key (movie_id, position)
);
