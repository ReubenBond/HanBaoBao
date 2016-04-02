drop index idx_simplified_pinyin;
drop index idx_simplified;
drop index idx_traditional;

create virtual table fts_definition using fts4 (content='dictionary', definition);
insert into fts_definition(rowid, definition) select rowid, definition from dictionary;
insert into fts_definition(fts_definition) values ('optimize');

create virtual table fts_prefix using fts4 (term, frequency, pinyin);
insert into fts_prefix select simplified, frequency, pinyin from (select * from dictionary where simplified not null order by hsk_level not null asc, hsk_level desc, part_of_speech not null desc, frequency desc, rowid desc) group by simplified;
insert into fts_prefix select traditional, frequency, pinyin from (select * from dictionary where traditional not null and not exists (SELECT 1 FROM fts_prefix where fts_prefix match traditional) order by hsk_level not null asc, hsk_level desc, part_of_speech not null desc, frequency desc, rowid desc) group by traditional;
insert into fts_prefix (fts_prefix) values('optimize');

ANALYZE;
vacuum;