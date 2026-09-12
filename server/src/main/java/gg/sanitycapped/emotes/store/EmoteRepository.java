package gg.sanitycapped.emotes.store;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class EmoteRepository {

    private static final String COLUMNS =
            "id, name, ext, sha256, dhash, uploader, status, note, animated, bytes, created_at, decided_at, github_sha";

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
                .param("id", e.id()).param("name", e.name()).param("ext", e.ext())
                .param("sha", e.sha256()).param("dhash", e.dhash()).param("uploader", e.uploader())
                .param("status", e.status()).param("note", e.note())
                .param("animated", e.animated() ? 1 : 0).param("bytes", e.bytes())
                .param("createdAt", e.createdAt()).param("decidedAt", e.decidedAt())
                .param("githubSha", e.githubSha())
                .update();
    }

    public Optional<Emote> byId(String id) {
        return db.sql("SELECT " + COLUMNS + " FROM emotes WHERE id = :id")
                .param("id", id).query(EmoteRepository::map).optional();
    }

    /** Rejected rows are ignored everywhere, which frees their name and hash again. */
    public Optional<Emote> liveByName(String name) {
        return db.sql("SELECT " + COLUMNS + " FROM emotes WHERE name = :name AND status IN ('pending','approved')")
                .param("name", name).query(EmoteRepository::map).optional();
    }

    public Optional<Emote> liveBySha(String sha) {
        return db.sql("SELECT " + COLUMNS + " FROM emotes WHERE sha256 = :sha AND status IN ('pending','approved')")
                .param("sha", sha).query(EmoteRepository::map).optional();
    }

    public List<Emote> byStatus(String status) {
        return db.sql("SELECT " + COLUMNS + " FROM emotes WHERE status = :status ORDER BY created_at DESC")
                .param("status", status).query(EmoteRepository::map).list();
    }

    public List<Emote> pendingOldestFirst() {
        return db.sql("SELECT " + COLUMNS + " FROM emotes WHERE status = 'pending' ORDER BY created_at ASC")
                .query(EmoteRepository::map).list();
    }

    public List<Emote> all() {
        return db.sql("SELECT " + COLUMNS + " FROM emotes ORDER BY created_at DESC LIMIT 500")
                .query(EmoteRepository::map).list();
    }

    public List<Emote> liveWithDhash() {
        return db.sql("SELECT " + COLUMNS + " FROM emotes WHERE dhash IS NOT NULL AND status IN ('pending','approved')")
                .query(EmoteRepository::map).list();
    }

    public void approve(String id, String name, String githubSha, long at) {
        db.sql("""
                UPDATE emotes SET status = 'approved', name = :name, note = NULL,
                                  github_sha = :sha, decided_at = :at
                WHERE id = :id
                """)
                .param("name", name).param("sha", githubSha).param("at", at).param("id", id)
                .update();
    }

    public void reject(String id, String reason, long at) {
        db.sql("UPDATE emotes SET status = 'rejected', note = :note, decided_at = :at WHERE id = :id")
                .param("note", reason).param("at", at).param("id", id).update();
    }

    public void delete(String id) {
        db.sql("DELETE FROM emotes WHERE id = :id").param("id", id).update();
    }

    public int uploadsSince(String ip, long since) {
        return db.sql("SELECT COUNT(*) FROM upload_log WHERE ip = :ip AND at > :since")
                .param("ip", ip).param("since", since).query(Integer.class).single();
    }

    public void logUpload(String ip, long at) {
        db.sql("INSERT INTO upload_log (ip, at) VALUES (:ip, :at)")
                .param("ip", ip).param("at", at).update();
    }

    private static Emote map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        long decided = rs.getLong("decided_at");
        return new Emote(
                rs.getString("id"), rs.getString("name"), rs.getString("ext"), rs.getString("sha256"),
                rs.getString("dhash"), rs.getString("uploader"), rs.getString("status"), rs.getString("note"),
                rs.getInt("animated") == 1, rs.getLong("bytes"), rs.getLong("created_at"),
                rs.wasNull() ? null : decided, rs.getString("github_sha"));
    }
}
