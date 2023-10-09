import java.io.*;
import java.util.*;
import java.net.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.table.*;

class FileUploadEvent 
{
private String uploaderId;
private File file;
private long numberOfBytesUploaded;
public FileUploadEvent()
{
this.uploaderId = null;
this.file = null;
this.numberOfBytesUploaded = 0;
}
public void setUploaderId( String uploaderId)
{
this.uploaderId = uploaderId;
}

public String getUploaderId()
{
return this.uploaderId;
}


public void setFiles( File file)
{
this.file = file;
}

public File getFiles()
{
return this.file;
}

public void setNumberOfBytesUploaded( long  numberOfBytesUploaded)
{
this.numberOfBytesUploaded = numberOfBytesUploaded;
}

public long getNumberOfBytesUploaded()
{
return this.numberOfBytesUploaded;
}
}


interface FileUploadListener
{
public void fileUploadStatusChanged( FileUploadEvent fileUploadEvent);
}
class FileModel extends AbstractTableModel
{
private ArrayList<File> files;
FileModel()
{
files = new ArrayList<>();
}
public ArrayList<File> getFiles()
{
return files;
}
public int getRowCount()
{
return this.files.size();
}
public int getColumnCount()
{
return 2;
}
public String getColumnName( int c)
{
if( c== 0 ) return "S.NO";
return "Files";
}
public Class getColumnClass( int c)
{
if( c== 0) return Integer.class;
return String.class;
}
public boolean isCellEditable( int r , int c)
{
return false;
}
public Object getValueAt( int r  , int c)
{
if( c == 0) return ( r+1);
return this.files.get(r).getAbsolutePath();
}
public void add( File file)
{
this.files.add(file);
fireTableDataChanged();
}
}

