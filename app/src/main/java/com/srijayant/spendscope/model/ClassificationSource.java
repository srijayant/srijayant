package com.srijayant.spendscope.model;

public enum ClassificationSource {
    USER_RULE,
    IDENTITY,
    MESSAGE_TYPE,
    REMARK_HINT,
    EXACT_SEED,
    PREFIX_SEED,
    REGEX_SEED,
    ORDER_CORRELATION,
    PERSON_HEURISTIC,
    EMBEDDING,
    MANUAL,
    UNKNOWN,
    KNOWN_MERCHANT,
    MESSAGE_KEYWORD,
    TRANSACTION_TYPE
}
