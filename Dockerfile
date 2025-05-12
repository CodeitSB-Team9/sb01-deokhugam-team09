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

RUN apk add --no-cache tesseract-ocr

ENV JVM_OPTS="-Xms512m -Xmx1024m"

COPY --from=builder /app/build/libs/deokhugam.jar deokhugam.jar
COPY --from=builder /app/src/main/resources/ocr/tessdata ./tessdata
ENV TESSDATA_PREFIX=/app/tessdata/

ENTRYPOINT sh -c "java ${JVM_OPTS} -jar deokhugam.jar"
