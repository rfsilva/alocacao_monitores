package br.com.rodrigo.monitores.model;

public enum NaoSim {
    NAO,
    SIM;

    public static NaoSim converter(String input) {
        if ("Sim".equals(input)) {
            return SIM;
        }
        return NAO;
    }
}
