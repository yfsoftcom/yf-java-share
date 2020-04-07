package io.yunplus.share1;

import lombok.Data;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.util.Assert;

import java.io.*;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XlsUtil {

    static final BiFunction<String, String, Date> getDateData = (date, formatter) -> {
        try {
            return new SimpleDateFormat(formatter).parse(date);
        } catch (ParseException e) {
            return new Date();
        }
    };

    @Data
    public static class DataCell {
        private int col;
        private int row;
        /**
         *      _NONE(-1),
         *     NUMERIC(0),
         *     STRING(1),
         *     FORMULA(2),
         *     BLANK(3),
         *     BOOLEAN(4),
         *     ERROR(5);
         */
        private String type = "STRING";
        private Object value;

        public DataCell() {}

        public DataCell(int row, int col, String type, Object value) {
            this.col = col;
            this.row = row;
            this.type = type;
            this.value = value;
        }

        public DataCell(int row, int col, Object value) {
            this.col = col;
            this.row = row;
            this.value = value;
        }

        public DataCell(int row, int col) {
            this.col = col;
            this.row = row;
        }
    }


    static Pattern patternForColumn = Pattern.compile("^[A-Za-z]+");   // the pattern to search column

    static Pattern patternForRow = Pattern.compile("[0-9]+$");   // the pattern to search row



    /**
     * get the cell position
     * @param key the E1, AA88 ; the absolute position of the xls
     * @return
     */
    public static int[] getCellPosition(String key) throws Exception{

        Matcher m = patternForColumn.matcher(key);
        Assert.state( m.find(), key + " is illegal! should be E1, AA88 !");
        String columnStr = m.group();
        char[] chars = columnStr.toCharArray();
        int column = 0;
        // formula: Sum(Math.pow(26, len - i - 1) * (X - A + 1))
        for(int i = 0; i < chars.length; i++){
            column += (chars[i] - 'A' + 1) * Math.pow(26, (chars.length - i - 1));
        }
        m = patternForRow.matcher(key);
        Assert.state( m.find(), key + " is illegal! should be E1, AA88 !");
        int row = Integer.parseInt(m.group());
        return new int[]{ column - 1, row - 1 };
    }

    /**
     * fill the placeholder by template workbook
     * get the workbook reference
     * get sheet -> row -> col -> set cell type -> set cell value
     * save as another workbook
     * @param template
     * @param bookDatas
     * @throws IOException
     * @throws InvalidFormatException
     */
    public static void fillPlaceholderByTemplateWorkbook(String template, List<DataCell[]> bookDatas, OutputStream outputStream) throws IOException, InvalidFormatException {
        fillPlaceholderByTemplateWorkbook(new FileInputStream(template), bookDatas, outputStream);
    }


    /**
     * fill the placeholder by template workbook
     * get the workbook reference
     * get sheet -> row -> col -> set cell type -> set cell value
     * save as another workbook
     * @param template
     * @param bookDatas
     * @throws IOException
     * @throws InvalidFormatException
     */
    public static void fillPlaceholderByTemplateWorkbook(FileInputStream template, List<DataCell[]> bookDatas, OutputStream outputStream) throws IOException, InvalidFormatException {
        // Obtain a workbook from the excel file
        try(Workbook workbook = WorkbookFactory.create(template)) {
            for(int i =0 ; i < bookDatas.size(); i++){
                Sheet sheet = workbook.getSheetAt(i);
                DataCell[] cells = bookDatas.get(i);
                Arrays.stream(cells).forEach(c -> {
                    Row row = sheet.getRow(c.getRow());
                    Cell cell = row.getCell(c.getCol());
                    if(cell == null){
                        cell = row.createCell(c.getCol());
                        cell.setCellType(CellType.valueOf(c.getType()));
                    }

                    String tmpStr = String.valueOf(c.getValue());
                    switch(c.getType()){
                        case "STRING":
                            cell.setCellValue(tmpStr);
                            break;
                        case "NUMERIC":
                            cell.setCellValue(Double.parseDouble(tmpStr));
                            break;
                        case "yyyyMM":
                            cell.setCellValue(getDateData.apply(tmpStr, "yyyyMM"));
                            break;
                        case "yyyy-MM-dd":
                            cell.setCellValue(getDateData.apply(tmpStr, "yyyy-MM-dd"));
                            break;
                        case "yyyy-MM-dd HH:mm:ss":
                            cell.setCellValue(getDateData.apply(tmpStr, "yyyy-MM-dd HH:mm:ss"));
                            break;
                        case "BOOLEAN":
                            cell.setCellValue(Boolean.parseBoolean(tmpStr));
                            break;
                        default:
                            cell.setCellValue(tmpStr);
                            break;
                    }

                });
            }
            // Write the output
            workbook.write(outputStream);
        }
    }

    public static void readWorkbookCell(InputStream is, int sheetIndex, DataCell cell) throws IOException, InvalidFormatException {
        try(Workbook workbook = WorkbookFactory.create(is)){
            Sheet sheet = workbook.getSheetAt(sheetIndex);
            Row row = sheet.getRow(cell.getRow());
            Cell c = row.getCell(cell.getCol());
            if(c == null){
                return;
            }
            switch(cell.getType()){
                case "STRING":
                    cell.setValue(c.getStringCellValue());
                    break;
                case "NUMERIC":
                    cell.setValue(c.getNumericCellValue());
                    break;
                case "BOOLEAN":
                    cell.setValue(c.getBooleanCellValue());
                    break;
                case "DATE":
                    cell.setValue(c.getDateCellValue());
                    break;
                default:
                    cell.setValue("");
                    break;
            }
        }

    }
}