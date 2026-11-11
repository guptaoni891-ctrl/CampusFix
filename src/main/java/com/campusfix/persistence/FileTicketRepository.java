package com.campusfix.persistence;
import com.campusfix.model.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.*;
public final class FileTicketRepository implements TicketRepository {
    private final Path path;
    private final Map<Integer,Complaint> tickets=new LinkedHashMap<>();
    public FileTicketRepository(Path path){this.path=path;load();}
    private void load(){
        if(!Files.exists(path))return;
        try(BufferedReader r=Files.newBufferedReader(path)){
            String line;
            while((line=r.readLine())!=null){
                if(line.isBlank())continue;
                String[] a=line.split("\\t",-1);
                if(a.length!=12)throw new IOException("Invalid file data");
                for(int i=0;i<a.length;i++)a[i]=unsafe(a[i]);
                Complaint t=new Complaint(Integer.parseInt(a[0]),a[1],a[2],a[3],a[4],Category.valueOf(a[5]),Severity.valueOf(a[6]),LocalDateTime.parse(a[7]),Status.valueOf(a[8]),a[9].isEmpty()?null:a[9],LocalDateTime.parse(a[10]));
                if(!a[11].equals("v1"))throw new IOException("Unrecognized record version");
                tickets.put(t.getId(),t);
            }
        }catch(Exception e){throw new IllegalStateException("Could not load ticket file: "+path,e);}
    }
    // Base64 encoding keeps record delimiters and multiline user text safe in a plain file.
    private static String safe(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
    private static String unsafe(String s){return new String(Base64.getDecoder().decode(s),StandardCharsets.UTF_8);}
    @Override public synchronized List<Complaint> findAll(){return tickets.values().stream().map(Complaint::copy).toList();}
    @Override public synchronized int nextId(){return tickets.keySet().stream().mapToInt(Integer::intValue).max().orElse(1000)+1;}
    @Override public synchronized void save(Complaint t){
        try{
            Files.createDirectories(path.toAbsolutePath().getParent());
            Map<Integer,Complaint> replacement=new LinkedHashMap<>(tickets);replacement.put(t.getId(),t.copy());
            Path tmp=Files.createTempFile(path.toAbsolutePath().getParent(),"campusfix-",".tmp");
            try(BufferedWriter w=Files.newBufferedWriter(tmp)){
                for(Complaint c:replacement.values()){
                    String[] a={String.valueOf(c.getId()),c.getStudentId(),c.getTitle(),c.getDescription(),c.getLocation(),c.getCategory().name(),c.getSeverity().name(),c.getCreatedAt().toString(),c.getStatus().name(),c.getTechnicianId()==null?"":c.getTechnicianId(),c.getUpdatedAt().toString(),"v1"};
                    for(int i=0;i<a.length;i++)a[i]=safe(a[i]);
                    w.write(String.join("\t",a));w.newLine();
                }
            }
            try{Files.move(tmp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException e){Files.move(tmp,path,StandardCopyOption.REPLACE_EXISTING);}
            tickets.clear();tickets.putAll(replacement);
        }catch(IOException e){throw new IllegalStateException("Unable to save complaint file",e);}
    }
    @Override public String mode(){return "Local File";}
}
