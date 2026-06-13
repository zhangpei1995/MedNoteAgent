package org.med.note;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("org.med.note.dao")
@SpringBootApplication
public class MedNoteAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MedNoteAgentApplication.class, args);
    }
}
