import java.util.*;
import java.io.*;
import java.net.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

class RequestProcessor extends Thread
{
private Socket socket;
private String id;
private FTServerFrame fsf;
RequestProcessor( Socket socket , String id , FTServerFrame fsf)
{
this.id = id;
this.fsf = fsf;
this.socket = socket;
start();
}

public void run()
{
try
{
SwingUtilities.invokeLater( new Runnable(){
public void run()
{
fsf.updateLog(" Client Connected and id Alloted is :" + id);
}
});

InputStream is = socket.getInputStream();
OutputStream os = socket.getOutputStream();
int byteToRecieve = 1024;
byte tmp[] = new byte[1024];
byte header[] = new byte[1024];
int byteReadCount;
int i,j,k;
i=0;
j=0;
while( j< byteToRecieve)
{
byteReadCount = is.read(tmp);
if( byteReadCount == -1) continue;

for( k=0; k<byteReadCount; k++)
{
header[i] = tmp[k];
i++;
}
j = j+ byteReadCount;
}

int lengthOfFile = 0;
i=0;
j=1;
while( header[i] != ',')
{
lengthOfFile = lengthOfFile + ( header[i]*j);
j = j*10;
i++;
}
i++;
StringBuffer sb = new StringBuffer();
while( i<=1023)
{
sb.append((char)header[i]);
i++;
}

String fileName = sb.toString().trim();
int lof = lengthOfFile;
SwingUtilities.invokeLater(()-> {
fsf.updateLog(" Recieiving file :" + fileName + " of Length :" +  lof);
});


File file = new File("uploads" + File.separator + fileName);
if( file.exists()) file.delete();

FileOutputStream fos = new FileOutputStream(file);
byte ack[] = new byte[1];
ack[0] = 1;
os.write( ack , 0 , 1);
os.flush();

int chunkSize = 4096;
byte bytes[] = new byte[chunkSize];
i=0;
long m;
m=0;
while( m < lengthOfFile)
{
byteReadCount = is.read(bytes);
if( byteReadCount == -1) continue;

fos.write( bytes , 0 , byteReadCount);
fos.flush();
m = m + byteReadCount;
}
fos.close();
ack[0] = 1;
os.write( ack , 0 , 1);
os.flush();
socket.close();

SwingUtilities.invokeLater(()->{

fsf.updateLog(" File Saved to :" + file.getAbsolutePath());
fsf.updateLog(" Connection with Client  whose id is :" + id+ " closed");
});
}catch( Exception e)
{
System.out.println(e);
}
}
}


class FTServer extends Thread
{
private ServerSocket serverSocket;
private FTServerFrame fsf;


FTServer( FTServerFrame fsf)
{
this.fsf = fsf;
}
public void run() 
{
try
{
serverSocket = new ServerSocket(5500);
startListening();
}catch( Exception e)
{
System.out.println(e);
}
}

public void shutDown()
{
try
{
serverSocket.close();
}catch( Exception e)
{
System.out.println(e);
}
}




private void startListening()
{
try
{
Socket socket;
RequestProcessor requestProcessor;
while(true)
{

SwingUtilities.invokeLater( new Thread()
{
public void run()
{
fsf.updateLog(" Server started and is listening on port 5050");
}
});

socket = serverSocket.accept();
requestProcessor = new RequestProcessor(socket , UUID.randomUUID().toString() , fsf);
}
}catch(Exception e)
{
System.out.println(" Server Stop Listening");

System.out.println(e);
}
}
}

class FTServerFrame extends JFrame implements ActionListener
{
private FTServer server;
private JButton button;
private Container container;
private JTextArea jta;
private JScrollPane jsp;
private boolean serverState = false;


FTServerFrame()
{
container = getContentPane();
container.setLayout( new BorderLayout());
button = new JButton("start");

jta = new JTextArea();
jsp = new JScrollPane( jta , ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS , ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
container.add( jsp , BorderLayout.CENTER);
container.add( button , BorderLayout.SOUTH);
server = new FTServer(this);
setLocation( 100  , 100);
setSize( 500 , 500);
setVisible(true);
button.addActionListener(this);
}


public void updateLog( String message)
{
jta.append( message + "\n");
}
public void actionPerformed( ActionEvent ev)
{
if( serverState == false)
{
server =  new FTServer(this);
server.start();
serverState = true;
button.setText("stop");
}
else
{
server.shutDown();
serverState = false;
button.setText("Start");
jta.append("Server Stopped\n");
}
}

public static void main ( String gg[])
{
FTServerFrame fsf = new FTServerFrame();
}
}
