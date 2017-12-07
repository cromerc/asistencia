package cl.cromer.ubb.attendance;

import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Workbook;

public class Excel {
    private Workbook workbook;

    private CellStyle mainCellStyle = null;
    private CellStyle leftCellStyle = null;
    private CellStyle rightCellStyle = null;
    private CellStyle middleCellStyle = null;
    private CellStyle blueCellStyle = null;
    private CellStyle greenCellStyle = null;
    private CellStyle yellowCellStyle = null;
    private CellStyle orangeCellStyle = null;
    private CellStyle redCellStyle = null;

    public Excel(Workbook workbook) {
        this.workbook = workbook;
    }

    protected void createStyles() {
        Font font = workbook.createFont();
        mainCellStyle = workbook.createCellStyle();
        mainCellStyle.setBorderBottom(CellStyle.BORDER_THIN);
        mainCellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        mainCellStyle.setBorderLeft(CellStyle.BORDER_THIN);
        mainCellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        mainCellStyle.setBorderRight(CellStyle.BORDER_THIN);
        mainCellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
        mainCellStyle.setBorderTop(CellStyle.BORDER_THIN);
        mainCellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());

        leftCellStyle = workbook.createCellStyle();
        leftCellStyle.setBorderBottom(CellStyle.BORDER_THIN);
        leftCellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        leftCellStyle.setBorderLeft(CellStyle.BORDER_THIN);
        leftCellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        leftCellStyle.setBorderTop(CellStyle.BORDER_THIN);
        leftCellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
        leftCellStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        leftCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        font.setColor(HSSFColor.WHITE.index);
        leftCellStyle.setFont(font);

        rightCellStyle = workbook.createCellStyle();
        rightCellStyle.setBorderBottom(CellStyle.BORDER_THIN);
        rightCellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        rightCellStyle.setBorderRight(CellStyle.BORDER_THIN);
        rightCellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
        rightCellStyle.setBorderTop(CellStyle.BORDER_THIN);
        rightCellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
        rightCellStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        rightCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        font.setColor(HSSFColor.WHITE.index);
        rightCellStyle.setFont(font);

        middleCellStyle = workbook.createCellStyle();
        middleCellStyle.setBorderBottom(CellStyle.BORDER_THIN);
        middleCellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        middleCellStyle.setBorderTop(CellStyle.BORDER_THIN);
        middleCellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
        middleCellStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        middleCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        font.setColor(HSSFColor.WHITE.index);
        middleCellStyle.setFont(font);

        blueCellStyle = workbook.createCellStyle();
        blueCellStyle.setBorderBottom(CellStyle.BORDER_THIN);
        blueCellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        blueCellStyle.setBorderLeft(CellStyle.BORDER_THIN);
        blueCellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        blueCellStyle.setBorderRight(CellStyle.BORDER_THIN);
        blueCellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
        blueCellStyle.setBorderTop(CellStyle.BORDER_THIN);
        blueCellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
        blueCellStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        blueCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        font.setColor(HSSFColor.WHITE.index);
        blueCellStyle.setFont(font);

        greenCellStyle = workbook.createCellStyle();
        greenCellStyle.setBorderBottom(CellStyle.BORDER_THIN);
        greenCellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        greenCellStyle.setBorderLeft(CellStyle.BORDER_THIN);
        greenCellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        greenCellStyle.setBorderRight(CellStyle.BORDER_THIN);
        greenCellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
        greenCellStyle.setBorderTop(CellStyle.BORDER_THIN);
        greenCellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
        greenCellStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
        greenCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        font.setColor(HSSFColor.BLACK.index);
        greenCellStyle.setFont(font);
        greenCellStyle.setAlignment(CellStyle.ALIGN_CENTER);

        yellowCellStyle = workbook.createCellStyle();
        yellowCellStyle.setBorderBottom(CellStyle.BORDER_THIN);
        yellowCellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        yellowCellStyle.setBorderLeft(CellStyle.BORDER_THIN);
        yellowCellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        yellowCellStyle.setBorderRight(CellStyle.BORDER_THIN);
        yellowCellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
        yellowCellStyle.setBorderTop(CellStyle.BORDER_THIN);
        yellowCellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
        yellowCellStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        yellowCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        font.setColor(HSSFColor.BLACK.index);
        yellowCellStyle.setFont(font);
        yellowCellStyle.setAlignment(CellStyle.ALIGN_CENTER);

        orangeCellStyle = workbook.createCellStyle();
        orangeCellStyle.setBorderBottom(CellStyle.BORDER_THIN);
        orangeCellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        orangeCellStyle.setBorderLeft(CellStyle.BORDER_THIN);
        orangeCellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        orangeCellStyle.setBorderRight(CellStyle.BORDER_THIN);
        orangeCellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
        orangeCellStyle.setBorderTop(CellStyle.BORDER_THIN);
        orangeCellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
        orangeCellStyle.setFillForegroundColor(IndexedColors.ORANGE.getIndex());
        orangeCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        font.setColor(HSSFColor.BLACK.index);
        orangeCellStyle.setFont(font);
        orangeCellStyle.setAlignment(CellStyle.ALIGN_CENTER);

        redCellStyle = workbook.createCellStyle();
        redCellStyle.setBorderBottom(CellStyle.BORDER_THIN);
        redCellStyle.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        redCellStyle.setBorderLeft(CellStyle.BORDER_THIN);
        redCellStyle.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        redCellStyle.setBorderRight(CellStyle.BORDER_THIN);
        redCellStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());
        redCellStyle.setBorderTop(CellStyle.BORDER_THIN);
        redCellStyle.setTopBorderColor(IndexedColors.BLACK.getIndex());
        redCellStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        redCellStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);
        font.setColor(IndexedColors.BLACK.getIndex());
        redCellStyle.setFont(font);
        redCellStyle.setAlignment(CellStyle.ALIGN_CENTER);
    }

    protected CellStyle getCellStyle(String type) {
        switch (type) {
            case "main":
                return mainCellStyle;
            case "left":
                return leftCellStyle;
            case "middle":
                return middleCellStyle;
            case "right":
                return rightCellStyle;
            case "blue":
                return blueCellStyle;
            case "green":
                return greenCellStyle;
            case "yellow":
                return yellowCellStyle;
            case "orange":
                return orangeCellStyle;
            case "red":
                return redCellStyle;
            default:
                return null;
        }
    }
}
