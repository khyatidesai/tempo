select count(taskid) from tempo_task where id=(select id from tempo_pa where output_xml like ? and not state=1); 