package com.farhad.example.reactor.netty;

import org.junit.jupiter.api.Test;

import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.netty.Connection;
import reactor.netty.DisposableServer;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpServer;
import reactor.netty.tcp.TcpSslContextSpec;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

 /**
 * TCP server and client
 *
 * If one wants to utilize the TCP protocol one will need to create a TCP servers that can send packages to the connected clients or TCP clients 
 * that can connect to specific TCP servers. 
 * 
 * Reactor Netty provides easy to use and configure TcpServer and TcpClient, they hide most of the Netty functionality that is needed in order to 
 * create with TCP server and client, and in addition add Reactive Streams backpressure.
 * 
 * Creating TCP server
 * 
 * TcpServer allows to build, configure and materialize a TCP server. Invoking TcpServer.create() one can prepare the TCP server for configuration. 
 * 
 * Having already a TcpServer instance, one can start configuring the host, port, the IO handler etc. For configuration purposes, the same immutable 
 * builder pattern is used as in UdpServer.
 * 
 * When finished with the server configuration, invoking TcpServer.bind, one will bind the server and Mono<DisposableServer> will be return, 
 * subscribing to this Publisher one can react on a successfully finished operation or handle issues that might happen. On the other hand 
 * cancelling this Mono, the underlying connecting operation will be aborted. If one do not need to interact with the Mono, there is 
 * TcpServer.bindNow(Duration) which is a convenient method for binding the server and obtaining the DisposableServer. DisposableServer holds 
 * contextual information for the underlying server. Disposing the resources can be done via DisposableServer.dispose() or 
 * DisposableServer.disposeNow().
 * 
 * Enabling SSL support for TCP server
 * 
 * TcpServer provides several convenient methods for configuring SSL:
 * 
 *     - TcpServer.secure(SslContext) where SslContext is already configured
 * 
 *     - TcpServer.secure(Consumer<? super SslProvider.SslContextSpec>) where the SSL configuration customization 
 *       can be done via the passed builder.
 * 
 * Add TCP server IO handler that will send a file
 * 
 * TcpServer.handle(BiFunction<NettyInbound, NettyOutbound, Publisher<Void>>) should be used if one wants to attach IO handler that will 
 * process the incoming messages and will eventually send messages as a reply. 
 * 
 * NettyInbound is used to receive bytes from the peer where NettyInbound.receiveObject() returns the pure inbound Flux, while NettyInbound.receive() 
 * returns ByteBufFlux which provides an extra API to handle the incoming traffic.
 * 
 * NettyOutbound is used to send bytes to the peer, listen for any error returned by the write operation and close on terminal signal (complete|error). 
 * 
 * If more than one Publisher is attached (multiple calls to NettyOutbound.send* methods), completion occurs when all publishers complete. 
 * 
 * The Publisher<Void> that has to be returned as a result represents the sequence of the operations that will be applied for the incoming and 
 * outgoing traffic.
 * 
 * For example if one wants to send a file to the client where the file name is received as an incoming package, the snippet bellow can be used:
 * 
 * .handle((in, out) ->
 *       in.receive()
 *         .asString()
 *         .flatMap(s -> {
 *             try {
 *                 Path file = Paths.get(getClass().getResource(s).toURI());
 *                 return out.sendFile(file)
 *                           .then();
 *             } catch (URISyntaxException e) {
 *                 return Mono.error(e);
 *             }
 *         }))
 *
 * 
 * Creating TCP client
 * 
 * TcpClient allows to build, configure and materialize a TCP client. Invoking TcpClient.create() one can prepare the TCP client for configuration. 
 * Having already a TcpClient instance, one can start configuring the host, port, the IO handler etc. For configuration purposes, the same immutable 
 * builder pattern is used as in UdpServer.
 * 
 * When finished with the client configuration, invoking TcpClient.connect(), one will connect the client and Mono<Connection> will be return, 
 * subscribing to this Publisher one can react on a successfully finished operation or handle issues that might happen. 
 * 
 * On the other hand cancelling this Mono, the underlying connecting operation will be aborted. If one do not need to interact with the Mono, 
 * there is TcpClient.connectNow(Duration) which is a convenient method for connecting the client and obtaining the Connection. 
 * 
 * Disposing the resources can be done via Connection.dispose() or Connection.disposeNow().
 * 
 * Enabling SSL support for TCP client
 * 
 * TcpClient provides several convenient methods for configuring SSL. When one wants to use the default SSL configuration provided by Reactor 
 * Netty TcpClient.secure() can be used. If additional configuration is necessary then one of the following methods can be used:
 * 
 *     - TcpClient.secure(SslContext) where SslContext is already configured
 * 
 *     - TcpClient.secure(Consumer<? super SslProvider.SslContextSpec>) where the SSL configuration 
 *       customization can be done via the passed builder.
 * 
 * Add TCP client IO handler
 * 
 * TcpClient.handle(BiFunction<NettyInbound, NettyOutbound, Publisher<Void>>) should be used if one wants to attach IO handler that will process 
 * the incoming messages and will eventually send messages as reply. 
 * 
 * Here as a convenience NettyOutbound.send* (e.g. NettyOutbound.sendString) methods can be used instead of NettyOutbound.sendObject. 
 * 
 * The same is also for using NettyInbound.receive() instead of NettyInbound.receiveObject().
 * 
 */
