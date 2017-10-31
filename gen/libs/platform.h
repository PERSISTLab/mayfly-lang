#ifndef PLATFORM_H
#define PLATFORM_H

#include <msp430fr5969.h>
#include <stdint.h>
#include <stdbool.h>

#define SLEEP_TIME_100NANOS 1000 // 100ms sleep time in schedule loop

static inline void log_debug(uint32_t val) {
    P1OUT &= ~BIT2;
    UCA1TXBUF = val >> 24;
    while(!(UCA1IFG & UCRXIFG));
    UCA1TXBUF = val >> 16;
    while(!(UCA1IFG & UCRXIFG));
    UCA1TXBUF = val >> 8;
    while(!(UCA1IFG & UCRXIFG));
    UCA1TXBUF = val;
    while(!(UCA1IFG & UCRXIFG));
    P1OUT |= BIT2;
}

uint32_t mayfly_hw_init();
bool was_rtc_reset_by_power_failure();
uint32_t get_time();
void __led1_on();
void __led1_off();
void __led2_on();
void __led2_off();


#endif // PLATFORM_H
