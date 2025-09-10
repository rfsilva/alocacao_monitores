package br.com.rodrigo.monitores.builder;

import org.apache.poi.ss.usermodel.*;

import java.util.*;

public final class SheetDuplicator {

    private SheetDuplicator() {
    }

    public static void copySheet(Sheet srcSheet, Sheet destSheet, Workbook destWorkbook) {
        int maxColumnNum = 0;

        Map<Integer, CellStyle> styleMap = new HashMap<>();

        for (int i = srcSheet.getFirstRowNum(); i <= srcSheet.getLastRowNum(); i++) {
            Row srcRow = srcSheet.getRow(i);
            Row destRow = destSheet.createRow(i);
            if (srcRow != null) {
                copyRow(srcRow, destRow, destWorkbook, styleMap);
                if (srcRow.getLastCellNum() > maxColumnNum) {
                    maxColumnNum = srcRow.getLastCellNum();
                }
            }
        }

        // Copia largura das colunas
        for (int i = 0; i <= maxColumnNum; i++) {
            destSheet.setColumnWidth(i, srcSheet.getColumnWidth(i));
        }

        // Copia regiões mescladas
        for (int i = 0; i < srcSheet.getNumMergedRegions(); i++) {
            destSheet.addMergedRegion(srcSheet.getMergedRegion(i));
        }
    }

    private static void copyRow(Row srcRow, Row destRow, Workbook destWorkbook, Map<Integer, CellStyle> styleMap) {
        destRow.setHeight(srcRow.getHeight());

        for (int j = srcRow.getFirstCellNum(); j < srcRow.getLastCellNum(); j++) {
            Cell srcCell = srcRow.getCell(j);
            Cell destCell = destRow.createCell(j);
            if (srcCell != null) {
                copyCell(srcCell, destCell, destWorkbook, styleMap);
            }
        }
    }

    private static void copyCell(Cell srcCell, Cell destCell, Workbook destWorkbook, Map<Integer, CellStyle> styleMap) {
        // Copia estilo
        if (srcCell.getSheet().getWorkbook() != destWorkbook) {
            int stHashCode = srcCell.getCellStyle().hashCode();
            CellStyle newCellStyle = styleMap.get(stHashCode);
            if (newCellStyle == null) {
                newCellStyle = destWorkbook.createCellStyle();
                newCellStyle.cloneStyleFrom(srcCell.getCellStyle());
                styleMap.put(stHashCode, newCellStyle);
            }
            destCell.setCellStyle(newCellStyle);
        } else {
            destCell.setCellStyle(srcCell.getCellStyle());
        }

        // Copia valor
        switch (srcCell.getCellType()) {
            case STRING:
                destCell.setCellValue(srcCell.getStringCellValue());
                break;
            case NUMERIC:
                destCell.setCellValue(srcCell.getNumericCellValue());
                break;
            case BOOLEAN:
                destCell.setCellValue(srcCell.getBooleanCellValue());
                break;
            case FORMULA:
                destCell.setCellFormula(srcCell.getCellFormula());
                break;
            case BLANK:
                destCell.setBlank();
                break;
            default:
                break;
        }

        // Copia comentário (se houver)
        if (srcCell.getCellComment() != null) {
            CreationHelper factory = destWorkbook.getCreationHelper();
            Drawing<?> drawing = destCell.getSheet().createDrawingPatriarch();
            ClientAnchor anchor = factory.createClientAnchor();
            Comment srcComment = srcCell.getCellComment();
            Comment destComment = drawing.createCellComment(anchor);
            destComment.setString(srcComment.getString());
            destComment.setAuthor(srcComment.getAuthor());
            destCell.setCellComment(destComment);
        }
    }

}

