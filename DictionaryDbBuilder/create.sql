CREATE TABLE dictionary (
    rowid       INTEGER PRIMARY KEY AUTOINCREMENT,
    simplified  TEXT,
    traditional TEXT,
    pinyin      TEXT,
    definition  TEXT,
    classifier  TEXT,
    hsk_level	INTEGER,
    part_of_speech BLOB,
    frequency	REAL,
    concept		TEXT,
    topic		TEXT,
    parent_topic	TEXT,
    notes		TEXT
);

CREATE INDEX idx_simplified ON dictionary(simplified);
CREATE INDEX idx_traditional ON dictionary(traditional);

CREATE INDEX idx_simplified_pinyin ON dictionary(simplified, pinyin);