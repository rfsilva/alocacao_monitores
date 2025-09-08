package br.com.rodrigo.monitores.loader;

import br.com.rodrigo.monitores.model.*;
import br.com.rodrigo.monitores.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.time.*;
import java.util.*;

public final class ContentLoader {

    private ContentLoader() {
    }

    private static String caminhoCandidatos = "input/monitores_candidatos.xlsx";

    private static String caminhoProgramacao = "input/programacao.xlsx";

    private static String caminhoRodasConversa = "input/rodas.xlsx";

    public static void carregarSalasAtividades(ProgramacaoVO programacao) {
        try (FileInputStream fis = new FileInputStream(caminhoProgramacao)) {
            Workbook workbook = new XSSFWorkbook(caminhoProgramacao);
            Sheet sheet = workbook.getSheet("Grupos Monitoria Sist");

            Iterator<Row> rowIt = sheet.iterator();
            LocalDate dataAtividade = null;
            String periodoAtividade = "";
            Integer totalMonitoresAtividade = null;
            Integer numeroLinha = 0;
            while (rowIt.hasNext()) {
                numeroLinha++;

                Row row = rowIt.next();
                LocalDate data = ContentUtil.getLocalDateValue(row, 0);

                if (data != null) {
                    //Linha com a separação da data do evento
                    dataAtividade = data;
                    continue;
                }
                if (ContentUtil.isLinhaEmBranco(row)) {
                    continue;
                }
                String conteudoHorario = ContentUtil.getStringValue(row, 0);
                if ("HORÁRIO".equals(conteudoHorario) || "???".equals(conteudoHorario)) {
                    continue;
                }

                String periodo = ContentUtil.getStringValue(row, 0);
                if (periodo != null && !"".equals(periodo)) {
                    periodoAtividade = periodo;
                }

                String sala = ContentUtil.getStringValue(row, 1);
                String grupo = ContentUtil.getStringValue(row, 3);
                Integer total = ContentUtil.getIntegerValue(row, 4);
                if (total != null && total > 0) {
                    totalMonitoresAtividade = total;
                }
                if (sala == null || "".equals(sala)) {
                    continue;
                }
                sala = sala.trim();
                String nomeAtividade = ContentUtil.getStringValue(row, 2);
                List<TurnoVO> turnos = ContentUtil.obterTurnos(dataAtividade, periodoAtividade);

                for (TurnoVO turno : turnos) {
                    AtividadeVO atividade = AtividadeVO.builder()
                            .nome(nomeAtividade)
                            .codigo(ContentUtil.extrairCodigo(nomeAtividade))
                            .strSala(sala)
                            .strGrupo(grupo)
                            .strHorario(conteudoHorario)
                            .sala(ContentUtil.obterSala(programacao.getSalas(), sala))
                            .grupo(ContentUtil.obterGrupo(programacao.getGrupos(), grupo))
                            .totalMonitores(totalMonitoresAtividade)
                            .turno(turno)
                            .build();
                    programacao.getAtividades().add(atividade);
                }
            }
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void carregarRodasConversa(ProgramacaoVO programacao) {
        try (FileInputStream fis = new FileInputStream(caminhoProgramacao)) {

            Workbook workbook = new XSSFWorkbook(caminhoRodasConversa);
            Map<TurnoVO, Sheet> planilhas = new LinkedHashMap<>();
            TurnoVO turno1 = TurnoVO.builder().dia(LocalDate.of(2025, Month.SEPTEMBER, 12))
                    .inicio(LocalTime.of(8, 0))
                    .fim(LocalTime.of(10, 0))
                    .build();
            turno1.setPeriodo(ContentUtil.classificarPeriodo(turno1));
            planilhas.put(turno1, workbook.getSheet("RC 1-18"));
            TurnoVO turno2 = TurnoVO.builder().dia(LocalDate.of(2025, Month.SEPTEMBER, 13))
                    .inicio(LocalTime.of(8, 0))
                    .fim(LocalTime.of(10, 0))
                    .build();
            turno2.setPeriodo(ContentUtil.classificarPeriodo(turno2));
            planilhas.put(turno2, workbook.getSheet("RC 19-36"));
            TurnoVO turno3 = TurnoVO.builder().dia(LocalDate.of(2025, Month.SEPTEMBER, 13))
                    .inicio(LocalTime.of(14, 0))
                    .fim(LocalTime.of(16, 0))
                    .build();
            turno3.setPeriodo(ContentUtil.classificarPeriodo(turno3));
            planilhas.put(turno3, workbook.getSheet("RC 37-43"));

            for (TurnoVO turno : planilhas.keySet()) {
                Sheet sheet = planilhas.get(turno);

                Iterator<Row> rowIt = sheet.iterator();
                Integer numeroLinha = 0;
                while (rowIt.hasNext()) {
                    numeroLinha++;
                    Row row = rowIt.next();

                    String codigoRoda = ContentUtil.getStringValue(row, 0);
                    if (codigoRoda == null || "".equals(codigoRoda)
                            || "Roda de Conversa".equalsIgnoreCase(codigoRoda)) {
                        continue;
                    }

                    String strSala = ContentUtil.getStringValue(row, 1);
                    if (strSala != null && !"".equals(strSala)) {
                        strSala = strSala.trim();
                    }
                    String strGrupo = "Grupo 02";
                    List<RodaConversaVO> rodas = programacao.getRodasConversa().stream().filter(r -> r.getCodigo().equals(codigoRoda)).toList();
                    if (rodas.size() == 0) {
                        RodaConversaVO rodaConversa = RodaConversaVO.builder()
                                .codigo(codigoRoda)
                                .strSala(strSala)
                                .strGrupo(strGrupo)
                                .grupo(ContentUtil.obterGrupo(programacao.getGrupos(), strGrupo))
                                .sala(ContentUtil.obterSala(programacao.getSalas(), strSala))
                                .turno(turno)
                                .build();
                        programacao.getRodasConversa().add(rodaConversa);
                    }
                }
            }

            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void carregarCandidatos(ProgramacaoVO programacao) {
        try (FileInputStream fis = new FileInputStream(caminhoCandidatos)) {
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            //Eliminando cabeçalho
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                String strGrupo = ContentUtil.getStringValue(row, 11); //TODO confirmar
                CandidatoVO candidato = CandidatoVO.builder()
                        .instanteCadastro(ContentUtil.getDateTimeValue(row, 0))
                        .email(ContentUtil.getStringValue(row, 1))
                        .nome(ContentUtil.getStringValue(row, 3))
                        .status(Status.converter(ContentUtil.getStringValue(row, 4)))
                        .inscricaoCursoOficina(ContentUtil.getStringValue(row, 5))
                        .apresentaTrabalho(NaoSim.converter(ContentUtil.getStringValue(row, 6)))
                        .rodasConversa(ContentUtil.getStringValue(row, 7))
                        .indisponibilidade(ContentUtil.getStringValue(row, 8))
                        .informacaoRelevante(ContentUtil.getStringValue(row, 9))
                        .indisponibilidadeAjustada(ContentUtil.getStringValue(row, 10))
                        .strGrupo(strGrupo)
                        .grupo(ContentUtil.obterGrupo(programacao.getGrupos(), strGrupo))
                        .build();
                programacao.getCandidatos().add(candidato);
            }
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
