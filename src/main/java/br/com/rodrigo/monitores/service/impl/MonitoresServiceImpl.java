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

    private Boolean respeitarGrupoCandidato;

    public List<AlocacaoDTO> distribuir(Boolean respeitarGrupoCandidato) {
        this.respeitarGrupoCandidato = respeitarGrupoCandidato;
        carregarDados();
        List<AlocacaoVO> alocacoes = alocarMonitores();
        listarCandidatosSemAlocacao();
        ReportBuilder.gerarRelatorios(programacao);
        return AlocacaoMapper.toDtoList(alocacoes);
    }

    private void carregarDados() {
        programacao = new ProgramacaoVO();
        ContentLoader.carregarSalasAtividadesGrupos(programacao);
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
        //Critérios: match entre o candidato apresentador e o evento de apresentação
        List<AlocacaoVO> alocacoesRestantes = alocacoes.stream().filter(a -> a.getTotalMonitores() > a.getMonitores().size()).toList();
        atribuirCandidatosApresentadoresRodasConversa(alocacoesRestantes, candidatos);

        //Passo 2. Alocar os candidatos com base nas atividades de interesse (minicursos)
        //Critérios: match entre o interesse do candidato e o minicurso/oficina de interesse
        alocacoesRestantes = alocacoes.stream().filter(a -> a.getTotalMonitores() > a.getMonitores().size()).toList();
        atribuirCandidatosMinicursosOficinas(alocacoesRestantes, candidatos);

        //Ordenando do evento mais próximo do final - difícil achar candidatos que trabalhem no último dia do evento
        Collections.sort(alocacoes);

        //Passo 3. Priorizar alocação em atividades/rodas/eventos que demandam menos de 8 monitores - distribuindo capacidade
        //Critérios: eventos com menos de 8 monitores, grupo correspondente, disponibilidade do monitor e carga horária limite do monitor
        alocacoesRestantes = alocacoes.stream().filter(a -> a.getTotalMonitores() > a.getMonitores().size()).toList();
        atribuirCandidatosGrupoLimiteCargaHorariaEventosPequenos(alocacoesRestantes, candidatos);

        //Passo 4. Realizar alocação nas demais atividades, respeitando carga horária do monitor
        //Critérios: grupo correspondente, disponibilidade do monitor e carga horária limite do monitor
        alocacoesRestantes = alocacoes.stream().filter(a -> a.getTotalMonitores() > a.getMonitores().size()).toList();
        atribuirCandidatosGrupoLimiteCargaHorariaEventosGrandes(alocacoesRestantes, candidatos);

        //Passo 5. Completando com o restante em função da não-alocação sem limite de atividade
        //Critérios: grupo correspondente e disponibilidade do monitor
        alocacoesRestantes = alocacoes.stream().filter(a -> a.getTotalMonitores() > a.getMonitores().size()).toList();
        atribuirCandidatosGrupoSemLimiteCargaHoraria(alocacoesRestantes, candidatos);

        alocacoesRestantes = alocacoes.stream().filter(a -> a.getTotalMonitores() > a.getMonitores().size()).toList();
        if (!respeitarGrupoCandidato) {
            //Passo 6. É desespero que fala? Bumba-meu-boi, tenta atribuir candidatos nas alocações restantes
            //Critérios: disponibilidade do candidato
            atribuirCandidatosAtividadesRestantes(alocacoesRestantes, candidatos);
        }

        return alocacoes;
    }

    private void listarCandidatosSemAlocacao() {
        programacao.getCandidatos().stream().filter(c -> c.getAlocacoes().size() > 0).forEach(c -> {
            System.out.println("Candidato: " + c.getNome() + ", Status: " + c.getStatus() + ", Alocações: ");
            c.getAlocacoes().stream().forEach(a -> {
                System.out.println("\tAlocação: " + a.getTurno() + ", Sala: " + a.getSala().getNome());
            });
        });

        programacao.getCandidatos().stream().filter(a -> a.getAlocacoes().size() == 0).toList().forEach(c -> {
            System.out.println("Candidato: " + c.getNome() + ", Status: " + c.getStatus());
        });
    }

    private void atribuirCandidatosApresentadoresRodasConversa(List<AlocacaoVO> alocacoes, List<CandidatoVO> candidatos) {

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
                        System.out.println("Monitor '" + c.getNome() + "' (" + c.getGrupo().getNome() + ") alocado em '" + alocacaoRC.getSala().getNome() + "' no turno '" + alocacaoRC.getTurno() + ". Grupos das Atividades: '" + alocacaoRC.getGrupos() + "'");
                        alocacaoRC.getMonitores().add(c);
                        c.getAlocacoes().add(alocacaoRC);
                        c.getTurnosIndisponibilidade().add(alocacaoRC.getTurno());
                    }
                }
            });
        });
    }

    private void atribuirCandidatosMinicursosOficinas(List<AlocacaoVO> alocacoes, List<CandidatoVO> candidatos) {

        //Passo 2. Alocar os candidatos com base nas atividades de interesse (minicursos)
        List<CandidatoVO> candidatosComInteresse = candidatos.stream().filter(c -> c.getAtividadesDeInteresse().size() > 0).toList();
        candidatosComInteresse.stream().forEach(c -> {
            c.getAtividadesDeInteresse().stream().forEach(a -> {
                Optional<AlocacaoVO> alocacaoAtOpt = alocacoes.stream().filter(al -> al.getEventos().contains(a)).findFirst();
                if (alocacaoAtOpt.isPresent()) {
                    AlocacaoVO alocacaoAt = alocacaoAtOpt.get();
                    if (alocacaoAt.getTotalMonitores() > alocacaoAt.getMonitores().size()
                            && !ContentUtil.isConcorrente(c.getAlocacoes().stream().map(at -> at.getTurno()).toList(), alocacaoAt.getTurno())) {
                        System.out.println("Monitor '" + c.getNome() + "' (" + c.getGrupo().getNome() + ") alocado em '" + alocacaoAt.getSala().getNome() + "' no turno '" + alocacaoAt.getTurno() + ". Grupos das Atividades: '" + alocacaoAt.getGrupos() + "'");
                        alocacaoAt.getMonitores().add(c);
                        c.getAlocacoes().add(alocacaoAt);
                        c.getTurnosIndisponibilidade().add(alocacaoAt.getTurno());
                    }
                }
            });
        });
    }

    private void atribuirCandidatosGrupoLimiteCargaHorariaEventosPequenos(List<AlocacaoVO> alocacoes, List<CandidatoVO> candidatos) {

        //Passo 3: garantindo alocação em atividades/rodas/eventos que demandam menos de 8 monitores - distribuindo capacidade
        //Respeitando também o grupo no qual o candidato foi classificado e o grupo de classificação da atividade
        alocacoes.stream().filter(a -> a.getTotalMonitores() < 8).forEach(a -> {
            //Alocações que ainda não possuem o total de monitores
            List<CandidatoVO> candidatosDisponiveis = candidatos.stream().filter(c -> !ContentUtil.estaComCargaHorariaCompleta(c)
                    && a.getGrupos().contains(c.getGrupo())
                    && !ContentUtil.isConcorrente(c.getTurnosIndisponibilidade(), a.getTurno())).toList();
            for (CandidatoVO cand : candidatosDisponiveis) {
                if (a.getTotalMonitores() > a.getMonitores().size()
                        && !ContentUtil.isConcorrente(cand.getAlocacoes().stream().map(at -> at.getTurno()).toList(), a.getTurno())) {
                    System.out.println("Monitor '" + cand.getNome() + "' (" + cand.getGrupo().getNome() + ") alocado em '" + a.getSala().getNome() + "' no turno '" + a.getTurno() + ". Grupos das Atividades: '" + a.getGrupos() + "'");
                    a.getMonitores().add(cand);
                    cand.getAlocacoes().add(a);
                    cand.getTurnosIndisponibilidade().add(a.getTurno());
                } else {
                    break;
                }
            }
        });
    }

    private void atribuirCandidatosGrupoLimiteCargaHorariaEventosGrandes(List<AlocacaoVO> alocacoes, List<CandidatoVO> candidatos) {

        //Passo 4 - alocação nas demais atividades, incluindo grandes eventos
        //Alocação para demais eventos que "sobraram" - respeitando capacidade de trabalho do candidato
        //Respeitando também o grupo no qual o candidato foi classificado e o grupo de classificação da atividade
        alocacoes.stream().forEach(a -> {
            //Alocações que ainda não possuem o total de monitores
            List<CandidatoVO> candidatosDisponiveis = candidatos.stream().filter(c -> !ContentUtil.estaComCargaHorariaCompleta(c)
                    && a.getGrupos().contains(c.getGrupo())
                    && !ContentUtil.isConcorrente(c.getTurnosIndisponibilidade(), a.getTurno())).toList();
            for (CandidatoVO c : candidatosDisponiveis) {
                if (a.getTotalMonitores() > a.getMonitores().size()
                        && !ContentUtil.isConcorrente(c.getAlocacoes().stream().map(at -> at.getTurno()).toList(), a.getTurno())) {
                    System.out.println("Monitor '" + c.getNome() + "' (" + c.getGrupo().getNome() + ") alocado em '" + a.getSala().getNome() + "' no turno '" + a.getTurno() + ". Grupos das Atividades: '" + a.getGrupos() + "'");
                    a.getMonitores().add(c);
                    c.getAlocacoes().add(a);
                    c.getTurnosIndisponibilidade().add(a.getTurno());
                } else {
                    break;
                }
            }
        });
    }

    private void atribuirCandidatosGrupoSemLimiteCargaHoraria(List<AlocacaoVO> alocacoes, List<CandidatoVO> candidatos) {

        //Passo 5. Completando com o restante em função da não-alocação sem limite de atividade
        //Critérios: grupo correspondente e disponibilidade do monitor
        alocacoes.stream().forEach(a -> {
            //Alocações que ainda não possuem o total de monitores
            List<CandidatoVO> candidatosDisponiveis = candidatos.stream().filter(c -> !ContentUtil.isConcorrente(c.getTurnosIndisponibilidade(), a.getTurno())
                    && a.getGrupos().contains(c.getGrupo())).toList();
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
    }

    private void atribuirCandidatosAtividadesRestantes(List<AlocacaoVO> alocacoes, List<CandidatoVO> candidatos) {

        //Passo 6. É desespero que fala? Bumba-meu-boi, tenta atribuir candidatos nas alocações restantes
        //Critérios: disponibilidade do candidato
        alocacoes.stream().forEach(a -> {
            //Alocações que ainda não possuem o total de monitores
            List<CandidatoVO> candidatosDisponiveis = candidatos.stream().filter(c -> !ContentUtil.isConcorrente(c.getTurnosIndisponibilidade(), a.getTurno())).toList();
            for (CandidatoVO cand : candidatosDisponiveis) {
                if (a.getTotalMonitores() > a.getMonitores().size()
                        && !ContentUtil.isConcorrente(cand.getAlocacoes().stream().map(at -> at.getTurno()).toList(), a.getTurno())) {
                    System.out.println("Monitor '" + cand.getNome() + "' alocado em '" + a.getSala().getNome() + "' no turno '" + a.getTurno());
                    a.getMonitores().add(cand);
                    cand.getAlocacoes().add(a);
                    cand.getTurnosIndisponibilidade().add(a.getTurno());
                } else {
                    break;
                }
            }
        });
    }
}
