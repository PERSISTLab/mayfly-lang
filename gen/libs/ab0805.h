/**
 * Parts of this were adapted from:
 * https://github.com/curransinha/AB0805_lib
 *
 * @author Josiah Hester, 7/19/2016
 */
#ifndef AB0805_H
#define AB0805_H

#include "ab0805_regs.h"
#include <stdint.h>
#include <stdbool.h>
#include "i2c_ctl.h"
#include "platform.h"


void rtc_i2c_bus_init();
void rtc_initialize();
bool rtc_testConnection();
void rtc_startClock();
void rtc_stopClock();

uint8_t rtc_get_seconds(); // 0-59
uint8_t rtc_get_minutes(); //0-59
uint8_t rtc_get_hours();	// 0-23
uint8_t rtc_get_day();	// 1-31	
uint8_t rtc_get_month(); // 1-12
uint32_t rtc_get_time();
uint32_t rtc_get_time_long();

uint8_t rtc_read_ram(uint8_t offset);
void rtc_write_ram(uint8_t offset, uint8_t value);

uint8_t rtc_get_address();

#endif // AB0805_H