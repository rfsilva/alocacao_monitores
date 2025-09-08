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
}
