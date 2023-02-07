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
