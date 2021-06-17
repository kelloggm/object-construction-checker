docker rmi --force resource_leak
docker build --no-cache -t resource_leak .
docker run resource_leak
