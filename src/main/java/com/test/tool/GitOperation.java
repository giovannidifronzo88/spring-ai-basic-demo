package com.test.tool;

// Operazioni git che richiedono conferma. key(...) produce l'identificativo con cui l'approvazione è legata ai parametri.
enum GitOperation {
    PUSH,
    CREATE_BRANCH,
    MERGE_BRANCH;

    String key(String... params) {
        return name() + ":" + String.join(":", params);
    }
}
