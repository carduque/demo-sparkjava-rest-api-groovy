package demo

import groovy.json.JsonBuilder
import groovy.transform.CompileStatic
import org.joda.time.DateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spark.Request
import spark.Response

import static spark.Spark.*

@CompileStatic
class Main {

    public static final Logger log = LoggerFactory.getLogger(Main.class)
    static final String ERROR_MSG_TO_USER = 'Bad configuration. Check logs.'

    static String GREETING_MESSAGE
    static boolean DEBUG_FLAG

    static void main(String[] args) throws Exception {
        // ----------------------------------------------------------------------------------------------
        // Config and init stuff
        // ----------------------------------------------------------------------------------------------

        GREETING_MESSAGE = getEnvironmentVariableOrHalt('GREETING_MESSAGE', ERROR_MSG_TO_USER, 'ERROR: Missing GREETING_MESSAGE environment variable')
        DEBUG_FLAG = getEnvironmentVariable('DEBUG_FLAG') != null

        final listenPort = (getEnvironmentVariable('LISTEN_PORT') ?: '4567') as int
        port(listenPort)

        // ----------------------------------------------------------------------------------------------
        // General HTTP error handling
        // ----------------------------------------------------------------------------------------------

        internalServerError({ Request req, Response res ->
            // Make sure:
            //   1. JSON response
            //   2. Do not expose sensitive info!

            res.type("application/json");
            return '{"error": "500 Internal Server Error. Check server logs."}'
        })

        notFound({ Request req, Response res ->
            res.type("application/json");
            return '{"error": "404 Not Found"}'
        })

        // ----------------------------------------------------------------------------------------------
        // Home page - human readable HTML
        // ----------------------------------------------------------------------------------------------

        get('/', { Request req, Response res ->
            res.header('Content-Type', 'text/html')
            '''<html><body>
                REST API is up.<p>
                
                Available endpoints: <p>
                POST <a href="/api/v1/helloTo">/api/v1/helloTo</a><p/>
                GET  <a href="/api/v1/greeting">/api/v1/greeting</a><p/>
                </body></html>
                '''
        })

        // ----------------------------------------------------------------------------------------------
        // REST API begins here
        // ----------------------------------------------------------------------------------------------

        path('/api/v1') {

            before('/*', { req, res -> res.type("application/json") })

            get("/helloTo", { Request req, Response res ->
                return '{"error": "HTTP GET is not supported for this endpoint. Use HTTP POST."}'
            })

            post("/helloTo", { Request req, Response res ->
                // One line: the power of Groovy and SparkJava together.
                new JsonBuilder(['msg': "Hello ${req.queryParams('name')}"]).toPrettyString()
            })

            get("/greeting", { Request req, Response res ->
                sayHello(req, res)
            })

        }
        log.info "Server started on port ${listenPort}"
    }

    static String sayHello(Request req, Response res) {
        // Groovy map: initialize a map with values in a single operation:
        final json = [greeting_message: GREETING_MESSAGE]

        // Groovy - simpler assignment to a map like it is an object:
        // JodaTime - the simplest date time library for JVM languages. Built in ISO-8601 (e.g. "2018-01-01T12:13:14.567Z"):
        json.created_at = new DateTime().toString()

        if (DEBUG_FLAG) json.debug = true

        new JsonBuilder(json).toPrettyString() // Groovy - implicit return and built-in JSON support
    }

    // ----------------------------------------------------------------------------------------------
    // Utility methods
    // ----------------------------------------------------------------------------------------------

    static String getEnvironmentVariable(String key) {
        new ProcessBuilder().environment().get(key)
    }

    static String getEnvironmentVariableOrHalt(String key, String haltMessageToUser, String haltMessageToLog) {
        final value = getEnvironmentVariable(key)
        if (!value) {
            log.error haltMessageToLog
            throw new IllegalStateException(haltMessageToUser)
        }
        value
    }
}
