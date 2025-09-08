package br.com.rodrigo.monitores.builder;

import br.com.rodrigo.monitores.model.*;
import br.com.rodrigo.monitores.model.comparator.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.*;
import org.apache.poi.xssf.usermodel.*;

import java.io.*;
import java.nio.file.*;
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

        // Estilo cabeçalho
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Cabeçalho
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Sala");
        header.createCell(1).setCellValue("Turno");
        header.createCell(2).setCellValue("Atividades");
        header.createCell(3).setCellValue("Total Monitores");
        header.createCell(4).setCellValue("Monitores Selecionados");

        for (Cell cell : header) {
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (SalaVO sala : programacao.getSalas()) {
            int salaStartRow = rowIdx;

            for (AlocacaoVO alocacao : sala.getAlocacoes()) {
                int alocacaoStartRow = rowIdx;

                // Se não houver candidatos, cria pelo menos uma linha
                List<CandidatoVO> candidatos = alocacao.getMonitores().isEmpty() ?
                        Collections.singletonList(CandidatoVO.builder().nome("").build()) : alocacao.getMonitores();

                Collections.sort(candidatos);
                for (CandidatoVO candidato : candidatos) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(4).setCellValue(candidato.getNome());
                }

                int alocacaoEndRow = rowIdx - 1;

                //Mescla alocacao
                if (alocacao.getMonitores().size() > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(alocacaoStartRow, alocacaoEndRow, 1, 1));
                    sheet.addMergedRegion(new CellRangeAddress(alocacaoStartRow, alocacaoEndRow, 2, 2));
                }

                Row firstAlocacaoRow = sheet.getRow(alocacaoStartRow);
                firstAlocacaoRow.createCell(1).setCellValue(alocacao.getTurno().toString());
                firstAlocacaoRow.createCell(2).setCellValue(alocacao.getEventos().stream().map(e -> e.toString()).toList().toString());
                firstAlocacaoRow.createCell(3).setCellValue(alocacao.getTotalMonitores());
            }

            int salaEndRow = rowIdx - 1;

            //Mescla sala
            if (sala.getAlocacoes().stream().mapToInt(a -> a.getMonitores().size()).sum() > 1) {
                sheet.addMergedRegion(new CellRangeAddress(salaStartRow, salaEndRow, 0, 0));
            }

            Row firstSalaRow = sheet.getRow(salaStartRow);
            firstSalaRow.createCell(0).setCellValue(sala.getNome());
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

        // Estilo cabeçalho
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        // Cabeçalho
        Row header = sheet.createRow(0);
        header.createCell(0).setCellValue("Monitor");
        header.createCell(1).setCellValue("Sala");
        header.createCell(2).setCellValue("Turno");
        header.createCell(3).setCellValue("Atividades");

        for (Cell cell : header) {
            cell.setCellStyle(headerStyle);
        }

        int rowIdx = 1;
        for (Map.Entry<String, List<String[]>> entry : monitorMap.entrySet()) {
            String monitor = entry.getKey();
            List<String[]> participacoes = entry.getValue();

            int monitorStartRow = rowIdx;
            for (String[] info : participacoes) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(1).setCellValue(info[0]); //Sala
                row.createCell(2).setCellValue(info[1]); //Turno
                row.createCell(3).setCellValue(info[2]); //Atividades
            }

            int monitorEndRow = rowIdx - 1;

            //Mesclar células do monitor se tiver mais de um turno
            if (participacoes.size() > 1) {
                sheet.addMergedRegion(new CellRangeAddress(monitorStartRow, monitorEndRow, 0, 0)); // Monitor
            }

            Row firstRow = sheet.getRow(monitorStartRow);
            firstRow.createCell(0).setCellValue(monitor);
        }

        // Ajustar colunas
        for (int i = 0; i < 4; i++) {
            sheet.autoSizeColumn(i);
        }
        return sheet;
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
