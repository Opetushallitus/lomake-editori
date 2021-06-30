-- name: yesql-add-application<!
-- Add application
INSERT INTO applications (
  form_id,
  content,
  lang,
  preferred_name,
  last_name,
  hakukohde,
  haku,
  person_oid,
  ssn,
  dob,
  email,
  submitted
) VALUES (
  :form_id,
  :content,
  :lang,
  :preferred_name,
  :last_name,
  ARRAY[:hakukohde]::character varying(127)[],
  :haku,
  :person_oid,
  upper(:ssn),
  :dob,
  :email,
  now()
);

-- name: yesql-add-application-answers!
INSERT INTO answers (application_id, key, field_type, value, original_question, duplikoitu_kysymys_hakukohde_oid)
SELECT :application_id, t->>'key', t->>'fieldType', t->>'value', t->>'original-question', t->>'duplikoitu-kysymys-hakukohde-oid'
FROM jsonb_array_elements(:answers) AS t
WHERE jsonb_typeof(t->'value') = 'string' OR
      jsonb_typeof(t->'value') = 'null';

-- name: yesql-add-application-multi-answers!
INSERT INTO multi_answers (application_id, key, field_type, original_question, duplikoitu_kysymys_hakukohde_oid)
SELECT :application_id, t->>'key', t->>'fieldType', t->>'original-question', t->>'duplikoitu-kysymys-hakukohde-oid'
FROM jsonb_array_elements(:answers) AS t
WHERE jsonb_typeof(t->'value') = 'array' AND
      (jsonb_array_length(t->'value') = 0 OR
       jsonb_typeof(t->'value'->0) = 'string');

-- name: yesql-add-application-multi-answer-values!
INSERT INTO multi_answer_values (application_id, key, data_idx, value)
SELECT :application_id, t->>'key', tt.data_idx, tt.value->>0
FROM jsonb_array_elements(:answers) AS t
CROSS JOIN jsonb_array_elements(t->'value') WITH ORDINALITY AS tt(value, data_idx)
WHERE jsonb_typeof(t->'value') = 'array' AND
      jsonb_typeof(t->'value'->0) = 'string';

-- name: yesql-add-application-group-answers!
INSERT INTO group_answers (application_id, key, field_type)
SELECT :application_id, t->>'key', t->>'fieldType'
FROM jsonb_array_elements(:answers) AS t
WHERE jsonb_typeof(t->'value') = 'array' AND
      (jsonb_typeof(t->'value'->0) = 'array' OR
       jsonb_typeof(t->'value'->0) = 'null');

-- name: yesql-add-application-group-answer-groups!
INSERT INTO group_answer_groups (application_id, key, group_idx, is_null)
SELECT :application_id, t->>'key', tt.group_idx, jsonb_typeof(tt.value) = 'null'
FROM jsonb_array_elements(:answers) AS t
CROSS JOIN jsonb_array_elements(t->'value') WITH ORDINALITY AS tt(value, group_idx)
WHERE jsonb_typeof(t->'value') = 'array' AND
      (jsonb_typeof(t->'value'->0) = 'array' OR
       jsonb_typeof(t->'value'->0) = 'null');

-- name: yesql-add-application-group-answer-values!
INSERT INTO group_answer_values (application_id, key, group_idx, data_idx, value)
SELECT :application_id, t->>'key', tt.group_idx, ttt.data_idx, ttt.value->>0
FROM jsonb_array_elements(:answers) AS t
CROSS JOIN jsonb_array_elements(t->'value') WITH ORDINALITY AS tt(group_value, group_idx)
CROSS JOIN jsonb_array_elements(CASE jsonb_typeof(tt.group_value)
                                    WHEN 'array' THEN tt.group_value
                                    ELSE '[]'::jsonb
                                END) WITH ORDINALITY AS ttt(value, data_idx)
WHERE jsonb_typeof(t->'value') = 'array' AND
      (jsonb_typeof(t->'value'->0) = 'array' OR
       jsonb_typeof(t->'value'->0) = 'null');

-- name: yesql-add-application-version<!
-- Add application version
INSERT INTO applications (
  form_id,
  key,
  content,
  lang,
  preferred_name,
  last_name,
  hakukohde,
  haku,
  person_oid,
  ssn,
  dob,
  email,
  submitted
) VALUES (
  :form_id,
  :key,
  :content,
  :lang,
  :preferred_name,
  :last_name,
  ARRAY[:hakukohde]::character varying(127)[],
  :haku,
  :person_oid,
  upper(:ssn),
  :dob,
  :email,
  (SELECT created_time
   FROM applications
   WHERE key = :key
   ORDER BY id ASC
   LIMIT 1)
);

-- name: yesql-add-application-secret<!
INSERT INTO application_secrets (application_key, secret) VALUES (:application_key, :secret);

