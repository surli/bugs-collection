DROP INDEX if exists IDX_georep_slave_volume_name;
CREATE INDEX IDX_georep_slave_volume_name ON gluster_georep_session(slave_volume_name);
