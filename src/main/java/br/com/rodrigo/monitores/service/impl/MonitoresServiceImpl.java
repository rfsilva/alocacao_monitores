package br.com.rodrigo.monitores.service.impl;

import br.com.rodrigo.monitores.builder.*;
import br.com.rodrigo.monitores.dto.*;
import br.com.rodrigo.monitores.loader.*;
import br.com.rodrigo.monitores.model.*;
import br.com.rodrigo.monitores.service.*;
import br.com.rodrigo.monitores.util.*;
import lombok.extern.slf4j.*;
import org.springframework.stereotype.*;

import java.util.*;

@Service
@Slf4j
public class MonitoresServiceImpl implements MonitoresService {

    private ProgramacaoVO programacao;

    public List<AlocacaoDTO> distribuir() {
        carregarDados();
        List<AlocacaoVO> alocacoes = alocarMonitores();
        listarCandidatosSemAlocacao();
        ReportBuilder.gerarRelatorios(programacao);
        return AlocacaoMapper.toDtoList(alocacoes);
    }

    private void carregarDados() {
        programacao = new ProgramacaoVO();
        ContentLoader.carregarSalasAtividades(programacao);
        ContentLoader.carregarRodasConversa(programacao);
        ContentLoader.carregarCandidatos(programacao);
        programacao.atrelarDados();
        programacao.prepararSalas();
        programacao.ajustarIndisponibilidadeCandidatos();
    }