-- name: yesql-get-application-list-by-person-oid-for-omatsivut
SELECT
  a.key       AS oid,
  a.key       AS key,
  las.secret  AS secret,
  ar.state    AS state,
  a.haku      AS haku,
  a.email     AS email,
  a.hakukohde AS hakukohteet,
  a.submitted AS submitted
FROM applications AS a
JOIN application_reviews AS ar
  ON ar.application_key = a.key
LEFT JOIN LATERAL (SELECT secret, age(now(), created_time)
                   FROM application_secrets
                   WHERE application_key = a.key
                   ORDER BY id DESC
                   LIMIT 1) AS las
  ON las.age < (interval '1 day' * :secret_link_valid_days - '1 day')
WHERE a.person_oid = :person_oid AND
      a.haku IS NOT NULL AND
      ar.state <> 'inactivated' AND
      NOT EXISTS (SELECT 1
                  FROM applications AS a2
                  WHERE a2.key = a.key AND
                        a2.id > a.id)
ORDER BY a.created_time DESC;

-- name: yesql-get-application-events
SELECT
  ae.event_type,
  ae.time,
  ae.new_review_state,
  ae.application_key,
  ae.id,
  ae.hakukohde,
  ae.review_key,
  v.first_name,
  v.last_name,
  ae.virkailija_organizations
FROM application_events ae
LEFT JOIN virkailija v ON ae.virkailija_oid = v.oid
WHERE ae.application_key = :application_key
ORDER BY ae.id ASC;

-- name: yesql-get-application-review
SELECT
  id,
  modified_time,
  state,
  score,
  application_key
FROM application_reviews
WHERE application_key = :application_key;

-- name: yesql-get-application-reviews-by-keys
SELECT
  id,
  modified_time,
  state,
  score,
  application_key
FROM application_reviews
WHERE application_key IN (:application_keys);

-- name: yesql-get-application-review-notes
SELECT rn.id, rn.created_time, rn.application_key, rn.notes, rn.hakukohde, rn.state_name, rn.virkailija_organizations, v.first_name, v.last_name
FROM application_review_notes rn
LEFT JOIN virkailija v ON rn.virkailija_oid = v.oid
WHERE rn.application_key = :application_key AND (removed IS NULL OR removed > NOW())
ORDER BY rn.created_time DESC;

-- name: yesql-get-application-review-notes-by-keys
SELECT rn.id, rn.created_time, rn.application_key, rn.notes, rn.hakukohde, rn.state_name, rn.virkailija_organizations, v.first_name, v.last_name
FROM application_review_notes rn
  LEFT JOIN virkailija v ON rn.virkailija_oid = v.oid
WHERE rn.application_key IN (:application_keys) AND (rn.removed IS NULL OR rn.removed > NOW())
ORDER BY rn.created_time DESC;

-- name: yesql-get-applications-by-keys
-- Get list of applications by their keys
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id                           AS form,
  a.created_time,
  (SELECT content
   FROM answers_as_content
   WHERE application_id = a.id) AS content,
  a.person_oid,
  a.hakukohde,
  a.haku,
  ar.state                            AS state,
  f.key                               AS form_key,
  (SELECT json_agg(json_build_object('requirement', requirement,
                                     'state', state,
                                     'hakukohde', hakukohde))
   FROM application_hakukohde_reviews ahr
   WHERE ahr.application_key = a.key) AS application_hakukohde_reviews,
  (SELECT coalesce(array_agg(ae.hakukohde), '{}')
   FROM application_events ae
   WHERE ae.id = (SELECT max(id)
                  FROM application_events
                  WHERE application_key = ae.application_key AND
                        hakukohde = ae.hakukohde AND
                        review_key = ae.review_key) AND
         ae.application_key = a.key AND
         ae.event_type = 'eligibility-state-automatically-changed' AND
         ae.review_key = 'eligibility-state') AS "eligibility-set-automatically"
FROM latest_applications AS a
JOIN application_reviews AS ar ON a.key = ar.application_key
JOIN forms AS f ON a.form_id = f.id
WHERE a.key IN (:application_keys)
ORDER BY a.created_time DESC;

-- name: yesql-get-application-by-id
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  a.submitted,
  (SELECT content
   FROM answers_as_content
   WHERE application_id = a.id) AS content,
  a.haku,
  a.hakukohde,
  a.person_oid,
  las.secret
FROM applications a
JOIN LATERAL (SELECT secret
              FROM application_secrets
              WHERE application_key = a.key
              ORDER BY id DESC
              LIMIT 1) AS las ON true
WHERE a.id = :application_id;

