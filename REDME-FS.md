 
## Instructions


## Docker Setup

### Docker installation

```sh
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo usermod -aG docker $USER
newgrp docker
docker --version
```

### Build Docker Image

```sh
cd tika
mvn clean install -DskipTests
docker build --build-arg TIKA_VERSION=4.0.0-SNAPSHOT -t tika-fs .
docker save -o tika-fs.tar tika-fs:latest
docker tag tika-fs  hyoavh/tika-fs:<VERSION>
docker push hyoavh/tika-fs:<VERSION>
```

### Run Docker Container

```sh
docker load -i tika-fs.tar
docker run --name tika-server -d -p 9998:9998 tika-fs
```

---

### Install Sleuthkit JAR to Local Maven Repository

```sh
mvn install:install-file \
    -Dfile=/home/yoav/y_tika/jars/sleuthkit-4.10.1.jar \
    -DgroupId=org.sleuthkit \
    -DartifactId=sleuthkit \
    -Dversion=4.10.1 \
    -Dpackaging=jar
```

### Start Tika Server

```sh
java -jar tika-server/tika-server-standard/target/tika-server-standard-4.0.0-SNAPSHOT.jar
```

### Send Image to Tika

```sh
curl -T raw_ntfs_image.img http://localhost:9998/tika
```

---

## Build Latest Version of Sleuthkit

### Install Build Dependencies

```sh
sudo apt update
sudo apt install -y build-essential autoconf libtool automake git zip wget ant default-jdk \
    libpq-dev zlib1g-dev libafflib-dev libewf-dev libvhdi-dev libvmdk-dev libbfio-dev libsqlite3-dev
```

### Clone and Build Sleuthkit

```sh
git clone https://github.com/sleuthkit/sleuthkit.git
cd sleuthkit

./bootstrap
./configure --with-afflib --with-ewf --with-vhdi --with-vmdk --with-bfio --enable-java
make -j$(nproc)
sudo make install
```

### Copy Built Artifacts

```sh
cp bindings/java/dist/sleuthkit-4.10.1.jar ~/tika/tika-parsers/tika-parsers-standard/tika-parsers-standard-modules/tika-parser-fs-module/lib/
cp bindings/java/build/NATIVELIBS/x86_64/linux/libtsk_jni.so ~/tika/libs
```

---

## Update Project Configuration

- Update `tilpom.xml`:
    1. Set the correct Sleuthkit version.
    2. Add dependencies as listed in `bindings/java/ivy.xml`.

---

## Debug Docker

```sh
docker stop $(docker ps -q)
docker rm $(docker ps -aq)
docker build --build-arg TIKA_VERSION=4.0.0-SNAPSHOT -t tika-fs .
docker run --name tika-server -d -p 9998:9998 tika-fs
docker logs -f tika-server
```

## Final Steps

- Build the Docker image again and run tests.



