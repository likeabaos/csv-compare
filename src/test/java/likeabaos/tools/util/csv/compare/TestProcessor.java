package likeabaos.tools.util.csv.compare;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import picocli.CommandLine;

public class TestProcessor {
    private static App app;

    @BeforeClass
    public static void prep() throws Exception {
	String[] args = { "-c", "src/test/resources/sample_config.properties", "-l",
		"src/test/resources/test_data_1.csv", "-r", "src/test/resources/test_data_2.csv" };
	app = new App();
	CommandLine CLI = new CommandLine(app);
	CLI.parseArgs(args);
	app.loadConfigFile();
    }

    @Test
    public void testCompare() throws FileNotFoundException, IOException {
	Map<String, List<String>> leftData = Helper.loadCsvFile(new File("src/test/resources/test_data_1.csv"), null);
	Map<String, List<String>> rightData = Helper.loadCsvFile(new File("src/test/resources/test_data_2.csv"), null);
	Processor proc = new Processor(app);
	proc.compare("Book Name", leftData, rightData);
	assertEquals("{2=([How to program C++, , Cpp Guy, 2/1/1990, 56.75],[2, C++ Guy, 2/1/1990, 56.75])}",
		proc.getDiff().toString());
	assertEquals("{5=[Awesome Math, 5, Math Teacher, 1/1/2009, 20]}", proc.getRight().toString());
	assertEquals("{5=[Best Dishes, 6, A Chief, 1/1/1999, 7.99]}", proc.getLeft().toString());
    }

}
