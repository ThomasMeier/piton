insert into table_one (id, nammen) values (4, 'a');
insert into table_one (id, nammen) values (6, 'b');
-- rollback
delete from table_one where id = 4;
delete from table_one where id = 6;
