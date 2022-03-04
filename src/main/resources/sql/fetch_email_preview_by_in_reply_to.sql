SELECT EMAIL.MESSAGE_ID, SUBJECT, DATE, LISTAGG(TAG, ',')
FROM EMAIL
LEFT JOIN EMAIL_TAG ON EMAIL.MESSAGE_ID = EMAIL_TAG.MESSAGE_ID
WHERE EMAIL.IN_REPLY_TO = ?
GROUP BY EMAIL.MESSAGE_ID
ORDER BY EMAIL.DATE DESC