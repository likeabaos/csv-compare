package likeabaos.tools.util.csv.compare;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class Helper {
    private final static Logger LOG = LogManager.getLogger();

    public static String formatTimestampPrintable(long millis) {
	return String.format("%1$TY-%1$Tm-%1$Td_%1$TH-%1$TM-%1$TS-%1$TL", millis);
    }

    public static ArrayList<String> getCSV(String csvString) {
	ArrayList<String> fields = new ArrayList<String>();
	if (!StringUtils.isBlank(csvString) && !csvString.equals("*")) {
	    for (String str : StringUtils.split(csvString, ",")) {
		fields.add(StringUtils.trim(str));
	    }
	}
	return fields;
    }

    public static Map<String, List<String>> loadCsvFile(File file, String dataColumns)
	    throws FileNotFoundException, IOException {
	LOG.info("Loading file {}...", file.getAbsolutePath());

	List<String> columnNames = Helper.getCSV(dataColumns);
	LOG.debug("Set to load these columns: {}", columnNames);

	int rowCount = 0;
	Map<String, List<String>> data = new LinkedHashMap<>();
	try (final Reader reader = new FileReader(file)) {
	    Iterable<CSVRecord> rows = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(reader);
	    for (CSVRecord row : rows) {
		if (++rowCount == 1 && columnNames.size() == 0) {
		    LOG.debug("Trying to load all columns in CSV file");
		    columnNames.addAll(row.getParser().getHeaderNames());
		}
		for (String colName : columnNames) {
		    List<String> column = data.get(colName);
		    if (column == null) {
			column = new ArrayList<>();
			data.put(colName, column);
		    }
		    column.add(row.get(colName));
		}
	    }
	}
	LOG.info("Loaded {} row(s) and {} column(s)", rowCount, data.size());
	return data;
    }

    public static Map<List<String>, List<String>> getDataByKeys(List<String> keyColNames,
	    Map<String, List<String>> rawData) {

	int count = 0;
	int max = 0;
	Map<List<String>, List<String>> dataByKeys = new LinkedHashMap<>();
	do {
	    List<String> key = new ArrayList<>();
	    List<String> data = new ArrayList<>();
	    for (Entry<String, List<String>> entry : rawData.entrySet()) {
		String colName = entry.getKey();
		List<String> column = entry.getValue();
		if (max == 0)
		    max = column.size();

		if (keyColNames.contains(colName))
		    key.add(column.get(count));
		else
		    data.add(column.get(count));
	    }
	    dataByKeys.put(key, data);
	} while (++count < max);
	return dataByKeys;
    }

    public static void createSheet(Workbook workbook, List<String> keyColumns, List<String> dataColumns,
	    String sheetName, Map<Integer, List<String>> data) {

	LOG.info("Creating sheet {}...", sheetName);
	Sheet sheet = workbook.createSheet(sheetName);

	int rowCount = 0;
	int colCount = 0;
	for (Entry<Integer, List<String>> entry : data.entrySet()) {
	    // first row... should also write the header fields
	    if (rowCount == 0) {
		Row row = sheet.createRow(rowCount++);

		colCount = 0;
		row.createCell(colCount++).setCellValue("Row #");
		colCount = Helper.setCellValue(row, keyColumns, colCount);
		colCount = Helper.setCellValue(row, dataColumns, colCount);

		Helper.setRowStyle(workbook, true, false, row);
		sheet.createFreezePane(1, 1);
	    }

	    Row row = sheet.createRow(rowCount++);
	    colCount = 0;
	    row.createCell(colCount++).setCellValue(entry.getKey());
	    colCount = Helper.setCellValue(row, entry.getValue(), colCount);
	}

	for (int i = 0; i < colCount; i++) {
	    sheet.autoSizeColumn(i);
	}
    }

    public static int setCellValue(Row row, List<String> dataColumns, int startIndex) {
	int colCount = startIndex;
	for (String field : dataColumns) {
	    Cell cell = row.createCell(colCount++);
	    cell.setCellValue(field);
	}
	return colCount;
    }

    public static void setRowStyle(Workbook workbook, boolean bold, boolean italicized, Row... rows) {
	Font font = workbook.createFont();
	font.setBold(bold);
	font.setItalic(italicized);
	CellStyle style = workbook.createCellStyle();
	style.setFont(font);
	for (Row row : rows) {
	    for (Cell cell : row) {
		cell.setCellStyle(style);
	    }
	}
    }
}
