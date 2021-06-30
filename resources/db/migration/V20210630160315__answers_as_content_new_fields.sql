ALTER TABLE answers
    ADD COLUMN original_question text,
    ADD COLUMN duplikoitu_kysymys_hakukohde_oid text;

ALTER TABLE multi_answers
    ADD COLUMN original_question text,
    ADD COLUMN duplikoitu_kysymys_hakukohde_oid text;

CREATE OR REPLACE VIEW answers_as_content AS
SELECT application_id,
       jsonb_build_object('answers', jsonb_agg(jsonb_build_object('key', key,
                                                                  'fieldType', field_type,
                                                                  'value', value,
                                                                  'duplikoitu-kysymys-hakukohde-oid', duplikoitu_kysymys_hakukohde_oid,
                                                                  'original-question', original_question))) AS content
FROM ((SELECT application_id,
           key,
           field_type,
           to_jsonb(value) AS value,
           original_question,
           duplikoitu_kysymys_hakukohde_oid
       FROM answers)
      UNION ALL
      (SELECT application_id,
           key,
           field_type,
           (SELECT coalesce(jsonb_agg(value ORDER BY data_idx ASC), '[]'::jsonb)
           FROM multi_answer_values
           WHERE application_id = ma.application_id AND
           key = ma.key) AS value,
           original_question,
           duplikoitu_kysymys_hakukohde_oid
       FROM multi_answers AS ma)
      UNION ALL
      (SELECT application_id,
           key,
           field_type,
           (SELECT jsonb_agg(CASE
           WHEN gag.is_null THEN 'null'::jsonb
           ELSE (SELECT coalesce(jsonb_agg(value ORDER BY data_idx ASC), '[]'::jsonb)
           FROM group_answer_values
           WHERE application_id = gag.application_id AND
           key = gag.key AND
           group_idx = gag.group_idx)
           END ORDER BY group_idx ASC)
           FROM group_answer_groups AS gag
           WHERE application_id = ga.application_id AND
           key = ga.key) AS value,
           NULL as original_question,
           NULL as duplikoitu_kysymys_hakukohde_oid
       FROM group_answers AS ga)) AS t
GROUP BY application_id;