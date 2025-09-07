package br.com.rodrigo.monitores.model;

public enum Status {
    CONFIRMED,
    CANCELLED;

    public static Status converter(String input) {
        if ("Confirmo participação.".equals(input)) {
            return CONFIRMED;
        }
        return CANCELLED;
    }
}