class FTClientFrame extends JFrame
{
private String host;
private int portNumber;
private FileSelectionPanel fileSelectionPanel;
private FileUploadViewPanel fileUploadViewPanel;
private Container container;
FTClientFrame( String host , int portNumber)
{
this.host = host;
this.portNumber = portNumber;
fileSelectionPanel = new FileSelectionPanel();
fileUploadViewPanel = new FileUploadViewPanel();
container = getContentPane();
container.setLayout( new GridLayout( 1 , 2));
container.add( fileSelectionPanel);
container.add( fileUploadViewPanel);
setSize( 1200 , 600);
setLocation( 10 , 20);
setVisible(true);
}

class FileSelectionPanel extends JPanel implements ActionListener
{
private JLabel titleLabel;
private FileModel model;
private JTable table;
private JScrollPane jsp;
private JButton addFileButton;

FileSelectionPanel()
{
setLayout( new BorderLayout());
titleLabel = new JLabel("Selected Files");
model = new FileModel();
table = new JTable(model);
jsp = new JScrollPane( table , ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS , ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
addFileButton = new JButton("Add File");
addFileButton.addActionListener(this);
add( titleLabel , BorderLayout.NORTH);
add( jsp , BorderLayout.CENTER);
add( addFileButton , BorderLayout.SOUTH);
}

public ArrayList<File> getFiles()
{
return model.getFiles();
}

public void actionPerformed( ActionEvent ev)
{
JFileChooser jfc = new JFileChooser();
jfc.setCurrentDirectory( new File("."));
int selectedOption = jfc.showOpenDialog(this);
if( selectedOption == jfc.APPROVE_OPTION)
{
File selectedFile = jfc.getSelectedFile();
model.add(selectedFile);
}
}
}

class FileUploadViewPanel extends JPanel implements  ActionListener , FileUploadListener
{
private JButton uploadFileButton;
private JPanel  progressPanelsContainer;
private JScrollPane jsp;
private ArrayList<ProgressPanel> progressPanels;
ArrayList<File> files;
ArrayList<FileUploadThread>fileUploaders;

FileUploadViewPanel()
{
uploadFileButton  = new JButton("upload Files");
setLayout( new BorderLayout());
add( uploadFileButton , BorderLayout.NORTH);
uploadFileButton.addActionListener(this); 
}

public void actionPerformed( ActionEvent ev)
{
files = fileSelectionPanel.getFiles();
if( files.size() == 0)
{
JOptionPane.showMessageDialog( FTClientFrame.this , " No Files select to upload");
return;
} 
progressPanelsContainer = new JPanel();
progressPanelsContainer.setLayout( new GridLayout( files.size() , 1));
ProgressPanel progressPanel;
progressPanels = new ArrayList<>();
fileUploaders = new ArrayList<>();
FileUploadThread fut;
String uploaderId;
for( File file : files)
{
uploaderId = UUID.randomUUID().toString();
progressPanel = new ProgressPanel( uploaderId , file); 
progressPanels.add( progressPanel);
progressPanelsContainer.add( progressPanel);
fut = new FileUploadThread( this , uploaderId , file ,    host , portNumber);
fileUploaders.add(fut);
}
jsp = new JScrollPane( progressPanelsContainer , ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS , ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
add( jsp , BorderLayout.CENTER);
this.revalidate();
this.repaint();

for( FileUploadThread fileUploadThread : fileUploaders )
{
fileUploadThread.start();
}
}

public void fileUploadStatusChanged( FileUploadEvent fileUploadEvent)
{
String uploaderId = fileUploadEvent.getUploaderId();
long  numberOfBytesUploaded = fileUploadEvent. getNumberOfBytesUploaded();
File file = fileUploadEvent.getFiles();
 
for( ProgressPanel progressPanel : progressPanels)
{
if( progressPanel.getId().equals(uploaderId))
{
progressPanel.updateProgressBar(numberOfBytesUploaded);
break;
}
}
}

class ProgressPanel extends JPanel
{
private File file;
private JLabel fileNameLabel;
private JProgressBar progressBar;
private long fileLength; 
private String id;

public ProgressPanel( String id  , File file) 
{
this.id= id;
this.file = file;
this.fileLength = file.length();
fileNameLabel = new JLabel("Uploading:"   + file.getAbsolutePath()); 
progressBar = new JProgressBar(1 , 100);
setLayout( new GridLayout( 2 , 1));
add(fileNameLabel);
add(progressBar);
}

public String getId()
{
return id;
}

public void updateProgressBar( long bytesUploaded)
{
int percentage;
if( bytesUploaded == fileLength) percentage = 100;
else
percentage = (int)(( bytesUploaded * 100)/fileLength);
progressBar.setValue(percentage);

if( percentage == 100)
{
fileNameLabel.setText("Uploaded:" + file.getAbsolutePath()); 
}  
}
}
}

public static void main ( String gg[])
{
FTClientFrame fcf = new FTClientFrame( "localhost" , 5500 );
}
}

class FileUploadThread extends Thread
{
private FTClientFrame fcf;
private String id;
private File file;
private String host;
private int portNumber;
private FileUploadListener fileUploadListener;

FileUploadThread( FileUploadListener fileUploadListener , String id , File file , String host , int portNumber)
{
this.fileUploadListener = fileUploadListener ;
this.id = id;
this.file= file;
this.host = host;
this.portNumber = portNumber;
}

public void run()
{
try
{
long lengthOfFile = file.length();
String name = file.getName();
byte header[] = new byte[1024];
int i;
long x,k;
i=0;
k = lengthOfFile;
while( k >0 )
{
header[i] = (byte)(k%10);
k = k/10;
i++;
}
header[i] = (byte)',';
i++;
x = name.length();
int r=0;
while( r < x)
{
header[i] = (byte)name.charAt(r);
i++;
r++;
}
while( i <= 1023)
{
header[i] = (byte)32;
i++;
}
Socket socket = new Socket( host , portNumber);
OutputStream os = socket.getOutputStream();
os.write( header , 0 , 1024);
os.flush();
InputStream is = socket.getInputStream();
byte ack[] = new byte[1];
int bytesReadCount;
while(true)
{
bytesReadCount = is.read(ack);
if( bytesReadCount == -1) continue;
break;
}
FileInputStream fis = new FileInputStream(file);
int chunkSize = 4096;
byte bytes[] = new byte[chunkSize];
int j=0;
while( j < lengthOfFile)
{
bytesReadCount = fis.read(bytes);
os.write( bytes , 0 , bytesReadCount);
os.flush();
j = j+ bytesReadCount;
long brc = j;
SwingUtilities.invokeLater(()->{
FileUploadEvent fue = new FileUploadEvent();
fue.setUploaderId(id);
fue.setFiles(file);
fue.setNumberOfBytesUploaded(brc);
fileUploadListener.fileUploadStatusChanged(fue);
});
}
fis.close();
while( true)
{
bytesReadCount = is.read(ack);
if( bytesReadCount == -1) continue;
break;
}
socket.close();
}catch( Exception exception)
{
System.out.println(exception.getMessage());
}
}
}