-- name: yesql-has-ssn-applied
SELECT EXISTS (SELECT 1 FROM (SELECT a.id, a.key FROM applications AS a
                              JOIN application_reviews
                              ON application_key = a.key
                              WHERE a.haku = :haku_oid AND
                                    a.ssn = upper(:ssn) AND
                                    state <> 'inactivated') AS t
               WHERE t.id = (SELECT max(id) FROM applications
                             WHERE key = t.key)) AS has_applied;

-- name: yesql-has-email-applied
SELECT EXISTS (SELECT 1 FROM (SELECT a.id, a.key FROM applications AS a
                              JOIN application_reviews
                              ON application_key = a.key
                              WHERE a.haku = :haku_oid AND
                                    a.email = :email AND
                                    state <> 'inactivated') AS t
               WHERE t.id = (SELECT max(id) FROM applications
                             WHERE key = t.key)) AS has_applied;

-- name: yesql-get-latest-application-by-key
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  (SELECT content
   FROM answers_as_content
   WHERE application_id = a.id) AS content,
  a.hakukohde,
  a.haku,
  a.person_oid,
  las.secret,
  (SELECT organization_oid
   FROM forms
   WHERE key = (SELECT key FROM forms WHERE id = a.form_id)
   ORDER BY id DESC
   LIMIT 1) AS organization_oid,
  (SELECT count(*)
   FROM applications AS oa
   LEFT JOIN applications AS la
     ON la.key = oa.key AND la.id > oa.id
   WHERE la.id IS NULL AND
         ((a.ssn IS NOT NULL AND oa.ssn = a.ssn) OR
          (a.email IS NOT NULL AND oa.email = a.email))) AS applications_count,
  (SELECT json_agg(json_build_object('requirement', requirement,
                                     'state', state,
                                     'hakukohde', hakukohde))
   FROM application_hakukohde_reviews ahr
   WHERE ahr.application_key = a.key) AS application_hakukohde_reviews
FROM latest_applications AS a
JOIN latest_application_secrets las ON a.key = las.application_key
WHERE a.key = :application_key;

-- name: yesql-applications-authorization-data
SELECT
  a.haku,
  a.hakukohde,
  (SELECT organization_oid
   FROM forms
   WHERE key = (SELECT key FROM forms WHERE id = a.form_id)
   ORDER BY id DESC
   LIMIT 1) AS organization_oid
FROM applications AS a
LEFT JOIN applications AS la
  ON la.key = a.key AND
     la.id > a.id
WHERE la.id IS NULL AND
      a.key IN (:application_keys);

-- name: yesql-persons-applications-authorization-data
SELECT a.haku,
       a.hakukohde,
       f.organization_oid
FROM applications AS a, forms AS f
WHERE a.person_oid IN (:person_oids) AND
      a.id = (SELECT max(id) FROM applications WHERE key = a.key) AND
      f.id = (SELECT max(id) FROM forms WHERE key = (SELECT key FROM forms WHERE id = a.form_id));

-- name: yesql-get-latest-application-by-secret
SELECT
  a.id,
  a.person_oid,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  (SELECT content
   FROM answers_as_content
   WHERE application_id = a.id) AS content,
  a.haku,
  a.hakukohde,
  f.key     AS form_key
FROM applications AS a
JOIN forms AS f ON a.form_id = f.id
JOIN application_secrets AS las ON las.application_key = a.key
WHERE las.secret = :secret AND
      las.created_time > now() - INTERVAL '1 day' * :secret_link_valid_days AND
      NOT EXISTS (SELECT 1
                  FROM applications AS a2
                  WHERE a2.key = a.key AND
                        a2.id > a.id) AND
      NOT EXISTS (SELECT 1
                  FROM application_secrets AS las2
                  WHERE las2.application_key = las.application_key AND
                        las2.id > las.id);

-- name: yesql-get-latest-application-by-virkailija-secret
SELECT
  a.id,
  a.key,
  a.person_oid,
  a.lang,
  a.form_id AS form,
  a.created_time,
  (SELECT content
   FROM answers_as_content
   WHERE application_id = a.id) AS content,
  a.haku,
  a.hakukohde,
  f.key     AS form_key
FROM latest_applications AS a
JOIN forms f ON a.form_id = f.id
JOIN virkailija_update_secrets AS vus ON vus.application_key = a.key
WHERE vus.secret = :virkailija_secret;

-- name: yesql-get-latest-application-by-virkailija-rewrite-secret
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  (SELECT content
   FROM answers_as_content
   WHERE application_id = a.id) AS content,
  a.haku,
  a.hakukohde,
  f.key     AS form_key
FROM latest_applications AS a
JOIN forms f ON a.form_id = f.id
JOIN virkailija_rewrite_secrets AS vus ON vus.application_key = a.key
WHERE vus.secret = :virkailija_secret;

