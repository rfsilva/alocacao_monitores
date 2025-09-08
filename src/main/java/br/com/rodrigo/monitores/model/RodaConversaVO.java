package br.com.rodrigo.monitores.model;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.io.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class RodaConversaVO implements Serializable, Evento, Comparable<Evento> {

    private static final long serialVersionUID = 1L;

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
}
