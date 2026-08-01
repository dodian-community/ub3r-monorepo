FROM eclipse-temurin:21-jre

EXPOSE 43594
EXPOSE 8080

RUN mkdir /app
WORKDIR /app

# Copy the necessary server data
COPY ./data/cache ./data/cache
COPY ./data/world ./data/world
COPY ./data/shops.cfg ./data/shops.cfg
COPY ./data/mappings ./data/mappings
COPY ./Settings.toml ./Settings.toml

# Copy the server application jar file
COPY ./build/libs/*.jar ./ub3r-server.jar

ENTRYPOINT ["java", \
	"-Xms1g", \
	"-Xmx4g", \
	"-XX:+UseZGC", \
	"-XX:+ZGenerational", \
	"-Djava.security.egd=file:/dev/./urandom", \
	"-Ddodian.log.level=info", \
	"-jar", \
	"./ub3r-server.jar" \
]

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD test -f /tmp/ub3r-ready || exit 1
