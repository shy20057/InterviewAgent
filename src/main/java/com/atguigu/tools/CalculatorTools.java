package com.atguigu.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTools {

    @Tool(name="加法运算", value = "用于计算两个数字的和")
    double sum(
            @ToolMemoryId int memoryId,
            @P(value = "a", required = true) double a,
            @P(value = "b", required = true) double b) {
        System.out.println("调用加法运算 memoryId:"+memoryId);
        return a + b;
    }
    @Tool
    double squareRoot(double x) {
        System.out.println("调用平方根运算");
        return Math.sqrt(x);
    }

}
