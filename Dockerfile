FROM ghcr.io/sen2vm/sen2vm-build-env:latest AS launcher

ENV SEN2VM_VERSION=1.1.3

WORKDIR /Sen2vm

RUN curl -L -o sen2vm-core.jar https://github.com/sen2vm/sen2vm-core/releases/download/${SEN2VM_VERSION}/sen2vm-core-${SEN2VM_VERSION}.jar

ENTRYPOINT ["java", "-jar","sen2vm-core.jar"]