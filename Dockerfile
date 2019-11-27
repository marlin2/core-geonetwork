FROM maven:3.6.2-jdk-8
MAINTAINER Chris Jackett <chris.jackett@csiro.au>

RUN apt-get update -y

RUN mkdir -p /usr/src
WORKDIR /usr/src
COPY . /usr/src

RUN cd schemas/iso19115-3 && git submodule update --init --recursive
RUN mvn clean install -DskipTests

WORKDIR /usr/src/web
CMD ["mvn", "jetty:run"]
