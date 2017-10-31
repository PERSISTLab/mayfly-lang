#include "i2c_ctl.h"
#include <msp430.h>

void i2c_init(uint8_t address) {
	// Software reset enabled
	UCB0CTLW0 |= UCSWRST;
	// Baud rate
	UCB0BR0 = 32;
	UCB0CTLW0 |= UCMODE_3 | UCMST | UCSYNC;   // I2C mode, Master mode, sync
	UCB0I2CSA = address;                         // Slave address for AB0815
	UCB0CTL1 &= ~UCSWRST;
	//UCB0IE |= UCTXIE0 | UCRXIE0 | UCSTPIE;
}

void i2c_change_address(uint8_t address) {
	// Software reset enabled
	UCB0CTLW0 |= UCSWRST;
	UCB0I2CSA = address;                         // Slave address for AB0815
	UCB0CTL1 &= ~UCSWRST;
}

uint8_t is_i2c_bus_busy() {
  return (UCB0STAT & UCBBUSY);
}

void i2c_read_register_sequence(uint8_t start_offset, uint8_t num_reads, uint8_t values[]) {
	while(is_i2c_bus_busy());
	UCB0CTL1 |= UCTR + UCTXSTT; 
	// Wait for start
	while(UCB0CTL1 & UCTXSTT);

	// TX buf empty
	while(!(UCB0IFG & UCTXIFG0));     
	UCB0TXBUF = start_offset;
	while(!(UCB0IFG & UCTXIFG0));

	// Start condition again (a restart)
	UCB0CTL1 &= ~(UCTR);
	UCB0CTL1 |= UCTXSTT; 
	// Wait for start
	while(UCB0CTL1 & UCTXSTT);

	uint8_t bytes_read = 0;
	while(bytes_read < (num_reads-1)) {
		// Byte rxed
		while(!(UCB0IFG & UCRXIFG0));     
		values[bytes_read++] = UCB0RXBUF;
	}
	UCB0CTL1 |= UCTXSTP;
	
	while(!(UCB0IFG & UCRXIFG0));     
	values[bytes_read] = UCB0RXBUF;
	while(UCB0CTL1 & UCTXSTP);
	uint8_t temp = UCB0RXBUF;
	
}
/*
volatile uint8_t bytes_read;
void i2c_read_register_sequence(uint8_t start_offset, uint8_t num_reads, uint8_t values[]) {
	//if(num_reads < 2) num_reads = 2;
	while(is_i2c_bus_busy());
	UCB0CTL1 |= UCTR + UCTXSTT; 
	// Wait for start
	while(UCB0CTL1 & UCTXSTT);

	// TX buf empty
	while(!(UCB0IFG & UCTXIFG0));     
	UCB0TXBUF = start_offset;
	while(!(UCB0IFG & UCTXIFG0));

	// Start condition again (a restart)
	UCB0CTL1 &= ~(UCTR);
	UCB0CTL1 |= UCTXSTT; 
	// Wait for start
	while(UCB0CTL1 & UCTXSTT);

	// Now read out the sequence of registers starting at the offset
	bytes_read = 0;
	while(bytes_read < (num_reads -1)) {
		// Byte rxed
		while(!(UCB0IFG & UCRXIFG0));     
		values[bytes_read++] = UCB0RXBUF;
	}
	
	// Set the stop before recieving the next buffer of data to make sure NAck is generated
	UCB0CTL1 |= UCTXSTP;
	while(!(UCB0IFG & UCRXIFG0));     
	values[bytes_read++] = UCB0RXBUF;

	
	// Wait for stop
	while(UCB0CTL1 & UCTXSTP);
}*/

/**
 * Returns the 8-bit value of the AB0805 register specified. 
 * @param  offset [description]
 * @return        [description]
 */
uint8_t i2c_read_register(uint8_t offset) {
	uint8_t values[2];
	i2c_read_register_sequence(offset, 1, values);
	return values[0];
}

void i2c_write_register(uint8_t offset, uint8_t value) {
	// Wait for bus to be free
	while(is_i2c_bus_busy());
	// Send start
	UCB0CTL1 |= UCTR + UCTXSTT; 
	// Wait for start
	while(UCB0CTL1 & UCTXSTT);
	// TX buf empty
	while(!(UCB0IFG & UCTXIFG0));     
	UCB0TXBUF = offset;
	while(!(UCB0IFG & UCTXIFG0));
	UCB0TXBUF = value;
	while(!(UCB0IFG & UCTXIFG0));
	UCB0CTL1 |= UCTXSTP;
	while(UCB0CTL1 & UCTXSTP);
}
