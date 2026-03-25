package com.campus.love.devtools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 一次性迁移：将 {@code uploads/} 根目录平铺的文件移到 {@code uploads/{userId}/}，并更新库内 URL。
 * <p>
 * 运行前<strong>停后端</strong>、备份数据库与 uploads。需与 .env 相同环境变量：
 * {@code DB_URL}、{@code DB_USERNAME}、{@code DB_PASSWORD}，以及上传根目录参数或 {@code APP_UPLOAD_PATH}。
 * <pre>
 *   cd campus-love-backend
 *   mvn -q compile exec:java -Dexec.mainClass=com.campus.love.devtools.MigrateFlatUploadsToUserDirs \
 *       -Dexec.args="/绝对路径/uploads"
 * </pre>
 */
public final class MigrateFlatUploadsToUserDirs {

    private static final Pattern COVER = Pattern.compile("^cover_(\\d+)_");
    private static final Pattern MOMENT = Pattern.compile("^moment_(\\d+)_");

    private MigrateFlatUploadsToUserDirs() {}

    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.cj.jdbc.Driver");
        String rootStr = args.length > 0 ? args[0]
                : System.getenv().getOrDefault("APP_UPLOAD_PATH",
                System.getProperty("user.home") + "/campus-love/uploads");
        Path root = Paths.get(rootStr).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            System.err.println("Not a directory: " + root);
            System.exit(1);
        }
        String url = Objects.requireNonNull(System.getenv("DB_URL"), "DB_URL");
        String user = Objects.requireNonNull(System.getenv("DB_USERNAME"), "DB_USERNAME");
        String pass = System.getenv().getOrDefault("DB_PASSWORD", "");

        Map<String, String> replacements = new LinkedHashMap<>();
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            collectMovesFromFlatFiles(root, conn, replacements);
        }

        if (replacements.isEmpty()) {
            System.out.println("No flat files to migrate (or all skipped).");
            return;
        }

        Map<String, String> ordered = sortByKeyLengthDesc(replacements);

        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            conn.setAutoCommit(false);
            try {
                applyFileMoves(root, ordered);
                updateDb(conn, ordered);
                conn.commit();
                System.out.println("Migration committed. Distinct URL pairs: " + ordered.size());
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static Path pathFromUploadsSuffix(Path root, String suffix) {
        Path p = root;
        for (String part : suffix.split("/")) {
            if (!part.isEmpty()) {
                p = p.resolve(part);
            }
        }
        return p;
    }

    private static void applyFileMoves(Path root, Map<String, String> ordered) throws IOException {
        for (Map.Entry<String, String> e : ordered.entrySet()) {
            String oldUrl = e.getKey();
            String newUrl = e.getValue();
            if (!oldUrl.startsWith("/uploads/") || !newUrl.startsWith("/uploads/")) {
                continue;
            }
            String oldSuffix = oldUrl.substring("/uploads/".length());
            String newSuffix = newUrl.substring("/uploads/".length());
            if (oldSuffix.contains("/")) {
                continue;
            }
            Path src = pathFromUploadsSuffix(root, oldSuffix);
            Path dest = pathFromUploadsSuffix(root, newSuffix);
            if (!Files.isRegularFile(src)) {
                System.err.println("Missing file, skip move: " + src);
                continue;
            }
            Files.createDirectories(dest.getParent());
            Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Moved: " + src.getFileName() + " -> " + dest);
        }
    }

    private static Map<String, String> sortByKeyLengthDesc(Map<String, String> m) {
        return m.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed())
                .collect(LinkedHashMap::new, (map, e) -> map.put(e.getKey(), e.getValue()), LinkedHashMap::putAll);
    }

    private static void collectMovesFromFlatFiles(Path root, Connection conn, Map<String, String> out)
            throws SQLException, IOException {
        try (Stream<Path> stream = Files.list(root)) {
            for (Path p : stream.toList()) {
                if (!Files.isRegularFile(p)) {
                    continue;
                }
                String name = p.getFileName().toString();
                if (name.startsWith(".")) {
                    continue;
                }
                Long uid = resolveOwnerUserId(conn, name);
                if (uid == null) {
                    System.err.println("Skip (cannot resolve user): " + name);
                    continue;
                }
                Path userDir = root.resolve(String.valueOf(uid));
                Files.createDirectories(userDir);
                Path dest = userDir.resolve(name);
                if (Files.exists(dest)) {
                    System.err.println("Skip move, target exists: " + dest);
                    continue;
                }
                String oldUrl = "/uploads/" + name;
                String newUrl = "/uploads/" + uid + "/" + name;
                out.put(oldUrl, newUrl);
            }
        }
    }

    private static Long resolveOwnerUserId(Connection conn, String filename) throws SQLException {
        if (filename.startsWith("thumb_feed_img_")) {
            filename = filename.substring("thumb_".length());
        }
        Matcher cm = COVER.matcher(filename);
        if (cm.find()) {
            return Long.parseLong(cm.group(1));
        }
        Matcher mm = MOMENT.matcher(filename);
        if (mm.find()) {
            return Long.parseLong(mm.group(1));
        }
        if (filename.startsWith("chat_")) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT sender_id FROM t_message WHERE content LIKE ? LIMIT 1")) {
                ps.setString(1, "%" + filename + "%");
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        }
        if (filename.startsWith("feed_img_") || filename.startsWith("feed_video_")) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT user_id FROM t_feed_post WHERE (images LIKE ? OR videos LIKE ?) LIMIT 1")) {
                String like = "%" + filename + "%";
                ps.setString(1, like);
                ps.setString(2, like);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getLong(1);
                    }
                }
            }
        }
        return null;
    }

    private static void updateDb(Connection conn, Map<String, String> ordered) throws SQLException {
        patchTextColumn(conn, "t_feed_post", "id", "images", ordered);
        patchTextColumn(conn, "t_feed_post", "id", "videos", ordered);
        patchTextColumn(conn, "t_feed_comment", "id", "images", ordered);
        patchTextColumn(conn, "t_user", "id", "cover_image_url", ordered);
        patchTextColumn(conn, "t_user", "id", "moment_photo_url", ordered);
        patchTextColumn(conn, "t_message", "id", "content", ordered);
    }

    private static void patchTextColumn(Connection conn, String table, String idCol, String col,
                                        Map<String, String> ordered) throws SQLException {
        String sql = "SELECT " + idCol + ", " + col + " FROM " + table + " WHERE " + col + " IS NOT NULL AND " + col + " <> ''";
        try (PreparedStatement sel = conn.prepareStatement(sql);
             ResultSet rs = sel.executeQuery()) {
            String upd = "UPDATE " + table + " SET " + col + " = ? WHERE " + idCol + " = ?";
            try (PreparedStatement up = conn.prepareStatement(upd)) {
                while (rs.next()) {
                    long id = rs.getLong(1);
                    String val = rs.getString(2);
                    if (val == null) {
                        continue;
                    }
                    String next = applyAll(val, ordered);
                    if (!next.equals(val)) {
                        up.setString(1, next);
                        up.setLong(2, id);
                        up.executeUpdate();
                    }
                }
            }
        }
    }

    private static String applyAll(String val, Map<String, String> ordered) {
        String s = val;
        for (Map.Entry<String, String> e : ordered.entrySet()) {
            if (s.contains(e.getKey())) {
                s = s.replace(e.getKey(), e.getValue());
            }
        }
        return s;
    }
}
