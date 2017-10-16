BEGIN;
SET CONSTRAINTS ALL DEFERRED;
DROP TABLE IF EXISTS "observation";
DROP TABLE IF EXISTS "variabledef";
DROP TABLE IF EXISTS "datastream";
DROP TABLE IF EXISTS "sensorsystem";
COMMIT;

CREATE TABLE "sensorsystem" (
    sysid             VARCHAR(255) PRIMARY KEY,
    name              VARCHAR(255),
    description       TEXT,
    pushEvents        BOOLEAN,
    centerLat         double precision,
    centerLon         double precision,
    zoom              INTEGER,
    clickListener     VARCHAR(255)
);

CREATE TABLE "datastream" (
    sysid             VARCHAR(255),
    strid             VARCHAR(255),
    name              VARCHAR(255),
    description       TEXT,
    mapStyle          TEXT,
    zOrder            INT,
    chartStyle        TEXT,
    PRIMARY KEY (sysid, strid),
    constraint fk_ds_ss foreign key (sysid) references sensorsystem (sysid)
);

CREATE TABLE "variabledef" (
    sysid             VARCHAR(255),
    strid             VARCHAR(255),
    name              VARCHAR(255),
    units             VARCHAR(255),
    chartStyle        TEXT,
    PRIMARY KEY (sysid, strid, name),
    constraint fk_ds_ss foreign key (sysid, strid) references datastream (sysid, strid)
);

CREATE TABLE "observation" (
    sysid             VARCHAR(255) NOT NULL,
    strid             VARCHAR(255) NOT NULL,
    time              VARCHAR(255) NOT NULL,
    feature           TEXT,
    geometry          TEXT,
    -- scalarData:
    vars    VARCHAR(255)[],
    vals    double precision[],
    lat     double precision,
    lon     double precision,
    --
    constraint fk_ds_ss foreign key (sysid, strid) references datastream (sysid, strid)
);
