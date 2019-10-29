package likeabaos.tools.util.csv.compare;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;
import picocli.CommandLine.Option;

@Command(name = "Utility Template",
	mixinStandardHelpOptions = true,
	versionProvider = App.VersionProvider.class,
	description = "A template for Java utility program.")
public class App implements Callable<Integer> {
    private final static Logger LOG = LogManager.getLogger();
    private final static CommandLine CLI = new CommandLine(new App());

    public static void main(String[] args) throws Exception {
	int exitCode = CLI.execute(args);
	System.exit(exitCode);
    }

    @Option(names = { "-c", "--config" }, required = true, description = "The full path to configuration file.")
    private File config_file;

    @Option(names = { "-l", "--left" }, required = true, description = "The file on the left side to compare.")
    private File fileLeft;

    @Option(names = { "-r", "--right" }, required = true, description = "The file on the right side to compare.")
    private File fileRight;

    @Option(names = { "-m", "--columns" },
	    description = "The columns to use. Not specified or empty or star(*) means all columns on the left file.")
    private String dataColumns;

    @Option(names = { "-k", "--keys" },
	    description = "The key columns to use to be used when comparing. Not specified or empty or star(*) means all columns on the left file.")
    private String keyColumns;

    @Option(names = { "-o", "--output" },
	    description = "The file to save the result of the compare. Not specified or empty means using the default specified in the config file.")
    private File fileOutput;

    private Properties properties;

    @Override
    public Integer call() throws Exception {
	int errorCode = 0;
	try {
	    LOG.info("Program started");
	    this.loadConfigFile();
	    LOG.info("Starting process...");
	    Processor p = new Processor(this);
	    p.start(this.fileLeft, this.fileRight, this.dataColumns, this.keyColumns, this.fileOutput);
	    LOG.info("Program completed");
	} catch (Exception e) {
	    LOG.error("Program encounter fatal error, not able to continue", e);
	    errorCode = 1;
	}
	return errorCode;
    }

    public void loadConfigFile() throws FileNotFoundException, IOException {
	if (this.config_file == null)
	    throw new IllegalStateException("Need to specify a config file. See help with -h option.");
	LOG.info("Loading configuration from file: {}", this.config_file.getAbsolutePath());
	try (FileReader reader = new FileReader(this.config_file)) {
	    this.properties = new Properties();
	    this.properties.load(reader);
	}
    }

    public Properties getProperties() {
	return this.properties;
    }

    public static class VersionProvider implements IVersionProvider {
	@Override
	public String[] getVersion() throws Exception {
	    try (InputStream input = this.getClass().getClassLoader().getResourceAsStream("build.properties")) {
		Properties prop = new Properties();
		prop.load(input);
		return new String[] { prop.getProperty("app_version") };
	    }
	}
    }
}
