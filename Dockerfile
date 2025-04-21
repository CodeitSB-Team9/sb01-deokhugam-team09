FROM amazoncorretto:17 AS builder

WORKDIR /app

COPY gradle ./gradle
COPY gradlew ./gradlew
COPY build.gradle settings.gradle ./

RUN chmod +x ./gradlew
RUN ./gradlew dependencies

COPY src ./src

RUN ./gradlew build -x test

FROM amazoncorretto:17-alpine-jdk

WORKDIR /app

ENV JVM_OPTS="-Xms512m -Xmx1024m"

COPY --from=builder /app/build/libs/deokhugam.jar ./

ENTRYPOINT ["sh", "-c", "java ${JVM_OPTS} -jar build/libs/deokhugam.jar"]
