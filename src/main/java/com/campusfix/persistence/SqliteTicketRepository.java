package com.campusfix.persistence;
import com.campusfix.model.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
public final class SqliteTicketRepository implements TicketRepository {
    private final String jdbcUrl;
    public SqliteTicketRepository(Path file){
        try{
            Files.createDirectories(file.toAbsolutePath().getParent());
            // Driver is supplied by Maven; keep the rest of the app runnable without Maven using file mode.
            Class.forName("org.sqlite.JDBC");
            jdbcUrl="jdbc:sqlite:"+file.toAbsolutePath();
            try(Connection c=DriverManager.getConnection(jdbcUrl);Statement st=c.createStatement()){
                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS complaints (
                      id INTEGER PRIMARY KEY, student_id TEXT NOT NULL, title TEXT NOT NULL,
                      description TEXT NOT NULL, location TEXT NOT NULL, category TEXT NOT NULL,
                      severity TEXT NOT NULL, created_at TEXT NOT NULL, status TEXT NOT NULL,
                      technician_id TEXT, updated_at TEXT NOT NULL)
                    """);
            }
        }catch(Exception e){throw new IllegalStateException("SQLite unavailable: "+e.getMessage(),e);}
    }
    @Override public synchronized List<Complaint> findAll(){
        List<Complaint> result=new ArrayList<>();
        try(Connection c=DriverManager.getConnection(jdbcUrl);PreparedStatement st=c.prepareStatement("SELECT * FROM complaints ORDER BY id DESC");ResultSet rs=st.executeQuery()){
            while(rs.next())result.add(new Complaint(rs.getInt("id"),rs.getString("student_id"),rs.getString("title"),rs.getString("description"),rs.getString("location"),Category.valueOf(rs.getString("category")),Severity.valueOf(rs.getString("severity")),LocalDateTime.parse(rs.getString("created_at")),Status.valueOf(rs.getString("status")),rs.getString("technician_id"),LocalDateTime.parse(rs.getString("updated_at"))));
        }catch(SQLException e){throw new IllegalStateException("Database read error",e);}
        return result;
    }
    @Override public synchronized int nextId(){
        try(Connection c=DriverManager.getConnection(jdbcUrl);Statement st=c.createStatement();ResultSet rs=st.executeQuery("SELECT COALESCE(MAX(id),1000)+1 FROM complaints")){
            return rs.next()?rs.getInt(1):1001;
        }catch(SQLException e){throw new IllegalStateException("Database ID error",e);}
    }
    @Override public synchronized void save(Complaint t){
        String sql="""
            INSERT INTO complaints (id,student_id,title,description,location,category,severity,created_at,status,technician_id,updated_at)
            VALUES (?,?,?,?,?,?,?,?,?,?,?)
            ON CONFLICT(id) DO UPDATE SET title=excluded.title, description=excluded.description,
            location=excluded.location, category=excluded.category, severity=excluded.severity,
            status=excluded.status, technician_id=excluded.technician_id,updated_at=excluded.updated_at
            """;
        try(Connection c=DriverManager.getConnection(jdbcUrl);PreparedStatement st=c.prepareStatement(sql)){
            st.setInt(1,t.getId());st.setString(2,t.getStudentId());st.setString(3,t.getTitle());
            st.setString(4,t.getDescription());st.setString(5,t.getLocation());
            st.setString(6,t.getCategory().name());st.setString(7,t.getSeverity().name());
            st.setString(8,t.getCreatedAt().toString());st.setString(9,t.getStatus().name());
            st.setString(10,t.getTechnicianId());st.setString(11,t.getUpdatedAt().toString());
            st.executeUpdate();
        }catch(SQLException e){throw new IllegalStateException("Database write error",e);}
    }
    @Override public String mode(){return "SQLite / JDBC";}
}
