NAME		?=  main
SOURCES 	=  $(wildcard *.c) $(wildcard libs/*.c)
OBJECTS 	=  $(patsubst %.c,%.o,$(SOURCES))
EXE 		=  $(NAME).out
CC          = msp430-elf-gcc
DEVICE  	= msp430fr5969
GDB     	= msp430-elf-gdb


# GCC flags
MSPGCC_ROOT			=

CSTD_FLAGS 			= -funsigned-char -std=c99
DEBUG_FLAGS 		= -g3 -ggdb -gdwarf-2
ERROR_FLAGS 		= -Wall -Wextra -Wshadow -fmax-errors=5
NO_ERROR_FLAGS 		= -Wno-unused-parameter -Wno-unknown-pragmas -Wno-unused-variable -Wno-type-limits -Wno-comment
LIB_INCLUDES 		= -I $(MSPGCC_ROOT)/../include/ -I. -Ilibs/
MSP430_FLAGS 		= -mmcu=$(DEVICE) -mhwmult=none -D__$(DEVICE)__ -DDEPRECATED -mlarge
REDUCE_SIZE_FLAGS	= -fdata-sections -ffunction-sections -finline-small-functions -O0
CFLAGS 				= $(CSTD_FLAGS) $(DEBUG_FLAGS) $(ERROR_FLAGS) $(NO_ERROR_FLAGS) $(LIB_INCLUDES) $(MSP430_FLAGS) $(REDUCE_SIZE_FLAGS) $(MSPSIM)
	 
LFLAGS = -Wl,--gc-sections -Wl,--reduce-memory-overheads -Wl,--stats -Wl,--relax
LIBS = -L $(MSPGCC_ROOT)/../include/

all: compile

$(NAME).o: $(NAME).c
	$(CC) $(CFLAGS) -c $(LFLAGS) $< -o $@

%.o: %.c 
	$(CC) $(CFLAGS) -c $(LFLAGS) $< -o $@

%.asm: %.c 
	$(CC) $(CFLAGS) -S -fverbose-asm -c $< -o $@

# Change -o to $(DEVICE) when we need multiple devices
compile: $(OBJECTS) 
	$(CC) $(CFLAGS) $(OBJECTS) $(LFLAGS) -DDEVICEID=$(ID) -o $(EXE) $(LIBS) 

install: all
	mspdebug tilib "prog $(EXE)"

debug: all
	mspdebug tilib gdb &

gdb:
	rm -f gdb.cmd
	printf "file main.out\ntarget remote localhost:2000\nbreak main\nload main.out\nc\n" >> gdb.cmd
	$(GDB) -x gdb.cmd
	rm -f gdb.cmd

clean:
	rm -rf $(OBJECTS) *.asm 
	rm -f $(EXE)
	rm -f gdb.cmd
