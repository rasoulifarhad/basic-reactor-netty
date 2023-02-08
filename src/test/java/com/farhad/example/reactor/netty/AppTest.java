package com.farhad.example.reactor.netty;

import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.function.BiFunction;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
@Slf4j
public class AppTest {
    
    @Test
    public void testBiFunction() {


        BiFunction<String,String,String> concatStrings = (s1,s2) -> s1.concat(s2) ;

        String s1 = "Hello" ;
        String s2 = " ";
        String s3 = "farhad" ;

        log.info("{}",concatStrings.apply(s1,concatStrings.apply(s2, s3)));
        
        Function<String,String> concatConstants = (s) -> s.concat("-");
        Function<String,String> convertToUpppercase = String::toUpperCase;

        BiFunction<String,String,String> stage1 = concatStrings.andThen(concatConstants);
        BiFunction<String,String,String> stage2 = stage1.andThen(convertToUpppercase);

        log.info("{}",stage2.apply(s1,s3));


     }

     @Test
     public void test1() {

        List<String> list1 = Arrays.asList("a", "b", "c");
        List<Integer> list2 = Arrays.asList(1, 2, 3);

        List<String> result = new ArrayList<>();
        for (int i=0; i < list1.size(); i++) {
            result.add(list1.get(i) + list2.get(i));
        }

        // assertThat(result).containsExactly("a1", "b2", "c3");

     }

     private <T, U, R> List<R> listCombiner(List<T> list1, 
                                                    List<U> list2, 
                                                    BiFunction<T, U, R> combiner) {
        List<R> result = new ArrayList<>();
        for (int i = 0; i < list1.size(); i++) {
            result.add(combiner.apply(list1.get(i), list2.get(i)));
        }
        return result;
    }

    @Test
    public void testBiFunction2() {

        List<String> list1 = Arrays.asList("a", "b", "c");
        List<Integer> list2 = Arrays.asList(1, 2, 3);

        List<String> result = listCombiner(list1,
                                            list2,
                                            (letter,number) -> letter + number    );

        log.info("{}", result);

    }

    @Test
    public void testBiFunction3() {

        List<Double> list1 = Arrays.asList(1.0d, 2.1d, 3.3d);
        List<Float> list2 = Arrays.asList(0.1f, 0.2f, 4f);

        List<Boolean> result = listCombiner(list1, list2, (doubleNumber, floatNumber) -> doubleNumber > floatNumber);

        log.info("{}", result);
    }

    private boolean firstGreaterThanNext(Double a, Float b) {
        return a > b ;
    }

    private String concatLetterAndNumber(String a, Integer b) {
        return a + b ;
    }
 
    @Test
    public void testBiFunction4() {

        List<Double> list1 = Arrays.asList(1.0d, 2.1d, 3.3d);
        List<Float> list2 = Arrays.asList(0.1f, 0.2f, 4f);

        List<Boolean> result = listCombiner(list1, list2, this::firstGreaterThanNext);

        log.info("{}", result);
    }

    @Test
    public void testBiFunction5() {

        List<String> list1 = Arrays.asList("a", "b", "c");
        List<Integer> list2 = Arrays.asList(1, 2, 3);

        List<String> result = listCombiner(list1, list2, this::concatLetterAndNumber);

        log.info("{}", result);
    }

    private static <T,U,R> BiFunction<T,U,R> asBiFunction(BiFunction<T,U,R> function){

        return function;

    }

    @Test
    public void testBiFunction6() {
        List<Double> list1 = Arrays.asList(1.0d, 2.1d, 3.3d);
        List<Double> list2 = Arrays.asList(0.1d, 0.2d, 4d);

        List<Integer> result = listCombiner(list1,
                                            list2,
                                            Double::compareTo);

        log.info("{}", result);

    }

    @Test
    public void testBiFunction7() {
        List<Double> list1 = Arrays.asList(1.0d, 2.1d, 3.3d);
        List<Double> list2 = Arrays.asList(0.1d, 0.2d, 4d);

        List<Boolean> result = listCombiner(list1,
                                            list2,
                                            asBiFunction(Double::compareTo).andThen(i -> i> 0 ));

        log.info("{}", result);

    }


}
