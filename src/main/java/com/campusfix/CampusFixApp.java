package com.campusfix;
import com.campusfix.model.*;
import com.campusfix.persistence.*;
import com.campusfix.service.*;
import com.campusfix.ui.CampusFixFrame;
import java.nio.file.*;
import javax.swing.*;
public final class CampusFixApp {
    public static void main(String[] args){
        System.setProperty("file.encoding","UTF-8");
        Path directory=Paths.get("data");
        TicketRepository repo;
        boolean forceFile=java.util.Arrays.asList(args).contains("--file") || "file".equalsIgnoreCase(System.getenv("CAMPUSFIX_STORAGE"));
        try {repo=forceFile?new FileTicketRepository(directory.resolve("complaints.tsv")):new SqliteTicketRepository(directory.resolve("campusfix.db"));}
        catch(IllegalStateException e){System.err.println("SQLite unavailable; using local file storage: "+e.getMessage());repo=new FileTicketRepository(directory.resolve("complaints.tsv"));}
        UserDirectory users=new UserDirectory();
        ComplaintService service=new ComplaintService(repo,users,new WeightedPriorityScorer());
        if(!java.util.Arrays.asList(args).contains("--empty")) DemoSeeder.seedIfEmpty(service,users);
        SwingUtilities.invokeLater(()->{
            CampusFixFrame frame=new CampusFixFrame(service,users);
            frame.setVisible(true);
            if(java.util.Arrays.asList(args).contains("--demo-admin"))frame.demoLogin("admin");
        });
    }
}
