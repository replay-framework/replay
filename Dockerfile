FROM openjdk:11

COPY . /build
WORKDIR /build

RUN ./gradlew
