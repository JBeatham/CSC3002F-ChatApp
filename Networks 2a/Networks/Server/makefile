
BINDIR = bin
TESTDIR = test
DOCDIR = doc

JAVAC = javac
JFLAGS = -g 


vpath %.class 
# define general build rule for java sources
.SUFFIXES:  .java  .class

.java.class:
	$(JAVAC)  $(JFLAGS)  $<

#default rule - will be invoked by make


all: 	ClientConnection.class PublicMessageTransmitter.class ServerThread.class OutputMessageThread.class FileSend.class InputMessageThread.class ServerHub.class ServerDriver.class


		
clean:
	@rm -f  $(BINDIR)/*.class
	@rm -f *.class
	@rm -f $(BINDIR)/*/*.class

