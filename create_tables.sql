DROP TABLE "sensorsystem";

CREATE TABLE "sensorsystem" (
    sysid          VARCHAR(255) PRIMARY KEY,
    name           VARCHAR(255),
    description    VARCHAR(255),
    pushEvents     BOOLEAN,
    centerLat      FLOAT,
    centerLon      FLOAT,
    zoom           INTEGER,
    clickListener  VARCHAR(255)
);

DROP TABLE "datastream";

CREATE TABLE "datastream" (
    sysid          VARCHAR(255),
    strid          VARCHAR(255),
    name           VARCHAR(255),
    description    VARCHAR(255),
    mapStyle       VARCHAR(255),
    zOrder         INT,
    PRIMARY KEY (sysid, strid),
    constraint fk_ds_ss foreign key (sysid) references sensorsystem (sysid)
);

DROP TABLE "variabledef";

CREATE TABLE "variabledef" (
    sysid          VARCHAR(255),
    strid          VARCHAR(255),
    name           VARCHAR(255),
    units          VARCHAR(255),
    chartStyle     VARCHAR(255),
    PRIMARY KEY (sysid, strid),
    constraint fk_ds_ss foreign key (sysid, strid) references datastream (sysid, strid)
);
