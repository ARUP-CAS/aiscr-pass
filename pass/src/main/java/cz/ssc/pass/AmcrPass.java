package cz.ssc.pass;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;

@WebServlet(name = "AmcrPass", urlPatterns = {"/check"})
public class AmcrPass extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(AmcrPass.class.getName());

    private static final int AUTH_NONE = 0;

    private static final int AUTH_PASSIVE = 1;

    private static final int AUTH_ADMIN = 4;

    private Options options;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // https://stackoverflow.com/questions/1911253/the-infamous-java-sql-sqlexception-no-suitable-driver-found
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.SEVERE, "DB config error", ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ServletOutputStream out = response.getOutputStream();
            out.print("Database failed");
            return;
        }

        synchronized (this) {
            if (options == null) {
                options = new Options(getServletContext());
            }
        }

        StringBuffer jb = new StringBuffer();
        String line;
        BufferedReader reader = request.getReader();
        while ((line = reader.readLine()) != null) {
            jb.append(line);
        }

        String reqBody = jb.toString();
        try {
            JSONObject reqJson = new JSONObject(reqBody);
            JSONObject rspJson = authenticate(reqJson);

            response.setContentType("application/json;charset=UTF-8");
            ServletOutputStream out = response.getOutputStream();
            out.print(rspJson.toString());
        } catch (JSONException ex) {
            LOGGER.log(Level.SEVERE, "invalid input: " + reqBody, ex);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ServletOutputStream out = response.getOutputStream();
            out.print("cannot parse JSON body: " + ex.getMessage());
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE, "DB error", ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ServletOutputStream out = response.getOutputStream();
            out.print("Database request failed");
        } catch (EmptyArgumentException ex) {
            LOGGER.log(Level.SEVERE, "empty input: " + reqBody, ex);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            ServletOutputStream out = response.getOutputStream();
            out.print("cannot parse parameters: " + ex.getMessage());
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.log(Level.SEVERE, "crypto config error", ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            ServletOutputStream out = response.getOutputStream();
            out.print("Internal cryptography error");
        }
    }

    private JSONObject authenticate(JSONObject reqJson)
            throws JSONException, SQLException, EmptyArgumentException, NoSuchAlgorithmException {
        JSONObject rspJson = new JSONObject();

        String hash = Util.makeSHA1(reqJson.getString("pwd"));
        try (Connection conn = DriverManager.getConnection(
                options.getDbUrl(), options.getDbUser(), options.getDbPwd())) {
            String access = getAccess(conn, reqJson.getString("user"), hash);
            if (access == null) {
                rspJson.put("error", "unknown");
            } else {
                rspJson.put("auth", access);
            }
        }

        return rspJson;
    }

    private static String getAccess(Connection conn, String user, String hash)
            throws SQLException {
        String sql = "select user_group_auth_storage.auth_level\n" +
                "from atree\n" +
                "join user_storage on atree.id = user_storage.id\n" +
                "left join user_group_auth_storage on atree.id=user_group_id\n" +
                "where (item_type = -2) and\n" +
                "(lower(caption) = ?) and (pasw = ?)\n" +
                "order by caption";
        try (PreparedStatement stm = conn.prepareStatement(sql)) {
            stm.setString(1, user.toLowerCase());
            stm.setString(2, hash);
            ResultSet res = stm.executeQuery();
            while (res.next()) {
                // getInt() doesn't distinguish 0 from NULL, but
                // in this case it doesn't matter
                int level = res.getInt(1);
                if ("admin".equals(user)) {
                    level = AUTH_ADMIN;
                } else {
                    if (level == AUTH_NONE) {
                        return null;
                    }
                }

                // If getAuthLevel had been ported, calling it here
                // would have matched either admin, or a row in
                // user_group_auth_storage (because the level check
                // above passed). Moreover, it would not have matched
                // any group membership, because there are no nodes in
                // atree of item_type PID_USERS_IN_GROUP and therefore
                // (theoretically ported) isMember would never return
                // true. So, it's really unnecessary to call, and ipso
                // facto port that stuff...
                return convertAuthLevel(level);
                // also, not updating stats_login on success - nobody
                // uses that...
            }
        }

        return null;
    }

    private static String convertAuthLevel(int level) {
        // not quite clear how to interpret higher numbers, but even
        // if they're bitmasks, all found in DB have bit 4 set...
        if (level > AUTH_ADMIN) {
            level = AUTH_ADMIN;
        }

        if (level >= AUTH_PASSIVE) {
            return String.valueOf((char)('A' + level));
        } else {
            return null;
        }
    }
}
