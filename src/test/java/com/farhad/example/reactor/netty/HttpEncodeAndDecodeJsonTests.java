package com.farhad.example.reactor.netty;

import org.junit.jupiter.api.Test;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.core.publisher.Flux;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.json.JsonObjectDecoder;
import org.junit.jupiter.api.BeforeEach;
import reactor.netty.http.client.HttpClient;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 
 * Creating HTTP server
 * 
 * HttpServer allows to build, configure and materialize a HTTP server. Invoking HttpServer.create() one can prepare the HTTP server for configuration. 
 * 
 * Having already a HttpServer instance, one can start configuring the host, port, the IO handler, compression etc. For configuration purposes, the 
 * same immutable builder pattern is used as in UdpServer.
 * 
 * When finished with the server configuration, invoking HttpServer.bind, one will bind the server and Mono<DisposableServer> will be return, subscribing 
 * to this Publisher one can react on a successfully finished operation or handle issues that might happen. On the other hand cancelling this Mono, the 
 * underlying connecting operation will be aborted. If one do not need to interact with the Mono, there is HttpServer.bindNow(Duration) which is a 
 * convenient method for binding the server and obtaining the DisposableServer. Disposing the resources can be done via DisposableServer.dispose() or 
 * DisposableServer.disposeNow().
 * 
 * Defining routes for the HTTP server
 * 
 * In HttpServer one can handle the incoming requests and outgoing responses using HttpServer.handle(BiFunction<HttpServerRequest, HttpServerResponse, 
 * Publisher<Void>>) which is similar to the mechanism that was already described for UdpServer/TcpServer. However there is also a possibility to 
 * specify concrete routes and HTTP methods that the server will respond. This can be done using HttpServer.route(Consumer<HttpServerRoutes>). Using 
 * HttpServerRoutes one can specify the HTTP method, paths etc. For example the snippet below specifies that the server will respond only on POST method, 
 * where the path starts with /test and has a path parameter.
 * 
 * .route(routes ->
 *        routes.post("/test/{param}", (req, res) ->
 *                res.sendString(req.receive()
 *                                  .asString()
 *                                 .map(s -> s + ' ' + req.param("param") + '!'))))
 * 
 * HttpServerRequest provides API for accessing http request attributes as method, path, headers, path parameters etc. as well as to receive the request 
 * body. HttpServerResponse provides API for accessing http response attributes as status code, headers, compression etc. as well as to send the response 
 * body.
 * 
 * Creating HTTP client
 * 
 * HttpClient allows to build, configure and materialize a HTTP client. Invoking HttpClient.create() one can prepare the HTTP client for configuration. 
 * Having already a HttpClient instance, one can start configuring the host, port, headers, compression etc. For configuration purposes, the same immutable 
 * builder pattern is used as in UdpServer.
 * 
 * When finished with the client configuration, invoking HttpClient.get|post|…​ methods, one will receive HttpClient.RequestSender and will be able start 
 * configuring the HTTP request such as the uri and the request body. HttpClient.RequestSender.send* will end the HTTP request’s configuration and one 
 * can start discribing the actions on the HTTP response when it is received on the returned HttpClient.ResponseReceiver, the response body can be 
 * obtained via the provided HttpClient.ResponseReceiver.response* methods. As HttpClient.ResponseReceiver API always returns Publisher, the request and 
 * response executions are always deferred to the moment when there is a Subscriber that subscribes to the defined sequence. For example in the snippet 
 * below block() will subscribe to the defined sequence and in fact will trigger the execution.
 * 
 * In the snippet below can be used to send POST request with a body and received the answer from the server:
 * 
 * 
 * HttpClient.create()             // Prepares a HTTP client for configuration.
 *           .port(server.port())  // Obtain the server's port and provide it as a port to which this
 *                                 // client should connect.
 *           .wiretap(true)        // Applies a wire logger configuration.
 *           .headers(h -> h.add("Content-Type", "text/plain")) // Adds headers to the HTTP request.
 *           .post()              // Specifies that POST method will be used.
 *           .uri("/test/World")  // Specifies the path.
 *           .send(ByteBufFlux.fromString(Flux.just("Hello")))  // Sends the request body.
 *           .responseContent()   // Receives the response body.
 *           .aggregate()
 *           .asString()
 *           .block();
 * 
 * 
 */
public class HttpEncodeAndDecodeJsonTests {
   
    private Function<List<Pojo>,ByteBuf> jsonEncoder ;
    private Function<String,Pojo[]> jsonDecoder ;

    @BeforeEach
    public void setUp() {

        ObjectMapper  mapper = new ObjectMapper();

        jsonEncoder = pojo -> {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream() ;
                mapper.writeValue(out, pojo);
                return Unpooled.copiedBuffer(out.toByteArray());
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        };

        jsonDecoder = s -> {
            try {
                return mapper.readValue(s, Pojo[].class);
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Test
    public void encodeAndDecodeJsonTest() {
        DisposableServer server =   HttpServer
                                        .create()       // Prepares an HTTP server ready for configuration
                                        .port(0)   // Configures the port number as zero, this will let the system pick up
                                                        // an ephemeral port when binding the server
                                        
                                        .handle((in,out) ->
                                                // Calls the passed callback with Connection to operate on the
                                                // underlying channel state. This allows changing the channel pipeline.  
                                                out.send(in.withConnection(c -> c.addHandlerLast(new JsonObjectDecoder()))
                                                            .receive()
                                                            .asString()
                                                            .map(jsonDecoder)
                                                            .concatMap(Flux::fromArray)
                                                            .window(5)
                                                            .concatMap(w -> w.collectList().map(jsonEncoder)))                                              
                                        )
                                        .wiretap(true)
                                        .bindNow(); // Starts the server in a blocking fashion, and waits for it to finish its initialization
        assertNotNull(server);

        Pojo response =
                        HttpClient.create()            // Prepares a HTTP client for configuration.
                                        .port(server.port()) // Obtains the server's port and provides it as a port to which this
                                                            // client should connect.
                                        // Extends the channel pipeline.
                                        .doOnResponse((res, conn) -> conn.addHandlerLast(new JsonObjectDecoder()))
                                        .wiretap(true)       // Applies a wire logger configuration.
                                        .post()              // Specifies that POST method will be used.
                                        .uri("/test")        // Specifies the path.
                                        .send(Flux.range(1, 10)
                                                    .map(i -> new Pojo("test " + i))
                                                    .collectList()
                                                    .map(jsonEncoder))
                                        .response((res, byteBufFlux) ->
                                                            byteBufFlux.asString()
                                                                        .map(jsonDecoder)
                                                                        .concatMap(Flux::fromArray))
                                        .blockLast();

        assertEquals("test 10", response.getName());

        server.disposeNow();          // Stops the server and releases the resources.
        
    }
    
    @NoArgsConstructor
    @ToString
    @Setter
    @Getter
    @AllArgsConstructor
    private static final class Pojo {

        private String name ;
    }
}
