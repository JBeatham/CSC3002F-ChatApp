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


all: 	ClientThread.class client.class


		
clean:
	@rm -f  $(BINDIR)/*.class
	@rm -f *.class
	@rm -f $(BINDIR)/*/*.class

