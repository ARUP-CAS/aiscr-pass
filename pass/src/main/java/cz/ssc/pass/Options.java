package cz.ssc.pass;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

public class Options {
    private static final Logger LOGGER = Logger.getLogger(Options.class.getName());

    private final String dbUrl;

    private final String dbUser;

    private final String dbPwd;

    public Options(ServletContext context) throws IOException {
        String confDir = context.getInitParameter("confdir");
        if ((confDir == null) || confDir.isEmpty()) {
            confDir = "/var/lib/archeo/amcr";
        }

        LOGGER.info(String.format("conf dir = %s", confDir));
        Path confPath = Paths.get(confDir, "amcr-pass.json");
        File confFile = new File(confPath.toString());
        String confStr = FileUtils.readFileToString(confFile, "UTF-8");
        JSONObject json = new JSONObject(confStr);

        dbUrl = json.getString("dburl");
        dbUser = json.getString("dbuser");
        dbPwd = json.getString("dbpwd");
    }

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPwd() {
        return dbPwd;
    }
}
