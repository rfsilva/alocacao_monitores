package br.com.rodrigo.monitores.model;

import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.io.*;
import java.util.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SalaVO implements Serializable, Comparable<SalaVO> {

    private static final long serialVersionUID = 1L;

    private String nome;

    @Builder.Default
    @JsonIgnore
    private List<AtividadeVO> atividades = new ArrayList<>();
    @Builder.Default
    @JsonIgnore
    private List<RodaConversaVO> rodasConversa = new ArrayList<>();
    @Builder.Default
    private List<AlocacaoVO> alocacoes = new ArrayList<>();

    @Override
    public String toString() {
        return nome;
    }

    @Override
    public int compareTo(SalaVO o) {
        return nome.compareTo(o.getNome());
    }
}
