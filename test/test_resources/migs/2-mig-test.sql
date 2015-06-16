alter table table_one add column nammen varchar(1);
-- rollback
alter table table_one drop column nammen;
