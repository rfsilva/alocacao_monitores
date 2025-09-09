package br.com.rodrigo.monitores.builder;

import br.com.rodrigo.monitores.model.*;
import br.com.rodrigo.monitores.model.comparator.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public final class ReportBuilder {

    private ReportBuilder() {
    }

    private static String caminhoRelatorio = "output/alocacoes.xlsx";

    public static void gerarRelatorios(ProgramacaoVO programacao) {
        XSSFWorkbook wb = new XSSFWorkbook();

        Sheet salasSheet = criarAbaAlocacaoPorSala(wb, programacao);
        Sheet monitoresSheet = criarAbaAlocacaoPorMonitor(wb, programacao);

        try {
            salvarExcel(wb, caminhoRelatorio);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Sheet criarAbaAlocacaoPorSala(Workbook wb, ProgramacaoVO programacao) {
        Sheet sheet = wb.createSheet("Alocacao por Sala");

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
        createCell(header, 0, "Sala", estiloCabecalho);
        createCell(header, 1, "Turno", estiloCabecalho);
        createCell(header, 2, "Atividades", estiloCabecalho);
        createCell(header, 3, "Total Monitores", estiloCabecalho);
        createCell(header, 4, "Monitores Selecionados", estiloCabecalho);

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
                    createCell(row, 4, candidato.getNome(), estiloBorda);
                }

                int alocacaoEndRow = rowIdx - 1;

                //Mescla alocacao
                if (alocacao.getMonitores().size() > 1) {
                    applyMergedRegionWithBorders(sheet, alocacaoStartRow, alocacaoEndRow, 1, 1, estiloBorda);
                    applyMergedRegionWithBorders(sheet, alocacaoStartRow, alocacaoEndRow, 2, 2, estiloBorda);
                    applyMergedRegionWithBorders(sheet, alocacaoStartRow, alocacaoEndRow, 3, 3, estiloBorda);
                }

                Row firstAlocacaoRow = sheet.getRow(alocacaoStartRow);
                createCell(firstAlocacaoRow, 1, alocacao.getTurno().toString(), estiloBorda);
                createCell(firstAlocacaoRow, 2, alocacao.getEventos().stream().map(e -> e.toString()).toList().toString(), estiloBorda);
                createCell(firstAlocacaoRow, 3, alocacao.getTotalMonitores(), estiloBorda);
            }

            int salaEndRow = rowIdx - 1;

            //Mescla sala
            if (salaStartRow < salaEndRow) {
                applyMergedRegionWithBorders(sheet, salaStartRow, salaEndRow, 0, 0, estiloBorda);
            }

            Row firstSalaRow = sheet.getRow(salaStartRow);
            createCell(firstSalaRow, 0, sala.getNome(), estiloBorda);
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
                list.add(new String[]{alocacao.getSala().getNome(), alocacao.getTurno().toString(), alocacao.getEventos().stream().map(e -> e.toString()).toList().toString()});
                monitorMap.put(monitor.getNome(), list);
            }
        }

        Sheet sheet = wb.createSheet("Alocacao por Monitor");

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

        // Cabeçalho
        Row header = sheet.createRow(0);
        createCell(header, 0, "Monitor", estiloCabecalho);
        createCell(header, 1, "Carga Horária", estiloCabecalho);
        createCell(header, 2, "Sala", estiloCabecalho);
        createCell(header, 3, "Turno", estiloCabecalho);
        createCell(header, 4, "Atividade(s)", estiloCabecalho);

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
                createCell(row, 2, info[0], estiloBorda); //Sala
                createCell(row, 3, info[1], estiloBorda); //Turno
                createCell(row, 4, info[2], estiloBorda); //Atividade
            }

            int monitorEndRow = rowIdx - 1;

            //Mesclar células do monitor se tiver mais de um turno
            if (participacoes.size() > 1) {
                applyMergedRegionWithBorders(sheet, monitorStartRow, monitorEndRow, 0, 0, estiloBorda); //Monitor
                applyMergedRegionWithBorders(sheet, monitorStartRow, monitorEndRow, 1, 1, estiloBorda); //Carga horária
            }

            Row firstRow = sheet.getRow(monitorStartRow);
            createCell(firstRow, 0, nomeMonitor, estiloBorda);
            createCell(firstRow, 1, obterCargaHoraria(monitor), estiloBorda);
        }

        // Ajustar colunas
        for (int i = 0; i < 5; i++) {
            sheet.autoSizeColumn(i);
        }
        return sheet;
    }

    private static void createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private static void createCell(Row row, int col, Integer value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    /**
     * Cria a região mesclada, aplica bordas via RegionUtil e define o CellStyle
     * em todas as células da região para garantir que a borda apareça corretamente.
     */
    private static void applyMergedRegionWithBorders(Sheet sheet,
                                                     int firstRow, int lastRow,
                                                     int firstCol, int lastCol,
                                                     CellStyle style) {
        CellRangeAddress region = new CellRangeAddress(firstRow, lastRow, firstCol, lastCol);
        sheet.addMergedRegion(region);

        // aplica bordas na região (usando RegionUtil)
        RegionUtil.setBorderTop(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderBottom(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderLeft(BorderStyle.THIN, region, sheet);
        RegionUtil.setBorderRight(BorderStyle.THIN, region, sheet);

        short black = IndexedColors.BLACK.getIndex();
        RegionUtil.setTopBorderColor(black, region, sheet);
        RegionUtil.setBottomBorderColor(black, region, sheet);
        RegionUtil.setLeftBorderColor(black, region, sheet);
        RegionUtil.setRightBorderColor(black, region, sheet);

        // garante que cada célula visível/invisível dentro da região tenha o estilo
        for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) row = sheet.createRow(r);
            for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                Cell cell = row.getCell(c);
                if (cell == null) cell = row.createCell(c);
                cell.setCellStyle(style);
            }
        }
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

    private static void salvarExcel(Workbook workbook, String filePath) throws Exception {
        Path path = Paths.get(filePath).toAbsolutePath();
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent); // garante todas as pastas
        }

        // cria/gera o arquivo (substitui se já existir)
        try (OutputStream os = Files.newOutputStream(
                path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            workbook.write(os);
        } finally {
            workbook.close(); // importante fechar o workbook
        }
    }
}
