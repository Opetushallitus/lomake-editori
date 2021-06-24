-- name: yesql-upsert-email-template!<
INSERT INTO email_templates (form_key, virkailija_oid, lang, subject, content, content_ending, signature, haku_oid)
VALUES (:form_key, :virkailija_oid, :lang, :subject, :content, :content_ending, :signature, :haku_oid)
ON CONFLICT ON CONSTRAINT email_templates_form_key_haku_oid_lang_key
  DO UPDATE SET content = :content, virkailija_oid = :virkailija_oid, content_ending = :content_ending,
                subject = :subject, signature = :signature
RETURNING *;

-- name: yesql-get-email-templates
SELECT *
FROM email_templates
WHERE form_key = :form_key AND haku_oid = :haku_oid;
