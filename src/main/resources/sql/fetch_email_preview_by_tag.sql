SELECT EMAIL.MESSAGE_ID, SUBJECT, SENT_FROM, DATE, LISTAGG(TAG, ','), HIDDEN
FROM EMAIL
LEFT JOIN EMAIL_TAG ON EMAIL.MESSAGE_ID = EMAIL_TAG.MESSAGE_ID
WHERE EMAIL_TAG.TAG = ?
GROUP BY EMAIL.MESSAGE_ID