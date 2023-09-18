package io.github.spafka.flowable.join;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONPath;
import lombok.Data;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class S {

    @Data
    static class A {

        String id;
        String userName;
        String personId;
        String personName;

    }

    public static void main(String[] args) throws IOException {

        String s = FileUtils.readFileToString(new File("aa.json"), null);
        s = JSONPath.read(s, "$.list").toString();
        List<A> as = JSON.parseArray(s, A.class);

        as.stream().filter(x -> x.getPersonName() == null).forEach(System.out::println);

        System.out.println();


    }

}
