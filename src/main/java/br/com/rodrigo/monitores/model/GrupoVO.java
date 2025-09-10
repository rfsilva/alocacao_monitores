package br.com.rodrigo.monitores.model;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.io.*;
import java.util.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class GrupoVO implements Serializable, Comparable<GrupoVO> {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private String nome;


    @Builder.Default
    @JsonIgnore
    private List<CandidatoVO> candidatos = new ArrayList<>();

    @Override
    public String toString() {
        return nome;
    }

    @Override
    public int compareTo(GrupoVO o) {
        return nome.compareTo(o.getNome());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GrupoVO grupoVO = (GrupoVO) o;
        return Objects.equals(id, grupoVO.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
