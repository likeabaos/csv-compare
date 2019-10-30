package likeabaos.tools.util.csv.compare;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.FileUtils;
import org.apache.poi.xssf.extractor.XSSFExcelExtractor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import picocli.CommandLine;
import picocli.CommandLine.MissingParameterException;

public class TestApp {

    @Test
    public void testLoadingProperties() throws FileNotFoundException, IOException {
	String[] args = { "-c", "src/test/resources/sample_config.properties", "-l", "left", "-r", "right" };
	App app = new App();
	CommandLine CLI = new CommandLine(app);
	CLI.parseArgs(args);
	app.loadConfigFile();
	String value = app.getProperties().getProperty("tesing_sample_config_start");
	assertEquals("The first line of the sample config file should be 001", "001", value);

	value = app.getProperties().getProperty("testing_sample_config_end");
	assertEquals("The last line of the sample config file should be 999", "999", value);

	value = app.getProperties().getProperty("testing_sample_config_string");
	assertEquals("this is a test", value);

	value = app.getProperties().getProperty("testing_sample_config_boolean");
	assertEquals("false", value);
    }

    @Test
    public void testExceptionReturnCode() throws Exception {
	String[] args = { "-c", "lalaland/lalaland/abcdef.xyz", "-l", "left", "-r", "right" }; // this does not exist
	App app = new App();
	CommandLine CLI = new CommandLine(app);
	CLI.parseArgs(args);
	int retCode = app.call();
	assertEquals(1, retCode); // bad config path, so this will have an 1
    }

    @Test(expected = MissingParameterException.class)
    public void testMissingRequiredArguments() throws Exception {
	String[] args = {};
	App app = new App();
	CommandLine CLI = new CommandLine(app);
	CLI.parseArgs(args);
    }

    @Test
    public void testGoodReturnCode() throws Exception {
	File testfile = new File("output/test-integrated-output.xlsx");
	if (testfile.exists())
	    FileUtils.deleteQuietly(testfile);

	String[] args = { "-c", "src/test/resources/sample_config.properties", "-l",
		"src/test/resources/test_data_2.csv", "-r", "src/test/resources/test_data_1.csv", "-k", "Book Name",
		"-o", testfile.getAbsolutePath() };
	App app = new App();
	CommandLine CLI = new CommandLine(app);
	CLI.parseArgs(args);
	int retCode = app.call();
	assertEquals(0, retCode);

	// Since the output is an excel file... the best way to compare this without
	// right a lot of code or using other third parties is to compare the string
	// extraction of a good file and the output.
	// Obviously the "good" file are prepared and inspected manually. This may not
	// test everything to the smallest unit, but at least we know if something
	// unexpected happened.
	String actual = null;
	try (InputStream inp = new FileInputStream(testfile);
		XSSFExcelExtractor extractor = new XSSFExcelExtractor(new XSSFWorkbook(testfile))) {
	    actual = extractor.getText();
	}
	assertNotNull(actual);

	String expected = null;
	String expectedFile = "src/test/resources/test-integrated-expected.xlsx";
	try (InputStream inp = new FileInputStream(expectedFile);
		XSSFExcelExtractor extractor = new XSSFExcelExtractor(new XSSFWorkbook(expectedFile))) {
	    expected = extractor.getText();
	}
	assertNotNull(expected);
	assertEquals(expected, actual);

	if (testfile.exists())
	    FileUtils.deleteQuietly(testfile);
    }
}
