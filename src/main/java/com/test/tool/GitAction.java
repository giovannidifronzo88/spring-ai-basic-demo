package com.test.tool;

// Operazione git da eseguire solo dopo l'approvazione dell'utente.
@FunctionalInterface
interface GitAction {
    String run() throws Exception;
}
