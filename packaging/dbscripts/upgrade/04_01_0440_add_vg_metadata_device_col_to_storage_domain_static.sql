select fn_db_add_column('storage_domain_static', 'vg_metadata_device', 'VARCHAR(100) DEFAULT NULL REFERENCES LUNS(lun_id)');
