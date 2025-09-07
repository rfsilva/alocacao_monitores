package br.com.rodrigo.monitores.model;

import lombok.*;

import java.time.*;

// Classe que representa um turno (intervalo de tempo)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoVO implements Comparable<TurnoVO> {
    private LocalDate dia;
    private LocalTime inicio;
    private LocalTime fim;
    private Periodo periodo;

    public Long obterTempoMinutos() {
        return Duration.between(inicio, fim).toMinutes();
    }

    @Override
    public String toString() {
        return dia + " - " + inicio + " - " + fim + " - " + periodo;
    }

    @Override
    public int compareTo(TurnoVO o) {
        if (dia.compareTo(o.getDia()) != 0) {
            return dia.compareTo(o.getDia());
        }
        return inicio.compareTo(o.getInicio());
    }
}