CC=gcc
#CFLAGS?=-Wall -ggdb

LDFLAGS+=-lpcap -lrt 
PROGRAMS=ipv6-agent
LIBS=thc-ipv6-lib.o

PREFIX=/usr/local

# Number of RTTs and IATs in learn mode
# IAT?=5000
# RTT?=500000

# Number of RTTs and IATs in measure mode
IAT?=100
RTT?=10000

IFACE?=enp5s0

all:	$(LIBS) $(PROGRAMS)

%:	%.c $(LIBS)
	$(CC) -o $@ $^ $(LDFLAGS)

run: $(PROGRAMS)
	sudo ./ipv6-agent $(IFACE) $(RTT) $(IAT)
	
clean:
	rm -f $(PROGRAMS) $(LIBS) core DEADJOE *~

backup:	clean
	tar czvf ../thc-ipv6-bak.tar.gz *
	sync

.PHONY: all install clean
