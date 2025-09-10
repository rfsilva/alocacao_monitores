package br.com.rodrigo.monitores.model;

import br.com.rodrigo.monitores.util.*;
import com.fasterxml.jackson.annotation.*;
import lombok.*;

import java.io.*;
import java.time.*;
import java.util.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class ProgramacaoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Builder.Default
    @JsonIgnore
    private List<GrupoVO> grupos = new ArrayList<>();
    @Builder.Default
    @JsonIgnore
    private List<SalaVO> salas = new ArrayList<>();
    @Builder.Default
    private List<AtividadeVO> atividades = new ArrayList<>();
    @Builder.Default
    private List<RodaConversaVO> rodasConversa = new ArrayList<>();
    @Builder.Default
    @JsonIgnore
    private List<CandidatoVO> candidatos = new ArrayList<>();
    @Builder.Default
    private List<MonitorAprovadoVO> monitoresAprovados = new ArrayList<>();

    public void atrelarDados() {
        atividades.stream().forEach(a -> a.getSala().getAtividades().add(a));
        rodasConversa.stream().forEach(r -> r.getSala().getRodasConversa().add(r));
        candidatos.stream().forEach(c -> {
            if (c.getInscricaoCursoOficina() != null && !"".equals(c.getInscricaoCursoOficina())) {
                List<String> eventos = ContentUtil.extrairEventos(c.getInscricaoCursoOficina());
                eventos.stream().forEach(e -> {
                    AtividadeVO atividade = ContentUtil.obterAtividade(atividades, e);
                    if (atividade != null) {
                        c.getAtividadesDeInteresse().add(atividade);
                    }
                });
            }
            if (c.getApresentaTrabalho() != null && c.getApresentaTrabalho().equals(NaoSim.SIM)) {
                String strRodasConversa = c.getRodasConversa();
                if (strRodasConversa != null && !"".equals(strRodasConversa)) {
                    List<String> rcs = ContentUtil.extrairRC(strRodasConversa);
                    rcs.stream().forEach(rc -> {
                        RodaConversaVO rodaConversa = ContentUtil.obterRodaConversa(rodasConversa, rc);
                        if (rodaConversa != null) {
                            c.getRodasApresentacao().add(rodaConversa);
                        }
                    });
                }
            }
        });
    }

    public void prepararSalas() {
        Collections.sort(salas);
        for (SalaVO sala : salas) {
            System.out.println("Sala: " + sala.getNome());
            List<Evento> eventos = new ArrayList<>();
            eventos.addAll(sala.getAtividades());
            eventos.addAll(sala.getRodasConversa());
            Collections.sort(eventos);

            for (Evento evento : eventos) {
                System.out.println("\tTurno: " + evento.getTurno() + "; Evento: " + evento.getNome() + "(" + evento.getGrupo().getNome() + "), Total Monitores: " + evento.getTotalMonitores());

                AlocacaoVO alocacao = ContentUtil.obterAlocacao(sala.getAlocacoes(), evento.getTurno());
                if (alocacao == null) {
                    //Nova alocação
                    alocacao = AlocacaoVO.builder()
                            .id(UUID.randomUUID())
                            .sala(sala)
                            .totalMonitores(evento.getTotalMonitores())
                            .turno(TurnoVO.builder()
                                    .dia(evento.getTurno().getDia())
                                    .inicio(evento.getTurno().getInicio())
                                    .fim(evento.getTurno().getFim())
                                    .periodo(evento.getTurno().getPeriodo())
                                    .build())
                            .build();
                    sala.getAlocacoes().add(alocacao);
                } else {
                    //Alocação existente - ajuste de total e intervalo
                    if (alocacao.getTotalMonitores() < evento.getTotalMonitores()) {
                        alocacao.setTotalMonitores(evento.getTotalMonitores());
                    }
                    if (alocacao.getTurno().getInicio().isAfter(evento.getTurno().getInicio())) {
                        alocacao.getTurno().setInicio(evento.getTurno().getInicio());
                    }
                    if (alocacao.getTurno().getFim().isBefore(evento.getTurno().getFim())) {
                        alocacao.getTurno().setFim(evento.getTurno().getFim());
                    }
                }
                alocacao.getEventos().add(evento);
            }

            for (AlocacaoVO alocacao: sala.getAlocacoes()) {
                System.out.println("\tAlocações da sala: ");
                System.out.println("\t\tTurno: " + alocacao.getTurno() + "; Total de Monitores: " + alocacao.getTotalMonitores() + "; Eventos:");
                for (Evento evento : alocacao.getEventos()) {
                    System.out.println("\t\t\tEvento: " + evento.getNome() + "(" + evento.getGrupo().getNome() + ")");
                }
            }
        }
    }

    public void ajustarIndisponibilidadeCandidatos() {
        candidatos.stream().forEach(c -> {
            if (c.getIndisponibilidadeAjustada() != null && !"".equals(c.getIndisponibilidadeAjustada().trim())) {
                c.setTurnosIndisponibilidade(ContentUtil.obterTurnos(c.getIndisponibilidadeAjustada()));
            }
            //Adicionando indisponibilidade das atividades e eventos de interesse - isso será considerado na alocação preferencial
            //e depois sinalizado como indisponíbilidade para não concorrer na alocação
            c.getRodasApresentacao().stream().forEach(r -> c.getTurnosIndisponibilidade().add(r.getTurno()));
            c.getAtividadesDeInteresse().stream().forEach(a -> c.getTurnosIndisponibilidade().add(a.getTurno()));
        });
    }
}
