--name: yesql-get-forms
-- Get stored forms, without content. Use the latest version.
SELECT
  f.id,
  f.key,
  f.name,
  f.organization_oid,
  f.deleted,
  f.created_by,
  f.created_time,
  f.languages,
  f.locked
FROM (SELECT DISTINCT key FROM forms) AS k
JOIN LATERAL (SELECT *
              FROM forms
              WHERE key = k.key
              ORDER BY id DESC
              LIMIT 1) AS f ON true
WHERE (f.deleted IS NULL OR NOT f.deleted)
  AND :hakukohderyhma_oid::varchar IS NULL
  OR f.content->'content' @>
    jsonb_build_array(jsonb_build_object('belongs-to-hakukohderyhma', jsonb_build_array(:hakukohderyhma_oid::varchar)))
ORDER BY f.created_time DESC;

-- name: yesql-add-form<!
-- Add form
INSERT INTO forms (name, content, created_by, key, languages, organization_oid, deleted, locked, locked_by)
VALUES (:name, :content, :created_by, :key, :languages, :organization_oid, :deleted, :locked::timestamp, :locked_by);

-- name: yesql-get-by-id
SELECT
  id,
  key,
  name,
  content,
  created_by,
  created_time,
  languages,
  deleted,
  organization_oid
FROM forms
WHERE id = :id;

-- name: yesql-fetch-latest-version-by-id
SELECT
  f.id,
  f.key,
  f.name,
  f.content,
  f.created_by,
  f.created_time,
  f.languages,
  f.deleted,
  f.organization_oid,
  f.locked,
  (CASE WHEN f.locked_by IS NULL THEN NULL ELSE CONCAT(first_name, ' ', last_name) END) as locked_by,
  (SELECT count(*)
   FROM latest_applications
   WHERE haku IS NULL
     AND form_id IN (SELECT id
                     FROM forms
                     WHERE key = f.key)) AS application_count
FROM latest_forms f
LEFT JOIN virkailija ON f.locked_by = virkailija.oid
WHERE f.key = (SELECT key FROM forms WHERE id = :id);

-- name: yesql-fetch-latest-version-by-key
SELECT
  id,
  key,
  name,
  content,
  created_by,
  created_time,
  languages,
  deleted,
  organization_oid
FROM latest_forms
WHERE key = :key;

-- name: yesql-fetch-latest-version-by-id-lock-for-update
SELECT
  id,
  key,
  name,
  content,
  created_by,
  created_time,
  organization_oid,
  languages,
  deleted
FROM forms
WHERE id = (SELECT max(id)
            FROM forms
            WHERE key = (SELECT key FROM forms WHERE id = :id))
FOR UPDATE;

-- name: yesql-latest-id-by-key
SELECT max(id) AS id
FROM forms
WHERE key = :key;

-- name: yesql-get-latest-version-organization-by-key
SELECT organization_oid
FROM latest_forms
WHERE key = :key;

-- name: yesql-get-latest-version-organization-by-id
SELECT organization_oid
FROM forms f
WHERE id = :id;

-- name: yesql-get-latest-form-by-name
WITH latest_forms AS (
    SELECT
      key,
      MAX(id) AS max_id
    FROM forms
    GROUP BY key
)
SELECT
  f.key
FROM forms f
  JOIN latest_forms lf ON f.id = lf.max_id
WHERE (f.deleted IS NULL OR f.deleted = FALSE)
      AND f.name->>'fi' = :form_name
ORDER BY created_time DESC
LIMIT 1;
