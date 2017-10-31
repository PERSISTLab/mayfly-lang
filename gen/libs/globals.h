#ifndef GLOBALS_H_
#define GLOBALS_H_

#include <msp430.h>
#include <stdint.h>

#define BITSET(port,pin)    port |= (pin)
#define BITCLR(port,pin)    port &= ~(pin)
#define BITTOG(port,pin)    port ^= (pin)

/* Global constants */
#define FOREVER         (1)
#define NEVER           (0)

#define TRUE            (1)
#define FALSE           (0)

#define HIGH            (1)
#define LOW             (0)

#define FAIL            (0)
#define SUCCESS         (1)

// Boolean type
typedef uint8_t     BOOL;

typedef unsigned letter_t;
typedef unsigned sample_t;

/* Platform hardware pinouts */

// ADXL362
#define PDIR_ACCEL_CS P4DIR
#define POUT_ACCEL_CS P4OUT
#define PIN_ACCEL_CS BIT2

#define PDIR_ACCEL_EN P3DIR
#define POUT_ACCEL_EN P3OUT
#define PIN_ACCEL_EN BIT0

// Timekeeper, AB0805
#define POUT_TIME_IRQ P3OUT
#define PIN_TIME_IRQ BIT5



#endif /* GLOBALS_H_ */
