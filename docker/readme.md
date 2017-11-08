Set environment variables

    POSTGRES_DATA="$(pwd)/postgres_data"
    GPDVIZ_DB_USERNAME="gpdviz"
    GPDVIZ_DB_USERPASS=?
    GPDVIZ_HOST_POSTGRES_PORT=5432    
    GPDVIZ_VERSION=0.3.2
    GPDVIZ_CONF_DIR="$(pwd)/docker/conf"

Assuming those variables are set in `setenv.sh`:
    
    source setenv.sh
    
Build images and launch services:    

    cd ..
    docker-compose up --build
