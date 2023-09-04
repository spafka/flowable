package io.github.spafka.flowable;

import io.github.spafka.flowable.core.JoinUtils;
import io.github.spafka.util.Utils;
import io.vavr.Tuple2;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class JoinUtilsTests {

    @Test
    public void _1() {


        {
            List<Integer> collect = IntStream.rangeClosed(1, 10000).boxed().collect(Collectors.toList());


            Utils.timeTakeMS(() -> {
                List<Object> objects = JoinUtils.sortJoin(collect, collect, Function.identity(), Function.identity(), (a, b) -> a);
                System.out.println();
            }, "new join");
        }


        {
            List<Integer> collect = IntStream.rangeClosed(1, 10000).boxed().collect(Collectors.toList());


            Utils.timeTakeMS(() -> {
                List<Object> objects = JoinUtils.sortJoin(collect, collect, Function.identity(), Function.identity(), (a, b) -> a);
                System.out.println();
            }, "new join");
        }

        {
            List<Integer> collect = Arrays.asList(1, 1);
            List<Integer> collect2 = Arrays.asList(1, 1);

            Utils.timeTakeMS(() -> {
                List<Object> objects = JoinUtils.sortJoin(collect, collect, Function.identity(), Function.identity(), (a, b) -> a);
                System.out.println();
            }, "new join");

        }

    }

    @Test
    public void _2() {


        {
            List<Integer> list1 = IntStream.rangeClosed(2, 3).boxed().collect(Collectors.toList());
            List<Integer> list2 = IntStream.rangeClosed(1, 1).boxed().collect(Collectors.toList());


            Utils.timeTakeMS(() -> {
                List<Tuple2<Integer, Integer>> tuple2s = JoinUtils.sortLeftJoin(list1, list2, Function.identity(), Function.identity(), (a, b) -> new Tuple2<>(a, b));
                System.out.println(tuple2s);
            }, "new join");
        }

        {
            List<Integer> list1 = Arrays.asList(1, 3, 3, 4, 5, 6);
            List<Integer> list2 = Arrays.asList(1, 3, 5);


            Utils.timeTakeMS(() -> {
                List<Tuple2<Integer, Integer>> tuple2s = JoinUtils.sortLeftJoin(list1, list2, Function.identity(), Function.identity(), (a, b) -> new Tuple2<>(a, b));
                System.out.println(tuple2s);
            }, "new join");
        }

        {
            List<Integer> list1 = Arrays.asList(1, 3, 3, 4, 5, 6);
            List<Integer> list2 = Arrays.asList(1);


            Utils.timeTakeMS(() -> {
                List<Tuple2<Integer, Integer>> tuple2s = JoinUtils.sortLeftJoin(list1, list2, Function.identity(), Function.identity(), (a, b) -> new Tuple2<>(a, b));
                System.out.println(tuple2s);
            }, "new join");
        }

        {
            List<Integer> list1 = Arrays.asList(7, 3, 3, 4, 5, 6);
            List<Integer> list2 = Arrays.asList(1, 3, 3);


            Utils.timeTakeMS(() -> {
                List<Tuple2<Integer, Integer>> tuple2s = JoinUtils.sortLeftJoin(list1, list2, Function.identity(), Function.identity(), (a, b) -> new Tuple2<>(a, b));
                System.out.println(tuple2s);
            }, "new join");
        }

        {
            List<Integer> list1 = Arrays.asList(7, 9, 0);
            List<Integer> list2 = Arrays.asList(1, 3, 3);


            Utils.timeTakeMS(() -> {
                List<Tuple2<Integer, Integer>> tuple2s = JoinUtils.sortLeftJoin(list1, list2, Function.identity(), Function.identity(), (a, b) -> new Tuple2<>(a, b));
                System.out.println(tuple2s);
            }, "new join");
        }

        {
            List<Integer> list1 = Arrays.asList(3, 3, 3, 3);
            List<Integer> list2 = Arrays.asList(1, 3, 3, 3);


            Utils.timeTakeMS(() -> {
                List<Tuple2<Integer, Integer>> tuple2s = JoinUtils.sortLeftJoin(list1, list2, Function.identity(), Function.identity(), (a, b) -> new Tuple2<>(a, b));
                System.out.println(tuple2s);
            }, "new join");
        }


    }

    @Test
    public void _3() {
        {
            List<Integer> list1 = Arrays.asList(3, 3, 3, 3);
            List<Integer> list2 = Arrays.asList(1, 3, 3);


            Utils.timeTakeMS(() -> {
                List<Tuple2<Integer, Integer>> tuple2s = JoinUtils
                        .sortLeftJoin(list1,
                                list2,
                                Function.identity(),
                                Function.identity(),

                                (a, b) -> new Tuple2<>(a, b));
                System.out.println(tuple2s);
            }, "new join");
        }

    }


}
