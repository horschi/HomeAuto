EESchema Schematic File Version 2
LIBS:power
LIBS:device
LIBS:transistors
LIBS:conn
LIBS:linear
LIBS:regul
LIBS:74xx
LIBS:cmos4000
LIBS:adc-dac
LIBS:memory
LIBS:xilinx
LIBS:microcontrollers
LIBS:dsp
LIBS:microchip
LIBS:analog_switches
LIBS:motorola
LIBS:texas
LIBS:intel
LIBS:audio
LIBS:interface
LIBS:digital-audio
LIBS:philips
LIBS:display
LIBS:cypress
LIBS:siliconi
LIBS:opto
LIBS:atmel
LIBS:contrib
LIBS:valves
LIBS:EBusConv-cache
EELAYER 25 0
EELAYER END
$Descr A4 11693 8268
encoding utf-8
Sheet 1 1
Title ""
Date ""
Rev ""
Comp ""
Comment1 ""
Comment2 ""
Comment3 ""
Comment4 ""
$EndDescr
$Comp
L LM358 U1
U 2 1 5A21646D
P 5600 2700
F 0 "U1" H 5550 2900 50  0000 L CNN
F 1 "LM358" H 5550 2450 50  0000 L CNN
F 2 "Housings_DIP:DIP-8_W7.62mm" H 5600 2700 50  0001 C CNN
F 3 "" H 5600 2700 50  0000 C CNN
	2    5600 2700
	1    0    0    -1  
$EndComp
$Comp
L LM358 U1
U 1 1 5A21657A
P 5250 3650
F 0 "U1" H 5200 3850 50  0000 L CNN
F 1 "LM358" H 5200 3400 50  0000 L CNN
F 2 "Housings_DIP:DIP-8_W7.62mm" H 5250 3650 50  0001 C CNN
F 3 "" H 5250 3650 50  0000 C CNN
	1    5250 3650
	1    0    0    -1  
$EndComp
$Comp
L R R1
U 1 1 5A216ED5
P 3900 2600
F 0 "R1" V 3980 2600 50  0000 C CNN
F 1 "30k" V 3900 2600 50  0000 C CNN
F 2 "Resistors_ThroughHole:Resistor_Horizontal_RM10mm" V 3830 2600 50  0001 C CNN
F 3 "" H 3900 2600 50  0000 C CNN
	1    3900 2600
	0    1    1    0   
$EndComp
$Comp
L R R2
U 1 1 5A217003
P 4150 3150
F 0 "R2" V 4230 3150 50  0000 C CNN
F 1 "3k" V 4150 3150 50  0000 C CNN
F 2 "Resistors_ThroughHole:Resistor_Horizontal_RM10mm" V 4080 3150 50  0001 C CNN
F 3 "" H 4150 3150 50  0000 C CNN
	1    4150 3150
	1    0    0    -1  
$EndComp
$Comp
L POT RV1
U 1 1 5A21730C
P 4500 3150
F 0 "RV1" H 4500 3070 50  0000 C CNN
F 1 "100k" H 4500 3150 50  0000 C CNN
F 2 "Potentiometers:Potentiometer_Trimmer-Piher_PT15-V15_horizontal" H 4500 3150 50  0001 C CNN
F 3 "" H 4500 3150 50  0000 C CNN
	1    4500 3150
	0    1    1    0   
$EndComp
$Comp
L LED D1
U 1 1 5A217999
P 6200 4000
F 0 "D1" H 6200 4100 50  0000 C CNN
F 1 "LED" H 6200 3900 50  0000 C CNN
F 2 "LEDs:LED-5MM" H 6200 4000 50  0001 C CNN
F 3 "" H 6200 4000 50  0000 C CNN
	1    6200 4000
	0    -1   -1   0   
$EndComp
$Comp
L R R3
U 1 1 5A217A5A
P 5900 3650
F 0 "R3" V 5980 3650 50  0000 C CNN
F 1 "1k" V 5900 3650 50  0000 C CNN
F 2 "Resistors_ThroughHole:Resistor_Horizontal_RM10mm" V 5830 3650 50  0001 C CNN
F 3 "" H 5900 3650 50  0000 C CNN
	1    5900 3650
	0    1    1    0   
$EndComp
$Comp
L R R4
U 1 1 5A217B2B
P 6250 2700
F 0 "R4" V 6330 2700 50  0000 C CNN
F 1 "4k" V 6250 2700 50  0000 C CNN
F 2 "Resistors_ThroughHole:Resistor_Horizontal_RM10mm" V 6180 2700 50  0001 C CNN
F 3 "" H 6250 2700 50  0000 C CNN
	1    6250 2700
	0    1    1    0   
