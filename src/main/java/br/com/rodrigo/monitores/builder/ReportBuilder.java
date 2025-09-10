package br.com.rodrigo.monitores.builder;

import br.com.rodrigo.monitores.model.*;
import br.com.rodrigo.monitores.model.comparator.*;
import br.com.rodrigo.monitores.util.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

public final class ReportBuilder {

    private ReportBuilder() {
    }

    private static final String CAMINHO_RELATORIO = "output/alocacoes";

    public static void gerarRelatorios(ProgramacaoVO programacao) {
        XSSFWorkbook wb = new XSSFWorkbook();

        Sheet salasSheet = criarAbaAlocacaoPorSala(wb, programacao);
        Sheet monitoresSheet = criarAbaAlocacaoPorMonitor(wb, programacao);
        Sheet candidatosSemAlocacao = criarAbaMonitoresSemAlocacao(wb, programacao);
        Sheet monitoresNaoConsiderados = criarAbaMonitorNaoConsiderado(wb, programacao);
        Sheet programacaoOriginalSheet = criarAbaProgramacaoOriginal(wb, programacao);
        List<Sheet> rodasConversaSheetList = criarAbasProgramacaoRodasConversa(wb, programacao);

        try {
            ReportUtil.salvarExcel(wb, CAMINHO_RELATORIO + LocalDateTime.now().format(DateTimeFormatter.ofPattern("_ddMMyyyy_HHmmss")) + ".xlsx");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Sheet criarAbaAlocacaoPorSala(Workbook wb, ProgramacaoVO programacao) {
        Sheet sheet = wb.createSheet("Alocação por Sala");

        // --- Estilo padrão (bordas finas pretas) ---
        CellStyle estiloBorda = wb.createCellStyle();
        estiloBorda.setBorderTop(BorderStyle.THIN);
        estiloBorda.setBorderBottom(BorderStyle.THIN);
        estiloBorda.setBorderLeft(BorderStyle.THIN);
        estiloBorda.setBorderRight(BorderStyle.THIN);
        estiloBorda.setTopBorderColor(IndexedColors.BLACK.getIndex());
        estiloBorda.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        estiloBorda.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        estiloBorda.setRightBorderColor(IndexedColors.BLACK.getIndex());

        CellStyle estiloBordaQuebraLinha = wb.createCellStyle();
        estiloBordaQuebraLinha.cloneStyleFrom(estiloBorda);
        estiloBordaQuebraLinha.setWrapText(true);

        // --- Estilo cabeçalho (bordas + fundo + negrito) ---
        CellStyle estiloCabecalho = wb.createCellStyle();
        estiloCabecalho.cloneStyleFrom(estiloBorda);
        estiloCabecalho.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estiloCabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font fonteCab = wb.createFont();
        fonteCab.setBold(true);
        estiloCabecalho.setFont(fonteCab);

        // Cabeçalho
        Row header = sheet.createRow(0);
        Cell cellComentario = ReportUtil.createCell(header, 0, "Sala", estiloCabecalho);
        ReportUtil.createCell(header, 1, "Período", estiloCabecalho);
        ReportUtil.createCell(header, 2, "Atividade(s)", estiloCabecalho);
        ReportUtil.createCell(header, 3, "Total - Ideal (Alocado)", estiloCabecalho);
        ReportUtil.createCell(header, 4, "Monitores Selecionados", estiloCabecalho);

        String comentario = "Lista de alocação de monitores (aprovados e confirmados) por sala / período.";
        adicionarComentario(wb, sheet, cellComentario, comentario);

        int rowIdx = 1;
        for (SalaVO sala : programacao.getSalas()) {
            int salaStartRow = rowIdx;

            for (AlocacaoVO alocacao : sala.getAlocacoes()) {
                int alocacaoStartRow = rowIdx;

                // Se não houver candidatos, cria pelo menos uma linha
                List<CandidatoVO> candidatos = alocacao.getMonitores().isEmpty()
                        ? Collections.singletonList(CandidatoVO.builder().nome("").build())
                        : alocacao.getMonitores();

                Collections.sort(candidatos);
                for (CandidatoVO candidato : candidatos) {
                    Row row = sheet.createRow(rowIdx++);
                    String content = "";
                    if (candidato.getGrupo() != null) {
                        content = candidato.getNome() + " (" + candidato.getGrupo().getNome() + ")";
                    }
                    ReportUtil.createCell(row, 4, content, estiloBorda);
                }

                int alocacaoEndRow = rowIdx - 1;

                //Mescla alocacao
                if (alocacao.getMonitores().size() > 1) {
                    ReportUtil.applyMergedRegionWithBorders(sheet, alocacaoStartRow, alocacaoEndRow, 1, 1, estiloBorda);
                    ReportUtil.applyMergedRegionWithBorders(sheet, alocacaoStartRow, alocacaoEndRow, 2, 2, estiloBordaQuebraLinha);
                    ReportUtil.applyMergedRegionWithBorders(sheet, alocacaoStartRow, alocacaoEndRow, 3, 3, estiloBorda);
                }

                Row firstAlocacaoRow = sheet.getRow(alocacaoStartRow);
                ReportUtil.createCell(firstAlocacaoRow, 1, alocacao.getTurno().toString(), estiloBorda);
                ReportUtil.createCell(firstAlocacaoRow, 2, ContentUtil.toStringList(alocacao.getEventos().stream().map(e -> (e.toString() + " (" + e.getGrupo().getNome() + ")")).toList()), estiloBorda);
                ReportUtil.createCell(firstAlocacaoRow, 3, alocacao.getTotalMonitores() + " (" + alocacao.getMonitores().size() + ")", estiloBorda);
                firstAlocacaoRow.setHeight((short) -1);
            }

            int salaEndRow = rowIdx - 1;

            //Mescla sala
            if (salaStartRow < salaEndRow) {
                ReportUtil.applyMergedRegionWithBorders(sheet, salaStartRow, salaEndRow, 0, 0, estiloBorda);
            }

            Row firstSalaRow = sheet.getRow(salaStartRow);
            ReportUtil.createCell(firstSalaRow, 0, sala.getNome(), estiloBorda);
        }

        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }
        return sheet;
    }

    private static Sheet criarAbaAlocacaoPorMonitor(Workbook wb, ProgramacaoVO programacao) {

        // Mapa aluno -> lista de (sala, turno, professor)
        Map<String, List<String[]>> monitorMap = new LinkedHashMap<>();

        Collections.sort(programacao.getCandidatos());
        List<CandidatoVO> monitores = programacao.getCandidatos().stream().filter(m -> m.getStatus() == Status.CONFIRMED).toList();
        for (CandidatoVO monitor : monitores) {
            List<AlocacaoVO> alocacoes = monitor.getAlocacoes();
            Collections.sort(alocacoes, new AlocacaoComparator());
            for (AlocacaoVO alocacao : alocacoes) {
                List<String[]> list = monitorMap.get(monitor.getNome());
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(new String[]{alocacao.getSala().getNome(), alocacao.getTurno().toString(), ContentUtil.toStringList(alocacao.getEventos().stream().map(e -> (e.toString() + " (" + e.getGrupo().getNome() + ")")).toList())});
                monitorMap.put(monitor.getNome(), list);
            }
        }

        Sheet sheet = wb.createSheet("Alocação por Monitor");

        // --- Estilo padrão (bordas finas pretas) ---
        CellStyle estiloBorda = wb.createCellStyle();
        estiloBorda.setBorderTop(BorderStyle.THIN);
        estiloBorda.setBorderBottom(BorderStyle.THIN);
        estiloBorda.setBorderLeft(BorderStyle.THIN);
        estiloBorda.setBorderRight(BorderStyle.THIN);
        estiloBorda.setTopBorderColor(IndexedColors.BLACK.getIndex());
        estiloBorda.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        estiloBorda.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        estiloBorda.setRightBorderColor(IndexedColors.BLACK.getIndex());

        CellStyle estiloBordaComQuebra = wb.createCellStyle();
        estiloBordaComQuebra.cloneStyleFrom(estiloBorda);
        estiloBordaComQuebra.setWrapText(true);

        // --- Estilo cabeçalho ---
        CellStyle estiloCabecalho = wb.createCellStyle();
        estiloCabecalho.cloneStyleFrom(estiloBorda); // mantém as bordas
        estiloCabecalho.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estiloCabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font fonteCabecalho = wb.createFont();
        fonteCabecalho.setBold(true);
        estiloCabecalho.setFont(fonteCabecalho);

        // Estilo cabeçalho
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Cabeçalho
        Row header = sheet.createRow(0);
        Cell cellComentario = ReportUtil.createCell(header, 0, "Monitor", estiloCabecalho);
        ReportUtil.createCell(header, 1, "Grupo", estiloCabecalho);
        ReportUtil.createCell(header, 2, "Carga Horária", estiloCabecalho);
        ReportUtil.createCell(header, 3, "Sala", estiloCabecalho);
        ReportUtil.createCell(header, 4, "Período", estiloCabecalho);
        ReportUtil.createCell(header, 5, "Atividade(s)", estiloCabecalho);

        String comentario = "Lista de alocação de monitores (aprovados e confirmados) por nome de monitor.";
        adicionarComentario(wb, sheet, cellComentario, comentario);

        int rowIdx = 1;
        for (Map.Entry<String, List<String[]>> entry : monitorMap.entrySet()) {
            String nomeMonitor = entry.getKey();
            CandidatoVO monitor = null;
            Optional<CandidatoVO> monitorOpt = monitores.stream().filter(c -> c.getNome().equals(nomeMonitor)).findFirst();
            if (monitorOpt.isPresent()) {
                monitor = monitorOpt.get();
            }
            List<String[]> participacoes = entry.getValue();

            int monitorStartRow = rowIdx;
            for (String[] info : participacoes) {
                Row row = sheet.createRow(rowIdx++);
                ReportUtil.createCell(row, 3, info[0], estiloBorda); //Sala
                ReportUtil.createCell(row, 4, info[1], estiloBorda); //Turno
                ReportUtil.createCell(row, 5, info[2], estiloBorda); //Atividade
                row.setHeight((short) -1);
            }

            int monitorEndRow = rowIdx - 1;

            //Mesclar células do monitor se tiver mais de um turno
            if (participacoes.size() > 1) {
                ReportUtil.applyMergedRegionWithBorders(sheet, monitorStartRow, monitorEndRow, 0, 0, estiloBorda); //Monitor
                ReportUtil.applyMergedRegionWithBorders(sheet, monitorStartRow, monitorEndRow, 1, 1, estiloBorda); //Grupo
                ReportUtil.applyMergedRegionWithBorders(sheet, monitorStartRow, monitorEndRow, 2, 2, estiloBorda); //Carga horária
            }

            Row firstRow = sheet.getRow(monitorStartRow);
            ReportUtil.createCell(firstRow, 0, monitor.getNome(), estiloBorda);
            ReportUtil.createCell(firstRow, 1, monitor.getGrupo().getNome(), estiloBorda);
            ReportUtil.createCell(firstRow, 2, obterCargaHoraria(monitor), estiloBorda);
        }

        // Ajustar colunas
        for (int i = 0; i < 6; i++) {
            sheet.autoSizeColumn(i);
        }
        return sheet;
    }

    private static Sheet criarAbaMonitoresSemAlocacao(Workbook wb, ProgramacaoVO programacao) {

        Sheet sheet = wb.createSheet("Monitores sem Atribuição");

        // --- Estilo padrão (bordas finas pretas) ---
        CellStyle estiloBorda = wb.createCellStyle();
        estiloBorda.setBorderTop(BorderStyle.THIN);
        estiloBorda.setBorderBottom(BorderStyle.THIN);
        estiloBorda.setBorderLeft(BorderStyle.THIN);
        estiloBorda.setBorderRight(BorderStyle.THIN);
        estiloBorda.setTopBorderColor(IndexedColors.BLACK.getIndex());
        estiloBorda.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        estiloBorda.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        estiloBorda.setRightBorderColor(IndexedColors.BLACK.getIndex());

        // --- Estilo cabeçalho ---
        CellStyle estiloCabecalho = wb.createCellStyle();
        estiloCabecalho.cloneStyleFrom(estiloBorda); // mantém as bordas
        estiloCabecalho.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estiloCabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font fonteCabecalho = wb.createFont();
        fonteCabecalho.setBold(true);
        estiloCabecalho.setFont(fonteCabecalho);

        // Estilo cabeçalho
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        CellStyle estiloDataHora = wb.createCellStyle();
        estiloDataHora.cloneStyleFrom(estiloBorda);
        CreationHelper createHelper = wb.getCreationHelper();
        estiloDataHora.setDataFormat(
                createHelper.createDataFormat().getFormat("dd/MM/yyyy HH:mm")
        );

        // Cabeçalho
        Row header = sheet.createRow(0);
        Cell cellComentario = ReportUtil.createCell(header, 0, "Data Preenchimento", estiloCabecalho);
        ReportUtil.createCell(header, 1, "Nome", estiloCabecalho);
        ReportUtil.createCell(header, 2, "Grupo", estiloCabecalho);
        ReportUtil.createCell(header, 3, "E-mail", estiloCabecalho);
        ReportUtil.createCell(header, 4, "Inscrição Minicurso/Oficina", estiloCabecalho);
        ReportUtil.createCell(header, 5, "Apresenta em RC", estiloCabecalho);
        ReportUtil.createCell(header, 6, "Roda(s) de Conversa", estiloCabecalho);
        ReportUtil.createCell(header, 7, "Indisponibilidade informada", estiloCabecalho);

        String comentario = "Lista de monitores aprovados e com cadastro confirmado, mas que não tiveram alocação atribuída.";
        adicionarComentario(wb, sheet, cellComentario, comentario);

        int rowIdx = 1;
        List<CandidatoVO> candidatos = programacao.getCandidatos().stream().filter(c -> c.getStatus().equals(Status.CONFIRMED)
                && c.getAlocacoes().size() == 0).toList();
        for (CandidatoVO candidato : candidatos) {
            Row row = sheet.createRow(rowIdx++);
            ReportUtil.createCell(row, 0, candidato.getInstanteCadastro(), estiloDataHora);
            ReportUtil.createCell(row, 1, candidato.getNome(), estiloBorda);
            ReportUtil.createCell(row, 2, candidato.getStrGrupo(), estiloBorda);
            ReportUtil.createCell(row, 3, candidato.getEmail1(), estiloBorda);
            ReportUtil.createCell(row, 4, candidato.getInscricaoCursoOficina(), estiloBorda);
            ReportUtil.createCell(row, 5, candidato.getApresentaTrabalho().toString(), estiloBorda);
            ReportUtil.createCell(row, 6, candidato.getRodasConversa(), estiloBorda);
            ReportUtil.createCell(row, 7, candidato.getIndisponibilidade(), estiloBorda);
        }

        // Ajustar colunas
        for (int i = 0; i < 8; i++) {
            sheet.autoSizeColumn(i);
        }
        return sheet;
    }

    private static Sheet criarAbaMonitorNaoConsiderado(Workbook wb, ProgramacaoVO programacao) {

        Sheet sheet = wb.createSheet("Monitores Cadastro Não Confirmado");

        // --- Estilo padrão (bordas finas pretas) ---
        CellStyle estiloBorda = wb.createCellStyle();
        estiloBorda.setBorderTop(BorderStyle.THIN);
        estiloBorda.setBorderBottom(BorderStyle.THIN);
        estiloBorda.setBorderLeft(BorderStyle.THIN);
        estiloBorda.setBorderRight(BorderStyle.THIN);
        estiloBorda.setTopBorderColor(IndexedColors.BLACK.getIndex());
        estiloBorda.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        estiloBorda.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        estiloBorda.setRightBorderColor(IndexedColors.BLACK.getIndex());

        // --- Estilo cabeçalho ---
        CellStyle estiloCabecalho = wb.createCellStyle();
        estiloCabecalho.cloneStyleFrom(estiloBorda); // mantém as bordas
        estiloCabecalho.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estiloCabecalho.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font fonteCabecalho = wb.createFont();
        fonteCabecalho.setBold(true);
        estiloCabecalho.setFont(fonteCabecalho);

        // Estilo cabeçalho
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        CellStyle estiloDataHora = wb.createCellStyle();
        estiloDataHora.cloneStyleFrom(estiloBorda);
        CreationHelper createHelper = wb.getCreationHelper();
        estiloDataHora.setDataFormat(
                createHelper.createDataFormat().getFormat("dd/MM/yyyy HH:mm")
        );

        // Cabeçalho
        Row header = sheet.createRow(0);
        Cell cellComentario = ReportUtil.createCell(header, 0, "Nome", estiloCabecalho);
        ReportUtil.createCell(header, 1, "E-mail", estiloCabecalho);
        ReportUtil.createCell(header, 2, "Grupo", estiloCabecalho);

        String comentario = "Lista de monitores aprovados (planilha programação - aba 'Monitores aprovados'), mas que não realizaram o preenchimento do formulário de confirmação.";
        adicionarComentario(wb, sheet, cellComentario, comentario);

        int rowIdx = 1;
        List<MonitorAprovadoVO> monitores = programacao.getMonitoresAprovados().stream().filter(m -> !m.getParticipacaoConfirmada()
                && m.getGrupo() != null && !"".equals(m.getGrupo())).toList();
        for (MonitorAprovadoVO monitor : monitores) {
            Row row = sheet.createRow(rowIdx++);
            ReportUtil.createCell(row, 0, monitor.getNome(), estiloBorda);
            ReportUtil.createCell(row, 1, monitor.getEmail(), estiloBorda);
            ReportUtil.createCell(row, 2, monitor.getGrupo(), estiloBorda);
        }

        // Ajustar colunas
        for (int i = 0; i < 2; i++) {
            sheet.autoSizeColumn(i);
        }
        return sheet;
    }

    private static Sheet criarAbaProgramacaoOriginal(Workbook wb, ProgramacaoVO programacao) {

        Sheet sheet = wb.createSheet("Monitoria na Programação");
        SheetDuplicator.copySheet(programacao.getProgramacaoSheet(), sheet, wb);

        List<AlocacaoVO> alocacoes = programacao.getSalas().stream().flatMap(s -> s.getAlocacoes().stream()).toList();
        Iterator<Row> rowIt = sheet.rowIterator();
        while (rowIt.hasNext()) {
            Row row = rowIt.next();
            try {
                Cell monitorCell = row.getCell(4);
                Cell hashCell = row.getCell(5);
                if (monitorCell != null && hashCell != null) {
                    String hash = hashCell.getStringCellValue();

                    if (!ContentUtil.estaVazio(hash) && hash.length() == 36) {
                        AtividadeVO atividade = AtividadeVO.builder().id(UUID.fromString(hash)).build();
                        Optional<AlocacaoVO> alocacaoOpt = alocacoes.stream().filter(a -> a.getEventos().contains(atividade)).findFirst();

                        if (alocacaoOpt.isPresent()) {
                            AlocacaoVO alocacao = alocacaoOpt.get();
                            String listaMonitores = ContentUtil.listarString(alocacao.getMonitores().stream().map(m -> m.getNome()).toList());
                            monitorCell.setCellValue(listaMonitores);
                            hashCell.setCellValue("");
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sheet;
    }

    private static List<Sheet> criarAbasProgramacaoRodasConversa(Workbook wb, ProgramacaoVO programacao) {

        List<Sheet> sheetList = new ArrayList<>();
        for (Map.Entry<TurnoVO, Sheet> entry : programacao.getPlanilhasRodasConversa().entrySet()) {
            Sheet sheetOriginal = entry.getValue();

            Sheet sheet = wb.createSheet(sheetOriginal.getSheetName());
            SheetDuplicator.copySheet(sheetOriginal, sheet, wb);
            sheetList.add(sheet);

            List<AlocacaoVO> alocacoes = programacao.getSalas().stream().flatMap(s -> s.getAlocacoes().stream()).toList();
            Iterator<Row> rowIt = sheet.rowIterator();
            while (rowIt.hasNext()) {
                Row row = rowIt.next();
                try {
                    Cell monitorCell = row.getCell(6);
                    if (monitorCell != null) {
                        String hash = monitorCell.getStringCellValue();

                        if (!ContentUtil.estaVazio(hash) && hash.length() == 36) {
                            RodaConversaVO rodaConversa = RodaConversaVO.builder().id(UUID.fromString(hash)).build();
                            Optional<AlocacaoVO> alocacaoOpt = alocacoes.stream().filter(a -> a.getEventos().contains(rodaConversa)).findFirst();

                            if (alocacaoOpt.isPresent()) {
                                AlocacaoVO alocacao = alocacaoOpt.get();
                                String listaMonitores = ContentUtil.listarString(alocacao.getMonitores().stream().map(m -> m.getNome()).toList());
                                monitorCell.setCellValue(listaMonitores);
                            }
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return sheetList;
    }

    private static String obterCargaHoraria(CandidatoVO monitor) {
        long cargaHorariaMinutos = 0;
        String cargaHoraria = "";
        if (monitor != null) {
            cargaHorariaMinutos = monitor.getAlocacoes().stream().mapToLong(a -> a.getTurno().obterTempoMinutos()).sum();
            Duration duracao = Duration.ofMinutes(cargaHorariaMinutos);
            long horas = duracao.toHours();
            long minutos = duracao.toMinutesPart();
            cargaHoraria = String.format("%02d:%02d", horas, minutos);
        }
        return cargaHoraria;
    }

    private static void adicionarComentario(Workbook wb, Sheet sheet, Cell cell, String comentario) {
        //Cria o "drawing" que gerencia caixas de texto, comentários, etc.
        Drawing<?> drawing = sheet.createDrawingPatriarch();

        //Define a posição e tamanho do comentário (coluna inicial, linha inicial, coluna final, linha final)
        CreationHelper factory = wb.getCreationHelper();
        ClientAnchor anchor = factory.createClientAnchor();
        anchor.setCol1(cell.getColumnIndex());
        anchor.setCol2(cell.getColumnIndex() + 5);
        anchor.setRow1(cell.getColumnIndex());
        anchor.setRow2(cell.getColumnIndex() + 3);

        //Cria o comentário
        Comment comment = drawing.createCellComment(anchor);
        comment.setString(factory.createRichTextString(comentario));
        comment.setAuthor("Atribuidor de Monitores");
        cell.setCellComment(comment);

    }
}