-- name: yesql-get-latest-version-by-secret-lock-for-update
SELECT a.id,
       a.key,
       a.lang,
       a.form_id AS form,
       a.created_time,
       (SELECT content
        FROM answers_as_content
        WHERE application_id = a.id) AS content,
       a.haku,
       a.hakukohde,
       a.person_oid
FROM applications AS a
WHERE id = (SELECT max(a.id)
            FROM applications AS a
            JOIN application_secrets
              ON application_secrets.application_key = a.key
            LEFT JOIN application_secrets AS las
              ON las.application_key = application_secrets.application_key AND
                 las.id > application_secrets.id
            WHERE las.id IS NULL AND
                  application_secrets.secret = :secret)
FOR UPDATE;

-- name: yesql-get-application-count-for-secret
SELECT count(id) AS count
FROM application_secrets
WHERE secret = :secret;

-- name: yesql-get-application-key-for-any-version-of-secret
SELECT application_key
FROM application_secrets
WHERE secret = :secret
ORDER BY id DESC
LIMIT 1;

-- name: yesql-get-latest-application-language-by-any-version-of-secret
SELECT lang
FROM latest_applications
WHERE key = (SELECT application_key
             FROM application_secrets
             WHERE secret = :secret);

-- name: yesql-get-latest-version-by-virkailija-secret-lock-for-update
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  (SELECT content
   FROM answers_as_content
   WHERE application_id = a.id) AS content,
  a.haku,
  a.hakukohde,
  a.person_oid
FROM applications a
WHERE a.id = (SELECT max(a.id)
              FROM applications AS a
              JOIN virkailija_update_secrets AS vus
                ON vus.application_key = a.key
              WHERE vus.secret = :virkailija_secret)
FOR UPDATE;

-- name: yesql-get-latest-version-by-virkailija-secret-lock-for-rewrite
SELECT
  a.id,
  a.key,
  a.lang,
  a.form_id AS form,
  a.created_time,
  (SELECT content
   FROM answers_as_content
   WHERE application_id = a.id) AS content,
  a.haku,
  a.hakukohde
FROM applications a
WHERE a.id = (SELECT max(a.id)
              FROM applications AS a
              JOIN virkailija_rewrite_secrets AS vus
                ON vus.application_key = a.key
              WHERE vus.secret = :virkailija_secret)
FOR UPDATE;

-- name: yesql-add-application-event<!
-- Add application event
INSERT INTO application_events (application_key, event_type, new_review_state, virkailija_oid, hakukohde, review_key, virkailija_organizations)
VALUES (:application_key, :event_type, :new_review_state, :virkailija_oid, :hakukohde, :review_key, :virkailija_organizations::jsonb);

-- name: yesql-add-application-review!
-- Add application review, initially it doesn't have all fields. This is just a "skeleton"
INSERT INTO application_reviews (application_key, state) VALUES (:application_key, :state);

-- name: yesql-save-attachment-review!
INSERT INTO application_hakukohde_attachment_reviews (application_key, attachment_key, hakukohde, state)
VALUES (:application_key, :attachment_key, :hakukohde, :state)
ON CONFLICT (application_key, attachment_key, hakukohde)
  DO NOTHING;

-- name: yesql-save-application-review!
-- Save modifications for existing review record
UPDATE application_reviews
SET
  score         = :score,
  modified_time = now(),
  state         = :state
WHERE application_key = :application_key;

-- name: yesql-add-person-oid!
-- Add person OID to an application. Update also new versions of application if the user has updated
-- the application while we have been talking to person service (ONR)
UPDATE applications
SET person_oid = :person_oid
WHERE key IN (select key from applications where id = :id)
      AND id >= :id;

-- name: yesql-get-haut-and-hakukohteet-from-applications
SELECT a.haku,
       a.hakukohde,
       max(lf.organization_oid) AS organization_oid,
       max(haku_application_count.n) AS haku_application_count,
       count(*) AS application_count,
       count(*) FILTER (WHERE ahr.state IS NOT DISTINCT FROM 'processed') AS processed,
       count(*) FILTER (WHERE ahr.state IS NOT NULL AND
                              ahr.state NOT IN ('unprocessed', 'processed')) AS processing
FROM (SELECT key, form_id, haku, unnest(hakukohde) AS hakukohde
      FROM latest_applications
      WHERE haku IS NOT NULL) AS a
JOIN forms AS f
  ON f.id = a.form_id
JOIN latest_forms AS lf
  ON lf.key = f.key
JOIN (SELECT haku, count(*) AS n
      FROM latest_applications AS a
      JOIN application_reviews AS ar
        ON ar.application_key = a.key
      WHERE ar.state != 'inactivated'
      GROUP BY haku) AS haku_application_count
  ON haku_application_count.haku = a.haku
JOIN application_reviews AS ar
  ON ar.application_key = a.key
