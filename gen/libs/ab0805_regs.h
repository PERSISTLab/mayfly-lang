#ifndef AB0805_REGS_H
#define AB0805_REGS_H

#define AB0805_ADDRESS              0x69 // this device only has one address
#define AB0805_DEFAULT_ADDRESS      0x69

#define AB0805_RA_HUNDREDTHS        0X00
#define AB0805_RA_SECONDS           0x01
#define AB0805_RA_MINUTES           0x02
#define AB0805_RA_HOURS             0x03
#define AB0805_RA_DATE              0x04
#define AB0805_RA_MONTH             0x05
#define AB0805_RA_YEAR              0x06
#define AB0805_RA_DAY               0X07

#define AB0805_RA_HUNDREDTHS_ALARM  0X08
#define AB0805_RA_SECONDS_ALARM     0X09
#define AB0805_RA_MINUTES_ALARM     0X0A
#define AB0805_RA_HOURS_ALARM       0X0B
#define AB0805_RA_DATE_ALARM        0X0C
#define AB0805_RA_MONTH_ALARM       0X0D
#define AB0805_RA_WEEKDAYS_ALARM    0X0E

#define AB0805_RA_STATUS            0X0F
#define AB0805_RA_CONTROL1          0x10            
#define AB0805_RA_CONTROL2          0x11            //interrupt control
#define AB0805_RA_OSC_CONTROL       0x1C
#define AB0805_RA_OSC_STATUS        0x1D
//Config Key-written with specific values to access certain registers
//access Oscillator control (0x1C) -> write 0xA1
//Software Reset (doesn't update Config Key) -> write 0x3C
#define AB0805_RA_CONFIG_KEY           0X1F
#define AB0805_RA_ID0                  0X28            //0x08
#define AB0805_RA_ID1                  0X29            //0x05
#define AB0805_NORMAL_RAM_START		   0x40
#define AB0805_TRICKLE		   		   0x20
#define AB0805_BATMODE		   		   0x27
#define AB0805_BREF		   		   	   0x21




#define AB0805_HUNDRETHS_10_BIT       7
#define AB0805_HUNDRETHS_10_LENGTH    4
#define AB0805_HUNDRETHS_1_BIT        3
#define AB0805_HUNDRETHS_1_LENGTH     4

#define AB0805_SECONDS_10_BIT         6
#define AB0805_SECONDS_10_LENGTH      3
#define AB0805_SECONDS_1_BIT          3
#define AB0805_SECONDS_1_LENGTH       4

#define AB0805_MINUTES_10_BIT         6
#define AB0805_MINUTES_10_LENGTH      3
#define AB0805_MINUTES_1_BIT          3
#define AB0805_MINUTES_1_LENGTH       4

//24 HOUR MODE if 0x10 bit 6 = 0
//12 HOUR MODE if 0X10 bit 6 = 1
#define AB0805_HOURS_AMPM_BIT         5         //2nd HOURS_10 bit if in 24-hour mode
#define AB0805_HOURS_10_BIT           4
#define AB0805_HOURS_1_BIT            3
#define AB0805_HOURS_1_LENGTH         4

#define AB0805_DATE_10_BIT            5
#define AB0805_DATE_10_LENGTH         2
#define AB0805_DATE_1_BIT             3
#define AB0805_DATE_1_LENGTH          4

#define AB0805_MONTH_10_BIT           4
#define AB0805_MONTH_1_BIT            3
#define AB0805_MONTH_1_LENGTH         4

#define AB0805_YEAR_10H_BIT           7
#define AB0805_YEAR_10H_LENGTH        4
#define AB0805_YEAR_1H_BIT            3
#define AB0805_YEAR_1H_LENGTH         4

#define AB0805_DAY_BIT                2
#define AB0805_DAY_LENGTH             3

#define AB0805_CONTROL1_STOP_BIT      7           //If 1 stops clocking system
#define AB0805_CONTROL1_12OR24_BIT    6           //0 -> 24 hour mode, 1-> 12 hour mode
#define AB0805_CONTROL_WRTC           0           //must be 1 to write to time registers
#define AB0805_OSC_CONTROL_OSC_SEL    7

#endif // AB0805_REGS_H