CREATE TABLE scale_achievement
(
    id              bigint  GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    member_id       bigint  NOT NULL
        CONSTRAINT uk_member_id_scale_achievement UNIQUE
        CONSTRAINT fk_member_id_scale_achievement REFERENCES member,
    scale_id        bigint  NOT NULL
        CONSTRAINT uk_scale_id_scale_achievement UNIQUE
        CONSTRAINT fk_scale_id_scale_achievement REFERENCES scale,
    achieved_date   timestamp(6) with time zone NOT NULL,
    created_at      timestamp(6) with time zone,
    updated_at      timestamp(6) with time zone
);
