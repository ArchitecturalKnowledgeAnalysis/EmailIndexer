SELECT
    TAG.ID,
    TAG.NAME,
    TAG.DESCRIPTION,
    TAG.SEQ
FROM TAG
LEFT JOIN EMAIL_TAG ET ON TAG.ID = ET.TAG_ID
WHERE ET.EMAIL_ID = ?
ORDER BY TAG.SEQ
