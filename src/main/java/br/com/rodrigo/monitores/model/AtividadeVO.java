package br.com.rodrigo.monitores.model;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.io.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class AtividadeVO implements Serializable, Evento, Comparable<Evento> {

    private static final long serialVersionUID = 1L;

    @JsonIgnore
    private Long sequencia;
    @JsonIgnore
    private String strHorario;
    @JsonIgnore
    private String strSala;
    private String codigo;
    private String nome;
    private Integer totalMonitores;

    private SalaVO sala;
    private TurnoVO turno;

    @Override
    public int compareTo(Evento o) {
        return turno.compareTo(o.getTurno());
    }

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Código: ").append(codigo)
                .toString();
    }
}
