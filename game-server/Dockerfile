FROM eclipse-temurin:17-jre

EXPOSE 43594
EXPOSE 8080

RUN mkdir /app
WORKDIR /app

# Copy the necessary server data
COPY ./data/cache ./data/cache
COPY ./data/world ./data/world
COPY ./data/shops.cfg ./data/shops.cfg

# Copy the server application jar file
COPY ./build/libs/*.jar ./ub3r-server.jar

ENTRYPOINT ["java", \
	"-Xms1g", \
	"-Xmx4g", \
	"-XX:+UseG1GC", \
	"-XX:MaxGCPauseMillis=100", \
	"-Djava.security.egd=file:/dev/./urandom", \
	"-Ddodian.log.level=info", \
	"-jar", \
	"./ub3r-server.jar" \
]
