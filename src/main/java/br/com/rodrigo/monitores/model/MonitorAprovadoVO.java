package br.com.rodrigo.monitores.model;

import lombok.*;

import java.time.*;
import java.util.*;

// Classe que representa um turno (intervalo de tempo)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitorAprovadoVO implements Comparable<MonitorAprovadoVO> {
    private UUID id;
    private String email;
    private String nome;
    private String grupo;

    @Override
    public String toString() {
        return email + " - " + nome + " - " + grupo;
    }

    @Override
    public int compareTo(MonitorAprovadoVO o) {
       return email.compareToIgnoreCase(o.getEmail());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MonitorAprovadoVO that = (MonitorAprovadoVO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}