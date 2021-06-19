ssptest: main.o
	g++ main.o -o ssptest -lpthread -m32 -DHOST

main.o:
	g++ -c main.cpp -o main.o -std=c++11 -m32 -DHOST

clean:
	rm ssptest *.o
