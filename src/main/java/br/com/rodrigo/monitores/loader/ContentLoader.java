package br.com.rodrigo.monitores.loader;

import br.com.rodrigo.monitores.builder.*;
import br.com.rodrigo.monitores.model.*;
import br.com.rodrigo.monitores.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public final class ContentLoader {

    private ContentLoader() {
    }

    private static String caminhoCandidatos = "input/monitores_candidatos.xlsx";

    private static String caminhoProgramacao = "input/programacao.xlsx";

    private static String caminhoRodasConversa = "input/rodas.xlsx";

    public static void carregarSalasAtividadesGrupos(ProgramacaoVO programacao) {
        try (FileInputStream fis = new FileInputStream(caminhoProgramacao)) {
            Workbook workbook = new XSSFWorkbook(caminhoProgramacao);

            carregarSalasAtividades(workbook, programacao);
            carregarGrupos(workbook, programacao);
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void carregarSalasAtividades(Workbook workbook, ProgramacaoVO programacao) {

        Sheet sheet = workbook.getSheet("Grupos Monitoria Sist");

        Iterator<Row> rowIt = sheet.iterator();
        LocalDate dataAtividade = null;
        String periodoAtividade = "";
        Integer totalMonitoresAtividade = null;
        Integer numeroLinha = 0;
        String strGrupo = "";
        while (rowIt.hasNext()) {
            numeroLinha++;

            Row row = rowIt.next();
            LocalDate data = ContentUtil.getLocalDateValue(row, 0);

            if (data != null) {
                //Linha com a separação da data do evento
                dataAtividade = data;
                strGrupo = "";
                continue;
            }
            if (ContentUtil.isLinhaEmBranco(row)) {
                strGrupo = "";
                continue;
            }
            String conteudoHorario = ContentUtil.getStringValue(row, 0);
            if ("HORÁRIO".equals(conteudoHorario) || "???".equals(conteudoHorario)) {
                strGrupo = "";
                continue;
            }

            String periodo = ContentUtil.getStringValue(row, 0);
            if (periodo != null && !"".equals(periodo)) {
                periodoAtividade = periodo;
            }

            String strSala = ContentUtil.getStringValue(row, 1);
            String grupoAtividade = ContentUtil.getStringValue(row, 3);
            if (grupoAtividade != null && !"".equals(grupoAtividade)) {
                grupoAtividade = grupoAtividade.trim();
                strGrupo = grupoAtividade;
            }
            Integer total = ContentUtil.getIntegerValue(row, 4);
            if (total != null && total > 0) {
                totalMonitoresAtividade = total;
            }
            if (strSala == null || "".equals(strSala)) {
                continue;
            }
            strSala = strSala.trim();
            String nomeAtividade = ContentUtil.getStringValue(row, 2);
            List<TurnoVO> turnos = ContentUtil.obterTurnos(dataAtividade, periodoAtividade);

            System.out.println("Atividade: " + nomeAtividade + ", Grupo: " + strGrupo);
            for (TurnoVO turno : turnos) {
                AtividadeVO atividade = AtividadeVO.builder()
                        .id(UUID.randomUUID())
                        .nome(nomeAtividade)
                        .codigo(ContentUtil.extrairCodigo(nomeAtividade))
                        .strSala(strSala)
                        .strGrupo(strGrupo)
                        .strHorario(conteudoHorario)
                        .sala(ContentUtil.obterSala(programacao.getSalas(), strSala))
                        .grupo(ContentUtil.obterGrupo(programacao.getGrupos(), strGrupo))
                        .totalMonitores(totalMonitoresAtividade)
                        .turno(turno)
                        .build();
                programacao.getAtividades().add(atividade);
                ReportUtil.createCell(row, 5, atividade.getId().toString());
            }
        }
        programacao.setProgramacaoSheet(sheet);
    }

    private static void carregarGrupos(Workbook workbook, ProgramacaoVO programacao) {

        Sheet sheet = workbook.getSheet("Monitores aprovados");

        Iterator<Row> rowIt = sheet.iterator();
        if (rowIt.hasNext()) {
            //Cabeçalho
            rowIt.next();
        }

        while (rowIt.hasNext()) {
            Row row = rowIt.next();

            String email = ContentUtil.getStringValue(row, 0);
            String nome = ContentUtil.getStringValue(row, 1);
            String grupo = ContentUtil.getStringValue(row, 2);
            programacao.getMonitoresAprovados().add(MonitorAprovadoVO.builder()
                    .email(email)
                    .nome(nome)
                    .grupo(grupo)
                    .build());
        }
        Collections.sort(programacao.getMonitoresAprovados());
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
            programacao.setPlanilhasRodasConversa(planilhas);

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
                                .id(UUID.randomUUID())
                                .codigo(codigoRoda)
                                .strSala(strSala)
                                .strGrupo(strGrupo)
                                .grupo(ContentUtil.obterGrupo(programacao.getGrupos(), strGrupo))
                                .sala(ContentUtil.obterSala(programacao.getSalas(), strSala))
                                .turno(turno)
                                .build();
                        programacao.getRodasConversa().add(rodaConversa);
                        Cell cell = row.getCell(6);
                        if (cell == null) {
                            ReportUtil.createCell(row, 6, rodaConversa.getId().toString());
                        } else {
                            cell.setCellValue(rodaConversa.getId().toString());
                        }
                    } else {
                        Cell cell = row.getCell(6);
                        if (cell == null) {
                            ReportUtil.createCell(row, 6, rodas.get(0).getId().toString());
                        } else {
                            cell.setCellValue(rodas.get(0).getId().toString());
                        }
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
                String nomeCandidato = ContentUtil.getStringValue(row, 3);
                if (nomeCandidato != null && !"".equals(nomeCandidato)) {
                    nomeCandidato = nomeCandidato.trim();
                }
                CandidatoVO candidato = CandidatoVO.builder()
                        .id(UUID.randomUUID())
                        .instanteCadastro(ContentUtil.getDateTimeValue(row, 0))
                        .email1(ContentUtil.getStringValue(row, 1))
                        .email2(ContentUtil.getStringValue(row, 2))
                        .nome(nomeCandidato)
                        .status(Status.converter(ContentUtil.getStringValue(row, 4)))
                        .inscricaoCursoOficina(ContentUtil.getStringValue(row, 5))
                        .apresentaTrabalho(NaoSim.converter(ContentUtil.getStringValue(row, 6)))
                        .rodasConversa(ContentUtil.getStringValue(row, 7))
                        .indisponibilidade(ContentUtil.getStringValue(row, 8))
                        .informacaoRelevante(ContentUtil.getStringValue(row, 9))
                        .indisponibilidadeAjustada(ContentUtil.getStringValue(row, 10))
                        .build();
                MonitorAprovadoVO monitorAprovado = ContentUtil.encontrarMonitor(programacao.getMonitoresAprovados(), candidato);
                String strGrupo = "";
                if (monitorAprovado != null) {
                    strGrupo = monitorAprovado.getGrupo();
                }
                candidato.setStrGrupo(strGrupo);
                candidato.setGrupo(ContentUtil.obterGrupo(programacao.getGrupos(), strGrupo));
                if (candidato.getGrupo() == null) {
                    candidato.setGrupo(GrupoVO.builder().nome("").build());
                }
                System.out.println("Candidato: " + nomeCandidato + ", Status: " + candidato.getStatus() + ", Grupo: " + candidato.getGrupo() + ", Grupo: " + strGrupo);
                programacao.getCandidatos().add(candidato);
            }
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
