CREATE TABLE open_position_side_duplicate_ids (
    id BIGINT NOT NULL,
    CONSTRAINT pk_open_position_side_duplicate_ids PRIMARY KEY (id)
);

INSERT INTO open_position_side_duplicate_ids (id)
SELECT id
  FROM (
        SELECT id,
               ROW_NUMBER() OVER (
                   PARTITION BY member_id, symbol, position_side
                   ORDER BY updated_at DESC, created_at DESC, id DESC
               ) AS duplicate_rank
          FROM open_positions
       ) ranked_open_positions
 WHERE duplicate_rank > 1;

DELETE FROM open_positions
 WHERE id IN (SELECT id FROM open_position_side_duplicate_ids);

DROP TABLE open_position_side_duplicate_ids;

CREATE UNIQUE INDEX uk_open_position_member_symbol_side
    ON open_positions (member_id, symbol, position_side);
