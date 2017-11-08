#!/bin/bash
set -e

echo "gpdviz: moving pg_hba.conf to pg_hba.conf.bak ..."
mv ${PGDATA}/pg_hba.conf ${PGDATA}/pg_hba.conf.bak
echo "gpdviz: setting pg_hba.conf ..."
echo -e "\
# === STOQS ========================\n\
host    gpdviz       gpdviz     all  md5\n\
host    gpdviz_test  gpdviz     all  md5\n\
host    all          postgres   all  md5\n\
local   all          all             trust" > ${PGDATA}/pg_hba.conf
