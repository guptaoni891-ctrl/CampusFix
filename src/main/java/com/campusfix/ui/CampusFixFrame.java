package com.campusfix.ui;
import com.campusfix.model.*;
import com.campusfix.service.*;
import com.campusfix.util.CsvExporter;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

public final class CampusFixFrame extends JFrame {
    private final ComplaintService service;
    private final UserDirectory directory;
    private User currentUser;
    private JPanel root, center, cards;
    private JLabel subtitle, banner;
    private JTable table;
    private DefaultTableModel model;
    private JTextField search;
    private JComboBox<String> filter;
    private JTextArea details;
    private List<Complaint> filtered=new ArrayList<>();
    private final DateTimeFormatter date=DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    public CampusFixFrame(ComplaintService service,UserDirectory directory){
        super("CampusFix | Campus Maintenance Management");
        this.service=service;this.directory=directory;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);setSize(1250,770);setMinimumSize(new Dimension(1040,640));
        setLocationRelativeTo(null);
        try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ignored){}
        service.addListener((ticket,action)->SwingUtilities.invokeLater(()->{if(currentUser!=null)refresh();}));
        loginView();
    }
    public void demoLogin(String username){try{currentUser=directory.login(username,"demo123");dashboard();}catch(ValidationException e){error(e.getMessage());}}
    private void replace(JPanel next){setContentPane(next);revalidate();repaint();}
    private JPanel outer(){JPanel p=new JPanel(new BorderLayout());p.setBackground(Style.BG);return p;}
    private JLabel title(String s,int size){return Style.label(s,size,true);}
    private void loginView(){
        currentUser=null;
        JPanel p=outer();
        JPanel left=new JPanel(new GridBagLayout());left.setPreferredSize(new Dimension(480,600));left.setBackground(Style.NAVY);
        JPanel branding=new JPanel();branding.setOpaque(false);branding.setLayout(new BoxLayout(branding,BoxLayout.Y_AXIS));
        JLabel icon=new JLabel("CF");icon.setOpaque(true);icon.setBackground(Style.BLUE);icon.setForeground(Color.WHITE);icon.setFont(Style.font(34,true));icon.setBorder(new EmptyBorder(12,18,12,18));icon.setAlignmentX(0);branding.add(icon);
        branding.add(Box.createVerticalStrut(35));
        JLabel name=new JLabel("CampusFix");name.setForeground(Color.WHITE);name.setFont(Style.font(43,true));name.setAlignmentX(0);branding.add(name);
        JLabel tagline=new JLabel("A smarter campus starts here.");tagline.setForeground(new Color(184,205,231));tagline.setFont(Style.font(17,false));tagline.setAlignmentX(0);branding.add(tagline);
        branding.add(Box.createVerticalStrut(35));
        for(String bullet:List.of("01   REPORT CAMPUS ISSUES","02   ASSIGN & PRIORITIZE","03   TRACK RESOLUTION")){
            JLabel line=new JLabel(bullet);line.setForeground(new Color(223,234,248));line.setFont(Style.font(14,false));line.setAlignmentX(0);branding.add(line);branding.add(Box.createVerticalStrut(19));
        }
        left.add(branding);p.add(left,BorderLayout.WEST);
        JPanel formHost=new JPanel(new GridBagLayout());formHost.setBackground(Style.BG);
        JPanel form=Style.panel();form.setPreferredSize(new Dimension(405,420));form.setLayout(new BoxLayout(form,BoxLayout.Y_AXIS));form.setBorder(new EmptyBorder(36,34,34,34));
        JLabel heading=title("Welcome back",27);heading.setAlignmentX(0);form.add(heading);form.add(Box.createVerticalStrut(9));
        JLabel caption=Style.label("Sign in to manage campus maintenance",13,false);caption.setForeground(Style.MUTED);caption.setAlignmentX(0);form.add(caption);
        form.add(Box.createVerticalStrut(30));
        JLabel userLabel=title("USERNAME",12);userLabel.setAlignmentX(0);form.add(userLabel);form.add(Box.createVerticalStrut(8));
        JTextField userField=new JTextField("admin");styleField(userField);form.add(userField);form.add(Box.createVerticalStrut(18));
        JLabel passLabel=title("PASSWORD",12);passLabel.setAlignmentX(0);form.add(passLabel);form.add(Box.createVerticalStrut(8));
        JPasswordField passField=new JPasswordField("demo123");styleField(passField);form.add(passField);form.add(Box.createVerticalStrut(25));
        JButton signIn=Style.button("Sign in  →");signIn.setMaximumSize(new Dimension(Integer.MAX_VALUE,43));signIn.setAlignmentX(0);form.add(signIn);
        signIn.addActionListener(e->{try{currentUser=directory.login(userField.getText(),new String(passField.getPassword()));dashboard();}catch(ValidationException ex){error(ex.getMessage());}});
        passField.addActionListener(e->signIn.doClick());
        form.add(Box.createVerticalStrut(22));
        JLabel info=Style.label("Demo accounts: admin / student / student2 / tech / tech2",11,false);
        info.setForeground(Style.MUTED);info.setAlignmentX(0);form.add(info);
        JLabel pwd=Style.label("Password for all demo accounts: demo123",11,false);pwd.setForeground(Style.MUTED);pwd.setAlignmentX(0);form.add(pwd);
        formHost.add(form);p.add(formHost,BorderLayout.CENTER);replace(p);
    }
    private void styleField(JTextField field){field.setFont(Style.font(14,false));field.setMaximumSize(new Dimension(Integer.MAX_VALUE,44));field.setPreferredSize(new Dimension(100,44));field.setBorder(new CompoundBorder(new LineBorder(Style.BORDER),new EmptyBorder(7,10,7,10)));}
    private JPanel padded(){JPanel p=Style.panel();p.setBorder(new EmptyBorder(18,22,18,22));return p;}
    private void dashboard(){
        root=outer();
        JPanel header=new JPanel(new BorderLayout());header.setBackground(Style.NAVY);header.setBorder(new EmptyBorder(20,28,20,28));
        JLabel logo=new JLabel("◆  CampusFix");logo.setFont(Style.font(25,true));logo.setForeground(Color.WHITE);header.add(logo,BorderLayout.WEST);
        JPanel account=new JPanel(new FlowLayout(FlowLayout.RIGHT,16,0));account.setOpaque(false);
        JLabel user=new JLabel(currentUser.getName()+"   •   "+currentUser.getRole());user.setForeground(new Color(221,231,245));user.setFont(Style.font(13,true));account.add(user);
        JButton logout=Style.secondary("Sign out");logout.addActionListener(e->loginView());account.add(logout);header.add(account,BorderLayout.EAST);
        root.add(header,BorderLayout.NORTH);

        center=new JPanel(new BorderLayout(0,14));center.setBackground(Style.BG);center.setBorder(new EmptyBorder(24,28,24,28));
        JPanel intro=new JPanel(new BorderLayout());intro.setOpaque(false);
        JPanel greeting=new JPanel();greeting.setOpaque(false);greeting.setLayout(new BoxLayout(greeting,BoxLayout.Y_AXIS));
        JLabel welcome=title(switch(currentUser.getRole()){case ADMIN->"Operations overview";case STUDENT->"My maintenance requests";case TECHNICIAN->"Assigned work orders";},27);
        greeting.add(welcome);
        subtitle=Style.label("Track issues, prioritise responses, and keep campus running.",13,false);subtitle.setForeground(Style.MUTED);greeting.add(Box.createVerticalStrut(6));greeting.add(subtitle);intro.add(greeting,BorderLayout.WEST);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));actions.setOpaque(false);
        JButton csv=Style.secondary("Export CSV");csv.addActionListener(e->exportCsv());actions.add(csv);
        JButton reload=Style.secondary("Refresh");reload.addActionListener(e->refresh());actions.add(reload);
        if(currentUser.getRole()==Role.STUDENT){JButton add=Style.button("+ New Complaint");add.addActionListener(e->newComplaint());actions.add(add);}
        if(currentUser.getRole()==Role.ADMIN){JButton assign=Style.button("Assign Technician");assign.addActionListener(e->assignTicket());actions.add(assign);}
        if(currentUser.getRole()==Role.TECHNICIAN||currentUser.getRole()==Role.ADMIN){JButton status=Style.button("Advance Status");status.addActionListener(e->advanceStatus());actions.add(status);}
        intro.add(actions,BorderLayout.EAST);center.add(intro,BorderLayout.NORTH);

        JPanel content=new JPanel(new BorderLayout(0,15));content.setOpaque(false);
        cards=new JPanel(new GridLayout(1,4,14,0));cards.setOpaque(false);content.add(cards,BorderLayout.NORTH);
        JPanel work=Style.panel();work.setLayout(new BorderLayout(0,15));work.setBorder(new EmptyBorder(20,20,20,20));
        JPanel controls=new JPanel(new BorderLayout(10,0));controls.setOpaque(false);
        JLabel tableTitle=title("Complaint tracker",18);controls.add(tableTitle,BorderLayout.WEST);
        JPanel tools=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));tools.setOpaque(false);
        search=new JTextField(19);styleField(search);search.setPreferredSize(new Dimension(210,35));search.setMaximumSize(new Dimension(210,35));search.setToolTipText("Search title, location, category, or ID");tools.add(search);
        filter=new JComboBox<>(new String[]{"All statuses","Open","Assigned","In progress","Resolved"});filter.setFont(Style.font(12,false));tools.add(filter);
        controls.add(tools,BorderLayout.EAST);work.add(controls,BorderLayout.NORTH);
        String[] cols={"ID","TITLE","CATEGORY","SEVERITY","LOCATION","PRIORITY","STATUS","TECHNICIAN","CREATED"};
        model=new DefaultTableModel(cols,0){@Override public boolean isCellEditable(int row,int col){return false;}};
        table=new JTable(model);table.setFont(Style.font(12,false));table.setRowHeight(39);table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);table.getTableHeader().setFont(Style.font(11,true));table.getTableHeader().setBackground(new Color(237,242,248));table.getTableHeader().setForeground(Style.MUTED);
        table.setGridColor(new Color(241,244,249));table.setShowVerticalLines(false);table.setSelectionBackground(Style.LIGHT_BLUE);table.setSelectionForeground(Style.TEXT);
        table.setDefaultRenderer(Object.class,new DefaultTableCellRenderer(){@Override public Component getTableCellRendererComponent(JTable t,Object v,boolean selected,boolean focus,int r,int col){super.getTableCellRendererComponent(t,v,selected,focus,r,col);setBorder(new EmptyBorder(3,8,3,8));if(!selected){setBackground(r%2==0?Color.WHITE:new Color(250,252,254));setForeground(Style.TEXT);}if(col==5)setFont(Style.font(12,true));else setFont(Style.font(12,false));return this;}});
        table.getColumnModel().getColumn(0).setPreferredWidth(48);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(4).setPreferredWidth(140);
        table.getColumnModel().getColumn(8).setPreferredWidth(134);
        JScrollPane tableScroll=new JScrollPane(table);tableScroll.setBorder(new LineBorder(Style.BORDER));
        JSplitPane split=new JSplitPane(JSplitPane.VERTICAL_SPLIT,tableScroll,detailsPanel());split.setResizeWeight(.78);split.setBorder(null);split.setDividerSize(8);
        work.add(split,BorderLayout.CENTER);
        content.add(work,BorderLayout.CENTER);center.add(content,BorderLayout.CENTER);
        banner=Style.label("",12,false);banner.setForeground(Style.MUTED);
        JPanel bottom=new JPanel(new BorderLayout());bottom.setOpaque(false);bottom.add(banner,BorderLayout.WEST);
        JLabel mode=Style.label("Storage: "+service.persistenceMode()+"     |     Academic demo build v1.0",12,false);mode.setForeground(Style.MUTED);bottom.add(mode,BorderLayout.EAST);center.add(bottom,BorderLayout.SOUTH);
        root.add(center,BorderLayout.CENTER);replace(root);
        search.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){refreshRows();}public void removeUpdate(DocumentEvent e){refreshRows();}public void changedUpdate(DocumentEvent e){refreshRows();}});
        filter.addActionListener(e->refreshRows());
        table.getSelectionModel().addListSelectionListener(e->{if(!e.getValueIsAdjusting())updateDetails();});
        refresh();
    }
    private JPanel detailsPanel(){
        JPanel p=Style.panel();p.setLayout(new BorderLayout(0,8));p.setBorder(new EmptyBorder(10,8,0,8));
        p.add(title("Selected complaint details",13),BorderLayout.NORTH);
        details=new JTextArea("Select a complaint to inspect its full description and history.");details.setFont(Style.font(12,false));details.setForeground(Style.MUTED);details.setEditable(false);details.setOpaque(false);details.setLineWrap(true);details.setWrapStyleWord(true);details.setRows(4);p.add(details,BorderLayout.CENTER);
        return p;
    }
    private JPanel card(String heading,int count,String hint,Color accent){
        JPanel p=Style.panel();p.setBorder(new CompoundBorder(new MatteBorder(0,4,0,0,accent),new EmptyBorder(15,18,15,15)));
        p.setLayout(new BoxLayout(p,BoxLayout.Y_AXIS));
        JLabel label=Style.label(heading,12,false);label.setForeground(Style.MUTED);label.setAlignmentX(0);p.add(label);p.add(Box.createVerticalStrut(5));
        JLabel value=title(String.valueOf(count),28);value.setAlignmentX(0);p.add(value);
        JLabel desc=Style.label(hint,11,false);desc.setForeground(Style.MUTED);desc.setAlignmentX(0);p.add(desc);return p;
    }
    private void refresh(){
        if(currentUser==null)return;
        List<Complaint> tickets=service.visibleTo(currentUser);
        long open=tickets.stream().filter(t->t.getStatus()==Status.OPEN).count();
        long assigned=tickets.stream().filter(t->t.getStatus()==Status.ASSIGNED||t.getStatus()==Status.IN_PROGRESS).count();
        long resolved=tickets.stream().filter(t->t.getStatus()==Status.RESOLVED).count();
        cards.removeAll();cards.add(card("TOTAL REQUESTS",tickets.size(),"Visible in your account",Style.BLUE));
        cards.add(card("OPEN",(int)open,"Awaiting assignment",new Color(230,152,28)));
        cards.add(card("IN THE WORKFLOW",(int)assigned,"Assigned / in progress",new Color(122,93,205)));
        cards.add(card("RESOLVED",(int)resolved,"Completed requests",Style.GREEN));
        cards.revalidate();cards.repaint();refreshRows();
        banner.setText(currentUser.getRole()==Role.ADMIN ? "Suggested technician: "+Optional.ofNullable(service.recommendTechnician()).map(Technician::toString).orElse("None")+"    •    Pending priority queue: "+service.urgentQueue().size()+" tickets" : "Tip: click a ticket for details; all changes are saved automatically.");
    }
    private void refreshRows(){
        if(model==null||currentUser==null||search==null||filter==null)return;
        String q=search.getText().trim().toLowerCase(Locale.ROOT);
        String status=String.valueOf(filter.getSelectedItem());
        filtered=service.visibleTo(currentUser).stream().filter(c->q.isEmpty()||(
            c.getTitle()+" "+c.getLocation()+" "+c.getCategory()+" "+c.getId()+" "+c.getDescription()).toLowerCase(Locale.ROOT).contains(q))
            .filter(c->status.equals("All statuses")||c.getStatus().name().replace('_',' ').equalsIgnoreCase(status))
            .toList();
        model.setRowCount(0);
        for(Complaint c:filtered){
            User tech=directory.byId(c.getTechnicianId());
            model.addRow(new Object[]{"#"+c.getId(),c.getTitle(),c.getCategory(),c.getSeverity(),c.getLocation(),service.score(c),c.getStatus().name().replace('_',' '),tech==null?"Not assigned":tech.getName(),date.format(c.getCreatedAt())});
        }
        updateDetails();
    }
    private Complaint selected(){
        int row=table==null?-1:table.getSelectedRow();
        if(row<0)return null;
        int modelRow=table.convertRowIndexToModel(row);
        if(modelRow<0||modelRow>=filtered.size())return null;
        String idText=String.valueOf(model.getValueAt(modelRow,0));
        int id=Integer.parseInt(idText.substring(1));
        return filtered.stream().filter(t->t.getId()==id).findFirst().orElse(null);
    }
    private void updateDetails(){
        if(details==null)return;
        Complaint c=selected();
        if(c==null){details.setText("Select a complaint to inspect its full description and history.");return;}
        User student=directory.byId(c.getStudentId());
        details.setText("Ticket #"+c.getId()+"  •  Reported by: "+(student==null?c.getStudentId():student.getName())+"  •  Priority: "+service.score(c)+"/100\n"
            +"Description: "+c.getDescription()+"\nLocation: "+c.getLocation()+"  •  Last updated: "+date.format(c.getUpdatedAt()));
        details.setCaretPosition(0);
    }
    private void newComplaint(){
        JTextField title=new JTextField(), location=new JTextField();
        JTextArea desc=new JTextArea(4,27);desc.setLineWrap(true);desc.setWrapStyleWord(true);
        JComboBox<Category> category=new JComboBox<>(Category.values());
        JComboBox<Severity> severity=new JComboBox<>(Severity.values());
        JPanel form=new JPanel(new GridLayout(0,1,3,4));form.add(new JLabel("Title (5-90 chars):"));form.add(title);
        form.add(new JLabel("Description (10-1000 chars):"));form.add(new JScrollPane(desc));
        form.add(new JLabel("Building / room:"));form.add(location);
        form.add(new JLabel("Category:"));form.add(category);
        form.add(new JLabel("Severity:"));form.add(severity);
        int result=JOptionPane.showConfirmDialog(this,form,"New maintenance complaint",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(result==JOptionPane.OK_OPTION)try{
            Complaint c=service.create(currentUser,title.getText(),desc.getText(),location.getText(),(Category)category.getSelectedItem(),(Severity)severity.getSelectedItem());
            JOptionPane.showMessageDialog(this,"Complaint #"+c.getId()+" submitted successfully.\nPriority score: "+service.score(c)+"/100.");
        }catch(RuntimeException e){error(e.getMessage());}
    }
    private void assignTicket(){
        Complaint c=selected();if(c==null){error("Select a complaint in the table first.");return;}
        List<Technician> techs=directory.technicians();
        Technician suggestion=service.recommendTechnician();
        JComboBox<Technician> cb=new JComboBox<>(techs.toArray(new Technician[0]));cb.setSelectedItem(suggestion);
        JPanel box=new JPanel(new BorderLayout(0,10));box.add(new JLabel("Assign #"+c.getId()+" to a technician (recommended: "+suggestion+")"),BorderLayout.NORTH);box.add(cb,BorderLayout.CENTER);
        if(JOptionPane.showConfirmDialog(this,box,"Assign technician",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION)try{
            service.assign(currentUser,c.getId(),(Technician)cb.getSelectedItem());
            JOptionPane.showMessageDialog(this,"Technician assigned. Status is now ASSIGNED.");
        }catch(RuntimeException e){error(e.getMessage());}
    }
    private void advanceStatus(){
        Complaint c=selected();if(c==null){error("Select a complaint in the table first.");return;}
        Status next=switch(c.getStatus()){case ASSIGNED->Status.IN_PROGRESS;case IN_PROGRESS->Status.RESOLVED;default->null;};
        if(next==null){error("Ticket must be ASSIGNED or IN_PROGRESS to advance.");return;}
        if(JOptionPane.showConfirmDialog(this,"Move ticket #"+c.getId()+" from "+c.getStatus()+" to "+next+"?","Confirm status change",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)try{
            service.changeStatus(currentUser,c.getId(),next);
        }catch(RuntimeException e){error(e.getMessage());}
    }
    private void exportCsv(){
        JFileChooser chooser=new JFileChooser();chooser.setSelectedFile(new java.io.File("campusfix_export.csv"));
        if(chooser.showSaveDialog(this)==JFileChooser.APPROVE_OPTION){
            try{Path file=chooser.getSelectedFile().toPath();CsvExporter.export(file,service.visibleTo(currentUser),new WeightedPriorityScorer());JOptionPane.showMessageDialog(this,"Exported to: "+file.toAbsolutePath());}
            catch(Exception e){error("Unable to export file: "+e.getMessage());}
        }
    }
    private void error(String text){JOptionPane.showMessageDialog(this,text,"CampusFix",JOptionPane.WARNING_MESSAGE);}
}
