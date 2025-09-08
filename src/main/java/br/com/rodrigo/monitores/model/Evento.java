package br.com.rodrigo.monitores.model;

public interface Evento extends Comparable<Evento> {
    String getCodigo();
    String getNome();
    Integer getTotalMonitores();
    SalaVO getSala();
    TurnoVO getTurno();
    GrupoVO getGrupo();
}
