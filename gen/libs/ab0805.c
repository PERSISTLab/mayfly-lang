#include "ab0805.h"

uint8_t read_buf[6];

uint8_t rtc_get_address() {
  return AB0805_ADDRESS;
}

void rtc_i2c_bus_init() {
  i2c_init(AB0805_ADDRESS);
}

void rtc_initialize() {
  uint8_t stopClk = 0x91;
  uint8_t allowOscEdit = 0xA1;
  uint8_t rcOscSelNoCal = 0b10011000;
  i2c_write_register(AB0805_RA_CONTROL1, stopClk);          // stops clock
  i2c_write_register(AB0805_RA_CONFIG_KEY, allowOscEdit);   // Allows edits to osc. cntrl register (0x1C)
  i2c_write_register(AB0805_RA_OSC_CONTROL, rcOscSelNoCal);      // RC used, no calibration


  // Now set the low power mode settings
  uint8_t allowAnalogEdit = 0x9D;
  uint8_t trickleSettings = 0b10100101;     
  uint8_t brefSetting = 0b01110000;
  i2c_write_register(AB0805_RA_CONFIG_KEY, allowAnalogEdit); // Allow setting trickle charger settings
  i2c_write_register(AB0805_TRICKLE, trickleSettings);       // Enable trickle charger, set schottkey diode, and 6k res
  i2c_write_register(AB0805_RA_CONFIG_KEY, allowAnalogEdit); // Allow setting trickle charger settings
  i2c_write_register(AB0805_BREF, brefSetting);       // Enable trickle charger, set schottkey diode, and 6k res
  i2c_write_register(AB0805_RA_CONFIG_KEY, allowAnalogEdit); // Allow setting trickle charger settings
  i2c_write_register(AB0805_BATMODE, 0x00);       // Disable IOBM so No IO access when powered by VBAT

}

bool rtc_testConnection() {
  // Confirm I2C bus is working and RTC chip exists
  uint8_t values[2];
  i2c_read_register_sequence(AB0805_RA_ID0, 2, values);
  if(values[0] == 0x08 && values[1] == 0x05) {
    return true;
  } else {
    return false;
  }
}

void rtc_startClock() {
  uint8_t startClk = 0b00010001;
  i2c_write_register(AB0805_RA_CONTROL1, startClk);          // stops clock

}

void rtc_stopClock() {
  uint8_t stopClk = 0b10010001;
  i2c_write_register(AB0805_RA_CONTROL1, stopClk);          // stops clock
}

uint8_t rtc_get_seconds() {
  // Byte: [7 = CH] [6:4 = 10SEC] [3:0 = 1SEC]
  uint8_t valread = i2c_read_register(AB0805_RA_SECONDS);
  return (valread & 0x0F) + (valread >> 4) * 10;
} // 0-59

uint8_t rtc_get_minutes() {
  uint8_t valread = i2c_read_register(AB0805_RA_MINUTES);
  return (valread & 0x0F) + ((valread & 0x70) >> 4) * 10;
} //0-59

uint8_t rtc_get_hours() {
  uint8_t valread = i2c_read_register(AB0805_RA_HOURS);
  // bit 6 is low, 24-hour mode (default)
  // Byte: [5:4 = 10HR] [3:0 = 1HR]
  return (valread & 0x0F) + ((valread & 0x30) >> 4) * 10; 
}  // 0-23

uint8_t rtc_get_day() {
    // Byte: [7:6 = 0] [5:4 = 10DAY] [3:0 = 1DAY]
    uint8_t valread = i2c_read_register(AB0805_RA_DATE);
    return (valread & 0x0F) + ((valread & 0x30) >> 4) * 10;
}  // 1-31 

uint8_t rtc_get_month() {
  // Byte: [7:5 = 0] [4 = 10MONTH] [3:0 = 1MONTH]
  uint8_t valread = i2c_read_register(AB0805_RA_MONTH);
  return (valread & 0x0F) + ((valread & 0x10) >> 4) * 10;
} // 1-12

uint8_t rtc_get_years() {
  // Byte: [7:4 = 10s] [3:0 = 1s]
  uint8_t valread = i2c_read_register(AB0805_RA_YEAR);
  return (valread & 0x0F) + (valread >> 4) * 10;
} // 0-99

uint8_t rtc_read_ram(uint8_t offset) {
  return i2c_read_register(AB0805_NORMAL_RAM_START+offset);
}

uint32_t rtc_get_time_long() {
  return rtc_get_seconds() + rtc_get_minutes() * 60 + rtc_get_hours() * 3600;
}
uint32_t rtc_get_time() {
  // Read from seconds to years
  i2c_read_register_sequence(AB0805_RA_SECONDS, 6, read_buf);
  uint8_t secs =  (read_buf[0] & 0x0F) + (read_buf[0] >> 4) * 10;
  uint8_t mins =  (read_buf[1] & 0x0F) + ((read_buf[1] & 0x70) >> 4) * 10;
  uint8_t hours = (read_buf[2] & 0x0F) + ((read_buf[2] & 0x30) >> 4) * 10;
  uint8_t days =  (read_buf[3] & 0x0F) + ((read_buf[3] & 0x30) >> 4) * 10;
  uint8_t mths =  (read_buf[4] & 0x0F) + ((read_buf[4] & 0x10) >> 4) * 10;
  uint8_t years = (read_buf[5] & 0x0F) + (read_buf[5] >> 4) * 10;
  return secs + mins * 60 + hours * 3600 + (days-1) * 86400 + (mths-1) * 2592000 + years * 31104000;
}

void rtc_write_ram(uint8_t offset, uint8_t value) {
  i2c_write_register(AB0805_NORMAL_RAM_START+offset, value);
}