LEFT JOIN application_hakukohde_reviews AS ahr
  ON ahr.application_key = a.key AND
     ahr.hakukohde = a.hakukohde AND
     ahr.requirement = 'processing-state'
WHERE ar.state != 'inactivated'
GROUP BY a.haku, a.hakukohde;

-- name: yesql-get-direct-form-haut
SELECT
  lf.name,
  lf.key,
  lf.organization_oid,
  count(a.key)    AS haku_application_count,
  count(a.key)    AS application_count,
  sum(CASE WHEN ar.state = 'inactivated' OR ahr.state = 'processed'
    THEN 1
      ELSE 0 END) AS processed,
  sum(CASE WHEN ar.state != 'inactivated' AND (ahr.state IS NULL OR ahr.state = 'unprocessed')
    THEN 1
      ELSE 0 END) AS unprocessed
FROM latest_applications AS a
  JOIN forms AS f ON f.id = a.form_id
  JOIN latest_forms AS lf ON lf.key = f.key
  JOIN application_reviews AS ar ON a.key = ar.application_key
  LEFT JOIN application_hakukohde_reviews AS ahr
    ON ahr.application_key = a.key
       AND ahr.hakukohde = 'form'
       AND ahr.requirement = 'processing-state'
WHERE a.haku IS NULL
GROUP BY lf.name, lf.key, lf.organization_oid;

-- name: yesql-add-application-feedback<!
INSERT INTO application_feedback (created_time, form_key, form_id, form_name, stars, feedback, user_agent)
VALUES
  (now(), :form_key, :form_id, :form_name, :rating, left(:feedback, 2000), :user_agent);

-- name: yesql-get-application-hakukohde-reviews
SELECT
  id,
  modified_time,
  requirement,
  state,
  hakukohde,
  application_key
FROM application_hakukohde_reviews
WHERE application_key = :application_key;

-- name: yesql-get-application-attachment-reviews
SELECT
  attachment_key,
  state,
  hakukohde
FROM application_hakukohde_attachment_reviews
WHERE application_key = :application_key;

-- name: yesql-upsert-application-hakukohde-review!
INSERT INTO application_hakukohde_reviews (application_key, requirement, state, hakukohde)
VALUES (:application_key, :requirement, :state, :hakukohde)
ON CONFLICT (application_key, requirement, hakukohde)
  WHERE hakukohde IS NOT NULL
  DO UPDATE SET state = :state;

-- name: yesql-get-existing-application-hakukohde-review
SELECT id
FROM application_hakukohde_reviews
WHERE application_key = :application_key AND requirement = :requirement AND state = :state AND hakukohde = :hakukohde;

-- name: yesql-get-existing-requirement-review
SELECT
  id,
  modified_time,
  requirement,
  state,
  hakukohde,
  application_key
FROM application_hakukohde_reviews
WHERE application_key = :application_key AND requirement = :requirement AND hakukohde = :hakukohde;

-- name: yesql-update-attachment-hakukohde-review!
INSERT INTO application_hakukohde_attachment_reviews (application_key, attachment_key, hakukohde, state)
VALUES (:application_key, :attachment_key, :hakukohde, :state)
ON CONFLICT (application_key, attachment_key, hakukohde)
DO UPDATE SET state = :state, modified_time = now();

-- name: yesql-get-existing-attachment-review
SELECT *
FROM application_hakukohde_attachment_reviews
WHERE application_key = :application_key AND attachment_key = :attachment_key AND hakukohde = :hakukohde;

-- name: yesql-delete-application-attachment-reviews!
DELETE FROM application_hakukohde_attachment_reviews
WHERE application_key = :application_key
      AND NOT EXISTS
      (SELECT 1 FROM jsonb_array_elements(:attachment_key_and_applied_hakukohde_array::jsonb)
                WHERE attachment_key = (value->>0)::text AND hakukohde = (value->>1)::text);

-- name: yesql-applications-by-haku-and-hakukohde-oids
SELECT
  la.key,
  haku,
  person_oid,
  lang,
  preferred_name,
  email,
  ssn,
  hakukohde,
  lf.organization_oid,
  (SELECT content
   FROM answers_as_content
   WHERE application_id = la.id) AS content,
  (SELECT json_agg(json_build_object('requirement', requirement,
                                     'state', state,
                                     'hakukohde', hakukohde))
   FROM application_hakukohde_reviews AS ahr
   WHERE ahr.application_key = la.key) AS application_hakukohde_reviews
