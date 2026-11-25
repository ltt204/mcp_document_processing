CLIENT_JAR = mcp-client/target/mcp-client-1.0-SNAPSHOT.jar
WORKER_JAR = mcp-worker/target/mcp-worker-1.0-SNAPSHOT.jar

bundle:
	mvn clean package

run:
	java -jar $(CLIENT_JAR)

run-worker:
	java -jar $(WORKER_JAR)

mcp-inspector:
	npx --yes --package=@modelcontextprotocol/inspector mcp-inspector java -jar $(CLIENT_JAR)
