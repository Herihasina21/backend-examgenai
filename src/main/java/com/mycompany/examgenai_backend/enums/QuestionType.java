package com.mycompany.examgenai_backend.enums;

/**
 * Types de questions partagés par l'API, la BDD et Gemini.
 * Une seule source de vérité — ne pas redéfinir d'enum équivalent dans entity.
 */
public enum QuestionType {
    QCM,
    TRUE_FALSE,
    OPEN,
    FILL_IN_BLANK
}
