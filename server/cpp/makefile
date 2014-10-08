CFLAGS= -lboost_system

all: send receive

send: sendServer.o
	g++ $(CFLAGS) sendServer.o -o send

send.o: sendServer.cpp
	g++ -c sendServer.cpp

receive: receiveServer.o
	g++ $(CFLAGS) receiveServer.o -o receive

receive.o: receiveServer.cpp
	g++ -c receiveServer.cpp

clean:
	rm -rf *.o send receive
