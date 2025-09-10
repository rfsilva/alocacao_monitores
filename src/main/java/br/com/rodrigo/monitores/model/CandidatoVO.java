package br.com.rodrigo.monitores.model;

import lombok.*;

import java.io.*;
import java.time.*;
import java.util.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CandidatoVO implements Serializable, Comparable<CandidatoVO> {

    private static final long serialVersionUID = 1L;

    private UUID id;
    private LocalDateTime instanteCadastro;
    private String email1;
    private String email2;
    private String nome;
    private Status status;
    private String inscricaoCursoOficina;
    private NaoSim apresentaTrabalho;
    private String rodasConversa;
    private String indisponibilidade;
    private String informacaoRelevante;
    private String indisponibilidadeAjustada;
    private String strGrupo;

    private GrupoVO grupo;

    @Builder.Default
    private List<AlocacaoVO> alocacoes = new ArrayList<>();

    @Builder.Default
    private List<TurnoVO> turnosDisponibilidade = new ArrayList<>();
    @Builder.Default
    private List<TurnoVO> turnosIndisponibilidade = new ArrayList<>();
    @Builder.Default
    private List<AtividadeVO> atividadesDeInteresse = new ArrayList<>();
    @Builder.Default
    private List<RodaConversaVO> rodasApresentacao = new ArrayList<>();

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Nome: ").append(nome)
                .toString();
    }

    @Override
    public int compareTo(CandidatoVO o) {
        return nome.compareToIgnoreCase(o.getNome());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CandidatoVO that = (CandidatoVO) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
