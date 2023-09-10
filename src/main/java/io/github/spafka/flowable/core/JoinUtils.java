package io.github.spafka.flowable.core;

import lombok.NonNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JoinUtils {

    @FunctionalInterface
    public interface BiCompare<A, B> {

        int compareTo(A a, B b);
    }


    public static <L, R, KEY extends Comparable, U> List<U> sortJoin(@NonNull List<L> left,
                                                                     @NonNull List<R> right,
                                                                     @NonNull Function<L, KEY> lkeyFunction,
                                                                     @NonNull Function<R, KEY> rkeyFunction,
                                                                     @NonNull BiFunction<L, R, U> function
    ) {

        left = (List<L>) left.stream()
                .filter(new Predicate<L>() {
                    Set s = new HashSet<L>();

                    @Override
                    public boolean test(L l) {
                        return s.add(l);
                    }
                })
                .sorted(Comparator.comparing(lkeyFunction::apply)).collect(Collectors.toList());
        right = (List<R>) right.stream()
                .filter(new Predicate<R>() {
                    Set s = new HashSet<R>();

                    @Override
                    public boolean test(R r) {
                        return s.add(r);
                    }
                })
                .sorted(Comparator.comparing(rkeyFunction::apply)).collect(Collectors.toList());
        List<U> u = new ArrayList<>();
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            int compare = lkeyFunction.apply(left.get(i)).compareTo(rkeyFunction.apply(right.get(j)));

            if (compare == 0) {
                u.add(function.apply(left.get(i), right.get(j)));
                i++;
                j++;
            } else if (compare < 0) {
                i++;
            } else {
                j++;
            }
        }
        return u;
    }

    public static <L, KEY extends Comparable, R, U> List<U> sortLeftJoin(
            @NonNull List<L> left,
            @NonNull List<R> right,
            @NonNull Function<L, KEY> lkeyFunction,
            @NonNull Function<R, KEY> rkeyFunction,
            @NonNull BiFunction<L, R, U> function
    ) {
        left = (List<L>) left.stream()
                        .filter(new Predicate<L>() {
                            Set s = new HashSet<L>();

                            @Override
                            public boolean test(L l) {
                                return s.add(l);
                            }
                        })
                        .sorted(Comparator.comparing(lkeyFunction::apply)).collect(Collectors.toList());
        right = (List<R>) right.stream()
                .filter(new Predicate<R>() {
                    Set s = new HashSet<R>();

                    @Override
                    public boolean test(R r) {
                        return s.add(r);
                    }
                })
                .sorted(Comparator.comparing(rkeyFunction::apply)).collect(Collectors.toList());
        List<U> u = new ArrayList<>();
        int i = 0, j = 0;
        while (i < left.size() && j < right.size()) {
            int compare = lkeyFunction.apply(left.get(i)).compareTo(rkeyFunction.apply(right.get(j)));
            if (compare == 0) {
                u.add(function.apply(left.get(i), right.get(j)));
                i++;
                j++;
            } else if (compare < 0) {
                u.add(function.apply(left.get(i), null));
                i++;
            } else {
                j++;
            }
        }
        while (i < left.size()) {
            u.add(function.apply(left.get(i), null));
            i++;
        }
        return u;
    }
}
