#!/usr/bin/env bash
set -eu

userName=$1
userPass=$2

pgConn="-h localhost -U postgres"

function _main {
    _createUser
    _createDb   gpdviz
    _createDb   gpdviz_test
}

function _createUser {
    echo "_createUser $userName"
    psql $pgConn -tc "SELECT 1 FROM pg_user WHERE usename = '$userName'" |
         grep -q 1 || psql $pgConn -c "CREATE USER $userName WITH PASSWORD '$userPass'"
}

function _createDb {
    dbName=$1
    echo "_createDb $dbName"
    psql $pgConn -tc "SELECT 1 FROM pg_database WHERE datname = '$dbName'" |
         grep -q 1 || psql $pgConn -c "CREATE DATABASE $dbName"
    psql $pgConn -tc "ALTER DATABASE $dbName SET TIMEZONE='GMT'"
    echo "_grantAll dbName=$dbName userName=$userName"
    psql $pgConn -tc "GRANT ALL PRIVILEGES ON DATABASE $dbName TO $userName"
}

_main
