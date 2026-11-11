package com.campusfix;
import com.campusfix.persistence.*;
import com.campusfix.service.*;
import com.campusfix.ui.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.CountDownLatch;
public final class CaptureScreens {
    public static void main(String[] args)throws Exception {
        Path dir=Files.createTempDirectory("campusfix-screens-");
        UserDirectory users=new UserDirectory();
        ComplaintService svc=new ComplaintService(new FileTicketRepository(dir.resolve("samples.tsv")),users,new WeightedPriorityScorer());
        DemoSeeder.seedIfEmpty(svc,users);
        CampusFixFrame[] holder=new CampusFixFrame[1];
        SwingUtilities.invokeAndWait(()->{holder[0]=new CampusFixFrame(svc,users);holder[0].setVisible(true);holder[0].demoLogin("admin");});
        Thread.sleep(1200);
        Robot robot=new Robot();robot.setAutoDelay(250);
        capture(robot,holder[0],Paths.get("docs/admin_dashboard.png"));
        SwingUtilities.invokeAndWait(()->holder[0].demoLogin("student"));
        Thread.sleep(900);
        capture(robot,holder[0],Paths.get("docs/student_dashboard.png"));
        SwingUtilities.invokeAndWait(()->holder[0].dispose());
        System.out.println("Captured real UI screenshots.");
    }
    private static void capture(Robot r,Window frame,Path path)throws Exception{
        Point p=frame.getLocationOnScreen();
        BufferedImage image=r.createScreenCapture(new Rectangle(p.x,p.y,frame.getWidth(),frame.getHeight()));
        Files.createDirectories(path.getParent());ImageIO.write(image,"png",path.toFile());
    }
}