public class TcpSendFileTests {
    
    @Test
    public void sendFileTest() throws Exception  {


        SelfSignedCertificate cert = new SelfSignedCertificate();
        TcpSslContextSpec sslcontextBuilder = 
                                    TcpSslContextSpec.forServer(cert.certificate(), cert.privateKey());

        DisposableServer server = 
                            TcpServer.create()
                                     .port(0)
                                     .secure(spec -> spec.sslContext(sslcontextBuilder))
                                     .wiretap(true)
                                     .handle((in,out) -> 
                                                in.receive()
                                                    .asString()
                                                    .flatMap(s -> {
                                                            try {
                                                                Path file = Paths.get(getClass().getResource(s).toURI());
                                                                return out.sendFile(file).then();

                                                            } catch(URISyntaxException e) {
                                                                return Mono.error(e);
                                                            }

                                                        })
                                                    .log("tcp-server"))
                                     .bindNow();

        assertNotNull(server);

        CountDownLatch latch = new CountDownLatch(1);

        Connection client = 
                    TcpClient .create()                 // Prepares a TCP client for configuration.
                              .port(server.port())      // Obtains the server's port and provide it as a port to which this
                                                        // client should connect.
                              // Configures SSL providing an already configured SslContext.
                              .secure(spec -> spec.sslContext(
                                        TcpSslContextSpec
                                            .forClient()
                                            .configure(builder -> builder.trustManager(InsecureTrustManagerFactory.INSTANCE))))
                               .wiretap(true)
                               .handle((in,out) -> 
                                        out.sendString(Mono.just("/index.html"))
                                           .then(
                                              in.receive()
                                                .asByteArray()
                                                .doOnNext(actualBytes -> {
                                                    try{    
                                                        Path file = Paths.get(getClass().getResource("/index.html").toURI());
                                                        byte[] expectedBytes = Files.readAllBytes(file);
                                                        if (Arrays.equals(expectedBytes, expectedBytes)) {
                                                            latch.countDown();
                                                        }
                                                    } catch(URISyntaxException | IOException e){
                                                        e.printStackTrace();
                                                    }
                                                })
                                                .log("tcp-client")
                                                .then()))
                               .connectNow();                  // Blocks the client and returns a Connection.
        assertNotNull(client); 

        assertTrue(latch.await(30, TimeUnit.SECONDS));

        server.disposeNow();
        client.disposeNow();

    }
}
