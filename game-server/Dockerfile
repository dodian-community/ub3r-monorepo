FROM openjdk:11-jre-slim

EXPOSE 43594

RUN mkdir /app
WORKDIR /app

# Copy the necessary server data
COPY ./data/cache ./data/cache
COPY ./data/world ./data/world
COPY ./data/shops.cfg ./data/shops

# Copy the server application jar file
COPY ./build/libs/*.jar ./ub3r-server.jar

ENTRYPOINT ["java", \
	"-XX:+UnlockExperimentalVMOptions", \
	"-Djava.security.egd=file:/dev/./urandom","-jar", \
	"./ub3r-server.jar" \
]