package br.com.rodrigo.monitores.model;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.io.*;
import java.util.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AtividadeVO implements Serializable, Evento, Comparable<Evento> {

    private static final long serialVersionUID = 1L;

    private UUID id;
    @JsonIgnore
    private Long sequencia;
    @JsonIgnore
    private String strHorario;
    @JsonIgnore
    private String strSala;
    private String codigo;
    private String nome;
    private String strGrupo;
    private Integer totalMonitores;

    private SalaVO sala;
    private TurnoVO turno;
    private GrupoVO grupo;

    @Override
    public int compareTo(Evento o) {
        return turno.compareTo(o.getTurno());
    }

    @Override
    public String toString() {
        return nome;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtividadeVO that = (AtividadeVO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
