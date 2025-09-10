package br.com.rodrigo.monitores.model;

import java.util.*;

public interface Evento extends Comparable<Evento> {
    UUID getId();
    String getCodigo();
    String getNome();
    Integer getTotalMonitores();
    SalaVO getSala();
    TurnoVO getTurno();
    GrupoVO getGrupo();
}
