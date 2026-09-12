CREATE TABLE IF NOT EXISTS emotes (
  id          TEXT PRIMARY KEY,
  name        TEXT NOT NULL,
  ext         TEXT NOT NULL,
  sha256      TEXT NOT NULL,
  dhash       TEXT,
  uploader    TEXT NOT NULL,
  status      TEXT NOT NULL CHECK (status IN ('pending','approved','rejected')),
  note        TEXT,
  animated    INTEGER NOT NULL DEFAULT 0,
  bytes       INTEGER,
  created_at  INTEGER NOT NULL,
  decided_at  INTEGER,
  github_sha  TEXT
);

-- Rejected rows keep their name and hash free for someone to try again.
CREATE UNIQUE INDEX IF NOT EXISTS emotes_name_live
  ON emotes(name) WHERE status IN ('pending','approved');
CREATE UNIQUE INDEX IF NOT EXISTS emotes_sha_live
  ON emotes(sha256) WHERE status IN ('pending','approved');
CREATE INDEX IF NOT EXISTS emotes_status ON emotes(status, created_at);

CREATE TABLE IF NOT EXISTS upload_log (
  ip TEXT NOT NULL,
  at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS upload_log_ip ON upload_log(ip, at);