    private List<AlocacaoVO> alocarMonitores() {
        List<AlocacaoVO> alocacoes = new ArrayList<>();
        programacao.getSalas().stream().forEach(s -> alocacoes.addAll(s.getAlocacoes()));

        List<CandidatoVO> candidatos = new ArrayList<>(programacao.getCandidatos().stream().filter(c -> c.getStatus() == Status.CONFIRMED).toList());
        Collections.shuffle(candidatos);

        //Passo 1. Alocar os candidatos com base nas RCs que vão apresentar
        // ---->>>> Não considerar candidatos que possuem inscrição em mais de uma RC no mesmo horário
        List<CandidatoVO> candidatosComRC = candidatos.stream().filter(c -> c.getRodasApresentacao().size() > 0).toList();
        candidatosComRC.stream().forEach(c -> {
            c.getRodasApresentacao().stream().forEach(r -> {
                Optional<AlocacaoVO> alocacaoRCOpt = alocacoes.stream().filter(a -> a.getEventos().contains(r)).findFirst();
                if (alocacaoRCOpt.isPresent()) {
                    AlocacaoVO alocacaoRC = alocacaoRCOpt.get();
                    if (alocacaoRC.getTotalMonitores() > alocacaoRC.getMonitores().size()
                            && !ContentUtil.isConcorrente(c.getAlocacoes().stream().map(a -> a.getTurno()).toList(), alocacaoRC.getTurno())) {
                        System.out.println("Monitor '" + c.getNome() + "' alocado em '" + alocacaoRC.getSala().getNome() + "' no turno '" + alocacaoRC.getTurno());
                        alocacaoRC.getMonitores().add(c);
                        c.getAlocacoes().add(alocacaoRC);
                        c.getTurnosIndisponibilidade().add(alocacaoRC.getTurno());
                    }
                }
            });
        });

        //Passo 2. Alocar os candidatos com base nas atividades de interesse (minicursos)
        List<CandidatoVO> candidatosComInteresse = candidatos.stream().filter(c -> c.getAtividadesDeInteresse().size() > 0).toList();
        candidatosComInteresse.stream().forEach(c -> {
            c.getAtividadesDeInteresse().stream().forEach(a -> {
                Optional<AlocacaoVO> alocacaoAtOpt = alocacoes.stream().filter(al -> al.getEventos().contains(a)).findFirst();
                if (alocacaoAtOpt.isPresent()) {
                    AlocacaoVO alocacaoAt = alocacaoAtOpt.get();
                    if (alocacaoAt.getTotalMonitores() > alocacaoAt.getMonitores().size()
                            && !ContentUtil.isConcorrente(c.getAlocacoes().stream().map(at -> at.getTurno()).toList(), alocacaoAt.getTurno())) {
                        System.out.println("Monitor '" + c.getNome() + "' alocado em '" + alocacaoAt.getSala().getNome() + "' no turno '" + alocacaoAt.getTurno());
                        alocacaoAt.getMonitores().add(c);
                        c.getAlocacoes().add(alocacaoAt);
                        c.getTurnosIndisponibilidade().add(alocacaoAt.getTurno());
                    }
                }
            });
        });

        Collections.sort(alocacoes);
        //Demais eventos - prioridade para alocações próximas do fim do evento, respeitando a alocação limite do candidato (8h)
        //Passo 3: garantindo alocação em atividades/rodas/eventos que demandam menos de 8 monitores - distribuindo capacidade
        alocacoes.stream().filter(a -> (a.getTotalMonitores() > a.getMonitores().size()
                        && a.getTotalMonitores() < 8)).forEach(a -> {
            //Alocações que ainda não possuem o total de monitores
            List<CandidatoVO> candidatosDisponiveis = candidatos.stream().filter(c -> !ContentUtil.estaComCargaHorariaCompleta(c)
                    && !ContentUtil.isConcorrente(c.getTurnosIndisponibilidade(), a.getTurno())).toList();
            for (CandidatoVO cand : candidatosDisponiveis) {
                if (a.getTotalMonitores() > a.getMonitores().size()
                        && !ContentUtil.isConcorrente(cand.getAlocacoes().stream().map(at -> at.getTurno()).toList(), a.getTurno())) {
                    System.out.println("Monitor '" + cand.getNome() + "' alocado em '" + a.getSala().getNome() + "' no turno '" + a.getTurno());
                    a.getMonitores().add(cand);
                    cand.getAlocacoes().add(a);
                } else {
                    break;
                }
            }
        });

        //Alocação para demais eventos que "sobraram" - respeitando capacidade de trabalho do candidato
        alocacoes.stream().filter(a -> a.getTotalMonitores() > a.getMonitores().size()).forEach(a -> {
            //Alocações que ainda não possuem o total de monitores
            List<CandidatoVO> candidatosDisponiveis = candidatos.stream().filter(c -> !ContentUtil.estaComCargaHorariaCompleta(c)
                            && !ContentUtil.isConcorrente(c.getTurnosIndisponibilidade(), a.getTurno())).toList();
            for (CandidatoVO cand : candidatosDisponiveis) {
                if (a.getTotalMonitores() > a.getMonitores().size()
                        && !ContentUtil.isConcorrente(cand.getAlocacoes().stream().map(at -> at.getTurno()).toList(), a.getTurno())) {
                    System.out.println("Monitor '" + cand.getNome() + "' alocado em '" + a.getSala().getNome() + "' no turno '" + a.getTurno());
                    a.getMonitores().add(cand);
                    cand.getAlocacoes().add(a);
                } else {
                    break;
                }
            }
        });

        //Completando com o restante em função da não-alocação sem limite de atividade
        alocacoes.stream().filter(a -> a.getTotalMonitores() > a.getMonitores().size()).forEach(a -> {
            //Alocações que ainda não possuem o total de monitores
            List<CandidatoVO> candidatosDisponiveis = candidatos.stream().filter(c -> !ContentUtil.isConcorrente(c.getTurnosIndisponibilidade(), a.getTurno())).toList();
            for (CandidatoVO cand : candidatosDisponiveis) {
                if (a.getTotalMonitores() > a.getMonitores().size()
                        && !ContentUtil.isConcorrente(cand.getAlocacoes().stream().map(at -> at.getTurno()).toList(), a.getTurno())) {
                    System.out.println("Monitor '" + cand.getNome() + "' alocado em '" + a.getSala().getNome() + "' no turno '" + a.getTurno());
                    a.getMonitores().add(cand);
                    cand.getAlocacoes().add(a);
                } else {
                    break;
                }
            }
        });

        return alocacoes;
    }

    private void listarCandidatosSemAlocacao() {
        programacao.getCandidatos().stream().forEach(c -> {
            System.out.println("Candidato: " + c.getNome() + ", Status: " + c.getStatus() + ", Alocações: ");
            c.getAlocacoes().stream().forEach(a -> {
                System.out.println("\tAlocação: " + a.getTurno() + ", Sala: " + a.getSala().getNome());
            });
        });

        programacao.getCandidatos().stream().filter(a -> a.getAlocacoes().size() == 0).toList().forEach(c -> {
            System.out.println("Candidato: " + c.getNome() + ", Status: " + c.getStatus());
        });
    }
}
