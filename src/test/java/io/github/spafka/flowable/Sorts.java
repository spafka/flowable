package io.github.spafka.flowable;


import com.google.common.collect.*;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class Sorts {

    @Test
    public void _1(){

        ListMultimap<Integer, String> multiMap = ArrayListMultimap.create();
        // 添加键值对到多重映射


        // 使用Ordering对多重映射进行排序


        Ordering<Map.Entry<Integer, String>> ordering
                = Ordering.natural().onResultOf(Map.Entry::getKey);
        List<Map.Entry<Integer, String>> sortedEntries = ordering.sortedCopy(multiMap.entries());



        // 打印排序后的多重映射
        for (Map.Entry<Integer, String> entry : sortedEntries) {
            int key = entry.getKey();
            String value = entry.getValue();
            System.out.println("Key: " + key + ", Value: " + value);
        }
    }
}