FROM latest_applications AS la
JOIN application_reviews as ar ON ar.application_key = la.key
JOIN forms AS f ON la.form_id = f.id
JOIN latest_forms AS lf ON lf.key = f.key
WHERE
  person_oid IS NOT NULL
  AND haku IS NOT NULL
  AND (:haku_oid::text IS NULL OR haku = :haku_oid)
  -- Parameter list contains empty string to avoid empty lists
  AND (array_length(ARRAY[:hakemus_oids], 1) < 2 OR la.key IN (:hakemus_oids))
  AND (array_length(ARRAY[:hakukohde_oids], 1) < 2 OR ARRAY[:hakukohde_oids] && hakukohde)
  AND state <> 'inactivated'
ORDER BY la.created_time DESC;

-- name: yesql-valinta-tulos-service-applications
SELECT a.key AS "oid",
       a.haku AS "haku",
       a.hakukohde AS "hakukohde",
       a.person_oid AS "person-oid",
       coalesce((SELECT CASE value
                            WHEN '1' THEN 'fi'
                            WHEN '2' THEN 'sv'
                            WHEN '3' THEN 'en'
                        END
                 FROM answers
                 WHERE key = 'asiointikieli' AND
                       application_id = a.id),
                a.lang) AS "asiointikieli",
       a.email AS "email"
FROM applications AS a
JOIN application_reviews AS ar ON ar.application_key = a.key
LEFT JOIN applications AS la ON la.key = a.key AND la.id > a.id
WHERE la.id IS NULL AND
      a.person_oid IS NOT NULL AND
      a.haku IS NOT NULL AND
      ar.state <> 'inactivated' AND
      (:haku_oid::text IS NULL OR a.haku = :haku_oid) AND
      (:hakukohde_oid::text IS NULL OR :hakukohde_oid = ANY (a.hakukohde)) AND
      (:hakemus_oids::text[] IS NULL OR a.key = ANY (:hakemus_oids)) AND
      CASE
        WHEN :offset::text IS NULL THEN true
        ELSE a.key > :offset
      END
ORDER BY a.key
LIMIT 1000;

-- name: yesql-valinta-ui-applications
SELECT
  a.key AS oid,
  haku AS haku_oid,
  person_oid AS person_oid,
  hakukohde AS hakukohde,
  (SELECT content->'answers'
   FROM answers_as_content
   WHERE application_id = a.id) AS answers,
  (SELECT value
   FROM answers
   WHERE key = 'address' AND
         application_id = a.id) AS lahiosoite,
  (SELECT value
   FROM answers
   WHERE key = 'postal-code' AND
         application_id = a.id) AS postinumero,
  (SELECT CASE value
              WHEN '1' THEN 'fi'
              WHEN '2' THEN 'sv'
              WHEN '3' THEN 'en'
              END AS value
   FROM answers
   WHERE key = 'asiointikieli' AND
         application_id = a.id) AS asiointikieli,
  (SELECT json_agg(json_build_object('requirement', requirement,
                                     'state', state,
                                     'hakukohde', hakukohde))
   FROM application_hakukohde_reviews AS ahr
   WHERE ahr.application_key = a.key) AS application_hakukohde_reviews
FROM applications AS a
JOIN application_reviews as ar ON ar.application_key = a.key
WHERE
  a.id = (SELECT id FROM latest_applications WHERE key = a.key)
  AND person_oid IS NOT NULL
  AND haku IS NOT NULL
  AND state <> 'inactivated'
  AND (:application_oids::text[] IS NULL OR a.key = ANY (:application_oids))
  AND (:name::text IS NULL OR to_tsvector('unaccent_simple', a.preferred_name || ' ' || a.last_name) @@ to_tsquery('unaccent_simple', :name))
  AND (:haku::text IS NULL OR a.haku = :haku)
  AND (:hakukohde::text IS NULL OR a.hakukohde && :hakukohde)
ORDER BY a.created_time DESC;

--name: yesql-suoritusrekisteri-applications
SELECT
  a.key,
  a.haku,
  a.hakukohde,
  a.person_oid,
  coalesce((SELECT CASE value
                       WHEN '1' THEN 'fi'
                       WHEN '2' THEN 'sv'
                       WHEN '3' THEN 'en'
                   END
            FROM answers
            WHERE key = 'asiointikieli' AND
                  application_id = a.id),
            a.lang) AS "lang",
  a.email,
  a.created_time,
  (SELECT content
   FROM answers_as_content
   WHERE application_id = a.id) AS content,
  coalesce(ahar.attachment_reviews, '{}') AS "attachment_reviews",
  coalesce(ahr.payment_obligations, '{}') AS "payment-obligations",
  coalesce(ahr.eligibilities, '{}') AS eligibilities
FROM applications AS a
JOIN application_reviews AS ar
  ON ar.application_key = a.key
LEFT JOIN applications AS la
  ON la.key = a.key AND la.id > a.id
LEFT JOIN LATERAL (SELECT jsonb_object_agg(hakukohde, state) AS attachment_reviews
                   FROM application_hakukohde_attachment_reviews
                   WHERE application_key = a.key) AS ahar ON true
