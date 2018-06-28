## Build images

Cd to the root of this project:

    cd ..

To build gpdviz_db:

    docker/build.sh gpdviz_db

To build gpdviz:

    docker/build.sh gpdviz

## Launch gpdviz

Set required environment variables indicated in
`docker/setenv.template.sh`, for example:

    cd docker/
    cp setenv.template.sh setenv.sh
    vi setenv.sh
    source setenv.sh

    cd ..  # root again

    docker-compose up -d
