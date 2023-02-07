package com.farhad.example.reactor.netty;

import org.junit.jupiter.api.Test;

import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
// import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
// import reactor.netty.ByteBufFlux;
import reactor.netty.ByteBufMono;

import reactor.netty.http.client.HttpClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class AppTest {
    
    @Test
    public void testHttpPost() {

        DisposableServer server =   HttpServer
                                        .create()       // Prepares an HTTP server ready for configuration
                                        .port(0)   // Configures the port number as zero, this will let the system pick up
                                                        // an ephemeral port when binding the server
                                        
                                        .route(routes -> 
                                                        // The server will respond only on POST requests
                                                        // where the path starts with /test and then there is path parameter
                                                    routes.post("/test/{param}", (request,response) -> 
                                                                response.sendString( request.receive()
                                                                                            .asString()
                                                                                            .map(s -> s + ' ' + request.param("param") + '!')
                                                                                            .log("httpServer"))))
                                        .wiretap(true)
                                        .bindNow(); // Starts the server in a blocking fashion, and waits for it to finish its initialization
        assertNotNull(server);
                                        
        String response = HttpClient.create()             // Prepares an HTTP client ready for configuration
                                        .port(server.port())             // Obtains the server's port and provides it as a port to which this
                                                                        // client should connect
                                        .wiretap(true)        // Applies a wire logger configuration.
                                        .headers(h -> h.add("Content-Type", "text/plain")) // Adds headers to the HTTP request.
                                        .post()                       // Specifies that POST method will be used
                                        .uri("/test/World")       // Specifies the path
                                        // .send(ByteBufFlux.fromString(Flux.just("Hello")))  // Sends the request body
                                         .send(ByteBufMono.fromString(Mono.just("Hello")))  // Sends the request body
                                        .responseContent()          // Receives the response body
                                        .aggregate()
                                        .asString()
                                        .log("http-client")
                                        .block();

        assertEquals("Hello World!" ,response);

        server.disposeNow();
    }

    @Test
    public void testHttpCompress() {
        
        DisposableServer server = HttpServer
                                        .create()
                                        .port(0)
                                        .handle((req,res) -> res.sendString(Mono.just("compressed response")))
                                        .compress(true)
                                        .wiretap(true)
                                        .bindNow();

        assertNotNull(server);

        String response = HttpClient
                                .create()
                                .port(server.port())
                                .compress(true)   // Enables compression.
                                .wiretap(true)                 // Applies a wire logger configuration.
                                .get()                             // Specifies that GET metho// Receives the response body.d will be used.
                                .uri("/test")                   // Specifies the path.
                                .responseContent()                 // Receives the response body.
                                .aggregate()
                                .asString()
                                .log("http-client")
                                .block();

        assertEquals("compressed response",response);

        server.disposeNow();

    }



}
