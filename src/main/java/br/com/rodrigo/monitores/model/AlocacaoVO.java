package br.com.rodrigo.monitores.model;

import lombok.*;

import java.io.*;
import java.util.*;

// Classe que representa um turno (intervalo de tempo)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlocacaoVO implements Serializable, Comparable<AlocacaoVO> {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private SalaVO sala;
    private TurnoVO turno;
    private Integer totalMonitores;
    @Builder.Default
    private List<Evento> eventos = new ArrayList<>();
    @Builder.Default
    private List<CandidatoVO> monitores = new ArrayList<>();

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Sala: ").append(sala.getNome())
                .append(", Turno: [").append(turno)
                .append("], Eventos: [").append(eventos)
                .append("], Total Monitores: ").append(totalMonitores)
                .append(", Monitores Alocados: [").append(monitores).append("]");
        return builder.toString();
    }

    @Override
    public int compareTo(AlocacaoVO o) {
        return -turno.compareTo(o.getTurno());
    }

    public List<GrupoVO> getGrupos() {
        return eventos.stream().map(e -> e.getGrupo()).toList();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlocacaoVO that = (AlocacaoVO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}