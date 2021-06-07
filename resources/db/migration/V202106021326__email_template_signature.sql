ALTER TABLE email_templates ADD COLUMN signature TEXT;
COMMENT ON COLUMN email_templates.signature IS 'Sähköpostin allekirjoitus';