LEFT JOIN LATERAL (SELECT jsonb_object_agg(hakukohde, state) FILTER (WHERE requirement = 'payment-obligation') AS payment_obligations,
                          jsonb_object_agg(hakukohde, state) FILTER (WHERE requirement = 'eligibility-state') AS eligibilities
                   FROM application_hakukohde_reviews
                   WHERE application_key = a.key) AS ahr ON true
WHERE a.person_oid IS NOT NULL AND
      a.haku IS NOT NULL AND
      (:haku_oid::text IS NULL OR a.haku = :haku_oid) AND
      (:hakukohde_oids::varchar[] IS NULL OR a.hakukohde && :hakukohde_oids) AND
      (:person_oids::text[] IS NULL OR a.person_oid = ANY (:person_oids)) AND
      ar.state <> 'inactivated' AND
      (:modified_after::timestamptz IS NULL OR
         (EXISTS (SELECT 1 FROM applications
                  WHERE created_time > :modified_after AND key = a.key) OR
          EXISTS (SELECT 1 FROM application_reviews
                  WHERE modified_time > :modified_after AND application_key = a.key) OR
          EXISTS (SELECT 1 FROM application_hakukohde_reviews
                  WHERE modified_time > :modified_after AND application_key = a.key))) AND
      la.id IS NULL AND
      CASE
        WHEN :offset::text IS NULL THEN true
        ELSE a.key > :offset
      END
ORDER BY a.key
LIMIT 1000;

--name: yesql-get-applications-by-created-time
SELECT
  a.key,
  a.haku,
  a.hakukohde,
  a.person_oid AS "person-oid",
  (SELECT content
   FROM answers_as_content
   WHERE application_id = a.id) AS content,
  application_reviews.state,
  payment_obligations.states AS "payment-obligations",
  eligibilities.states AS eligibilities
FROM applications AS a
LEFT JOIN applications AS la
  ON la.key = a.key AND
     la.id > a.id
JOIN application_reviews
  ON application_reviews.application_key = a.key
LEFT JOIN LATERAL (SELECT jsonb_object_agg(hakukohde, state) AS states
                   FROM application_hakukohde_reviews AS payment_obligations
                   WHERE payment_obligations.requirement = 'payment-obligation' AND
                         application_key = a.key
                   GROUP BY application_key) AS payment_obligations
  ON true
LEFT JOIN LATERAL (SELECT jsonb_object_agg(hakukohde, state) AS states
                   FROM application_hakukohde_reviews AS payment_obligations
                   WHERE payment_obligations.requirement = 'eligibility-state' AND
                         application_key = a.key
                   GROUP BY application_key) AS eligibilities
  ON true
WHERE a.person_oid IS NOT NULL AND
      la.id IS NULL AND
      (a.created_time > :date::DATE OR
       application_reviews.modified_time > :date::DATE OR
       EXISTS (SELECT 1
               FROM application_hakukohde_reviews
               WHERE application_key = a.key AND
                     modified_time > :date::DATE))
ORDER BY a.created_time DESC
LIMIT :limit
OFFSET :offset;

--name: yesql-onr-applications
SELECT a.key AS key,
       a.haku AS haku,
       f.key AS form,
       a.email AS email,
       (SELECT content
        FROM answers_as_content
        WHERE application_id = a.id) AS content
FROM latest_applications AS a
JOIN forms AS f ON f.id = a.form_id
WHERE a.person_oid = :person_oid
ORDER BY a.created_time DESC;

--name: yesql-add-review-note<!
INSERT INTO application_review_notes (application_key, notes, virkailija_oid, hakukohde, state_name, virkailija_organizations)
VALUES (:application_key, :notes, :virkailija_oid, :hakukohde, :state_name, :virkailija_organizations::jsonb);

-- name: yesql-remove-review-note!
UPDATE application_review_notes SET removed = NOW() WHERE id = :id;

--name: yesql-tilastokeskus-applications
SELECT
  haku AS "haku-oid",
  key AS "hakemus-oid",
  person_oid "henkilo-oid",
  hakukohde AS "hakukohde-oids",
  (SELECT content
   FROM answers_as_content
   WHERE application_id = la.id) AS "content",
  state AS "hakemus-tila"
FROM latest_applications AS la
JOIN application_reviews ON application_key = la.key
WHERE person_oid IS NOT NULL
  AND haku IS NOT NULL
  AND haku = :haku_oid
  AND state <> 'inactivated'
  AND (:hakukohde_oid::TEXT IS NULL OR :hakukohde_oid = ANY (hakukohde))
ORDER BY created_time DESC;

