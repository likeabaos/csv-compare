package likeabaos.tools.util.csv.compare;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class TestHelper {

    @Test
    public void testFormatTimestampPrintable() throws ParseException {
	Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2019-05-12 13:50:59");
	String value = Helper.formatTimestampPrintable(date.getTime());
	assertEquals("2019-05-12_13-50-59-000", value);
    }

    @Test
    public void testLoadCsvFileWithNullColumn() throws FileNotFoundException, IOException {
	File file = new File("src/test/resources/test_data_1.csv");
	String columns = null;
	Map<String, List<String>> data = Helper.loadCsvFile(file, columns);
	validateCsvFileData(data);
    }

    @Test
    public void testLoadCsvFileWithStarColumn() throws FileNotFoundException, IOException {
	File file = new File("src/test/resources/test_data_1.csv");
	String columns = "*";
	Map<String, List<String>> data = Helper.loadCsvFile(file, columns);
	validateCsvFileData(data);
    }

    private void validateCsvFileData(Map<String, List<String>> data) {
	assertEquals(5, data.size());
	assertEquals("[ID, Book Name, Author, Release Date, Cost]", data.keySet().toString());

	for (List<String> column : data.values()) {
	    assertEquals(4, column.size());
	}
	// null cell should result having the same row count
	assertEquals("[1, , 3, 4]", data.get("ID").toString());
    }

    @Test
    public void testLoadCsvFileWithSpecifiedColumns() throws FileNotFoundException, IOException {
	File file = new File("src/test/resources/test_data_1.csv");
	String columns = "ID, Book Name";
	Map<String, List<String>> data = Helper.loadCsvFile(file, columns);

	assertEquals(2, data.size());
	assertEquals("[ID, Book Name]", data.keySet().toString());

	for (List<String> column : data.values()) {
	    assertEquals(4, column.size());
	}
	// null cell should result having the same row count
	assertEquals("[1, , 3, 4]", data.get("ID").toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadCsvFileWithSpecifiedColumnsMismatchedCases() throws FileNotFoundException, IOException {
	File file = new File("src/test/resources/test_data_1.csv");
	String columns = "id, book name";
	Helper.loadCsvFile(file, columns);
    }

    @Test
    public void testGetDataByKeys() throws FileNotFoundException, IOException {
	File file = new File("src/test/resources/test_data_1.csv");
	String columns = "*";
	Map<String, List<String>> data = Helper.loadCsvFile(file, columns);
	List<String> keys = Helper.getCSV("ID, Book Name");
	Map<List<String>, List<String>> byKeys = Helper.getDataByKeys(keys, data);
	assertEquals("[[1, How to program Java], [, How to program C++], [3, Programming Tips], [4, My Back Yard]]",
		byKeys.keySet().toString());
	assertEquals(
		"[[Java Guy, 1/1/2011, 25.99], [Cpp Guy, 2/1/1990, 56.75], [A Coder, 3/12/2012, 25.99], [Random Person, 9/9/2019, 5]]",
		byKeys.values().toString());
    }
}
