#include "platform.h"
#include "ab0805.h"
#include "globals.h"
#include "spi.h"

uint8_t rtc_was_reset = 0;

void config_compe() {
  // Setup Comparator_E
  CECTL0 = CEIPEN | CEIPSEL_5;              // Enable V+, input channel C5
  CECTL1 = CEPWRMD_2;                       // ultra low power mode
  CECTL2 = CEREFL_1 | CERS_3 | CERSEL;      // VREF is applied to -terminal
                                            // R-ladder off; bandgap ref voltage
                                            // supplied to ref amplifier to get Vcref=1.2V
  CECTL3 = CEPD5;                           // Input Buffer Disable for C5 
  CECTL1 |= CEON;                           // Turn On Comparator_E
  CEINT |= CEIE;
  __delay_cycles(75);                       // delay for the reference to settle
}


// Returns the curretn RTC time
uint32_t mayfly_hw_init() {
  // Stop WDT
  WDTCTL = WDTPW | WDTHOLD;                 

  // LED pins
  P1DIR |= BIT0;
  P1OUT &= ~BIT0;
  P4DIR |= BIT6;
  P4OUT &= ~BIT6;

  // Setup startup interrupt from RTC
  P3DIR &= ~BIT5;  // make this pin input
  P3REN |= BIT5;   // turn on internal pull resistor
  P3OUT |= BIT5;   // pullup to VCC
  P3IE |= BIT5;   // Enable pin interrupt
  P3IES &= ~BIT5;   // low to high transition fires interrupt
  P3IFG &= ~BIT5;  // clear interrupt  

  // Power to ADXL362 and CS
  PDIR_ACCEL_CS |= PIN_ACCEL_CS;
  POUT_ACCEL_CS &= ~PIN_ACCEL_CS;
  PDIR_ACCEL_EN |= PIN_ACCEL_EN;
  POUT_ACCEL_EN &= ~PIN_ACCEL_EN;  

  // I2C Pins 
  P1DIR |= (BIT6 | BIT7);
  P1OUT &= ~(BIT6 | BIT7);

  P1SEL1 |= (BIT6 | BIT7);    
  P1SEL0 &= ~(BIT6 | BIT7);   

  // COMPE sel C5 (p1.5)
  P1DIR &= ~BIT5;
  P1SEL0 |= BIT5;
  P1SEL1 |= BIT5;

  // Disable the GPIO power-on default high-impedance mode to activate
  // previously configured port settings
  PM5CTL0 &= ~LOCKLPM5;


  // Wait for VCC above 1.2V divider (attach external signal to P1.5)
  config_compe();
  __bis_SR_register(LPM3_bits + GIE);      // LPM3, COMPE_ISR will force exit
  __no_operation();                        // For debug only

   // SPI pins
  P2SEL1 |= (BIT4 | BIT5 | BIT6);
  // Debug SPI CS pin
  P1DIR |= BIT2;
  P1OUT |= BIT2;
  
  // Configure clock speed to 8MHz
  CSCTL0_H = CSKEY >> 8;                    // Unlock CS registers
  CSCTL1 = DCOFSEL_6;                       // Set DCO to 8MHz
  CSCTL2 = SELA__VLOCLK | SELS__DCOCLK | SELM__DCOCLK;  // Set SMCLK = MCLK = DCO
                                            // ACLK = VLOCLK
  CSCTL3 = DIVA__1 | DIVS__1 | DIVM__1;     // Set all dividers to 1
  CSCTL0_H = 0;                             // Lock CS registers

  // Configure the sleep timer
  TA0CCTL0 = CCIE;                          // TACCR0 interrupt enabled
  TA0CCR0 = 1000;
  TA0CTL = TASSEL__ACLK | MC__UP | TAIE;   // ACLK, continuous mode

  // We need to sleep till the RTC becomes available; if the timer expired, this could take 0.4s
  // otherwise this happens almost instantaneously
  if(!(P3IN & BIT5)) {
    __bis_SR_register(LPM3_bits + GIE);      // LPM3, pin will force exit
    __no_operation();                        // For debug only
  }

  // Init SPI for debug and Xl
  SPI_initialize();
  // Init I2C bus for RTC
  rtc_i2c_bus_init();

  // Test the connection
  if(rtc_testConnection()) {
#ifdef DEBUG    
    P1OUT |= BIT0;
    P1DIR |= BIT0;
#endif
    log_debug32(120);
  } 
  
  // Test wether the RTC lost power, i.e. RAM was reset, if so, then re init
  uint8_t is_reset_ram = rtc_read_ram(0);
  if(is_reset_ram != 0xbb) {
    rtc_was_reset = 1;
    rtc_initialize();  
    rtc_write_ram(0, 0xbb);
  }

  // Start rtc clock
  rtc_startClock();
  return rtc_get_time();
}

// Resets the flag once consumed
bool was_rtc_reset_by_power_failure() {
  uint8_t tt = rtc_was_reset;
  rtc_was_reset = 0;
  return tt;
}

uint32_t get_time() {
  return rtc_get_time_long();
  //return rtc_get_seconds();
  //return rtc_get_time();
}

void __led1_on() {
  P4OUT |= BIT6;
  P4DIR |= BIT6;
}

void __led1_off() {
  P4OUT &= BIT6;
}

void __led2_on() {
  P1OUT |= BIT0;
  P1DIR |= BIT0;
}

void __led2_off() {
  P1OUT &= BIT0;
}

// P3 interupt that tells us that the RTC is ready to go
void __attribute__((__interrupt__(PORT3_VECTOR))) port3_interrupt (void) {
  P3IE &= ~BIT5;   // Disable pin interrupt
  __bic_SR_register_on_exit(LPM3_bits); // Exit to active CPU
}

// Comparator interrupt that tells us that we are at a high enough VCAP voltage to begin execution
void __attribute__ ((interrupt(COMP_E_VECTOR))) COMPE_ISR (void) {
  CEINT &= ~CEIE;
  CECTL1 &= ~CEON;                           // Turn Off Comparator_E
  __bic_SR_register_on_exit(LPM3_bits); // Exit active CPU
}