--name: yesql-valintapiste-applications
SELECT
  haku AS "haku-oid",
  key AS "hakemus-oid",
  person_oid "henkilo-oid",
  hakukohde AS "hakukohde-oids",
  (SELECT content
   FROM answers_as_content
   WHERE application_id = la.id) AS "content",
  state AS "hakemus-tila"
FROM latest_applications AS la
JOIN application_reviews ON application_key = la.key
WHERE person_oid IS NOT NULL
  AND haku IS NOT NULL
  AND haku = :haku_oid
  AND state <> 'inactivated'
  AND (:hakukohde_oid::TEXT IS NULL OR :hakukohde_oid = ANY (hakukohde))
ORDER BY created_time DESC;

--name: yesql-valintalaskenta-applications
SELECT
  key,
  lang as asiointikieli,
  person_oid,
  haku,
  hakukohde,
  (SELECT content
   FROM answers_as_content
   WHERE application_id = la.id) AS content,
  (SELECT json_agg(json_build_object('requirement', requirement,
                                     'state', state,
                                     'hakukohde', hakukohde))
   FROM application_hakukohde_reviews AS ahr
   WHERE ahr.application_key = key) AS application_hakukohde_reviews,
  coalesce((SELECT jsonb_object_agg(hakukohde, state)
            FROM application_hakukohde_reviews AS ahr
            WHERE ahr.application_key = key AND
                  ahr.requirement = 'payment-obligation'), '{}') AS maksuvelvollisuus
FROM latest_applications AS la
JOIN application_reviews ON application_key = la.key
WHERE person_oid IS NOT NULL
  AND haku IS NOT NULL
  AND (:hakukohde_oid::TEXT IS NULL OR :hakukohde_oid = ANY (hakukohde))
  AND (array_length(ARRAY[:application_keys], 1) < 2 OR key IN (:application_keys))
  AND state <> 'inactivated';

--name: yesql-siirto-applications
SELECT
  a.key,
  a.person_oid AS "person-oid",
  a.haku,
  a.hakukohde AS hakutoiveet,
  (SELECT content
   FROM answers_as_content
   WHERE application_id = a.id) AS content,
  a.lang,
  lf.organization_oid AS "organization-oid"
FROM latest_applications AS a
JOIN application_reviews AS ar ON ar.application_key = a.key
JOIN forms AS f ON form_id = f.id
JOIN latest_forms AS lf ON lf.key = f.key
WHERE a.person_oid IS NOT NULL
  AND (:hakukohde_oid::TEXT IS NULL OR :hakukohde_oid = ANY (a.hakukohde))
  AND (array_length(ARRAY[:application_keys], 1) < 2 OR a.key IN (:application_keys))
  AND ar.state <> 'inactivated';

--name: yesql-get-latest-application-keys-distinct-by-person-oid
SELECT a.key
FROM applications AS a
LEFT JOIN applications AS la ON la.key = a.key AND la.id > a.id
WHERE la.id IS NULL AND
      a.person_oid = :person_oid;

--name: yesql-get-latest-application-secret
SELECT secret
FROM latest_applications
  JOIN latest_application_secrets ON latest_applications.key = latest_application_secrets.application_key
ORDER BY latest_applications.id DESC
LIMIT 1;

--name: yesql-set-application-hakukohteet-by-secret!
UPDATE applications
SET hakukohde = ARRAY [:hakukohde] :: CHARACTER VARYING(127) [],
    content = :content
WHERE id = (SELECT max(id)
            FROM applications
            WHERE key = (SELECT application_key
                         FROM application_secrets
                         WHERE secret = :secret));

--name: yesql-delete-application-hakukohteet-answer-values-by-secret!
DELETE FROM multi_answer_values
WHERE application_id = (SELECT max(id)
                        FROM applications
                        WHERE key = (SELECT application_key
                                     FROM application_secrets
                                     WHERE secret = :secret)) AND
      key = 'hakukohteet';

--name: yesql-insert-application-hakukohteet-answer-values-by-secret!
INSERT INTO multi_answer_values (application_id, key, data_idx, value)
SELECT (SELECT max(id)
        FROM applications
        WHERE key = (SELECT application_key
                     FROM application_secrets
                     WHERE secret = :secret)),
       'hakukohteet',
       t.data_idx,
       t.value->>0
FROM jsonb_array_elements(:hakukohteet) WITH ORDINALITY AS t(value, data_idx);

--name: yesql-get-application-versions
SELECT (SELECT content
        FROM answers_as_content
        WHERE application_id = a.id) AS content,
        a.form_id
FROM applications AS a
WHERE a.key = :application_key
ORDER BY a.id ASC;

--name: yesql-get-application-ids-for-haku
SELECT la.id AS id
FROM latest_applications la
JOIN application_reviews AS ar ON ar.application_key = la.key
WHERE la.haku = :haku
    AND ar.state <> 'inactivated';