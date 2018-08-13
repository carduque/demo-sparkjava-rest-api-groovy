FROM base_lib

WORKDIR /

ADD target/demo-sparkjava-rest-api-groovy-1.0-SNAPSHOT.jar main.jar
ADD target/lib lib

ENV GREETING_MESSAGE Hello there!

EXPOSE 4567

CMD java -jar main.jar

