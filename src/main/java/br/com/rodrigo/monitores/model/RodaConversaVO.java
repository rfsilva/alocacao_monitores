package br.com.rodrigo.monitores.model;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.io.*;
import java.util.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class RodaConversaVO implements Serializable, Evento, Comparable<Evento> {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private String codigo;
    @JsonIgnore
    private String strSala;

    private String strGrupo;

    @Builder.Default
    private Integer totalMonitores = 2;

    private GrupoVO grupo;
    private SalaVO sala;
    private TurnoVO turno;

    public String getNome() {
        return codigo;
    }

    @Override
    public int compareTo(Evento o) {
        return turno.compareTo(o.getTurno());
    }

    @Override
    public String toString() {
        return codigo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RodaConversaVO that = (RodaConversaVO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
