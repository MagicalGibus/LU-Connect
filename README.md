# LU-Connect
Term project for LZSCC.232

In order to compile the program on a Windows machine, navigate to the java directory type the command: 
"javac Authentication/*.java Client/*.java Encryption/*.java FileTransfer/*.java Server/*.java -d ."

While still in the java directory, you can run the Server by typing:
java Server.Main

I have disabled the feautre to choose the ip or port number when creating, the server is bound to localhost and to port 1060, this can be easily changed manually however.

You can then run the Client by typing:
java Client.Main 127.0.0.1 1060

The IP and port number can be changed if necessary here, however in this case you should stick to localhost and 1060
