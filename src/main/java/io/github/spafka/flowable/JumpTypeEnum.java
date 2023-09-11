package io.github.spafka.flowable;


public enum JumpTypeEnum {

    /**
     *
     */
    simple_serial,

    /**
     * 串行
     */
    serial,
    /**
     * 并行
     */
    paral_to_father,
    /**
     * 跳到父流程
     */
    paral_to_child,



}
