package io.github.spafka.flowable;


public enum JumpTypeEnum {

    /**
     * 串行
     */
    simple_serial,
    /**
     * 并行
     */
    paral_to_father,
    /**
     * 跳到父流程
     */
    paral_to_child,



}