$EndComp
$Comp
L R R5
U 1 1 5A217B84
P 6700 3550
F 0 "R5" V 6780 3550 50  0000 C CNN
F 1 "40k" V 6700 3550 50  0000 C CNN
F 2 "Resistors_ThroughHole:Resistor_Horizontal_RM10mm" V 6630 3550 50  0001 C CNN
F 3 "" H 6700 3550 50  0000 C CNN
	1    6700 3550
	1    0    0    -1  
$EndComp
$Comp
L VCC #PWR01
U 1 1 5A217CCB
P 7100 1700
F 0 "#PWR01" H 7100 1550 50  0001 C CNN
F 1 "VCC" H 7100 1850 50  0000 C CNN
F 2 "" H 7100 1700 50  0000 C CNN
F 3 "" H 7100 1700 50  0000 C CNN
	1    7100 1700
	1    0    0    -1  
$EndComp
$Comp
L GND #PWR02
U 1 1 5A217D0C
P 6900 4400
F 0 "#PWR02" H 6900 4150 50  0001 C CNN
F 1 "GND" H 6900 4250 50  0000 C CNN
F 2 "" H 6900 4400 50  0000 C CNN
F 3 "" H 6900 4400 50  0000 C CNN
	1    6900 4400
	1    0    0    -1  
$EndComp
$Comp
L CONN_01X02 P1
U 1 1 5A21832E
P 3350 2650
F 0 "P1" H 3350 2800 50  0000 C CNN
F 1 "EBus in" V 3450 2650 50  0000 C CNN
F 2 "Pin_Headers:Pin_Header_Straight_1x02" H 3350 2650 50  0001 C CNN
F 3 "" H 3350 2650 50  0000 C CNN
	1    3350 2650
	-1   0    0    1   
$EndComp
$Comp
L R R6
U 1 1 5A21B45D
P 4500 3800
F 0 "R6" V 4580 3800 50  0000 C CNN
F 1 "10k" V 4500 3800 50  0000 C CNN
F 2 "Resistors_ThroughHole:Resistor_Horizontal_RM10mm" V 4430 3800 50  0001 C CNN
F 3 "" H 4500 3800 50  0000 C CNN
	1    4500 3800
	1    0    0    -1  
$EndComp
Wire Wire Line
	5300 2800 4750 2800
Wire Wire Line
	4750 2800 4750 3750
Wire Wire Line
	4050 2600 5300 2600
Wire Wire Line
	4650 3150 4750 3150
Connection ~ 4750 3150
Connection ~ 4850 2600
Wire Wire Line
	4150 3000 4150 2600
Connection ~ 4150 2600
Wire Wire Line
	5150 3350 5150 1700
Wire Wire Line
	4500 1700 7200 1700
Wire Wire Line
	5500 1700 5500 2400
Wire Wire Line
	5500 4400 5500 3000
Wire Wire Line
	3550 4400 7200 4400
Wire Wire Line
	5150 4400 5150 3950
Wire Wire Line
	4150 4400 4150 3300
Connection ~ 5150 4400
Connection ~ 4150 4400
Connection ~ 4500 4400
Connection ~ 5150 1700
Wire Wire Line
	4500 3000 4500 1700
Wire Wire Line
	5550 3650 5750 3650
Wire Wire Line
	6050 3650 6200 3650
Wire Wire Line
	6200 3650 6200 3800
Wire Wire Line
	6200 4400 6200 4200
Connection ~ 5500 4400
Wire Wire Line
	6700 4400 6700 3700
Connection ~ 6200 4400
Wire Wire Line
	6700 3400 6700 2700
Wire Wire Line
	6100 2700 5900 2700
Connection ~ 6700 2700
Connection ~ 5500 1700
Connection ~ 6700 4400
Wire Wire Line
	3750 2600 3550 2600
Wire Wire Line
	3550 2700 3550 4400
Wire Wire Line
	4500 3650 4500 3300
Wire Wire Line
	4500 3950 4500 4400
Connection ~ 6900 4400
Connection ~ 7100 1700
Wire Wire Line
	4750 3750 4950 3750
Wire Wire Line
	4950 3550 4850 3550
Wire Wire Line
	4850 3550 4850 2600
$Comp
L CONN_01X03 P2
U 1 1 5A242EAF
P 7600 2950
F 0 "P2" H 7600 3150 50  0000 C CNN
F 1 "Rasp" V 7700 2950 50  0000 C CNN
F 2 "Socket_Strips:Socket_Strip_Straight_1x03" H 7600 2950 50  0001 C CNN
F 3 "" H 7600 2950 50  0000 C CNN
	1    7600 2950
	1    0    0    -1  
$EndComp
Wire Wire Line
	6400 2700 7050 2700
Wire Wire Line
	7050 2700 7050 2950
Wire Wire Line
	7050 2950 7400 2950
Wire Wire Line
	7200 4400 7200 3050
Wire Wire Line
	7200 3050 7400 3050
Wire Wire Line
	7200 1700 7200 2850
Wire Wire Line
	7200 2850 7400 2850
$EndSCHEMATC
