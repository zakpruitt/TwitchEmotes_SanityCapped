package gg.sanitycapped.emotes.emote;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * SQLite, through JdbcClient. "Live" means pending or approved: rejected rows
 * are invisible to every lookup, which is what frees their name and hash again.
 */
@Repository
public class EmoteRepository {

    private static final String COLUMNS = """
            id, name, ext, sha256, dhash, uploader, status, note, animated, bytes,
            created_at, decided_at, github_sha
            """;

    private static final String LIVE = "status IN ('pending','approved')";

    private final JdbcClient db;

    EmoteRepository(JdbcClient db) {
        this.db = db;
    }

    public void insert(Emote e) {
        db.sql("""
                INSERT INTO emotes (id, name, ext, sha256, dhash, uploader, status, note, animated,
                                    bytes, created_at, decided_at, github_sha)
                VALUES (:id, :name, :ext, :sha, :dhash, :uploader, :status, :note, :animated,
                        :bytes, :createdAt, :decidedAt, :githubSha)
                """)
                .param("id", e.id())
                .param("name", e.name())
                .param("ext", e.ext())
                .param("sha", e.sha256())
                .param("dhash", e.dhash())
                .param("uploader", e.uploader())
                .param("status", e.status().value())
                .param("note", e.note())
                .param("animated", e.animated())
                .param("bytes", e.bytes())
                .param("createdAt", e.createdAt().getEpochSecond())
                .param("decidedAt", epochOrNull(e.decidedAt()))
                .param("githubSha", e.githubSha())
                .update();
    }

    public Optional<Emote> byId(String id) {
        return select("WHERE id = :id").param("id", id).query(EmoteRepository::toEmote).optional();
    }

    public Optional<Emote> liveByName(String name) {
        return select("WHERE name = :name AND " + LIVE)
                .param("name", name).query(EmoteRepository::toEmote).optional();
    }

    public Optional<Emote> liveBySha(String sha256) {
        return select("WHERE sha256 = :sha AND " + LIVE)
                .param("sha", sha256).query(EmoteRepository::toEmote).optional();
    }

    public List<Emote> live() {
        return select("WHERE " + LIVE).query(EmoteRepository::toEmote).list();
    }

    public List<Emote> approvedNewestFirst() {
        return select("WHERE status = 'approved' ORDER BY created_at DESC")
                .query(EmoteRepository::toEmote).list();
    }

    public List<Emote> pendingOldestFirst() {
        return select("WHERE status = 'pending' ORDER BY created_at ASC")
                .query(EmoteRepository::toEmote).list();
    }

    public void approve(String id, String name, String githubSha, Instant at) {
        db.sql("""
                UPDATE emotes
                   SET status = 'approved', name = :name, note = NULL,
                       github_sha = :sha, decided_at = :at
                 WHERE id = :id
                """)
                .param("name", name).param("sha", githubSha)
                .param("at", at.getEpochSecond()).param("id", id)
                .update();
    }

    public void reject(String id, String reason, Instant at) {
        db.sql("UPDATE emotes SET status = 'rejected', note = :note, decided_at = :at WHERE id = :id")
                .param("note", reason).param("at", at.getEpochSecond()).param("id", id)
                .update();
    }

    public void delete(String id) {
        db.sql("DELETE FROM emotes WHERE id = :id").param("id", id).update();
    }

    public int uploadsSince(String ip, Instant since) {
        return db.sql("SELECT COUNT(*) FROM upload_log WHERE ip = :ip AND at > :since")
                .param("ip", ip).param("since", since.getEpochSecond())
                .query(Integer.class).single();
    }

    public void logUpload(String ip, Instant at) {
        db.sql("INSERT INTO upload_log (ip, at) VALUES (:ip, :at)")
                .param("ip", ip).param("at", at.getEpochSecond())
                .update();
    }

    private JdbcClient.StatementSpec select(String where) {
        return db.sql("SELECT " + COLUMNS + " FROM emotes " + where);
    }

    private static Emote toEmote(ResultSet rs, int rowNum) throws SQLException {
        return new Emote(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("ext"),
                rs.getString("sha256"),
                rs.getString("dhash"),
                rs.getString("uploader"),
                EmoteStatus.of(rs.getString("status")),
                rs.getString("note"),
                rs.getBoolean("animated"),
                rs.getLong("bytes"),
                Instant.ofEpochSecond(rs.getLong("created_at")),
                instantOrNull(rs, "decided_at"),
                rs.getString("github_sha"));
    }

    private static Instant instantOrNull(ResultSet rs, String column) throws SQLException {
        long seconds = rs.getLong(column);
        return rs.wasNull() ? null : Instant.ofEpochSecond(seconds);
    }

    private static Long epochOrNull(Instant instant) {
        return instant == null ? null : instant.getEpochSecond();
    }
}
