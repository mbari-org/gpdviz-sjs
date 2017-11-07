Preliminaries for dockerized db, eventually to also include dockerized gpdviz.

For now, launch postgres with created gpdviz database: 

    vim setenv.sh  # to adjust env vars
    source setenv.sh
    docker-compose up --build
