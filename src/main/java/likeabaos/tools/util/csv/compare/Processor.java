package likeabaos.tools.util.csv.compare;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Processor {
    private static final Logger LOG = LogManager.getLogger();

    private String defaultOutputFilename;
    private Map<Integer, List<String>> left;
    private Map<Integer, List<String>> right;
    private Map<Integer, Pair<List<String>, List<String>>> diff;

    public Processor(App app) {
	String value = app.getProperties().getProperty("default_output_filename");
	this.defaultOutputFilename = value;
    }

    public Map<Integer, List<String>> getLeft() {
	if (left == null)
	    left = new LinkedHashMap<>();
	return left;
    }

    public Map<Integer, List<String>> getRight() {
	if (right == null)
	    right = new LinkedHashMap<>();
	return right;
    }

    public Map<Integer, Pair<List<String>, List<String>>> getDiff() {
	if (diff == null)
	    diff = new LinkedHashMap<>();
	return diff;
    }

    public void start(File left, File right, String allColumns, String keyColumns, File output)
	    throws FileNotFoundException, IOException {
	StopWatch watch = new StopWatch();
	watch.start();
	LOG.debug("Started stopwatch");

	try {
	    Map<String, List<String>> leftData = Helper.loadCsvFile(left, allColumns);
	    Map<String, List<String>> rightData = Helper.loadCsvFile(right, allColumns);
	    List<String> keys = compare(keyColumns, leftData, rightData);
	    List<String> dataColumns = getDataColumns(leftData.keySet(), keys);

	    // clear out some raw data so we don't keep too much data in memory.
	    // we should already have all the data needed for output by now.
	    leftData.clear();
	    leftData = null;
	    rightData.clear();
	    rightData = null;

	    outputResults(dataColumns, keys, output);
	} finally {
	    watch.stop();
	    LOG.info("Process completed in: {}", watch.toString());
	}
    }

    List<String> compare(String keyColumns, Map<String, List<String>> left, Map<String, List<String>> right) {
	List<String> keyColNames = Helper.getCSV(keyColumns);
	if (keyColNames.size() == 0) {
	    keyColNames.addAll(left.keySet());
	    LOG.info("No specific key columns are provided, trying to use all columns");
	}
	LOG.info("Comparing using these columns as key: {}", keyColNames);

	Map<List<String>, List<String>> leftByKeys = Helper.getDataByKeys(keyColNames, left);
	Map<List<String>, List<String>> rightByKeys = Helper.getDataByKeys(keyColNames, right);

	LOG.info("Calculating Left Side...");
	int count = 0;
	for (Entry<List<String>, List<String>> entry : leftByKeys.entrySet()) {
	    LOG.trace("Getting Left Side and Diff for Row # {}", ++count);

	    List<String> rightSide = rightByKeys.get(entry.getKey());
	    if (rightSide == null) {
		List<String> side = new ArrayList<String>();
		side.addAll(entry.getKey());
		side.addAll(entry.getValue());
		this.getLeft().put(count, side);
	    } else if (!entry.getValue().equals(rightSide)) {
		List<String> side = new ArrayList<String>();
		side.addAll(entry.getKey());
		side.addAll(entry.getValue());
		Pair<List<String>, List<String>> pair = Pair.of(side, rightSide);
		this.getDiff().put(count, pair);
	    }
	}

	LOG.info("Calculating Right Side...");
	count = 0;
	for (Entry<List<String>, List<String>> entry : rightByKeys.entrySet()) {
	    LOG.trace("Getting Right for Row # {}", ++count);

	    List<String> leftSide = leftByKeys.get(entry.getKey());
	    if (leftSide == null) {
		List<String> side = new ArrayList<String>();
		side.addAll(entry.getKey());
		side.addAll(entry.getValue());
		this.getRight().put(count, side);
	    }
	}

	LOG.info("Comparison Completed");
	return keyColNames;
    }

    List<String> getDataColumns(Collection<String> allColumns, Collection<String> keyColumns) {
	List<String> dataColumns = new ArrayList<String>();
	for (String col : allColumns) {
	    if (!keyColumns.contains(col))
		dataColumns.add(col);
	}
	return dataColumns;
    }

    void outputResults(List<String> dataColumns, List<String> keyColumns, File output)
	    throws FileNotFoundException, IOException {

	if (output == null) {
	    output = new File("output/"
		    + this.defaultOutputFilename
		    + "_"
		    + Helper.formatTimestampPrintable(System.currentTimeMillis())
		    + ".xlsx");
	}
	FileUtils.forceMkdirParent(output);

	LOG.info("Creating result file: {}", output.getName());
	try (Workbook workbook = new XSSFWorkbook(); OutputStream out = new FileOutputStream(output)) {
	    this.createSheetDIFF(workbook, keyColumns, dataColumns);
	    Helper.createSheet(workbook, keyColumns, dataColumns, "LEFT NEW", this.getLeft());
	    Helper.createSheet(workbook, keyColumns, dataColumns, "RIGHT NEW", this.getRight());
	    workbook.write(out);
	    LOG.info("Created file: {}", output.getAbsoluteFile());
	}
    }

    public void createSheetDIFF(Workbook workbook, List<String> keyColumns, List<String> dataColumns) {
	LOG.info("Creating sheet {}...", "DIFF");
	Sheet diff = workbook.createSheet("DIFF");

	int rowCount = 0;
	Row row = diff.createRow(rowCount++);
	row.createCell(1).setCellValue("KEYS");
	row.createCell(keyColumns.size() + 1).setCellValue("LEFT");
	row.createCell(keyColumns.size() + dataColumns.size() + 2).setCellValue("RIGHT");
	Helper.setRowStyle(workbook, true, true, row);

	row = diff.createRow(rowCount++);
	int colCount = 0;
	row.createCell(colCount++).setCellValue("Row #");
	colCount = Helper.setCellValue(row, keyColumns, colCount);
	colCount = Helper.setCellValue(row, dataColumns, colCount);
	colCount++; // empty line between 2 sets of data
	colCount = Helper.setCellValue(row, dataColumns, colCount);
	Helper.setRowStyle(workbook, true, false, row);
	diff.createFreezePane(keyColumns.size() + 1, rowCount);

	for (Entry<Integer, Pair<List<String>, List<String>>> entry : this.getDiff().entrySet()) {
	    row = diff.createRow(rowCount++);
	    colCount = 0;
	    row.createCell(colCount++).setCellValue(entry.getKey());
	    colCount = Helper.setCellValue(row, entry.getValue().getLeft(), colCount);
	    colCount++; // empty line between 2 sets of data
	    colCount = Helper.setCellValue(row, entry.getValue().getRight(), colCount);
	}

	for (int i = 0; i < colCount; i++) {
	    diff.autoSizeColumn(i);
	}
    }
}
