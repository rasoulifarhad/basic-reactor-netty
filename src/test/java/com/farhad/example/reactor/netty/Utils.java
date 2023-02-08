package com.farhad.example.reactor.netty;
 
import java.util.List;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Utils {
    
    private <T> Flux<T> monoTofluxUsingFlatMapMany(Mono<List<T>> monoList) {
        return monoList
                    .flatMapMany(Flux::fromIterable)
                    .log();
    }


    private <T> Flux<T> monoTofluxUsingFlatMapIterable(Mono<List<T>> monoList) {
        return monoList
                .flatMapIterable(list -> list)
                .log();
    }
    
}
