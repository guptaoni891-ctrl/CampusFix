package com.campusfix.util;
import com.campusfix.model.Complaint;
import com.campusfix.service.PriorityScorer;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
public final class CsvExporter {
    private CsvExporter(){}
    private static String csv(String value){return "\""+value.replace("\"","\"\"")+"\"";}
    public static void export(Path file,List<Complaint> tickets,PriorityScorer scorer) throws IOException {
        try(BufferedWriter w=Files.newBufferedWriter(file,StandardCharsets.UTF_8)){
            w.write("ID,Title,Category,Severity,Location,Status,Student ID,Technician ID,Priority,Created At,Updated At");w.newLine();
            for(Complaint t:tickets){
                List<String> cells=Arrays.asList(String.valueOf(t.getId()),t.getTitle(),t.getCategory().name(),t.getSeverity().name(),t.getLocation(),t.getStatus().name(),t.getStudentId(),t.getTechnicianId()==null?"":t.getTechnicianId(),String.valueOf(scorer.score(t,LocalDateTime.now())),t.getCreatedAt().toString(),t.getUpdatedAt().toString());
                w.write(String.join(",",cells.stream().map(CsvExporter::csv).toList()));w.newLine();
            }
        }
    }
}
