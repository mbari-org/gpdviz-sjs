Set required environment variables indicated in `setenv.template.sh`
in a local file `setenv.sh`:
    
    cp setenv.template.sh setenv.sh
    vi setenv.sh
    source setenv.sh
    
Build images:    

    cd ..
    docker/build.sh gpdviz_db
    docker/build.sh gpdviz

Launch gpdviz:    

    docker-compose up -d
