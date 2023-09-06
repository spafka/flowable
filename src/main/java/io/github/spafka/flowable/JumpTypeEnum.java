package io.github.spafka.flowable;


public enum JumpTypeEnum {

    /**
     * 串行
     */
    serial,
    /**
     * 并行
     */
    paral,
    /**
     * 跳到父流程
     */
    subToParentProcess,

    /**
     * 未知类型
     */
    un_known;

}
