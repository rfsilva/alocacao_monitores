package br.com.rodrigo.monitores.builder;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.*;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public final class ReportUtil {

    private ReportUtil() {
    }

    public static void salvarExcel(Workbook workbook, String filePath) throws Exception {
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

    public static Cell createCell(Row row, int col, String value) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        return cell;
    }

    public static Cell createCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        return cell;
    }

    public static Cell createCell(Row row, int col, Integer value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
        return cell;
    }

    public static Cell createCell(Row row, int col, LocalDateTime value, CellStyle style) {
        Date date = Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
        Cell cell = row.createCell(col);
        cell.setCellValue(date);
        cell.setCellStyle(style);
        return cell;
    }

    /**
     * Cria a região mesclada, aplica bordas via RegionUtil e define o CellStyle
     * em todas as células da região para garantir que a borda apareça corretamente.
     */
    public static void applyMergedRegionWithBorders(Sheet sheet,
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
}
