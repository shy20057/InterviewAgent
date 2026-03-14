package com.atguigu.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.V;
import dev.langchain4j.service.spring.AiService;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
        chatModel = "qwenChatModel"
)
public interface ExtractSkillsAgent {


    @SystemMessage(fromResource = "extractSkills_prompt_template.txt")
    String extractSkills(@V("resume_text") String resumeText);


}
