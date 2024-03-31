FROM debian:stable AS build
WORKDIR /build
COPY . .
ADD https://download.oracle.com/java/22/latest/jdk-22_linux-x64_bin.deb /opt
RUN apt-get update && apt-get -y install /opt/jdk-22_linux-x64_bin.deb && apt-get install -y maven && apt-get clean
RUN mvn clean package -DskipTests=true

FROM debian:stable
WORKDIR /app
COPY --from=build /opt/jdk-22_linux-x64_bin.deb /opt
RUN apt-get update && apt-get install -y /opt/jdk-22_linux-x64_bin.deb && apt-get clean
COPY --from=build /build/target/timeslot-telegram-bot-0.0.1-SNAPSHOT.jar ./
EXPOSE 8083
ENTRYPOINT ["java", "-showversion", "-jar", "/app/timeslot-telegram-bot-0.0.1-SNAPSHOT.jar"]